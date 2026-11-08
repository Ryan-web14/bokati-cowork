# WalletController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [payment](../README.md) · 5 route(s).

## Routes

<a id="r-get-sni-api-v1-wallets"></a>
### GET /sni/api/v1/wallets

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ`
- Réponse : `PaginatedResponse<WalletResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [WalletResponse](#m-WalletResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/WalletController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ownerType` | requête | `String` |  |  |
| `ownerCode` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-post-sni-api-v1-wallets-admin-get-or-create"></a>
### POST /sni/api/v1/wallets/admin/get-or-create

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS`
- Corps JSON (obligatoire) : `WalletLookupRequest`
- Réponse : `WalletResponse`
- Modèles : [WalletResponse](#m-WalletResponse), [WalletLookupRequest](#m-WalletLookupRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/WalletController.java`, méthode `getOrCreate`

<a id="r-post-sni-api-v1-wallets-admin-top-up"></a>
### POST /sni/api/v1/wallets/admin/top-up

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS`
- Corps JSON (obligatoire) : `WalletTopUpRequest`
- Réponse : `WalletResponse`
- Modèles : [WalletResponse](#m-WalletResponse), [WalletTopUpRequest](#m-WalletTopUpRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/WalletController.java`, méthode `adminTopUp`

<a id="r-get-sni-api-v1-wallets-walletnumber"></a>
### GET /sni/api/v1/wallets/{walletNumber}

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ`
- Réponse : `WalletResponse`
- Modèles : [WalletResponse](#m-WalletResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/WalletController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-wallets-walletnumber-ledger"></a>
### GET /sni/api/v1/wallets/{walletNumber}/ledger

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ`
- Réponse : `PaginatedResponse<WalletLedgerEntryResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [WalletLedgerEntryResponse](#m-WalletLedgerEntryResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/WalletController.java`, méthode `ledger`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

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

<a id="m-WalletLookupRequest"></a>
#### WalletLookupRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `ownerType` | texte | oui | longueur min `1` |
| `ownerCode` | texte | oui | longueur min `1` |
| `currency` | texte | oui | longueur min `1` |

<a id="m-WalletTopUpRequest"></a>
#### WalletTopUpRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `ownerType` | texte | oui | longueur min `1` |
| `ownerCode` | texte | oui | longueur min `1` |
| `currency` | texte | oui | longueur min `1` |
| `amount` | nombre décimal | oui |  |
| `createdBy` | texte | oui | longueur min `1` |
| `reference` | texte |  |  |
| `metadataJson` | texte |  |  |

<a id="m-WalletLedgerEntryResponse"></a>
#### WalletLedgerEntryResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `entryNumber` | texte |  |  |
| `transactionNumber` | texte |  |  |
| `transactionUuid` | uuid |  |  |
| `walletNumber` | texte |  |  |
| `direction` | enum : `DEBIT`, `CREDIT` |  |  |
| `amount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `balanceAfter` | nombre décimal |  |  |
| `entryType` | enum : `ADMIN_TOPUP`, `TOPUP`, `TRANSFER_IN`, `TRANSFER_OUT`, `TRANSFER_FEE`, `ADMIN_DEBIT`, `PAYMENT`, `REFUND`, `REVERSAL`, `HOLD`, `HOLD_RELEASE`, `ADJUSTMENT`, `CASHBACK`, `PROMOTIONAL_CREDIT`, `OVERPAYMENT_CREDIT` |  |  |
| `sourceType` | texte |  |  |
| `sourceCode` | texte |  |  |
| `reference` | texte |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
