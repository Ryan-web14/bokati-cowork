-- Lot F13 : achats. Fournisseurs (avec retenue a la source), commandes issues des etats de besoin
-- et engagees au budget, factures et avoirs fournisseurs ecrits au journal AC, reglements lettres
-- sur le 401.

CREATE TABLE IF NOT EXISTS acc_supplier
(
    id                  BIGINT PRIMARY KEY,
    code                VARCHAR(40)   NOT NULL UNIQUE,
    name                VARCHAR(200)  NOT NULL,
    niu                 VARCHAR(60),
    rccm                VARCHAR(80),
    address             VARCHAR(500),
    phone               VARCHAR(40),
    email               VARCHAR(180),
    bank_details        VARCHAR(500),
    payment_terms_days  INTEGER       NOT NULL DEFAULT 30,
    account_number      VARCHAR(20)   NOT NULL DEFAULT '401',
    default_account     VARCHAR(20),
    vat_exempt          BOOLEAN       NOT NULL DEFAULT FALSE,
    withholding_rate    NUMERIC(7, 4) NOT NULL DEFAULT 0,
    withholding_account VARCHAR(20)   NOT NULL DEFAULT '447',
    withholding_base    VARCHAR(4)    NOT NULL DEFAULT 'HT',
    withholding_label   VARCHAR(200),
    status              VARCHAR(10)   NOT NULL DEFAULT 'ACTIVE',
    created_by          VARCHAR(180)  NOT NULL,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_acc_supplier_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_acc_supplier_withholding CHECK (withholding_rate >= 0 AND withholding_rate < 100 AND withholding_base IN ('HT', 'TTC'))
);

CREATE TABLE IF NOT EXISTS acc_purchase_order
(
    id                    BIGINT PRIMARY KEY,
    number                VARCHAR(40)    NOT NULL UNIQUE,
    supplier_code         VARCHAR(40)    NOT NULL,
    need_statement_number VARCHAR(40),
    subject               VARCHAR(500)   NOT NULL,
    order_date            DATE           NOT NULL,
    expected_date         DATE,
    section_code          VARCHAR(60),
    total_ht              NUMERIC(19, 4) NOT NULL DEFAULT 0,
    total_vat             NUMERIC(19, 4) NOT NULL DEFAULT 0,
    total_ttc             NUMERIC(19, 4) NOT NULL DEFAULT 0,
    invoiced_ht           NUMERIC(19, 4) NOT NULL DEFAULT 0,
    status                VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    budget_committed      BOOLEAN        NOT NULL DEFAULT FALSE,
    validated_by          VARCHAR(180),
    validated_at          TIMESTAMPTZ,
    cancelled_by          VARCHAR(180),
    cancel_reason         VARCHAR(500),
    created_by            VARCHAR(180)   NOT NULL,
    created_at            TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_acc_purchase_order_status CHECK (status IN ('DRAFT', 'VALIDATED', 'PARTIALLY_INVOICED', 'INVOICED', 'CANCELLED'))
);

CREATE TABLE IF NOT EXISTS acc_purchase_order_line
(
    id             BIGINT PRIMARY KEY,
    order_id       BIGINT         NOT NULL REFERENCES acc_purchase_order (id),
    line_no        INTEGER        NOT NULL,
    designation    VARCHAR(500)   NOT NULL,
    quantity       NUMERIC(19, 4) NOT NULL DEFAULT 1,
    unit_price     NUMERIC(19, 4) NOT NULL,
    nature_code    VARCHAR(60),
    account_number VARCHAR(20)    NOT NULL,
    vat_rate       NUMERIC(7, 4)  NOT NULL DEFAULT 0,
    amount_ht      NUMERIC(19, 4) NOT NULL,
    vat_amount     NUMERIC(19, 4) NOT NULL DEFAULT 0,
    section_code   VARCHAR(60),
    invoiced_ht    NUMERIC(19, 4) NOT NULL DEFAULT 0,
    need_line_no   INTEGER
);
CREATE INDEX IF NOT EXISTS ix_acc_purchase_order_line_order ON acc_purchase_order_line (order_id);

