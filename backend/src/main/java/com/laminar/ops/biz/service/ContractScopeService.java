package com.laminar.ops.biz.service;

import com.laminar.ops.common.BizException;
import com.laminar.ops.security.AuthContext;
import com.laminar.ops.security.DataScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ContractScopeService {

    private final JdbcTemplate jdbc;
    private final DataScopeService dataScope;

    private static final Set<String> TYPES = Set.of("hospital", "department", "area", "equipment");

    public List<Map<String, Object>> list(Long contractId) {
        Map<String, Object> contract = requireContract(contractId);
        checkAccess(((Number) contract.get("hospital_id")).longValue());
        return jdbc.queryForList(
                "select s.scope_id as `scopeId`, s.scope_type as `scopeType`, s.scope_value as `scopeValue`, " +
                        "case s.scope_type " +
                        "  when 'hospital' then h.hospital_name " +
                        "  when 'department' then d.department_name " +
                        "  when 'area' then a.area_name " +
                        "  when 'equipment' then e.equipment_name " +
                        "end as `scopeName` " +
                        "from biz_contract_scope s " +
                        "left join biz_hospital h on s.scope_type='hospital' and h.hospital_id=? " +
                        "left join biz_department d on s.scope_type='department' and d.department_id=s.scope_value " +
                        "left join biz_clean_area a on s.scope_type='area' and a.area_id=s.scope_value " +
                        "left join biz_equipment e on s.scope_type='equipment' and e.equipment_id=s.scope_value " +
                        "where s.contract_id=? order by s.scope_type, s.scope_id",
                contract.get("hospital_id"), contractId);
    }

    @Transactional
    public void save(Long contractId, List<Map<String, Object>> scopes) {
        Map<String, Object> contract = requireContract(contractId);
        Long hospitalId = ((Number) contract.get("hospital_id")).longValue();
        checkAccess(hospitalId);
        if (scopes == null) scopes = List.of();
        List<Object[]> rows = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        boolean wholeHospital = false;
        for (Map<String, Object> item : scopes) {
            String type = String.valueOf(item.get("scopeType"));
            if (!TYPES.contains(type)) throw BizException.badRequest("不支持的服务范围类型：" + type);
            Long value = longOrNull(item.get("scopeValue"));
            if ("hospital".equals(type)) {
                if (scopes.size() > 1) throw BizException.badRequest("全院范围不能与其他范围同时选择");
                wholeHospital = true;
            } else {
                if (value == null) throw BizException.badRequest("服务范围对象不能为空");
                Long targetHospital = scopeHospital(type, value);
                if (targetHospital == null || !targetHospital.equals(hospitalId)) {
                    throw BizException.badRequest("服务范围不属于该合同医院");
                }
            }
            String key = type + ":" + (value == null ? "" : value);
            if (keys.add(key)) rows.add(new Object[]{contractId, type, value});
        }
        jdbc.update("delete from biz_contract_scope where contract_id=?", contractId);
        if (!rows.isEmpty()) {
            jdbc.batchUpdate("insert into biz_contract_scope(contract_id,scope_type,scope_value) values(?,?,?)", rows);
        }
    }

    /**
     * 按工单位置匹配有效合同：设备 > 洁净区域 > 科室 > 全院/无范围。
     * 命中唯一公司返回公司 ID，多公司冲突返回 null。
     */
    public Long resolveCompany(Long hospitalId, Long departmentId, Long areaId, Long equipmentId) {
        if (hospitalId == null) return null;
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select c.company_id, s.scope_type from biz_contract c " +
                        "left join biz_contract_scope s on s.contract_id=c.contract_id " +
                        "where c.hospital_id=? and c.company_id is not null and c.status='0' " +
                        "and (c.start_date is null or c.start_date<=curdate()) " +
                        "and (c.end_date is null or c.end_date>=curdate()) " +
                        "and (not exists (select 1 from biz_contract_scope x where x.contract_id=c.contract_id) " +
                        "  or s.scope_type='hospital' " +
                        "  or (s.scope_type='equipment' and s.scope_value=?) " +
                        "  or (s.scope_type='area' and s.scope_value=?) " +
                        "  or (s.scope_type='department' and s.scope_value=?))",
                hospitalId, equipmentId, areaId, departmentId);
        Map<Long, Integer> companyPriority = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Long companyId = ((Number) row.get("company_id")).longValue();
            int priority = priority(String.valueOf(row.get("scope_type")));
            companyPriority.merge(companyId, priority, Math::min);
        }
        if (companyPriority.isEmpty()) return null;
        int best = companyPriority.values().stream().mapToInt(Integer::intValue).min().orElse(99);
        List<Long> matched = companyPriority.entrySet().stream()
                .filter(e -> e.getValue() == best).map(Map.Entry::getKey).toList();
        return matched.size() == 1 ? matched.get(0) : null;
    }

    private int priority(String type) {
        return switch (type) {
            case "equipment" -> 1;
            case "area" -> 2;
            case "department" -> 3;
            default -> 4;
        };
    }

    /** 本公司当前有效合同覆盖的医院 */
    public List<Long> hospitalIdsForCompany(Long companyId) {        if (companyId == null) return List.of();
        return jdbc.queryForList(
                "select distinct hospital_id from biz_contract " +
                        "where company_id=? and status='0' " +
                        "and (start_date is null or start_date<=curdate()) " +
                        "and (end_date is null or end_date>=curdate())",
                Long.class, companyId);
    }

    /** 本公司负责的设备 ID */
    public List<Long> equipmentIdsForCompany(Long companyId) {
        if (companyId == null) return List.of();
        return jdbc.queryForList(
                "select equipment_id from biz_equipment where company_id=?",
                Long.class, companyId);
    }

    public boolean coversEquipment(Long companyId, Long equipmentId) {
        if (companyId == null || equipmentId == null) return false;
        List<Long> ids = jdbc.queryForList(
                "select company_id from biz_equipment where equipment_id=?", Long.class, equipmentId);
        return !ids.isEmpty() && companyId.equals(ids.get(0));
    }

    public boolean coversPoint(Long companyId, Long pointId) {
        List<Long> ids = jdbc.queryForList(
                "select equipment_id from biz_monitor_point where point_id=?", Long.class, pointId);
        return !ids.isEmpty() && coversEquipment(companyId, ids.get(0));
    }

    /** 医院唯一有效合同公司；多家公司时不猜测 */
    public Long uniqueActiveCompany(Long hospitalId) {
        if (hospitalId == null) return null;
        List<Long> ids = jdbc.queryForList(
                "select distinct company_id from biz_contract " +
                        "where hospital_id=? and company_id is not null and status='0' " +
                        "and (start_date is null or start_date<=curdate()) " +
                        "and (end_date is null or end_date>=curdate())",
                Long.class, hospitalId);
        return ids.size() == 1 ? ids.get(0) : null;
    }

    private Long scopeHospital(String type, Long value) {
        String table;
        String column;
        switch (type) {
            case "department" -> { table = "biz_department"; column = "department_id"; }
            case "area" -> { table = "biz_clean_area"; column = "area_id"; }
            case "equipment" -> { table = "biz_equipment"; column = "equipment_id"; }
            default -> { return null; }
        }
        List<Long> rows = jdbc.queryForList(
                "select hospital_id from " + table + " where " + column + "=?", Long.class, value);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Map<String, Object> requireContract(Long contractId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select contract_id, hospital_id from biz_contract where contract_id=?", contractId);
        if (rows.isEmpty()) throw BizException.badRequest("合同不存在");
        return rows.get(0);
    }

    private void checkAccess(Long hospitalId) {
        if (!dataScope.canAccessHospital(AuthContext.require(), hospitalId)) {
            throw BizException.forbidden("无权操作其他医院合同");
        }
    }

    private Long longOrNull(Object v) {
        if (v == null || String.valueOf(v).isBlank()) return null;
        return Long.valueOf(String.valueOf(v));
    }
}
