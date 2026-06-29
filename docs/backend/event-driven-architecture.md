# Event-Driven Architecture — Backend Reference

Technical documentation for the real-time notification system built on Redis Pub/Sub and WebSocket STOMP.

---

## 1. Architecture Overview

```
┌─────────────┐     ┌──────────────────┐     ┌──────────────────┐
│  Modules    │     │  Redis Pub/Sub   │     │  WebSocket       │
│  (Booking,  │────>│  Channels:       │────>│  STOMP Broker     │
│  Billing,   │     │  • in_app        │     │  (/ws + SockJS)  │
│  Inventory  │     │  • inventory     │     │                  │
│  Contract   │     │  • admin_alert   │     │  Destinations:   │
│  Payment)   │     └──────────────────┘     │  /user/queue/*   │
│             │            ↑                 │  /topic/admin/*  │
│             │     ┌──────┴──────┐          │  /topic/inventory│
│             │────>│ RedisEvent  │          └────────┬─────────┘
│             │     │ Publisher   │                    │
└─────────────┘     └─────────────┘                   │
                                              ┌───────┴────────┐
                                              │   Connected    │
                                              │   clients      │
                                              └────────────────┘
```

### Why Redis Pub/Sub

- **Multi-instance:** Spring `ApplicationEventPublisher` is JVM-local. A user connected on instance A would never receive an event published on instance B. Redis solves cross-instance fan-out.
- **No new infra:** Redis already runs on port 6379 (sessions + cache).
- **At-most-once:** Acceptable for real-time push — the database is the source of truth. Clients can always fall back to REST polling.

---

## 2. Package Structure

### Event infrastructure (`core/event/`)

```
core/event/
  RedisChannelSubscriber.java          — Interface: channel() + MessageListener
  RedisEventPublisher.java             — publish(channel, event) → StringRedisTemplate.convertAndSend()
  RedisChannels.java                   — Channel name constants
  WebSocketTopics.java                 — WebSocket destination constants
  dto/
    WebSocketNotificationEvent.java    — Redis transit DTO for IN_APP notifications
    InventoryAlertEvent.java           — Redis transit DTO for inventory alerts
    ClientNotificationPayload.java     — Final payload pushed to WebSocket clients
    AdminAlertEvent.java               — Broadcast payload for admin dashboard
  subscriber/
    InAppNotificationRedisSubscriber   — Redis → convertAndSendToUser(/queue/notifications)
    InventoryAlertRedisSubscriber      — Redis → convertAndSend(/topic/inventory/alerts)
    AdminAlertRedisSubscriber          — Redis → convertAndSend(/topic/admin/alerts)
```

### Configuration (`core/configuration/`)

| Class | Role |
|-------|------|
| `RedisPubSubConfig` | `RedisMessageListenerContainer` bean, auto-registers all `RedisChannelSubscriber` implementations, dedicated `redisPubSubExecutor` thread pool |
| `WebSocketConfig` | STOMP broker (`/topic`, `/queue`), user destination prefix (`/user`), transport limits (64KB msg, 512KB buffer, 20s timeout), inbound/outbound channel executors |
| `WebSocketAuthInterceptor` | JWT validation on STOMP CONNECT frame, sets `UsernamePasswordAuthenticationToken` as Principal |
| `WebSocketSubscriptionInterceptor` | Rejects unauthenticated SUBSCRIBE to `/topic/**` |

### Admin notification layer

| Class | File | Role |
|-------|------|------|
| `AdminInAppNotifier` | `features/portal/notification/service/` | Creates IN_APP notifications for admin users + broadcasts alerts to `/topic/admin/alerts` via Redis |
| `AdminNotificationController` | `features/notification/controller/` | REST endpoints for admin unread/mark-read operations |

---

## 3. Redis Channels

