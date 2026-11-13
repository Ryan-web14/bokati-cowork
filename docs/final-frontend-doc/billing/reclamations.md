# Réclamations (lot C11)

> Rédigé d'après le code (compléments de facturation, lots C0 à C16, 13/11/2026). Routes exactes et champs JSON : [routes.md](routes.md) et `routes/`.

- `POST /billing/documents/{n}/claims` : `{"reasonCode": "ERREUR_PRIX", "description", "lineOrders": [1], "requestedAmount": 50000}`. Une seule réclamation ouverte par facture (`RECLAMATION_EN_COURS`).
- Tant qu'elle est ouverte, **les relances de la facture sont suspendues**.
- `PATCH /billing/claims/{n}/accept` (`{"grantedAmount", "comment"}`) : avoir du montant accordé (par défaut le montant demandé), appliqué sur le reste dû s'il le couvre ; `PARTIALLY_ACCEPTED` si moins que demandé. `PATCH .../reject` (`{"comment"}` obligatoire). `PATCH .../withdraw`.
- `GET /billing/claims?documentNumber=&customerType=&customerCode=&openOnly=`, `GET /billing/claims/{n}`.
- Motifs : `GET /billing/claim-reasons`, `PUT /billing/claim-reasons` (ajout ou renommage, droit `BILLING:CATALOG_REFERENCES`).
- Statuts : `OPEN`, `ACCEPTED`, `PARTIALLY_ACCEPTED`, `REJECTED`, `WITHDRAWN`. Canal : `STAFF` ou `PORTAL` (espace client, voir [portal](../portal/README.md)).
- Codes : `RECLAMATION_IMPOSSIBLE`, `RECLAMATION_EN_COURS`, `RECLAMATION_MONTANT`, `RECLAMATION_CLOSE`, `REFERENCE_INCONNUE`, `MOTIF_REQUIS`.
