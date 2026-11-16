package com.laminar.ops.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "01234567890123456789012345678901");
        ReflectionTestUtils.setField(jwtUtil, "expireHours", 12L);
    }

    @Test
    void createAndParseToken() {
        String token = jwtUtil.createToken(42L, "nurse");

        Claims claims = jwtUtil.parse(token);
        assertEquals("42", claims.getSubject());
        assertEquals("nurse", claims.get("username", String.class));
        assertEquals(12 * 3600L, jwtUtil.getExpireSeconds());
    }

    @Test
    void parseRejectsTamperedToken() {
        String token = jwtUtil.createToken(42L, "nurse");

        assertThrows(Exception.class, () -> jwtUtil.parse(token + "x"));
    }
}
