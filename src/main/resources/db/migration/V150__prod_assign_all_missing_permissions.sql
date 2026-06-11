-- ============================================================
-- V150 — Production permission safety net
-- Combines V148 + V149 into one idempotent script so prod
-- converges to the same state as dev without manual patching.
-- All INSERTs are guarded by NOT EXISTS — safe to re-run.
-- ============================================================

-- 1. Give SUPER_ADMIN and ADMIN every active permission in the system
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

-- 2. Give MANAGER all DOCUMENT_REVIEW* permissions
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

-- 3. Give KYC_REVIEWER document-review permissions for the KYC space
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

-- 4. Give OPERATIONS_AGENT subscription and inventory read/activate
INSERT INTO role_permission(role_id, permission_id, created_by, is_active)
SELECT r.id, p.id, 'SYSTEM', true
FROM role r
JOIN permission p ON p.name IN (
    'SUBSCRIPTION_READ',
    'SUBSCRIPTION_ACTIVATE',
    'INVENTORY_READ'
)
WHERE r.name = 'OPERATIONS_AGENT'
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 5. Give MANAGER full inventory permissions
INSERT INTO role_permission(role_id, permission_id, created_by, is_active)
SELECT r.id, p.id, 'SYSTEM', true
FROM role r
JOIN permission p ON p.name IN (
    'INVENTORY_READ',
    'INVENTORY_CREATE',
    'INVENTORY_UPDATE',
    'INVENTORY_DELETE',
    'INVENTORY_APPROVE'
)
WHERE r.name = 'MANAGER'
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 6. Give STAFF subscription read (needed to view client passes during booking)
INSERT INTO role_permission(role_id, permission_id, created_by, is_active)
SELECT r.id, p.id, 'SYSTEM', true
FROM role r
JOIN permission p ON p.name = 'SUBSCRIPTION_READ'
WHERE r.name = 'STAFF'
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
