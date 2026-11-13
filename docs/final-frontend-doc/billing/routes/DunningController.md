# DunningController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [billing](../README.md) · 8 route(s).

## Routes

<a id="r-get-sni-api-v1-billing-dunning-notices"></a>
### GET /sni/api/v1/billing/dunning/notices

- Accès : back-office : rôle d'administration et droit `BILLING:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `List<DunningController.NoticeView>`
- Modèles : [DunningController.NoticeView](#m-DunningController-NoticeView)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/dunning/controller/DunningController.java`, méthode `recent`

<a id="r-get-sni-api-v1-billing-dunning-notices-customers-customertype-customercode"></a>
### GET /sni/api/v1/billing/dunning/notices/customers/{customerType}/{customerCode}

- Accès : back-office : rôle d'administration et droit `BILLING:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `List<DunningController.NoticeView>`
- Modèles : [DunningController.NoticeView](#m-DunningController-NoticeView)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/dunning/controller/DunningController.java`, méthode `ofCustomer`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerType` | chemin | `String` | oui |  |
| `customerCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-billing-dunning-notices-documents-documentnumber"></a>
### GET /sni/api/v1/billing/dunning/notices/documents/{documentNumber}

- Accès : back-office : rôle d'administration et droit `BILLING:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `List<DunningController.NoticeView>`
- Modèles : [DunningController.NoticeView](#m-DunningController-NoticeView)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/dunning/controller/DunningController.java`, méthode `ofDocument`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-billing-dunning-policies"></a>
### GET /sni/api/v1/billing/dunning/policies

- Accès : back-office : rôle d'administration et droit `BILLING:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `List<DunningController.PolicyView>`
- Modèles : [DunningController.PolicyView](#m-DunningController-PolicyView)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/dunning/controller/DunningController.java`, méthode `policies`

<a id="r-put-sni-api-v1-billing-dunning-policies"></a>
### PUT /sni/api/v1/billing/dunning/policies

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Corps JSON (obligatoire) : `DunningController.PolicyRequest`
- Réponse : `DunningController.PolicyView`
- Modèles : [DunningController.PolicyView](#m-DunningController-PolicyView), [DunningController.PolicyRequest](#m-DunningController-PolicyRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/dunning/controller/DunningController.java`, méthode `save`

<a id="r-get-sni-api-v1-billing-dunning-policies-policycode"></a>
### GET /sni/api/v1/billing/dunning/policies/{policyCode}

- Accès : back-office : rôle d'administration et droit `BILLING:READ` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `DunningController.PolicyView`
- Modèles : [DunningController.PolicyView](#m-DunningController-PolicyView)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/dunning/controller/DunningController.java`, méthode `policy`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `policyCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-billing-dunning-policies-policycode-deactivate"></a>
### POST /sni/api/v1/billing/dunning/policies/{policyCode}/deactivate

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Réponse : `DunningController.PolicyView`
- Modèles : [DunningController.PolicyView](#m-DunningController-PolicyView)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/dunning/controller/DunningController.java`, méthode `deactivate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `policyCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-billing-dunning-run"></a>
### POST /sni/api/v1/billing/dunning/run

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Réponse : `DunningPolicyWorker.Outcome`
- Modèles : [DunningPolicyWorker.Outcome](#m-DunningPolicyWorker-Outcome)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/dunning/controller/DunningController.java`, méthode `run`

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-DunningController-NoticeView"></a>
#### DunningController.NoticeView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentNumber` | texte |  |  |
| `policyCode` | texte |  |  |
| `stepOrder` | entier |  |  |
| `action` | enum : `REMINDER`, `FORMAL_NOTICE`, `GRACE_PERIOD`, `SUSPEND`, `HANDOVER` |  |  |
| `customerType` | texte |  |  |
| `customerCode` | texte |  |  |
| `subscriptionNumber` | texte |  |  |
| `daysOverdue` | entier |  |  |
| `balanceDue` | nombre décimal |  |  |
| `outcome` | enum : `SENT`, `SKIPPED`, `FAILED` |  |  |
| `detail` | texte |  |  |
| `executedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-DunningController-PolicyView"></a>
#### DunningController.PolicyView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `policyCode` | texte |  |  |
| `name` | texte |  |  |
| `segment` | enum : `DEFAULT`, `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` |  |  |
| `tone` | enum : `SOFT`, `STANDARD`, `FIRM` |  |  |
| `active` | booléen |  |  |
| `steps` | [DunningController.StepView](#m-DunningController-StepView)[] |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-DunningController-PolicyRequest"></a>
#### DunningController.PolicyRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `policyCode` | texte | oui | longueur min `1` |
| `name` | texte | oui | longueur min `1` |
| `segment` | enum : `DEFAULT`, `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` |  |  |
| `tone` | enum : `SOFT`, `STANDARD`, `FIRM` |  |  |
| `active` | booléen |  |  |
| `steps` | [DunningController.StepRequest](#m-DunningController-StepRequest)[] | oui | éléments min `1` |

<a id="m-DunningPolicyWorker-Outcome"></a>
#### DunningPolicyWorker.Outcome

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documents` | entier |  |  |
| `notices` | entier |  |  |
| `failed` | entier |  |  |

<a id="m-DunningController-StepView"></a>
#### DunningController.StepView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `stepOrder` | entier |  |  |
| `daysAfterDue` | entier |  |  |
| `action` | enum : `REMINDER`, `FORMAL_NOTICE`, `GRACE_PERIOD`, `SUSPEND`, `HANDOVER` |  |  |
| `channel` | enum : `EMAIL`, `IN_APP`, `STAFF`, `WHATSAPP` |  |  |
| `subjectTemplate` | texte |  |  |
| `messageTemplate` | texte |  |  |

<a id="m-DunningController-StepRequest"></a>
#### DunningController.StepRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `daysAfterDue` | entier |  |  |
| `action` | enum : `REMINDER`, `FORMAL_NOTICE`, `GRACE_PERIOD`, `SUSPEND`, `HANDOVER` |  |  |
| `channel` | enum : `EMAIL`, `IN_APP`, `STAFF`, `WHATSAPP` |  |  |
| `subjectTemplate` | texte |  |  |
| `messageTemplate` | texte |  |  |
