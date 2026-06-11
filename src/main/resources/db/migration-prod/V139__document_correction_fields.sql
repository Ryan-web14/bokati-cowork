-- NEEDS_CORRECTION workflow: deadline, reviewer note, and counter on the document
ALTER TABLE document
    ADD COLUMN correction_deadline TIMESTAMP,
    ADD COLUMN correction_note     TEXT,
    ADD COLUMN correction_count    INT NOT NULL DEFAULT 0;

-- Mirror correction context on the review record
ALTER TABLE document_review
    ADD COLUMN correction_deadline TIMESTAMP,
    ADD COLUMN correction_note     TEXT;
