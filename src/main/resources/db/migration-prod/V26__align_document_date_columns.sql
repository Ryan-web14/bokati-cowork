ALTER TABLE document
    ALTER COLUMN issue_date TYPE DATE USING issue_date::date;

ALTER TABLE document
    ALTER COLUMN expiry_date TYPE DATE USING expiry_date::date;

ALTER TABLE kyc_document
    ALTER COLUMN issue_date TYPE DATE USING issue_date::date;

ALTER TABLE kyc_document
    ALTER COLUMN expiry_date TYPE DATE USING expiry_date::date;
