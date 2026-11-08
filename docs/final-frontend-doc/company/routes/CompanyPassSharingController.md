# CompanyPassSharingController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [company](../README.md) · 4 route(s).

## Routes

<a id="r-get-sni-api-v1-companies-customerid-passes"></a>
### GET /sni/api/v1/companies/{customerId}/passes

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Réponse : `List<PassSharingDtos.PassSummary>`
- Modèles : [PassSharingDtos.PassSummary](#m-PassSharingDtos-PassSummary)
- Code : `src/main/java/com/sni/bokaticowork/features/company/sharing/controller/CompanyPassSharingController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-companies-customerid-passes-passnumber-beneficiaries"></a>
### GET /sni/api/v1/companies/{customerId}/passes/{passNumber}/beneficiaries

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Réponse : `PassSharingDtos.PassView`
- Modèles : [PassSharingDtos.PassView](#m-PassSharingDtos-PassView)
- Code : `src/main/java/com/sni/bokaticowork/features/company/sharing/controller/CompanyPassSharingController.java`, méthode `view`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `passNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-companies-customerid-passes-passnumber-beneficiaries"></a>
### POST /sni/api/v1/companies/{customerId}/passes/{passNumber}/beneficiaries

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Corps JSON (obligatoire) : `PassSharingDtos.AddBeneficiaryRequest`
- Réponse : `PassSharingDtos.PassView`
- Modèles : [PassSharingDtos.PassView](#m-PassSharingDtos-PassView), [PassSharingDtos.AddBeneficiaryRequest](#m-PassSharingDtos-AddBeneficiaryRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/company/sharing/controller/CompanyPassSharingController.java`, méthode `add`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `passNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-companies-customerid-passes-passnumber-beneficiaries-accessid-revoke"></a>
### POST /sni/api/v1/companies/{customerId}/passes/{passNumber}/beneficiaries/{accessId}/revoke

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Corps JSON (facultatif) : `PassSharingDtos.RevokeBeneficiaryRequest`
- Réponse : `PassSharingDtos.RevokeResult`
- Modèles : [PassSharingDtos.RevokeResult](#m-PassSharingDtos-RevokeResult), [PassSharingDtos.RevokeBeneficiaryRequest](#m-PassSharingDtos-RevokeBeneficiaryRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/company/sharing/controller/CompanyPassSharingController.java`, méthode `revoke`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `passNumber` | chemin | `String` | oui |  |
| `accessId` | chemin | `Long` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-PassSharingDtos-PassSummary"></a>
#### PassSharingDtos.PassSummary

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `passNumber` | texte |  |  |
| `name` | texte |  |  |
| `passType` | texte |  |  |
| `status` | texte |  |  |
| `validFrom` | date-heure ISO 8601 UTC |  |  |
| `validUntil` | date-heure ISO 8601 UTC |  |  |
| `maxUses` | entier |  |  |
| `usedCount` | entier |  |  |
| `beneficiaries` | entier int64 ⚠ |  |  |

<a id="m-PassSharingDtos-PassView"></a>
#### PassSharingDtos.PassView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `companyCustomerCode` | texte |  |  |
| `pass` | [PassSharingDtos.PassSummary](#m-PassSharingDtos-PassSummary) |  |  |
| `entitlements` | [PassSharingDtos.PassEntitlement](#m-PassSharingDtos-PassEntitlement)[] |  |  |
| `beneficiaries` | [PassSharingDtos.BeneficiaryView](#m-PassSharingDtos-BeneficiaryView)[] |  |  |

<a id="m-PassSharingDtos-AddBeneficiaryRequest"></a>
#### PassSharingDtos.AddBeneficiaryRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `memberId` | texte | oui | longueur min `1` |
| `maxUses` | entier |  |  |
| `validFrom` | date-heure ISO 8601 UTC |  |  |
| `validUntil` | date-heure ISO 8601 UTC |  |  |

<a id="m-PassSharingDtos-RevokeResult"></a>
#### PassSharingDtos.RevokeResult

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `memberId` | texte |  |  |
| `futureBookings` | texte[] |  |  |
| `warning` | texte |  |  |

<a id="m-PassSharingDtos-RevokeBeneficiaryRequest"></a>
#### PassSharingDtos.RevokeBeneficiaryRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte |  | longueur min `0`, longueur max `255` |

<a id="m-PassSharingDtos-PassEntitlement"></a>
#### PassSharingDtos.PassEntitlement

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `entitlementCode` | texte |  |  |
| `name` | texte |  |  |
| `unit` | texte |  |  |
| `unlimited` | booléen |  |  |
| `granted` | nombre décimal |  |  |
| `remaining` | nombre décimal |  |  |

<a id="m-PassSharingDtos-BeneficiaryView"></a>
#### PassSharingDtos.BeneficiaryView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `memberId` | texte |  |  |
| `memberName` | texte |  |  |
| `open` | booléen |  |  |
| `maxUses` | entier |  |  |
| `usedCount` | entier |  |  |
| `validFrom` | date-heure ISO 8601 UTC |  |  |
| `validUntil` | date-heure ISO 8601 UTC |  |  |
| `addedAt` | date-heure ISO 8601 UTC |  |  |
| `addedBy` | texte |  |  |
| `revokedAt` | date-heure ISO 8601 UTC |  |  |
| `revokedReason` | texte |  |  |
| `consumption` | [PassSharingDtos.BeneficiaryConsumption](#m-PassSharingDtos-BeneficiaryConsumption)[] |  |  |

<a id="m-PassSharingDtos-BeneficiaryConsumption"></a>
#### PassSharingDtos.BeneficiaryConsumption

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `entitlementCode` | texte |  |  |
| `consumed` | nombre décimal |  |  |
