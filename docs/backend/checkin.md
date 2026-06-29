  # Check-in & Booking Modification — Backend Documentation

## 1. Configuration

Add the following environment variables (or edit `application.yml` directly in dev):

```yaml
bokati:
  checkin:
    scanner-key: ${CHECKIN_SCANNER_KEY:}         # Secret key for scanner device registration
    early-window-minutes: ${CHECKIN_EARLY_WINDOW_MINUTES:15}  # Minutes before start allowed for client check-in
    location:
      latitude: ${CHECKIN_LOCATION_LAT:0}        # Coworking space latitude (0 = disabled)
      longitude: ${CHECKIN_LOCATION_LNG:0}       # Coworking space longitude (0 = disabled)
      radius-meters: ${CHECKIN_LOCATION_RADIUS:200}  # Geofence radius in meters
```

**Env vars to set in production:**

| Variable | Example | Purpose |
|---|---|---|
| `CHECKIN_SCANNER_KEY` | `a8f3d2c1-9b4e-...` | Secret for authorizing scanner devices |
| `CHECKIN_EARLY_WINDOW_MINUTES` | `15` | How early clients can check in |
| `CHECKIN_LOCATION_LAT` | `4.3947` | Latitude of the coworking space |
| `CHECKIN_LOCATION_LNG` | `18.5582` | Longitude of the coworking space |
| `CHECKIN_LOCATION_RADIUS` | `200` | Geofence radius in meters |

If `CHECKIN_SCANNER_KEY` is empty, the scanner cookie mechanism is disabled (scan URL is open).  
If `CHECKIN_LOCATION_LAT`/`LNG` are both `0`, geolocation validation is skipped.

---

## 2. Check-in Flow Overview

### 2a. Client self-check-in (from email / confirmation page)

**Endpoint:** `POST /sni/api/v1/public/bookings/check-in/self`  
**Auth:** None (public)  
**Params (form-encoded):**

| Param | Type | Required | Description |
|---|---|---|---|
| `token` | String | Yes | `checkInToken` from the booking |
| `lat` | Double | Conditional | Client latitude (required if geolocation configured) |
| `lng` | Double | Conditional | Client longitude (required if geolocation configured) |

**Validations applied (in order):**
1. If location configured and `lat`/`lng` absent → `400 location_required`
2. If `lat`/`lng` provided and outside geofence → `403 location_invalid`
3. Service validates time window via `assertActivationAllowed`:
   - Current time must be ≥ `startedAt - earlyWindowMinutes`
   - Current time must be < `endedAt`
   - Violation → `403 time_invalid`
4. Booking must be `CONFIRMED` or `IN_PROGRESS` → otherwise `409`

**Success response `200`:**
```json
{ "success": true, "bookingNumber": "BKG-...", "resourceName": "Salle A", "status": "IN_PROGRESS" }
```

**Error responses:**
```json
{ "success": false, "error": "location_required|location_invalid|time_invalid|checkin_failed", "message": "..." }
```

---

### 2b. Scanner device check-in (QR code at entrance)

**Setup a scanner terminal:**
1. Visit `GET /sni/api/v1/public/bookings/check-in/scanner-setup` on the tablet
2. Enter the `CHECKIN_SCANNER_KEY` value
3. `POST /sni/api/v1/public/bookings/check-in/scanner-setup` validates the key and sets a `bokati_scanner` cookie (1-year, HttpOnly)
4. The tablet is now an authorized scanner

**QR scan flow:**  
`GET /sni/api/v1/public/bookings/check-in/scan/{checkInToken}`

- If `bokati_scanner` cookie is valid → check-in performed immediately, shows `booking/checkin-result` (no time or geo restriction)
- If cookie absent or invalid → shows `booking/checkin-admin-code` (admin code entry form)

**Admin code override (fallback when scanner unavailable):**  
`POST /sni/api/v1/public/bookings/check-in/scanner-verify`

| Param | Description |
|---|---|
| `checkInToken` | The token from the QR scan |
| `adminCode` | The `CHECKIN_SCANNER_KEY` value |

On success: sets the scanner cookie + performs check-in. On failure: shows error in the same form.

---

### 2c. Admin check-in (authenticated, no restrictions)

These endpoints require authentication and bypass all time/geolocation restrictions because `isCurrentUserAdmin()` returns `true`.

| Endpoint | Description |
|---|---|
| `PATCH /sni/api/v1/bookings/{bookingNumber}/check-in` | Standard check-in (time-restricted for non-admins, unrestricted for admins) |
| `PATCH /sni/api/v1/bookings/check-in/qr/{checkInToken}` | Check-in by QR token (same rules) |
| `PATCH /sni/api/v1/bookings/{bookingNumber}/early-check-in` | **Explicit early check-in** — always bypasses time window, admin only |

