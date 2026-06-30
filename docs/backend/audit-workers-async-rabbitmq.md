# Audit — Workers, Async & RabbitMQ

> Date : 2026-06-30  
> Périmètre : tous les traitements différés, planifiés ou asynchrones  
> Statut : **Problèmes critiques identifiés**

---

## Résumé exécutif

| # | Problème | Sévérité |
|---|----------|----------|
| C1 | Scheduler Spring à **thread unique** — un worker bloquant retarde tout le reste | Critique |
| C2 | `email.dlq`, `notification.dlq`, `notification.admin` **sans consommateur** — messages perdus silencieusement | Critique |
| C3 | `PaymentTransactionWorkflowListener` : `@Async + @TransactionalEventListener` sans retry — souscriptions bloquées en PENDING_ACTIVATION | Critique |
| W1 | `BillingPaymentReminderWorker` : une exception sur un item abandonne tout le batch restant | Élevée |
| W2 | `contractGenerationExecutor` déclaré mais jamais utilisé — `@Async("contractGenerationExecutor")` absent de tout service | Élevée |
| W3 | `PaymentIntentExpiryAlertWorker` envoie des emails **directement** (hors RabbitMQ), contournant le pipeline de tracking | Moyenne |
| W4 | 19 workers sans `try/catch` — une exception non rattrapée tue l'exécution et n'est jamais retryée | Moyenne |
| W5 | `SubscriptionContractGenerationRepairWorker` se ré-invente en outbox item-par-item — doublon partiel avec l'outbox | Faible |

---

## 1. Vue d'ensemble — Cartographie complète

### 1.1 Threads disponibles

```
Spring Default Scheduler    → 1 thread  (CRITIQUE — tous les @Scheduled partagent ce thread)
taskExecutor (pool @Async)  → 4 core / 12 max / 200 queue
contractGenerationExecutor  → 2 core / 4 max / 100 queue  ← déclaré, jamais utilisé
RabbitMQ consumers          → voir §2
```

### 1.2 Comptage des traitements asynchrones

| Type | Nombre |
|---|---|
| Workers `@Scheduled` | 41 fichiers / **48 méthodes** |
| Consumers `@RabbitListener` | 3 classes / 6 méthodes |
| Méthodes `@Async` | ~18 dans ~12 classes |
| `@TransactionalEventListener` | 3 listeners |
| `@EventListener` synchrone | 0 identifié |

---

## 2. Catalogue complet des workers `@Scheduled`

### 2.1 Infrastructure critique

| Worker | Delay / Cron | Batch | try/catch par item | Notes |
|---|---|---|---|---|
| `OutboxWorker` | `500 ms` (dev) / `2 s` (prod) | 100 / 25 | via service | Pipeline outbox entier |
| `NotificationWorker` | `30 s` | 25 | non | Dispatch notifications in-app |
| `WebhookDeliveryWorker` | `1 min` | 25 | non | Envoi webhooks |

### 2.2 Billing

| Worker | Delay / Cron | Batch | try/catch par item | Risque |
|---|---|---|---|---|
| `BillingPeriodClosureWorker.runDailyClosure` | `0 30 23 * * *` | — | non | — |
| `BillingPeriodClosureWorker.runMonthlyClosure` | `0 15 0 1 * *` | — | non | — |
| `BillingPeriodClosureWorker.runAnnualClosure` | `0 0 1 1 1 *` | — | non | — |
| `BillingOverdueWorker` | `1 h` | — | non | — |
| `FiscalIntegrityWorker` | `0 0 2 * * *` | — | non | — |
| `BillingPaymentReminderWorker` | `0 0 8 * * *` | **100 hardcodé** | **NON — batch abandonné sur erreur** | **W1** |
| `PaymentScheduleOverdueWorker` | `1 h` | — | non | — |

### 2.3 Payment

| Worker | Delay / Cron | Batch | try/catch par item | Risque |
|---|---|---|---|---|
| `DunningWorker` | `1 min` | tout le due | **OUI** | Candidat RabbitMQ |
| `MobileMoneyStatusPollingWorker` | `5 min` (si pawaypay enabled) | tout le processing | **OUI** | Candidat RabbitMQ |
| `PaymentReconciliationWorker` | `0 30 2 * * *` | batch unique | non | — |
| `PaymentIntentExpiryWorker` | `5 min` | bulk UPDATE direct | non | — |
| `PaymentIntentExpiryAlertWorker` | `30 min` | par item | **OUI** | **W3 — email hors RabbitMQ** |
| `WalletHoldExpiryWorker` | `5 min` | bulk | non | — |

