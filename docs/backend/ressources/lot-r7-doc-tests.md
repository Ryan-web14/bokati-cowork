# Lot R7 : doc et tests

> Partie du [plan ressources](README.md). Fait le 18/11/2026 sur `feature/ressources`.

## Écart Rn-6 réglé

- Une règle tarifaire modifiée (`PUT /resource-pricing-rules/{id}`) est revalidée comme à la création : fin avant début, validité inversée, minimum au-dessus du maximum, prix de base sans prix, ajustement sans valeur, segment, canal ou jour férié inconnus. Réponse `400 VALIDATION_FAILED`, rien n'est enregistré.
- `clear` vide des conditions : `startsAt`, `endsAt`, `validFrom`, `validUntil`, `dayOfWeek`, `daysOfWeek`, `lastMinuteMinutes`, `minLeadMinutes`, `minParticipants`, `maxParticipants`, `minDurationMinutes`, `maxDurationMinutes`, `customerSegment`, `planCode`, `channel`, `holidayMode`, `adjustment`. Un nom inconnu est refusé.

## Documentation frontend

- `docs/final-frontend-doc/ressource/README.md` réécrit à jour du chantier (notions, statuts, parcours, règles et erreurs en français, droits, effets automatiques). Tous les écarts relevés y sont marqués réglés.
- Une fiche par fonction dans `docs/final-frontend-doc/ressource/fonctions/` : cycle de vie, créneaux, calendrier, tarifs, sites et plans, vente, exploitation, pilotage.
- `booking/README.md` : dépassement et non-présentation à l'accueil, suppléments, remises et lots, exploitation (tâches, règlement, état des lieux) ; prix recalculé au changement de ressource ; écart d'annulation par défaut en partie réglé.
- `portal/parcours-reservations.md` : règlement d'usage, invitation d'agenda, état des lieux, signalement d'incident.
- Routes régénérées (`python tools/frontend-doc/generate.py docs/final-frontend-doc`) : 119 routes de ressources (80 avant le chantier).

## Tests

Tests unitaires par lot : `ResourceR2Test` à `ResourceR7Test`, `ResourceR5aTest`, `ResourceR5bTest`, `BookingR5cTest`, `BookingR5dTest`, `ResourceR5eTest`, plus les tests existants adaptés. Suite unitaire complète verte à chaque lot.

Tests d'intégration sur base réelle (moteur de prix et créneaux, migrations V373 à V380) : **non lancés**. La suite d'intégration demande Docker et a déjà été arrêtée faute de mémoire ; elle reste à lancer à la demande, avant la fusion.
