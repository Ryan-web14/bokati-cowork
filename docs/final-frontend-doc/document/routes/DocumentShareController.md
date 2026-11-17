# DocumentShareController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [document](../README.md) · 6 route(s).

## Routes

<a id="r-post-sni-api-v1-document-folders-foldercode-share"></a>
### POST /sni/api/v1/document-folders/{folderCode}/share

- Accès : back-office : rôle d'administration et droit `DOCUMENT:WRITE`
- Corps JSON (obligatoire) : `CreateShareLinkRequest`
- Réponse : `ShareLinkResponse`
- Modèles : [ShareLinkResponse](#m-ShareLinkResponse), [CreateShareLinkRequest](#m-CreateShareLinkRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentShareController.java`, méthode `shareFolder`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `folderCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-documents-documentcode-share"></a>
### POST /sni/api/v1/documents/{documentCode}/share

- Accès : back-office : rôle d'administration et droit `DOCUMENT:UPLOAD`
- Corps JSON (obligatoire) : `CreateShareLinkRequest`
- Réponse : `ShareLinkResponse`
- Modèles : [ShareLinkResponse](#m-ShareLinkResponse), [CreateShareLinkRequest](#m-CreateShareLinkRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentShareController.java`, méthode `shareDocument`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-documents-documentcode-shares"></a>
### GET /sni/api/v1/documents/{documentCode}/shares

- Accès : back-office : rôle d'administration et droit `DOCUMENT:READ`
- Réponse : `List<ShareLinkResponse>`
- Modèles : [ShareLinkResponse](#m-ShareLinkResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentShareController.java`, méthode `listByDocument`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentCode` | chemin | `String` | oui |  |

<a id="r-delete-sni-api-v1-shares-token"></a>
### DELETE /sni/api/v1/shares/{token}

- Accès : back-office : rôle d'administration et droit `DOCUMENT:WRITE`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentShareController.java`, méthode `revoke`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `token` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-shares-token"></a>
### GET /sni/api/v1/shares/{token}

- Accès : back-office : rôle d'administration et droit `DOCUMENT:READ`
- Réponse : `ShareLinkResponse`
- Modèles : [ShareLinkResponse](#m-ShareLinkResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentShareController.java`, méthode `access`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `token` | chemin | `String` | oui |  |
| `X-Share-Password` | en-tête | `String` |  |  |

<a id="r-get-sni-api-v1-shares-token-file"></a>
### GET /sni/api/v1/shares/{token}/file

- Accès : back-office : rôle d'administration et droit `DOCUMENT:READ`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentShareController.java`, méthode `file`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `token` | chemin | `String` | oui |  |
| `X-Share-Password` | en-tête | `String` |  |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ShareLinkResponse"></a>
#### ShareLinkResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `token` | texte |  |  |
| `shareUrl` | texte |  |  |
| `documentCode` | texte |  |  |
| `folderCode` | texte |  |  |
| `allowDownload` | booléen |  |  |
| `passwordProtected` | booléen |  |  |
| `maxAccessCount` | entier |  |  |
| `accessCount` | entier |  |  |
| `expiresAt` | date-heure ISO 8601 UTC |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `createdBy` | entier int64 ⚠ |  |  |
| `createdByName` | texte |  |  |
| `active` | booléen |  |  |

<a id="m-CreateShareLinkRequest"></a>
#### CreateShareLinkRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `expiresInHours` | entier | oui |  |
| `password` | texte |  |  |
| `allowDownload` | booléen |  |  |
| `maxAccessCount` | entier |  |  |
