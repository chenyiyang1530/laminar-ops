package com.laminar.ops.system.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SysConfigCipherTest {

    private SysConfigCipher cipher() {
        return new SysConfigCipher("unit-test-config-secret", "unit-test-jwt-secret");
    }

    @Test
    void encryptAndDecryptRoundTrip() {
        SysConfigCipher cipher = cipher();
        String encrypted = cipher.encrypt("Laminar@2026");

        assertTrue(encrypted.startsWith("ENC:"));
        assertNotEquals("Laminar@2026", encrypted);
        assertEquals("Laminar@2026", cipher.decrypt(encrypted));
    }

    @Test
    void encryptIsIdempotent() {
        SysConfigCipher cipher = cipher();
        String encrypted = cipher.encrypt("secret");
        assertEquals(encrypted, cipher.encrypt(encrypted));
    }

    @Test
    void wrongKeyCannotDecrypt() {
        String encrypted = cipher().encrypt("secret");
        SysConfigCipher other = new SysConfigCipher("another-secret", "jwt");
        assertThrows(IllegalStateException.class, () -> other.decrypt(encrypted));
    }
}
