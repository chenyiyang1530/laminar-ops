package com.laminar.ops.biz.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final JdbcTemplate jdbc;
    private final ContractScopeService contractScope;

    public Map<String, Object> summary(Integer year, List<Long> hospitalIds, Long companyId) {
        int y = year == null ? java.time.LocalDate.now().getYear() : year;
        Map<String, Object> result = new LinkedHashMap<>();
        String hospitalFilter = in("b.hospital_id", hospitalIds);
        String companyFilter = companyId == null ? "" : " and b.company_id=" + companyId + " ";

        List<Map<String, Object>> budgets = jdbc.queryForList(
                "select b.budget_id as `budgetId`, b.budget_name as `budgetName`, b.subject, b.amount, b.used_amount as `usedAmount`, " +
                        "round(b.used_amount / nullif(b.amount,0) * 100, 1) as `rate`, b.status, h.hospital_name as `hospitalName` " +
                        "from biz_budget b left join biz_hospital h on h.hospital_id=b.hospital_id " +
                        "where b.budget_year=?" + hospitalFilter + companyFilter + " order by b.budget_id", y);
        result.put("budgets", budgets);
        Map<String, Object> total = new LinkedHashMap<>();
        total.put("amount", jdbc.queryForObject("select coalesce(sum(b.amount),0) from biz_budget b where b.budget_year=?" +
                hospitalFilter + companyFilter, Object.class, y));
        total.put("used", jdbc.queryForObject("select coalesce(sum(b.used_amount),0) from biz_budget b where b.budget_year=?" +
                hospitalFilter + companyFilter, Object.class, y));
        result.put("budgetTotal", total);

        String woHospital = in("w.hospital_id", hospitalIds);
        String woCompany = companyId == null ? "" : " and w.company_id=" + companyId + " ";
        result.put("workorderByStatus", jdbc.queryForList(
                "select w.status, count(*) as `count` from biz_workorder w where 1=1" + woHospital + woCompany +
                        " group by w.status order by w.status"));
        result.put("monthly", jdbc.queryForList(
                "select date_format(w.create_time,'%Y-%m') as `month`, count(*) as `count` from biz_workorder w " +
                        "where year(w.create_time)=?" + woHospital + woCompany +
                        " group by date_format(w.create_time,'%Y-%m') order by `month`", y));
        result.put("costByType", jdbc.queryForList(
                "select wc.cost_type as `costType`, coalesce(sum(wc.amount),0) as `amount` " +
                        "from biz_workorder_cost wc join biz_workorder w on w.workorder_id=wc.workorder_id " +
                        "where 1=1" + woHospital + woCompany + " group by wc.cost_type"));
        result.put("contractAmount", jdbc.queryForObject(
                "select coalesce(sum(c.amount),0) from biz_contract c where c.status='0'" +
                        in("c.hospital_id", hospitalIds) +
                        (companyId == null ? "" : " and c.company_id=" + companyId), Object.class));

        String alarmHospital = in("a.hospital_id", hospitalIds);
        String alarmCompany = "";
        if (companyId != null) {
            List<Long> equipmentIds = contractScope.equipmentIdsForCompany(companyId);
            alarmCompany = equipmentIds.isEmpty() ? " and 1=0 "
                    : " and a.equipment_id in (" + equipmentIds.stream().map(String::valueOf)
                    .collect(Collectors.joining(",")) + ") ";
        }
        result.put("alarmTotal", jdbc.queryForObject(
                "select count(*) from biz_alarm a where 1=1" + alarmHospital + alarmCompany, Object.class));
        result.put("workorderTotal", jdbc.queryForObject(
                "select count(*) from biz_workorder w where 1=1" + woHospital + woCompany, Object.class));
        return result;
    }

    private String in(String column, List<Long> ids) {
        if (ids == null) return "";
        if (ids.isEmpty()) return " and 1=0 ";
        return " and " + column + " in (" + ids.stream().map(String::valueOf).collect(Collectors.joining(",")) + ") ";
    }
}
