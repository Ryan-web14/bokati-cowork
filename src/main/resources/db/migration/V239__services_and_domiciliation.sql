-- Catalogue de services et domiciliation.
--
-- Le modele raisonnait en plans et en droits. La domiciliation impose la notion de service
-- souscrit, qui a ses propres obligations operationnelles · un abonnement devient un contenant :
-- il porte un plan, et un ou plusieurs services.
--
-- Trois regles metier actees structurent ce qui suit.
--   1. La domiciliation se facture au mois, au trimestre ou a l'annee.
--   2. L'adresse n'est fiscale que si l'engagement est d'un an. La qualite fiscale se deduit de
--      l'engagement, ne se saisit jamais, et se perd si l'engagement se raccourcit.
--   3. Toute domiciliation genere un contrat enregistre et timbre. L'enregistrement est une etape
--      bloquante, pas une formalite annexe.

-- ---------------------------------------------------------------------------------------------
-- 1. Definitions de service
-- ---------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS service_definition (
    id BIGINT PRIMARY KEY,
    code VARCHAR(60) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(1000),
    service_category VARCHAR(40) NOT NULL,
    delivery_mode VARCHAR(20) NOT NULL DEFAULT 'CONTINUOUS',
    requires_contract BOOLEAN NOT NULL DEFAULT FALSE,
    requires_kyc_level INTEGER,
    requires_physical_resource BOOLEAN NOT NULL DEFAULT FALSE,
    has_regulatory_obligations BOOLEAN NOT NULL DEFAULT FALSE,
    default_billing_cycle VARCHAR(20),
    default_notice_period_days INTEGER,
    default_commitment_months INTEGER,
    unit_price NUMERIC(19,4),
    currency VARCHAR(3) NOT NULL DEFAULT 'XAF',
    usage_entitlement_code VARCHAR(60),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON COLUMN service_definition.delivery_mode IS 'CONTINUOUS, ON_DEMAND, SCHEDULED ou METERED';
COMMENT ON COLUMN service_definition.usage_entitlement_code IS
    'Pour un service METERED · le code de droit sous lequel chaque acte est enregistre et facture';

INSERT INTO service_definition (id, code, name, description, service_category, delivery_mode, requires_contract, requires_kyc_level, has_regulatory_obligations, default_billing_cycle, default_notice_period_days, default_commitment_months, currency)
SELECT 2390001, 'SVC-DOMICILIATION', 'Domiciliation d entreprise', 'Adresse commerciale, fiscale si engagement annuel · contrat enregistre et timbre', 'DOMICILIATION', 'CONTINUOUS', TRUE, 2, TRUE, 'MONTHLY', 30, 1, 'XAF'
WHERE NOT EXISTS (SELECT 1 FROM service_definition WHERE code = 'SVC-DOMICILIATION');

INSERT INTO service_definition (id, code, name, description, service_category, delivery_mode, requires_contract, has_regulatory_obligations, default_billing_cycle, currency)
SELECT 2390002, 'SVC-MAIL-HANDLING', 'Reception du courrier', 'Reception, notification et remise du courrier · incluse dans la domiciliation', 'MAIL_HANDLING', 'CONTINUOUS', FALSE, FALSE, 'MONTHLY', 'XAF'
WHERE NOT EXISTS (SELECT 1 FROM service_definition WHERE code = 'SVC-MAIL-HANDLING');

INSERT INTO service_definition (id, code, name, description, service_category, delivery_mode, unit_price, currency, usage_entitlement_code)
SELECT 2390003, 'SVC-MAIL-SCAN', 'Numerisation du courrier', 'Facturee a l acte', 'MAIL_HANDLING', 'METERED', 500, 'XAF', 'MAIL_SCAN'
WHERE NOT EXISTS (SELECT 1 FROM service_definition WHERE code = 'SVC-MAIL-SCAN');

INSERT INTO service_definition (id, code, name, description, service_category, delivery_mode, unit_price, currency, usage_entitlement_code)
SELECT 2390004, 'SVC-MAIL-FORWARD', 'Reexpedition du courrier', 'Facturee a l acte, frais de transport en sus', 'MAIL_HANDLING', 'METERED', 2000, 'XAF', 'MAIL_FORWARD'
WHERE NOT EXISTS (SELECT 1 FROM service_definition WHERE code = 'SVC-MAIL-FORWARD');

INSERT INTO service_definition (id, code, name, description, service_category, delivery_mode, unit_price, currency, usage_entitlement_code)
SELECT 2390005, 'SVC-MAIL-STORAGE', 'Stockage prolonge du courrier', 'Au-dela du delai de garde, par jour et par pli', 'MAIL_HANDLING', 'METERED', 100, 'XAF', 'MAIL_STORAGE'
WHERE NOT EXISTS (SELECT 1 FROM service_definition WHERE code = 'SVC-MAIL-STORAGE');

INSERT INTO service_definition (id, code, name, description, service_category, delivery_mode, requires_physical_resource, default_billing_cycle, currency)
SELECT 2390006, 'SVC-PHONE-ANSWERING', 'Permanence telephonique', 'Ligne attribuee, accueil, transfert de messages', 'PHONE_ANSWERING', 'CONTINUOUS', FALSE, 'MONTHLY', 'XAF'
WHERE NOT EXISTS (SELECT 1 FROM service_definition WHERE code = 'SVC-PHONE-ANSWERING');

INSERT INTO service_definition (id, code, name, description, service_category, delivery_mode, requires_physical_resource, default_billing_cycle, currency)
SELECT 2390007, 'SVC-LOCKER', 'Casier', 'Casier attribue, cle ou code', 'LOCKER', 'CONTINUOUS', TRUE, 'MONTHLY', 'XAF'
WHERE NOT EXISTS (SELECT 1 FROM service_definition WHERE code = 'SVC-LOCKER');

INSERT INTO service_definition (id, code, name, description, service_category, delivery_mode, requires_physical_resource, default_billing_cycle, currency)
SELECT 2390008, 'SVC-PARKING', 'Place de parking', 'Place attribuee, plaque, badge', 'PARKING', 'CONTINUOUS', TRUE, 'MONTHLY', 'XAF'
WHERE NOT EXISTS (SELECT 1 FROM service_definition WHERE code = 'SVC-PARKING');

-- ---------------------------------------------------------------------------------------------
-- 2. Services souscrits · l'abonnement devient un contenant
-- ---------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS subscription_service (
    id BIGINT PRIMARY KEY,
    service_number VARCHAR(100) NOT NULL UNIQUE,
    subscription_id BIGINT NOT NULL,
    service_definition_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    quantity INTEGER NOT NULL DEFAULT 1,
    unit_price NUMERIC(19,4),
    currency VARCHAR(3) NOT NULL DEFAULT 'XAF',
    activated_at TIMESTAMPTZ,
    suspended_at TIMESTAMPTZ,
    terminated_at TIMESTAMPTZ,
    service_metadata_json TEXT,
    created_by VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_subscription_service_subscription FOREIGN KEY (subscription_id) REFERENCES subscription(id),
    CONSTRAINT fk_subscription_service_definition FOREIGN KEY (service_definition_id) REFERENCES service_definition(id),
    CONSTRAINT ck_subscription_service_quantity CHECK (quantity > 0)
);

CREATE INDEX IF NOT EXISTS idx_subscription_service_subscription ON subscription_service(subscription_id, status);

-- ---------------------------------------------------------------------------------------------
-- 3. Registre des adresses attribuables
-- ---------------------------------------------------------------------------------------------
--
-- Deux societes peuvent partager une adresse ; le complement distinctif les separe. Le registre
-- dit quelles adresses existent, lesquelles peuvent porter une qualite fiscale, et combien de
-- domicilies chacune accueille.

CREATE TABLE IF NOT EXISTS domiciliation_address (
    id BIGINT PRIMARY KEY,
    code VARCHAR(60) NOT NULL UNIQUE,
    label VARCHAR(160) NOT NULL,
    address_id BIGINT NOT NULL,
    fiscal_capable BOOLEAN NOT NULL DEFAULT TRUE,
    max_occupants INTEGER,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    notes VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_domiciliation_address_address FOREIGN KEY (address_id) REFERENCES address(id)
);

-- ---------------------------------------------------------------------------------------------
-- 4. Contrat de domiciliation
-- ---------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS domiciliation_contract (
    id BIGINT PRIMARY KEY,
    contract_number VARCHAR(100) NOT NULL UNIQUE,
    subscription_id BIGINT NOT NULL,
    subscription_service_id BIGINT NOT NULL,
    business_entity_id BIGINT,
    legal_name VARCHAR(255) NOT NULL,
    legal_form VARCHAR(100),
    registration_number VARCHAR(120),
    tax_number VARCHAR(120),
    legal_representative_type VARCHAR(40),
    legal_representative_code VARCHAR(120),
    legal_representative_name VARCHAR(255),
    assigned_address_id BIGINT NOT NULL,
    suite_number VARCHAR(40),

    billing_cycle VARCHAR(20) NOT NULL,
    commitment_months INTEGER NOT NULL,
    fiscal_address_eligible BOOLEAN NOT NULL DEFAULT FALSE,
    fiscal_address_granted_at TIMESTAMPTZ,
    fiscal_address_revoked_at TIMESTAMPTZ,
    fiscal_address_revocation_reason VARCHAR(500),

    status VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
    start_date DATE,
    end_date DATE,
    notice_period_days INTEGER NOT NULL DEFAULT 30,

    contract_document_code VARCHAR(120),
    signed_document_code VARCHAR(120),
    signed_at TIMESTAMPTZ,
    certificate_document_code VARCHAR(120),
    certificate_scope VARCHAR(20),
    certificate_issued_at TIMESTAMPTZ,
    certificate_valid_until DATE,
    certificate_revoked_at TIMESTAMPTZ,
    certificate_revocation_reason VARCHAR(500),

    mail_forwarding_mode VARCHAR(30) NOT NULL DEFAULT 'HOLD',
    forwarding_address_id BIGINT,
    forwarding_frequency VARCHAR(30),

    terminated_at TIMESTAMPTZ,
    termination_reason VARCHAR(500),
    termination_notified_at TIMESTAMPTZ,
    administration_notified_at TIMESTAMPTZ,
    retain_documents_until DATE,

    created_by VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_domiciliation_subscription FOREIGN KEY (subscription_id) REFERENCES subscription(id),
    CONSTRAINT fk_domiciliation_service FOREIGN KEY (subscription_service_id) REFERENCES subscription_service(id),
    CONSTRAINT fk_domiciliation_business FOREIGN KEY (business_entity_id) REFERENCES business_entity(id),
    CONSTRAINT fk_domiciliation_assigned_address FOREIGN KEY (assigned_address_id) REFERENCES domiciliation_address(id),
    CONSTRAINT fk_domiciliation_forwarding_address FOREIGN KEY (forwarding_address_id) REFERENCES address(id),
    CONSTRAINT ck_domiciliation_cycle CHECK (billing_cycle IN ('MONTHLY', 'QUARTERLY', 'YEARLY')),
    CONSTRAINT ck_domiciliation_commitment CHECK (commitment_months >= 1),
    -- La qualite fiscale ne se saisit pas : la base la refuse sous douze mois d'engagement.
    CONSTRAINT ck_domiciliation_fiscal CHECK (fiscal_address_eligible = FALSE OR commitment_months >= 12),
    -- Une attestation fiscale sur un contrat non eligible n'existe pas, meme par erreur.
    CONSTRAINT ck_domiciliation_certificate_scope CHECK (
        certificate_scope IS NULL OR certificate_scope = 'COMMERCIAL'
        OR (certificate_scope = 'FISCAL' AND fiscal_address_eligible = TRUE)
    )
);

CREATE INDEX IF NOT EXISTS idx_domiciliation_status ON domiciliation_contract(status);
CREATE INDEX IF NOT EXISTS idx_domiciliation_subscription ON domiciliation_contract(subscription_id);
CREATE INDEX IF NOT EXISTS idx_domiciliation_certificate_expiry ON domiciliation_contract(certificate_valid_until) WHERE certificate_document_code IS NOT NULL;

-- Une adresse et un complement distinctif ne designent qu'un domicilie en cours.
CREATE UNIQUE INDEX IF NOT EXISTS uk_domiciliation_suite_active
    ON domiciliation_contract(assigned_address_id, COALESCE(suite_number, ''))
    WHERE status NOT IN ('TERMINATED', 'EXPIRED');

-- ---------------------------------------------------------------------------------------------
-- 5. Enregistrement aupres de l'administration · une demarche externe, suivie a part
-- ---------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS domiciliation_registration (
    id BIGINT PRIMARY KEY,
    registration_number VARCHAR(100) NOT NULL UNIQUE,
    domiciliation_contract_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    submitted_at TIMESTAMPTZ,
    submitted_by VARCHAR(120),
    administration_office VARCHAR(255),
    administration_reference VARCHAR(120),
    registration_date DATE,
    stamp_duty_amount NUMERIC(19,4),
    registration_fee_amount NUMERIC(19,4),
    total_duty_amount NUMERIC(19,4),
    currency VARCHAR(3) NOT NULL DEFAULT 'XAF',
    paid_by VARCHAR(20),
    rebilled BOOLEAN NOT NULL DEFAULT FALSE,
    rebilled_document_code VARCHAR(120),
    receipt_document_code VARCHAR(120),
    registered_document_code VARCHAR(120),
    expires_at DATE,
    rejection_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_domiciliation_registration_contract FOREIGN KEY (domiciliation_contract_id) REFERENCES domiciliation_contract(id)
);

CREATE INDEX IF NOT EXISTS idx_domiciliation_registration_status ON domiciliation_registration(status, submitted_at);

-- ---------------------------------------------------------------------------------------------
-- 6. Courrier · avec preuve de remise
-- ---------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS mail_item (
    id BIGINT PRIMARY KEY,
    item_number VARCHAR(100) NOT NULL UNIQUE,
    domiciliation_contract_id BIGINT NOT NULL,
    mail_type VARCHAR(30) NOT NULL,
    sender_name VARCHAR(255),
    sender_reference VARCHAR(120),
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    received_by VARCHAR(120) NOT NULL,
    weight_grams INTEGER,
    dimensions VARCHAR(60),
    status VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',
    scan_document_code VARCHAR(120),
    notified_at TIMESTAMPTZ,
    notification_channel VARCHAR(30),
    collected_at TIMESTAMPTZ,
    collected_by VARCHAR(255),
    collector_id_document VARCHAR(120),
    collector_signature_url VARCHAR(500),
    handed_over_by VARCHAR(120),
    forwarded_at TIMESTAMPTZ,
    forwarding_tracking_number VARCHAR(120),
    forwarding_cost NUMERIC(19,4),
    storage_deadline DATE,
    billable BOOLEAN NOT NULL DEFAULT FALSE,
    billed_amount NUMERIC(19,4),
    usage_number VARCHAR(100),
    notes VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_mail_item_contract FOREIGN KEY (domiciliation_contract_id) REFERENCES domiciliation_contract(id),
    -- Une remise sans identite du porteur n'est pas une remise · pour le recommande et les actes,
    -- la base l'exige, pas seulement le code.
    CONSTRAINT ck_mail_item_collection_proof CHECK (
        status <> 'COLLECTED'
        OR (collected_by IS NOT NULL AND collected_at IS NOT NULL
            AND (mail_type NOT IN ('REGISTERED_LETTER', 'ADMINISTRATIVE', 'LEGAL_NOTICE') OR collector_id_document IS NOT NULL))
    )
);

CREATE INDEX IF NOT EXISTS idx_mail_item_contract ON mail_item(domiciliation_contract_id, status);
CREATE INDEX IF NOT EXISTS idx_mail_item_deadline ON mail_item(status, storage_deadline);

-- Chaque passage d'un pli laisse une ligne · c'est le journal qui fait la preuve, et il ne se
-- reecrit pas.
CREATE TABLE IF NOT EXISTS mail_item_event (
    id BIGINT PRIMARY KEY,
    mail_item_id BIGINT NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    actor VARCHAR(120) NOT NULL,
    details VARCHAR(1000),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_mail_item_event_item FOREIGN KEY (mail_item_id) REFERENCES mail_item(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_mail_item_event_item ON mail_item_event(mail_item_id, occurred_at);

CREATE OR REPLACE FUNCTION mail_item_event_append_only()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Le journal du courrier ne se modifie pas · c est lui qui fait la preuve de remise' USING ERRCODE = '23514';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_mail_item_event_append_only ON mail_item_event;
CREATE TRIGGER trg_mail_item_event_append_only
    BEFORE UPDATE OR DELETE ON mail_item_event
    FOR EACH ROW EXECUTE FUNCTION mail_item_event_append_only();

-- ---------------------------------------------------------------------------------------------
-- 7. Types de documents
-- ---------------------------------------------------------------------------------------------

INSERT INTO document_type (id, code, name, category, owner_type, description, required, requires_expiry_date, requires_review, requires_signature, active, multiple_allowed, allowed_mime_types, max_file_size_bytes)
SELECT 1030, 'DOMICILIATION_CONTRACT', 'Contrat de domiciliation', 'LEGAL', NULL, 'Contrat de domiciliation genere', FALSE, FALSE, FALSE, TRUE, TRUE, TRUE, 'application/pdf', 15728640
WHERE NOT EXISTS (SELECT 1 FROM document_type WHERE code = 'DOMICILIATION_CONTRACT');

INSERT INTO document_type (id, code, name, category, owner_type, description, required, requires_expiry_date, requires_review, requires_signature, active, multiple_allowed, allowed_mime_types, max_file_size_bytes)
SELECT 1031, 'DOMICILIATION_CONTRACT_SIGNED', 'Contrat de domiciliation signe', 'LEGAL', NULL, 'Exemplaire signe par les parties', FALSE, FALSE, TRUE, FALSE, TRUE, TRUE, 'application/pdf,image/jpeg,image/png', 15728640
WHERE NOT EXISTS (SELECT 1 FROM document_type WHERE code = 'DOMICILIATION_CONTRACT_SIGNED');

INSERT INTO document_type (id, code, name, category, owner_type, description, required, requires_expiry_date, requires_review, requires_signature, active, multiple_allowed, allowed_mime_types, max_file_size_bytes)
SELECT 1032, 'DOMICILIATION_REGISTERED_COPY', 'Contrat enregistre et timbre', 'LEGAL', NULL, 'Exemplaire enregistre et timbre par l administration, numerise', FALSE, TRUE, TRUE, FALSE, TRUE, TRUE, 'application/pdf,image/jpeg,image/png', 15728640
WHERE NOT EXISTS (SELECT 1 FROM document_type WHERE code = 'DOMICILIATION_REGISTERED_COPY');

INSERT INTO document_type (id, code, name, category, owner_type, description, required, requires_expiry_date, requires_review, requires_signature, active, multiple_allowed, allowed_mime_types, max_file_size_bytes)
SELECT 1033, 'DOMICILIATION_REGISTRATION_RECEIPT', 'Quittance d enregistrement', 'FINANCIAL', NULL, 'Recu de l administration pour les droits acquittes', FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, 'application/pdf,image/jpeg,image/png', 15728640
WHERE NOT EXISTS (SELECT 1 FROM document_type WHERE code = 'DOMICILIATION_REGISTRATION_RECEIPT');

INSERT INTO document_type (id, code, name, category, owner_type, description, required, requires_expiry_date, requires_review, requires_signature, active, multiple_allowed, allowed_mime_types, max_file_size_bytes)
SELECT 1034, 'DOMICILIATION_CERTIFICATE', 'Attestation de domiciliation', 'LEGAL', NULL, 'Attestation commerciale ou fiscale, generee', FALSE, TRUE, FALSE, FALSE, TRUE, TRUE, 'application/pdf', 15728640
WHERE NOT EXISTS (SELECT 1 FROM document_type WHERE code = 'DOMICILIATION_CERTIFICATE');

INSERT INTO document_type (id, code, name, category, owner_type, description, required, requires_expiry_date, requires_review, requires_signature, active, multiple_allowed, allowed_mime_types, max_file_size_bytes)
SELECT 1035, 'MAIL_SCAN', 'Courrier numerise', 'OTHER', NULL, 'Numerisation d un pli recu en domiciliation', FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, 'application/pdf,image/jpeg,image/png', 31457280
WHERE NOT EXISTS (SELECT 1 FROM document_type WHERE code = 'MAIL_SCAN');

-- ---------------------------------------------------------------------------------------------
-- 8. Sequences
-- ---------------------------------------------------------------------------------------------

INSERT INTO sequence_definition (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2390101, 'subscription_service', 'Service souscrit', 'Sequence des services souscrits', 'SSV', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'NEVER', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'subscription_service');

INSERT INTO sequence_definition (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2390102, 'domiciliation_contract', 'Contrat de domiciliation', 'Sequence des contrats de domiciliation', 'DOM', null, '{PREFIX}-{YYYY}-{SEQ}', 5, 1, 1, 'YEARLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'domiciliation_contract');

INSERT INTO sequence_definition (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2390103, 'domiciliation_registration', 'Enregistrement de domiciliation', 'Sequence des demarches d enregistrement', 'DRG', null, '{PREFIX}-{YYYY}-{SEQ}', 5, 1, 1, 'YEARLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'domiciliation_registration');

INSERT INTO sequence_definition (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2390104, 'domiciliation_address', 'Adresse de domiciliation', 'Sequence du registre des adresses', 'DAD', null, '{PREFIX}-{SEQ}', 4, 1, 1, 'NEVER', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'domiciliation_address');

INSERT INTO sequence_definition (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2390105, 'mail_item', 'Pli recu', 'Sequence du courrier recu en domiciliation', 'MAIL', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'mail_item');
