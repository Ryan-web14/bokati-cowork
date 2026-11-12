# WalletControlCentreController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [payment](../README.md) · 15 route(s).

## Routes

<a id="r-get-sni-api-v1-wallets-control-actions-pending"></a>
### GET /sni/api/v1/wallets/control/actions/pending

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Réponse : `PaginatedResponse<WalletControlDtos.AdminActionView>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [WalletControlDtos.AdminActionView](#m-WalletControlDtos-AdminActionView)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/control/controller/WalletControlCentreController.java`, méthode `pending`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-post-sni-api-v1-wallets-control-actions-actionnumber-approve"></a>
### POST /sni/api/v1/wallets/control/actions/{actionNumber}/approve

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Réponse : `WalletControlDtos.AdminActionView`
- Modèles : [WalletControlDtos.AdminActionView](#m-WalletControlDtos-AdminActionView)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/control/controller/WalletControlCentreController.java`, méthode `approve`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `actionNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-wallets-control-actions-actionnumber-reject"></a>
### POST /sni/api/v1/wallets/control/actions/{actionNumber}/reject

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Corps JSON (obligatoire) : `WalletControlDtos.RejectRequest`
- Réponse : `WalletControlDtos.AdminActionView`
- Modèles : [WalletControlDtos.AdminActionView](#m-WalletControlDtos-AdminActionView), [WalletControlDtos.RejectRequest](#m-WalletControlDtos-RejectRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/control/controller/WalletControlCentreController.java`, méthode `reject`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `actionNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-wallets-control-dashboard"></a>
### GET /sni/api/v1/wallets/control/dashboard

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Réponse : `WalletControlCentreService.Dashboard`
- Modèles : [WalletControlCentreService.Dashboard](#m-WalletControlCentreService-Dashboard)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/control/controller/WalletControlCentreController.java`, méthode `dashboard`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `currency` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-wallets-control-export"></a>
### GET /sni/api/v1/wallets/control/export

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Renvoie : `text/csv`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/payment/control/controller/WalletControlCentreController.java`, méthode `export`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `from` | requête | `LocalDate` | oui |  |
| `to` | requête | `LocalDate` | oui |  |

<a id="r-get-sni-api-v1-wallets-control-flags"></a>
### GET /sni/api/v1/wallets/control/flags

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Réponse : `PaginatedResponse<WalletControlDtos.FlagView>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [WalletControlDtos.FlagView](#m-WalletControlDtos-FlagView), [WalletRiskFlag.Status](#m-WalletRiskFlag-Status)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/control/controller/WalletControlCentreController.java`, méthode `flags`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `status` | requête | `List<WalletRiskFlag.Status>` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-post-sni-api-v1-wallets-control-flags-flagnumber-resolve"></a>
### POST /sni/api/v1/wallets/control/flags/{flagNumber}/resolve

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Corps JSON (obligatoire) : `WalletControlDtos.FlagResolveRequest`
- Réponse : `WalletControlDtos.FlagView`
- Modèles : [WalletControlDtos.FlagView](#m-WalletControlDtos-FlagView), [WalletControlDtos.FlagResolveRequest](#m-WalletControlDtos-FlagResolveRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/control/controller/WalletControlCentreController.java`, méthode `resolve`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `flagNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-wallets-control-flags-flagnumber-review"></a>
### POST /sni/api/v1/wallets/control/flags/{flagNumber}/review

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Corps JSON (facultatif) : `WalletControlDtos.FlagReviewRequest`
- Réponse : `WalletControlDtos.FlagView`
- Modèles : [WalletControlDtos.FlagView](#m-WalletControlDtos-FlagView), [WalletControlDtos.FlagReviewRequest](#m-WalletControlDtos-FlagReviewRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/control/controller/WalletControlCentreController.java`, méthode `review`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `flagNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-wallets-control-treasury-reconciliations"></a>
### GET /sni/api/v1/wallets/control/treasury/reconciliations

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Réponse : `PaginatedResponse<WalletTreasuryReconciliation>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [WalletTreasuryReconciliation](#m-WalletTreasuryReconciliation)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/control/controller/WalletControlCentreController.java`, méthode `reconciliations`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-post-sni-api-v1-wallets-control-treasury-reconciliations"></a>
### POST /sni/api/v1/wallets/control/treasury/reconciliations

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Corps JSON (obligatoire) : `WalletControlDtos.TreasuryRequest`
- Réponse : `WalletTreasuryReconciliation`
- Modèles : [WalletTreasuryReconciliation](#m-WalletTreasuryReconciliation), [WalletControlDtos.TreasuryRequest](#m-WalletControlDtos-TreasuryRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/control/controller/WalletControlCentreController.java`, méthode `reconcile`

<a id="r-get-sni-api-v1-wallets-control-treasury-reconciliations-number"></a>
### GET /sni/api/v1/wallets/control/treasury/reconciliations/{number}

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Réponse : `WalletTreasuryReconciliation`
- Modèles : [WalletTreasuryReconciliation](#m-WalletTreasuryReconciliation)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/control/controller/WalletControlCentreController.java`, méthode `reconciliation`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-wallets-control-treasury-snapshot"></a>
### GET /sni/api/v1/wallets/control/treasury/snapshot

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Réponse : `WalletTreasuryReconciliationService.Snapshot`
- Modèles : [WalletTreasuryReconciliationService.Snapshot](#m-WalletTreasuryReconciliationService-Snapshot)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/control/controller/WalletControlCentreController.java`, méthode `snapshot`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `currency` | requête | `String` |  | XAF |

<a id="r-get-sni-api-v1-wallets-control-wallets-walletnumber-actions"></a>
### GET /sni/api/v1/wallets/control/wallets/{walletNumber}/actions

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Réponse : `PaginatedResponse<WalletControlDtos.AdminActionView>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [WalletControlDtos.AdminActionView](#m-WalletControlDtos-AdminActionView)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/control/controller/WalletControlCentreController.java`, méthode `history`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-post-sni-api-v1-wallets-control-wallets-walletnumber-actions"></a>
### POST /sni/api/v1/wallets/control/wallets/{walletNumber}/actions

- Accès : back-office : rôle d'administration et droit `PAYMENT:PROCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Corps JSON (obligatoire) : `WalletControlDtos.AdminActionRequest`
- Réponse : `WalletControlDtos.AdminActionView`
- Modèles : [WalletControlDtos.AdminActionView](#m-WalletControlDtos-AdminActionView), [WalletControlDtos.AdminActionRequest](#m-WalletControlDtos-AdminActionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/control/controller/WalletControlCentreController.java`, méthode `request`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-wallets-control-wallets-walletnumber-flags"></a>
### GET /sni/api/v1/wallets/control/wallets/{walletNumber}/flags

- Accès : back-office : rôle d'administration et droit `PAYMENT:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Réponse : `PaginatedResponse<WalletControlDtos.FlagView>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [WalletControlDtos.FlagView](#m-WalletControlDtos-FlagView)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/control/controller/WalletControlCentreController.java`, méthode `walletFlags`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `walletNumber` | chemin | `String` | oui |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-WalletControlDtos-AdminActionView"></a>
#### WalletControlDtos.AdminActionView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `actionNumber` | texte |  |  |
| `walletNumber` | texte |  |  |
| `type` | enum : `TOPUP`, `DEBIT`, `ADJUST`, `REVERSE`, `SUSPEND`, `UNSUSPEND`, `RESET_PIN`, `RAISE_LIMIT`, `CLOSE`, `REVIEW_FLAG` |  |  |
| `amount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `reference` | texte |  |  |
| `reason` | texte |  |  |
| `status` | enum : `PENDING_APPROVAL`, `EXECUTED`, `REJECTED` |  |  |
| `requestedBy` | texte |  |  |
| `approvedBy` | texte |  |  |
| `approvedAt` | date-heure ISO 8601 UTC |  |  |
| `rejectedBy` | texte |  |  |
| `rejectionReason` | texte |  |  |
| `executedAt` | date-heure ISO 8601 UTC |  |  |
| `resultReference` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-WalletControlDtos-RejectRequest"></a>
#### WalletControlDtos.RejectRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte | oui | longueur min `1` |

<a id="m-WalletControlCentreService-Dashboard"></a>
#### WalletControlCentreService.Dashboard

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `currency` | texte |  |  |
| `totalWalletBalance` | nombre décimal |  |  |
| `totalHeldBalance` | nombre décimal |  |  |
| `openWallets` | entier |  |  |
| `walletsByStatus` | objet { clé : entier int64 ⚠ } |  |  |
| `frozenWallets` | entier int64 ⚠ |  |  |
| `lockedByOwner` | entier int64 ⚠ |  |  |
| `dormantWallets` | entier int64 ⚠ |  |  |
| `negativeBalances` | entier int64 ⚠ |  |  |
| `todayVolume` | [WalletControlCentreService.VolumeLine](#m-WalletControlCentreService-VolumeLine)[] |  |  |
| `openFlags` | entier int64 ⚠ |  |  |
| `pendingApprovals` | entier int64 ⚠ |  |  |
| `latestReconciliation` | [WalletTreasuryReconciliation](#m-WalletTreasuryReconciliation) |  |  |

<a id="m-WalletControlDtos-FlagView"></a>
#### WalletControlDtos.FlagView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `flagNumber` | texte |  |  |
| `walletNumber` | texte |  |  |
| `type` | enum : `UNUSUAL_VOLUME`, `RAPID_IN_OUT`, `STRUCTURING`, `MANY_COUNTERPARTIES`, `DORMANT_REACTIVATION`, `FAILED_PIN_BURST`, `LIMIT_BREACH_ATTEMPT`, `NEGATIVE_BALANCE`, `INTEGRITY_BREAK`, `REVOKED_DEVICE_USED`, `NEW_DEVICE_LARGE_TRANSFER`, `LIST_MATCH`, `IDENTITY_REVIEW_DUE`, `PHONE_CHANGED` |  |  |
| `severity` | enum : `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` |  |  |
| `status` | enum : `OPEN`, `UNDER_REVIEW`, `CONFIRMED`, `DISMISSED` |  |  |
| `details` | texte |  |  |
| `reference` | texte |  |  |
| `detectedAt` | date-heure ISO 8601 UTC |  |  |
| `detectedBy` | texte |  |  |
| `reviewedBy` | texte |  |  |
| `reviewedAt` | date-heure ISO 8601 UTC |  |  |
| `resolution` | texte |  |  |

<a id="m-WalletRiskFlag-Status"></a>
#### WalletRiskFlag.Status

Énumération : `OPEN`, `UNDER_REVIEW`, `CONFIRMED`, `DISMISSED`

<a id="m-WalletControlDtos-FlagResolveRequest"></a>
#### WalletControlDtos.FlagResolveRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `confirmed` | booléen |  |  |
| `resolution` | texte | oui | longueur min `1` |

<a id="m-WalletControlDtos-FlagReviewRequest"></a>
#### WalletControlDtos.FlagReviewRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `holdWallet` | booléen |  |  |

<a id="m-WalletTreasuryReconciliation"></a>
#### WalletTreasuryReconciliation

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `reconciliationNumber` | texte |  |  |
| `reconciliationDate` | date AAAA-MM-JJ |  |  |
| `currency` | texte |  |  |
| `totalWalletBalance` | nombre décimal |  |  |
| `totalHeldBalance` | nombre décimal |  |  |
| `walletCount` | entier |  |  |
| `ledgerAccountBalance` | nombre décimal |  |  |
| `availableCash` | nombre décimal |  |  |
| `coverageRatio` | nombre décimal |  |  |
| `variance` | nombre décimal |  |  |
| `varianceExplained` | booléen |  |  |
| `explainedBy` | texte |  |  |
| `status` | enum : `BALANCED`, `VARIANCE`, `UNDER_REVIEW` |  |  |
| `preparedBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-WalletControlDtos-TreasuryRequest"></a>
#### WalletControlDtos.TreasuryRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `date` | date AAAA-MM-JJ | oui |  |
| `currency` | texte |  |  |
| `ledgerAccountBalance` | nombre décimal |  |  |
| `availableCash` | nombre décimal | oui |  |
| `explanation` | texte |  |  |

<a id="m-WalletTreasuryReconciliationService-Snapshot"></a>
#### WalletTreasuryReconciliationService.Snapshot

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `currency` | texte |  |  |
| `totalWalletBalance` | nombre décimal |  |  |
| `totalHeldBalance` | nombre décimal |  |  |
| `walletCount` | entier |  |  |

<a id="m-WalletControlDtos-AdminActionRequest"></a>
#### WalletControlDtos.AdminActionRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `type` | enum : `TOPUP`, `DEBIT`, `ADJUST`, `REVERSE`, `SUSPEND`, `UNSUSPEND`, `RESET_PIN`, `RAISE_LIMIT`, `CLOSE`, `REVIEW_FLAG` | oui |  |
| `amount` | nombre décimal |  |  |
| `reference` | texte |  |  |
| `reason` | texte | oui | longueur min `1` |
| `until` | date-heure ISO 8601 UTC |  |  |

<a id="m-WalletControlCentreService-VolumeLine"></a>
#### WalletControlCentreService.VolumeLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `entryType` | enum : `ADMIN_TOPUP`, `TOPUP`, `TRANSFER_IN`, `TRANSFER_OUT`, `TRANSFER_FEE`, `ADMIN_DEBIT`, `PAYMENT`, `REFUND`, `REVERSAL`, `HOLD`, `HOLD_RELEASE`, `ADJUSTMENT`, `CASHBACK`, `PROMOTIONAL_CREDIT`, `OVERPAYMENT_CREDIT` |  |  |
| `direction` | enum : `DEBIT`, `CREDIT` |  |  |
| `count` | entier int64 ⚠ |  |  |
| `amount` | nombre décimal |  |  |
