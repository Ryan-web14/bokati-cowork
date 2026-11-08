# inventory : parcours inventaires, valorisation et comptabilité

> Suite de [README.md](README.md). Routes exactes et champs JSON : [routes/InventoryCountController.md](routes/InventoryCountController.md), [routes/InventoryAccountingController.md](routes/InventoryAccountingController.md), [routes/InventoryAdminController.md](routes/InventoryAdminController.md). Code : `src/main/java/com/sni/bokaticowork/features/inventory/control/service/InventoryCountServiceImpl.java`, `stock/service/implementation/InventoryPeriodServiceImpl.java`, `StockAccountingServiceImpl.java`, `StockValuationServiceImpl.java`, `admin/service/InventoryAdminServiceImpl.java`.

## Inventaire physique

Un inventaire porte sur **un emplacement**.

1. Créer : `POST /sni/api/v1/inventory/counts` avec `locationCode` (obligatoire), `createdBy`, `notes`, et `Idempotency-Key`. Statut `DRAFT`, aucune ligne.
2. Démarrer : `PATCH .../counts/{countCode}/start`. Doit être `DRAFT`. Le serveur crée une ligne par niveau de stock existant à cet emplacement, avec `expectedQuantity` = quantité physique **à cet instant**. Statut `IN_PROGRESS`.
3. Saisir : `PUT .../counts/{countCode}/lines` avec `itemCode`, `countedQuantity` (>= 0), `notes`, une ligne par appel. Doit être `IN_PROGRESS`. Un article absent des lignes (trouvé en rayon sans niveau de stock) est ajouté avec l'attendu courant. Ressaisir le même article remplace le comptage. La réponse est l'inventaire complet : afficher `varianceQuantity` (compté moins attendu) de chaque ligne.
4. Passer en revue : `PATCH .../review`. Toutes les lignes doivent être comptées (`400` `All inventory count lines must be counted before review`). Un article réellement absent se saisit à `0`.
5. Valider : `PATCH .../validate?performedBy=` avec `Idempotency-Key`. Doit être `REVIEW`. Chaque écart non nul devient un ajustement (`referenceType = INVENTORY_COUNT`, `referenceCode = countCode`, dérogation négative accordée d'office). Statut `VALIDATED`, événement `inventory.count.validated`.
6. Annuler : `PATCH .../cancel` (tout statut sauf `VALIDATED`).
7. Liste : `GET .../counts?status=` (page Spring, tri `createdAt`) ; détail : `GET .../counts/{countCode}`.

À savoir avant de valider :

- Les attendus sont figés au démarrage : un mouvement fait pendant le comptage fausse l'écart. Conseiller de geler l'emplacement pendant l'inventaire.
- La validation est **tout ou rien** (une seule transaction). Elle échoue si une période close couvre le jour, si un écart positif concerne un article suivi par lot ou péremption (l'ajustement n'a pas de lot), ou si un seuil d'approbation d'ajustement couvre la valeur d'un écart (aucun `approvedBy` n'est transmis). Le message d'erreur est celui de l'ajustement en cause, sans indication de la ligne.
- Les mouvements d'inventaire ne peuvent pas être annulés par la route d'annulation de mouvement.
- Un inventaire non validé (`DRAFT`, `IN_PROGRESS`, `REVIEW`) créé dans une période empêche de clore cette période.

## Valorisation

