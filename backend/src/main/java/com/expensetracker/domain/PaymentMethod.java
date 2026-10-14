package com.expensetracker.domain;

/** How a transaction was paid or received. */
public enum PaymentMethod {
    CASH, UPI, CARD, BANK_TRANSFER;

    public String label() {
        return switch (this) {
            case CASH -> "Cash";
            case UPI -> "UPI";
            case CARD -> "Card";
            case BANK_TRANSFER -> "Bank Transfer";
        };
    }
}
