CREATE TABLE IF NOT EXISTS contract_record (
    id BIGINT PRIMARY KEY,
    contract_code VARCHAR(120) NOT NULL UNIQUE,
    title VARCHAR(300) NOT NULL,
    description TEXT,
    template_code VARCHAR(120) NOT NULL,
    owner_type VARCHAR(30) NOT NULL,
    owner_id BIGINT NOT NULL,
    owner_code VARCHAR(120) NOT NULL,
    business_id BIGINT,
    status VARCHAR(30) NOT NULL,
    renewal_type VARCHAR(30) NOT NULL,
    effective_date DATE,
    start_date DATE,
    end_date DATE,
    signed_at TIMESTAMP,
    activated_at TIMESTAMP,
    terminated_at TIMESTAMP,
    termination_reason TEXT,
    draft_document_code VARCHAR(100),
    signed_document_code VARCHAR(100),
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_contract_business FOREIGN KEY (business_id) REFERENCES business_entity(id),
    CONSTRAINT ck_contract_record_owner_type CHECK (
        owner_type IN ('CUSTOMER', 'MEMBER', 'BUSINESS', 'CONTRACT', 'INVOICE', 'PAYMENT', 'PROPOSAL', 'ASSET')
    ),
    CONSTRAINT ck_contract_record_status CHECK (
        status IN ('DRAFT', 'GENERATED', 'UNDER_REVIEW', 'AWAITING_SIGNATURE', 'SIGNED', 'ACTIVE', 'SUSPENDED', 'EXPIRED', 'TERMINATED', 'CANCELLED')
    ),
    CONSTRAINT ck_contract_record_renewal_type CHECK (
        renewal_type IN ('NONE', 'FIXED_TERM', 'AUTO_RENEW')
    )
);

CREATE INDEX IF NOT EXISTS idx_contract_record_owner ON contract_record(owner_type, owner_code);
CREATE INDEX IF NOT EXISTS idx_contract_record_business_id ON contract_record(business_id);
CREATE INDEX IF NOT EXISTS idx_contract_record_status ON contract_record(status);
CREATE INDEX IF NOT EXISTS idx_contract_record_deleted ON contract_record(deleted);

CREATE TABLE IF NOT EXISTS contract_party (
    id BIGINT PRIMARY KEY,
    contract_id BIGINT NOT NULL,
    party_type VARCHAR(30) NOT NULL,
    party_id BIGINT NOT NULL,
    party_code VARCHAR(120) NOT NULL,
    display_name VARCHAR(250) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    role VARCHAR(30) NOT NULL,
    sign_order INTEGER,
    must_sign BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_contract_party_contract FOREIGN KEY (contract_id) REFERENCES contract_record(id) ON DELETE CASCADE,
    CONSTRAINT ck_contract_party_type CHECK (
        party_type IN ('CUSTOMER', 'MEMBER', 'BUSINESS', 'CONTRACT', 'INVOICE', 'PAYMENT', 'PROPOSAL', 'ASSET')
    ),
    CONSTRAINT ck_contract_party_role CHECK (
        role IN ('BENEFICIARY', 'OPERATOR', 'SIGNATORY', 'BILLING_CONTACT')
    )
);

CREATE INDEX IF NOT EXISTS idx_contract_party_contract_id ON contract_party(contract_id);
CREATE INDEX IF NOT EXISTS idx_contract_party_code ON contract_party(party_type, party_code);
