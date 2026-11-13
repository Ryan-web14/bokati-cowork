# billing : catalogue (lot B10)

> Retour au [README du module](README.md). Routes : `/sni/api/v1/billing/catalog` ([routes/ServiceCatalogController.md](routes/ServiceCatalogController.md)).

Un article du catalogue (`itemCode`) porte prix, unité, coût, plancher, marge minimale, remise maximale, politique de remise. **Familles et sous-familles** : champs `category` et `subcategory`.

## Prix par segment et prix datés

- `POST /catalog/{itemCode}/prices` avec `{ "segment": "MEMBER", "unitPrice": 120000, "validFrom": "2027-01-01", "reason": "Hausse annuelle" }`. Segments : `ALL` (défaut), `MEMBER`, `COMPANY`, `VISITOR`. Le prix en vigueur du même segment s'arrête la veille ; **rien n'est effacé**. Un prix `ALL` en vigueur aujourd'hui devient aussi le prix de la fiche.
- `GET /catalog/{itemCode}/prices` : historique des prix.
- `GET /catalog/{itemCode}/price?segment=MEMBER&date=2026-12-15` : prix applicable ; `source` vaut `SEGMENT` (prix du segment), `ALL` (prix daté commun) ou `CATALOG` (prix de la fiche, à défaut).
- Modifier le prix de la fiche (`PUT /catalog/{itemCode}`) l'inscrit dans l'historique.

## Lien vers l'inventaire et compte de produit

`PUT /catalog/{itemCode}/links` avec `{ "inventoryItemCode": "ART-CHAISE", "revenueAccount": "7061" }` (vide : retire) :
- l'article d'inventaire lié fait contrôler, réserver et sortir le stock des lignes de cet article ([stock](stock.md)) ;
- le compte de produit propre passe avant le compte par type de ligne dans les écritures de vente.

## Marges, import et export

- `GET /catalog/margins?search=&category=` : prix, coût, marge, taux de marge, plancher, remise maximale, liens.
- `GET /catalog/export` : CSV (séparateur point-virgule), colonnes `itemCode;name;category;subcategory;unit;unitPrice;currency;costPrice;floorPrice;maxDiscountRate;inventoryItemCode;revenueAccount;active`.
- `POST /catalog/import` (corps `text/csv`, même en-tête) : met à jour les articles existants par leur code, crée les autres ; réponse `created`, `updated`, `errors` (une erreur par ligne, « Ligne N : ... »).
