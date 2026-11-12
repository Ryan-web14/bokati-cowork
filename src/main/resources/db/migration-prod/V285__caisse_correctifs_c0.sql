-- Plan finance, lot C0 : correctifs des caisses.
--
-- 1. Un ajustement de caisse porte desormais un sens (IN ou OUT). Avant, il etait ajoute a
--    l'attendu de cloture mais ignore par le solde courant, et ne pouvait pas baisser la caisse.
--    Les ajustements existants etaient comptes en entree : ils recoivent le sens IN, ce qui ne
--    change aucun attendu deja calcule.
-- 2. Le solde courant de chaque mouvement est recalcule avec la regle unique de sens : il partait
--    du fonds d'ouverture puis ajoutait le mouvement OPENING_FLOAT (fonds compte deux fois), ignorait
--    les ajustements, et n'etait pas pose sur les bons ni sur les paiements.
-- 3. Permissions fines des caisses : chaque action a la sienne, au lieu de CASH_ADJUST pour tout.

ALTER TABLE cash_movement ADD COLUMN IF NOT EXISTS direction VARCHAR(10);

UPDATE cash_movement SET direction = 'IN'
WHERE movement_type = 'ADJUSTMENT' AND direction IS NULL;

ALTER TABLE cash_movement DROP CONSTRAINT IF EXISTS ck_cash_movement_direction;
ALTER TABLE cash_movement ADD CONSTRAINT ck_cash_movement_direction CHECK (
    (direction IS NULL OR direction IN ('IN', 'OUT'))
    AND (movement_type <> 'ADJUSTMENT' OR direction IS NOT NULL)
);

UPDATE cash_movement cm
SET running_balance = recomputed.balance
FROM (
    SELECT m.id,
           SUM(CASE
                   WHEN m.status NOT IN ('CONFIRMED', 'REVERSED') THEN 0
                   WHEN m.movement_type IN ('OPENING_FLOAT', 'PAYMENT', 'CASH_IN', 'TRANSFER_IN') THEN m.amount
                   WHEN m.movement_type IN ('REFUND', 'CASH_OUT', 'SAFE_DEPOSIT', 'TRANSFER_OUT') THEN -m.amount
                   WHEN m.movement_type = 'ADJUSTMENT' AND m.direction = 'OUT' THEN -m.amount
                   WHEN m.movement_type = 'ADJUSTMENT' THEN m.amount
                   ELSE 0
               END) OVER (PARTITION BY m.cash_session_id ORDER BY m.created_at, m.id
                          ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS balance
    FROM cash_movement m
) recomputed
WHERE recomputed.id = cm.id
  AND cm.running_balance IS DISTINCT FROM recomputed.balance;

WITH new_permission (name, display_name, module, action) AS (
    VALUES ('CASH_REGISTER_MANAGE', 'Gerer les caisses', 'CASH_REGISTER', 'MANAGE'),
           ('CASH_SESSION_OPEN', 'Ouvrir une session de caisse', 'CASH_SESSION', 'OPEN'),
           ('CASH_SESSION_CLOSE', 'Clore une session de caisse', 'CASH_SESSION', 'CLOSE'),
           ('CASH_VARIANCE_DECIDE', 'Decider d''un ecart de caisse', 'CASH_VARIANCE', 'DECIDE'),
           ('CASH_MOVEMENT_CREATE', 'Saisir un mouvement de caisse', 'CASH_MOVEMENT', 'CREATE'),
           ('CASH_MOVEMENT_APPROVE', 'Viser un mouvement ou une demande de caisse', 'CASH_MOVEMENT', 'APPROVE'),
           ('CASH_MOVEMENT_REVERSE', 'Contre-passer un mouvement de caisse', 'CASH_MOVEMENT', 'REVERSE')
), numbered AS (
    SELECT n.*, ROW_NUMBER() OVER (ORDER BY n.name) AS rank
    FROM new_permission n
    WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.name = n.name)
)
INSERT INTO permission (id, name, display_name, module, action, is_system_permission, is_active)
SELECT COALESCE((SELECT MAX(id) FROM permission), 0) + rank, name, display_name, module, action, TRUE, TRUE
FROM numbered;

-- Reprise des anciens droits : OPEN_SESSION donne CASH_SESSION_OPEN, CLOSE_SESSION donne
-- CASH_SESSION_CLOSE, ADJUST donne CASH_MOVEMENT_CREATE. La decision d'un ecart, le visa et la
-- contre-passation ne se deduisent pas de ADJUST (tout caissier l'avait) : ils vont aux roles
-- d'encadrement seulement.
WITH by_equivalent (permission_name, equivalent) AS (
    VALUES ('CASH_SESSION_OPEN', 'CASH_OPEN_SESSION'),
           ('CASH_SESSION_CLOSE', 'CASH_CLOSE_SESSION'),
           ('CASH_MOVEMENT_CREATE', 'CASH_ADJUST')
), by_role (permission_name, role_name) AS (
    VALUES ('CASH_REGISTER_MANAGE', 'SUPER_ADMIN'), ('CASH_REGISTER_MANAGE', 'ADMIN'), ('CASH_REGISTER_MANAGE', 'MANAGER'),
           ('CASH_VARIANCE_DECIDE', 'SUPER_ADMIN'), ('CASH_VARIANCE_DECIDE', 'ADMIN'), ('CASH_VARIANCE_DECIDE', 'MANAGER'), ('CASH_VARIANCE_DECIDE', 'FINANCE'),
           ('CASH_MOVEMENT_APPROVE', 'SUPER_ADMIN'), ('CASH_MOVEMENT_APPROVE', 'ADMIN'), ('CASH_MOVEMENT_APPROVE', 'MANAGER'), ('CASH_MOVEMENT_APPROVE', 'FINANCE'),
           ('CASH_MOVEMENT_REVERSE', 'SUPER_ADMIN'), ('CASH_MOVEMENT_REVERSE', 'ADMIN'), ('CASH_MOVEMENT_REVERSE', 'MANAGER'),
           ('CASH_SESSION_OPEN', 'SUPER_ADMIN'), ('CASH_SESSION_OPEN', 'ADMIN'),
           ('CASH_SESSION_CLOSE', 'SUPER_ADMIN'), ('CASH_SESSION_CLOSE', 'ADMIN'),
           ('CASH_MOVEMENT_CREATE', 'SUPER_ADMIN'), ('CASH_MOVEMENT_CREATE', 'ADMIN')
), holders AS (
    SELECT DISTINCT e.permission_name, rp.role_id
    FROM by_equivalent e
    JOIN permission equivalent ON equivalent.name = e.equivalent
    JOIN role_permission rp ON rp.permission_id = equivalent.id AND rp.is_active = TRUE
    UNION
    SELECT DISTINCT b.permission_name, r.id
    FROM by_role b
    JOIN role r ON r.name = b.role_name
)
INSERT INTO role_permission (role_id, permission_id, created_by, is_active)
SELECT h.role_id, p.id, 'SYSTEM', TRUE
FROM holders h
JOIN permission p ON p.name = h.permission_name
WHERE NOT EXISTS (
    SELECT 1 FROM role_permission rp WHERE rp.role_id = h.role_id AND rp.permission_id = p.id
);
