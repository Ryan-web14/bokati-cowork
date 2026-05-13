INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 600001, 'entitlement_definition', 'Definition droit abonnement', 'Sequence des definitions de droits abonnement', 'EDF', null, '{PREFIX}-{SEQ}', 5, 1, 1, 'NEVER', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'entitlement_definition');

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 600002, 'promotion', 'Promotion abonnement', 'Sequence des promotions abonnement', 'PRO', null, '{PREFIX}-{SEQ}', 5, 1, 1, 'NEVER', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'promotion');
