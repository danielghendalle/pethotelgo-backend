CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS vaccination_cards (
    id          VARCHAR(36)  PRIMARY KEY,
    pet_id      VARCHAR(36)  NOT NULL,
    file_name   VARCHAR(255) NOT NULL,
    file_type   VARCHAR(100),
    file_data   TEXT         NOT NULL,
    file_size   BIGINT,
    uploaded_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_vaccination_cards_pet_id ON vaccination_cards (pet_id);