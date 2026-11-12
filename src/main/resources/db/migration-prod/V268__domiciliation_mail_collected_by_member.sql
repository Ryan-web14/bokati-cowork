-- Tranche 6 du centre de gestion des entreprises · le courrier d'une organisation domiciliee se
-- retire par son gerant ou un membre autorise ; le retrait garde lequel.
ALTER TABLE mail_item ADD COLUMN IF NOT EXISTS collected_by_member VARCHAR(120);
