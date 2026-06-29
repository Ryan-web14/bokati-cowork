# Guide Frontend/API - Subscription, Plans, Pass et Booking

Base API: `/sni/api/v1`

Ce document decrit l'integration frontend du module subscription actuel. Il couvre les plans, subscriptions, pass, entitlements, add-ons, promotions, seats, usage, overage, rollover, timeline, notifications, metrics et le futur branchement booking/payment/invoice.

Le trial n'est pas expose dans cette version.

## Regle importante sur les codes

Les codes propres au module subscription sont generes par le backend et ne doivent pas etre saisis par l'utilisateur dans les formulaires de creation.

Codes generes par l'API:

- `plan.code`
- `entitlementDefinition.code`
- `subscriptionNumber`
- `passNumber`
- `grantNumber`
- `usageNumber`
- `changeNumber`
- `promotion.code`
- `notificationNumber`
- `eventNumber`
- `billableNumber`
- `chargeNumber`
- `rolloverNumber`
- `bookingNumber`
- `holdNumber`
- `recurrenceGroupNumber`

Exemples de formats actuels:

- `plan.code`: `PLN-COW-IND-202604-00000001`
- `entitlementDefinition.code`: `ENT-TIM-HOU-RES-PER-MEE-202604-00000001`
- `subscriptionNumber`: `SUB-MEM-COW-MON-202604-00000001`

Codes qui restent envoyes par le frontend car ils referencent une donnee existante:

- `planCode`
- `subscriptionNumber`
- `entitlementCode`
- `ownerCode`
- `subscriberCode`
- `memberCode`
- `resourceTypeCode`
- `resourceGroupCode`
- `sourceId`
- `referenceId`

Pour un formulaire, ne jamais afficher un champ editable pour un code genere. Apres creation, utiliser le code retourne par la reponse pour les prochaines operations.

Regle specifique booking: le frontend ne doit pas envoyer `ownerCode`, `subscriptionNumber`, `passNumber` ou `entitlementCode` dans les payloads de reservation. Il envoie seulement un selecteur d'identite (`memberId`, `customerId`, `businessCode`, `email`, `phone` ou `walkIn=true`) et `paymentMode`. Le backend resout ensuite le proprietaire, la subscription active, le pass utilisable et l'entitlement compatible avec la ressource.

## Format des listes

Les listes paginees retournent un `PaginatedResponse<T>`. Toujours envoyer les parametres standards Spring si besoin:

- `page`
- `size`
- `sort`

Exemple:

`GET /sni/api/v1/subscriptions/plans?page=0&size=20&sort=createdAt,desc`

## Ecrans frontend recommandes

- Catalogue plans: `/subscriptions/plans`
- Detail plan: `/subscriptions/plans/:planCode`
- Creation/edition version plan: `/subscriptions/plans/:planCode/versions`
- Liste subscriptions: `/subscriptions`
- Detail subscription: `/subscriptions/:subscriptionNumber`
- Pass: `/passes`
- Detail pass: `/passes/:passNumber`
- Promotions: `/promotions`
- Usage records: `/usage-records`
- Metrics: `/subscription-metrics`
- Booking: `/bookings`

Onglets recommandes sur le detail subscription:

- Resume
- Entitlements
- Billing schedule
- Billable items
- Usage
- Add-ons
- Seats
- Changes
- Timeline
- Notifications
- Overage
- Rollover

## Plans et catalogue

### Creer un plan

`POST /sni/api/v1/subscriptions/plans`

Le champ `code` ne doit pas etre envoye.

```json
{
  "name": "Coworking Essentiel",
  "description": "Acces open space avec credits de reservation",
  "planType": "COWORKING_ACCESS",
  "targetAudience": "INDIVIDUAL",
  "visible": false,
  "sortOrder": 1
}
```

Reponse principale: `PlanResponse`

- `code`: genere par l'API
- `status`: `DRAFT` a la creation
- `activeVersion`: null tant qu'aucune version n'est publiee

### Mettre a jour un plan

`PUT /sni/api/v1/subscriptions/plans/{planCode}`

Le champ `code` reste genere par le backend et ne doit pas etre modifie.
Le backend accepte aussi une mise a jour partielle: seuls les champs fournis sont modifies.

