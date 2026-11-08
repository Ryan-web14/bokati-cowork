# MobileMoneyOperationsController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [payment](../README.md) · 5 route(s).

## Routes

<a id="r-get-sni-api-v1-payments-mobile-money-operations"></a>
### GET /sni/api/v1/payments/mobile-money/operations

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Réponse : `PaginatedResponse<MobileMoneyDepositResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [MobileMoneyDepositResponse](#m-MobileMoneyDepositResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/MobileMoneyOperationsController.java`, méthode `search`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `LocalDate` |  |  |
| `to` | requête | `LocalDate` |  |  |
| `status` | requête | `String` |  |  |
| `provider` | requête | `String` |  |  |
| `customerCode` | requête | `String` |  |  |
| `phone` | requête | `String` |  |  |
| `reference` | requête | `String` |  |  |
| `page` | requête | `int` |  | 0 |
| `size` | requête | `int` |  | 20 |

<a id="r-get-sni-api-v1-payments-mobile-money-operations-reconciliation"></a>
### GET /sni/api/v1/payments/mobile-money/operations/reconciliation

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Réponse : `MobileMoneyOperationsService.Reconciliation`
- Modèles : [MobileMoneyOperationsService.Reconciliation](#m-MobileMoneyOperationsService-Reconciliation)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/MobileMoneyOperationsController.java`, méthode `reconciliation`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `LocalDate` |  |  |
| `to` | requête | `LocalDate` |  |  |

<a id="r-get-sni-api-v1-payments-mobile-money-operations-summary"></a>
### GET /sni/api/v1/payments/mobile-money/operations/summary

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Réponse : `List<MobileMoneyOperationsService.StatusTotal>`
- Modèles : [MobileMoneyOperationsService.StatusTotal](#m-MobileMoneyOperationsService-StatusTotal)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/MobileMoneyOperationsController.java`, méthode `summary`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `LocalDate` |  |  |
| `to` | requête | `LocalDate` |  |  |

<a id="r-get-sni-api-v1-payments-mobile-money-operations-depositid"></a>
### GET /sni/api/v1/payments/mobile-money/operations/{depositId}

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Réponse : `MobileMoneyOperationsService.Detail`
- Modèles : [MobileMoneyOperationsService.Detail](#m-MobileMoneyOperationsService-Detail)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/MobileMoneyOperationsController.java`, méthode `detail`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `depositId` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-payments-mobile-money-operations-depositid-resolve"></a>
### POST /sni/api/v1/payments/mobile-money/operations/{depositId}/resolve

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Corps JSON (obligatoire) : `MobileMoneyOperationsController.ResolveRequest`
- Réponse : `MobileMoneyOperationsService.Detail`
- Modèles : [MobileMoneyOperationsService.Detail](#m-MobileMoneyOperationsService-Detail), [MobileMoneyOperationsController.ResolveRequest](#m-MobileMoneyOperationsController-ResolveRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/MobileMoneyOperationsController.java`, méthode `resolve`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `depositId` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-MobileMoneyDepositResponse"></a>
#### MobileMoneyDepositResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `depositId` | texte |  |  |
| `clientReferenceId` | texte |  |  |
| `customerMessage` | texte |  |  |
| `metadata` | objet { clé : objet libre }[] |  |  |
| `payer` | [MobileMoneyDepositResponse.PayerResponse](#m-MobileMoneyDepositResponse-PayerResponse) |  |  |
| `provider` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `status` | texte |  |  |
| `phase` | texte |  |  |
| `providerMessage` | texte |  |  |
| `failureReason` | texte |  |  |
| `failureCode` | texte |  |  |
| `userMessage` | texte |  |  |
| `retryable` | booléen |  |  |
| `nextStatusCheckAt` | date-heure ISO 8601 UTC |  |  |
| `intentNumber` | texte |  |  |
| `transactionNumber` | texte |  |  |
| `customerType` | texte |  |  |
| `customerCode` | texte |  |  |
| `callbackUrl` | texte |  |  |
| `requestPayloadJson` | texte |  |  |
| `providerResponseJson` | texte |  |  |
| `lastStatusCheckedAt` | date-heure ISO 8601 UTC |  |  |
| `completedAt` | date-heure ISO 8601 UTC |  |  |
| `failedAt` | date-heure ISO 8601 UTC |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |
| `transaction` | [PaymentTransactionResponse](#m-PaymentTransactionResponse) |  |  |

<a id="m-MobileMoneyOperationsService-Reconciliation"></a>
#### MobileMoneyOperationsService.Reconciliation

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `from` | date-heure ISO 8601 UTC |  |  |
| `to` | date-heure ISO 8601 UTC |  |  |
| `checked` | entier |  |  |
| `truncated` | booléen |  |  |
| `discrepancies` | [MobileMoneyOperationsService.ReconciliationLine](#m-MobileMoneyOperationsService-ReconciliationLine)[] |  |  |

<a id="m-MobileMoneyOperationsService-StatusTotal"></a>
#### MobileMoneyOperationsService.StatusTotal

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `status` | texte |  |  |
| `currency` | texte |  |  |
| `count` | entier int64 ⚠ |  |  |
| `amount` | nombre décimal |  |  |

<a id="m-MobileMoneyOperationsService-Detail"></a>
#### MobileMoneyOperationsService.Detail

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `deposit` | [MobileMoneyDepositResponse](#m-MobileMoneyDepositResponse) |  |  |
| `intentStatus` | texte |  |  |
| `transactionStatus` | texte |  |  |
| `provider` | [MobileMoneyOperationsService.ProviderView](#m-MobileMoneyOperationsService-ProviderView) |  |  |
| `discrepancies` | [MobileMoneyOperationsService.Discrepancy](#m-MobileMoneyOperationsService-Discrepancy)[] |  |  |
| `callbacks` | [MobileMoneyCallbackResponse](#m-MobileMoneyCallbackResponse)[] |  |  |

<a id="m-MobileMoneyOperationsController-ResolveRequest"></a>
#### MobileMoneyOperationsController.ResolveRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `action` | enum : `SYNC`, `MARK_SUCCEEDED`, `MARK_FAILED` |  |  |
| `reason` | texte |  |  |
| `providerTransactionId` | texte |  |  |

<a id="m-MobileMoneyDepositResponse-PayerResponse"></a>
#### MobileMoneyDepositResponse.PayerResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `type` | texte |  |  |
| `accountDetails` | [MobileMoneyDepositResponse.AccountDetailsResponse](#m-MobileMoneyDepositResponse-AccountDetailsResponse) |  |  |

<a id="m-PaymentTransactionResponse"></a>
#### PaymentTransactionResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `transactionNumber` | texte |  |  |
| `intentNumber` | texte |  |  |
| `paymentMethod` | enum : `CASH`, `WALLET`, `MOBILE_MONEY`, `BANK_TRANSFER`, `CARD`, `CHEQUE`, `MANUAL_ADJUSTMENT`, `CREDIT_NOTE` |  |  |
| `provider` | texte |  |  |
| `providerReference` | texte |  |  |
| `receiptNumber` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `status` | enum : `PENDING`, `PROCESSING`, `SUCCEEDED`, `FAILED`, `CANCELLED`, `REFUNDED`, `REVERSED` |  |  |
| `paidAt` | date-heure ISO 8601 UTC |  |  |
| `receivedBy` | texte |  |  |
| `failureReason` | texte |  |  |
| `metadataJson` | texte |  |  |
| `channel` | enum : `SELF_SERVICE`, `BACK_OFFICE`, `SYSTEM` |  |  |

<a id="m-MobileMoneyOperationsService-ReconciliationLine"></a>
#### MobileMoneyOperationsService.ReconciliationLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `deposit` | [MobileMoneyDepositResponse](#m-MobileMoneyDepositResponse) |  |  |
| `provider` | [MobileMoneyOperationsService.ProviderView](#m-MobileMoneyOperationsService-ProviderView) |  |  |
| `discrepancies` | [MobileMoneyOperationsService.Discrepancy](#m-MobileMoneyOperationsService-Discrepancy)[] |  |  |

<a id="m-MobileMoneyOperationsService-ProviderView"></a>
#### MobileMoneyOperationsService.ProviderView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `available` | booléen |  |  |
| `found` | booléen |  |  |
| `status` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `providerTransactionId` | texte |  |  |
| `failureCode` | texte |  |  |
| `failureMessage` | texte |  |  |
| `error` | texte |  |  |

<a id="m-MobileMoneyOperationsService-Discrepancy"></a>
#### MobileMoneyOperationsService.Discrepancy

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `severity` | texte |  |  |
| `code` | texte |  |  |
| `message` | texte |  |  |
| `suggestedAction` | texte |  |  |

<a id="m-MobileMoneyCallbackResponse"></a>
#### MobileMoneyCallbackResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `kind` | texte |  |  |
| `referenceId` | texte |  |  |
| `reportedStatus` | texte |  |  |
| `signaturePresent` | booléen |  |  |
| `verifiedVia` | texte |  |  |
| `outcome` | texte |  |  |
| `detail` | texte |  |  |
| `remoteAddress` | texte |  |  |
| `receivedAt` | date-heure ISO 8601 UTC |  |  |
| `processedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-MobileMoneyDepositResponse-AccountDetailsResponse"></a>
#### MobileMoneyDepositResponse.AccountDetailsResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `phoneNumber` | texte |  |  |
| `provider` | texte |  |  |
