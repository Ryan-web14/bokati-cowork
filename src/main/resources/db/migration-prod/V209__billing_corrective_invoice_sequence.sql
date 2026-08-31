-- =====================================================================================
-- Sequence des factures rectificatives
--
-- BillingNumberingSupport resout CORRECTIVE_INVOICE vers le code 'billing_corrective_invoice',
-- mais ce code n'a jamais ete seme : V68 a pose les cinq autres types de document et a oublie
-- celui-ci. Une facture rectificative ne pouvait donc pas etre creee du tout, l'emission
-- echouant sur « Sequence definition not found for code: billing_corrective_invoice ».
--
-- Meme forme que les autres sequences de facturation, remise a zero mensuelle comprise.
--
-- L'identifiant est calcule a partir du maximum existant et non fige : sequence_definition n'a
-- pas de sequence propre, et prolonger a la main la serie 6800xx de V68 se heurte a un
-- identifiant deja pris par une migration posterieure — c'est la convention suivie depuis V87.
-- =====================================================================================

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step,
 reset_policy, enabled, system_managed, created_at, updated_at)
SELECT COALESCE((SELECT MAX(id) FROM sequence_definition), 0) + 1,
       'billing_corrective_invoice',
       'Facture rectificative',
       'Sequence des factures rectificatives',
       'REC',
       null,
       '{PREFIX}-{YYYY}{MM}-{SEQ}',
       6, 1, 1,
       'MONTHLY',
       true,
       false,
       now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'billing_corrective_invoice');


-- -------------------------------------------------------------------------------------
-- Garde-fou · sans cette sequence, l'emission d'une rectificative echoue a l'appel et non
-- au deploiement. Autant le signaler ici.
-- -------------------------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'billing_corrective_invoice') THEN
        RAISE WARNING 'V209 : la sequence billing_corrective_invoice n''a pas ete creee · les factures rectificatives resteront impossibles a emettre.';
    END IF;
END $$;
