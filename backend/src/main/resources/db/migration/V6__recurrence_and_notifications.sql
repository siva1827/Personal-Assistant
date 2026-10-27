ALTER TABLE tasks
    ADD COLUMN recurrence ENUM ('NONE', 'DAILY', 'WEEKLY', 'MONTHLY') NOT NULL DEFAULT 'NONE' AFTER due_date;

CREATE TABLE notifications (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    type        ENUM ('TASK_DUE', 'TASK_OVERDUE', 'BUDGET_NEAR', 'BUDGET_OVER', 'PANTRY_EXPIRING') NOT NULL,
    title       VARCHAR(120) NOT NULL,
    body        VARCHAR(300) NOT NULL,
    link        VARCHAR(100) NULL DEFAULT NULL,
    -- one notification per event: the same key is never created twice for a user
    dedupe_key  VARCHAR(120) NOT NULL,
    read_at     TIMESTAMP    NULL DEFAULT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_notifications_user_key (user_id, dedupe_key),
    KEY idx_notifications_user_created (user_id, created_at),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
