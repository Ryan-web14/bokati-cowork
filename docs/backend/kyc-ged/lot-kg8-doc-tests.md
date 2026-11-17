# Lot KG8 : doc et tests

> Partie du [plan KYC et GED](README.md). Fait le 17/11/2026 sur `feature/kyc-ged-kg8`, après la fusion des lots KG0 à KG7 dans new-main (f118f860).

## Écarts

- Plan : K1, K10, K-n1 et G-n1, réglés au lot KG0 (règles d'URL KYC par méthode, auteur toujours le compte connecté, dossiers restreints respectés par les contrôleurs de dépôt, étiquettes, métadonnées, signatures, partages, journaux et files de revue), marqués Réglé ; plus aucun écart ouvert dans le tableau.
- Doc frontend `document/README.md` : écarts 2 (approbation automatique sans courriel ni portefeuille) et 3 (exigences du dossier ignorées par le recalcul), réglés au lot KG1, marqués Réglé ; plus aucun écart ouvert.
- `parcours-kyc.md` : copie ancienne de trois sections (courriels, export PDF, règles de contrôle croisé) laissée par une fusion, retirée au lot KG7.

## Tests

| Lot | Tests unitaires |
| --- | --- |
| KG0 | `KycGedRightsKg0Test` |
| KG1 | `KycLevelChangeKg1Test` |
| KG2 | `KycPieceLinkerKg2Test`, `KycDocumentSyncRunnerKg2Test` |
| KG3 | `KycOutboxEventProcessorKg3Test`, `KycAutomationWorkerKg3Test`, `DocumentAutoApproveKg3Test` |
| KG4 | `DocumentGedKg4Test` |
| KG5 | `DocumentKg5Test` |
| KG6 | `DedicatedErrorsKg6Test` |
| KG7 | `KycCenterKg7Test` |
| KG9 | `KycCrossCheckServiceTest`, `KycImageQualityServiceTest` |

Intégration (`KycGedMigrationsIT`, PostgreSQL réel, tag `integration`) : les migrations de production V361 à V364 passent sur une base vierge, les 11 motifs et les 10 règles préenregistrés sont là, les colonnes des lots KG1, KG7 et KG9 existent, un dossier relit son motif normalisé.

Démarrage complet : `BokatiCoworkApplicationTests` (application entière, configuration de production, sans base) passe dans la suite normale.

## Reste

KG9 seconde partie faite le 17/11/2026 (voir le lot KG9).
