# approval : routes

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

14 routes. Le fonctionnement et les parcours sont dans [README.md](README.md).

| Verbe | Chemin | Accès | Détail |
| --- | --- | --- | --- |
| GET | `/sni/api/v1/approvals/circuits` | ADMIN:ACCESS | [ApprovalController](routes/ApprovalController.md#r-get-sni-api-v1-approvals-circuits) |
| POST | `/sni/api/v1/approvals/circuits` | APPROVAL:MANAGE | [ApprovalController](routes/ApprovalController.md#r-post-sni-api-v1-approvals-circuits) |
| GET | `/sni/api/v1/approvals/delegations` | ADMIN:ACCESS | [ApprovalController](routes/ApprovalController.md#r-get-sni-api-v1-approvals-delegations) |
| POST | `/sni/api/v1/approvals/delegations` | ADMIN:ACCESS | [ApprovalController](routes/ApprovalController.md#r-post-sni-api-v1-approvals-delegations) |
| DELETE | `/sni/api/v1/approvals/delegations/{id}` | ADMIN:ACCESS | [ApprovalController](routes/ApprovalController.md#r-delete-sni-api-v1-approvals-delegations-id) |
| GET | `/sni/api/v1/approvals/hierarchy` | ADMIN:ACCESS | [ApprovalController](routes/ApprovalController.md#r-get-sni-api-v1-approvals-hierarchy) |
| PUT | `/sni/api/v1/approvals/hierarchy` | APPROVAL:MANAGE | [ApprovalController](routes/ApprovalController.md#r-put-sni-api-v1-approvals-hierarchy) |
| GET | `/sni/api/v1/approvals/requests/{number}` | ADMIN:ACCESS | [ApprovalController](routes/ApprovalController.md#r-get-sni-api-v1-approvals-requests-number) |
| GET | `/sni/api/v1/approvals/{center}/history` | ADMIN:ACCESS | [ApprovalController](routes/ApprovalController.md#r-get-sni-api-v1-approvals-center-history) |
| GET | `/sni/api/v1/approvals/{center}/mine` | ADMIN:ACCESS | [ApprovalController](routes/ApprovalController.md#r-get-sni-api-v1-approvals-center-mine) |
| GET | `/sni/api/v1/approvals/{center}/to-approve` | ADMIN:ACCESS | [ApprovalController](routes/ApprovalController.md#r-get-sni-api-v1-approvals-center-to-approve) |
| PATCH | `/sni/api/v1/approvals/{number}/approve` | ADMIN:ACCESS | [ApprovalController](routes/ApprovalController.md#r-patch-sni-api-v1-approvals-number-approve) |
| PATCH | `/sni/api/v1/approvals/{number}/reject` | ADMIN:ACCESS | [ApprovalController](routes/ApprovalController.md#r-patch-sni-api-v1-approvals-number-reject) |
| PATCH | `/sni/api/v1/approvals/{number}/send-back` | ADMIN:ACCESS | [ApprovalController](routes/ApprovalController.md#r-patch-sni-api-v1-approvals-number-send-back) |
