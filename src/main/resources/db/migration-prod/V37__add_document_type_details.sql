ALTER TABLE document_type
    ADD COLUMN IF NOT EXISTS document_details TEXT;

UPDATE document_type
SET document_details = 'Document d''identification individuel. Le fichier doit montrer clairement le nom, le prenom, la date de naissance, le numero du document, la photo et la date de validite si disponible.'
WHERE code IN ('MEMBER_NATIONAL_ID', 'CUSTOMER_PERSON_ID_CARD')
  AND document_details IS NULL;

UPDATE document_type
SET document_details = 'Passeport individuel. La page d''identification doit etre visible, avec le numero du passeport, le nom, la nationalite, la photo, la date d''emission et la date d''expiration.'
WHERE code = 'CUSTOMER_PERSON_PASSPORT'
  AND document_details IS NULL;

UPDATE document_type
SET document_details = 'Titre de sejour ou document equivalent autorisant la residence. La date de validite doit etre lisible.'
WHERE code = 'MEMBER_RESIDENCE_PERMIT'
  AND document_details IS NULL;

UPDATE document_type
SET document_details = 'Justificatif de domicile recent: facture, attestation, bail, quittance ou document equivalent mentionnant le nom ou l''adresse du titulaire.'
WHERE code = 'MEMBER_PROOF_OF_ADDRESS'
  AND document_details IS NULL;

UPDATE document_type
SET document_details = 'RCCM de l''entreprise. Le document doit contenir le numero RCCM, la denomination sociale, la forme juridique, le siege et les informations d''immatriculation.'
WHERE code IN ('BUSINESS_RCCM', 'CUSTOMER_COMPANY_RCCM')
  AND document_details IS NULL;

UPDATE document_type
SET document_details = 'NIU de l''entreprise. Le document doit contenir le numero fiscal, la denomination, l''administration emettrice et les informations d''identification fiscale.'
WHERE code IN ('BUSINESS_NIU', 'CUSTOMER_COMPANY_NIU')
  AND document_details IS NULL;

UPDATE document_type
SET document_details = 'Statuts de l''entreprise. Le fichier doit inclure les pages principales, l''objet social, la forme juridique, le capital, les associes/actionnaires et les signatures si disponibles.'
WHERE code IN ('BUSINESS_STATUTES', 'CUSTOMER_COMPANY_STATUTES')
  AND document_details IS NULL;

UPDATE document_type
SET document_details = 'Piece d''identite du representant legal. Le document doit correspondre a la personne habilitee a representer l''entreprise.'
WHERE code IN ('BUSINESS_REPRESENTATIVE_ID', 'CUSTOMER_COMPANY_REPRESENTATIVE_ID')
  AND document_details IS NULL;

UPDATE document_type
SET document_details = 'Justificatif d''adresse de l''entreprise: bail, facture, attestation de domiciliation ou document equivalent indiquant l''adresse officielle.'
WHERE code = 'BUSINESS_ADDRESS_PROOF'
  AND document_details IS NULL;

UPDATE document_type
SET document_details = 'Brouillon de contrat genere par le systeme depuis un template. Il peut etre relu, remplace ou converti en document signe.'
WHERE code IN ('CONTRACT_DRAFT', 'CONTRACT_TEMPLATE')
  AND document_details IS NULL;

UPDATE document_type
SET document_details = 'Copie finale signee du contrat. Le fichier doit contenir toutes les signatures obligatoires et les annexes references si elles font partie du document principal.'
WHERE code = 'CONTRACT_SIGNED_COPY'
  AND document_details IS NULL;

UPDATE document_type
SET document_details = 'Annexe contractuelle: avenant, grille tarifaire, conditions particulieres, planning ou tout document complementaire rattache au contrat.'
WHERE code = 'CONTRACT_ANNEX'
  AND document_details IS NULL;
