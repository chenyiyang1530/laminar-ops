package com.laminar.ops.system.service;

import java.time.Duration;
import java.time.LocalDateTime;

public final class FileCheckSlaPolicy {

    private FileCheckSlaPolicy() {
    }

    public static int level(LocalDateTime deadline, LocalDateTime now, int level2Hours, int level3Hours) {
        if (deadline == null || !now.isAfter(deadline)) return 0;
        long overdue = Duration.between(deadline, now).toHours();
        if (overdue >= level3Hours) return 3;
        if (overdue >= level2Hours) return 2;
        return 1;
    }
}
