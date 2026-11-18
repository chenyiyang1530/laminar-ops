package com.laminar.ops.biz.service;

import com.laminar.ops.common.BizException;
import com.laminar.ops.security.AuthContext;
import com.laminar.ops.security.DataScopeService;
import com.laminar.ops.security.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
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
public class MonitorService {

    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;
    private final AlarmService alarmService;
    private final DataScopeService dataScope;
    private final ContractScopeService contractScope;

    private volatile long lastHistoryWrite = 0;
    private final Map<Long, Integer> normalStreak = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, Integer> breachStreak = new java.util.concurrent.ConcurrentHashMap<>();

    private static final String POINT_SQL =
            "select p.point_id, p.equipment_id, p.point_name, p.metric, p.unit, p.precision, " +
                    "p.warn_lower, p.warn_upper, p.alarm_lower, p.alarm_upper, " +
                    "e.equipment_name, e.equipment_code, e.hospital_id, e.campus_id, e.department_id, " +
                    "e.device_type_id, e.company_id " +
                    "from biz_monitor_point p join biz_equipment e on e.equipment_id = p.equipment_id " +
                    "where p.enabled = '1' and e.status = '0' and e.simulated = '1'";

    /** 定时采集：生成模拟值、写 Redis 最新值、落历史、触发规则告警与自动恢复 */
    public int collect() {
        List<Map<String, Object>> points = jdbc.queryForList(POINT_SQL);
        List<Map<String, Object>> rules = activeRules();
        LocalDateTime now = LocalDateTime.now();
        List<Object[]> history = new ArrayList<>();
        boolean writeHistory = System.currentTimeMillis() - lastHistoryWrite > 4 * 60 * 1000;
        int alarms = 0;
        java.util.Set<Long> equipmentIds = new java.util.LinkedHashSet<>();
        for (Map<String, Object> p : points) {
            String metric = str(p.get("metric"));
            int precision = num(p.get("precision"), 1);
            double value = sanitize(metric, simulate(metric, p));
            value = round(value, precision);
            Long equipmentId = ((Number) p.get("equipment_id")).longValue();
            Long pointId = ((Number) p.get("point_id")).longValue();
            equipmentIds.add(equipmentId);
            String field = String.valueOf(pointId);
            String payload = value + "|" + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            redis.opsForHash().put("monitor:latest:" + equipmentId, field, payload);
            redis.expire("monitor:latest:" + equipmentId, Duration.ofHours(24));

            EvalResult eval = evaluateAndRaise(p, value, rulesFor(p, rules));
            alarms += eval.raised();
            if (writeHistory) {
                history.add(new Object[]{equipmentId, pointId, metric, value, eval.breach() ? "2" : "0", now});
            }
        }
        if (writeHistory && !history.isEmpty()) {
            jdbc.batchUpdate("insert into biz_monitor_data(equipment_id, point_id, metric, value, status, collect_time) " +
                    "values(?,?,?,?,?,?)", history);
            lastHistoryWrite = System.currentTimeMillis();
        }
        for (Long equipmentId : equipmentIds) {
            jdbc.update("update biz_equipment set last_heartbeat=now(), monitor_status='1' where equipment_id=?", equipmentId);
            alarmService.recoverOffline(equipmentId);
        }
        log.info("[采集] points={}, alarms={}, history={}", points.size(), alarms, writeHistory);
        return points.size();
    }

