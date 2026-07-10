# KYC — Self-service (portail client)

> Base : `/sni/api/v1/client/documents/kyc` — **seul module de cette documentation destiné au portail client** (authentification membre standard, pas admin). L'identité est résolue depuis le token du membre connecté : aucun `ownerType`/`ownerCode` à fournir, tout est implicitement "mon propre dossier".
>
> Ces routes restent accessibles même avant la fin de l'onboarding/KYC (non bloquées par le garde-fou d'onboarding), pour permettre au membre de justement compléter son KYC.

| Méthode &amp; Route | Rôle | Paramètres clés | Réponse |
|---|---|---|---|
| `GET /client/documents/kyc` | Statut de mon dossier KYC | — | `ClientKycStatusResponse` (caseCode, status, riskLevel, kycLevel, completionPercent, complete, approved, startedAt/submittedAt/completedAt, decisionComment, missingDocumentTypeCodes, requirements, documents) |
| `GET /client/documents/kyc/requirements` | Documents requis pour mon dossier | — | `List<ClientKycRequirementResponse>` (documentTypeCode, documentTypeName, required, requiresBackSide, status, documentCode, backDocumentCode) |
| `GET /client/documents/kyc/completion` | Progression de complétion | — | `ClientKycCompletionResponse` (completionPercent, totalRequired, totalSubmitted, totalVerified, missingDocumentTypeCodes) |
| `GET /client/documents/kyc/documents` | Mes documents uploadés | — | `List<ClientKycDocumentResponse>` |
| `POST /client/documents/kyc/documents` (multipart) | Uploader un document | `file*`, `documentType*`, `documentNumber`, `issueDate`/`expiryDate` (ISO), `side` (défaut FRONT) | `ClientKycDocumentResponse` (201) |
| `GET /client/documents/kyc/documents/{id}` | Détail d'un de mes documents | — | `ClientKycDocumentResponse` |
| `GET /client/documents/kyc/documents/{id}/download` | Télécharger un de mes documents | — | fichier binaire |
| `PUT /client/documents/kyc/documents/{id}` (multipart) | Re-soumettre un document (ex: après correction demandée) | `file*` | `ClientKycDocumentResponse` |
| `DELETE /client/documents/kyc/documents/{id}` | Supprimer un document pas encore revu | — | 204 |
| `POST /client/documents/kyc/submit` | Soumettre mon dossier pour revue (une fois toutes les exigences remplies) | — | `ClientKycStatusResponse` |

⚠️ Les endpoints `POST`/`PUT .../documents` **exigent `multipart/form-data`** — un appel en JSON échoue systématiquement avec une erreur 400 explicite ("requires multipart/form-data").

**`ClientKycDocumentResponse`** : `id, documentCode, documentType, documentTypeName, documentNumber, fileName, fileSize, mimeType, status, requiresBackSide, backDocumentCode, backFileName, backFileSize, backMimeType, issueDate, expiryDate, uploadedAt`. (`status` est une chaîne libre mais correspond 1:1 aux valeurs de `KycDocumentVerificationStatus` — voir [kyc-document-review-admin.md](./kyc-document-review-admin.md).)

Pour le mapping complet des statuts et l'écran de suivi d'onboarding, voir aussi `docs/frontend/client-portal/registration-flow.md` (section Onboarding Status).
