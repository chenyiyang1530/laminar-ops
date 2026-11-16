package com.laminar.ops.biz.service;

import com.laminar.ops.common.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 派工评分：区域/在岗/请假做门槛，按工单类型用六维权重评分。
 * 证件暂不作为淘汰项。
 */
@Service
@RequiredArgsConstructor
public class SmartDispatchService {

    private static final String SCORE_VERSION = "dispatch-v1";
    private static final int[] WEIGHTS_FILTER = {32, 20, 14, 10, 16, 8};
    private static final int[] WEIGHTS_CLEAN = {30, 22, 14, 8, 18, 8};
    private static final int[] WEIGHTS_HVAC = {28, 18, 24, 10, 12, 8};
    private static final int[] WEIGHTS_TEMP = {30, 20, 20, 10, 12, 8};
    private static final int[] WEIGHTS_ELECTRIC = {34, 16, 20, 10, 12, 8};
    private static final int[] WEIGHTS_ROUTINE = {22, 20, 16, 14, 18, 10};
    private static final int[] WEIGHTS_ENVELOPE = {30, 18, 14, 10, 16, 12};
    private static final int[] WEIGHTS_GAS = {30, 18, 16, 10, 14, 12};
    private static final int[] WEIGHTS_P1 = {24, 16, 34, 8, 10, 8};

    private static final Map<String, String> TYPE_NAMES = Map.of(
            "FILTER_PAO", "高效过滤器更换＋PAO检漏",
            "CLEAN_DETECT", "洁净度/压差/风速检测",
            "HVAC_REPAIR", "净化空调/模块机/多联机抢修",
            "OR_TEMP_HUM", "手术室温湿度异常调试",
            "ELECTRICAL", "电气/自控/传感器故障",
            "ROUTINE", "日常巡检保养",
            "ENVELOPE", "气密围护/洁净门/水封",
            "GAS_ROOM", "空压/负压机房",
            "P1_EMERGENCY", "P1紧急抢修"
    );

    private final JdbcTemplate jdbc;

