teamsHHE # Check-in & Booking Modification — Frontend Documentation

## 1. Client Self-Check-in (Confirmation Page Button)

The confirmation page (`GET /sni/api/v1/public/bookings/{bookingNumber}?token=...`) now renders a **"S'enregistrer (check-in)"** button. The button is wired in the page's JS and does the following automatically:

1. Requests the browser's geolocation (if the server has geolocation configured)
2. POSTs to `/sni/api/v1/public/bookings/check-in/self` with the booking token + coordinates
3. Shows an inline success or error message — no page reload

### Conditions for success
| Condition | Controlled by |
|---|---|
| Date/time: current time must be ≥ booking start − 15 min | Server (configurable via `CHECKIN_EARLY_WINDOW_MINUTES`) |
| Date/time: current time must be < booking end | Server |
| Geolocation: within configured radius of the coworking | Server (configurable via `CHECKIN_LOCATION_*`) |
| Booking status: must be `CONFIRMED` or `IN_PROGRESS` | Server |

### Error messages displayed to the client
| Server error code | Message shown |
|---|---|
| `time_invalid` | "Le check-in n'est pas encore disponible. Revenez 15 minutes avant votre réservation." |
| `location_invalid` | "Vous devez être présent dans l'espace pour effectuer le check-in." |
| `location_required` | "Veuillez autoriser la géolocalisation pour effectuer le check-in." |
| `checkin_failed` | Server message (booking already checked in, invalid token, etc.) |
| Network error | "Erreur réseau. Veuillez réessayer ou contacter l'accueil." |

### API Reference

**`POST /sni/api/v1/public/bookings/check-in/self`**

```
Content-Type: application/x-www-form-urlencoded

token=XXXX-XXXX&lat=4.3947&lng=18.5582
```

**Success `200`:**
```json
{ "success": true, "bookingNumber": "BKG-000", "resourceName": "Salle A", "status": "IN_PROGRESS" }
```

**Error `400`/`403`:**
```json
{ "success": false, "error": "time_invalid", "message": "Le check-in est disponible à partir de 15 minutes avant le début de la réservation..." }
```

---

## 2. QR Code Scanner — Admin Terminal Setup

The QR code printed in confirmation emails and PDFs points to:
```
{API_BASE}/sni/api/v1/public/bookings/check-in/scan/{checkInToken}
```

This URL now requires the device to be configured as an authorized scanner terminal.

### Setting up the scanner tablet/device

1. On the dedicated scanner device (entrance tablet), navigate to:
   ```
   {API_BASE}/sni/api/v1/public/bookings/check-in/scanner-setup
   ```
2. Enter the scanner key (provided by the system administrator via `CHECKIN_SCANNER_KEY`)
3. Click **"Configurer ce terminal"**
4. On success, the device is now authorized for 1 year — a `bokati_scanner` cookie is set

Once configured, scanning any booking QR code on that device will automatically perform check-in and display the result page.

### What happens when an unauthorized device scans the QR

Any phone that scans the QR code and does NOT have the scanner cookie will see the **"Accès restreint"** page (`booking/checkin-admin-code`) with:

- The scanned token displayed for reference
- A form to enter the admin code (same value as the scanner key)
- Entering the correct code: registers the device as a scanner + performs the check-in immediately

This is the fallback path if the main scanner tablet is unavailable.

---

## 3. Admin Portal — New Booking Management Endpoints

All endpoints below require a valid JWT (`Authorization: Bearer {token}`) with admin privileges.

### 3a. Early Check-in

Forces a check-in regardless of current time (useful before the client arrives).

```
PATCH /sni/api/v1/bookings/{bookingNumber}/early-check-in
```

**Body (optional):**
```json
{ "actor": "reception", "note": "Client arrivé en avance", "sendEmail": false }
```

**Response:** `BookingResponse` with `status: "IN_PROGRESS"` and `checkedInAt` populated.

---

### 3b. Transfer Booking

Reassigns the booking to a different member or customer.

```
PATCH /sni/api/v1/bookings/{bookingNumber}/transfer
```

**Body:**
```json
{
  "newOwnerType": "MEMBER",
  "newOwnerCode": "MBR-0099",
  "newContactName": "Marie Martin",
  "newContactEmail": "marie@example.com",
  "newContactPhone": "+24200000001",
  "actor": "admin",
  "note": "Transfert demandé",
  "sendEmail": true
}
```

