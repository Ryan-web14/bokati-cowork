# KycDocumentAdminController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [document](../README.md) · 2 route(s).

## Routes

<a id="r-post-sni-api-v1-kyc-documents-admin-upload"></a>
### POST /sni/api/v1/kyc/documents/admin/upload

- Accès : back-office : rôle d'administration et droit `KYC:READ`
- Envoi : `multipart/form-data`
- Réponse : `KycDocumentResponse`
- Modèles : [KycDocumentResponse](#m-KycDocumentResponse), [DocumentOwnerType](#m-DocumentOwnerType)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycDocumentAdminController.java`, méthode `upload`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `ownerType` | requête | `DocumentOwnerType` | oui |  |
| `ownerCode` | requête | `String` | oui |  |
| `documentTypeCode` | requête | `String` | oui |  |
| `side` | requête | `String` |  | FRONT |
| `documentNumber` | requête | `String` |  |  |
| `issueDate` | requête | `LocalDate` |  |  |
| `expiryDate` | requête | `LocalDate` |  |  |
| `file` | partie multipart | `MultipartFile` | oui |  |

<a id="r-patch-sni-api-v1-kyc-documents-admin-kycdocumentid-details"></a>
### PATCH /sni/api/v1/kyc/documents/admin/{kycDocumentId}/details

- Accès : back-office : rôle d'administration et droit `KYC:READ`
- Corps JSON (obligatoire) : `KycDocumentDetailsRequest`
- Réponse : `KycDocumentResponse`
- Modèles : [KycDocumentResponse](#m-KycDocumentResponse), [KycDocumentDetailsRequest](#m-KycDocumentDetailsRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycDocumentAdminController.java`, méthode `updateDetails`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `kycDocumentId` | chemin | `Long` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-KycDocumentResponse"></a>
#### KycDocumentResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `documentCode` | texte |  |  |
| `documentType` | texte |  |  |
| `ownerName` | texte |  |  |
| `documentNumber` | texte |  |  |
| `fileName` | texte |  |  |
| `fileSize` | entier int64 ⚠ |  |  |
| `mimeType` | texte |  |  |
| `previewUrl` | texte |  |  |
| `downloadUrl` | texte |  |  |
| `requiresBackSide` | booléen |  |  |
| `backDocumentCode` | texte |  |  |
| `backFileName` | texte |  |  |
| `backFileSize` | entier int64 ⚠ |  |  |
| `backMimeType` | texte |  |  |
| `backPreviewUrl` | texte |  |  |
| `backDownloadUrl` | texte |  |  |
| `issueDate` | date AAAA-MM-JJ |  |  |
| `expiryDate` | date AAAA-MM-JJ |  |  |
| `status` | enum : `PENDING`, `VERIFIED`, `REJECTED`, `EXPIRED` |  |  |
| `uploadedBy` | entier int64 ⚠ |  |  |
| `uploadedByEmail` | texte |  |  |
| `uploadedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-DocumentOwnerType"></a>
#### DocumentOwnerType

Énumération : `CUSTOMER`, `MEMBER`, `BUSINESS`, `CONTRACT`, `INVOICE`, `PAYMENT`, `PROPOSAL`, `ASSET`, `INTERNAL`

<a id="m-KycDocumentDetailsRequest"></a>
#### KycDocumentDetailsRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentNumber` | texte |  |  |
| `issueDate` | date AAAA-MM-JJ |  |  |
| `expiryDate` | date AAAA-MM-JJ |  |  |
