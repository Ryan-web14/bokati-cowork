# GED — Dossiers, espaces, tags & métadonnées

> **Base URL** : `https://api.elleaose.com/sni/api/v1`
> **Back-office uniquement.** Aucune permission spécifique déclarée sur ces contrôleurs.
> Voir le [README](./README.md) pour `ownerType`/`ownerCode` et les enums transverses.

Trois façons complémentaires d'organiser les documents :

- **Dossiers** (`/document-folders`) — arborescence libre créée par l'utilisateur.
- **Espaces** (`/spaces`) — 5 raccourcis fixes (contrats, financier, actifs, administratif, général).
- **Tags & métadonnées** — étiquettes du catalogue + paires clé/valeur libres sur un document.

---

## Partie A — Dossiers (arborescence) · `/document-folders`

### A.1 Le modèle `FolderDetailResponse`

```json
{
  "code": "FOL-202607-00000005",
  "name": "Pièces d'identité",
  "description": "Documents KYC du client",
  "parentCode": null,
  "space": "KYC_SPACE",
  "ownerType": "CUSTOMER",
  "ownerId": 1287,
  "ownerName": "Jean Kabila",
  "path": "/Pièces d'identité",
  "depth": 0,
  "sortOrder": 1,
  "color": "#3B82F6",
  "icon": "folder",
  "childrenCount": 2,
  "documentsCount": 5,
  "createdBy": 42,
  "createdByEmail": "agent@elleaose.com",
  "createdAt": "2026-07-01T09:00:00Z",
  "updatedAt": "2026-07-09T10:00:00Z"
}
```

### A.2 Créer un dossier

```http
POST /document-folders
Content-Type: application/json
```
```json
{
  "name": "Pièces d'identité",
  "description": "Documents KYC du client",
  "parentCode": null,
  "space": "KYC_SPACE",
  "ownerType": "CUSTOMER",
  "ownerCode": "CUS-202606-00000012",
  "color": "#3B82F6",
  "icon": "folder"
}
```

| Champ | Obligatoire | Description |
|---|---|---|
| `name` | ✅ | Nom du dossier. |
| `parentCode` | — | Code du dossier parent ; **nul = dossier racine**. |
| `space` | — | `DocumentSpace` de rattachement. |
| `ownerType` / `ownerCode` | — | Rattache le dossier à un propriétaire. |
| `description` / `color` / `icon` | — | Métadonnées d'affichage. |

**Réponse** `201 Created` → `FolderDetailResponse`.

### A.3 Modifier / déplacer / supprimer

```http
PATCH  /document-folders/{code}          → FolderDetailResponse
```
```json
{ "name": "Identité", "description": "", "color": "#10B981", "icon": "id-card", "sortOrder": 2 }
```
Tous les champs sont optionnels (renommer, restyler, réordonner).

```http
POST   /document-folders/{code}/move     → FolderDetailResponse
```
```json
{ "targetParentCode": "FOL-202607-00000001" }
```
`targetParentCode` nullable → `null` déplace le dossier à la racine.

```http
DELETE /document-folders/{code}          → 204 No Content
```

### A.4 Naviguer dans l'arborescence

```http
GET /document-folders/{code}                 → FolderDetailResponse (détail)
GET /document-folders/roots?space=KYC_SPACE   → List<FolderDetailResponse> (racines ; space optionnel)
GET /document-folders/{code}/children         → List<FolderDetailResponse> (sous-dossiers directs)
GET /document-folders/{code}/tree             → DocumentFolderTreeNode (sous-arbre récursif)
GET /document-folders/{code}/breadcrumb       → List<BreadcrumbItem> (fil d'Ariane)
GET /document-folders/{code}/documents        → PaginatedResponse<DocumentResponse> (paginé)
```

`DocumentFolderTreeNode` (récursif) :
```json
{
  "code": "FOL-...01",
  "name": "Racine",
  "icon": "folder",
  "color": "#3B82F6",
  "depth": 0,
  "documentsCount": 0,
  "children": [
    { "code": "FOL-...05", "name": "Pièces d'identité", "icon": "id-card", "color": "#10B981", "depth": 1, "documentsCount": 5, "children": [] }
  ]
}
```

