# Lot KG0 : droits et auteurs

Branche `feature/kyc-ged`. Migrations : dev `V367__kyc_ged_kg0_droits.sql`, prod `V361__kyc_ged_kg0_droits.sql`.

## Ce qui change

- **Nouveau droit `KYC:MANAGE`** (`KYC_MANAGE`), donné aux rôles qui ont `KYC_APPROVE` et aux administrateurs (`ADMIN`, `SUPER_ADMIN`).
- **Règles d'URL KYC** (`AdminApiAuthorizationManager`) : toute lecture `KYC:READ` (les listes `/approved`, `/rejected` et leurs alias comprises) ; décisions sur **suffixe exact** (`/approve`, `/bulk-approve` : `KYC:APPROVE` ; `/reject`, `/bulk-reject`, `/bulk-disapprove`, `/bulk-desapprove`, `/request-correction` : `KYC:REJECT`) ; toute autre écriture (création, soumission, assignation, risque, notes, exigences, dépôt par l'équipe, détails d'une pièce, règles de contrôle croisé) `KYC:MANAGE`.
- **Règles d'URL GED** : décisions `/documents/{code}/approve|reject|request-correction` ouvertes à `DOCUMENT:REVIEW` **ou** à un droit de revue d'espace (exigence générique `DOCUMENT:REVIEW*`) ; `PATCH`, `PUT`, `DELETE` sous `/documents` : `DOCUMENT:WRITE` ; signature : `DOCUMENT:READ` (le service vérifie le signataire) ; règles dédiées pour `/document-folders`, `/document-tags` (lecture / écriture), `/document-workflows` (lecture / `SYSTEM:SETTINGS`), `/document-permissions` (`SYSTEM:SETTINGS`), `/spaces` (lecture / dépôt / écriture), `/review` (revue d'au moins un espace).
- **Droit de décider contrôlé par le service** (`DocumentReviewGuard`, appelé par `DocumentServiceImpl.approve`, `reject`, `requestCorrection`) : pièce de l'espace KYC → `KYC:APPROVE` ou `KYC:REJECT` (le droit de revue documentaire ne fait plus aboutir un dossier KYC) ; autre document → `DOCUMENT:REVIEW` ou le droit de revue de son espace. Une tâche du système n'est pas contrôlée ; le super administrateur passe.
- **Auteur = compte connecté** : `reviewedBy` (décisions unitaires et en masse, corrections, risque) et `authorId` (notes KYC) ne sont plus obligatoires et sont ignorés pour un compte connecté ; « ma file » est celle du compte connecté.
- **File de revue** : limitée aux espaces que le compte peut revoir ; documents des dossiers restreints masqués.
- **Opérations en masse** : chaque document dans sa propre transaction ; document hors de l'espace de la route refusé.
- **Dossiers restreints respectés** : dépôt dans un dossier (droit d'écriture sur le dossier), étiquettes et métadonnées (lecture / écriture), signatures, partages, journaux d'accès, décisions. `DocumentResponse.folderCode` et `folderName` remplis par le mapper (ils ne l'étaient jamais : le masquage des listes ne marchait pas).
- **Signature** : seul le signataire désigné (même compte ou même courriel) signe.

## Écarts réglés

K1, K10, K-n1 ; G1 (transactions et espace), G2, G3 (droits et signataire ; statut `SIGNED` au lot KG5), G12, G-n1, G-n4.

## Tests

`KycGedRightsKg0Test` (règles d'URL KYC et GED, droits d'espace) ; `DocumentReviewGuardKg0Test` (pièce KYC, espace, tâche du système) ; tests existants des autorisations, des droits semés, des documents et du KYC.
