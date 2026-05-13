-- =========================================================
-- RESOURCE TYPES
-- =========================================================

INSERT INTO resource_type (id, code, name, description, active, created_at, updated_at)
SELECT 750001, 'RTY-00001', 'Salle de reunion', 'Espace dedie aux reunions professionnelles, equipe de tables et chaises modulables', TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM resource_type WHERE code = 'RTY-00001');

INSERT INTO resource_type (id, code, name, description, active, created_at, updated_at)
SELECT 750002, 'RTY-00002', 'Bureau privatif', 'Bureau ferme a usage exclusif, adapte au travail individuel ou en petit groupe', TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM resource_type WHERE code = 'RTY-00002');

INSERT INTO resource_type (id, code, name, description, active, created_at, updated_at)
SELECT 750003, 'RTY-00003', 'Poste Open Space', 'Poste de travail partage en espace ouvert, ideal pour les freelances et travailleurs nomades', TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM resource_type WHERE code = 'RTY-00003');

INSERT INTO resource_type (id, code, name, description, active, created_at, updated_at)
SELECT 750004, 'RTY-00004', 'Salle de formation', 'Grande salle equipee pour animer des sessions de formation, workshops et seminaires', TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM resource_type WHERE code = 'RTY-00004');

INSERT INTO resource_type (id, code, name, description, active, created_at, updated_at)
SELECT 750005, 'RTY-00005', 'Cabine telephonique', 'Espace isole acoustiquement pour les appels et visioconferences en toute confidentialite', TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM resource_type WHERE code = 'RTY-00005');

INSERT INTO resource_type (id, code, name, description, active, created_at, updated_at)
SELECT 750006, 'RTY-00006', 'Espace evenementiel', 'Grande salle polyvalente pour l''organisation d''evenements, conferences et receptions', TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM resource_type WHERE code = 'RTY-00006');

INSERT INTO resource_type (id, code, name, description, active, created_at, updated_at)
SELECT 750007, 'RTY-00007', 'Parking', 'Place de stationnement securisee reservable a la journee ou au mois', TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM resource_type WHERE code = 'RTY-00007');

INSERT INTO resource_type (id, code, name, description, active, created_at, updated_at)
SELECT 750008, 'RTY-00008', 'Casier', 'Casier de rangement securise pour stocker affaires personnelles et equipements', TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM resource_type WHERE code = 'RTY-00008');

INSERT INTO resource_type (id, code, name, description, active, created_at, updated_at)
SELECT 750009, 'RTY-00009', 'Studio podcast', 'Studio insonorise equipe pour l''enregistrement audio et video de contenu professionnel', TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM resource_type WHERE code = 'RTY-00009');

INSERT INTO resource_type (id, code, name, description, active, created_at, updated_at)
SELECT 750010, 'RTY-00010', 'Labo / Atelier', 'Espace equipe pour les activites techniques, fabrication et prototypage', TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM resource_type WHERE code = 'RTY-00010');

-- =========================================================
-- RESOURCE GROUPS
-- =========================================================

INSERT INTO resource_group (id, code, name, description, portal_visible, active, created_at, updated_at)
SELECT 750101, 'RGP-00001', 'Premium', 'Espaces haut de gamme avec equipements et services eleves', TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM resource_group WHERE code = 'RGP-00001');

INSERT INTO resource_group (id, code, name, description, portal_visible, active, created_at, updated_at)
SELECT 750102, 'RGP-00002', 'Standard', 'Espaces a tarif accessible avec les equipements essentiels', TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM resource_group WHERE code = 'RGP-00002');

INSERT INTO resource_group (id, code, name, description, portal_visible, active, created_at, updated_at)
SELECT 750103, 'RGP-00003', 'Niveau 1', 'Ressources situees au premier etage du batiment', TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM resource_group WHERE code = 'RGP-00003');

INSERT INTO resource_group (id, code, name, description, portal_visible, active, created_at, updated_at)
SELECT 750104, 'RGP-00004', 'Niveau 2', 'Ressources situees au deuxieme etage du batiment', TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM resource_group WHERE code = 'RGP-00004');

INSERT INTO resource_group (id, code, name, description, portal_visible, active, created_at, updated_at)
SELECT 750105, 'RGP-00005', 'Niveau 3', 'Ressources situees au troisieme etage du batiment', TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM resource_group WHERE code = 'RGP-00005');

