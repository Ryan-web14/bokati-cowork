# CompanyWalletController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [company](../README.md) · 7 route(s).

## Routes

<a id="r-get-sni-api-v1-companies-customerid-spending-authorizations"></a>
### GET /sni/api/v1/companies/{customerId}/spending-authorizations

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Réponse : `List<CompanyWalletDtos.AuthorizationView>`
- Modèles : [CompanyWalletDtos.AuthorizationView](#m-CompanyWalletDtos-AuthorizationView)
- Code : `src/main/java/com/sni/bokaticowork/features/company/wallet/controller/CompanyWalletController.java`, méthode `authorizations`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-companies-customerid-spending-authorizations"></a>
### POST /sni/api/v1/companies/{customerId}/spending-authorizations

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Corps JSON (obligatoire) : `CompanyWalletDtos.AuthorizationRequest`
- Réponse : `CompanyWalletDtos.AuthorizationView`
- Modèles : [CompanyWalletDtos.AuthorizationView](#m-CompanyWalletDtos-AuthorizationView), [CompanyWalletDtos.AuthorizationRequest](#m-CompanyWalletDtos-AuthorizationRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/company/wallet/controller/CompanyWalletController.java`, méthode `authorize`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-companies-customerid-spending-authorizations-authorizationid"></a>
### PATCH /sni/api/v1/companies/{customerId}/spending-authorizations/{authorizationId}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Corps JSON (obligatoire) : `CompanyWalletDtos.AuthorizationUpdateRequest`
- Réponse : `CompanyWalletDtos.AuthorizationView`
- Modèles : [CompanyWalletDtos.AuthorizationView](#m-CompanyWalletDtos-AuthorizationView), [CompanyWalletDtos.AuthorizationUpdateRequest](#m-CompanyWalletDtos-AuthorizationUpdateRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/company/wallet/controller/CompanyWalletController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `authorizationId` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-companies-customerid-spending-authorizations-authorizationid-pin-reset"></a>
### POST /sni/api/v1/companies/{customerId}/spending-authorizations/{authorizationId}/pin/reset

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Réponse : `CompanyWalletDtos.AuthorizationView`
- Modèles : [CompanyWalletDtos.AuthorizationView](#m-CompanyWalletDtos-AuthorizationView)
- Code : `src/main/java/com/sni/bokaticowork/features/company/wallet/controller/CompanyWalletController.java`, méthode `resetPin`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `authorizationId` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-companies-customerid-wallet"></a>
### GET /sni/api/v1/companies/{customerId}/wallet

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Réponse : `CompanyWalletDtos.WalletView`
- Modèles : [CompanyWalletDtos.WalletView](#m-CompanyWalletDtos-WalletView)
- Code : `src/main/java/com/sni/bokaticowork/features/company/wallet/controller/CompanyWalletController.java`, méthode `wallet`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `currency` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-companies-customerid-wallet-spends"></a>
### GET /sni/api/v1/companies/{customerId}/wallet/spends

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Réponse : `List<CompanyWalletDtos.SpendView>`
- Modèles : [CompanyWalletDtos.SpendView](#m-CompanyWalletDtos-SpendView), [CompanyWalletSpend.Status](#m-CompanyWalletSpend-Status)
- Code : `src/main/java/com/sni/bokaticowork/features/company/wallet/controller/CompanyWalletController.java`, méthode `spends`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `currency` | requête | `String` |  |  |
| `status` | requête | `CompanyWalletSpend.Status` |  |  |

<a id="r-post-sni-api-v1-companies-customerid-wallet-top-ups"></a>
### POST /sni/api/v1/companies/{customerId}/wallet/top-ups

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Corps JSON (obligatoire) : `CompanyWalletDtos.AdminTopUpRequest`
- Réponse : `CompanyWalletDtos.AdminTopUpResult`
- Modèles : [CompanyWalletDtos.AdminTopUpResult](#m-CompanyWalletDtos-AdminTopUpResult), [CompanyWalletDtos.AdminTopUpRequest](#m-CompanyWalletDtos-AdminTopUpRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/company/wallet/controller/CompanyWalletController.java`, méthode `topUp`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-CompanyWalletDtos-AuthorizationView"></a>
#### CompanyWalletDtos.AuthorizationView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `companyCustomerCode` | texte |  |  |
| `walletNumber` | texte |  |  |
| `memberId` | texte |  |  |
| `memberName` | texte |  |  |
| `operations` | enum : `BOOKING`, `SUBSCRIPTION`, `PASS`, `INVOICE_PAYMENT`, `MARKETPLACE`[] |  |  |
| `maxPerTransaction` | nombre décimal |  |  |
| `maxPerDay` | nombre décimal |  |  |
| `maxPerMonth` | nombre décimal |  |  |
| `approvalAbove` | nombre décimal |  |  |
| `validFrom` | date-heure ISO 8601 UTC |  |  |
| `validUntil` | date-heure ISO 8601 UTC |  |  |
| `status` | enum : `ACTIVE`, `SUSPENDED`, `REVOKED` |  |  |
| `statusReason` | texte |  |  |
| `inForce` | booléen |  |  |
| `pinSet` | booléen |  |  |
| `engagedToday` | nombre décimal |  |  |
| `engagedThisMonth` | nombre décimal |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-CompanyWalletDtos-AuthorizationRequest"></a>
#### CompanyWalletDtos.AuthorizationRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `memberId` | texte | oui | longueur min `1` |
| `operations` | enum : `BOOKING`, `SUBSCRIPTION`, `PASS`, `INVOICE_PAYMENT`, `MARKETPLACE`[] | oui | éléments min `1` |
| `maxPerTransaction` | nombre décimal |  |  |
| `maxPerDay` | nombre décimal |  |  |
| `maxPerMonth` | nombre décimal |  |  |
| `approvalAbove` | nombre décimal |  |  |
| `validFrom` | date-heure ISO 8601 UTC |  |  |
| `validUntil` | date-heure ISO 8601 UTC |  |  |
| `currency` | texte |  |  |

<a id="m-CompanyWalletDtos-AuthorizationUpdateRequest"></a>
#### CompanyWalletDtos.AuthorizationUpdateRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `operations` | enum : `BOOKING`, `SUBSCRIPTION`, `PASS`, `INVOICE_PAYMENT`, `MARKETPLACE`[] | oui | éléments min `1` |
| `maxPerTransaction` | nombre décimal |  |  |
| `maxPerDay` | nombre décimal |  |  |
| `maxPerMonth` | nombre décimal |  |  |
| `approvalAbove` | nombre décimal |  |  |
| `validFrom` | date-heure ISO 8601 UTC |  |  |
| `validUntil` | date-heure ISO 8601 UTC |  |  |
| `status` | enum : `ACTIVE`, `SUSPENDED`, `REVOKED` |  |  |
| `reason` | texte |  | longueur min `0`, longueur max `500` |

<a id="m-CompanyWalletDtos-WalletView"></a>
#### CompanyWalletDtos.WalletView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `companyCustomerCode` | texte |  |  |
| `wallet` | [WalletResponse](#m-WalletResponse) |  |  |
| `entries` | [CompanyWalletDtos.EntryView](#m-CompanyWalletDtos-EntryView)[] |  |  |
| `pendingApprovals` | [CompanyWalletDtos.SpendView](#m-CompanyWalletDtos-SpendView)[] |  |  |

<a id="m-CompanyWalletDtos-SpendView"></a>
#### CompanyWalletDtos.SpendView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `memberId` | texte |  |  |
| `memberName` | texte |  |  |
| `operation` | enum : `BOOKING`, `SUBSCRIPTION`, `PASS`, `INVOICE_PAYMENT`, `MARKETPLACE` |  |  |
| `amount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `documentNumber` | texte |  |  |
| `status` | enum : `AWAITING_OTP`, `HELD`, `PENDING_APPROVAL`, `EXECUTED`, `REJECTED`, `EXPIRED`, `CANCELLED` |  |  |
| `expiresAt` | date-heure ISO 8601 UTC |  |  |
| `transactionNumber` | texte |  |  |
| `decidedBy` | texte |  |  |
| `decidedAt` | date-heure ISO 8601 UTC |  |  |
| `decisionReason` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-CompanyWalletSpend-Status"></a>
#### CompanyWalletSpend.Status

Énumération : `AWAITING_OTP`, `HELD`, `PENDING_APPROVAL`, `EXECUTED`, `REJECTED`, `EXPIRED`, `CANCELLED`

<a id="m-CompanyWalletDtos-AdminTopUpResult"></a>
#### CompanyWalletDtos.AdminTopUpResult

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `actionNumber` | texte |  |  |
| `status` | texte |  |  |
| `walletNumber` | texte |  |  |
| `message` | texte |  |  |

<a id="m-CompanyWalletDtos-AdminTopUpRequest"></a>
#### CompanyWalletDtos.AdminTopUpRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `amount` | nombre décimal | oui |  |
| `currency` | texte |  |  |
| `method` | enum : `CASH`, `BANK_TRANSFER` | oui |  |
| `reference` | texte |  | longueur min `0`, longueur max `180` |
| `reason` | texte | oui | longueur min `5`, longueur max `500` |

<a id="m-WalletResponse"></a>
#### WalletResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `walletNumber` | texte |  |  |
| `ownerType` | texte |  |  |
| `ownerCode` | texte |  |  |
| `currency` | texte |  |  |
| `status` | enum : `ACTIVE`, `SUSPENDED`, `LOCKED`, `CLOSED`, `UNDER_REVIEW` |  |  |
| `availableBalance` | nombre décimal |  |  |
| `ledgerBalance` | nombre décimal |  |  |
| `heldBalance` | nombre décimal |  |  |
| `openedAt` | date-heure ISO 8601 UTC |  |  |
| `closedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-CompanyWalletDtos-EntryView"></a>
#### CompanyWalletDtos.EntryView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `transactionNumber` | texte |  |  |
| `direction` | enum : `DEBIT`, `CREDIT` |  |  |
| `entryType` | enum : `ADMIN_TOPUP`, `TOPUP`, `TRANSFER_IN`, `TRANSFER_OUT`, `TRANSFER_FEE`, `ADMIN_DEBIT`, `PAYMENT`, `REFUND`, `REVERSAL`, `HOLD`, `HOLD_RELEASE`, `ADJUSTMENT`, `CASHBACK`, `PROMOTIONAL_CREDIT`, `OVERPAYMENT_CREDIT` |  |  |
| `amount` | nombre décimal |  |  |
| `balanceAfter` | nombre décimal |  |  |
| `reference` | texte |  |  |
| `spentByMember` | texte |  |  |
| `spentByName` | texte |  |  |
| `documentNumber` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
