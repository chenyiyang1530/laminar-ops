package com.laminar.ops.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laminar.ops.common.Result;
import com.laminar.ops.security.AuthContext;
import com.laminar.ops.security.Perm;
import com.laminar.ops.system.service.FileCheckAsyncService;
import com.laminar.ops.system.service.FileCheckExportService;
import com.laminar.ops.system.service.FileCheckService;
import com.laminar.ops.system.service.FileCheckRecipientService;
import com.laminar.ops.system.service.FileCheckSlaJob;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/file-check")
@RequiredArgsConstructor
public class FileCheckController {

    private final FileCheckService fileCheckService;
    private final FileCheckExportService fileCheckExportService;
    private final FileCheckSlaJob fileCheckSlaJob;
    private final FileCheckRecipientService fileCheckRecipientService;
    private final FileCheckAsyncService fileCheckAsyncService;
    private final ObjectMapper objectMapper;

    @PostMapping("/run")
    @Perm("system:filecheck:run")
    public Result<Map<String, Object>> run() {
        return Result.ok(fileCheckService.run("manual"));
    }

    @GetMapping("/runs")
    @Perm("system:filecheck:list")
    public Result<Map<String, Object>> runs(@RequestParam(defaultValue = "1") int pageNum,
                                             @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(fileCheckService.pageRuns(pageNum, pageSize));
    }

    @PostMapping("/sla-run")
    @Perm("system:filecheck:run")
    public Result<Void> slaRun() {
        fileCheckSlaJob.escalate();
        return Result.ok();
    }

    @GetMapping("/recipients")
    @Perm("system:filecheck:list")
    public Result<List<Map<String, Object>>> recipients() {
        return Result.ok(fileCheckRecipientService.list());
    }

    @PostMapping("/recipients")
    @Perm("system:filecheck:edit")
    public Result<Map<String, Object>> saveRecipient(@RequestBody Map<String, Object> body) {
        return Result.ok(fileCheckRecipientService.save(body));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/recipients/{id}")
    @Perm("system:filecheck:edit")
    public Result<Void> removeRecipient(@PathVariable Long id) {
        fileCheckRecipientService.remove(id);
        return Result.ok();
    }

    @PostMapping("/items/batch-recover")
    @Perm("system:filecheck:edit")
    public Result<Map<String, Object>> batchRecover(@RequestBody List<Map<String, String>> items) {
        String user = AuthContext.require().getNickName();
        try {
            long jobId = fileCheckAsyncService.createJob("batch_recover", user, objectMapper.writeValueAsString(items));
            fileCheckAsyncService.batchRecover(jobId, items, user);
            return Result.ok(Map.of("jobId", jobId));
        } catch (Exception e) {
            throw new com.laminar.ops.common.BizException(500, "提交批量恢复失败：" + e.getMessage());
        }
    }

    @PostMapping("/export-async")
    @Perm("system:filecheck:list")
    public Result<Map<String, Object>> exportAsync(@RequestParam(defaultValue = "xlsx") String format,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) Long companyId,
                                                   @RequestParam(required = false) Long hospitalId,
                                                   @RequestParam(required = false) String fileType,
                                                   @RequestParam(required = false) String severity) throws Exception {
        String user = AuthContext.require().getNickName();
        java.util.Map<String, String> payload = new java.util.LinkedHashMap<>();
        payload.put("format", format); payload.put("status", status); payload.put("keyword", keyword);
        payload.put("companyId", companyId == null ? null : String.valueOf(companyId));
        payload.put("hospitalId", hospitalId == null ? null : String.valueOf(hospitalId));
        payload.put("fileType", fileType); payload.put("severity", severity);
        long jobId = fileCheckAsyncService.createJob("file_check_export", user, objectMapper.writeValueAsString(payload));
        fileCheckAsyncService.exportAsync(jobId, format, status, keyword, companyId, hospitalId, fileType, severity, user);
        return Result.ok(Map.of("jobId", jobId));
    }