    public Map<String, Object> recommend(Long workorderId) {
        List<Map<String, Object>> woList = jdbc.queryForList(
                "select w.workorder_id, w.workorder_no, w.title, w.fault_desc, w.fault_level, w.order_type, " +
                        "w.required_skill_ids, w.hospital_id, w.equipment_id, w.sla_deadline, h.region_id, " +
                        "h.latitude as hospital_lat, h.longitude as hospital_lng, t.type_name " +
                        "from biz_workorder w left join biz_equipment e on e.equipment_id=w.equipment_id " +
                        "left join biz_device_type t on t.type_id=e.device_type_id " +
                        "left join biz_hospital h on h.hospital_id=w.hospital_id where w.workorder_id=?", workorderId);
        if (woList.isEmpty()) throw BizException.badRequest("工单不存在");
        Map<String, Object> wo = woList.get(0);
        Long hospitalId = ((Number) wo.get("hospital_id")).longValue();
        String typeName = wo.get("type_name") == null ? "" : String.valueOf(wo.get("type_name"));
        String orderType = wo.get("order_type") == null || String.valueOf(wo.get("order_type")).isBlank()
                ? classify(typeName, String.valueOf(wo.get("title") == null ? "" : wo.get("title")),
                String.valueOf(wo.get("fault_desc") == null ? "" : wo.get("fault_desc")),
                String.valueOf(wo.get("fault_level") == null ? "2" : wo.get("fault_level")))
                : String.valueOf(wo.get("order_type"));
        if (!orderType.equals(String.valueOf(wo.get("order_type")))) {
            jdbc.update("update biz_workorder set order_type=? where workorder_id=?", orderType, workorderId);
        }
        int[] weights = weights(orderType);
        String typeLabel = TYPE_NAMES.getOrDefault(orderType, orderType);
        Object regionId = wo.get("region_id");
        List<String> alarmSkills = requiredSkillNames(wo.get("required_skill_ids"));

        List<Map<String, Object>> engineers = jdbc.queryForList(
                "select e.engineer_id, e.engineer_name, e.phone, e.job_title, e.specialty, e.region_id, e.on_duty, " +
                        "e.max_open_orders, e.latitude, e.longitude, " +
                        "group_concat(concat(s.skill_name, ':', coalesce(es.level, '3')) separator '、') as skill_details, " +
                        "(select count(*) from biz_workorder w2 where w2.engineer_id=e.engineer_id and w2.status in ('0','1','2','3','4')) as load_count, " +
                        "(select count(*) from biz_workorder w2 where w2.engineer_id=e.engineer_id and w2.status='5' and w2.order_type=?) as same_type_done, " +
                        "(select count(*) from biz_workorder w2 where w2.engineer_id=e.engineer_id and w2.status='5' and w2.hospital_id=?) as hospital_done, " +
                        "(select coalesce(avg(w2.callback_score), 0) from biz_workorder w2 where w2.engineer_id=e.engineer_id and w2.status='5') as avg_callback, " +
                        "(select count(*) from biz_workorder w2 where w2.engineer_id=e.engineer_id and w2.accept_result='1') as accept_ok, " +
                        "(select count(*) from biz_workorder w2 where w2.engineer_id=e.engineer_id and w2.accept_result is not null) as accept_total, " +
                        "(select count(*) from biz_engineer_leave l where l.engineer_id=e.engineer_id and l.status='APPROVED' " +
                        "and l.start_date <= curdate() and l.end_date >= curdate()) as on_leave " +
                        "from biz_engineer e left join biz_engineer_skill es on es.engineer_id=e.engineer_id " +
                        "left join biz_skill s on s.skill_id=es.skill_id where e.status='0' group by e.engineer_id",
                orderType, hospitalId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> e : engineers) {
            Long engineerId = ((Number) e.get("engineer_id")).longValue();
            if (regionId != null && (e.get("region_id") == null
                    || !String.valueOf(regionId).equals(String.valueOf(e.get("region_id"))))) continue;
            if (!"1".equals(String.valueOf(e.get("on_duty")))) continue;
            if (((Number) e.get("on_leave")).intValue() > 0) continue;
            int load = ((Number) e.get("load_count")).intValue();
            int maxOpen = e.get("max_open_orders") == null ? 5 : ((Number) e.get("max_open_orders")).intValue();
            if (maxOpen <= 0 || load >= maxOpen) continue;

            Map<String, Object> score = scoreCandidate(e, orderType, typeName, weights, wo, alarmSkills);
            score.put("engineer_id", engineerId);
            score.put("engineer_name", e.get("engineer_name"));
            score.put("job_title", e.get("job_title"));
            score.put("load_count", load);
            score.put("on_duty", e.get("on_duty"));
            score.put("region_id", e.get("region_id"));
            result.add(score);
        }
        result.sort(Comparator
                .comparingInt((Map<String, Object> m) -> ((Number) m.get("score")).intValue()).reversed()
                .thenComparingInt(m -> ((Number) m.get("load_count")).intValue()));

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("workorderNo", wo.get("workorder_no"));
        resp.put("workorderTitle", wo.get("title"));
        resp.put("typeName", typeName);
        resp.put("orderType", orderType);
        resp.put("orderTypeName", typeLabel);
        resp.put("requiredSkillNames", alarmSkills);
        resp.put("weights", weightsMap(weights));
        resp.put("scoreVersion", SCORE_VERSION);
        resp.put("candidates", result);
        return resp;
    }

    /** 给指定工程师评分，供服务端写派工快照。 */
    public Map<String, Object> scoreForEngineer(Long workorderId, Long engineerId) {
        Map<String, Object> response = recommend(workorderId);
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        for (Map<String, Object> candidate : candidates) {
            if (String.valueOf(engineerId).equals(String.valueOf(candidate.get("engineer_id")))) {
                candidate.put("orderType", response.get("orderType"));
                candidate.put("orderTypeName", response.get("orderTypeName"));
                candidate.put("scoreVersion", response.get("scoreVersion"));
                return candidate;
            }
        }
        return null;
    }

