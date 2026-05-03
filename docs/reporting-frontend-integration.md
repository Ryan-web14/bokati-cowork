# Guide Frontend — Reporting Financier & Taux d'occupation

## Objectif

Ce document decrit comment integrer cote frontend le nouveau module `reporting`.

Il couvre:

- tableau de bord financier global (KPIs)
- repartition des paiements par methode
- analyse du vieillissement des factures (aging)
- rapport par caisse enregistreuse
- flux de tresorerie (cash flow)
- taux d'occupation des ressources

Base API: `/sni/api/v1/reports`

---

## Pas de webhook dans ce module

Le module `reporting` est **uniquement en lecture**. Il n'emet aucun evenement et ne modifie aucune donnee.

Les webhooks existent dans les modules qui changent d'etat (paiement, reservation, abonnement). Ici, le frontend **poll** les endpoints quand il a besoin de donnees fraiches:

- Tableau de bord live: appel toutes les 30-60 secondes
- Rapport mensuel: appel au chargement de la page
- Export: appel unique a la demande

Il n'y a rien a "ecouter" — il n'y a pas d'evenement "rapport pret". Toutes les requetes sont synchrones et retournent immediatement.

---

## Parametres communs

Tous les endpoints acceptent des filtres de periode optionnels:

| Parametre | Type | Format | Description |
|-----------|------|--------|-------------|
| `from` | `string` | `YYYY-MM-DD` | Debut de periode (inclus). Si absent: pas de borne basse |
| `to` | `string` | `YYYY-MM-DD` | Fin de periode (inclus). Si absent: pas de borne haute |

Exemples:
```
?from=2026-04-01&to=2026-04-30   → avril 2026
?from=2026-01-01                  → depuis le 1er janvier 2026
                                   → toutes les donnees (pas de filtre)
```

---

## 1. Tableau de bord financier

```
GET /sni/api/v1/reports/finance/dashboard
```

Endpoint principal pour un resume KPI d'une periode. A utiliser pour:
- la page d'accueil admin
- le widget "finances du mois"
- les indicateurs de performance

### Reponse

```json
{
  "generatedAt": "2026-04-28T14:00:00Z",
  "fromDate": "2026-04-01",
  "toDate": "2026-04-30",

  "invoiceCount": 87,
  "totalInvoiced": 4850000,
  "totalPaid": 3920000,
  "totalOutstanding": 930000,
  "overdueCount": 5,
  "overdueAmount": 320000,
  "collectionRate": 80.82,
  "totalVat": 485000,
  "totalAdditionalCent": 24250,
  "totalDiscounts": 120000,
  "cancelledInvoiceCount": 3,

  "revenueBySource": [
    {
      "sourceType": "BOOKING",
      "invoiceCount": 52,
      "totalInvoiced": 2600000,
      "totalPaid": 2100000,
      "outstandingAmount": 500000
    },
    {
      "sourceType": "SUBSCRIPTION",
      "invoiceCount": 28,
      "totalInvoiced": 1800000,
      "totalPaid": 1620000,
      "outstandingAmount": 180000
    },
    {
      "sourceType": "MANUAL",
      "invoiceCount": 7,
      "totalInvoiced": 450000,
      "totalPaid": 200000,
      "outstandingAmount": 250000
    }
  ],

  "payments": {
    "transactionCount": 74,
    "totalReceived": 3920000,
    "cashAmount": 1200000,
    "walletAmount": 850000,
    "mobileMoneyAmount": 1050000,
    "bankTransferAmount": 620000,
    "cardAmount": 200000,
    "chequeAmount": 0,
    "failedCount": 4,
    "pendingCount": 1
  }
}
```

### Champs cles

| Champ | Usage UI |
|-------|----------|
| `collectionRate` | Jauge ou barre de progression "taux de recouvrement" |
| `totalOutstanding` | Badge rouge si > 0 |
| `overdueCount` / `overdueAmount` | Alerte si non nuls |
| `revenueBySource` | Camembert ou barres empilees par source |
| `payments.totalReceived` | Montant encaisse de la periode |
| `payments.failedCount` | Badge d'alerte sur la section paiements |

### Valeurs possibles de `sourceType`

| Valeur | Signification |
|--------|---------------|
| `BOOKING` | Facture generee depuis une reservation |
| `SUBSCRIPTION` | Facture generee depuis un abonnement |
| `MANUAL` | Facture creee manuellement (ou source non renseignee) |
| Autre | Source personnalisee configuree dans le systeme |

---

## 2. Repartition des paiements par methode

```
GET /sni/api/v1/reports/finance/payments
```

