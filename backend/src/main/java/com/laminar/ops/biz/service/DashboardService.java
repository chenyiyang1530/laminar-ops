package com.laminar.ops.biz.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final JdbcTemplate jdbc;
    private final ContractScopeService contractScope;

    public Map<String, Object> overview(List<Long> hospitalIds, Long companyId) {
        return overview(hospitalIds, companyId, null);
    }

    public Map<String, Object> overview(List<Long> hospitalIds, Long companyId, Long departmentId) {
        List<Long> companyHospitals = companyId == null ? null : contractScope.hospitalIdsForCompany(companyId);
        List<Long> equipmentIds = companyId == null ? null : contractScope.equipmentIdsForCompany(companyId);
        List<Long> allowed = intersect(hospitalIds, companyHospitals);

        String h = filter("hospital_id", allowed);
        String ha = filter("a.hospital_id", allowed);
        String eh = filter("e.hospital_id", allowed);
        String eq = filter("e.equipment_id", equipmentIds);
        String aeq = companyId == null ? "" : " and company_id=" + companyId + " ";
        String aeqA = companyId == null ? "" : " and a.company_id=" + companyId + " ";
        String taskCompany = companyId == null ? "" : " and company_id=" + companyId + " ";
        String taskCompanyT = companyId == null ? "" : " and t.company_id=" + companyId + " ";
        String woCompany = companyFilter("", companyId);
        String contractCompany = companyFilter("c.", companyId);
        String mapEquipmentCompany = companyId == null ? "" : " and e.company_id=" + companyId + " ";
        String deptE = departmentId == null ? "" : " and e.department_id=" + departmentId + " ";
        String deptA = departmentId == null ? "" : " and a.department_id=" + departmentId + " ";
        String deptW = departmentId == null ? "" : " and department_id=" + departmentId + " ";
        String deptT = departmentId == null ? "" : " and t.department_id=" + departmentId + " ";

        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> stats = new LinkedHashMap<>();
        if (departmentId == null) {
            stats.put("hospitalCount", count("select count(*) from biz_hospital where status='0'" + h));
            stats.put("campusCount", count("select count(*) from biz_campus where status='0'" + h));
        } else {
            stats.put("hospitalCount", count("select count(distinct hospital_id) from biz_department where department_id=" + departmentId));
            stats.put("campusCount", count("select count(distinct campus_id) from biz_department where department_id=" + departmentId));
        }
        int eqTotal = count("select count(*) from biz_equipment e where e.status='0'" + eh + eq + deptE);
        int online = count("select count(*) from biz_equipment e where e.status='0' and e.monitor_status='1'" + eh + eq + deptE);
        stats.put("equipmentCount", eqTotal);
        stats.put("equipmentOnline", online);
        stats.put("onlineRate", eqTotal == 0 ? 0 : Math.round(online * 1000.0 / eqTotal) / 10.0);
        stats.put("alarmUnhandled", count("select count(*) from biz_alarm where status in ('0','1','2')" + h + aeq + deptW));
        stats.put("alarmToday", count("select count(*) from biz_alarm where date(first_time)=curdate()" + h + aeq + deptW));
        stats.put("alarmCritical", count("select count(*) from biz_alarm where level='1' and status in ('0','1','2')" + h + aeq + deptW));
        stats.put("workorderPending", count("select count(*) from biz_workorder where status in ('0','1')" + h + woCompany + deptW));
        stats.put("workorderProcessing", count("select count(*) from biz_workorder where status='2'" + h + woCompany + deptW));
        stats.put("workorderAccepting", count("select count(*) from biz_workorder where status='3'" + h + woCompany + deptW));
        stats.put("workorderMonth", count("select count(*) from biz_workorder where date_format(create_time,'%Y%m')=date_format(curdate(),'%Y%m')" + h + woCompany + deptW));
        stats.put("contractExpiring", departmentId != null ? 0 : count("select count(*) from biz_contract c where c.status='0' and c.end_date between curdate() and date_add(curdate(), interval 90 day)" +
                filter("c.hospital_id", allowed) + contractCompany));
        stats.put("certExpiring", departmentId != null ? 0 : count("select count(*) from biz_certificate c where c.expire_date between curdate() and date_add(curdate(), interval 60 day)" +
                certHospitalFilter(allowed) + certCompanyFilter(companyId)));
        stats.put("inspectionDue", count("select count(*) from biz_inspection_task where status in ('0','3') and plan_time <= curdate()" + h + taskCompany + deptW));
        stats.put("maintenanceDue", count("select count(*) from biz_maintenance_task where status in ('0','3') and plan_date <= curdate()" + h + taskCompany + deptW));
        stats.put("partBelowSafety", departmentId != null ? 0 : companyId != null
                ? count("select count(*) from biz_part where current_stock < safety_stock and company_id=" + companyId)
                : allowed == null ? count("select count(*) from biz_part where current_stock < safety_stock") : 0);
        result.put("stats", stats);

        result.put("alarmTrend", trend("select date(first_time) d, count(*) c from biz_alarm " +
                "where first_time >= date_sub(curdate(), interval 6 day)" + h + aeq + deptW + " group by date(first_time)"));
        result.put("workorderTrend", trend("select date(create_time) d, count(*) c from biz_workorder " +
                "where create_time >= date_sub(curdate(), interval 6 day)" + h + woCompany + deptW + " group by date(create_time)"));

        result.put("alarmByLevel", jdbc.queryForList(
                "select level as `level`, count(*) as `count` from biz_alarm where status in ('0','1','2')" + h + aeq + deptW + " group by level order by level"));
        result.put("workorderByStatus", jdbc.queryForList(
                "select status as `status`, count(*) as `count` from biz_workorder where 1=1" + h + woCompany + deptW + " group by status order by status"));
        result.put("equipmentByCategory", jdbc.queryForList(
                "select t.type_name as `name`, count(*) as `value` from biz_equipment e " +
                        "left join biz_device_type t on t.type_id=e.device_type_id " +
                        "where e.status='0'" + eh + eq + deptE + " group by t.type_name order by value desc"));

        result.put("recentAlarms", jdbc.queryForList(
                "select a.alarm_id as `alarmId`, a.alarm_no as `alarmNo`, a.level as `level`, a.content as `content`, " +
                        "a.status as `status`, date_format(a.first_time,'%m-%d %H:%i') as `time`, " +
                        "e.equipment_name as `equipmentName`, h.hospital_name as `hospitalName` " +
                        "from biz_alarm a left join biz_equipment e on e.equipment_id=a.equipment_id " +
                        "left join biz_hospital h on h.hospital_id=a.hospital_id " +
                        "where 1=1" + ha + aeqA + deptA +
                        " order by a.first_time desc limit 6"));

        result.put("expiringContracts", departmentId != null ? List.of() : jdbc.queryForList(
                "select c.contract_id as `contractId`, c.contract_name as `contractName`, c.end_date as `endDate`, " +
                        "datediff(c.end_date, curdate()) as `days`, h.hospital_name as `hospitalName` " +
                        "from biz_contract c left join biz_hospital h on h.hospital_id=c.hospital_id " +
                        "where c.status='0' and c.end_date between curdate() and date_add(curdate(), interval 120 day)" +
                        filter("c.hospital_id", allowed) + contractCompany + " order by c.end_date limit 5"));

        result.put("hospitalMap", jdbc.queryForList(
                "select h.hospital_id as `hospitalId`, h.hospital_name as `hospitalName`, h.longitude as `longitude`, " +
                        "h.latitude as `latitude`, count(e.equipment_id) as `equipmentCount`, " +
                        "sum(case when e.monitor_status='1' then 1 else 0 end) as `onlineCount` " +
                        "from biz_hospital h left join biz_equipment e on e.hospital_id=h.hospital_id and e.status='0'" + mapEquipmentCompany + deptE +
                        "where 1=1" + filter("h.hospital_id", allowed) +
                        " group by h.hospital_id order by equipmentCount desc"));

        result.put("dueTasks", jdbc.queryForList(
                "select 'inspection' as `type`, t.task_no as `taskNo`, t.plan_time as `planDate`, " +
                        "e.equipment_name as `title`, t.status as `status` from biz_inspection_task t " +
                        "left join biz_equipment e on e.equipment_id=t.equipment_id " +
                        "where t.status in ('0','3') and t.plan_time <= date_add(curdate(), interval 1 day)" +
                        filter("t.hospital_id", allowed) + taskCompanyT + deptT +
                        " union all " +
                        "select 'maintenance' as `type`, t.task_no as `taskNo`, t.plan_date as `planDate`, " +
                        "e.equipment_name as `title`, t.status as `status` from biz_maintenance_task t " +
                        "left join biz_equipment e on e.equipment_id=t.equipment_id " +
                        "where t.status in ('0','3') and t.plan_date <= date_add(curdate(), interval 7 day)" +
                        filter("t.hospital_id", allowed) + taskCompanyT + deptT +
                        " order by planDate limit 6"));

        return result;
    }

    private List<Long> intersect(List<Long> a, List<Long> b) {
        if (a == null) return b;
        if (b == null) return a;
        Set<Long> set = new LinkedHashSet<>(a);
        set.retainAll(b);
        return new ArrayList<>(set);
    }

    private String companyFilter(String alias, Long companyId) {
        return companyId == null ? "" : " and " + alias + "company_id=" + companyId + " ";
    }

    private String certHospitalFilter(List<Long> hospitalIds) {
        if (hospitalIds == null) return "";
        if (hospitalIds.isEmpty()) return " and 1=0 ";
        return " and exists (select 1 from biz_engineer_hospital eh where eh.engineer_id=c.engineer_id and eh.hospital_id in (" +
                join(hospitalIds) + ")) ";
    }

    private String certCompanyFilter(Long companyId) {
        return companyId == null ? "" :
                " and exists (select 1 from biz_engineer eng where eng.engineer_id=c.engineer_id and eng.company_id=" + companyId + ") ";
    }

    private String filter(String column, List<Long> ids) {
        if (ids == null) return "";
        if (ids.isEmpty()) return " and 1=0 ";
        return " and " + column + " in (" + join(ids) + ") ";
    }

    private String join(List<Long> ids) {
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private int count(String sql) {
        Integer n = jdbc.queryForObject(sql, Integer.class);
        return n == null ? 0 : n;
    }

    private List<Map<String, Object>> trend(String sql) {
        Map<String, Integer> map = new HashMap<>();
        for (Map<String, Object> row : jdbc.queryForList(sql)) {
            Object d = row.get("d");
            Object c = row.get("c");
            if (d != null) {
                map.put(String.valueOf(d), ((Number) c).intValue());
            }
        }
        List<Map<String, Object>> list = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", day.format(fmt));
            item.put("value", map.getOrDefault(day.toString(), 0));
            list.add(item);
        }
        return list;
    }
}
