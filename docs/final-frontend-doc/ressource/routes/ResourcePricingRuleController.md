# ResourcePricingRuleController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [ressource](../README.md) · 9 route(s).

## Routes

<a id="r-get-sni-api-v1-resource-pricing-rules"></a>
### GET /sni/api/v1/resource-pricing-rules

- Accès : back-office : rôle d'administration et droit `RESOURCE:PRICE`
- Réponse : `PaginatedResponse<ResourcePricingRuleResponse>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [ResourcePricingRuleResponse](#m-ResourcePricingRuleResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourcePricingRuleController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `resourceCode` | requête | `String` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20, sort=createdAt,DESC |

<a id="r-post-sni-api-v1-resource-pricing-rules"></a>
### POST /sni/api/v1/resource-pricing-rules

- Accès : back-office : rôle d'administration et droit `RESOURCE:PRICE`
- En-tête `Idempotency-Key` : conseillé (opération `RESOURCE_PRICING_RULE_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `CreateResourcePricingRuleRequest`
- Réponse : `vide`
- Modèles : [CreateResourcePricingRuleRequest](#m-CreateResourcePricingRuleRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourcePricingRuleController.java`, méthode `create`

<a id="r-post-sni-api-v1-resource-pricing-rules-bulk"></a>
### POST /sni/api/v1/resource-pricing-rules/bulk

- Accès : back-office : rôle d'administration et droit `RESOURCE:PRICE`
- Corps JSON (obligatoire) : `BulkCreateResourcePricingRuleRequest`
- Réponse : `BulkResourceOperationResponse`
- Modèles : [BulkResourceOperationResponse](#m-BulkResourceOperationResponse), [BulkCreateResourcePricingRuleRequest](#m-BulkCreateResourcePricingRuleRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourcePricingRuleController.java`, méthode `createBulk`

<a id="r-post-sni-api-v1-resource-pricing-rules-bulk-preview"></a>
### POST /sni/api/v1/resource-pricing-rules/bulk/preview

- Accès : back-office : rôle d'administration et droit `RESOURCE:PRICE`
- Corps JSON (obligatoire) : `BulkCreateResourcePricingRuleRequest`
- Réponse : `PricingRuleConflictPreviewResponse`
- Modèles : [PricingRuleConflictPreviewResponse](#m-PricingRuleConflictPreviewResponse), [BulkCreateResourcePricingRuleRequest](#m-BulkCreateResourcePricingRuleRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourcePricingRuleController.java`, méthode `previewBulk`

<a id="r-get-sni-api-v1-resource-pricing-rules-quote"></a>
### GET /sni/api/v1/resource-pricing-rules/quote

- Accès : back-office : rôle d'administration et droit `RESOURCE:PRICE`
- Réponse : `ResourcePriceQuoteResponse`
- Modèles : [ResourcePriceQuoteResponse](#m-ResourcePriceQuoteResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourcePricingRuleController.java`, méthode `quote`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `resourceCode` | requête | `String` | oui |  |
| `bookingUnit` | requête | `String` | oui |  |
| `startedAt` | requête | `LocalDateTime` | oui |  |
| `endedAt` | requête | `LocalDateTime` | oui |  |

<a id="r-delete-sni-api-v1-resource-pricing-rules-id"></a>
### DELETE /sni/api/v1/resource-pricing-rules/{id}

- Accès : back-office : rôle d'administration et droit `RESOURCE:PRICE`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourcePricingRuleController.java`, méthode `delete`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-resource-pricing-rules-id"></a>
### GET /sni/api/v1/resource-pricing-rules/{id}

- Accès : back-office : rôle d'administration et droit `RESOURCE:PRICE`
- Réponse : `ResourcePricingRuleResponse`
- Modèles : [ResourcePricingRuleResponse](#m-ResourcePricingRuleResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourcePricingRuleController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-patch-put-sni-api-v1-resource-pricing-rules-id"></a>
### PATCH | PUT /sni/api/v1/resource-pricing-rules/{id}

- Accès : back-office : rôle d'administration et droit `RESOURCE:PRICE`
- Corps JSON (obligatoire) : `UpdateResourcePricingRuleRequest`
- Réponse : `ResourcePricingRuleResponse`
- Modèles : [ResourcePricingRuleResponse](#m-ResourcePricingRuleResponse), [UpdateResourcePricingRuleRequest](#m-UpdateResourcePricingRuleRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourcePricingRuleController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-patch-sni-api-v1-resource-pricing-rules-id-active"></a>
### PATCH /sni/api/v1/resource-pricing-rules/{id}/active

- Accès : back-office : rôle d'administration et droit `RESOURCE:PRICE`
- En-tête `Idempotency-Key` : conseillé (opération `RESOURCE_PRICING_RULE_UPDATE_ACTIVE`, voir le README racine)
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourcePricingRuleController.java`, méthode `updateActive`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |
| `active` | requête | `Boolean` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ResourcePricingRuleResponse"></a>
#### ResourcePricingRuleResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `resourceCode` | texte |  |  |
| `bookingUnit` | texte |  |  |
| `price` | entier |  |  |
| `label` | texte |  |  |
| `dayOfWeek` | entier |  |  |
| `startsAt` | [LocalTime](#m-LocalTime) |  |  |
| `endsAt` | [LocalTime](#m-LocalTime) |  |  |
| `adjustmentType` | texte |  |  |
| `adjustmentValue` | entier |  |  |
| `validFrom` | date AAAA-MM-JJ |  |  |
| `validUntil` | date AAAA-MM-JJ |  |  |
| `lastMinuteMinutes` | entier |  |  |
| `priority` | entier |  |  |
| `active` | booléen |  |  |

<a id="m-CreateResourcePricingRuleRequest"></a>
#### CreateResourcePricingRuleRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte | oui | longueur min `1` |
| `bookingUnit` | texte | oui | longueur min `1` |
| `price` | entier | oui |  |
| `label` | texte |  |  |
| `dayOfWeek` | entier |  |  |
| `startsAt` | [LocalTime](#m-LocalTime) |  |  |
| `endsAt` | [LocalTime](#m-LocalTime) |  |  |
| `adjustmentType` | texte |  |  |
| `adjustmentValue` | entier |  |  |
| `validFrom` | date AAAA-MM-JJ |  |  |
| `validUntil` | date AAAA-MM-JJ |  |  |
| `lastMinuteMinutes` | entier |  |  |
| `priority` | entier |  |  |
| `active` | booléen |  |  |

<a id="m-BulkResourceOperationResponse"></a>
#### BulkResourceOperationResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `processed` | entier |  |  |
| `skipped` | entier |  |  |
| `results` | [BulkResourceOperationResponse.Entry](#m-BulkResourceOperationResponse-Entry)[] |  |  |

<a id="m-BulkCreateResourcePricingRuleRequest"></a>
#### BulkCreateResourcePricingRuleRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCodes` | texte[] | oui | éléments min `1` |
| `bookingUnit` | texte | oui | longueur min `1` |
| `price` | entier | oui |  |
| `label` | texte |  |  |
| `dayOfWeek` | entier |  |  |
| `startsAt` | [LocalTime](#m-LocalTime) |  |  |
| `endsAt` | [LocalTime](#m-LocalTime) |  |  |
| `adjustmentType` | texte |  |  |
| `adjustmentValue` | entier |  |  |
| `validFrom` | date AAAA-MM-JJ |  |  |
| `validUntil` | date AAAA-MM-JJ |  |  |
| `lastMinuteMinutes` | entier |  |  |
| `priority` | entier |  |  |
| `active` | booléen |  |  |

<a id="m-PricingRuleConflictPreviewResponse"></a>
#### PricingRuleConflictPreviewResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourcesInspected` | entier |  |  |
| `resourcesWithOverlap` | entier |  |  |
| `strictDuplicates` | entier |  |  |
| `results` | [PricingRuleConflictPreviewResponse.ResourcePreview](#m-PricingRuleConflictPreviewResponse-ResourcePreview)[] |  |  |

<a id="m-ResourcePriceQuoteResponse"></a>
#### ResourcePriceQuoteResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte |  |  |
| `bookingUnit` | texte |  |  |
| `startedAt` | date-heure locale sans fuseau |  |  |
| `endedAt` | date-heure locale sans fuseau |  |  |
| `basePrice` | entier |  |  |
| `finalPrice` | entier |  |  |
| `appliedRuleId` | entier int64 ⚠ |  |  |
| `appliedRuleLabel` | texte |  |  |
| `adjustmentType` | texte |  |  |
| `adjustmentValue` | entier |  |  |

<a id="m-UpdateResourcePricingRuleRequest"></a>
#### UpdateResourcePricingRuleRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `bookingUnit` | texte |  |  |
| `price` | entier |  |  |
| `label` | texte |  |  |
| `dayOfWeek` | entier |  |  |
| `startsAt` | [LocalTime](#m-LocalTime) |  |  |
| `endsAt` | [LocalTime](#m-LocalTime) |  |  |
| `adjustmentType` | texte |  |  |
| `adjustmentValue` | entier |  |  |
| `validFrom` | date AAAA-MM-JJ |  |  |
| `validUntil` | date AAAA-MM-JJ |  |  |
| `lastMinuteMinutes` | entier |  |  |
| `priority` | entier |  |  |
| `active` | booléen |  |  |

<a id="m-LocalTime"></a>
#### LocalTime

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `hour` | entier |  |  |
| `minute` | entier |  |  |
| `second` | entier |  |  |
| `nano` | entier |  |  |

<a id="m-BulkResourceOperationResponse-Entry"></a>
#### BulkResourceOperationResponse.Entry

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte |  |  |
| `status` | texte |  |  |
| `reason` | texte |  |  |
| `detail` | entier |  |  |

<a id="m-PricingRuleConflictPreviewResponse-ResourcePreview"></a>
#### PricingRuleConflictPreviewResponse.ResourcePreview

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `resourceCode` | texte |  |  |
| `duplicate` | booléen |  |  |
| `overlapping` | [PricingRuleConflictPreviewResponse.CompetingRule](#m-PricingRuleConflictPreviewResponse-CompetingRule)[] |  |  |

<a id="m-PricingRuleConflictPreviewResponse-CompetingRule"></a>
#### PricingRuleConflictPreviewResponse.CompetingRule

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `label` | texte |  |  |
| `price` | entier |  |  |
| `priority` | entier |  |  |
| `dayOfWeek` | entier |  |  |
| `startsAt` | texte |  |  |
| `endsAt` | texte |  |  |
| `wins` | booléen |  |  |
