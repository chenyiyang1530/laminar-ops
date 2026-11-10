package com.laminar.ops.system.service;

import com.laminar.ops.common.BizException;
import com.laminar.ops.security.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReleaseService {

    private final JdbcTemplate jdbc;

    public List<Map<String, Object>> list() {
        String where = AuthContext.require().isSuperAdmin() ? "" : " where status='0' ";
        return jdbc.queryForList(
                "select id, version, title, release_date as `releaseDate`, summary, features, improvements, fixes, " +
                        "status, create_time as `createTime` from sys_release_note " + where +
                        " order by release_date desc, id desc");
    }

    public Map<String, Object> latest() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select id, version, title, release_date as `releaseDate`, summary, features, improvements, fixes " +
                        "from sys_release_note where status='0' order by release_date desc, id desc limit 1");
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        if (rows.isEmpty()) {
            result.put("latest", null);
            result.put("unread", false);
            return result;
        }
        Map<String, Object> latest = rows.get(0);
        Long userId = AuthContext.require().getUserId();
        List<Long> seen = jdbc.queryForList(
                "select last_seen_release_id from sys_user where user_id=?", Long.class, userId);
        long lastSeen = seen.isEmpty() || seen.get(0) == null ? 0 : seen.get(0);
        long latestId = ((Number) latest.get("id")).longValue();
        result.put("latest", latest);
        result.put("unread", latestId > lastSeen);
        return result;
    }

    public void markSeen(Long releaseId) {
        Long id = releaseId;
        if (id == null) {
            List<Long> rows = jdbc.queryForList(
                    "select id from sys_release_note where status='0' order by release_date desc, id desc limit 1",
                    Long.class);
            if (rows.isEmpty()) return;
            id = rows.get(0);
        }
        jdbc.update("update sys_user set last_seen_release_id=? where user_id=? and " +
                        "(last_seen_release_id is null or last_seen_release_id < ?)",
                id, AuthContext.require().getUserId(), id);
    }

    public void save(Map<String, Object> body) {
        String version = str(body.get("version"));
        String title = str(body.get("title"));
        String releaseDate = str(body.get("releaseDate"));
        if (version.isBlank()) throw BizException.badRequest("请填写版本号");
        if (title.isBlank()) throw BizException.badRequest("请填写版本标题");
        if (releaseDate.isBlank()) throw BizException.badRequest("请选择发布日期");
        Integer exists = jdbc.queryForObject(
                "select count(*) from sys_release_note where version=?", Integer.class, version);
        if (exists != null && exists > 0) throw BizException.badRequest("版本号已存在");
        jdbc.update("insert into sys_release_note(version,title,release_date,summary,features,improvements,fixes," +
                        "status,create_by,create_time) values(?,?,?,?,?,?,?,?,?,now())",
                version, title, releaseDate, str(body.get("summary")), str(body.get("features")),
                str(body.get("improvements")), str(body.get("fixes")),
                str(body.getOrDefault("status", "0")), AuthContext.require().getUsername());
    }

    public void update(Map<String, Object> body) {
        Long id = longOf(body.get("id"));
        if (id == null) throw BizException.badRequest("缺少版本记录");
        String version = str(body.get("version"));
        String title = str(body.get("title"));
        String releaseDate = str(body.get("releaseDate"));
        if (version.isBlank() || title.isBlank() || releaseDate.isBlank()) {
            throw BizException.badRequest("版本号、标题和发布日期不能为空");
        }
        Integer exists = jdbc.queryForObject(
                "select count(*) from sys_release_note where version=? and id<>?", Integer.class, version, id);
        if (exists != null && exists > 0) throw BizException.badRequest("版本号已存在");
        jdbc.update("update sys_release_note set version=?,title=?,release_date=?,summary=?,features=?," +
                        "improvements=?,fixes=?,status=?,update_by=?,update_time=now() where id=?",
                version, title, releaseDate, str(body.get("summary")), str(body.get("features")),
                str(body.get("improvements")), str(body.get("fixes")),
                str(body.getOrDefault("status", "0")), AuthContext.require().getUsername(), id);
    }

    public void delete(Long id) {
        jdbc.update("delete from sys_release_note where id=?", id);
    }

    private String str(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private Long longOf(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return null;
        return Long.valueOf(String.valueOf(value));
    }
}
