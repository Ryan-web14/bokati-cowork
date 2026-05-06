-- ── Normalisation colonne "interest" ────────────────────────────────────────
-- Mappe les valeurs texte libres (français / anglais / variantes) vers les
-- constantes LeadInterest. Toute valeur non reconnue → 'OTHER'.

UPDATE crm_lead
SET interest = CASE LOWER(TRIM(interest))

    -- MEETING_ROOM
    WHEN 'salle de reunion'         THEN 'MEETING_ROOM'
    WHEN 'salle de réunion'         THEN 'MEETING_ROOM'
    WHEN 'salles de reunion'        THEN 'MEETING_ROOM'
    WHEN 'salles de réunion'        THEN 'MEETING_ROOM'
    WHEN 'meeting room'             THEN 'MEETING_ROOM'
    WHEN 'meeting_room'             THEN 'MEETING_ROOM'
    WHEN 'salle reunion'            THEN 'MEETING_ROOM'

    -- WORKSPACE
    WHEN 'espace de travail'        THEN 'WORKSPACE'
    WHEN 'coworking'                THEN 'WORKSPACE'
    WHEN 'open space'               THEN 'WORKSPACE'
    WHEN 'workspace'                THEN 'WORKSPACE'
    WHEN 'poste de travail'         THEN 'WORKSPACE'
    WHEN 'espace coworking'         THEN 'WORKSPACE'

    -- DEDICATED_DESK
    WHEN 'bureau dédié'             THEN 'DEDICATED_DESK'
    WHEN 'bureau dedie'             THEN 'DEDICATED_DESK'
    WHEN 'bureau privatif dédié'    THEN 'DEDICATED_DESK'
    WHEN 'dedicated desk'           THEN 'DEDICATED_DESK'
    WHEN 'dedicated_desk'           THEN 'DEDICATED_DESK'
    WHEN 'poste dédié'              THEN 'DEDICATED_DESK'

    -- PRIVATE_OFFICE
    WHEN 'bureau privé'             THEN 'PRIVATE_OFFICE'
    WHEN 'bureau prive'             THEN 'PRIVATE_OFFICE'
    WHEN 'bureau privatif'          THEN 'PRIVATE_OFFICE'
    WHEN 'private office'           THEN 'PRIVATE_OFFICE'
    WHEN 'private_office'           THEN 'PRIVATE_OFFICE'
    WHEN 'bureau individuel'        THEN 'PRIVATE_OFFICE'

    -- VIRTUAL_OFFICE
    WHEN 'bureau virtuel'           THEN 'VIRTUAL_OFFICE'
    WHEN 'virtual office'           THEN 'VIRTUAL_OFFICE'
    WHEN 'virtual_office'           THEN 'VIRTUAL_OFFICE'
    WHEN 'domiciliation commerciale' THEN 'VIRTUAL_OFFICE'

    -- DOMICILIATION
    WHEN 'domiciliation'            THEN 'DOMICILIATION'
    WHEN 'adresse commerciale'      THEN 'DOMICILIATION'
    WHEN 'siege social'             THEN 'DOMICILIATION'
    WHEN 'siège social'             THEN 'DOMICILIATION'

    -- DAY_PASS
    WHEN 'day pass'                 THEN 'DAY_PASS'
    WHEN 'day_pass'                 THEN 'DAY_PASS'
    WHEN 'pass journalier'          THEN 'DAY_PASS'
    WHEN 'accès ponctuel'           THEN 'DAY_PASS'
    WHEN 'acces ponctuel'           THEN 'DAY_PASS'
    WHEN 'journée'                  THEN 'DAY_PASS'
    WHEN 'journee'                  THEN 'DAY_PASS'

    -- Déjà valides (au cas où des lignes ont une casse différente)
    WHEN 'workspace'                THEN 'WORKSPACE'
    WHEN 'meeting_room'             THEN 'MEETING_ROOM'
    WHEN 'virtual_office'           THEN 'VIRTUAL_OFFICE'
    WHEN 'domiciliation'            THEN 'DOMICILIATION'
    WHEN 'day_pass'                 THEN 'DAY_PASS'
    WHEN 'dedicated_desk'           THEN 'DEDICATED_DESK'
    WHEN 'private_office'           THEN 'PRIVATE_OFFICE'
    WHEN 'other'                    THEN 'OTHER'

    ELSE 'OTHER'
