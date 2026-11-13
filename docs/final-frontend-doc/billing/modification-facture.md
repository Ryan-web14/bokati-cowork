# billing : modifier une facture non payée (lot B14)

> Retour au [README du module](README.md). Toutes les routes sont sous `/sni/api/v1/billing`. Circuit d'approbation : [module approval](../approval/README.md).

Une facture se modifie **tant qu'aucun paiement n'est imputé**. Après le premier paiement, la correction passe par un avoir ou une facture rectificative. Rien n'est supprimé : chaque modification crée une version.

## Demander une modification

`POST /invoices/{invoiceNumber}/change-requests` :

```json
{
  "reason": "Remise commerciale accordée par la direction",
  "discountOverrideReason": null,
  "changes": { "lines": [ { "action": "UPSERT", "lineOrder": 1, "description": "Bureau", "quantity": 1, "unitPrice": 80000 } ], "dueDate": "2026-12-31" }
}
```

- `changes` : même forme que `PUT /documents/{n}` (lignes par `lineOrder`, `action` `UPSERT` ou `REMOVE`, remises, clauses, échéance, conditions).
- **Brouillon** : la modification s'applique aussitôt (statut `APPLIED`).
- **Facture émise** : la demande part au **centre d'approbation de la facturation** (circuit `INVOICE_CHANGE`, montant = nouveau total ; par défaut N+1, puis N+2 au-delà de 500 000). Statut `PENDING` ; la facture garde sa version en vigueur jusqu'à l'accord.
- Refus : facture payée même en partie, annulée, remplacée, réglée ; type autre que facture.
- Une nouvelle demande remplace celle encore en attente.

Réponse `InvoiceChangeResponse` : `id`, `status` (`PENDING`, `APPLIED`, `REJECTED`, `RETURNED`, `CANCELLED`), `totalBefore`, `totalAfter`, `approvalNumber` (demande d'approbation), `decidedAt`, `decidedBy`, `decisionComment`, `result`, `resultDocumentNumber`, `versionNumber`.

## Décision

L'approbateur décide dans le centre de la facturation : `PATCH /sni/api/v1/approvals/{approvalNumber}/approve`, `/reject` ou `/send-back` ([approval](../approval/README.md)). À l'accord du dernier niveau :

- facture **ni certifiée ni comptabilisée** : nouvelle version de la même pièce (`result = NEW_VERSION`, `versionNumber`), auteur de la version = le demandeur, motif gardé ; la version précédente reste consultable (`GET /documents/{n}/versions`) ;
- facture **certifiée ou déjà comptabilisée** (décision D8) : une **facture rectificative** reprend la facture modifiée (émise, et certifiée si l'originale l'était), présentée comme sa nouvelle version (`result = CORRECTIVE_INVOICE`, `resultDocumentNumber`) ; la facture d'origine est annulée (avoir si elle était certifiée), ce qui corrige les écritures par contre-passation et nouvelle pièce.

Si un paiement arrive entre la demande et l'accord, l'accord est refusé (`409`) : passer par un avoir.

## Suivre

- `GET /invoices/{invoiceNumber}/change-requests` : historique des demandes.
- `PATCH /invoices/change-requests/{id}/cancel` : retrait par le demandeur d'une demande en attente.

Écran conseillé : sur la fiche d'une facture émise non payée, un bouton « Demander une modification » qui ouvre l'éditeur de lignes ; un bandeau « Modification en attente d'approbation (APR-...) » tant que la demande est `PENDING` ; l'onglet Versions montre les versions et, le cas échéant, le lien vers la rectificative.
