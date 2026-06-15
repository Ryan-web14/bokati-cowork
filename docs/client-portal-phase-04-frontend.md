# Portail Client — Phase 4 : Guide Frontend — KYC & Contrats

Tous les endpoints requièrent `Authorization: Bearer <accessToken>` sauf mention contraire.

---

## Dossier KYC — `/client/documents/kyc`

### GET `/client/documents/kyc`

Retourne le dossier KYC complet du membre : statut, documents soumis, liste des pièces requises.

**Réponse — `200 OK` :**
```json
{
  "caseCode": "KYCCASE-20260614-0001",
  "status": "IN_PROGRESS",
  "riskLevel": "LOW",
  "kycLevel": 1,
  "completionPercent": 33,
  "complete": false,
  "approved": false,
  "startedAt": "2026-06-14T09:00:00Z",
  "submittedAt": null,
  "completedAt": null,
  "decisionComment": null,
  "missingDocumentTypeCodes": ["PROOF_OF_ADDRESS", "PHOTO"],
  "requirements": [
    { "documentTypeCode": "NATIONAL_ID", "documentTypeName": "Carte Nationale d'Identité", "required": true, "status": "VERIFIED", "documentCode": "DOC-2026-0001" },
    { "documentTypeCode": "PROOF_OF_ADDRESS", "documentTypeName": "Justificatif de domicile", "required": true, "status": null, "documentCode": null },
    { "documentTypeCode": "PHOTO", "documentTypeName": "Photo d'identité", "required": true, "status": null, "documentCode": null }
  ],
  "documents": [
    { "id": 1, "documentCode": "DOC-2026-0001", "documentType": "NATIONAL_ID", "documentNumber": "CG-123456", "fileName": "doc_20260614.jpg", "fileSize": 250000, "mimeType": "image/jpeg", "status": "VERIFIED", "issueDate": "2020-01-01", "expiryDate": "2030-01-01", "uploadedAt": "2026-06-14T09:05:00Z" }
  ]
}
```

**Valeurs de `status` et messages à afficher :**
| Statut | Message UI |
|---|---|
| `NOT_STARTED` | "Votre dossier KYC n'a pas encore été démarré" |
| `IN_PROGRESS` | "Complétez votre dossier KYC" |
| `SUBMITTED` | "Dossier soumis — en attente d'examen" |
| `UNDER_REVIEW` | "Votre dossier est en cours d'examen" |
| `APPROVED` | "KYC approuvé ✓" |
| `REJECTED` | "Dossier refusé — voir commentaire" |
| `PENDING_CORRECTION` | "Correction requise — voir détails" |

---

### GET `/client/documents/kyc/requirements`

Retourne la liste des types de documents requis avec leur statut de soumission actuel.

**Réponse — `200 OK` :** Tableau d'objets `requirement` (même structure que le champ `requirements` de la réponse du dossier).

---

### GET `/client/documents/kyc/completion`

Retourne un résumé allégé de complétion (sans la liste des documents).

**Réponse — `200 OK` :**
```json
{
  "caseCode": "KYCCASE-20260614-0001",
  "caseStatus": "IN_PROGRESS",
  "completionPercent": 33,
  "totalRequired": 3,
  "totalSubmitted": 1,
  "totalVerified": 1,
  "missingDocumentTypeCodes": ["PROOF_OF_ADDRESS", "PHOTO"],
  "requirements": [...]
}
```

Utiliser cet endpoint pour afficher la barre de progression KYC sans charger toutes les données de documents.

---

## Documents KYC

### GET `/client/documents/kyc/documents`

Liste tous les documents soumis dans le dossier KYC en cours.

**Réponse — `200 OK` :** Tableau de `ClientKycDocumentResponse`.

---

### POST `/client/documents/kyc/documents`

Téléverse un nouveau document KYC. **Content-Type : `multipart/form-data`**