    /** 离线检测：按公司/设备类型的心跳规则判断，无规则不产生离线告警 */
    public int checkOffline() {
        List<Map<String, Object>> rules = jdbc.queryForList(
                "select company_id, device_type_id, duration from biz_alarm_rule " +
                        "where metric='heartbeat' and enabled='1' order by rule_id");
        if (rules.isEmpty()) return 0;
        java.util.Set<Long> handled = new java.util.HashSet<>();
        int count = 0;
        for (Map<String, Object> rule : rules) {
            if (rule.get("company_id") == null) continue;
            int seconds = rule.get("duration") == null ? 120 : ((Number) rule.get("duration")).intValue();
            if (seconds <= 0) seconds = 120;
            StringBuilder sql = new StringBuilder(
                    "select e.equipment_id, e.equipment_name, e.equipment_code, e.company_id, e.hospital_id, " +
                            "e.campus_id, e.department_id from biz_equipment e " +
                            "where e.status='0' and e.monitor_status='1' and e.last_heartbeat is not null " +
                            "and exists (select 1 from biz_monitor_point p where p.equipment_id=e.equipment_id and p.enabled='1') " +
                            "and e.last_heartbeat < date_sub(now(), interval " + seconds + " second)");
            List<Object> args = new ArrayList<>();
            if (rule.get("company_id") != null) {
                sql.append(" and e.company_id=?");
                args.add(((Number) rule.get("company_id")).longValue());
            }
            if (rule.get("device_type_id") != null) {
                sql.append(" and e.device_type_id=?");
                args.add(((Number) rule.get("device_type_id")).longValue());
            }
            for (Map<String, Object> eq : jdbc.queryForList(sql.toString(), args.toArray())) {
                Long equipmentId = ((Number) eq.get("equipment_id")).longValue();
                if (!handled.add(equipmentId)) continue;
                jdbc.update("update biz_equipment set monitor_status='0' where equipment_id=?", equipmentId);
                alarmService.raiseOffline(eq, seconds);
                count++;
            }
        }
        if (count > 0) log.info("[离线] 标记 {} 台设备离线", count);
        return count;
    }

