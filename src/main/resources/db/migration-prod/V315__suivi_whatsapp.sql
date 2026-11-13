-- Suivi des messages WhatsApp : statut de remise et de lecture rendu par Infobip (webhook signe HMAC,
-- ou relecture des journaux), repris dans notification_message (canal WHATSAPP). Modeles avec pied
-- de page, bouton (lien vers l'espace client ou copie du code) et duree de validite des codes.

ALTER TABLE whatsapp_message ADD COLUMN IF NOT EXISTS notification_number VARCHAR(100);
ALTER TABLE whatsapp_message ADD COLUMN IF NOT EXISTS status_description VARCHAR(500);
ALTER TABLE whatsapp_message ADD COLUMN IF NOT EXISTS sent_at TIMESTAMPTZ;
ALTER TABLE whatsapp_message ADD COLUMN IF NOT EXISTS done_at TIMESTAMPTZ;
ALTER TABLE whatsapp_message ADD COLUMN IF NOT EXISTS seen_at TIMESTAMPTZ;
ALTER TABLE whatsapp_message ADD COLUMN IF NOT EXISTS checked_at TIMESTAMPTZ;
CREATE INDEX IF NOT EXISTS ix_whatsapp_message_pending ON whatsapp_message (status_group, created_at);

ALTER TABLE notification_message ADD COLUMN IF NOT EXISTS recipient_phone VARCHAR(30);

ALTER TABLE whatsapp_template ADD COLUMN IF NOT EXISTS footer_text VARCHAR(60);
ALTER TABLE whatsapp_template ADD COLUMN IF NOT EXISTS button_type VARCHAR(20);
ALTER TABLE whatsapp_template ADD COLUMN IF NOT EXISTS button_text VARCHAR(25);
ALTER TABLE whatsapp_template ADD COLUMN IF NOT EXISTS button_url VARCHAR(2000);
ALTER TABLE whatsapp_template ADD COLUMN IF NOT EXISTS button_example VARCHAR(2000);
ALTER TABLE whatsapp_template ADD COLUMN IF NOT EXISTS code_expiration_minutes INTEGER;
ALTER TABLE whatsapp_template ALTER COLUMN body_text DROP NOT NULL;
