# document : revue, signatures et circuits d'approbation

> Partie du [README du module](README.md). Routes exactes : [routes/DocumentReviewQueueController.md](routes/DocumentReviewQueueController.md), [routes/DocumentController.md](routes/DocumentController.md), [routes/DocumentSignatureController.md](routes/DocumentSignatureController.md), [routes/DocumentWorkflowController.md](routes/DocumentWorkflowController.md).

Depuis le lot KG0 (16/11/2026), l'auteur d'une décision est **toujours le compte connecté** : `reviewedBy` n'est plus obligatoire et il est ignoré s'il est envoyé (les exemples ci-dessous peuvent l'omettre). Une pièce KYC se décide avec `KYC:APPROVE` ou `KYC:REJECT` ; un autre document avec `DOCUMENT:REVIEW` ou le droit de revue de son espace. Les décisions en masse traitent chaque document à part (résultat partiel exact) et refusent un document hors de l'espace de la route. Les files ne montrent que les espaces permis et les documents des dossiers visibles.

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
2. Demander : `POST /sni/api/v1/documents/{documentCode}/signatures` ([route](routes/DocumentSignatureController.md#r-post-sni-api-v1-documents-documentcode-signatures)), droit `DOCUMENT:UPLOAD`. Corps : `{ "signerType": "USER", "signerId": …, "signerName": "…", "signerEmail": "…" }` (`signerId` int64 obligatoire ; pour qu'un compte de l'équipe puisse signer, `signerType = USER` avec son identifiant, ou son courriel dans `signerEmail`). Réponse `201`, courriel au signataire si `signerEmail` est rempli. `400` sur un document `REJECTED`, `ARCHIVED`, `EXPIRED` ou `SUPERSEDED` (lot KG5).
3. Signer : `PATCH /sni/api/v1/documents/{documentCode}/signatures/{signatureId}/sign` ([route](routes/DocumentSignatureController.md#r-patch-sni-api-v1-documents-documentcode-signatures-signatureid-sign)), droit `DOCUMENT:READ`, **seulement par le signataire désigné, avec son propre compte** (`403` sinon, administrateur compris). Corps : `{ "accepted": true, "signatureData": "…" }` (`accepted` doit valoir `true`). Le serveur note l'heure, l'adresse IP et le navigateur. `400` « Cette signature n'est plus en attente » si déjà signée ou refusée ; `409` si le document est verrouillé par un autre compte.
4. **La dernière signature attendue passe le document `SIGNED`** (lot KG5) et publie `DOCUMENT_FULLY_SIGNED`. Recharger le document après la signature. Le portail membre signe ses contrats par une autre route (module `portal`).

## Circuits d'approbation

Un circuit fait passer un document par des approbateurs successifs ; sa dernière approbation approuve le document, un refus le refuse (lot KG5).

1. Créer : `POST /sni/api/v1/document-workflows` ([route](routes/DocumentWorkflowController.md#r-post-sni-api-v1-document-workflows)), droit `SYSTEM:SETTINGS`. Corps : `name` (obligatoire), `description`, `workflowType` (texte, `SIMPLE` par défaut), `space`, `documentTypeCode`, `steps[]` (au moins une) avec `stepName`, `approverType` (`USER` ou `ROLE`), `approverValue` (pour `USER` : courriel ou identifiant du compte ; pour `ROLE` : nom du rôle, `MANAGER` ou `ROLE_MANAGER`), `required`, `autoApproveDays` (approbation automatique de l'étape après ce nombre de jours sans décision, par la tâche horaire des documents). L'ordre de la liste fixe `stepOrder`. Réponse `201` avec `code`.
2. Lister : `GET /sni/api/v1/document-workflows?activeOnly=true` ; détail `GET /document-workflows/{code}` ; désactiver `DELETE /document-workflows/{code}`.
3. Lancer sur un document : `POST /sni/api/v1/documents/{documentCode}/start-workflow?workflowCode=WF…` ([route](routes/DocumentWorkflowController.md#r-post-sni-api-v1-documents-documentcode-start-workflow)), droit `DOCUMENT:UPLOAD`, sans corps. Refusé (`400`) : circuit désactivé, circuit d'un autre espace ou d'un autre type de document que le document, document déjà tranché (`APPROVED`, `SIGNED`, `REJECTED`, `ARCHIVED`, `EXPIRED`, `SUPERSEDED`), circuit déjà en cours. Réponse `201` `WorkflowInstanceResponse` (`currentStep = 1`, `status = IN_PROGRESS`). Proposer dans la liste les circuits actifs dont `space` et `documentTypeCode` sont vides ou égaux à ceux du document.
4. Agir : `POST /sni/api/v1/documents/{documentCode}/workflow/approve` ou `/workflow/reject`, droit `DOCUMENT:READ`, **seulement par l'approbateur de l'étape courante** (`403` « Cette étape (…) revient à … » sinon). Corps `{ "comment": "…" }`, **obligatoire pour un refus**. Approuver fait avancer d'une étape ; la dernière approbation termine en `APPROVED` et approuve le document (trace de revue, KYC, événements, comme une approbation normale). Refuser termine en `REJECTED` et refuse le document avec le commentaire pour motif. La décision sur le document ne s'applique que s'il est encore en attente (`UPLOADED`, `PENDING_REVIEW`, `NEEDS_CORRECTION`). `400` « Aucun circuit d'approbation en cours pour ce document ».
5. Suivre : `GET /sni/api/v1/documents/{documentCode}/workflow/status` : **la dernière instance** du document (en cours ou terminée), avec `actions[]` (étape, action, auteur, courriel, date, commentaire ; auteur nul pour une approbation automatique) ; `GET /sni/api/v1/document-workflows/my-pending` : instances dont l'étape courante désigne le courriel ou un rôle du compte connecté (la boîte de l'approbateur).
