package com.laminar.ops.biz.service;

import com.laminar.ops.common.BizException;
import com.laminar.ops.common.SystemAlertService;
import com.laminar.ops.security.AuthContext;
import com.laminar.ops.security.DataScopeService;
import com.laminar.ops.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PartsService {

    private final JdbcTemplate jdbc;
    private final DataScopeService dataScope;
    private final SystemAlertService systemAlertService;

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
                "select item_id as `itemId`, part_id as `partId`, part_name as `partName`, spec, " +
                        "quantity, reserved_qty as `reservedQty`, outbound_qty as `outboundQty`, " +
                        "returned_qty as `returnedQty`, price, amount from biz_part_requisition_item where req_id=?", id));
        return result;
    }

    @Transactional
    public Long create(Map<String, Object> body) {
        LoginUser user = AuthContext.require();
        Object itemsObj = body.get("items");
        if (!(itemsObj instanceof List<?> items) || items.isEmpty()) {
            throw BizException.badRequest("请添加领用备件");
        }
        Long companyId = dataScope.companyId(user);
        String requestId = str(body.get("requestId"));
        if (!requestId.isBlank()) {
            List<Long> exists = jdbc.queryForList(
                    "select req_id from biz_part_requisition where company_id <=> ? and request_id=?",
                    Long.class, companyId, requestId);
            if (!exists.isEmpty()) return exists.get(0);
        }

        String no = "RQ-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        BigDecimal total = BigDecimal.ZERO;
        List<Object[]> rows = new ArrayList<>();
        for (Object o : items) {
            if (!(o instanceof Map<?, ?> item)) continue;
            Long partId = longOf(item.get("partId"));
            int qty = item.get("quantity") == null ? 1 : ((Number) item.get("quantity")).intValue();
            if (partId == null || qty <= 0) throw BizException.badRequest("备件和数量不能为空，数量必须大于 0");
            Map<String, Object> part = requirePart(partId);
            Long partCompany = part.get("company_id") == null ? null : ((Number) part.get("company_id")).longValue();
            if (companyId != null && (partCompany == null || !companyId.equals(partCompany))) {
                throw BizException.forbidden("备件不属于当前公司");
            }
            if (companyId == null && partCompany != null) companyId = partCompany;
            String warehouse = warehouseOf(part);
            BizStock stock = lockStock(partId, warehouse);
            if (stock.onHand - stock.reserved < qty) {
                throw BizException.badRequest("库存不足：" + part.get("part_name") + "，可用 " + (stock.onHand - stock.reserved));
            }
            BigDecimal price = part.get("price") == null ? BigDecimal.ZERO : (BigDecimal) part.get("price");
            BigDecimal amount = price.multiply(BigDecimal.valueOf(qty));
            total = total.add(amount);
            rows.add(new Object[]{partId, part.get("part_name"), part.get("spec"), qty, price, amount, warehouse});
        }
        Long finalCompanyId = companyId;
        KeyHolder holder = new GeneratedKeyHolder();
        BigDecimal finalTotal = total;
        try {
            jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement(
                        "insert into biz_part_requisition(req_no,company_id,request_id,workorder_id,hospital_id,applicant_id,applicant_name,status," +
                                "apply_time,total_amount,remark) values(?,?,?,?,?,?,?,'0',now(),?,?)",
                        Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, no);
                ps.setObject(2, finalCompanyId);
                ps.setString(3, requestId.isBlank() ? null : requestId);
                ps.setObject(4, body.get("workorderId"));
                ps.setObject(5, body.get("hospitalId"));
                ps.setObject(6, user.getUserId());
                ps.setString(7, user.getNickName());
                ps.setBigDecimal(8, finalTotal);
                ps.setString(9, str(body.get("remark")));
                return ps;
            }, holder);
        } catch (DuplicateKeyException e) {
            List<Long> exists = jdbc.queryForList(
                    "select req_id from biz_part_requisition where company_id <=> ? and request_id=?",
                    Long.class, finalCompanyId, requestId);
            if (!exists.isEmpty()) return exists.get(0);
            throw e;
        }
        Long reqId = holder.getKey() == null ? 0L : holder.getKey().longValue();
        for (Object[] r : rows) {
            jdbc.update("insert into biz_part_requisition_item(req_id,part_id,part_name,spec,quantity,price,amount) values(?,?,?,?,?,?,?)",
                    reqId, r[0], r[1], r[2], r[3], r[4], r[5]);
        }
        return reqId;
    }

    @Transactional
    public void approve(Long id, boolean pass, String remark) {
        Map<String, Object> req = require(id);
        if (!"0".equals(String.valueOf(req.get("status")))) {
            throw BizException.badRequest("只有待审批领用单可以审批");
        }
        int changed = jdbc.update("update biz_part_requisition set status=?, approve_by=?, approve_time=now(), " +
                        "remark=coalesce(?,remark) where req_id=? and status='0'",
                pass ? "1" : "3", AuthContext.nickName(), remark, id);
        if (changed != 1) throw BizException.badRequest("领用单状态已变化，请刷新后重试");
        if (!pass) return;

        Long companyId = req.get("company_id") == null ? null : ((Number) req.get("company_id")).longValue();
        for (Map<String, Object> item : items(id)) {
            Long partId = ((Number) item.get("part_id")).longValue();
            int qty = ((Number) item.get("quantity")).intValue();
            Map<String, Object> part = requirePart(partId);
            String warehouse = warehouseOf(part);
            BizStock before = lockStock(partId, warehouse);
            if (before.onHand - before.reserved < qty) {
                throw BizException.badRequest("库存不足：" + item.get("part_name"));
            }
            jdbc.update("update biz_part_stock set reserved_qty=reserved_qty+?, version=version+1, update_time=now() " +
                            "where part_id=? and warehouse=?",
                    qty, partId, warehouse);
            int itemChanged = jdbc.update("update biz_part_requisition_item set reserved_qty=reserved_qty+? where item_id=?",
                    qty, item.get("item_id"));
            if (itemChanged != 1) throw BizException.badRequest("领用明细状态异常");
            writeTxn(companyId, partId, warehouse, "RESERVE", qty,
                    before.onHand, before.onHand, before.reserved, before.reserved + qty,
                    "REQUISITION", id, "req-reserve-" + id + "-" + partId, "审批预占");
        }
    }

    @Transactional
    public void outbound(Long id) {
        Map<String, Object> req = require(id);
        if (!"1".equals(String.valueOf(req.get("status")))) {
            throw BizException.badRequest("只有已批准领用单可以出库");
        }
        int changed = jdbc.update("update biz_part_requisition set status='2', out_by=?, out_time=now() " +
                "where req_id=? and status='1'", AuthContext.nickName(), id);
        if (changed != 1) throw BizException.badRequest("领用单状态已变化，请刷新后重试");

        Long companyId = req.get("company_id") == null ? null : ((Number) req.get("company_id")).longValue();
        for (Map<String, Object> item : items(id)) {
            Long partId = ((Number) item.get("part_id")).longValue();
            int qty = ((Number) item.get("reserved_qty")).intValue();
            if (qty <= 0) continue;
            Map<String, Object> part = requirePart(partId);
            String warehouse = warehouseOf(part);
            BizStock before = lockStock(partId, warehouse);
            if (before.reserved < qty || before.onHand < qty) {
                throw BizException.badRequest("库存预占不足：" + item.get("part_name"));
            }
            jdbc.update("update biz_part_stock set on_hand_qty=on_hand_qty-?, reserved_qty=reserved_qty-?, " +
                            "version=version+1, update_time=now() where part_id=? and warehouse=?",
                    qty, qty, partId, warehouse);
            jdbc.update("update biz_part_requisition_item set reserved_qty=reserved_qty-?, outbound_qty=outbound_qty+? " +
                    "where item_id=?", qty, qty, item.get("item_id"));
            syncCurrentStock(partId, warehouse);
            writeTxn(companyId, partId, warehouse, "OUTBOUND", -qty,
                    before.onHand, before.onHand - qty, before.reserved, before.reserved - qty,
                    "REQUISITION", id, "req-out-" + id + "-" + partId, "领用出库");
        }
    }

    /**
     * 手工库存变动只允许明确的入库/出库；库存余额和流水必须在同一事务内完成。
     */
    @Transactional
    public void stock(Long partId, String type, Integer quantity, String remark) {
        if (quantity == null || quantity <= 0) throw BizException.badRequest("数量必须大于 0");
        if (!"in".equals(type) && !"out".equals(type)) throw BizException.badRequest("库存类型只能是 in 或 out");
        Map<String, Object> part = requirePart(partId);
        checkPartCompany(null, partId);
        String warehouse = warehouseOf(part);
        BizStock before = lockStock(partId, warehouse);
        int delta = "in".equals(type) ? quantity : -quantity;
        if (before.onHand + delta < 0) throw BizException.badRequest("库存不足");
        jdbc.update("update biz_part_stock set on_hand_qty=on_hand_qty+?, version=version+1, update_time=now() " +
                "where part_id=? and warehouse=?", delta, partId, warehouse);
        syncCurrentStock(partId, warehouse);
        writeTxn(part.get("company_id") == null ? null : ((Number) part.get("company_id")).longValue(),
                partId, warehouse, "in".equals(type) ? "MANUAL_IN" : "MANUAL_OUT", delta,
                before.onHand, before.onHand + delta, before.reserved, before.reserved,
                "MANUAL", null, "manual-" + UUID.randomUUID(), remark);
    }

    public Map<String, Object> adjustmentPage(int pageNum, int pageSize, String status) {
        LoginUser user = AuthContext.require();
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" where 1=1 ");
        if (status != null && !status.isBlank()) { where.append(" and a.status=? "); args.add(status); }
        Long companyId = dataScope.companyId(user);
        if (companyId != null) { where.append(" and a.company_id=? "); args.add(companyId); }
        Long total = jdbc.queryForObject("select count(*) from biz_part_adjustment a" + where, Long.class, args.toArray());
        args.add(pageSize);
        args.add(Math.max(0, (pageNum - 1) * pageSize));
        List<Map<String, Object>> records = jdbc.queryForList(
                "select a.adjustment_id as `adjustmentId`, a.adjustment_no as `adjustmentNo`, a.part_id as `partId`, " +
                        "p.part_name as `partName`, a.warehouse, a.adjust_type as `adjustType`, a.quantity, " +
                        "a.reason_type as `reasonType`, a.reason, a.status, a.applicant_name as `applicantName`, " +
                        "a.approve_by as `approveBy`, date_format(a.approve_time,'%Y-%m-%d %H:%i') as `approveTime`, " +
                        "a.review_by as `reviewBy`, date_format(a.review_time,'%Y-%m-%d %H:%i') as `reviewTime`, " +
                        "a.before_on_hand as `beforeOnHand`, a.after_on_hand as `afterOnHand`, " +
                        "date_format(a.create_time,'%Y-%m-%d %H:%i') as `createTime` " +
                        "from biz_part_adjustment a left join biz_part p on p.part_id=a.part_id" +
                        where + " order by a.adjustment_id desc limit ? offset ?", args.toArray());
        return Map.of("records", records, "total", total == null ? 0 : total);
    }

    @Transactional
    public Long createAdjustment(Map<String, Object> body) {
        LoginUser user = AuthContext.require();
        Long partId = longOf(body.get("partId"));
        String type = str(body.get("type")).trim();
        int quantity = body.get("quantity") == null ? 0 : ((Number) body.get("quantity")).intValue();
        String reasonType = str(body.get("reasonType")).trim();
        String reason = str(body.get("reason")).trim();
        if (partId == null || quantity <= 0) throw BizException.badRequest("备件和数量不能为空");
        if (!"in".equals(type) && !"out".equals(type)) throw BizException.badRequest("库存类型只能是 in 或 out");
        if (reasonType.isBlank() || reason.isBlank()) throw BizException.badRequest("请填写调整原因类型和原因");
        Map<String, Object> part = requirePart(partId);
        checkPartCompany(null, partId);
        String no = "ADJ-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        jdbc.update("insert into biz_part_adjustment(adjustment_no,company_id,part_id,warehouse,adjust_type,quantity," +
                        "reason_type,reason,status,applicant_id,applicant_name,create_time) values(?,?,?,?,?,?,?,?,'0',?,?,now())",
                no, part.get("company_id"), partId, warehouseOf(part), type, quantity, reasonType, reason,
                user.getUserId(), user.getNickName());
        return jdbc.queryForObject("select last_insert_id()", Long.class);
    }

    @Transactional
    public void approveAdjustment(Long id, boolean pass, String remark) {
        LoginUser user = AuthContext.require();
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select * from biz_part_adjustment where adjustment_id=? for update", id);
        if (rows.isEmpty()) throw BizException.badRequest("库存调整单不存在");
        Map<String, Object> adj = rows.get(0);
        Long companyId = adj.get("company_id") == null ? null : ((Number) adj.get("company_id")).longValue();
        if (dataScope.companyId(user) != null && !dataScope.companyId(user).equals(companyId)) {
            throw BizException.forbidden("无权审批其他公司的库存调整");
        }
        if (!"0".equals(String.valueOf(adj.get("status")))) throw BizException.badRequest("调整单不在待审批状态");
        if (String.valueOf(adj.get("applicant_id")).equals(String.valueOf(user.getUserId()))) {
            throw BizException.forbidden("申请人不能审批自己的调整单");
        }
        int changed = jdbc.update("update biz_part_adjustment set status=?, approve_by=?, approve_time=now(), " +
                        "reason=concat(reason, coalesce(?, '')) where adjustment_id=? and status='0'",
                pass ? "3" : "2", user.getNickName(), remark == null || remark.isBlank() ? "" : "；初审意见：" + remark, id);
        if (changed != 1) throw BizException.badRequest("调整单状态已变化");
    }

    /** 第二人复核：只有初审通过（status=3）的调整单才能执行库存变更。 */
    @Transactional
    public void reviewAdjustment(Long id, boolean pass, String remark) {
        LoginUser user = AuthContext.require();
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select * from biz_part_adjustment where adjustment_id=? for update", id);
        if (rows.isEmpty()) throw BizException.badRequest("库存调整单不存在");
        Map<String, Object> adj = rows.get(0);
        Long companyId = adj.get("company_id") == null ? null : ((Number) adj.get("company_id")).longValue();
        if (dataScope.companyId(user) != null && !dataScope.companyId(user).equals(companyId)) {
            throw BizException.forbidden("无权复核其他公司的库存调整");
        }
        if (!"3".equals(String.valueOf(adj.get("status")))) throw BizException.badRequest("调整单不在待复核状态");
        if (String.valueOf(adj.get("approve_by")).equals(user.getNickName())
                || String.valueOf(adj.get("applicant_id")).equals(String.valueOf(user.getUserId()))) {
            throw BizException.forbidden("复核人不能是申请人和初审人");
        }
        int changed = jdbc.update("update biz_part_adjustment set status=?, review_by=?, review_time=now(), " +
                        "reason=concat(reason, coalesce(?, '')) where adjustment_id=? and status='3'",
                pass ? "1" : "2", user.getNickName(), remark == null || remark.isBlank() ? "" : "；复核意见：" + remark, id);
        if (changed != 1) throw BizException.badRequest("调整单状态已变化");
        if (!pass) return;

        Long partId = ((Number) adj.get("part_id")).longValue();
        String warehouse = str(adj.get("warehouse"));
        String type = str(adj.get("adjust_type"));
        int qty = ((Number) adj.get("quantity")).intValue();
        BizStock before = lockStock(partId, warehouse);
        int delta = "in".equals(type) ? qty : -qty;
        if (before.onHand + delta < 0) throw BizException.badRequest("库存不足，无法出库");
        jdbc.update("update biz_part_stock set on_hand_qty=on_hand_qty+?, version=version+1, update_time=now() " +
                "where part_id=? and warehouse=?", delta, partId, warehouse);
        syncCurrentStock(partId, warehouse);
        writeTxn(companyId, partId, warehouse, "in".equals(type) ? "ADJUST_IN" : "ADJUST_OUT", delta,
                before.onHand, before.onHand + delta, before.reserved, before.reserved,
                "ADJUSTMENT", id, "adjust-" + id, "库存调整单 " + adj.get("adjustment_no") + "（复核通过）");
        jdbc.update("update biz_part_adjustment set before_on_hand=?, after_on_hand=? where adjustment_id=?",
                before.onHand, before.onHand + delta, id);
    }

    @Transactional
    public void returnItem(Long reqId, Long itemId, Integer quantity, String reason) {
        if (quantity == null || quantity <= 0) throw BizException.badRequest("退料数量必须大于 0");
        if (reason == null || reason.isBlank()) throw BizException.badRequest("请填写退料原因");
        Map<String, Object> req = require(reqId);
        if (!"2".equals(String.valueOf(req.get("status")))) throw BizException.badRequest("只有已出库领用单可以退料");
        List<Map<String, Object>> items = jdbc.queryForList(
                "select item_id, part_id, part_name, outbound_qty, returned_qty from biz_part_requisition_item " +
                        "where item_id=? and req_id=? for update", itemId, reqId);
        if (items.isEmpty()) throw BizException.badRequest("领用明细不存在");
        Map<String, Object> item = items.get(0);
        int outbound = ((Number) item.get("outbound_qty")).intValue();
        int returned = ((Number) item.get("returned_qty")).intValue();
        if (quantity > outbound - returned) throw BizException.badRequest("退料数量不能超过未退数量");
        Long partId = ((Number) item.get("part_id")).longValue();
        Map<String, Object> part = requirePart(partId);
        checkPartCompany(req, partId);
        String warehouse = warehouseOf(part);
        BizStock before = lockStock(partId, warehouse);
        jdbc.update("update biz_part_stock set on_hand_qty=on_hand_qty+?, version=version+1, update_time=now() " +
                "where part_id=? and warehouse=?", quantity, partId, warehouse);
        jdbc.update("update biz_part_requisition_item set returned_qty=returned_qty+? where item_id=?",
                quantity, itemId);
        syncCurrentStock(partId, warehouse);
        String no = "RT-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        jdbc.update("insert into biz_part_return(return_no,company_id,req_id,item_id,part_id,quantity,reason," +
                        "operator_id,operator_name,create_time) values(?,?,?,?,?,?,?,?,?,now())",
                no, req.get("company_id"), reqId, itemId, partId, quantity, reason,
                AuthContext.userId(), AuthContext.nickName());
        writeTxn(req.get("company_id") == null ? null : ((Number) req.get("company_id")).longValue(),
                partId, warehouse, "RETURN_IN", quantity, before.onHand, before.onHand + quantity,
                before.reserved, before.reserved, "PART_RETURN", reqId, "return-" + no, "领用退料");
        Integer remaining = jdbc.queryForObject(
                "select count(*) from biz_part_requisition_item where req_id=? and outbound_qty > returned_qty",
                Integer.class, reqId);
        if (remaining != null && remaining == 0) {
            jdbc.update("update biz_part_requisition set status='4' where req_id=? and status='2'", reqId);
        }
    }

    public int reconcile() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select p.part_id, p.company_id, p.part_name, p.current_stock, coalesce(sum(s.on_hand_qty),0) stock_sum " +
                        "from biz_part p left join biz_part_stock s on s.part_id=p.part_id " +
                        "group by p.part_id, p.company_id, p.part_name, p.current_stock " +
                        "having p.current_stock <> coalesce(sum(s.on_hand_qty),0)");
        for (Map<String, Object> row : rows) {
            int book = ((Number) row.get("current_stock")).intValue();
            int sum = ((Number) row.get("stock_sum")).intValue();
            jdbc.update("insert into biz_part_reconcile_log(company_id,part_id,part_name,book_stock,stock_sum,difference,checked_at) " +
                            "values(?,?,?,?,?,?,now())",
                    row.get("company_id"), row.get("part_id"), row.get("part_name"), book, sum, book - sum);
            systemAlertService.error("part-stock-mismatch-" + row.get("part_id"), "备件库存对账异常",
                    "备件 " + row.get("part_name") + " 账面 " + book + "，库存汇总 " + sum + "，差异 " + (book - sum));
        }
        return rows.size();
    }

    public Map<String, Object> reconcilePage(int pageNum, int pageSize, String resolved) {
        LoginUser user = AuthContext.require();
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" where 1=1 ");
        if ("0".equals(resolved) || "1".equals(resolved)) {
            where.append(" and r.resolved=? ");
            args.add(resolved);
        }
        Long companyId = dataScope.companyId(user);
        if (companyId != null) { where.append(" and r.company_id=? "); args.add(companyId); }
        Long total = jdbc.queryForObject("select count(*) from biz_part_reconcile_log r" + where, Long.class, args.toArray());
        args.add(pageSize);
        args.add(Math.max(0, (pageNum - 1) * pageSize));
        List<Map<String, Object>> records = jdbc.queryForList(
                "select r.id, r.part_id as `partId`, r.part_name as `partName`, r.warehouse, r.book_stock as `bookStock`, " +
                        "r.stock_sum as `stockSum`, r.difference, r.resolved, r.resolved_by as `resolvedBy`, " +
                        "r.resolve_note as `resolveNote`, date_format(r.checked_at,'%Y-%m-%d %H:%i:%s') as `checkedAt` " +
                        "from biz_part_reconcile_log r" + where + " order by r.id desc limit ? offset ?", args.toArray());
        return Map.of("records", records, "total", total == null ? 0 : total);
    }

    @Transactional
    public void resolveReconcile(Long id, String note) {
        if (note == null || note.isBlank()) throw BizException.badRequest("请填写核销说明");
        LoginUser user = AuthContext.require();
        int changed = jdbc.update("update biz_part_reconcile_log set resolved='1', resolved_by=?, resolve_note=?, " +
                        "resolved_at=now() where id=? and resolved='0'",
                user.getNickName(), note, id);
        if (changed != 1) throw BizException.badRequest("对账异常不存在或已核销");
    }

    public Map<String, Object> returnPage(int pageNum, int pageSize) {
        LoginUser user = AuthContext.require();
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" where 1=1 ");
        Long companyId = dataScope.companyId(user);
        if (companyId != null) { where.append(" and r.company_id=? "); args.add(companyId); }
        Long total = jdbc.queryForObject("select count(*) from biz_part_return r" + where, Long.class, args.toArray());
        args.add(pageSize);
        args.add(Math.max(0, (pageNum - 1) * pageSize));
        List<Map<String, Object>> records = jdbc.queryForList(
                "select r.return_id as `returnId`, r.return_no as `returnNo`, r.req_id as `reqId`, " +
                        "q.req_no as `reqNo`, r.part_id as `partId`, p.part_name as `partName`, r.quantity, " +
                        "r.reason, r.operator_name as `operatorName`, date_format(r.create_time,'%Y-%m-%d %H:%i:%s') as `createTime` " +
                        "from biz_part_return r left join biz_part_requisition q on q.req_id=r.req_id " +
                        "left join biz_part p on p.part_id=r.part_id" + where +
                        " order by r.return_id desc limit ? offset ?", args.toArray());
        return Map.of("records", records, "total", total == null ? 0 : total);
    }

    public List<Map<String, Object>> lowStock() {
        LoginUser user = AuthContext.require();
        Long companyId = dataScope.companyId(user);
        String sql = "select part_id as `partId`, part_name as `partName`, part_code as `partCode`, " +
                "safety_stock as `safetyStock`, current_stock as `currentStock`, " +
                "(safety_stock - current_stock) as `suggestQty` from biz_part " +
                "where status='0' and current_stock <= safety_stock ";
        if (companyId != null) {
            return jdbc.queryForList(sql + "and company_id=? order by (safety_stock-current_stock) desc", companyId);
        }
        return jdbc.queryForList(sql + "order by (safety_stock-current_stock) desc");
    }

    public List<Map<String, Object>> transactions(Long partId, int limit) {
        requirePart(partId);
        checkPartCompany(null, partId);
        int size = Math.min(Math.max(limit, 1), 100);
        return jdbc.queryForList(
                "select txn_id as `txnId`, txn_no as `txnNo`, part_id as `partId`, warehouse, txn_type as `txnType`, " +
                        "quantity, before_on_hand as `beforeOnHand`, after_on_hand as `afterOnHand`, " +
                        "before_reserved as `beforeReserved`, after_reserved as `afterReserved`, " +
                        "ref_type as `refType`, ref_id as `refId`, operator_name as `operatorName`, " +
                        "date_format(txn_time,'%Y-%m-%d %H:%i:%s') as `txnTime`, remark " +
                        "from biz_part_txn where part_id=? order by txn_id desc limit ?", partId, size);
    }

    private List<Map<String, Object>> items(Long reqId) {
        return jdbc.queryForList("select item_id, part_id, part_name, quantity, reserved_qty, outbound_qty " +
                "from biz_part_requisition_item where req_id=? order by item_id", reqId);
    }

    private Map<String, Object> requirePart(Long partId) {
        List<Map<String, Object>> parts = jdbc.queryForList("select * from biz_part where part_id=?", partId);
        if (parts.isEmpty()) throw BizException.badRequest("备件不存在");
        return parts.get(0);
    }

    private BizStock lockStock(Long partId, String warehouse) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select stock_id, on_hand_qty, reserved_qty from biz_part_stock where part_id=? and warehouse=? for update",
                partId, warehouse);
        if (rows.isEmpty()) {
            jdbc.update("insert into biz_part_stock(company_id,part_id,warehouse,on_hand_qty,reserved_qty) " +
                            "select company_id,part_id,?,coalesce(current_stock,0),0 from biz_part where part_id=?",
                    warehouse, partId);
            rows = jdbc.queryForList("select stock_id, on_hand_qty, reserved_qty from biz_part_stock " +
                    "where part_id=? and warehouse=? for update", partId, warehouse);
        }
        if (rows.isEmpty()) throw BizException.badRequest("库存记录不存在");
        Map<String, Object> row = rows.get(0);
        return new BizStock(((Number) row.get("stock_id")).longValue(),
                ((Number) row.get("on_hand_qty")).intValue(), ((Number) row.get("reserved_qty")).intValue());
    }

    private void syncCurrentStock(Long partId, String warehouse) {
        Integer onHand = jdbc.queryForObject(
                "select on_hand_qty from biz_part_stock where part_id=? and warehouse=?", Integer.class, partId, warehouse);
        jdbc.update("update biz_part set current_stock=?, update_time=now() where part_id=?", onHand == null ? 0 : onHand, partId);
    }

    private void writeTxn(Long companyId, Long partId, String warehouse, String type, int quantity,
                          int beforeOnHand, int afterOnHand, int beforeReserved, int afterReserved,
                          String refType, Long refId, String idempotencyKey, String remark) {
        jdbc.update("insert into biz_part_txn(txn_no,company_id,part_id,warehouse,txn_type,quantity," +
                        "before_on_hand,after_on_hand,before_reserved,after_reserved,ref_type,ref_id,idempotency_key," +
                        "operator_id,operator_name,txn_time,remark) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,now(),?)",
                "PT-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-" +
                        UUID.randomUUID().toString().substring(0, 6).toUpperCase(),
                companyId, partId, warehouse, type, quantity, beforeOnHand, afterOnHand,
                beforeReserved, afterReserved, refType, refId, idempotencyKey,
                AuthContext.userId(), AuthContext.nickName(), remark);
    }

    private String warehouseOf(Map<String, Object> part) {
        String warehouse = str(part.get("warehouse"));
        return warehouse.isBlank() ? "DEFAULT" : warehouse;
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
        Map<String, Object> part = requirePart(partId);
        Long partCompany = part.get("company_id") == null ? null : ((Number) part.get("company_id")).longValue();
        Long companyId = dataScope.companyId(AuthContext.require());
        if (req != null && req.get("company_id") != null) {
            companyId = ((Number) req.get("company_id")).longValue();
        }
        if (companyId != null && (partCompany == null || !companyId.equals(partCompany))) {
            throw BizException.forbidden("备件不属于当前公司");
        }
    }

    private static Long longOf(Object v) {
        if (v == null || String.valueOf(v).isBlank()) return null;
        return Long.valueOf(String.valueOf(v));
    }

    private static String str(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private record BizStock(long id, int onHand, int reserved) {}
}
