package com.laminar.ops.biz.controller;

import com.laminar.ops.common.BizException;
import com.laminar.ops.common.Result;
import com.laminar.ops.security.Perm;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/budget")
@RequiredArgsConstructor
public class BudgetController {

    private final JdbcTemplate jdbc;

    @GetMapping("/{id}/executions")
    @Perm("budget:list")
    public Result<List<Map<String, Object>>> executions(@PathVariable Long id) {
        return Result.ok(jdbc.queryForList(
                "select exec_id as `execId`, amount, occur_date as `occurDate`, exec_type as `execType`, remark, " +
                        "date_format(create_time,'%Y-%m-%d %H:%i') as `createTime` " +
                        "from biz_budget_execution where budget_id=? order by exec_id desc", id));
    }

    @PostMapping("/execution")
    @Perm("budget:edit")
    @Transactional
    public Result<Void> addExecution(@RequestBody Map<String, Object> body) {
        Long budgetId = ((Number) body.get("budgetId")).longValue();
        BigDecimal amount = new BigDecimal(String.valueOf(body.get("amount")));
        if (amount.signum() <= 0) throw BizException.badRequest("执行金额必须大于 0");
        Map<String, Object> budget = jdbc.queryForMap("select amount, used_amount from biz_budget where budget_id=?", budgetId);
        BigDecimal used = (BigDecimal) budget.get("used_amount");
        jdbc.update("insert into biz_budget_execution(budget_id,amount,occur_date,exec_type,remark,create_by,create_time) " +
                        "values(?,?,?,'1',?,?,now())", budgetId, amount,
                body.get("occurDate") == null ? java.time.LocalDate.now() : body.get("occurDate"),
                (String) body.get("remark"), com.laminar.ops.security.AuthContext.nickName());
        jdbc.update("update biz_budget set used_amount=? where budget_id=?", used.add(amount), budgetId);
        return Result.ok();
    }
}
