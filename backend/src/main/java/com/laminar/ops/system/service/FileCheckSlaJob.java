package com.laminar.ops.system.service;

import com.laminar.ops.common.SystemAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SLA 超时升级：对未恢复且已超过截止时间的丢失文件发升级通知。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileCheckSlaJob {

    private final JdbcTemplate jdbc;
    private final SystemAlertService systemAlertService;
    private final MailNotificationService mailNotificationService;

    @Value("${laminar.file-check.enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "${laminar.file-check.sla-cron:0 */10 * * * *}")
    public void escalate() {
        if (!enabled) return;
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select i.item_id, i.table_name, i.record_id, i.file_column, i.company_id, i.hospital_id, " +
                        "c.company_name, h.hospital_name from sys_file_check_item i " +
                        "left join biz_company c on c.company_id=i.company_id " +
                        "left join biz_hospital h on h.hospital_id=i.hospital_id " +
                        "where i.status='0' and i.escalated='0' and i.sla_deadline is not null " +
                        "and i.sla_deadline < now() order by i.sla_deadline limit 200");
        if (rows.isEmpty()) return;

        Map<String, List<Map<String, Object>>> companies = rows.stream()
                .filter(r -> r.get("company_id") != null)
                .collect(Collectors.groupingBy(r -> String.valueOf(r.get("company_id"))));
        Map<String, List<Map<String, Object>>> hospitals = rows.stream()
                .filter(r -> r.get("hospital_id") != null)
                .collect(Collectors.groupingBy(r -> String.valueOf(r.get("hospital_id"))));
        companies.forEach((id, list) -> send("213", Long.valueOf(id), null, list));
        hospitals.forEach((id, list) -> send("200", null, Long.valueOf(id), list));

        List<Object> ids = rows.stream().map(r -> r.get("item_id")).toList();
        String placeholders = ids.stream().map(x -> "?").collect(Collectors.joining(","));
        jdbc.update("update sys_file_check_item set escalated='1', escalated_time=now() where item_id in (" + placeholders + ")", ids.toArray());
        systemAlertService.error("file-sla-overdue", "文件丢失 SLA 超时",
                "有 " + rows.size() + " 个文件丢失已超过 SLA，请及时处理");
        mailNotificationService.send("文件丢失 SLA 超时", "有 " + rows.size() + " 个文件丢失已超过 SLA，请登录管理端处理。");
        log.warn("[文件巡检] SLA 超时升级 {} 项", rows.size());
    }

    private void send(String role, Long companyId, Long hospitalId, List<Map<String, Object>> list) {
        try {
            Map<String, Object> first = list.get(0);
            String title = "文件丢失 SLA 超时：" + list.size() + " 个";
            String content = "请及时处理丢失文件，首项：" + first.get("table_name") + "#" + first.get("record_id");
            jdbc.update("insert into biz_message(msg_type,title,content,level,biz_type,biz_id,biz_key,receiver_id,receiver_role," +
                            "hospital_id,company_id,is_read,create_time) values('system',?,?,'1','file_check',?,null,0,?,?,?,'0',now())",
                    title, content, first.get("item_id"), role, hospitalId, companyId);
        } catch (Exception e) {
            log.warn("SLA 升级通知失败: {}", e.getMessage());
        }
    }
}
