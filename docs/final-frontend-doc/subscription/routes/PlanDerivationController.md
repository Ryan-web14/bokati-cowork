# PlanDerivationController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [subscription](../README.md) · 16 route(s).

## Routes

<a id="r-get-sni-api-v1-subscriptions-derivations"></a>
### GET /sni/api/v1/subscriptions/derivations

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `PaginatedResponse<PlanDerivationController.DerivationView>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [PlanDerivationController.DerivationView](#m-PlanDerivationController-DerivationView), [PlanDerivation.Status](#m-PlanDerivation-Status)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/derivation/controller/PlanDerivationController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `status` | requête | `List<PlanDerivation.Status>` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-get-sni-api-v1-subscriptions-derivations-batches-batchcode"></a>
### GET /sni/api/v1/subscriptions/derivations/batches/{batchCode}

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `List<PlanDerivationController.DerivationView>`
- Modèles : [PlanDerivationController.DerivationView](#m-PlanDerivationController-DerivationView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/derivation/controller/PlanDerivationController.java`, méthode `batch`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `batchCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-subscriptions-derivations-bulk"></a>
### POST /sni/api/v1/subscriptions/derivations/bulk

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Corps JSON (obligatoire) : `PlanDerivationController.BulkRequest`
- Réponse : `PlanDerivationBatchService.Outcome`
- Modèles : [PlanDerivationBatchService.Outcome](#m-PlanDerivationBatchService-Outcome), [PlanDerivationController.BulkRequest](#m-PlanDerivationController-BulkRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/derivation/controller/PlanDerivationController.java`, méthode `bulk`

