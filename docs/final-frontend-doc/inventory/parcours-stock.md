# inventory : parcours du stock

> Suite de [README.md](README.md). Routes exactes et champs JSON : [routes/StockController.md](routes/StockController.md), [routes/StockLotController.md](routes/StockLotController.md), [routes/StockTraceabilityController.md](routes/StockTraceabilityController.md), [routes/StockTransferWorkflowController.md](routes/StockTransferWorkflowController.md), [routes/StockPlanningController.md](routes/StockPlanningController.md).

Code source principal : `src/main/java/com/sni/bokaticowork/features/inventory/stock/service/implementation/StockServiceImpl.java`.

## Consulter le stock

1. Niveaux : `GET /sni/api/v1/inventory/stock/levels` ([détail](routes/StockController.md#r-get-sni-api-v1-inventory-stock-levels)), filtres `itemCode`, `locationCode`, `categoryCode`, `availableOnly` (disponible > 0), `lowStock` (disponible <= 0, **pas** le seuil des règles de réapprovisionnement). Page Spring, tri `item.name`.
2. Pour chaque ligne afficher `quantityOnHand` (physique), `quantityReserved`, `quantityAvailable`. La part immobilisée (lots en quarantaine ou bloqués) n'est pas un champ de la réponse : elle vaut `quantityOnHand - quantityReserved - quantityAvailable`.
3. `averageCost` : coût moyen pondéré courant (entier), maintenu quelle que soit la méthode de valorisation.
4. Journal : `GET /sni/api/v1/inventory/stock/movements` ([détail](routes/StockController.md#r-get-sni-api-v1-inventory-stock-movements)), filtres `itemCode`, `locationCode`, `movementType`, `referenceType`, `referenceCode`, `fromDate`, `toDate` (date-heure ISO, sans fuseau acceptée en paramètre). Toujours trié du plus récent au plus ancien, `sort` ignoré.

Chaque mouvement donne `movementCode`, `movementType`, `locationFromCode` (vide pour une entrée), `locationToCode` (vide pour une sortie), `quantity` (toujours positive, le sens est dans le type), `unitCost` et `totalCost` (peuvent être vides si aucun coût n'est connu), `reasonCode`, `reason`, `allowNegativeOverride`, `lots[]` (lots consommés), `reversed`, `reversalOfMovementCode`, `performedBy`, `performedAt`.

## Entrée en stock

`POST /sni/api/v1/inventory/stock/in` ([détail](routes/StockController.md#r-post-sni-api-v1-inventory-stock-in)), avec `Idempotency-Key`.

1. Envoyer `itemCode`, `locationCode`, `quantity` (positive), `unitCost` (entier, conseillé : il alimente le coût moyen et l'écriture comptable), `referenceType` et `referenceCode` (facultatifs), `reason`, `performedBy`.
2. Selon l'article (lire la fiche avant d'afficher le formulaire) :
   - suivi par lot (`trackingType` `LOT` ou `requiresLotNumber`) : `lotNumber` obligatoire ;
   - suivi par péremption (`trackingType` `EXPIRY` ou `requiresExpiryDate`) : `expiryDate` obligatoire ;
   - article non `ASSET` avec `requiresSerialNumber` : `assetSerialNumbers` obligatoire, **autant que d'unités** (quantité entière), chacun inconnu pour cet article ;
   - article `ASSET` : quantité entière ; un équipement est créé par unité (`AVAILABLE`, état `GOOD`). `assetSerialNumbers` et `assetTags` facultatifs, mais si fournis, autant que d'unités.
3. Facultatif sur le lot : `quarantined` + `quarantineReason`, `ownershipType` (`COMPANY`, `CUSTOMER`, `SUPPLIER`, `CONSIGNMENT`) + `ownerCode`. Un même numéro de lot et une même date de péremption au même emplacement cumulent sur le lot existant.
4. Réponse : le mouvement `IN`. Rafraîchir les niveaux. Si un plan qualité de réception l'exige, le lot est mis en quarantaine automatiquement (non visible dans la réponse : relire le lot).

Refus fréquents : article hors `NEW`/`ACTIVE` (`400`), période close (`400`), `Lot number is required for this inventory item`, `Expiry date is required for this inventory item`, `Serial numbers count (x) must match received quantity (y)`, `Serial number already registered: X`, `Asset stock receipt quantity must be a whole number`, `Asset serial number or tag already exists`.

## Sortie de stock

`POST /sni/api/v1/inventory/stock/out` ([détail](routes/StockController.md#r-post-sni-api-v1-inventory-stock-out)), avec `Idempotency-Key`.

1. Obligatoires : `itemCode`, `locationCode`, `quantity`, `reasonCode` (`CONSUMPTION`, `DAMAGE`, `LOSS`, `INTERNAL_USE`, `SAMPLE`, `DONATION`, `OTHER`), `performedBy`. Facultatifs : `reasonDetails`, `referenceType`, `referenceCode`.
2. Lots : sans `lotNumber`, le serveur consomme les lots du plus proche de la péremption au plus lointain (lots sans date en dernier), en ignorant les lots en quarantaine ou bloqués. Avec `lotNumber`, seul ce lot est consommé ; s'il n'existe pas à cet emplacement : `409` `CONFLICT`, message `Could not process LOT_NOT_FOUND: Lot 'X' does not exist at this location...` (le drapeau `confirmCreateLot` proposé dans ce message ne fonctionne pas, voir l'écart 8 du [README](README.md#écarts-relevés)).
3. Numéros de série (article non `ASSET` avec `requiresSerialNumber`) : `serialNumbers` obligatoire, chacun `AVAILABLE` et présent à cet emplacement. Ils passent `ISSUED`. Le nombre n'est pas comparé à la quantité.
4. Dérogation : `allowNegativeOverride: true` laisse passer une sortie supérieure au disponible (ou aux lots consommables). Ne proposer la case qu'après un refus `Insufficient stock available` ou `Insufficient lot quantity available`, avec confirmation. La dérogation est tracée.
5. Réponse : le mouvement `OUT` avec `lots[]` consommés et le coût de sortie (coût moyen, ou couches FIFO).

## Transfert direct

`POST /sni/api/v1/inventory/stock/transfer` ([détail](routes/StockController.md#r-post-sni-api-v1-inventory-stock-transfer)) : déplacement immédiat entre deux emplacements, en un mouvement `TRANSFER`.

- Obligatoires : `itemCode`, `fromLocationCode`, `toLocationCode` (différents, sinon `400`), `quantity`, `reasonCode` (exigé même pour un transfert ; `INTERNAL_USE` convient), `performedBy`.
- Les lots partent de la source dans l'ordre de péremption et sont recréés à destination avec le même numéro et la même date ; pas de choix de lot. Les numéros de série (si exigés) passent dans `serialNumbers`.
- Aucune écriture comptable : un transfert ne change pas la valeur du stock.

Pour un circuit avec validation, utiliser le [transfert à étapes](#transfert-à-étapes).

## Ajustement

`POST /sni/api/v1/inventory/stock/adjust` ([détail](routes/StockController.md#r-post-sni-api-v1-inventory-stock-adjust)) : correction d'écart, seul mouvement permis quel que soit le cycle de vie de l'article.

1. Obligatoires : `itemCode`, `locationCode`, `quantityDelta` (signé, non nul : `+5` ou `-3`), `performedBy`.
2. `adjustmentReasonCode` : motif codifié, à choisir dans `GET /sni/api/v1/inventory/accounting/adjustment-reasons?active=true`. Facultatif, mais s'il est fourni il doit exister, être actif et accepter le sens de l'écart (motifs `positiveOnly` ou `negativeOnly`), sinon `400` (`Unknown adjustment reason`, `Adjustment reason is inactive`, `... does not apply to a positive adjustment`). Filtrer la liste selon le signe saisi.
3. `approvedBy` : obligatoire si la valeur de l'écart (`|quantité| x coût`, avec le coût envoyé, sinon le coût moyen, sinon le coût par défaut) tombe dans une tranche de `GET /sni/api/v1/inventory/accounting/adjustment-approval-rules`. Sinon `400` `Adjustment of N requires a MANAGER approval. Provide approvedBy.` Calculer la valeur à l'écran pour afficher le champ à l'avance.
4. Écart positif sur un article suivi par lot ou péremption : `lotNumber` ou `expiryDate` obligatoire. Écart négatif : consomme les lots comme une sortie.
5. `unitCost` facultatif : valorise le mouvement (et, en positif, entre dans le coût moyen).
6. `referenceType` vaut `MANUAL_ADJUSTMENT` par défaut. `reasonCode` (motif de sortie) n'est conservé que sur un écart négatif.
7. Réponse : un mouvement `ADJUSTMENT_IN` ou `ADJUSTMENT_OUT`.

## Annuler un mouvement

`POST /sni/api/v1/inventory/stock/movements/{movementCode}/reverse?performedBy=&reason=` ([détail](routes/StockController.md#r-post-sni-api-v1-inventory-stock-movements-movementcode-reverse)), droit `INVENTORY:CREATE`, avec `Idempotency-Key`.

- Crée le mouvement inverse (une entrée annulée donne un `ADJUSTMENT_OUT`, une sortie un `ADJUSTMENT_IN`, un transfert un transfert en sens inverse) et marque l'original `reversed: true`. La réponse est le **mouvement inverse** ; son `reversalOfMovementCode` pointe l'original.
- Refusé (`400`) : mouvement déjà annulé, mouvement qui est lui-même une annulation, mouvement d'inventaire physique (`INVENTORY_COUNT`), mouvement de réception d'achat (`GOODS_RECEIPT`).
- Annuler une entrée ou un transfert exige que la quantité soit encore disponible à l'emplacement (pas de dérogation possible ici, sauf article autorisé en stock négatif).
- N'afficher le bouton que si `reversed` est faux, `reversalOfMovementCode` est vide et `referenceType` n'est ni `INVENTORY_COUNT` ni `GOODS_RECEIPT`.
- Limites (écart 12 du [README](README.md#écarts-relevés)) : numéros de série et équipements créés ne sont pas défaits, la période close n'est pas vérifiée.

## Réservations

1. Réserver : `POST /sni/api/v1/inventory/stock/reservations` avec `itemCode`, `locationCode`, `quantity`, `referenceType`, `referenceCode`, `reservedBy`, `expiresAt` (date-heure avec fuseau, facultative). Exige du disponible (aucune dérogation). Ni le cycle de vie ni la période ne sont vérifiés. Le disponible baisse aussitôt.
2. Liste : `GET .../reservations?status=` (page Spring, tri `reservedAt`).
3. Consommer : `PATCH .../reservations/{reservationCode}/consume?performedBy=` : crée un `OUT` (motif `CONSUMPTION`) de toute la quantité réservée, sans coût. Pas de consommation partielle.
4. Lever : `PATCH .../reservations/{reservationCode}/release` : rend la quantité au disponible.
5. Réservation non `ACTIVE` : `400` `Stock reservation is not active`. Échéance dépassée au moment de l'action : la réservation passe `EXPIRED` et l'appel répond `400` `Stock reservation has expired`.

Les commandes du module marketplace créent leurs propres réservations (`referenceType` `POS_SALE`, `referenceCode` = numéro de commande) : ne pas les lever à la main depuis cet écran sans prévenir, la commande les consomme ensuite.

## Transfert à étapes

Pour un déplacement entre sites qui doit être approuvé et suivi ([routes](routes/StockTransferWorkflowController.md)).

1. Demander : `POST /sni/api/v1/inventory/stock/transfers` avec `itemCode`, `fromLocationCode`, `toLocationCode`, `quantity`, `requestedBy`, `reason`, et `Idempotency-Key`. Statut `REQUESTED`. Aucun contrôle de stock à ce stade, rien ne bouge.
2. Approuver : `PATCH .../transfers/{transferCode}/approve?approvedBy=` (droit `INVENTORY:APPROVE`). Doit être `REQUESTED`.
3. Expédier : `PATCH .../ship?shippedBy=`. Doit être `APPROVED`. Le stock reste compté à la source.
4. Recevoir : `PATCH .../receive?receivedBy=`. Doit être `SHIPPED`. Le transfert réel a lieu maintenant, avec tous les contrôles d'un transfert direct (stock suffisant à la source, cycle de vie, période). En cas de refus, le transfert reste `SHIPPED`. `movementCode` est alors rempli.
5. Annuler : `PATCH .../cancel?cancelledBy=&reason=`, possible tant que le statut n'est pas `RECEIVED`.
6. Liste : `GET .../transfers?status=` (page Spring, tri `requestedAt`).

Messages de transition : `Transfer must be REQUESTED`, `Transfer must be APPROVED`, `Transfer must be SHIPPED`, `Received transfer cannot be cancelled` (tous en `400`). Ne proposer que le bouton de l'étape suivante. Ces transitions n'acceptent pas d'`Idempotency-Key` : désactiver le bouton pendant l'appel.

## Lots

1. Recherche : `GET /sni/api/v1/inventory/stock/lots` avec `itemCode`, `locationCode`, `lotNumber` (recherche partielle), `expiringBefore` (date), `active`, `remainingOnly` ; page Spring, tri `expiryDate`.
2. Lots d'un article : `GET /sni/api/v1/inventory/items/{itemCode}/lots?locationCode=&activeOnly=` (liste).
3. Lots immobilisés : `GET /sni/api/v1/inventory/stock/lots/immobilised?itemCode=&locationCode=` (quarantaine ou blocage).
4. Création manuelle : `POST /sni/api/v1/inventory/items/{itemCode}/lots`. **À éviter** : elle ne crée ni mouvement ni stock (écart 5 du [README](README.md#écarts-relevés)). Les lots se créent normalement par une entrée ou une réception d'achat.

**Obtenir le `lotId`.** Les réponses de lots n'ont pas d'identifiant. Pour agir sur un lot (quarantaine, blocage, inspection), appeler `GET /sni/api/v1/inventory/traceability/{lotNumber}` et prendre `positions[].lotId` de la ligne dont `itemCode` et `locationCode` correspondent. C'est un int64 ⚠ : le lire en préservant les grands entiers.

## Quarantaine et blocage

Les deux immobilisent un lot (ses quantités sortent du disponible et ne sont plus consommées) ; la quarantaine relève de la qualité, le blocage d'une décision (litige, rappel, administratif).

1. Quarantaine : `PATCH /sni/api/v1/inventory/stock/lots/{lotId}/quarantine` avec `reason` (obligatoire), `quarantinedBy`. Déjà en quarantaine : `400`.
2. Libération : `PATCH .../lots/{lotId}/release` avec `requestedBy` et `approvedBy` **différents** (comparaison sans casse), sinon `400` `Releasing a quarantined lot requires an approver different from the requester`. Le formulaire doit faire saisir l'approbateur.
3. Blocage : `PATCH .../lots/{lotId}/block` avec `reasonType` (`PENDING_INSPECTION`, `FAILED_INSPECTION`, `SUPPLIER_DISPUTE`, `RECALL`, `EXPIRY`, `ADMINISTRATIVE`), `reason`, `blockedBy`. Déjà bloqué : `400`.
4. Déblocage : `PATCH .../lots/{lotId}/unblock?unblockedBy=`. Pas de double contrôle. Éviter de débloquer un lot bloqué pour `RECALL` tant que le rappel est actif.

Chaque action renvoie le lot et recalcule le disponible du niveau concerné.

## Numéros de série

`GET /sni/api/v1/inventory/stock/sni/api/v1/inventory/items/{itemCode}/serials?locationCode=&status=` (chemin anormal mais réel, écart 1 du [README](README.md#écarts-relevés)). Liste des numéros avec `status`, `locationCode`, `receivedAt`, `issuedAt`. Sert à proposer les numéros `AVAILABLE` d'un emplacement dans les formulaires de sortie et de transfert.

## Traçabilité d'un lot

`GET /sni/api/v1/inventory/traceability/{lotNumber}` ([détail](routes/StockTraceabilityController.md#r-get-sni-api-v1-inventory-traceability-lotnumber)). Numéro mis en majuscules, recherche exacte.

- `resolved` : faux si le lot n'existe nulle part (pas d'erreur `404`).
- `positions[]` : une ligne par emplacement où le lot est actif, avec `lotId`, quantités, quarantaine, blocage.
- `downstream[]` : sorties, transferts et ajustements négatifs qui ont consommé ce lot, avec `reversed`.
- `upstream[]`, `parents[]`, `children[]` : toujours vides dans l'état du code (écart 9 du [README](README.md#écarts-relevés)). Pour l'origine du lot, chercher les mouvements `IN` de l'article dans le journal.

## Aide au prélèvement

`GET /sni/api/v1/inventory/stock/planning/picking?itemCode=&quantity=` : répartit la quantité demandée sur les emplacements ayant du disponible, du plus fourni au moins fourni. Réponse : `lines[]` (`locationCode`, `availableQuantity`, `pickQuantity`), `suggestedQuantity`, `fullyCovered`. Simple suggestion : chaque prélèvement reste une sortie à enregistrer.

## Idempotence des mouvements

Règle générale : [README racine](../README.md#idempotence). Fonctionnement vérifié dans `src/main/java/com/sni/bokaticowork/core/idempotency/` :

- La clé est propre à une **opération** (`INVENTORY_STOCK_IN`, `INVENTORY_STOCK_OUT`, `INVENTORY_STOCK_TRANSFER`, `INVENTORY_STOCK_ADJUST`, `INVENTORY_STOCK_MOVEMENT_REVERSE`, `INVENTORY_STOCK_RESERVE`, `INVENTORY_STOCK_RESERVATION_RELEASE`, `INVENTORY_STOCK_RESERVATION_CONSUME`, `INVENTORY_STOCK_TRANSFER_REQUEST`, `INVENTORY_GOODS_RECEIPT_POST`, `INVENTORY_COUNT_VALIDATE`, `INVENTORY_IMPORT`…), pas à un compte : deux postes ne doivent jamais partager une clé. Utiliser un UUID par action.
- Même clé, même contenu, première exécution réussie : la réponse d'origine est rejouée, rien n'est refait.
- Même clé, contenu différent : `409` `CONFLICT` `Could not process idempotency request: the same key was reused with a different payload`. Ce cas arrive si l'utilisateur corrige le formulaire après une erreur : générer une **nouvelle clé** dès qu'un champ change.
- Même clé pendant que la première requête tourne : `409` `... another request with the same key is still processing`. Attendre puis rejouer la même requête.
- Première exécution en erreur : la même clé peut être réutilisée, l'opération est retentée.
- Le contenu comparé est le corps JSON. Pour les routes sans corps (`reverse`, `consume`, `release`, `validate`), seul le code du chemin compte : les paramètres de requête (`performedBy`, `reason`) ne sont pas comparés.
- La clé est facultative par défaut mais peut devenir obligatoire par configuration (`IDEMPOTENCY_REQUIRED_OPERATIONS`, exemple documenté : `INVENTORY_STOCK_IN,INVENTORY_STOCK_OUT,INVENTORY_STOCK_TRANSFER,INVENTORY_STOCK_ADJUST`) ; sans elle, `400` `Missing required header: Idempotency-Key`.

Recommandation : générer la clé à l'ouverture du formulaire, la garder pour les nouvelles tentatives après une coupure réseau, la renouveler à chaque modification d'un champ et après chaque succès.