**Champs du formulaire :**
| Champ | Requis | Description |
|---|---|---|
| `file` | Oui | Fichier à uploader (PDF, JPEG, PNG — max 10 Mo) |
| `documentType` | Oui | Code du type de document (voir tableau ci-dessous) |
| `documentNumber` | Non | Numéro du document |
| `issueDate` | Non | Date d'émission (ISO : `2020-01-15`) |
| `expiryDate` | Non | Date d'expiration (ISO : `2030-01-15`) |

**Codes `documentType` acceptés :**
| Code | Libellé |
|---|---|
| `NATIONAL_ID` | Carte nationale d'identité |
| `PASSPORT` | Passeport |
| `RESIDENCE_PERMIT` | Titre de séjour |
| `PROOF_OF_ADDRESS` | Justificatif de domicile |
| `COMPANY_REGISTRATION` | Registre de commerce |
| `TAX_CERTIFICATE` | Attestation fiscale |
| `PHOTO` | Photo d'identité |
| `BANK_STATEMENT` | Relevé bancaire |
| `OTHER` | Autre document |

**Réponse — `201 Created` :** `ClientKycDocumentResponse` avec `status: "PENDING"`.

**Erreurs :**
| Statut | Signification |
|---|---|
| `400` | Fichier manquant, trop volumineux (>10 Mo) ou type non supporté |
| `400` | Dossier KYC non ouvert pour les uploads (SUBMITTED/UNDER_REVIEW/APPROVED) |
| `404` | Aucun dossier KYC trouvé |

