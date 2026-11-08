# domiciliation : routes

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

40 routes. Le fonctionnement et les parcours sont dans [README.md](README.md).

| Verbe | Chemin | Accès | Détail |
| --- | --- | --- | --- |
| GET | `/sni/api/v1/client/companies/{customerId}/domiciliation` | membre | [ClientDomiciliationController](routes/ClientDomiciliationController.md#r-get-sni-api-v1-client-companies-customerid-domiciliation) |
| GET | `/sni/api/v1/client/domiciliation/contracts` | membre | [ClientDomiciliationController](routes/ClientDomiciliationController.md#r-get-sni-api-v1-client-domiciliation-contracts) |
| GET | `/sni/api/v1/client/domiciliation/contracts/{contractNumber}` | membre | [ClientDomiciliationController](routes/ClientDomiciliationController.md#r-get-sni-api-v1-client-domiciliation-contracts-contractnumber) |
| GET | `/sni/api/v1/client/domiciliation/contracts/{contractNumber}/certificate` | membre | [ClientDomiciliationController](routes/ClientDomiciliationController.md#r-get-sni-api-v1-client-domiciliation-contracts-contractnumber-certificate) |
| GET | `/sni/api/v1/client/domiciliation/contracts/{contractNumber}/document` | membre | [ClientDomiciliationController](routes/ClientDomiciliationController.md#r-get-sni-api-v1-client-domiciliation-contracts-contractnumber-document) |
| GET | `/sni/api/v1/client/domiciliation/contracts/{contractNumber}/mail` | membre | [ClientDomiciliationController](routes/ClientDomiciliationController.md#r-get-sni-api-v1-client-domiciliation-contracts-contractnumber-mail) |
| GET | `/sni/api/v1/client/domiciliation/contracts/{contractNumber}/mail/{itemNumber}/trail` | membre | [ClientDomiciliationController](routes/ClientDomiciliationController.md#r-get-sni-api-v1-client-domiciliation-contracts-contractnumber-mail-itemnumber-trail) |
| GET | `/sni/api/v1/services/catalogue` | ADMIN:ACCESS | [DomiciliationController](routes/DomiciliationController.md#r-get-sni-api-v1-services-catalogue) |
| PUT | `/sni/api/v1/services/catalogue` | ADMIN:ACCESS | [DomiciliationController](routes/DomiciliationController.md#r-put-sni-api-v1-services-catalogue) |
| GET | `/sni/api/v1/services/domiciliation/addresses` | ADMIN:ACCESS | [DomiciliationController](routes/DomiciliationController.md#r-get-sni-api-v1-services-domiciliation-addresses) |
| POST | `/sni/api/v1/services/domiciliation/addresses` | ADMIN:ACCESS | [DomiciliationController](routes/DomiciliationController.md#r-post-sni-api-v1-services-domiciliation-addresses) |
| PATCH | `/sni/api/v1/services/domiciliation/addresses/{code}` | ADMIN:ACCESS | [DomiciliationController](routes/DomiciliationController.md#r-patch-sni-api-v1-services-domiciliation-addresses-code) |
| GET | `/sni/api/v1/services/domiciliation/contracts` | ADMIN:ACCESS | [DomiciliationController](routes/DomiciliationController.md#r-get-sni-api-v1-services-domiciliation-contracts) |
| POST | `/sni/api/v1/services/domiciliation/contracts` | ADMIN:ACCESS | [DomiciliationController](routes/DomiciliationController.md#r-post-sni-api-v1-services-domiciliation-contracts) |
| GET | `/sni/api/v1/services/domiciliation/contracts/{contractNumber}` | ADMIN:ACCESS | [DomiciliationController](routes/DomiciliationController.md#r-get-sni-api-v1-services-domiciliation-contracts-contractnumber) |
| POST | `/sni/api/v1/services/domiciliation/contracts/{contractNumber}/certificate` | ADMIN:ACCESS | [DomiciliationController](routes/DomiciliationController.md#r-post-sni-api-v1-services-domiciliation-contracts-contractnumber-certificate) |
| POST | `/sni/api/v1/services/domiciliation/contracts/{contractNumber}/commitment` | ADMIN:ACCESS | [DomiciliationController](routes/DomiciliationController.md#r-post-sni-api-v1-services-domiciliation-contracts-contractnumber-commitment) |
| GET | `/sni/api/v1/services/domiciliation/contracts/{contractNumber}/mail` | ADMIN:ACCESS | [DomiciliationController](routes/DomiciliationController.md#r-get-sni-api-v1-services-domiciliation-contracts-contractnumber-mail) |
| POST | `/sni/api/v1/services/domiciliation/contracts/{contractNumber}/mail` | ADMIN:ACCESS | [DomiciliationController](routes/DomiciliationController.md#r-post-sni-api-v1-services-domiciliation-contracts-contractnumber-mail) |
| POST | `/sni/api/v1/services/domiciliation/contracts/{contractNumber}/prepare` | ADMIN:ACCESS | [DomiciliationController](routes/DomiciliationController.md#r-post-sni-api-v1-services-domiciliation-contracts-contractnumber-prepare) |
| POST | `/sni/api/v1/services/domiciliation/contracts/{contractNumber}/registration/confirm` | ADMIN:ACCESS | [DomiciliationController](routes/DomiciliationController.md#r-post-sni-api-v1-services-domiciliation-contracts-contractnumber-registration-confirm) |
| POST | `/sni/api/v1/services/domiciliation/contracts/{contractNumber}/registration/reject` | ADMIN:ACCESS | [DomiciliationController](routes/DomiciliationController.md#r-post-sni-api-v1-services-domiciliation-contracts-contractnumber-registration-reject) |
| POST | `/sni/api/v1/services/domiciliation/contracts/{contractNumber}/registration/submit` | ADMIN:ACCESS | [DomiciliationController](routes/DomiciliationController.md#r-post-sni-api-v1-services-domiciliation-contracts-contractnumber-registration-submit) |
| GET | `/sni/api/v1/services/domiciliation/contracts/{contractNumber}/registrations` | ADMIN:ACCESS | [DomiciliationController](routes/DomiciliationController.md#r-get-sni-api-v1-services-domiciliation-contracts-contractnumber-registrations) |
| POST | `/sni/api/v1/services/domiciliation/contracts/{contractNumber}/signed` | ADMIN:ACCESS | [DomiciliationController](routes/DomiciliationController.md#r-post-sni-api-v1-services-domiciliation-contracts-contractnumber-signed) |
| POST | `/sni/api/v1/services/domiciliation/contracts/{contractNumber}/terminate` | ADMIN:ACCESS | [DomiciliationController](routes/DomiciliationController.md#r-post-sni-api-v1-services-domiciliation-contracts-contractnumber-terminate) |
| GET | `/sni/api/v1/services/domiciliation/mail/pending` | ADMIN:ACCESS | [DomiciliationController](routes/DomiciliationController.md#r-get-sni-api-v1-services-domiciliation-mail-pending) |
| GET | `/sni/api/v1/services/domiciliation/mail/{itemNumber}` | ADMIN:ACCESS | [DomiciliationController](routes/DomiciliationController.md#r-get-sni-api-v1-services-domiciliation-mail-itemnumber) |
| POST | `/sni/api/v1/services/domiciliation/mail/{itemNumber}/collect` | ADMIN:ACCESS | [DomiciliationController](routes/DomiciliationController.md#r-post-sni-api-v1-services-domiciliation-mail-itemnumber-collect) |
| POST | `/sni/api/v1/services/domiciliation/mail/{itemNumber}/destroy` | ADMIN:ACCESS | [DomiciliationController](routes/DomiciliationController.md#r-post-sni-api-v1-services-domiciliation-mail-itemnumber-destroy) |
| POST | `/sni/api/v1/services/domiciliation/mail/{itemNumber}/forward` | ADMIN:ACCESS | [DomiciliationController](routes/DomiciliationController.md#r-post-sni-api-v1-services-domiciliation-mail-itemnumber-forward) |
| POST | `/sni/api/v1/services/domiciliation/mail/{itemNumber}/return` | ADMIN:ACCESS | [DomiciliationController](routes/DomiciliationController.md#r-post-sni-api-v1-services-domiciliation-mail-itemnumber-return) |
| POST | `/sni/api/v1/services/domiciliation/mail/{itemNumber}/scan` | ADMIN:ACCESS | [DomiciliationController](routes/DomiciliationController.md#r-post-sni-api-v1-services-domiciliation-mail-itemnumber-scan) |
| GET | `/sni/api/v1/services/domiciliation/mail/{itemNumber}/trail` | ADMIN:ACCESS | [DomiciliationController](routes/DomiciliationController.md#r-get-sni-api-v1-services-domiciliation-mail-itemnumber-trail) |
| GET | `/sni/api/v1/services/domiciliation/registrations/in-progress` | ADMIN:ACCESS | [DomiciliationController](routes/DomiciliationController.md#r-get-sni-api-v1-services-domiciliation-registrations-in-progress) |
| GET | `/sni/api/v1/services/domiciliation/registry` | ADMIN:ACCESS | [DomiciliationController](routes/DomiciliationController.md#r-get-sni-api-v1-services-domiciliation-registry) |
| POST | `/sni/api/v1/services/subscribed/{serviceNumber}/suspend` | ADMIN:ACCESS | [DomiciliationController](routes/DomiciliationController.md#r-post-sni-api-v1-services-subscribed-servicenumber-suspend) |
| POST | `/sni/api/v1/services/subscribed/{serviceNumber}/terminate` | ADMIN:ACCESS | [DomiciliationController](routes/DomiciliationController.md#r-post-sni-api-v1-services-subscribed-servicenumber-terminate) |
| GET | `/sni/api/v1/services/subscriptions/{subscriptionNumber}` | ADMIN:ACCESS | [DomiciliationController](routes/DomiciliationController.md#r-get-sni-api-v1-services-subscriptions-subscriptionnumber) |
| POST | `/sni/api/v1/services/subscriptions/{subscriptionNumber}` | ADMIN:ACCESS | [DomiciliationController](routes/DomiciliationController.md#r-post-sni-api-v1-services-subscriptions-subscriptionnumber) |
