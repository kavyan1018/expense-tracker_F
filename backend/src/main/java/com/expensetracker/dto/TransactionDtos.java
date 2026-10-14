package com.expensetracker.dto;

import com.expensetracker.domain.PaymentMethod;
import com.expensetracker.domain.Transaction;
import com.expensetracker.domain.TxnType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

public final class TransactionDtos {

    private TransactionDtos() {
    }

    public record TransactionRequest(
            @NotNull(message = "Category is required") Long categoryId,
            @NotNull(message = "Amount is required")
            @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
            @DecimalMax(value = "9999999999.99", message = "Amount is too large")
            @Digits(integer = 10, fraction = 2, message = "Use at most 2 decimal places") BigDecimal amount,
            @NotNull(message = "Date is required") LocalDate date,
            @Size(max = 255) String note,
            PaymentMethod paymentMethod) {
    }

    public record TransactionResponse(Long id, TxnType type, BigDecimal amount, LocalDate date, String note,
                                      Long categoryId, String categoryName, String categoryColor,
                                      PaymentMethod paymentMethod, boolean recurring) {
        public static TransactionResponse of(Transaction t) {
            return new TransactionResponse(t.getId(), t.getType(), t.getAmount(), t.getTxnDate(), t.getNote(),
                    t.getCategory().getId(), t.getCategory().getName(), t.getCategory().getColor(),
                    t.getPaymentMethod(), t.getRecurringId() != null);
        }
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
            return new PageResponse<>(page.getContent().stream().map(mapper).toList(), page.getNumber(),
                    page.getSize(), page.getTotalElements(), page.getTotalPages());
        }
    }
}