INSERT INTO resource_group (id, code, name, description, portal_visible, active, created_at, updated_at)
SELECT 750106, 'RGP-00006', 'Aile Nord', 'Ressources de l''aile nord, ambiance calme et lumineuse', TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM resource_group WHERE code = 'RGP-00006');

INSERT INTO resource_group (id, code, name, description, portal_visible, active, created_at, updated_at)
SELECT 750107, 'RGP-00007', 'Aile Sud', 'Ressources de l''aile sud, proche des espaces communs', TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM resource_group WHERE code = 'RGP-00007');

INSERT INTO resource_group (id, code, name, description, portal_visible, active, created_at, updated_at)
SELECT 750108, 'RGP-00008', 'Zone Reunion', 'Regroupement de toutes les salles de reunion et espaces collaboratifs', TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM resource_group WHERE code = 'RGP-00008');

INSERT INTO resource_group (id, code, name, description, portal_visible, active, created_at, updated_at)
SELECT 750109, 'RGP-00009', 'Zone Evenementielle', 'Regroupement des espaces destines aux evenements et grandes reunions', TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM resource_group WHERE code = 'RGP-00009');

INSERT INTO resource_group (id, code, name, description, portal_visible, active, created_at, updated_at)
SELECT 750110, 'RGP-00010', 'Usage Interne', 'Ressources a usage interne, non visibles sur le portail client', FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM resource_group WHERE code = 'RGP-00010');

-- =========================================================
-- RESOURCE POLICIES
-- =========================================================

-- Politique Standard : 30 min min, 4h max, 24h de preavis, annulation autorisee sous 12h
INSERT INTO resource_policy (id, code, name, description,
    min_booking_duration_minutes, max_booking_duration_minutes,
    min_booking_notice_minutes, cancellation_notice_minutes,
    allow_cancellation, active, created_at, updated_at)
SELECT 750201, 'RPL-00001', 'Politique Standard',
    'Politique de reservation standard pour la majorite des espaces : duree de 30 min a 4h, preavis de 24h, annulation possible sous 12h avant la reservation',
    30, 240, 1440, 720, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM resource_policy WHERE code = 'RPL-00001');

-- Politique Flexible : 15 min min, 8h max, 2h de preavis, annulation autorisee sous 1h
INSERT INTO resource_policy (id, code, name, description,
    min_booking_duration_minutes, max_booking_duration_minutes,
    min_booking_notice_minutes, cancellation_notice_minutes,
    allow_cancellation, active, created_at, updated_at)
SELECT 750202, 'RPL-00002', 'Politique Flexible',
    'Politique souple pour les besoins spontanes : duree de 15 min a 8h, preavis de 2h seulement, annulation possible 1h avant',
    15, 480, 120, 60, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM resource_policy WHERE code = 'RPL-00002');

-- Politique Stricte : 1h min, 4h max, 48h de preavis, aucune annulation
INSERT INTO resource_policy (id, code, name, description,
    min_booking_duration_minutes, max_booking_duration_minutes,
    min_booking_notice_minutes, cancellation_notice_minutes,
    allow_cancellation, active, created_at, updated_at)
SELECT 750203, 'RPL-00003', 'Politique Stricte',
    'Politique sans annulation pour les espaces a forte demande : duree de 1h a 4h, preavis de 48h obligatoire, aucune annulation permise',
    60, 240, 2880, 2880, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM resource_policy WHERE code = 'RPL-00003');

-- Politique Demi-journee : 3h min, 5h max, 24h de preavis, annulation sous 24h
INSERT INTO resource_policy (id, code, name, description,
    min_booking_duration_minutes, max_booking_duration_minutes,
    min_booking_notice_minutes, cancellation_notice_minutes,
    allow_cancellation, active, created_at, updated_at)
SELECT 750204, 'RPL-00004', 'Politique Demi-journee',
    'Politique adaptee aux reservations demi-journee (matin ou apres-midi) : duree de 3h a 5h, preavis 24h, annulation possible sous 24h',
    180, 300, 1440, 1440, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM resource_policy WHERE code = 'RPL-00004');

-- Politique Journee Complete : 7h min, 10h max, 48h de preavis, annulation sous 48h
INSERT INTO resource_policy (id, code, name, description,
    min_booking_duration_minutes, max_booking_duration_minutes,
    min_booking_notice_minutes, cancellation_notice_minutes,
    allow_cancellation, active, created_at, updated_at)
