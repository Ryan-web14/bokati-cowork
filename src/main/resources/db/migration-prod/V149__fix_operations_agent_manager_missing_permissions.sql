-- OPERATIONS_AGENT: add subscription read/activate and inventory read
-- (V91 omitted these; an ops agent needs to view client subscriptions and stock)
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

-- MANAGER: add full inventory permissions (all omitted from V91)
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

-- STAFF: add subscription read so staff can see a client's active pass when booking
INSERT INTO role_permission(role_id, permission_id, created_by, is_active)
SELECT r.id, p.id, 'SYSTEM', true
FROM role r
JOIN permission p ON p.name = 'SUBSCRIPTION_READ'
WHERE r.name = 'STAFF'
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
