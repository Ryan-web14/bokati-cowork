# DocumentWorkflowController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [document](../README.md) · 9 route(s).

## Routes

<a id="r-get-sni-api-v1-document-workflows"></a>
### GET /sni/api/v1/document-workflows

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `List<WorkflowResponse>`
- Modèles : [WorkflowResponse](#m-WorkflowResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentWorkflowController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `activeOnly` | requête | `Boolean` |  |  |

<a id="r-post-sni-api-v1-document-workflows"></a>
### POST /sni/api/v1/document-workflows

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (obligatoire) : `CreateWorkflowRequest`
- Réponse : `WorkflowResponse`
- Modèles : [WorkflowResponse](#m-WorkflowResponse), [CreateWorkflowRequest](#m-CreateWorkflowRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentWorkflowController.java`, méthode `create`

<a id="r-get-sni-api-v1-document-workflows-my-pending"></a>
### GET /sni/api/v1/document-workflows/my-pending

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `List<WorkflowInstanceResponse>`
- Modèles : [WorkflowInstanceResponse](#m-WorkflowInstanceResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentWorkflowController.java`, méthode `myPending`

<a id="r-delete-sni-api-v1-document-workflows-code"></a>
### DELETE /sni/api/v1/document-workflows/{code}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentWorkflowController.java`, méthode `delete`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-document-workflows-code"></a>
### GET /sni/api/v1/document-workflows/{code}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `WorkflowResponse`
- Modèles : [WorkflowResponse](#m-WorkflowResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentWorkflowController.java`, méthode `getByCode`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-documents-documentcode-start-workflow"></a>
### POST /sni/api/v1/documents/{documentCode}/start-workflow

- Accès : back-office : rôle d'administration et droit `DOCUMENT:UPLOAD`
- Réponse : `WorkflowInstanceResponse`
- Modèles : [WorkflowInstanceResponse](#m-WorkflowInstanceResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentWorkflowController.java`, méthode `startWorkflow`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentCode` | chemin | `String` | oui |  |
| `workflowCode` | requête | `String` | oui |  |

<a id="r-post-sni-api-v1-documents-documentcode-workflow-approve"></a>
### POST /sni/api/v1/documents/{documentCode}/workflow/approve

- Accès : back-office : rôle d'administration et droit `DOCUMENT:REVIEW`
- Corps JSON (facultatif) : `WorkflowActionRequest`
- Réponse : `WorkflowInstanceResponse`
- Modèles : [WorkflowInstanceResponse](#m-WorkflowInstanceResponse), [WorkflowActionRequest](#m-WorkflowActionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentWorkflowController.java`, méthode `approve`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentCode` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-documents-documentcode-workflow-reject"></a>
### POST /sni/api/v1/documents/{documentCode}/workflow/reject

- Accès : back-office : rôle d'administration et droit `DOCUMENT:REVIEW`
- Corps JSON (facultatif) : `WorkflowActionRequest`
- Réponse : `WorkflowInstanceResponse`
- Modèles : [WorkflowInstanceResponse](#m-WorkflowInstanceResponse), [WorkflowActionRequest](#m-WorkflowActionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentWorkflowController.java`, méthode `reject`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-documents-documentcode-workflow-status"></a>
### GET /sni/api/v1/documents/{documentCode}/workflow/status

- Accès : back-office : rôle d'administration et droit `DOCUMENT:READ`
- Réponse : `WorkflowInstanceResponse`
- Modèles : [WorkflowInstanceResponse](#m-WorkflowInstanceResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/documentMaster/controller/DocumentWorkflowController.java`, méthode `status`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `documentCode` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-WorkflowResponse"></a>
#### WorkflowResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `workflowType` | texte |  |  |
| `space` | enum : `KYC_SPACE`, `CONTRACT_SPACE`, `FINANCIAL_SPACE`, `ASSET_SPACE`, `ADMINISTRATIVE`, `GENERIC` |  |  |
| `documentTypeCode` | texte |  |  |
| `active` | booléen |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `steps` | [WorkflowResponse.StepResponse](#m-WorkflowResponse-StepResponse)[] |  |  |

<a id="m-CreateWorkflowRequest"></a>
#### CreateWorkflowRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte | oui | longueur min `1` |
| `description` | texte |  |  |
| `workflowType` | texte |  |  |
| `space` | enum : `KYC_SPACE`, `CONTRACT_SPACE`, `FINANCIAL_SPACE`, `ASSET_SPACE`, `ADMINISTRATIVE`, `GENERIC` |  |  |
| `documentTypeCode` | texte |  |  |
| `steps` | [CreateWorkflowRequest.WorkflowStepRequest](#m-CreateWorkflowRequest-WorkflowStepRequest)[] | oui | éléments min `1` |

<a id="m-WorkflowInstanceResponse"></a>
#### WorkflowInstanceResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `workflowCode` | texte |  |  |
| `workflowName` | texte |  |  |
| `documentCode` | texte |  |  |
| `documentTitle` | texte |  |  |
| `currentStep` | entier |  |  |
| `currentStepName` | texte |  |  |
| `status` | texte |  |  |
| `startedAt` | date-heure ISO 8601 UTC |  |  |
| `completedAt` | date-heure ISO 8601 UTC |  |  |
| `actions` | [WorkflowInstanceResponse.ActionResponse](#m-WorkflowInstanceResponse-ActionResponse)[] |  |  |

<a id="m-WorkflowActionRequest"></a>
#### WorkflowActionRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `comment` | texte |  |  |

<a id="m-WorkflowResponse-StepResponse"></a>
#### WorkflowResponse.StepResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `stepOrder` | entier |  |  |
| `stepName` | texte |  |  |
| `approverType` | texte |  |  |
| `approverValue` | texte |  |  |
| `required` | booléen |  |  |
| `autoApproveDays` | entier |  |  |

<a id="m-CreateWorkflowRequest-WorkflowStepRequest"></a>
#### CreateWorkflowRequest.WorkflowStepRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `stepName` | texte | oui | longueur min `1` |
| `approverType` | texte | oui | longueur min `1` |
| `approverValue` | texte | oui | longueur min `1` |
| `required` | booléen |  |  |
| `autoApproveDays` | entier |  |  |

<a id="m-WorkflowInstanceResponse-ActionResponse"></a>
#### WorkflowInstanceResponse.ActionResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `stepOrder` | entier |  |  |
| `stepName` | texte |  |  |
| `action` | texte |  |  |
| `actorId` | entier int64 ⚠ |  |  |
| `actorEmail` | texte |  |  |
| `actedAt` | date-heure ISO 8601 UTC |  |  |
| `comment` | texte |  |  |
