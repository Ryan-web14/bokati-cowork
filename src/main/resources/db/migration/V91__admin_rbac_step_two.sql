WITH role_seed(name, display_name, description) AS (
    VALUES
        ('SUPER_ADMIN', 'Super administrateur', 'Acces complet a la plateforme et a la securite'),
        ('FINANCE', 'Finance', 'Facturation, paiements et rapports financiers'),
        ('CASHIER', 'Caissier', 'Encaissement et gestion des sessions de caisse'),
        ('KYC_REVIEWER', 'Controle KYC', 'Verification et decision sur les documents KYC'),
        ('SUPPORT', 'Support', 'Gestion des demandes de support'),
        ('AUDITOR', 'Auditeur', 'Consultation, audit et rapports'),
        ('VIEWER', 'Lecteur', 'Acces limite en lecture'),
        ('OPERATIONS_AGENT', 'Agent operations', 'Finance, caisse, accueil et support sans remboursement ni modification des prix')
),
missing_roles AS (
    SELECT role_seed.*, row_number() OVER (ORDER BY name) AS rn
    FROM role_seed
    WHERE NOT EXISTS (SELECT 1 FROM role r WHERE r.name = role_seed.name)
)
INSERT INTO role (id, name, display_name, description, is_system_role, version, created_at, updated_at, is_active)
SELECT COALESCE((SELECT MAX(id) FROM role), 0) + rn,
       name,
       display_name,
       description,
       true,
       0,
       now(),
       now(),
       true
FROM missing_roles;

