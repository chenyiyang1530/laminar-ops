package com.laminar.ops.system.service;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "laminar.file-check")
public class FileCheckProperties {

    private boolean enabled = true;
    private String cron = "0 30 3 * * *";
    private int batchSize = 500;
    private int threads = 4;
    private Map<String, String> typeSeverity = new LinkedHashMap<>(Map.of(
            "document", "P2",
            "image", "P3",
            "other", "P3"
    ));
    private Map<String, Integer> slaHours = new LinkedHashMap<>(Map.of(
            "P1", 24,
            "P2", 72,
            "P3", 168
    ));
    private boolean emailEnabled = false;
    private String emailTo = "";
    private int slaLevel2Hours = 24;
    private int slaLevel3Hours = 72;
}
