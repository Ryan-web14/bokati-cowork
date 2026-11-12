-- Lot F11 : fiscalite. Codes de taxe sur les lignes de TVA, exigibilite sur les debits ou sur les
-- encaissements (compte d'attente), declaration mensuelle de TVA, liquidation.

-- Compte d'attente de la TVA facturee non encore exigible (regime des encaissements).
INSERT INTO acc_account (id, number, label, parent_number, class_code, nature, normal_side, reconcilable, third_party_type,
                         treasury_type, analytic_required, statement_line_code, status, created_by)
SELECT 2970001, '4438', 'TVA facturee en attente d''exigibilite', '443', '4', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE, 'CLASSE_4',
       'ACTIVE', 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM acc_account WHERE number = '4438');

CREATE TABLE IF NOT EXISTS acc_vat_return
(
    id                      BIGINT PRIMARY KEY,
    period_code             VARCHAR(7)     NOT NULL,
    start_date              DATE           NOT NULL,
    end_date                DATE           NOT NULL,
    regime                  VARCHAR(10)    NOT NULL,
    collected               NUMERIC(19, 4) NOT NULL DEFAULT 0,
    deductible              NUMERIC(19, 4) NOT NULL DEFAULT 0,
    credit_brought_forward  NUMERIC(19, 4) NOT NULL DEFAULT 0,
    due_amount              NUMERIC(19, 4) NOT NULL DEFAULT 0,
    credit_carried_forward  NUMERIC(19, 4) NOT NULL DEFAULT 0,
    revenue                 NUMERIC(19, 4) NOT NULL DEFAULT 0,
    invoice_tax_total       NUMERIC(19, 4),
    control_difference      NUMERIC(19, 4),
    pending_balance         NUMERIC(19, 4) NOT NULL DEFAULT 0,
    detail_json             TEXT,
    status                  VARCHAR(12)    NOT NULL DEFAULT 'DRAFT',
    voucher_number          VARCHAR(40),
    liquidation_entry_number VARCHAR(40),
    prepared_by             VARCHAR(180)   NOT NULL,
    prepared_at             TIMESTAMPTZ    NOT NULL DEFAULT now(),
    validated_by            VARCHAR(180),
    validated_at            TIMESTAMPTZ,
    liquidated_by           VARCHAR(180),
    liquidated_at           TIMESTAMPTZ,
    CONSTRAINT uq_acc_vat_return_period UNIQUE (period_code),
    CONSTRAINT ck_acc_vat_return_status CHECK (status IN ('DRAFT', 'VALIDATED', 'LIQUIDATED')),
    CONSTRAINT ck_acc_vat_return_regime CHECK (regime IN ('INVOICE', 'PAYMENT'))
);

-- Factures et avoirs : les lignes de TVA et de centime additionnel portent la reference du document,
-- leur code de taxe et leur base.
INSERT INTO acc_posting_rule (id, code, version, event_type, label, description, family, priority, conditions, journal, entry_label, lines,
                              no_entry, sample_payload, effective_from, status, activated_by, activated_at, created_by)
SELECT r.id + 40000, r.code, r.version + 1, r.event_type, r.label, r.description, r.family, r.priority, r.conditions, r.journal, r.entry_label,
       replace(replace(replace(replace(r.lines,
           '{"side":"CREDIT","account":"{vatAccount}","amount":"vatAmount","label":"TVA"}',
           '{"side":"CREDIT","account":"{vatAccount}","amount":"vatAmount","label":"TVA","reference":"{documentNumber}","taxCode":"{?vatCode}","taxBase":"vatBase"}'),
           '{"side":"CREDIT","account":"{extraTaxAccount}","amount":"extraTaxAmount","label":"Centime additionnel"}',
           '{"side":"CREDIT","account":"{extraTaxAccount}","amount":"extraTaxAmount","label":"Centime additionnel","reference":"{documentNumber}","taxCode":"{?extraTaxCode}","taxBase":"vatAmount"}'),
           '{"side":"DEBIT","account":"{vatAccount}","amount":"vatAmount","label":"TVA"}',
           '{"side":"DEBIT","account":"{vatAccount}","amount":"vatAmount","label":"TVA","reference":"{vatReference}","taxCode":"{?vatCode}","taxBase":"vatBase"}'),
           '{"side":"DEBIT","account":"{extraTaxAccount}","amount":"extraTaxAmount","label":"Centime additionnel"}',
           '{"side":"DEBIT","account":"{extraTaxAccount}","amount":"extraTaxAmount","label":"Centime additionnel","reference":"{vatReference}","taxCode":"{?extraTaxCode}","taxBase":"vatAmount"}'),
       r.no_entry, r.sample_payload, r.effective_from, 'ACTIVE', 'SYSTEM', now(), 'SYSTEM'
