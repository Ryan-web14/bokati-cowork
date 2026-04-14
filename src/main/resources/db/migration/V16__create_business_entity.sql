CREATE TABLE IF NOT EXISTS business_entity (
    id BIGINT PRIMARY KEY,
    entity_code VARCHAR(120) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    legal_form VARCHAR(40) NOT NULL,
    niu_number VARCHAR(18) NOT NULL UNIQUE,
    rccm_number VARCHAR(120) NOT NULL UNIQUE,
    tax_id VARCHAR(120),
    activity VARCHAR(255),
    address_id BIGINT,
    phone VARCHAR(30) NOT NULL,
    email VARCHAR(255) NOT NULL,
    base_currency_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_business_entity_address FOREIGN KEY (address_id) REFERENCES address(id),
    CONSTRAINT fk_business_entity_currency FOREIGN KEY (base_currency_id) REFERENCES currency(id)
);

CREATE INDEX IF NOT EXISTS idx_business_entity_code ON business_entity(entity_code);
CREATE INDEX IF NOT EXISTS idx_business_entity_name ON business_entity(name);
CREATE INDEX IF NOT EXISTS idx_business_entity_niu_number ON business_entity(niu_number);
CREATE INDEX IF NOT EXISTS idx_business_entity_rccm_number ON business_entity(rccm_number);
CREATE INDEX IF NOT EXISTS idx_business_entity_deleted ON business_entity(deleted);
