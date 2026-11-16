-- Centre d'audit, lot A6 : alertes a seuil enregistrees, avec accuse de lecture et trace des envois.

CREATE TABLE IF NOT EXISTS audit_alert
(
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    rule_code         VARCHAR(40)    NOT NULL,
    severity          VARCHAR(10)    NOT NULL,
    status            VARCHAR(15)    NOT NULL,
    title             VARCHAR(200)   NOT NULL,
    message           VARCHAR(1000)  NOT NULL,
    metric_value      NUMERIC(18, 2) NOT NULL,
    threshold         NUMERIC(18, 2) NOT NULL,
    opened_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    last_seen_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    resolved_at       TIMESTAMPTZ,
    acknowledged_at   TIMESTAMPTZ,
    acknowledged_by   VARCHAR(250),
    acknowledge_note  VARCHAR(500),
    notifications     VARCHAR(1000)
);
CREATE INDEX IF NOT EXISTS ix_audit_alert_opened ON audit_alert (opened_at DESC);
CREATE INDEX IF NOT EXISTS ix_audit_alert_status ON audit_alert (status, opened_at DESC);
-- Une seule alerte en cours par regle.
CREATE UNIQUE INDEX IF NOT EXISTS ux_audit_alert_open_rule ON audit_alert (rule_code) WHERE status <> 'RESOLVED';