`BreadcrumbItem[]` (du parent racine jusqu'au dossier courant) :
```json
[
  { "code": "FOL-...01", "name": "Racine", "depth": 0 },
  { "code": "FOL-...05", "name": "Pièces d'identité", "depth": 1 }
]
```

### A.5 Déplacer un document dans un dossier

```http
POST /document-folders/{documentCode}/move-document
Content-Type: application/json
```
```json
{ "folderCode": "FOL-202607-00000005" }
```
⚠️ Le path prend un **`documentCode`** (pas un code de dossier). **Réponse** `200 OK` → `DocumentResponse` mis à jour.

---

## Partie B — Espaces · `/spaces`

Cinq espaces fixes, chacun avec les 3 mêmes opérations : **upload**, **liste**, **dashboard**. Pour éditer/supprimer/télécharger un document précis, passez par [ged-documents-core.md](./ged-documents-core.md) via son `code`.

| Espace | Upload | Liste | Dashboard |
|---|---|---|---|
| Contrats | `POST /spaces/contracts/documents/upload` | `GET /spaces/contracts/documents` | `GET /spaces/contracts/dashboard` |
| Financier | `POST /spaces/financial/documents/upload` | `GET /spaces/financial/documents` | `GET /spaces/financial/dashboard` |
| Actifs | `POST /spaces/assets/documents/upload` | `GET /spaces/assets/documents` | `GET /spaces/assets/dashboard` |
| Administratif | `POST /spaces/administrative/documents/upload` | `GET /spaces/administrative/documents` | `GET /spaces/administrative/dashboard` |
| Général | `POST /spaces/general/documents/upload` | `GET /spaces/general/documents` | `GET /spaces/general/dashboard` |

### B.1 Upload dans un espace (multipart)

```http
POST /spaces/{space}/documents/upload
Content-Type: multipart/form-data
Idempotency-Key: <uuid>
```
Partie `file` + champs `SpaceDocumentUploadRequest` :

| Champ | Obligatoire | Description |
|---|---|---|
| `ownerType` | ✅ | `DocumentOwnerType`. |
| `ownerCode` | ✅ | Code public du propriétaire. |
| `documentTypeCode` | ✅ | Type de document. |
| `title` | ✅ | Titre affiché. |
| `description` | — | Texte libre. |
| `documentNumber` | — | Numéro sur la pièce. |
| `issueDate` / `expiryDate` | — | `yyyy-MM-dd`. |
| `referenceCode` | — | Référence métier libre. |
| `folderCode` | — | Dossier de destination. |
| `tagCodes` | — | Codes de tags. |
| `metadata` | — | Map clé/valeur. |

**Réponse** `201 Created` → `DocumentResponse` (avec `space` renseigné). Ces uploads portent `@Idempotent`.

### B.2 Liste d'un espace

```http
GET /spaces/{space}/documents?ownerType=&ownerCode=&statuses=&q=&page=0&size=20
```
Filtres optionnels : `ownerType`, `ownerCode`, `statuses` (répétable), `q`. **L'espace Financier ajoute** `uploadedAfter`/`uploadedBefore` (`yyyy-MM-dd`).
**Réponse** → `PaginatedResponse<DocumentResponse>` (enveloppe `data`/`pageable`, voir [ged-documents-core.md](./ged-documents-core.md)).

### B.3 Dashboard d'un espace

```http
GET /spaces/{space}/dashboard
```
Aucun paramètre. **Réponse** → `DocumentDashboardResponse` (voir [ged-documents-core.md](./ged-documents-core.md) §10).

> ⚠️ `KYC_SPACE` (6ᵉ valeur de l'enum `DocumentSpace`) n'a **pas** de wrapper ici — les documents KYC passent par le [module KYC dédié](./kyc-cases.md), pas par `/spaces`.

---

## Partie C — Tags & métadonnées

Deux responsabilités sous le même contrôleur : le **catalogue** de tags et leur **assignation** aux documents, plus les **métadonnées** libres.

### C.1 Catalogue de tags · `/document-tags`

```http
POST /document-tags
Content-Type: application/json
```
```json
{ "code": "urgent", "label": "Urgent", "color": "#FF0000", "space": "KYC_SPACE" }
```

| Champ | Règle |
|---|---|
| `code` | obligatoire, `^[a-z0-9-]+$`, max 80 (minuscules, chiffres, tirets). |
| `label` | obligatoire, max 150. |
| `color` | optionnel, hex `^#[0-9A-Fa-f]{6}$` (ex `#3B82F6`). |
| `space` | optionnel, `DocumentSpace`. |

**Réponse** `201 Created` → `DocumentTagResponse` :
```json
{ "id": 7, "code": "urgent", "label": "Urgent", "color": "#FF0000", "space": "KYC_SPACE", "createdBy": 42, "createdAt": "2026-07-01T09:00:00Z" }
```

```http
PUT    /document-tags/{code}              → DocumentTagResponse   (body { label, color })
GET    /document-tags/{code}              → DocumentTagResponse
GET    /document-tags?space=KYC_SPACE     → List<DocumentTagResponse> (space optionnel)
DELETE /document-tags/{code}              → 204 No Content
```

### C.2 Assignation sur un document

```http
POST /documents/{code}/tags
Content-Type: application/json
```
```json
{ "tagCodes": ["urgent", "legal"] }
```
`tagCodes` non vide. **Réponse** `200 OK` → `List<DocumentTagResponse>` (les tags du document après ajout).

```http
DELETE /documents/{code}/tags/{tagCode}   → 204 No Content
GET    /documents/{code}/tags             → List<DocumentTagResponse>
```

### C.3 Métadonnées libres (clé/valeur)

```http
PATCH /documents/{code}/metadata
Content-Type: application/json
```
```json
{ "source": "guichet", "agence": "Brazzaville-Centre" }
```
**Fusionne** les clés fournies avec les métadonnées existantes. **Réponse** `200 OK` → la map complète après fusion :
```json
{ "source": "guichet", "agence": "Brazzaville-Centre" }
```

```http
GET    /documents/{code}/metadata         → Map<String,String>   (toutes les métadonnées)
DELETE /documents/{code}/metadata/{key}   → 204 No Content        (supprime une clé)
```
