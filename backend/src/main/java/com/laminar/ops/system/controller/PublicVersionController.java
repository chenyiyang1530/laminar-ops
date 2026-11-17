package com.laminar.ops.system.controller;

import com.laminar.ops.common.Result;
import com.laminar.ops.system.service.SysConfigService;
import com.laminar.ops.system.service.VersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicVersionController {

    private final VersionService versionService;
    private final SysConfigService sysConfig;

    @Value("${laminar.upload.path:}")
    private String uploadPath;

    @GetMapping("/version")
    public Result<Map<String, Object>> version() {
        return Result.ok(versionService.info());
    }

    @GetMapping("/features")
    public Result<Map<String, Object>> features() {
        Map<String, Object> result = new LinkedHashMap<>();
        String logo = sysConfig.value("platform.logoUrl", "");
        result.put("platformName", sysConfig.value("platform.name", "层流智能运维"));
        result.put("platformLogo", logo.startsWith("/upload/") ? "/api/public/branding/logo" : logo);
        result.put("defaultPageSize", Math.max(1, sysConfig.intValue("platform.defaultPageSize", 10)));
        result.put("demoLoginEnabled", sysConfig.bool("platform.demoLoginEnabled", true));
        result.put("maintenanceEnabled", sysConfig.bool("platform.maintenance.enabled", false));
        result.put("maintenanceMessage", sysConfig.value("platform.maintenance.message", "平台正在维护，请稍后再试"));
        return Result.ok(result);
    }

    /** 平台 Logo 属于公开品牌资源；只允许读取配置中指定的那一个文件。 */
    @GetMapping("/branding/logo")
    public ResponseEntity<Resource> brandingLogo() {
        String logo = sysConfig.value("platform.logoUrl", "");
        if (!logo.startsWith("/upload/")) {
            return ResponseEntity.notFound().build();
        }
        String relative = logo.substring("/upload/".length()).replace('\\', '/');
        Path root = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root) || !Files.isRegularFile(target)) {
            return ResponseEntity.notFound().build();
        }
        FileSystemResource resource = new FileSystemResource(target);
        MediaType mediaType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.IMAGE_PNG);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"logo\"")
                .body(resource);
    }
}

