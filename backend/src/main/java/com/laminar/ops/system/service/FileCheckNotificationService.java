package com.laminar.ops.system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileCheckNotificationService {

    private final JdbcTemplate jdbc;
    private final MailNotificationService mailNotificationService;
    private final FileCheckRecipientService recipientService;
    private final FileCheckQueryService queryService;
    private final FileCheckExportService exportService;

    public void sendMissingEmailRows(List<Map<String, Object>> rows) {
        if (rows.isEmpty() || !mailNotificationService.isEnabled()) return;
        byte[] attachment = exportService.excel(queryService.exportRows("0", null, null, null, null, null));
        Map<String, List<Map<String, Object>>> groups = rows.stream().collect(Collectors.groupingBy(row ->
                (row.get("companyId") == null ? "-" : row.get("companyId")) + "|" +
                        (row.get("hospitalId") == null ? "-" : row.get("hospitalId"))));
        boolean sent = false;
        for (List<Map<String, Object>> group : groups.values()) {
            Map<String, Object> first = group.get(0);
            Long companyId = longOrNull(first.get("companyId"));
            Long hospitalId = longOrNull(first.get("hospitalId"));
            List<String> emails = recipientService.emails(companyId, hospitalId);
            if (emails.isEmpty()) continue;
            sent = true;
            String subject = "文件巡检发现 " + group.size() + " 个文件丢失";
            mailNotificationService.sendHtml(emails, subject, htmlMapReport(group), attachment, "file-check.xlsx");
        }
        if (!sent) {
            mailNotificationService.sendHtml(null, "文件巡检发现 " + rows.size() + " 个文件丢失",
                    htmlMapReport(rows), attachment, "file-check.xlsx");
        }
    }

    private String htmlMapReport(List<Map<String, Object>> rows) {
        StringBuilder html = new StringBuilder("<div style=\"font-family:Arial,sans-serif\">");
        html.append("<h3 style=\"color:#c0392b\">文件巡检报告</h3>");
        html.append("<p>共发现 <b>").append(rows.size()).append("</b> 个文件丢失，请登录系统管理-文件巡检处理。</p>");
        html.append("<table border=\"1\" cellspacing=\"0\" cellpadding=\"6\" style=\"border-collapse:collapse;font-size:12px\">");
        html.append("<tr><th>等级</th><th>表</th><th>记录</th><th>类型</th><th>路径</th></tr>");
        for (Map<String, Object> row : rows.stream().limit(30).toList()) {
            html.append("<tr><td>").append(escape(text(row, "severity")))
                    .append("</td><td>").append(escape(text(row, "tableName")))
                    .append("</td><td>").append(text(row, "recordId"))
                    .append("</td><td>").append(escape(text(row, "fileType")))
                    .append("</td><td>").append(escape(text(row, "filePath"))).append("</td></tr>");
        }
        return html.append("</table><p>完整明细见附件。</p></div>").toString();
    }

    private String text(Map<String, Object> row, String key) {
        return row.get(key) == null ? "" : String.valueOf(row.get(key));
    }

    private Long longOrNull(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    public void notifyGroups(long runId, List<FileCheckModels.Missing> missing, Map<String, Long> itemIds) {
        Map<String, List<FileCheckModels.Missing>> byCompany = missing.stream()
                .filter(m -> m.context().companyId() != null)
                .collect(Collectors.groupingBy(m -> "c" + m.context().companyId()));
        Map<String, List<FileCheckModels.Missing>> byHospital = missing.stream()
                .filter(m -> m.context().hospitalId() != null)
                .collect(Collectors.groupingBy(m -> "h" + m.context().hospitalId()));
        byCompany.forEach((key, list) -> insertMessage("213", list.get(0).context().companyId(), null, list, runId, itemIds));
        byHospital.forEach((key, list) -> insertMessage("200", null, list.get(0).context().hospitalId(), list, runId, itemIds));
        sendMissingEmails(missing);
    }

    private void insertMessage(String role, Long companyId, Long hospitalId, List<FileCheckModels.Missing> list,
                               long runId, Map<String, Long> itemIds) {
        try {
            Long firstItem = itemIds.get(FileCheckModels.key(list.get(0)));
            if (firstItem == null) return;
            String samples = list.stream().limit(5).map(m -> m.ref().table() + "#" + m.recordId())
                    .collect(Collectors.joining("、"));
            String title = "文件巡检发现 " + list.size() + " 个文件丢失";
            String content = "请到系统管理-文件巡检处理。样本：" + samples;
            jdbc.update("insert into biz_message(msg_type,title,content,level,biz_type,biz_id,biz_key,receiver_id,receiver_role," +
                            "hospital_id,company_id,is_read,create_time) values('system',?,?,'1','file_check',?,?,0,?,?,?,'0',now())",
                    title, content, firstItem, String.valueOf(runId), role, hospitalId, companyId);
        } catch (Exception e) {
            log.warn("文件巡检分组通知写入失败: {}", e.getMessage());
        }
    }

    private void sendMissingEmails(List<FileCheckModels.Missing> missing) {
        if (missing.isEmpty() || !mailNotificationService.isEnabled()) return;
        byte[] attachment = exportService.excel(queryService.exportRows("0", null, null, null, null, null));
        Map<String, List<FileCheckModels.Missing>> groups = missing.stream().collect(Collectors.groupingBy(m ->
                (m.context().companyId() == null ? "-" : m.context().companyId()) + "|" +
                        (m.context().hospitalId() == null ? "-" : m.context().hospitalId())));
        boolean sent = false;
        for (List<FileCheckModels.Missing> group : groups.values()) {
            FileCheckModels.Missing first = group.get(0);
            List<String> emails = recipientService.emails(first.context().companyId(), first.context().hospitalId());
            if (emails.isEmpty()) continue;
            sent = true;
            String subject = "文件巡检发现 " + group.size() + " 个文件丢失";
            mailNotificationService.sendHtml(emails, subject, htmlReport(group), attachment, "file-check.xlsx");
        }
        if (!sent) {
            mailNotificationService.sendHtml(null, "文件巡检发现 " + missing.size() + " 个文件丢失",
                    htmlReport(missing), attachment, "file-check.xlsx");
        }
    }

    private String htmlReport(List<FileCheckModels.Missing> rows) {
        StringBuilder html = new StringBuilder("<div style=\"font-family:Arial,sans-serif\">");
        html.append("<h3 style=\"color:#c0392b\">文件巡检报告</h3>");
        html.append("<p>共发现 <b>").append(rows.size()).append("</b> 个文件丢失，请登录系统管理-文件巡检处理。</p>");
        html.append("<table border=\"1\" cellspacing=\"0\" cellpadding=\"6\" style=\"border-collapse:collapse;font-size:12px\">");
        html.append("<tr><th>等级</th><th>表</th><th>记录</th><th>类型</th><th>路径</th></tr>");
        for (FileCheckModels.Missing row : rows.stream().limit(30).toList()) {
            html.append("<tr><td>").append(escape(row.severity()))
                    .append("</td><td>").append(escape(row.ref().table()))
                    .append("</td><td>").append(row.recordId())
                    .append("</td><td>").append(escape(row.fileType()))
                    .append("</td><td>").append(escape(row.path())).append("</td></tr>");
        }
        return html.append("</table><p>完整明细见附件。</p></div>").toString();
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
