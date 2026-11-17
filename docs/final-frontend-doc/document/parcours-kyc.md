# document : KYC (vérification d'identité)

> Partie du [README du module](README.md). Routes exactes : [routes/KycController.md](routes/KycController.md), [routes/KycDocumentAdminController.md](routes/KycDocumentAdminController.md), [routes/KycDocumentReviewController.md](routes/KycDocumentReviewController.md), [routes/KycCrossValidationRuleController.md](routes/KycCrossValidationRuleController.md).

> **Lot KG0 (16/11/2026)** : les écritures sur un dossier (création, soumission, affectation, niveau de risque, notes, exigences, dépôt par l'équipe, détails d'une pièce, règles) demandent `KYC:MANAGE` ; l'auteur d'une décision ou d'une note est toujours le compte connecté (`reviewedBy`, `authorId` ne sont plus à envoyer, ils sont ignorés) ; une pièce KYC ne se décide qu'avec `KYC:APPROVE` ou `KYC:REJECT`, même par les routes documentaires.

## Principe

- Un dossier KYC (`code` `KYCCASE…`) par propriétaire `MEMBER`, `CUSTOMER` ou `BUSINESS` (les autres types de propriétaire sont refusés : `400` « KYC is only supported for member, customer or business »).
- Les pièces attendues viennent, dans l'ordre de priorité : des exigences propres au dossier (`/kyc/cases/{code}/requirements`) si le dossier en a au moins une active, sinon des exigences globales actives pour le type de propriétaire (`/document-requirements`, filtrées par forme juridique pour une entreprise). Attention : le recalcul automatique n'utilise que les exigences globales (voir écarts du README).
- Une pièce KYC = un document recto (`documentCode`) et, si le type a `requiresBackSide = true`, un document verso (`backDocumentCode`). Le verso est un document à part entière du même type, avec sa propre décision ; il ne forme jamais une pièce à lui seul (lot KG2).
- Remplacer une pièce garde la même pièce (même `id`) : l'ancien fichier passe `SUPERSEDED` (il reste consultable dans la GED), il ne bloque plus de nouveau dépôt.
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

