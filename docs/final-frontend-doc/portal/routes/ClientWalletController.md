# ClientWalletController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [portal](../README.md) · 3 route(s).

## Routes

<a id="r-get-sni-api-v1-client-wallet"></a>
### GET /sni/api/v1/client/wallet

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `PaginatedResponse<WalletResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [WalletResponse](#m-WalletResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/wallet/controller/ClientWalletController.java`, méthode `listWallets`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-get-sni-api-v1-client-wallet-walletnumber"></a>
### GET /sni/api/v1/client/wallet/{walletNumber}

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `WalletResponse`
- Modèles : [WalletResponse](#m-WalletResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/wallet/controller/ClientWalletController.java`, méthode `getWallet`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-client-wallet-walletnumber-ledger"></a>
### GET /sni/api/v1/client/wallet/{walletNumber}/ledger

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `PaginatedResponse<WalletLedgerEntryResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [WalletLedgerEntryResponse](#m-WalletLedgerEntryResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/wallet/controller/ClientWalletController.java`, méthode `getLedger`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=30, sort=createdAt,DESC |

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
