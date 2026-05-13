ALTER TABLE role
    ADD COLUMN IF NOT EXISTS display_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS is_system_role BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS version INTEGER,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE permission
    ADD COLUMN IF NOT EXISTS display_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS module VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    ADD COLUMN IF NOT EXISTS action VARCHAR(20) NOT NULL DEFAULT 'READ',
    ADD COLUMN IF NOT EXISTS is_system_permission BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS user_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS email VARCHAR(350),
    ADD COLUMN IF NOT EXISTS "passwordHash" VARCHAR(250),
    ADD COLUMN IF NOT EXISTS last_login TIMESTAMP,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS is_account_expired BOOLEAN,
    ADD COLUMN IF NOT EXISTS is_account_locked BOOLEAN,
    ADD COLUMN IF NOT EXISTS is_account_enabled BOOLEAN,
    ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER;

ALTER TABLE role_user
    ADD COLUMN IF NOT EXISTS assigned_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS assigned_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

ALTER TABLE role_permission
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX IF NOT EXISTS idx_role_active_name
    ON role(is_active, name);

CREATE INDEX IF NOT EXISTS idx_permission_module_action
    ON permission(module, action);

CREATE INDEX IF NOT EXISTS idx_permission_active_name
    ON permission(is_active, name);

CREATE INDEX IF NOT EXISTS idx_users_email_deleted
    ON users(email, deleted);

CREATE INDEX IF NOT EXISTS idx_users_enabled_locked
    ON users(is_account_enabled, is_account_locked);

CREATE INDEX IF NOT EXISTS idx_role_user_role_id_assigned_at
    ON role_user(role_id, assigned_at);

CREATE INDEX IF NOT EXISTS idx_role_permission_role_id_is_active
    ON role_permission(role_id, is_active);
