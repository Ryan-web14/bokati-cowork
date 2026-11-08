# inventory : fonctionnement

> Rédigé d'après le code. Routes exactes, droits et champs JSON : [routes.md](routes.md) et `routes/`. Règles communes : [README racine](../README.md).

## En bref

Le module gère tout ce qui est physique dans les espaces : le catalogue d'articles (consommables, équipements, pièces, services), les emplacements, le stock par article et par emplacement (entrées, sorties, transferts, ajustements, réservations, lots, numéros de série, quarantaine), la qualité (plans de contrôle, inspections, non-conformités, rappels), les achats (fournisseurs, demandes, commandes, réceptions), les inventaires physiques, la valorisation et les écritures comptables de stock, les équipements suivis à l'unité (prêts, réservations, maintenance), les alertes et les suggestions de réapprovisionnement.

Les 176 routes sont toutes réservées au **back-office** (droit `INVENTORY:*`). Aucune route n'est ouverte au portail membre ni au public. Le module [marketplace](../marketplace/routes.md) réserve et consomme du stock par les services internes (référence `POS_SALE`), le catalogue de services de [billing](../billing/routes.md) lit les articles, et chaque alerte de réapprovisionnement crée une tâche dans le module [task](../task/routes.md).

Le module étant volumineux, les parcours détaillés sont dans des fichiers séparés (voir [Parcours](#parcours)).

## Notions

Tous les objets s'adressent par un **code lisible** dans les chemins, sauf les exceptions signalées par ⚠ (identifiant numérique int64, voir [Identifiants](../README.md#identifiants--attention-aux-grands-nombres)).

| Objet | Identifiant exposé | Rôle et liens |
| --- | --- | --- |
| Article (`InventoryItem`) | `itemCode`, généré : `INV-<type>-<catégorie>-<AAAAMMJJ>-<8 chiffres>` (`InventoryItemServiceImpl.java:424`). Aussi `psku` (12 chiffres, généré si absent), `shortCode`, `displayCode`, `identificationCode`. | Type (`CONSUMABLE`, `ASSET`, `SERVICE`, `SPARE_PART`), mode de suivi (`trackingType`), catégorie, unité, coût par défaut et prix de vente (entiers), statut de cycle de vie. |
| Catégorie | `code` (fourni ou dérivé du nom, mis en majuscules, séparateur `_`) | Arborescence par `parentCategoryCode`. |
| Unité | `code` (fourni ou dérivé du nom, séparateur `_`) | `unitType` : `UNIT`, `KG`, `LITER`, `BOX`, `PACK`, `METER`. |
| Emplacement (`InventoryLocation`) | `locationCode`, **toujours généré** (séquence + millisecondes) : la requête n'a pas de champ code. | `SITE`, `WAREHOUSE`, `ROOM`, `SHELF`, `LOCKER`, `VIRTUAL`, parent facultatif, `businessCode` libre. |
| Niveau de stock (`StockLevel`) | couple `itemCode` + `locationCode` | `quantityOnHand` (physique), `quantityReserved`, `quantityAvailable` = physique moins réservé moins immobilisé (lots en quarantaine ou bloqués), `averageCost`. Créé automatiquement au premier mouvement. |
| Mouvement (`StockMovement`) | `movementCode` (séquence + `-` + millisecondes) | `IN`, `OUT`, `TRANSFER`, `ADJUSTMENT_IN`, `ADJUSTMENT_OUT`. Immuable : on le corrige par un mouvement inverse. |
| Réservation de stock | `reservationCode` | Bloque une quantité disponible sans la sortir. |
| Transfert à étapes (`StockTransferWorkflow`) | `transferCode` | Demande, approbation, expédition, réception ; le mouvement n'a lieu qu'à la réception. |
| Lot (`StockLot`) | `lotNumber` (texte, majuscules) ; **id numérique ⚠ `lotId`** pour quarantaine, blocage, inspection | Un lot par article, emplacement, numéro et date de péremption. `remainingQuantity`, péremption, quarantaine, blocage, propriétaire. |
| Numéro de série (`InventoryItemSerial`) | `serialNumber` | Pour les articles non `ASSET` avec `requiresSerialNumber`. Statuts `AVAILABLE`, `ISSUED`… |
| Équipement (`Asset`) | `assetCode` (`AST-<article>-<AAAAMM>-<séquence>`), `assetTag`, `serialNumber` | Un exemplaire physique d'un article de type `ASSET`, suivi à l'unité. |
| Attribution d'équipement (`AssetAssignment`) | aucun code ; id numérique ⚠ non exposé | Prêt (`ACTIVE`) ou réservation (`RESERVED`) à un membre, client, entreprise, utilisateur ou ressource. |
| Maintenance | `maintenanceCode` | Intervention planifiée sur un équipement. |
| Code-barres, conditionnement, substitut, traduction | **id numérique ⚠** (`id` dans la réponse) | Rattachés à un article ; suppression et marquage par id. |
| Modèle d'article (`InventoryItemTemplate`) | `templateCode` | Axes de variation (taille, couleur…) qui génèrent des articles variantes. |
| Plan de contrôle qualité | `planCode` | Vise un article **ou** une catégorie ; critères mesurés. |
| Inspection | `inspectionCode` | Résultat d'un contrôle sur un lot. |
| Non-conformité | `nonConformanceCode` (paramètre `code`) | Écart qualité, ouvert à la main ou par une inspection en échec. |
| Rappel produit | `recallCode` | Gèle une plage de lots d'un article. |
| Fournisseur | `supplierCode` (toujours généré, le champ `supplierCode` envoyé est ignoré) | Statut `ACTIVE`, `INACTIVE`, `BLOCKED`. |
| Demande d'achat | `requestCode` | Lignes article + quantité + coût estimé. |
| Commande d'achat | `orderCode` | Lignes, montant total, niveau d'approbation calculé. |
| Réception (`GoodsReceipt`) | `receiptCode` | Entrée en stock des quantités acceptées. |
| Inventaire physique (`InventoryCount`) | `countCode` | Comptage d'un emplacement, ajustements à la validation. |
| Période comptable | `periodCode` (fourni, sinon `AAAA-MM` de la date de début) | Ouverte ou close ; une période close refuse les mouvements. |
| Photo de valorisation | date `snapshotDate` | Valeur du stock par article et emplacement. |
| Écriture de stock | `movementCode` | Débit ou crédit du compte de stock pour chaque mouvement valorisé. |
| Motif d'ajustement | `reasonCode` (majuscules, séparateur `_`) | Compte de contrepartie, sens autorisé. |
| Seuil d'approbation d'ajustement | id ⚠ (non utilisé dans un chemin) | Tranche de montant qui exige un visa `approvedBy`. |
| Règle de réapprovisionnement | couple article + emplacement (ou article seul = règle globale) ; id ⚠ non utilisé | Seuils min, max, quantité de commande, fournisseur préféré. |
| Alerte | `alertCode` | Stock bas, rupture, péremption, garantie, maintenance, retour en retard, divergence. |

**Normalisation des codes.** Le serveur réécrit presque tous les codes reçus : espaces et caractères non alphanumériques remplacés par `-`, majuscules (`StockServiceImpl.java:926`). `abc 12` devient `ABC-12`. Exceptions : catégories, unités et motifs d'ajustement utilisent `_`, les numéros de lot et de rappel sont seulement mis en majuscules. Afficher toujours le code renvoyé par le serveur, jamais celui saisi.

**Montants.** Coûts et prix (`defaultCost`, `salePrice`, `unitCost`, `totalCost`, `averageCost`, montants des seuils) sont des entiers, sans décimales (unité monétaire non vérifiée dans le module). Les quantités sont des décimaux.

**Champs « par qui ».** `performedBy`, `approvedBy`, `receivedBy`, `requestedBy`… sont du **texte libre** que le serveur ne déduit pas du jeton. Le frontend doit les remplir lui-même (nom ou courriel du compte connecté). Plusieurs sont obligatoires (voir chaque parcours).

## Statuts et cycle de vie

### Article : `lifecycleStatus`

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `DRAFT` | En création, aucun mouvement possible. | Création ou `PATCH .../lifecycle`. | Tout autre statut. |
| `NEW`, `ACTIVE` | Entrées et sorties autorisées. `ACTIVE` est le défaut. | Création ; `PATCH .../activate` (met `ACTIVE`) ; `PATCH .../lifecycle`. | Tout autre statut. |
| `PHASE_OUT` | Fin de série : sorties et transferts autorisés, **entrées refusées**. | `PATCH .../lifecycle`. | Tout autre statut. |
| `OBSOLETE` | Plus aucune entrée ni sortie. | `PATCH .../deactivate` ; `PATCH .../lifecycle`. | Tout autre statut. |
| `BLOCKED` | Bloqué : ni entrée ni sortie. | `PATCH .../lifecycle`. | Tout autre statut. |

Aucune transition n'est interdite (`InventoryItemServiceImpl.java:113`). L'**ajustement** reste possible quel que soit le statut. Le champ `active` de la réponse est dérivé du statut (`true` pour `NEW`, `ACTIVE`, `PHASE_OUT`).

### Réservation de stock

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `ACTIVE` | Quantité retenue sur le disponible. | `POST /sni/api/v1/inventory/stock/reservations` (ou marketplace). | `CONSUMED`, `RELEASED`, `EXPIRED`. |
| `CONSUMED` | Sortie effectuée (mouvement `OUT`). | `PATCH .../consume`. | Terminal. |
| `RELEASED` | Levée sans sortie. | `PATCH .../release`. | Terminal. |
| `EXPIRED` | `expiresAt` dépassé. | Worker toutes les 30 min, ou tentative de consommer ou lever après l'échéance. | Terminal. |
| `CANCELLED` | Jamais attribué par le code. | Aucun. | |

### Transfert à étapes

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `REQUESTED` | Demandé, aucun mouvement. | `POST /sni/api/v1/inventory/stock/transfers`. | `APPROVED`, `CANCELLED`. |
| `APPROVED` | Approuvé. | `PATCH .../approve` (droit `INVENTORY:APPROVE`). | `SHIPPED`, `CANCELLED`. |
| `SHIPPED` | Expédié ; le stock est **toujours** à la source. | `PATCH .../ship`. | `RECEIVED`, `CANCELLED`. |
| `RECEIVED` | Mouvement `TRANSFER` créé (`movementCode` rempli). | `PATCH .../receive`. | Terminal. |
| `CANCELLED` | Annulé. | `PATCH .../cancel` depuis tout statut sauf `RECEIVED`. | Terminal. |

### Lot

Un lot n'a pas de statut unique mais trois indicateurs : `active`, `quarantined`, `blocked`. Un lot en quarantaine **ou** bloqué n'est pas consommé par les sorties et sa quantité restante est retirée du disponible.

| État | On y arrive par | On en sort par |
| --- | --- | --- |
| Quarantaine | `PATCH .../lots/{lotId}/quarantine` ; entrée avec `quarantined: true` ; plan qualité `quarantineOnReceipt` ; inspection décidée `QUARANTINED` ou `REJECTED`. | `PATCH .../lots/{lotId}/release` (deux personnes différentes) ; inspection `ACCEPTED` ou `ACCEPTED_BY_DEROGATION`. |
| Bloqué | `PATCH .../lots/{lotId}/block` ; lancement d'un rappel (motif `RECALL`). | `PATCH .../lots/{lotId}/unblock` ; annulation du rappel. |

### Équipement : `status`

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `AVAILABLE` | Libre. | Création ; retour en bon état ; fin de maintenance en bon état ; annulation de réservation ou de maintenance en cours. | `RESERVED`, `ASSIGNED`, `IN_MAINTENANCE`, `DAMAGED`, `LOST`, `RETIRED`. |
| `RESERVED` | Réservé pour un bénéficiaire. | `PATCH .../reserve`. | `ASSIGNED` (même bénéficiaire), `AVAILABLE` (annulation). |
| `ASSIGNED` | Prêté. | `PATCH .../assign`. | `AVAILABLE` ou `DAMAGED` au retour ; `LOST`, `RETIRED`. |
| `IN_MAINTENANCE` | En maintenance. | `PATCH /sni/api/v1/inventory/asset-maintenances/{code}/start`. | `AVAILABLE` ou `DAMAGED`. |
| `DAMAGED` | Endommagé. | `PATCH .../mark-damaged` ; retour ou fin de maintenance avec état `DAMAGED` ou `UNUSABLE`. | Maintenance, `LOST`, `RETIRED`, ou `PUT` avec un statut. |
| `LOST`, `RETIRED` | Perdu, réformé. Ferment le prêt en cours (`CANCELLED`). | `PATCH .../mark-lost`, `PATCH .../retire`. | Seulement par `PUT` avec `status`. |
| `IN_USE` | Jamais attribué par le code. | Aucun. | |

Le `PUT /sni/api/v1/inventory/assets/{assetCode}` accepte un `status` et l'applique sans contrôle de transition.

Attribution (`AssetAssignmentStatus`) : `RESERVED` puis `ACTIVE` (prêt), puis `RETURNED` ; `CANCELLED` à l'annulation de réservation, à la perte ou à la réforme.

Maintenance (`AssetMaintenanceStatus`) : `PLANNED` (création) puis `IN_PROGRESS` (`start`) puis `COMPLETED` (`complete`) ; `CANCELLED` depuis `PLANNED` ou `IN_PROGRESS`.

### Inventaire physique

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `DRAFT` | Créé pour un emplacement, sans ligne. | `POST /sni/api/v1/inventory/counts`. | `IN_PROGRESS`, `CANCELLED`. |
| `IN_PROGRESS` | Lignes créées (une par niveau de stock de l'emplacement, quantité attendue figée), saisie des comptages. | `PATCH .../start`. | `REVIEW`, `CANCELLED`. |
| `REVIEW` | Toutes les lignes comptées. | `PATCH .../review`. | `VALIDATED`, `CANCELLED`. |
| `VALIDATED` | Écarts passés en ajustements de stock. | `PATCH .../validate`. | Terminal. |
| `CANCELLED` | Annulé. | `PATCH .../cancel` depuis tout statut sauf `VALIDATED`. | Terminal (même un inventaire déjà annulé peut être « annulé » de nouveau). |

### Achats

Demande d'achat : `DRAFT` (création) puis `SUBMITTED` (`submit`) puis `APPROVED` (`approve`, droit `INVENTORY:APPROVE`) ou `REJECTED` (`reject`), puis `CONVERTED` quand une commande en est tirée. `CANCELLED` n'est jamais attribué.

Commande d'achat : `DRAFT` (création) puis `APPROVED` quand le nombre d'approbations requis est atteint (`approve` répété par des personnes différentes), puis `ORDERED` (`mark-ordered`), puis `PARTIALLY_RECEIVED` ou `RECEIVED` selon les réceptions. `CANCELLED` n'est jamais attribué : aucune route d'annulation.

Réception : toujours créée `POSTED`. `DRAFT` et `CANCELLED` ne sont jamais attribués.

### Qualité

Non-conformité : ouverte (`closedAt` vide) puis close (`close`). La clôture exige une `disposition` autre que `PENDING`.

Rappel produit (`ProductRecallStatus`) :

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `DRAFT` | Préparé, rien de gelé. | `POST /sni/api/v1/inventory/quality/recalls`. | `ACTIVE`, `CLOSED`, `CANCELLED`. |
| `ACTIVE` | Lots de la plage bloqués (`RECALL`), détenteurs calculés. | `PATCH .../launch`. | `CLOSED`, `CANCELLED`. |
| `CLOSED` | Terminé. Les lots **restent bloqués**. | `PATCH .../close` depuis tout statut sauf `CLOSED`. | Terminal. |
| `CANCELLED` | Annulé ; les lots bloqués pour rappel sont débloqués. | `PATCH .../cancel` depuis tout statut sauf `CLOSED`. | `CLOSED` reste possible. |

### Alerte

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `OPEN` | Active. Une seule alerte `OPEN` par type, article et emplacement (ou par équipement). | Mouvements de stock, workers. | `RESOLVED` automatiquement quand le stock remonte (rupture, négatif, stock bas, surstock) ; toute action manuelle. |
| `ACKNOWLEDGED` | Vue. | `PATCH .../acknowledge`. | Actions manuelles seulement : une alerte vue n'est **plus** résolue automatiquement, et une nouvelle alerte `OPEN` du même type peut apparaître. |
| `RESOLVED` | Résolue. | `PATCH .../resolve` ou automatisme. | Toute action manuelle (aucun contrôle de transition). |
| `DISMISSED` | Écartée. | `PATCH .../dismiss`. | Toute action manuelle. |

### Période comptable

`OPEN` (création, `reopen`) et `CLOSED` (`close`). `CLOSING` existe mais n'est jamais attribué.

## Parcours

| Fichier | Contenu |
| --- | --- |
| [parcours-catalogue.md](parcours-catalogue.md) | Listes de référence, catégories, unités, emplacements, articles (création, cycle de vie, prix, révisions), codes-barres, conditionnements, substituts, traductions, modèles et variantes, scan, étiquettes, conversions d'unités, import CSV ou XLSX. |
| [parcours-stock.md](parcours-stock.md) | Niveaux et mouvements, entrée, sortie, transfert direct, ajustement, annulation d'un mouvement, réservations, transferts à étapes, lots, numéros de série, quarantaine et blocage, traçabilité, aide au prélèvement, idempotence des mouvements. |
| [parcours-qualite.md](parcours-qualite.md) | Plans de contrôle, inspections, non-conformités, rappels produit. |
| [parcours-achats.md](parcours-achats.md) | Fournisseurs, règles de réapprovisionnement et suggestions, demandes d'achat, commandes et approbations, réceptions, alertes. |
| [parcours-inventaire-comptabilite.md](parcours-inventaire-comptabilite.md) | Inventaires physiques, valorisation (coût moyen, FIFO), motifs et seuils d'ajustement, périodes, photos de valorisation, journal comptable, tableau de bord, rapports, réconciliation. |
| [parcours-equipements.md](parcours-equipements.md) | Équipements suivis à l'unité : création, réservation, prêt, fiche de prêt, retour, perte, réforme, maintenance. |

## Règles et erreurs à refléter dans l'écran

Règles transverses (le détail est dans chaque parcours) :

- **Stock suffisant.** Sortie, transfert, réservation et ajustement négatif exigent `quantityAvailable >= quantité`, sauf si l'article a `allowNegativeStock` ou si la requête porte `allowNegativeOverride: true`. Sinon `400` `Insufficient stock available`. Les dérogations sont listées dans `GET /sni/api/v1/inventory/admin/reports/overrides`.
- **Cycle de vie.** Entrée refusée hors `NEW`, `ACTIVE` (`400` `Item X is in lifecycle status Y and cannot receive stock`) ; sortie et transfert refusés hors `NEW`, `ACTIVE`, `PHASE_OUT`.
- **Période close.** Si une période couvrant la date du jour (UTC) est `CLOSED`, toute entrée, sortie, transfert et ajustement est refusé : `400` `Period X is CLOSED : no movement can be dated in it.` Les mouvements sont toujours datés de l'instant présent : on ne peut pas antidater.
- **Quantités** strictement positives (`@Positive`) sur entrée, sortie, transfert, réservation, lot, ligne d'achat ; un ajustement de `0` est refusé.
- **Unicité** : code-barres unique sur tout le catalogue (`409` `Barcode already assigned: X`), `psku` unique (`409`), numéro de série unique par article, `assetTag` et numéro de série d'équipement uniques (`409` `Asset serial number or tag already exists`), un seul conditionnement par niveau et par article (`409`).
- **Formats** : `psku` = exactement 12 chiffres ; code-barres `EAN13` 13 chiffres, `EAN8` 8, `UPC` 12 (pas de contrôle de clé) ; `languageCode` des traductions limité à 8 caractères.

Forme des erreurs : celle du [README racine](../README.md#erreurs). Correspondances utiles :

| Exception serveur | Statut | `errorCode` |
| --- | --- | --- |
| Règle métier (`BadRequestException`) | `400` | `BAD_REQUEST` |
| Objet introuvable | `404` | `RESOURCE_NOT_FOUND` |
| Doublon (`ResourceAlreadyExistException`) | `409` | `RESOURCE_ALREADY_EXISTS` |
| Lot demandé absent en sortie, clé d'idempotence réutilisée | `409` | `CONFLICT` |
| Erreur non prévue (suppression d'un article référencé, date mal formée dans les rapports) | `500` | `INTERNAL_SERVER_ERROR` |

Les messages métier du module sont **en anglais** pour la plupart (`Insufficient stock available`, `Purchase order must be DRAFT`…), quelques-uns en français (scan, alertes). Prévoir une traduction côté écran à partir du message, ou un message générique avec le `traceId`.

## À prévoir côté frontend

**Écrans.** Catalogue (articles, catégories, unités, emplacements, modèles), fiche article (onglets codes-barres, conditionnements, substituts, traductions, prix, révisions, lots, numéros de série, conversions), stock (niveaux, journal des mouvements, formulaires entrée, sortie, transfert, ajustement), réservations, transferts à étapes, lots et quarantaine, traçabilité d'un lot, scan, qualité (plans, inspections, non-conformités, rappels), achats (fournisseurs, demandes, commandes, réceptions, suggestions), inventaires physiques, comptabilité (périodes, photos, journal, motifs, seuils), équipements et maintenance, alertes, tableau de bord et rapports, import.

**Droits** (pour masquer les boutons) : `INVENTORY:READ` pour toute lecture, `INVENTORY:CREATE` pour les `POST`, `INVENTORY:UPDATE` pour `PUT` et `PATCH`, `INVENTORY:DELETE` pour les `DELETE`, et `INVENTORY:APPROVE` pour tout chemin contenant `/approve` (transfert, demande d'achat, commande d'achat ; `AdminApiAuthorizationManager.java:183`). Attention : rejeter une demande d'achat ne demande que `INVENTORY:UPDATE`, et l'annulation d'un mouvement (`POST .../reverse`) demande `INVENTORY:CREATE`.

**Listes de référence** à charger une fois : `GET /sni/api/v1/inventory/reference/enums` (toutes les énumérations avec libellés français, cachable une heure) et `GET /sni/api/v1/inventory/reference/data` (unités, catégories, emplacements actifs et fournisseurs `ACTIVE`, à recharger après une modification). Chaque option a `value` (à envoyer), `label` (à afficher), `description`. Détail dans [parcours-catalogue.md](parcours-catalogue.md#listes-de-référence).

**Pagination.** Toutes les listes paginées du module renvoient une **page Spring** (`Page<T>`, éléments dans `content`), pas la page maison. Le paramètre `sort` est **ignoré** sur les listes servies par une requête native : mouvements (toujours du plus récent au plus ancien), recherche d'articles avec texte, recherche de fournisseurs avec texte, demandes, commandes et réceptions d'achat. Plusieurs listes ne sont pas paginées du tout (catégories, unités, emplacements, modèles, plans, rappels, règles, suggestions, périodes, journal).

**Identifiants numériques** ⚠ : `lotId` (quarantaine, blocage, inspections), `barcodeId`, `packagingId`, `substituteId`, `translationId`, `assignmentId`. Les lire en préservant les grands entiers et les garder en texte. Le `lotId` n'est **pas** dans les réponses de lots ; il ne s'obtient que par la traçabilité (voir [parcours-stock.md](parcours-stock.md#lots)).

**Idempotence.** Toutes les créations de mouvement (entrée, sortie, transfert, ajustement, annulation, réservation, consommation, levée, demande de transfert, réception d'achat, validation d'inventaire, import) acceptent `Idempotency-Key`. L'envoyer systématiquement : un double clic sur « Sortie » sans clé sort deux fois le stock. Détail dans [parcours-stock.md](parcours-stock.md#idempotence-des-mouvements).

**Fichiers.** Téléchargements : `journal.csv`, `reconciliation.csv`, `reports/movements.csv`, `reports/movements.pdf`, `reports/overrides.csv`, `loan-sheet.pdf`. Les routes CSV renvoient du texte (type `String` dans la doc générée) avec `Content-Disposition: attachment`. Téléversement : `POST /sni/api/v1/inventory/import/{type}` en `multipart/form-data`, partie `file` (CSV ou XLSX).

**Temps réel.** Abonnement STOMP à `/topic/inventory/alerts` (back-office seulement). Chaque message est un `InventoryAlertEvent` : `{ alertCode, alertType, itemCode, locationCode, assetCode, message, createdAt }`. **Seules les alertes créées par les workers** y sont publiées (péremption, rotation lente, garantie, maintenance, retours d'équipement, divergence). Les alertes nées d'un mouvement (stock bas, rupture, stock négatif, surstock) ne sont **pas** diffusées : rafraîchir `GET /sni/api/v1/inventory/alerts` après chaque mouvement, ou périodiquement.

**Champs à ne pas envoyer** : codes générés (`itemCode` en création, `locationCode`, `supplierCode`, `assetCode`, `templateCode`), champs d'affichage des réponses (`itemName`, `locationName`, `categoryName`, `active` dérivé…). Le JSON est strict : une propriété inconnue donne `400`.

**`PUT` = remplacement complet.** Un `PUT` d'article remet à leur valeur par défaut les booléens absents (`allowNegativeStock`, `requiresLotNumber`… à `false`, `stackable` à `true`) et vide la catégorie ou l'unité absentes. Renvoyer tout le formulaire.

## Effets automatiques

| Déclencheur | Effet visible |
| --- | --- |
| Tout mouvement de stock | Écriture comptable si le mouvement est valorisé (hors transfert) ; événement `inventory.stock.increased`, `.decreased` ou `.transferred` dans la file d'événements (outbox) ; détection d'alertes `NEGATIVE_STOCK`, `OUT_OF_STOCK`, `LOW_STOCK` ou `RECURRING_LOW_STOCK` (à partir de deux stocks bas résolus), `OVERSTOCK` ; résolution automatique des alertes `OPEN` correspondantes quand le stock remonte. |
| Alerte `LOW_STOCK` ou `RECURRING_LOW_STOCK` créée | Tâche « Réapprovisionner … » dans le module task, priorité `HIGH` ou `URGENT`, échéance `TASK_INVENTORY_REORDER_DUE_HOURS` (24 h par défaut), assignée à `TASK_INVENTORY_DEFAULT_ASSIGNEE` si configuré. |
| Entrée d'un article `ASSET` | Un équipement `AVAILABLE` créé par unité reçue. |
| Entrée d'un lot avec un plan qualité `ON_RECEIPT` à `quarantineOnReceipt` | Lot mis en quarantaine par `system`. |
| Worker réservations, toutes les 30 min (`INVENTORY_RESERVATION_CRON`) | Réservations échues passées `EXPIRED`, quantité rendue au disponible. |
| Worker péremption, toutes les 2 h (`INVENTORY_EXPIRY_CRON`) | Alertes `EXPIRY_SOON` (lot périmant sous 30 jours) et `EXPIRY_IMMINENT` (sous 7 jours, lots déjà périmés compris). |
| Worker rotation lente, toutes les 6 h | Alerte `SLOW_MOVING` pour un article ayant une règle de réapprovisionnement, du stock disponible et aucune sortie depuis 30 jours. |
| Worker équipements, toutes les 3 h | Alertes `WARRANTY_SOON` (garantie finissant sous 30 jours ou déjà finie), `MAINTENANCE_DUE` (maintenance `PLANNED` sous 24 h), `ASSET_RETURN_OVERDUE`, `ASSET_RETURN_DUE_SOON` (retour attendu sous 24 h). |
| Worker réconciliation, lundi 3 h (`INVENTORY_RECONCILIATION_CRON`) | Alerte `STOCK_LEVEL_DIVERGENCE` si un niveau de stock diffère de la somme de ses mouvements. Rien n'est corrigé. |
| Achats, inventaires, équipements, fournisseurs | Événements outbox (`inventory.purchase_order.created`, `inventory.goods_receipt.posted`, `inventory.count.validated`, `inventory.asset.assigned`…), exploitables par les webhooks du module [notification](../notification/README.md) (abonnement par type d'événement, non vérifié pour ces types précis). |

Aucun courriel n'est envoyé par ce module. Les alertes créées par les workers sont publiées sur `/topic/inventory/alerts` et dans l'outbox (`inventory.alert.created`) ; celles nées d'un mouvement seulement dans l'outbox.

## Écarts relevés

1. **Corrigé le 2026-11-08** : la route répond sur `GET /sni/api/v1/inventory/items/{itemCode}/serials`. Constat d'origine : **Route des numéros de série inaccessible au chemin prévu.** `StockController` est préfixé `/inventory/stock` et la méthode déclare le chemin complet : la route réelle est `GET /sni/api/v1/inventory/stock/sni/api/v1/inventory/items/{itemCode}/serials` (`src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/StockController.java:114`). Le frontend doit appeler ce chemin tel quel.
2. **`lotId` absent des réponses de lots.** `StockLotResponse` n'a pas d'identifiant (`stock/dto/response/StockLotResponse.java`), alors que quarantaine, libération, blocage, déblocage et inspections exigent `{lotId}`. Seule `GET /sni/api/v1/inventory/traceability/{lotNumber}` le donne (`positions[].lotId`).
3. **Fiche de prêt inutilisable.** `GET .../assets/{assetCode}/assignments/{assignmentId}/loan-sheet.pdf` exige un `assignmentId` qu'aucune réponse n'expose (`asset/dto/response/AssetAssignmentResponse.java:13`).
4. **Non-conformités automatiques impossibles à clore.** Une inspection en échec ouvre une non-conformité avec `disposition = PENDING` ; la clôture refuse `PENDING` (`stock/service/implementation/QualityControlServiceImpl.java:230`) et aucune route ne permet de changer la disposition.
5. **Création de lot hors stock.** `POST /sni/api/v1/inventory/items/{itemCode}/lots` crée un lot avec `initialQuantity` sans mouvement ni mise à jour du niveau de stock (`stock/service/implementation/StockLotServiceImpl.java:61`) : le lot et le niveau divergent. Son numéro est seulement mis en majuscules, alors que l'entrée de stock le normalise avec `-` : `lot 1` donne `LOT 1` ici et `LOT-1` à l'entrée.
6. **Libération de quarantaine par inspection sans recalcul du disponible.** Une inspection acceptée retire la quarantaine mais ne recalcule pas `quantityQuarantined` du niveau (`QualityControlServiceImpl.java:341`), alors que `StockQuarantineServiceImpl` le fait : le disponible reste minoré jusqu'au prochain blocage ou quarantaine sur ce couple.
7. **Filtres par catégorie inopérants pour les codes à `_`.** Les catégories ont des codes à `_` (`catalog/service/implementation/InventoryCategoryServiceImpl.java:99`), mais `GET /stock/levels?categoryCode=` et `GET /items?categoryCode=` normalisent le filtre avec `-` (`StockServiceImpl.java:377`, `catalog/repository/specification/InventoryItemSpecification.java:38`) : `FOURNITURES_BUREAU` devient `FOURNITURES-BUREAU` et ne trouve rien.
8. **`confirmCreateLot` sans effet.** En sortie, un lot inconnu avec `confirmCreateLot: true` passe le contrôle (`StockServiceImpl.java:548`), puis la consommation filtre sur ce lot inexistant et échoue en `Insufficient lot quantity available`, sauf dérogation négative.
9. **Traçabilité amont toujours vide, généalogie jamais remplie.** Les entrées n'enregistrent pas de ligne de lot par mouvement (seules les sorties, transferts et ajustements négatifs le font, `StockServiceImpl.java:594`), donc `upstream` est toujours vide ; aucune écriture de `StockLotGenealogy` n'existe dans le code, donc `parents` et `children` sont toujours vides.
10. **Articles suivis par numéro de série bloquants dans les automatismes.** Réception d'achat, réception de transfert à étapes et consommation de réservation n'envoient aucun numéro de série : pour un article non `ASSET` avec `requiresSerialNumber`, ces opérations échouent (`Serial numbers are required for this item`). De même, la validation d'un inventaire avec un écart positif sur un article suivi par lot échoue (`Lot number or expiry date is required for tracked inventory adjustment`), car l'ajustement automatique ne porte pas de lot ; elle échoue aussi dès qu'un seuil d'approbation d'ajustement couvre la valeur d'un écart, car aucun `approvedBy` n'est transmis (`control/service/InventoryCountServiceImpl.java`, méthode `validate`).
11. **Import tout ou rien probable.** `importFile` est transactionnel (`importer/service/InventoryImportServiceImpl.java:46`) : une ligne qui échoue dans un service transactionnel marque la transaction pour annulation ; la réponse ligne par ligne risque alors d'être remplacée par une erreur `500` et rien n'est importé. Non vérifié à l'exécution.
12. **Annulation de mouvement incomplète.** `reverseMovement` ne contrôle pas la période comptable, ne recalcule ni couches FIFO ni coût moyen, et ne touche ni aux numéros de série ni aux équipements créés (`StockServiceImpl.java:323`). La consommation de réservation crée un `OUT` sans coût (`StockServiceImpl.java:304`), donc sans écriture comptable.
13. **Recherche d'alerte par chargement complet.** `byCode` charge toutes les alertes pour en trouver une (`intelligence/service/implementation/InventoryAlertServiceImpl.java:58`) : lenteur croissante des actions sur alertes.
14. **Statuts jamais atteints** : `StockReservationStatus.CANCELLED`, `PurchaseRequestStatus.CANCELLED`, `PurchaseOrderStatus.CANCELLED`, `GoodsReceiptStatus.DRAFT` et `CANCELLED`, `InventoryPeriodStatus.CLOSING`, `AssetStatus.IN_USE`, `InventoryAlertType.SUSPICIOUS_ADJUSTMENT`. Aucune route d'annulation de demande ou de commande d'achat.
15. **Pas de route pour** : modifier ou désactiver un motif d'ajustement ou un seuil d'approbation, modifier un plan qualité, supprimer une règle de réapprovisionnement, gérer les tarifs fournisseurs (`SupplierItem`, jamais écrit : `bestSupplierCode` des suggestions toujours vide), fixer la méthode de valorisation d'une catégorie.
16. **Équipements et stock non reliés.** Créer un équipement par `POST /assets` ne touche pas au stock ; le perdre ou le réformer non plus. Seule l'entrée de stock d'un article `ASSET` crée des équipements. Les deux vues peuvent diverger.
17. **Emplacement ou fournisseur inactif accepté.** Les mouvements ne vérifient pas `active` de l'emplacement ; une commande d'achat accepte un fournisseur `BLOCKED` ou `INACTIVE`.
18. **Transitions sans audit ni idempotence** : approbation, expédition, réception, annulation de transfert à étapes ; `start`, `review`, `cancel` d'inventaire ; toutes les transitions d'achat sauf création et réception ; création de conversion d'unité et étiquettes.
19. **Photo de valorisation à date passée** : `POST /accounting/snapshots?snapshotDate=` enregistre le stock **actuel** sous la date demandée (`InventoryPeriodServiceImpl.java:165`), et remplace toute photo existante à cette date.
20. **Messages d'erreur majoritairement en anglais** (voir plus haut).
