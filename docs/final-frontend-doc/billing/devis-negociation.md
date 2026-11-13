# billing : négociation et révision des devis (lots B0, B1, B2)

> Retour au [README du module](README.md). Toutes les routes sont sous `/sni/api/v1/billing`.

## Réviser un devis

Un devis se modifie tant qu'il n'est pas converti : prix, quantités, remises, lignes ajoutées ou retirées, conditions. Chaque révision crée une **version** ; rien n'est effacé.

`PUT /quotes/{quoteNumber}` ([détail](routes/BillingDocumentController.md)) :

```json
{
  "reason": "Le client demande 10 % de moins sur la salle",
  "origin": "CLIENT_PROPOSAL",
  "discountOverrideReason": null,
  "changes": { "lines": [ { "action": "UPSERT", "lineOrder": 1, "description": "Salle", "quantity": 2, "unitPrice": 45000 } ] }
}
```

- `reason` : obligatoire (1 000 caractères au plus).
- `origin` : `CLIENT_PROPOSAL` (proposition du client), `COUNTER_PROPOSAL` (contre-proposition), `INTERNAL` (défaut).
- `changes` : même forme que `PUT /documents/{n}` (`UpdateBillingDocumentRequest`) ; une ligne se modifie ou s'ajoute par `action: "UPSERT"` et son `lineOrder`, se retire par `action: "REMOVE"`.
- Statuts admis : `DRAFT`, `ISSUED`, `SENT`, `VIEWED`, `NEGOTIATION`, `ACCEPTED`. Un devis `EXPIRED` se réactive d'abord ; `CONVERTED`, `REJECTED`, `CANCELLED` : refus.
- Effets (hors brouillon) :
  - le devis passe en `NEGOTIATION` ;
  - **devis accepté** (décision D1) : il repasse en négociation, ses signatures (en attente ou obtenues) passent à `CANCELLED`, son stock réservé est libéré ; le client doit accepter de nouveau ;
  - prix plancher et remise maximale du catalogue contrôlés : au-delà, `400` sauf `discountOverrideReason` et le droit de dérogation ;
  - la date de validité (`dueDate`) est portée à au moins aujourd'hui plus `billing.quote.revision-validity-days` jours (30 par défaut).
- La version courante porte son auteur (bloc `author` de la réponse), son motif et son origine.

`PUT /documents/{n}` sur un devis envoyé, vu, en négociation ou accepté fait la même chose avec le motif « Modification du devis ».

## Frise de négociation

`GET /quotes/{quoteNumber}/negotiation` renvoie `QuoteNegotiationResponse` :

| Champ | Contenu |
| --- | --- |
| `currentRevision` | Numéro de la version courante |
| `rounds[]` | Une entrée par version, la courante en dernier (`current = true`) |
| `rounds[].authorName`, `reason`, `origin`, `status` | Qui, pourquoi, d'où vient la proposition, statut à ce moment |
| `rounds[].changedAt`, `sentToCustomerAt` | Date de la version, date de transmission au client |
| `rounds[].subtotalAmount`, `discountAmount`, `totalAmount` | Montants de la version |
| `rounds[].totalDelta` | Écart de total TTC avec la version précédente (vide pour la première) |
| `rounds[].lineChanges[]` | `change` (`ADDED`, `REMOVED`, `CHANGED`), `description`, valeurs avant et après de quantité, prix unitaire, remise, total |

Écran conseillé : une frise verticale, chaque version avec son auteur et son motif, l'écart de prix en couleur, les lignes changées dépliables. Le détail complet d'une version : `GET /documents/{n}/versions/{versionNumber}` ; la comparaison champ par champ : `GET /documents/{n}/versions/{from}/diff/{to}`.

## Réactiver un devis expiré (décision D4)

`PATCH /quotes/{quoteNumber}/revalidate` avec `{ "validUntil": "2026-12-31", "reason": "Le client confirme son intérêt" }`. Seul un devis `EXPIRED` ; la date est postérieure à aujourd'hui. Le devis repasse à `SENT`, une version est archivée.

## Signature électronique (lot B0)

- `POST /quotes/{n}/request-signature` : statuts `DRAFT`, `ISSUED`, `SENT`, `VIEWED`, `NEGOTIATION`, `ACCEPTED`. La demande retient la version du devis présentée.
- Page publique : `POST /public/quotes/sign/{token}/confirm` répond `PublicSignatureConfirmation` (numéro, date, montant, devise, état de la signature, signataire, date, version). Plus aucune note interne ni ligne.
- `409` si le devis est sorti du circuit (annulé, refusé, converti) ou a été **révisé** depuis la demande (« une nouvelle demande est nécessaire ») : la demande passe à `CANCELLED`.
- Une signature confirmée accepte le devis et réserve son stock.

## Expiration

Chaque heure, un devis `ISSUED`, `SENT`, `VIEWED`, `NEGOTIATION` ou `DEPOSIT_REQUESTED` dont la `dueDate` est passée passe à `EXPIRED`, sauf s'il porte un acompte encaissé. Ses acomptes non encaissés sont annulés et son stock réservé libéré.
