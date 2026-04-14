INSERT INTO document_type (
    id, code, name, category, owner_type, description, required,
    requires_expiry_date, requires_review, requires_signature, active,
    multiple_allowed, allowed_mime_types, max_file_size_bytes
)
SELECT 1007, 'MEMBER_NATIONAL_ID', 'Carte nationale d''identite membre', 'KYC', 'MEMBER',
       'Piece d''identite nationale du membre', TRUE, TRUE, TRUE, FALSE, TRUE,
       FALSE, 'application/pdf,image/jpeg,image/png,image/webp', 10485760
WHERE NOT EXISTS (SELECT 1 FROM document_type WHERE code = 'MEMBER_NATIONAL_ID');

INSERT INTO document_type (
    id, code, name, category, owner_type, description, required,
    requires_expiry_date, requires_review, requires_signature, active,
    multiple_allowed, allowed_mime_types, max_file_size_bytes
)
SELECT 1008, 'MEMBER_RESIDENCE_PERMIT', 'Titre de sejour membre', 'KYC', 'MEMBER',
       'Titre de sejour ou visa long sejour du membre', FALSE, TRUE, TRUE, FALSE, TRUE,
       FALSE, 'application/pdf,image/jpeg,image/png,image/webp', 10485760
WHERE NOT EXISTS (SELECT 1 FROM document_type WHERE code = 'MEMBER_RESIDENCE_PERMIT');

INSERT INTO document_type (
    id, code, name, category, owner_type, description, required,
    requires_expiry_date, requires_review, requires_signature, active,
    multiple_allowed, allowed_mime_types, max_file_size_bytes
)
SELECT 1009, 'MEMBER_PROOF_OF_ADDRESS', 'Justificatif de domicile membre', 'KYC', 'MEMBER',
       'Justificatif de domicile recent du membre', FALSE, FALSE, TRUE, FALSE, TRUE,
       FALSE, 'application/pdf,image/jpeg,image/png,image/webp', 10485760
WHERE NOT EXISTS (SELECT 1 FROM document_type WHERE code = 'MEMBER_PROOF_OF_ADDRESS');

INSERT INTO document_type (
    id, code, name, category, owner_type, description, required,
    requires_expiry_date, requires_review, requires_signature, active,
    multiple_allowed, allowed_mime_types, max_file_size_bytes
)
SELECT 1010, 'CUSTOMER_PERSON_ID_CARD', 'Piece d''identite client personne', 'KYC', 'CUSTOMER',
       'Piece d''identite du client personne physique', TRUE, TRUE, TRUE, FALSE, TRUE,
       FALSE, 'application/pdf,image/jpeg,image/png,image/webp', 10485760
WHERE NOT EXISTS (SELECT 1 FROM document_type WHERE code = 'CUSTOMER_PERSON_ID_CARD');

INSERT INTO document_type (
    id, code, name, category, owner_type, description, required,
    requires_expiry_date, requires_review, requires_signature, active,
    multiple_allowed, allowed_mime_types, max_file_size_bytes
)
SELECT 1011, 'CUSTOMER_PERSON_PASSPORT', 'Passeport client personne', 'KYC', 'CUSTOMER',
       'Passeport du client personne physique', FALSE, TRUE, TRUE, FALSE, TRUE,
       FALSE, 'application/pdf,image/jpeg,image/png,image/webp', 10485760
WHERE NOT EXISTS (SELECT 1 FROM document_type WHERE code = 'CUSTOMER_PERSON_PASSPORT');

INSERT INTO document_type (
    id, code, name, category, owner_type, description, required,
    requires_expiry_date, requires_review, requires_signature, active,
    multiple_allowed, allowed_mime_types, max_file_size_bytes
)
SELECT 1012, 'CUSTOMER_COMPANY_RCCM', 'RCCM client entreprise', 'KYC', 'CUSTOMER',
       'Registre du commerce et du credit mobilier du client entreprise', TRUE, FALSE, TRUE, FALSE, TRUE,
       FALSE, 'application/pdf,image/jpeg,image/png,image/webp', 10485760
WHERE NOT EXISTS (SELECT 1 FROM document_type WHERE code = 'CUSTOMER_COMPANY_RCCM');

INSERT INTO document_type (
    id, code, name, category, owner_type, description, required,
    requires_expiry_date, requires_review, requires_signature, active,
    multiple_allowed, allowed_mime_types, max_file_size_bytes
)
SELECT 1013, 'CUSTOMER_COMPANY_NIU', 'NIU client entreprise', 'KYC', 'CUSTOMER',
       'Numero d''identification unique du client entreprise', TRUE, FALSE, TRUE, FALSE, TRUE,
       FALSE, 'application/pdf,image/jpeg,image/png,image/webp', 10485760
WHERE NOT EXISTS (SELECT 1 FROM document_type WHERE code = 'CUSTOMER_COMPANY_NIU');