<a id="r-get-sni-api-v1-subscriptions-derivations-concessions"></a>
### GET /sni/api/v1/subscriptions/derivations/concessions

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `List<PlanDerivationService.ConcessionLine>`
- Modèles : [PlanDerivationService.ConcessionLine](#m-PlanDerivationService-ConcessionLine)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/derivation/controller/PlanDerivationController.java`, méthode `concessions`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `LocalDate` | oui |  |
| `to` | requête | `LocalDate` | oui |  |

<a id="r-get-sni-api-v1-subscriptions-derivations-policy-approval-rule"></a>
### GET /sni/api/v1/subscriptions/derivations/policy/approval-rule

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `PlanDerivationController.ApprovalRuleView`
- Modèles : [PlanDerivationController.ApprovalRuleView](#m-PlanDerivationController-ApprovalRuleView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/derivation/controller/PlanDerivationController.java`, méthode `approvalRule`

<a id="r-put-sni-api-v1-subscriptions-derivations-policy-approval-rule"></a>
### PUT /sni/api/v1/subscriptions/derivations/policy/approval-rule

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:UPDATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Corps JSON (obligatoire) : `PlanDerivationController.ApprovalRuleRequest`
- Réponse : `PlanDerivationController.ApprovalRuleView`
- Modèles : [PlanDerivationController.ApprovalRuleView](#m-PlanDerivationController-ApprovalRuleView), [PlanDerivationController.ApprovalRuleRequest](#m-PlanDerivationController-ApprovalRuleRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/derivation/controller/PlanDerivationController.java`, méthode `updateApprovalRule`

<a id="r-put-sni-api-v1-subscriptions-derivations-policy-plan-versions-versionid-floor-price"></a>
### PUT /sni/api/v1/subscriptions/derivations/policy/plan-versions/{versionId}/floor-price

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:UPDATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Corps JSON (facultatif) : `PlanDerivationController.FloorPriceRequest`
- Réponse : `PlanDerivationController.FloorPriceView`
- Modèles : [PlanDerivationController.FloorPriceView](#m-PlanDerivationController-FloorPriceView), [PlanDerivationController.FloorPriceRequest](#m-PlanDerivationController-FloorPriceRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/derivation/controller/PlanDerivationController.java`, méthode `setFloorPrice`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `versionId` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-subscriptions-derivations-protect"></a>
### POST /sni/api/v1/subscriptions/derivations/protect

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Corps JSON (obligatoire) : `PlanDerivationController.ProtectRequest`
- Réponse : `PlanDerivationBatchService.Outcome`
- Modèles : [PlanDerivationBatchService.Outcome](#m-PlanDerivationBatchService-Outcome), [PlanDerivationController.ProtectRequest](#m-PlanDerivationController-ProtectRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/derivation/controller/PlanDerivationController.java`, méthode `protect`

<a id="r-get-sni-api-v1-subscriptions-derivations-subscriptions-subscriptionnumber"></a>
### GET /sni/api/v1/subscriptions/derivations/subscriptions/{subscriptionNumber}

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `List<PlanDerivationController.DerivationView>`
- Modèles : [PlanDerivationController.DerivationView](#m-PlanDerivationController-DerivationView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/derivation/controller/PlanDerivationController.java`, méthode `ofSubscription`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-subscriptions-derivations-subscriptions-subscriptionnumber"></a>
### POST /sni/api/v1/subscriptions/derivations/subscriptions/{subscriptionNumber}

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Corps JSON (obligatoire) : `PlanDerivationController.SpecRequest`
- Réponse : `PlanDerivationController.DerivationView`
- Modèles : [PlanDerivationController.DerivationView](#m-PlanDerivationController-DerivationView), [PlanDerivationController.SpecRequest](#m-PlanDerivationController-SpecRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/derivation/controller/PlanDerivationController.java`, méthode `create`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-subscriptions-derivations-subscriptions-subscriptionnumber-revert"></a>
### POST /sni/api/v1/subscriptions/derivations/subscriptions/{subscriptionNumber}/revert

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Corps JSON (obligatoire) : `PlanDerivationController.ReasonRequest`
- Réponse : `PlanDerivationController.DerivationView`
- Modèles : [PlanDerivationController.DerivationView](#m-PlanDerivationController-DerivationView), [PlanDerivationController.ReasonRequest](#m-PlanDerivationController-ReasonRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/derivation/controller/PlanDerivationController.java`, méthode `revert`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-subscriptions-derivations-subscriptions-subscriptionnumber-simulate"></a>
### POST /sni/api/v1/subscriptions/derivations/subscriptions/{subscriptionNumber}/simulate

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Corps JSON (obligatoire) : `PlanDerivationController.SpecRequest`
- Réponse : `PlanDerivationService.Preview`
- Modèles : [PlanDerivationService.Preview](#m-PlanDerivationService-Preview), [PlanDerivationController.SpecRequest](#m-PlanDerivationController-SpecRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/derivation/controller/PlanDerivationController.java`, méthode `simulate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-subscriptions-derivations-derivationcode"></a>
### GET /sni/api/v1/subscriptions/derivations/{derivationCode}

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `PlanDerivationController.DerivationView`
- Modèles : [PlanDerivationController.DerivationView](#m-PlanDerivationController-DerivationView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/derivation/controller/PlanDerivationController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `derivationCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-subscriptions-derivations-derivationcode-approve"></a>
### POST /sni/api/v1/subscriptions/derivations/{derivationCode}/approve

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `PlanDerivationController.DerivationView`
- Modèles : [PlanDerivationController.DerivationView](#m-PlanDerivationController-DerivationView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/derivation/controller/PlanDerivationController.java`, méthode `approve`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `derivationCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-subscriptions-derivations-derivationcode-deltas"></a>
### GET /sni/api/v1/subscriptions/derivations/{derivationCode}/deltas

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `List<PlanDerivationController.DeltaView>`
- Modèles : [PlanDerivationController.DeltaView](#m-PlanDerivationController-DeltaView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/derivation/controller/PlanDerivationController.java`, méthode `deltas`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `derivationCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-subscriptions-derivations-derivationcode-reject"></a>
### POST /sni/api/v1/subscriptions/derivations/{derivationCode}/reject

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Corps JSON (obligatoire) : `PlanDerivationController.ReasonRequest`
- Réponse : `PlanDerivationController.DerivationView`
- Modèles : [PlanDerivationController.DerivationView](#m-PlanDerivationController-DerivationView), [PlanDerivationController.ReasonRequest](#m-PlanDerivationController-ReasonRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/derivation/controller/PlanDerivationController.java`, méthode `reject`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `derivationCode` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-PlanDerivationController-DerivationView"></a>
#### PlanDerivationController.DerivationView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `derivationCode` | texte |  |  |
| `subscriptionNumber` | texte |  |  |
| `planCode` | texte |  |  |
| `sourceVersion` | texte |  |  |
| `derivedVersion` | texte |  |  |
| `status` | enum : `DRAFT`, `PENDING_APPROVAL`, `ACTIVE`, `SUPERSEDED`, `EXPIRED`, `REJECTED` |  |  |
| `reason` | enum : `NEGOTIATION`, `GOODWILL`, `PARTNERSHIP`, `PILOT`, `GRANDFATHERING`, `LOYALTY`, `CORRECTION` |  |  |
| `reasonDetails` | texte |  |  |
| `effectiveFrom` | date AAAA-MM-JJ |  |  |
| `effectiveTo` | date AAAA-MM-JJ |  |  |
| `renewalBehaviour` | enum : `KEEP`, `REVERT_TO_CATALOGUE`, `REVERT_AFTER_PERIODS` |  |  |
| `revertAfterPeriods` | entier |  |  |
| `periodsApplied` | entier |  |  |
| `promotionsAllowed` | booléen |  |  |
| `totalImpactAmount` | nombre décimal |  |  |
| `discountPercent` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `requestedBy` | texte |  |  |
| `approvedBy` | texte |  |  |
| `approvedAt` | date-heure ISO 8601 UTC |  |  |
| `rejectionReason` | texte |  |  |
| `appliedAt` | date-heure ISO 8601 UTC |  |  |
| `endedAt` | date-heure ISO 8601 UTC |  |  |
| `supersedesDerivationId` | entier int64 ⚠ |  |  |
| `batchCode` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-PlanDerivation-Status"></a>
#### PlanDerivation.Status

Énumération : `DRAFT`, `PENDING_APPROVAL`, `ACTIVE`, `SUPERSEDED`, `EXPIRED`, `REJECTED`

<a id="m-PlanDerivationBatchService-Outcome"></a>
#### PlanDerivationBatchService.Outcome

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `batchCode` | texte |  |  |
| `total` | entier |  |  |
| `succeeded` | entier |  |  |
| `failed` | entier |  |  |
| `pendingApproval` | entier |  |  |
| `totalImpact` | nombre décimal |  |  |
| `lines` | [PlanDerivationBatchService.Line](#m-PlanDerivationBatchService-Line)[] |  |  |

<a id="m-PlanDerivationController-BulkRequest"></a>
#### PlanDerivationController.BulkRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `subscriptionNumbers` | texte[] | oui |  |
| `spec` | [PlanDerivationController.SpecRequest](#m-PlanDerivationController-SpecRequest) | oui |  |
| `simulateOnly` | booléen |  |  |

<a id="m-PlanDerivationService-ConcessionLine"></a>
#### PlanDerivationService.ConcessionLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `requestedBy` | texte |  |  |
| `reason` | enum : `NEGOTIATION`, `GOODWILL`, `PARTNERSHIP`, `PILOT`, `GRANDFATHERING`, `LOYALTY`, `CORRECTION` |  |  |
| `count` | entier int64 ⚠ |  |  |
| `totalImpact` | nombre décimal |  |  |

<a id="m-PlanDerivationController-ApprovalRuleView"></a>
#### PlanDerivationController.ApprovalRuleView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `ruleCode` | texte |  |  |
| `name` | texte |  |  |
| `maxDiscountPercentWithoutApproval` | nombre décimal |  |  |
| `maxImpactWithoutApproval` | nombre décimal |  |  |
| `maxDiscountPercentAllowed` | nombre décimal |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-PlanDerivationController-ApprovalRuleRequest"></a>
#### PlanDerivationController.ApprovalRuleRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `maxDiscountPercentWithoutApproval` | nombre décimal |  |  |
| `maxImpactWithoutApproval` | nombre décimal |  |  |
| `maxDiscountPercentAllowed` | nombre décimal |  |  |

<a id="m-PlanDerivationController-FloorPriceView"></a>
#### PlanDerivationController.FloorPriceView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `versionId` | entier int64 ⚠ |  |  |
| `planCode` | texte |  |  |
| `versionNumber` | entier |  |  |
| `floorPrice` | nombre décimal |  |  |

<a id="m-PlanDerivationController-FloorPriceRequest"></a>
#### PlanDerivationController.FloorPriceRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `floorPrice` | nombre décimal |  |  |

<a id="m-PlanDerivationController-ProtectRequest"></a>
#### PlanDerivationController.ProtectRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `previousVersionId` | entier int64 ⚠ | oui |  |
| `protectedUntil` | date AAAA-MM-JJ |  |  |
| `simulateOnly` | booléen |  |  |

<a id="m-PlanDerivationController-SpecRequest"></a>
#### PlanDerivationController.SpecRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `price` | nombre décimal |  |  |
| `setupFee` | nombre décimal |  |  |
| `trialDays` | entier |  |  |
| `commitmentMonths` | entier |  |  |
| `entitlementQuantities` | objet { clé : nombre décimal } |  |  |
| `unlimitedEntitlements` | texte[] |  |  |
| `includedBenefits` | texte[] |  |  |
| `reason` | enum : `NEGOTIATION`, `GOODWILL`, `PARTNERSHIP`, `PILOT`, `GRANDFATHERING`, `LOYALTY`, `CORRECTION` | oui |  |
| `reasonDetails` | texte |  |  |
| `effectiveFrom` | date AAAA-MM-JJ |  |  |
| `effectiveTo` | date AAAA-MM-JJ |  |  |
| `renewalBehaviour` | enum : `KEEP`, `REVERT_TO_CATALOGUE`, `REVERT_AFTER_PERIODS` |  |  |
| `revertAfterPeriods` | entier |  |  |
| `promotionsAllowed` | booléen |  |  |

<a id="m-PlanDerivationController-ReasonRequest"></a>
#### PlanDerivationController.ReasonRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte | oui | longueur min `1` |

<a id="m-PlanDerivationService-Preview"></a>
#### PlanDerivationService.Preview

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `subscriptionNumber` | texte |  |  |
| `catalogueVersion` | texte |  |  |
| `cataloguePrice` | nombre décimal |  |  |
| `derivedPrice` | nombre décimal |  |  |
| `discountPercent` | nombre décimal |  |  |
| `totalImpact` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `deltas` | [PlanDerivationService.DeltaLine](#m-PlanDerivationService-DeltaLine)[] |  |  |
| `requiresApproval` | booléen |  |  |
| `blockingReason` | texte |  |  |

<a id="m-PlanDerivationController-DeltaView"></a>
#### PlanDerivationController.DeltaView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `type` | enum : `PRICE`, `ENTITLEMENT`, `BILLING_CYCLE`, `COMMITMENT`, `NOTICE_PERIOD`, `TRIAL`, `SETUP_FEE`, `PAYMENT_TERMS`, `ADDON_INCLUDED` |  |  |
| `targetCode` | texte |  |  |
| `catalogueValue` | texte |  |  |
| `derivedValue` | texte |  |  |
| `impactAmount` | nombre décimal |  |  |

<a id="m-PlanDerivationBatchService-Line"></a>
#### PlanDerivationBatchService.Line

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `subscriptionNumber` | texte |  |  |
| `ok` | booléen |  |  |
| `derivationCode` | texte |  |  |
| `impact` | nombre décimal |  |  |
| `requiresApproval` | booléen |  |  |
| `message` | texte |  |  |

<a id="m-PlanDerivationService-DeltaLine"></a>
#### PlanDerivationService.DeltaLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `type` | enum : `PRICE`, `ENTITLEMENT`, `BILLING_CYCLE`, `COMMITMENT`, `NOTICE_PERIOD`, `TRIAL`, `SETUP_FEE`, `PAYMENT_TERMS`, `ADDON_INCLUDED` |  |  |
| `target` | texte |  |  |
| `catalogueValue` | texte |  |  |
| `derivedValue` | texte |  |  |
| `impact` | nombre décimal |  |  |
