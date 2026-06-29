# Guide Frontend — Module Reporting Complet

## Objectif

Ce document decrit l'integration complete du module `reporting`. Il couvre les 8 rapports disponibles :

**Rapports financiers (existants)**
1. Tableau de bord financier (KPIs, paiements, aging, caisses, cash flow)
2. Taux d'occupation des ressources

**Nouveaux rapports metier**
3. Rapport d'activite consolide (cross-module)
4. Fiche membre 360
5. Rapport de recouvrement (impayes)
6. Utilisation des ressources (creneaux, heatmap, revenu/heure)
7. Valorisation du stock
8. Analytics abonnements (MRR, churn, renouvellements)

Base API : `/sni/api/v1/reports`

---

## Conventions communes

### Module en lecture seule

Le module `reporting` ne modifie aucune donnee. Pas de webhook. Le frontend poll les endpoints :
- Dashboard live : toutes les 30-60 secondes
- Rapport mensuel : au chargement de la page
- Export PDF/CSV : appel unique a la demande

### Parametres de periode

Tous les rapports avec filtre temporel acceptent :

| Parametre | Type | Format | Description |
|-----------|------|--------|-------------|
| `from` | `string` | `YYYY-MM-DD` | Debut de periode (inclus) |
| `to` | `string` | `YYYY-MM-DD` | Fin de periode (inclus) |

Alternatives acceptees : `fromDate` / `toDate` (meme format, prioritaires si les deux sont fournis).

```
?from=2026-06-01&to=2026-06-30    → juin 2026
?from=2026-01-01                   → depuis le 1er janvier
(aucun parametre)                  → toutes les donnees
```

### Formats d'export

| Format | Content-Type | Suffixe endpoint |
|--------|-------------|-----------------|
| JSON | `application/json` | (endpoint principal) |
| PDF | `application/pdf` | `/pdf` |
| CSV | `text/csv` | `/csv` |

Les PDF sont retournes avec `Content-Disposition: attachment; filename="nom.pdf"`.

### Montants

Tous les montants sont en **XAF** (francs CFA), entiers sans decimales. Formater cote frontend :
```
2450000 → "2 450 000 XAF"
```

---

## 1. Rapport d'activite consolide

Resume cross-module de toute l'activite sur une periode : bookings, factures, paiements, abonnements, stock, contrats, membres, support.

### Endpoints

```
GET /sni/api/v1/reports/activity
GET /sni/api/v1/reports/activity/pdf
GET /sni/api/v1/reports/activity/csv
```

### Parametres

| Parametre | Type | Default | Description |
|-----------|------|---------|-------------|
| `from` | string | - | Debut de periode |
| `to` | string | - | Fin de periode |
| `compareWithPrevious` | boolean | `false` | Inclure la comparaison avec la periode precedente |

Quand `compareWithPrevious=true`, chaque section contient un objet `variation` avec les valeurs actuelles, precedentes et le pourcentage de changement.

### Reponse JSON

```json
{
  "generatedAt": "2026-06-22T15:00:00Z",
  "fromDate": "2026-06-01",
  "toDate": "2026-06-30",

  "bookings": {
    "totalCount": 47,
    "confirmedCount": 35,
    "completedCount": 30,
    "cancelledCount": 5,
    "totalRevenue": 2450000,
    "totalBookedMinutes": 1420,
    "variation": null
  },

  "invoices": {
    "totalCount": 32,
    "totalInvoiced": 4850000,
    "totalPaid": 3920000,
    "totalOutstanding": 930000,
    "overdueCount": 5,
    "variation": null
  },

  "payments": {
    "transactionCount": 28,
    "totalReceived": 3920000,
    "cashAmount": 850000,
    "walletAmount": 620000,
    "mobileMoneyAmount": 1200000,
    "bankTransferAmount": 1050000,
    "cardAmount": 200000,
    "variation": null
  },

  "subscriptions": {
    "activeCount": 15,
    "newCount": 3,
    "cancelledCount": 1,
    "renewedCount": 5,
    "variation": null
  },

  "stock": {
    "inboundMovements": 15,
    "outboundMovements": 8,
    "inboundValue": 750000,
    "outboundValue": 320000,
    "variation": null
  },

  "contracts": {
    "signedCount": 4,
    "expiredCount": 1,
    "activeCount": 18,
    "variation": null
  },

  "members": {
    "totalActive": 125,
    "newCount": 12,
    "variation": null
  },

  "support": {
    "openedCount": 8,
    "resolvedCount": 6,
    "closedCount": 5,
    "highPriorityCount": 2,
    "variation": null
  }
}
```

