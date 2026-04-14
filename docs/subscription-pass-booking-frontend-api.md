# Guide Frontend/API — Booking · Subscription · Pass

## Vue d'ensemble

Ce document explique comment le frontend doit utiliser les endpoints des modules :

- `booking` — reservation de ressources, cycle de vie, participants
- `subscriptions/plans` — plans tarifaires avec avantages configures
- `subscriptions` — souscriptions actives d'un titulaire a un plan
- `passes` — passes prépayés ou a duree limitee

Base URL :

- `/sni/api/v1`

---

## Concepts principaux

- `Booking`
  - represente une reservation d'une ressource pour une plage horaire
- `BookingLine`
  - chaque service ou ressource inclus dans la reservation
- `BookingParticipant`
  - co-participants invites sur une reservation
- `SubscriptionPlan`
  - definit un offre commerciale avec ses avantages (ce qu'on vend)
- `PlanBenefit`
  - chaque avantage inclus dans un plan (heures, acces, remises)
- `Subscription`
  - souscription active d'un titulaire (member, customer, business) a un plan
- `SubscriptionEntitlement`
  - solde de droits disponibles pour la periode de facturation courante
- `PassTemplate`
  - modele de pass vendable (ex : carnet 10h, journée illimitée)
- `Pass`
  - instance d'un pass emis et attribue a un titulaire
- `PassUsage`
  - trace chaque consommation de credits sur un pass

Types de titulaire :

- `MEMBER`
- `CUSTOMER`
- `BUSINESS`

Modes de paiement d'une reservation :

- `DIRECT` — tarif horaire standard, facture generee
- `SUBSCRIPTION` — credits deduits de la souscription active
- `PASS` — credits deduits d'un pass actif

---

## 1. Reservation — Verification de disponibilite

### Verifier un creneau

`POST /bookings/check-availability`

Body :

```json
{
  "resourceCode": "SALLE-A",
  "startAt": "2026-04-15T09:00:00Z",
  "endAt": "2026-04-15T11:00:00Z"
}
```

Reponse :

```json
{
  "resourceCode": "SALLE-A",
  "resourceName": "Salle de reunion A",
  "startAt": "2026-04-15T09:00:00Z",
  "endAt": "2026-04-15T11:00:00Z",
  "available": true,
  "conflictingBookingCodes": [],
  "applicablePricingRules": [
    {
      "bookingUnit": "HOUR",
      "price": 5000,
      "currency": "XAF"
    }
  ],
  "estimatedAmount": 10000
}
```

Si indisponible :

```json
{
  "available": false,
  "conflictingBookingCodes": ["BOK-202604-000012"],
  "nextAvailableSlot": "2026-04-15T11:00:00Z"
}
```

Usage frontend :

- appeler avant d'afficher le formulaire de confirmation
- afficher le prix estimatif en temps reel
- proposer le prochain creneau disponible si conflit

---

## 2. Reservation — Creation

### Creer une reservation

`POST /bookings`

Body (reservation directe) :

```json
{
  "resourceCode": "SALLE-A",
  "customerCode": "MBR-000001",
  "bookingType": "INSTANT",
  "startAt": "2026-04-15T09:00:00Z",
  "endAt": "2026-04-15T11:00:00Z",
  "notes": "Reunion equipe produit"
}
```

Body (reservation avec souscription) :

```json
{
  "resourceCode": "SALLE-A",
  "customerCode": "MBR-000001",
  "bookingType": "INSTANT",
  "startAt": "2026-04-15T09:00:00Z",
  "endAt": "2026-04-15T11:00:00Z",
  "paymentMode": "SUBSCRIPTION",
  "subscriptionCode": "SUB-202604-000001",
  "notes": "Reunion equipe produit"
}
```

Body (reservation avec pass) :

```json
{
  "resourceCode": "SALLE-A",
  "customerCode": "MBR-000001",
  "bookingType": "INSTANT",
  "startAt": "2026-04-15T09:00:00Z",
  "endAt": "2026-04-15T11:00:00Z",
  "paymentMode": "PASS",
  "passCode": "PASS-202604-000001",
  "notes": "Reunion equipe produit"
}
```

Reponse `201` :

```json
{
  "bookingCode": "BOK-202604-000001",
  "resourceCode": "SALLE-A",
  "resourceName": "Salle de reunion A",
  "customerCode": "MBR-000001",
  "bookingType": "INSTANT",
  "status": "CONFIRMED",
  "startAt": "2026-04-15T09:00:00Z",
  "endAt": "2026-04-15T11:00:00Z",
  "totalAmount": 10000,
  "currency": "XAF",
  "paymentMode": "DIRECT",
  "subscriptionCode": null,
  "passCode": null,
  "notes": "Reunion equipe produit",
  "participants": [],
  "lines": [
    {
      "resourceCode": "SALLE-A",
      "resourceName": "Salle de reunion A",
      "unitPrice": 5000,
      "quantity": 2,
      "unit": "HOUR",
      "subtotal": 10000
    }
  ],
  "createdAt": "2026-04-14T08:00:00Z"
}
```

Types de reservation (`BookingType`) :

- `INSTANT` — reservation immediate confirmee
- `ON_DEMAND` — demande soumise, confirmation manuelle
- `RECURRING` — reservation repetee (a implémenter ulterieurement)

Statuts (`BookingStatus`) :

- `DRAFT`
- `CONFIRMED`
- `IN_PROGRESS`
- `COMPLETED`
- `CANCELLED`
- `NO_SHOW`

---

## 3. Reservation — Consultation et recherche

### Consulter une reservation

`GET /bookings/{bookingCode}`

Reponse : `BookingResponse` complet avec lignes et participants.

### Lister et filtrer les reservations

`GET /bookings?customerCode=MBR-000001&resourceCode=SALLE-A&status=CONFIRMED&from=2026-04-01&to=2026-04-30&page=0&size=20`

Parametres :

- `customerCode`
- `resourceCode`
- `status` — un ou plusieurs statuts
- `from`, `to` — plage de dates
- `bookingType`
- `page`, `size`

Retour : `Page<BookingResponse>`

---

## 4. Reservation — Transitions de statut

### Confirmer une reservation

`PATCH /bookings/{bookingCode}/confirm`

Usage : pour les reservations `ON_DEMAND` en attente de validation manuelle.

### Marquer en cours

`PATCH /bookings/{bookingCode}/start`

Usage : quand le titulaire prend possession physique de l'espace.

### Cloture

`PATCH /bookings/{bookingCode}/complete`

Comportement :

- si `paymentMode = DIRECT`, une facture est generee automatiquement
- si `paymentMode = SUBSCRIPTION`, les credits ont deja ete consommes a la confirmation
- si `paymentMode = PASS`, les credits ont deja ete consommes a la confirmation

### Annuler

`PATCH /bookings/{bookingCode}/cancel`

Body :

```json
{
  "cancelledBy": "MBR-000001",
  "cancellationReason": "CUSTOMER_REQUEST",
  "notes": "Reunion reportee"
}
```

Raisons d'annulation (`BookingCancellationReason`) :

- `CUSTOMER_REQUEST`
- `NO_SHOW`
- `RESOURCE_UNAVAILABLE`
- `POLICY_VIOLATION`
- `ADMIN_DECISION`

Comportement :

- si `paymentMode = SUBSCRIPTION` ou `PASS`, les credits sont restitues si annulation dans le delai de la politique
- la `ResourcePolicy` determine le delai minimum d'annulation autorise

### Marquer absent

`PATCH /bookings/{bookingCode}/no-show`

Usage : le titulaire ne s'est pas presente. Aucun remboursement de credits.

---

## 5. Reservation — Participants

### Ajouter un participant

`POST /bookings/{bookingCode}/participants`

Body :

```json
{
  "memberCode": "MBR-000002",
  "email": "jean@example.com",
  "role": "GUEST"
}
```

Roles (`ParticipantRole`) :

- `HOST` — organisateur principal
- `GUEST` — invite
- `ORGANIZER` — co-organisateur

### Retirer un participant

`DELETE /bookings/{bookingCode}/participants/{email}`

### Lister les participants

`GET /bookings/{bookingCode}/participants`

---

## 6. Reservation — Historique des statuts

### Consulter l'historique

`GET /bookings/{bookingCode}/history`

Reponse :

```json
[
  {
    "fromStatus": null,
    "toStatus": "DRAFT",
    "changedBy": "MBR-000001",
    "reason": "Creation",
    "changedAt": "2026-04-14T08:00:00Z"
  },
  {
    "fromStatus": "DRAFT",
    "toStatus": "CONFIRMED",
    "changedBy": "MBR-000001",
    "reason": null,
    "changedAt": "2026-04-14T08:00:05Z"
  }
]
```

---

## 7. Plans tarifaires — Gestion

### Creer un plan

`POST /subscriptions/plans`

Body :

```json
{
  "name": "Coworking Essentiel",
  "description": "Acces illimite aux espaces ouverts, 10h de salle de reunion",
  "billingCycle": "MONTHLY",
  "price": 50000,
  "currency": "XAF",
  "trialDays": 7,
  "visibility": "PUBLIC",
  "sortOrder": 1
}
```

Cycles de facturation (`PlanBillingCycle`) :

- `MONTHLY`
- `QUARTERLY`
- `YEARLY`
- `ONE_TIME`

Visibilite (`PlanVisibility`) :

- `PUBLIC` — visible dans le catalogue client
- `PRIVATE` — sur invitation uniquement
- `INTERNAL` — usage interne uniquement

Statuts plan (`PlanStatus`) :

- `DRAFT`
- `ACTIVE`
- `ARCHIVED`

### Modifier un plan

`PUT /subscriptions/plans/{planCode}`

Note : la modification d'un plan actif ne modifie pas les souscriptions existantes.

### Activer / archiver un plan

`PATCH /subscriptions/plans/{planCode}/activate`

`PATCH /subscriptions/plans/{planCode}/archive`

### Lister les plans

`GET /subscriptions/plans?status=ACTIVE&visibility=PUBLIC`

Reponse :

```json
[
  {
    "planCode": "PLAN-001",
    "name": "Coworking Essentiel",
    "description": "Acces illimite aux espaces ouverts, 10h de salle de reunion",
    "billingCycle": "MONTHLY",
    "price": 50000,
    "currency": "XAF",
    "trialDays": 7,
    "status": "ACTIVE",
    "visibility": "PUBLIC",
    "sortOrder": 1,
    "benefits": [
      {
        "benefitCode": "BEN-001",
        "benefitType": "RESOURCE_ACCESS",
        "resourceTypeCode": "OPEN_SPACE",
        "quantity": null,
        "unit": null,
        "description": "Acces illimite open space"
      },
      {
        "benefitCode": "BEN-002",
        "benefitType": "RESOURCE_HOURS",
        "resourceTypeCode": "MEETING_ROOM",
        "quantity": 10,
        "unit": "HOUR",
        "description": "10 heures de salle de reunion par mois"
      }
    ]
  }
]
```

---

## 8. Plans tarifaires — Avantages

### Ajouter un avantage

`POST /subscriptions/plans/{planCode}/benefits`

Body :

```json
{
  "benefitType": "RESOURCE_HOURS",
  "resourceTypeCode": "MEETING_ROOM",
  "quantity": 10,
  "unit": "HOUR",
  "description": "10 heures de salle de reunion par mois"
}
```

Types d'avantages (`PlanBenefitType`) :

- `RESOURCE_HOURS` — credits d'heures sur un type de ressource
- `RESOURCE_DAYS` — credits de journees
- `RESOURCE_ACCESS` — acces illimite a un type de ressource
- `DISCOUNT_PERCENT` — remise en pourcentage sur les reservations
- `WALLET_CREDIT` — credits credites sur le wallet a chaque cycle
- `GUEST_PASSES` — passes invites inclus
- `PRIORITY_BOOKING` — priorite sur les reservations
- `CUSTOM` — avantage libre

### Modifier un avantage

`PUT /subscriptions/plans/{planCode}/benefits/{benefitCode}`

### Supprimer un avantage

`DELETE /subscriptions/plans/{planCode}/benefits/{benefitCode}`

### Lister les avantages d'un plan

`GET /subscriptions/plans/{planCode}/benefits`

---

## 9. Souscriptions — Creation

### Souscrire a un plan

`POST /subscriptions`

Body :

```json
{
  "planCode": "PLAN-001",
  "holderType": "MEMBER",
  "holderCode": "MBR-000001",
  "contractCode": "CONT-202604-000001",
  "startDate": "2026-04-15",
  "autoRenew": true,
  "notes": "Abonnement mensuel bureau fixe"
}
```

Reponse `201` :

```json
{
  "subscriptionCode": "SUB-202604-000001",
  "planCode": "PLAN-001",
  "planName": "Coworking Essentiel",
  "holderType": "MEMBER",
  "holderCode": "MBR-000001",
  "holderName": "Jean Dupont",
  "contractCode": "CONT-202604-000001",
  "status": "DRAFT",
  "startDate": "2026-04-15",
  "endDate": null,
  "nextBillingDate": "2026-05-15",
  "lastBillingDate": null,
  "totalPaid": 0,
  "autoRenew": true,
  "notes": "Abonnement mensuel bureau fixe",
  "createdAt": "2026-04-14T09:00:00Z"
}
```

---

## 10. Souscriptions — Transitions de statut

### Activer une souscription

`PATCH /subscriptions/{subscriptionCode}/activate`

Comportement :

- genere une premiere facture si `billingCycle != ONE_TIME`
- initialise les `SubscriptionEntitlement` pour la premiere periode
- passe le statut a `ACTIVE`

### Suspendre

`PATCH /subscriptions/{subscriptionCode}/suspend`

Body :

```json
{
  "suspendedBy": "MANAGER-001",
  "suspensionReason": "Impaye cycle precedent"
}
```

Comportement :

- les reservations futures sont bloquees
- les entitlements restants sont conserves mais gelés

### Reserver

`PATCH /subscriptions/{subscriptionCode}/reactivate`

Body :

```json
{
  "reactivatedBy": "MANAGER-001"
}
```

Comportement : remet les entitlements a disposition.

### Annuler

`PATCH /subscriptions/{subscriptionCode}/cancel`

Body :

```json
{
  "cancelledBy": "MBR-000001",
  "cancellationReason": "Depart de l'espace"
}
```

### Renouveler manuellement

`PATCH /subscriptions/{subscriptionCode}/renew`

Usage : declencher un renouvellement hors cycle automatique.

---

## 11. Souscriptions — Droits et historique

### Consulter les droits de la periode courante

`GET /subscriptions/{subscriptionCode}/entitlements`

Reponse :

```json
[
  {
    "benefitCode": "BEN-001",
    "benefitType": "RESOURCE_ACCESS",
    "resourceTypeCode": "OPEN_SPACE",
    "description": "Acces illimite open space",
    "periodStart": "2026-04-15",
    "periodEnd": "2026-05-14",
    "allocatedQuantity": null,
    "usedQuantity": null,
    "remainingQuantity": null,
    "unit": null,
    "unlimited": true
  },
  {
    "benefitCode": "BEN-002",
    "benefitType": "RESOURCE_HOURS",
    "resourceTypeCode": "MEETING_ROOM",
    "description": "10 heures de salle de reunion",
    "periodStart": "2026-04-15",
    "periodEnd": "2026-05-14",
    "allocatedQuantity": 10,
    "usedQuantity": 3.5,
    "remainingQuantity": 6.5,
    "unit": "HOUR",
    "unlimited": false
  }
]
```

Usage frontend :

- afficher les credits restants avant la reservation
- alerter si credits insuffisants
- proposer le passage en mode `DIRECT` ou par `PASS` si credits epuises

### Historique des statuts

`GET /subscriptions/{subscriptionCode}/history`

Reponse :

```json
[
  {
    "fromStatus": null,
    "toStatus": "DRAFT",
    "changedBy": "ADMIN",
    "reason": "Creation",
    "changedAt": "2026-04-14T09:00:00Z"
  },
  {
    "fromStatus": "DRAFT",
    "toStatus": "ACTIVE",
    "changedBy": "ADMIN",
    "reason": "Premier paiement valide",
    "changedAt": "2026-04-14T09:05:00Z"
  }
]
```

### Factures liees a la souscription

`GET /subscriptions/{subscriptionCode}/invoices`

Retour : `Page<InvoiceSummaryResponse>`

### Lister les souscriptions

`GET /subscriptions?holderCode=MBR-000001&holderType=MEMBER&status=ACTIVE&page=0&size=20`

Parametres :

- `holderCode`
- `holderType`
- `planCode`
- `status`
- `page`, `size`

---

## 12. Passes — Modeles de pass

### Creer un modele de pass

`POST /passes/templates`

Body :

```json
{
  "name": "Carnet 10 heures",
  "description": "10 heures de coworking ou salle de reunion",
  "passType": "CREDIT_HOURS",
  "price": 35000,
  "currency": "XAF",
  "totalQuantity": 10,
  "unit": "HOUR",
  "resourceTypeCode": null,
  "validityDays": 90,
  "transferable": false
}
```

Types de pass (`PassType`) :

- `CREDIT_HOURS` — credits d'heures
- `CREDIT_DAYS` — credits de journees
- `DAY_PASS` — acces illimite pour une journee
- `UNLIMITED_DAY` — acces illimite sur la duree de validite
- `MULTI_ENTRY` — nombre fixe d'entrees
- `CUSTOM` — pass libre

### Modifier un modele

`PUT /passes/templates/{templateCode}`

### Activer / desactiver un modele

`PATCH /passes/templates/{templateCode}/activate`

`PATCH /passes/templates/{templateCode}/deactivate`

### Lister les modeles

`GET /passes/templates?active=true`

Reponse :

```json
[
  {
    "templateCode": "TPL-001",
    "name": "Carnet 10 heures",
    "description": "10 heures de coworking ou salle de reunion",
    "passType": "CREDIT_HOURS",
    "price": 35000,
    "currency": "XAF",
    "totalQuantity": 10,
    "unit": "HOUR",
    "resourceTypeCode": null,
    "validityDays": 90,
    "transferable": false,
    "active": true
  }
]
```

---

## 13. Passes — Emission et gestion

### Emettre un pass

`POST /passes`

Body :

```json
{
  "templateCode": "TPL-001",
  "holderType": "MEMBER",
  "holderCode": "MBR-000001",
  "paymentCode": "PAY-202604-000001",
  "notes": "Achat via portail client"
}
```

Reponse `201` :

```json
{
  "passCode": "PASS-202604-000001",
  "templateCode": "TPL-001",
  "templateName": "Carnet 10 heures",
  "passType": "CREDIT_HOURS",
  "holderType": "MEMBER",
  "holderCode": "MBR-000001",
  "holderName": "Jean Dupont",
  "status": "ACTIVE",
  "totalQuantity": 10,
  "usedQuantity": 0,
  "remainingQuantity": 10,
  "unit": "HOUR",
  "resourceTypeCode": null,
  "issuedAt": "2026-04-14T10:00:00Z",
  "expiresAt": "2026-07-13T10:00:00Z",
  "consumedAt": null,
  "notes": "Achat via portail client"
}
```

Statuts pass (`PassStatus`) :

- `ACTIVE`
- `EXPIRED`
- `CONSUMED`
- `SUSPENDED`
- `CANCELLED`

### Consulter un pass

`GET /passes/{passCode}`

### Lister les passes d'un titulaire

`GET /passes?holderCode=MBR-000001&holderType=MEMBER&status=ACTIVE&page=0&size=20`

Parametres :

- `holderCode`, `holderType`
- `status`
- `templateCode`
- `expiringBefore` — filtrer les passes qui expirent avant une date

---

## 14. Passes — Consommation

### Consommer des credits

`POST /passes/{passCode}/consume`

Body :

```json
{
  "bookingCode": "BOK-202604-000001",
  "quantityUsed": 2,
  "usedBy": "MBR-000001",
  "notes": "2h salle de reunion"
}
```

Reponse :

```json
{
  "usageId": "USAGE-202604-000001",
  "passCode": "PASS-202604-000001",
  "bookingCode": "BOK-202604-000001",
  "quantityUsed": 2,
  "remainingBefore": 10,
  "remainingAfter": 8,
  "usedAt": "2026-04-15T09:00:00Z",
  "usedBy": "MBR-000001"
}
```

Comportement :

- verifie que `remainingQuantity >= quantityUsed`
- verifie que le pass n'est pas expire
- si `remainingAfter = 0`, passe le statut du pass a `CONSUMED`

### Inverser une consommation

`POST /passes/{passCode}/reverse-usage/{usageId}`

Body :

```json
{
  "reversedBy": "MANAGER-001",
  "reason": "Reservation annulee dans le delai"
}
```

Comportement :

- restitue `quantityUsed` au solde du pass
- si le pass etait `CONSUMED`, repasse en `ACTIVE`

### Transferer un pass

`PATCH /passes/{passCode}/transfer`

Body :

```json
{
  "newHolderType": "MEMBER",
  "newHolderCode": "MBR-000002",
  "transferredBy": "MANAGER-001",
  "reason": "Transfert suite a depart du membre initial"
}
```

Condition : le modele de pass doit avoir `transferable = true`.

### Suspendre / annuler un pass

`PATCH /passes/{passCode}/suspend`

`PATCH /passes/{passCode}/cancel`

Body :

```json
{
  "reason": "Fraude suspectee",
  "actionBy": "MANAGER-001"
}
```

---

## 15. Passes — Historique des consommations

### Consulter les usages

`GET /passes/{passCode}/usages`

Reponse :

```json
[
  {
    "usageId": "USAGE-202604-000001",
    "bookingCode": "BOK-202604-000001",
    "quantityUsed": 2,
    "remainingBefore": 10,
    "remainingAfter": 8,
    "usedAt": "2026-04-15T09:00:00Z",
    "usedBy": "MBR-000001",
    "reversed": false,
    "reversedAt": null
  },
  {
    "usageId": "USAGE-202604-000002",
    "bookingCode": "BOK-202604-000003",
    "quantityUsed": 3,
    "remainingBefore": 8,
    "remainingAfter": 5,
    "usedAt": "2026-04-16T14:00:00Z",
    "usedBy": "MBR-000001",
    "reversed": true,
    "reversedAt": "2026-04-16T15:30:00Z"
  }
]
```

---

## Workflows frontend recommandes

### Workflow reservation directe (sans pass ni souscription)

1. L'utilisateur selectionne une ressource sur le calendrier.
2. Appeler `POST /bookings/check-availability` avec la plage horaire.
3. Si disponible, afficher le recap prix et duree.
4. L'utilisateur confirme.
5. Appeler `POST /bookings` avec `paymentMode: DIRECT`.
6. Afficher la confirmation avec le `bookingCode`.
7. A la cloture (`complete`), une facture est generee automatiquement.

### Workflow reservation avec souscription

1. L'utilisateur selectionne une ressource.
2. Verifier la disponibilite.
3. Charger les souscriptions actives du membre : `GET /subscriptions?holderCode=MBR-000001&status=ACTIVE`.
4. Pour chaque souscription, verifier les entitlements : `GET /subscriptions/{code}/entitlements`.
5. Si credits suffisants, proposer de payer via la souscription.
6. Appeler `POST /bookings` avec `paymentMode: SUBSCRIPTION` et `subscriptionCode`.
7. Les credits sont debites automatiquement.

### Workflow reservation avec pass

1. L'utilisateur selectionne une ressource.
2. Verifier la disponibilite.
3. Charger les passes actifs du membre : `GET /passes?holderCode=MBR-000001&status=ACTIVE`.
4. Filtrer les passes compatibles avec le type de ressource et les credits restants.
5. Afficher les passes disponibles avec leur solde.
6. L'utilisateur choisit un pass.
7. Appeler `POST /bookings` avec `paymentMode: PASS` et `passCode`.
8. Les credits sont debites du pass.

### Workflow souscription — creation et activation

1. L'admin ou le client choisit un plan dans le catalogue.
2. Appeler `POST /subscriptions`.
3. Lier au contrat legal si present.
4. Appeler `POST /payments` pour le premier paiement.
5. Apres paiement valide, appeler `PATCH /subscriptions/{code}/activate`.
6. Les entitlements sont crees pour la premiere periode.
7. Afficher le recapitulatif des droits disponibles.

### Workflow achat d'un pass

1. L'utilisateur choisit un modele de pass dans la boutique.
2. Afficher le prix, la duree de validite et les credits inclus.
3. Declencher le paiement : `POST /payments`.
4. Apres confirmation de paiement, emettre le pass : `POST /passes`.
5. Afficher le pass avec son solde et sa date d'expiration.

### Workflow renouvellement de souscription (cote admin)

1. Ouvrir la liste des souscriptions avec `nextBillingDate <= aujourd'hui`.
2. Pour chaque souscription en retard :
   - Afficher le statut et le montant du.
   - Declencher le paiement manuellement si le renouvellement auto a echoue.
3. Apres paiement valide, les entitlements sont reinitialises automatiquement.

---

## Ecrans recommandes pour le frontend

### 1. Calendrier de reservation

Usage :

- vue principale de reservation pour les membres et clients

Contenu recommande :

- calendrier avec vue semaine ou mois
- code couleur par statut de reservation
- par ressource ou vue globale
- clic sur un creneau libre → ouverture formulaire
- clic sur une reservation existante → detail ou annulation
- indicateur en haut :
  - credits restants sur souscription active
  - passes actifs disponibles

Presentation suggeree :

- timeline horizontale par ressource
- filtres : type de ressource, groupe, zone
- barre laterale : souscription active + passes actifs avec soldes

---

### 2. Formulaire de reservation

Usage :

- creer ou modifier une reservation

Contenu recommande :

- resource pre-remplie si selectionnee depuis le calendrier
- plage horaire avec picker date + heure
- bouton `Verifier la disponibilite` (auto-appele au changement de creneau)
- section mode de paiement :
  - `Tarif standard` (DIRECT) — afficher le montant estimé
  - `Souscription` — si souscription active avec credits compatibles, afficher le solde restant
  - `Pass` — si pass actif compatible, afficher les credits restants et la date d'expiration
- section participants (optionnel)
- notes
- bouton `Confirmer la reservation`

Etats a gerer :

- creneau disponible
- creneau indisponible (afficher prochain creneau)
- credits insuffisants (suggerer tarif standard ou autre pass)
- pass expire

---

### 3. Detail d'une reservation

Usage :

- visualiser et gerer une reservation

Contenu recommande :

- en-tete avec badge de statut
- ressource, creneau, duree
- mode de paiement utilise
- montant ou credits consommes
- participants
- timeline de statut
- actions disponibles selon statut :
  - `DRAFT` : `Confirmer`, `Annuler`
  - `CONFIRMED` : `Marquer en cours`, `Annuler`, `Ajouter participant`
  - `IN_PROGRESS` : `Cloturer`, `Marquer absent`
  - `COMPLETED` : `Voir la facture`
  - `CANCELLED` / `NO_SHOW` : lecture seule

---

### 4. Mon espace membre — Reservations

Usage :

- vue personnelle des reservations du membre

Contenu recommande :

- onglets : `A venir`, `En cours`, `Passees`, `Annulees`
- pour chaque reservation :
  - ressource + creneau
  - statut (badge colore)
  - mode de paiement
  - montant ou credits utilises
  - action rapide

---

### 5. Catalogue des plans

Usage :

- portail client pour decouvrir et souscrire a un plan

Contenu recommande :

- cartes par plan avec :
  - nom et description
  - prix et cycle
  - liste des avantages (icones)
  - badge `Essai X jours` si applicable
  - bouton `Souscrire`
- comparateur optionnel si plusieurs plans publics
- filter : `MONTHLY`, `YEARLY`

Presentation suggeree :

- grille 2 a 3 colonnes
- plan recommande mis en avant (border + badge)
- switch periode de facturation (mensuel / annuel)

---

### 6. Detail et gestion d'une souscription

Usage :

- vue complete d'une souscription active

Contenu recommande :

- en-tete :
  - nom du plan
  - statut (badge)
  - date debut, prochain renouvellement
  - renouvellement auto on/off
- section droits du mois :
  - pour chaque benefit :
    - libelle
    - barre de progression (utilise / alloue)
    - credits restants
    - date de reinitialisation
- section facturation :
  - liste des factures liees
  - montant total paye
- section contrat lie (lien vers le contrat legal)
- actions :
  - `Suspendre`
  - `Annuler la souscription`
  - `Renouveler maintenant` (si suspendue ou en retard)

---

### 7. Mes passes

Usage :

- vue personnelle des passes du membre

Contenu recommande :

- liste des passes avec :
  - nom du pass
  - statut (badge + couleur)
  - barre de progression credits
  - date d'expiration (en rouge si < 7 jours)
  - actions : `Voir les usages`, `Utiliser`
- bouton `Acheter un pass`
- section expiration imminente visible en haut

---

### 8. Detail d'un pass

Usage :

- consulter les credits et les usages d'un pass

Contenu recommande :

- en-tete avec nom, statut, type, credits restants
- barre de progression visuelle (restant / total)
- date d'expiration
- historique des usages :
  - date
  - reservation liee
  - credits debites
  - inversions eventuelles
- actions :
  - `Utiliser sur une reservation`
  - si transferable : `Transferer`

---

### 9. Boutique des passes

Usage :

- portail client pour acheter des passes

Contenu recommande :

- grille des modeles de pass actifs :
  - nom et description
  - credits inclus et unite
  - duree de validite
  - prix
  - bouton `Acheter`
- filtrer par type de pass ou type de ressource

---

### 10. Administration — Gestion des plans

Usage :

- backoffice admin pour creer et maintenir les plans

Contenu recommande :

- liste des plans avec filtres (statut, visibilite)
- pour chaque plan : nom, cycle, prix, nb de souscriptions actives, statut
- fiche plan :
  - section informations generales
  - section avantages (liste avec CRUD inline)
  - onglet `Souscriptions actives`
- actions :
  - `Activer`, `Archiver`, `Dupliquer`

---

### 11. Administration — Gestion des passes

Usage :

- backoffice admin pour emettre et surveiller les passes

Contenu recommande :

- liste des modeles avec filtres (type, statut)
- liste des passes emis avec filtres (titulaire, statut, expiration)
- actions rapides : `Suspendre`, `Annuler`, `Voir les usages`
- bouton `Emettre un pass` (emission manuelle)

---

### 12. Administration — Souscriptions

Usage :

- backoffice pour gerer toutes les souscriptions

Contenu recommande :

- tableau avec filtres : plan, statut, titulaire, renouvellement a venir
- alertes : souscriptions avec nextBillingDate dans < 3 jours
- alertes : souscriptions en statut `SUSPENDED` (impaye)
- fiche souscription avec droits, historique et facturation

---

## Parcours UX recommandes

### Parcours membre — premiere visite

1. Decouvrir le catalogue des plans.
2. Comparer les plans publics.
3. Souscrire au plan choisi.
4. Payer le premier mois.
5. Voir ses droits du mois actives.
6. Faire sa premiere reservation en utilisant ses credits.

### Parcours membre — reservation rapide

1. Ouvrir le calendrier.
2. Verifier les credits disponibles (affichage en haut).
3. Cliquer sur un creneau libre.
4. Confirmer la reservation (mode de paiement suggere automatiquement selon credits disponibles).

### Parcours admin — gestion des renouvellements

1. Ouvrir la liste des souscriptions avec renouvellement imminent.
2. Identifier les impayés en statut `SUSPENDED`.
3. Contacter le client ou relancer le paiement.
4. Apres paiement, reactiver la souscription.
5. Verifier que les entitlements sont bien reinitialises.

### Parcours admin — emission d'un pass a un nouveau client

1. Ouvrir la liste des modeles de pass.
2. Choisir le modele adapte.
3. Emettre le pass au nom du client (`POST /passes`).
4. Lier au paiement correspondant.
5. Confirmer au client via notification.

---

## Recommandations de presentation

### Codes couleur utiles

- gris : draft / non commence
- bleu : confirme / en cours
- vert : complete / actif / credits suffisants
- orange : credits bas (< 20%) / expiration proche / suspendu
- rouge : rupture de credits / expire / annule / absent

### Composants utiles

- `BookingCalendar`
- `BookingStatusBadge`
- `AvailabilityIndicator`
- `CreditBalanceBar` — barre de progression credits restants
- `PassCard` — carte pass avec solde et expiration
- `SubscriptionPlanCard` — carte plan catalogue
- `EntitlementList` — liste des droits de la periode avec barres
- `PassUsageTimeline`
- `SubscriptionStatusTimeline`
- `ExpiryAlert` — alerte expiration pass imminente
- `PaymentModeSelector` — selection DIRECT / SUBSCRIPTION / PASS avec contexte

### Bonnes pratiques UI

- toujours afficher les credits restants **avant** le choix du creneau pour que le membre sache ce qu'il peut faire
- ne jamais laisser le formulaire de reservation se soumettre sans verifier la disponibilite au prealable
- distinguer clairement les credits alloues, utilises, et restants sur chaque entitlement
- afficher une alerte visuelle si un pass expire dans moins de 7 jours
- afficher le prochain renouvellement sur la souscription de facon permanente
- sur le formulaire de reservation, selectionner automatiquement le mode de paiement optimal (souscription si credits suffisants, sinon pass, sinon tarif direct)
- sur mobile, remplacer le calendrier hebdomadaire par une vue liste journaliere

---

## Proposition de structure d'ecrans

### Cote membre / client

- `BookingCalendarPage`
- `BookingFormPage`
- `BookingDetailPage`
- `MyBookingsPage`
- `PlanCatalogPage`
- `SubscriptionDetailPage`
- `PassStorePage`
- `MyPassesPage`
- `PassDetailPage`

### Cote admin / backoffice

- `AdminBookingListPage`
- `AdminBookingDetailPage`
- `AdminPlanListPage`
- `AdminPlanDetailPage`
- `AdminPlanFormPage`
- `AdminSubscriptionListPage`
- `AdminSubscriptionDetailPage`
- `AdminPassTemplateListPage`
- `AdminPassListPage`
- `AdminPassIssuePage`

---

## Priorite de construction frontend

Ordre recommande :

1. `BookingCalendarPage` + `AvailabilityIndicator`
2. `BookingFormPage` (mode DIRECT uniquement en premier)
3. `BookingDetailPage` + transitions de statut
4. `MyBookingsPage`
5. `AdminPlanListPage` + `AdminPlanFormPage` (CRUD plans + avantages)
6. `PlanCatalogPage` (portail client)
7. `AdminSubscriptionListPage` + `AdminSubscriptionDetailPage`
8. `SubscriptionDetailPage` + `EntitlementList`
9. Enrichissement `BookingFormPage` avec mode SUBSCRIPTION
10. `AdminPassTemplateListPage` + CRUD modeles de pass
11. `PassStorePage` (boutique)
12. `AdminPassIssuePage` + emission manuelle
13. `MyPassesPage` + `PassDetailPage`
14. Enrichissement `BookingFormPage` avec mode PASS
15. `CreditBalanceBar` + `ExpiryAlert` (transversaux)

---

## Cas d'erreur a gerer

- creneau indisponible au moment de la soumission (conflit entre verif et soumission)
- credits insuffisants sur la souscription lors de la reservation
- pass expire au moment de la consommation
- souscription suspendue lors d'une tentative de reservation
- annulation hors delai de politique (ResourcePolicy)
- pass non transferable lors d'une tentative de transfert
- tentative de souscription a un plan archive
- consommation de credits sur un pass annule ou suspendu
- renouvellement de souscription sans moyen de paiement disponible
