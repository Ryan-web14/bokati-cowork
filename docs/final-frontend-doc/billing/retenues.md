# Retenues à la source (lot C6)

> Rédigé d'après le code (compléments de facturation, lots C0 à C16, 13/11/2026). Routes exactes et champs JSON : [routes.md](routes.md) et `routes/`.

## Saisie à l'encaissement

`POST /billing/documents/{n}/withholdings` :

```json
{"typeCode": "TVA_RETENUE", "amount": null, "withheldOn": "2026-11-13", "certificateReference": "ATT-2026-14", "certificateDocument": null}
```

- Sans `amount`, le montant vient du taux du type appliqué à sa base (TTC, HT ou TVA de la facture) ; un type sans taux (précompte) exige `amount`.
- La retenue solde la facture à hauteur de son montant ; elle ne dépasse pas le reste dû (`RETENUE_MONTANT`).
- Réponse : `withholdingNumber`, `amount`, `account`, `certificateMissing`...

## Suivi

- `GET /billing/documents/{n}/withholdings` ; `GET /billing/withholdings?certificate=missing` (attestations attendues) ou `?customerType=&customerCode=`.
- `PATCH /billing/withholdings/{n}/certificate` : `{"certificateReference", "certificateDocument"}` (fichier déposé en GED).
- `PATCH /billing/withholdings/{n}/cancel` : `{"reason"}` ; la facture redevient due d'autant.

## Types

`GET /billing/withholding-types`, `POST`, `PUT /{code}`, `PATCH /{code}/activate`, `/deactivate` (droit `BILLING:TAX_RULES`) : `code`, `label`, `rate` (facultatif), `base` (`TTC`, `HT`, `TVA`), `account`. Fournis : « Retenue de TVA » (100 % de la TVA, `4449`) et « Précompte » (montant saisi, `4492`).

Codes : `RETENUE_IMPOSSIBLE`, `RETENUE_MONTANT`, `RETENUE_ANNULEE`.
