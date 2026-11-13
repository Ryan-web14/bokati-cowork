# billing : statuts et cycle de vie

> Retour au [README du module](README.md). Routes exactes : [routes.md](routes.md). Toutes les routes ci-dessous sont sous `/sni/api/v1/billing`.

Les statuts sont communs (`BillingDocumentStatus`) mais chaque type n'en parcourt qu'une partie. Le statut est **toujours tenu par le serveur** : l'écran ne le déduit pas, il lit `status` et affiche les actions de la table [Actions par état](#actions-par-état).

Une facture **certifiée** est une facture validée par notre système (`locked = true`) : numéro de certification (`fiscalNumber`), empreinte chaînée et signature. Ce n'est pas une certification d'un organisme public : afficher « Facture certifiée par notre système ».

## Règles communes

- **Envoyer ne change jamais le statut** (`PATCH /documents/{n}/send` ou `POST /documents/{n}/send`). L'envoi est daté (`sentAt`) et tracé ([envois](envois.md)). Une facture payée envoyée reste `PAID`. Seule exception : un devis ou une proforma `ISSUED` passe à `SENT`.
- **Un brouillon ne part pas** : `send` sur un `DRAFT` répond `400` (« Un brouillon s'émet avant d'être envoyé »). Émettre d'abord : `PATCH /documents/{n}/issue`.
- **Valider (certifier) exige une pièce émise** : `PATCH /documents/{n}/validate` refuse un `DRAFT` (« Un brouillon s'émet avant d'être validé »). Statuts admis : `ISSUED`, `SENT`, `PARTIALLY_PAID`, `PAID`, `OVERDUE` (et pour un avoir `APPLIED`, `REFUNDED`). La validation **garde** un statut de paiement : une facture `PAID` validée reste `PAID`, seule une pièce `ISSUED` ou `SENT` passe à `VALIDATED`.
- **Une proforma ne se valide pas** (lot B5) : ce n'est pas une pièce fiscale.
- `VOIDED` n'existe plus : un ancien document `VOIDED` est devenu `CANCELLED`.

## Devis (`QUOTE`)

