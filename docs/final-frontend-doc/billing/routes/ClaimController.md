# ClaimController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [billing](../README.md) · 8 route(s).

## Routes

<a id="r-get-sni-api-v1-billing-claim-reasons"></a>
### GET /sni/api/v1/billing/claim-reasons

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `List<ClaimDtos.ReasonView>`
- Modèles : [ClaimDtos.ReasonView](#m-ClaimDtos-ReasonView)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/ClaimController.java`, méthode `reasons`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `includeInactive` | requête | `boolean` |  | false |

<a id="r-put-sni-api-v1-billing-claim-reasons"></a>
### PUT /sni/api/v1/billing/claim-reasons

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE` ; contrôle supplémentaire sur la méthode : `hasAuthority('BILLING:CATALOG_REFERENCES')`
- Corps JSON (obligatoire) : `ClaimDtos.ReasonRequest`
- Réponse : `ClaimDtos.ReasonView`
- Modèles : [ClaimDtos.ReasonView](#m-ClaimDtos-ReasonView), [ClaimDtos.ReasonRequest](#m-ClaimDtos-ReasonRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/ClaimController.java`, méthode `saveReason`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `active` | requête | `Boolean` |  |  |

<a id="r-get-sni-api-v1-billing-claims"></a>
### GET /sni/api/v1/billing/claims

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `List<ClaimDtos.ClaimView>`
- Modèles : [ClaimDtos.ClaimView](#m-ClaimDtos-ClaimView)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/ClaimController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | requête | `String` |  |  |
| `customerType` | requête | `String` |  |  |
| `customerCode` | requête | `String` |  |  |
| `openOnly` | requête | `boolean` |  | false |

<a id="r-get-sni-api-v1-billing-claims-claimnumber"></a>
### GET /sni/api/v1/billing/claims/{claimNumber}

- Accès : back-office : rôle d'administration et droit `BILLING:READ`
- Réponse : `ClaimDtos.ClaimView`
- Modèles : [ClaimDtos.ClaimView](#m-ClaimDtos-ClaimView)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/ClaimController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `claimNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-billing-claims-claimnumber-accept"></a>
### PATCH /sni/api/v1/billing/claims/{claimNumber}/accept

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Corps JSON (facultatif) : `ClaimDtos.AcceptRequest`
- Réponse : `ClaimDtos.ClaimView`
- Modèles : [ClaimDtos.ClaimView](#m-ClaimDtos-ClaimView), [ClaimDtos.AcceptRequest](#m-ClaimDtos-AcceptRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/ClaimController.java`, méthode `accept`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `claimNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-billing-claims-claimnumber-reject"></a>
### PATCH /sni/api/v1/billing/claims/{claimNumber}/reject

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Corps JSON (obligatoire) : `ClaimDtos.RejectRequest`
- Réponse : `ClaimDtos.ClaimView`
- Modèles : [ClaimDtos.ClaimView](#m-ClaimDtos-ClaimView), [ClaimDtos.RejectRequest](#m-ClaimDtos-RejectRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/ClaimController.java`, méthode `reject`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `claimNumber` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-billing-claims-claimnumber-withdraw"></a>
### PATCH /sni/api/v1/billing/claims/{claimNumber}/withdraw

- Accès : back-office : rôle d'administration et droit `BILLING:UPDATE`
- Réponse : `ClaimDtos.ClaimView`
- Modèles : [ClaimDtos.ClaimView](#m-ClaimDtos-ClaimView)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/ClaimController.java`, méthode `withdraw`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `claimNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-billing-documents-documentnumber-claims"></a>
### POST /sni/api/v1/billing/documents/{documentNumber}/claims

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Corps JSON (obligatoire) : `ClaimDtos.OpenRequest`
- Réponse : `ClaimDtos.ClaimView`
- Modèles : [ClaimDtos.ClaimView](#m-ClaimDtos-ClaimView), [ClaimDtos.OpenRequest](#m-ClaimDtos-OpenRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/ClaimController.java`, méthode `open`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ClaimDtos-ReasonView"></a>
#### ClaimDtos.ReasonView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `displayOrder` | entier |  |  |
| `active` | booléen |  |  |

<a id="m-ClaimDtos-ReasonRequest"></a>
#### ClaimDtos.ReasonRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte | oui | longueur min `0`, longueur max `40` |
| `label` | texte | oui | longueur min `0`, longueur max `150` |
| `displayOrder` | entier |  |  |

<a id="m-ClaimDtos-ClaimView"></a>
#### ClaimDtos.ClaimView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `claimNumber` | texte |  |  |
| `documentNumber` | texte |  |  |
| `customerType` | texte |  |  |
| `customerCode` | texte |  |  |
| `reasonCode` | texte |  |  |
| `reasonLabel` | texte |  |  |
| `description` | texte |  |  |
| `lineOrders` | entier[] |  |  |
| `requestedAmount` | nombre décimal |  |  |
| `status` | texte |  |  |
| `channel` | texte |  |  |
| `openedBy` | texte |  |  |
| `grantedAmount` | nombre décimal |  |  |
| `creditNoteNumber` | texte |  |  |
| `decisionComment` | texte |  |  |
| `decidedBy` | texte |  |  |
| `decidedAt` | date-heure ISO 8601 UTC |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-ClaimDtos-AcceptRequest"></a>
#### ClaimDtos.AcceptRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `grantedAmount` | nombre décimal |  | min `0.01` |
| `comment` | texte |  | longueur min `0`, longueur max `2000` |

<a id="m-ClaimDtos-RejectRequest"></a>
#### ClaimDtos.RejectRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `comment` | texte | oui | longueur min `0`, longueur max `2000` |

<a id="m-ClaimDtos-OpenRequest"></a>
#### ClaimDtos.OpenRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reasonCode` | texte | oui | longueur min `1` |
| `description` | texte |  | longueur min `0`, longueur max `4000` |
| `lineOrders` | entier[] |  |  |
| `requestedAmount` | nombre décimal |  | min `0` |
