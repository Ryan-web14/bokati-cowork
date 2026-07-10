# GED &amp; KYC — Documentation frontend

> **Audience** : Développeur frontend
> **Base URL** : `https://api.elleaose.com/sni/api/v1`
> Toutes les routes ci-dessous sont **back-office** (JWT admin requis) sauf mention contraire (`kyc-client-portal.md` est le seul module client-portail).

Audit du module GED (gestion électronique des documents) et du module KYC — un document par sous-module, court et centré sur les endpoints.

## Sommaire

### GED
| Document | Contenu |
|---|---|
| [ged-documents-core.md](./ged-documents-core.md) | Upload, versions, décisions de revue, recherche, dashboard, analytics, export, logs d'accès |
| [ged-catalog-config.md](./ged-catalog-config.md) | Types de documents (catalogue) + règles d'exigence (documents obligatoires par type de propriétaire) |
| [ged-organization.md](./ged-organization.md) | Dossiers (arborescence), espaces (contrats/financier/actifs/administratif/général), tags &amp; métadonnées |
| [ged-review-workflow-retention.md](./ged-review-workflow-retention.md) | File de revue en masse, workflows d'approbation personnalisés, politiques de rétention |
| [ged-sharing-permissions-signature.md](./ged-sharing-permissions-signature.md) | Liens de partage publics, permissions fines, signature électronique |

### KYC
| Document | Contenu |
|---|---|
| [kyc-cases.md](./kyc-cases.md) | Cycle de vie du dossier KYC (admin) : création, revue, notes, timeline, risque, export PDF |
| [kyc-document-review-admin.md](./kyc-document-review-admin.md) | Revue en masse des documents KYC + upload par un admin pour le compte d'un client |
| [kyc-configuration.md](./kyc-configuration.md) | Règles de validation croisée entre documents |
| [kyc-client-portal.md](./kyc-client-portal.md) | Self-service — le client gère son propre dossier KYC depuis le portail |

---

## Rattachement d'un document/dossier KYC à un Customer / Member / Business

Le GED et le KYC ne lient jamais un document via des colonnes `customerId`/`memberId` dédiées. Ils utilisent une **paire polymorphe** :

- `ownerType` — enum `DocumentOwnerType` : `CUSTOMER, MEMBER, BUSINESS, CONTRACT, INVOICE, PAYMENT, PROPOSAL, ASSET`
- `ownerCode` — le **code public** de l'entité (ex: le `customerId` ou `memberId` affiché dans l'UI — **jamais l'id interne numérique**)

Cette paire apparaît dans quasiment toutes les requêtes de création/upload/filtre du GED et dans la création de dossier KYC (`ownerType` + `ownerCode`).

## Endpoints de recherche pour l'auto-remplissage

À utiliser pour peupler un champ "rechercher un client/membre/entreprise" avant de récupérer son `ownerCode`, quand un écran GED ou KYC doit rattacher un document/dossier à une entité existante.

| Entité | Recherche rapide (texte libre) | Recherche avancée (filtres) | Par email/nom exact | Détail |
|---|---|---|---|---|
| **Customer** | `GET /customers/search/basic?query=&type=` → `List<CustomerSummaryresponse>` | — | `GET /customers/by-email?email=` | `GET /customers/{customerId}` |
| **Member** | `GET /members/search/basic?query=` (alias `/members/search/by-name`) → `List<MemberSummaryResponse>` | `POST /members/search` (body `MemberSearchCriteria`, paginé) | `GET /members/by-email?email=` | `GET /members/{id}` |
| **Business** | `GET /businesses/search/basic?query=` → `List<BusinessEntityResponse>` | `POST /businesses/search` (body `BusinessSearchCriteria`, paginé) | `GET /businesses/by-name?name=` | `GET /businesses/{code}` |

Le champ `query` fait un texte libre (nom, email, code…). Utiliser la recherche rapide pour un typeahead/autocomplete ; la recherche avancée (`POST .../search`) pour un écran de filtre avec pagination.

## Enums transverses

- **DocumentOwnerType** : `CUSTOMER, MEMBER, BUSINESS, CONTRACT, INVOICE, PAYMENT, PROPOSAL, ASSET`
- **DocumentSpace** : `KYC_SPACE, CONTRACT_SPACE, FINANCIAL_SPACE, ASSET_SPACE, ADMINISTRATIVE, GENERIC`
- **DocumentCategory** : `KYC, LEGAL, SYSTEM, FINANCIAL, ASSET, OTHER`
- **DocumentStatus** : `DRAFT, UPLOADED, PENDING_REVIEW, NEEDS_CORRECTION, APPROVED, REJECTED, SIGNED, EXPIRED, ARCHIVED, SUPERSEDED`

## Notes générales

- Pagination par défaut sur toutes les listes paginées : `size=20`, triée par date de création/upload décroissante.
- Les endpoints `download`/`preview`/`export/csv`/`export/pdf` renvoient des octets bruts (`Content-Disposition`), pas du JSON — à traiter comme un blob côté frontend.
- Plusieurs routes de création/décision portent `@Idempotent` : envoyer un header `Idempotency-Key` pour sécuriser les retries réseau (recommandé sur les formulaires d'upload et les décisions d'approbation/rejet).
