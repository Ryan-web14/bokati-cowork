# Module Contract Amendment — Documentation Backend

Documentation technique pour les développeurs backend intervenant sur le module d'avenants contractuels (`ContractAmendment`).

---

## 1. Principe central : immutabilité post-signature

Un contrat signé (`SIGNED`, `ACTIVE`, `SUSPENDED`, `AMENDED`) ne peut jamais être modifié en place.
`ContractServiceImpl.ensureMutable()` lève `BadRequestException` pour tout statut post-signature.

```java
case SIGNED, ACTIVE, SUSPENDED, AMENDED ->
    throw new BadRequestException(
        "Ce contrat a été signé et ne peut plus être modifié en place. " +
        "Toute modification doit passer par un avenant.");
```

Toute modification doit passer par le workflow `ContractAmendment`.

---

## 2. Architecture des entités

### Entités impliquées

| Entité                       | Table                         | Rôle                                     |
|------------------------------|-------------------------------|------------------------------------------|
| `ContractAmendment`          | `contract_amendment`          | Entête de l'avenant                      |
| `ContractAmendmentSection`   | `contract_amendment_section`  | Modifications au niveau des articles     |
| `ContractAmendmentVariable`  | `contract_amendment_variable` | Modifications au niveau des variables    |
| `ContractAuditEvent`         | `contract_audit_event`        | Piste d'audit append-only hash-chainée   |

### Migrations

- **V184** : tables `contract_amendment` et `contract_audit_event` + trigger d'immutabilité + ajout de `AMENDED` à la contrainte CHECK sur `contract_record.status`
- **V185** : tables `contract_amendment_section` et `contract_amendment_variable`

---

## 3. Cycle de vie d'un avenant

```
DRAFT ──→ UNDER_REVIEW ──→ PENDING_SIGNATURE ──→ ACTIVE
  │              │
  ▼              ▼
CANCELLED     REJECTED
```

### Transitions dans `ContractAmendmentServiceImpl`

| Méthode            | De                 | Vers                | Effet collatéral                          |
|--------------------|--------------------|---------------------|-------------------------------------------|
| `propose()`        | (création)         | `DRAFT`             | Crée l'avenant                            |
| `submitForReview()`| `DRAFT`            | `UNDER_REVIEW`      | Vérifie qu'il y a au moins un changement  |
| `approveReview()`  | `UNDER_REVIEW`     | `PENDING_SIGNATURE` | Enregistre `reviewedBy`, commentaire      |
| `rejectReview()`   | `UNDER_REVIEW`     | `REJECTED`          | Enregistre motif de rejet                 |
| `sign()`           | `PENDING_SIGNATURE`| `ACTIVE`            | Contrat original → `AMENDED`              |
| `cancel()`         | any open           | `CANCELLED`         | Enregistre motif d'annulation             |

### Guard : un seul avenant ouvert par contrat

```java
private static final List<AmendmentStatus> OPEN_STATUSES =
    List.of(DRAFT, UNDER_REVIEW, PENDING_SIGNATURE);
```

`propose()` vérifie `amendmentRepository.findAllByOriginalContractCodeAndStatusIn(OPEN_STATUSES)` et lève `BadRequestException` si la liste est non-vide.

### Guard : au moins un changement avant soumission

```java
private void requireAtLeastOneChange(ContractAmendment amendment) {
    boolean hasSection = !sectionRepository.findAllByAmendmentCodeOrderBySectionOrderAsc(
        amendment.getCode()).isEmpty();
    boolean hasVariable = !variableRepository.findAllByAmendmentCodeOrderByVariableKeyAsc(
        amendment.getCode()).isEmpty();
    if (!hasSection && !hasVariable)
        throw new BadRequestException("L'avenant doit contenir au moins une modification...");
}
```

---

## 4. Gestion des sections

### Règles de validation (`validateSectionRequest`)

| Action   | `targetSectionRef` | `content` |
|----------|--------------------|-----------|
| `ADD`    | optionnel          | requis    |
| `MODIFY` | requis             | requis    |
| `REMOVE` | requis             | ignoré    |

### Guard statut DRAFT

`requireDraftStatus()` est appelé avant chaque modification de section ou variable. L'avenant doit être en `DRAFT`, sinon → `BadRequestException`.

---

## 5. Gestion des variables

`setVariables()` utilise une sémantique de **remplacement total** :

```java
variableRepository.deleteAllByAmendmentCode(amendment.getCode());
// puis re-insert de toutes les variables du request
```

Cela évite les conflits d'upsert sur la contrainte unique `(amendment_code, variable_key)`.

---

## 6. Prévisualisation HTML et génération PDF

### `previewHtml()`

Construit un document HTML complet A4 avec :
- En-tête : référence de l'avenant + contrat original
- Objet de l'avenant
- Sections de modifications avec badges colorés :
  - `ADD` → badge vert `#28a745`
  - `MODIFY` → badge orange `#fd7e14`
  - `REMOVE` → badge rouge `#dc3545`
- Tableau des variables : ancienne valeur barrée (`text-decoration: line-through; color: #dc3545`) + nouvelle valeur en gras (`color: #28a745`)
- Clause de sauvegarde légale
- Bloc de signatures

### `generatePdf()`

Suit exactement le même pattern que `ContractDraftServiceImpl` :

```java
try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
    PdfRendererBuilder builder = new PdfRendererBuilder();
    builder.withW3cDocument(
        Jsoup.parse(html).ownerDocument(), ""); // org.jsoup + OpenHtmlToPDF
    builder.toStream(out);
    builder.run();
    // → documentService.createGeneratedDocument(...)
    // → amendment.setDraftDocumentCode(doc.getCode())
}
```

---

