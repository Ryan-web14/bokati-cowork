ALTER TABLE member_profile
    ADD COLUMN IF NOT EXISTS company_role VARCHAR(120);
