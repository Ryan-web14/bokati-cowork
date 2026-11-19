# Lot B3 : rendez-vous sans paiement, réglé sur place

> Partie du [plan booking](README.md). Fait le 19/11/2026 sur `feature/booking`. Décisions D1 à D3. Migration V389 (prod V383). Demande 2.

## Rendez-vous réglé sur place

- `CreateBookingRequest.payOnSite: true` (équipe) ou `ClientCreateBookingRequest.payOnSite: true` (portail), en paiement direct seulement (`400 PAY_ON_SITE_DIRECT_ONLY` sinon). La réservation porte `payOnSite` (réponse comprise).
- Comme toute réservation en attente de paiement (lot B1) : créneau retenu, facture émise pour l'encaissement, échéance calculée selon la règle D1 (2 h, 30 min avant le début, 24 h si le créneau est loin, jour ouvré).
- **Rappel avant l'échéance** (D2) : courriel au client `bookings.payment-due.reminder-minutes` (30) avant l'échéance, une seule fois (`payment_reminder_sent_at`), par la tâche d'expiration (toutes les minutes). Événement `BOOKING_PAYMENT_REMINDER`. Vaut pour toute réservation en attente de paiement. WhatsApp : avec les rappels du lot B6 (modèle à soumettre à Meta).
- **À l'échéance** (D2) : la réservation passe `EXPIRED` (lot B1), le créneau est rendu, et **l'équipe est prévenue** par l'alerte en direct `BOOKING_EXPIRED` (module `BOOKING`, sévérité `WARNING`).

## Accès au portail (D3)

- Réglage global `bookings.pay-on-site.portal-enabled` (coupé par défaut).
- Option par client posée par l'équipe (membre, client, entreprise), qui l'emporte : `PUT /bookings/pay-on-site/clients/{ownerType}/{ownerCode}` `{ enabled, reason }` ; liste `GET /bookings/pay-on-site/clients` ; état d'un client `GET /bookings/pay-on-site/clients/{ownerType}/{ownerCode}` → `{ allowed, source, reason }` (`source` : `CLIENT_OPTION` ou `GLOBAL_SETTING`).
- Portail : `GET /client/bookings/pay-on-site` → même réponse pour le membre connecté ; une réservation `payOnSite` refusée donne `409 PAY_ON_SITE_NOT_ALLOWED`. L'équipe en pose toujours.
- **Retrait d'office** : après `bookings.pay-on-site.max-no-shows` (2) absences sur des rendez-vous réglés sur place en `bookings.pay-on-site.no-show-window-days` (90) jours, l'option du client passe à « fermé », `revokedAutomatically: true`, motif « Retiré d'office après N absences ». L'équipe peut la rouvrir.

## Accueil

`GET /bookings/pay-on-site/due?date=AAAA-MM-JJ` (aujourd'hui par défaut) : rendez-vous réglés sur place encore à encaisser qui commencent ce jour-là (en attente, confirmés ou en cours, sans règlement constaté). L'encaissement se fait à la caisse sur la facture de la réservation (`invoiceNumber` de la fiche) ; le paiement reçu confirme la réservation et la marque réglée.

## Réglages

| Clé | Défaut | Effet |
| --- | --- | --- |
| `bookings.pay-on-site.portal-enabled` | `false` | Portail ouvert à tous les clients sans option propre |
| `bookings.pay-on-site.max-no-shows` | 2 | Absences avant retrait d'office (0 : jamais) |
| `bookings.pay-on-site.no-show-window-days` | 90 | Période de décompte des absences |
| `bookings.payment-due.reminder-minutes` | 30 | Rappel avant l'échéance (0 : aucun) |

## Tests

`BookingB3Test` : option du client et réglage global, retrait d'office, rappel unique avant l'échéance.
