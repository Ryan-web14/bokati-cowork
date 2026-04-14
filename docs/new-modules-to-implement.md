# Nouveaux modules a implementer

## But

Ce document liste uniquement des nouveaux modules a lancer dans le projet.

Il ne parle pas des modules deja existants ou deja commences comme:

- `client/customer`
- `client/member`
- `company`
- `document`
- `ressource`

L'objectif est d'identifier les prochains vrais blocs fonctionnels a ajouter pour faire evoluer la plateforme.

---

## Ordre recommande

1. module `booking / reservation`
2. module `billing / invoicing`
3. module `payment / wallet`
4. module `contract / subscription`
5. module `notification center`
6. module `support / ticketing`
7. module `analytics / reporting`
8. module `organization / tenant settings`

---

## 1. Module Booking / Reservation

### Pourquoi ce module

Le projet contient deja un socle `ressource` avec:

- resources
- policies
- closures
- pricing rules

Le module naturel suivant est donc la reservation.

### Objectif

Permettre:

- reserver une ressource
- verifier les disponibilites
- gerer les annulations
- gerer les conflits et blocages
- preparer la facturation des usages

### Sous-modules proposes

- `booking`
- `booking-slot`
- `booking-conflict`
- `booking-participant`
- `booking-history`

### Endpoints cibles

- `POST /bookings`
- `GET /bookings/{bookingId}`
- `GET /bookings`
- `POST /bookings/search`
- `POST /bookings/check-availability`
- `PATCH /bookings/{bookingId}/confirm`
- `PATCH /bookings/{bookingId}/cancel`
- `PATCH /bookings/{bookingId}/complete`

### Entites metier envisagees

- `Booking`
- `BookingLine`
- `BookingParticipant`
- `BookingStatusHistory`
- `AvailabilitySlot`

### Dependances metier

- `resource`
- `resource_policy`
- `resource_closure`
- `customer`
- `member`

---

## 2. Module Billing / Invoicing

### Pourquoi ce module

Le projet contient deja:

- `BillingEntityType`
- `ReceiptPreference`
- preferences utilisateur de facturation
- base `business_entity`

Le module de facturation est donc un prolongement logique.

### Objectif

Permettre:

- generer des factures
- calculer les montants a partir des reservations et services
- gerer taxes, remises, statuts de paiement
- produire des recus et documents financiers

### Sous-modules proposes

- `invoice`
- `invoice-line`
- `credit-note`
- `tax-rule`
- `receipt`

### Endpoints cibles

- `POST /invoices/generate`
- `GET /invoices/{invoiceId}`
- `GET /invoices`
- `POST /invoices/search`
- `PATCH /invoices/{invoiceId}/issue`
- `PATCH /invoices/{invoiceId}/cancel`
- `PATCH /invoices/{invoiceId}/mark-paid`
- `GET /invoices/{invoiceId}/pdf`

### Entites metier envisagees

- `Invoice`
- `InvoiceLine`
- `InvoicePayment`
- `CreditNote`
- `TaxRule`

### Dependances metier

- `booking`
- `customer`
- `member`
- `business_entity`
- `currency`

---

## 3. Module Payment / Wallet

### Pourquoi ce module

Le projet contient deja des DTOs autour des preferences wallet utilisateur. Il manque la vraie couche transactionnelle.

### Objectif

Permettre:

- enregistrer des paiements
- suivre les transactions
- gerer un wallet/prepaid balance
- brancher plusieurs moyens de paiement

### Sous-modules proposes

- `payment`
- `payment-method`
- `wallet`
- `wallet-transaction`
- `refund`

### Endpoints cibles

- `POST /payments`
- `GET /payments/{paymentId}`
- `POST /payments/webhook`
- `POST /wallets/top-up`
- `GET /wallets/{customerId}`
- `GET /wallets/{customerId}/transactions`
- `POST /refunds`

### Entites metier envisagees

- `Payment`
- `PaymentAttempt`
- `PaymentProviderEvent`
- `Wallet`
- `WalletTransaction`

### Dependances metier

- `invoice`
- `customer`
- `member`
- `user settings`

---

## 4. Module Contract / Subscription

### Pourquoi ce module

Le coworking demande souvent:

- abonnements
- plans mensuels
- contrats entreprise
- renouvellements

Ce module manque aujourd'hui.

### Objectif

Permettre:

- gerer les abonnements des clients
- gerer les contrats et conditions
- suivre les periodes de validite
- lier des droits d'acces ou de reservation

### Sous-modules proposes

- `subscription-plan`
- `subscription`
- `contract`
- `contract-amendment`
- `benefit-rule`

### Endpoints cibles

- `POST /subscriptions`
- `GET /subscriptions/{subscriptionId}`
- `POST /subscriptions/search`
- `PATCH /subscriptions/{subscriptionId}/activate`
- `PATCH /subscriptions/{subscriptionId}/suspend`
- `PATCH /subscriptions/{subscriptionId}/renew`
- `POST /contracts`
- `GET /contracts/{contractId}`

