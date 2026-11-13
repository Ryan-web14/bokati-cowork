# SecurityDepositController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [billing](../README.md) · 5 route(s).

## Routes

<a id="r-put-sni-api-v1-billing-catalog-itemcode-security-deposit"></a>
### PUT /sni/api/v1/billing/catalog/{itemCode}/security-deposit

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Corps JSON (obligatoire) : `SecurityDepositDtos.FlagRequest`
- Réponse : `Map<String, Object>`
- Modèles : [SecurityDepositDtos.FlagRequest](#m-SecurityDepositDtos-FlagRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/SecurityDepositController.java`, méthode `flag`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-billing-security-deposits"></a>
### GET /sni/api/v1/billing/security-deposits

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `List<SecurityDepositDtos.DepositView>`
- Modèles : [SecurityDepositDtos.DepositView](#m-SecurityDepositDtos-DepositView)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/SecurityDepositController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerType` | requête | `String` |  |  |
| `customerCode` | requête | `String` |  |  |
| `open` | requête | `boolean` |  | true |

<a id="r-get-sni-api-v1-billing-security-deposits-legacy"></a>
### GET /sni/api/v1/billing/security-deposits/legacy

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `List<SecurityDepositDtos.LegacyLine>`
- Modèles : [SecurityDepositDtos.LegacyLine](#m-SecurityDepositDtos-LegacyLine)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/SecurityDepositController.java`, méthode `legacy`

<a id="r-get-sni-api-v1-billing-security-deposits-depositnumber"></a>
### GET /sni/api/v1/billing/security-deposits/{depositNumber}

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `SecurityDepositDtos.DepositView`
- Modèles : [SecurityDepositDtos.DepositView](#m-SecurityDepositDtos-DepositView)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/SecurityDepositController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `depositNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-billing-security-deposits-depositnumber-movements"></a>
### POST /sni/api/v1/billing/security-deposits/{depositNumber}/movements

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Corps JSON (obligatoire) : `SecurityDepositDtos.MovementRequest`
- Réponse : `SecurityDepositDtos.DepositView`
- Modèles : [SecurityDepositDtos.DepositView](#m-SecurityDepositDtos-DepositView), [SecurityDepositDtos.MovementRequest](#m-SecurityDepositDtos-MovementRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/SecurityDepositController.java`, méthode `move`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `depositNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-SecurityDepositDtos-FlagRequest"></a>
#### SecurityDepositDtos.FlagRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `securityDeposit` | booléen | oui |  |

<a id="m-SecurityDepositDtos-DepositView"></a>
#### SecurityDepositDtos.DepositView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `depositNumber` | texte |  |  |
| `documentNumber` | texte |  |  |
| `subscriptionNumber` | texte |  |  |
| `customerType` | texte |  |  |
| `customerCode` | texte |  |  |
| `customerName` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `releasedAmount` | nombre décimal |  |  |
| `retainedAmount` | nombre décimal |  |  |
| `appliedAmount` | nombre décimal |  |  |
| `remaining` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `status` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `movements` | [SecurityDepositDtos.MovementView](#m-SecurityDepositDtos-MovementView)[] |  |  |

<a id="m-SecurityDepositDtos-LegacyLine"></a>
#### SecurityDepositDtos.LegacyLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentNumber` | texte |  |  |
| `customerCode` | texte |  |  |
| `customerName` | texte |  |  |
| `issueDate` | date AAAA-MM-JJ |  |  |
| `description` | texte |  |  |
| `totalAmount` | nombre décimal |  |  |
| `netAmount` | nombre décimal |  |  |
| `taxAmount` | nombre décimal |  |  |
| `posted` | booléen |  |  |

<a id="m-SecurityDepositDtos-MovementRequest"></a>
#### SecurityDepositDtos.MovementRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `kind` | texte | oui | longueur min `1` |
| `amount` | nombre décimal |  |  |
| `reason` | texte |  | longueur min `0`, longueur max `500` |
| `targetDocumentNumber` | texte |  |  |

<a id="m-SecurityDepositDtos-MovementView"></a>
#### SecurityDepositDtos.MovementView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `movementNumber` | texte |  |  |
| `kind` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `reason` | texte |  |  |
| `targetDocumentNumber` | texte |  |  |
| `movementDate` | date AAAA-MM-JJ |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
