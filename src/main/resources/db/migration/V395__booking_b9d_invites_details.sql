-- Booking, lot B9d : invitations des participants (reponse, pointage), disposition de la salle,
-- demandes speciales, notes internes et etiquettes (listes souples de l'administrateur).

ALTER TABLE booking_participant ADD COLUMN IF NOT EXISTS invite_token VARCHAR(64);
ALTER TABLE booking_participant ADD COLUMN IF NOT EXISTS invited_at TIMESTAMPTZ;
ALTER TABLE booking_participant ADD COLUMN IF NOT EXISTS responded_at TIMESTAMPTZ;
ALTER TABLE booking_participant ADD COLUMN IF NOT EXISTS checked_in_at TIMESTAMPTZ;
CREATE UNIQUE INDEX IF NOT EXISTS ux_booking_participant_invite_token ON booking_participant (invite_token) WHERE invite_token IS NOT NULL;

ALTER TABLE booking ADD COLUMN IF NOT EXISTS room_layout VARCHAR(60);
ALTER TABLE booking ADD COLUMN IF NOT EXISTS special_requests VARCHAR(1000);
ALTER TABLE booking ADD COLUMN IF NOT EXISTS internal_notes VARCHAR(2000);
ALTER TABLE booking ADD COLUMN IF NOT EXISTS tags VARCHAR(700);

WITH lists (code, label, description) AS (
    VALUES ('BOOKING_ROOM_LAYOUT', 'Dispositions de salle', 'Disposition demandée pour une réunion. Liste souple : une valeur nouvelle est ajoutée à relire.'),
           ('BOOKING_TAG', 'Étiquettes de réservation', 'Étiquettes internes (client important, événement, à rappeler...). Liste souple.')
), numbered AS (
    SELECT l.*, ROW_NUMBER() OVER (ORDER BY l.code) AS rank
    FROM lists l
    WHERE NOT EXISTS (SELECT 1 FROM reference_list r WHERE r.code = l.code)
)
INSERT INTO reference_list (id, code, module, label, description, strict)
SELECT COALESCE((SELECT MAX(id) FROM reference_list), 3950000) + rank, code, 'BOOKING', label, description, FALSE
FROM numbered;

WITH seed (list_code, code, label, sort_order) AS (
    VALUES ('BOOKING_ROOM_LAYOUT', 'BOARDROOM', 'Table de conseil', 10),
           ('BOOKING_ROOM_LAYOUT', 'U_SHAPE', 'En U', 20),
           ('BOOKING_ROOM_LAYOUT', 'CLASSROOM', 'Salle de classe', 30),
           ('BOOKING_ROOM_LAYOUT', 'THEATRE', 'Théâtre', 40),
           ('BOOKING_ROOM_LAYOUT', 'CABARET', 'Cabaret', 50),
           ('BOOKING_TAG', 'VIP', 'Client important', 10),
           ('BOOKING_TAG', 'EVENT', 'Événement', 20),
           ('BOOKING_TAG', 'CALL_BACK', 'À rappeler', 30),
           ('BOOKING_TAG', 'FIRST_VISIT', 'Première visite', 40)
), numbered AS (
    SELECT s.*, ROW_NUMBER() OVER (ORDER BY s.list_code, s.sort_order) AS rank
    FROM seed s
    WHERE NOT EXISTS (SELECT 1 FROM reference_value v WHERE v.list_code = s.list_code AND v.code = s.code)
)
INSERT INTO reference_value (id, list_code, code, label, sort_order, active, requires_text, system_value)
SELECT COALESCE((SELECT MAX(id) FROM reference_value), 3950000) + rank, list_code, code, label, sort_order, TRUE, FALSE, TRUE
FROM numbered;
