# Rattachement des paiements et des factures aux éléments (lots R1 à R4)

> Rédigé d'après le code (branche `feature/rattachement-paiements`, 14/11/2026). Routes exactes et champs JSON : [routes.md](routes.md) et `routes/BillingElementController.md`. Droits : ceux de la facturation (lecture `BILLING:READ`, rattachement `BILLING:CREATE`, annulation et défaire un lien `BILLING:CANCEL`).

Un **élément** est ce qu'une facture règle : un abonnement (`SUBSCRIPTION`), un pass (`PASS`), une réservation (`BOOKING`) ou une domiciliation (`DOMICILIATION`). Il se désigne par son type et son code, par exemple `{ "type": "BOOKING", "code": "BKG-SAL-20260903-00000008" }`.

## Écran : trouver l'élément

1. `GET /billing/elements/search?query=...&limit=10` (au moins deux caractères) : nom, courriel, téléphone ou code. Réponse : liste de tiers `{ type, code, name, email, phone, external, elements[] }` :
   - `type` : `MEMBER`, `CUSTOMER` ou `BUSINESS_ENTITY` ;
   - `external = true` : client sans compte (client externe saisi au comptoir), ou trouvé seulement par le contact d'une réservation ;
   - un membre remonte aussi les éléments de son client personne.
