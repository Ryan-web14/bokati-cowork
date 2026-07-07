CREATE TABLE IF NOT EXISTS event (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    event_date DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS event_registration (
    id BIGSERIAL PRIMARY KEY,
    registration_number VARCHAR(80) UNIQUE NOT NULL,
    event_id BIGINT NOT NULL REFERENCES event(id),
    firstname VARCHAR(180) NOT NULL,
    lastname VARCHAR(180) NOT NULL,
    email VARCHAR(250) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    whatsapp_phone VARCHAR(30),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_VALIDATION',
    rejection_reason VARCHAR(255),
    validated_by BIGINT,
    validated_at TIMESTAMP,
    member_id VARCHAR(60),
    customer_id VARCHAR(60),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_event_registration_status ON event_registration(status);
CREATE INDEX IF NOT EXISTS idx_event_registration_email ON event_registration(email);
CREATE INDEX IF NOT EXISTS idx_event_registration_event ON event_registration(event_id);

-- Événement pré-chargé pour la grande ouverture — actif par défaut, aucune intervention admin requise.
INSERT INTO event (code, name, description, active)
VALUES ('OUVERTURE', 'Grande Ouverture ELLE A OSE',
        'Confirmez votre présence à l''ouverture officielle de notre espace de coworking.', true)
ON CONFLICT (code) DO NOTHING;
