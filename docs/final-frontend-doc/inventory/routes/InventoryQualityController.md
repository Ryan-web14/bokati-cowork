# InventoryQualityController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [inventory](../README.md) · 15 route(s).

## Routes

<a id="r-get-sni-api-v1-inventory-quality-lots-lotid-inspections"></a>
### GET /sni/api/v1/inventory/quality/lots/{lotId}/inspections

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `List<QualityInspectionResponse>`
- Modèles : [QualityInspectionResponse](#m-QualityInspectionResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/InventoryQualityController.java`, méthode `inspections`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `lotId` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-inventory-quality-lots-lotid-inspections"></a>
### POST /sni/api/v1/inventory/quality/lots/{lotId}/inspections

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_QUALITY_INSPECT`, voir le README racine)
- Corps JSON (obligatoire) : `QualityInspectionRequest`
- Réponse : `QualityInspectionResponse`
- Modèles : [QualityInspectionResponse](#m-QualityInspectionResponse), [QualityInspectionRequest](#m-QualityInspectionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/InventoryQualityController.java`, méthode `inspect`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `lotId` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-inventory-quality-non-conformances"></a>
### GET /sni/api/v1/inventory/quality/non-conformances

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `Page<NonConformanceResponse>`
- Modèles : [NonConformanceResponse](#m-NonConformanceResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/InventoryQualityController.java`, méthode `searchNonConformances`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | requête | `String` |  |  |
| `openOnly` | requête | `Boolean` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-post-sni-api-v1-inventory-quality-non-conformances"></a>
### POST /sni/api/v1/inventory/quality/non-conformances

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_NON_CONFORMANCE_OPEN`, voir le README racine)
- Corps JSON (obligatoire) : `NonConformanceRequest`
- Réponse : `NonConformanceResponse`
- Modèles : [NonConformanceResponse](#m-NonConformanceResponse), [NonConformanceRequest](#m-NonConformanceRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/InventoryQualityController.java`, méthode `openNonConformance`

<a id="r-patch-sni-api-v1-inventory-quality-non-conformances-code-close"></a>
### PATCH /sni/api/v1/inventory/quality/non-conformances/{code}/close

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_NON_CONFORMANCE_CLOSE`, voir le README racine)
- Réponse : `NonConformanceResponse`
- Modèles : [NonConformanceResponse](#m-NonConformanceResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/InventoryQualityController.java`, méthode `closeNonConformance`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `closedBy` | requête | `String` |  |  |
| `correctiveAction` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-inventory-quality-plans"></a>
### GET /sni/api/v1/inventory/quality/plans

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `List<QualityControlPlanResponse>`
- Modèles : [QualityControlPlanResponse](#m-QualityControlPlanResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/InventoryQualityController.java`, méthode `listPlans`

<a id="r-post-sni-api-v1-inventory-quality-plans"></a>
### POST /sni/api/v1/inventory/quality/plans

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_QUALITY_PLAN_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `QualityControlPlanRequest`
- Réponse : `QualityControlPlanResponse`
- Modèles : [QualityControlPlanResponse](#m-QualityControlPlanResponse), [QualityControlPlanRequest](#m-QualityControlPlanRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/InventoryQualityController.java`, méthode `createPlan`

<a id="r-get-sni-api-v1-inventory-quality-plans-plancode"></a>
### GET /sni/api/v1/inventory/quality/plans/{planCode}

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `QualityControlPlanResponse`
- Modèles : [QualityControlPlanResponse](#m-QualityControlPlanResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/InventoryQualityController.java`, méthode `getPlan`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `planCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-inventory-quality-recalls"></a>
### GET /sni/api/v1/inventory/quality/recalls

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `List<ProductRecallResponse>`
- Modèles : [ProductRecallResponse](#m-ProductRecallResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/InventoryQualityController.java`, méthode `listRecalls`

<a id="r-post-sni-api-v1-inventory-quality-recalls"></a>
### POST /sni/api/v1/inventory/quality/recalls

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_RECALL_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `ProductRecallRequest`
- Réponse : `ProductRecallResponse`
- Modèles : [ProductRecallResponse](#m-ProductRecallResponse), [ProductRecallRequest](#m-ProductRecallRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/InventoryQualityController.java`, méthode `createRecall`

<a id="r-get-sni-api-v1-inventory-quality-recalls-recallcode"></a>
### GET /sni/api/v1/inventory/quality/recalls/{recallCode}

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `ProductRecallResponse`
- Modèles : [ProductRecallResponse](#m-ProductRecallResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/InventoryQualityController.java`, méthode `getRecall`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `recallCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-inventory-quality-recalls-recallcode-cancel"></a>
### PATCH /sni/api/v1/inventory/quality/recalls/{recallCode}/cancel

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_RECALL_CANCEL`, voir le README racine)
- Réponse : `ProductRecallResponse`
- Modèles : [ProductRecallResponse](#m-ProductRecallResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/InventoryQualityController.java`, méthode `cancelRecall`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `recallCode` | chemin | `String` | oui |  |
| `cancelledBy` | requête | `String` |  |  |

<a id="r-patch-sni-api-v1-inventory-quality-recalls-recallcode-close"></a>
### PATCH /sni/api/v1/inventory/quality/recalls/{recallCode}/close

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_RECALL_CLOSE`, voir le README racine)
- Réponse : `ProductRecallResponse`
- Modèles : [ProductRecallResponse](#m-ProductRecallResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/InventoryQualityController.java`, méthode `closeRecall`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `recallCode` | chemin | `String` | oui |  |
| `closedBy` | requête | `String` |  |  |

<a id="r-patch-sni-api-v1-inventory-quality-recalls-recallcode-launch"></a>
### PATCH /sni/api/v1/inventory/quality/recalls/{recallCode}/launch

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_RECALL_LAUNCH`, voir le README racine)
- Réponse : `ProductRecallResponse`
- Modèles : [ProductRecallResponse](#m-ProductRecallResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/InventoryQualityController.java`, méthode `launchRecall`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `recallCode` | chemin | `String` | oui |  |
| `launchedBy` | requête | `String` |  |  |

<a id="r-patch-sni-api-v1-inventory-quality-recalls-recallcode-recovery"></a>
### PATCH /sni/api/v1/inventory/quality/recalls/{recallCode}/recovery

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_RECALL_RECOVERY`, voir le README racine)
- Réponse : `ProductRecallResponse`
- Modèles : [ProductRecallResponse](#m-ProductRecallResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/controller/InventoryQualityController.java`, méthode `recordRecovery`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `recallCode` | chemin | `String` | oui |  |
| `quantity` | requête | `BigDecimal` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-QualityInspectionResponse"></a>
#### QualityInspectionResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `inspectionCode` | texte |  |  |
| `planCode` | texte |  |  |
| `itemCode` | texte |  |  |
| `lotId` | entier int64 ⚠ |  |  |
| `lotNumber` | texte |  |  |
| `locationCode` | texte |  |  |
| `sampledQuantity` | nombre décimal |  |  |
| `conformQuantity` | nombre décimal |  |  |
| `nonConformQuantity` | nombre décimal |  |  |
| `decision` | enum : `ACCEPTED`, `REJECTED`, `QUARANTINED`, `ACCEPTED_BY_DEROGATION` |  |  |
| `decisionBy` | texte |  |  |
| `decisionReason` | texte |  |  |
| `inspectedBy` | texte |  |  |
| `inspectedAt` | date-heure ISO 8601 UTC |  |  |
| `nonConformanceCode` | texte |  |  |
| `lotQuarantined` | booléen |  |  |
| `results` | [QualityInspectionResponse.Result](#m-QualityInspectionResponse-Result)[] |  |  |

<a id="m-QualityInspectionRequest"></a>
#### QualityInspectionRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `planCode` | texte |  |  |
| `sampledQuantity` | nombre décimal |  |  |
| `conformQuantity` | nombre décimal |  |  |
| `nonConformQuantity` | nombre décimal |  |  |
| `inspectedBy` | texte |  |  |
| `decisionReason` | texte |  |  |
| `decisionBy` | texte |  |  |
| `acceptByDerogation` | booléen |  |  |
| `measures` | [QualityInspectionRequest.Measure](#m-QualityInspectionRequest-Measure)[] | oui | éléments min `1` |

<a id="m-NonConformanceResponse"></a>
#### NonConformanceResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `nonConformanceCode` | texte |  |  |
| `itemCode` | texte |  |  |
| `lotId` | entier int64 ⚠ |  |  |
| `lotNumber` | texte |  |  |
| `sourceType` | texte |  |  |
| `sourceCode` | texte |  |  |
| `quantity` | nombre décimal |  |  |
| `severity` | enum : `MINOR`, `MAJOR`, `CRITICAL` |  |  |
| `description` | texte |  |  |
| `disposition` | enum : `USE_AS_IS`, `REWORK`, `RETURN_TO_SUPPLIER`, `SCRAP`, `DOWNGRADE`, `PENDING` |  |  |
| `correctiveAction` | texte |  |  |
| `responsibleCode` | texte |  |  |
| `dueDate` | date AAAA-MM-JJ |  |  |
| `supplierClaimCode` | texte |  |  |
| `costImpact` | entier int64 ⚠ |  |  |
| `detectedBy` | texte |  |  |
| `detectedAt` | date-heure ISO 8601 UTC |  |  |
| `closedBy` | texte |  |  |
| `closedAt` | date-heure ISO 8601 UTC |  |  |
| `open` | booléen |  |  |

<a id="m-NonConformanceRequest"></a>
#### NonConformanceRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCode` | texte | oui | longueur min `1` |
| `lotId` | entier int64 ⚠ |  |  |
| `quantity` | nombre décimal |  |  |
| `severity` | enum : `MINOR`, `MAJOR`, `CRITICAL` | oui |  |
| `description` | texte | oui | longueur min `1` |
| `disposition` | enum : `USE_AS_IS`, `REWORK`, `RETURN_TO_SUPPLIER`, `SCRAP`, `DOWNGRADE`, `PENDING` |  |  |
| `correctiveAction` | texte |  |  |
| `responsibleCode` | texte |  |  |
| `dueDate` | date AAAA-MM-JJ |  |  |
| `supplierClaimCode` | texte |  |  |
| `costImpact` | entier int64 ⚠ |  |  |
| `detectedBy` | texte |  |  |

<a id="m-QualityControlPlanResponse"></a>
#### QualityControlPlanResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `planCode` | texte |  |  |
| `name` | texte |  |  |
| `itemCode` | texte |  |  |
| `categoryCode` | texte |  |  |
| `controlStage` | enum : `ON_RECEIPT`, `IN_STORAGE`, `BEFORE_ISSUE`, `ON_DEMAND` |  |  |
| `samplingMode` | enum : `FULL`, `FIXED_QUANTITY`, `PERCENTAGE` |  |  |
| `samplingParameter` | nombre décimal |  |  |
| `decisionOnFail` | enum : `ACCEPTED`, `REJECTED`, `QUARANTINED`, `ACCEPTED_BY_DEROGATION` |  |  |
| `quarantineOnReceipt` | booléen |  |  |
| `active` | booléen |  |  |
| `criteria` | [QualityControlPlanResponse.Criterion](#m-QualityControlPlanResponse-Criterion)[] |  |  |

<a id="m-QualityControlPlanRequest"></a>
#### QualityControlPlanRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte | oui | longueur min `1` |
| `itemCode` | texte |  |  |
| `categoryCode` | texte |  |  |
| `controlStage` | enum : `ON_RECEIPT`, `IN_STORAGE`, `BEFORE_ISSUE`, `ON_DEMAND` | oui |  |
| `samplingMode` | enum : `FULL`, `FIXED_QUANTITY`, `PERCENTAGE` | oui |  |
| `samplingParameter` | nombre décimal |  |  |
| `decisionOnFail` | enum : `ACCEPTED`, `REJECTED`, `QUARANTINED`, `ACCEPTED_BY_DEROGATION` |  |  |
| `quarantineOnReceipt` | booléen |  |  |
| `active` | booléen |  |  |
| `criteria` | [QualityControlPlanRequest.Criterion](#m-QualityControlPlanRequest-Criterion)[] | oui | éléments min `1` |

<a id="m-ProductRecallResponse"></a>
#### ProductRecallResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `recallCode` | texte |  |  |
| `itemCode` | texte |  |  |
| `lotNumberFrom` | texte |  |  |
| `lotNumberTo` | texte |  |  |
| `status` | enum : `DRAFT`, `ACTIVE`, `CLOSED`, `CANCELLED` |  |  |
| `reason` | texte |  |  |
| `frozenLotCount` | entier |  |  |
| `quantityInStock` | nombre décimal |  |  |
| `quantityIssued` | nombre décimal |  |  |
| `quantityRecovered` | nombre décimal |  |  |
| `recoveryRate` | nombre décimal |  |  |
| `launchedBy` | texte |  |  |
| `launchedAt` | date-heure ISO 8601 UTC |  |  |
| `closedBy` | texte |  |  |
| `closedAt` | date-heure ISO 8601 UTC |  |  |
| `affectedLotNumbers` | texte[] |  |  |
| `holders` | [ProductRecallResponse.Holder](#m-ProductRecallResponse-Holder)[] |  |  |

<a id="m-ProductRecallRequest"></a>
#### ProductRecallRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCode` | texte | oui | longueur min `1` |
| `lotNumberFrom` | texte |  |  |
| `lotNumberTo` | texte |  |  |
| `reason` | texte | oui | longueur min `1` |

<a id="m-QualityInspectionResponse-Result"></a>
#### QualityInspectionResponse.Result

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `criterionCode` | texte |  |  |
| `criterionName` | texte |  |  |
| `measuredValue` | nombre décimal |  |  |
| `measuredFlag` | booléen |  |  |
| `conform` | booléen |  |  |
| `blocking` | booléen |  |  |
| `notes` | texte |  |  |

<a id="m-QualityInspectionRequest-Measure"></a>
#### QualityInspectionRequest.Measure

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `criterionCode` | texte |  |  |
| `measuredValue` | nombre décimal |  |  |
| `measuredFlag` | booléen |  |  |
| `notes` | texte |  |  |

<a id="m-QualityControlPlanResponse-Criterion"></a>
#### QualityControlPlanResponse.Criterion

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `criterionCode` | texte |  |  |
| `name` | texte |  |  |
| `unit` | texte |  |  |
| `minValue` | nombre décimal |  |  |
| `maxValue` | nombre décimal |  |  |
| `booleanExpected` | booléen |  |  |
| `blocking` | booléen |  |  |
| `position` | entier |  |  |

<a id="m-QualityControlPlanRequest-Criterion"></a>
#### QualityControlPlanRequest.Criterion

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `criterionCode` | texte | oui | longueur min `1` |
| `name` | texte | oui | longueur min `1` |
| `unit` | texte |  |  |
| `minValue` | nombre décimal |  |  |
| `maxValue` | nombre décimal |  |  |
| `booleanExpected` | booléen |  |  |
| `blocking` | booléen |  |  |
| `position` | entier |  |  |

<a id="m-ProductRecallResponse-Holder"></a>
#### ProductRecallResponse.Holder

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `lotNumber` | texte |  |  |
| `movementCode` | texte |  |  |
| `locationCode` | texte |  |  |
| `quantity` | nombre décimal |  |  |
| `referenceType` | texte |  |  |
| `referenceCode` | texte |  |  |
| `issuedAt` | date-heure ISO 8601 UTC |  |  |
