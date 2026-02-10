-- Flyway migration V1: add token_hash column and populate from token
-- This migration:
-- 1) creates the pgcrypto extension if not exists (provides digest function)
-- 2) adds column token_hash
-- 3) populates token_hash = encode(digest(token, 'sha256'), 'hex') for existing rows where token is not null
-- 4) creates unique index on token_hash

CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE IF EXISTS refresh_tokens ADD COLUMN IF NOT EXISTS token_hash varchar(128);

-- Populate token_hash for existing tokens (if any)
UPDATE refresh_tokens
SET token_hash = encode(digest(token::bytea, 'sha256'), 'hex')
WHERE token IS NOT NULL AND (token_hash IS NULL OR token_hash = '');

-- Create unique index for fast lookup
CREATE UNIQUE INDEX IF NOT EXISTS idx_refresh_tokens_token_hash ON refresh_tokens (token_hash);

-- Optionally keep token column for backward compatibility; consider dropping later

