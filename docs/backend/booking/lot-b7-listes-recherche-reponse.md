# Lot B7 : listes, recherche, réponse complète, performances

> Partie du [plan booking](README.md). Fait le 19/11/2026 sur `feature/booking`, sur le socle des listes de choix ([lot L0](../listes-de-choix/lot-l0-socle.md)). Migration V388 (prod V382). Écarts 7, 12, 13, 14, 16, Bn-7, Bn-22, Bn-24 ; Bn-25 en partie.

## Listes de choix

- `GET /bookings/reference` : en un appel, les valeurs fixes du module (registre L0, groupes `STATUS`, `PAYMENT_MODE`, `PAYMENT_STATUS`, `CHANNEL`, `PARTICIPANT_ROLE`, `PARTICIPANT_STATUS`, `RECURRENCE`, `HOLD_STATUS`, `WAITLIST_STATUS`, `LINE_TYPE`, `OPTION_KIND`), les listes de l'administrateur du module `BOOKING` avec leurs valeurs actives, et la grille d'annulation générale.
- Nouvelles listes (migration) : `BOOKING_CANCEL_REASON` (demande du client, changement d'agenda, maladie, doublon, erreur de saisie, ressource indisponible, paiement non reçu, autre) et `BOOKING_REJECT_REASON` (non éligible, créneau réservé, dossier incomplet, impayés, autre). Modifiables par `/reference-lists`.
- Motifs : `PATCH .../cancel` accepte `reasonCode` et `reasonText` (corps `BookingStatusChangeRequest`), `PATCH .../reject` aussi (`BookingApprovalRequest`). Le code est contrôlé par la liste (inconnu, retiré, précision manquante : 400) ; le libellé plus la précision deviennent le motif enregistré ; le code est gardé (`cancellation_reason_code`, `rejection_reason_code`). `reason`, texte libre, reste accepté.

## Recherche unique du titulaire

`GET /bookings/lookup/owners?q=&types=&page=&size=` : membres, clients et entreprises par nom, code, courriel ou téléphone, au format des recherches de choix L0 (`type` `MEMBER`, `CUSTOMER` ou `BUSINESS_ENTITY` ; `code` à renvoyer dans `memberId`, `customerId` ou `businessCode`). `types` restreint ; 50 éléments au plus par page.

## Réponse complète (écarts 13, Bn-7)

`BookingResponse` gagne : `createdBy`, `channel`, `holdNumber`, `recurrenceGroupNumber`, `paymentDueAt`, `paidAt`, `checkedInAt`, `checkedOutAt`, `approvedBy`, `approvedAt`, `rejectedBy`, `rejectedAt`, `rejectionReason`, `rejectionReasonCode`, `cancellationReason`, `cancellationReasonCode`, `invoiceNumber`, `extrasAmount`, `grandTotal`.

- `extrasAmount` : somme des lignes services, équipements, participants supplémentaires, caution, dépassement, non-présentation. `grandTotal` = `totalAmount` (ressource, remises déduites, sens inchangé) + `extrasAmount`.
- `invoiceNumber` : facture de la réservation, sur la fiche seulement.

## Listes allégées (écart Bn-24)

`GET /bookings`, `GET /bookings/search` et la liste du portail passent par `toListResponses` : lignes et participants de toute la page en deux requêtes (avant : deux par réservation), ni QR (`checkInQrValue` nul : avant, une image PNG rendue par ligne) ni facture. La fiche (`GET /bookings/{numéro}`) garde tout.

## Tri (écart 16)

`sort` est appliqué par `GET /bookings` et par le portail : `startedAt`, `endedAt`, `createdAt`, `totalAmount`, `status`, `bookingNumber`, `resourceCode`, `contactName`, `asc` ou `desc`. Sans `sort` : pertinence si `query` est donné, puis début décroissant. Un champ inconnu : 400. Le portail trie par défaut par début décroissant.

## Politiques et dérogations (écart 12)

- `PUT /bookings/policies/{policyNumber}` (même corps que la création), `PATCH .../activate`, `PATCH .../deactivate` ; `GET /bookings/policies?active=`.
- `PUT /bookings/quota-overrides/{overrideNumber}` (quotas, ressource, motif, validité ; le titulaire ne change pas), `PATCH .../activate`, `PATCH .../deactivate` ; `GET /bookings/quota-overrides?active=`.
- L'auteur d'une dérogation est lu dans la session (`approvedBy` de la requête ignoré).

## Participants (écart 14)

L'ajout et le retrait d'un participant sont journalisés `BOOKING_PARTICIPANT_ADDED` et `BOOKING_PARTICIPANT_REMOVED` (avant : `BOOKING_CREATED`), avec le nom et le courriel.

## Réparation (écart 7)

`POST /bookings/repair?limit=` rejoue tout de suite les tâches automatiques (options échues, réservations non réglées à l'échéance, démarrages, absences, clôtures) et rend `{ inspected, repaired, failed }` réels. Avant : un événement « inspecté » et toujours 0.

## Quota d'absences (écart Bn-22)

Un seul ticket au support par titulaire et par mois (avant : un par tentative de réservation dès qu'un ticket était fermé).

## Reporté

Pagination de la liste d'attente (lot B6, liste d'attente refaite). Historique et événements d'une réservation : bornés par réservation, gardés en liste.

## Tests

`BookingB7Test` (tri, types du titulaire, motifs, ligne de liste sans QR avec suppléments, réparation) ; tests existants des réservations, des droits et du portail.
