-- Caisse par defaut creee par le systeme et ouverture de session par un administrateur.
--
-- default_register : la caisse qui recoit les paiements saisis sans session de caisse. Une seule
-- caisse la porte (index unique partiel). Elle est creee au demarrage par l'application.
--
-- opening_checks_waived_by : administrateur qui a ouvert la session sans les controles
-- d'ouverture (affectation, poste, horaires, confirmation de l'ecart d'ouverture).

ALTER TABLE cash_register ADD COLUMN IF NOT EXISTS default_register BOOLEAN NOT NULL DEFAULT FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS ux_cash_register_default
    ON cash_register (default_register) WHERE default_register;

ALTER TABLE cash_session ADD COLUMN IF NOT EXISTS opening_checks_waived_by VARCHAR(255);