### Entites metier envisagees

- `SubscriptionPlan`
- `Subscription`
- `Contract`
- `ContractVersion`
- `PlanBenefit`

### Dependances metier

- `customer`
- `member`
- `business_entity`
- `document`
- `billing`

---

## 5. Module Notification Center

### Pourquoi ce module

Le projet contient deja des bases:

- `NotificationChannel`
- preferences utilisateur
- service mail

Il manque un centre de notification central et exploitable.

### Objectif

Permettre:

- centraliser les notifications
- suivre leur statut
- gerer email, SMS, in-app, push
- historiser les envois

### Sous-modules proposes

- `notification`
- `notification-template`
- `notification-event`
- `notification-delivery`
- `in-app-notification`

### Endpoints cibles

- `GET /notifications`
- `GET /notifications/unread`
- `PATCH /notifications/{id}/read`
- `POST /notifications/test`
- `GET /notification-templates`
- `PUT /notification-templates/{code}`

### Entites metier envisagees

- `Notification`
- `NotificationTemplate`
- `NotificationDelivery`
- `NotificationPreference`
- `NotificationEvent`

### Dependances metier

- `member`
- `customer`
- `authentication`
- `booking`
- `billing`

---

## 6. Module Support / Ticketing

### Pourquoi ce module

Une plateforme B2B/B2C de coworking gagne vite a avoir une gestion de demandes:

- incidents
- demandes de support
- questions de facturation
- demandes liees aux reservations

### Objectif

Permettre:

- creer des tickets
- suivre les statuts
- assigner a des agents
- ajouter des commentaires et pieces jointes

### Sous-modules proposes

- `ticket`
- `ticket-comment`
- `ticket-attachment`
- `ticket-category`
- `ticket-assignment`

### Endpoints cibles

- `POST /tickets`
- `GET /tickets/{ticketId}`
- `GET /tickets`
- `POST /tickets/search`
- `POST /tickets/{ticketId}/comments`
- `PATCH /tickets/{ticketId}/assign`
- `PATCH /tickets/{ticketId}/close`

### Entites metier envisagees

- `SupportTicket`
- `TicketComment`
- `TicketAttachment`
- `TicketStatusHistory`

### Dependances metier

- `member`
- `customer`
- `document`
- `booking`
- `billing`

---

## 7. Module Analytics / Reporting

### Pourquoi ce module

Une fois les modules d'operation actifs, il faut une vision de pilotage.

### Objectif

Permettre:

- suivre l'activite commerciale
- suivre l'usage des ressources
- suivre les revenus
- alimenter des dashboards admin

### Sous-modules proposes

- `dashboard`
- `report`
- `kpi-snapshot`
- `occupancy-metric`
- `revenue-metric`

### Endpoints cibles

- `GET /analytics/dashboard`
- `GET /analytics/occupancy`
- `GET /analytics/revenue`
- `POST /reports/generate`
- `GET /reports/{reportId}`

### Entites metier envisagees

- `KpiSnapshot`
- `GeneratedReport`
- `OccupancyMetric`
- `RevenueMetric`

### Dependances metier

- `booking`
- `billing`
- `payment`
- `resource`
- `customer`

---

## 8. Module Organization / Tenant Settings

### Pourquoi ce module

Le projet a deja:

- `business_entity`
- user settings
- base de roles/utilisateurs

Il manque un vrai module de parametres organisationnels pour administrer une entite d'exploitation.

### Objectif

Permettre:

- parametrer une organisation
- gerer ses regles generales
- definir fuseau horaire, devise, horaires, branding, numerotation

### Sous-modules proposes

- `organization`
- `organization-settings`
- `branding`
- `business-hours`
- `numbering-rules`

### Endpoints cibles

- `GET /organization`
- `PUT /organization`
- `GET /organization/settings`
- `PUT /organization/settings`
- `PUT /organization/branding`
- `PUT /organization/business-hours`

### Entites metier envisagees

- `Organization`
- `OrganizationSettings`
- `BrandingSettings`
- `BusinessHours`
- `WorkingDayRule`

### Dependances metier

- `business_entity`
- `currency`
- `country`
- `sequence engine`

---

## Modules optionnels ensuite

Apres ces modules, les suivants peuvent etre envisages:

- module `crm / leads / opportunities`
- module `visitor management`
- module `inventory / stock`
- module `marketplace / add-on services`
- module `loyalty / referral`
- module `task / internal operations`

---

## Recommandation finale

Si vous voulez lancer les prochains nouveaux modules avec un maximum de coherence metier, l'ordre le plus logique est:

1. `booking / reservation`
2. `billing / invoicing`
3. `payment / wallet`
4. `contract / subscription`

Ces quatre modules prolongent directement les briques deja presentes dans le projet et permettent de construire un vrai coeur metier de plateforme de coworking.
