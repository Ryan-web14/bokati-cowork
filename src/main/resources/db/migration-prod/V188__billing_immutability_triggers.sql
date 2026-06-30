-- ============================================================
-- V188: Triggers DB — immutabilité des documents fiscaux scellés
-- ============================================================

-- ── 1. Protection des champs financiers de billing_document ──────────────────

CREATE OR REPLACE FUNCTION fn_billing_document_check_immutable()
RETURNS TRIGGER AS $$
BEGIN
    IF (
           NEW.fiscal_number        IS DISTINCT FROM OLD.fiscal_number
        OR NEW.current_hash         IS DISTINCT FROM OLD.current_hash
        OR NEW.previous_hash        IS DISTINCT FROM OLD.previous_hash
        OR NEW.fiscal_signature     IS DISTINCT FROM OLD.fiscal_signature
        OR NEW.total_amount         IS DISTINCT FROM OLD.total_amount
        OR NEW.subtotal_amount      IS DISTINCT FROM OLD.subtotal_amount
        OR NEW.discount_amount      IS DISTINCT FROM OLD.discount_amount
        OR NEW.taxable_amount       IS DISTINCT FROM OLD.taxable_amount
        OR NEW.vat_amount           IS DISTINCT FROM OLD.vat_amount
        OR NEW.tax_amount           IS DISTINCT FROM OLD.tax_amount
        OR NEW.customer_code        IS DISTINCT FROM OLD.customer_code
        OR NEW.customer_name        IS DISTINCT FROM OLD.customer_name
        OR NEW.customer_niu         IS DISTINCT FROM OLD.customer_niu
        OR NEW.document_type        IS DISTINCT FROM OLD.document_type
        OR NEW.document_number      IS DISTINCT FROM OLD.document_number
        OR NEW.issue_date           IS DISTINCT FROM OLD.issue_date
        OR NEW.currency             IS DISTINCT FROM OLD.currency
        OR NEW.exchange_rate        IS DISTINCT FROM OLD.exchange_rate
        OR NEW.billing_address_json IS DISTINCT FROM OLD.billing_address_json
        OR NEW.seller_name          IS DISTINCT FROM OLD.seller_name
        OR NEW.seller_niu           IS DISTINCT FROM OLD.seller_niu
        OR NEW.seller_address_json  IS DISTINCT FROM OLD.seller_address_json
    ) THEN
        RAISE EXCEPTION
            'billing_document % est verrouillé (locked=true) — les champs financiers sont immuables. '
            'Toute correction doit passer par un avoir (AVR) ou une facture rectificative (REC).',
            OLD.document_number;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_billing_document_immutable
    BEFORE UPDATE ON billing_document
    FOR EACH ROW
    WHEN (OLD.locked = TRUE)
    EXECUTE FUNCTION fn_billing_document_check_immutable();

-- ── 2. Interdire DELETE sur un document scellé ────────────────────────────────

CREATE OR REPLACE FUNCTION fn_billing_document_no_delete()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.locked = TRUE THEN
        RAISE EXCEPTION
            'billing_document % est verrouillé — la suppression physique est interdite. '
            'Utilisez cancelAndArchive() pour une annulation légale via avoir.',
            OLD.document_number;
    END IF;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_billing_document_no_delete
    BEFORE DELETE ON billing_document
    FOR EACH ROW EXECUTE FUNCTION fn_billing_document_no_delete();

-- ── 3. Protéger les lignes d'un document scellé ──────────────────────────────

CREATE OR REPLACE FUNCTION fn_billing_document_line_immutable()
RETURNS TRIGGER AS $$
DECLARE
    v_locked  BOOLEAN;
    v_docnum  VARCHAR(100);
BEGIN
    SELECT locked, document_number
    INTO   v_locked, v_docnum
    FROM   billing_document
    WHERE  id = COALESCE(OLD.document_id, NEW.document_id);

    IF v_locked = TRUE THEN
        RAISE EXCEPTION
            'Les lignes de la facture % sont immuables — document verrouillé (locked=true).',
            v_docnum;
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_billing_line_immutable
    BEFORE UPDATE OR DELETE ON billing_document_line
    FOR EACH ROW EXECUTE FUNCTION fn_billing_document_line_immutable();
