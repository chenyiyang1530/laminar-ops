package com.laminar.ops.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DataScopeServiceTest {

    @Mock
    private JdbcTemplate jdbc;

    @InjectMocks
    private DataScopeService service;

    @Test
    void hospitalUserIsLimitedToOwnHospital() {
        LoginUser user = new LoginUser();
        user.setUserType("2");
        user.setHospitalId(11L);

        assertEquals(List.of(11L), service.visibleHospitalIds(user));
        assertTrue(service.canAccessHospital(user, 11L));
        assertFalse(service.canAccessHospital(user, 10L));
    }

    @Test
    void superAdminIsNotHospitalLimited() {
        LoginUser user = new LoginUser();
        user.setRoleKeys(Set.of("super_admin"));

        assertNull(service.visibleHospitalIds(user));
        assertTrue(service.canAccessHospital(user, 99L));
        assertNull(service.companyId(user));
    }

    @Test
    void companyUserKeepsCompanyScope() {
        LoginUser user = new LoginUser();
        user.setUserType("1");
        user.setCompanyId(9L);
        user.setDataScope("all");

        assertNull(service.visibleHospitalIds(user));
        assertEquals(9L, service.companyId(user));
        assertTrue(service.canAccessHospital(user, 11L));
    }

    @Test
    void hospitalDataScopeUsesHospitalIdWithoutQuery() {
        LoginUser user = new LoginUser();
        user.setDataScope("hospital");
        user.setHospitalId(10L);

        assertEquals(List.of(10L), service.visibleHospitalIds(user));
        assertEquals(10L, service.singleHospital(user));
        verifyNoInteractions(jdbc);
    }

    @Test
    void nullUserHasNoVisibleHospitals() {
        assertTrue(service.visibleHospitalIds(null).isEmpty());
        assertFalse(service.canAccessHospital(null, 11L));
    }
}
