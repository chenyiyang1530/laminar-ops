package com.laminar.ops.biz.service;

import com.laminar.ops.common.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyCodeService {

    private final JdbcTemplate jdbc;

    public String normalize(String code) {
        return code == null ? null : code.trim().toUpperCase();
    }

    /** 当前编码优先，其次历史别名 */
    public Long resolveCompanyId(String rawCode) {
        String code = normalize(rawCode);
        if (code == null || code.isBlank()) return null;
        List<Long> ids = jdbc.queryForList(
                "select company_id from biz_company where company_code=?", Long.class, code);
        if (!ids.isEmpty()) return ids.get(0);
        ids = jdbc.queryForList(
                "select company_id from biz_company_code_alias where alias_code=?", Long.class, code);
        return ids.isEmpty() ? null : ids.get(0);
    }

    public String currentCode(Long companyId) {
        if (companyId == null) return null;
        List<String> codes = jdbc.queryForList(
                "select company_code from biz_company where company_id=?", String.class, companyId);
        return codes.isEmpty() ? null : codes.get(0);
    }

    public void validateNewCode(String rawCode, Long companyId) {
        String code = normalize(rawCode);
        if (code == null || code.isBlank()) throw BizException.badRequest("请填写公司编码");
        List<Long> ids = jdbc.queryForList(
                "select company_id from biz_company_code_alias where alias_code=?", Long.class, code);
        if (!ids.isEmpty() && (companyId == null || !ids.get(0).equals(companyId))) {
            throw BizException.badRequest("该编码已被其他公司历史使用，请更换");
        }
    }

    /** 编码变更后，旧编码进入别名表，保证旧登录方式仍可用 */
    public void recordCodeChange(Long companyId, String oldCode, String newCode) {
        String oldNorm = normalize(oldCode);
        String newNorm = normalize(newCode);
        if (oldNorm == null || oldNorm.isBlank() || oldNorm.equals(newNorm)) return;
        jdbc.update("insert ignore into biz_company_code_alias(alias_code,company_id,create_time) values(?,?,now())",
                oldNorm, companyId);
    }
}
