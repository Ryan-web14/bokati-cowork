# GED — Dossiers, espaces, tags &amp; métadonnées

> Back-office uniquement. Voir [README](./README.md) pour `ownerType`/`ownerCode` et la recherche client/membre/entreprise (utile pour pré-remplir un dossier ou un upload par espace).

## Dossiers (arborescence) — `/sni/api/v1/document-folders`

Organisation hiérarchique des documents, indépendante des "espaces" ci-dessous.

| Méthode &amp; Route | Rôle | Paramètres clés | Réponse |
|---|---|---|---|
| `POST /document-folders` | Créer un dossier | `name*`, `description`, `parentCode` (nul = racine), `space`, `ownerType`, `ownerCode`, `color`, `icon` | `FolderDetailResponse` |
| `GET /document-folders/{code}` | Détail | — | `FolderDetailResponse` (path, depth, sortOrder, childrenCount, documentsCount, ownerName) |
| `PATCH /document-folders/{code}` | Renommer/styliser/réordonner | `name`, `description`, `color`, `icon`, `sortOrder` | `FolderDetailResponse` |
| `DELETE /document-folders/{code}` | Supprimer | — | 204 |
| `POST /document-folders/{code}/move` | Déplacer sous un nouveau parent (ou racine) | `targetParentCode` (nullable) | `FolderDetailResponse` |
| `GET /document-folders/roots` | Dossiers racine | query `space` (optionnel) | `List<FolderDetailResponse>` |
| `GET /document-folders/{code}/children` | Sous-dossiers directs | — | `List<FolderDetailResponse>` |
| `GET /document-folders/{code}/tree` | Sous-arbre complet (récursif) | — | `DocumentFolderTreeNode` (children[] récursif) |
| `GET /document-folders/{code}/documents` | Documents du dossier, paginé | `Pageable` | `PaginatedResponse<DocumentResponse>` |
| `GET /document-folders/{code}/breadcrumb` | Fil d'Ariane | — | `List<BreadcrumbItem>` (code, name, depth) |
| `POST /document-folders/{documentCode}/move-document` | Déplacer un document dans un dossier | `folderCode` | `DocumentResponse` |

Aucune permission spécifique déclarée.

## Espaces — `/sni/api/v1/spaces`

Raccourcis d'upload/liste/dashboard pour 5 espaces fixes (upload + liste + dashboard, même forme pour chacun). Pour éditer/supprimer/télécharger un document précis, utiliser [ged-documents-core.md](./ged-documents-core.md) via son `code`.

| Espace | Upload | Liste | Dashboard |
|---|---|---|---|
| Contrats | `POST /spaces/contracts/documents/upload` | `GET /spaces/contracts/documents` | `GET /spaces/contracts/dashboard` |
| Financier | `POST /spaces/financial/documents/upload` | `GET /spaces/financial/documents` | `GET /spaces/financial/dashboard` |
| Actifs | `POST /spaces/assets/documents/upload` | `GET /spaces/assets/documents` | `GET /spaces/assets/dashboard` |
| Administratif | `POST /spaces/administrative/documents/upload` | `GET /spaces/administrative/documents` | `GET /spaces/administrative/dashboard` |
| Général | `POST /spaces/general/documents/upload` | `GET /spaces/general/documents` | `GET /spaces/general/dashboard` |

- **Upload** (multipart, part `file`) : body `ownerType*`, `ownerCode*`, `documentTypeCode*`, `title*`, `description`, `documentNumber`, `issueDate`, `expiryDate`, `referenceCode`, `folderCode`, `tagCodes[]`, `metadata` → `DocumentResponse` (201).
- **Liste** : query `ownerType`, `ownerCode`, `statuses[]`, `q`, `Pageable` (l'espace Financier ajoute aussi `uploadedAfter`/`uploadedBefore`) → `PaginatedResponse<DocumentResponse>`.
- **Dashboard** : aucun paramètre → `DocumentDashboardResponse`.

⚠️ `KYC_SPACE` (6ᵉ valeur de l'enum `DocumentSpace`) n'a **pas** de wrapper ici — les documents KYC passent par le [module KYC dédié](./kyc-cases.md), pas par `/spaces`.

## Tags &amp; métadonnées

Deux responsabilités sous le même contrôleur : le catalogue de tags, et leur assignation aux documents.

**Catalogue** :
| Méthode &amp; Route | Rôle | Paramètres clés |
|---|---|---|
| `POST /document-tags` | Créer un tag | `code*` (`^[a-z0-9-]+$`, max 80), `label*` (max 150), `color` (`^#[0-9A-Fa-f]{6}$`), `space` (optionnel) |
| `PUT /document-tags/{code}` | Modifier | `label`, `color` |
| `GET /document-tags/{code}` | Détail | — |
| `GET /document-tags` | Liste | query `space` (optionnel) |
| `DELETE /document-tags/{code}` | Supprimer | — |

**Assignation sur un document** :
| Méthode &amp; Route | Rôle |
|---|---|
| `POST /documents/{code}/tags` | Assigner un ou plusieurs tags (`tagCodes*` non vide) |
| `DELETE /documents/{code}/tags/{tagCode}` | Retirer un tag |
| `GET /documents/{code}/tags` | Lister les tags d'un document |

**Métadonnées libres (clé/valeur)** :
| Méthode &amp; Route | Rôle |
|---|---|
| `PATCH /documents/{code}/metadata` | Fusionner des métadonnées (body `Map<String,String>`) |
| `GET /documents/{code}/metadata` | Lire toutes les métadonnées |
| `DELETE /documents/{code}/metadata/{key}` | Supprimer une clé |

Aucune permission spécifique déclarée sur ces routes.