    @GetMapping("/jobs")
    @Perm("system:filecheck:list")
    public Result<Map<String, Object>> jobs(@RequestParam(defaultValue = "1") int pageNum,
                                             @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(fileCheckAsyncService.list(pageNum, pageSize));
    }

    @PostMapping("/jobs/{id}/cancel")
    @Perm("system:filecheck:run")
    public Result<Void> cancelJob(@PathVariable Long id) {
        fileCheckAsyncService.cancel(id);
        return Result.ok();
    }

    @PostMapping("/jobs/cancel")
    @Perm("system:filecheck:run")
    public Result<Map<String, Object>> cancelJobs(@RequestBody List<Long> ids) {
        return Result.ok(Map.of("cancelled", fileCheckAsyncService.cancelAll(ids)));
    }

    @PostMapping("/jobs/{id}/retry")
    @Perm("system:filecheck:run")
    public Result<Map<String, Object>> retryJob(@PathVariable Long id) {
        return Result.ok(Map.of("jobId", fileCheckAsyncService.retry(id, AuthContext.require().getNickName())));
    }

    @GetMapping("/jobs/{id}")
    @Perm("system:filecheck:list")
    public Result<Map<String, Object>> job(@PathVariable Long id) {
        Map<String, Object> job = fileCheckAsyncService.job(id);
        return job == null ? Result.fail("任务不存在") : Result.ok(job);
    }

    @GetMapping("/jobs/{id}/download")
    @Perm("system:filecheck:list")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        java.nio.file.Path file = fileCheckAsyncService.result(id);
        if (file == null) return ResponseEntity.notFound().build();
        String name = file.getFileName().toString();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(name).build().toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new FileSystemResource(file));
    }

    @GetMapping("/items")
    @Perm("system:filecheck:list")
    public Result<Map<String, Object>> items(@RequestParam(defaultValue = "1") int pageNum,
                                              @RequestParam(defaultValue = "20") int pageSize,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Long companyId,
                                              @RequestParam(required = false) Long hospitalId,
                                              @RequestParam(required = false) String fileType,
                                              @RequestParam(required = false) String severity,
                                              @RequestParam(required = false) Long itemId) {
        return Result.ok(fileCheckService.pageItems(pageNum, pageSize, status, keyword,
                companyId, hospitalId, fileType, severity, itemId));
    }

    @GetMapping("/trend")
    @Perm("system:filecheck:list")
    public Result<List<Map<String, Object>>> trend(@RequestParam(defaultValue = "30") int days,
                                                    @RequestParam(required = false) Long companyId,
                                                    @RequestParam(required = false) Long hospitalId,
                                                    @RequestParam(defaultValue = "day") String granularity) {
        return Result.ok(fileCheckService.trend(days, companyId, hospitalId, granularity));
    }

    @GetMapping("/export")
    @Perm("system:filecheck:list")
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue = "xlsx") String format,
                                         @RequestParam(required = false) String status,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) Long companyId,
                                         @RequestParam(required = false) Long hospitalId,
                                         @RequestParam(required = false) String fileType,
                                         @RequestParam(required = false) String severity) {
        List<Map<String, Object>> rows = fileCheckService.exportRows(status, keyword, companyId, hospitalId, fileType, severity);
        boolean pdf = "pdf".equalsIgnoreCase(format);
        byte[] body = pdf ? fileCheckExportService.pdf(rows) : fileCheckExportService.excel(rows);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(pdf ? MediaType.APPLICATION_PDF : MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(pdf ? "file-check.pdf" : "file-check.xlsx").build());
        return ResponseEntity.ok().headers(headers).body(body);
    }

    @PostMapping("/items/{id}/recover")
    @Perm("system:filecheck:edit")
    public Result<Map<String, Object>> recover(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.ok(fileCheckService.recover(id, body == null ? null : body.get("fileUrl")));
    }
}


