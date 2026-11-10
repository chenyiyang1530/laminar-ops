package com.laminar.ops.biz.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laminar.ops.common.BizException;
import com.laminar.ops.security.AuthContext;
import com.laminar.ops.security.DataScopeService;
import com.laminar.ops.security.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InspectionService {

    private final JdbcTemplate jdbc;
    private final WorkorderService workorderService;
    private final ObjectMapper objectMapper;
    private final DataScopeService dataScope;

    public Map<String, Object> page(int pageNum, int pageSize, Map<String, String> params) {
        LoginUser user = AuthContext.require();
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" where 1=1 ");
        String status = params.get("status");
        String hospitalId = params.get("hospitalId");
        String keyword = params.get("keyword");
        String mine = params.get("mine");
        if (status != null && !status.isBlank()) {
            if ("todo".equals(status)) where.append(" and t.status in ('0','3','1') ");
            else { where.append(" and t.status = ? "); args.add(status); }
        }
        if (hospitalId != null && !hospitalId.isBlank()) { where.append(" and t.hospital_id = ? "); args.add(hospitalId); }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" and (t.task_no like ? or t.problem_desc like ?) ");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
        }
        java.util.List<Long> allowed = dataScope.visibleHospitalIds(user);
        if (allowed != null) {
            if (allowed.isEmpty()) {
                where.append(" and 1=0 ");
            } else {
                where.append(" and t.hospital_id in (")
                        .append(allowed.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")))
                        .append(") ");
            }
        }
        Long companyId = dataScope.companyId(user);
        if (companyId != null) {
            where.append(" and t.company_id = ? ");
            args.add(companyId);
        }
        if (!user.isHospitalUser() && "self".equals(user.getDataScope()) && user.getEngineerId() != null) {
            where.append(" and (t.assignee_id = ? or t.assignee_id is null) ");
            args.add(user.getEngineerId());
        }
        if ("1".equals(mine) && user.getEngineerId() != null) {
            where.append(" and t.assignee_id = ? ");
            args.add(user.getEngineerId());
        }
        Long total = jdbc.queryForObject("select count(*) from biz_inspection_task t" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add(Math.max(0, (pageNum - 1) * pageSize));
        List<Map<String, Object>> records = jdbc.queryForList(
                "select t.task_id as `taskId`, t.task_no as `taskNo`, t.plan_id as `planId`, t.plan_time as `planTime`, " +
                        "t.status, t.result, t.assignee_name as `assigneeName`, t.workorder_id as `workorderId`, " +
                        "t.problem_desc as `problemDesc`, t.finish_time as `finishTime`, " +
                        "p.plan_name as `planName`, h.hospital_name as `hospitalName`, " +
                        "a.area_name as `areaName`, e.equipment_name as `equipmentName` " +
                        "from biz_inspection_task t " +
                        "left join biz_inspection_plan p on p.plan_id=t.plan_id " +
                        "left join biz_hospital h on h.hospital_id=t.hospital_id " +
                        "left join biz_clean_area a on a.area_id=t.area_id " +
                        "left join biz_equipment e on e.equipment_id=t.equipment_id " +
                        where + " order by t.plan_time desc, t.task_id desc limit ? offset ?", pageArgs.toArray());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total == null ? 0 : total);
        return result;
    }

    public Map<String, Object> detail(Long id) {
        Map<String, Object> task = jdbc.queryForMap(
                "select t.*, p.plan_name as plan_name, h.hospital_name as hospital_name, a.area_name as area_name, " +
                        "e.equipment_name as equipment_name, d.department_name as department_name " +
                        "from biz_inspection_task t left join biz_inspection_plan p on p.plan_id=t.plan_id " +
                        "left join biz_hospital h on h.hospital_id=t.hospital_id " +
                        "left join biz_clean_area a on a.area_id=t.area_id " +
                        "left join biz_equipment e on e.equipment_id=t.equipment_id " +
                        "left join biz_department d on d.department_id=t.department_id where t.task_id=?", id);
        checkAccess(task);
        List<Map<String, Object>> items = jdbc.queryForList(
                "select item_id as `itemId`, item_name as `itemName`, standard, result, item_value as `itemValue`, " +
                        "image_url as `imageUrl`, remark, sort from biz_inspection_item where task_id=? order by sort, item_id", id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("task", task);
        result.put("items", items);
        return result;
    }

    public void start(Long id) {
        Map<String, Object> task = require(id);
        if (!"0".equals(String.valueOf(task.get("status"))) && !"3".equals(String.valueOf(task.get("status")))) {
            throw BizException.badRequest("只有待执行/已逾期任务可以开始");
        }
        jdbc.update("update biz_inspection_task set status='1', start_time=coalesce(start_time, now()) where task_id=?", id);
    }

    @Transactional
    public Long complete(Long id, Map<String, Object> body) {
        Map<String, Object> task = require(id);
        String status = String.valueOf(task.get("status"));
        if (!List.of("0", "1", "3").contains(status)) throw BizException.badRequest("任务已结束");
        String result = String.valueOf(body.getOrDefault("result", "1"));
        String problem = (String) body.get("problemDesc");
        if ("0".equals(result) && (problem == null || problem.isBlank())) {
            throw BizException.badRequest("结果异常时请填写问题描述");
        }
        if (body.get("items") instanceof List<?> items) {
            for (Object o : items) {
                if (!(o instanceof Map<?, ?> item)) continue;
                Object itemId = item.get("itemId");
                if (itemId == null) continue;
                jdbc.update("update biz_inspection_item set result=?, item_value=?, remark=? where item_id=?",
                        item.get("result"), item.get("itemValue"), item.get("remark"), ((Number) itemId).longValue());
            }
        }
        jdbc.update("update biz_inspection_task set status='2', finish_time=now(), result=?, problem_desc=?, score=? where task_id=?",
                result, problem, body.get("score"), id);

        Long workorderId = null;
        Object create = body.get("createWorkorder");
        if ("0".equals(result) && Boolean.parseBoolean(String.valueOf(create))) {
            String title = "巡检异常：" + (task.get("task_no") == null ? "" : task.get("task_no"));
            workorderId = workorderService.createInternal("inspection", id,
                    longOrNull(task.get("hospital_id")), longOrNull(task.get("campus_id")),
                    longOrNull(task.get("department_id")), longOrNull(task.get("area_id")),
                    longOrNull(task.get("equipment_id")), title, problem);
            jdbc.update("update biz_inspection_task set workorder_id=? where task_id=?", workorderId, id);
            jdbc.update("update biz_workorder set inspection_task_id=? where workorder_id=?", id, workorderId);
        }
        return workorderId;
    }

    /** 由计划生成到期任务 */
    public int generate() {
        List<Map<String, Object>> plans = jdbc.queryForList(
                "select * from biz_inspection_plan where status='0' and (end_date is null or end_date >= curdate()) " +
                        "and (next_run_date is null or next_run_date <= curdate())");
        int created = 0;
        for (Map<String, Object> plan : plans) {
            LocalDate planDate = toLocalDate(plan.get("next_run_date"));
            if (planDate == null) planDate = LocalDate.now();
            if (planDate.isBefore(LocalDate.now())) planDate = LocalDate.now();
            Integer exists = jdbc.queryForObject("select count(*) from biz_inspection_task where plan_id=? and plan_time=?",
                    Integer.class, plan.get("plan_id"), planDate);
            if (exists == null || exists == 0) {
                try {
                    String no = nextNo(planDate);
                    Long taskId = insertTask(no, plan, planDate);
                    createItems(taskId, (String) plan.get("item_template"));
                    created++;
                } catch (org.springframework.dao.DuplicateKeyException e) {
                    log.warn("[巡检] 任务编号冲突，跳过 plan={} date={}", plan.get("plan_id"), planDate);
                }
            }
            jdbc.update("update biz_inspection_plan set next_run_date=? where plan_id=?",
                    nextDate(planDate, (String) plan.get("cycle_type"), plan.get("cycle_value")), plan.get("plan_id"));
        }
        int overdue = jdbc.update("update biz_inspection_task set status='3' where status='0' and plan_time < curdate()");
        log.info("[巡检] 生成 {} 个任务，标记逾期 {} 个", created, overdue);
        return created;
    }

    private Long insertTask(String no, Map<String, Object> plan, LocalDate planDate) {
        KeyHolder holder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "insert into biz_inspection_task(task_no,company_id,plan_id,task_type,hospital_id,campus_id,department_id,area_id," +
                            "plan_time,assignee_id,status,create_by,create_time) values(?,?,?,?,?,?,?,?,?,?,'0','system',now())",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, no);
            ps.setObject(2, plan.get("company_id"));
            ps.setObject(3, plan.get("plan_id"));
            ps.setString(4, "plan");
            ps.setObject(5, plan.get("hospital_id"));
            ps.setObject(6, plan.get("campus_id"));
            ps.setObject(7, plan.get("department_id"));
            ps.setObject(8, plan.get("area_id"));
            ps.setObject(9, planDate);
            ps.setObject(10, plan.get("assignee_id"));
            return ps;
        }, holder);
        Long taskId = holder.getKey() == null ? 0L : holder.getKey().longValue();
        if (plan.get("assignee_id") != null) {
            List<String> names = jdbc.queryForList("select engineer_name from biz_engineer where engineer_id=?",
                    String.class, plan.get("assignee_id"));
            if (!names.isEmpty()) {
                jdbc.update("update biz_inspection_task set assignee_name=? where task_id=?", names.get(0), taskId);
            }
        }
        return taskId;
    }

    private void createItems(Long taskId, String template) {
        List<String> items = new ArrayList<>();
        if (template != null && !template.isBlank()) {
            try {
                items = objectMapper.readValue(template, new TypeReference<List<String>>() {
                });
            } catch (Exception e) {
                items = List.of(template.split("\\n"));
            }
        }
        if (items.isEmpty()) {
            items = List.of("检查设备运行状态", "检查过滤器压差", "检查房间压差", "记录温湿度");
        }
        int sort = 1;
        for (String name : items) {
            String n = name == null ? "" : name.trim();
            if (n.isEmpty()) continue;
            jdbc.update("insert into biz_inspection_item(task_id,item_name,standard,sort) values(?,?,?,?)",
                    taskId, n, "", sort++);
        }
    }

    private Map<String, Object> require(Long id) {
        List<Map<String, Object>> list = jdbc.queryForList("select * from biz_inspection_task where task_id=?", id);
        if (list.isEmpty()) throw BizException.badRequest("巡检任务不存在");
        Map<String, Object> task = list.get(0);
        checkAccess(task);
        return task;
    }

    private void checkAccess(Map<String, Object> task) {
        LoginUser user = AuthContext.require();
        Object hospital = task.get("hospital_id");
        if (hospital != null && !dataScope.canAccessHospital(user, ((Number) hospital).longValue())) {
            throw BizException.forbidden("无权操作其他医院任务");
        }
        Long companyId = dataScope.companyId(user);
        if (companyId != null && task.get("company_id") != null
                && !companyId.equals(((Number) task.get("company_id")).longValue())) {
            throw BizException.forbidden("无权操作其他公司巡检任务");
        }
        if ("self".equals(user.getDataScope()) && user.getEngineerId() != null) {
            Object assignee = task.get("assignee_id");
            if (assignee != null && !user.getEngineerId().equals(((Number) assignee).longValue())) {
                throw BizException.forbidden("只能执行分配给本人的巡检任务");
            }
        }
    }

    private String nextNo(LocalDate date) {
        String head = "IT-" + date.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        Integer max = jdbc.queryForObject(
                "select coalesce(max(cast(substring_index(task_no,'-',-1) as unsigned)),0) " +
                        "from biz_inspection_task where task_no like ?", Integer.class, head + "%");
        return head + String.format("%03d", (max == null ? 0 : max) + 1);
    }

    static LocalDate nextDate(LocalDate date, String type, Object value) {
        int v = value == null ? 1 : ((Number) value).intValue();
        if (v <= 0) v = 1;
        return switch (type == null ? "day" : type) {
            case "week" -> date.plusWeeks(v);
            case "month" -> date.plusMonths(v);
            case "quarter" -> date.plusMonths(3L * v);
            case "year" -> date.plusYears(v);
            default -> date.plusDays(v);
        };
    }

    private Long longOrNull(Object o) {
        return o == null ? null : ((Number) o).longValue();
    }

    static LocalDate toLocalDate(Object o) {
        if (o == null) return null;
        if (o instanceof LocalDate d) return d;
        if (o instanceof java.sql.Date d) return d.toLocalDate();
        if (o instanceof java.util.Date d) return new java.sql.Date(d.getTime()).toLocalDate();
        String s = String.valueOf(o);
        return s.length() >= 10 ? LocalDate.parse(s.substring(0, 10)) : null;
    }
}
