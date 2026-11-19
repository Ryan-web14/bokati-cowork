# Paramètres : domaines et clés

> Généré depuis `settings/platform-settings.tsv` et `SettingDomain.java` par `python tools/frontend-doc/settings_catalog.py`. Ne pas modifier à la main. Même contenu pour un programme : [cles.json](cles.json). Fonctionnement : [README](README.md).

`GET /sni/api/v1/admin/settings/domains` et `GET /sni/api/v1/admin/settings?domain=` rendent ces réglages avec leur valeur courante. Un domaine `adminEditable: false` n'est visible que de `SUPER_ADMIN` (liste vide pour `ADMIN`). Un paramètre précis : `GET /sni/api/v1/admin/settings/{clé}`.

## Domaines

| Domaine | Libellé | Ouvert à `ADMIN` | Réglages |
| --- | --- | --- | --- |
| [`BOOKING`](#booking) | Réservation | oui | 23 |
| [`RESOURCES`](#resources) | Ressources et horaires | oui | 19 |
| [`SUBSCRIPTIONS`](#subscriptions) | Abonnements | oui | 19 |
| [`PASSES`](#passes) | Passes | oui | 6 |
| [`MARKETPLACE`](#marketplace) | Marketplace | oui | 3 |
| [`PROMOTIONS`](#promotions) | Promotions | oui | 3 |
| [`DOMICILIATION`](#domiciliation) | Domiciliation | oui | 10 |
| [`CONTRACTS`](#contracts) | Contrats | oui | 3 |
| [`SUPPORT`](#support) | Support | oui | 8 |
| [`TASKS`](#tasks) | Tâches | oui | 7 |
| [`CRM`](#crm) | CRM | oui | 2 |
| [`EVENTS`](#events) | Événements | oui | 2 |
| [`PORTAL`](#portal) | Portail | oui | 1 |
| [`COMPANIES`](#companies) | Entreprises | non | 3 |
| [`WALLET`](#wallet) | Portefeuille | non | 16 |
| [`PAYMENT`](#payment) | Paiement et caisses | non | 44 |
| [`ACCOUNTING`](#accounting) | Comptabilité | oui | 190 |
| [`BILLING`](#billing) | Facturation | non | 21 |
| [`COMPLIANCE`](#compliance) | Conformité | non | 11 |
| [`DOCUMENTS`](#documents) | Documents | non | 38 |
| [`LOCALE`](#locale) | Langue et téléphone | non | 5 |
| [`SECURITY`](#security) | Sécurité | non | 37 |
| [`AUDIT`](#audit) | Centre d'audit | oui | 24 |
| [`NOTIFICATIONS`](#notifications) | Notifications et e-mails | non | 15 |
| [`WEBHOOKS`](#webhooks) | Webhooks | non | 3 |
| [`OUTBOX`](#outbox) | Boîte d'envoi | non | 3 |
| [`GRAPH_API`](#graph_api) | API Graph | non | 3 |
| [`WEBSOCKET`](#websocket) | WebSocket | non | 10 |
| [`ENVIRONMENT`](#environment) | Adresses et environnement | non | 8 |

Types : `BOOLEAN`, `CRON`, `DECIMAL`, `EMAIL`, `ENUM`, `INTEGER`, `SECRET`, `TEXT`, `TIME`, `TIME_RANGE`. Effets : `LIVE`, `RESTART` (`LIVE` : pris en compte tout de suite ; sinon au redémarrage). Une valeur vide dans « Défaut » : aucune valeur par défaut.

## BOOKING

Réservation · ouvert à ADMIN · 23 réglage(s)

| Clé | Type | Effet | Défaut | Libellé |
| --- | --- | --- | --- | --- |
| `bokati.booking.expiry.worker-delay-ms` | INTEGER | LIVE | `60000` | Réservations : intervalle de la tâche d'expiration (options et réservations non réglées), en millisecondes |
| `bokati.booking.lifecycle.worker-delay-ms` | INTEGER | LIVE | `60000` | Cadence du suivi des réservations (ms) |
| `bokati.booking.no-show.worker-delay-ms` | INTEGER | LIVE | `60000` | Cadence du suivi des absences (ms) |
| `bokati.booking.reminders.worker-delay-ms` | INTEGER | LIVE | `300000` | Réservations : intervalle de la tâche des rappels automatiques, en millisecondes |
| `bokati.booking.staff.max-backdate-days` | INTEGER | LIVE | `31` | Rétroaction d'une réservation de l'équipe (jours) |
| `bokati.booking.virtual-room.base-url` | TEXT | LIVE | `https://meet.jit.si` | Adresse des salles virtuelles |
| `bokati.booking.waitlist.offer-ttl-minutes` | INTEGER | LIVE | `30` | Durée d'une offre de liste d'attente (minutes) |
| `bokati.booking.waitlist.worker-delay-ms` | INTEGER | LIVE | `300000` | Cadence de la liste d'attente (ms) |
| `bokati.booking.wallet.hold-grace-minutes` | INTEGER | LIVE | `60` | Grâce de retenue sur portefeuille (minutes) |
| `bokati.booking.wallet.settlement-delay-minutes` | INTEGER | LIVE | `1` | Délai de règlement sur portefeuille (minutes) |
| `bokati.booking.wallet.worker-delay-ms` | INTEGER | LIVE | `15000` | Cadence des retenues de réservation (ms) |
| `bookings.pay-on-site.max-no-shows` | INTEGER | LIVE | `2` | Réservations : absences sur des rendez-vous réglés sur place avant retrait d'office de l'option (0 : jamais) |
| `bookings.pay-on-site.no-show-window-days` | INTEGER | LIVE | `90` | Réservations : période, en jours, sur laquelle les absences sont comptées |
| `bookings.pay-on-site.portal-enabled` | BOOLEAN | LIVE | `false` | Réservations : rendez-vous réglé sur place ouvert au portail pour tous les clients sans option propre |
| `bookings.payment-due.before-start-minutes` | INTEGER | LIVE | `30` | Réservations : échéance de paiement au plus tard tant de minutes avant le début |
| `bookings.payment-due.business-hour` | INTEGER | LIVE | `10` | Réservations : heure de l'échéance reportée au prochain jour ouvré |
| `bookings.payment-due.default-minutes` | INTEGER | LIVE | `120` | Réservations : validité d'une réservation non réglée, en minutes |
| `bookings.payment-due.far-days` | INTEGER | LIVE | `3` | Réservations : au-delà de ce nombre de jours avant le début, validité longue |
| `bookings.payment-due.far-minutes` | INTEGER | LIVE | `1440` | Réservations : validité longue, en minutes |
| `bookings.payment-due.reminder-minutes` | INTEGER | LIVE | `30` | Réservations : rappel au client tant de minutes avant l'échéance de paiement (0 : aucun rappel) |
| `bookings.price-adjustment.max-rate` | INTEGER | LIVE | `20` | Réservations : remise manuelle maximale (%) sans approbation, pour le titulaire du droit BOOKING:PRICE_ADJUST |
| `bookings.reminders.enabled` | BOOLEAN | LIVE | `true` | Réservations : rappels automatiques par courriel avant le début |
| `bookings.reminders.offsets-minutes` | TEXT | LIVE | `1440,120` | Réservations : délais des rappels avant le début, en minutes, séparés par des virgules (la veille et 2 h avant) |

## RESOURCES

Ressources et horaires · ouvert à ADMIN · 19 réglage(s)

| Clé | Type | Effet | Défaut | Libellé |
| --- | --- | --- | --- | --- |
| `app.resources.booking-horizon-days` | INTEGER | LIVE | `90` | Ressources : horizon de réservation, en jours (créneaux ouverts jusque-là) |
| `app.resources.holidays.hours` | TEXT | LIVE | `NORMAL` | Ressources : horaires des jours fériés (NORMAL = semaine type, CLOSED = fermé, ou plages HH:mm-HH:mm séparées par des virgules) |
| `app.resources.holidays.national` | BOOLEAN | LIVE | `true` | Ressources : jours fériés nationaux du Congo calculés (créneaux, prix, calendrier) |
| `app.resources.ical.public-feed-enabled` | BOOLEAN | LIVE | `false` | Ressources : flux iCalendar public par lien à jeton (« Occupé » et « Fermé » seulement) |
| `app.resources.peak-hours` | TEXT | LIVE | `09:00-12:00,14:00-17:00` | Ressources : heures pleines du lundi au vendredi, pour le pilotage (plages HH:mm-HH:mm séparées par des virgules) |
| `app.resources.pricing.day-hours` | INTEGER | LIVE | `10` | Ressources : durée d'une journée facturée, en heures (décision D2 : 10) |
| `app.resources.pricing.half-day-hours` | INTEGER | LIVE | `5` | Ressources : durée d'une demi-journée facturée, en heures (décision D2 : 5) |
| `bokati.resource.availability.generation.saturday-hours` | TIME_RANGE | LIVE | `08:00-12:00` | Horaires du samedi |
| `bokati.resource.availability.generation.sunday-hours` | TIME_RANGE | LIVE | `` | Horaires du dimanche |
| `bokati.resource.availability.generation.weekday-hours` | TIME_RANGE | LIVE | `08:00-18:00` | Horaires de semaine |
| `bokati.resource.availability.generation.weekday-subscriber-hours` | TIME_RANGE | LIVE | `18:00-22:00` | Horaires de semaine réservés aux abonnés |
| `bokati.resource.availability.subscriber-hours-from` | TIME | LIVE | `18:00` | Début de la plage réservée aux abonnés |
| `bokati.resource.bulk-max-resources` | INTEGER | LIVE | `20` | Ressources par action groupée |
| `bokati.resource.workers.monthly-availability-cron` | CRON | LIVE | `0 0 3 * * *` | Planification de l'ouverture des créneaux jusqu'à l'horizon (chaque jour) |
| `bokati.resource.workers.monthly-availability-enabled` | BOOLEAN | LIVE | `true` | Ouverture automatique du mois active |
| `bokati.resource.workers.monthly-availability-initial-delay-ms` | INTEGER | RESTART | `120000` | Attente avant l'ouverture du mois (ms) |
| `bokati.resource.workers.slot-capacity-delay-ms` | INTEGER | LIVE | `3600000` | Cadence de recalcul des capacités (ms) |
| `bokati.resource.workers.slot-capacity-enabled` | BOOLEAN | LIVE | `true` | Recalcul des capacités actif |
| `bokati.resource.workers.slot-capacity-initial-delay-ms` | INTEGER | RESTART | `60000` | Attente avant le recalcul des capacités (ms) |

## SUBSCRIPTIONS

Abonnements · ouvert à ADMIN · 19 réglage(s)

| Clé | Type | Effet | Défaut | Libellé |
| --- | --- | --- | --- | --- |
| `bokati.subscription.kpi.workspace-type-codes` | TEXT | LIVE | `` | Types d'espace des indicateurs |
| `bokati.subscription.workers.addon-expiry-delay-ms` | INTEGER | LIVE | `900000` | Cadence d'expiration des options (ms) |
| `bokati.subscription.workers.cancellation-delay-ms` | INTEGER | LIVE | `900000` | Cadence des clôtures (ms) |
| `bokati.subscription.workers.change-delay-ms` | INTEGER | LIVE | `900000` | Cadence des changements planifiés (ms) |
| `bokati.subscription.workers.contract-catch-up-initial-delay-ms` | INTEGER | RESTART | `120000` | Attente avant le rattrapage des contrats (ms) |
| `bokati.subscription.workers.contract-repair-batch-size` | INTEGER | LIVE | `20` | Taille de lot de la réparation des contrats |
| `bokati.subscription.workers.contract-repair-delay-ms` | INTEGER | LIVE | `60000` | Cadence de la réparation des contrats (ms) |
| `bokati.subscription.workers.contract-repair-enabled` | BOOLEAN | LIVE | `true` | Réparation des contrats active |
| `bokati.subscription.workers.contract-repair-min-age-seconds` | INTEGER | LIVE | `300` | Ancienneté minimale avant réparation (secondes) |
| `bokati.subscription.workers.entitlement-expiry-delay-ms` | INTEGER | LIVE | `600000` | Cadence d'expiration des droits (ms) |
| `bokati.subscription.workers.integrity-delay-ms` | INTEGER | LIVE | `1800000` | Cadence du contrôle d'intégrité (ms) |
| `bokati.subscription.workers.lifecycle-delay-ms` | INTEGER | LIVE | `3600000` | Cadence du cycle de vie (ms) |
| `bokati.subscription.workers.lifecycle-initial-delay-ms` | INTEGER | RESTART | `120000` | Attente avant le cycle de vie (ms) |
| `bokati.subscription.workers.notification-dispatch-delay-ms` | INTEGER | LIVE | `60000` | Cadence des avis d'abonnement (ms) |
| `bokati.subscription.workers.pass-expiry-delay-ms` | INTEGER | LIVE | `600000` | Cadence d'expiration des pass (ms) |
| `bokati.subscription.workers.promotion-expiry-delay-ms` | INTEGER | LIVE | `900000` | Cadence d'expiration des promotions (ms) |
| `bokati.subscription.workers.renewal-delay-ms` | INTEGER | LIVE | `900000` | Cadence des renouvellements (ms) |
| `bokati.subscription.workers.reservation-expiry-delay-ms` | INTEGER | LIVE | `300000` | Cadence d'expiration des retenues de droits (ms) |
| `bokati.subscription.workers.rollover-delay-ms` | INTEGER | LIVE | `900000` | Cadence des reports de droits (ms) |

## PASSES

Passes · ouvert à ADMIN · 6 réglage(s)

| Clé | Type | Effet | Défaut | Libellé |
| --- | --- | --- | --- | --- |
| `bokati.pass.alert.cron` | CRON | LIVE | `0 0 8 * * *` | Planification des alertes de pass |
| `bokati.pass.alert.expiring-days` | INTEGER | LIVE | `7` | Alerte avant expiration (jours) |
| `bokati.pass.alert.low-balance-ratio` | DECIMAL | LIVE | `0.2` | Alerte de solde bas (part restante) |
| `bokati.pass.alert.unused-days` | INTEGER | LIVE | `14` | Alerte de non-usage (jours) |
| `bokati.pass.credential.rotation-minutes` | INTEGER | LIVE | `15` | Rotation du code d'accès (minutes) |
| `bokati.pass.workers.renewal-delay-ms` | INTEGER | LIVE | `900000` | Cadence des renouvellements de pass (ms) |

## MARKETPLACE

Marketplace · ouvert à ADMIN · 3 réglage(s)

| Clé | Type | Effet | Défaut | Libellé |
| --- | --- | --- | --- | --- |
| `bokati.marketplace.payment-window-minutes` | INTEGER | LIVE | `30` | Délai de paiement d'une commande (minutes) |
| `bokati.marketplace.sync-delay-ms` | INTEGER | LIVE | `60000` | Cadence de suivi des commandes (ms) |
| `bokati.marketplace.sync-initial-delay-ms` | INTEGER | RESTART | `60000` | Attente avant le premier suivi (ms) |

## PROMOTIONS

Promotions · ouvert à ADMIN · 3 réglage(s)

| Clé | Type | Effet | Défaut | Libellé |
| --- | --- | --- | --- | --- |
| `bokati.promotion.coupon.expiry-worker-delay-ms` | INTEGER | LIVE | `120000` | Cadence d'expiration des coupons (ms) |
| `bokati.promotion.coupon.reservation-minutes` | INTEGER | LIVE | `30` | Réservation d'un coupon (minutes) |
| `bokati.promotion.referral.tenure-cron` | CRON | LIVE | `0 30 2 * * *` | Planification du suivi du parrainage |

## DOMICILIATION

Domiciliation · ouvert à ADMIN · 10 réglage(s)

| Clé | Type | Effet | Défaut | Libellé |
| --- | --- | --- | --- | --- |
| `bokati.domiciliation.administration-email` | EMAIL | LIVE | `` | Adresse de l'administration à notifier |
| `bokati.domiciliation.certificate-validity-days` | INTEGER | LIVE | `365` | Validité de l'attestation (jours) |
| `bokati.domiciliation.expiry-cron` | CRON | LIVE | `0 40 7 * * *` | Planification des expirations |
| `bokati.domiciliation.kyc-level-required` | INTEGER | LIVE | `2` | Niveau KYC exigé |
| `bokati.domiciliation.mail-cron` | CRON | LIVE | `0 30 7 * * *` | Planification du suivi du courrier |
| `bokati.domiciliation.mail.storage-days` | INTEGER | LIVE | `30` | Garde du courrier (jours) |
| `bokati.domiciliation.notify-administration-on-termination` | BOOLEAN | LIVE | `false` | Notifier l'administration à la résiliation |
| `bokati.domiciliation.reminder-cron` | CRON | LIVE | `0 20 7 * * *` | Planification des rappels |
| `bokati.domiciliation.renewal-reminder-days` | INTEGER | LIVE | `30` | Rappel avant renouvellement (jours) |
| `bokati.domiciliation.retention-years` | INTEGER | LIVE | `10` | Conservation (années) |

## CONTRACTS

Contrats · ouvert à ADMIN · 3 réglage(s)

| Clé | Type | Effet | Défaut | Libellé |
| --- | --- | --- | --- | --- |
| `app.contract.signing-token-hours` | INTEGER | LIVE | `72` | Validité du lien de signature (heures) |
| `app.contract.worker.enabled` | BOOLEAN | LIVE | `true` | Traitement des contrats actif |
| `app.contract.worker.fixed-delay-ms` | INTEGER | LIVE | `3600000` | Cadence du traitement des contrats (ms) |

## SUPPORT

Support · ouvert à ADMIN · 8 réglage(s)

| Clé | Type | Effet | Défaut | Libellé |
| --- | --- | --- | --- | --- |
| `bokati.support.admin-email` | EMAIL | LIVE | `supportela@elleaose.com` | Adresse de l'administration |
| `bokati.support.auto-close-check-delay-ms` | INTEGER | LIVE | `3600000` | Cadence de fermeture automatique (ms) |
| `bokati.support.auto-close-delay-days` | INTEGER | LIVE | `5` | Fermeture automatique après (jours) |
| `bokati.support.csat-check-delay-ms` | INTEGER | LIVE | `3600000` | Cadence des enquêtes de satisfaction (ms) |
| `bokati.support.inbound-poll-delay-ms` | INTEGER | LIVE | `60000` | Cadence de lecture de la boîte support (ms) |
| `bokati.support.manager-email` | EMAIL | LIVE | `supportela@elleaose.com` | Adresse du responsable |
| `bokati.support.sender-email` | EMAIL | LIVE | `supportela@elleaose.com` | Adresse d'envoi |
| `bokati.support.sla-check-delay-ms` | INTEGER | LIVE | `3600000` | Cadence du contrôle des délais (ms) |

## TASKS

Tâches · ouvert à ADMIN · 7 réglage(s)

| Clé | Type | Effet | Défaut | Libellé |
| --- | --- | --- | --- | --- |
| `bokati.task.booking-cleanup-default-assignee` | TEXT | LIVE | `` | Assigné par défaut · remise en état |
| `bokati.task.booking-default-assignee` | TEXT | LIVE | `` | Assigné par défaut · préparation |
| `bokati.task.booking-prep-hours` | INTEGER | LIVE | `2` | Préparation d'une réservation (heures) |
| `bokati.task.due-soon-hours` | INTEGER | LIVE | `24` | Échéance proche (heures) |
| `bokati.task.inventory-default-assignee` | TEXT | LIVE | `` | Assigné par défaut · stock |
| `bokati.task.inventory-reorder-due-hours` | INTEGER | LIVE | `24` | Délai de réapprovisionnement (heures) |
| `bokati.task.reminder-cron` | CRON | LIVE | `0 0 * * * *` | Planification des rappels de tâches |

## CRM

CRM · ouvert à ADMIN · 2 réglage(s)

| Clé | Type | Effet | Défaut | Libellé |
| --- | --- | --- | --- | --- |
| `bokati.crm.dormant-cron` | CRON | LIVE | `0 0 8 * * MON-FRI` | Planification du suivi des clients dormants |
| `bokati.crm.dormant-threshold-days` | INTEGER | LIVE | `7` | Client dormant après (jours) |

## EVENTS

Événements · ouvert à ADMIN · 2 réglage(s)

| Clé | Type | Effet | Défaut | Libellé |
| --- | --- | --- | --- | --- |
| `app.event.notification-email` | EMAIL | LIVE | `coworkspace@elleaose.com` | Adresse de notification des événements |
| `app.event.opening-code` | TEXT | LIVE | `OUVERTURE` | Code de l'événement d'ouverture |

## PORTAL

Portail · ouvert à ADMIN · 1 réglage(s)

| Clé | Type | Effet | Défaut | Libellé |
| --- | --- | --- | --- | --- |
| `app.portal.kyc-grace-period-days` | INTEGER | LIVE | `7` | Grâce KYC du portail (jours) |

## COMPANIES

Entreprises · SUPER_ADMIN seul · 3 réglage(s)

| Clé | Type | Effet | Défaut | Libellé |
| --- | --- | --- | --- | --- |
| `bokati.company.wallet.approval-expiry-delay-ms` | INTEGER | LIVE | `600000` | Cadence d'expiration des validations (ms) |
| `bokati.company.wallet.approval-hours` | INTEGER | LIVE | `48` | Délai de validation d'une dépense (heures) |
| `bokati.company.wallet.currency` | TEXT | LIVE | `XAF` | Devise du portefeuille d'organisation |

## WALLET

Portefeuille · SUPER_ADMIN seul · 16 réglage(s)

| Clé | Type | Effet | Défaut | Libellé |
| --- | --- | --- | --- | --- |
| `bokati.wallet.admin.four-eyes-threshold` | INTEGER | LIVE | `100000` | Seuil de double validation |
| `bokati.wallet.confirmation.expiry-delay-ms` | INTEGER | LIVE | `300000` | Cadence d'expiration des confirmations (ms) |
| `bokati.wallet.confirmation.validity-minutes` | INTEGER | LIVE | `5` | Validité d'une confirmation (minutes) |
| `bokati.wallet.dormancy.after-days` | INTEGER | LIVE | `180` | Dormance après (jours) |
| `bokati.wallet.dormancy.cron` | CRON | LIVE | `0 15 3 * * *` | Planification de la dormance |
| `bokati.wallet.payment-request.validity-days` | INTEGER | LIVE | `7` | Validité d'une demande de paiement (jours) |
| `bokati.wallet.risk.large-transfer-amount` | INTEGER | LIVE | `100000` | Montant d'un gros transfert |
| `bokati.wallet.transfer.confirmation-validity-minutes` | INTEGER | LIVE | `5` | Validité de la confirmation d'un transfert (minutes) |
| `bokati.wallet.transfer.fee-max` | DECIMAL | LIVE | `0` | Frais de transfert maximum |
| `bokati.wallet.transfer.fee-min` | DECIMAL | LIVE | `0` | Frais de transfert minimum |
| `bokati.wallet.transfer.fee-rate` | DECIMAL | LIVE | `0` | Taux des frais de transfert |
| `bokati.wallet.transfer.fee-wallet-number` | TEXT | LIVE | `` | Portefeuille des frais |
| `bokati.wallet.treasury.alert-email` | EMAIL | LIVE | `` | Adresse des alertes de trésorerie |
| `bokati.wallet.treasury.coverage-alert-ratio` | DECIMAL | LIVE | `1.0` | Seuil d'alerte de couverture |
| `bokati.wallet.treasury.variance-tolerance` | DECIMAL | LIVE | `1` | Tolérance d'écart de trésorerie |
| `bokati.wallet.usage.expiry-delay-ms` | INTEGER | LIVE | `600000` | Cadence d'expiration des usages (ms) |

## PAYMENT

Paiement et caisses · SUPER_ADMIN seul · 44 réglage(s)

| Clé | Type | Effet | Défaut | Libellé |
| --- | --- | --- | --- | --- |
| `approval.escalation-hours` | INTEGER | LIVE | `72` | Heures d'attente d'un niveau avant escalade au supérieur du décideur |
| `approval.reminder-hours` | INTEGER | LIVE | `24` | Heures d'attente d'un niveau avant rappel aux décideurs (puis à chaque intervalle) |
| `approval.worker-cron` | CRON | LIVE | `0 0 8-18 * * *` | Planification des rappels et escalades des centres d'approbation |
| `bokati.cash.assignment.enforced` | BOOLEAN | LIVE | `true` | Ouverture de session reservee aux personnes affectees a la caisse |
| `bokati.cash.forced-close.contest-days` | INTEGER | LIVE | `3` | Delai de contestation d'une cloture d'office (jours) |
| `bokati.cash.refund.max-cash-amount` | INTEGER | LIVE | `0` | Remboursement en especes au-dela duquel le virement est impose (0 : sans limite) |
| `bokati.cash.reversal.cashier-max-amount` | INTEGER | LIVE | `50000` | Plafond d'une contre-passation par le caissier seul |
| `bokati.cash.reversal.cashier-minutes` | INTEGER | LIVE | `15` | Delai pendant lequel le caissier contre-passe seul (minutes) |
| `bokati.cash.staff-worker-cron` | CRON | LIVE | `0 10 * * * *` | Planification du suivi des absences et des sessions trop longues |
| `bokati.cash.transfer.max-transit-hours` | INTEGER | LIVE | `2` | Transit maximal d'un transfert de caisse avant alerte (heures) |
| `bokati.cash.variance.investigation-days` | INTEGER | LIVE | `30` | Echeance de revue d'un ecart mis en recherche (jours) |
| `bokati.cash.visa.expiry-hours` | INTEGER | LIVE | `24` | Delai du second visa d'une operation de caisse (heures) |
| `bokati.payment.cash-declaration.desk-email` | EMAIL | LIVE | `` | Adresse de l'accueil pour les espèces |
| `bokati.payment.cash-declaration.expiry-delay-ms` | INTEGER | LIVE | `900000` | Cadence d'expiration des déclarations (ms) |
| `bokati.payment.cash-declaration.validity-hours` | INTEGER | LIVE | `4` | Validité d'une déclaration d'espèces (heures) |
| `bokati.payment.cash-register.alert-cooldown-hours` | INTEGER | LIVE | `6` | Intervalle entre deux alertes de caisse (heures) |
| `bokati.payment.cash-session.auto-limit` | INTEGER | LIVE | `10000000` | Plafond d'une session automatique |
| `bokati.payment.cash-session.closing-report.enabled` | BOOLEAN | LIVE | `true` | Rapport de clôture de caisse actif |
| `bokati.payment.cash-session.month-end-cron` | CRON | LIVE | `0 5 0 * * *` | Planification de la clôture de fin de mois |
| `bokati.payment.cash-session.supervisor-email` | EMAIL | LIVE | `supportela@elleaose.com` | Adresse du superviseur de caisse |
| `bokati.payment.duplicate-window-seconds` | INTEGER | LIVE | `30` | Paiements : délai (secondes) pendant lequel un paiement identique (même mode, montant, auteur) est rendu au lieu d'être enregistré deux fois ; 0 coupe le contrôle |
| `bokati.payment.pawaypay.enabled-providers` | TEXT | LIVE | `` | Opérateurs mobile money actifs |
| `bokati.payment.pawaypay.operator-prefixes` | TEXT | LIVE | `` | Préfixes des opérateurs |
| `bokati.payment.pawaypay.polling-delay-ms` | INTEGER | LIVE | `60000` | Cadence de lecture de l'opérateur (ms) |
| `bokati.payment.pawaypay.reconciliation-cron` | CRON | LIVE | `0 0 6 * * *` | Planification du rapprochement mobile money |
| `bokati.payment.self-service.confirmation-email` | EMAIL | LIVE | `` | Adresse de confirmation du libre-service |
| `bokati.payment.workers.expiry-alert-delay-ms` | INTEGER | LIVE | `1800000` | Cadence des alertes d'expiration (ms) |
| `bokati.payment.workers.intent-expiry-delay-ms` | INTEGER | LIVE | `300000` | Cadence d'expiration des intentions de paiement (ms) |
| `bokati.payment.workers.wallet-hold-expiry-delay-ms` | INTEGER | LIVE | `300000` | Cadence d'expiration des retenues (ms) |
| `bokati.payment.workers.wallet-reconciliation-cron` | CRON | LIVE | `0 15 3 * * *` | Planification du rapprochement des portefeuilles |
| `bokati.payment.workers.wallet-reconciliation-enabled` | BOOLEAN | LIVE | `true` | Rapprochement des portefeuilles actif |
| `fund.advance.overdue-policy` | ENUM | LIVE | `BLOCK` | Avance en retard de justification : BLOCK (pas de nouvelle avance) ou ALERT |
| `fund.approval.level1-max` | INTEGER | LIVE | `50000` | Montant maximal valide par le seul chef de service (XAF) |
| `fund.approval.level2-max` | INTEGER | LIVE | `500000` | Montant maximal valide sans la direction (XAF) |
| `fund.budget.blocking-mode` | ENUM | LIVE | `EXTRA_APPROVAL` | Depassement d'une ligne bloquante : REFUSE (demande refusee) ou EXTRA_APPROVAL (validation budgetaire en plus) |
| `fund.budget.require-forecast-choice` | BOOLEAN | LIVE | `true` | Avec un budget actif, chaque demande cite sa prevision ou se declare non prevue |
| `fund.budget.unplanned-approval` | BOOLEAN | LIVE | `false` | Une depense non prevue demande une validation budgetaire en plus |
| `fund.justification.days` | INTEGER | LIVE | `7` | Delai de justification d'une avance apres la mission ou le decaissement (jours) |
| `fund.justification.receipt-exempt-max` | INTEGER | LIVE | `5000` | Montant d'une ligne de justification admis sans piece, sur attestation (XAF) |
| `fund.request.expiry-days` | INTEGER | LIVE | `15` | Jours apres lesquels une demande approuvee non decaissee expire |
| `fund.vat-recoverable-account` | TEXT | LIVE | `4452` | Compte de TVA recuperable des justifications d'avance |
| `fund.voucher.issuer-details` | TEXT | LIVE | `` | Mentions de l'exploitant en tete des pieces (RCCM, NIU, adresse) |
| `fund.voucher.issuer-name` | TEXT | LIVE | `ELLE A OSE` | Raison sociale en tete des pieces comptables |
| `fund.worker-cron` | CRON | LIVE | `0 0 8-18 * * *` | Planification des rappels et relances des demandes de fonds |

## ACCOUNTING

Comptabilité · ouvert à ADMIN · 190 réglage(s)

| Clé | Type | Effet | Défaut | Libellé |
| --- | --- | --- | --- | --- |
| `accounting.account.creation-mode` | ENUM | LIVE | `IMMEDIATE` | Creation de compte : IMMEDIATE ou AFTER_APPROVAL |
| `accounting.accrual.alert-days` | INTEGER | LIVE | `60` | Jours avant alerte d'une estimation non rapprochee d'une facture reelle |
| `accounting.accrual.expense-account` | TEXT | LIVE | `408` | Compte des factures non parvenues |
| `accounting.accrual.expense-vat-account` | TEXT | LIVE | `4454` | TVA sur les factures non parvenues quand elle est connue |
| `accounting.accrual.revenue-account` | TEXT | LIVE | `4181` | Compte des factures a etablir |
| `accounting.accrual.reversal-journal` | TEXT | LIVE | `OD` | Journal des extournes des estimations |
| `accounting.analytic.centre-account` | TEXT | LIVE | `93` | Compte de classe 9 des centres d'analyse (reflet analytique) |
| `accounting.analytic.class9-axis` | TEXT | LIVE | `ACTIVITE` | Axe reflechi en classe 9 dans le journal CAG (vide : pas de reflet) |
| `accounting.analytic.creation-mode` | ENUM | LIVE | `IMMEDIATE` | Creation d'une section par un administrateur : IMMEDIATE ou AFTER_APPROVAL |
| `accounting.analytic.enabled` | BOOLEAN | LIVE | `false` | Comptabilite analytique active (ventilation obligatoire sur les comptes concernes) |
| `accounting.analytic.reflected-account` | TEXT | LIVE | `91` | Compte des charges et produits reflechis |
| `accounting.bank.auto-match` | BOOLEAN | LIVE | `true` | Pointage d'office des correspondances sures |
| `accounting.bank.date-window-days` | INTEGER | LIVE | `5` | Fenetre de dates du pointage automatique (jours) |
| `accounting.bank.max-combined-lines` | INTEGER | LIVE | `10` | Nombre maximal d'ecritures combinees pour une ligne de releve |
| `accounting.bank.missing-statement` | ENUM | LIVE | `ALERT` | Releve manquant a l'import : ALERT ou REFUSE |
| `accounting.bank.reconciliation-before-lock` | BOOLEAN | LIVE | `true` | Rapprochement bancaire valide exige avant de verrouiller une periode |
| `accounting.cash.default-in-account` | TEXT | LIVE | `471` | Compte d'une entree de caisse sans nature d'operation |
| `accounting.cash.default-out-account` | TEXT | LIVE | `471` | Compte d'une sortie de caisse sans nature d'operation |
| `accounting.cash.exchange-gain-account` | TEXT | LIVE | `776` | Compte des gains de change sur especes en devise |
| `accounting.cash.exchange-loss-account` | TEXT | LIVE | `676` | Compte des pertes de change sur especes en devise |
| `accounting.cash.treasury-parent-account` | TEXT | LIVE | `571` | Compte parent sous lequel chaque caisse recoit son compte |
| `accounting.cash.vat-collected-account` | TEXT | LIVE | `4431` | Compte de TVA collectee sur une entree de caisse |
| `accounting.cash.vat-recoverable-account` | TEXT | LIVE | `4452` | Compte de TVA recuperable sur une sortie de caisse |
| `accounting.closing.collective-accounts` | TEXT | LIVE | `411,401,421,4191,422,462,165` | Comptes collectifs a justifier par leurs tiers avant la cloture |
| `accounting.closing.inventory-sources` | TEXT | LIVE | `FIXED_ASSET_DEPRECIATION,FIXED_ASSET_DEROGATORY,FIXED_ASSET_IMPAIRMENT,IMPAIRMENT_REVERSAL,RECEIVABLE_IMPAIRMENT_ADJUSTED,RECEIVABLE_IMPAIRMENT_RELEASED,RECEIVABLE_IMPAIRMENT_RECLASS,RISK_PROVISION_POSTED,RISK_PROVISION_RELEASED,RISK_PROVISION_RECLASS,STOCK_IMPAIRMENT,TREASURY_IMPAIRMENT,STAFF_ADVANCE_IMPAIRMENT,DEPOSIT_IMPAIRMENT,DISPOSAL_RECEIVABLE_IMPAIRMENT,REVENUE_DEFERRAL_ADJUSTED,EXPENSE_DEFERRAL_ADJUSTED,ACCRUED_REVENUE_POSTED,ACCRUED_EXPENSE_POSTED,TAX_RESULT` | Types de piece des ecritures d'inventaire (balance avant et apres inventaire) |
| `accounting.closing.loss-account` | TEXT | LIVE | `139` | Compte du resultat en perte repris en a-nouveau |
| `accounting.closing.off-balance-journal` | TEXT | LIVE | `HB` | Journal de reprise des engagements hors bilan a la cloture |
| `accounting.closing.opening-journal` | TEXT | LIVE | `AN` | Journal des a-nouveaux de l'exercice suivant |
| `accounting.closing.profit-account` | TEXT | LIVE | `131` | Compte du resultat en benefice repris en a-nouveau |
| `accounting.closing.suspense-accounts` | TEXT | LIVE | `471,585` | Comptes d'attente a solder avant la cloture (prefixes separes par des virgules) |
| `accounting.commitment.alert-days` | INTEGER | LIVE | `30` | Hors bilan : alerte avant echeance, preavis et renouvellement (jours ; aussi a 7 et 1 jour) |
| `accounting.commitment.given-account` | TEXT | LIVE | `901` | Compte des engagements donnes |
| `accounting.commitment.given-counter-account` | TEXT | LIVE | `902` | Compte de contrepartie des engagements donnes |
| `accounting.commitment.journal` | TEXT | LIVE | `HB` | Journal des engagements hors bilan |
| `accounting.commitment.received-account` | TEXT | LIVE | `905` | Compte des engagements recus |
| `accounting.commitment.received-counter-account` | TEXT | LIVE | `906` | Compte de contrepartie des engagements recus |
| `accounting.confirmation.threshold` | DECIMAL | LIVE | `1000000` | Confirmations de solde : tout solde au-dela de ce montant est circularise |
| `accounting.confirmation.top-count` | INTEGER | LIVE | `20` | Confirmations de solde : nombre des plus gros soldes circularises |
| `accounting.currencies` | TEXT | LIVE | `EUR,USD,GBP,CHF,CNY,JPY,CAD,ZAR` | Devises suivies (taux recuperes et proposes), separees par des virgules |
| `accounting.deferral.analytic-axis` | TEXT | LIVE | `` | Constatations d'avance : axe analytique des sections reprises sur les ecritures (vide : celui des achats) |
| `accounting.deferral.cron` | CRON | LIVE | `0 0 4 * * *` | Constatations d'avance : planification (echeanciers chaque nuit, fin de periode le 1er du mois) |
| `accounting.deferral.journal` | TEXT | LIVE | `OD` | Constatations d'avance : journal des ajustements et des estimations |
| `accounting.doubtful.customer-policy` | ENUM | LIVE | `ALERT` | Client a creance douteuse : ALERT, PREPAY ou BLOCK (suffixe .member, .customer, .business-entity par segment) |
| `accounting.enabled` | BOOLEAN | LIVE | `false` | Comptabilite active (ecritures automatiques de la plateforme) |
| `accounting.exchange.auto-fetch` | BOOLEAN | LIVE | `true` | Recuperation automatique des taux du jour sur internet |
| `accounting.exchange.eur-parity` | DECIMAL | LIVE | `655.957` | Parite fixe de l'euro en francs CFA |
| `accounting.exchange.fetch-cron` | CRON | LIVE | `0 15 7 * * *` | Planification de la recuperation des taux de change |
| `accounting.exchange.gain-account` | TEXT | LIVE | `776` | Compte des gains de change au reglement d'une facture en devise |
| `accounting.exchange.loss-account` | TEXT | LIVE | `676` | Compte des pertes de change au reglement d'une facture en devise |
| `accounting.exchange.provider-url` | TEXT | LIVE | `https://api.frankfurter.dev/v1/latest?from=EUR` | Adresse du fournisseur de taux (reponse JSON en base EUR) |
| `accounting.expense.deferral` | ENUM | LIVE | `MONTHLY` | Charges constatees d'avance : MONTHLY, YEAR_END ou NONE |
| `accounting.expense.deferral-threshold` | INTEGER | LIVE | `50000` | Seuil de signification des charges a etaler (XAF HT par ligne) |
| `accounting.expense.deferred-account` | TEXT | LIVE | `476` | Compte des charges constatees d'avance |
| `accounting.export.odoo.account-length` | INTEGER | LIVE | `0` | Export Odoo : longueur des comptes, completes par des zeros (0 : inchanges) |
| `accounting.export.sage100.account-length` | INTEGER | LIVE | `8` | Export Sage 100 : longueur des comptes generaux, completes par des zeros |
| `accounting.export.sagex3.account-length` | INTEGER | LIVE | `0` | Export Sage X3 : longueur des comptes, completes par des zeros (0 : inchanges) |
| `accounting.export.sagex3.entry-type` | TEXT | LIVE | `OD` | Export Sage X3 : type de piece des en-tetes G |
| `accounting.export.sagex3.site` | TEXT | LIVE | `` | Export Sage X3 : site des en-tetes G |
| `accounting.financing.analytic-axis` | TEXT | LIVE | `` | Axe analytique des sections portees par les emprunts et la paie (vide : pas de ventilation) |
| `accounting.financing.cron` | CRON | LIVE | `0 45 6 * * *` | Tenue quotidienne : echeances d'emprunt, alertes, hors bilan |
| `accounting.fixed-asset.commission-alert-days` | INTEGER | LIVE | `30` | Immobilisations : jours avant alerte d'un bien recu non mis en service |
| `accounting.fixed-asset.controls-cron` | CRON | LIVE | `0 30 6 * * *` | Immobilisations : planification des controles quotidiens du fichier |
| `accounting.fixed-asset.day-basis` | TEXT | LIVE | `360` | Immobilisations : base du prorata temporis (360 ou 365 jours) |
| `accounting.fixed-asset.declining-coefficients` | TEXT | LIVE | `3-4:1.5,5-6:2,7+:2.5` | Immobilisations : coefficients du degressif par duree en annees |
| `accounting.fixed-asset.depreciation-cron` | CRON | LIVE | `0 0 3 1 * *` | Immobilisations : planification des dotations automatiques |
| `accounting.fixed-asset.derogatory-account` | TEXT | LIVE | `151` | Immobilisations : compte des amortissements derogatoires |
| `accounting.fixed-asset.derogatory-expense-account` | TEXT | LIVE | `851` | Immobilisations : compte de dotation aux amortissements derogatoires |
| `accounting.fixed-asset.derogatory-reversal-account` | TEXT | LIVE | `861` | Immobilisations : compte de reprise des amortissements derogatoires |
| `accounting.fixed-asset.disposal-receivable-account` | TEXT | LIVE | `485` | Immobilisations : compte de creance sur cession |
| `accounting.fixed-asset.draft-alert-days` | INTEGER | LIVE | `15` | Immobilisations : jours avant alerte d'une fiche a completer |
| `accounting.fixed-asset.impairment-second-validation` | INTEGER | LIVE | `1000000` | Immobilisations : montant de depreciation au-dela duquel une seconde validation est exigee (XAF) |
| `accounting.fixed-asset.in-progress-account` | TEXT | LIVE | `239` | Immobilisations : compte des immobilisations en cours |
| `accounting.fixed-asset.insurance-claim-account` | TEXT | LIVE | `471` | Immobilisations : compte de l'indemnite d'assurance a recevoir |
| `accounting.fixed-asset.insurance-income-account` | TEXT | LIVE | `841` | Immobilisations : compte de produit des indemnites d'assurance |
| `accounting.fixed-asset.inventory-required` | BOOLEAN | LIVE | `true` | Immobilisations : une campagne d'inventaire physique close est exigee avant la cloture |
| `accounting.fixed-asset.journal` | TEXT | LIVE | `OD` | Immobilisations : journal des dotations et operations |
| `accounting.fixed-asset.manual-entry-guard` | BOOLEAN | LIVE | `true` | Immobilisations : interdire la saisie directe sur les comptes 2, 28 et 29 sans motif ni droit |
| `accounting.fixed-asset.resource-axis` | TEXT | LIVE | `RESSOURCE` | Immobilisations : axe analytique des ressources |
| `accounting.fixed-asset.rhythm` | ENUM | LIVE | `MONTHLY` | Immobilisations : rythme des dotations (MONTHLY, QUARTERLY, YEARLY) |
| `accounting.fixed-asset.sale-journal` | TEXT | LIVE | `VT` | Immobilisations : journal des cessions |
| `accounting.fixed-asset.subsidy-income-account` | TEXT | LIVE | `799` | Immobilisations : compte de reprise des subventions d'equipement |
| `accounting.fixed-asset.supplier-account` | TEXT | LIVE | `481` | Immobilisations : compte des fournisseurs d'investissement |
| `accounting.fixed-asset.vat-regularization-account` | TEXT | LIVE | `658` | Immobilisations : compte de charge des regularisations de TVA |
| `accounting.fixed-asset.vat-regularization-years` | INTEGER | LIVE | `5` | Immobilisations : periode de regularisation de la TVA deduite (annees) |
| `accounting.fixed-asset.warranty-alert-days` | INTEGER | LIVE | `30` | Immobilisations : jours d'alerte avant la fin d'une garantie ou d'un contrat |
| `accounting.forecast.history-months` | INTEGER | LIVE | `3` | Previsionnel de tresorerie : mois d'historique pour la moyenne du personnel et des charges courantes |
| `accounting.forecast.payment-term-days` | INTEGER | LIVE | `30` | Previsionnel de tresorerie : delai de reglement d'une piece sans echeance (jours) |
| `accounting.lettering.auto-delay-ms` | INTEGER | LIVE | `600000` | Intervalle du lettrage automatique des comptes clients (millisecondes) |
| `accounting.lettering.difference-threshold` | INTEGER | LIVE | `500` | Ecart de reglement maximal au lettrage (XAF) |
| `accounting.lettering.gain-account` | TEXT | LIVE | `758` | Compte des ecarts de reglement favorables |
| `accounting.lettering.loss-account` | TEXT | LIVE | `658` | Compte des ecarts de reglement defavorables |
| `accounting.liasse.accountant` | TEXT | LIVE | `` | Liasse : expert-comptable |
| `accounting.liasse.activity` | TEXT | LIVE | `Location d'espaces de travail partages et services associes` | Liasse : activite principale |
| `accounting.liasse.auditor` | TEXT | LIVE | `` | Liasse : commissaire aux comptes |
| `accounting.liasse.tax-office` | TEXT | LIVE | `` | Liasse : centre des impots de rattachement |
| `accounting.liasse.tax-regime` | TEXT | LIVE | `Reel normal` | Liasse : regime fiscal |
| `accounting.loan.accrual-journal` | TEXT | LIVE | `OD` | Journal des interets courus des emprunts |
| `accounting.loan.alert-days` | INTEGER | LIVE | `7` | Alerte des comptables avant une echeance d'emprunt (jours) |
| `accounting.manual-entry.second-validation-threshold` | INTEGER | LIVE | `0` | Montant au-dela duquel une piece manuelle est validee par un autre comptable (0 : jamais) |
| `accounting.night-worker-cron` | CRON | LIVE | `0 30 2 * * *` | Planification du controle de continuite et des relances comptables |
| `accounting.notify-emails` | TEXT | LIVE | `` | Destinataires des alertes comptables (comptables, administrateur finance), separes par des virgules |
| `accounting.numbering.chronology` | ENUM | LIVE | `STRICT` | Ordre des pieces : STRICT (dates croissantes) ou PERIOD |
| `accounting.numbering.width` | INTEGER | LIVE | `6` | Longueur de la sequence des numeros de piece |
| `accounting.partner.account` | TEXT | LIVE | `462` | Compte des comptes courants d'associes (462, ou divisionnaire de 16 si bloques) |
| `accounting.partner.dividend-account` | TEXT | LIVE | `465` | Compte des dividendes a payer affectes en compte courant |
| `accounting.partner.interest-account` | TEXT | LIVE | `671` | Compte de charge des interets des comptes courants |
| `accounting.partner.journal` | TEXT | LIVE | `OD` | Journal des interets et mouvements sans tresorerie des comptes courants |
| `accounting.payroll.journal` | TEXT | LIVE | `PA` | Journal de paie |
| `accounting.payroll.section-required` | BOOLEAN | LIVE | `false` | Paie : refuser un salarie sans section analytique |
| `accounting.period.billing-closure-sync` | ENUM | LIVE | `PROPOSE` | Clore un mois de facturation : PROPOSE (notifier les comptables), LOCK (verrouiller la periode comptable) ou NONE |
| `accounting.posting.batch-size` | INTEGER | LIVE | `50` | Nombre d'evenements comptables traites par passage du moteur d'imputation |
| `accounting.posting.disabled-families` | TEXT | LIVE | `` | Familles d'evenements non comptabilisees pour l'instant (CASH, SALES, PAYMENT...), separees par des virgules |
| `accounting.posting.worker-delay-ms` | INTEGER | RESTART | `30000` | Intervalle du moteur d'imputation (millisecondes) |
| `accounting.posting.worker-delay-ms` | INTEGER | LIVE | `30000` | Intervalle du moteur d'imputation (millisecondes) |
| `accounting.provision.journal` | TEXT | LIVE | `OD` | Provisions : journal des dotations, reprises et reclassements |
| `accounting.provision.short-term-account` | TEXT | LIVE | `4991` | Provisions : compte des provisions pour risques a court terme |
| `accounting.provision.short-term-expense-account` | TEXT | LIVE | `6591` | Provisions : compte de dotation a court terme |
| `accounting.provision.short-term-reversal-account` | TEXT | LIVE | `7591` | Provisions : compte de reprise a court terme |
| `accounting.purchase.analytic-axis` | TEXT | LIVE | `` | Axe analytique des sections portees par les lignes d'achat (vide : ventilation par defaut des comptes) |
| `accounting.purchase.default-vat-rate` | DECIMAL | LIVE | `18` | Taux de TVA propose sur une ligne d'achat sans taux (en %) |
| `accounting.purchase.journal` | TEXT | LIVE | `AC` | Journal des achats (factures et avoirs fournisseurs) |
| `accounting.purchase.supplier-account` | TEXT | LIVE | `401` | Compte collectif des fournisseurs |
| `accounting.purchase.vat-account` | TEXT | LIVE | `4452` | TVA recuperable sur achats |
| `accounting.purchase.vat-account-fixed-assets` | TEXT | LIVE | `4451` | TVA recuperable sur immobilisations (comptes de classe 2) |
| `accounting.purchase.vat-account-services` | TEXT | LIVE | `4454` | TVA recuperable sur services exterieurs (comptes 62 et 63) |
| `accounting.purchase.withholding-account` | TEXT | LIVE | `447` | Compte des retenues a la source operees sur les fournisseurs |
| `accounting.receivable.adjustment` | ENUM | LIVE | `MONTHLY` | Creances : rythme de l'ajustement des depreciations (MONTHLY ou CLOSING_ONLY) |
| `accounting.receivable.auto-days` | INTEGER | LIVE | `365` | Creances : retard en jours au-dela duquel le classement en douteux est automatique |
| `accounting.receivable.cheque-rejected-days` | INTEGER | LIVE | `15` | Creances : jours apres le rejet d'un cheque non regularise avant proposition de classement |
| `accounting.receivable.contagion` | BOOLEAN | LIVE | `true` | Creances : proposer le classement des autres factures echues du client classe douteux |
| `accounting.receivable.cron` | CRON | LIVE | `0 15 4 * * *` | Creances a risque : planification du traitement de nuit (propositions, pertes, ajustement, controles) |
| `accounting.receivable.deposit-account` | TEXT | LIVE | `165` | Creances : compte des cautions recues deduites de la base de depreciation |
| `accounting.receivable.disputed-account` | TEXT | LIVE | `4161` | Creances : compte des creances litigieuses |
| `accounting.receivable.disputed-allowance-account` | TEXT | LIVE | `4911` | Creances : compte de depreciation des creances litigieuses |
| `accounting.receivable.doubtful-account` | TEXT | LIVE | `4162` | Creances : compte des creances douteuses |
| `accounting.receivable.doubtful-allowance-account` | TEXT | LIVE | `4912` | Creances : compte de depreciation des creances douteuses |
| `accounting.receivable.expense-account` | TEXT | LIVE | `6594` | Creances : compte de dotation aux depreciations |
| `accounting.receivable.grid` | TEXT | LIVE | `91-180:25,181-365:50,366+:100` | Creances : grille statistique de depreciation (jours de retard : taux en %) |
| `accounting.receivable.guarantee-account` | TEXT | LIVE | `4191` | Creances : compte des garanties deduites de la base (portefeuille du client) |
| `accounting.receivable.individual-threshold` | INTEGER | LIVE | `500000` | Creances : reste du HT au-dela duquel la methode individuelle est attendue (XAF) |
| `accounting.receivable.journal` | TEXT | LIVE | `OD` | Creances : journal des reclassements, depreciations et pertes |
| `accounting.receivable.loss-account` | TEXT | LIVE | `6511` | Creances : compte des pertes sur creances clients |
| `accounting.receivable.recovery-account` | TEXT | LIVE | `758` | Creances : compte de recuperation sur creances passees en perte |
| `accounting.receivable.release` | ENUM | LIVE | `IMMEDIATE` | Creances : reprise de la depreciation a l'encaissement (IMMEDIATE ou NEXT_ADJUSTMENT) |
| `accounting.receivable.reversal-account` | TEXT | LIVE | `7594` | Creances : compte de reprise des depreciations |
| `accounting.receivable.suspended-days` | INTEGER | LIVE | `30` | Creances : jours de suspension d'un abonnement pour impaye avant proposition de classement |
| `accounting.receivable.trigger-days` | INTEGER | LIVE | `90` | Creances : retard en jours au-dela duquel le classement en douteux est propose |
| `accounting.receivable.writeoff-direction-threshold` | INTEGER | LIVE | `100000` | Creances : perte TTC au-dela de laquelle la direction valide (XAF) |
| `accounting.receivable.writeoff-vat-recovery` | BOOLEAN | LIVE | `true` | Creances : recuperer la TVA collectee sur une creance passee en perte |
| `accounting.replay-until` | TEXT | LIVE | `` | Reprise : les faits dates avant ce jour produisent des brouillards a valider en bloc (vide : pas de reprise) |
| `accounting.revenue.deferral` | ENUM | LIVE | `MONTHLY` | Produits constates d'avance : MONTHLY, YEAR_END ou NONE |
| `accounting.revenue.deferred-account` | TEXT | LIVE | `477` | Compte des produits constates d'avance |
| `accounting.rounding.max-difference` | INTEGER | LIVE | `1` | Ecart d'arrondi maximal accepte sur une ligne explicite (XAF) |
| `accounting.sales.client-account` | TEXT | LIVE | `411` | Compte collectif des clients |
| `accounting.sales.deposit-account` | TEXT | LIVE | `4191` | Compte des acomptes recus des clients (factures d'acompte, soldé par la facture finale) |
| `accounting.sales.deposit-retention-account` | TEXT | LIVE | `758` | Compte de produit d'une caution retenue (dégâts, impayés) |
| `accounting.sales.disbursement-account` | TEXT | LIVE | `4718` | Compte des débours refacturés aux clients (frais avancés pour leur compte, hors TVA et hors chiffre d'affaires) |
| `accounting.sales.discount-account` | TEXT | LIVE | `709` | Compte des remises accordees sur facture |
| `accounting.sales.extra-tax-account` | TEXT | LIVE | `4431` | Compte du centime additionnel facture |
| `accounting.sales.journal` | TEXT | LIVE | `VT` | Journal des factures et avoirs |
| `accounting.sales.payment-delay-minutes` | INTEGER | LIVE | `5` | Delai avant comptabilisation d'un paiement, le temps de son imputation sur les factures (minutes) |
| `accounting.sales.security-deposit-account` | TEXT | LIVE | `165` | Compte des dépôts et cautionnements reçus des clients (cautions facturées) |
| `accounting.sales.unallocated-account` | TEXT | LIVE | `4719` | Compte de liaison des encaissements non imputes et du portefeuille |
| `accounting.sales.vat-account` | TEXT | LIVE | `4431` | Compte de TVA facturee |
| `accounting.start-date` | TEXT | LIVE | `2026-01-01` | Date de bascule : les faits anterieurs ne sont comptabilises que par la reprise |
| `accounting.tax.corporate-rate` | DECIMAL | LIVE | `28` | Taux de l'impot sur le resultat (en %) ; a valider par l'expert-comptable |
| `accounting.tax.expense-account` | TEXT | LIVE | `891` | Compte de charge de l'impot sur le resultat |
| `accounting.tax.journal` | TEXT | LIVE | `OD` | Journal de l'ecriture d'impot sur le resultat |
| `accounting.tax.minimum-rate` | DECIMAL | LIVE | `1` | Minimum de perception sur le chiffre d'affaires (en %) ; a valider par l'expert-comptable |
| `accounting.tax.opening-loss` | DECIMAL | LIVE | `0` | Deficits reportables a l'ouverture du premier exercice suivi |
| `accounting.tax.payable-account` | TEXT | LIVE | `441` | Compte de dette de l'impot sur le resultat |
| `accounting.unbalanced-entry.blocks-period-close` | BOOLEAN | LIVE | `true` | Un ecart non regularise bloque le verrouillage de la periode |
| `accounting.unbalanced-entry.policy` | ENUM | LIVE | `HOLD` | Piece desequilibree : HOLD (attente), SUSPENSE_ACCOUNT (compte d'attente) ou ACCEPT |
| `accounting.unbalanced-entry.suspense-account` | TEXT | LIVE | `4718` | Compte d'attente d'equilibrage |
| `accounting.unbalanced-entry.suspense-max-difference` | INTEGER | LIVE | `0` | Difference maximale passee en compte d'attente (0 : sans limite) |
| `accounting.vat.chargeability` | ENUM | LIVE | `INVOICE` | Exigibilite de la TVA facturee : INVOICE (a la facture, regime des debits) ou PAYMENT (a l'encaissement) |
| `accounting.vat.collected-accounts` | TEXT | LIVE | `4431,4432` | Comptes de TVA facturee repris dans la declaration |
| `accounting.vat.credit-account` | TEXT | LIVE | `4449` | Compte du credit de TVA a reporter |
| `accounting.vat.deductible-accounts` | TEXT | LIVE | `4451,4452,4454` | Comptes de TVA recuperable repris dans la declaration |
| `accounting.vat.deduction-prorata` | DECIMAL | LIVE | `100` | Prorata de deduction de la TVA recuperable, en pourcentage (100 : tout est deductible) |
| `accounting.vat.due-account` | TEXT | LIVE | `4441` | Compte de la TVA due a l'Etat |
| `accounting.vat.extra-tax-code` | TEXT | LIVE | `CA` | Code de taxe du centime additionnel sur les ecritures |
| `accounting.vat.journal` | TEXT | LIVE | `OD` | Journal de la piece de liquidation de la TVA |
| `accounting.vat.non-deductible-account` | TEXT | LIVE | `658` | Compte de charge de la TVA non deductible (prorata) a la liquidation |
| `accounting.vat.pending-account` | TEXT | LIVE | `4438` | Compte d'attente de la TVA facturee non encore exigible (regime des encaissements) |
| `accounting.wallet.admin-account` | TEXT | LIVE | `471` | Contrepartie des ajustements administratifs de portefeuille |
| `accounting.wallet.fee-account` | TEXT | LIVE | `706` | Compte des frais de transfert entre portefeuilles |
| `accounting.wallet.journal` | TEXT | LIVE | `PF` | Journal du portefeuille electronique |
| `accounting.wallet.promotion-account` | TEXT | LIVE | `658` | Compte des credits promotionnels et remises en argent |
| `inventory.supplier-suggestion.limit` | INTEGER | LIVE | `5` | Nombre de fournisseurs proposes par la suggestion de fournisseurs (au plus 50) |

## BILLING

Facturation · SUPER_ADMIN seul · 21 réglage(s)

| Clé | Type | Effet | Défaut | Libellé |
| --- | --- | --- | --- | --- |
| `billing.catalog.reference-lists.strict` | BOOLEAN | LIVE | `false` | Catalogue : refuser une famille, sous-famille, unité ou taxe absente des listes (sinon elle est ajoutée à la liste, et un code de taxe inconnu est gardé tel quel) |
| `billing.collection.plan-approval-months` | INTEGER | LIVE | `3` | Recouvrement : au-delà de ce nombre de mois, un plan de règlement demande le droit d'approbation de la facturation (responsable N+1) |
| `billing.deal.auto-create` | BOOLEAN | LIVE | `true` | Affaires : le premier devis d'un client sans affaire ouverte en crée une (sinon il rejoint son unique affaire ouverte) |
| `billing.discount.override-mode` | ENUM | LIVE | `APPROVAL` | Remise ou prix hors limites : APPROVAL (demande au responsable N+1 par le centre d'approbation, l'émission attend son accord) ou PERMISSION (droit de dérogation et motif, sans circuit) |
| `billing.invoice.equivalent-currencies` | TEXT | LIVE | `USD,EUR` | Facturation : devises dont l'équivalent figure sur les factures (codes séparés par des virgules ; vide : aucune) |
| `billing.quote.revision-validity-days` | INTEGER | LIVE | `30` | Devis : jours de validité garantis après une révision (la date de validité est prolongée si besoin) |
| `billing.stock.issue-on` | ENUM | LIVE | `INVOICE` | Sortie de stock des articles vendus : INVOICE (à l'émission de la facture), DELIVERY (à la livraison de chaque bon de livraison, la facture ne sort plus rien) ou NONE (aucune sortie automatique) |
| `bokati.billing.default-currency` | TEXT | LIVE | `XAF` | Devise par défaut |
| `bokati.billing.dunning.batch-size` | INTEGER | LIVE | `200` | Taille de lot des relances |
| `bokati.billing.dunning.cron` | CRON | LIVE | `0 30 8 * * *` | Planification des relances |
| `bokati.billing.dunning.handover-email` | EMAIL | LIVE | `` | Adresse de reprise des impayés |
| `bokati.billing.workers.annual-closure-cron` | CRON | LIVE | `0 0 1 1 1 *` | Planification de la clôture annuelle |
| `bokati.billing.workers.daily-closure-cron` | CRON | LIVE | `0 30 23 * * *` | Planification de la clôture journalière |
| `bokati.billing.workers.fiscal-integrity-cron` | CRON | LIVE | `0 0 2 * * *` | Planification du contrôle d'intégrité fiscale |
| `bokati.billing.workers.monthly-closure-cron` | CRON | LIVE | `0 15 0 1 * *` | Planification de la clôture mensuelle |
| `bokati.billing.workers.overdue-delay-ms` | INTEGER | LIVE | `3600000` | Cadence du suivi des factures échues (ms) |
| `bokati.billing.workers.payment-reminder-cooldown-days` | INTEGER | LIVE | `7` | Intervalle entre deux rappels (jours) |
| `bokati.billing.workers.payment-reminder-cron` | CRON | LIVE | `0 0 8 * * *` | Planification des rappels de paiement |
| `bokati.billing.workers.payment-reminder-max` | INTEGER | LIVE | `3` | Nombre maximum de rappels |
| `bokati.billing.workers.quote-expiry-delay-ms` | INTEGER | LIVE | `3600000` | Cadence du passage en expiré des devis échus (ms) |
| `bokati.billing.workers.schedule-overdue-delay-ms` | INTEGER | LIVE | `3600000` | Cadence du suivi des échéanciers (ms) |

## COMPLIANCE

Conformité · SUPER_ADMIN seul · 11 réglage(s)

| Clé | Type | Effet | Défaut | Libellé |
| --- | --- | --- | --- | --- |
| `bokati.compliance.alert-email` | EMAIL | LIVE | `` | Adresse(s) prévenue(s) chaque jour des dossiers de conformité hors délai (séparées par des virgules) |
| `bokati.compliance.case.due-hours.critical` | INTEGER | LIVE | `24` | Délai d'un dossier critique (heures) |
| `bokati.compliance.case.due-hours.high` | INTEGER | LIVE | `72` | Délai d'un dossier élevé (heures) |
| `bokati.compliance.case.due-hours.low` | INTEGER | LIVE | `336` | Délai d'un dossier faible (heures) |
| `bokati.compliance.case.due-hours.medium` | INTEGER | LIVE | `168` | Délai d'un dossier moyen (heures) |
| `bokati.compliance.identity.cron` | CRON | LIVE | `0 50 3 * * *` | Planification du contrôle d'identité |
| `bokati.compliance.identity.exposure-threshold` | INTEGER | LIVE | `1000000` | Seuil d'exposition |
| `bokati.compliance.identity.review-days` | INTEGER | LIVE | `365` | Revue d'identité (jours) |
| `bokati.compliance.overdue.cron` | CRON | LIVE | `0 0 8 * * *` | Planification du suivi des dossiers en retard |
| `bokati.compliance.screening.cron` | CRON | LIVE | `0 40 3 * * *` | Planification du filtrage |
| `bokati.compliance.screening.recheck-days` | INTEGER | LIVE | `180` | Nouveau filtrage (jours) |

## DOCUMENTS

Documents · SUPER_ADMIN seul · 38 réglage(s)

| Clé | Type | Effet | Défaut | Libellé |
| --- | --- | --- | --- | --- |
| `app.document.default-expiry-years` | INTEGER | LIVE | `5` | Expiration par défaut d'un document (années) |
| `app.document.retention.cron` | CRON | LIVE | `0 30 2 * * *` | Planification de la rétention des documents |
| `app.document.retention.enabled` | BOOLEAN | LIVE | `true` | Rétention des documents active |
| `app.document.storage.minio.access-key` | SECRET | RESTART | `` | Clé d'accès du stockage |
| `app.document.storage.minio.bucket` | TEXT | RESTART | `bokati-documents` | Compartiment de stockage |
| `app.document.storage.minio.endpoint` | TEXT | RESTART | `http://localhost:9000` | Adresse du stockage |
| `app.document.storage.minio.region` | TEXT | RESTART | `` | Région du stockage |
| `app.document.storage.minio.secret-key` | SECRET | RESTART | `` | Clé secrète du stockage |
| `app.document.storage.provider` | TEXT | RESTART | `filesystem` | Fournisseur de stockage |
| `app.document.storage.root-path` | TEXT | RESTART | `storage/documents` | Dossier racine du stockage |
| `app.document.worker.enabled` | BOOLEAN | LIVE | `true` | Traitement des documents actif |
| `app.document.worker.fixed-delay-ms` | INTEGER | LIVE | `3600000` | Cadence du traitement des documents (ms) |
| `app.documents.preview-token.secret` | SECRET | RESTART | `` | Secret des liens d'aperçu |
| `app.documents.preview-token.ttl-seconds` | INTEGER | LIVE | `300` | Validité d'un lien d'aperçu (secondes) |
| `app.documents.public-preview-enabled` | BOOLEAN | RESTART | `true` | Aperçu public des documents |
| `app.ged.filing.batch-size` | INTEGER | LIVE | `1000` | Documents rangés par passage du classement GED |
| `app.ged.filing.enabled` | BOOLEAN | LIVE | `true` | Classement automatique de la GED actif |
| `app.ged.filing.fixed-delay-ms` | INTEGER | LIVE | `3600000` | Cadence du classement GED (ms) |
| `app.ged.filing.initial-delay-ms` | INTEGER | RESTART | `120000` | Délai avant le premier classement GED (ms) |
| `app.ged.quota.total-bytes` | INTEGER | LIVE | `0` | Quota d'espace de toute la GED en octets (0 : aucun) |
| `app.ged.text.batch-size` | INTEGER | LIVE | `20` | Fichiers GED lus par passage |
| `app.ged.text.enabled` | BOOLEAN | LIVE | `true` | Lecture du texte des fichiers GED (recherche) active |
| `app.ged.text.fixed-delay-ms` | INTEGER | LIVE | `60000` | Cadence de lecture des fichiers GED (ms) |
| `app.ged.text.initial-delay-ms` | INTEGER | RESTART | `180000` | Délai avant la première lecture des fichiers GED (ms) |
| `app.ged.text.max-chars` | INTEGER | LIVE | `300000` | Caractères gardés au plus, par fichier GED |
| `app.ged.text.max-ocr-pages` | INTEGER | LIVE | `30` | Pages lues par OCR au plus, par fichier GED |
| `app.ged.text.ocr-data-path` | TEXT | LIVE | `` | Dossier des langues Tesseract (tessdata) pour la GED |
| `app.ged.text.ocr-enabled` | BOOLEAN | LIVE | `true` | OCR des scans et images de la GED |
| `app.ged.text.ocr-language` | TEXT | LIVE | `fra+eng` | Langues de l'OCR de la GED (Tesseract) |
| `app.ged.trash.purge-cron` | CRON | LIVE | `0 15 3 * * *` | Heure du vidage de la corbeille GED |
| `app.ged.trash.purge-enabled` | BOOLEAN | LIVE | `true` | Vidage automatique de la corbeille GED |
| `app.ged.trash.retention-days` | INTEGER | LIVE | `30` | Jours en corbeille GED avant destruction |
| `app.kyc.identity.document-types` | TEXT | LIVE | `ID_CARD,NATIONAL_ID,PASSPORT,DRIVERS_LICENSE,RESIDENCE_PERMIT,REPRESENTATIVE_ID` | Pièces KYC : morceaux de code des types de pièces d'identité analysés au dépôt (séparés par des virgules) |
| `app.kyc.identity.enabled` | BOOLEAN | LIVE | `true` | Pièces KYC : analyse « pièce d'identité » au dépôt (contour, format, MRZ, visage avec OpenCV), avertissement seulement |
| `app.kyc.identity.engine` | ENUM | LIVE | `JAVA` | Pièces KYC : moteur d'analyse des pièces d'identité, JAVA (défaut, sans bibliothèque native) ou OPENCV (visage, plus de mémoire) |
| `app.kyc.quality.blur-threshold` | INTEGER | LIVE | `100` | Pièces KYC : seuil de netteté (variance du laplacien) sous lequel l'image est signalée floue |
| `app.kyc.quality.enabled` | BOOLEAN | LIVE | `true` | Pièces KYC : contrôle de qualité d'image au dépôt |
| `app.kyc.quality.min-side-px` | INTEGER | LIVE | `600` | Pièces KYC : plus petit côté minimal de l'image (pixels) |

## LOCALE

Langue et téléphone · SUPER_ADMIN seul · 5 réglage(s)

| Clé | Type | Effet | Défaut | Libellé |
| --- | --- | --- | --- | --- |
| `app.locale` | TEXT | RESTART | `fr_FR` | Langue de la plateforme |
| `app.phone.default-country` | TEXT | LIVE | `CG` | Pays par défaut des téléphones |
| `app.phone.default-dial-code` | TEXT | LIVE | `+242` | Indicatif par défaut |
| `app.phone.keep-trunk-zero` | TEXT | LIVE | `CG,GA,CI,IT` | Pays qui gardent le zéro initial |
| `app.time-zone` | TEXT | RESTART | `Africa/Lagos` | Fuseau horaire |

## SECURITY

Sécurité · SUPER_ADMIN seul · 37 réglage(s)

| Clé | Type | Effet | Défaut | Libellé |
| --- | --- | --- | --- | --- |
| `app.security.bootstrap.secret` | SECRET | RESTART | `` | Secret d'initialisation |
| `app.security.cors.allowed-origins` | TEXT | RESTART | `*` | Origines autorisées |
| `app.security.failed-login.lock-minutes` | INTEGER | LIVE | `30` | Durée du verrouillage après trop d'échecs de connexion (minutes, 0 : jusqu'au déverrouillage manuel) |
| `app.security.failed-login.max-attempts` | INTEGER | LIVE | `5` | Essais de connexion avant blocage |
| `app.security.jwt.access-token-expiration-ms` | INTEGER | LIVE | `900000` | Durée d'un jeton d'accès (ms) |
| `app.security.jwt.refresh-reuse-grace-seconds` | INTEGER | LIVE | `30` | Grâce de réutilisation du jeton de rafraîchissement (secondes) |
| `app.security.jwt.refresh-token-expiration-ms` | INTEGER | LIVE | `604800000` | Durée d'un jeton de rafraîchissement (ms) |
| `app.security.jwt.secret` | SECRET | RESTART | `` | Secret des jetons |
| `app.security.jwt.verification-token-expiration-ms` | INTEGER | LIVE | `900000` | Durée d'un jeton de vérification (ms) |
| `app.security.one-time-token.expiration-ms` | INTEGER | LIVE | `900000` | Durée d'un jeton à usage unique (ms) |
| `app.security.password-reset.expiration-ms` | INTEGER | LIVE | `900000` | Durée d'un lien de mot de passe oublié (ms) |
| `app.security.rate-limit.admin.max-requests` | INTEGER | LIVE | `600` | Plafond de requêtes · administration |
| `app.security.rate-limit.admin.window-seconds` | INTEGER | LIVE | `60` | Fenêtre · administration (secondes) |
| `app.security.rate-limit.callback.max-requests` | INTEGER | LIVE | `120` | Plafond de requêtes · rappels des opérateurs |
| `app.security.rate-limit.callback.window-seconds` | INTEGER | LIVE | `60` | Fenêtre · rappels des opérateurs (secondes) |
| `app.security.rate-limit.checkin.max-requests` | INTEGER | LIVE | `120` | Plafond de requêtes · check-in |
| `app.security.rate-limit.checkin.window-seconds` | INTEGER | LIVE | `60` | Fenêtre · check-in (secondes) |
| `app.security.rate-limit.default.max-requests` | INTEGER | LIVE | `300` | Plafond de requêtes · par défaut |
| `app.security.rate-limit.default.window-seconds` | INTEGER | LIVE | `60` | Fenêtre · par défaut (secondes) |
| `app.security.rate-limit.enabled` | BOOLEAN | LIVE | `true` | Plafonds de requêtes actifs |
| `app.security.rate-limit.login.max-requests` | INTEGER | LIVE | `5` | Plafond de requêtes · connexion |
| `app.security.rate-limit.login.window-seconds` | INTEGER | LIVE | `60` | Fenêtre · connexion (secondes) |
| `app.security.rate-limit.otp.max-requests` | INTEGER | LIVE | `3` | Plafond de requêtes · codes à usage unique |
| `app.security.rate-limit.otp.window-seconds` | INTEGER | LIVE | `600` | Fenêtre · codes à usage unique (secondes) |
| `app.security.rate-limit.public-write.max-requests` | INTEGER | LIVE | `10` | Plafond de requêtes · formulaires publics |
| `app.security.rate-limit.public-write.window-seconds` | INTEGER | LIVE | `600` | Fenêtre · formulaires publics (secondes) |
| `app.security.rate-limit.register.max-requests` | INTEGER | LIVE | `5` | Plafond de requêtes · inscription |
| `app.security.rate-limit.register.window-seconds` | INTEGER | LIVE | `3600` | Fenêtre · inscription (secondes) |
| `app.security.rate-limit.trusted-proxy-count` | INTEGER | LIVE | `1` | Nombre de relais de confiance |
| `app.security.rate-limit.upload.max-requests` | INTEGER | LIVE | `5` | Plafond de requêtes · envois de fichiers |
| `app.security.rate-limit.upload.window-seconds` | INTEGER | LIVE | `600` | Fenêtre · envois de fichiers (secondes) |
| `app.security.rate-limit.verify.max-requests` | INTEGER | LIVE | `60` | Plafond de requêtes · vérification |
| `app.security.rate-limit.verify.window-seconds` | INTEGER | LIVE | `600` | Fenêtre · vérification (secondes) |
| `app.security.registration.blocked-domains` | TEXT | LIVE | `` | Domaines d'e-mail bloqués à l'inscription |
| `app.security.registration.dns-timeout-ms` | INTEGER | LIVE | `3000` | Délai de vérification DNS (ms) |
| `app.security.registration.mx-check-enabled` | BOOLEAN | LIVE | `true` | Vérification du domaine d'e-mail |
| `app.security.token-hash.secret` | SECRET | RESTART | `` | Secret d'empreinte des jetons |

## AUDIT

Centre d'audit · ouvert à ADMIN · 24 réglage(s)

| Clé | Type | Effet | Défaut | Libellé |
| --- | --- | --- | --- | --- |
| `bokati.audit.alerts.channels` | TEXT | LIVE | `APP,EMAIL,WHATSAPP` | Canaux des alertes du centre d'audit (APP, EMAIL, WHATSAPP, séparés par des virgules) |
| `bokati.audit.alerts.email-failures` | INTEGER | LIVE | `5` | Alerte : courriels en échec dans l'heure (0 : coupée) |
| `bokati.audit.alerts.email-recipients` | TEXT | LIVE | `` | Courriels des administrateurs qui reçoivent les alertes (séparés par des virgules) |
| `bokati.audit.alerts.error-rate-percent` | INTEGER | LIVE | `5` | Alerte : taux d'erreurs serveur sur 5 minutes (%, 0 : coupée) |
| `bokati.audit.alerts.failed-logins` | INTEGER | LIVE | `20` | Alerte : échecs de connexion en 15 minutes (0 : coupée) |
| `bokati.audit.alerts.locked-accounts` | INTEGER | LIVE | `3` | Alerte : comptes verrouillés dans l'heure (0 : coupée) |
| `bokati.audit.alerts.outbox-dead` | INTEGER | LIVE | `1` | Alerte : messages abandonnés de la boîte d'envoi dans l'heure (0 : coupée) |
| `bokati.audit.alerts.outbox-wait-minutes` | INTEGER | LIVE | `15` | Alerte : attente du plus ancien message de la boîte d'envoi (minutes, 0 : coupée) |
| `bokati.audit.alerts.p95-ms` | INTEGER | LIVE | `3000` | Alerte : durée sous laquelle répondent 95 % des requêtes sur 15 minutes (ms, 0 : coupée) |
| `bokati.audit.alerts.pawapay-failures` | INTEGER | LIVE | `3` | Alerte : rappels PawaPay en échec dans l'heure (0 : coupée) |
| `bokati.audit.alerts.server-errors` | INTEGER | LIVE | `10` | Alerte : erreurs serveur en 15 minutes (0 : coupée) |
| `bokati.audit.alerts.task-failures` | INTEGER | LIVE | `3` | Alerte : échecs consécutifs d'une tâche planifiée (0 : coupée) |
| `bokati.audit.alerts.whatsapp-failures` | INTEGER | LIVE | `5` | Alerte : messages WhatsApp refusés ou non remis dans l'heure (0 : coupée) |
| `bokati.audit.alerts.whatsapp-recipients` | TEXT | LIVE | `` | Numéros WhatsApp qui reçoivent les alertes (séparés par des virgules) |
| `bokati.audit.export.max-rows` | INTEGER | LIVE | `10000` | Lignes au plus dans un export CSV du journal des actions |
| `bokati.audit.retention.actions-days` | INTEGER | LIVE | `1825` | Conservation du journal des actions (jours, 0 : sans limite) |
| `bokati.audit.retention.alerts-days` | INTEGER | LIVE | `365` | Conservation des alertes fermées (jours, 0 : sans limite) |
| `bokati.audit.retention.errors-days` | INTEGER | LIVE | `90` | Conservation des erreurs serveur (jours, 0 : sans limite) |
| `bokati.audit.retention.jobs-days` | INTEGER | LIVE | `90` | Conservation des exécutions de tâches gardées une par une (jours, 0 : sans limite) |
| `bokati.audit.retention.requests-days` | INTEGER | LIVE | `30` | Conservation des requêtes lentes ou en erreur gardées une par une (jours, 0 : sans limite) |
| `bokati.audit.retention.security-days` | INTEGER | LIVE | `365` | Conservation des événements de sécurité (jours, 0 : sans limite) |
| `bokati.audit.retention.traffic-hours-days` | INTEGER | LIVE | `395` | Conservation du trafic par heure (jours, 0 : sans limite) |
| `bokati.audit.retention.traffic-minutes-days` | INTEGER | LIVE | `7` | Conservation du trafic par minute (jours, 0 : sans limite) |
| `bokati.audit.traffic.slow-ms` | INTEGER | LIVE | `2000` | Durée à partir de laquelle une requête est gardée une par une comme lente (ms) |

## NOTIFICATIONS

Notifications et e-mails · SUPER_ADMIN seul · 15 réglage(s)

| Clé | Type | Effet | Défaut | Libellé |
| --- | --- | --- | --- | --- |
| `bokati.email.dedup.business-window-seconds` | INTEGER | LIVE | `86400` | Anti-doublon métier (secondes) |
| `bokati.email.dedup.enabled` | BOOLEAN | LIVE | `true` | Anti-doublon d'e-mails actif |
| `bokati.email.dedup.generic-window-seconds` | INTEGER | LIVE | `3600` | Anti-doublon générique (secondes) |
| `bokati.mail.requeue.after-seconds` | INTEGER | LIVE | `120` | Renvoi d'un e-mail bloqué après (secondes) |
| `bokati.mail.requeue.delay-ms` | INTEGER | LIVE | `60000` | Cadence des renvois d'e-mails (ms) |
| `bokati.notification.worker.batch-size` | INTEGER | LIVE | `25` | Taille de lot des notifications |
| `bokati.notification.worker.delay-ms` | INTEGER | LIVE | `30000` | Cadence des notifications (ms) |
| `bokati.notification.worker.enabled` | BOOLEAN | LIVE | `true` | Envoi des notifications actif |
| `bokati.whatsapp.auto-notifications.enabled` | BOOLEAN | LIVE | `true` | Messages WhatsApp automatiques aux clients (réservation, abonnement, facture soldée, portefeuille) |
| `bokati.whatsapp.portal-path.bookings` | TEXT | LIVE | `client/bookings` | Page de l'espace client ouverte par le bouton WhatsApp : réservations |
| `bokati.whatsapp.portal-path.invoices` | TEXT | LIVE | `client/invoices` | Page de l'espace client ouverte par le bouton WhatsApp : factures, proformas et avoirs |
| `bokati.whatsapp.portal-path.kyc` | TEXT | LIVE | `client/kyc` | Page de l'espace client ouverte par le bouton WhatsApp : pièces KYC à déposer |
| `bokati.whatsapp.portal-path.quotes` | TEXT | LIVE | `client/quotes` | Page de l'espace client ouverte par le bouton WhatsApp : devis |
| `bokati.whatsapp.portal-path.subscriptions` | TEXT | LIVE | `client/subscriptions` | Page de l'espace client ouverte par le bouton WhatsApp : abonnements |
| `bokati.whatsapp.portal-path.wallet` | TEXT | LIVE | `client/wallet` | Page de l'espace client ouverte par le bouton WhatsApp : portefeuille |

## WEBHOOKS

Webhooks · SUPER_ADMIN seul · 3 réglage(s)

| Clé | Type | Effet | Défaut | Libellé |
| --- | --- | --- | --- | --- |
| `bokati.webhook.worker.batch-size` | INTEGER | LIVE | `25` | Taille de lot des webhooks |
| `bokati.webhook.worker.delay-ms` | INTEGER | LIVE | `60000` | Cadence des webhooks (ms) |
| `bokati.webhook.worker.enabled` | BOOLEAN | LIVE | `true` | Envoi des webhooks actif |

## OUTBOX

Boîte d'envoi · SUPER_ADMIN seul · 3 réglage(s)

| Clé | Type | Effet | Défaut | Libellé |
| --- | --- | --- | --- | --- |
| `app.outbox.worker.batch-size` | INTEGER | LIVE | `25` | Taille de lot de la boîte d'envoi |
| `app.outbox.worker.enabled` | BOOLEAN | LIVE | `true` | Boîte d'envoi active |
| `app.outbox.worker.fixed-delay-ms` | INTEGER | LIVE | `2000` | Cadence de la boîte d'envoi (ms) |

## GRAPH_API

API Graph · SUPER_ADMIN seul · 3 réglage(s)

| Clé | Type | Effet | Défaut | Libellé |
| --- | --- | --- | --- | --- |
| `bokati.graph-api.rate-limit.limit-for-period` | INTEGER | RESTART | `25` | Requêtes par période |
| `bokati.graph-api.rate-limit.limit-refresh-period-ms` | INTEGER | LIVE | `60000` | Durée d'une période (ms) |
| `bokati.graph-api.rate-limit.timeout-ms` | INTEGER | LIVE | `30000` | Délai d'attente (ms) |

## WEBSOCKET

WebSocket · SUPER_ADMIN seul · 10 réglage(s)

| Clé | Type | Effet | Défaut | Libellé |
| --- | --- | --- | --- | --- |
| `bokati.websocket.stomp.broker-url` | SECRET | RESTART | `` | Adresse du relais de messages |
| `bokati.websocket.stomp.client-login` | SECRET | RESTART | `` | Identifiant client du relais |
| `bokati.websocket.stomp.client-passcode` | SECRET | RESTART | `` | Mot de passe client du relais |
| `bokati.websocket.stomp.relay-host` | TEXT | RESTART | `localhost` | Hôte du relais |
| `bokati.websocket.stomp.relay-port` | INTEGER | RESTART | `61613` | Port du relais |
| `bokati.websocket.stomp.system-login` | SECRET | RESTART | `` | Identifiant système du relais |
| `bokati.websocket.stomp.system-passcode` | SECRET | RESTART | `` | Mot de passe système du relais |
| `bokati.websocket.stomp.use-tls` | BOOLEAN | RESTART | `false` | Relais chiffré |
| `bokati.websocket.stomp.virtual-host` | TEXT | RESTART | `/` | Hôte virtuel du relais |
| `bokati.websocket.use-stomp-relay` | BOOLEAN | RESTART | `false` | Relais de messages actif |

## ENVIRONMENT

Adresses et environnement · SUPER_ADMIN seul · 8 réglage(s)

| Clé | Type | Effet | Défaut | Libellé |
| --- | --- | --- | --- | --- |
| `app.admin-access-url` | TEXT | LIVE | `https://admin.elleaose.com` | Adresse de l'administration |
| `app.api-base-url` | TEXT | LIVE | `http://localhost:8080` | Adresse de l'API |
| `app.company.machine-code` | TEXT | LIVE | `00000` | Code machine de l'exploitant |
| `app.dev-mode` | BOOLEAN | LIVE | `false` | Mode développement |
| `app.error.verbose` | BOOLEAN | RESTART | `false` | Erreurs détaillées |
| `app.frontend-url` | TEXT | LIVE | `https://elleaose.com` | Adresse du portail |
| `app.referral.share-url-template` | TEXT | LIVE | `https://elleaose.com/inscription?parrain={code}` | Adresse d'un lien de parrainage ({code} remplacé par le code) |
| `app.verify-base-url` | TEXT | LIVE | `https://api.elleaose.com` | Adresse de vérification |
