# document : types, exigences et rétention

> Partie du [README du module](README.md). Routes exactes : [routes/DocumentTypeController.md](routes/DocumentTypeController.md), [routes/DocumentRequirementController.md](routes/DocumentRequirementController.md), [routes/DocumentRetentionPolicyController.md](routes/DocumentRetentionPolicyController.md).

Droit commun : `SYSTEM:SETTINGS` (règle d'URL). Les politiques de rétention exigent en plus `DOCUMENT:REVIEW` ou `DOCUMENT_REVIEW`, `ROLE_SUPER_ADMIN` compris.

## Types de documents

### Lister et consulter

- `GET /sni/api/v1/document-types?ownerType=MEMBER&active=true` ([route](routes/DocumentTypeController.md#r-get-sni-api-v1-document-types)) : liste triée par nom ; `active=false` ne renvoie que les désactivés, sans `active` tous.
- `GET /sni/api/v1/document-types/{code}` (code insensible à la casse, mis en majuscules).
- Dans les réponses, `description` vaut `helpText` s'il est rempli, sinon la description saisie : l'écran d'édition ne peut pas relire la description d'origine quand une aide existe.

### Créer

`POST /sni/api/v1/document-types` ([route](routes/DocumentTypeController.md#r-post-sni-api-v1-document-types)), `Idempotency-Key` conseillé.

| Champ | Rôle | Défaut |
| --- | --- | --- |
| `code` (obligatoire) | identifiant, mis en majuscules ; `409` « Document type already exists » s'il existe | |
| `name` (obligatoire) | libellé | |
| `category` (obligatoire) | `KYC`, `LEGAL`, `SYSTEM`, `FINANCIAL`, `ASSET`, `OTHER` : fixe l'espace des documents et le rattachement KYC (`KYC` seulement) | |
| `ownerType` | propriétaire visé (filtre de liste, aucun contrôle au dépôt) | aucun |
| `description`, `helpText`, `documentDetails` | textes | |
| `requiresReview` | dépôt en `PENDING_REVIEW` au lieu d'`APPROVED` | `false` (**dépôts approuvés d'office**) |
| `autoApprove` | dépôt en `APPROVED` même si `requiresReview` | `false` |
| `autoApproveAfterDays` | approbation automatique après N jours (workers, voir README) | aucun |
| `requiresExpiryDate` | expiration complétée à émission + 5 ans si absente | `false` |
| `requiresIssueDate`, `requiresDocumentNumber` | imposés au dépôt par le portail membre seulement | `false` |
| `requiresSignature` | crée une demande de signature à l'approbation | `false` |
| `multipleAllowed` | autorise plusieurs documents vivants de ce type par propriétaire ; **obligatoire à `true` pour un type recto/verso** | `false` |
| `requiresBackSide` | pièce KYC avec verso | `false` |
| `allowedMimeTypes` | liste séparée par des virgules, ex. `application/pdf,image/jpeg,image/png` ; seuls ces quatre types et `image/webp` peuvent réellement passer | vide = PDF, JPEG, PNG, WebP |
| `maxFileSizeBytes` | taille maximale, plafonnée de fait à 10 Mo par la configuration du serveur | vide = 10 Mo |
| `required`, `active` | `required` n'est lu nulle part ; `active` masque le type des listes `active=true` mais ne bloque pas le dépôt | `false`, `true` |

### Modifier, activer, désactiver

- `PATCH /sni/api/v1/document-types/{code}` : même corps que la création, et **`code`, `name`, `category` restent obligatoires** (validation du modèle complet). Les champs absents ou `null` sont conservés ; le `code` du corps est ignoré (pas de renommage).
- `PATCH /document-types/{code}/activate`, `/deactivate` ; `DELETE /document-types/{code}` désactive aussi (`204`).
- Changer `requiresReview`, `allowedMimeTypes` ou `requiresBackSide` n'agit que sur les dépôts suivants.

### Configuration de téléversement

`GET /sni/api/v1/document-types/{code}/upload-config` : accessible à tout compte connecté, membres compris. Détail dans [parcours-televersement-et-apercus.md](parcours-televersement-et-apercus.md#préparer-un-formulaire-de-dépôt).

## Exigences documentaires

Elles disent quels types chaque propriétaire doit fournir ; elles pilotent le KYC (pièces manquantes, soumission, approbation, recalcul automatique) et la vue « pièces manquantes » d'un propriétaire.

1. Lister : `GET /sni/api/v1/document-requirements?ownerType=MEMBER&active=true` ([route](routes/DocumentRequirementController.md#r-get-sni-api-v1-document-requirements)).
2. Créer : `POST /sni/api/v1/document-requirements`, `Idempotency-Key` conseillé. Corps : `{ "ownerType": "CUSTOMER", "documentTypeCode": "RCCM", "documentTypeName": "Registre du commerce", "customerType": "COMPANY", "businessLegalForm": null, "required": true, "active": true }`.
   - `documentTypeCode` doit exister (`404` « Document type not found »).
   - `documentTypeName` : libellé affiché dans les check-lists, par défaut le code.
   - `customerType` (pour `CUSTOMER`) et `businessLegalForm` (pour `BUSINESS`) restreignent l'exigence ; mis en majuscules ; vides = tous. Valeurs attendues : celles des modules `client` et `company` (non listées ici).
   - `required` (défaut `true`) : seules les exigences obligatoires bloquent la soumission et l'approbation KYC.
   - Doublon (même propriétaire, type, type de client, forme juridique) : `409` « Document requirement already exists ».
3. Modifier : `PATCH /sni/api/v1/document-requirements/{id}` avec le corps complet (`ownerType` et `documentTypeCode` obligatoires). Le doublon n'y est pas contrôlé.
4. Activer, désactiver : `PATCH /document-requirements/{id}/activate`, `/deactivate` ; `DELETE /document-requirements/{id}` désactive.
5. `{id}` est un int64 ⚠ : le lire et le renvoyer en texte.

Effets à connaître :
- S'il n'existe **aucune** exigence obligatoire active pour un type de propriétaire, le recalcul automatique considère tout dossier KYC de ce type comme approuvé (et active le membre ou le client).
- Ajouter une exigence ne relance pas les dossiers existants : ils ne seront recalculés qu'au prochain dépôt ou à la prochaine revue d'une de leurs pièces.

## Politiques de rétention

`/sni/api/v1/document-retention-policies/**` ([routes](routes/DocumentRetentionPolicyController.md)), droits `SYSTEM:SETTINGS` et `DOCUMENT:REVIEW`.

1. Lister : `GET /document-retention-policies` (actives et inactives). Détail : `GET /document-retention-policies/{code}`.
2. Créer : `POST /document-retention-policies`, corps `{ "code": "FIN-10ANS", "name": "Pièces financières 10 ans", "space": "FINANCIAL_SPACE", "documentTypeCode": null, "retentionDays": 3650, "retentionReference": "UPLOAD_DATE", "action": "ARCHIVE" }`.
   - `code` mis en majuscules, unique (`400` « Retention policy code already exists »).
   - `retentionDays` ≥ 1.
   - `retentionReference` : `UPLOAD_DATE` (date de dépôt, défaut) ou `EXPIRY_DATE` (date d'expiration ; documents sans expiration ignorés).
   - `action` : `ARCHIVE` (défaut) ; `PURGE_FLAG` est accepté mais n'a aucun effet.
   - `space` et `documentTypeCode` facultatifs : sans eux, la politique vise tous les documents. Le code du type est comparé tel quel (sensible à la casse).
3. Modifier : `PUT /document-retention-policies/{code}` avec le corps complet (le `code` du corps est ignoré, l'état actif est conservé).
4. Désactiver : `DELETE /document-retention-policies/{code}` (`204`). Aucune route ne réactive.
5. Application : chaque nuit à 02 h 30, les documents `APPROVED`, `SIGNED` ou `EXPIRED` qui correspondent et ont dépassé le délai passent `ARCHIVED`. Prévenir dans l'écran que l'effet n'est pas immédiat et qu'un document archivé ne compte plus comme valide dans la vue « pièces manquantes » du propriétaire.
