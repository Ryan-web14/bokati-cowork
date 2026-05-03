ALTER TABLE resource_pricing_rule
    ADD COLUMN IF NOT EXISTS label VARCHAR(150),
    ADD COLUMN IF NOT EXISTS day_of_week INTEGER,
    ADD COLUMN IF NOT EXISTS starts_at TIME,
    ADD COLUMN IF NOT EXISTS ends_at TIME,
    ADD COLUMN IF NOT EXISTS adjustment_type VARCHAR(30),
    ADD COLUMN IF NOT EXISTS adjustment_value INTEGER,
    ADD COLUMN IF NOT EXISTS valid_from DATE,
    ADD COLUMN IF NOT EXISTS valid_until DATE,
    ADD COLUMN IF NOT EXISTS last_minute_minutes INTEGER,
    ADD COLUMN IF NOT EXISTS priority INTEGER NOT NULL DEFAULT 0;

ALTER TABLE resource_amenity_link
    ADD COLUMN IF NOT EXISTS quantity INTEGER,
    ADD COLUMN IF NOT EXISTS optional BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS extra_price INTEGER;

CREATE TABLE IF NOT EXISTS resource_photo (
    id BIGSERIAL PRIMARY KEY,
    resource_id BIGINT NOT NULL REFERENCES resource(id),
    document_code VARCHAR(120) NOT NULL,
    caption VARCHAR(255),
    cover BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INTEGER,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_resource_photo_resource_active
    ON resource_photo(resource_id, active, display_order);

CREATE TABLE IF NOT EXISTS support_ticket (
    id BIGSERIAL PRIMARY KEY,
    ticket_number VARCHAR(80) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(40) NOT NULL,
    priority VARCHAR(40) NOT NULL,
    category VARCHAR(60) NOT NULL,
    owner_type VARCHAR(40),
    owner_code VARCHAR(120),
    contact_name VARCHAR(160),
    contact_email VARCHAR(180),
    contact_phone VARCHAR(80),
    assigned_to BIGINT,
    related_type VARCHAR(60),
    related_code VARCHAR(120),
    first_response_due_at TIMESTAMP,
    resolution_due_at TIMESTAMP,
    first_responded_at TIMESTAMP,
    resolved_at TIMESTAMP,
    closed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ticket_message (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES support_ticket(id),
    sender_type VARCHAR(40) NOT NULL,
    sender_id VARCHAR(120),
    sender_name VARCHAR(160),
    message TEXT NOT NULL,
    internal BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS ticket_attachment (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES support_ticket(id),
    message_id BIGINT REFERENCES ticket_message(id),
    document_code VARCHAR(120) NOT NULL,
    file_name VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_support_ticket_status_priority ON support_ticket(status, priority);
CREATE INDEX IF NOT EXISTS idx_support_ticket_owner ON support_ticket(owner_type, owner_code);
CREATE INDEX IF NOT EXISTS idx_ticket_message_ticket_created ON ticket_message(ticket_id, created_at);

CREATE TABLE IF NOT EXISTS visitor (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(180) NOT NULL,
    email VARCHAR(180),
    phone VARCHAR(80),
    company VARCHAR(180),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS visitor_pass (
    id BIGSERIAL PRIMARY KEY,
    pass_number VARCHAR(80) NOT NULL UNIQUE,
    visitor_id BIGINT NOT NULL REFERENCES visitor(id),
    host_member_code VARCHAR(120),
    host_name VARCHAR(180),
    valid_from TIMESTAMP NOT NULL,
    valid_until TIMESTAMP NOT NULL,
    purpose VARCHAR(255),
    status VARCHAR(40) NOT NULL,
    qr_value VARCHAR(255),
    created_by VARCHAR(120),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS visitor_check_in (
    id BIGSERIAL PRIMARY KEY,
    pass_id BIGINT NOT NULL REFERENCES visitor_pass(id),
    checked_in_at TIMESTAMP NOT NULL,
    checked_out_at TIMESTAMP,
    check_in_agent VARCHAR(120),
    check_out_agent VARCHAR(120),
    notes TEXT
);

CREATE INDEX IF NOT EXISTS idx_visitor_pass_today ON visitor_pass(valid_from, valid_until, status);
CREATE INDEX IF NOT EXISTS idx_visitor_check_in_pass ON visitor_check_in(pass_id, checked_in_at);

CREATE TABLE IF NOT EXISTS crm_lead (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(180) NOT NULL,
    email VARCHAR(180),
    phone VARCHAR(80),
    company VARCHAR(180),
    source VARCHAR(80),
    interest VARCHAR(255),
    stage VARCHAR(40) NOT NULL,
    estimated_amount NUMERIC(19, 2),
    probability INTEGER,
    expected_close_date DATE,
    assigned_to BIGINT,
    converted_owner_type VARCHAR(40),
    converted_owner_code VARCHAR(120),
    lost_reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS crm_lead_activity (
    id BIGSERIAL PRIMARY KEY,
    lead_id BIGINT NOT NULL REFERENCES crm_lead(id),
    activity_type VARCHAR(40) NOT NULL,
    subject VARCHAR(180),
    notes TEXT,
    performed_by VARCHAR(120),
    performed_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_crm_lead_stage ON crm_lead(stage, assigned_to);
CREATE INDEX IF NOT EXISTS idx_crm_activity_lead ON crm_lead_activity(lead_id, performed_at);

CREATE TABLE IF NOT EXISTS task_item (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    assigned_to BIGINT,
    status VARCHAR(40) NOT NULL,
    priority VARCHAR(40) NOT NULL,
    due_at TIMESTAMP,
    source_type VARCHAR(80),
    source_code VARCHAR(120),
    recurrence VARCHAR(40),
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS task_checklist (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES task_item(id),
    label VARCHAR(255) NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INTEGER
);

CREATE TABLE IF NOT EXISTS task_comment (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES task_item(id),
    author_id VARCHAR(120),
    author_name VARCHAR(160),
    comment TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_task_item_status_due ON task_item(status, due_at);
CREATE INDEX IF NOT EXISTS idx_task_item_assigned_due ON task_item(assigned_to, due_at);

SELECT setval(seq_name::regclass, COALESCE((SELECT MAX(id) FROM permission), 0) + 1, false)
FROM (SELECT pg_get_serial_sequence('permission', 'id') AS seq_name) sequence_info
WHERE seq_name IS NOT NULL;

WITH perms(name, display_name, module, action) AS (
    VALUES
        ('RESOURCE_PRICE', 'Manage resource dynamic pricing', 'RESOURCE', 'PRICE'),
        ('RESOURCE_GALLERY', 'Manage resource gallery', 'RESOURCE', 'GALLERY'),
        ('SUPPORT_READ', 'Read support tickets', 'SUPPORT', 'READ'),
        ('SUPPORT_WRITE', 'Manage support tickets', 'SUPPORT', 'WRITE'),
        ('SUPPORT_ASSIGN', 'Assign support tickets', 'SUPPORT', 'ASSIGN'),
        ('SUPPORT_METRICS', 'Read support metrics', 'SUPPORT', 'METRICS'),
        ('VISITOR_READ', 'Read visitors', 'VISITOR', 'READ'),
        ('VISITOR_WRITE', 'Manage visitor passes', 'VISITOR', 'WRITE'),
        ('VISITOR_CHECKIN', 'Check visitors in and out', 'VISITOR', 'CHECKIN'),
        ('CRM_READ', 'Read CRM leads', 'CRM', 'READ'),
        ('CRM_WRITE', 'Manage CRM leads', 'CRM', 'WRITE'),
        ('CRM_CONVERT', 'Convert CRM leads', 'CRM', 'CONVERT'),
        ('TASK_READ', 'Read tasks', 'TASK', 'READ'),
        ('TASK_WRITE', 'Manage tasks', 'TASK', 'WRITE'),
        ('TASK_ASSIGN', 'Assign tasks', 'TASK', 'ASSIGN')
)
INSERT INTO permission(name, display_name, module, action, is_system_permission, is_active)
SELECT name, display_name, module, action, true, true
FROM perms
WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.name = perms.name);

INSERT INTO role_permission(role_id, permission_id, created_by, is_active)
SELECT r.id, p.id, 'SYSTEM', true
FROM role r
JOIN permission p ON p.name IN (
    'RESOURCE_PRICE', 'RESOURCE_GALLERY',
    'SUPPORT_READ', 'SUPPORT_WRITE', 'SUPPORT_ASSIGN', 'SUPPORT_METRICS',
    'VISITOR_READ', 'VISITOR_WRITE', 'VISITOR_CHECKIN',
    'CRM_READ', 'CRM_WRITE', 'CRM_CONVERT',
    'TASK_READ', 'TASK_WRITE', 'TASK_ASSIGN'
)
WHERE r.name = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permission(role_id, permission_id, created_by, is_active)
SELECT r.id, p.id, 'SYSTEM', true
FROM role r
JOIN permission p ON p.name IN (
    'RESOURCE_READ', 'RESOURCE_WRITE', 'RESOURCE_PRICE', 'RESOURCE_GALLERY',
    'SUPPORT_READ', 'SUPPORT_WRITE', 'SUPPORT_ASSIGN', 'SUPPORT_METRICS',
    'VISITOR_READ', 'VISITOR_WRITE', 'VISITOR_CHECKIN',
    'CRM_READ', 'CRM_WRITE', 'CRM_CONVERT',
    'TASK_READ', 'TASK_WRITE', 'TASK_ASSIGN'
)
WHERE r.name = 'MANAGER'
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permission(role_id, permission_id, created_by, is_active)
SELECT r.id, p.id, 'SYSTEM', true
FROM role r
JOIN permission p ON p.name IN (
    'RESOURCE_READ',
    'SUPPORT_READ', 'SUPPORT_WRITE',
    'VISITOR_READ', 'VISITOR_WRITE', 'VISITOR_CHECKIN',
    'CRM_READ',
    'TASK_READ', 'TASK_WRITE'
)
WHERE r.name = 'STAFF'
  AND NOT EXISTS (
      SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
