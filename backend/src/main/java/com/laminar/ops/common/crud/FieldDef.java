package com.laminar.ops.common.crud;

import lombok.Data;

@Data
public class FieldDef {

    private String name;
    private String column;
    private String label;
    /** string | number | date | datetime | textarea | select | dict | password */
    private String type = "string";
    /** like | eq | none */
    private String query = "none";
    /** 选项来源资源 key，如 hospital */
    private String options;
    private String dictType;
    private String placeholder;
    private boolean list = true;
    private boolean form = true;
    private boolean required = false;
    private boolean virtual = false;
    private boolean allowCreate = false;
    private Integer width;
    private Integer span = 12;
    private String defaultValue;

    public static FieldDef of(String name, String column, String label) {
        FieldDef f = new FieldDef();
        f.name = name;
        f.column = column;
        f.label = label;
        return f;
    }

    public FieldDef type(String t) { this.type = t; return this; }
    public FieldDef query(String q) { this.query = q; return this; }
    public FieldDef options(String o) { this.options = o; return this; }
    public FieldDef dict(String d) { this.dictType = d; return this; }
    public FieldDef list(boolean b) { this.list = b; return this; }
    public FieldDef form(boolean b) { this.form = b; return this; }
    public FieldDef required() { this.required = true; return this; }
    public FieldDef virtual() { this.virtual = true; return this; }
    public FieldDef allowCreate() { this.allowCreate = true; return this; }
    public FieldDef width(int w) { this.width = w; return this; }
    public FieldDef span(int s) { this.span = s; return this; }
    public FieldDef def(String v) { this.defaultValue = v; return this; }
    public FieldDef placeholder(String p) { this.placeholder = p; return this; }
}