Detail complet des encaissements: repartition par methode + serie temporelle journaliere. A utiliser pour:
- les graphiques de tendance
- les barres de repartition cash / mobile money / virement
- les exports journaliers

### Reponse

```json
{
  "generatedAt": "2026-04-28T14:00:00Z",
  "fromDate": "2026-04-01",
  "toDate": "2026-04-30",
  "totalReceived": 3920000,
  "totalTransactions": 74,

  "byMethod": [
    { "method": "MOBILE_MONEY",  "count": 31, "amount": 1050000, "percentage": 26.79 },
    { "method": "CASH",          "count": 22, "amount": 1200000, "percentage": 30.61 },
    { "method": "WALLET",        "count": 12, "amount": 850000,  "percentage": 21.68 },
    { "method": "BANK_TRANSFER", "count": 6,  "amount": 620000,  "percentage": 15.82 },
    { "method": "CARD",          "count": 3,  "amount": 200000,  "percentage": 5.10  }
  ],

  "dailySeries": [
    {
      "date": "2026-04-01",
      "cash": 80000,
      "wallet": 30000,
      "mobileMoney": 45000,
      "bankTransfer": 0,
      "card": 0,
      "cheque": 0,
      "total": 155000
    },
    {
      "date": "2026-04-02",
      "cash": 120000,
      "wallet": 55000,
      "mobileMoney": 70000,
      "bankTransfer": 200000,
      "card": 0,
      "cheque": 0,
      "total": 445000
    }
  ]
}
```

### Notes

- `byMethod` est trie par montant decroissant. Ne pas supposer un ordre fixe.
- `dailySeries` ne contient que les jours avec au moins un paiement. Les jours sans transaction sont absents — le frontend doit combler les trous si une serie continue est necessaire.
- `percentage` est calcule sur `totalReceived`. La somme peut differer de 100 % a cause des arrondis.
- Les methodes `CHEQUE`, `CREDIT_NOTE`, `MANUAL_ADJUSTMENT` apparaissent uniquement si des transactions de ce type existent dans la periode.

### Methodes de paiement possibles

| Valeur | Label affiche |
|--------|---------------|
| `CASH` | Especes |
| `WALLET` | Portefeuille |
| `MOBILE_MONEY` | Mobile Money |
| `BANK_TRANSFER` | Virement bancaire |
| `CARD` | Carte bancaire |
| `CHEQUE` | Cheque |
| `CREDIT_NOTE` | Avoir |
| `MANUAL_ADJUSTMENT` | Ajustement manuel |

---

## 3. Vieillissement des factures (Aging)

```
GET /sni/api/v1/reports/finance/aging
```

Analyse des factures impayees classees par anciennete. Cet endpoint n'accepte **pas** de filtre de periode — il retourne toujours l'etat actuel du portefeuille creances. A utiliser pour:
- le tableau de suivi des impayes
- la relance client
- l'analyse du risque creances

### Reponse

```json
{
  "generatedAt": "2026-04-28T14:00:00Z",
  "totalOutstanding": 930000,
  "totalInvoices": 18,

  "current": {
    "label": "Non echu",
    "count": 9,
    "amount": 400000,
    "percentage": 43.01
  },
  "days1to30": {
    "label": "1 – 30 jours",
    "count": 4,
    "amount": 210000,
    "percentage": 22.58
  },
  "days31to60": {
    "label": "31 – 60 jours",
    "count": 3,
    "amount": 200000,
    "percentage": 21.51
  },
  "days61to90": {
    "label": "61 – 90 jours",
    "count": 1,
    "amount": 80000,
    "percentage": 8.60
  },
  "over90": {
    "label": "+ 90 jours",
    "count": 1,
    "amount": 40000,
    "percentage": 4.30
  }
}
```

### Notes

- Un bucket avec `count: 0` et `amount: 0` signifie qu'aucune facture ne tombe dans cette tranche.
- `current`: factures dont l'echeance est aujourd'hui ou dans le futur.
- Le classement est base sur `due_date` de la facture, pas sur sa date de creation.
- Seules les factures avec `balance_due > 0` et un statut actif (ni DRAFT, CANCELLED, VOIDED, PAID) sont incluses.

### Affichage recommande

Tableau avec une colonne par bucket et un code couleur:

| Bucket | Couleur |
|--------|---------|
| Non echu | Gris / neutre |
| 1 – 30 jours | Jaune / avertissement |
| 31 – 60 jours | Orange |
| 61 – 90 jours | Rouge clair |
| + 90 jours | Rouge fonce / critique |

---

## 4. Rapport par caisse enregistreuse

