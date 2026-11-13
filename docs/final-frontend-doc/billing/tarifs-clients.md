# Tarifs négociés par client (lot C8)

> Rédigé d'après le code (compléments de facturation, lots C0 à C16, 13/11/2026). Routes exactes et champs JSON : [routes.md](routes.md) et `routes/`.

- `GET /billing/customers/{type}/{code}/prices?current=true`, `POST` (`itemCode`, `unitPrice` **ou** `discountRate`, `validFrom`, `validUntil`, `contractReference`, `reason`), `PATCH .../{id}/end` (`{"validUntil"}`).
- Un nouveau tarif du même article arrête le précédent la veille ; un tarif qui commencerait le même jour qu'un autre est refusé (`TARIF_INVALIDE`).
- Prix applicable à un client : `GET /billing/catalog/{itemCode}/price?customerType=&customerCode=&segment=&date=`. Ordre : contrat du client, prix du segment, prix de la fiche. `source` vaut `CONTRACT` quand le contrat s'applique (`reason` = référence du contrat) ; un taux négocié s'applique au prix du segment ou de la fiche.
- À l'écran : appeler cette route en choisissant l'article d'une ligne, une fois le client connu, et afficher la source du prix.
