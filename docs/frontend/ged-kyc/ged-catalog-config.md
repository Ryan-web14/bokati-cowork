# GED — Catalogue des types de documents & règles d'exigence

> **Base URL** : `https://api.elleaose.com/sni/api/v1`
> **Back-office uniquement.** Aucune permission spécifique n'est déclarée sur ces contrôleurs : l'authentification admin standard suffit.

Ces deux écrans de configuration pilotent le reste de la GED :

- **Types de documents** → alimentent les listes déroulantes des formulaires d'upload et définissent les contraintes (champs requis, mime-types, taille).
- **Règles d'exigence** → alimentent `missingRequirements[]` de `GET /documents/folder` (voir [ged-documents-core.md](./ged-documents-core.md)).

---

## Partie A — Types de documents · `/document-types`

Le catalogue des types uploadables (ex : « Carte nationale d'identité », « Relevé bancaire »).

### A.1 Créer un type

```http
POST /document-types
Content-Type: application/json
```
```json
{
  "code": "CNI",
  "name": "Carte nationale d'identité",
  "category": "KYC",
  "ownerType": "CUSTOMER",
  "description": "Pièce d'identité officielle",
  "helpText": "Scannez le recto et le verso, lisibles et non tronqués.",
  "documentDetails": "Recto + verso obligatoires",
  "required": true,
  "requiresExpiryDate": true,
  "requiresIssueDate": true,
  "requiresDocumentNumber": true,
  "requiresReview": true,
  "autoApprove": false,
  "autoApproveAfterDays": null,
  "requiresSignature": false,
  "multipleAllowed": false,
  "requiresBackSide": true,
  "allowedMimeTypes": "image/jpeg,image/png,application/pdf",
  "maxFileSizeBytes": 5242880,
  "active": true
}
```

| Champ | Obligatoire | Description |
|---|---|---|
| `code` | ✅ | Identifiant unique du type (utilisé partout comme `documentTypeCode`). |
| `name` | ✅ | Libellé affiché. |
| `category` | ✅ | `DocumentCategory` : `KYC, LEGAL, SYSTEM, FINANCIAL, ASSET, OTHER`. |
| `ownerType` | — | Restreint le type à un `DocumentOwnerType` (nul = tous). |
| `description` / `helpText` / `documentDetails` | — | Textes d'aide affichables dans le formulaire. |
| `required` | — | Le type est-il globalement requis. |
| `requiresIssueDate` / `requiresExpiryDate` / `requiresDocumentNumber` | — | Rendent ces champs obligatoires à l'upload. |
| `requiresBackSide` | — | Impose un verso (recto + verso). |
| `requiresReview` | — | `false` = pas de revue manuelle nécessaire. |
| `autoApprove` + `autoApproveAfterDays` | — | Auto-approbation après N jours sans décision. |
| `requiresSignature` | — | Le document doit être signé électroniquement. |
| `multipleAllowed` | — | Autorise plusieurs exemplaires par propriétaire. |
| `allowedMimeTypes` | — | Liste de mime-types séparés par des virgules. |
| `maxFileSizeBytes` | — | Taille max en octets. |
| `active` | — | Type actif/visible dans les formulaires. |

**Réponse** `200 OK` → `DocumentTypeResponse` (mêmes champs que la requête, sans wrapper).

### A.2 Modifier / activer / supprimer

```http
PATCH  /document-types/{code}              → DocumentTypeResponse   (mêmes champs, tous optionnels)
PATCH  /document-types/{code}/activate     → DocumentTypeResponse   (active = true)
PATCH  /document-types/{code}/deactivate   → DocumentTypeResponse   (active = false)
DELETE /document-types/{code}              → 204 No Content
```

### A.3 Lire

```http
GET /document-types/{code}                 → DocumentTypeResponse
GET /document-types?ownerType=CUSTOMER&active=true   → List<DocumentTypeResponse>
```
`ownerType` et `active` sont des filtres optionnels. La liste renvoie un **tableau brut** (pas d'enveloppe paginée).

### A.4 Config d'upload — à appeler avant d'afficher un formulaire

```http
GET /document-types/{code}/upload-config
```
**Réponse** `200 OK` → `DocumentUploadConfigResponse` :

```json
{
  "documentTypeCode": "CNI",
  "documentTypeName": "Carte nationale d'identité",
  "helpText": "Scannez le recto et le verso, lisibles et non tronqués.",
  "requiresDocumentNumber": true,
  "requiresIssueDate": true,
  "requiresExpiryDate": true,
  "requiresBackSide": true,
  "allowedMimeTypes": ["image/jpeg", "image/png", "application/pdf"],
  "maxFileSizeBytes": 5242880,
  "category": "KYC"
}
```

> **Recommandation frontend** : appelez cette route avant d'afficher un formulaire d'upload (dropzone) pour connaître les champs requis et les contraintes de fichier (taille, mime-types autorisés) au lieu de les coder en dur. Ici `allowedMimeTypes` est un **tableau** déjà découpé (alors qu'il est stocké en chaîne côté type de document).

---

## Partie B — Règles d'exigence · `/document-requirements`

Définissent quels types de documents sont **obligatoires** selon le type de propriétaire, le type de client et/ou la forme juridique. C'est cette configuration qui remplit `missingRequirements[]` dans `GET /documents/folder`.

### B.1 Créer une règle

```http
POST /document-requirements
Content-Type: application/json
```
```json
{
  "ownerType": "BUSINESS",
  "documentTypeCode": "RCCM",
  "documentTypeName": "Registre du commerce",
  "customerType": null,
  "businessLegalForm": "SARL",
  "required": true,
  "active": true
}
```

| Champ | Obligatoire | Description |
|---|---|---|
| `ownerType` | ✅ | `DocumentOwnerType` concerné. |
| `documentTypeCode` | ✅ | Type de document rendu obligatoire. |
| `documentTypeName` | — | Libellé (facilite l'affichage sans jointure). |
| `customerType` | — | Restreint la règle à un type de client (nul = tous). |
| `businessLegalForm` | — | Restreint à une forme juridique (ex `SARL`, `SA`). |
| `required` | — | `true` = document obligatoire. |
| `active` | — | Règle active. |

**Réponse** `200 OK` → `DocumentRequirementResponse` :

```json
{
  "id": 15,
  "ownerType": "BUSINESS",
  "documentTypeCode": "RCCM",
  "documentTypeName": "Registre du commerce",
  "customerType": null,
  "businessLegalForm": "SARL",
  "required": true,
  "active": true
}
```

### B.2 Modifier / activer / supprimer / lire

```http
PATCH  /document-requirements/{id}              → DocumentRequirementResponse   (mêmes champs)
PATCH  /document-requirements/{id}/activate     → DocumentRequirementResponse
PATCH  /document-requirements/{id}/deactivate   → DocumentRequirementResponse
DELETE /document-requirements/{id}              → 204 No Content
GET    /document-requirements/{id}              → DocumentRequirementResponse
GET    /document-requirements?ownerType=BUSINESS&active=true   → List<DocumentRequirementResponse>
```
La liste est un **tableau brut** ; `ownerType` et `active` sont des filtres optionnels.

---

## Enum

- **DocumentCategory** : `KYC, LEGAL, SYSTEM, FINANCIAL, ASSET, OTHER`
- **DocumentOwnerType** : `CUSTOMER, MEMBER, BUSINESS, CONTRACT, INVOICE, PAYMENT, PROPOSAL, ASSET`