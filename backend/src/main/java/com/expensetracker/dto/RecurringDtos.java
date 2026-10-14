package com.expensetracker.dto;

import com.expensetracker.domain.Frequency;
import com.expensetracker.domain.PaymentMethod;
import com.expensetracker.domain.RecurringExpense;
import com.expensetracker.domain.TxnType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class RecurringDtos {

    private RecurringDtos() {
    }

    public record RecurringRequest(
            @NotNull(message = "Category is required") Long categoryId,
            @NotNull(message = "Amount is required")
            @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
            @Digits(integer = 10, fraction = 2, message = "Use at most 2 decimal places") BigDecimal amount,
            @NotBlank(message = "Description is required") @Size(max = 255) String description,
            @NotNull(message = "Frequency is required") Frequency frequency,
            @NotNull(message = "Start date is required") LocalDate startDate,
            LocalDate endDate,
            PaymentMethod paymentMethod) {
    }

    public record RecurringResponse(Long id, Long categoryId, String categoryName, TxnType type,
                                    BigDecimal amount, String description, Frequency frequency,
                                    LocalDate startDate, LocalDate endDate, LocalDate nextRunDate,
                                    PaymentMethod paymentMethod, boolean active) {
        public static RecurringResponse of(RecurringExpense r) {
            return new RecurringResponse(r.getId(), r.getCategory().getId(), r.getCategory().getName(),
                    r.getCategory().getType(), r.getAmount(), r.getDescription(), r.getFrequency(),
                    r.getStartDate(), r.getEndDate(), r.getNextRunDate(),
                    r.getPaymentMethod(), r.isActive());
        }
    }
}
