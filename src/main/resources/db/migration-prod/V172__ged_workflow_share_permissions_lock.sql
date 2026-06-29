-- V172: GED phases 5-9 — Workflow, Sharing, Permissions, Locking

-- ═══════════════════════════════════════════════════════════════════
-- PHASE 5: Document Workflow
-- ═══════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS document_workflow
(
    id                 BIGINT       NOT NULL,
    code               VARCHAR(100) NOT NULL,
    name               VARCHAR(300) NOT NULL,
    description        TEXT,
    workflow_type      VARCHAR(30)  NOT NULL DEFAULT 'SIMPLE',
    space              VARCHAR(50),
    document_type_code VARCHAR(100),
    active             BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by         BIGINT,
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_document_workflow PRIMARY KEY (id),
    CONSTRAINT uk_document_workflow_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS document_workflow_step
(
    id                BIGINT       NOT NULL,
    workflow_id       BIGINT       NOT NULL,
    step_order        INTEGER      NOT NULL,
    step_name         VARCHAR(200) NOT NULL,
    approver_type     VARCHAR(30)  NOT NULL,
    approver_value    VARCHAR(200) NOT NULL,
    required          BOOLEAN      NOT NULL DEFAULT TRUE,
    auto_approve_days INTEGER,
    CONSTRAINT pk_document_workflow_step PRIMARY KEY (id),
    CONSTRAINT fk_workflow_step_workflow FOREIGN KEY (workflow_id) REFERENCES document_workflow (id)
);

CREATE INDEX IF NOT EXISTS idx_workflow_step_workflow ON document_workflow_step (workflow_id);

CREATE TABLE IF NOT EXISTS document_workflow_instance
(
    id           BIGINT      NOT NULL,
    workflow_id  BIGINT      NOT NULL,
    document_id  BIGINT      NOT NULL,
    current_step INTEGER     NOT NULL DEFAULT 1,
    status       VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    started_at   TIMESTAMP   NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP,
    started_by   BIGINT,
    CONSTRAINT pk_document_workflow_instance PRIMARY KEY (id),
    CONSTRAINT fk_workflow_instance_workflow FOREIGN KEY (workflow_id) REFERENCES document_workflow (id),
    CONSTRAINT fk_workflow_instance_document FOREIGN KEY (document_id) REFERENCES document (id)
);

CREATE INDEX IF NOT EXISTS idx_workflow_instance_document ON document_workflow_instance (document_id);
CREATE INDEX IF NOT EXISTS idx_workflow_instance_status ON document_workflow_instance (status);

CREATE TABLE IF NOT EXISTS document_workflow_action
(
    id          BIGINT      NOT NULL,
    instance_id BIGINT      NOT NULL,
    step_order  INTEGER     NOT NULL,
    action      VARCHAR(20) NOT NULL,
    actor_id    BIGINT      NOT NULL,
    acted_at    TIMESTAMP   NOT NULL DEFAULT NOW(),
    comment     TEXT,
    CONSTRAINT pk_document_workflow_action PRIMARY KEY (id),
    CONSTRAINT fk_workflow_action_instance FOREIGN KEY (instance_id) REFERENCES document_workflow_instance (id)
);

CREATE INDEX IF NOT EXISTS idx_workflow_action_instance ON document_workflow_action (instance_id);

-- Sequences
INSERT INTO sequence_definition (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 172001, 'WORKFLOW', 'Workflow documentaire', 'Séquence des workflows', 'WFL', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'WORKFLOW');

-- ═══════════════════════════════════════════════════════════════════
-- PHASE 6: Document Sharing
-- ═══════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS document_share_link
(
    id               BIGINT       NOT NULL,
    token            VARCHAR(100) NOT NULL,
    document_id      BIGINT,
    folder_id        BIGINT,
    created_by       BIGINT       NOT NULL,
    expires_at       TIMESTAMP    NOT NULL,
    password_hash    VARCHAR(255),
    allow_download   BOOLEAN      NOT NULL DEFAULT TRUE,
    max_access_count INTEGER,
    access_count     INTEGER      NOT NULL DEFAULT 0,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_accessed_at TIMESTAMP,
    CONSTRAINT pk_document_share_link PRIMARY KEY (id),
    CONSTRAINT uk_document_share_link_token UNIQUE (token),
    CONSTRAINT fk_share_link_document FOREIGN KEY (document_id) REFERENCES document (id),
    CONSTRAINT fk_share_link_folder FOREIGN KEY (folder_id) REFERENCES document_folder (id)
);

CREATE INDEX IF NOT EXISTS idx_share_link_document ON document_share_link (document_id);
CREATE INDEX IF NOT EXISTS idx_share_link_folder ON document_share_link (folder_id);
CREATE INDEX IF NOT EXISTS idx_share_link_token ON document_share_link (token);

-- ═══════════════════════════════════════════════════════════════════
-- PHASE 7: Document Permissions
-- ═══════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS document_permission
(
    id           BIGINT      NOT NULL,
    target_type  VARCHAR(20) NOT NULL,
    target_id    BIGINT      NOT NULL,
    grantee_type VARCHAR(20) NOT NULL,
    grantee_id   BIGINT      NOT NULL,
    permission   VARCHAR(20) NOT NULL,
    granted_by   BIGINT,
    granted_at   TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_document_permission PRIMARY KEY (id),
    CONSTRAINT uk_document_permission UNIQUE (target_type, target_id, grantee_type, grantee_id, permission)
);

CREATE INDEX IF NOT EXISTS idx_document_permission_target ON document_permission (target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_document_permission_grantee ON document_permission (grantee_type, grantee_id);

-- ═══════════════════════════════════════════════════════════════════
-- PHASE 9: Document Locking
-- ═══════════════════════════════════════════════════════════════════

ALTER TABLE document ADD COLUMN IF NOT EXISTS locked_by BIGINT;
ALTER TABLE document ADD COLUMN IF NOT EXISTS locked_at TIMESTAMP;
ALTER TABLE document ADD COLUMN IF NOT EXISTS lock_expires_at TIMESTAMP;
