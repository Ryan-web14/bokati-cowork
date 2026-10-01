# Ce qu'un client doit · et ce qui n'en est pas

## Le problème

Un document de facturation porte un `balance_due` dès sa création. Ce montant ne signifie « le
client doit ceci » que pour certains types, dans certains états. Il était pourtant sommé sans
discernement, avec deux erreurs opposées et toutes deux coûteuses :

- une facture en **brouillon** comptait comme due · le client voyait un solde qu'on ne lui avait
  jamais réclamé, et une facture encore en préparation le faisait passer pour débiteur ;
- un **avoir** comptait comme dû · on lui réclamait ce qu'on lui devait.

Une troisième erreur, dans l'autre sens, était visible au même endroit : une facture **scellée**
(`VALIDATED`, SEFC) n'apparaissait dans aucune des requêtes de créance. Une facture définitive
était donc invisible du recouvrement, du vieillissement et des relances.

## La règle, écrite une fois · `BillingReceivables`

```java
RECEIVABLE_TYPES = { INVOICE, PROFORMA_INVOICE }          // un avoir n'en est pas un
NOT_ISSUED       = { DRAFT, CANCELLED, VOIDED, REJECTED, EXPIRED, CONVERTED }
SETTLED          = { PAID, REFUNDED, WRITTEN_OFF }

issued(type, status)      = RECEIVABLE_TYPES.contains(type) && !NOT_ISSUED.contains(status)
receivable(type, status)  = issued(...) && !SETTLED.contains(status)
creditAvailable(type, st) = type == CREDIT_NOTE && st == VALIDATED
```

Sur un avoir, `ISSUED` signifie **consommé** : c'est l'état posé quand il est imputé sur une
facture ou reversé au portefeuille. Un avoir encore utilisable est donc `VALIDATED`. Cette
surcharge est celle du code existant, elle n'a pas été changée.

Les listes SQL des requêtes d'agrégation reprennent **exactement** ces ensembles. Quand l'une
change, l'autre change · c'est écrit dans le commentaire de chaque requête.

## `customerImpact` · ce qu'une ligne pèse vraiment

Chaque `BillingDocumentResponse` porte désormais deux champs :

| Champ | Ce qu'il dit |
|---|---|
| `receivable` | Ce document constitue-t-il une créance en cours ? |
| `customerImpact` | Ce qu'il change au solde · **positif** s'il doit, **négatif** si on lui doit, **zéro** sinon |

Additionner `customerImpact` sur une liste de documents donne le solde juste, quels que soient les
types et les états présents. Additionner `balanceDue` ne le donne pas : c'est exactement ce que
faisaient les consommateurs du relevé.

## Le relevé client · `CustomerStatementResponse`

`statementTotals` rend maintenant cinq sommes au lieu de trois :

| Champ | Contenu |
|---|---|
| `totalInvoiced` | Ce qui a été facturé · les brouillons n'en font pas partie |
| `totalPaid` | Ce qui a été encaissé |
| `totalBalanceDue` | Ce qui reste dû sur les factures émises et non réglées · rien d'autre |
| `totalCreditAvailable` | Les avoirs scellés et pas encore consommés |
| `netBalanceDue` | `totalBalanceDue − totalCreditAvailable`, jamais négatif |
| `totalDraft` | Les factures en préparation · pour information, hors de tout solde |

`netBalanceDue` s'arrête à zéro : si les avoirs dépassent les factures, le client n'est pas
débiteur pour autant, il garde un crédit (lisible dans `totalCreditAvailable`).

`ClientSpendingSummaryResponse` porte les mêmes notions, et son `invoiceCount` / `unpaidCount`
ne comptent plus les brouillons.

## Ce que le client voit

Une facture en brouillon est un document de travail. Le client ne la voit plus :

- `GET /client/billing/invoices` la filtre ;
- `GET /client/billing/invoices/{n}` répond `404`, comme pour une facture qui n'est pas la sienne.

## Les factures scellées comptent à nouveau

`VALIDATED` (et `VIEWED`, absent de plusieurs listes) a été ajouté aux requêtes de créance :
`findPayableDocuments`, `findRecoverableDocuments`, `findForAgingReport`, `markOverdueDocuments`,
`findOverdueReminderCandidates`, `findOverdueWithBalance`.

Conséquence attendue en production : des factures validées jusqu'ici muettes vont apparaître dans
le recouvrement, le rapport de vieillissement et les relances. Ce sont de vraies créances ; si
certaines ne doivent pas être relancées, c'est leur statut qu'il faut corriger, pas la requête.

## L'analytique

`AnalyticsRepository.financial` sommait `balance_due` sur toutes les factures non annulées ·
brouillons compris. Le montant restant à encaisser exclut désormais les brouillons, les documents
abandonnés et les créances éteintes ; le montant facturé exclut les brouillons.

## Ce qui n'a pas été touché

Le jeu de types reste `{ INVOICE, PROFORMA_INVOICE }` partout. `CORRECTIVE_INVOICE` et
`DEBIT_NOTE` restent hors des agrégations de créance, comme avant · les inclure changerait des
comportements qui n'ont pas été signalés, et demande une décision comptable.
