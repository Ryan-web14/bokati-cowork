# Module Contrat — API Frontend

> Base URL: `/sni/api/v1`
> Auth: Bearer JWT requis sur tous les endpoints sauf ceux marqués **PUBLIC**.
> Content-Type: `application/json`

---

## Table des matières

### PARTIE 1 — Contrats (endpoints existants)

1. [Patterns communs](#1-patterns-communs)
2. [Enums de référence](#2-enums-de-référence)
3. [Templates de contrat (liste)](#3-templates-de-contrat)
4. [Contrats — CRUD](#4-contrats--crud)
5. [Cycle de vie du contrat](#5-cycle-de-vie-du-contrat)
6. [Revue de contrat](#6-revue-de-contrat)
7. [Génération & Prévisualisation PDF](#7-génération--prévisualisation-pdf)
8. [Signature de contrat](#8-signature-de-contrat)
9. [Signature publique (portail client)](#9-signature-publique-portail-client)

### PARTIE 2 — Gestion des templates de contrat

10. [Templates — CRUD complet](#10-templates--crud-complet)
11. [Variables de template](#11-variables-de-template)
12. [Prévisualisation & Duplication de template](#12-prévisualisation--duplication-de-template)

### PARTIE 3 — Rédaction de contrat (Drafting)

13. [Rédaction — CRUD](#13-rédaction--crud)
14. [Sections / Articles](#14-sections--articles)
15. [Prévisualisation & Génération PDF](#15-prévisualisation--génération-pdf-de-rédaction)

### PARTIE 4 — Intégration

16. [Workflows recommandés](#16-workflows-recommandés)
17. [Écrans recommandés](#17-écrans-recommandés)
18. [Codes d'erreur](#18-codes-derreur)

---

## 1. Patterns communs

### Réponse paginée

```json
{
  "content": [ ... ],
  "totalElements": 42,
  "totalPages": 3,
  "page": 0,
  "size": 20,
  "first": true,
  "last": false
}
```

| Param  | Type   | Défaut   | Description                        |
|--------|--------|----------|------------------------------------|
| `page` | int    | `0`      | Numéro de page (0-indexed)         |
| `size` | int    | `20`     | Éléments par page                  |
| `sort` | string | variable | Champ de tri, ex: `createdAt,desc` |

### Résolution automatique

Le backend résout automatiquement les codes (`ownerCode`, `businessCode`, `templateCode`) vers les entités correspondantes. Le frontend n'a jamais besoin d'envoyer des IDs numériques.

### Headers d'idempotence

Les endpoints marqués **Idempotent** acceptent le header :

```
Idempotency-Key: <UUID unique>
```

Cela garantit qu'une requête dupliquée (retry réseau) ne crée pas de doublon.

---

## 2. Enums de référence

### ContractStatus — Statut du contrat

| Valeur               | Description                                                         |
|----------------------|---------------------------------------------------------------------|
| `DRAFT`              | Brouillon, contrat en cours de rédaction                            |
| `GENERATED`          | PDF brouillon généré                                                |
| `UNDER_REVIEW`       | En attente de validation interne                                    |
| `AWAITING_SIGNATURE` | Envoyé pour signature                                               |
| `SIGNED`             | Signé par toutes les parties                                        |
| `ACTIVE`             | Contrat en vigueur                                                  |
| `SUSPENDED`          | Temporairement suspendu                                             |
| `AMENDED`            | Modifié par un avenant — le contrat effectif est l'avenant signé   |
| `EXPIRED`            | Expiré (fin de validité atteinte)                                   |
| `TERMINATED`         | Résilié avant terme                                                 |
| `CANCELLED`          | Annulé (jamais entré en vigueur)                                    |

### AmendmentStatus — Statut d'un avenant

| Valeur              | Description                                                     |
|---------------------|-----------------------------------------------------------------|
| `DRAFT`             | Avenant en cours de rédaction — modifiable                     |
| `UNDER_REVIEW`      | Soumis pour validation interne                                  |
| `PENDING_SIGNATURE` | En attente de signature des parties                             |
| `ACTIVE`            | Avenant signé et entré en vigueur                               |
| `REJECTED`          | Rejeté lors de la revue                                         |
| `CANCELLED`         | Annulé                                                          |

### ContractAmendmentSectionAction — Action sur une section

| Valeur   | Description                                              |
|----------|----------------------------------------------------------|
| `ADD`    | Ajoute un nouveau article/clause au contrat              |
| `MODIFY` | Remplace le contenu d'un article existant                |
| `REMOVE` | Supprime un article existant du contrat                  |

### ContractRenewalType — Type de renouvellement

| Valeur       | Description                          |
|--------------|--------------------------------------|
| `NONE`       | Pas de renouvellement                |
| `FIXED_TERM` | Renouvellement à durée déterminée    |
| `AUTO_RENEW` | Renouvellement automatique (tacite)  |

### ContractPartyRole — Rôle d'une partie au contrat

| Valeur            | Description                              |
|-------------------|------------------------------------------|
| `BENEFICIARY`     | Bénéficiaire du contrat                  |
| `OPERATOR`        | Opérateur / gestionnaire                 |
| `SIGNATORY`       | Signataire habilité                      |
| `BILLING_CONTACT` | Contact facturation                      |

### DocumentOwnerType — Type de propriétaire

| Valeur     | Description           |
|------------|-----------------------|
| `CUSTOMER` | Client (personne)     |
| `MEMBER`   | Membre                |
| `COMPANY`  | Entreprise / business |

### ContractDraftStatus — Statut de rédaction

| Valeur      | Description                              |
|-------------|------------------------------------------|
| `DRAFTING`  | En cours de rédaction                    |
| `READY`     | Prêt pour génération PDF                 |
| `GENERATED` | PDF généré depuis ce brouillon           |

### ContractSectionType — Type de section

| Valeur            | Description                              |
|-------------------|------------------------------------------|
| `PREAMBLE`        | Préambule du contrat                     |
| `ARTICLE`         | Article / clause principale              |
| `CLAUSE`          | Clause secondaire                        |
| `SIGNATURE_BLOCK` | Bloc de signatures                       |
| `ANNEXE`          | Annexe au contrat                        |

### VariableSource — Source de résolution des variables

| Valeur   | Description                                         |
|----------|-----------------------------------------------------|
| `AUTO`   | Résolu automatiquement depuis le contexte (client, business) |
| `MANUAL` | Fourni manuellement par l'utilisateur               |

---

## 3. Templates de contrat

### 3.1 Lister les templates disponibles

```
GET /api/v1/contracts/templates
```

**Réponse** `200 OK`

```json
[
  {
    "code": "TPL-COWORK-001",
    "name": "Contrat de coworking standard",
    "description": "Modèle pour abonnement espace partagé"
  }
]
```

| Champ         | Type   | Description                   |
|---------------|--------|-------------------------------|
| `code`        | string | Code unique du template       |
| `name`        | string | Nom du template               |
| `description` | string | Description du template       |

---

## 4. Contrats — CRUD

### 4.1 Créer un contrat

```
POST /api/v1/contracts
```

**Headers** : `Idempotency-Key: <UUID>` (recommandé)

**Body**

```json
{
  "title": "Contrat Coworking - Société ABC",
  "description": "Contrat pour bureau dédié 3ème étage",
  "templateCode": "TPL-COWORK-001",
  "ownerType": "CUSTOMER",
  "ownerCode": "CUST-00042",
  "businessCode": "BUS-001",
  "renewalType": "AUTO_RENEW",
  "effectiveDate": "2026-07-01",
  "startDate": "2026-07-01",
  "endDate": "2027-06-30",
  "createdBy": 1,
  "parties": [
    {
      "partyType": "CUSTOMER",
      "partyCode": "CUST-00042",
      "displayName": "Société ABC SARL",
      "email": "contact@abc.cg",
      "phone": "+242060000000",
      "role": "BENEFICIARY",
      "signOrder": 1,
      "mustSign": true
    },
    {
      "partyType": "COMPANY",
      "partyCode": "BUS-001",
      "displayName": "Bokati Cowork",
      "email": "admin@bokati.cg",
      "role": "OPERATOR",
      "signOrder": 2,
      "mustSign": true
    }
  ]
}
```

| Champ           | Type               | Requis | Description                         |
|-----------------|--------------------|--------|-------------------------------------|
| `title`         | string             | oui    | Titre du contrat                    |
| `description`   | string             | non    | Description libre                   |
| `templateCode`  | string             | oui    | Code du template à utiliser         |
| `ownerType`     | DocumentOwnerType  | oui    | Type du propriétaire                |
| `ownerCode`     | string             | oui    | Code du propriétaire                |
| `businessCode`  | string             | non    | Code de l'espace/business associé   |
| `renewalType`   | ContractRenewalType| non    | Type de renouvellement              |
| `effectiveDate` | date (ISO)         | non    | Date de prise d'effet               |
| `startDate`     | date (ISO)         | non    | Date de début                       |
| `endDate`       | date (ISO)         | non    | Date de fin                         |
| `createdBy`     | number             | oui    | ID de l'utilisateur créateur        |
| `parties`       | ContractParty[]    | non    | Liste des parties au contrat        |

**Objet `ContractParty` (dans `parties`)**

| Champ         | Type              | Requis | Description                          |
|---------------|-------------------|--------|--------------------------------------|
| `partyType`   | DocumentOwnerType | oui    | Type de la partie                    |
| `partyCode`   | string            | oui    | Code de la partie                    |
| `displayName` | string            | oui    | Nom affiché                          |
| `email`       | string            | non    | Email de la partie                   |
| `phone`       | string            | non    | Téléphone                            |
| `role`        | ContractPartyRole | oui    | Rôle dans le contrat                 |
| `signOrder`   | number            | non    | Ordre de signature (1, 2, 3…)        |
| `mustSign`    | boolean           | non    | Doit obligatoirement signer          |

**Réponse** `201 Created`

```json
{
  "contractCode": "CTR-20260701-00001",
  "title": "Contrat Coworking - Société ABC",
  "description": "Contrat pour bureau dédié 3ème étage",
  "templateCode": "TPL-COWORK-001",
  "ownerType": "CUSTOMER",
  "ownerCode": "CUST-00042",
  "businessCode": "BUS-001",
  "status": "DRAFT",
  "renewalType": "AUTO_RENEW",
  "effectiveDate": "2026-07-01",
  "startDate": "2026-07-01",
  "endDate": "2027-06-30",
  "signedAt": null,
  "activatedAt": null,
  "terminatedAt": null,
  "terminationReason": null,
  "draftDocumentCode": null,
  "signedDocumentCode": null,
  "createdBy": 1,
  "createdAt": "2026-06-20T10:00:00Z",
  "updatedAt": "2026-06-20T10:00:00Z",
  "parties": [
    {
      "partyType": "CUSTOMER",
      "partyCode": "CUST-00042",
      "displayName": "Société ABC SARL",
      "email": "contact@abc.cg",
      "phone": "+242060000000",
      "role": "BENEFICIARY",
      "signOrder": 1,
      "mustSign": true
    }
  ]
}
```

### 4.2 Récupérer un contrat par code

```
GET /api/v1/contracts/{contractCode}
```

| Param          | Type   | Description              |
|----------------|--------|--------------------------|
| `contractCode` | path   | Code unique du contrat   |

**Réponse** `200 OK` → `ContractResponse` (même structure que ci-dessus)

### 4.3 Lister les contrats (avec filtres et pagination)

```
GET /api/v1/contracts
```

| Param          | Type              | Requis | Description                              |
|----------------|-------------------|--------|------------------------------------------|
| `ownerType`    | DocumentOwnerType | non    | Filtrer par type de propriétaire         |
| `ownerCode`    | string            | non    | Filtrer par code propriétaire            |
| `status`       | ContractStatus    | non    | Filtrer par statut                       |
| `businessCode` | string            | non    | Filtrer par business/espace              |
| `templateCode` | string            | non    | Filtrer par template                     |
| `page`         | int               | non    | Page (défaut: 0)                         |
| `size`         | int               | non    | Taille page (défaut: 20)                 |
| `sort`         | string            | non    | Tri, ex: `createdAt,desc`               |

**Réponse** `200 OK` → `PaginatedResponse<ContractResponse>`

```json
{
  "content": [ { /* ContractResponse */ } ],
  "totalElements": 15,
  "totalPages": 1,
  "page": 0,
  "size": 20,
  "first": true,
  "last": true
}
```

### 4.4 Modifier un contrat

> ⚠️ **Immutabilité post-signature** : Cette route retourne `400` si le contrat est en statut `SIGNED`, `ACTIVE`, `SUSPENDED` ou `AMENDED`. Pour modifier un contrat signé, utiliser le workflow d'avenant (Partie 5).

```
PUT /api/v1/contracts/{contractCode}
```

**Headers** : `Idempotency-Key: <UUID>` (recommandé)

**Body** — Seuls les champs à modifier sont envoyés.

```json
{
  "title": "Contrat Coworking - Société ABC (modifié)",
  "description": "Mise à jour description",
  "templateCode": "TPL-COWORK-002",
  "businessCode": "BUS-002",
  "renewalType": "FIXED_TERM",
  "effectiveDate": "2026-08-01",
  "startDate": "2026-08-01",
  "endDate": "2027-07-31",
  "parties": [
    {
      "partyType": "CUSTOMER",
      "partyCode": "CUST-00042",
      "displayName": "Société ABC SARL",
      "email": "nouveau@abc.cg",
      "role": "BENEFICIARY",
      "signOrder": 1,
      "mustSign": true
    }
  ]
}
```

| Champ           | Type               | Requis | Description                          |
|-----------------|--------------------|--------|--------------------------------------|
| `title`         | string             | non    | Nouveau titre                        |
| `description`   | string             | non    | Nouvelle description                 |
| `templateCode`  | string             | non    | Nouveau template                     |
| `businessCode`  | string             | non    | Nouveau business                     |
| `renewalType`   | ContractRenewalType| non    | Nouveau type de renouvellement       |
| `effectiveDate` | date (ISO)         | non    | Nouvelle date d'effet                |
| `startDate`     | date (ISO)         | non    | Nouvelle date de début               |
| `endDate`       | date (ISO)         | non    | Nouvelle date de fin                 |
| `parties`       | ContractParty[]    | non    | Remplace la liste des parties        |

**Réponse** `200 OK` → `ContractResponse`

### 4.5 Supprimer un contrat

```
DELETE /api/v1/contracts/{contractCode}
```

**Réponse** `204 No Content`

> Suppression logique (soft delete). Le contrat n'apparaît plus dans les listes.

---

## 5. Cycle de vie du contrat

### 5.1 Changer le statut (générique)

```
PATCH /api/v1/contracts/{contractCode}/status
```

**Headers** : `Idempotency-Key: <UUID>` (recommandé)

**Body**

```json
{
  "status": "UNDER_REVIEW",
  "reason": "Passage en revue pour validation juridique"
}
```

| Champ    | Type   | Requis | Description                                 |
|----------|--------|--------|---------------------------------------------|
| `status` | string | oui    | Nouveau statut (valeur de ContractStatus)   |
| `reason` | string | non    | Motif du changement                         |

**Réponse** `200 OK` → `ContractResponse`

### 5.2 Activer un contrat

```
PATCH /api/v1/contracts/{contractCode}/activate
```

**Headers** : `Idempotency-Key: <UUID>` (recommandé)

**Réponse** `200 OK` → `ContractResponse`

> Passe le contrat de `SIGNED` à `ACTIVE`. Enregistre `activatedAt`.

### 5.3 Suspendre un contrat

```
PATCH /api/v1/contracts/{contractCode}/suspend
```

**Headers** : `Idempotency-Key: <UUID>` (recommandé)

| Param    | Type   | Requis | In    | Description          |
|----------|--------|--------|-------|----------------------|
| `reason` | string | non    | query | Motif de suspension  |

**Réponse** `200 OK` → `ContractResponse`

> Passe le contrat à `SUSPENDED`.

### 5.4 Résilier un contrat

```
PATCH /api/v1/contracts/{contractCode}/terminate
```

**Headers** : `Idempotency-Key: <UUID>` (recommandé)

| Param    | Type   | Requis | In    | Description           |
|----------|--------|--------|-------|-----------------------|
| `reason` | string | non    | query | Motif de résiliation  |

**Réponse** `200 OK` → `ContractResponse`

> Passe le contrat à `TERMINATED`. Enregistre `terminatedAt` et `terminationReason`.

### 5.5 Annuler un contrat

```
PATCH /api/v1/contracts/{contractCode}/cancel
```

**Headers** : `Idempotency-Key: <UUID>` (recommandé)

| Param    | Type   | Requis | In    | Description          |
|----------|--------|--------|-------|----------------------|
| `reason` | string | non    | query | Motif d'annulation   |

**Réponse** `200 OK` → `ContractResponse`

> Passe le contrat à `CANCELLED`. Utilisé pour les contrats jamais entrés en vigueur.

### Diagramme d'état du contrat

```
DRAFT ──→ GENERATED ──→ UNDER_REVIEW ──→ AWAITING_SIGNATURE ──→ SIGNED ──→ ACTIVE
  │                          │                                               │
  │                          ▼                                               ├──→ SUSPENDED ──→ ACTIVE (réactivation)
  │                     CANCELLED                                            │         │
  │                                                                          │         ▼
  ▼                                                                          ├──→ AMENDED (avenant signé)
CANCELLED                                                                    │         │
                                                                             ├──→ TERMINATED
                                                                             │
                                                                             ▼
                                                                          EXPIRED
```

> **Règle d'or** : Un contrat en statut `SIGNED`, `ACTIVE`, `SUSPENDED` ou `AMENDED` ne peut **jamais** être modifié en place via `PUT /contracts/{code}`. Toute modification post-signature doit passer par un **avenant** (`POST /contracts/{code}/amendments`).

---

## 6. Revue de contrat

### 6.1 Approuver la revue

```
POST /api/v1/contracts/{contractCode}/approve
```

| Param        | Type   | Requis | In    | Description                     |
|--------------|--------|--------|-------|---------------------------------|
| `reviewedBy` | number | oui    | query | ID de l'utilisateur validant    |
| `comment`    | string | non    | query | Commentaire de validation       |

**Réponse** `200 OK` → `ContractResponse`

> Fait passer le contrat de `UNDER_REVIEW` à `AWAITING_SIGNATURE`.

### 6.2 Rejeter la revue

```
POST /api/v1/contracts/{contractCode}/reject-review
```

| Param        | Type   | Requis | In    | Description                    |
|--------------|--------|--------|-------|--------------------------------|
| `reviewedBy` | number | oui    | query | ID de l'utilisateur rejetant   |
| `comment`    | string | non    | query | Commentaire de rejet           |

**Réponse** `200 OK` → `ContractResponse`

> Ramène le contrat à `DRAFT` pour corrections.

---

## 7. Génération & Prévisualisation PDF

### 7.1 Prévisualiser un contrat (HTML)

```
POST /api/v1/contracts/preview
```

**Body**

```json
{
  "templateCode": "TPL-COWORK-001",
  "ownerType": "CUSTOMER",
  "ownerCode": "CUST-00042",
  "businessCode": "BUS-001",
  "title": "Contrat Coworking - Société ABC",
  "description": "Bureau dédié 3ème étage",
  "effectiveDate": "2026-07-01",
  "startDate": "2026-07-01",
  "endDate": "2027-06-30",
  "signatoryName": "Jean Dupont",
  "signatoryRole": "Directeur Général",
  "clauses": [
    "Utilisation exclusive du bureau B-301",
    "Accès 24/7 aux espaces communs"
  ],
  "variables": {
    "montant_mensuel": "150 000 FCFA",
    "adresse_espace": "123 Avenue de la Paix, Brazzaville"
  }
}
```

| Champ            | Type              | Requis | Description                             |
|------------------|-------------------|--------|-----------------------------------------|
| `templateCode`   | string            | oui    | Code du template                        |
| `ownerType`      | DocumentOwnerType | oui    | Type du propriétaire                    |
| `ownerCode`      | string            | oui    | Code du propriétaire                    |
| `businessCode`   | string            | non    | Code du business                        |
| `title`          | string            | oui    | Titre du contrat                        |
| `description`    | string            | non    | Description                             |
| `uploadedBy`     | number            | non    | ID utilisateur                          |
| `effectiveDate`  | date (ISO)        | non    | Date d'effet                            |
| `startDate`      | date (ISO)        | non    | Date de début                           |
| `endDate`        | date (ISO)        | non    | Date de fin                             |
| `signatoryName`  | string            | non    | Nom du signataire                       |
| `signatoryRole`  | string            | non    | Rôle du signataire                      |
| `clauses`        | string[]          | non    | Clauses additionnelles                  |
| `variables`      | Map<string,string> | non   | Variables de template (clé-valeur)      |

**Réponse** `200 OK`

```json
{
  "templateCode": "TPL-COWORK-001",
  "html": "<html><body><h1>Contrat de Coworking</h1>..."
}
```

> Le frontend peut afficher ce HTML dans un iframe ou un conteneur pour prévisualisation.

### 7.2 Générer le PDF brouillon (standalone)

```
POST /api/v1/contracts/generate
```

**Headers** : `Idempotency-Key: <UUID>` (recommandé)

**Body** — Même structure que `preview` ci-dessus.

**Réponse** `201 Created` → `DocumentResponse`

```json
{
  "documentCode": "DOC-20260620-00001",
  "fileName": "Contrat_Coworking_Société_ABC.pdf",
  "fileType": "application/pdf",
  "fileSize": 245000,
  "uploadedAt": "2026-06-20T10:15:00Z"
}
```

> Génère un PDF via le moteur OpenHtmlToPDF et le stocke dans le système de documents (MinIO).

### 7.3 Générer le PDF brouillon depuis un contrat existant

```
POST /api/v1/contracts/{contractCode}/generate-draft
```

**Headers** : `Idempotency-Key: <UUID>` (recommandé)

**Body**

```json
{
  "uploadedBy": 1
}
```

| Champ        | Type   | Requis | Description                  |
|--------------|--------|--------|------------------------------|
| `uploadedBy` | number | oui    | ID de l'utilisateur générant |

**Réponse** `200 OK` → `ContractResponse`

> Le `draftDocumentCode` du contrat est mis à jour. Le statut passe à `GENERATED`.

### 7.4 Marquer comme signé (attacher document signé)

```
PATCH /api/v1/contracts/{contractCode}/signed-document
```

**Headers** : `Idempotency-Key: <UUID>` (recommandé)

**Body**

```json
{
  "documentCode": "DOC-20260625-00005"
}
```

| Champ          | Type   | Requis | Description                                    |
|----------------|--------|--------|------------------------------------------------|
| `documentCode` | string | oui    | Code du document signé (uploadé séparément)    |

**Réponse** `200 OK` → `ContractResponse`

> Met à jour `signedDocumentCode` et passe le statut à `SIGNED`.

---

## 8. Signature de contrat

### 8.1 Demander une signature par email

```
POST /api/v1/contracts/{contractCode}/request-signing
```

**Body**

```json
{
  "signerEmail": "directeur@abc.cg",
  "signerName": "Jean Dupont"
}
```

| Champ        | Type   | Requis | Description                    |
|--------------|--------|--------|--------------------------------|
| `signerEmail`| string | oui    | Email du signataire (validé)   |
| `signerName` | string | oui    | Nom complet du signataire      |

**Réponse** `201 Created`

```json
{
  "token": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "contractCode": "CTR-20260701-00001",
  "contractTitle": "Contrat Coworking - Société ABC",
  "signerEmail": "directeur@abc.cg",
  "signerName": "Jean Dupont",
  "expiresAt": "2026-06-27T10:00:00Z",
  "signedAt": null,
  "revoked": false
}
```

> Un email est envoyé au signataire avec un lien contenant le token de signature. Le contrat passe à `AWAITING_SIGNATURE`.

---

## 9. Signature publique (portail client)

Ces endpoints sont accessibles **sans authentification** (sous `/api/v1/public/`).

### 9.1 Consulter les détails de signature

```
GET /api/v1/public/contracts/sign/{token}
```

| Param   | Type | In   | Description                |
|---------|------|------|----------------------------|
| `token` | UUID | path | Token de signature unique  |

**Réponse** `200 OK` → `ContractSigningResponse`

```json
{
  "token": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "contractCode": "CTR-20260701-00001",
  "contractTitle": "Contrat Coworking - Société ABC",
  "signerEmail": "directeur@abc.cg",
  "signerName": "Jean Dupont",
  "expiresAt": "2026-06-27T10:00:00Z",
  "signedAt": null,
  "revoked": false
}
```

> Permet d'afficher la page de signature avec les informations du contrat.

### 9.2 Signer le contrat

```
POST /api/v1/public/contracts/sign/{token}
```

**Body**

```json
{
  "accepted": true,
  "consentText": "Je confirme avoir lu et accepté les termes du contrat."
}
```

| Champ         | Type    | Requis | Description                                           |
|---------------|---------|--------|-------------------------------------------------------|
| `accepted`    | boolean | oui    | Doit être `true` (acceptation obligatoire)            |
| `consentText` | string  | non    | Texte de consentement libre                           |

**Réponse** `200 OK` → `ContractSigningResponse`

```json
{
  "token": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "contractCode": "CTR-20260701-00001",
  "contractTitle": "Contrat Coworking - Société ABC",
  "signerEmail": "directeur@abc.cg",
  "signerName": "Jean Dupont",
  "expiresAt": "2026-06-27T10:00:00Z",
  "signedAt": "2026-06-22T14:30:00Z",
  "revoked": false
}
```

> Le backend capture automatiquement l'adresse IP et le User-Agent du signataire pour la traçabilité juridique.

---

# PARTIE 2 — Gestion des templates de contrat

---

## 10. Templates — CRUD complet

> Base path: `/api/v1/contract-templates`

### 10.1 Créer un template

```
POST /api/v1/contract-templates
```

**Headers** : `Idempotency-Key: <UUID>` (recommandé)

**Body**

```json
{
  "name": "Contrat de coworking premium",
  "description": "Modèle pour abonnement espace dédié premium",
  "language": "fr",
  "category": "coworking",
  "htmlContent": "<div class=\"contract\"><h1>{{contractTitle}}</h1><p>Entre {{operatorName}} (ci-après \"le Prestataire\") et {{clientName}} (ci-après \"le Bénéficiaire\")...</p><h2>Article 1 — Objet</h2><p>Le présent contrat a pour objet la mise à disposition d'un espace de travail dédié situé à {{operatorAddress}}.</p><h2>Article 2 — Durée</h2><p>Le contrat prend effet le {{startDate}} et se termine le {{endDate}}.</p><h2>Article 3 — Tarification</h2><p>Le montant mensuel est de {{montantMensuel}} payable le {{jourPaiement}} de chaque mois.</p></div>",
  "cssContent": "@page { size: A4; margin: 20mm; } body { font-family: 'Times New Roman', serif; font-size: 11pt; } h1 { text-align: center; font-size: 14pt; } h2 { font-size: 12pt; margin-top: 14pt; text-decoration: underline; }",
  "headerHtml": "<div style=\"text-align:center; border-bottom:2pt solid #111; padding-bottom:10pt;\"><strong>{{operatorName}}</strong><br/><small>{{operatorAddress}}</small></div>",
  "footerHtml": "<div style=\"text-align:center; font-size:8pt; margin-top:30pt; border-top:1pt solid #ccc; padding-top:5pt;\">Document confidentiel — {{operatorName}} — Généré le {{generatedDate}}</div>",
  "variableDefinitions": [
    {
      "key": "clientName",
      "label": "Nom du client",
      "type": "TEXT",
      "required": true,
      "defaultValue": null,
      "source": "AUTO",
      "description": "Résolu depuis le propriétaire du contrat"
    },
    {
      "key": "operatorName",
      "label": "Nom de l'opérateur",
      "type": "TEXT",
      "required": true,
      "defaultValue": null,
      "source": "AUTO",
      "description": "Résolu depuis le business"
    },
    {
      "key": "montantMensuel",
      "label": "Montant mensuel",
      "type": "TEXT",
      "required": true,
      "defaultValue": null,
      "source": "MANUAL",
      "description": "Montant en FCFA à saisir manuellement"
    },
    {
      "key": "jourPaiement",
      "label": "Jour de paiement",
      "type": "TEXT",
      "required": false,
      "defaultValue": "1er",
      "source": "MANUAL",
      "description": "Jour du mois pour le prélèvement"
    }
  ]
}
```

| Champ                  | Type                       | Requis | Description                                       |
|------------------------|----------------------------|--------|---------------------------------------------------|
| `name`                 | string                     | oui    | Nom du template                                   |
| `description`          | string                     | non    | Description                                       |
| `language`             | string                     | non    | Langue (défaut: `fr`)                             |
| `category`             | string                     | non    | Catégorie (ex: coworking, domiciliation, service) |
| `htmlContent`          | string (HTML)              | non    | Corps HTML du contrat avec `{{variables}}`        |
| `cssContent`           | string (CSS)               | non    | Feuille de style personnalisée                    |
| `headerHtml`           | string (HTML)              | non    | En-tête HTML du document                          |
| `footerHtml`           | string (HTML)              | non    | Pied de page HTML du document                     |
| `variableDefinitions`  | VariableDefinition[]       | non    | Définition des variables disponibles              |

**Réponse** `201 Created` → `ContractTemplateResponse`

```json
{
  "code": "CONTRACT_TEMPLATE-00001",
  "name": "Contrat de coworking premium",
  "description": "Modèle pour abonnement espace dédié premium",
  "language": "fr",
  "version": 1,
  "category": "coworking",
  "htmlContent": "...",
  "cssContent": "...",
  "headerHtml": "...",
  "footerHtml": "...",
  "variableDefinitions": [ ... ],
  "active": true,
  "createdAt": "2026-06-20T10:00:00Z",
  "updatedAt": "2026-06-20T10:00:00Z"
}
```

### 10.2 Récupérer un template

```
GET /api/v1/contract-templates/{code}
```

**Réponse** `200 OK` → `ContractTemplateResponse`

### 10.3 Lister les templates (paginé)

```
GET /api/v1/contract-templates
```

| Param  | Type | Requis | Description              |
|--------|------|--------|--------------------------|
| `page` | int  | non    | Page (défaut: 0)         |
| `size` | int  | non    | Taille page (défaut: 20) |

**Réponse** `200 OK` → `PaginatedResponse<ContractTemplateResponse>`

### 10.4 Rechercher des templates

```
GET /api/v1/contract-templates/search?query=coworking
```

| Param   | Type   | Requis | Description                                    |
|---------|--------|--------|------------------------------------------------|
| `query` | string | oui    | Recherche dans nom, description et catégorie   |

**Réponse** `200 OK` → `PaginatedResponse<ContractTemplateResponse>`

### 10.5 Lister par catégorie

```
GET /api/v1/contract-templates/category/{category}
```

**Réponse** `200 OK` → `PaginatedResponse<ContractTemplateResponse>`

### 10.6 Modifier un template

```
PUT /api/v1/contract-templates/{code}
```

**Body** — Seuls les champs à modifier sont envoyés.

```json
{
  "name": "Contrat de coworking premium v2",
  "htmlContent": "<div>...contenu mis à jour...</div>",
  "cssContent": "body { font-size: 12pt; }",
  "variableDefinitions": [ ... ],
  "active": true
}
```

| Champ                  | Type                 | Requis | Description                     |
|------------------------|----------------------|--------|---------------------------------|
| `name`                 | string               | non    | Nouveau nom                     |
| `description`          | string               | non    | Nouvelle description            |
| `language`             | string               | non    | Nouvelle langue                 |
| `category`             | string               | non    | Nouvelle catégorie              |
| `htmlContent`          | string (HTML)        | non    | Nouveau corps HTML              |
| `cssContent`           | string (CSS)         | non    | Nouveau CSS                     |
| `headerHtml`           | string (HTML)        | non    | Nouvel en-tête                  |
| `footerHtml`           | string (HTML)        | non    | Nouveau pied de page            |
| `variableDefinitions`  | VariableDefinition[] | non    | Nouvelles définitions           |
| `active`               | boolean              | non    | Activer/désactiver              |

**Réponse** `200 OK` → `ContractTemplateResponse`

### 10.7 Supprimer un template

```
DELETE /api/v1/contract-templates/{code}
```

**Réponse** `204 No Content`

---

## 11. Variables de template

### Objet `VariableDefinition`

Chaque template peut définir la liste de ses variables avec leur schéma. Le frontend utilise cette définition pour construire dynamiquement le formulaire de saisie.

```json
{
  "key": "montantMensuel",
  "label": "Montant mensuel",
  "type": "TEXT",
  "required": true,
  "defaultValue": "150 000 FCFA",
  "source": "MANUAL",
  "description": "Montant en FCFA à saisir manuellement"
}
```

| Champ          | Type    | Description                                                 |
|----------------|---------|-------------------------------------------------------------|
| `key`          | string  | Clé de la variable (utilisée dans `{{key}}`)               |
| `label`        | string  | Libellé affiché dans le formulaire                         |
| `type`         | string  | Type de champ : `TEXT`, `NUMBER`, `DATE`, `TEXTAREA`       |
| `required`     | boolean | Variable obligatoire                                       |
| `defaultValue` | string  | Valeur par défaut (pré-remplie dans le formulaire)         |
| `source`       | string  | `AUTO` (résolu par le système) ou `MANUAL` (saisie user)   |
| `description`  | string  | Aide contextuelle pour le formulaire                       |

### Variables résolues automatiquement (source: `AUTO`)

Les variables suivantes sont renseignées automatiquement par le backend à partir du propriétaire et du business :

| Variable         | Source                          | Description                    |
|------------------|---------------------------------|--------------------------------|
| `clientName`     | Propriétaire (Member/Customer)  | Nom complet du client          |
| `clientEmail`    | Propriétaire                    | Email du client                |
| `clientPhone`    | Propriétaire                    | Téléphone du client            |
| `clientCode`     | Propriétaire                    | Code unique du client          |
| `clientRccm`     | Propriétaire (Business)         | RCCM si propriétaire business  |
| `operatorName`   | Business                        | Nom de l'opérateur             |
| `businessName`   | Business                        | Nom du business                |
| `businessEmail`  | Business                        | Email du business              |
| `businessPhone`  | Business                        | Téléphone du business          |
| `operatorRccm`   | Business                        | RCCM de l'opérateur            |
| `operatorAddress` | Business                       | Adresse formatée               |
| `startDate`      | Contrat                         | Date de début                  |
| `endDate`        | Contrat                         | Date de fin                    |
| `effectiveDate`  | Contrat                         | Date d'effet                   |
| `contractTitle`  | Contrat                         | Titre du contrat               |
| `generatedDate`  | Système                         | Date du jour de génération     |
| `signatoryName`  | Contrat                         | Nom du signataire              |
| `signatoryRole`  | Contrat                         | Rôle du signataire             |

---

## 12. Prévisualisation & Duplication de template

### 12.1 Prévisualiser un template avec des variables d'exemple

```
POST /api/v1/contract-templates/{code}/preview
```

**Body** (optionnel) — Variables d'exemple pour le rendu

```json
{
  "clientName": "Société ABC SARL",
  "operatorName": "Bokati Cowork",
  "montantMensuel": "150 000 FCFA",
  "startDate": "2026-07-01",
  "endDate": "2027-06-30"
}
```

**Réponse** `200 OK`

```json
{
  "templateCode": "CONTRACT_TEMPLATE-00001",
  "html": "<!DOCTYPE html><html>...HTML résolu avec les variables...</html>"
}
```

> Le frontend affiche ce HTML dans un iframe ou conteneur pour prévisualiser l'aspect final du contrat. Les variables `{{non_fournies}}` restent affichées telles quelles.

### 12.2 Dupliquer un template

```
POST /api/v1/contract-templates/{code}/duplicate
```

**Réponse** `201 Created` → `ContractTemplateResponse`

> Crée une copie du template avec un nouveau code et le suffixe « (copie) » dans le nom.

---

# PARTIE 3 — Rédaction de contrat (Drafting)

---

## 13. Rédaction — CRUD

> Base path: `/api/v1/contract-drafts`

La rédaction permet de composer un contrat complet de zéro ou à partir d'un template, avec des sections modulaires et des variables résolues automatiquement.

### 13.1 Créer une rédaction (de zéro)

```
POST /api/v1/contract-drafts
```

**Headers** : `Idempotency-Key: <UUID>` (recommandé)

**Body**

```json
{
  "title": "Contrat de prestation - Client XYZ",
  "description": "Contrat personnalisé pour prestation de services",
  "ownerType": "CUSTOMER",
  "ownerCode": "CUST-00042",
  "businessCode": "BUS-001",
  "createdBy": 1,
  "cssContent": "body { font-family: Arial, sans-serif; }",
  "headerHtml": "<div style=\"text-align:center\"><h1>{{operatorName}}</h1></div>",
  "footerHtml": "<div style=\"font-size:8pt; text-align:center\">Confidentiel</div>",
  "sections": [
    {
      "title": "Préambule",
      "content": "<p>Le présent contrat est conclu entre {{operatorName}} et {{clientName}}.</p>",
      "sectionType": "PREAMBLE",
      "sectionOrder": 0
    },
    {
      "title": "Article 1 — Objet",
      "content": "<p>Le prestataire s'engage à fournir les services suivants : {{descriptionServices}}.</p>",
      "sectionType": "ARTICLE",
      "sectionOrder": 1
    },
    {
      "title": "Article 2 — Durée",
      "content": "<p>Ce contrat est conclu pour une durée de {{dureeContrat}} à compter du {{startDate}}.</p>",
      "sectionType": "ARTICLE",
      "sectionOrder": 2
    },
    {
      "title": "Signatures",
      "content": "<table width=\"100%\"><tr><td width=\"50%\"><p><strong>Le Prestataire</strong></p><br/><br/><p>{{operatorName}}</p></td><td width=\"50%\"><p><strong>Le Client</strong></p><br/><br/><p>{{clientName}}</p></td></tr></table>",
      "sectionType": "SIGNATURE_BLOCK",
      "sectionOrder": 99
    }
  ]
}
```

| Champ          | Type                         | Requis | Description                              |
|----------------|------------------------------|--------|------------------------------------------|
| `title`        | string                       | oui    | Titre de la rédaction                    |
| `description`  | string                       | non    | Description                              |
| `templateCode` | string                       | non    | Code template d'origine (référence)      |
| `ownerType`    | DocumentOwnerType            | non    | Type du propriétaire (pour résolution auto) |
| `ownerCode`    | string                       | non    | Code du propriétaire                     |
| `businessCode` | string                       | non    | Code business (pour résolution auto)     |
| `createdBy`    | number                       | oui    | ID utilisateur créateur                  |
| `cssContent`   | string (CSS)                 | non    | Style personnalisé                       |
| `headerHtml`   | string (HTML)                | non    | En-tête du document                      |
| `footerHtml`   | string (HTML)                | non    | Pied de page du document                 |
| `sections`     | ContractDraftSectionRequest[]| non    | Sections initiales                       |

**Réponse** `201 Created` → `ContractDraftResponse`

```json
{
  "code": "CONTRACT_DRAFT-00001",
  "title": "Contrat de prestation - Client XYZ",
  "description": "Contrat personnalisé pour prestation de services",
  "templateCode": null,
  "ownerType": "CUSTOMER",
  "ownerCode": "CUST-00042",
  "businessCode": "BUS-001",
  "cssContent": "...",
  "headerHtml": "...",
  "footerHtml": "...",
  "status": "DRAFTING",
  "createdBy": 1,
  "createdAt": "2026-06-20T10:00:00Z",
  "updatedAt": "2026-06-20T10:00:00Z",
  "sections": [
    {
      "id": 1,
      "title": "Préambule",
      "content": "<p>Le présent contrat est conclu entre {{operatorName}} et {{clientName}}.</p>",
      "sectionType": "PREAMBLE",
      "sectionOrder": 0,
      "active": true,
      "createdAt": "2026-06-20T10:00:00Z",
      "updatedAt": "2026-06-20T10:00:00Z"
    }
  ]
}
```

### 13.2 Créer depuis un template

```
POST /api/v1/contract-drafts/from-template/{templateCode}
```

**Headers** : `Idempotency-Key: <UUID>` (recommandé)

**Body** — Même structure que la création standard. Le CSS, header et footer sont pré-remplis depuis le template. Le contenu HTML du template est injecté comme première section.

```json
{
  "title": "Contrat Coworking - Société ABC",
  "ownerType": "CUSTOMER",
  "ownerCode": "CUST-00042",
  "businessCode": "BUS-001",
  "createdBy": 1,
  "sections": [
    {
      "title": "Clause additionnelle",
      "content": "<p>En complément, le bénéficiaire aura accès à {{equipementsSupp}}.</p>",
      "sectionType": "CLAUSE",
      "sectionOrder": 10
    }
  ]
}
```

**Réponse** `201 Created` → `ContractDraftResponse`

> Le contenu HTML du template est inséré comme section principale (sectionOrder=0). Les sections additionnelles s'ajoutent après.

### 13.3 Récupérer une rédaction

```
GET /api/v1/contract-drafts/{code}
```

**Réponse** `200 OK` → `ContractDraftResponse` (avec toutes ses sections)

### 13.4 Lister les rédactions (avec filtres)

```
GET /api/v1/contract-drafts
```

| Param       | Type               | Requis | Description                          |
|-------------|--------------------|--------|--------------------------------------|
| `status`    | ContractDraftStatus| non    | Filtrer par statut                   |
| `ownerType` | DocumentOwnerType  | non    | Filtrer par type de propriétaire     |
| `ownerCode` | string             | non    | Filtrer par code propriétaire        |
| `page`      | int                | non    | Page (défaut: 0)                     |
| `size`      | int                | non    | Taille page (défaut: 20)             |

**Réponse** `200 OK` → `PaginatedResponse<ContractDraftResponse>`

### 13.5 Rechercher des rédactions

```
GET /api/v1/contract-drafts/search?query=prestation
```

**Réponse** `200 OK` → `PaginatedResponse<ContractDraftResponse>`

### 13.6 Modifier une rédaction

```
PUT /api/v1/contract-drafts/{code}
```

**Body** — Champs optionnels. Si `sections` est fourni, il remplace toutes les sections existantes.

```json
{
  "title": "Contrat de prestation v2 - Client XYZ",
  "cssContent": "body { font-family: Georgia, serif; font-size: 11pt; }",
  "sections": [
    {
      "title": "Préambule",
      "content": "<p>Contenu mis à jour...</p>",
      "sectionType": "PREAMBLE",
      "sectionOrder": 0
    }
  ]
}
```

**Réponse** `200 OK` → `ContractDraftResponse`

### 13.7 Changer le statut

```
PATCH /api/v1/contract-drafts/{code}/status?status=READY
```

| Param    | Type               | Requis | In    | Description        |
|----------|--------------------|--------|-------|--------------------|
| `status` | ContractDraftStatus| oui    | query | Nouveau statut     |

**Réponse** `200 OK` → `ContractDraftResponse`

### 13.8 Supprimer une rédaction

```
DELETE /api/v1/contract-drafts/{code}
```

**Réponse** `204 No Content`

---

## 14. Sections / Articles

### 14.1 Ajouter une section

```
POST /api/v1/contract-drafts/{code}/sections
```

**Body**

```json
{
  "title": "Article 3 — Conditions financières",
  "content": "<p>Le montant total de la prestation est de {{montantTotal}} FCFA TTC.</p>",
  "sectionType": "ARTICLE",
  "sectionOrder": 3,
  "active": true
}
```

| Champ          | Type                | Requis | Description                        |
|----------------|---------------------|--------|------------------------------------|
| `title`        | string              | non    | Titre de la section                |
| `content`      | string (HTML)       | oui    | Contenu avec `{{variables}}`       |
| `sectionType`  | ContractSectionType | oui    | Type de section                    |
| `sectionOrder` | number              | non    | Ordre d'affichage (auto si omis)   |
| `active`       | boolean             | non    | Active (défaut: true)              |

**Réponse** `201 Created` → `ContractDraftSectionResponse`

```json
{
  "id": 5,
  "title": "Article 3 — Conditions financières",
  "content": "<p>Le montant total de la prestation est de {{montantTotal}} FCFA TTC.</p>",
  "sectionType": "ARTICLE",
  "sectionOrder": 3,
  "active": true,
  "createdAt": "2026-06-20T11:00:00Z",
  "updatedAt": "2026-06-20T11:00:00Z"
}
```

### 14.2 Modifier une section

```
PUT /api/v1/contract-drafts/{code}/sections/{sectionId}
```

**Body** — Seuls les champs à modifier

```json
{
  "content": "<p>Le montant total révisé est de {{montantRevise}} FCFA TTC.</p>",
  "sectionOrder": 4
}
```

**Réponse** `200 OK` → `ContractDraftSectionResponse`

### 14.3 Supprimer une section

```
DELETE /api/v1/contract-drafts/{code}/sections/{sectionId}
```

**Réponse** `204 No Content`

---

## 15. Prévisualisation & Génération PDF de rédaction

### 15.1 Prévisualiser une rédaction (HTML résolu)

```
POST /api/v1/contract-drafts/{code}/preview
```

**Body** (optionnel) — Variables manuelles supplémentaires

```json
{
  "variables": {
    "montantMensuel": "150 000 FCFA",
    "dureeContrat": "12 mois",
    "descriptionServices": "Bureau dédié + accès salle de réunion"
  }
}
```

**Réponse** `200 OK`

```json
{
  "templateCode": null,
  "html": "<!DOCTYPE html><html>...HTML complet avec variables résolues...</html>"
}
```

> Les variables `AUTO` (clientName, operatorName, etc.) sont résolues depuis le propriétaire et le business.
> Les variables `MANUAL` (montantMensuel, etc.) sont prises du body de la requête.
> Les variables non résolues restent affichées sous forme `{{nomVariable}}`.

### 15.2 Générer le PDF

```
POST /api/v1/contract-drafts/{code}/generate
```

**Headers** : `Idempotency-Key: <UUID>` (recommandé)

| Param        | Type   | Requis | In    | Description                  |
|--------------|--------|--------|-------|------------------------------|
| `uploadedBy` | number | non    | query | ID de l'utilisateur générant |

**Body** (optionnel) — Variables manuelles

```json
{
  "variables": {
    "montantMensuel": "150 000 FCFA",
    "dureeContrat": "12 mois"
  }
}
```

**Réponse** `201 Created` → `DocumentResponse`

```json
{
  "documentCode": "DOC-20260620-00015",
  "fileName": "contrat-de-prestation-client-xyz.pdf",
  "fileType": "application/pdf",
  "fileSize": 185000,
  "uploadedAt": "2026-06-20T11:30:00Z"
}
```

> Le statut de la rédaction passe automatiquement à `GENERATED`. Le PDF est stocké dans MinIO via le module Document.

---

# PARTIE 4 — Intégration

---

## 16. Workflows recommandés

### Workflow 1 — Création et signature complète d'un contrat

```
1. GET  /contracts/templates                    → Charger les templates disponibles
2. POST /contracts/preview                      → Prévisualiser le contrat (HTML)
3. POST /contracts                              → Créer le contrat (DRAFT)
4. POST /contracts/{code}/generate-draft        → Générer le PDF brouillon (GENERATED)
5. PATCH /contracts/{code}/status               → Passer en revue (UNDER_REVIEW)
6. POST /contracts/{code}/approve               → Approuver la revue (AWAITING_SIGNATURE)
7. POST /contracts/{code}/request-signing       → Envoyer le lien de signature par email
   --- Le client signe via le portail public ---
8. GET  /public/contracts/sign/{token}          → [PUBLIC] Afficher page de signature
9. POST /public/contracts/sign/{token}          → [PUBLIC] Signer le contrat (SIGNED)
10. PATCH /contracts/{code}/activate            → Activer le contrat (ACTIVE)
```

### Workflow 2 — Signature manuelle (document physique)

```
1. POST /contracts                              → Créer le contrat (DRAFT)
2. POST /contracts/{code}/generate-draft        → Générer le PDF (GENERATED)
3. --- Impression, signature papier, scan ---
4. --- Upload du scan via module Document ---
5. PATCH /contracts/{code}/signed-document      → Attacher le document signé (SIGNED)
6. PATCH /contracts/{code}/activate             → Activer le contrat (ACTIVE)
```

### Workflow 3 — Génération PDF autonome (sans contrat persisté)

```
1. GET  /contracts/templates                    → Charger les templates
2. POST /contracts/preview                      → Prévisualiser
3. POST /contracts/generate                     → Générer et télécharger le PDF
```

### Workflow 4 — Suspension et réactivation

```
1. PATCH /contracts/{code}/suspend?reason=...   → Suspendre (ACTIVE → SUSPENDED)
2. PATCH /contracts/{code}/activate             → Réactiver (SUSPENDED → ACTIVE)
```

### Workflow 5 — Résiliation anticipée

```
1. PATCH /contracts/{code}/terminate?reason=... → Résilier (ACTIVE → TERMINATED)
```

### Workflow 6 — Rejet et correction

```
1. POST /contracts/{code}/reject-review         → Rejeter (UNDER_REVIEW → DRAFT)
2. PUT  /contracts/{code}                       → Corriger le contrat
3. POST /contracts/{code}/generate-draft        → Régénérer le PDF
4. PATCH /contracts/{code}/status               → Repasser en revue
```

### Workflow 7 — Création complète d'un template personnalisé

```
1. POST /contract-templates                     → Créer le template (HTML + CSS + variables)
2. POST /contract-templates/{code}/preview      → Prévisualiser avec variables d'exemple
3. PUT  /contract-templates/{code}              → Ajuster le contenu si nécessaire
4. POST /contract-templates/{code}/preview      → Re-prévisualiser
   --- Le template est prêt à être utilisé ---
5. POST /contracts                              → Créer un contrat avec ce template
```

### Workflow 8 — Duplication et personnalisation de template

```
1. POST /contract-templates/{code}/duplicate    → Dupliquer un template existant
2. PUT  /contract-templates/{newCode}           → Personnaliser le HTML/CSS/variables
3. POST /contract-templates/{newCode}/preview   → Prévisualiser
```

### Workflow 9 — Rédaction libre de contrat

```
1. POST /contract-drafts                        → Créer une rédaction vide (DRAFTING)
2. POST /contract-drafts/{code}/sections        → Ajouter le préambule
3. POST /contract-drafts/{code}/sections        → Ajouter l'article 1
4. POST /contract-drafts/{code}/sections        → Ajouter l'article 2
5. POST /contract-drafts/{code}/sections        → Ajouter le bloc signatures
6. POST /contract-drafts/{code}/preview         → Prévisualiser avec variables
7. PUT  /contract-drafts/{code}/sections/{id}   → Corriger une section
8. PATCH /contract-drafts/{code}/status?status=READY → Marquer comme prêt
9. POST /contract-drafts/{code}/generate        → Générer le PDF (GENERATED)
```

### Workflow 10 — Rédaction depuis un template

```
1. GET  /contract-templates                     → Choisir un template
2. POST /contract-drafts/from-template/{code}   → Créer la rédaction pré-remplie
3. POST /contract-drafts/{code}/sections        → Ajouter des sections complémentaires
4. PUT  /contract-drafts/{code}/sections/{id}   → Modifier les sections héritées
5. POST /contract-drafts/{code}/preview         → Prévisualiser
6. POST /contract-drafts/{code}/generate        → Générer le PDF
```

---

## 17. Écrans recommandés

### 17.1 Liste des contrats

**Route suggérée** : `/admin/contracts`

| Composant            | Description                                                    |
|----------------------|----------------------------------------------------------------|
| Tableau paginé       | Colonnes : Code, Titre, Client, Statut, Type renouv., Dates   |
| Filtres              | Par statut, propriétaire, business, template                   |
| Badges couleur       | Vert=ACTIVE, Jaune=DRAFT, Orange=UNDER_REVIEW, Rouge=TERMINATED |
| Actions rapides      | Voir, Modifier, Changer statut                                 |
| Bouton               | « + Nouveau contrat »                                         |

### 17.2 Détail d'un contrat

**Route suggérée** : `/admin/contracts/:contractCode`

| Section              | Description                                                    |
|----------------------|----------------------------------------------------------------|
| En-tête              | Titre, code, statut (badge), dates                             |
| Informations         | Template, propriétaire, business, type de renouvellement       |
| Parties              | Tableau des parties (nom, rôle, email, doit signer)            |
| Documents            | Lien vers le PDF brouillon et/ou signé                         |
| Timeline             | Historique des changements de statut (via audit)               |
| Actions              | Boutons contextuels selon le statut courant                    |

**Actions contextuelles par statut :**

| Statut actuel        | Actions disponibles                                            |
|----------------------|----------------------------------------------------------------|
| `DRAFT`              | Modifier, Générer PDF, Supprimer                               |
| `GENERATED`          | Passer en revue, Régénérer PDF                                 |
| `UNDER_REVIEW`       | Approuver, Rejeter                                             |
| `AWAITING_SIGNATURE` | Envoyer lien signature, Attacher doc signé                     |
| `SIGNED`             | Activer                                                        |
| `ACTIVE`             | Suspendre, Résilier                                            |
| `SUSPENDED`          | Réactiver, Résilier                                            |

### 17.3 Formulaire de création / modification de contrat

**Route suggérée** : `/admin/contracts/new` et `/admin/contracts/:contractCode/edit`

| Section              | Description                                                    |
|----------------------|----------------------------------------------------------------|
| Infos générales      | Titre, description, template (dropdown), dates                 |
| Propriétaire         | Sélection type + recherche par code                            |
| Business             | Dropdown des espaces disponibles                               |
| Renouvellement       | Radio buttons (Aucun / Durée déterminée / Tacite)              |
| Parties              | Formulaire dynamique (ajouter/retirer des parties)             |
| Prévisualisation     | Bouton pour afficher l'aperçu HTML dans un modal/drawer        |

### 17.4 Page de signature publique

**Route suggérée** : `/sign/:token` (page publique, sans layout admin)

| Section              | Description                                                    |
|----------------------|----------------------------------------------------------------|
| En-tête              | Logo, titre du contrat                                         |
| Infos signataire     | Nom, email (pré-rempli depuis le token)                        |
| PDF viewer           | Affichage du contrat PDF à lire                                |
| Consentement         | Checkbox d'acceptation + champ texte optionnel                 |
| Bouton signer        | Action finale, désactivé si non accepté                        |
| Confirmation         | Message de succès avec date de signature                       |
| Expiration           | Message d'erreur si le token est expiré ou révoqué             |

### 17.5 Écran de revue

**Route suggérée** : `/admin/contracts/:contractCode/review`

| Section              | Description                                                    |
|----------------------|----------------------------------------------------------------|
| Résumé contrat       | Informations clés en lecture seule                              |
| PDF viewer           | Affichage du brouillon PDF                                     |
| Parties              | Liste des parties                                              |
| Actions              | Approuver (avec commentaire opt.) / Rejeter (avec commentaire) |

### 17.6 Gestion des templates

**Route suggérée** : `/admin/contract-templates`

| Composant            | Description                                                    |
|----------------------|----------------------------------------------------------------|
| Tableau paginé       | Colonnes : Code, Nom, Catégorie, Langue, Version, Actif       |
| Filtres              | Par catégorie, par état actif                                  |
| Recherche            | Barre de recherche (nom, description, catégorie)               |
| Actions              | Voir, Modifier, Dupliquer, Supprimer                           |
| Bouton               | « + Nouveau template »                                        |

### 17.7 Éditeur de template

**Route suggérée** : `/admin/contract-templates/:code/edit`

| Section              | Description                                                    |
|----------------------|----------------------------------------------------------------|
| Infos générales      | Nom, description, catégorie (dropdown), langue (select)        |
| Éditeur HTML         | Éditeur de code pour le corps HTML (`htmlContent`)             |
| Éditeur CSS          | Éditeur de code pour les styles (`cssContent`)                 |
| En-tête / Pied       | Éditeurs séparés pour `headerHtml` et `footerHtml`             |
| Variables            | Tableau dynamique des définitions de variables                 |
| Prévisualisation     | Panel latéral ou modal montrant le rendu HTML en temps réel    |
| Actions              | Sauvegarder, Prévisualiser, Dupliquer                          |

**Éditeur de variables :**

| Colonne      | Input                    | Description                        |
|--------------|--------------------------|------------------------------------|
| Clé          | Text input               | Nom technique (ex: `montantMensuel`) |
| Libellé      | Text input               | Label affiché (ex: « Montant mensuel ») |
| Type         | Select (TEXT/NUMBER/DATE) | Type de champ dans le formulaire   |
| Obligatoire  | Switch                   | Variable requise                   |
| Défaut       | Text input               | Valeur pré-remplie                 |
| Source       | Select (AUTO/MANUAL)     | Résolue par le système ou manuelle |
| Description  | Text input               | Aide contextuelle                  |

### 17.8 Liste des rédactions

**Route suggérée** : `/admin/contract-drafts`

| Composant            | Description                                                    |
|----------------------|----------------------------------------------------------------|
| Tableau paginé       | Colonnes : Code, Titre, Client, Statut, Template, Mis à jour  |
| Filtres              | Par statut (DRAFTING/READY/GENERATED), propriétaire            |
| Recherche            | Barre de recherche (titre, description)                        |
| Badges               | Bleu=DRAFTING, Vert=READY, Gris=GENERATED                     |
| Actions              | Voir, Modifier, Prévisualiser, Générer PDF                     |
| Boutons              | « + Nouvelle rédaction » / « + Depuis un template »            |

### 17.9 Éditeur de rédaction

**Route suggérée** : `/admin/contract-drafts/:code/edit`

| Section              | Description                                                    |
|----------------------|----------------------------------------------------------------|
| Infos générales      | Titre, description, propriétaire, business                     |
| Style global         | Éditeur CSS, en-tête HTML, pied de page HTML                   |
| Sections             | Liste ordonnée des sections avec drag & drop                   |
| Ajouter section      | Bouton → Formulaire : titre, type (select), contenu (éditeur) |
| Par section          | Modifier, Réordonner, Activer/Désactiver, Supprimer            |
| Variables            | Formulaire des variables MANUAL à renseigner                   |
| Prévisualisation     | Panel en temps réel ou bouton → modal plein écran              |
| Actions              | Sauvegarder, Prévisualiser, Marquer prêt, Générer PDF          |

**Éditeur de section :**

| Champ         | Input                     | Description                             |
|---------------|---------------------------|-----------------------------------------|
| Titre         | Text input                | Titre de la section                     |
| Type          | Select dropdown           | PREAMBLE / ARTICLE / CLAUSE / SIGNATURE_BLOCK / ANNEXE |
| Contenu       | Éditeur HTML riche        | Contenu avec bouton « Insérer variable » |
| Ordre         | Number input ou drag      | Position dans le document                |
| Actif         | Switch                    | Inclure dans le rendu                   |

**Bouton « Insérer variable »** : Affiche un dropdown des variables disponibles (AUTO + MANUAL). Insère `{{variableName}}` à la position du curseur dans l'éditeur.

---

# PARTIE 5 — Avenants et Audit Trail

---

> Un contrat signé est un fait juridique immuable. Toute modification d'un contrat `ACTIVE` ou `SUSPENDED` passe obligatoirement par un **avenant** (`ContractAmendment`). L'avenant porte ses propres sections (ajout, modification, suppression d'articles) et variables. Une fois signé, l'avenant entre en vigueur et le contrat original passe à `AMENDED`.

---

## 19. Avenants — Cycle de vie

> Base paths : `/api/v1/contracts/{contractCode}/amendments` et `/api/v1/amendments/{amendmentCode}`

### Diagramme d'état d'un avenant

```
DRAFT ──→ UNDER_REVIEW ──→ PENDING_SIGNATURE ──→ ACTIVE
  │              │
  ▼              ▼
CANCELLED     REJECTED
```

Les transitions autorisées :

| De                  | Vers                | Action / endpoint                            |
|---------------------|---------------------|----------------------------------------------|
| `DRAFT`             | `UNDER_REVIEW`      | `POST /amendments/{code}/submit`             |
| `DRAFT`             | `CANCELLED`         | `POST /amendments/{code}/cancel`             |
| `UNDER_REVIEW`      | `PENDING_SIGNATURE` | `POST /amendments/{code}/approve`            |
| `UNDER_REVIEW`      | `REJECTED`          | `POST /amendments/{code}/reject`             |
| `UNDER_REVIEW`      | `CANCELLED`         | `POST /amendments/{code}/cancel`             |
| `PENDING_SIGNATURE` | `ACTIVE`            | `POST /amendments/{code}/sign`               |
| `PENDING_SIGNATURE` | `CANCELLED`         | `POST /amendments/{code}/cancel`             |

> Quand un avenant passe à `ACTIVE`, le contrat original passe automatiquement à `AMENDED`.

---

### 19.1 Proposer un avenant

```
POST /api/v1/contracts/{contractCode}/amendments
```

**Pré-condition** : le contrat doit être `ACTIVE` ou `SUSPENDED`. Un seul avenant ouvert (DRAFT/UNDER_REVIEW/PENDING_SIGNATURE) par contrat à la fois.

**Query params**

| Param        | Type   | Requis | Description                         |
|--------------|--------|--------|-------------------------------------|
| `proposedBy` | number | oui    | ID de l'utilisateur proposant       |

**Body**

```json
{
  "description": "Modification du montant mensuel suite à la révision tarifaire 2026",
  "effectiveDate": "2026-08-01"
}
```

| Champ           | Type       | Requis | Description                                     |
|-----------------|------------|--------|-------------------------------------------------|
| `description`   | string     | oui    | Objet de l'avenant (raison de la modification)  |
| `effectiveDate` | date (ISO) | non    | Date d'entrée en vigueur de l'avenant           |

**Réponse** `201 Created` → `ContractAmendmentResponse`

```json
{
  "code": "AMENDMENT-20260629-00001",
  "originalContractCode": "CTR-20260701-00001",
  "status": "DRAFT",
  "description": "Modification du montant mensuel suite à la révision tarifaire 2026",
  "proposedBy": 1,
  "proposedAt": "2026-06-29T10:00:00Z",
  "effectiveDate": "2026-08-01",
  "draftDocumentCode": null,
  "signedDocumentCode": null,
  "signedAt": null,
  "activatedAt": null,
  "reviewedBy": null,
  "reviewComment": null,
  "rejectionReason": null,
  "cancellationReason": null,
  "createdAt": "2026-06-29T10:00:00Z",
  "updatedAt": "2026-06-29T10:00:00Z",
  "sections": [],
  "variableChanges": []
}
```

---

### 19.2 Récupérer un avenant

```
GET /api/v1/amendments/{amendmentCode}
```

**Réponse** `200 OK` → `ContractAmendmentResponse` (avec sections et variableChanges)

---

### 19.3 Lister les avenants d'un contrat

```
GET /api/v1/contracts/{contractCode}/amendments
```

**Réponse** `200 OK` → `ContractAmendmentResponse[]` (triés du plus récent au plus ancien)

---

### 19.4 Soumettre pour revue

```
POST /api/v1/amendments/{amendmentCode}/submit
```

**Pré-condition** : L'avenant doit être en `DRAFT` et contenir au moins une modification de section ou de variable.

**Réponse** `200 OK` → `ContractAmendmentResponse` (status = `UNDER_REVIEW`)

---

### 19.5 Approuver la revue

```
POST /api/v1/amendments/{amendmentCode}/approve
```

| Param        | Type   | Requis | In    | Description                    |
|--------------|--------|--------|-------|--------------------------------|
| `reviewedBy` | number | oui    | query | ID du réviseur                 |

**Body** (optionnel)

```json
{
  "comment": "Avenant conforme à l'accord verbal du 28/06/2026"
}
```

**Réponse** `200 OK` → `ContractAmendmentResponse` (status = `PENDING_SIGNATURE`)

---

### 19.6 Rejeter la revue

```
POST /api/v1/amendments/{amendmentCode}/reject
```

| Param        | Type   | Requis | In    | Description                 |
|--------------|--------|--------|-------|-----------------------------|
| `reviewedBy` | number | oui    | query | ID du réviseur              |

**Body** (optionnel)

```json
{
  "comment": "Le montant proposé ne respecte pas la grille tarifaire approuvée"
}
```

**Réponse** `200 OK` → `ContractAmendmentResponse` (status = `REJECTED`)

---

### 19.7 Signer l'avenant (entrée en vigueur)

```
POST /api/v1/amendments/{amendmentCode}/sign
```

| Param     | Type   | Requis | In    | Description                      |
|-----------|--------|--------|-------|----------------------------------|
| `actorId` | number | oui    | query | ID de la personne qui signe      |

**Body** (optionnel)

```json
{
  "signedDocumentCode": "DOC-20260629-00003",
  "justification": "Avenant signé physiquement et scanné"
}
```

| Champ                | Type   | Requis | Description                                  |
|----------------------|--------|--------|----------------------------------------------|
| `signedDocumentCode` | string | non    | Code du document signé (upload séparé)       |
| `justification`      | string | non    | Commentaire sur la signature                 |

**Réponse** `200 OK` → `ContractAmendmentResponse` (status = `ACTIVE`)

> Le contrat original passe automatiquement à `AMENDED`. Un événement d'audit est enregistré pour les deux.

---

### 19.8 Annuler un avenant

```
POST /api/v1/amendments/{amendmentCode}/cancel
```

| Param     | Type   | Requis | In    | Description                  |
|-----------|--------|--------|-------|------------------------------|
| `actorId` | number | oui    | query | ID de l'utilisateur          |

**Body** (optionnel)

```json
{
  "reason": "Négociations abandonnées"
}
```

**Réponse** `200 OK` → `ContractAmendmentResponse` (status = `CANCELLED`)

---

## 20. Modifications de contenu d'un avenant (en DRAFT)

> Toutes les routes de cette section ne sont accessibles que lorsque l'avenant est en statut `DRAFT`.

---

### 20.1 Ajouter une modification de section

```
POST /api/v1/amendments/{amendmentCode}/sections
```

**Body**

```json
{
  "action": "MODIFY",
  "targetSectionRef": "Article 3 — Tarification",
  "sectionType": "ARTICLE",
  "title": "Article 3 — Tarification (révisée)",
  "content": "<p>À compter du {{effectiveDate}}, le montant mensuel est porté à <strong>175 000 FCFA TTC</strong>, payable le 1er de chaque mois.</p>",
  "sectionOrder": 3
}
```

| Champ              | Type                          | Requis | Description                                                  |
|--------------------|-------------------------------|--------|--------------------------------------------------------------|
| `action`           | ContractAmendmentSectionAction| oui    | `ADD`, `MODIFY` ou `REMOVE`                                  |
| `targetSectionRef` | string                        | cond.  | Référence de la section originale (requis pour MODIFY/REMOVE) |
| `sectionType`      | ContractSectionType           | non    | Type de section (défaut: `ARTICLE`)                          |
| `title`            | string                        | non    | Nouveau titre de la section                                  |
| `content`          | string (HTML)                 | cond.  | Nouveau contenu avec `{{variables}}` (requis sauf REMOVE)    |
| `sectionOrder`     | number                        | non    | Ordre d'affichage dans l'avenant (auto si omis)              |

**Exemples par action :**

**ADD** — Nouveau article qui n'existait pas dans le contrat original :
```json
{
  "action": "ADD",
  "sectionType": "CLAUSE",
  "title": "Clause de confidentialité renforcée",
  "content": "<p>En complément de l'article 7, le bénéficiaire s'engage à une clause de non-divulgation étendue à 24 mois après résiliation.</p>",
  "sectionOrder": 8
}
```

**MODIFY** — Remplacement d'un article existant :
```json
{
  "action": "MODIFY",
  "targetSectionRef": "Article 3 — Tarification",
  "title": "Article 3 — Tarification (révisée)",
  "content": "<p>Le montant mensuel est porté à 175 000 FCFA TTC à compter du {{effectiveDate}}.</p>"
}
```

**REMOVE** — Suppression d'un article :
```json
{
  "action": "REMOVE",
  "targetSectionRef": "Article 6 — Clause d'exclusivité"
}
```

**Réponse** `201 Created` → `ContractAmendmentSectionResponse`

```json
{
  "id": 12,
  "action": "MODIFY",
  "targetSectionRef": "Article 3 — Tarification",
  "sectionType": "ARTICLE",
  "title": "Article 3 — Tarification (révisée)",
  "content": "<p>Le montant mensuel est porté à 175 000 FCFA TTC...</p>",
  "sectionOrder": 3,
  "createdAt": "2026-06-29T10:05:00Z",
  "updatedAt": "2026-06-29T10:05:00Z"
}
```

---

### 20.2 Modifier une section d'avenant

```
PUT /api/v1/amendments/{amendmentCode}/sections/{sectionId}
```

**Body** — Seuls les champs à modifier

```json
{
  "content": "<p>Montant révisé : 180 000 FCFA TTC à compter du {{effectiveDate}}.</p>",
  "title": "Article 3 — Tarification (révisée v2)"
}
```

**Réponse** `200 OK` → `ContractAmendmentSectionResponse`

---

### 20.3 Lister les sections d'un avenant

```
GET /api/v1/amendments/{amendmentCode}/sections
```

**Réponse** `200 OK` → `ContractAmendmentSectionResponse[]` (triés par sectionOrder)

---

### 20.4 Supprimer une section d'avenant

```
DELETE /api/v1/amendments/{amendmentCode}/sections/{sectionId}
```

**Réponse** `204 No Content`

---

### 20.5 Définir les changements de variables

> Remplace entièrement la liste des modifications de variables pour l'avenant.

```
PUT /api/v1/amendments/{amendmentCode}/variables
```

**Body** — Tableau de changements (remplace tout)

```json
[
  {
    "variableKey": "montantMensuel",
    "previousValue": "150 000 FCFA",
    "newValue": "175 000 FCFA"
  },
  {
    "variableKey": "jourPaiement",
    "previousValue": "1er",
    "newValue": "5"
  }
]
```

| Champ           | Type   | Requis | Description                                          |
|-----------------|--------|--------|------------------------------------------------------|
| `variableKey`   | string | oui    | Clé de la variable (doit correspondre au template)   |
| `previousValue` | string | non    | Valeur actuelle dans le contrat (pour audit)         |
| `newValue`      | string | oui    | Nouvelle valeur proposée                             |

**Réponse** `200 OK` → `ContractAmendmentVariableResponse[]`

```json
[
  {
    "id": 5,
    "variableKey": "montantMensuel",
    "previousValue": "150 000 FCFA",
    "newValue": "175 000 FCFA",
    "createdAt": "2026-06-29T10:10:00Z",
    "updatedAt": "2026-06-29T10:10:00Z"
  },
  {
    "id": 6,
    "variableKey": "jourPaiement",
    "previousValue": "1er",
    "newValue": "5",
    "createdAt": "2026-06-29T10:10:00Z",
    "updatedAt": "2026-06-29T10:10:00Z"
  }
]
```

---

### 20.6 Lister les changements de variables

```
GET /api/v1/amendments/{amendmentCode}/variables
```

**Réponse** `200 OK` → `ContractAmendmentVariableResponse[]` (triés par variableKey)

---

## 21. Prévisualisation & PDF d'avenant

### 21.1 Prévisualiser l'avenant (HTML)

```
GET /api/v1/amendments/{amendmentCode}/preview
```

**Réponse** `200 OK` `text/html`

> Retourne un HTML complet représentant l'avenant avec :
> - En-tête avec référence de l'avenant et du contrat original
> - Objet de l'avenant
> - Toutes les modifications de sections (avec badges ADD/MODIFY/REMOVE en couleur)
> - Tableau des changements de variables (ancienne vs nouvelle valeur)
> - Clause de sauvegarde légale
> - Bloc de signatures

Le frontend peut afficher ce HTML dans un `<iframe>` ou un conteneur dédié.

---

### 21.2 Générer le PDF de l'avenant

```
POST /api/v1/amendments/{amendmentCode}/generate-pdf
```

| Param        | Type   | Requis | In    | Description                      |
|--------------|--------|--------|-------|----------------------------------|
| `uploadedBy` | number | oui    | query | ID de l'utilisateur générant     |

**Réponse** `200 OK` → `DocumentResponse`

```json
{
  "documentCode": "DOC-20260629-00010",
  "fileName": "avenant-amendment-20260629-00001.pdf",
  "fileType": "application/pdf",
  "fileSize": 152000,
  "uploadedAt": "2026-06-29T10:20:00Z"
}
```

> Le `draftDocumentCode` de l'avenant est mis à jour avec ce code. Le PDF est stocké dans MinIO.

---

## 22. Piste d'audit chaînée par hash

### 22.1 Récupérer la piste d'audit d'un contrat

```
GET /api/v1/contracts/{contractCode}/audit-events
```

**Réponse** `200 OK` → `ContractAuditEventResponse[]` (triés par date d'événement, du plus ancien au plus récent)

```json
[
  {
    "id": 1,
    "contractCode": "CTR-20260701-00001",
    "amendmentCode": null,
    "eventType": "AMENDMENT_PROPOSED",
    "actorId": 1,
    "actorType": "USER",
    "actorName": null,
    "occurredAt": "2026-06-29T10:00:00Z",
    "previousHash": null,
    "eventHash": "a3f4e2b1c8d7e6f5a4b3c2d1e0f9a8b7c6d5e4f3a2b1c0d9e8f7a6b5c4d3e2",
    "payload": "{\"amendmentCode\":\"AMENDMENT-20260629-00001\"}",
    "justification": "Avenant proposé : Modification du montant mensuel"
  },
  {
    "id": 2,
    "contractCode": "CTR-20260701-00001",
    "amendmentCode": "AMENDMENT-20260629-00001",
    "eventType": "AMENDMENT_SUBMITTED_FOR_REVIEW",
    "actorId": null,
    "actorType": "SYSTEM",
    "actorName": null,
    "occurredAt": "2026-06-29T10:30:00Z",
    "previousHash": "a3f4e2b1c8d7e6f5a4b3c2d1e0f9a8b7c6d5e4f3a2b1c0d9e8f7a6b5c4d3e2",
    "eventHash": "b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2",
    "payload": null,
    "justification": "Avenant soumis pour examen"
  }
]
```

| Champ           | Type    | Description                                                         |
|-----------------|---------|---------------------------------------------------------------------|
| `id`            | number  | Identifiant unique de l'événement                                   |
| `contractCode`  | string  | Code du contrat concerné                                            |
| `amendmentCode` | string  | Code de l'avenant concerné (null si événement sur le contrat lui-même) |
| `eventType`     | string  | Type d'événement (voir tableau ci-dessous)                          |
| `actorId`       | number  | ID de l'utilisateur ayant déclenché l'événement                     |
| `actorType`     | string  | `USER` ou `SYSTEM`                                                  |
| `actorName`     | string  | Nom de l'acteur (peut être null)                                    |
| `occurredAt`    | instant | Timestamp précis de l'événement                                     |
| `previousHash`  | string  | SHA-256 de l'événement précédent (null pour le premier)             |
| `eventHash`     | string  | SHA-256 de cet événement (chaîné avec le précédent)                 |
| `payload`       | string  | JSON optionnel avec des données supplémentaires                     |
| `justification` | string  | Motif ou commentaire de l'événement                                 |

### Types d'événements d'audit

| eventType                       | Déclencheur                                           |
|---------------------------------|-------------------------------------------------------|
| `AMENDMENT_PROPOSED`            | Nouvel avenant créé                                   |
| `AMENDMENT_SUBMITTED_FOR_REVIEW`| Soumission pour revue                                 |
| `AMENDMENT_REVIEW_APPROVED`     | Revue approuvée                                       |
| `AMENDMENT_REVIEW_REJECTED`     | Revue rejetée                                         |
| `AMENDMENT_SIGNED`              | Avenant signé                                         |
| `AMENDMENT_CANCELLED`           | Avenant annulé                                        |
| `CONTRACT_AMENDED`              | Contrat original basculé en AMENDED suite à signature |

### Vérification de l'intégrité de la chaîne

Chaque `eventHash` est calculé :
```
SHA-256(previousHash | contractCode | eventType | actorId | timestamp | payload)
```
Si `previousHash` de l'événement N+1 ≠ `eventHash` de l'événement N, la chaîne a été altérée.

---

## 23. Workflows d'avenant recommandés

### Workflow A — Modification tarifaire

```
1. POST /contracts/{code}/amendments?proposedBy=1           → Proposer l'avenant (DRAFT)
2. PUT  /amendments/{amendCode}/variables                   → Définir les nouvelles valeurs
3. POST /amendments/{amendCode}/sections                    → Modifier l'article "Tarification"
4. GET  /amendments/{amendCode}/preview                     → Prévisualiser l'avenant HTML
5. POST /amendments/{amendCode}/generate-pdf?uploadedBy=1  → Générer le PDF
6. POST /amendments/{amendCode}/submit                      → Soumettre pour revue (UNDER_REVIEW)
7. POST /amendments/{amendCode}/approve?reviewedBy=2        → Approuver (PENDING_SIGNATURE)
8. POST /amendments/{amendCode}/sign?actorId=1              → Signer (ACTIVE)
   → Contrat original passe automatiquement en AMENDED
```

### Workflow B — Ajout d'une clause

```
1. POST /contracts/{code}/amendments?proposedBy=1           → Proposer l'avenant
2. POST /amendments/{amendCode}/sections                    → ADD: nouvelle clause
3. POST /amendments/{amendCode}/submit                      → Soumettre
4. POST /amendments/{amendCode}/approve?reviewedBy=2        → Approuver
5. POST /amendments/{amendCode}/sign?actorId=1              → Signer
```

### Workflow C — Avenant rejeté, corrigé, resigné

```
1. POST /contracts/{code}/amendments                        → Proposer (DRAFT)
2. PUT  /amendments/{amendCode}/variables                   → Définir les changements
3. POST /amendments/{amendCode}/submit                      → Soumettre (UNDER_REVIEW)
4. POST /amendments/{amendCode}/reject?reviewedBy=2         → Rejeter (REJECTED)
   → L'avenant est figé en REJECTED — créer un nouvel avenant si nécessaire
5. POST /contracts/{code}/amendments                        → Nouveau brouillon corrigé
6. [Répéter les étapes 2-3-approve-sign]
```

---

## 24. Écrans recommandés — Avenants

### 24.1 Onglet « Avenants » dans le détail d'un contrat

**Route** : `/admin/contracts/:contractCode` → onglet « Avenants »

| Composant              | Description                                                          |
|------------------------|----------------------------------------------------------------------|
| Liste des avenants     | Tableau : Code, Date, Statut, Proposé par, Date entrée en vigueur   |
| Badges statut          | Bleu=DRAFT, Orange=UNDER_REVIEW, Violet=PENDING_SIGNATURE, Vert=ACTIVE, Rouge=REJECTED/CANCELLED |
| Bouton                 | « + Proposer un avenant » (visible si contrat ACTIVE/SUSPENDED)      |
| Actions par avenant    | Voir, Modifier (si DRAFT), Soumettre, Approuver, Rejeter, Signer     |

### 24.2 Éditeur d'avenant

**Route** : `/admin/amendments/:amendmentCode/edit`

| Section                | Description                                                          |
|------------------------|----------------------------------------------------------------------|
| En-tête                | Objet de l'avenant + statut (badge)                                  |
| Contrat référencé      | Code + titre du contrat original (lien vers fiche)                   |
| Date d'effet           | Date picker                                                          |
| Modifications de sections | Tableau des changements avec actions ADD/MODIFY/REMOVE + éditeur HTML |
| Variables modifiées    | Tableau : Variable / Ancienne valeur / Nouvelle valeur               |
| Prévisualisation       | Bouton → ouverture HTML dans iframe / panel latéral                  |
| Générer PDF            | Bouton → téléchargement PDF de l'avenant                             |
| Actions                | Soumettre / Annuler (selon statut)                                   |

**Éditeur de modification de section :**

| Champ               | Input                  | Description                                     |
|---------------------|------------------------|-------------------------------------------------|
| Action              | Select (ADD/MODIFY/REMOVE) | Type de modification                        |
| Section visée       | Text (autocomplete)    | Titre de la section originale à modifier/supprimer |
| Type               | Select (ContractSectionType) | Type de la nouvelle section                |
| Titre               | Text input             | Nouveau titre                                   |
| Contenu             | Éditeur HTML riche     | Nouveau contenu avec `{{variables}}`            |

### 24.3 Piste d'audit d'un contrat

**Route** : `/admin/contracts/:contractCode` → onglet « Piste d'audit »

| Composant             | Description                                                           |
|-----------------------|-----------------------------------------------------------------------|
| Timeline verticale    | Événements chronologiques (du plus récent au plus ancien)             |
| Par événement         | Type, acteur, date, justification, lien vers avenant si applicable    |
| Hash                  | Affichage condensé du hash (6 premiers caractères) avec tooltip complet |
| Vérification          | Indicateur « Chaîne intègre » / « Chaîne compromise »                 |

---

## 18. Codes d'erreur

| Code HTTP | Situation                                           |
|-----------|-----------------------------------------------------|
| `400`     | Données invalides, champs requis manquants          |
| `404`     | Contrat, template, rédaction ou propriétaire non trouvé |
| `409`     | Transition de statut invalide                       |
| `422`     | Règle métier violée (ex: signer un contrat expiré)  |

**Format d'erreur standard**

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Le champ 'title' est obligatoire",
  "errorCode": "VALIDATION_ERROR",
  "timestamp": "2026-06-20T10:00:00Z"
}
```
