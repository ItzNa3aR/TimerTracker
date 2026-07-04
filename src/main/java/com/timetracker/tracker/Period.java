package com.timetracker.tracker;

import java.time.LocalDate;
import java.time.LocalDateTime;

public enum Period {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year"),
    ALL("All Time");

    public final String label;

    Period(String label) {
        this.label = label;
    }

    public LocalDateTime since() {
        LocalDate today = LocalDate.now();
        switch (this) {
            case TODAY:
                return today.atStartOfDay();
            case YESTERDAY:
                return today.minusDays(1).atStartOfDay();
            case WEEK:
                return today.minusDays(6).atStartOfDay();
            case MONTH:
                return today.withDayOfMonth(1).atStartOfDay();
            case YEAR:
                return today.withDayOfYear(1).atStartOfDay();
            case ALL:
            default:
                return null;
        }
    }

    public LocalDateTime until() {
        if (this == YESTERDAY) {
            return LocalDate.now().atStartOfDay();
        }
        return null;
    }
}
