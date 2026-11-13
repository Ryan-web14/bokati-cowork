# SubscriptionBillingLinkController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [subscription](../README.md) · 1 route(s).

## Routes

<a id="r-post-sni-api-v1-subscriptions-subscriptionnumber-activate-with-billing"></a>
### POST /sni/api/v1/subscriptions/{subscriptionNumber}/activate-with-billing

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:ACTIVATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Corps JSON (obligatoire) : `SubscriptionBillingLinkController.LinkRequest`
- Réponse : `SubscriptionBillingLinkService.LinkResult`
- Modèles : [SubscriptionBillingLinkService.LinkResult](#m-SubscriptionBillingLinkService-LinkResult), [SubscriptionBillingLinkController.LinkRequest](#m-SubscriptionBillingLinkController-LinkRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/SubscriptionBillingLinkController.java`, méthode `link`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-SubscriptionBillingLinkService-LinkResult"></a>
#### SubscriptionBillingLinkService.LinkResult

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `subscriptionNumber` | texte |  |  |
| `status` | enum : `DRAFT`, `PENDING_ACTIVATION`, `PENDING_DOCUMENTS`, `TRIALING`, `ACTIVE`, `GRACE_PERIOD`, `PAST_DUE`, `PAUSED`, `SUSPENDED`, `PENDING_TERMINATION`, `CANCELLED`, `EXPIRED` |  |  |
| `link` | [SubscriptionBillingLinkService.Link](#m-SubscriptionBillingLinkService-Link) |  |  |
| `cancelledInvoices` | texte[] |  |  |
| `warnings` | texte[] |  |  |

<a id="m-SubscriptionBillingLinkController-LinkRequest"></a>
#### SubscriptionBillingLinkController.LinkRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `invoiceNumber` | texte |  |  |
| `paymentNumber` | texte |  |  |
| `activate` | booléen |  |  |
| `force` | booléen |  |  |

<a id="m-SubscriptionBillingLinkService-Link"></a>
#### SubscriptionBillingLinkService.Link

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `invoiceNumber` | texte |  |  |
| `paymentNumber` | texte |  |  |
| `invoiceTotal` | nombre décimal |  |  |
| `balanceDue` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `paid` | booléen |  |  |
| `warnings` | texte[] |  |  |
