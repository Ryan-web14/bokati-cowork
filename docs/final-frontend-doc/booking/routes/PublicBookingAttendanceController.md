# PublicBookingAttendanceController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [booking](../README.md) · 2 route(s).

## Routes

<a id="r-get-sni-api-v1-public-bookings-bookingnumber-attendance"></a>
### GET /sni/api/v1/public/bookings/{bookingNumber}/attendance

- Accès : public, sans jeton
- Réponse : `ModelAndView`
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/PublicBookingAttendanceController.java`, méthode `answer`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `bookingNumber` | chemin | `String` | oui |  |
| `token` | requête | `String` |  |  |
| `answer` | requête | `String` |  | COMING |

<a id="r-post-sni-api-v1-public-bookings-bookingnumber-attendance-cancel"></a>
### POST /sni/api/v1/public/bookings/{bookingNumber}/attendance/cancel

- Accès : public, sans jeton
- Réponse : `ModelAndView`
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/PublicBookingAttendanceController.java`, méthode `cancel`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `bookingNumber` | chemin | `String` | oui |  |
| `token` | requête | `String` |  |  |
