-- ============================================================
-- Immuabilite du journal de mouvements de stock
-- ============================================================
-- Un mouvement de stock est un fait comptable : une fois ecrit, il ne se
-- corrige pas, il se contre-passe. Seules les colonnes de contre-passation
-- restent modifiables apres insertion.
--
-- Colonnes modifiables :
--   reversed, reversed_at, reversed_by, reversal_reason
--     -> renseignees sur le mouvement d'origine lors de sa contre-passation
--   reversal_of_movement_id
--     -> renseigne sur le mouvement de contre-passation juste apres son insertion
--
-- Toute autre modification, et toute suppression, sont refusees par la base.

-- 1. Blocage des modifications hors contre-passation

CREATE OR REPLACE FUNCTION fn_stock_movement_check_immutable()
RETURNS TRIGGER AS $$
BEGIN
    IF (
           NEW.id                      IS DISTINCT FROM OLD.id
        OR NEW.movement_code           IS DISTINCT FROM OLD.movement_code
        OR NEW.item_id                 IS DISTINCT FROM OLD.item_id
        OR NEW.location_from_id        IS DISTINCT FROM OLD.location_from_id
        OR NEW.location_to_id          IS DISTINCT FROM OLD.location_to_id
        OR NEW.movement_type           IS DISTINCT FROM OLD.movement_type
        OR NEW.quantity                IS DISTINCT FROM OLD.quantity
        OR NEW.unit_cost               IS DISTINCT FROM OLD.unit_cost
        OR NEW.total_cost              IS DISTINCT FROM OLD.total_cost
        OR NEW.reference_type          IS DISTINCT FROM OLD.reference_type
        OR NEW.reference_code          IS DISTINCT FROM OLD.reference_code
        OR NEW.reason                  IS DISTINCT FROM OLD.reason
        OR NEW.reason_code             IS DISTINCT FROM OLD.reason_code
        OR NEW.allow_negative_override IS DISTINCT FROM OLD.allow_negative_override
        OR NEW.performed_by            IS DISTINCT FROM OLD.performed_by
        OR NEW.performed_at            IS DISTINCT FROM OLD.performed_at
    ) THEN
        RAISE EXCEPTION
            'Le mouvement de stock % est immuable. Seule la contre-passation est autorisee : '
            'creez un mouvement inverse via reverseMovement() au lieu de modifier celui-ci.',
            OLD.movement_code;
    END IF;

    IF OLD.reversed = TRUE AND NEW.reversed = FALSE THEN
        RAISE EXCEPTION
            'Le mouvement de stock % est deja contre-passe. Le retour en arriere est interdit.',
            OLD.movement_code;
    END IF;

    IF OLD.reversal_of_movement_id IS NOT NULL
       AND NEW.reversal_of_movement_id IS DISTINCT FROM OLD.reversal_of_movement_id THEN
        RAISE EXCEPTION
            'Le rattachement de contre-passation du mouvement % est fige.',
            OLD.movement_code;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_stock_movement_immutable ON stock_movement;

CREATE TRIGGER trg_stock_movement_immutable
    BEFORE UPDATE ON stock_movement
    FOR EACH ROW
    EXECUTE FUNCTION fn_stock_movement_check_immutable();

-- 2. Interdiction de suppression

CREATE OR REPLACE FUNCTION fn_stock_movement_no_delete()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION
        'La suppression du mouvement de stock % est interdite. '
        'Le journal de stock est un historique permanent : utilisez la contre-passation.',
        OLD.movement_code;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_stock_movement_no_delete ON stock_movement;

CREATE TRIGGER trg_stock_movement_no_delete
    BEFORE DELETE ON stock_movement
    FOR EACH ROW
    EXECUTE FUNCTION fn_stock_movement_no_delete();

-- 3. Protection de la ventilation par lot d'un mouvement

CREATE OR REPLACE FUNCTION fn_stock_movement_lot_immutable()
RETURNS TRIGGER AS $$
DECLARE
    v_movement_code VARCHAR(90);
BEGIN
    SELECT movement_code
    INTO   v_movement_code
    FROM   stock_movement
    WHERE  id = COALESCE(OLD.movement_id, NEW.movement_id);

    RAISE EXCEPTION
        'La ventilation par lot du mouvement % est immuable.',
        COALESCE(v_movement_code, '(inconnu)');
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_stock_movement_lot_immutable ON stock_movement_lot;

CREATE TRIGGER trg_stock_movement_lot_immutable
    BEFORE UPDATE OR DELETE ON stock_movement_lot
    FOR EACH ROW
    EXECUTE FUNCTION fn_stock_movement_lot_immutable();

-- 4. Index de support pour la reconciliation des niveaux de stock

CREATE INDEX IF NOT EXISTS idx_stock_movement_item_from
    ON stock_movement (item_id, location_from_id);

CREATE INDEX IF NOT EXISTS idx_stock_movement_item_to
    ON stock_movement (item_id, location_to_id);

CREATE INDEX IF NOT EXISTS idx_stock_movement_override
    ON stock_movement (performed_at DESC)
    WHERE allow_negative_override = TRUE;
