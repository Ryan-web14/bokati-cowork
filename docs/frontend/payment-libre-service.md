# Paiement depuis l'espace client · ce que le back-office doit voir

Quand un client règle depuis son espace, personne n'est en face de lui. La caisse doit l'apprendre
autrement qu'en ouvrant la facture : sinon elle refuse l'accès à un client à jour, ou relance
quelqu'un qui a déjà payé.

Le backend prévient tout seul. Ce document dit ce que le frontend en fait.

## 1. `channel` sur une transaction

`PaymentTransactionResponse` porte désormais `channel` :

| Valeur | À afficher |
|---|---|
| `SELF_SERVICE` | « Espace client » · le client a payé lui-même |
| `BACK_OFFICE` | « Guichet » · un agent a enregistré le paiement |
| `SYSTEM` | « Automatique » · prélèvement, reprise, rapprochement |
| `null` | Rien · transaction antérieure à ce suivi, l'origine n'est pas connue |

N'inventez pas de valeur pour `null` : une transaction ancienne n'a pas d'origine connue, et lui
en attribuer une fausserait toute lecture.

Un filtre « origine » sur la liste des transactions est le complément naturel de la file de caisse.

## 2. L'alerte temps réel · `/topic/admin/alerts`

Un règlement libre-service abouti diffuse un `AdminAlertEvent` sur le topic STOMP déjà utilisé par
le portail d'administration :

```json
{
  "alertType": "SELF_SERVICE_PAYMENT_RECEIVED",
  "module": "PAYMENT",
  "title": "Paiement client · 25000 XAF par Joël Bikindou",
  "message": "Joël Bikindou a réglé 25000 XAF depuis son espace client (Mobile money).",
  "severity": "INFO",
  "payloadJson": null,
  "occurredAt": "2026-09-23T10:15:31Z"
}
```

C'est ce qui doit allumer l'écran de caisse ouvert · un bandeau, un compteur, un son discret.
Sévérité `INFO` : c'est une bonne nouvelle, pas un incident.

## 3. La notification persistante

Le même événement crée une notification `IN_APP` pour chaque adresse configurée, lisible par les
routes existantes :

```http
GET /api/v1/admin/notifications/unread?limit=20
GET /api/v1/admin/notifications/unread-count
```

Elle survit à l'écran fermé · l'agent qui prend son poste voit ce qui est passé pendant la nuit.

Le `payload` de la notification porte les faits utiles : `transactionNumber`, `receiptNumber`,
`amount`, `currency`, `paymentMethod` (déjà en français), `paidAt`, `purpose` (`Facture INV-…`,
`Abonnement SUB-…`), `customerName`, `customerCode`, `customerEmail`, `customerPhone`.

## 4. Il n'y a rien à valider

Le message **confirme** la transaction, il ne demande pas une approbation. Le paiement est acquis
dès que l'opérateur ou le portefeuille l'a confirmé ; un refus après coup n'aurait aucun effet sur
l'argent.

N'affichez donc pas de bouton « Accepter » ou « Refuser ». Ce qui est utile : « Voir la
transaction », « Voir la facture », « Voir le client ».

## 5. Quand ça ne part pas

- Paiement enregistré au guichet (`BACK_OFFICE`) · quelqu'un était déjà en face.
- Écriture automatique (`SYSTEM`) · aucun client à servir derrière.
- Paiement mobile money encore en cours · l'alerte part à la **confirmation de l'opérateur**, pas à
  l'initiation. Un client qui lance un paiement et ne le valide jamais ne génère aucune alerte.
