# La facture renvoyée après paiement dit qu'elle est soldée

## Le problème

Après tout règlement d'un document de facturation, `PaymentTransactionWorkflowProcessor`
renvoie la facture au client avec son PDF à jour. Le PDF était juste · le courriel qui l'entourait
disait le contraire :

> **Votre facture est disponible**
> Le récapitulatif est disponible en pièce jointe.
> …
> **Solde dû** · 0 XAF

Un client qui venait de payer recevait donc ce qui ressemble à une nouvelle facture, avec un
« solde dû » à zéro. Plusieurs rappelaient l'accueil pour demander s'ils devaient payer encore.

## L'état se lit sur les montants, pas sur le statut

`BillingEmailServiceImpl.stateOf` distingue quatre situations :

| État | Condition | Ce que le courriel dit |
|---|---|---|
| `SETTLED` | `balanceDue <= 0` **et** `paidAmount > 0` | « Votre facture est soldée · plus rien ne reste à régler » |
| `PARTIALLY_PAID` | `paidAmount > 0` et un solde reste | « Votre règlement a été enregistré · il reste X » |
| `OVERDUE` | statut `OVERDUE`, rien de réglé | « Rappel de règlement » (inchangé) |
| `ISSUED` | rien de réglé | « Votre facture est disponible » (inchangé) |

Le statut du document suit son cycle de vie ; les montants suivent l'argent. Une facture peut être
`SENT` et déjà soldée, ou `OVERDUE` et réglée depuis. C'est l'argent qui intéresse le destinataire,
donc c'est lui qui décide du message.

Une facture à zéro que personne n'a payée n'est pas « soldée » · elle est simplement disponible.

## Ce qui change dans le courriel

- **Le sujet** · « Votre facture INV-1 **est soldée** », « Règlement enregistré - INV-1 ». Un sujet
  se lit sans ouvrir : « Votre facture INV-1 » sur une facture qu'on vient de payer se lit comme
  une relance.
- **Le corps** accuse réception du montant réglé, et nomme le solde restant s'il y en a un.
- **La ligne de total** · une facture soldée affiche « Solde · **Réglé** » en vert, pas
  « Solde dû · 0 XAF ».
- **Le bandeau** passe au vert pour une facture soldée.

Le PDF joint est celui du document à jour · il l'était déjà.

## Ce qui n'a pas changé

- **Quand** le courriel part : après tout règlement d'un document, comme avant. Un client qui paie
  au guichet sans avoir jamais reçu la facture la reçoit toujours · avec son PDF, c'est sa facture.
- Le courriel court de confirmation de règlement (`sendPaymentConfirmation`, gabarit
  `payment-confirmation`) reste tel quel. Il n'est envoyé que par `payInvoice`.
- Les relances d'impayés (`BillingPaymentReminderWorker`) passent par le même gabarit et
  bénéficient de la distinction : une facture réglée entre la sélection et l'envoi ne part plus
  comme un rappel.
