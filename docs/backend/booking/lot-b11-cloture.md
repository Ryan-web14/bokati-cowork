# Lot B11 : clôture du plan booking

> Partie du [plan booking](README.md). 19/11/2026, `feature/booking`. Tous les lots du plan sont faits ; ce document rassemble ce qu'il reste à faire **côté exploitant** avant et après la mise en production, et l'état des tests.

## Lots livrés

| Lot | Contenu | Doc |
| --- | --- | --- |
| B0 | Sécurité et traçabilité | [lot-b0](lot-b0-securite-tracabilite.md) |
| B10 | Visibilité des ressources par client, prix au cas par cas | [lot-b10](lot-b10-visibilite-prix-clients.md) |
| B1 | Créneaux et cycle de vie (échéance, expiration, règlement constaté) | [lot-b1](lot-b1-creneaux-cycle-de-vie.md) |
| B2, B2b | Argent ; reports, écart de prix, options | [lot-b2](lot-b2-argent.md), [lot-b2b](lot-b2b-reports-options.md) |
| B8 | Messages en français, fuseau | [lot-b8](lot-b8-messages-fuseau.md) |
| L0 | Socle des listes de choix | [lot-l0](../listes-de-choix/lot-l0-socle.md) |
| B7 | Listes, recherche, réponse complète | [lot-b7](lot-b7-listes-recherche-reponse.md) |
| B3 | Rendez-vous réglé sur place | [lot-b3](lot-b3-regle-sur-place.md) |
| B4 | Ajustement du prix | [lot-b4](lot-b4-ajustement-prix.md) |
| B5 | Rendez-vous au téléphone | [lot-b5](lot-b5-telephone.md) |
| B6 | Rappels, liste d'attente | [lot-b6](lot-b6-notifications-attente.md) |
| B9a, B9b | Venue, planning, séries | [lot-b9a-b9b](lot-b9a-b9b-venue-planning.md) |
| B9c, B9d | Fiabilité, acompte, invités, détails | [lot-b9c-b9d](lot-b9c-b9d-fiabilite-invites.md) |
| B9e | Entreprises | [lot-b9e](lot-b9e-entreprises.md) |
| B9f | Remplissage | [lot-b9f](lot-b9f-remplissage.md) |
| B9g | Suivi | [lot-b9g](lot-b9g-suivi.md) |

## Migrations

Dev V381 à V397, prod V375 à V391 (dont V385/V379, équivalents en devises de la facturation, et V387/V381, socle des listes de choix). Toutes en `IF NOT EXISTS` ou avec garde : rejouables. La suite d'intégration (Docker) n'a pas été relancée sur ces migrations : à faire avant la mise en production (`./mvnw test` avec Docker).

## Réglages à décider (écran des paramètres, domaine `BOOKING`)

| Réglage | Défaut | Décision de l'exploitant |
| --- | --- | --- |
| `bookings.pay-on-site.portal-enabled` | coupé | Ouvrir le règlement sur place au portail pour tous, ou client par client |
| `bookings.early-release.minutes` | 0 (coupé) | Libérer un créneau sans arrivée (par exemple 30) |
| `bookings.last-minute.discount-rate` | 0 (coupé) | Remise de dernière minute (par exemple 15) |
| `bookings.calendar-feed.enabled` | coupé | Proposer l'agenda synchronisé aux membres |
| `bookings.reliability.threshold`, `.mode` | 60, `DEPOSIT` | Seuil de fiabilité et garantie (acompte ou prépaiement) |
| `bookings.deposit.rate` | 30 | Acompte en % |
| `bookings.price-adjustment.max-rate` | 20 | Plafond d'ajustement sans approbation |
| `bookings.reminders.offsets-minutes` | `1440,120` | Délais des rappels |

## Actions de l'exploitant

1. **Droits** : `BOOKING:PRICE_ADJUST` et `BOOKING:PRICE_APPROVE` sont donnés à `SUPER_ADMIN`, `ADMIN`, `MANAGER` ; `SYSTEM:REFERENCE_LISTS` à `SUPER_ADMIN` et `ADMIN`. Ajuster dans l'écran des rôles si besoin.
2. **Circuit d'approbation** `BOOKING_PRICE_ADJUSTMENT` (N+1) : vérifier la hiérarchie (organigramme) pour que chaque agent ait un responsable.
3. **Listes de l'administrateur** à relire : motifs d'annulation et de refus, motifs d'ajustement, dispositions de salle, étiquettes (`/reference-lists?module=BOOKING`).
4. **Modèles WhatsApp** à créer et faire approuver par Meta avant tout envoi : rappel de réservation (avec « je viens / j'annule »), offre de dernière minute aux membres volontaires. Rien n'est soumis sans ton accord ; tant qu'ils ne le sont pas, rappels et offres partent par courriel ou au portail seulement.
5. **Rattrapage KYC** (reste du chantier ressources) : avec un compte d'administration, `POST /sni/api/v1/kyc/cases/backfill` (compte sans rien créer), puis `POST /sni/api/v1/kyc/cases/backfill?apply=true` dans chaque environnement.

## Tests

Une classe de tests par lot (`BookingB0Test` à `BookingB9gTest`, `ReferenceListsL0Test`) ; suite unitaire complète verte à chaque livraison (dernier passage : voir le commit du lot B9g). Les tests d'intégration (Docker) restent à lancer avant la production.

## Doc frontend

[booking](../../final-frontend-doc/booking/README.md) (une section par lot, en fin de page), [portail](../../final-frontend-doc/portal/parcours-reservations.md), [listes de choix](../../final-frontend-doc/listes-de-choix/README.md), [approbation](../../final-frontend-doc/approval/README.md) (centre `BOOKING`), [paramètres](../../final-frontend-doc/parametres/cles.md) (clés `bookings.*`) ; routes régénérées depuis le code (`booking/routes.md`, `portal/routes.md`).
