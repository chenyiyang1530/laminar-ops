package com.laminar.ops.system.service;

import com.laminar.ops.common.BizException;
import com.laminar.ops.common.SystemAlertService;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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

    private static final int BATCH_SIZE = 500;
    private static final int THREADS = 4;
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);

    private final JdbcTemplate jdbc;
    private final SystemAlertService systemAlertService;

    @Value("${laminar.upload.path:}")
    private String uploadPath;

    public Map<String, Object> run(String triggerType) {
        if (!RUNNING.compareAndSet(false, true)) {
            return Map.of("status", "running");
        }
        long runId = createRun(triggerType);
        long start = System.currentTimeMillis();
        try {
            Path root = Path.of(uploadPath).toAbsolutePath().normalize();
            ScanResult scan = scanAll(root);
            List<Missing> missing = scan.missing();
            upsertItems(runId, missing);
            int recovered = markRecovered(runId);

            jdbc.update("update sys_file_check_run set status='success', checked=?, missing=?, recovered=?, end_time=now() where run_id=?",
                    scan.checked(), missing.size(), recovered, runId);
            notifyGroups(runId, missing);
            if (!missing.isEmpty()) {
                String samples = missing.stream().limit(10)
                        .map(m -> m.ref().table() + "#" + m.recordId() + " " + m.ref().column() + "=" + m.path())
                        .collect(Collectors.joining("；"));
                systemAlertService.error("file-existence-missing", "文件存在性巡检告警",
                        "发现 " + missing.size() + " 个文件丢失：" + samples);
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("runId", runId);
            result.put("status", "success");
            result.put("missing", missing.size());
            result.put("recovered", recovered);
            result.put("elapsedMs", System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            log.error("[文件巡检] 执行失败", e);
            jdbc.update("update sys_file_check_run set status='failed', end_time=now(), error_msg=? where run_id=?",
                    e.getMessage() == null ? "unknown" : e.getMessage().substring(0, Math.min(e.getMessage().length(), 500)), runId);
            throw new BizException(500, "文件巡检失败：" + e.getMessage());
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

    public Map<String, Object> pageItems(int pageNum, int pageSize, String status, String keyword) {
        int offset = Math.max(0, (pageNum - 1) * pageSize);
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" where 1=1 ");
        if (status != null && !status.isBlank()) { where.append(" and i.status=?"); args.add(status); }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" and (i.file_path like ? or i.table_name like ?)");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
        }
        Integer total = jdbc.queryForObject("select count(*) from sys_file_check_item i" + where, Integer.class, args.toArray());
        args.add(pageSize);
        args.add(offset);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select i.item_id as itemId, i.run_id as runId, i.table_name as tableName, i.record_id as recordId, " +
                        "i.file_column as fileColumn, i.file_path as filePath, i.company_id as companyId, i.hospital_id as hospitalId, " +
                        "i.status, i.first_seen as firstSeen, i.last_seen as lastSeen, i.resolved_time as resolvedTime, " +
                        "c.company_name as companyName, h.hospital_name as hospitalName " +
                        "from sys_file_check_item i left join biz_company c on c.company_id=i.company_id " +
                        "left join biz_hospital h on h.hospital_id=i.hospital_id" + where +
                        " order by i.status asc, i.last_seen desc limit ? offset ?", args.toArray());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", rows);
        result.put("total", total == null ? 0 : total);
        return result;
    }

    public Map<String, Object> recover(Long itemId, String fileUrl) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select item_id, table_name, record_id, file_column, company_id, hospital_id from sys_file_check_item where item_id=?", itemId);
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
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("itemId", itemId);
        result.put("filePath", relative);
        result.put("status", "1");
        return result;
    }

    private ScanResult scanAll(Path root) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        try {
            List<Future<RefResult>> futures = new ArrayList<>();
            for (FileRef ref : REFS) {
                futures.add(executor.submit(scanRef(root, ref)));
            }
            List<Missing> all = new ArrayList<>();
            int checked = 0;
            for (Future<RefResult> future : futures) {
                RefResult result = future.get();
                all.addAll(result.missing());
                checked += result.checked();
            }
            return new ScanResult(all, checked);
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<RefResult> scanRef(Path root, FileRef ref) {
        return () -> {
            List<Missing> result = new ArrayList<>();
            int checked = 0;
            long lastId = 0;
            while (true) {
                List<Map<String, Object>> rows = jdbc.queryForList(
                        "select `" + ref.id() + "` as id, `" + ref.column() + "` as path from `" + ref.table() + "` " +
                                "where `" + ref.column() + "` is not null and `" + ref.column() + "` <> '' and `" + ref.id() + "` > ? " +
                                "order by `" + ref.id() + "` limit ?", lastId, BATCH_SIZE);
                if (rows.isEmpty()) break;
                for (Map<String, Object> row : rows) {
                    Long recordId = ((Number) row.get("id")).longValue();
                    lastId = recordId;
                    String path = normalize(String.valueOf(row.get("path")));
                    if (path == null) continue;
                    checked++;
                    Path target = root.resolve(path).normalize();
                    if (!target.startsWith(root) || !Files.isRegularFile(target)) {
                        result.add(new Missing(ref, recordId, path, context(ref, recordId)));
                    }
                }
                if (rows.size() < BATCH_SIZE) break;
            }
            return new RefResult(result, checked);
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

    private void upsertItems(long runId, List<Missing> missing) {
        for (Missing m : missing) {
            int updated = jdbc.update("update sys_file_check_item set run_id=?, file_path=?, company_id=?, hospital_id=?, " +
                            "last_seen=now(), status='0', resolved_time=null where table_name=? and record_id=? and file_column=?",
                    runId, m.path(), m.context().companyId(), m.context().hospitalId(),
                    m.ref().table(), m.recordId(), m.ref().column());
            if (updated == 0) {
                jdbc.update("insert into sys_file_check_item(run_id, table_name, record_id, file_column, file_path, company_id, hospital_id, status, first_seen, last_seen) " +
                                "values(?,?,?,?,?,?,?,'0',now(),now())",
                        runId, m.ref().table(), m.recordId(), m.ref().column(), m.path(),
                        m.context().companyId(), m.context().hospitalId());
            }
        }
    }

    private void notifyGroups(long runId, List<Missing> missing) {
        Map<String, List<Missing>> byCompany = missing.stream()
                .filter(m -> m.context().companyId() != null)
                .collect(Collectors.groupingBy(m -> "c" + m.context().companyId()));
        Map<String, List<Missing>> byHospital = missing.stream()
                .filter(m -> m.context().hospitalId() != null)
                .collect(Collectors.groupingBy(m -> "h" + m.context().hospitalId()));
        byCompany.forEach((key, list) -> insertMessage("213", list.get(0).context().companyId(), null, list, runId));
        byHospital.forEach((key, list) -> insertMessage("200", null, list.get(0).context().hospitalId(), list, runId));
    }

    private void insertMessage(String role, Long companyId, Long hospitalId, List<Missing> list, long runId) {
        try {
            String samples = list.stream().limit(5).map(m -> m.ref().table() + "#" + m.recordId())
                    .collect(Collectors.joining("、"));
            String title = "文件巡检发现 " + list.size() + " 个文件丢失";
            String content = "请到系统管理-文件巡检处理。样本：" + samples;
            jdbc.update("insert into biz_message(msg_type,title,content,level,biz_type,biz_id,biz_key,receiver_id,receiver_role," +
                            "hospital_id,company_id,is_read,create_time) values('system',?,?,'1','file_check',?,null,0,?,?,?,'0',now())",
                    title, content, runId, role, hospitalId, companyId);
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

    public record FileRef(String table, String id, String column) {
    }

    private record Context(Long companyId, Long hospitalId) {
    }

    private record Missing(FileRef ref, Long recordId, String path, Context context) {
    }

    private record RefResult(List<Missing> missing, int checked) {
    }

    private record ScanResult(List<Missing> missing, int checked) {
    }
}
