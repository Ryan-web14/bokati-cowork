# inventory : parcours du catalogue

> Suite de [README.md](README.md). Routes exactes et champs JSON : [routes.md](routes.md). Toutes ces routes sont back-office, droit `INVENTORY:*`.

## Listes de référence

À charger à l'ouverture du module, avant tout formulaire.

1. `GET /sni/api/v1/inventory/reference/enums` ([détail](routes/InventoryReferenceController.md#r-get-sni-api-v1-inventory-reference-enums)) : un objet dont chaque clé est un groupe (`itemTypes`, `trackingTypes`, `unitTypes`, `lifecycleStatuses`, `barcodeTypes`, `packagingLevels`, `locationTypes`, `movementTypes`, `stockOutReasons`, `ownershipTypes`, `referenceTypes`, `reservationStatuses`, `transferStatuses`, `serialStatuses`, `valuationMethods`, `periodStatuses`, `lotBlockReasonTypes`, `qualityControlStages`, `qualitySamplingModes`, `qualityDecisions`, `nonConformanceSeverities`, `nonConformanceDispositions`, `productRecallStatuses`, `assetStatuses`, `assetConditions`, `assetAssigneeTypes`, `assetAssignmentStatuses`, `maintenanceTypes`, `maintenanceStatuses`, `countStatuses`, `importTypes`, `supplierStatuses`, `purchaseRequestStatuses`, `purchaseOrderStatuses`, `goodsReceiptStatuses`, `approvalLevels`, `alertTypes`, `alertStatuses`, `consumptionTrends`, `reorderReasons`, `reorderSeverities`…). Chaque valeur est `{ value, label, description, selectable }` : envoyer `value`, afficher `label` (français). Réponse cachable une heure (`Cache-Control: public, max-age=3600`).
2. Pour un seul groupe : `GET /sni/api/v1/inventory/reference/enums/{group}` (nom exact, sensible à la casse ; groupe inconnu : `404` avec la liste des groupes). Liste des noms : `GET /sni/api/v1/inventory/reference/enums/groups`.
3. `GET /sni/api/v1/inventory/reference/data` : `units`, `categories`, `locations` (seulement les actifs) et `suppliers` (seulement `ACTIVE`). `value` est le code, `label` le nom, `description` le type d'unité, la catégorie parente, le type d'emplacement ou le téléphone du fournisseur. Non cachable : recharger après toute création ou désactivation.

Ces listes ne contiennent pas les éléments inactifs : pour afficher un article rattaché à une catégorie désactivée, utiliser `categoryName` de la réponse article.

## Catégories

1. Liste : `GET /sni/api/v1/inventory/categories?active=true|false` (non paginée).
2. Création : `POST /sni/api/v1/inventory/categories` avec `name` (obligatoire), `code` (facultatif, sinon dérivé du nom), `description`, `parentCategoryCode`. Le code est mis en majuscules, caractères spéciaux remplacés par `_` (`Fournitures bureau` donne `FOURNITURES_BUREAU`). Doublon : `409` `Inventory category already exists`.
3. Modification : `PUT /sni/api/v1/inventory/categories/{code}` (même corps). `PATCH .../activate` et `.../deactivate` basculent `active`.

Attention au filtre par catégorie des listes d'articles et de niveaux : un code contenant `_` ne trouve rien (écart 7 du [README](README.md#écarts-relevés)).

## Unités

Même schéma que les catégories : `GET`, `POST`, `PUT`, `PATCH activate|deactivate` sur `/sni/api/v1/inventory/units`. `name` et `unitType` obligatoires ; code fourni ou dérivé du nom, séparateur `_`.

## Emplacements

1. Liste : `GET /sni/api/v1/inventory/locations?active=` (non paginée, triée par nom). Construire l'arbre à partir du code parent de chaque réponse.
2. Création : `POST /sni/api/v1/inventory/locations` avec `name`, `locationType` (obligatoires), `parentLocationCode`, `description`, `businessCode`. **Le code est généré** (séquence suivie de `-` et de millisecondes) ; le récupérer dans la réponse.
3. Modification : `PUT /sni/api/v1/inventory/locations/{locationCode}`. Désactivation : `PATCH .../deactivate`. Un emplacement inactif reste accepté par les mouvements de stock : masquer côté écran les emplacements inactifs dans les listes de choix (`reference/data` le fait déjà).

## Articles

### Lister et rechercher

`GET /sni/api/v1/inventory/items` ([détail](routes/InventoryItemController.md#r-get-sni-api-v1-inventory-items)) avec :

- texte libre dans `q` (ou `query`, `searchText`, `name` : le premier non vide est retenu) : recherche approchée (trigrammes) sur code, nom, description, codes d'identification, spécification, catégorie. Avec texte, le tri `sort` est ignoré.
- filtres `categoryCode`, `itemType`, `active`.
- page Spring, 20 par défaut, tri `name`.

### Créer

`POST /sni/api/v1/inventory/items` ([détail](routes/InventoryItemController.md#r-post-sni-api-v1-inventory-items)), `201`.

- Obligatoires : `name`, `itemType`.
- `trackingType` : par défaut `SERIAL` pour `ASSET`, `QUANTITY` sinon. `LOT` ou `EXPIRY` imposent un numéro de lot ou une date de péremption à chaque entrée ; `requiresLotNumber` et `requiresExpiryDate` ont le même effet.
- `requiresSerialNumber` : par défaut `true` pour `ASSET`. Pour un article **non** `ASSET`, il impose une liste de numéros de série à chaque entrée (autant que d'unités), sortie et transfert.
- `allowNegativeStock` : autorise les sorties au-delà du disponible sans dérogation.
- `psku` : 12 chiffres exactement, sinon généré. `identificationCode` facultatif (sinon égal au `displayCode`).
- `lifecycleStatus` prime sur `active` ; `active: false` seul donne `OBSOLETE`.
- `defaultCost`, `salePrice` : entiers. Leur première valeur est inscrite dans l'historique des prix.
- Ne pas envoyer `itemCode`, `shortCode`, `displayCode` : générés. Doublon de `psku` ou de code d'identification : `409`.

La réponse donne `itemCode` (à garder pour toutes les routes suivantes), `shortCode`, `displayCode`.

### Modifier

`PUT /sni/api/v1/inventory/items/{itemCode}` : **remplacement complet** (voir le [README](README.md#à-prévoir-côté-frontend)). Un changement de `defaultCost` ou `salePrice` ajoute une ligne à l'historique (`GET .../price-history`, page Spring, plus récent d'abord ; `changedBy` et `reason` y sont toujours vides).

### Cycle de vie

- `PATCH .../lifecycle` avec `{ "lifecycleStatus": "PHASE_OUT", "reason": "..." }`. Toute transition est permise.
- `PATCH .../activate` (met `ACTIVE`), `PATCH .../deactivate` (met `OBSOLETE`).
- Afficher l'effet : `PHASE_OUT` bloque les entrées, `OBSOLETE`, `BLOCKED`, `DRAFT` bloquent entrées et sorties (l'ajustement reste possible).

### Révision technique

`PATCH .../revision` avec `revision` (obligatoire, différente de l'actuelle, sinon `400` `Item is already on revision X`), `reason`, `documentCode`, `changedBy`. Historique : `GET .../revision-history` (liste, plus récent d'abord).

### Méthode de valorisation

`PATCH .../valuation-method` avec `valuationMethod` (`WEIGHTED_AVERAGE` ou `FIFO`), `reason`, `changedBy`. Réponse : un **entier**, le nombre de couches de coût amorcées (une par emplacement portant du stock, au coût moyen du moment). Voir [parcours-inventaire-comptabilite.md](parcours-inventaire-comptabilite.md#valorisation).

### Supprimer

`DELETE /sni/api/v1/inventory/items/{itemCode}` : suppression physique, `204`. Un article déjà utilisé (niveaux, mouvements, lots…) ne peut probablement pas être supprimé et la base renvoie une erreur traitée en `500` (non vérifié en base). Proposer plutôt `deactivate` ; réserver la suppression aux articles créés par erreur.

## Codes-barres

1. `GET /sni/api/v1/inventory/items/{itemCode}/barcodes` : liste, le principal en premier.
2. `POST .../barcodes` avec `barcodeType`, `barcodeValue` (obligatoires), `unitCode`, `quantity` (contenu d'un scan, défaut 1), `primaryCode`, `active`. Valeur mise en majuscules. Le premier code d'un article devient principal d'office. Contrôle de longueur pour `EAN13` (13 chiffres), `EAN8` (8), `UPC` (12). Valeur déjà utilisée par n'importe quel article ou conditionnement : `409`.
3. `PATCH /sni/api/v1/inventory/items/barcodes/{barcodeId}/primary` et `DELETE /sni/api/v1/inventory/items/barcodes/{barcodeId}` : `barcodeId` est l'`id` ⚠ de la réponse, à garder en texte.

## Conditionnements

`POST .../packagings` avec `packagingLevel` (`EACH`, `INNER`, `CASE`, `PALLET`), `quantity` (unités de base contenues, positive), `name`, `barcodeValue` facultatif (unique avec les codes-barres), dimensions. Un seul conditionnement par niveau : `409` sinon. Liste : `GET .../packagings` (par quantité croissante). Suppression par `packagingId` ⚠.

## Substituts

`POST .../substitutes` avec `substituteItemCode`, `priority` (défaut : à la suite), `conversionFactor` (défaut 1), `bidirectional`, `notes`. Un article ne peut pas se substituer à lui-même ; doublon : `409`. `GET .../substitutes` renvoie aussi les liens déclarés réciproques depuis l'autre article, marqués `inherited: true` ; leur `id` est celui du lien d'origine, donc les supprimer supprime le lien des deux côtés. Suppression : `DELETE /sni/api/v1/inventory/items/substitutes/{substituteId}`.

## Traductions

`PUT .../translations` avec `languageCode` (8 caractères maximum, mis en minuscules), `name`, `description` : crée ou remplace la traduction de cette langue. Liste : `GET .../translations`. Suppression par `translationId` ⚠.

## Modèles et variantes

1. Créer le modèle : `POST /sni/api/v1/inventory/item-templates` avec `name`, `itemType`, `axes` (au moins un) ; chaque axe a `axisCode`, `name`, `values` (au moins une, chacune `valueCode`, `label`). `templateCode` généré. Codes d'axe ou de valeur en double : `400`.
2. Simuler : `POST .../item-templates/{templateCode}/variants` avec `{ "dryRun": true, "selectedValues": { "TAILLE": ["S","M"] } }`. Sans `selectedValues`, toutes les valeurs actives de chaque axe. Réponse : `requestedCombinations`, et pour chaque combinaison `name`, `variantSignature`, `alreadyExisted`.
3. Générer : même appel avec `dryRun: false` (ou corps vide). Les variantes déjà existantes (même signature) sont ignorées (`skippedCount`), les autres créées comme articles normaux (`createdCount`, `itemCode` rempli). Plus de 500 combinaisons : `400`, restreindre la sélection.
4. `PUT .../item-templates/{templateCode}` remplace tous les axes ; les variantes déjà créées restent des articles autonomes.

## Scan

`GET /sni/api/v1/inventory/scan/{code}` ([détail](routes/InventoryScanController.md#r-get-sni-api-v1-inventory-scan-code)) identifie un code lu par une douchette ou une caméra. Ordre de recherche : code-barres, code de conditionnement, codes de l'article, emplacement, équipement (code, étiquette, numéro de série), numéro de série, lot. Réponse :

- `resolved`, `resolvedType` (`BARCODE`, `PACKAGING`, `ITEM`, `LOCATION`, `ASSET`, `SERIAL`, `LOT` ou `UNKNOWN`), `code`, `label`, `itemCode`, `locationCode`, `quantityPerScan` (contenu d'un scan : 1, quantité du code-barres ou du carton, ou quantité restante du lot), `unitCode`.
- `suggestedActions` : boutons à proposer, parmi `ITEM_DETAIL`, `STOCK_IN`, `STOCK_OUT`, `STOCK_TRANSFER`, `STOCK_LEVELS`, `LOCATION_DETAIL`, `START_COUNT`, `ASSET_DETAIL`, `ASSIGN`, `RETURN`, `MAINTENANCE`, `LOCATION_HISTORY`, `LOT_DETAIL`, `CREATE_ITEM`, `ASSIGN_BARCODE`, `SEARCH`. Les actions d'entrée et de sortie disparaissent selon le cycle de vie de l'article.
- `message` : avertissement en français à afficher (article en fin de série, code-barres désactivé, lot en quarantaine, plusieurs correspondances…).

Un code inconnu n'est pas une erreur (`200`, `resolved: false`) : proposer de créer l'article ou de rattacher le code à un article existant.

## Étiquettes

`GET /sni/api/v1/inventory/admin/labels/{type}/{code}` (`type` = `ITEM`, `LOCATION`, `ASSET`, `DISPLAY`) ou `POST /sni/api/v1/inventory/admin/labels` avec `{ "items": [ { "type": "ITEM", "code": "..." } ] }`. Réponse : `labelType`, `code`, `displayText`, `barcodeValue`, `qrValue` (`<TYPE>:<valeur>`). Le serveur ne produit **pas** d'image : le frontend dessine le code-barres et le QR à partir de ces valeurs. Une étiquette inconnue fait échouer tout le lot (`404`).

## Conversions d'unités

1. `POST /sni/api/v1/inventory/stock/planning/unit-conversions` avec `itemCode`, `fromUnitCode`, `toUnitCode` (différents), `factor` (positif) : crée ou remplace la conversion de cet article. Les codes d'unité ne sont pas vérifiés contre la liste des unités et sont normalisés avec `-`.
2. `GET .../unit-conversions/{itemCode}` : conversions actives.
3. `GET .../unit-conversions/{itemCode}/convert?fromUnitCode=&toUnitCode=&quantity=` : renvoie un nombre, `quantity x factor`. Pas de conversion inverse automatique : sans conversion déclarée dans ce sens, `400` `Unit conversion not configured`.

## Import

`POST /sni/api/v1/inventory/import/{type}` ([détail](routes/InventoryImportController.md#r-post-sni-api-v1-inventory-import-type)), `multipart/form-data`, partie `file`, paramètre `dryRun` (défaut `false`). `type` : `ITEMS`, `INITIAL_STOCK`, `SUPPLIERS`, `ASSETS`.

- Fichier `.csv` (UTF-8, séparateur virgule, guillemets doubles) ou `.xlsx` (première feuille). L'extension du nom de fichier décide.
- Première ligne = en-têtes, mis en minuscules, caractères spéciaux remplacés par `_` (`Item Code` devient `item_code`).
- Colonnes : `ITEMS` : `name` (obligatoire), `description`, `psku`, `identification_code`, `specification`, `category_code`, `unit_code`, `item_type`, `tracking_type`, `default_cost`, `sale_price`, `taxable`, `allow_negative_stock`, `requires_expiry_date`, `requires_lot_number`, `requires_serial_number`, `active`. `INITIAL_STOCK` : `item_code`, `location_code`, `quantity` (obligatoires), `unit_cost`, `lot_number`, `expiry_date`, `reference_code`, `reason`, `performed_by`. `SUPPLIERS` : `name` (obligatoire), `email`, `phone`, `tax_id`, `address`, `status`. `ASSETS` : `item_code` (obligatoire), `serial_number`, `asset_tag`, `status`, `condition`, `location_code`, `purchase_date`, `purchase_cost`, `warranty_end_date`, `notes`.
- Booléens : `true`, `1`, `yes`, `y`, `oui` ou `false`, `0`, `no`, `n`, `non`. Dates `AAAA-MM-JJ`. Décimaux avec point ou virgule.
- Réponse : `totalRows`, `successCount`, `errorCount`, et `rows[]` avec `rowNumber` (2 = première ligne de données), `success`, `referenceCode` (code créé), `message` (erreur brute, en anglais).
- `dryRun: true` ne vérifie que la présence des colonnes obligatoires et le format des valeurs, pas les règles métier (unicité, existence de la catégorie…).
- Faire toujours un `dryRun` d'abord, puis l'import réel. Voir l'écart 11 du [README](README.md#écarts-relevés) : une erreur métier sur une ligne peut faire échouer l'import entier.