| Channel | Constant | Publisher | Subscriber | WebSocket Destination |
|---------|----------|-----------|------------|----------------------|
| `events:notification:in_app` | `RedisChannels.NOTIFICATION_IN_APP` | `NotificationDispatchSupport` | `InAppNotificationRedisSubscriber` | `/user/queue/notifications` (per-user) |
| `events:inventory:alert` | `RedisChannels.INVENTORY_ALERT` | `InventoryDailyWorker` | `InventoryAlertRedisSubscriber` | `/topic/inventory/alerts` (broadcast) |
| `events:admin:alert` | `RedisChannels.ADMIN_ALERT` | `AdminInAppNotifier` | `AdminAlertRedisSubscriber` | `/topic/admin/alerts` (broadcast) |

---

## 4. Data Flow — IN_APP Notifications

```
Module action
  → MemberInAppNotifier.notify() / AdminInAppNotifier.notify()
    → NotificationService.send() [@Async]
      → NotificationDispatchSupport.create()     ← persists NotificationMessage in DB
      → NotificationDispatchSupport.process()
        → channel == IN_APP ?
          → markSent()                            ← DB status = SENT
          → publishInAppToRedis(message)          ← NEW
            → RedisEventPublisher.publish("events:notification:in_app", WebSocketNotificationEvent)
              → [Redis Pub/Sub fan-out to all JVM instances]
                → InAppNotificationRedisSubscriber.onMessage()
                  → SimpMessagingTemplate.convertAndSendToUser(email, "/queue/notifications", payload)
                  → SimpMessagingTemplate.convertAndSendToUser(email, "/queue/notifications/unread-count", INCREMENT)
```

**Key point:** The single modification in `NotificationDispatchSupport.process()` (adding `publishInAppToRedis()` after `markSent()`) makes all 5 modules that call `MemberInAppNotifier` automatically push through WebSocket with zero code changes.

---

## 5. Modified Files (4 total)

### `NotificationDispatchSupport.java`
**File:** `features/notification/service/support/NotificationDispatchSupport.java`

- Injected: `RedisEventPublisher`
- In `process()`, after `markSent(message)` for `IN_APP` channel, calls `publishInAppToRedis(message)` which publishes a `WebSocketNotificationEvent` to Redis channel `events:notification:in_app`
- Non-fatal: if Redis publish fails, notification is already persisted and marked SENT

### `InventoryDailyWorker.java`
**File:** `features/inventory/intelligence/worker/InventoryDailyWorker.java`

- Injected: `RedisEventPublisher`
- In `saveAndPublish()`, after the existing `outboxService.publish()` call (email delivery unchanged), publishes `InventoryAlertEvent` to Redis channel `events:inventory:alert`
- Both paths coexist: outbox for email, Redis for real-time dashboard

### `CashSessionAnomalyDetectionListener.java`
**File:** `features/payment/service/support/CashSessionAnomalyDetectionListener.java`

- Injected: `AdminInAppNotifier`
- After `anomalyDetectionService.analyzeSession()`, if anomalies returned is non-empty, calls `adminInAppNotifier.broadcastAlert("CASH_ANOMALY", "PAYMENT", ..., "CRITICAL")`

### `BillingOverdueWorker.java`
**File:** `features/billing/worker/BillingOverdueWorker.java`

- Injected: `AdminInAppNotifier`
- After `billingDocumentService.markOverdueDocuments()` returns > 0, calls `adminInAppNotifier.broadcastAlert("BILLING_OVERDUE", "BILLING", ..., "WARNING")`

---

## 6. Unmodified Files (backward compatibility)

- `MemberInAppNotifier` + its 5 callers (Booking, Billing, Contract, Subscription ×2) — gain WebSocket push for free
- `OutboxWorker`, `OutboxService`, all 6 `OutboxEventProcessor` implementations
- `NotificationWorker`, `WebhookDeliveryWorker`
- `SubscriptionNotificationDispatchWorker`
- `ClientNotificationController`, `NotificationController` (REST polling still works)
- `NotificationService` interface
- All existing Spring `@TransactionalEventListener` implementations

---

## 7. Module Classification

### Event-driven (real-time via Redis → WebSocket)

