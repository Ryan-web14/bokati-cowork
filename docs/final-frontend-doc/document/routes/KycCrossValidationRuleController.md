# KycCrossValidationRuleController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [document](../README.md) · 5 route(s).

## Routes

<a id="r-get-sni-api-v1-kyc-cross-validation-rules"></a>
### GET /sni/api/v1/kyc/cross-validation-rules

- Accès : back-office : rôle d'administration et droit `KYC:READ`
- Réponse : `List<KycCrossValidationRuleResponse>`
- Modèles : [KycCrossValidationRuleResponse](#m-KycCrossValidationRuleResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycCrossValidationRuleController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `activeOnly` | requête | `Boolean` |  |  |

<a id="r-post-sni-api-v1-kyc-cross-validation-rules"></a>
### POST /sni/api/v1/kyc/cross-validation-rules

- Accès : back-office : rôle d'administration et droit `KYC:MANAGE`
- Corps JSON (obligatoire) : `KycCrossValidationRuleRequest`
- Réponse : `KycCrossValidationRuleResponse`
- Modèles : [KycCrossValidationRuleResponse](#m-KycCrossValidationRuleResponse), [KycCrossValidationRuleRequest](#m-KycCrossValidationRuleRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycCrossValidationRuleController.java`, méthode `create`

<a id="r-delete-sni-api-v1-kyc-cross-validation-rules-id"></a>
### DELETE /sni/api/v1/kyc/cross-validation-rules/{id}

- Accès : back-office : rôle d'administration et droit `KYC:MANAGE`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycCrossValidationRuleController.java`, méthode `delete`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` (code identifiant ou nombre) | oui |  |

<a id="r-get-sni-api-v1-kyc-cross-validation-rules-id"></a>
### GET /sni/api/v1/kyc/cross-validation-rules/{id}

- Accès : back-office : rôle d'administration et droit `KYC:READ`
- Réponse : `KycCrossValidationRuleResponse`
- Modèles : [KycCrossValidationRuleResponse](#m-KycCrossValidationRuleResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycCrossValidationRuleController.java`, méthode `getById`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` (code identifiant ou nombre) | oui |  |

<a id="r-put-sni-api-v1-kyc-cross-validation-rules-id"></a>
### PUT /sni/api/v1/kyc/cross-validation-rules/{id}

- Accès : back-office : rôle d'administration et droit `KYC:MANAGE`
- Corps JSON (obligatoire) : `KycCrossValidationRuleRequest`
- Réponse : `KycCrossValidationRuleResponse`
- Modèles : [KycCrossValidationRuleResponse](#m-KycCrossValidationRuleResponse), [KycCrossValidationRuleRequest](#m-KycCrossValidationRuleRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycCrossValidationRuleController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` (code identifiant ou nombre) | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-KycCrossValidationRuleResponse"></a>
#### KycCrossValidationRuleResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | code identifiant (texte) |  |  |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `ruleType` | enum : `FIELD_MATCH`, `DECLARED_VS_OCR`, `PROFILE_MATCH`, `DOCUMENT_NOT_EXPIRED`, `ISSUE_BEFORE_EXPIRY`, `MIN_VALIDITY`, `UNIQUE_DOCUMENT_NUMBER` |  |  |
| `parameter` | texte |  |  |
| `systemRule` | booléen |  |  |
| `documentTypeCode1` | texte |  |  |
| `documentTypeCode2` | texte |  |  |
| `fieldToCompare` | texte |  |  |
| `blocking` | booléen |  |  |
| `active` | booléen |  |  |

<a id="m-KycCrossValidationRuleRequest"></a>
#### KycCrossValidationRuleRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  | longueur min `0`, longueur max `80` |
| `name` | texte |  | longueur min `0`, longueur max `200` |
| `description` | texte |  |  |
| `ruleType` | enum : `FIELD_MATCH`, `DECLARED_VS_OCR`, `PROFILE_MATCH`, `DOCUMENT_NOT_EXPIRED`, `ISSUE_BEFORE_EXPIRY`, `MIN_VALIDITY`, `UNIQUE_DOCUMENT_NUMBER` |  |  |
| `documentTypeCode1` | texte |  | longueur min `0`, longueur max `1000` |
| `documentTypeCode2` | texte |  | longueur min `0`, longueur max `1000` |
| `fieldToCompare` | texte |  | longueur min `0`, longueur max `80` |
| `parameter` | texte |  | longueur min `0`, longueur max `200` |
| `blocking` | booléen |  |  |
| `active` | booléen |  |  |