2. `GET /billing/elements?partyType=&partyCode=` : éléments d'un tiers déjà connu.
3. Chaque élément : `{ type, code, label, status, holderType, holderCode, startDate, endDate, totalAmount }`. Les éléments annulés, refusés, expirés ou terminés ne sont pas proposés.
4. `GET /billing/elements/{type}/{code}/invoices?openOnly=true` : ses factures, `origin` `OWN` (produite par l'élément) ou `LINKED` (rattachée), `balanceDue`, `activationMode`.

Une domiciliation est facturée par son abonnement parent : ses factures sont celles de cet abonnement.

## Geste A : un paiement vers une facture

Inchangé côté écran ([paiements-en-attente.md](paiements-en-attente.md)) : `POST /billing/unallocated-payments/{transactionNumber}/allocate` avec `allocations`. Nouveau : l'élément que la facture règle réagit (voir « Effets »).

## Geste B : un paiement vers un élément

Même route, avec `elements` à la place (ou en plus) de `allocations` :

```json
{ "elements": [ { "type": "SUBSCRIPTION", "code": "SUB-MEM-COW-MON-202606-00000001", "amount": 150000, "activationMode": "FIRST_PAYMENT" } ] }
```

- Le montant va sur les factures ouvertes de l'élément, échéance la plus ancienne d'abord, sans dépasser leur reste dû. Le surplus reste en attente (`stillPending`).
- `activationMode` : `FULL_SETTLEMENT` (défaut, l'élément réagit quand toutes ses factures sont soldées) ou `FIRST_PAYMENT` (dès ce paiement). L'écran propose le choix à l'administrateur, selon ce qui a été convenu avec le client.
- Un paiement non réussi ou remboursé est refusé (`PAIEMENT_NON_REUSSI`).

## Geste C : une facture vers un élément

Cas visé : une facture faite à la main pour une réservation, un pass, un abonnement ou une domiciliation.

- `POST /billing/documents/{documentNumber}/elements` avec `{ "type", "code", "activationMode", "replaceOwnInvoice" }`. Réponse : les éléments désormais réglés par la facture.
- Seule une facture, une facture rectificative ou une note de débit, non annulée, se rattache.
- La facture doit être au nom du titulaire de l'élément, de son client personne s'il est membre, ou de l'entreprise qui paie pour lui (entité du client, organisation facturée de la réservation). Sinon `TIERS_DIFFERENT`.
- `replaceOwnInvoice` (défaut `true`) : la facture propre de l'élément qui n'a reçu aucun paiement est annulée, par avoir si elle est certifiée. Une réservation ainsi réglée par une autre facture n'est pas annulée avec sa facture propre.
- Si la facture rattachée a déjà reçu un paiement, les effets partent aussitôt ; sinon à son paiement, quel que soit le moyen.
- `GET /billing/documents/{documentNumber}/elements` : éléments réglés par une facture (`origin`, `activationMode`).
- `DELETE /billing/documents/{documentNumber}/elements/{type}/{code}` : défaire le lien, tant que la facture n'a reçu aucun paiement (sinon `LIEN_PAYE` : annuler d'abord ses affectations).

## Effets d'un paiement sur l'élément

Chaque paiement reçu sur une facture, quelle que soit la voie, est suivi (par la boîte de sortie, en quelques secondes) des mêmes effets qu'un paiement fait directement sur l'élément :

| Élément | Effet quand il est réglé |
| --- | --- |
| Abonnement | Renouvellement payé appliqué, sortie de la période de grâce, ou activation s'il attendait |
| Domiciliation | Effets de son abonnement parent ; le contrat reste à confirmer par l'équipe (immatriculation) |
| Pass | Activation s'il attendait |
| Réservation | Confirmation si elle attendait son paiement |

Un élément déjà actif ou confirmé est laissé tel quel. L'écran rafraîchit l'élément après le geste pour voir son nouvel état.

## Geste D : annuler une affectation

1. `GET /billing/transactions/{transactionNumber}/allocations` : `current` (affectations en cours, avec leur `id`), `reversed` (annulées), `pending`.
2. `POST /billing/allocations/{id}/cancel` avec `{ "invoiceOutcome": "REOPEN" | "NEUTRALIZE", "reason": "..." }` (motif obligatoire) :
   - la somme revient en attente sur le paiement ;
   - `REOPEN` (défaut) : la facture redevient due ; `NEUTRALIZE` : la facture est annulée, par avoir si elle est certifiée (une réservation dont c'est la facture propre est alors annulée avec elle) ;
   - un élément qui n'est plus réglé est remis en attente : abonnement remis en attente d'activation, droits retirés, contrat annulé ; pass remis en attente, droits retirés ; réservation confirmée remise en attente de paiement, créneaux libérés. L'historique reste ;
   - refus, sans rien changer, si l'élément a déjà été consommé (`ELEMENT_CONSOMME` : réservation commencée, droits utilisés, réservations faites avec l'abonnement ou le pass) ou si l'abonnement a déjà été renouvelé (`ABONNEMENT_RENOUVELE`, à traiter à la main).
3. Réponse : `{ allocationId, transactionNumber, documentNumber, amount, invoiceOutcome, elementsReverted[], accountingRequired }`.

## Codes d'erreur

| Code | Cas |
| --- | --- |
| `ELEMENT_TYPE_INCONNU`, `ELEMENT_CODE_REQUIS` | Élément mal désigné |
| `MODE_ACTIVATION_INCONNU` | `activationMode` autre que `FULL_SETTLEMENT` ou `FIRST_PAYMENT` |
| `ELEMENT_CLOS` | Élément annulé, refusé, expiré ou terminé |
| `ELEMENT_SANS_FACTURE` | Élément sans facture à régler (rattacher une facture ou émettre la sienne) |
| `RECHERCHE_TROP_COURTE` | Recherche de moins de deux caractères |
| `RATTACHEMENT_INVALIDE`, `DEJA_RATTACHEE`, `TIERS_DIFFERENT`, `LIEN_PAYE` | Geste C |
| `PAIEMENT_NON_REUSSI`, `AFFECTATION_MONTANT`, `AFFECTATION_INVALIDE` | Gestes A et B |
| `SORT_FACTURE_INCONNU`, `MOTIF_REQUIS`, `ELEMENT_CONSOMME`, `ABONNEMENT_RENOUVELE` | Geste D |

## Comptabilité

- Affectation tardive : D 4719 / C 411 (inchangé).
- Annulation d'une affectation déjà comptabilisée : D 411 / C 4719 (règle `SALES_ALLOCATION_REVERSAL`, référence `UNALLOC-<id>`). Une affectation pas encore comptabilisée n'a pas d'écriture à défaire.
