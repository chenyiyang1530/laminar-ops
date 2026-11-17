package com.laminar.ops.system.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileCheckResumeTest {

    @Mock
    private JdbcTemplate jdbc;
    @Mock
    private com.laminar.ops.common.SystemAlertService systemAlertService;
    @Mock
    private FileCheckProperties properties;
    @Mock
    private FileCheckQueryService queryService;
    @Mock
    private FileCheckNotificationService notificationService;

    @InjectMocks
    private FileCheckScanService scanService;

    @Test
    void restartsFromExistingRunningBatchAndFinishedCheckpoints() throws Exception {
        ReflectionTestUtils.setField(scanService, "uploadPath", Files.createTempDirectory("file-check").toString());
        when(jdbc.queryForList(anyString(), eq(Long.class))).thenReturn(List.of(1L));
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("sys_file_check_checkpoint")) {
                return List.of(Map.of("last_id", 10L, "checked", 2, "missing", 0, "status", "done"));
            }
            return List.of();
        });
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(0);
        when(jdbc.update(contains("insert ignore into sys_file_check_lock"), any(Object[].class))).thenReturn(1);

        Map<String, Object> result = scanService.run("scheduled");
        assertEquals(1L, result.get("runId"));
        assertEquals("success", result.get("status"));
    }
}