Chaque article a une méthode : la sienne, sinon celle de sa catégorie, sinon `WEIGHTED_AVERAGE` (aucune route ne fixe la méthode d'une catégorie).

- `WEIGHTED_AVERAGE` (coût moyen pondéré) : chaque entrée recalcule `averageCost` du niveau ; sorties et transferts sont valorisés au coût moyen courant ; un transfert donne à la destination le coût moyen de la source.
- `FIFO` : chaque entrée crée une couche de coût ; les sorties consomment les couches les plus anciennes et leur coût unitaire est la moyenne pondérée des couches prélevées. Les transferts déplacent les couches avec leur date d'origine. `averageCost` reste maintenu.
- Changer de méthode : `PATCH /sni/api/v1/inventory/items/{itemCode}/valuation-method` avec `valuationMethod`, `reason`, `changedBy`. Le passage à `FIFO` crée une couche par emplacement en stock, au coût moyen du moment ; la réponse est le nombre de couches créées.
- Une entrée sans `unitCost` n'apporte pas de coût : le mouvement peut rester non valorisé et ne produit alors aucune écriture.

## Motifs et seuils d'ajustement

1. Motifs : `GET /sni/api/v1/inventory/accounting/adjustment-reasons?active=true` ; création `POST` avec `reasonCode` (majuscules, séparateur `_`), `label`, `counterpartAccount` (compte de contrepartie de l'écriture), `positiveOnly` ou `negativeOnly` (pas les deux), `active`. Doublon : `409`. Ni modification ni désactivation après coup.
2. Seuils : `GET /sni/api/v1/inventory/accounting/adjustment-approval-rules` ; création `POST` avec `approvalLevel`, `minAmount` (>= 0), `maxAmount` (vide = sans plafond), `active`. Deux tranches actives ne peuvent pas se chevaucher (`400` `Approval rule overlaps with an existing range starting at N`). Un ajustement dont la valeur tombe dans une tranche, quel que soit son niveau (même `NONE`), exige `approvedBy`.

## Périodes comptables

1. Liste : `GET /sni/api/v1/inventory/accounting/periods` (plus récente d'abord) ; détail `GET .../periods/{periodCode}`.
2. Créer : `POST .../periods` avec `startDate`, `endDate` (fin >= début), `periodCode` facultatif (défaut `AAAA-MM` de la date de début, mis en majuscules). Deux périodes ne peuvent pas se chevaucher. Statut `OPEN`.
3. Clore : `PATCH .../periods/{periodCode}/close?closedBy=`. Refus si déjà close ou si un inventaire non validé a été créé dans la période. La clôture prend une photo de valorisation à la date de fin et enregistre `closingStockValue`.
4. Rouvrir : `PATCH .../periods/{periodCode}/reopen` avec `reason` (obligatoire) et `reopenedBy`.

Effet : seule la période qui couvre **la date du jour** (UTC) compte, car les mouvements sont toujours datés de l'instant présent. Clore une période passée ne bloque donc rien ; clore la période en cours bloque toutes les entrées, sorties, transferts, ajustements, réceptions et validations d'inventaire jusqu'à réouverture. Sans aucune période déclarée, aucun contrôle.

## Photos de valorisation

1. Prendre : `POST /sni/api/v1/inventory/accounting/snapshots?snapshotDate=AAAA-MM-JJ&periodCode=` : une ligne par niveau de stock non nul (`quantityOnHand`, `unitCost` = coût moyen, `totalValue`, `valuationMethod`, `oldestLayerAgeDays` en FIFO). Remplace toute photo existante à cette date. La photo reflète le stock **actuel**, quelle que soit la date indiquée.
2. Consulter : `GET .../snapshots?snapshotDate=` (vide si aucune photo à cette date).

## Journal comptable

1. `GET /sni/api/v1/inventory/accounting/journal?fromDate=&toDate=&periodCode=` : une écriture par mouvement valorisé (hors transferts) : `movementCode`, `stockAccount`, `counterpartAccount`, `direction` (`DEBIT` = le stock augmente, `CREDIT` = il diminue), `amount`, `accountingDate`, `periodCode`, `label`.
2. Export : `GET .../journal.csv` avec les mêmes filtres (fichier `inventory-journal.csv`, colonnes Date, Mouvement, Article, Emplacement, Compte stock, Contrepartie, Sens, Débit, Crédit, Période, Libellé, puis une ligne `TOTAL`).

Comptes utilisés (configuration serveur) : stock `31` ; contrepartie d'un achat (référence `GOODS_RECEIPT` ou `PURCHASE_ORDER`) `601` ; consommation et usage interne `6031` ; casse et perte `6581` ; défaut `6031`. Le compte du motif d'ajustement l'emporte quand il est renseigné. Seule la jambe stock est exportée : le CSV n'est pas équilibré débit-crédit.

## Tableau de bord

`GET /sni/api/v1/inventory/admin/dashboard` : compteurs `openAlerts`, `lowStockAlerts`, `outOfStockAlerts`, `negativeStockAlerts`, `expirySoonAlerts`, `expiringLots30Days`, `outOfStockLevels`, `openPurchaseOrders`, et `totalStockValue`. À rafraîchir à l'ouverture et après chaque action importante ; aucune poussée temps réel.

## Rapports

Filtres de date de ces routes : texte `AAAA-MM-JJ` (début de journée pour `fromDate`, fin de journée pour `toDate`, en UTC) ou date-heure ISO avec `Z`. Une autre forme provoque une erreur `500` : contrôler le format avant l'envoi.

- Mouvements : `GET /sni/api/v1/inventory/admin/reports/movements?itemCode=&locationCode=&fromDate=&toDate=` : `summary` (entrées, sorties, net, en quantité et en valeur), `lines` par type de mouvement, `items` et `locations` (classés par valeur), `recentMovements`. Dates de la réponse déjà formatées en texte `jj/MM/aaaa HH:mm` (UTC). Exports `reports/movements.csv` et `reports/movements.pdf` avec les mêmes filtres.
- Dérogations : `GET .../reports/overrides?referenceType=&fromDate=&toDate=` : mouvements passés avec `allowNegativeOverride`, ventilés par type de référence et auteur. Export `reports/overrides.csv`.
- Anomalies : `GET .../reports/anomalies` : `negativeStockLevels` (niveaux à disponible nul **ou** négatif), `negativeStockAlerts`, `movementsWithNegativeOverride`, `suspiciousAdjustments` (ajustements de plus de 100 unités), `largeInventoryCountVariances` (écarts d'inventaire au-delà de 100 unités). Simples compteurs, sans liste.

## Réconciliation

`GET /sni/api/v1/inventory/admin/reconciliation?itemCode=&locationCode=` compare chaque niveau de stock à la somme de ses mouvements : `pairsChecked`, `divergenceCount`, `totalAbsoluteDifference`, `consistent`, et `divergences[]`. Export `reconciliation.csv`. Rien n'est corrigé : un écart se traite par un ajustement, après analyse. Le worker du lundi 3 h ouvre une alerte `STOCK_LEVEL_DIVERGENCE` si l'ensemble n'est pas cohérent.
