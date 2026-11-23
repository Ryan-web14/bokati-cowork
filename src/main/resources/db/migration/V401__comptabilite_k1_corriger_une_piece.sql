-- Lot K1 de la revue comptable (23/11/2026) : corriger une piece mal placee.
-- Reclasser (piece OD qui deplace une ligne), Corriger (contre-passation et piece juste),
-- Recomptabiliser (piece automatique refaite depuis l'evenement, donnees relues a la source).
-- Les pieces de correction sont liees a la piece corrigee ; une piece automatique modifiee a la main
-- garde la trace de ce que le schema avait produit ; un evenement dont la piece est contre-passee
-- hors du moteur passe en REVERSED.

ALTER TABLE acc_entry DROP CONSTRAINT IF EXISTS ck_acc_entry_origin;
ALTER TABLE acc_entry ADD CONSTRAINT ck_acc_entry_origin
    CHECK (origin IN ('MANUAL', 'CASH', 'PLATFORM', 'IMPORT', 'CLOSING', 'OPENING', 'REVERSAL', 'REGULARIZATION', 'CORRECTION'));

ALTER TABLE acc_entry ADD COLUMN IF NOT EXISTS correction_of VARCHAR(60);
ALTER TABLE acc_entry ADD COLUMN IF NOT EXISTS correction_kind VARCHAR(20);
ALTER TABLE acc_entry ADD COLUMN IF NOT EXISTS corrected_by VARCHAR(60);
ALTER TABLE acc_entry ADD COLUMN IF NOT EXISTS generated_lines_json JSONB;
ALTER TABLE acc_entry ADD COLUMN IF NOT EXISTS manually_changed_by VARCHAR(180);
ALTER TABLE acc_entry ADD COLUMN IF NOT EXISTS manually_changed_at TIMESTAMPTZ;
ALTER TABLE acc_entry ADD COLUMN IF NOT EXISTS change_reason VARCHAR(500);

ALTER TABLE acc_entry DROP CONSTRAINT IF EXISTS ck_acc_entry_correction_kind;
ALTER TABLE acc_entry ADD CONSTRAINT ck_acc_entry_correction_kind
    CHECK (correction_kind IS NULL OR correction_kind IN ('RECLASS', 'CORRECTION', 'REPOST'));

CREATE INDEX IF NOT EXISTS ix_acc_entry_correction_of ON acc_entry (correction_of);
CREATE INDEX IF NOT EXISTS ix_acc_entry_reversal_of_k1 ON acc_entry (reversal_of);

ALTER TABLE acc_posting_event DROP CONSTRAINT IF EXISTS ck_acc_posting_event_status;
ALTER TABLE acc_posting_event ADD CONSTRAINT ck_acc_posting_event_status
    CHECK (status IN ('PENDING', 'POSTED', 'UNBALANCED', 'FAILED', 'IGNORED', 'REVERSED'));

-- Le mouvement de caisse suit l'etat de son evenement comptable.
ALTER TABLE cash_movement DROP CONSTRAINT IF EXISTS ck_cash_movement_accounting_status;
ALTER TABLE cash_movement ADD CONSTRAINT ck_cash_movement_accounting_status
    CHECK (accounting_status IN ('NOT_APPLICABLE', 'PENDING', 'QUEUED', 'POSTED', 'UNBALANCED', 'FAILED', 'IGNORED', 'REVERSED'));
