-- L'entite exploitante est desormais designee, et non plus devinee.
--
-- business_entity porte deux roles tres differents : l'espace de coworking lui-meme, et les
-- clients entreprises · SubscriptionOwnerResolver resout un souscripteur BUSINESS_ENTITY dans
-- cette meme table. La generation de contrat prenait « la seule ligne active » pour l'entite
-- exploitante, ce qui ne tient que tant qu il n y a qu une ligne au total : des le premier client
-- entreprise, tous les contrats echouaient sur « Plusieurs entites exploitantes sont
-- enregistrees ».
--
-- L index unique partiel rend l ambiguite impossible : au plus une ligne vivante peut porter le
-- drapeau. Aucune ligne ne le porte par defaut · la designation est un geste explicite, et son
-- absence donne une erreur qui nomme ce qu il faut faire.

ALTER TABLE business_entity
    ADD COLUMN IF NOT EXISTS is_operator BOOLEAN NOT NULL DEFAULT FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS uq_business_entity_operator
    ON business_entity (is_operator)
    WHERE is_operator AND NOT deleted;

COMMENT ON COLUMN business_entity.is_operator IS
    'Vrai pour l entite exploitante de l espace · son adresse determine le lieu de signature et la juridiction competente. Au plus une ligne vivante.';
