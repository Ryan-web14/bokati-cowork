# TaxRuleController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [billing](../README.md) · 7 route(s).

## Routes

<a id="r-get-sni-api-v1-billing-tax-rules"></a>
### GET /sni/api/v1/billing/tax-rules

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `List<CatalogReferenceDtos.TaxRuleResponse>`
- Modèles : [CatalogReferenceDtos.TaxRuleResponse](#m-CatalogReferenceDtos-TaxRuleResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/TaxRuleController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `includeInactive` | requête | `boolean` |  | false |

<a id="r-post-sni-api-v1-billing-tax-rules"></a>
### POST /sni/api/v1/billing/tax-rules

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE` ; contrôle supplémentaire sur la méthode : `hasAuthority('BILLING:TAX_RULES')`
- Corps JSON (obligatoire) : `CatalogReferenceDtos.TaxRuleRequest`
- Réponse : `CatalogReferenceDtos.TaxRuleResponse`
- Modèles : [CatalogReferenceDtos.TaxRuleResponse](#m-CatalogReferenceDtos-TaxRuleResponse), [CatalogReferenceDtos.TaxRuleRequest](#m-CatalogReferenceDtos-TaxRuleRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/TaxRuleController.java`, méthode `create`

<a id="r-post-sni-api-v1-billing-tax-rules-basecode-successor"></a>
### POST /sni/api/v1/billing/tax-rules/{baseCode}/successor

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE` ; contrôle supplémentaire sur la méthode : `hasAuthority('BILLING:TAX_RULES')`
- Corps JSON (obligatoire) : `CatalogReferenceDtos.TaxRuleSuccessorRequest`
- Réponse : `CatalogReferenceDtos.TaxRuleResponse`
- Modèles : [CatalogReferenceDtos.TaxRuleResponse](#m-CatalogReferenceDtos-TaxRuleResponse), [CatalogReferenceDtos.TaxRuleSuccessorRequest](#m-CatalogReferenceDtos-TaxRuleSuccessorRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/TaxRuleController.java`, méthode `successor`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `baseCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-billing-tax-rules-basecode-versions"></a>
### GET /sni/api/v1/billing/tax-rules/{baseCode}/versions

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `List<CatalogReferenceDtos.TaxRuleResponse>`
- Modèles : [CatalogReferenceDtos.TaxRuleResponse](#m-CatalogReferenceDtos-TaxRuleResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/TaxRuleController.java`, méthode `versions`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `baseCode` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-billing-tax-rules-code"></a>
### PUT /sni/api/v1/billing/tax-rules/{code}

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE` ; contrôle supplémentaire sur la méthode : `hasAuthority('BILLING:TAX_RULES')`
- Corps JSON (obligatoire) : `CatalogReferenceDtos.TaxRuleUpdateRequest`
- Réponse : `CatalogReferenceDtos.TaxRuleResponse`
- Modèles : [CatalogReferenceDtos.TaxRuleResponse](#m-CatalogReferenceDtos-TaxRuleResponse), [CatalogReferenceDtos.TaxRuleUpdateRequest](#m-CatalogReferenceDtos-TaxRuleUpdateRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/TaxRuleController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-billing-tax-rules-code-activate"></a>
### PATCH /sni/api/v1/billing/tax-rules/{code}/activate

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE` ; contrôle supplémentaire sur la méthode : `hasAuthority('BILLING:TAX_RULES')`
- Réponse : `CatalogReferenceDtos.TaxRuleResponse`
- Modèles : [CatalogReferenceDtos.TaxRuleResponse](#m-CatalogReferenceDtos-TaxRuleResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/TaxRuleController.java`, méthode `activate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-billing-tax-rules-code-deactivate"></a>
### PATCH /sni/api/v1/billing/tax-rules/{code}/deactivate

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE` ; contrôle supplémentaire sur la méthode : `hasAuthority('BILLING:TAX_RULES')`
- Réponse : `CatalogReferenceDtos.TaxRuleResponse`
- Modèles : [CatalogReferenceDtos.TaxRuleResponse](#m-CatalogReferenceDtos-TaxRuleResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/TaxRuleController.java`, méthode `deactivate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-CatalogReferenceDtos-TaxRuleResponse"></a>
#### CatalogReferenceDtos.TaxRuleResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `code` | texte |  |  |
| `baseCode` | texte |  |  |
| `name` | texte |  |  |
| `taxType` | texte |  |  |
| `rate` | nombre décimal |  |  |
| `appliesOn` | texte |  |  |
| `treatment` | texte |  |  |
| `labelOnDocument` | texte |  |  |
| `exemptionReason` | texte |  |  |
| `countryCode` | texte |  |  |
| `validFrom` | date AAAA-MM-JJ |  |  |
| `validUntil` | date AAAA-MM-JJ |  |  |
| `active` | booléen |  |  |
| `displayOrder` | entier |  |  |
| `currentlyValid` | booléen |  |  |
| `selectableOnItem` | booléen |  |  |

<a id="m-CatalogReferenceDtos-TaxRuleRequest"></a>
#### CatalogReferenceDtos.TaxRuleRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte | oui | longueur min `0`, longueur max `120` |
| `name` | texte | oui | longueur min `0`, longueur max `255` |
| `taxType` | texte |  |  |
| `rate` | nombre décimal | oui | min `0` |
| `treatment` | texte |  |  |
| `labelOnDocument` | texte |  | longueur min `0`, longueur max `255` |
| `exemptionReason` | texte |  | longueur min `0`, longueur max `500` |
| `countryCode` | texte |  | longueur min `0`, longueur max `3` |
| `validFrom` | date AAAA-MM-JJ |  |  |
| `displayOrder` | entier |  |  |

<a id="m-CatalogReferenceDtos-TaxRuleSuccessorRequest"></a>
#### CatalogReferenceDtos.TaxRuleSuccessorRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `rate` | nombre décimal | oui | min `0` |
| `validFrom` | date AAAA-MM-JJ | oui |  |
| `name` | texte |  | longueur min `0`, longueur max `255` |
| `labelOnDocument` | texte |  | longueur min `0`, longueur max `255` |

<a id="m-CatalogReferenceDtos-TaxRuleUpdateRequest"></a>
#### CatalogReferenceDtos.TaxRuleUpdateRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte |  | longueur min `0`, longueur max `255` |
| `labelOnDocument` | texte |  | longueur min `0`, longueur max `255` |
| `exemptionReason` | texte |  | longueur min `0`, longueur max `500` |
| `displayOrder` | entier |  |  |
