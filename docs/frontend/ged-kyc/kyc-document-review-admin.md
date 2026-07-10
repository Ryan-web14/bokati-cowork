# KYC — Revue de documents &amp; upload admin

## Revue de documents (globale, tous dossiers confondus) — `/sni/api/v1/kyc/documents`

| Méthode &amp; Route | Rôle | Paramètres clés | Réponse | 🔒 |
|---|---|---|---|---|
| `POST /kyc/documents/bulk-approve` | Approuver des documents en masse | `documentIds*` (liste de codes), `reviewedBy*` | `KycBulkActionResponse` | `KYC:APPROVE` |
| `POST /kyc/documents/bulk-reject` (alias `/bulk-disapprove`, `/bulk-desapprove`) | Rejeter des documents en masse, **motif par document** | `items*` (liste `{documentCode, reason}`), `reviewedBy*` | `KycBulkActionResponse` | `KYC:REJECT` |
| `GET /kyc/documents/review-queue` | Tous les documents en attente de revue (global) | — | `List<KycDocumentResponse>` | `KYC:READ` |
| `GET /kyc/documents/reviewed` | Documents déjà traités, filtrables | query `status` (accepte APPROVED/VERIFIED ou REJECTED/DISAPPROVED, insensible à la casse ; défaut = les deux) | `List<KycDocumentResponse>` | `KYC:READ` |
| `GET /kyc/documents/approved` | Raccourci — uniquement les documents vérifiés | — | `List<KycDocumentResponse>` | `KYC:READ` |
| `GET /kyc/documents/rejected` (alias `/disapproved`, `/desapproved`) | Raccourci — uniquement les documents rejetés | — | `List<KycDocumentResponse>` | `KYC:READ` |
| `GET /kyc/documents/{documentCode}/ocr-result` | Résultat de l'extraction OCR | — | `KycDocumentOcrResultResponse` | `KYC:READ` |

**`KycDocumentOcrResultResponse`** : `documentCode, extractedFirstName, extractedLastName, extractedDateOfBirth, extractedExpiryDate, extractedDocumentNumber, extractedNationality, confidenceScore, rawOcrJson, processedAt`.

- `confidenceScore` : score global de confiance de l'extraction (0–1).
- `rawOcrJson` : payload brut renvoyé par le moteur OCR (JSON sérialisé en chaîne), utile pour du debug/affichage avancé mais pas destiné à un affichage direct utilisateur — préférer les champs `extracted*` structurés pour l'UI standard.

## Upload par un admin pour le compte d'un client — `/sni/api/v1/kyc/documents/admin`

Utile quand un document est reçu hors ligne (guichet, email) et doit être saisi par le staff.

| Méthode &amp; Route | Rôle | Paramètres clés | Réponse | 🔒 |
|---|---|---|---|---|
| `POST /kyc/documents/admin/upload` (multipart) | Upload d'un document KYC pour un propriétaire donné | `ownerType*` (MEMBER/CUSTOMER/BUSINESS), `ownerCode*`, `documentTypeCode*` (ex: CNI, PASSEPORT, NIU), `side` (FRONT défaut / BACK), `documentNumber`, `issueDate`/`expiryDate` (yyyy-MM-dd) + part `file*` | `KycDocumentResponse` (201) | `KYC:READ` |
| `PATCH /kyc/documents/admin/{kycDocumentId}/details` | Compléter les métadonnées d'un document déjà uploadé | `documentNumber`, `issueDate`, `expiryDate` (tous optionnels) | `KycDocumentResponse` | `KYC:READ` |

**Auto-remplissage `ownerCode`** : `GET /customers/search/basic?query=`, `GET /members/search/basic?query=`, `GET /businesses/search/basic?query=` — voir [README](./README.md#endpoints-de-recherche-pour-lauto-remplissage).

## Enum

- **KycDocumentVerificationStatus** : `PENDING, VERIFIED, REJECTED, EXPIRED`

⚠️ Note d'implémentation : `KycDocumentReviewController` (`/kyc/documents/**`) et `KycDocumentAdminController` (`/kyc/documents/admin/**`) sont soumis aux permissions `KYC:*` (pas `DOCUMENT:*`), car le préfixe `/kyc` est résolu avant `/documents` côté back-office.
