# billing : affaires (lot B11)

> Retour au [README du module](README.md). Routes : `/sni/api/v1/billing/deals` ([routes/DealController.md](routes/DealController.md)).

Une affaire regroupe tout ce qui concerne une vente à un client : devis et leurs versions, proformas, acomptes, factures, avoirs, paiements.

## Fiche

`POST /billing/deals`, `PUT /billing/deals/{code}` :

```json
{
  "customerType": "CUSTOMER", "customerCode": "CUS-...", "customerName": null,
  "title": "Aménagement du plateau", "owner": "awa@elleaose.com",
  "expectedAmount": 2500000, "currency": "XAF", "probability": 60,
  "expectedCloseDate": "2027-01-31", "analyticSection": "AFF-PLATEAU", "notes": "..."
}
```

- Client connu (`customerType` + `customerCode`) ou prospect (`customerName` seul).
- `owner` : responsable commercial ; vide : l'utilisateur connecté.
- `probability` : 0 à 100. `analyticSection` : section analytique (comptabilité, lot F9).
- Code attribué par le serveur : `AFF-AAAA-NNNNN`.

Étape : `PATCH /billing/deals/{code}/stage` avec `{ "stage": "LOST", "lostReason": "Budget reporté" }`. Étapes : `PROSPECTING`, `PROPOSAL`, `NEGOTIATION`, `WON`, `LOST` (motif obligatoire). `WON` et `LOST` datent la clôture (`closedAt`).

## Création automatique et rattachement

- Depuis une opportunité du CRM : `POST /billing/deals/from-opportunity/{opportunityNumber}` (client converti, montant, probabilité, échéance repris ; étape `PROPOSAL`, source `CRM`).
- Au **premier devis** d'un client sans affaire ouverte : une affaire est créée (source `AUTO`) ; avec une seule affaire ouverte, le devis la rejoint. Paramètre `billing.deal.auto-create`.
- Chaque pièce dérivée rejoint l'affaire de sa pièce d'origine : facture issue du devis, facture d'acompte, avoir, rectificative, facture d'un changement de client.
- Rattacher ou détacher à la main : `PATCH /billing/deals/{code}/documents/{documentNumber}`, `DELETE /billing/deals/{code}/documents/{documentNumber}`.

## Tableau de l'affaire

`GET /billing/deals/{code}` renvoie `DealDashboardResponse` :

| Champ | Contenu |
| --- | --- |
| `deal` | La fiche |
| `quoted` | Total des devis (hors annulés) |
| `accepted` | Total des devis acceptés (acceptés, acompte demandé ou encaissé, convertis) |
| `invoiced`, `creditNotes`, `invoicedNet` | Facturé (hors factures d'acompte), avoirs, net |
| `depositsInvoiced`, `depositsPaid` | Factures d'acompte, encaissé sur elles |
| `collected` | Encaissé, sans compter deux fois un acompte déduit de la facture finale |
| `remainingToInvoice` | Accepté moins facturé net (jamais négatif) |
| `remainingToCollect` | Soldes des créances de l'affaire |
| `plannedMargin`, `realizedMargin`, `costedShare` | Marges prévue (devis acceptés) et réalisée (factures), HT moins coût des articles connus ; `costedShare` : part du HT dont le coût est connu |
| `documents[]` | Les pièces de l'affaire (numéro, type, statut, acompte ou non, date, montants) |

## Liste et export

- `GET /billing/deals?stage=&owner=&customerCode=&search=&from=&to=&page=&size=` (pagination standard).
- `GET /billing/deals/export` avec les mêmes filtres : CSV séparé par des points-virgules.
