# Plan d'amélioration — Module KYC & Upload de documents

> Date : 2026-07-01  
> Branche cible : `feature/kyc-dynamic-upload`  
> Périmètre : `features/document/`, `features/portal/`, `core/`

---

## Table des matières

1. [Contexte et objectifs](#1-contexte-et-objectifs)
2. [Audit de l'existant](#2-audit-de-lexistant)
3. [Phase 1 — Enrichissement de DocumentType](#3-phase-1--enrichissement-de-documenttype)
4. [Phase 2 — Validation dynamique à l'upload](#4-phase-2--validation-dynamique-à-lupload)
5. [Phase 3 — Admin KYC upload FRONT/BACK](#5-phase-3--admin-kyc-upload-frontback)
6. [Phase 4 — Enrichissement des détails post-upload](#6-phase-4--enrichissement-des-détails-post-upload)
7. [Phase 5 — Activer autoApproveAfterDays](#7-phase-5--activer-autoapprovearchdays)
8. [Récapitulatif des fichiers touchés](#8-récapitulatif-des-fichiers-touchés)
9. [Migrations Flyway](#9-migrations-flyway)
10. [Seeds mis à jour](#10-seeds-mis-à-jour)

---

## 1. Contexte et objectifs

### Problème

Le formulaire d'upload de documents KYC est aujourd'hui statique côté client et inexistant côté admin — tous les types de documents reçoivent les mêmes champs (`documentNumber`, `issueDate`, `expiryDate`) alors que leur pertinence dépend du type :

- Un **justificatif de domicile** n'a pas de numéro, pas de date d'émission et pas de date d'expiration.
- Un **passeport** exige les trois.
- Un **RCCM** exige un numéro mais pas de date d'expiration.
- Une **attestation de résidence** ne nécessite rien d'autre que le fichier.

De plus, l'admin n'a pas de chemin dédié pour uploader un document KYC au nom d'un client avec la même logique FRONT/BACK que le portail client, et il ne peut pas enrichir les détails d'un document après upload.

### Objectifs

1. Construire dynamiquement le formulaire d'upload côté client selon la config du type de document.
2. Imposer les champs requis strictement côté client, de manière laxiste côté admin.
3. Donner à l'admin un endpoint dédié pour uploader un document KYC (FRONT/BACK) au nom d'un client.
4. Permettre à l'admin de renseigner les informations lues sur une pièce physique après upload.
5. Activer la logique `autoApproveAfterDays` dormante.
6. Ne rien casser : toutes les modifications sont additives ou des enrichissements de validations existantes.

---

## 2. Audit de l'existant

### 2.1 Ce qui existe

| Composant | État |
|---|---|
| `DocumentType.requiresExpiryDate` | Fonctionnel, vérifié dans `validateDocumentMetadata()` |
| `DocumentType.requiresReview` | Fonctionnel, utilisé dans `initialDocumentStatus()` |
| `DocumentType.requiresBackSide` | Champ présent mais **jamais enforced côté admin** — uniquement géré dans `ClientKycService` |
| `DocumentType.autoApprove` | En base + model, **absent du `DocumentTypeRequest`** → non modifiable via API |
| `DocumentType.autoApproveAfterDays` | En base + model, **jamais consommé par aucun job** → code mort |
| Upload admin | Endpoint générique `/documents/upload` — pas de logique FRONT/BACK |
| Upload client | `ClientKycService.uploadDocument()` gère FRONT/BACK mais valide les MIME de façon hardcodée |
| Validation upload | `validateDocumentMetadata()` vérifie seulement `expiryDate` et la taille |

### 2.2 Ce qui manque

| Manque | Impact |
|---|---|
| `requiresIssueDate` sur `DocumentType` | Impossible de différencier les types nécessitant une date d'émission |
| `requiresDocumentNumber` sur `DocumentType` | Idem pour le numéro de document |
| Validation per-type côté client | Le frontend doit interroger une config pour afficher les bons champs |
| Endpoint `upload-config` | Pas de contrat API pour le formulaire dynamique |
| Endpoint admin KYC upload | Admin utilise le chemin générique sans logique FRONT/BACK |
| Endpoint d'enrichissement post-upload | Aucun moyen de renseigner `documentNumber`/`issueDate`/`expiryDate` après coup |
| `autoApproveAfterDays` actif | Job absent dans `DocumentLifecycleWorker` |

---

## 3. Phase 1 — Enrichissement de DocumentType

### 3.1 Nouveaux champs sur l'entité

```java
// DocumentType.java — champs à ajouter
@Column(name = "requires_issue_date", nullable = false)
private Boolean requiresIssueDate = false;

@Column(name = "requires_document_number", nullable = false)
private Boolean requiresDocumentNumber = false;
```

Réparer également les champs existants omis du DTO :

```java
// DocumentTypeRequest.java — champs à ajouter
private Boolean requiresIssueDate;
private Boolean requiresDocumentNumber;
private Boolean autoApprove;         // existait en base, absent du DTO
private Integer autoApproveAfterDays; // idem
```

Exposer dans `DocumentTypeResponse` :

```java
private Boolean requiresIssueDate;
private Boolean requiresDocumentNumber;
private Boolean autoApprove;
private Integer autoApproveAfterDays;
```

### 3.2 Endpoint upload-config

**Nouveau DTO :** `DocumentUploadConfigResponse`

```java
@Builder @Data
public class DocumentUploadConfigResponse {
    private String documentTypeCode;
    private String documentTypeName;
    private String helpText;
    private boolean requiresDocumentNumber;
    private boolean requiresIssueDate;
    private boolean requiresExpiryDate;
    private boolean requiresBackSide;
    private List<String> allowedMimeTypes;  // parsé depuis la chaîne CSV
    private long maxFileSizeBytes;
    private String category;
}
```

**Endpoint :** `GET /api/v1/documents/types/{code}/upload-config`  
Autorisation : public (authentifié uniquement) — utilisé par le frontend pour construire le formulaire.

Exemple de réponse pour `CNI` :
```json
{
  "documentTypeCode": "CNI",
  "documentTypeName": "Carte nationale d'identité",
  "helpText": "Recto-verso obligatoire. Doit être en cours de validité.",
  "requiresDocumentNumber": true,
  "requiresIssueDate": true,
  "requiresExpiryDate": true,
  "requiresBackSide": true,
  "allowedMimeTypes": ["application/pdf", "image/jpeg", "image/png"],
  "maxFileSizeBytes": 10485760,
  "category": "KYC"
}
```

Exemple pour `JUSTIFICATIF_DOMICILE` :
```json
{
  "documentTypeCode": "JUSTIFICATIF_DOMICILE",
  "documentTypeName": "Justificatif de domicile",
  "helpText": "Facture, quittance ou attestation datant de moins de 3 mois.",
  "requiresDocumentNumber": false,
  "requiresIssueDate": false,
  "requiresExpiryDate": false,
  "requiresBackSide": false,
  "allowedMimeTypes": ["application/pdf", "image/jpeg", "image/png"],
  "maxFileSizeBytes": 10485760,
  "category": "KYC"
}
```

---

## 4. Phase 2 — Validation dynamique à l'upload

### 4.1 Composant `DocumentUploadValidator`

Nouveau bean `@Component` extrait de `DocumentServiceImpl` :

```java
@Component
public class DocumentUploadValidator {

    // Mode CLIENT : enforce tous les champs requis par le type
    public void validateForClient(DocumentUploadMetadataRequest req, DocumentType type) {
        if (Boolean.TRUE.equals(type.getRequiresDocumentNumber())
                && !StringUtils.hasText(req.getDocumentNumber())) {
            throw new BadRequestException("Le numéro de document est obligatoire pour ce type de pièce");
        }
        if (Boolean.TRUE.equals(type.getRequiresIssueDate())
                && req.getIssueDate() == null) {
            throw new BadRequestException("La date d'émission est obligatoire pour ce type de pièce");
        }
        if (Boolean.TRUE.equals(type.getRequiresExpiryDate())
                && req.getExpiryDate() == null) {
            throw new BadRequestException("La date d'expiration est obligatoire pour ce type de pièce");
        }
        // MIME validé depuis la config du type (supprimer la liste hardcodée dans ClientKycService)
    }

    // Mode ADMIN : aucun champ bloquant, seulement un log warning
    public void validateForAdmin(DocumentUploadMetadataRequest req, DocumentType type) {
        if (Boolean.TRUE.equals(type.getRequiresDocumentNumber())
                && !StringUtils.hasText(req.getDocumentNumber())) {
            log.warn("Admin upload sans documentNumber pour type={}", type.getCode());
        }
        // idem pour issueDate, expiryDate
    }
}
```

### 4.2 Détection du mode dans `DocumentServiceImpl`

```java
// Dans upload() — avant validateDocumentMetadata()
boolean isAdminUpload = securityContext.hasAuthority("DOCUMENT:ADMIN_UPLOAD");
if (isAdminUpload) {
    uploadValidator.validateForAdmin(metadata, documentType);
} else {
    uploadValidator.validateForClient(metadata, documentType);
}
```

### 4.3 Alignement MIME côté client

Dans `ClientKycService`, supprimer :
```java
// AVANT — hardcodé
private static final Set<String> ALLOWED_MIME = Set.of("application/pdf", "image/jpeg", "image/png");
```

Remplacer par la validation déléguée au `DocumentUploadValidator` qui lit `documentType.getAllowedMimeTypes()`.

---

## 5. Phase 3 — Admin KYC upload FRONT/BACK

### 5.1 Nouveau service partagé `KycDocumentUploadService`

Extraire la logique de `ClientKycService.uploadDocument()` dans un service partagé :

```java
@Service
public class KycDocumentUploadService {

    // Appelé par ClientKycService (isAdminUpload=false) et l'admin controller (isAdminUpload=true)
    public DocumentResponse upload(
        String ownerType, String ownerCode,
        String documentTypeCode,
        DocumentSide side,
        String documentNumber, LocalDate issueDate, LocalDate expiryDate,
        MultipartFile file,
        boolean isAdminUpload
    ) {
        // 1. Valider selon le mode
        // 2. Uploader via DocumentService.upload()
        // 3. Si BACK : retrouver le KycDocument FRONT existant → y attacher backDocument
        // 4. Si FRONT : créer/mettre à jour le KycDocument avec les métadonnées fournies
        // 5. Déclencher kycAutomationService.syncFromDocumentUpload()
    }
}
```

### 5.2 Nouveau endpoint admin

**Controller :** `KycDocumentAdminController`  
**Endpoint :** `POST /api/v1/kyc/documents/upload`  
**Autorisation :** `DOCUMENT:ADMIN_UPLOAD`

Paramètres multipart :

| Paramètre | Type | Requis | Description |
|---|---|---|---|
| `file` | `MultipartFile` | Oui | Fichier du document |
| `ownerType` | String | Oui | `MEMBER`, `CUSTOMER`, `BUSINESS` |
| `ownerCode` | String | Oui | Code de l'entité propriétaire |
| `documentTypeCode` | String | Oui | Code du type (ex: `CNI`) |
| `side` | String | Non | `FRONT` (défaut) ou `BACK` |
| `documentNumber` | String | Non | Numéro de la pièce |
| `issueDate` | LocalDate | Non | Date d'émission |
| `expiryDate` | LocalDate | Non | Date d'expiration |

**Comportement BACK :**
1. Retrouver le `KycDocument` FRONT existant pour `(ownerType, ownerId, documentTypeCode)`.
2. Si introuvable → `BadRequestException("Le document recto doit être uploadé en premier")`.
3. Uploader le fichier → `Document` créé.
4. Mettre à jour `kycDocument.backDocument = nouveauDocument`.
5. Remettre le statut à `PENDING`.
6. Déclencher `kycAutomationService.syncFromDocumentUpload()`.

---

## 6. Phase 4 — Enrichissement des détails post-upload

### 6.1 Nouveau DTO

```java
@Data
public class KycDocumentDetailsRequest {
    private String documentNumber;
    private LocalDate issueDate;
    private LocalDate expiryDate;
}
```

### 6.2 Endpoint

**`PATCH /api/v1/kyc/documents/{kycDocumentCode}/details`**  
**Autorisation :** `DOCUMENT:REVIEW` (admin uniquement)

Logique :
1. Retrouver le `KycDocument` par code.
2. Mettre à jour les champs non-null fournis (`documentNumber`, `issueDate`, `expiryDate`).
3. Sauvegarder.
4. Retourner le `KycDocumentResponse` mis à jour.

**Cas d'usage typique :** L'admin reçoit une photo floue d'une CNI mais peut lire le numéro et les dates. Il upload le fichier puis appelle ce endpoint pour renseigner les métadonnées sans ré-uploader.

---

## 7. Phase 5 — Activer autoApproveAfterDays

### 7.1 Ajout dans `DocumentLifecycleWorker`

Dans la méthode `runLifecycleChecks()` existante, ajouter une phase dédiée :

```java
private void processAutoApprovals() {
    Instant now = Instant.now();
    // Cherche tous les types avec autoApproveAfterDays défini
    documentTypeRepository.findAllByAutoApproveAfterDaysIsNotNull()
        .forEach(type -> {
            Instant threshold = now.minus(type.getAutoApproveAfterDays(), ChronoUnit.DAYS);
            documentRepository
                .findAllByDocumentTypeAndStatusAndUploadedAtBefore(
                    type, DocumentStatus.PENDING_REVIEW, threshold)
                .forEach(doc -> {
                    doc.setStatus(DocumentStatus.APPROVED);
                    documentRepository.save(doc);
                    kycAutomationService.syncFromDocumentUpload(doc);
                    log.info("Auto-approved document={} after {}d", doc.getCode(), type.getAutoApproveAfterDays());
                });
        });
}
```

---

## 8. Récapitulatif des fichiers touchés

### Fichiers modifiés

| Fichier | Changement |
|---|---|
| `DocumentType.java` | +2 champs : `requiresIssueDate`, `requiresDocumentNumber` |
| `DocumentTypeRequest.java` | +4 champs : `requiresIssueDate`, `requiresDocumentNumber`, `autoApprove`, `autoApproveAfterDays` |
| `DocumentTypeResponse.java` | +4 champs exposés |
| `DocumentTypeMapper` (interface + decorator) | Mapping des nouveaux champs |
| `DocumentServiceImpl.java` | Détecter mode ADMIN/CLIENT, déléguer validation à `DocumentUploadValidator` |
| `ClientKycService.java` | Déléguer upload vers `KycDocumentUploadService`, supprimer MIME hardcodé |
| `DocumentLifecycleWorker.java` | Phase `processAutoApprovals()` |
| `DocumentTypeController.java` | Endpoint `GET /{code}/upload-config` |

### Nouveaux fichiers

| Fichier | Rôle |
|---|---|
| `DocumentUploadValidator.java` | Validation ADMIN vs CLIENT selon config du type |
| `DocumentUploadConfigResponse.java` | DTO formulaire dynamique |
| `KycDocumentUploadService.java` | Logique upload FRONT/BACK partagée admin + client |
| `KycDocumentAdminController.java` | `POST /kyc/documents/upload` + `PATCH /{code}/details` |
| `KycDocumentDetailsRequest.java` | DTO enrichissement post-upload |

### Migrations Flyway

| Version | Description |
|---|---|
| V195 (dev) / V191 (prod) | Ajout `requires_issue_date`, `requires_document_number` sur `document_type` |
| V196 (dev) / V192 (prod) | Seeds mis à jour pour les 12 types existants |

---

## 9. Migrations Flyway

### V195__document_type_field_requirements.sql

```sql
ALTER TABLE document_type
    ADD COLUMN IF NOT EXISTS requires_issue_date    BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS requires_document_number BOOLEAN NOT NULL DEFAULT FALSE;
```

### V196__document_type_seeds_update.sql

```sql
-- Pièces d'identité : numéro + date d'émission + date d'expiration
UPDATE document_type
SET requires_document_number = TRUE,
    requires_issue_date      = TRUE,
    requires_expiry_date     = TRUE
WHERE code IN ('CNI', 'PASSPORT', 'RESIDENCE_PERMIT', 'ID_CARD');

-- Documents d'entreprise avec numéro mais sans expiration systématique
UPDATE document_type
SET requires_document_number = TRUE,
    requires_issue_date      = FALSE,
    requires_expiry_date     = FALSE
WHERE code IN ('RCCM', 'NIU');

-- Justificatifs : aucun champ obligatoire hors fichier
UPDATE document_type
SET requires_document_number = FALSE,
    requires_issue_date      = FALSE,
    requires_expiry_date     = FALSE
WHERE code IN (
    'JUSTIFICATIF_DOMICILE',
    'ATTESTATION_RESIDENCE',
    'STATUTS',
    'PROCURATION'
);
```

---

## 10. Seeds mis à jour

Résumé des règles par type de document après migrations :

| Code | Numéro requis | Date émission requise | Date expiration requise | Recto-verso |
|---|---|---|---|---|
| `CNI` | Oui | Oui | Oui | Oui |
| `PASSPORT` | Oui | Oui | Oui | Non |
| `RESIDENCE_PERMIT` | Oui | Oui | Oui | Oui |
| `ID_CARD` | Oui | Oui | Oui | Oui |
| `RCCM` | Oui | Non | Non | Non |
| `NIU` | Oui | Non | Non | Non |
| `STATUTS` | Non | Non | Non | Non |
| `JUSTIFICATIF_DOMICILE` | Non | Non | Non | Non |
| `ATTESTATION_RESIDENCE` | Non | Non | Non | Non |
| `PROCURATION` | Non | Non | Non | Non |

---

## Ordre d'implémentation recommandé

```
Phase 1 (DB + config)
  └─ Migration V195 + V196
  └─ DocumentType champs
  └─ DocumentTypeRequest/Response
  └─ Endpoint upload-config

Phase 2 (validation)
  └─ DocumentUploadValidator
  └─ Intégration dans DocumentServiceImpl
  └─ Fix MIME côté ClientKycService

Phase 3 (admin upload)
  └─ KycDocumentUploadService
  └─ KycDocumentAdminController (POST upload)
  └─ Refactor ClientKycService → KycDocumentUploadService

Phase 4 (enrichissement)
  └─ KycDocumentAdminController (PATCH details)
  └─ KycDocumentDetailsRequest

Phase 5 (auto-approve)
  └─ DocumentLifecycleWorker phase auto-approval
  └─ Repository query uploadedAtBefore
```

---

*Document technique — Bokati Cowork / Elle A Osé. Ne pas distribuer.*
