# ProcurementController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [inventory](../README.md) · 19 route(s).

## Routes

<a id="r-get-sni-api-v1-inventory-procurement-approval-rules"></a>
### GET /sni/api/v1/inventory/procurement/approval-rules

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `List<ProcurementDtos.PurchaseApprovalRuleResponse>`
- Modèles : [ProcurementDtos.PurchaseApprovalRuleResponse](#m-ProcurementDtos-PurchaseApprovalRuleResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/procurement/controller/ProcurementController.java`, méthode `approvalRules`

<a id="r-post-sni-api-v1-inventory-procurement-approval-rules"></a>
### POST /sni/api/v1/inventory/procurement/approval-rules

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- Corps JSON (obligatoire) : `ProcurementDtos.PurchaseApprovalRuleRequest`
- Réponse : `ProcurementDtos.PurchaseApprovalRuleResponse`
- Modèles : [ProcurementDtos.PurchaseApprovalRuleResponse](#m-ProcurementDtos-PurchaseApprovalRuleResponse), [ProcurementDtos.PurchaseApprovalRuleRequest](#m-ProcurementDtos-PurchaseApprovalRuleRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/procurement/controller/ProcurementController.java`, méthode `saveApprovalRule`

<a id="r-get-sni-api-v1-inventory-procurement-goods-receipts"></a>
### GET /sni/api/v1/inventory/procurement/goods-receipts

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `Page<ProcurementDtos.GoodsReceiptResponse>`
- Modèles : [ProcurementDtos.GoodsReceiptResponse](#m-ProcurementDtos-GoodsReceiptResponse), [GoodsReceiptStatus](#m-GoodsReceiptStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/procurement/controller/ProcurementController.java`, méthode `goodsReceipts`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `status` | requête | `GoodsReceiptStatus` |  |  |
| `locationCode` | requête | `String` |  |  |
| `orderCode` | requête | `String` |  |  |
| `q` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=receivedAt,ASC |

<a id="r-post-sni-api-v1-inventory-procurement-goods-receipts"></a>
### POST /sni/api/v1/inventory/procurement/goods-receipts

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_GOODS_RECEIPT_POST`, voir le README racine)
- Corps JSON (obligatoire) : `ProcurementDtos.GoodsReceiptRequest`
- Réponse : `ProcurementDtos.GoodsReceiptResponse`
- Modèles : [ProcurementDtos.GoodsReceiptResponse](#m-ProcurementDtos-GoodsReceiptResponse), [ProcurementDtos.GoodsReceiptRequest](#m-ProcurementDtos-GoodsReceiptRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/procurement/controller/ProcurementController.java`, méthode `receiveGoods`

<a id="r-get-sni-api-v1-inventory-procurement-purchase-orders"></a>
### GET /sni/api/v1/inventory/procurement/purchase-orders

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `Page<ProcurementDtos.PurchaseOrderResponse>`
- Modèles : [ProcurementDtos.PurchaseOrderResponse](#m-ProcurementDtos-PurchaseOrderResponse), [PurchaseOrderStatus](#m-PurchaseOrderStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/procurement/controller/ProcurementController.java`, méthode `purchaseOrders`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `status` | requête | `PurchaseOrderStatus` |  |  |
| `supplierCode` | requête | `String` |  |  |
| `locationCode` | requête | `String` |  |  |
| `q` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,ASC |

<a id="r-post-sni-api-v1-inventory-procurement-purchase-orders"></a>
### POST /sni/api/v1/inventory/procurement/purchase-orders

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_PURCHASE_ORDER_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `ProcurementDtos.PurchaseOrderCreateRequest`
- Réponse : `ProcurementDtos.PurchaseOrderResponse`
- Modèles : [ProcurementDtos.PurchaseOrderResponse](#m-ProcurementDtos-PurchaseOrderResponse), [ProcurementDtos.PurchaseOrderCreateRequest](#m-ProcurementDtos-PurchaseOrderCreateRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/procurement/controller/ProcurementController.java`, méthode `createPurchaseOrder`

<a id="r-patch-sni-api-v1-inventory-procurement-purchase-orders-ordercode-approve"></a>
### PATCH /sni/api/v1/inventory/procurement/purchase-orders/{orderCode}/approve

- Accès : back-office : rôle d'administration et droit `INVENTORY:APPROVE`
- Réponse : `ProcurementDtos.PurchaseOrderResponse`
- Modèles : [ProcurementDtos.PurchaseOrderResponse](#m-ProcurementDtos-PurchaseOrderResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/procurement/controller/ProcurementController.java`, méthode `approvePurchaseOrder`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `orderCode` | chemin | `String` | oui |  |
| `approvedBy` | requête | `String` |  |  |

<a id="r-patch-sni-api-v1-inventory-procurement-purchase-orders-ordercode-mark-ordered"></a>
### PATCH /sni/api/v1/inventory/procurement/purchase-orders/{orderCode}/mark-ordered

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- Réponse : `ProcurementDtos.PurchaseOrderResponse`
- Modèles : [ProcurementDtos.PurchaseOrderResponse](#m-ProcurementDtos-PurchaseOrderResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/procurement/controller/ProcurementController.java`, méthode `markOrdered`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `orderCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-inventory-procurement-purchase-requests"></a>
### GET /sni/api/v1/inventory/procurement/purchase-requests

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `Page<ProcurementDtos.PurchaseRequestResponse>`
- Modèles : [ProcurementDtos.PurchaseRequestResponse](#m-ProcurementDtos-PurchaseRequestResponse), [PurchaseRequestStatus](#m-PurchaseRequestStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/procurement/controller/ProcurementController.java`, méthode `purchaseRequests`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `status` | requête | `PurchaseRequestStatus` |  |  |
| `locationCode` | requête | `String` |  |  |
| `q` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,ASC |

<a id="r-post-sni-api-v1-inventory-procurement-purchase-requests"></a>
### POST /sni/api/v1/inventory/procurement/purchase-requests

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_PURCHASE_REQUEST_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `ProcurementDtos.PurchaseRequestCreateRequest`
- Réponse : `ProcurementDtos.PurchaseRequestResponse`
- Modèles : [ProcurementDtos.PurchaseRequestResponse](#m-ProcurementDtos-PurchaseRequestResponse), [ProcurementDtos.PurchaseRequestCreateRequest](#m-ProcurementDtos-PurchaseRequestCreateRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/procurement/controller/ProcurementController.java`, méthode `createPurchaseRequest`

<a id="r-post-sni-api-v1-inventory-procurement-purchase-requests-from-reorder-suggestions"></a>
### POST /sni/api/v1/inventory/procurement/purchase-requests/from-reorder-suggestions

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_PURCHASE_REQUEST_FROM_REORDER`, voir le README racine)
- Réponse : `ProcurementDtos.PurchaseRequestResponse`
- Modèles : [ProcurementDtos.PurchaseRequestResponse](#m-ProcurementDtos-PurchaseRequestResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/procurement/controller/ProcurementController.java`, méthode `createPurchaseRequestFromReorderSuggestions`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `locationCode` | requête | `String` |  |  |
| `supplierCode` | requête | `String` |  |  |
| `requestedBy` | requête | `String` |  |  |

<a id="r-patch-sni-api-v1-inventory-procurement-purchase-requests-requestcode-approve"></a>
### PATCH /sni/api/v1/inventory/procurement/purchase-requests/{requestCode}/approve

- Accès : back-office : rôle d'administration et droit `INVENTORY:APPROVE`
- Réponse : `ProcurementDtos.PurchaseRequestResponse`
- Modèles : [ProcurementDtos.PurchaseRequestResponse](#m-ProcurementDtos-PurchaseRequestResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/procurement/controller/ProcurementController.java`, méthode `approvePurchaseRequest`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `requestCode` | chemin | `String` | oui |  |
| `approvedBy` | requête | `String` |  |  |
| `supplierCode` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-inventory-procurement-purchase-requests-requestcode-purchase-order"></a>
### POST /sni/api/v1/inventory/procurement/purchase-requests/{requestCode}/purchase-order

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- Réponse : `ProcurementDtos.PurchaseOrderResponse`
- Modèles : [ProcurementDtos.PurchaseOrderResponse](#m-ProcurementDtos-PurchaseOrderResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/procurement/controller/ProcurementController.java`, méthode `createPurchaseOrderFromRequest`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `requestCode` | chemin | `String` | oui |  |
| `supplierCode` | requête | `String` | oui |  |

<a id="r-patch-sni-api-v1-inventory-procurement-purchase-requests-requestcode-reject"></a>
### PATCH /sni/api/v1/inventory/procurement/purchase-requests/{requestCode}/reject

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- Réponse : `ProcurementDtos.PurchaseRequestResponse`
- Modèles : [ProcurementDtos.PurchaseRequestResponse](#m-ProcurementDtos-PurchaseRequestResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/procurement/controller/ProcurementController.java`, méthode `rejectPurchaseRequest`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `requestCode` | chemin | `String` | oui |  |
| `reason` | requête | `String` |  |  |

<a id="r-patch-sni-api-v1-inventory-procurement-purchase-requests-requestcode-submit"></a>
### PATCH /sni/api/v1/inventory/procurement/purchase-requests/{requestCode}/submit

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- Réponse : `ProcurementDtos.PurchaseRequestResponse`
- Modèles : [ProcurementDtos.PurchaseRequestResponse](#m-ProcurementDtos-PurchaseRequestResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/procurement/controller/ProcurementController.java`, méthode `submitPurchaseRequest`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `requestCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-inventory-procurement-suppliers"></a>
### GET /sni/api/v1/inventory/procurement/suppliers

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `Page<ProcurementDtos.SupplierResponse>`
- Modèles : [ProcurementDtos.SupplierResponse](#m-ProcurementDtos-SupplierResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/procurement/controller/ProcurementController.java`, méthode `suppliers`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `q` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=name,ASC |

<a id="r-post-sni-api-v1-inventory-procurement-suppliers"></a>
### POST /sni/api/v1/inventory/procurement/suppliers

- Accès : back-office : rôle d'administration et droit `INVENTORY:CREATE`
- En-tête `Idempotency-Key` : conseillé (opération `INVENTORY_SUPPLIER_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `ProcurementDtos.SupplierRequest`
- Réponse : `ProcurementDtos.SupplierResponse`
- Modèles : [ProcurementDtos.SupplierResponse](#m-ProcurementDtos-SupplierResponse), [ProcurementDtos.SupplierRequest](#m-ProcurementDtos-SupplierRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/procurement/controller/ProcurementController.java`, méthode `createSupplier`

<a id="r-put-sni-api-v1-inventory-procurement-suppliers-suppliercode"></a>
### PUT /sni/api/v1/inventory/procurement/suppliers/{supplierCode}

- Accès : back-office : rôle d'administration et droit `INVENTORY:UPDATE`
- Corps JSON (obligatoire) : `ProcurementDtos.SupplierRequest`
- Réponse : `ProcurementDtos.SupplierResponse`
- Modèles : [ProcurementDtos.SupplierResponse](#m-ProcurementDtos-SupplierResponse), [ProcurementDtos.SupplierRequest](#m-ProcurementDtos-SupplierRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/procurement/controller/ProcurementController.java`, méthode `updateSupplier`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `supplierCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-inventory-procurement-suppliers-suppliercode-performance"></a>
### GET /sni/api/v1/inventory/procurement/suppliers/{supplierCode}/performance

- Accès : back-office : rôle d'administration et droit `INVENTORY:READ`
- Réponse : `ProcurementDtos.SupplierPerformanceResponse`
- Modèles : [ProcurementDtos.SupplierPerformanceResponse](#m-ProcurementDtos-SupplierPerformanceResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/inventory/procurement/controller/ProcurementController.java`, méthode `supplierPerformance`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `supplierCode` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ProcurementDtos-PurchaseApprovalRuleResponse"></a>
#### ProcurementDtos.PurchaseApprovalRuleResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `approvalLevel` | enum : `NONE`, `MANAGER`, `DIRECTOR`, `EXECUTIVE` |  |  |
| `minAmount` | entier int64 ⚠ |  |  |
| `maxAmount` | entier int64 ⚠ |  |  |
| `requiredApprovals` | entier |  |  |
| `active` | booléen |  |  |

<a id="m-ProcurementDtos-PurchaseApprovalRuleRequest"></a>
#### ProcurementDtos.PurchaseApprovalRuleRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `approvalLevel` | enum : `NONE`, `MANAGER`, `DIRECTOR`, `EXECUTIVE` | oui |  |
| `minAmount` | entier int64 ⚠ | oui |  |
| `maxAmount` | entier int64 ⚠ |  |  |
| `requiredApprovals` | entier | oui |  |
| `active` | booléen |  |  |

<a id="m-ProcurementDtos-GoodsReceiptResponse"></a>
#### ProcurementDtos.GoodsReceiptResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `receiptCode` | texte |  |  |
| `orderCode` | texte |  |  |
| `locationCode` | texte |  |  |
| `status` | enum : `DRAFT`, `POSTED`, `CANCELLED` |  |  |
| `invoiceDocumentCode` | texte |  |  |
| `deliveryNoteDocumentCode` | texte |  |  |
| `proofDocumentCode` | texte |  |  |
| `receivedBy` | texte |  |  |
| `receivedAt` | date-heure ISO 8601 UTC |  |  |
| `postedAt` | date-heure ISO 8601 UTC |  |  |
| `lines` | [ProcurementDtos.ProcurementLineResponse](#m-ProcurementDtos-ProcurementLineResponse)[] |  |  |

<a id="m-GoodsReceiptStatus"></a>
#### GoodsReceiptStatus

Énumération : `DRAFT`, `POSTED`, `CANCELLED`

<a id="m-ProcurementDtos-GoodsReceiptRequest"></a>
#### ProcurementDtos.GoodsReceiptRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `orderCode` | texte |  |  |
| `locationCode` | texte | oui | longueur min `1` |
| `receivedBy` | texte |  |  |
| `invoiceDocumentCode` | texte |  |  |
| `deliveryNoteDocumentCode` | texte |  |  |
| `proofDocumentCode` | texte |  |  |
| `lines` | [ProcurementDtos.GoodsReceiptLineRequest](#m-ProcurementDtos-GoodsReceiptLineRequest)[] | oui | éléments min `1` |

<a id="m-ProcurementDtos-PurchaseOrderResponse"></a>
#### ProcurementDtos.PurchaseOrderResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `orderCode` | texte |  |  |
| `supplierCode` | texte |  |  |
| `supplierName` | texte |  |  |
| `locationCode` | texte |  |  |
| `sourceRequestCode` | texte |  |  |
| `status` | enum : `DRAFT`, `APPROVED`, `ORDERED`, `PARTIALLY_RECEIVED`, `RECEIVED`, `CANCELLED` |  |  |
| `approvalLevel` | enum : `NONE`, `MANAGER`, `DIRECTOR`, `EXECUTIVE` |  |  |
| `approvedBy` | texte |  |  |
| `approvedAt` | date-heure ISO 8601 UTC |  |  |
| `totalAmount` | entier int64 ⚠ |  |  |
| `expectedDeliveryDate` | date AAAA-MM-JJ |  |  |
| `orderedBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `lines` | [ProcurementDtos.ProcurementLineResponse](#m-ProcurementDtos-ProcurementLineResponse)[] |  |  |
| `approvalSteps` | [ProcurementDtos.PurchaseApprovalStepResponse](#m-ProcurementDtos-PurchaseApprovalStepResponse)[] |  |  |

<a id="m-PurchaseOrderStatus"></a>
#### PurchaseOrderStatus

Énumération : `DRAFT`, `APPROVED`, `ORDERED`, `PARTIALLY_RECEIVED`, `RECEIVED`, `CANCELLED`

<a id="m-ProcurementDtos-PurchaseOrderCreateRequest"></a>
#### ProcurementDtos.PurchaseOrderCreateRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `supplierCode` | texte | oui | longueur min `1` |
| `locationCode` | texte |  |  |
| `sourceRequestCode` | texte |  |  |
| `expectedDeliveryDate` | date AAAA-MM-JJ |  |  |
| `orderedBy` | texte |  |  |
| `lines` | [ProcurementDtos.PurchaseLineRequest](#m-ProcurementDtos-PurchaseLineRequest)[] | oui | éléments min `1` |

<a id="m-ProcurementDtos-PurchaseRequestResponse"></a>
#### ProcurementDtos.PurchaseRequestResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `requestCode` | texte |  |  |
| `locationCode` | texte |  |  |
| `locationName` | texte |  |  |
| `status` | enum : `DRAFT`, `SUBMITTED`, `APPROVED`, `REJECTED`, `CONVERTED`, `CANCELLED` |  |  |
| `requestedBy` | texte |  |  |
| `approvedBy` | texte |  |  |
| `rejectionReason` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `lines` | [ProcurementDtos.ProcurementLineResponse](#m-ProcurementDtos-ProcurementLineResponse)[] |  |  |
| `autoCreatedOrderCode` | texte |  |  |

<a id="m-PurchaseRequestStatus"></a>
#### PurchaseRequestStatus

Énumération : `DRAFT`, `SUBMITTED`, `APPROVED`, `REJECTED`, `CONVERTED`, `CANCELLED`

<a id="m-ProcurementDtos-PurchaseRequestCreateRequest"></a>
#### ProcurementDtos.PurchaseRequestCreateRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `locationCode` | texte |  |  |
| `requestedBy` | texte |  |  |
| `lines` | [ProcurementDtos.PurchaseLineRequest](#m-ProcurementDtos-PurchaseLineRequest)[] | oui | éléments min `1` |

<a id="m-ProcurementDtos-SupplierResponse"></a>
#### ProcurementDtos.SupplierResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `supplierCode` | texte |  |  |
| `name` | texte |  |  |
| `email` | texte |  |  |
| `phone` | texte |  |  |
| `taxId` | texte |  |  |
| `address` | texte |  |  |
| `status` | enum : `ACTIVE`, `INACTIVE`, `BLOCKED` |  |  |

<a id="m-ProcurementDtos-SupplierRequest"></a>
#### ProcurementDtos.SupplierRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `supplierCode` | texte |  |  |
| `name` | texte | oui | longueur min `1` |
| `email` | texte |  |  |
| `phone` | texte |  |  |
| `taxId` | texte |  |  |
| `address` | texte |  |  |
| `status` | enum : `ACTIVE`, `INACTIVE`, `BLOCKED` |  |  |

<a id="m-ProcurementDtos-SupplierPerformanceResponse"></a>
#### ProcurementDtos.SupplierPerformanceResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `supplierCode` | texte |  |  |
| `supplierName` | texte |  |  |
| `purchaseOrderCount` | entier int64 ⚠ |  |  |
| `receivedOrderCount` | entier int64 ⚠ |  |  |
| `completeReceiptCount` | entier int64 ⚠ |  |  |
| `completeReceiptRate` | nombre décimal |  |  |
| `averageDeliveryDelayDays` | nombre décimal |  |  |
| `averageUnitCost` | entier int64 ⚠ |  |  |

<a id="m-ProcurementDtos-ProcurementLineResponse"></a>
#### ProcurementDtos.ProcurementLineResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCode` | texte |  |  |
| `itemName` | texte |  |  |
| `quantity` | nombre décimal |  |  |
| `receivedQuantity` | nombre décimal |  |  |
| `rejectedQuantity` | nombre décimal |  |  |
| `backorderQuantity` | nombre décimal |  |  |
| `qualityAccepted` | booléen |  |  |
| `rejectionReason` | texte |  |  |
| `unitCost` | entier int64 ⚠ |  |  |
| `totalCost` | entier int64 ⚠ |  |  |
| `lotNumber` | texte |  |  |
| `expiryDate` | date AAAA-MM-JJ |  |  |

<a id="m-ProcurementDtos-GoodsReceiptLineRequest"></a>
#### ProcurementDtos.GoodsReceiptLineRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCode` | texte | oui | longueur min `1` |
| `receivedQuantity` | nombre décimal | oui |  |
| `rejectedQuantity` | nombre décimal |  |  |
| `rejectionReason` | texte |  |  |
| `qualityAccepted` | booléen |  |  |
| `quarantineAcceptedStock` | booléen |  |  |
| `quarantineReason` | texte |  |  |
| `ownershipType` | enum : `COMPANY`, `CUSTOMER`, `SUPPLIER`, `CONSIGNMENT` |  |  |
| `ownerCode` | texte |  |  |
| `unitCost` | entier int64 ⚠ |  |  |
| `lotNumber` | texte |  |  |
| `expiryDate` | date AAAA-MM-JJ |  |  |

<a id="m-ProcurementDtos-PurchaseApprovalStepResponse"></a>
#### ProcurementDtos.PurchaseApprovalStepResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `approvalLevel` | enum : `NONE`, `MANAGER`, `DIRECTOR`, `EXECUTIVE` |  |  |
| `approvedBy` | texte |  |  |
| `approvedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-ProcurementDtos-PurchaseLineRequest"></a>
#### ProcurementDtos.PurchaseLineRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCode` | texte | oui | longueur min `1` |
| `quantity` | nombre décimal | oui |  |
| `unitCost` | entier int64 ⚠ |  |  |
