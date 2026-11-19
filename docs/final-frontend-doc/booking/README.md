# booking : fonctionnement

> Rédigé d'après le code, à jour des lots B0 à B8 et B10 (19/11/2026). Ce qui n'est pas encore fait est signalé « lot Bx à venir ». Routes exactes, droits et champs JSON : [routes.md](routes.md) et `routes/`. Règles communes : [README racine](../README.md).

## En bref

Le module gère les réservations de ressources (salles, postes, bureaux) saisies par le back-office : vérification de disponibilité et devis, blocages temporaires (holds), création simple ou récurrente, approbation, règlement (paiement direct, portefeuille, abonnement, pass), arrivée et départ, absence, annulation avec remboursement, liste d'attente, quotas par public. Il expose aussi des pages publiques à jeton (confirmation, PDF, auto-pointage, borne de scan, satisfaction). Le portail membre réserve par ses propres routes `/sni/api/v1/client/bookings/**` (module [portal](../portal/routes.md)), qui s'appuient sur les mêmes règles. Modules liés : [ressource](../ressource/README.md) (créneaux, politiques, prix), `subscription` (droits d'abonnement et de pass), `billing` et `payment` (facture, paiement, portefeuille, avoir), `client` et `company` (titulaires), `task` et `support` (tâches et tickets automatiques).

## Notions

| Objet | Identifiant exposé | Rôle |
| --- | --- | --- |
| Réservation (`Booking`) | `bookingNumber` (`BKG-...`) | Une ressource, une plage, une quantité, un titulaire, un mode de règlement, un statut. Toutes les routes la désignent par ce numéro. |
| Titulaire | `ownerType` (`MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY`) + `ownerCode` | Membre (`memberId`), client (`customerId`), entreprise (`businessCode`), ou invité de passage (type `CUSTOMER` avec un code généré qui n'est pas un vrai client). |
| Contact | `contactName`, `contactEmail`, `contactPhone` | Destinataire des courriels et notifications. Pris dans la requête, sinon sur la fiche du titulaire. |
| Blocage temporaire (`BookingHold`) | `holdNumber` | Retient la capacité d'une plage pendant une durée limitée, le temps de finaliser. |
| Ligne (`lines`) | aucune | Détail du montant : ligne `RESOURCE` (prix) et, pour abonnement ou pass, ligne `ENTITLEMENT` (ce qui est décompté du droit). |
| Participant | `id` numérique ⚠ int64 | Personnes invitées (`HOST`, `GUEST`, `ORGANIZER`, `MEMBER`), statut `INVITED` ou `REMOVED`. |
| Historique de statut | aucun | `GET .../history` : `fromStatus`, `toStatus`, `changedBy` (lu dans la session de l'auteur), `reason`, `changedAt`. Textes en français depuis le lot B8. |
| Événement | `eventNumber` | `GET .../events` : journal fin (ressource réservée, droit consommé, courriel envoyé, pointage...). |
| Entrée de liste d'attente | `id` numérique ⚠ int64 | Demande sur une plage prise, avec offre à durée limitée. |
| Politique d'audience | `policyNumber` | Quotas et approbation par type de titulaire, type et groupe de ressource. |
| Dérogation de quota | `overrideNumber` | Quotas supplémentaires pour un titulaire, sur une période. |
| Jeton de pointage | `checkInToken` (`XXXX-XXXX`) | Sert à la fois de clé des pages publiques (confirmation, PDF) et de code de pointage. `checkInQrValue` est une **image** (data URI PNG) du QR qui pointe vers la page de scan. |

Identifiants numériques à garder en texte (lecture JSON sans perte, voir [README racine](../README.md#identifiants--codes-en-texte)) : `participantId` dans `DELETE .../participants/{participantId}`, `id` d'une entrée de liste d'attente dans `PATCH /bookings/waitlist/{id}/cancel`, `id` des règles d'annulation.

### Dates et heures

`startedAt`, `endedAt`, `newStartedAt`, les filtres `startedFrom`/`startedTo` sont des `LocalDateTime` : date-heure **sans fuseau**, heure murale de l'espace (`2026-11-09T09:00:00`). Le générateur les décrit « date-heure ISO 8601 », mais il ne faut pas y ajouter `Z` ni de décalage. Le serveur les compare à l'heure de l'application (`app.time-zone`, Africa/Brazzaville par défaut), quel que soit le fuseau de la machine (lot B8). Les horodatages techniques (`confirmedAt`, `cancelledAt`, `createdAt`, `changedAt`...) sont en revanche des `Instant` UTC. Les plages doivent tomber sur les créneaux de la ressource (multiples de `slotDurationMinutes`, sans secondes).

### Modes de règlement (`paymentMode`)

| Mode | Payant | Effet |
| --- | --- | --- |
| `DIRECT` | oui | Attend un paiement venu de l'extérieur (espèces, mobile money...) : la réservation reste `PENDING_PAYMENT`, **retient son créneau** jusqu'à son échéance de paiement (voir [Échéance de paiement](#échéance-de-paiement-et-expiration)), et sa facture est émise. Confirmée automatiquement quand l'intention de paiement est soldée. |
| `WALLET` | oui | Les fonds sont **bloqués** sur le portefeuille du titulaire (ou de l'organisation choisie) au moment de la confirmation : la réservation est confirmée tout de suite. Le débit réel suit une minute plus tard. Solde insuffisant : la création échoue. |
| `SUBSCRIPTION` | non | Couverte par un droit d'un abonnement actif du titulaire (ou d'une place sur l'abonnement d'une organisation). Le droit est réservé à la confirmation, consommé à la clôture ou à l'absence, rendu à l'annulation. Si le droit est épuisé mais que le plan autorise le dépassement, la réservation passe. |
| `PASS` | non | Idem avec un droit porté par un pass (du titulaire ou d'une organisation). |

`paymentStatus` et `paymentStatusLabel` sont calculés par le serveur : `COVERED` (« Couvert ») pour abonnement et pass, `CANCELLED` (« Annulé ») pour une réservation annulée, refusée ou expirée, sinon `PAID` (« Payé ») si un règlement est **constaté** (paiement reçu par la chaîne de paiement, ou portefeuille bloqué à la confirmation), `PENDING` (« En attente de paiement ») sinon. Une réservation confirmée à la main ou saisie après coup sans règlement reste donc `PENDING` (« à régler »), et un complément à encaisser après un report la repasse `PENDING` (lot B2b). Afficher le libellé tel quel.

## Statuts et cycle de vie

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `DRAFT` | Créée, non confirmée | `POST /bookings` avec `confirmImmediately: false` (mode non `DIRECT`, sans approbation) ; approbation d'une réservation non `DIRECT` (transitoire) | `CONFIRMED` (`PATCH .../confirm`), `CANCELLED` |
| `PENDING_APPROVAL` | Attend l'accord du back-office ; créneau retenu | Création quand la politique d'audience exige une approbation, ou ressource « sur demande » réservée au portail par un client non désigné (lot B10) | `CONFIRMED` ou `PENDING_PAYMENT` (`PATCH .../approve`), `REJECTED` (`PATCH .../reject`), `CANCELLED` |
| `PENDING_PAYMENT` | Attend le paiement (`DIRECT`) ; créneau retenu jusqu'à l'échéance | Création en `DIRECT` ; approbation d'une réservation `DIRECT` ; confirmation défaite (paiement retourné) | `CONFIRMED` (paiement soldé, ou `PATCH .../confirm`), `EXPIRED` (échéance passée), `CANCELLED` (manuel, annonce d'espèces expirée, facture annulée) |
| `CONFIRMED` | Ressource et droit réservés | Confirmation (auto à la création, manuelle, après paiement, après approbation) | `IN_PROGRESS`, `COMPLETED`, `NO_SHOW`, `CANCELLED`, `PENDING_PAYMENT` (paiement retourné) |
| `IN_PROGRESS` | En cours d'occupation | `PATCH .../start` ; pointage à l'heure ou après ; worker (pointé d'avance, heure atteinte) ; saisie après coup par l'équipe | `COMPLETED`, `NO_SHOW`, `CANCELLED` |
| `COMPLETED` | Terminée | `PATCH .../complete` (une fois commencée) ; worker : fin passée avec pointage ou démarrage | `CANCELLED` seulement par le système (remboursement ou annulation de facture) |
| `NO_SHOW` | Absence | `PATCH .../no-show` (après la fenêtre d'arrivée) ; worker : fin passée sans pointage ni démarrage | aucun (ne s'annule plus, lot B1) |
| `REJECTED` | Refusée à l'approbation | `PATCH .../reject` | aucun |
| `CANCELLED` | Annulée | `PATCH .../cancel` ; annulation système | aucun (rappeler `cancel` ne fait rien) |
| `EXPIRED` | Non réglée à l'échéance | Worker d'expiration (lot B1) : créneau rendu, facture annulée, tâches annulées, client prévenu | aucun |

Le serveur nomme les statuts en français dans ses messages (« confirmée », « en attente de paiement », « marquée absente »...) ; l'écran garde sa propre table de libellés pour les badges.

Le pointage (check-in) et la sortie (check-out) ne sont pas des statuts : ils posent `checkedInAt` et `checkedOutAt`, rendus dans la réponse (lot B7). Un pointage avant l'heure laisse la réservation `CONFIRMED` ; le worker la passe `IN_PROGRESS` à l'heure de début. La sortie ne clôt pas la réservation : la clôture vient du worker à l'heure de fin, ou de `PATCH .../complete`.

Blocage temporaire (`BookingHold.status`) : `ACTIVE` à la création ; `CONFIRMED` quand une réservation qui le cite est confirmée ; `RELEASED` par `PATCH /bookings/holds/{holdNumber}/release` ; `EXPIRED` à échéance, par le worker d'expiration (toutes les minutes) ou par `POST /bookings/holds/expire-due`.

### Échéance de paiement et expiration

Une réservation `PENDING_PAYMENT` reçoit une échéance de paiement (lot B1) :

| Règle | Réglage |
| --- | --- |
| 2 h après la création par défaut | `bookings.payment-due.default-minutes` |
| Au plus tard 30 min avant le début ; dans les 30 dernières minutes, échéance au début (règlement à l'arrivée) | `bookings.payment-due.before-start-minutes` |
| 24 h si le créneau est à plus de 3 jours | `bookings.payment-due.far-days`, `bookings.payment-due.far-minutes` |
| Échéance un samedi, un dimanche ou un jour férié : prochain jour ouvré à 10:00 | `bookings.payment-due.business-hour` |

À l'échéance, sans règlement, la réservation passe `EXPIRED` et l'équipe reçoit l'alerte en direct `BOOKING_EXPIRED` (module `BOOKING`). L'échéance est dans la réponse (`paymentDueAt`). Un courriel de rappel part au client 30 minutes avant (`bookings.payment-due.reminder-minutes`), une fois.

Liste d'attente (`BookingWaitlistStatus`) : `WAITING` à l'inscription ; `OFFERED` quand le worker trouve la plage libre (offre de 30 minutes, réglage `bokati.booking.waitlist.offer-ttl-minutes`, 5 minimum) ; `EXPIRED` à échéance de l'offre ; `CANCELLED` par `PATCH /bookings/waitlist/{id}/cancel` ; `BOOKED` quand l'offre est acceptée (la réservation est créée, `bookingNumber`).

## Parcours

### Vérifier une disponibilité et chiffrer

1. `POST /sni/api/v1/bookings/check-availability` ([détail](routes/BookingController.md#r-post-sni-api-v1-bookings-check-availability)) avec `resourceCode`, `startedAt`, `endedAt`, `quantity` (défaut 1), `paymentMode`, le titulaire (`memberId`, `customerId` ou `businessCode`, voir plus bas), et facultativement `bookingUnit` et `billingCompanyCode`.
2. Réponse toujours `200` (sauf ressource inconnue `404` et champs manquants `400`) : `available` vrai ou faux, `message`, et si disponible `bookingUnit`, `unitPrice`, `estimatedAmount`, `currency` (`XAF`), `remainingCapacity`. Quand `available` est faux, `message` contient la raison en français, à afficher telle quelle (par exemple « Délai minimal de réservation de la ressource non respecté »).
3. Pour abonnement ou pass, la vérification contrôle aussi qu'un droit utilisable existe ; pour le portefeuille, le solde n'est **pas** vérifié ici.
4. Si indisponible, la réponse porte `occupiedRanges` : les plages occupées qui chevauchent le créneau, chacune en entier (`startedAt`, `endedAt`), avec `reason` (`BOOKED` : réservations, `UNAVAILABLE` : fermeture, option, créneau fermé) et `bookingNumbers` (réservations en cause ; `null` sur le portail, qui ne montre pas les réservations des autres). Liste vide si le créneau est disponible. Afficher par exemple « Occupé de 08:00 à 16:00 ».
5. Proposer des alternatives : `POST /sni/api/v1/bookings/suggestions` avec le même corps. Rend jusqu'à 10 fenêtres libres de même durée, de la plus proche à la plus éloignée de l'heure demandée : d'abord à 4 heures près, puis sur toute la journée s'il y en a moins de cinq, puis sur les 7 jours suivants. `reason` : `nearby_available_slot`, `same_day_slot` ou `later_day_slot`. Liste vide seulement si rien n'est libre sur cette durée.
5. Le montant à afficher est `estimatedAmount` : c'est le calcul qui sera facturé. Le devis du module ressource (`/resource-pricing-rules/quote`) peut différer (voir [ressource](../ressource/README.md#règles-et-erreurs-à-refléter-dans-lécran)).

### Désigner le titulaire

Les mêmes champs servent à la vérification, au blocage, à la création et à la dérogation de quota :

1. Au plus **un** de `memberId`, `customerId`, `businessCode` (sinon `400` « Un seul titulaire à la fois : memberId, customerId ou businessCode »). Il doit exister (sinon `404`) ; un client supprimé est refusé (`400`).
2. À défaut, `email` puis `phone` sont cherchés parmi les membres, puis les clients, puis les entreprises.
3. À défaut encore, si `walkIn: true` ou un champ de contact est fourni, un **invité** est créé à la volée (code généré). Un invité ne peut pas payer par abonnement ou pass (`400`).
4. Rien de tout cela : `400` « Un titulaire est requis : memberId, customerId, businessCode, email ou phone ».
5. `contactName`, `contactEmail`, `contactPhone` remplacent les coordonnées de la fiche pour cette réservation.
6. Recherche unique du titulaire, un seul champ de saisie (lot B7) : `GET /bookings/lookup/owners?q=&types=&page=&size=` rend `{ data: [{ type, code, label, hint, active }], pageable }`. Selon `type`, envoyer `code` dans `memberId` (`MEMBER`), `customerId` (`CUSTOMER`) ou `businessCode` (`BUSINESS_ENTITY`). `types` restreint (`MEMBER,CUSTOMER`) ; 50 au plus par page.

### Créer une réservation (back-office)

1. Facultatif : poser un blocage pendant la saisie, voir [Blocage temporaire](#blocage-temporaire-hold).
2. `POST /sni/api/v1/bookings` ([détail](routes/BookingController.md#r-post-sni-api-v1-bookings)) avec `resourceCode`, `startedAt`, `endedAt`, `paymentMode` (obligatoires), le titulaire, `quantity`, et selon le besoin :
   - `idempotencyKey` (dans le **corps**, pas en en-tête) : une clé par intention ; un rejeu rend la réservation existante, ou `409` si la clé appartient à un autre titulaire ;
   - `holdNumber` : le blocage à convertir (même titulaire, ressource, plage et quantité exactes) ;
   - `confirmImmediately` (défaut `true`) : `false` laisse la réservation en `DRAFT` (sans effet en `DIRECT` ou avec approbation) ;
   - `sendEmail` (défaut `true`) : `false` supprime les courriels de cette transition (pas la notification dans l'application) ;
   - `bookingUnit` : impose l'unité facturée (le forfait compte alors une unité quelle que soit la durée, sauf l'heure) ; refusé si la ressource n'a pas de règle tarifaire active pour cette unité ;
   - `billingCompanyCode` : organisation pour le compte de laquelle le membre réserve (doit lui être rattachée), ou `NONE` ;
   - `participants`, `notes`, `metadataJson` (texte JSON libre) ;
   - `options` : `[{ "kind": "SERVICE" | "AMENITY", "code": "...", "quantity": 1 }]`, services du catalogue et équipements optionnels ajoutés dès la création, sur la même facture (lot B2b) ;
   - `caller` (lot B5, équipe) : rendez-vous pris au téléphone, `{ fullName, phone, email, company, callNote, leadNumber, createLead }`. Voir [Rendez-vous au téléphone](#rendez-vous-au-téléphone-lot-b5-19112026) ;
   - `priceAdjustment` (lot B4, équipe seulement) : `{ "discountRate": 10 }` ou `{ "newAmount": 40000 }`, avec `reasonCode` (liste `BOOKING_PRICE_ADJUSTMENT_REASON`) et `reasonText`. Voir [Ajuster le prix](#ajuster-le-prix-lot-b4-19112026) ;
   - `payOnSite: true` (lot B3) : rendez-vous sans paiement, réglé sur place ; paiement `DIRECT` seulement (`400 PAY_ON_SITE_DIRECT_ONLY`). Toujours permis à l'équipe ;
   - `channel` (équipe seulement) : `PHONE`, `DESK`, `EMAIL`, `WHATSAPP`. Sans lui, le serveur note `TEAM` (back-office) ou `PORTAL` (espace client). L'auteur (`createdBy`) est lu dans la session. Les deux sont dans la réponse.
3. Réponse `201` : la réservation. Selon le cas, `status` vaut :
   - `CONFIRMED` (cas courant : abonnement, pass, portefeuille, ou `DIRECT` saisi par l'équipe sur un début déjà passé) ;
   - `PENDING_PAYMENT` (`DIRECT`) : la facture est émise et envoyée, le créneau est retenu jusqu'à l'échéance ; orienter vers l'encaissement (modules `billing` et `payment`) ;
   - `PENDING_APPROVAL` : afficher « en attente d'approbation » ;
   - `IN_PROGRESS` ou `COMPLETED` : saisie par l'équipe d'une réservation déjà commencée ou terminée ;
   - `DRAFT` : si `confirmImmediately: false`.
4. Afficher `bookingNumber`, `paymentStatusLabel`, `seatsLabel` (« 2 places réservées sur 4 », « Espace entier · 3 participants (12 places) »), le QR (`checkInQrValue` dans une balise `<img>`), et le lien de visioconférence `virtualMeetingUrl` s'il existe.

Réservation par l'équipe : un compte `ROLE_SUPER_ADMIN`, `ROLE_ADMIN`, `ROLE_MANAGER` ou `ROLE_OPERATIONS_AGENT`, hors routes du portail, réserve **sans préavis** et peut saisir un début passé jusqu'à 31 jours en arrière (réglage `bokati.booking.staff.max-backdate-days`), sauf si la politique de la ressource l'interdit. La durée minimale et maximale s'appliquent toujours, l'approbation est sautée. Un début déjà passé confirme la réservation sur-le-champ, quel que soit le mode de règlement ; en `DIRECT`, elle reste « à régler » (`paymentStatus: PENDING`) jusqu'à l'encaissement. La pose d'option (`POST /bookings/holds`) suit la même règle : l'équipe pose une option sur un début passé ou hors préavis, le client non.

### Créer une série récurrente

1. `POST /sni/api/v1/bookings/recurring` avec `booking` (le même objet que la création, pour la première occurrence), `frequency` (`DAILY`, `WEEKLY`, `MONTHLY`), `intervalValue` (défaut 1), `occurrences` (2 à 60, défaut 2), `endDate` facultative (arrêt au-delà), `excludedDates` (dates `AAAA-MM-JJ` sautées).
2. Chaque occurrence passe toutes les règles de la création. Si une seule échoue, **aucune** n'est créée (une seule transaction) : afficher l'erreur et proposer d'exclure la date fautive.
3. Réponse `201` : la liste des réservations créées. `idempotencyKey` et `holdNumber` de l'objet `booking` sont ignorés pour les occurrences.

### Réserver plusieurs ressources pour un ou plusieurs membres

`POST /sni/api/v1/bookings/batches` : plusieurs lignes (ressource et créneau) pour un ou plusieurs membres, tout ou rien, **une seule facture par membre**. Détail complet, corps, erreurs et effets sur le paiement et l'annulation : [reservation-multi-ressources.md](reservation-multi-ressources.md). Chaque réservation porte `batchNumber` (`null` hors lot).

### Blocage temporaire (hold)

1. `POST /sni/api/v1/bookings/holds` avec `resourceCode`, titulaire, `startedAt`, `endedAt`, `quantity`, `ttlMinutes` (défaut 10, de 1 à 1 440), `idempotencyKey` facultatif. Le blocage retire aussitôt la capacité des créneaux. Plusieurs blocages sur un espace partagé (la capacité tranche), un seul sur une pièce entière. Un invité garde ses coordonnées : la réservation le retrouve par son courriel ou son téléphone.
2. Réponse `201` : `holdNumber`, `status: ACTIVE`, `expiresAt`. Afficher un compte à rebours.
3. Créer la réservation avec `holdNumber`. À la confirmation, le blocage passe `CONFIRMED` et la capacité n'est pas reprise une seconde fois.
4. Abandon : `PATCH /sni/api/v1/bookings/holds/{holdNumber}/release` rend la capacité.
5. Un blocage échu est expiré par le worker dans la minute et sa capacité rendue. Créer la réservation avec un blocage échu ne bloque plus : elle reprend la capacité directement si elle est encore libre. Libérer quand même un blocage abandonné, pour rendre la place tout de suite.

### Approuver ou refuser

1. Liste : `GET /sni/api/v1/bookings?status=PENDING_APPROVAL`.
2. `PATCH /sni/api/v1/bookings/{bookingNumber}/approve` avec `{ "reason": "...", "sendEmail": true }` (corps facultatif ; l'auteur est lu dans la session). `DIRECT` passe en `PENDING_PAYMENT` (facture émise) ; les autres modes sont confirmés (ressource, droit, portefeuille).
3. `PATCH /sni/api/v1/bookings/{bookingNumber}/reject` avec `{ "reasonCode": "INCOMPLETE_FILE", "reasonText": "...", "sendEmail": true }` (liste `BOOKING_REJECT_REASON` ; `reason` libre accepté) : statut `REJECTED`, `rejectionReason` et `rejectionReasonCode` dans la réponse, fonds du portefeuille rendus.

### Confirmer manuellement

`PATCH /sni/api/v1/bookings/{bookingNumber}/confirm` avec `{ "reason": "...", "sendEmail": true }` (facultatif). Accepté depuis `DRAFT` et `PENDING_PAYMENT`. Depuis `PENDING_PAYMENT` (règlement sur place), la réservation est confirmée mais reste « En attente de paiement » : seul un paiement reçu la marque « Payé » (lot B1).

### Accueil : arrivée, départ, démarrage, clôture

1. Liste du jour : `GET /sni/api/v1/bookings?startedFrom=2026-11-09T00:00:00&startedTo=2026-11-09T23:59:59&status=CONFIRMED`.
2. Arrivée : `PATCH /sni/api/v1/bookings/{bookingNumber}/check-in` avec `{ "note": "...", "sendEmail": false }` (facultatif), ou par le QR : `PATCH /sni/api/v1/bookings/check-in/qr/{checkInToken}`. Accepté en `CONFIRMED` ou `IN_PROGRESS`. La réservation passe `IN_PROGRESS` si l'heure de début est atteinte, sinon reste `CONFIRMED`.
3. Fenêtre de pointage : de 15 minutes avant à 15 minutes après le début (`CHECKIN_EARLY_WINDOW_MINUTES`, `CHECKIN_LATE_WINDOW_MINUTES`). L'équipe d'accueil (`ROLE_SUPER_ADMIN`, `ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_OPERATIONS_AGENT`) pointe hors fenêtre (lot B0). Un membre hors fenêtre reçoit `409` avec `reason: CHECKIN_OUTSIDE_WINDOW` et un message lisible, à afficher.
4. Arrivée anticipée : `PATCH /sni/api/v1/bookings/{bookingNumber}/early-check-in`, droit `BOOKING:CHECKIN`.
5. Démarrer sans pointage : `PATCH .../start` (`CONFIRMED` uniquement, même fenêtre que le pointage).
6. Départ : `PATCH .../check-out`, seulement en `IN_PROGRESS` (`409` sinon). Le statut ne change pas. Lot R5c : un départ au-delà de la tolérance de la politique (`overrunGraceMinutes`) facture le dépassement, arrondi aux créneaux ([vente](../ressource/fonctions/vente.md)). Lot R5d : proposer ensuite l'état des lieux de fin (`POST .../end-report`).
7. Clôturer : `PATCH .../complete` (`CONFIRMED` ou `IN_PROGRESS`, une fois la réservation commencée), consomme le droit d'abonnement ou de pass, crée une tâche de remise en état. Sinon le worker clôt à l'heure de fin.
8. Absence : `PATCH .../no-show` (`CONFIRMED` ou `IN_PROGRESS`, après la fenêtre d'arrivée, droit `BOOKING:CANCEL`). Le droit est consommé ; aucun remboursement ; frais de non-présentation de la politique (`noShowFee`, lot R5c). Sinon le worker la marque à l'heure de fin si personne n'a pointé ni démarré.

### Annuler

1. Afficher les règles de remboursement : la grille de la politique de la ressource (`refundRules`, « heures:pourcentage », par exemple `72:100,24:50,0:0`, module [ressource](../ressource/README.md)) ; sans grille, la grille générale `GET /sni/api/v1/bookings/cancellation-policy` (droit `BOOKING:READ`) : paliers `hoursBeforeStart` et `refundPercentage`, dans l'ordre `ruleOrder`.
2. `PATCH /sni/api/v1/bookings/{bookingNumber}/cancel` avec `{ "reasonCode": "CLIENT_REQUEST", "reasonText": "...", "sendEmail": true }` (facultatif). `reasonCode` vient de la liste `BOOKING_CANCEL_REASON` (`GET /bookings/reference`) ; `OTHER` exige `reasonText`. L'ancien `reason` (texte libre) reste accepté. La réponse porte `cancellationReason` (libellé et précision) et `cancellationReasonCode`.
3. Refusée (`409`) depuis `COMPLETED`, `NO_SHOW`, `REJECTED` et `EXPIRED`. Pour une réservation `CONFIRMED` ou `IN_PROGRESS`, la politique de la ressource s'applique **à tous, équipe comprise** (lot B2b) : `allowCancellation` doit être vrai (« La politique de la ressource n'autorise pas l'annulation ») et le début doit être à plus de `cancellationNoticeMinutes` (« Le délai d'annulation de la ressource est dépassé »). Sans condition : `DRAFT`, `PENDING_PAYMENT`, `PENDING_APPROVAL`, et une réservation au portefeuille dont les fonds sont encore bloqués (première minute).
4. Effets : créneaux rendus et droit libéré, blocage de portefeuille rendu, statut `CANCELLED`. Facture (lot B2) :
   - sans encaissement, elle est annulée et archivée ;
   - réservation encore en attente : tout le payé est rendu ;
   - réservation active encaissée : avoir selon le premier palier atteint de la grille, reste dû d'une facture payée en partie annulé, remboursement par le moyen de paiement d'origine (portefeuille compris) ;
   - suppléments et compléments facturés à part : même traitement ; la caution suit le registre des cautions.

   Ces écritures sont faites après coup : un échec n'empêche pas l'annulation, mais crée une tâche « Remboursement à traiter » (priorité haute, 24 h) et un événement sur la réservation.

### Modifier une réservation

1. **Aperçu du prix d'abord** (lot B2b) : `GET .../reschedule/preview?newStartedAt=&newEndedAt=` ou `GET .../change-resource/preview?newResourceCode=`. Réponse `{ bookingNumber, currentAmount, newAmount, difference, change, currency, invoiced, modificationCount, lastModifiedAt, lastModification }` ; `change` vaut `INCREASE`, `DECREASE` ou `SAME`. Montrer l'ancien prix, le nouveau et l'écart.
2. Déplacer : `PATCH .../reschedule` avec `newStartedAt`, `newEndedAt`, `note`, `sendEmail`, `acceptPriceChange`. Accepté en `CONFIRMED`, `PENDING_APPROVAL` et `PENDING_PAYMENT`. Mêmes contrôles qu'à la réservation (ressource réservable, alignement, politique, date passée sauf équipe si la politique l'admet) ; capacité contrôlée, même sur un espace partagé.
3. Changer de ressource : `PATCH .../change-resource` avec `newResourceCode`, `note`, `sendEmail`, `acceptPriceChange`. `CONFIRMED` uniquement, mêmes contrôles ; le droit d'abonnement doit couvrir la nouvelle ressource.
4. Prix : recalculé par le moteur unique, remises acquises et unité imposée gardées (lot B2).
   - **Plus cher** : refusé (`409`, `reason: PRICE_INCREASE_CONFIRMATION_REQUIRED`) tant que `acceptPriceChange: true` n'est pas envoyé. Demander confirmation, puis renvoyer. Si la réservation est facturée, un complément est émis et la réservation repasse « à régler » : proposer « Encaisser ».
   - **Moins cher** : sans confirmation ; avoir sur la facture d'origine si elle est émise.
   - Abonnement ou pass : le droit est rendu puis repris sur la nouvelle durée ou ressource ; pas de confirmation de prix, sauf couverture partielle.
   - L'historique porte `BOOKING_PRICE_ADJUSTED` (ancien et nouveau montant, pièce émise). Recharger la réservation et ses pièces après l'opération.
5. Marquage : la réponse porte `modificationCount`, `lastModifiedAt`, `lastModification` (« Report au … · +5000 XAF (pièce FAC-…) ») ; afficher un badge « Modifiée N fois ». L'équipe reçoit l'alerte en direct `BOOKING_MODIFIED` (module `BOOKING`, sévérité `WARNING` si un complément est à encaisser, `INFO` sinon) ; l'agent qui a créé la réservation la voit aussi dans ses notifications. Le client est averti par courriel (sauf `sendEmail: false`).
6. Transférer à un autre titulaire : `PATCH .../transfer` avec `newOwnerType`, `newOwnerCode`, coordonnées facultatives. Depuis `DRAFT`, `PENDING_PAYMENT`, `PENDING_APPROVAL` ou `CONFIRMED`. Un membre doit exister. Refusé pour une réservation couverte par un droit ou réglée par portefeuille : annuler et réserver pour le nouveau titulaire.
7. Participants (événements `BOOKING_PARTICIPANT_ADDED` et `BOOKING_PARTICIPANT_REMOVED`) : `POST .../participants` avec `memberCode`, `name`, `email`, `phone`, `role` (défaut `GUEST`, valeurs insensibles à la casse), sur une réservation vivante et dans la capacité de la ressource ; retrait : `DELETE .../participants/{participantId}` (statut `REMOVED`, la ligne reste).
8. Historique : `GET .../history` (statuts, auteurs, motifs) et `GET .../events` (journal détaillé).

### Suppléments, remises, lots (chantier ressources, lot R5c)

Détail : [ressource/fonctions/vente.md](../ressource/fonctions/vente.md).

1. Lignes de la réservation : `GET /bookings/{bookingNumber}/lines` (ressource, droit d'abonnement, services, options, participants, caution, dépassement, non-présentation, remise). Chaque ligne porte son élément à facturer (`billableNumber`).
2. Services du catalogue : `POST .../services` `{ code, quantity }` ; équipements optionnels : `POST .../amenities` `{ code, quantity }` ; participants supplémentaires : `POST .../extra-participants` `{ count, waive }` (`waive` : offerts par l'équipe).
3. Avant la facture, un supplément part avec elle ; après, sur une facture à lui. La caution de la ressource s'ajoute d'elle-même à la première facture.
4. Code promo : `POST .../promo-code` `{ code }` (une remise par réservation ; avant facture, le montant baisse ; après, un avoir).
5. Lot de ressources : `POST /resource-bundles/{code}/book` (droit `BOOKING:CREATE`) réserve toutes les ressources du lot sur le même créneau, une facture par membre, remise du lot appliquée.

### Exploitation (chantier ressources, lot R5d)

Détail : [ressource/fonctions/exploitation.md](../ressource/fonctions/exploitation.md).

1. Tâches de l'équipe : à la confirmation, une tâche par modèle de la ressource (préparer, remettre en état), datée, avec sa liste de vérification ; sans modèle, les deux tâches par défaut. Annulées avec la réservation. Liste : module [task](../task/README.md), filtre `sourceCode` = numéro de réservation.
2. Règlement d'usage : `GET /bookings/{bookingNumber}/usage-rules` (texte, version, acceptation à jour ou non) ; `POST .../usage-rules/accept` à l'accueil. Un règlement modifié se ré-accepte. L'acceptation ne bloque pas l'arrivée : l'écran la demande.
3. État des lieux de fin : `POST .../end-report` `{ condition: GOOD|DAMAGED, checklist, photoDocumentCodes, notes }`, lecture `GET .../end-report`. `DAMAGED` ouvre un incident sur la ressource.

### Liste d'attente

1. Inscrire : `POST /sni/api/v1/bookings/waitlist` avec `resourceCode`, `identityLookup` (**objet imbriqué** : `{ "memberId": "...", ... }`), `startedAt` et `endedAt` dans le futur, `quantity`, `paymentMode`. Réponse `201` avec `alternatives` (fenêtres libres proches).
2. Suivre : `GET /sni/api/v1/bookings/waitlist?status=&resourceCode=&ownerType=&ownerCode=&page=0&size=20` : **page** `{ data, pageable }` (lot B6), tous états sans `status`, la plus ancienne d'abord.
3. Offre (lot B6) : quand la place se libère vraiment (capacité, fermetures, politique), la plus ancienne inscription du créneau passe `OFFERED` pour 30 minutes (`expiresAt`) et son contact reçoit un courriel (`notifiedAt`). Une seule offre à la fois par créneau ; refusée ou échue, la place passe à la suivante.
4. Accepter pour le client : `POST /sni/api/v1/bookings/waitlist/{id}/accept` crée la réservation (mêmes règles qu'une réservation) ; réponse avec `status: BOOKED` et `bookingNumber`. Offre échue : `409 WAITLIST_OFFER_EXPIRED` ; pas d'offre : `409 WAITLIST_OFFER_NOT_OPEN`.
5. Refuser : `POST .../{id}/decline`. Annuler l'inscription : `PATCH .../{id}/cancel`.
6. Une inscription dont le créneau est passé expire d'elle-même.

### Quotas et approbation par public

1. Politiques : `POST /sni/api/v1/bookings/policies` avec `audienceType` (obligatoire), `resourceTypeCode`, `resourceGroupCode`, `approvalRequired`, plafonds (`maxActiveBookings`, `maxBookingsPerDay`/`Week`/`Month`, `maxNoShowsPerMonth`), `minBookingNoticeMinutes`, `maxBookingDurationMinutes`, `active`. Liste : `GET /sni/api/v1/bookings/policies`. La politique retenue pour une réservation est la plus précise (type puis groupe renseignés), puis la plus récente.
2. Dérogations : `POST /sni/api/v1/bookings/quota-overrides` avec le titulaire (champs à plat), `resourceCode` facultatif, quotas supplémentaires, `reason`, `approvedBy`, `validFrom` et `validUntil` (`Instant` avec fuseau). Liste : `GET /sni/api/v1/bookings/quota-overrides`.
3. Modifier (lot B7) : `PUT /bookings/policies/{policyNumber}` (même corps que la création), `PATCH .../activate`, `PATCH .../deactivate` ; `PUT /bookings/quota-overrides/{overrideNumber}` avec `{ resourceCode, extraActiveBookings, extraBookingsPerDay, extraBookingsPerWeek, extraBookingsPerMonth, reason, validFrom, validUntil }` (le titulaire ne change pas), `PATCH .../activate`, `PATCH .../deactivate`. Listes filtrables par `?active=true|false`. L'auteur d'une dérogation est lu dans la session (`approvedBy` ignoré). Rien ne se supprime.

### Pages publiques à jeton

Ces routes sont des **pages HTML** rendues par le serveur (sauf l'auto-pointage, en JSON). Le frontend n'a en général qu'à y renvoyer par un lien.

1. Confirmation : `GET /sni/api/v1/public/bookings/{bookingNumber}?token=<checkInToken>` (page HTML avec bouton d'auto-pointage) et `GET /sni/api/v1/public/bookings/{bookingNumber}/confirmation.pdf?token=<checkInToken>` (PDF). Mauvais jeton : `404`. Ces liens figurent dans le courriel de confirmation.
2. Auto-pointage : `POST /sni/api/v1/public/bookings/check-in/self?token=<checkInToken>&lat=...&lng=...` (paramètres de requête, pas de corps). Réponse JSON `{ "success": true, "bookingNumber", "resourceName", "status" }` ou `{ "success": false, "error", "message" }` avec `error` = `location_required` (`400`, position exigée quand un point de géolocalisation est configuré), `location_invalid` (`403`, à plus de 200 m), `time_invalid` (`403`, hors fenêtre de pointage, message en français à afficher), `checkin_failed` (`400`). Jeton inconnu : `404` au format d'erreur standard.
3. Borne de scan à l'accueil : le QR de la réservation ouvre `GET /sni/api/v1/public/bookings/check-in/scan/{checkInToken}`. Un poste non autorisé voit une page qui demande la clé du poste (`POST .../check-in/scanner-verify` avec `checkInToken` et `adminCode` en formulaire) ; un poste autorisé voit une page de confirmation qui envoie `POST .../check-in/scan` (`checkInToken` en formulaire). L'autorisation est un cookie `bokati_checkin_grant` (`HttpOnly`, `SameSite=Strict`, 30 jours), posé aussi par `GET`/`POST .../check-in/scanner-setup` (`setupKey` en formulaire). La clé est `CHECKIN_SCANNER_KEY` côté serveur. Seuls les envois (`POST`) vers `.../check-in/scanner*` sont limités à 3 par 10 minutes ; la page d'installation (`GET`) ne l'est plus (lot B0).
4. Satisfaction : `GET /sni/api/v1/public/bookings/csat/{bookingNumber}?score=1..5&token=<checkInToken>`, lien du courriel de clôture ; page de remerciement. Sans jeton valable, la note n'est pas enregistrée (lot B0).

## Règles et erreurs à refléter dans l'écran

Depuis le lot B8, les refus métier sont des `409` `CONFLICT` dont le message est en français, sans préfixe, à afficher tel quel. Pour une logique d'écran, tester `reason` quand il est fourni (`CHECKIN_OUTSIDE_WINDOW`, `PRICE_INCREASE_CONFIRMATION_REQUIRED`...), jamais le texte. Principaux messages :

| Message | Cause | Quoi afficher |
| --- | --- | --- |
| « La ressource n'est pas ouverte à la réservation » / « L'état de la ressource ne permet pas de la réserver » | Ressource inactive, non réservable ou pas `ACTIVE` | Ressource indisponible |
| « Le début demandé est déjà passé » | Début passé (hors équipe) | Choisir un horaire futur |
| « Délai minimal de réservation de la ressource non respecté » / « ... pour ce profil » | Préavis de la ressource ou du public | Indiquer le préavis |
| « Durée inférieure au minimum de la ressource » / « Durée supérieure au maximum ... » / « Durée maximale de réservation dépassée pour ce profil » | Durée hors bornes | Bornes de la politique |
| « Quantité supérieure aux places réservables de la ressource » / « Trop de participants pour la capacité de la ressource » | Trop de places ou de participants | Capacité restante |
| « La ressource est déjà réservée sur ce créneau » | Pièce entière déjà prise (réservation confirmée ou en attente) | Proposer les suggestions |
| Plage pleine, fermée ou non couverte par les créneaux | Créneaux pleins, fermés ou inexistants (mois suivant non ouvert) | Plage indisponible |
| « Nombre maximal de réservations en cours atteint », « ... par jour », « ... par semaine », « ... par mois » | Quota du public atteint | Proposer une dérogation |
| « Trop d'absences ce mois-ci : nouvelles réservations bloquées » | Quota d'absences (un ticket support est ouvert) | Contacter le support |
| « Action impossible : la réservation est annulée, il faut qu'elle soit confirmée », « Seule une réservation ... » | Action non permise dans le statut actuel | Masquer le bouton |
| « La politique de la ressource n'autorise pas l'annulation » / « Le délai d'annulation de la ressource est dépassé » | Règles d'annulation | Expliquer, proposer l'absence ou le support |
| « La pré-réservation n'est plus active » / « ... appartient à un autre titulaire » / « ... porte sur un autre créneau » | Blocage libéré ou différent | Refaire un blocage |
| « Cette clé d'idempotence est déjà utilisée par une autre réservation » | Clé rejouée pour un autre titulaire | Générer une nouvelle clé |
| `reason: PRICE_INCREASE_CONFIRMATION_REQUIRED` | Report ou changement plus cher sans `acceptPriceChange` | Demander confirmation |
| `reason: CHECKIN_OUTSIDE_WINDOW` | Pointage d'un membre hors fenêtre | Afficher le message (heures permises) |

Autres :

| Situation | Statut | Message |
| --- | --- | --- |
| Plage non alignée, secondes | `400` | « La plage doit être alignée sur des créneaux de <n> minutes » / « Le début et la fin se donnent à la minute, sans secondes » |
| Fin avant début | `400` | « La fin doit être après le début » |
| Saisie de l'équipe trop ancienne | `400` | « Une réservation saisie après coup ne remonte pas au-delà de 31 jours. » |
| Solde du portefeuille insuffisant | `400` | « Solde du portefeuille insuffisant pour cette réservation · <montant> XAF requis » (ou « de l'organisation ») |
| Aucun abonnement ou pass utilisable | `404` | « Aucun abonnement actif pour ce titulaire » / « Aucun pass utilisable pour cette ressource » / « Aucun abonnement actif ne donne droit à cette ressource (...) » |
| Unité imposée sans grille | `400` | « La ressource <code> n'a aucune grille tarifaire active en <unité> ... » |
| Option à la création d'un autre type | `400` | `kind` doit valoir `SERVICE` ou `AMENITY` |
| Photo qui n'est pas un document de la GED (ou d'un autre membre) | `400` | Choisir un document existant |
| Réservation, blocage, participant inconnus | `404` | « Réservation introuvable : <n> » (et variantes) |

Concurrence (lot B1) : une réservation `PENDING_PAYMENT` ou `PENDING_APPROVAL` **retient son créneau** dès sa création et compte dans les conflits et dans le quota de réservations actives. La confirmation ne reprend pas le créneau et ne recontrôle pas la politique : un paiement arrivé tard confirme sans échec. Un `DRAFT` ne retient rien.

Quotas du public : `maxActiveBookings` compte les réservations `CONFIRMED`, `IN_PROGRESS`, `PENDING_APPROVAL` et `PENDING_PAYMENT` du titulaire ; les quotas jour, semaine (lundi à dimanche) et mois comptent celles qui commencent dans la période, hors `CANCELLED`, `REJECTED` et `EXPIRED`.

## À prévoir côté frontend

- Écrans : recherche de disponibilité avec devis et suggestions ; formulaire de réservation (choix du titulaire, mode de règlement, organisation, participants, unité imposée) ; liste filtrable (texte, titulaire, ressource, statut, période) ; fiche réservation (statut, paiement, lignes, participants, QR, historique, événements, actions) ; accueil du jour (arrivée, départ, absence) ; file d'approbation ; liste d'attente ; politiques d'audience et dérogations ; règles d'annulation (lecture seule).
- Droits, à comparer à la liste `roles` :
  - `BOOKING:READ` : listes, fiche, historique, événements, politiques, dérogations, liste d'attente, indicateurs (`GET /bookings/metrics/overview`), règles d'annulation, aperçus de report et de changement de ressource ;
  - `BOOKING:CREATE` : vérification, suggestions, création, série, blocage, participants, liste d'attente, politiques, dérogations, rappel, et les déclenchements manuels (`holds/expire-due`, `notifications/dispatch-due`, `waitlist/promote-available`, `waitlist/expire-offers`, `repair`) ;
  - `BOOKING:UPDATE` : confirmer, approuver, démarrer, clôturer, déplacer, changer de ressource, transférer, retirer un participant, libérer un blocage ;
  - `BOOKING:CANCEL` : annuler, refuser, absence, annuler une entrée de liste d'attente ;
  - `BOOKING:CHECKIN` : arrivée, arrivée anticipée, arrivée par QR, départ.
- Boutons selon le statut : Confirmer (`DRAFT`, `PENDING_PAYMENT`, à protéger) ; Approuver et Refuser (`PENDING_APPROVAL`) ; Arrivée, Démarrer (`CONFIRMED`), Arrivée et Départ (`IN_PROGRESS`) ; Clôturer et Absence (`CONFIRMED`, `IN_PROGRESS`) ; Déplacer (`CONFIRMED`, `PENDING_APPROVAL`, `PENDING_PAYMENT`) ; Changer de ressource (`CONFIRMED`) ; Transférer (`DRAFT`, `PENDING_PAYMENT`, `PENDING_APPROVAL`, `CONFIRMED`, hors droit et portefeuille) ; Annuler (`DRAFT`, `PENDING_PAYMENT`, `PENDING_APPROVAL`, `CONFIRMED`, `IN_PROGRESS`) ; Encaisser (`paymentStatus: PENDING` hors brouillon).
- Tri de `GET /bookings` (lot B7) : `sort=startedAt,desc` ; champs `startedAt`, `endedAt`, `createdAt`, `totalAmount`, `status`, `bookingNumber`, `resourceCode`, `contactName`. Sans `sort` : pertinence si `query`, puis début décroissant. Champ inconnu : `400`. Même tri au portail. `GET /bookings/search?query=` rend au plus 20 résultats pour l'autocomplétion.
- Listes (`GET /bookings`, `/bookings/search`, portail) : chaque ligne a tous les champs, sauf `checkInQrValue` et `invoiceNumber`, nuls : les lire sur la fiche `GET /bookings/{numéro}`.
- Réponse complète (lot B7) : `createdBy`, `channel`, `holdNumber`, `recurrenceGroupNumber`, `paymentDueAt`, `paidAt`, `checkedInAt`, `checkedOutAt`, `approvedBy`, `approvedAt`, `rejectedBy`, `rejectedAt`, `rejectionReason`, `rejectionReasonCode`, `cancellationReason`, `cancellationReasonCode`, `invoiceNumber` (fiche seulement), `extrasAmount` (services, équipements, participants, caution, dépassement, non-présentation), `grandTotal` (`totalAmount` + `extrasAmount` : le montant à afficher comme total). Depuis B2b : `modificationCount`, `lastModifiedAt`, `lastModification`.
- Listes déroulantes de l'écran : `GET /bookings/reference` en un appel (valeurs fixes, motifs d'annulation et de refus, grille d'annulation générale), voir [listes-de-choix](../listes-de-choix/README.md).
- Ne pas envoyer de fuseau dans les dates de plage ; arrondir les heures saisies sur la durée de créneau de la ressource (`slotDurationMinutes` de la ressource).
- Rappels automatiques (lot B6) : la veille et 2 h avant le début (`bookings.reminders.offsets-minutes`), par courriel, pour toute réservation confirmée avec un courriel de contact ; rien à déclencher. Un rappel en plus : `POST /bookings/{bookingNumber}/notifications/reminder?minutesBefore=30` (`400 RAPPEL_SANS_COURRIEL` sans courriel, `409` si la réservation n'est pas confirmée).
- Afficher `checkInQrValue` comme image ; le contenu du QR est l'adresse de la page de scan publique, utilisable par la borne d'accueil.
- Alerte en direct `BOOKING_MODIFIED` (module `BOOKING`) sur le canal des alertes admin à chaque report ou changement de ressource ; les autres notifications dans l'application passent par le module `notification`.

## Effets automatiques

- **Cycle de vie** (`BookingNoShowWorker`, toutes les 60 s, `bokati.booking.lifecycle.worker-delay-ms`), par lots de 200, une réservation à la fois : passe en `IN_PROGRESS` les réservations pointées d'avance dont l'heure est venue ; marque `NO_SHOW` celles dont la fin est passée sans pointage ni démarrage ; clôt `COMPLETED` celles dont la fin est passée après pointage ou démarrage. Ces transitions automatiques n'envoient **aucun courriel** (donc pas de lien de satisfaction).
- **Règlement des portefeuilles** (`BookingWalletSettlementWorker`, toutes les 15 s) : un blocage de plus d'une minute (`bokati.booking.wallet.settlement-delay-minutes`) devient un débit sur la facture de la réservation ; il est rendu si la réservation est annulée ou refusée, ou si la facture est déjà soldée. Un blocage orphelin expire au bout d'une heure de grâce (`WalletHoldExpiryWorker`, module `payment`).
- **Expiration** (`BookingExpiryWorker`, toutes les minutes, `bokati.booking.expiry.worker-delay-ms`, lot B1) : expire les blocages échus (capacité rendue) et passe `EXPIRED` les réservations non réglées à l'échéance (créneau rendu, facture annulée, tâches annulées, client prévenu).
- **Liste d'attente** (`BookingWaitlistWorker`, toutes les 5 minutes) : expire les offres échues et les inscriptions passées, puis propose la place libérée à la plus ancienne inscription du créneau, avec un courriel.
- **Rappels** (`BookingReminderWorker`, toutes les 5 minutes) : prévoit et envoie les rappels (la veille, 2 h avant) ; un rappel d'une réservation qui n'est plus active est annulé.
- **Confirmation après paiement** (module `payment`) : quand l'intention de paiement d'une réservation `DRAFT` ou `PENDING_PAYMENT` est entièrement réglée, la réservation est confirmée et marquée réglée, courriel compris ; l'historique note l'acteur `SYSTEM` et le motif « Confirmation automatique suite au paiement … ». Un paiement retourné défait la confirmation : créneau gardé avec une nouvelle échéance, règlement effacé, blocage de portefeuille rendu.
- **Annulations système** : facture de la réservation annulée, paiement remboursé, ou annonce de paiement en espèces expirée sans encaissement (réservation encore en attente, sa facture est annulée avec elle) : la réservation passe `CANCELLED`, sans contrôle de politique.
- **Remboursement en échec** : tâche « Remboursement à traiter » (priorité haute, 24 h) et événement sur la réservation (lot B2).
- **Report ou changement de ressource** : alerte `BOOKING_MODIFIED` à l'équipe, notification à l'agent qui a créé la réservation (lot B2b).
- **Courriels** : chaque transition écrit un événement publié dans la file d'événements (traitée toutes les 10 s, par 25) ; si `sendEmail` n'est pas `false`, un courriel part au contact pour la création, la confirmation (avec QR, lien de la page publique et du PDF), l'approbation, le refus, le démarrage, l'arrivée, la sortie, la clôture (avec liens de satisfaction 1 à 5, porteurs du jeton), l'annulation, l'absence, le déplacement, le transfert et le changement de ressource. La facture est envoyée séparément à son émission.
- **Notification dans l'application** au contact pour chaque transition, même avec `sendEmail: false`.
- **Tâches** (module `task`) : à la confirmation, les tâches de la ressource (« Préparer la ressource », « Nettoyer la ressource » par défaut, voir Exploitation), annulées avec la réservation ou à son expiration.
- **Ticket support** automatique quand un titulaire dépasse son quota mensuel d'absences.
- **Facture** : émise et envoyée à la création pour un `DIRECT` en attente, sinon à la confirmation. Non vérifié : émission d'une facture à montant nul pour les réservations couvertes par un droit.

## Écarts relevés

Les écarts relevés dans la première version de cette page sont réglés (lots B0 à B8). Il n'en reste aucun d'ouvert ; la suite du plan ajoute des fonctions (lots B9a à B9g).

Plan complet et suivi : [plan booking](../../backend/booking/README.md).

## Reports, écart de prix, options (lot B2b, 18/11/2026)

- Avant un report ou un changement de ressource, appeler l'aperçu (`GET /bookings/{numéro}/reschedule/preview` ou `/change-resource/preview`) et montrer l'ancien prix, le nouveau et l'écart.
- Plus cher (`change: INCREASE`) : demander confirmation, puis envoyer `acceptPriceChange: true`. Sans elle, l'API répond 409 `PRICE_INCREASE_CONFIRMATION_REQUIRED`. Si la réservation est facturée, le complément est à encaisser (bouton « Encaisser »).
- Moins cher : pas de confirmation, un avoir suit.
- Réponse de la réservation : `modificationCount`, `lastModifiedAt`, `lastModification` ; afficher un badge « Modifiée N fois ».
- Équipe : écouter l'alerte `BOOKING_MODIFIED` (module `BOOKING`) sur le canal des alertes admin ; l'auteur de la réservation la voit aussi dans ses notifications.
- L'annulation suit la politique de la ressource pour tous, équipe comprise.
- Création : `options: [{ kind: "SERVICE" | "AMENITY", code, quantity }]` et `channel` (PHONE, DESK, EMAIL, WHATSAPP) pour l'équipe.

Détail : [lot B2b](../../backend/booking/lot-b2b-reports-options.md).

## Messages et fuseau (lot B8, 18/11/2026)

- Les messages d'erreur, l'historique et les événements d'une réservation sont en français, sans le préfixe « Could not process booking: ». Afficher `message` tel quel ; pour une logique d'écran, tester le code d'erreur, jamais le texte.
- Les heures des créneaux sont en heure locale de l'application (Africa/Brazzaville), quel que soit le fuseau du serveur.

Détail : [lot B8](../../backend/booking/lot-b8-messages-fuseau.md).

## Listes, recherche, réponse complète (lot B7, 19/11/2026)

- `GET /bookings/reference` : valeurs fixes, motifs d'annulation et de refus, grille d'annulation générale, en un appel.
- Motifs en `reasonCode` + `reasonText` pour annuler et refuser.
- `GET /bookings/lookup/owners` : un seul champ pour trouver un membre, un client ou une entreprise.
- Réponse complète (pointage, motifs, facture, échéance, auteur, canal, `extrasAmount`, `grandTotal`) ; listes sans QR ni facture.
- `sort` appliqué ; politiques et dérogations modifiables et désactivables ; `POST /bookings/repair` répare vraiment (`{ inspected, repaired, failed }`).

Détail : [lot B7](../../backend/booking/lot-b7-listes-recherche-reponse.md).

## Rendez-vous réglé sur place (lot B3, 19/11/2026)

- Création avec `payOnSite: true` (paiement `DIRECT`) : créneau retenu jusqu'à l'échéance, facture émise pour l'encaissement, rappel au client avant l'échéance, puis `EXPIRED` et alerte `BOOKING_EXPIRED` à l'équipe. La réponse porte `payOnSite` : afficher un badge « À régler sur place ».
- Portail : `GET /client/bookings/pay-on-site` → `{ allowed, source, reason }` ; ne proposer « Régler sur place » que si `allowed`. Sinon `409 PAY_ON_SITE_NOT_ALLOWED`.
- Équipe : écran « Règlement sur place » par client : `GET /bookings/pay-on-site/clients`, `GET|PUT /bookings/pay-on-site/clients/{ownerType}/{ownerCode}` (`{ enabled, reason }`). Une option retirée d'office après trop d'absences a `revokedAutomatically: true`.
- Accueil : `GET /bookings/pay-on-site/due?date=` liste les rendez-vous réglés sur place à encaisser ce jour-là ; encaisser à la caisse sur la facture (`invoiceNumber` de la fiche).
- Réglages : `bookings.pay-on-site.portal-enabled` (coupé), `bookings.pay-on-site.max-no-shows` (2), `bookings.pay-on-site.no-show-window-days` (90), `bookings.payment-due.reminder-minutes` (30).

Détail : [lot B3](../../backend/booking/lot-b3-regle-sur-place.md).

## Ajuster le prix (lot B4, 19/11/2026)

- À la création (`priceAdjustment` dans le corps) ou ensuite : `POST /bookings/{numéro}/price-adjustment` avec `{ "discountRate": 10, "reasonCode": "LOYAL_CLIENT" }` ou `{ "newAmount": 40000, "reasonCode": "OTHER", "reasonText": "..." }`. Un seul des deux montants ; seulement une baisse ; motif obligatoire (liste `BOOKING_PRICE_ADJUSTMENT_REASON`, dans `GET /bookings/reference`).
- Avec le droit `BOOKING:PRICE_ADJUST` et jusqu'à `bookings.price-adjustment.max-rate` (20 %) : appliqué tout de suite (`priceAdjustmentStatus: APPLIED`), facture ou avoir à jour.
- Au-delà, ou sans le droit : demande d'approbation (centre `BOOKING`, type `PRICE_ADJUSTMENT`, `subjectRef` = numéro de réservation ; `priceAdjustmentStatus: PENDING`, `priceAdjustmentApprovalNumber`). À la création, la réservation attend en `PENDING_APPROVAL`. Accord : remise appliquée et la réservation continue ; refus : prix calculé gardé (`REJECTED`) et la réservation continue.
- Afficher sur la fiche : prix d'origine barré (`originalAmount`), remise (`priceAdjustmentAmount`, `priceAdjustmentRate`), motif, auteur, état. Masquer le bouton si un ajustement existe déjà (`409`).
- Écran d'approbation : bouton « Ouvrir la réservation » quand `subjectType === "BOOKING"` (`GET /bookings/{subjectRef}`) ; `detailsJson` : `amountBefore`, `amountAfter`, `discount`, `rate`, `reason`, `resourceCode`, `contactName`, `startedAt`.
- Tarif préférentiel par client ou segment : règles tarifaires à audience (lot B10, module ressource).

Détail : [lot B4](../../backend/booking/lot-b4-ajustement-prix.md).

## Rendez-vous au téléphone (lot B5, 19/11/2026)

- Formulaire « Prise d'appel » : `POST /bookings` avec `caller: { fullName, phone, email, company, callNote, leadNumber?, createLead? }` en plus du reste. Sans `channel`, le canal devient `PHONE`.
- Appelant inconnu (ni `memberId`, ni `customerId`, ni `businessCode`) : un prospect du CRM est créé (source `PHONE`), relié à la réservation (`leadNumber` dans la réponse), avec une activité « Appel » portant la note. `createLead: false` l'évite ; `leadNumber` relie un prospect existant (le chercher dans le module `crm`).
- La réponse porte `callNote` et `leadNumber` : afficher la note sur la fiche et un lien vers le prospect.
- À l'arrivée ou au paiement, le prospect est converti tout seul (événement `BOOKING_LEAD_CONVERTED` dans `GET .../events`).
- Confirmation au client : courriel au contact (`contactEmail`) comme toute réservation.

Détail : [lot B5](../../backend/booking/lot-b5-telephone.md).

## Rappels, courriels en attente, liste d'attente (lot B6, 19/11/2026)

- Rappels automatiques par courriel la veille et 2 h avant (réglages `bookings.reminders.enabled`, `bookings.reminders.offsets-minutes`) ; plus aucun rappel pour une réservation annulée.
- Réservation en attente de paiement ou d'approbation : courriel au contact dès la création, avec l'échéance de paiement.
- Liste d'attente : voir [Liste d'attente](#liste-dattente) ; portail : [portal/parcours-reservations.md](../portal/parcours-reservations.md#liste-dattente).

Détail : [lot B6](../../backend/booking/lot-b6-notifications-attente.md).
