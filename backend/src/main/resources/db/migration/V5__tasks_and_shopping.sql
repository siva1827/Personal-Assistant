CREATE TABLE tasks (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    user_id           BIGINT       NOT NULL,
    title             VARCHAR(200) NOT NULL,
    due_date          DATE         NULL DEFAULT NULL,
    -- used by push reminders in a later step
    reminder_at       TIMESTAMP    NULL DEFAULT NULL,
    status            ENUM ('OPEN', 'DONE') NOT NULL DEFAULT 'OPEN',
    linked_recipe_id  BIGINT       NULL DEFAULT NULL,
    completed_at      TIMESTAMP    NULL DEFAULT NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_tasks_user_status_due (user_id, status, due_date),
    CONSTRAINT fk_tasks_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_tasks_recipe FOREIGN KEY (linked_recipe_id) REFERENCES recipes (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE shopping_list_items (
    id                BIGINT         NOT NULL AUTO_INCREMENT,
    user_id           BIGINT         NOT NULL,
    name              VARCHAR(100)   NOT NULL,
    estimated_cost    DECIMAL(10, 2) NULL DEFAULT NULL,
    purchased         BOOLEAN        NOT NULL DEFAULT FALSE,
    source_recipe_id  BIGINT         NULL DEFAULT NULL,
    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_shopping_user_purchased (user_id, purchased),
    CONSTRAINT fk_shopping_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_shopping_recipe FOREIGN KEY (source_recipe_id) REFERENCES recipes (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
