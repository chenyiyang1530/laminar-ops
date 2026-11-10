package com.laminar.ops.biz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderService {

    private final JdbcTemplate jdbc;

    /** 合同到期提醒：按剩余天数分档，幂等写入消息 */
    public int contractRemind() {
        List<Map<String, Object>> contracts = jdbc.queryForList(
                "select c.contract_id, c.contract_no, c.contract_name, c.hospital_id, c.company_id, c.end_date, c.warn_days, h.hospital_name " +
                        "from biz_contract c left join biz_hospital h on h.hospital_id=c.hospital_id " +
                        "where c.status='0' and c.end_date is not null " +
                        "and c.end_date between curdate() and date_add(curdate(), interval coalesce(c.warn_days,90) day)");
        int n = 0;
        for (Map<String, Object> c : contracts) {
            LocalDate end = InspectionService.toLocalDate(c.get("end_date"));
            if (end == null) continue;
            long days = ChronoUnit.DAYS.between(LocalDate.now(), end);
            String key = "contract:" + c.get("contract_id") + ":" + days;
            n += jdbc.update("insert ignore into biz_message(msg_type,title,content,level,biz_type,biz_id,biz_key,receiver_id," +
                            "receiver_role,hospital_id,company_id,is_read,create_time) values('contract',?,?,?,'contract',?,?,0,'100,101,200,201',?,?,'0',now())",
                    "合同到期提醒：" + c.get("contract_name"),
                    "合同 " + c.get("contract_no") + " 将于 " + end + " 到期，剩余 " + days + " 天，请及时续签",
                    days <= 30 ? "1" : "2", c.get("contract_id"), key, c.get("hospital_id"), c.get("company_id"));
        }
        return n;
    }

    /** 证书到期提醒：提醒工程师本人与维保经理 */
    public int certRemind() {
        List<Map<String, Object>> certs = jdbc.queryForList(
                "select c.cert_id, c.cert_name, c.cert_no, c.expire_date, c.remind_days, c.engineer_id, " +
                        "e.engineer_name, e.user_id, e.company_id from biz_certificate c join biz_engineer e on e.engineer_id=c.engineer_id " +
                        "where c.expire_date is not null and c.expire_date between curdate() " +
                        "and date_add(curdate(), interval coalesce(c.remind_days,60) day)");
        int n = 0;
        for (Map<String, Object> c : certs) {
            LocalDate expire = InspectionService.toLocalDate(c.get("expire_date"));
            if (expire == null) continue;
            long days = ChronoUnit.DAYS.between(LocalDate.now(), expire);
            String key = "cert:" + c.get("cert_id") + ":" + days;
            Long receiver = c.get("user_id") == null ? 0L : ((Number) c.get("user_id")).longValue();
            n += jdbc.update("insert ignore into biz_message(msg_type,title,content,level,biz_type,biz_id,biz_key,receiver_id," +
                            "receiver_role,company_id,is_read,create_time) values('cert',?,?,?,'certificate',?,?,?,'100',?,'0',now())",
                    "证书到期提醒：" + c.get("engineer_name") + " - " + c.get("cert_name"),
                    "证书 " + c.get("cert_no") + " 将于 " + expire + " 到期，剩余 " + days + " 天",
                    days <= 30 ? "1" : "2", c.get("cert_id"), key, receiver, c.get("company_id"));

            Set<Long> managers = new LinkedHashSet<>();
            List<Long> regional = jdbc.queryForList(
                    "select r.manager_id from biz_engineer e join biz_region r on r.region_id=e.region_id " +
                            "where e.engineer_id=? and r.manager_id is not null", Long.class, c.get("engineer_id"));
            if (!regional.isEmpty()) managers.add(regional.get(0));
            List<Long> parent = jdbc.queryForList(
                    "select p.manager_id from biz_engineer e join biz_region r on r.region_id=e.region_id " +
                            "left join biz_region p on p.region_id=r.parent_id where e.engineer_id=? and p.manager_id is not null",
                    Long.class, c.get("engineer_id"));
            if (!parent.isEmpty()) managers.add(parent.get(0));
            List<Long> companyManagers = jdbc.queryForList(
                    "select distinct u.user_id from sys_user u join sys_user_role ur on ur.user_id=u.user_id " +
                            "join sys_role r on r.role_id=ur.role_id where u.company_id=? and u.status='0' " +
                            "and r.role_key='maintenance_manager' order by u.user_id", Long.class, c.get("company_id"));
            managers.addAll(companyManagers);
            for (Long managerId : managers) {
                if (managerId == null || managerId.equals(receiver)) continue;
                n += jdbc.update("insert ignore into biz_message(msg_type,title,content,level,biz_type,biz_id,biz_key,receiver_id," +
                                "receiver_role,company_id,is_read,create_time) values('cert',?,?,?,'certificate',?,?,?,'100',?,'0',now())",
                        "证书到期提醒：" + c.get("engineer_name") + " - " + c.get("cert_name"),
                        "工程师 " + c.get("engineer_name") + " 的证书 " + c.get("cert_no") +
                                " 将于 " + expire + " 到期，剩余 " + days + " 天",
                        days <= 30 ? "1" : "2", c.get("cert_id"), key + ":mgr:" + managerId, managerId, c.get("company_id"));
            }
        }
        return n;
    }

    public void refreshStatus() {
        jdbc.update("update biz_certificate set status='1' where expire_date between curdate() and date_add(curdate(), interval 60 day) and status<>'2'");
        jdbc.update("update biz_certificate set status='2' where expire_date < curdate()");
        jdbc.update("update biz_contract set status='2' where status='0' and end_date < curdate()");
        jdbc.update("update biz_contract set status='0' where status='3' and start_date <= curdate()");
    }
}
