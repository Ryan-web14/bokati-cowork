# CashPaymentDeclarationController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [payment](../README.md) · 4 route(s).

## Routes

<a id="r-get-sni-api-v1-payments-cash-declarations"></a>
### GET /sni/api/v1/payments/cash-declarations

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER') or hasRole('CASHIER')`
- Réponse : `PaginatedResponse<CashPaymentDeclarationResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [CashPaymentDeclarationResponse](#m-CashPaymentDeclarationResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/cash/controller/CashPaymentDeclarationController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `status` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-get-sni-api-v1-payments-cash-declarations-declarationnumber"></a>
### GET /sni/api/v1/payments/cash-declarations/{declarationNumber}

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER') or hasRole('CASHIER')`
- Réponse : `CashPaymentDeclarationResponse`
- Modèles : [CashPaymentDeclarationResponse](#m-CashPaymentDeclarationResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/cash/controller/CashPaymentDeclarationController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `declarationNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-payments-cash-declarations-declarationnumber-cancel"></a>
### POST /sni/api/v1/payments/cash-declarations/{declarationNumber}/cancel

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER') or hasRole('CASHIER')`
- Corps JSON (facultatif) : `CloseCashDeclarationRequest`
- Réponse : `CashPaymentDeclarationResponse`
- Modèles : [CashPaymentDeclarationResponse](#m-CashPaymentDeclarationResponse), [CloseCashDeclarationRequest](#m-CloseCashDeclarationRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/cash/controller/CashPaymentDeclarationController.java`, méthode `cancel`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `declarationNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-payments-cash-declarations-declarationnumber-confirm"></a>
### POST /sni/api/v1/payments/cash-declarations/{declarationNumber}/confirm

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER') or hasRole('CASHIER')`
- Corps JSON (obligatoire) : `ConfirmCashPaymentRequest`
- Réponse : `CashPaymentDeclarationResponse`
- Modèles : [CashPaymentDeclarationResponse](#m-CashPaymentDeclarationResponse), [ConfirmCashPaymentRequest](#m-ConfirmCashPaymentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/cash/controller/CashPaymentDeclarationController.java`, méthode `confirm`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `declarationNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-CashPaymentDeclarationResponse"></a>
#### CashPaymentDeclarationResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `declarationNumber` | texte |  |  |
| `documentNumber` | texte |  |  |
| `bookingNumber` | texte |  |  |
| `customerType` | texte |  |  |
| `customerCode` | texte |  |  |
| `customerName` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `status` | enum : `AWAITING_CONFIRMATION`, `CONFIRMED`, `CANCELLED`, `EXPIRED` |  |  |
| `note` | texte |  |  |
| `declaredAt` | date-heure ISO 8601 UTC |  |  |
| `declaredBy` | texte |  |  |
| `expiresAt` | date-heure ISO 8601 UTC |  |  |
| `confirmedAt` | date-heure ISO 8601 UTC |  |  |
| `confirmedBy` | texte |  |  |
| `confirmedAmount` | nombre décimal |  |  |
| `cashSessionNumber` | texte |  |  |
| `transactionNumber` | texte |  |  |
| `closedAt` | date-heure ISO 8601 UTC |  |  |
| `closedBy` | texte |  |  |
| `closeReason` | texte |  |  |

<a id="m-CloseCashDeclarationRequest"></a>
#### CloseCashDeclarationRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte |  | longueur min `0`, longueur max `500` |

<a id="m-ConfirmCashPaymentRequest"></a>
#### ConfirmCashPaymentRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `cashSessionNumber` | texte | oui | longueur min `1` |
| `amount` | nombre décimal |  |  |
| `receivedBy` | texte |  |  |
| `note` | texte |  |  |
