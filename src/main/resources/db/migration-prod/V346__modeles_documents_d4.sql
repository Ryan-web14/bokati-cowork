-- Modeles de documents, lot D4 : autres pieces de fonds, d'achats, de comptabilite et de caisse.
-- Modeles enregistres avec un brouillon de depart (en-tete commun, toutes les sections de la piece,
-- cases de signature) : ils ne sont pas publies, faute de maquette validee par l'exploitant ; la
-- piece sort de son gabarit d'origine jusqu'a la publication depuis le back-office.

INSERT INTO document_template (id, code, label, family, domain, fallback_template, description)
SELECT v.id, v.code, v.label, 'PDF', v.domain, v.fallback, v.description
FROM (VALUES
          (3520001, 'PURCHASE_ORDER', 'Bon de commande fournisseur', 'ACCOUNTING', 'fund/voucher', 'Commande passée à un fournisseur.'),
          (3520002, 'VAT_RETURN', 'Déclaration de TVA', 'ACCOUNTING', 'fund/voucher', 'Déclaration mensuelle de TVA.'),
          (3520003, 'BANK_RECONCILIATION', 'État de rapprochement bancaire', 'ACCOUNTING', 'fund/voucher', 'Rapprochement d''un compte bancaire.'),
          (3520004, 'LOAN_SCHEDULE', 'Tableau d''amortissement d''emprunt', 'ACCOUNTING', 'fund/voucher', 'Échéancier d''un emprunt.'),
          (3520005, 'BANK_DEPOSIT_SLIP', 'Bordereau de remise en banque', 'ACCOUNTING', 'fund/voucher',
           'Espèces et chèques remis à la banque.'),
          (3520006, 'SUPPLIER_STATEMENT', 'Relevé fournisseur', 'ACCOUNTING', 'fund/voucher', 'Écritures et solde d''un fournisseur.'),
          (3520007, 'PARTNER_STATEMENT', 'Relevé de compte partenaire', 'ACCOUNTING', 'fund/voucher', 'Écritures et solde d''un compte partenaire.'),
          (3520008, 'CASH_REQUEST', 'Demande de caisse', 'CASH', 'cash/request-document',
           'Demande de justificatif, de remise de fonds, d''avance ou de remboursement.'),
          (3520009, 'CASH_CLOSING_REPORT', 'Rapport de clôture de caisse', 'CASH', 'cash/session-closing-report',
           'Comptes, écart et mouvements d''une session de caisse.')
     ) AS v (id, code, label, domain, fallback, description)
WHERE NOT EXISTS (SELECT 1 FROM document_template t WHERE t.code = v.code);

-- Brouillons de depart.
INSERT INTO document_template_version (id, template_code, version_number, status, page_format, copies_json, style_json, blocks_json, note,
                                       created_by)
SELECT v.id, v.code, COALESCE((SELECT MAX(d.version_number) FROM document_template_version d WHERE d.template_code = v.code), 0) + 1,
       'DRAFT', 'A4', v.copies::jsonb, '{"accent": "#c9a84c"}'::jsonb, v.blocks::jsonb,
       'Brouillon de départ, à relire avant publication', 'SYSTEM'
