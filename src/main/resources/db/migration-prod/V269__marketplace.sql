-- Tranche 7 du centre de gestion des entreprises · la marketplace.
--
-- Une offre rend vendable dans le portail un article de stock (il sort d'un emplacement) ou une
-- prestation du catalogue (sans stock). Une commande distingue l'acheteur (le membre) et le payeur
-- (lui-meme, ou son organisation selon son lien) ; validee, elle retient le stock et emet sa
-- facture ; payee, elle sort le stock ; remise a l'accueil, elle est close.

CREATE SEQUENCE IF NOT EXISTS marketplace_offer_short_id_seq START WITH 1;
CREATE TABLE IF NOT EXISTS marketplace_offer (
    id                  BIGINT PRIMARY KEY,
    short_id            BIGINT        NOT NULL DEFAULT nextval('marketplace_offer_short_id_seq'),
    offer_code          VARCHAR(60)   NOT NULL,
    kind                VARCHAR(20)   NOT NULL,
    inventory_item_code VARCHAR(120),
    location_code       VARCHAR(120),
    service_item_code   VARCHAR(120),
    title               VARCHAR(200)  NOT NULL,
    description         TEXT,
    image_url           VARCHAR(500),
    category            VARCHAR(120),
    -- Prix propre a la marketplace · vide, celui de l'article ou de la prestation.
    price               NUMERIC(19, 4),
    currency            VARCHAR(3)    NOT NULL DEFAULT 'XAF',
    max_per_order       INTEGER,
    published           BOOLEAN       NOT NULL DEFAULT FALSE,
    display_order       INTEGER       NOT NULL DEFAULT 0,
    created_by          VARCHAR(150),
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ,
    CONSTRAINT ck_marketplace_offer_kind CHECK (
        (kind = 'PRODUCT' AND inventory_item_code IS NOT NULL AND location_code IS NOT NULL)
        OR (kind = 'SERVICE' AND service_item_code IS NOT NULL)),
    CONSTRAINT ck_marketplace_offer_amounts CHECK ((price IS NULL OR price >= 0) AND (max_per_order IS NULL OR max_per_order >= 1))
);
ALTER SEQUENCE marketplace_offer_short_id_seq OWNED BY marketplace_offer.short_id;
CREATE UNIQUE INDEX IF NOT EXISTS uk_marketplace_offer_short_id ON marketplace_offer (short_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_marketplace_offer_code ON marketplace_offer (offer_code);
CREATE INDEX IF NOT EXISTS idx_marketplace_offer_published ON marketplace_offer (published, display_order);

CREATE SEQUENCE IF NOT EXISTS marketplace_order_short_id_seq START WITH 1;
CREATE TABLE IF NOT EXISTS marketplace_order (
    id                   BIGINT PRIMARY KEY,
    short_id             BIGINT        NOT NULL DEFAULT nextval('marketplace_order_short_id_seq'),
    order_number         VARCHAR(60),
    buyer_member_code    VARCHAR(120)  NOT NULL,
    billing_company_code VARCHAR(120),
    status               VARCHAR(20)   NOT NULL,
    currency             VARCHAR(3)    NOT NULL DEFAULT 'XAF',
    total_amount         NUMERIC(19, 4) NOT NULL DEFAULT 0,
    invoice_number       VARCHAR(120),
    expires_at           TIMESTAMPTZ,
    placed_at            TIMESTAMPTZ,
    paid_at              TIMESTAMPTZ,
    fulfilled_at         TIMESTAMPTZ,
    fulfilled_by         VARCHAR(150),
    collected_by         VARCHAR(200),
    cancelled_at         TIMESTAMPTZ,
    cancelled_by         VARCHAR(150),
    cancellation_reason  VARCHAR(500),
    credit_note_number   VARCHAR(120),
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ,
    CONSTRAINT ck_marketplace_order_status
        CHECK (status IN ('CART', 'PENDING_PAYMENT', 'PAID', 'FULFILLED', 'CANCELLED', 'EXPIRED'))
);
ALTER SEQUENCE marketplace_order_short_id_seq OWNED BY marketplace_order.short_id;
CREATE UNIQUE INDEX IF NOT EXISTS uk_marketplace_order_short_id ON marketplace_order (short_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_marketplace_order_number ON marketplace_order (order_number) WHERE order_number IS NOT NULL;
-- Un seul panier ouvert par membre.
CREATE UNIQUE INDEX IF NOT EXISTS uk_marketplace_order_cart ON marketplace_order (lower(buyer_member_code)) WHERE status = 'CART';
CREATE INDEX IF NOT EXISTS idx_marketplace_order_buyer ON marketplace_order (lower(buyer_member_code), created_at);
CREATE INDEX IF NOT EXISTS idx_marketplace_order_company ON marketplace_order (billing_company_code, created_at) WHERE billing_company_code IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_marketplace_order_pending ON marketplace_order (status, expires_at) WHERE status = 'PENDING_PAYMENT';

CREATE SEQUENCE IF NOT EXISTS marketplace_order_line_short_id_seq START WITH 1;
CREATE TABLE IF NOT EXISTS marketplace_order_line (
    id                     BIGINT PRIMARY KEY,
    short_id               BIGINT        NOT NULL DEFAULT nextval('marketplace_order_line_short_id_seq'),
    order_id               BIGINT        NOT NULL REFERENCES marketplace_order (id),
    offer_id               BIGINT        NOT NULL REFERENCES marketplace_offer (id),
    title                  VARCHAR(200)  NOT NULL,
    quantity               INTEGER       NOT NULL,
    unit_price             NUMERIC(19, 4),
    amount                 NUMERIC(19, 4),
    stock_reservation_code VARCHAR(120),
    billable_number        VARCHAR(120),
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ,
    CONSTRAINT ck_marketplace_order_line_quantity CHECK (quantity >= 1)
);
ALTER SEQUENCE marketplace_order_line_short_id_seq OWNED BY marketplace_order_line.short_id;
CREATE UNIQUE INDEX IF NOT EXISTS uk_marketplace_order_line_short_id ON marketplace_order_line (short_id);
CREATE INDEX IF NOT EXISTS idx_marketplace_order_line_order ON marketplace_order_line (order_id);

INSERT INTO sequence_definition (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2730001, 'marketplace_offer', 'Offre de la marketplace', 'Sequence des offres de la marketplace', 'MKO', null, '{PREFIX}-{SEQ}', 5, 1, 1, 'NEVER', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE lower(code) = 'marketplace_offer');

INSERT INTO sequence_definition (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2730002, 'marketplace_order', 'Commande de la marketplace', 'Sequence des commandes de la marketplace', 'MKC', null, '{PREFIX}-{YYYY}-{SEQ}', 6, 1, 1, 'YEARLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE lower(code) = 'marketplace_order');
