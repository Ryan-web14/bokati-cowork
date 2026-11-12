-- Lot F7 du plan finance : pieces comptables emises par le logiciel.
--
-- Numero sans trou par type et par mois : le compteur est verrouille et avance dans la
-- transaction d'emission (une emission annulee ne consomme pas de numero). Chaque piece emise est
-- enregistree une fois par fait d'origine, avec ses reimpressions.

CREATE TABLE IF NOT EXISTS acc_voucher_counter
(
    voucher_type VARCHAR(10) NOT NULL,
    period       VARCHAR(6)  NOT NULL,
    last_number  INTEGER     NOT NULL DEFAULT 0,
    CONSTRAINT pk_acc_voucher_counter PRIMARY KEY (voucher_type, period)
);

CREATE TABLE IF NOT EXISTS acc_voucher
(
    id                  BIGINT PRIMARY KEY,
    voucher_type        VARCHAR(10)    NOT NULL,
    number              VARCHAR(40)    NOT NULL UNIQUE,
    source_type         VARCHAR(40)    NOT NULL,
    source_code         VARCHAR(120)   NOT NULL,
    amount              NUMERIC(19, 4),
    label               VARCHAR(500),
    issued_by           VARCHAR(180)   NOT NULL,
    issued_at           TIMESTAMPTZ    NOT NULL DEFAULT now(),
    print_count         INTEGER        NOT NULL DEFAULT 0,
    last_printed_at     TIMESTAMPTZ,
    last_printed_by     VARCHAR(180),
    cancelled_by_number VARCHAR(40),
    CONSTRAINT uq_acc_voucher_source UNIQUE (voucher_type, source_code)
);

CREATE OR REPLACE FUNCTION acc_voucher_counter_protect() RETURNS trigger AS
$$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'Un compteur de pieces ne se supprime pas';
    END IF;
    IF NEW.last_number <> OLD.last_number + 1 THEN
        RAISE EXCEPTION 'Le compteur de pieces % % n''avance que d''un numero a la fois', OLD.voucher_type, OLD.period;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_acc_voucher_counter_protect ON acc_voucher_counter;
CREATE TRIGGER trg_acc_voucher_counter_protect
    BEFORE UPDATE OR DELETE ON acc_voucher_counter
    FOR EACH ROW EXECUTE FUNCTION acc_voucher_counter_protect();

CREATE OR REPLACE FUNCTION acc_voucher_protect() RETURNS trigger AS
$$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'Une piece emise ne se supprime pas : elle s''annule par une piece liee';
    END IF;
    IF NEW.number <> OLD.number OR NEW.voucher_type <> OLD.voucher_type OR NEW.source_code <> OLD.source_code
        OR NEW.amount IS DISTINCT FROM OLD.amount OR NEW.issued_at <> OLD.issued_at THEN
        RAISE EXCEPTION 'La piece % ne se modifie pas', OLD.number;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_acc_voucher_protect ON acc_voucher;
CREATE TRIGGER trg_acc_voucher_protect
    BEFORE UPDATE OR DELETE ON acc_voucher
    FOR EACH ROW EXECUTE FUNCTION acc_voucher_protect();
