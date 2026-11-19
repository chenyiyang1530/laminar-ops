package com.laminar.ops.common.crud;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 资源元数据注册表：新增标准档案模块只需在此登记。
 */
public final class CrudRegistry {

    private static final Map<String, ResourceDef> MAP = new LinkedHashMap<>();
    private static final String MOBILE = "^1[3-9]\\d{9}$";
    private static final String PHONE = "^(1[3-9]\\d{9}|0\\d{2,3}-?\\d{7,8}(-\\d{1,6})?)$";
    private static final String EMAIL = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

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

    private static void validate() {
        for (ResourceDef def : MAP.values()) {
            if (def.getKey() == null || def.getKey().isBlank()
                    || def.getTable() == null || def.getTable().isBlank()
                    || def.getPk() == null || def.getPk().isBlank()
                    || def.getPermPrefix() == null || def.getPermPrefix().isBlank()) {
                throw new IllegalStateException("CRUD 资源定义缺少 key/table/pk/perm: " + def.getKey());
            }
            if (def.getFields() == null || def.getFields().isEmpty()) {
                throw new IllegalStateException("CRUD 资源缺少字段定义: " + def.getKey());
            }
            long names = def.getFields().stream().map(FieldDef::getName).distinct().count();
            long columns = def.getFields().stream().map(FieldDef::getColumn).distinct().count();
            if (names != def.getFields().size() || columns != def.getFields().size()) {
                throw new IllegalStateException("CRUD 资源存在重复字段: " + def.getKey());
            }
            if (def.getFields().stream().noneMatch(f -> def.getPk().equals(f.getColumn()))) {
                throw new IllegalStateException("CRUD 资源缺少主键字段: " + def.getKey());
            }
            if (def.getNameColumn() != null
                    && def.getFields().stream().noneMatch(f -> def.getNameColumn().equals(f.getColumn()))) {
                throw new IllegalStateException("CRUD 资源缺少名称字段: " + def.getKey());
            }
        }
    }

