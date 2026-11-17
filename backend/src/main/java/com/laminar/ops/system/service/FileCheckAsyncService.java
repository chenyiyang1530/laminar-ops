package com.laminar.ops.system.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileCheckAsyncService {

    private final JdbcTemplate jdbc;
    private final FileCheckService fileCheckService;
    private final FileCheckExportService exportService;
    private final ObjectMapper objectMapper;

    @Value("${laminar.upload.path:}")
    private String uploadPath;

    public long createJob(String type, String username, String payload) {
        KeyHolder holder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "insert into sys_async_job(job_type,status,progress,create_by,payload,create_time,update_time) " +
                            "values(?,'running',0,?,?,now(),now())", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, type);
            ps.setString(2, username);
            ps.setString(3, payload);
            return ps;
        }, holder);
        return holder.getKey() == null ? 0 : holder.getKey().longValue();
    }

    @Async
    public void batchRecover(long jobId, List<Map<String, String>> items, String username) {
        int done = 0;
        List<String> failures = new ArrayList<>();
        List<Long> failedIds = new ArrayList<>();
        try {
            for (Map<String, String> item : items) {
                if (cancelled(jobId)) {
                    finish(jobId, "cancelled", "任务已取消");
                    return;
                }
                try {
                    Long id = Long.valueOf(String.valueOf(item.get("itemId")));
                    fileCheckService.recover(id, item.get("fileUrl"));
                    done++;
                } catch (Exception e) {
                    failures.add(String.valueOf(item.get("itemId")) + ":" + e.getMessage());
                    failedIds.add(Long.valueOf(String.valueOf(item.get("itemId"))));
                }
                update(jobId, done * 100 / Math.max(1, items.size()), "已处理 " + done + "/" + items.size());
            }
            String message = "成功 " + done + "，失败 " + failures.size()
                    + (failedIds.isEmpty() ? "" : "；失败ID=" + failedIds)
                    + (failures.isEmpty() ? "" : "；" + String.join("；", failures.stream().limit(10).toList()));
            finish(jobId, failures.isEmpty() ? "success" : "partial", message);
        } catch (Exception e) {
            log.error("批量恢复任务失败 {}", jobId, e);
            finish(jobId, "failed", e.getMessage());
        }
    }

    @Async
    public void exportAsync(long jobId, String format, String status, String keyword,
                            Long companyId, Long hospitalId, String fileType, String severity, String username) {
        try {
            List<Map<String, Object>> rows = fileCheckService.exportRows(status, keyword, companyId, hospitalId, fileType, severity);
            if (cancelled(jobId)) {
                finish(jobId, "cancelled", "任务已取消");
                return;
            }
            update(jobId, 30, "查询完成，共 " + rows.size() + " 条，生成文件");
            byte[] data = "pdf".equalsIgnoreCase(format) ? exportService.pdf(rows) : exportService.excel(rows);
            Path dir = Path.of(uploadPath).toAbsolutePath().normalize().resolve("temp/exports");
            Files.createDirectories(dir);
            String ext = "pdf".equalsIgnoreCase(format) ? "pdf" : "xlsx";
            Path file = dir.resolve("file-check-" + jobId + "." + ext).normalize();
            Files.write(file, data);
            jdbc.update("update sys_async_job set status='success', progress=100, message=?, result_path=?, update_time=now() where job_id=?",
                    "导出完成，共 " + rows.size() + " 条", "temp/exports/" + file.getFileName(), jobId);
        } catch (Exception e) {
            log.error("异步导出任务失败 {}", jobId, e);
            finish(jobId, "failed", e.getMessage());
        }
    }

    public Map<String, Object> job(long jobId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select job_id as jobId, job_type as jobType, status, progress, message, result_path as resultPath, " +
                        "payload, create_by as createBy, create_time as createTime, update_time as updateTime " +
                        "from sys_async_job where job_id=?", jobId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Map<String, Object> list(int pageNum, int pageSize) {
        int offset = Math.max(0, (pageNum - 1) * pageSize);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select job_id as jobId, job_type as jobType, status, progress, message, create_by as createBy, " +
                        "create_time as createTime, update_time as updateTime from sys_async_job " +
                        "order by job_id desc limit ? offset ?", pageSize, offset);
        Integer total = jdbc.queryForObject("select count(*) from sys_async_job", Integer.class);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", rows);
        result.put("total", total == null ? 0 : total);
        return result;
    }

    public void cancel(long jobId) {
        jdbc.update("update sys_async_job set status='cancelled', update_time=now() where job_id=? and status='running'", jobId);
    }

    @SuppressWarnings("unchecked")
    public long retry(long jobId, String username) {
        Map<String, Object> job = job(jobId);
        if (job == null) throw new RuntimeException("任务不存在");
        String status = String.valueOf(job.get("status"));
        if (!List.of("failed", "partial", "cancelled").contains(status)) {
            throw new RuntimeException("只有失败、部分成功或已取消的任务可以重试");
        }
        String type = String.valueOf(job.get("jobType"));
        String payload = job.get("payload") == null ? null : String.valueOf(job.get("payload"));
        long newId = createJob(type, username, payload);
        try {
            if ("batch_recover".equals(type)) {
                List<Map<String, String>> items = objectMapper.readValue(payload, List.class);
                batchRecover(newId, items, username);
            } else if ("file_check_export".equals(type)) {
                Map<String, String> p = objectMapper.readValue(payload, Map.class);
                exportAsync(newId, p.get("format"), p.get("status"), p.get("keyword"),
                        p.get("companyId") == null ? null : Long.valueOf(p.get("companyId")),
                        p.get("hospitalId") == null ? null : Long.valueOf(p.get("hospitalId")),
                        p.get("fileType"), p.get("severity"), username);
            }
        } catch (Exception e) {
            finish(newId, "failed", e.getMessage());
        }
        return newId;
    }

    private boolean cancelled(long jobId) {
        try {
            String status = jdbc.queryForObject("select status from sys_async_job where job_id=?", String.class, jobId);
            return "cancelled".equals(status);
        } catch (Exception e) {
            return false;
        }
    }

    public Path result(long jobId) {
        Map<String, Object> job = job(jobId);
        if (job == null) return null;
        String path = String.valueOf(job.get("resultPath"));
        if (path == null || "null".equals(path) || path.isBlank()) return null;
        Path root = Path.of(uploadPath).toAbsolutePath().normalize();
        Path file = root.resolve(path).normalize();
        return file.startsWith(root) && Files.isRegularFile(file) ? file : null;
    }

    private void update(long jobId, int progress, String message) {
        jdbc.update("update sys_async_job set progress=?, message=?, update_time=now() where job_id=?",
                Math.min(100, progress), message, jobId);
    }

    private void finish(long jobId, String status, String message) {
        jdbc.update("update sys_async_job set status=?, progress=100, message=?, update_time=now() where job_id=?",
                status, message == null ? "" : message.substring(0, Math.min(message.length(), 1900)), jobId);
    }
}
