package com.laminar.ops.biz.service;

import com.laminar.ops.common.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SmartDispatchService {

    private final JdbcTemplate jdbc;

    public Map<String, Object> recommend(Long workorderId) {
        List<Map<String, Object>> woList = jdbc.queryForList(
                "select w.workorder_id, w.workorder_no, w.title, w.hospital_id, w.equipment_id, w.fault_level, " +
                        "t.type_name, t.category, h.region_id " +
                        "from biz_workorder w left join biz_equipment e on e.equipment_id=w.equipment_id " +
                        "left join biz_device_type t on t.type_id=e.device_type_id " +
                        "left join biz_hospital h on h.hospital_id=w.hospital_id where w.workorder_id=?", workorderId);
        if (woList.isEmpty()) throw BizException.badRequest("工单不存在");
        Map<String, Object> wo = woList.get(0);
        Long hospitalId = ((Number) wo.get("hospital_id")).longValue();

        Set<Long> serving = new HashSet<>(jdbc.queryForList(
                "select engineer_id from biz_engineer_hospital where hospital_id=?", Long.class, hospitalId));
        String typeName = wo.get("type_name") == null ? "" : String.valueOf(wo.get("type_name"));
        String category = wo.get("category") == null ? "" : String.valueOf(wo.get("category"));
        List<String> keywords = keywords(category);
        Object regionId = wo.get("region_id");

        List<Map<String, Object>> engineers = jdbc.queryForList(
                "select e.engineer_id, e.engineer_name, e.phone, e.job_title, e.specialty, e.region_id, e.on_duty, e.max_open_orders, " +
                        "group_concat(s.skill_name separator '、') as skills, " +
                        "(select count(*) from biz_workorder w2 where w2.engineer_id=e.engineer_id and w2.status in ('0','1','2','3','4')) as load_count, " +
                        "(select count(*) from biz_certificate c where c.engineer_id=e.engineer_id and c.expire_date < curdate()) as expired_cert, " +
                        "(select count(*) from biz_engineer_leave l where l.engineer_id=e.engineer_id and l.status='APPROVED' " +
                        "and l.start_date <= curdate() and l.end_date >= curdate()) as on_leave " +
                        "from biz_engineer e left join biz_engineer_skill es on es.engineer_id=e.engineer_id " +
                        "left join biz_skill s on s.skill_id=es.skill_id where e.status='0' group by e.engineer_id");

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> e : engineers) {
            Long engineerId = ((Number) e.get("engineer_id")).longValue();
            boolean serves = serving.contains(engineerId);
            if (!serving.isEmpty() && !serves) continue;
            if (!"1".equals(String.valueOf(e.get("on_duty")))) continue;
            int load = ((Number) e.get("load_count")).intValue();
            int maxOpen = e.get("max_open_orders") == null ? 5 : ((Number) e.get("max_open_orders")).intValue();
            if (load >= maxOpen) continue;
            int expired = ((Number) e.get("expired_cert")).intValue();
            if (expired > 0) continue;
            int onLeave = ((Number) e.get("on_leave")).intValue();
            if (onLeave > 0) continue;
            int score = 0;
            List<String> reasons = new ArrayList<>();
            if (serves) { score += 40; reasons.add("服务该医院"); }
            String skills = (e.get("skills") == null ? "" : String.valueOf(e.get("skills"))) + " " +
                    (e.get("specialty") == null ? "" : String.valueOf(e.get("specialty")));
            boolean skillMatch = keywords.stream().anyMatch(skills::contains);
            if (skillMatch) { score += 30; reasons.add("技能匹配" + String.join("/", keywords)); }
            if (regionId != null && e.get("region_id") != null && String.valueOf(regionId).equals(String.valueOf(e.get("region_id")))) {
                score += 10;
                reasons.add("同区域");
            }
            score += 10;
            reasons.add("在岗");
            score += Math.max(0, 20 - load * 5);
            reasons.add("在手工单 " + load + " 张");

            Map<String, Object> row = new LinkedHashMap<>(e);
            row.put("score", score);
            row.put("reasons", reasons);
            row.put("servesHospital", serves);
            result.add(row);
        }
        result.sort(Comparator.comparingInt((Map<String, Object> m) -> ((Number) m.get("score")).intValue()).reversed());

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("workorderNo", wo.get("workorder_no"));
        resp.put("workorderTitle", wo.get("title"));
        resp.put("typeName", typeName);
        resp.put("candidates", result);
        return resp;
    }

    private List<String> keywords(String category) {
        return switch (category) {
            case "laminar" -> List.of("层流");
            case "purifier" -> List.of("净化空调", "净化");
            case "fresh_air" -> List.of("新风");
            case "dehumidifier" -> List.of("除湿");
            case "oxygen" -> List.of("供氧", "氧气");
            case "vacuum" -> List.of("负压", "真空");
            default -> List.of("层流", "净化");
        };
    }
}
