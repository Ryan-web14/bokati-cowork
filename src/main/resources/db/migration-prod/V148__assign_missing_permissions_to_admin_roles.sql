-- Safety net: assign ALL active permissions to SUPER_ADMIN and ADMIN
INSERT INTO role_permission(role_id, permission_id, created_by, is_active)
SELECT r.id, p.id, 'SYSTEM', true
FROM role r
CROSS JOIN permission p
WHERE r.name IN ('SUPER_ADMIN', 'ADMIN')
  AND p.is_active = true
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Assign all DOCUMENT_REVIEW* permissions to MANAGER
INSERT INTO role_permission(role_id, permission_id, created_by, is_active)
SELECT r.id, p.id, 'SYSTEM', true
FROM role r
JOIN permission p ON p.name IN (
    'DOCUMENT_REVIEW',
    'DOCUMENT_REVIEW_KYC_SPACE',
    'DOCUMENT_REVIEW_CONTRACT_SPACE',
    'DOCUMENT_REVIEW_FINANCIAL_SPACE',
    'DOCUMENT_REVIEW_ASSET_SPACE',
    'DOCUMENT_REVIEW_ADMINISTRATIVE',
    'DOCUMENT_REVIEW_GENERIC'
)
WHERE r.name = 'MANAGER'
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Assign DOCUMENT_REVIEW and KYC-space review to KYC_REVIEWER
INSERT INTO role_permission(role_id, permission_id, created_by, is_active)
SELECT r.id, p.id, 'SYSTEM', true
FROM role r
JOIN permission p ON p.name IN (
    'DOCUMENT_REVIEW',
    'DOCUMENT_REVIEW_KYC_SPACE'
)
WHERE r.name = 'KYC_REVIEWER'
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
