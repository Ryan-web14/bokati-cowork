-- Lot I · relances d'impayes parametrees
-- Une politique par segment, des paliers qui sont des donnees, une trace par facture et par palier.
-- Un grand compte et un particulier ne se relancent pas au meme rythme ni sur le meme ton.

CREATE TABLE IF NOT EXISTS dunning_policy (
    id              BIGINT PRIMARY KEY,
    policy_code     VARCHAR(40)  NOT NULL UNIQUE,
    name            VARCHAR(160) NOT NULL,
    -- Le segment que la politique vise · DEFAULT vaut pour ceux qui n'ont pas la leur
    segment         VARCHAR(40)  NOT NULL DEFAULT 'DEFAULT',
    tone            VARCHAR(20)  NOT NULL DEFAULT 'STANDARD',
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_dunning_policy_segment CHECK (segment IN ('DEFAULT', 'MEMBER', 'CUSTOMER', 'BUSINESS_ENTITY')),
    CONSTRAINT ck_dunning_policy_tone CHECK (tone IN ('SOFT', 'STANDARD', 'FIRM'))
);

-- Une seule politique active par segment
CREATE UNIQUE INDEX IF NOT EXISTS uk_dunning_policy_segment_active ON dunning_policy(segment) WHERE active = TRUE;

CREATE TABLE IF NOT EXISTS dunning_step (
    id                  BIGINT PRIMARY KEY,
    policy_id           BIGINT       NOT NULL,
    step_order          INTEGER      NOT NULL,
    days_after_due      INTEGER      NOT NULL,
    action              VARCHAR(30)  NOT NULL,
    channel             VARCHAR(20)  NOT NULL DEFAULT 'EMAIL',
    subject_template    VARCHAR(200),
    message_template    TEXT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_dunning_step_policy FOREIGN KEY (policy_id) REFERENCES dunning_policy(id) ON DELETE CASCADE,
    CONSTRAINT uk_dunning_step_order UNIQUE (policy_id, step_order),
    CONSTRAINT ck_dunning_step_days CHECK (days_after_due >= 0),
    CONSTRAINT ck_dunning_step_action CHECK (action IN ('REMINDER', 'FORMAL_NOTICE', 'GRACE_PERIOD', 'SUSPEND', 'HANDOVER')),
    CONSTRAINT ck_dunning_step_channel CHECK (channel IN ('EMAIL', 'IN_APP', 'STAFF'))
);

CREATE TABLE IF NOT EXISTS dunning_notice (
    id                  BIGINT PRIMARY KEY,
    document_number     VARCHAR(100) NOT NULL,
    step_id             BIGINT       NOT NULL,
    policy_code         VARCHAR(40)  NOT NULL,
    action              VARCHAR(30)  NOT NULL,
    customer_type       VARCHAR(40)  NOT NULL,
    customer_code       VARCHAR(120) NOT NULL,
    subscription_number VARCHAR(100),
    days_overdue        INTEGER      NOT NULL,
    balance_due         NUMERIC(19,4) NOT NULL,
    outcome             VARCHAR(20)  NOT NULL,
    detail              TEXT,
    executed_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_dunning_notice_step FOREIGN KEY (step_id) REFERENCES dunning_step(id),
    CONSTRAINT uk_dunning_notice_step UNIQUE (document_number, step_id),
    CONSTRAINT ck_dunning_notice_outcome CHECK (outcome IN ('SENT', 'SKIPPED', 'FAILED'))
);
CREATE INDEX IF NOT EXISTS idx_dunning_notice_document ON dunning_notice(document_number, executed_at DESC);
CREATE INDEX IF NOT EXISTS idx_dunning_notice_customer ON dunning_notice(customer_type, customer_code, executed_at DESC);

-- ---------------------------------------------------------------------------------------------
-- Seeds · deux politiques, deux rythmes
-- ---------------------------------------------------------------------------------------------
INSERT INTO dunning_policy (id, policy_code, name, segment, tone)
SELECT 2420001, 'DNP-DEFAULT', 'Relance standard', 'DEFAULT', 'STANDARD'
WHERE NOT EXISTS (SELECT 1 FROM dunning_policy WHERE policy_code = 'DNP-DEFAULT');

