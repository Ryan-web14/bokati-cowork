# Devis types et lots d'articles (lot C14)

> Rédigé d'après le code (compléments de facturation, lots C0 à C16, 13/11/2026). Routes exactes et champs JSON : [routes.md](routes.md) et `routes/`.

## Modèles de devis

- `PUT /billing/quote-templates` : `{"code": "SEMINAIRE", "name": "Séminaire journée", "validityDays": 15, "terms", "lines": [{"itemCode": "SRV-SALLE", "quantity": 1, "description": null}], "active": true}` (création ou remplacement).
- `GET /billing/quote-templates`, `GET /billing/quote-templates/{code}`.
- `POST /billing/quotes/from-template/{code}` `{"customerType", "customerCode", "customerName", "customerEmail", "customerPhone", "segment", "title"}` : devis en brouillon, aux prix du client (contrat, segment, fiche), à ajuster puis émettre comme tout devis.

## Articles composés

- `PUT /billing/catalog/{itemCode}/components` `{"components": [{"componentCode": "ART-KIT", "componentSource": "INVENTORY_ITEM", "quantity": 2}]}` ; liste vide : article simple. `GET` pour relire.
- Vendu à prix global ; la comptabilité ventile le produit entre les composants ; le stock des composants d'inventaire sort à la facture ou au bon de livraison.
- Codes : `MODELE_INACTIF`, `COMPOSANT_INVALIDE`.
