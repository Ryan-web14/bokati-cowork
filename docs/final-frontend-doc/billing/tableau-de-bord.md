# Tableau de bord commercial (lot C15)

> Rédigé d'après le code (compléments de facturation, lots C0 à C16, 13/11/2026). Routes exactes et champs JSON : [routes.md](routes.md) et `routes/`.

`GET /billing/dashboard?from=&to=&currency=XAF&weeks=8` (par défaut le mois en cours) :

| Bloc | Champs |
| --- | --- |
| `quotes` | `issued`, `converted`, `conversionRate` (%), `issuedAmount`, `convertedAmount`, `averageDaysToInvoice` (du devis à sa facture) |
| `invoicing` | `invoices`, `invoiced`, `collected`, `outstanding`, `overdue`, `averageDaysToPay` (factures soldées de la période) |
| `forecast` | par semaine (`weekStart` lundi) : `expected`, `documents` ; une échéance passée compte pour la semaine en cours |
| `deals` | par étape : `deals`, `expectedAmount`, `weightedAmount` (pondéré par la probabilité) |
| `topCustomers` | dix meilleurs clients : `invoiced`, `outstanding` |

Une devise à la fois, jamais de cumul entre devises.
