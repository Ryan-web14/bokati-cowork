-- Cash movement enrichments: status, batching, balances, channel/device context, risk scoring
ALTER TABLE cash_movement
    ADD COLUMN IF NOT EXISTS status VARCHAR(40) NOT NULL DEFAULT 'CONFIRMED',
    ADD COLUMN IF NOT EXISTS related_movement_id BIGINT,
    ADD COLUMN IF NOT EXISTS batch_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS running_balance NUMERIC(19,4),
    ADD COLUMN IF NOT EXISTS exchange_rate NUMERIC(19,6),
    ADD COLUMN IF NOT EXISTS channel VARCHAR(40) NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN IF NOT EXISTS device_code VARCHAR(120),
    ADD COLUMN IF NOT EXISTS device_ip VARCHAR(64),
    ADD COLUMN IF NOT EXISTS sub_category VARCHAR(120),
    ADD COLUMN IF NOT EXISTS tags VARCHAR(500),
    ADD COLUMN IF NOT EXISTS risk_score NUMERIC(6,2),
    ADD COLUMN IF NOT EXISTS requires_signature BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS signed_by VARCHAR(120),
    ADD COLUMN IF NOT EXISTS signed_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS printed_at TIMESTAMPTZ;

ALTER TABLE cash_movement
    ADD CONSTRAINT fk_cash_movement_related_movement
        FOREIGN KEY (related_movement_id) REFERENCES cash_movement (id);

CREATE INDEX IF NOT EXISTS idx_cash_movement_status ON cash_movement (status);
CREATE INDEX IF NOT EXISTS idx_cash_movement_channel ON cash_movement (channel);
CREATE INDEX IF NOT EXISTS idx_cash_movement_batch_id ON cash_movement (batch_id);
CREATE INDEX IF NOT EXISTS idx_cash_movement_related_movement ON cash_movement (related_movement_id);
CREATE INDEX IF NOT EXISTS idx_cash_movement_sub_category ON cash_movement (sub_category);
CREATE INDEX IF NOT EXISTS idx_cash_movement_risk_score ON cash_movement (risk_score);

-- Attachments (justificatifs, photos, scans) linked to a cash movement
CREATE TABLE IF NOT EXISTS cash_movement_attachment
(
    id               BIGINT PRIMARY KEY,
    cash_movement_id BIGINT       NOT NULL,
    file_name        VARCHAR(255) NOT NULL,
    content_type     VARCHAR(120),
    storage_path     VARCHAR(500) NOT NULL,
    label            VARCHAR(255),
    uploaded_by      VARCHAR(120),
    uploaded_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_cash_movement_attachment_movement
        FOREIGN KEY (cash_movement_id) REFERENCES cash_movement (id)
);

CREATE INDEX IF NOT EXISTS idx_cash_movement_attachment_movement
    ON cash_movement_attachment (cash_movement_id);

-- Cash requests: justificatif submissions and "remise de fonds" (cash handover) requests
CREATE TABLE IF NOT EXISTS cash_request
(
    id                   BIGINT PRIMARY KEY,
    request_number       VARCHAR(100) NOT NULL UNIQUE,
    cash_session_id      BIGINT       NOT NULL,
    request_type         VARCHAR(40)  NOT NULL,
    status               VARCHAR(40)  NOT NULL DEFAULT 'PENDING',
    amount               NUMERIC(19,4) NOT NULL,
    currency             VARCHAR(3)   NOT NULL DEFAULT 'XAF',
    reason               TEXT         NOT NULL,
    requested_by         VARCHAR(120) NOT NULL,
    requested_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    reviewed_by          VARCHAR(120),
    reviewed_at          TIMESTAMPTZ,
    review_note          TEXT,
    executed_movement_id BIGINT,
    executed_at          TIMESTAMPTZ,
    attachments_json     JSONB,
    metadata_json        JSONB,
    CONSTRAINT fk_cash_request_session
        FOREIGN KEY (cash_session_id) REFERENCES cash_session (id),
    CONSTRAINT fk_cash_request_executed_movement
        FOREIGN KEY (executed_movement_id) REFERENCES cash_movement (id)
);

CREATE INDEX IF NOT EXISTS idx_cash_request_session ON cash_request (cash_session_id);
CREATE INDEX IF NOT EXISTS idx_cash_request_status ON cash_request (status);
CREATE INDEX IF NOT EXISTS idx_cash_request_type ON cash_request (request_type);
CREATE INDEX IF NOT EXISTS idx_cash_request_requested_by ON cash_request (requested_by);

-- Anomaly flags raised by the semi-automated fraud detection rules
CREATE TABLE IF NOT EXISTS cash_anomaly_flag
(
    id              BIGINT PRIMARY KEY,
    flag_number     VARCHAR(100) NOT NULL UNIQUE,
    cash_session_id BIGINT,
    cash_movement_id BIGINT,
    anomaly_type    VARCHAR(60)  NOT NULL,
    severity        VARCHAR(20)  NOT NULL,
    score           NUMERIC(6,2),
    description     TEXT         NOT NULL,
    detected_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    status          VARCHAR(40)  NOT NULL DEFAULT 'OPEN',
    reviewed_by     VARCHAR(120),
    reviewed_at     TIMESTAMPTZ,
    review_note     TEXT,
    metadata_json   JSONB,
    CONSTRAINT fk_cash_anomaly_flag_session
        FOREIGN KEY (cash_session_id) REFERENCES cash_session (id),
    CONSTRAINT fk_cash_anomaly_flag_movement
        FOREIGN KEY (cash_movement_id) REFERENCES cash_movement (id)
);

CREATE INDEX IF NOT EXISTS idx_cash_anomaly_flag_session ON cash_anomaly_flag (cash_session_id);
CREATE INDEX IF NOT EXISTS idx_cash_anomaly_flag_movement ON cash_anomaly_flag (cash_movement_id);
CREATE INDEX IF NOT EXISTS idx_cash_anomaly_flag_severity ON cash_anomaly_flag (severity);
CREATE INDEX IF NOT EXISTS idx_cash_anomaly_flag_status ON cash_anomaly_flag (status);
CREATE INDEX IF NOT EXISTS idx_cash_anomaly_flag_type ON cash_anomaly_flag (anomaly_type);

-- Sequence definitions for the new cash request and anomaly flag identifiers
INSERT INTO sequence_definition
    (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 1240001, 'cash_request', 'Demande de caisse', 'Sequence des demandes de justificatif et de remise de fonds', 'CRQ', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'cash_request');

INSERT INTO sequence_definition
    (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 1240002, 'cash_anomaly_flag', 'Anomalie de caisse', 'Sequence des signalements d''anomalies de caisse', 'CAF', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'cash_anomaly_flag');
