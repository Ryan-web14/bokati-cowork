# DocumentSignatureController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [document](../README.md) · 3 route(s).

## Routes

<a id="r-get-sni-api-v1-documents-documentcode-signatures"></a>
### GET /sni/api/v1/documents/{documentCode}/signatures

- Accès : back-office : rôle d'administration et droit `DOCUMENT:READ`
- Réponse : `List<DocumentSignatureResponse>`
- Modèles : [DocumentSignatureResponse](#m-DocumentSignatureResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentSignatureController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-documents-documentcode-signatures"></a>
### POST /sni/api/v1/documents/{documentCode}/signatures

- Accès : back-office : rôle d'administration et droit `DOCUMENT:UPLOAD`
- Corps JSON (obligatoire) : `CreateDocumentSignatureRequest`
- Réponse : `DocumentSignatureResponse`
- Modèles : [DocumentSignatureResponse](#m-DocumentSignatureResponse), [CreateDocumentSignatureRequest](#m-CreateDocumentSignatureRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentSignatureController.java`, méthode `requestSignature`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentCode` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-documents-documentcode-signatures-signatureid-sign"></a>
### PATCH /sni/api/v1/documents/{documentCode}/signatures/{signatureId}/sign

- Accès : back-office : rôle d'administration et droit `DOCUMENT:READ`
- Corps JSON (obligatoire) : `SignDocumentRequest`
- Réponse : `DocumentSignatureResponse`
- Modèles : [DocumentSignatureResponse](#m-DocumentSignatureResponse), [SignDocumentRequest](#m-SignDocumentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentSignatureController.java`, méthode `sign`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentCode` | chemin | `String` | oui |  |
| `signatureId` | chemin | `Long` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-DocumentSignatureResponse"></a>
#### DocumentSignatureResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `documentCode` | texte |  |  |
| `signerType` | texte |  |  |
| `signerId` | entier int64 ⚠ |  |  |
| `signerName` | texte |  |  |
| `signerEmail` | texte |  |  |
| `signatureStatus` | enum : `PENDING`, `SIGNED`, `DECLINED`, `EXPIRED` |  |  |
| `signedAt` | date-heure ISO 8601 UTC |  |  |
| `ipAddress` | texte |  |  |
| `userAgent` | texte |  |  |

<a id="m-CreateDocumentSignatureRequest"></a>
#### CreateDocumentSignatureRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `signerType` | texte | oui | longueur min `1` |
| `signerId` | entier int64 ⚠ | oui |  |
| `signerName` | texte | oui | longueur min `1` |
| `signerEmail` | string (email) |  |  |

<a id="m-SignDocumentRequest"></a>
#### SignDocumentRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `accepted` | booléen |  |  |
| `signatureData` | texte |  |  |