FROM acc_posting_rule r
WHERE r.code IN ('SALES_INVOICE', 'SALES_CREDIT_NOTE') AND r.status = 'ACTIVE'
  AND NOT EXISTS (SELECT 1 FROM acc_posting_rule o WHERE o.code = r.code AND o.version = r.version + 1);

-- Encaissements : en regime des encaissements, la TVA de chaque facture reglee passe du compte
-- d'attente au compte de TVA facturee, au prorata du reglement (liste vatTransfers, vide sinon).
INSERT INTO acc_posting_rule (id, code, version, event_type, label, description, family, priority, conditions, journal, entry_label, lines,
                              no_entry, sample_payload, effective_from, status, activated_by, activated_at, created_by)
SELECT r.id + 40000, r.code, r.version + 1, r.event_type, r.label, r.description, r.family, r.priority, r.conditions, r.journal, r.entry_label,
       left(r.lines, length(r.lines) - 1) || CASE WHEN r.code = 'SALES_PAYMENT' THEN
           ',{"side":"DEBIT","account":"{vatPendingAccount}","amount":"vat","foreach":"vatTransfers","label":"TVA devenue exigible","reference":"{documentNumber}"},{"side":"CREDIT","account":"{account}","amount":"vat","foreach":"vatTransfers","label":"TVA exigible","reference":"{documentNumber}","taxCode":"{?taxCode}","taxBase":"base"}]'
       ELSE
           ',{"side":"DEBIT","account":"{account}","amount":"vat","foreach":"vatTransfers","label":"TVA exigible remboursee","reference":"{documentNumber}","taxCode":"{?taxCode}","taxBase":"base"},{"side":"CREDIT","account":"{vatPendingAccount}","amount":"vat","foreach":"vatTransfers","label":"TVA remise en attente","reference":"{documentNumber}"}]'
       END,
       r.no_entry, r.sample_payload, r.effective_from, 'ACTIVE', 'SYSTEM', now(), 'SYSTEM'
FROM acc_posting_rule r
WHERE r.code IN ('SALES_PAYMENT', 'SALES_REFUND') AND r.status = 'ACTIVE'
  AND NOT EXISTS (SELECT 1 FROM acc_posting_rule o WHERE o.code = r.code AND o.version = r.version + 1);

UPDATE acc_posting_rule r SET status = 'RETIRED'
WHERE r.code IN ('SALES_INVOICE', 'SALES_CREDIT_NOTE', 'SALES_PAYMENT', 'SALES_REFUND') AND r.status = 'ACTIVE'
  AND EXISTS (SELECT 1 FROM acc_posting_rule o WHERE o.code = r.code AND o.version > r.version AND o.status = 'ACTIVE');

-- Droits.
WITH new_permission (name, display_name, module, action) AS (
    VALUES ('VAT_RETURN_READ', 'Consulter les declarations de TVA', 'VAT_RETURN', 'READ'),
           ('VAT_RETURN_PREPARE', 'Preparer une declaration de TVA', 'VAT_RETURN', 'PREPARE'),
           ('VAT_RETURN_VALIDATE', 'Valider et liquider une declaration de TVA', 'VAT_RETURN', 'VALIDATE')
), numbered AS (
    SELECT n.*, ROW_NUMBER() OVER (ORDER BY n.name) AS rank FROM new_permission n
    WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.name = n.name)
)
INSERT INTO permission (id, name, display_name, module, action, is_system_permission, is_active)
SELECT COALESCE((SELECT MAX(id) FROM permission), 0) + rank, name, display_name, module, action, TRUE, TRUE FROM numbered;

WITH grants (role_name, permission_name) AS (
    SELECT r, p FROM (VALUES ('SUPER_ADMIN'), ('ADMIN'), ('FINANCE')) AS roles(r)
    CROSS JOIN (VALUES ('VAT_RETURN_READ'), ('VAT_RETURN_PREPARE'), ('VAT_RETURN_VALIDATE')) AS perms(p)
    UNION ALL
    SELECT 'MANAGER', 'VAT_RETURN_READ'
)
INSERT INTO role_permission (role_id, permission_id, created_by, is_active)
SELECT r.id, p.id, 'SYSTEM', TRUE FROM grants g JOIN role r ON r.name = g.role_name JOIN permission p ON p.name = g.permission_name
WHERE NOT EXISTS (SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
