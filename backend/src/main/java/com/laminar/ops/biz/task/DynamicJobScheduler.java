package com.laminar.ops.biz.task;

import com.laminar.ops.biz.service.InspectionService;
import com.laminar.ops.biz.service.MaintenanceService;
import com.laminar.ops.biz.service.MonitorService;
import com.laminar.ops.biz.service.ApprovalService;
import com.laminar.ops.biz.service.LeaveService;
import com.laminar.ops.system.service.ArchiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 由 sys_job 驱动的动态调度器：启停、Cron、最近执行结果都以后台配置为准。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicJobScheduler {

    private final JdbcTemplate jdbc;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final MonitorService monitorService;
    private final InspectionService inspectionService;
    private final MaintenanceService maintenanceService;
    private final ApprovalService approvalService;
    private final LeaveService leaveService;
    private final ArchiveService archiveService;
    private final ReminderTask reminderTask;

    private final Map<Long, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();

    @EventListener(ApplicationReadyEvent.class)
    public synchronized void reload() {
        futures.values().forEach(f -> f.cancel(false));
        futures.clear();
        List<Map<String, Object>> jobs = jdbc.queryForList(
                "select job_id, bean_name, method_name, cron, status from sys_job");
        for (Map<String, Object> job : jobs) {
            Long id = ((Number) job.get("job_id")).longValue();
            if (!"0".equals(String.valueOf(job.get("status")))) continue;
            schedule(id, String.valueOf(job.get("bean_name")),
                    String.valueOf(job.get("method_name")), String.valueOf(job.get("cron")));
        }
        log.info("[调度] 已加载 {} 个启用任务", futures.size());
    }

    public synchronized void reschedule(Long jobId) {
        ScheduledFuture<?> old = futures.remove(jobId);
        if (old != null) old.cancel(false);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select job_id, bean_name, method_name, cron, status from sys_job where job_id=?", jobId);
        if (rows.isEmpty()) return;
        Map<String, Object> job = rows.get(0);
        if (!"0".equals(String.valueOf(job.get("status")))) return;
        schedule(jobId, String.valueOf(job.get("bean_name")),
                String.valueOf(job.get("method_name")), String.valueOf(job.get("cron")));
    }

    public void runNow(Long jobId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select bean_name, method_name from sys_job where job_id=?", jobId);
        if (rows.isEmpty()) throw new IllegalArgumentException("任务不存在");
        Map<String, Object> job = rows.get(0);
        run(jobId, String.valueOf(job.get("bean_name")), String.valueOf(job.get("method_name")));
    }

    private void schedule(Long id, String bean, String method, String cron) {
        try {
            CronTrigger trigger = new CronTrigger(cron);
            ScheduledFuture<?> future = taskScheduler.schedule(() -> run(id, bean, method), trigger);
            if (future != null) {
                futures.put(id, future);
            }
        } catch (Exception e) {
            log.error("[调度] 任务 {} 的 Cron 无效: {}", id, cron, e);
            updateResult(id, "Cron 无效：" + cron);
        }
    }

    private void run(Long id, String bean, String method) {
        String result;
        try {
            execute(bean, method);
            result = "成功";
        } catch (Exception e) {
            log.error("[调度] 任务执行失败 {}.{}", bean, method, e);
            result = "失败：" + shorten(e.getMessage());
        }
        updateResult(id, result);
    }

    private void execute(String bean, String method) {
        String key = bean + "." + method;
        switch (key) {
            case "monitorCollectTask.collect" -> monitorService.collect();
            case "monitorCollectTask.checkOffline" -> monitorService.checkOffline();
            case "inspectionService.generate" -> inspectionService.generate();
            case "maintenanceService.generate" -> maintenanceService.generate();
            case "approvalService.escalateTimeouts" -> approvalService.escalateTimeouts();
            case "approvalService.escalateDelegateExpiry" -> approvalService.escalateDelegateExpiry();
            case "leaveService.escalateHandoverTimeouts" -> leaveService.escalateHandoverTimeouts();
            case "archiveService.autoArchive" -> archiveService.autoArchive();
            case "reminderTask.remind" -> reminderTask.remind();
            default -> throw new IllegalStateException("未注册的任务：" + key);
        }
    }

    private void updateResult(Long id, String result) {
        try {
            jdbc.update("update sys_job set last_run_time=?, last_result=? where job_id=?",
                    LocalDateTime.now(), result, id);
        } catch (Exception e) {
            log.warn("[调度] 写回任务状态失败: {}", e.getMessage());
        }
    }

    private String shorten(String s) {
        if (s == null) return "未知错误";
        return s.length() > 200 ? s.substring(0, 200) : s;
    }
}
