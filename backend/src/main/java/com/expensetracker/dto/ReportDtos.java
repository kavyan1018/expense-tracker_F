package com.expensetracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class ReportDtos {

    private ReportDtos() {
    }

    public record Summary(LocalDate from, LocalDate to, BigDecimal income, BigDecimal expense, BigDecimal balance) {
    }

    public record CategoryTotal(Long categoryId, String name, String color, BigDecimal total, double percent) {
    }

    /** month is formatted as yyyy-MM. */
    public record MonthTotal(String month, BigDecimal income, BigDecimal expense) {
    }
}
