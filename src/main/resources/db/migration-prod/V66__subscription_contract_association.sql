ALTER TABLE subscription
    ADD COLUMN IF NOT EXISTS contract_code VARCHAR(120);

ALTER TABLE subscription_pass
    ADD COLUMN IF NOT EXISTS contract_code VARCHAR(120);

ALTER TABLE subscription_addon
    ADD COLUMN IF NOT EXISTS contract_code VARCHAR(120);

CREATE INDEX IF NOT EXISTS idx_subscription_contract_code
    ON subscription(contract_code);

CREATE INDEX IF NOT EXISTS idx_subscription_pass_contract_code
    ON subscription_pass(contract_code);

CREATE INDEX IF NOT EXISTS idx_subscription_addon_contract_code
    ON subscription_addon(contract_code);