### Avec comparaison (`compareWithPrevious=true`)

Chaque section inclut un objet `variation` :

```json
{
  "bookings": {
    "totalCount": 47,
    "variation": {
      "previousValue": 42,
      "currentValue": 47,
      "changePercent": 11.9
    }
  }
}
```

| Champ | Description |
|-------|-------------|
| `previousValue` | Valeur de la periode precedente (meme duree) |
| `currentValue` | Valeur de la periode actuelle |
| `changePercent` | Variation en % (positif = hausse, negatif = baisse) |

### Affichage recommande

- **Page d'accueil admin** : 8 KPI cards (une par section)
- **Couleur variation** : vert si `changePercent > 0`, rouge si `< 0`
- **Icone** : fleche haut/bas selon le signe

### Exports

- **PDF** : `rapport-activite.pdf` — A4, KPI cards + tableaux par section + variation
- **CSV** : `rapport-activite.csv` — Sections separees par lignes vides

---

## 2. Fiche membre 360

Dossier complet d'un membre : profil, abonnements, bookings, factures, paiements, wallet, contrats, tickets support.

### Endpoints

```
GET /sni/api/v1/reports/member-profile/{memberId}
GET /sni/api/v1/reports/member-profile/{memberId}/pdf
```

### Parametres

| Parametre | Type | Description |
|-----------|------|-------------|
| `memberId` | string (path) | Code du membre (ex: `MBR-000012`) |

### Reponse JSON

```json
{
  "generatedAt": "2026-06-22T15:00:00Z",

  "member": {
    "memberCode": "MBR-000012",
    "firstname": "Jean",
    "lastname": "Dupont",
    "email": "jean.dupont@example.com",
    "phone": "+242 06 XXX XX XX",
    "whatsappPhone": "+242 06 XXX XX XX",
    "status": "ACTIVE",
    "customerCode": "CUS-000008",
    "customerName": "Jean Dupont",
    "jobTitle": "Freelance Designer",
    "companyRole": null,
    "birthDate": "1990-05-15",
    "city": "Brazzaville",
    "country": "CG",
    "photoUrl": null,
    "portalAccess": true,
    "createdAt": "2026-01-15"
  },

  "subscriptions": [
    {
      "subscriptionNumber": "SUB-000045",
      "planName": "Premium Cowork",
      "status": "ACTIVE",
      "billingCycle": "MONTHLY",
      "totalAmount": 75000,
      "currency": "XAF",
      "startDate": "2026-01-15",
      "currentPeriodEnd": "2026-07-15",
      "autoRenew": true
    }
  ],

  "bookings": [
    {
      "bookingNumber": "BKG-000123",
      "resourceName": "Salle A - Conference",
      "status": "COMPLETED",
      "startDate": "2026-06-20",
      "durationMinutes": 120,
      "totalAmount": 25000,
      "currency": "XAF"
    }
  ],

  "invoices": [
    {
      "documentNumber": "FAC-000087",
      "status": "PAID",
      "totalAmount": 75000,
      "paidAmount": 75000,
      "balanceDue": 0,
      "currency": "XAF",
      "issueDate": "2026-06-15",
      "dueDate": "2026-06-30"
    }
  ],

  "payments": [
    {
      "transactionNumber": "TXN-000156",
      "paymentMethod": "CASH",
      "amount": 75000,
      "currency": "XAF",
      "status": "SUCCEEDED",
      "paidAt": "2026-06-15"
    }
  ],

  "wallet": {
    "walletNumber": "WAL-000012",
    "availableBalance": 150000,
    "ledgerBalance": 150000,
    "heldBalance": 0,
    "currency": "XAF",
    "status": "ACTIVE"
  },

  "contracts": [
    {
      "contractCode": "CTR-000005",
      "title": "Contrat Premium 2026",
      "status": "ACTIVATED",
      "startDate": "2026-01-15",
      "endDate": "2027-01-15",
      "renewalType": "AUTO_RENEWAL",
      "signedAt": "2026-01-14"
    }
  ],

  "tickets": [
    {
      "ticketNumber": "TKT-000034",
      "title": "Probleme WiFi salle B",
      "status": "RESOLVED",
      "priority": "MEDIUM",
      "category": "TECHNICAL",
      "createdAt": "2026-06-10",
      "resolvedAt": "2026-06-11"
    }
  ]
}
```

