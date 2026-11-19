-- Booking, lot B7 : motifs d'annulation et de refus en listes de l'administrateur (socle L0),
-- codes des motifs gardes sur la reservation.

ALTER TABLE booking ADD COLUMN IF NOT EXISTS cancellation_reason_code VARCHAR(60);
ALTER TABLE booking ADD COLUMN IF NOT EXISTS rejection_reason_code VARCHAR(60);

WITH lists (code, module, label, description) AS (
    VALUES ('BOOKING_CANCEL_REASON', 'BOOKING', 'Motifs d''annulation d''une réservation', 'Choisis à l''annulation par l''équipe ou le client.'),
           ('BOOKING_REJECT_REASON', 'BOOKING', 'Motifs de refus d''une réservation', 'Choisis au refus d''une réservation en attente d''approbation.')
), numbered AS (
    SELECT l.*, ROW_NUMBER() OVER (ORDER BY l.code) AS rank
    FROM lists l
    WHERE NOT EXISTS (SELECT 1 FROM reference_list r WHERE r.code = l.code)
)
INSERT INTO reference_list (id, code, module, label, description, strict)
SELECT COALESCE((SELECT MAX(id) FROM reference_list), 3880000) + rank, code, module, label, description, TRUE
FROM numbered;

WITH seed (list_code, code, label, sort_order, requires_text) AS (
    VALUES ('BOOKING_CANCEL_REASON', 'CLIENT_REQUEST', 'Demande du client', 10, FALSE),
           ('BOOKING_CANCEL_REASON', 'SCHEDULE_CHANGE', 'Changement d''agenda', 20, FALSE),
           ('BOOKING_CANCEL_REASON', 'ILLNESS', 'Maladie ou empêchement', 30, FALSE),
           ('BOOKING_CANCEL_REASON', 'DUPLICATE', 'Réservation en double', 40, FALSE),
           ('BOOKING_CANCEL_REASON', 'ENTRY_ERROR', 'Erreur de saisie', 50, FALSE),
           ('BOOKING_CANCEL_REASON', 'RESOURCE_UNAVAILABLE', 'Ressource indisponible', 60, FALSE),
           ('BOOKING_CANCEL_REASON', 'PAYMENT_NOT_RECEIVED', 'Paiement non reçu', 70, FALSE),
           ('BOOKING_CANCEL_REASON', 'OTHER', 'Autre, préciser', 999, TRUE),
           ('BOOKING_REJECT_REASON', 'NOT_ELIGIBLE', 'Client non éligible à cette ressource', 10, FALSE),
           ('BOOKING_REJECT_REASON', 'SLOT_RESERVED', 'Créneau réservé à un autre usage', 20, FALSE),
           ('BOOKING_REJECT_REASON', 'INCOMPLETE_FILE', 'Dossier client incomplet', 30, FALSE),
           ('BOOKING_REJECT_REASON', 'OUTSTANDING_BALANCE', 'Impayés en cours', 40, FALSE),
           ('BOOKING_REJECT_REASON', 'OTHER', 'Autre, préciser', 999, TRUE)
), numbered AS (
    SELECT s.*, ROW_NUMBER() OVER (ORDER BY s.list_code, s.sort_order) AS rank
    FROM seed s
    WHERE NOT EXISTS (SELECT 1 FROM reference_value v WHERE v.list_code = s.list_code AND v.code = s.code)
)
INSERT INTO reference_value (id, list_code, code, label, sort_order, active, requires_text, system_value)
SELECT COALESCE((SELECT MAX(id) FROM reference_value), 3880000) + rank, list_code, code, label, sort_order, TRUE, requires_text, TRUE
FROM numbered;
