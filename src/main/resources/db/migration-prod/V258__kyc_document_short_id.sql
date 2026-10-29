-- Un numero court pour chaque piece KYC, renvoye a la place de l identifiant genere.
--
-- L identifiant genere depasse 2^53 · JavaScript l arrondit en le lisant, et le portail demandait
-- ensuite le telechargement d une piece qui n existait pas (404). Meme remede que pour les regles
-- de tarif : une sequence, les pieces existantes numerotees dans leur ordre de creation, les
-- suivantes a l insertion par la valeur par defaut de la colonne.
CREATE SEQUENCE IF NOT EXISTS kyc_document_short_id_seq START WITH 1;

ALTER TABLE kyc_document ADD COLUMN IF NOT EXISTS short_id BIGINT;

UPDATE kyc_document doc
SET short_id = numbered.n
FROM (SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS n
      FROM kyc_document) numbered
WHERE doc.id = numbered.id
  AND doc.short_id IS NULL;

SELECT setval('kyc_document_short_id_seq',
              GREATEST((SELECT COALESCE(MAX(short_id), 0) FROM kyc_document), 1),
              (SELECT COUNT(*) > 0 FROM kyc_document));

ALTER TABLE kyc_document ALTER COLUMN short_id SET DEFAULT nextval('kyc_document_short_id_seq');
ALTER TABLE kyc_document ALTER COLUMN short_id SET NOT NULL;
ALTER SEQUENCE kyc_document_short_id_seq OWNED BY kyc_document.short_id;

CREATE UNIQUE INDEX IF NOT EXISTS uk_kyc_document_short_id ON kyc_document (short_id);