    static {
        register(ResourceDef.of("company", "biz_company", "company_id", "维保公司", "customer:company", "company_name")
                .noHospitalScope()
                .field(pk("companyId", "company_id"))
                .field(FieldDef.of("companyName", "company_name", "公司名称").query("like").required().maxLength(100).width(220))
                .field(FieldDef.of("companyCode", "company_code", "公司编码").width(120))
                .field(FieldDef.of("totalManagerId", "total_manager_id", "总负责人").type("select").options("user").list(false).width(160))
                .field(FieldDef.of("totalManagerName", "total_manager_name", "总负责人").virtual().form(false).width(160))
                .field(FieldDef.of("contact", "contact", "联系人").maxLength(50).width(110))
                .field(FieldDef.of("phone", "phone", "电话").pattern(PHONE, "电话格式不正确").maxLength(20).width(130))
                .field(FieldDef.of("address", "address", "公司地址").type("address").width(260))
                .field(statusField())
                .field(FieldDef.of("remark", "remark", "备注").type("textarea").list(false).span(24))
                .join("u.nick_name as `totalManagerName`", "left join sys_user u on u.user_id=e.total_manager_id"));

        register(ResourceDef.of("companyQualification", "biz_company_qualification", "qualification_id",
                        "公司资质", "customer:qualification", "qualification_name")
                .noHospitalScope()
                .field(pk("qualificationId", "qualification_id"))
                .field(FieldDef.of("companyId", "company_id", "所属公司").type("select").options("company").required().list(false).width(160))
                .field(FieldDef.of("companyName", "company_name", "公司名称").virtual().form(false).width(180))
                .field(FieldDef.of("qualificationName", "qualification_name", "资质名称").query("like").required().maxLength(200).width(220))
                .field(FieldDef.of("qualificationType", "qualification_type", "资质类型").type("dict").dict("company_qual_type").query("eq").width(150))
                .field(FieldDef.of("qualificationNo", "qualification_no", "资质编号").query("like").maxLength(100).width(150))
                .field(FieldDef.of("level", "level", "资质等级").type("dict").dict("company_qual_level").width(100))
                .field(FieldDef.of("issuingAuthority", "issuing_authority", "发证机关").maxLength(200).width(180))
                .field(FieldDef.of("scope", "scope", "许可范围").type("textarea").list(false).span(24))
                .field(FieldDef.of("issueDate", "issue_date", "发证日期").type("date").list(false).width(120))
                .field(FieldDef.of("expireDate", "expire_date", "到期日期").type("date").width(120))
                .field(statusField())
                .field(FieldDef.of("fileUrl", "file_url", "扫描件").type("file").maxLength(500).list(false).width(200))
                .field(FieldDef.of("remark", "remark", "备注").type("textarea").list(false).span(24))
                .join("cp.company_name as companyName", "left join biz_company cp on cp.company_id=e.company_id"));

        register(ResourceDef.of("region", "biz_region", "region_id", "业务区域", "customer:region", "region_name")                .noHospitalScope()
                .field(pk("regionId", "region_id"))
                .field(FieldDef.of("companyId", "company_id", "所属公司").type("select").options("company").required().list(false).width(160))
                .field(FieldDef.of("companyName", "company_name", "公司名称").virtual().form(false).width(180))
                .field(FieldDef.of("parentId", "parent_id", "上级区域").type("select").options("region").list(false).width(150))
                .field(FieldDef.of("parentName", "parent_name", "上级区域").virtual().form(false).width(150))
                .field(FieldDef.of("regionName", "region_name", "区域名称").query("like").required().maxLength(50).width(160))
                .field(FieldDef.of("regionCode", "region_code", "区域编码").maxLength(50).width(100))
                .field(FieldDef.of("managerId", "manager_id", "区域主管").type("select").options("user").list(false).width(140))
                .field(FieldDef.of("managerName", "manager_name", "区域主管").virtual().form(false).width(140))
                .field(statusField())
                .field(FieldDef.of("remark", "remark", "备注").type("textarea").list(false).span(24))
                .join("cp.company_name as companyName, p.region_name as `parentName`, u.nick_name as `managerName`",
                        "left join biz_company cp on cp.company_id=e.company_id " +
                                "left join biz_region p on p.region_id=e.parent_id " +
                                "left join sys_user u on u.user_id=e.manager_id"));

        register(ResourceDef.of("hospital", "biz_hospital", "hospital_id", "医院", "customer:hospital", "hospital_name")
                .noHospitalScope()
                .field(pk("hospitalId", "hospital_id"))
                .field(FieldDef.of("companyIds", "company_ids", "合作公司").virtual().type("select").options("company").multiple().list(false).span(24).formSpan(24))
                .field(FieldDef.of("serviceCompanyNames", "service_company_names", "合作公司").virtual().form(false).tag().width(200))
                .field(FieldDef.of("hospitalName", "hospital_name", "医院名称").query("like").required().maxLength(100).width(220))
                .field(FieldDef.of("hospitalCode", "hospital_code", "医院编码").query("like").required().width(110))
                .field(FieldDef.of("regionId", "region_id", "所属区域").type("select").options("region").query("eq").width(120))
                .field(FieldDef.of("grade", "grade", "等级").type("dict").dict("hospital_grade").query("eq").width(90))
                .field(FieldDef.of("hospitalType", "hospital_type", "类型").type("dict").dict("hospital_type").width(110))
                .field(FieldDef.of("nature", "nature", "性质").type("dict").dict("hospital_nature").width(90))
                .field(FieldDef.of("contactPerson", "contact_person", "联系人").width(100))
                .field(FieldDef.of("phone", "phone", "电话").pattern(PHONE, "电话格式不正确").maxLength(20).width(130))
                .field(FieldDef.of("address", "address", "地址").width(240))
                .field(statusField())
                .join("r.region_name as `regionName`, " +
                                "(select group_concat(c2.company_name separator '、') from biz_company_hospital ch " +
                                "join biz_company c2 on c2.company_id=ch.company_id where ch.hospital_id=e.hospital_id) as `serviceCompanyNames`",
                        "left join biz_region r on r.region_id = e.region_id"));

        register(ResourceDef.of("campus", "biz_campus", "campus_id", "院区", "customer:campus", "campus_name")
                .field(pk("campusId", "campus_id"))
                .field(hospitalField().list(false))
                .field(FieldDef.of("hospitalName", "hospital_name", "医院").virtual().form(false).width(160))
                .field(FieldDef.of("companyIds", "company_ids", "合作公司").virtual().type("select").options("company").multiple().list(false).span(24).formSpan(24))
                .field(FieldDef.of("serviceCompanyNames", "service_company_names", "合作公司").virtual().form(false).tag().width(200))
                .field(FieldDef.of("campusName", "campus_name", "院区名称").query("like").required().maxLength(80).width(160))
                .field(FieldDef.of("campusCode", "campus_code", "院区编码").readOnly().placeholder("保存后自动生成，如 医院编码-01").width(120))
                .field(FieldDef.of("address", "address", "地址").type("address").width(220))
                .field(FieldDef.of("phone", "phone", "联系电话").pattern(PHONE, "电话格式不正确").maxLength(20).width(130))
                .field(FieldDef.of("bedCount", "bed_count", "床位数").type("number").min(0).max(100000).width(90))
                .field(statusField())
                .field(FieldDef.of("remark", "remark", "备注").type("textarea").list(false).span(24))
                .join("h.hospital_name as `hospitalName`, " +
                                "(select group_concat(c2.company_name separator '、') from biz_company_campus cc " +
                                "join biz_company c2 on c2.company_id=cc.company_id where cc.campus_id=e.campus_id) as `serviceCompanyNames`",
                        "left join biz_hospital h on h.hospital_id = e.hospital_id"));

        register(ResourceDef.of("department", "biz_department", "department_id", "科室", "customer:department", "department_name")
                .field(pk("departmentId", "department_id"))
                .field(hospitalField().list(false))
                .field(FieldDef.of("hospitalName", "hospital_name", "医院").virtual().form(false).width(160))
                .field(campusField().list(false))
                .field(FieldDef.of("campusName", "campus_name", "院区").virtual().form(false).width(130))
                .field(FieldDef.of("departmentName", "department_name", "科室名称").query("like").required().maxLength(80).width(140))
                .field(FieldDef.of("deptCode", "dept_code", "科室编码").readOnly().placeholder("保存后自动生成，如 医院编码-KS01").width(110))
                .field(FieldDef.of("deptType", "dept_type", "科室类型").type("dict").dict("dept_type").query("eq").width(110))
                .field(FieldDef.of("contactId", "contact_id", "对接人").type("select").options("contact").list(false).placeholder("保存科室后可选择对接人").width(140))
                .field(FieldDef.of("director", "director", "对接人").form(false).width(100))
                .field(FieldDef.of("phone", "phone", "电话").form(false).pattern(PHONE, "电话格式不正确").maxLength(20).width(130))
                .field(FieldDef.of("location", "location", "位置").width(160))
                .field(statusField())
                .field(FieldDef.of("remark", "remark", "备注").type("textarea").list(false).span(24))
                .join("h.hospital_name as `hospitalName`, c.campus_name as `campusName`",
                        "left join biz_hospital h on h.hospital_id = e.hospital_id left join biz_campus c on c.campus_id = e.campus_id"));

        register(ResourceDef.of("contact", "biz_contact", "contact_id", "客户对接人", "customer:contact", "contact_name")
                .field(pk("contactId", "contact_id"))
                .field(hospitalField().list(false))
                .field(FieldDef.of("hospitalName", "hospital_name", "医院").virtual().form(false).width(160))
                .field(campusField().list(false))
                .field(FieldDef.of("campusName", "campus_name", "院区").virtual().form(false).width(130))
                .field(FieldDef.of("departmentId", "department_id", "科室").type("select").options("department").query("eq").list(false).width(130))
                .field(FieldDef.of("departmentName", "department_name", "科室").virtual().form(false).width(130))
                .field(FieldDef.of("contactName", "contact_name", "姓名").query("like").required().maxLength(50).width(100))
                .field(FieldDef.of("position", "position", "职务").width(110))
                .field(FieldDef.of("contactType", "contact_type", "对接类型").type("dict").dict("contact_type").query("eq").width(130))
                .field(FieldDef.of("phone", "phone", "手机号").pattern(MOBILE, "手机号格式不正确").maxLength(11).width(130))
                .field(FieldDef.of("tel", "tel", "座机").pattern(PHONE, "座机格式不正确").maxLength(20).width(120))
                .field(FieldDef.of("email", "email", "邮箱").pattern(EMAIL, "邮箱格式不正确").maxLength(100).width(170))
                .field(FieldDef.of("duty", "duty", "负责事项").width(200))
                .field(FieldDef.of("isPrimary", "is_primary", "主联系人").type("switch").switchText("是", "否").width(100))
                .field(statusField())
                .field(FieldDef.of("remark", "remark", "备注").type("textarea").list(false).span(24))
                .join("h.hospital_name as `hospitalName`, c.campus_name as `campusName`, d.department_name as `departmentName`",
                        "left join biz_hospital h on h.hospital_id = e.hospital_id " +
                                "left join biz_campus c on c.campus_id = e.campus_id " +
                                "left join biz_department d on d.department_id = e.department_id"));

        register(ResourceDef.of("contract", "biz_contract", "contract_id", "维保合同", "customer:contract", "contract_name")
                .field(pk("contractId", "contract_id"))
                .field(hospitalField().list(false))
                .field(FieldDef.of("hospitalName", "hospital_name", "医院").virtual().form(false).width(160))
                .field(FieldDef.of("contractNo", "contract_no", "合同编号").query("like").required().width(140))
                .field(FieldDef.of("contractName", "contract_name", "合同名称").query("like").required().width(220))
                .field(FieldDef.of("companyId", "company_id", "维保公司").type("select").options("company").list(false).width(150))
                .field(FieldDef.of("companyName", "company_name", "维保公司").virtual().form(false).width(160))
                .field(FieldDef.of("contractType", "contract_type", "合同类型").type("dict").dict("contract_type").width(100))
                .field(FieldDef.of("startDate", "start_date", "开始日期").type("date").width(110))
                .field(FieldDef.of("endDate", "end_date", "结束日期").type("date").query("eq").width(110))
                .field(FieldDef.of("amount", "amount", "合同金额").type("number").min(0).width(120))
                .field(FieldDef.of("warnDays", "warn_days", "预警天数").type("number").min(0).max(3650).placeholder("到期前多少天开始提醒").list(false))
                .field(FieldDef.of("managerId", "manager_id", "负责人").type("select").options("engineer").list(false).width(110))
                .field(FieldDef.of("managerName", "manager_name", "负责人").virtual().form(false).width(110))
                .field(FieldDef.of("status", "status", "状态").type("select").options("contractStatus").query("eq").width(90))
                .field(FieldDef.of("fileUrl", "attachment", "合同文件").type("file").maxLength(500).list(false).width(200))
                .field(FieldDef.of("remark", "remark", "备注").type("textarea").list(false).span(24))
                .join("h.hospital_name as `hospitalName`, cp.company_name as `companyName`, mg.engineer_name as `managerName`",
                        "left join biz_hospital h on h.hospital_id = e.hospital_id " +
                                "left join biz_company cp on cp.company_id = e.company_id " +
                                "left join biz_engineer mg on mg.engineer_id = e.manager_id"));

        register(ResourceDef.of("deviceType", "biz_device_type", "type_id", "设备类型", "device:type", "type_name")
                .noHospitalScope()
                .field(pk("typeId", "type_id"))
                .field(FieldDef.of("companyId", "company_id", "所属公司").type("select").options("company").required().list(false).width(160))
                .field(FieldDef.of("parentId", "parent_id", "上级类型").type("select").options("deviceType").list(false).width(140))
                .field(FieldDef.of("parentName", "parent_name", "上级类型").virtual().form(false).width(140))
                .field(FieldDef.of("companyName", "company_name", "公司名称").virtual().form(false).width(180))
                .field(FieldDef.of("typeCode", "type_code", "类型编码").query("like").required().width(120))
                .field(FieldDef.of("typeName", "type_name", "类型名称").query("like").required().width(150))
                .field(FieldDef.of("monitorEnabled", "monitor_enabled", "是否监测").type("switch").switchText("监测", "不监测").placeholder("关闭后该类型设备不能接入监测点位").width(130))
                .field(FieldDef.of("sort", "sort", "排序").type("number").min(0).width(80))
                .field(statusField())
                .field(FieldDef.of("remark", "remark", "备注").type("textarea").list(false).span(24))
                .join("cp.company_name as companyName, parent.type_name as `parentName`",
                        "left join biz_company cp on cp.company_id=e.company_id " +
                                "left join biz_device_type parent on parent.type_id=e.parent_id"));

        register(ResourceDef.of("equipment", "biz_equipment", "equipment_id", "设备台账", "device:equipment", "equipment_name")
                .field(pk("equipmentId", "equipment_id"))
                .field(FieldDef.of("companyId", "company_id", "维保公司").type("select").options("company").required().list(false).width(160))
                .field(FieldDef.of("companyName", "company_name", "维保公司").virtual().form(false).list(false).width(170))
                .field(hospitalField().list(false))
                .field(FieldDef.of("hospitalName", "hospital_name", "医院").virtual().form(false).width(150))
                .field(campusField().list(false))
                .field(FieldDef.of("campusName", "campus_name", "院区").virtual().form(false).width(110))
                .field(FieldDef.of("departmentId", "department_id", "科室").type("select").options("department").query("eq").list(false).width(120))
                .field(FieldDef.of("departmentName", "department_name", "科室").virtual().form(false).width(120))
                .field(FieldDef.of("deviceTypeId", "device_type_id", "设备类型").type("select").options("deviceType").query("eq").required().list(false).width(180))
                .field(FieldDef.of("deviceTypeName", "device_type_name", "设备类型").virtual().form(false).width(150))
                .field(FieldDef.of("gatewayId", "gateway_id", "接入网关").type("select").options("gateway").list(false).width(160))
                .field(FieldDef.of("gatewayName", "gateway_name", "接入网关").virtual().form(false).width(150))
                .field(FieldDef.of("monitorAccess", "monitor_access", "接入状态").virtual().form(false).type("switch").switchText("已接入", "未接入").width(110))
                .field(FieldDef.of("simulated", "simulated", "模拟采集").type("switch").switchText("开启", "关闭").list(false).width(100))
                .field(FieldDef.of("equipmentCode", "equipment_code", "设备编号").query("like").width(130))
                .field(FieldDef.of("equipmentName", "equipment_name", "设备名称").query("like").required().width(190))
                .field(FieldDef.of("monitorStatus", "monitor_status", "监测状态").type("dict").dict("monitor_status").list(false).width(100))
                .field(FieldDef.of("model", "model", "型号").width(110))
                .field(FieldDef.of("brand", "brand", "品牌").width(100))
                .field(FieldDef.of("serialNo", "serial_no", "序列号").list(false))
                .field(FieldDef.of("installDate", "install_date", "安装日期").type("date").list(false))
                .field(FieldDef.of("warrantyEnd", "warranty_end", "质保截止").type("date").list(false))
                .field(FieldDef.of("runStatus", "run_status", "运行状态").type("dict").dict("run_status").query("eq").width(100))
                .field(FieldDef.of("protocol", "protocol", "通讯协议").width(110))
                .field(FieldDef.of("commAddress", "comm_address", "通讯地址").list(false))
                .field(statusField())
                .join("h.hospital_name as `hospitalName`, c.campus_name as `campusName`, " +
                                "t.type_name as `deviceTypeName`, cp.company_name as `companyName`, dept.department_name as `departmentName`, " +
                                "gw.gateway_name as `gatewayName`, " +
                                "case when e.monitor_status='0' then '0' else '1' end as `monitorAccess`",
                        "left join biz_hospital h on h.hospital_id = e.hospital_id " +
                                "left join biz_campus c on c.campus_id = e.campus_id " +
                                "left join biz_device_type t on t.type_id = e.device_type_id " +
                                "left join biz_company cp on cp.company_id = e.company_id " +
                                "left join biz_department dept on dept.department_id = e.department_id " +
                                "left join biz_monitor_gateway gw on gw.gateway_id = e.gateway_id"));

        register(ResourceDef.of("monitorPoint", "biz_monitor_point", "point_id", "监测点位", "device:point", "point_name")
                .noHospitalScope()
                .field(pk("pointId", "point_id"))
                .field(FieldDef.of("companyId", "company_id", "所属公司").type("select").options("company").required().list(false).width(160))
                .field(FieldDef.of("companyName", "company_name", "公司名称").virtual().form(false).width(180))
                .field(FieldDef.of("equipmentId", "equipment_id", "设备").type("select").options("equipment").query("eq").required().list(false).width(180))
                .field(FieldDef.of("equipmentName", "equipment_name", "设备").virtual().form(false).width(180))
                .field(FieldDef.of("pointCode", "point_code", "点位编码").query("like").readOnly().placeholder("保存后自动生成，如 AHU-01-TEMP").width(130))
                .field(FieldDef.of("pointName", "point_name", "点位名称").query("like").required().width(150))
                .field(FieldDef.of("metric", "metric", "监测指标").type("dict").dict("monitor_metric").query("eq").required().width(130))
                .field(FieldDef.of("unit", "unit", "单位").width(90))
                .field(FieldDef.of("warnLower", "warn_lower", "预警下限").type("number").width(100))
                .field(FieldDef.of("warnUpper", "warn_upper", "预警上限").type("number").width(100))
                .field(FieldDef.of("alarmLower", "alarm_lower", "告警下限").type("number").width(100))
                .field(FieldDef.of("alarmUpper", "alarm_upper", "告警上限").type("number").width(100))
                .field(FieldDef.of("collectInterval", "collect_interval", "采集周期(秒)").type("number").min(1).max(86400).width(110))
                .field(FieldDef.of("enabled", "enabled", "启用").type("switch").switchText("启用", "停用").width(100))
                .join("e2.equipment_name as `equipmentName`, cp.company_name as `companyName`",
                        "left join biz_equipment e2 on e2.equipment_id = e.equipment_id " +
                                "left join biz_company cp on cp.company_id = e.company_id"));

        register(ResourceDef.of("engineer", "biz_engineer", "engineer_id", "工程师", "staff:engineer", "engineer_name")
                .noHospitalScope()
                .field(pk("engineerId", "engineer_id"))
                .field(FieldDef.of("engineerName", "engineer_name", "姓名").query("like").required().width(110))
                .field(FieldDef.of("teamTitle", "team_title", "团队职称").tag().def("维保工程师").placeholder("如 总工程师 / 副总工程师").maxLength(30).width(110))
                .field(FieldDef.of("leaveStatus", "leave_status", "请假状态").virtual().form(false).tag().width(90))
                .field(FieldDef.of("skillNames", "skill_names", "技能标签").virtual().form(false).tag().width(200))
                .field(FieldDef.of("skillLevels", "skill_levels", "技能与等级").virtual().type("skills").list(false).span(24).formSpan(24))
                .field(FieldDef.of("companyId", "company_id", "所属公司").type("select").options("company").query("eq").required().list(false).width(160))
                .field(FieldDef.of("companyName", "company_name", "所属公司").virtual().form(false).width(160))
                .field(FieldDef.of("phone", "phone", "电话").readOnly().list(false).width(130))
                .field(FieldDef.of("accountPhone", "accountPhone", "电话").virtual().form(false).width(130))
                .field(FieldDef.of("specialty", "specialty", "专业方向").width(200))
                .field(FieldDef.of("regionId", "region_id", "所属区域").type("select").options("region").query("eq").list(false).width(120))
                .field(FieldDef.of("regionName", "region_name", "所属区域").virtual().form(false).width(120))
                .field(FieldDef.of("onDuty", "on_duty", "在岗").type("switch").switchText("在岗", "离岗").width(100))
                .field(FieldDef.of("maxOpenOrders", "max_open_orders", "最大在手单").type("number").min(0).max(999).width(100))
                .field(FieldDef.of("entryDate", "entry_date", "入职日期").type("date").width(110))
                .field(statusField())
                .field(FieldDef.of("remark", "remark", "备注").type("textarea").list(false).span(24))
                .join("au.phone as accountPhone, " +
                                "(select group_concat(s.skill_name separator '、') from biz_engineer_skill es " +
                                "join biz_skill s on s.skill_id=es.skill_id where es.engineer_id=e.engineer_id) as skillNames, " +
                                "(select case when count(*)>0 then '请假中' else '' end from biz_engineer_leave l " +
                                "where l.engineer_id=e.engineer_id and l.status='APPROVED' and curdate() between l.start_date and l.end_date) as leaveStatus, " +
                                "cp.company_name as `companyName`, r.region_name as `regionName`",
                        "left join sys_user au on au.user_id=e.user_id " +
                                "left join biz_company cp on cp.company_id=e.company_id " +
                                "left join biz_region r on r.region_id=e.region_id"));

        register(ResourceDef.of("certificate", "biz_certificate", "cert_id", "员工证书", "staff:certificate", "cert_name")
                .noHospitalScope()
                .field(pk("certId", "cert_id"))
                .field(FieldDef.of("companyId", "company_id", "所属公司").type("select").options("company").required().list(false).width(160))
                .field(FieldDef.of("companyName", "company_name", "公司名称").virtual().form(false).width(180))
                .field(FieldDef.of("engineerId", "engineer_id", "工程师").type("select").options("engineer").query("eq").required().list(false).width(120))
                .field(FieldDef.of("engineerName", "engineer_name", "工程师").virtual().form(false).width(120))
                .field(FieldDef.of("certName", "cert_name", "证书名称").query("like").required().width(190))
                .field(FieldDef.of("certType", "cert_type", "证书类型").type("select").dict("cert_type").query("eq").width(140))
                .field(FieldDef.of("certNo", "cert_no", "证书编号").maxLength(100).width(150))
                .field(FieldDef.of("issuingAuthority", "issuing_authority", "发证机构").width(180))
                .field(FieldDef.of("issueDate", "issue_date", "发证日期").type("date").list(false))
                .field(FieldDef.of("expireDate", "expire_date", "到期日期").type("date").width(120))
                .field(FieldDef.of("remindDays", "remind_days", "提醒天数").type("number").min(0).max(3650).list(false))
                .field(FieldDef.of("status", "status", "状态").type("dict").dict("cert_status").width(100))
                .field(FieldDef.of("fileUrl", "file_url", "扫描件").type("file").list(false))
                .join("g.engineer_name as `engineerName`, cp.company_name as `companyName`",
                        "left join biz_engineer g on g.engineer_id = e.engineer_id " +
                                "left join biz_company cp on cp.company_id = e.company_id"));

        register(ResourceDef.of("skill", "biz_skill", "skill_id", "技能标签", "staff:skill", "skill_name")
                .noHospitalScope().audit(true, false, false, false)
                .field(pk("skillId", "skill_id"))
                .field(FieldDef.of("companyId", "company_id", "所属公司").type("select").options("company").required().list(false).width(160))
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
                .field(FieldDef.of("companyId", "company_id", "所属公司").type("select").options("company").required().list(false).width(160))
                .field(FieldDef.of("companyName", "company_name", "公司名称").virtual().form(false).width(180))
                .field(FieldDef.of("partName", "part_name", "备件名称").query("like").required().width(170))
                .field(FieldDef.of("partCode", "part_code", "备件编码").query("like").required().width(120))
                .field(FieldDef.of("partCategory", "part_category", "类别").type("dict").dict("part_category").query("eq").width(110))
                .field(FieldDef.of("spec", "spec", "规格型号").width(140))
                .field(FieldDef.of("unit", "unit", "单位").width(70))
                .field(FieldDef.of("safetyStock", "safety_stock", "安全库存").type("number").min(0).width(100))
                .field(FieldDef.of("currentStock", "current_stock", "当前库存").type("number").min(0).form(false).width(100))
                .field(FieldDef.of("price", "price", "参考单价").type("number").min(0).width(100))
                .field(FieldDef.of("supplier", "supplier", "供应商").width(170))
                .field(FieldDef.of("warehouse", "warehouse", "库位").width(130))
                .field(statusField())
                .field(FieldDef.of("remark", "remark", "备注").type("textarea").list(false).span(24))
                .join("cp.company_name as companyName", "left join biz_company cp on cp.company_id=e.company_id"));

        register(ResourceDef.of("partEquipment", "biz_part_equipment", "id", "备件兼容设备", "resource:part", "remark")
                .noHospitalScope()
                .noAudit()
                .field(pk("id", "id"))
                .field(FieldDef.of("companyId", "company_id", "所属公司").type("select").options("company").required().list(false).width(160))
                .field(FieldDef.of("companyName", "company_name", "公司名称").virtual().form(false).width(180))
                .field(FieldDef.of("partId", "part_id", "备件").type("select").options("part").query("eq").required().width(180))
                .field(FieldDef.of("partName", "part_name", "备件名称").virtual().form(false).width(150))
                .field(FieldDef.of("equipmentId", "equipment_id", "具体设备").type("select").options("equipment").query("eq").width(180))
                .field(FieldDef.of("equipmentName", "equipment_name", "设备名称").virtual().form(false).width(150))
                .field(FieldDef.of("deviceTypeId", "device_type_id", "设备类型").type("select").options("deviceType").query("eq").width(150))
                .field(FieldDef.of("deviceTypeName", "device_type_name", "设备类型名称").virtual().form(false).width(150))
                .field(FieldDef.of("deviceModel", "device_model", "设备型号").query("like").width(150))
                .field(FieldDef.of("effectiveFrom", "effective_from", "生效日期").type("date").width(120))
                .field(FieldDef.of("effectiveTo", "effective_to", "失效日期").type("date").width(120))
                .field(FieldDef.of("priority", "priority", "优先级").type("number").min(0).width(90))
                .field(FieldDef.of("relationType", "relation_type", "关系").type("dict").dict("part_relation_type").query("eq").width(110))
                .field(FieldDef.of("remark", "remark", "备注").type("textarea").list(false).span(24))
                .join("cp.company_name as `companyName`, p.part_name as `partName`, eq.equipment_name as `equipmentName`, dt.type_name as `deviceTypeName`",
                        "left join biz_company cp on cp.company_id=e.company_id " +
                                "left join biz_part p on p.part_id=e.part_id " +
                                "left join biz_equipment eq on eq.equipment_id=e.equipment_id " +
                                "left join biz_device_type dt on dt.type_id=e.device_type_id"));

        register(ResourceDef.of("document", "biz_document", "doc_id", "图纸文档", "resource:document", "doc_name")                .noAudit()
                .noHospitalScope()
                .field(pk("docId", "doc_id"))
                .field(FieldDef.of("companyId", "company_id", "所属公司").type("select").options("company").required().list(false).width(160))
                .field(FieldDef.of("companyName", "company_name", "公司名称").virtual().form(false).width(180))
                .field(FieldDef.of("docName", "doc_name", "文档名称").query("like").required().width(220))
                .field(FieldDef.of("docCategory", "doc_category", "分类").type("dict").dict("doc_category").query("eq").width(120))
                .field(FieldDef.of("fileUrl", "file_url", "文件地址").type("file").width(200))
                .field(FieldDef.of("version", "version", "版本").width(80))
                .field(FieldDef.of("uploaderName", "uploader_name", "上传人").form(false).width(100))
                .field(FieldDef.of("uploadTime", "upload_time", "上传时间").type("datetime").form(false).width(150))
                .field(FieldDef.of("remark", "remark", "备注").type("textarea").list(false).span(24))
                .join("cp.company_name as `companyName`",
                        "left join biz_company cp on cp.company_id = e.company_id"));

        register(ResourceDef.of("knowledge", "biz_knowledge", "article_id", "知识库", "resource:knowledge", "title")
                .noHospitalScope()
                .field(pk("articleId", "article_id"))
                .field(FieldDef.of("companyId", "company_id", "所属公司").type("select").options("company").required().list(false).width(160))
                .field(FieldDef.of("companyName", "company_name", "公司名称").virtual().form(false).width(180))
                .field(FieldDef.of("title", "title", "标题").query("like").required().width(240))
                .field(FieldDef.of("category", "category", "分类").type("dict").dict("knowledge_category").query("eq").width(120))
                .field(FieldDef.of("summary", "summary", "摘要").width(240))
                .field(FieldDef.of("tags", "tags", "标签").width(150))
                .field(FieldDef.of("authorName", "author_name", "作者").width(100))
                .field(FieldDef.of("viewCount", "view_count", "阅读数").type("number").min(0).width(80))
                .field(FieldDef.of("publishTime", "publish_time", "发布时间").type("datetime").width(150))
                .field(FieldDef.of("content", "content", "正文").type("textarea").list(false).span(24))
                .join("cp.company_name as companyName", "left join biz_company cp on cp.company_id=e.company_id"));

        register(ResourceDef.of("alarmRule", "biz_alarm_rule", "rule_id", "告警规则", "device:rule", "rule_name")
                .noHospitalScope()
                .field(pk("ruleId", "rule_id"))
                .field(FieldDef.of("companyId", "company_id", "所属公司").type("select").options("company").required().list(false).width(160))
                .field(FieldDef.of("companyName", "company_name", "公司名称").virtual().form(false).width(180))
                .field(FieldDef.of("ruleName", "rule_name", "规则名称").query("like").required().width(200))
                .field(FieldDef.of("requiredSkillIds", "required_skill_ids", "需要技能").virtual().type("skillPicker").span(24).formSpan(24).list(false))
                .field(FieldDef.of("deviceTypeId", "device_type_id", "设备类型").type("select").options("deviceType").query("eq").list(false).width(130))
                .field(FieldDef.of("deviceTypeName", "device_type_name", "设备类型").virtual().form(false).width(150))
                .field(FieldDef.of("metric", "metric", "监测指标").type("dict").dict("monitor_metric").query("eq").required().width(120))
                .field(FieldDef.of("level", "level", "告警级别").type("dict").dict("alarm_level").width(90))
                .field(FieldDef.of("operator", "operator", "比较符").type("dict").dict("alarm_operator").width(100))
                .field(FieldDef.of("threshold", "threshold", "阈值/下限").type("number").width(110))
                .field(FieldDef.of("threshold2", "threshold2", "上限（仅区间）").type("number").width(120))
                .field(FieldDef.of("duration", "duration", "持续(秒)").type("number").min(0).width(90))
                .field(FieldDef.of("autoWorkorder", "auto_workorder", "自动派单").type("switch").switchText("开启", "关闭").width(110))
                .field(FieldDef.of("enabled", "enabled", "启用").type("switch").switchText("启用", "停用").width(90))
                .field(FieldDef.of("remark", "remark", "备注").type("textarea").list(false).span(24))
                .join("cp.company_name as companyName, t.type_name as `deviceTypeName`",
                        "left join biz_company cp on cp.company_id=e.company_id " +
                                "left join biz_device_type t on t.type_id=e.device_type_id"));

        register(ResourceDef.of("inspectionPlan", "biz_inspection_plan", "plan_id", "巡检计划", "inspection:plan", "plan_name")
                .field(pk("planId", "plan_id"))
                .field(FieldDef.of("companyId", "company_id", "所属公司").type("select").options("company").required().list(false).width(160))
                .field(FieldDef.of("companyName", "company_name", "公司名称").virtual().form(false).width(180))
                .field(hospitalField().list(false))
                .field(FieldDef.of("hospitalName", "hospital_name", "医院").virtual().form(false).width(160))
                .field(campusField().list(false))
                .field(FieldDef.of("campusName", "campus_name", "院区").virtual().form(false).width(130))
                .field(FieldDef.of("departmentId", "department_id", "科室").type("select").options("department").query("eq").list(false).width(130))
                .field(FieldDef.of("departmentName", "department_name", "科室").virtual().form(false).width(130))
                .field(FieldDef.of("equipmentIds", "equipment_ids", "适用设备").virtual().type("select").options("equipment").multiple().list(false).width(240))
                .field(FieldDef.of("equipmentNames", "equipment_names", "适用设备").virtual().form(false).tag().width(220))
                .field(FieldDef.of("planName", "plan_name", "计划名称").query("like").required().width(220))
                .field(FieldDef.of("planType", "plan_type", "巡检类型").type("dict").dict("inspection_type").width(110))
                .field(FieldDef.of("cycleType", "cycle_type", "周期单位").type("select").options("cycleType").width(100))
                .field(FieldDef.of("cycleValue", "cycle_value", "周期值").type("number").min(1).max(3650).width(90))
                .field(FieldDef.of("startDate", "start_date", "开始日期").type("date").width(120))
                .field(FieldDef.of("nextRunDate", "next_run_date", "下次生成").type("date").width(120))
                .field(FieldDef.of("assigneeId", "assignee_id", "执行人").type("select").options("engineer").list(false).placeholder("请先选择医院，只能选该区域工程师").width(110))
                .field(FieldDef.of("assigneeName", "assignee_name", "执行人").virtual().form(false).width(110))
                .field(FieldDef.of("status", "status", "状态").type("switch").switchText("启用", "停用").switchValues("0", "1").width(90))
                .field(FieldDef.of("itemTemplate", "item_template", "检查项模板").type("textarea").list(false).placeholder("每行一个检查项，例如：\n检查运行状态\n清洁过滤器\n检查电气接线").span(24))
                .join("cp.company_name as companyName, h.hospital_name as `hospitalName`, c.campus_name as `campusName`, dept.department_name as `departmentName`, eng.engineer_name as `assigneeName`, " +
                                "(select group_concat(eq.equipment_name separator '、') from biz_inspection_plan_equipment pe " +
                                "join biz_equipment eq on eq.equipment_id=pe.equipment_id where pe.plan_id=e.plan_id) as `equipmentNames`",
                        "left join biz_company cp on cp.company_id=e.company_id " +
                                "left join biz_hospital h on h.hospital_id=e.hospital_id " +
                                "left join biz_campus c on c.campus_id=e.campus_id " +
                                "left join biz_department dept on dept.department_id=e.department_id " +
                                "left join biz_engineer eng on eng.engineer_id=e.assignee_id"));

        register(ResourceDef.of("maintenancePlan", "biz_maintenance_plan", "plan_id", "保养计划", "maintenance:plan", "plan_name")
                .field(pk("planId", "plan_id"))
                .field(FieldDef.of("companyId", "company_id", "所属公司").type("select").options("company").required().list(false).width(160))
                .field(FieldDef.of("companyName", "company_name", "公司名称").virtual().form(false).width(180))
                .field(hospitalField().list(false))
                .field(FieldDef.of("hospitalName", "hospital_name", "医院").virtual().form(false).width(160))
                .field(FieldDef.of("equipmentId", "equipment_id", "设备").type("select").options("equipment").list(false).form(false).width(190))
                .field(FieldDef.of("equipmentIds", "equipment_ids", "适用设备").virtual().type("select").options("equipment").multiple().list(false).width(240))
                .field(FieldDef.of("equipmentNames", "equipment_names", "适用设备").virtual().form(false).tag().width(220))
                .field(FieldDef.of("planName", "plan_name", "计划名称").query("like").required().width(220))
                .field(FieldDef.of("maintenanceLevel", "maintenance_level", "保养级别").type("dict").dict("maintenance_level").width(110))
                .field(FieldDef.of("cycleType", "cycle_type", "周期单位").type("select").options("cycleType").width(100))
                .field(FieldDef.of("cycleValue", "cycle_value", "周期值").type("number").min(1).max(3650).width(90))
                .field(FieldDef.of("startDate", "start_date", "开始日期").type("date").width(120))
                .field(FieldDef.of("nextRunDate", "next_run_date", "下次生成").type("date").width(120))
                .field(FieldDef.of("assigneeId", "assignee_id", "执行人").type("select").options("engineer").list(false).placeholder("请先选择医院，只能选该区域工程师").width(110))
                .field(FieldDef.of("assigneeName", "assignee_name", "执行人").virtual().form(false).width(110))
                .field(FieldDef.of("status", "status", "状态").type("switch").switchText("启用", "停用").switchValues("0", "1").width(90))
                .field(FieldDef.of("content", "content", "保养内容").type("textarea").list(false).placeholder("每行一个保养项，例如：\n检查运行状态\n清洁过滤器\n检查电气接线").span(24))
                .join("cp.company_name as companyName, h.hospital_name as `hospitalName`, " +
                                "(select group_concat(eq.equipment_name separator '、') from biz_maintenance_plan_equipment pe " +
                                "join biz_equipment eq on eq.equipment_id=pe.equipment_id where pe.plan_id=e.plan_id) as `equipmentNames`, " +
                                "eng.engineer_name as `assigneeName`",
                        "left join biz_company cp on cp.company_id=e.company_id " +
                                "left join biz_hospital h on h.hospital_id=e.hospital_id " +
                                "left join biz_equipment eq on eq.equipment_id=e.equipment_id " +
                                "left join biz_engineer eng on eng.engineer_id=e.assignee_id"));

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
                .field(FieldDef.of("companyId", "company_id", "所属公司").type("select").options("company").required().list(false).width(160))
                .field(FieldDef.of("companyName", "company_name", "公司名称").virtual().form(false).width(180))
                .field(FieldDef.of("postCode", "post_code", "岗位编码").query("like").required().width(120))
                .field(FieldDef.of("postName", "post_name", "岗位名称").query("like").required().width(150))
                .field(FieldDef.of("sort", "sort", "排序").type("number").min(0).width(80))
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
        validate();
    }
}




