package com.laminar.ops.common;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 上传目录必须在启动时明确配置，避免相对路径随工作目录变化。
 */
@Slf4j
@Component
public class UploadPathValidator {

    @Value("${laminar.upload.path:}")
    private String uploadPath;

    @PostConstruct
    public void validate() {
        if (uploadPath == null || uploadPath.isBlank()) {
            throw new IllegalStateException(
                    "UPLOAD_DIR 必须配置为绝对路径，例如 D:/ideaWork/laminar-ops/backend/upload");
        }
        Path path = Path.of(uploadPath.trim());
        if (!path.isAbsolute()) {
            throw new IllegalStateException(
                    "UPLOAD_DIR 必须是绝对路径，当前值：" + uploadPath);
        }
        if (!Files.isDirectory(path)) {
            throw new IllegalStateException(
                    "UPLOAD_DIR 目录不存在：" + path + "，请先创建目录");
        }
        if (!Files.isWritable(path)) {
            throw new IllegalStateException(
                    "UPLOAD_DIR 目录不可写：" + path);
        }
        log.info("[启动] 上传目录校验通过：{}", path.toAbsolutePath().normalize());
    }
}