### Sections du rapport

| Section | Contenu | Utilisation |
|---------|---------|-------------|
| `member` | Profil complet (nom, contact, status, date creation) | En-tete du rapport |
| `subscriptions` | Abonnements actifs avec plan et cycle | Encart abonnements |
| `bookings` | Reservations recentes (triees par date DESC) | Historique |
| `invoices` | Factures avec statut et montants | Suivi facturation |
| `payments` | Paiements effectues (methode, montant, date) | Historique paiements |
| `wallet` | Solde disponible, bloque, ledger | Encart portefeuille |
| `contracts` | Contrats actifs/expires | Suivi contractuel |
| `tickets` | Tickets support recents | Suivi SAV |

### Affichage recommande

- **KPI cards en haut** : Nombre de bookings, Total facture, Total paye, Solde wallet
- **Onglets ou sections** : Un par domaine (Bookings, Factures, Paiements, etc.)
- **Badges couleur** pour les statuts : `ACTIVE`=vert, `OVERDUE`=rouge, `PENDING`=orange

### Export PDF

`fiche-membre-{memberId}.pdf` — Document multi-page A4 avec toutes les sections.

---

## 3. Rapport de recouvrement (impayes)

Toutes les factures impayees avec detail client, montants et age.

### Endpoints

```
GET /sni/api/v1/reports/debt-recovery
GET /sni/api/v1/reports/debt-recovery/pdf
GET /sni/api/v1/reports/debt-recovery/csv
```

### Parametres

| Parametre | Type | Default | Description |
|-----------|------|---------|-------------|
| `sortBy` | string | `age` | Tri des factures : `age`, `amount`, `customer` |

### Reponse JSON

```json
{
  "generatedAt": "2026-06-22T15:00:00Z",
  "totalOutstanding": 930000,
  "totalInvoices": 12,

  "aging": {
    "current": {
      "label": "Non echu",
      "invoiceCount": 3,
      "totalAmount": 180000,
      "averageAmount": 60000,
      "percentage": 19.4
    },
    "days1to30": {
      "label": "1-30 jours",
      "invoiceCount": 4,
      "totalAmount": 250000,
      "averageAmount": 62500,
      "percentage": 26.9
    },
    "days31to60": {
      "label": "31-60 jours",
      "invoiceCount": 2,
      "totalAmount": 200000,
      "averageAmount": 100000,
      "percentage": 21.5
    },
    "days61to90": {
      "label": "61-90 jours",
      "invoiceCount": 1,
      "totalAmount": 100000,
      "averageAmount": 100000,
      "percentage": 10.8
    },
    "days90plus": {
      "label": "90+ jours",
      "invoiceCount": 2,
      "totalAmount": 200000,
      "averageAmount": 100000,
      "percentage": 21.5
    }
  },

  "unpaidInvoices": [
    {
      "documentNumber": "FAC-000045",
      "customerName": "Jean Dupont",
      "customerCode": "MBR-000012",
      "customerType": "MEMBER",
      "customerEmail": "jean@example.com",
      "customerPhone": "+242 06 XXX XX XX",
      "issueDate": "2026-05-01",
      "dueDate": "2026-05-15",
      "totalAmount": 75000,
      "paidAmount": 0,
      "balanceDue": 75000,
      "agingDays": 38,
      "agingBucket": "31-60 jours"
    }
  ]
}
```