Fields:
- `newOwnerType` — `MEMBER`, `CUSTOMER`, or other `SubscriberType` values (required)
- `newOwnerCode` — the target owner's identifier (required)
- `newContactName/Email/Phone` — optional, updates the booking contact info
- `sendEmail: true` — sends a booking confirmation email to the new contact email

**Works on:** any status except `COMPLETED` and `CANCELLED`.

---

### 3c. Reschedule Booking

Moves the booking to a different time slot on the same or different day.

```
PATCH /sni/api/v1/bookings/{bookingNumber}/reschedule
```

**Body:**
```json
{
  "newStartedAt": "2026-06-22T14:00:00",
  "newEndedAt": "2026-06-22T16:00:00",
  "actor": "admin",
  "note": "Report à la demande du client",
  "sendEmail": true
}
```

**Works on:** `CONFIRMED` and `PENDING_APPROVAL` bookings.

**Errors:**
- `409` if the new time slot conflicts with another booking on the same resource
- `409` if the resource is inactive or booking-disabled
- `400` if `newEndedAt` ≤ `newStartedAt`

**Note:** If `sendEmail: true`, a booking confirmation email with the updated times is sent to the contact email. The email subject and content are the same as a standard booking confirmation.

---

### 3d. Change Resource

Moves the booking to a different resource at the same date/time.

```
PATCH /sni/api/v1/bookings/{bookingNumber}/change-resource
```

**Body:**
```json
{
  "newResourceCode": "SALLE-B",
  "actor": "admin",
  "note": "Salle A indisponible",
  "sendEmail": true
}
```

**Works on:** `CONFIRMED` bookings only.

**Errors:**
- `404` if the resource code does not exist
- `409` if the new resource is already booked at that time
- `409` if the resource is inactive or booking-disabled

**Note:** Pricing is not recalculated. The booking amount stays the same regardless of the new resource's rate. Handle billing adjustments separately if needed.

---

## 4. Booking Event Log — New Event Types

The `GET /sni/api/v1/bookings/{bookingNumber}/events` endpoint will now return the following new event types:

| `eventType` | When |
|---|---|
| `BOOKING_RESCHEDULED` | After a successful reschedule |
| `BOOKING_TRANSFERRED` | After a successful transfer |
| `BOOKING_RESOURCE_CHANGED` | After a successful resource change |

Use these to display change history in the admin booking detail view.

---

## 5. Response Shape Reference

All modification endpoints return the standard `BookingResponse`:

```json
{
  "bookingNumber": "BKG-...",
  "resourceCode": "SALLE-A",
  "resourceName": "Salle de réunion A",
  "status": "CONFIRMED",
  "startedAt": "2026-06-22T14:00:00",
  "endedAt": "2026-06-22T16:00:00",
  "durationMinutes": 120,
  "ownerType": "MEMBER",
  "ownerCode": "MBR-0042",
  "contactName": "Alice Dupont",
  "contactEmail": "alice@example.com",
  "checkedInAt": null,
  "checkedOutAt": null,
  ...
}
```

---

## 6. Quick Reference Table

| Action | Method | URL | Auth |
|---|---|---|---|
| Client self-check-in | `POST` | `/sni/api/v1/public/bookings/check-in/self` | None |
| QR scan (scanner device) | `GET` | `/sni/api/v1/public/bookings/check-in/scan/{token}` | Scanner cookie |
| Admin code verify + check-in | `POST` | `/sni/api/v1/public/bookings/check-in/scanner-verify` | None |
| Register scanner device | `GET/POST` | `/sni/api/v1/public/bookings/check-in/scanner-setup` | None |
| Admin check-in | `PATCH` | `/sni/api/v1/bookings/{num}/check-in` | JWT |
| Admin early check-in | `PATCH` | `/sni/api/v1/bookings/{num}/early-check-in` | JWT |
| Admin check-in by QR token | `PATCH` | `/sni/api/v1/bookings/check-in/qr/{token}` | JWT |
| Transfer booking | `PATCH` | `/sni/api/v1/bookings/{num}/transfer` | JWT |
| Reschedule booking | `PATCH` | `/sni/api/v1/bookings/{num}/reschedule` | JWT |
| Change resource | `PATCH` | `/sni/api/v1/bookings/{num}/change-resource` | JWT |