| Module | Event | Destination | Mechanism |
|--------|-------|-------------|-----------|
| Booking | CONFIRMED, CANCELLED, NO_SHOW | `/user/queue/notifications` | Via MemberInAppNotifier → auto |
| Subscription | ACTIVATED, CANCELLED, RENEWED | `/user/queue/notifications` | Via MemberInAppNotifier → auto |
| Billing | INVOICE_ISSUED | `/user/queue/notifications` | Via MemberInAppNotifier → auto |
| Contract | SIGNING_REQUESTED, SIGNED | `/user/queue/notifications` | Via MemberInAppNotifier → auto |
| Inventory | 13 alert types | `/topic/inventory/alerts` | Direct RedisEventPublisher |
| Payment | Cash anomaly | `/topic/admin/alerts` | AdminInAppNotifier.broadcastAlert |
| Billing | Overdue detection | `/topic/admin/alerts` | AdminInAppNotifier.broadcastAlert |
| Support | Ticket messages | `/topic/support/tickets/{id}` | Direct SimpMessagingTemplate (existing) |

### Workers (polling — unchanged)

| Worker | Interval | Purpose |
|--------|----------|---------|
| NotificationWorker | 30s | Email retry dispatch |
| WebhookDeliveryWorker | 60s | External webhook delivery |
| OutboxWorker | 2s | Guaranteed at-least-once delivery |
| BillingPaymentReminderWorker | Daily 8AM | Scheduled email reminders |
| LeadDormantAlertWorker | Daily 8AM | CRM daily batch |
| SubscriptionNotificationDispatchWorker | 60s | Subscription email pipeline |
| All nightly workers | Daily 2-3AM | Reconciliation, retention, fiscal |

---

## 8. Thread Pool Sizing (20K concurrent users)

| Executor | Purpose | Core | Max | Queue |
|----------|---------|------|-----|-------|
| `redisPubSubExecutor` | Inbound Redis messages | 4 | 16 | 500 |
| WS outbound | STOMP push to clients | 4 | 20 | 500 |
| WS inbound | STOMP from clients | 4 | 16 | 200 |
| `taskExecutor` (existing) | @Async email | 4 | 12 | 200 |

### WebSocket Transport

| Parameter | Value |
|-----------|-------|
| Message size limit | 64 KB |
| Send buffer size | 512 KB |
| Send timeout | 20 seconds |

---

## 9. Adding a New Real-Time Event

To add a new module event to the real-time system:

### Option A: IN_APP notification (per-user, persisted)

Already works for any module using `MemberInAppNotifier` or `AdminInAppNotifier`. Just call:

```java
memberInAppNotifier.notify(
    "MY_EVENT_TYPE", "MY_AGGREGATE", aggregateId,
    recipientEmail, recipientName, recipientCode,
    "Subject text", Map.of("key", "value")
);
```

The WebSocket push happens automatically downstream.

### Option B: Broadcast alert (admin dashboard, not persisted)

```java
adminInAppNotifier.broadcastAlert(
    "ALERT_TYPE", "MODULE_NAME",
    "Short title", "Detailed message",
    "WARNING",  // INFO, WARNING, CRITICAL
    null        // optional JSON payload
);
```

### Option C: Custom Redis channel (new topic)

1. Add channel constant to `RedisChannels.java`
2. Add topic constant to `WebSocketTopics.java`
3. Create a new DTO record in `core/event/dto/`
4. Create a new subscriber implementing `RedisChannelSubscriber`
5. Publish via `redisEventPublisher.publish(channel, dto)` from your module

The `RedisPubSubConfig` auto-discovers all `RedisChannelSubscriber` beans — no config changes needed.

---

## 10. Guarantees & Limitations

| Property | Guarantee |
|----------|-----------|
| **Persistence** | Every IN_APP notification is persisted in DB before Redis publish. If Redis fails, the notification exists and can be retrieved via REST. |
| **Delivery** | At-most-once (Redis Pub/Sub). If the subscriber is disconnected when the message is published, it is lost. Client must reconnect and fetch via REST. |
| **Ordering** | Per-channel ordering is guaranteed within a single Redis instance. |
| **Cross-instance** | Redis Pub/Sub ensures all JVM instances receive every message. |
| **Idempotency** | Each notification has a unique `notificationNumber`. Clients should deduplicate on this field. |
