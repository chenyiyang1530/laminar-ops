package com.laminar.ops.common.crud;

import com.laminar.ops.biz.service.CompanyCodeService;
import com.laminar.ops.biz.service.ContractScopeService;
import com.laminar.ops.common.BizException;
import com.laminar.ops.security.AuthContext;
import com.laminar.ops.security.DataScopeService;
import com.laminar.ops.security.LoginUser;
import com.laminar.ops.system.service.CertificateTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 元数据驱动通用 CRUD：字段白名单、参数化 SQL、数据范围（医院/区域/本人）。
 */
@Service
@RequiredArgsConstructor
public class GenericCrudService {

    private final JdbcTemplate jdbc;
    private final DataScopeService dataScope;
    private final ContractScopeService contractScope;
    private final CompanyCodeService companyCodeService;
    private final CertificateTypeService certificateTypeService;

    private static final List<String> CUSTOMER_KEYS =
            List.of("hospital", "campus", "department", "cleanArea", "contact");

    private static final Map<String, String[][]> REFERENCES = Map.ofEntries(
            Map.entry("hospital", new String[][]{
                    ref("biz_campus", "hospital_id", "院区"),
                    ref("biz_department", "hospital_id", "科室"),
                    ref("biz_clean_area", "hospital_id", "洁净区域"),
                    ref("biz_contact", "hospital_id", "对接人"),
                    ref("biz_equipment", "hospital_id", "设备"),
                    ref("biz_contract", "hospital_id", "合同"),
                    ref("biz_workorder", "hospital_id", "工单"),
                    ref("biz_hospital_user", "hospital_id", "院方账号")}),
            Map.entry("region", new String[][]{ref("biz_hospital", "region_id", "医院")}),
            Map.entry("campus", new String[][]{
                    ref("biz_department", "campus_id", "科室"),
                    ref("biz_clean_area", "campus_id", "洁净区域"),
                    ref("biz_contact", "campus_id", "对接人"),
                    ref("biz_equipment", "campus_id", "设备"),
                    ref("biz_workorder", "campus_id", "工单")}),
            Map.entry("department", new String[][]{
                    ref("biz_clean_area", "department_id", "洁净区域"),
                    ref("biz_contact", "department_id", "对接人"),
                    ref("biz_equipment", "department_id", "设备"),
                    ref("biz_workorder", "department_id", "工单")}),
            Map.entry("cleanArea", new String[][]{
                    ref("biz_equipment", "area_id", "设备"),
                    ref("biz_workorder", "area_id", "工单")}),
            Map.entry("equipment", new String[][]{
                    ref("biz_monitor_point", "equipment_id", "监测点位"),
                    ref("biz_workorder", "equipment_id", "工单"),
                    ref("biz_alarm", "equipment_id", "告警"),
                    ref("biz_document", "equipment_id", "文档")}),
            Map.entry("deviceType", new String[][]{
                    ref("biz_equipment", "device_type_id", "设备"),
                    ref("biz_maintenance_plan", "device_type_id", "保养计划")}),
            Map.entry("company", new String[][]{
                    ref("biz_contract", "company_id", "合同"),
                    ref("biz_region", "company_id", "区域"),
                    ref("biz_engineer", "company_id", "工程师")}),
            Map.entry("part", new String[][]{
                    ref("biz_part_requisition_item", "part_id", "领用明细"),
                    ref("biz_part_record", "part_id", "出入库流水")})
    );

    private static String[] ref(String table, String column, String label) {
        return new String[]{table, column, label};
    }

    public ResourceDef def(String key) {
        ResourceDef def = CrudRegistry.get(key);
        if (def == null) {
            throw BizException.badRequest("未知资源：" + key);
        }
        return def;
    }

    public Map<String, Object> meta(String key) {
        ResourceDef def = def(key);
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("key", def.getKey());
        meta.put("table", def.getTable());
        meta.put("pk", def.getFields().stream()
                .filter(f -> f.getColumn().equals(def.getPk()))
                .map(FieldDef::getName)
                .findFirst()
                .orElse(def.getPk()));
        meta.put("pkColumn", def.getPk());
        meta.put("title", def.getTitle());
        meta.put("permPrefix", def.getPermPrefix());
        meta.put("nameColumn", def.getNameColumn());
        meta.put("hospitalScoped", def.isHospitalScoped());
        meta.put("fields", def.getFields());
        return meta;
    }

