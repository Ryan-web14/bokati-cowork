-- Modeles de documents, lot D5 : pieces clients. Factures et pieces de facturation (INVOICE),
-- avoirs, recus de paiement, releves et recus de portefeuille, confirmations de reservation.
-- Brouillons de depart non publies : chaque piece sort de son gabarit d'origine jusqu'a la
-- publication. Une piece de facturation scellee retient la version de modele qui l'a produite
-- (0 : gabarit d'origine) pour qu'une reimpression reste identique.

ALTER TABLE billing_document ADD COLUMN IF NOT EXISTS template_version INTEGER;

INSERT INTO document_template (id, code, label, family, domain, fallback_template, description)
SELECT v.id, v.code, v.label, 'PDF', v.domain, v.fallback, v.description
FROM (VALUES
          (3530001, 'INVOICE', 'Facture, devis, proforma, note de débit, facture rectificative', 'BILLING', 'billing/document',
           'Pièces de facturation hors avoir. Une pièce scellée garde la version qui l''a produite.'),
          (3530002, 'CREDIT_NOTE', 'Avoir', 'BILLING', 'billing/avoir-note-credit', 'Avoir sur une facture.'),
          (3530003, 'PAYMENT_RECEIPT', 'Reçu de paiement', 'PAYMENT', 'payment/receipt', 'Reçu remis après un paiement.'),
          (3530004, 'WALLET_STATEMENT', 'Relevé de portefeuille', 'PAYMENT', 'wallet/statement', 'Opérations d''un portefeuille sur une période.'),
          (3530005, 'WALLET_RECEIPT', 'Reçu d''opération de portefeuille', 'PAYMENT', 'wallet/transaction-receipt',
           'Reçu d''une opération de portefeuille.'),
          (3530006, 'BOOKING_CONFIRMATION', 'Confirmation de réservation', 'BOOKING', 'booking/confirmation-pdf',
           'Confirmation envoyée au client, avec le QR d''arrivée.')
     ) AS v (id, code, label, domain, fallback, description)
WHERE NOT EXISTS (SELECT 1 FROM document_template t WHERE t.code = v.code);

INSERT INTO document_template_version (id, template_code, version_number, status, page_format, copies_json, style_json, blocks_json, note,
                                       created_by)
SELECT v.id, v.code, 1, 'DRAFT', v.format, '[]'::jsonb, '{"accent": "#c9a84c"}'::jsonb, v.blocks::jsonb,
       'Brouillon de départ, à relire avant publication', 'SYSTEM'
FROM (VALUES
          (3530101, 'INVOICE', 'A4', '[{"type": "HEADER", "qr": true}, {"type": "TITLE"},
             {"type": "SECTIONS", "columns": 3, "exclude": ["totaux"]},
             {"type": "FIELDS", "title": "Totaux", "columns": 4, "fields": ["sous_total", "remise", "tva", "centimes_additionnels",
                 "deja_paye", "reste_du"]},
             {"type": "TOTALS", "label": "Total TTC", "words": true, "wordsLabel": "Arrêté la présente pièce à la somme de"},
             {"type": "SIGNATURES", "width": 160, "height": 120, "boxes": [
                 {"label": "Visa {{exploitant}}", "signer": "emetteur", "note": "Cachet et signature"}]},
             {"type": "FOOTER", "text": "Facture certifiée par notre système"}]'),
          (3530102, 'CREDIT_NOTE', 'A4', '[{"type": "HEADER", "qr": true}, {"type": "TITLE"},
             {"type": "SECTIONS", "columns": 3, "exclude": ["totaux"]},
             {"type": "TOTALS", "label": "Montant de l''avoir", "words": true, "wordsLabel": "Montant de l''avoir en lettres"},
             {"type": "SIGNATURES", "width": 160, "height": 120, "boxes": [{"label": "Visa {{exploitant}}", "note": "Cachet et signature"}]},
             {"type": "FOOTER"}]'),
          (3530103, 'PAYMENT_RECEIPT', 'A4', '[{"type": "HEADER", "qr": true}, {"type": "TITLE"}, {"type": "SECTIONS", "columns": 3},
             {"type": "TOTALS", "label": "Montant payé", "words": true, "wordsLabel": "Reçu la somme de"},
             {"type": "SIGNATURES", "width": 160, "height": 120, "boxes": [{"label": "Encaissé par", "signer": "encaisse_par"},
                 {"label": "Cachet {{exploitant}}", "stamp": true}]},
             {"type": "FOOTER", "text": "Document vérifiable en ligne au moyen du code QR"}]'),
          (3530104, 'WALLET_STATEMENT', 'A4', '[{"type": "HEADER", "qr": false}, {"type": "TITLE"}, {"type": "SECTIONS", "columns": 4}, {"type": "FOOTER"}]'),
          (3530105, 'WALLET_RECEIPT', 'A6', '[{"type": "HEADER", "qr": false}, {"type": "TITLE"}, {"type": "SECTIONS", "columns": 2},
             {"type": "FOOTER"}]'),
          (3530106, 'BOOKING_CONFIRMATION', 'A4', '[{"type": "HEADER", "qr": true}, {"type": "TITLE"}, {"type": "SECTIONS", "columns": 3},
             {"type": "TEXT", "boxed": true, "text": "Présentez le code QR ou le code {{code_verification}} à l''accueil à votre arrivée."},
             {"type": "FOOTER"}]')
     ) AS v (id, code, format, blocks)
WHERE NOT EXISTS (SELECT 1 FROM document_template_version d WHERE d.template_code = v.code);
