# Portail Client — Phase 11 : Guide Frontend — Notifications, Profil & Sessions

Tous les endpoints requièrent `Authorization: Bearer <accessToken>`.

---

## Profil — `/client/profile`

### GET `/client/profile`

Retourne le profil du membre authentifié.

**Réponse — `200 OK` :**
```json
{
  "memberId": "MBR-202601-XXXX",
  "firstname": "Jean",
  "lastname": "Dupont",
  "fullName": "Jean Dupont",
  "email": "jean@example.com",
  "phone": "+242064000001",
  "whatsappPhone": "+242064000001",
  "status": "ACTIVE",
  "avatarUrl": null,
  "portalActivatedAt": "2026-01-15T09:00:00Z",
  "createdAt": "2026-01-15T09:00:00Z",
  "updatedAt": "2026-06-14T12:00:00Z"
}
```

> **Note :** `email` est affiché mais ne peut pas être modifié depuis ce endpoint (géré par l'administrateur). Ne pas inclure l'email dans le formulaire de modification de profil.

---

### PATCH `/client/profile`

Met à jour les informations du profil. Tous les champs sont optionnels.

**Corps de la requête :**
```json
{
  "firstname": "Jean",
  "lastname": "Dupont",
  "phone": "+242064000001",
  "whatsappPhone": "+242064000001"
}
```

**Contraintes :** `firstname`/`lastname` max 350 caractères, `phone`/`whatsappPhone` max 30 caractères.

**Réponse — `200 OK` :** `ClientProfileResponse` mis à jour.

**UX :** Mettre à jour l'état local depuis la réponse — ne pas refaire un GET.

---

### POST `/client/profile/change-password`

Initie un changement de mot de passe en envoyant un email de réinitialisation.

**Corps :** Aucun.

**Réponse — `204 No Content`**

> **Comportement :** Cette action envoie un email de réinitialisation de mot de passe à l'adresse du membre. Le membre clique sur le lien dans l'email pour définir son nouveau mot de passe. Il n'y a pas de saisie d'ancien/nouveau mot de passe directement dans le portail.

**Flux UX :**
1. Le membre clique sur "Changer mon mot de passe"
2. Boîte de confirmation : "Un email contenant un lien de réinitialisation sera envoyé à {email}"
3. À la confirmation → `POST /client/profile/change-password`
4. Afficher : "Email envoyé. Vérifiez votre boîte de réception."

---

## Notifications — `/client/notifications`

### GET `/client/notifications`

Retourne les notifications non lues du membre (les plus récentes en premier).

**Paramètres de requête :**
| Paramètre | Type | Description |
|---|---|---|
| `limit` | int | Nombre max à retourner (défaut : 10, max : 50) |

**Réponse — `200 OK` :**
```json
[
  {
    "notificationNumber": "NOTIF-2026-0042",
    "title": "Réservation confirmée",
    "body": "Votre réservation BKG-2026-0010 pour la Salle Brazza est confirmée.",
    "type": "BOOKING",
    "readAt": null,
    "createdAt": "2026-06-14T10:00:00Z"
  }
]
```

**Valeurs de `type` :**
| Type | Icône suggérée |
|---|---|
| `BOOKING` | Calendrier |
| `PAYMENT` | Carte bancaire |
| `SUBSCRIPTION` | Renouvellement |
| `KYC` | Bouclier |
| `CONTRACT` | Document |
| `SUPPORT` | Message |
| `SYSTEM` | Cloche |

---

### POST `/client/notifications/{notificationNumber}/read`

Marque une notification spécifique comme lue.

**Corps :** Aucun.

**Réponse — `204 No Content`**

---

### POST `/client/notifications/read-all`

Marque toutes les notifications non lues du membre comme lues.

**Corps :** Aucun.

**Réponse — `204 No Content`**

---

### Widget de notification — Barre de navigation

```
┌──────────────────────────────────────────────┐
│  🔔  3         ← badge rouge si unreadCount > 0 │
│                                               │
│  ┌───────────────────────────────────────┐   │
│  │ • Réservation confirmée    il y a 5m  │   │
│  │ • Paiement reçu            il y a 1h  │   │
│  │ • Contrat disponible       il y a 2h  │   │
│  │                                       │   │
│  │ [Tout marquer comme lu]               │   │
│  └───────────────────────────────────────┘   │
└──────────────────────────────────────────────┘
```

**Recommandation :** Interroger `GET /client/notifications?limit=5` à l'ouverture du menu. Actualiser toutes les 60 secondes si la page est active.

---

## Sessions — `/client/sessions`

### GET `/client/sessions`

Liste les sessions actives (refresh tokens non révoqués) du membre.

**Réponse — `200 OK` :**
```json
[
  {
    "id": 12,
    "expiresAt": "2026-06-21T09:00:00Z",
    "active": true
  },
  {
    "id": 8,
    "expiresAt": "2026-06-20T14:30:00Z",
    "active": true
  }
]
```

> **Note :** Les sessions n'incluent pas d'informations sur l'appareil ou l'adresse IP — uniquement l'ID, la date d'expiration et le statut actif.

---

### DELETE `/client/sessions/{id}`

Révoque une session spécifique.

**Réponse — `204 No Content`**

**Erreur — `404` :** Session introuvable ou appartient à un autre membre.

> **Note :** Si le membre révoque sa session actuelle, son access token restera valide jusqu'à expiration (15 min), mais il ne pourra plus renouveler — il sera déconnecté au prochain rafraîchissement.

---

### DELETE `/client/sessions`

Révoque **toutes** les sessions actives du membre.

**Réponse — `204 No Content`**

> **ATTENTION :** Cela déconnecte le membre de tous ses appareils, y compris la session en cours. Afficher un avertissement clair avant d'appeler cet endpoint.

**Flux UX :**
```
Boîte de confirmation :
"Cette action vous déconnectera de tous vos appareils,
y compris celui-ci. Voulez-vous continuer ?"

[Annuler]  [Déconnecter partout]
```

Après succès : effacer les tokens côté client → rediriger vers la page de connexion.

---

## Page Paramètres — Structure recommandée

```
Paramètres
├── Profil
│   ├── GET  /client/profile       → afficher les informations
│   ├── PATCH /client/profile      → modifier prénom, nom, téléphone
│   └── POST /client/profile/change-password  → initier le reset
│
├── Notifications
│   ├── GET  /client/notifications → liste non lus
│   └── POST /client/notifications/read-all  → tout marquer lu
│
└── Sécurité & Sessions
    ├── GET    /client/sessions     → liste des sessions
    ├── DELETE /client/sessions/{id} → révoquer une session
    └── DELETE /client/sessions     → déconnecter partout
```

---

## Séquence de chargement recommandée

```
1. GET /client/profile                     → page profil
2. PATCH /client/profile                   → soumettre modifications
3. POST /client/profile/change-password    → initier reset MDP
4. GET /client/notifications?limit=5       → widget barre nav
5. POST /client/notifications/{n}/read     → marquer lu (au clic)
6. POST /client/notifications/read-all     → tout marquer lu
7. GET /client/sessions                    → page sécurité
8. DELETE /client/sessions/{id}            → révoquer une session
9. DELETE /client/sessions                 → déconnecter partout
```

---

## Référence des erreurs

| HTTP | Condition |
|---|---|
| `400` | Corps de requête invalide (champ trop long, etc.) |
| `403` | Pas un membre ou accès portail non accordé |
| `404` | Notification ou session introuvable (ou appartient à un autre membre) |
