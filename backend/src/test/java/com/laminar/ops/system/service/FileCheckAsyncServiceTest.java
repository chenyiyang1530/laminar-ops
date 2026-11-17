package com.laminar.ops.system.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FileCheckAsyncServiceTest {

    @Mock
    private JdbcTemplate jdbc;
    @Mock
    private FileCheckService fileCheckService;
    @Mock
    private FileCheckExportService exportService;

    @InjectMocks
    private FileCheckAsyncService service;

    @Test
    void batchRecoverContinuesAfterOneFailure() {
        doThrow(new RuntimeException("bad file")).when(fileCheckService).recover(anyLong(), anyString());
        service.batchRecover(1L, List.of(
                Map.of("itemId", "10", "fileUrl", "company/1/a.pdf"),
                Map.of("itemId", "11", "fileUrl", "company/1/b.pdf")
        ), "tester");
        verify(fileCheckService, org.mockito.Mockito.times(2)).recover(anyLong(), anyString());
        verify(jdbc, org.mockito.Mockito.atLeastOnce()).update(anyString(), any(Object[].class));
    }
}
