package com.laminar.ops.system.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class VersionService {

    @Value("${laminar.app.version:1.6.0}")
    private String appVersion;

    @Value("${server.port:8080}")
    private String port;

    private final LocalDateTime startedAt = LocalDateTime.now();
    private final String buildTime;

    public VersionService() {
        this.buildTime = resolveBuildTime();
    }

    public Map<String, Object> info() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", "laminar-ops");
        result.put("version", appVersion);
        result.put("artifactVersion", artifactVersion());
        result.put("buildTime", buildTime);
        result.put("gitCommit", env("GIT_COMMIT", "unknown"));
        result.put("javaVersion", System.getProperty("java.version"));
        result.put("port", port);
        result.put("startedAt", startedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        result.put("uptimeSeconds", Duration.between(startedAt, LocalDateTime.now()).getSeconds());
        return result;
    }

    private String artifactVersion() {
        String v = getClass().getPackage() == null ? null : getClass().getPackage().getImplementationVersion();
        return v == null || v.isBlank() ? appVersion : v;
    }

    private String resolveBuildTime() {
        try {
            URI uri = getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
            Path path = Paths.get(uri);
            return Files.getLastModifiedTime(path).toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
