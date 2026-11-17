package com.laminar.ops.system.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileCheckSlaPolicyTest {

    private final LocalDateTime now = LocalDateTime.of(2026, 11, 17, 12, 0);

    @Test
    void levelIsZeroBeforeDeadline() {
        assertEquals(0, FileCheckSlaPolicy.level(now.plusHours(1), now, 24, 72));
    }

    @Test
    void escalatesThroughThreeLevels() {
        assertEquals(1, FileCheckSlaPolicy.level(now.minusHours(1), now, 24, 72));
        assertEquals(2, FileCheckSlaPolicy.level(now.minusHours(24), now, 24, 72));
        assertEquals(3, FileCheckSlaPolicy.level(now.minusHours(72), now, 24, 72));
    }
}
