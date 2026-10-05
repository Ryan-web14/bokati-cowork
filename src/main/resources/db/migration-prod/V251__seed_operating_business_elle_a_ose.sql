-- La fiche de l'entite exploitante, et les contraintes qui empechaient de l'enregistrer.
--
-- Aucun contrat ne se generait en production faute de cette fiche · ni pour un membre
-- particulier, ni pour une entreprise. L'entite exploitante est la partie qui signe EN FACE du
-- client : elle figure sur tous les contrats, et l'adresse de son siege determine le lieu de
-- signature et la juridiction competente.
--
-- Semee ici plutot que laissee a un appel d'API : c'est une donnee de reference sans laquelle le
-- module contrat ne fonctionne pas, au meme titre que la politique de gabarit semee en V67.

-- 1. Le NIU n'est pas detenu par toutes les entites au moment de leur enregistrement. Il reste
--    unique quand il est renseigne · Postgres autorise plusieurs NULL sous une contrainte UNIQUE.
ALTER TABLE business_entity ALTER COLUMN niu_number DROP NOT NULL;

-- 2. Le courriel etait NOT NULL en base alors que le DTO le declare facultatif · toute creation
--    sans courriel echouait donc en erreur serveur au lieu d'etre acceptee.
ALTER TABLE business_entity ALTER COLUMN email DROP NOT NULL;

-- 3. base_currency_code est une colonne restee d'un mappage precedent · le modele ecrit
--    base_currency_id. La, ou elle existe encore en NOT NULL, aucune entreprise ne peut etre
--    creee du tout : l'insertion JPA ne la renseigne jamais. Elle est alignee sur
--    base_currency_id puis liberee. Sans effet la ou elle n'existe pas.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'business_entity' AND column_name = 'base_currency_code'
    ) THEN
        UPDATE business_entity SET base_currency_code = base_currency_id
         WHERE base_currency_code IS NULL AND base_currency_id IS NOT NULL;
        ALTER TABLE business_entity ALTER COLUMN base_currency_code DROP NOT NULL;
    END IF;
END $$;

-- 4. La fiche elle-meme · ETS ELLE A OSE, RCCM CG-PNR-01-2017-A11-00472.
DO $$
DECLARE
    v_currency_id BIGINT;
    v_country_id  BIGINT;
    v_address_id  BIGINT := 255001;
    v_business_id BIGINT := 255002;
    v_rccm        TEXT   := 'CG-PNR-01-2017-A11-00472';
BEGIN
    -- Une entite exploitante a deja ete designee · on ne touche a rien.
    IF EXISTS (SELECT 1 FROM business_entity WHERE is_operator AND NOT deleted) THEN
        RETURN;
    END IF;

    -- Le RCCM existe deja sans le drapeau · on le pose plutot que de creer un doublon.
    IF EXISTS (SELECT 1 FROM business_entity WHERE rccm_number = v_rccm AND NOT deleted) THEN
        UPDATE business_entity
           SET is_operator = TRUE, updated_by = 'SYSTEM', updated_at = NOW()
         WHERE rccm_number = v_rccm AND NOT deleted;
        RETURN;
    END IF;

    SELECT id INTO v_currency_id FROM currency WHERE currency_code = 'XAF' LIMIT 1;
    IF v_currency_id IS NULL THEN
        RAISE EXCEPTION 'Devise XAF absente · la V11 doit avoir ete jouee avant celle-ci';
    END IF;

    SELECT id INTO v_country_id FROM country WHERE country_code = 'CG' LIMIT 1;
    IF v_country_id IS NULL THEN
        RAISE EXCEPTION 'Pays CG absent · la V11 doit avoir ete jouee avant celle-ci';
    END IF;

    INSERT INTO address (id, street_number, street_name, district, city, country, created_at)
    SELECT v_address_id,
           '84',
           'Boulevard du General Charles de Gaulle',
           'centre-ville',
           'Pointe-Noire',
           v_country_id,
           NOW()
    WHERE NOT EXISTS (SELECT 1 FROM address WHERE id = v_address_id);

    INSERT INTO business_entity (
        id, entity_code, name, legal_form, niu_number, rccm_number, tax_id, activity,
        address_id, phone, email, base_currency_id, status, deleted, is_operator,
        created_by, created_at, updated_at
    )
    VALUES (
        v_business_id,
        'ELLEAOSE',
        'ETS ELLE A OSE',
        'ETABLISSEMENT',
        NULL,                                 -- NIU non detenu a ce jour
        v_rccm,
        NULL,
        'Espace de coworking',
        v_address_id,
        '+242 05 204 25 51',                  -- le second numero attend le champ multi-telephone
        'coworkspace@elleaose.com',
        v_currency_id,
        'ACTIVE',
        FALSE,
        TRUE,                                 -- l entite exploitante
        'SYSTEM',
        NOW(),
        NOW()
    );
END $$;
