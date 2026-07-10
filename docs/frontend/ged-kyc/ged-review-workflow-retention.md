# GED — File de revue, workflows d'approbation &amp; rétention

> Back-office uniquement, réservé aux réviseurs/compliance.

## File de revue en masse — `/sni/api/v1/review`

🔒 Nécessite `DOCUMENT:REVIEW`, globalement ou par espace (`DOCUMENT_REVIEW_<SPACE>`).

| Méthode &amp; Route | Rôle | Paramètres clés | Réponse |
|---|---|---|---|
| `GET /review/queue` | File de revue globale (tous espaces), paginée | `Pageable` | `PaginatedResponse<DocumentResponse>` |
| `GET /review/{space}` | File de revue d'un espace précis | path `space` (`DocumentSpace`) | `PaginatedResponse<DocumentResponse>` |
| `POST /review/{space}/bulk-approve` | Approuver plusieurs documents d'un coup | `codes*` (liste), `reviewedBy*`, `comment` | `DocumentBulkActionResponse` |
| `POST /review/{space}/bulk-reject` | Rejeter plusieurs documents | `codes*`, `reviewedBy*`, `rejectionReasonCode`, `rejectionReasonDetail`, `comment` | `DocumentBulkActionResponse` |
| `POST /review/{space}/bulk-request-correction` | Demander correction sur plusieurs documents | `codes*`, `reviewedBy*`, `correctionNote*`, `deadlineDays` (défaut 7) | `DocumentBulkActionResponse` |

⚠️ **Un seul motif/commentaire pour tout le lot** — les actions en masse n'acceptent pas un motif différent par document (contrairement au KYC, voir [kyc-document-review-admin.md](./kyc-document-review-admin.md)). La réponse détaille en revanche le résultat individuel de chaque élément (`results[]`: code, status, reason).

## Workflows d'approbation personnalisés — `/sni/api/v1`

Pour les documents nécessitant plusieurs étapes de validation successives (au-delà du simple approve/reject).

| Méthode &amp; Route | Rôle | Paramètres clés | Réponse |
|---|---|---|---|
| `POST /document-workflows` | Créer une définition de workflow | `name*`, `description`, `workflowType`, `space`, `documentTypeCode`, `steps*[]` (`stepName*`, `approverType*`, `approverValue*`, `required`, `autoApproveDays`) | `WorkflowResponse` (201) |
| `GET /document-workflows/{code}` | Détail d'une définition | — | `WorkflowResponse` |
| `GET /document-workflows` | Liste des définitions | query `activeOnly` (optionnel) | `List<WorkflowResponse>` |
| `DELETE /document-workflows/{code}` | Supprimer une définition | — | 204 |
| `POST /documents/{documentCode}/start-workflow` | Démarrer une instance de workflow sur un document | query `workflowCode*` | `WorkflowInstanceResponse` (201) |
| `POST /documents/{documentCode}/workflow/approve` | Approuver l'étape courante | body optionnel `{ comment }` | `WorkflowInstanceResponse` |
| `POST /documents/{documentCode}/workflow/reject` | Rejeter l'étape courante | body optionnel `{ comment }` | `WorkflowInstanceResponse` |
| `GET /documents/{documentCode}/workflow/status` | Statut de l'instance en cours | — | `WorkflowInstanceResponse` |
| `GET /document-workflows/my-pending` | Instances en attente de l'utilisateur connecté | — | `List<WorkflowInstanceResponse>` |

Aucune permission spécifique déclarée (authentification admin standard).

## Politiques de rétention — `/sni/api/v1/document-retention-policies`

🔒 Toutes les routes nécessitent `DOCUMENT:REVIEW`.

| Méthode &amp; Route | Rôle | Paramètres clés |
|---|---|---|
| `GET /document-retention-policies` | Liste des politiques | — |
| `GET /document-retention-policies/{code}` | Détail | — |
| `POST /document-retention-policies` | Créer | `code*`, `name*`, `space` (optionnel, nul = global), `documentTypeCode`, `retentionDays*` (≥1), `retentionReference` (défaut `UPLOAD_DATE`), `action` (défaut `ARCHIVE`) |
| `PUT /document-retention-policies/{code}` | Modifier | mêmes champs |
| `DELETE /document-retention-policies/{code}` | Désactiver (soft-delete) | — |

**Enums** :
- `DocumentRetentionAction` : `ARCHIVE, PURGE_FLAG`
- `DocumentRetentionReference` : `UPLOAD_DATE, EXPIRY_DATE`
