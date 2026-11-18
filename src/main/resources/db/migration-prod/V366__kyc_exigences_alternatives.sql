-- Exigences KYC en ET / OU (17/11/2026) : les exigences d'un meme groupe d'alternatives valent OU
-- (une piece du groupe suffit) ; groupes et exigences seules valent ET.
ALTER TABLE document_requirement ADD COLUMN IF NOT EXISTS alternative_group VARCHAR(60);
ALTER TABLE document_requirement ADD COLUMN IF NOT EXISTS alternative_group_label VARCHAR(150);
ALTER TABLE kyc_case_requirement ADD COLUMN IF NOT EXISTS alternative_group VARCHAR(60);
ALTER TABLE kyc_case_requirement ADD COLUMN IF NOT EXISTS alternative_group_label VARCHAR(150);
