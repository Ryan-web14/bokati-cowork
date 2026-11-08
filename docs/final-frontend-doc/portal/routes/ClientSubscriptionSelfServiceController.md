# ClientSubscriptionSelfServiceController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [portal](../README.md) · 19 route(s).

## Routes

<a id="r-get-sni-api-v1-client-history"></a>
### GET /sni/api/v1/client/history

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<ClientHistoryService.Entry>`
- Modèles : [ClientHistoryService.Entry](#m-ClientHistoryService-Entry)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/subscription/controller/ClientSubscriptionSelfServiceController.java`, méthode `history`

<a id="r-get-sni-api-v1-client-quotes"></a>
### GET /sni/api/v1/client/quotes

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<ClientSubscriptionSelfServiceController.QuoteView>`
- Modèles : [ClientSubscriptionSelfServiceController.QuoteView](#m-ClientSubscriptionSelfServiceController-QuoteView)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/subscription/controller/ClientSubscriptionSelfServiceController.java`, méthode `quotes`

<a id="r-get-sni-api-v1-client-quotes-quotenumber"></a>
### GET /sni/api/v1/client/quotes/{quoteNumber}

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `ClientSubscriptionSelfServiceController.QuoteView`
- Modèles : [ClientSubscriptionSelfServiceController.QuoteView](#m-ClientSubscriptionSelfServiceController-QuoteView)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/subscription/controller/ClientSubscriptionSelfServiceController.java`, méthode `quote`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `quoteNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-quotes-quotenumber-accept"></a>
### POST /sni/api/v1/client/quotes/{quoteNumber}/accept

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `ClientSubscriptionSelfServiceController.QuoteView`
- Modèles : [ClientSubscriptionSelfServiceController.QuoteView](#m-ClientSubscriptionSelfServiceController-QuoteView)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/subscription/controller/ClientSubscriptionSelfServiceController.java`, méthode `acceptQuote`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `quoteNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-quotes-quotenumber-reject"></a>
### POST /sni/api/v1/client/quotes/{quoteNumber}/reject

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (facultatif) : `ClientSubscriptionSelfServiceController.ReasonRequest`
- Réponse : `ClientSubscriptionSelfServiceController.QuoteView`
- Modèles : [ClientSubscriptionSelfServiceController.QuoteView](#m-ClientSubscriptionSelfServiceController-QuoteView), [ClientSubscriptionSelfServiceController.ReasonRequest](#m-ClientSubscriptionSelfServiceController-ReasonRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/subscription/controller/ClientSubscriptionSelfServiceController.java`, méthode `rejectQuote`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `quoteNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-client-subscriptions-subscriptionnumber-commitment-early-termination"></a>
### GET /sni/api/v1/client/subscriptions/{subscriptionNumber}/commitment/early-termination

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `SubscriptionCommitmentService.EarlyTermination`
- Modèles : [SubscriptionCommitmentService.EarlyTermination](#m-SubscriptionCommitmentService-EarlyTermination)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/subscription/controller/ClientSubscriptionSelfServiceController.java`, méthode `earlyTermination`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |
| `on` | requête | `LocalDate` |  |  |

<a id="r-delete-sni-api-v1-client-subscriptions-subscriptionnumber-debit-mandate"></a>
### DELETE /sni/api/v1/client/subscriptions/{subscriptionNumber}/debit-mandate

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `ClientSubscriptionSelfServiceController.MandateView`
- Modèles : [ClientSubscriptionSelfServiceController.MandateView](#m-ClientSubscriptionSelfServiceController-MandateView)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/subscription/controller/ClientSubscriptionSelfServiceController.java`, méthode `revokeMandate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-client-subscriptions-subscriptionnumber-debit-mandate"></a>
### GET /sni/api/v1/client/subscriptions/{subscriptionNumber}/debit-mandate

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `ClientSubscriptionSelfServiceController.MandateStatusView`
- Modèles : [ClientSubscriptionSelfServiceController.MandateStatusView](#m-ClientSubscriptionSelfServiceController-MandateStatusView)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/subscription/controller/ClientSubscriptionSelfServiceController.java`, méthode `mandate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-subscriptions-subscriptionnumber-debit-mandate"></a>
### POST /sni/api/v1/client/subscriptions/{subscriptionNumber}/debit-mandate

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `ClientSubscriptionSelfServiceController.MandateRequest`
- Réponse : `ClientSubscriptionSelfServiceController.MandateView`
- Modèles : [ClientSubscriptionSelfServiceController.MandateView](#m-ClientSubscriptionSelfServiceController-MandateView), [ClientSubscriptionSelfServiceController.MandateRequest](#m-ClientSubscriptionSelfServiceController-MandateRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/subscription/controller/ClientSubscriptionSelfServiceController.java`, méthode `giveMandate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-subscriptions-subscriptionnumber-freeze"></a>
### POST /sni/api/v1/client/subscriptions/{subscriptionNumber}/freeze

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `ClientSubscriptionSelfServiceController.FreezeRequest`
- Réponse : `SubscriptionResponse`
- Modèles : [SubscriptionResponse](#m-SubscriptionResponse), [ClientSubscriptionSelfServiceController.FreezeRequest](#m-ClientSubscriptionSelfServiceController-FreezeRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/subscription/controller/ClientSubscriptionSelfServiceController.java`, méthode `freeze`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-client-subscriptions-subscriptionnumber-freeze-terms"></a>
### GET /sni/api/v1/client/subscriptions/{subscriptionNumber}/freeze/terms

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `ClientSubscriptionSelfService.FreezeTerms`
- Modèles : [ClientSubscriptionSelfService.FreezeTerms](#m-ClientSubscriptionSelfService-FreezeTerms)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/subscription/controller/ClientSubscriptionSelfServiceController.java`, méthode `freezeTerms`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-subscriptions-subscriptionnumber-plan-change"></a>
### POST /sni/api/v1/client/subscriptions/{subscriptionNumber}/plan-change

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `ClientSubscriptionSelfServiceController.PlanChangeRequest`
- Réponse : `SubscriptionChangeResponse`
- Modèles : [SubscriptionChangeResponse](#m-SubscriptionChangeResponse), [ClientSubscriptionSelfServiceController.PlanChangeRequest](#m-ClientSubscriptionSelfServiceController-PlanChangeRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/subscription/controller/ClientSubscriptionSelfServiceController.java`, méthode `changePlan`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-client-subscriptions-subscriptionnumber-plan-change-preview"></a>
### GET /sni/api/v1/client/subscriptions/{subscriptionNumber}/plan-change/preview

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `ClientSubscriptionSelfService.PlanChangePreview`
- Modèles : [ClientSubscriptionSelfService.PlanChangePreview](#m-ClientSubscriptionSelfService-PlanChangePreview)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/subscription/controller/ClientSubscriptionSelfServiceController.java`, méthode `previewPlanChange`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |
| `targetPlanCode` | requête | `String` | oui |  |
| `atNextPeriod` | requête | `boolean` |  | false |

<a id="r-post-sni-api-v1-client-subscriptions-subscriptionnumber-resume"></a>
### POST /sni/api/v1/client/subscriptions/{subscriptionNumber}/resume

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `SubscriptionResponse`
- Modèles : [SubscriptionResponse](#m-SubscriptionResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/subscription/controller/ClientSubscriptionSelfServiceController.java`, méthode `resume`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-subscriptions-subscriptionnumber-termination"></a>
### POST /sni/api/v1/client/subscriptions/{subscriptionNumber}/termination

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (facultatif) : `ClientSubscriptionSelfServiceController.TerminationRequest`
- Réponse : `ClientSubscriptionSelfServiceController.TerminationView`
- Modèles : [ClientSubscriptionSelfServiceController.TerminationView](#m-ClientSubscriptionSelfServiceController-TerminationView), [ClientSubscriptionSelfServiceController.TerminationRequest](#m-ClientSubscriptionSelfServiceController-TerminationRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/subscription/controller/ClientSubscriptionSelfServiceController.java`, méthode `requestTermination`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-subscriptions-subscriptionnumber-termination-preview"></a>
### POST /sni/api/v1/client/subscriptions/{subscriptionNumber}/termination/preview

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (facultatif) : `ClientSubscriptionSelfServiceController.TerminationRequest`
- Réponse : `SubscriptionTerminationService.Preview`
- Modèles : [SubscriptionTerminationService.Preview](#m-SubscriptionTerminationService-Preview), [ClientSubscriptionSelfServiceController.TerminationRequest](#m-ClientSubscriptionSelfServiceController-TerminationRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/subscription/controller/ClientSubscriptionSelfServiceController.java`, méthode `previewTermination`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-client-subscriptions-subscriptionnumber-terminations"></a>
### GET /sni/api/v1/client/subscriptions/{subscriptionNumber}/terminations

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<ClientSubscriptionSelfServiceController.TerminationView>`
- Modèles : [ClientSubscriptionSelfServiceController.TerminationView](#m-ClientSubscriptionSelfServiceController-TerminationView)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/subscription/controller/ClientSubscriptionSelfServiceController.java`, méthode `terminations`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-client-subscriptions-subscriptionnumber-timeline"></a>
### GET /sni/api/v1/client/subscriptions/{subscriptionNumber}/timeline

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<ClientHistoryService.Entry>`
- Modèles : [ClientHistoryService.Entry](#m-ClientHistoryService-Entry)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/subscription/controller/ClientSubscriptionSelfServiceController.java`, méthode `timeline`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-client-terminations-terminationcode-retract"></a>
### POST /sni/api/v1/client/terminations/{terminationCode}/retract

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (facultatif) : `ClientSubscriptionSelfServiceController.ReasonRequest`
- Réponse : `ClientSubscriptionSelfServiceController.TerminationView`
- Modèles : [ClientSubscriptionSelfServiceController.TerminationView](#m-ClientSubscriptionSelfServiceController-TerminationView), [ClientSubscriptionSelfServiceController.ReasonRequest](#m-ClientSubscriptionSelfServiceController-ReasonRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/subscription/controller/ClientSubscriptionSelfServiceController.java`, méthode `retract`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `terminationCode` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ClientHistoryService-Entry"></a>
#### ClientHistoryService.Entry

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `occurredAt` | date-heure ISO 8601 UTC |  |  |
| `kind` | enum : `SUBSCRIPTION`, `PAYMENT`, `CHANGE`, `FREEZE`, `TERMINATION`, `BENEFIT`, `PASS`, `USAGE` |  |  |
| `subscriptionNumber` | texte |  |  |
| `title` | texte |  |  |
| `detail` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `currency` | texte |  |  |

<a id="m-ClientSubscriptionSelfServiceController-QuoteView"></a>
#### ClientSubscriptionSelfServiceController.QuoteView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `quoteNumber` | texte |  |  |
| `status` | enum : `DRAFT`, `SENT`, `ACCEPTED`, `REJECTED`, `EXPIRED`, `CONVERTED` |  |  |
| `planCode` | texte |  |  |
| `planName` | texte |  |  |
| `billingCycle` | texte |  |  |
| `currency` | texte |  |  |
| `cataloguePrice` | nombre décimal |  |  |
| `quotedPrice` | nombre décimal |  |  |
| `setupFee` | nombre décimal |  |  |
| `commitmentMonths` | entier |  |  |
| `trialDays` | entier |  |  |
| `startDate` | date AAAA-MM-JJ |  |  |
| `validUntil` | date AAAA-MM-JJ |  |  |
| `notes` | texte |  |  |
| `convertedSubscriptionNumber` | texte |  |  |

<a id="m-ClientSubscriptionSelfServiceController-ReasonRequest"></a>
#### ClientSubscriptionSelfServiceController.ReasonRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte |  |  |

<a id="m-SubscriptionCommitmentService-EarlyTermination"></a>
#### SubscriptionCommitmentService.EarlyTermination

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `early` | booléen |  |  |
| `remainingMonths` | entier |  |  |
| `fee` | nombre décimal |  |  |
| `formula` | enum : `NONE`, `FIXED_FEE`, `PERCENT_OF_REMAINING`, `REMAINING_PERIODS` |  |  |
| `commitmentEnd` | date AAAA-MM-JJ |  |  |

<a id="m-ClientSubscriptionSelfServiceController-MandateView"></a>
#### ClientSubscriptionSelfServiceController.MandateView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `mandateCode` | texte |  |  |
| `walletNumber` | texte |  |  |
| `status` | enum : `ACTIVE`, `SUSPENDED`, `REVOKED` |  |  |
| `consentGivenAt` | date-heure ISO 8601 UTC |  |  |
| `maxAmountPerDebit` | nombre décimal |  |  |
| `lastDebitAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-ClientSubscriptionSelfServiceController-MandateStatusView"></a>
#### ClientSubscriptionSelfServiceController.MandateStatusView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `hasMandate` | booléen |  |  |
| `mandate` | [ClientSubscriptionSelfServiceController.MandateView](#m-ClientSubscriptionSelfServiceController-MandateView) |  |  |

<a id="m-ClientSubscriptionSelfServiceController-MandateRequest"></a>
#### ClientSubscriptionSelfServiceController.MandateRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `walletNumber` | texte | oui | longueur min `1` |
| `maxAmountPerDebit` | nombre décimal |  |  |

<a id="m-SubscriptionResponse"></a>
#### SubscriptionResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `subscriptionNumber` | texte |  |  |
| `subscriberType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` |  |  |
| `subscriberCode` | texte |  |  |
| `subscriberName` | texte |  |  |
| `planCode` | texte |  |  |
| `planName` | texte |  |  |
| `planVersion` | entier |  |  |
| `status` | enum : `DRAFT`, `PENDING_ACTIVATION`, `PENDING_DOCUMENTS`, `TRIALING`, `ACTIVE`, `GRACE_PERIOD`, `PAST_DUE`, `PAUSED`, `SUSPENDED`, `PENDING_TERMINATION`, `CANCELLED`, `EXPIRED` |  |  |
| `startDate` | date AAAA-MM-JJ |  |  |
| `currentPeriodStart` | date AAAA-MM-JJ |  |  |
| `currentPeriodEnd` | date AAAA-MM-JJ |  |  |
| `nextBillingDate` | date AAAA-MM-JJ |  |  |
| `autoRenew` | booléen |  |  |
| `billingCycle` | enum : `ONE_TIME`, `DAILY`, `WEEKLY`, `MONTHLY`, `QUARTERLY`, `YEARLY` |  |  |
| `currency` | texte |  |  |
| `subtotalAmount` | nombre décimal |  |  |
| `taxAmount` | nombre décimal |  |  |
| `totalAmount` | nombre décimal |  |  |
| `cancelAtPeriodEnd` | booléen |  |  |
| `pausedAt` | date-heure ISO 8601 UTC |  |  |
| `pauseUntil` | date AAAA-MM-JJ |  |  |
| `contractCode` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |
| `pendingRenewalInvoice` | texte |  |  |
| `pendingRenewalStart` | date AAAA-MM-JJ |  |  |
| `pendingRenewalEnd` | date AAAA-MM-JJ |  |  |

<a id="m-ClientSubscriptionSelfServiceController-FreezeRequest"></a>
#### ClientSubscriptionSelfServiceController.FreezeRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `until` | date AAAA-MM-JJ | oui |  |
| `reason` | texte |  |  |

<a id="m-ClientSubscriptionSelfService-FreezeTerms"></a>
#### ClientSubscriptionSelfService.FreezeTerms

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `freezesUsedThisYear` | entier |  |  |
| `freezesAllowedPerYear` | entier |  |  |
| `maxDays` | entier |  |  |
| `feePercent` | nombre décimal |  |  |
| `noticeDays` | entier |  |  |
| `canFreezeNow` | booléen |  |  |

<a id="m-SubscriptionChangeResponse"></a>
#### SubscriptionChangeResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `changeNumber` | texte |  |  |
| `subscriptionNumber` | texte |  |  |
| `changeType` | enum : `UPGRADE`, `DOWNGRADE`, `ADD_ADDON`, `REMOVE_ADDON`, `QUANTITY_CHANGE` |  |  |
| `currentPlanVersionId` | entier int64 ⚠ |  |  |
| `targetPlanVersionId` | entier int64 ⚠ |  |  |
| `effectivePolicy` | enum : `IMMEDIATE`, `NEXT_BILLING_PERIOD`, `CUSTOM_DATE` |  |  |
| `effectiveDate` | date AAAA-MM-JJ |  |  |
| `prorationAmount` | nombre décimal |  |  |
| `prorationPolicy` | enum : `DAILY`, `MONTH_STARTED`, `NONE` |  |  |
| `prorationCredit` | booléen |  |  |
| `prorationBillableNumber` | texte |  |  |
| `status` | enum : `REQUESTED`, `APPROVED`, `APPLIED`, `REJECTED`, `CANCELLED` |  |  |
| `reason` | texte |  |  |
| `requestedBy` | texte |  |  |
| `approvedBy` | texte |  |  |
| `appliedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-ClientSubscriptionSelfServiceController-PlanChangeRequest"></a>
#### ClientSubscriptionSelfServiceController.PlanChangeRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `targetPlanCode` | texte | oui | longueur min `1` |
| `atNextPeriod` | booléen |  |  |
| `reason` | texte |  |  |

<a id="m-ClientSubscriptionSelfService-PlanChangePreview"></a>
#### ClientSubscriptionSelfService.PlanChangePreview

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `targetPlanCode` | texte |  |  |
| `targetPlanName` | texte |  |  |
| `targetPeriodTotal` | nombre décimal |  |  |
| `currentPeriodTotal` | nombre décimal |  |  |
| `amount` | nombre décimal |  |  |
| `credit` | booléen |  |  |
| `prorationPolicy` | texte |  |  |
| `effectiveDate` | date AAAA-MM-JJ |  |  |
| `upgrade` | booléen |  |  |

<a id="m-ClientSubscriptionSelfServiceController-TerminationView"></a>
#### ClientSubscriptionSelfServiceController.TerminationView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `terminationCode` | texte |  |  |
| `status` | enum : `REQUESTED`, `ACCEPTED`, `RETRACTED`, `COMPLETED` |  |  |
| `requestedAt` | date-heure ISO 8601 UTC |  |  |
| `effectiveDate` | date AAAA-MM-JJ |  |  |
| `noticePeriodDays` | entier |  |  |
| `reasonCategory` | enum : `RELOCATION`, `COST`, `SERVICE_QUALITY`, `BUSINESS_CLOSURE`, `NO_LONGER_NEEDED`, `COMPETITOR`, `NON_PAYMENT`, `OTHER` |  |  |
| `earlyTermination` | booléen |  |  |
| `feeDue` | nombre décimal |  |  |
| `exitItemsPending` | texte[] |  |  |

<a id="m-ClientSubscriptionSelfServiceController-TerminationRequest"></a>
#### ClientSubscriptionSelfServiceController.TerminationRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reasonCategory` | enum : `RELOCATION`, `COST`, `SERVICE_QUALITY`, `BUSINESS_CLOSURE`, `NO_LONGER_NEEDED`, `COMPETITOR`, `NON_PAYMENT`, `OTHER` |  |  |
| `reason` | texte |  |  |
| `requestedEffectiveDate` | date AAAA-MM-JJ |  |  |

<a id="m-SubscriptionTerminationService-Preview"></a>
#### SubscriptionTerminationService.Preview

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `subscriptionNumber` | texte |  |  |
| `noticePeriodDays` | entier |  |  |
| `earliestEffectiveDate` | date AAAA-MM-JJ |  |  |
| `effectiveDate` | date AAAA-MM-JJ |  |  |
| `earlyTermination` | booléen |  |  |
| `commitmentEnd` | date AAAA-MM-JJ |  |  |
| `remainingCommitmentMonths` | entier |  |  |
| `feeAmount` | nombre décimal |  |  |
| `bridgingAmount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `exitChecklist` | texte[] |  |  |
