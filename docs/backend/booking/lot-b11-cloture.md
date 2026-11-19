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

## Réglages décidés le 19/11/2026 (écran des paramètres, domaine `BOOKING`)

Ces valeurs sont désormais les valeurs par défaut du logiciel ; elles restent modifiables à l'écran.

| Réglage | Défaut | Décision |
| --- | --- | --- |
| `bookings.pay-on-site.portal-enabled` | coupé | Client par client. Actions groupées : ouvrir ou fermer à tous (`PUT /bookings/pay-on-site/all`), puis sélectionner ou désélectionner des clients (`PUT /bookings/pay-on-site/clients`) |
| `bookings.early-release.minutes` | 30 | Créneau sans arrivée libéré 30 minutes après le début |
| `bookings.last-minute.discount-rate` | 10 | Remise de dernière minute de 10 % au portail |
| `bookings.calendar-feed.enabled` | ouvert | Agenda synchronisé proposé aux membres |
| `bookings.reliability.threshold` | 60 | Seuil de fiabilité |
| `bookings.reliability.mode` | `DEPOSIT` | Acompte par défaut ; `PREPAY` (prépaiement) reste possible par le réglage |
| `bookings.deposit.rate` | 50 | Acompte de 50 % |
| `bookings.price-adjustment.max-rate` | 20 | Plafond d'ajustement sans approbation |
| `bookings.reminders.offsets-minutes` | `1440,120` | Délais des rappels |
| `bookings.reminders.whatsapp-enabled` | coupé | À ouvrir quand `rappel_reservation` est approuvé par Meta |
| `bookings.last-minute.whatsapp-enabled`, `.whatsapp-max` | coupé, 50 | À ouvrir quand `offre_derniere_minute` est approuvé par Meta |

## Actions de l'exploitant

1. **Droits** : `BOOKING:PRICE_ADJUST` et `BOOKING:PRICE_APPROVE` sont donnés à `SUPER_ADMIN`, `ADMIN`, `MANAGER` ; `SYSTEM:REFERENCE_LISTS` à `SUPER_ADMIN` et `ADMIN`. Ajuster dans l'écran des rôles si besoin.
2. **Circuit d'approbation** `BOOKING_PRICE_ADJUSTMENT` (N+1) : vérifier la hiérarchie (organigramme) pour que chaque agent ait un responsable.
3. **Listes de l'administrateur** à relire : motifs d'annulation et de refus, motifs d'ajustement, dispositions de salle, étiquettes (`/reference-lists?module=BOOKING`).
4. **Modèles WhatsApp** écrits dans le logiciel, **non soumis** à Meta (soumission seulement avec l'accord de l'exploitant, par `POST /sni/api/v1/admin/whatsapp/templates/defaults`) :
   - `rappel_reservation` (UTILITY) : prénom, espace, date, horaire, référence ; bouton « Je viens ou j'annule » vers la réservation au portail ;
   - `offre_derniere_minute` (MARKETING) : prénom, espace, date, horaire, prix remisé, prix normal ; bouton « Réserver ». Envoyé seulement aux membres qui ont accepté la préférence `LAST_MINUTE_OFFERS` (WhatsApp), coupée par défaut.
   Tant qu'ils ne sont pas approuvés et que les réglages `*.whatsapp-enabled` restent coupés, rappels et offres partent par courriel ou au portail seulement.
5. **Rattrapage KYC** (reste du chantier ressources) : avec un compte d'administration, `POST /sni/api/v1/kyc/cases/backfill` (compte sans rien créer), puis `POST /sni/api/v1/kyc/cases/backfill?apply=true` dans chaque environnement.

## Tests

Une classe de tests par lot (`BookingB0Test` à `BookingB9gTest`, `ReferenceListsL0Test`) ; suite unitaire complète verte à chaque livraison (dernier passage : voir le commit du lot B9g). Les tests d'intégration (Docker) restent à lancer avant la production.

## Doc frontend

[booking](../../final-frontend-doc/booking/README.md) (une section par lot, en fin de page), [portail](../../final-frontend-doc/portal/parcours-reservations.md), [listes de choix](../../final-frontend-doc/listes-de-choix/README.md), [approbation](../../final-frontend-doc/approval/README.md) (centre `BOOKING`), [paramètres](../../final-frontend-doc/parametres/cles.md) (clés `bookings.*`) ; routes régénérées depuis le code (`booking/routes.md`, `portal/routes.md`).
