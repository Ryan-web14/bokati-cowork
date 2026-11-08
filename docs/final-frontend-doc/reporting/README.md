# reporting : fonctionnement

> Rédigé d'après le code. Routes exactes, droits et champs JSON : [routes.md](routes.md) et `routes/`. Règles communes : [README racine](../README.md).

## En bref

Le module produit des rapports de lecture seule pour le back-office : tableau de bord financier, encaissements, balance âgée, caisses, flux de trésorerie, rapport d'activité, recouvrement des impayés, occupation, utilisation des ressources, valorisation du stock, abonnements et fiche complète d'un membre. Chaque rapport existe en JSON (pour l'écran) et, selon le cas, en PDF et en CSV (pour le téléchargement).

Il ne crée ni ne modifie rien : il calcule à la demande, en SQL natif, sur les tables de `billing`, `payment` (transactions, caisses, portefeuilles), `booking`, `resource`, `subscription`, `contract`, `client/member`, `support` et `inventory`. Aucun cache, aucun worker : chaque appel relit la base.

Toutes les routes exigent `REPORT:VIEW` (back-office). Les indicateurs synthétiques et le temps réel sont dans le module voisin [analytics](../analytics/README.md), sous le même droit.

## Notions

| Rapport | Contrôleur | Période | Formats | Ce qu'il mesure |
| --- | --- | --- | --- | --- |
| Tableau de bord financier | `FinancialReportController.dashboard` | `from`/`to` | JSON, PDF | factures (montants, taux d'encaissement, retards, TVA, centimes additionnels, remises), revenus par origine de facture, résumé des paiements |
| Encaissements par moyen | `FinancialReportController.payments` | `from`/`to` | JSON | paiements réussis par moyen, part en %, série journalière |
| Balance âgée | `FinancialReportController.aging` | aucune (état actuel) | JSON | encours des factures par tranche de retard |
| Caisses | `FinancialReportController.cashRegisters` | `from`/`to` | JSON | par caisse active : sessions, fonds d'ouverture, entrées, sorties, écarts |
| Flux de trésorerie | `FinancialReportController.cashFlow` | `from`/`to` | JSON | mouvements de caisse par type et série journalière |
| Activité | `ActivityReportController` | `from`/`to`, comparaison | JSON, PDF, CSV | réservations, factures, paiements, abonnements, stock, contrats, membres, tickets |
| Recouvrement | `DebtRecoveryReportController` | aucune (état actuel) | JSON, PDF, CSV | liste des factures impayées avec coordonnées du client, et tranches de retard |
| Occupation | `OccupancyReportController` | `from`/`to` | JSON, PDF | minutes réservées contre minutes disponibles, par type et par ressource |
| Utilisation des ressources | `ResourceUtilizationReportController` | `from`/`to` | JSON, PDF | par ressource réservable : réservations matin/après-midi/soir, taux, heures de pointe, ressources sous-utilisées |
| Valorisation du stock | `StockValuationReportController` | `inactiveDays` | JSON, PDF | valeur du stock par catégorie et par emplacement, articles sous le seuil, stock dormant |
| Abonnements | `SubscriptionReportController` | `months`, `renewalDays` | JSON, PDF | courbe du revenu mensuel récurrent, répartition par plan, attrition, renouvellements à venir |
| Fiche membre | `MemberProfileReportController` | aucune | JSON, PDF | identité, abonnements, réservations, factures, paiements, portefeuille, contrats, tickets d'un membre |

Identifiants : tout est exposé par **codes lisibles** (numéro de facture `documentNumber`, code client, code de ressource, `subscriptionNumber`, `walletNumber`, `ticketNumber`, `contractCode`, code d'article). Aucun identifiant numérique n'est renvoyé. Le seul paramètre de chemin, `{memberId}` de la fiche membre, est le **code du membre** (colonne `member.member_id`, celui que le module `client/member` affiche), pas un id numérique (`src/main/java/com/sni/bokaticowork/features/reporting/repository/MemberProfileReportRepository.java:26`).

Chaque réponse JSON porte `generatedAt` (instant du calcul). Les rapports à période renvoient aussi `fromDate` et `toDate` tels que reçus (`null` si absents).

### Chemins en double

Chaque route existe sous deux ou trois préfixes qui appellent exactement la même méthode :

- `/sni/api/v1/reports/<rapport>` et `/sni/api/v1/reporting/<rapport>` pour activité, recouvrement, occupation, utilisation, stock, abonnements, fiche membre ;
- pour la finance, trois préfixes : `/sni/api/v1/reports/finance/...`, `/sni/api/v1/reporting/finance/...` et `/sni/api/v1/reporting/...` (par exemple `/sni/api/v1/reporting/dashboard`).

Choisir une seule forme dans le frontend. Proposition : `/sni/api/v1/reporting/<rapport>` et `/sni/api/v1/reporting/finance/<rapport>`. Les 50 routes se réduisent ainsi à 20 appels distincts.

## Statuts et cycle de vie

Le module n'a aucun objet à statut. Il lit les statuts des autres modules ; ceux qui entrent dans chaque calcul sont utiles pour libeller les écrans :

| Calcul | Ce qui est compté | Source |
| --- | --- | --- |
| Factures émises (dashboard, activité) | `document_type = INVOICE`, statut hors `DRAFT`, `CANCELLED`, `VOIDED` | `FinancialReportRepository.java:20`, `ActivityReportRepository.java:43` |
| Montant payé (dashboard) | factures hors `CANCELLED`, `VOIDED` (les brouillons inclus) | `FinancialReportRepository.java:22` |
| Encours, balance âgée, recouvrement | factures hors `DRAFT`, `CANCELLED`, `VOIDED`, `PAID` avec `balance_due > 0` | `FinancialReportRepository.java:126`, `DebtRecoveryReportRepository.java:47` |
| En retard | statut `OVERDUE` (posé par le module billing, pas recalculé ici) | `FinancialReportRepository.java:24` |
| Paiements reçus | transactions `SUCCEEDED`, datées par `paid_at` | `FinancialReportRepository.java:59` |
| Occupation | réservations `CONFIRMED`, `IN_PROGRESS`, `COMPLETED`, datées par `started_at` | `OccupancyReportRepository.java:24` |
| Utilisation des ressources | réservations hors `CANCELLED`, `NO_SHOW` (donc brouillons, attentes, refus et expirées compris), datées par `started_at` | `ResourceUtilizationReportRepository.java:34` |
| Activité, réservations | toutes les réservations non supprimées créées dans la période (`created_at`) | `ActivityReportRepository.java:17` |
| Abonnements actifs, revenu par plan | abonnements au statut `ACTIVE` aujourd'hui | `SubscriptionReportRepository.java:48` |

Tranches de retard (balance âgée, recouvrement) : `CURRENT` (pas d'échéance ou échéance future ou du jour), `1_30`, `31_60`, `61_90`, `90_PLUS`, calculées sur `CURRENT_DATE - due_date` de la base. Les libellés français (« Non échu », « + 90 jours »…) arrivent dans le champ `label` de chaque tranche : les afficher tels quels.

## Parcours

### Choisir la période

1. Les rapports à période acceptent `fromDate`/`toDate` **ou** `from`/`to`, au format `AAAA-MM-JJ` (date seule, pas d'heure). Si les deux noms sont envoyés, `fromDate`/`toDate` l'emporte (`src/main/java/com/sni/bokaticowork/features/reporting/controller/ActivityReportController.java:72`). Envoyer toujours `fromDate` et `toDate`.
2. Les deux bornes sont **incluses** : `fromDate` va de 00:00 à `toDate` 23:59:59.999. Le jour est découpé en **UTC** (`date.atStartOfDay().toInstant(ZoneOffset.UTC)`), sauf pour l'occupation qui compare en heure locale de la base (`LocalDateTime`).
3. Sans borne, la borne correspondante est ouverte : sans aucune date, le rapport porte sur tout l'historique.
4. Proposer des raccourcis (mois en cours, mois précédent, 30 derniers jours) et envoyer toujours les deux dates : l'occupation et l'utilisation supposent 30 jours quand une borne manque (voir [Règles](#règles-et-erreurs-à-refléter-dans-lécran)).

### Tableau de bord financier

1. `GET /sni/api/v1/reporting/finance/dashboard?fromDate=2026-10-01&toDate=2026-10-31` ([détail](routes/FinancialReportController.md#r-get-sni-api-v1-reporting-finance-dashboard)).
2. Cartes à afficher : nombre de factures, montant facturé, montant payé, encours, retards (nombre et montant), `collectionRate` (payé / facturé en %, 2 décimales, `0` si rien n'est facturé), TVA, centimes additionnels, remises, factures annulées.
3. `revenueBySource` : une ligne par origine de facture (`source_type` du module billing ; `MANUAL` quand l'origine est vide), triée par montant facturé décroissant. Graphique en barres.
4. `payments` : nombre de transactions de la période (tous statuts), total reçu et ventilation par moyen (espèces, portefeuille, mobile money, virement, carte, chèque), nombre d'échecs et nombre en attente (`PENDING` ou `PROCESSING`).
5. Bouton « PDF » : `GET /sni/api/v1/reporting/finance/dashboard/pdf` avec les mêmes dates, fichier `rapport-financier.pdf`.

Les factures sont rattachées à la période par leur **date de création** (`created_at`), pas par leur date d'émission. Les paiements le sont par `paid_at`.

### Encaissements par moyen

1. `GET /sni/api/v1/reporting/finance/payments?fromDate=...&toDate=...`.
2. `totalReceived`, `totalTransactions` ; `byMethod` (moyen, nombre, montant, part en %) pour un graphique circulaire ; `dailySeries` (un point par jour **qui a eu au moins un paiement**, avec une colonne par moyen et `total`) pour un graphique empilé. Les jours sans paiement sont absents : les combler à zéro côté écran.
3. `byMethod` peut contenir `MANUAL_ADJUSTMENT` ou `CREDIT_NOTE` (valeurs de `PaymentMethod`) ; `dailySeries` n'a pas de colonne pour eux, mais ils entrent dans `total`.

### Balance âgée et recouvrement

1. Synthèse : `GET /sni/api/v1/reporting/finance/aging` (sans paramètre). Renvoie `totalOutstanding`, `totalInvoices` et cinq tranches `current`, `days1to30`, `days31to60`, `days61to90`, `over90` (`label`, `count`, `amount`, `percentage`).
2. Liste à relancer : `GET /sni/api/v1/reporting/debt-recovery?sortBy=age` ([détail](routes/DebtRecoveryReportController.md#r-get-sni-api-v1-reporting-debt-recovery)). `sortBy` vaut `age` (défaut, le plus ancien retard d'abord), `amount` (solde dû décroissant) ou `customer` (nom du client puis solde). Toute autre valeur retombe sur `age`.
3. `aging` reprend les cinq tranches (avec en plus `averageAmount`). Chaque ligne `unpaidInvoices` donne le numéro de facture, le client (nom, code, type, courriel, téléphone), les dates d'émission et d'échéance, total, payé, `balanceDue`, `agingDays` (0 si pas encore échu) et `agingBucket`. Proposer un lien vers la facture (module billing, par `documentNumber`) et vers la fiche client (par `customerCode` et `customerType`).
4. Le total et les tranches portent sur **toutes** les factures impayées : la liste n'est pas paginée. Prévoir un filtre et une pagination côté écran si le volume est grand.
5. Exports : `GET /sni/api/v1/reporting/debt-recovery/pdf?sortBy=...` (`rapport-recouvrement.pdf`) et `GET /sni/api/v1/reporting/debt-recovery/csv?sortBy=...` (`rapport-recouvrement.csv`, séparateur `;`, une ligne par facture).

### Caisses et trésorerie

1. `GET /sni/api/v1/reporting/finance/cash-registers?fromDate=...&toDate=...` : une ligne par caisse **active** (code, nom, nombre de sessions ouvertes dans la période, fonds d'ouverture cumulés, entrées, sorties, net, écarts de clôture cumulés, nombre de mouvements), triée par entrées décroissantes, plus les totaux. Une caisse sans session dans la période apparaît avec des zéros.
2. Entrées = mouvements `PAYMENT`, `CASH_IN`, `TRANSFER_IN` ; sorties = `REFUND`, `CASH_OUT`, `TRANSFER_OUT`, `SAFE_DEPOSIT`. Les sessions sont rattachées à la période par leur ouverture (`opened_at`).
3. `GET /sni/api/v1/reporting/finance/cash-flow?fromDate=...&toDate=...` : `byMovementType` (type de mouvement, sens `IN` ou `OUT`, nombre, montant), `totalInflows`, `totalOutflows`, `netCashFlow`, et `dailySeries` (jours ayant des mouvements seulement). Les mouvements sont datés par leur création (`created_at`).

### Rapport d'activité

1. `GET /sni/api/v1/reporting/activity?fromDate=...&toDate=...&compareWithPrevious=true` ([détail](routes/ActivityReportController.md#r-get-sni-api-v1-reporting-activity)).
2. Huit sections : `bookings`, `invoices`, `payments`, `subscriptions`, `stock`, `contracts`, `members`, `support`. Une carte par section.
3. Avec `compareWithPrevious=true` **et** les deux dates, le serveur calcule la période précédente de même longueur (du `fromDate - n jours` à la veille de `fromDate`) et remplit `variation` (`previousValue`, `currentValue`, `changePercent`) pour trois indicateurs seulement : revenu des réservations, montant facturé, montant reçu. Toutes les autres `variation` sont `null`. `changePercent` vaut `0` quand la période précédente est nulle : ne pas l'afficher comme « stable » dans ce cas, mais comme « sans référence ».
4. Certains chiffres ne dépendent **pas** de la période : `subscriptions.activeCount`, `contracts.activeCount` et `members.totalActive` sont l'état actuel. Les libeller « actuellement ».
5. Exports : `.../activity/pdf` (`rapport-activite.pdf`) et `.../activity/csv` (`rapport-activite.csv`, colonnes `Section;Indicateur;Valeur`, sans les variations).

### Occupation

1. `GET /sni/api/v1/reporting/occupancy?fromDate=...&toDate=...` ([détail](routes/OccupancyReportController.md#r-get-sni-api-v1-reporting-occupancy)).
2. Le taux = minutes réservées / (nombre de ressources x jours x 480 minutes), en %, 2 décimales. La journée ouvrée est fixée à **8 heures** dans le code (`src/main/java/com/sni/bokaticowork/features/reporting/service/implementation/OccupancyReportServiceImpl.java:25`), indépendamment des horaires d'ouverture configurés. Un taux supérieur à 100 % est possible (ressource réservée plus de 8 h par jour) : prévoir l'affichage.
3. `overallOccupancyRate` est le taux global. `byResourceType` ne compte que les types actifs ; `byResource` liste toutes les ressources non supprimées, triées par minutes réservées.
4. PDF : `.../occupancy/pdf` (`rapport-occupation.pdf`). Pas de CSV.

### Utilisation des ressources

1. `GET /sni/api/v1/reporting/resource-utilization?fromDate=...&toDate=...`.
2. `resources` : une ligne par ressource active, non supprimée et ouverte à la réservation, avec réservations du matin (6 h à 12 h), de l'après-midi (12 h à 18 h), du soir (18 h à 22 h), minutes réservées, minutes disponibles (jours x 480), taux, revenu.
3. `hourlyHeatmap` : une entrée par heure de début (0 à 23) **ayant au moins une réservation**, avec sa part du total. Compléter les heures manquantes à zéro pour une carte de chaleur.
4. `underutilized` : ressources dont le taux est inférieur à **20 %** (seuil fixe, `ResourceUtilizationReportServiceImpl.java:25`).
5. PDF : `.../resource-utilization/pdf` (`rapport-utilisation-ressources.pdf`).

Les heures sont celles de `started_at` tel que stocké en base, sans conversion de fuseau (non vérifié : fuseau de la session PostgreSQL).

### Valorisation du stock

1. `GET /sni/api/v1/reporting/stock-valuation?inactiveDays=30`.
2. Valeur = quantité en stock x coût moyen, sur les articles actifs et les niveaux de stock positifs. `byCategory` (une catégorie `null` regroupe les articles sans catégorie), `byLocation` (emplacements actifs), `lowStockItems` (articles dont la quantité disponible totale est au plus le minimum de leur règle de réapprovisionnement active), `deadStockItems` (stock sans mouvement depuis plus de `inactiveDays` jours).
3. Ce rapport est un état actuel : pas de période.
4. PDF : `.../stock-valuation/pdf?inactiveDays=...`. Pas de CSV.

### Abonnements

1. `GET /sni/api/v1/reporting/subscriptions?months=12&renewalDays=30`.
2. `mrrTrend` : un point par mois sur les `months` derniers mois (mois en cours inclus), somme des `total_amount` des abonnements **mensuels** au statut `ACTIVE` qui couvraient ce mois. `currentMrr` = dernier point.
3. `activeByPlan` et `revenueByPlan` : abonnements `ACTIVE` regroupés par nom de plan (même calcul, tri différent : par nombre ou par montant).
4. `churnTrend` : par mois, abonnements résiliés dans le mois (`cancelled_at`), total des abonnements présents, taux.
5. `upcomingRenewals` : abonnements `ACTIVE` en renouvellement automatique dont la fin de période tombe entre aujourd'hui et `renewalDays` jours, avec `subscriberCode` et `subscriberType` pour lier vers le membre ou le client.
6. PDF : `.../subscriptions/pdf`. Pas de CSV.

### Fiche membre

1. Depuis la fiche d'un membre : `GET /sni/api/v1/reporting/member-profile/{memberId}` avec le **code** du membre ([détail](routes/MemberProfileReportController.md#r-get-sni-api-v1-reporting-member-profile-memberid)).
2. Sections : `member` (identité, statut, accès portail, client rattaché, profil), `subscriptions` (tous), `bookings`, `invoices`, `payments` (les **20** plus récents chacun), `wallet` (`null` si le membre n'a pas de portefeuille à son nom), `contracts`, `tickets` (tous).
3. Seuls les objets **au nom du membre** sont listés (`owner_type`, `customer_type` ou `subscriber_type` = `MEMBER`). Les factures, réservations ou abonnements portés par l'entreprise du membre n'y figurent pas.
4. PDF : `.../member-profile/{memberId}/pdf`, fichier `fiche-membre-<code>.pdf`.

### Télécharger un PDF ou un CSV

1. Appeler la route avec l'en-tête `Authorization` (un simple lien `<a href>` n'enverrait pas le jeton) et lire la réponse en `blob`.
2. Nom du fichier : en-tête `Content-Disposition` (exposé par la configuration CORS, `src/main/java/com/sni/bokaticowork/security/config/SecurityConfig.java:158`).
3. Le PDF est généré à la volée (Thymeleaf puis OpenHtmlToPDF, gabarits `src/main/resources/templates/reporting/`). Sur une longue période il peut prendre plusieurs secondes : afficher un indicateur d'attente et désactiver le bouton pendant l'appel.

## Règles et erreurs à refléter dans l'écran

- **Date mal formée** : `LocalDate.parse` lève une exception que le gestionnaire global ne traite pas, d'où un `500 INTERNAL_SERVER_ERROR` avec « An unexpected error occurred... » au lieu d'un `400` (`src/main/java/com/sni/bokaticowork/features/reporting/controller/ActivityReportController.java:77`, idem dans les autres contrôleurs). Valider le format `AAAA-MM-JJ` côté écran avant l'appel.
- **Paramètre entier invalide** (`months=abc`, `inactiveDays=x`) : même résultat, `500` (aucun gestionnaire de `MethodArgumentTypeMismatchException` dans `src/main/java/com/sni/bokaticowork/core/exception/GlobalExceptionHandler.java`). Utiliser des champs numériques.
- **Code membre inconnu** sur la fiche membre : `getSingleResult` lève `NoResultException`, non traitée, d'où `500` et non `404` (`src/main/java/com/sni/bokaticowork/features/reporting/repository/MemberProfileReportRepository.java:29`). N'ouvrir la fiche qu'à partir d'un membre existant, et traiter un `500` sur cette route comme « membre introuvable ».
- **`fromDate` après `toDate`** : aucune vérification. Le rapport revient vide et, pour l'occupation et l'utilisation, `periodDays` devient nul ou négatif (taux à `0` ou négatifs). Empêcher la saisie côté écran.
- **Une seule borne** : occupation et utilisation calculent la capacité sur 30 jours alors que les réservations sont prises sur toute la période ouverte (`OccupancyReportServiceImpl.java:41`, `ResourceUtilizationReportServiceImpl.java:35`). Toujours envoyer les deux dates pour ces deux rapports.
- **`months`, `renewalDays`, `inactiveDays`** : aucune borne côté serveur. `months` à `0` ou négatif donne des séries vides et un `currentMrr` à `0`. Proposer des valeurs fixes (3, 6, 12, 24 mois ; 7, 30, 60, 90 jours).
- **Refus d'accès** : `403` avec `requiredPermission: "REPORT:VIEW"`.
- Les montants sont des nombres décimaux sans devise : la devise n'est renvoyée que sur les lignes de la fiche membre, des renouvellements et du portefeuille. Afficher la devise de l'application pour les totaux (non vérifié : aucun rapport agrégé ne mélange les devises, les sommes additionnent les montants quelle que soit leur devise).

## À prévoir côté frontend

- **Écrans** : un menu « Rapports » avec un onglet par rapport ; un sélecteur de période commun ; un bouton « Comparer à la période précédente » pour l'activité seulement ; boutons PDF (8 rapports) et CSV (activité, recouvrement) ; un lien « Fiche complète » depuis la fiche membre.
- **Droit** : masquer le menu entier sans `REPORT:VIEW`. Le droit `REPORT:EXPORT` existe mais **n'est exigé par aucune** de ces routes (voir [Écarts](#écarts-relevés)) : ne pas l'utiliser pour masquer les boutons PDF et CSV.
- **Listes de référence** : aucune. Les libellés de tranches arrivent dans les réponses ; les codes de moyens de paiement (`CASH`, `WALLET`, `MOBILE_MONEY`, `BANK_TRANSFER`, `CARD`, `CHEQUE`, `MANUAL_ADJUSTMENT`, `CREDIT_NOTE`) et de mouvements de caisse (`PAYMENT`, `REFUND`, `CASH_IN`, `CASH_OUT`, `SAFE_DEPOSIT`, `TRANSFER_IN`, `TRANSFER_OUT`, `ADJUSTMENT`) sont à traduire côté écran.
- **Séries** : les séries journalières et horaires omettent les jours ou heures sans donnée ; les séries mensuelles des abonnements, elles, ont un point par mois.
- **CSV** : séparateur `;`, encodage UTF-8 **sans BOM**. Ouvert tel quel dans Excel, les accents des en-têtes peuvent s'afficher mal ; le signaler ou proposer le PDF.
- **Pas de temps réel ni de pagination** : recharger à la demande (bouton « Actualiser »), éventuellement garder la réponse en mémoire le temps de la navigation entre onglets.
- **Pas d'appel à répéter automatiquement** : chaque appel relance toutes les requêtes ; éviter de recalculer à chaque frappe dans le sélecteur de dates.

## Effets automatiques

Aucun. Le module n'a ni worker planifié, ni écouteur d'événement, ni courriel, ni notification. Les chiffres reflètent l'état de la base au moment de l'appel ; le statut `OVERDUE` des factures et `EXPIRED` des contrats sont posés par les workers des modules billing et contract.

## Écarts relevés

1. **Entrées de stock comptées en sorties** dans le rapport d'activité : la liste des types entrants est `RECEIPT`, `PURCHASE_RECEIPT`, `RETURN_IN`, `ADJUSTMENT_IN`, `TRANSFER_IN`, `PRODUCTION_OUTPUT`, alors que `StockMovementType` vaut `IN`, `OUT`, `TRANSFER`, `ADJUSTMENT_IN`, `ADJUSTMENT_OUT`. Tout mouvement `IN` (et `TRANSFER`) tombe dans `outboundMovements` (`src/main/java/com/sni/bokaticowork/features/reporting/mapper/decorator/ActivityReportMapperDecorator.java:23`).
2. **Ressource sans réservation comptée une fois** : `count(*)` sur une jointure externe renvoie `1` au lieu de `0` pour `totalBookings` d'une ressource sans réservation dans la période (les compteurs matin, après-midi, soir et les minutes restent justes) (`src/main/java/com/sni/bokaticowork/features/reporting/repository/ResourceUtilizationReportRepository.java:24`). Afficher `totalBookings` avec prudence quand `bookedMinutes` vaut `0`.
3. **Flux de trésorerie incohérent** : `byMovementType` classe `ADJUSTMENT` en sortie (`ELSE 'OUT'`, `src/main/java/com/sni/bokaticowork/features/reporting/repository/FinancialReportRepository.java:166`) et l'inclut dans `totalOutflows`, alors que la série journalière l'exclut (`FinancialReportRepository.java:190`). La somme des jours ne retombe pas sur le total quand il y a des ajustements.
4. **Droit d'export jamais exigé** : la règle d'URL ne demande `REPORT:EXPORT` que pour un chemin finissant par `.csv` ou contenant `/export` ; les routes réelles finissent par `/csv` et `/pdf`, donc tout porteur de `REPORT:VIEW` exporte (`src/main/java/com/sni/bokaticowork/security/authorization/AdminApiAuthorizationManager.java:192`).
5. **Erreurs d'entrée en `500`** : dates mal formées, entiers invalides et code membre inconnu ne sont pas traduits en `400` ou `404` (voir [Règles](#règles-et-erreurs-à-refléter-dans-lécran)).
6. **Capacité supposée sur 30 jours** sans date, et journée fixe de 8 heures, sans lien avec les horaires d'ouverture (`OccupancyReportServiceImpl.java:25` et `:41`, `ResourceUtilizationReportServiceImpl.java:24` et `:35`).
7. **Deux définitions de l'occupation** : le rapport d'occupation ne compte que `CONFIRMED`, `IN_PROGRESS`, `COMPLETED`, celui d'utilisation compte tout sauf `CANCELLED` et `NO_SHOW` (brouillons, refus, expirées compris, `ResourceUtilizationReportRepository.java:34`). Les deux écrans afficheront des taux différents pour la même période.
8. **Encours incluant les pertes** : recouvrement et balance âgée excluent seulement `DRAFT`, `CANCELLED`, `VOIDED`, `PAID` ; une facture `WRITTEN_OFF` ou `REFUNDED` avec un solde reste à relancer (`DebtRecoveryReportRepository.java:47` et `:69`), alors que le module analytics les exclut de l'encours.
9. **Jour découpé en UTC** et non dans le fuseau de l'application (`ActivityReportServiceImpl.java:116`, idem finance et utilisation), alors que l'occupation compare en `LocalDateTime` : une réservation ou un paiement proche de minuit peut changer de jour d'un rapport à l'autre.
10. **Courbe du revenu récurrent** : chaque mois passé est calculé avec le statut **actuel** des abonnements (`s.status = 'ACTIVE'`, `SubscriptionReportRepository.java:21`) ; un abonnement expiré depuis disparaît rétroactivement des mois où il était actif.
11. **Priorité `CRITICAL`** inexistante dans `TicketPriority` (`LOW`, `MEDIUM`, `HIGH`, `URGENT`), sans effet (`ActivityReportRepository.java:150`).
12. **Montant payé du dashboard** inclut les factures en brouillon alors que le montant facturé les exclut (`FinancialReportRepository.java:21` et `:22`) : le taux d'encaissement peut dépasser 100 % en théorie.
