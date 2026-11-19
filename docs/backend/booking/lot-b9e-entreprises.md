# Lot B9e : entreprises

> Partie du [plan booking](README.md). Fait le 19/11/2026 sur `feature/booking`. Décision D7. Migration V396 (prod V390).

Concerne les réservations **facturées à une entreprise** (`billingCompanyCode`, choix « pour mon organisation » du membre, ou saisi par l'équipe).

## Conditions de l'entreprise

`GET|PUT /bookings/companies/{businessCode}/terms` : `{ monthlyStatement, monthlyBudget, monthlyHours }` (0 ou vide : sans).

- **Budget mensuel de l'entreprise** (`monthlyBudget`) : somme des réservations du mois facturées à l'entreprise (hors annulées, refusées, expirées). Une réservation qui le dépasse est refusée : `409 COMPANY_BUDGET_EXCEEDED` (« Budget mensuel de l'entreprise dépassé : 110000 pour 100000 XAF »).
- **Budget par collaborateur** : `GET /bookings/companies/{businessCode}/budgets`, `PUT /bookings/companies/{businessCode}/budgets/{memberId}` `{ monthlyBudget }` (0 le retire). Même refus, « Budget mensuel du collaborateur dépassé ».
- **Forfait d'heures partagé** (`monthlyHours`) : les minutes restantes du mois couvrent la réservation (`companyCoveredMinutes`), le reste est facturé au prorata (comme la couverture partielle du lot B2).
- **Relevé mensuel groupé** (`monthlyStatement`) : la réservation est confirmée sans attendre de paiement (`monthlyStatement: true` sur la réservation), son élément à facturer reste en attente ; une seule facture par entreprise reprend le mois.

## Suivi

- `GET /bookings/companies/{businessCode}/usage?month=AAAA-MM` : dépense du mois, budget, minutes couvertes et restantes du forfait, et dépense de chaque collaborateur qui a un budget.
- Relevés : tâche `BookingCompanyStatementWorker` (toutes les 6 heures, `bokati.booking.company-statements.worker-delay-ms`) qui émet le relevé du mois précédent (« Relevé des réservations AAAA-MM ») ; `POST /bookings/companies/statements/run?month=AAAA-MM` pour le lancer à la main. Un relevé déjà émis pour un mois n'est jamais refait (table `booking_company_statement`).

## Tests

`BookingB9eTest` : forfait d'heures au prorata, budgets de l'entreprise et du collaborateur, relevé mensuel marqué, réservation sans entreprise intacte.
