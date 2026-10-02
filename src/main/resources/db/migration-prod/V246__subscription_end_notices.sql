-- Les avis de fin d abonnement · J-7, J-3, le jour meme, puis la cloture le lendemain.
--
-- Le worker passe toutes les heures : sans marqueur, chaque passe renverrait le meme avis. La
-- colonne retient l etape la plus urgente deja envoyee (7, puis 3, puis 0) · un avis ne part
-- donc qu une fois, et un renouvellement la remet a zero pour la periode suivante.

ALTER TABLE subscription
    ADD COLUMN IF NOT EXISTS end_notice_stage INT;

-- Ce que le balayage parcourt · les fins proches sur les abonnements encore ouverts.
CREATE INDEX IF NOT EXISTS idx_subscription_period_end_status
    ON subscription (status, current_period_end);
