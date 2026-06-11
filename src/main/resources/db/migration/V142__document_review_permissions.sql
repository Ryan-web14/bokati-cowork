ALTER TABLE permission ALTER COLUMN action TYPE VARCHAR(60);

WITH permission_seed(name, display_name, module, action) AS (
    VALUES
        ('DOCUMENT_REVIEW',                  'Review documents (global)',          'DOCUMENT', 'REVIEW'),
        ('DOCUMENT_REVIEW_KYC_SPACE',        'Review KYC documents',               'DOCUMENT', 'REVIEW_KYC_SPACE'),
        ('DOCUMENT_REVIEW_CONTRACT_SPACE',   'Review contract documents',          'DOCUMENT', 'REVIEW_CONTRACT_SPACE'),
        ('DOCUMENT_REVIEW_FINANCIAL_SPACE',  'Review financial documents',         'DOCUMENT', 'REVIEW_FINANCIAL_SPACE'),
        ('DOCUMENT_REVIEW_ASSET_SPACE',      'Review asset documents',             'DOCUMENT', 'REVIEW_ASSET_SPACE'),
        ('DOCUMENT_REVIEW_ADMINISTRATIVE',   'Review administrative documents',    'DOCUMENT', 'REVIEW_ADMINISTRATIVE'),
        ('DOCUMENT_REVIEW_GENERIC',          'Review generic documents',           'DOCUMENT', 'REVIEW_GENERIC')
),
missing_permissions AS (
    SELECT permission_seed.*, row_number() OVER (ORDER BY name) AS rn
    FROM permission_seed
    WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.name = permission_seed.name)
)
INSERT INTO permission (id, name, display_name, module, action, is_system_permission, is_active)
SELECT COALESCE((SELECT MAX(id) FROM permission), 0) + rn,
       name,
       display_name,
       module,
       action,
       TRUE,
       TRUE
FROM missing_permissions;
