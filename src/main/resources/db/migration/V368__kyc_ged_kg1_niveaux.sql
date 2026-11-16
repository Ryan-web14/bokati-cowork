-- Centre KYC et GED, lot KG1 : niveau KYC des exigences et niveau demande d'un dossier.
-- Une exigence de niveau 2 n'est demandee qu'aux dossiers de niveau 2 et plus. Les exigences et
-- dossiers existants sont au niveau 1 (rien ne change pour eux).
ALTER TABLE document_requirement ADD COLUMN IF NOT EXISTS kyc_level INTEGER NOT NULL DEFAULT 1;
ALTER TABLE kyc_case ADD COLUMN IF NOT EXISTS requested_kyc_level INTEGER NOT NULL DEFAULT 1;
