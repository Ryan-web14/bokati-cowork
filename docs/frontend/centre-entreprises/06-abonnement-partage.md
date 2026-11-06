# Abonnement d'organisation partagé

Préfixe des routes : `/sni/api/v1`.

L'organisation détient l'abonnement (souscrit à son nom : `subscriberType = CUSTOMER`, son code
`CUS-COM-...`). Ses collaborateurs le consomment par une **place**. Le droit est décompté chez
l'organisation ; la réservation garde le collaborateur comme bénéficiaire. Seule l'organisation
est facturée, dépassement compris (la ligne de dépassement nomme le collaborateur).

## Répartition, réglable par droit

| Mode | Fonctionnement |
| --- | --- |
| `POOL` (par défaut) | pot commun : chaque place y puise jusqu'à épuisement du solde de l'organisation |
| `QUOTA` | part nominative fixe par place, rien au-delà |
| `QUOTA_AND_POOL` | sa part d'abord, puis le pot commun (ce que l'organisation a reçu moins les parts distribuées) |

- `defaultQuota` : la part de chaque place (modes `QUOTA` et `QUOTA_AND_POOL`). Une place peut
  avoir sa propre part.
- `seatCap` : plafond facultatif par place sur la période, dans tous les modes. Une place peut avoir
  son propre plafond.
- Les parts distribuées ne peuvent pas dépasser ce que l'organisation a reçu pour la période.
- Parts, plafonds et pot repartent à chaque période de l'abonnement.
- La politique de dépassement du plan s'applique au solde de l'organisation. Une réservation qui
  dépasserait la part, le plafond ou le pot d'une place est refusée, avec la raison.

## Réservation par un collaborateur

En mode abonnement, la réservation cherche d'abord les droits du membre lui-même, puis ceux des
organisations où il a une place active (lien actif, abonnement actif sur sa période). Les
organisations ne sont consultées que si ses propres droits ne couvrent pas la réservation. Quand
plusieurs organisations sont possibles :

1. celle passée dans `billingCompanyCode` (`NONE` : aucune) ;
2. sinon celle dont le lien est en mode automatique ;
3. sinon la seule où il a une place ;
4. sinon refus (400) : le membre doit choisir (`billingCompanyCode`).

## Départ d'un collaborateur

La fin de son lien ferme ses places. Ce qui reste de sa part est perdu ; ce qu'il n'a pas pris
dans le pot y reste. Ses réservations à venir sur ces places sont signalées (`warning` de la
réponse), pas annulées.

## Routes

| Qui | Méthode et chemin | Effet |
| --- | --- | --- |
| Administration | `GET /companies/{customerId}/subscriptions` | abonnements ouverts de l'organisation, places occupées |
| Administration | `GET /companies/{customerId}/subscriptions/{n}/sharing` | règles, droits reçus et restants, places et consommation |
| Administration | `PUT /companies/{customerId}/subscriptions/{n}/sharing/{entitlementCode}` | règle d'un droit `{ mode, defaultQuota, seatCap }` |
| Administration | `PATCH /companies/{customerId}/subscriptions/{n}/max-seats` | plafond de places vendu et nombre de places `{ maxSeats }` (vide = sans limite) |
| Gérant | `PUT /client/companies/{customerId}/subscriptions/{n}/sharing/{entitlementCode}` | règle d'un droit, comme l'administration |
| Gérant | `PATCH /client/companies/{customerId}/subscriptions/{n}/max-seats` | son nombre de places `{ maxSeats }`, au plus le plafond vendu (vide = le plafond) |
| Administration et gérant | `POST .../subscriptions/{n}/seats` | attribue une place `{ memberId, allowances? }` à un membre rattaché |
| Administration et gérant | `PATCH .../subscriptions/{n}/seats/{seatId}` | change part et plafond `{ allowances: [{ entitlementCode, quota, cap }] }` |
| Administration et gérant | `POST .../subscriptions/{n}/seats/{seatId}/release` | libère la place `{ reason? }` |
| Gérant | `GET /client/companies/{customerId}/subscriptions` (et `.../{n}/sharing`) | comme l'administration |
| Gérant | `GET /client/companies/{customerId}/usage` | consommation par collaborateur, tous abonnements ouverts |
| Collaborateur | `GET /client/me/entitlements` | ses places : sa part, ce qui en reste, son plafond, le pot et le solde de l'organisation |

Côté gérant, les chemins commencent par `/client/companies/{customerId}`. Le gérant règle la
répartition et son nombre de places ; seul l'administration fixe le plafond vendu (`seatCeiling`
dans le résumé de l'abonnement). `seatId` est le numéro court de la place.

Extrait de `sharing` :

```json
{
  "companyCustomerCode": "CUS-COM-...",
  "subscription": { "subscriptionNumber": "SUB-...", "planName": "Équipe", "maxSeats": 10, "activeSeats": 4 },
  "entitlements": [{
    "entitlementCode": "SALLE_H", "unit": "HOUR", "mode": "QUOTA_AND_POOL",
    "defaultQuota": 5, "seatCap": 12, "granted": 50, "remaining": 31,
    "distributedQuotas": 20, "poolSize": 30, "poolUsed": 7
  }],
  "seats": [{
    "id": 12, "memberId": "MBR-...", "memberName": "Jean Mbemba",
    "entitlements": [{ "entitlementCode": "SALLE_H", "quota": 5, "quotaUsed": 5, "quotaRemaining": 0, "cap": 12, "used": 9 }]
  }]
}
```

## Vérifier avant de réserver

`POST /bookings/check-availability` (administration) et `POST /client/bookings/availability`
(portail) acceptent `paymentMode` (`SUBSCRIPTION` ou `PASS`) et `billingCompanyCode`. Avec un mode,
la réponse dit aussi si un droit couvre la réservation, en appliquant le même choix
d'organisation que la réservation : un membre placé chez deux organisations passe
`billingCompanyCode` pour ne pas lire « choisissez ».

## Bénéficiaire et ciblage

Sur un droit de l'organisation, la retenue et chaque ligne du journal des droits portent le
collaborateur (`beneficiaryType`, `beneficiaryCode`). La retenue ne vise que l'abonnement de sa
place, ou le pass partagé : les autres abonnements et passes de l'organisation portant le même
droit ne sont pas touchés. Le dépassement suit la politique de cet abonnement-là.

## Annexe de facture

La facture de renouvellement d'un abonnement d'organisation porte, en page séparée, une annexe
« Consommation par collaborateur » sur la période qui vient de s'achever : par collaborateur et par
droit, ce qui a été pris sur sa part, sur le pot commun, le total, et les dépassements pour mémoire
(déjà facturés à l'organisation, chacun sur sa facture). L'annexe est figée à la création de la
facture et scellée avec elle : une réimpression montre la même. Pas d'annexe si personne n'a
consommé sur la période.

La facture de renouvellement d'un pass d'organisation porte la même annexe, réduite au total pris
par chaque bénéficiaire (un pass n'a ni part, ni pot, ni dépassement). La période écoulée va du
début de la facturation précédente du pass (émission ou renouvellement) jusqu'au renouvellement.

L'annexe se lit dans sa propre transaction : si cette lecture échoue, la facture sort sans annexe
au lieu d'échouer avec elle.
