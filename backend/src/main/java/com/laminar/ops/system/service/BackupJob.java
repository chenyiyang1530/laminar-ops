package com.laminar.ops.system.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 应用内定时备份：调用现有 backup.ps1，备份 MySQL + 上传目录。
 * Windows 部署使用；Linux 可把脚本换成 backup.sh 后复用同一入口。
 */
@Slf4j
@Service
public class BackupJob {

    @Value("${laminar.backup.enabled:true}")
    private boolean enabled;

    @Value("${laminar.backup.script:D:/ideaWork/laminar-ops/tools/backup.ps1}")
    private String script;

    @Value("${laminar.backup.dir:D:/backups/laminar-ops}")
    private String backupDir;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(cron = "${laminar.backup.cron:0 0 2 * * *}")
    public void scheduled() {
        if (!enabled) return;
        runNow();
    }

    public synchronized String runNow() {
        if (!running.compareAndSet(false, true)) {
            return "备份正在执行，请稍后再试";
        }
        try {
            Path scriptPath = Path.of(script);
            if (!Files.isRegularFile(scriptPath)) {
                return "备份脚本不存在：" + script;
            }
            if (dbPassword == null || dbPassword.isBlank()) {
                return "未配置数据库密码，无法备份";
            }
            String powershell = Files.isRegularFile(Path.of("C:/Program Files/PowerShell/7/pwsh.exe"))
                    ? "C:/Program Files/PowerShell/7/pwsh.exe" : "powershell.exe";
            List<String> command = new ArrayList<>(List.of(
                    powershell, "-NoProfile", "-ExecutionPolicy", "Bypass",
                    "-File", script, "-BackupDir", backupDir));
            ProcessBuilder builder = new ProcessBuilder(command);
            Path logFile = Files.createTempFile("laminar-backup-", ".log");
            builder.redirectOutput(logFile.toFile());
            builder.redirectError(ProcessBuilder.Redirect.appendTo(logFile.toFile()));
            builder.environment().put("DB_PASSWORD", dbPassword);
            Process process = builder.start();
            boolean finished = process.waitFor(30, TimeUnit.MINUTES);
            String output = Files.exists(logFile) ? Files.readString(logFile, StandardCharsets.UTF_8) : "";
            Files.deleteIfExists(logFile);
            if (!finished) {
                process.destroyForcibly();
                return "备份超时";
            }
            log.info("备份完成，退出码={}，输出={}", process.exitValue(), output);
            return output.isBlank() ? "备份完成" : output;
        } catch (Exception e) {
            log.error("备份失败", e);
            return "备份失败：" + e.getMessage();
        } finally {
            running.set(false);
        }
    }
}
