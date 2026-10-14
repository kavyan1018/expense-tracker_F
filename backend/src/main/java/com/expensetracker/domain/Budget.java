package com.expensetracker.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "budgets")
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    /** Null means the overall budget for the month. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", updatable = false)
    private Category category;

    /** Always the first day of the month. */
    @Column(name = "period_month", nullable = false, updatable = false)
    private LocalDate periodMonth;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    protected Budget() {
    }

    public Budget(Long userId, Category category, LocalDate periodMonth, BigDecimal amount) {
        this.userId = userId;
        this.category = category;
        this.periodMonth = periodMonth;
        this.amount = amount;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Category getCategory() { return category; }
    public LocalDate getPeriodMonth() { return periodMonth; }
    public BigDecimal getAmount() { return amount; }

    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
