-- Account Aggregator (bank connect). A consent is the user's permission for the app to read
-- their bank data for a limited purpose and period; it can be revoked at any time.
CREATE TABLE aa_consents (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL,
    provider      VARCHAR(30)  NOT NULL,
    provider_ref  VARCHAR(100) NOT NULL,
    status        ENUM ('PENDING', 'ACTIVE', 'REJECTED', 'REVOKED', 'EXPIRED') NOT NULL,
    purpose       VARCHAR(100) NOT NULL,
    data_from     DATE         NOT NULL,
    data_to       DATE         NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_at   TIMESTAMP    NULL DEFAULT NULL,
    expires_at    TIMESTAMP    NULL DEFAULT NULL,
    last_synced_at TIMESTAMP   NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_aa_consents_provider_ref (provider, provider_ref),
    KEY idx_aa_consents_user (user_id, status),
    CONSTRAINT fk_aa_consents_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Accounts discovered through a consent. Only a masked number is kept, never the full one.
CREATE TABLE bank_accounts (
    id             BIGINT         NOT NULL AUTO_INCREMENT,
    user_id        BIGINT         NOT NULL,
    consent_id     BIGINT         NOT NULL,
    provider_ref   VARCHAR(100)   NOT NULL,
    bank_name      VARCHAR(100)   NOT NULL,
    masked_number  VARCHAR(20)    NOT NULL,
    account_type   VARCHAR(30)    NOT NULL,
    balance        DECIMAL(14, 2) NULL DEFAULT NULL,
    balance_at     TIMESTAMP      NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_bank_accounts_consent_ref (consent_id, provider_ref),
    KEY idx_bank_accounts_user (user_id),
    CONSTRAINT fk_bank_accounts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_bank_accounts_consent FOREIGN KEY (consent_id) REFERENCES aa_consents (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