### 2.4 Subscription & Pass

| Worker | Delay / Cron | Batch | try/catch par item | Risque |
|---|---|---|---|---|
| `SubscriptionRenewalWorker` | `15 min` | via service | non | Candidat RabbitMQ |
| `SubscriptionCancellationWorker` | `15 min` | via service | non | Candidat RabbitMQ |
| `SubscriptionIntegrityWorker` | `30 min` | via service | non | — |
| `PassRenewalWorker` | `15 min` | via service | non | Candidat RabbitMQ |
| `PassExpiryWorker` | `10 min` | via service | non | — |
| `EntitlementExpiryWorker` | `10 min` | via service | non | — |
| `EntitlementReservationExpiryWorker` | `5 min` | via service | non | — |
| `SubscriptionContractGenerationRepairWorker` | `1 min` | 20 / type | **OUI** | Doublon partiel outbox — **W5** |
| `SubscriptionRolloverWorker` | `15 min` | via service | non | Candidat RabbitMQ |
| `SubscriptionNotificationDispatchWorker` | `1 min` | 100 | non | — |
| `SubscriptionAddonExpiryWorker` | `15 min` | via service | non | — |
| `PromotionExpiryWorker` | `15 min` | via service | non | — |
| `SubscriptionChangeWorker` | `15 min` | via service | non | Candidat RabbitMQ |

### 2.5 Document / KYC / Contract

| Worker | Delay / Cron | Batch | try/catch par item | Risque |
|---|---|---|---|---|
| `KycAutomationWorker.checkExpiringKycDocuments` | `0 0 8 * * MON` | via service | non (forEach sans catch) | — |
| `KycAutomationWorker.remindIncompleteKycCases` | `0 0 9 * * *` | via service | non | — |
| `KycAutomationWorker.autoApproveEligibleDocuments` | `0 0 8 * * *` | via service | **OUI** | — |
| `DocumentLifecycleWorker` | `1 h` | via service | non | — |
| `DocumentRetentionWorker` | `0 30 2 * * *` | via service | non | — |
| `ContractLifecycleWorker` | `1 h` | via service | non | — |

### 2.6 Support

| Worker | Delay / Cron | Batch | try/catch par item | Risque |
|---|---|---|---|---|
| `SupportAutoCloseWorker` | `1 h` | via repo | **OUI** | — |
| `SupportSlaEscalationWorker` | `1 h` | via repo | **OUI** | — |
| `SupportInboundEmailWorker` | `1 min` | 25 | **OUI (double niveau)** | Candidat Graph Webhook |
| `SupportCsatRequestWorker` | `1 h` | via repo | **OUI** | — |

### 2.7 Booking & Autres

| Worker | Delay / Cron | Batch | try/catch par item | Risque |
|---|---|---|---|---|
| `BookingNoShowWorker` | `1 min` | 200 x 2 | non | — |
| `BookingWaitlistWorker` | `5 min` | 100 | non | — |
| `VisitorPassExpiryWorker` | `0 0 * * * *` (cron non configurable) | via repo | non | — |
| `TaskReminderWorker` | `0 0 * * * *` | via repo | **OUI** | — |
| `LeadDormantAlertWorker` | `0 0 8 * * MON-FRI` | via repo | **OUI** | — |
| `InventoryDailyWorker.runReservationExpiryChecks` | `0 */30 * * * *` | via service | non | — |
| `InventoryDailyWorker.runExpirySoonChecks` | `0 0 */2 * * *` | via service | non (WebSocket try/catch) | — |
| `InventoryDailyWorker.runSlowMovingChecks` | `0 0 */6 * * *` | via service | non | — |
| `InventoryDailyWorker.runAssetAlertChecks` | `0 0 */3 * * *` | via service | non | — |

---

## 3. Défaillances détaillées

---

### [C1] Scheduler Spring à thread unique