CREATE TABLE IF NOT EXISTS acc_supplier_invoice
(
    id                 BIGINT PRIMARY KEY,
    number             VARCHAR(40)    NOT NULL UNIQUE,
    kind               VARCHAR(12)    NOT NULL DEFAULT 'INVOICE',
    supplier_code      VARCHAR(40)    NOT NULL,
    supplier_reference VARCHAR(80)    NOT NULL,
    invoice_date       DATE           NOT NULL,
    due_date           DATE           NOT NULL,
    order_number       VARCHAR(40),
    original_number    VARCHAR(40),
    subject            VARCHAR(500),
    total_ht           NUMERIC(19, 4) NOT NULL DEFAULT 0,
    total_vat          NUMERIC(19, 4) NOT NULL DEFAULT 0,
    total_ttc          NUMERIC(19, 4) NOT NULL DEFAULT 0,
    withholding_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    net_to_pay         NUMERIC(19, 4) NOT NULL DEFAULT 0,
    paid_amount        NUMERIC(19, 4) NOT NULL DEFAULT 0,
    close_order        BOOLEAN        NOT NULL DEFAULT FALSE,
    document_ref       VARCHAR(500),
    status             VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    entry_number       VARCHAR(60),
    posted_by          VARCHAR(180),
    posted_at          TIMESTAMPTZ,
    created_by         VARCHAR(180)   NOT NULL,
    created_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_acc_supplier_invoice_reference UNIQUE (supplier_code, kind, supplier_reference),
    CONSTRAINT ck_acc_supplier_invoice_kind CHECK (kind IN ('INVOICE', 'CREDIT_NOTE')),
    CONSTRAINT ck_acc_supplier_invoice_status CHECK (status IN ('DRAFT', 'POSTED', 'PARTIALLY_PAID', 'PAID', 'CANCELLED'))
);

CREATE TABLE IF NOT EXISTS acc_supplier_invoice_line
(
    id             BIGINT PRIMARY KEY,
    invoice_id     BIGINT         NOT NULL REFERENCES acc_supplier_invoice (id),
    line_no        INTEGER        NOT NULL,
    designation    VARCHAR(500)   NOT NULL,
    nature_code    VARCHAR(60),
    account_number VARCHAR(20)    NOT NULL,
    amount_ht      NUMERIC(19, 4) NOT NULL,
    vat_rate       NUMERIC(7, 4)  NOT NULL DEFAULT 0,
    vat_amount     NUMERIC(19, 4) NOT NULL DEFAULT 0,
    vat_account    VARCHAR(20),
    section_code   VARCHAR(60),
    order_line_no  INTEGER
);
CREATE INDEX IF NOT EXISTS ix_acc_supplier_invoice_line_invoice ON acc_supplier_invoice_line (invoice_id);

CREATE TABLE IF NOT EXISTS acc_supplier_payment
(
    id               BIGINT PRIMARY KEY,
    invoice_number   VARCHAR(40)    NOT NULL,
    supplier_code    VARCHAR(40)    NOT NULL,
    payment_date     DATE           NOT NULL,
    amount           NUMERIC(19, 4) NOT NULL,
    treasury_account VARCHAR(20)    NOT NULL,
    journal_code     VARCHAR(30)    NOT NULL,
    reference        VARCHAR(120),
    entry_number     VARCHAR(60),
    created_by       VARCHAR(180)   NOT NULL,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now()
);

-- Droits.
WITH new_permission (name, display_name, module, action) AS (
    VALUES ('PURCHASE_READ', 'Consulter fournisseurs, commandes et factures fournisseurs', 'PURCHASE', 'READ'),
           ('SUPPLIER_MANAGE', 'Gerer les fournisseurs', 'SUPPLIER', 'MANAGE'),
           ('PURCHASE_ORDER_MANAGE', 'Preparer les commandes fournisseurs', 'PURCHASE_ORDER', 'MANAGE'),
           ('PURCHASE_ORDER_VALIDATE', 'Valider ou annuler une commande fournisseur', 'PURCHASE_ORDER', 'VALIDATE'),
           ('SUPPLIER_INVOICE_MANAGE', 'Saisir et comptabiliser les factures fournisseurs', 'SUPPLIER_INVOICE', 'MANAGE'),
           ('SUPPLIER_PAYMENT_MANAGE', 'Regler les fournisseurs', 'SUPPLIER_PAYMENT', 'MANAGE')
), numbered AS (
    SELECT n.*, ROW_NUMBER() OVER (ORDER BY n.name) AS rank FROM new_permission n
    WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.name = n.name)
)
INSERT INTO permission (id, name, display_name, module, action, is_system_permission, is_active)
SELECT COALESCE((SELECT MAX(id) FROM permission), 0) + rank, name, display_name, module, action, TRUE, TRUE FROM numbered;

WITH grants (role_name, permission_name) AS (
    SELECT r, p FROM (VALUES ('SUPER_ADMIN'), ('ADMIN'), ('FINANCE')) AS roles(r)
    CROSS JOIN (VALUES ('PURCHASE_READ'), ('SUPPLIER_MANAGE'), ('PURCHASE_ORDER_MANAGE'), ('PURCHASE_ORDER_VALIDATE'),
                       ('SUPPLIER_INVOICE_MANAGE'), ('SUPPLIER_PAYMENT_MANAGE')) AS perms(p)
    UNION ALL
    SELECT 'MANAGER', p FROM (VALUES ('PURCHASE_READ'), ('PURCHASE_ORDER_MANAGE'), ('PURCHASE_ORDER_VALIDATE')) AS perms(p)
)
INSERT INTO role_permission (role_id, permission_id, created_by, is_active)
SELECT r.id, p.id, 'SYSTEM', TRUE FROM grants g JOIN role r ON r.name = g.role_name JOIN permission p ON p.name = g.permission_name
WHERE NOT EXISTS (SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
