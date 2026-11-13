# Facturation groupée (lot C13)

> Rédigé d'après le code (compléments de facturation, lots C0 à C16, 13/11/2026). Routes exactes et champs JSON : [routes.md](routes.md) et `routes/`.

1. `POST /billing/batch-runs` `{"periodFrom", "periodTo", "sourceTypes": ["BOOKING"], "title"}` : aperçu des consommations en attente (abonnements, options, dépassements, réservations...) client par client ; `totalToInvoice`.
2. `PATCH /billing/batch-runs/{n}/exclude` `{"itemId", "excluded": true}` : écarter un client.
3. `POST /billing/batch-runs/{n}/issue` : une facture émise par client non exclu ; chaque ligne renvoie `invoiceNumber` ou `error`.
4. `POST /billing/batch-runs/{n}/cancel` : abandon de l'aperçu.

`GET /billing/batch-runs`, `GET /billing/batch-runs/{n}`. Statuts : `PREVIEW`, `ISSUED`, `CANCELLED`. Une consommation facturée ne revient jamais ; une consommation retenue par un aperçu en cours n'entre pas dans un second. Codes : `LANCEMENT_INVALIDE`, `RIEN_A_FACTURER`, `LANCEMENT_STATUT`.
