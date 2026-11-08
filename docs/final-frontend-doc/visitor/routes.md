# visitor : routes

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

7 routes. Le fonctionnement et les parcours sont dans [README.md](README.md).

| Verbe | Chemin | Accès | Détail |
| --- | --- | --- | --- |
| GET | `/sni/api/v1/visitors/log` | VISITOR:READ | [VisitorManagementController](routes/VisitorManagementController.md#r-get-sni-api-v1-visitors-log) |
| GET | `/sni/api/v1/visitors/passes` | VISITOR:READ | [VisitorManagementController](routes/VisitorManagementController.md#r-get-sni-api-v1-visitors-passes) |
| POST | `/sni/api/v1/visitors/passes` | VISITOR:WRITE | [VisitorManagementController](routes/VisitorManagementController.md#r-post-sni-api-v1-visitors-passes) |
| GET | `/sni/api/v1/visitors/passes/today` | VISITOR:READ | [VisitorManagementController](routes/VisitorManagementController.md#r-get-sni-api-v1-visitors-passes-today) |
| GET | `/sni/api/v1/visitors/passes/{passNumber}/badge` | VISITOR:READ | [VisitorManagementController](routes/VisitorManagementController.md#r-get-sni-api-v1-visitors-passes-passnumber-badge) |
| POST | `/sni/api/v1/visitors/{passNumber}/check-in` | VISITOR:CHECKIN | [VisitorManagementController](routes/VisitorManagementController.md#r-post-sni-api-v1-visitors-passnumber-check-in) |
| POST | `/sni/api/v1/visitors/{passNumber}/check-out` | VISITOR:CHECKIN | [VisitorManagementController](routes/VisitorManagementController.md#r-post-sni-api-v1-visitors-passnumber-check-out) |
