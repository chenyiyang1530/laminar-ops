package com.laminar.ops.system.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FileCheckExportServiceTest {

    @Test
    void excelContainsZipHeaderAndRows() {
        FileCheckExportService service = new FileCheckExportService();
        byte[] bytes = service.excel(List.of(Map.ofEntries(
                Map.entry("severity", "P2"),
                Map.entry("tableName", "biz_document"),
                Map.entry("recordId", 1),
                Map.entry("fileColumn", "file_url"),
                Map.entry("fileType", "document"),
                Map.entry("companyName", "测试公司"),
                Map.entry("hospitalName", "测试医院"),
                Map.entry("filePath", "company/1/document/a.pdf"),
                Map.entry("status", "0"),
                Map.entry("slaDeadline", "2026-11-20 12:00:00"),
                Map.entry("firstSeen", "2026-11-17 12:00:00")
        )));
        assertTrue(bytes.length > 100);
        assertTrue(bytes[0] == 'P' && bytes[1] == 'K');
    }
}
