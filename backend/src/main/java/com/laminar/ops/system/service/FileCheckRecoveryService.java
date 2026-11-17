package com.laminar.ops.system.service;

import com.laminar.ops.common.BizException;
import com.laminar.ops.security.AuthContext;
import com.laminar.ops.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FileCheckRecoveryService {

    private final JdbcTemplate jdbc;

    @Value("${laminar.upload.path:}")
    private String uploadPath;

    public Map<String, Object> recover(Long itemId, String fileUrl) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select item_id, table_name, record_id, file_column, company_id, hospital_id, file_path " +
                        "from sys_file_check_item where item_id=?", itemId);
        if (rows.isEmpty()) throw BizException.badRequest("巡检项不存在");
        Map<String, Object> item = rows.get(0);
        String table = String.valueOf(item.get("table_name"));
        String column = String.valueOf(item.get("file_column"));
        Long recordId = ((Number) item.get("record_id")).longValue();
        FileCheckModels.FileRef ref = FileCheckModels.REFS.stream()
                .filter(r -> r.table().equals(table) && r.column().equals(column)).findFirst()
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
        audit(itemId, table, recordId, relative);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("itemId", itemId);
        result.put("filePath", relative);
        result.put("status", "1");
        return result;
    }

    private void audit(Long itemId, String table, Long recordId, String relative) {
        try {
            LoginUser user = AuthContext.get();
            jdbc.update("insert into sys_oper_log(title,business_type,method,request_method,oper_name,oper_url,oper_ip," +
                            "oper_param,status,error_msg,cost_time,oper_time) values(?,?,?,?,?,?,?,?,?,?,0,now())",
                    "文件恢复", "file_check", "FileCheckRecoveryService", "POST",
                    user == null ? "系统" : user.getNickName(), "/api/system/file-check",
                    "127.0.0.1", "item=" + itemId + " table=" + table + " record=" + recordId + " file=" + relative,
                    "0", null);
        } catch (Exception ignored) {
        }
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
}
