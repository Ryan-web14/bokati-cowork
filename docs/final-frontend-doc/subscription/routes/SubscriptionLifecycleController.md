# SubscriptionLifecycleController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [subscription](../README.md) · 17 route(s).

## Routes

<a id="r-post-sni-api-v1-subscriptions-lifecycle-align"></a>
### POST /sni/api/v1/subscriptions/lifecycle/align

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Corps JSON (obligatoire) : `SubscriptionLifecycleController.AlignRequest`
- Réponse : `SubscriptionAlignmentService.Outcome`
- Modèles : [SubscriptionAlignmentService.Outcome](#m-SubscriptionAlignmentService-Outcome), [SubscriptionLifecycleController.AlignRequest](#m-SubscriptionLifecycleController-AlignRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/controller/SubscriptionLifecycleController.java`, méthode `align`

<a id="r-post-sni-api-v1-subscriptions-lifecycle-debit-mandates-mandatecode-reactivate"></a>
### POST /sni/api/v1/subscriptions/lifecycle/debit-mandates/{mandateCode}/reactivate

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `SubscriptionLifecycleController.MandateView`
- Modèles : [SubscriptionLifecycleController.MandateView](#m-SubscriptionLifecycleController-MandateView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/controller/SubscriptionLifecycleController.java`, méthode `reactivateMandate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `mandateCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-subscriptions-lifecycle-debit-mandates-mandatecode-revoke"></a>
### POST /sni/api/v1/subscriptions/lifecycle/debit-mandates/{mandateCode}/revoke

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Corps JSON (facultatif) : `SubscriptionLifecycleController.ReasonRequest`
- Réponse : `SubscriptionLifecycleController.MandateView`
- Modèles : [SubscriptionLifecycleController.MandateView](#m-SubscriptionLifecycleController-MandateView), [SubscriptionLifecycleController.ReasonRequest](#m-SubscriptionLifecycleController-ReasonRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/controller/SubscriptionLifecycleController.java`, méthode `revokeMandate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `mandateCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-subscriptions-lifecycle-policy"></a>
### GET /sni/api/v1/subscriptions/lifecycle/policy

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `SubscriptionPolicy`
- Modèles : [SubscriptionPolicy](#m-SubscriptionPolicy)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/controller/SubscriptionLifecycleController.java`, méthode `policy`

<a id="r-put-sni-api-v1-subscriptions-lifecycle-policy"></a>
### PUT /sni/api/v1/subscriptions/lifecycle/policy

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:UPDATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Corps JSON (obligatoire) : `SubscriptionLifecycleController.PolicyRequest`
- Réponse : `SubscriptionPolicy`
- Modèles : [SubscriptionPolicy](#m-SubscriptionPolicy), [SubscriptionLifecycleController.PolicyRequest](#m-SubscriptionLifecycleController-PolicyRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/controller/SubscriptionLifecycleController.java`, méthode `updatePolicy`

<a id="r-delete-sni-api-v1-subscriptions-lifecycle-subscriptionnumber-commitment"></a>
### DELETE /sni/api/v1/subscriptions/lifecycle/{subscriptionNumber}/commitment

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:UPDATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/controller/SubscriptionLifecycleController.java`, méthode `releaseCommitment`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-subscriptions-lifecycle-subscriptionnumber-commitment"></a>
### GET /sni/api/v1/subscriptions/lifecycle/{subscriptionNumber}/commitment

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `SubscriptionLifecycleController.CommitmentView`
- Modèles : [SubscriptionLifecycleController.CommitmentView](#m-SubscriptionLifecycleController-CommitmentView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/controller/SubscriptionLifecycleController.java`, méthode `commitment`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-subscriptions-lifecycle-subscriptionnumber-commitment"></a>
### PUT /sni/api/v1/subscriptions/lifecycle/{subscriptionNumber}/commitment

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:UPDATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Corps JSON (obligatoire) : `SubscriptionLifecycleController.CommitmentRequest`
- Réponse : `SubscriptionLifecycleController.CommitmentView`
- Modèles : [SubscriptionLifecycleController.CommitmentView](#m-SubscriptionLifecycleController-CommitmentView), [SubscriptionLifecycleController.CommitmentRequest](#m-SubscriptionLifecycleController-CommitmentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/controller/SubscriptionLifecycleController.java`, méthode `setCommitment`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-subscriptions-lifecycle-subscriptionnumber-commitment-early-termination"></a>
### GET /sni/api/v1/subscriptions/lifecycle/{subscriptionNumber}/commitment/early-termination

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `SubscriptionCommitmentService.EarlyTermination`
- Modèles : [SubscriptionCommitmentService.EarlyTermination](#m-SubscriptionCommitmentService-EarlyTermination)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/controller/SubscriptionLifecycleController.java`, méthode `earlyTermination`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |
| `on` | requête | `LocalDate` |  |  |

<a id="r-get-sni-api-v1-subscriptions-lifecycle-subscriptionnumber-debit-attempts"></a>
### GET /sni/api/v1/subscriptions/lifecycle/{subscriptionNumber}/debit-attempts

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `List<SubscriptionLifecycleController.AttemptView>`
- Modèles : [SubscriptionLifecycleController.AttemptView](#m-SubscriptionLifecycleController-AttemptView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/controller/SubscriptionLifecycleController.java`, méthode `attempts`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-subscriptions-lifecycle-subscriptionnumber-debit-mandate"></a>
### GET /sni/api/v1/subscriptions/lifecycle/{subscriptionNumber}/debit-mandate

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `List<SubscriptionLifecycleController.MandateView>`
- Modèles : [SubscriptionLifecycleController.MandateView](#m-SubscriptionLifecycleController-MandateView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/controller/SubscriptionLifecycleController.java`, méthode `mandates`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-subscriptions-lifecycle-subscriptionnumber-debit-mandate"></a>
### POST /sni/api/v1/subscriptions/lifecycle/{subscriptionNumber}/debit-mandate

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Corps JSON (obligatoire) : `SubscriptionLifecycleController.MandateRequest`
- Réponse : `SubscriptionLifecycleController.MandateView`
- Modèles : [SubscriptionLifecycleController.MandateView](#m-SubscriptionLifecycleController-MandateView), [SubscriptionLifecycleController.MandateRequest](#m-SubscriptionLifecycleController-MandateRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/controller/SubscriptionLifecycleController.java`, méthode `giveMandate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-subscriptions-lifecycle-subscriptionnumber-debit-mandate-collect-invoicenumber"></a>
### POST /sni/api/v1/subscriptions/lifecycle/{subscriptionNumber}/debit-mandate/collect/{invoiceNumber}

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `SubscriptionLifecycleController.AttemptView`
- Modèles : [SubscriptionLifecycleController.AttemptView](#m-SubscriptionLifecycleController-AttemptView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/controller/SubscriptionLifecycleController.java`, méthode `collect`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |
| `invoiceNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-subscriptions-lifecycle-subscriptionnumber-freezes"></a>
### GET /sni/api/v1/subscriptions/lifecycle/{subscriptionNumber}/freezes

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `List<SubscriptionLifecycleController.FreezeView>`
- Modèles : [SubscriptionLifecycleController.FreezeView](#m-SubscriptionLifecycleController-FreezeView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/controller/SubscriptionLifecycleController.java`, méthode `freezes`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-subscriptions-lifecycle-subscriptionnumber-freezes-allowance"></a>
### GET /sni/api/v1/subscriptions/lifecycle/{subscriptionNumber}/freezes/allowance

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `SubscriptionFreezeService.Allowance`
- Modèles : [SubscriptionFreezeService.Allowance](#m-SubscriptionFreezeService-Allowance)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/controller/SubscriptionLifecycleController.java`, méthode `freezeAllowance`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-subscriptions-lifecycle-subscriptionnumber-grace-exit"></a>
### POST /sni/api/v1/subscriptions/lifecycle/{subscriptionNumber}/grace/exit

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Corps JSON (facultatif) : `SubscriptionLifecycleController.ReasonRequest`
- Réponse : `vide`
- Modèles : [SubscriptionLifecycleController.ReasonRequest](#m-SubscriptionLifecycleController-ReasonRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/controller/SubscriptionLifecycleController.java`, méthode `exitGrace`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-subscriptions-lifecycle-subscriptionnumber-plan-change-preview"></a>
### GET /sni/api/v1/subscriptions/lifecycle/{subscriptionNumber}/plan-change/preview

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `PlanChangeProrationService.Preview`
- Modèles : [PlanChangeProrationService.Preview](#m-PlanChangeProrationService-Preview)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/controller/SubscriptionLifecycleController.java`, méthode `planChangePreview`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |
| `targetPlanVersionId` | requête | `Long` | oui |  |
| `effectiveDate` | requête | `LocalDate` |  |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-SubscriptionAlignmentService-Outcome"></a>
#### SubscriptionAlignmentService.Outcome

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `subscriberType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` |  |  |
| `subscriberCode` | texte |  |  |
| `targetPeriodEnd` | date AAAA-MM-JJ |  |  |
| `total` | entier |  |  |
| `aligned` | entier |  |  |
| `skipped` | entier |  |  |
| `totalBridging` | nombre décimal |  |  |
| `lines` | [SubscriptionAlignmentService.Line](#m-SubscriptionAlignmentService-Line)[] |  |  |

<a id="m-SubscriptionLifecycleController-AlignRequest"></a>
#### SubscriptionLifecycleController.AlignRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `subscriberType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` | oui |  |
| `subscriberCode` | texte | oui | longueur min `1` |
| `targetPeriodEnd` | date AAAA-MM-JJ |  |  |
| `simulateOnly` | booléen |  |  |

<a id="m-SubscriptionLifecycleController-MandateView"></a>
#### SubscriptionLifecycleController.MandateView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `mandateCode` | texte |  |  |
| `subscriptionNumber` | texte |  |  |
| `walletNumber` | texte |  |  |
| `status` | enum : `ACTIVE`, `SUSPENDED`, `REVOKED` |  |  |
| `consentGivenAt` | date-heure ISO 8601 UTC |  |  |
| `consentGivenBy` | texte |  |  |
| `consentChannel` | enum : `PORTAL`, `SIGNED_FORM`, `EMAIL`, `STAFF` |  |  |
| `consentReference` | texte |  |  |
| `maxAmountPerDebit` | nombre décimal |  |  |
| `consecutiveFailures` | entier |  |  |
| `lastDebitAt` | date-heure ISO 8601 UTC |  |  |
| `revokedAt` | date-heure ISO 8601 UTC |  |  |
| `revokedBy` | texte |  |  |
| `revocationReason` | texte |  |  |

<a id="m-SubscriptionLifecycleController-ReasonRequest"></a>
#### SubscriptionLifecycleController.ReasonRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte |  |  |

<a id="m-SubscriptionPolicy"></a>
#### SubscriptionPolicy

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `policyCode` | texte |  |  |
| `name` | texte |  |  |
| `prorationPolicy` | enum : `DAILY`, `MONTH_STARTED`, `NONE` |  |  |
| `defaultNoticeDays` | entier |  |  |
| `earlyTerminationFormula` | enum : `NONE`, `FIXED_FEE`, `PERCENT_OF_REMAINING`, `REMAINING_PERIODS` |  |  |
| `earlyTerminationPercent` | nombre décimal |  |  |
| `earlyTerminationFixedFee` | nombre décimal |  |  |
| `freezeMaxPerYear` | entier |  |  |
| `freezeMaxDays` | entier |  |  |
| `freezeNoticeDays` | entier |  |  |
| `freezeFeePercent` | nombre décimal |  |  |
| `gracePeriodDays` | entier |  |  |
| `suspensionAfterGraceDays` | entier |  |  |
| `renewalExpiryDays` | entier |  |  |
| `quoteValidityDays` | entier |  |  |
| `active` | booléen |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-SubscriptionLifecycleController-PolicyRequest"></a>
#### SubscriptionLifecycleController.PolicyRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `prorationPolicy` | enum : `DAILY`, `MONTH_STARTED`, `NONE` |  |  |
| `defaultNoticeDays` | entier |  |  |
| `earlyTerminationFormula` | enum : `NONE`, `FIXED_FEE`, `PERCENT_OF_REMAINING`, `REMAINING_PERIODS` |  |  |
| `earlyTerminationPercent` | nombre décimal |  |  |
| `earlyTerminationFixedFee` | nombre décimal |  |  |
| `freezeMaxPerYear` | entier |  |  |
| `freezeMaxDays` | entier |  |  |
| `freezeNoticeDays` | entier |  |  |
| `freezeFeePercent` | nombre décimal |  |  |
| `gracePeriodDays` | entier |  |  |
| `suspensionAfterGraceDays` | entier |  |  |
| `renewalExpiryDays` | entier |  |  |
| `quoteValidityDays` | entier |  |  |

<a id="m-SubscriptionLifecycleController-CommitmentView"></a>
#### SubscriptionLifecycleController.CommitmentView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `subscriptionNumber` | texte |  |  |
| `commitmentMonths` | entier |  |  |
| `commitmentStart` | date AAAA-MM-JJ |  |  |
| `commitmentEnd` | date AAAA-MM-JJ |  |  |
| `earlyTerminationFormula` | enum : `NONE`, `FIXED_FEE`, `PERCENT_OF_REMAINING`, `REMAINING_PERIODS` |  |  |
| `earlyTerminationPercent` | nombre décimal |  |  |
| `earlyTerminationFixedFee` | nombre décimal |  |  |
| `autoRenewCommitment` | booléen |  |  |
| `source` | enum : `PLAN`, `QUOTE`, `MANUAL` |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-SubscriptionLifecycleController-CommitmentRequest"></a>
#### SubscriptionLifecycleController.CommitmentRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `commitmentMonths` | entier | oui |  |
| `commitmentStart` | date AAAA-MM-JJ |  |  |
| `formula` | enum : `NONE`, `FIXED_FEE`, `PERCENT_OF_REMAINING`, `REMAINING_PERIODS` |  |  |
| `percent` | nombre décimal |  |  |
| `fixedFee` | nombre décimal |  |  |
| `autoRenewCommitment` | booléen |  |  |

<a id="m-SubscriptionCommitmentService-EarlyTermination"></a>
#### SubscriptionCommitmentService.EarlyTermination

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `early` | booléen |  |  |
| `remainingMonths` | entier |  |  |
| `fee` | nombre décimal |  |  |
| `formula` | enum : `NONE`, `FIXED_FEE`, `PERCENT_OF_REMAINING`, `REMAINING_PERIODS` |  |  |
| `commitmentEnd` | date AAAA-MM-JJ |  |  |

<a id="m-SubscriptionLifecycleController-AttemptView"></a>
#### SubscriptionLifecycleController.AttemptView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `invoiceNumber` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `status` | enum : `SUCCEEDED`, `INSUFFICIENT_FUNDS`, `OVER_LIMIT`, `FAILED` |  |  |
| `message` | texte |  |  |
| `transactionNumber` | texte |  |  |
| `executedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-SubscriptionLifecycleController-MandateRequest"></a>
#### SubscriptionLifecycleController.MandateRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `walletNumber` | texte | oui | longueur min `1` |
| `channel` | enum : `PORTAL`, `SIGNED_FORM`, `EMAIL`, `STAFF` | oui |  |
| `reference` | texte |  |  |
| `maxAmountPerDebit` | nombre décimal |  |  |

<a id="m-SubscriptionLifecycleController-FreezeView"></a>
#### SubscriptionLifecycleController.FreezeView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `startedOn` | date AAAA-MM-JJ |  |  |
| `plannedUntil` | date AAAA-MM-JJ |  |  |
| `resumedOn` | date AAAA-MM-JJ |  |  |
| `daysPlanned` | entier |  |  |
| `daysEffective` | entier |  |  |
| `feeAmount` | nombre décimal |  |  |
| `feeBillableNumber` | texte |  |  |
| `reason` | texte |  |  |
| `requestedBy` | texte |  |  |

<a id="m-SubscriptionFreezeService-Allowance"></a>
#### SubscriptionFreezeService.Allowance

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `freezesUsedThisYear` | entier |  |  |
| `freezesAllowedPerYear` | entier |  |  |
| `maxDays` | entier |  |  |
| `feePercent` | nombre décimal |  |  |
| `canFreeze` | booléen |  |  |

<a id="m-PlanChangeProrationService-Preview"></a>
#### PlanChangeProrationService.Preview

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `policy` | enum : `DAILY`, `MONTH_STARTED`, `NONE` |  |  |
| `effectiveDate` | date AAAA-MM-JJ |  |  |
| `currentPeriodTotal` | nombre décimal |  |  |
| `targetPeriodTotal` | nombre décimal |  |  |
| `unusedAtCurrentPrice` | nombre décimal |  |  |
| `remainingAtTargetPrice` | nombre décimal |  |  |
| `amount` | nombre décimal |  |  |
| `credit` | booléen |  |  |
| `targetAmounts` | [PlanPriceAmountCalculator.Amounts](#m-PlanPriceAmountCalculator-Amounts) |  |  |
| `currency` | texte |  |  |

<a id="m-SubscriptionAlignmentService-Line"></a>
#### SubscriptionAlignmentService.Line

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `subscriptionNumber` | texte |  |  |
| `currentPeriodEnd` | date AAAA-MM-JJ |  |  |
| `newPeriodEnd` | date AAAA-MM-JJ |  |  |
| `bridgingDays` | entier |  |  |
| `bridgingAmount` | nombre décimal |  |  |
| `aligned` | booléen |  |  |
| `message` | texte |  |  |

<a id="m-PlanPriceAmountCalculator-Amounts"></a>
#### PlanPriceAmountCalculator.Amounts

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `subtotal` | nombre décimal |  |  |
| `tax` | nombre décimal |  |  |
| `total` | nombre décimal |  |  |
