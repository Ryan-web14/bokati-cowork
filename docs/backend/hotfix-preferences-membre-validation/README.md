# Correctif : préférences membre refusées en 500

## Constat

`PATCH /sni/api/v1/members/{memberId}/settings` avec `billingPref.billingCompanyName = ""` répondait `500` (« size must be between 1 and 150 » au commit). L'entité `MemberSettings` impose 1 à 150 caractères, alors que la requête acceptait une chaîne vide et jusqu'à 255 caractères.

## Correction

- Chaîne vide : depuis le lot « préférences des membres » (`docs/backend/preferences-membres/`), une chaîne vide retire la valeur (`null`). Le cas est désormais couvert par un test.
- `BillingPreferences.billingCompanyName` : 150 caractères au plus, comme la colonne. Au-delà, la réponse est `400` à la lecture de la requête.
- `GlobalExceptionHandler` : une contrainte d'entité violée au commit (`TransactionSystemException` qui enveloppe une `ConstraintViolationException`) répond `400 VALIDATION_FAILED`. La réponse indique le champ et le message, au lieu d'un `500`. Les autres échecs de transaction restent des `500`.

## Tests

- `EntityValidationErrorTest` : 400 pour une contrainte d'entité, 500 sinon.
- `MemberSettingsPatchTest` : un nom vide est retiré.
