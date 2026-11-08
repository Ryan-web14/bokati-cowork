# SubscriptionTerminationController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [subscription](../README.md) · 12 route(s).

## Routes

<a id="r-get-sni-api-v1-subscriptions-terminations"></a>
### GET /sni/api/v1/subscriptions/terminations

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `List<SubscriptionTerminationController.TerminationView>`
- Modèles : [SubscriptionTerminationController.TerminationView](#m-SubscriptionTerminationController-TerminationView), [SubscriptionTermination.Status](#m-SubscriptionTermination-Status)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/controller/SubscriptionTerminationController.java`, méthode `byStatus`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `status` | requête | `SubscriptionTermination.Status` |  | REQUESTED |

<a id="r-get-sni-api-v1-subscriptions-terminations-blocked"></a>
### GET /sni/api/v1/subscriptions/terminations/blocked

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `List<SubscriptionTerminationController.TerminationView>`
- Modèles : [SubscriptionTerminationController.TerminationView](#m-SubscriptionTerminationController-TerminationView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/controller/SubscriptionTerminationController.java`, méthode `blocked`

<a id="r-get-sni-api-v1-subscriptions-terminations-subscriptions-subscriptionnumber"></a>
### GET /sni/api/v1/subscriptions/terminations/subscriptions/{subscriptionNumber}

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `List<SubscriptionTerminationController.TerminationView>`
- Modèles : [SubscriptionTerminationController.TerminationView](#m-SubscriptionTerminationController-TerminationView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/controller/SubscriptionTerminationController.java`, méthode `ofSubscription`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-subscriptions-terminations-subscriptions-subscriptionnumber"></a>
### POST /sni/api/v1/subscriptions/terminations/subscriptions/{subscriptionNumber}

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Corps JSON (obligatoire) : `SubscriptionTerminationController.TerminationRequest`
- Réponse : `SubscriptionTerminationController.TerminationView`
- Modèles : [SubscriptionTerminationController.TerminationView](#m-SubscriptionTerminationController-TerminationView), [SubscriptionTerminationController.TerminationRequest](#m-SubscriptionTerminationController-TerminationRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/controller/SubscriptionTerminationController.java`, méthode `request`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-subscriptions-terminations-subscriptions-subscriptionnumber-preview"></a>
### POST /sni/api/v1/subscriptions/terminations/subscriptions/{subscriptionNumber}/preview

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Corps JSON (obligatoire) : `SubscriptionTerminationController.TerminationRequest`
- Réponse : `SubscriptionTerminationService.Preview`
- Modèles : [SubscriptionTerminationService.Preview](#m-SubscriptionTerminationService-Preview), [SubscriptionTerminationController.TerminationRequest](#m-SubscriptionTerminationController-TerminationRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/controller/SubscriptionTerminationController.java`, méthode `preview`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-subscriptions-terminations-terminationcode"></a>
### GET /sni/api/v1/subscriptions/terminations/{terminationCode}

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `SubscriptionTerminationController.TerminationView`
- Modèles : [SubscriptionTerminationController.TerminationView](#m-SubscriptionTerminationController-TerminationView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/controller/SubscriptionTerminationController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `terminationCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-subscriptions-terminations-terminationcode-accept"></a>
### POST /sni/api/v1/subscriptions/terminations/{terminationCode}/accept

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `SubscriptionTerminationController.TerminationView`
- Modèles : [SubscriptionTerminationController.TerminationView](#m-SubscriptionTerminationController-TerminationView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/controller/SubscriptionTerminationController.java`, méthode `accept`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `terminationCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-subscriptions-terminations-terminationcode-complete"></a>
### POST /sni/api/v1/subscriptions/terminations/{terminationCode}/complete

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `SubscriptionTerminationController.TerminationView`
- Modèles : [SubscriptionTerminationController.TerminationView](#m-SubscriptionTerminationController-TerminationView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/controller/SubscriptionTerminationController.java`, méthode `complete`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `terminationCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-subscriptions-terminations-terminationcode-exit-items-itemcode-done"></a>
### POST /sni/api/v1/subscriptions/terminations/{terminationCode}/exit-items/{itemCode}/done

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Corps JSON (facultatif) : `SubscriptionTerminationController.ItemRequest`
- Réponse : `SubscriptionTerminationController.TerminationView`
- Modèles : [SubscriptionTerminationController.TerminationView](#m-SubscriptionTerminationController-TerminationView), [SubscriptionTerminationController.ItemRequest](#m-SubscriptionTerminationController-ItemRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/controller/SubscriptionTerminationController.java`, méthode `tick`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `terminationCode` | chemin | `String` | oui |  |
| `itemCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-subscriptions-terminations-terminationcode-exit-items-itemcode-waive"></a>
### POST /sni/api/v1/subscriptions/terminations/{terminationCode}/exit-items/{itemCode}/waive

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Corps JSON (obligatoire) : `SubscriptionTerminationController.ReasonRequest`
- Réponse : `SubscriptionTerminationController.TerminationView`
- Modèles : [SubscriptionTerminationController.TerminationView](#m-SubscriptionTerminationController-TerminationView), [SubscriptionTerminationController.ReasonRequest](#m-SubscriptionTerminationController-ReasonRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/controller/SubscriptionTerminationController.java`, méthode `waiveItem`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `terminationCode` | chemin | `String` | oui |  |
| `itemCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-subscriptions-terminations-terminationcode-retract"></a>
### POST /sni/api/v1/subscriptions/terminations/{terminationCode}/retract

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Corps JSON (facultatif) : `SubscriptionTerminationController.ReasonRequest`
- Réponse : `SubscriptionTerminationController.TerminationView`
- Modèles : [SubscriptionTerminationController.TerminationView](#m-SubscriptionTerminationController-TerminationView), [SubscriptionTerminationController.ReasonRequest](#m-SubscriptionTerminationController-ReasonRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/controller/SubscriptionTerminationController.java`, méthode `retract`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `terminationCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-subscriptions-terminations-terminationcode-waive-fee"></a>
### POST /sni/api/v1/subscriptions/terminations/{terminationCode}/waive-fee

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Corps JSON (obligatoire) : `SubscriptionTerminationController.ReasonRequest`
- Réponse : `SubscriptionTerminationController.TerminationView`
- Modèles : [SubscriptionTerminationController.TerminationView](#m-SubscriptionTerminationController-TerminationView), [SubscriptionTerminationController.ReasonRequest](#m-SubscriptionTerminationController-ReasonRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/controller/SubscriptionTerminationController.java`, méthode `waiveFee`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `terminationCode` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-SubscriptionTerminationController-TerminationView"></a>
#### SubscriptionTerminationController.TerminationView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `terminationCode` | texte |  |  |
| `subscriptionNumber` | texte |  |  |
| `status` | enum : `REQUESTED`, `ACCEPTED`, `RETRACTED`, `COMPLETED` |  |  |
| `requestedAt` | date-heure ISO 8601 UTC |  |  |
| `requestedBy` | texte |  |  |
| `channel` | enum : `STAFF`, `PORTAL`, `LETTER`, `EMAIL` |  |  |
| `noticePeriodDays` | entier |  |  |
| `effectiveDate` | date AAAA-MM-JJ |  |  |
| `reasonCategory` | enum : `RELOCATION`, `COST`, `SERVICE_QUALITY`, `BUSINESS_CLOSURE`, `NO_LONGER_NEEDED`, `COMPETITOR`, `NON_PAYMENT`, `OTHER` |  |  |
| `reason` | texte |  |  |
| `earlyTermination` | booléen |  |  |
| `remainingCommitmentMonths` | entier |  |  |
| `feeAmount` | nombre décimal |  |  |
| `feeDue` | nombre décimal |  |  |
| `feeWaived` | booléen |  |  |
| `feeWaivedBy` | texte |  |  |
| `feeWaivedReason` | texte |  |  |
| `feeBillableNumber` | texte |  |  |
| `bridgingBillableNumber` | texte |  |  |
| `acceptedBy` | texte |  |  |
| `acceptedAt` | date-heure ISO 8601 UTC |  |  |
| `retractedBy` | texte |  |  |
| `retractedAt` | date-heure ISO 8601 UTC |  |  |
| `completedAt` | date-heure ISO 8601 UTC |  |  |
| `exitChecklistClear` | booléen |  |  |
| `exitItems` | [SubscriptionTerminationController.ExitItemView](#m-SubscriptionTerminationController-ExitItemView)[] |  |  |
| `notes` | texte |  |  |

<a id="m-SubscriptionTermination-Status"></a>
#### SubscriptionTermination.Status

Énumération : `REQUESTED`, `ACCEPTED`, `RETRACTED`, `COMPLETED`

<a id="m-SubscriptionTerminationController-TerminationRequest"></a>
#### SubscriptionTerminationController.TerminationRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reasonCategory` | enum : `RELOCATION`, `COST`, `SERVICE_QUALITY`, `BUSINESS_CLOSURE`, `NO_LONGER_NEEDED`, `COMPETITOR`, `NON_PAYMENT`, `OTHER` | oui |  |
| `reason` | texte |  |  |
| `channel` | enum : `STAFF`, `PORTAL`, `LETTER`, `EMAIL` |  |  |
| `requestedEffectiveDate` | date AAAA-MM-JJ |  |  |
| `noticePeriodDays` | entier |  |  |

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

<a id="m-SubscriptionTerminationController-ItemRequest"></a>
#### SubscriptionTerminationController.ItemRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `detail` | texte |  |  |

<a id="m-SubscriptionTerminationController-ReasonRequest"></a>
#### SubscriptionTerminationController.ReasonRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte | oui | longueur min `1` |

<a id="m-SubscriptionTerminationController-ExitItemView"></a>
#### SubscriptionTerminationController.ExitItemView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCode` | texte |  |  |
| `label` | texte |  |  |
| `mandatory` | booléen |  |  |
| `status` | enum : `PENDING`, `DONE`, `WAIVED` |  |  |
| `detail` | texte |  |  |
| `doneBy` | texte |  |  |
| `doneAt` | date-heure ISO 8601 UTC |  |  |
