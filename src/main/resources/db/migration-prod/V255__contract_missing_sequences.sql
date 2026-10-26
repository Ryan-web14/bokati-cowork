-- Les brouillons de contrat, les avenants et les modeles de contrat demandent chacun une sequence
-- (CONTRACT_DRAFT, AMENDMENT, CONTRACT_TEMPLATE) que rien n avait semee · leur creation echouait
-- sur une definition introuvable.

INSERT INTO sequence_definition (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2590001, 'contract_draft', 'Brouillon de contrat', 'Sequence des brouillons de contrat', 'CTD', null, '{PREFIX}-{YYYY}-{SEQ}', 6, 1, 1, 'YEARLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE lower(code) = 'contract_draft');

INSERT INTO sequence_definition (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2590002, 'amendment', 'Avenant', 'Sequence des avenants de contrat', 'AVT', null, '{PREFIX}-{YYYY}-{SEQ}', 6, 1, 1, 'YEARLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE lower(code) = 'amendment');

INSERT INTO sequence_definition (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2590003, 'contract_template', 'Modele de contrat', 'Sequence des modeles de contrat', 'CTM', null, '{PREFIX}-{SEQ}', 4, 1, 1, 'NEVER', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE lower(code) = 'contract_template');
