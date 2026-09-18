-- Caisse automatique · exclusive a son moyen de paiement, fermee a toute saisie humaine.
--
-- La caisse automatique du portefeuille existait deja, mais rien n'empechait d'y enregistrer autre
-- chose. Or c'est precisement son homogeneite qui la rend verifiable : le total d'une caisse de
-- portefeuille doit pouvoir se rapprocher du grand livre du portefeuille, et ce rapprochement ne
-- veut plus rien dire des qu'un paiement en especes s'y est glisse.
--
-- Deux marques, deux effets distincts :
--   system_managed       ferme la caisse a la main · ni ouverture de session, ni piece, ni cloture
--   restricted_to_method limite ce qui peut y entrer · une seule nature de paiement

ALTER TABLE cash_register
    ADD COLUMN IF NOT EXISTS system_managed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS restricted_to_method VARCHAR(40);

COMMENT ON COLUMN cash_register.system_managed IS
    'Caisse tenue par le systeme · aucune saisie manuelle n y est acceptee';
COMMENT ON COLUMN cash_register.restricted_to_method IS
    'Unique moyen de paiement admis · nul si la caisse les accepte tous';

-- Les caisses automatiques deja creees prennent leur marque ici plutot qu au premier passage : un
-- systeme redemarre sans nouveau paiement laisserait sinon une caisse ouverte a la saisie.
UPDATE cash_register
SET system_managed = TRUE,
    restricted_to_method = 'WALLET'
WHERE register_code = 'CSR-AUTO-WALLET';

UPDATE cash_register
SET system_managed = TRUE,
    restricted_to_method = 'MOBILE_MONEY'
WHERE register_code = 'CSR-AUTO-MOBILE-MONEY';

-- La caisse generique recueille ce qui ne releve d aucune caisse dediee : elle reste automatique
-- mais n impose aucun moyen, sans quoi un troisieme moyen de paiement n aurait nulle part ou aller.
UPDATE cash_register
SET system_managed = TRUE
WHERE register_code = 'CSR-AUTO-PAYMENT';