```
GET /sni/api/v1/reports/finance/cash-registers
```

Synthese par caisse pour une periode: sessions ouvertes, entrees/sorties, ecarts de caisse. A utiliser pour:
- la supervision des caisses
- la cloture de journee / semaine
- la verification des ecarts

### Reponse

```json
{
  "generatedAt": "2026-04-28T14:00:00Z",
  "fromDate": "2026-04-01",
  "toDate": "2026-04-30",
  "totalCashIn": 1200000,
  "totalCashOut": 180000,
  "netFlow": 1020000,

  "registers": [
    {
      "registerCode": "REG-001",
      "registerName": "Caisse reception",
      "sessionsCount": 22,
      "openingAmount": 440000,
      "cashIn": 900000,
      "cashOut": 80000,
      "netFlow": 820000,
      "totalVariance": -5000,
      "movementsCount": 145
    },
    {
      "registerCode": "REG-002",
      "registerName": "Caisse co-working",
      "sessionsCount": 18,
      "openingAmount": 360000,
      "cashIn": 300000,
      "cashOut": 100000,
      "netFlow": 200000,
      "totalVariance": 0,
      "movementsCount": 63
    }
  ]
}
```

### Champs cles

| Champ | Signification |
|-------|---------------|
| `openingAmount` | Somme des fonds de caisse en debut de session |
| `cashIn` | Encaissements: paiements recus, apports de caisse, transferts entrants |
| `cashOut` | Decaissements: remboursements, sorties de caisse, depots coffre |
| `netFlow` | `cashIn - cashOut` |
| `totalVariance` | Somme des ecarts constates a la cloture des sessions (negatif = manque, positif = surplus) |
| `movementsCount` | Nombre total de mouvements hors fond de caisse et comptage de cloture |

### Note variance

`totalVariance` negatif indique que les agents ont declare moins d'argent que ce que le systeme attendait. C'est le principal indicateur d'alerte sur une caisse.

---

## 5. Flux de tresorerie (Cash Flow)

```
GET /sni/api/v1/reports/finance/cash-flow
```

Analyse des flux de caisse physique par type de mouvement et par jour. A utiliser pour:
- le graphique entrees/sorties journalier
- la reconciliation de tresorerie
- le suivi des flux par categorie

### Reponse

```json
{
  "generatedAt": "2026-04-28T14:00:00Z",
  "fromDate": "2026-04-01",
  "toDate": "2026-04-30",
  "totalInflows": 1200000,
  "totalOutflows": 180000,
  "netCashFlow": 1020000,

  "byMovementType": [
    { "movementType": "PAYMENT",      "direction": "IN",  "count": 120, "totalAmount": 1050000 },
    { "movementType": "CASH_IN",      "direction": "IN",  "count": 8,   "totalAmount": 150000 },
    { "movementType": "REFUND",       "direction": "OUT", "count": 5,   "totalAmount": 80000 },
    { "movementType": "SAFE_DEPOSIT", "direction": "OUT", "count": 10,  "totalAmount": 60000 },
    { "movementType": "CASH_OUT",     "direction": "OUT", "count": 3,   "totalAmount": 40000 }
  ],

  "dailySeries": [
    { "date": "2026-04-01", "inflows": 155000, "outflows": 12000, "net": 143000 },
    { "date": "2026-04-02", "inflows": 210000, "outflows": 8000,  "net": 202000 }
  ]
}
```

### Types de mouvement

| `movementType` | `direction` | Description |
|----------------|-------------|-------------|
| `PAYMENT` | IN | Paiement client encaisse en especes |
| `CASH_IN` | IN | Apport manuel en caisse |
| `TRANSFER_IN` | IN | Transfert depuis une autre caisse |
| `REFUND` | OUT | Remboursement en especes |
| `CASH_OUT` | OUT | Retrait manuel de caisse |
| `SAFE_DEPOSIT` | OUT | Depot au coffre |
| `TRANSFER_OUT` | OUT | Transfert vers une autre caisse |
| `ADJUSTMENT` | — | Ajustement (exclu de la serie journaliere) |

### Notes

- `dailySeries` ne contient que les jours avec au moins un mouvement. Combler les trous si necessaire.
- `OPENING_FLOAT` et `CLOSING_COUNT` sont exclus de tous les calculs — ce sont des operations de controle, pas des flux reels.
- `ADJUSTMENT` est exclu de la serie journaliere mais present dans `byMovementType` si des ajustements ont ete faits.

---

## 6. Taux d'occupation des ressources

```
GET /sni/api/v1/reports/occupancy
```