```json
{
  "name": "Coworking Essentiel Plus",
  "description": "Acces open space avec credits et priorite de reservation",
  "planType": "COWORKING_ACCESS",
  "targetAudience": "INDIVIDUAL",
  "visible": true,
  "sortOrder": 2
}
```

Effet frontend:

- met a jour les metadonnees du plan
- conserve le `code`, le `status` et les versions existantes

### Creer une version de plan

`POST /sni/api/v1/subscriptions/plans/{planCode}/versions`

```json
{
  "name": "Version mensuelle 2026",
  "description": "Tarif public mensuel",
  "effectiveFrom": "2026-04-15",
  "effectiveTo": null,
  "termsJson": "{\"commitment\":\"monthly\"}",
  "prices": [
    {
      "billingCycle": "MONTHLY",
      "currency": "XAF",
      "amount": 50000,
      "setupFee": 0,
      "depositAmount": 0,
      "taxIncluded": true,
      "commitmentMonths": 1
    }
  ],
  "benefits": [
    {
      "title": "Open space",
      "description": "Acces open space en heures ouvrables",
      "icon": "workspace",
      "category": "ACCESS",
      "displayOrder": 1,
      "highlighted": true,
      "included": true,
      "metadataJson": null
    }
  ],
  "entitlements": [
    {
      "entitlementCode": "ENT-TIM-HOU-RES-PER-MEE-202604-00000001",
      "quantity": 10,
      "unlimited": false,
      "rolloverAllowed": true,
      "rolloverLimit": 5,
      "validForDays": 30,
      "priority": 1,
      "restrictionsJson": null
    }
  ]
}
```

`entitlementCode` est un code retourne par `POST /subscriptions/entitlement-definitions`.

### Publier une version

`PATCH /sni/api/v1/subscriptions/plans/{planCode}/versions/{versionId}/publish`

Important frontend:

- traiter `PlanVersionResponse.id` comme une chaine opaque, pas comme un `number` JavaScript
- les IDs backend peuvent depasser `Number.MAX_SAFE_INTEGER`
- reutiliser la valeur exacte retournee par l'API dans l'URL `{versionId}`

Effet frontend:

- la version devient `ACTIVE`
- l'ancienne version active est archivee
- le plan devient visible et actif

### Lister et consulter

- `GET /sni/api/v1/subscriptions/plans`
- `GET /sni/api/v1/subscriptions/plans/{planCode}`

Filtres:

- `code`
- `name`
- `planType`
- `targetAudience`
- `status`
- `visible`

Enums utiles:

- `PlanType`: `MEMBERSHIP`, `COWORKING_ACCESS`, `DEDICATED_DESK`, `PRIVATE_OFFICE`, `MEETING_ROOM_PACK`, `VIRTUAL_OFFICE`, `COMPANY_PLAN`, `CUSTOM`
- `TargetAudience`: `INDIVIDUAL`, `COMPANY`, `BOTH`
- `PlanStatus`: `DRAFT`, `ACTIVE`, `ARCHIVED`
- `BillingCycle`: `ONE_TIME`, `DAILY`, `WEEKLY`, `MONTHLY`, `QUARTERLY`, `YEARLY`

## Entitlement definitions

### Creer une definition de droit

`POST /sni/api/v1/subscriptions/entitlement-definitions`

Le champ `code` ne doit pas etre envoye. Le backend genere le code.

```json
{
  "name": "Heures salle de reunion",
  "description": "Credit consommable pour reservations de salles",
  "entitlementType": "TIME",
  "unit": "HOUR",
  "consumptionMode": "RESERVE_THEN_CONSUME",
  "resetPolicy": "PER_BILLING_CYCLE",
  "stackable": true,
  "transferable": false,
  "resourceTypeCode": "MEETING_ROOM",
  "resourceGroupCode": null,
  "metadataJson": null,
  "active": true
}
```

Reponse: `EntitlementDefinitionResponse`, avec `code` genere.

### Lister les definitions

`GET /sni/api/v1/subscriptions/entitlement-definitions`

Recherche basique trigram:

- `GET /sni/api/v1/subscriptions/entitlement-definitions/search/basic?query=meeting`
- la recherche `pg_trgm` porte en priorite sur `code` et `name`
- `description`, `resourceTypeCode` et `resourceGroupCode` participent aussi au score

