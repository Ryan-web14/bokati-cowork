-- Modeles de documents, lot E1 : courriels en blocs. Un courriel dont le modele est publie est servi
-- par le gabarit genere a la place de son fichier d'origine ; sans version publiee, rien ne change.
-- Jetons obligatoires par modele (codes et liens des courriels de securite, lot E2).

ALTER TABLE document_template ADD COLUMN IF NOT EXISTS required_tokens VARCHAR(300);

INSERT INTO document_template (id, code, label, family, domain, fallback_template, description)
SELECT v.id, v.code, v.label, 'EMAIL', v.domain, v.fallback, v.description
FROM (VALUES
          (3540001, 'EMAIL_BILLING_DOCUMENT', 'Courriel : document de facturation', 'BILLING', 'email/billing-document',
           'Envoi d''une facture, d''un devis ou d''un avoir, PDF joint.'),
          (3540002, 'EMAIL_PAYMENT_CONFIRMATION', 'Courriel : paiement reçu', 'BILLING', 'email/payment-confirmation',
           'Confirmation d''un paiement sur une pièce de facturation.')
     ) AS v (id, code, label, domain, fallback, description)
WHERE NOT EXISTS (SELECT 1 FROM document_template t WHERE t.code = v.code);

INSERT INTO document_template_version (id, template_code, version_number, status, page_format, copies_json, style_json, blocks_json, note,
                                       created_by)
SELECT v.id, v.code, 1, 'DRAFT', 'A4', '[]'::jsonb, '{}'::jsonb, v.blocks::jsonb, 'Brouillon de départ, à relire avant publication', 'SYSTEM'
FROM (VALUES
          (3540101, 'EMAIL_BILLING_DOCUMENT', '[
             {"type": "SUBJECT", "text": "{{title}} {{documentNumber}}"},
             {"type": "PREHEADER", "text": "{{preheader}}"},
             {"type": "TITLE", "text": "{{title}}"},
             {"type": "GREETING", "text": "Bonjour {{customerName}},"},
             {"type": "TEXT", "text": "{{message}}"},
             {"type": "DETAILS", "title": "Récapitulatif", "rows": [
                 {"label": "Référence", "value": "{{documentNumber}}"}, {"label": "Date", "value": "{{issueDate}}"},
                 {"label": "Échéance", "value": "{{dueDate}}"}, {"label": "Montant", "value": "{{totalAmount}}"},
                 {"label": "Déjà payé", "value": "{{paidAmount}}"}, {"label": "Reste dû", "value": "{{balanceDue}}"}]},
             {"type": "ATTACHMENT_NOTE", "text": "Le document est joint à ce message au format PDF."},
             {"type": "LEGAL"}]'),
          (3540102, 'EMAIL_PAYMENT_CONFIRMATION', '[
             {"type": "SUBJECT", "text": "Paiement reçu pour {{documentNumber}}"},
             {"type": "PREHEADER", "text": "{{preheader}}"},
             {"type": "TITLE", "text": "Paiement reçu"},
             {"type": "GREETING", "text": "Bonjour {{customerName}},"},
             {"type": "TEXT", "text": "Nous avons bien reçu votre paiement. Merci."},
             {"type": "DETAILS", "rows": [
                 {"label": "Pièce", "value": "{{documentNumber}}"}, {"label": "Montant payé", "value": "{{paidAmount}}"},
                 {"label": "Moyen", "value": "{{paymentMethod}}"}, {"label": "Reste dû", "value": "{{balanceDue}}"}]},
             {"type": "LEGAL"}]')
     ) AS v (id, code, blocks)
WHERE NOT EXISTS (SELECT 1 FROM document_template_version d WHERE d.template_code = v.code);
