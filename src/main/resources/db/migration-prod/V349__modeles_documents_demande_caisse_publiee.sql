-- Modeles de documents : la demande de caisse (brouillon de depart du lot D4) est publiee.
-- Une version deja publiee depuis le back-office est gardee : rien ne change alors.
UPDATE document_template_version
SET status       = 'PUBLISHED',
    published_by = 'migration',
    published_at = now(),
    updated_at   = now()
WHERE template_code = 'CASH_REQUEST'
  AND status = 'DRAFT'
  AND NOT EXISTS (SELECT 1 FROM document_template_version p
                  WHERE p.template_code = 'CASH_REQUEST' AND p.status = 'PUBLISHED');
