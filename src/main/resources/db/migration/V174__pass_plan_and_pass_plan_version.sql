CREATE TABLE IF NOT EXISTS pass_plan (
    id                  BIGINT          PRIMARY KEY,
    code                VARCHAR(80)     NOT NULL UNIQUE,
    name                VARCHAR(255)    NOT NULL,
    description         TEXT,
    pass_type           VARCHAR(60)     NOT NULL,
    target_audience     VARCHAR(60)     NOT NULL,
    status              VARCHAR(40)     NOT NULL DEFAULT 'DRAFT',
    visible             BOOLEAN         NOT NULL DEFAULT FALSE,
    sort_order          INTEGER,
    required_kyc_level  INTEGER         NOT NULL DEFAULT 1,
    deleted             BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pass_plan_code   ON pass_plan(code)   WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_pass_plan_status ON pass_plan(status) WHERE deleted = FALSE;

CREATE TABLE IF NOT EXISTS pass_plan_version (
    id                  BIGINT          PRIMARY KEY,
    plan_id             BIGINT          NOT NULL REFERENCES pass_plan(id),
    version_number      INTEGER         NOT NULL,
    name                VARCHAR(255)    NOT NULL,
    description         TEXT,
    status              VARCHAR(40)     NOT NULL DEFAULT 'DRAFT',
    duration            INTEGER         NOT NULL,
    duration_unit       VARCHAR(20)     NOT NULL,
    max_uses            INTEGER,
    auto_renewable      BOOLEAN         NOT NULL DEFAULT FALSE,
    required_kyc_level  INTEGER         NOT NULL DEFAULT 1,
    effective_from      DATE,
    effective_to        DATE,
    terms_json          JSONB,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_pass_plan_version_number UNIQUE (plan_id, version_number),
    CONSTRAINT ck_pass_plan_version_duration_unit CHECK (
        duration_unit IN ('DAY', 'WEEK', 'MONTH', 'YEAR')
    )
);

CREATE INDEX IF NOT EXISTS idx_pass_plan_version_plan   ON pass_plan_version(plan_id);
CREATE INDEX IF NOT EXISTS idx_pass_plan_version_status ON pass_plan_version(status);