Calcul du taux d'occupation par type de ressource et par ressource individuelle. A utiliser pour:
- le tableau de bord d'exploitation des espaces
- la comparaison de performance entre ressources
- l'identification des ressources sous-utilisees

### Reponse

```json
{
  "generatedAt": "2026-04-28T14:00:00Z",
  "fromDate": "2026-04-01",
  "toDate": "2026-04-30",
  "periodDays": 30,
  "totalBookings": 214,
  "totalBookedMinutes": 38520,
  "overallOccupancyRate": 53.50,

  "byResourceType": [
    {
      "typeCode": "BUREAU_PRIVE",
      "typeName": "Bureau prive",
      "resourceCount": 6,
      "totalBookings": 148,
      "totalBookedMinutes": 26640,
      "totalRevenue": 2220000,
      "occupancyRate": 92.50
    },
    {
      "typeCode": "SALLE_REUNION",
      "typeName": "Salle de reunion",
      "resourceCount": 3,
      "totalBookings": 52,
      "totalBookedMinutes": 7800,
      "totalRevenue": 390000,
      "occupancyRate": 54.17
    },
    {
      "typeCode": "OPEN_SPACE",
      "typeName": "Open space",
      "resourceCount": 2,
      "totalBookings": 14,
      "totalBookedMinutes": 4080,
      "totalRevenue": 136000,
      "occupancyRate": 28.33
    }
  ],

  "byResource": [
    {
      "resourceCode": "RES-BUR-001",
      "resourceName": "Bureau Lembissi",
      "typeCode": "BUREAU_PRIVE",
      "typeName": "Bureau prive",
      "bookingCount": 28,
      "bookedMinutes": 5040,
      "availableMinutes": 14400,
      "occupancyRate": 35.00,
      "revenue": 420000
    }
  ]
}
```

### Formule du taux d'occupation

```
occupancyRate = (bookedMinutes / availableMinutes) × 100

availableMinutes = periodDays × 480   (8 heures × 60 minutes par jour)
```

Le denominateur de 8h/jour est une convention coworking standard. Il represente la plage horaire d'exploitation normale. Le numerateur est la somme des `duration_minutes` des reservations en statut `CONFIRMED`, `IN_PROGRESS` ou `COMPLETED`.

### Interpretation

| Taux | Interpretation |
|------|----------------|
| < 30 % | Ressource sous-utilisee |
| 30 – 60 % | Utilisation normale |
| 60 – 80 % | Bonne performance |
| > 80 % | Ressource tres demandee — envisager l'extension |

### Reservations incluses dans le calcul

Seuls les statuts suivants sont comptabilises:

| Statut | Inclus |
|--------|--------|
| `CONFIRMED` | Oui |
| `IN_PROGRESS` | Oui |
| `COMPLETED` | Oui |
| `DRAFT` | Non |
| `PENDING_APPROVAL` | Non |
| `CANCELLED` | Non |
| `NO_SHOW` | Non |
| `EXPIRED` | Non |

### Note sur `overallOccupancyRate`

Le taux global est calcule sur l'ensemble des ressources actives, pas une moyenne des taux individuels. Une ressource avec beaucoup de reservations courtes pese plus qu'une ressource avec peu de reservations longues.

---

## Cas d'usage recommandes par ecran

### Dashboard admin (page d'accueil)

```
GET /reports/finance/dashboard?from=DEBUT_MOIS&to=AUJOURD_HUI
GET /reports/occupancy?from=DEBUT_MOIS&to=AUJOURD_HUI
```

Afficher: total encaisse, taux de recouvrement, taux d'occupation global, alertes impayes.

### Page "Rapports financiers"

```
GET /reports/finance/dashboard?from=...&to=...
GET /reports/finance/payments?from=...&to=...
GET /reports/finance/aging
```

Afficher: KPIs, camembert des methodes de paiement, serie journaliere, tableau aging.

### Page "Caisses"

```
GET /reports/finance/cash-registers?from=...&to=...
GET /reports/finance/cash-flow?from=...&to=...
```

Afficher: tableau des caisses, graphique entrees/sorties journalier.

### Page "Occupation"

```
GET /reports/occupancy?from=...&to=...
```

Afficher: tableau par type, tableau par ressource, barres de progression taux d'occupation.

---

## Periodicite recommandee pour le rafraichissement

| Ecran | Frequence recommandee |
|-------|-----------------------|
| Dashboard live | 60 secondes |
| Rapport du mois en cours | Au chargement de la page |
| Rapport historique | Une seule fois (pas de refresh automatique) |
| Aging | Au chargement (les donnees changent rarement en temps reel) |