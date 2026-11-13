# CashStaffController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [payment](../README.md) · 12 route(s).

## Routes

<a id="r-get-sni-api-v1-cash-registers-absences"></a>
### GET /sni/api/v1/cash-registers/absences

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `List<CashAbsenceResponse>`
- Modèles : [CashAbsenceResponse](#m-CashAbsenceResponse), [CashAbsenceStatus](#m-CashAbsenceStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashStaffController.java`, méthode `absences`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `userEmail` | requête | `String` |  |  |
| `status` | requête | `CashAbsenceStatus` |  |  |

<a id="r-post-sni-api-v1-cash-registers-absences"></a>
### POST /sni/api/v1/cash-registers/absences

- Accès : back-office : rôle d'administration et droit `CASH_STAFF:DECLARE_ABSENCE`
- Corps JSON (obligatoire) : `CashAbsenceRequest`
- Réponse : `CashAbsenceResponse`
- Modèles : [CashAbsenceResponse](#m-CashAbsenceResponse), [CashAbsenceRequest](#m-CashAbsenceRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashStaffController.java`, méthode `declare`

<a id="r-get-sni-api-v1-cash-registers-absences-replacement-suggestions"></a>
### GET /sni/api/v1/cash-registers/absences/replacement-suggestions

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `List<CashReplacementSuggestionResponse>`
- Modèles : [CashReplacementSuggestionResponse](#m-CashReplacementSuggestionResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashStaffController.java`, méthode `suggestions`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `registerCode` | requête | `String` | oui |  |
| `from` | requête | `LocalDate` |  |  |
| `to` | requête | `LocalDate` |  |  |

<a id="r-patch-sni-api-v1-cash-registers-absences-absencenumber-approve"></a>
### PATCH /sni/api/v1/cash-registers/absences/{absenceNumber}/approve

- Accès : back-office : rôle d'administration et droit `CASH_STAFF:MANAGE`
- Réponse : `CashAbsenceResponse`
- Modèles : [CashAbsenceResponse](#m-CashAbsenceResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashStaffController.java`, méthode `approve`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `absenceNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-cash-registers-absences-absencenumber-cancel"></a>
### PATCH /sni/api/v1/cash-registers/absences/{absenceNumber}/cancel

- Accès : back-office : rôle d'administration et droit `CASH_STAFF:DECLARE_ABSENCE`
- Réponse : `CashAbsenceResponse`
- Modèles : [CashAbsenceResponse](#m-CashAbsenceResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashStaffController.java`, méthode `cancel`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `absenceNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-cash-registers-absences-absencenumber-end"></a>
### PATCH /sni/api/v1/cash-registers/absences/{absenceNumber}/end

- Accès : back-office : rôle d'administration et droit `CASH_STAFF:MANAGE`
- Réponse : `CashAbsenceResponse`
- Modèles : [CashAbsenceResponse](#m-CashAbsenceResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashStaffController.java`, méthode `end`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `absenceNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-cash-registers-delegations"></a>
### GET /sni/api/v1/cash-registers/delegations

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `List<CashDelegationResponse>`
- Modèles : [CashDelegationResponse](#m-CashDelegationResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashStaffController.java`, méthode `delegations`

<a id="r-post-sni-api-v1-cash-registers-delegations"></a>
### POST /sni/api/v1/cash-registers/delegations

- Accès : back-office : rôle d'administration et droit `CASH_VARIANCE:DECIDE`
- Corps JSON (obligatoire) : `CashDelegationRequest`
- Réponse : `CashDelegationResponse`
- Modèles : [CashDelegationResponse](#m-CashDelegationResponse), [CashDelegationRequest](#m-CashDelegationRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashStaffController.java`, méthode `delegate`

<a id="r-patch-sni-api-v1-cash-registers-delegations-id-cancel"></a>
### PATCH /sni/api/v1/cash-registers/delegations/{id}/cancel

- Accès : back-office : rôle d'administration et droit `CASH_VARIANCE:DECIDE`
- Réponse : `CashDelegationResponse`
- Modèles : [CashDelegationResponse](#m-CashDelegationResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashStaffController.java`, méthode `cancelDelegation`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-cash-registers-registercode-seal"></a>
### POST /sni/api/v1/cash-registers/{registerCode}/seal

- Accès : back-office : rôle d'administration et droit `CASH_STAFF:MANAGE`
- Corps JSON (obligatoire) : `CashSealRequest`
- Réponse : `CashSealResponse`
- Modèles : [CashSealResponse](#m-CashSealResponse), [CashSealRequest](#m-CashSealRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashStaffController.java`, méthode `seal`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `registerCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-cash-registers-registercode-seals"></a>
### GET /sni/api/v1/cash-registers/{registerCode}/seals

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `List<CashSealResponse>`
- Modèles : [CashSealResponse](#m-CashSealResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashStaffController.java`, méthode `seals`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `registerCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-cash-registers-registercode-unseal"></a>
### POST /sni/api/v1/cash-registers/{registerCode}/unseal

- Accès : back-office : rôle d'administration et droit `CASH_STAFF:MANAGE`
- Corps JSON (obligatoire) : `CashSealRequest`
- Réponse : `CashSealResponse`
- Modèles : [CashSealResponse](#m-CashSealResponse), [CashSealRequest](#m-CashSealRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashStaffController.java`, méthode `unseal`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `registerCode` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-CashAbsenceResponse"></a>
#### CashAbsenceResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `absenceNumber` | texte |  |  |
| `userEmail` | texte |  |  |
| `kindCode` | texte |  |  |
| `kindLabel` | texte |  |  |
| `reason` | texte |  |  |
| `startsOn` | date AAAA-MM-JJ |  |  |
| `endsOn` | date AAAA-MM-JJ |  |  |
| `replacementUser` | texte |  |  |
| `registerCodes` | texte[] |  |  |
| `noReplacementPolicy` | enum : `REMIT_TO_PARENT`, `SEAL`, `SIMPLE_SUSPENSION` |  |  |
| `status` | enum : `PLANNED`, `APPROVED`, `ACTIVE`, `ENDED`, `CANCELLED` |  |  |
| `unplanned` | booléen |  |  |
| `declaredBy` | texte |  |  |
| `approvedBy` | texte |  |  |
| `approvedAt` | date-heure ISO 8601 UTC |  |  |
| `forced` | booléen |  |  |
| `forcedNote` | texte |  |  |
| `warnings` | texte[] |  |  |

<a id="m-CashAbsenceStatus"></a>
#### CashAbsenceStatus

Énumération : `PLANNED`, `APPROVED`, `ACTIVE`, `ENDED`, `CANCELLED`

<a id="m-CashAbsenceRequest"></a>
#### CashAbsenceRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `userEmail` | texte |  |  |
| `kindCode` | texte | oui | longueur min `1` |
| `reasonText` | texte |  |  |
| `startsOn` | date AAAA-MM-JJ | oui |  |
| `endsOn` | date AAAA-MM-JJ |  |  |
| `replacementEmail` | texte |  |  |
| `noReplacementPolicy` | enum : `REMIT_TO_PARENT`, `SEAL`, `SIMPLE_SUSPENSION` |  |  |
| `unplanned` | booléen |  |  |
| `force` | booléen |  |  |
| `forceNote` | texte |  |  |

<a id="m-CashReplacementSuggestionResponse"></a>
#### CashReplacementSuggestionResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `userEmail` | texte |  |  |
| `origin` | texte |  |  |
| `rank` | entier |  |  |
| `registerCode` | texte |  |  |

<a id="m-CashDelegationResponse"></a>
#### CashDelegationResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `delegator` | texte |  |  |
| `delegate` | texte |  |  |
| `permissions` | texte[] |  |  |
| `startsOn` | date AAAA-MM-JJ |  |  |
| `endsOn` | date AAAA-MM-JJ |  |  |
| `note` | texte |  |  |
| `activeNow` | booléen |  |  |
| `cancelledAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-CashDelegationRequest"></a>
#### CashDelegationRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `delegateEmail` | texte | oui | longueur min `1` |
| `permissions` | texte[] | oui | éléments min `1` |
| `startsOn` | date AAAA-MM-JJ | oui |  |
| `endsOn` | date AAAA-MM-JJ | oui |  |
| `note` | texte |  |  |

<a id="m-CashSealResponse"></a>
#### CashSealResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `sealNumber` | texte |  |  |
| `registerCode` | texte |  |  |
| `envelopeNumber` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `countNumber` | texte |  |  |
| `sealedBy` | texte |  |  |
| `witness` | texte |  |  |
| `depositedIn` | texte |  |  |
| `sealedAt` | date-heure ISO 8601 UTC |  |  |
| `unsealedAt` | date-heure ISO 8601 UTC |  |  |
| `unsealedBy` | texte |  |  |
| `unsealWitness` | texte |  |  |
| `unsealCount` | texte |  |  |
| `unsealAmount` | nombre décimal |  |  |
| `difference` | nombre décimal |  |  |
| `absenceNumber` | texte |  |  |

<a id="m-CashSealRequest"></a>
#### CashSealRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `envelopeNumber` | texte |  |  |
| `depositedIn` | texte |  |  |
| `witnessEmail` | texte | oui | longueur min `1` |
| `count` | [CashCountRequest](#m-CashCountRequest) | oui |  |
| `absenceNumber` | texte |  |  |

<a id="m-CashCountRequest"></a>
#### CashCountRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `currency` | texte |  |  |
| `lines` | [CashCountRequest.Line](#m-CashCountRequest-Line)[] |  |  |
| `totalAmount` | nombre décimal |  |  |
| `secondCounterEmail` | texte |  |  |
| `witnessEmail` | texte |  |  |
| `keptFloat` | nombre décimal |  |  |
| `varianceReasonCode` | texte |  |  |
| `varianceReasonText` | texte |  |  |
| `note` | texte |  |  |

<a id="m-CashCountRequest-Line"></a>
#### CashCountRequest.Line

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `value` | nombre décimal |  |  |
| `kind` | enum : `BILL`, `COIN` |  |  |
| `quantity` | entier |  |  |
| `damagedQuantity` | entier |  |  |
