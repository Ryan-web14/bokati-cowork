repnon# Audit & plan d'amélioration — Modules KYC et Gestion documentaire

> Dernière mise à jour : 2026-06-08
> Statut : audit + propositions — à prioriser et planifier par phases

---

## Sommaire

1. [Module Gestion documentaire](#1-module-gestion-documentaire-featuresdocumentdocumentmaster)
2. [Module KYC](#2-module-kyc-featuresdocumentkyc)
3. [Constats transverses](#3-constats-transverses)

---

## 1. Module Gestion documentaire (`features/document/documentMaster`)

### 1.1 État actuel

Module mature qui sert de socle de stockage/versioning/revue à l'ensemble de la plateforme (KYC, contrats, factures…) :

- **Entités** : `Document` (propriétaire polymorphe `ownerType`/`ownerId`, `category`, `status`, version courante, checksum SHA-256, soft delete), `DocumentType` (catalogue configurable : `required`, `requiresExpiryDate`, `requiresReview`, `autoApprove`/`autoApproveAfterDays`, `requiresSignature`, `multipleAllowed`, MIME autorisés, taille max), `DocumentRequirement` (exigences par `ownerType` + `documentTypeCode` + `customerType` + `businessLegalForm`), `DocumentVersion` (historique de versions, checksum, statut d'upload/antivirus), `DocumentReview` (trail de revue), `DocumentSignature` (workflow de signature électronique simple).
- **Enums** : `DocumentCategory` (6), `DocumentOwnerType` (8 : CUSTOMER, MEMBER, BUSINESS, CONTRACT, INVOICE, PAYMENT, PROPOSAL, ASSET), `DocumentStatus` (9), `DocumentReviewStatus` (4), `DocumentSignatureStatus` (4), `DocumentVersionUploadStatus` (5), `DocumentAntivirusStatus` (4).
- **API** : upload (multipart, idempotent, audité), remplacement (nouvelle version), consultation/listing paginé, téléchargement/aperçu, historique de versions, approbation/rejet, workflow de signature (demande/liste/signature), CRUD types de documents et exigences.
- **Logique métier** :
  - Abstraction de stockage (filesystem / MinIO / S3) via `DocumentStorageService`.
  - Inspection de sécurité synchrone : détection MIME par signature binaire (magic bytes), calcul de checksum SHA-256, "antivirus" basique recherchant les en-têtes `MZ`/`ELF`.
  - Versioning avec marqueur `current` et bascule automatique de l'ancienne version.
  - Cascade de rejet (`DocumentRejectionCascadeService`) : refuse les signatures en attente et annule en cascade contrats → abonnements/passes/addons liés, avec un libellé de politique de remboursement (`PAYMENTS_UNCHANGED_REFUND_REQUIRED`).
  - `DocumentLifecycleWorker` (toutes les heures) : expiration automatique des documents dont `expiryDate` est dépassée.
  - Hooks de synchronisation KYC : `syncFromDocumentUpload`/`syncFromDocumentReview` appelés à chaque upload/remplacement/décision de revue.

### 1.2 Faiblesses identifiées

| Problème | Référence | Gravité |
|---|---|---|
| L'« antivirus » se limite à une détection de signatures `MZ`/`ELF` — aucun scan réel (ClamAV, service tiers…) ; `DocumentAntivirusStatus.PENDING`/`ERROR` ne sont jamais atteints, tout est `CLEAN` ou exception immédiate au moment de l'upload | `DocumentSecurityService.java:86-91` | Haute |
| `DocumentType.requiresSignature` et `multipleAllowed` sont persistés et exposés en DTO mais **jamais lus** par la logique métier : aucune signature n'est déclenchée automatiquement, et rien n'empêche d'uploader plusieurs documents du même type quand `multipleAllowed = false` | `DocumentType.java:69,73`, `DocumentTypeMapperDecorator.java:44-45` | Moyenne |
| `DocumentRequirement.businessLegalForm` est normalisé et stocké à la création/mise à jour mais **jamais utilisé** dans le filtrage des exigences — seul `customerType` est testé, donc deux entreprises de formes juridiques différentes reçoivent les mêmes exigences | `KycAutomationServiceImpl.java:141-149`, `DocumentRequirementServiceImpl.java:117-118` | Moyenne |
| `DocumentVersionUploadStatus` (`PENDING_SCAN`/`QUARANTINED`/`REJECTED`/`DELETED`) et `DocumentAntivirusStatus.PENDING`/`ERROR` ne sont jamais atteints : l'inspection est synchrone et bloquante, sans pipeline asynchrone de scan ni mise en quarantaine | `DocumentServiceImpl.java:280-296` | Moyenne |
| `DocumentReviewStatus.NEEDS_CORRECTION` n'est jamais assigné — seules les décisions `APPROVED`/`REJECTED` sont enregistrées par `saveReview` | `DocumentServiceImpl.java:383-395` | Basse |
| `DocumentLifecycleWorker` n'envoie aucun rappel avant expiration (contrairement au worker KYC qui dispose de `reminderDays`) — un document `LEGAL`/`FINANCIAL` expire sans préavis ni notification | `DocumentLifecycleAutomationServiceImpl.java:33-50` | Moyenne |
| `DocumentSignatureService` ne notifie jamais les signataires (ni à la création de la demande, ni à la signature/au refus) — aucun email, aucun événement outbox publié | `DocumentSignatureServiceImpl.java` | Moyenne |
| Pas d'endpoint d'archivage/suppression manuelle d'un document ; seul un rejet peut amener un document à un état terminal | `DocumentController.java` | Basse |

### 1.3 Plan d'amélioration (priorisé)

1. **Brancher un vrai moteur antivirus** (ClamAV via socket/REST, ou service tiers type VirusTotal) en mode asynchrone : `upload → PENDING_SCAN → scan → CLEAN/QUARANTINED`, avec notification de l'uploader en cas de blocage.
2. **Appliquer `requiresSignature`** : déclencher automatiquement une `DocumentSignature` lors de l'approbation d'un document dont le type l'exige, et bloquer la finalisation du workflow tant que la signature n'est pas obtenue.
3. **Appliquer `multipleAllowed`** : refuser l'upload d'un second document du même type pour le même propriétaire quand le type ne l'autorise pas.
4. **Exploiter `businessLegalForm`** dans le filtrage des exigences documentaires, à l'image de ce qui existe déjà pour `customerType`.
5. **Notifier les signataires** à chaque étape du workflow de signature (demande créée, rappel avant expiration, signé/refusé), en réutilisant le pattern Outbox déjà en place pour KYC et contrats.
6. **Ajouter des rappels avant expiration** sur `DocumentLifecycleWorker`, symétriques à ceux du worker KYC (`reminderDays`).
7. **Trancher le sort de `NEEDS_CORRECTION`** : soit l'assigner réellement pour distinguer un rejet « à corriger » d'un rejet définitif, soit le retirer de l'énumération.

### 1.4 Nouvelles fonctionnalités proposées

- **Pipeline de scan antivirus asynchrone** avec file d'attente et statut observable (`PENDING_SCAN → CLEAN/QUARANTINED`), exploitant les statuts d'énumération déjà définis mais inutilisés.
- **Aperçu/miniatures** pour les documents image/PDF — `/preview` retourne aujourd'hui le fichier brut sans transformation ni redimensionnement.
- **Annotations positionnelles** sur les documents (ex. « page 2, signature manquante ») pour fluidifier le rejet ciblé.
- **Politique de rétention et purge automatique** par catégorie (RGPD : suppression définitive après délai pour les documents `ARCHIVED`/`EXPIRED`).
- **Tableau de bord documentaire global** (volumes par catégorie/statut, taux de rejet, délais moyens de revue) — miroir du dashboard KYC déjà construit.

---

## 2. Module KYC (`features/document/kyc`)

### 2.1 État actuel

Module très avancé — la quasi-totalité des fonctionnalités listées dans `docs/kyc-ameliorations-features.md` (rédigé courant avril 2026) sont aujourd'hui construites et en production (voir [§3.1](#31-un-document-de-roadmap-existant-est-désormais-largement-obsolète)) :

- **Entités** : `KycCase` (propriétaire polymorphe MEMBER/CUSTOMER/BUSINESS, machine à états à 9 statuts, `riskLevel`, `kycLevel` 1-4, `slaDeadline`, assignation `assignedTo`/`assignedAt`, suivi de relances), `KycDocument` (lié au `Document` maître, statut de vérification + dates), `KycVerification` (trail d'audit des décisions), `KycCaseNote` (notes internes/externes), `KycCrossValidationRule` (règles de cohérence inter-documents), `KycDocumentOcrResult` (extraction OCR + score de confiance).
- **Enums** : `KycCaseStatus` (9 : `NOT_STARTED → IN_PROGRESS → SUBMITTED → UNDER_REVIEW → APPROVED/REJECTED/PENDING_CORRECTION/RENEWAL_REQUIRED/EXPIRED`), `KycDocumentVerificationStatus` (4), `KycRiskLevel` (4 : `LOW/MEDIUM/HIGH/VERY_HIGH`).
- **API** (19 endpoints sur 2 contrôleurs) : cycle de vie complet (créer/soumettre/approuver/rejeter), recherche multi-filtres, dashboard compliance, dossiers expirant bientôt, file personnelle (`my-queue`), assignation + SLA, notes, timeline, statut d'expiration, niveau de risque, export PDF, revue de documents (approbation/rejet en lot, file de revue, documents approuvés/rejetés, résultat OCR).
- **Automatisation** :
  - `KycAutomationServiceImpl` — synchronisation bidirectionnelle Member/Customer ↔ `KycCase`, création automatique de dossier (`ensureCase`), recalcul d'état après upload/revue de document (`recomputeCaseState`).
  - `KycAutomationWorker` — 3 jobs planifiés : surveillance des expirations (lundi 8h, avec rappels à J-60/J-30/J-7), relance des dossiers incomplets (quotidien 9h, jusqu'à `maxReminders`), auto-approbation différée (quotidien 8h, basée sur `autoApproveAfterDays`).
  - `TesseractKycOcrService` — OCR local (Tess4J) déclenché de façon asynchrone à chaque upload/rafraîchissement de document KYC.
- **Notifications** par email via le pattern Outbox (`KycOutboxEventProcessor` / `KycDocumentOutboxEventProcessor`) avec templates Thymeleaf dédiés (`kyc-event.html`, `kyc-expiry-reminder.html`).
- **Idempotence** sur toutes les actions sensibles (`@Idempotent`) et **audit** (`@Audited`).
- **Export PDF** du dossier complet (Thymeleaf → Jsoup → openhtmltopdf), avec ou sans notes internes.

### 2.2 Faiblesses identifiées

| Problème | Référence | Gravité |
|---|---|---|
| L'OCR Tesseract ne renseigne que `extractedDocumentNumber` et `extractedExpiryDate` — les champs `extractedFirstName`/`LastName`/`DateOfBirth`/`Nationality` ne sont **jamais** remplis, ce qui rend les règles de cross-validation portant sur `name`/`date_of_birth`/`nationality` silencieusement inopérantes (valeurs toujours `null`, donc jamais bloquantes) | `TesseractKycOcrService.java:119-127` vs `KycServiceImpl.java:752-775` | Haute |
| `confidenceScore` est toujours fixé à `BigDecimal.ONE` en cas de succès OCR, indépendamment de la qualité réelle de l'extraction ; `app.kyc.ocr.min-confidence-percent` est configuré mais n'est lu nulle part dans le code | `TesseractKycOcrService.java:123`, `KycAutomationProperties.java:36` | Moyenne |
| Aucun endpoint pour gérer les `KycCrossValidationRule` (pas de `KycCrossValidationRuleController`) — les règles de cohérence ne peuvent être créées/modifiées que par insertion SQL directe en base | absence de contrôleur dédié | Moyenne |
| `KycEmailNotifier` est du code mort : entièrement écrit (résolution de destinataire, sujets différenciés par type d'événement, gestion d'erreurs) mais **jamais injecté ni appelé** — la notification réelle passe par `KycOutboxEventProcessor`, qui envoie un template générique unique (`kyc-event`) sans distinguer approbation/rejet/soumission dans le sujet | `KycEmailNotifier.java` vs `KycOutboxEventProcessor.java:34-60` | Moyenne |
| `app.kyc.expiry.grace-period-days` est configuré mais jamais utilisé — un document expiré déclenche immédiatement `PENDING_CORRECTION`/`RENEWAL_REQUIRED`, sans la période de grâce pourtant prévue dans la configuration | `KycAutomationProperties.java:20` | Basse |
| `SYSTEM_REVIEWER_ID = 0L` (worker KYC) et `SYSTEM_UPLOADER_ID = 0L` (service Document) sont deux constantes identiques dupliquées plutôt que centralisées — fragile si l'identifiant 0 venait à être attribué à un vrai utilisateur | `KycAutomationWorker.java:41`, `DocumentServiceImpl.java:62` | Basse |
| `KycController.list`/`expiringSoon`/`myQueue` renvoient des `List` non paginées — un volume important de dossiers chargera l'intégralité en mémoire à chaque appel, contrairement au listing `Document` qui est paginé | `KycController.java:52-65,72-80` vs `DocumentController.java:59-65` | Moyenne |
| `recomputeCaseState` peut faire repasser un dossier `APPROVED` à `IN_PROGRESS`/`UNDER_REVIEW` (ex. nouveau document uploadé après approbation) sans publier d'événement distinct — le générique `KYC_CASE_AUTO_STATUS_SYNC` ne permet pas à la conformité de distinguer une « réouverture » d'une simple synchronisation | `KycAutomationServiceImpl.java:225-256` | Basse |
| Locale d'export PDF et d'email codée en dur (`Locale.FRANCE`) — pas d'i18n, même constat que pour le module Contrat | `KycServiceImpl.java:606`, `KycEmailNotifier.java:40` | Basse |

### 2.3 Plan d'amélioration (priorisé)

1. **Compléter l'extraction OCR** (nom, prénom, date de naissance, nationalité) — ou, à défaut, retirer ces champs et les règles de cross-validation associées tant qu'ils ne peuvent pas être alimentés. En l'état, la fonctionnalité de cohérence documentaire donne une fausse impression de protection AML.
2. **Calculer un véritable score de confiance OCR** et exploiter `minConfidencePercent` pour distinguer une extraction fiable d'une extraction nécessitant une vérification manuelle par l'agent.
3. **Exposer un CRUD pour `KycCrossValidationRule`** afin que les règles de cohérence soient administrables sans accès direct à la base de données.
4. **Trancher le sort de `KycEmailNotifier`** : le brancher réellement (en remplaçant le chemin `KycOutboxEventProcessor` générique pour obtenir des sujets/contenus différenciés par type d'événement) ou le supprimer proprement.
5. **Implémenter la période de grâce** (`gracePeriodDays`) après expiration d'un document, pour éviter un blocage brutal du compte du membre/client.
6. **Paginer les listings KYC** (`list`, `expiringSoon`, `myQueue`) à l'image du module Document.
7. **Centraliser la constante d'acteur système** (`SYSTEM_REVIEWER_ID`/`SYSTEM_UPLOADER_ID`) dans une classe utilitaire partagée (ex. `SystemActors.SYSTEM_USER_ID`).
8. **Distinguer la « réouverture après approbation »** par un événement dédié (`KYC_CASE_REOPENED`) plutôt que le générique `AUTO_STATUS_SYNC`, pour permettre une alerte spécifique côté conformité.

### 2.4 Nouvelles fonctionnalités proposées

- **Portail client KYC** : visibilité du membre/client sur le statut de son dossier, documents manquants/rejetés avec motif en langage clair, upload direct (déjà esquissé dans `docs/kyc-ameliorations-features.md` §15, toujours pas construit).
- **Screening PEP / Sanctions (AML)** et **vérification de liveness/selfie** pour les profils à risque élevé — toujours pertinents et non construits (§12-13 du document existant).
- **Webhook KYC** vers un CRM/ERP externe sur les événements d'approbation/rejet/expiration (§17).
- **Vérification RCCM/NIU Congo** via une intégration externe au moment de l'upload d'un document d'entreprise (§19).
- **KYC différencié par niveau de plan d'abonnement** (champ `requiredKycLevel` sur `Plan`, contrôle à l'activation) — le champ `kycLevel` existe déjà sur `KycCase` et n'attend que ce branchement (§14).

---

## 3. Constats transverses

### 3.1 Un document de roadmap existant est désormais largement obsolète

`docs/kyc-ameliorations-features.md` proposait 20 fonctionnalités à construire. Or, à l'examen du code actuel, **au moins 13 d'entre elles sont déjà livrées et en production** : §1 expiration/renouvellement (worker + `RENEWAL_REQUIRED`), §2 dashboard compliance, §3 assignation + SLA, §4 notes internes, §5 timeline, §6 rappels automatiques, §7 auto-approbation différée, §8 niveau de risque, §9 export PDF, §10 cross-validation documentaire, §11 OCR, §16 types de documents Congo (à confirmer en base), §18 revue en lot. Seules les sections §12 (liveness/selfie), §13 (AML/PEP), §14 (KYC différencié par plan), §15 (portail client), §17 (webhooks), §19 (RCCM/NIU) et §20 (ML anti-fraude) restent une roadmap valide.

Il est recommandé d'**archiver ou de réécrire ce document** pour ne conserver que les éléments encore pertinents — sinon il continuera d'induire en erreur quiconque s'y réfère pour prioriser le travail (au moment de cet audit, il est toujours présenté comme une liste de choses « à construire »).

### 3.2 Code mort récurrent (même schéma que dans l'audit Contrat/Ressource/Member/Customer)

Plusieurs éléments sont entièrement écrits ou configurés mais jamais branchés/appliqués :
- `KycEmailNotifier` (module KYC) — service de notification complet, jamais injecté.
- `DocumentType.requiresSignature` / `multipleAllowed` — persistés et exposés en DTO, jamais lus par la logique métier.
- `DocumentRequirement.businessLegalForm` — normalisé et stocké, jamais utilisé pour filtrer les exigences.
- `app.kyc.expiry.grace-period-days` et `app.kyc.ocr.min-confidence-percent` — propriétés de configuration définies, jamais lues dans le code.
- Plusieurs valeurs d'énumération jamais atteintes en pratique : `DocumentReviewStatus.NEEDS_CORRECTION`, `DocumentVersionUploadStatus.PENDING_SCAN/QUARANTINED/REJECTED/DELETED`, `DocumentAntivirusStatus.PENDING/ERROR`.

Le même verdict que pour les autres modules s'applique : pour chaque cas, terminer le câblage (souvent peu coûteux puisque le code/la configuration existent déjà) ou supprimer proprement, pour ne pas laisser une fausse impression de couverture fonctionnelle ou de configurabilité.

### 3.3 Sécurité de l'ingestion documentaire à renforcer avant un audit de conformité externe

La combinaison « antivirus » basé sur 2 signatures de fichiers (`MZ`/`ELF`) et détection MIME par magic bytes peut suffire pour un MVP, mais ne résisterait pas à un audit de sécurité sérieux (pas de détection de macros/scripts embarqués dans des PDF/images, pas de sandboxing, pas de file de quarantaine malgré les statuts d'énumération prévus à cet effet). Ce pipeline d'ingestion est le point d'entrée unique de **tout** le module KYC ainsi que des documents contractuels et financiers — il mérite d'être renforcé avant toute certification ou tout audit de conformité externe portant sur la lutte contre le blanchiment.

---

## Priorités recommandées (vue d'ensemble)

| Priorité | Action | Modules concernés |
|---|---|---|
| 1 | Compléter ou retirer l'extraction OCR des champs nom/naissance/nationalité — la cross-validation est aujourd'hui un théâtre de sécurité | KYC |
| 2 | Brancher un vrai moteur antivirus asynchrone avec mise en quarantaine | Document |
| 3 | Réécrire/archiver `docs/kyc-ameliorations-features.md` (13 items sur 20 déjà livrés) | KYC (documentation) |
| 4 | Appliquer `requiresSignature` et `multipleAllowed` sur `DocumentType` | Document |
| 5 | Exposer un CRUD pour les règles de cross-validation KYC | KYC |
| 6 | Trancher le sort de `KycEmailNotifier` (brancher ou supprimer) | KYC |
| 7 | Paginer les listings KYC (`list`, `expiringSoon`, `myQueue`) | KYC |
| 8 | Implémenter la période de grâce après expiration d'un document KYC | KYC |
| 9 | Ajouter des rappels avant expiration sur le worker documentaire générique | Document |
| 10 | Construire le portail client KYC (visibilité + upload direct) | KYC |
