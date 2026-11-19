# PublicBookingCalendarController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [booking](../README.md) · 2 route(s).

## Routes

<a id="r-get-sni-api-v1-public-bookings-calendar-token-ics"></a>
### GET /sni/api/v1/public/bookings/calendar/{token}.ics

- Accès : public, sans jeton
- Réponse : `String`
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/PublicBookingCalendarController.java`, méthode `calendar`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `token` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-public-bookings-csat-bookingnumber-comment"></a>
### POST /sni/api/v1/public/bookings/csat/{bookingNumber}/comment

- Accès : public, sans jeton
- Réponse : `ModelAndView`
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/PublicBookingCalendarController.java`, méthode `comment`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `bookingNumber` | chemin | `String` | oui |  |
| `token` | requête | `String` |  |  |
| `comment` | requête | `String` |  |  |
