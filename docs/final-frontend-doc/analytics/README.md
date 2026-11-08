# analytics : fonctionnement

> Rédigé d'après le code. Routes exactes, droits et champs JSON : [routes.md](routes.md) et `routes/`. Règles communes : [README racine](../README.md).

## En bref

Le module fournit les indicateurs du tableau de bord d'accueil du back-office : une vue d'ensemble chiffrée sur une période, une comparaison avec la période précédente, un instantané « en ce moment » de l'occupation, une courbe des réservations et les classements des meilleurs clients et des ressources les plus réservées.

Lecture seule, calculée à chaque appel en SQL natif sur les tables de `billing`, `payment` (transactions et portefeuilles), `booking`, `resource`, `subscription`, `client` et `inventory`. Pas de cache, pas de worker, pas de temps réel poussé.

Toutes les routes exigent `REPORT:VIEW` (back-office), comme le module [reporting](../reporting/README.md), qui porte les rapports détaillés et les exports PDF et CSV.

## Notions

| Route | Contenu | Période |
| --- | --- | --- |
| `GET /sni/api/v1/analytics/overview` | sept blocs : `financial`, `payment`, `wallet`, `booking`, `subscription`, `customer`, `inventory` | `fromDate`, `toDate` facultatifs |
| `GET /sni/api/v1/analytics/comparison` | deux vues d'ensemble (`current`, `previous`) et quatre variations (`revenue`, `paid`, `bookings`, `activeSubscriptions`) | `fromDate` obligatoire, `toDate` facultatif (aujourd'hui par défaut) |
| `GET /sni/api/v1/analytics/live` | instantané : réservations en cours, ressources occupées, taux d'occupation, attentes, chiffres du jour | aucune (maintenant et aujourd'hui) |
| `GET /sni/api/v1/analytics/trends/bookings` | série de réservations par jour, semaine ou mois | `fromDate`, `toDate`, `groupBy` |
| `GET /sni/api/v1/analytics/top-owners` | classement des titulaires de réservations | `fromDate`, `toDate`, `limit` |
| `GET /sni/api/v1/analytics/top-resources` | classement des ressources | `fromDate`, `toDate`, `limit` |
| `GET /sni/api/v1/reports/financial/summary` | le seul bloc `financial` de la vue d'ensemble | `fromDate`, `toDate` |
| `GET /sni/api/v1/reports/attendance/summary` | le seul bloc `booking` de la vue d'ensemble | `fromDate`, `toDate` |

