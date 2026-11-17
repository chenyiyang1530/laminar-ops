package com.laminar.ops.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 可选 ClamAV 病毒扫描；未安装扫描服务时可关闭，开启但不可用会拒绝上传。
 */
@Slf4j
@Service
public class UploadScanService {

    @Value("${laminar.upload.virus-scan-enabled:false}")
    private boolean enabled;

    @Value("${laminar.upload.clamav-command:}")
    private String command;

    @Value("${laminar.upload.path:}")
    private String uploadPath;

    public void scan(MultipartFile file) {
        if (!enabled) return;
        if (command == null || command.isBlank()) {
            throw new BizException(500, "病毒扫描已开启但未配置 CLAMAV_COMMAND");
        }
        Path temp = null;
        try {
            Path scanDir = Path.of(uploadPath).toAbsolutePath().normalize().resolve(".scan");
            Files.createDirectories(scanDir);
            temp = Files.createTempFile(scanDir, "upload-", ".tmp");
            file.transferTo(temp);
            List<String> cmd = new ArrayList<>();
            cmd.add(command);
            cmd.add("--no-summary");
            cmd.add(temp.toString());
            ProcessBuilder builder = new ProcessBuilder(cmd);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            String output = new String(process.getInputStream().readAllBytes());
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new BizException(500, "病毒扫描超时");
            }
            int code = process.exitValue();
            if (code == 1) {
                log.warn("上传文件疑似病毒：{}，扫描输出：{}", file.getOriginalFilename(), output);
                throw new BizException(400, "文件未通过病毒扫描");
            }
            if (code != 0) {
                log.error("病毒扫描服务异常，退出码 {}：{}", code, output);
                throw new BizException(500, "病毒扫描服务异常，请稍后重试");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("病毒扫描失败", e);
            throw new BizException(500, "病毒扫描失败");
        } finally {
            if (temp != null) {
                try { Files.deleteIfExists(temp); } catch (IOException ignored) { }
            }
        }
    }
}