    public Map<String, Object> page(String key, Map<String, String> params) {
        ResourceDef def = def(key);
        check(def, "list");
        LoginUser user = AuthContext.require();
        int pageNum = Math.max(1, intVal(params.get("pageNum"), 1));
        int pageSize = Math.min(200, Math.max(1, intVal(params.get("pageSize"), 10)));

        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" where 1=1 ");
        for (FieldDef f : def.getFields()) {
            if (!"like".equals(f.getQuery()) && !"eq".equals(f.getQuery())) continue;
            String v = params.get(f.getName());
            if (v == null || v.isBlank()) continue;
            if ("like".equals(f.getQuery())) {
                where.append(" and e.").append(f.getColumn()).append(" like ? ");
                args.add("%" + v.trim() + "%");
            } else {
                where.append(" and e.").append(f.getColumn()).append(" = ? ");
                args.add(convert(f, v));
            }
        }

        List<Long> allowed = scopeIds(def, user);
        if (allowed != null && allowed.isEmpty()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("records", List.of());
            empty.put("total", 0);
            return empty;
        }
        if ("region".equals(def.getKey()) && "region".equals(user.getDataScope())) {
            applyRegionScope(where, args, "e.", user);
        }
        applyScopedResourceFilter(def, user, where, args, "e.");
        applyCompanyFilter(def, user, where, args, "e.");
        String hospitalColumn = hospitalColumn(def);
        if (allowed != null) {
            where.append(" and e.").append(hospitalColumn)
                    .append(" in (").append(joinIds(allowed)).append(") ");
        }