## 7. Piste d'audit hash-chainée

### Architecture de `ContractAuditEventServiceImpl`

```java
// Récupérer le dernier hash de la chaîne
String previousHash = repository
    .findTopByContractCodeOrderByOccurredAtDesc(contractCode)
    .map(ContractAuditEvent::getEventHash)
    .orElse(null);

// Construire la donnée à hasher
String rawData = (previousHash != null ? previousHash : "GENESIS")
    + "|" + contractCode
    + "|" + eventType
    + "|" + actorId
    + "|" + now
    + "|" + payload;

// SHA-256
MessageDigest digest = MessageDigest.getInstance("SHA-256");
String eventHash = HexFormat.of().formatHex(
    digest.digest(rawData.getBytes(StandardCharsets.UTF_8)));
```

### Vérification de l'intégrité (côté application)

Pour vérifier qu'une chaîne n'a pas été altérée, récupérer tous les événements triés par `occurredAt` et vérifier :
- `events[0].previousHash == null`
- Pour tout `i > 0` : `events[i].previousHash == events[i-1].eventHash`

### Immutabilité PostgreSQL

```sql
CREATE OR REPLACE FUNCTION fn_contract_audit_event_immutable() RETURNS TRIGGER AS $
BEGIN
    RAISE EXCEPTION 'contract_audit_event rows are immutable';
END;
$ LANGUAGE plpgsql;

CREATE TRIGGER trg_contract_audit_event_immutable
    BEFORE UPDATE OR DELETE ON contract_audit_event
    FOR EACH ROW EXECUTE FUNCTION fn_contract_audit_event_immutable();
```

Tout `UPDATE` ou `DELETE` sur `contract_audit_event` lève une exception PostgreSQL → aucune modification silencieuse possible.

---

## 8. Intégration avec `validateTransition()` dans `ContractServiceImpl`

La méthode `validateTransition()` a été étendue pour accepter `AMENDED` comme cible depuis `ACTIVE` et `SUSPENDED` :

```java
case ACTIVE    -> allowed = ... || targetStatus == ContractStatus.AMENDED;
case SUSPENDED -> allowed = ... || targetStatus == ContractStatus.AMENDED;
case AMENDED   -> allowed = targetStatus == TERMINATED || targetStatus == EXPIRED;
```

Quand `ContractAmendmentServiceImpl.sign()` bascule le contrat original en `AMENDED`, il appelle `contractRepository.save()` directement (bypass de `validateTransition()` est intentionnel : l'amendement service valide lui-même que le contrat est `ACTIVE` ou `SUSPENDED`).

---

## 9. Types d'événements d'audit enregistrés

| Constant string                       | Déclencheur                                            |
|---------------------------------------|--------------------------------------------------------|
| `AMENDMENT_PROPOSED`                  | `propose()`                                            |
| `AMENDMENT_SUBMITTED_FOR_REVIEW`      | `submitForReview()`                                    |
| `AMENDMENT_REVIEW_APPROVED`           | `approveReview()`                                      |
| `AMENDMENT_REVIEW_REJECTED`           | `rejectReview()`                                       |
| `AMENDMENT_SIGNED`                    | `sign()` — pour l'avenant                              |
| `CONTRACT_AMENDED`                    | `sign()` — pour le contrat original                    |
| `AMENDMENT_CANCELLED`                 | `cancel()`                                             |

---

## 10. Repos et services

| Interface                       | Implémentation                              |
|---------------------------------|---------------------------------------------|
| `ContractAmendmentService`      | `ContractAmendmentServiceImpl`              |
| `ContractAuditEventService`     | `ContractAuditEventServiceImpl`             |

| Repository                          | Méthodes clés                                                          |
|-------------------------------------|------------------------------------------------------------------------|
| `ContractAmendmentRepository`       | `findByCode`, `findAllByOriginalContractCodeOrderByCreatedAtDesc`, `findAllByOriginalContractCodeAndStatusIn` |
| `ContractAuditEventRepository`      | `findAllByContractCodeOrderByOccurredAtAsc`, `findTopByContractCodeOrderByOccurredAtDesc` |
| `ContractAmendmentSectionRepository`| `findAllByAmendmentCodeOrderBySectionOrderAsc`, `findByIdAndAmendmentCode` |
| `ContractAmendmentVariableRepository`| `findAllByAmendmentCodeOrderByVariableKeyAsc`, `findByAmendmentCodeAndVariableKey`, `deleteAllByAmendmentCode` |

---

## 11. Fichiers clés

| Fichier                                                        | Rôle                              |
|----------------------------------------------------------------|-----------------------------------|
| `features/contract/enums/ContractStatus.java`                  | Ajouter `AMENDED`                 |
| `features/contract/enums/AmendmentStatus.java`                 | Statuts d'un avenant              |
| `features/contract/enums/ContractAmendmentSectionAction.java`  | ADD/MODIFY/REMOVE                 |
| `features/contract/model/ContractAmendment.java`               | Entité avenant                    |
| `features/contract/model/ContractAmendmentSection.java`        | Entité section d'avenant          |
| `features/contract/model/ContractAmendmentVariable.java`       | Entité variable d'avenant         |
| `features/contract/model/ContractAuditEvent.java`              | Entité audit append-only          |
| `features/contract/service/implementation/ContractAmendmentServiceImpl.java` | Logique métier    |
| `features/contract/service/implementation/ContractAuditEventServiceImpl.java` | Hash chain     |
| `features/contract/controller/ContractAmendmentController.java`| 17 endpoints REST                 |
| `db/migration/V184__contract_amendment_and_audit_trail.sql`    | Tables + trigger immutabilité     |
| `db/migration/V185__contract_amendment_content.sql`            | Tables sections + variables       |
