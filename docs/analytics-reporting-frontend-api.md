# Analytics And Reporting Frontend API

Base path: `/sni/api/v1`

Les rapports utilisent des requetes SQL natives et consolident les modules billing, payment, wallet, booking, subscription, customer/member et inventory.

## Overview

`GET /analytics/overview?fromDate=2026-04-01&toDate=2026-04-30`

Retourne:

- `financial`: factures, montant facture, encaisse, restant du, devis, TVA, centimes additionnels.
- `payment`: transactions, encaissement reussi, cash, wallet, mobile money, banque, carte, echecs, pending.
- `wallet`: nombre de wallets, solde disponible, solde bloque, credits, debits.
- `booking`: reservations, confirmees, terminees, annulees, no-show, revenus, minutes reservees, participants.
- `subscription`: abonnements actifs, pending, suspendus, pass actifs, MRR, usage billable.
- `customer`: clients, membres, membres actifs, bookings guest.
- `inventory`: items actifs, valeur stock, alertes low/out of stock, bons de commande ouverts.

## Financial Summary

`GET /reports/financial/summary?fromDate=2026-04-01&toDate=2026-04-30`

Response:

```json
{
  "invoiceCount": 12,
  "invoicedAmount": 1500000,
  "paidAmount": 950000,
  "outstandingAmount": 550000,
  "overdueInvoiceCount": 2,
  "quotationCount": 4,
  "quotedAmount": 700000,
  "vatAmount": 228813,
  "additionalCentAmount": 11440
}
```

## Attendance Summary

`GET /reports/attendance/summary?fromDate=2026-04-01&toDate=2026-04-30`

Response:

```json
{
  "bookingCount": 42,
  "confirmedBookings": 20,
  "completedBookings": 15,
  "cancelledBookings": 4,
  "noShowBookings": 3,
  "bookingRevenue": 500000,
  "bookedMinutes": 8400,
  "participantCount": 63
}
```

## Notes Frontend

Les dates sont au format `YYYY-MM-DD`. Si aucune date n'est fournie, les agregats couvrent toutes les donnees disponibles.

Les montants sont retournes avec precision decimal backend. Le frontend doit formatter selon la devise du contexte, par defaut `XAF` pour le Congo CG.

