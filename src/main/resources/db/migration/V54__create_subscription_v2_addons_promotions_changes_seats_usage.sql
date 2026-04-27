CREATE TABLE IF NOT EXISTS subscription_addon (
    id BIGINT PRIMARY KEY,
    subscription_id BIGINT NOT NULL,
    plan_version_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    unit_price NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(40) NOT NULL,
    starts_at DATE NOT NULL,
    ends_at DATE,
    metadata_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_subscription_addon_subscription FOREIGN KEY (subscription_id) REFERENCES subscription(id),
    CONSTRAINT fk_subscription_addon_plan_version FOREIGN KEY (plan_version_id) REFERENCES subscription_plan_version(id),
    CONSTRAINT ck_subscription_addon_quantity CHECK (quantity > 0),
    CONSTRAINT ck_subscription_addon_unit_price CHECK (unit_price >= 0)
);

CREATE INDEX IF NOT EXISTS idx_subscription_addon_subscription ON subscription_addon(subscription_id, status);
CREATE INDEX IF NOT EXISTS idx_subscription_addon_plan_version ON subscription_addon(plan_version_id);

CREATE TABLE IF NOT EXISTS subscription_change_request (
    id BIGINT PRIMARY KEY,
    change_number VARCHAR(100) NOT NULL UNIQUE,
    subscription_id BIGINT NOT NULL,
    change_type VARCHAR(60) NOT NULL,
    current_plan_version_id BIGINT,
    target_plan_version_id BIGINT,
    effective_policy VARCHAR(60) NOT NULL,
    effective_date DATE,
    proration_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    status VARCHAR(40) NOT NULL,
    reason TEXT,
    requested_by VARCHAR(120),
    approved_by VARCHAR(120),
    applied_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_subscription_change_subscription FOREIGN KEY (subscription_id) REFERENCES subscription(id),
    CONSTRAINT fk_subscription_change_current_plan FOREIGN KEY (current_plan_version_id) REFERENCES subscription_plan_version(id),
    CONSTRAINT fk_subscription_change_target_plan FOREIGN KEY (target_plan_version_id) REFERENCES subscription_plan_version(id),
    CONSTRAINT ck_subscription_change_proration CHECK (proration_amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_subscription_change_subscription ON subscription_change_request(subscription_id, status);
CREATE INDEX IF NOT EXISTS idx_subscription_change_effective ON subscription_change_request(effective_policy, effective_date, status);

CREATE TABLE IF NOT EXISTS subscription_seat (
    id BIGINT PRIMARY KEY,
    subscription_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    member_code VARCHAR(120) NOT NULL,
    role VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    invited_at TIMESTAMPTZ,
    activated_at TIMESTAMPTZ,
    removed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_subscription_seat_subscription FOREIGN KEY (subscription_id) REFERENCES subscription(id),
    CONSTRAINT fk_subscription_seat_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE INDEX IF NOT EXISTS idx_subscription_seat_subscription ON subscription_seat(subscription_id, status);
CREATE INDEX IF NOT EXISTS idx_subscription_seat_member ON subscription_seat(member_id, status);
CREATE UNIQUE INDEX IF NOT EXISTS uk_subscription_seat_active_member
    ON subscription_seat(subscription_id, member_code)
    WHERE status <> 'REMOVED';

CREATE TABLE IF NOT EXISTS promotion (
    id BIGINT PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    discount_type VARCHAR(60) NOT NULL,
    discount_value NUMERIC(19,4) NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ,
    max_redemptions INTEGER,
    redemption_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(40) NOT NULL,
    metadata_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_promotion_discount_value CHECK (discount_value >= 0),
    CONSTRAINT ck_promotion_redemptions CHECK (redemption_count >= 0 AND (max_redemptions IS NULL OR max_redemptions >= 0))
);

CREATE INDEX IF NOT EXISTS idx_promotion_code ON promotion(code);
CREATE INDEX IF NOT EXISTS idx_promotion_status_dates ON promotion(status, starts_at, ends_at);

CREATE TABLE IF NOT EXISTS coupon_redemption (
    id BIGINT PRIMARY KEY,
    promotion_id BIGINT NOT NULL,
    subscriber_type VARCHAR(60) NOT NULL,
    subscriber_code VARCHAR(120) NOT NULL,
    subscription_id BIGINT,
    redeemed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_coupon_redemption_promotion FOREIGN KEY (promotion_id) REFERENCES promotion(id),
    CONSTRAINT fk_coupon_redemption_subscription FOREIGN KEY (subscription_id) REFERENCES subscription(id)
);

CREATE INDEX IF NOT EXISTS idx_coupon_redemption_promotion ON coupon_redemption(promotion_id);
CREATE INDEX IF NOT EXISTS idx_coupon_redemption_subscriber ON coupon_redemption(subscriber_type, subscriber_code);
CREATE UNIQUE INDEX IF NOT EXISTS uk_coupon_redemption_once_per_subscriber
    ON coupon_redemption(promotion_id, subscriber_type, subscriber_code);

CREATE TABLE IF NOT EXISTS usage_record (
    id BIGINT PRIMARY KEY,
    usage_number VARCHAR(100) NOT NULL UNIQUE,
    owner_type VARCHAR(60) NOT NULL,
    owner_code VARCHAR(120) NOT NULL,
    entitlement_code VARCHAR(100) NOT NULL,
    quantity NUMERIC(19,4) NOT NULL,
    unit VARCHAR(40) NOT NULL,
    reference_type VARCHAR(80) NOT NULL,
    reference_id VARCHAR(120) NOT NULL,
    billable BOOLEAN NOT NULL DEFAULT FALSE,
    billable_item_id BIGINT,
    status VARCHAR(40) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    metadata_json JSONB,
    CONSTRAINT fk_usage_record_billable_item FOREIGN KEY (billable_item_id) REFERENCES billable_item(id),
    CONSTRAINT ck_usage_record_quantity CHECK (quantity > 0)
);

CREATE INDEX IF NOT EXISTS idx_usage_record_owner ON usage_record(owner_type, owner_code, status);
CREATE INDEX IF NOT EXISTS idx_usage_record_reference ON usage_record(reference_type, reference_id);
CREATE INDEX IF NOT EXISTS idx_usage_record_entitlement ON usage_record(entitlement_code);