**Symptôme** : si un worker se bloque (timeout DB, appel réseau lent, deadlock), **tous les autres workers sont retardés** proportionnellement.

**Cause** : aucune `@Bean ThreadPoolTaskScheduler` n'est déclarée. Spring utilise son scheduler par défaut : **1 thread unique** partagé par tous les `@Scheduled`.

**Impact potentiel** :
- `OutboxWorker` (500 ms) bloqué par `SupportInboundEmailWorker` (poll Graph API — réseau lent) → outbox en retard → emails, contrats, notifications en retard
- `DunningWorker` (1 min) bloqué par `BillingPeriodClosureWorker` (lourde agrégation) → relances de paiement retardées
- `BookingNoShowWorker` (1 min, batch 400 rows) consomme le thread et bloque tous les workers sous charge

**Correction** :

```java
// AsyncConfig.java ou SchedulingConfig.java
@Bean
public ThreadPoolTaskScheduler taskScheduler() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(8);           // au moins autant que de workers critiques simultanés
    scheduler.setThreadNamePrefix("scheduler-");
    scheduler.setWaitForTasksToCompleteOnShutdown(true);
    scheduler.setAwaitTerminationSeconds(30);
    return scheduler;
}
```

Et dans la classe principale ou `SchedulingConfig` :
```java
@EnableScheduling
@Configuration
public class SchedulingConfig implements SchedulingConfigurer {
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.setTaskScheduler(taskScheduler());
    }
}
```

---

### [C2] DLQ sans consommateurs — messages silencieusement perdus

**Queues orphelines** :

| Queue | Alimentée par | Consommateur | Impact |
|---|---|---|---|
| `email.dlq` | Tous les `EmailConsumer` en échec | **AUCUN** | Emails OTP, contrats, relances — perdus définitivement |
| `notification.dlq` | `NotificationWebhookConsumer`, `NotificationInAppConsumer` en échec | **AUCUN** | Webhooks et notifications perdus |
| `notification.admin` | `EmailRabbitPublisher.publishNotification(ADMIN)` | **AUCUN** | Alertes administrateur jamais reçues |

Comportement actuel : `setDefaultRequeueRejected(false)` sur toutes les factories → en cas d'exception dans un consumer, le message est transféré en DLQ **sans retry**. Il y stagne indéfiniment, invisible, sans alerte.

