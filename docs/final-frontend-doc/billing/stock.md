# billing : devis, factures et inventaire (lots B7, B13)

> Retour au [README du module](README.md). Toutes les routes sont sous `/sni/api/v1/billing`.

Une ligne vise un article d'inventaire quand son `lineType` est `PRODUCT`, sa provenance (`sourceType`) contient `INVENTORY`, ou que son article du catalogue est lié à un article d'inventaire ([catalogue](catalogue.md)). Seuls les articles suivis en stock sont contrôlés (pas les services, pas le suivi `NONE`).

## Vérifier avant d'enregistrer

`POST /documents/check-availability` :

```json
{
  "documentType": "QUOTE",
  "lines": [ { "description": "Chaise", "itemCode": "ART-CHAISE", "lineType": "PRODUCT", "quantity": 12, "unitPrice": 15000 } ],
  "resources": [ { "lineIndex": 1, "resourceCode": "RES-SALLE-A", "startAt": "2026-12-01T09:00:00", "endAt": "2026-12-01T12:00:00", "quantity": 1 } ]
}
```

Réponse `CheckAvailabilityResponse` :
- `stock[]` : par ligne d'article, `requested`, `available` (en stock moins réservé), `missing`, `active`, `negativeStockAllowed` ;
- `resources[]` : disponibilité de chaque ressource aux dates demandées (`available`, `remainingCapacity`, `message`) ;
- `alerts[]` (mêmes codes que la simulation) et `blocked`.

Les mêmes alertes de stock arrivent dans `POST /documents/simulate` et `simulate-line`.

## Règles (décision D5)

- **Devis ou proforma** : stock insuffisant = alerte précise (« 12 demandé(s), 10 disponible(s), 2 manquant(s) »).
- **Facture** : l'émission est refusée tant qu'il manque du stock, sauf si l'article autorise le stock négatif (réglage de l'article d'inventaire).
- Article inactif : alerte `INACTIVE_ITEM` (`BLOCK`).

## Réservation et sortie

- À l'**acceptation** d'un devis (au guichet, par signature, ou proforma transformée) : le stock est réservé dans la limite du disponible (12 demandées, 10 disponibles : 10 réservées).
- **Refus, expiration, annulation ou révision** d'un devis accepté : réservations libérées.
- À l'**émission de la facture** : les réservations du devis sont consommées, le reste sort du stock ; le coût sort au `603` par le module de stock. Paramètre `billing.stock.issue-on` : `INVOICE` (défaut) ou `NONE` (aucune sortie automatique).
