-- Family Expense Tracker: MySQL 8 schema (draft, Step 1)
-- Every financial row carries user_id. Child rows reference categories through
-- (category_id, user_id) so a record can never point at another user's category.

CREATE TABLE users (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(100) NOT NULL,          -- BCrypt
    display_name   VARCHAR(100) NOT NULL,
    currency       CHAR(3)      NOT NULL DEFAULT 'INR',
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE categories (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    name       VARCHAR(60)  NOT NULL,
    type       ENUM('INCOME','EXPENSE') NOT NULL,
    color      CHAR(7)      NULL,                  -- hex, for charts
    archived   BOOLEAN      NOT NULL DEFAULT FALSE,-- hide instead of delete when in use
    CONSTRAINT fk_cat_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_cat_name UNIQUE (user_id, type, name),
    CONSTRAINT uq_cat_owner UNIQUE (id, user_id)   -- target for ownership-safe FKs
) ENGINE=InnoDB;

CREATE TABLE recurring_expenses (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT        NOT NULL,
    category_id    BIGINT        NOT NULL,
    amount         DECIMAL(12,2) NOT NULL CHECK (amount > 0),
    description    VARCHAR(255)  NOT NULL,
    frequency      ENUM('WEEKLY','MONTHLY','YEARLY') NOT NULL,
    start_date     DATE          NOT NULL,
    end_date       DATE          NULL,
    next_run_date  DATE          NOT NULL,         -- next occurrence still to be posted
    active         BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_rec_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_rec_cat  FOREIGN KEY (category_id, user_id) REFERENCES categories(id, user_id),
    INDEX ix_rec_due (active, next_run_date)
) ENGINE=InnoDB;

CREATE TABLE transactions (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT        NOT NULL,
    category_id   BIGINT        NOT NULL,
    type          ENUM('INCOME','EXPENSE') NOT NULL, -- must match the category's type (checked in service layer)
    amount        DECIMAL(12,2) NOT NULL CHECK (amount > 0),
    txn_date      DATE          NOT NULL,
    note          VARCHAR(255)  NULL,
    recurring_id  BIGINT        NULL,              -- set when posted by a recurring rule
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_txn_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_txn_cat  FOREIGN KEY (category_id, user_id) REFERENCES categories(id, user_id),
    CONSTRAINT fk_txn_rec  FOREIGN KEY (recurring_id) REFERENCES recurring_expenses(id) ON DELETE SET NULL,
    CONSTRAINT uq_txn_rec_date UNIQUE (recurring_id, txn_date), -- a rule never posts the same day twice
    INDEX ix_txn_user_date (user_id, txn_date),
    INDEX ix_txn_user_cat  (user_id, category_id, txn_date)
) ENGINE=InnoDB;

CREATE TABLE budgets (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT        NOT NULL,
    category_id   BIGINT        NULL,              -- NULL = overall monthly budget
    period_month  DATE          NOT NULL,          -- always the 1st of the month
    amount        DECIMAL(12,2) NOT NULL CHECK (amount > 0),
    CONSTRAINT fk_bud_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_bud_cat  FOREIGN KEY (category_id, user_id) REFERENCES categories(id, user_id),
    CONSTRAINT uq_bud UNIQUE (user_id, category_id, period_month),
    INDEX ix_bud_user_month (user_id, period_month)
) ENGINE=InnoDB;
-- Note: MySQL treats NULLs as distinct in UNIQUE, so the service layer also
-- guarantees one overall (category_id NULL) budget per user per month.
