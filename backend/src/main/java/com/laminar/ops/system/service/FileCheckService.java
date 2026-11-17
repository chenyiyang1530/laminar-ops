package com.laminar.ops.system.service;

import com.laminar.ops.common.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 文件巡检门面：对外保持稳定接口，内部拆分为扫描、查询和恢复。
 */
@Service
@RequiredArgsConstructor
public class FileCheckService {

    private final FileCheckScanService scanService;
    private final FileCheckQueryService queryService;
    private final FileCheckRecoveryService recoveryService;

    public Map<String, Object> run(String triggerType) {
        return scanService.run(triggerType);
    }

    public Map<String, Object> pageRuns(int pageNum, int pageSize) {
        return queryService.pageRuns(pageNum, pageSize);
    }

    public Map<String, Object> pageItems(int pageNum, int pageSize, String status, String keyword,
                                         Long companyId, Long hospitalId, String fileType, String severity, Long itemId) {
        return queryService.pageItems(pageNum, pageSize, status, keyword, companyId, hospitalId, fileType, severity, itemId);
    }

    public List<Map<String, Object>> exportRows(String status, String keyword, Long companyId,
                                                Long hospitalId, String fileType, String severity) {
        return queryService.exportRows(status, keyword, companyId, hospitalId, fileType, severity);
    }

    public List<Map<String, Object>> trend(int days, Long companyId, Long hospitalId, String granularity) {
        return queryService.trend(days, companyId, hospitalId, granularity);
    }

    public Map<String, Object> recover(Long itemId, String fileUrl) {
        if (itemId == null) throw BizException.badRequest("巡检项不能为空");
        return recoveryService.recover(itemId, fileUrl);
    }
}
