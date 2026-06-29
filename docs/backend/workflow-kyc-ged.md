# Workflow KYC & GED — Documentation technique complète

> Dernière mise à jour : 2026-06-18
> Couverture : module `features/document/` + `features/portal/document/` + `features/portal/profile/`

---

## Table des matières

1. [Module GED (Gestion Électronique de Documents)](#1-module-ged)
2. [Module KYC](#2-module-kyc)
3. [Portail client](#3-portail-client)
4. [Machines à états](#4-machines-à-états)
5. [Automatisation & workers](#5-automatisation--workers)
6. [Sécurité & RBAC](#6-sécurité--rbac)
7. [Modèle de données](#7-modèle-de-données)
8. [Workflows détaillés](#8-workflows-détaillés)
9. [Recommandations](#9-recommandations)

---

## 1. Module GED

### 1.1 Documents — CRUD & stockage

Base path : `/sni/api/v1/documents`

| Méthode | Endpoint | Description | Permissions |
|---------|----------|-------------|-------------|
| `POST` | `/upload` | Upload document (multipart) | Authentifié |
| `POST` | `/{code}/replace` | Remplacer le fichier (nouvelle version) | Authentifié |
| `GET` | `/{code}` | Détail d'un document | Authentifié |
| `GET` | `/` | Lister (paginé, filtrable par owner) | Authentifié |
| `GET` | `/{code}/versions` | Historique des versions | Authentifié |
| `GET` | `/{code}/download` | Téléchargement (Content-Disposition: attachment) | Authentifié |
| `GET` | `/{code}/preview` | Aperçu inline (Content-Disposition: inline) | Authentifié |
| `POST` | `/{code}/approve` | Approuver un document | `DOCUMENT:REVIEW` |
| `POST` | `/{code}/reject` | Rejeter un document | `DOCUMENT:REVIEW` |
| `POST` | `/{code}/request-correction` | Demander une correction | `DOCUMENT:REVIEW` |
| `POST` | `/{code}/archive` | Archiver manuellement | Authentifié |
| `POST` | `/{code}/versions/{n}/restore` | Restaurer une version antérieure | `DOCUMENT:REVIEW` |

**Comportement de l'upload :**

1. Validation des métadonnées (titre, expiryDate obligatoire selon le type)
2. Contrôle `multipleAllowed` — refuse un 2e document du même type si interdit (exclut les statuts `REJECTED`, `ARCHIVED`, `SUPERSEDED`, `EXPIRED` du comptage)
3. Inspection de sécurité synchrone (MIME par magic bytes, checksum SHA-256, scan MZ/ELF)
4. Stockage (filesystem ou MinIO selon config) — path : `{ownerFolder}/{documentCode}/v{N}/{uuid}.{ext}`
5. Création `DocumentVersion` (version courante marquée `current=true`, ancienne version marquée `current=false`)
6. Si catégorie `KYC` → création automatique d'un `KycDocument` lié au `KycCase` du propriétaire
7. Synchronisation KYC (`syncFromDocumentUpload`) — recompute automatique du statut du dossier
8. Publication événement outbox `DOCUMENT_UPLOADED`

**Comportement de l'approbation :**

1. Vérifie que le document est dans un état reviewable (`PENDING_REVIEW`, `UPLOADED`, `NEEDS_CORRECTION`)
2. Passe le statut à `APPROVED`
3. Synchronise le `KycDocument` associé → `VERIFIED`
4. Si `documentType.requiresSignature = true` → crée automatiquement une `DocumentSignature` PENDING pour le propriétaire (guard contre les doublons : ne crée pas si une signature PENDING existe déjà)
5. Publication événement `DOCUMENT_APPROVED` (+ `DOCUMENT_SIGNATURE_REQUIRED` si applicable)

**Comportement de la demande de correction :**

1. Passe le statut à `NEEDS_CORRECTION`
2. Enregistre `correctionNote`, `correctionDeadline` (défaut : +7 jours), incrémente `correctionCount`
3. Crée un `DocumentReview` avec statut `NEEDS_CORRECTION`
4. Resynchronise le KYC document → `PENDING`
5. Publication événement `DOCUMENT_CORRECTION_REQUESTED`

**Comportement du rejet :**

1. Passe le statut à `REJECTED`
2. Synchronise KYC → `REJECTED`
3. Cascade de rejet (`DocumentRejectionCascadeService`) :
   - Decline les signatures en attente (`PENDING` → `DECLINED`)
   - Annule les contrats liés (`CANCELLED`, avec `terminationReason`)
   - Annule en cascade les abonnements, passes et addons liés aux contrats annulés
   - Politique de remboursement : `PAYMENTS_UNCHANGED_REFUND_REQUIRED`
4. Publication événement `DOCUMENT_REJECTED`

**Comportement du remplacement :**

1. Crée une nouvelle `DocumentVersion` (incrémente le numéro)
2. Remet le statut à `PENDING_REVIEW` (ou `APPROVED` si `requiresReview=false`)
3. Réinitialise tous les `KycDocument` liés → `PENDING`
4. Recompute le statut du dossier KYC
5. Publication événement `DOCUMENT_REPLACED`

**Comportement de la restauration de version :**

1. Marque la version courante comme non-courante
2. Crée une NOUVELLE version (numéro incrémenté) copiant les métadonnées de la version cible (ne modifie pas l'ancienne)
3. Met à jour le Document avec les informations de la version restaurée
4. Publication événement `DOCUMENT_VERSION_RESTORED`

### 1.2 Types de documents

Base path : `/sni/api/v1/document-types`

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `POST` | `/` | Créer un type |
| `PATCH` | `/{code}` | Modifier un type |
| `GET` | `/{code}` | Détail |
| `GET` | `/` | Lister (filtrable `ownerType`, `active`) |
| `PATCH` | `/{code}/activate` | Activer |
| `PATCH` | `/{code}/deactivate` | Désactiver |
| `DELETE` | `/{code}` | Supprimer (seulement si aucun document lié) |

**Champs clés du type :**

| Champ | Effet |
|-------|-------|
| `code`, `name` | Identifiant unique et libellé |
| `category` | `KYC`, `LEGAL`, `SYSTEM`, `FINANCIAL`, `ASSET`, `OTHER` |
| `ownerType` | `CUSTOMER`, `MEMBER`, `BUSINESS`, `CONTRACT`, `INVOICE`, `PAYMENT`, `PROPOSAL`, `ASSET` |
| `required` | Indicateur pour les exigences documentaires |
| `requiresExpiryDate` | Valide la présence d'une date d'expiration à l'upload |
| `requiresReview` | Statut initial `PENDING_REVIEW` au lieu de `APPROVED` |
| `autoApprove` | Statut initial `APPROVED` immédiatement (priorité sur `requiresReview`) |
| `autoApproveAfterDays` | Utilisé par le worker d'auto-approbation différée |
| `requiresSignature` | Déclenche auto-création d'une `DocumentSignature` PENDING à l'approbation |
| `multipleAllowed` | Empêche un 2e upload du même type par owner si `false` |
| `allowedMimeTypes` | CSV de types MIME autorisés (défaut : `application/pdf,image/jpeg,image/png,image/webp`) |
| `maxFileSizeBytes` | Taille maximale du fichier |

### 1.3 Exigences documentaires

Base path : `/sni/api/v1/document-requirements`

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `POST` | `/` | Créer une exigence |
| `PATCH` | `/{id}` | Modifier |
| `GET` | `/{id}` | Détail |
| `GET` | `/` | Lister (filtrable `ownerType`, `active`) |
| `PATCH` | `/{id}/activate` | Activer |
| `PATCH` | `/{id}/deactivate` | Désactiver |
| `DELETE` | `/{id}` | Supprimer |

**Filtrage contextuel :**

- Par `ownerType` (MEMBER, CUSTOMER, BUSINESS)
- Par `customerType` (si CUSTOMER) — les exigences avec `customerType=null` s'appliquent à tous les types de clients
- Par `businessLegalForm` (si BUSINESS) — les exigences avec `businessLegalForm=null` s'appliquent à toutes les formes juridiques

Ce filtrage est appliqué dans :
- `KycAutomationServiceImpl.recomputeCaseState()` — recalcul automatique du statut
- `KycServiceImpl.assess()`, `toResponse()`, `getMissingRequirements()` — évaluation et affichage

### 1.4 Signatures

Base path : `/sni/api/v1/documents/{documentCode}/signatures`

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `POST` | `/` | Demander une signature manuellement |
| `GET` | `/` | Lister les demandes de signature |
| `PATCH` | `/{signatureId}/sign` | Enregistrer la signature (avec IP, user-agent, données de signature) |

Statuts : `PENDING` → `SIGNED` ou `DECLINED` ou `EXPIRED`

Création automatique : à l'approbation d'un document dont le type a `requiresSignature=true`, une signature PENDING est créée avec `signerType=ownerType` et `signerId=ownerId`.

### 1.5 Tags & métadonnées

**Catalogue de tags** : `/sni/api/v1/document-tags`
- CRUD complet (code, label, couleur hex, space)
- Scopé par `DocumentSpace` (optionnel)

**Assignation** : `/sni/api/v1/documents/{code}/tags`
- POST pour assigner, DELETE pour retirer, GET pour lister

**Métadonnées** : `/sni/api/v1/documents/{code}/metadata`
- PATCH pour upsert clé/valeur, GET pour lister, DELETE par clé
- Utilisable pour les filtres de recherche (`metaKey`, `metaValue`)

### 1.6 File de revue

Base path : `/sni/api/v1/review`

| Méthode | Endpoint | Description | Permission |
|---------|----------|-------------|------------|
| `GET` | `/queue` | File globale (tous les `PENDING_REVIEW`) | `DOCUMENT:REVIEW` |
| `GET` | `/{space}` | File par space | `@docSpaceSecurity.canReview(space)` |
| `POST` | `/{space}/bulk-approve` | Approbation en lot | Permission space |
| `POST` | `/{space}/bulk-reject` | Rejet en lot | Permission space |
| `POST` | `/{space}/bulk-request-correction` | Correction en lot | Permission space |

Évaluation RBAC par space : `DocumentSpaceSecurityService` (bean `docSpaceSecurity`) vérifie `DOCUMENT_REVIEW_{SPACE}` ou la permission globale `DOCUMENT_REVIEW`.

### 1.7 Recherche, dashboard, analytics, export

| Méthode | Endpoint | Description | Permission |
|---------|----------|-------------|------------|
| `GET` | `/documents/search` | Recherche multi-filtres | Authentifié |
| `GET` | `/documents/dashboard` | Compteurs agrégés | Authentifié |
| `GET` | `/documents/folder` | Dossier d'un owner (groupé par space) | Authentifié |
| `GET` | `/documents/analytics` | Analytics sur période | `DOCUMENT:REVIEW` |
| `GET` | `/documents/export/csv` | Export CSV filtré (max 10 000 lignes) | `DOCUMENT:REVIEW` |

**Filtres de recherche disponibles :** `q` (full-text titre/description), `space`, `category`, `tags` (multi-valeur), `ownerType`, `ownerCode`, `statuses` (multi-valeur), `expiresInDays`, `isExpired`, `uploadedAfter`, `uploadedBefore`, `metaKey`, `metaValue`.

**Dashboard** : total, par statut, par catégorie, documents >48h en revue, taux de rejet 30j, délai moyen de revue, top 5 tags.

**Analytics** : séries temporelles (uploads/approbations/rejets/corrections par mois), taux correction 30j, taux rejet 30j, délai moyen.

**Folder** : documents groupés par `DocumentSpace`, taux de complétude par space, exigences manquantes, documents expirant sous 30j.

### 1.8 Logs d'accès

| Méthode | Endpoint | Description | Permission |
|---------|----------|-------------|------------|
| `GET` | `/documents/{code}/access-logs` | Historique d'accès d'un document | `DOCUMENT:REVIEW` |
| `GET` | `/documents/access-logs/by-user` | Accès par utilisateur | `DOCUMENT:REVIEW` |

Enregistrement automatique à chaque `download` et `preview` : userId, action, IP, user-agent, timestamp.

### 1.9 Politiques de rétention

Base path : `/sni/api/v1/document-retention-policies`

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `GET` | `/` | Lister les politiques |
| `GET` | `/{code}` | Détail |
| `POST` | `/` | Créer |
| `PUT` | `/{code}` | Modifier |
| `DELETE` | `/{code}` | Désactiver (soft delete) |

Toutes les opérations requièrent `DOCUMENT:REVIEW`.

**Champs clés :** `code`, `name`, `space` (optionnel), `documentTypeCode` (optionnel), `retentionDays`, `retentionReference` (`UPLOAD_DATE` ou `EXPIRY_DATE`), `action` (`ARCHIVE` ou `PURGE_FLAG`).

**Worker** : quotidien (02:30 UTC), filtre les documents `APPROVED`/`SIGNED`/`EXPIRED` dont la date de rétention est dépassée.

### 1.10 Automatisation du cycle de vie documentaire

**`DocumentLifecycleWorker`** — exécuté toutes les heures :
- `expireDocuments()` : expire les documents dont `expiryDate < today` (statuts `APPROVED`, `SIGNED`, `PENDING_REVIEW`) → `EXPIRED`
- `notifyPreExpiry()` : publie `DOCUMENT_EXPIRY_REMINDER` pour les documents expirant dans 30 et 7 jours

---

## 2. Module KYC

### 2.1 API Admin — Gestion des dossiers

Base path : `/sni/api/v1/kyc/cases`

| Méthode | Endpoint | Description | Annotations |
|---------|----------|-------------|-------------|
| `POST` | `/` | Créer un dossier KYC | `@Audited` `@Idempotent` |
| `GET` | `/{code}` | Détail du dossier (avec documents, requirements, completion) | |
| `GET` | `/` | Recherche multi-filtres (paginée via Specification) | |
| `GET` | `/dashboard` | Métriques compliance (SLA, expirations, activité) | |
| `GET` | `/expiring-soon` | Dossiers expirant bientôt (paginé) | |
| `GET` | `/my-queue` | File du reviewer connecté (paginée niveau DB) | |
| `POST` | `/{code}/submit` | Soumettre pour revue | `@Audited` `@Idempotent` |
| `POST` | `/{code}/approve` | Approuver le dossier | `@Audited` `@Idempotent` |
| `POST` | `/{code}/reject` | Rejeter le dossier | `@Audited` `@Idempotent` |
| `GET` | `/{code}/missing-requirements` | Exigences non satisfaites | |
| `PATCH` | `/{code}/assign` | Assigner à un reviewer (+ calcul SLA) | `@Audited` |
| `POST` | `/{code}/notes` | Ajouter une note (interne ou publique) | `@Audited` |
| `GET` | `/{code}/notes` | Lister les notes | |
| `GET` | `/{code}/timeline` | Timeline d'audit complète (5 sources agrégées) | |
| `GET` | `/{code}/expiry-status` | Statut d'expiration par document | |
| `PATCH` | `/{code}/risk-level` | Modifier le niveau de risque | `@Audited` |
| `GET` | `/{code}/export/pdf` | Export PDF du dossier (Thymeleaf → openhtmltopdf) | |

**Filtres de recherche** (`GET /`) : `status`, `ownerType`, `submittedAfter`, `submittedBefore`, `reviewedBy`, `pendingReviewOnly`, `expiringWithinDays`, `riskLevel` + pagination.

**Dashboard** : totalCases, par statut, SLA (revus sous 24h / dépassant 48h / délai moyen), expirations (7j / 30j / 60j), par ownerType, activité récente.

**SLA** : calculé à la soumission — `+24h` (MEMBER), `+48h` (CUSTOMER), `+72h` (BUSINESS).

**KYC Level** : calculé automatiquement (1-4) basé sur les types de documents et le niveau de risque. Niveau 4 si documents SELFIE/LIVENESS/AML ou risque HIGH/VERY_HIGH.

### 2.2 Revue de documents KYC

Base path : `/sni/api/v1/kyc/documents`

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `POST` | `/bulk-approve` | Approbation en lot (isolation par item) |
| `POST` | `/bulk-reject` | Rejet en lot (aliases : `/bulk-disapprove`, `/bulk-desapprove`) |
| `GET` | `/review-queue` | Documents KYC en attente (PENDING) |
| `GET` | `/reviewed` | Documents revus (filtrable `?status=APPROVED` ou `REJECTED`) |
| `GET` | `/approved` | Raccourci : documents approuvés |
| `GET` | `/rejected` | Raccourci : documents rejetés (aliases : `/disapproved`, `/desapproved`) |
| `GET` | `/{documentCode}/ocr-result` | Résultat OCR d'un document |

Les opérations bulk utilisent `Propagation.NOT_SUPPORTED` pour isoler chaque item — un échec n'annule pas les autres.

### 2.3 Règles de cross-validation

Base path : `/sni/api/v1/kyc/cross-validation-rules`

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `POST` | `/` | Créer une règle (`@Audited`) |
| `PUT` | `/{id}` | Modifier (`@Audited`) |
| `GET` | `/{id}` | Détail |
| `GET` | `/` | Lister (filtrable `?activeOnly=true`) |
| `DELETE` | `/{id}` | Supprimer (`@Audited`) |

**Champs** : `documentTypeCode1`, `documentTypeCode2`, `fieldToCompare`, `blocking` (défaut true), `active` (défaut true).

**Champs comparables** : `first_name`, `last_name`, `date_of_birth`, `expiry_date`, `document_number`, `nationality`.

**Comportement** : à l'approbation d'un dossier KYC, `validateCrossDocumentRules()` compare les valeurs OCR extraites entre les deux types de documents. Si `blocking=true` et les valeurs divergent → `BadRequestException`, l'approbation est refusée.

### 2.4 OCR — Extraction automatique

**Service** : `TesseractKycOcrService` (Tess4J, langues `fra+eng`)

Déclenché de façon asynchrone après chaque upload/rafraîchissement de document KYC.

**Champs extraits (6/6) :**

| Champ | Stratégie d'extraction |
|-------|----------------------|
| `extractedLastName` | Labels `Nom`, `Surname`, `Last name` + fallback ligne seule `NOM` |
| `extractedFirstName` | Labels `Prénoms`, `Given names`, `First name`, `Forenames` + fallback `PRENOMS` |
| `extractedDateOfBirth` | Labels `Né(e) le`, `Date de naissance`, `Date of birth`, `DOB`, `Born` → date passée. Fallback : toute date > 10 ans dans le passé |
| `extractedExpiryDate` | Première date future trouvée dans le texte |
| `extractedDocumentNumber` | Labels `N°`, `Numero`, `Number`, `ID`, `Document n` → séquence alphanumérique ≥5 chars |
| `extractedNationality` | Labels `Nationalité`, `Nationality` + détection directe : `CONGOLAIS(E)`, `CAMEROUNAIS(E)`, `GABONAIS(E)`, `CENTRAFRICAIN(E)`, `TCHADIEN(NE)`, `FRANÇAIS(E)` |

**Formats de dates supportés** : `dd/MM/yyyy`, `dd-MM-yyyy`, `dd.MM.yyyy`, `yyyy-MM-dd`

**Score de confiance** : `nombre de champs extraits / 6` (0.0000 à 1.0000). Seuil configurable : `app.kyc.ocr.min-confidence-percent` (défaut 60%).

**États du résultat OCR** : `SUCCESS` (extraction tentée), `SKIPPED` (OCR désactivé/provider non supporté/pas de fichier), `FAILED` (exception Tesseract).

---

## 3. Portail client

### 3.1 KYC client

Base path : `/sni/api/v1/client/documents/kyc`

| Méthode | Endpoint | Content-Type | Description |
|---------|----------|-------------|-------------|
| `GET` | `/` | — | Mon dossier KYC (statut, documents, requirements, completion) |
| `GET` | `/requirements` | — | Exigences documentaires pour mon profil |
| `GET` | `/completion` | — | Taux de complétion détaillé |
| `GET` | `/documents` | — | Mes documents KYC |
| `POST` | `/documents` | `multipart/form-data` | Uploader un document |
| `POST` | `/documents` | `application/json` | → 400 avec message explicite |
| `GET` | `/documents/{id}` | — | Détail d'un document |
| `GET` | `/documents/{id}/download` | — | Télécharger |
| `PUT` | `/documents/{id}` | `multipart/form-data` | Re-soumettre un document rejeté |
| `PUT` | `/documents/{id}` | `application/json` | → 400 avec message explicite |
| `DELETE` | `/documents/{id}` | — | Supprimer un document PENDING |
| `POST` | `/submit` | — | Soumettre le dossier pour revue |

**Validations côté portail :**

- Fichier obligatoire, max 10 MB, types acceptés : `application/pdf`, `image/jpeg`, `image/png`
- Le dossier doit être dans un état éditable (ni `APPROVED`, ni `UNDER_REVIEW`, ni `SUBMITTED`)
- Re-soumission : seuls les documents `PENDING` ou `REJECTED` sont acceptés
- Suppression : seuls les documents `PENDING` sont acceptés
- Les requêtes JSON vers les endpoints d'upload retournent un 400 avec un message guidant vers `multipart/form-data`

### 3.2 Statut d'onboarding

Endpoint : `GET /sni/api/v1/client/me/onboarding-status`

Retourne l'état de progression du compte pour piloter l'affichage frontend (aucun localStorage nécessaire) :

```json
{
  "emailVerified": true,
  "profileComplete": true,
  "kycStatus": "NOT_STARTED",
  "kycCompletionPercent": 0,
  "gracePeriodActive": false,
  "gracePeriodDaysRemaining": 0,
  "portalFullyActive": false,
  "nextStep": "SUBMIT_KYC",
  "restrictedActions": ["BOOKING", "PAYMENT", "CONTRACT", "SUBSCRIPTION"]
}
```

**Mapping des 9 statuts backend → 5 statuts frontend :**

| Backend | Frontend | `nextStep` | `kycCompletionPercent` |
|---------|----------|------------|----------------------|
| `null` / `NOT_STARTED` | `NOT_STARTED` | `SUBMIT_KYC` | 0% |
| `IN_PROGRESS` | `DRAFT` | `COMPLETE_KYC` | 30% |
| `SUBMITTED` / `UNDER_REVIEW` | `SUBMITTED` | `AWAIT_KYC_REVIEW` | 70% |
| `APPROVED` | `APPROVED` | `COMPLETED` | 100% |
| `REJECTED` / `PENDING_CORRECTION` / `RENEWAL_REQUIRED` / `EXPIRED` | `REJECTED` | `CORRECT_KYC` | 40% |

**Logique :**
- `portalFullyActive = emailVerified AND kycApproved`
- `restrictedActions` : si email non vérifié OU (KYC non approuvé ET hors période de grâce) → `BOOKING`, `PAYMENT`, `CONTRACT`, `SUBSCRIPTION`
- Cohérent cross-device : l'état est serveur-only, pas de dépendance localStorage

### 3.3 Guard d'onboarding

`ClientOnboardingGuard` (HandlerInterceptor) — intercepte toutes les requêtes du portail client :

| Condition | Réponse | Code |
|-----------|---------|------|
| Email non vérifié (`isAccountLocked=true`) | 403 | `EMAIL_NOT_VERIFIED` |
| KYC non approuvé ET hors période de grâce | 403 | `KYC_REQUIRED` |
| Endpoint `/me/onboarding-status` | Toujours autorisé | Whitelisté |
| Sinon | Laisse passer | — |

**Période de grâce** : `member.kycGracePeriodEndAt` — durée configurable (défaut 7 jours), permet l'accès au portail pendant le processus KYC.

---

## 4. Machines à états

### 4.1 DocumentStatus (10 valeurs)

```
                                        ┌──────────┐
                                   ┌───→│  SIGNED  │
                                   │    └──────────┘
                                   │
DRAFT → UPLOADED → PENDING_REVIEW ─┼──→ APPROVED ──→ EXPIRED ──→ ARCHIVED
                       ↑           │
                       │           └──→ NEEDS_CORRECTION ──→ (replace) ──→ PENDING_REVIEW
                       │           │
                       │           └──→ REJECTED ──→ ARCHIVED
                       │
                       └── SUPERSEDED (ancienne version)
```

### 4.2 KycCaseStatus (9 valeurs)

```
NOT_STARTED
   │
   └──→ IN_PROGRESS ──→ SUBMITTED ──→ UNDER_REVIEW ──→ APPROVED
              ↑                                     │       │
              │                                     │       └──→ RENEWAL_REQUIRED
              │                                     │
              └──── PENDING_CORRECTION ←────────────┘
                          ↑
                          └──── REJECTED
                          └──── EXPIRED
```

**Transitions automatiques (`recomputeCaseState`) :**

| Condition | Statut cible |
|-----------|-------------|
| Aucune exigence obligatoire | `APPROVED` |
| Au moins un document rejeté | `PENDING_CORRECTION` |
| Tous les documents obligatoires vérifiés | `APPROVED` |
| Tous les documents obligatoires présents (non tous vérifiés) | `UNDER_REVIEW` |
| Documents manquants | `IN_PROGRESS` |

**Synchronisation avec le statut du propriétaire :**

| KycCaseStatus | MemberStatus | CustomerStatus |
|---------------|-------------|----------------|
| `IN_PROGRESS` / `NOT_STARTED` | `PENDING` | `PENDING` |
| `SUBMITTED` / `UNDER_REVIEW` | `UNDER_REVIEW` | `PENDING` |
| `PENDING_CORRECTION` / `REJECTED` | `PENDING_CORRECTION` | `PENDING` |
| `APPROVED` | `ACTIVE` | `ACTIVE` |
| `RENEWAL_REQUIRED` / `EXPIRED` | `PENDING_CORRECTION` | `PENDING` |

### 4.3 KycDocumentVerificationStatus (4 valeurs)

```
PENDING ──→ VERIFIED   (document approuvé)
       ──→ REJECTED   (document rejeté)
       ──→ EXPIRED    (document expiré via worker)
```

### 4.4 DocumentSignatureStatus (4 valeurs)

```
PENDING ──→ SIGNED    (signé avec données)
       ──→ DECLINED  (décliné ou cascade de rejet)
       ──→ EXPIRED   (délai dépassé)
```

---

## 5. Automatisation & workers

### 5.1 Workers KYC — `KycAutomationWorker`

| Job | Schedule | Description |
|-----|----------|-------------|
| `checkExpiringKycDocuments` | Lundi 8h | Envoie des rappels à J-60, J-30, J-7 avant expiration. Expire les documents après la période de grâce (défaut 15 jours). |
| `remindIncompleteKycCases` | Quotidien 9h | Relance les dossiers `IN_PROGRESS` / `PENDING_CORRECTION`. Max 3 relances à J+3, J+7, J+14 (configurable). |
| `autoApproveEligibleDocuments` | Quotidien 8h | Approuve automatiquement les documents dont le type a `autoApproveAfterDays` défini et le délai écoulé. |

### 5.2 Synchronisation bidirectionnelle — `KycAutomationServiceImpl`

| Méthode | Déclencheur | Effet |
|---------|-------------|-------|
| `initializeMemberKyc(memberId)` | Création de member | Crée un `KycCase` si des exigences existent |
| `initializeCustomerKyc(customerId)` | Création de customer | Idem |
| `syncFromDocumentUpload(document)` | Chaque upload KYC | Attache au case, recompute le statut, lance l'OCR |
| `syncFromDocumentReview(document)` | Chaque revue de document | Attache au case, recompute le statut |
| `recomputeCaseState(kycCase)` | Automatique | Recalcule le statut, synchronise le propriétaire |

Le filtrage des exigences documentaires tient compte du `customerType` (CUSTOMER) et du `businessLegalForm` (BUSINESS) — résolu dynamiquement via `BusinessService`.

### 5.3 Workers documentaires

| Worker | Schedule | Description |
|--------|----------|-------------|
| `DocumentLifecycleWorker` | Toutes les heures | Expire les documents périmés, envoie des rappels pré-expiration (30j, 7j) |
| `DocumentRetentionWorker` | Quotidien 02:30 UTC | Applique les politiques de rétention (`ARCHIVE` ou `PURGE_FLAG`) |

### 5.4 Notifications — pattern Outbox

**Processeur** : `KycOutboxEventProcessor` — traite les événements et envoie des emails via Thymeleaf.

**Templates email** :
- `kyc-event.html` — notification générique (création, soumission, approbation, rejet, assignation)
- `kyc-expiry-reminder.html` — avertissement d'expiration

**Événements publiés** : `KYC_CASE_CREATED`, `KYC_CASE_SUBMITTED`, `KYC_CASE_APPROVED`, `KYC_CASE_REJECTED`, `KYC_CASE_ASSIGNED`, `KYC_CASE_NOTE_ADDED`, `KYC_CASE_AUTO_CREATED`, `KYC_CASE_AUTO_STATUS_SYNC`, `KYC_RISK_LEVEL_UPDATED`, `KYC_RENEWAL_REQUIRED`, `KYC_INCOMPLETE_REMINDER`, `KYC_DOCUMENT_EXPIRY_REMINDER`, `KYC_DOCUMENT_EXPIRED`.

### 5.5 Configuration — `KycAutomationProperties`

```yaml
app.kyc:
  expiry:
    reminderDays: [60, 30, 7]       # Jours avant expiration pour les rappels
    gracePeriodDays: 15              # Délai de grâce après expiration
  reminders:
    enabled: true
    reminderAfterDays: [3, 7, 14]   # Relances pour dossiers incomplets
    maxReminders: 3
  ocr:
    enabled: true
    provider: TESSERACT
    language: fra+eng
    dataPath: storage/tessdata
    minConfidencePercent: 60         # Seuil de confiance exploitable
```

---

## 6. Sécurité & RBAC

### 6.1 Permissions documentaires

| Permission DB | Authority Spring | Portée |
|---------------|------------------|--------|
| `DOCUMENT_REVIEW` | `DOCUMENT:REVIEW` | Toutes les spaces |
| `DOCUMENT_REVIEW_KYC_SPACE` | `DOCUMENT:REVIEW_KYC_SPACE` | KYC uniquement |
| `DOCUMENT_REVIEW_CONTRACT_SPACE` | `DOCUMENT:REVIEW_CONTRACT_SPACE` | Contrats uniquement |
| `DOCUMENT_REVIEW_FINANCIAL_SPACE` | `DOCUMENT:REVIEW_FINANCIAL_SPACE` | Financier uniquement |
| `DOCUMENT_REVIEW_ASSET_SPACE` | `DOCUMENT:REVIEW_ASSET_SPACE` | Assets uniquement |
| `DOCUMENT_REVIEW_ADMINISTRATIVE` | `DOCUMENT:REVIEW_ADMINISTRATIVE` | Administratif uniquement |
| `DOCUMENT_REVIEW_GENERIC` | `DOCUMENT:REVIEW_GENERIC` | Générique uniquement |

Un utilisateur avec `DOCUMENT_REVIEW` (global) peut agir sur **toutes** les spaces.

### 6.2 Sécurité de l'ingestion

| Contrôle | Détail |
|----------|--------|
| **Détection MIME** | Par magic bytes (PDF: `%PDF`, JPEG: `0xFF 0xD8 0xFF`, PNG: `0x89PNG`, WEBP: `RIFF...WEBP`) — ne fait pas confiance au header client |
| **Checksum SHA-256** | Calculé et stocké pour chaque version — détection de doublons et de falsification |
| **Scan antivirus** | Détection des headers `MZ` (exécutables Windows) et `ELF` (exécutables Linux) |
| **Stockage** | Filesystem local (dev) ou MinIO/S3 (prod) — configurable via `app.document.storage.provider` |

### 6.3 Gestion des erreurs Content-Type

`HttpMediaTypeNotSupportedException` est interceptée par `GlobalExceptionHandler` et retourne un **415 Unsupported Media Type** avec un message actionnable (au lieu du 500 générique précédent).

Les endpoints d'upload du portail client ont des fallbacks explicites pour `application/json` → **400 Bad Request** avec instructions `multipart/form-data`.

---

## 7. Modèle de données

### 7.1 Entités GED

| Entité | Table | Rôle |
|--------|-------|------|
| `Document` | `document` | Document logique (propriétaire polymorphe, statut, space, soft-delete) |
| `DocumentType` | `document_type` | Catalogue des types de documents avec contraintes |
| `DocumentVersion` | `document_version` | Versions physiques (1 seule `current=true` par document) |
| `DocumentRequirement` | `document_requirement` | Exigences par ownerType, customerType, businessLegalForm |
| `DocumentReview` | `document_review` | Trail de revue (décision, motif, correction, deadline) |
| `DocumentSignature` | `document_signature` | Workflow de signature (signerType/Id, statut, données, IP) |
| `DocumentTag` | `document_tag` | Catalogue de tags (code, label, couleur, space) |
| `DocumentTagAssignment` | `document_tag_assignment` | Liaison document ↔ tag |
| `DocumentMetadata` | `document_metadata` | Clé/valeur libre par document |
| `DocumentAccessLog` | `document_access_log` | Log d'accès (userId, action, IP, user-agent) |
| `DocumentRetentionPolicy` | `document_retention_policy` | Politiques de rétention/archivage |

### 7.2 Entités KYC

| Entité | Table | Rôle |
|--------|-------|------|
| `KycCase` | `kyc_case` | Dossier KYC (propriétaire, statut, SLA, assignation, risque, niveau) |
| `KycDocument` | `kyc_document` | Lien document ↔ dossier KYC (statut de vérification, numéro, dates) |
| `KycVerification` | `kyc_verification` | Trail d'audit des décisions de vérification |
| `KycCaseNote` | `kyc_case_note` | Notes internes/publiques sur un dossier |
| `KycDocumentOcrResult` | `kyc_document_ocr_result` | Résultats OCR (6 champs extraits, score de confiance, JSON brut) |
| `KycCrossValidationRule` | `kyc_cross_validation_rule` | Règles de cohérence inter-documents |

---

## 8. Workflows détaillés

### 8.1 Onboarding Member — KYC complet

```
 1. Création du member → statut PENDING
 2. initializeMemberKyc() → crée un KycCase (IN_PROGRESS) si des exigences MEMBER existent
 3. Frontend : GET /client/me/onboarding-status → kycStatus: NOT_STARTED ou DRAFT
 4. Frontend : GET /client/documents/kyc/requirements → liste des pièces requises
 5. Utilisateur uploade chaque document :
    POST /client/documents/kyc/documents (multipart/form-data)
    → Document créé (PENDING_REVIEW ou APPROVED si autoApprove)
    → KycDocument créé (PENDING)
    → OCR Tesseract déclenché en async (extraction 6 champs)
    → recomputeCaseState() → IN_PROGRESS puis UNDER_REVIEW quand complet
 6. Utilisateur soumet : POST /client/documents/kyc/submit
    → KycCase: SUBMITTED, Member: UNDER_REVIEW
    → Calcul riskLevel, kycLevel, SLA deadline (+24h)
    → Événement KYC_CASE_SUBMITTED → email
 7. Agent compliance : GET /kyc/cases/my-queue ou /kyc/cases?pendingReviewOnly=true
 8. Agent revoit chaque document :
    POST /documents/{code}/approve ou /reject ou /request-correction
    → recomputeCaseState() recalcule automatiquement
 9. Quand tous les docs obligatoires sont VERIFIED :
    POST /kyc/cases/{code}/approve
    → Cross-validation exécutée (compare OCR entre documents)
    → KycCase: APPROVED, Member: ACTIVE
    → Wallet XAF provisionné (non-bloquant)
    → Événement KYC_CASE_APPROVED → email
10. Si rejet :
    POST /kyc/cases/{code}/reject (commentaire obligatoire)
    → KycCase: PENDING_CORRECTION, Member: PENDING_CORRECTION
    → Événement KYC_CASE_REJECTED → email
    → Frontend : kycStatus: REJECTED, nextStep: CORRECT_KYC
    → Utilisateur re-soumet via PUT /client/documents/kyc/documents/{id}
```

### 8.2 Onboarding Business

```
1. Création du business entity
2. POST /kyc/cases → dossier KYC créé
3. Exigences filtrées par businessLegalForm (si configuré dans document_requirement)
4. Upload : RCCM, NIU, statuts, pièce du représentant, justificatif d'adresse, etc.
5. Soumission → SLA +72h → revue compliance → approbation/rejet
```

### 8.3 Renouvellement KYC

```
1. KycAutomationWorker (lundi 8h) détecte un document expirant
2. Rappels envoyés à J-60, J-30, J-7 (configurable)
3. Après expiration + grace period (15j) → document EXPIRED
4. Si dossier APPROVED → RENEWAL_REQUIRED
   Sinon → PENDING_CORRECTION
5. Member/Customer → PENDING_CORRECTION / PENDING
6. Utilisateur re-soumet le document renouvelé
7. recomputeCaseState() → retour à APPROVED si tout est vérifié
```

### 8.4 Workflow documentaire générique (non-KYC)

```
1. POST /documents/upload (multipart, @Audited, @Idempotent)
2. Statut initial selon le type :
   - autoApprove=true        → APPROVED
   - requiresReview=true     → PENDING_REVIEW
   - sinon                   → APPROVED
3. Reviewer : POST /documents/{code}/approve|reject|request-correction
4. Si requiresSignature=true à l'approbation → DocumentSignature PENDING créée
5. Signataire : PATCH /documents/{docCode}/signatures/{id}/sign
6. Lifecycle worker : expiration automatique si expiryDate dépassée
7. Retention worker : archivage automatique selon les politiques
```

---

## 9. Recommandations

### 9.1 Priorité haute — Correctifs fonctionnels

| # | Problème | Impact | Effort |
|---|----------|--------|--------|
| 1 | **Antivirus basique** : seule la détection de signatures `MZ`/`ELF` est en place. Pas de scan ClamAV ni de pipeline de quarantaine malgré les statuts prévus (`PENDING_SCAN`, `QUARANTINED`). | Ne résisterait pas à un audit de sécurité. | Moyen — intégrer ClamAV via socket ou REST, activer le pipeline asynchrone. |
| 2 | ~~**OCR incomplet**~~ | ~~Résolu — les 6 champs sont maintenant extraits avec un score de confiance réel.~~ | ~~Fait.~~ |

### 9.2 Priorité moyenne — Améliorations de workflow

| # | Amélioration | Bénéfice |
|---|-------------|----------|
| 3 | **Notifications des signataires** : `DocumentSignatureService` ne notifie jamais les signataires (ni à la création de la demande, ni à la signature/au refus). | Les signataires ne savent pas qu'une signature est attendue. |
| 4 | **Emails différenciés** : `KycOutboxEventProcessor` envoie un template générique unique (`kyc-event.html`). Des sujets différenciés par type d'événement (approbation/rejet/soumission) amélioreraient la communication. | Meilleure UX email. |
| 5 | **Événement `KYC_CASE_REOPENED`** distinct de `AUTO_STATUS_SYNC` : quand un dossier `APPROVED` repasse en `IN_PROGRESS` (nouveau document uploadé après approbation), l'événement générique ne permet pas de distinguer une réouverture. | Alerte compliance spécifique. |
| 6 | **Motifs de rejet détaillés côté portail** : le frontend reçoit `decisionComment` au niveau du dossier mais pas les motifs de rejet par document (`rejectionReasonCode`, `rejectionReasonDetail`). | L'utilisateur voit « rejeté » sans savoir quoi corriger exactement. |

### 9.3 Priorité basse — Évolutions fonctionnelles

| # | Fonctionnalité | Description |
|---|---------------|-------------|
| 7 | **KYC différencié par plan d'abonnement** | `kycLevel` existe sur KycCase mais n'est pas contrôlé à l'activation. Un champ `requiredKycLevel` sur `Plan` bloquerait les souscriptions premium sans KYC suffisant. |
| 8 | **Screening PEP/Sanctions (AML)** | Intégration d'un service tiers pour vérifier les noms contre les listes de sanctions. |
| 9 | **Vérification RCCM/NIU Congo** | Validation automatique des identifiants d'entreprise via un service externe. |
| 10 | **Webhooks KYC** | Publication d'événements vers un CRM/ERP externe. Le pattern Outbox est déjà en place — il suffirait d'ajouter un processeur webhook. |

### 9.4 Dette technique

| # | Problème | Recommandation |
|---|----------|----------------|
| 11 | `SYSTEM_REVIEWER_ID = 0L` dupliqué dans `KycAutomationWorker` et `DocumentServiceImpl` | Centraliser dans `SystemActors.SYSTEM_USER_ID`. |
| 12 | Locale codée en dur `Locale.FRANCE` pour l'export PDF et les emails | Paramétrer ou résoudre depuis le profil utilisateur. |
| 13 | `DocumentVersionUploadStatus.PENDING_SCAN` / `QUARANTINED` / `REJECTED` / `DELETED` et `DocumentAntivirusStatus.PENDING` / `ERROR` jamais atteints | Brancher le pipeline de scan asynchrone ou retirer ces valeurs. |
