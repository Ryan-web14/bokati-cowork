# booking : routes

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

51 routes. Le fonctionnement et les parcours sont dans [README.md](README.md).

| Verbe | Chemin | Accès | Détail |
| --- | --- | --- | --- |
| GET | `/sni/api/v1/bookings` | BOOKING:READ | [BookingController](routes/BookingController.md#r-get-sni-api-v1-bookings) |
| POST | `/sni/api/v1/bookings` | BOOKING:CREATE | [BookingController](routes/BookingController.md#r-post-sni-api-v1-bookings) |
| GET | `/sni/api/v1/bookings/cancellation-policy` | BOOKING:CANCEL | [BookingController](routes/BookingController.md#r-get-sni-api-v1-bookings-cancellation-policy) |
| POST | `/sni/api/v1/bookings/check-availability` | BOOKING:CREATE | [BookingController](routes/BookingController.md#r-post-sni-api-v1-bookings-check-availability) |
| PATCH | `/sni/api/v1/bookings/check-in/qr/{checkInToken}` | BOOKING:CHECKIN | [BookingController](routes/BookingController.md#r-patch-sni-api-v1-bookings-check-in-qr-checkintoken) |
| POST | `/sni/api/v1/bookings/holds` | BOOKING:CREATE | [BookingController](routes/BookingController.md#r-post-sni-api-v1-bookings-holds) |
| POST | `/sni/api/v1/bookings/holds/expire-due` | BOOKING:CREATE | [BookingController](routes/BookingController.md#r-post-sni-api-v1-bookings-holds-expire-due) |
| PATCH | `/sni/api/v1/bookings/holds/{holdNumber}/release` | BOOKING:UPDATE | [BookingController](routes/BookingController.md#r-patch-sni-api-v1-bookings-holds-holdnumber-release) |
| GET | `/sni/api/v1/bookings/metrics/overview` | BOOKING:READ | [BookingController](routes/BookingController.md#r-get-sni-api-v1-bookings-metrics-overview) |
| POST | `/sni/api/v1/bookings/notifications/dispatch-due` | BOOKING:CREATE | [BookingController](routes/BookingController.md#r-post-sni-api-v1-bookings-notifications-dispatch-due) |
| GET | `/sni/api/v1/bookings/policies` | BOOKING:READ | [BookingController](routes/BookingController.md#r-get-sni-api-v1-bookings-policies) |
| POST | `/sni/api/v1/bookings/policies` | BOOKING:CREATE | [BookingController](routes/BookingController.md#r-post-sni-api-v1-bookings-policies) |
| GET | `/sni/api/v1/bookings/quota-overrides` | BOOKING:READ | [BookingController](routes/BookingController.md#r-get-sni-api-v1-bookings-quota-overrides) |
| POST | `/sni/api/v1/bookings/quota-overrides` | BOOKING:CREATE | [BookingController](routes/BookingController.md#r-post-sni-api-v1-bookings-quota-overrides) |
| POST | `/sni/api/v1/bookings/recurring` | BOOKING:CREATE | [BookingController](routes/BookingController.md#r-post-sni-api-v1-bookings-recurring) |
| POST | `/sni/api/v1/bookings/repair` | BOOKING:CREATE | [BookingController](routes/BookingController.md#r-post-sni-api-v1-bookings-repair) |
| GET | `/sni/api/v1/bookings/search` | BOOKING:READ | [BookingController](routes/BookingController.md#r-get-sni-api-v1-bookings-search) |
| POST | `/sni/api/v1/bookings/suggestions` | BOOKING:CREATE | [BookingController](routes/BookingController.md#r-post-sni-api-v1-bookings-suggestions) |
| GET | `/sni/api/v1/bookings/{bookingNumber}` | BOOKING:READ | [BookingController](routes/BookingController.md#r-get-sni-api-v1-bookings-bookingnumber) |
| PATCH | `/sni/api/v1/bookings/{bookingNumber}/approve` | BOOKING:UPDATE | [BookingController](routes/BookingController.md#r-patch-sni-api-v1-bookings-bookingnumber-approve) |
| PATCH | `/sni/api/v1/bookings/{bookingNumber}/cancel` | BOOKING:CANCEL | [BookingController](routes/BookingController.md#r-patch-sni-api-v1-bookings-bookingnumber-cancel) |
| PATCH | `/sni/api/v1/bookings/{bookingNumber}/change-resource` | BOOKING:UPDATE | [BookingController](routes/BookingController.md#r-patch-sni-api-v1-bookings-bookingnumber-change-resource) |
| PATCH | `/sni/api/v1/bookings/{bookingNumber}/check-in` | BOOKING:CHECKIN | [BookingController](routes/BookingController.md#r-patch-sni-api-v1-bookings-bookingnumber-check-in) |
| PATCH | `/sni/api/v1/bookings/{bookingNumber}/check-out` | BOOKING:CHECKIN | [BookingController](routes/BookingController.md#r-patch-sni-api-v1-bookings-bookingnumber-check-out) |
| PATCH | `/sni/api/v1/bookings/{bookingNumber}/complete` | BOOKING:UPDATE | [BookingController](routes/BookingController.md#r-patch-sni-api-v1-bookings-bookingnumber-complete) |
| PATCH | `/sni/api/v1/bookings/{bookingNumber}/confirm` | BOOKING:UPDATE | [BookingController](routes/BookingController.md#r-patch-sni-api-v1-bookings-bookingnumber-confirm) |
| PATCH | `/sni/api/v1/bookings/{bookingNumber}/early-check-in` | BOOKING:UPDATE | [BookingController](routes/BookingController.md#r-patch-sni-api-v1-bookings-bookingnumber-early-check-in) |
| GET | `/sni/api/v1/bookings/{bookingNumber}/events` | BOOKING:READ | [BookingController](routes/BookingController.md#r-get-sni-api-v1-bookings-bookingnumber-events) |
| GET | `/sni/api/v1/bookings/{bookingNumber}/history` | BOOKING:READ | [BookingController](routes/BookingController.md#r-get-sni-api-v1-bookings-bookingnumber-history) |
| PATCH | `/sni/api/v1/bookings/{bookingNumber}/no-show` | BOOKING:CANCEL | [BookingController](routes/BookingController.md#r-patch-sni-api-v1-bookings-bookingnumber-no-show) |
| POST | `/sni/api/v1/bookings/{bookingNumber}/notifications/reminder` | BOOKING:CREATE | [BookingController](routes/BookingController.md#r-post-sni-api-v1-bookings-bookingnumber-notifications-reminder) |
| POST | `/sni/api/v1/bookings/{bookingNumber}/participants` | BOOKING:CREATE | [BookingController](routes/BookingController.md#r-post-sni-api-v1-bookings-bookingnumber-participants) |
| DELETE | `/sni/api/v1/bookings/{bookingNumber}/participants/{participantId}` | BOOKING:UPDATE | [BookingController](routes/BookingController.md#r-delete-sni-api-v1-bookings-bookingnumber-participants-participantid) |
| PATCH | `/sni/api/v1/bookings/{bookingNumber}/reject` | BOOKING:CANCEL | [BookingController](routes/BookingController.md#r-patch-sni-api-v1-bookings-bookingnumber-reject) |
| PATCH | `/sni/api/v1/bookings/{bookingNumber}/reschedule` | BOOKING:UPDATE | [BookingController](routes/BookingController.md#r-patch-sni-api-v1-bookings-bookingnumber-reschedule) |
| PATCH | `/sni/api/v1/bookings/{bookingNumber}/start` | BOOKING:UPDATE | [BookingController](routes/BookingController.md#r-patch-sni-api-v1-bookings-bookingnumber-start) |
| PATCH | `/sni/api/v1/bookings/{bookingNumber}/transfer` | BOOKING:UPDATE | [BookingController](routes/BookingController.md#r-patch-sni-api-v1-bookings-bookingnumber-transfer) |
| GET | `/sni/api/v1/bookings/waitlist` | BOOKING:READ | [BookingWaitlistController](routes/BookingWaitlistController.md#r-get-sni-api-v1-bookings-waitlist) |
| POST | `/sni/api/v1/bookings/waitlist` | BOOKING:CREATE | [BookingWaitlistController](routes/BookingWaitlistController.md#r-post-sni-api-v1-bookings-waitlist) |
| POST | `/sni/api/v1/bookings/waitlist/expire-offers` | BOOKING:CREATE | [BookingWaitlistController](routes/BookingWaitlistController.md#r-post-sni-api-v1-bookings-waitlist-expire-offers) |
| POST | `/sni/api/v1/bookings/waitlist/promote-available` | BOOKING:CREATE | [BookingWaitlistController](routes/BookingWaitlistController.md#r-post-sni-api-v1-bookings-waitlist-promote-available) |
| PATCH | `/sni/api/v1/bookings/waitlist/{id}/cancel` | BOOKING:CANCEL | [BookingWaitlistController](routes/BookingWaitlistController.md#r-patch-sni-api-v1-bookings-waitlist-id-cancel) |
| POST | `/sni/api/v1/public/bookings/check-in/scan` | public | [PublicBookingController](routes/PublicBookingController.md#r-post-sni-api-v1-public-bookings-check-in-scan) |
| GET | `/sni/api/v1/public/bookings/check-in/scan/{checkInToken}` | public | [PublicBookingController](routes/PublicBookingController.md#r-get-sni-api-v1-public-bookings-check-in-scan-checkintoken) |
| GET | `/sni/api/v1/public/bookings/check-in/scanner-setup` | public | [PublicBookingController](routes/PublicBookingController.md#r-get-sni-api-v1-public-bookings-check-in-scanner-setup) |
| POST | `/sni/api/v1/public/bookings/check-in/scanner-setup` | public | [PublicBookingController](routes/PublicBookingController.md#r-post-sni-api-v1-public-bookings-check-in-scanner-setup) |
| POST | `/sni/api/v1/public/bookings/check-in/scanner-verify` | public | [PublicBookingController](routes/PublicBookingController.md#r-post-sni-api-v1-public-bookings-check-in-scanner-verify) |
| POST | `/sni/api/v1/public/bookings/check-in/self` | public | [PublicBookingController](routes/PublicBookingController.md#r-post-sni-api-v1-public-bookings-check-in-self) |
| GET | `/sni/api/v1/public/bookings/csat/{bookingNumber}` | public | [PublicBookingController](routes/PublicBookingController.md#r-get-sni-api-v1-public-bookings-csat-bookingnumber) |
| GET | `/sni/api/v1/public/bookings/{bookingNumber}` | public | [PublicBookingController](routes/PublicBookingController.md#r-get-sni-api-v1-public-bookings-bookingnumber) |
| GET | `/sni/api/v1/public/bookings/{bookingNumber}/confirmation.pdf` | public | [PublicBookingController](routes/PublicBookingController.md#r-get-sni-api-v1-public-bookings-bookingnumber-confirmation-pdf) |
