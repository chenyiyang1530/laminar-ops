package com.laminar.ops.biz.controller;

import com.laminar.ops.biz.service.ContractScopeService;
import com.laminar.ops.common.Result;
import com.laminar.ops.security.Perm;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contract-scope")
@RequiredArgsConstructor
public class ContractScopeController {

    private final ContractScopeService contractScopeService;

    @GetMapping("/{contractId}")
    @Perm("customer:contract:list")
    public Result<List<Map<String, Object>>> list(@PathVariable Long contractId) {
        return Result.ok(contractScopeService.list(contractId));
    }

    @PostMapping("/{contractId}")
    @Perm("customer:contract:edit")
    public Result<Void> save(@PathVariable Long contractId, @RequestBody List<Map<String, Object>> scopes) {
        contractScopeService.save(contractId, scopes);
        return Result.ok();
    }
}
