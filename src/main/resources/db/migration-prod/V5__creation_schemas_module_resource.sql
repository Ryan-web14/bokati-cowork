

CREATE TABLE IF NOT EXISTS resource_type (
    id BIGINT PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(250) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP  NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_resource_type_code ON resource_type(code);

CREATE TABLE IF NOT EXISTS resource_group (
    id BIGINT PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(250) NOT NULL,
    description TEXT,
    portal_visible BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP  NOT NULL
);

CREATE table IF NOT EXISTS resource_policy(
    id BIGINT PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(250) NOT NULL,
    description TEXT,
    min_booking_duration_minutes INT NOT NULL DEFAULT 1,
    max_booking_duration_minutes INT NOT NULL DEFAULT 1,
    min_booking_notice_minutes INT NOT NULL DEFAULT 1,
    cancellation_notice_minutes INT NOT NULL DEFAULT 1,
    allow_cancellation BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP  NOT NULL

);

CREATE TABLE IF NOT EXISTS resource (
    id BIGINT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    type_id BIGINT NOT NULL,
    group_id BIGINT,
    policy_id BIGINT,

    name VARCHAR(150) NOT NULL,
    description TEXT,

    capacity INT NOT NULL DEFAULT 1,

    zone VARCHAR(100),
    location_label VARCHAR(150),

    status VARCHAR(60) NOT NULL,
    booking_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    portal_visible BOOLEAN NOT NULL DEFAULT TRUE,

    display_order INT,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP  NOT NULL,
    updated_at TIMESTAMP  NOT NULL,

    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT uk_resource_code UNIQUE (code),

    CONSTRAINT fk_resource_type FOREIGN KEY (type_id) REFERENCES resource_type(id),

    CONSTRAINT fk_resource_group FOREIGN KEY (group_id) REFERENCES resource_group(id),

    CONSTRAINT fk_resource_policy FOREIGN KEY (policy_id) REFERENCES resource_policy(id)

);


CREATE INDEX IF NOT EXISTS idx_resource_type_id ON resource(type_id);
CREATE INDEX IF NOT EXISTS id_resource_name ON resource(name);
CREATE INDEX IF NOT EXISTS idx_resource_status ON resource(status);
CREATE INDEX IF NOT EXISTS idx_resource_booking_enabled ON resource(booking_enabled);
CREATE INDEX IF NOT EXISTS idx_resource_portal_visible ON resource(portal_visible);

CREATE TABLE IF NOT EXISTS resource_closure (
    id BIGINT PRIMARY KEY,
    resource_id BIGINT NOT NULL,
    start_at TIMESTAMP  NOT NULL,
    end_at TIMESTAMP  NOT NULL,
    reason VARCHAR(350),
    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP  NOT NULL,
    updated_at TIMESTAMP  NOT NULL,

    CONSTRAINT fk_resource_closure_resource FOREIGN KEY (resource_id) REFERENCES resource(id)
);


CREATE TABLE IF NOT EXISTS resource_pricing_rule (
    id BIGINT PRIMARY KEY,
    resource_id BIGINT NOT NULL,
    unit VARCHAR(50) NOT NULL,
    price INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_resource_pricing_rule_resource FOREIGN KEY (resource_id) REFERENCES resource(id)
);

CREATE TABLE IF NOT EXISTS resource_amenity (
    id BIGINT PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(250) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS resource_amenity_link (
    id BIGINT PRIMARY KEY,
    resource_id BIGSERIAL NOT NULL,
    amenity_id BIGINT NOT NULL,

    CONSTRAINT fk_resource_amenity_link_resource FOREIGN KEY (resource_id) REFERENCES resource(id),

    CONSTRAINT fk_resource_amenity_link_amenity FOREIGN KEY (amenity_id) REFERENCES resource_amenity(id),

    CONSTRAINT uk_resource_amenity UNIQUE (resource_id, amenity_id)
);