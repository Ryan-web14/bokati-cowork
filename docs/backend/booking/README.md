# Plan du module booking (réservations)

> Chantier ouvert le 18/11/2026 sur `feature/booking` (depuis `new-main` f65db369). Audit du code et de la doc frontend `docs/final-frontend-doc/booking/README.md`. Une page par lot, à mesure. Décisions à valider avant l'implémentation (section [Décisions](#décisions)).

## Demande

1. Corriger tous les écarts du module.
2. **Rendez-vous sans paiement, réglé sur place** :
   - possible côté équipe, et côté client si l'équipe l'active pour ce client ;
   - valable 2 heures par défaut, durée qui s'ajuste selon l'heure et le jour réservés.
3. **Tarif préférentiel ou ajustement du prix** directement à la réservation.
4. **Rendez-vous pris au téléphone** notés sur la plateforme.
5. **Fonctions nouvelles** pour un module complet.
6. Côté ressources : **visibilité par client** (membre, entreprise, client). Certaines ressources ne sont montrées qu'à certains clients, d'autres sur demande.
7. Restes reportés du chantier ressources :
   - couverture partielle d'une réservation par les heures incluses ;
   - messages en anglais ;
   - backfill KYC (jeton).

## Écarts de la doc frontend (18)

14 confirmés, 4 en partie réglés, aucun entièrement réglé. Le chantier ressources n'a réglé que le prix du déplacement (recalcul) et l'annulation permise par défaut. `BSI` = `features/booking/service/implementation/BookingServiceImpl.java`.

| N° | Écart | État | Preuve |
| --- | --- | --- | --- |
| 1 | Pointage hors fenêtre refusé à tout rôle sauf ADMIN, message remplacé par « Access to this resource is forbidden » | confirmé | BSI:1173-1209 ; `GlobalExceptionHandler.java:79-83` ; contredit `StaffBookingOverride.java:33` (MANAGER, OPERATIONS_AGENT) |
| 2 | `PATCH /confirm` confirme un `PENDING_PAYMENT` sans paiement, statut de paiement « PAID » | confirmé | BSI:398-401, 803-806 ; `BookingPaymentStatus.java:61` |
| 3 | Déplacement : créneaux jamais réservés libérés, conflit sans capacité, ni politique ni alignement ni date passée | en partie (prix recalculé) | BSI:682, 692-697, 1084-1094 ; `BookingRepository.java:71-86` |
| 4 | Réservations en attente sans créneau retenu ; paiement reçu puis confirmation échouée sans bruit | confirmé | `BookingRepository.java:77` ; BSI:814-818 ; `PaymentTransactionWorkflowProcessor.java:310-320` |
| 5 | Aucune tâche d'expiration des blocages ni d'envoi des rappels ; un blocage expiré casse la confirmation après paiement | confirmé | `BookingHoldServiceImpl.java:99-108` ; `BookingNotificationServiceImpl.java:63-85` ; BSI:1039-1041 |
| 6 | Liste d'attente : aucune notification à l'offre, `BOOKED` jamais posé, capacité ignorée, `BUSINESS` au lieu de `BUSINESS_ENTITY` | confirmé | `BookingWaitlistServiceImpl.java:98-131` |
| 7 | `POST /bookings/repair` ne répare rien, requête contradictoire | confirmé | `BookingRepairServiceImpl.java:21-25` ; `BookingRepository.java:224-227` |
| 8 | Satisfaction publique sans jeton (GET qui écrit) | confirmé | `PublicBookingController.java:233-244` |
| 9 | Statut `EXPIRED` jamais atteint | confirmé | `BookingStatus.java:13` |
| 10 | Règle d'annulation appliquée à l'équipe, y compris `PENDING_APPROVAL` et `NO_SHOW` | en partie (défaut réglé) | BSI:1146-1161 |
| 11 | Transfert sans contrôle du nouveau titulaire ni report du droit, de la facture, du portefeuille ; permis depuis `REJECTED` et `NO_SHOW` | confirmé | BSI:659-677 |
| 12 | Politiques d'audience et dérogations de quota : création et liste seulement | confirmé | `BookingController.java:240-258` ; `BookingPolicyServiceImpl.java:33-86` |
| 13 | Réponse sans `checkedInAt`, `checkedOutAt`, motifs, numéro de facture (ni `approvedBy`, `holdNumber`, série, trace du prix…) | confirmé | `BookingResponse.java:14-60` |
| 14 | Ajout et retrait de participant journalisés « BOOKING_CREATED » | confirmé | BSI:752, 763 |
| 15 | `changedBy` et `reason` inversés après paiement | confirmé | `PaymentTransactionWorkflowProcessor.java:311-315` |
| 16 | `sort` ignoré par `GET /bookings` (et au portail) | confirmé | BSI:366 ; `BookingRepository.java:118-127` |
| 17 | 3 requêtes par 10 minutes sur tout `/public/bookings/check-in/scanner*`, page d'installation comprise | confirmé | `RateLimitRules.java:158-160` |
| 18 | Messages en anglais (environ 65 sur 112), préfixe « Could not process booking » | en partie | `ConflictException.java:15-17` |

