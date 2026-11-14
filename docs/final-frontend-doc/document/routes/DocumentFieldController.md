# DocumentFieldController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [document](../README.md) · 8 route(s).

## Routes

<a id="r-get-sni-api-v1-cash-registers-movements-number-fields"></a>
### GET /sni/api/v1/cash-registers/movements/{number}/fields

- Accès : back-office : rôle d'administration et droit `CASH:READ`
- Réponse : `DocumentTemplateDtos.PieceFields`
- Modèles : [DocumentTemplateDtos.PieceFields](#m-DocumentTemplateDtos-PieceFields)
- Code : `src/main/java/com/sni/bokaticowork/features/document/template/controller/DocumentFieldController.java`, méthode `cash`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-cash-registers-movements-number-fields"></a>
### PUT /sni/api/v1/cash-registers/movements/{number}/fields

- Accès : back-office : rôle d'administration et droit `CASH_MOVEMENT:CREATE`
- Corps JSON (obligatoire) : `DocumentTemplateDtos.PieceFieldsRequest`
- Réponse : `DocumentTemplateDtos.PieceFields`
- Modèles : [DocumentTemplateDtos.PieceFields](#m-DocumentTemplateDtos-PieceFields), [DocumentTemplateDtos.PieceFieldsRequest](#m-DocumentTemplateDtos-PieceFieldsRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/template/controller/DocumentFieldController.java`, méthode `saveCash`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-fund-needs-number-fields"></a>
### GET /sni/api/v1/fund-needs/{number}/fields

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:CREATE`
- Réponse : `DocumentTemplateDtos.PieceFields`
- Modèles : [DocumentTemplateDtos.PieceFields](#m-DocumentTemplateDtos-PieceFields)
- Code : `src/main/java/com/sni/bokaticowork/features/document/template/controller/DocumentFieldController.java`, méthode `need`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-fund-needs-number-fields"></a>
### PUT /sni/api/v1/fund-needs/{number}/fields

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:CREATE`
- Corps JSON (obligatoire) : `DocumentTemplateDtos.PieceFieldsRequest`
- Réponse : `DocumentTemplateDtos.PieceFields`
- Modèles : [DocumentTemplateDtos.PieceFields](#m-DocumentTemplateDtos-PieceFields), [DocumentTemplateDtos.PieceFieldsRequest](#m-DocumentTemplateDtos-PieceFieldsRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/template/controller/DocumentFieldController.java`, méthode `saveNeed`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-fund-requests-number-disbursements-movement-fields"></a>
### GET /sni/api/v1/fund-requests/{number}/disbursements/{movement}/fields

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:CREATE`
- Réponse : `DocumentTemplateDtos.PieceFields`
- Modèles : [DocumentTemplateDtos.PieceFields](#m-DocumentTemplateDtos-PieceFields)
- Code : `src/main/java/com/sni/bokaticowork/features/document/template/controller/DocumentFieldController.java`, méthode `disbursement`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |
| `movement` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-fund-requests-number-disbursements-movement-fields"></a>
### PUT /sni/api/v1/fund-requests/{number}/disbursements/{movement}/fields

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:CREATE`
- Corps JSON (obligatoire) : `DocumentTemplateDtos.PieceFieldsRequest`
- Réponse : `DocumentTemplateDtos.PieceFields`
- Modèles : [DocumentTemplateDtos.PieceFields](#m-DocumentTemplateDtos-PieceFields), [DocumentTemplateDtos.PieceFieldsRequest](#m-DocumentTemplateDtos-PieceFieldsRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/template/controller/DocumentFieldController.java`, méthode `saveDisbursement`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |
| `movement` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-fund-requests-number-fields"></a>
### GET /sni/api/v1/fund-requests/{number}/fields

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:CREATE`
- Réponse : `DocumentTemplateDtos.PieceFields`
- Modèles : [DocumentTemplateDtos.PieceFields](#m-DocumentTemplateDtos-PieceFields)
- Code : `src/main/java/com/sni/bokaticowork/features/document/template/controller/DocumentFieldController.java`, méthode `request`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |
| `template` | requête | `String` |  | FUND_REQUEST |

<a id="r-put-sni-api-v1-fund-requests-number-fields"></a>
### PUT /sni/api/v1/fund-requests/{number}/fields

- Accès : back-office : rôle d'administration et droit `FUND_REQUEST:CREATE`
- Corps JSON (obligatoire) : `DocumentTemplateDtos.PieceFieldsRequest`
- Réponse : `DocumentTemplateDtos.PieceFields`
- Modèles : [DocumentTemplateDtos.PieceFields](#m-DocumentTemplateDtos-PieceFields), [DocumentTemplateDtos.PieceFieldsRequest](#m-DocumentTemplateDtos-PieceFieldsRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/template/controller/DocumentFieldController.java`, méthode `saveRequest`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |
| `template` | requête | `String` |  | FUND_REQUEST |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-DocumentTemplateDtos-PieceFields"></a>
#### DocumentTemplateDtos.PieceFields

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `templateCode` | texte |  |  |
| `publishedVersion` | entier |  |  |
| `sourceCode` | texte |  |  |
| `fields` | [DocumentTemplateDtos.PieceField](#m-DocumentTemplateDtos-PieceField)[] |  |  |
| `missingRequired` | texte[] |  |  |

<a id="m-DocumentTemplateDtos-PieceFieldsRequest"></a>
#### DocumentTemplateDtos.PieceFieldsRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `values` | objet { clé : objet libre } |  |  |

<a id="m-DocumentTemplateDtos-PieceField"></a>
#### DocumentTemplateDtos.PieceField

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `key` | texte |  |  |
| `blockKey` | texte |  |  |
| `label` | texte |  |  |
| `type` | texte |  |  |
| `mode` | texte |  |  |
| `required` | booléen |  |  |
| `options` | texte[] |  |  |
| `help` | texte |  |  |
| `value` | texte |  |  |
| `enteredBy` | texte |  |  |
| `enteredAt` | date-heure ISO 8601 UTC |  |  |
