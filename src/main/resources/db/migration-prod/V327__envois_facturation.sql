-- Lot B4 (facturation) : envois. L'envoi choisit ses destinataires (principaux, copie, copie
-- cachee) et ses canaux (courriel avec le PDF, WhatsApp) ; chaque envoi est trace. Preference
-- d'envoi par client. Correction de destinataire d'une piece scellee : le PDF envoye devient un
-- duplicata date, l'original scelle reste la reference (ecart E15).

CREATE TABLE IF NOT EXISTS billing_document_sending
(
    id                 BIGINT PRIMARY KEY,
    document_id        BIGINT        NOT NULL REFERENCES billing_document (id),
    document_number    VARCHAR(60)   NOT NULL,
    channel            VARCHAR(20)   NOT NULL,
    recipients_to      TEXT,
    recipients_cc      TEXT,
    recipients_bcc     TEXT,
    subject            VARCHAR(500),
    message            TEXT,
    duplicate          BOOLEAN       NOT NULL DEFAULT FALSE,
    status             VARCHAR(20)   NOT NULL,
    provider_reference VARCHAR(120),
    error              VARCHAR(1000),
    sent_by            VARCHAR(180)  NOT NULL,
    sent_by_name       VARCHAR(255),
    sent_at            TIMESTAMPTZ   NOT NULL,
    CONSTRAINT ck_billing_sending_channel CHECK (channel IN ('EMAIL', 'WHATSAPP'))
);

CREATE INDEX IF NOT EXISTS ix_billing_sending_document ON billing_document_sending (document_number, sent_at);

CREATE TABLE IF NOT EXISTS billing_recipient_preference
(
    id               BIGINT PRIMARY KEY,
    customer_type    VARCHAR(30)  NOT NULL,
    customer_code    VARCHAR(80)  NOT NULL,
    to_addresses     TEXT,
    cc_addresses     TEXT,
    bcc_addresses    TEXT,
    whatsapp_numbers TEXT,
    channels         VARCHAR(60),
    updated_by       VARCHAR(180),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ux_billing_recipient_preference UNIQUE (customer_type, customer_code)
);

ALTER TABLE billing_document ADD COLUMN IF NOT EXISTS recipient_corrected_at TIMESTAMPTZ;