Filtres avances disponibles sur `GET /sni/api/v1/subscriptions/entitlement-definitions`:

- `query`
- `code`
- `name`
- `entitlementType`
- `unit`
- `consumptionMode`
- `resetPolicy`
- `resourceTypeCode`
- `resourceGroupCode`
- `stackable`
- `transferable`
- `active`

### Consulter une definition

`GET /sni/api/v1/subscriptions/entitlement-definitions/{code}`

Enums:

- `EntitlementType`: `ACCESS`, `CREDIT`, `TIME`, `DISCOUNT`, `QUOTA`, `FEATURE_FLAG`, `SERVICE_ALLOWANCE`
- `EntitlementUnit`: `DAY`, `HOUR`, `CREDIT`, `BOOKING`, `VISIT`, `MEMBER`, `PERCENT`, `AMOUNT`, `BOOLEAN`
- `ConsumptionMode`: `CHECK_ONLY`, `RESERVABLE`, `CONSUMABLE`, `RESERVE_THEN_CONSUME`
- `EntitlementResetPolicy`: `NEVER`, `DAILY`, `WEEKLY`, `MONTHLY`, `PER_BILLING_CYCLE`

## Subscriptions

### Creer une subscription

`POST /sni/api/v1/subscriptions`

`subscriptionNumber` est genere par le backend.

```json
{
  "planCode": "PLN-COW-IND-202604-00000001",
  "planVersionId": 123,
  "billingCycle": "MONTHLY",
  "subscriberType": "MEMBER",
  "subscriberCode": "MBR-000001",
  "startDate": "2026-04-15",
  "autoRenew": true,
  "metadataJson": "{\"source\":\"backoffice\"}"
}
```

Notes:

- `planCode` reference un plan existant.
- `planVersionId` est optionnel si le backend peut resoudre la version active.
- `subscriberCode` reference un membre, client ou business entity existant.

### Consultation

- `GET /sni/api/v1/subscriptions/{subscriptionNumber}`
- `GET /sni/api/v1/subscriptions/current?subscriberType=MEMBER&subscriberCode=MBR-000001`
- `GET /sni/api/v1/subscriptions`

Filtres liste:

- `subscriberType`
- `subscriberCode`
- `planCode`
- `status`
- `nextBillingBefore`

### Actions cycle de vie

- `PATCH /sni/api/v1/subscriptions/{subscriptionNumber}/activate`
- `PATCH /sni/api/v1/subscriptions/{subscriptionNumber}/suspend`
- `PATCH /sni/api/v1/subscriptions/{subscriptionNumber}/cancel`
- `PATCH /sni/api/v1/subscriptions/{subscriptionNumber}/renew`

Body optionnel pour activate/suspend/cancel:

```json
{
  "reason": "Premier paiement valide",
  "changedBy": "ADMIN-001"
}
```

### Donnees liees

- `GET /sni/api/v1/subscriptions/{subscriptionNumber}/entitlements`
- `GET /sni/api/v1/subscriptions/{subscriptionNumber}/history`
- `GET /sni/api/v1/subscriptions/{subscriptionNumber}/billing-schedule`

Enums:

- `SubscriberType`: `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY`
- `SubscriptionStatus`: `DRAFT`, `PENDING_ACTIVATION`, `ACTIVE`, `PAST_DUE`, `SUSPENDED`, `CANCELLED`, `EXPIRED`

`TRIALING` existe dans l'enum technique mais ne doit pas etre expose comme parcours UI dans cette version.

## Entitlements et booking

Ces endpoints sont prevus pour les services internes et pour le futur module booking. Le frontend peut les utiliser uniquement si le backoffice expose une action manuelle de verification ou correction.

### Lire les grants et soldes

- `GET /sni/api/v1/entitlements/grants`
- `GET /sni/api/v1/entitlements/balances?ownerType=MEMBER&ownerCode=MBR-000001`

Filtres grants:

- `ownerType`
- `ownerCode`
- `entitlementCode`
- `status`
- `validAt`

### Operations

