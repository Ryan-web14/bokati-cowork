# Lot KG6 : erreurs et messages

> Partie du [plan KYC et GED](README.md). Fait le 17/11/2026 sur `feature/kyc-ged`.

## Réponses dédiées (`GlobalExceptionHandler`)

| Exception | Avant | Après |
| --- | --- | --- |
| `MaxUploadSizeExceededException` | `500` | `413 FILE_SIZE_EXCEEDED`, limite en Mo dans le message |
| `MethodArgumentTypeMismatchException` (paramètre d'URL ou de chemin) | `500` | `400 TYPE_MISMATCH`, paramètre nommé, valeurs admises d'une énumération |
| `InvalidFormatException` dans le corps (Jackson 3) | `400` « malformed » | `400 VALIDATION_FAILED`, champ (chemin complet) et valeurs admises |
| `MissingServletRequestParameterException` | `400 BAD_REQUEST` en anglais | `400 MISSING_PARAMETER` en français |
| `UnexpectedRollbackException`, verrou optimiste ou pessimiste, `TransactionSystemException` hors contrainte | `500` | `409 CONFLICT`, « Rechargez puis réessayez » |

## Messages en français

- Messages d'exception du module `document` (GED, KYC, rétention, partages, circuits) et du portail KYC traduits (environ 80).
- Le dépôt ZIP reconnaît le dossier existant au message français (et à l'ancien).

## Droits

- Politiques de rétention : `SYSTEM:SETTINGS` seul (le `@PreAuthorize` `DOCUMENT:REVIEW` en plus a été retiré).

## Écarts réglés

G13, G-n7.

## Tests

`DedicatedErrorsKg6Test` (413, paramètre d'énumération invalide, énumération invalide dans le corps, transaction annulée en 409).
