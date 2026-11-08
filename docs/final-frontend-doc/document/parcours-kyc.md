# document : KYC (vérification d'identité)

> Partie du [README du module](README.md). Routes exactes : [routes/KycController.md](routes/KycController.md), [routes/KycDocumentAdminController.md](routes/KycDocumentAdminController.md), [routes/KycDocumentReviewController.md](routes/KycDocumentReviewController.md), [routes/KycCrossValidationRuleController.md](routes/KycCrossValidationRuleController.md).

## Principe

- Un dossier KYC (`code` `KYCCASE…`) par propriétaire `MEMBER`, `CUSTOMER` ou `BUSINESS` (les autres types de propriétaire sont refusés : `400` « KYC is only supported for member, customer or business »).
- Les pièces attendues viennent, dans l'ordre de priorité : des exigences propres au dossier (`/kyc/cases/{code}/requirements`) si le dossier en a au moins une active, sinon des exigences globales actives pour le type de propriétaire (`/document-requirements`, filtrées par forme juridique pour une entreprise). Attention : le recalcul automatique n'utilise que les exigences globales (voir écarts du README).
- Une pièce KYC = un document recto (`documentCode`) et, si le type a `requiresBackSide = true`, un document verso (`backDocumentCode`). Le verso est un document à part entière du même type.
- Le statut de vérification d'une pièce suit le statut du document recto : approuver ou refuser une pièce, c'est approuver ou refuser son document.
- Le dossier évolue seul quand ses pièces changent (recalcul asynchrone) et peut aussi recevoir une décision manuelle.
- Le membre dépose ses pièces depuis le portail (routes `/sni/api/v1/client/documents/kyc/**`, module [portal](../portal/routes.md)) ; elles arrivent dans les mêmes files.

## Tableau de bord KYC

