package com.expensetracker.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public enum Frequency {
    WEEKLY, MONTHLY, YEARLY;

    /** The n-th occurrence counted from the start date (n = 0 is the start date). */
    public LocalDate occurrence(LocalDate start, long n) {
        return switch (this) {
            case WEEKLY -> start.plusWeeks(n);
            case MONTHLY -> start.plusMonths(n);
            case YEARLY -> start.plusYears(n);
        };
    }

    /**
     * First occurrence strictly after {@code after}. Always computed from the start date,
     * so a rule starting on the 31st posts on the last day of short months and returns to
     * the 31st afterwards instead of drifting.
     */
    public LocalDate nextAfter(LocalDate start, LocalDate after) {
        if (after.isBefore(start)) {
            return start;
        }
        long n = switch (this) {
            case WEEKLY -> ChronoUnit.WEEKS.between(start, after);
            case MONTHLY -> ChronoUnit.MONTHS.between(start, after);
            case YEARLY -> ChronoUnit.YEARS.between(start, after);
        };
        LocalDate next = occurrence(start, n);
        while (!next.isAfter(after)) {
            next = occurrence(start, ++n);
        }
        return next;
    }
}
