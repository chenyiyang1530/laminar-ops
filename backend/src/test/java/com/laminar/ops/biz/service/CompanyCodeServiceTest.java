package com.laminar.ops.biz.service;

import com.laminar.ops.common.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyCodeServiceTest {

    @Mock
    private JdbcTemplate jdbc;

    @InjectMocks
    private CompanyCodeService service;

    @Test
    void normalizeTrimsAndUppercases() {
        assertEquals("TESTCO2", service.normalize(" testco2 "));
        assertNull(service.normalize(null));
    }

    @Test
    void resolveCurrentCodeFirst() {
        when(jdbc.queryForList(contains("biz_company where company_code"), eq(Long.class), eq("TESTCO2")))
                .thenReturn(List.of(9L));

        assertEquals(9L, service.resolveCompanyId(" testco2 "));
    }

    @Test
    void resolveFallsBackToAlias() {
        when(jdbc.queryForList(contains("biz_company where company_code"), eq(Long.class), eq("OLD")))
                .thenReturn(List.of());
        when(jdbc.queryForList(contains("biz_company_code_alias"), eq(Long.class), eq("OLD")))
                .thenReturn(List.of(7L));

        assertEquals(7L, service.resolveCompanyId("old"));
    }

    @Test
    void resolveBlankReturnsNull() {
        assertNull(service.resolveCompanyId("  "));
    }

    @Test
    void validateNewCodeRejectsMissingCode() {
        assertThrows(BizException.class, () -> service.validateNewCode(" ", 7L));
    }
}