1. `POST /sni/api/v1/kyc/cases` ([route](routes/KycController.md#r-post-sni-api-v1-kyc-cases)), droit `KYC:MANAGE`, `Idempotency-Key` conseillé. Corps : `{ "ownerType": "MEMBER", "ownerCode": "MBR…", "documentTypeCodes": ["CNI", "JUSTIFICATIF_DOMICILE"] }` (`documentTypeCodes` facultatif ; chaque code doit exister, sinon `400` « Unknown document type: X »).
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

`POST /sni/api/v1/kyc/documents/admin/upload` ([route](routes/KycDocumentAdminController.md#r-post-sni-api-v1-kyc-documents-admin-upload)), droit `KYC:MANAGE`, `multipart/form-data`.

1. Préalable : le propriétaire doit avoir un dossier (sinon `400` « Aucun dossier KYC actif pour ce propriétaire · créez-en un d'abord ») ; le type doit être de catégorie `KYC` pour que la pièce soit suivie ; la pièce doit être demandée pour le dossier (lot KG1). La règle « un seul document du type » (`multipleAllowed`) ne s'applique plus aux pièces KYC : un recto remplace, un verso s'ajoute. **Réglé** (KG2).
2. Parties du formulaire (toutes en texte, sauf le fichier) : `ownerType`, `ownerCode`, `documentTypeCode` (obligatoires), `side` (`FRONT` par défaut, ou `BACK`), `documentNumber`, `issueDate`, `expiryDate` (`AAAA-MM-JJ`), et la partie fichier `file`.
3. Recto (`side=FRONT`) : crée le document (titre « <nom du type> · <ownerCode> »). Si le dossier a déjà une pièce de ce type, elle est **réutilisée** : son recto est remplacé par le nouveau document, ses champs mis à jour, son statut remis `PENDING`. L'ancien recto **et l'ancien verso** passent `SUPERSEDED` (une nouvelle carte demande un nouveau verso : `backDocumentCode` redevient nul). Sinon une nouvelle pièce est créée.
4. Verso (`side=BACK`) : déposer **après** le recto (sinon `400` « Le recto doit être déposé avant le verso pour la pièce : <nom> »). Le document verso (titre « … (Arrière) · … ») devient le `backDocument` de la pièce de ce type ; un ancien verso passe `SUPERSEDED` ; la pièce repasse `PENDING`. Aucune pièce n'est créée pour un verso. La réponse est la pièce, avec `backDocumentCode`.
5. Les champs exigés par le type (`requiresDocumentNumber`…) sont seulement journalisés ici, pas imposés. Les compléter après coup si besoin (étape 7).
6. Réponse `201` `KycDocumentResponse` ; son `id` est l'identifiant court de la pièce, à conserver pour l'étape 7. `previewUrl` et `downloadUrl` n'y sont pas remplis : utiliser `documentCode`.
7. Compléter : `PATCH /sni/api/v1/kyc/documents/admin/{kycDocumentId}/details` ([route](routes/KycDocumentAdminController.md#r-patch-sni-api-v1-kyc-documents-admin-kycdocumentid-details)) avec `{ "documentNumber": "…", "issueDate": "2024-01-15", "expiryDate": "2034-01-15" }` (champs vides ignorés). `{kycDocumentId}` = l'`id` de la pièce. Toutes les réponses KYC (`documents[]` de la fiche, files de revue, dépôt) donnent désormais le même `id` court. **Réglé** (KG2). Ces valeurs ne sont pas recopiées sur le document lui-même.

Écran conseillé : pour chaque ligne de `requirements[]`, un champ recto et, si `requiresBackSide`, un champ verso activé une fois le recto envoyé. Après l'envoi, recharger la fiche : le recalcul du dossier part juste après l'enregistrement du dépôt (asynchrone, une fois par document).

Qualité d'image : chaque pièce porte `qualityStatus` (`OK`, `WARNING`, `UNKNOWN` pour un format non analysé comme HEIC) et `qualityWarnings` (phrases à afficher : image floue, faible résolution, trop sombre), et `backQualityStatus`, `backQualityWarnings` pour le verso. Mesurées au dépôt (netteté par variance du laplacien, plus petit côté, luminosité), elles **n'empêchent rien** : à montrer au réviseur près de l'aperçu, et à l'équipe juste après un dépôt pour reprendre la photo si besoin. Seuils : paramètres `app.kyc.quality.blur-threshold` (100), `app.kyc.quality.min-side-px` (600), `app.kyc.quality.enabled`.

## Réviser les pièces

### File de revue des pièces

- `GET /sni/api/v1/kyc/documents/review-queue` ([route](routes/KycDocumentReviewController.md#r-get-sni-api-v1-kyc-documents-review-queue)) : toutes les pièces `PENDING`, liste complète (pas de pagination), les plus récemment modifiées d'abord.
- `GET /sni/api/v1/kyc/documents/reviewed?status=APPROVED|REJECTED` (sans `status` : les deux) ; alias `GET /kyc/documents/approved` (droit `KYC:READ` depuis le lot KG0), `GET /kyc/documents/rejected` (droit `KYC:REJECT`), `/disapproved` et `/desapproved` (droit `KYC:READ`).
- Chaque ligne porte `ownerName`, `documentType`, `documentCode`, `backDocumentCode`, `status`, `uploadedAt`.

### Approuver une pièce

Au choix :
- unitaire : `POST /sni/api/v1/documents/{documentCode}/approve` avec `{ "reviewedBy": "…" }` (droit `DOCUMENT:REVIEW`, voir [parcours-revue.md](parcours-revue.md#approuver-un-document)) ;
- en masse : `POST /sni/api/v1/kyc/documents/bulk-approve` ([route](routes/KycDocumentReviewController.md#r-post-sni-api-v1-kyc-documents-bulk-approve)) avec `{ "documentCodes": ["DOC…"] }` (`documentIds`, ancien nom, reste accepté ; les deux attendent des **codes** de documents ; `400` si aucun code), ou par dossier `POST /sni/api/v1/kyc/cases/{code}/documents/bulk-approve` avec `{ "documentCodes": [...], "reviewedBy": "…" }` (les codes étrangers au dossier sont signalés en échec). Droit `KYC:APPROVE`, `Idempotency-Key` conseillé.

Réponse en masse `KycBulkActionResponse` : `processed`, `succeeded`, `failed`, `results[]` (`documentCode`, `success`, `error`). Chaque pièce est traitée dans sa propre transaction.

Le verso a sa propre décision : l'approuver ou le refuser par son code (`backDocumentCode`), par les mêmes routes (unitaire ou en masse). Une pièce n'est complète que si son recto est `VERIFIED` et son verso approuvé (lot KG1) ; un verso refusé rend la pièce `REJECTED`. Une décision sur un verso met à jour sa pièce, elle n'en crée plus une. **Réglé** (KG2). Écran conseillé : sur la ligne de la pièce, deux aperçus et deux décisions (recto, verso), ou une action « Approuver la pièce » qui envoie les deux codes en masse.

Quand toutes les pièces exigées sont vérifiées, le recalcul passe le dossier en `APPROVED` avec les mêmes effets qu'une approbation manuelle : titulaire actif, portefeuille créé, courriel « votre dossier KYC est validé » (lot KG1).

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

### Niveau KYC demandé

`PATCH /sni/api/v1/kyc/cases/{code}/level`, droit `KYC:MANAGE`, corps `{ "level": 2, "reason": "Volume de transactions" }` (`level` de 1 à 4, `reason` facultatif, repris dans le courriel). Réponse : la fiche `KycCaseResponse`.

- `requestedKycLevel` (fiche) : niveau demandé ; `kycLevel` : niveau atteint (au moins le niveau demandé une fois le dossier approuvé).
- Les exigences globales de niveau inférieur ou égal deviennent exigées ; elles apparaissent aussitôt dans `requirements` et `missingDocumentTypeCodes`, et dans l'espace client du titulaire.
- Le titulaire reçoit le courriel « Quelques pièces complémentaires pour votre dossier KYC » avec la liste des pièces à fournir.
- Un dossier approuvé auquel il manque une pièce obligatoire du nouveau niveau repasse `IN_PROGRESS` ; le membre ou le client reste actif.
- `400` si le niveau sort de 1 à 4, ou si le dossier est `REJECTED` ou `EXPIRED`. Même niveau : rien ne change. Baisser le niveau ne rouvre rien.
- Écran conseillé : sélecteur de niveau 1 à 4 avec motif sur la fiche ; montrer les pièces de chaque niveau d'après `GET /document-requirements` (champ `kycLevel`).

### Approuver le dossier

`POST /sni/api/v1/kyc/cases/{code}/approve` ([route](routes/KycController.md#r-post-sni-api-v1-kyc-cases-code-approve)), droit `KYC:APPROVE`, `Idempotency-Key` conseillé, corps `{ "reviewedBy": "…", "comment": "…" }`.

1. Conditions : aucune pièce ni verso manquant ; toutes les pièces exigées `VERIFIED` ; règles de contrôle croisé bloquantes respectées. Sinon `400` (messages dans le [README](README.md#kyc)).
2. Effets : `APPROVED`, réviseur et dates posés, échéance effacée ; membre `ACTIVE` ou client `ACTIVE` ; portefeuille par défaut en `XAF` créé s'il n'existe pas (un échec ici ne bloque pas) ; courriel « Bonne nouvelle : votre dossier KYC est validé ».
3. Un dossier déjà `APPROVED` ne se réapprouve pas (`400` « Ce dossier KYC est déjà approuvé ») ; un dossier `REJECTED` ou `EXPIRED` doit être rouvert avant (`400`). **Réglé** (KG1).
4. Les mêmes effets (titulaire actif, portefeuille, courriel) s'appliquent quand le recalcul approuve le dossier de lui-même.

### Refuser le dossier (renvoyer en correction)

`POST /sni/api/v1/kyc/cases/{code}/reject` ([route](routes/KycController.md#r-post-sni-api-v1-kyc-cases-code-reject)), droit `KYC:REJECT`, corps `{ "reviewedBy": "…", "comment": "Motif" }` (`comment` obligatoire).

- Le dossier passe **`PENDING_CORRECTION`** (pas `REJECTED`), le membre `PENDING_CORRECTION`, le client `PENDING`. Une trace de vérification est ajoutée pour chaque pièce `PENDING` ou `REJECTED`. Courriel « Une petite mise à jour est nécessaire sur votre dossier KYC ».
- Le refus du dossier ne refuse aucune pièce : indiquer au membre lesquelles refaire en refusant ou en faisant corriger les pièces concernées.
- Le recalcul ne fait plus quitter un dossier renvoyé en correction sans nouvelle pièce ; il ne touche plus un dossier `REJECTED` et garde `SUBMITTED`. **Réglé** (KG1).

## Notes internes

- Ajouter : `POST /sni/api/v1/kyc/cases/{code}/notes`, corps `{ "content": "…", "authorId": "…", "internal": true }` (`authorId` int64 obligatoire, `internal` vrai par défaut). Réponse `201` `KycCaseNoteResponse` avec `authorEmail`.
- Lister : `GET /sni/api/v1/kyc/cases/{code}/notes`, les plus récentes d'abord.
- Le drapeau `internal` n'est exploité que par l'export PDF (toutes les notes y figurent si `includeInternalNotes=true`, aucune sinon).

## Exigences propres au dossier

- Lister : `GET /sni/api/v1/kyc/cases/{code}/requirements` (actives et inactives, `active` dans chaque ligne).
- Ajouter : `POST /sni/api/v1/kyc/cases/{code}/requirements`, corps `{ "documentTypeCode": "CNI", "documentTypeName": "…", "required": true }`. `400` « Cette pièce est déjà demandée pour ce dossier : CNI », `400` « Type de document inconnu : … ». Une exigence retirée se remet par le même appel. **Réglé** (KG1).
- Retirer : `DELETE /sni/api/v1/kyc/cases/{code}/requirements/{documentTypeCode}` (`204`). Retirer une exigence globale en dispense ce seul dossier (une ligne inactive est créée). `404` si la pièce n'est pas demandée.
- Les exigences propres **s'ajoutent** aux exigences globales (avant, elles les remplaçaient). Une ligne active remplace le caractère obligatoire d'une exigence globale du même type ; une ligne inactive dispense. **Réglé** (KG1).
- Après un ajout ou un retrait, le dossier est recalculé : un dossier approuvé se rouvre (`IN_PROGRESS`) si une pièce obligatoire manque ; la pièce apparaît aussitôt dans l'espace client du titulaire, qui peut la déposer.
- Dépôt : seules les pièces demandées pour le dossier se déposent (équipe et titulaire) : `400` « Cette pièce n'est pas demandée pour ce dossier … ».

## Courriels au titulaire

Envoyés par la boîte d'envoi (un échec de mise en file est réessayé, lot KG3) :

| Événement | Objet |
| --- | --- |
| `KYC_CASE_CREATED` | Votre dossier KYC est ouvert |
| `KYC_CASE_APPROVED` (manuel ou par recalcul) | Bonne nouvelle : votre dossier KYC est validé |
| `KYC_CASE_CORRECTION_REQUESTED` | Une petite mise à jour est nécessaire sur votre dossier KYC |
| `KYC_DOCUMENT_CORRECTION_REQUESTED` | Une pièce de votre dossier KYC est à corriger (avec le motif) |
| `KYC_CASE_REQUIREMENT_ADDED` | Une pièce de plus pour votre dossier KYC (nom de la pièce, facultative ou non) |
| `KYC_CASE_LEVEL_CHANGED` | Quelques pièces complémentaires pour votre dossier KYC (liste) |
| `KYC_INCOMPLETE_REMINDER` | Petit rappel : votre dossier KYC vous attend (après 3, 7, 14 jours, 3 au plus) |
| `KYC_DOCUMENT_EXPIRY_REMINDER` | Échéance d'une pièce : un rappel par seuil (60, 30, 7 jours) |
| `KYC_RENEWAL_REQUIRED` | Votre dossier KYC est à renouveler (pièce échue, après 15 jours de grâce) |

Les autres événements (soumission, affectation, note, risque, recalcul…) sont consignés dans la chronologie du dossier sans courriel ; ils ne finissent plus en erreur dans la boîte d'envoi.

## Export PDF

`GET /sni/api/v1/kyc/cases/{code}/export/pdf?includeInternalNotes=true` ([route](routes/KycController.md#r-get-sni-api-v1-kyc-cases-code-export-pdf)) : fichier `kyc-<code>.pdf` (`attachment`), avec dossier, chronologie, échéances et notes. Télécharger par `fetch` avec Bearer puis `blob`.

## Règles de contrôle croisé

Les règles ne s'appuient plus seulement sur l'OCR (absent en production) : elles ont un **type**, et la plupart vérifient les données saisies au dépôt et la fiche du titulaire (17/11/2026).

### Résultat sur un dossier

`GET /sni/api/v1/kyc/cases/{code}/cross-checks` (droit `KYC:READ`, lecture seule) : une ligne par règle active et par pièce visée.

```json
[ { "ruleCode": "PIECE_NON_ECHUE", "ruleName": "Pièce non échue", "ruleType": "DOCUMENT_NOT_EXPIRED",
    "blocking": true, "outcome": "FAILED", "documentType": "MEMBER_NATIONAL_ID", "detail": "Pièce échue le 2026-10-01" } ]
```

- `outcome` : `PASSED`, `FAILED`, `SKIPPED` (donnée absente, OCR indisponible, une des deux pièces manque).
- Seules les pièces en attente ou vérifiées sont contrôlées.
- À l'approbation manuelle, une règle **bloquante** en échec refuse : `400` « Contrôles croisés non satisfaits : Pièce non échue (MEMBER_NATIONAL_ID : Pièce échue le …) ; … ». Les autres sont des signaux pour le réviseur.
- Écran conseillé : un encart « Contrôles » sur la fiche, échecs en rouge (bloquants) ou orange, ignorés en gris avec leur raison.

### Règles préenregistrées

Créées par migration (`systemRule = true`) : modifiables (libellé, paramètre, bloquante, active), **pas supprimables** (`400`, les désactiver).

| Code | Type | Contrôle | Bloquante |
| --- | --- | --- | --- |
| `PIECE_NON_ECHUE` | `DOCUMENT_NOT_EXPIRED` | date d'expiration saisie non dépassée | oui |
| `DATES_COHERENTES` | `ISSUE_BEFORE_EXPIRY` | émission passée et antérieure à l'expiration | oui |
| `VALIDITE_RESTANTE` | `MIN_VALIDITY` | encore valable au moins `parameter` jours (30) | non |
| `NUMERO_UNIQUE` | `UNIQUE_DOCUMENT_NUMBER` | numéro saisi non utilisé par un autre titulaire pour le même type | non |
| `NIU_PROFIL` | `PROFILE_MATCH` (`niu`) | numéro de la pièce NIU = NIU de la fiche entreprise | non |
| `RCCM_PROFIL` | `PROFILE_MATCH` (`rccm`) | numéro de la pièce RCCM = RCCM de la fiche entreprise | non |
| `NOM_PROFIL_OCR` | `PROFILE_MATCH` (`last_name`) | nom lu sur la pièce d'identité = nom de la fiche (OCR) | non |
| `NUMERO_SAISI_OCR` | `DECLARED_VS_OCR` (`document_number`) | numéro saisi = numéro lu (OCR) | non |
| `ECHEANCE_SAISIE_OCR` | `DECLARED_VS_OCR` (`expiry_date`) | échéance saisie = échéance lue (OCR) | non |
| `NOM_IDENTITE_DOMICILE` | `FIELD_MATCH` (`last_name`) | même nom lu sur l'identité et le justificatif de domicile (OCR) | non |

Les comparaisons ignorent la casse, les accents, les espaces et la ponctuation (« M 0123-45 » = « m012345 »). Les règles OCR restent `SKIPPED` tant que l'OCR n'est pas disponible.

### Paramétrage

Lecture `KYC:READ`, création, modification et suppression `KYC:MANAGE` : `GET/POST /sni/api/v1/kyc/cross-validation-rules`, `GET/PUT/DELETE /sni/api/v1/kyc/cross-validation-rules/{id}` ([routes](routes/KycCrossValidationRuleController.md)). `{id}` est un code identifiant (texte, voir le README racine).

- Corps : `{ "code": "…", "name": "…", "description": "…", "ruleType": "MIN_VALIDITY", "documentTypeCode1": "MEMBER_PASSPORT,MEMBER_NATIONAL_ID", "documentTypeCode2": null, "fieldToCompare": null, "parameter": "90", "blocking": false, "active": true }`.
- `ruleType` absent = `FIELD_MATCH` (comme avant). `documentTypeCode1` : un ou plusieurs types séparés par des virgules, vide = toutes les pièces.
- Champs exigés : `FIELD_MATCH` : deux types et `fieldToCompare` parmi `first_name`, `last_name`, `date_of_birth`, `expiry_date`, `document_number`, `nationality` ; `DECLARED_VS_OCR` : `document_number` ou `expiry_date` ; `PROFILE_MATCH` : `niu`, `rccm`, `last_name`, `first_name` ; `MIN_VALIDITY` : `parameter` en jours. Sinon `400` qui nomme les valeurs admises.
- Réponse : en plus, `id`, `code`, `name`, `description`, `ruleType`, `parameter`, `systemRule`.

## Courriels au titulaire

Envoyés par la boîte d'envoi (un échec de mise en file est réessayé, lot KG3) :

| Événement | Objet |
| --- | --- |
| `KYC_CASE_CREATED` | Votre dossier KYC est ouvert |
| `KYC_CASE_APPROVED` (manuel ou par recalcul) | Bonne nouvelle : votre dossier KYC est validé |
| `KYC_CASE_CORRECTION_REQUESTED` | Une petite mise à jour est nécessaire sur votre dossier KYC |
| `KYC_DOCUMENT_CORRECTION_REQUESTED` | Une pièce de votre dossier KYC est à corriger (avec le motif) |
| `KYC_CASE_REQUIREMENT_ADDED` | Une pièce de plus pour votre dossier KYC (nom de la pièce, facultative ou non) |
| `KYC_CASE_LEVEL_CHANGED` | Quelques pièces complémentaires pour votre dossier KYC (liste) |
| `KYC_INCOMPLETE_REMINDER` | Petit rappel : votre dossier KYC vous attend (après 3, 7, 14 jours, 3 au plus) |
| `KYC_DOCUMENT_EXPIRY_REMINDER` | Échéance d'une pièce : un rappel par seuil (60, 30, 7 jours) |
| `KYC_RENEWAL_REQUIRED` | Votre dossier KYC est à renouveler (pièce échue, après 15 jours de grâce) |

Les autres événements (soumission, affectation, note, risque, recalcul…) sont consignés dans la chronologie du dossier sans courriel ; ils ne finissent plus en erreur dans la boîte d'envoi.

## Export PDF

`GET /sni/api/v1/kyc/cases/{code}/export/pdf?includeInternalNotes=true` ([route](routes/KycController.md#r-get-sni-api-v1-kyc-cases-code-export-pdf)) : fichier `kyc-<code>.pdf` (`attachment`), avec dossier, chronologie, échéances et notes. Télécharger par `fetch` avec Bearer puis `blob`.

## Règles de contrôle croisé

Paramétrage (lecture `KYC:READ`, création, modification et suppression `KYC:MANAGE`) : `GET/POST /sni/api/v1/kyc/cross-validation-rules`, `GET/PUT/DELETE /sni/api/v1/kyc/cross-validation-rules/{id}` ([routes](routes/KycCrossValidationRuleController.md)). `{id}` est un code identifiant (texte, voir le README racine).

- Corps : `{ "documentTypeCode1": "CNI", "documentTypeCode2": "PASSEPORT", "fieldToCompare": "last_name", "blocking": true, "active": true }`. Codes mis en majuscules, champ en minuscules.
- Champs comparables : `first_name`/`firstname`, `last_name`/`lastname`/`name`, `date_of_birth`/`dob`, `expiry_date`, `document_number`, `nationality`. Toute autre valeur est acceptée à l'enregistrement mais ne compare rien.
- À l'approbation manuelle d'un dossier, pour chaque règle active : si les deux types de pièces sont présents, que l'OCR a lu le champ sur les deux et que les valeurs diffèrent (sans tenir compte de la casse), une règle `blocking` refuse l'approbation (`400` « KYC cross-validation failed for field last_name »). Sans OCR, aucune règle ne bloque.
- `DELETE` supprime réellement la règle ; `PUT` exige tous les champs obligatoires.
- `GET` sans `activeOnly` renvoie toutes les règles.
