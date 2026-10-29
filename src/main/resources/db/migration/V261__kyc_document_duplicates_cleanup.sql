-- Les pieces KYC deposees depuis le portail etaient enregistrees deux fois.
--
-- L'envoi d'un document de categorie KYC cree deja sa ligne kyc_document ; le portail en creait
-- une seconde pour le meme fichier. Un verso devenait en plus une piece a part entiere, a cote du
-- recto qui le portait deja. Le code ne le fait plus ; on retire ici ce qui a ete cree.
--
-- On ne garde, dans un meme dossier, qu'une ligne par fichier (la plus ancienne), et on retire les
-- lignes dont le fichier est le verso d'une autre piece du meme dossier. Les verifications sont
-- rattachees a la ligne gardee ; les resultats d'OCR des doublons, recalculables, sont supprimes.
-- Un meme fichier dans deux dossiers differents (renouvellement) n'est pas touche.

CREATE TEMP TABLE kyc_document_duplicate (
    duplicate_id BIGINT PRIMARY KEY,
    keep_id      BIGINT NOT NULL
) ON COMMIT DROP;

INSERT INTO kyc_document_duplicate (duplicate_id, keep_id)
SELECT d.id, k.keep_id
FROM kyc_document d
JOIN (
    SELECT kyc_case_id, document_id, MIN(id) AS keep_id
    FROM kyc_document
    GROUP BY kyc_case_id, document_id
    HAVING COUNT(*) > 1
) k ON k.document_id = d.document_id
   AND k.kyc_case_id IS NOT DISTINCT FROM d.kyc_case_id
   AND d.id <> k.keep_id;

INSERT INTO kyc_document_duplicate (duplicate_id, keep_id)
SELECT d.id, MIN(f.id)
FROM kyc_document d
JOIN kyc_document f ON f.back_document_id = d.document_id
                   AND f.kyc_case_id IS NOT DISTINCT FROM d.kyc_case_id
                   AND f.id <> d.id
WHERE d.id NOT IN (SELECT duplicate_id FROM kyc_document_duplicate)
  AND d.id NOT IN (SELECT keep_id FROM kyc_document_duplicate)
GROUP BY d.id;

UPDATE kyc_verification v
SET kyc_document_id = x.keep_id
FROM kyc_document_duplicate x
WHERE v.kyc_document_id = x.duplicate_id;

DELETE FROM kyc_document_ocr_result o
USING kyc_document_duplicate x
WHERE o.kyc_document_id = x.duplicate_id;

DELETE FROM kyc_document d
USING kyc_document_duplicate x
WHERE d.id = x.duplicate_id;