FROM (VALUES
          (3520101, 'DISBURSEMENT_VOUCHER', '[{"label": "Original · à conserver par la caisse"}, {"label": "Copie · à remettre au bénéficiaire"}]',
           '[{"type": "HEADER", "qr": true}, {"type": "TITLE", "copyLabel": true},
             {"type": "FIELDS", "columns": 4, "fields": ["numero", "beneficiaire", "objet", "mouvement_de_caisse", "session", "caissiere",
                 "decaissement_date", "reste_a_decaisser"]},
             {"type": "LINES", "table": "lignes", "title": "Lignes de la demande"},
             {"type": "TOTALS", "label": "Montant décaissé", "words": true, "wordsLabel": "Montant décaissé en lettres"},
             {"type": "TEXT", "text": "{{engagement}}", "boxed": true},
             {"type": "SIGNATURES", "width": 160, "height": 164, "boxes": [
                 {"label": "Bénéficiaire", "note": "Nom, date et signature"},
                 {"label": "Caissière", "signer": "caissiere"},
                 {"label": "Cachet {{exploitant}}", "stamp": true}]},
             {"type": "FOOTER", "text": "Tout bon raturé est nul."}]'),
          (3520102, 'JUSTIFICATION_SHEET', '[{"label": "Exemplaire · à transmettre à la comptabilité"}]',
           '[{"type": "HEADER", "qr": true}, {"type": "TITLE", "copyLabel": true}, {"type": "SECTIONS", "columns": 4},
             {"type": "TOTALS", "label": "Total justifié", "words": true, "wordsLabel": "Total justifié en lettres"},
             {"type": "SIGNATURES", "width": 160, "height": 164, "boxes": [
                 {"label": "Bénéficiaire", "note": "Nom, date et signature"},
                 {"label": "Comptable valideur", "signer": "comptable_valideur"},
                 {"label": "Cachet {{exploitant}}", "stamp": true}]},
             {"type": "FOOTER"}]'),
          (3520103, 'PURCHASE_ORDER', '[{"label": "Exemplaire fournisseur"}]', '[{"type": "HEADER", "qr": true}, {"type": "TITLE", "copyLabel": true},
             {"type": "SECTIONS", "columns": 4}, {"type": "TOTALS", "label": "Total TTC", "words": true, "wordsLabel": "Total TTC en lettres"},
             {"type": "SIGNATURES", "fromData": true, "width": 160, "height": 164}, {"type": "FOOTER"}]'),
          (3520104, 'VAT_RETURN', '[]', '[{"type": "HEADER", "qr": true}, {"type": "TITLE"}, {"type": "SECTIONS", "columns": 2},
             {"type": "TOTALS", "label": "TVA due", "words": true, "wordsLabel": "TVA due en lettres"},
             {"type": "SIGNATURES", "fromData": true, "width": 160, "height": 164}, {"type": "FOOTER"}]'),
          (3520105, 'BANK_RECONCILIATION', '[]', '[{"type": "HEADER", "qr": true}, {"type": "TITLE"}, {"type": "SECTIONS", "columns": 2},
             {"type": "SIGNATURES", "fromData": true, "width": 160, "height": 164}, {"type": "FOOTER"}]'),
          (3520106, 'LOAN_SCHEDULE', '[]', '[{"type": "HEADER", "qr": false}, {"type": "TITLE"}, {"type": "SECTIONS", "columns": 3}, {"type": "FOOTER"}]'),
          (3520107, 'BANK_DEPOSIT_SLIP', '[{"label": "Original · à conserver par la caisse"}, {"label": "Copie · pour la banque"}]',
           '[{"type": "HEADER", "qr": true}, {"type": "TITLE", "copyLabel": true}, {"type": "SECTIONS", "columns": 4},
             {"type": "TOTALS", "label": "Total remis", "words": true, "wordsLabel": "Total remis en lettres"},
             {"type": "SIGNATURES", "fromData": true, "width": 160, "height": 164}, {"type": "FOOTER"}]'),
          (3520108, 'SUPPLIER_STATEMENT', '[]', '[{"type": "HEADER", "qr": false}, {"type": "TITLE"}, {"type": "SECTIONS", "columns": 3},
             {"type": "SIGNATURES", "fromData": true, "width": 160, "height": 120}, {"type": "FOOTER"}]'),
          (3520109, 'PARTNER_STATEMENT', '[]', '[{"type": "HEADER", "qr": false}, {"type": "TITLE"}, {"type": "SECTIONS", "columns": 3},
             {"type": "SIGNATURES", "fromData": true, "width": 160, "height": 120}, {"type": "FOOTER"}]'),
          (3520110, 'CASH_REQUEST', '[]', '[{"type": "HEADER", "qr": false}, {"type": "TITLE"}, {"type": "SECTIONS", "columns": 3},
             {"type": "TOTALS", "label": "Montant demandé", "words": true, "wordsLabel": "Montant demandé en lettres"},
             {"type": "SIGNATURES", "width": 160, "height": 164, "boxes": [
                 {"label": "Demandeur", "signer": "demandeur"}, {"label": "Valideur", "signer": "valideur"}]},
             {"type": "FOOTER"}]'),
          (3520111, 'CASH_CLOSING_REPORT', '[]', '[{"type": "HEADER", "qr": false}, {"type": "TITLE"}, {"type": "SECTIONS", "columns": 4},
             {"type": "SIGNATURES", "width": 160, "height": 164, "boxes": [
                 {"label": "Caissier", "signer": "caissier"}, {"label": "Superviseur", "signer": "superviseur"},
                 {"label": "Cachet {{exploitant}}", "stamp": true}]},
             {"type": "FOOTER"}]')
     ) AS v (id, code, copies, blocks)
WHERE NOT EXISTS (SELECT 1 FROM document_template_version d WHERE d.template_code = v.code AND d.status = 'DRAFT');
