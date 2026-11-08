# company : routes

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

58 routes. Le fonctionnement et les parcours sont dans [README.md](README.md).

| Verbe | Chemin | Accès | Détail |
| --- | --- | --- | --- |
| GET | `/sni/api/v1/companies/{customerId}/statement` | ADMIN:ACCESS | [AccountStatementController](routes/AccountStatementController.md#r-get-sni-api-v1-companies-customerid-statement) |
| GET | `/sni/api/v1/companies/{customerId}/statement.csv` | ADMIN:ACCESS | [AccountStatementController](routes/AccountStatementController.md#r-get-sni-api-v1-companies-customerid-statement-csv) |
| GET | `/sni/api/v1/companies/{customerId}/statement.pdf` | ADMIN:ACCESS | [AccountStatementController](routes/AccountStatementController.md#r-get-sni-api-v1-companies-customerid-statement-pdf) |
| GET | `/sni/api/v1/members/{memberId}/statement` | CLIENT:READ | [AccountStatementController](routes/AccountStatementController.md#r-get-sni-api-v1-members-memberid-statement) |
| GET | `/sni/api/v1/members/{memberId}/statement.csv` | CLIENT:READ | [AccountStatementController](routes/AccountStatementController.md#r-get-sni-api-v1-members-memberid-statement-csv) |
| GET | `/sni/api/v1/members/{memberId}/statement.pdf` | CLIENT:READ | [AccountStatementController](routes/AccountStatementController.md#r-get-sni-api-v1-members-memberid-statement-pdf) |
| GET | `/sni/api/v1/businesses` | SYSTEM:SETTINGS | [BusinessController](routes/BusinessController.md#r-get-sni-api-v1-businesses) |
| POST | `/sni/api/v1/businesses` | SYSTEM:SETTINGS | [BusinessController](routes/BusinessController.md#r-post-sni-api-v1-businesses) |
| GET | `/sni/api/v1/businesses/by-name` | SYSTEM:SETTINGS | [BusinessController](routes/BusinessController.md#r-get-sni-api-v1-businesses-by-name) |
| GET | `/sni/api/v1/businesses/paged` | SYSTEM:SETTINGS | [BusinessController](routes/BusinessController.md#r-get-sni-api-v1-businesses-paged) |
| POST | `/sni/api/v1/businesses/search` | SYSTEM:SETTINGS | [BusinessController](routes/BusinessController.md#r-post-sni-api-v1-businesses-search) |
| GET | `/sni/api/v1/businesses/search/basic` | SYSTEM:SETTINGS | [BusinessController](routes/BusinessController.md#r-get-sni-api-v1-businesses-search-basic) |
| DELETE | `/sni/api/v1/businesses/{code}` | SYSTEM:SETTINGS | [BusinessController](routes/BusinessController.md#r-delete-sni-api-v1-businesses-code) |
| GET | `/sni/api/v1/businesses/{code}` | SYSTEM:SETTINGS | [BusinessController](routes/BusinessController.md#r-get-sni-api-v1-businesses-code) |
| PUT | `/sni/api/v1/businesses/{code}` | SYSTEM:SETTINGS | [BusinessController](routes/BusinessController.md#r-put-sni-api-v1-businesses-code) |
| PATCH | `/sni/api/v1/businesses/{code}/address` | SYSTEM:SETTINGS | [BusinessController](routes/BusinessController.md#r-patch-sni-api-v1-businesses-code-address) |
| PATCH | `/sni/api/v1/businesses/{code}/operator` | SYSTEM:SETTINGS | [BusinessController](routes/BusinessController.md#r-patch-sni-api-v1-businesses-code-operator) |
| PATCH | `/sni/api/v1/businesses/{code}/status` | SYSTEM:SETTINGS | [BusinessController](routes/BusinessController.md#r-patch-sni-api-v1-businesses-code-status) |
| GET | `/sni/api/v1/companies` | ADMIN:ACCESS | [CompanyAccountController](routes/CompanyAccountController.md#r-get-sni-api-v1-companies) |
| POST | `/sni/api/v1/companies` | ADMIN:ACCESS | [CompanyAccountController](routes/CompanyAccountController.md#r-post-sni-api-v1-companies) |
| GET | `/sni/api/v1/companies/{customerId}` | ADMIN:ACCESS | [CompanyAccountController](routes/CompanyAccountController.md#r-get-sni-api-v1-companies-customerid) |
| PATCH | `/sni/api/v1/companies/{customerId}/billing` | ADMIN:ACCESS | [CompanyAccountController](routes/CompanyAccountController.md#r-patch-sni-api-v1-companies-customerid-billing) |
| POST | `/sni/api/v1/companies/{customerId}/block` | ADMIN:ACCESS | [CompanyAccountController](routes/CompanyAccountController.md#r-post-sni-api-v1-companies-customerid-block) |
| PATCH | `/sni/api/v1/companies/{customerId}/contact` | ADMIN:ACCESS | [CompanyAccountController](routes/CompanyAccountController.md#r-patch-sni-api-v1-companies-customerid-contact) |
| PATCH | `/sni/api/v1/companies/{customerId}/legal` | ADMIN:ACCESS | [CompanyAccountController](routes/CompanyAccountController.md#r-patch-sni-api-v1-companies-customerid-legal) |
| POST | `/sni/api/v1/companies/{customerId}/legal-profile` | ADMIN:ACCESS | [CompanyAccountController](routes/CompanyAccountController.md#r-post-sni-api-v1-companies-customerid-legal-profile) |
| POST | `/sni/api/v1/companies/{customerId}/unblock` | ADMIN:ACCESS | [CompanyAccountController](routes/CompanyAccountController.md#r-post-sni-api-v1-companies-customerid-unblock) |
| GET | `/sni/api/v1/client/companies/{customerId}/contracts` | membre | [CompanyContractsController](routes/CompanyContractsController.md#r-get-sni-api-v1-client-companies-customerid-contracts) |
| GET | `/sni/api/v1/client/companies/{customerId}/contracts/{contractCode}/pdf` | membre | [CompanyContractsController](routes/CompanyContractsController.md#r-get-sni-api-v1-client-companies-customerid-contracts-contractcode-pdf) |
| GET | `/sni/api/v1/companies/{customerId}/contracts` | ADMIN:ACCESS | [CompanyContractsController](routes/CompanyContractsController.md#r-get-sni-api-v1-companies-customerid-contracts) |
| GET | `/sni/api/v1/companies/{customerId}/contracts/{contractCode}/pdf` | ADMIN:ACCESS | [CompanyContractsController](routes/CompanyContractsController.md#r-get-sni-api-v1-companies-customerid-contracts-contractcode-pdf) |
| POST | `/sni/api/v1/billing/documents/company-links` | BILLING:CREATE | [CompanyInvoiceLinkController](routes/CompanyInvoiceLinkController.md#r-post-sni-api-v1-billing-documents-company-links) |
| POST | `/sni/api/v1/billing/documents/company-links/preview` | BILLING:CREATE | [CompanyInvoiceLinkController](routes/CompanyInvoiceLinkController.md#r-post-sni-api-v1-billing-documents-company-links-preview) |
| DELETE | `/sni/api/v1/billing/documents/{documentNumber}/company-link` | BILLING:CANCEL | [CompanyInvoiceLinkController](routes/CompanyInvoiceLinkController.md#r-delete-sni-api-v1-billing-documents-documentnumber-company-link) |
| PATCH | `/sni/api/v1/companies/{customerId}/invoice-link-mode` | ADMIN:ACCESS | [CompanyMembershipController](routes/CompanyMembershipController.md#r-patch-sni-api-v1-companies-customerid-invoice-link-mode) |
| GET | `/sni/api/v1/customers/{customerId}/members` | CLIENT:READ | [CompanyMembershipController](routes/CompanyMembershipController.md#r-get-sni-api-v1-customers-customerid-members) |
| POST | `/sni/api/v1/customers/{customerId}/members` | CLIENT:CREATE | [CompanyMembershipController](routes/CompanyMembershipController.md#r-post-sni-api-v1-customers-customerid-members) |
| PATCH | `/sni/api/v1/customers/{customerId}/members/{linkId}` | CLIENT:UPDATE | [CompanyMembershipController](routes/CompanyMembershipController.md#r-patch-sni-api-v1-customers-customerid-members-linkid) |
| POST | `/sni/api/v1/customers/{customerId}/members/{linkId}/end` | CLIENT:CREATE | [CompanyMembershipController](routes/CompanyMembershipController.md#r-post-sni-api-v1-customers-customerid-members-linkid-end) |
| GET | `/sni/api/v1/members/{memberId}/companies` | CLIENT:READ | [CompanyMembershipController](routes/CompanyMembershipController.md#r-get-sni-api-v1-members-memberid-companies) |
| GET | `/sni/api/v1/companies/{customerId}/passes` | ADMIN:ACCESS | [CompanyPassSharingController](routes/CompanyPassSharingController.md#r-get-sni-api-v1-companies-customerid-passes) |
| GET | `/sni/api/v1/companies/{customerId}/passes/{passNumber}/beneficiaries` | ADMIN:ACCESS | [CompanyPassSharingController](routes/CompanyPassSharingController.md#r-get-sni-api-v1-companies-customerid-passes-passnumber-beneficiaries) |
| POST | `/sni/api/v1/companies/{customerId}/passes/{passNumber}/beneficiaries` | ADMIN:ACCESS | [CompanyPassSharingController](routes/CompanyPassSharingController.md#r-post-sni-api-v1-companies-customerid-passes-passnumber-beneficiaries) |
| POST | `/sni/api/v1/companies/{customerId}/passes/{passNumber}/beneficiaries/{accessId}/revoke` | ADMIN:ACCESS | [CompanyPassSharingController](routes/CompanyPassSharingController.md#r-post-sni-api-v1-companies-customerid-passes-passnumber-beneficiaries-accessid-revoke) |
| GET | `/sni/api/v1/companies/{customerId}/subscriptions` | ADMIN:ACCESS | [CompanySharingController](routes/CompanySharingController.md#r-get-sni-api-v1-companies-customerid-subscriptions) |
| PATCH | `/sni/api/v1/companies/{customerId}/subscriptions/{subscriptionNumber}/max-seats` | ADMIN:ACCESS | [CompanySharingController](routes/CompanySharingController.md#r-patch-sni-api-v1-companies-customerid-subscriptions-subscriptionnumber-max-seats) |
| POST | `/sni/api/v1/companies/{customerId}/subscriptions/{subscriptionNumber}/seats` | ADMIN:ACCESS | [CompanySharingController](routes/CompanySharingController.md#r-post-sni-api-v1-companies-customerid-subscriptions-subscriptionnumber-seats) |
| PATCH | `/sni/api/v1/companies/{customerId}/subscriptions/{subscriptionNumber}/seats/{seatId}` | ADMIN:ACCESS | [CompanySharingController](routes/CompanySharingController.md#r-patch-sni-api-v1-companies-customerid-subscriptions-subscriptionnumber-seats-seatid) |
| POST | `/sni/api/v1/companies/{customerId}/subscriptions/{subscriptionNumber}/seats/{seatId}/release` | ADMIN:ACCESS | [CompanySharingController](routes/CompanySharingController.md#r-post-sni-api-v1-companies-customerid-subscriptions-subscriptionnumber-seats-seatid-release) |
| GET | `/sni/api/v1/companies/{customerId}/subscriptions/{subscriptionNumber}/sharing` | ADMIN:ACCESS | [CompanySharingController](routes/CompanySharingController.md#r-get-sni-api-v1-companies-customerid-subscriptions-subscriptionnumber-sharing) |
| PUT | `/sni/api/v1/companies/{customerId}/subscriptions/{subscriptionNumber}/sharing/{entitlementCode}` | ADMIN:ACCESS | [CompanySharingController](routes/CompanySharingController.md#r-put-sni-api-v1-companies-customerid-subscriptions-subscriptionnumber-sharing-entitlementcode) |
| GET | `/sni/api/v1/companies/{customerId}/spending-authorizations` | ADMIN:ACCESS | [CompanyWalletController](routes/CompanyWalletController.md#r-get-sni-api-v1-companies-customerid-spending-authorizations) |
| POST | `/sni/api/v1/companies/{customerId}/spending-authorizations` | ADMIN:ACCESS | [CompanyWalletController](routes/CompanyWalletController.md#r-post-sni-api-v1-companies-customerid-spending-authorizations) |
| PATCH | `/sni/api/v1/companies/{customerId}/spending-authorizations/{authorizationId}` | ADMIN:ACCESS | [CompanyWalletController](routes/CompanyWalletController.md#r-patch-sni-api-v1-companies-customerid-spending-authorizations-authorizationid) |
| POST | `/sni/api/v1/companies/{customerId}/spending-authorizations/{authorizationId}/pin/reset` | ADMIN:ACCESS | [CompanyWalletController](routes/CompanyWalletController.md#r-post-sni-api-v1-companies-customerid-spending-authorizations-authorizationid-pin-reset) |
| GET | `/sni/api/v1/companies/{customerId}/wallet` | ADMIN:ACCESS | [CompanyWalletController](routes/CompanyWalletController.md#r-get-sni-api-v1-companies-customerid-wallet) |
| GET | `/sni/api/v1/companies/{customerId}/wallet/spends` | ADMIN:ACCESS | [CompanyWalletController](routes/CompanyWalletController.md#r-get-sni-api-v1-companies-customerid-wallet-spends) |
| POST | `/sni/api/v1/companies/{customerId}/wallet/top-ups` | ADMIN:ACCESS | [CompanyWalletController](routes/CompanyWalletController.md#r-post-sni-api-v1-companies-customerid-wallet-top-ups) |