### Tranches d'age (aging buckets)

| Tranche | Couleur recommandee | Urgence |
|---------|-------------------|---------|
| Non echu | Gris `#9CA3AF` | Basse |
| 1-30 jours | Jaune `#F59E0B` | Moyenne |
| 31-60 jours | Orange `#F97316` | Haute |
| 61-90 jours | Rouge `#EF4444` | Tres haute |
| 90+ jours | Rouge fonce `#991B1B` | Critique |

### Affichage recommande

- **Resume** : Total impaye + nombre de factures + diagramme camembert par tranche
- **Tableau principal** : Liste triable des factures impayees
- **Actions** : Lien vers la facture, bouton "envoyer relance"
- **Filtres** : Par tranche d'age, par client

### Exports

- **PDF** : `rapport-recouvrement.pdf`
- **CSV** : `rapport-recouvrement.csv`

---

## 4. Utilisation des ressources

Occupation par creneau (matin/apres-midi/soir), heatmap horaire, revenu par heure, ressources sous-utilisees.

### Endpoints

```
GET /sni/api/v1/reports/resource-utilization
GET /sni/api/v1/reports/resource-utilization/pdf
```

### Parametres

| Parametre | Type | Default | Description |
|-----------|------|---------|-------------|
| `from` | string | - | Debut de periode |
| `to` | string | - | Fin de periode |

### Reponse JSON

```json
{
  "generatedAt": "2026-06-22T15:00:00Z",
  "fromDate": "2026-06-01",
  "toDate": "2026-06-30",
  "periodDays": 30,
  "overallOccupancyRate": 62.5,

  "resources": [
    {
      "resourceCode": "RES-001",
      "resourceName": "Salle A - Conference",
      "resourceType": "MEETING_ROOM",
      "totalBookings": 28,
      "morningBookings": 12,
      "afternoonBookings": 10,
      "eveningBookings": 6,
      "bookedMinutes": 3360,
      "availableMinutes": 7200,
      "occupancyRate": 46.7,
      "totalRevenue": 700000,
      "revenuePerHour": 12500
    }
  ],

  "hourlyHeatmap": [
    { "hour": 8,  "bookingCount": 15, "percentage": 10.2 },
    { "hour": 9,  "bookingCount": 25, "percentage": 17.0 },
    { "hour": 10, "bookingCount": 30, "percentage": 20.4 },
    { "hour": 11, "bookingCount": 22, "percentage": 15.0 },
    { "hour": 14, "bookingCount": 28, "percentage": 19.0 },
    { "hour": 15, "bookingCount": 18, "percentage": 12.2 }
  ],

  "underutilized": [
    {
      "resourceCode": "RES-005",
      "resourceName": "Bureau ouvert C",
      "resourceType": "HOT_DESK",
      "bookedMinutes": 960,
      "occupancyRate": 13.3,
      "totalRevenue": 120000
    }
  ]
}
```

### Creneaux horaires

| Creneau | Heures | Champ |
|---------|--------|-------|
| Matin | 8h - 12h | `morningBookings` |
| Apres-midi | 12h - 17h | `afternoonBookings` |
| Soir | 17h - 21h | `eveningBookings` |

### Heatmap

Le tableau `hourlyHeatmap` fournit le nombre de bookings par heure de la journee. Utiliser pour afficher une heatmap :

```
Heure   | Bookings | Intensite
08:00   |    15    | ████░░░░░░
09:00   |    25    | ██████░░░░
10:00   |    30    | ████████░░  ← pic
11:00   |    22    | █████░░░░░
...
```

### Ressources sous-utilisees

Le tableau `underutilized` liste les ressources avec un taux d'occupation inferieur a 30%. Afficher avec un badge rouge d'alerte.

### Export PDF

`rapport-utilisation-ressources.pdf`

---

## 5. Valorisation du stock

