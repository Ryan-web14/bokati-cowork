CREATE TABLE IF NOT EXISTS pass_plan_price (
    id                    BIGINT          PRIMARY KEY,
    pass_plan_version_id  BIGINT          NOT NULL REFERENCES pass_plan_version(id),
    currency              VARCHAR(3)      NOT NULL,
    amount                NUMERIC(19,4)   NOT NULL,
    setup_fee             NUMERIC(19,4)   NOT NULL DEFAULT 0,
    deposit_amount        NUMERIC(19,4)   NOT NULL DEFAULT 0,
    tax_included          BOOLEAN         NOT NULL DEFAULT TRUE,
    tax_code              VARCHAR(80),
    created_at            TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_pass_plan_price_version_currency UNIQUE (pass_plan_version_id, currency),
    CONSTRAINT ck_pass_plan_price_amounts CHECK (amount >= 0 AND setup_fee >= 0 AND deposit_amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_pass_plan_price_version ON pass_plan_price(pass_plan_version_id);

CREATE TABLE IF NOT EXISTS pass_plan_entitlement (
    id                          BIGINT          PRIMARY KEY,
    pass_plan_version_id        BIGINT          NOT NULL REFERENCES pass_plan_version(id),
    entitlement_definition_id   BIGINT          NOT NULL REFERENCES entitlement_definition(id),
    quantity                    NUMERIC(19,4),
    unlimited                   BOOLEAN         NOT NULL DEFAULT FALSE,
    rollover_allowed            BOOLEAN         NOT NULL DEFAULT FALSE,
    rollover_limit              NUMERIC(19,4),
    valid_for_days              INTEGER,
    priority                    INTEGER         NOT NULL DEFAULT 100,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_pass_plan_ent_qty CHECK (unlimited = TRUE OR quantity IS NOT NULL),
    CONSTRAINT ck_pass_plan_ent_non_neg CHECK (quantity IS NULL OR quantity >= 0)
);

CREATE INDEX IF NOT EXISTS idx_pass_plan_ent_version    ON pass_plan_entitlement(pass_plan_version_id);
CREATE INDEX IF NOT EXISTS idx_pass_plan_ent_definition ON pass_plan_entitlement(entitlement_definition_id);
