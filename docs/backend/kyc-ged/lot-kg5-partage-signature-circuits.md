# Lot KG5 : partage, signature, circuits, permissions

> Partie du [plan KYC et GED](README.md). Fait le 17/11/2026 sur `feature/kyc-ged`.

## Partage interne seulement

- `/shares/**` retiré des chemins publics (`PublicPaths`) : un lien exige la connexion, `DOCUMENT:READ` et les droits du compte sur le document ou le dossier visé (dossiers restreints compris, `GedAccessService`).
- `GET /shares/{token}/file` sert le fichier d'un lien de document (droit de téléchargement, `allowDownload`, expiration et compteur respectés ; l'accès est compté).
- Mot de passe dans l'en-tête `X-Share-Password` au lieu de l'URL ; mauvais mot de passe en `403` (plus `401`, qui déclenchait le rafraîchissement du jeton).
- Révocation (`DOCUMENT:WRITE`) par l'auteur du lien ou un administrateur seulement.
- `createdByEmail` remplacé par `createdByName` ; un auteur inconnu ne fait plus échouer la réponse.

## Permissions par document retirées

`DocumentPermissionController` et `DocumentPermissionService` supprimés : l'accès se règle par les dossiers restreints de l'explorateur. La table `document_permission` reste, utilisée par l'explorateur pour les droits des dossiers.

## Circuits d'approbation réels

- Démarrage refusé : circuit inactif, d'un autre espace ou d'un autre type de document, document déjà tranché.
- Approbateur contrôlé à chaque étape (`USER` : courriel ou identifiant du compte ; `ROLE` : rôle avec ou sans `ROLE_`). Règle d'URL `/documents/{code}/workflow/**` ramenée à `DOCUMENT:READ` : c'est le circuit qui désigne.
- Dernière approbation : le document est approuvé par l'approbation normale (`DocumentService.approve` : trace de revue, KYC, événements). Refus (motif obligatoire) : le document est refusé par `DocumentService.reject`. La garde d'espace (`DocumentReviewGuard`) laisse passer cette décision, déjà contrôlée par le circuit (`DocumentReviewGuard.asWorkflow`).
- `autoApproveDays` : `DocumentWorkflowService.autoApproveDueSteps`, appelé par `DocumentLifecycleWorker` (horaire), approuve l'étape dont le délai court depuis la dernière action ou le démarrage ; une transaction par instance.
- `GET /documents/{code}/workflow/status` lit la dernière instance (`findFirstByDocumentOrderByStartedAtDesc`), plus d'erreur 500 au deuxième circuit.

## Signature

- Demande refusée sur un document `REJECTED`, `ARCHIVED`, `EXPIRED` ou `SUPERSEDED`.
- La dernière signature attendue passe le document `SIGNED` et publie `DOCUMENT_FULLY_SIGNED`.
- Signataire connecté et verrou : déjà faits aux lots KG0 et KG4.

## Écarts réglés

G3, G5, G6, G10, G-n5, G-n9 ; G2 et G12 étaient déjà réglés au lot KG0 (marqués dans le plan).

## Tests

`DocumentKg5Test` (circuit inactif ou d'un autre espace, approbateur contrôlé, étape intermédiaire, dernière approbation, refus motivé, dernière instance, signature sur document refusé, dernière signature, révocation par l'auteur) ; `PublicPathsTest` et `KycGedRightsKg0Test` mis à jour.