END
WHERE interest IS NOT NULL
  AND interest NOT IN (
      'WORKSPACE', 'MEETING_ROOM', 'VIRTUAL_OFFICE',
      'DOMICILIATION', 'DAY_PASS', 'DEDICATED_DESK',
      'PRIVATE_OFFICE', 'OTHER'
  );

-- ── Normalisation colonne "source" ───────────────────────────────────────────
UPDATE crm_lead
SET source = CASE LOWER(TRIM(source))

    -- WEBSITE
    WHEN 'site web'                 THEN 'WEBSITE'
    WHEN 'site internet'            THEN 'WEBSITE'
    WHEN 'website'                  THEN 'WEBSITE'
    WHEN 'web'                      THEN 'WEBSITE'
    WHEN 'formulaire'               THEN 'WEBSITE'
    WHEN 'formulaire site'          THEN 'WEBSITE'

    -- REFERRAL
    WHEN 'parrainage'               THEN 'REFERRAL'
    WHEN 'recommandation'           THEN 'REFERRAL'
    WHEN 'bouche a oreille'         THEN 'REFERRAL'
    WHEN 'bouche à oreille'         THEN 'REFERRAL'
    WHEN 'referral'                 THEN 'REFERRAL'
    WHEN 'reference'                THEN 'REFERRAL'
    WHEN 'référence'                THEN 'REFERRAL'

    -- COLD_CALL
    WHEN 'appel'                    THEN 'COLD_CALL'
    WHEN 'démarchage'               THEN 'COLD_CALL'
    WHEN 'demarchage'               THEN 'COLD_CALL'
    WHEN 'appel sortant'            THEN 'COLD_CALL'
    WHEN 'prospection'              THEN 'COLD_CALL'
    WHEN 'cold call'                THEN 'COLD_CALL'
    WHEN 'cold_call'                THEN 'COLD_CALL'

    -- SOCIAL_MEDIA
    WHEN 'réseaux sociaux'          THEN 'SOCIAL_MEDIA'
    WHEN 'reseaux sociaux'          THEN 'SOCIAL_MEDIA'
    WHEN 'social media'             THEN 'SOCIAL_MEDIA'
    WHEN 'social_media'             THEN 'SOCIAL_MEDIA'
    WHEN 'facebook'                 THEN 'SOCIAL_MEDIA'
    WHEN 'instagram'                THEN 'SOCIAL_MEDIA'
    WHEN 'linkedin'                 THEN 'SOCIAL_MEDIA'
    WHEN 'twitter'                  THEN 'SOCIAL_MEDIA'
    WHEN 'tiktok'                   THEN 'SOCIAL_MEDIA'
    WHEN 'whatsapp'                 THEN 'SOCIAL_MEDIA'

    -- PARTNER
    WHEN 'partenaire'               THEN 'PARTNER'
    WHEN 'partenariat'              THEN 'PARTNER'
    WHEN 'partner'                  THEN 'PARTNER'

    -- TRADE_SHOW
    WHEN 'salon'                    THEN 'TRADE_SHOW'
    WHEN 'foire'                    THEN 'TRADE_SHOW'
    WHEN 'salon professionnel'      THEN 'TRADE_SHOW'
    WHEN 'trade show'               THEN 'TRADE_SHOW'
    WHEN 'trade_show'               THEN 'TRADE_SHOW'
    WHEN 'exposition'               THEN 'TRADE_SHOW'

    -- EVENT
    WHEN 'événement'                THEN 'EVENT'
    WHEN 'evenement'                THEN 'EVENT'
    WHEN 'event'                    THEN 'EVENT'
    WHEN 'conférence'               THEN 'EVENT'
    WHEN 'conference'               THEN 'EVENT'
    WHEN 'atelier'                  THEN 'EVENT'
    WHEN 'meetup'                   THEN 'EVENT'
    WHEN 'networking'               THEN 'EVENT'

    WHEN 'other'                    THEN 'OTHER'
    ELSE 'OTHER'
END
WHERE source IS NOT NULL
  AND source NOT IN (
      'WEBSITE', 'REFERRAL', 'COLD_CALL', 'TRADE_SHOW',
      'PARTNER', 'SOCIAL_MEDIA', 'EVENT', 'OTHER'
  );
