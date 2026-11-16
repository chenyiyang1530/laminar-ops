package com.laminar.ops.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClientInfoUtilTest {

    private static final String CHROME_WINDOWS =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/142.0.0.0 Safari/537.36";

    @Test
    void parsesChromeAndWindows() {
        assertEquals("Chrome 142.0.0.0", ClientInfoUtil.browser(CHROME_WINDOWS));
        assertEquals("Windows 10/11", ClientInfoUtil.os(CHROME_WINDOWS));
    }

    @Test
    void privateAddressesDoNotUseExternalLookup() {
        assertEquals("本机", ClientInfoUtil.location("127.0.0.1"));
        assertEquals("内网", ClientInfoUtil.location("192.168.1.10"));
        assertEquals("未知", ClientInfoUtil.location(null));
    }

    @Test
    void parsesMobileOperatingSystems() {
        assertEquals("Android", ClientInfoUtil.os("Mozilla/5.0 (Linux; Android 14)"));
        assertEquals("iOS", ClientInfoUtil.os("Mozilla/5.0 (iPhone; CPU iPhone OS 18_0)"));
    }
}