- `POST /sni/api/v1/internal/entitlements/check`
- `POST /sni/api/v1/internal/entitlements/reserve`
- `POST /sni/api/v1/internal/entitlements/consume`
- `POST /sni/api/v1/internal/entitlements/release`
- `POST /sni/api/v1/internal/entitlements/refund`

Payload:

```json
{
  "ownerType": "MEMBER",
  "ownerCode": "MBR-000001",
  "entitlementCode": "ENT-TIM-HOU-RES-PER-MEE-202604-00000001",
  "quantity": 2,
  "referenceType": "BOOKING",
  "referenceId": "BKG-202604-000001",
  "idempotencyKey": "booking:BKG-202604-000001:reserve",
  "reason": "Reservation salle de reunion"
}
```

Workflow booking recommande:

1. Verifier la disponibilite de la ressource dans booking.
2. Appeler `check` avec `referenceType=BOOKING`.
3. A la confirmation booking, appeler `reserve`.
4. A la fin effective du booking, appeler `consume`.
5. Si booking annule avant consommation, appeler `release`.
6. Si correction apres consommation, appeler `refund`.

Reponse:

```json
{
  "allowed": true,
  "entitlementCode": "ENT-TIM-HOU-RES-PER-MEE-202604-00000001",
  "requestedQuantity": 2,
  "availableQuantity": 8,
  "message": "Entitlement reserved"
}
```

## Passes

### Creer un pass

`POST /sni/api/v1/passes`

`passNumber` est genere par le backend.

```json
{
  "passType": "TIME_PACK",
  "ownerType": "MEMBER",
  "ownerCode": "MBR-000001",
  "subscriptionNumber": null,
  "planVersionId": 123,
  "name": "Pack 10 heures",
  "description": "Credits valables 90 jours",
  "validFrom": "2026-04-15T08:00:00Z",
  "validUntil": "2026-07-14T08:00:00Z",
  "transferable": false,
  "shareable": false,
  "maxUses": 10,
  "metadataJson": null,
  "entitlements": [
    {
      "entitlementCode": "ENT-TIM-HOU-RES-PER-MEE-202604-00000001",
      "quantity": 10,
      "unlimited": false,
      "validFrom": "2026-04-15T08:00:00Z",
      "validUntil": "2026-07-14T08:00:00Z"
    }
  ]
}
```

### Consultation et annulation

- `GET /sni/api/v1/passes/{passNumber}`
- `GET /sni/api/v1/passes`
- `PATCH /sni/api/v1/passes/{passNumber}/cancel`

Filtres:

- `ownerType`
- `ownerCode`
- `passType`
- `status`
- `expiringBefore`

Payload cancel:

```json
{
  "reason": "Demande client"
}
```

Enums:

- `PassType`: `DAY_PASS`, `TIME_PACK`, `VISITOR_PASS`, `MEETING_ROOM_PACK`, `PROMOTIONAL_PASS`, `SUBSCRIPTION_PASS`, `COMPANY_SHARED_PASS`, `CUSTOM`

## Booking

### Resolution d'identite

Les formulaires booking doivent utiliser un seul selecteur principal:

- `memberId` pour un membre connu
- `customerId` pour un client connu
- `businessCode` pour une entreprise connue
- `email` ou `phone` pour laisser le backend retrouver le membre/client/entreprise
- `walkIn=true` avec les infos de contact pour un passant sans compte

Si `walkIn=true`, le backend genere un code temporaire de type invite booking. Ce code n'est pas saisi par l'utilisateur.

### Verifier une disponibilite

`POST /sni/api/v1/bookings/check-availability`

```json
{
  "resourceCode": "ROOM-001",
  "memberId": "MBR-000001",
  "paymentMode": "SUBSCRIPTION",
  "startedAt": "2026-04-16T10:00:00",
  "endedAt": "2026-04-16T12:00:00",
  "quantity": 1
}
```

Pour un passant:

```json
{
  "resourceCode": "ROOM-001",
  "walkIn": true,
  "contactName": "Client passage",
  "contactEmail": "client@example.com",
  "contactPhone": "+237600000000",
  "paymentMode": "DIRECT",
  "startedAt": "2026-04-16T10:00:00",
  "endedAt": "2026-04-16T12:00:00",
  "quantity": 1
}
```

### Creer un hold

`POST /sni/api/v1/bookings/holds`

