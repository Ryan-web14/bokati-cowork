# Portail Client — Phase 10 : Guide Frontend — Support & Base de connaissances

Tous les endpoints requièrent `Authorization: Bearer <accessToken>`.

---

## Tickets de support — `/client/support/tickets`

### POST `/client/support/tickets`

Crée un nouveau ticket de support.

**Corps de la requête :**
```json
{
  "title": "Impossible d'accéder à ma salle de réunion réservée",
  "description": "J'ai une réservation pour la salle Brazza à 14h mais la porte est verrouillée.",
  "priority": "HIGH",
  "category": "BOOKING",
  "relatedType": "BOOKING",
  "relatedCode": "BKG-2026-0042"
}
```

**Champs :**
| Champ | Requis | Contrainte |
|---|---|---|
| `title` | Oui | Max 255 caractères |
| `description` | Non | Max 5 000 caractères |
| `priority` | Non | `LOW`, `MEDIUM`, `HIGH`, `URGENT` |
| `category` | Non | Voir tableau ci-dessous |
| `relatedType` | Non | Type de l'objet lié (ex. `BOOKING`, `SUBSCRIPTION`) |
| `relatedCode` | Non | Identifiant de l'objet lié |

**Valeurs de `category` :**
| Valeur | Libellé |
|---|---|
| `GENERAL` | Question générale |
| `BILLING` | Facturation & paiements |
| `BOOKING` | Réservations |
| `TECHNICAL` | Problème technique |
| `SUBSCRIPTION` | Abonnement & passes |
| `KYC` | Vérification d'identité |
| `OTHER` | Autre |

**Réponse — `201 Created` :** `SupportTicketResponse`

---

### GET `/client/support/tickets`

Liste les tickets du membre, les plus récents en premier.

**Paramètres de requête :**
| Paramètre | Type | Description |
|---|---|---|
| `status` | enum | Filtrer par statut |
| `page` | int | Défaut : 0 |
| `size` | int | Défaut : 20 |

**Valeurs de `status` :**
| Valeur | Libellé |
|---|---|
| `OPEN` | Ouvert |
| `IN_PROGRESS` | En cours de traitement |
| `WAITING_CUSTOMER` | En attente de votre réponse |
| `RESOLVED` | Résolu |
| `CLOSED` | Clôturé |

**Réponse — `200 OK` :**
```json
{
  "data": [
    {
      "ticketNumber": "TKT-2026-0001",
      "title": "Impossible d'accéder à ma salle de réunion",
      "status": "OPEN",
      "priority": "HIGH",
      "category": "BOOKING",
      "createdAt": "2026-06-14T10:00:00Z",
      "updatedAt": "2026-06-14T10:05:00Z",
      "resolvedAt": null,
      "messageCount": 2,
      "unreadCount": 1
    }
  ],
  "pageable": { "page": 0, "size": 20, "totalElements": 5, "totalPages": 1, "first": true, "last": true }
}
```

**Badge de priorité :**
| Priorité | Couleur |
|---|---|
| `URGENT` | Rouge |
| `HIGH` | Orange |
| `MEDIUM` | Jaune |
| `LOW` | Bleu |

---

### GET `/client/support/tickets/summary`

Retourne le résumé des tickets du membre. Utiliser pour le widget tableau de bord.

**Réponse — `200 OK` :**
```json
{
  "totalTickets": 5,
  "openCount": 2,
  "inProgressCount": 1,
  "waitingCustomerCount": 1,
  "resolvedCount": 1,
  "closedCount": 0,
  "unreadCount": 3
}
```

---

### GET `/client/support/tickets/{ticketNumber}`

Retourne le détail complet d'un ticket avec tous ses messages.

**Réponse — `200 OK` :**
```json
{
  "ticketNumber": "TKT-2026-0001",
  "title": "Impossible d'accéder à ma salle de réunion",
  "description": "J'ai une réservation pour la salle Brazza à 14h...",
  "status": "IN_PROGRESS",
  "priority": "HIGH",
  "category": "BOOKING",
  "relatedType": "BOOKING",
  "relatedCode": "BKG-2026-0042",
  "ownerType": "MEMBER",
  "ownerCode": "MBR-202601-XXXX",
  "ownerName": "Jean Dupont",
  "ownerEmail": "jean@example.com",
  "assignedToName": "Support Bokati",
  "createdAt": "2026-06-14T10:00:00Z",
  "updatedAt": "2026-06-14T10:30:00Z",
  "resolvedAt": null,
  "messages": [
    {
      "id": 1,
      "content": "Bonjour, je suis bloqué devant la salle.",
      "senderType": "MEMBER",
      "senderName": "Jean Dupont",
      "createdAt": "2026-06-14T10:00:00Z",
      "attachments": []
    },
    {
      "id": 2,
      "content": "Nous avons contacté le responsable. Merci de patienter.",
      "senderType": "AGENT",
      "senderName": "Support Bokati",
      "createdAt": "2026-06-14T10:25:00Z",
      "attachments": [
        { "id": 5, "fileName": "procedure.pdf", "fileSize": 45000, "contentType": "application/pdf" }
      ]
    }
  ],
  "attachments": []
}
```

