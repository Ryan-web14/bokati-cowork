# client : routes

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

32 routes. Le fonctionnement et les parcours sont dans [README.md](README.md).

| Verbe | Chemin | Accès | Détail |
| --- | --- | --- | --- |
| GET | `/sni/api/v1/customers` | CLIENT:READ | [CustomerController](routes/CustomerController.md#r-get-sni-api-v1-customers) |
| POST | `/sni/api/v1/customers` | CLIENT:CREATE | [CustomerController](routes/CustomerController.md#r-post-sni-api-v1-customers) |
| GET | `/sni/api/v1/customers/by-email` | CLIENT:READ | [CustomerController](routes/CustomerController.md#r-get-sni-api-v1-customers-by-email) |
| GET | `/sni/api/v1/customers/search/basic` | CLIENT:READ | [CustomerController](routes/CustomerController.md#r-get-sni-api-v1-customers-search-basic) |
| GET | `/sni/api/v1/customers/summary` | CLIENT:READ | [CustomerController](routes/CustomerController.md#r-get-sni-api-v1-customers-summary) |
| GET | `/sni/api/v1/customers/summary/all` | CLIENT:READ | [CustomerController](routes/CustomerController.md#r-get-sni-api-v1-customers-summary-all) |
| DELETE | `/sni/api/v1/customers/{customerId}` | CLIENT:DELETE | [CustomerController](routes/CustomerController.md#r-delete-sni-api-v1-customers-customerid) |
| GET | `/sni/api/v1/customers/{customerId}` | CLIENT:READ | [CustomerController](routes/CustomerController.md#r-get-sni-api-v1-customers-customerid) |
| PATCH | `/sni/api/v1/customers/{customerId}` | CLIENT:UPDATE | [CustomerController](routes/CustomerController.md#r-patch-sni-api-v1-customers-customerid) |
| PATCH | `/sni/api/v1/customers/{customerId}/status` | CLIENT:UPDATE | [CustomerController](routes/CustomerController.md#r-patch-sni-api-v1-customers-customerid-status) |
| GET | `/sni/api/v1/members` | CLIENT:READ | [MemberController](routes/MemberController.md#r-get-sni-api-v1-members) |
| POST | `/sni/api/v1/members` | CLIENT:CREATE | [MemberController](routes/MemberController.md#r-post-sni-api-v1-members) |
| POST | `/sni/api/v1/members/admin/create` | CLIENT:CREATE | [MemberController](routes/MemberController.md#r-post-sni-api-v1-members-admin-create) |
| GET | `/sni/api/v1/members/by-email` | CLIENT:READ | [MemberController](routes/MemberController.md#r-get-sni-api-v1-members-by-email) |
| GET | `/sni/api/v1/members/me/profile` | CLIENT:READ | [MemberController](routes/MemberController.md#r-get-sni-api-v1-members-me-profile) |
| PUT | `/sni/api/v1/members/me/profile` | CLIENT:UPDATE | [MemberController](routes/MemberController.md#r-put-sni-api-v1-members-me-profile) |
| POST | `/sni/api/v1/members/portal/complete` | CLIENT:CREATE | [MemberController](routes/MemberController.md#r-post-sni-api-v1-members-portal-complete) |
| POST | `/sni/api/v1/members/search` | CLIENT:CREATE | [MemberController](routes/MemberController.md#r-post-sni-api-v1-members-search) |
| GET | `/sni/api/v1/members/search/basic` | CLIENT:READ | [MemberController](routes/MemberController.md#r-get-sni-api-v1-members-search-basic) |
| GET | `/sni/api/v1/members/search/by-name` | CLIENT:READ | [MemberController](routes/MemberController.md#r-get-sni-api-v1-members-search-by-name) |
| GET | `/sni/api/v1/members/summary` | CLIENT:READ | [MemberController](routes/MemberController.md#r-get-sni-api-v1-members-summary) |
| GET | `/sni/api/v1/members/summary/all` | CLIENT:READ | [MemberController](routes/MemberController.md#r-get-sni-api-v1-members-summary-all) |
| DELETE | `/sni/api/v1/members/{id}` | CLIENT:DELETE | [MemberController](routes/MemberController.md#r-delete-sni-api-v1-members-id) |
| GET | `/sni/api/v1/members/{id}` | CLIENT:READ | [MemberController](routes/MemberController.md#r-get-sni-api-v1-members-id) |
| PATCH | `/sni/api/v1/members/{id}` | CLIENT:UPDATE | [MemberController](routes/MemberController.md#r-patch-sni-api-v1-members-id) |
| PATCH | `/sni/api/v1/members/{id}/kyc-grace-period` | CLIENT:UPDATE | [MemberController](routes/MemberController.md#r-patch-sni-api-v1-members-id-kyc-grace-period) |
| PATCH | `/sni/api/v1/members/{id}/portal-access/disable` | CLIENT:UPDATE | [MemberController](routes/MemberController.md#r-patch-sni-api-v1-members-id-portal-access-disable) |
| PATCH | `/sni/api/v1/members/{id}/portal-access/enable` | CLIENT:UPDATE | [MemberController](routes/MemberController.md#r-patch-sni-api-v1-members-id-portal-access-enable) |
| GET | `/sni/api/v1/members/{id}/profile` | CLIENT:READ | [MemberController](routes/MemberController.md#r-get-sni-api-v1-members-id-profile) |
| PUT | `/sni/api/v1/members/{id}/profile` | CLIENT:UPDATE | [MemberController](routes/MemberController.md#r-put-sni-api-v1-members-id-profile) |
| PATCH | `/sni/api/v1/members/{id}/status` | CLIENT:UPDATE | [MemberController](routes/MemberController.md#r-patch-sni-api-v1-members-id-status) |
| PATCH | `/sni/api/v1/members/{id}/transfer-customer` | CLIENT:UPDATE | [MemberController](routes/MemberController.md#r-patch-sni-api-v1-members-id-transfer-customer) |
