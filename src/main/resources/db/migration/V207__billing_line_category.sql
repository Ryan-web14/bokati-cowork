-- =====================================================================================
-- Categorie sur la ligne de document de facturation (devis et facture)
--
-- La categorie n'existait que sur service_catalog_item : une ligne de facture perdait donc
-- l'information des sa creation et le PDF ne pouvait pas la restituer. On la fige sur la
-- ligne au lieu de la relire du catalogue a l'impression, pour la meme raison que
-- seller_name / customer_niu sont figes sur billing_document : un document emis doit rester
-- identique a l'impression meme si le catalogue evolue ensuite.
--
-- Nullable : les lignes libres (sans item_code) et tout l'historique n'ont pas de categorie.
-- =====================================================================================
ALTER TABLE billing_document_line
    ADD COLUMN IF NOT EXISTS category VARCHAR(100);

-- Regroupement / filtrage par categorie dans les rapports de facturation.
CREATE INDEX IF NOT EXISTS idx_billing_document_line_category
    ON billing_document_line (category)
    WHERE category IS NOT NULL;
