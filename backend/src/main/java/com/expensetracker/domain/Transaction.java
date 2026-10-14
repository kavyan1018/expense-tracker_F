package com.expensetracker.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TxnType type;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "txn_date", nullable = false)
    private LocalDate txnDate;

    @Column(length = 255)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    @Column(name = "recurring_id", updatable = false)
    private Long recurringId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Transaction() {
    }

    public Transaction(Long userId, Category category, BigDecimal amount, LocalDate txnDate,
                       String note, PaymentMethod paymentMethod, Long recurringId) {
        this.userId = userId;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.txnDate = txnDate;
        this.note = note;
        this.recurringId = recurringId;
        setCategory(category);
    }

    /** The transaction type always follows its category. */
    public void setCategory(Category category) {
        this.category = category;
        this.type = category.getType();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Category getCategory() { return category; }
    public TxnType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getTxnDate() { return txnDate; }
    public String getNote() { return note; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public Long getRecurringId() { return recurringId; }

    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setTxnDate(LocalDate txnDate) { this.txnDate = txnDate; }
    public void setNote(String note) { this.note = note; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
}
