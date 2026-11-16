-- Centre d'audit, lot A4 : erreurs serveur une par une, etat et executions des taches planifiees.

CREATE TABLE IF NOT EXISTS app_error
(
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    trace_id        VARCHAR(64),
    request_id      VARCHAR(64),
    http_method     VARCHAR(10),
    path            VARCHAR(300),
    status          INTEGER       NOT NULL,
    error_code      VARCHAR(60),
    exception_class VARCHAR(200),
    message         VARCHAR(1000),
    root_cause      VARCHAR(500),
    stack_trace     TEXT,
    user_email      VARCHAR(250),
    client_ip       VARCHAR(80)
);
CREATE INDEX IF NOT EXISTS ix_app_error_created ON app_error (created_at DESC);
CREATE INDEX IF NOT EXISTS ix_app_error_request ON app_error (request_id) WHERE request_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_app_error_exception ON app_error (exception_class, created_at DESC);

CREATE TABLE IF NOT EXISTS scheduled_task_status
(
    task_name            VARCHAR(150) PRIMARY KEY,
    last_started_at      TIMESTAMPTZ,
    last_finished_at     TIMESTAMPTZ,
    last_duration_ms     BIGINT,
    last_outcome         VARCHAR(10),
    last_error           VARCHAR(1000),
    last_failure_at      TIMESTAMPTZ,
    run_count            BIGINT       NOT NULL DEFAULT 0,
    failure_count        BIGINT       NOT NULL DEFAULT 0,
    consecutive_failures INTEGER      NOT NULL DEFAULT 0,
    total_duration_ms    BIGINT       NOT NULL DEFAULT 0,
    max_duration_ms      BIGINT       NOT NULL DEFAULT 0,
    instance             VARCHAR(40),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS scheduled_task_run
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    task_name   VARCHAR(150) NOT NULL,
    started_at  TIMESTAMPTZ  NOT NULL,
    finished_at TIMESTAMPTZ  NOT NULL,
    duration_ms BIGINT       NOT NULL,
    outcome     VARCHAR(10)  NOT NULL,
    error       VARCHAR(1000),
    reason      VARCHAR(20)  NOT NULL,
    instance    VARCHAR(40)
);
CREATE INDEX IF NOT EXISTS ix_scheduled_task_run_task ON scheduled_task_run (task_name, started_at DESC);
CREATE INDEX IF NOT EXISTS ix_scheduled_task_run_outcome ON scheduled_task_run (outcome, started_at DESC);
