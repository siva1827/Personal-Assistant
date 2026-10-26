CREATE TABLE users (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    email               VARCHAR(255) NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,
    display_name        VARCHAR(100),
    dietary_preference  ENUM ('ANY', 'VEG', 'NON_VEG', 'VEGAN', 'EGGETARIAN') NOT NULL DEFAULT 'ANY',
    allergies           JSON,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
) ENGINE = InnoDB;

CREATE TABLE ingredients (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(100) NOT NULL,
    category   VARCHAR(50),
    is_staple  BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ingredients_name (name)
) ENGINE = InnoDB;

CREATE TABLE pantry_items (
    id             BIGINT         NOT NULL AUTO_INCREMENT,
    user_id        BIGINT         NOT NULL,
    ingredient_id  BIGINT         NOT NULL,
    quantity       DECIMAL(10, 2),
    unit           VARCHAR(20),
    expiry_date    DATE,
    added_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_pantry_user_ingredient (user_id, ingredient_id),
    KEY idx_pantry_expiry (user_id, expiry_date),
    CONSTRAINT fk_pantry_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_pantry_ingredient FOREIGN KEY (ingredient_id) REFERENCES ingredients (id)
) ENGINE = InnoDB;

CREATE TABLE day_themes (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    code         VARCHAR(50)  NOT NULL,
    label        VARCHAR(100) NOT NULL,
    prompt_hint  VARCHAR(500) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_day_themes_code (code)
) ENGINE = InnoDB;

CREATE TABLE recipe_requests (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT       NOT NULL,
    theme_id         BIGINT,
    custom_note      VARCHAR(300),
    pantry_snapshot  JSON         NOT NULL,
    model_name       VARCHAR(100) NOT NULL,
    tokens_used      INT,
    status           ENUM ('SUCCESS', 'INVALID_RESPONSE', 'FAILED') NOT NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_requests_user_created (user_id, created_at),
    CONSTRAINT fk_requests_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_requests_theme FOREIGN KEY (theme_id) REFERENCES day_themes (id)
) ENGINE = InnoDB;

CREATE TABLE recipes (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    user_id              BIGINT       NOT NULL,
    request_id           BIGINT       NOT NULL,
    title                VARCHAR(200) NOT NULL,
    description          TEXT,
    cook_time_minutes    INT,
    servings             INT,
    cuisine              VARCHAR(100),
    dietary_type         ENUM ('ANY', 'VEG', 'NON_VEG', 'VEGAN', 'EGGETARIAN') NOT NULL DEFAULT 'ANY',
    ingredients          JSON         NOT NULL,
    missing_ingredients  JSON         NOT NULL,
    steps                JSON         NOT NULL,
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_recipes_user_created (user_id, created_at),
    CONSTRAINT fk_recipes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_recipes_request FOREIGN KEY (request_id) REFERENCES recipe_requests (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE favorite_recipes (
    user_id    BIGINT    NOT NULL,
    recipe_id  BIGINT    NOT NULL,
    saved_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, recipe_id),
    CONSTRAINT fk_favorites_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_favorites_recipe FOREIGN KEY (recipe_id) REFERENCES recipes (id) ON DELETE CASCADE
) ENGINE = InnoDB;
