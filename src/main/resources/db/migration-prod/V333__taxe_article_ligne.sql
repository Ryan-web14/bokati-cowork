-- Lot C1 (facturation, complements) : la regle de taxe de l'article (code de base) est figee sur
-- la ligne qui l'a appliquee ; elle sert a imprimer la mention et le motif d'exoneration.
ALTER TABLE billing_document_line ADD COLUMN IF NOT EXISTS tax_rule_code VARCHAR(120);
