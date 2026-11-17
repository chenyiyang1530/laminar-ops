package com.laminar.ops.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件存在性巡检：扫描业务表中的文件路径，核对上传目录中的真实文件。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileExistenceCheckJob {

    private static final List<FileRef> REFS = List.of(
            new FileRef("biz_certificate", "cert_id", "file_url"),
            new FileRef("biz_company_qualification", "qualification_id", "file_url"),
            new FileRef("biz_contract", "contract_id", "attachment"),
            new FileRef("biz_document", "doc_id", "file_url"),
            new FileRef("biz_inspection_item", "item_id", "image_url"),
            new FileRef("biz_workorder", "workorder_id", "finish_image"),
            new FileRef("biz_workorder", "workorder_id", "acceptance_image"),
            new FileRef("biz_workorder_archive", "workorder_id", "finish_image"),
            new FileRef("biz_workorder_archive", "workorder_id", "acceptance_image")
    );

    private final JdbcTemplate jdbc;
    private final SystemAlertService systemAlertService;

    @Value("${laminar.upload.path:}")
    private String uploadPath;

    @Value("${laminar.file-check.enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "${laminar.file-check.cron:0 30 3 * * *}")
    public void scheduledCheck() {
        if (!enabled) return;
        check();
    }

    public Map<String, Object> check() {
        Path root = Path.of(uploadPath).toAbsolutePath().normalize();
        int checked = 0;
        int missing = 0;
        List<String> samples = new ArrayList<>();

        for (FileRef ref : REFS) {
            String sql = "select `" + ref.idColumn() + "` as id, `" + ref.fileColumn() + "` as path " +
                    "from `" + ref.table() + "` where `" + ref.fileColumn() + "` is not null and `" + ref.fileColumn() + "` <> ''";
            try {
                List<Map<String, Object>> rows = jdbc.queryForList(sql);
                for (Map<String, Object> row : rows) {
                    String relative = normalize(String.valueOf(row.get("path")));
                    if (relative == null) continue;
                    checked++;
                    Path target = root.resolve(relative).normalize();
                    if (!target.startsWith(root) || !Files.isRegularFile(target)) {
                        missing++;
                        if (samples.size() < 10) {
                            samples.add(ref.table() + "#" + row.get("id") + " " + ref.fileColumn() + "=" + relative);
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("文件巡检跳过 {}.{}: {}", ref.table(), ref.fileColumn(), e.getMessage());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("checked", checked);
        result.put("missing", missing);
        result.put("samples", samples);
        result.put("time", LocalDateTime.now().toString());

        if (missing > 0) {
            String content = "共检查 " + checked + " 个文件引用，发现 " + missing + " 个丢失：" + String.join("；", samples);
            systemAlertService.error("file-existence-missing", "文件存在性巡检告警", content);
            log.warn("[文件巡检] checked={} missing={} samples={}", checked, missing, samples);
        } else {
            log.info("[文件巡检] checked={} missing=0", checked);
        }
        return result;
    }

    private String normalize(String raw) {
        if (raw == null) return null;
        String value = raw.trim().replace('\\', '/');
        if (value.isBlank() || value.startsWith("http://") || value.startsWith("https://")) return null;
        if (value.startsWith("/upload/")) value = value.substring("/upload/".length());
        while (value.startsWith("/")) value = value.substring(1);
        if (value.isBlank() || value.contains("..")) return null;
        return value;
    }

    private record FileRef(String table, String idColumn, String fileColumn) {
    }
}