**Correction minimale — `EmailDlqConsumer`** :

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailDlqConsumer {

    private final EmailDeliveryLogRepository deliveryLogRepository;
    private final AdminInAppNotifier adminNotifier;

    @RabbitListener(queues = "email.dlq", containerFactory = "dlqListenerContainerFactory")
    public void consumeDlq(EmailRabbitMessage message, @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        log.error("[DLQ] Email échoué définitivement: to={}, subject={}, emailNumber={}",
                message.to(), message.subject(), message.emailNumber());
        deliveryLogRepository.findByEmailNumber(message.emailNumber())
                .ifPresent(log -> log.setLastError("Message définitivement en DLQ après échec RabbitMQ"));
        adminNotifier.broadcastAlert(
            "Email DLQ — livraison échouée",
            "email:" + message.emailNumber() + " vers " + message.to(),
            AlertLevel.HIGH
        );
        // ACK manuel — ne pas relancer, enregistrement en base suffit
    }
}
```

Ajouter dans `RabbitMQConfig` :
```java
@Bean
public SimpleRabbitListenerContainerFactory dlqListenerContainerFactory(ConnectionFactory cf) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(cf);
    factory.setConcurrentConsumers(1);
    factory.setMaxConcurrentConsumers(1);
    factory.setDefaultRequeueRejected(false);
    factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
    return factory;
}
```

---

### [C3] `PaymentTransactionWorkflowListener` — pas de retry

Détaillé dans `audit-outbox-email-pipeline.md §P1`. En résumé : `@Async + @TransactionalEventListener(AFTER_COMMIT)` — si le thread async échoue, la souscription reste `PENDING_ACTIVATION` sans aucun mécanisme de reprise.

**Correction** : publier un event outbox `SUBSCRIPTION_ACTIVATION_REQUESTED` dans `handleSucceededTransaction()` au lieu d'appeler `activate()` directement. Voir le document précédent pour le code complet.

---

### [W1] `BillingPaymentReminderWorker` — batch abandonné sur erreur

```java
// BillingPaymentReminderWorker.java
List<BillingDocument> candidates = documentRepository.findOverdueReminderCandidates(100);
for (BillingDocument doc : candidates) {
    billingEmailService.sendDocument(doc.getDocumentNumber());  // ← si exception ici
    count++;                                                    // ← les suivants ne sont JAMAIS traités
}
```

Si `sendDocument` lève une exception sur le document n°3, les documents 4 à 100 ne reçoivent jamais leur rappel.

**Correction** :
```java
for (BillingDocument doc : candidates) {
    try {
        billingEmailService.sendDocument(doc.getDocumentNumber());
        count++;
    } catch (Exception ex) {
        log.warn("Rappel échec pour document {}: {}", doc.getDocumentNumber(), ex.getMessage());
    }
}
```

---

### [W2] `contractGenerationExecutor` — pool déclaré mais inutilisé

```java
// AsyncConfig.java
@Bean(name = "contractGenerationExecutor")
public Executor contractGenerationExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("contract-generation-");
    ...
}
```

Aucune méthode dans le projet n'utilise `@Async("contractGenerationExecutor")`. La génération de contrats s'exécute soit dans l'`OutboxWorker` (thread scheduler), soit dans le pool `taskExecutor` par défaut.

**Options** :
1. Supprimer le bean (mort code)
2. L'utiliser dans `ContractGenerationOutboxEventProcessor.process()` si le processor doit être exécuté en parallèle (mais le worker outbox gère déjà la concurrence par batch)

---

### [W3] `PaymentIntentExpiryAlertWorker` — email direct hors pipeline

```java
// PaymentIntentExpiryAlertWorker.java
for (PaymentIntent intent : expiring) {
    try {
        // ...construit le HTML...
        emailSender.sendHtmlEmail(billingEmail, subject, html, EmailPriority.HIGH);  // ← OK, passe par RabbitMQ
    } catch (Exception ex) {
        log.warn("Failed to send expiry alert ...", ex);
    }
}
```

En fait l'email passe bien par `sendHtmlEmail()` qui est async via RabbitMQ. Mais le corps du message est construit **directement dans le worker** avec une interpolation de chaîne (pas de template Thymeleaf/Freemarker), créant une incohérence de style avec les autres emails.

**Recommandation** : extraire dans un template Thymeleaf `email/payment-intent-expiry-alert.html` et passer par `DefaultEmailSender` comme les autres notifiers.

---

### [W4] 19 workers sans `try/catch` au niveau du worker

Les workers suivants n'ont aucun `try/catch` dans leur méthode `@Scheduled`. Si le service délégué lève une exception :
- L'exception remonte au thread scheduler
- Spring log l'erreur (`ScheduledTaskWrapper`)
- Le worker ne s'exécute pas pour ce cycle (pas de retry immédiat)
- Le cycle suivant recommence normalement (pas de blocage permanent)

**Risque réel** : faible pour des exceptions ponctuelles (DB timeout, etc.), mais en cas d'exception systématique (bug de service, migration manquante), le worker silences sa défaillance dans les logs sans alerte visible.

**Workers concernés** (sélection des plus critiques) :
- `OutboxWorker` — si `outboxService.processPending()` lève une exception unchecked, tout le pipeline s'arrête pour ce cycle
- `SubscriptionRenewalWorker`, `PassRenewalWorker` — renouvellements silencieusement manqués
- `BookingNoShowWorker` — no-show non marqués

**Correction générique recommandée** :
```java
@Scheduled(...)
public void process() {
    try {
        service.doWork();
    } catch (Exception ex) {
        log.error("[WorkerName] Erreur lors du traitement: {}", ex.getMessage(), ex);
        // optionnel: adminNotifier.broadcastAlert(...)
    }
}
```

---

### [W5] `SubscriptionContractGenerationRepairWorker` — doublon partiel avec l'outbox

Ce worker re-scanne la base toutes les minutes pour trouver les souscriptions/pass/addons sans contrat depuis plus de 2 minutes. Depuis l'introduction de `ContractGenerationOutboxEventProcessor`, cette génération est déjà gérée par l'outbox avec retry automatique.

Le worker reste utile comme filet de sécurité (cas où l'outbox event n'aurait pas été créé), mais sa fréquence de 1 min et son minAge de 2 min créent une fenêtre de double-traitement potentielle si l'outbox est lent.

**Recommandation** : augmenter `minAgeSeconds` à 300 (5 min) pour laisser l'outbox gérer en premier.

---

## 4. Consumers RabbitMQ — Détail

### 4.1 `EmailConsumer` — Flux complet

```
@RabbitListener(queues="email.critical", containerFactory="criticalEmailListenerFactory")
consumeCritical(EmailRabbitMessage msg) → processEmail(msg)