WITH permission_seed(name, display_name, module, action) AS (
    VALUES
        ('ADMIN_ACCESS', 'Access admin API', 'ADMIN', 'ACCESS'),
        ('SYSTEM_USERS', 'Manage admin users', 'SYSTEM', 'USERS'),
        ('SYSTEM_ROLES', 'Manage roles', 'SYSTEM', 'ROLES'),
        ('SYSTEM_PERMISSIONS', 'Manage permissions', 'SYSTEM', 'PERMISSIONS'),
        ('SYSTEM_AUDIT', 'Read audit logs', 'SYSTEM', 'AUDIT'),
        ('SYSTEM_SETTINGS', 'Manage system settings', 'SYSTEM', 'SETTINGS'),
        ('BILLING_READ', 'Read billing documents', 'BILLING', 'READ'),
        ('BILLING_CREATE', 'Create billing documents', 'BILLING', 'CREATE'),
        ('BILLING_UPDATE', 'Update billing documents', 'BILLING', 'UPDATE'),
        ('BILLING_SEND', 'Send billing documents', 'BILLING', 'SEND'),
        ('BILLING_CANCEL', 'Cancel billing documents', 'BILLING', 'CANCEL'),
        ('PAYMENT_READ', 'Read payments', 'PAYMENT', 'READ'),
        ('PAYMENT_PROCESS', 'Process payments', 'PAYMENT', 'PROCESS'),
        ('PAYMENT_REFUND', 'Refund payments', 'PAYMENT', 'REFUND'),
        ('CASH_READ', 'Read cash register', 'CASH', 'READ'),
        ('CASH_OPEN_SESSION', 'Open cash sessions', 'CASH', 'OPEN_SESSION'),
        ('CASH_CLOSE_SESSION', 'Close cash sessions', 'CASH', 'CLOSE_SESSION'),
        ('CASH_ADJUST', 'Adjust cash sessions', 'CASH', 'ADJUST'),
        ('BOOKING_READ', 'Read bookings', 'BOOKING', 'READ'),
        ('BOOKING_CREATE', 'Create bookings', 'BOOKING', 'CREATE'),
        ('BOOKING_UPDATE', 'Update bookings', 'BOOKING', 'UPDATE'),
        ('BOOKING_CANCEL', 'Cancel bookings', 'BOOKING', 'CANCEL'),
        ('BOOKING_CHECKIN', 'Check bookings in and out', 'BOOKING', 'CHECKIN'),
        ('CLIENT_READ', 'Read clients', 'CLIENT', 'READ'),
        ('CLIENT_CREATE', 'Create clients', 'CLIENT', 'CREATE'),
        ('CLIENT_UPDATE', 'Update clients', 'CLIENT', 'UPDATE'),
        ('CLIENT_DELETE', 'Delete clients', 'CLIENT', 'DELETE'),
        ('KYC_READ', 'Read KYC documents', 'KYC', 'READ'),
        ('KYC_APPROVE', 'Approve KYC documents', 'KYC', 'APPROVE'),
        ('KYC_REJECT', 'Reject KYC documents', 'KYC', 'REJECT'),
        ('DOCUMENT_READ', 'Read documents', 'DOCUMENT', 'READ'),
        ('DOCUMENT_UPLOAD', 'Upload documents', 'DOCUMENT', 'UPLOAD'),
        ('DOCUMENT_APPROVE', 'Approve documents', 'DOCUMENT', 'APPROVE'),
        ('SUBSCRIPTION_READ', 'Read subscriptions', 'SUBSCRIPTION', 'READ'),
        ('SUBSCRIPTION_CREATE', 'Create subscriptions', 'SUBSCRIPTION', 'CREATE'),
        ('SUBSCRIPTION_UPDATE', 'Update subscriptions', 'SUBSCRIPTION', 'UPDATE'),
        ('SUBSCRIPTION_ACTIVATE', 'Activate subscriptions', 'SUBSCRIPTION', 'ACTIVATE'),
        ('SUBSCRIPTION_CANCEL', 'Cancel subscriptions', 'SUBSCRIPTION', 'CANCEL'),
        ('INVENTORY_READ', 'Read inventory', 'INVENTORY', 'READ'),
        ('INVENTORY_CREATE', 'Create inventory records', 'INVENTORY', 'CREATE'),
        ('INVENTORY_UPDATE', 'Update inventory records', 'INVENTORY', 'UPDATE'),
        ('INVENTORY_DELETE', 'Delete inventory records', 'INVENTORY', 'DELETE'),
        ('INVENTORY_APPROVE', 'Approve inventory workflows', 'INVENTORY', 'APPROVE'),
        ('RESOURCE_READ', 'Read resources', 'RESOURCE', 'READ'),
        ('RESOURCE_CREATE', 'Create resources', 'RESOURCE', 'CREATE'),
        ('RESOURCE_UPDATE', 'Update resources', 'RESOURCE', 'UPDATE'),
        ('RESOURCE_DELETE', 'Delete resources', 'RESOURCE', 'DELETE'),
        ('RESOURCE_PRICE', 'Manage resource prices', 'RESOURCE', 'PRICE'),
        ('REPORT_VIEW', 'View reports', 'REPORT', 'VIEW'),
        ('REPORT_EXPORT', 'Export reports', 'REPORT', 'EXPORT'),
        ('VISITOR_READ', 'Read visitors', 'VISITOR', 'READ'),
        ('VISITOR_WRITE', 'Manage visitor passes', 'VISITOR', 'WRITE'),
        ('VISITOR_CHECKIN', 'Check visitors in and out', 'VISITOR', 'CHECKIN'),
        ('SUPPORT_READ', 'Read support tickets', 'SUPPORT', 'READ'),
        ('SUPPORT_WRITE', 'Manage support tickets', 'SUPPORT', 'WRITE'),
        ('SUPPORT_ASSIGN', 'Assign support tickets', 'SUPPORT', 'ASSIGN'),
        ('SUPPORT_METRICS', 'Read support metrics', 'SUPPORT', 'METRICS'),
        ('TASK_READ', 'Read tasks', 'TASK', 'READ'),
        ('TASK_WRITE', 'Manage tasks', 'TASK', 'WRITE'),
        ('TASK_ASSIGN', 'Assign tasks', 'TASK', 'ASSIGN'),
        ('CRM_READ', 'Read CRM leads', 'CRM', 'READ'),
        ('CRM_WRITE', 'Manage CRM leads', 'CRM', 'WRITE'),
        ('CRM_CONVERT', 'Convert CRM leads', 'CRM', 'CONVERT')
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
       true,
       true
FROM missing_permissions;

WITH role_permission_seed(role_name, permission_name) AS (
    SELECT r.name, p.name
    FROM role r
    CROSS JOIN permission p
    WHERE r.name IN ('SUPER_ADMIN', 'ADMIN')
      AND p.name IN (
          'ADMIN_ACCESS',
          'SYSTEM_USERS', 'SYSTEM_ROLES', 'SYSTEM_PERMISSIONS', 'SYSTEM_AUDIT', 'SYSTEM_SETTINGS',
          'BILLING_READ', 'BILLING_CREATE', 'BILLING_UPDATE', 'BILLING_SEND', 'BILLING_CANCEL',
          'PAYMENT_READ', 'PAYMENT_PROCESS', 'PAYMENT_REFUND',
          'CASH_READ', 'CASH_OPEN_SESSION', 'CASH_CLOSE_SESSION', 'CASH_ADJUST',
          'BOOKING_READ', 'BOOKING_CREATE', 'BOOKING_UPDATE', 'BOOKING_CANCEL', 'BOOKING_CHECKIN',
          'CLIENT_READ', 'CLIENT_CREATE', 'CLIENT_UPDATE', 'CLIENT_DELETE',
          'KYC_READ', 'KYC_APPROVE', 'KYC_REJECT',
          'DOCUMENT_READ', 'DOCUMENT_UPLOAD', 'DOCUMENT_APPROVE',
          'SUBSCRIPTION_READ', 'SUBSCRIPTION_CREATE', 'SUBSCRIPTION_UPDATE', 'SUBSCRIPTION_ACTIVATE', 'SUBSCRIPTION_CANCEL',
          'INVENTORY_READ', 'INVENTORY_CREATE', 'INVENTORY_UPDATE', 'INVENTORY_DELETE', 'INVENTORY_APPROVE',
          'RESOURCE_READ', 'RESOURCE_CREATE', 'RESOURCE_UPDATE', 'RESOURCE_DELETE', 'RESOURCE_PRICE',
          'REPORT_VIEW', 'REPORT_EXPORT',
          'VISITOR_READ', 'VISITOR_WRITE', 'VISITOR_CHECKIN',
          'SUPPORT_READ', 'SUPPORT_WRITE', 'SUPPORT_ASSIGN', 'SUPPORT_METRICS',
          'TASK_READ', 'TASK_WRITE', 'TASK_ASSIGN',
          'CRM_READ', 'CRM_WRITE', 'CRM_CONVERT'
      )
    UNION ALL
    SELECT 'MANAGER', name FROM permission WHERE name IN (
        'ADMIN_ACCESS',
        'BILLING_READ', 'BILLING_CREATE', 'BILLING_UPDATE', 'BILLING_SEND', 'BILLING_CANCEL',
        'PAYMENT_READ', 'PAYMENT_PROCESS', 'PAYMENT_REFUND',
        'CASH_READ', 'CASH_OPEN_SESSION', 'CASH_CLOSE_SESSION', 'CASH_ADJUST',
        'BOOKING_READ', 'BOOKING_CREATE', 'BOOKING_UPDATE', 'BOOKING_CANCEL', 'BOOKING_CHECKIN',
        'CLIENT_READ', 'CLIENT_CREATE', 'CLIENT_UPDATE',
        'KYC_READ', 'KYC_APPROVE', 'KYC_REJECT',
        'DOCUMENT_READ', 'DOCUMENT_UPLOAD',
        'SUBSCRIPTION_READ', 'SUBSCRIPTION_CREATE', 'SUBSCRIPTION_UPDATE', 'SUBSCRIPTION_ACTIVATE', 'SUBSCRIPTION_CANCEL',
        'RESOURCE_READ', 'RESOURCE_CREATE', 'RESOURCE_UPDATE', 'REPORT_VIEW', 'REPORT_EXPORT',
        'VISITOR_READ', 'VISITOR_WRITE', 'VISITOR_CHECKIN',
        'SUPPORT_READ', 'SUPPORT_WRITE', 'SUPPORT_ASSIGN', 'SUPPORT_METRICS',
        'TASK_READ', 'TASK_WRITE', 'TASK_ASSIGN', 'CRM_READ', 'CRM_WRITE', 'CRM_CONVERT'
    )
    UNION ALL
    SELECT 'FINANCE', name FROM permission WHERE name IN (
        'ADMIN_ACCESS', 'BILLING_READ', 'BILLING_CREATE', 'BILLING_UPDATE', 'BILLING_SEND', 'BILLING_CANCEL',
        'PAYMENT_READ', 'PAYMENT_PROCESS', 'PAYMENT_REFUND',
        'CASH_READ', 'REPORT_VIEW', 'REPORT_EXPORT'
    )
    UNION ALL
    SELECT 'CASHIER', name FROM permission WHERE name IN (
        'ADMIN_ACCESS', 'BILLING_READ', 'PAYMENT_READ', 'PAYMENT_PROCESS',
        'CASH_READ', 'CASH_OPEN_SESSION', 'CASH_CLOSE_SESSION', 'CASH_ADJUST', 'REPORT_VIEW'
    )
    UNION ALL
    SELECT 'STAFF', name FROM permission WHERE name IN (
        'ADMIN_ACCESS', 'BOOKING_READ', 'BOOKING_CREATE', 'BOOKING_UPDATE', 'BOOKING_CANCEL', 'BOOKING_CHECKIN',
        'CLIENT_READ', 'CLIENT_CREATE', 'CLIENT_UPDATE',
        'RESOURCE_READ', 'DOCUMENT_READ', 'VISITOR_READ', 'VISITOR_WRITE', 'VISITOR_CHECKIN',
        'TASK_READ', 'TASK_WRITE'
    )
    UNION ALL
    SELECT 'SUPPORT', name FROM permission WHERE name IN (
        'ADMIN_ACCESS', 'CLIENT_READ', 'BOOKING_READ', 'DOCUMENT_READ',
        'SUPPORT_READ', 'SUPPORT_WRITE', 'SUPPORT_ASSIGN', 'SUPPORT_METRICS',
        'TASK_READ', 'TASK_WRITE'
    )
    UNION ALL
    SELECT 'KYC_REVIEWER', name FROM permission WHERE name IN (
        'ADMIN_ACCESS', 'CLIENT_READ', 'DOCUMENT_READ', 'KYC_READ', 'KYC_APPROVE', 'KYC_REJECT'
    )
    UNION ALL
    SELECT 'AUDITOR', name FROM permission WHERE name IN (
        'ADMIN_ACCESS', 'SYSTEM_AUDIT',
        'BILLING_READ', 'PAYMENT_READ', 'CASH_READ', 'BOOKING_READ', 'CLIENT_READ',
        'KYC_READ', 'DOCUMENT_READ', 'SUBSCRIPTION_READ', 'INVENTORY_READ',
        'RESOURCE_READ', 'REPORT_VIEW', 'REPORT_EXPORT', 'VISITOR_READ', 'SUPPORT_READ',
        'TASK_READ', 'CRM_READ'
    )
    UNION ALL
    SELECT 'VIEWER', name FROM permission WHERE name IN (
        'ADMIN_ACCESS', 'BILLING_READ', 'PAYMENT_READ', 'CASH_READ', 'BOOKING_READ',
        'CLIENT_READ', 'KYC_READ', 'DOCUMENT_READ', 'SUBSCRIPTION_READ', 'INVENTORY_READ',
        'RESOURCE_READ', 'REPORT_VIEW', 'VISITOR_READ', 'SUPPORT_READ', 'TASK_READ', 'CRM_READ'
    )
    UNION ALL
    SELECT 'OPERATIONS_AGENT', name FROM permission WHERE name IN (
        'ADMIN_ACCESS',
        'BILLING_READ', 'BILLING_CREATE', 'BILLING_SEND',
        'PAYMENT_READ', 'PAYMENT_PROCESS',
        'CASH_READ', 'CASH_OPEN_SESSION', 'CASH_CLOSE_SESSION', 'CASH_ADJUST',
        'BOOKING_READ', 'BOOKING_CREATE', 'BOOKING_UPDATE', 'BOOKING_CANCEL', 'BOOKING_CHECKIN',
        'CLIENT_READ', 'CLIENT_CREATE', 'CLIENT_UPDATE',
        'RESOURCE_READ', 'DOCUMENT_READ',
        'REPORT_VIEW',
        'VISITOR_READ', 'VISITOR_WRITE', 'VISITOR_CHECKIN',
        'SUPPORT_READ', 'SUPPORT_WRITE', 'SUPPORT_ASSIGN', 'SUPPORT_METRICS',
        'TASK_READ', 'TASK_WRITE'
    )
)
INSERT INTO role_permission(role_id, permission_id, created_by, is_active)
SELECT r.id, p.id, 'SYSTEM', true
FROM role_permission_seed seed
JOIN role r ON r.name = seed.role_name
JOIN permission p ON p.name = seed.permission_name
WHERE NOT EXISTS (
    SELECT 1
    FROM role_permission rp
    WHERE rp.role_id = r.id
      AND rp.permission_id = p.id
);
