# PassOperationController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [subscription](../README.md) · 7 route(s).

## Routes

<a id="r-delete-sni-api-v1-passes-transfers-transfernumber"></a>
### DELETE /sni/api/v1/passes/transfers/{transferNumber}

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:UPDATE`
- Réponse : `Map<String, String>`
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/pass/controller/PassOperationController.java`, méthode `cancel`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `transferNumber` | chemin | `String` | oui |  |
| `reason` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-passes-transfers-transfernumber-accept"></a>
### POST /sni/api/v1/passes/transfers/{transferNumber}/accept

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE`
- Réponse : `Map<String, String>`
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/pass/controller/PassOperationController.java`, méthode `accept`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `transferNumber` | chemin | `String` | oui |  |
| `acceptedBy` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-passes-transfers-transfernumber-reject"></a>
### POST /sni/api/v1/passes/transfers/{transferNumber}/reject

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE`
- Réponse : `Map<String, String>`
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/pass/controller/PassOperationController.java`, méthode `reject`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `transferNumber` | chemin | `String` | oui |  |
| `reason` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-passes-usages-usagenumber-reverse"></a>
### POST /sni/api/v1/passes/usages/{usageNumber}/reverse

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE`
- Réponse : `Map<String, String>`
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/pass/controller/PassOperationController.java`, méthode `reverse`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `usageNumber` | chemin | `String` | oui |  |
| `reason` | requête | `String` | oui |  |
| `reversedBy` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-passes-use"></a>
### POST /sni/api/v1/passes/use

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE`
- Corps JSON (obligatoire) : `PassOperationController.UsePassRequest`
- Réponse : `Map<String, Object>`
- Modèles : [PassOperationController.UsePassRequest](#m-PassOperationController-UsePassRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/pass/controller/PassOperationController.java`, méthode `use`

<a id="r-post-sni-api-v1-passes-validate"></a>
### POST /sni/api/v1/passes/validate

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE`
- Corps JSON (obligatoire) : `PassOperationController.ValidateRequest`
- Réponse : `Map<String, Object>`
- Modèles : [PassOperationController.ValidateRequest](#m-PassOperationController-ValidateRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/pass/controller/PassOperationController.java`, méthode `validate`

<a id="r-post-sni-api-v1-passes-passnumber-transfers"></a>
### POST /sni/api/v1/passes/{passNumber}/transfers

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE`
- Corps JSON (obligatoire) : `PassOperationController.TransferRequest`
- Réponse : `Map<String, String>`
- Modèles : [PassOperationController.TransferRequest](#m-PassOperationController-TransferRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/pass/controller/PassOperationController.java`, méthode `requestTransfer`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `passNumber` | chemin | `String` | oui |  |
| `requestedBy` | requête | `String` |  |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-PassOperationController-UsePassRequest"></a>
#### PassOperationController.UsePassRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `passNumber` | texte |  |  |
| `credentialValue` | texte |  |  |
| `bearerType` | texte |  |  |
| `bearerCode` | texte |  |  |
| `usageType` | enum : `CHECK_IN`, `BOOKING`, `SERVICE`, `MEETING_ROOM`, `PRINT`, `OTHER` |  |  |
| `locationCode` | texte |  |  |
| `resourceCode` | texte |  |  |
| `resourceTypeCode` | texte |  |  |
| `channel` | enum : `QR_SCAN`, `MANUAL`, `KIOSK`, `API` |  |  |
| `validatedBy` | texte |  |  |
| `referenceType` | texte |  |  |
| `referenceCode` | texte |  |  |

<a id="m-PassOperationController-ValidateRequest"></a>
#### PassOperationController.ValidateRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `passNumber` | texte |  |  |
| `credentialValue` | texte |  |  |
| `bearerType` | texte |  |  |
| `bearerCode` | texte |  |  |
| `locationCode` | texte |  |  |
| `resourceTypeCode` | texte |  |  |

<a id="m-PassOperationController-TransferRequest"></a>
#### PassOperationController.TransferRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `toOwnerType` | enum : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` | oui |  |
| `toOwnerCode` | texte | oui | longueur min `1` |
| `reason` | texte |  |  |
| `transferFee` | nombre décimal |  |  |
