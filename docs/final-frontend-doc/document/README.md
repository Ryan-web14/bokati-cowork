# document : fonctionnement

> Rédigé d'après le code. Routes exactes, droits et champs JSON : [routes.md](routes.md) et `routes/`. Règles communes : [README racine](../README.md).

## En bref

- Gestion documentaire (GED) du back-office : dépôt de fichiers rattachés à un propriétaire (membre, client, entreprise, contrat, facture…), versions, revue (approuver, refuser, demander une correction), classement (dossiers, étiquettes, métadonnées), partages, signatures, circuits d'approbation, rétention et tableaux de bord.
- KYC (vérification d'identité) : dossiers KYC par propriétaire, pièces recto et verso, revue pièce par pièce, décision sur le dossier, notes, OCR, règles de contrôle croisé, relances et expirations automatiques.
- Public visé : back-office uniquement. Le portail membre dépose ses pièces KYC par ses propres routes (`/sni/api/v1/client/documents/kyc/**` et l'alias `/sni/api/v1/portal/kyc/**`, voir [portal/routes.md](../portal/routes.md)), qui réutilisent les services de ce module : les pièces déposées par un membre arrivent dans les files de revue décrites ici.
- Modules liés : `client` (statut du membre et du client changé par le KYC), `company` (entreprises propriétaires), `contract` (un refus de pièce annule le contrat lié, ses abonnements et pass), `payment` (portefeuille créé à l'approbation KYC), `socle` (file d'événements et courriels).

## Table des matières

| Fichier | Contenu |
| --- | --- |
| [parcours-televersement-et-apercus.md](parcours-televersement-et-apercus.md) | Téléverser (simple, par espace, par lot, ZIP), formats et tailles, remplacer, versions, afficher un aperçu dans le navigateur, télécharger, verrouiller |
| [parcours-revue.md](parcours-revue.md) | Files de revue, approbation, refus et correction (unitaire et en masse), droits par espace, signatures, circuits d'approbation |
| [parcours-kyc.md](parcours-kyc.md) | Dossiers KYC : création, dépôt recto et verso par l'administration, revue des pièces, soumission, approbation, refus, affectation, notes, chronologie, OCR, contrôles croisés, export PDF |
| [parcours-classement-et-partage.md](parcours-classement-et-partage.md) | Recherche, tableaux de bord, vue par propriétaire, statistiques, export CSV, dossiers (arborescence), étiquettes, métadonnées, permissions, liens de partage, journal d'accès |
| [parcours-configuration.md](parcours-configuration.md) | Types de documents et configuration de téléversement, exigences documentaires, politiques de rétention |

## Notions

| Objet | Identifiant exposé | Ce que c'est | Liens |
| --- | --- | --- | --- |
| Document | `code` (séquence `DOCUMENT`, texte) | Un fichier rattaché à un propriétaire, avec un statut de revue. Toutes les routes `/documents/{code}` l'utilisent. | un type, un propriétaire, un espace, un dossier (facultatif), des versions, des étiquettes, des métadonnées |
| Version | `versionNumber` (1, 2, 3…) | Chaque dépôt ou remplacement crée une version ; une seule est `current`. | appartient à un document |
| Type de document | `code` (texte en majuscules, ex. `CNI`) | Gabarit : catégorie, formats acceptés, taille maximale, champs obligatoires, revue, recto/verso. | utilisé par les documents, les exigences, les dossiers KYC |
| Catégorie | énumération `DocumentCategory` | `KYC`, `LEGAL`, `SYSTEM`, `FINANCIAL`, `ASSET`, `OTHER`. Portée par le type, recopiée sur le document. | détermine l'espace et le rattachement KYC |
| Espace | énumération `DocumentSpace` | `KYC_SPACE`, `CONTRACT_SPACE`, `FINANCIAL_SPACE`, `ASSET_SPACE`, `ADMINISTRATIVE`, `GENERIC`. Rangement logique et périmètre des droits de revue par espace. | voir « Espace d'un document » ci-dessous |
| Propriétaire | `ownerType` + `ownerCode` à l'envoi ; `ownerType` + `ownerId` (int64) en réponse | `MEMBER` (code membre `memberId`), `CUSTOMER` (code client `customerId`), `BUSINESS` (code entreprise) ; pour `CONTRACT`, `INVOICE`, `PAYMENT`, `PROPOSAL`, `ASSET`, `ownerCode` doit être l'identifiant numérique en texte. | `DocumentResponse` ne renvoie pas le code du propriétaire, seulement `ownerId` |
| Exigence documentaire | `id` (int64 ⚠) | « Tel type de propriétaire doit fournir tel type de document », éventuellement filtré par type de client ou forme juridique. | alimente le KYC et la vue « pièces manquantes » |
| Dossier KYC | `code` (séquence `KYCCASE`) | Le dossier de vérification d'un membre, client ou entreprise. Un seul dossier « vivant » par propriétaire. | des pièces KYC, des notes, des exigences propres au dossier |
| Pièce KYC (`KycDocument`) | `documentCode` (code du document recto) ; `id` numérique (voir piège plus bas) | Lien entre un dossier KYC et un document recto, plus un document verso facultatif (`backDocumentCode`). Son statut de vérification suit celui du document. | un document recto, un document verso |
| Dossier de classement | `code` (séquence `FOLDER`) | Arborescence libre (10 niveaux au plus) dans un espace. | contient des documents |
| Étiquette | `code` (minuscules, chiffres, tirets) | Libellé coloré, global ou propre à un espace. | assignée aux documents |
| Métadonnée | clé texte | Paires clé/valeur libres sur un document. | |
| Lien de partage | `token` (32 caractères hexadécimaux) | Lien à durée limitée, éventuellement protégé par mot de passe, vers un document ou un dossier. | |
| Demande de signature | `id` (int64 ⚠) | Signature attendue d'un signataire sur un document. | |
| Circuit d'approbation | `code` (séquence `WORKFLOW`) ; instance : `id` (int64) | Suite d'étapes d'approbation que l'on lance sur un document. | |
| Politique de rétention | `code` (majuscules) | Archive automatiquement les documents anciens d'un espace ou d'un type. | |
| Règle de contrôle croisé KYC | `id` (int64 ⚠) | Compare un champ lu par OCR entre deux types de pièces avant l'approbation d'un dossier. | |

### Espace d'un document

Au dépôt par `POST /sni/api/v1/documents/upload`, l'espace est déduit (`DocumentServiceImpl.java:637`) : catégorie `KYC` → `KYC_SPACE` ; propriétaire `CONTRACT` → `CONTRACT_SPACE` ; propriétaire `INVOICE` ou `PAYMENT` → `FINANCIAL_SPACE` ; propriétaire `ASSET` → `ASSET_SPACE` ; catégorie `LEGAL` ou `SYSTEM` → `ADMINISTRATIVE` ; sinon `GENERIC`. Les routes `/sni/api/v1/spaces/<espace>/documents/upload` et le lot (`space`) imposent l'espace à la place.

## Statuts et cycle de vie

### Document (`DocumentStatus`)

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `PENDING_REVIEW` | En attente de revue. Figure dans les files `/review`. | dépôt d'un type avec `requiresReview = true` et `autoApprove = false` ; tout remplacement (`/replace`) d'un type avec `requiresReview = true` | `APPROVED`, `REJECTED`, `NEEDS_CORRECTION`, `EXPIRED` (date dépassée), `ARCHIVED` |
| `APPROVED` | Validé. | dépôt d'un type sans revue ou avec `autoApprove = true` ; `POST .../approve` ; approbation en masse ; approbation automatique (workers) | `EXPIRED`, `ARCHIVED`, ou de nouveau `PENDING_REVIEW` par remplacement |
| `NEEDS_CORRECTION` | Le déposant doit fournir un nouveau fichier. `correctionNote`, `correctionDeadline` et `correctionCount` sont tenus côté serveur mais **non renvoyés** par `DocumentResponse`. | `POST .../request-correction` (unitaire, en masse, ou par le dossier KYC) | `APPROVED`, `REJECTED`, `PENDING_REVIEW` (remplacement) |
| `REJECTED` | Refusé. Exclu de `GET /sni/api/v1/documents` (mais pas de `/search`). Un refus déclenche des annulations en cascade (voir [parcours-revue.md](parcours-revue.md#refus-et-annulations-en-cascade)). | `POST .../reject` | `APPROVED` (revenir sur un refus est permis), `NEEDS_CORRECTION`, `PENDING_REVIEW` (remplacement) |
| `EXPIRED` | Date d'expiration dépassée. | worker horaire (statuts `APPROVED`, `SIGNED`, `PENDING_REVIEW` dont `expiryDate` < aujourd'hui) ; worker KYC du lundi (après la période de grâce) | `PENDING_REVIEW` ou `APPROVED` par remplacement ; `ARCHIVED` |
| `ARCHIVED` | Archivé. | `POST .../archive` ; worker de rétention (02 h 30) | `PENDING_REVIEW` ou `APPROVED` par remplacement (aucun contrôle de statut sur `/replace`) |
| `DRAFT`, `UPLOADED`, `SIGNED`, `SUPERSEDED` | Déclarés mais jamais attribués par le code. | aucun chemin | |

Une revue n'est possible que depuis `PENDING_REVIEW`, `UPLOADED` ou `NEEDS_CORRECTION`, et depuis `REJECTED` pour approuver ou demander une correction (`DocumentServiceImpl.java:363`). Ailleurs : `400`.

### Version (`DocumentVersionUploadStatus`, `DocumentAntivirusStatus`)

Toujours `READY` et `CLEAN` en pratique : un fichier suspect est refusé avant d'être enregistré. Les autres valeurs (`PENDING_SCAN`, `QUARANTINED`, `INFECTED`…) ne sont jamais posées.

### Revue (`DocumentReviewStatus`)

Chaque décision crée une ligne d'historique `APPROVED`, `REJECTED` ou `NEEDS_CORRECTION` (aucune route ne la lit directement ; elle alimente tableaux de bord et statistiques).

### Pièce KYC (`KycDocumentVerificationStatus`)

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `PENDING` | À vérifier. Figure dans `/kyc/documents/review-queue`. | dépôt ; remplacement ; demande de correction unitaire (`/documents/{code}/request-correction`) | `VERIFIED`, `REJECTED`, `EXPIRED` |
| `VERIFIED` | Document approuvé (statut document `APPROVED` ou `SIGNED`). | approbation du document | `EXPIRED`, `PENDING` (remplacement) |
| `REJECTED` | Document refusé ou archivé ; aussi après une demande de correction passée par le dossier KYC (`/kyc/cases/{code}/documents/request-correction`). | refus, correction via le dossier ; un document `ARCHIVED` est traduit en `REJECTED` seulement si une synchronisation KYC le traite ensuite (l'archivage seul ne la déclenche pas) | `PENDING` (nouveau dépôt), `VERIFIED` |
| `EXPIRED` | Pièce expirée. | worker KYC du lundi (expiration + 15 jours de grâce) ; worker horaire des documents | `PENDING` (nouveau dépôt) |

### Dossier KYC (`KycCaseStatus`)

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `NOT_STARTED` | Valeur par défaut technique, jamais conservée : tout dossier est créé en `IN_PROGRESS`. | | |
| `IN_PROGRESS` | Pièces exigées encore manquantes. | création (`POST /kyc/cases` ou automatique au premier dépôt KYC) ; recalcul automatique | `UNDER_REVIEW`, `SUBMITTED`, `APPROVED`, `PENDING_CORRECTION` |
| `SUBMITTED` | Soumis par l'administration, en attente d'un réviseur. Membres et entreprises seulement (un client passe directement à `UNDER_REVIEW`). | `POST /kyc/cases/{code}/submit` | `UNDER_REVIEW` (affectation ou recalcul), `APPROVED`, `PENDING_CORRECTION` |
| `UNDER_REVIEW` | Toutes les pièces exigées sont présentes, revue en cours. | recalcul automatique ; affectation d'un dossier `SUBMITTED` ; soumission d'un client | `APPROVED`, `PENDING_CORRECTION` |
| `APPROVED` | Dossier validé. Le membre ou client passe `ACTIVE`. | `POST /kyc/cases/{code}/approve` ; **recalcul automatique quand toutes les pièces exigées sont `VERIFIED`** ; aucune exigence active pour ce type de propriétaire | `RENEWAL_REQUIRED` (pièce expirée), `PENDING_CORRECTION`, `IN_PROGRESS`/`UNDER_REVIEW` (recalcul après un nouveau dépôt) |
| `PENDING_CORRECTION` | Des pièces sont refusées ou à refaire. | `POST /kyc/cases/{code}/reject` ; recalcul automatique dès qu'une pièce est `REJECTED` ; expiration d'une pièce d'un dossier non approuvé | `UNDER_REVIEW`, `APPROVED`, `IN_PROGRESS` |
| `RENEWAL_REQUIRED` | Dossier approuvé dont une pièce a expiré. | worker KYC du lundi | recalcul au prochain dépôt |
| `EXPIRED` | Dossier clos (membre ou client archivé). Un nouveau dépôt ouvre un nouveau dossier. | synchronisation depuis le statut du propriétaire `ARCHIVED` | aucun |
| `REJECTED` | Jamais attribué : « refuser » un dossier le met en `PENDING_CORRECTION`. | aucun chemin | |

Le recalcul automatique (`KycAutomationServiceImpl.recomputeCaseState`) tourne en tâche asynchrone après chaque dépôt, remplacement, revue, expiration ou approbation automatique d'un document de catégorie `KYC`. Il ne tient compte que des exigences **globales** (pas de celles propres au dossier) et répercute le statut sur le membre (`PENDING`, `UNDER_REVIEW`, `PENDING_CORRECTION`, `ACTIVE`) ou le client (`PENDING`, `ACTIVE`).

### Autres objets à statut

- Signature (`DocumentSignatureStatus`) : `PENDING` → `SIGNED` (`PATCH .../sign`) ou `DECLINED` (refus du document). `EXPIRED` jamais attribué.
- Instance de circuit (`status` texte) : `IN_PROGRESS` → `APPROVED` (dernière étape approuvée) ou `REJECTED`. `PENDING` jamais attribué.
- Lien de partage : `active` vrai jusqu'à révocation ; inutilisable après `expiresAt` ou `maxAccessCount`.
- Type de document, exigence, politique de rétention, circuit : `active` ; la route `DELETE` désactive, elle ne supprime pas.

## Parcours

Les parcours détaillés sont dans les fichiers listés en [table des matières](#table-des-matières). Résumé des écrans :

- Téléversement, aperçu, versions : [parcours-televersement-et-apercus.md](parcours-televersement-et-apercus.md).
- Revue documentaire et circuits : [parcours-revue.md](parcours-revue.md).
- KYC : [parcours-kyc.md](parcours-kyc.md).
- Recherche, classement, partage, journaux : [parcours-classement-et-partage.md](parcours-classement-et-partage.md).
- Paramétrage (types, exigences, rétention) : [parcours-configuration.md](parcours-configuration.md).

## Règles et erreurs à refléter dans l'écran

### Fichiers

- Taille : **10 Mo** par requête au plus (`spring.servlet.multipart.max-request-size`), tous fichiers confondus pour un lot. Au-delà, l'erreur n'a pas de traitement dédié : elle tombe dans le gestionnaire générique (`500`, « An unexpected error occurred… ») (non vérifié à l'exécution). Vérifier la taille **avant** l'envoi.
- Taille par type : `maxFileSizeBytes` du type. Dépassement : `400` « File size (N KB) exceeds the maximum allowed size of M KB for this document type ».
- Format : le serveur lit les premiers octets du fichier. Seuls **PDF, JPEG, PNG et WebP** sont reconnus (`DocumentSecurityService.java:68`). Tout autre contenu (HEIC des iPhone, GIF, TIFF, DOCX, XLSX, CSV…) : `400` « Unsupported or unrecognized file content ». Un format reconnu mais absent des `allowedMimeTypes` du type : `400` « Unsupported document MIME type: image/png ». Sans `allowedMimeTypes` sur le type, la liste par défaut est PDF, JPEG, PNG, WebP.
- Fichier vide ou absent : `400` « Document file is required ».

### Dépôt

- Un seul document « vivant » par propriétaire et par type si le type a `multipleAllowed = false` (statuts autres que `REJECTED`, `ARCHIVED`, `SUPERSEDED`, `EXPIRED`) : `400` « Only one document of type 'X' is allowed per owner ». **Ce contrôle vise aussi le verso d'une pièce KYC et le redépôt d'un recto** : un type à recto et verso doit être configuré avec `multipleAllowed = true`.
- Propriétaire introuvable : `404` venant du module client ou entreprise. `ownerCode` non numérique pour `CONTRACT`, `INVOICE`, `PAYMENT`, `PROPOSAL`, `ASSET` : `400` « Owner code must be numeric for owner type X ».
- Type inconnu : `404` « Document type not found ». Un type **désactivé** reste accepté au dépôt (aucun contrôle).
- Type avec `requiresExpiryDate` et aucune date fournie : le serveur fixe l'expiration à la date d'émission (ou aujourd'hui) plus 5 ans (`app.document.default-expiry-years`), sans erreur. `requiresIssueDate` et `requiresDocumentNumber` ne sont pas contrôlés sur les routes de ce module (seulement journalisés pour le dépôt KYC par l'administration) ; le portail membre, lui, les impose.

### Revue

- `reviewedBy` (int64) est **obligatoire** dans le corps de toutes les décisions (`approve`, `reject`, `request-correction`, masse, KYC) et n'est pas contrôlé : le serveur enregistre la valeur reçue. Voir [À prévoir](#à-prévoir-côté-frontend).
- Décision sur un document `APPROVED`, `EXPIRED`, `ARCHIVED`… : `400` « Document DOC… · Une pièce APPROVED ne se revoit plus · le client doit en déposer une nouvelle. » ; refus d'une pièce déjà refusée : `400` « … elle peut être approuvée ou renvoyée en correction, pas refusée une seconde fois. »
- Demande de correction : `correctionNote` obligatoire, `deadlineDays` ≥ 1 (7 par défaut).

### KYC

- Soumettre un dossier incomplet : `400` « KYC case is incomplete. Missing required documents: CNI, CNI_BACK » (le suffixe `_BACK` signale un verso manquant).
- Approuver : `400` « KYC case cannot be approved while mandatory documents are missing », « … while documents are not verified », ou « KYC cross-validation failed for field X ».
- Refuser un dossier : `comment` obligatoire, sinon `400` « A rejection reason is required ».
- Verso avant recto : `400` « Le recto doit être uploadé avant le verso pour le type : X ».
- Dépôt par l'administration sans dossier KYC : `400` « Aucun dossier KYC actif pour ce propriétaire · créez-en un d'abord ».
- Filtre `status` inconnu sur `/kyc/cases` : `400` « Unsupported KYC case status: X ».

### Autres

- Énumération invalide dans un chemin ou un paramètre (ex. `/review/kyc_space` en minuscules) : aucune conversion tolérante, l'erreur de conversion n'a pas de traitement dédié (probablement `500`, non vérifié). Envoyer les valeurs exactes en majuscules.
- Messages : la plupart sont en anglais (`Document not found`, `Folder not found: X`…). Prévoir une traduction côté écran par `errorCode` et statut plutôt que d'afficher `message` tel quel.

## À prévoir côté frontend

### Écrans

1. GED : liste et recherche, fiche document (versions, étiquettes, métadonnées, partages, signatures, circuit, journal d'accès), dépôt, dépôt en lot ou ZIP.
2. Espaces : un onglet par espace (`contracts`, `financial`, `assets`, `administrative`, `general`) avec son tableau de bord, sa liste et son dépôt. Il n'y a pas de route d'espace pour le KYC : utiliser les écrans KYC.
3. Revue : file globale et files par espace, actions unitaires et en masse.
4. Arborescence de dossiers, étiquettes, permissions.
5. KYC : tableau de bord, liste des dossiers, « ma file », fiche dossier (exigences, pièces recto/verso avec aperçu, notes, chronologie, échéances, OCR), file de revue des pièces, règles de contrôle croisé.
6. Paramétrage : types de documents, exigences, politiques de rétention, circuits d'approbation.

### Droits pour masquer les boutons

| Action | Droit exigé (règle d'URL, plus `@PreAuthorize` quand indiqué) |
| --- | --- |
| Lister, voir, aperçu, télécharger, jeton d'aperçu, recherche, tableau de bord, vue propriétaire | `DOCUMENT:READ` |
| Déposer, remplacer, lot, ZIP, verrouiller, partager, assigner des étiquettes, demander une signature, lancer un circuit | `DOCUMENT:UPLOAD` (tout `POST` sous `/documents` sans mot-clé de revue) |
| Approuver, refuser, correction, restaurer une version, archiver, statistiques, export CSV, journaux d'accès, approuver ou refuser une étape de circuit | `DOCUMENT:REVIEW` |
| Files `/review/**` | `DOCUMENT:REVIEW` (voir écart sur `DOCUMENT:REVIEW_<ESPACE>`) |
| Espaces, dossiers, étiquettes (catalogue), permissions, circuits, liens de dossier | `ADMIN:ACCESS` (règle par défaut, aucune règle dédiée) |
| Types, exigences | `SYSTEM:SETTINGS` |
| Politiques de rétention | `SYSTEM:SETTINGS` **et** `DOCUMENT:REVIEW` (ou `DOCUMENT_REVIEW`) |
| KYC : lecture et toutes les actions sans `/approve` ni `/reject` dans le chemin (création, soumission, affectation, notes, exigences, dépôt, approbation et refus **en masse** des pièces, correction, contrôles croisés) | `KYC:READ` |
| KYC : approuver un dossier ; lister `/kyc/documents/approved` | `KYC:APPROVE` |
| KYC : refuser un dossier ; lister `/kyc/documents/rejected` | `KYC:REJECT` |
| Configuration de téléversement d'un type | tout compte connecté |

Le droit se compare à la liste `roles` reçue à la connexion ([README racine](../README.md#qui-peut-appeler-quoi)). `ROLE_SUPER_ADMIN` passe la règle d'URL mais pas les `@PreAuthorize` (revue unitaire, statistiques, export, journaux, files, rétention) : il doit porter `DOCUMENT:REVIEW`.

### Listes de référence à charger

- Types de documents actifs : `GET /sni/api/v1/document-types?active=true` (option `ownerType`) ; pour un écran de dépôt sans `SYSTEM:SETTINGS`, `GET /sni/api/v1/document-types/{code}/upload-config`.
- Étiquettes : `GET /sni/api/v1/document-tags?space=…`.
- Dossiers racines : `GET /sni/api/v1/document-folders/roots?space=…`.
- Énumérations (statuts, espaces, catégories, propriétaires, niveaux de risque) : valeurs dans les fichiers `routes/*.md`.
- Propriétaires : codes membre, client, entreprise depuis les modules `client` et `company`.

### Identifiants numériques à manipuler sans perte

Les routes de ce module exigent plusieurs entiers int64 (voir [README racine](../README.md#identifiants--attention-aux-grands-nombres)) :

- `reviewedBy`, `authorId` (notes KYC), `assignedTo`, `userId` (journal par utilisateur, « ma file »), `granteeId`, `signerId` : l'identifiant numérique d'un compte. `GET /sni/api/v1/auth/me` ne le fournit pas (il renvoie `userId` texte) ; il figure dans `AdminUserResponse.id` des routes d'administration des comptes (module `securite`, route exacte non vérifiée). Le garder en texte.
- Chemins `{id}` : exigences documentaires, règles de contrôle croisé, `{signatureId}`.
- `ownerCode` des propriétaires `CONTRACT`, `INVOICE`, `PAYMENT`, `PROPOSAL`, `ASSET`.
- `KycDocumentResponse.id` : voir le piège ci-dessous.

### Pièges propres au module

- **Afficher un fichier** : `previewUrl` et `downloadUrl` des réponses pointent vers des routes qui exigent le jeton Bearer ; un `<img src>` ou un `<iframe>` ne peut pas les charger directement. Méthode : [parcours-televersement-et-apercus.md](parcours-televersement-et-apercus.md#afficher-un-aperçu-dans-le-navigateur).
- **Toutes les réponses portent `X-Frame-Options: DENY`** : un PDF servi par l'API ne s'affiche pas dans un `<iframe>`, même avec un jeton d'aperçu. Passer par un `blob:` ou ouvrir un nouvel onglet.
- `fileUrl` de `DocumentResponse` vaut la même chose que `previewUrl` ; `folderCode` et `folderName` sont toujours `null` (jamais remplis) ; l'état de verrouillage et la note de correction ne sont pas renvoyés.
- Les URL de `versions[]` (`previewUrl`, `downloadUrl`) pointent vers le fichier **courant**, pas vers la version listée : il n'existe pas de route pour télécharger une ancienne version (seulement la restaurer).
- `KycDocumentResponse.id` vaut l'identifiant **court** (`short_id`) dans la réponse du dépôt par l'administration, mais l'identifiant **interne** (int64) dans les réponses des dossiers et des files de revue. La route `PATCH /kyc/documents/admin/{kycDocumentId}/details` attend l'identifiant court : n'utiliser que l'`id` reçu du dépôt (voir écarts).
- Un `401` de `GET /sni/api/v1/shares/{token}` signifie « mot de passe du lien invalide », pas « session expirée » : ne pas déclencher le rafraîchissement du jeton.
- Les champs envoyés en `multipart/form-data` s'écrivent en parties de formulaire texte (pas de JSON) : `ownerType`, `ownerCode`, dates `AAAA-MM-JJ`. Les listes (`tagCodes`) se répètent : une partie par valeur.
- Après une approbation ou un refus de pièce KYC, le statut du dossier et du membre change **en tâche de fond** : recharger le dossier une à deux secondes plus tard.

## Effets automatiques

| Automatisme | Fréquence | Effet visible |
| --- | --- | --- |
| `DocumentLifecycleWorker` (`documentMaster/worker/DocumentLifecycleWorker.java`) | toutes les heures après la fin du passage précédent (`app.document.worker.fixed-delay-ms`, 3 600 000 ms ; désactivable par `DOCUMENT_WORKER_ENABLED`) | 1) passe en `EXPIRED` les documents `APPROVED`, `SIGNED`, `PENDING_REVIEW` dont `expiryDate` est passée (événement `DOCUMENT_AUTO_EXPIRED`, recalcul KYC) ; 2) publie `DOCUMENT_EXPIRY_REMINDER` à J-30 puis J-7, une seule fois par seuil ; 3) approuve les documents `PENDING_REVIEW` d'un type ayant `autoApprove = true` et `autoApproveAfterDays` > 0 quand le délai depuis le dépôt est écoulé (`DOCUMENT_AUTO_APPROVED`) |
| `KycAutomationWorker.autoApproveEligibleDocuments` | chaque jour à 08 h | approuve les pièces KYC `PENDING` dont le type a `autoApproveAfterDays` > 0 et dont le délai est écoulé (même sans `autoApprove`), au nom de l'utilisateur système |
| `KycAutomationWorker.checkExpiringKycDocuments` | le lundi à 08 h | publie `KYC_DOCUMENT_EXPIRY_REMINDER` pour les pièces qui expirent exactement dans 60, 30 ou 7 jours ce lundi-là ; passe en `EXPIRED` les pièces expirées depuis plus de 15 jours, met le dossier en `RENEWAL_REQUIRED` (s'il était approuvé) ou `PENDING_CORRECTION`, le membre en `PENDING_CORRECTION`, le client en `PENDING` |
| `KycAutomationWorker.remindIncompleteKycCases` | chaque jour à 09 h | relance les dossiers `IN_PROGRESS` ou `PENDING_CORRECTION` inactifs depuis 3, 7 ou 14 jours, 3 relances au plus (`reminderCount`, `lastReminderSentAt`) |
| `DocumentRetentionWorker` | chaque jour à 02 h 30 (`app.document.retention.cron`) | archive les documents `APPROVED`, `SIGNED`, `EXPIRED` visés par une politique active `ARCHIVE` ; `PURGE_FLAG` ne fait rien |
| OCR Tesseract | à chaque rattachement d'une pièce KYC | remplit le résultat lu (`/kyc/documents/{code}/ocr-result`). Activé par défaut en développement, **désactivé par défaut en production** (`KYC_OCR_ENABLED`) : le résultat porte alors seulement le statut « OCR_DISABLED » dans `rawOcrJson` |
| Garde du portail | à chaque appel du portail | un membre dont le dossier n'est pas `APPROVED` est bloqué (`403`, code `KYC_REQUIRED`) une fois passé son délai de grâce (`PORTAL_KYC_GRACE_PERIOD_DAYS`, 7 jours) (`features/portal/guard/ClientOnboardingGuard.java`) |

Courriels (via la file d'événements, traitée toutes les 2 s par défaut) :

- Dossier KYC, au propriétaire : `KYC_CASE_CREATED` (création manuelle), `KYC_CASE_APPROVED` (approbation manuelle seulement), `KYC_CASE_CORRECTION_REQUESTED` (refus du dossier), `KYC_INCOMPLETE_REMINDER` (`KycOutboxEventProcessor.java`).
- Pièce KYC, au propriétaire : `KYC_DOCUMENT_EXPIRY_REMINDER`, `KYC_DOCUMENT_EXPIRED`.
- Signature, au signataire (si `signerEmail`) : `DOCUMENT_SIGNATURE_REQUESTED`, `DOCUMENT_SIGNED`.
- Événements `DOCUMENT_*` : un courriel seulement si le propriétaire n'est **pas** un membre, client ou entreprise, et jamais pour le type `CONTRACT_DRAFT`.

Aucune notification dans l'application ni message WebSocket n'est émis par ce module : l'écran doit recharger.

## Écarts relevés

1. **Approbation KYC avec le seul droit `KYC:READ`** : la règle d'URL ne reconnaît que `/approve` et `/reject` précédés d'une barre ; `/kyc/documents/bulk-approve`, `/kyc/cases/{code}/documents/bulk-approve` et `bulk-reject` relèvent donc de `KYC:READ` (`security/authorization/AdminApiAuthorizationManager.java:161`). Comme le recalcul automatique approuve le dossier dès que toutes les pièces sont `VERIFIED` (`KycAutomationServiceImpl.java:286`), un compte `KYC:READ` peut faire aboutir un dossier sans `KYC:APPROVE`. De même `/kyc/documents/approved` exige `KYC:APPROVE` et `/rejected` `KYC:REJECT`, alors que les alias `/disapproved` et `/desapproved` (mêmes données) n'exigent que `KYC:READ`.
2. **Approbation automatique sans courriel ni portefeuille** : quand le dossier passe `APPROVED` par recalcul, l'événement est `KYC_CASE_AUTO_STATUS_SYNC` (aucun courriel) et le portefeuille par défaut n'est pas créé, contrairement à `POST /kyc/cases/{code}/approve` (`KycServiceImpl.java:293`, `KycAutomationServiceImpl.java:234`).
3. **Exigences propres au dossier ignorées par le recalcul** : `KycServiceImpl.filteredRequirementsForCase` (`KycServiceImpl.java:1096`) préfère les exigences du dossier, `KycAutomationServiceImpl.recomputeCaseState` (`KycAutomationServiceImpl.java:263`) n'utilise que les exigences globales et ignore le type de client. Les indicateurs `complete`/`approved` de la fiche et le statut automatique peuvent donc se contredire.
4. **Verso KYC et `multipleAllowed`** : le verso est un second document du même type ; avec `multipleAllowed = false` son dépôt échoue (`DocumentServiceImpl.java:551`). Redéposer un recto laisse l'ancien document en `PENDING_REVIEW` dans les files (jamais `SUPERSEDED`).
5. **Verso recréé comme pièce distincte (probable, non vérifié à l'exécution)** : après rattachement du verso, `KycDocumentUploadService.java:95` relance `syncFromDocumentUpload` sur le document verso ; la synchronisation asynchrone cherche une pièce par `document` (pas par `backDocument`) et en crée une nouvelle (`KycAutomationServiceImpl.java:247`). Même effet en approuvant le verso par `POST /documents/{codeVerso}/approve`.
6. **`KycDocumentResponse.id` incohérent** : identifiant court dans `KycDocumentUploadService.java:159`, identifiant interne via MapStruct ailleurs (`KycMapper.toDocumentResponse`, champ `id` copié tel quel), alors que `PATCH /kyc/documents/admin/{kycDocumentId}/details` cherche par `short_id` (`KycDocumentUploadService.java:144`). Le champ `KycBulkApproveRequest.documentIds` attend en fait des **codes** de documents.
7. **Opérations en masse documentaires et transaction unique** : `DocumentReviewQueueServiceImpl` est `@Transactional` et appelle `DocumentServiceImpl` (transactionnel) pour chaque code ; un échec sur un élément marque probablement toute la transaction en annulation, la réponse « partielle » finit alors en erreur `500` à la validation (non vérifié à l'exécution). Les variantes KYC sont en `NOT_SUPPORTED` et n'ont pas ce défaut. Le paramètre `{space}` n'y filtre rien : n'importe quel code est traité.
8. **Droits `DOCUMENT:REVIEW_<ESPACE>` inopérants** : `DocumentSpaceSecurityService.canReview` les accepte, mais la règle d'URL `/review` exige déjà `DOCUMENT:REVIEW` (`AdminApiAuthorizationManager.java:240`), et les routes unitaires `/documents/{code}/approve` aussi.
9. **Écritures avec le droit de lecture** : `PATCH`/`DELETE /documents/{code}/metadata…`, `DELETE /documents/{code}/tags/{tagCode}` et `PATCH /documents/{code}/signatures/{id}/sign` ne demandent que `DOCUMENT:READ` (méthodes non `POST`). La signature ne vérifie pas l'identité du signataire et ne change pas le statut du document (`SIGNED` jamais atteint, `DocumentSignatureServiceImpl.java:39`).
10. **Verrou sans effet** : `DocumentLockService.isLocked` n'est appelé nulle part ; remplacer, revoir ou archiver un document verrouillé reste possible, et l'état du verrou n'est pas exposé.
11. **Permissions documentaires déclaratives** : `/document-permissions` enregistre des droits que rien ne contrôle (`DocumentPermissionService.hasPermission` n'est jamais appelé).
12. **Lien de partage sans contenu** : `GET /shares/{token}` ne renvoie que les métadonnées du lien, aucune route publique ne sert le fichier ; `allowDownload` n'est lu nulle part ; le mot de passe passe en paramètre d'URL ; `DELETE /shares/{token}` est public (`security/config/PublicPaths.java:63`), quiconque connaît le jeton peut le révoquer.
13. **Formats annoncés mais refusés** : les espaces financier, contrats et actifs acceptent XLSX, CSV, DOCX et TIFF (`DocumentSpaceService.java:48`), mais la détection par signature ne reconnaît que PDF, JPEG, PNG et WebP (`DocumentSecurityService.java:68`). `upload-config` annonce par défaut PDF, JPEG, PNG, alors que le dépôt accepte aussi WebP. La limite ZIP de 100 Mo (`DocumentBatchService.java:31`) est inatteignable (10 Mo par requête).
14. **Remplacement sans garde** : `/replace` accepte un document `ARCHIVED`, `EXPIRED` ou `REJECTED` et le remet en revue, ignore `autoApprove` et le verrou (`DocumentServiceImpl.java:170`).
15. **`ownerCode` numérique pour les contrats et factures** : `DocumentServiceImpl.java:665` exige l'identifiant interne en texte au lieu du code métier ; `DocumentResponse` ne renvoie que `ownerId`.
16. **Circuits d'approbation décoratifs** : l'approbateur désigné n'est pas contrôlé, le statut du document ne change pas, `autoApproveDays` n'est utilisé nulle part, un circuit désactivé peut être lancé, et `GET /documents/{code}/workflow/status` lève une erreur si le document a déjà eu plusieurs instances (`findByDocumentAndStatusIn` renvoie un seul résultat, `DocumentWorkflowService.java:112`).
17. **Événements KYC sans destinataire** : `KYC_CASE_SUBMITTED`, `KYC_CASE_ASSIGNED`, `KYC_CASE_AUTO_CREATED`, `KYC_CASE_NOTE_ADDED`… n'ont aucun processeur (`KycOutboxEventProcessor` en filtre la liste, `NotificationOutboxEventProcessor` ne gère pas `KYC_CASE`) ; `OutboxServiceImpl.resolveProcessor` lève alors une erreur et l'événement reste en échec. `KYC_CASE_REJECTED` n'est jamais publié.
18. **Relances d'expiration KYC rares** : le contrôle hebdomadaire ne retient que les pièces qui expirent exactement à J+60, J+30 ou J+7 du lundi (`KycAutomationWorker.java:58`) ; six pièces sur sept ne reçoivent pas la relance correspondante.
19. **Approbation automatique incohérente** : le worker documentaire exige `autoApprove = true` (mais un tel type est déjà approuvé au dépôt, seuls les remplacements en profitent), le worker KYC l'ignore (`DocumentLifecycleAutomationServiceImpl.java:101`, `KycAutomationWorker.java:95`).
20. **Rétention** : aucune route ne réactive une politique désactivée ; `PURGE_FLAG` est accepté mais sans effet (`DocumentRetentionServiceImpl.java:117`).
21. **`reviewedBy`, `authorId` fournis par le client** : l'auteur d'une décision ou d'une note est celui que le corps déclare, pas le compte connecté (`DocumentReviewDecisionRequest`, `KycCaseNoteRequest`).
22. Messages d'erreur majoritairement en anglais ; erreurs de taille de fichier et de conversion d'énumération sans traitement dédié dans `core/exception/GlobalExceptionHandler.java` (repli `500`).
