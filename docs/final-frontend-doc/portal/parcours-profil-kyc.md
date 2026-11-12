# portal : accueil, profil, sessions, notifications, KYC

> Partie du [README du module portal](README.md). Routes exactes et modèles : [ClientProfileController](routes/ClientProfileController.md), [ClientSessionController](routes/ClientSessionController.md), [ClientNotificationController](routes/ClientNotificationController.md), [ClientKycController](routes/ClientKycController.md). Règles communes : [README racine](../README.md).

Code : `src/main/java/com/sni/bokaticowork/features/portal/profile/`, `session/`, `notification/`, `document/kyc/`.

## Parcours

### Écran d'accueil et barrière KYC

1. `GET /sni/api/v1/client/me/onboarding-status` ([détail](routes/ClientProfileController.md#r-get-sni-api-v1-client-me-onboarding-status)). Toujours accessible, même quand la barrière KYC bloque le reste. Champs utiles : `emailVerified`, `kycStatus`, `kycCompletionPercent`, `gracePeriodActive`, `gracePeriodEndsAt`, `gracePeriodDaysRemaining`, `portalFullyActive`, `nextStep`, `restrictedActions`. Correspondances : [README.md#statut-daccueil](README.md#statut-daccueil).
2. Selon `nextStep` :
   - `VERIFY_EMAIL` : afficher « vérifiez votre adresse » (la vérification relève du module `securite`, voir [../securite/README.md](../securite/README.md)) ;
   - `SUBMIT_KYC`, `COMPLETE_KYC`, `CORRECT_KYC` : bouton vers l'écran KYC ;
   - `AWAIT_KYC_REVIEW` : message d'attente, pas d'action ;
   - `COMPLETED` : rien.
3. Si `kycStatus != "APPROVED"` et `gracePeriodActive = false`, ne proposer que l'écran KYC et la déconnexion : les autres routes `/client/**` répondent `403 KYC_REQUIRED`.
4. Si `gracePeriodActive = true`, afficher un compte à rebours (`gracePeriodDaysRemaining`) : à la fin, le portail se ferme jusqu'à l'approbation.

Sur tout `403` dont `errorCode` vaut `KYC_REQUIRED`, rediriger vers l'écran KYC et afficher `message` (déjà en français, il nomme l'étape) et, s'il existe, `errors` (noms des pièces refusées).

### Consulter et modifier son profil

1. `GET /sni/api/v1/client/me` ou `GET /sni/api/v1/client/profile` (mêmes données, [détail](routes/ClientProfileController.md#r-get-sni-api-v1-client-me)) : `memberId`, `firstname`, `lastname`, `displayName`, `email`, `phone`, `whatsappPhone`, `status`, `avatarUrl`, `portalActivatedAt`.
2. `PUT /sni/api/v1/client/profile` avec `{ "firstname", "lastname", "phone", "whatsappPhone" }` (chacun 350 caractères au plus pour les noms, 30 pour les téléphones). Un champ `null` ou absent est laissé inchangé (mise à jour MapStruct en `IGNORE`, `src/main/java/com/sni/bokaticowork/features/client/member/mapper/interfaces/MemberMapper.java:27`). Réponse : le profil à jour.
3. L'adresse électronique et l'avatar ne se modifient pas depuis le portail (aucune route).

Erreurs : `400` « A valid phone number is required » si le téléphone est invalide (`MemberServiceImpl.validateUpdateRequest`). Un changement de téléphone est signalé à la conformité (effet invisible pour le membre).

### Changer son mot de passe

1. `POST /sni/api/v1/client/profile/change-password`, **sans corps**. Le serveur déclenche la procédure de réinitialisation par courriel sur l'adresse du membre (`AuthenticationService.resetPassword`). Réponse `204`.
2. Afficher « un courriel vous a été envoyé ». La suite (lien, nouveau mot de passe) est décrite dans [../securite/README.md](../securite/README.md).

### Gérer ses sessions

1. `GET /sni/api/v1/client/sessions` : une ligne par jeton de rafraîchissement non révoqué : `id`, `expiresAt`, `active` (non révoqué et non expiré). Aucune information d'appareil ni d'adresse IP n'est rendue.
2. `DELETE /sni/api/v1/client/sessions/{sessionId}` : révoque ce jeton et invalide la session associée. `404` « Session not found » si l'`id` n'est pas au membre. **`id` est un grand entier généré** : le garder en texte (lecture JSON sans perte), sinon l'appel vise un autre nombre et répond `404`.
3. `DELETE /sni/api/v1/client/sessions` : révoque toutes les sessions **y compris la session courante** (les jetons d'accès déjà émis cessent aussi d'être acceptés). Après `204`, renvoyer vers la connexion.

### Notifications

1. Badge : `GET /sni/api/v1/client/notifications/unread-count` renvoie `{ "count": n }`.
2. Liste : `GET /sni/api/v1/client/notifications?limit=20` renvoie **seulement les non lues** du canal `IN_APP`, au plus 50 (`limit` borné entre 1 et 50). Chaque élément : `notificationNumber`, `title` (sujet), `message`, `createdAt`, `read`. Il n'existe aucune route pour relire les notifications déjà lues.
3. Marquer une notification lue : `PATCH /sni/api/v1/client/notifications/{notificationNumber}/read` (`204`). Une notification d'un autre destinataire est refusée avant toute écriture.
4. Tout marquer lu : `PATCH` ou `POST /sni/api/v1/client/notifications/read-all` (`204`).

`message` est composé par le serveur pour les types `BOOKING_*`, `SUBSCRIPTION_*`, `CONTRACT_SIGNING_REQUESTED`, `CONTRACT_SIGNED_VIA_ESIGN`, `BILLING_DOCUMENT_ISSUED` (`src/main/java/com/sni/bokaticowork/features/portal/notification/service/ClientNotificationFormatter.java:37`). Pour tout autre type (relances `BILLING_DUNNING_*`, `COMPANY_SPEND_APPROVAL_REQUESTED`, `COMPANY_SPEND_DECIDED`, `COMPANY_WALLET_SPEND`, marketplace), `message` est **vide** : afficher `title` seul.

**Temps réel** : une notification `IN_APP` est poussée sur la destination utilisateur `/user/queue/notifications` (contenu : `notificationNumber`, `subject`, `eventType`, `aggregateType`, `aggregateId`, `payloadJson`, `createdAt`, `read`), puis `{ "action": "INCREMENT" }` sur `/user/queue/notifications/unread-count` (`src/main/java/com/sni/bokaticowork/features/notification/consumer/NotificationInAppConsumer.java:45-55`). La destination est rattachée à l'adresse électronique du compte authentifié sur la trame `CONNECT`. Le message poussé n'a **pas** le même format que la liste REST (pas de `message` composé) : à réception, incrémenter le badge et recharger la liste plutôt que d'afficher le contenu brut. L'envoi est « au mieux » : à la reconnexion, relire la liste.

### Déposer son dossier KYC

Toutes ces routes existent sous deux préfixes identiques : `/sni/api/v1/client/documents/kyc` et `/sni/api/v1/portal/kyc`. Elles restent accessibles quand la barrière KYC bloque le reste du portail.

1. **État du dossier** : `GET /sni/api/v1/client/documents/kyc/my-case` (ou `GET /sni/api/v1/client/documents/kyc`, [détail](routes/ClientKycController.md#r-get-sni-api-v1-client-documents-kyc-my-case)) : `caseCode`, `status` (statut brut du dossier, voir [README.md#dossier-kyc-vu-du-membre](README.md#dossier-kyc-vu-du-membre)), `completionPercent`, `complete`, `approved`, `decisionComment` (motif du back-office), `missingDocumentTypeCodes`, `requirements`, `documents`. `404` « No KYC case found... » si le membre n'a pas de dossier.
2. **Pièces exigées** : `GET .../requirements` : une ligne par type de pièce exigible pour un membre : `documentTypeCode`, `documentTypeName`, `required`, `requiresBackSide`, `status` (`PENDING`, `VERIFIED`, `REJECTED`, `EXPIRED`, ou nul si rien de déposé), `documentCode`, `backDocumentCode`. Liste **vide** quand le dossier est `APPROVED`, `SUBMITTED` ou `UNDER_REVIEW` (plus rien à fournir).
3. **Avancement** : `GET .../completion` : `completionPercent` (pièces exigées **vérifiées** sur pièces exigées), `totalRequired`, `totalSubmitted`, `totalVerified`, `missingDocumentTypeCodes`, `requirements`.
4. **Règles de dépôt d'un type** : `GET /sni/api/v1/document-types/{documentTypeCode}/upload-config` (module `document`, ouvert à tout compte connecté) : formats acceptés, taille maximale, recto verso. Voir [../document/README.md](../document/README.md).
5. **Déposer une pièce** : `POST .../documents` (ou `.../documents/upload`) en `multipart/form-data` ([détail](routes/ClientKycController.md#r-post-sni-api-v1-client-documents-kyc-documents)) :
   - `file` (obligatoire), `documentType` (code du type, obligatoire) ;
   - `documentNumber`, `issueDate`, `expiryDate` (AAAA-MM-JJ) : obligatoires si le type l'exige (`requiresDocumentNumber`, `requiresIssueDate`, `requiresExpiryDate`) ;
   - `side` : `FRONT` (défaut) ou `BACK`. Le verso exige un recto déjà déposé pour ce type ; il est rattaché au recto (`backDocumentCode`) et ne crée pas de nouvelle pièce.
   - Un nouveau recto pour un type déjà déposé **remplace** la pièce existante (même `id`, statut remis à `PENDING`).
   - Réponse `201` avec la pièce (`id`, `documentCode`, `status`, `requiresBackSide`...). Un envoi en JSON répond `400` (« Document upload requires Content-Type: multipart/form-data... »).
6. **Remplacer le fichier** d'une pièce `PENDING` ou `REJECTED` : `PUT .../documents/{id}` en `multipart/form-data`, partie `file`. Statut remis à `PENDING`.
7. **Supprimer** une pièce `PENDING` : `DELETE .../documents/{id}` (`204`).
8. **Lire** : `GET .../documents` (toutes les pièces du dossier), `GET .../documents/{id}`, `GET .../documents/{id}/download` (octets, nom dans `Content-Disposition`).
9. **Soumettre** : `POST .../submit`, accepté seulement si le dossier est `IN_PROGRESS` ou `PENDING_CORRECTION`. Réponse : le dossier. En pratique, le dépôt de la dernière pièce exigée fait déjà passer le dossier en `UNDER_REVIEW` automatiquement ; relire `my-case` après chaque dépôt et ne proposer « Soumettre » que si `status` vaut encore `IN_PROGRESS` ou `PENDING_CORRECTION`.
10. Après décision du back-office : `APPROVED` lève la barrière (relire `onboarding-status`), `PENDING_CORRECTION` remet les pièces refusées (`status = REJECTED`) à redéposer, avec le motif dans `decisionComment`.

`id` d'une pièce est son numéro court (`short_id`), pas l'identifiant généré : petit entier, mais le garder en texte par prudence.

## Règles et erreurs à refléter dans l'écran

| Cas | Statut | Message |
| --- | --- | --- |
| Type de pièce inconnu | `400` | `Type de document inconnu : <code>` |
| Fichier absent ou vide | `400` | `Le fichier est requis` |
| Fichier trop lourd (limite du type, sinon 10 Mo) | `400` | `Taille du fichier (... Ko) dépasse la limite autorisée de ... Mo` |
| Format refusé | `400` | `Type de fichier non autorisé. Acceptés : ...` |
| Numéro, date d'émission ou d'expiration manquants | `400` | `Le numéro de document est obligatoire pour le type : ...` (et variantes) |
| Dépôt alors que le dossier est validé ou en examen | `400` | `KYC case is not open for document uploads. Current status: ...` |
| Verso avant recto | `400` | `Front side must be uploaded before the back side for document type: ...` |
| Remplacement d'une pièce vérifiée | `400` | `Only PENDING or REJECTED documents can be resubmitted` |
| Suppression d'une pièce non `PENDING` | `400` | `Only PENDING documents can be deleted` |
| Soumission hors `IN_PROGRESS`/`PENDING_CORRECTION` | `400` | `Your KYC case is not in a submittable state. Current status: ...` |
| Pièce d'un autre dossier | `404` | `KYC document not found` |

## À prévoir côté frontend

- Un écran KYC autonome, utilisable même quand le reste du portail est fermé : liste des pièces exigées (avec recto et verso), dépôt, remplacement, suppression, soumission, motif de refus.
- Charger `upload-config` par type pour filtrer les fichiers avant envoi (format, taille) et savoir s'il faut un verso.
- Désactiver les boutons de dépôt quand `requirements` est vide ou que `status` vaut `SUBMITTED`, `UNDER_REVIEW` ou `APPROVED`.
- Le pourcentage de `onboarding-status` (fixe par étape : 0, 30, 40, 70, 100) diffère de celui de `my-case`/`completion` (pièces vérifiées) : en choisir un seul par écran.
- Abonnement STOMP aux deux destinations de notification après la connexion ; recharger la liste à chaque message.
- Après `DELETE /sni/api/v1/client/sessions`, effacer les jetons locaux.
