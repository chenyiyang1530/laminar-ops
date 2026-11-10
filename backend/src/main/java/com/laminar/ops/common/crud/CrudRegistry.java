package com.laminar.ops.common.crud;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 资源元数据注册表：新增标准档案模块只需在此登记。
 */
public final class CrudRegistry {

    private static final Map<String, ResourceDef> MAP = new LinkedHashMap<>();

    private CrudRegistry() {
    }

    public static ResourceDef get(String key) {
        return MAP.get(key);
    }

    private static FieldDef pk(String name, String column) {
        return FieldDef.of(name, column, "ID").list(false).form(false);
    }

    private static FieldDef statusField() {
        return FieldDef.of("status", "status", "状态").type("dict").dict("sys_normal_disable").query("eq").def("0").width(90);
    }

    private static FieldDef hospitalField() {
        return FieldDef.of("hospitalId", "hospital_id", "医院").type("select").options("hospital").query("eq").required().width(160);
    }

    private static FieldDef campusField() {
        return FieldDef.of("campusId", "campus_id", "院区").type("select").options("campus").query("eq").width(150);
    }

    private static void register(ResourceDef def) {
        MAP.put(def.getKey(), def);
    }

    static {
        register(ResourceDef.of("company", "biz_company", "company_id", "维保公司", "customer:company", "company_name")
                .noHospitalScope()
                .field(pk("companyId", "company_id"))
                .field(FieldDef.of("companyName", "company_name", "公司名称").query("like").required().width(220))
                .field(FieldDef.of("companyCode", "company_code", "公司编码").width(120))
                .field(FieldDef.of("totalManagerId", "total_manager_id", "总负责人").type("select").options("user").width(160))
                .field(FieldDef.of("contact", "contact", "联系人").width(110))
                .field(FieldDef.of("phone", "phone", "电话").width(130))
                .field(FieldDef.of("address", "address", "公司地址").type("address").width(260))
                .field(statusField())
                .field(FieldDef.of("remark", "remark", "备注").type("textarea").list(false).span(24)));

        register(ResourceDef.of("companyRegion", "biz_company_region", "id", "公司服务区域", "customer:companyregion", "region_id")
                .noHospitalScope().audit(true, false, false, false)
                .field(pk("id", "id"))
                .field(FieldDef.of("companyId", "company_id", "公司").type("select").options("company").query("eq").required().width(180))
                .field(FieldDef.of("regionId", "region_id", "区域").type("select").options("region").query("eq").required().width(150))
                .field(FieldDef.of("managerId", "manager_id", "区域主管").type("select").options("user").width(160))
                .field(FieldDef.of("managerPhone", "manager_phone", "主管电话").virtual().form(false).width(130))
                .field(FieldDef.of("companyName", "company_name", "公司名称").virtual().form(false).width(180))
                .field(FieldDef.of("regionName", "region_name", "区域名称").virtual().form(false).width(150))
                .field(FieldDef.of("remark", "remark", "备注").type("textarea").list(false).span(24))
                .join("cp.company_name as companyName, r.region_name as regionName, coalesce(g.phone, u.phone) as managerPhone",
                        "left join biz_company cp on cp.company_id=e.company_id left join biz_region r on r.region_id=e.region_id " +
                                "left join biz_engineer g on g.user_id=e.manager_id left join sys_user u on u.user_id=e.manager_id"));

        register(ResourceDef.of("region", "biz_region", "region_id", "业务区域", "customer:region", "region_name")
                .noHospitalScope()
                .field(pk("regionId", "region_id"))
                .field(FieldDef.of("companyId", "company_id", "所属公司").type("select").options("company").required().width(160))
                .field(FieldDef.of("companyName", "company_name", "公司名称").virtual().form(false).width(180))
                .field(FieldDef.of("parentId", "parent_id", "上级区域").type("select").options("region").width(150))
                .field(FieldDef.of("regionName", "region_name", "区域名称").query("like").required().width(160))
                .field(FieldDef.of("regionCode", "region_code", "区域编码").width(100))
                .field(statusField())
                .field(FieldDef.of("remark", "remark", "备注").type("textarea").list(false).span(24))
                .join("cp.company_name as companyName", "left join biz_company cp on cp.company_id=e.company_id"));

        register(ResourceDef.of("hospital", "biz_hospital", "hospital_id", "医院", "customer:hospital", "hospital_name")
                .noHospitalScope()
                .field(pk("hospitalId", "hospital_id"))
                .field(FieldDef.of("hospitalName", "hospital_name", "医院名称").query("like").required().width(220))
                .field(FieldDef.of("hospitalCode", "hospital_code", "医院编码").query("like").width(110))
                .field(FieldDef.of("regionId", "region_id", "所属区域").type("select").options("region").query("eq").width(120))
                .field(FieldDef.of("grade", "grade", "等级").type("dict").dict("hospital_grade").query("eq").width(90))
                .field(FieldDef.of("hospitalType", "hospital_type", "类型").type("dict").dict("hospital_type").width(110))
                .field(FieldDef.of("nature", "nature", "性质").type("dict").dict("hospital_nature").width(90))
                .field(FieldDef.of("contactPerson", "contact_person", "联系人").width(100))
                .field(FieldDef.of("phone", "phone", "电话").width(130))
                .field(FieldDef.of("address", "address", "地址").width(240))
                .field(statusField())
                .join("r.region_name as `regionName`", "left join biz_region r on r.region_id = e.region_id"));

        register(ResourceDef.of("campus", "biz_campus", "campus_id", "院区", "customer:campus", "campus_name")
                .field(pk("campusId", "campus_id"))
                .field(hospitalField())
                .field(FieldDef.of("campusName", "campus_name", "院区名称").query("like").required().width(160))
                .field(FieldDef.of("campusCode", "campus_code", "院区编码").query("like").width(120))
                .field(FieldDef.of("address", "address", "地址").type("address").width(220))
                .field(FieldDef.of("phone", "phone", "联系电话").width(130))
                .field(FieldDef.of("bedCount", "bed_count", "床位数").type("number").width(90))
                .field(statusField())
                .field(FieldDef.of("remark", "remark", "备注").type("textarea").list(false).span(24))
                .join("h.hospital_name as `hospitalName`", "left join biz_hospital h on h.hospital_id = e.hospital_id"));

        register(ResourceDef.of("department", "biz_department", "department_id", "科室", "customer:department", "department_name")
                .field(pk("departmentId", "department_id"))
                .field(hospitalField())
                .field(campusField())
                .field(FieldDef.of("departmentName", "department_name", "科室名称").query("like").required().width(140))
                .field(FieldDef.of("deptCode", "dept_code", "科室编码").width(110))
                .field(FieldDef.of("deptType", "dept_type", "科室类型").type("dict").dict("dept_type").query("eq").width(110))
                .field(FieldDef.of("director", "director", "负责人").width(100))
                .field(FieldDef.of("phone", "phone", "电话").width(130))
                .field(FieldDef.of("location", "location", "位置").width(160))
                .field(statusField())
                .field(FieldDef.of("remark", "remark", "备注").type("textarea").list(false).span(24))
                .join("h.hospital_name as `hospitalName`, c.campus_name as `campusName`",
                        "left join biz_hospital h on h.hospital_id = e.hospital_id left join biz_campus c on c.campus_id = e.campus_id"));

        register(ResourceDef.of("cleanArea", "biz_clean_area", "area_id", "洁净区域", "customer:cleanarea", "area_name")
                .field(pk("areaId", "area_id"))
                .field(hospitalField())
                .field(campusField())
                .field(FieldDef.of("departmentId", "department_id", "科室").type("select").options("department").query("eq").width(130))
                .field(FieldDef.of("areaName", "area_name", "区域名称").query("like").required().width(180))
                .field(FieldDef.of("areaCode", "area_code", "区域编码").width(120))
                .field(FieldDef.of("areaType", "area_type", "区域类型").type("dict").dict("clean_area_type").query("eq").width(110))
                .field(FieldDef.of("cleanLevel", "clean_level", "洁净级别").type("dict").dict("clean_level").query("eq").width(110))
                .field(FieldDef.of("areaSize", "area_size", "面积(㎡)").type("number").width(90))
                .field(FieldDef.of("designPressure", "design_pressure", "设计正压(Pa)").type("number").width(110))
                .field(FieldDef.of("designTempMin", "design_temp_min", "温度下限").type("number").list(false))
                .field(FieldDef.of("designTempMax", "design_temp_max", "温度上限").type("number").list(false))
                .field(FieldDef.of("designHumidityMin", "design_humidity_min", "湿度下限").type("number").list(false))
                .field(FieldDef.of("designHumidityMax", "design_humidity_max", "湿度上限").type("number").list(false))
                .field(FieldDef.of("cleanRequire", "clean_require", "院感要求").type("textarea").list(false).span(24))
                .field(statusField())
                .join("h.hospital_name as `hospitalName`, c.campus_name as `campusName`, d.department_name as `departmentName`",
                        "left join biz_hospital h on h.hospital_id = e.hospital_id " +
                                "left join biz_campus c on c.campus_id = e.campus_id " +
                                "left join biz_department d on d.department_id = e.department_id"));

        register(ResourceDef.of("contact", "biz_contact", "contact_id", "客户对接人", "customer:contact", "contact_name")
                .field(pk("contactId", "contact_id"))
                .field(hospitalField())
                .field(campusField())
                .field(FieldDef.of("departmentId", "department_id", "科室").type("select").options("department").query("eq").width(130))
                .field(FieldDef.of("contactName", "contact_name", "姓名").query("like").required().width(100))
                .field(FieldDef.of("position", "position", "职务").width(110))
                .field(FieldDef.of("contactType", "contact_type", "对接类型").type("dict").dict("contact_type").query("eq").width(130))
                .field(FieldDef.of("phone", "phone", "手机号").width(130))
                .field(FieldDef.of("tel", "tel", "座机").width(120))
                .field(FieldDef.of("email", "email", "邮箱").width(170))
                .field(FieldDef.of("duty", "duty", "负责事项").width(200))
                .field(FieldDef.of("isPrimary", "is_primary", "主联系人").type("select").options("yesNo").width(100))
                .field(statusField())
                .field(FieldDef.of("remark", "remark", "备注").type("textarea").list(false).span(24))
                .join("h.hospital_name as `hospitalName`, c.campus_name as `campusName`",
                        "left join biz_hospital h on h.hospital_id = e.hospital_id left join biz_campus c on c.campus_id = e.campus_id"));

        register(ResourceDef.of("contract", "biz_contract", "contract_id", "维保合同", "customer:contract", "contract_name")
                .field(pk("contractId", "contract_id"))
                .field(hospitalField())
                .field(FieldDef.of("contractNo", "contract_no", "合同编号").query("like").required().width(140))
                .field(FieldDef.of("contractName", "contract_name", "合同名称").query("like").required().width(220))
                .field(FieldDef.of("companyId", "company_id", "维保公司").type("select").options("company").width(150))
                .field(FieldDef.of("contractType", "contract_type", "合同类型").type("dict").dict("contract_type").width(100))
                .field(FieldDef.of("startDate", "start_date", "开始日期").type("date").width(110))
                .field(FieldDef.of("endDate", "end_date", "结束日期").type("date").query("eq").width(110))
                .field(FieldDef.of("signDate", "sign_date", "签订日期").type("date").list(false))
                .field(FieldDef.of("amount", "amount", "合同金额").type("number").width(120))
                .field(FieldDef.of("warnDays", "warn_days", "预警天数").type("number").list(false))
                .field(FieldDef.of("managerId", "manager_id", "负责人").type("select").options("engineer").width(110))
                .field(FieldDef.of("status", "status", "状态").type("select").options("contractStatus").query("eq").width(90))
                .field(FieldDef.of("remark", "remark", "备注").type("textarea").list(false).span(24))
                .join("h.hospital_name as `hospitalName`", "left join biz_hospital h on h.hospital_id = e.hospital_id"));

        register(ResourceDef.of("deviceType", "biz_device_type", "type_id", "设备类型", "device:type", "type_name")
                .noHospitalScope()
                .field(pk("typeId", "type_id"))
                .field(FieldDef.of("companyId", "company_id", "所属公司").type("select").options("company").required().width(160))
                .field(FieldDef.of("companyName", "company_name", "公司名称").virtual().form(false).width(180))
                .field(FieldDef.of("typeCode", "type_code", "类型编码").query("like").required().width(120))
                .field(FieldDef.of("typeName", "type_name", "类型名称").query("like").required().width(150))
                .field(FieldDef.of("category", "category", "设备大类").type("dict").dict("device_category").query("eq").required().width(130))
                .field(FieldDef.of("monitorEnabled", "monitor_enabled", "是否监测").type("select").options("yesNo").width(100))
                .field(FieldDef.of("sort", "sort", "排序").type("number").width(80))
                .field(statusField())
                .field(FieldDef.of("remark", "remark", "备注").type("textarea").list(false).span(24))
                .join("cp.company_name as companyName", "left join biz_company cp on cp.company_id=e.company_id"));

        register(ResourceDef.of("equipment", "biz_equipment", "equipment_id", "设备台账", "device:equipment", "equipment_name")
                .field(pk("equipmentId", "equipment_id"))
                .field(FieldDef.of("companyId", "company_id", "维保公司").type("select").options("company").required().width(160))
                .field(FieldDef.of("companyName", "company_name", "维保公司").virtual().form(false).width(170))
                .field(hospitalField())
                .field(campusField())
                .field(FieldDef.of("departmentId", "department_id", "科室").type("select").options("department").query("eq").width(120))
                .field(FieldDef.of("areaId", "area_id", "洁净区域").type("select").options("cleanArea").width(150))
                .field(FieldDef.of("deviceTypeId", "device_type_id", "设备类型").type("select").options("deviceType").query("eq").width(130))
                .field(FieldDef.of("equipmentCode", "equipment_code", "设备编号").query("like").width(130))
                .field(FieldDef.of("equipmentName", "equipment_name", "设备名称").query("like").required().width(190))
                .field(FieldDef.of("model", "model", "型号").width(110))
                .field(FieldDef.of("brand", "brand", "品牌").width(100))
                .field(FieldDef.of("serialNo", "serial_no", "序列号").list(false))
                .field(FieldDef.of("installDate", "install_date", "安装日期").type("date").list(false))
                .field(FieldDef.of("warrantyEnd", "warranty_end", "质保截止").type("date").list(false))
                .field(FieldDef.of("runStatus", "run_status", "运行状态").type("dict").dict("run_status").query("eq").width(100))
                .field(FieldDef.of("monitorStatus", "monitor_status", "接入状态").type("dict").dict("monitor_status").width(100))
                .field(FieldDef.of("protocol", "protocol", "通讯协议").width(110))
                .field(FieldDef.of("commAddress", "comm_address", "通讯地址").list(false))
                .field(statusField())
                .join("h.hospital_name as `hospitalName`, c.campus_name as `campusName`, " +
                                "a.area_name as `areaName`, t.type_name as `deviceTypeName`, cp.company_name as `companyName`",
                        "left join biz_hospital h on h.hospital_id = e.hospital_id " +
                                "left join biz_campus c on c.campus_id = e.campus_id " +
                                "left join biz_clean_area a on a.area_id = e.area_id " +
                                "left join biz_device_type t on t.type_id = e.device_type_id " +
                                "left join biz_company cp on cp.company_id = e.company_id"));

        register(ResourceDef.of("monitorPoint", "biz_monitor_point", "point_id", "监测点位", "device:point", "point_name")
                .noHospitalScope()
                .field(pk("pointId", "point_id"))
                .field(FieldDef.of("companyId", "company_id", "所属公司").type("select").options("company").required().width(160))
                .field(FieldDef.of("companyName", "company_name", "公司名称").virtual().form(false).width(180))
                .field(FieldDef.of("equipmentId", "equipment_id", "设备").type("select").options("equipment").query("eq").required().width(180))
                .field(FieldDef.of("pointCode", "point_code", "点位编码").query("like").required().width(110))
                .field(FieldDef.of("pointName", "point_name", "点位名称").query("like").required().width(150))
                .field(FieldDef.of("metric", "metric", "监测指标").type("dict").dict("monitor_metric").query("eq").width(130))
                .field(FieldDef.of("unit", "unit", "单位").width(90))
                .field(FieldDef.of("warnLower", "warn_lower", "预警下限").type("number").width(100))
                .field(FieldDef.of("warnUpper", "warn_upper", "预警上限").type("number").width(100))
                .field(FieldDef.of("alarmLower", "alarm_lower", "告警下限").type("number").width(100))
                .field(FieldDef.of("alarmUpper", "alarm_upper", "告警上限").type("number").width(100))
                .field(FieldDef.of("collectInterval", "collect_interval", "采集周期(秒)").type("number").width(110))
                .field(FieldDef.of("enabled", "enabled", "启用").type("select").options("yesNo").width(90))
                .join("e2.equipment_name as `equipmentName`, cp.company_name as `companyName`",
                        "left join biz_equipment e2 on e2.equipment_id = e.equipment_id " +
                                "left join biz_company cp on cp.company_id = e.company_id"));

        register(ResourceDef.of("engineer", "biz_engineer", "engineer_id", "工程师", "staff:engineer", "engineer_name")
                .noHospitalScope()
                .field(pk("engineerId", "engineer_id"))
                .field(FieldDef.of("engineerName", "engineer_name", "姓名").query("like").required().width(110))
                .field(FieldDef.of("companyId", "company_id", "所属公司").type("select").options("company").query("eq").required().width(160))
                .field(FieldDef.of("phone", "phone", "电话").width(130))
                .field(FieldDef.of("jobTitle", "job_title", "职称").type("dict").dict("engineer_title").query("eq").width(120))
                .field(FieldDef.of("specialty", "specialty", "专业方向").width(200))
                .field(FieldDef.of("regionId", "region_id", "所属区域").type("select").options("region").query("eq").width(120))
                .field(FieldDef.of("onDuty", "on_duty", "在岗").type("select").options("yesNo").width(90))
                .field(FieldDef.of("maxOpenOrders", "max_open_orders", "最大在手单").type("number").width(100))
                .field(FieldDef.of("entryDate", "entry_date", "入职日期").type("date").width(110))
                .field(statusField())
                .field(FieldDef.of("remark", "remark", "备注").type("textarea").list(false).span(24)));

        register(ResourceDef.of("certificate", "biz_certificate", "cert_id", "资质证书", "staff:certificate", "cert_name")
                .noHospitalScope()
                .field(pk("certId", "cert_id"))
                .field(FieldDef.of("companyId", "company_id", "所属公司").type("select").options("company").required().width(160))
                .field(FieldDef.of("companyName", "company_name", "公司名称").virtual().form(false).width(180))
                .field(FieldDef.of("engineerId", "engineer_id", "工程师").type("select").options("engineer").query("eq").required().width(120))
                .field(FieldDef.of("certName", "cert_name", "证书名称").query("like").required().width(190))
                .field(FieldDef.of("certType", "cert_type", "证书类型").type("autocomplete").dict("cert_type").query("eq").width(140))
                .field(FieldDef.of("certNo", "cert_no", "证书编号").width(150))
                .field(FieldDef.of("issuingAuthority", "issuing_authority", "发证机构").width(180))
                .field(FieldDef.of("issueDate", "issue_date", "发证日期").type("date").list(false))
                .field(FieldDef.of("expireDate", "expire_date", "到期日期").type("date").width(120))
                .field(FieldDef.of("remindDays", "remind_days", "提醒天数").type("number").list(false))
                .field(FieldDef.of("status", "status", "状态").type("dict").dict("cert_status").width(100))
                .field(FieldDef.of("fileUrl", "file_url", "扫描件").list(false))
                .join("g.engineer_name as `engineerName`, cp.company_name as `companyName`",
                        "left join biz_engineer g on g.engineer_id = e.engineer_id " +
                                "left join biz_company cp on cp.company_id = e.company_id"));

        register(ResourceDef.of("skill", "biz_skill", "skill_id", "技能标签", "staff:skill", "skill_name")
                .noHospitalScope()
                .field(pk("skillId", "skill_id"))
                .field(FieldDef.of("companyId", "company_id", "所属公司").type("select").options("company").required().width(160))
                .field(FieldDef.of("companyName", "company_name", "公司名称").virtual().form(false).width(180))
                .field(FieldDef.of("skillName", "skill_name", "技能名称").query("like").required().width(180))
                .field(FieldDef.of("skillCode", "skill_code", "编码").width(120))
                .field(FieldDef.of("category", "category", "分类").type("dict").dict("skill_category").query("eq").width(110))
                .field(statusField())
                .field(FieldDef.of("remark", "remark", "备注").type("textarea").list(false).span(24))
                .join("cp.company_name as companyName", "left join biz_company cp on cp.company_id=e.company_id"));

        register(ResourceDef.of("part", "biz_part", "part_id", "备件耗材", "resource:part", "part_name")
                .noHospitalScope()
                .field(pk("partId", "part_id"))
                .field(FieldDef.of("companyId", "company_id", "所属公司").type("select").options("company").required().width(160))
                .field(FieldDef.of("companyName", "company_name", "公司名称").virtual().form(false).width(180))
                .field(FieldDef.of("partName", "part_name", "备件名称").query("like").required().width(170))
                .field(FieldDef.of("partCode", "part_code", "备件编码").query("like").width(120))
                .field(FieldDef.of("partCategory", "part_category", "类别").type("dict").dict("part_category").query("eq").width(110))
                .field(FieldDef.of("spec", "spec", "规格型号").width(140))
                .field(FieldDef.of("unit", "unit", "单位").width(70))
                .field(FieldDef.of("safetyStock", "safety_stock", "安全库存").type("number").width(100))
                .field(FieldDef.of("currentStock", "current_stock", "当前库存").type("number").width(100))
                .field(FieldDef.of("price", "price", "参考单价").type("number").width(100))
                .field(FieldDef.of("supplier", "supplier", "供应商").width(170))
                .field(FieldDef.of("warehouse", "warehouse", "库位").width(130))
                .field(statusField())
                .field(FieldDef.of("remark", "remark", "备注").type("textarea").list(false).span(24))
                .join("cp.company_name as companyName", "left join biz_company cp on cp.company_id=e.company_id"));

        register(ResourceDef.of("document", "biz_document", "doc_id", "图纸文档", "resource:document", "doc_name")
                .field(pk("docId", "doc_id"))
                .field(FieldDef.of("companyId", "company_id", "所属公司").type("select").options("company").required().width(160))
                .field(FieldDef.of("companyName", "company_name", "公司名称").virtual().form(false).width(180))
                .field(FieldDef.of("docName", "doc_name", "文档名称").query("like").required().width(220))
                .field(FieldDef.of("docCategory", "doc_category", "分类").type("dict").dict("doc_category").query("eq").width(120))
                .field(hospitalField())
                .field(FieldDef.of("fileUrl", "file_url", "文件地址").width(200))
                .field(FieldDef.of("version", "version", "版本").width(80))
                .field(FieldDef.of("uploaderName", "uploader_name", "上传人").width(100))
                .field(FieldDef.of("uploadTime", "upload_time", "上传时间").type("datetime").width(150))
                .field(FieldDef.of("tags", "tags", "标签").width(150))
                .field(FieldDef.of("remark", "remark", "备注").type("textarea").list(false).span(24))
                .join("h.hospital_name as `hospitalName`, cp.company_name as `companyName`",
                        "left join biz_hospital h on h.hospital_id = e.hospital_id " +
                                "left join biz_company cp on cp.company_id = e.company_id"));

        register(ResourceDef.of("knowledge", "biz_knowledge", "article_id", "知识库", "resource:knowledge", "title")
                .noHospitalScope()
                .field(pk("articleId", "article_id"))
                .field(FieldDef.of("companyId", "company_id", "所属公司").type("select").options("company").required().width(160))
                .field(FieldDef.of("companyName", "company_name", "公司名称").virtual().form(false).width(180))
                .field(FieldDef.of("title", "title", "标题").query("like").required().width(240))
                .field(FieldDef.of("category", "category", "分类").type("dict").dict("knowledge_category").query("eq").width(120))
                .field(FieldDef.of("summary", "summary", "摘要").width(240))
                .field(FieldDef.of("tags", "tags", "标签").width(150))
                .field(FieldDef.of("authorName", "author_name", "作者").width(100))
                .field(FieldDef.of("viewCount", "view_count", "阅读数").type("number").width(80))
                .field(FieldDef.of("publishTime", "publish_time", "发布时间").type("datetime").width(150))
                .field(FieldDef.of("content", "content", "正文").type("textarea").list(false).span(24))
                .join("cp.company_name as companyName", "left join biz_company cp on cp.company_id=e.company_id"));

        register(ResourceDef.of("budget", "biz_budget", "budget_id", "预算", "budget", "budget_name")
                .field(pk("budgetId", "budget_id"))
                .field(FieldDef.of("companyId", "company_id", "所属公司").type("select").options("company").required().width(160))
                .field(FieldDef.of("companyName", "company_name", "公司名称").virtual().form(false).width(180))
                .field(FieldDef.of("budgetYear", "budget_year", "年度").type("number").query("eq").required().width(80))
                .field(FieldDef.of("budgetName", "budget_name", "预算名称").query("like").required().width(220))
                .field(hospitalField())
                .field(FieldDef.of("subject", "subject", "预算科目").type("dict").dict("budget_subject").query("eq").width(110))
                .field(FieldDef.of("amount", "amount", "预算金额").type("number").width(120))
                .field(FieldDef.of("usedAmount", "used_amount", "已执行").type("number").width(120))
                .field(FieldDef.of("status", "status", "状态").type("dict").dict("budget_status").width(90))
                .field(FieldDef.of("remark", "remark", "备注").type("textarea").list(false).span(24))
                .join("h.hospital_name as `hospitalName`, cp.company_name as `companyName`",
                        "left join biz_hospital h on h.hospital_id = e.hospital_id " +
                                "left join biz_company cp on cp.company_id = e.company_id"));

        register(ResourceDef.of("alarmRule", "biz_alarm_rule", "rule_id", "告警规则", "device:rule", "rule_name")
                .noHospitalScope()
                .field(pk("ruleId", "rule_id"))
                .field(FieldDef.of("companyId", "company_id", "所属公司").type("select").options("company").required().width(160))
                .field(FieldDef.of("companyName", "company_name", "公司名称").virtual().form(false).width(180))
                .field(FieldDef.of("ruleName", "rule_name", "规则名称").query("like").required().width(200))
                .field(FieldDef.of("deviceTypeId", "device_type_id", "设备类型").type("select").options("deviceType").query("eq").width(130))
                .field(FieldDef.of("metric", "metric", "监测指标").type("dict").dict("monitor_metric").query("eq").required().width(120))
                .field(FieldDef.of("level", "level", "告警级别").type("dict").dict("alarm_level").width(90))
                .field(FieldDef.of("operator", "operator", "比较符").type("dict").dict("alarm_operator").width(100))
                .field(FieldDef.of("threshold", "threshold", "阈值").type("number").width(100))
                .field(FieldDef.of("threshold2", "threshold2", "阈值2").type("number").width(100))
                .field(FieldDef.of("duration", "duration", "持续(秒)").type("number").width(90))
                .field(FieldDef.of("autoWorkorder", "auto_workorder", "自动派单").type("select").options("yesNo").width(100))
                .field(FieldDef.of("enabled", "enabled", "启用").type("select").options("yesNo").width(80))
                .field(FieldDef.of("remark", "remark", "备注").type("textarea").list(false).span(24))
                .join("cp.company_name as companyName", "left join biz_company cp on cp.company_id=e.company_id"));

        register(ResourceDef.of("inspectionPlan", "biz_inspection_plan", "plan_id", "巡检计划", "inspection:plan", "plan_name")
                .field(pk("planId", "plan_id"))
                .field(FieldDef.of("companyId", "company_id", "所属公司").type("select").options("company").required().width(160))
                .field(FieldDef.of("companyName", "company_name", "公司名称").virtual().form(false).width(180))
                .field(hospitalField())
                .field(campusField())
                .field(FieldDef.of("areaId", "area_id", "洁净区域").type("select").options("cleanArea").width(160))
                .field(FieldDef.of("planName", "plan_name", "计划名称").query("like").required().width(220))
                .field(FieldDef.of("planType", "plan_type", "巡检类型").type("dict").dict("inspection_type").width(110))
                .field(FieldDef.of("cycleType", "cycle_type", "周期单位").type("select").options("cycleType").width(100))
                .field(FieldDef.of("cycleValue", "cycle_value", "周期值").type("number").width(90))
                .field(FieldDef.of("startDate", "start_date", "开始日期").type("date").width(120))
                .field(FieldDef.of("nextRunDate", "next_run_date", "下次生成").type("date").width(120))
                .field(FieldDef.of("assigneeId", "assignee_id", "执行人").type("select").options("engineer").width(110))
                .field(FieldDef.of("status", "status", "状态").type("select").options("enableStatus").width(80))
                .field(FieldDef.of("itemTemplate", "item_template", "检查项模板").type("textarea").list(false).span(24))
                .join("cp.company_name as companyName", "left join biz_company cp on cp.company_id=e.company_id"));

        register(ResourceDef.of("maintenancePlan", "biz_maintenance_plan", "plan_id", "保养计划", "maintenance:plan", "plan_name")
                .field(pk("planId", "plan_id"))
                .field(FieldDef.of("companyId", "company_id", "所属公司").type("select").options("company").required().width(160))
                .field(FieldDef.of("companyName", "company_name", "公司名称").virtual().form(false).width(180))
                .field(hospitalField())
                .field(FieldDef.of("equipmentId", "equipment_id", "设备").type("select").options("equipment").query("eq").width(190))
                .field(FieldDef.of("planName", "plan_name", "计划名称").query("like").required().width(220))
                .field(FieldDef.of("maintenanceLevel", "maintenance_level", "保养级别").type("dict").dict("maintenance_level").width(110))
                .field(FieldDef.of("cycleType", "cycle_type", "周期单位").type("select").options("cycleType").width(100))
                .field(FieldDef.of("cycleValue", "cycle_value", "周期值").type("number").width(90))
                .field(FieldDef.of("startDate", "start_date", "开始日期").type("date").width(120))
                .field(FieldDef.of("nextRunDate", "next_run_date", "下次生成").type("date").width(120))
                .field(FieldDef.of("assigneeId", "assignee_id", "执行人").type("select").options("engineer").width(110))
                .field(FieldDef.of("status", "status", "状态").type("select").options("enableStatus").width(80))
                .field(FieldDef.of("content", "content", "保养内容").type("textarea").list(false).span(24))
                .join("cp.company_name as companyName", "left join biz_company cp on cp.company_id=e.company_id"));

        register(ResourceDef.of("optimization", "sys_optimization_item", "id", "优化管理", "system:optimization", "title")
                .noHospitalScope()
                .field(pk("id", "id"))
                .field(FieldDef.of("title", "title", "优化事项").query("like").required().width(240))
                .field(FieldDef.of("module", "module", "所属模块").query("like").width(130))
                .field(FieldDef.of("priority", "priority", "紧急程度").type("dict").dict("optimization_priority").query("eq").width(100))
                .field(FieldDef.of("status", "status", "状态").type("dict").dict("optimization_status").query("eq").width(100))
                .field(FieldDef.of("owner", "owner", "负责人").width(110))
                .field(FieldDef.of("dueDate", "due_date", "计划完成").type("date").width(120))
                .field(FieldDef.of("sort", "sort", "排序").type("number").width(70))
                .field(FieldDef.of("createTime", "create_time", "创建时间").type("datetime").form(false).width(150))
                .field(FieldDef.of("description", "description", "说明").type("textarea").list(false).span(24))
                .order("field(e.priority,'urgent','high','medium','low'), field(e.status,'todo','doing','hold','done'), e.sort, e.id desc"));

        register(ResourceDef.of("post", "sys_post", "post_id", "岗位", "system:post", "post_name")
                .noHospitalScope().noAudit()
                .field(pk("postId", "post_id"))
                .field(FieldDef.of("companyId", "company_id", "所属公司").type("select").options("company").required().width(160))
                .field(FieldDef.of("companyName", "company_name", "公司名称").virtual().form(false).width(180))
                .field(FieldDef.of("postCode", "post_code", "岗位编码").query("like").required().width(120))
                .field(FieldDef.of("postName", "post_name", "岗位名称").query("like").required().width(150))
                .field(FieldDef.of("sort", "sort", "排序").type("number").width(80))
                .field(statusField())
                .field(FieldDef.of("remark", "remark", "备注").type("textarea").list(false).span(24))
                .join("c.company_name as companyName", "left join biz_company c on c.company_id=e.company_id"));

        register(ResourceDef.of("dictType", "sys_dict_type", "dict_id", "字典类型", "system:dict", "dict_name")
                .noHospitalScope().noAudit()
                .field(pk("dictId", "dict_id"))
                .field(FieldDef.of("dictName", "dict_name", "字典名称").query("like").required().width(160))
                .field(FieldDef.of("dictType", "dict_type", "字典类型").query("like").required().width(180))
                .field(statusField())
                .field(FieldDef.of("remark", "remark", "备注").type("textarea").list(false).span(24)));

        register(ResourceDef.of("dictData", "sys_dict_data", "dict_code", "字典数据", "system:dict", "dict_label")
                .noHospitalScope().noAudit()
                .field(pk("dictCode", "dict_code"))
                .field(FieldDef.of("dictType", "dict_type", "字典类型").query("eq").required().width(160))
                .field(FieldDef.of("dictLabel", "dict_label", "标签").query("like").required().width(140))
                .field(FieldDef.of("dictValue", "dict_value", "键值").required().width(100))
                .field(FieldDef.of("dictSort", "dict_sort", "排序").type("number").width(80))
                .field(FieldDef.of("listClass", "list_class", "标签样式").width(110))
                .field(statusField()));
    }
}
