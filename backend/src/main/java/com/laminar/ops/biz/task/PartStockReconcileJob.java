package com.laminar.ops.biz.task;

import com.laminar.ops.biz.service.PartsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 每日校验备件账面库存与库存余额表是否一致。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PartStockReconcileJob {

    private final PartsService partsService;

    @Scheduled(cron = "0 30 2 * * ?")
    public void run() {
        try {
            int mismatch = partsService.reconcile();
            if (mismatch > 0) {
                log.warn("[备件对账] 发现 {} 个备件账面库存与余额不一致", mismatch);
            } else {
                log.info("[备件对账] 库存一致");
            }
        } catch (Exception e) {
            log.error("[备件对账] 执行失败: {}", e.getMessage(), e);
        }
    }
}
