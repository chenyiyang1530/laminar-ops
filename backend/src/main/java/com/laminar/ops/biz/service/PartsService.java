package com.laminar.ops.biz.service;

import com.laminar.ops.common.BizException;
import com.laminar.ops.security.AuthContext;
import com.laminar.ops.security.DataScopeService;
import com.laminar.ops.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PartsService {

    private final JdbcTemplate jdbc;
    private final DataScopeService dataScope;

    public Map<String, Object> page(int pageNum, int pageSize, String status) {
        LoginUser user = AuthContext.require();
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" where 1=1 ");
        if (status != null && !status.isBlank()) { where.append(" and r.status=? "); args.add(status); }
        Long companyId = dataScope.companyId(user);
        if (companyId != null) { where.append(" and r.company_id=? "); args.add(companyId); }
        Long total = jdbc.queryForObject("select count(*) from biz_part_requisition r" + where, Long.class, args.toArray());
        args.add(pageSize);
        args.add(Math.max(0, (pageNum - 1) * pageSize));
        List<Map<String, Object>> records = jdbc.queryForList(
                "select r.req_id as `reqId`, r.req_no as `reqNo`, r.applicant_name as `applicantName`, r.status, " +
                        "r.total_amount as `totalAmount`, date_format(r.apply_time,'%Y-%m-%d %H:%i') as `applyTime`, " +
                        "r.approve_by as `approveBy`, date_format(r.out_time,'%Y-%m-%d %H:%i') as `outTime`, " +
                        "h.hospital_name as `hospitalName`, w.workorder_no as `workorderNo`, r.remark " +
                        "from biz_part_requisition r left join biz_hospital h on h.hospital_id=r.hospital_id " +
                        "left join biz_workorder w on w.workorder_id=r.workorder_id" +
                        where + " order by r.req_id desc limit ? offset ?", args.toArray());
        return Map.of("records", records, "total", total == null ? 0 : total);
    }

    public Map<String, Object> detail(Long id) {
        Map<String, Object> req = require(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("requisition", req);
        result.put("items", jdbc.queryForList(
                "select item_id as `itemId`, part_id as `partId`, part_name as `partName`, spec, quantity, price, amount " +
                        "from biz_part_requisition_item where req_id=?", id));
        return result;
    }

    @Transactional
    public Long create(Map<String, Object> body) {
        LoginUser user = AuthContext.require();
        Object itemsObj = body.get("items");
        if (!(itemsObj instanceof List<?> items) || items.isEmpty()) {
            throw BizException.badRequest("请添加领用备件");
        }
        String no = "RQ-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        Long companyId = dataScope.companyId(user);
        BigDecimal total = BigDecimal.ZERO;
        List<Object[]> rows = new ArrayList<>();
        for (Object o : items) {
            if (!(o instanceof Map<?, ?> item)) continue;
            Long partId = ((Number) item.get("partId")).longValue();
            int qty = item.get("quantity") == null ? 1 : ((Number) item.get("quantity")).intValue();
            Map<String, Object> part = jdbc.queryForMap(
                    "select part_name, spec, price, company_id from biz_part where part_id=?", partId);
            if (companyId != null && !companyId.equals(((Number) part.get("company_id")).longValue())) {
                throw BizException.forbidden("备件不属于当前公司");
            }
            if (companyId == null && part.get("company_id") != null) {
                companyId = ((Number) part.get("company_id")).longValue();
            }
            BigDecimal price = part.get("price") == null ? BigDecimal.ZERO : (BigDecimal) part.get("price");
            BigDecimal amount = price.multiply(BigDecimal.valueOf(qty));
            total = total.add(amount);
            rows.add(new Object[]{partId, part.get("part_name"), part.get("spec"), qty, price, amount});
        }
        Long finalCompanyId = companyId;
        KeyHolder holder = new GeneratedKeyHolder();
        BigDecimal finalTotal = total;
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "insert into biz_part_requisition(req_no,company_id,workorder_id,hospital_id,applicant_id,applicant_name,status," +
                            "apply_time,total_amount,remark) values(?,?,?,?,?,?,'0',now(),?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, no);
            ps.setObject(2, finalCompanyId);
            ps.setObject(3, body.get("workorderId"));
            ps.setObject(4, body.get("hospitalId"));
            ps.setObject(5, user.getUserId());
            ps.setString(6, user.getNickName());
            ps.setBigDecimal(7, finalTotal);
            ps.setString(8, (String) body.get("remark"));
            return ps;
        }, holder);
        Long reqId = holder.getKey() == null ? 0L : holder.getKey().longValue();
        for (Object[] r : rows) {
            jdbc.update("insert into biz_part_requisition_item(req_id,part_id,part_name,spec,quantity,price,amount) values(?,?,?,?,?,?,?)",
                    reqId, r[0], r[1], r[2], r[3], r[4], r[5]);
        }
        return reqId;
    }

    public void approve(Long id, boolean pass, String remark) {
        Map<String, Object> req = require(id);
        if (!"0".equals(String.valueOf(req.get("status")))) throw BizException.badRequest("只有待审批领用单可以审批");
        jdbc.update("update biz_part_requisition set status=?, approve_by=?, approve_time=now(), remark=coalesce(?,remark) where req_id=?",
                pass ? "1" : "3", AuthContext.nickName(), remark, id);
    }

    @Transactional
    public void outbound(Long id) {
        Map<String, Object> req = require(id);
        if (!"1".equals(String.valueOf(req.get("status")))) throw BizException.badRequest("只有已批准领用单可以出库");
        List<Map<String, Object>> items = jdbc.queryForList(
                "select part_id, part_name, quantity from biz_part_requisition_item where req_id=?", id);
        for (Map<String, Object> item : items) {
            Long partId = ((Number) item.get("part_id")).longValue();
            int qty = ((Number) item.get("quantity")).intValue();
            checkPartCompany(req, partId);
            Integer stock = jdbc.queryForObject("select current_stock from biz_part where part_id=?", Integer.class, partId);
            int before = stock == null ? 0 : stock;
            if (before < qty) throw BizException.badRequest("备件库存不足：" + item.get("part_name"));
            jdbc.update("update biz_part set current_stock=? where part_id=?", before - qty, partId);
            jdbc.update("insert into biz_part_record(part_id,record_type,quantity,before_stock,after_stock,requisition_id," +
                            "operator_id,operator_name,record_time,remark) values(?,'out',?,?,?,?,?,?,now(),?)",
                    partId, qty, before, before - qty, id, AuthContext.userId(), AuthContext.nickName(), "领用出库");
        }
        jdbc.update("update biz_part_requisition set status='2', out_by=?, out_time=now() where req_id=?",
                AuthContext.nickName(), id);
    }

    @Transactional
    public void stock(Long partId, String type, Integer quantity, String remark) {
        if (quantity == null || quantity <= 0) throw BizException.badRequest("数量必须大于 0");
        checkPartCompany(null, partId);
        Integer stock = jdbc.queryForObject("select current_stock from biz_part where part_id=?", Integer.class, partId);
        int before = stock == null ? 0 : stock;
        int delta = "in".equals(type) ? quantity : -quantity;
        if (before + delta < 0) throw BizException.badRequest("库存不足");
        jdbc.update("update biz_part set current_stock=? where part_id=?", before + delta, partId);
        jdbc.update("insert into biz_part_record(part_id,record_type,quantity,before_stock,after_stock,operator_id,operator_name," +
                        "record_time,remark) values(?,?,?,?,?,?,?,now(),?)",
                partId, type, quantity, before, before + delta, AuthContext.userId(), AuthContext.nickName(), remark);
    }

    private Map<String, Object> require(Long id) {
        List<Map<String, Object>> rows = jdbc.queryForList("select * from biz_part_requisition where req_id=?", id);
        if (rows.isEmpty()) throw BizException.badRequest("领用单不存在");
        Map<String, Object> req = rows.get(0);
        Long companyId = dataScope.companyId(AuthContext.require());
        Object reqCompany = req.get("company_id");
        if (companyId != null && (reqCompany == null
                || !companyId.equals(((Number) reqCompany).longValue()))) {
            throw BizException.forbidden("无权访问其他公司的领用单");
        }
        return req;
    }

    private void checkPartCompany(Map<String, Object> req, Long partId) {
        List<Map<String, Object>> parts = jdbc.queryForList(
                "select company_id from biz_part where part_id=?", partId);
        if (parts.isEmpty()) throw BizException.badRequest("备件不存在");
        Long partCompany = parts.get(0).get("company_id") == null ? null
                : ((Number) parts.get(0).get("company_id")).longValue();
        Long companyId = dataScope.companyId(AuthContext.require());
        if (req != null && req.get("company_id") != null) {
            companyId = ((Number) req.get("company_id")).longValue();
        }
        if (companyId != null && (partCompany == null || !companyId.equals(partCompany))) {
            throw BizException.forbidden("备件不属于当前公司");
        }
    }
}
