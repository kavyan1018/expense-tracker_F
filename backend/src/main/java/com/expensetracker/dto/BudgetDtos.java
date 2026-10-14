package com.expensetracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.YearMonth;

public final class BudgetDtos {

    private BudgetDtos() {
    }

    /** categoryId null sets the overall budget for the month. */
    public record BudgetRequest(
            @NotNull(message = "Month is required") YearMonth month,
            Long categoryId,
            @NotNull(message = "Amount is required")
            @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
            @Digits(integer = 10, fraction = 2, message = "Use at most 2 decimal places") BigDecimal amount) {
    }

    public record BudgetResponse(Long id, YearMonth month, Long categoryId, String categoryName,
                                 String categoryColor, BigDecimal amount, BigDecimal spent,
                                 BigDecimal remaining, int percentUsed) {
    }
}
