# booking : fonctionnement

> Rédigé d'après le code. Routes exactes, droits et champs JSON : [routes.md](routes.md) et `routes/`. Règles communes : [README racine](../README.md).

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
| Historique de statut | aucun | `GET .../history` : `fromStatus`, `toStatus`, `changedBy`, `reason`, `changedAt`. |
| Événement | `eventNumber` | `GET .../events` : journal fin (ressource réservée, droit consommé, courriel envoyé, pointage...). |
| Entrée de liste d'attente | `id` numérique ⚠ int64 | Demande sur une plage prise, avec offre à durée limitée. |
| Politique d'audience | `policyNumber` | Quotas et approbation par type de titulaire, type et groupe de ressource. |
| Dérogation de quota | `overrideNumber` | Quotas supplémentaires pour un titulaire, sur une période. |
| Jeton de pointage | `checkInToken` (`XXXX-XXXX`) | Sert à la fois de clé des pages publiques (confirmation, PDF) et de code de pointage. `checkInQrValue` est une **image** (data URI PNG) du QR qui pointe vers la page de scan. |

Identifiants numériques à garder en texte (lecture JSON sans perte, voir [README racine](../README.md#identifiants--attention-aux-grands-nombres)) : `participantId` dans `DELETE .../participants/{participantId}`, `id` d'une entrée de liste d'attente dans `PATCH /bookings/waitlist/{id}/cancel`, `id` des règles d'annulation.

### Dates et heures

`startedAt`, `endedAt`, `newStartedAt`, les filtres `startedFrom`/`startedTo` sont des `LocalDateTime` : date-heure **sans fuseau**, heure murale de l'espace (`2026-11-09T09:00:00`). Le générateur les décrit « date-heure ISO 8601 », mais il ne faut pas y ajouter `Z` ni de décalage. Le serveur les compare à son heure locale. Les horodatages techniques (`confirmedAt`, `cancelledAt`, `createdAt`, `changedAt`...) sont en revanche des `Instant` UTC. Les plages doivent tomber sur les créneaux de la ressource (multiples de `slotDurationMinutes`, sans secondes).

### Modes de règlement (`paymentMode`)

| Mode | Payant | Effet |
| --- | --- | --- |
| `DIRECT` | oui | Attend un paiement venu de l'extérieur (espèces, mobile money...) : la réservation reste `PENDING_PAYMENT` et sa facture est émise. Confirmée automatiquement quand l'intention de paiement est soldée. |
| `WALLET` | oui | Les fonds sont **bloqués** sur le portefeuille du titulaire (ou de l'organisation choisie) au moment de la confirmation : la réservation est confirmée tout de suite. Le débit réel suit une minute plus tard. Solde insuffisant : la création échoue. |
| `SUBSCRIPTION` | non | Couverte par un droit d'un abonnement actif du titulaire (ou d'une place sur l'abonnement d'une organisation). Le droit est réservé à la confirmation, consommé à la clôture ou à l'absence, rendu à l'annulation. Si le droit est épuisé mais que le plan autorise le dépassement, la réservation passe. |
| `PASS` | non | Idem avec un droit porté par un pass (du titulaire ou d'une organisation). |

`paymentStatus` et `paymentStatusLabel` sont calculés par le serveur : `COVERED` (« Couvert ») pour abonnement et pass, `CANCELLED` (« Annulé ») pour une réservation annulée ou refusée, sinon `PAID` (« Payé ») si la réservation a été confirmée, `PENDING` (« En attente de paiement ») sinon. Afficher le libellé tel quel.

## Statuts et cycle de vie

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `DRAFT` | Créée, non confirmée | `POST /bookings` avec `confirmImmediately: false` (mode non `DIRECT`, sans approbation) ; approbation d'une réservation non `DIRECT` (transitoire) | `CONFIRMED` (`PATCH .../confirm`), `CANCELLED` |
| `PENDING_APPROVAL` | Attend l'accord du back-office | Création quand la politique d'audience exige une approbation | `CONFIRMED` ou `PENDING_PAYMENT` (`PATCH .../approve`), `REJECTED` (`PATCH .../reject`), `CANCELLED` |
| `PENDING_PAYMENT` | Attend le paiement (`DIRECT`) | Création en `DIRECT` ; approbation d'une réservation `DIRECT` | `CONFIRMED` (paiement soldé, ou `PATCH .../confirm`), `CANCELLED` (manuel, annonce d'espèces expirée, facture annulée) |
| `CONFIRMED` | Ressource et droit réservés, fonds acquis ou bloqués | Confirmation (auto à la création, manuelle, après paiement, après approbation) | `IN_PROGRESS`, `COMPLETED`, `NO_SHOW`, `CANCELLED` |
| `IN_PROGRESS` | En cours d'occupation | `PATCH .../start` ; pointage à l'heure ou après ; worker (pointé d'avance, heure atteinte) ; saisie après coup par l'équipe | `COMPLETED`, `NO_SHOW`, `CANCELLED` |
| `COMPLETED` | Terminée | `PATCH .../complete` ; worker : fin passée avec pointage ou démarrage | `CANCELLED` seulement par le système (remboursement ou annulation de facture) |
| `NO_SHOW` | Absence | `PATCH .../no-show` ; worker : fin passée sans pointage ni démarrage | `CANCELLED` (manuel, sous les règles d'annulation) |
| `REJECTED` | Refusée à l'approbation | `PATCH .../reject` | `CANCELLED` |
| `CANCELLED` | Annulée | `PATCH .../cancel` ; annulation système | aucun (rappeler `cancel` ne fait rien) |
| `EXPIRED` | Prévu par l'énumération | Jamais atteint : rien ne le pose | aucun |

Le pointage (check-in) et la sortie (check-out) ne sont pas des statuts : ils posent `checkedInAt` et `checkedOutAt`, que la réponse **n'expose pas** (les retrouver dans `GET .../events`, événements `BOOKING_CHECKED_IN` et `BOOKING_CHECKED_OUT`). Un pointage avant l'heure laisse la réservation `CONFIRMED` ; le worker la passe `IN_PROGRESS` à l'heure de début. La sortie ne clôt pas la réservation : la clôture vient du worker à l'heure de fin, ou de `PATCH .../complete`.

Blocage temporaire (`BookingHold.status`) : `ACTIVE` à la création ; `CONFIRMED` quand une réservation qui le cite est confirmée ; `RELEASED` par `PATCH /bookings/holds/{holdNumber}/release` ; `EXPIRED` uniquement par `POST /bookings/holds/expire-due` (aucun worker ne l'appelle).

Liste d'attente (`BookingWaitlistStatus`) : `WAITING` à l'inscription ; `OFFERED` quand le worker trouve la plage libre (offre de 30 minutes, réglage `bokati.booking.waitlist.offer-ttl-minutes`, 5 minimum) ; `EXPIRED` à échéance de l'offre ; `CANCELLED` par `PATCH /bookings/waitlist/{id}/cancel` ; `BOOKED` jamais atteint.

## Parcours

### Vérifier une disponibilité et chiffrer

1. `POST /sni/api/v1/bookings/check-availability` ([détail](routes/BookingController.md#r-post-sni-api-v1-bookings-check-availability)) avec `resourceCode`, `startedAt`, `endedAt`, `quantity` (défaut 1), `paymentMode`, le titulaire (`memberId`, `customerId` ou `businessCode`, voir plus bas), et facultativement `bookingUnit` et `billingCompanyCode`.
2. Réponse toujours `200` (sauf ressource inconnue `404` et champs manquants `400`) : `available` vrai ou faux, `message`, et si disponible `bookingUnit`, `unitPrice`, `estimatedAmount`, `currency` (`XAF`), `remainingCapacity`. Quand `available` est faux, `message` contient la raison (souvent un message technique en anglais, par exemple `Could not process booking: minimum booking notice is not respected`, ou `Creneau non disponible pour les horaires demandes`).
3. Pour abonnement ou pass, la vérification contrôle aussi qu'un droit utilisable existe ; pour le portefeuille, le solde n'est **pas** vérifié ici.
4. Si indisponible, proposer des alternatives : `POST /sni/api/v1/bookings/suggestions` avec le même corps. Rend jusqu'à 10 fenêtres libres de même durée dans les 4 heures avant et après, triées par montant estimé.
5. Le montant à afficher est `estimatedAmount` : c'est le calcul qui sera facturé. Le devis du module ressource (`/resource-pricing-rules/quote`) peut différer (voir [ressource](../ressource/README.md#règles-et-erreurs-à-refléter-dans-lécran)).

### Désigner le titulaire

Les mêmes champs servent à la vérification, au blocage, à la création et à la dérogation de quota :

1. Au plus **un** de `memberId`, `customerId`, `businessCode` (sinon `400` `Only one owner selector is allowed...`). Il doit exister (sinon `404`) ; un client supprimé est refusé (`400`).
2. À défaut, `email` puis `phone` sont cherchés parmi les membres, puis les clients, puis les entreprises.
3. À défaut encore, si `walkIn: true` ou un champ de contact est fourni, un **invité** est créé à la volée (code généré). Un invité ne peut pas payer par abonnement ou pass (`400`).
4. Rien de tout cela : `400` `A memberId, customerId, businessCode, email or phone is required`.
5. `contactName`, `contactEmail`, `contactPhone` remplacent les coordonnées de la fiche pour cette réservation.

### Créer une réservation (back-office)

1. Facultatif : poser un blocage pendant la saisie, voir [Blocage temporaire](#blocage-temporaire-hold).
2. `POST /sni/api/v1/bookings` ([détail](routes/BookingController.md#r-post-sni-api-v1-bookings)) avec `resourceCode`, `startedAt`, `endedAt`, `paymentMode` (obligatoires), le titulaire, `quantity`, et selon le besoin :
   - `idempotencyKey` (dans le **corps**, pas en en-tête) : une clé par intention ; un rejeu rend la réservation existante, ou `409` si la clé appartient à un autre titulaire ;
   - `holdNumber` : le blocage à convertir (même titulaire, ressource, plage et quantité exactes) ;
   - `confirmImmediately` (défaut `true`) : `false` laisse la réservation en `DRAFT` (sans effet en `DIRECT` ou avec approbation) ;
   - `sendEmail` (défaut `true`) : `false` supprime les courriels de cette transition (pas la notification dans l'application) ;
   - `bookingUnit` : impose l'unité facturée (le forfait compte alors une unité quelle que soit la durée, sauf l'heure) ; refusé si la ressource n'a pas de règle tarifaire active pour cette unité ;
   - `billingCompanyCode` : organisation pour le compte de laquelle le membre réserve (doit lui être rattachée), ou `NONE` ;
   - `participants`, `notes`, `metadataJson` (texte JSON libre).
3. Réponse `201` : la réservation. Selon le cas, `status` vaut :
   - `CONFIRMED` (cas courant : abonnement, pass, portefeuille, ou `DIRECT` saisi par l'équipe sur un début déjà passé) ;
   - `PENDING_PAYMENT` (`DIRECT`) : la facture est émise et envoyée ; orienter vers l'encaissement (modules `billing` et `payment`) ;
   - `PENDING_APPROVAL` : afficher « en attente d'approbation » ;
   - `IN_PROGRESS` ou `COMPLETED` : saisie par l'équipe d'une réservation déjà commencée ou terminée ;
   - `DRAFT` : si `confirmImmediately: false`.
4. Afficher `bookingNumber`, `paymentStatusLabel`, `seatsLabel` (« 2 places réservées sur 4 », « Espace entier · 3 participants (12 places) »), le QR (`checkInQrValue` dans une balise `<img>`), et le lien de visioconférence `virtualMeetingUrl` s'il existe.

Réservation par l'équipe : un compte `ROLE_SUPER_ADMIN`, `ROLE_ADMIN`, `ROLE_MANAGER` ou `ROLE_OPERATIONS_AGENT`, hors routes du portail, réserve **sans préavis** et peut saisir un début passé jusqu'à 31 jours en arrière (réglage `bokati.booking.staff.max-backdate-days`), sauf si la politique de la ressource l'interdit. La durée minimale et maximale s'appliquent toujours, l'approbation est sautée. Un début déjà passé confirme la réservation sur-le-champ, quel que soit le mode de règlement.

### Créer une série récurrente

1. `POST /sni/api/v1/bookings/recurring` avec `booking` (le même objet que la création, pour la première occurrence), `frequency` (`DAILY`, `WEEKLY`, `MONTHLY`), `intervalValue` (défaut 1), `occurrences` (2 à 60, défaut 2), `endDate` facultative (arrêt au-delà), `excludedDates` (dates `AAAA-MM-JJ` sautées).
2. Chaque occurrence passe toutes les règles de la création. Si une seule échoue, **aucune** n'est créée (une seule transaction) : afficher l'erreur et proposer d'exclure la date fautive.
3. Réponse `201` : la liste des réservations créées. `idempotencyKey` et `holdNumber` de l'objet `booking` sont ignorés pour les occurrences.

### Réserver plusieurs ressources pour un ou plusieurs membres

`POST /sni/api/v1/bookings/batches` : plusieurs lignes (ressource et créneau) pour un ou plusieurs membres, tout ou rien, **une seule facture par membre**. Détail complet, corps, erreurs et effets sur le paiement et l'annulation : [reservation-multi-ressources.md](reservation-multi-ressources.md). Chaque réservation porte `batchNumber` (`null` hors lot).

### Blocage temporaire (hold)

1. `POST /sni/api/v1/bookings/holds` avec `resourceCode`, titulaire, `startedAt`, `endedAt`, `quantity`, `ttlMinutes` (défaut 10), `idempotencyKey` facultatif. Le blocage retire aussitôt la capacité des créneaux.
2. Réponse `201` : `holdNumber`, `status: ACTIVE`, `expiresAt`. Afficher un compte à rebours.
3. Créer la réservation avec `holdNumber`. À la confirmation, le blocage passe `CONFIRMED` et la capacité n'est pas reprise une seconde fois.
4. Abandon : `PATCH /sni/api/v1/bookings/holds/{holdNumber}/release` rend la capacité.
5. Un blocage échu n'est plus utilisable (`409` `hold is not active`) mais **garde la capacité** tant que `POST /sni/api/v1/bookings/holds/expire-due?limit=100` n'a pas été appelé : toujours libérer explicitement un blocage abandonné.

### Approuver ou refuser

1. Liste : `GET /sni/api/v1/bookings?status=PENDING_APPROVAL`.
2. `PATCH /sni/api/v1/bookings/{bookingNumber}/approve` avec `{ "actor": "...", "reason": "...", "sendEmail": true }` (corps facultatif). `DIRECT` passe en `PENDING_PAYMENT` (facture émise) ; les autres modes sont confirmés (ressource, droit, portefeuille).
3. `PATCH /sni/api/v1/bookings/{bookingNumber}/reject` avec le même corps : statut `REJECTED`, motif enregistré (lisible dans l'historique), fonds du portefeuille rendus.

### Confirmer manuellement

`PATCH /sni/api/v1/bookings/{bookingNumber}/confirm` avec `{ "changedBy": "...", "reason": "...", "sendEmail": true }` (facultatif). Accepté depuis `DRAFT` et `PENDING_PAYMENT`. Attention : depuis `PENDING_PAYMENT`, la confirmation ne vérifie **aucun paiement** et la réservation s'affiche ensuite « Payé ». Réserver ce bouton aux cas où le paiement est constaté ailleurs.

### Accueil : arrivée, départ, démarrage, clôture

1. Liste du jour : `GET /sni/api/v1/bookings?startedFrom=2026-11-09T00:00:00&startedTo=2026-11-09T23:59:59&status=CONFIRMED`.
2. Arrivée : `PATCH /sni/api/v1/bookings/{bookingNumber}/check-in` avec `{ "actor": "...", "note": "...", "sendEmail": false }` (facultatif), ou par le QR : `PATCH /sni/api/v1/bookings/check-in/qr/{checkInToken}`. Accepté en `CONFIRMED` ou `IN_PROGRESS`. La réservation passe `IN_PROGRESS` si l'heure de début est atteinte, sinon reste `CONFIRMED`.
3. Fenêtre de pointage : de 15 minutes avant à 15 minutes après le début (`CHECKIN_EARLY_WINDOW_MINUTES`, `CHECKIN_LATE_WINDOW_MINUTES`). Seuls `ROLE_ADMIN` et `ROLE_SUPER_ADMIN` y échappent ; tout autre rôle reçoit `403` avec le message générique `Access to this resource is forbidden.` Afficher alors « hors de la fenêtre de pointage » et proposer l'arrivée anticipée.
4. Arrivée anticipée (sans fenêtre) : `PATCH /sni/api/v1/bookings/{bookingNumber}/early-check-in`, droit `BOOKING:UPDATE`.
5. Démarrer sans pointage : `PATCH .../start` (`CONFIRMED` uniquement, même fenêtre que le pointage).
6. Départ : `PATCH .../check-out`, seulement en `IN_PROGRESS` (`409` sinon). Le statut ne change pas.
7. Clôturer : `PATCH .../complete` (`CONFIRMED` ou `IN_PROGRESS`), consomme le droit d'abonnement ou de pass, crée une tâche de remise en état. Sinon le worker clôt à l'heure de fin.
8. Absence : `PATCH .../no-show` (`CONFIRMED` ou `IN_PROGRESS`, droit `BOOKING:CANCEL`). Le droit est consommé ; aucun remboursement. Sinon le worker la marque à l'heure de fin si personne n'a pointé ni démarré.

### Annuler

1. Afficher les règles de remboursement : `GET /sni/api/v1/bookings/cancellation-policy` (droit `BOOKING:CANCEL`) : paliers `hoursBeforeStart` et `refundPercentage`, dans l'ordre `ruleOrder`.
2. `PATCH /sni/api/v1/bookings/{bookingNumber}/cancel` avec `{ "changedBy": "...", "reason": "...", "sendEmail": true }` (facultatif).
3. Refusée (`409`) depuis `COMPLETED`. Pour une réservation qui engage quelque chose (`CONFIRMED`, `IN_PROGRESS`, `PENDING_APPROVAL`, `NO_SHOW`, `REJECTED`), la politique de la ressource s'applique, **pour le back-office aussi** : `allowCancellation` doit être vrai (`resource policy does not allow cancellation`) et le début doit être à plus de `cancellationNoticeMinutes` (`cancellation notice is not respected`). Exemptées : `DRAFT`, `PENDING_PAYMENT`, et une réservation au portefeuille dont les fonds sont encore bloqués (première minute).
4. Effets : créneaux rendus et droit libéré (si `CONFIRMED` ou `IN_PROGRESS`), blocage de portefeuille rendu, statut `CANCELLED`. Facture : sans encaissement, elle est annulée et archivée ; avec encaissement sur une réservation active, un avoir est émis selon le premier palier dont `hoursBeforeStart` est atteint, et le remboursement suit le moyen de paiement d'origine (portefeuille compris). Ces écritures sont faites après coup : un échec n'empêche pas l'annulation.

### Modifier une réservation

1. Déplacer : `PATCH .../reschedule` avec `newStartedAt`, `newEndedAt`, `actor`, `note`, `sendEmail`. Accepté en `CONFIRMED` et `PENDING_APPROVAL`. Refusé si une autre réservation active chevauche la nouvelle plage sur la ressource (même partagée) ; ni la politique ni l'alignement ni le prix ne sont recalculés.
2. Changer de ressource : `PATCH .../change-resource` avec `newResourceCode`, `actor`, `note`, `sendEmail`. `CONFIRMED` uniquement, même contrôle de chevauchement, prix inchangé.
3. Transférer à un autre titulaire : `PATCH .../transfer` avec `newOwnerType`, `newOwnerCode` (non vérifié en base), coordonnées facultatives. Tout statut sauf `COMPLETED` et `CANCELLED`. Le droit consommé, la facture et le portefeuille restent ceux du titulaire d'origine.
4. Participants : `POST .../participants` avec `memberCode`, `name`, `email`, `phone`, `role` (défaut `GUEST`, valeurs insensibles à la casse) ; retrait : `DELETE .../participants/{participantId}` (statut `REMOVED`, la ligne reste).
5. Historique : `GET .../history` (statuts, auteurs, motifs) et `GET .../events` (journal détaillé).

### Liste d'attente

1. Inscrire : `POST /sni/api/v1/bookings/waitlist` avec `resourceCode`, `identityLookup` (**objet imbriqué** : `{ "memberId": "...", ... }`), `startedAt` et `endedAt` dans le futur, `quantity`, `paymentMode`. Réponse `201` avec `alternatives` (fenêtres libres proches).
2. Suivre : `GET /sni/api/v1/bookings/waitlist?status=OFFERED` (défaut `WAITING`). Une entrée `OFFERED` signifie que la plage est redevenue libre jusqu'à `expiresAt` : **personne n'est prévenu**, l'équipe doit contacter la personne et créer la réservation elle-même.
3. Annuler : `PATCH /sni/api/v1/bookings/waitlist/{id}/cancel`.

### Quotas et approbation par public

1. Politiques : `POST /sni/api/v1/bookings/policies` avec `audienceType` (obligatoire), `resourceTypeCode`, `resourceGroupCode`, `approvalRequired`, plafonds (`maxActiveBookings`, `maxBookingsPerDay`/`Week`/`Month`, `maxNoShowsPerMonth`), `minBookingNoticeMinutes`, `maxBookingDurationMinutes`, `active`. Liste : `GET /sni/api/v1/bookings/policies`. La politique retenue pour une réservation est la plus précise (type puis groupe renseignés), puis la plus récente.
2. Dérogations : `POST /sni/api/v1/bookings/quota-overrides` avec le titulaire (champs à plat), `resourceCode` facultatif, quotas supplémentaires, `reason`, `approvedBy`, `validFrom` et `validUntil` (`Instant` avec fuseau). Liste : `GET /sni/api/v1/bookings/quota-overrides`.
3. Aucune route ne modifie, désactive ni supprime une politique ou une dérogation.

### Pages publiques à jeton

Ces routes sont des **pages HTML** rendues par le serveur (sauf l'auto-pointage, en JSON). Le frontend n'a en général qu'à y renvoyer par un lien.

1. Confirmation : `GET /sni/api/v1/public/bookings/{bookingNumber}?token=<checkInToken>` (page HTML avec bouton d'auto-pointage) et `GET /sni/api/v1/public/bookings/{bookingNumber}/confirmation.pdf?token=<checkInToken>` (PDF). Mauvais jeton : `404`. Ces liens figurent dans le courriel de confirmation.
2. Auto-pointage : `POST /sni/api/v1/public/bookings/check-in/self?token=<checkInToken>&lat=...&lng=...` (paramètres de requête, pas de corps). Réponse JSON `{ "success": true, "bookingNumber", "resourceName", "status" }` ou `{ "success": false, "error", "message" }` avec `error` = `location_required` (`400`, position exigée quand un point de géolocalisation est configuré), `location_invalid` (`403`, à plus de 200 m), `time_invalid` (`403`, hors fenêtre de pointage, message en français à afficher), `checkin_failed` (`400`). Jeton inconnu : `404` au format d'erreur standard.
3. Borne de scan à l'accueil : le QR de la réservation ouvre `GET /sni/api/v1/public/bookings/check-in/scan/{checkInToken}`. Un poste non autorisé voit une page qui demande la clé du poste (`POST .../check-in/scanner-verify` avec `checkInToken` et `adminCode` en formulaire) ; un poste autorisé voit une page de confirmation qui envoie `POST .../check-in/scan` (`checkInToken` en formulaire). L'autorisation est un cookie `bokati_checkin_grant` (`HttpOnly`, `SameSite=Strict`, 30 jours), posé aussi par `GET`/`POST .../check-in/scanner-setup` (`setupKey` en formulaire). La clé est `CHECKIN_SCANNER_KEY` côté serveur.
4. Satisfaction : `GET /sni/api/v1/public/bookings/csat/{bookingNumber}?score=1..5`, lien du courriel de clôture ; page de remerciement.

## Règles et erreurs à refléter dans l'écran

Les refus métier sont des `409` `CONFLICT` dont le message commence par `Could not process booking: ` (ou `booking policy`, `booking quota`, `booking hold`), suivi d'une raison en anglais. Principales raisons :

| Raison (fin du message) | Cause | Quoi afficher |
| --- | --- | --- |
| `resource is not enabled for booking` / `resource status does not allow booking` | Ressource inactive, non réservable ou pas `ACTIVE` | Ressource indisponible |
| `the requested start time is in the past` | Début passé (hors équipe) | Choisir un horaire futur |
| `minimum booking notice is not respected` / `audience minimum booking notice is not respected` | Préavis de la ressource ou du public | Indiquer le préavis |
| `requested duration is below resource policy minimum` / `exceeds resource policy maximum` / `audience maximum booking duration exceeded` | Durée hors bornes | Bornes de la politique |
| `requested quantity exceeds resource bookable slots` / `requested attendees exceed the resource seating capacity` | Trop de places ou de participants | Capacité restante |
| `resource is already booked for the requested range` | Ressource à une seule place déjà confirmée | Proposer les suggestions |
| `insufficient remaining availability for the requested range` / `no availability exists for the requested range` / `the requested range is not fully covered by contiguous availability slots` | Créneaux pleins, fermés ou inexistants (mois suivant non ouvert) | Plage indisponible |
| `active booking quota exceeded`, `daily ...`, `weekly ...`, `monthly booking quota exceeded` | Quota du public atteint | Proposer une dérogation |
| `monthly no-show quota exceeded` | Trop d'absences ce mois (un ticket support est ouvert) | Contacter le support |
| `expected status X but was Y`, `only confirmed or in-progress bookings can ...` | Action non permise dans le statut actuel | Masquer le bouton |
| `resource policy does not allow cancellation` / `cancellation notice is not respected` | Règles d'annulation | Expliquer, proposer l'absence ou le support |
| `hold is not active` / `hold owner does not match booking owner` / `hold does not match booking slot` | Blocage échu, libéré, ou différent | Refaire un blocage |
| `this idempotency key is already used by another booking` | Clé rejouée pour un autre titulaire | Générer une nouvelle clé |

Autres :

| Situation | Statut | Message |
| --- | --- | --- |
| Plage non alignée, secondes | `400` | `La plage doit etre alignee sur des creneaux de <n> minutes` / `Booking range must not contain seconds or fractional seconds` |
| Fin avant début | `400` | `Booking end date must be after start date` |
| Saisie de l'équipe trop ancienne | `400` | `Une reservation saisie apres coup ne remonte pas au-dela de 31 jours.` |
| Solde du portefeuille insuffisant | `400` | `Solde du portefeuille insuffisant pour cette reservation · <montant> XAF requis` (ou « de l'organisation ») |
| Aucun abonnement ou pass utilisable | `404` | `No active subscription found for booking owner` / `No usable pass entitlement found for this resource` / `No active subscription with a matching entitlement found ...` |
| Unité imposée sans grille | `400` | `La ressource <code> n'a aucune grille tarifaire active en <unité> ...` |
| Pointage hors fenêtre (back-office) | `403` | `Access to this resource is forbidden.` |
| Réservation, blocage, participant inconnus | `404` | `Booking <n> not found` (et variantes) |

Concurrence : une réservation `PENDING_PAYMENT`, `PENDING_APPROVAL` ou `DRAFT` ne retient **aucun** créneau. Deux réservations en attente peuvent viser la même plage ; la première confirmée l'emporte, la seconde échoue à sa confirmation (pour un paiement reçu, l'échec est seulement journalisé et la réservation reste `PENDING_PAYMENT`). Pour garantir la plage pendant l'attente, poser un blocage.

Quotas du public : `maxActiveBookings` compte les réservations `CONFIRMED`, `IN_PROGRESS` et `PENDING_APPROVAL` du titulaire ; les quotas jour, semaine (lundi à dimanche) et mois comptent celles qui commencent dans la période, hors `CANCELLED`, `REJECTED` et `EXPIRED`.

## À prévoir côté frontend

- Écrans : recherche de disponibilité avec devis et suggestions ; formulaire de réservation (choix du titulaire, mode de règlement, organisation, participants, unité imposée) ; liste filtrable (texte, titulaire, ressource, statut, période) ; fiche réservation (statut, paiement, lignes, participants, QR, historique, événements, actions) ; accueil du jour (arrivée, départ, absence) ; file d'approbation ; liste d'attente ; politiques d'audience et dérogations ; règles d'annulation (lecture seule).
- Droits, à comparer à la liste `roles` :
  - `BOOKING:READ` : listes, fiche, historique, événements, politiques, dérogations, liste d'attente, indicateurs (`GET /bookings/metrics/overview`) ;
  - `BOOKING:CREATE` : vérification, suggestions, création, série, blocage, participants, liste d'attente, politiques, dérogations, rappel, et les déclenchements manuels (`holds/expire-due`, `notifications/dispatch-due`, `waitlist/promote-available`, `waitlist/expire-offers`, `repair`) ;
  - `BOOKING:UPDATE` : confirmer, approuver, démarrer, clôturer, arrivée anticipée, déplacer, changer de ressource, transférer, retirer un participant, libérer un blocage ;
  - `BOOKING:CANCEL` : annuler, refuser, absence, annuler une entrée de liste d'attente, lire les règles d'annulation ;
  - `BOOKING:CHECKIN` : arrivée, arrivée par QR, départ.
- Boutons selon le statut : Confirmer (`DRAFT`, `PENDING_PAYMENT`, à protéger) ; Approuver et Refuser (`PENDING_APPROVAL`) ; Arrivée, Démarrer (`CONFIRMED`), Arrivée et Départ (`IN_PROGRESS`) ; Clôturer et Absence (`CONFIRMED`, `IN_PROGRESS`) ; Déplacer (`CONFIRMED`, `PENDING_APPROVAL`) ; Changer de ressource (`CONFIRMED`) ; Annuler (tout sauf `COMPLETED` et `CANCELLED`).
- La liste `GET /bookings` est une page maison triée par pertinence puis début décroissant ; le paramètre `sort` est ignoré. `GET /bookings/search?query=` rend au plus 20 résultats pour l'autocomplétion.
- Champs absents de la réponse : heure de pointage et de sortie, motif de refus ou d'annulation, numéro de facture, numéro de série récurrente. Les lire dans `history` et `events`, ou dans le module `billing`.
- Ne pas envoyer de fuseau dans les dates de plage ; arrondir les heures saisies sur la durée de créneau de la ressource (`slotDurationMinutes` de la ressource).
- Un rappel (`POST /bookings/{bookingNumber}/notifications/reminder?minutesBefore=30`) est seulement mis en file : il ne part que si quelqu'un appelle `POST /bookings/notifications/dispatch-due`. Si l'écran propose le rappel, prévoir aussi ce déclenchement.
- Afficher `checkInQrValue` comme image ; le contenu du QR est l'adresse de la page de scan publique, utilisable par la borne d'accueil.
- Aucune notification temps réel propre au module ; les notifications dans l'application passent par le module `notification`.

## Effets automatiques

- **Cycle de vie** (`BookingNoShowWorker`, toutes les 60 s, `bokati.booking.lifecycle.worker-delay-ms`), par lots de 200, une réservation à la fois : passe en `IN_PROGRESS` les réservations pointées d'avance dont l'heure est venue ; marque `NO_SHOW` celles dont la fin est passée sans pointage ni démarrage ; clôt `COMPLETED` celles dont la fin est passée après pointage ou démarrage. Ces transitions automatiques n'envoient **aucun courriel** (donc pas de lien de satisfaction).
- **Règlement des portefeuilles** (`BookingWalletSettlementWorker`, toutes les 15 s) : un blocage de plus d'une minute (`bokati.booking.wallet.settlement-delay-minutes`) devient un débit sur la facture de la réservation ; il est rendu si la réservation est annulée ou refusée, ou si la facture est déjà soldée. Un blocage orphelin expire au bout d'une heure de grâce (`WalletHoldExpiryWorker`, module `payment`).
- **Liste d'attente** (`BookingWaitlistWorker`, toutes les 5 minutes) : expire les offres échues, puis passe en `OFFERED` les entrées dont la plage n'a plus de réservation active. Aucune notification.
- **Confirmation après paiement** (module `payment`) : quand l'intention de paiement d'une réservation `DRAFT` ou `PENDING_PAYMENT` est entièrement réglée, la réservation est confirmée, courriel compris.
- **Annulations système** : facture de la réservation annulée, paiement remboursé, ou annonce de paiement en espèces expirée sans encaissement (réservation encore en attente) : la réservation passe `CANCELLED`, sans contrôle de politique.
- **Courriels** : chaque transition écrit un événement publié dans la file d'événements (traitée toutes les 10 s, par 25) ; si `sendEmail` n'est pas `false`, un courriel part au contact pour la création, la confirmation (avec QR, lien de la page publique et du PDF), l'approbation, le refus, le démarrage, l'arrivée, la sortie, la clôture (avec liens de satisfaction 1 à 5), l'annulation, l'absence, le déplacement, le transfert et le changement de ressource. La facture est envoyée séparément à son émission.
- **Notification dans l'application** au contact pour chaque transition, même avec `sendEmail: false`.
- **Tâches** (module `task`) : à la confirmation, « Préparer la ressource » (priorité haute, échéance 2 h avant le début, `bokati.task.booking-prep-hours`) ; à la clôture manuelle ou automatique, « Nettoyer la ressource ».
- **Ticket support** automatique quand un titulaire dépasse son quota mensuel d'absences.
- **Facture** : émise et envoyée à la création pour un `DIRECT` en attente, sinon à la confirmation. Non vérifié : émission d'une facture à montant nul pour les réservations couvertes par un droit.

## Écarts relevés

- Pointage hors fenêtre refusé pour tout rôle autre que `ROLE_ADMIN` et `ROLE_SUPER_ADMIN` (un agent d'accueil `ROLE_OPERATIONS_AGENT` muni de `BOOKING:CHECKIN` est bloqué), et le message français est remplacé par le générique `Access to this resource is forbidden.` (`src/main/java/com/sni/bokaticowork/features/booking/service/implementation/BookingServiceImpl.java:1026-1062`, `src/main/java/com/sni/bokaticowork/core/exception/GlobalExceptionHandler.java:77-80`).
- `PATCH /bookings/{bookingNumber}/confirm` confirme une réservation `PENDING_PAYMENT` sans aucun paiement, et `paymentStatus` passe alors à `PAID` (`BookingServiceImpl.java:702-705`, `src/main/java/com/sni/bokaticowork/features/booking/enums/BookingPaymentStatus.java:61`). Même effet pour une réservation `DIRECT` saisie après coup par l'équipe.
- Déplacer une réservation `PENDING_APPROVAL` libère des créneaux jamais réservés puis en réserve d'autres avant toute confirmation, qui les réservera une seconde fois (`BookingServiceImpl.java:587-617`). Le déplacement et le changement de ressource refusent aussi tout chevauchement avec une autre réservation, même sur une ressource partagée, et ne recalculent ni prix ni politique (`BookingServiceImpl.java:937-947`).
- Les réservations en attente (`DRAFT`, `PENDING_PAYMENT`, `PENDING_APPROVAL`) ne retiennent aucun créneau et ne comptent pas dans les conflits : un paiement reçu peut échouer à confirmer en silence (`src/main/java/com/sni/bokaticowork/features/booking/repository/BookingRepository.java:43-58`, `src/main/java/com/sni/bokaticowork/features/payment/service/support/PaymentTransactionWorkflowProcessor.java:253-262`).
- Aucun worker n'expire les blocages temporaires ni n'envoie les rappels : capacité retenue et rappels en file jusqu'à un appel manuel (`src/main/java/com/sni/bokaticowork/features/booking/service/implementation/BookingHoldServiceImpl.java:88-96`, `src/main/java/com/sni/bokaticowork/features/booking/service/implementation/BookingNotificationServiceImpl.java:52-65`). Un blocage échu fait aussi échouer la confirmation d'une réservation payée plus tard (`BookingServiceImpl.java:892-894`).
- Liste d'attente : aucune notification à l'offre, statut `BOOKED` jamais posé, promotion fondée sur l'absence de réservation et non sur la capacité, et le type `BUSINESS` comparé au lieu de `BUSINESS_ENTITY` dans le calcul des alternatives (`src/main/java/com/sni/bokaticowork/features/booking/service/implementation/BookingWaitlistServiceImpl.java:83-101` et `:116`).
- `POST /bookings/repair` ne répare rien : il écrit un événement « inspecté » et rend toujours 0 réparation (`src/main/java/com/sni/bokaticowork/features/booking/service/implementation/BookingRepairServiceImpl.java`).
- Satisfaction publique sans jeton : n'importe qui peut noter n'importe quelle réservation par son numéro (`src/main/java/com/sni/bokaticowork/features/booking/controller/PublicBookingController.java:203-214`).
- Statut `EXPIRED` jamais atteint (`src/main/java/com/sni/bokaticowork/features/booking/enums/BookingStatus.java:13`).
- La règle d'annulation s'applique au back-office comme au client, y compris à `PENDING_APPROVAL` et `NO_SHOW` ; une politique de ressource créée sans `allowCancellation` rend les réservations confirmées non annulables (`BookingServiceImpl.java:999-1014`).
- Transfert sans vérification du nouveau titulaire et sans report du droit, de la facture ni du portefeuille (`BookingServiceImpl.java:567-584`).
- Politiques d'audience et dérogations de quota : création et liste seulement, aucune désactivation possible (`src/main/java/com/sni/bokaticowork/features/booking/controller/BookingController.java:217-235`).
- La réponse n'expose pas `checkedInAt`, `checkedOutAt`, `rejectionReason`, `cancellationReason` ni le numéro de facture (`src/main/java/com/sni/bokaticowork/features/booking/dto/response/BookingResponse.java`).
- L'ajout ou le retrait d'un participant est journalisé sous l'événement `BOOKING_CREATED` (`BookingServiceImpl.java:651` et `:662`).
- Confirmation après paiement : `changedBy` et `reason` inversés dans l'historique (`changedBy` reçoit la phrase, `reason` reçoit `SYSTEM`) (`PaymentTransactionWorkflowProcessor.java:254-258`).
- Paramètre `sort` ignoré par `GET /bookings` (`BookingServiceImpl.java:303`).
- Plafond de 3 requêtes par 10 minutes sur tout chemin `/public/bookings/check-in/scanner*`, lecture de la page d'installation comprise (`src/main/java/com/sni/bokaticowork/security/ratelimit/RateLimitRules.java:134-136`).
- Messages d'erreur majoritairement en anglais.
