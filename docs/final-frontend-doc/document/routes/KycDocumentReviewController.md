# KycDocumentReviewController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [document](../README.md) · 11 route(s).

## Routes

<a id="r-get-sni-api-v1-kyc-documents-approved"></a>
### GET /sni/api/v1/kyc/documents/approved

- Accès : back-office : rôle d'administration et droit `KYC:APPROVE`
- Réponse : `List<KycDocumentResponse>`
- Modèles : [KycDocumentResponse](#m-KycDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycDocumentReviewController.java`, méthode `approved`

<a id="r-post-sni-api-v1-kyc-documents-bulk-approve"></a>
### POST /sni/api/v1/kyc/documents/bulk-approve

- Accès : back-office : rôle d'administration et droit `KYC:APPROVE`
- En-tête `Idempotency-Key` : conseillé (opération `KYC_DOCUMENT_BULK_APPROVE`, voir le README racine)
- Corps JSON (obligatoire) : `KycBulkApproveRequest`
- Réponse : `KycBulkActionResponse`
- Modèles : [KycBulkActionResponse](#m-KycBulkActionResponse), [KycBulkApproveRequest](#m-KycBulkApproveRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycDocumentReviewController.java`, méthode `bulkApprove`

<a id="r-post-sni-api-v1-kyc-documents-bulk-desapprove"></a>
### POST /sni/api/v1/kyc/documents/bulk-desapprove

- Accès : back-office : rôle d'administration et droit `KYC:REJECT`
- En-tête `Idempotency-Key` : conseillé (opération `KYC_DOCUMENT_BULK_REJECT`, voir le README racine)
- Corps JSON (obligatoire) : `KycBulkRejectRequest`
- Réponse : `KycBulkActionResponse`
- Modèles : [KycBulkActionResponse](#m-KycBulkActionResponse), [KycBulkRejectRequest](#m-KycBulkRejectRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycDocumentReviewController.java`, méthode `bulkReject`

<a id="r-post-sni-api-v1-kyc-documents-bulk-disapprove"></a>
### POST /sni/api/v1/kyc/documents/bulk-disapprove

- Accès : back-office : rôle d'administration et droit `KYC:REJECT`
- En-tête `Idempotency-Key` : conseillé (opération `KYC_DOCUMENT_BULK_REJECT`, voir le README racine)
- Corps JSON (obligatoire) : `KycBulkRejectRequest`
- Réponse : `KycBulkActionResponse`
- Modèles : [KycBulkActionResponse](#m-KycBulkActionResponse), [KycBulkRejectRequest](#m-KycBulkRejectRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycDocumentReviewController.java`, méthode `bulkReject`

<a id="r-post-sni-api-v1-kyc-documents-bulk-reject"></a>
### POST /sni/api/v1/kyc/documents/bulk-reject

- Accès : back-office : rôle d'administration et droit `KYC:REJECT`
- En-tête `Idempotency-Key` : conseillé (opération `KYC_DOCUMENT_BULK_REJECT`, voir le README racine)
- Corps JSON (obligatoire) : `KycBulkRejectRequest`
- Réponse : `KycBulkActionResponse`
- Modèles : [KycBulkActionResponse](#m-KycBulkActionResponse), [KycBulkRejectRequest](#m-KycBulkRejectRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycDocumentReviewController.java`, méthode `bulkReject`

<a id="r-get-sni-api-v1-kyc-documents-desapproved"></a>
### GET /sni/api/v1/kyc/documents/desapproved

- Accès : back-office : rôle d'administration et droit `KYC:READ`
- Réponse : `List<KycDocumentResponse>`
- Modèles : [KycDocumentResponse](#m-KycDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycDocumentReviewController.java`, méthode `rejected`

<a id="r-get-sni-api-v1-kyc-documents-disapproved"></a>
### GET /sni/api/v1/kyc/documents/disapproved

- Accès : back-office : rôle d'administration et droit `KYC:READ`
- Réponse : `List<KycDocumentResponse>`
- Modèles : [KycDocumentResponse](#m-KycDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycDocumentReviewController.java`, méthode `rejected`

<a id="r-get-sni-api-v1-kyc-documents-rejected"></a>
### GET /sni/api/v1/kyc/documents/rejected

- Accès : back-office : rôle d'administration et droit `KYC:REJECT`
- Réponse : `List<KycDocumentResponse>`
- Modèles : [KycDocumentResponse](#m-KycDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycDocumentReviewController.java`, méthode `rejected`

<a id="r-get-sni-api-v1-kyc-documents-review-queue"></a>
### GET /sni/api/v1/kyc/documents/review-queue

- Accès : back-office : rôle d'administration et droit `KYC:READ`
- Réponse : `List<KycDocumentResponse>`
- Modèles : [KycDocumentResponse](#m-KycDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycDocumentReviewController.java`, méthode `reviewQueue`

<a id="r-get-sni-api-v1-kyc-documents-reviewed"></a>
### GET /sni/api/v1/kyc/documents/reviewed

- Accès : back-office : rôle d'administration et droit `KYC:READ`
- Réponse : `List<KycDocumentResponse>`
- Modèles : [KycDocumentResponse](#m-KycDocumentResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycDocumentReviewController.java`, méthode `reviewed`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `status` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-kyc-documents-documentcode-ocr-result"></a>
### GET /sni/api/v1/kyc/documents/{documentCode}/ocr-result

- Accès : back-office : rôle d'administration et droit `KYC:READ`
- Réponse : `KycDocumentOcrResultResponse`
- Modèles : [KycDocumentOcrResultResponse](#m-KycDocumentOcrResultResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/kyc/controller/KycDocumentReviewController.java`, méthode `ocrResult`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentCode` | chemin | `String` | oui |  |

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

<a id="m-KycBulkActionResponse"></a>
#### KycBulkActionResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `processed` | entier |  |  |
| `succeeded` | entier |  |  |
| `failed` | entier |  |  |
| `results` | [KycBulkItemResult](#m-KycBulkItemResult)[] |  |  |

<a id="m-KycBulkApproveRequest"></a>
#### KycBulkApproveRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentIds` | texte[] | oui | éléments min `1` |
| `reviewedBy` | entier int64 ⚠ | oui |  |

<a id="m-KycBulkRejectRequest"></a>
#### KycBulkRejectRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `items` | [KycBulkRejectItemRequest](#m-KycBulkRejectItemRequest)[] | oui | éléments min `1` |
| `reviewedBy` | entier int64 ⚠ | oui |  |

<a id="m-KycDocumentOcrResultResponse"></a>
#### KycDocumentOcrResultResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentCode` | texte |  |  |
| `extractedFirstName` | texte |  |  |
| `extractedLastName` | texte |  |  |
| `extractedDateOfBirth` | date AAAA-MM-JJ |  |  |
| `extractedExpiryDate` | date AAAA-MM-JJ |  |  |
| `extractedDocumentNumber` | texte |  |  |
| `extractedNationality` | texte |  |  |
| `confidenceScore` | nombre décimal |  |  |
| `rawOcrJson` | texte |  |  |
| `processedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-KycBulkItemResult"></a>
#### KycBulkItemResult

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentCode` | texte |  |  |
| `success` | booléen |  |  |
| `error` | texte |  |  |

<a id="m-KycBulkRejectItemRequest"></a>
#### KycBulkRejectItemRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentCode` | texte | oui | longueur min `1` |
| `reason` | texte | oui | longueur min `1` |
