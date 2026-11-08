# EntitlementController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [subscription](../README.md) · 7 route(s).

## Routes

<a id="r-get-sni-api-v1-entitlements-balances"></a>
### GET /sni/api/v1/entitlements/balances

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `List<EntitlementGrantResponse>`
- Modèles : [EntitlementGrantResponse](#m-EntitlementGrantResponse), [SubscriberType](#m-SubscriberType)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/EntitlementController.java`, méthode `balances`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ownerType` | requête | `SubscriberType` | oui |  |
| `ownerCode` | requête | `String` | oui |  |

<a id="r-get-sni-api-v1-entitlements-grants"></a>
### GET /sni/api/v1/entitlements/grants

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `PaginatedResponse<EntitlementGrantResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [EntitlementGrantResponse](#m-EntitlementGrantResponse), [SubscriberType](#m-SubscriberType), [EntitlementGrantStatus](#m-EntitlementGrantStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/EntitlementController.java`, méthode `listGrants`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ownerType` | requête | `SubscriberType` |  |  |
| `ownerCode` | requête | `String` |  |  |
| `entitlementCode` | requête | `String` |  |  |
| `status` | requête | `EntitlementGrantStatus` |  |  |
| `validAt` | requête | `Instant` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

<a id="r-post-sni-api-v1-internal-entitlements-check"></a>
### POST /sni/api/v1/internal/entitlements/check

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (obligatoire) : `EntitlementOperationRequest`
- Réponse : `EntitlementOperationResponse`
- Modèles : [EntitlementOperationResponse](#m-EntitlementOperationResponse), [EntitlementOperationRequest](#m-EntitlementOperationRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/EntitlementController.java`, méthode `check`

<a id="r-post-sni-api-v1-internal-entitlements-consume"></a>
### POST /sni/api/v1/internal/entitlements/consume

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (obligatoire) : `EntitlementOperationRequest`
- Réponse : `EntitlementOperationResponse`
- Modèles : [EntitlementOperationResponse](#m-EntitlementOperationResponse), [EntitlementOperationRequest](#m-EntitlementOperationRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/EntitlementController.java`, méthode `consume`

<a id="r-post-sni-api-v1-internal-entitlements-refund"></a>
### POST /sni/api/v1/internal/entitlements/refund

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (obligatoire) : `EntitlementOperationRequest`
- Réponse : `EntitlementOperationResponse`
- Modèles : [EntitlementOperationResponse](#m-EntitlementOperationResponse), [EntitlementOperationRequest](#m-EntitlementOperationRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/EntitlementController.java`, méthode `refund`

<a id="r-post-sni-api-v1-internal-entitlements-release"></a>
### POST /sni/api/v1/internal/entitlements/release

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (obligatoire) : `EntitlementOperationRequest`
- Réponse : `EntitlementOperationResponse`
- Modèles : [EntitlementOperationResponse](#m-EntitlementOperationResponse), [EntitlementOperationRequest](#m-EntitlementOperationRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/EntitlementController.java`, méthode `release`

<a id="r-post-sni-api-v1-internal-entitlements-reserve"></a>
### POST /sni/api/v1/internal/entitlements/reserve

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (obligatoire) : `EntitlementOperationRequest`
- Réponse : `EntitlementOperationResponse`
- Modèles : [EntitlementOperationResponse](#m-EntitlementOperationResponse), [EntitlementOperationRequest](#m-EntitlementOperationRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/EntitlementController.java`, méthode `reserve`

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-EntitlementGrantResponse"></a>
#### EntitlementGrantResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `grantNumber` | texte |  |  |
| `entitlementCode` | texte |  |  |
| `entitlementName` | texte |  |  |
| `unit` | enum : `DAY`, `HOUR`, `CREDIT`, `BOOKING`, `VISIT`, `MEMBER`, `PERCENT`, `AMOUNT`, `BOOLEAN` |  |  |
| `ownerType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` |  |  |
| `ownerCode` | texte |  |  |
| `quantityGranted` | nombre décimal |  |  |
| `quantityRemaining` | nombre décimal |  |  |
| `unlimited` | booléen |  |  |
| `validFrom` | date-heure ISO 8601 UTC |  |  |
| `validUntil` | date-heure ISO 8601 UTC |  |  |
| `status` | enum : `ACTIVE`, `RESERVED`, `DEPLETED`, `EXPIRED`, `CANCELLED`, `SUSPENDED` |  |  |
| `sourceType` | texte |  |  |
| `sourceId` | texte |  |  |

<a id="m-SubscriberType"></a>
#### SubscriberType

Énumération : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY`

<a id="m-EntitlementGrantStatus"></a>
#### EntitlementGrantStatus

Énumération : `ACTIVE`, `RESERVED`, `DEPLETED`, `EXPIRED`, `CANCELLED`, `SUSPENDED`

<a id="m-EntitlementOperationResponse"></a>
#### EntitlementOperationResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `allowed` | booléen |  |  |
| `entitlementCode` | texte |  |  |
| `requestedQuantity` | nombre décimal |  |  |
| `availableQuantity` | nombre décimal |  |  |
| `message` | texte |  |  |

<a id="m-EntitlementOperationRequest"></a>
#### EntitlementOperationRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `ownerType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` | oui |  |
| `ownerCode` | texte | oui | longueur min `1` |
| `entitlementCode` | texte | oui | longueur min `1` |
| `quantity` | nombre décimal | oui |  |
| `referenceType` | texte |  |  |
| `referenceId` | texte |  |  |
| `idempotencyKey` | texte |  |  |
| `reason` | texte |  |  |
| `holdUntil` | date-heure ISO 8601 UTC |  |  |
| `beneficiaryType` | texte |  |  |
| `beneficiaryCode` | texte |  |  |
| `scopeType` | texte |  |  |
| `scopeNumber` | texte |  |  |
