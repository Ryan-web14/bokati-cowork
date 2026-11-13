# socle : routes

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

48 routes. Le fonctionnement et les parcours sont dans [README.md](README.md).

| Verbe | Chemin | Accès | Détail |
| --- | --- | --- | --- |
| POST | `/sni/api/v1/addresses` | ADMIN:ACCESS | [AddressController](routes/AddressController.md#r-post-sni-api-v1-addresses) |
| DELETE | `/sni/api/v1/addresses/{id}` | ADMIN:ACCESS | [AddressController](routes/AddressController.md#r-delete-sni-api-v1-addresses-id) |
| GET | `/sni/api/v1/addresses/{id}` | ADMIN:ACCESS | [AddressController](routes/AddressController.md#r-get-sni-api-v1-addresses-id) |
| PUT | `/sni/api/v1/addresses/{id}` | ADMIN:ACCESS | [AddressController](routes/AddressController.md#r-put-sni-api-v1-addresses-id) |
| GET | `/sni/api/v1/admin/audit-logs` | SYSTEM:AUDIT | [AdminAuditController](routes/AdminAuditController.md#r-get-sni-api-v1-admin-audit-logs) |
| GET | `/sni/api/v1/admin/idempotency-records` | SYSTEM:SETTINGS | [AdminIdempotencyController](routes/AdminIdempotencyController.md#r-get-sni-api-v1-admin-idempotency-records) |
| GET | `/sni/api/v1/admin/outbox-events` | SYSTEM:SETTINGS | [AdminOutboxController](routes/AdminOutboxController.md#r-get-sni-api-v1-admin-outbox-events) |
| POST | `/sni/api/v1/admin/outbox-events/process` | SYSTEM:SETTINGS | [AdminOutboxController](routes/AdminOutboxController.md#r-post-sni-api-v1-admin-outbox-events-process) |
| POST | `/sni/api/v1/admin/outbox-events/requeue-failed` | SYSTEM:SETTINGS | [AdminOutboxController](routes/AdminOutboxController.md#r-post-sni-api-v1-admin-outbox-events-requeue-failed) |
| POST | `/sni/api/v1/admin/outbox-events/{id}/requeue` | SYSTEM:SETTINGS | [AdminOutboxController](routes/AdminOutboxController.md#r-post-sni-api-v1-admin-outbox-events-id-requeue) |
| GET | `/sni/api/v1/admin/settings-audit-logs` | SYSTEM:AUDIT | [AdminSettingsAuditController](routes/AdminSettingsAuditController.md#r-get-sni-api-v1-admin-settings-audit-logs) |
| GET | `/sni/api/v1/countries` | public | [CountryController](routes/CountryController.md#r-get-sni-api-v1-countries) |
| POST | `/sni/api/v1/countries` | SYSTEM:SETTINGS | [CountryController](routes/CountryController.md#r-post-sni-api-v1-countries) |
| GET | `/sni/api/v1/countries/by-phone-code` | SYSTEM:SETTINGS | [CountryController](routes/CountryController.md#r-get-sni-api-v1-countries-by-phone-code) |
| GET | `/sni/api/v1/countries/phone-codes` | SYSTEM:SETTINGS | [CountryController](routes/CountryController.md#r-get-sni-api-v1-countries-phone-codes) |
| DELETE | `/sni/api/v1/countries/{countryCode}` | SYSTEM:SETTINGS | [CountryController](routes/CountryController.md#r-delete-sni-api-v1-countries-countrycode) |
| GET | `/sni/api/v1/countries/{countryCode}` | SYSTEM:SETTINGS | [CountryController](routes/CountryController.md#r-get-sni-api-v1-countries-countrycode) |
| PUT | `/sni/api/v1/countries/{countryCode}` | SYSTEM:SETTINGS | [CountryController](routes/CountryController.md#r-put-sni-api-v1-countries-countrycode) |
| GET | `/sni/api/v1/currencies` | SYSTEM:SETTINGS | [CurrencyController](routes/CurrencyController.md#r-get-sni-api-v1-currencies) |
| POST | `/sni/api/v1/currencies` | SYSTEM:SETTINGS | [CurrencyController](routes/CurrencyController.md#r-post-sni-api-v1-currencies) |
| GET | `/sni/api/v1/currencies/all` | SYSTEM:SETTINGS | [CurrencyController](routes/CurrencyController.md#r-get-sni-api-v1-currencies-all) |
| DELETE | `/sni/api/v1/currencies/{currencyCode}` | SYSTEM:SETTINGS | [CurrencyController](routes/CurrencyController.md#r-delete-sni-api-v1-currencies-currencycode) |
| GET | `/sni/api/v1/currencies/{currencyCode}` | SYSTEM:SETTINGS | [CurrencyController](routes/CurrencyController.md#r-get-sni-api-v1-currencies-currencycode) |
| PUT | `/sni/api/v1/currencies/{currencyCode}` | SYSTEM:SETTINGS | [CurrencyController](routes/CurrencyController.md#r-put-sni-api-v1-currencies-currencycode) |
| GET | `/sni/api/v1/currency` | SYSTEM:SETTINGS | [CurrencyController](routes/CurrencyController.md#r-get-sni-api-v1-currency) |
| POST | `/sni/api/v1/currency` | SYSTEM:SETTINGS | [CurrencyController](routes/CurrencyController.md#r-post-sni-api-v1-currency) |
| GET | `/sni/api/v1/currency/all` | SYSTEM:SETTINGS | [CurrencyController](routes/CurrencyController.md#r-get-sni-api-v1-currency-all) |
| DELETE | `/sni/api/v1/currency/{currencyCode}` | SYSTEM:SETTINGS | [CurrencyController](routes/CurrencyController.md#r-delete-sni-api-v1-currency-currencycode) |
| GET | `/sni/api/v1/currency/{currencyCode}` | SYSTEM:SETTINGS | [CurrencyController](routes/CurrencyController.md#r-get-sni-api-v1-currency-currencycode) |
| PUT | `/sni/api/v1/currency/{currencyCode}` | SYSTEM:SETTINGS | [CurrencyController](routes/CurrencyController.md#r-put-sni-api-v1-currency-currencycode) |
| DELETE | `/sni/api/v1/admin/maintenance/purge-data` | SYSTEM:SETTINGS | [DataPurgeController](routes/DataPurgeController.md#r-delete-sni-api-v1-admin-maintenance-purge-data) |
| GET | `/sni/api/v1/admin/email-deliveries` | SYSTEM:SETTINGS | [EmailDeliveryController](routes/EmailDeliveryController.md#r-get-sni-api-v1-admin-email-deliveries) |
| GET | `/sni/api/v1/admin/email-deliveries/{emailNumber}` | SYSTEM:SETTINGS | [EmailDeliveryController](routes/EmailDeliveryController.md#r-get-sni-api-v1-admin-email-deliveries-emailnumber) |
| PATCH | `/sni/api/v1/admin/email-deliveries/{emailNumber}/retry` | SYSTEM:SETTINGS | [EmailDeliveryController](routes/EmailDeliveryController.md#r-patch-sni-api-v1-admin-email-deliveries-emailnumber-retry) |
| POST | `/sni/api/v1/admin/maintenance/members/purge-batch` | SYSTEM:SETTINGS | [MemberPurgeController](routes/MemberPurgeController.md#r-post-sni-api-v1-admin-maintenance-members-purge-batch) |
| GET | `/sni/api/v1/admin/maintenance/members/purge-candidates` | ADMIN:ACCESS | [MemberPurgeController](routes/MemberPurgeController.md#r-get-sni-api-v1-admin-maintenance-members-purge-candidates) |
| DELETE | `/sni/api/v1/admin/maintenance/members/{memberCode}` | SYSTEM:SETTINGS | [MemberPurgeController](routes/MemberPurgeController.md#r-delete-sni-api-v1-admin-maintenance-members-membercode) |
| GET | `/sni/api/v1/admin/maintenance/members/{memberCode}/purge-preview` | ADMIN:ACCESS | [MemberPurgeController](routes/MemberPurgeController.md#r-get-sni-api-v1-admin-maintenance-members-membercode-purge-preview) |
| GET | `/sni/api/v1/admin/whatsapp/messages` | SYSTEM:SETTINGS | [WhatsAppController](routes/WhatsAppController.md#r-get-sni-api-v1-admin-whatsapp-messages) |
| POST | `/sni/api/v1/admin/whatsapp/messages/refresh` | SYSTEM:SETTINGS | [WhatsAppController](routes/WhatsAppController.md#r-post-sni-api-v1-admin-whatsapp-messages-refresh) |
| GET | `/sni/api/v1/admin/whatsapp/subscription` | SYSTEM:SETTINGS | [WhatsAppController](routes/WhatsAppController.md#r-get-sni-api-v1-admin-whatsapp-subscription) |
| POST | `/sni/api/v1/admin/whatsapp/subscription` | SYSTEM:SETTINGS | [WhatsAppController](routes/WhatsAppController.md#r-post-sni-api-v1-admin-whatsapp-subscription) |
| GET | `/sni/api/v1/admin/whatsapp/templates` | SYSTEM:SETTINGS | [WhatsAppController](routes/WhatsAppController.md#r-get-sni-api-v1-admin-whatsapp-templates) |
| POST | `/sni/api/v1/admin/whatsapp/templates` | SYSTEM:SETTINGS | [WhatsAppController](routes/WhatsAppController.md#r-post-sni-api-v1-admin-whatsapp-templates) |
| POST | `/sni/api/v1/admin/whatsapp/templates/defaults` | SYSTEM:SETTINGS | [WhatsAppController](routes/WhatsAppController.md#r-post-sni-api-v1-admin-whatsapp-templates-defaults) |
| POST | `/sni/api/v1/admin/whatsapp/templates/sync` | SYSTEM:SETTINGS | [WhatsAppController](routes/WhatsAppController.md#r-post-sni-api-v1-admin-whatsapp-templates-sync) |
| POST | `/sni/api/v1/admin/whatsapp/test` | SYSTEM:SETTINGS | [WhatsAppController](routes/WhatsAppController.md#r-post-sni-api-v1-admin-whatsapp-test) |
| POST | `/sni/api/v1/public/webhooks/infobip/whatsapp` | public | [WhatsAppWebhookController](routes/WhatsAppWebhookController.md#r-post-sni-api-v1-public-webhooks-infobip-whatsapp) |
