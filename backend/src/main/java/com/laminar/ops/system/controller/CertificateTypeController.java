package com.laminar.ops.system.controller;

import com.laminar.ops.common.Result;
import com.laminar.ops.security.Perm;
import com.laminar.ops.system.service.CertificateTypeService;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/system/cert-type")
@RequiredArgsConstructor
public class CertificateTypeController {

    private final CertificateTypeService certificateTypeService;

    @GetMapping("/requests")
    @Perm("system:certtype:list")
    public Result<List<Map<String, Object>>> requests(@RequestParam(required = false) String status) {
        return Result.ok(certificateTypeService.requests(status));
    }

    @GetMapping("/my")
    @Perm("staff:certtype:list")
    public Result<List<Map<String, Object>>> myRequests() {
        return Result.ok(certificateTypeService.myRequests(com.laminar.ops.security.AuthContext.require()));
    }

    @PostMapping("/requests/{id}/resubmit")
    @Perm("staff:certtype:resubmit")
    public Result<Void> resubmit(@PathVariable Long id) {
        certificateTypeService.resubmit(id);
        return Result.ok();
    }

    @PostMapping("/requests/{id}/approve")
    @Perm("system:certtype:approve")
    public Result<Void> approve(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        certificateTypeService.approve(id, body == null ? null : String.valueOf(body.getOrDefault("comment", "")));
        return Result.ok();
    }

    @PostMapping("/requests/{id}/reject")
    @Perm("system:certtype:approve")
    public Result<Void> reject(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        certificateTypeService.reject(id, body == null ? null : String.valueOf(body.getOrDefault("comment", "")));
        return Result.ok();
    }
}
