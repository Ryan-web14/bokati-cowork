# CashAnomalyFlagController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [payment](../README.md) · 3 route(s).

## Routes

<a id="r-get-sni-api-v1-cash-registers-anomalies"></a>
### GET /sni/api/v1/cash-registers/anomalies

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `PaginatedResponse<CashAnomalyFlagResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [CashAnomalyFlagResponse](#m-CashAnomalyFlagResponse), [CashAnomalySeverity](#m-CashAnomalySeverity), [CashAnomalyStatus](#m-CashAnomalyStatus), [CashAnomalyType](#m-CashAnomalyType)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashAnomalyFlagController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `registerCode` | requête | `String` |  |  |
| `sessionNumber` | requête | `String` |  |  |
| `severity` | requête | `CashAnomalySeverity` |  |  |
| `status` | requête | `CashAnomalyStatus` |  |  |
| `anomalyType` | requête | `CashAnomalyType` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=detectedAt,DESC |

<a id="r-post-sni-api-v1-cash-registers-anomalies-sessions-sessionnumber-analyze"></a>
### POST /sni/api/v1/cash-registers/anomalies/sessions/{sessionNumber}/analyze

- Accès : back-office : rôle d'administration et droit `CASH_VARIANCE:DECIDE`
- Réponse : `List<CashAnomalyFlagResponse>`
- Modèles : [CashAnomalyFlagResponse](#m-CashAnomalyFlagResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashAnomalyFlagController.java`, méthode `analyzeSession`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `sessionNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-cash-registers-anomalies-flagnumber-review"></a>
### PATCH /sni/api/v1/cash-registers/anomalies/{flagNumber}/review

- Accès : back-office : rôle d'administration et droit `CASH_VARIANCE:DECIDE`
- Corps JSON (obligatoire) : `ReviewCashAnomalyRequest`
- Réponse : `CashAnomalyFlagResponse`
- Modèles : [CashAnomalyFlagResponse](#m-CashAnomalyFlagResponse), [ReviewCashAnomalyRequest](#m-ReviewCashAnomalyRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashAnomalyFlagController.java`, méthode `review`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `flagNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-CashAnomalyFlagResponse"></a>
#### CashAnomalyFlagResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `flagNumber` | texte |  |  |
| `sessionNumber` | texte |  |  |
| `movementNumber` | texte |  |  |
| `registerCode` | texte |  |  |
| `anomalyType` | enum : `VARIANCE_OUTLIER`, `ROUND_NUMBER_PATTERN`, `EXCESSIVE_REFUNDS`, `EXCESSIVE_ADJUSTMENTS`, `OFF_HOURS_SESSION`, `THRESHOLD_STRUCTURING`, `NEAR_MAX_CASH_RECURRENCE`, `OPENING_VARIANCE`, `LONG_SESSION`, `LONG_PAUSES`, `REPEATED_REVERSALS`, `DETENTION_CAP_EXCEEDED`, `UNKNOWN_DEVICE`, `DAMAGED_NOTES`, `TRANSFER_VARIANCE` |  |  |
| `severity` | enum : `LOW`, `MEDIUM`, `HIGH` |  |  |
| `score` | nombre décimal |  |  |
| `description` | texte |  |  |
| `detectedAt` | date-heure ISO 8601 UTC |  |  |
| `status` | enum : `OPEN`, `ACKNOWLEDGED`, `DISMISSED`, `ESCALATED`, `RESOLVED` |  |  |
| `reviewedBy` | texte |  |  |
| `reviewedAt` | date-heure ISO 8601 UTC |  |  |
| `reviewNote` | texte |  |  |

<a id="m-CashAnomalySeverity"></a>
#### CashAnomalySeverity

Énumération : `LOW`, `MEDIUM`, `HIGH`

<a id="m-CashAnomalyStatus"></a>
#### CashAnomalyStatus

Énumération : `OPEN`, `ACKNOWLEDGED`, `DISMISSED`, `ESCALATED`, `RESOLVED`

<a id="m-CashAnomalyType"></a>
#### CashAnomalyType

Énumération : `VARIANCE_OUTLIER`, `ROUND_NUMBER_PATTERN`, `EXCESSIVE_REFUNDS`, `EXCESSIVE_ADJUSTMENTS`, `OFF_HOURS_SESSION`, `THRESHOLD_STRUCTURING`, `NEAR_MAX_CASH_RECURRENCE`, `OPENING_VARIANCE`, `LONG_SESSION`, `LONG_PAUSES`, `REPEATED_REVERSALS`, `DETENTION_CAP_EXCEEDED`, `UNKNOWN_DEVICE`, `DAMAGED_NOTES`, `TRANSFER_VARIANCE`

<a id="m-ReviewCashAnomalyRequest"></a>
#### ReviewCashAnomalyRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reviewedBy` | texte |  |  |
| `status` | enum : `OPEN`, `ACKNOWLEDGED`, `DISMISSED`, `ESCALATED`, `RESOLVED` | oui |  |
| `note` | texte |  |  |
