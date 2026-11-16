package com.laminar.ops.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 清理未被业务表引用的历史上传文件。
 * 默认关闭，只能通过平台参数或管理员手动执行；支持 dryRun。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UploadCleanupService {

    private final JdbcTemplate jdbc;

    @Value("${laminar.upload.path:./upload}")
    private String uploadPath;

    @Value("${laminar.upload.cleanup-enabled:false}")
    private boolean enabled;

    @Value("${laminar.upload.cleanup-retention-days:180}")
    private int retentionDays;

    @Scheduled(cron = "0 0 4 * * MON")
    public void scheduled() {
        if (!enabled) return;
        try {
            Map<String, Object> result = cleanup(false);
            log.info("[上传清理] {}", result);
        } catch (Exception e) {
            log.warn("[上传清理] 失败: {}", e.getMessage());
        }
    }

    public Map<String, Object> cleanup(boolean dryRun) {
        Path root = Path.of(uploadPath).toAbsolutePath().normalize();
        Set<String> referenced = referencedPaths();
        Set<String> deleted = new HashSet<>();
        long cutoff = Instant.now().minus(Math.max(1, retentionDays), ChronoUnit.DAYS).toEpochMilli();
        int scanned = 0;
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                if (file.getFileName().toString().startsWith(".")) continue;
                if (file.getParent() != null && file.getParent().getFileName().toString().equals(".scan")) continue;
                scanned++;
                String relative = root.relativize(file).toString().replace('\\', '/');
                if (referenced.contains(relative) || referenced.contains("/upload/" + relative)) continue;
                FileTime modified = Files.getLastModifiedTime(file);
                if (modified.toMillis() >= cutoff) continue;
                deleted.add(relative);
                if (!dryRun) Files.deleteIfExists(file);
            }
        } catch (Exception e) {
            throw new IllegalStateException("上传文件清理失败: " + e.getMessage(), e);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scanned", scanned);
        result.put("deleted", deleted.size());
        result.put("dryRun", dryRun);
        result.put("files", new ArrayList<>(deleted).stream().limit(200).toList());
        return result;
    }

    private Set<String> referencedPaths() {
        Set<String> values = new HashSet<>();
        add(values, "select file_url from biz_certificate where file_url is not null");
        add(values, "select file_url from biz_company_qualification where file_url is not null");
        add(values, "select attachment from biz_contract where attachment is not null");
        add(values, "select file_url from biz_document where file_url is not null");
        add(values, "select image_url from biz_inspection_item where image_url is not null");
        add(values, "select finish_image from biz_workorder where finish_image is not null");
        add(values, "select acceptance_image from biz_workorder where acceptance_image is not null");
        add(values, "select finish_image from biz_workorder_archive where finish_image is not null");
        add(values, "select acceptance_image from biz_workorder_archive where acceptance_image is not null");
        add(values, "select config_value from sys_config where config_key='platform.logoUrl' and config_value is not null");
        return values;
    }

    private void add(Set<String> values, String sql) {
        try {
            List<String> rows = jdbc.queryForList(sql, String.class);
            for (String row : rows) {
                if (row == null || row.isBlank()) continue;
                values.add(row.trim());
                if (row.startsWith("/upload/")) values.add(row.substring("/upload/".length()));
            }
        } catch (Exception e) {
            log.debug("跳过文件引用查询: {} {}", sql, e.getMessage());
        }
    }
}
