# portal : ressources et réservations

> Partie du [README du module portal](README.md). Routes exactes et modèles : [ClientResourceController](routes/ClientResourceController.md), [ClientResourceTypeController](routes/ClientResourceTypeController.md), [ClientBookingController](routes/ClientBookingController.md). Fonctionnement de fond (statuts de réservation, modes de paiement, droits consommés, politiques d'annulation, arrivée et départ) : [../booking/README.md](../booking/README.md) ; ressources, horaires, tarifs : [../ressource/README.md](../ressource/README.md). Règles communes : [README racine](../README.md).

Code : `src/main/java/com/sni/bokaticowork/features/portal/resource/`, `booking/`, `context/SubscriberAccess.java`.

## Ce qui est propre au portail

- **Ressources visibles** : seules celles marquées visibles au portail, au statut `ACTIVE` et réservables (`portalVisible`, `status = ACTIVE`, `bookingEnabled`). Toute autre répond `404` « Resource not found », en lecture comme en réservation par son code (`ClientResourceService.assertPortalVisible`, `src/main/java/com/sni/bokaticowork/features/portal/resource/service/ClientResourceService.java:149`).
- **Créneaux réservés aux abonnés** : certaines plages de disponibilité d'une ressource sont marquées « abonnés seulement » (en pratique les soirées). Un membre est abonné s'il a un abonnement actif à la date du jour, à son nom, au nom de son client, ou au nom d'une organisation où il est rattaché et actif (`SubscriberAccess.isSubscriber`, `src/main/java/com/sni/bokaticowork/features/portal/context/SubscriberAccess.java:39`). Pour un non-abonné, ces plages n'apparaissent pas dans le calendrier ni dans les créneaux, la vérification de disponibilité répond `available = false` avec le motif « Créneau réservé aux abonnés », et la création répond `403` « Ce créneau est réservé aux abonnés. Souscrivez un abonnement ou adressez-vous à l'accueil. » L'accueil, lui, peut réserver ces plages pour n'importe qui.
- **Identité** : le serveur remplit le titulaire et le contact (nom, courriel, téléphone du membre) ; l'écran ne les envoie pas.
- **Tarif** : déduit de la durée ; le portail n'envoie aucune unité de réservation à la création.
- **Pour une organisation** : `billingCompanyCode` désigne l'organisation pour le compte de laquelle le membre réserve (lien en mode « au choix »). Vide : pour lui-même, ou pour son organisation si son lien est en mode automatique. Dans la vérification de disponibilité, `NONE` signifie « aucune organisation ». Le membre doit être rattaché à l'organisation, sinon refus (module `booking`).
- **Périmètre** : la liste et le détail ne montrent que les réservations dont le titulaire est le membre (`ownerType = MEMBER`). Une autre répond `404` « Booking not found ».

## Parcours

### Trouver une ressource

1. Types pour le filtre : `GET /sni/api/v1/client/resource-types` (types actifs) ou `GET /sni/api/v1/client/resource-types/search?query=...`.
2. Liste : `GET /sni/api/v1/client/resources?type=<code de type>&group=<code de groupe>&q=<texte dans le nom>&capacity=<capacité minimale>` (page, tri `displayOrder` croissant). Chaque ressource : `code`, `name`, `description`, `typeName`, `capacity`, `zone`, `locationLabel`. Un `type` ou `group` inconnu est ignoré sans erreur.
3. Détail : `GET /sni/api/v1/client/resources/{code}` : en plus, `amenities`, `pricingRules` actives, et la politique de réservation : `minBookingDurationMinutes`, `maxBookingDurationMinutes`, `minBookingNoticeMinutes` (délai de prévenance), `cancellationNoticeMinutes`, `allowCancellation`.

### Choisir un créneau

1. Vue calendrier : `GET /sni/api/v1/client/resources/{code}/calendar?from=AAAA-MM-JJ&to=AAAA-MM-JJ` (obligatoires). Les plages réservées aux abonnés n'y figurent que pour un abonné.
2. Créneaux libres : `GET /sni/api/v1/client/resources/{code}/slots?startedAt=...&endedAt=...&durationMinutes=30&quantity=1` (dates-heures ISO, sans fuseau accepté en paramètre de requête). Réponse : la liste des fenêtres restantes de la durée demandée.
3. Prix indicatif : `GET /sni/api/v1/client/resources/{code}/quote?bookingUnit=...&startedAt=...&endedAt=...` (`bookingUnit` obligatoire, valeurs du module `ressource`). Ce devis ne tient pas compte de la restriction abonnés.
4. Vérification finale : `POST /sni/api/v1/client/bookings/availability` avec `{ "resourceCode", "startedAt", "endedAt", "quantity" (défaut 1), "paymentMode" (facultatif), "billingCompanyCode" (facultatif, `NONE` pour aucune) }`. Avec `paymentMode` `SUBSCRIPTION` ou `PASS`, le serveur vérifie aussi qu'un droit couvre la réservation. Réponse : `available`, motif d'indisponibilité, `durationMinutes`, `bookingUnit`, `unitPrice`, `estimatedAmount`, `currency`.

### Réserver

1. `POST /sni/api/v1/client/bookings` (ou l'alias `POST /sni/api/v1/client/bookings/create`, [détail](routes/ClientBookingController.md#r-post-sni-api-v1-client-bookings)) avec :
   - `resourceCode`, `startedAt`, `endedAt` (dates-heures **locales sans fuseau**, par exemple `2026-11-12T18:00:00`), `quantity` (défaut 1, au moins 1) ;
   - `paymentMode` (obligatoire) : `DIRECT` (à régler par un moyen classique), `WALLET` (fonds bloqués tout de suite sur le portefeuille du membre, réservation confirmée aussitôt), `SUBSCRIPTION` ou `PASS` (consomme un droit, rien à payer) ;
   - `notes`, `participants` (liste de `{ memberCode, name, email, phone, role }`), `billingCompanyCode` ;
   - `idempotencyKey` (dans le **corps**) : générer une clé par clic sur « Réserver », la garder pour les nouvelles tentatives.
2. Réponse `201` : la réservation (`bookingNumber`, `status`, `paymentMode`, `paymentStatus`, `paymentStatusLabel`, `totalAmount`, `currency`, `checkInToken`, `checkInQrValue`, `virtualMeetingUrl`, `subscriptionNumber` ou `passNumber` consommé).
3. Selon le statut :
   - `CONFIRMED` : afficher le QR d'arrivée (`checkInQrValue`) ;
   - `PENDING_PAYMENT` (mode `DIRECT`) : proposer le paiement de la facture de la réservation (la retrouver dans `GET /sni/api/v1/client/billing/invoices/payable`, source `BOOKING`, puis [payer](parcours-facturation-paiements.md#payer-une-facture)) ou [annoncer un paiement en espèces](#annoncer-un-paiement-en-espèces) ;
   - `PENDING_APPROVAL` : attendre la validation de l'équipe (notification `BOOKING_CONFIRMED` ou `BOOKING_REJECTED`).

Les transitions et délais (expiration d'une réservation non payée, libération des droits, arrivée, absence) sont décrits dans [../booking/README.md](../booking/README.md).

### Annoncer un paiement en espèces

1. `POST /sni/api/v1/client/bookings/{bookingNumber}/pay/cash` avec `{ "amount" (facultatif, défaut : solde), "note" }` (corps facultatif). Accepté seulement si la réservation est `PENDING_PAYMENT` ou `DRAFT`.
2. Le serveur retrouve la facture de la réservation et crée une annonce `AWAITING_CONFIRMATION` valable 4 heures par défaut (`bokati.payment.cash-declaration.validity-hours`). Le créneau est tenu jusqu'à l'échéance de l'annonce ; passé ce délai sans encaissement à la caisse, l'annonce expire et la réservation est rendue (worker toutes les 15 minutes).
3. Afficher `declarationNumber`, le montant et l'échéance. Suivi et annulation : [parcours-facturation-paiements.md](parcours-facturation-paiements.md#annoncer-un-paiement-en-espèces).

### Consulter et annuler ses réservations

1. Liste : `GET /sni/api/v1/client/bookings?status=...&from=...&to=...` (dates-heures, page, tri `startedAt` décroissant).
2. Détail : `GET /sni/api/v1/client/bookings/{bookingNumber}`.
3. Annuler : `DELETE /sni/api/v1/client/bookings/{bookingNumber}` avec `{ "reason" }` facultatif (500 caractères au plus). Accepté seulement pour `CONFIRMED`, `PENDING_APPROVAL` ou `PENDING_PAYMENT` ; les règles de délai (`allowCancellation`, `cancellationNoticeMinutes`) et de remboursement ou de libération du droit sont appliquées par le module `booking`. Réponse : la réservation `CANCELLED`.

## Règles et erreurs à refléter dans l'écran

| Cas | Statut | Message |
| --- | --- | --- |
| Ressource non proposée au portail | `404` | `Resource not found` |
| Plage réservée aux abonnés, membre non abonné | `403` | `Ce créneau est réservé aux abonnés. Souscrivez un abonnement ou adressez-vous à l'accueil.` |
| Annulation hors `CONFIRMED`, `PENDING_APPROVAL`, `PENDING_PAYMENT` | `400` | `Only CONFIRMED, PENDING_APPROVAL or PENDING_PAYMENT bookings can be cancelled. Current status: ...` |
| Annonce d'espèces sur une réservation qui n'attend pas de paiement | `400` | `La réservation ... n'attend pas de paiement (...).` |
| Réservation d'un autre | `404` | `Booking not found` |
| Durée, prévenance, chevauchement, capacité, droit insuffisant, solde insuffisant en mode `WALLET` | `400`/`409` | messages du module `booking` |

Avant d'envoyer, l'écran peut contrôler lui-même la durée (`minBookingDurationMinutes`, `maxBookingDurationMinutes`) et la prévenance (`minBookingNoticeMinutes`) lues dans le détail de la ressource.

## À prévoir côté frontend

- Écrans : catalogue de ressources avec filtres, fiche ressource avec calendrier, choix du créneau, récapitulatif (via `availability`), confirmation avec QR, mes réservations, détail avec annulation.
- Proposer `paymentMode` selon le contexte : `SUBSCRIPTION` si le membre a un droit (`GET /sni/api/v1/client/entitlements/balances`), `PASS` s'il a un pass, `WALLET` s'il a un portefeuille approvisionné, sinon `DIRECT`. Vérifier avec `availability` en passant le mode choisi.
- Proposer le choix « pour moi / pour mon organisation » seulement si `GET /sni/api/v1/client/companies` rend une organisation dont `effectiveInvoiceLinkMode` vaut `ON_CHOICE`.
- Envoyer `startedAt`/`endedAt` sans fuseau ni `Z` dans les corps de réservation.
- Le QR d'arrivée se construit à partir de `checkInQrValue`.