    private Map<String, Object> scoreCandidate(Map<String, Object> e, String orderType, String typeName,
                                               int[] weights, Map<String, Object> wo, List<String> alarmSkills) {
        List<String> required = new ArrayList<>(alarmSkills);
        for (String skill : requiredSkills(orderType)) {
            if (!required.contains(skill)) required.add(skill);
        }
        int skill = skillScore(e, required);
        int experience = experienceScore(e);
        int timeliness = timelinessScore(e, wo);
        int load = loadScore(e);
        int quality = qualityScore(e);
        int resource = 50;

        int[] scores = {skill, experience, timeliness, load, quality, resource};
        String[] names = {"skill", "experience", "timeliness", "load", "quality", "resource"};
        String[] labels = {"技能", "经验", "时效", "负荷", "质量", "资源"};
        String[] reasons = {
                skillReason(e, required, alarmSkills),
                "同类完成 " + e.get("same_type_done") + " 单，本院完成 " + e.get("hospital_done") + " 单",
                timelinessReason(e, wo, timeliness),
                "在手 " + e.get("load_count") + " 单 / 上限 " + e.get("max_open_orders") + " 单",
                qualityReason(e, quality),
                "资源数据未接入，暂按中性分"
        };
        int total = 0;
        List<Map<String, Object>> dimensions = new ArrayList<>();
        for (int i = 0; i < scores.length; i++) {
            int weighted = Math.round(scores[i] * weights[i] / 100f);
            total += weighted;
            Map<String, Object> dimension = new LinkedHashMap<>();
            dimension.put("key", names[i]);
            dimension.put("label", labels[i]);
            dimension.put("score", scores[i]);
            dimension.put("weight", weights[i]);
            dimension.put("weighted", weighted);
            dimension.put("reason", reasons[i]);
            dimensions.add(dimension);
        }

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("score", Math.min(100, Math.max(0, total)));
        row.put("orderType", orderType);
        row.put("orderTypeName", TYPE_NAMES.getOrDefault(orderType, orderType));
        row.put("weights", weightsMap(weights));
        row.put("dimensions", dimensions);
        row.put("reasons", List.of(
                "技能 " + skill + " 分",
                "经验 " + experience + " 分",
                "时效 " + timeliness + " 分",
                "负荷 " + load + " 分",
                "质量 " + quality + " 分",
                "资源 " + resource + " 分"
        ));
        row.put("scoreVersion", SCORE_VERSION);
        return row;
    }

    private int skillScore(Map<String, Object> e, List<String> required) {
        Map<String, Integer> levels = parseSkillLevels(String.valueOf(e.get("skill_details") == null ? "" : e.get("skill_details")));
        List<Integer> matchedLevels = new ArrayList<>();
        for (String skill : required) {
            Integer level = levels.get(skill);
            if (level != null) matchedLevels.add(level);
        }
        if (matchedLevels.isEmpty()) return 30;
        matchedLevels.sort(Comparator.reverseOrder());
        int top = Math.min(5, matchedLevels.get(0));
        int second = matchedLevels.size() > 1 ? Math.min(5, matchedLevels.get(1)) : top;
        return (int) Math.round(top * 20 * 0.7 + second * 20 * 0.3);
    }

    private String skillReason(Map<String, Object> e, List<String> required, List<String> alarmSkills) {
        Map<String, Integer> levels = parseSkillLevels(String.valueOf(e.get("skill_details") == null ? "" : e.get("skill_details")));
        List<String> matched = new ArrayList<>();
        for (String skill : required) {
            if (levels.containsKey(skill)) matched.add(skill);
        }
        if (matched.isEmpty()) return "未匹配到本工单技能标签";
        String prefix = alarmSkills.isEmpty() ? "匹配技能：" : "告警规则要求技能：";
        return prefix + String.join("、", matched);
    }

