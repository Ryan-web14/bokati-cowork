# Lot B0 : sécurité et traçabilité

> Partie du [plan booking](README.md). Fait le 18/11/2026 sur `feature/booking`. Migration V381 (prod V375).

## Qui a fait (écart Bn-18)

- « Qui a fait » vient de la session pour toutes les actions de l'équipe et du membre : création, confirmation, approbation, refus, arrivée, départ, déplacement, changement de ressource, transfert, annulation, absence. La valeur `changedBy` ou `actor` de la requête ne sert plus que sans session (tâches du système, paiement reçu).
- Nouvelle colonne `createdBy` : l'utilisateur qui a créé la réservation. L'historique de création le porte (avant : vide).
- Nouvelle colonne `channel` : `PORTAL` (espace client) ou `TEAM` (back-office), déduite de l'appel. `PHONE`, `DESK`, `EMAIL` et `WHATSAPP` arrivent avec la prise de rendez-vous au téléphone (lot B5). Les deux champs rejoignent la réponse au lot B7.

## Pointage hors fenêtre (écart 1)

Toute l'équipe d'accueil (administrateurs, responsables, agents d'exploitation : les rôles de la dérogation de l'équipe) pointe hors fenêtre, plus seulement l'administration. Le membre hors fenêtre reçoit un `409` `CHECKIN_OUTSIDE_WINDOW` avec un message lisible (avant : `403` remplacé par « Access to this resource is forbidden »).

## Paiement reçu (écart 15)

La confirmation automatique après paiement enregistre l'acteur `SYSTEM` et le motif « Confirmation automatique suite au paiement … » dans le bon ordre (ils étaient inversés).

## Satisfaction (écart 8)

`GET /public/bookings/csat/{numéro}` exige le jeton de la réservation (`token`), que le lien du courriel porte désormais ; sans jeton valable, la note n'est pas enregistrée.

## Règles d'accès

- `PATCH /bookings/{numéro}/early-check-in` demande `BOOKING:CHECKIN` comme le pointage (écart Bn-11 ; avant : `BOOKING:UPDATE`).
- `GET /bookings/cancellation-policy` demande `BOOKING:READ` (écart Bn-21 ; avant : `BOOKING:CANCEL`).
- Limite de débit (écart 17) : seuls les envois (`POST`) vers `/public/bookings/check-in/scanner*` consomment le compteur des codes (3 par 10 minutes) ; la page d'installation (`GET`) passe par le compteur ordinaire du pointage.

## Photos (écart Bn-19)

Les photos de l'état des lieux de fin et des incidents doivent être des documents existants de la GED ; un membre ne joint que ses propres documents (`400` sinon).

## Non retenu

Écart Bn-20 (position du pointage autonome fournie par le téléphone) : risque faible, accepté. La position reste un indice ; le jeton du QR et la fenêtre de pointage restent les contrôles.

## Tests

`BookingB0Test` (acteur lu dans la session, photos d'un membre) ; `BookingServiceImplTest` adapté (refus hors fenêtre en `409`).
