# Portail Client — Phase 5 : Guide Frontend — Espaces & Réservations

Tous les endpoints requièrent `Authorization: Bearer <accessToken>` sauf mention contraire.

---

## Espaces réservables — `/client/resources`

Seuls les espaces **actifs, visibles sur le portail et ouverts à la réservation** sont retournés. Les espaces internes ou en maintenance sont masqués automatiquement.

### GET `/client/resources`

Parcourir les espaces de coworking disponibles.

**Paramètres de requête :**
| Paramètre | Type | Description |
|---|---|---|
| `type` | String | Filtrer par code de type (ex. : `MEETING_ROOM`, `DESK`, `OFFICE`) |
| `group` | String | Filtrer par code de groupe |
| `q` | String | Recherche par nom (correspondance partielle) |
| `capacity` | Integer | Capacité minimale |
| `page` | Integer | Numéro de page (défaut : 0) |
| `size` | Integer | Taille de page (défaut : 20) |

**Réponse — `200 OK` :**
```json
{
  "data": [
    {
      "code": "RES-MEETING-001",
      "name": "Salle Edison",
      "description": "Salle de réunion pour 8 personnes, équipée d'un vidéoprojecteur",
      "typeCode": "MEETING_ROOM",
      "typeName": "Salle de réunion",
      "groupCode": "PRIVATE_SPACES",
      "groupName": "Espaces Privés",
      "capacity": 8,
      "zone": "FLOOR_2",
      "locationLabel": "2ème étage, aile nord",
      "displayOrder": 1
    }
  ],
  "pageable": {
    "page": 0,
    "size": 20,
    "totalElements": 5,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

---

### GET `/client/resources/{code}`

Détail complet d'un espace avec équipements et tarification.

**Réponse — `200 OK` :**
```json
{
  "code": "RES-MEETING-001",
  "name": "Salle Edison",
  "description": "...",
  "typeCode": "MEETING_ROOM",
  "typeName": "Salle de réunion",
  "groupCode": "PRIVATE_SPACES",
  "groupName": "Espaces Privés",
  "capacity": 8,
  "zone": "FLOOR_2",
  "locationLabel": "2ème étage, aile nord",
  "displayOrder": 1,
  "minBookingDurationMinutes": 60,
  "maxBookingDurationMinutes": 480,
  "minBookingNoticeMinutes": 30,
  "cancellationNoticeMinutes": 120,
  "allowCancellation": true,
  "amenities": [
    { "code": "PROJECTOR", "name": "Vidéoprojecteur", "description": null, "active": true, "quantity": 1, "optional": false, "extraPrice": 0 },
    { "code": "WHITEBOARD", "name": "Tableau blanc", "description": null, "active": true, "quantity": 2, "optional": false, "extraPrice": 0 }
  ],
  "pricingRules": [
    { "id": 1, "resourceCode": "RES-MEETING-001", "bookingUnit": "HOUR", "price": 15000, "label": "Tarif horaire standard", "dayOfWeek": null, "startsAt": null, "endsAt": null, "adjustmentType": "FIXED_PRICE", "adjustmentValue": null, "validFrom": null, "validUntil": null, "lastMinuteMinutes": null, "priority": 1, "active": true }
  ]
}
```

**Explication des champs de politique :**
| Champ | Signification |
|---|---|
| `minBookingDurationMinutes` | Durée minimale de réservation (ex. : 60 = au moins 1 heure) |
| `maxBookingDurationMinutes` | Durée maximale de réservation (ex. : 480 = max 8 heures) |
| `minBookingNoticeMinutes` | Délai minimum de réservation à l'avance (ex. : 30 = au moins 30 min avant) |
| `cancellationNoticeMinutes` | Délai minimum d'annulation pour un remboursement |
| `allowCancellation` | Si l'annulation est autorisée |

**Valeurs de `bookingUnit` (tarification) :** `HOUR`, `HALF_DAY`, `DAY`, `WEEK`, `MONTH`

---

### GET `/client/resources/{code}/calendar`

Retourne la disponibilité jour par jour sur une plage de dates (pour afficher un calendrier).

**Paramètres de requête :**
| Paramètre | Requis | Description |
|---|---|---|
| `from` | Oui | Date de début (ISO : `2026-06-14`) |
| `to` | Oui | Date de fin (ISO : `2026-06-30`) |

**Réponse — `200 OK` :**
```json
{
  "resourceCode": "RES-MEETING-001",
  "resourceName": "Salle Edison",
  "fromDate": "2026-06-14",
  "toDate": "2026-06-30",
  "days": [
    { "date": "2026-06-14", "totalSlots": 8, "availableSlots": 6, "totalCapacity": 8, "remainingCapacity": 6, "status": "AVAILABLE" },
    { "date": "2026-06-15", "totalSlots": 0, "availableSlots": 0, "totalCapacity": 0, "remainingCapacity": 0, "status": "CLOSED" }
  ]
}
```

**Valeurs de `status` par jour :**
| Statut | Signification |
|---|---|
| `AVAILABLE` | Des créneaux sont disponibles |
| `PARTIAL` | Certains créneaux pris, d'autres libres |
| `FULL` | Tous les créneaux réservés |
| `CLOSED` | Aucune disponibilité configurée ce jour |

---

### GET `/client/resources/{code}/slots`

Retourne les fenêtres horaires disponibles pour une date/plage donnée.

**Paramètres de requête :**
| Paramètre | Requis | Défaut | Description |
|---|---|---|---|
| `startedAt` | Oui | — | Début de plage (ISO datetime : `2026-06-14T08:00:00`) |
| `endedAt` | Oui | — | Fin de plage (ISO datetime : `2026-06-14T18:00:00`) |
| `durationMinutes` | Non | `30` | Durée minimale de créneau |
| `quantity` | Non | `1` | Nombre de places nécessaires |

**Réponse — `200 OK` :** Tableau des fenêtres disponibles :
```json
[
  { "resourceCode": "RES-MEETING-001", "startedAt": "2026-06-14T08:00:00", "endedAt": "2026-06-14T10:00:00", "durationMinutes": 120, "remainingCapacity": 8, "slotCount": 4 },
  { "resourceCode": "RES-MEETING-001", "startedAt": "2026-06-14T14:00:00", "endedAt": "2026-06-14T18:00:00", "durationMinutes": 240, "remainingCapacity": 8, "slotCount": 8 }
]
```

Utiliser ce résultat pour afficher le sélecteur horaire et bloquer les fenêtres indisponibles.

---

### GET `/client/resources/{code}/quote`

Obtenir une estimation de prix avant de réserver.

**Paramètres de requête :**
| Paramètre | Requis | Description |
|---|---|---|
| `bookingUnit` | Oui | `HOUR`, `HALF_DAY`, `DAY`, `WEEK`, `MONTH` |
| `startedAt` | Oui | Début (ISO datetime) |
| `endedAt` | Oui | Fin (ISO datetime) |

**Réponse — `200 OK` :**
```json
{
  "resourceCode": "RES-MEETING-001",
  "bookingUnit": "HOUR",
  "startedAt": "2026-06-14T09:00:00",
  "endedAt": "2026-06-14T11:00:00",
  "basePrice": 15000,
  "finalPrice": 12000,
  "appliedRuleId": 3,
  "appliedRuleLabel": "Happy hour -20%",
  "adjustmentType": "PERCENT_DELTA",
  "adjustmentValue": -20
}
```

Si aucune règle tarifaire ne s'applique, `appliedRuleId` est null et `finalPrice == basePrice`.

---

## Réservations — `/client/bookings`

### GET `/client/bookings`

Liste les réservations du membre authentifié.

**Paramètres de requête :**
| Paramètre | Type | Description |
|---|---|---|
| `status` | String | Filtrer par statut (voir tableau ci-dessous) |
| `from` | LocalDateTime | Filtre : `startedAt >= from` (ISO : `2026-06-01T00:00:00`) |
| `to` | LocalDateTime | Filtre : `startedAt <= to` |
| `page` | Integer | Défaut : 0 |
| `size` | Integer | Défaut : 20 |

**Valeurs de `status` :**
| Statut | Signification |
|---|---|
| `DRAFT` | Pas encore confirmée |
| `PENDING_APPROVAL` | En attente d'approbation admin |
| `CONFIRMED` | Confirmée, à venir |
| `IN_PROGRESS` | En cours |
| `COMPLETED` | Terminée |
| `CANCELLED` | Annulée par le membre ou l'admin |
| `NO_SHOW` | Le membre ne s'est pas présenté |
| `EXPIRED` | Fenêtre de réservation passée sans action |

**Réponse — `200 OK` :**
```json
{
  "data": [
    {
      "bookingNumber": "BOOK-20260614-0001",
      "resourceCode": "RES-MEETING-001",
      "resourceName": "Salle Edison",
      "resourceTypeCode": "MEETING_ROOM",
      "resourceGroupCode": "PRIVATE_SPACES",
      "status": "CONFIRMED",
      "startedAt": "2026-06-20T09:00:00",
      "endedAt": "2026-06-20T11:00:00",
      "durationMinutes": 120,
      "quantity": 1,
      "bookingUnit": "HOUR",
      "paymentMode": "DIRECT",
      "totalAmount": 30000,
      "currency": "XAF",
      "confirmedAt": "2026-06-14T10:00:00Z",
      "cancelledAt": null,
      "completedAt": null,
      "createdAt": "2026-06-14T09:55:00Z"
    }
  ],
  "pageable": { ... }
}
```

---

### POST `/client/bookings`

Crée une réservation. Le membre authentifié est automatiquement défini comme propriétaire.

**Corps de la requête :**
```json
{
  "resourceCode": "RES-MEETING-001",
  "startedAt": "2026-06-20T09:00:00",
  "endedAt": "2026-06-20T11:00:00",
  "quantity": 1,
  "paymentMode": "DIRECT",
  "notes": "Réunion équipe projet",
  "idempotencyKey": "uuid-optionnel-pour-retry-securise",
  "participants": [
    { "name": "Jean Dupont", "email": "jean@example.com", "role": "GUEST" }
  ]
}
```

**Valeurs de `paymentMode` :**
| Valeur | Signification |
|---|---|
| `DIRECT` | Paiement à l'acte (facturé séparément) |
| `SUBSCRIPTION` | Déduit d'un droit d'abonnement actif |
| `PASS` | Déduit d'un crédit de pass journée |

**Réponse — `201 Created` :** `ClientBookingResponse` complet (voir GET /{bookingNumber} ci-dessous).

**Erreurs :**
| Statut | Signification |
|---|---|
| `400` | Champs requis manquants, dates invalides |
| `409` | Créneau plus disponible (conflit) |
| `409` | Requête en double (`idempotencyKey` déjà utilisée) |

**Flux UX :**
1. Afficher le détail de l'espace + estimation de prix
2. Le membre sélectionne un créneau depuis la vue calendrier/créneaux
3. Le membre confirme — POST vers `/bookings`
4. En cas de `201` → afficher la page de confirmation avec `bookingNumber` et `checkInToken`

---

### GET `/client/bookings/{bookingNumber}`

Obtenir les détails d'une réservation spécifique.

**Réponse — `200 OK` :**
```json
{
  "bookingNumber": "BOOK-20260614-0001",
  "resourceCode": "RES-MEETING-001",
  "resourceName": "Salle Edison",
  "resourceTypeCode": "MEETING_ROOM",
  "resourceGroupCode": "PRIVATE_SPACES",
  "status": "CONFIRMED",
  "startedAt": "2026-06-20T09:00:00",
  "endedAt": "2026-06-20T11:00:00",
  "durationMinutes": 120,
  "quantity": 1,
  "bookingUnit": "HOUR",
  "paymentMode": "DIRECT",
  "subscriptionNumber": null,
  "passNumber": null,
  "entitlementCode": null,
  "unitPrice": 15000,
  "subtotalAmount": 30000,
  "totalAmount": 30000,
  "currency": "XAF",
  "notes": "Réunion équipe projet",
  "checkInToken": "TKN-ABC123",
  "checkInQrValue": "...",
  "virtualMeetingUrl": null,
  "confirmedAt": "2026-06-14T10:00:00Z",
  "completedAt": null,
  "cancelledAt": null,
  "createdAt": "2026-06-14T09:55:00Z",
  "lines": [ ... ],
  "participants": [
    { "id": 1, "name": "Jean Dupont", "email": "jean@example.com", "role": "GUEST", "status": "INVITED" }
  ]
}
```

**Champs clés :**
- `checkInToken` — afficher sous forme de QR code ou code-barres pour le check-in sur place
- `checkInQrValue` — valeur QR code pré-formatée (si fournie par le serveur)
- `virtualMeetingUrl` — pour les salles virtuelles ou hybrides

---

### DELETE `/client/bookings/{bookingNumber}`

Annule une réservation. Autorisé uniquement si le statut est `CONFIRMED` ou `PENDING_APPROVAL`.

**Corps de la requête (optionnel) :**
```json
{ "reason": "Réunion annulée" }
```

**Réponse — `200 OK` :** Réservation mise à jour avec `status: "CANCELLED"`.

**Erreurs :**
| Statut | Signification |
|---|---|
| `400` | La réservation n'est pas dans un état annulable |
| `404` | Réservation introuvable (ou appartient à un autre membre) |

**Note UX :** Avant d'appeler cet endpoint, vérifier `allowCancellation` et `cancellationNoticeMinutes` de l'espace. Si la réservation commence dans moins de `cancellationNoticeMinutes`, avertir le membre qu'il pourrait ne pas être remboursé.

---

### POST `/client/bookings/availability`

Vérifie la disponibilité d'un créneau avant de créer une réservation. À utiliser avant d'afficher le bouton "Confirmer".

**Corps de la requête :**
```json
{
  "resourceCode": "RES-MEETING-001",
  "startedAt": "2026-06-20T09:00:00",
  "endedAt": "2026-06-20T11:00:00",
  "quantity": 1
}
```

**Réponse — `200 OK` :**
```json
{
  "resourceCode": "RES-MEETING-001",
  "resourceName": "Salle Edison",
  "startedAt": "2026-06-20T09:00:00",
  "endedAt": "2026-06-20T11:00:00",
  "durationMinutes": 120,
  "quantity": 1,
  "available": true,
  "remainingCapacity": 7,
  "bookingUnit": "HOUR",
  "unitPrice": 15000,
  "estimatedAmount": 30000,
  "currency": "XAF",
  "message": null
}
```

Si `available: false`, afficher le champ `message` à l'utilisateur (ex. : "Créneau complet").

---

## Flux UX recommandé : Réserver un espace

```
1. Parcourir les espaces    → GET /client/resources (filtres type/groupe/capacité)
2. Voir le détail           → GET /client/resources/{code}
3. Choisir une date         → GET /client/resources/{code}/calendar?from=...&to=...
4. Choisir un créneau       → GET /client/resources/{code}/slots?startedAt=...&endedAt=...
5. Estimer le prix          → GET /client/resources/{code}/quote?bookingUnit=HOUR&startedAt=...&endedAt=...
6. Vérifier la dispo        → POST /client/bookings/availability
7. Confirmer la réservation → POST /client/bookings
8. Voir la confirmation     → GET /client/bookings/{bookingNumber}
```

Les étapes 4 à 6 peuvent être parallélisées une fois que le membre a sélectionné un créneau.

---

## Référence des codes d'erreur

| Code | HTTP | Déclencheur |
|---|---|---|
| `RESOURCE_NOT_FOUND` | `404` | Espace non visible sur le portail ou non réservable |
| `BOOKING_NOT_FOUND` | `404` | Réservation introuvable ou appartient à un autre membre |
| `BOOKING_NOT_CANCELLABLE` | `400` | Statut de la réservation ni CONFIRMED ni PENDING_APPROVAL |
| `SLOT_UNAVAILABLE` | `409` | Conflit de réservation concurrent |