```json
{
  "resourceCode": "ROOM-001",
  "memberId": "MBR-000001",
  "startedAt": "2026-04-16T10:00:00",
  "endedAt": "2026-04-16T12:00:00",
  "quantity": 1,
  "ttlMinutes": 10,
  "idempotencyKey": "front:hold:ROOM-001:202604161000"
}
```

### Creer une reservation

`POST /sni/api/v1/bookings`

```json
{
  "resourceCode": "ROOM-001",
  "memberId": "MBR-000001",
  "holdNumber": "BKH-202604-000001",
  "idempotencyKey": "front:booking:ROOM-001:202604161000",
  "startedAt": "2026-04-16T10:00:00",
  "endedAt": "2026-04-16T12:00:00",
  "quantity": 1,
  "paymentMode": "SUBSCRIPTION",
  "confirmImmediately": true,
  "sendEmail": true,
  "notes": "Besoin video projecteur",
  "metadataJson": null,
  "participants": [
    {
      "memberCode": null,
      "name": "Invite externe",
      "email": "invite@example.com",
      "role": "GUEST"
    }
  ]
}
```

Pour `paymentMode=SUBSCRIPTION`, le backend selectionne la subscription active et l'entitlement compatible avec la ressource. Pour `paymentMode=PASS`, il selectionne un pass actif ayant un grant compatible. Pour `paymentMode=DIRECT`, il cree la ligne facturable.

### Recurrence, approval et check-in

- `POST /sni/api/v1/bookings/recurring`
- `PATCH /sni/api/v1/bookings/{bookingNumber}/approve`
- `PATCH /sni/api/v1/bookings/{bookingNumber}/reject`
- `PATCH /sni/api/v1/bookings/{bookingNumber}/check-in`
- `PATCH /sni/api/v1/bookings/{bookingNumber}/check-out`

### Quota override

`POST /sni/api/v1/bookings/quota-overrides`

Le frontend ne fournit pas `ownerCode`; il selectionne le membre/client/entreprise.

```json
{
  "memberId": "MBR-000001",
  "resourceCode": "ROOM-001",
  "extraActiveBookings": 1,
  "extraBookingsPerDay": 2,
  "reason": "Deblocage ponctuel",
  "approvedBy": "ADMIN-001",
  "validFrom": "2026-04-15T08:00:00Z",
  "validUntil": "2026-04-30T18:00:00Z"
}
```

## Add-ons

### Ajouter un add-on

`POST /sni/api/v1/subscriptions/{subscriptionNumber}/addons`

```json
{
  "planVersionId": 456,
  "quantity": 2,
  "startsAt": "2026-04-15",
  "endsAt": null,
  "metadataJson": null
}
```

### Lister et annuler

- `GET /sni/api/v1/subscriptions/addons`
- `PATCH /sni/api/v1/subscriptions/addons/{addonId}/cancel`

Filtres:

- `subscriptionNumber`
- `planCode`
- `status`

## Promotions

### Creer une promotion

`POST /sni/api/v1/promotions`

Le champ `code` ne doit pas etre envoye. Le backend genere le code.

```json
{
  "name": "Remise lancement",
  "description": "Reduction de 10%",
  "discountType": "PERCENTAGE",
  "discountValue": 10,
  "startsAt": "2026-04-15T00:00:00Z",
  "endsAt": "2026-05-15T00:00:00Z",
  "maxRedemptions": 100,
  "metadataJson": null
}
```

### Activer et appliquer

- `PATCH /sni/api/v1/promotions/{code}/activate`
- `POST /sni/api/v1/promotions/{code}/redeem`

Payload redeem:

```json
{
  "subscriberType": "MEMBER",
  "subscriberCode": "MBR-000001",
  "subscriptionNumber": "SUB-MEM-COW-MON-202604-00000001"
}
```

### Lister

`GET /sni/api/v1/promotions?code=PRO-00001&status=ACTIVE`

Enums:

- `DiscountType`: `PERCENTAGE`, `FIXED_AMOUNT`, `FREE_ENTITLEMENT`, `WAIVE_SETUP_FEE`

Ne pas exposer `FREE_TRIAL_DAYS` dans l'UI; le backend le rejette dans cette version.

## Seats

### Ajouter un seat

