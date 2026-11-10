package com.laminar.ops.biz.task;

import com.laminar.ops.biz.service.ReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderTask {

    private final ReminderService reminderService;

    public void remind() {
        reminderService.refreshStatus();
        int c = reminderService.contractRemind();
        int cert = reminderService.certRemind();
        log.info("[提醒] 合同 {} 条，证书 {} 条", c, cert);
    }
}
