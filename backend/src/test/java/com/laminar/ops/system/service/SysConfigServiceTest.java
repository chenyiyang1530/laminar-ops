package com.laminar.ops.system.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysConfigServiceTest {

    @Mock
    private JdbcTemplate jdbc;

    @Mock
    private SysConfigCipher cipher;

    @Mock
    private StringRedisTemplate redis;

    @InjectMocks
    private SysConfigService service;

    @Test
    void readsStringAndBooleanValues() {
        when(jdbc.queryForList(contains("sys_config"), eq(String.class), eq("sys.account.captchaEnabled")))
                .thenReturn(List.of("true"));
        when(jdbc.queryForList(contains("sys_config"), eq(String.class), eq("sys.session.idleMinutes")))
                .thenReturn(List.of("45"));

        assertTrue(service.bool("sys.account.captchaEnabled", false));
        assertEquals(45, service.intValue("sys.session.idleMinutes", 30));
    }

    @Test
    void fallsBackWhenValueMissing() {
        when(jdbc.queryForList(anyString(), eq(String.class), anyString())).thenReturn(List.of());

        assertEquals("123456", service.value("sys.user.initPassword", "123456"));
        assertEquals(5, service.intValue("sys.user.login.maxFail", 5));
        assertFalse(service.bool("missing.key", false));
    }

    @Test
    void invalidIntegerFallsBack() {
        when(jdbc.queryForList(anyString(), eq(String.class), anyString())).thenReturn(List.of("not-a-number"));

        assertEquals(30, service.intValue("sys.session.idleMinutes", 30));
    }

    @Test
    void versionChangeInvalidatesLocalCache() {
        ValueOperations<String, String> valueOps = org.mockito.Mockito.mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("sys:config:version")).thenReturn("0", "1");
        when(jdbc.queryForList(anyString(), eq(String.class), eq("sys.session.idleMinutes")))
                .thenReturn(List.of("30"), List.of("45"));

        assertEquals(30, service.intValue("sys.session.idleMinutes", 10));
        assertEquals(45, service.intValue("sys.session.idleMinutes", 10));
    }
}
