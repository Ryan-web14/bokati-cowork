# Notifications Temps Réel — Guide Frontend

Guide complet pour l'intégration des notifications temps réel via WebSocket STOMP dans les
applications front-end (back-office admin et portail client).

> **Endpoint WebSocket :** `/ws` (SockJS activé)  
> **Auth :** Header STOMP `Authorization: Bearer <jwt>` au CONNECT  
> **Fallback REST :** Tous les endpoints de polling HTTP restent fonctionnels  
> **Bibliothèques recommandées :** `@stomp/stompjs` + `sockjs-client`

---

## Table des matières

1. [Installation et connexion](#1-installation-et-connexion)
2. [Topics disponibles](#2-topics-disponibles)
3. [Notifications personnelles (members + admins)](#3-notifications-personnelles-members--admins)
4. [Alertes admin broadcast](#4-alertes-admin-broadcast)
5. [Alertes inventaire temps réel](#5-alertes-inventaire-temps-réel)
6. [Messages support (tickets)](#6-messages-support-tickets)
7. [Endpoints REST de notification](#7-endpoints-rest-de-notification)
8. [Payloads TypeScript](#8-payloads-typescript)
9. [Gestion de la reconnexion](#9-gestion-de-la-reconnexion)
10. [Intégration recommandée par écran](#10-intégration-recommandée-par-écran)

---

## 1. Installation et connexion

### Dépendances

```bash
npm install @stomp/stompjs sockjs-client
```

### Connexion

```javascript
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const stompClient = new Client({
  webSocketFactory: () => new SockJS('https://<host>/ws'),
  connectHeaders: {
    Authorization: `Bearer ${accessToken}`,
  },
  reconnectDelay: 5000,     // reconnexion auto toutes les 5s
  heartbeatIncoming: 10000, // heartbeat serveur → client
  heartbeatOutgoing: 10000, // heartbeat client → serveur
});

stompClient.onConnect = () => {
  console.log('WebSocket connecté');
  // Souscrire aux topics ici (voir sections suivantes)
};

stompClient.onStompError = (frame) => {
  console.error('Erreur STOMP:', frame.headers['message']);
};

stompClient.onWebSocketClose = () => {
  console.log('WebSocket fermé, reconnexion automatique...');
};

stompClient.activate();
```

### Déconnexion

```javascript
// À la déconnexion utilisateur ou changement de page
stompClient.deactivate();
```

---

## 2. Topics disponibles

### Topics per-utilisateur (personnels, scoped par session)

| Souscription côté client | Contenu | Audience |
|--------------------------|---------|----------|
| `/user/queue/notifications` | Notification IN_APP personnelle | Member connecté **ou** admin connecté |
| `/user/queue/notifications/unread-count` | Signal d'incrément du compteur | Member connecté **ou** admin connecté |

> **Important :** Le préfixe `/user` est géré par Spring — chaque utilisateur ne reçoit que **ses**
> notifications. Deux utilisateurs connectés simultanément ne voient jamais les données de l'autre.

### Topics broadcast (tous les abonnés reçoivent)

| Souscription côté client | Contenu | Audience |
|--------------------------|---------|----------|
| `/topic/admin/alerts` | Alertes critiques en temps réel | Tous les admins connectés |
| `/topic/inventory/alerts` | Alertes stock et assets | Admins sur le dashboard inventaire |
| `/topic/support/tickets/{ticketNumber}` | Messages de conversation ticket | Agent + client du ticket |

---

## 3. Notifications personnelles (members + admins)

### Souscription

```javascript
stompClient.onConnect = () => {
  // Recevoir les notifications personnelles
  stompClient.subscribe('/user/queue/notifications', (message) => {
    const notification = JSON.parse(message.body);
    
    // Afficher un toast
    showToast({
      title: notification.title,
      type: notification.eventType,
      time: notification.createdAt,
    });
    
    // Mettre à jour le badge compteur
    incrementUnreadCount();
    
    // Ajouter à la liste de notifications dans l'UI
    prependToNotificationList(notification);
  });
};
```

### Payload reçu — `ClientNotificationPayload`

```json
{
  "notificationNumber": "NTF-2026-000456",
  "title": "Votre réservation a été confirmée",
  "eventType": "BOOKING_CONFIRMED",
  "aggregateType": "BOOKING",
  "aggregateId": "BK-2026-000789",
  "payloadJson": "{\"bookingNumber\":\"BK-2026-000789\",\"resourceName\":\"Salle A\"}",
  "createdAt": "2026-06-21T10:00:00Z",
  "read": false
}
```

| Champ | Type | Description |
|-------|------|-------------|
| `notificationNumber` | `string` | Identifiant unique de la notification |
| `title` | `string` | Titre affiché (sujet de la notification) |
| `eventType` | `string` | Type d'événement (voir tableau ci-dessous) |
| `aggregateType` | `string` | Type d'objet métier concerné (`BOOKING`, `SUBSCRIPTION`, etc.) |
| `aggregateId` | `string` | Identifiant de l'objet métier |
| `payloadJson` | `string` | Données supplémentaires en JSON (à parser côté client) |
| `createdAt` | `string` | Horodatage ISO 8601 |
| `read` | `boolean` | Toujours `false` à la réception (non-lu) |

### Types d'événements reçus

#### Portail client (member)

| `eventType` | Description | Navigation suggérée |
|-------------|-------------|---------------------|
| `BOOKING_CONFIRMED` | Réservation confirmée | → Détail réservation |
| `BOOKING_CANCELLED` | Réservation annulée | → Détail réservation |
| `BOOKING_COMPLETED` | Réservation terminée | → Historique |
| `BOOKING_NO_SHOW` | Absence détectée | → Détail réservation |
| `SUBSCRIPTION_ACTIVATED` | Abonnement activé | → Mon abonnement |
| `SUBSCRIPTION_CANCELLED` | Abonnement annulé | → Mon abonnement |
| `SUBSCRIPTION_RENEWED` | Abonnement renouvelé | → Mon abonnement |
| `BILLING_DOCUMENT_ISSUED` | Nouvelle facture émise | → Mes factures |
| `CONTRACT_SIGNING_REQUESTED` | Signature requise | → Signature contrat |
| `CONTRACT_SIGNED_VIA_ESIGN` | Contrat signé | → Mes contrats |

#### Back-office (admin)

| `eventType` | Description | Navigation suggérée |
|-------------|-------------|---------------------|
| `SUPPORT_CLIENT_REPLY` | Client a répondu à un ticket | → Détail ticket |
| `CASH_ANOMALY` | Anomalie caisse détectée | → Session caisse |
| `BILLING_OVERDUE` | Factures en retard | → Facturation |

### Signal compteur de non-lues

```javascript
stompClient.subscribe('/user/queue/notifications/unread-count', (message) => {
  const signal = JSON.parse(message.body);
  if (signal.action === 'INCREMENT') {
    // Incrémenter le badge local
    setUnreadCount(prev => prev + 1);
  }
});
```

Payload : `{ "action": "INCREMENT" }`

---

## 4. Alertes admin broadcast

Les alertes broadcast sont envoyées à **tous les admins connectés** via `/topic/admin/alerts`.
Elles ne sont **pas persistées en DB** — elles servent au dashboard temps réel.

### Souscription (back-office uniquement)

```javascript
stompClient.subscribe('/topic/admin/alerts', (message) => {
  const alert = JSON.parse(message.body);
  
  switch (alert.severity) {
    case 'CRITICAL':
      // Modal rouge + notification système
      showCriticalAlert(alert);
      playAlertSound();
      break;
    case 'WARNING':
      // Toast orange persistant
      showWarningToast(alert);
      break;
    case 'INFO':
      // Toast discret auto-dismiss
      showInfoToast(alert);
      break;
  }
});
```

### Payload reçu — `AdminAlertEvent`

```json
{
  "alertType": "CASH_ANOMALY",
  "module": "PAYMENT",
  "title": "Anomalie caisse détectée",
  "message": "Anomalie détectée à la clôture de la session CSH-2026-000123",
  "severity": "CRITICAL",
  "payloadJson": null,
  "createdAt": "2026-06-21T14:30:00Z"
}
```

| Champ | Type | Description |
|-------|------|-------------|
| `alertType` | `string` | Type d'alerte (`CASH_ANOMALY`, `BILLING_OVERDUE`, `SLA_BREACH`, etc.) |
| `module` | `string` | Module source (`PAYMENT`, `BILLING`, `SUPPORT`, `INVENTORY`) |
| `title` | `string` | Titre court pour le toast/badge |
| `message` | `string` | Description détaillée de l'alerte |
| `severity` | `string` | `INFO`, `WARNING` ou `CRITICAL` |
| `payloadJson` | `string \| null` | Données additionnelles (JSON libre) |
| `createdAt` | `string` | Horodatage ISO 8601 |

### Alertes actuellement émises

| `alertType` | `module` | `severity` | Déclencheur |
|-------------|----------|------------|-------------|
| `CASH_ANOMALY` | `PAYMENT` | `CRITICAL` | Anomalie détectée à la clôture d'une session caisse |
| `BILLING_OVERDUE` | `BILLING` | `WARNING` | N factures marquées en retard (worker horaire) |

---

## 5. Alertes inventaire temps réel

Les alertes stock et assets sont diffusées en broadcast à tous les admins abonnés au topic
`/topic/inventory/alerts`. Elles sont également persistées en DB (accessibles via REST).

### Souscription (dashboard inventaire)

```javascript
stompClient.subscribe('/topic/inventory/alerts', (message) => {
  const alert = JSON.parse(message.body);
  
  // Ajouter en haut de la liste des alertes
  prependToAlertList(alert);
  
  // Mettre à jour le compteur d'alertes ouvertes
  incrementOpenAlertCount();
  
  // Toast selon la gravité du type
  const isCritical = ['OUT_OF_STOCK', 'NEGATIVE_STOCK', 'EXPIRY_IMMINENT',
                      'ASSET_RETURN_OVERDUE'].includes(alert.alertType);
  if (isCritical) {
    showWarningToast(`${alert.alertType}: ${alert.message}`);
  }
});
```

### Payload reçu — `InventoryAlertEvent`

```json
{
  "alertCode": "INV-2026-000789",
  "alertType": "LOW_STOCK",
  "itemCode": "ITM-00045",
  "locationCode": "LOC-01",
  "assetCode": null,
  "message": "Stock bas pour Café Premium — 5 unités restantes (seuil: 10)",
  "createdAt": "2026-06-21T06:00:00Z"
}
```

| Champ | Type | Description |
|-------|------|-------------|
| `alertCode` | `string` | Identifiant unique de l'alerte |
| `alertType` | `string` | Type d'alerte (voir tableau ci-dessous) |
| `itemCode` | `string \| null` | Code article concerné |
| `locationCode` | `string \| null` | Code emplacement |
| `assetCode` | `string \| null` | Code asset (si alerte asset) |
| `message` | `string` | Description de l'alerte |
| `createdAt` | `string` | Horodatage ISO 8601 |

### Types d'alertes inventaire

| `alertType` | Description | Criticité | Fréquence détection |
|-------------|-------------|-----------|---------------------|
| `LOW_STOCK` | Stock sous le seuil de réapprovisionnement | Moyenne | Toutes les 30 min |
| `RECURRING_LOW_STOCK` | Stock bas de manière récurrente | Haute | Toutes les 30 min |
| `OUT_OF_STOCK` | Rupture de stock | Critique | Toutes les 30 min |
| `NEGATIVE_STOCK` | Stock négatif (erreur de données) | Critique | Toutes les 30 min |
| `OVERSTOCK` | Surstock détecté | Basse | Toutes les 30 min |
| `SLOW_MOVING` | Article à rotation lente (>30 jours) | Basse | Toutes les 6h |
| `EXPIRY_SOON` | Lot expire dans 30 jours | Moyenne | Toutes les 2h |
| `EXPIRY_IMMINENT` | Lot expire dans 7 jours | Critique | Toutes les 2h |
| `WARRANTY_SOON` | Garantie asset expire dans 30 jours | Moyenne | Toutes les 3h |
| `MAINTENANCE_DUE` | Maintenance planifiée dans 24h | Haute | Toutes les 3h |
| `ASSET_RETURN_OVERDUE` | Retour asset en retard | Critique | Toutes les 3h |
| `ASSET_RETURN_DUE_SOON` | Retour asset prévu dans 24h | Moyenne | Toutes les 3h |
| `SUSPICIOUS_ADJUSTMENT` | Ajustement de stock suspect | Haute | Temps réel |

---

## 6. Messages support (tickets)

Le WebSocket pour les messages de tickets support existait déjà.

### Souscription (écran détail ticket)

```javascript
// À l'ouverture du détail d'un ticket
stompClient.subscribe(`/topic/support/tickets/${ticketNumber}`, (message) => {
  const newMessage = JSON.parse(message.body);
  
  // Ajouter au fil de conversation sans recharger la page
  appendToConversation(newMessage);
});
```

### Payload — `TicketMessageResponse`

```json
{
  "id": 42,
  "senderType": "AGENT",
  "senderId": "12",
  "senderName": "Marc K.",
  "content": "Votre badge a été réinitialisé.",
  "internal": false,
  "createdAt": "2026-06-20T14:30:00Z"
}
```

> **Rappel :** Les messages avec `internal: true` sont des notes internes. Sur le portail client,
> filtrer ces messages pour ne pas les afficher.

---

## 7. Endpoints REST de notification

Les endpoints REST restent disponibles comme fallback et pour la gestion du statut lu/non-lu.

### Portail client — `/client/notifications`

| Méthode | Chemin | Description |
|---------|--------|-------------|
| GET | `/sni/api/v1/client/notifications?limit=20` | Liste des notifications non-lues |
| GET | `/sni/api/v1/client/notifications/unread-count` | Compteur de non-lues → `{ "count": 3 }` |
| PATCH | `/sni/api/v1/client/notifications/{notificationNumber}/read` | Marquer comme lue |
| PATCH | `/sni/api/v1/client/notifications/read-all` | Tout marquer comme lu |

### Back-office admin — `/admin/notifications`

| Méthode | Chemin | Description |
|---------|--------|-------------|
| GET | `/sni/api/v1/admin/notifications/unread?limit=20` | Liste des notifications non-lues de l'admin connecté |
| GET | `/sni/api/v1/admin/notifications/unread-count` | Compteur de non-lues → `{ "count": 5 }` |
| PATCH | `/sni/api/v1/admin/notifications/{notificationNumber}/read` | Marquer comme lue |
| PATCH/POST | `/sni/api/v1/admin/notifications/read-all` | Tout marquer comme lu |

### Quand utiliser REST vs WebSocket ?

| Situation | Mécanisme |
|-----------|-----------|
| Chargement initial de la page | REST `GET .../unread` |
| Réception temps réel d'une nouvelle notification | WebSocket `/user/queue/notifications` |
| Marquer comme lu | REST `PATCH .../read` |
| Après reconnexion WebSocket | REST `GET .../unread` pour rattraper les messages manqués |

---

## 8. Payloads TypeScript

```typescript
// Notification personnelle (member ou admin)
interface ClientNotificationPayload {
  notificationNumber: string;
  title: string;
  eventType: string;
  aggregateType: string;
  aggregateId: string;
  payloadJson: string;    // JSON stringifié — parser avec JSON.parse()
  createdAt: string;      // ISO 8601
  read: boolean;          // toujours false à la réception
}

// Alerte admin broadcast
interface AdminAlertEvent {
  alertType: string;
  module: string;
  title: string;
  message: string;
  severity: 'INFO' | 'WARNING' | 'CRITICAL';
  payloadJson: string | null;
  createdAt: string;
}

// Alerte inventaire
interface InventoryAlertEvent {
  alertCode: string;
  alertType: string;
  itemCode: string | null;
  locationCode: string | null;
  assetCode: string | null;
  message: string;
  createdAt: string;
}

// Signal compteur
interface UnreadCountSignal {
  action: 'INCREMENT';
}

// Message support (existant)
interface TicketMessageResponse {
  id: number;
  senderType: 'CLIENT' | 'AGENT' | 'SYSTEM';
  senderId: string;
  senderName: string;
  content: string;
  internal: boolean;
  createdAt: string;
}
```

---

## 9. Gestion de la reconnexion

### Comportement automatique

Le client STOMP avec `reconnectDelay: 5000` se reconnecte automatiquement toutes les 5 secondes
après une déconnexion. Les souscriptions sont **automatiquement restaurées** par `@stomp/stompjs`.

### Rattrapage des messages manqués

Après une reconnexion, les messages émis pendant la déconnexion sont perdus (Redis Pub/Sub est
at-most-once). Il faut recharger via REST :

```javascript
stompClient.onConnect = () => {
  // Re-souscrire aux topics
  subscribeToTopics();
  
  // Rattraper les notifications manquées
  fetchUnreadNotifications().then(notifications => {
    updateNotificationList(notifications);
    updateUnreadBadge(notifications.length);
  });
};
```

---

## 10. Intégration recommandée par écran

### Portail client — Layout principal

```javascript
// Au login / montage du layout
stompClient.activate();

// Souscriptions
stompClient.onConnect = () => {
  stompClient.subscribe('/user/queue/notifications', handleNotification);
  stompClient.subscribe('/user/queue/notifications/unread-count', handleUnreadCount);
  
  // Chargement initial depuis REST
  fetchUnreadCount().then(setUnreadBadge);
};

// Au logout
stompClient.deactivate();
```

### Back-office — Layout principal

```javascript
stompClient.onConnect = () => {
  // Notifications personnelles admin
  stompClient.subscribe('/user/queue/notifications', handleNotification);
  stompClient.subscribe('/user/queue/notifications/unread-count', handleUnreadCount);
  
  // Alertes broadcast admin
  stompClient.subscribe('/topic/admin/alerts', handleAdminAlert);
  
  // Chargement initial
  fetchAdminUnreadCount().then(setUnreadBadge);
};
```

### Back-office — Dashboard inventaire

```javascript
stompClient.onConnect = () => {
  // En plus des souscriptions du layout principal
  stompClient.subscribe('/topic/inventory/alerts', (message) => {
    const alert = JSON.parse(message.body);
    addToAlertFeed(alert);
    refreshAlertCounters();
  });
};
```

### Back-office — Détail ticket support

```javascript
// À l'ouverture du ticket
const ticketSub = stompClient.subscribe(
  `/topic/support/tickets/${ticketNumber}`,
  handleNewMessage
);

// À la fermeture / navigation hors du ticket
ticketSub.unsubscribe();
```

### Portail client — Détail ticket support

```javascript
// Même souscription que l'admin, mais filtrer les messages internes
stompClient.subscribe(`/topic/support/tickets/${ticketNumber}`, (message) => {
  const msg = JSON.parse(message.body);
  if (!msg.internal) {
    appendToConversation(msg);
  }
});
```
