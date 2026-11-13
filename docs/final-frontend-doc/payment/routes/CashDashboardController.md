# CashDashboardController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [payment](../README.md) · 2 route(s).

## Routes

<a id="r-get-sni-api-v1-cash-registers-position"></a>
### GET /sni/api/v1/cash-registers/position

- Accès : back-office : rôle d'administration et droit `CASH_REPORT:READ`
- Réponse : `CashPositionResponse`
- Modèles : [CashPositionResponse](#m-CashPositionResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashDashboardController.java`, méthode `position`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `locationCode` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-cash-registers-reports-kind"></a>
### GET /sni/api/v1/cash-registers/reports/{kind}

- Accès : back-office : rôle d'administration et droit `CASH_REPORT:READ`
- Réponse : `non typé, voir le code`
- Code : `src/main/java/com/sni/bokaticowork/features/payment/controller/CashDashboardController.java`, méthode `report`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `kind` | chemin | `String` | oui |  |
| `registerCode` | requête | `String` |  |  |
| `from` | requête | `LocalDate` |  |  |
| `to` | requête | `LocalDate` |  |  |
| `format` | requête | `String` |  |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-CashPositionResponse"></a>
#### CashPositionResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `generatedAt` | date-heure ISO 8601 UTC |  |  |
| `registers` | [CashPositionResponse.RegisterPosition](#m-CashPositionResponse-RegisterPosition)[] |  |  |
| `sites` | [CashPositionResponse.SiteTotal](#m-CashPositionResponse-SiteTotal)[] |  |  |
| `total` | [CashPositionResponse.SiteTotal](#m-CashPositionResponse-SiteTotal) |  |  |

<a id="m-CashPositionResponse-RegisterPosition"></a>
#### CashPositionResponse.RegisterPosition

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `registerCode` | texte |  |  |
| `name` | texte |  |  |
| `registerType` | texte |  |  |
| `status` | texte |  |  |
| `locationCode` | texte |  |  |
| `sessionNumber` | texte |  |  |
| `sessionStatus` | texte |  |  |
| `cashier` | texte |  |  |
| `expectedBalance` | nombre décimal |  |  |
| `expectedHidden` | booléen |  |  |
| `lastCountAmount` | nombre décimal |  |  |
| `lastCountAt` | date-heure ISO 8601 UTC |  |  |
| `daysSinceCount` | entier int64 ⚠ |  |  |
| `maxCashAmount` | nombre décimal |  |  |
| `occupancyPercent` | nombre décimal |  |  |
| `transfersInCount` | entier |  |  |
| `transfersInAmount` | nombre décimal |  |  |
| `transfersOutCount` | entier |  |  |
| `transfersOutAmount` | nombre décimal |  |  |
| `pendingReviews` | entier |  |  |
| `pendingVisas` | entier |  |  |
| `lastSpotCheckAt` | date-heure ISO 8601 UTC |  |  |
| `spotCheckOverdue` | booléen |  |  |
| `alerts` | texte[] |  |  |

<a id="m-CashPositionResponse-SiteTotal"></a>
#### CashPositionResponse.SiteTotal

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `locationCode` | texte |  |  |
| `registers` | entier |  |  |
| `openSessions` | entier |  |  |
| `expectedBalance` | nombre décimal |  |  |
| `lastCountTotal` | nombre décimal |  |  |
| `inTransit` | nombre décimal |  |  |
| `pendingReviews` | entier |  |  |
| `alerts` | entier |  |  |
