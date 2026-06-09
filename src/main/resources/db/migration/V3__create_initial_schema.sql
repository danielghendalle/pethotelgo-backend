CREATE TABLE IF NOT EXISTS users (
    id            VARCHAR(36)  PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    name          VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(50)  NOT NULL DEFAULT 'USER',
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS owners (
    id         VARCHAR(36)  PRIMARY KEY,
    name       VARCHAR(255) NOT NULL DEFAULT '',
    phone      VARCHAR(50)  NOT NULL DEFAULT '',
    email      VARCHAR(255) NOT NULL DEFAULT '',
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS pets (
    id                   VARCHAR(36)  PRIMARY KEY,
    owner_id             VARCHAR(36),
    name                 VARCHAR(255) NOT NULL DEFAULT '',
    breed                VARCHAR(255) NOT NULL DEFAULT '',
    size                 VARCHAR(50)  NOT NULL DEFAULT 'medio',
    needs_separate_space BOOLEAN      NOT NULL DEFAULT FALSE,
    sociability          VARCHAR(50)  NOT NULL DEFAULT 'media',
    allergies            TEXT         NOT NULL DEFAULT '',
    special_care         TEXT         NOT NULL DEFAULT '',
    feeding_schedule     VARCHAR(255) NOT NULL DEFAULT '',
    feeding_amount       VARCHAR(255) NOT NULL DEFAULT '',
    vaccination_card_url TEXT,
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pets_owner FOREIGN KEY (owner_id) REFERENCES owners(id)
);

CREATE TABLE IF NOT EXISTS reservations (
    id                  VARCHAR(36)    PRIMARY KEY,
    pet_id              VARCHAR(36),
    owner_id            VARCHAR(36),
    check_in            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    check_out           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status              VARCHAR(50)    NOT NULL DEFAULT 'pending',
    notes               TEXT           NOT NULL DEFAULT '',
    daily_rate          DECIMAL(10,2),
    discount_percentage DECIMAL(5,2),
    created_at          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reservations_pet   FOREIGN KEY (pet_id)   REFERENCES pets(id),
    CONSTRAINT fk_reservations_owner FOREIGN KEY (owner_id) REFERENCES owners(id)
);

CREATE TABLE IF NOT EXISTS stay_histories (
    id             VARCHAR(36) PRIMARY KEY,
    pet_id         VARCHAR(36),
    reservation_id VARCHAR(36),
    check_in       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    check_out      TIMESTAMP,
    behavior       TEXT        NOT NULL DEFAULT '',
    notes          TEXT        NOT NULL DEFAULT '',
    CONSTRAINT fk_stay_histories_pet         FOREIGN KEY (pet_id)         REFERENCES pets(id),
    CONSTRAINT fk_stay_histories_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id)
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id         VARCHAR(36)   PRIMARY KEY,
    user_id    VARCHAR(36),
    token      VARCHAR(1000),
    token_hash VARCHAR(1000),
    expires_at TIMESTAMP     NOT NULL,
    created_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP,
    CONSTRAINT uq_refresh_tokens_token      UNIQUE (token),
    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user       FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_token_hash ON refresh_tokens(token_hash);