package com.laminar.ops.biz.service;

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

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmService {

    private final JdbcTemplate jdbc;
    private final DataScopeService dataScope;
    private final WorkorderService workorderService;
    private final ContractScopeService contractScope;

    /** 监测值越限：合并或新建告警。返回 1 表示新建。 */
    public int raise(Map<String, Object> point, double value, Double lo, Double hi) {
        String metric = String.valueOf(point.get("metric"));
        String level = defaultLevel(metric);
        Double threshold = (lo != null && value < lo) ? lo : hi;
        String content = point.get("equipment_name") + " " + point.get("point_name") + " 实测 " +
                trim(value) + point.get("unit") + "，" + (threshold != null && value < threshold ? "低于下限 " : "超过上限 ") +
                trim(threshold) + point.get("unit");
        return insertOrMerge(point, value, level, threshold, content, false);
    }

    /** 按告警规则触发：使用规则级别与文案 */
    public int raiseRule(Map<String, Object> point, double value, Map<String, Object> rule,
                         Double threshold, String content) {
        String level = rule.get("level") == null ? "3" : String.valueOf(rule.get("level"));
        boolean auto = "1".equals(String.valueOf(rule.get("auto_workorder")));
        return insertOrMerge(point, value, level, threshold, content, auto);
    }

    private int insertOrMerge(Map<String, Object> point, double value, String level,
                              Double threshold, String content, boolean autoWorkorder) {
        Long pointId = ((Number) point.get("point_id")).longValue();
        Long equipmentId = ((Number) point.get("equipment_id")).longValue();
        if (mergeOpen(pointId, value)) {
            return 0;
        }
        String metric = String.valueOf(point.get("metric"));
        String alarmNo = "AL-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
                "-" + ThreadLocalRandom.current().nextInt(100, 999);
        KeyHolder holder = new GeneratedKeyHolder();
        try {
            jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement(
                        "insert into biz_alarm(alarm_no,company_id,hospital_id,campus_id,area_id,department_id,equipment_id,point_id," +
                                "metric,level,alarm_value,threshold,content,status,first_time,last_time,duration,create_by,create_time) " +
                                "values(?,?,?,?,?,?,?,?,?,?,?,?,?,'0',now(),now(),0,'system',now())",
                        Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, alarmNo);
                ps.setObject(2, point.get("company_id"));
                ps.setObject(3, point.get("hospital_id"));
                ps.setObject(4, point.get("campus_id"));
                ps.setObject(5, point.get("area_id"));
                ps.setObject(6, point.get("department_id"));
                ps.setObject(7, equipmentId);
                ps.setObject(8, pointId);
                ps.setString(9, metric);
                ps.setString(10, level);
                ps.setDouble(11, value);
                ps.setObject(12, threshold);
                ps.setString(13, content);
                return ps;
            }, holder);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 并发下已有活动告警，合并即可
            mergeOpen(pointId, value);
            return 0;
        }
        Long alarmId = holder.getKey() == null ? 0L : holder.getKey().longValue();
        log(alarmId, "create", null, "0", "监测数据越限，自动生成告警");
        insertMessage(alarmId, point, content, level);
        if ("1".equals(level)) {
            jdbc.update("update biz_equipment set run_status='2' where equipment_id=?", equipmentId);
        }
        if (autoWorkorder && alarmId > 0) {
            try {
                workorderService.fromAlarmSystem(alarmId);
            } catch (Exception e) {
                log.warn("[告警] 自动转工单失败 alarmId={}: {}", alarmId, e.getMessage());
            }
        }
        return 1;
    }

    private boolean mergeOpen(Long pointId, double value) {
        List<Map<String, Object>> open = jdbc.queryForList(
                "select alarm_id from biz_alarm where point_id = ? and status in ('0','1','2') order by alarm_id desc limit 1", pointId);
        if (open.isEmpty()) return false;
        Long alarmId = ((Number) open.get(0).get("alarm_id")).longValue();
        jdbc.update("update biz_alarm set last_time = now(), duration = timestampdiff(second, first_time, now()), " +
                "alarm_value = ? where alarm_id = ?", value, alarmId);
        return true;
    }

    /** 指标恢复正常：自动关闭该点位活动告警 */
    public int recover(Long pointId) {
        List<Map<String, Object>> active = jdbc.queryForList(
                "select alarm_id, status, equipment_id from biz_alarm where point_id=? and status in ('0','1','2')", pointId);
        int n = 0;
        for (Map<String, Object> alarm : active) {
            Long alarmId = ((Number) alarm.get("alarm_id")).longValue();
            String from = String.valueOf(alarm.get("status"));
            jdbc.update("update biz_alarm set status='3', close_time=now(), close_by='system', " +
                            "root_cause='指标恢复正常', solution='系统自动关闭', duration=timestampdiff(second, first_time, now()) " +
                            "where alarm_id=? and status in ('0','1','2')", alarmId);
            log(alarmId, "auto_close", from, "3", "指标恢复正常，系统自动关闭");
            n++;
        }
        if (n > 0) {
            Long equipmentId = ((Number) active.get(0).get("equipment_id")).longValue();
            restoreRunStatus(equipmentId);
        }
        return n;
    }

    /** 设备离线告警 */
    public int raiseOffline(Map<String, Object> equipment, int offlineSeconds) {
        Long equipmentId = ((Number) equipment.get("equipment_id")).longValue();
        Integer exists = jdbc.queryForObject(
                "select count(*) from biz_alarm where equipment_id=? and metric='offline' and status in ('0','1','2')",
                Integer.class, equipmentId);
        if (exists != null && exists > 0) {
            jdbc.update("update biz_alarm set last_time=now(), duration=timestampdiff(second, first_time, now()) " +
                    "where equipment_id=? and metric='offline' and status in ('0','1','2')", equipmentId);
            return 0;
        }
        String content = "设备离线：" + equipment.get("equipment_name") + "（" + equipment.get("equipment_code") +
                "）已超过 " + offlineSeconds + " 秒未上报心跳";
        String alarmNo = "AL-OFF-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
                "-" + ThreadLocalRandom.current().nextInt(100, 999);
        KeyHolder holder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "insert into biz_alarm(alarm_no,company_id,hospital_id,campus_id,area_id,department_id,equipment_id,point_id," +
                            "metric,level,content,status,first_time,last_time,duration,create_by,create_time) " +
                            "values(?,?,?,?,?,?,?,null,'offline','1',?,'0',now(),now(),0,'system',now())",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, alarmNo);
            ps.setObject(2, equipment.get("company_id"));
            ps.setObject(3, equipment.get("hospital_id"));
            ps.setObject(4, equipment.get("campus_id"));
            ps.setObject(5, equipment.get("area_id"));
            ps.setObject(6, equipment.get("department_id"));
            ps.setObject(7, equipmentId);
            ps.setString(8, content);
            return ps;
        }, holder);
        Long alarmId = holder.getKey() == null ? 0L : holder.getKey().longValue();
        log(alarmId, "create", null, "0", "设备心跳超时，自动生成离线告警");
        insertMessage(alarmId, equipment, content, "1");
        return 1;
    }

    public int recoverOffline(Long equipmentId) {
        List<Map<String, Object>> active = jdbc.queryForList(
                "select alarm_id, status from biz_alarm where equipment_id=? and metric='offline' and status in ('0','1','2')",
                equipmentId);
        for (Map<String, Object> alarm : active) {
            Long alarmId = ((Number) alarm.get("alarm_id")).longValue();
            jdbc.update("update biz_alarm set status='3', close_time=now(), close_by='system', " +
                            "root_cause='设备恢复心跳', solution='系统自动关闭', duration=timestampdiff(second, first_time, now()) " +
                            "where alarm_id=?", alarmId);
            log(alarmId, "auto_close", String.valueOf(alarm.get("status")), "3", "设备恢复心跳，自动关闭");
        }
        return active.size();
    }

    private void restoreRunStatus(Long equipmentId) {
        Integer open = jdbc.queryForObject(
                "select count(*) from biz_alarm where equipment_id=? and level='1' and status in ('0','1','2')",
                Integer.class, equipmentId);
        if (open != null && open == 0) {
            jdbc.update("update biz_equipment set run_status='1' where equipment_id=?", equipmentId);
        }
    }

    private String defaultLevel(String metric) {
        return switch (metric) {
            case "pressure", "oxygen_pressure", "vacuum", "offline" -> "1";
            case "temperature", "particle" -> "2";
            default -> "3";
        };
    }

    public Map<String, Object> page(int pageNum, int pageSize, Map<String, String> params) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" where 1=1 ");
        String level = params.get("level");
        String status = params.get("status");
        String hospitalId = params.get("hospitalId");
        String equipmentId = params.get("equipmentId");
        String keyword = params.get("keyword");
        if (level != null && !level.isBlank()) { where.append(" and a.level = ? "); args.add(level); }
        if (status != null && !status.isBlank()) {
            if ("open".equals(status)) where.append(" and a.status in ('0','1','2') ");
            else { where.append(" and a.status = ? "); args.add(status); }
        }
        if (hospitalId != null && !hospitalId.isBlank()) { where.append(" and a.hospital_id = ? "); args.add(hospitalId); }
        if (equipmentId != null && !equipmentId.isBlank()) { where.append(" and a.equipment_id = ? "); args.add(equipmentId); }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" and (a.alarm_no like ? or a.content like ?) ");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
        }
        LoginUser user = AuthContext.require();
        java.util.List<Long> allowed = dataScope.visibleHospitalIds(user);
        if (allowed != null) {
            if (allowed.isEmpty()) {
                where.append(" and 1=0 ");
            } else {
                where.append(" and a.hospital_id in (")
                        .append(allowed.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")))
                        .append(") ");
            }
        }
        if (user.isHospitalUser() && user.getRoleKeys().contains("hospital_dept") && user.getDepartmentId() != null) {
            where.append(" and a.department_id = ? ");
            args.add(user.getDepartmentId());
        }
        Long companyId = dataScope.companyId(user);
        if (companyId != null) {
            where.append(" and a.company_id = ? ");
            args.add(companyId);
        }
        Long total = jdbc.queryForObject("select count(*) from biz_alarm a" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add(Math.max(0, (pageNum - 1) * pageSize));
        List<Map<String, Object>> records = jdbc.queryForList(
                "select a.alarm_id as `alarmId`, a.alarm_no as `alarmNo`, a.equipment_id as `equipmentId`, " +
                        "a.metric, a.level, a.alarm_value as `alarmValue`, a.threshold, a.content, a.status, " +
                        "date_format(a.first_time,'%Y-%m-%d %H:%i:%s') as `firstTime`, " +
                        "date_format(a.last_time,'%Y-%m-%d %H:%i:%s') as `lastTime`, a.duration, " +
                        "a.confirm_by as `confirmBy`, date_format(a.confirm_time,'%Y-%m-%d %H:%i:%s') as `confirmTime`, " +
                        "a.close_by as `closeBy`, a.root_cause as `rootCause`, a.solution, a.workorder_id as `workorderId`, " +
                        "e.equipment_name as `equipmentName`, h.hospital_name as `hospitalName`, " +
                        "ar.area_name as `areaName`, c.campus_name as `campusName` " +
                        "from biz_alarm a left join biz_equipment e on e.equipment_id=a.equipment_id " +
                        "left join biz_hospital h on h.hospital_id=a.hospital_id " +
                        "left join biz_clean_area ar on ar.area_id=a.area_id " +
                        "left join biz_campus c on c.campus_id=a.campus_id " +
                        where + " order by a.first_time desc limit ? offset ?", pageArgs.toArray());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total == null ? 0 : total);
        return result;
    }

    public Map<String, Object> detail(Long id) {
        Map<String, Object> alarm = jdbc.queryForMap(
                "select a.*, e.equipment_name, e.equipment_code, h.hospital_name, ar.area_name, c.campus_name " +
                        "from biz_alarm a left join biz_equipment e on e.equipment_id=a.equipment_id " +
                        "left join biz_hospital h on h.hospital_id=a.hospital_id " +
                        "left join biz_clean_area ar on ar.area_id=a.area_id " +
                        "left join biz_campus c on c.campus_id=a.campus_id where a.alarm_id=?", id);
        LoginUser user = AuthContext.require();
        Object alarmHospital = alarm.get("hospital_id");
        if (alarmHospital != null && !dataScope.canAccessHospital(user, ((Number) alarmHospital).longValue())) {
            throw BizException.forbidden("无权查看其他医院告警");
        }
        checkCompany(user, alarm);
        List<Map<String, Object>> logs = jdbc.queryForList(
                "select action, operator_name as `operatorName`, from_status as `fromStatus`, to_status as `toStatus`, " +
                        "content, date_format(create_time,'%Y-%m-%d %H:%i:%s') as `createTime` " +
                        "from biz_alarm_log where alarm_id=? order by log_id", id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("alarm", alarm);
        result.put("logs", logs);
        return result;
    }

    public void confirm(Long id, String remark) {
        Map<String, Object> a = getAlarm(id);
        String from = String.valueOf(a.get("status"));
        if (!"0".equals(from)) {
            throw BizException.badRequest("只有未确认告警可以确认");
        }
        jdbc.update("update biz_alarm set status='1', confirm_by=?, confirm_time=now() where alarm_id=?",
                AuthContext.nickName(), id);
        log(id, "confirm", from, "1", remark == null || remark.isBlank() ? "确认告警" : remark);
    }

    public void handle(Long id, String remark) {
        Map<String, Object> a = getAlarm(id);
        String from = String.valueOf(a.get("status"));
        if (!"0".equals(from) && !"1".equals(from)) {
            throw BizException.badRequest("当前状态不能开始处理");
        }
        jdbc.update("update biz_alarm set status='2', confirm_by=coalesce(confirm_by,?), confirm_time=coalesce(confirm_time,now()), " +
                "handle_by=?, handle_time=now() where alarm_id=?", AuthContext.nickName(), AuthContext.nickName(), id);
        log(id, "handle", from, "2", remark == null || remark.isBlank() ? "开始处理" : remark);
    }

    public void close(Long id, String rootCause, String solution) {
        if (rootCause == null || rootCause.isBlank()) {
            throw BizException.badRequest("请填写根本原因");
        }
        Map<String, Object> a = getAlarm(id);
        String from = String.valueOf(a.get("status"));
        if (!List.of("0", "1", "2").contains(from)) {
            throw BizException.badRequest("当前状态不能消缺");
        }
        jdbc.update("update biz_alarm set status='3', close_by=?, close_time=now(), root_cause=?, solution=? where alarm_id=?",
                AuthContext.nickName(), rootCause, solution, id);
        log(id, "close", from, "3", rootCause + (solution == null ? "" : "；" + solution));
        Long equipmentId = ((Number) a.get("equipment_id")).longValue();
        Integer open = jdbc.queryForObject("select count(*) from biz_alarm where equipment_id=? and level='1' and status in ('0','1','2')",
                Integer.class, equipmentId);
        if (open == null || open == 0) {
            jdbc.update("update biz_equipment set run_status='0' where equipment_id=?", equipmentId);
        }
    }

    public void ignore(Long id, String reason) {
        Map<String, Object> a = getAlarm(id);
        String from = String.valueOf(a.get("status"));
        if (!List.of("0", "1", "2").contains(from)) {
            throw BizException.badRequest("当前状态不能忽略");
        }
        jdbc.update("update biz_alarm set status='4', close_by=?, close_time=now(), remark=? where alarm_id=?",
                AuthContext.nickName(), reason, id);
        log(id, "ignore", from, "4", reason == null ? "忽略告警" : reason);
    }

    private Map<String, Object> getAlarm(Long id) {
        List<Map<String, Object>> list = jdbc.queryForList("select * from biz_alarm where alarm_id=?", id);
        if (list.isEmpty()) {
            throw BizException.badRequest("告警不存在");
        }
        Map<String, Object> alarm = list.get(0);
        LoginUser user = AuthContext.require();
        Object hospital = alarm.get("hospital_id");
        if (hospital != null && !dataScope.canAccessHospital(user, ((Number) hospital).longValue())) {
            throw BizException.forbidden("无权操作其他医院告警");
        }
        checkCompany(user, alarm);
        return alarm;
    }

    private void checkCompany(LoginUser user, Map<String, Object> alarm) {
        Long companyId = dataScope.companyId(user);
        if (companyId == null) return;
        Object alarmCompany = alarm.get("company_id");
        if (alarmCompany == null || !companyId.equals(((Number) alarmCompany).longValue())) {
            throw BizException.forbidden("无权访问其他公司告警");
        }
    }

    private void log(Long alarmId, String action, String from, String to, String content) {
        LoginUser user = AuthContext.get();
        jdbc.update("insert into biz_alarm_log(alarm_id,action,operator_id,operator_name,from_status,to_status,content,create_time) " +
                        "values(?,?,?,?,?,?,?,now())", alarmId, action,
                user == null ? null : user.getUserId(), user == null ? "系统" : user.getNickName(), from, to, content);
    }

    private void insertMessage(Long alarmId, Map<String, Object> point, String content, String level) {
        Long companyId = null;
        if (point.get("equipment_id") != null) {
            java.util.List<Long> companies = jdbc.queryForList(
                    "select company_id from biz_equipment where equipment_id=?", Long.class,
                    ((Number) point.get("equipment_id")).longValue());
            companyId = companies.isEmpty() ? null : companies.get(0);
        }
        if (companyId == null) {
            companyId = contractScope.resolveCompany(
                    point.get("hospital_id") == null ? null : ((Number) point.get("hospital_id")).longValue(),
                    point.get("department_id") == null ? null : ((Number) point.get("department_id")).longValue(),
                    point.get("area_id") == null ? null : ((Number) point.get("area_id")).longValue(),
                    point.get("equipment_id") == null ? null : ((Number) point.get("equipment_id")).longValue());
        }
        if (companyId == null && point.get("hospital_id") != null) {
            companyId = contractScope.uniqueActiveCompany(((Number) point.get("hospital_id")).longValue());
        }
        jdbc.update("insert ignore into biz_message(msg_type,title,content,level,biz_type,biz_id,biz_key,receiver_id," +
                        "receiver_role,hospital_id,company_id,is_read,create_time) values('alarm',?,?,?,'alarm',?,?,0,'100,101,200,201',?,?,'0',now())",
                "设备告警：" + point.get("equipment_name"), content, level, alarmId,
                "alarm:" + alarmId + ":create", point.get("hospital_id"), companyId);
    }

    private String trim(Object v) {
        if (v == null) return "";
        String s = String.valueOf(v);
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }
}
