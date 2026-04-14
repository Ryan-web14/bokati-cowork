CREATE INDEX IF NOT EXISTS idx_audit_log_created_at
    ON audit_log(created_at);

CREATE INDEX IF NOT EXISTS idx_audit_log_module_action_status_created_at
    ON audit_log(module, action, audit_status, created_at);

CREATE INDEX IF NOT EXISTS idx_audit_log_actor_email
    ON audit_log(actor_email);

CREATE INDEX IF NOT EXISTS idx_outbox_created_at
    ON outbox_event(created_at);

CREATE INDEX IF NOT EXISTS idx_outbox_aggregate_status_created_at
    ON outbox_event(aggregate_type, status, created_at);

CREATE INDEX IF NOT EXISTS idx_idempotency_created_at
    ON idempotency_record(created_at);

CREATE INDEX IF NOT EXISTS idx_idempotency_operation_status_created_at
    ON idempotency_record(operation, status, created_at);