    private int offlineSeconds() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select duration from biz_alarm_rule where metric='heartbeat' and enabled='1' order by rule_id limit 1");
        if (rows.isEmpty()) return 120;
        Integer d = rows.get(0).get("duration") == null ? null : ((Number) rows.get(0).get("duration")).intValue();
        return d == null || d <= 0 ? 120 : d;
    }

    /** 首次启动回填 24 小时历史数据，保证曲线可用 */
    public void backfillIfEmpty() {
        Integer n = jdbc.queryForObject("select count(*) from biz_monitor_data", Integer.class);
        if (n != null && n > 0) {
            return;
        }
        List<Map<String, Object>> points = jdbc.queryForList(POINT_SQL);
        if (points.isEmpty()) {
            return;
        }
        LocalDateTime start = LocalDateTime.now().minusHours(24);
        List<Object[]> batch = new ArrayList<>();
        for (int step = 0; step < 288; step++) {
            LocalDateTime t = start.plusMinutes(step * 5L);
            for (Map<String, Object> p : points) {
                String metric = str(p.get("metric"));
                double v = round(sanitize(metric, simulate(metric, p)), num(p.get("precision"), 1));
                batch.add(new Object[]{((Number) p.get("equipment_id")).longValue(),
                        ((Number) p.get("point_id")).longValue(), metric, v, "0", t});
            }
        }
        for (int i = 0; i < batch.size(); i += 500) {
            jdbc.batchUpdate("insert into biz_monitor_data(equipment_id, point_id, metric, value, status, collect_time) " +
                    "values(?,?,?,?,?,?)", batch.subList(i, Math.min(i + 500, batch.size())));
        }
        log.info("[采集] 回填 24h 历史数据 {} 条", batch.size());
    }

    public Map<String, Object> overview(List<Long> hospitalIds, Long campusId, Long departmentId) {
        String hf = filter("e.hospital_id", hospitalIds);
        String hfAlarm = filter("hospital_id", hospitalIds);
        if (campusId != null) {
            hf += " and e.campus_id=" + campusId + " ";
            hfAlarm += " and campus_id=" + campusId + " ";
        }
        if (departmentId != null) {
            hf += " and e.department_id=" + departmentId + " ";
            hfAlarm += " and department_id=" + departmentId + " ";
        }
        LoginUser current = AuthContext.get();
        Long companyId = current == null ? null : dataScope.companyId(current);
        if (companyId != null) {
            List<Long> equipmentIds = contractScope.equipmentIdsForCompany(companyId);
            if (equipmentIds.isEmpty()) {
                hf += " and 1=0 ";
                hfAlarm += " and 1=0 ";
            } else {
                String ids = equipmentIds.stream().map(String::valueOf)
                        .collect(java.util.stream.Collectors.joining(","));
                hf += " and e.equipment_id in (" + ids + ") ";
                hfAlarm += " and equipment_id in (" + ids + ") ";
            }
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", count("select count(*) from biz_equipment e where e.status='0' and e.monitor_status='1'" + hf));
        summary.put("offline", count("select count(*) from biz_equipment e where e.status='0' and e.monitor_status<>'1'" + hf));
        summary.put("alarmOpen", count("select count(*) from biz_alarm where status in ('0','1','2')" + hfAlarm));

        List<Map<String, Object>> rows = jdbc.queryForList(
                "select e.equipment_id, e.equipment_code, e.equipment_name, e.run_status, e.monitor_status, " +
                        "e.last_heartbeat, h.hospital_name, c.campus_name, d.department_name, t.type_name " +
                        "from biz_equipment e " +
                        "left join biz_hospital h on h.hospital_id = e.hospital_id " +
                        "left join biz_campus c on c.campus_id = e.campus_id " +
                        "left join biz_department d on d.department_id = e.department_id " +
                        "left join biz_device_type t on t.type_id = e.device_type_id " +
                        "where e.status='0' " + hf + " order by e.monitor_status, e.equipment_id");
        List<Map<String, Object>> pointRows = jdbc.queryForList(
                "select point_id, equipment_id, point_name, metric, unit from biz_monitor_point where enabled='1' order by sort, point_id");
        Map<Long, List<Map<String, Object>>> pointsByEquip = new LinkedHashMap<>();
        for (Map<String, Object> p : pointRows) {
            long eid = ((Number) p.get("equipment_id")).longValue();
            pointsByEquip.computeIfAbsent(eid, k -> new ArrayList<>()).add(p);
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Long equipmentId = ((Number) row.get("equipment_id")).longValue();
            Map<Object, Object> latest = redis.opsForHash().entries("monitor:latest:" + equipmentId);
            List<Map<String, Object>> metrics = new ArrayList<>();
            String lastTime = "";
            for (Map<String, Object> p : pointsByEquip.getOrDefault(equipmentId, List.of())) {
                long pointId = ((Number) p.get("point_id")).longValue();
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("pointId", pointId);
                m.put("name", p.get("point_name"));
                m.put("metric", p.get("metric"));
                m.put("unit", p.get("unit"));
                Object v = latest.get(String.valueOf(pointId));
                if (v != null) {
                    String[] parts = String.valueOf(v).split("\\|");
                    m.put("value", parts[0]);
                    m.put("time", parts.length > 1 ? parts[1] : "");
                    lastTime = parts.length > 1 ? parts[1] : lastTime;
                }
                metrics.add(m);
            }
            row.put("metrics", metrics);
            row.put("lastTime", lastTime);
            list.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", summary);
        result.put("list", list);
        return result;
    }

    public Map<String, Object> equipmentDetail(Long equipmentId) {
        Map<String, Object> equipment = jdbc.queryForMap(
                "select e.*, h.hospital_name, c.campus_name, d.department_name, t.type_name " +
                        "from biz_equipment e left join biz_hospital h on h.hospital_id=e.hospital_id " +
                        "left join biz_campus c on c.campus_id=e.campus_id " +
                        "left join biz_department d on d.department_id=e.department_id " +
                        "left join biz_device_type t on t.type_id=e.device_type_id where e.equipment_id=?", equipmentId);
        List<Map<String, Object>> points = jdbc.queryForList(
                "select * from biz_monitor_point where equipment_id=? and enabled='1' order by sort, point_id", equipmentId);
        Map<Object, Object> latest = redis.opsForHash().entries("monitor:latest:" + equipmentId);
        for (Map<String, Object> p : points) {
            Object v = latest.get(String.valueOf(p.get("point_id")));
            if (v != null) {
                String[] parts = String.valueOf(v).split("\\|");
                p.put("latestValue", parts[0]);
                p.put("latestTime", parts.length > 1 ? parts[1] : "");
            }
            Long pointId = ((Number) p.get("point_id")).longValue();
            p.put("history", jdbc.queryForList(
                    "select date_format(collect_time,'%H:%i') as t, value from biz_monitor_data " +
                            "where point_id=? and collect_time >= date_sub(now(), interval 24 hour) order by collect_time", pointId));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("equipment", equipment);
        result.put("points", points);
        return result;
    }

    public List<Map<String, Object>> history(Long pointId, int hours) {
        if (hours <= 0 || hours > 168) hours = 24;
        return jdbc.queryForList("select collect_time as collectTime, value, status from biz_monitor_data " +
                "where point_id=? and collect_time >= date_sub(now(), interval " + hours + " hour) order by collect_time", pointId);
    }

    /** 网关上报：按设备编码 + 点位编码写入实时值/历史并判定告警 */
    public int report(String equipmentCode, String pointCode, double value) {
        return report(equipmentCode, pointCode, value, LocalDateTime.now());
    }

    public int report(String equipmentCode, String pointCode, double value, LocalDateTime collectedAt) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select p.point_id, p.equipment_id, p.point_name, p.metric, p.unit, p.precision, " +
                        "p.warn_lower, p.warn_upper, p.alarm_lower, p.alarm_upper, " +
                        "e.equipment_name, e.hospital_id, e.campus_id, e.department_id, e.device_type_id, e.company_id " +
                        "from biz_monitor_point p join biz_equipment e on e.equipment_id = p.equipment_id " +
                        "where e.equipment_code = ? and p.point_code = ?", equipmentCode, pointCode);
        if (rows.isEmpty()) {
            throw BizException.badRequest("设备或点位不存在：" + equipmentCode + "/" + pointCode);
        }
        Map<String, Object> p = rows.get(0);
        qualityCheck(str(p.get("metric")), value);
        Long equipmentId = ((Number) p.get("equipment_id")).longValue();
        Long pointId = ((Number) p.get("point_id")).longValue();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime at = collectedAt == null || collectedAt.isAfter(now.plusMinutes(5)) ? now : collectedAt;
        boolean live = !at.isBefore(now.minusMinutes(5));
        String ts = at.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        EvalResult eval = new EvalResult(0, false);
        if (live) {
            redis.opsForHash().put("monitor:latest:" + equipmentId, String.valueOf(pointId), value + "|" + ts);
            redis.expire("monitor:latest:" + equipmentId, Duration.ofHours(24));
            jdbc.update("update biz_equipment set last_heartbeat=now(), monitor_status='1' where equipment_id=?", equipmentId);
            alarmService.recoverOffline(equipmentId);
            eval = evaluateAndRaise(p, value, rulesFor(p, activeRules()));
        }
        jdbc.update("insert into biz_monitor_data(equipment_id,point_id,metric,value,status,collect_time) values(?,?,?,?,?,?)",
                equipmentId, pointId, p.get("metric"), value, eval.breach() ? "2" : "0", at);
        return 1;
    }

    /** 测试上报前的规则预览：不写数据、不触发告警。 */
    public Map<String, Object> preview(String equipmentCode, String pointCode, double value) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select p.point_id, p.point_name, p.metric, p.unit, p.precision, " +
                        "p.warn_lower, p.warn_upper, p.alarm_lower, p.alarm_upper, " +
                        "e.equipment_name, e.company_id, e.device_type_id " +
                        "from biz_monitor_point p join biz_equipment e on e.equipment_id=p.equipment_id " +
                        "where e.equipment_code=? and p.point_code=?", equipmentCode, pointCode);
        if (rows.isEmpty()) throw BizException.badRequest("设备或点位不存在：" + equipmentCode + "/" + pointCode);
        Map<String, Object> p = rows.get(0);
        Double lo = dbl(p.get("alarm_lower"));
        Double hi = dbl(p.get("alarm_upper"));
        boolean pointBreach = (lo != null && value < lo) || (hi != null && value > hi);
        Map<String, Object> rule = matchRule(rulesFor(p, activeRules()), p);
        boolean ruleViolated = rule != null && violates(rule, value);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("breach", pointBreach || ruleViolated);
        result.put("ruleMatched", rule != null);
        result.put("ruleName", rule == null ? "" : str(rule.get("rule_name")));
        result.put("duration", rule == null ? 0 : num(rule.get("duration"), 0));
        result.put("source", ruleViolated ? "rule" : pointBreach ? "point" : "normal");
        result.put("message", ruleViolated
                ? "命中告警规则[" + rule.get("rule_name") + "]，持续 " + num(rule.get("duration"), 0) + " 秒后触发"
                : pointBreach ? "超出点位告警范围" : "未触发点位阈值或告警规则");
        result.put("unit", str(p.get("unit")));
        return result;
    }

    private List<Map<String, Object>> activeRules() {
        return jdbc.queryForList(
                "select * from biz_alarm_rule where enabled='1' and metric <> 'heartbeat' order by rule_id");
    }

    /** 规则只对设备所属公司生效；无公司归属时仅使用全局规则 */
    private List<Map<String, Object>> rulesFor(Map<String, Object> p, List<Map<String, Object>> rules) {
        Long companyId = number(p.get("company_id"));
        return rules.stream().filter(r -> {
            Object ruleCompany = r.get("company_id");
            return companyId != null && ruleCompany != null
                    && companyId.equals(((Number) ruleCompany).longValue());
        }).toList();
    }

    private Long number(Object v) {
        return v == null ? null : ((Number) v).longValue();
    }

    private EvalResult evaluateAndRaise(Map<String, Object> p, double value, List<Map<String, Object>> rules) {
        Long pointId = ((Number) p.get("point_id")).longValue();
        Double lo = dbl(p.get("alarm_lower"));
        Double hi = dbl(p.get("alarm_upper"));
        boolean pointBreach = (lo != null && value < lo) || (hi != null && value > hi);
        Map<String, Object> rule = matchRule(rules, p);
        RuleEval eval = rule == null ? null : evalRule(rule, p, value);
        boolean ruleViolated = eval != null && eval.violated();
        boolean breach = ruleViolated || pointBreach;
        if (breach) {
            normalStreak.remove(pointId);
            int raised = 0;
            if (ruleViolated) {
                if (eval.triggered()) {
                    raised = alarmService.raiseRule(p, value, rule, eval.threshold(), eval.content());
                }
            } else if (pointBreach) {
                raised = alarmService.raise(p, value, lo, hi);
            }
            return new EvalResult(raised, true);
        }
        int streak = normalStreak.merge(pointId, 1, Integer::sum);
        if (streak >= 3) {
            alarmService.recover(pointId);
            normalStreak.remove(pointId);
        }
        return new EvalResult(0, false);
    }

    private Map<String, Object> matchRule(List<Map<String, Object>> rules, Map<String, Object> p) {
        Long equipmentId = ((Number) p.get("equipment_id")).longValue();
        Long deviceTypeId = p.get("device_type_id") == null ? null : ((Number) p.get("device_type_id")).longValue();
        String metric = str(p.get("metric"));
        for (Map<String, Object> rule : rules) {
            Object eq = rule.get("equipment_id");
            if (eq != null && metric.equals(str(rule.get("metric")))
                    && equipmentId.equals(((Number) eq).longValue())) {
                return rule;
            }
        }
        for (Map<String, Object> rule : rules) {
            Object type = rule.get("device_type_id");
            Object eq = rule.get("equipment_id");
            if (eq == null && type != null && deviceTypeId != null && metric.equals(str(rule.get("metric")))
                    && deviceTypeId.equals(((Number) type).longValue())) {
                return rule;
            }
        }
        for (Map<String, Object> rule : rules) {
            if (rule.get("equipment_id") == null && rule.get("device_type_id") == null
                    && metric.equals(str(rule.get("metric")))) {
                return rule;
            }
        }
        return null;
    }

    private RuleEval evalRule(Map<String, Object> rule, Map<String, Object> p, double value) {
        boolean violated = violates(rule, value);
        String key = rule.get("rule_id") + "#" + p.get("point_id");
        boolean triggered = false;
        if (violated) {
            int duration = num(rule.get("duration"), 0);
            int need = Math.max(1, (int) Math.ceil(duration / 30.0));
            int count = breachStreak.merge(key, 1, Integer::sum);
            triggered = count >= need;
        } else {
            breachStreak.remove(key);
        }
        Double threshold = dbl(rule.get("threshold"));
        Double threshold2 = dbl(rule.get("threshold2"));
        String op = str(rule.get("operator"));
        String content;
        if ("between".equals(op) && threshold != null && threshold2 != null) {
            String unit = str(p.get("unit"));
            String direction = value < threshold
                    ? "低于下限 " + trim(threshold) + unit
                    : "高于上限 " + trim(threshold2) + unit;
            threshold = value < threshold ? threshold : threshold2;
            content = "规则[" + rule.get("rule_name") + "]：" + p.get("equipment_name") + " " + p.get("point_name") +
                    " 实测 " + trim(value) + unit + "，" + direction + "（允许范围 " +
                    trim(threshold2 == null ? threshold : dbl(rule.get("threshold"))) + "~" + trim(threshold2) + unit + "）";
        } else {
            content = "规则[" + rule.get("rule_name") + "]：" + p.get("equipment_name") + " " + p.get("point_name") +
                    " 实测 " + trim(value) + str(p.get("unit")) + "，" + opText(op) + " " + trim(threshold) + str(p.get("unit"));
        }
        return new RuleEval(violated, triggered, threshold, content);
    }

    private boolean violates(Map<String, Object> rule, double value) {
        Double t = dbl(rule.get("threshold"));
        Double t2 = dbl(rule.get("threshold2"));
        String op = str(rule.get("operator"));
        if (t == null && !"between".equals(op)) return false;
        return switch (op) {
            case "gt" -> value > t;
            case "gte", "ge" -> value >= t;
            case "lt" -> value < t;
            case "lte", "le" -> value <= t;
            case "eq" -> Math.abs(value - t) < 1e-6;
            case "between" -> t != null && t2 != null && (value < t || value > t2);
            default -> false;
        };
    }

    private String opText(String op) {
        return switch (op) {
            case "gt" -> "高于";
            case "gte" -> "不低于";
            case "ge" -> "不低于";
            case "lt" -> "低于";
            case "lte" -> "不高于";
            case "le" -> "不高于";
            case "eq" -> "等于";
            case "between" -> "超出范围";
            default -> op;
        };
    }

    private double sanitize(String metric, double v) {
        return switch (metric) {
            case "wind_speed" -> Math.max(0.02, v);
            case "particle" -> Math.max(0, v);
            case "oxygen_pressure" -> Math.max(0, v);
            case "humidity" -> Math.min(100, Math.max(0, v));
            case "temperature" -> Math.min(60, Math.max(-20, v));
            case "vacuum" -> Math.min(-1, v);
            default -> v;
        };
    }

    /** 基础数据质量：拒绝明显不可能的量测值，避免污染曲线和告警。 */
    private void qualityCheck(String metric, double value) {
        double min = -Double.MAX_VALUE;
        double max = Double.MAX_VALUE;
        switch (metric) {
            case "temperature" -> { min = -50; max = 100; }
            case "humidity" -> { min = 0; max = 100; }
            case "pressure" -> { min = -100000; max = 100000; }
            case "wind_speed" -> { min = 0; max = 100; }
            case "particle" -> { min = 0; max = 10000000; }
            case "oxygen_pressure" -> { min = 0; max = 100; }
            case "vacuum" -> { min = -100; max = 0; }
            default -> { return; }
        }
        if (value < min || value > max) {
            throw BizException.badRequest("数据越界，已过滤：" + metric + "=" + value + "，允许范围 " + min + "~" + max);
        }
    }

    private String trim(Double v) {
        return v == null ? "-" : BigDecimal.valueOf(v).stripTrailingZeros().toPlainString();
    }

    private record RuleEval(boolean violated, boolean triggered, Double threshold, String content) {
    }

    private record EvalResult(int raised, boolean breach) {
    }

    private double simulate(String metric, Map<String, Object> p) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double base = switch (metric) {
            case "temperature" -> 23.0 + r.nextDouble(-1.2, 1.2);
            case "humidity" -> 52.0 + r.nextDouble(-5, 5);
            case "pressure" -> 15.0 + r.nextDouble(-5, 5);
            case "wind_speed" -> 0.35 + r.nextDouble(-0.06, 0.06);
            case "particle" -> 1200 + r.nextDouble(-600, 900);
            case "oxygen_pressure" -> 0.45 + r.nextDouble(-0.05, 0.05);
            case "vacuum" -> -71.0 + r.nextDouble(-5, 5);
            case "run_state" -> 1.0;
            default -> 10.0;
        };
        if (r.nextDouble() < 0.015) {
            Double lo = dbl(p.get("alarm_lower"));
            Double hi = dbl(p.get("alarm_upper"));
            if (lo != null) base = lo - r.nextDouble(0.5, 3);
            else if (hi != null) base = hi + r.nextDouble(0.5, 3);
        }
        return base;
    }

    private double round(double v, int precision) {
        return BigDecimal.valueOf(v).setScale(Math.max(0, precision), RoundingMode.HALF_UP).doubleValue();
    }

    private int count(String sql) {
        Integer n = jdbc.queryForObject(sql, Integer.class);
        return n == null ? 0 : n;
    }

    private String filter(String column, List<Long> ids) {
        if (ids == null) return "";
        if (ids.isEmpty()) return " and 1=0 ";
        return " and " + column + " in (" + ids.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")) + ") ";
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static int num(Object o, int def) {
        return o == null ? def : ((Number) o).intValue();
    }

    private static Double dbl(Object o) {
        return o == null ? null : ((Number) o).doubleValue();
    }
}
