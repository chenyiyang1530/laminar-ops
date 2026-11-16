package com.laminar.ops.biz.service;

import com.laminar.ops.security.DataScopeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractScopeServiceTest {

    @Mock
    private JdbcTemplate jdbc;

    @Mock
    private DataScopeService dataScope;

    @InjectMocks
    private ContractScopeService service;

    @Test
    void equipmentScopeHasHighestPriority() {
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(
                Map.of("company_id", 9L, "scope_type", "department"),
                Map.of("company_id", 7L, "scope_type", "equipment"),
                Map.of("company_id", 8L, "scope_type", "hospital")
        ));

        assertEquals(7L, service.resolveCompany(11L, 15L, 18L, 10L));
    }

    @Test
    void uniqueCampusScopeCanMatch() {
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(
                Map.of("company_id", 9L, "scope_type", "campus")
        ));

        assertEquals(9L, service.resolveCompany(11L, null, null, 10L));
    }

    @Test
    void samePriorityMultipleCompaniesReturnsNull() {
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(
                Map.of("company_id", 7L, "scope_type", "department"),
                Map.of("company_id", 9L, "scope_type", "department")
        ));

        assertNull(service.resolveCompany(11L, 15L, null, null));
    }

    @Test
    void noMatchingContractReturnsNull() {
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of());

        assertNull(service.resolveCompany(11L, 15L, 18L, 10L));
    }

    @Test
    void nullHospitalDoesNotQueryDatabase() {
        assertNull(service.resolveCompany(null, 15L, 18L, 10L));
        verifyNoInteractions(jdbc);
    }
}