INSERT INTO dunning_step (id, policy_id, step_order, days_after_due, action, channel, subject_template, message_template)
SELECT 2420101, 2420001, 1, 3, 'REMINDER', 'EMAIL', 'Rappel · facture {documentNumber}', 'Votre facture {documentNumber} de {balanceDue} {currency} est arrivée à échéance le {dueDate}. Vous pouvez la régler depuis votre espace client.'
WHERE NOT EXISTS (SELECT 1 FROM dunning_step WHERE id = 2420101);
INSERT INTO dunning_step (id, policy_id, step_order, days_after_due, action, channel, subject_template, message_template)
SELECT 2420102, 2420001, 2, 10, 'REMINDER', 'EMAIL', 'Second rappel · facture {documentNumber}', 'Sauf erreur de notre part, la facture {documentNumber} ({balanceDue} {currency}) reste impayée depuis {daysOverdue} jours.'
WHERE NOT EXISTS (SELECT 1 FROM dunning_step WHERE id = 2420102);
INSERT INTO dunning_step (id, policy_id, step_order, days_after_due, action, channel, subject_template, message_template)
SELECT 2420103, 2420001, 3, 20, 'FORMAL_NOTICE', 'EMAIL', 'Mise en demeure · facture {documentNumber}', 'Sans règlement de la facture {documentNumber} ({balanceDue} {currency}) sous huit jours, votre abonnement sera suspendu.'
WHERE NOT EXISTS (SELECT 1 FROM dunning_step WHERE id = 2420103);
INSERT INTO dunning_step (id, policy_id, step_order, days_after_due, action, channel, subject_template, message_template)
SELECT 2420104, 2420001, 4, 30, 'SUSPEND', 'EMAIL', 'Suspension · facture {documentNumber}', 'Votre abonnement est suspendu pour impayé. Il sera rétabli dès réception du règlement.'
WHERE NOT EXISTS (SELECT 1 FROM dunning_step WHERE id = 2420104);
INSERT INTO dunning_step (id, policy_id, step_order, days_after_due, action, channel, subject_template, message_template)
SELECT 2420105, 2420001, 5, 45, 'HANDOVER', 'STAFF', 'Impayé à traiter · {documentNumber}', 'La facture {documentNumber} de {customerName} reste impayée après 45 jours et toutes les relances.'
WHERE NOT EXISTS (SELECT 1 FROM dunning_step WHERE id = 2420105);

INSERT INTO dunning_policy (id, policy_code, name, segment, tone)
SELECT 2420002, 'DNP-BUSINESS', 'Relance entreprise', 'BUSINESS_ENTITY', 'SOFT'
WHERE NOT EXISTS (SELECT 1 FROM dunning_policy WHERE policy_code = 'DNP-BUSINESS');

INSERT INTO dunning_step (id, policy_id, step_order, days_after_due, action, channel, subject_template, message_template)
SELECT 2420201, 2420002, 1, 7, 'REMINDER', 'EMAIL', 'Rappel · facture {documentNumber}', 'Nous vous rappelons que la facture {documentNumber} ({balanceDue} {currency}) est échue depuis le {dueDate}.'
WHERE NOT EXISTS (SELECT 1 FROM dunning_step WHERE id = 2420201);
INSERT INTO dunning_step (id, policy_id, step_order, days_after_due, action, channel, subject_template, message_template)
SELECT 2420202, 2420002, 2, 21, 'REMINDER', 'EMAIL', 'Second rappel · facture {documentNumber}', 'La facture {documentNumber} ({balanceDue} {currency}) reste en attente de règlement. Votre comptabilité en a-t-elle connaissance ?'
WHERE NOT EXISTS (SELECT 1 FROM dunning_step WHERE id = 2420202);
INSERT INTO dunning_step (id, policy_id, step_order, days_after_due, action, channel, subject_template, message_template)
SELECT 2420203, 2420002, 3, 35, 'FORMAL_NOTICE', 'EMAIL', 'Mise en demeure · facture {documentNumber}', 'Malgré nos rappels, la facture {documentNumber} ({balanceDue} {currency}) demeure impayée. Nous vous mettons en demeure de la régler sous quinze jours.'
WHERE NOT EXISTS (SELECT 1 FROM dunning_step WHERE id = 2420203);
INSERT INTO dunning_step (id, policy_id, step_order, days_after_due, action, channel, subject_template, message_template)
SELECT 2420204, 2420002, 4, 60, 'HANDOVER', 'STAFF', 'Impayé grand compte · {documentNumber}', 'La facture {documentNumber} de {customerName} est impayée depuis 60 jours · à traiter par le commercial en charge.'
WHERE NOT EXISTS (SELECT 1 FROM dunning_step WHERE id = 2420204);
