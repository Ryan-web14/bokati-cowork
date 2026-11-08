# document : téléversement, aperçus et versions

> Partie du [README du module](README.md). Routes exactes : [routes/DocumentController.md](routes/DocumentController.md), [routes/DocumentSpaceController.md](routes/DocumentSpaceController.md), [routes/DocumentTypeController.md](routes/DocumentTypeController.md).

## Préparer un formulaire de dépôt

1. Choisir le type : `GET /sni/api/v1/document-types?active=true` (droit `SYSTEM:SETTINGS`), ou, sans ce droit, connaître le code du type et appeler `GET /sni/api/v1/document-types/{code}/upload-config` ([route](routes/DocumentTypeController.md#r-get-sni-api-v1-document-types-code-upload-config), tout compte connecté).
2. `upload-config` renvoie ce qu'il faut pour construire le formulaire :
   - `allowedMimeTypes` : liste des types MIME acceptés (défaut annoncé : `application/pdf`, `image/jpeg`, `image/png`). Mettre dans l'attribut `accept` du champ fichier.
   - `maxFileSizeBytes` : taille maximale (10 485 760 par défaut). Vérifier avant l'envoi.
   - `requiresDocumentNumber`, `requiresIssueDate`, `requiresExpiryDate` : champs à rendre obligatoires.
   - `requiresBackSide` : prévoir un second fichier (verso), uniquement pour les pièces KYC (voir [parcours-kyc.md](parcours-kyc.md#déposer-une-pièce-pour-le-compte-dun-propriétaire)).
   - `helpText` : aide à afficher sous le champ.
3. Côté écran, refuser d'emblée les fichiers qui ne sont ni PDF, ni JPEG, ni PNG, ni WebP, quel que soit `allowedMimeTypes` : le serveur lit la signature binaire du fichier et ne reconnaît que ces quatre formats (une photo HEIC d'iPhone est refusée ; la convertir en JPEG avant l'envoi). L'extension et le `Content-Type` déclarés ne suffisent pas.

## Téléverser un document

`POST /sni/api/v1/documents/upload` ([route](routes/DocumentController.md#r-post-sni-api-v1-documents-upload)), droit `DOCUMENT:UPLOAD`, `multipart/form-data`, en-tête `Idempotency-Key` conseillé.

1. Construire un `FormData` :
   - partie fichier `file` (obligatoire) ;
   - parties texte : `ownerType` (`MEMBER`, `CUSTOMER`, `BUSINESS`, `CONTRACT`, `INVOICE`, `PAYMENT`, `PROPOSAL`, `ASSET`), `ownerCode`, `documentTypeCode`, `title` (obligatoires), `description`, `documentNumber`, `issueDate`, `expiryDate` (facultatifs, dates au format `AAAA-MM-JJ` ; le format exact accepté par la liaison de formulaire n'a pas été vérifié, s'en tenir à celui-ci).
   - Ne pas fixer l'en-tête `Content-Type` à la main : laisser le navigateur ajouter la frontière (`boundary`).
2. `ownerCode` : code membre (`memberId`), code client (`customerId`) ou code entreprise ; pour les autres types de propriétaire, l'identifiant numérique en texte.
3. Le serveur, dans l'ordre : cherche le type (`404` si inconnu, même désactivé il est accepté), résout le propriétaire, complète l'expiration si le type l'exige et qu'elle manque (émission ou aujourd'hui, plus 5 ans), contrôle la taille du type, refuse un second document « vivant » du même type si `multipleAllowed = false`, lit la signature du fichier, calcule le SHA-256, range le fichier, crée la version 1.
4. Réponse `201` : `DocumentResponse` avec `code`, `status` (`PENDING_REVIEW` si le type exige une revue, sinon `APPROVED`), `space`, `currentVersionNumber = 1`, `versions[]`, `previewUrl`, `downloadUrl`.
5. Si la catégorie du type est `KYC` : une pièce KYC est créée et rattachée au dernier dossier KYC du propriétaire (un dossier est ouvert automatiquement s'il n'en a pas), le recalcul KYC part en tâche de fond. Pour un dépôt recto/verso, utiliser plutôt la route KYC dédiée.
6. Ensuite, proposer : ouvrir la fiche (`GET /sni/api/v1/documents/{code}`), ajouter des étiquettes, ranger dans un dossier.

Erreurs fréquentes : `400` « Only one document of type 'X' is allowed per owner », `400` « Unsupported or unrecognized file content », `400` « Unsupported document MIME type: … », `400` « File size (…) exceeds… », `400` « Owner code must be numeric for owner type CONTRACT ».

## Téléverser dans un espace

`POST /sni/api/v1/spaces/{contracts|financial|assets|administrative|general}/documents/upload` ([routes](routes/DocumentSpaceController.md)), droit `ADMIN:ACCESS`, `multipart/form-data`, `Idempotency-Key` conseillé.

1. Mêmes parties que le dépôt simple, plus : `referenceCode` (référence métier libre, rendue dans `spaceReferenceCode`), `folderCode` (dossier de classement), `tagCodes` (une partie par code d'étiquette), `metadata` (non vérifié : la liaison d'une `Map` depuis des parties de formulaire suit la syntaxe `metadata[cle]=valeur` de Spring ; préférer `PATCH /documents/{code}/metadata` après le dépôt).
2. Contrôles propres à l'espace (`DocumentSpaceService.java`) :
   - catégorie du type : `contracts` → `LEGAL` ; `financial` → `FINANCIAL` ; `assets` → `ASSET` ; `administrative` → `SYSTEM` ou `OTHER` ; `general` → toutes. Sinon `400` « Document type X (category: Y) is not allowed in space Z ». Ici le code du type est cherché **tel quel** (sensible à la casse) : `400` « Unknown document type » si la casse diffère.
   - `Content-Type` **déclaré** par le navigateur pour `contracts`, `financial`, `assets` : doit figurer dans la liste de l'espace, sinon `400` « File type … is not allowed in space … ». Le contrôle par signature s'applique ensuite (voir README, écart sur les formats).
3. Le document est créé comme au dépôt simple, puis l'espace est forcé, le dossier, les étiquettes et les métadonnées appliqués. Un `folderCode` inconnu donne `400` « Folder not found: X » (le document est alors annulé avec la transaction).
4. Réponse `201` : `DocumentResponse`.

Pas de route d'espace pour `KYC_SPACE` : les pièces KYC passent par `/documents/upload` ou `/kyc/documents/admin/upload`.

## Téléverser un lot ou une archive ZIP

### Lot de fichiers

`POST /sni/api/v1/documents/upload-batch` ([route](routes/DocumentController.md#r-post-sni-api-v1-documents-upload-batch)), droit `DOCUMENT:UPLOAD`.

1. Parties : `files` répétée (une par fichier, 20 au plus), `ownerType`, `ownerCode`, `documentTypeCode` (obligatoires), `space`, `folderCode`, `referenceCode`, `tagCodes` (facultatifs). Le total ne doit pas dépasser 10 Mo.
2. Chaque fichier devient un document distinct ; son titre est le nom du fichier sans extension, `_` remplacés par des espaces. Un `folderCode` inconnu est ignoré sans erreur.
3. Réponse `201` `DocumentBulkActionResponse` : `total`, `succeeded`, `failed`, `results[]` avec `code`, `status` (`UPLOADED` ou `FAILED`) et `reason` (nom du fichier, suivi du message d'erreur en cas d'échec). Afficher le détail par fichier.
4. Avec un type `multipleAllowed = false`, seul le premier fichier passe ; les suivants échouent.

### Archive ZIP

`POST /sni/api/v1/documents/import-zip` ([route](routes/DocumentController.md#r-post-sni-api-v1-documents-import-zip)), droit `DOCUMENT:UPLOAD`.

1. La partie fichier s'appelle **`file`** (le code lit `@RequestPart("file")` ; le fichier généré l'affiche sous le nom de variable `zipFile`). Autres parties : comme le lot.
2. 50 fichiers au plus ; au-delà, une ligne d'erreur et l'arrêt. Chaque répertoire de l'archive devient un dossier de classement (créé sous `folderCode` s'il est fourni, dans l'espace `space` ou `GENERIC`) ; un dossier de même nom existant est réutilisé.
3. Le type MIME de chaque fichier est deviné d'après son nom ; la signature binaire reste contrôlée.
4. Réponse `201` `ZipImportResponse` : `totalFiles`, `succeeded`, `failed`, `foldersCreated`, `items[]` (`originalPath`, `documentCode`, `folderCode`, `success`, `error`).

## Remplacer le fichier d'un document

`POST /sni/api/v1/documents/{code}/replace` ([route](routes/DocumentController.md#r-post-sni-api-v1-documents-code-replace)), droit `DOCUMENT:UPLOAD`, partie `file` seule, `Idempotency-Key` conseillé.

1. Le fichier est contrôlé contre les formats du type (pas contre sa taille maximale).
2. Une nouvelle version devient courante ; le statut repasse à `PENDING_REVIEW` si le type exige une revue, sinon `APPROVED`. Les pièces KYC liées repassent `PENDING`.
3. Aucun contrôle de statut ni de verrou : un document archivé, expiré ou refusé est remis en circuit. Proposer ce bouton surtout pour `NEEDS_CORRECTION`, `REJECTED` et `EXPIRED`.
4. Réponse `200` `DocumentResponse` à jour.

## Consulter et restaurer les versions

1. `GET /sni/api/v1/documents/{code}/versions` ([route](routes/DocumentController.md#r-get-sni-api-v1-documents-code-versions)) ou le champ `versions[]` de `GET /documents/{code}` : de la plus récente à la plus ancienne, avec `versionNumber`, `originalFileName`, `mimeTypeDetected`, `fileSizeBytes`, `checksumSha256`, `uploadedBy`, `uploadedAt`, `current`. `storagePath` vaut toujours `null`.
2. Les `previewUrl`/`downloadUrl` d'une version pointent vers le fichier **courant** : ne pas les proposer comme « ouvrir cette version ».
3. Restaurer : `POST /sni/api/v1/documents/{code}/versions/{versionNumber}/restore` ([route](routes/DocumentController.md#r-post-sni-api-v1-documents-code-versions-versionnumber-restore)), droit `DOCUMENT:REVIEW`. Crée une **nouvelle** version (numéro suivant) qui reprend le fichier de la version visée ; le statut du document ne change pas. `404` « Version N not found for document X ».

## Afficher un aperçu dans le navigateur

Trois routes servent les octets du fichier courant, avec `Content-Type` = type MIME détecté au dépôt et `Content-Disposition` portant le nom d'origine :

| Route | Jeton | `Content-Disposition` | Usage |
| --- | --- | --- | --- |
| `GET /sni/api/v1/documents/{code}/preview` ([route](routes/DocumentController.md#r-get-sni-api-v1-documents-code-preview)) | Bearer, `DOCUMENT:READ` | `inline` | aperçu par `fetch` |
| `GET /sni/api/v1/documents/{code}/download` ([route](routes/DocumentController.md#r-get-sni-api-v1-documents-code-download)) | Bearer, `DOCUMENT:READ` | `attachment` | téléchargement |
| `GET /sni/api/v1/documents/{code}/signed-preview?token=…` ([route](routes/DocumentController.md#r-get-sni-api-v1-documents-code-signed-preview)) | aucun, jeton signé dans l'URL | `inline` | `<img src>`, nouvel onglet |

Chaque appel des trois routes est inscrit au journal d'accès (`PREVIEW` ou `DOWNLOAD`, adresse IP, navigateur).

### Méthode A : `fetch` puis URL `blob:` (recommandée, marche pour PDF et images)

1. `fetch('/sni/api/v1/documents/{code}/preview', { headers: { Authorization: 'Bearer …' } })`.
2. `const blob = await res.blob(); const url = URL.createObjectURL(blob);`
3. Image (`mimeType` commence par `image/`) : `<img src={url}>`. PDF : `<iframe src={url}>` ou `<embed>` ; l'URL `blob:` appartient à l'origine du frontend, l'en-tête `X-Frame-Options: DENY` de l'API ne la bloque pas.
4. Libérer avec `URL.revokeObjectURL(url)` en quittant l'écran.

### Méthode B : jeton d'aperçu signé (images en liste, ouverture dans un onglet)

1. `GET /sni/api/v1/documents/{code}/preview-token` ([route](routes/DocumentController.md#r-get-sni-api-v1-documents-code-preview-token)), droit `DOCUMENT:READ`. Réponse : `{ "token": "…", "signedPreviewUrl": "/sni/api/v1/documents/{code}/signed-preview?token=…", "expiresAt": "2026-11-08T10:20:30Z" }`.
2. `signedPreviewUrl` est **relative** : la préfixer par l'origine de l'API.
3. Le jeton vaut pour ce seul document, pendant 300 s par défaut (`DOCUMENTS_PREVIEW_TOKEN_TTL_SECONDS`), et peut resservir pendant ce délai. En redemander un avant `expiresAt` si l'écran reste ouvert.
4. Utilisable dans `<img src>` ou `window.open(url)` (navigation de premier niveau, le PDF s'ouvre dans la visionneuse du navigateur). **Pas dans un `<iframe>`** : `X-Frame-Options: DENY`.
5. Jeton absent, falsifié, expiré ou d'un autre document : `403` « A valid access token is required to preview this document ». Si le serveur est configuré avec `DOCUMENTS_PUBLIC_PREVIEW_ENABLED=false`, la route exige le Bearer comme les autres (`401` sans lui) : prévoir le repli sur la méthode A.

### Télécharger

`fetch` avec Bearer sur `/download`, lire le nom dans `Content-Disposition` (en-tête exposé par CORS), puis `blob` et lien `<a download>`. Un document sans fichier : `400` « Document has no file stored ».

### URL reçues dans les réponses

`previewUrl`, `downloadUrl` (et `fileUrl`, identique à `previewUrl`) sont relatives sauf si le serveur définit `app.api-base-url`. Elles pointent vers `/preview` et `/download`, donc exigent le Bearer : ne jamais les mettre telles quelles dans `src`. Même chose pour `previewUrl`, `downloadUrl`, `backPreviewUrl`, `backDownloadUrl` des pièces KYC.

## Verrouiller un document pendant une modification

1. `POST /sni/api/v1/documents/{code}/lock` ([route](routes/DocumentController.md#r-post-sni-api-v1-documents-code-lock)), droit `DOCUMENT:UPLOAD`, sans corps : verrou d'une heure au nom de l'utilisateur. `400` « Document is locked by another user » si un autre utilisateur détient un verrou non expiré.
2. `POST /sni/api/v1/documents/{code}/unlock` : par le détenteur, ou par un compte `ROLE_ADMIN`/`ROLE_SUPER_ADMIN`, sinon `400` « Only the user who locked the document or an admin can unlock it ». Sans verrou : `200` sans effet.
3. Réponses vides `200`. L'état du verrou n'est lisible nulle part et n'empêche aucune autre action : ne pas construire d'interface qui en dépende.

## Archiver un document

`POST /sni/api/v1/documents/{code}/archive?reason=…` ([route](routes/DocumentController.md#r-post-sni-api-v1-documents-code-archive)), droit `DOCUMENT:REVIEW`. `reason` est accepté mais non conservé. `400` « Document is already archived ». L'archivage ne prévient pas le KYC : la pièce KYC liée garde son statut de vérification.
