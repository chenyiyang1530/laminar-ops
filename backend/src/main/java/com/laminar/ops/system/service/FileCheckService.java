package com.laminar.ops.system.service;

import com.laminar.ops.common.BizException;
import com.laminar.ops.common.SystemAlertService;
import com.laminar.ops.security.AuthContext;
import com.laminar.ops.security.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 文件存在性巡检：分批扫描、并发检查、历史留痕、按公司/医院通知和重新上传恢复。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileCheckService {

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

    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);

    private final JdbcTemplate jdbc;
    private final SystemAlertService systemAlertService;
    private final FileCheckProperties properties;
    private final MailNotificationService mailNotificationService;

    @Value("${laminar.upload.path:}")
    private String uploadPath;

    public Map<String, Object> run(String triggerType) {
        if (!RUNNING.compareAndSet(false, true)) {
            return Map.of("status", "running");
        }
        long runId = findResumableRun();
        if (runId == 0) runId = createRun(triggerType);
        long start = System.currentTimeMillis();
        try {
            Path root = Path.of(uploadPath).toAbsolutePath().normalize();
            ScanResult scan = scanAll(root, runId);
            MissingItems loaded = loadMissing(runId);
            List<Missing> missing = loaded.missing();
            Map<String, Long> itemIds = loaded.ids();
            int recovered = markRecovered(runId);

            jdbc.update("update sys_file_check_run set status='success', checked=?, missing=?, recovered=?, end_time=now() where run_id=?",
                    scan.checked(), missing.size(), recovered, runId);
            notifyGroups(runId, missing, itemIds);
            if (!missing.isEmpty()) {
                String samples = missing.stream().limit(10)
                        .map(m -> m.ref().table() + "#" + m.recordId() + " " + m.ref().column() + "=" + m.path())
                        .collect(Collectors.joining("；"));
                systemAlertService.error("file-existence-missing", "文件存在性巡检告警",
                        "发现 " + missing.size() + " 个文件丢失：" + samples);
                mailNotificationService.send("文件巡检发现 " + missing.size() + " 个文件丢失",
                        "文件巡检批次 " + runId + " 发现 " + missing.size() + " 个文件丢失。\n" + samples);
            }
            if ("scheduled".equals(triggerType)) {
                audit("文件巡检完成", "0", null, "batch=" + runId + " checked=" + scan.checked() + " missing=" + missing.size());
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("runId", runId);
            result.put("status", "success");
            result.put("checked", scan.checked());
            result.put("missing", missing.size());
            result.put("recovered", recovered);
            result.put("elapsedMs", System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            String message = e.getMessage() == null ? "unknown" : e.getMessage();
            log.error("[文件巡检] 执行失败", e);
            jdbc.update("update sys_file_check_run set status='failed', end_time=now(), error_msg=? where run_id=?",
                    message.substring(0, Math.min(message.length(), 500)), runId);
            audit("文件巡检失败", "1", message, "batch=" + runId + " trigger=" + triggerType);
            throw new BizException(500, "文件巡检失败：" + message);
        } finally {
            RUNNING.set(false);
        }
    }

    public Map<String, Object> pageRuns(int pageNum, int pageSize) {
        int offset = Math.max(0, (pageNum - 1) * pageSize);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select run_id as runId, trigger_type as triggerType, status, checked, missing, recovered, " +
                        "start_time as startTime, end_time as endTime, error_msg as errorMsg " +
                        "from sys_file_check_run order by run_id desc limit ? offset ?", pageSize, offset);
        Integer total = jdbc.queryForObject("select count(*) from sys_file_check_run", Integer.class);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", rows);
        result.put("total", total == null ? 0 : total);
        return result;
    }

    public Map<String, Object> pageItems(int pageNum, int pageSize, String status, String keyword,
                                         Long companyId, Long hospitalId, String fileType, String severity, Long itemId) {        int offset = Math.max(0, (pageNum - 1) * pageSize);
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" where 1=1 ");
        if (status != null && !status.isBlank()) { where.append(" and i.status=?"); args.add(status); }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" and (i.file_path like ? or i.table_name like ?)");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
        }
        if (companyId != null) { where.append(" and i.company_id=?"); args.add(companyId); }
        if (hospitalId != null) { where.append(" and i.hospital_id=?"); args.add(hospitalId); }
        if (fileType != null && !fileType.isBlank()) { where.append(" and i.file_type=?"); args.add(fileType); }
        if (severity != null && !severity.isBlank()) { where.append(" and i.severity=?"); args.add(severity); }
        if (itemId != null) { where.append(" and i.item_id=?"); args.add(itemId); }
        Integer total = jdbc.queryForObject("select count(*) from sys_file_check_item i" + where, Integer.class, args.toArray());
        args.add(pageSize);
        args.add(offset);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select i.item_id as itemId, i.run_id as runId, i.table_name as tableName, i.record_id as recordId, " +
                        "i.file_column as fileColumn, i.file_path as filePath, i.file_type as fileType, i.severity, " +
                        "i.sla_hours as slaHours, i.sla_deadline as slaDeadline, " +
                        "i.company_id as companyId, i.hospital_id as hospitalId, i.status, " +
                        "i.first_seen as firstSeen, i.last_seen as lastSeen, i.resolved_time as resolvedTime, " +
                        "c.company_name as companyName, h.hospital_name as hospitalName " +
                        "from sys_file_check_item i left join biz_company c on c.company_id=i.company_id " +
                        "left join biz_hospital h on h.hospital_id=i.hospital_id" + where +
                        " order by i.status asc, i.severity asc, i.last_seen desc limit ? offset ?", args.toArray());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", rows);
        result.put("total", total == null ? 0 : total);
        return result;
    }

    public List<Map<String, Object>> exportRows(String status, String keyword, Long companyId,
                                                Long hospitalId, String fileType, String severity) {
        return (List<Map<String, Object>>) pageItems(1, 10000, status, keyword,
                companyId, hospitalId, fileType, severity, null).get("records");
    }

    public List<Map<String, Object>> trend(int days, Long companyId, Long hospitalId) {
        int span = Math.max(1, Math.min(days, 365));
        LocalDate start = LocalDate.now().minusDays(span - 1L);
        Map<String, long[]> data = new LinkedHashMap<>();
        for (int i = 0; i < span; i++) {
            data.put(start.plusDays(i).format(DateTimeFormatter.ISO_DATE), new long[]{0, 0});
        }
        List<Object> args = new ArrayList<>();
        args.add(start);
        StringBuilder scope = new StringBuilder();
        if (companyId != null) { scope.append(" and company_id=?"); args.add(companyId); }
        if (hospitalId != null) { scope.append(" and hospital_id=?"); args.add(hospitalId); }
        jdbc.queryForList("select date(first_seen) d, count(*) c from sys_file_check_item " +
                        "where first_seen>=?" + scope + " group by date(first_seen)", args.toArray())
                .forEach(row -> {
                    String day = String.valueOf(row.get("d"));
                    if (data.containsKey(day)) data.get(day)[0] = ((Number) row.get("c")).longValue();
                });
        List<Object> resolvedArgs = new ArrayList<>(args);
        jdbc.queryForList("select date(resolved_time) d, count(*) c from sys_file_check_item " +
                        "where resolved_time is not null and resolved_time>=?" + scope + " group by date(resolved_time)", resolvedArgs.toArray())
                .forEach(row -> {
                    String day = String.valueOf(row.get("d"));
                    if (data.containsKey(day)) data.get(day)[1] = ((Number) row.get("c")).longValue();
                });
        List<Map<String, Object>> result = new ArrayList<>();
        data.forEach((day, counts) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", day);
            item.put("missing", counts[0]);
            item.put("recovered", counts[1]);
            result.add(item);
        });
        return result;
    }

    public Map<String, Object> recover(Long itemId, String fileUrl) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select item_id, table_name, record_id, file_column, company_id, hospital_id, file_path " +
                        "from sys_file_check_item where item_id=?", itemId);
        if (rows.isEmpty()) throw BizException.badRequest("巡检项不存在");
        Map<String, Object> item = rows.get(0);
        String table = String.valueOf(item.get("table_name"));
        String column = String.valueOf(item.get("file_column"));
        Long recordId = ((Number) item.get("record_id")).longValue();
        FileRef ref = REFS.stream().filter(r -> r.table().equals(table) && r.column().equals(column)).findFirst()
                .orElseThrow(() -> BizException.forbidden("不允许修改该文件字段"));

        String relative = normalize(fileUrl);
        if (relative == null) throw BizException.badRequest("文件地址无效");
        Path root = Path.of(uploadPath).toAbsolutePath().normalize();
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root) || !Files.isRegularFile(target)) {
            throw BizException.badRequest("上传文件不存在");
        }
        int updated = jdbc.update("update `" + ref.table() + "` set `" + ref.column() + "`=? where `" + ref.id() + "`=?",
                relative, recordId);
        if (updated == 0) throw BizException.badRequest("原业务记录不存在");
        jdbc.update("update sys_file_check_item set file_path=?, status='1', resolved_time=now(), last_seen=now() where item_id=?",
                relative, itemId);
        audit("文件恢复", "0", null, "item=" + itemId + " table=" + table + " record=" + recordId + " file=" + relative);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("itemId", itemId);
        result.put("filePath", relative);
        result.put("status", "1");
        return result;
    }

    private ScanResult scanAll(Path root, long runId) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(Math.max(1, Math.min(properties.getThreads(), 16)));
        try {
            List<Future<RefResult>> futures = new ArrayList<>();
            for (FileRef ref : REFS) {
                futures.add(executor.submit(scanRef(runId, root, ref)));
            }
            int checked = 0;
            for (Future<RefResult> future : futures) checked += future.get().checked();
            return new ScanResult(checked);
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<RefResult> scanRef(long runId, Path root, FileRef ref) {
        return () -> {
            String key = ref.table() + "|" + ref.column();
            Map<String, Object> checkpoint = jdbc.queryForList(
                            "select last_id, checked, missing, status from sys_file_check_checkpoint where run_id=? and ref_key=?",
                            runId, key)
                    .stream().findFirst().orElse(null);
            if (checkpoint != null && "done".equals(String.valueOf(checkpoint.get("status")))) {
                return new RefResult(((Number) checkpoint.get("checked")).intValue());
            }
            long lastId = checkpoint == null ? 0 : ((Number) checkpoint.get("last_id")).longValue();
            int checked = checkpoint == null ? 0 : ((Number) checkpoint.get("checked")).intValue();
            int missingCount = checkpoint == null ? 0 : ((Number) checkpoint.get("missing")).intValue();
            int size = Math.max(50, Math.min(properties.getBatchSize(), 5000));
            while (true) {
                List<Map<String, Object>> rows = jdbc.queryForList(
                        "select `" + ref.id() + "` as id, `" + ref.column() + "` as path from `" + ref.table() + "` " +
                                "where `" + ref.column() + "` is not null and `" + ref.column() + "` <> '' and `" + ref.id() + "` > ? " +
                                "order by `" + ref.id() + "` limit ?", lastId, size);
                if (rows.isEmpty()) break;
                for (Map<String, Object> row : rows) {
                    Long recordId = ((Number) row.get("id")).longValue();
                    lastId = recordId;
                    String path = normalize(String.valueOf(row.get("path")));
                    if (path == null) continue;
                    checked++;
                    Path target = root.resolve(path).normalize();
                    if (!target.startsWith(root) || !Files.isRegularFile(target)) {
                        String type = fileType(path);
                        String severity = properties.getTypeSeverity().getOrDefault(type, "P3");
                        int sla = properties.getSlaHours().getOrDefault(severity, 168);
                        upsertMissing(runId, new Missing(ref, recordId, path, context(ref, recordId), type, severity, sla));
                        missingCount++;
                    }
                }
                saveCheckpoint(runId, key, lastId, checked, missingCount, "running");
                if (rows.size() < size) break;
            }
            saveCheckpoint(runId, key, lastId, checked, missingCount, "done");
            return new RefResult(checked);
        };
    }

    private Context context(FileRef ref, Long recordId) {
        if ("biz_inspection_item".equals(ref.table())) return new Context(null, null);
        try {
            Map<String, Object> row = jdbc.queryForMap(
                    "select company_id, hospital_id from `" + ref.table() + "` where `" + ref.id() + "`=?", recordId);
            return new Context(longOrNull(row.get("company_id")), longOrNull(row.get("hospital_id")));
        } catch (Exception e) {
            return new Context(null, null);
        }
    }

    private int markRecovered(long runId) {
        return jdbc.update("update sys_file_check_item set status='1', resolved_time=now() " +
                "where status='0' and run_id<>?", runId);
    }

    private Long upsertMissing(long runId, Missing m) {
        int updated = jdbc.update("update sys_file_check_item set run_id=?, file_path=?, file_type=?, severity=?, " +
                        "sla_hours=?, company_id=?, hospital_id=?, last_seen=now(), status='0', resolved_time=null, " +
                        "escalated='0', escalated_time=null, " +
                        "sla_deadline=case when status='1' or sla_deadline is null then date_add(now(), interval ? hour) else sla_deadline end, " +
                        "first_seen=case when status='1' then now() else first_seen end " +
                        "where table_name=? and record_id=? and file_column=?",
                runId, m.path(), m.fileType(), m.severity(), m.slaHours(),
                m.context().companyId(), m.context().hospitalId(), m.slaHours(),
                m.ref().table(), m.recordId(), m.ref().column());
        if (updated == 0) {
            jdbc.update("insert into sys_file_check_item(run_id, table_name, record_id, file_column, file_path, " +
                            "file_type, severity, sla_hours, sla_deadline, company_id, hospital_id, status, first_seen, last_seen) " +
                            "values(?,?,?,?,?,?,?,?,date_add(now(), interval ? hour),?,?,'0',now(),now())",
                    runId, m.ref().table(), m.recordId(), m.ref().column(), m.path(),
                    m.fileType(), m.severity(), m.slaHours(), m.slaHours(),
                    m.context().companyId(), m.context().hospitalId());
        }
        return jdbc.queryForObject("select item_id from sys_file_check_item where table_name=? and record_id=? and file_column=?",
                Long.class, m.ref().table(), m.recordId(), m.ref().column());
    }

    private MissingItems loadMissing(long runId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select item_id, table_name, record_id, file_column, file_path, file_type, severity, sla_hours, company_id, hospital_id " +
                        "from sys_file_check_item where run_id=? and status='0'", runId);
        List<Missing> missing = new ArrayList<>();
        Map<String, Long> ids = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            FileRef ref = REFS.stream()
                    .filter(r -> r.table().equals(String.valueOf(row.get("table_name")))
                            && r.column().equals(String.valueOf(row.get("file_column"))))
                    .findFirst().orElse(null);
            if (ref == null) continue;
            Long recordId = ((Number) row.get("record_id")).longValue();
            Missing m = new Missing(ref, recordId, String.valueOf(row.get("file_path")),
                    new Context(longOrNull(row.get("company_id")), longOrNull(row.get("hospital_id"))),
                    String.valueOf(row.get("file_type")), String.valueOf(row.get("severity")),
                    ((Number) row.get("sla_hours")).intValue());
            missing.add(m);
            ids.put(itemKey(m), ((Number) row.get("item_id")).longValue());
        }
        return new MissingItems(missing, ids);
    }

    private void saveCheckpoint(long runId, String refKey, long lastId, int checked, int missing, String status) {
        int updated = jdbc.update("update sys_file_check_checkpoint set last_id=?, checked=?, missing=?, status=?, updated_time=now() " +
                        "where run_id=? and ref_key=?", lastId, checked, missing, status, runId, refKey);
        if (updated == 0) {
            jdbc.update("insert into sys_file_check_checkpoint(run_id, ref_key, last_id, checked, missing, status, updated_time) " +
                            "values(?,?,?,?,?,?,now())", runId, refKey, lastId, checked, missing, status);
        }
    }

    private long findResumableRun() {
        List<Long> rows = jdbc.queryForList(
                "select run_id from sys_file_check_run where status='running' order by run_id desc limit 1", Long.class);
        return rows.isEmpty() ? 0 : rows.get(0);
    }

    private void notifyGroups(long runId, List<Missing> missing, Map<String, Long> itemIds) {
        Map<String, List<Missing>> byCompany = missing.stream()
                .filter(m -> m.context().companyId() != null)
                .collect(Collectors.groupingBy(m -> "c" + m.context().companyId()));
        Map<String, List<Missing>> byHospital = missing.stream()
                .filter(m -> m.context().hospitalId() != null)
                .collect(Collectors.groupingBy(m -> "h" + m.context().hospitalId()));
        byCompany.forEach((key, list) -> insertMessage("213", list.get(0).context().companyId(), null, list, runId, itemIds));
        byHospital.forEach((key, list) -> insertMessage("200", null, list.get(0).context().hospitalId(), list, runId, itemIds));
    }

    private void insertMessage(String role, Long companyId, Long hospitalId, List<Missing> list,
                               long runId, Map<String, Long> itemIds) {
        try {
            Long firstItem = itemIds.get(itemKey(list.get(0)));
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

    private long createRun(String triggerType) {
        KeyHolder holder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "insert into sys_file_check_run(trigger_type,status,start_time) values(?,'running',now())",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, triggerType);
            return ps;
        }, holder);
        return holder.getKey() == null ? 0 : holder.getKey().longValue();
    }

    private void audit(String title, String status, String errorMsg, String param) {
        try {
            LoginUser user = AuthContext.get();
            jdbc.update("insert into sys_oper_log(title,business_type,method,request_method,oper_name,oper_url,oper_ip," +
                            "oper_param,status,error_msg,cost_time,oper_time) values(?,?,?,?,?,?,?,?,?,?,0,now())",
                    truncate(title, 100), "file_check", "FileCheckService", "POST",
                    user == null ? "系统" : user.getNickName(), "/api/system/file-check",
                    "127.0.0.1", truncate(param, 2000), status, truncate(errorMsg, 2000));
        } catch (Exception e) {
            log.debug("文件巡检审计日志写入失败: {}", e.getMessage());
        }
    }

    private String fileType(String path) {
        String lower = path.toLowerCase();
        int dot = lower.lastIndexOf('.');
        String ext = dot >= 0 ? lower.substring(dot + 1) : "";
        if (List.of("jpg", "jpeg", "png", "webp", "gif").contains(ext)) return "image";
        if (List.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt").contains(ext)) return "document";
        return "other";
    }

    private String itemKey(Missing m) {
        return m.ref().table() + "|" + m.recordId() + "|" + m.ref().column();
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

    private Long longOrNull(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() > max ? value.substring(0, max) : value;
    }

    public record FileRef(String table, String id, String column) {
    }

    private record Context(Long companyId, Long hospitalId) {
    }

    private record Missing(FileRef ref, Long recordId, String path, Context context,
                           String fileType, String severity, int slaHours) {
    }

    private record RefResult(int checked) {
    }

    private record ScanResult(int checked) {
    }

    private record MissingItems(List<Missing> missing, Map<String, Long> ids) {
    }
}


