# FiscalIntegrityController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [billing](../README.md) · 2 route(s).

## Routes

<a id="r-get-sni-api-v1-admin-fiscal-integrity-check"></a>
### GET /sni/api/v1/admin/fiscal-integrity/check

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Réponse : `FiscalIntegrityCheckService.FiscalIntegrityReport`
- Modèles : [FiscalIntegrityCheckService.FiscalIntegrityReport](#m-FiscalIntegrityCheckService-FiscalIntegrityReport)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/FiscalIntegrityController.java`, méthode `check`

<a id="r-get-sni-api-v1-admin-fiscal-integrity-last-report"></a>
### GET /sni/api/v1/admin/fiscal-integrity/last-report

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN')`
- Réponse : `FiscalIntegrityReportLog`
- Modèles : [FiscalIntegrityReportLog](#m-FiscalIntegrityReportLog)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/FiscalIntegrityController.java`, méthode `lastReport`

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-FiscalIntegrityCheckService-FiscalIntegrityReport"></a>
#### FiscalIntegrityCheckService.FiscalIntegrityReport

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `valid` | booléen |  |  |
| `checkedInvoices` | entier |  |  |
| `brokenChains` | entier |  |  |
| `missingSignatures` | entier |  |  |
| `numberingGaps` | entier |  |  |
| `checkedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-FiscalIntegrityReportLog"></a>
#### FiscalIntegrityReportLog

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `valid` | booléen |  |  |
| `checkedInvoices` | entier |  |  |
| `brokenChains` | entier |  |  |
| `missingSignatures` | entier |  |  |
| `numberingGaps` | entier |  |  |
| `checkedAt` | date-heure ISO 8601 UTC |  |  |
| `triggeredBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
