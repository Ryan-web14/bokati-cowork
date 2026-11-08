# BillingBridgeController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [subscription](../README.md) · 1 route(s).

## Routes

<a id="r-get-sni-api-v1-billable-items"></a>
### GET /sni/api/v1/billable-items

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ`
- Réponse : `PaginatedResponse<BillableItemResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [BillableItemResponse](#m-BillableItemResponse), [BillableItemStatus](#m-BillableItemStatus), [SubscriberType](#m-SubscriberType)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/BillingBridgeController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `status` | requête | `BillableItemStatus` |  |  |
| `subscriberType` | requête | `SubscriberType` |  |  |
| `subscriberCode` | requête | `String` |  |  |
| `sourceType` | requête | `String` |  |  |
| `sourceId` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-BillableItemResponse"></a>
#### BillableItemResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `billableNumber` | texte |  |  |
| `sourceType` | texte |  |  |
| `sourceId` | texte |  |  |
| `subscriberType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` |  |  |
| `subscriberCode` | texte |  |  |
| `description` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `taxCode` | texte |  |  |
| `billingPeriodStart` | date AAAA-MM-JJ |  |  |
| `billingPeriodEnd` | date AAAA-MM-JJ |  |  |
| `status` | enum : `PENDING`, `SENT_TO_INVOICE`, `INVOICED`, `CANCELLED` |  |  |
| `invoiceId` | entier int64 ⚠ |  |  |

<a id="m-BillableItemStatus"></a>
#### BillableItemStatus

Énumération : `PENDING`, `SENT_TO_INVOICE`, `INVOICED`, `CANCELLED`

<a id="m-SubscriberType"></a>
#### SubscriberType

Énumération : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY`
