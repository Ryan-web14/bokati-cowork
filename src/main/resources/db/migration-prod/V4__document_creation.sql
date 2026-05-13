CREATE TABLE IF NOT EXISTS document_type (
                               id BIGINT PRIMARY KEY,
                               code VARCHAR(100) NOT NULL UNIQUE,
                               name VARCHAR(200) NOT NULL,

                               category VARCHAR(50) NOT NULL,
                               owner_type VARCHAR(50),

                               description TEXT,

                               required BOOLEAN DEFAULT FALSE,
                               requires_expiry_date BOOLEAN DEFAULT FALSE,
                               requires_review BOOLEAN DEFAULT FALSE,
                               requires_signature BOOLEAN DEFAULT FALSE,

                               active BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS document (
    id BIGINT PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,


    owner_id BIGINT NOT NULL,
    owner_type VARCHAR(50) NOT NULL,

    category VARCHAR(50) NOT NULL,
    type_code VARCHAR(50) NOT NULL,

    document_type_id BIGSERIAL NOT NULL,

    title VARCHAR(300) NOT NULL,
    description TEXT,

    file_name VARCHAR(350),
    file_url TEXT NOT NULL,
    file_size BIGINT,

    status VARCHAR(50) NOT NULL,

    issue_date DATE,
    expiry_date DATE,

    uploaded_by BIGINT,
    uploaded_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_document_type FOREIGN KEY (document_type_id) REFERENCES document_type(id)
);

CREATE TABLE IF NOT EXISTS document_review (
    id BIGINT PRIMARY KEY,
    document_id BIGSERIAL NOT NULL,
    review_status VARCHAR(50) NOT NULL,
    reviewed_by BIGSERIAL,
    reviewed_at TIMESTAMP,
    comment TEXT,

    CONSTRAINT fk_document_review_document
        FOREIGN KEY (document_id) REFERENCES document(id)
);

CREATE TABLE IF NOT EXISTS document_signature (
    id BIGINT PRIMARY KEY,
    document_id BIGINT NOT NULL,

    signer_type VARCHAR(30) NOT NULL,
    signer_id BIGINT NOT NULL,

    signature_status VARCHAR(50) NOT NULL,
    signed_at TIMESTAMP,

    signer_name VARCHAR(250),
    signer_email VARCHAR(250),
    ip_address VARCHAR(60),
    user_agent TEXT,

    signature_data TEXT,

    CONSTRAINT fk_document_signature_document FOREIGN KEY (document_id) REFERENCES document(id)
);

CREATE TABLE IF NOT EXISTS document_requirement (
    id BIGINT PRIMARY KEY,
    owner_type VARCHAR(30) NOT NULL,
    document_type_code VARCHAR(50) NOT NULL,
    required BOOLEAN DEFAULT TRUE,
    active BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS kyc_document (
                              id BIGINT PRIMARY KEY,
                              owner_type VARCHAR(20) NOT NULL,
                              member_id BIGINT,
                              customer_id BIGINT,
                              document_id BIGINT NOT NULL,
                              document_type VARCHAR(150) NOT NULL,
                              document_number VARCHAR(100),
                              issue_date DATE,
                              expiry_date DATE,
                              status VARCHAR(20) NOT NULL,

                              CONSTRAINT fk_kyc_document_owner FOREIGN KEY (member_id) REFERENCES member(id),

                              CONSTRAINT fk_kyc_document_customer FOREIGN KEY (customer_id) REFERENCES customer(id)

);

CREATE TABLE IF NOT EXISTS kyc_verification (
                                  id BIGINT PRIMARY KEY,
                                  owner_type VARCHAR(20),
                                  kyc_document_id BIGSERIAL NOT NULL,
                                  verification_status VARCHAR(20),
                                  verified_by BIGSERIAL,
                                  verified_at TIMESTAMPTZ,
                                  notes TEXT,

                                  CONSTRAINT fk_kyc_document FOREIGN KEY (kyc_document_id) REFERENCES kyc_document(id)
);


CREATE TABLE IF NOT EXISTS  legal_document (
                                id BIGINT PRIMARY KEY,

                                owner_type VARCHAR(20),

                                owner_id BIGINT,

                                member_id BIGINT,

                                customer_id BIGINT,

                                document_type_id BIGINT,

                                title VARCHAR(300),

                                file_url TEXT,

                                status VARCHAR(50),

                                uploaded_at TIMESTAMP,

                                uploaded_by BIGSERIAL,

                                CONSTRAINT fk_document_type FOREIGN KEY (document_type_id) REFERENCES document_type(id),

                                CONSTRAINT fk_member FOREIGN KEY (member_id) REFERENCES member(id),

                                CONSTRAINT fk_customer FOREIGN KEY (customer_id) REFERENCES customer(id)

);