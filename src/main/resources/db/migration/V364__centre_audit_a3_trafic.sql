-- Centre d'audit, lot A3 : trafic par minute et par route (gabarit), cumul horaire, requetes lentes
-- ou en erreur serveur une par une.

CREATE TABLE IF NOT EXISTS traffic_minute
(
    minute        TIMESTAMPTZ  NOT NULL,
    http_method   VARCHAR(10)  NOT NULL,
    route         VARCHAR(300) NOT NULL,
    request_count BIGINT       NOT NULL,
    client_errors BIGINT       NOT NULL,
    server_errors BIGINT       NOT NULL,
    total_ms      BIGINT       NOT NULL,
    max_ms        BIGINT       NOT NULL,
    histogram     INTEGER[]    NOT NULL,
    PRIMARY KEY (minute, http_method, route)
);

CREATE TABLE IF NOT EXISTS traffic_hour
(
    hour          TIMESTAMPTZ  NOT NULL,
    http_method   VARCHAR(10)  NOT NULL,
    route         VARCHAR(300) NOT NULL,
    request_count BIGINT       NOT NULL,
    client_errors BIGINT       NOT NULL,
    server_errors BIGINT       NOT NULL,
    total_ms      BIGINT       NOT NULL,
    max_ms        BIGINT       NOT NULL,
    histogram     INTEGER[]    NOT NULL,
    PRIMARY KEY (hour, http_method, route)
);

CREATE TABLE IF NOT EXISTS request_log
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at  TIMESTAMPTZ  NOT NULL,
    request_id  VARCHAR(64),
    http_method VARCHAR(10)  NOT NULL,
    path        VARCHAR(300) NOT NULL,
    route       VARCHAR(300) NOT NULL,
    status      INTEGER      NOT NULL,
    duration_ms BIGINT       NOT NULL,
    user_email  VARCHAR(250),
    client_ip   VARCHAR(80),
    reason      VARCHAR(10)  NOT NULL,
    instance    VARCHAR(40)
);
CREATE INDEX IF NOT EXISTS ix_request_log_created ON request_log (created_at DESC);
CREATE INDEX IF NOT EXISTS ix_request_log_request ON request_log (request_id) WHERE request_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_request_log_route ON request_log (route, created_at DESC);
