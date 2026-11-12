# PassCredentialController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [subscription](../README.md) · 3 route(s).

## Routes

<a id="r-delete-sni-api-v1-passes-passnumber-credentials"></a>
### DELETE /sni/api/v1/passes/{passNumber}/credentials

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:UPDATE`
- Réponse : `Map<String, Integer>`
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/pass/controller/PassCredentialController.java`, méthode `revokeAll`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `passNumber` | chemin | `String` | oui |  |
| `reason` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-passes-passnumber-credentials"></a>
### GET /sni/api/v1/passes/{passNumber}/credentials

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:READ`
- Réponse : `PassCredentialService.IssuedCredential`
- Modèles : [PassCredentialService.IssuedCredential](#m-PassCredentialService-IssuedCredential), [PassCredentialType](#m-PassCredentialType)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/pass/controller/PassCredentialController.java`, méthode `current`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `passNumber` | chemin | `String` | oui |  |
| `type` | requête | `PassCredentialType` |  |  |

<a id="r-post-sni-api-v1-passes-passnumber-credentials"></a>
### POST /sni/api/v1/passes/{passNumber}/credentials

- Accès : back-office : rôle d'administration et droit `SUBSCRIPTION:CREATE`
- Réponse : `PassCredentialService.IssuedCredential`
- Modèles : [PassCredentialService.IssuedCredential](#m-PassCredentialService-IssuedCredential), [PassCredentialType](#m-PassCredentialType)
- Code : `src/main/java/com/sni/bokaticowork/features/subscription/subscription/pass/controller/PassCredentialController.java`, méthode `issue`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `passNumber` | chemin | `String` | oui |  |
| `type` | requête | `PassCredentialType` |  |  |
| `rotating` | requête | `boolean` |  | true |
| `issuedBy` | requête | `String` |  |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-PassCredentialService-IssuedCredential"></a>
#### PassCredentialService.IssuedCredential

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `value` | texte |  |  |
| `credentialType` | enum : `QR_CODE`, `BARCODE`, `NFC_TAG`, `ACCESS_CODE` |  |  |
| `rotating` | booléen |  |  |
| `validUntil` | date-heure ISO 8601 UTC |  |  |
| `imageDataUri` | texte |  |  |

<a id="m-PassCredentialType"></a>
#### PassCredentialType

Énumération : `QR_CODE`, `BARCODE`, `NFC_TAG`, `ACCESS_CODE`
