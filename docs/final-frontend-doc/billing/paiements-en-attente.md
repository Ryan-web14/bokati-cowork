# Paiements non affectés (lot C9)

> Rédigé d'après le code (compléments de facturation, lots C0 à C16, 13/11/2026). Routes exactes et champs JSON : [routes.md](routes.md) et `routes/`.

- `GET /billing/unallocated-payments/suggestions?customerType=&customerCode=&limit=` : paiements dont une partie n'est imputée à aucune facture (hors portefeuille), avec `pending` et des `candidates` : factures ouvertes du même client, `score` sur 100, `reasons` (« même montant », « numéro dans la référence du paiement », « échéance proche »), `proposedAmount`.
- `POST /billing/unallocated-payments/{transactionNumber}/allocate` : `{"allocations": [{"documentNumber": "FAC-50", "amount": 590000}]}` ; renvoie `allocated`, `stillPending`. Chaque montant ne dépasse ni le reste dû ni l'attente.
- Codes : `AFFECTATION_INVALIDE` (autre client), `AFFECTATION_MONTANT`.
- Depuis le 14/11/2026 (lots R1 à R4) : l'affectation déclenche les effets sur l'élément que la facture règle (activation, confirmation), accepte `elements` pour viser un abonnement, un pass, une réservation ou une domiciliation, refuse un paiement non réussi (`PAIEMENT_NON_REUSSI`) et peut être annulée. Voir [rattachement-paiements.md](rattachement-paiements.md).
