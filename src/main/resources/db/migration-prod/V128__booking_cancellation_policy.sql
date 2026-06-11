CREATE TABLE IF NOT EXISTS cancellation_policy (
    id BIGINT PRIMARY KEY,
    hours_before_start INT NOT NULL,
    refund_percentage NUMERIC(5,2) NOT NULL,
    rule_order INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_cancellation_policy_active_order ON cancellation_policy (active, rule_order);

INSERT INTO cancellation_policy (id, hours_before_start, refund_percentage, rule_order, active, created_at, updated_at)
VALUES
    (1280001, 24, 100.00, 1, TRUE, now(), now()),
    (1280002, 12, 50.00, 2, TRUE, now(), now()),
    (1280003, 0, 0.00, 3, TRUE, now(), now())
ON CONFLICT (id) DO NOTHING;
