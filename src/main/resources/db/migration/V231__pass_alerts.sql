-- Alertes sur les pass.
--
-- Trois situations qu'un titulaire decouvre aujourd'hui trop tard : un pass qui expire dans
-- quelques jours, un solde presque epuise, et un pass achete puis jamais utilise. Les deux
-- premieres coutent au client, la troisieme coute a la relation commerciale · un pass jamais
-- utilise est un client qui ne reviendra pas, et personne ne l'apprend avant qu'il ne parte.
--
-- L'infrastructure de notification existe deja, avec sa file, son ordonnanceur et ses canaux. Ce
-- qui manque n'est pas l'envoi, c'est la detection et la memoire de ce qui a deja ete annonce.
--
-- Cette table est cette memoire. Sans elle, un traitement quotidien renverrait la meme alerte tous
-- les jours jusqu'a l'expiration du pass, ce qui est la facon la plus sure de faire ignorer les
-- alertes suivantes.

CREATE TABLE IF NOT EXISTS pass_alert (
    id BIGINT PRIMARY KEY,
    pass_id BIGINT NOT NULL,
    alert_type VARCHAR(40) NOT NULL,
    -- Ce qui a declenche l'alerte · une date d'expiration, un seuil de solde. Deux alertes du meme
    -- type sur des declencheurs differents sont deux alertes distinctes : un pass renouvele porte
    -- une nouvelle echeance, et merite d'etre annonce a nouveau.
    threshold_key VARCHAR(120) NOT NULL,
    notification_number VARCHAR(100),
    detected_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    payload_json JSONB,
    CONSTRAINT fk_pass_alert_pass
        FOREIGN KEY (pass_id) REFERENCES subscription_pass(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_pass_alert
    ON pass_alert(pass_id, alert_type, threshold_key);

CREATE INDEX IF NOT EXISTS idx_pass_alert_type
    ON pass_alert(alert_type, detected_at);

COMMENT ON TABLE pass_alert IS
    'Memoire des alertes deja annoncees · sans elle, le traitement quotidien repeterait la meme chaque jour';
COMMENT ON COLUMN pass_alert.threshold_key IS
    'Declencheur de l alerte · deux declencheurs differents sont deux alertes, un pass renouvele se reannonce';
