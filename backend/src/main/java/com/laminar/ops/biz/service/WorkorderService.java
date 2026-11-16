package com.laminar.ops.biz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laminar.ops.common.BizException;
import com.laminar.ops.security.AuthContext;
import com.laminar.ops.security.DataScopeService;
import com.laminar.ops.security.LoginUser;
import com.laminar.ops.system.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WorkorderService {

    private final JdbcTemplate jdbc;
    private final DataScopeService dataScope;
    private final ContractScopeService contractScope;
    private final SmartDispatchService smartDispatchService;
    private final ObjectMapper objectMapper;
    private final SysConfigService sysConfig;

    private static final Map<String, String> STATUS = Map.of(
            "0", "待受理", "1", "待派工", "2", "处理中", "3", "待验收",
            "4", "待回访", "5", "已完成", "6", "已取消");

    public Map<String, Object> page(int pageNum, int pageSize, Map<String, String> params, boolean mine) {
        LoginUser user = AuthContext.require();
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" where 1=1 ");
        String status = params.get("status");
        String level = params.get("level");
        String hospitalId = params.get("hospitalId");
        String keyword = params.get("keyword");
        if (status != null && !status.isBlank()) {
            if ("open".equals(status)) where.append(" and w.status in ('0','1','2','3','4') ");
            else { where.append(" and w.status = ? "); args.add(status); }
        }
        if (level != null && !level.isBlank()) { where.append(" and w.fault_level = ? "); args.add(level); }
        if (hospitalId != null && !hospitalId.isBlank()) { where.append(" and w.hospital_id = ? "); args.add(hospitalId); }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" and (w.workorder_no like ? or w.title like ?) ");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
        }
        java.util.List<Long> allowed = dataScope.visibleHospitalIds(user);
        boolean selfEngineer = "self".equals(user.getDataScope()) && user.getEngineerId() != null;
        if (allowed != null && !selfEngineer) {
            if (allowed.isEmpty()) {
                where.append(" and 1=0 ");
            } else {
                where.append(" and w.hospital_id in (")
                        .append(allowed.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")))
                        .append(") ");
            }
        }
        Long companyId = dataScope.companyId(user);
        if (companyId != null) {
            where.append(" and w.company_id = ? ");
            args.add(companyId);
        }
        if (mine) {
            if (user.getEngineerId() == null) {
                throw BizException.forbidden("当前账号未绑定工程师档案");
            }
            where.append(" and w.engineer_id = ? ");
            args.add(user.getEngineerId());
        } else if (!user.isHospitalUser() && "self".equals(user.getDataScope()) && user.getEngineerId() != null) {
            where.append(" and w.engineer_id = ? ");
            args.add(user.getEngineerId());
        }
        if (user.isHospitalUser() && (user.getRoleKeys().contains("hospital_dept")
                || user.getRoleKeys().contains("hospital_sub_admin")) && user.getDepartmentId() != null) {
            where.append(" and w.department_id = ? ");
            args.add(user.getDepartmentId());
        }
        Long total = jdbc.queryForObject("select count(*) from biz_workorder w" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add(Math.max(0, (pageNum - 1) * pageSize));
        List<Map<String, Object>> records = jdbc.queryForList(
                "select w.workorder_id as `workorderId`, w.workorder_no as `workorderNo`, w.title, w.fault_level as `faultLevel`, " +
                        "w.status, w.source, w.service_type as `serviceType`, w.fault_desc as `faultDesc`, " +
                        "w.hospital_id as `hospitalId`, w.equipment_id as `equipmentId`, w.company_id as `companyId`, " +
                        "w.engineer_id as `engineerId`, " +
                        "w.sla_deadline as `slaDeadline`, w.arrive_time as `arriveTime`, w.finish_time as `finishTime`, " +
                        "w.total_cost as `totalCost`, " +
                        "date_format(w.create_time,'%Y-%m-%d %H:%i') as `createTime`, " +
                        "h.hospital_name as `hospitalName`, e.equipment_name as `equipmentName`, " +
                        "g.engineer_name as `engineerName`, c.campus_name as `campusName`, cp.company_name as `companyName` " +
                        "from biz_workorder w " +
                        "left join biz_hospital h on h.hospital_id=w.hospital_id " +
                        "left join biz_equipment e on e.equipment_id=w.equipment_id " +
                        "left join biz_engineer g on g.engineer_id=w.engineer_id " +
                        "left join biz_campus c on c.campus_id=w.campus_id " +
                        "left join biz_company cp on cp.company_id=w.company_id " +
                        where + " order by w.workorder_id desc limit ? offset ?", pageArgs.toArray());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total == null ? 0 : total);
        result.put("statusDict", STATUS);
        return result;
    }

    public Map<String, Object> detail(Long id) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select w.*, h.hospital_name as hospital_name, e.equipment_name as equipment_name, " +
                        "e.equipment_code as equipment_code, g.engineer_name as engineer_name, g.phone as engineer_phone, " +
                        "c.campus_name as campus_name, d.department_name as department_name, " +
                        "cp.company_name as company_name " +
                        "from biz_workorder w left join biz_hospital h on h.hospital_id=w.hospital_id " +
                        "left join biz_equipment e on e.equipment_id=w.equipment_id " +
                        "left join biz_engineer g on g.engineer_id=w.engineer_id " +
                        "left join biz_campus c on c.campus_id=w.campus_id " +
                        "left join biz_department d on d.department_id=w.department_id " +
                        "left join biz_company cp on cp.company_id=w.company_id " +
                        "where w.workorder_id=?", id);
        if (rows.isEmpty()) throw BizException.badRequest("工单不存在");
        Map<String, Object> wo = rows.get(0);
        checkScope(wo);
        List<Map<String, Object>> logs = jdbc.queryForList(
                "select action, operator_name as `operatorName`, from_status as `fromStatus`, to_status as `toStatus`, " +
                        "content, date_format(create_time,'%Y-%m-%d %H:%i:%s') as `createTime` " +
                        "from biz_workorder_log where workorder_id=? order by log_id", id);
        List<Map<String, Object>> costs = jdbc.queryForList(
                "select cost_id as `costId`, cost_type as `costType`, cost_name as `costName`, quantity, price, amount, remark " +
                        "from biz_workorder_cost where workorder_id=?", id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workorder", wo);
        result.put("logs", logs);
        result.put("costs", costs);
        return result;
    }

    public Long create(Map<String, Object> body) {
        LoginUser user = AuthContext.require();
        Object hospitalId = body.get("hospitalId");
        java.util.List<Long> allowed = dataScope.visibleHospitalIds(user);
        if (allowed != null) {
            if (allowed.isEmpty()) throw BizException.forbidden("当前账号没有可操作的医院");
            Long hid = longOrNull(hospitalId);
            if (hid == null) {
                if (allowed.size() == 1) hospitalId = allowed.get(0);
                else throw BizException.badRequest("请选择医院");
            } else if (!allowed.contains(hid)) {
                throw BizException.forbidden("无权在该医院创建工单");
            }
            if (allowed.size() == 1) hospitalId = allowed.get(0);
        }
        if (hospitalId == null) throw BizException.badRequest("请选择医院");
        String title = str(body.get("title"));
        if (title.isBlank()) throw BizException.badRequest("请填写工单标题");
        String level = str(body.getOrDefault("faultLevel", "2"));
        String no = nextNo();
        Long equipmentId = longOrNull(body.get("equipmentId"));
        Long campusId = longOrNull(body.get("campusId"));
        Long deptId = longOrNull(body.get("departmentId"));
        if (equipmentId != null) {
            Map<String, Object> eq = jdbc.queryForMap(
                    "select hospital_id, campus_id, department_id from biz_equipment where equipment_id=?", equipmentId);
            if (!String.valueOf(eq.get("hospital_id")).equals(String.valueOf(longOf(hospitalId)))) {
                throw BizException.badRequest("所选设备不属于该医院");
            }
            if (campusId == null) campusId = longOrNull(eq.get("campus_id"));
            if (deptId == null) deptId = longOrNull(eq.get("department_id"));
        }
        if (equipmentId == null && user.isHospitalUser()) {
            if (deptId == null && user.getDepartmentId() != null) deptId = user.getDepartmentId();
            if (campusId == null && deptId != null) {
                List<Long> campuses = jdbc.queryForList(
                        "select campus_id from biz_department where department_id=?", Long.class, deptId);
                campusId = campuses.isEmpty() ? null : campuses.get(0);
            }
        }
        if (user.isHospitalUser() && user.getDepartmentId() != null
                && (user.getRoleKeys().contains("hospital_sub_admin")
                || user.getRoleKeys().contains("hospital_dept"))
                && (deptId == null || !deptId.equals(user.getDepartmentId()))) {
            throw BizException.forbidden("只能报修本科室的设备");
        }
        if (campusId != null) {
            List<Map<String, Object>> campuses = jdbc.queryForList(
                    "select hospital_id from biz_campus where campus_id=?", campusId);
            if (campuses.isEmpty() || !String.valueOf(campuses.get(0).get("hospital_id")).equals(String.valueOf(longOf(hospitalId)))) {
                throw BizException.badRequest("所选院区不属于该医院");
            }
            Long currentCompany = dataScope.companyId(user);
            if (currentCompany != null) {
                Integer allowedCampus = jdbc.queryForObject(
                        "select count(*) from biz_company_campus where company_id=? and campus_id=?",
                        Integer.class, currentCompany, campusId);
                if (allowedCampus == null || allowedCampus == 0) {
                    throw BizException.forbidden("本公司未服务该院区，不能创建工单");
                }
            }
        }
        Long id = insertWorkorder(no, title, str(body.get("faultDesc")), level, "0",
                str(body.getOrDefault("source", "hospital")), str(body.getOrDefault("serviceType", "0")),
                longOf(hospitalId), campusId, deptId, equipmentId, null,
                str(body.get("reportContact")), str(body.get("reportPhone")), user.getNickName());
        assignCompany(id, longOf(hospitalId), deptId, equipmentId, campusId);
        log(id, "create", null, "0", "创建工单：" + title);
        notify(id, longOf(hospitalId), "新工单待受理：" + title, "工单 " + no + "，请及时受理派工", "100", "2");
        return id;
    }

    public void receive(Long id) {
        Map<String, Object> wo = require(id);
        checkStatus(wo, "0", "只有待受理工单可以受理");
        jdbc.update("update biz_workorder set status='1', receive_time=now(), receive_by=? where workorder_id=?",
                AuthContext.nickName(), id);
        log(id, "receive", "0", "1", "受理工单");
        notify(id, longOf(wo.get("hospital_id")), "工单已受理：" + wo.get("workorder_no"), "工程师将尽快联系您", "200,201", "3");
    }

    @Transactional
    public void dispatch(Long id, Long engineerId, String remark) {
        Map<String, Object> wo = require(id);
        checkStatus(wo, "1", "只有待派工工单可以派工");
        if (engineerId == null) throw BizException.badRequest("请选择工程师");
        List<Map<String, Object>> engineerRows = jdbc.queryForList(
                "select status, on_duty, max_open_orders from biz_engineer where engineer_id=? for update", engineerId);
        if (engineerRows.isEmpty()) throw BizException.badRequest("工程师不存在");
        Map<String, Object> engineer = engineerRows.get(0);
        if (!"0".equals(String.valueOf(engineer.get("status")))) {
            throw BizException.badRequest("该工程师已停用，不能派工");
        }
        if (!"1".equals(String.valueOf(engineer.get("on_duty")))) {
            throw BizException.badRequest("该工程师当前不在岗，不能派工");
        }
        int maxOpen = engineer.get("max_open_orders") == null ? 5
                : ((Number) engineer.get("max_open_orders")).intValue();
        Integer load = jdbc.queryForObject(
                "select count(*) from biz_workorder where engineer_id=? and status in ('0','1','2','3','4')",
                Integer.class, engineerId);
        if (load != null && load >= maxOpen) {
            throw BizException.badRequest("该工程师在手单已达上限（" + maxOpen + " 单），不能派工");
        }
        Integer onLeave = jdbc.queryForObject(
                "select count(*) from biz_engineer_leave where engineer_id=? and status='APPROVED' " +
                        "and start_date <= curdate() and end_date >= curdate()",
                Integer.class, engineerId);
        if (onLeave != null && onLeave > 0) {
            throw BizException.badRequest("该工程师当前请假中，不能派工");
        }
        validateEngineerRegion(wo, engineerId, "派工");
        Object woCompany = wo.get("company_id");
        if (woCompany != null) {
            List<Long> companies = jdbc.queryForList(
                    "select company_id from biz_engineer where engineer_id=?", Long.class, engineerId);
            Long engineerCompany = companies.isEmpty() ? null : companies.get(0);
            if (engineerCompany == null || !engineerCompany.equals(((Number) woCompany).longValue())) {
                throw BizException.badRequest("该工程师不属于工单承接公司，不能派工");
            }
        }
        Map<String, Object> dispatchScore = smartDispatchService.scoreForEngineer(id, engineerId);
        Integer scoreValue = dispatchScore == null ? null : ((Number) dispatchScore.get("score")).intValue();
        String scoreText = toJson(dispatchScore);
        String scoreVersion = dispatchScore == null ? null : String.valueOf(dispatchScore.get("scoreVersion"));
        jdbc.update("update biz_workorder set status='2', engineer_id=?, dispatch_time=now(), dispatch_by=?, " +
                        "sla_deadline=coalesce(sla_deadline, date_add(now(), interval ? hour)), " +
                        "dispatch_score=?, score_detail=?, score_version=? where workorder_id=?",
                engineerId, AuthContext.nickName(), slaHours(String.valueOf(wo.get("fault_level"))),
                scoreValue, scoreText, scoreVersion, id);
        log(id, "dispatch", "1", "2", "派工：" + engineerName(engineerId) + (remark == null ? "" : "，" + remark));
        notify(id, longOf(wo.get("hospital_id")), "工单已派工：" + wo.get("workorder_no"),
                "工程师：" + engineerName(engineerId), "200,201", "3");
        Long userId = jdbc.queryForObject("select user_id from biz_engineer where engineer_id=?", Long.class, engineerId);
        if (userId != null) {
            jdbc.update("insert into biz_message(msg_type,title,content,level,biz_type,biz_id,biz_key,receiver_id," +
                            "receiver_role,hospital_id,company_id,is_read,create_time) " +
                            "values('workorder',?,?,'2','workorder',?,null,?,null,?,?,'0',now())",
                    "新派工单：" + wo.get("workorder_no"), wo.get("title"), id, userId,
                    wo.get("hospital_id"), wo.get("company_id"));
        }
    }

    public void arrive(Long id) {
        Map<String, Object> wo = require(id);
        checkStatus(wo, "2", "只有处理中的工单可以签到");
        checkAssignedEngineer(wo, "到场");
        if (wo.get("arrive_time") != null) {
            throw BizException.badRequest("该工单已到场签到，请勿重复操作");
        }
        jdbc.update("update biz_workorder set arrive_time=now() where workorder_id=?", id);
        log(id, "arrive", "2", "2", "工程师到场签到");
    }

    public void finish(Long id, Map<String, Object> body) {
        Map<String, Object> wo = require(id);
        checkStatus(wo, "2", "只有处理中的工单可以完工");
        checkAssignedEngineer(wo, "完工");
        if (wo.get("arrive_time") == null) {
            throw BizException.badRequest("请先到场签到，再提交完工");
        }
        String report = str(body.get("report"));
        String rootCause = str(body.get("rootCause"));
        String solution = str(body.get("solution"));
        String parts = str(body.get("partsUsed"));
        String image = str(body.get("imageUrl"));
        if (report == null || report.isBlank()) throw BizException.badRequest("请填写维修报告");
        if (rootCause == null || rootCause.isBlank()) throw BizException.badRequest("请填写故障原因");
        if (solution == null || solution.isBlank()) throw BizException.badRequest("请填写处理措施");
        Integer downtime = jdbc.queryForObject(
                "select greatest(0, coalesce(timestampdiff(minute, arrive_time, now()), 0)) from biz_workorder where workorder_id=?",
                Integer.class, id);
        Integer response = jdbc.queryForObject(
                "select greatest(0, coalesce(timestampdiff(minute, " +
                        "coalesce((select a.first_time from biz_alarm a where a.alarm_id=w.alarm_id), w.create_time), " +
                        "w.arrive_time), 0)) from biz_workorder w where w.workorder_id=?",
                Integer.class, id);
        jdbc.update("update biz_workorder set status='3', finish_time=now(), repair_report=?, " +
                        "finish_root_cause=?, finish_solution=?, finish_parts=?, finish_image=?, " +
                        "downtime_minutes=?, response_minutes=? where workorder_id=?",
                report, rootCause, solution, parts, image, downtime, response, id);
        log(id, "finish", "2", "3", report);
        notify(id, longOf(wo.get("hospital_id")), "工单已完工待验收：" + wo.get("workorder_no"),
                "原因：" + rootCause + "；处理：" + solution, "200,201", "2");
    }

    public void accept(Long id, String result, String remark) {
        LoginUser user = AuthContext.require();
        if (!user.isHospitalUser()) {
            throw BizException.forbidden("只有院方账号可以验收工单");
        }
        Map<String, Object> wo = require(id);
        checkStatus(wo, "3", "只有待验收工单可以验收");
        boolean pass = "1".equals(result);
        jdbc.update("update biz_workorder set status=?, accept_time=now(), accept_by=?, accept_result=?, accept_remark=? where workorder_id=?",
                pass ? "4" : "2", AuthContext.nickName(), pass ? "1" : "0", remark, id);
        log(id, pass ? "accept" : "reject", "3", pass ? "4" : "2", pass ? "验收通过" : "验收驳回：" + str(remark));
        if (pass) {
            notify(id, longOf(wo.get("hospital_id")), "院方验收通过：" + wo.get("workorder_no"),
                    "请安排回访", "100", "3");
        }
        if (pass) {
            BigDecimal total = jdbc.queryForObject("select coalesce(sum(amount),0) from biz_workorder_cost where workorder_id=?",
                    BigDecimal.class, id);
            jdbc.update("update biz_workorder set total_cost=? where workorder_id=?", total, id);
        }
    }

    public void callback(Long id, Integer score, String content) {
        Map<String, Object> wo = require(id);
        checkStatus(wo, "4", "只有待回访工单可以回访");
        jdbc.update("update biz_workorder set status='5', callback_time=now(), callback_by=?, callback_score=?, " +
                        "callback_content=?, close_time=now(), close_by=? where workorder_id=?",
                AuthContext.nickName(), score, content, AuthContext.nickName(), id);
        log(id, "callback", "4", "5", "回访完成，满意度 " + score + " 分");
    }

    public void cancel(Long id, String reason) {
        Map<String, Object> wo = require(id);
        String status = String.valueOf(wo.get("status"));
        if (!List.of("0", "1").contains(status)) throw BizException.badRequest("只有待受理/待派工工单可以取消");
        jdbc.update("update biz_workorder set status='6', cancel_reason=?, close_time=now(), close_by=? where workorder_id=?",
                reason, AuthContext.nickName(), id);
        log(id, "cancel", status, "6", "取消工单：" + str(reason));
    }

    @Transactional
    public void reassign(Long id, Long newEngineerId, String remark) {
        Map<String, Object> wo = require(id);
        String status = String.valueOf(wo.get("status"));
        if (!"2".equals(status)) {
            throw BizException.badRequest("只有处理中的工单可以改派");
        }
        if (newEngineerId == null) throw BizException.badRequest("请选择改派工程师");
        Object oldEngineerObj = wo.get("engineer_id");
        if (oldEngineerObj == null) throw BizException.badRequest("工单尚未派工");
        Long oldEngineerId = ((Number) oldEngineerObj).longValue();
        if (oldEngineerId.equals(newEngineerId)) throw BizException.badRequest("新工程师不能与原工程师相同");
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select status, on_duty, company_id, max_open_orders from biz_engineer where engineer_id=? for update",
                newEngineerId);
        if (rows.isEmpty()) throw BizException.badRequest("工程师不存在");
        Map<String, Object> engineer = rows.get(0);
        if (!"0".equals(String.valueOf(engineer.get("status")))) throw BizException.badRequest("该工程师已停用，不能改派");
        if (!"1".equals(String.valueOf(engineer.get("on_duty")))) throw BizException.badRequest("该工程师当前不在岗，不能改派");
        Object woCompany = wo.get("company_id");
        Object engineerCompany = engineer.get("company_id");
        if (woCompany != null && (engineerCompany == null
                || !((Number) woCompany).equals(((Number) engineerCompany)))) {
            throw BizException.badRequest("该工程师不属于工单承接公司，不能改派");
        }
        validateEngineerRegion(wo, newEngineerId, "改派");
        int maxOpen = engineer.get("max_open_orders") == null ? 5 : ((Number) engineer.get("max_open_orders")).intValue();
        Integer load = jdbc.queryForObject(
                "select count(*) from biz_workorder where engineer_id=? and workorder_id<>? and status in ('0','1','2','3','4')",
                Integer.class, newEngineerId, id);
        if (load != null && load >= maxOpen) throw BizException.badRequest("该工程师在手单已达上限，不能改派");
        Integer onLeave = jdbc.queryForObject(
                "select count(*) from biz_engineer_leave where engineer_id=? and status='APPROVED' " +
                        "and start_date <= curdate() and end_date >= curdate()", Integer.class, newEngineerId);
        if (onLeave != null && onLeave > 0) throw BizException.badRequest("该工程师当前请假中，不能改派");
        Map<String, Object> reassignScore = smartDispatchService.scoreForEngineer(id, newEngineerId);
        Integer reassignValue = reassignScore == null ? null : ((Number) reassignScore.get("score")).intValue();
        jdbc.update("update biz_workorder set engineer_id=?, dispatch_by=?, dispatch_score=?, score_detail=?, score_version=? where workorder_id=?",
                newEngineerId, AuthContext.nickName(), reassignValue, toJson(reassignScore),
                reassignScore == null ? null : String.valueOf(reassignScore.get("scoreVersion")), id);
        String oldName = engineerName(oldEngineerId);
        String newName = engineerName(newEngineerId);
        log(id, "reassign", status, status, "改派：" + oldName + " → " + newName + (remark == null ? "" : "，" + remark));
        completeLeaveHandover(id);
        Long newUser = jdbc.queryForObject("select user_id from biz_engineer where engineer_id=?", Long.class, newEngineerId);
        Long oldUser = jdbc.queryForObject("select user_id from biz_engineer where engineer_id=?", Long.class, oldEngineerId);
        if (newUser != null) {
            jdbc.update("insert into biz_message(msg_type,title,content,level,biz_type,biz_id,receiver_id,company_id,is_read,create_time) " +
                            "values('workorder',?,?,'2','workorder',?,?,?,'0',now())",
                    "工单已改派给你：" + wo.get("workorder_no"), wo.get("title"), id, newUser, woCompany);
        }
        if (oldUser != null) {
            jdbc.update("insert into biz_message(msg_type,title,content,level,biz_type,biz_id,receiver_id,company_id,is_read,create_time) " +
                            "values('workorder',?,?,'2','workorder',?,?,?,'0',now())",
                    "工单已改派：" + wo.get("workorder_no"), "已改派给 " + newName + "，原因：" + remark, id, oldUser, woCompany);
        }
    }

    private void completeLeaveHandover(Long workorderId) {
        List<Long> leaveIds = jdbc.queryForList(
                "select distinct leave_id from biz_leave_handover where workorder_id=? and status='PENDING'",
                Long.class, workorderId);
        if (leaveIds.isEmpty()) return;
        jdbc.update("update biz_leave_handover set status='DONE',done_time=now() where workorder_id=? and status='PENDING'",
                workorderId);
        for (Long leaveId : leaveIds) {
            Integer pending = jdbc.queryForObject(
                    "select count(*) from biz_leave_handover where leave_id=? and status='PENDING'",
                    Integer.class, leaveId);
            if (pending != null && pending == 0) {
                jdbc.update("update biz_engineer_leave set status='APPROVED',current_step=3,update_time=now() " +
                        "where leave_id=? and status='PENDING_HANDOVER'", leaveId);
                List<Map<String, Object>> rows = jdbc.queryForList(
                        "select e.user_id, e.company_id from biz_engineer_leave l join biz_engineer e " +
                                "on e.engineer_id=l.engineer_id where l.leave_id=?", leaveId);
                if (!rows.isEmpty() && rows.get(0).get("user_id") != null) {
                    jdbc.update("insert ignore into biz_message(msg_type,title,content,level,biz_type,biz_id,receiver_id," +
                                    "company_id,is_read,create_time) values('leave',?,?,'2','engineer_leave',?,?,?,'0',now())",
                            "请假已生效", "在手单已完成改派，请假审批正式生效", leaveId,
                            ((Number) rows.get(0).get("user_id")).longValue(), rows.get(0).get("company_id"));
                }
            }
        }
    }

    @Transactional
    public Long fromAlarm(Long alarmId) {
        return fromAlarm(alarmId, true);
    }

    /** 系统/规则自动转工单，不依赖登录上下文 */
    public Long fromAlarmSystem(Long alarmId) {
        return fromAlarm(alarmId, false);
    }

    private Long fromAlarm(Long alarmId, boolean checkScope) {
        Map<String, Object> alarm;
        try {
            alarm = jdbc.queryForMap("select * from biz_alarm where alarm_id=?", alarmId);
        } catch (Exception e) {
            throw BizException.badRequest("告警不存在");
        }
        Object wid = alarm.get("workorder_id");
        if (wid != null) return ((Number) wid).longValue();
        Object alarmHospital = alarm.get("hospital_id");
        if (checkScope && alarmHospital != null
                && !dataScope.canAccessHospital(AuthContext.require(), ((Number) alarmHospital).longValue())) {
            throw BizException.forbidden("无权操作其他医院告警");
        }
        String level = "1".equals(String.valueOf(alarm.get("level"))) ? "0"
                : "2".equals(String.valueOf(alarm.get("level"))) ? "1" : "2";
        Long id = insertWorkorder(nextNo(), "告警处置：" + alarm.get("content"), str(alarm.get("content")), level, "0",
                "alarm", "0", longOf(alarm.get("hospital_id")), longOrNull(alarm.get("campus_id")),
                longOrNull(alarm.get("department_id")),
                longOrNull(alarm.get("equipment_id")), null, "系统", "-", "系统");
        assignCompany(id, longOf(alarm.get("hospital_id")), longOrNull(alarm.get("department_id")),
                longOrNull(alarm.get("equipment_id")), longOrNull(alarm.get("campus_id")));
        Long alarmCompany = longOrNull(alarm.get("company_id"));
        if (alarmCompany != null) {
            jdbc.update("update biz_workorder set company_id=? where workorder_id=?", alarmCompany, id);
        }
        String requiredSkillIds = null;
        if (alarm.get("rule_id") != null) {
            List<String> values = jdbc.queryForList(
                    "select required_skill_ids from biz_alarm_rule where rule_id=?",
                    String.class, ((Number) alarm.get("rule_id")).longValue());
            requiredSkillIds = values.isEmpty() ? null : values.get(0);
        }
        jdbc.update("update biz_workorder set alarm_id=?, required_skill_ids=? where workorder_id=?",
                alarmId, requiredSkillIds, id);
        jdbc.update("update biz_alarm set workorder_id=? where alarm_id=?", id, alarmId);
        log(id, "create", null, "0", "由告警 " + alarm.get("alarm_no") + " 转工单");
        return id;
    }

    /** 巡检/保养异常转工单（内部调用，使用当前登录人） */
    public Long createInternal(String source, Long taskId, Long hospitalId, Long campusId, Long deptId,
                               Long equipmentId, String title, String desc) {
        LoginUser current = AuthContext.get();
        if (current != null && hospitalId != null && !dataScope.canAccessHospital(current, hospitalId)) {
            throw BizException.forbidden("无权操作该医院任务");
        }
        Long id = insertWorkorder(nextNo(), title, desc, "2", "0", source, "0", hospitalId, campusId, deptId,
                equipmentId, null, "系统", "-", AuthContext.nickName());
        assignCompany(id, hospitalId, deptId, equipmentId, campusId);
        log(id, "create", null, "0", ("inspection".equals(source) ? "巡检" : "保养") + "异常转工单");
        return id;
    }

    private Long insertWorkorder(String no, String title, String desc, String level, String status, String source,                                 String serviceType, Long hospitalId, Long campusId, Long deptId,
                                 Long equipmentId, Long engineerId, String contact, String phone, String createBy) {
        KeyHolder holder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "insert into biz_workorder(workorder_no,title,fault_desc,fault_level,status,source,service_type," +
                            "hospital_id,campus_id,department_id,equipment_id,engineer_id,report_contact,report_phone," +
                            "sla_deadline,create_by,create_time) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,date_add(now(), interval ? hour),?,now())",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, no);
            ps.setString(2, title);
            ps.setString(3, desc);
            ps.setString(4, level);
            ps.setString(5, status);
            ps.setString(6, source);
            ps.setString(7, serviceType);
            ps.setObject(8, hospitalId);
            ps.setObject(9, campusId);
            ps.setObject(10, deptId);
            ps.setObject(11, equipmentId);
            ps.setObject(12, engineerId);
            ps.setString(13, contact);
            ps.setString(14, phone);
            ps.setInt(15, slaHours(level));
            ps.setString(16, createBy);
            return ps;
        }, holder);
        return holder.getKey() == null ? 0L : holder.getKey().longValue();
    }

    private Map<String, Object> require(Long id) {
        List<Map<String, Object>> list = jdbc.queryForList("select * from biz_workorder where workorder_id=?", id);
        if (list.isEmpty()) throw BizException.badRequest("工单不存在");
        checkScope(list.get(0));
        return list.get(0);
    }

    private void checkAssignedEngineer(Map<String, Object> wo, String action) {
        Long engineerId = AuthContext.require().getEngineerId();
        Object assigned = wo.get("engineer_id");
        if (engineerId == null || assigned == null
                || !engineerId.equals(((Number) assigned).longValue())) {
            throw BizException.forbidden("只有接单工程师可以" + action);
        }
    }

    private void checkScope(Map<String, Object> wo) {
        LoginUser user = AuthContext.require();
        Object assigned = wo.get("engineer_id");
        boolean selfAssigned = "self".equals(user.getDataScope()) && user.getEngineerId() != null
                && assigned != null && user.getEngineerId().equals(((Number) assigned).longValue());
        Object hospital = wo.get("hospital_id");
        if (!selfAssigned && hospital != null && !dataScope.canAccessHospital(user, ((Number) hospital).longValue())) {
            throw BizException.forbidden("无权访问其他医院工单");
        }
        Long companyId = dataScope.companyId(user);
        if (companyId != null) {
            Object woCompany = wo.get("company_id");
            if (woCompany == null || !companyId.equals(((Number) woCompany).longValue())) {
                throw BizException.forbidden("无权访问其他公司工单");
            }
        }
        if (user.getEngineerId() != null && "self".equals(user.getDataScope())) {
            Object engineer = wo.get("engineer_id");
            if (engineer == null) {
                throw BizException.forbidden("工单尚未派工，不能操作");
            }
            if (!user.getEngineerId().equals(((Number) engineer).longValue())) {
                throw BizException.forbidden("只能操作本人负责的工单");
            }
        }
        if (user.isHospitalUser() && (user.getRoleKeys().contains("hospital_dept")
                || user.getRoleKeys().contains("hospital_sub_admin")) && user.getDepartmentId() != null) {
            Object dept = wo.get("department_id");
            if (dept == null || !user.getDepartmentId().equals(((Number) dept).longValue())) {
                throw BizException.forbidden("只能操作本科室的工单");
            }
        }
    }

    private void checkStatus(Map<String, Object> wo, String expect, String message) {
        if (!expect.equals(String.valueOf(wo.get("status")))) throw BizException.badRequest(message);
    }

    private void notify(Long woId, Long hospitalId, String title, String content, String roles, String level) {
        if (!sysConfig.bool("platform.notify.inAppEnabled", true)) return;
        jdbc.update("insert into biz_message(msg_type,title,content,level,biz_type,biz_id,biz_key,receiver_id," +
                        "receiver_role,hospital_id,company_id,is_read,create_time) " +
                        "values('workorder',?,?,?,'workorder',?,null,0,?,?,(select company_id from biz_workorder where workorder_id=?),'0',now())",
                title, content, level, woId, roles, hospitalId, woId);
    }

    private void assignCompany(Long id, Long hospitalId, Long deptId, Long equipmentId, Long campusId) {
        Long companyId = null;
        LoginUser actor = AuthContext.get();
        Long actorCompany = actor == null ? null : dataScope.companyId(actor);
        if (actorCompany != null) {
            companyId = actorCompany;
        }
        if (companyId == null && equipmentId != null) {
            List<Long> companies = jdbc.queryForList(
                    "select company_id from biz_equipment where equipment_id=?", Long.class, equipmentId);
            companyId = companies.isEmpty() ? null : companies.get(0);
        }
        if (companyId == null && campusId != null) {
            List<Long> campusCompanies = jdbc.queryForList(
                    "select company_id from biz_company_campus where campus_id=?", Long.class, campusId);
            if (campusCompanies.size() == 1) companyId = campusCompanies.get(0);
        }
        if (companyId == null) {
            companyId = contractScope.resolveCompany(hospitalId, deptId, equipmentId, campusId);
        }
        if (companyId == null) {
            LoginUser user = AuthContext.get();
            if (user != null) companyId = dataScope.companyId(user);
        }
        if (companyId != null) {
            jdbc.update("update biz_workorder set company_id=? where workorder_id=?", companyId, id);
        }
    }

    private void log(Long woId, String action, String from, String to, String content) {
        LoginUser user = AuthContext.get();
        jdbc.update("insert into biz_workorder_log(workorder_id,action,operator_id,operator_name,from_status,to_status,content,create_time) " +
                        "values(?,?,?,?,?,?,?,now())", woId, action,
                user == null ? null : user.getUserId(), user == null ? "系统" : user.getNickName(), from, to, content);
    }

    private String nextNo() {
        String day = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Integer max = jdbc.queryForObject(
                "select coalesce(max(cast(substring_index(workorder_no,'-',-1) as unsigned)),0) " +
                        "from biz_workorder where workorder_no like concat('WO-', ?, '-%')", Integer.class, day);
        return "WO-" + day + "-" + String.format("%03d", (max == null ? 0 : max) + 1);
    }

    private void validateEngineerRegion(Map<String, Object> wo, Long engineerId, String action) {
        Object hospitalObj = wo.get("hospital_id");
        if (hospitalObj == null) return;
        List<Long> hospitalRegions = jdbc.queryForList(
                "select region_id from biz_hospital where hospital_id=?", Long.class,
                ((Number) hospitalObj).longValue());
        Long workorderRegion = hospitalRegions.isEmpty() ? null : hospitalRegions.get(0);
        if (workorderRegion == null) return;
        List<Long> engineerRegions = jdbc.queryForList(
                "select region_id from biz_engineer where engineer_id=?", Long.class, engineerId);
        Long engineerRegion = engineerRegions.isEmpty() ? null : engineerRegions.get(0);
        if (engineerRegion == null || !workorderRegion.equals(engineerRegion)) {
            throw BizException.badRequest("该工程师不属于工单所在区域，不能" + action);
        }
    }

    private int slaHours(String level) {
        return switch (level) {
            case "0" -> 2;
            case "1" -> 8;
            default -> 24;
        };
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }

    private String engineerName(Long id) {
        List<String> names = jdbc.queryForList("select engineer_name from biz_engineer where engineer_id=?", String.class, id);
        return names.isEmpty() ? String.valueOf(id) : names.get(0);
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static Long longOf(Object o) {
        return o == null ? null : ((Number) o).longValue();
    }

    private static Long longOrNull(Object o) {
        if (o == null || "".equals(String.valueOf(o))) return null;
        return ((Number) o).longValue();
    }
}
