# crm : routes

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

29 routes. Le fonctionnement et les parcours sont dans [README.md](README.md).

| Verbe | Chemin | Accès | Détail |
| --- | --- | --- | --- |
| GET | `/sni/api/v1/crm/analytics` | CRM:READ | [CrmController](routes/CrmController.md#r-get-sni-api-v1-crm-analytics) |
| GET | `/sni/api/v1/crm/board` | CRM:READ | [CrmController](routes/CrmController.md#r-get-sni-api-v1-crm-board) |
| GET | `/sni/api/v1/crm/leads` | CRM:READ | [CrmController](routes/CrmController.md#r-get-sni-api-v1-crm-leads) |
| POST | `/sni/api/v1/crm/leads` | CRM:WRITE | [CrmController](routes/CrmController.md#r-post-sni-api-v1-crm-leads) |
| GET | `/sni/api/v1/crm/leads/number/{leadNumber}` | CRM:READ | [CrmController](routes/CrmController.md#r-get-sni-api-v1-crm-leads-number-leadnumber) |
| GET | `/sni/api/v1/crm/leads/{id}` | CRM:READ | [CrmController](routes/CrmController.md#r-get-sni-api-v1-crm-leads-id) |
| PUT | `/sni/api/v1/crm/leads/{id}` | CRM:WRITE | [CrmController](routes/CrmController.md#r-put-sni-api-v1-crm-leads-id) |
| POST | `/sni/api/v1/crm/leads/{id}/activities` | CRM:WRITE | [CrmController](routes/CrmController.md#r-post-sni-api-v1-crm-leads-id-activities) |
| PATCH | `/sni/api/v1/crm/leads/{id}/convert` | CRM:CONVERT | [CrmController](routes/CrmController.md#r-patch-sni-api-v1-crm-leads-id-convert) |
| POST | `/sni/api/v1/crm/leads/{id}/generate-quote` | CRM:WRITE | [CrmController](routes/CrmController.md#r-post-sni-api-v1-crm-leads-id-generate-quote) |
| POST | `/sni/api/v1/crm/leads/{id}/opportunities` | CRM:WRITE | [CrmController](routes/CrmController.md#r-post-sni-api-v1-crm-leads-id-opportunities) |
| PATCH | `/sni/api/v1/crm/leads/{id}/qualify` | CRM:WRITE | [CrmController](routes/CrmController.md#r-patch-sni-api-v1-crm-leads-id-qualify) |
| PATCH | `/sni/api/v1/crm/leads/{id}/stage` | CRM:WRITE | [CrmController](routes/CrmController.md#r-patch-sni-api-v1-crm-leads-id-stage) |
| GET | `/sni/api/v1/crm/metrics` | CRM:READ | [CrmController](routes/CrmController.md#r-get-sni-api-v1-crm-metrics) |
| GET | `/sni/api/v1/crm/opportunities` | CRM:READ | [CrmController](routes/CrmController.md#r-get-sni-api-v1-crm-opportunities) |
| PATCH | `/sni/api/v1/crm/opportunities/{id}/stage` | CRM:WRITE | [CrmController](routes/CrmController.md#r-patch-sni-api-v1-crm-opportunities-id-stage) |
| GET | `/sni/api/v1/crm/pipeline` | CRM:READ | [CrmController](routes/CrmController.md#r-get-sni-api-v1-crm-pipeline) |
| GET | `/sni/api/v1/crm/proposals` | CRM:READ | [ProposalController](routes/ProposalController.md#r-get-sni-api-v1-crm-proposals) |
| POST | `/sni/api/v1/crm/proposals` | CRM:WRITE | [ProposalController](routes/ProposalController.md#r-post-sni-api-v1-crm-proposals) |
| GET | `/sni/api/v1/crm/proposals/number/{proposalNumber}` | CRM:READ | [ProposalController](routes/ProposalController.md#r-get-sni-api-v1-crm-proposals-number-proposalnumber) |
| GET | `/sni/api/v1/crm/proposals/{id}` | CRM:READ | [ProposalController](routes/ProposalController.md#r-get-sni-api-v1-crm-proposals-id) |
| PUT | `/sni/api/v1/crm/proposals/{id}` | CRM:WRITE | [ProposalController](routes/ProposalController.md#r-put-sni-api-v1-crm-proposals-id) |
| PATCH | `/sni/api/v1/crm/proposals/{id}/accept` | CRM:WRITE | [ProposalController](routes/ProposalController.md#r-patch-sni-api-v1-crm-proposals-id-accept) |
| POST | `/sni/api/v1/crm/proposals/{id}/lines` | CRM:WRITE | [ProposalController](routes/ProposalController.md#r-post-sni-api-v1-crm-proposals-id-lines) |
| DELETE | `/sni/api/v1/crm/proposals/{id}/lines/{lineId}` | CRM:WRITE | [ProposalController](routes/ProposalController.md#r-delete-sni-api-v1-crm-proposals-id-lines-lineid) |
| PUT | `/sni/api/v1/crm/proposals/{id}/lines/{lineId}` | CRM:WRITE | [ProposalController](routes/ProposalController.md#r-put-sni-api-v1-crm-proposals-id-lines-lineid) |
| PATCH | `/sni/api/v1/crm/proposals/{id}/reject` | CRM:WRITE | [ProposalController](routes/ProposalController.md#r-patch-sni-api-v1-crm-proposals-id-reject) |
| PATCH | `/sni/api/v1/crm/proposals/{id}/send` | CRM:WRITE | [ProposalController](routes/ProposalController.md#r-patch-sni-api-v1-crm-proposals-id-send) |
| POST | `/sni/api/v1/public/crm/leads` | public | [PublicCrmController](routes/PublicCrmController.md#r-post-sni-api-v1-public-crm-leads) |
