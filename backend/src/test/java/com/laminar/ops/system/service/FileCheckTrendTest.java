package com.laminar.ops.system.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileCheckTrendTest {

    @Mock
    private JdbcTemplate jdbc;

    @Test
    void weekTrendAggregatesMissingAndRecovered() {
        FileCheckQueryService service = new FileCheckQueryService(jdbc);
        when(jdbc.queryForList(anyString(), any(Object[].class)))
                .thenReturn(List.of(Map.of("d", "2026-W47", "c", 2)))
                .thenReturn(List.of(Map.of("d", "2026-W47", "c", 1)));
        List<Map<String, Object>> result = service.trend(30, 7L, null, "week");
        assertEquals(1, result.size());
        assertEquals("2026-W47", result.get(0).get("date"));
        assertEquals(2L, result.get(0).get("missing"));
        assertEquals(1L, result.get(0).get("recovered"));
    }

    @Test
    void dayTrendFillsEmptyDays() {
        FileCheckQueryService service = new FileCheckQueryService(jdbc);
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of());
        List<Map<String, Object>> result = service.trend(3, null, null, "day");
        assertEquals(3, result.size());
        assertEquals(0L, result.get(0).get("missing"));
    }
}
