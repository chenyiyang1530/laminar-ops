package com.laminar.ops.common;

import com.laminar.ops.system.service.FileCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时触发文件存在性巡检。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileExistenceCheckJob {

    private final FileCheckService fileCheckService;

    @Value("${laminar.file-check.enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "${laminar.file-check.cron:0 30 3 * * *}")
    public void scheduledCheck() {
        if (!enabled) return;
        log.info("[文件巡检] 定时任务开始");
        fileCheckService.run("scheduled");
    }
}
