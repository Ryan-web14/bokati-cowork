# SubscriptionQuoteController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [subscription](../README.md) · 8 route(s).

## Routes

<a id="r-get-sni-api-v1-subscriptions-quotes"></a>
### GET /sni/api/v1/subscriptions/quotes

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `PaginatedResponse<SubscriptionQuoteController.QuoteView>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [SubscriptionQuoteController.QuoteView](#m-SubscriptionQuoteController-QuoteView), [SubscriptionQuote.Status](#m-SubscriptionQuote-Status)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/controller/SubscriptionQuoteController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `status` | requête | `SubscriptionQuote.Status` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-post-sni-api-v1-subscriptions-quotes"></a>
### POST /sni/api/v1/subscriptions/quotes

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Corps JSON (obligatoire) : `SubscriptionQuoteController.QuoteRequest`
- Réponse : `SubscriptionQuoteController.QuoteView`
- Modèles : [SubscriptionQuoteController.QuoteView](#m-SubscriptionQuoteController-QuoteView), [SubscriptionQuoteController.QuoteRequest](#m-SubscriptionQuoteController-QuoteRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/controller/SubscriptionQuoteController.java`, méthode `create`

<a id="r-get-sni-api-v1-subscriptions-quotes-subscribers-subscribertype-subscribercode"></a>
### GET /sni/api/v1/subscriptions/quotes/subscribers/{subscriberType}/{subscriberCode}

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `List<SubscriptionQuoteController.QuoteView>`
- Modèles : [SubscriptionQuoteController.QuoteView](#m-SubscriptionQuoteController-QuoteView), [SubscriberType](#m-SubscriberType)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/controller/SubscriptionQuoteController.java`, méthode `ofSubscriber`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriberType` | chemin | `SubscriberType` | oui |  |
| `subscriberCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-subscriptions-quotes-quotenumber"></a>
### GET /sni/api/v1/subscriptions/quotes/{quoteNumber}

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `SubscriptionQuoteController.QuoteView`
- Modèles : [SubscriptionQuoteController.QuoteView](#m-SubscriptionQuoteController-QuoteView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/controller/SubscriptionQuoteController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `quoteNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-subscriptions-quotes-quotenumber-accept"></a>
### POST /sni/api/v1/subscriptions/quotes/{quoteNumber}/accept

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `SubscriptionQuoteController.QuoteView`
- Modèles : [SubscriptionQuoteController.QuoteView](#m-SubscriptionQuoteController-QuoteView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/controller/SubscriptionQuoteController.java`, méthode `accept`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `quoteNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-subscriptions-quotes-quotenumber-outlook"></a>
### GET /sni/api/v1/subscriptions/quotes/{quoteNumber}/outlook

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `SubscriptionQuoteService.Outlook`
- Modèles : [SubscriptionQuoteService.Outlook](#m-SubscriptionQuoteService-Outlook)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/controller/SubscriptionQuoteController.java`, méthode `outlook`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `quoteNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-subscriptions-quotes-quotenumber-reject"></a>
### POST /sni/api/v1/subscriptions/quotes/{quoteNumber}/reject

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Corps JSON (facultatif) : `SubscriptionQuoteController.ReasonRequest`
- Réponse : `SubscriptionQuoteController.QuoteView`
- Modèles : [SubscriptionQuoteController.QuoteView](#m-SubscriptionQuoteController-QuoteView), [SubscriptionQuoteController.ReasonRequest](#m-SubscriptionQuoteController-ReasonRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/controller/SubscriptionQuoteController.java`, méthode `reject`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `quoteNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-subscriptions-quotes-quotenumber-send"></a>
### POST /sni/api/v1/subscriptions/quotes/{quoteNumber}/send

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `SubscriptionQuoteController.QuoteView`
- Modèles : [SubscriptionQuoteController.QuoteView](#m-SubscriptionQuoteController-QuoteView)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/controller/SubscriptionQuoteController.java`, méthode `send`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `quoteNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-SubscriptionQuoteController-QuoteView"></a>
#### SubscriptionQuoteController.QuoteView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `quoteNumber` | texte |  |  |
| `status` | enum : `DRAFT`, `SENT`, `ACCEPTED`, `REJECTED`, `EXPIRED`, `CONVERTED` |  |  |
| `subscriberType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` |  |  |
| `subscriberCode` | texte |  |  |
| `subscriberName` | texte |  |  |
| `subscriberEmail` | texte |  |  |
| `planCode` | texte |  |  |
| `planName` | texte |  |  |
| `planVersionNumber` | entier |  |  |
| `billingCycle` | enum : `ONE_TIME`, `DAILY`, `WEEKLY`, `MONTHLY`, `QUARTERLY`, `YEARLY` |  |  |
| `currency` | texte |  |  |
| `cataloguePrice` | nombre décimal |  |  |
| `quotedPrice` | nombre décimal |  |  |
| `negotiated` | booléen |  |  |
| `setupFee` | nombre décimal |  |  |
| `commitmentMonths` | entier |  |  |
| `trialDays` | entier |  |  |
| `startDate` | date AAAA-MM-JJ |  |  |
| `validUntil` | date AAAA-MM-JJ |  |  |
| `notes` | texte |  |  |
| `preparedBy` | texte |  |  |
| `sentAt` | date-heure ISO 8601 UTC |  |  |
| `acceptedAt` | date-heure ISO 8601 UTC |  |  |
| `acceptedBy` | texte |  |  |
| `rejectedAt` | date-heure ISO 8601 UTC |  |  |
| `rejectionReason` | texte |  |  |
| `convertedSubscriptionNumber` | texte |  |  |
| `derivationCode` | texte |  |  |
| `proposalNumber` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-SubscriptionQuote-Status"></a>
#### SubscriptionQuote.Status

Énumération : `DRAFT`, `SENT`, `ACCEPTED`, `REJECTED`, `EXPIRED`, `CONVERTED`

<a id="m-SubscriptionQuoteController-QuoteRequest"></a>
#### SubscriptionQuoteController.QuoteRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `subscriberType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` | oui |  |
| `subscriberCode` | texte | oui | longueur min `1` |
| `planCode` | texte | oui | longueur min `1` |
| `planVersionId` | texte |  |  |
| `billingCycle` | enum : `ONE_TIME`, `DAILY`, `WEEKLY`, `MONTHLY`, `QUARTERLY`, `YEARLY` |  |  |
| `quotedPrice` | nombre décimal |  |  |
| `setupFee` | nombre décimal |  |  |
| `commitmentMonths` | entier |  |  |
| `trialDays` | entier |  |  |
| `startDate` | date AAAA-MM-JJ |  |  |
| `validUntil` | date AAAA-MM-JJ |  |  |
| `notes` | texte |  |  |
| `proposalNumber` | texte |  |  |

<a id="m-SubscriberType"></a>
#### SubscriberType

Énumération : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY`

<a id="m-SubscriptionQuoteService-Outlook"></a>
#### SubscriptionQuoteService.Outlook

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `quoteNumber` | texte |  |  |
| `cataloguePrice` | nombre décimal |  |  |
| `quotedPrice` | nombre décimal |  |  |
| `discountPercent` | nombre décimal |  |  |
| `negotiated` | booléen |  |  |
| `requiresApproval` | booléen |  |  |
| `blockingReason` | texte |  |  |
| `commitmentMonths` | entier |  |  |
| `validUntil` | date AAAA-MM-JJ |  |  |

<a id="m-SubscriptionQuoteController-ReasonRequest"></a>
#### SubscriptionQuoteController.ReasonRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte |  |  |
