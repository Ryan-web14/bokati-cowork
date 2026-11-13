# WithholdingController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [billing](../README.md) · 10 route(s).

## Routes

<a id="r-get-sni-api-v1-billing-documents-documentnumber-withholdings"></a>
### GET /sni/api/v1/billing/documents/{documentNumber}/withholdings

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `List<WithholdingDtos.WithholdingView>`
- Modèles : [WithholdingDtos.WithholdingView](#m-WithholdingDtos-WithholdingView)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/WithholdingController.java`, méthode `ofDocument`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-billing-documents-documentnumber-withholdings"></a>
### POST /sni/api/v1/billing/documents/{documentNumber}/withholdings

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Corps JSON (obligatoire) : `WithholdingDtos.RecordRequest`
- Réponse : `WithholdingDtos.WithholdingView`
- Modèles : [WithholdingDtos.WithholdingView](#m-WithholdingDtos-WithholdingView), [WithholdingDtos.RecordRequest](#m-WithholdingDtos-RecordRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/WithholdingController.java`, méthode `record`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-billing-withholding-types"></a>
### GET /sni/api/v1/billing/withholding-types

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `List<WithholdingDtos.TypeView>`
- Modèles : [WithholdingDtos.TypeView](#m-WithholdingDtos-TypeView)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/WithholdingController.java`, méthode `types`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `includeInactive` | requête | `boolean` |  | false |

<a id="r-post-sni-api-v1-billing-withholding-types"></a>
### POST /sni/api/v1/billing/withholding-types

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE` ; contrôle supplémentaire sur la méthode : `hasAuthority('BILLING:TAX_RULES')`
- Corps JSON (obligatoire) : `WithholdingDtos.TypeRequest`
- Réponse : `WithholdingDtos.TypeView`
- Modèles : [WithholdingDtos.TypeView](#m-WithholdingDtos-TypeView), [WithholdingDtos.TypeRequest](#m-WithholdingDtos-TypeRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/WithholdingController.java`, méthode `createType`

<a id="r-put-sni-api-v1-billing-withholding-types-code"></a>
### PUT /sni/api/v1/billing/withholding-types/{code}

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE` ; contrôle supplémentaire sur la méthode : `hasAuthority('BILLING:TAX_RULES')`
- Corps JSON (obligatoire) : `WithholdingDtos.TypeUpdateRequest`
- Réponse : `WithholdingDtos.TypeView`
- Modèles : [WithholdingDtos.TypeView](#m-WithholdingDtos-TypeView), [WithholdingDtos.TypeUpdateRequest](#m-WithholdingDtos-TypeUpdateRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/WithholdingController.java`, méthode `updateType`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-billing-withholding-types-code-activate"></a>
### PATCH /sni/api/v1/billing/withholding-types/{code}/activate

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE` ; contrôle supplémentaire sur la méthode : `hasAuthority('BILLING:TAX_RULES')`
- Réponse : `WithholdingDtos.TypeView`
- Modèles : [WithholdingDtos.TypeView](#m-WithholdingDtos-TypeView)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/WithholdingController.java`, méthode `activateType`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-billing-withholding-types-code-deactivate"></a>
### PATCH /sni/api/v1/billing/withholding-types/{code}/deactivate

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE` ; contrôle supplémentaire sur la méthode : `hasAuthority('BILLING:TAX_RULES')`
- Réponse : `WithholdingDtos.TypeView`
- Modèles : [WithholdingDtos.TypeView](#m-WithholdingDtos-TypeView)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/WithholdingController.java`, méthode `deactivateType`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-billing-withholdings"></a>
### GET /sni/api/v1/billing/withholdings

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `List<WithholdingDtos.WithholdingView>`
- Modèles : [WithholdingDtos.WithholdingView](#m-WithholdingDtos-WithholdingView)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/WithholdingController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerType` | requête | `String` |  |  |
| `customerCode` | requête | `String` |  |  |
| `certificate` | requête | `String` |  |  |

<a id="r-patch-sni-api-v1-billing-withholdings-number-cancel"></a>
### PATCH /sni/api/v1/billing/withholdings/{number}/cancel

- Accès : back-office : rôle d'administration et droit `BILLING:CANCEL`
- Corps JSON (obligatoire) : `WithholdingDtos.CancelRequest`
- Réponse : `WithholdingDtos.WithholdingView`
- Modèles : [WithholdingDtos.WithholdingView](#m-WithholdingDtos-WithholdingView), [WithholdingDtos.CancelRequest](#m-WithholdingDtos-CancelRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/WithholdingController.java`, méthode `cancel`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-billing-withholdings-number-certificate"></a>
### PATCH /sni/api/v1/billing/withholdings/{number}/certificate

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Corps JSON (obligatoire) : `WithholdingDtos.CertificateRequest`
- Réponse : `WithholdingDtos.WithholdingView`
- Modèles : [WithholdingDtos.WithholdingView](#m-WithholdingDtos-WithholdingView), [WithholdingDtos.CertificateRequest](#m-WithholdingDtos-CertificateRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/WithholdingController.java`, méthode `certificate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-WithholdingDtos-WithholdingView"></a>
#### WithholdingDtos.WithholdingView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `withholdingNumber` | texte |  |  |
| `documentNumber` | texte |  |  |
| `typeCode` | texte |  |  |
| `typeLabel` | texte |  |  |
| `account` | texte |  |  |
| `baseAmount` | nombre décimal |  |  |
| `rate` | nombre décimal |  |  |
| `amount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `customerType` | texte |  |  |
| `customerCode` | texte |  |  |
| `certificateReference` | texte |  |  |
| `certificateDocument` | texte |  |  |
| `certificateAt` | date-heure ISO 8601 UTC |  |  |
| `certificateMissing` | booléen |  |  |
| `status` | texte |  |  |
| `withheldOn` | date AAAA-MM-JJ |  |  |
| `recordedBy` | texte |  |  |
| `cancelledAt` | date-heure ISO 8601 UTC |  |  |
| `cancelReason` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-WithholdingDtos-RecordRequest"></a>
#### WithholdingDtos.RecordRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `typeCode` | texte | oui | longueur min `1` |
| `amount` | nombre décimal |  | min `0.01` |
| `withheldOn` | date AAAA-MM-JJ |  |  |
| `certificateReference` | texte |  | longueur min `0`, longueur max `120` |
| `certificateDocument` | texte |  | longueur min `0`, longueur max `120` |

<a id="m-WithholdingDtos-TypeView"></a>
#### WithholdingDtos.TypeView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `rate` | nombre décimal |  |  |
| `base` | texte |  |  |
| `account` | texte |  |  |
| `displayOrder` | entier |  |  |
| `active` | booléen |  |  |

<a id="m-WithholdingDtos-TypeRequest"></a>
#### WithholdingDtos.TypeRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte | oui | longueur min `0`, longueur max `40` |
| `label` | texte | oui | longueur min `0`, longueur max `150` |
| `rate` | nombre décimal |  |  |
| `base` | texte | oui | longueur min `1` |
| `account` | texte | oui | longueur min `0`, longueur max `20` |
| `displayOrder` | entier |  |  |

<a id="m-WithholdingDtos-TypeUpdateRequest"></a>
#### WithholdingDtos.TypeUpdateRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `label` | texte |  | longueur min `0`, longueur max `150` |
| `rate` | nombre décimal |  |  |
| `base` | texte |  |  |
| `account` | texte |  | longueur min `0`, longueur max `20` |
| `displayOrder` | entier |  |  |

<a id="m-WithholdingDtos-CancelRequest"></a>
#### WithholdingDtos.CancelRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte | oui | longueur min `0`, longueur max `500` |

<a id="m-WithholdingDtos-CertificateRequest"></a>
#### WithholdingDtos.CertificateRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `certificateReference` | texte |  | longueur min `0`, longueur max `120` |
| `certificateDocument` | texte | oui | longueur min `0`, longueur max `120` |
