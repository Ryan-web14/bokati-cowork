# event : routes

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

8 routes. Le fonctionnement et les parcours sont dans [README.md](README.md).

| Verbe | Chemin | Accès | Détail |
| --- | --- | --- | --- |
| GET | `/sni/api/v1/event-registrations` | CRM:READ | [EventRegistrationController](routes/EventRegistrationController.md#r-get-sni-api-v1-event-registrations) |
| GET | `/sni/api/v1/event-registrations/{id}` | CRM:READ | [EventRegistrationController](routes/EventRegistrationController.md#r-get-sni-api-v1-event-registrations-id) |
| PATCH | `/sni/api/v1/event-registrations/{id}/reject` | CRM:CONVERT | [EventRegistrationController](routes/EventRegistrationController.md#r-patch-sni-api-v1-event-registrations-id-reject) |
| PATCH | `/sni/api/v1/event-registrations/{id}/validate` | CRM:CONVERT | [EventRegistrationController](routes/EventRegistrationController.md#r-patch-sni-api-v1-event-registrations-id-validate) |
| GET | `/sni/api/v1/public/events/{eventCode}` | public | [PublicEventController](routes/PublicEventController.md#r-get-sni-api-v1-public-events-eventcode) |
| POST | `/sni/api/v1/public/events/{eventCode}/registrations` | public | [PublicEventController](routes/PublicEventController.md#r-post-sni-api-v1-public-events-eventcode-registrations) |
| GET | `/sni/api/v1/public/opening` | public | [PublicOpeningController](routes/PublicOpeningController.md#r-get-sni-api-v1-public-opening) |
| POST | `/sni/api/v1/public/opening/registrations` | public | [PublicOpeningController](routes/PublicOpeningController.md#r-post-sni-api-v1-public-opening-registrations) |
