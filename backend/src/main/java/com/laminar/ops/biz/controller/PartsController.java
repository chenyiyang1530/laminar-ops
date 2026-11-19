package com.laminar.ops.biz.controller;

import com.laminar.ops.biz.service.PartsService;
import com.laminar.ops.common.Result;
import com.laminar.ops.security.Perm;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/parts")
@RequiredArgsConstructor
public class PartsController {

    private final PartsService partsService;

    @GetMapping("/requisition/page")
    @Perm("resource:requisition:list")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") int pageNum,
                                            @RequestParam(defaultValue = "10") int pageSize,
                                            @RequestParam(required = false) String status) {
        return Result.ok(partsService.page(pageNum, pageSize, status));
    }

    @GetMapping("/requisition/{id}")
    @Perm("resource:requisition:list")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(partsService.detail(id));
    }

    @PostMapping("/requisition")
    @Perm("resource:requisition:add")
    public Result<Long> create(@RequestBody Map<String, Object> body) {
        return Result.ok(partsService.create(body));
    }

    @PostMapping("/requisition/{id}/approve")
    @Perm("resource:requisition:approve")
    public Result<Void> approve(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        partsService.approve(id, true, body == null ? null : body.get("remark"));
        return Result.ok();
    }

    @PostMapping("/requisition/{id}/reject")
    @Perm("resource:requisition:approve")
    public Result<Void> reject(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        partsService.approve(id, false, body == null ? null : body.get("remark"));
        return Result.ok();
    }

    @PostMapping("/requisition/{id}/out")
    @Perm("resource:requisition:out")
    public Result<Void> out(@PathVariable Long id) {
        partsService.outbound(id);
        return Result.ok();
    }

    @PostMapping("/stock")
    @Perm("resource:part:stock")
    public Result<Long> stock(@RequestBody Map<String, Object> body) {
        return Result.ok(partsService.createAdjustment(body));
    }

    @GetMapping("/adjustment/page")
    @Perm("resource:part:list")
    public Result<Map<String, Object>> adjustmentPage(@RequestParam(defaultValue = "1") int pageNum,
                                                      @RequestParam(defaultValue = "10") int pageSize,
                                                      @RequestParam(required = false) String status) {
        return Result.ok(partsService.adjustmentPage(pageNum, pageSize, status));
    }

    @PostMapping("/adjustment/{id}/approve")
    @Perm("resource:requisition:approve")
    public Result<Void> approveAdjustment(@PathVariable Long id,
                                           @RequestBody(required = false) Map<String, String> body) {
        partsService.approveAdjustment(id, true, body == null ? null : body.get("remark"));
        return Result.ok();
    }

    @PostMapping("/adjustment/{id}/reject")
    @Perm("resource:requisition:approve")
    public Result<Void> rejectAdjustment(@PathVariable Long id,
                                          @RequestBody(required = false) Map<String, String> body) {
        partsService.approveAdjustment(id, false, body == null ? null : body.get("remark"));
        return Result.ok();
    }

    @PostMapping("/adjustment/{id}/review")
    @Perm("resource:requisition:approve")
    public Result<Void> reviewAdjustment(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Object pass = body.get("pass");
        boolean approved = pass == null || Boolean.parseBoolean(String.valueOf(pass));
        partsService.reviewAdjustment(id, approved, (String) body.get("remark"));
        return Result.ok();
    }

    @PostMapping("/requisition/{id}/return")
    @Perm("resource:requisition:out")
    public Result<Void> returnItem(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        partsService.returnItem(id, ((Number) body.get("itemId")).longValue(),
                ((Number) body.get("quantity")).intValue(), (String) body.get("reason"));
        return Result.ok();
    }

    @PostMapping("/reconcile")
    @Perm("resource:part:stock")
    public Result<Integer> reconcile() {
        return Result.ok(partsService.reconcile());
    }

    @GetMapping("/reconcile/page")
    @Perm("resource:part:list")
    public Result<Map<String, Object>> reconcilePage(@RequestParam(defaultValue = "1") int pageNum,
                                                     @RequestParam(defaultValue = "10") int pageSize,
                                                     @RequestParam(required = false) String resolved) {
        return Result.ok(partsService.reconcilePage(pageNum, pageSize, resolved));
    }

    @PostMapping("/reconcile/{id}/resolve")
    @Perm("resource:requisition:approve")
    public Result<Void> resolveReconcile(@PathVariable Long id, @RequestBody Map<String, String> body) {
        partsService.resolveReconcile(id, body == null ? null : body.get("note"));
        return Result.ok();
    }

    @GetMapping("/returns/page")
    @Perm("resource:part:list")
    public Result<Map<String, Object>> returnPage(@RequestParam(defaultValue = "1") int pageNum,
                                                   @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(partsService.returnPage(pageNum, pageSize));
    }

    @GetMapping("/returns/export")
    @Perm("resource:part:export")
    public ResponseEntity<String> exportReturns() {
        Map<String, Object> page = partsService.returnPage(1, 10000);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) page.get("records");
        StringBuilder csv = new StringBuilder("\uFEFF退料单号,领用单号,备件,使用设备,数量,原因,操作人,时间\n");
        for (Map<String, Object> row : rows) {
            csv.append(csv(row.get("returnNo"))).append(',')
                    .append(csv(row.get("reqNo"))).append(',')
                    .append(csv(row.get("partName"))).append(',')
                    .append(csv(row.get("equipmentName"))).append(',')
                    .append(row.get("quantity")).append(',')
                    .append(csv(row.get("reason"))).append(',')
                    .append(csv(row.get("operatorName"))).append(',')
                    .append(csv(row.get("createTime"))).append('\n');
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=part-returns.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv.toString());
    }

    @GetMapping("/low-stock")
    @Perm("resource:part:list")
    public Result<List<Map<String, Object>>> lowStock() {
        return Result.ok(partsService.lowStock());
    }

    @GetMapping("/warehouse-stock")
    @Perm("resource:part:list")
    public Result<List<Map<String, Object>>> warehouseStock() {
        return Result.ok(partsService.warehouseStock());
    }

    @PostMapping("/transfer")
    @Perm("resource:part:stock")
    public Result<Void> transfer(@RequestBody Map<String, Object> body) {
        partsService.transfer(((Number) body.get("partId")).longValue(),
                (String) body.get("fromWarehouse"), (String) body.get("toWarehouse"),
                ((Number) body.get("quantity")).intValue(), (String) body.get("reason"));
        return Result.ok();
    }

    @GetMapping("/transfers/page")
    @Perm("resource:part:list")
    public Result<Map<String, Object>> transferPage(@RequestParam(defaultValue = "1") int pageNum,
                                                    @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(partsService.transferPage(pageNum, pageSize));
    }

    @GetMapping("/reports/stock")
    @Perm("resource:part:list")
    public Result<Map<String, Object>> stockReports() {
        return Result.ok(partsService.stockReports());
    }

    private String csv(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        return '"' + text.replace("\"", "\"\"") + '"';
    }

    @GetMapping("/{partId}/transactions")
    @Perm("resource:part:list")
    public Result<java.util.List<Map<String, Object>>> transactions(@PathVariable Long partId,
                                                                   @RequestParam(defaultValue = "20") int limit) {
        return Result.ok(partsService.transactions(partId, limit));
    }
}