INSERT INTO document_type (
    id, code, name, category, owner_type, description, required,
    requires_expiry_date, requires_review, requires_signature, active,
    multiple_allowed, allowed_mime_types, max_file_size_bytes
)
SELECT 1014, 'CUSTOMER_COMPANY_STATUTES', 'Statuts client entreprise', 'LEGAL', 'CUSTOMER',
       'Statuts signes du client entreprise', TRUE, FALSE, TRUE, FALSE, TRUE,
       FALSE, 'application/pdf,image/jpeg,image/png', 15728640
WHERE NOT EXISTS (SELECT 1 FROM document_type WHERE code = 'CUSTOMER_COMPANY_STATUTES');

INSERT INTO document_type (
    id, code, name, category, owner_type, description, required,
    requires_expiry_date, requires_review, requires_signature, active,
    multiple_allowed, allowed_mime_types, max_file_size_bytes
)
SELECT 1015, 'CUSTOMER_COMPANY_REPRESENTATIVE_ID', 'Piece representant client entreprise', 'KYC', 'CUSTOMER',
       'Piece d''identite du representant legal du client entreprise', TRUE, TRUE, TRUE, FALSE, TRUE,
       FALSE, 'application/pdf,image/jpeg,image/png,image/webp', 10485760
WHERE NOT EXISTS (SELECT 1 FROM document_type WHERE code = 'CUSTOMER_COMPANY_REPRESENTATIVE_ID');

INSERT INTO document_type (
    id, code, name, category, owner_type, description, required,
    requires_expiry_date, requires_review, requires_signature, active,
    multiple_allowed, allowed_mime_types, max_file_size_bytes
)
SELECT 1016, 'BUSINESS_STATUTES', 'Statuts entreprise', 'LEGAL', 'BUSINESS',
       'Statuts signes de l''entreprise exploitee dans la plateforme', TRUE, FALSE, TRUE, FALSE, TRUE,
       FALSE, 'application/pdf,image/jpeg,image/png', 15728640
WHERE NOT EXISTS (SELECT 1 FROM document_type WHERE code = 'BUSINESS_STATUTES');

INSERT INTO document_type (
    id, code, name, category, owner_type, description, required,
    requires_expiry_date, requires_review, requires_signature, active,
    multiple_allowed, allowed_mime_types, max_file_size_bytes
)
SELECT 1017, 'BUSINESS_REPRESENTATIVE_ID', 'Piece representant entreprise', 'KYC', 'BUSINESS',
       'Piece d''identite du representant legal de l''entreprise', TRUE, TRUE, TRUE, FALSE, TRUE,
       FALSE, 'application/pdf,image/jpeg,image/png,image/webp', 10485760
WHERE NOT EXISTS (SELECT 1 FROM document_type WHERE code = 'BUSINESS_REPRESENTATIVE_ID');

INSERT INTO document_type (
    id, code, name, category, owner_type, description, required,
    requires_expiry_date, requires_review, requires_signature, active,
    multiple_allowed, allowed_mime_types, max_file_size_bytes
)
SELECT 1018, 'BUSINESS_ADDRESS_PROOF', 'Justificatif adresse entreprise', 'KYC', 'BUSINESS',
       'Justificatif d''adresse ou de siege de l''entreprise', FALSE, FALSE, TRUE, FALSE, TRUE,
       FALSE, 'application/pdf,image/jpeg,image/png,image/webp', 10485760
WHERE NOT EXISTS (SELECT 1 FROM document_type WHERE code = 'BUSINESS_ADDRESS_PROOF');

INSERT INTO document_type (
    id, code, name, category, owner_type, description, required,
    requires_expiry_date, requires_review, requires_signature, active,
    multiple_allowed, allowed_mime_types, max_file_size_bytes
)
SELECT 1019, 'CONTRACT_TEMPLATE', 'Modele de contrat', 'LEGAL', 'CONTRACT',
       'Modele de contrat ou annexe contractuelle', FALSE, FALSE, TRUE, FALSE, TRUE,
       TRUE, 'application/pdf,text/html', 15728640
WHERE NOT EXISTS (SELECT 1 FROM document_type WHERE code = 'CONTRACT_TEMPLATE');

INSERT INTO document_type (
    id, code, name, category, owner_type, description, required,
    requires_expiry_date, requires_review, requires_signature, active,
    multiple_allowed, allowed_mime_types, max_file_size_bytes
)
SELECT 1020, 'CONTRACT_ANNEX', 'Annexe de contrat', 'LEGAL', 'CONTRACT',
       'Annexe contractuelle signee ou jointe au contrat', FALSE, FALSE, TRUE, TRUE, TRUE,
       TRUE, 'application/pdf,image/jpeg,image/png', 15728640
WHERE NOT EXISTS (SELECT 1 FROM document_type WHERE code = 'CONTRACT_ANNEX');

