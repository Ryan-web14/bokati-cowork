UPDATE sequence_definition
SET reset_policy = 'NEVER',
    description = 'Sequence globale utilisee comme suffixe des codes articles dynamiques',
    updated_at = NOW()
WHERE code = 'inventory_item';

ALTER TABLE audit_log
    ALTER COLUMN error_message TYPE TEXT;