| Statut | Signification | On y arrive par | Suites possibles |
| --- | --- | --- | --- |
| `DRAFT` | Brouillon | création | `SENT`, `ACCEPTED`, `REJECTED`, `CANCELLED` |
| `SENT` | Transmis | `PATCH /documents/{n}/issue` (un devis émis est envoyé) ; `PATCH /quotes/{n}/revalidate` (devis expiré remis en circuit) | `VIEWED`, `NEGOTIATION`, `ACCEPTED`, `REJECTED`, `EXPIRED` |
| `VIEWED` | Ouvert par le client | `PATCH /quotes/{n}/mark-viewed` | `NEGOTIATION`, `ACCEPTED`, `REJECTED`, `EXPIRED` |
| `NEGOTIATION` | En discussion | `PATCH /quotes/{n}/start-negotiation` ; toute révision d'un devis envoyé, vu ou accepté ([négociation](devis-negociation.md)) | `ACCEPTED`, `REJECTED`, `EXPIRED` |
| `ACCEPTED` | Accepté | `PATCH /quotes/{n}/accept` ; signature publique ; proforma transformée ([proforma](proforma.md)) | `DEPOSIT_REQUESTED`, `CONVERTED`, `NEGOTIATION` (révision), `REJECTED` |
| `DEPOSIT_REQUESTED` | Acompte(s) facturé(s), pas tous encaissés | plan d'acomptes sur un devis accepté ([acomptes](acomptes.md)) | `DEPOSIT_PAID`, `CONVERTED`, `REJECTED`, `EXPIRED` |
| `DEPOSIT_PAID` | Acomptes facturés tous encaissés | encaissement des factures d'acompte | `CONVERTED` |
| `CONVERTED` | Transformé en facture | `POST /quotes/{n}/convert-to-invoice` depuis `ACCEPTED`, `DEPOSIT_REQUESTED` ou `DEPOSIT_PAID` | aucune |
| `REJECTED` | Refusé | `PATCH /quotes/{n}/reject` | aucune (acomptes non encaissés annulés ; acompte encaissé : voir [acomptes](acomptes.md#devis-refusé-ou-expiré)) |
| `EXPIRED` | Date de validité (`dueDate`) passée sans réponse | tâche horaire (paramètre `bokati.billing.workers.quote-expiry-delay-ms`) sur les devis `ISSUED`, `SENT`, `VIEWED`, `NEGOTIATION`, `DEPOSIT_REQUESTED` sans acompte encaissé | `SENT` par `PATCH /quotes/{n}/revalidate` |
| `CANCELLED` | Annulé et archivé | `DELETE /documents/{n}` | aucune |

Un devis expiré ou refusé libère son stock réservé ([stock](stock.md)). La tâche d'expiration envoie une alerte WebSocket `BILLING_QUOTE_EXPIRED` sur `/topic/admin/alerts`.

## Facture (`INVOICE`), rectificative (`CORRECTIVE_INVOICE`), note de débit (`DEBIT_NOTE`)

| Statut | Signification | On y arrive par | Suites possibles |
| --- | --- | --- | --- |
| `DRAFT` | Brouillon, pas une créance | création | `ISSUED`, `CANCELLED` |
| `ISSUED` | Émise, non certifiée | `PATCH /documents/{n}/issue` (contrôle des remises, du stock, déduction des acomptes) ; annulation d'un encaissement sur une pièce non certifiée | `VALIDATED`, `PARTIALLY_PAID`, `PAID`, `OVERDUE`, `CANCELLED` |
| `VALIDATED` | Certifiée par notre système | `PATCH /documents/{n}/validate` depuis `ISSUED` ou `SENT` ; annulation d'un encaissement sur une pièce certifiée | `PARTIALLY_PAID`, `PAID`, `OVERDUE`, `CANCELLED` (par avoir) |
| `SENT` | Ancien statut d'envoi | n'est plus posé sur une facture (lot B1) ; des factures anciennes peuvent l'avoir | comme `ISSUED` |
| `PARTIALLY_PAID` | Payée en partie | encaissement, acompte déduit | `PAID`, `OVERDUE` |
| `PAID` | Soldée | encaissement complet | aucune (correction : avoir) |
| `OVERDUE` | Échue avec un solde | tâche horaire | `PARTIALLY_PAID`, `PAID` |
| `WRITTEN_OFF` | Passée en perte | comptabilité (créances irrécouvrables) | aucune |
| `CANCELLED` | Annulée | `DELETE /documents/{n}` ; remplacement par une rectificative ([modification](modification-facture.md)) ou par une facture d'un autre client ([changement de client](changement-client.md)) | aucune |

Une facture remplacée porte `replacedByDocumentNumber` (champ de l'entité, visible dans les événements et le journal) ; la rectificative porte `originalDocumentNumber`.

## Facture d'acompte

Une facture ordinaire (`INVOICE`) qui porte `depositForDocumentNumber` (le devis ou la facture dont elle est un acompte) : même cycle qu'une facture. Elle ne porte pas de TVA (la TVA est sur la facture finale). Voir [acomptes](acomptes.md).

## Avoir (`CREDIT_NOTE`)

| Statut | Signification | On y arrive par |
| --- | --- | --- |
| `DRAFT` | Brouillon, sans valeur | création (`POST /invoices/{n}/credit-note`) |
| `ISSUED` | Émis, **non consommé** (lot B1 : avant, `ISSUED` voulait dire consommé) | émission |
| `VALIDATED` | Certifié, **disponible** pour le client | validation (création avec `validateImmediately`) |
| `APPLIED` | Imputé sur une facture (consommé) | `PATCH /credit-notes/{n}/apply`, `PATCH /credit-notes/{n}/apply-to/{facture}`, création avec `applyImmediately`, transfert lors d'un changement de client |
| `REFUNDED` | Reversé au portefeuille du client (consommé) | `POST /credit-notes/{n}/refund-to-wallet` |
| `CANCELLED` | Annulé | `DELETE /documents/{n}` (un avoir imputé est d'abord retiré de sa facture) |

Seul un avoir `VALIDATED` compte dans `customerImpact` (négatif) et dans `totalCreditAvailable` du relevé.

## Proforma (`PROFORMA_INVOICE`)

`DRAFT`, `SENT` (émise ou envoyée), `EXPIRED` (date de validité passée), `CONVERTED` (transformée en devis). Ni certifiée, ni encaissable, ni créance : voir [proforma](proforma.md).

## Actions par état

| Document | Condition | Actions à proposer |
| --- | --- | --- |
| Tout type | `status = DRAFT` | Modifier, Émettre, Supprimer |
| Devis | `DRAFT`, `ISSUED`, `SENT`, `VIEWED`, `NEGOTIATION`, `ACCEPTED` | Réviser ([négociation](devis-negociation.md)), Demander une signature |
| Devis | `SENT`, `VIEWED`, `NEGOTIATION`, `DRAFT` | Accepter, Refuser |
| Devis | non `CONVERTED`, `REJECTED`, `CANCELLED`, `EXPIRED` | Plan d'acomptes |
| Devis | `ACCEPTED`, `DEPOSIT_REQUESTED`, `DEPOSIT_PAID` | Convertir en facture |
| Devis | `EXPIRED` | Réactiver |
| Devis | tout sauf `CANCELLED` | Établir une proforma |
| Facture | `DRAFT` | Plan d'acomptes, Changer le client |
| Facture | émise, `paidAmount = 0`, non remplacée | Demander une modification ([modification](modification-facture.md)) |
| Facture | émise, non annulée | Changer le client, Envoyer, Échéancier (si solde) |
| Facture, rectificative, note de débit | `ISSUED` ou `SENT` et `locked = false` | Certifier |
| Facture | `receivable = true` | Encaisser |
| Facture | `locked = true` | Avoir, Rectificative, Corriger le destinataire |
| Avoir | `VALIDATED` | Imputer, Reverser au portefeuille |
| Proforma | tout statut | Modifier (nouvelle version), Envoyer |
| Proforma | non `CONVERTED` | Transformer en devis |
