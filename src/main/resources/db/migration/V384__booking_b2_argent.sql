-- Lot B2 du module booking : unite imposee gardee au recalcul, quantite retenue sur le droit,
-- couverture partielle par les heures incluses, grille de remboursement par politique.

ALTER TABLE booking
    ADD COLUMN IF NOT EXISTS unit_forced          BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS entitlement_quantity NUMERIC(19, 4),
    ADD COLUMN IF NOT EXISTS covered_minutes      INTEGER;

ALTER TABLE resource_policy
    ADD COLUMN IF NOT EXISTS refund_rules VARCHAR(300);