`GET /sni/api/v1/kyc/cases/dashboard` ([route](routes/KycController.md#r-get-sni-api-v1-kyc-cases-dashboard)), droit `KYC:READ`.

- `summary` : `totalCases` et un compteur par statut de dossier (clés `IN_PROGRESS`, `SUBMITTED`…).
- `sla` : `casesReviewedWithin24h`, `casesExceeding48h` (soumis, non revus depuis plus de 48 h), `avgReviewTimeHours`.
- `expiringSoon` : pièces qui expirent sous 7, 30, 60 jours.
- `byOwnerType` : nombre de dossiers et dossiers en attente par type de propriétaire.
- `recentActivity` : dossiers approuvés et refusés sur 7 jours.

## Lister et filtrer les dossiers

1. `GET /sni/api/v1/kyc/cases` ([route](routes/KycController.md#r-get-sni-api-v1-kyc-cases)), droit `KYC:READ`, tri par défaut `startedAt` croissant, 20 par page. Filtres :
   - `status` : nom exact du statut, ou alias `PENDING`/`PENDING_REVIEW` (→ `SUBMITTED`) et `IN_REVIEW`/`REVIEWING` (→ `UNDER_REVIEW`) ;
   - `ownerType`, `riskLevel` (`LOW`, `MEDIUM`, `HIGH`, `VERY_HIGH`) ;
   - `submittedAfter`, `submittedBefore` (date-heure ISO) ;
   - `reviewedBy` (int64) ;
   - `pendingReviewOnly=true` (statuts `SUBMITTED` et `UNDER_REVIEW`) ;
   - `expiringWithinDays` (dossiers ayant une pièce qui expire dans ce délai).
2. Dossiers à échéance : `GET /sni/api/v1/kyc/cases/expiring-soon?days=30`.
3. Ma file : `GET /sni/api/v1/kyc/cases/my-queue` ([route](routes/KycController.md#r-get-sni-api-v1-kyc-cases-my-queue)) : dossiers affectés au compte connecté (ou au `userId` passé), du plus ancien soumis au plus récent.
4. Chaque ligne `KycCaseResponse` : `code`, `ownerName`, `ownerCode`, `ownerType`, `status`, `riskLevel`, `kycLevel` (1 à 4), `slaDeadline`, `assignedToEmail`, `complete`, `approved`, `missingDocumentTypeCodes`, et le détail complet (`requirements[]`, `documents[]`). La liste est lourde : chaque ligne embarque toutes les pièces.

## Ouvrir un dossier

1. `POST /sni/api/v1/kyc/cases` ([route](routes/KycController.md#r-post-sni-api-v1-kyc-cases)), droit `KYC:READ`, `Idempotency-Key` conseillé. Corps : `{ "ownerType": "MEMBER", "ownerCode": "MBR…", "documentTypeCodes": ["CNI", "JUSTIFICATIF_DOMICILE"] }` (`documentTypeCodes` facultatif ; chaque code doit exister, sinon `400` « Unknown document type: X »).
2. Si le propriétaire a déjà un dossier qui n'est ni `APPROVED` ni `REJECTED`, ce dossier est renvoyé tel quel (même statut `201`, `documentTypeCodes` ignoré). Comme un dossier « refusé » est en `PENDING_CORRECTION`, c'est le cas le plus fréquent.
3. Sinon : nouveau dossier `IN_PROGRESS`, risque `MEDIUM` pour une entreprise, `LOW` sinon ; `documentTypeCodes` devient la liste des exigences propres au dossier ; un membre passe `PENDING` ; courriel « Votre dossier KYC est ouvert » au propriétaire.
4. Un dossier est aussi ouvert automatiquement au premier dépôt d'un document de catégorie `KYC` (sans courriel).

## Fiche dossier

`GET /sni/api/v1/kyc/cases/{code}` ([route](routes/KycController.md#r-get-sni-api-v1-kyc-cases-code)).

- En-tête : propriétaire, statut, niveau de risque, niveau KYC, dates (`startedAt`, `submittedAt`, `reviewedAt`, `completedAt`), réviseur et affecté (courriels), `slaDeadline`, `decisionComment`, relances (`reminderCount`, `lastReminderSentAt`).
- `requirements[]` (`KycRequirementStatus`) : une ligne par type exigé, avec `required`, `requiresBackSide`, `status` (statut de la pièce, `null` si absente), `documentCode`, `backDocumentCode`. C'est la check-list à afficher.
- `missingDocumentTypeCodes` : types manquants ; `X_BACK` signale un verso manquant. `complete` vrai si rien ne manque ; `approved` vrai si toutes les pièces exigées sont `VERIFIED`.
- `documents[]` (`KycDocumentResponse`) : `documentCode`, `documentType`, `documentNumber`, dates, `status`, fichier recto (`fileName`, `mimeType`, `previewUrl`) et verso (`backDocumentCode`, `backMimeType`, `backPreviewUrl`), `uploadedByEmail`, `uploadedAt`. Pour afficher recto et verso, appliquer [la méthode d'aperçu](parcours-televersement-et-apercus.md#afficher-un-aperçu-dans-le-navigateur) à `documentCode` et `backDocumentCode`.
- Compléments :
  - `GET /kyc/cases/{code}/missing-requirements` : exigences obligatoires manquantes (sans le détail verso) ;
  - `GET /kyc/cases/{code}/expiry-status` : pour chaque pièce datée, `expiryDate`, `daysUntilExpiry`, `expired`, `status` ;
  - `GET /kyc/cases/{code}/timeline` : événements datés (`CASE_CREATED`, `CASE_SUBMITTED`, `DOCUMENT_UPLOADED`, `DOCUMENT_VERIFIED`/`DOCUMENT_REJECTED`, `NOTE_ADDED`, et tous les événements publiés pour le dossier), acteur `SYSTEM`, code propriétaire ou `USR-<id>` ;
  - `GET /kyc/cases/{code}/review-queue` : pièces `PENDING` du dossier.

## Déposer une pièce pour le compte d'un propriétaire

`POST /sni/api/v1/kyc/documents/admin/upload` ([route](routes/KycDocumentAdminController.md#r-post-sni-api-v1-kyc-documents-admin-upload)), droit `KYC:READ`, `multipart/form-data`.

1. Préalable : le propriétaire doit avoir un dossier (sinon `400` « Aucun dossier KYC actif pour ce propriétaire · créez-en un d'abord ») ; le type doit être de catégorie `KYC` pour que la pièce soit suivie ; un type recto/verso doit avoir `multipleAllowed = true` (sinon le verso échoue en `400` « Only one document of type 'X' is allowed per owner »).
2. Parties du formulaire (toutes en texte, sauf le fichier) : `ownerType`, `ownerCode`, `documentTypeCode` (obligatoires), `side` (`FRONT` par défaut, ou `BACK`), `documentNumber`, `issueDate`, `expiryDate` (`AAAA-MM-JJ`), et la partie fichier `file`.
3. Recto (`side=FRONT`) : crée le document (titre « <nom du type> · <ownerCode> »). Si le dossier a déjà une pièce de ce type, elle est **réutilisée** : son recto est remplacé par le nouveau document, ses champs mis à jour s'ils sont fournis, son statut remis `PENDING` (l'ancien document reste en base, en `PENDING_REVIEW`). Sinon une nouvelle pièce est créée.
4. Verso (`side=BACK`) : déposer **après** le recto (sinon `400` « Le recto doit être uploadé avant le verso pour le type : X »). Le document verso (titre « … (Arrière) · … ») est rattaché comme `backDocument` de la première pièce de ce type ; la réponse est la pièce recto enrichie de `backDocumentCode`.
5. Les champs exigés par le type (`requiresDocumentNumber`…) sont seulement journalisés ici, pas imposés. Les compléter après coup si besoin (étape 7).
6. Réponse `201` `KycDocumentResponse` ; **son `id` est l'identifiant court** de la pièce, à conserver pour l'étape 7. `previewUrl` et `downloadUrl` n'y sont pas remplis : utiliser `documentCode`.
7. Compléter : `PATCH /sni/api/v1/kyc/documents/admin/{kycDocumentId}/details` ([route](routes/KycDocumentAdminController.md#r-patch-sni-api-v1-kyc-documents-admin-kycdocumentid-details)) avec `{ "documentNumber": "…", "issueDate": "2024-01-15", "expiryDate": "2034-01-15" }` (champs vides ignorés). `{kycDocumentId}` = l'`id` court reçu à l'étape 6 ; l'`id` des autres réponses KYC est l'identifiant interne et donne `404` « Document KYC introuvable ». Ces valeurs ne sont pas recopiées sur le document lui-même.

Écran conseillé : pour chaque ligne de `requirements[]`, un champ recto et, si `requiresBackSide`, un champ verso activé une fois le recto envoyé. Après l'envoi, recharger la fiche (le recalcul du dossier est asynchrone).

## Réviser les pièces

### File de revue des pièces

- `GET /sni/api/v1/kyc/documents/review-queue` ([route](routes/KycDocumentReviewController.md#r-get-sni-api-v1-kyc-documents-review-queue)) : toutes les pièces `PENDING`, liste complète (pas de pagination), les plus récemment modifiées d'abord.
- `GET /sni/api/v1/kyc/documents/reviewed?status=APPROVED|REJECTED` (sans `status` : les deux) ; alias `GET /kyc/documents/approved` (droit `KYC:APPROVE`), `GET /kyc/documents/rejected` (droit `KYC:REJECT`), `/disapproved` et `/desapproved` (droit `KYC:READ`).
- Chaque ligne porte `ownerName`, `documentType`, `documentCode`, `backDocumentCode`, `status`, `uploadedAt`.

### Approuver une pièce

Au choix :
- unitaire : `POST /sni/api/v1/documents/{documentCode}/approve` avec `{ "reviewedBy": "…" }` (droit `DOCUMENT:REVIEW`, voir [parcours-revue.md](parcours-revue.md#approuver-un-document)) ;
- en masse : `POST /sni/api/v1/kyc/documents/bulk-approve` ([route](routes/KycDocumentReviewController.md#r-post-sni-api-v1-kyc-documents-bulk-approve)) avec `{ "documentIds": ["DOC…"], "reviewedBy": "…" }` (malgré son nom, `documentIds` attend des **codes** de documents), ou par dossier `POST /sni/api/v1/kyc/cases/{code}/documents/bulk-approve` avec `{ "documentCodes": [...], "reviewedBy": "…" }` (les codes étrangers au dossier sont signalés en échec). Droit `KYC:READ`, `Idempotency-Key` conseillé.

Réponse en masse `KycBulkActionResponse` : `processed`, `succeeded`, `failed`, `results[]` (`documentCode`, `success`, `error`). Chaque pièce est traitée dans sa propre transaction.

Le verso n'a pas de décision propre côté KYC : approuver le recto suffit à rendre la pièce `VERIFIED`. Le document verso reste `PENDING_REVIEW` dans les files documentaires ; l'approuver par son code fait recréer une pièce séparée (voir écarts). Conseil : ne pas présenter le verso dans les files documentaires comme une décision à prendre.

Quand toutes les pièces exigées sont `VERIFIED`, le recalcul passe le dossier en `APPROVED` et le membre en `ACTIVE`, sans courriel ni création de portefeuille. Pour obtenir le courriel et le portefeuille, appeler ensuite l'approbation manuelle du dossier (elle reste possible sur un dossier déjà `APPROVED`).

### Refuser ou faire corriger des pièces

- Refus en masse : `POST /sni/api/v1/kyc/documents/bulk-reject` (alias `/bulk-disapprove`, `/bulk-desapprove`) ou `POST /sni/api/v1/kyc/cases/{code}/documents/bulk-reject`, corps `{ "items": [ { "documentCode": "DOC…", "reason": "Photo illisible" } ], "reviewedBy": "…" }` (`reason` obligatoire par élément). Chaque document est refusé comme en [refus unitaire](parcours-revue.md#refuser-un-document), cascade comprise.
- Correction d'une pièce du dossier : `POST /sni/api/v1/kyc/cases/{code}/documents/request-correction` ([route](routes/KycController.md#r-post-sni-api-v1-kyc-cases-code-documents-request-correction)), corps `{ "documentCode": "DOC…", "reviewedBy": "…", "correctionNote": "…", "deadlineDays": 7 }`. Le document passe `NEEDS_CORRECTION` et la pièce **`REJECTED`** (contrairement à la correction unitaire documentaire qui la laisse `PENDING`). `404` « Document not found in case X » si le code n'appartient pas au dossier.
- Effet sur le dossier : dès qu'une pièce est `REJECTED`, le recalcul met le dossier et le membre en `PENDING_CORRECTION`.

### Résultat OCR

`GET /sni/api/v1/kyc/documents/{documentCode}/ocr-result` ([route](routes/KycDocumentReviewController.md#r-get-sni-api-v1-kyc-documents-documentcode-ocr-result)) : `extractedFirstName`, `extractedLastName`, `extractedDateOfBirth`, `extractedExpiryDate`, `extractedDocumentNumber`, `extractedNationality`, `confidenceScore`, `rawOcrJson`, `processedAt`. À afficher à côté de l'aperçu pour aider la vérification. Sans résultat : objet avec seulement `documentCode`. Un code vide, `null`, `undefined` ou `-` renvoie un objet vide (pas d'erreur). En production l'OCR est désactivé par défaut : prévoir l'absence de valeurs.

## Soumettre, affecter, décider

### Soumettre

`POST /sni/api/v1/kyc/cases/{code}/submit` ([route](routes/KycController.md#r-post-sni-api-v1-kyc-cases-code-submit)), sans corps, `Idempotency-Key` conseillé.

- Refusé si une pièce exigée ou un verso manque : `400` « KYC case is incomplete. Missing required documents: … ».
- Effets : `SUBMITTED` (client : `UNDER_REVIEW`), `submittedAt`, niveau de risque et niveau KYC recalculés, échéance de revue `slaDeadline` = soumission + 24 h (membre), 48 h (client) ou 72 h (entreprise). Le membre passe `UNDER_REVIEW`.
- Bouton à afficher quand `complete` est vrai et le statut `IN_PROGRESS` ou `PENDING_CORRECTION`.

### Affecter à un réviseur

`PATCH /sni/api/v1/kyc/cases/{code}/assign` ([route](routes/KycController.md#r-patch-sni-api-v1-kyc-cases-code-assign)), corps `{ "assignedTo": "…" }` (int64) **ou** `{ "email": "agent@…" }`. `400` « Either assignedTo (ID) or email is required », `400` « No user found with email: … ». Un dossier `SUBMITTED` passe `UNDER_REVIEW` ; l'échéance de revue est recalculée (si le dossier n'avait pas de date de soumission, l'affectation en pose une). Privilégier le courriel : il évite de manipuler l'identifiant numérique.

### Niveau de risque

`PATCH /sni/api/v1/kyc/cases/{code}/risk-level`, corps `{ "riskLevel": "HIGH", "reviewedBy": "…", "comment": "…" }`. Écrase `decisionComment` par `comment` (même vide). Un risque `HIGH` ou `VERY_HIGH` est conservé lors des recalculs et porte le niveau KYC à 4.

### Approuver le dossier

`POST /sni/api/v1/kyc/cases/{code}/approve` ([route](routes/KycController.md#r-post-sni-api-v1-kyc-cases-code-approve)), droit `KYC:APPROVE`, `Idempotency-Key` conseillé, corps `{ "reviewedBy": "…", "comment": "…" }`.

1. Conditions : aucune pièce ni verso manquant ; toutes les pièces exigées `VERIFIED` ; règles de contrôle croisé bloquantes respectées. Sinon `400` (messages dans le [README](README.md#kyc)).
2. Effets : `APPROVED`, réviseur et dates posés, échéance effacée ; membre `ACTIVE` ou client `ACTIVE` ; portefeuille par défaut en `XAF` créé s'il n'existe pas (un échec ici ne bloque pas) ; courriel « Bonne nouvelle : votre dossier KYC est validé ».
3. Aucun contrôle de statut de départ : un dossier déjà approuvé peut être réapprouvé.

### Refuser le dossier (renvoyer en correction)

`POST /sni/api/v1/kyc/cases/{code}/reject` ([route](routes/KycController.md#r-post-sni-api-v1-kyc-cases-code-reject)), droit `KYC:REJECT`, corps `{ "reviewedBy": "…", "comment": "Motif" }` (`comment` obligatoire).

- Le dossier passe **`PENDING_CORRECTION`** (pas `REJECTED`), le membre `PENDING_CORRECTION`, le client `PENDING`. Une trace de vérification est ajoutée pour chaque pièce `PENDING` ou `REJECTED`. Courriel « Une petite mise à jour est nécessaire sur votre dossier KYC ».
- Le refus du dossier ne refuse aucune pièce : indiquer au membre lesquelles refaire en refusant ou en faisant corriger les pièces concernées.
- Attention : un nouveau dépôt ou une nouvelle revue de pièce relance le recalcul, qui peut faire quitter `PENDING_CORRECTION` sans action du réviseur.

## Notes internes

- Ajouter : `POST /sni/api/v1/kyc/cases/{code}/notes`, corps `{ "content": "…", "authorId": "…", "internal": true }` (`authorId` int64 obligatoire, `internal` vrai par défaut). Réponse `201` `KycCaseNoteResponse` avec `authorEmail`.
- Lister : `GET /sni/api/v1/kyc/cases/{code}/notes`, les plus récentes d'abord.
- Le drapeau `internal` n'est exploité que par l'export PDF (toutes les notes y figurent si `includeInternalNotes=true`, aucune sinon).

## Exigences propres au dossier

- Lister : `GET /sni/api/v1/kyc/cases/{code}/requirements` (actives et inactives, `active` dans chaque ligne).
- Ajouter : `POST /sni/api/v1/kyc/cases/{code}/requirements`, corps `{ "documentTypeCode": "CNI", "documentTypeName": "…", "required": true }`. `400` « Requirement already exists for document type: CNI » (même si elle a été retirée : on ne peut pas la remettre). Le code de type n'est pas vérifié ; le nom par défaut est celui du type, sinon le code.
- Retirer : `DELETE /sni/api/v1/kyc/cases/{code}/requirements/{documentTypeCode}` (désactivation, `204`).
- Dès qu'un dossier a une exigence propre active, ses exigences globales ne comptent plus pour la fiche, la soumission et l'approbation manuelle.

## Export PDF

`GET /sni/api/v1/kyc/cases/{code}/export/pdf?includeInternalNotes=true` ([route](routes/KycController.md#r-get-sni-api-v1-kyc-cases-code-export-pdf)) : fichier `kyc-<code>.pdf` (`attachment`), avec dossier, chronologie, échéances et notes. Télécharger par `fetch` avec Bearer puis `blob`.

## Règles de contrôle croisé

Paramétrage (droit `KYC:READ`) : `GET/POST /sni/api/v1/kyc/cross-validation-rules`, `GET/PUT/DELETE /sni/api/v1/kyc/cross-validation-rules/{id}` ([routes](routes/KycCrossValidationRuleController.md)). `{id}` est un int64 ⚠.

- Corps : `{ "documentTypeCode1": "CNI", "documentTypeCode2": "PASSEPORT", "fieldToCompare": "last_name", "blocking": true, "active": true }`. Codes mis en majuscules, champ en minuscules.
- Champs comparables : `first_name`/`firstname`, `last_name`/`lastname`/`name`, `date_of_birth`/`dob`, `expiry_date`, `document_number`, `nationality`. Toute autre valeur est acceptée à l'enregistrement mais ne compare rien.
- À l'approbation manuelle d'un dossier, pour chaque règle active : si les deux types de pièces sont présents, que l'OCR a lu le champ sur les deux et que les valeurs diffèrent (sans tenir compte de la casse), une règle `blocking` refuse l'approbation (`400` « KYC cross-validation failed for field last_name »). Sans OCR, aucune règle ne bloque.
- `DELETE` supprime réellement la règle ; `PUT` exige tous les champs obligatoires.
- `GET` sans `activeOnly` renvoie toutes les règles.
