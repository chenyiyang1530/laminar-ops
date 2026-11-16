package com.laminar.ops.common;

import org.junit.jupiter.api.Test;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FileSignServiceTest {

    @Test
    void signedUrlCanBeVerifiedAndRejectsTampering() {
        FileSignService service = new FileSignService("sign-secret", "config-secret", 1800);
        String url = service.signedUrl("company/9/test/a.png");
        Map<String, String> params = query(url);
        Long expires = Long.valueOf(params.get("expires"));
        assertTrue(service.verify("company/9/test/a.png", expires, params.get("signature")));
        assertFalse(service.verify("company/9/test/b.png", expires, params.get("signature")));
        assertFalse(service.verify("company/9/test/a.png", expires, params.get("signature") + "x"));
    }

    @Test
    void expiredSignatureIsRejected() {
        FileSignService service = new FileSignService("", "config-secret", 60);
        String url = service.signedUrl("company/9/test/a.png");
        Map<String, String> params = query(url);
        assertFalse(service.verify("company/9/test/a.png", 1L, params.get("signature")));
    }

    private Map<String, String> query(String url) {
        Map<String, String> result = new HashMap<>();
        for (String pair : url.substring(url.indexOf('?') + 1).split("&")) {
            String[] parts = pair.split("=", 2);
            result.put(parts[0], URLDecoder.decode(parts.length > 1 ? parts[1] : "", StandardCharsets.UTF_8));
        }
        return result;
    }
}