**Exemple d'upload frontend :**
```javascript
const formData = new FormData();
formData.append('file', fileInput.files[0]);
formData.append('documentType', 'NATIONAL_ID');
formData.append('documentNumber', 'CG-123456');
formData.append('issueDate', '2020-01-15');
formData.append('expiryDate', '2030-01-15');

await fetch('/sni/api/v1/client/documents/kyc/documents', {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${accessToken}` },
  body: formData
});
```

---

### GET `/client/documents/kyc/documents/{id}`

Retourne les détails et le statut de vérification d'un document spécifique.

**Réponse — `200 OK` :** `ClientKycDocumentResponse`.

**Valeurs de `status` :**
| Statut | Signification |
|---|---|
| `PENDING` | En attente d'examen |
| `VERIFIED` | Approuvé par l'admin |
| `REJECTED` | Refusé — une nouvelle soumission est requise |
| `EXPIRED` | Document expiré |

---

### GET `/client/documents/kyc/documents/{id}/download`

Télécharge le fichier du document. Retourne le contenu binaire avec les en-têtes `Content-Type` et `Content-Disposition` appropriés.

---

### PUT `/client/documents/kyc/documents/{id}`

Resoumet un document (remplace le fichier). Autorisé uniquement si le statut actuel est `PENDING` ou `REJECTED`. **Content-Type : `multipart/form-data`**

**Champs du formulaire :**
| Champ | Requis |
|---|---|
| `file` | Oui |

**Réponse — `200 OK` :** `ClientKycDocumentResponse` mis à jour avec `status: "PENDING"`.

---

### DELETE `/client/documents/kyc/documents/{id}`

Supprime un document. Autorisé uniquement si `status = PENDING` (pas encore examiné).

**Réponse — `204 No Content`**

---

### POST `/client/documents/kyc/submit`

Soumet le dossier KYC complet pour examen par l'admin. Autorisé uniquement si le statut du dossier est `IN_PROGRESS` ou `PENDING_CORRECTION`.

**Réponse — `200 OK` :** `ClientKycStatusResponse` mis à jour avec `status: "SUBMITTED"`.

**Erreurs :**
| Statut | Signification |
|---|---|
| `400` | Dossier pas dans un état soumissible |
| `404` | Aucun dossier KYC trouvé |

---

## Contrats — `/client/documents/contracts`

### GET `/client/documents/contracts`

Retourne les contrats du membre, paginés.

**Paramètres de requête :** `page`, `size`, `sort` (défaut : `createdAt,desc`)

**Réponse — `200 OK` :**
```json
{
  "content": [
    {
      "contractCode": "CONTRACT-20260601-0001",
      "title": "Contrat Abonnement Coworking",
      "status": "ACTIVE",
      "startDate": "2026-06-01",
      "endDate": "2027-06-01",
      "signedAt": "2026-06-01T10:00:00Z",
      "hasPdf": true
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "page": 0,
  "size": 20
}
```

---

### GET `/client/documents/contracts/{contractCode}`

Retourne les détails complets d'un contrat.

**Réponse — `200 OK` :**
```json
{
  "contractCode": "CONTRACT-20260601-0001",
  "title": "Contrat Abonnement Coworking",
  "status": "AWAITING_SIGNATURE",
  "renewalType": "ANNUAL",
  "startDate": "2026-06-01",
  "endDate": "2027-06-01",
  "signedAt": null,
  "draftDocumentCode": "DOC-CONTRACT-2026-0001",
  "signedDocumentCode": null,
  "canSign": true,
  "canDownloadPdf": true,
  "createdAt": "2026-05-30T09:00:00Z"
}
```

Champs clés pour la logique UI :
- `canSign: true` → afficher le bouton "Signer"
- `canDownloadPdf: true` → afficher le bouton "Télécharger PDF"
- `status = "ACTIVE"` → afficher le badge actif

---

### GET `/client/documents/contracts/{contractCode}/pdf`

Télécharge le contrat en PDF (brouillon ou signé, selon disponibilité).

Retourne le PDF binaire avec les en-têtes :
```
Content-Type: application/pdf
Content-Disposition: attachment; filename="..."
```

---

### POST `/client/documents/contracts/{contractCode}/sign`

Signe le contrat électroniquement. Autorisé uniquement quand `canSign: true`.

**Corps de la requête :**
```json
{
  "accepted": true,
  "signatureData": null
}
```

`accepted` doit être `true`. `signatureData` est optionnel (ex. : signature dessinée encodée en base64).

**Réponse — `200 OK` :** `ClientContractResponse` mis à jour avec `status: "SIGNED"`.

**Erreurs :**
| Statut | Signification |
|---|---|
| `400` | Contrat non disponible à la signature |
| `400` | Document de contrat pas encore généré |
| `404` | Contrat introuvable |

**Flux UX :**
1. Afficher l'aperçu PDF du contrat (chargé via l'endpoint `/pdf`)
2. Afficher une case à cocher d'acceptation des conditions
3. À la confirmation → POST vers `/sign` avec `{accepted: true}`
4. En cas de succès → afficher "Contrat signé ✓" et rafraîchir les détails du contrat

---

## Sécurité des opérations sur les documents

- Les opérations sur les documents vérifient que le document appartient au dossier KYC du membre authentifié.
- Les opérations sur les contrats vérifient que `ownerType = MEMBER` et `ownerCode = memberId`.
- Toute tentative d'accès aux documents d'un autre membre retourne `404` (et non `403`) pour éviter la fuite d'informations.

---

## Référence des codes d'erreur

| Code | HTTP | Déclencheur |
|---|---|---|
| `FILE_TOO_LARGE` | `400` | Upload supérieur à 10 Mo |
| `UNSUPPORTED_FILE_TYPE` | `400` | Format non supporté (ni PDF, ni JPEG, ni PNG) |
| `KYC_CASE_NOT_EDITABLE` | `400` | Uploads bloqués en statut SUBMITTED/UNDER_REVIEW/APPROVED |
| `DOCUMENT_NOT_DELETABLE` | `400` | Document pas en statut PENDING |
| `DOCUMENT_NOT_RESUBMITTABLE` | `400` | Document VERIFIED ou EXPIRED |
| `KYC_NOT_SUBMITTABLE` | `400` | Dossier pas en statut IN_PROGRESS ou PENDING_CORRECTION |
| `CONTRACT_NOT_SIGNABLE` | `400` | Contrat pas en statut GENERATED ou AWAITING_SIGNATURE |
