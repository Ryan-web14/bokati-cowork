# Lot R0 : sécurité et accès

> Partie du [plan ressources](README.md). Fait le 18/11/2026 sur `feature/ressources`.

## Calendriers

- Calendrier public (`/public/resources/{code}/calendar`, sans jeton) et calendrier du portail : seulement une ressource visible au portail, active, réservable et au statut `ACTIVE` (même règle que la liste du portail), sinon `404` comme une ressource inconnue : plus d'énumération des ressources cachées.
- Plage : `ResourceCalendarRange`, défaut d'aujourd'hui à un mois, 92 jours au plus (l'horizon de 90 jours tient dans une demande), fin avant le début refusée. Même borne pour le calendrier de l'équipe et ses créneaux occupés. Avant : boucle jour par jour sans limite sur une route publique.
- Jour sans créneau : `CLOSED` (fermé, hors horaires ou au-delà de l'horizon), plus `FULL`.

## Droits

- `POST /resources/search` et les `/search` des catalogues : `RESOURCE:READ` (avant : `RESOURCE:CREATE`, règle sur le verbe POST).
- Galerie : lecture en `RESOURCE:READ`, ajout, modification et retrait en `RESOURCE:GALLERY`.
- Horaires d'ouverture : lecture `RESOURCE:READ`, modification et retour aux horaires globaux `RESOURCE:UPDATE` ; le `@PreAuthorize` qui exigeait le rôle ADMIN est retiré.

## Photos

- `PATCH` et `DELETE /resources/{code}/photos/{id}` : la photo doit appartenir à la ressource du chemin (`404` sinon).
- Le document doit exister dans la GED et être une image.
- Une seule couverture : en poser une retire l'ancienne.

## Journal des actions

`@Audited` ajouté : créneaux et règles tarifaires en lot, ajout, modification et retrait de photo, horaires d'ouverture (modification et retour aux horaires globaux).

## Écarts réglés

8, 13, Rn-7, Rn-8, Rn-9 ; 2 en partie (droit ; filtres par codes au lot R4).

## Tests

`ResourceR0Test` : ressource cachée introuvable au calendrier public, plage bornée et ordonnée, jour sans créneau `CLOSED`, photo d'une autre ressource introuvable, photo qui n'est pas une image ou absente de la GED refusée, nouvelle couverture qui remplace l'ancienne, droits de la recherche, de la galerie et des horaires. `PublicResourceCalendarEveningTest` adapté (ressource `ACTIVE`).
