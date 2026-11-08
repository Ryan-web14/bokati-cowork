# document : revue, signatures et circuits d'approbation

> Partie du [README du module](README.md). Routes exactes : [routes/DocumentReviewQueueController.md](routes/DocumentReviewQueueController.md), [routes/DocumentController.md](routes/DocumentController.md), [routes/DocumentSignatureController.md](routes/DocumentSignatureController.md), [routes/DocumentWorkflowController.md](routes/DocumentWorkflowController.md).

Toutes les décisions exigent `reviewedBy` : l'identifiant numérique (int64, gardé en texte) du compte qui décide. Le serveur ne le compare pas au compte connecté ; envoyer celui de l'utilisateur connecté (voir [README](README.md#identifiants-numériques-à-manipuler-sans-perte)).

## File de revue

1. File globale : `GET /sni/api/v1/review/queue?page=0&size=20` ([route](routes/DocumentReviewQueueController.md#r-get-sni-api-v1-review-queue)), droit `DOCUMENT:REVIEW`. Renvoie les documents `PENDING_REVIEW` de tous les espaces, en `PaginatedResponse<DocumentResponse>`.
2. File d'un espace : `GET /sni/api/v1/review/{space}` ([route](routes/DocumentReviewQueueController.md#r-get-sni-api-v1-review-space)) avec `space` parmi `KYC_SPACE`, `CONTRACT_SPACE`, `FINANCIAL_SPACE`, `ASSET_SPACE`, `ADMINISTRATIVE`, `GENERIC` (majuscules exactes). Droit effectif : `DOCUMENT:REVIEW` (les droits `DOCUMENT:REVIEW_<ESPACE>` prévus par le code ne suffisent pas, voir écarts).
3. Aucun tri par défaut n'est imposé : passer `sort=uploadedAt,ASC` pour traiter les plus anciens d'abord.
4. Chaque ligne : `code`, `title`, `documentTypeName`, `ownerType`, `ownerId`, `uploadedAt`, `mimeType`, `previewUrl`. Afficher l'aperçu selon [la méthode décrite](parcours-televersement-et-apercus.md#afficher-un-aperçu-dans-le-navigateur).
5. Les documents `NEEDS_CORRECTION` ne figurent pas dans ces files : les retrouver par `GET /sni/api/v1/documents/search?statuses=NEEDS_CORRECTION`.

## Approuver un document

`POST /sni/api/v1/documents/{code}/approve` ([route](routes/DocumentController.md#r-post-sni-api-v1-documents-code-approve)), droit `DOCUMENT:REVIEW`, `Idempotency-Key` conseillé.

1. Corps : `{ "reviewedBy": "…", "comment": "…" }` (`comment` facultatif).
2. Statut de départ admis : `PENDING_REVIEW`, `NEEDS_CORRECTION`, `REJECTED` (revenir sur un refus).
3. Effets : statut `APPROVED`, ligne d'historique, pièce KYC liée `VERIFIED`, recalcul du dossier KYC en tâche de fond (qui peut **approuver le dossier et activer le membre** si c'était la dernière pièce exigée).
4. Si le type a `requiresSignature = true`, une demande de signature `PENDING` est créée au nom du propriétaire (sans nom ni courriel de signataire).
5. Si le document était `REJECTED` : événement `DOCUMENT_REJECTION_LIFTED`. Les annulations faites par le refus (contrat, abonnements, pass) **ne sont pas rétablies** : l'écran doit le dire à l'utilisateur.
6. Réponse `200` `DocumentResponse`.

## Refuser un document

`POST /sni/api/v1/documents/{code}/reject` ([route](routes/DocumentController.md#r-post-sni-api-v1-documents-code-reject)), droit `DOCUMENT:REVIEW`, `Idempotency-Key` conseillé.

1. Corps : `{ "reviewedBy": "…", "rejectionReasonCode": "…", "rejectionReasonDetail": "…", "comment": "…" }`. Seul `reviewedBy` est obligatoire ; l'écran devrait exiger au moins un motif (le motif retenu pour les annulations est `rejectionReasonDetail`, sinon `rejectionReasonCode`, sinon `comment`). Aucune liste de codes de motif n'existe côté serveur : texte libre.
2. Statut de départ admis : `PENDING_REVIEW`, `NEEDS_CORRECTION`. Un document déjà `REJECTED` : `400`.
3. Effets : statut `REJECTED`, pièce KYC liée `REJECTED`, dossier KYC en `PENDING_CORRECTION` et membre en `PENDING_CORRECTION` (en tâche de fond), puis cascade ci-dessous.
4. Réponse `200` `DocumentResponse`.

### Refus et annulations en cascade

`DocumentRejectionCascadeService` agit dès qu'un document est refusé (unitaire ou en masse) :

1. Toutes ses demandes de signature `PENDING` passent `DECLINED`.
2. Tout contrat dont il est le brouillon ou le document signé, ou dont il est la propriété (`ownerType = CONTRACT`), et qui n'est pas déjà `CANCELLED`, `TERMINATED` ou `EXPIRED`, passe `CANCELLED` (motif « Document rejected: DOC… - motif »).
3. Pour chacun de ces contrats : abonnements liés `CANCELLED` (droits d'usage actifs annulés), pass liés annulés, options d'abonnement liées `CANCELLED`.
4. Les paiements ne sont pas touchés (`paymentPolicy = PAYMENTS_UNCHANGED_REFUND_REQUIRED` dans les événements) : un remboursement éventuel reste à faire à la main.

À prévoir : avant d'envoyer un refus sur un document d'espace `CONTRACT_SPACE` ou de propriétaire `CONTRACT`, afficher une confirmation explicite qui annonce l'annulation du contrat et de ses abonnements. Aucune route ne prévisualise la cascade.

## Demander une correction

`POST /sni/api/v1/documents/{code}/request-correction` ([route](routes/DocumentController.md#r-post-sni-api-v1-documents-code-request-correction)), droit `DOCUMENT:REVIEW`.

1. Corps : `{ "reviewedBy": "…", "correctionNote": "Le document est flou", "comment": "…", "deadlineDays": 7 }`. `correctionNote` obligatoire, `deadlineDays` ≥ 1 (7 par défaut).
2. Effets : statut `NEEDS_CORRECTION`, échéance = maintenant + `deadlineDays`, compteur de corrections incrémenté, pièce KYC liée remise `PENDING`, événement `DOCUMENT_CORRECTION_REQUESTED`.
3. La note et l'échéance ne sont pas renvoyées par `DocumentResponse` : les afficher à partir de ce que l'écran vient d'envoyer. Aucun automatisme n'agit à l'échéance.
4. Pour une pièce KYC, préférer la route du dossier ([parcours-kyc.md](parcours-kyc.md#refuser-ou-faire-corriger-des-pièces)).

## Décisions en masse

Routes `POST /sni/api/v1/review/{space}/bulk-approve`, `/bulk-reject`, `/bulk-request-correction` ([routes](routes/DocumentReviewQueueController.md)), droit `DOCUMENT:REVIEW`.

1. Corps :
   - approbation : `{ "codes": ["DOC…", "DOC…"], "reviewedBy": "…", "comment": "…" }` ;
   - refus : `{ "codes": […], "reviewedBy": "…", "rejectionReasonCode": "…", "rejectionReasonDetail": "…", "comment": "…" }` ;
   - correction : `{ "codes": […], "reviewedBy": "…", "correctionNote": "…", "deadlineDays": 7 }`.
2. `{space}` n'est qu'un contrôle d'accès : les codes ne sont pas filtrés par espace.
3. Réponse `200` `DocumentBulkActionResponse` : `total`, `succeeded`, `failed`, `results[]` (`code`, `status` `SUCCESS` ou `FAILED`, `reason`).
4. Prudence : toutes les décisions d'un lot partagent une transaction ; si l'une échoue (document déjà approuvé…), la réponse risque de finir en erreur et rien n'être enregistré (voir écarts du README). Côté écran, ne proposer en masse que des documents encore `PENDING_REVIEW` et, en cas d'erreur `500`, recharger la file avant de réessayer.

## Signatures

1. Lister : `GET /sni/api/v1/documents/{documentCode}/signatures` ([route](routes/DocumentSignatureController.md#r-get-sni-api-v1-documents-documentcode-signatures)), droit `DOCUMENT:READ`. Chaque signature : `id` (int64 ⚠), `signerType`, `signerId`, `signerName`, `signerEmail`, `signatureStatus`, `signedAt`, `ipAddress`, `userAgent`.
2. Demander : `POST /sni/api/v1/documents/{documentCode}/signatures` ([route](routes/DocumentSignatureController.md#r-post-sni-api-v1-documents-documentcode-signatures)), droit `DOCUMENT:UPLOAD`. Corps : `{ "signerType": "MEMBER", "signerId": "…", "signerName": "…", "signerEmail": "…" }` (`signerType` texte libre mis en majuscules, `signerId` int64 obligatoire, `signerEmail` facultatif mais nécessaire pour le courriel). Réponse `201`. Un courriel part au signataire si `signerEmail` est rempli.
3. Signer : `PATCH /sni/api/v1/documents/{documentCode}/signatures/{signatureId}/sign` ([route](routes/DocumentSignatureController.md#r-patch-sni-api-v1-documents-documentcode-signatures-signatureid-sign)), droit `DOCUMENT:READ`. Corps : `{ "accepted": true, "signatureData": "…" }` (`accepted` doit valoir `true`, sinon `400` ; `signatureData` libre, par exemple une image en base64). Le serveur note l'heure, l'adresse IP et le navigateur. `400` « Document signature is not pending » si déjà signée ou refusée.
4. La signature ne change pas le statut du document. Le portail membre signe ses contrats par une autre route (module `portal`).

## Circuits d'approbation

Les circuits enregistrent une suite d'approbations sans effet sur le statut du document ni contrôle de l'approbateur (voir écarts). À n'utiliser que comme traçabilité.

1. Créer : `POST /sni/api/v1/document-workflows` ([route](routes/DocumentWorkflowController.md#r-post-sni-api-v1-document-workflows)), droit `ADMIN:ACCESS`. Corps : `name` (obligatoire), `description`, `workflowType` (texte, `SIMPLE` par défaut), `space`, `documentTypeCode`, `steps[]` (au moins une) avec `stepName`, `approverType` (`USER` ou `ROLE`), `approverValue` (courriel du compte pour `USER` ; nom d'autorité tel que `ROLE_ADMIN` ou `DOCUMENT:REVIEW` pour `ROLE`), `required`, `autoApproveDays` (sans effet). L'ordre de la liste fixe `stepOrder`. Réponse `201` avec `code`.
2. Lister : `GET /sni/api/v1/document-workflows?activeOnly=true` ; détail `GET /document-workflows/{code}` ; désactiver `DELETE /document-workflows/{code}`.
3. Lancer sur un document : `POST /sni/api/v1/documents/{documentCode}/start-workflow?workflowCode=WF…` ([route](routes/DocumentWorkflowController.md#r-post-sni-api-v1-documents-documentcode-start-workflow)), droit `DOCUMENT:UPLOAD`, sans corps. `400` « Document already has an active workflow ». Réponse `201` `WorkflowInstanceResponse` (`currentStep = 1`, `status = IN_PROGRESS`).
4. Agir : `POST /sni/api/v1/documents/{documentCode}/workflow/approve` ou `/workflow/reject`, droit `DOCUMENT:REVIEW`, corps facultatif `{ "comment": "…" }`. Approuver fait avancer d'une étape, ou termine en `APPROVED` à la dernière ; refuser termine en `REJECTED`. `400` « No active workflow for this document ».
5. Suivre : `GET /sni/api/v1/documents/{documentCode}/workflow/status` (instance avec `actions[]` : étape, action, auteur, courriel, date, commentaire) ; `GET /sni/api/v1/document-workflows/my-pending` (instances dont l'étape courante désigne le courriel ou une autorité du compte connecté).
