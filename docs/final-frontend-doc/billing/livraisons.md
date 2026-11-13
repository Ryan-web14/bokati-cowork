# Bons de livraison (lot C4)

> Rédigé d'après le code (compléments de facturation, lots C0 à C16, 13/11/2026). Routes exactes et champs JSON : [routes.md](routes.md) et `routes/`.

## Parcours

1. Depuis un devis accepté (aussi après conversion) ou une facture émise : `POST /billing/documents/{n}/delivery-notes` avec `{"lines": [{"lineOrder": 1, "quantity": 3}], "deliveryAddress", "notes", "showPrices"}`. Sans `lines`, tout le reste à livrer. Le bon naît en `DRAFT`.
2. `GET /billing/documents/{n}/delivery-status` : par ligne `ordered`, `delivered`, `inDraft`, `remaining` ; `complete` ; liste des bons.
3. `PATCH /billing/delivery-notes/{n}/deliver` (`{"receivedBy": "M. Okemba"}`) : `DELIVERED`. Le stock sort à ce moment si le paramètre `billing.stock.issue-on` vaut `DELIVERY` (`stockIssued: true`).
4. `PATCH /billing/delivery-notes/{n}/signed-copy` (`{"reference": "..."}`) : référence de la copie signée déposée en GED.
5. Bons livrés d'un même devis : `POST /billing/delivery-notes/invoice` (`{"noteNumbers": [...]}`) crée une facture en brouillon aux quantités livrées ; les bons passent `INVOICED`.

`PATCH /billing/delivery-notes/{n}/cancel` (`{"reason"}` obligatoire) : un bon livré rend sa marchandise au stock ; un bon facturé ne s'annule pas (avoir sur la facture).

Liste : `GET /billing/delivery-notes?customerType=&customerCode=&status=` ; détail `GET /billing/delivery-notes/{n}` ; PDF `GET .../pdf` (sans prix sauf `showPrices`).

Statuts : `DRAFT`, `DELIVERED`, `CANCELLED`, `INVOICED`. Codes : `LIVRAISON_IMPOSSIBLE`, `LIVRAISON_INVALIDE`, `LIVRAISON_AU_DELA_DU_RESTE`, `RIEN_A_LIVRER`, `LIVRAISON_STATUT`, `LIVRAISON_DEJA_FACTUREE`, `MOTIF_REQUIS`.

Limite : un devis avec acomptes se facture par sa conversion, pas par les bons (les acomptes ne sont pas repris).
