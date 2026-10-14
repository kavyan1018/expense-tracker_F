package com.expensetracker.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class FrequencyTest {

    @Test
    void monthlyFromThe31stUsesMonthEndWithoutDrifting() {
        LocalDate start = LocalDate.of(2026, 1, 31);
        LocalDate feb = Frequency.MONTHLY.nextAfter(start, start);
        LocalDate mar = Frequency.MONTHLY.nextAfter(start, feb);
        assertThat(feb).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(mar).isEqualTo(LocalDate.of(2026, 3, 31));
    }

    @Test
    void beforeStartReturnsStart() {
        LocalDate start = LocalDate.of(2026, 5, 10);
        assertThat(Frequency.WEEKLY.nextAfter(start, LocalDate.of(2026, 1, 1))).isEqualTo(start);
    }

    @Test
    void weeklyAndYearly() {
        LocalDate start = LocalDate.of(2024, 2, 29);
        assertThat(Frequency.WEEKLY.nextAfter(start, LocalDate.of(2024, 3, 7))).isEqualTo(LocalDate.of(2024, 3, 14));
        assertThat(Frequency.YEARLY.nextAfter(start, start)).isEqualTo(LocalDate.of(2025, 2, 28));
        assertThat(Frequency.YEARLY.nextAfter(start, LocalDate.of(2027, 6, 1))).isEqualTo(LocalDate.of(2028, 2, 29));
    }
}
