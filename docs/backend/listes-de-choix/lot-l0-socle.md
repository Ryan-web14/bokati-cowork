# Lot L0 : socle des listes de choix

> 19/11/2026, sur `feature/booking` (avant le lot B7 qui s'en sert). Migration V387 (prod V381). Doc frontend : [listes-de-choix](../../final-frontend-doc/listes-de-choix/README.md).

## Valeurs fixes

- `core/reference/service/ReferenceEnumRegistry` : registre unique, construit au démarrage à partir des `ReferenceEnumRegistry.Contributor` de chaque module (code, libellé français, ordre, sélectionnable). Une déclaration en double fait échouer le démarrage.
- `GET /reference/enums?module=&group=` l'expose ; `require(module, group, code)` contrôle un code reçu avec le même registre (`400 VALEUR_INCONNUE` avec les valeurs permises) ; `label(...)` rend le libellé.
- Premier module branché : réservations (`BookingReferenceEnums` : statut, mode et état de règlement, canal, rôle et état de participant, fréquence, pré-réservation, liste d'attente, type de ligne, type d'option). `PORTAL` et `TEAM` sont des canaux non sélectionnables (posés par le serveur).

## Listes de l'administrateur

- Promotion au socle des valeurs de référence de la caisse : table `cash_reference_value` renommée `reference_value` (données gardées, colonne `auto_added` en plus) ; nouvelle table `reference_list` (code, module, libellé, description, mode strict ou souple).
- `core/reference/service/ReferenceLists` : `lists`, `values`, `add`, `update`, `replace` (désactive ce qui manque, ne supprime jamais), `updateList`, et pour les services `require`, `describe` (libellé plus précision, emploi compté), `describeReason` (motif `reasonCode` plus `reasonText`, ancien texte libre accepté), `label`.
- Liste souple : une valeur inconnue est ajoutée, marquée `autoAdded`, comptée dans `autoAddedCount` pour être relue.
- Routes : `GET /reference-lists?module=`, `GET /reference-lists/{code}`, `POST .../values`, `PATCH .../values/{code}`, `PUT /reference-lists/{code}`, `PATCH /reference-lists/{code}`.
- Caisse : `CashReferenceLists` délègue au socle ; `CashReferenceValue` et son dépôt sont retirés ; `/cash-registers/reference-lists` inchangé. Les messages d'erreur des motifs de caisse suivent ceux du socle (« Valeur inconnue « X » dans la liste « Motifs de suspension » »).

## Recherches de choix

- Convention : `GET /{module}/lookup/{type}?q=&page=&size=&includeInactive=`, réponse `PaginatedResponse<LookupItem>` (`type`, `code`, `label`, `hint`, `active`), 50 éléments au plus par page.
- Livré : `GET /lookup/users` (utilisateurs de l'équipe, par la fonction de recherche `search_admin_user`). La recherche unique du titulaire d'une réservation (membres, clients, entreprises) arrive avec le lot B7 sur la même convention.

## Droits

- Lecture (`/reference/**`, `/reference-lists/**`, `/lookup/**` en `GET`) : toute l'équipe (rôle du back-office), sans droit particulier. Règle `ANY_STAFF` de `AdminApiAuthorizationManager`.
- Modification : `SYSTEM:REFERENCE_LISTS` (nouveau droit, `SUPER_ADMIN` et `ADMIN`).

## Acteur

`core/reference/service/SessionActor` : l'utilisateur de la session, sinon la valeur du traitement interne, sinon `SYSTEM`. À employer par les lots suivants à la place des champs `createdBy`, `changedBy`... des requêtes.

## Tests

`ReferenceListsL0Test` (liste stricte, souple, motifs, remplacement, registre) ; `AdminApiAuthorizationManagerTest` (lecture ouverte à l'équipe, modification sous droit) ; tests de la caisse adaptés.