processEmail(msg):
  1. rateLimiter.acquirePermission()           ← bloquant si rate limit atteint (800 req/60s)
  2. deliveryTracker.markSending(emailNumber)
  3. switch dispatch:
     - attachmentBytes → sendHtmlEmailWithPdfAttachmentBlocking()
     - inlineImageBytes → sendWithGraphInlineImageBlocking()
     - else → sendHtmlEmailBlocking()
  4. deliveryTracker.markSent(emailNumber)
  |  EXCEPTION:
  4. deliveryTracker.markFailed(emailNumber, msg)
     throw RuntimeException → message → email.dlq
```

**Paramètres des factories** :

| Factory | Queue | Consumers | Prefetch | Requeue |
|---|---|---|---|---|
| `criticalEmailListenerFactory` | `email.critical` | 4–8 | 5 | false |
| `highEmailListenerFactory` | `email.high` | 2–4 | 10 | false |
| `normalEmailListenerFactory` | `email.normal` | 1–2 | 20 | false |
| `bulkEmailListenerFactory` | `email.bulk` | 1–1 | 50 | false |

### 4.2 `NotificationInAppConsumer`

```
@RabbitListener(queues="notification.inapp")
consume(NotificationRabbitMessage msg):
  1. Guard: recipientEmail == null → ACK + return
  2. Build ClientNotificationPayload
  3. messagingTemplate.convertAndSendToUser(email, "/queue/notifications", payload)
  4. messagingTemplate.convertAndSendToUser(email, "/queue/unread-count", {INCREMENT})
  EXCEPTION → notification.dlq (sans consommateur)
```

### 4.3 `NotificationWebhookConsumer`

```
@RabbitListener(queues="notification.webhook")
consume(NotificationRabbitMessage msg):
  → webhookService.enqueueForEvent(...)  ← INSERT en base, traité par WebhookDeliveryWorker
  EXCEPTION → notification.dlq (sans consommateur)
