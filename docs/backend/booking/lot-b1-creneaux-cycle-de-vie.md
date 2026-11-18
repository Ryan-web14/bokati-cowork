# Lot B1 : créneaux et cycle de vie

> Partie du [plan booking](README.md). Fait le 18/11/2026 sur `feature/booking`. Migration V383 (prod V377).

## Créneau retenu dès la réservation en attente (écart 4, décision D2)

- Une réservation en attente de paiement ou d'approbation **retient son créneau dès sa création** (colonne `slots_reserved`). Avant, rien n'était retenu jusqu'à la confirmation : deux clients pouvaient attendre le même créneau, et le second paiement échouait sans bruit.
- La confirmation ne reprend pas un créneau déjà retenu et ne recontrôle pas la politique. Avant, un paiement arrivé tard pouvait buter sur le préavis.
- Le conflit d'une pièce entière compte les réservations en attente qui retiennent leur créneau ; le quota de réservations actives compte celles en attente de paiement (écart Bn-23).
- Créneau rendu à l'annulation, au refus et à l'expiration.

## Échéance de paiement et expiration (écarts 5 et 9, décision D1)

- Une réservation en attente de paiement reçoit une échéance (`paymentDueAt`). La règle et ses réglages :

  | Règle | Réglage |
  | --- | --- |
  | 2 h par défaut | `bookings.payment-due.default-minutes` |
  | Au plus tard 30 min avant le début ; dans les 30 dernières minutes, échéance au début (règlement à l'arrivée) | `...before-start-minutes` |
  | 24 h si le créneau est à plus de 3 jours | `...far-days`, `...far-minutes` |
  | Une échéance un samedi, un dimanche ou un jour férié passe au prochain jour ouvré à 10:00 | `...business-hour` |

- Nouvelle tâche `BookingExpiryWorker` (toutes les minutes, `bokati.booking.expiry.worker-delay-ms`) :
  - expire les options dont la durée est passée (capacité rendue ; avant, seulement par appel manuel) ;
  - passe en **`EXPIRED`** les réservations non réglées à l'échéance : créneau rendu, facture annulée, tâches annulées, client prévenu par l'événement d'annulation.
- Une option expirée au moment de confirmer ne bloque plus : la réservation reprend la capacité directement.
- Le rappel avant l'échéance arrive avec les notifications (lot B6).

## Règlement constaté (écart 2)

- Nouvelle colonne `paidAt`. Le statut de règlement vaut `PAID` seulement si un règlement est constaté : paiement reçu par la chaîne de paiement, ou portefeuille bloqué à la confirmation. Avant, toute confirmation valait « payé ».
- Une confirmation manuelle d'une réservation en attente de paiement reste possible (règlement sur place) et laisse la réservation « à régler ». Le paiement reçu plus tard la marque réglée.
- Un paiement reçu sans confirmation possible est journalisé en erreur, et le règlement est noté quand même.
- Reprise : les réservations déjà confirmées (hors droits) sont marquées réglées à leur date de confirmation.

## Retour d'une confirmation (écart Bn-8)

Le créneau reste retenu avec une nouvelle échéance. Le règlement et la date de confirmation sont effacés, et le blocage du portefeuille est rendu.

## Gardes d'état

- **Annulation (Bn-9)** : une réservation terminée, absente, refusée ou expirée ne s'annule plus (sauf annulation système).
- **Règle d'annulation (écart 10)** : rien d'engagé tant que la réservation attend (paiement ou approbation). L'équipe annule hors règle : la décision est la sienne, tracée.
- **Absence et clôture (Bn-10)** : absence constatée seulement après la fenêtre d'arrivée ; clôture seulement une fois la réservation commencée.
- **Transfert (écart 11)** :
  - seulement depuis le brouillon, l'attente ou la confirmation ;
  - nouveau titulaire vérifié pour un membre ;
  - refusé pour une réservation couverte par un droit ou réglée par portefeuille (annuler et réserver pour le nouveau titulaire).
- **Participants (Bn-12)** : seulement sur une réservation vivante, dans la capacité de la ressource.

## Déplacement et changement de ressource (écart 3)

- Mêmes contrôles qu'à la réservation : ressource réservable, alignement, politique, date passée (l'équipe passe outre si la politique l'admet).
- Les créneaux ne sont rendus puis repris que s'ils étaient retenus. La reprise contrôle la capacité, y compris sur un espace partagé.
- Déplacement admis aussi pour une réservation en attente de paiement.

## Options (écarts Bn-13, Bn-14)

- Durée d'une option bornée : de 1 à 1 440 minutes.
- Un rejeu de clé d'idempotence ne rend jamais l'option d'un autre créneau.
- Plusieurs options sur un espace partagé (la capacité des créneaux tranche) ; une seule sur une pièce entière.
- Option d'un invité : ses coordonnées sont gardées, et la réservation la retrouve par le courriel ou le téléphone (un invité reçoit un nouveau code à chaque appel).

## Tests

`BookingB1Test` : échéance par défaut, créneau proche, créneau lointain, week-end, règlement constaté. Les tests existants des réservations, options et paiements passent.