## Écarts nouveaux (Bn)

| N° | Écart | Preuve |
| --- | --- | --- |
| **Argent** | | |
| Bn-1 | Le recalcul refacture la remise d'un code promo (déplacement au même prix : facture complémentaire égale à la remise) et perd l'unité imposée par l'équipe | `BookingRepricer.java:74-98` ; `BookingExtras.java:339` |
| Bn-2 | Droit d'abonnement faussé après déplacement ou changement de ressource (réservé sur l'ancienne durée, libéré ou consommé sur la nouvelle ; ressource nouvelle peut-être non couverte) | `BookingEntitlementBridge.java:110, 171, 230` ; BSI:716-744 |
| Bn-3 | Annuler une réservation payée en partie laisse le reste dû | `BookingCancellationRefundSupport.java:93-127` |
| Bn-4 | Suppléments et compléments orphelins à l'annulation (factures à part jamais annulées ni remboursées) | `BookingExtras.java:385-393` ; `BookingRepricer.java:120-139` |
| Bn-5 | Échec de remboursement seulement journalisé : réservation annulée sans remboursement ni alerte | BSI:888-892 |
| Bn-6 | Deux sources de règles d'annulation : préavis de la politique de la ressource, pourcentage de la table globale | BSI:1146-1161 ; `BookingCancellationRefundSupport.java:103-107` |
| Bn-7 | `totalAmount` de la réponse sans les suppléments | `BookingExtras.java:385-397` |
| Bn-8 | Retour d'une confirmation : la réservation reste « payée », blocage de portefeuille et tâches non rendus | BSI:837-851 |
| **Cycle de vie** | | |
| Bn-9 | `NO_SHOW` et `REJECTED` annulables (et `COMPLETED` en mode système) | BSI:853-862 |
| Bn-10 | Absence et clôture sans garde horaire : une réservation future marquée absente (frais facturés) ou close avant de commencer (droit consommé) | BSI:909-949 |
| Bn-11 | `early-check-in` contourne la fenêtre et le droit de pointage (règle d'URL) | BSI:639-657 ; `AdminApiAuthorizationManager.java:388` |
| Bn-12 | Ajout de participant sans contrôle de statut ni de capacité, même sur une réservation annulée | BSI:746-754 |
| **Blocages, créneaux, tâches** | | |
| Bn-13 | Blocage d'un non-membre jamais confirmable (nouveau code invité à chaque appel) ; coordonnées du blocage perdues | `BookingIdentityResolver.java:139-147` ; BSI:1042 |
| Bn-14 | Blocage : durée sans borne (même négative), rejeu d'idempotence qui rend le blocage d'un autre, un seul blocage par plage quelle que soit la capacité, blocages expirés qui retiennent la capacité | `BookingHoldServiceImpl.java:47-85` |
| Bn-15 | Liste d'attente : 100 premières entrées sans filtre de date, entrées passées offertes, toutes les entrées d'un créneau offertes ensemble | `BookingWaitlistEntryRepository` |
| Bn-16 | Rappels envoyés aussi pour des réservations annulées, en anglais ; `200` avec 0 sans courriel | `BookingNotificationServiceImpl.java:47-85` |
| Bn-17 | Aucun courriel à la création en attente de paiement ou d'approbation : pas d'instructions de paiement | BSI:284-297 ; `BookingEventWriter.java:21-23` |
| **Sécurité et traçabilité** | | |
| Bn-18 | « Qui a fait » lu dans le corps de la requête (`changedBy`, `actor`, `approvedBy`, `rejectedBy`) ; pas de `createdBy` | BSI:407, 440, 1291-1293 |
| Bn-19 | Codes de photos (état des lieux, incidents) non contrôlés | `ClientBookingOperationsController.java:52-59` ; `BookingOperations.java:230-238` |
| Bn-20 | Position du pointage autonome fournie par le client | `PublicBookingController.java:188-206` |
| Bn-21 | Lire `/bookings/cancellation-policy` exige `BOOKING:CANCEL` (règle d'URL) | `AdminApiAuthorizationManager.java:389` |
| **Contrôles et quotas** | | |
| Bn-22 | Quota d'absences atteint : un ticket au support à chaque tentative | `BookingPolicyEnforcer.java:90-95` |
| Bn-23 | Les réservations en attente de paiement échappent au quota : un membre empile des réservations impayées | `BookingRepository.java:186` |
| **Performances** | | |
| Bn-24 | Par ligne de liste : deux requêtes, chargements paresseux et rendu d'un QR en PNG | `BookingMapperDecorator.java:81-134` |
| Bn-25 | Listes non paginées (attente, historique, événements, politiques, dérogations), moteur de suggestion par entrée d'attente | `BookingWaitlistServiceImpl.java:82-87` |
| **Fuseau** | | |
| Bn-26 | Dépassement calculé en heure de Brazzaville, le reste en heure du serveur ; `app.time-zone` jamais appliqué par défaut : un serveur en UTC facture une heure de dépassement à chaque départ | `BookingExtras.java:75, 252` ; `ApplicationConfig.java:37` |
| **Contradiction** | | |
| Bn-27 | La tâche d'expiration des déclarations d'espèces annule et archive la facture que sa doc dit maintenue ; les commentaires du portail promettent un créneau retenu qui ne l'est pas | `CashDeclarationExpiryWorker.java:30-32` ; `ClientPortalBookingService.java:104-106` |

Listes de choix, recherches et automatisations du module : voir [constat des listes de choix](../listes-de-choix/documents-audit-ressources-portail.md#réservations).

## Briques existantes pour les demandes

- **Sans paiement** : `DIRECT` attend déjà le paiement (`PENDING_PAYMENT`, élément à facturer). Le portail l'accepte sans condition. Modèle d'expiration : `CashDeclarationExpiryWorker` (passage toutes les 15 minutes, validité fixe de 4 h). Le blocage (`BookingHold`) retient la capacité mais n'a pas de tâche d'expiration. Aucun réglage par client : les voisins sont les conditions de paiement du client, la politique PREPAY du risque client, les dérogations de quota et les politiques d'audience.
- **Prix** : seule l'unité s'impose ; segment, plan et canal viennent du titulaire ; code promo, participants offerts et remise de lot existent. Aucun ajustement manuel, aucun motif, aucune approbation (le moteur d'approbation existe).
- **Téléphone** : ni canal ni origine ; `metadataJson` et les coordonnées du contact existent. Un non-membre devient un invité de passage (sans fiche client). Le prospect du CRM (`Lead`, avec `source`) n'est pas relié.
- **Rappels** : file de rappels par courriel (manuelle). WhatsApp seulement pour la confirmation, le déplacement, le changement de ressource et le transfert.

## Lots proposés

| Lot | Contenu | Écarts |
| --- | --- | --- |
| **B0. Sécurité et traçabilité** | « Qui a fait » lu dans la session, `createdBy` et `channel` enregistrés et rendus ; droits du pointage anticipé et de la grille d'annulation ; satisfaction à jeton ; photos contrôlées ; limite de débit du scanner revue ; pointage hors fenêtre pour l'équipe (rôles de `StaffBookingOverride`) avec message lisible | 1, 8, 15, 17, Bn-11, Bn-18 à Bn-21 |
| **B1. Créneaux et cycle de vie** | Réservations en attente qui retiennent leur créneau (et comptent dans les conflits et les quotas) ; tâche d'expiration (`EXPIRED`) ; blocages (tâche, invités, bornes, idempotence, capacité) ; gardes d'état (annulation, absence, clôture, transfert, participants) ; déplacement contrôlé ; confirmation sans paiement réservée à un droit et tracée | 2, 3, 4, 5, 9, 10, 11, Bn-9, Bn-10, Bn-12 à Bn-14, Bn-23 |
| **B2. Argent** | Recalcul qui garde remises et unité imposée ; droit d'abonnement recalculé ; annulation qui traite suppléments, compléments et reste dû ; échec de remboursement signalé (tâche) ; une seule source de règles d'annulation (politique de la ressource, grille par politique) ; total avec suppléments ; retour de confirmation complet ; couverture partielle par les heures incluses | Bn-1 à Bn-8, reste ressources |
| **B3. Rendez-vous sans paiement** | Mode « paiement sur place » : créneau retenu, validité calculée (2 h par défaut, ajustée selon l'heure et le jour), rappel avant échéance, expiration automatique ; côté client si l'équipe l'active pour ce client (membre, entreprise, client) ; encaissement sur place à l'accueil | demande 2 |
| **B4. Prix à la réservation** | Tarif préférentiel (grille par client ou segment) et ajustement manuel du prix avec motif, plafond et approbation au-delà ; trace dans la réservation et la facture | demande 3 |
| **B5. Prise de rendez-vous au téléphone** | Canal (téléphone, accueil, courriel, portail, WhatsApp), appelant non membre noté (prospect CRM ou fiche client), note d'appel, rappel et confirmation par SMS ou WhatsApp | demande 4 |
| **B6. Notifications et liste d'attente** | Rappels automatiques (courriel et WhatsApp) ; courriel des réservations en attente ; liste d'attente refaite (offre limitée dans le temps, notification, réservation à l'acceptation, portail) | 6, Bn-15 à Bn-17 |
| **B7. Listes, recherche, performances** | `GET /bookings/reference`, listes de motifs de l'administrateur, recherche unique du titulaire, pagination et tri, politiques et dérogations modifiables, réponse complète, mapper allégé, réparation réelle ou retirée | 7, 12, 13, 14, 16, Bn-22, Bn-24, Bn-25 |
| **B8. Messages et fuseau** | Messages en français ; fuseau de l'application appliqué partout | 18, Bn-26, Bn-27 |
| **B9. Fonctions nouvelles** | Selon le choix (voir décision D7) | |
| **B10. Visibilité des ressources par client** | Ressource visible de tous, réservée à des clients désignés, ou visible sur demande | demande 6 |
| **B11. Doc et tests** | Doc frontend booking, portail et ressource ; tests par lot ; backfill KYC | reste ressources |

## Décisions

À trancher avant l'implémentation : voir la conversation du 18/11/2026 ; reportées ici une fois prises.
