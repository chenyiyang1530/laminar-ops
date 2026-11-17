package com.laminar.ops.system.service;

import com.laminar.ops.common.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FileCheckRecipientService {

    private final JdbcTemplate jdbc;

    public List<Map<String, Object>> list() {
        return jdbc.queryForList(
                "select r.recipient_id as recipientId, r.company_id as companyId, r.hospital_id as hospitalId, " +
                        "r.email, r.enabled, r.create_time as createTime, c.company_name as companyName, h.hospital_name as hospitalName " +
                        "from sys_file_notify_recipient r left join biz_company c on c.company_id=r.company_id " +
                        "left join biz_hospital h on h.hospital_id=r.hospital_id order by r.recipient_id desc");
    }

    public Map<String, Object> save(Map<String, Object> body) {
        String email = body.get("email") == null ? "" : String.valueOf(body.get("email")).trim();
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw BizException.badRequest("邮箱格式不正确");
        }
        Long companyId = longOrNull(body.get("companyId"));
        Long hospitalId = longOrNull(body.get("hospitalId"));
        if (companyId == null && hospitalId == null) {
            throw BizException.badRequest("公司或医院至少选择一个");
        }
        String enabled = body.get("enabled") == null ? "1" : String.valueOf(body.get("enabled"));
        Long id = longOrNull(body.get("recipientId"));
        if (id == null) {
            jdbc.update("insert into sys_file_notify_recipient(company_id,hospital_id,email,enabled,create_time) values(?,?,?,?,now())",
                    companyId, hospitalId, email, enabled);
            id = jdbc.queryForObject("select last_insert_id()", Long.class);
        } else {
            jdbc.update("update sys_file_notify_recipient set company_id=?, hospital_id=?, email=?, enabled=? where recipient_id=?",
                    companyId, hospitalId, email, enabled, id);
        }
        return Map.of("recipientId", id);
    }

    public void remove(Long id) {
        jdbc.update("delete from sys_file_notify_recipient where recipient_id=?", id);
    }

    public List<String> emails(Long companyId, Long hospitalId) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" where enabled='1' and (");
        if (companyId != null) { where.append("company_id=?"); args.add(companyId); }
        if (hospitalId != null) {
            if (!args.isEmpty()) where.append(" or ");
            where.append("hospital_id=?"); args.add(hospitalId);
        }
        where.append(")");
        if (args.isEmpty()) return List.of();
        List<String> emails = jdbc.queryForList("select email from sys_file_notify_recipient" + where, String.class, args.toArray());
        return emails.stream().filter(e -> e != null && !e.isBlank()).distinct().toList();
    }

    private Long longOrNull(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return null;
        return ((Number) value).longValue();
    }
}
