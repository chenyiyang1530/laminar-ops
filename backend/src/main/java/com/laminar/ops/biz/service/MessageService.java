package com.laminar.ops.biz.service;

import com.laminar.ops.security.AuthContext;
import com.laminar.ops.security.DataScopeService;
import com.laminar.ops.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final JdbcTemplate jdbc;
    private final DataScopeService dataScope;

    private String scope(LoginUser user, List<Object> args) {
        if (user.isSuperAdmin()) return " 1=1 ";
        StringBuilder sb = new StringBuilder(" (m.receiver_id = ? or (m.receiver_id = 0 and exists (" +
                "select 1 from sys_user_role ur where ur.user_id = ? and find_in_set(ur.role_id, m.receiver_role)))) ");
        args.add(user.getUserId());
        args.add(user.getUserId());
        List<Long> hospitals = dataScope.visibleHospitalIds(user);
        if (hospitals != null) {
            if (hospitals.isEmpty()) {
                sb.append(" and 1=0 ");
            } else {
                sb.append(" and (m.hospital_id is null or m.hospital_id in (")
                        .append(hospitals.stream().map(String::valueOf).collect(Collectors.joining(",")))
                        .append(")) ");
            }
        }
        Long companyId = dataScope.companyId(user);
        if (companyId != null) {
            sb.append(" and m.company_id = ? ");
            args.add(companyId);
        }
        return " " + sb + " ";
    }

    public Map<String, Object> page(int pageNum, int pageSize, String msgType, String isRead) {
        LoginUser user = AuthContext.require();
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" where " + scope(user, args));
        if (msgType != null && !msgType.isBlank()) {
            where.append(" and m.msg_type = ? ");
            args.add(msgType);
        }
        if (isRead != null && !isRead.isBlank()) {
            where.append(" and m.is_read = ? ");
            args.add(isRead);
        }
        Long total = jdbc.queryForObject("select count(*) from biz_message m" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add(Math.max(0, (pageNum - 1) * pageSize));
        List<Map<String, Object>> records = jdbc.queryForList(
                "select m.msg_id as `msgId`, m.msg_type as `msgType`, m.title, m.content, m.level, m.biz_type as `bizType`, " +
                        "m.biz_id as `bizId`, m.is_read as `isRead`, m.create_time as `createTime` " +
                        "from biz_message m" + where + " order by m.create_time desc, m.msg_id desc limit ? offset ?",
                pageArgs.toArray());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total == null ? 0 : total);
        return result;
    }

    public int unreadCount() {
        LoginUser user = AuthContext.require();
        List<Object> args = new ArrayList<>();
        String scope = scope(user, args);
        Integer n = jdbc.queryForObject("select count(*) from biz_message m where m.is_read = '0' and " + scope,
                Integer.class, args.toArray());
        return n == null ? 0 : n;
    }

    public void read(Long id) {
        LoginUser user = AuthContext.require();
        List<Object> args = new ArrayList<>();
        args.add(id);
        String scope = scope(user, args);
        jdbc.update("update biz_message m set m.is_read = '1', m.read_time = now() where m.msg_id = ? and " + scope,
                args.toArray());
    }

    public void readAll() {
        LoginUser user = AuthContext.require();
        List<Object> args = new ArrayList<>();
        String scope = scope(user, args);
        jdbc.update("update biz_message m set m.is_read = '1', m.read_time = now() where m.is_read = '0' and " + scope,
                args.toArray());
    }
}