Identifiants : uniquement des codes lisibles (`ownerCode` et `ownerType` d'un titulaire, `resourceCode`, codes de type et de groupe de ressource). Aucun id numérique.

Ce que mesure chaque bloc de la vue d'ensemble (`src/main/java/com/sni/bokaticowork/features/analytics/repository/AnalyticsRepository.java`) :

| Bloc | Dépend de la période ? | Contenu |
| --- | --- | --- |
| `financial` | oui, par `created_at` du document | factures (`invoiceCount`, `invoicedAmount` hors brouillon, refus, expiré, converti ; `paidAmount` ; `outstandingAmount` hors brouillon, payé, remboursé, passé en perte ; `overdueInvoiceCount`), devis (`quotationCount`, `quotedAmount`), `vatAmount`, `additionalCentAmount`. Les documents `CANCELLED` et `VOIDED` sont exclus partout |
| `payment` | oui, par `created_at` de la transaction | `transactionCount` (tous statuts), `succeededAmount` et sa ventilation espèces, portefeuille, mobile money, virement, carte ; nombre d'échecs et d'attentes |
| `wallet` | en partie | `walletCount`, `availableBalance`, `heldBalance` : état actuel de tous les portefeuilles ; `credits`, `debits` : écritures de la période |
| `booking` | oui, par `created_at` de la réservation | nombre, confirmées, terminées, annulées, absences (`NO_SHOW`), revenu, minutes, participants |
| `subscription` | **non** | abonnements `ACTIVE`, `PENDING_ACTIVATION`, `SUSPENDED` ; pass `ACTIVE` ou `PARTIALLY_USED` ; revenu mensuel récurrent (abonnements actifs au cycle `MONTHLY`) ; quantité d'usage facturable cumulée |
| `customer` | en partie | `customerCount`, `memberCount`, `activeMembers` : état actuel ; `guestBookingCount` : réservations d'invités créées dans la période |
| `inventory` | **non** | articles actifs, valeur du stock, alertes ouvertes de stock bas ou de rupture, bons de commande ouverts (`APPROVED`, `ORDERED`, `PARTIALLY_RECEIVED`) |

## Statuts et cycle de vie

Le module n'a aucun objet à statut. Il compte les statuts des autres modules, listés dans le tableau ci-dessus et dans les parcours. Les réservations sont rattachées à une période par leur **date de création**, pas par leur date de début (sauf dans `live`, qui raisonne sur `started_at`).

## Parcours

### Tableau de bord d'accueil

1. Au chargement : `GET /sni/api/v1/analytics/live` ([détail](routes/AnalyticsController.md#r-get-sni-api-v1-analytics-live)) pour le bandeau « en ce moment ».
2. En parallèle : `GET /sni/api/v1/analytics/comparison?fromDate=2026-11-01&toDate=2026-11-08&compareWithPrevious=true` ([détail](routes/AnalyticsController.md#r-get-sni-api-v1-analytics-comparison)) pour les cartes chiffrées avec leur évolution.
3. Puis les graphiques et classements avec la même période : `.../analytics/trends/bookings`, `.../analytics/top-owners`, `.../analytics/top-resources`.
4. Un changement de période relance les appels 2 et 3 ; `live` se rafraîchit seul (voir plus bas).

### Bandeau « en ce moment »

1. `GET /sni/api/v1/analytics/live`, sans paramètre.
2. Instantané : `activeBookings` (réservations `CONFIRMED` ou `IN_PROGRESS` dont l'intervalle contient maintenant), `checkedInBookings` (réservations `IN_PROGRESS` avec arrivée enregistrée, sans condition d'heure), `occupiedResources`, `totalBookableResources` (ressources actives, non supprimées, ouvertes à la réservation), `availableResources` (différence, jamais négative), `occupancyRate` (en %, **une** décimale), `pendingApproval` (réservations `PENDING_APPROVAL`, toutes dates), `upcomingNextHour` (réservations `CONFIRMED` ou `PENDING_APPROVAL` qui commencent dans l'heure), `activeHolds` (blocages de créneau `ACTIVE` non expirés).
3. Aujourd'hui (jour de la base, `CURRENT_DATE`) : `bookingsToday` (toutes réservations qui commencent aujourd'hui, tous statuts), `revenueToday`, `avgBookingAmount` et `bookedMinutesToday` (réservations confirmées, en cours ou terminées seulement), `cancelledToday`, `noShowToday`, `checkInsToday`, `newMembersToday`.
4. Liens utiles : `pendingApproval` vers la liste des réservations à valider (module booking), `activeHolds` vers les blocages.
5. Rafraîchir par minuterie (par exemple toutes les 60 secondes, page visible seulement) : aucun sujet WebSocket ne pousse ces chiffres.

### Cartes avec évolution

1. `GET /sni/api/v1/analytics/comparison?fromDate=...&toDate=...&compareWithPrevious=true`.
2. La période précédente a la même longueur et se termine la veille de `fromDate` (du 1er au 8 novembre, elle va du 24 au 31 octobre, `src/main/java/com/sni/bokaticowork/features/analytics/controller/AnalyticsController.java:51`).
3. Afficher `current` (vue d'ensemble complète de la période) et, pour quatre cartes, la variation : `revenue` (montant facturé), `paid` (montant payé), `bookings` (nombre de réservations), `activeSubscriptions`. Chaque variation porte `current`, `previous`, `percentChange` (2 décimales).
4. `percentChange` vaut `0` quand la valeur précédente est nulle : afficher « sans référence » plutôt que « 0 % ».
5. Sans `compareWithPrevious=true` (défaut `false`), `previous` est une vue d'ensemble remplie de zéros avec `fromDate` et `toDate` à `null`, et tous les `percentChange` valent `0`. Pour une simple vue d'ensemble, appeler plutôt `/sni/api/v1/analytics/overview`.

### Vue d'ensemble sans comparaison

1. `GET /sni/api/v1/analytics/overview?fromDate=...&toDate=...` ([détail](routes/AnalyticsController.md#r-get-sni-api-v1-analytics-overview)). Les deux dates sont facultatives ; sans elles, tout l'historique.
2. Les blocs `subscription` et `inventory`, et une partie de `wallet` et `customer`, ne changent pas avec la période (tableau des [Notions](#notions)) : les libeller « actuellement ».
3. Résumés partiels : `GET /sni/api/v1/reports/financial/summary` renvoie le seul bloc `financial`, `GET /sni/api/v1/reports/attendance/summary` le seul bloc `booking`, avec les mêmes paramètres.

### Courbe des réservations

1. `GET /sni/api/v1/analytics/trends/bookings?fromDate=...&toDate=...&groupBy=day` ([détail](routes/AnalyticsController.md#r-get-sni-api-v1-analytics-trends-bookings)).
2. `groupBy` vaut `day` (défaut), `week` ou `month` (casse indifférente). La `date` de chaque point est le premier jour de l'intervalle (le lundi pour `week`, le 1er pour `month`).
3. Chaque point : `bookingCount`, `confirmedCount`, `cancelledCount`, `revenue`. Les réservations sont regroupées par date de **création**. Seuls les intervalles ayant au moins une réservation sont renvoyés : compléter à zéro côté écran.

### Classements

1. `GET /sni/api/v1/analytics/top-owners?fromDate=...&toDate=...&limit=10` : titulaires de réservations triés par nombre de réservations, puis revenu, puis code. `ownerType` (par exemple `MEMBER`, `CUSTOMER`, `GUEST` selon le module booking) et `ownerCode` permettent de lier vers la fiche ; `ownerName` est le nom de contact le plus grand alphabétiquement parmi ses réservations, ou le code à défaut.
2. `GET /sni/api/v1/analytics/top-resources?fromDate=...&toDate=...&limit=10` : ressources non supprimées, avec type et groupe, triées de même.
3. `limit` : 10 par défaut, ramené à 100 au maximum.
4. Tous les statuts de réservation sont comptés dans `bookingCount` et `revenue`, annulées comprises : afficher aussi `cancelledCount`.

## Règles et erreurs à refléter dans l'écran

- **`fromDate` manquant sur `/analytics/comparison`** : `400 MISSING_PARAMETER`. Il est obligatoire sur cette seule route.
- **`groupBy` hors `day`, `week`, `month`** : `400 BAD_REQUEST`, message `groupBy must be one of: day, week, month` (`src/main/java/com/sni/bokaticowork/features/analytics/service/AnalyticsServiceImpl.java:247`).
- **`limit` inférieur à 1** : `400 BAD_REQUEST`, message `limit must be greater than zero` (`AnalyticsServiceImpl.java:253`). Au-delà de 100, ramené à 100 sans erreur.
- **Date mal formée** (attendu `AAAA-MM-JJ`) : `LocalDate.parse` lève une exception non traitée, d'où `500 INTERNAL_SERVER_ERROR` (`AnalyticsController.java:142`). Valider le format avant l'appel.
- **`limit` non numérique** : `500` également (pas de gestionnaire de conversion dans `src/main/java/com/sni/bokaticowork/core/exception/GlobalExceptionHandler.java`).
- **`fromDate` après `toDate`** : aucune vérification, résultats vides ou à zéro. Empêcher la saisie.
- Bornes incluses ; les jours sont découpés en **UTC** (`AnalyticsController.java:147`, `AnalyticsServiceImpl.java:29`).
- Refus d'accès : `403` avec `requiredPermission: "REPORT:VIEW"`.

## À prévoir côté frontend

- **Écran** : tableau de bord d'accueil du back-office (bandeau live, cartes avec évolution, courbe, deux classements). Le masquer sans `REPORT:VIEW` et afficher à la place un accueil sans chiffres.
- **Sélecteur de période** commun aux cartes, à la courbe et aux classements ; choix de granularité `day`, `week`, `month` pour la courbe (proposer `month` au-delà de trois mois).
- **Montants** sans devise : afficher la devise de l'application. Les taux sont des pourcentages déjà multipliés par 100.
- **Libellés** : traduire les codes de statut et de type de titulaire côté écran ; distinguer « sur la période » et « actuellement » selon le tableau des [Notions](#notions).
- **Rafraîchissement** : minuterie sur `live` uniquement ; les autres appels à la demande. Pas de WebSocket pour ce module.
- **Pas de pagination** : les classements sont limités par `limit`, les autres réponses sont des objets fixes.

## Effets automatiques

Aucun. Ni worker, ni écouteur d'événement, ni courriel, ni notification. Les chiffres reflètent la base au moment de l'appel ; les statuts comptés (`OVERDUE`, `EXPIRED`, `NO_SHOW`, expiration des blocages) sont posés par les workers des modules billing, booking et subscription.

## Écarts relevés

1. **Variation des abonnements toujours nulle** : `activeSubscriptions` est un état actuel, sans période (`AnalyticsRepository.java:180`), donc identique pour `current` et `previous` ; la variation affichée vaut toujours `0 %` (`src/main/java/com/sni/bokaticowork/features/analytics/controller/AnalyticsController.java:64`).
2. **TVA et centimes additionnels de tous les documents** : `vatAmount` et `additionalCentAmount` additionnent factures, devis, proformas, avoirs et brouillons, sans filtre de type (`src/main/java/com/sni/bokaticowork/features/analytics/repository/AnalyticsRepository.java:33` et `:34`). Le montant ne correspond pas à la TVA facturée.
3. **Paiements datés par création** ici (`created_at`, `AnalyticsRepository.java:55`) et par encaissement dans reporting (`paid_at`) : le montant reçu d'une même période diffère entre le tableau d'accueil et le rapport financier. De même, l'encours exclut ici `REFUNDED` et `WRITTEN_OFF`, pas dans reporting.
4. **`paidAmount`** compte aussi les factures en brouillon, refusées, expirées ou converties, contrairement à `invoicedAmount` (`AnalyticsRepository.java:24`).
5. **Participants** : la sous-requête `participantCount` ne filtre pas les réservations supprimées (`AnalyticsRepository.java:84` à `:87`).
6. **Erreurs d'entrée en `500`** pour une date mal formée ou un `limit` non numérique, au lieu de `400`.
7. **Deux préfixes** pour un même module : `/sni/api/v1/analytics/...` et `/sni/api/v1/reports/.../summary`, ce dernier partageant le préfixe `/reports` du module reporting.
8. **`checkedInBookings`** ne vérifie pas que la réservation est en cours maintenant (`AnalyticsRepository.java:224` à `:226`) : une réservation restée `IN_PROGRESS` après sa fin y reste comptée.
