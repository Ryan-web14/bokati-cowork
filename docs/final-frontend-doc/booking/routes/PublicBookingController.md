# PublicBookingController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [booking](../README.md) · 9 route(s).

## Routes

<a id="r-post-sni-api-v1-public-bookings-check-in-scan"></a>
### POST /sni/api/v1/public/bookings/check-in/scan

- Accès : public, sans jeton
- Réponse : `page HTML (vue serveur)`
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/PublicBookingController.java`, méthode `confirmScanCheckIn`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `checkInToken` | requête | `String` | oui |  |

<a id="r-get-sni-api-v1-public-bookings-check-in-scan-checkintoken"></a>
### GET /sni/api/v1/public/bookings/check-in/scan/{checkInToken}

- Accès : public, sans jeton
- Réponse : `page HTML (vue serveur)`
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/PublicBookingController.java`, méthode `scanCheckIn`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `checkInToken` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-public-bookings-check-in-scanner-setup"></a>
### GET /sni/api/v1/public/bookings/check-in/scanner-setup

- Accès : public, sans jeton
- Réponse : `page HTML (vue serveur)`
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/PublicBookingController.java`, méthode `scannerSetupPage`

<a id="r-post-sni-api-v1-public-bookings-check-in-scanner-setup"></a>
### POST /sni/api/v1/public/bookings/check-in/scanner-setup

- Accès : public, sans jeton
- Réponse : `page HTML (vue serveur)`
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/PublicBookingController.java`, méthode `scannerSetup`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `setupKey` | requête | `String` | oui |  |

<a id="r-post-sni-api-v1-public-bookings-check-in-scanner-verify"></a>
### POST /sni/api/v1/public/bookings/check-in/scanner-verify

- Accès : public, sans jeton
- Réponse : `page HTML (vue serveur)`
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/PublicBookingController.java`, méthode `scannerVerify`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `checkInToken` | requête | `String` | oui |  |
| `adminCode` | requête | `String` | oui |  |

<a id="r-post-sni-api-v1-public-bookings-check-in-self"></a>
### POST /sni/api/v1/public/bookings/check-in/self

- Accès : public, sans jeton
- Renvoie : `application/json`
- Réponse : `Map<String, Object>`
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/PublicBookingController.java`, méthode `clientSelfCheckIn`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `token` | requête | `String` | oui |  |
| `lat` | requête | `Double` |  |  |
| `lng` | requête | `Double` |  |  |

<a id="r-get-sni-api-v1-public-bookings-csat-bookingnumber"></a>
### GET /sni/api/v1/public/bookings/csat/{bookingNumber}

- Accès : public, sans jeton
- Réponse : `page HTML (vue serveur)`
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/PublicBookingController.java`, méthode `recordCsat`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `bookingNumber` | chemin | `String` | oui |  |
| `score` | requête | `int` |  | 0 |

<a id="r-get-sni-api-v1-public-bookings-bookingnumber"></a>
### GET /sni/api/v1/public/bookings/{bookingNumber}

- Accès : public, sans jeton
- Réponse : `page HTML (vue serveur)`
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/PublicBookingController.java`, méthode `view`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `bookingNumber` | chemin | `String` | oui |  |
| `token` | requête | `String` | oui |  |

<a id="r-get-sni-api-v1-public-bookings-bookingnumber-confirmation-pdf"></a>
### GET /sni/api/v1/public/bookings/{bookingNumber}/confirmation.pdf

- Accès : public, sans jeton
- Réponse : `page HTML (vue serveur)`
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/PublicBookingController.java`, méthode `confirmationPdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `bookingNumber` | chemin | `String` | oui |  |
| `token` | requête | `String` | oui |  |