---

### POST `/client/support/tickets/{ticketNumber}/messages`

Ajoute un message au ticket.

**Corps de la requête :**
```json
{
  "content": "Le problème persiste, la porte est toujours verrouillée."
}
```

**Contrainte :** `content` requis, max 10 000 caractères.

**Réponse — `201 Created` :** Le message créé.

---

### POST `/client/support/tickets/{ticketNumber}/attachments`

Téléverse une pièce jointe sur le ticket. **Content-Type : `multipart/form-data`**

**Champs du formulaire :**
| Champ | Description |
|---|---|
| `file` | Fichier à uploader (image, PDF, etc.) |
| `messageId` | ID du message associé (optionnel) |

**Réponse — `201 Created` :** Métadonnées de la pièce jointe.

---

### GET `/client/support/tickets/{ticketNumber}/attachments/{id}/download`

Télécharge une pièce jointe. Retourne le contenu binaire avec les en-têtes appropriés.

---

### POST `/client/support/tickets/{ticketNumber}/close`

Ferme un ticket résolu. Ne peut être effectué que si `status = RESOLVED` ou `OPEN`.

**Corps :** Aucun.

**Réponse — `200 OK` :** Ticket mis à jour avec `status: "CLOSED"`.

---

### POST `/client/support/tickets/{ticketNumber}/reopen`

Réouvre un ticket fermé.

**Réponse — `200 OK` :** Ticket mis à jour avec `status: "OPEN"`.

---

## Base de connaissances — `/client/support/knowledge-base`

### GET `/client/support/knowledge-base`

Liste les articles de la base de connaissances.

**Paramètres de requête :**
| Paramètre | Type | Description |
|---|---|---|
| `category` | String | Filtrer par catégorie |
| `search` | String | Recherche textuelle dans le titre et le contenu |
| `page` / `size` | int | Pagination |

**Réponse — `200 OK` :**
```json
{
  "data": [
    {
      "articleCode": "KB-2026-0001",
      "title": "Comment réserver un espace de travail",
      "category": "BOOKING",
      "summary": "Guide pas-à-pas pour effectuer une réservation via le portail...",
      "readTimeMinutes": 3,
      "publishedAt": "2026-05-01T09:00:00Z"
    }
  ],
  "pageable": { ... }
}
```

---

### GET `/client/support/knowledge-base/{articleCode}`

Retourne le contenu complet d'un article.

**Réponse — `200 OK` :**
```json
{
  "articleCode": "KB-2026-0001",
  "title": "Comment réserver un espace de travail",
  "category": "BOOKING",
  "content": "<p>Pour réserver un espace...</p>",
  "contentFormat": "HTML",
  "tags": ["réservation", "espace", "guide"],
  "readTimeMinutes": 3,
  "relatedArticles": [
    { "articleCode": "KB-2026-0002", "title": "Modifier ou annuler une réservation" }
  ],
  "publishedAt": "2026-05-01T09:00:00Z",
  "updatedAt": "2026-06-01T10:00:00Z"
}
```

---

## Flux UX — Page d'aide

```
┌─────────────────────────────────────────────────┐
│  Centre d'aide                                   │
│                                                  │
│  [🔍 Rechercher dans la base de connaissances]   │
│                                                  │
│  Articles populaires :                           │
│  • Comment réserver un espace de travail         │
│  • Gérer mon abonnement                          │
│  • Configurer mon profil                         │
│                                                  │
│  Mes tickets (2 ouverts)                         │
│  ┌──────────────────────────────────────┐        │
│  │ TKT-2026-0001  Salle verrouillée     │        │
│  │ EN COURS  HAUTE PRIORITÉ  il y a 1h  │        │
│  └──────────────────────────────────────┘        │
│                                                  │
│  [+ Ouvrir un ticket]                            │
└─────────────────────────────────────────────────┘
```

---

## Séquence de chargement recommandée

```
1. GET /client/support/tickets/summary       → widget tableau de bord
2. GET /client/support/tickets               → liste des tickets
3. GET /client/support/tickets/{n}           → vue conversation
4. POST /client/support/tickets/{n}/messages → envoyer un message
5. GET /client/support/knowledge-base        → liste articles
6. GET /client/support/knowledge-base/{code} → lire un article
```

---

## Référence des erreurs

| HTTP | Condition |
|---|---|
| `400` | Corps de requête invalide (titre manquant, contenu vide, etc.) |
| `403` | Pas un membre ou accès portail non accordé |
| `404` | Ticket ou article introuvable (ou appartient à un autre membre) |
| `409` | Impossible de fermer un ticket déjà clôturé ou en cours |
