-- Une remise qui se reclame d'une campagne doit designer une campagne qui existe.
--
-- billing_document_discount porte desormais source_type et source_code. Sans controle, rien
-- n'empeche d'ecrire source_type = 'PROMOTION' avec un code qui ne correspond a rien, et l'etat
-- « cout des promotions par campagne » rendrait alors des lignes rattachees a du vide, sans que
-- personne ne puisse dire s'il s'agit d'une campagne supprimee ou d'une faute de frappe.
--
-- Le controle est pose en base et non dans le code applicatif, parce qu'une remise peut arriver par
-- plusieurs chemins · creation de document, duplication d'un devis, reprise d'un avoir. Un garde
-- place dans un seul de ces chemins ne garde rien.
--
-- Une clef etrangere ne convient pas ici : elle vaudrait pour toutes les lignes, y compris celles
-- dont la source est un coupon, une grille ou une saisie manuelle, dont les codes vivent dans
-- d'autres tables ou nulle part. D'ou un declencheur, qui ne controle que ce qui se dit promotion.

CREATE OR REPLACE FUNCTION billing_discount_source_must_exist()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.source_type IS NULL THEN
        RETURN NEW;
    END IF;

    IF NEW.source_code IS NULL OR btrim(NEW.source_code) = '' THEN
        RAISE EXCEPTION 'Une remise de source % doit porter un code de source', NEW.source_type
            USING ERRCODE = '23514';
    END IF;

    IF NEW.source_type = 'PROMOTION' THEN
        IF NOT EXISTS (SELECT 1 FROM promotion WHERE lower(code) = lower(NEW.source_code)) THEN
            RAISE EXCEPTION 'Promotion % introuvable pour cette remise', NEW.source_code
                USING ERRCODE = '23503';
        END IF;
    END IF;

    IF NEW.source_type = 'COUPON' THEN
        -- Le coupon peut avoir ete revoque depuis, mais il doit avoir existe : une remise attribuee
        -- a un code qui n'a jamais ete emis n'est pas une remise, c'est une anomalie.
        IF NOT EXISTS (SELECT 1 FROM coupon WHERE lower(code) = lower(NEW.source_code)) THEN
            RAISE EXCEPTION 'Coupon % introuvable pour cette remise', NEW.source_code
                USING ERRCODE = '23503';
        END IF;
    END IF;

    IF NEW.source_type = 'PRICE_LIST' THEN
        IF NOT EXISTS (SELECT 1 FROM price_list WHERE lower(code) = lower(NEW.source_code)) THEN
            RAISE EXCEPTION 'Grille tarifaire % introuvable pour cette remise', NEW.source_code
                USING ERRCODE = '23503';
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_billing_discount_source_must_exist ON billing_document_discount;

CREATE TRIGGER trg_billing_discount_source_must_exist
    BEFORE INSERT OR UPDATE OF source_type, source_code ON billing_document_discount
    FOR EACH ROW
    EXECUTE FUNCTION billing_discount_source_must_exist();

COMMENT ON FUNCTION billing_discount_source_must_exist() IS
    'Refuse une remise dont la source est declaree mais introuvable · MANUAL et REFERRAL ne sont pas controles, leur code ne vit dans aucune table';
