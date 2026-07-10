# GED — Catalogue des types de documents &amp; règles d'exigence

> Back-office uniquement. Ce sont les écrans de configuration qui pilotent les formulaires d'upload (types disponibles) et la liste "documents manquants" (`GET /documents/folder`, voir [ged-documents-core.md](./ged-documents-core.md)).

## Types de documents — `/sni/api/v1/document-types`

Catalogue des types uploadables (ex: "Carte d'identité", "Relevé bancaire").

| Méthode &amp; Route | Rôle | Paramètres clés | Réponse |
|---|---|---|---|
| `POST /document-types` | Créer un type | `code*`, `name*`, `category*`, `ownerType`, `description`, `helpText`, `required`, `requiresExpiryDate`, `requiresIssueDate`, `requiresDocumentNumber`, `requiresReview`, `autoApprove`, `autoApproveAfterDays`, `requiresSignature`, `multipleAllowed`, `requiresBackSide`, `allowedMimeTypes`, `maxFileSizeBytes`, `active` | `DocumentTypeResponse` |
| `PATCH /document-types/{code}` | Modifier un type | mêmes champs | `DocumentTypeResponse` |
| `GET /document-types/{code}` | Détail | — | `DocumentTypeResponse` |
| `GET /document-types` | Liste | query `ownerType`, `active` (optionnels) | `List<DocumentTypeResponse>` |
| `PATCH /document-types/{code}/activate` / `deactivate` | Activer/désactiver | — | `DocumentTypeResponse` |
| `DELETE /document-types/{code}` | Supprimer | — | 204 |
| `GET /document-types/{code}/upload-config` | Contraintes d'upload pour construire un formulaire (dropzone) | — | `DocumentUploadConfigResponse` (helpText, requiresDocumentNumber/IssueDate/ExpiryDate/BackSide, allowedMimeTypes[], maxFileSizeBytes, category) |

**Recommandation frontend** : appeler `GET /document-types/{code}/upload-config` avant d'afficher un formulaire d'upload pour connaître les champs requis et les contraintes de fichier (taille, mime-types), plutôt que de les coder en dur.

Aucune permission spécifique déclarée sur ce contrôleur (authentification admin standard suffit).

## Règles d'exigence — `/sni/api/v1/document-requirements`

Définit quels types de documents sont obligatoires selon le type de propriétaire / type de client / forme juridique. C'est ce qui alimente `missingRequirements[]` dans `GET /documents/folder`.

| Méthode &amp; Route | Rôle | Paramètres clés | Réponse |
|---|---|---|---|
| `POST /document-requirements` | Créer une règle | `ownerType*`, `documentTypeCode*`, `documentTypeName`, `customerType`, `businessLegalForm`, `required`, `active` | `DocumentRequirementResponse` |
| `PATCH /document-requirements/{id}` | Modifier | mêmes champs | `DocumentRequirementResponse` |
| `GET /document-requirements/{id}` | Détail | — | `DocumentRequirementResponse` |
| `GET /document-requirements` | Liste | query `ownerType`, `active` (optionnels) | `List<DocumentRequirementResponse>` |
| `PATCH /document-requirements/{id}/activate` / `deactivate` | Activer/désactiver | — | `DocumentRequirementResponse` |
| `DELETE /document-requirements/{id}` | Supprimer | — | 204 |

Aucune permission spécifique déclarée sur ce contrôleur.

## Enum

- **DocumentCategory** : `KYC, LEGAL, SYSTEM, FINANCIAL, ASSET, OTHER`
