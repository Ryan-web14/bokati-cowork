# StaffProfileController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [securite](../README.md) · 5 route(s).

## Routes

<a id="r-get-sni-api-v1-admin-users-usercode-staff-profile"></a>
### GET /sni/api/v1/admin/users/{userCode}/staff-profile

- Accès : back-office : rôle d'administration et droit `SYSTEM:USERS` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:USERS')`
- Réponse : `StaffProfileResponse`
- Modèles : [StaffProfileResponse](#m-StaffProfileResponse)
- Code : `src/main/java/com/sni/bokaticowork/security/admin/user/controller/StaffProfileController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `userCode` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-admin-users-usercode-staff-profile"></a>
### PUT /sni/api/v1/admin/users/{userCode}/staff-profile

- Accès : back-office : rôle d'administration et droit `SYSTEM:USERS` ; contrôle supplémentaire sur la méthode : `hasAuthority('SYSTEM:USERS')`
- Corps JSON (obligatoire) : `UpdateStaffProfileRequest`
- Réponse : `StaffProfileResponse`
- Modèles : [StaffProfileResponse](#m-StaffProfileResponse), [UpdateStaffProfileRequest](#m-UpdateStaffProfileRequest)
- Code : `src/main/java/com/sni/bokaticowork/security/admin/user/controller/StaffProfileController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `userCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-staff-me-profile"></a>
### GET /sni/api/v1/staff/me/profile

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `StaffProfileResponse`
- Modèles : [StaffProfileResponse](#m-StaffProfileResponse)
- Code : `src/main/java/com/sni/bokaticowork/security/admin/user/controller/StaffProfileController.java`, méthode `me`

<a id="r-delete-sni-api-v1-staff-me-signature"></a>
### DELETE /sni/api/v1/staff/me/signature

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `StaffProfileResponse`
- Modèles : [StaffProfileResponse](#m-StaffProfileResponse)
- Code : `src/main/java/com/sni/bokaticowork/security/admin/user/controller/StaffProfileController.java`, méthode `clearMySignature`

<a id="r-put-sni-api-v1-staff-me-signature"></a>
### PUT /sni/api/v1/staff/me/signature

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (obligatoire) : `UpdateMySignatureRequest`
- Réponse : `StaffProfileResponse`
- Modèles : [StaffProfileResponse](#m-StaffProfileResponse), [UpdateMySignatureRequest](#m-UpdateMySignatureRequest)
- Code : `src/main/java/com/sni/bokaticowork/security/admin/user/controller/StaffProfileController.java`, méthode `updateMySignature`

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-StaffProfileResponse"></a>
#### StaffProfileResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `userCode` | texte |  |  |
| `email` | texte |  |  |
| `fullName` | texte |  |  |
| `jobTitle` | texte |  |  |
| `hasSignature` | booléen |  |  |
| `signatureImageBase64` | texte |  |  |
| `signatureUpdatedAt` | date-heure ISO 8601 UTC |  |  |
| `managerUserCode` | texte |  |  |
| `managerName` | texte |  |  |
| `managerEmail` | texte |  |  |

<a id="m-UpdateStaffProfileRequest"></a>
#### UpdateStaffProfileRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `jobTitle` | texte |  | longueur min `0`, longueur max `160` |
| `managerUserCode` | texte |  |  |
| `signatureImageBase64` | texte |  |  |
| `clearSignature` | booléen |  |  |

<a id="m-UpdateMySignatureRequest"></a>
#### UpdateMySignatureRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `signatureImageBase64` | texte | oui | longueur min `1` |
