# Paiement depuis l'espace client · confirmation au back-office

## Le problème

Un paiement encaissé au guichet est connu de la personne qui l'a encaissé. Un paiement fait depuis
l'espace client, lui, n'était annoncé à personne. La caisse découvrait le règlement en ouvrant la
facture, parfois des jours plus tard, parfois jamais :

- le client se présentait avec une facture réglée et on lui refusait l'accès ;
- la relance d'impayés partait vers quelqu'un qui avait déjà payé ;
- le rapprochement de la recette du jour ne voyait pas un montant qui n'était passé par aucune caisse.

## Le canal d'un encaissement

`payment_transaction.channel` (`PaymentChannel`) dit d'où vient le règlement :

| Valeur | Ce que ça veut dire |
|---|---|
| `SELF_SERVICE` | Le client a payé depuis son espace, sans personne en face |
| `BACK_OFFICE` | Un agent a enregistré le paiement · caisse, portail d'administration |
| `SYSTEM` | Ni l'un ni l'autre · worker, rappel d'opérateur, reprise de l'outbox |
| `null` | Transaction antérieure à ce suivi · on ne l'invente pas |

Le canal **n'est pas demandé à l'appelant**. Un paramètre de plus sur chaque méthode de paiement
aurait été oublié quelque part, et c'est précisément là qu'on aurait voulu prévenir la caisse.
`PaymentOriginResolver` le lit dans la session en cours :

- une autorité du realm d'administration (`ROLE_ADMIN`, `ROLE_CASHIER`, `ROLE_MANAGER`, …) ⇒ `BACK_OFFICE` ;
- toute autre session authentifiée ⇒ `SELF_SERVICE` ;
- pas de session, ou une authentification anonyme (route publique) ⇒ `SYSTEM`.

La liste des rôles est alignée sur `AdminApiAuthorizationManager`. Si un rôle y est ajouté sans
l'être ici, un agent passe pour un client : on prévient une fois de trop, ce qui est le bon sens de
l'erreur.

Le canal est posé **à la création de la transaction**, pas à son règlement. Pour le mobile money
c'est indispensable : quand l'opérateur confirme, plusieurs minutes plus tard, il n'y a plus de
session pour dire qui avait lancé le paiement.

`PaymentTransactionResponse` porte désormais `channel`, pour que les écrans d'administration
distinguent un règlement libre-service d'un encaissement au guichet.

## La notification · `SelfServicePaymentNotifier`

Déclenchée quand une transaction `SELF_SERVICE` aboutit :

- **portefeuille** · `PaymentServiceImpl.payWithWallet`, immédiatement ;
- **mobile money** · `PawapayCallbackProcessor.handleCompleted`, à la confirmation de l'opérateur.

Rien ne part pour `BACK_OFFICE` (quelqu'un était déjà en face) ni pour `SYSTEM` (pas de client à
servir derrière).

Trois canaux, parce qu'ils ne servent pas au même moment :

1. **Courriel** (`SELF_SERVICE_PAYMENT_RECEIVED`, gabarit `self-service-payment-received`) vers
   chaque adresse de `bokati.payment.self-service.confirmation-email`, un événement outbox par
   destinataire · l'identifiant d'agrégat est suffixé par l'adresse, sans quoi l'outbox
   dédoublonnerait les envois.
2. **Notification dans le portail** (`AdminInAppNotifier.notify`, canal `IN_APP`) pour chaque
   destinataire · elle survit à l'écran fermé.
3. **Diffusion WebSocket** sur `/topic/admin/alerts` (`AdminAlertEvent`, sévérité `INFO`) · l'écran
   de caisse ouvert s'allume tout de suite, c'est là que le client se présente.

Le message **confirme** la transaction : elle est déjà encaissée, il n'y a rien à valider. Ce qui
est attendu de la caisse, c'est de la voir.

Prévenir la caisse ne peut jamais faire échouer un encaissement : toute exception est journalisée
et avalée. L'argent est reçu, la transaction doit rester `SUCCEEDED`.

### Ce que le message contient

Référence de transaction et de reçu, montant, devise, moyen de paiement **en français**
(« Mobile money », « Portefeuille », « Espèces »…), date d'encaissement, objet résolu
(`Facture INV-…`, `Abonnement SUB-…`), et l'identité du client : nom, code, courriel, téléphone.

## Configuration

```yaml
bokati:
  payment:
    self-service:
      # Une ou plusieurs adresses, séparées par des virgules ou des points-virgules.
      # Vide : seules la notification du portail et la diffusion WebSocket partent.
      confirmation-email: ${SELF_SERVICE_CONFIRMATION_EMAIL:${SUPPORT_MANAGER_EMAIL:}}
```

## Migrations

- `V246` (dev) / `V242` (prod) · `payment_transaction.channel` + index `(channel, status)`.
- `V247` (dev) / `V243` (prod) · gabarit `SELF_SERVICE_PAYMENT_RECEIVED`.

## Ce que ceci ne fait pas

Il n'y a **pas** d'étape d'acceptation : le back-office ne « valide » rien, parce qu'il n'a rien à
valider. Le paiement est acquis dès que l'opérateur ou le portefeuille l'a confirmé, et refuser
après coup n'aurait aucun effet sur l'argent. Si un circuit d'accusé de réception explicite est
voulu (un agent coche « vu », avec une file des non-vus), c'est un état de plus sur la transaction
et une route d'administration · à demander séparément.
