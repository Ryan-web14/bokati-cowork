# inventory : parcours achats et réapprovisionnement

> Suite de [README.md](README.md). Routes exactes et champs JSON : [routes/ProcurementController.md](routes/ProcurementController.md), [routes/InventoryReorderRuleController.md](routes/InventoryReorderRuleController.md), [routes/InventoryAlertController.md](routes/InventoryAlertController.md). Code : `src/main/java/com/sni/bokaticowork/features/inventory/procurement/service/implementation/ProcurementServiceImpl.java`, `intelligence/service/implementation/InventoryReorderRuleServiceImpl.java`.

Circuit complet : règle de réapprovisionnement, suggestion, demande d'achat, approbation, commande, approbations de la commande, commande passée, réception, entrée en stock. Chaque étape peut aussi se faire seule (commande sans demande, réception sans commande).

## Fournisseurs

1. Liste : `GET /sni/api/v1/inventory/procurement/suppliers?q=` (page Spring, tri `name` ; avec `q`, recherche sans tenir compte du tri).
2. Création : `POST .../suppliers` avec `name` (obligatoire), `email`, `phone`, `taxId`, `address`, `status` (défaut `ACTIVE`). Le `supplierCode` est **toujours généré** ; un code envoyé est ignoré.
3. Modification : `PUT .../suppliers/{supplierCode}` (remplacement complet, `status` absent remet `ACTIVE`).
4. Performance : `GET .../suppliers/{supplierCode}/performance` : `purchaseOrderCount`, `receivedOrderCount` (commandes reçues en partie ou en totalité), `completeReceiptCount`, `completeReceiptRate` (0 à 1), `averageDeliveryDelayDays` (moyenne des écarts entre la date de livraison prévue et la réception, négatif = en avance, vide si aucune date prévue), `averageUnitCost`.

Le statut du fournisseur n'est pas contrôlé à la commande : filtrer soi-même les fournisseurs `ACTIVE` (la liste `GET /sni/api/v1/inventory/reference/data` ne contient que ceux-là).

## Règles de réapprovisionnement

