# États de compte

Préfixe des routes : `/sni/api/v1`.

Un état de compte dit ce qui a été facturé sur une période, ce qui a été réglé, ce qui reste dû et
depuis combien de temps.

- **Période** : `from` et `to` (dates ISO), douze mois jusqu'à aujourd'hui par défaut, trois ans
  au plus.
- **Ce qui reste dû se lit à la date de l'état**, période ou non : une facture de mars encore
  impayée figure dans l'état de septembre, avec `inPeriod: false` (en italique dans le PDF). Elle
  compte dans `balanceDue` et l'ancienneté, pas dans `invoiced`.
- Les brouillons n'y figurent jamais.
- **Organisation** : ses propres documents, plus les factures de membres qui lui sont reliées
  (`AT_ISSUE` ou `ATTACHED`) et les avoirs émis sur ces factures.
- **Membre** : toutes ses factures, réparties entre son compte personnel (`PERSONAL`) et chaque
  organisation à laquelle une facture est reliée (`COMPANY`).

## Routes

| Qui | Méthode et chemin |
| --- | --- |
| Administration | `GET /companies/{customerId}/statement` (et `.pdf`, `.csv`) |
| Administration | `GET /members/{memberId}/statement` (et `.pdf`, `.csv`) |
| Gérant (portail) | `GET /client/companies/{customerId}/statement` (et `.pdf`) |
| Membre (portail) | `GET /client/billing/statement` (et `.pdf`) |

## Réponse (extrait)

```json
{
  "subject": "COMPANY",
  "code": "CUS-COM-202610-00000012",
  "name": "SARL Mboté",
  "from": "2025-10-31", "to": "2026-10-30", "asOf": "2026-10-30",
  "creditLimit": 500000, "paymentTermsDays": 30, "blocked": false,
  "totals": [{
    "currency": "XAF",
    "invoiced": 350000, "paid": 240000,
    "balanceDue": 110000, "overdue": 60000,
    "creditAvailable": 0, "netBalanceDue": 110000,
    "openInvoices": 2, "overdueInvoices": 1,
    "aging": { "notDue": 50000, "days1To30": 0, "days31To60": 60000, "days61To90": 0, "over90": 0 }
  }],
  "accounts": [
    { "accountType": "COMPANY", "accountCode": "CUS-COM-...", "accountName": "SARL Mboté", "balanceDue": 60000, "...": "..." },
    { "accountType": "MEMBER", "accountCode": "MBR-...", "accountName": "Jean Mbemba", "balanceDue": 50000, "...": "..." }
  ],
  "lines": [{
    "documentNumber": "INV-...", "documentType": "INVOICE", "status": "OVERDUE",
    "issueDate": "2026-09-01", "dueDate": "2026-09-15", "currency": "XAF",
    "accountType": "COMPANY", "accountCode": "CUS-COM-...", "accountName": "SARL Mboté",
    "companyLinkSource": null, "totalAmount": 100000, "paidAmount": 40000, "balanceDue": 60000,
    "impact": 60000, "daysOverdue": 45, "inPeriod": true
  }]
}
```

`totals` contient une entrée par devise : deux devises ne s'additionnent pas. `impact` est l'effet
signé sur le solde (un avoir disponible compte en négatif, un document soldé ou annulé à zéro).
