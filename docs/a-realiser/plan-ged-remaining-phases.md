# Plan d'implémentation — GED Complète

**Version :** 1.0
**Date :** 2026-06-20
**Basé sur :** `docs/Specification_GED_Complete.md` + architecture existante Bokati Cowork

---

## Etat des lieux

### Ce qui existe déjà

| Fonctionnalité | Couverture | Détail |
|---|---|---|
| Stockage fichiers | 100% | MinIO (dev) / S3 (prod) via `DocumentStorageService` |
| Modèle Document | 90% | Entity complète, versioning, métadonnées, tags, statuts |
| Upload unitaire | 100% | Multipart + metadata JSON |
| Versioning | 100% | `DocumentVersion` avec numéro, checksum SHA256, antivirus |
| Tags | 100% | CRUD + assignation par document |
| Métadonnées clé-valeur | 100% | Flexible key-value sur chaque document |
| Workflow validation | 60% | Validation simple (approve/reject/correction). Pas de circuit hiérarchique |
| Signature électronique | 80% | Demande, signature, journal. Pas de signature externe (DocuSign) |
| Notifications | 70% | Outbox pattern, email, webhook, in-app. Pas de centre de notifications dédié GED |
| Échéances & expiration | 80% | Tracking expiry, alertes expiring-soon |
| Audit & traçabilité | 100% | AuditLog complet (acteur, action, diff, IP, session) + DocumentAccessLog |
| Dashboard & Analytics | 90% | Compteurs, tendances, taux, export CSV |
| Politiques de rétention | 100% | CRUD, archivage/purge automatique |
| Recherche | 40% | JPA/PostgreSQL uniquement, pas de full-text sur contenu fichier |
| Espaces documentaires | 80% | 6 espaces (KYC, Contract, Financial, Asset, Admin, Generic) |

### Ce qui manque

| Fonctionnalité | Priorité | Complexité |
|---|---|---|
| **Dossiers / sous-dossiers / arborescence** | Haute | Haute |
| **Upload multiple / ZIP** | Haute | Moyenne |
| **Recherche plein texte (Elasticsearch)** | Haute | Haute |
| **OCR généralisé** (pas juste KYC) | Moyenne | Moyenne |
| **Workflow avancé** (hiérarchique, parallèle, séquentiel) | Moyenne | Haute |
| **Partage documentaire** (liens temporaires, externe) | Moyenne | Moyenne |
| **Permissions granulaires** par dossier/document | Moyenne | Haute |
| **Centre de notifications GED** | Basse | Moyenne |
| **Classification automatique** | Basse | Haute |
| **Verrouillage concurrent** (check-out/check-in) | Basse | Moyenne |

---

## Phase 1 — Dossiers, sous-dossiers et arborescence

### Concept

Introduire une entité `DocumentFolder` qui représente un noeud dans une arborescence hiérarchique. Chaque dossier peut contenir des sous-dossiers et des documents. Un document peut appartenir à un dossier (optionnel — les documents existants restent sans dossier).

### Modèle de données

#### Nouvelle entité : `DocumentFolder`

```
Table : document_folder

id                  BIGINT PK          @IdGeneration
code                VARCHAR(100) UNIQUE Code généré (FLD-YYYYMM-NNNNNN)
name                VARCHAR(300) NOT NULL Nom du dossier
description         TEXT                Description optionnelle
parent_id           BIGINT FK(document_folder.id) NULL Dossier parent (NULL = racine)
space               VARCHAR(50) NOT NULL DEFAULT 'GENERIC' Espace documentaire
owner_type          VARCHAR(20)         Propriétaire optionnel (MEMBER, CUSTOMER, etc.)
owner_id            BIGINT              ID du propriétaire
path                VARCHAR(2000) NOT NULL Chemin matérialisé (/racine/parent/enfant/)
depth               INTEGER NOT NULL DEFAULT 0 Profondeur dans l'arbre
sort_order          INTEGER DEFAULT 0   Ordre d'affichage
color               VARCHAR(7)          Couleur hex optionnelle
icon                VARCHAR(50)         Icône optionnelle
created_by          BIGINT              Utilisateur créateur
created_at          TIMESTAMP NOT NULL DEFAULT NOW()
updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
deleted             BOOLEAN NOT NULL DEFAULT FALSE Soft delete

INDEX idx_document_folder_parent ON (parent_id)
INDEX idx_document_folder_space ON (space)
INDEX idx_document_folder_path ON (path)  -- Pour les requêtes LIKE 'path%'
INDEX idx_document_folder_owner ON (owner_type, owner_id)
```

