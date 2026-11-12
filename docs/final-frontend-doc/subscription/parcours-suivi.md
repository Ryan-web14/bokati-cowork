# subscription : parcours de suivi

> Partie de [README.md](README.md). Routes exactes et champs : [routes.md](routes.md). Règles communes : [README racine](../README.md).

## Chronologie

Une ligne par fait marquant d'un abonnement ou d'un titulaire : changements de statut, événements du cycle de vie, usages, reports, notifications.

1. Lire : `GET /sni/api/v1/subscription-timeline` ([route](routes/SubscriptionTimelineController.md#r-get-sni-api-v1-subscription-timeline)) avec `subscriptionNumber` ou `ownerType` + `ownerCode`, et au besoin `eventType`, `occurredFrom`, `occurredTo` (dates-heures) ; tri par défaut `occurredAt,DESC`.
2. Afficher `occurredAt`, `title`, `description`, et le contenu `payloadJson` dépliable.
   - Changement de statut : `eventType: STATUS_CHANGED`, `title` « Subscription status changed », `description` « ANCIEN -> NOUVEAU », `payloadJson` avec `reason` et `actor`.
   - Événements du cycle de vie : `title` est le nom technique de l'événement (`SUBSCRIPTION_RENEWED`, `GRACE_PERIOD_ENTERED`, `TERMINATION_REQUESTED`, `FREEZE_STARTED`, `DIRECT_DEBIT_FAILED`, `DATES_ALIGNED`, `COMMITMENT_SET`…). Ceux qui n'ont pas d'équivalent dans l'énumération de la chronologie arrivent avec `eventType: STATUS_CHANGED` : se fier à `title`, et prévoir une table de libellés français côté frontend.
3. Ajouter une note manuelle : `POST /sni/api/v1/subscription-timeline` avec `eventType` et `title` obligatoires, `subscriptionNumber` ou `ownerType` + `ownerCode`, `description`, `payloadJson` (JSON valide), `occurredAt`.

## Notifications d'abonnement

File de messages propre au module : alertes de quota (80 %, 100 %), alertes pass (expiration proche, solde bas, inutilisé), messages ajoutés à la main.

1. Lister : `GET /sni/api/v1/subscription-notifications` avec `subscriptionNumber`, `ownerType`, `ownerCode`, `notificationType`, `status` ; tri par défaut `scheduledAt,DESC`.
2. Ajouter : `POST /sni/api/v1/subscription-notifications` avec `notificationType`, `channel`, `recipient` obligatoires, `subject`, `body`, `payloadJson`, `scheduledAt`, `subscriptionNumber` (le titulaire est alors repris de l'abonnement).
3. Annuler un message en attente : `PATCH /sni/api/v1/subscription-notifications/{notificationNumber}/cancel` rend le nombre de lignes annulées (`0` si le message n'était plus en attente).
4. Forcer le traitement : `POST /sni/api/v1/subscription-notifications/dispatch-due?limit=100` rend le nombre de messages traités.

Le traitement (worker chaque minute ou route ci-dessus) passe les messages `DISPATCHED` **sans rien envoyer** : ni courriel, ni SMS, ni notification in-app. Ne pas présenter `DISPATCHED` comme « envoyé ». Les courriels réellement envoyés par le module passent par d'autres canaux (voir [Effets automatiques](README.md#effets-automatiques)), et les notifications in-app du membre sont dans le module [notification](../notification/README.md).

## Indicateurs

1. Vue d'ensemble : `GET /sni/api/v1/subscription-metrics/overview` : nombres d'abonnements actifs, en attente, suspendus, annulés, de pass et d'options actifs, de promotions actives, d'éléments facturables en attente et leur montant, revenu récurrent des abonnements actifs, quantités d'usage, dépassements, reports. Les compteurs sont des entiers 64 bits (petits en pratique).
2. Indicateurs sur une période : `GET /sni/api/v1/subscription-metrics/kpis?from=2026-10-01&to=2026-10-31` ; par défaut le mois glissant qui finit aujourd'hui. Fin avant début : `400`.
   - `activeSubscriptions` : abonnements ouvrant des droits (`ACTIVE`, `GRACE_PERIOD`, `PENDING_TERMINATION`, et les statuts jamais atteints `TRIALING`, `PAST_DUE`).
   - `mrr` : leur montant ramené au mois, `arr` = `mrr` × 12, `mrrByCycle` par cycle. Calculé sur `totalAmount` (voir [Écarts](README.md#écarts-relevés)).
   - `activeAtStart`, `newInPeriod`, `churnedInPeriod` (annulés sur la période ; les abonnements `EXPIRED` n'y sont pas comptés), `churnRate` (en %), `monthlyChurnRate`, `newMrr`, `churnedMrr`, `netMrrMovement`.
   - `arpu`, `averageLifetimeMonths` et `customerLifetimeValue` (estimations, nulles si l'attrition est nulle).
   - `seatsOccupied` (sièges actifs), `workspaceCapacity` (capacité des ressources actives), `occupancyRate` (%), `revenuePerSeat`.
   - `inGracePeriod`, `pendingTermination`, `pendingDocuments` : à relier aux files de travail correspondantes.

## Éléments facturables

Chaque somme due produite par le module (souscription, renouvellement, gel, frais de rupture, raccordement, prorata, option, dépassement, pass) devient un élément facturable, aussitôt transmis à la facturation.

`GET /sni/api/v1/billable-items` (droit `PAYMENT:READ`) avec `status` (`PENDING`, `SENT_TO_INVOICE`, `INVOICED`, `CANCELLED`), `subscriberType`, `subscriberCode`, `sourceType`, `sourceId` ; tri par défaut `createdAt,DESC`.

| `sourceType` | Origine | `sourceId` |
| --- | --- | --- |
| `SUBSCRIPTION_RECURRING`, `SUBSCRIPTION_SETUP_FEE`, `DEPOSIT` | première facture d'un abonnement | numéro d'abonnement |
| `SUBSCRIPTION_RENEWAL` | renouvellement | numéro d'abonnement |
| `FREEZE_FEE` | part facturée d'un gel | numéro d'abonnement |
| `TERMINATION_FEE`, `TERMINATION_BRIDGING` | frais de rupture, jours jusqu'à la date d'effet | numéro d'abonnement |
| `ALIGNMENT_BRIDGING` | raccordement d'un alignement | numéro d'abonnement |
| `PLAN_CHANGE_PRORATION` | prorata d'un changement de plan | numéro d'abonnement |
| `SUBSCRIPTION_ADDON` | option | id numérique de l'option |
| `USAGE_OVERAGE` | relevé facturable ou dépassement | `referenceId` du relevé |
| `PASS_SETUP`, `PASS_RENEWAL` | vente et renouvellement d'un pass | numéro de pass |

La facture correspondante se consulte dans [billing](../billing/README.md) ; `invoiceId` est un id interne 64 bits, préférer la recherche des factures du souscripteur dans billing.
