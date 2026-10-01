# Paiement en espèces annoncé · et confirmé par la caisse

## Le problème

Le mobile money et le portefeuille aboutissent seuls : quand la réponse arrive, l'argent est là.
Les espèces non · elles arrivent avec la personne.

Entre le moment où un client annonce depuis son espace qu'il passera régler et le moment où la
caisse compte les billets, il n'y a rien d'encaissé. Cette attente n'était représentée nulle part :

- le client ne pouvait pas annoncer · l'espace client n'offrait que mobile money et portefeuille ;
- la caisse ne voyait rien venir ;
- personne ne savait qu'un créneau était tenu pour quelqu'un qui allait passer payer.

## Les deux règles qui tiennent tout le reste

**Annoncer n'éteint rien.** La facture reste due, la réservation reste `PENDING_PAYMENT`. Une
annonce jamais honorée effacerait sinon une créance réelle.

**Seule la caisse confirme**, depuis une session de caisse ouverte. Un encaissement qui n'atterrit
dans aucune caisse rend le comptage du soir faux et ne laisse personne responsable.

## L'entité · `cash_payment_declaration`

L'annonce est adossée à une **facture**, qui est le dénominateur commun : une réservation réglée
en paiement direct passe elle aussi par une facture (via son `billable_item`). Le numéro de
réservation est conservé à part, parce qu'une annonce qui expire doit rendre le créneau.

| État | Ce que ça veut dire |
|---|---|
| `AWAITING_CONFIRMATION` | Le client a annoncé · la caisse attend |
| `CONFIRMED` | Les billets sont comptés · un paiement `CASH` a été enregistré |
| `CANCELLED` | Le client s'est ravisé, ou la caisse a fait le ménage |
| `EXPIRED` | Le délai est passé sans que personne ne se présente |

Le délai par défaut est de **4 heures**. Un créneau tenu pour quelqu'un qui ne vient pas est un
créneau que personne d'autre ne peut prendre : mieux vaut le rendre dans la demi-journée que le
lendemain. Le worker passe toutes les 15 minutes, pour que ce délai soit tenu de près.

Une seule annonce en cours par facture : deux annonces finissent par deux encaissements, et le
client paie deux fois. La seconde demande rend la première.

## Le parcours

```
Client  → facture INV-… (ou réservation BKG-…) → « je paierai en espèces »
        → ESP-2026-000001, AWAITING_CONFIRMATION
        → la facture reste due, le créneau reste tenu

Caisse  → file « à encaisser »
        → confirme : session de caisse + montant réellement compté
        → intention de paiement → transaction CASH → imputation sur la facture
        → la réservation se confirme d'elle-même quand l'intention est soldée

Personne ne vient
        → worker (toutes les 15 min) → EXPIRED
        → réservation PENDING_PAYMENT → annulée, créneau rendu
        → facture → reste due, simplement plus annoncée
```

La confirmation passe par le circuit habituel (`createIntentFromBillingDocument` puis
`registerCashPayment`). Rien n'est réinventé : l'imputation sur la facture, l'enregistrement en
caisse et la confirmation de la réservation sont ceux qui existent déjà.

Le montant confirmé peut différer du montant annoncé · c'est le cas fréquent où la personne ne
règle qu'une partie. C'est ce que la caisse a compté qui fait foi, jamais ce qui était annoncé.

## Les garde-fous

- Une facture en **brouillon**, annulée ou déjà soldée ne s'annonce pas.
- Une devise autre que **XAF** est refusée · la caisse ne tient que du XAF.
- Un montant **supérieur au solde** est refusé à l'annonce, pas au guichet où le client serait
  déjà là avec ses billets.
- Une facture **réglée entre-temps** par un autre moyen ferme l'annonce au lieu d'encaisser une
  seconde fois.
- L'annonce d'un autre client répond `404`, pas `403`.
- À l'expiration, une réservation déjà `CONFIRMED` (le client a réglé autrement) **n'est pas
  annulée** · annuler une réservation payée serait bien pire que laisser une annonce en trop.

## Les routes

| Route | Accès | Ce qu'elle fait |
|---|---|---|
| `POST /client/billing/invoices/{n}/pay/cash` | membre | Annonce sur une facture |
| `POST /client/bookings/{n}/pay/cash` | membre | Annonce sur une réservation en paiement direct |
| `GET /client/billing/cash-declarations` | membre | Ses annonces |
| `POST /client/billing/cash-declarations/{n}/cancel` | membre | Il se ravise |
| `GET /payments/cash-declarations?status=AWAITING_CONFIRMATION` | caisse | La file à encaisser |
| `GET /payments/cash-declarations/{n}` | caisse | Une annonce |
| `POST /payments/cash-declarations/{n}/confirm` | caisse | Les billets sont comptés |
| `POST /payments/cash-declarations/{n}/cancel` | caisse | Le client ne viendra pas |

Côté caisse : `ADMIN`, `SUPER_ADMIN`, `MANAGER` ou `CASHIER`.

## Les notifications

« À encaisser » n'est pas « encaissé » · les deux messages ne disent pas la même chose. Un
règlement mobile money se signale comme un fait accompli
(`docs/backend/paiement-libre-service.md`) ; une annonce d'espèces est une **demande d'action**.

| Événement | Vers | Quand |
|---|---|---|
| `CASH_PAYMENT_DECLARED` | la caisse · courriel, notification portail, WebSocket `severity: WARNING` | À l'annonce |
| `CASH_PAYMENT_CONFIRMED` | le client | Les billets sont comptés · vaut confirmation écrite |
| `CASH_PAYMENT_DECLARATION_EXPIRED` | le client | Le délai est passé · dit si le créneau a été rendu |

## Configuration

```yaml
bokati:
  payment:
    cash-declaration:
      validity-hours: ${CASH_DECLARATION_VALIDITY_HOURS:4}
      expiry-delay-ms: ${CASH_DECLARATION_EXPIRY_DELAY_MS:900000}    # cadence du worker
      # Qui tient la caisse · une ou plusieurs adresses, séparées par des virgules.
      desk-email: ${CASH_DECLARATION_DESK_EMAIL:${SELF_SERVICE_CONFIRMATION_EMAIL:${SUPPORT_MANAGER_EMAIL:supportela@elleaose.com}}}
```

La chaîne de repli vit dans la configuration, pas dans le `@Value`. Une propriété **déclarée avec
une valeur vide n'est pas une propriété absente** : le défaut écrit en Java ne se déclenchait
jamais, et la caisse ne recevait aucun courriel · seules la notification du portail et la
diffusion WebSocket partaient.

## Migrations

- `V248` (dev) / `V244` (prod) · table `cash_payment_declaration`, ses index et la séquence `ESP`.
- `V249` (dev) / `V245` (prod) · les trois gabarits de courriel.