```

---

## 5. Méthodes `@Async` — Catalogue et risques

### 5.1 Pool `taskExecutor` (4 core / 12 max / 200 queue)

| Classe | Méthode | Return | Gestion erreurs |
|---|---|---|---|
| `PaymentTransactionWorkflowListener` | `onPaymentTransactionWorkflow` | `void` | try/catch + log.error — **AUCUN RETRY** |
| `AuditServiceImpl` | `save(AuditLog)` | `void` | `@Transactional(REQUIRES_NEW)` — isolé |
| `BillingEmailServiceImpl` | `sendDocumentAsync` | `void` | try/catch interne — log warn |
| `BillingEmailServiceImpl` | `sendPaymentConfirmation` | `void` | try/catch interne — log warn |
| `NotificationServiceImpl` | `send(request)` | `CompletableFuture<?>` | Exception dans Future — `AsyncUncaughtExceptionHandler` |
| `KycAutomationServiceImpl` | `syncFromDocumentUpload` | `void` | `@Transactional(REQUIRES_NEW)` |
| `KycAutomationServiceImpl` | `syncFromDocumentReview` | `void` | `@Transactional(REQUIRES_NEW)` |
| `OttMailServiceImpl` | `sendOneTimeTokenMail` | `CompletableFuture<Boolean>` | retourne `false` sur exception |
| `PasswordResetMailServiceImpl` | `sendPasswordResetMail` | `CompletableFuture<Boolean>` | retourne `false` sur exception |
| `PasswordResetNotifier` | `sendPasswordChangedConfirmation` | `void` | try/catch interne |
| `PasswordResetNotifier` | `notifyAdminResetCompleted` | `void` | try/catch interne |
| `ContractEmailNotifier` | `notifyGenerated` | `void` | try/catch interne |
| `PassEmailNotifier` | `notify*` | `void` | try/catch interne |
| `VisitorEmailNotifier` | `sendInvitation` | `void` | try/catch interne |
| `SupportEmailServiceImpl` | `sendTicket*` | `void` | try/catch interne (pattern cohérent) |
| `TaskEmailServiceImpl` | `sendTask*` | `void` | try/catch interne |
| `CrmEmailServiceImpl` | `send*` | `void` | try/catch interne |

**`AsyncUncaughtExceptionHandler`** : toutes les exceptions non catchées dans les méthodes `@Async void` sont capturées par le handler global et loguées en `ERROR`.

### 5.2 `@TransactionalEventListener`

| Classe | Phase | Event | Async | Retry |
|---|---|---|---|---|
| `PaymentTransactionWorkflowListener` | `AFTER_COMMIT` | `PaymentTransactionWorkflowEvent` | **OUI** | **NON** |
| `CashSessionAnomalyDetectionListener` | `AFTER_COMMIT` | `CashSessionClosedEvent` | non | non (one-shot) |
| `EntitlementQuotaAlertListener` | `AFTER_COMMIT` | `EntitlementQuotaThresholdEvent` | non | non (one-shot) |

---

## 6. Candidats à la délégation RabbitMQ

### Priorité 1 — Impact direct sur la robustesse

#### `SubscriptionRenewalWorker` + `PassRenewalWorker`

**Problème actuel** : scan batch de toutes les souscriptions dues. Si le renouvellement d'une souscription échoue, les autres continuent (comportement OK via le service), mais il n'y a aucune retry automatique par item.

**Avec RabbitMQ** : à chaque `nextBillingDate` atteinte, publier un message `subscription.renewal` dans une queue dédiée. Chaque message = 1 renouvellement. Si échec → DLQ → replay ciblé.

```
Queue: subscription.renewal  (priority normale, TTL configurable)
Consumer: SubscriptionRenewalConsumer
  → lifecycleOperator.renewActive(subscriptionNumber)
  EXCEPTION → subscription.renewal.dlq
```

Le worker actuel devient un "feeder" léger qui publie les messages au lieu de tout traiter lui-même.

#### `DunningWorker`

**Problème actuel** : poll toutes les minutes sur `findDuePending(now)`. Chaque tentative dunning est indépendante.

**Avec RabbitMQ** : à la création d'une `PaymentDunningAttempt`, publier un message delayed (via plugin `rabbitmq-delayed-message-exchange` ou TTL + DLQ pattern) pour la date `scheduledAt`. Le consumer exécute la tentative exactement à l'heure prévue.

```
Exchange: dunning.delayed (x-delayed-message)
Queue: dunning.execute
Consumer: DunningConsumer → dunningService.executeAttempt(attemptId)
```

### Priorité 2 — Amélioration de la latence

#### `SupportInboundEmailWorker` → Microsoft Graph Webhook

**Problème actuel** : poll Graph API toutes les minutes. Latence de 0–60s sur la réception des réponses client.

**Avec Graph Change Notifications** : Microsoft Graph publie un webhook HTTP vers notre API à chaque email reçu. L'endpoint publie dans RabbitMQ.

```
POST /api/v1/webhooks/graph/mail → EmailRabbitPublisher → support.inbound
Consumer: SupportInboundEmailConsumer → ticketService.addEmailReply(...)
```

Bénéfice : latence de secondes (vs jusqu'à 1 minute), suppression du poll réseau permanent.

#### `BillingPaymentReminderWorker` → queue `billing.reminder`

Corriger [W1] + gagner la retryabilité par document :

```
Queue: billing.reminder
Producer: BillingPaymentReminderWorker (publie 1 message par document candidat)
Consumer: BillingReminderConsumer → billingEmailService.sendDocument(docNumber)
EXCEPTION → billing.reminder.dlq → replay admin
```

### Priorité 3 — Non prioritaire (traitements bulk non décomposables)

| Worker | Raison de non-migration |
|---|---|
| `BillingPeriodClosureWorker` | Agrégation globale par période — indivisible |
| `FiscalIntegrityWorker` | Vérification de la chaîne SEFC en une passe — indivisible |
| `PaymentReconciliationWorker` | Batch quotidien unique par design |
| `PaymentIntentExpiryWorker` | Requête bulk UPDATE — plus efficace en SQL direct |
| `WalletHoldExpiryWorker` | Requête bulk UPDATE |
| `EntitlementExpiryWorker` | Requête bulk UPDATE |
| `VisitorPassExpiryWorker` | `saveAll(expired)` — bulk UPDATE |
| `InventoryDailyWorker` | Détection avec déduplication intégrée, volume faible |

---

## 7. Checklist de correction

```
CRITIQUE
[ ] C1  — Déclarer ThreadPoolTaskScheduler (poolSize=8 minimum)
[ ] C2  — Ajouter @RabbitListener sur email.dlq, notification.dlq, notification.admin
[ ] C3  — Migrer activation post-paiement vers outbox (SUBSCRIPTION_ACTIVATION_REQUESTED)

