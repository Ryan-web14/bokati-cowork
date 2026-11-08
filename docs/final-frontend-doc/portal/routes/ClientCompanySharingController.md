# ClientCompanySharingController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [portal](../README.md) · 9 route(s).

## Routes

<a id="r-get-sni-api-v1-client-companies-customerid-subscriptions"></a>
### GET /sni/api/v1/client/companies/{customerId}/subscriptions

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<SharingDtos.SubscriptionSummary>`
- Modèles : [SharingDtos.SubscriptionSummary](#m-SharingDtos-SubscriptionSummary)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanySharingController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-client-companies-customerid-subscriptions-subscriptionnumber-max-seats"></a>
### PATCH /sni/api/v1/client/companies/{customerId}/subscriptions/{subscriptionNumber}/max-seats

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `SharingDtos.MaxSeatsRequest`
- Réponse : `SharingDtos.SharingView`
- Modèles : [SharingDtos.SharingView](#m-SharingDtos-SharingView), [SharingDtos.MaxSeatsRequest](#m-SharingDtos-MaxSeatsRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanySharingController.java`, méthode `setMaxSeats`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-companies-customerid-subscriptions-subscriptionnumber-seats"></a>
### POST /sni/api/v1/client/companies/{customerId}/subscriptions/{subscriptionNumber}/seats

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `SharingDtos.AssignSeatRequest`
- Réponse : `SharingDtos.SharingView`
- Modèles : [SharingDtos.SharingView](#m-SharingDtos-SharingView), [SharingDtos.AssignSeatRequest](#m-SharingDtos-AssignSeatRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanySharingController.java`, méthode `assign`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-client-companies-customerid-subscriptions-subscriptionnumber-seats-seatid"></a>
### PATCH /sni/api/v1/client/companies/{customerId}/subscriptions/{subscriptionNumber}/seats/{seatId}

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `SharingDtos.UpdateSeatRequest`
- Réponse : `SharingDtos.SharingView`
- Modèles : [SharingDtos.SharingView](#m-SharingDtos-SharingView), [SharingDtos.UpdateSeatRequest](#m-SharingDtos-UpdateSeatRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanySharingController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `subscriptionNumber` | chemin | `String` | oui |  |
| `seatId` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-client-companies-customerid-subscriptions-subscriptionnumber-seats-seatid-release"></a>
### POST /sni/api/v1/client/companies/{customerId}/subscriptions/{subscriptionNumber}/seats/{seatId}/release

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (facultatif) : `SharingDtos.ReleaseSeatRequest`
- Réponse : `SharingDtos.ReleaseResult`
- Modèles : [SharingDtos.ReleaseResult](#m-SharingDtos-ReleaseResult), [SharingDtos.ReleaseSeatRequest](#m-SharingDtos-ReleaseSeatRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanySharingController.java`, méthode `release`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `subscriptionNumber` | chemin | `String` | oui |  |
| `seatId` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-client-companies-customerid-subscriptions-subscriptionnumber-sharing"></a>
### GET /sni/api/v1/client/companies/{customerId}/subscriptions/{subscriptionNumber}/sharing

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `SharingDtos.SharingView`
- Modèles : [SharingDtos.SharingView](#m-SharingDtos-SharingView)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanySharingController.java`, méthode `view`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-client-companies-customerid-subscriptions-subscriptionnumber-sharing-entitlementcode"></a>
### PUT /sni/api/v1/client/companies/{customerId}/subscriptions/{subscriptionNumber}/sharing/{entitlementCode}

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `SharingDtos.RuleRequest`
- Réponse : `SharingDtos.SharingView`
- Modèles : [SharingDtos.SharingView](#m-SharingDtos-SharingView), [SharingDtos.RuleRequest](#m-SharingDtos-RuleRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanySharingController.java`, méthode `setRule`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `subscriptionNumber` | chemin | `String` | oui |  |
| `entitlementCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-client-companies-customerid-usage"></a>
### GET /sni/api/v1/client/companies/{customerId}/usage

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<SharingDtos.SharingView>`
- Modèles : [SharingDtos.SharingView](#m-SharingDtos-SharingView)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanySharingController.java`, méthode `usage`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-client-me-entitlements"></a>
### GET /sni/api/v1/client/me/entitlements

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<SharingDtos.MySeat>`
- Modèles : [SharingDtos.MySeat](#m-SharingDtos-MySeat)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/company/controller/ClientCompanySharingController.java`, méthode `mySeats`

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-SharingDtos-SubscriptionSummary"></a>
#### SharingDtos.SubscriptionSummary

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `subscriptionNumber` | texte |  |  |
| `planName` | texte |  |  |
| `status` | texte |  |  |
| `periodStart` | date AAAA-MM-JJ |  |  |
| `periodEnd` | date AAAA-MM-JJ |  |  |
| `maxSeats` | entier |  |  |
| `seatCeiling` | entier |  |  |
| `activeSeats` | entier int64 ⚠ |  |  |

<a id="m-SharingDtos-SharingView"></a>
#### SharingDtos.SharingView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `companyCustomerCode` | texte |  |  |
| `subscription` | [SharingDtos.SubscriptionSummary](#m-SharingDtos-SubscriptionSummary) |  |  |
| `entitlements` | [SharingDtos.EntitlementView](#m-SharingDtos-EntitlementView)[] |  |  |
| `seats` | [SharingDtos.SeatView](#m-SharingDtos-SeatView)[] |  |  |

<a id="m-SharingDtos-MaxSeatsRequest"></a>
#### SharingDtos.MaxSeatsRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `maxSeats` | entier |  |  |

<a id="m-SharingDtos-AssignSeatRequest"></a>
#### SharingDtos.AssignSeatRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `memberId` | texte | oui | longueur min `1` |
| `allowances` | [SharingDtos.AllowanceRequest](#m-SharingDtos-AllowanceRequest)[] |  |  |

<a id="m-SharingDtos-UpdateSeatRequest"></a>
#### SharingDtos.UpdateSeatRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `allowances` | [SharingDtos.AllowanceRequest](#m-SharingDtos-AllowanceRequest)[] |  |  |

<a id="m-SharingDtos-ReleaseResult"></a>
#### SharingDtos.ReleaseResult

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `seatId` | entier int64 ⚠ |  |  |
| `memberId` | texte |  |  |
| `futureBookings` | texte[] |  |  |
| `warning` | texte |  |  |

<a id="m-SharingDtos-ReleaseSeatRequest"></a>
#### SharingDtos.ReleaseSeatRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte |  | longueur min `0`, longueur max `500` |

<a id="m-SharingDtos-RuleRequest"></a>
#### SharingDtos.RuleRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `mode` | enum : `POOL`, `QUOTA`, `QUOTA_AND_POOL` | oui |  |
| `defaultQuota` | nombre décimal |  |  |
| `seatCap` | nombre décimal |  |  |

<a id="m-SharingDtos-MySeat"></a>
#### SharingDtos.MySeat

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `companyCustomerCode` | texte |  |  |
| `companyName` | texte |  |  |
| `subscriptionNumber` | texte |  |  |
| `seatId` | entier int64 ⚠ |  |  |
| `periodStart` | date AAAA-MM-JJ |  |  |
| `periodEnd` | date AAAA-MM-JJ |  |  |
| `entitlements` | [SharingDtos.MySeatEntitlement](#m-SharingDtos-MySeatEntitlement)[] |  |  |

<a id="m-SharingDtos-EntitlementView"></a>
#### SharingDtos.EntitlementView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `entitlementCode` | texte |  |  |
| `entitlementName` | texte |  |  |
| `unit` | texte |  |  |
| `mode` | enum : `POOL`, `QUOTA`, `QUOTA_AND_POOL` |  |  |
| `defaultQuota` | nombre décimal |  |  |
| `seatCap` | nombre décimal |  |  |
| `unlimited` | booléen |  |  |
| `granted` | nombre décimal |  |  |
| `remaining` | nombre décimal |  |  |
| `distributedQuotas` | nombre décimal |  |  |
| `poolSize` | nombre décimal |  |  |
| `poolUsed` | nombre décimal |  |  |

<a id="m-SharingDtos-SeatView"></a>
#### SharingDtos.SeatView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `memberId` | texte |  |  |
| `memberName` | texte |  |  |
| `status` | texte |  |  |
| `assignedAt` | date-heure ISO 8601 UTC |  |  |
| `assignedBy` | texte |  |  |
| `entitlements` | [SharingDtos.SeatEntitlement](#m-SharingDtos-SeatEntitlement)[] |  |  |

<a id="m-SharingDtos-AllowanceRequest"></a>
#### SharingDtos.AllowanceRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `entitlementCode` | texte | oui | longueur min `1` |
| `quota` | nombre décimal |  |  |
| `cap` | nombre décimal |  |  |

<a id="m-SharingDtos-MySeatEntitlement"></a>
#### SharingDtos.MySeatEntitlement

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `entitlementCode` | texte |  |  |
| `entitlementName` | texte |  |  |
| `unit` | texte |  |  |
| `mode` | enum : `POOL`, `QUOTA`, `QUOTA_AND_POOL` |  |  |
| `quota` | nombre décimal |  |  |
| `quotaRemaining` | nombre décimal |  |  |
| `cap` | nombre décimal |  |  |
| `used` | nombre décimal |  |  |
| `companyRemaining` | nombre décimal |  |  |
| `poolRemaining` | nombre décimal |  |  |
| `unlimited` | booléen |  |  |

<a id="m-SharingDtos-SeatEntitlement"></a>
#### SharingDtos.SeatEntitlement

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `entitlementCode` | texte |  |  |
| `quota` | nombre décimal |  |  |
| `quotaUsed` | nombre décimal |  |  |
| `quotaRemaining` | nombre décimal |  |  |
| `cap` | nombre décimal |  |  |
| `used` | nombre décimal |  |  |
