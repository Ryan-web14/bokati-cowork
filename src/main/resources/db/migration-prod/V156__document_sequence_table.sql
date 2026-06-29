-- Séquence fiscale SEFC : numérotation continue annuelle par type de document
CREATE TABLE document_sequence (
    id             BIGINT       PRIMARY KEY,
    document_type  VARCHAR(40)  NOT NULL,
    year           INT          NOT NULL,
    prefix         VARCHAR(10)  NOT NULL,
    current_value  BIGINT       NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_document_sequence_type_year UNIQUE (document_type, year)
);

-- Amorçage pour l'année courante
INSERT INTO document_sequence (id, document_type, year, prefix, current_value)
VALUES
    (1560001, 'QUOTE',            2026, 'DEV', 0),
    (1560002, 'INVOICE',          2026, 'FAC', 0),
    (1560003, 'CREDIT_NOTE',      2026, 'AVR', 0),
    (1560004, 'DEBIT_NOTE',       2026, 'DBN', 0),
    (1560005, 'PROFORMA_INVOICE', 2026, 'PRF', 0);