INSERT INTO document_requirement (
    id, owner_type, document_type_code, document_type_name, customer_type, business_legal_form, required, active
)
SELECT 2004, 'MEMBER', 'MEMBER_NATIONAL_ID', 'Carte nationale d''identite membre', NULL, NULL, TRUE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM document_requirement WHERE id = 2004);

INSERT INTO document_requirement (
    id, owner_type, document_type_code, document_type_name, customer_type, business_legal_form, required, active
)
SELECT 2005, 'MEMBER', 'MEMBER_PROOF_OF_ADDRESS', 'Justificatif de domicile membre', NULL, NULL, FALSE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM document_requirement WHERE id = 2005);

INSERT INTO document_requirement (
    id, owner_type, document_type_code, document_type_name, customer_type, business_legal_form, required, active
)
SELECT 2006, 'CUSTOMER', 'CUSTOMER_PERSON_ID_CARD', 'Piece d''identite client personne', 'PERSON', NULL, TRUE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM document_requirement WHERE id = 2006);

INSERT INTO document_requirement (
    id, owner_type, document_type_code, document_type_name, customer_type, business_legal_form, required, active
)
SELECT 2007, 'CUSTOMER', 'CUSTOMER_PERSON_PASSPORT', 'Passeport client personne', 'PERSON', NULL, FALSE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM document_requirement WHERE id = 2007);

INSERT INTO document_requirement (
    id, owner_type, document_type_code, document_type_name, customer_type, business_legal_form, required, active
)
SELECT 2008, 'CUSTOMER', 'CUSTOMER_COMPANY_RCCM', 'RCCM client entreprise', 'COMPANY', NULL, TRUE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM document_requirement WHERE id = 2008);

INSERT INTO document_requirement (
    id, owner_type, document_type_code, document_type_name, customer_type, business_legal_form, required, active
)
SELECT 2009, 'CUSTOMER', 'CUSTOMER_COMPANY_NIU', 'NIU client entreprise', 'COMPANY', NULL, TRUE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM document_requirement WHERE id = 2009);

INSERT INTO document_requirement (
    id, owner_type, document_type_code, document_type_name, customer_type, business_legal_form, required, active
)
SELECT 2010, 'CUSTOMER', 'CUSTOMER_COMPANY_STATUTES', 'Statuts client entreprise', 'COMPANY', NULL, TRUE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM document_requirement WHERE id = 2010);

INSERT INTO document_requirement (
    id, owner_type, document_type_code, document_type_name, customer_type, business_legal_form, required, active
)
SELECT 2011, 'CUSTOMER', 'CUSTOMER_COMPANY_REPRESENTATIVE_ID', 'Piece representant client entreprise', 'COMPANY', NULL, TRUE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM document_requirement WHERE id = 2011);

INSERT INTO document_requirement (
    id, owner_type, document_type_code, document_type_name, customer_type, business_legal_form, required, active
)
SELECT 2012, 'BUSINESS', 'BUSINESS_RCCM', 'Business RCCM', NULL, NULL, TRUE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM document_requirement WHERE id = 2012);

INSERT INTO document_requirement (
    id, owner_type, document_type_code, document_type_name, customer_type, business_legal_form, required, active
)
SELECT 2013, 'BUSINESS', 'BUSINESS_NIU', 'Business NIU', NULL, NULL, TRUE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM document_requirement WHERE id = 2013);

INSERT INTO document_requirement (
    id, owner_type, document_type_code, document_type_name, customer_type, business_legal_form, required, active
)
SELECT 2014, 'BUSINESS', 'BUSINESS_STATUTES', 'Statuts entreprise', NULL, NULL, TRUE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM document_requirement WHERE id = 2014);

INSERT INTO document_requirement (
    id, owner_type, document_type_code, document_type_name, customer_type, business_legal_form, required, active
)
SELECT 2015, 'BUSINESS', 'BUSINESS_REPRESENTATIVE_ID', 'Piece representant entreprise', NULL, NULL, TRUE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM document_requirement WHERE id = 2015);

INSERT INTO document_requirement (
    id, owner_type, document_type_code, document_type_name, customer_type, business_legal_form, required, active
)
SELECT 2016, 'BUSINESS', 'BUSINESS_ADDRESS_PROOF', 'Justificatif adresse entreprise', NULL, NULL, FALSE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM document_requirement WHERE id = 2016);

INSERT INTO document_requirement (
    id, owner_type, document_type_code, document_type_name, customer_type, business_legal_form, required, active
)
SELECT 2017, 'CONTRACT', 'CONTRACT_DRAFT', 'Contract Draft', NULL, NULL, TRUE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM document_requirement WHERE id = 2017);

INSERT INTO document_requirement (
    id, owner_type, document_type_code, document_type_name, customer_type, business_legal_form, required, active
)
SELECT 2018, 'CONTRACT', 'CONTRACT_SIGNED_COPY', 'Signed Contract Copy', NULL, NULL, TRUE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM document_requirement WHERE id = 2018);
