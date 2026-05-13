CREATE TABLE IF NOT EXISTS subscription_overage_policy (
    id BIGINT PRIMARY KEY,
    plan_version_id BIGINT NOT NULL,
    entitlement_definition_id BIGINT NOT NULL,
    mode VARCHAR(40) NOT NULL,
    unit_price NUMERIC(19,4),
    currency VARCHAR(3),
    free_quantity NUMERIC(19,4) NOT NULL DEFAULT 0,
    metadata_json JSONB,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_overage_policy_plan_version FOREIGN KEY (plan_version_id) REFERENCES subscription_plan_version(id),
    CONSTRAINT fk_overage_policy_entitlement FOREIGN KEY (entitlement_definition_id) REFERENCES entitlement_definition(id),
    CONSTRAINT ck_overage_policy_free_quantity CHECK (free_quantity >= 0),
    CONSTRAINT ck_overage_policy_price CHECK (unit_price IS NULL OR unit_price >= 0)
);

CREATE INDEX IF NOT EXISTS idx_subscription_overage_policy_plan ON subscription_overage_policy(plan_version_id, entitlement_definition_id);
CREATE INDEX IF NOT EXISTS idx_subscription_overage_policy_mode ON subscription_overage_policy(mode, active);

CREATE TABLE IF NOT EXISTS subscription_overage_charge (
    id BIGINT PRIMARY KEY,
    charge_number VARCHAR(100) NOT NULL UNIQUE,
    subscription_id BIGINT,
    usage_record_id BIGINT NOT NULL,
    billable_item_id BIGINT,
    owner_type VARCHAR(60) NOT NULL,
    owner_code VARCHAR(120) NOT NULL,
    entitlement_code VARCHAR(100) NOT NULL,
    overage_quantity NUMERIC(19,4) NOT NULL,
    unit_price NUMERIC(19,4) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_overage_charge_subscription FOREIGN KEY (subscription_id) REFERENCES subscription(id),
    CONSTRAINT fk_overage_charge_usage FOREIGN KEY (usage_record_id) REFERENCES usage_record(id),
    CONSTRAINT fk_overage_charge_billable_item FOREIGN KEY (billable_item_id) REFERENCES billable_item(id),
    CONSTRAINT ck_overage_charge_amounts CHECK (overage_quantity > 0 AND unit_price >= 0 AND amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_subscription_overage_charge_subscription ON subscription_overage_charge(subscription_id, created_at);
CREATE INDEX IF NOT EXISTS idx_subscription_overage_charge_owner ON subscription_overage_charge(owner_type, owner_code, created_at);
CREATE INDEX IF NOT EXISTS idx_subscription_overage_charge_usage ON subscription_overage_charge(usage_record_id);

CREATE TABLE IF NOT EXISTS subscription_rollover_record (
    id BIGINT PRIMARY KEY,
    rollover_number VARCHAR(100) NOT NULL UNIQUE,
    subscription_id BIGINT NOT NULL,
    source_grant_id BIGINT NOT NULL,
    rollover_grant_id BIGINT NOT NULL,
    entitlement_code VARCHAR(100) NOT NULL,
    quantity NUMERIC(19,4) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_rollover_subscription FOREIGN KEY (subscription_id) REFERENCES subscription(id),
    CONSTRAINT fk_rollover_source_grant FOREIGN KEY (source_grant_id) REFERENCES entitlement_grant(id),
    CONSTRAINT fk_rollover_new_grant FOREIGN KEY (rollover_grant_id) REFERENCES entitlement_grant(id),
    CONSTRAINT ck_rollover_quantity CHECK (quantity > 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_subscription_rollover_source ON subscription_rollover_record(source_grant_id);
CREATE INDEX IF NOT EXISTS idx_subscription_rollover_subscription ON subscription_rollover_record(subscription_id, created_at);
CREATE INDEX IF NOT EXISTS idx_subscription_rollover_grant ON subscription_rollover_record(rollover_grant_id);
