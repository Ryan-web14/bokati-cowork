CREATE TABLE IF NOT EXISTS contract_template_policy (
    id BIGINT PRIMARY KEY,
    code VARCHAR(120) NOT NULL UNIQUE,
    name VARCHAR(180) NOT NULL,
    description TEXT,
    template_code VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS contract_template_clause (
    id BIGINT PRIMARY KEY,
    policy_code VARCHAR(120) NOT NULL,
    clause_code VARCHAR(120) NOT NULL,
    title VARCHAR(180) NOT NULL,
    body TEXT NOT NULL,
    display_order INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_contract_template_clause_policy_code UNIQUE (policy_code, clause_code),
    CONSTRAINT fk_contract_template_clause_policy FOREIGN KEY (policy_code)
        REFERENCES contract_template_policy(code)
);

CREATE INDEX IF NOT EXISTS idx_contract_template_policy_active
    ON contract_template_policy(code, active);

CREATE INDEX IF NOT EXISTS idx_contract_template_clause_policy
    ON contract_template_clause(policy_code, active, display_order);

INSERT INTO contract_template_policy (
    id,
    code,
    name,
    description,
    template_code,
    active,
    created_at,
    updated_at
)
SELECT
    67001,
    'SUBSCRIPTION_PASS_NON_REFUNDABLE',
    'Contrat abonnement / pass non remboursable',
    'Template systeme utilise pour la prise automatique de subscription, pass et add-on non remboursables.',
    'subscription-pass-non-refundable',
    TRUE,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM contract_template_policy WHERE code = 'SUBSCRIPTION_PASS_NON_REFUNDABLE'
);

INSERT INTO contract_template_clause (
    id,
    policy_code,
    clause_code,
    title,
    body,
    display_order,
    active,
    created_at,
    updated_at
)
SELECT
    67011,
    'SUBSCRIPTION_PASS_NON_REFUNDABLE',
    'NON_REFUNDABLE',
    'Non remboursement',
    'Toute souscription, achat de pass ou activation d''add-on validee par le systeme est ferme, definitive et non remboursable, sauf obligation legale contraire ou decision commerciale explicite de l''operateur.',
    10,
    TRUE,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM contract_template_clause
    WHERE policy_code = 'SUBSCRIPTION_PASS_NON_REFUNDABLE'
      AND clause_code = 'NON_REFUNDABLE'
);

INSERT INTO contract_template_clause (
    id,
    policy_code,
    clause_code,
    title,
    body,
    display_order,
    active,
    created_at,
    updated_at
)
SELECT
    67012,
    'SUBSCRIPTION_PASS_NON_REFUNDABLE',
    'SYSTEM_SIGNATURE',
    'Signature systeme',
    'Le contrat est genere et signe automatiquement par le systeme au nom du beneficiaire ou de son representant selectionne, sur la base des informations associees a son compte, son email, son member id, son customer id ou son business code.',
    20,
    TRUE,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM contract_template_clause
    WHERE policy_code = 'SUBSCRIPTION_PASS_NON_REFUNDABLE'
      AND clause_code = 'SYSTEM_SIGNATURE'
);

INSERT INTO contract_template_clause (
    id,
    policy_code,
    clause_code,
    title,
    body,
    display_order,
    active,
    created_at,
    updated_at
)
SELECT
    67013,
    'SUBSCRIPTION_PASS_NON_REFUNDABLE',
    'ENTITLEMENT_CONSUMPTION',
    'Consommation des avantages',
    'Les avantages, quotas, credits, reservations ou droits associes a l''offre sont personnels au beneficiaire indique, sauf regle de transferabilite explicite, et peuvent etre consommes selon les limites, periodes et politiques applicables.',
    30,
    TRUE,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM contract_template_clause
    WHERE policy_code = 'SUBSCRIPTION_PASS_NON_REFUNDABLE'
      AND clause_code = 'ENTITLEMENT_CONSUMPTION'
);

INSERT INTO contract_template_clause (
    id,
    policy_code,
    clause_code,
    title,
    body,
    display_order,
    active,
    created_at,
    updated_at
)
SELECT
    67014,
    'SUBSCRIPTION_PASS_NON_REFUNDABLE',
    'SERVICE_RULES',
    'Regles de service',
    'Le beneficiaire s''engage a respecter les regles d''utilisation des espaces, ressources, services, horaires, politiques de booking, controle d''acces, check-in/check-out et no-show applicables.',
    40,
    TRUE,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM contract_template_clause
    WHERE policy_code = 'SUBSCRIPTION_PASS_NON_REFUNDABLE'
      AND clause_code = 'SERVICE_RULES'
);
