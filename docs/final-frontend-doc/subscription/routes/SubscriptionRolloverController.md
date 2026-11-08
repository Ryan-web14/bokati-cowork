# SubscriptionRolloverController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [subscription](../README.md) · 2 route(s).

## Routes

<a id="r-get-sni-api-v1-subscription-rollovers"></a>
### GET /sni/api/v1/subscription-rollovers

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ`
- Réponse : `List<RolloverRecordResponse>`
- Modèles : [RolloverRecordResponse](#m-RolloverRecordResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/rollover/controller/SubscriptionRolloverController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | requête | `String` |  |  |
| `entitlementCode` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-subscription-rollovers-apply-due"></a>
### POST /sni/api/v1/subscription-rollovers/apply-due

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE`
- Réponse : `Integer`
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/rollover/controller/SubscriptionRolloverController.java`, méthode `applyDue`

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-RolloverRecordResponse"></a>
#### RolloverRecordResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `rolloverNumber` | texte |  |  |
| `subscriptionNumber` | texte |  |  |
| `sourceGrantNumber` | texte |  |  |
| `rolloverGrantNumber` | texte |  |  |
| `entitlementCode` | texte |  |  |
| `quantity` | nombre décimal |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
