-- How each transaction was paid: Cash, UPI, Card or Bank Transfer.
-- NULL for records made before this column existed. Recurring rules pass theirs on to the transactions they post.
ALTER TABLE transactions ADD COLUMN payment_method ENUM('CASH','UPI','CARD','BANK_TRANSFER') NULL;
ALTER TABLE recurring_expenses ADD COLUMN payment_method ENUM('CASH','UPI','CARD','BANK_TRANSFER') NULL;