#### Modification : `document` (table existante)

```
ALTER TABLE document ADD COLUMN folder_id BIGINT;
ALTER TABLE document ADD CONSTRAINT fk_document_folder
    FOREIGN KEY (folder_id) REFERENCES document_folder(id);
CREATE INDEX idx_document_folder ON document(folder_id);
```

### Pattern du chemin matérialisé (Materialized Path)

Le champ `path` stocke le chemin complet depuis la racine :
- Racine : `/`
- Dossier de niveau 1 : `/15/` (où 15 = id du dossier)
- Dossier de niveau 2 : `/15/42/`
- Dossier de niveau 3 : `/15/42/78/`

Avantages :
- Lecture des sous-arborescences ultra rapide : `WHERE path LIKE '/15/%'`
- Pas de requêtes récursives CTE nécessaires
- Compatible PostgreSQL, simple à indexer

### Entité Java

```
features/document/documentMaster/model/DocumentFolder.java

Champs : id, code, name, description, parent (ManyToOne self-ref),
         space, ownerType, ownerId, path, depth, sortOrder,
         color, icon, createdBy, createdAt, updatedAt, deleted
Relations : children (OneToMany), documents (OneToMany via folder_id)
Soft delete : @SQLDelete / @SQLRestriction
```

### Modification entité Document

Ajouter à `Document.java` :
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "folder_id", foreignKey = @ForeignKey(name = "fk_document_folder"))
private DocumentFolder folder;
```

### Repository

```
DocumentFolderRepository.java

findByCode(String code)
findAllByParentAndDeletedFalseOrderBySortOrderAscNameAsc(DocumentFolder parent)
findAllByParentIsNullAndSpaceAndDeletedFalseOrderBySortOrderAsc(DocumentSpace space)
findAllByPathStartingWithAndDeletedFalse(String pathPrefix) // sous-arborescence
findAllByOwnerTypeAndOwnerIdAndDeletedFalse(DocumentOwnerType, Long)
countByParentAndDeletedFalse(DocumentFolder parent)
existsByParentAndNameAndDeletedFalse(DocumentFolder parent, String name)
```

### DTOs

**Request :**
```
CreateFolderRequest : name, description, parentCode (opt), space, ownerType (opt),
                      ownerCode (opt), color (opt), icon (opt)
UpdateFolderRequest : name, description, color, icon, sortOrder
MoveFolderRequest   : targetParentCode (null = racine)
MoveDocumentRequest : folderCode (null = retirer du dossier)
```

**Response :**
```
DocumentFolderResponse : code, name, description, parentCode, space, ownerType,
                         ownerName, path, depth, sortOrder, color, icon,
                         childrenCount, documentsCount, createdAt, createdBy
DocumentFolderTreeResponse : code, name, children (récursif), documentsCount, depth
```

### Endpoints

```
Base : /v1/document-folders

POST   /                           Créer un dossier (Audité)
GET    /{code}                     Obtenir un dossier
PATCH  /{code}                     Modifier un dossier (Audité)
DELETE /{code}                     Soft-delete un dossier (Audité) — échoue si non vide
POST   /{code}/move                Déplacer un dossier (Audité)

GET    /roots?space=               Dossiers racine par espace
GET    /{code}/children            Enfants directs d'un dossier
GET    /{code}/tree                Arborescence complète sous un dossier (récursif)
GET    /{code}/documents           Documents dans ce dossier (paginé)
GET    /{code}/breadcrumb          Fil d'Ariane (chemin du dossier racine à ce dossier)

POST   /v1/documents/{code}/move   Déplacer un document dans un dossier (Audité)
```

### Service

```
DocumentFolderService.java