SELECT 750205, 'RPL-00005', 'Politique Journee Complete',
    'Politique pour les reservations a la journee entiere : duree de 7h a 10h, preavis de 48h, annulation sous 48h avant',
    420, 600, 2880, 2880, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM resource_policy WHERE code = 'RPL-00005');

-- Politique Horaire Express : 30 min min, 2h max, 30 min de preavis, annulation sous 15 min
INSERT INTO resource_policy (id, code, name, description,
    min_booking_duration_minutes, max_booking_duration_minutes,
    min_booking_notice_minutes, cancellation_notice_minutes,
    allow_cancellation, active, created_at, updated_at)
SELECT 750206, 'RPL-00006', 'Politique Horaire Express',
    'Politique rapide pour les cabines et petits espaces : 30 min a 2h, preavis de 30 min, annulation possible 15 min avant',
    30, 120, 30, 15, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM resource_policy WHERE code = 'RPL-00006');

-- Politique Evenementielle : 2h min, 24h max, 72h de preavis, aucune annulation
INSERT INTO resource_policy (id, code, name, description,
    min_booking_duration_minutes, max_booking_duration_minutes,
    min_booking_notice_minutes, cancellation_notice_minutes,
    allow_cancellation, active, created_at, updated_at)
SELECT 750207, 'RPL-00007', 'Politique Evenementielle',
    'Politique pour les espaces evenementiels et formations : duree de 2h a 24h, preavis de 72h, aucune annulation une fois confirmee',
    120, 1440, 4320, 4320, FALSE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM resource_policy WHERE code = 'RPL-00007');

-- Politique Abonnement Mensuel : 1h min, 10h max, 2h de preavis, annulation sous 24h
INSERT INTO resource_policy (id, code, name, description,
    min_booking_duration_minutes, max_booking_duration_minutes,
    min_booking_notice_minutes, cancellation_notice_minutes,
    allow_cancellation, active, created_at, updated_at)
SELECT 750208, 'RPL-00008', 'Politique Abonnement Mensuel',
    'Politique pour les membres avec abonnement mensuel : duree de 1h a 10h, preavis de 2h, annulation possible sous 24h',
    60, 600, 120, 1440, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM resource_policy WHERE code = 'RPL-00008');

-- Politique Parking : 60 min min, 720 min max (12h), 1h de preavis, annulation sous 2h
INSERT INTO resource_policy (id, code, name, description,
    min_booking_duration_minutes, max_booking_duration_minutes,
    min_booking_notice_minutes, cancellation_notice_minutes,
    allow_cancellation, active, created_at, updated_at)
SELECT 750209, 'RPL-00009', 'Politique Parking',
    'Politique pour les places de parking : duree de 1h a 12h, preavis de 1h, annulation possible 2h avant',
    60, 720, 60, 120, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM resource_policy WHERE code = 'RPL-00009');

-- Politique Casier : 1440 min min, 43200 min max (30 jours), 1h de preavis, annulation sous 24h
INSERT INTO resource_policy (id, code, name, description,
    min_booking_duration_minutes, max_booking_duration_minutes,
    min_booking_notice_minutes, cancellation_notice_minutes,
    allow_cancellation, active, created_at, updated_at)
SELECT 750210, 'RPL-00010', 'Politique Casier',
    'Politique pour les casiers de rangement : location a la journee (min) jusqu''au mois (max), preavis de 1h, annulation sous 24h',
    1440, 43200, 60, 1440, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM resource_policy WHERE code = 'RPL-00010');

-- =========================================================
-- SEQUENCE COUNTERS (synchronisation du moteur de sequences)
-- =========================================================

INSERT INTO sequence_counter (id, sequence_definition_id, sequence_code, period_key, current_value, version, created_at, updated_at)
SELECT 750301, 38, 'resource_type', 'GLOBAL', 10, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sequence_counter WHERE sequence_definition_id = 38 AND period_key = 'GLOBAL');

INSERT INTO sequence_counter (id, sequence_definition_id, sequence_code, period_key, current_value, version, created_at, updated_at)
SELECT 750302, 39, 'resource_group', 'GLOBAL', 10, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sequence_counter WHERE sequence_definition_id = 39 AND period_key = 'GLOBAL');

INSERT INTO sequence_counter (id, sequence_definition_id, sequence_code, period_key, current_value, version, created_at, updated_at)
SELECT 750303, 40, 'resource_policy', 'GLOBAL', 10, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sequence_counter WHERE sequence_definition_id = 40 AND period_key = 'GLOBAL');
