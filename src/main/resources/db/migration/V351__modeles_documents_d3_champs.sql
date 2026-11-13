-- Modeles de documents, lot D3 : champs ajoutes. Chaque version declare ses champs (imprimes vides
-- et remplis a la main, ou saisis dans l'application) ; les valeurs saisies sont rattachees a la
-- piece metier (mouvement de caisse, etat de besoin, demande de fonds, mouvement de decaissement).

ALTER TABLE document_template_version ADD COLUMN IF NOT EXISTS fields_json JSONB NOT NULL DEFAULT '[]'::jsonb;

CREATE TABLE IF NOT EXISTS document_field_value
(
    id            BIGINT PRIMARY KEY,
    template_code VARCHAR(80)  NOT NULL REFERENCES document_template (code),
    source_code   VARCHAR(120) NOT NULL,
    field_key     VARCHAR(40)  NOT NULL,
    field_value   VARCHAR(500) NOT NULL,
    entered_by    VARCHAR(150),
    entered_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_document_field_value_key ON document_field_value (template_code, source_code, field_key);
