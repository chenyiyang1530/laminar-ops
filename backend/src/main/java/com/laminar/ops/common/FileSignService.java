package com.laminar.ops.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 文件短期签名 URL：避免把登录 Token 放进下载地址。
 */
@Service
public class FileSignService {

    private final String secret;
    private final long ttlSeconds;

    public FileSignService(@Value("${laminar.file.sign-secret:}") String signSecret,
                           @Value("${laminar.config.secret:}") String configSecret,
                           @Value("${laminar.file.sign-ttl-seconds:1800}") long ttlSeconds) {
        String secret = signSecret == null || signSecret.isBlank() ? configSecret : signSecret;
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("未配置文件签名密钥 FILE_SIGN_SECRET / CONFIG_SECRET");
        }
        this.secret = secret;
        this.ttlSeconds = Math.max(60, ttlSeconds);
    }

    public long ttlSeconds() {
        return ttlSeconds;
    }

    public String signedUrl(String path) {
        long expires = System.currentTimeMillis() / 1000 + ttlSeconds;
        String signature = sign(path, expires);
        return "/api/common/signed-file?path=" + URLEncoder.encode(path, StandardCharsets.UTF_8)
                + "&expires=" + expires + "&signature=" + URLEncoder.encode(signature, StandardCharsets.UTF_8);
    }

    public boolean verify(String path, Long expires, String signature) {
        if (path == null || expires == null || signature == null) return false;
        if (expires < System.currentTimeMillis() / 1000) return false;
        String expected = sign(path, expires);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String path, long expires) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((path + "\n" + expires).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("文件签名失败", e);
        }
    }
}
