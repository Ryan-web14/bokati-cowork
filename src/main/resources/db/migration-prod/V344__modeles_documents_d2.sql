-- Modeles de documents, lot D2 : modeles en blocs stockes en base, versions (brouillon, publiee,
-- archivee), droit SYSTEM:DOCUMENT_TEMPLATES. Sans version publiee, une piece sort de son gabarit
-- Thymeleaf d'origine. Versions de depart publiees pour les quatre pieces maquettees et validees
-- par l'exploitant (bons de sortie et d'entree de caisse, demande de fonds, etat de besoin).

CREATE TABLE IF NOT EXISTS document_template
(
    id                BIGINT PRIMARY KEY,
    code              VARCHAR(80)  NOT NULL,
    label             VARCHAR(150) NOT NULL,
    family            VARCHAR(20)  NOT NULL,
    domain            VARCHAR(40)  NOT NULL,
    fallback_template VARCHAR(150),
    description       TEXT,
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_document_template_family CHECK (family IN ('PDF', 'EMAIL'))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_document_template_code ON document_template (code);

CREATE TABLE IF NOT EXISTS document_template_version
(
    id             BIGINT PRIMARY KEY,
    template_code  VARCHAR(80)  NOT NULL REFERENCES document_template (code),
    version_number INTEGER      NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    page_format    VARCHAR(20)  NOT NULL DEFAULT 'A4',
    copies_json    JSONB        NOT NULL DEFAULT '[]'::jsonb,
    style_json     JSONB        NOT NULL DEFAULT '{}'::jsonb,
    blocks_json    JSONB        NOT NULL DEFAULT '[]'::jsonb,
    note           VARCHAR(500),
    created_by     VARCHAR(150),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ,
    published_by   VARCHAR(150),
    published_at   TIMESTAMPTZ,
    archived_at    TIMESTAMPTZ,
    CONSTRAINT ck_document_template_version_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT ck_document_template_version_format CHECK (page_format IN ('A4', 'A5_TWO_UP', 'A6'))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_document_template_version_number ON document_template_version (template_code, version_number);
-- Un seul brouillon et une seule version publiee par modele.
CREATE UNIQUE INDEX IF NOT EXISTS ux_document_template_version_draft
    ON document_template_version (template_code) WHERE status = 'DRAFT';
CREATE UNIQUE INDEX IF NOT EXISTS ux_document_template_version_published
    ON document_template_version (template_code) WHERE status = 'PUBLISHED';

INSERT INTO document_template (id, code, label, family, domain, fallback_template, description)
SELECT v.id, v.code, v.label, 'PDF', v.domain, v.fallback, v.description
FROM (VALUES
          (3500001, 'CASH_OUT_VOUCHER', 'Bon de sortie de caisse', 'CASH', 'cash/movement-document',
           'Sortie de caisse : deux exemplaires A5 sur une feuille (original pour la caisse, copie pour le bénéficiaire).'),
          (3500002, 'CASH_IN_VOUCHER', 'Bon d''entrée de caisse', 'CASH', 'cash/movement-document',
           'Entrée de caisse : deux exemplaires A5 sur une feuille (original pour la caisse, copie pour le déposant).'),
          (3500003, 'FUND_REQUEST', 'Demande de fonds', 'FUND', 'fund/voucher', 'Demande de fonds avec son circuit de validation.'),
          (3500004, 'NEED_STATEMENT', 'État de besoin', 'FUND', 'fund/voucher', 'État de besoin exprimé par un service.'),
          (3500005, 'DISBURSEMENT_VOUCHER', 'Bon de décaissement de fonds', 'FUND', 'fund/voucher',
           'Bon remis au bénéficiaire à chaque décaissement d''une demande de fonds.'),
          (3500006, 'JUSTIFICATION_SHEET', 'Fiche de justification d''avance', 'FUND', 'fund/voucher',
           'Dépenses justifiées d''une avance et solde à rendre ou à compléter.')
     ) AS v (id, code, label, domain, fallback, description)
WHERE NOT EXISTS (SELECT 1 FROM document_template t WHERE t.code = v.code);

-- Versions de depart (maquettes validees). Cases de signature 160 x 164 px, centrees.
INSERT INTO document_template_version (id, template_code, version_number, status, page_format, copies_json, style_json, blocks_json, note,
                                       created_by, published_by, published_at)
SELECT v.id, v.code, 1, 'PUBLISHED', v.format, v.copies::jsonb, '{"accent": "#c9a84c"}'::jsonb, v.blocks::jsonb,
       'Version de départ (maquette validée)', 'SYSTEM', 'SYSTEM', now()
FROM (VALUES
          (3500101, 'CASH_OUT_VOUCHER', 'A5_TWO_UP',
           '[{"label": "Original · à conserver par la caisse"}, {"label": "Copie · à remettre au bénéficiaire"}]',
           '[{"type": "HEADER", "qr": true},
             {"type": "TITLE", "title": "Bon de sortie", "copyLabel": true},
             {"type": "FIELDS", "columns": 3, "fields": ["date_heure", "caisse_session", "moyen", "beneficiaire", "nature", "demande_liee"]},
             {"type": "TEXT", "title": "Motif", "text": "{{motif}}"},
             {"type": "TOTALS", "label": "Montant", "words": true, "wordsLabel": "Montant en lettres"},
             {"type": "SIGNATURES", "width": 160, "height": 164, "boxes": [
                 {"label": "Caissier", "signer": "caissier"},
                 {"label": "Bénéficiaire", "note": "Nom, date et signature"},
                 {"label": "Responsable", "signer": "responsable", "note": "Visa"},
                 {"label": "Cachet {{exploitant}}", "stamp": true}]},
             {"type": "FOOTER", "text": "Tout bon raturé est nul."}]'),
          (3500102, 'CASH_IN_VOUCHER', 'A5_TWO_UP',
           '[{"label": "Original · à conserver par la caisse"}, {"label": "Copie · à remettre au déposant"}]',
           '[{"type": "HEADER", "qr": true},
             {"type": "TITLE", "title": "Bon d''entrée", "copyLabel": true},
             {"type": "FIELDS", "columns": 3, "fields": ["date_heure", "caisse_session", "moyen", "deposant", "nature", "demande_liee"]},
             {"type": "TEXT", "title": "Motif", "text": "{{motif}}"},
             {"type": "TOTALS", "label": "Montant", "words": true, "wordsLabel": "Montant encaissé en lettres"},
             {"type": "SIGNATURES", "width": 160, "height": 164, "boxes": [
                 {"label": "Déposant", "note": "Nom, date et signature"},
                 {"label": "Caissier", "signer": "caissier"},
                 {"label": "Cachet {{exploitant}}", "stamp": true}]},
             {"type": "FOOTER", "text": "Ce bon ne remplace pas la facture."}]'),
          (3500103, 'FUND_REQUEST', 'A4',
           '[{"label": "Exemplaire · à transmettre à la comptabilité"}]',
           '[{"type": "HEADER", "qr": true},
             {"type": "TITLE", "title": "Demande de fonds", "copyLabel": true},
             {"type": "FIELDS", "columns": 4, "fields": ["demandeur", "service", {"key": "date", "label": "Date de la demande"},
                 {"key": "date_souhaitee", "label": "Besoin pour le"}, "type", "etat_de_besoin",
                 {"key": "caisse_de_remise", "label": "Caisse de décaissement"}, "beneficiaire"]},
             {"type": "TEXT", "title": "Objet", "text": "{{objet}}"},
             {"type": "LINES", "table": "lignes", "title": "Lignes"},
             {"type": "TOTALS", "label": "Total", "words": true, "wordsLabel": "Total en lettres"},
             {"type": "LINES", "table": "circuit_de_validation", "title": "Circuit de validation",
              "columns": ["niveau", "nom", "decision", "date", "commentaire"]},
             {"type": "SIGNATURES", "width": 160, "height": 164, "boxes": [
                 {"label": "Demandeur", "signer": "demandeur"},
                 {"label": "Chef de service", "signer": "niveau_1"},
                 {"label": "Comptable", "signer": "niveau_2"},
                 {"label": "Gérant", "note": "Bon pour accord"},
                 {"label": "Cachet {{exploitant}}", "stamp": true}]},
             {"type": "FOOTER", "text": "Justificatifs à remettre sous 7 jours après le décaissement."}]'),
          (3500104, 'NEED_STATEMENT', 'A4',
           '[{"label": "Exemplaire · à conserver par le service"}]',
           '[{"type": "HEADER", "qr": true},
             {"type": "TITLE", "title": "État de besoin", "copyLabel": true},
             {"type": "FIELDS", "columns": 4, "fields": ["demandeur", "service", "site", "centre_de_cout",
                 {"key": "date", "label": "Date d''expression"}, {"key": "date_souhaitee", "label": "Besoin pour le"}, "urgence", "etat"]},
             {"type": "TEXT", "title": "Motif du besoin", "text": "{{objet}}"},
             {"type": "LINES", "table": "besoins", "title": "Besoins", "columns": [
                 {"key": "n", "label": "N°", "width": 26}, "designation", {"key": "quantite", "label": "Qté", "width": 46},
                 {"key": "unite", "width": 52}, {"key": "prix_estime", "label": "PU estimé", "width": 78}, {"key": "total", "width": 82},
                 {"key": "fournisseur_suggere", "width": 112}]},
             {"type": "TOTALS", "label": "Total estimé", "words": true, "wordsLabel": "Total estimé en lettres"},
             {"type": "FIELDS", "title": "Décision et suite donnée", "columns": 4,
              "fields": ["decide_par", "le", "commentaire", {"key": "demandes_de_fonds", "label": "Demandes de fonds"}]},
             {"type": "SIGNATURES", "width": 160, "height": 164, "boxes": [
                 {"label": "Demandeur", "signer": "demandeur"},
                 {"label": "Chef de service", "signer": "chef_de_service"},
                 {"label": "Gérant", "note": "Bon pour accord"},
                 {"label": "Cachet {{exploitant}}", "stamp": true}]},
             {"type": "FOOTER", "text": "Un état de besoin n''engage aucune dépense avant sa transformation."}]')
     ) AS v (id, code, format, copies, blocks)
WHERE NOT EXISTS (SELECT 1 FROM document_template_version d WHERE d.template_code = v.code);

-- Droit.
WITH new_permission (name, display_name, module, action) AS (
    VALUES ('SYSTEM_DOCUMENT_TEMPLATES', 'Gerer les modeles de documents (brouillon, apercu, publication)', 'SYSTEM', 'DOCUMENT_TEMPLATES')
), numbered AS (
    SELECT n.*, ROW_NUMBER() OVER (ORDER BY n.name) AS rank
    FROM new_permission n
    WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.name = n.name)
)
INSERT INTO permission (id, name, display_name, module, action, is_system_permission, is_active)
SELECT COALESCE((SELECT MAX(id) FROM permission), 0) + rank, name, display_name, module, action, TRUE, TRUE
FROM numbered;

WITH grants (role_name, permission_name) AS (
    VALUES ('SUPER_ADMIN', 'SYSTEM_DOCUMENT_TEMPLATES'), ('ADMIN', 'SYSTEM_DOCUMENT_TEMPLATES')
)
INSERT INTO role_permission (role_id, permission_id, created_by, is_active)
SELECT r.id, p.id, 'SYSTEM', TRUE
FROM grants g
JOIN role r ON r.name = g.role_name
JOIN permission p ON p.name = g.permission_name
WHERE NOT EXISTS (SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