    private List<String> requiredSkillNames(Object rawIds) {
        if (rawIds == null || String.valueOf(rawIds).isBlank()) return new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        for (String part : String.valueOf(rawIds).split(",")) {
            if (part.isBlank()) continue;
            try {
                ids.add(Long.valueOf(part.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        if (ids.isEmpty()) return new ArrayList<>();
        String placeholders = String.join(",", ids.stream().map(id -> "?").toList());
        return jdbc.queryForList(
                "select skill_name from biz_skill where skill_id in (" + placeholders + ")",
                String.class, ids.toArray());
    }

    private Map<String, Integer> parseSkillLevels(String details) {
        Map<String, Integer> levels = new LinkedHashMap<>();
        if (details == null || details.isBlank()) return levels;
        for (String part : details.split("、")) {
            int idx = part.lastIndexOf(':');
            if (idx <= 0) continue;
            String name = part.substring(0, idx);
            String raw = part.substring(idx + 1).replaceAll("[^0-9]", "");
            int level = raw.isBlank() ? 3 : Integer.parseInt(raw);
            if (level < 1 || level > 5) level = 3;
            levels.put(name, level);
        }
        return levels;
    }

    private List<String> requiredSkills(String orderType) {
        return switch (orderType) {
            case "FILTER_PAO" -> List.of("高效过滤器更换检漏", "初中效过滤器清洗更换", "洁净度检测", "设备巡检");
            case "CLEAN_DETECT" -> List.of("洁净区风量调试", "洁净区压差调试", "洁净度检测", "高效过滤器更换检漏");
            case "HVAC_REPAIR" -> List.of("净化空调机组检修", "风机皮带轴承维护", "表冷器加热器清洗", "温湿度调试", "设备巡检");
            case "OR_TEMP_HUM" -> List.of("温湿度调试", "表冷器加热器清洗", "加湿除湿段维护", "空调自控系统调试", "设备巡检");
            case "ELECTRICAL" -> List.of("强弱电系统检修", "变频器传感器检修", "IT隔离电源漏保检修", "设备巡检");
            case "ROUTINE" -> List.of("设备巡检", "净化空调机组检修", "初中效过滤器清洗更换", "维保记录报告");
            case "ENVELOPE" -> List.of("自动门维修", "风管风阀软接维护", "洁净室安全规范");
            case "GAS_ROOM" -> List.of("医用气体终端管路检修", "医气报警系统测试", "给排水水泵维护", "设备巡检");
            case "P1_EMERGENCY" -> List.of("故障抢修", "设备巡检", "净化空调机组检修", "强弱电系统检修", "医用气体终端管路检修");
            default -> List.of("设备巡检", "故障抢修");
        };
    }

    private int experienceScore(Map<String, Object> e) {
        int same = ((Number) e.get("same_type_done")).intValue();
        int hospital = ((Number) e.get("hospital_done")).intValue();
        int ok = ((Number) e.get("accept_ok")).intValue();
        int total = ((Number) e.get("accept_total")).intValue();
        double acceptRate = total > 0 ? ok * 1.0 / total : 0.5;
        return (int) Math.round(45 * Math.min(1, same / 10.0)
                + 30 * Math.min(1, hospital / 5.0)
                + 25 * acceptRate);
    }

    private int timelinessScore(Map<String, Object> e, Map<String, Object> wo) {
        Double eLat = dbl(e.get("latitude"));
        Double eLng = dbl(e.get("longitude"));
        Double hLat = dbl(wo.get("hospital_lat"));
        Double hLng = dbl(wo.get("hospital_lng"));
        if (eLat == null || eLng == null || hLat == null || hLng == null) return 50;
        double km = distanceKm(eLat, eLng, hLat, hLng);
        double eta = km / 30.0 * 60 + 5;
        return (int) Math.round(Math.max(0, Math.min(100, 100 * (1 - eta / 60.0))));
    }

    private String timelinessReason(Map<String, Object> e, Map<String, Object> wo, int score) {
        Double eLat = dbl(e.get("latitude"));
        Double eLng = dbl(e.get("longitude"));
        Double hLat = dbl(wo.get("hospital_lat"));
        Double hLng = dbl(wo.get("hospital_lng"));
        if (eLat == null || eLng == null || hLat == null || hLng == null) return "位置未维护，暂按中性分";
        double km = distanceKm(eLat, eLng, hLat, hLng);
        double eta = km / 30.0 * 60 + 5;
        return "直线约 " + Math.round(km) + " 公里，预计 " + Math.round(eta) + " 分钟";
    }

    private int loadScore(Map<String, Object> e) {
        int load = ((Number) e.get("load_count")).intValue();
        int maxOpen = e.get("max_open_orders") == null ? 5 : ((Number) e.get("max_open_orders")).intValue();
        if (maxOpen <= 0) return 0;
        return (int) Math.round(Math.max(0, Math.min(100, 100 * (1 - load * 1.0 / maxOpen))));
    }

    private int qualityScore(Map<String, Object> e) {
        double avgCallback = e.get("avg_callback") == null ? 0 : ((Number) e.get("avg_callback")).doubleValue();
        int ok = ((Number) e.get("accept_ok")).intValue();
        int total = ((Number) e.get("accept_total")).intValue();
        double acceptance = total > 0 ? ok * 1.0 / total * 100 : 50;
        if (avgCallback <= 0) return (int) Math.round(acceptance);
        return (int) Math.round((avgCallback * 20) * 0.7 + acceptance * 0.3);
    }

    private String qualityReason(Map<String, Object> e, int quality) {
        int ok = ((Number) e.get("accept_ok")).intValue();
        int total = ((Number) e.get("accept_total")).intValue();
        double avgCallback = e.get("avg_callback") == null ? 0 : ((Number) e.get("avg_callback")).doubleValue();
        if (total == 0 && avgCallback <= 0) return "历史质量数据不足，按中性分";
        return "验收通过率 " + (total == 0 ? "无样本" : Math.round(ok * 100.0 / total) + "%")
                + "，回访均分 " + (avgCallback <= 0 ? "无" : String.format("%.1f", avgCallback));
    }

    private String classify(String typeName, String title, String faultDesc, String faultLevel) {
        if ("0".equals(faultLevel)) return "P1_EMERGENCY";
        String text = typeName + " " + title + " " + faultDesc;
        if (text.contains("巡检") || text.contains("保养") || text.contains("维保")) return "ROUTINE";
        if (text.contains("过滤器") || text.contains("PAO") || text.contains("检漏")) return "FILTER_PAO";
        if (text.contains("洁净度") || text.contains("压差") || text.contains("风速") || text.contains("检测")) {
            return "CLEAN_DETECT";
        }
        if (text.contains("温湿度") || (text.contains("手术室") && (text.contains("温度") || text.contains("湿度")))) {
            return "OR_TEMP_HUM";
        }
        if (text.contains("温度") || text.contains("湿度")) return "HVAC_REPAIR";
        if (text.contains("电气") || text.contains("自控") || text.contains("传感器")
                || text.contains("跳闸") || text.contains("电源")) return "ELECTRICAL";
        if (text.contains("气密") || text.contains("洁净门") || text.contains("水封")
                || text.contains("围护") || text.contains("自动门")) return "ENVELOPE";
        if (text.contains("空压") || text.contains("负压") || text.contains("真空")) return "GAS_ROOM";
        return "HVAC_REPAIR";
    }

    private int[] weights(String orderType) {
        return switch (orderType) {
            case "FILTER_PAO" -> WEIGHTS_FILTER;
            case "CLEAN_DETECT" -> WEIGHTS_CLEAN;
            case "HVAC_REPAIR" -> WEIGHTS_HVAC;
            case "OR_TEMP_HUM" -> WEIGHTS_TEMP;
            case "ELECTRICAL" -> WEIGHTS_ELECTRIC;
            case "ROUTINE" -> WEIGHTS_ROUTINE;
            case "ENVELOPE" -> WEIGHTS_ENVELOPE;
            case "GAS_ROOM" -> WEIGHTS_GAS;
            case "P1_EMERGENCY" -> WEIGHTS_P1;
            default -> WEIGHTS_HVAC;
        };
    }

    private Map<String, Object> weightsMap(int[] weights) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("skill", weights[0]);
        map.put("experience", weights[1]);
        map.put("timeliness", weights[2]);
        map.put("load", weights[3]);
        map.put("quality", weights[4]);
        map.put("resource", weights[5]);
        return map;
    }

    private Double dbl(Object value) {
        if (value == null) return null;
        try {
            return Double.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double r = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
