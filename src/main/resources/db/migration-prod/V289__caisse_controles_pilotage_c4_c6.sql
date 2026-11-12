-- Plan finance, lots C4 (controles) et C6 (pilotage) des caisses.
--
-- Reimpressions tracees (duplicata), calendrier des comptages inopines et alerte de caisse non
-- comptee, droit de lecture des etats de caisse.

ALTER TABLE cash_movement ADD COLUMN IF NOT EXISTS print_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE cash_movement ADD COLUMN IF NOT EXISTS last_printed_by VARCHAR(180);
UPDATE cash_movement SET print_count = 1 WHERE printed_at IS NOT NULL AND print_count = 0;

ALTER TABLE cash_register ADD COLUMN IF NOT EXISTS spot_check_every_days INTEGER;
ALTER TABLE cash_register ADD COLUMN IF NOT EXISTS not_counted_alert_days INTEGER;
ALTER TABLE cash_register ADD COLUMN IF NOT EXISTS not_counted_alerted_at TIMESTAMPTZ;
UPDATE cash_register SET spot_check_every_days = 30 WHERE spot_check_every_days IS NULL AND register_type <> 'AUTOMATIC';
UPDATE cash_register SET not_counted_alert_days = 7 WHERE not_counted_alert_days IS NULL AND register_type <> 'AUTOMATIC';

INSERT INTO permission (id, name, display_name, module, action, is_system_permission, is_active)
SELECT COALESCE((SELECT MAX(id) FROM permission), 0) + 1, 'CASH_REPORT_READ', 'Consulter les etats et la position des caisses',
       'CASH_REPORT', 'READ', TRUE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE name = 'CASH_REPORT_READ');

INSERT INTO role_permission (role_id, permission_id, created_by, is_active)
SELECT r.id, p.id, 'SYSTEM', TRUE
FROM role r
JOIN permission p ON p.name = 'CASH_REPORT_READ'
WHERE r.name IN ('SUPER_ADMIN', 'ADMIN', 'MANAGER', 'FINANCE')
  AND NOT EXISTS (SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
