# CustomerPriceController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [billing](../README.md) · 3 route(s).

## Routes

<a id="r-get-sni-api-v1-billing-customers-customertype-customercode-prices"></a>
### GET /sni/api/v1/billing/customers/{customerType}/{customerCode}/prices

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `List<CustomerPriceDtos.PriceView>`
- Modèles : [CustomerPriceDtos.PriceView](#m-CustomerPriceDtos-PriceView)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/CustomerPriceController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerType` | chemin | `String` | oui |  |
| `customerCode` | chemin | `String` | oui |  |
| `current` | requête | `boolean` |  | false |

<a id="r-post-sni-api-v1-billing-customers-customertype-customercode-prices"></a>
### POST /sni/api/v1/billing/customers/{customerType}/{customerCode}/prices

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Corps JSON (obligatoire) : `CustomerPriceDtos.PriceRequest`
- Réponse : `CustomerPriceDtos.PriceView`
- Modèles : [CustomerPriceDtos.PriceView](#m-CustomerPriceDtos-PriceView), [CustomerPriceDtos.PriceRequest](#m-CustomerPriceDtos-PriceRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/CustomerPriceController.java`, méthode `create`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerType` | chemin | `String` | oui |  |
| `customerCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-billing-customers-customertype-customercode-prices-id-end"></a>
### PATCH /sni/api/v1/billing/customers/{customerType}/{customerCode}/prices/{id}/end

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Corps JSON (obligatoire) : `CustomerPriceDtos.EndRequest`
- Réponse : `CustomerPriceDtos.PriceView`
- Modèles : [CustomerPriceDtos.PriceView](#m-CustomerPriceDtos-PriceView), [CustomerPriceDtos.EndRequest](#m-CustomerPriceDtos-EndRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/CustomerPriceController.java`, méthode `end`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerType` | chemin | `String` | oui |  |
| `customerCode` | chemin | `String` | oui |  |
| `id` | chemin | `Long` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-CustomerPriceDtos-PriceView"></a>
#### CustomerPriceDtos.PriceView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `customerType` | texte |  |  |
| `customerCode` | texte |  |  |
| `itemCode` | texte |  |  |
| `itemName` | texte |  |  |
| `unitPrice` | nombre décimal |  |  |
| `discountRate` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `validFrom` | date AAAA-MM-JJ |  |  |
| `validUntil` | date AAAA-MM-JJ |  |  |
| `currentlyValid` | booléen |  |  |
| `contractReference` | texte |  |  |
| `reason` | texte |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-CustomerPriceDtos-PriceRequest"></a>
#### CustomerPriceDtos.PriceRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemCode` | texte | oui | longueur min `1` |
| `unitPrice` | nombre décimal |  | min `0` |
| `discountRate` | nombre décimal |  | min `0`, max `100` |
| `validFrom` | date AAAA-MM-JJ | oui |  |
| `validUntil` | date AAAA-MM-JJ |  |  |
| `contractReference` | texte |  | longueur min `0`, longueur max `120` |
| `reason` | texte |  | longueur min `0`, longueur max `500` |

<a id="m-CustomerPriceDtos-EndRequest"></a>
#### CustomerPriceDtos.EndRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `validUntil` | date AAAA-MM-JJ | oui |  |
