# KYC — Dossiers (back-office)

> Base : `/sni/api/v1/kyc/cases`. Voir [README](./README.md) pour les endpoints de recherche client/membre/entreprise (auto-remplissage de `ownerCode`).

## Rattachement à un Customer / Member / Business

`POST /kyc/cases` prend `ownerType` (enum) + `ownerCode` (**le code public** — ex: le `memberId`/`customerId` affiché dans l'UI, **jamais l'id interne**). Même règle pour l'upload admin ([kyc-document-review-admin.md](./kyc-document-review-admin.md)). La création est idempotente par propriétaire : un second appel sur le même `ownerType`+`ownerCode` réutilise le dossier existant plutôt que d'en créer un doublon.

**Recherche pour pré-remplir `ownerCode`** : `GET /customers/search/basic?query=`, `GET /members/search/basic?query=`, `GET /businesses/search/basic?query=` (voir [README](./README.md#endpoints-de-recherche-pour-lauto-remplissage)).

## Cycle de vie du dossier

Toutes les routes nécessitent l'authentification admin ; permissions indiquées par route (🔒).

| Méthode &amp; Route | Rôle | Paramètres clés | Réponse | 🔒 |
|---|---|---|---|---|
| `POST /kyc/cases` | Créer un dossier KYC | `ownerType*`, `ownerCode*`, `documentTypeCodes` (optionnel, pré-remplit les exigences) | `KycCaseResponse` (201) | `KYC:READ` |
| `GET /kyc/cases/{code}` | Détail d'un dossier | — | `KycCaseResponse` | `KYC:READ` |
| `GET /kyc/cases` | Recherche/liste paginée | `status`, `ownerType`, `submittedAfter`/`submittedBefore`, `reviewedBy`, `pendingReviewOnly`, `expiringWithinDays`, `riskLevel`, `Pageable` | `PaginatedResponse<KycCaseResponse>` | `KYC:READ` |
| `POST /kyc/cases/{code}/submit` | Soumettre le dossier pour revue | — | `KycCaseResponse` | `KYC:READ` |
| `POST /kyc/cases/{code}/approve` | Approuver le dossier | `reviewedBy*`, `comment` | `KycCaseResponse` (status → `APPROVED`) | `KYC:APPROVE` |
| `POST /kyc/cases/{code}/reject` | Rejeter le dossier | `reviewedBy*`, `comment` | `KycCaseResponse` (status → `REJECTED`) | `KYC:REJECT` |
| `PATCH /kyc/cases/{code}/assign` | Assigner à un réviseur | `assignedTo` (Long) ou `email` | `KycCaseResponse` | `KYC:READ` |
| `PATCH /kyc/cases/{code}/risk-level` | Modifier le niveau de risque | `riskLevel*`, `reviewedBy`, `comment` | `KycCaseResponse` | `KYC:READ` |

## Suivi &amp; exigences

| Méthode &amp; Route | Rôle | Réponse | 🔒 |
|---|---|---|---|
| `GET /kyc/cases/{code}/missing-requirements` | Exigences documentaires manquantes | `List<KycRequirementStatus>` | `KYC:READ` |
| `GET /kyc/cases/{code}/requirements` | Toutes les exigences configurées | `List<KycCaseRequirementResponse>` | `KYC:READ` |
| `POST /kyc/cases/{code}/requirements` | Ajouter une exigence au dossier | `documentTypeCode*`, `documentTypeName`, `required` | `KycCaseRequirementResponse` (201) | `KYC:READ` |
| `DELETE /kyc/cases/{code}/requirements/{documentTypeCode}` | Retirer une exigence | — | 204 | `KYC:READ` |
| `GET /kyc/cases/{code}/expiry-status` | Statut d'expiration par document | `List<KycExpiryDocumentStatus>` | `KYC:READ` |
| `GET /kyc/cases/{code}/review-queue` | Documents en attente de revue pour ce dossier | `List<KycDocumentResponse>` | `KYC:READ` |
| `GET /kyc/cases/{code}/timeline` | Historique complet des événements | `List<KycTimelineEntryResponse>` | `KYC:READ` |
| `GET /kyc/cases/{code}/notes` | Notes du dossier | `List<KycCaseNoteResponse>` | `KYC:READ` |
| `POST /kyc/cases/{code}/notes` | Ajouter une note | `content*`, `authorId*`, `internal` (défaut true) | `KycCaseNoteResponse` (201) | `KYC:READ` |
| `GET /kyc/cases/{code}/export/pdf` | Rapport PDF du dossier | query `includeInternalNotes` (défaut true) | fichier PDF | `KYC:READ` |

## Décisions sur les documents d'un dossier

| Méthode &amp; Route | Rôle | Paramètres clés | 🔒 |
|---|---|---|---|
| `POST /kyc/cases/{code}/documents/bulk-approve` | Approuver plusieurs documents du dossier | `documentCodes*` (liste), `reviewedBy*` | `KYC:APPROVE` |
| `POST /kyc/cases/{code}/documents/bulk-reject` | Rejeter plusieurs documents, **motif par document** | `items*` (liste `{documentCode, reason}`), `reviewedBy*` | `KYC:REJECT` |
| `POST /kyc/cases/{code}/documents/request-correction` | Demander correction d'un document précis | `documentCode*`, `reviewedBy*`, `correctionNote*`, `deadlineDays` (défaut 7) | `KYC:READ` |

## Tableau de bord &amp; files

| Méthode &amp; Route | Rôle | Réponse | 🔒 |
|---|---|---|---|
| `GET /kyc/cases/dashboard` | Métriques globales (compteurs, SLA, expirations, activité récente) | `KycDashboardResponse` | `KYC:READ` |
| `GET /kyc/cases/expiring-soon` | Dossiers dont un document expire bientôt | query `days` (défaut 30), `Pageable` | `PaginatedResponse<KycCaseResponse>` | `KYC:READ` |
| `GET /kyc/cases/my-queue` | File du réviseur connecté | query `userId` (optionnel), `Pageable` | `PaginatedResponse<KycCaseResponse>` | `KYC:READ` |

## Enums

- **KycCaseStatus** : `NOT_STARTED` (créé, aucune activité) → `IN_PROGRESS` (upload en cours) → `SUBMITTED` (soumis) → `UNDER_REVIEW` (pris en charge) → `APPROVED` / `REJECTED` / `PENDING_CORRECTION` (correction demandée) / `RENEWAL_REQUIRED` (document expiré sur dossier déjà approuvé) / `EXPIRED`
- **KycRiskLevel** : `LOW, MEDIUM, HIGH, VERY_HIGH`

## Modèle (`KycCaseResponse`, champs clés)

`code` (numéro public du dossier), `ownerType`, `ownerCode`/`ownerName`, `status`, `riskLevel`, `kycLevel`, `startedAt`, `submittedAt`, `completedAt`, `reviewedBy`, `reviewedAt`, `decisionComment`, `assignedTo`, `assignedAt`, `slaDeadline`.

## Automatisations (aucune action frontend requise)

- OCR par document (résultat consultable via [kyc-document-review-admin.md](./kyc-document-review-admin.md)).
- Rappels d'expiration hebdomadaires + expiration automatique après la période de grâce (config, pas de champ par dossier).
- Relances des dossiers incomplets (job quotidien).
- Auto-approbation de certains types de documents après un délai configuré (`autoApproveAfterDays` sur le type de document).
