# Visibilité d'une ressource par client

> Fiche du module [ressource](../README.md), d'après le lot B10 du [plan booking](../../../backend/booking/README.md) (18/11/2026, décision D6). Routes exactes et champs JSON : [routes.md](../routes.md).

## Trois modes

| Mode | Au portail | Réservation au portail |
| --- | --- | --- |
| `PUBLIC` (défaut) | Visible de tous | Directe pour tous |
| `RESTRICTED` | Visible seulement des clients désignés ; introuvable pour les autres (`404` sur la liste, la fiche, le calendrier, les créneaux et le devis) | Directe pour les clients désignés |
| `ON_REQUEST` | Visible de tous | Directe pour les clients désignés ; **demande** pour les autres : la réservation naît en `PENDING_APPROVAL`, l'équipe approuve ou refuse |

Le calendrier public sans jeton ne montre jamais une ressource `RESTRICTED`. Le back-office voit et réserve toutes les ressources, quel que soit le mode.

## Clients désignés

Une audience est une liste de `{ type, code }` :

| `type` | `code` | Qui est visé |
| --- | --- | --- |
| `MEMBER` | code du membre (`MBR-…`) | Ce membre |
| `CUSTOMER` | code du client ou de l'organisation | Ce client ; pour une organisation, **aussi tous les membres qui y sont rattachés** (liens actifs) |
| `BUSINESS_ENTITY` | code de l'entreprise | Cette entreprise |
| `SEGMENT` | `PUBLIC`, `MEMBER`, `SUBSCRIBER`, `COMPANY`, `PARTNER` | Tout client de ce segment (par exemple tous les abonnés) |
| `PLAN` | code d'un plan d'abonnement | Les abonnés de ce plan |

## Routes (équipe)

- `GET /resources/{code}/visibility` rend `{ resourceCode, mode, audiences: [{ type, code }] }`.
- `PUT /resources/{code}/visibility` `{ mode, audiences }` remplace le mode et l'audience. `RESTRICTED` exige au moins un client désigné (`400`). Un type ou un segment inconnu est refusé (`400`).
- La réponse d'une ressource rend `visibilityMode`.

## Portail

- La liste `GET /client/resources` n'inclut une ressource `RESTRICTED` que pour son audience.
- La fiche `GET /client/resources/{code}` rend `bookingOnRequest: true` quand la réservation sera une demande : afficher « Sur demande » et prévenir que l'équipe confirmera.

## Écrans à prévoir

Fiche ressource, onglet « Visibilité » :

- choix du mode ;
- liste des clients désignés, avec recherche des membres, clients et entreprises, choix d'un segment ou d'un plan ;
- rappel de l'effet de chaque mode.
