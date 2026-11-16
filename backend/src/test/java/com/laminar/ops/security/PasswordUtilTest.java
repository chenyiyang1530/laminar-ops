package com.laminar.ops.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    @Test
    void encodeProducesStableSha256Hex() {
        String first = PasswordUtil.encode("123456");
        String second = PasswordUtil.encode("123456");

        assertEquals(first, second);
        assertEquals(64, first.length());
        assertTrue(first.matches("[0-9a-f]{64}"));
    }

    @Test
    void matchesAcceptsCorrectPasswordAndIgnoresCase() {
        String encoded = PasswordUtil.encode("Admin@123");

        assertTrue(PasswordUtil.matches("Admin@123", encoded));
        assertTrue(PasswordUtil.matches("Admin@123", encoded.toUpperCase()));
        assertFalse(PasswordUtil.matches("admin@123", encoded));
    }

    @Test
    void matchesRejectsNullEncodedValue() {
        assertFalse(PasswordUtil.matches("123456", null));
    }
}
