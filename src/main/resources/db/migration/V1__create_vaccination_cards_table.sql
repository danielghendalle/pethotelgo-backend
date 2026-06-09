-- Migration: Create vaccination_cards table
-- Enable pgcrypto extension for gen_random_uuid() if not already enabled
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS vaccination_cards (
    id               VARCHAR(36)  PRIMARY KEY,
    pet_id           VARCHAR(36)  NOT NULL,
    google_drive_file_id VARCHAR(255) NOT NULL UNIQUE,
    google_drive_url TEXT         NOT NULL,
    file_name        VARCHAR(255) NOT NULL,
    file_size        BIGINT,
    file_type        VARCHAR(100),
    uploaded_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_vaccination_cards_pet_id
    ON vaccination_cards (pet_id);

