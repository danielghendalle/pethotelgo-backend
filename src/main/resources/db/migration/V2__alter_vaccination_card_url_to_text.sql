-- Migration: Alter vaccination_card_url column to TEXT for base64 support
ALTER TABLE pets
MODIFY COLUMN vaccination_card_url TEXT;

