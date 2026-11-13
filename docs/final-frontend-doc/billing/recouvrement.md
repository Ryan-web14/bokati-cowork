# Recouvrement (lot C12)

> Rédigé d'après le code (compléments de facturation, lots C0 à C16, 13/11/2026). Routes exactes et champs JSON : [routes.md](routes.md) et `routes/`.

- Promesse de paiement : `POST /billing/documents/{n}/payment-promises` `{"promisedDate", "amount", "channel": "PHONE"|"EMAIL"|"WHATSAPP"|"VISIT"|"PORTAL", "note"}`. Les relances attendent la date ; ensuite la promesse est `KEPT` (le reste dû a baissé du montant promis) ou `BROKEN`, et les relances reprennent. Une nouvelle promesse remplace celle en cours. `GET /billing/payment-promises?documentNumber=&openOnly=`, `PATCH /billing/payment-promises/{n}/cancel`.
- Plan de règlement : `POST /billing/documents/{n}/payment-plan` `{"installments": 3, "firstDueDate", "intervalMonths": 1, "notes"}` : le reste dû en échéances égales, échéancier de la facture (voir [états clients](etats-clients.md#échéanciers)). Au-delà de 3 mois (paramètre `billing.collection.plan-approval-months`), 403 sans le droit d'approbation de la facturation.
- Relances : nouveau canal `WHATSAPP` pour un palier (modèle `rappel_paiement` au numéro de la facture). Un palier sauté (réclamation en cours, promesse non échue) est noté avec son motif.
- Codes : `PROMESSE_INVALIDE`, `PROMESSE_CLOSE`, `RECOUVREMENT_IMPOSSIBLE`.