create(request) : Valide unicité du nom dans le parent, génère code, calcule path/depth
update(code, request) : Met à jour nom/description/couleur
delete(code) : Vérifie dossier vide (pas d'enfants ni docs), soft-delete
move(code, targetParentCode) : Recalcule path/depth pour tout le sous-arbre
getRoots(space) : Dossiers racine
getChildren(code) : Enfants directs avec compteurs
getTree(code) : Construction récursive de l'arborescence
getDocuments(code, pageable) : Documents dans le dossier
getBreadcrumb(code) : Résolution du chemin vers la racine
moveDocument(docCode, folderCode) : Change le folder_id du document
```

### Règles métier

- Profondeur max : 10 niveaux
- Un dossier ne peut pas être déplacé dans un de ses descendants (détection de cycle via path)
- Nom unique dans un même parent
- La suppression d'un dossier non vide est interdite (le frontend doit d'abord déplacer/supprimer le contenu)
- Le path est recalculé en cascade lors d'un déplacement

---

## Phase 2 — Upload multiple et import ZIP

### Endpoints

```
POST /v1/documents/upload-batch        Upload multiple (multipart, plusieurs fichiers)
POST /v1/documents/import-zip          Import d'une archive ZIP
```

### Upload batch

Accepte N fichiers + un JSON de metadata partagée (ownerType, ownerCode, folderCode). Chaque fichier crée un Document distinct. Retourne `DocumentBulkActionResponse` avec succès/échecs par fichier.

### Import ZIP

1. Réception du ZIP en multipart
2. Extraction en mémoire (limite : 50 fichiers, 100 Mo total)
3. Chaque fichier extrait → création Document avec metadata commune
4. Support optionnel : si le ZIP contient des sous-dossiers → création automatique de `DocumentFolder`
5. Retourne un rapport d'import (fichiers traités, erreurs, dossiers créés)

### Service

```
DocumentBatchService.java

uploadBatch(files[], metadata, folderCode) → DocumentBulkActionResponse
importZip(zipFile, metadata, folderCode) → ZipImportResponse
```

---

## Phase 3 — Recherche plein texte (Elasticsearch)

### Architecture

```
PostgreSQL (source de vérité) → Outbox Event → ES Indexer → Elasticsearch (index de recherche)
```

Les documents sont indexés dans ES après chaque opération (create, update, approve, archive). La recherche utilise ES pour le scoring et la pertinence, PostgreSQL reste la source de vérité.

### Configuration

```yaml
# application-dev.yml
spring.elasticsearch:
  uris: http://localhost:9200

app.document.search:
  engine: elasticsearch   # ou "database" pour fallback JPA
  index-name: bokati-documents
  auto-index: true
```

### Index Elasticsearch

```json
{
  "mappings": {
    "properties": {
      "code":          { "type": "keyword" },
      "title":         { "type": "text", "analyzer": "french" },
      "description":   { "type": "text", "analyzer": "french" },
      "fileName":      { "type": "text" },
      "content":       { "type": "text", "analyzer": "french" },
      "ownerType":     { "type": "keyword" },
      "ownerId":       { "type": "long" },
      "ownerName":     { "type": "text" },
      "category":      { "type": "keyword" },
      "space":         { "type": "keyword" },
      "status":        { "type": "keyword" },
      "documentType":  { "type": "keyword" },
      "tags":          { "type": "keyword" },
      "folderCode":    { "type": "keyword" },
      "folderPath":    { "type": "keyword" },
      "metadata":      { "type": "object", "dynamic": true },
      "issueDate":     { "type": "date" },
      "expiryDate":    { "type": "date" },
      "uploadedAt":    { "type": "date" },
      "fileSize":      { "type": "long" },
      "mimeType":      { "type": "keyword" }
    }
  }
}
```

### Service

```
DocumentElasticsearchService.java

indexDocument(Document doc)       Indexer/mettre à jour un document
deleteDocument(String code)       Supprimer de l'index
search(query, filters, pageable)  Recherche multi-critères avec scoring
reindexAll()                      Réindexation complète (admin)
```

### Intégration

- `DocumentServiceImpl` : après chaque save/update/delete → publier un événement outbox `DOCUMENT_INDEXED`
- `DocumentIndexOutboxEventProcessor` : consomme l'événement et appelle `DocumentElasticsearchService.indexDocument()`
- `DocumentSearchServiceImpl` : basculer entre ES et JPA selon la config `app.document.search.engine`

---

## Phase 4 — OCR généralisé

### Extension du système OCR existant

Le module KYC utilise déjà Tesseract (via Tess4J). L'idée est de généraliser l'extraction de texte à tout document uploadé, pas uniquement les documents KYC.

### Fonctionnement

1. Après upload d'un document (image ou PDF), le `DocumentLifecycleWorker` déclenche l'OCR
2. Le texte extrait est stocké dans `DocumentMetadata` (clé : `_ocr_content`)
3. Le texte est aussi indexé dans Elasticsearch (champ `content`)
4. L'extraction s'applique aux formats : PDF, JPEG, PNG, TIFF

### Configuration

```yaml
app.document.ocr:
  enabled: true
  languages: fra+eng
  formats: application/pdf,image/jpeg,image/png,image/tiff
  max-file-size-bytes: 20971520   # 20 Mo
```

### Service

```
DocumentOcrService.java

extractText(Document doc) → String           Extraction du texte
processAndIndex(Document doc)                Extraction + stockage metadata + indexation ES
isOcrEligible(Document doc) → boolean        Vérifie format/taille
```

---

## Phase 5 — Workflow documentaire avancé

### Modèle de données

#### Nouvelle entité : `DocumentWorkflow`

```
Table : document_workflow

id                  BIGINT PK
code                VARCHAR(100) UNIQUE
name                VARCHAR(300) NOT NULL
description         TEXT
workflow_type       VARCHAR(30) NOT NULL   SIMPLE | SEQUENTIAL | PARALLEL | HIERARCHICAL
space               VARCHAR(50)            Espace concerné
document_type_code  VARCHAR(100)           Type de document concerné
active              BOOLEAN DEFAULT TRUE
created_by          BIGINT
created_at          TIMESTAMP
```

#### Nouvelle entité : `DocumentWorkflowStep`

```
Table : document_workflow_step

id                  BIGINT PK
workflow_id         BIGINT FK(document_workflow.id) NOT NULL
step_order          INTEGER NOT NULL
step_name           VARCHAR(200) NOT NULL
approver_type       VARCHAR(30) NOT NULL   USER | ROLE | DEPARTMENT
approver_value      VARCHAR(200) NOT NULL  Email, rôle, ou département
required            BOOLEAN DEFAULT TRUE
auto_approve_days   INTEGER               Auto-approbation après N jours
```

#### Nouvelle entité : `DocumentWorkflowInstance`

```
Table : document_workflow_instance

id                  BIGINT PK
workflow_id         BIGINT FK
document_id         BIGINT FK(document.id) NOT NULL
current_step        INTEGER NOT NULL DEFAULT 1
status              VARCHAR(30) NOT NULL   PENDING | IN_PROGRESS | APPROVED | REJECTED | CANCELLED
started_at          TIMESTAMP NOT NULL
completed_at        TIMESTAMP
started_by          BIGINT
```

#### Nouvelle entité : `DocumentWorkflowAction`

```
Table : document_workflow_action

id                  BIGINT PK
instance_id         BIGINT FK(document_workflow_instance.id)
step_order          INTEGER NOT NULL
action              VARCHAR(20) NOT NULL   APPROVE | REJECT | SKIP
actor_id            BIGINT NOT NULL
acted_at            TIMESTAMP NOT NULL
comment             TEXT
```

### Endpoints

```
Base : /v1/document-workflows

CRUD sur les workflows (définitions)
CRUD sur les steps

POST /v1/documents/{code}/start-workflow       Démarrer un workflow
POST /v1/documents/{code}/workflow/approve      Approuver l'étape courante
POST /v1/documents/{code}/workflow/reject       Rejeter l'étape courante
GET  /v1/documents/{code}/workflow/status        Statut du workflow en cours
GET  /v1/document-workflows/my-pending          Mes approbations en attente
```

---

## Phase 6 — Partage documentaire

### Modèle de données

#### Nouvelle entité : `DocumentShareLink`

```
Table : document_share_link

id                  BIGINT PK
code                VARCHAR(100) UNIQUE    Token du lien
document_id         BIGINT FK(document.id)
folder_id           BIGINT FK(document_folder.id)  Un des deux rempli
created_by          BIGINT NOT NULL
expires_at          TIMESTAMP NOT NULL
password_hash       VARCHAR(255)           Protection par mot de passe (optionnel)
allow_download      BOOLEAN DEFAULT TRUE
max_access_count    INTEGER                Nombre max de consultations (null = illimité)
access_count        INTEGER DEFAULT 0
active              BOOLEAN DEFAULT TRUE
created_at          TIMESTAMP NOT NULL
last_accessed_at    TIMESTAMP
```

### Endpoints

```
POST   /v1/documents/{code}/share             Créer un lien de partage
POST   /v1/document-folders/{code}/share       Partager un dossier
GET    /v1/shares/{shareCode}                  Accéder au document partagé (public, auth optionnelle)
GET    /v1/shares/{shareCode}/download         Télécharger (si autorisé)
DELETE /v1/shares/{shareCode}                  Révoquer un lien
GET    /v1/documents/{code}/shares             Lister les liens actifs d'un document
```

---

## Phase 7 — Permissions granulaires

### Modèle de données

#### Nouvelle entité : `DocumentPermission`

```
Table : document_permission

id                  BIGINT PK
target_type         VARCHAR(20) NOT NULL   DOCUMENT | FOLDER
target_id           BIGINT NOT NULL        ID du document ou dossier
grantee_type        VARCHAR(20) NOT NULL   USER | ROLE
grantee_id          BIGINT NOT NULL
permission          VARCHAR(20) NOT NULL   READ | DOWNLOAD | EDIT | REVIEW | DELETE | ADMIN
granted_by          BIGINT
granted_at          TIMESTAMP NOT NULL

UNIQUE (target_type, target_id, grantee_type, grantee_id, permission)
```

### Règles d'héritage

1. Les permissions sur un dossier s'appliquent à tous ses documents et sous-dossiers
2. Une permission explicite sur un document surcharge la permission héritée du dossier
3. Le rôle `ADMIN` a toujours accès total
4. L'absence de permission = pas d'accès (sauf ADMIN)

### Service

```
DocumentPermissionService.java

grant(targetType, targetCode, granteeType, granteeId, permission)
revoke(targetType, targetCode, granteeType, granteeId, permission)
hasPermission(userId, targetType, targetId, permission) → boolean
listPermissions(targetType, targetCode) → List<DocumentPermissionResponse>
getEffectivePermissions(userId, documentCode) → Set<Permission>  // inclut héritage
```

### Intégration

Ajouter un filtre de sécurité `DocumentAccessFilter` qui vérifie les permissions avant chaque opération sur un document ou dossier.

---

## Phase 8 — Centre de notifications GED

### Extension du système existant

Le système de notifications (`NotificationMessage`) est déjà en place. L'extension pour la GED consiste à :

1. Ajouter de nouveaux templates de notification pour les événements GED
2. Créer un endpoint de préférences de notification par utilisateur
3. Implémenter les événements suivants :

### Événements à notifier

| Événement | Destinataire | Canaux |
|---|---|---|
| Document uploadé dans mon dossier | Propriétaire du dossier | IN_APP, EMAIL |
| Validation demandée | Approbateur désigné | IN_APP, EMAIL |
| Document approuvé | Uploadeur | IN_APP |
| Document rejeté | Uploadeur | IN_APP, EMAIL |
| Correction demandée | Uploadeur | IN_APP, EMAIL |
| Document expirant (30j, 7j) | Propriétaire | IN_APP, EMAIL |
| Nouvelle version uploadée | Observateurs | IN_APP |
| Lien de partage accédé | Créateur du lien | IN_APP |

### Endpoints

```
GET  /v1/notifications/ged                   Mes notifications GED
POST /v1/notifications/ged/mark-read         Marquer comme lu
GET  /v1/notifications/ged/preferences       Mes préférences
PUT  /v1/notifications/ged/preferences       Modifier mes préférences
```

---

## Phase 9 — Verrouillage concurrent (Check-out / Check-in)

### Modèle

Ajouter à la table `document` :

```
locked_by           BIGINT               ID de l'utilisateur qui a verrouillé
locked_at           TIMESTAMP            Date de verrouillage
lock_expires_at     TIMESTAMP            Expiration automatique du verrou (défaut : 1h)
```

### Endpoints

```
POST /v1/documents/{code}/lock           Verrouiller (check-out)
POST /v1/documents/{code}/unlock         Déverrouiller (check-in)
```

### Règles

- Un document verrouillé ne peut être modifié que par l'utilisateur qui l'a verrouillé
- Le verrou expire automatiquement après 1 heure (configurable)
- Un admin peut forcer le déverrouillage

---

## Phase 10 — Classification automatique et intelligence documentaire

### Intégration OCR + classification

1. Après extraction OCR du texte, analyser le contenu pour suggérer :
   - Le type de document (facture, contrat, CNI, etc.)
   - La catégorie (KYC, LEGAL, FINANCIAL, etc.)
   - Les tags pertinents
2. Stocker les suggestions dans `DocumentMetadata` (clé : `_ai_classification`)
3. L'utilisateur valide ou corrige la classification

### Extraction d'informations structurées

Pour les types de documents connus (factures, contrats) :
- Montants, dates, numéros de référence
- Parties prenantes (client, fournisseur)
- Stockage dans `DocumentMetadata`

### Endpoints

```
POST /v1/documents/{code}/classify        Lancer la classification automatique
GET  /v1/documents/{code}/suggestions     Obtenir les suggestions de classification
POST /v1/documents/{code}/apply-suggestions  Appliquer les suggestions
```

---

## Phase 11 — Endpoints par espace documentaire (DEJA IMPLEMENTE)

> **Statut : Implémenté** — `DocumentSpaceController` + `DocumentSpaceService`

### Concept

Chaque espace documentaire dispose de ses propres endpoints d'upload avec des validations spécifiques :
- Le type de document doit être compatible avec la catégorie de l'espace
- Les formats de fichiers sont restreints par espace
- Les tags et métadonnées sont appliqués en une seule opération à l'upload
- Un `referenceCode` permet de lier le document à une entité métier

### Endpoints implémentés

```
Base : /v1/spaces

POST /v1/spaces/contracts/documents/upload        Contrats (LEGAL → PDF, DOCX, JPEG, PNG)
GET  /v1/spaces/contracts/documents               Lister contrats
GET  /v1/spaces/contracts/dashboard               Dashboard contrats

POST /v1/spaces/financial/documents/upload        Financier (FINANCIAL → PDF, JPEG, PNG, XLSX, CSV)
GET  /v1/spaces/financial/documents               Lister + filtres dates
GET  /v1/spaces/financial/dashboard               Dashboard financier

POST /v1/spaces/assets/documents/upload           Actifs (ASSET → PDF, JPEG, PNG, WEBP, TIFF)
GET  /v1/spaces/assets/documents                  Lister actifs
GET  /v1/spaces/assets/dashboard                  Dashboard actifs

POST /v1/spaces/administrative/documents/upload   Administratif (SYSTEM, OTHER → tous formats)
GET  /v1/spaces/administrative/documents          Lister
GET  /v1/spaces/administrative/dashboard          Dashboard

POST /v1/spaces/general/documents/upload          Général (toutes catégories → tous formats)
GET  /v1/spaces/general/documents                 Lister
GET  /v1/spaces/general/dashboard                 Dashboard
```

### Request : `SpaceDocumentUploadRequest`

```json
{
  "ownerType": "MEMBER",
  "ownerCode": "MBR-202604-000012",
  "documentTypeCode": "CONTRACT_SIGNED_COPY",
  "title": "Contrat location bureau B12",
  "description": "Contrat signé 2026-2027",
  "documentNumber": "CTR-2026-0042",
  "issueDate": "2026-06-15",
  "expiryDate": "2027-06-15",
  "referenceCode": "CTR-202606-000042",
  "tagCodes": ["contrat-actif"],
  "metadata": { "contractType": "LOCATION", "montantMensuel": "150000" }
}
```

### Fichiers

- Contrôleur : `documentMaster/controller/DocumentSpaceController.java`
- Service : `documentMaster/service/implementation/DocumentSpaceService.java`
- DTO : `documentMaster/dto/request/SpaceDocumentUploadRequest.java`

### Validations par espace

| Espace | Catégories autorisées | Formats acceptés |
|--------|----------------------|------------------|
| Contrats | LEGAL | PDF, DOCX, JPEG, PNG |
| Financier | FINANCIAL | PDF, JPEG, PNG, XLSX, CSV |
| Actifs | ASSET | PDF, JPEG, PNG, WEBP, TIFF |
| Administratif | SYSTEM, OTHER | Tous |
| Général | Toutes | Tous |

### Intégration avec les phases futures

- **Phase 1 (Dossiers)** : ajouter `folderCode` au `SpaceDocumentUploadRequest` pour classer dans un dossier à l'upload
- **Phase 3 (Elasticsearch)** : les documents uploadés via espaces sont automatiquement indexés
- **Phase 7 (Permissions)** : les permissions d'espace contrôlent l'accès aux endpoints

---

## Ordre d'implémentation recommandé

```
Phase 1 : Dossiers / arborescence          ✅ FAIT
Phase 2 : Upload multiple / ZIP            ✅ FAIT
Phase 3 : PostgreSQL full-text search      ⏳ À FAIRE (remplace Elasticsearch)
Phase 4 : OCR généralisé                   ⏳ À FAIRE
Phase 5 : Workflow avancé                  ✅ FAIT
Phase 6 : Partage documentaire             ✅ FAIT
Phase 7 : Permissions granulaires          ✅ FAIT
Phase 8 : Notifications GED               ⏳ À FAIRE (extension des templates existants)
Phase 9 : Verrouillage concurrent          ✅ FAIT
Phase 10 : Classification automatique      ⏳ À FAIRE
Phase 11 : Espaces documentaires           ✅ FAIT
```

### Estimation de complexité

| Phase | Fichiers à créer/modifier | Migrations | Estimation |
|---|---|---|---|
| 1 — Dossiers | ~15 (entité, repo, DTOs, service, contrôleur, mapper) + modif Document | 1 | Complexe |
| 2 — Upload batch | ~5 (service, DTOs, endpoint) | 0 | Moyenne |
| 3 — Elasticsearch | ~8 (config, service, indexer, processor) | 0 | Complexe |
| 4 — OCR général | ~4 (service, config, worker) | 0 | Moyenne |
| 5 — Workflow | ~20 (4 entités, repos, DTOs, services, contrôleur) | 1 | Complexe |
| 6 — Partage | ~10 (entité, repo, DTOs, service, contrôleur public) | 1 | Moyenne |
| 7 — Permissions | ~10 (entité, repo, service, filtre sécurité) | 1 | Complexe |
| 8 — Notifications | ~6 (templates, préférences, endpoints) | 1 | Moyenne |
| 9 — Verrouillage | ~4 (champs, service, endpoints) | 1 | Simple |
| 10 — Classification | ~6 (service, config, endpoints) | 0 | Complexe |
| 11 — Espaces documentaires | 3 (contrôleur, service, DTO) | 0 | **FAIT** |

---

## Dépendances entre phases

```
Phase 1 (Dossiers) ──────────────────────────────────────→ indépendante
Phase 2 (Upload batch) ──→ dépend de Phase 1 (folderCode)
Phase 3 (Elasticsearch) ─→ dépend de Phase 1 (folderPath indexé)
Phase 4 (OCR) ───────────→ dépend de Phase 3 (indexation du contenu extrait)
Phase 5 (Workflow) ──────→ indépendante
Phase 6 (Partage) ───────→ dépend de Phase 1 (partage de dossiers)
Phase 7 (Permissions) ───→ dépend de Phase 1 (permissions sur dossiers)
Phase 8 (Notifications) ─→ indépendante (système notifications existe)
Phase 9 (Verrouillage) ──→ indépendante
Phase 10 (Classification)→ dépend de Phase 4 (OCR) + Phase 3 (ES)
Phase 11 (Espaces)       → FAIT — s'enrichit avec Phase 1 (folderCode) et Phase 7 (permissions)
```
