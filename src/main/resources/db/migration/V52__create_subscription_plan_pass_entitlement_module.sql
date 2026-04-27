CREATE TABLE IF NOT EXISTS subscription_plan (
    id BIGINT PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    plan_type VARCHAR(60) NOT NULL,
    target_audience VARCHAR(60) NOT NULL,
    status VARCHAR(40) NOT NULL,
    visible BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_subscription_plan_code ON subscription_plan(code);
CREATE INDEX IF NOT EXISTS idx_subscription_plan_status ON subscription_plan(status);

CREATE TABLE IF NOT EXISTS subscription_plan_version (
    id BIGINT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    version_number INTEGER NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(40) NOT NULL,
    effective_from DATE,
    effective_to DATE,
    terms_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_plan_version_plan FOREIGN KEY (plan_id) REFERENCES subscription_plan(id),
    CONSTRAINT uk_plan_version_number UNIQUE (plan_id, version_number)
);

CREATE INDEX IF NOT EXISTS idx_plan_version_plan ON subscription_plan_version(plan_id);
CREATE INDEX IF NOT EXISTS idx_plan_version_status ON subscription_plan_version(status);

CREATE TABLE IF NOT EXISTS subscription_plan_price (
    id BIGINT PRIMARY KEY,
    plan_version_id BIGINT NOT NULL,
    billing_cycle VARCHAR(40) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    setup_fee NUMERIC(19,4) NOT NULL DEFAULT 0,
    deposit_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    tax_included BOOLEAN NOT NULL DEFAULT TRUE,
    trial_days INTEGER NOT NULL DEFAULT 0,
    commitment_months INTEGER NOT NULL DEFAULT 0,
    tax_code VARCHAR(80),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_plan_price_version FOREIGN KEY (plan_version_id) REFERENCES subscription_plan_version(id),
    CONSTRAINT ck_plan_price_amount CHECK (amount >= 0 AND setup_fee >= 0 AND deposit_amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_plan_price_version ON subscription_plan_price(plan_version_id);

CREATE TABLE IF NOT EXISTS subscription_plan_benefit (
    id BIGINT PRIMARY KEY,
    plan_version_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    icon VARCHAR(100),
    category VARCHAR(60) NOT NULL,
    display_order INTEGER,
    highlighted BOOLEAN NOT NULL DEFAULT FALSE,
    included BOOLEAN NOT NULL DEFAULT TRUE,
    metadata_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_plan_benefit_version FOREIGN KEY (plan_version_id) REFERENCES subscription_plan_version(id)
);

CREATE INDEX IF NOT EXISTS idx_plan_benefit_version ON subscription_plan_benefit(plan_version_id);

CREATE TABLE IF NOT EXISTS entitlement_definition (
    id BIGINT PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    entitlement_type VARCHAR(60) NOT NULL,
    unit VARCHAR(40) NOT NULL,
    consumption_mode VARCHAR(60) NOT NULL,
    reset_policy VARCHAR(60) NOT NULL,
    stackable BOOLEAN NOT NULL DEFAULT TRUE,
    transferable BOOLEAN NOT NULL DEFAULT FALSE,
    resource_type_id BIGINT,
    resource_group_id BIGINT,
    metadata_json JSONB,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_entitlement_resource_type FOREIGN KEY (resource_type_id) REFERENCES resource_type(id),
    CONSTRAINT fk_entitlement_resource_group FOREIGN KEY (resource_group_id) REFERENCES resource_group(id)
);

CREATE INDEX IF NOT EXISTS idx_entitlement_definition_code ON entitlement_definition(code);

CREATE TABLE IF NOT EXISTS subscription_plan_entitlement (
    id BIGINT PRIMARY KEY,
    plan_version_id BIGINT NOT NULL,
    entitlement_definition_id BIGINT NOT NULL,
    quantity NUMERIC(19,4),
    unlimited BOOLEAN NOT NULL DEFAULT FALSE,
    rollover_allowed BOOLEAN NOT NULL DEFAULT FALSE,
    rollover_limit NUMERIC(19,4),
    valid_for_days INTEGER,
    priority INTEGER NOT NULL DEFAULT 100,
    restrictions_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_plan_entitlement_version FOREIGN KEY (plan_version_id) REFERENCES subscription_plan_version(id),
    CONSTRAINT fk_plan_entitlement_definition FOREIGN KEY (entitlement_definition_id) REFERENCES entitlement_definition(id),
    CONSTRAINT ck_plan_entitlement_quantity CHECK (unlimited = TRUE OR quantity IS NOT NULL),
    CONSTRAINT ck_plan_entitlement_non_negative CHECK (quantity IS NULL OR quantity >= 0)
);

CREATE INDEX IF NOT EXISTS idx_plan_entitlement_version ON subscription_plan_entitlement(plan_version_id);
CREATE INDEX IF NOT EXISTS idx_plan_entitlement_definition ON subscription_plan_entitlement(entitlement_definition_id);

CREATE TABLE IF NOT EXISTS subscription (
    id BIGINT PRIMARY KEY,
    subscription_number VARCHAR(100) NOT NULL UNIQUE,
    subscriber_type VARCHAR(60) NOT NULL,
    subscriber_code VARCHAR(120) NOT NULL,
    member_id BIGINT,
    customer_id BIGINT,
    business_entity_id BIGINT,
    plan_version_id BIGINT NOT NULL,
    status VARCHAR(40) NOT NULL,
    start_date DATE NOT NULL,
    current_period_start DATE,
    current_period_end DATE,
    next_billing_date DATE,
    trial_start DATE,
    trial_end DATE,
    auto_renew BOOLEAN NOT NULL DEFAULT TRUE,
    billing_cycle VARCHAR(40) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    subtotal_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    tax_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    total_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
    cancelled_at TIMESTAMPTZ,
    cancellation_reason TEXT,
    suspended_at TIMESTAMPTZ,
    suspension_reason TEXT,
    metadata_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_subscription_member FOREIGN KEY (member_id) REFERENCES member(id),
    CONSTRAINT fk_subscription_customer FOREIGN KEY (customer_id) REFERENCES customer(id),
    CONSTRAINT fk_subscription_business FOREIGN KEY (business_entity_id) REFERENCES business_entity(id),
    CONSTRAINT fk_subscription_plan_version FOREIGN KEY (plan_version_id) REFERENCES subscription_plan_version(id),
    CONSTRAINT ck_subscription_amounts CHECK (subtotal_amount >= 0 AND tax_amount >= 0 AND total_amount >= 0),
    CONSTRAINT ck_subscription_owner CHECK (
        (subscriber_type = 'MEMBER' AND member_id IS NOT NULL AND customer_id IS NULL AND business_entity_id IS NULL)
        OR (subscriber_type = 'CUSTOMER' AND customer_id IS NOT NULL AND member_id IS NULL AND business_entity_id IS NULL)
        OR (subscriber_type = 'BUSINESS_ENTITY' AND business_entity_id IS NOT NULL AND member_id IS NULL AND customer_id IS NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_subscription_number ON subscription(subscription_number);
CREATE INDEX IF NOT EXISTS idx_subscription_status_billing ON subscription(status, next_billing_date);
CREATE INDEX IF NOT EXISTS idx_subscription_member_status ON subscription(member_id, status);
CREATE INDEX IF NOT EXISTS idx_subscription_customer_status ON subscription(customer_id, status);
CREATE INDEX IF NOT EXISTS idx_subscription_business_status ON subscription(business_entity_id, status);

CREATE TABLE IF NOT EXISTS subscription_item (
    id BIGINT PRIMARY KEY,
    subscription_id BIGINT NOT NULL,
    plan_version_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    unit_price NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_subscription_item_subscription FOREIGN KEY (subscription_id) REFERENCES subscription(id),
    CONSTRAINT fk_subscription_item_plan_version FOREIGN KEY (plan_version_id) REFERENCES subscription_plan_version(id)
);

CREATE INDEX IF NOT EXISTS idx_subscription_item_subscription ON subscription_item(subscription_id);

CREATE TABLE IF NOT EXISTS subscription_status_history (
    id BIGINT PRIMARY KEY,
    subscription_id BIGINT NOT NULL,
    from_status VARCHAR(40),
    to_status VARCHAR(40) NOT NULL,
    reason TEXT,
    changed_by VARCHAR(120),
    changed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_subscription_status_history_subscription FOREIGN KEY (subscription_id) REFERENCES subscription(id)
);

CREATE INDEX IF NOT EXISTS idx_subscription_status_history_subscription ON subscription_status_history(subscription_id);

CREATE TABLE IF NOT EXISTS subscription_event (
    id BIGINT PRIMARY KEY,
    subscription_id BIGINT NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    payload_json JSONB,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_subscription_event_subscription FOREIGN KEY (subscription_id) REFERENCES subscription(id)
);

CREATE INDEX IF NOT EXISTS idx_subscription_event_subscription ON subscription_event(subscription_id);
CREATE INDEX IF NOT EXISTS idx_subscription_event_type ON subscription_event(event_type);

CREATE TABLE IF NOT EXISTS billing_schedule (
    id BIGINT PRIMARY KEY,
    subscription_id BIGINT NOT NULL UNIQUE,
    billing_cycle VARCHAR(40) NOT NULL,
    next_billing_date DATE,
    current_period_start DATE,
    current_period_end DATE,
    status VARCHAR(40) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_billing_schedule_subscription FOREIGN KEY (subscription_id) REFERENCES subscription(id),
    CONSTRAINT ck_billing_schedule_retry_count CHECK (retry_count >= 0)
);

CREATE INDEX IF NOT EXISTS idx_billing_schedule_subscription ON billing_schedule(subscription_id);
CREATE INDEX IF NOT EXISTS idx_billing_schedule_next ON billing_schedule(status, next_billing_date);

CREATE TABLE IF NOT EXISTS subscription_pass (
    id BIGINT PRIMARY KEY,
    pass_number VARCHAR(100) NOT NULL UNIQUE,
    pass_type VARCHAR(60) NOT NULL,
    owner_type VARCHAR(60) NOT NULL,
    owner_code VARCHAR(120) NOT NULL,
    subscription_id BIGINT,
    plan_version_id BIGINT,
    status VARCHAR(40) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    valid_from TIMESTAMPTZ NOT NULL,
    valid_until TIMESTAMPTZ,
    transferable BOOLEAN NOT NULL DEFAULT FALSE,
    shareable BOOLEAN NOT NULL DEFAULT FALSE,
    max_uses INTEGER,
    used_count INTEGER NOT NULL DEFAULT 0,
    metadata_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_subscription_pass_subscription FOREIGN KEY (subscription_id) REFERENCES subscription(id),
    CONSTRAINT fk_subscription_pass_plan_version FOREIGN KEY (plan_version_id) REFERENCES subscription_plan_version(id),
    CONSTRAINT ck_subscription_pass_used_count CHECK (used_count >= 0)
);

CREATE INDEX IF NOT EXISTS idx_subscription_pass_number ON subscription_pass(pass_number);
CREATE INDEX IF NOT EXISTS idx_subscription_pass_owner ON subscription_pass(owner_type, owner_code, status);
CREATE INDEX IF NOT EXISTS idx_subscription_pass_valid_until ON subscription_pass(valid_until);

CREATE TABLE IF NOT EXISTS subscription_pass_entitlement (
    id BIGINT PRIMARY KEY,
    pass_id BIGINT NOT NULL,
    entitlement_definition_id BIGINT NOT NULL,
    quantity NUMERIC(19,4),
    unlimited BOOLEAN NOT NULL DEFAULT FALSE,
    valid_from TIMESTAMPTZ NOT NULL,
    valid_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_pass_entitlement_pass FOREIGN KEY (pass_id) REFERENCES subscription_pass(id),
    CONSTRAINT fk_pass_entitlement_definition FOREIGN KEY (entitlement_definition_id) REFERENCES entitlement_definition(id),
    CONSTRAINT ck_pass_entitlement_quantity CHECK (unlimited = TRUE OR quantity IS NOT NULL),
    CONSTRAINT ck_pass_entitlement_non_negative CHECK (quantity IS NULL OR quantity >= 0)
);

CREATE INDEX IF NOT EXISTS idx_pass_entitlement_pass ON subscription_pass_entitlement(pass_id);

CREATE TABLE IF NOT EXISTS entitlement_grant (
    id BIGINT PRIMARY KEY,
    grant_number VARCHAR(100) NOT NULL UNIQUE,
    subscription_id BIGINT,
    pass_id BIGINT,
    entitlement_definition_id BIGINT NOT NULL,
    owner_type VARCHAR(60) NOT NULL,
    owner_code VARCHAR(120) NOT NULL,
    quantity_granted NUMERIC(19,4),
    quantity_remaining NUMERIC(19,4),
    unlimited BOOLEAN NOT NULL DEFAULT FALSE,
    valid_from TIMESTAMPTZ NOT NULL,
    valid_until TIMESTAMPTZ,
    status VARCHAR(40) NOT NULL,
    source_type VARCHAR(80) NOT NULL,
    source_id VARCHAR(120) NOT NULL,
    priority INTEGER NOT NULL DEFAULT 100,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_entitlement_grant_subscription FOREIGN KEY (subscription_id) REFERENCES subscription(id),
    CONSTRAINT fk_entitlement_grant_pass FOREIGN KEY (pass_id) REFERENCES subscription_pass(id),
    CONSTRAINT fk_entitlement_grant_definition FOREIGN KEY (entitlement_definition_id) REFERENCES entitlement_definition(id),
    CONSTRAINT ck_entitlement_grant_quantity CHECK (
        unlimited = TRUE
        OR (quantity_granted IS NOT NULL AND quantity_remaining IS NOT NULL AND quantity_granted >= quantity_remaining AND quantity_remaining >= 0)
    )
);

CREATE INDEX IF NOT EXISTS idx_entitlement_grant_number ON entitlement_grant(grant_number);
CREATE INDEX IF NOT EXISTS idx_entitlement_grant_owner ON entitlement_grant(owner_type, owner_code, status);
CREATE INDEX IF NOT EXISTS idx_entitlement_grant_valid_until ON entitlement_grant(valid_until);
CREATE INDEX IF NOT EXISTS idx_entitlement_grant_subscription ON entitlement_grant(subscription_id);
CREATE INDEX IF NOT EXISTS idx_entitlement_grant_pass ON entitlement_grant(pass_id);

CREATE TABLE IF NOT EXISTS entitlement_reservation (
    id BIGINT PRIMARY KEY,
    grant_id BIGINT NOT NULL,
    owner_type VARCHAR(60) NOT NULL,
    owner_code VARCHAR(120) NOT NULL,
    entitlement_code VARCHAR(100) NOT NULL,
    quantity NUMERIC(19,4) NOT NULL,
    reference_type VARCHAR(80) NOT NULL,
    reference_id VARCHAR(120) NOT NULL,
    status VARCHAR(40) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_entitlement_reservation_grant FOREIGN KEY (grant_id) REFERENCES entitlement_grant(id),
    CONSTRAINT ck_entitlement_reservation_quantity CHECK (quantity > 0)
);

CREATE INDEX IF NOT EXISTS idx_entitlement_reservation_reference ON entitlement_reservation(reference_type, reference_id, status);
CREATE INDEX IF NOT EXISTS idx_entitlement_reservation_owner ON entitlement_reservation(owner_type, owner_code, status);
CREATE INDEX IF NOT EXISTS idx_entitlement_reservation_expires ON entitlement_reservation(expires_at, status);

CREATE TABLE IF NOT EXISTS entitlement_ledger (
    id BIGINT PRIMARY KEY,
    grant_id BIGINT NOT NULL,
    transaction_type VARCHAR(40) NOT NULL,
    quantity NUMERIC(19,4),
    before_quantity NUMERIC(19,4),
    after_quantity NUMERIC(19,4),
    reference_type VARCHAR(80),
    reference_id VARCHAR(120),
    idempotency_key VARCHAR(160),
    reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_entitlement_ledger_grant FOREIGN KEY (grant_id) REFERENCES entitlement_grant(id)
);

CREATE INDEX IF NOT EXISTS idx_entitlement_ledger_grant ON entitlement_ledger(grant_id);
CREATE INDEX IF NOT EXISTS idx_entitlement_ledger_reference ON entitlement_ledger(reference_type, reference_id);
CREATE INDEX IF NOT EXISTS idx_entitlement_ledger_idempotency ON entitlement_ledger(idempotency_key);
CREATE UNIQUE INDEX IF NOT EXISTS uk_entitlement_ledger_idempotency_not_null
    ON entitlement_ledger(idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE TABLE IF NOT EXISTS subscription_pass_transaction (
    id BIGINT PRIMARY KEY,
    pass_id BIGINT NOT NULL,
    transaction_type VARCHAR(60) NOT NULL,
    reference_type VARCHAR(80),
    reference_id VARCHAR(120),
    payload_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_pass_transaction_pass FOREIGN KEY (pass_id) REFERENCES subscription_pass(id)
);

CREATE INDEX IF NOT EXISTS idx_pass_transaction_pass ON subscription_pass_transaction(pass_id);

CREATE TABLE IF NOT EXISTS billable_item (
    id BIGINT PRIMARY KEY,
    billable_number VARCHAR(100) NOT NULL UNIQUE,
    source_type VARCHAR(80) NOT NULL,
    source_id VARCHAR(120) NOT NULL,
    subscriber_type VARCHAR(60) NOT NULL,
    subscriber_code VARCHAR(120) NOT NULL,
    description TEXT NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    tax_code VARCHAR(80),
    billing_period_start DATE,
    billing_period_end DATE,
    status VARCHAR(40) NOT NULL,
    invoice_id BIGINT,
    metadata_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_billable_item_amount CHECK (amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_billable_item_status ON billable_item(status);
CREATE INDEX IF NOT EXISTS idx_billable_item_source ON billable_item(source_type, source_id);
CREATE INDEX IF NOT EXISTS idx_billable_item_subscriber ON billable_item(subscriber_type, subscriber_code);
