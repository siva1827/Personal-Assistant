CREATE TABLE categories (
    id       BIGINT      NOT NULL AUTO_INCREMENT,
    user_id  BIGINT      NOT NULL,
    name     VARCHAR(50) NOT NULL,
    emoji    VARCHAR(16),
    PRIMARY KEY (id),
    UNIQUE KEY uk_categories_user_name (user_id, name),
    CONSTRAINT fk_categories_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE expenses (
    id          BIGINT         NOT NULL AUTO_INCREMENT,
    user_id     BIGINT         NOT NULL,
    category_id BIGINT         NOT NULL,
    amount      DECIMAL(12, 2) NOT NULL,
    note        VARCHAR(200),
    spent_on    DATE           NOT NULL,
    source      ENUM ('MANUAL', 'SMS', 'BANK_SYNC') NOT NULL DEFAULT 'MANUAL',
    created_at  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_expenses_user_date (user_id, spent_on),
    KEY idx_expenses_category (category_id),
    CONSTRAINT fk_expenses_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    -- a category in use cannot be deleted
    CONSTRAINT fk_expenses_category FOREIGN KEY (category_id) REFERENCES categories (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE budgets (
    id           BIGINT         NOT NULL AUTO_INCREMENT,
    user_id      BIGINT         NOT NULL,
    category_id  BIGINT         NOT NULL,
    month        DATE           NOT NULL COMMENT 'first day of the month',
    limit_amount DECIMAL(12, 2) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_budgets_user_category_month (user_id, category_id, month),
    CONSTRAINT fk_budgets_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_budgets_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
