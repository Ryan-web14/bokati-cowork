# fund : routes

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

44 routes. Le fonctionnement et les parcours sont dans [README.md](README.md).

| Verbe | Chemin | Accès | Détail |
| --- | --- | --- | --- |
| POST | `/sni/api/v1/fund-needs` | FUND_REQUEST:CREATE | [FundController](routes/FundController.md#r-post-sni-api-v1-fund-needs) |
| GET | `/sni/api/v1/fund-needs/mine` | FUND_REQUEST:CREATE | [FundController](routes/FundController.md#r-get-sni-api-v1-fund-needs-mine) |
| GET | `/sni/api/v1/fund-needs/submitted` | FUND_REQUEST:CREATE | [FundController](routes/FundController.md#r-get-sni-api-v1-fund-needs-submitted) |
| GET | `/sni/api/v1/fund-needs/{number}` | FUND_REQUEST:CREATE | [FundController](routes/FundController.md#r-get-sni-api-v1-fund-needs-number) |
| PUT | `/sni/api/v1/fund-needs/{number}` | FUND_REQUEST:CREATE | [FundController](routes/FundController.md#r-put-sni-api-v1-fund-needs-number) |
| PATCH | `/sni/api/v1/fund-needs/{number}/reject` | FUND_REQUEST:APPROVE_L1 | [FundController](routes/FundController.md#r-patch-sni-api-v1-fund-needs-number-reject) |
| PATCH | `/sni/api/v1/fund-needs/{number}/submit` | FUND_REQUEST:CREATE | [FundController](routes/FundController.md#r-patch-sni-api-v1-fund-needs-number-submit) |
| GET | `/sni/api/v1/fund-needs/{number}/supplier-suggestions` | FUND_REQUEST:CREATE | [FundController](routes/FundController.md#r-get-sni-api-v1-fund-needs-number-supplier-suggestions) |
| POST | `/sni/api/v1/fund-needs/{number}/transform` | FUND_REQUEST:CREATE | [FundController](routes/FundController.md#r-post-sni-api-v1-fund-needs-number-transform) |
| PATCH | `/sni/api/v1/fund-needs/{number}/validate` | FUND_REQUEST:APPROVE_L1 | [FundController](routes/FundController.md#r-patch-sni-api-v1-fund-needs-number-validate) |
| GET | `/sni/api/v1/fund-requests` | FUND_REQUEST:READ | [FundController](routes/FundController.md#r-get-sni-api-v1-fund-requests) |
| POST | `/sni/api/v1/fund-requests` | FUND_REQUEST:CREATE | [FundController](routes/FundController.md#r-post-sni-api-v1-fund-requests) |
| GET | `/sni/api/v1/fund-requests/advances` | FUND_REQUEST:READ | [FundController](routes/FundController.md#r-get-sni-api-v1-fund-requests-advances) |
| GET | `/sni/api/v1/fund-requests/delegations` | FUND_REQUEST:CREATE | [FundController](routes/FundController.md#r-get-sni-api-v1-fund-requests-delegations) |
| POST | `/sni/api/v1/fund-requests/delegations` | FUND_REQUEST:CREATE | [FundController](routes/FundController.md#r-post-sni-api-v1-fund-requests-delegations) |
| DELETE | `/sni/api/v1/fund-requests/delegations/{id}` | FUND_REQUEST:CREATE | [FundController](routes/FundController.md#r-delete-sni-api-v1-fund-requests-delegations-id) |
| POST | `/sni/api/v1/fund-requests/import-legacy` | FUND_REQUEST:READ | [FundController](routes/FundController.md#r-post-sni-api-v1-fund-requests-import-legacy) |
| GET | `/sni/api/v1/fund-requests/justifications-to-validate` | FUND_JUSTIFICATION:VALIDATE | [FundController](routes/FundController.md#r-get-sni-api-v1-fund-requests-justifications-to-validate) |
| GET | `/sni/api/v1/fund-requests/mine` | FUND_REQUEST:CREATE | [FundController](routes/FundController.md#r-get-sni-api-v1-fund-requests-mine) |
| GET | `/sni/api/v1/fund-requests/to-approve` | FUND_REQUEST:CREATE | [FundController](routes/FundController.md#r-get-sni-api-v1-fund-requests-to-approve) |
| GET | `/sni/api/v1/fund-requests/to-disburse` | FUND_REQUEST:DISBURSE | [FundController](routes/FundController.md#r-get-sni-api-v1-fund-requests-to-disburse) |
| GET | `/sni/api/v1/fund-requests/{number}` | FUND_REQUEST:CREATE | [FundController](routes/FundController.md#r-get-sni-api-v1-fund-requests-number) |
| PUT | `/sni/api/v1/fund-requests/{number}` | FUND_REQUEST:CREATE | [FundController](routes/FundController.md#r-put-sni-api-v1-fund-requests-number) |
| PATCH | `/sni/api/v1/fund-requests/{number}/approve` | FUND_REQUEST:CREATE | [FundController](routes/FundController.md#r-patch-sni-api-v1-fund-requests-number-approve) |
| PATCH | `/sni/api/v1/fund-requests/{number}/cancel` | FUND_REQUEST:CREATE | [FundController](routes/FundController.md#r-patch-sni-api-v1-fund-requests-number-cancel) |
| POST | `/sni/api/v1/fund-requests/{number}/complement` | FUND_REQUEST:DISBURSE | [FundController](routes/FundController.md#r-post-sni-api-v1-fund-requests-number-complement) |
| POST | `/sni/api/v1/fund-requests/{number}/disburse` | FUND_REQUEST:DISBURSE | [FundController](routes/FundController.md#r-post-sni-api-v1-fund-requests-number-disburse) |
| POST | `/sni/api/v1/fund-requests/{number}/duplicate` | FUND_REQUEST:CREATE | [FundController](routes/FundController.md#r-post-sni-api-v1-fund-requests-number-duplicate) |
| POST | `/sni/api/v1/fund-requests/{number}/expenses` | FUND_REQUEST:CREATE | [FundController](routes/FundController.md#r-post-sni-api-v1-fund-requests-number-expenses) |
| DELETE | `/sni/api/v1/fund-requests/{number}/expenses/{line}` | FUND_REQUEST:CREATE | [FundController](routes/FundController.md#r-delete-sni-api-v1-fund-requests-number-expenses-line) |
| PATCH | `/sni/api/v1/fund-requests/{number}/expenses/{line}/reject` | FUND_JUSTIFICATION:VALIDATE | [FundController](routes/FundController.md#r-patch-sni-api-v1-fund-requests-number-expenses-line-reject) |
| PATCH | `/sni/api/v1/fund-requests/{number}/expenses/{line}/validate` | FUND_JUSTIFICATION:VALIDATE | [FundController](routes/FundController.md#r-patch-sni-api-v1-fund-requests-number-expenses-line-validate) |
| GET | `/sni/api/v1/fund-requests/{number}/insight` | FUND_REQUEST:CREATE | [FundController](routes/FundController.md#r-get-sni-api-v1-fund-requests-number-insight) |
| PATCH | `/sni/api/v1/fund-requests/{number}/reject` | FUND_REQUEST:CREATE | [FundController](routes/FundController.md#r-patch-sni-api-v1-fund-requests-number-reject) |
| POST | `/sni/api/v1/fund-requests/{number}/return` | FUND_REQUEST:DISBURSE | [FundController](routes/FundController.md#r-post-sni-api-v1-fund-requests-number-return) |
| PATCH | `/sni/api/v1/fund-requests/{number}/send-back` | FUND_REQUEST:CREATE | [FundController](routes/FundController.md#r-patch-sni-api-v1-fund-requests-number-send-back) |
| PATCH | `/sni/api/v1/fund-requests/{number}/submit` | FUND_REQUEST:CREATE | [FundController](routes/FundController.md#r-patch-sni-api-v1-fund-requests-number-submit) |
| GET | `/sni/api/v1/fund-requests/{number}/supplier-suggestions` | FUND_REQUEST:CREATE | [FundController](routes/FundController.md#r-get-sni-api-v1-fund-requests-number-supplier-suggestions) |
| PATCH | `/sni/api/v1/fund-requests/{number}/transfer` | FUND_REQUEST:DISBURSE | [FundController](routes/FundController.md#r-patch-sni-api-v1-fund-requests-number-transfer) |
| GET | `/sni/api/v1/fund-needs/{number}/pdf` | FUND_REQUEST:CREATE | [VoucherController](routes/VoucherController.md#r-get-sni-api-v1-fund-needs-number-pdf) |
| GET | `/sni/api/v1/fund-requests/{number}/disbursements/{movementNumber}/pdf` | FUND_REQUEST:CREATE | [VoucherController](routes/VoucherController.md#r-get-sni-api-v1-fund-requests-number-disbursements-movementnumber-pdf) |
| GET | `/sni/api/v1/fund-requests/{number}/justification/pdf` | FUND_REQUEST:CREATE | [VoucherController](routes/VoucherController.md#r-get-sni-api-v1-fund-requests-number-justification-pdf) |
| GET | `/sni/api/v1/fund-requests/{number}/pdf` | FUND_REQUEST:CREATE | [VoucherController](routes/VoucherController.md#r-get-sni-api-v1-fund-requests-number-pdf) |
| GET | `/verify/voucher/{number}` | public | [VoucherController](routes/VoucherController.md#r-get-verify-voucher-number) |
