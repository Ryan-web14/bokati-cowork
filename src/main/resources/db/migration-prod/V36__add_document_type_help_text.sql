ALTER TABLE document_type
    ADD COLUMN IF NOT EXISTS help_text TEXT;

UPDATE document_type
SET help_text = 'Importer une copie lisible de la carte nationale d''identite. Les quatre coins doivent etre visibles et le document ne doit pas etre flou.'
WHERE code IN ('MEMBER_NATIONAL_ID', 'CUSTOMER_PERSON_ID_CARD', 'BUSINESS_REPRESENTATIVE_ID', 'CUSTOMER_COMPANY_REPRESENTATIVE_ID')
  AND help_text IS NULL;

UPDATE document_type
SET help_text = 'Importer une copie lisible du passeport, incluant la page d''identification et la date d''expiration.'
WHERE code = 'CUSTOMER_PERSON_PASSPORT'
  AND help_text IS NULL;

UPDATE document_type
SET help_text = 'Importer un titre de sejour valide et lisible. La date d''expiration doit etre renseignee si elle est presente sur le document.'
WHERE code = 'MEMBER_RESIDENCE_PERMIT'
  AND help_text IS NULL;

UPDATE document_type
SET help_text = 'Importer un justificatif de domicile recent et lisible: facture, attestation, bail ou document equivalent.'
WHERE code IN ('MEMBER_PROOF_OF_ADDRESS', 'BUSINESS_ADDRESS_PROOF')
  AND help_text IS NULL;

UPDATE document_type
SET help_text = 'Importer le RCCM officiel de l''entreprise. Le numero RCCM doit etre visible et coherent avec les informations business/customer.'
WHERE code IN ('BUSINESS_RCCM', 'CUSTOMER_COMPANY_RCCM')
  AND help_text IS NULL;

UPDATE document_type
SET help_text = 'Importer le document NIU officiel. Le numero NIU doit etre visible et coherent avec les informations business/customer.'
WHERE code IN ('BUSINESS_NIU', 'CUSTOMER_COMPANY_NIU')
  AND help_text IS NULL;

UPDATE document_type
SET help_text = 'Importer les statuts complets de l''entreprise, idealement signes ou certifies si disponible.'
WHERE code IN ('BUSINESS_STATUTES', 'CUSTOMER_COMPANY_STATUTES')
  AND help_text IS NULL;

UPDATE document_type
SET help_text = 'Document genere par le systeme depuis un template contrat. Il peut etre verifie, telecharge ou remplace avant signature.'
WHERE code IN ('CONTRACT_DRAFT', 'CONTRACT_TEMPLATE')
  AND help_text IS NULL;

UPDATE document_type
SET help_text = 'Importer la copie signee du contrat final. Le document doit contenir les signatures attendues.'
WHERE code = 'CONTRACT_SIGNED_COPY'
  AND help_text IS NULL;

UPDATE document_type
SET help_text = 'Importer une annexe rattachee au contrat: conditions particulieres, grille tarifaire, avenant ou piece complementaire.'
WHERE code = 'CONTRACT_ANNEX'
  AND help_text IS NULL;
