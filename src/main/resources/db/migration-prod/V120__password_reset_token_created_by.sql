ALTER TABLE password_reset_token
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);