**`early-check-in` request body** (`BookingCheckRequest`, optional):
```json
{ "actor": "receptionist@elleaose.com", "note": "Client arrivé tôt", "sendEmail": false }
```

---

## 3. Admin Booking Modification Endpoints

All endpoints below require authentication. No Flyway migration needed (existing columns used).

### 3a. Transfer — `PATCH /sni/api/v1/bookings/{bookingNumber}/transfer`

Reassigns a booking to a different owner. Works on any status except `COMPLETED` or `CANCELLED`.

**Request body (`BookingTransferRequest`):**
```json
{
  "newOwnerType": "MEMBER",
  "newOwnerCode": "MBR-0042",
  "newContactName": "Alice Dupont",
  "newContactEmail": "alice@example.com",
  "newContactPhone": "+24200000000",
  "actor": "admin",
  "note": "Transfert à la demande du client",
  "sendEmail": true
}
```

- `newOwnerType` and `newOwnerCode` are required.
- `newContactName/Email/Phone` are optional — only updated if provided.
- When `sendEmail: true`, sends a `BOOKING_CONFIRMED` email to the new contact email.
- Writes a `BOOKING_TRANSFERRED` event to the audit log.

---

### 3b. Reschedule — `PATCH /sni/api/v1/bookings/{bookingNumber}/reschedule`

Changes the start/end time of a `CONFIRMED` or `PENDING_APPROVAL` booking.

**Request body (`BookingRescheduleRequest`):**
```json
{
  "newStartedAt": "2026-06-20T09:00:00",
  "newEndedAt": "2026-06-20T12:00:00",
  "actor": "admin",
  "note": "Report demandé par le client",
  "sendEmail": true
}
```

**Logic:**
1. Validates `newEndedAt > newStartedAt`
2. Checks the resource is still active/booking-enabled
3. Checks for conflicts on the new slot (excluding the current booking)
4. Releases the old resource availability slot
5. Reserves the new resource availability slot
6. Updates `startedAt`, `endedAt`, `durationMinutes` on the booking
7. Writes history + `BOOKING_RESCHEDULED` event
8. Sends `BOOKING_CONFIRMED` email with updated times if `sendEmail: true`

**Note:** Minimum booking notice (`ResourcePolicy.minBookingNoticeMinutes`) is **not** enforced for admin reschedule. Slot alignment (30-minute boundaries) is also not validated — admin has full freedom.

**Note on entitlements:** If the booking uses passes or subscriptions and the duration changes significantly, the entitlement quantity is NOT automatically adjusted. Handle billing adjustments manually if needed.

---

### 3c. Change Resource — `PATCH /sni/api/v1/bookings/{bookingNumber}/change-resource`

Moves a `CONFIRMED` booking to a different resource at the same time slot.

**Request body (`BookingChangeResourceRequest`):**
```json
{
  "newResourceCode": "SALLE-B",
  "actor": "admin",
  "note": "Salle A indisponible suite à un problème technique",
  "sendEmail": true
}
```

**Logic:**
1. Validates new resource is active and booking-enabled
2. Checks for conflicts on the new resource at the same slot
3. Releases the old resource availability
4. Updates the booking's resource reference
5. Reserves the new resource availability
6. Writes `BOOKING_RESOURCE_CHANGED` event
7. Sends `BOOKING_CONFIRMED` email with updated resource if `sendEmail: true`

**Note:** Pricing is NOT recalculated automatically. If the new resource has a different rate, update the billing item manually.

---

## 4. New Event Types

Three new values were added to `BookingEventType`:

| Type | When written |
|---|---|
| `BOOKING_RESCHEDULED` | After a successful reschedule |
| `BOOKING_TRANSFERRED` | After a successful transfer |
| `BOOKING_RESOURCE_CHANGED` | After a successful resource change |

These appear in `GET /sni/api/v1/bookings/{bookingNumber}/events` and in the audit log.

---

## 5. Modified Behaviour: `assertActivationAllowed`

Previously: non-admin check-in blocked if `now < startedAt` (check-in window opened exactly at start time).

**Now:** window opens `earlyWindowMinutes` (default 15) before `startedAt`.

Formula: `now ∈ [startedAt - earlyWindowMinutes, endedAt)`

Admins are still unrestricted (bypass the check entirely).

---

## 6. Security Notes

- The `bokati_scanner` cookie is `HttpOnly`. In production, configure `Secure` and `SameSite=Strict` via a `CookieSerializer` bean or reverse-proxy settings.
- The `CHECKIN_SCANNER_KEY` must be a strong random string (UUID v4 or similar). Rotate it if compromised — all existing scanner cookies immediately become invalid.
- The `/sni/api/v1/public/**` pattern is `permitAll` in `SecurityConfig`. No JWT is required for any public check-in endpoints.
