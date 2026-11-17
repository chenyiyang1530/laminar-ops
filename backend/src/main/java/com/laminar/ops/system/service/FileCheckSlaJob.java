package com.laminar.ops.system.service;

import com.laminar.ops.common.SystemAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SLA 三级升级：L1 到期、L2 超时 24 小时、L3 超时 72 小时。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileCheckSlaJob {

    private final JdbcTemplate jdbc;
    private final SystemAlertService systemAlertService;
    private final MailNotificationService mailNotificationService;
    private final FileCheckRecipientService recipientService;
    private final FileCheckProperties properties;

    @Value("${laminar.file-check.enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "${laminar.file-check.sla-cron:0 */10 * * * *}")
    public void escalate() {
        if (!enabled) return;
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select i.item_id, i.table_name, i.record_id, i.file_column, i.company_id, i.hospital_id, " +
                        "i.escalation_level, i.sla_deadline, c.company_name, h.hospital_name " +
                        "from sys_file_check_item i " +
                        "left join biz_company c on c.company_id=i.company_id " +
                        "left join biz_hospital h on h.hospital_id=i.hospital_id " +
                        "where i.status='0' and i.escalation_level < 3 and i.sla_deadline is not null " +
                        "and i.sla_deadline < now() order by i.sla_deadline limit 200");
        List<Map<String, Object>> due = rows.stream().filter(row -> targetLevel(row) > currentLevel(row)).toList();
        if (due.isEmpty()) return;

        due.forEach(row -> {
            int level = targetLevel(row);
            row.put("target_level", level);
            jdbc.update("update sys_file_check_item set escalation_level=?, escalated='1', last_escalation_time=now() " +
                    "where item_id=?", level, row.get("item_id"));
        });

        Map<String, List<Map<String, Object>>> groups = due.stream()
                .collect(Collectors.groupingBy(row -> {
                    int level = ((Number) row.get("target_level")).intValue();
                    Long company = row.get("company_id") == null ? null : ((Number) row.get("company_id")).longValue();
                    Long hospital = row.get("hospital_id") == null ? null : ((Number) row.get("hospital_id")).longValue();
                    return level + "|" + company + "|" + hospital;
                }));
        groups.values().forEach(this::notifyGroup);
        systemAlertService.error("file-sla-overdue", "文件丢失 SLA 升级",
                "有 " + due.size() + " 个文件丢失触发 SLA 升级");
        mailNotificationService.sendHtml(null, "文件丢失 SLA 升级",
                "<h3>文件丢失 SLA 升级</h3><p>有 " + due.size() + " 个文件丢失触发升级，请登录管理端处理。</p>", null, null);
        log.warn("[文件巡检] SLA 升级 {} 项", due.size());
    }

    private void notifyGroup(List<Map<String, Object>> list) {
        Map<String, Object> first = list.get(0);
        int level = ((Number) first.get("target_level")).intValue();
        Long companyId = first.get("company_id") == null ? null : ((Number) first.get("company_id")).longValue();
        Long hospitalId = first.get("hospital_id") == null ? null : ((Number) first.get("hospital_id")).longValue();
        try {
            String title = "文件丢失 SLA L" + level + " 超时：" + list.size() + " 个";
            String content = "请及时处理丢失文件，首项：" + first.get("table_name") + "#" + first.get("record_id");
            if (companyId != null) {
                jdbc.update("insert into biz_message(msg_type,title,content,level,biz_type,biz_id,biz_key,receiver_id,receiver_role," +
                                "hospital_id,company_id,is_read,create_time) values('system',?,?,'1','file_check',?,null,0,'213',?,?,'0',now())",
                        title, content, first.get("item_id"), null, companyId);
            }
            if (hospitalId != null) {
                jdbc.update("insert into biz_message(msg_type,title,content,level,biz_type,biz_id,biz_key,receiver_id,receiver_role," +
                                "hospital_id,company_id,is_read,create_time) values('system',?,?,'1','file_check',?,null,0,'200',?,?,'0',now())",
                        title, content, first.get("item_id"), hospitalId, null);
            }
            List<String> emails = recipientService.emails(companyId, hospitalId);
            if (!emails.isEmpty()) {
                String html = "<h3>文件丢失 SLA L" + level + " 升级</h3><p>机构有 " + list.size()
                        + " 个文件丢失已超时。首项：" + first.get("table_name") + "#" + first.get("record_id") + "</p>";
                mailNotificationService.sendHtml(emails, title, html, null, null);
            }
        } catch (Exception e) {
            log.warn("SLA 升级通知失败: {}", e.getMessage());
        }
    }

    private int currentLevel(Map<String, Object> row) {
        return row.get("escalation_level") == null ? 0 : ((Number) row.get("escalation_level")).intValue();
    }

    private int targetLevel(Map<String, Object> row) {
        Object value = row.get("sla_deadline");
        if (value == null) return 0;
        long deadlineMillis;
        if (value instanceof java.sql.Timestamp ts) {
            deadlineMillis = ts.getTime();
        } else if (value instanceof java.time.LocalDateTime ldt) {
            deadlineMillis = ldt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        } else {
            deadlineMillis = Long.parseLong(String.valueOf(value));
        }
        long overdueHours = (System.currentTimeMillis() - deadlineMillis) / 3_600_000L;
        if (overdueHours >= properties.getSlaLevel3Hours()) return 3;
        if (overdueHours >= properties.getSlaLevel2Hours()) return 2;
        return 1;
    }
}
