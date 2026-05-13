INSERT INTO sequence_definition (
    id,
    code,
    name,
    description,
    prefix,
    suffix,
    pattern,
    padding,
    initial_value,
    increment_step,
    reset_policy,
    enabled,
    system_managed,
    created_at,
    updated_at
)
SELECT
    53,
    'customer',
    'Client',
    'Identifiant des clients',
    'CUS',
    NULL,
    '{PREFIX}-{SEQ}',
    6,
    1,
    1,
    'NEVER',
    TRUE,
    FALSE,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM sequence_definition
    WHERE code = 'customer'
);
