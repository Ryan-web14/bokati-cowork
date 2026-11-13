# billing : acomptes, du devis à la facture (lot B3)

> Retour au [README du module](README.md). Toutes les routes sont sous `/sni/api/v1/billing`.

Un devis (à tout moment avant sa conversion) ou une facture **encore en brouillon** porte un **plan d'acomptes** : un ou plusieurs acomptes, chacun en montant ou en pourcentage, du total ou de lignes choisies, avec son échéance. Chaque acompte donne une **facture d'acompte** numérotée, encaissable par tous les moyens (caisse, mobile money, virement, portefeuille). La facture finale déduit chaque acompte encaissé et ne réclame que le reste.

## Créer un plan d'acomptes

`POST /quotes/{quoteNumber}/deposits` ou `POST /invoices/{invoiceNumber}/deposits` :

```json
{
  "deposits": [
    { "label": "À la commande", "mode": "PERCENTAGE", "value": 30, "dueDate": "2026-11-20" },
    { "label": "À mi-parcours", "mode": "PERCENTAGE", "value": 20, "lineOrders": [1, 2], "dueDate": "2026-12-15", "invoiceNow": false }
  ]
}
```

- `mode` : `AMOUNT` (montant TTC) ou `PERCENTAGE` (100 au plus). `lineOrders` : base du pourcentage (lignes choisies) ; vide : le total TTC.
- `invoiceNow` : vrai par défaut, la facture d'acompte est créée et émise aussitôt ; faux : l'acompte est prévu (`PLANNED`) et se facture plus tard par `PATCH /deposits/{id}/invoice`.
- Les acomptes (prévus, facturés, encaissés, déduits) ne dépassent pas le total de la pièce.
- Facture : seulement en brouillon. Une facture émise se règle par échéancier ([états clients](etats-clients.md#échéanciers)).
- Devis refusé, expiré, converti ou annulé : refus.
- Un devis `ACCEPTED` passe à `DEPOSIT_REQUESTED` dès qu'un acompte est facturé, puis à `DEPOSIT_PAID` quand tous les acomptes facturés sont encaissés.

Réponse : la liste des acomptes créés (`DepositResponse`).

## Suivre les acomptes

`GET /documents/{documentNumber}/deposits` : acomptes du devis ou de la facture, y compris ceux hérités du devis par la facture finale.

| Champ | Contenu |
| --- | --- |
| `id` | Identifiant de l'acompte (pour les actions) |
| `rank` | Acompte n° 1, 2... |
| `mode`, `value`, `baseLineOrders`, `amount` | Saisie et montant calculé |
| `status` | `PLANNED`, `INVOICED`, `PARTIALLY_PAID`, `PAID`, `DEDUCTED`, `CANCELLED`, `REFUNDED`, `CREDITED` |
| `depositInvoiceNumber` | Facture d'acompte (lien vers sa fiche, son PDF, son encaissement) |
| `paidAmount`, `deductedAmount`, `deductedOn`, `deductedAt` | Encaissé, déduit, sur quelle facture finale |
| `settlementDocument` | Avoir qui solde un acompte remboursé ou gardé en avoir |

La facture d'acompte se paie comme toute facture (`POST /invoices/{n}/pay` ou les parcours du module payment). Son PDF est titré « FACTURE D'ACOMPTE » ; sa ligne n'a pas de TVA (la TVA est portée par la facture finale, décision D3 validée).

## Facture finale

- La conversion du devis (`POST /quotes/{n}/convert-to-invoice`) rattache ses acomptes à la facture créée.
- À l'**émission** de la facture finale (`PATCH /documents/{n}/issue`) : chaque acompte encaissé est déduit (imputé sur la facture), l'acompte passe à `DEDUCTED` ; un acompte facturé mais non réglé tombe (`CANCELLED`, sa facture d'acompte est annulée), la facture réclamant le tout.
- PDF de la facture finale : une ligne « Acompte n° 1 du 13/11/2026 (FAC-...) » par acompte déduit, puis « Montant réglé (acomptes compris) » et « Reste dû ».

Exemple : devis de 1 000 000, acomptes de 30 % et 20 % encaissés ; la facture finale affiche 1 000 000, déduit 500 000 et réclame 500 000.

## Retirer un acompte

`PATCH /deposits/{id}/cancel?reason=...` : seulement s'il n'est pas encaissé ; sa facture d'acompte est annulée.

## Devis refusé ou expiré

Les acomptes non encaissés tombent avec le devis. Un devis qui porte un acompte encaissé **n'expire pas** ; pour un devis refusé ou annulé avec un acompte encaissé (décision D4) :

`PATCH /deposits/{id}/settle` avec `{ "mode": "REFUND", "reason": "..." }` (avoir reversé au portefeuille du client, statut `REFUNDED`) ou `{ "mode": "CREDIT", "reason": "..." }` (avoir gardé pour le client, statut `CREDITED`). Le devis peut aussi être réactivé (`PATCH /quotes/{n}/revalidate`).

## Comptabilité

La facture d'acompte crédite le compte `4191` (clients, avances et acomptes reçus) ; la déduction sur la facture finale solde le `4191` contre le `411` (paramètre `accounting.sales.deposit-account`). En régime des encaissements, la TVA en attente de la facture finale devient exigible au fur et à mesure que les acomptes sont déduits de la facture finale, pas à leur encaissement (décision D3, validée le 13/11/2026).