`POST /sni/api/v1/subscriptions/{subscriptionNumber}/seats`

```json
{
  "memberCode": "MBR-000002",
  "role": "MEMBER"
}
```

### Lister et retirer

- `GET /sni/api/v1/subscriptions/seats`
- `DELETE /sni/api/v1/subscriptions/{subscriptionNumber}/seats/{memberCode}`

Filtres:

- `subscriptionNumber`
- `memberCode`
- `status`

Roles:

- `OWNER`
- `ADMIN`
- `MEMBER`
- `GUEST`

## Change requests

### Demander un changement

`POST /sni/api/v1/subscriptions/{subscriptionNumber}/changes`

```json
{
  "changeType": "UPGRADE",
  "targetPlanVersionId": 456,
  "effectivePolicy": "NEXT_BILLING_PERIOD",
  "effectiveDate": null,
  "prorationAmount": 0,
  "reason": "Upgrade client",
  "requestedBy": "ADMIN-001"
}
```

### Approuver et appliquer

- `PATCH /sni/api/v1/subscriptions/changes/{changeNumber}/approve`
- `PATCH /sni/api/v1/subscriptions/changes/{changeNumber}/apply`
- `GET /sni/api/v1/subscriptions/changes`

Payload approve:

```json
{
  "approvedBy": "ADMIN-001"
}
```

Enums:

- `SubscriptionChangeType`: `UPGRADE`, `DOWNGRADE`, `ADD_ADDON`, `REMOVE_ADDON`, `QUANTITY_CHANGE`
- `SubscriptionChangeEffectivePolicy`: `IMMEDIATE`, `NEXT_BILLING_PERIOD`, `CUSTOM_DATE`

## Usage records et overage

### Enregistrer un usage

`POST /sni/api/v1/usage-records`

```json
{
  "ownerType": "MEMBER",
  "ownerCode": "MBR-000001",
  "entitlementCode": "ENT-TIM-HOU-RES-PER-MEE-202604-00000001",
  "quantity": 3,
  "unit": "HOUR",
  "referenceType": "BOOKING",
  "referenceId": "BKG-202604-000001",
  "consumeEntitlement": true,
  "billable": true,
  "billableAmount": 15000,
  "currency": "XAF",
  "occurredAt": "2026-04-15T10:00:00Z",
  "metadataJson": null
}
```

Comportement:

- si le solde suffit, l'usage consomme les entitlements
- si le solde ne suffit pas, la policy overage decide: `BLOCK`, `BILLABLE`, `ALLOW_UNBILLED`
- si `BILLABLE`, la reponse contient un `billableNumber`

### Lister les usages

`GET /sni/api/v1/usage-records`

Filtres:

- `ownerType`
- `ownerCode`
- `entitlementCode`
- `referenceType`
- `referenceId`
- `status`

### Creer une policy overage

`POST /sni/api/v1/subscription-overage/policies`

```json
{
  "planVersionId": 123,
  "entitlementCode": "ENT-TIM-HOU-RES-PER-MEE-202604-00000001",
  "mode": "BILLABLE",
  "unitPrice": 5000,
  "currency": "XAF",
  "freeQuantity": 0,
  "metadataJson": null,
  "active": true
}
```

### Lister policies et charges

- `GET /sni/api/v1/subscription-overage/policies`
- `GET /sni/api/v1/subscription-overage/charges`

Filtres charges:

- `subscriptionNumber`
- `ownerType`
- `ownerCode`
- `entitlementCode`

## Rollover

### Appliquer les rollovers dus

`POST /sni/api/v1/subscription-rollovers/apply-due`

Le frontend backoffice peut proposer cette action manuelle. Un worker doit l'appeler automatiquement cote backend.

### Lister

`GET /sni/api/v1/subscription-rollovers?subscriptionNumber=SUB-MEM-COW-MON-202604-00000001&entitlementCode=ENT-TIM-HOU-RES-PER-MEE-202604-00000001`

## Timeline

### Creer un evenement manuel

`POST /sni/api/v1/subscription-timeline`

