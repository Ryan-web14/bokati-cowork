CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE IF NOT EXISTS notification_template (
    id BIGINT PRIMARY KEY,
    template_code VARCHAR(120) NOT NULL UNIQUE,
    channel VARCHAR(40) NOT NULL,
    subject VARCHAR(255),
    template_name VARCHAR(180),
    body_template TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    admin_only BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS notification_message (
    id BIGINT PRIMARY KEY,
    notification_number VARCHAR(100) NOT NULL UNIQUE,
    event_type VARCHAR(120) NOT NULL,
    aggregate_type VARCHAR(80),
    aggregate_id VARCHAR(120),
    channel VARCHAR(40) NOT NULL,
    recipient_type VARCHAR(40),
    recipient_code VARCHAR(120),
    recipient_email VARCHAR(255),
    recipient_name VARCHAR(255),
    subject VARCHAR(255),
    template_code VARCHAR(120),
    template_name VARCHAR(180),
    payload_json JSONB,
    status VARCHAR(40) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    available_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sent_at TIMESTAMPTZ,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS webhook_endpoint (
    id BIGINT PRIMARY KEY,
    endpoint_code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    url TEXT NOT NULL,
    secret TEXT,
    event_types TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS webhook_delivery (
    id BIGINT PRIMARY KEY,
    delivery_number VARCHAR(100) NOT NULL UNIQUE,
    endpoint_id BIGINT NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    aggregate_type VARCHAR(80),
    aggregate_id VARCHAR(120),
    payload_json JSONB,
    status VARCHAR(40) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    available_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    delivered_at TIMESTAMPTZ,
    http_status INTEGER,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_webhook_delivery_endpoint FOREIGN KEY (endpoint_id) REFERENCES webhook_endpoint(id)
);

CREATE INDEX IF NOT EXISTS idx_notification_template_channel ON notification_template(channel, active);
CREATE INDEX IF NOT EXISTS idx_notification_message_status ON notification_message(status, available_at);
CREATE INDEX IF NOT EXISTS idx_notification_message_event ON notification_message(event_type, aggregate_type, aggregate_id);
CREATE INDEX IF NOT EXISTS idx_notification_message_recipient ON notification_message(recipient_type, recipient_code);
CREATE INDEX IF NOT EXISTS idx_webhook_endpoint_active ON webhook_endpoint(active);
CREATE INDEX IF NOT EXISTS idx_webhook_delivery_status ON webhook_delivery(status, available_at);
CREATE INDEX IF NOT EXISTS idx_webhook_delivery_event ON webhook_delivery(event_type, aggregate_type, aggregate_id);

CREATE INDEX IF NOT EXISTS idx_notification_message_number_trgm
    ON notification_message USING gin (notification_number gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_notification_message_email_trgm
    ON notification_message USING gin (recipient_email gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_notification_message_name_trgm
    ON notification_message USING gin (recipient_name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_webhook_endpoint_code_trgm
    ON webhook_endpoint USING gin (endpoint_code gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_webhook_endpoint_name_trgm
    ON webhook_endpoint USING gin (name gin_trgm_ops);

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 740001, 'notification_message', 'Notification message', 'Sequence des notifications', 'NTF', NULL, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'NEVER', TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'notification_message');

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 740002, 'webhook_endpoint', 'Webhook endpoint', 'Sequence des endpoints webhook', 'WHK', NULL, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'NEVER', TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'webhook_endpoint');

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 740003, 'webhook_delivery', 'Webhook delivery', 'Sequence des livraisons webhook', 'WHD', NULL, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'NEVER', TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'webhook_delivery');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 740101, 'EMAIL_CONFIRMATION', 'EMAIL', 'Confirmation de votre adresse email', 'email-confirmation', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'EMAIL_CONFIRMATION');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 740102, 'ONE_TIME_TOKEN', 'EMAIL', 'Votre code de connexion', 'ott-login', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'ONE_TIME_TOKEN');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 740103, 'GENERATED_PASSWORD', 'EMAIL', 'Vos acces Bokati', 'generated-password', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'GENERATED_PASSWORD');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 740104, 'ADMIN_ALERT', 'EMAIL', 'Alerte administration', 'admin-alert', NULL, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'ADMIN_ALERT');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 740105, 'BOOKING_CONFIRMED', 'EMAIL', 'Reservation confirmee', 'generic-notification', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'BOOKING_CONFIRMED');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 740106, 'PAYMENT_SUCCEEDED', 'EMAIL', 'Paiement confirme', 'generic-notification', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'PAYMENT_SUCCEEDED');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 740107, 'BILLING_DOCUMENT_ISSUED', 'EMAIL', 'Document de facturation disponible', 'generic-notification', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'BILLING_DOCUMENT_ISSUED');
