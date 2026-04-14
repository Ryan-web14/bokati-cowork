# Document Type Et Requirement CRUD

Ce document decrit les endpoints back-office pour gerer les types de documents et les exigences documentaires KYC/legal.

Base URL: `/sni/api/v1`

## Principes

- `document-type` definit la nature d'un document: piece d'identite, RCCM, NIU, contrat signe, annexe, etc.
- `document-requirement` indique qu'un type de document est requis ou optionnel pour un owner donne: `MEMBER`, `CUSTOMER`, `BUSINESS`, `CONTRACT`, etc.
- Les suppressions sont logiques: elles mettent `active=false` pour eviter de casser les documents deja rattaches.
- `Idempotency-Key` est supporte mais non bloquant sur les actions simples d'activation/desactivation/suppression.

## Champs Document Type

| Champ | Type | Description |
| --- | --- | --- |
| `code` | string | Code unique, normalise en majuscules. Exemple: `MEMBER_NATIONAL_ID`. |
| `name` | string | Nom lisible affiche dans le back-office/front. |
| `category` | enum | `KYC`, `LEGAL`, `SYSTEM`, `FINANCIAL`, `ASSET`, `OTHER`. |
| `ownerType` | enum/null | Type de proprietaire cible. Peut etre `MEMBER`, `CUSTOMER`, `BUSINESS`, `CONTRACT`, etc. |
| `description` | string | Dans la reponse API, ce champ retourne le texte d'aide utilisateur (`helpText`) en priorite pour faciliter l'affichage frontend. |
| `helpText` | string | Texte d'aide court pour guider l'utilisateur au moment de l'upload. |
| `documentDetails` | string | Detail precis du document demande: informations attendues, contenu minimal, points de verification. |
| `required` | boolean | Indique si le type est generalement obligatoire. Les regles fines restent dans `document-requirement`. |
| `requiresExpiryDate` | boolean | Force la saisie d'une date d'expiration a l'upload. |
| `requiresReview` | boolean | Force une revue admin/compliance avant validation. |
| `requiresSignature` | boolean | Indique qu'une signature est attendue. |
| `multipleAllowed` | boolean | Autorise plusieurs documents du meme type pour le meme owner. |
| `allowedMimeTypes` | string | Liste CSV. Exemple: `application/pdf,image/jpeg,image/png`. |
| `maxFileSizeBytes` | number | Taille maximale autorisee. |
| `active` | boolean | Type actif/inactif. |

## Document Type Endpoints

### Creer

`POST /document-types`

```json
{
  "code": "MEMBER_DRIVER_LICENSE",
  "name": "Permis de conduire membre",
  "category": "KYC",
  "ownerType": "MEMBER",
  "description": "Piece complementaire d'identification membre.",
  "helpText": "Importer une copie lisible du permis de conduire recto-verso.",
  "documentDetails": "Le permis doit afficher le nom, le numero, la photo et la date de validite.",
  "required": false,
  "requiresExpiryDate": true,
  "requiresReview": true,
  "requiresSignature": false,
  "multipleAllowed": false,
  "allowedMimeTypes": "application/pdf,image/jpeg,image/png",
  "maxFileSizeBytes": 10485760,
  "active": true
}
```

### Lire Un Type

`GET /document-types/{code}`

Exemple:

`GET /document-types/MEMBER_NATIONAL_ID`

### Lister

`GET /document-types`

Filtres:

| Param | Description |
| --- | --- |
| `ownerType` | Filtre par owner type. Exemple: `MEMBER`. |
| `active` | `true`, `false` ou absent pour tout afficher. |

Exemples:

`GET /document-types?ownerType=MEMBER&active=true`

`GET /document-types?active=false`

### Modifier

`PATCH /document-types/{code}`

Le body reprend les champs du create. Le `code` de l'URL identifie la ligne; le code du body n'est pas utilise pour renommer.

### Activer / Desactiver

`PATCH /document-types/{code}/activate`

`PATCH /document-types/{code}/deactivate`

### Supprimer Logiquement

`DELETE /document-types/{code}`

Effet: `active=false`.

## Champs Document Requirement

| Champ | Type | Description |
| --- | --- | --- |
| `id` | number | Identifiant technique. |
| `ownerType` | enum | Owner concerne: `MEMBER`, `CUSTOMER`, `BUSINESS`, etc. |
| `documentTypeCode` | string | Code du document type existant. |
| `documentTypeName` | string | Libelle copie pour affichage rapide. |
| `customerType` | string/null | Scope customer optionnel: `PERSON` ou `COMPANY`. |
| `businessLegalForm` | string/null | Scope business optionnel si une forme juridique est ciblee. |
| `required` | boolean | Obligatoire ou optionnel. |
| `active` | boolean | Regle active/inactive. |

## Document Requirement Endpoints

### Creer

`POST /document-requirements`

```json
{
  "ownerType": "CUSTOMER",
  "documentTypeCode": "CUSTOMER_COMPANY_RCCM",
  "documentTypeName": "RCCM client entreprise",
  "customerType": "COMPANY",
  "businessLegalForm": null,
  "required": true,
  "active": true
}
```

Le backend verifie que `documentTypeCode` existe.

### Lire Une Regle

`GET /document-requirements/{id}`

### Lister

`GET /document-requirements`

Filtres:

| Param | Description |
| --- | --- |
| `ownerType` | Filtre par owner type. |
| `active` | `true`, `false` ou absent pour tout afficher. |

Exemples:

`GET /document-requirements?ownerType=CUSTOMER&active=true`

`GET /document-requirements?active=false`

### Modifier

`PATCH /document-requirements/{id}`

```json
{
  "ownerType": "MEMBER",
  "documentTypeCode": "MEMBER_NATIONAL_ID",
  "documentTypeName": "Carte nationale d'identite membre",
  "customerType": null,
  "businessLegalForm": null,
  "required": true,
  "active": true
}
```

### Activer / Desactiver

`PATCH /document-requirements/{id}/activate`

`PATCH /document-requirements/{id}/deactivate`

### Supprimer Logiquement

`DELETE /document-requirements/{id}`

Effet: `active=false`.

## Recommandations Frontend

- Afficher `description` dans les listes et formulaires courts: il correspond au texte d'aide utilisateur.
- Afficher `documentDetails` dans une zone detaillee ou une aide contextuelle avant l'upload.
- Garder `helpText` si l'ecran a besoin explicitement du champ technique source.
- Ne pas supprimer physiquement dans l'UI: utiliser une action "Desactiver".
- Pour KYC, charger les requirements actifs par `ownerType`, puis charger les document types correspondants pour afficher `helpText`, MIME autorises et taille max.
