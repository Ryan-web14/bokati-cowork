# ApprovalController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [approval](../README.md) · 14 route(s).

## Routes

<a id="r-get-sni-api-v1-approvals-circuits"></a>
### GET /sni/api/v1/approvals/circuits

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `List<ApprovalDtos.CircuitView>`
- Modèles : [ApprovalDtos.CircuitView](#m-ApprovalDtos-CircuitView)
- Code : `src/main/java/com/sni/bokaticowork/features/approval/controller/ApprovalController.java`, méthode `circuits`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `center` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-approvals-circuits"></a>
### POST /sni/api/v1/approvals/circuits

- Accès : back-office : rôle d'administration et droit `APPROVAL:MANAGE` ; contrôle supplémentaire sur la méthode : `hasAuthority('APPROVAL:MANAGE')`
- Corps JSON (obligatoire) : `ApprovalDtos.CircuitInput`
- Réponse : `ApprovalDtos.CircuitView`
- Modèles : [ApprovalDtos.CircuitView](#m-ApprovalDtos-CircuitView), [ApprovalDtos.CircuitInput](#m-ApprovalDtos-CircuitInput)
- Code : `src/main/java/com/sni/bokaticowork/features/approval/controller/ApprovalController.java`, méthode `saveCircuit`

<a id="r-get-sni-api-v1-approvals-delegations"></a>
### GET /sni/api/v1/approvals/delegations

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `List<ApprovalDtos.DelegationView>`
- Modèles : [ApprovalDtos.DelegationView](#m-ApprovalDtos-DelegationView)
- Code : `src/main/java/com/sni/bokaticowork/features/approval/controller/ApprovalController.java`, méthode `delegations`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `center` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-approvals-delegations"></a>
### POST /sni/api/v1/approvals/delegations

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (obligatoire) : `ApprovalDtos.DelegationInput`
- Réponse : `ApprovalDtos.DelegationView`
- Modèles : [ApprovalDtos.DelegationView](#m-ApprovalDtos-DelegationView), [ApprovalDtos.DelegationInput](#m-ApprovalDtos-DelegationInput)
- Code : `src/main/java/com/sni/bokaticowork/features/approval/controller/ApprovalController.java`, méthode `delegate`

<a id="r-delete-sni-api-v1-approvals-delegations-id"></a>
### DELETE /sni/api/v1/approvals/delegations/{id}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/approval/controller/ApprovalController.java`, méthode `cancelDelegation`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-approvals-hierarchy"></a>
### GET /sni/api/v1/approvals/hierarchy

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `List<ApprovalDtos.HierarchyNode>`
- Modèles : [ApprovalDtos.HierarchyNode](#m-ApprovalDtos-HierarchyNode)
- Code : `src/main/java/com/sni/bokaticowork/features/approval/controller/ApprovalController.java`, méthode `hierarchy`

<a id="r-put-sni-api-v1-approvals-hierarchy"></a>
### PUT /sni/api/v1/approvals/hierarchy

- Accès : back-office : rôle d'administration et droit `APPROVAL:MANAGE` ; contrôle supplémentaire sur la méthode : `hasAuthority('APPROVAL:MANAGE')`
- Corps JSON (obligatoire) : `ApprovalDtos.HierarchyUpdate`
- Réponse : `List<ApprovalDtos.HierarchyNode>`
- Modèles : [ApprovalDtos.HierarchyNode](#m-ApprovalDtos-HierarchyNode), [ApprovalDtos.HierarchyUpdate](#m-ApprovalDtos-HierarchyUpdate)
- Code : `src/main/java/com/sni/bokaticowork/features/approval/controller/ApprovalController.java`, méthode `updateHierarchy`

<a id="r-get-sni-api-v1-approvals-requests-number"></a>
### GET /sni/api/v1/approvals/requests/{number}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `ApprovalDtos.RequestView`
- Modèles : [ApprovalDtos.RequestView](#m-ApprovalDtos-RequestView)
- Code : `src/main/java/com/sni/bokaticowork/features/approval/controller/ApprovalController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-approvals-center-history"></a>
### GET /sni/api/v1/approvals/{center}/history

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `PaginatedResponse<ApprovalDtos.RequestView>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [ApprovalDtos.RequestView](#m-ApprovalDtos-RequestView), [ApprovalStatus](#m-ApprovalStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/approval/controller/ApprovalController.java`, méthode `history`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `center` | chemin | `String` | oui |  |
| `type` | requête | `String` |  |  |
| `requester` | requête | `String` |  |  |
| `approver` | requête | `String` |  |  |
| `status` | requête | `ApprovalStatus` |  |  |
| `from` | requête | `LocalDate` |  |  |
| `to` | requête | `LocalDate` |  |  |
| `page` | requête | `int` |  | 0 |
| `size` | requête | `int` |  | 20 |

<a id="r-get-sni-api-v1-approvals-center-mine"></a>
### GET /sni/api/v1/approvals/{center}/mine

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `List<ApprovalDtos.RequestView>`
- Modèles : [ApprovalDtos.RequestView](#m-ApprovalDtos-RequestView)
- Code : `src/main/java/com/sni/bokaticowork/features/approval/controller/ApprovalController.java`, méthode `mine`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `center` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-approvals-center-to-approve"></a>
### GET /sni/api/v1/approvals/{center}/to-approve

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Réponse : `List<ApprovalDtos.RequestView>`
- Modèles : [ApprovalDtos.RequestView](#m-ApprovalDtos-RequestView)
- Code : `src/main/java/com/sni/bokaticowork/features/approval/controller/ApprovalController.java`, méthode `toApprove`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `center` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-approvals-number-approve"></a>
### PATCH /sni/api/v1/approvals/{number}/approve

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (facultatif) : `ApprovalDtos.Decision`
- Réponse : `ApprovalDtos.RequestView`
- Modèles : [ApprovalDtos.RequestView](#m-ApprovalDtos-RequestView), [ApprovalDtos.Decision](#m-ApprovalDtos-Decision)
- Code : `src/main/java/com/sni/bokaticowork/features/approval/controller/ApprovalController.java`, méthode `approve`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-approvals-number-reject"></a>
### PATCH /sni/api/v1/approvals/{number}/reject

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (obligatoire) : `ApprovalDtos.Decision`
- Réponse : `ApprovalDtos.RequestView`
- Modèles : [ApprovalDtos.RequestView](#m-ApprovalDtos-RequestView), [ApprovalDtos.Decision](#m-ApprovalDtos-Decision)
- Code : `src/main/java/com/sni/bokaticowork/features/approval/controller/ApprovalController.java`, méthode `reject`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-approvals-number-send-back"></a>
### PATCH /sni/api/v1/approvals/{number}/send-back

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS`
- Corps JSON (obligatoire) : `ApprovalDtos.Decision`
- Réponse : `ApprovalDtos.RequestView`
- Modèles : [ApprovalDtos.RequestView](#m-ApprovalDtos-RequestView), [ApprovalDtos.Decision](#m-ApprovalDtos-Decision)
- Code : `src/main/java/com/sni/bokaticowork/features/approval/controller/ApprovalController.java`, méthode `sendBack`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `number` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ApprovalDtos-CircuitView"></a>
#### ApprovalDtos.CircuitView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `center` | texte |  |  |
| `requestType` | texte |  |  |
| `label` | texte |  |  |
| `active` | booléen |  |  |
| `levels` | [ApprovalDtos.LevelSpec](#m-ApprovalDtos-LevelSpec)[] |  |  |

<a id="m-ApprovalDtos-CircuitInput"></a>
#### ApprovalDtos.CircuitInput

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte | oui | longueur min `0`, longueur max `60` |
| `center` | texte | oui | longueur min `0`, longueur max `30` |
| `requestType` | texte | oui | longueur min `0`, longueur max `60` |
| `label` | texte | oui | longueur min `0`, longueur max `160` |
| `active` | booléen |  |  |
| `levels` | [ApprovalDtos.LevelSpec](#m-ApprovalDtos-LevelSpec)[] | oui | éléments min `1` |

<a id="m-ApprovalDtos-DelegationView"></a>
#### ApprovalDtos.DelegationView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `delegator` | texte |  |  |
| `delegate` | texte |  |  |
| `center` | texte |  |  |
| `level` | entier |  |  |
| `startsOn` | date AAAA-MM-JJ |  |  |
| `endsOn` | date AAAA-MM-JJ |  |  |
| `reason` | texte |  |  |

<a id="m-ApprovalDtos-DelegationInput"></a>
#### ApprovalDtos.DelegationInput

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `delegate` | texte | oui | longueur min `1` |
| `center` | texte |  |  |
| `level` | entier |  |  |
| `startsOn` | date AAAA-MM-JJ | oui |  |
| `endsOn` | date AAAA-MM-JJ | oui |  |
| `reason` | texte |  | longueur min `0`, longueur max `500` |

<a id="m-ApprovalDtos-HierarchyNode"></a>
#### ApprovalDtos.HierarchyNode

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `userCode` | texte |  |  |
| `email` | texte |  |  |
| `fullName` | texte |  |  |
| `jobTitle` | texte |  |  |
| `managerUserCode` | texte |  |  |
| `managerName` | texte |  |  |

<a id="m-ApprovalDtos-HierarchyUpdate"></a>
#### ApprovalDtos.HierarchyUpdate

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `userCode` | texte | oui | longueur min `1` |
| `managerUserCode` | texte |  |  |

<a id="m-ApprovalDtos-RequestView"></a>
#### ApprovalDtos.RequestView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `number` | texte |  |  |
| `center` | texte |  |  |
| `requestType` | texte |  |  |
| `subjectType` | texte |  |  |
| `subjectRef` | texte |  |  |
| `title` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `requester` | texte |  |  |
| `requesterName` | texte |  |  |
| `status` | enum : `PENDING`, `APPROVED`, `REJECTED`, `RETURNED`, `CANCELLED` |  |  |
| `requiredLevels` | entier |  |  |
| `currentLevel` | entier |  |  |
| `currentLabel` | texte |  |  |
| `waitingFor` | texte |  |  |
| `levelSince` | date-heure ISO 8601 UTC |  |  |
| `lastComment` | texte |  |  |
| `submittedAt` | date-heure ISO 8601 UTC |  |  |
| `decidedAt` | date-heure ISO 8601 UTC |  |  |
| `decidedBy` | texte |  |  |
| `detailsJson` | texte |  |  |
| `canDecide` | booléen |  |  |
| `steps` | [ApprovalDtos.StepView](#m-ApprovalDtos-StepView)[] |  |  |
| `timeline` | [ApprovalDtos.EventView](#m-ApprovalDtos-EventView)[] |  |  |

<a id="m-ApprovalStatus"></a>
#### ApprovalStatus

Énumération : `PENDING`, `APPROVED`, `REJECTED`, `RETURNED`, `CANCELLED`

<a id="m-ApprovalDtos-Decision"></a>
#### ApprovalDtos.Decision

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `comment` | texte |  | longueur min `0`, longueur max `1000` |
| `reducedAmount` | nombre décimal |  |  |

<a id="m-ApprovalDtos-LevelSpec"></a>
#### ApprovalDtos.LevelSpec

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `label` | texte | oui | longueur min `0`, longueur max `120` |
| `rule` | enum : `MANAGER`, `PERMISSION`, `USER` | oui |  |
| `depth` | entier |  |  |
| `permission` | texte |  | longueur min `0`, longueur max `80` |
| `userLogin` | texte |  | longueur min `0`, longueur max `180` |
| `minAmount` | nombre décimal |  |  |

<a id="m-ApprovalDtos-StepView"></a>
#### ApprovalDtos.StepView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `level` | entier |  |  |
| `label` | texte |  |  |
| `rule` | enum : `MANAGER`, `PERMISSION`, `USER` |  |  |
| `expectedApprover` | texte |  |  |
| `expectedApproverName` | texte |  |  |
| `permission` | texte |  |  |
| `status` | enum : `WAITING`, `PENDING`, `APPROVED`, `APPROVED_BY_HIGHER`, `REJECTED`, `RETURNED`, `CANCELLED` |  |  |
| `decidedBy` | texte |  |  |
| `decidedByName` | texte |  |  |
| `delegatedFrom` | texte |  |  |
| `decidedAt` | date-heure ISO 8601 UTC |  |  |
| `comment` | texte |  |  |

<a id="m-ApprovalDtos-EventView"></a>
#### ApprovalDtos.EventView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `level` | entier |  |  |
| `actor` | texte |  |  |
| `actorName` | texte |  |  |
| `event` | enum : `SUBMITTED`, `APPROVED`, `APPROVED_BY_HIGHER`, `REJECTED`, `RETURNED`, `CANCELLED`, `REMINDED`, `ESCALATED` |  |  |
| `comment` | texte |  |  |
| `amountBefore` | nombre décimal |  |  |
| `amountAfter` | nombre décimal |  |  |
| `delegatedFrom` | texte |  |  |
| `occurredAt` | date-heure ISO 8601 UTC |  |  |
