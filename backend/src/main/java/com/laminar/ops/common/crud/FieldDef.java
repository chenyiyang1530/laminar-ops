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
    private boolean multiple = false;
    private boolean readOnly = false;
    private String pattern;
    private String validationMessage;
    private Integer maxLength;
    private Double minValue;
    private Double maxValue;
    private Integer width;
    private Integer span = 12;
    private Integer formSpan;
    private Integer formWidth;
    private boolean formLabelTop;
    private boolean formEditOnly;
    private boolean tag;
    private String defaultValue;
    private String switchOnText;
    private String switchOffText;
    private String switchOnValue;
    private String switchOffValue;

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
    public FieldDef multiple() { this.multiple = true; return this; }
    public FieldDef readOnly() { this.readOnly = true; return this; }
    public FieldDef pattern(String p, String message) { this.pattern = p; this.validationMessage = message; return this; }
    public FieldDef maxLength(int max) { this.maxLength = max; return this; }
    public FieldDef min(double min) { this.minValue = min; return this; }
    public FieldDef max(double max) { this.maxValue = max; return this; }
    public FieldDef width(int w) { this.width = w; return this; }
    public FieldDef span(int s) { this.span = s; return this; }
    public FieldDef formSpan(int s) { this.formSpan = s; return this; }
    public FieldDef formWidth(int w) { this.formWidth = w; return this; }
    public FieldDef formLabelTop() { this.formLabelTop = true; return this; }
    public FieldDef formEditOnly() { this.formEditOnly = true; return this; }
    public FieldDef tag() { this.tag = true; return this; }
    public FieldDef def(String v) { this.defaultValue = v; return this; }
    public FieldDef switchText(String on, String off) { this.switchOnText = on; this.switchOffText = off; return this; }
    public FieldDef switchValues(String on, String off) { this.switchOnValue = on; this.switchOffValue = off; return this; }
    public FieldDef placeholder(String p) { this.placeholder = p; return this; }
}
