package com.laminar.ops.biz.task;

import com.laminar.ops.common.SystemAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** 每日检查安全库存，低库存推送平台管理员告警。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PartStockAlertJob {

    private final JdbcTemplate jdbc;
    private final SystemAlertService systemAlertService;

    @Scheduled(cron = "0 0 9 * * ?")
    public void run() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select part_id, company_id, part_name, current_stock, safety_stock, unit from biz_part " +
                        "where status='0' and current_stock <= safety_stock");
        for (Map<String, Object> row : rows) {
            int current = ((Number) row.get("current_stock")).intValue();
            int safety = ((Number) row.get("safety_stock")).intValue();
            systemAlertService.error("part-low-stock-" + row.get("part_id"), "备件安全库存告警",
                    "备件 " + row.get("part_name") + " 当前库存 " + current + row.get("unit")
                            + "，安全库存 " + safety + row.get("unit") + "，请及时补货");
        }
        log.info("[备件安全库存] 检查 {} 个低库存备件", rows.size());
    }
}
