# PaymentReconciliationController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [payment](../README.md) · 2 route(s).

## Routes

<a id="r-post-sni-api-v1-payments-reconciliation-batches"></a>
### POST /sni/api/v1/payments/reconciliation-batches

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS`
- Corps JSON (obligatoire) : `CreateReconciliationBatchRequest`
- Réponse : `ReconciliationBatchResponse`
- Modèles : [ReconciliationBatchResponse](#m-ReconciliationBatchResponse), [CreateReconciliationBatchRequest](#m-CreateReconciliationBatchRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/PaymentReconciliationController.java`, méthode `create`

<a id="r-patch-sni-api-v1-payments-reconciliation-batches-batchnumber-complete"></a>
### PATCH /sni/api/v1/payments/reconciliation-batches/{batchNumber}/complete

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS`
- Réponse : `ReconciliationBatchResponse`
- Modèles : [ReconciliationBatchResponse](#m-ReconciliationBatchResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/PaymentReconciliationController.java`, méthode `complete`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `batchNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ReconciliationBatchResponse"></a>
#### ReconciliationBatchResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `batchNumber` | texte |  |  |
| `provider` | texte |  |  |
| `status` | enum : `OPEN`, `COMPLETED`, `FAILED` |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `completedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-CreateReconciliationBatchRequest"></a>
#### CreateReconciliationBatchRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `provider` | texte | oui | longueur min `1` |
| `createdBy` | texte |  |  |
