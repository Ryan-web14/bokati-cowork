# Lot R5c : vente

> Partie du [plan ressources](README.md). Fait le 18/11/2026 sur `feature/ressources`. Migration V378 (prod V372).

## Principe de facturation

Chaque supplément devient une ligne de la réservation (`GET /bookings/{numéro}/lines`) et un élément à facturer :

- avant la facture de la réservation, il attend et part **sur la même facture** que la ressource ;
- après, il part **sur une facture à lui** ;
- une remise après facture devient un **avoir**.

Types de ligne : `SERVICE`, `AMENITY`, `PARTICIPANT`, `DEPOSIT`, `OVERRUN`, `NO_SHOW`, `DISCOUNT` (montant négatif). Les supplements sont posés par l'équipe (droit `BOOKING`). Une réservation close (annulée, refusée, expirée, non présentée) n'en reçoit plus.

## Services additionnels

`POST /bookings/{numéro}/services` `{ code, quantity }`, où `code` est un article du catalogue des services (café, traiteur, impression, matériel). Il est facturé au prix du catalogue. Un article désactivé est refusé.

## Équipements optionnels (écart Rn-11)

`POST /bookings/{numéro}/amenities` `{ code, quantity }` : équipement rattaché à la ressource comme optionnel, facturé à son `extraPrice`. Un équipement compris dans la ressource (non optionnel) est refusé. `optional` et `extraPrice` servent enfin.

## Participants supplémentaires

- Réglages de la ressource, `PUT /resources/{code}/sales-settings` `{ includedParticipants, extraParticipantPrice, depositAmount }`. La réponse de la ressource les rend.
- `POST /bookings/{numéro}/extra-participants` `{ count, waive }` : au prix par participant de la ressource, ou offerts si l'équipe le décide pour cette réservation (`waive: true`). La ligne reste pour la trace. La capacité est contrôlée.

## Caution

`depositAmount` de la ressource : une ligne `DEPOSIT` ajoutée une fois, juste avant la première facture de la réservation. Elle est non taxée et entre au registre des cautions à l'émission de la facture. L'équipe la rend ou la retient par le registre des cautions, comme pour un abonnement.

## Dépassement et non-présentation

Politique de la ressource :

- `overrunGraceMinutes` : au départ (`check-out`), un dépassement au-delà de la tolérance est facturé, arrondi aux créneaux. Prix du moteur pour la plage dépassée (même titulaire), sinon au prorata de la réservation. Vide : jamais facturé.
- `noShowFee` : frais facturés quand la réservation passe en non-présentation (à la main ou par la tâche). Vide : aucun.

## Codes promo

`POST /bookings/{numéro}/promo-code` `{ code }` : le moteur des promotions des abonnements, avec la cible `RESOURCE` et le document `BOOKING`. Il applique conditions, budget, plafond et réservation du coupon, et refuse avec ses motifs. Une seule remise par réservation. Effet :

- avant facture : le montant baisse ;
- élément non facturé : il suit ;
- facture émise : avoir sur la facture.

## Lots de ressources

- `GET|POST /resource-bundles`, `GET|PATCH|DELETE /resource-bundles/{code}` : nom, description, `discountPercent`, ressources et quantités (deux au moins). Code `LOT-…`.
- `POST /resource-bundles/{code}/book` `{ memberIds, startedAt, endedAt, paymentMode, billingCompanyCode, confirmImmediately, sendEmail, notes, idempotencyKey }` (droit `BOOKING:CREATE`). Chaque ressource du lot est réservée sur le même créneau, par la réservation multi-ressources (tout ou rien, une facture par membre). La remise du lot baisse chaque réservation payante avant la facture, avec une ligne `DISCOUNT`.

## Heures incluses dans un abonnement

Déjà en place avant ce lot : une réservation couverte par un droit d'abonnement (heures, jours, réservations) le consomme à la confirmation, sans facture. Le solde est au portail (`GET /client/subscriptions/{numéro}/entitlements`) ; le dépassement passe par le module des dépassements.

**Limite restante** : pas de couverture partielle. Une réservation est soit couverte en entier par le droit, soit payée en entier ; le solde d'heures ne s'impute pas sur une partie de la réservation.

## Tests

`BookingR5cTest` :

- un service attend la facture puis part avec elle ;
- après la facture, un service est facturé à part ;
- un équipement compris n'est pas vendable ;
- la caution est posée une seule fois et facturée en caution ;
- un dépassement au-delà de la tolérance est facturé par créneaux ;
- un dépassement dans la tolérance est gratuit ;
- les frais de non-présentation sont facturés.