        String from = " from " + def.getTable() + " e " + (def.getJoinSql() == null ? "" : def.getJoinSql());
        Long total = jdbc.queryForObject("select count(*) " + from + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add((pageNum - 1) * pageSize);
        List<Map<String, Object>> records = jdbc.queryForList(
                "select " + selectColumns(def) + from + where + " order by " + def.getOrderBy() + " limit ? offset ?",
                pageArgs.toArray());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total == null ? 0 : total);
        return result;
    }

    public Map<String, Object> get(String key, Long id) {
        ResourceDef def = def(key);
        check(def, "list");
        List<Map<String, Object>> list = jdbc.queryForList(
                "select " + selectColumns(def) + " from " + def.getTable() + " e " +
                        (def.getJoinSql() == null ? "" : def.getJoinSql()) +
                        " where e." + def.getPk() + " = ?", id);
        if (list.isEmpty()) {
            throw BizException.badRequest("数据不存在");
        }
        checkScope(def, list.get(0));
        return list.get(0);
    }

    public Long save(String key, Map<String, Object> body) {
        ResourceDef def = def(key);
        check(def, "add");
        LoginUser user = AuthContext.require();
        checkCustomerWritable(def, user);
        if ("region".equals(def.getKey()) && "region".equals(user.getDataScope())) {
            throw BizException.forbidden("区域主管不能新增区域");
        }
        if ("company".equals(def.getKey()) && dataScope.companyId(user) != null) {
            throw BizException.forbidden("无权新增公司");
        }
        applyHospitalForWrite(def, user, body);
        applyResourceWriteScope(def, user, body);
        certificateTypeService.normalize(def, body);
        normalizeCodes(body);
        if ("company".equals(def.getKey())) {
            companyCodeService.validateNewCode(String.valueOf(body.get("companyCode")), null);
        }
        validateHierarchy(body);
        validateContract(def, body);
        validateSpecialReferences(def, body, null);
        clearOtherPrimary(def, body, null);

        List<String> cols = new ArrayList<>();
        List<Object> vals = new ArrayList<>();
        for (FieldDef f : def.getFields()) {
            if (!f.isForm() || isPk(def, f)) continue;
            Object v = normalize(body.get(f.getName()));
            if (f.isRequired() && v == null) {
                throw BizException.badRequest(f.getLabel() + "不能为空");
            }
            if (!body.containsKey(f.getName())) continue;
            if (v == null) continue;
            cols.add(f.getColumn());
            vals.add(v);
        }
        if (def.isHasCreateTime()) { cols.add("create_time"); vals.add(LocalDateTime.now()); }
        if (def.isHasCreateBy()) { cols.add("create_by"); vals.add(user.getNickName()); }
        StringBuilder sql = new StringBuilder("insert into " + def.getTable() + " (");
        sql.append(String.join(",", cols)).append(") values (");
        sql.append(String.join(",", cols.stream().map(c -> "?").toList())).append(")");
        KeyHolder holder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < vals.size(); i++) {
                ps.setObject(i + 1, vals.get(i));
            }
            return ps;
        }, holder);
        Long id = holder.getKey() == null ? null : holder.getKey().longValue();
        certificateTypeService.recordRequest(def, body, user);
        return id;
    }

    public void update(String key, Map<String, Object> body) {
        ResourceDef def = def(key);
        check(def, "edit");
        LoginUser user = AuthContext.require();
        checkCustomerWritable(def, user);
        String pkName = def.getFields().stream()
                .filter(f -> f.getColumn().equals(def.getPk()))
                .map(FieldDef::getName)
                .findFirst()
                .orElse(def.getPk());
        Object idObj = body.get(pkName);
        if (idObj == null) {
            throw BizException.badRequest("缺少主键");
        }
        Long id = Long.valueOf(String.valueOf(idObj));
        Map<String, Object> old = get(key, id);
        checkScope(def, old);
        validateEngineerOnDuty(def, body, id);
        validateSpecialReferences(def, body, id);
        applyHospitalForWrite(def, user, body, false);
        certificateTypeService.normalize(def, body);
        normalizeCodes(body);
        String oldCompanyCode = null;
        String newCompanyCode = null;
        if ("company".equals(def.getKey())) {
            oldCompanyCode = old.get("companyCode") == null ? null : String.valueOf(old.get("companyCode"));
            newCompanyCode = body.get("companyCode") == null ? oldCompanyCode : String.valueOf(body.get("companyCode"));
            if (oldCompanyCode != null && !oldCompanyCode.equals(newCompanyCode)) {
                if (dataScope.companyId(user) != null) {
                    throw BizException.forbidden("公司编码需平台管理员修改");
                }
                companyCodeService.validateNewCode(newCompanyCode, id);
            }
        }
        Map<String, Object> merged = new LinkedHashMap<>(old);
        merged.putAll(body);
        applyResourceWriteScope(def, user, body);
        applyResourceWriteScope(def, user, merged);
        validateHierarchy(merged);
        validateContract(def, merged);
        clearOtherPrimary(def, body, id);

        List<String> sets = new ArrayList<>();
        List<Object> vals = new ArrayList<>();
        for (FieldDef f : def.getFields()) {
            if (!f.isForm() || isPk(def, f)) continue;
            if (!body.containsKey(f.getName())) continue;
            Object v = normalize(body.get(f.getName()));
            if (f.isRequired() && v == null) {
                throw BizException.badRequest(f.getLabel() + "不能为空");
            }
            sets.add(f.getColumn() + " = ?");
            vals.add(v);
        }
        if (def.isHasUpdateTime()) { sets.add("update_time = ?"); vals.add(LocalDateTime.now()); }
        if (def.isHasUpdateBy()) { sets.add("update_by = ?"); vals.add(AuthContext.nickName()); }
        if (sets.isEmpty()) return;
        vals.add(id);
        jdbc.update("update " + def.getTable() + " set " + String.join(",", sets) + " where " + def.getPk() + " = ?",
                vals.toArray());
        certificateTypeService.recordRequest(def, body, user);
        if ("company".equals(def.getKey()) && oldCompanyCode != null
                && !oldCompanyCode.equals(newCompanyCode)) {
            companyCodeService.recordCodeChange(id, oldCompanyCode, newCompanyCode);
        }
    }

    public void delete(String key, Long[] ids) {
        ResourceDef def = def(key);
        check(def, "remove");
        checkCustomerWritable(def, AuthContext.require());
        if ("company".equals(def.getKey()) && dataScope.companyId(AuthContext.require()) != null) {
            throw BizException.forbidden("公司只能由平台管理员删除");
        }
        for (Long id : ids) {
            if (def.isHospitalScoped() || "region".equals(def.getKey())) {
                Map<String, Object> old = get(key, id);
                checkScope(def, old);
            }
            checkReferences(def, id);
            jdbc.update("delete from " + def.getTable() + " where " + def.getPk() + " = ?", id);
        }
    }

    private void checkReferences(ResourceDef def, Long id) {
        String[][] refs = REFERENCES.get(def.getKey());
        if (refs == null) return;
        for (String[] ref : refs) {
            Integer count = jdbc.queryForObject("select count(*) from " + ref[0] + " where " + ref[1] + " = ?",
                    Integer.class, id);
            if (count != null && count > 0) {
                throw BizException.badRequest("存在关联的" + ref[2] + "（" + count + " 条），不能删除");
            }
        }
    }

    public List<Map<String, Object>> options(String key, Map<String, String> params) {
        ResourceDef def = def(key);
        LoginUser user = AuthContext.require();
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" where 1=1 ");
        if (def.fieldByName("status") != null) {
            where.append(" and status = '0' ");
        }
        for (FieldDef f : def.getFields()) {
            String v = params == null ? null : params.get(f.getName());
            if (v == null || v.isBlank()) continue;
            where.append(" and ").append(f.getColumn()).append(" = ? ");
            args.add(convert(f, v));
        }
        if ("region".equals(def.getKey()) && "region".equals(user.getDataScope())) {
            if (user.getRegionId() == null) return List.of();
            applyRegionScope(where, args, "", user);
        }
        applyScopedResourceFilter(def, user, where, args, "");
        applyCompanyFilter(def, user, where, args, "");
        List<Long> allowed = scopeIdsForOptions(def, user);
        if (allowed != null) {
            if (allowed.isEmpty()) return List.of();
            String hospitalColumn = hospitalColumn(def);
            where.append(" and ").append(hospitalColumn)
                    .append(" in (").append(joinIds(allowed)).append(") ");
        }
        return jdbc.queryForList("select " + def.getPk() + " as value, " + def.getNameColumn() +
                " as label from " + def.getTable() + where + " order by " + def.getPk(), args.toArray());
    }

    private void validateContract(ResourceDef def, Map<String, Object> data) {
        if (!"contract".equals(def.getKey())) return;
        LocalDate start = parseDate(data.get("startDate"));
        LocalDate end = parseDate(data.get("endDate"));
        if (start != null && end != null && end.isBefore(start)) {
            throw BizException.badRequest("合同结束日期不能早于开始日期");
        }
        Object amount = data.get("amount");
        if (amount != null && !String.valueOf(amount).isBlank()) {
            try {
                if (new java.math.BigDecimal(String.valueOf(amount)).signum() < 0) {
                    throw BizException.badRequest("合同金额不能为负数");
                }
            } catch (NumberFormatException e) {
                throw BizException.badRequest("合同金额格式不正确");
            }
        }
    }

    private LocalDate parseDate(Object value) {
        if (value == null) return null;
        String s = String.valueOf(value);
        if (s.length() < 10) return null;
        try {
            return LocalDate.parse(s.substring(0, 10));
        } catch (Exception e) {
            return null;
        }
    }

    private void clearOtherPrimary(ResourceDef def, Map<String, Object> body, Long selfId) {
        if (!"contact".equals(def.getKey())) return;
        if (!"1".equals(String.valueOf(body.get("isPrimary")))) return;
        Long hospitalId = longOrNull(body.get("hospitalId"));
        if (hospitalId == null) return;
        Long campusId = longOrNull(body.get("campusId"));
        StringBuilder sql = new StringBuilder("update biz_contact set is_primary='0' where hospital_id=?");
        List<Object> args = new ArrayList<>();
        args.add(hospitalId);
        if (campusId != null) {
            sql.append(" and campus_id=?");
            args.add(campusId);
        } else {
            sql.append(" and campus_id is null");
        }
        if (selfId != null) {
            sql.append(" and contact_id<>?");
            args.add(selfId);
        }
        jdbc.update(sql.toString(), args.toArray());
    }

    private void normalizeCodes(Map<String, Object> body) {
        for (Map.Entry<String, Object> e : body.entrySet()) {
            if (!e.getKey().endsWith("Code")) continue;
            if (e.getValue() instanceof String s) {
                e.setValue(s.trim().toUpperCase());
            }
        }
    }

    private void validateHierarchy(Map<String, Object> data) {
        Long hospitalId = longOrNull(data.get("hospitalId"));
        Long campusId = longOrNull(data.get("campusId"));
        Long departmentId = longOrNull(data.get("departmentId"));
        Long areaId = longOrNull(data.get("areaId"));
        Long equipmentId = longOrNull(data.get("equipmentId"));
        if (hospitalId != null && campusId != null) {
            assertParent("biz_campus", "campus_id", campusId, "hospital_id", hospitalId, "院区不属于所选医院");
        }
        if (hospitalId != null && departmentId != null) {
            assertParent("biz_department", "department_id", departmentId, "hospital_id", hospitalId, "科室不属于所选医院");
        }
        if (campusId != null && departmentId != null) {
            assertParent("biz_department", "department_id", departmentId, "campus_id", campusId, "科室不属于所选院区");
        }
        if (campusId != null && areaId != null) {
            assertParent("biz_clean_area", "area_id", areaId, "campus_id", campusId, "洁净区域不属于所选院区");
        }
        if (departmentId != null && areaId != null) {
            assertParent("biz_clean_area", "area_id", areaId, "department_id", departmentId, "洁净区域不属于所选科室");
        }
        if (hospitalId != null && equipmentId != null) {
            assertParent("biz_equipment", "equipment_id", equipmentId, "hospital_id", hospitalId, "设备不属于所选医院");
        }
        if (campusId != null && equipmentId != null) {
            assertParent("biz_equipment", "equipment_id", equipmentId, "campus_id", campusId, "设备不属于所选院区");
        }
        if (departmentId != null && equipmentId != null) {
            assertParent("biz_equipment", "equipment_id", equipmentId, "department_id", departmentId, "设备不属于所选科室");
        }
    }

    private void assertParent(String table, String idColumn, Long id, String parentColumn, Long parentId, String message) {
        List<Long> values = jdbc.queryForList(
                "select " + parentColumn + " from " + table + " where " + idColumn + " = ?", Long.class, id);
        if (values.isEmpty()) {
            throw BizException.badRequest("关联数据不存在");
        }
        if (!parentId.equals(values.get(0))) {
            throw BizException.badRequest(message);
        }
    }

    /** 写操作：院方强制锁定本院；区域等范围校验医院归属 */
    private void applyHospitalForWrite(ResourceDef def, LoginUser user, Map<String, Object> body) {
        applyHospitalForWrite(def, user, body, true);
    }

    private void applyHospitalForWrite(ResourceDef def, LoginUser user, Map<String, Object> body, boolean requireHospital) {
        if (!"biz_hospital".equals(def.getTable()) && !def.isHospitalScoped()) return;
        List<Long> allowed = dataScope.visibleHospitalIds(user);
        if (allowed == null) return;
        if (allowed.isEmpty()) throw BizException.forbidden("当前账号没有可操作的医院");
        Long hospitalId = longOrNull(body.get("hospitalId"));
        if (hospitalId == null) {
            if (allowed.size() == 1) {
                body.put("hospitalId", allowed.get(0));
            } else if (requireHospital) {
                throw BizException.badRequest("请选择医院");
            }
            return;
        }
        if (!allowed.contains(hospitalId)) {
            throw BizException.forbidden("无权操作所选医院的数据");
        }
        if (allowed.size() == 1) {
            body.put("hospitalId", allowed.get(0));
        }
    }

    private List<Long> scopeIds(ResourceDef def, LoginUser user) {
        if (!def.isHospitalScoped() && !"biz_hospital".equals(def.getTable())) return null;
        return dataScope.visibleHospitalIds(user);
    }

    private List<Long> scopeIdsForOptions(ResourceDef def, LoginUser user) {
        return scopeIds(def, user);
    }

    private String hospitalColumn(ResourceDef def) {
        return "biz_hospital".equals(def.getTable()) ? def.getPk() : "hospital_id";
    }

    private String selectColumns(ResourceDef def) {
        StringBuilder sb = new StringBuilder();
        for (FieldDef f : def.getFields()) {
            if (f.isVirtual()) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append("e.").append(f.getColumn()).append(" as `").append(f.getName()).append("`");
        }
        if (!def.getFields().stream().anyMatch(f -> f.getColumn().equals(def.getPk()))) {
            sb.append(", e.").append(def.getPk()).append(" as `").append(def.getPk()).append("`");
        }
        if (def.getJoinSelect() != null) {
            sb.append(", ").append(def.getJoinSelect());
        }
        return sb.toString();
    }

    private boolean isPk(ResourceDef def, FieldDef f) {
        return f.getColumn().equals(def.getPk());
    }

    private void check(ResourceDef def, String action) {
        LoginUser user = AuthContext.require();
        if (!user.hasPerm(def.getPermPrefix() + ":" + action)) {
            throw BizException.forbidden("无权限：" + def.getPermPrefix() + ":" + action);
        }
    }

    private void applyScopedResourceFilter(ResourceDef def, LoginUser user, StringBuilder where,
                                           List<Object> args, String alias) {
        if ("engineer".equals(def.getKey())) {
            if ("region".equals(user.getDataScope())) {
                applyRegionScope(where, args, alias, user);
            } else if ("self".equals(user.getDataScope()) && user.getEngineerId() != null) {
                where.append(" and ").append(alias).append("engineer_id = ? ");
                args.add(user.getEngineerId());
            }
        }
        if ("certificate".equals(def.getKey())) {
            if ("region".equals(user.getDataScope())) {
                List<Long> regionIds = dataScope.visibleRegionIds(user);
                if (regionIds != null && regionIds.isEmpty()) {
                    where.append(" and 1=0 ");
                } else if (regionIds != null) {
                    where.append(" and ").append(alias)
                            .append("engineer_id in (select engineer_id from biz_engineer where region_id in (")
                            .append(regionIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")))
                            .append(")) ");
                }
            } else if ("self".equals(user.getDataScope()) && user.getEngineerId() != null) {
                where.append(" and ").append(alias).append("engineer_id = ? ");
                args.add(user.getEngineerId());
            }
        }
        if ("companyRegion".equals(def.getKey()) && "region".equals(user.getDataScope())) {
            applyRegionScope(where, args, alias, user);
        }
    }

    private void applyRegionScope(StringBuilder where, List<Object> args, String alias, LoginUser user) {
        List<Long> regionIds = dataScope.visibleRegionIds(user);
        if (regionIds == null) return;
        if (regionIds.isEmpty()) {
            where.append(" and 1=0 ");
        } else {
            where.append(" and ").append(alias).append("region_id in (")
                    .append(regionIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")))
                    .append(") ");
        }
    }

    private void applyCompanyFilter(ResourceDef def, LoginUser user, StringBuilder where,
                                    List<Object> args, String alias) {
        Long companyId = dataScope.companyId(user);
        if (companyId == null) return;
        String key = def.getKey();
        if (def.isCompanyScoped()) {
            where.append(" and ").append(alias).append("company_id = ? ");
            args.add(companyId);
        } else if ("certificate".equals(key)) {
            where.append(" and ").append(alias)
                    .append("engineer_id in (select engineer_id from biz_engineer where company_id = ?) ");
            args.add(companyId);
        } else if ("monitorPoint".equals(key)) {
            List<Long> ids = contractScope.equipmentIdsForCompany(companyId);
            if (ids.isEmpty()) {
                where.append(" and 1=0 ");
            } else {
                where.append(" and ").append(alias).append("equipment_id in (")
                        .append(joinIds(ids)).append(") ");
            }
        } else if (CUSTOMER_KEYS.contains(key)) {
            List<Long> ids = contractScope.hospitalIdsForCompany(companyId);
            if (ids.isEmpty()) {
                where.append(" and 1=0 ");
            } else {
                where.append(" and ").append(alias).append("hospital_id in (")
                        .append(joinIds(ids)).append(") ");
            }
        }
    }

    private void applyResourceWriteScope(ResourceDef def, LoginUser user, Map<String, Object> body) {
        Long companyId = dataScope.companyId(user);
        if (companyId != null) {
            String key = def.getKey();
            if (def.isCompanyScoped()) {
                body.put("companyId", companyId);
            }
            if ("certificate".equals(key)) {
                Long engineerId = longOrNull(body.get("engineerId"));
                if (engineerId != null) {
                    List<Long> companies = jdbc.queryForList(
                            "select company_id from biz_engineer where engineer_id=?", Long.class, engineerId);
                    Long engineerCompany = companies.isEmpty() ? null : companies.get(0);
                    if (!companyId.equals(engineerCompany)) {
                        throw BizException.forbidden("只能管理本公司工程师的证书");
                    }
                }
            }
            if ("monitorPoint".equals(key)) {
                Long equipmentId = longOrNull(body.get("equipmentId"));
                if (equipmentId == null || !contractScope.coversEquipment(companyId, equipmentId)) {
                    throw BizException.forbidden("只能维护本公司合同覆盖设备的点位");
                }
            }
        }
        if ("region".equals(user.getDataScope()) && "companyRegion".equals(def.getKey())) {
            if (user.getRegionId() == null) throw BizException.forbidden("当前账号未绑定区域");
            body.put("regionId", user.getRegionId());
        }
        if (!"region".equals(user.getDataScope())) return;
        if ("engineer".equals(def.getKey())) {
            if (user.getRegionId() == null) throw BizException.forbidden("当前账号未绑定区域");
            body.put("regionId", user.getRegionId());
        }
        if ("certificate".equals(def.getKey())) {
            Long engineerId = longOrNull(body.get("engineerId"));
            if (engineerId == null) return;
            List<Long> regions = jdbc.queryForList(
                    "select region_id from biz_engineer where engineer_id=?", Long.class, engineerId);
            Long engineerRegion = regions.isEmpty() ? null : regions.get(0);
            if (user.getRegionId() == null || !user.getRegionId().equals(engineerRegion)) {
                throw BizException.forbidden("只能管理本区域工程师的证书");
            }
        }
    }

    private void checkScope(ResourceDef def, Map<String, Object> row) {
        LoginUser user = AuthContext.require();
        Long companyId = dataScope.companyId(user);
        if (companyId != null) {
            String key = def.getKey();
            if (def.isCompanyScoped()) {
                Object cid = row.get("companyId");
                if (cid == null || !companyId.equals(((Number) cid).longValue())) {
                    throw BizException.forbidden("无权访问其他公司数据");
                }
            } else if ("certificate".equals(key)) {
                Object eid = row.get("engineerId");
                List<Long> companies = eid == null ? List.of() : jdbc.queryForList(
                        "select company_id from biz_engineer where engineer_id=?", Long.class,
                        ((Number) eid).longValue());
                Long engineerCompany = companies.isEmpty() ? null : companies.get(0);
                if (engineerCompany == null || !companyId.equals(engineerCompany)) {
                    throw BizException.forbidden("无权访问其他公司工程师证书");
                }
            } else if ("monitorPoint".equals(key)) {
                Object eid = row.get("equipmentId");
                if (eid == null || !contractScope.coversEquipment(companyId, ((Number) eid).longValue())) {
                    throw BizException.forbidden("无权访问其他公司设备");
                }
            } else if (CUSTOMER_KEYS.contains(key)) {
                Object hid = row.get("hospitalId");
                if (hid == null || !contractScope.hospitalIdsForCompany(companyId)
                        .contains(((Number) hid).longValue())) {
                    throw BizException.forbidden("无权访问其他公司客户数据");
                }
            }
        }
        if ("companyRegion".equals(def.getKey()) && "region".equals(user.getDataScope())) {
            Object rid = row.get("regionId");
            if (rid == null || !dataScope.canAccessRegion(user, ((Number) rid).longValue())) {
                throw BizException.forbidden("无权访问其他区域的服务关系");
            }
        }
        if ("region".equals(def.getKey()) && "region".equals(user.getDataScope())) {
            Object rid = row.get("regionId");
            if (rid == null || !dataScope.canAccessRegion(user, ((Number) rid).longValue())) {
                throw BizException.forbidden("无权访问其他区域数据");
            }
        }
        if ("engineer".equals(def.getKey())) {
            Object rid = row.get("regionId");
            if ("region".equals(user.getDataScope()) && (rid == null
                    || !dataScope.canAccessRegion(user, ((Number) rid).longValue()))) {
                throw BizException.forbidden("无权访问其他区域工程师");
            }
            Object eid = row.get("engineerId");
            if ("self".equals(user.getDataScope())
                    && (user.getEngineerId() == null || eid == null
                    || !user.getEngineerId().equals(((Number) eid).longValue()))) {
                throw BizException.forbidden("无权访问其他工程师");
            }
        }
        if ("certificate".equals(def.getKey())) {
            Object eid = row.get("engineerId");
            List<Long> regions = eid == null ? List.of() : jdbc.queryForList(
                    "select region_id from biz_engineer where engineer_id=?", Long.class, ((Number) eid).longValue());
            Long engineerRegion = regions.isEmpty() ? null : regions.get(0);
            if ("region".equals(user.getDataScope()) && (engineerRegion == null
                    || !dataScope.canAccessRegion(user, engineerRegion))) {
                throw BizException.forbidden("无权访问其他区域工程师证书");
            }
            if ("self".equals(user.getDataScope())
                    && (user.getEngineerId() == null || eid == null
                    || !user.getEngineerId().equals(((Number) eid).longValue()))) {
                throw BizException.forbidden("无权访问其他工程师证书");
            }
        }
        if (!def.isHospitalScoped()) return;
        Object hid = row.get("hospitalId");
        if (hid == null) return;
        if (!dataScope.canAccessHospital(user, ((Number) hid).longValue())) {
            throw BizException.forbidden("无权访问其他医院数据");
        }
    }

    private Object normalize(Object v) {
        if (v instanceof String s && s.isBlank()) return null;
        return v;
    }

    private Object convert(FieldDef f, String v) {
        if ("number".equals(f.getType())) {
            return Double.parseDouble(v);
        }
        return v;
    }

    private int intVal(String v, int def) {
        try {
            return v == null ? def : Integer.parseInt(v);
        } catch (Exception e) {
            return def;
        }
    }

    private void validateSpecialReferences(ResourceDef def, Map<String, Object> body, Long id) {
        if ("region".equals(def.getKey())) {
            Object managerValue = body.containsKey("managerId") ? body.get("managerId") : body.get("manager_id");
            if (managerValue != null && !String.valueOf(managerValue).isBlank()) {
                Long managerId = Long.valueOf(String.valueOf(managerValue));
                Integer bound = jdbc.queryForObject(
                        "select count(*) from biz_engineer where user_id=?", Integer.class, managerId);
                if (bound == null || bound == 0) {
                    throw BizException.badRequest("区域负责人必须先绑定工程师档案");
                }
                Integer occupied = jdbc.queryForObject(
                        "select count(*) from biz_region where manager_id=? and region_id<>?", Integer.class,
                        managerId, id == null ? 0L : id);
                if (occupied != null && occupied > 0) {
                    throw BizException.badRequest("该工程师已经是其他区域的负责人");
                }
            }
            Object parentValue = body.containsKey("parentId") ? body.get("parentId") : body.get("parent_id");
            if (parentValue != null && !String.valueOf(parentValue).isBlank()) {
                Long parentId = Long.valueOf(String.valueOf(parentValue));
                if (id != null && parentId.equals(id)) throw BizException.badRequest("上级区域不能是自己");
                List<Map<String, Object>> parent = jdbc.queryForList(
                        "select company_id from biz_region where region_id=?", parentId);
                if (parent.isEmpty()) throw BizException.badRequest("上级区域不存在");
                Object companyId = body.containsKey("companyId") ? body.get("companyId") : body.get("company_id");
                if (companyId != null && parent.get(0).get("company_id") != null
                        && !String.valueOf(companyId).equals(String.valueOf(parent.get(0).get("company_id")))) {
                    throw BizException.badRequest("上级区域必须属于同一公司");
                }
                Long current = parentId;
                int guard = 0;
                while (current != null && guard++ < 20) {
                    if (id != null && current.equals(id)) throw BizException.badRequest("上级区域不能形成循环");
                    List<Map<String, Object>> row = jdbc.queryForList(
                            "select parent_id from biz_region where region_id=?", current);
                    if (row.isEmpty()) break;
                    Object next = row.get(0).get("parent_id");
                    current = next == null ? null : ((Number) next).longValue();
                }
            }
        }
        if ("company".equals(def.getKey())) {
            Object totalValue = body.containsKey("totalManagerId") ? body.get("totalManagerId") : body.get("total_manager_id");
            if (totalValue != null && !String.valueOf(totalValue).isBlank()) {
                Long total = Long.valueOf(String.valueOf(totalValue));
                Integer bound = jdbc.queryForObject(
                        "select count(*) from biz_engineer where user_id=?", Integer.class, total);
                if (bound == null || bound == 0) {
                    throw BizException.badRequest("总负责人必须先绑定工程师档案");
                }
                Integer roleCount = jdbc.queryForObject(
                        "select count(*) from sys_user_role ur join sys_role r on r.role_id=ur.role_id " +
                                "where ur.user_id=? and r.role_key='maintenance_manager'", Integer.class, total);
                if (roleCount == null || roleCount == 0) {
                    throw BizException.badRequest("总负责人必须具有维保经理角色");
                }
                Integer regionCount = jdbc.queryForObject(
                        "select count(*) from biz_region where manager_id=?", Integer.class, total);
                if (regionCount != null && regionCount > 0) {
                    throw BizException.badRequest("总负责人不能同时担任区域负责人");
                }
            }
        }
    }

    private void validateEngineerOnDuty(ResourceDef def, Map<String, Object> body, Long id) {
        if (!"engineer".equals(def.getKey())) return;
        Object value = body.containsKey("onDuty") ? body.get("onDuty") : body.get("on_duty");
        if (value == null) return;
        boolean offDuty = "0".equals(String.valueOf(value)) || "false".equalsIgnoreCase(String.valueOf(value));
        if (!offDuty) return;
        Integer active = jdbc.queryForObject(
                "select count(*) from biz_workorder where engineer_id=? and status in ('0','1','2','3','4')",
                Integer.class, id);
        if (active != null && active > 0) {
            throw BizException.badRequest("该工程师还有 " + active + " 张在手单，请先改派后再离岗");
        }
    }

    private void checkCustomerWritable(ResourceDef def, LoginUser user) {        if (CUSTOMER_KEYS.contains(def.getKey()) && dataScope.companyId(user) != null) {
            throw BizException.forbidden("客户主数据由平台管理员维护");
        }
    }

    private String joinIds(List<Long> ids) {
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private Long longOrNull(Object o) {
        if (o == null || String.valueOf(o).isBlank()) return null;
        return Long.valueOf(String.valueOf(o));
    }
}
