# UserProvisioningController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [securite](../README.md) · 2 route(s).

## Routes

<a id="r-post-sni-api-v1-admin-provisioning-bootstrap-admin"></a>
### POST /sni/api/v1/admin/provisioning/bootstrap-admin

- Accès : public, sans jeton
- Corps JSON (obligatoire) : `UserRequest`
- Réponse : `UserResponse`
- Modèles : [UserResponse](#m-UserResponse), [UserRequest](#m-UserRequest)
- Code : `src/main/java/com/sni/bokaticowork/security/admin/provisioning/controller/UserProvisioningController.java`, méthode `bootstrapAdmin`

<a id="r-post-sni-api-v1-admin-provisioning-staff"></a>
### POST /sni/api/v1/admin/provisioning/staff

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS`
- Corps JSON (obligatoire) : `UserRequest`
- Réponse : `UserResponse`
- Modèles : [UserResponse](#m-UserResponse), [UserRequest](#m-UserRequest)
- Code : `src/main/java/com/sni/bokaticowork/security/admin/provisioning/controller/UserProvisioningController.java`, méthode `createStaff`

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-UserResponse"></a>
#### UserResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `userId` | texte |  |  |
| `email` | texte |  |  |
| `firstname` | texte |  |  |
| `lastname` | texte |  |  |
| `businessEntityCode` | texte |  |  |
| `businessEntityName` | texte |  |  |
| `status` | texte |  |  |
| `generatedPassword` | texte |  |  |

<a id="m-UserRequest"></a>
#### UserRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `email` | string (email) | oui | longueur min `1` |
| `firstname` | texte |  |  |
| `lastname` | texte |  |  |
| `password` | texte | oui | longueur min `1` |
| `generatePassword` | booléen |  |  |
| `bootstrapSecret` | texte |  |  |