Valeur du stock par categorie et emplacement, articles a reapprovisionner, stock mort.

### Endpoints

```
GET /sni/api/v1/reports/stock-valuation
GET /sni/api/v1/reports/stock-valuation/pdf
```

### Parametres

| Parametre | Type | Default | Description |
|-----------|------|---------|-------------|
| `inactiveDays` | int | `30` | Seuil en jours pour identifier le stock mort |

### Reponse JSON

```json
{
  "generatedAt": "2026-06-22T15:00:00Z",
  "totalValue": 12500000,
  "totalItems": 45,
  "totalLocations": 3,

  "byCategory": [
    {
      "categoryCode": "CAT-FOURNITURES",
      "categoryName": "Fournitures de bureau",
      "itemCount": 18,
      "totalQuantity": 450,
      "totalValue": 3200000,
      "percentage": 25.6
    }
  ],

  "byLocation": [
    {
      "locationCode": "LOC-STOCK-A",
      "locationName": "Stock principal",
      "locationType": "WAREHOUSE",
      "itemCount": 30,
      "totalQuantity": 800,
      "totalValue": 8500000,
      "percentage": 68.0
    }
  ],

  "lowStockItems": [
    {
      "itemCode": "ITM-000023",
      "itemName": "Ramettes papier A4",
      "categoryName": "Fournitures de bureau",
      "quantityAvailable": 5,
      "reorderPoint": 20,
      "reorderQuantity": 50
    }
  ],

  "deadStockItems": [
    {
      "itemCode": "ITM-000041",
      "itemName": "Cartouches encre HP",
      "categoryName": "Consommables",
      "locationName": "Stock principal",
      "quantityOnHand": 25,
      "stockValue": 375000,
      "lastMovementDate": "2026-04-15",
      "daysSinceLastMovement": 68
    }
  ]
}
```

### Sections

| Section | Description | Action frontend |
|---------|-------------|----------------|
| `byCategory` | Repartition de la valeur stock par categorie | Diagramme camembert |
| `byLocation` | Repartition par emplacement | Diagramme barres |
| `lowStockItems` | Articles sous le point de reapprovisionnement | Alerte rouge, lien commande |
| `deadStockItems` | Articles sans mouvement depuis N jours | Alerte orange, suggestion destockage |

### Export PDF

`rapport-valorisation-stock.pdf`

---

## 6. Analytics abonnements

Tendance MRR sur 12 mois, repartition par plan, taux de churn, renouvellements a venir.

### Endpoints

```
GET /sni/api/v1/reports/subscriptions
GET /sni/api/v1/reports/subscriptions/pdf
```

### Parametres

| Parametre | Type | Default | Description |
|-----------|------|---------|-------------|
| `months` | int | `12` | Nombre de mois pour les tendances MRR et churn |
| `renewalDays` | int | `30` | Horizon des renouvellements a venir (en jours) |

### Reponse JSON

```json
{
  "generatedAt": "2026-06-22T15:00:00Z",
  "totalActive": 45,
  "currentMrr": 3375000,

  "mrrTrend": [
    { "month": "2025-07-01", "mrr": 2100000, "activeCount": 28 },
    { "month": "2025-08-01", "mrr": 2250000, "activeCount": 30 },
    { "month": "2026-06-01", "mrr": 3375000, "activeCount": 45 }
  ],

  "activeByPlan": [
    {
      "planName": "Premium Cowork",
      "subscriptionCount": 20,
      "totalAmount": 1500000,
      "percentage": 44.4
    },
    {
      "planName": "Basic Desk",
      "subscriptionCount": 15,
      "totalAmount": 750000,
      "percentage": 22.2
    }
  ],

  "churnTrend": [
    { "month": "2025-07-01", "cancelledCount": 2, "totalCount": 28, "churnRate": 7.1 },
    { "month": "2025-08-01", "cancelledCount": 1, "totalCount": 30, "churnRate": 3.3 },
    { "month": "2026-06-01", "cancelledCount": 0, "totalCount": 45, "churnRate": 0.0 }
  ],

  "upcomingRenewals": [
    {
      "subscriptionNumber": "SUB-000045",
      "subscriberCode": "MBR-000012",
      "subscriberType": "MEMBER",
      "planName": "Premium Cowork",
      "totalAmount": 75000,
      "currency": "XAF",
      "currentPeriodEnd": "2026-07-15",
      "daysUntilRenewal": 23
    }
  ],

  "revenueByPlan": [
    {
      "planName": "Premium Cowork",
      "subscriptionCount": 20,
      "totalRevenue": 1500000,
      "percentage": 44.4
    }
  ]
}
```

