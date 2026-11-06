-- Marketplace · la sortie de stock se suit ligne par ligne. Une sortie qui echoue au reglement est
-- reessayee et signalee ; tant qu'elle n'a pas abouti, la commande ne se remet pas au comptoir.

ALTER TABLE marketplace_order_line ADD COLUMN IF NOT EXISTS stock_consumed_at TIMESTAMPTZ;
ALTER TABLE marketplace_order_line ADD COLUMN IF NOT EXISTS stock_error VARCHAR(500);

UPDATE marketplace_order_line l
SET stock_consumed_at = COALESCE(r.closed_at, now())
FROM stock_reservation r
WHERE r.reservation_code = l.stock_reservation_code
  AND r.status = 'CONSUMED'
  AND l.stock_consumed_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_marketplace_order_line_stock_pending ON marketplace_order_line (order_id)
    WHERE stock_reservation_code IS NOT NULL AND stock_consumed_at IS NULL;