1. Liste : `GET /sni/api/v1/inventory/reorder-rules` (non paginée).
2. Créer ou modifier : `POST /sni/api/v1/inventory/reorder-rules` avec `itemCode` (obligatoire), `locationCode` (vide = règle **globale** pour l'article, valable pour chaque emplacement sans règle propre), `minQuantity` (défaut 0), `maxQuantity`, `reorderQuantity`, `preferredSupplierCode`, `active`. Une seule règle par couple article et emplacement : renvoyer le même couple met la règle à jour. Désactiver = renvoyer avec `active: false` (pas de suppression).
3. Défaut de `reorderQuantity` : `max - min` si `max > min`, sinon `min`, sinon 1. Refus : `min` négatif, `max < min`, `reorderQuantity <= 0`.
4. Effets : à chaque mouvement, disponible `<= min` ouvre une alerte `LOW_STOCK` (et une tâche de réapprovisionnement), disponible `> max` ouvre `OVERSTOCK`. Une règle active est aussi la condition de l'alerte de rotation lente.

## Suggestions de réapprovisionnement

`GET /sni/api/v1/inventory/reorder-rules/suggestions?itemCode=&locationCode=&severity=` ([détail](routes/InventoryReorderRuleController.md#r-get-sni-api-v1-inventory-reorder-rules-suggestions)) : liste triée par `priorityScore` décroissant.

- Une ligne par règle active dont le disponible est `<= minQuantity`, ou dont l'emplacement n'a encore aucun niveau de stock (`reasonCode = NO_STOCK_LEVEL`).
- Filtrer par `locationCode` exclut les règles globales.
- Champs utiles pour l'écran : `severity` (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`), `reason` (phrase française), `currentQuantity`, `minQuantity`, `targetQuantity`, `reorderQuantity` (quantité proposée, déduction faite des commandes en cours, majorée de 20 % si la consommation monte de plus de 20 %), `quantityOnOrder`, `daysOfStockRemaining` (selon la consommation des 30 derniers jours), `consumptionTrend`, `recommendedOrderByDate`, `estimatedOrderCost` (avec le coût par défaut de l'article), `preferredSupplierCode`.
- `bestSupplierCode`, `bestSupplierPrice`, `bestSupplierLeadTimeDays` sont toujours vides : aucune route ne renseigne les tarifs fournisseurs.

## Demandes d'achat

1. Créer à la main : `POST /sni/api/v1/inventory/procurement/purchase-requests` avec `lines` (au moins une : `itemCode`, `quantity` positive, `unitCost` estimé), `locationCode`, `requestedBy`. Statut `DRAFT`.
2. Créer depuis les suggestions : `POST .../purchase-requests/from-reorder-suggestions?locationCode=&supplierCode=&requestedBy=` : une ligne par suggestion (quantité = `reorderQuantity`, sans coût estimé). `supplierCode` filtre sur le fournisseur **préféré** des règles. Aucune suggestion : `400` `No reorder suggestion available for the requested filters`.
3. Soumettre : `PATCH .../purchase-requests/{requestCode}/submit` (doit être `DRAFT`).
4. Approuver : `PATCH .../{requestCode}/approve?approvedBy=&supplierCode=` (droit `INVENTORY:APPROVE`, doit être `SUBMITTED`). Avec `supplierCode`, la commande est créée dans la foulée : `autoCreatedOrderCode` dans la réponse, et la demande passe directement `CONVERTED`. Si la création de la commande échoue (fournisseur inconnu), toute l'approbation est annulée.
5. Rejeter : `PATCH .../{requestCode}/reject?reason=` (droit `INVENTORY:UPDATE`, doit être `SUBMITTED`).
6. Transformer en commande plus tard : `POST .../purchase-requests/{requestCode}/purchase-order?supplierCode=` (doit être `APPROVED`) : copie les lignes et coûts estimés, `orderedBy` = approbateur de la demande, demande passée `CONVERTED`.
7. Liste : `GET .../purchase-requests?status=&locationCode=&q=` (page Spring ; `sort` ignoré).

Aucune modification ni annulation de demande : une demande erronée se rejette.

## Commandes d'achat

1. Créer : `POST /sni/api/v1/inventory/procurement/purchase-orders` avec `supplierCode`, `lines` (obligatoires), `locationCode`, `expectedDeliveryDate`, `orderedBy`, `sourceRequestCode`. Statut `DRAFT`.
2. Le serveur calcule `totalAmount` (somme `quantité x unitCost`, lignes sans coût comptées à 0) et `approvalLevel` :
   - selon la première règle active de `GET .../procurement/approval-rules` dont la tranche contient le montant ;
   - sans règle correspondante : `NONE` si 0, `MANAGER` sous 500 000, `DIRECTOR` sous 5 000 000, `EXECUTIVE` au-delà.
3. Approuver : `PATCH .../purchase-orders/{orderCode}/approve?approvedBy=` (droit `INVENTORY:APPROVE`). `approvedBy` obligatoire. Chaque appel ajoute une approbation ; la commande passe `APPROVED` quand le nombre requis est atteint (`requiredApprovals` de la règle du niveau, sinon 1 pour `NONE` et `MANAGER`, 2 pour `DIRECTOR`, 3 pour `EXECUTIVE`). Une même personne ne peut approuver deux fois (`400` `Approver has already approved this purchase order`). La réponse de cet appel est la seule à contenir `approvalSteps` : afficher « 1 approbation sur 2 » à partir de là.
4. Marquer commandée : `PATCH .../{orderCode}/mark-ordered` (doit être `APPROVED`).
5. Liste : `GET .../purchase-orders?status=&supplierCode=&locationCode=&q=` (page Spring ; `sort` ignoré).

Aucune modification ni annulation de commande. Le droit `INVENTORY:APPROVE` étant unique, les niveaux `MANAGER`, `DIRECTOR`, `EXECUTIVE` ne correspondent à aucun rôle : seul le nombre d'approbateurs distincts compte, et `approvedBy` est un texte libre que le frontend doit remplir avec l'identité du compte connecté.

Règles d'approbation : `POST .../procurement/approval-rules` avec `approvalLevel`, `minAmount`, `maxAmount`, `requiredApprovals` (positif), `active`. Une seule règle par niveau (renvoyer le même niveau la remplace). `GET` ne liste que les règles actives.

## Réceptions

`POST /sni/api/v1/inventory/procurement/goods-receipts` ([détail](routes/ProcurementController.md#r-post-sni-api-v1-inventory-procurement-goods-receipts)), avec `Idempotency-Key`.

1. Envoyer `locationCode` (obligatoire), `orderCode` (facultatif : réception sans commande possible), `receivedBy`, codes de pièces (`invoiceDocumentCode`, `deliveryNoteDocumentCode`, `proofDocumentCode`, texte libre normalisé), et `lines` : `itemCode`, `receivedQuantity` (positive), `rejectedQuantity` (0 à `receivedQuantity`), `rejectionReason`, `qualityAccepted` (défaut vrai ; faux = rien n'entre en stock), `unitCost`, `lotNumber`, `expiryDate`, `quarantineAcceptedStock` + `quarantineReason`, `ownershipType` + `ownerCode`.
2. Avec une commande : elle doit être `ORDERED` ou `PARTIALLY_RECEIVED` ; chaque article reçu doit figurer sur la commande ; la quantité acceptée cumulée ne doit pas dépasser la quantité commandée (`400` `Received quantity exceeds ordered quantity`).
3. Pour chaque ligne, la quantité acceptée (`reçue - rejetée`, ou 0 si `qualityAccepted` est faux) entre en stock comme une [entrée](parcours-stock.md#entrée-en-stock) de référence `GOODS_RECEIPT` : mêmes exigences de lot, de péremption et de numéros de série. Les numéros de série ne pouvant pas être transmis ici, un article non `ASSET` avec `requiresSerialNumber` fait échouer toute la réception. Un article `ASSET` crée ses équipements.
4. La commande passe `RECEIVED` si toutes ses lignes sont soldées, sinon `PARTIALLY_RECEIVED`. Chaque ligne de la réponse donne `backorderQuantity` (reste à recevoir).
5. La réception est `POSTED` d'emblée et ses mouvements ne peuvent pas être annulés par la route d'annulation de mouvement.
6. Liste : `GET .../goods-receipts?status=&locationCode=&orderCode=&q=` (page Spring ; `sort` ignoré).

Écran conseillé : préremplir les lignes depuis la commande (quantité restante = commandée moins reçue), demander lot et péremption selon la fiche de chaque article.

## Alertes

1. Liste : `GET /sni/api/v1/inventory/alerts?status=` (page Spring, plus récentes d'abord). Chaque alerte : `alertCode`, `alertType`, `status`, `itemCode`, `locationCode`, `assetCode`, `currentQuantity`, `thresholdQuantity`, `message` (français), dates.
2. Actions : `PATCH .../alerts/{alertCode}/acknowledge`, `.../resolve`, `.../dismiss`. Aucun contrôle de statut : toute action est acceptée sur toute alerte.
3. Prévenir l'utilisateur qu'une alerte « vue » (`ACKNOWLEDGED`) n'est plus résolue automatiquement quand le stock remonte, et qu'une nouvelle alerte du même type peut s'ouvrir à côté. Pour une alerte de stock, préférer laisser `OPEN` ou résoudre.
4. Temps réel : voir le [README](README.md#à-prévoir-côté-frontend) (`/topic/inventory/alerts`, alertes des workers seulement).

Types et origine :

| Type | Créée par | Résolue automatiquement |
| --- | --- | --- |
| `LOW_STOCK`, `RECURRING_LOW_STOCK` | mouvement, disponible `<= min` d'une règle (récurrente à partir de deux stocks bas déjà résolus) | quand le disponible repasse au-dessus de `min` |
| `OUT_OF_STOCK`, `NEGATIVE_STOCK` | mouvement, disponible `<= 0` ou `< 0` | quand le disponible redevient positif |
| `OVERSTOCK` | mouvement, disponible `> max` | quand il redescend |
| `EXPIRY_SOON`, `EXPIRY_IMMINENT` | worker toutes les 2 h | non |
| `SLOW_MOVING` | worker toutes les 6 h | non |
| `WARRANTY_SOON`, `MAINTENANCE_DUE`, `ASSET_RETURN_OVERDUE`, `ASSET_RETURN_DUE_SOON` | worker toutes les 3 h | non |
| `STOCK_LEVEL_DIVERGENCE` | worker du lundi 3 h | non |
| `SUSPICIOUS_ADJUSTMENT` | jamais créée | |
