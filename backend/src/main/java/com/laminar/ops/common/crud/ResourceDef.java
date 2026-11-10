package com.laminar.ops.common.crud;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ResourceDef {

    private String key;
    private String table;
    private String pk;
    private String title;
    private String permPrefix;
    private String nameColumn;
    private String orderBy;
    private String joinSelect;
    private String joinSql;
    private boolean hospitalScoped = true;
    private boolean companyScoped = false;
    private boolean hasCreateTime = true;
    private boolean hasCreateBy = true;
    private boolean hasUpdateTime = true;
    private boolean hasUpdateBy = true;
    private List<FieldDef> fields = new ArrayList<>();

    public static ResourceDef of(String key, String table, String pk, String title, String permPrefix, String nameColumn) {
        ResourceDef r = new ResourceDef();
        r.key = key;
        r.table = table;
        r.pk = pk;
        r.title = title;
        r.permPrefix = permPrefix;
        r.nameColumn = nameColumn;
        r.orderBy = "e." + pk + " desc";
        return r;
    }

    public ResourceDef field(FieldDef f) { this.fields.add(f); return this; }
    public ResourceDef join(String select, String sql) { this.joinSelect = select; this.joinSql = sql; return this; }
    public ResourceDef order(String o) { this.orderBy = o; return this; }
    public ResourceDef noHospitalScope() { this.hospitalScoped = false; return this; }
    public ResourceDef companyScoped() { this.companyScoped = true; return this; }

    /** 统一租户过滤：显式标记，或表中存在 company_id 字段时自动生效 */
    public boolean isCompanyScoped() {
        return companyScoped || "company".equals(key)
                || fields.stream().anyMatch(f -> "company_id".equals(f.getColumn()));
    }
    public ResourceDef noAudit() { this.hasCreateTime = false; this.hasCreateBy = false; this.hasUpdateTime = false; this.hasUpdateBy = false; return this; }
    public ResourceDef audit(boolean createTime, boolean createBy, boolean updateTime, boolean updateBy) {
        this.hasCreateTime = createTime; this.hasCreateBy = createBy; this.hasUpdateTime = updateTime; this.hasUpdateBy = updateBy; return this;
    }
    public FieldDef fieldByName(String name) {
        return fields.stream().filter(f -> f.getName().equals(name)).findFirst().orElse(null);
    }
    public FieldDef fieldByColumn(String column) {
        return fields.stream().filter(f -> f.getColumn().equals(column)).findFirst().orElse(null);
    }
}
