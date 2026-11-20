package com.laminar.ops.biz.task;

import com.laminar.ops.biz.service.DeviceControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 超时未复核的控制命令自动撤销，避免长时间悬挂。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ControlCommandExpireJob {

    private final DeviceControlService controlService;

    @Scheduled(fixedDelay = 60_000L)
    public void run() {
        try {
            int count = controlService.expirePending();
            if (count > 0) log.warn("[远程控制] 自动撤销 {} 条超时未复核命令", count);
        } catch (Exception e) {
            log.warn("[远程控制] 超时撤销任务失败: {}", e.getMessage());
        }
    }
}