```json
{
  "subscriptionNumber": "SUB-MEM-COW-MON-202604-00000001",
  "ownerType": "MEMBER",
  "ownerCode": "MBR-000001",
  "eventType": "CONTRACT_BOUND",
  "sourceType": "CONTRACT",
  "sourceId": "CTR-202604-000001",
  "title": "Contrat lie",
  "description": "Contrat signe et attache a la souscription",
  "payloadJson": null,
  "occurredAt": "2026-04-15T12:00:00Z"
}
```

### Lister la timeline

`GET /sni/api/v1/subscription-timeline`

Filtres:

- `subscriptionNumber`
- `ownerType`
- `ownerCode`
- `eventType`
- `occurredFrom`
- `occurredTo`

## Notifications

### Mettre une notification en file

`POST /sni/api/v1/subscription-notifications`

```json
{
  "subscriptionNumber": "SUB-MEM-COW-MON-202604-00000001",
  "ownerType": "MEMBER",
  "ownerCode": "MBR-000001",
  "notificationType": "RENEWAL_UPCOMING",
  "channel": "EMAIL",
  "recipient": "client@example.com",
  "subject": "Renouvellement abonnement",
  "body": "Votre abonnement arrive a renouvellement.",
  "payloadJson": null,
  "scheduledAt": "2026-04-20T08:00:00Z"
}
```

### Actions

- `PATCH /sni/api/v1/subscription-notifications/{notificationNumber}/cancel`
- `POST /sni/api/v1/subscription-notifications/dispatch-due?limit=100`
- `GET /sni/api/v1/subscription-notifications`

Filtres:

- `subscriptionNumber`
- `ownerType`
- `ownerCode`
- `notificationType`
- `status`

Canaux:

- `EMAIL`
- `SMS`
- `WHATSAPP`
- `IN_APP`
- `WEBHOOK`

## Billing bridge, payment et invoice

### Billable items

`GET /sni/api/v1/billable-items`

Filtres:

- `status`
- `subscriberType`
- `subscriberCode`
- `sourceType`
- `sourceId`

Usage frontend:

- afficher les lignes facturables non encore facturees
- connecter ensuite ces lignes au module invoice
- utiliser `billableNumber` comme reference publique

### Integration payment/invoice a venir

Le module subscription prepare les donnees de facturation avec `BillingSchedule` et `BillableItem`.

Flux cible:

1. Subscription cree ou renouvelle les schedules et billable items.
2. Payment confirme le paiement.
3. Invoice transforme les billable items en facture.
4. Subscription active, renouvelle ou suspend selon le resultat payment/invoice.

Le frontend ne doit pas creer directement de facture depuis subscription. Il doit afficher les billable items et rediriger vers les futurs ecrans invoice/payment.

## Metrics

`GET /sni/api/v1/subscription-metrics/overview`

Champs utiles:

- `activeSubscriptions`
- `pendingSubscriptions`
- `suspendedSubscriptions`
- `cancelledSubscriptions`
- `activePasses`
- `activeAddons`
- `activePromotions`
- `pendingBillableItems`
- `pendingBillableAmount`
- `activeSubscriptionMrr`
- `recordedUsageQuantity`
- `billableUsageQuantity`
- `overageCharges`
- `overageAmount`
- `rolloverRecords`
- `rolloverQuantity`

## Etats UX a gerer

- `400`: payload invalide, champ manquant ou valeur enum incorrecte
- `404`: code de reference inexistant (`planCode`, `entitlementCode`, `subscriptionNumber`, etc.)
- `409`: conflit metier, par exemple solde insuffisant, promotion deja utilisee, maximum atteint
- `500`: erreur inattendue

Cas importants:

- plan sans version active: ne pas proposer la souscription client
- subscription suspendue: bloquer le paiement par entitlement dans booking
- entitlement insuffisant: proposer overage si la policy le permet, sinon paiement direct
- pass expire ou annule: ne pas proposer dans booking
- promotion inactive ou expiree: masquer ou afficher comme non utilisable
- rollover applique: mettre a jour les soldes et timeline

## Priorite de construction frontend

1. Plans + entitlement definitions
2. Creation et detail subscription
3. Entitlement balances
4. Passes
5. Usage records
6. Add-ons
7. Promotions
8. Seats
9. Change requests
10. Timeline
11. Notifications
12. Overage
13. Rollover
14. Metrics
15. Integration booking/payment/invoice
