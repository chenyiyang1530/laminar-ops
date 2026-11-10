package com.laminar.ops.biz.task;

import com.laminar.ops.biz.service.MonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonitorCollectTask {

    private final MonitorService monitorService;

    public void collect() {
        try {
            monitorService.collect();
        } catch (Exception e) {
            log.error("[采集] 执行失败", e);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        try {
            monitorService.backfillIfEmpty();
            monitorService.collect();
        } catch (Exception e) {
            log.error("[采集] 初始化失败", e);
        }
    }
}
