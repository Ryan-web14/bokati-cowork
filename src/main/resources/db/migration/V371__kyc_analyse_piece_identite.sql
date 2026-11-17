-- Lot KG9 : verdict de l'analyse « piece d'identite » au depot (contour, format, MRZ, visage avec OpenCV).
ALTER TABLE kyc_document ADD COLUMN IF NOT EXISTS identity_verdict VARCHAR(30);
ALTER TABLE kyc_document ADD COLUMN IF NOT EXISTS back_identity_verdict VARCHAR(30);
ALTER TABLE kyc_document ADD COLUMN IF NOT EXISTS identity_engine VARCHAR(10);
