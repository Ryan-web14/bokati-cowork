-- Contact info du souscripteur pour les clients transients (walk-in, invités)
-- Permet la facturation sans lookup en base pour les codes GST-*
ALTER TABLE billable_item
    ADD COLUMN IF NOT EXISTS subscriber_name  VARCHAR(200),
    ADD COLUMN IF NOT EXISTS subscriber_email VARCHAR(200),
    ADD COLUMN IF NOT EXISTS subscriber_phone VARCHAR(60);
