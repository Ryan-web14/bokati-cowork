CREATE TABLE IF NOT EXISTS resource_availability (
    id BIGINT PRIMARY KEY,
    resource_id BIGINT NOT NULL,
    start_at TIMESTAMP NOT NULL,
    end_at TIMESTAMP NOT NULL,
    available BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_resource_availability_resource FOREIGN KEY (resource_id) REFERENCES resource(id)
);

CREATE INDEX IF NOT EXISTS idx_resource_availability_resource_id ON resource_availability(resource_id);
CREATE INDEX IF NOT EXISTS idx_resource_availability_active ON resource_availability(active);
