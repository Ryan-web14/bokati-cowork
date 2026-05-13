UPDATE sequence_definition
SET prefix = 'II',
    pattern = '{PREFIX}-{YYYY}{MM}{DD}-{SEQ}',
    padding = 8,
    reset_policy = 'DAILY',
    description = 'Sequence journaliere utilisee pour les codes articles dynamiques',
    updated_at = NOW()
WHERE code = 'inventory_item';
