CREATE INDEX IF NOT EXISTS idx_contract_record_template_code ON contract_record(template_code);
CREATE INDEX IF NOT EXISTS idx_contract_record_dates ON contract_record(start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_contract_record_signed_document_code ON contract_record(signed_document_code);
CREATE INDEX IF NOT EXISTS idx_contract_record_draft_document_code ON contract_record(draft_document_code);
