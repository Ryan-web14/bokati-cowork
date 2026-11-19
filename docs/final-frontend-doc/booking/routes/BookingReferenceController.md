# BookingReferenceController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [booking](../README.md) · 2 route(s).

## Routes

<a id="r-get-sni-api-v1-bookings-lookup-owners"></a>
### GET /sni/api/v1/bookings/lookup/owners

- Accès : back-office : rôle d'administration et droit `BOOKING:READ`
- Réponse : `PaginatedResponse<LookupItem>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [LookupItem](#m-LookupItem)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingReferenceController.java`, méthode `owners`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `q` | requête | `String` |  |  |
| `types` | requête | `String` |  |  |
| `page` | requête | `int` |  | 0 |
| `size` | requête | `int` |  | 20 |

<a id="r-get-sni-api-v1-bookings-reference"></a>
### GET /sni/api/v1/bookings/reference

- Accès : back-office : rôle d'administration et droit `BOOKING:READ`
- Réponse : `BookingReferenceController.BookingReference`
- Modèles : [BookingReferenceController.BookingReference](#m-BookingReferenceController-BookingReference)
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/BookingReferenceController.java`, méthode `reference`

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-LookupItem"></a>
#### LookupItem

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `type` | texte |  |  |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `hint` | texte |  |  |
| `active` | booléen |  |  |

<a id="m-BookingReferenceController-BookingReference"></a>
#### BookingReferenceController.BookingReference

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `enums` | [ReferenceDtos.EnumGroup](#m-ReferenceDtos-EnumGroup)[] |  |  |
| `lists` | objet { clé : [ReferenceDtos.Value](#m-ReferenceDtos-Value)[] } |  |  |
| `cancellationPolicy` | [CancellationPolicyResponse](#m-CancellationPolicyResponse)[] |  |  |

<a id="m-ReferenceDtos-EnumGroup"></a>
#### ReferenceDtos.EnumGroup

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `module` | texte |  |  |
| `group` | texte |  |  |
| `label` | texte |  |  |
| `values` | [ReferenceDtos.EnumValue](#m-ReferenceDtos-EnumValue)[] |  |  |

<a id="m-ReferenceDtos-Value"></a>
#### ReferenceDtos.Value

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `listCode` | texte |  |  |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `sortOrder` | entier |  |  |
| `active` | booléen |  |  |
| `requiresText` | booléen |  |  |
| `systemValue` | booléen |  |  |
| `autoAdded` | booléen |  |  |
| `usageCount` | entier int64 (texte au-delà de 2^53) |  |  |

<a id="m-CancellationPolicyResponse"></a>
#### CancellationPolicyResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | code identifiant (texte) |  |  |
| `hoursBeforeStart` | entier |  |  |
| `refundPercentage` | nombre décimal |  |  |
| `ruleOrder` | entier |  |  |
| `active` | booléen |  |  |

<a id="m-ReferenceDtos-EnumValue"></a>
#### ReferenceDtos.EnumValue

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `description` | texte |  |  |
| `order` | entier |  |  |
| `selectable` | booléen |  |  |
