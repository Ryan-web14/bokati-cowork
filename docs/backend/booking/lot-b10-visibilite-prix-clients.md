# Lot B10 : visibilité des ressources par client, prix au cas par cas

> Partie du [plan booking](README.md). Fait le 18/11/2026 sur `feature/booking`, avancé juste après B0 à la demande de l'utilisateur. Migration V382 (prod V376).

## Visibilité (décision D6)

- `Resource.visibilityMode` : `PUBLIC` (défaut), `RESTRICTED`, `ON_REQUEST` ; table `resource_audience` (type, code).
- `ResourceVisibilityService` : `access(ressource, client)` rend `DIRECT`, `ON_REQUEST` ou `HIDDEN`.
- Routes : `GET|PUT /resources/{code}/visibility`.
- Portail :
  - liste filtrée (spécification de recherche, identifiants des ressources réservées visibles) ;
  - fiche, calendrier, créneaux et devis introuvables si `HIDDEN` ;
  - `bookingOnRequest` sur la fiche.
- Calendrier public : une ressource `RESTRICTED` y est introuvable.
- Réservation au portail sur une ressource sur demande par un client non désigné : `PENDING_APPROVAL` (`approvalRequired`).

## Identités du client

`PriceContext` porte désormais `owners` (« TYPE:CODE ») : le titulaire, et pour un membre les organisations de ses liens actifs (`CUSTOMER:…`). `audienceKeys()` y ajoute les segments et le plan. Le même contexte sert à la visibilité et au prix.

## Prix au cas par cas

- `ResourcePricingRule.audience` (« TYPE:CODE » séparés par des virgules) ; requêtes et réponses en liste `{ type, code }` ; `clear: ["audience"]`.
- Le moteur n'applique une règle avec audience qu'à ses clients. Elle compte trois conditions de plus, donc l'emporte sur la grille générale à priorité égale.
- Les doublons et les versions de règles tiennent compte de l'audience.
- Le portail masque les règles réservées à d'autres clients.

## Tests

`ResourceB10Test` :

- prix d'un client et de ses membres rattachés, précédence ;
- segment et plan ;
- validation des audiences ;
- accès selon le mode ;
- ressource réservée sans audience refusée.