### KPIs principaux

| KPI | Champ | Description |
|-----|-------|-------------|
| MRR actuel | `currentMrr` | Revenu mensuel recurrent normalise |
| Abonnements actifs | `totalActive` | Nombre d'abonnements actifs |
| Taux de churn | `churnTrend[dernier].churnRate` | Pourcentage d'annulations du mois |

### Graphiques recommandes

| Graphique | Donnees | Type |
|-----------|---------|------|
| Evolution MRR | `mrrTrend` | Courbe (line chart) avec axe Y = montant |
| Repartition par plan | `activeByPlan` | Camembert avec `percentage` |
| Taux de churn | `churnTrend` | Barres avec couleur conditionnelle |
| Revenu par plan | `revenueByPlan` | Barres horizontales |

### Churn rate — interpretation

| Churn mensuel | Interpretation | Couleur |
|---------------|----------------|---------|
| 0 - 3% | Excellent | Vert |
| 3 - 7% | Normal | Jaune |
| 7 - 15% | Preoccupant | Orange |
| > 15% | Critique | Rouge |

### Export PDF

`rapport-abonnements.pdf`

---

## 7. Rappel — Rapports financiers existants

Les rapports financiers et d'occupation existants sont documentes en detail dans `reporting-frontend-integration.md`. Resume rapide :

| Endpoint | Description |
|----------|-------------|
| `GET /reports/finance/dashboard` | KPIs financiers globaux |
| `GET /reports/finance/payments` | Repartition paiements par methode + serie quotidienne |
| `GET /reports/finance/aging` | Aging des factures (5 tranches) |
| `GET /reports/finance/cash-registers` | Resume par caisse enregistreuse |
| `GET /reports/finance/cash-flow` | Flux de tresorerie quotidien |
| `GET /reports/finance/dashboard/pdf` | Export PDF du dashboard financier |
| `GET /reports/occupancy` | Taux d'occupation par ressource |
| `GET /reports/occupancy/pdf` | Export PDF occupation |

---

## 8. Resume des endpoints

### Tous les endpoints reporting

| # | Endpoint | Formats | Description |
|---|----------|---------|-------------|
| 1 | `/reports/finance/dashboard` | JSON, PDF | Dashboard financier |
| 2 | `/reports/finance/payments` | JSON | Paiements par methode |
| 3 | `/reports/finance/aging` | JSON | Aging factures |
| 4 | `/reports/finance/cash-registers` | JSON | Caisses enregistreuses |
| 5 | `/reports/finance/cash-flow` | JSON | Cash flow |
| 6 | `/reports/occupancy` | JSON, PDF | Occupation ressources |
| 7 | `/reports/activity` | JSON, PDF, CSV | Activite consolidee |
| 8 | `/reports/member-profile/{id}` | JSON, PDF | Fiche membre 360 |
| 9 | `/reports/debt-recovery` | JSON, PDF, CSV | Recouvrement impayes |
| 10 | `/reports/resource-utilization` | JSON, PDF | Utilisation ressources |
| 11 | `/reports/stock-valuation` | JSON, PDF | Valorisation stock |
| 12 | `/reports/subscriptions` | JSON, PDF | Analytics abonnements |

**Total : 12 rapports, 20 endpoints** (12 JSON + 6 PDF + 2 CSV)

### Alias

Tous les endpoints sous `/reports/` sont aussi accessibles sous `/reporting/` (ex: `/reporting/activity` = `/reports/activity`).