ÉLEVÉ
[ ] W1  — Ajouter try/catch par item dans BillingPaymentReminderWorker
[ ] W2  — Supprimer contractGenerationExecutor ou le câbler dans @Async("contractGenerationExecutor")
[ ] W4  — Ajouter try/catch global dans les 19 workers sans protection
          Priorité : OutboxWorker, SubscriptionRenewalWorker, BookingNoShowWorker

MOYEN
[ ] W3  — Extraire le template email de PaymentIntentExpiryAlertWorker en Thymeleaf
[ ] W5  — Augmenter minAgeSeconds de SubscriptionContractGenerationRepairWorker à 300s

AMÉLIORATIONS
[ ] R1  — Migrer DunningWorker vers queue rabbitmq-delayed-message-exchange
[ ] R2  — Migrer SubscriptionRenewalWorker vers queue subscription.renewal
[ ] R3  — Migrer SupportInboundEmailWorker vers Graph Change Notifications webhook
[ ] R4  — Migrer BillingPaymentReminderWorker vers queue billing.reminder
```

---

## 8. Configuration de référence — `application.yml` workers

```yaml
# Outbox
app.outbox.worker:
  enabled: true
  fixed-delay-ms: 2000
  batch-size: 25

# Billing
bokati.billing.workers:
  overdue-delay-ms: 3600000
  fiscal-integrity-cron: "0 0 2 * * *"
  payment-reminder-cron: "0 0 8 * * *"
  schedule-overdue-delay-ms: 3600000
  daily-closure-cron: "0 30 23 * * *"
  monthly-closure-cron: "0 15 0 1 * *"
  annual-closure-cron: "0 0 1 1 1 *"

# Payment
bokati.payment.workers:
  dunning-delay-ms: 60000
  reconciliation-cron: "0 30 2 * * *"
  intent-expiry-delay-ms: 300000
  expiry-alert-delay-ms: 1800000
  wallet-hold-expiry-delay-ms: 300000

# Subscription
bokati.subscription.workers:
  renewal-delay-ms: 900000
  cancellation-delay-ms: 900000
  integrity-delay-ms: 1800000
  pass-expiry-delay-ms: 600000
  entitlement-expiry-delay-ms: 600000
  reservation-expiry-delay-ms: 300000
  contract-repair-delay-ms: 60000
  contract-repair-min-age-seconds: 120
  contract-repair-batch-size: 20
  rollover-delay-ms: 900000
  notification-dispatch-delay-ms: 60000
  addon-expiry-delay-ms: 900000
  promotion-expiry-delay-ms: 900000
  change-delay-ms: 900000

# Contract / Document
app.contract.worker.fixed-delay-ms: 3600000
app.document.worker.fixed-delay-ms: 3600000
app.document.retention.cron: "0 30 2 * * *"

# Support
bokati.support:
  inbound-poll-delay-ms: 60000
  auto-close-check-delay-ms: 3600000
  sla-check-delay-ms: 3600000
  csat-check-delay-ms: 3600000

# Notification
bokati.notification.worker.delay-ms: 30000
bokati.webhook.worker.delay-ms: 60000

# Inventory
inventory.worker:
  reservation-cron: "0 */30 * * * *"
  expiry-cron: "0 0 */2 * * *"
  slow-moving-cron: "0 0 */6 * * *"
  asset-cron: "0 0 */3 * * *"
```
