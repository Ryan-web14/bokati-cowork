-- Centre KYC et GED : regles de controle croise preenregistrees et qualite d'image des pieces.
--
-- Les regles ne comparaient que des champs lus par OCR (Tesseract, absent en production) : elles
-- ne s'appliquaient jamais. Elles ont maintenant un type ; la plupart s'appuient sur les donnees
-- saisies et le profil, l'OCR ne sert que s'il est disponible.

ALTER TABLE kyc_cross_validation_rule ADD COLUMN IF NOT EXISTS code VARCHAR(80);
ALTER TABLE kyc_cross_validation_rule ADD COLUMN IF NOT EXISTS name VARCHAR(200);
ALTER TABLE kyc_cross_validation_rule ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE kyc_cross_validation_rule ADD COLUMN IF NOT EXISTS rule_type VARCHAR(40) NOT NULL DEFAULT 'FIELD_MATCH';
ALTER TABLE kyc_cross_validation_rule ADD COLUMN IF NOT EXISTS parameter VARCHAR(200);
ALTER TABLE kyc_cross_validation_rule ADD COLUMN IF NOT EXISTS system_rule BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE kyc_cross_validation_rule ALTER COLUMN document_type_code1 TYPE VARCHAR(1000);
ALTER TABLE kyc_cross_validation_rule ALTER COLUMN document_type_code2 TYPE VARCHAR(1000);
ALTER TABLE kyc_cross_validation_rule ALTER COLUMN document_type_code1 DROP NOT NULL;
ALTER TABLE kyc_cross_validation_rule ALTER COLUMN document_type_code2 DROP NOT NULL;
ALTER TABLE kyc_cross_validation_rule ALTER COLUMN field_to_compare DROP NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_kyc_cross_validation_rule_code ON kyc_cross_validation_rule (code) WHERE code IS NOT NULL;

WITH seed (code, name, description, rule_type, document_type_code1, document_type_code2, field_to_compare, parameter, blocking) AS (
    VALUES
    ('PIECE_NON_ECHUE', 'Pièce non échue',
     'La date d''expiration saisie n''est pas dépassée.',
     'DOCUMENT_NOT_EXPIRED', NULL, NULL, NULL, NULL, TRUE),
    ('DATES_COHERENTES', 'Dates cohérentes',
     'La date d''émission est passée et antérieure à la date d''expiration.',
     'ISSUE_BEFORE_EXPIRY', NULL, NULL, NULL, NULL, TRUE),
    ('VALIDITE_RESTANTE', 'Validité restante suffisante',
     'La pièce reste valable au moins le nombre de jours indiqué (paramètre).',
     'MIN_VALIDITY', NULL, NULL, NULL, '30', FALSE),
    ('NUMERO_UNIQUE', 'Numéro de pièce propre au titulaire',
     'Le numéro saisi n''est pas déjà utilisé par un autre titulaire.',
     'UNIQUE_DOCUMENT_NUMBER', NULL, NULL, NULL, NULL, FALSE),
    ('NIU_PROFIL', 'NIU conforme au profil',
     'Le numéro saisi sur la pièce NIU est celui de la fiche (entreprise ou client entreprise).',
     'PROFILE_MATCH', 'BUSINESS_NIU,CUSTOMER_COMPANY_NIU,MEMBER_NIU', NULL, 'niu', NULL, FALSE),
    ('RCCM_PROFIL', 'RCCM conforme au profil',
     'Le numéro saisi sur la pièce RCCM est celui de la fiche de l''entreprise.',
     'PROFILE_MATCH', 'BUSINESS_RCCM,CUSTOMER_COMPANY_RCCM', NULL, 'rccm', NULL, FALSE),
    ('NOM_PROFIL_OCR', 'Nom lu conforme au profil',
     'Le nom lu sur la pièce d''identité (OCR) est celui de la fiche. Ignorée sans OCR.',
     'PROFILE_MATCH', 'MEMBER_NATIONAL_ID,MEMBER_PASSPORT,MEMBER_ID_CARD,MEMBER_RESIDENCE_PERMIT,MEMBER_DRIVERS_LICENSE,CUSTOMER_PERSON_ID_CARD,CUSTOMER_PERSON_PASSPORT,NATIONAL_ID', NULL, 'last_name', NULL, FALSE),
    ('NUMERO_SAISI_OCR', 'Numéro saisi conforme à la pièce',
     'Le numéro saisi est celui lu sur la pièce (OCR). Ignorée sans OCR.',
     'DECLARED_VS_OCR', NULL, NULL, 'document_number', NULL, FALSE),
    ('ECHEANCE_SAISIE_OCR', 'Échéance saisie conforme à la pièce',
     'La date d''expiration saisie est celle lue sur la pièce (OCR). Ignorée sans OCR.',
     'DECLARED_VS_OCR', NULL, NULL, 'expiry_date', NULL, FALSE),
    ('NOM_IDENTITE_DOMICILE', 'Même nom sur l''identité et le justificatif de domicile',
     'Le nom lu (OCR) est le même sur la pièce d''identité et le justificatif de domicile. Ignorée sans OCR.',
     'FIELD_MATCH', 'MEMBER_NATIONAL_ID', 'MEMBER_PROOF_OF_ADDRESS', 'last_name', NULL, FALSE)
), numbered AS (
    SELECT seed.*, ROW_NUMBER() OVER () AS rn FROM seed
    WHERE NOT EXISTS (SELECT 1 FROM kyc_cross_validation_rule r WHERE r.code = seed.code)
)
INSERT INTO kyc_cross_validation_rule (id, code, name, description, rule_type, document_type_code1, document_type_code2,
                                       field_to_compare, parameter, blocking, active, system_rule)
SELECT COALESCE((SELECT MAX(id) FROM kyc_cross_validation_rule), 0) + rn, code, name, description, rule_type,
       document_type_code1, document_type_code2, field_to_compare, parameter, blocking, TRUE, TRUE
FROM numbered;

-- Qualite d'image mesuree au depot (recto et verso) : avertissement, jamais refus.
ALTER TABLE kyc_document ADD COLUMN IF NOT EXISTS quality_status VARCHAR(20);
ALTER TABLE kyc_document ADD COLUMN IF NOT EXISTS quality_warnings TEXT;
ALTER TABLE kyc_document ADD COLUMN IF NOT EXISTS quality_blur_score NUMERIC(12, 2);
ALTER TABLE kyc_document ADD COLUMN IF NOT EXISTS back_quality_status VARCHAR(20);
ALTER TABLE kyc_document ADD COLUMN IF NOT EXISTS back_quality_warnings TEXT;
ALTER TABLE kyc_document ADD COLUMN IF NOT EXISTS back_quality_blur_score NUMERIC(12, 2);
