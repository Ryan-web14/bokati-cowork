# support : routes

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

49 routes. Le fonctionnement et les parcours sont dans [README.md](README.md).

| Verbe | Chemin | Accès | Détail |
| --- | --- | --- | --- |
| GET | `/sni/api/v1/client/support/knowledge-base` | membre | [ClientKnowledgeArticleController](routes/ClientKnowledgeArticleController.md#r-get-sni-api-v1-client-support-knowledge-base) |
| GET | `/sni/api/v1/client/support/knowledge-base/suggestions` | membre | [ClientKnowledgeArticleController](routes/ClientKnowledgeArticleController.md#r-get-sni-api-v1-client-support-knowledge-base-suggestions) |
| GET | `/sni/api/v1/client/support/knowledge-base/{slug}` | membre | [ClientKnowledgeArticleController](routes/ClientKnowledgeArticleController.md#r-get-sni-api-v1-client-support-knowledge-base-slug) |
| GET | `/sni/api/v1/client/support/tickets` | membre | [ClientSupportController](routes/ClientSupportController.md#r-get-sni-api-v1-client-support-tickets) |
| POST | `/sni/api/v1/client/support/tickets` | membre | [ClientSupportController](routes/ClientSupportController.md#r-post-sni-api-v1-client-support-tickets) |
| GET | `/sni/api/v1/client/support/tickets/summary` | membre | [ClientSupportController](routes/ClientSupportController.md#r-get-sni-api-v1-client-support-tickets-summary) |
| GET | `/sni/api/v1/client/support/tickets/{ticketNumber}` | membre | [ClientSupportController](routes/ClientSupportController.md#r-get-sni-api-v1-client-support-tickets-ticketnumber) |
| GET | `/sni/api/v1/client/support/tickets/{ticketNumber}/attachments` | membre | [ClientSupportController](routes/ClientSupportController.md#r-get-sni-api-v1-client-support-tickets-ticketnumber-attachments) |
| POST | `/sni/api/v1/client/support/tickets/{ticketNumber}/attachments` | membre | [ClientSupportController](routes/ClientSupportController.md#r-post-sni-api-v1-client-support-tickets-ticketnumber-attachments) |
| GET | `/sni/api/v1/client/support/tickets/{ticketNumber}/attachments/{id}/download` | membre | [ClientSupportController](routes/ClientSupportController.md#r-get-sni-api-v1-client-support-tickets-ticketnumber-attachments-id-download) |
| POST | `/sni/api/v1/client/support/tickets/{ticketNumber}/close` | membre | [ClientSupportController](routes/ClientSupportController.md#r-post-sni-api-v1-client-support-tickets-ticketnumber-close) |
| POST | `/sni/api/v1/client/support/tickets/{ticketNumber}/csat` | membre | [ClientSupportController](routes/ClientSupportController.md#r-post-sni-api-v1-client-support-tickets-ticketnumber-csat) |
| POST | `/sni/api/v1/client/support/tickets/{ticketNumber}/messages` | membre | [ClientSupportController](routes/ClientSupportController.md#r-post-sni-api-v1-client-support-tickets-ticketnumber-messages) |
| GET | `/sni/api/v1/support/knowledge-base` | SUPPORT:READ | [KnowledgeArticleController](routes/KnowledgeArticleController.md#r-get-sni-api-v1-support-knowledge-base) |
| POST | `/sni/api/v1/support/knowledge-base` | SUPPORT:WRITE | [KnowledgeArticleController](routes/KnowledgeArticleController.md#r-post-sni-api-v1-support-knowledge-base) |
| GET | `/sni/api/v1/support/knowledge-base/analytics` | SUPPORT:METRICS | [KnowledgeArticleController](routes/KnowledgeArticleController.md#r-get-sni-api-v1-support-knowledge-base-analytics) |
| GET | `/sni/api/v1/support/knowledge-base/suggestions` | SUPPORT:READ | [KnowledgeArticleController](routes/KnowledgeArticleController.md#r-get-sni-api-v1-support-knowledge-base-suggestions) |
| DELETE | `/sni/api/v1/support/knowledge-base/{articleCode}` | SUPPORT:WRITE | [KnowledgeArticleController](routes/KnowledgeArticleController.md#r-delete-sni-api-v1-support-knowledge-base-articlecode) |
| GET | `/sni/api/v1/support/knowledge-base/{articleCode}` | SUPPORT:READ | [KnowledgeArticleController](routes/KnowledgeArticleController.md#r-get-sni-api-v1-support-knowledge-base-articlecode) |
| PUT | `/sni/api/v1/support/knowledge-base/{articleCode}` | SUPPORT:WRITE | [KnowledgeArticleController](routes/KnowledgeArticleController.md#r-put-sni-api-v1-support-knowledge-base-articlecode) |
| GET | `/sni/api/v1/public/support/csat` | public | [SupportCsatPublicController](routes/SupportCsatPublicController.md#r-get-sni-api-v1-public-support-csat) |
| GET | `/sni/api/v1/support/quick-replies` | SUPPORT:READ | [SupportQuickReplyController](routes/SupportQuickReplyController.md#r-get-sni-api-v1-support-quick-replies) |
| POST | `/sni/api/v1/support/quick-replies` | SUPPORT:WRITE | [SupportQuickReplyController](routes/SupportQuickReplyController.md#r-post-sni-api-v1-support-quick-replies) |
| DELETE | `/sni/api/v1/support/quick-replies/{id}` | SUPPORT:WRITE | [SupportQuickReplyController](routes/SupportQuickReplyController.md#r-delete-sni-api-v1-support-quick-replies-id) |
| GET | `/sni/api/v1/support/routing-rules` | SUPPORT:READ | [SupportRoutingRuleController](routes/SupportRoutingRuleController.md#r-get-sni-api-v1-support-routing-rules) |
| POST | `/sni/api/v1/support/routing-rules` | SUPPORT:WRITE | [SupportRoutingRuleController](routes/SupportRoutingRuleController.md#r-post-sni-api-v1-support-routing-rules) |
| DELETE | `/sni/api/v1/support/routing-rules/{id}` | SUPPORT:WRITE | [SupportRoutingRuleController](routes/SupportRoutingRuleController.md#r-delete-sni-api-v1-support-routing-rules-id) |
| PUT | `/sni/api/v1/support/routing-rules/{id}` | SUPPORT:WRITE | [SupportRoutingRuleController](routes/SupportRoutingRuleController.md#r-put-sni-api-v1-support-routing-rules-id) |
| GET | `/sni/api/v1/support/tags` | SUPPORT:READ | [SupportTagController](routes/SupportTagController.md#r-get-sni-api-v1-support-tags) |
| GET | `/sni/api/v1/support/tickets` | SUPPORT:READ | [SupportTicketController](routes/SupportTicketController.md#r-get-sni-api-v1-support-tickets) |
| POST | `/sni/api/v1/support/tickets` | SUPPORT:WRITE | [SupportTicketController](routes/SupportTicketController.md#r-post-sni-api-v1-support-tickets) |
| GET | `/sni/api/v1/support/tickets/agents/performance` | SUPPORT:METRICS | [SupportTicketController](routes/SupportTicketController.md#r-get-sni-api-v1-support-tickets-agents-performance) |
| GET | `/sni/api/v1/support/tickets/analytics` | SUPPORT:METRICS | [SupportTicketController](routes/SupportTicketController.md#r-get-sni-api-v1-support-tickets-analytics) |
| GET | `/sni/api/v1/support/tickets/analytics.csv` | SUPPORT:METRICS | [SupportTicketController](routes/SupportTicketController.md#r-get-sni-api-v1-support-tickets-analytics-csv) |
| GET | `/sni/api/v1/support/tickets/by-owner` | SUPPORT:READ | [SupportTicketController](routes/SupportTicketController.md#r-get-sni-api-v1-support-tickets-by-owner) |
| GET | `/sni/api/v1/support/tickets/metrics` | SUPPORT:METRICS | [SupportTicketController](routes/SupportTicketController.md#r-get-sni-api-v1-support-tickets-metrics) |
| GET | `/sni/api/v1/support/tickets/{ticketNumber}` | SUPPORT:READ | [SupportTicketController](routes/SupportTicketController.md#r-get-sni-api-v1-support-tickets-ticketnumber) |
| PATCH | `/sni/api/v1/support/tickets/{ticketNumber}/assign` | SUPPORT:ASSIGN | [SupportTicketController](routes/SupportTicketController.md#r-patch-sni-api-v1-support-tickets-ticketnumber-assign) |
| GET | `/sni/api/v1/support/tickets/{ticketNumber}/attachments` | SUPPORT:READ | [SupportTicketController](routes/SupportTicketController.md#r-get-sni-api-v1-support-tickets-ticketnumber-attachments) |
| POST | `/sni/api/v1/support/tickets/{ticketNumber}/attachments` | SUPPORT:WRITE | [SupportTicketController](routes/SupportTicketController.md#r-post-sni-api-v1-support-tickets-ticketnumber-attachments) |
| DELETE | `/sni/api/v1/support/tickets/{ticketNumber}/attachments/{id}` | SUPPORT:WRITE | [SupportTicketController](routes/SupportTicketController.md#r-delete-sni-api-v1-support-tickets-ticketnumber-attachments-id) |
| GET | `/sni/api/v1/support/tickets/{ticketNumber}/attachments/{id}/download` | SUPPORT:READ | [SupportTicketController](routes/SupportTicketController.md#r-get-sni-api-v1-support-tickets-ticketnumber-attachments-id-download) |
| POST | `/sni/api/v1/support/tickets/{ticketNumber}/messages` | SUPPORT:WRITE | [SupportTicketController](routes/SupportTicketController.md#r-post-sni-api-v1-support-tickets-ticketnumber-messages) |
| POST | `/sni/api/v1/support/tickets/{ticketNumber}/messages/{messageId}/convert-to-article` | SUPPORT:WRITE | [SupportTicketController](routes/SupportTicketController.md#r-post-sni-api-v1-support-tickets-ticketnumber-messages-messageid-convert-to-article) |
| PATCH | `/sni/api/v1/support/tickets/{ticketNumber}/status` | SUPPORT:WRITE | [SupportTicketController](routes/SupportTicketController.md#r-patch-sni-api-v1-support-tickets-ticketnumber-status) |
| POST | `/sni/api/v1/support/tickets/{ticketNumber}/tags` | SUPPORT:WRITE | [SupportTicketController](routes/SupportTicketController.md#r-post-sni-api-v1-support-tickets-ticketnumber-tags) |
| DELETE | `/sni/api/v1/support/tickets/{ticketNumber}/tags/{tag}` | SUPPORT:WRITE | [SupportTicketController](routes/SupportTicketController.md#r-delete-sni-api-v1-support-tickets-ticketnumber-tags-tag) |
| POST | `/sni/api/v1/support/tickets/{ticketNumber}/task` | SUPPORT:WRITE | [SupportTicketController](routes/SupportTicketController.md#r-post-sni-api-v1-support-tickets-ticketnumber-task) |
| GET | `/sni/api/v1/support/tickets/{ticketNumber}/timeline` | SUPPORT:READ | [SupportTicketController](routes/SupportTicketController.md#r-get-sni-api-v1-support-tickets-ticketnumber-timeline) |
