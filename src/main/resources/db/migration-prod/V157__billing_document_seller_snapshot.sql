-- Snapshot vendeur + identification fiscale client
ALTER TABLE billing_document
    ADD COLUMN seller_name          VARCHAR(255),
    ADD COLUMN seller_niu           VARCHAR(100),
    ADD COLUMN seller_address_json  JSONB,
    ADD COLUMN seller_phone         VARCHAR(60),
    ADD COLUMN seller_email         VARCHAR(255),
    ADD COLUMN customer_niu         VARCHAR(100),
    ADD COLUMN customer_category    VARCHAR(40);
