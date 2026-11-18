# Montants des factures en USD et en EUR

> Demande du 18/11/2026, faite sur `feature/booking`. Migration V385 (prod V379).

## Principe

Une facture affiche son total en devises étrangères, à titre indicatif : USD et EUR par défaut. La liste des devises se règle par le paramètre `billing.invoice.equivalent-currencies` (codes séparés par des virgules ; vide, aucune).

- **Taux** : ceux de la comptabilité (taux du jour récupérés ou saisis, `ExchangeRateService`) ; l'euro a sa parité fixe de 655,957 XAF. Une devise sans taux connu est omise.
- **Figés à l'émission** : au scellement de la pièce, les équivalents sont calculés au taux de la date d'émission et enregistrés sur la pièce (colonne `currency_equivalents_json`, hors des champs verrouillés). Le montant affiché ne bouge plus avec les taux.
- **Avant l'émission** : calcul au taux du jour, marqué `frozen: false`.
- **Reste dû** : converti au même taux que le total.

## Route

`GET /sni/api/v1/billing/documents/{documentNumber}/currency-equivalents` (droits de lecture de la facturation) rend une liste :

```json
[
  { "currency": "USD", "rate": 600.0000, "totalAmount": 1093.26, "balanceDue": 546.63, "rateDate": "2026-11-18", "frozen": true },
  { "currency": "EUR", "rate": 655.9570, "totalAmount": 1000.00, "balanceDue": 500.00, "rateDate": "2026-11-18", "frozen": true }
]
```

`rate` est le nombre d'unités de la devise de la pièce pour une unité étrangère.

## PDF

Sous le total TTC, une ligne par devise : « Soit en EUR (1 EUR = 655,96 XAF, taux du 18/11/2026) : 1 000,00 EUR ».

## Écran

Sur la fiche d'une facture, sous le total, afficher les équivalents rendus par la route, avec la mention « au taux du … » et, si `frozen` vaut faux, « indicatif, taux du jour ». Proposer un sélecteur « Afficher en » (XAF, USD, EUR) qui convertit le total et le reste dû avec ces taux.

## Tests

`BillingCurrencyEquivalentsTest` : USD et EUR calculés, taux et reste dû ; équivalents figés à l'émission et reste dû au taux figé. Les tests de rendu du PDF passent.
