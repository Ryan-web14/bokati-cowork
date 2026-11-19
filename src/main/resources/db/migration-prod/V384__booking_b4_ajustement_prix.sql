-- Booking, lot B4 : ajustement manuel du prix (decision D4) : motif en liste, plafond par un droit
-- dedie, approbation au-dela par le moteur d'approbation (centre BOOKING).

ALTER TABLE booking ADD COLUMN IF NOT EXISTS original_amount NUMERIC(19, 2);
ALTER TABLE booking ADD COLUMN IF NOT EXISTS price_adjustment_status VARCHAR(20);
ALTER TABLE booking ADD COLUMN IF NOT EXISTS price_adjustment_amount NUMERIC(19, 2);
ALTER TABLE booking ADD COLUMN IF NOT EXISTS price_adjustment_rate NUMERIC(7, 2);
ALTER TABLE booking ADD COLUMN IF NOT EXISTS price_adjustment_reason VARCHAR(600);
ALTER TABLE booking ADD COLUMN IF NOT EXISTS price_adjustment_reason_code VARCHAR(60);
ALTER TABLE booking ADD COLUMN IF NOT EXISTS price_adjustment_by VARCHAR(180);
ALTER TABLE booking ADD COLUMN IF NOT EXISTS price_adjustment_approval_number VARCHAR(60);
ALTER TABLE booking ADD COLUMN IF NOT EXISTS approval_for_price_only BOOLEAN NOT NULL DEFAULT FALSE;

-- Liste des motifs.
INSERT INTO reference_list (id, code, module, label, description, strict)
SELECT COALESCE((SELECT MAX(id) FROM reference_list), 3900000) + 1, 'BOOKING_PRICE_ADJUSTMENT_REASON', 'BOOKING',
       'Motifs d''ajustement du prix d''une réservation', 'Choisis par l''équipe quand elle baisse le prix calculé.', TRUE
WHERE NOT EXISTS (SELECT 1 FROM reference_list r WHERE r.code = 'BOOKING_PRICE_ADJUSTMENT_REASON');

WITH seed (code, label, sort_order, requires_text) AS (
    VALUES ('LOYAL_CLIENT', 'Client fidèle', 10, FALSE),
           ('COMMERCIAL_GESTURE', 'Geste commercial', 20, FALSE),
           ('INCIDENT_COMPENSATION', 'Compensation d''un incident', 30, FALSE),
           ('NEGOTIATED_RATE', 'Tarif négocié', 40, FALSE),
           ('PRICING_ERROR', 'Erreur de grille tarifaire', 50, FALSE),
           ('PARTNER', 'Partenaire de l''espace', 60, FALSE),
           ('OTHER', 'Autre, préciser', 999, TRUE)
), numbered AS (
    SELECT s.*, ROW_NUMBER() OVER (ORDER BY s.sort_order) AS rank
    FROM seed s
    WHERE NOT EXISTS (SELECT 1 FROM reference_value v WHERE v.list_code = 'BOOKING_PRICE_ADJUSTMENT_REASON' AND v.code = s.code)
)
INSERT INTO reference_value (id, list_code, code, label, sort_order, active, requires_text, system_value)
SELECT COALESCE((SELECT MAX(id) FROM reference_value), 3900000) + rank, 'BOOKING_PRICE_ADJUSTMENT_REASON', code, label, sort_order, TRUE,
       requires_text, TRUE
FROM numbered;

-- Droits : ajuster jusqu'au plafond, decider d'un ajustement au-dela.
WITH new_permission (name, display_name, module, action) AS (
    VALUES ('BOOKING_PRICE_ADJUST', 'Ajuster le prix d''une reservation jusqu''au plafond', 'BOOKING', 'PRICE_ADJUST'),
           ('BOOKING_PRICE_APPROVE', 'Approuver un ajustement de prix au-dela du plafond', 'BOOKING', 'PRICE_APPROVE')
), numbered AS (
    SELECT n.*, ROW_NUMBER() OVER (ORDER BY n.name) AS rank
    FROM new_permission n
    WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.name = n.name)
)
INSERT INTO permission (id, name, display_name, module, action, is_system_permission, is_active)
SELECT COALESCE((SELECT MAX(id) FROM permission), 0) + rank, name, display_name, module, action, TRUE, TRUE
FROM numbered;

WITH grants (role_name, permission_name) AS (
    VALUES ('SUPER_ADMIN', 'BOOKING_PRICE_ADJUST'), ('ADMIN', 'BOOKING_PRICE_ADJUST'), ('MANAGER', 'BOOKING_PRICE_ADJUST'),
           ('SUPER_ADMIN', 'BOOKING_PRICE_APPROVE'), ('ADMIN', 'BOOKING_PRICE_APPROVE'), ('MANAGER', 'BOOKING_PRICE_APPROVE')
)
INSERT INTO role_permission (role_id, permission_id, created_by, is_active)
SELECT r.id, p.id, 'SYSTEM', TRUE
FROM grants g
JOIN role r ON r.name = g.role_name
JOIN permission p ON p.name = g.permission_name
WHERE NOT EXISTS (SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- Circuit d'approbation du centre des reservations.
INSERT INTO approval_circuit (id, code, center, request_type, label)
VALUES (3900101, 'BOOKING_PRICE_ADJUSTMENT', 'BOOKING', 'PRICE_ADJUSTMENT', 'Ajustement du prix d''une réservation au-delà du plafond')
ON CONFLICT (code) DO NOTHING;

INSERT INTO approval_circuit_level (id, circuit_id, level_order, label, min_amount, approver_rule, hierarchy_depth, permission)
SELECT 3900111, c.id, 1, 'Responsable (N+1)', NULL, 'MANAGER', 1, 'BOOKING:PRICE_APPROVE'
FROM approval_circuit c
WHERE c.code = 'BOOKING_PRICE_ADJUSTMENT'
ON CONFLICT (id) DO NOTHING;
