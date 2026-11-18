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
import java.util.concurrent.ConcurrentHashMap;
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
    private final Map<String, Integer> columnLengthCache = new ConcurrentHashMap<>();

    private static final List<String> CUSTOMER_KEYS =
            List.of("hospital", "campus", "department", "contact");

    private static final Map<String, String[][]> REFERENCES = Map.ofEntries(
            Map.entry("hospital", new String[][]{
                    ref("biz_campus", "hospital_id", "院区"),
                    ref("biz_department", "hospital_id", "科室"),
                    ref("biz_contact", "hospital_id", "对接人"),
                    ref("biz_equipment", "hospital_id", "设备"),
                    ref("biz_contract", "hospital_id", "合同"),
                    ref("biz_workorder", "hospital_id", "工单"),
                    ref("biz_hospital_user", "hospital_id", "院方账号")}),
            Map.entry("region", new String[][]{ref("biz_hospital", "region_id", "医院")}),
            Map.entry("campus", new String[][]{
                    ref("biz_department", "campus_id", "科室"),
                    ref("biz_contact", "campus_id", "对接人"),
                    ref("biz_equipment", "campus_id", "设备"),
                    ref("biz_workorder", "campus_id", "工单")}),
            Map.entry("department", new String[][]{
                    ref("biz_contact", "department_id", "对接人"),
                    ref("biz_equipment", "department_id", "设备"),
                    ref("biz_workorder", "department_id", "工单")}),
            Map.entry("equipment", new String[][]{
                    ref("biz_monitor_point", "equipment_id", "监测点位"),
                    ref("biz_workorder", "equipment_id", "工单"),
                    ref("biz_alarm", "equipment_id", "告警"),
                    ref("biz_document", "equipment_id", "文档")}),
            Map.entry("deviceType", new String[][]{
                    ref("biz_device_type", "parent_id", "子类型"),
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
        for (FieldDef f : def.getFields()) {
            if (!f.isVirtual() && f.getMaxLength() == null) {
                Integer max = columnMaxLength(def.getTable(), f.getColumn());
                if (max != null) f.setMaxLength(max);
            }
        }
        meta.put("fields", def.getFields());
        return meta;
    }

    private Integer columnMaxLength(String table, String column) {
        if (table == null || column == null || column.contains(".")) return null;
        String key = table + "." + column;
        return columnLengthCache.computeIfAbsent(key, k -> {
            try {
                Integer max = jdbc.queryForObject(
                        "select character_maximum_length from information_schema.columns " +
                                "where table_schema=database() and table_name=? and column_name=?",
                        Integer.class, table, column);
                return max != null && max > 0 && max <= 4000 ? max : null;
            } catch (Exception e) {
                return null;
            }
        });
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
            } else if ("equipment".equals(def.getKey()) && "deviceTypeId".equals(f.getName())) {
                List<Long> typeIds = deviceTypeDescendants(Long.valueOf(v.trim()));
                if (typeIds.isEmpty()) {
                    where.append(" and 1=0 ");
                } else {
                    where.append(" and e.device_type_id in (")
                            .append(typeIds.stream().map(String::valueOf).collect(Collectors.joining(",")))
                            .append(") ");
                }
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
        Map<String, Object> row = list.get(0);
        if ("engineer".equals(key)) {
            row.put("skillLevels", jdbc.queryForList(
                    "select es.skill_id as `skillId`, s.skill_name as `skillName`, coalesce(es.level, '3') as level " +
                            "from biz_engineer_skill es join biz_skill s on s.skill_id=es.skill_id " +
                            "where es.engineer_id=? order by s.skill_id", id));
        }
        if ("maintenancePlan".equals(key)) {
            row.put("equipmentIds", jdbc.queryForList(
                    "select equipment_id from biz_maintenance_plan_equipment where plan_id=? order by id",
                    Long.class, id));
        }
        if ("inspectionPlan".equals(key)) {
            row.put("equipmentIds", jdbc.queryForList(
                    "select equipment_id from biz_inspection_plan_equipment where plan_id=? order by id",
                    Long.class, id));
        }
        if ("alarmRule".equals(key)) {
            List<String> raw = jdbc.queryForList(
                    "select required_skill_ids from biz_alarm_rule where rule_id=?", String.class, id);
            List<Long> ids = new ArrayList<>();
            if (!raw.isEmpty() && raw.get(0) != null) {
                for (String part : raw.get(0).split(",")) {
                    if (!part.isBlank()) ids.add(Long.valueOf(part.trim()));
                }
            }
            row.put("requiredSkillIds", ids);
        }
        if ("hospital".equals(key)) {
            row.put("companyIds", jdbc.queryForList(
                    "select company_id from biz_company_hospital where hospital_id=? order by id",
                    Long.class, id));
        }
        if ("campus".equals(key)) {
            row.put("companyIds", jdbc.queryForList(
                    "select company_id from biz_company_campus where campus_id=? order by id",
                    Long.class, id));
        }
        return row;
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
        if ("engineer".equals(def.getKey())) {
            Object teamTitle = body.get("teamTitle");
            if (teamTitle == null || String.valueOf(teamTitle).isBlank()) {
                body.put("teamTitle", "维保工程师");
            }
        }
        autoCampusCode(def, body);
        autoDepartmentCode(def, body);
        applyDepartmentContact(def, body, null);
        if ("company".equals(def.getKey())) {
            companyCodeService.validateNewCode(String.valueOf(body.get("companyCode")), null);
        }
        validateHierarchy(body);
        validateContract(def, body);
        validateContractManager(def, body);
        validateSpecialReferences(def, body, null);
        validateDeviceTypeParent(def, body, null);
        validateDeviceTypeMonitorInheritance(def, body, null);
        applyEquipmentMonitorAccess(def, body, null);
        validateMonitorRule(def, body, null);
        validateAlarmRule(def, body);
        validatePlanAssignee(def, body, null);
        validateInputFormat(def, body);
        clearOtherPrimary(def, body, null);

        List<String> cols = new ArrayList<>();
        List<Object> vals = new ArrayList<>();
        for (FieldDef f : def.getFields()) {
            if (!f.isForm() || isPk(def, f) || f.isVirtual()) continue;
            boolean autoCampusCode = "campus".equals(def.getKey()) && "campusCode".equals(f.getName());
            boolean autoDeptCode = "department".equals(def.getKey()) && "deptCode".equals(f.getName());
            boolean autoPointCode = "monitorPoint".equals(def.getKey()) && "pointCode".equals(f.getName());
            if (autoPointCode && (body.get("pointCode") == null || String.valueOf(body.get("pointCode")).isBlank())) {
                body.put("pointCode", nextMonitorPointCode(body));
            }
            if (f.isReadOnly() && !autoCampusCode && !autoDeptCode && !autoPointCode) continue;
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
        if ("document".equals(def.getKey())) {
            cols.add("upload_time");
            vals.add(LocalDateTime.now());
            cols.add("uploader_name");
            vals.add(user.getNickName());
        }
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
        syncEngineerUserDept(def, id);
        syncEngineerSkills(def, id, body);
        syncAlarmRuleSkills(def, id, body);
        syncMaintenancePlanEquipments(def, id, body);
        syncHospitalCompanies(def, id, body);
        syncCompanyCampus(def, id, body);
        syncCampusCompanies(def, id, body);
        syncRegionManagerRole(def, id, null);
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
        validateDeviceTypeParent(def, body, id);
        validateDeviceTypeMonitorInheritance(def, body, id);
        applyEquipmentMonitorAccess(def, body, id);
        validateMonitorRule(def, body, id);
        validateAlarmRule(def, body);
        validatePlanAssignee(def, body, id);
        validateInputFormat(def, body);
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
        applyDepartmentContact(def, merged, id);
        if ("department".equals(def.getKey()) && body.containsKey("contactId")) {
            body.put("director", merged.get("director"));
            body.put("phone", merged.get("phone"));
        }
        applyResourceWriteScope(def, user, body);
        applyResourceWriteScope(def, user, merged);
        validateHierarchy(merged);
        validateContract(def, merged);
        validateContractManager(def, merged);
        clearOtherPrimary(def, body, id);

        List<String> sets = new ArrayList<>();
        List<Object> vals = new ArrayList<>();
        for (FieldDef f : def.getFields()) {
            if (!f.isForm() || isPk(def, f) || f.isReadOnly() || f.isVirtual()) continue;
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
        syncEngineerUserDept(def, id);
        syncEngineerSkills(def, id, body);
        syncAlarmRuleSkills(def, id, body);
        syncDeviceTypeMonitorState(def, id, body);
        syncMaintenancePlanEquipments(def, id, body);
        if (body.containsKey("companyIds")) syncHospitalCompanies(def, id, body);
        syncCompanyCampus(def, id, body);
        if (body.containsKey("companyIds")) syncCampusCompanies(def, id, body);
        Long oldRegionManager = old.get("managerId") instanceof Number n ? n.longValue() : null;
        syncRegionManagerRole(def, id, oldRegionManager);
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
        if ("hospital".equals(def.getKey()) && dataScope.companyId(AuthContext.require()) != null) {
            throw BizException.forbidden("医院主数据只能由平台管理员删除");
        }
        for (Long id : ids) {
            Long oldRegionManager = null;
            if (def.isHospitalScoped() || "region".equals(def.getKey())) {
                Map<String, Object> old = get(key, id);
                checkScope(def, old);
                if ("region".equals(def.getKey()) && old.get("managerId") instanceof Number n) {
                    oldRegionManager = n.longValue();
                }
            }
            checkReferences(def, id);
            jdbc.update("delete from " + def.getTable() + " where " + def.getPk() + " = ?", id);
            if ("hospital".equals(def.getKey())) {
                jdbc.update("delete from biz_company_hospital where hospital_id=?", id);
            }
            if (oldRegionManager != null) removeRegionalRoleIfUnused(oldRegionManager);
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
        if ("gateway".equals(key)) {
            LoginUser user = AuthContext.require();
            String sql = "select gateway_id as value, gateway_name as label from biz_monitor_gateway where enabled='1' ";
            if (user.getCompanyId() == null) {
                return jdbc.queryForList(sql + "order by gateway_id");
            }
            return jdbc.queryForList(sql + "and company_id=? order by gateway_id", user.getCompanyId());
        }
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
        if ("hospital".equals(def.getKey()) && user.isHospitalUser() && user.getHospitalId() != null) {
            where.append(" and hospital_id = ? ");
            args.add(user.getHospitalId());
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
        if ("skill".equals(def.getKey())) {
            return jdbc.queryForList("select " + def.getPk() + " as value, " +
                    def.getNameColumn() + " as label, category from " + def.getTable() + where +
                    " order by category, " + def.getPk(), args.toArray());
        }
        if ("deviceType".equals(def.getKey())) {
            return deviceTypeOptions(def, where, args);
        }
        return jdbc.queryForList("select " + def.getPk() + " as value, " + def.getNameColumn() +
                " as label from " + def.getTable() + where + " order by " + def.getPk(), args.toArray());
    }

    private List<Long> deviceTypeDescendants(Long typeId) {
        List<Long> result = new ArrayList<>();
        if (typeId == null) return result;
        result.add(typeId);
        int index = 0;
        while (index < result.size() && result.size() < 500) {
            Long current = result.get(index++);
            result.addAll(jdbc.queryForList(
                    "select type_id from biz_device_type where parent_id=?", Long.class, current));
        }
        return result;
    }

    private List<Map<String, Object>> deviceTypeOptions(ResourceDef def, StringBuilder where, List<Object> args) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select " + def.getPk() + " as `value`, parent_id as `parentId`, " +
                        def.getNameColumn() + " as `name` from " + def.getTable() + where +
                        " order by sort, " + def.getPk(), args.toArray());
        Map<Long, Map<String, Object>> byId = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            byId.put(((Number) row.get("value")).longValue(), row);
        }
        Map<Long, List<Map<String, Object>>> children = new LinkedHashMap<>();
        List<Map<String, Object>> roots = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object parent = row.get("parentId");
            Long parentId = parent == null ? null : ((Number) parent).longValue();
            if (parentId != null && byId.containsKey(parentId)) {
                children.computeIfAbsent(parentId, k -> new ArrayList<>()).add(row);
            } else {
                roots.add(row);
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        appendDeviceTypeOptions(roots, children, result, 0);
        return result;
    }

    private void appendDeviceTypeOptions(List<Map<String, Object>> nodes, Map<Long, List<Map<String, Object>>> children,
                                         List<Map<String, Object>> result, int depth) {
        if (nodes == null || depth > 20) return;
        for (Map<String, Object> node : nodes) {
            Long id = ((Number) node.get("value")).longValue();
            Map<String, Object> option = new LinkedHashMap<>();
            option.put("value", id);
            option.put("label", (depth == 0 ? "" : "　".repeat(depth) + "└ ") + node.get("name"));
            option.put("parentId", node.get("parentId"));
            option.put("level", depth);
            result.add(option);
            appendDeviceTypeOptions(children.get(id), children, result, depth + 1);
        }
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

    private void validateContractManager(ResourceDef def, Map<String, Object> data) {
        if (!"contract".equals(def.getKey())) return;
        Long managerId = longOrNull(data.get("managerId"));
        Long hospitalId = longOrNull(data.get("hospitalId"));
        if (managerId == null) return;
        if (hospitalId == null) throw BizException.badRequest("请先选择合同医院");
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select e.region_id as engineerRegion, e.company_id as engineerCompany, " +
                        "h.region_id as hospitalRegion " +
                        "from biz_engineer e join biz_hospital h on h.hospital_id=? where e.engineer_id=?",
                hospitalId, managerId);
        if (rows.isEmpty()) {
            throw BizException.badRequest("合同负责人必须是本公司工程师");
        }
        Map<String, Object> row = rows.get(0);
        Object engineerRegion = row.get("engineerRegion");
        Object hospitalRegion = row.get("hospitalRegion");
        Object engineerCompany = row.get("engineerCompany");
        Integer serves = engineerCompany == null ? 0 : jdbc.queryForObject(
                "select count(*) from biz_company_hospital where hospital_id=? and company_id=?",
                Integer.class, hospitalId, ((Number) engineerCompany).longValue());
        if (engineerRegion == null || hospitalRegion == null
                || !String.valueOf(engineerRegion).equals(String.valueOf(hospitalRegion))
                || serves == null || serves == 0) {
            throw BizException.badRequest("合同负责人必须是医院所属区域且与本院有合作关系的工程师");
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

    private void autoCampusCode(ResourceDef def, Map<String, Object> body) {
        if (!"campus".equals(def.getKey())) return;
        Object supplied = body.get("campusCode");
        if (supplied != null && !String.valueOf(supplied).isBlank()) return;
        Long hospitalId = longOrNull(body.get("hospitalId"));
        if (hospitalId == null) return;
        List<String> hospitalCodes = jdbc.queryForList(
                "select hospital_code from biz_hospital where hospital_id=?", String.class, hospitalId);
        if (hospitalCodes.isEmpty() || hospitalCodes.get(0) == null || hospitalCodes.get(0).isBlank()) return;
        Integer used = jdbc.queryForObject(
                "select count(*) from biz_campus where hospital_id=?", Integer.class, hospitalId);
        int seq = (used == null ? 0 : used) + 1;
        for (int i = seq; i < seq + 100; i++) {
            String code = hospitalCodes.get(0).trim().toUpperCase() + "-" + String.format("%02d", i);
            Integer exists = jdbc.queryForObject(
                    "select count(*) from biz_campus where campus_code=?", Integer.class, code);
            if (exists == null || exists == 0) {
                body.put("campusCode", code);
                return;
            }
        }
    }

    private void autoDepartmentCode(ResourceDef def, Map<String, Object> body) {
        if (!"department".equals(def.getKey())) return;
        Object supplied = body.get("deptCode");
        if (supplied != null && !String.valueOf(supplied).isBlank()) return;
        Long hospitalId = longOrNull(body.get("hospitalId"));
        if (hospitalId == null) return;
        List<String> hospitalCodes = jdbc.queryForList(
                "select hospital_code from biz_hospital where hospital_id=?", String.class, hospitalId);
        if (hospitalCodes.isEmpty() || hospitalCodes.get(0) == null || hospitalCodes.get(0).isBlank()) return;
        Integer used = jdbc.queryForObject(
                "select count(*) from biz_department where hospital_id=?", Integer.class, hospitalId);
        int seq = (used == null ? 0 : used) + 1;
        for (int i = seq; i < seq + 100; i++) {
            String code = hospitalCodes.get(0).trim().toUpperCase() + "-KS" + String.format("%02d", i);
            Integer exists = jdbc.queryForObject(
                    "select count(*) from biz_department where dept_code=?", Integer.class, code);
            if (exists == null || exists == 0) {
                body.put("deptCode", code);
                return;
            }
        }
    }

    private void applyDepartmentContact(ResourceDef def, Map<String, Object> body, Long departmentId) {
        if (!"department".equals(def.getKey())) return;
        Long contactId = longOrNull(body.get("contactId"));
        if (contactId == null) {
            if (departmentId != null && body.containsKey("contactId")) {
                body.put("director", null);
                body.put("phone", null);
            }
            return;
        }
        if (departmentId == null) {
            throw BizException.badRequest("保存科室后再选择对接人");
        }
        List<Map<String, Object>> contacts = jdbc.queryForList(
                "select contact_name, phone, hospital_id, department_id from biz_contact where contact_id=?",
                contactId);
        if (contacts.isEmpty()) throw BizException.badRequest("对接人不存在");
        Map<String, Object> contact = contacts.get(0);
        Object cid = contact.get("department_id");
        if (cid != null && !departmentId.equals(((Number) cid).longValue())) {
            throw BizException.badRequest("对接人不属于该科室");
        }
        Long hospitalId = longOrNull(body.get("hospitalId"));
        Object chid = contact.get("hospital_id");
        if (hospitalId != null && chid != null && !hospitalId.equals(((Number) chid).longValue())) {
            throw BizException.badRequest("对接人不属于该医院");
        }
        body.put("director", contact.get("contact_name"));
        body.put("phone", contact.get("phone"));
    }

    private void validateHierarchy(Map<String, Object> data) {
        Long hospitalId = longOrNull(data.get("hospitalId"));
        Long campusId = longOrNull(data.get("campusId"));
        Long departmentId = longOrNull(data.get("departmentId"));
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
        if ("hospital".equals(key)) {
            String hospitalColumn = alias == null || alias.isBlank()
                    ? def.getTable() + ".hospital_id" : alias + "hospital_id";
            where.append(" and exists (select 1 from biz_company_hospital ch where ch.hospital_id=")
                    .append(hospitalColumn).append(" and ch.company_id=?) ");
            args.add(companyId);
        } else if (def.isCompanyScoped()) {
            where.append(" and ").append(alias).append("company_id = ? ");
            args.add(companyId);
        } else if ("campus".equals(key)) {
            String campusColumn = alias == null || alias.isBlank()
                    ? def.getTable() + ".campus_id" : alias + "campus_id";
            where.append(" and exists (select 1 from biz_company_campus cc where cc.campus_id=")
                    .append(campusColumn).append(" and cc.company_id=?) ");
            args.add(companyId);
        } else if ("department".equals(key)) {
            where.append(" and ").append(alias)
                    .append("campus_id in (select campus_id from biz_company_campus where company_id = ?) ");
            args.add(companyId);
        } else if ("contact".equals(key)) {
            where.append(" and ").append(alias)
                    .append("campus_id in (select campus_id from biz_company_campus where company_id = ?) ");
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
            where.append(" and ").append(alias)
                    .append("hospital_id in (select hospital_id from biz_company_hospital where company_id = ?) ");
            args.add(companyId);
        }
    }

    private void applyResourceWriteScope(ResourceDef def, LoginUser user, Map<String, Object> body) {
        Long companyId = dataScope.companyId(user);
        if (companyId != null) {
            String key = def.getKey();
            if (def.isCompanyScoped()) {
                body.put("companyId", companyId);
            }
            if ("hospital".equals(key)) {
                Long editingId = longOrNull(body.get("hospitalId"));
                if (editingId != null) {
                    Integer relation = jdbc.queryForObject(
                            "select count(*) from biz_company_hospital where hospital_id=? and company_id=?",
                            Integer.class, editingId, companyId);
                    if (relation == null || relation == 0) {
                        throw BizException.forbidden("无权维护未服务该医院的医院档案");
                    }
                }
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
                if (equipmentId == null) {
                    Long pointId = longOrNull(body.get("pointId"));
                    if (pointId != null) {
                        List<Long> equipmentIds = jdbc.queryForList(
                                "select equipment_id from biz_monitor_point where point_id=?", Long.class, pointId);
                        equipmentId = equipmentIds.isEmpty() ? null : equipmentIds.get(0);
                    }
                }
                if (equipmentId == null || !contractScope.coversEquipment(companyId, equipmentId)) {
                    throw BizException.forbidden("只能维护本公司合同覆盖设备的点位");
                }
            }
            if (CUSTOMER_KEYS.contains(key) && !"hospital".equals(key)) {
                Long hospitalId = longOrNull(body.get("hospitalId"));
                if (hospitalId == null) throw BizException.forbidden("请选择本公司医院");
                Integer count = jdbc.queryForObject(
                        "select count(*) from biz_company_hospital where hospital_id=? and company_id=?",
                        Integer.class, hospitalId, companyId);
                if (count == null || count == 0) {
                    throw BizException.forbidden("只能维护本公司服务医院的客户数据");
                }
                if ("campus".equals(key)) {
                    Long campusId = longOrNull(body.get("campusId"));
                    if (campusId != null) {
                        Integer campusCount = jdbc.queryForObject(
                                "select count(*) from biz_company_campus where campus_id=? and company_id=?",
                                Integer.class, campusId, companyId);
                        if (campusCount == null || campusCount == 0) {
                            throw BizException.forbidden("无权维护该公司院区数据");
                        }
                    }
                }
            }
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
            if ("hospital".equals(key)) {
                Object hid = row.get("hospitalId");
                if (hid == null) throw BizException.forbidden("无权访问其他公司数据");
                Integer relation = jdbc.queryForObject(
                        "select count(*) from biz_company_hospital where hospital_id=? and company_id=?",
                        Integer.class, ((Number) hid).longValue(), companyId);
                if (relation == null || relation == 0) {
                    throw BizException.forbidden("无权访问其他公司数据");
                }
            } else if ("campus".equals(key)) {
                Object cid = row.get("campusId");
                if (cid == null) throw BizException.forbidden("无权访问其他公司数据");
                Integer relation = jdbc.queryForObject(
                        "select count(*) from biz_company_campus where campus_id=? and company_id=?",
                        Integer.class, ((Number) cid).longValue(), companyId);
                if (relation == null || relation == 0) {
                    throw BizException.forbidden("无权访问其他公司数据");
                }
            } else if (def.isCompanyScoped()) {
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
                if (hid == null) throw BizException.forbidden("无权访问其他公司客户数据");
                Long hospitalId = ((Number) hid).longValue();
                Integer relation = jdbc.queryForObject(
                        "select count(*) from biz_company_hospital where hospital_id=? and company_id=?",
                        Integer.class, hospitalId, companyId);
                if (relation == null || relation == 0) {
                    throw BizException.forbidden("无权访问其他公司客户数据");
                }
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

    private String nextMonitorPointCode(Map<String, Object> body) {
        Long equipmentId = body.get("equipmentId") == null ? null : Long.valueOf(String.valueOf(body.get("equipmentId")));
        String equipmentCode = "EQ";
        if (equipmentId != null) {
            List<String> codes = jdbc.queryForList(
                    "select equipment_code from biz_equipment where equipment_id=?", String.class, equipmentId);
            if (!codes.isEmpty() && codes.get(0) != null && !codes.get(0).isBlank()) {
                equipmentCode = codes.get(0);
            } else {
                equipmentCode = "EQ" + equipmentId;
            }
        }
        String metric = String.valueOf(body.getOrDefault("metric", ""));
        String suffix = switch (metric) {
            case "temperature" -> "TEMP";
            case "humidity" -> "HUM";
            case "pressure" -> "DP";
            case "wind_speed" -> "WIND";
            case "particle" -> "PART";
            case "oxygen_pressure" -> "O2P";
            case "vacuum" -> "VAC";
            case "run_state" -> "RUN";
            default -> metric.isBlank() ? "POINT" : metric.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        };
        String base = equipmentCode + "-" + suffix;
        if (base.length() > 50) base = base.substring(0, 50);
        String code = base;
        int seq = 2;
        while (jdbc.queryForObject("select count(*) from biz_monitor_point where point_code=?", Integer.class, code) > 0) {
            String suffixSeq = "-" + seq++;
            code = (base.length() + suffixSeq.length() > 50
                    ? base.substring(0, 50 - suffixSeq.length()) : base) + suffixSeq;
        }
        return code;
    }

    private void validatePlanAssignee(ResourceDef def, Map<String, Object> body, Long id) {
        if (!"inspectionPlan".equals(def.getKey()) && !"maintenancePlan".equals(def.getKey())) return;
        Long assigneeId = body.get("assigneeId") == null ? null : Long.valueOf(String.valueOf(body.get("assigneeId")));
        if (assigneeId == null) return;
        Long hospitalId = body.get("hospitalId") == null ? null : Long.valueOf(String.valueOf(body.get("hospitalId")));
        if (hospitalId == null && id != null) {
            List<Long> hospitals = jdbc.queryForList(
                    "select hospital_id from " + def.getTable() + " where " + def.getPk() + "=?", Long.class, id);
            hospitalId = hospitals.isEmpty() ? null : hospitals.get(0);
        }
        if (hospitalId == null) throw BizException.badRequest("请先选择医院");
        Integer matched = jdbc.queryForObject(
                "select count(*) from biz_engineer e join biz_hospital h on h.hospital_id=? " +
                        "where e.engineer_id=? and e.region_id=h.region_id and e.status='0' " +
                        "and exists (select 1 from biz_company_hospital ch where ch.hospital_id=h.hospital_id and ch.company_id=e.company_id)",
                Integer.class, hospitalId, assigneeId);
        if (matched == null || matched == 0) {
            throw BizException.badRequest("只能选择该医院所属区域且有合作关系的工程师");
        }
    }

    private void validateAlarmRule(ResourceDef def, Map<String, Object> body) {
        if (!"alarmRule".equals(def.getKey())) return;
        if (!body.containsKey("operator") && !body.containsKey("threshold") && !body.containsKey("threshold2")) return;
        String operator = body.get("operator") == null ? "" : String.valueOf(body.get("operator"));
        Object first = body.get("threshold");
        Object second = body.get("threshold2");
        if ("between".equals(operator)) {
            if (first == null || second == null) {
                throw BizException.badRequest("区间规则必须同时填写下限和上限");
            }
            java.math.BigDecimal low = new java.math.BigDecimal(String.valueOf(first));
            java.math.BigDecimal high = new java.math.BigDecimal(String.valueOf(second));
            if (low.compareTo(high) > 0) {
                body.put("threshold", high);
                body.put("threshold2", low);
            }
            return;
        }
        body.put("threshold2", null);
        if (!"offline".equals(operator) && (first == null || String.valueOf(first).isBlank())) {
            throw BizException.badRequest("请填写阈值");
        }
    }

    private void applyEquipmentMonitorAccess(ResourceDef def, Map<String, Object> body, Long id) {
        if (!"equipment".equals(def.getKey()) || !body.containsKey("monitorAccess")) return;
        boolean enabled = "1".equals(String.valueOf(body.get("monitorAccess")));
        Long typeId = body.get("deviceTypeId") == null ? null : Long.valueOf(String.valueOf(body.get("deviceTypeId")));
        if (typeId == null && id != null) {
            List<Long> ids = jdbc.queryForList(
                    "select device_type_id from biz_equipment where equipment_id=?", Long.class, id);
            typeId = ids.isEmpty() ? null : ids.get(0);
        }
        if (enabled && typeId != null && !monitorEnabled(typeId)) {
            throw BizException.badRequest("该设备类型未启用监测，不能接入");
        }
        body.put("monitorStatus", enabled ? "1" : "0");
    }

    private void validateMonitorRule(ResourceDef def, Map<String, Object> body, Long id) {
        if ("equipment".equals(def.getKey())) {
            Long typeId = body.get("deviceTypeId") == null ? null : Long.valueOf(String.valueOf(body.get("deviceTypeId")));
            if (typeId == null && id != null) {
                List<Long> ids = jdbc.queryForList(
                        "select device_type_id from biz_equipment where equipment_id=?", Long.class, id);
                typeId = ids.isEmpty() ? null : ids.get(0);
            }
            if (typeId != null && !monitorEnabled(typeId)) {
                body.put("monitorStatus", "0");
            }
            return;
        }
        if ("monitorPoint".equals(def.getKey())) {
            Long equipmentId = body.get("equipmentId") == null ? null : Long.valueOf(String.valueOf(body.get("equipmentId")));
            if (equipmentId == null && id != null) {
                List<Long> ids = jdbc.queryForList(
                        "select equipment_id from biz_monitor_point where point_id=?", Long.class, id);
                equipmentId = ids.isEmpty() ? null : ids.get(0);
            }
            if (equipmentId != null) {
                List<Long> typeIds = jdbc.queryForList(
                        "select device_type_id from biz_equipment where equipment_id=?", Long.class, equipmentId);
                if (!typeIds.isEmpty() && typeIds.get(0) != null && !monitorEnabled(typeIds.get(0))) {
                    throw BizException.badRequest("该设备类型未启用监测，不能添加监测点位");
                }
            }
        }
    }

    private boolean monitorEnabled(Long typeId) {
        Long current = typeId;
        int guard = 0;
        while (current != null && guard++ < 20) {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "select monitor_enabled, parent_id from biz_device_type where type_id=?", current);
            if (rows.isEmpty()) return false;
            Map<String, Object> row = rows.get(0);
            Object enabled = row.get("monitor_enabled");
            if (enabled != null && "0".equals(String.valueOf(enabled))) return false;
            Object parent = row.get("parent_id");
            current = parent == null ? null : ((Number) parent).longValue();
        }
        return true;
    }

    private void validateDeviceTypeMonitorInheritance(ResourceDef def, Map<String, Object> body, Long id) {
        if (!"deviceType".equals(def.getKey())) return;
        Object parentValue = body.containsKey("parentId") ? body.get("parentId") : body.get("parent_id");
        Long parentId = parentValue == null || String.valueOf(parentValue).isBlank()
                ? null : Long.valueOf(String.valueOf(parentValue));
        if (parentId == null && id != null) {
            List<Long> parents = jdbc.queryForList(
                    "select parent_id from biz_device_type where type_id=?", Long.class, id);
            parentId = parents.isEmpty() ? null : parents.get(0);
        }
        if (parentId != null && !monitorEnabled(parentId)) {
            boolean requestedOn = body.get("monitorEnabled") != null
                    && "1".equals(String.valueOf(body.get("monitorEnabled")));
            if (requestedOn) {
                List<String> names = jdbc.queryForList(
                        "select type_name from biz_device_type where type_id=?", String.class, parentId);
                String parentName = names.isEmpty() ? "上级类型" : names.get(0);
                throw BizException.badRequest("请先开启上级类型「" + parentName + "」的监测");
            }
            body.put("monitorEnabled", "0");
        }
    }

    private void syncDeviceTypeMonitorState(ResourceDef def, Long id, Map<String, Object> body) {
        if (!"deviceType".equals(def.getKey()) || id == null || !body.containsKey("monitorEnabled")) return;
        if (!"0".equals(String.valueOf(body.get("monitorEnabled")))) return;
        List<Long> typeIds = deviceTypeDescendants(id);
        if (typeIds.isEmpty()) return;
        String ids = typeIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        jdbc.update("update biz_device_type set monitor_enabled='0' where type_id in (" + ids + ")");
        jdbc.update("update biz_equipment set monitor_status='0' where device_type_id in (" + ids + ")");
        jdbc.update("update biz_monitor_point p join biz_equipment e on e.equipment_id=p.equipment_id " +
                "set p.enabled='0' where e.device_type_id in (" + ids + ")");
    }

    private void validateDeviceTypeParent(ResourceDef def, Map<String, Object> body, Long id) {
        if (!"deviceType".equals(def.getKey())) return;
        Object parentValue = body.containsKey("parentId") ? body.get("parentId") : body.get("parent_id");
        if (parentValue == null || String.valueOf(parentValue).isBlank()) return;
        Long parentId = Long.valueOf(String.valueOf(parentValue));
        if (id != null && parentId.equals(id)) throw BizException.badRequest("上级类型不能是自己");
        List<Map<String, Object>> parent = jdbc.queryForList(
                "select company_id, parent_id from biz_device_type where type_id=?", parentId);
        if (parent.isEmpty()) throw BizException.badRequest("上级类型不存在");
        Object companyValue = body.containsKey("companyId") ? body.get("companyId") : body.get("company_id");
        if (companyValue == null && id != null) {
            List<Long> companies = jdbc.queryForList(
                    "select company_id from biz_device_type where type_id=?", Long.class, id);
            companyValue = companies.isEmpty() ? null : companies.get(0);
        }
        Object parentCompany = parent.get(0).get("company_id");
        if (companyValue != null && parentCompany != null
                && !String.valueOf(companyValue).equals(String.valueOf(parentCompany))) {
            throw BizException.badRequest("上级类型必须属于同一公司");
        }
        Long current = parentId;
        int guard = 0;
        while (current != null && guard++ < 20) {
            if (id != null && current.equals(id)) throw BizException.badRequest("上级类型不能形成循环");
            List<Map<String, Object>> row = jdbc.queryForList(
                    "select parent_id from biz_device_type where type_id=?", current);
            if (row.isEmpty()) break;
            Object next = row.get(0).get("parent_id");
            current = next == null ? null : ((Number) next).longValue();
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

    private void validateInputFormat(ResourceDef def, Map<String, Object> body) {
        for (FieldDef f : def.getFields()) {
            if (!f.isForm() || f.isReadOnly()) continue;
            Object raw = body.get(f.getName());
            if (raw == null) continue;
            String value = String.valueOf(raw).trim();
            if (value.isBlank()) continue;
            Integer dbMax = f.isVirtual() ? null : columnMaxLength(def.getTable(), f.getColumn());
            Integer max = f.getMaxLength();
            if (dbMax != null && (max == null || dbMax < max)) max = dbMax;
            if (max != null && value.length() > max) {
                throw BizException.badRequest(f.getLabel() + "不能超过 " + max + " 个字符");
            }
            if (f.getPattern() != null && !value.matches(f.getPattern())) {
                throw BizException.badRequest(f.getValidationMessage() == null
                        ? f.getLabel() + "格式不正确" : f.getValidationMessage());
            }
            if ("number".equals(f.getType()) && (f.getMinValue() != null || f.getMaxValue() != null)) {
                double number;
                try {
                    number = new java.math.BigDecimal(value).doubleValue();
                } catch (NumberFormatException e) {
                    throw BizException.badRequest(f.getLabel() + "必须是数字");
                }
                if (f.getMinValue() != null && number < f.getMinValue()) {
                    throw BizException.badRequest(f.getLabel() + "不能小于 " + f.getMinValue());
                }
                if (f.getMaxValue() != null && number > f.getMaxValue()) {
                    throw BizException.badRequest(f.getLabel() + "不能大于 " + f.getMaxValue());
                }
            }
        }
    }

    private void syncRegionManagerRole(ResourceDef def, Long regionId, Long oldManagerId) {
        if (!"region".equals(def.getKey()) || regionId == null) return;
        Object value = jdbc.queryForObject("select manager_id from biz_region where region_id=?", Object.class, regionId);
        Long newManagerId = value instanceof Number n ? n.longValue() : null;
        if (oldManagerId != null && !oldManagerId.equals(newManagerId)) {
            removeRegionalRoleIfUnused(oldManagerId);
        }
        if (newManagerId != null) {
            jdbc.update("insert ignore into sys_user_role(user_id,role_id) values(?,101)", newManagerId);
        }
        syncRegionDepartmentLeader(regionId, newManagerId);
    }

    private void syncRegionDepartmentLeader(Long regionId, Long managerId) {
        String leader = null;
        if (managerId != null) {
            List<String> names = jdbc.queryForList(
                    "select nick_name from sys_user where user_id=?", String.class, managerId);
            leader = names.isEmpty() ? null : names.get(0);
        }
        jdbc.update("update sys_dept set leader=? where region_id=?", leader, regionId);
    }

    private void removeRegionalRoleIfUnused(Long userId) {
        if (userId == null) return;
        Integer count = jdbc.queryForObject(
                "select count(*) from biz_region where manager_id=?", Integer.class, userId);
        if (count == null || count == 0) {
            jdbc.update("delete from sys_user_role where user_id=? and role_id=101", userId);
        }
    }

    private void syncMaintenancePlanEquipments(ResourceDef def, Long planId, Map<String, Object> body) {
        if (!"maintenancePlan".equals(def.getKey()) && !"inspectionPlan".equals(def.getKey())) return;
        if (planId == null || !body.containsKey("equipmentIds")) return;
        boolean maintenance = "maintenancePlan".equals(def.getKey());
        String table = maintenance ? "biz_maintenance_plan_equipment" : "biz_inspection_plan_equipment";
        jdbc.update("delete from " + table + " where plan_id=?", planId);
        Object value = body.get("equipmentIds");
        if (!(value instanceof List<?> list) || list.isEmpty()) {
            if (maintenance) jdbc.update("update biz_maintenance_plan set equipment_id=null where plan_id=?", planId);
            return;
        }
        Long first = null;
        for (Object item : list) {
            if (item == null || String.valueOf(item).isBlank()) continue;
            Long equipmentId = Long.valueOf(String.valueOf(item));
            if (first == null) first = equipmentId;
            jdbc.update("insert ignore into " + table + "(plan_id,equipment_id) values(?,?)", planId, equipmentId);
        }
        if (maintenance) jdbc.update("update biz_maintenance_plan set equipment_id=? where plan_id=?", first, planId);
    }

    private void syncHospitalCompanies(ResourceDef def, Long hospitalId, Map<String, Object> body) {
        if (!"hospital".equals(def.getKey()) || hospitalId == null) return;
        if (!body.containsKey("companyIds")) return;
        LoginUser user = AuthContext.require();
        Long ownCompany = dataScope.companyId(user);
        java.util.Set<Long> companyIds = new java.util.LinkedHashSet<>();
        if (body.get("companyIds") instanceof List<?> list) {
            for (Object item : list) {
                if (item != null && !String.valueOf(item).isBlank()) {
                    companyIds.add(Long.valueOf(String.valueOf(item)));
                }
            }
        }
        if (ownCompany != null) {
            companyIds.clear();
            companyIds.addAll(jdbc.queryForList(
                    "select company_id from biz_company_hospital where hospital_id=?", Long.class, hospitalId));
            companyIds.add(ownCompany);
        }
        if (companyIds.isEmpty()) return;
        List<Long> hospitalRegions = jdbc.queryForList(
                "select region_id from biz_hospital where hospital_id=?", Long.class, hospitalId);
        Long regionId = hospitalRegions.isEmpty() ? null : hospitalRegions.get(0);
        jdbc.update("delete from biz_company_hospital where hospital_id=?", hospitalId);
        for (Long companyId : companyIds) {
            jdbc.update("insert ignore into biz_company_hospital(company_id,hospital_id,region_id,service_status,start_date) " +
                            "values(?,?,?,'0',curdate())",
                    companyId, hospitalId, regionId);
            jdbc.update("insert ignore into biz_company_campus(company_id,campus_id,hospital_id,service_status,start_date) " +
                            "select ?, campus_id, hospital_id, '0', curdate() from biz_campus where hospital_id=?",
                    companyId, hospitalId);
        }
        jdbc.update("delete from biz_company_campus where hospital_id=? and company_id not in (" +
                        String.join(",", companyIds.stream().map(String::valueOf).toList()) + ")", hospitalId);
    }

    private void syncCompanyCampus(ResourceDef def, Long campusId, Map<String, Object> body) {
        if (!"campus".equals(def.getKey()) || campusId == null) return;
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select hospital_id from biz_campus where campus_id=?", campusId);
        if (rows.isEmpty()) return;
        Long hospitalId = ((Number) rows.get(0).get("hospital_id")).longValue();
        LoginUser user = AuthContext.require();
        Long ownCompany = dataScope.companyId(user);
        if (ownCompany != null) {
            jdbc.update("insert ignore into biz_company_campus(company_id,campus_id,hospital_id,service_status,start_date) " +
                            "values(?,?,?,'0',curdate())",
                    ownCompany, campusId, hospitalId);
        } else {
            List<Long> companies = jdbc.queryForList(
                    "select company_id from biz_company_hospital where hospital_id=?", Long.class, hospitalId);
            for (Long companyId : companies) {
                jdbc.update("insert ignore into biz_company_campus(company_id,campus_id,hospital_id,service_status,start_date) " +
                                "values(?,?,?,'0',curdate())",
                        companyId, campusId, hospitalId);
            }
        }
    }

    private void syncCampusCompanies(ResourceDef def, Long campusId, Map<String, Object> body) {
        if (!"campus".equals(def.getKey()) || campusId == null || !body.containsKey("companyIds")) return;
        List<Long> selected = new ArrayList<>();
        if (body.get("companyIds") instanceof List<?> list) {
            for (Object item : list) {
                if (item != null && !String.valueOf(item).isBlank()) selected.add(Long.valueOf(String.valueOf(item)));
            }
        }
        if (selected.isEmpty()) return;
        LoginUser user = AuthContext.require();
        Long ownCompany = dataScope.companyId(user);
        if (ownCompany != null) {
            selected = new ArrayList<>(jdbc.queryForList(
                    "select company_id from biz_company_campus where campus_id=?", Long.class, campusId));
            selected.add(ownCompany);
        }
        List<Map<String, Object>> campuses = jdbc.queryForList(
                "select hospital_id from biz_campus where campus_id=?", campusId);
        if (campuses.isEmpty()) return;
        Long hospitalId = ((Number) campuses.get(0).get("hospital_id")).longValue();
        jdbc.update("delete from biz_company_campus where campus_id=?", campusId);
        for (Long companyId : selected) {
            jdbc.update("insert ignore into biz_company_campus(company_id,campus_id,hospital_id,service_status,start_date) " +
                            "values(?,?,?,'0',curdate())",
                    companyId, campusId, hospitalId);
        }
    }

    private void syncAlarmRuleSkills(ResourceDef def, Long ruleId, Map<String, Object> body) {
        if (!"alarmRule".equals(def.getKey()) || ruleId == null || !body.containsKey("requiredSkillIds")) return;
        Object value = body.get("requiredSkillIds");
        List<String> ids = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (item != null && !String.valueOf(item).isBlank()) ids.add(String.valueOf(item));
            }
        }
        jdbc.update("update biz_alarm_rule set required_skill_ids=? where rule_id=?",
                ids.isEmpty() ? null : String.join(",", ids), ruleId);
    }

    private void syncEngineerSkills(ResourceDef def, Long engineerId, Map<String, Object> body) {
        if (!"engineer".equals(def.getKey()) || engineerId == null) return;
        if (!body.containsKey("skillIds") && !body.containsKey("skillLevels")) return;
        Map<Long, String> existingLevels = new java.util.HashMap<>();
        jdbc.query("select skill_id, level from biz_engineer_skill where engineer_id=?", rs -> {
            existingLevels.put(rs.getLong("skill_id"), rs.getString("level"));
        }, engineerId);
        jdbc.update("delete from biz_engineer_skill where engineer_id=?", engineerId);
        if (body.get("skillLevels") instanceof List<?> skills) {
            for (Object item : skills) {
                if (!(item instanceof Map<?, ?> skill)) continue;
                Object idValue = skill.get("skillId");
                if (idValue == null || String.valueOf(idValue).isBlank()) continue;
                Long skillId = Long.valueOf(String.valueOf(idValue));
                int level = parseLevel(skill.get("level"), existingLevels.get(skillId));
                jdbc.update("insert ignore into biz_engineer_skill(engineer_id,skill_id,level) values(?,?,?)",
                        engineerId, skillId, String.valueOf(level));
            }
            return;
        }
        Object value = body.get("skillIds");
        if (!(value instanceof List<?> list)) return;
        for (Object item : list) {
            if (item == null || String.valueOf(item).isBlank()) continue;
            Long skillId = Long.valueOf(String.valueOf(item));
            String level = existingLevels.get(skillId);
            if (level == null || level.isBlank()) level = "3";
            jdbc.update("insert ignore into biz_engineer_skill(engineer_id,skill_id,level) values(?,?,?)",
                    engineerId, skillId, level);
        }
    }

    private int parseLevel(Object raw, String fallback) {
        String source = raw == null ? fallback : String.valueOf(raw);
        if (source == null) return 3;
        String digits = source.replaceAll("[^0-9]", "");
        if (digits.isBlank()) return 3;
        try {
            return Math.min(5, Math.max(1, Integer.parseInt(digits)));
        } catch (NumberFormatException e) {
            return 3;
        }
    }

    private void syncEngineerUserDept(ResourceDef def, Long engineerId) {
        if (!"engineer".equals(def.getKey()) || engineerId == null) return;
        jdbc.update("update sys_user u join biz_engineer e on e.user_id=u.user_id " +
                "join sys_dept d on d.company_id=e.company_id and d.region_id=e.region_id " +
                "set u.dept_id=d.dept_id where e.engineer_id=?", engineerId);
    }

    private void validateEngineerOnDuty(ResourceDef def, Map<String, Object> body, Long id) {        if (!"engineer".equals(def.getKey())) return;
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

    private void checkCustomerWritable(ResourceDef def, LoginUser user) {
        // 客户档案按医院所属公司隔离，不再限制只能由平台管理员维护
    }

    private String joinIds(List<Long> ids) {
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private Long longOrNull(Object o) {
        if (o == null || String.valueOf(o).isBlank()) return null;
        return Long.valueOf(String.valueOf(o));
    }
}

