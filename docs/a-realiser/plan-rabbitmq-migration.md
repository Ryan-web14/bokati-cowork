

# Plan d'implémentation : Migration RabbitMQ pour Email & Notifications temps réel

> **Statut** : Draft  
> **Date** : 2026-06-22  
> **Cible** : Production-ready (Heroku + CloudAMQP) + Dev local (Docker Compose)  
> **Scope** : Infrastructure email (priorité, rate limiting, retry) + Notifications IN_APP temps réel (WebSocket STOMP relay)

---

## 1. Vue d'ensemble de l'architecture cible

### Avant (actuel)

```
Business Service → OutboxEvent (DB)
        ↓
OutboxWorker (polling 2s) → OutboxEventProcessor.process() [synchrone]
        ├── EMAIL: DefaultEmailSender.sendHtmlEmailBlocking() → Microsoft Graph API
        ├── IN_APP: Redis Pub/Sub → InAppNotificationRedisSubscriber → SimpleBroker WebSocket
        └── WEBHOOK: WebhookDeliveryWorker (polling 60s) → HTTP POST

Problèmes :
- Thread pool partagé 12 threads max
- Redis Pub/Sub lossy (fire & forget)
- WebSocket SimpleBroker = pas de clustering multi-dyno
- 30s de latence pour les notifications IN_APP (polling NotificationWorker)
- Aucun rate limiting Microsoft Graph
- Retry fixe 30s sans backoff exponentiel
```

### Après (cible)

```
Business Service → OutboxEvent (DB, transactionnel)
        ↓
OutboxWorker (polling 500ms) → publie dans RabbitMQ via RabbitTemplate
        ↓
    RabbitMQ (CloudAMQP en prod, Docker en dev)
        │
        ├── Exchange "email" (topic)
        │   ├── Queue "email.critical"   (routing: email.critical.#)  → 8 consumers
        │   ├── Queue "email.high"       (routing: email.high.#)      → 4 consumers
        │   ├── Queue "email.normal"     (routing: email.normal.#)    → 2 consumers
        │   └── Queue "email.bulk"       (routing: email.bulk.#)      → 1 consumer
        │                    ↓ (échec)
        │              DLX "email.dlx" → Queue "email.dlq"
        │
        ├── Exchange "notification" (topic)
        │   ├── Queue "notification.inapp"   (routing: notification.inapp.#)   → 4 consumers
        │   ├── Queue "notification.admin"   (routing: notification.admin.#)   → 2 consumers
        │   └── Queue "notification.webhook" (routing: notification.webhook.#) → 2 consumers
        │                    ↓ (échec)
        │              DLX "notification.dlx" → Queue "notification.dlq"
        │
        └── STOMP Relay (port 61613)
            ├── /user/{email}/queue/notifications      → per-user durable
            ├── /topic/admin/alerts                    → broadcast admin
            └── /topic/inventory/alerts                → broadcast inventory

Redis reste pour : sessions HTTP uniquement
```

---

## 2. Prérequis

### 2.1 Dépendance Maven

```xml
<!-- pom.xml — ajouter dans <dependencies> -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>

<!-- Pour les tests -->
<dependency>
    <groupId>org.springframework.amqp</groupId>
    <artifactId>spring-rabbit-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- Rate limiting (Resilience4j) -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-ratelimiter</artifactId>
</dependency>
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-retry</artifactId>
</dependency>

<!-- STOMP relay nécessite un client TCP (Reactor Netty) -->
<dependency>
    <groupId>io.projectreactor.netty</groupId>
    <artifactId>reactor-netty-core</artifactId>
</dependency>
```

### 2.2 Infrastructure — Dev (Docker Compose)

```yaml
# compose.yaml — ajouter ce service
  rabbitmq:
    image: rabbitmq:4-management-alpine
    container_name: bokati-rabbitmq
    ports:
      - "5672:5672"     # AMQP
      - "15672:15672"   # Management UI
      - "61613:61613"   # STOMP (pour WebSocket relay)
    environment:
      RABBITMQ_DEFAULT_USER: bokati
      RABBITMQ_DEFAULT_PASS: bokati_dev
      RABBITMQ_DEFAULT_VHOST: bokati
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
      - ./docker/rabbitmq/enabled_plugins:/etc/rabbitmq/enabled_plugins
    healthcheck:
      test: rabbitmq-diagnostics -q ping
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - bokati-network

# Ajouter dans volumes:
  rabbitmq_data:
```

```ini
# docker/rabbitmq/enabled_plugins (nouveau fichier)
[rabbitmq_management,rabbitmq_stomp,rabbitmq_web_stomp].
```

### 2.3 Infrastructure — Production (Heroku)

```bash
# Ajouter l'addon CloudAMQP (Little Lemur = $19/mois, suffisant pour 30k users)
heroku addons:create cloudamqp:lemur --app bokati-cowork

# CloudAMQP expose automatiquement CLOUDAMQP_URL dans les env vars Heroku
# Format : amqp://user:pass@host/vhost

# Pour le STOMP relay, CloudAMQP expose le port 61614 (STOMP over TLS)
# Host STOMP : même host que AMQP mais port 61614
```

---

## 3. Configuration Spring

### 3.1 application.yml (base commune)

```yaml
# Ajouter dans application.yml

# --- RabbitMQ Topology ---
bokati:
  rabbitmq:
    email:
      exchange: email
      dlx-exchange: email.dlx
      queues:
        critical:
          name: email.critical
          routing-key: email.critical.#
          concurrency: 4-8
          prefetch: 5
          retry-max: 10
          retry-initial-interval-ms: 1000
          retry-multiplier: 2.0
          retry-max-interval-ms: 60000
        high:
          name: email.high
          routing-key: email.high.#
          concurrency: 2-4
          prefetch: 10
          retry-max: 8
          retry-initial-interval-ms: 2000
          retry-multiplier: 2.0
          retry-max-interval-ms: 120000
        normal:
          name: email.normal
          routing-key: email.normal.#
          concurrency: 1-2
          prefetch: 20
          retry-max: 5
          retry-initial-interval-ms: 5000
          retry-multiplier: 2.0
          retry-max-interval-ms: 300000
        bulk:
          name: email.bulk
          routing-key: email.bulk.#
          concurrency: 1-1
          prefetch: 50
          retry-max: 3
          retry-initial-interval-ms: 30000
          retry-multiplier: 3.0
          retry-max-interval-ms: 3600000
      dlq: email.dlq
    notification:
      exchange: notification
      dlx-exchange: notification.dlx
      queues:
        inapp:
          name: notification.inapp
          routing-key: notification.inapp.#
          concurrency: 2-4
          prefetch: 20
        admin:
          name: notification.admin
          routing-key: notification.admin.#
          concurrency: 1-2
          prefetch: 10
        webhook:
          name: notification.webhook
          routing-key: notification.webhook.#
          concurrency: 1-2
          prefetch: 10
      dlq: notification.dlq
  graph-api:
    rate-limit:
      limit-for-period: 800
      limit-refresh-period-ms: 60000
      timeout-ms: 30000
```

### 3.2 application-dev.yml

```yaml
# RabbitMQ local Docker
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: bokati
    password: bokati_dev
    virtual-host: bokati

# WebSocket STOMP relay via RabbitMQ local
bokati:
  websocket:
    use-stomp-relay: true
    stomp:
      relay-host: localhost
      relay-port: 61613
      client-login: bokati
      client-passcode: bokati_dev
      system-login: bokati
      system-passcode: bokati_dev
```

### 3.3 application-prod.yml

```yaml
# CloudAMQP — les valeurs sont extraites de CLOUDAMQP_URL
spring:
  rabbitmq:
    addresses: ${CLOUDAMQP_URL}

# STOMP relay — CloudAMQP STOMP over TLS
bokati:
  websocket:
    use-stomp-relay: true
    stomp:
      relay-host: ${CLOUDAMQP_STOMP_HOST}
      relay-port: ${CLOUDAMQP_STOMP_PORT:61614}
      client-login: ${CLOUDAMQP_STOMP_USER}
      client-passcode: ${CLOUDAMQP_STOMP_PASS}
      system-login: ${CLOUDAMQP_STOMP_USER}
      system-passcode: ${CLOUDAMQP_STOMP_PASS}
```

---

## 4. Fichiers à créer

### 4.1 Configuration RabbitMQ — Topology

**Fichier** : `core/configuration/RabbitMQConfig.java`

```java
package com.sni.bokaticowork.core.configuration;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ──────────────────────────── Exchanges ────────────────────────────

    @Bean
    public TopicExchange emailExchange() {
        return ExchangeBuilder.topicExchange("email").durable(true).build();
    }

    @Bean
    public TopicExchange emailDlxExchange() {
        return ExchangeBuilder.topicExchange("email.dlx").durable(true).build();
    }

    @Bean
    public TopicExchange notificationExchange() {
        return ExchangeBuilder.topicExchange("notification").durable(true).build();
    }

    @Bean
    public TopicExchange notificationDlxExchange() {
        return ExchangeBuilder.topicExchange("notification.dlx").durable(true).build();
    }

    // ──────────────────────────── Email Queues ────────────────────────

    @Bean
    public Queue emailCriticalQueue() {
        return QueueBuilder.durable("email.critical")
                .withArgument("x-dead-letter-exchange", "email.dlx")
                .withArgument("x-dead-letter-routing-key", "email.dlq")
                .build();
    }

    @Bean
    public Queue emailHighQueue() {
        return QueueBuilder.durable("email.high")
                .withArgument("x-dead-letter-exchange", "email.dlx")
                .withArgument("x-dead-letter-routing-key", "email.dlq")
                .build();
    }

    @Bean
    public Queue emailNormalQueue() {
        return QueueBuilder.durable("email.normal")
                .withArgument("x-dead-letter-exchange", "email.dlx")
                .withArgument("x-dead-letter-routing-key", "email.dlq")
                .build();
    }

    @Bean
    public Queue emailBulkQueue() {
        return QueueBuilder.durable("email.bulk")
                .withArgument("x-dead-letter-exchange", "email.dlx")
                .withArgument("x-dead-letter-routing-key", "email.dlq")
                .build();
    }

    @Bean
    public Queue emailDlq() {
        return QueueBuilder.durable("email.dlq").build();
    }

    // ──────────────────────────── Notification Queues ─────────────────

    @Bean
    public Queue notificationInappQueue() {
        return QueueBuilder.durable("notification.inapp")
                .withArgument("x-dead-letter-exchange", "notification.dlx")
                .withArgument("x-dead-letter-routing-key", "notification.dlq")
                .build();
    }

    @Bean
    public Queue notificationAdminQueue() {
        return QueueBuilder.durable("notification.admin")
                .withArgument("x-dead-letter-exchange", "notification.dlx")
                .withArgument("x-dead-letter-routing-key", "notification.dlq")
                .build();
    }

    @Bean
    public Queue notificationWebhookQueue() {
        return QueueBuilder.durable("notification.webhook")
                .withArgument("x-dead-letter-exchange", "notification.dlx")
                .withArgument("x-dead-letter-routing-key", "notification.dlq")
                .build();
    }

    @Bean
    public Queue notificationDlq() {
        return QueueBuilder.durable("notification.dlq").build();
    }

    // ──────────────────────────── Bindings ────────────────────────────

    @Bean
    public Binding emailCriticalBinding() {
        return BindingBuilder.bind(emailCriticalQueue()).to(emailExchange()).with("email.critical.#");
    }

    @Bean
    public Binding emailHighBinding() {
        return BindingBuilder.bind(emailHighQueue()).to(emailExchange()).with("email.high.#");
    }

    @Bean
    public Binding emailNormalBinding() {
        return BindingBuilder.bind(emailNormalQueue()).to(emailExchange()).with("email.normal.#");
    }

    @Bean
    public Binding emailBulkBinding() {
        return BindingBuilder.bind(emailBulkQueue()).to(emailExchange()).with("email.bulk.#");
    }

    @Bean
    public Binding emailDlqBinding() {
        return BindingBuilder.bind(emailDlq()).to(emailDlxExchange()).with("email.dlq");
    }

    @Bean
    public Binding notificationInappBinding() {
        return BindingBuilder.bind(notificationInappQueue()).to(notificationExchange()).with("notification.inapp.#");
    }

    @Bean
    public Binding notificationAdminBinding() {
        return BindingBuilder.bind(notificationAdminQueue()).to(notificationExchange()).with("notification.admin.#");
    }

    @Bean
    public Binding notificationWebhookBinding() {
        return BindingBuilder.bind(notificationWebhookQueue()).to(notificationExchange()).with("notification.webhook.#");
    }

    @Bean
    public Binding notificationDlqBinding() {
        return BindingBuilder.bind(notificationDlq()).to(notificationDlxExchange()).with("notification.dlq");
    }

    // ──────────────────────────── Message Converter ───────────────────

    @Bean
    public MessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    // ──────────────────── Listener Container Factories ────────────────

    @Bean
    public SimpleRabbitListenerContainerFactory criticalEmailListenerFactory(
            ConnectionFactory connectionFactory, MessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        factory.setConcurrentConsumers(4);
        factory.setMaxConcurrentConsumers(8);
        factory.setPrefetchCount(5);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory highEmailListenerFactory(
            ConnectionFactory connectionFactory, MessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(4);
        factory.setPrefetchCount(10);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory normalEmailListenerFactory(
            ConnectionFactory connectionFactory, MessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(2);
        factory.setPrefetchCount(20);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory bulkEmailListenerFactory(
            ConnectionFactory connectionFactory, MessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(1);
        factory.setPrefetchCount(50);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory notificationListenerFactory(
            ConnectionFactory connectionFactory, MessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(4);
        factory.setPrefetchCount(20);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
```

### 4.2 Enum de priorité email

**Fichier** : `core/communication/mailService/enums/EmailPriority.java`

```java
package com.sni.bokaticowork.core.communication.mailService.enums;

public enum EmailPriority {

    CRITICAL("email.critical"),
    HIGH("email.high"),
    NORMAL("email.normal"),
    BULK("email.bulk");

    private final String routingKey;

    EmailPriority(String routingKey) {
        this.routingKey = routingKey;
    }

    public String routingKey() {
        return routingKey;
    }

    /**
     * Détermine la priorité d'un email en fonction de son event type.
     */
    public static EmailPriority fromEventType(String eventType) {
        if (eventType == null) return NORMAL;
        String upper = eventType.toUpperCase();

        // OTP, password reset, email confirmation = critique (< 5s)
        if (upper.contains("OTT") || upper.contains("OTP")
                || upper.contains("PASSWORD_RESET")
                || upper.contains("EMAIL_CONFIRMATION")
                || upper.contains("EMAIL_VERIFICATION")) {
            return CRITICAL;
        }

        // Paiement, contrat, signature = haute priorité (< 30s)
        if (upper.contains("PAYMENT") || upper.contains("CONTRACT_SIGNING")
                || upper.contains("BILLING_DOCUMENT")
                || upper.contains("REFUND")) {
            return HIGH;
        }

        // Reminders, KYC expiry, admin alerts = bulk (peut attendre)
        if (upper.contains("REMINDER") || upper.contains("EXPIRY")
                || upper.contains("ADMIN_ALERT") || upper.contains("REPORT")) {
            return BULK;
        }

        return NORMAL;
    }
}
```

### 4.3 Message DTO pour RabbitMQ

**Fichier** : `core/communication/mailService/dto/EmailMessage.java`

```java
package com.sni.bokaticowork.core.communication.mailService.dto;

import com.sni.bokaticowork.core.communication.mailService.enums.EmailPriority;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

public record EmailMessage(
        String emailNumber,
        String to,
        String from,
        String subject,
        String htmlContent,
        EmailPriority priority,
        String eventType,
        String aggregateType,
        String aggregateId,
        String relatedType,
        String relatedCode,
        String attachmentName,
        byte[] attachmentBytes,
        String inlineImageContentId,
        byte[] inlineImageBytes,
        int attemptCount,
        Instant createdAt
) implements Serializable {

    public String routingKey() {
        return priority().routingKey() + "." + safeEventType();
    }

    private String safeEventType() {
        return eventType != null ? eventType.toLowerCase().replace("_", ".") : "generic";
    }
}
```

**Fichier** : `core/communication/mailService/dto/NotificationRabbitMessage.java`

```java
package com.sni.bokaticowork.core.communication.mailService.dto;

import java.io.Serializable;
import java.time.Instant;

public record NotificationRabbitMessage(
        String notificationNumber,
        String channel,       // IN_APP, WEBHOOK
        String recipientEmail,
        String recipientName,
        String eventType,
        String aggregateType,
        String aggregateId,
        String subject,
        String payloadJson,
        String templateCode,
        String templateName,
        Instant createdAt
) implements Serializable {

    public String routingKey() {
        return switch (channel) {
            case "IN_APP" -> "notification.inapp." + safeEventType();
            case "WEBHOOK" -> "notification.webhook." + safeEventType();
            default -> "notification.admin." + safeEventType();
        };
    }

    private String safeEventType() {
        return eventType != null ? eventType.toLowerCase().replace("_", ".") : "generic";
    }
}
```

### 4.4 Publisher RabbitMQ

**Fichier** : `core/communication/mailService/service/EmailRabbitPublisher.java`

```java
package com.sni.bokaticowork.core.communication.mailService.service;

import com.sni.bokaticowork.core.communication.mailService.dto.EmailMessage;
import com.sni.bokaticowork.core.communication.mailService.dto.NotificationRabbitMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailRabbitPublisher {

    private static final String EMAIL_EXCHANGE = "email";
    private static final String NOTIFICATION_EXCHANGE = "notification";

    private final RabbitTemplate rabbitTemplate;

    public void publishEmail(EmailMessage message) {
        rabbitTemplate.convertAndSend(EMAIL_EXCHANGE, message.routingKey(), message);
        log.debug("Published email {} to {} with priority {}",
                message.emailNumber(), message.to(), message.priority());
    }

    public void publishNotification(NotificationRabbitMessage message) {
        rabbitTemplate.convertAndSend(NOTIFICATION_EXCHANGE, message.routingKey(), message);
        log.debug("Published notification {} to channel {}",
                message.notificationNumber(), message.channel());
    }
}
```

### 4.5 Consumers RabbitMQ

**Fichier** : `core/communication/mailService/consumer/EmailConsumer.java`

```java
package com.sni.bokaticowork.core.communication.mailService.consumer;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.core.communication.mailService.dto.EmailMessage;
import com.sni.bokaticowork.core.communication.mailService.service.EmailDeliveryTracker;
import com.sni.bokaticowork.core.communication.mailService.service.GraphApiRateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailConsumer {

    private final DefaultEmailSender emailSender;
    private final EmailDeliveryTracker deliveryTracker;
    private final GraphApiRateLimiter rateLimiter;

    @RabbitListener(queues = "email.critical", containerFactory = "criticalEmailListenerFactory")
    public void consumeCritical(EmailMessage message) {
        processEmail(message);
    }

    @RabbitListener(queues = "email.high", containerFactory = "highEmailListenerFactory")
    public void consumeHigh(EmailMessage message) {
        processEmail(message);
    }

    @RabbitListener(queues = "email.normal", containerFactory = "normalEmailListenerFactory")
    public void consumeNormal(EmailMessage message) {
        processEmail(message);
    }

    @RabbitListener(queues = "email.bulk", containerFactory = "bulkEmailListenerFactory")
    public void consumeBulk(EmailMessage message) {
        processEmail(message);
    }

    private void processEmail(EmailMessage message) {
        rateLimiter.acquirePermission();

        deliveryTracker.markSending(message.emailNumber());
        try {
            boolean sent;
            if (message.attachmentBytes() != null && message.attachmentBytes().length > 0) {
                sent = emailSender.sendHtmlEmailWithPdfAttachmentBlocking(
                        message.to(), message.subject(), message.htmlContent(),
                        message.attachmentName(), message.attachmentBytes());
            } else if (message.inlineImageBytes() != null && message.inlineImageBytes().length > 0) {
                sent = emailSender.sendWithGraphInlineImageBlocking(
                        message.to(), message.subject(), message.htmlContent(),
                        message.inlineImageContentId(), message.inlineImageBytes());
            } else {
                sent = emailSender.sendHtmlEmailBlocking(message.to(), message.subject(), message.htmlContent());
            }

            if (sent) {
                deliveryTracker.markSent(message.emailNumber());
                log.info("Email {} sent to {} [priority={}]",
                        message.emailNumber(), message.to(), message.priority());
            } else {
                throw new IllegalStateException("Email sender returned false");
            }
        } catch (Exception ex) {
            deliveryTracker.markFailed(message.emailNumber(), ex.getMessage());
            // Lancer l'exception pour que RabbitMQ retente via DLX/retry
            throw new RuntimeException("Email delivery failed: " + ex.getMessage(), ex);
        }
    }
}
```

**Fichier** : `features/notification/consumer/NotificationInAppConsumer.java`

```java
package com.sni.bokaticowork.features.notification.consumer;

import com.sni.bokaticowork.core.communication.mailService.dto.NotificationRabbitMessage;
import com.sni.bokaticowork.core.event.WebSocketTopics;
import com.sni.bokaticowork.core.event.dto.ClientNotificationPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationInAppConsumer {

    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = "notification.inapp", containerFactory = "notificationListenerFactory")
    public void consume(NotificationRabbitMessage message) {
        if (message.recipientEmail() == null) {
            log.warn("IN_APP notification {} missing recipientEmail, skipping",
                    message.notificationNumber());
            return;
        }

        ClientNotificationPayload payload = new ClientNotificationPayload(
                message.notificationNumber(),
                message.subject(),
                message.eventType(),
                message.aggregateType(),
                message.aggregateId(),
                message.payloadJson(),
                message.createdAt(),
                false
        );

        // Push via STOMP relay (RabbitMQ-backed, durable, multi-dyno)
        messagingTemplate.convertAndSendToUser(
                message.recipientEmail(),
                WebSocketTopics.USER_NOTIFICATIONS,
                payload
        );

        messagingTemplate.convertAndSendToUser(
                message.recipientEmail(),
                WebSocketTopics.USER_UNREAD_COUNT,
                Map.of("action", "INCREMENT")
        );

        log.debug("Pushed IN_APP notification {} to user {}",
                message.notificationNumber(), message.recipientEmail());
    }
}
```

**Fichier** : `features/notification/consumer/NotificationWebhookConsumer.java`

```java
package com.sni.bokaticowork.features.notification.consumer;

import com.sni.bokaticowork.core.communication.mailService.dto.NotificationRabbitMessage;
import com.sni.bokaticowork.features.notification.service.interfaces.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWebhookConsumer {

    private final WebhookService webhookService;

    @RabbitListener(queues = "notification.webhook", containerFactory = "notificationListenerFactory")
    public void consume(NotificationRabbitMessage message) {
        webhookService.enqueueForEvent(
                message.eventType(),
                message.aggregateType(),
                message.aggregateId(),
                message.payloadJson()
        );
        log.debug("Webhook notification {} dispatched", message.notificationNumber());
    }
}
```

### 4.6 Rate Limiter pour Microsoft Graph API

**Fichier** : `core/communication/mailService/service/GraphApiRateLimiter.java`

```java
package com.sni.bokaticowork.core.communication.mailService.service;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
public class GraphApiRateLimiter {

    @Value("${bokati.graph-api.rate-limit.limit-for-period:800}")
    private int limitForPeriod;

    @Value("${bokati.graph-api.rate-limit.limit-refresh-period-ms:60000}")
    private long refreshPeriodMs;

    @Value("${bokati.graph-api.rate-limit.timeout-ms:30000}")
    private long timeoutMs;

    private RateLimiter rateLimiter;

    @PostConstruct
    public void init() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(limitForPeriod)
                .limitRefreshPeriod(Duration.ofMillis(refreshPeriodMs))
                .timeoutDuration(Duration.ofMillis(timeoutMs))
                .build();
        rateLimiter = RateLimiter.of("microsoft-graph", config);
        log.info("Microsoft Graph rate limiter initialized: {}/{}ms",
                limitForPeriod, refreshPeriodMs);
    }

    public void acquirePermission() {
        rateLimiter.acquirePermission();
    }

    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }
}
```

---

## 5. Fichiers à modifier

### 5.1 WebSocketConfig.java — STOMP Relay

**Fichier** : `core/configuration/WebSocketConfig.java`

**Changement** : Remplacer `enableSimpleBroker()` par `enableStompBrokerRelay()` conditionnel.

```java
package com.sni.bokaticowork.core.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor authInterceptor;
    private final WebSocketSubscriptionInterceptor subscriptionInterceptor;

    @Value("${bokati.websocket.use-stomp-relay:false}")
    private boolean useStompRelay;

    @Value("${bokati.websocket.stomp.relay-host:localhost}")
    private String relayHost;

    @Value("${bokati.websocket.stomp.relay-port:61613}")
    private int relayPort;

    @Value("${bokati.websocket.stomp.client-login:guest}")
    private String clientLogin;

    @Value("${bokati.websocket.stomp.client-passcode:guest}")
    private String clientPasscode;

    @Value("${bokati.websocket.stomp.system-login:guest}")
    private String systemLogin;

    @Value("${bokati.websocket.stomp.system-passcode:guest}")
    private String systemPasscode;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        if (useStompRelay) {
            registry.enableStompBrokerRelay("/topic", "/queue")
                    .setRelayHost(relayHost)
                    .setRelayPort(relayPort)
                    .setClientLogin(clientLogin)
                    .setClientPasscode(clientPasscode)
                    .setSystemLogin(systemLogin)
                    .setSystemPasscode(systemPasscode)
                    .setSystemHeartbeatSendInterval(10_000)
                    .setSystemHeartbeatReceiveInterval(10_000);
        } else {
            registry.enableSimpleBroker("/topic", "/queue");
        }
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setHeartbeatTime(25_000); // Heroku coupe à 55s, heartbeat à 25s
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration
                .setMessageSizeLimit(64 * 1024)
                .setSendBufferSizeLimit(512 * 1024)
                .setSendTimeLimit(20_000);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authInterceptor, subscriptionInterceptor);
        registration.taskExecutor()
                .corePoolSize(4)
                .maxPoolSize(16)
                .queueCapacity(200);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.taskExecutor()
                .corePoolSize(4)
                .maxPoolSize(20)
                .queueCapacity(500);
    }
}
```

### 5.2 OutboxServiceImpl.java — Publier dans RabbitMQ au lieu de traiter synchrone

**Fichier** : `core/outbox/service/implementation/OutboxServiceImpl.java`

**Changement dans `processPending()`** : Au lieu d'appeler `processor.process(event)` synchrone, le processor publie dans RabbitMQ. Le pattern outbox reste intact (garantie transactionnelle), mais le traitement est délégué à RabbitMQ.

Aucun changement structurel dans OutboxServiceImpl — le changement est dans les **processors** qui publient dans RabbitMQ au lieu d'appeler les services synchrones.

### 5.3 NotificationOutboxEventProcessor.java — Publier dans RabbitMQ

**Fichier** : `core/outbox/service/implementation/NotificationOutboxEventProcessor.java`

```java
package com.sni.bokaticowork.core.outbox.service.implementation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.communication.mailService.dto.EmailMessage;
import com.sni.bokaticowork.core.communication.mailService.dto.NotificationRabbitMessage;
import com.sni.bokaticowork.core.communication.mailService.enums.EmailPriority;
import com.sni.bokaticowork.core.communication.mailService.service.EmailRabbitPublisher;
import com.sni.bokaticowork.core.outbox.model.OutboxEvent;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxEventProcessor;
import com.sni.bokaticowork.features.notification.service.support.NotificationDispatchSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class NotificationOutboxEventProcessor implements OutboxEventProcessor {

    private static final Set<String> SUPPORTED_AGGREGATES = Set.of(
            "NOTIFICATION", "BOOKING", "SUBSCRIPTION", "SUBSCRIPTION_PASS",
            "PAYMENT", "BILLING", "INVOICE", "QUOTE",
            "WALLET", "CASH_REGISTER", "INVENTORY", "ADMIN"
    );

    private final ObjectMapper objectMapper;
    private final NotificationDispatchSupport dispatchSupport;
    private final EmailRabbitPublisher rabbitPublisher;

    @Override
    public boolean supports(OutboxEvent event) {
        return (event.getEventType() != null && event.getEventType().startsWith("NOTIFICATION_"))
                || (event.getAggregateType() != null
                    && SUPPORTED_AGGREGATES.contains(event.getAggregateType().toUpperCase()));
    }

    @Override
    public void process(OutboxEvent event) {
        Map<String, Object> payload = readPayload(event);

        // Créer la NotificationMessage en DB (transactionnel) via le support existant
        var response = dispatchSupport.createForEvent(
                event.getEventType(),
                event.getAggregateType(),
                event.getAggregateId(),
                payload
        );

        // Publier dans RabbitMQ pour traitement asynchrone
        if (StringUtils.hasText(response.notificationNumber())) {
            // Le dispatch support a déjà sauvé le NotificationMessage en DB
            // On publie un message léger dans RabbitMQ pour le consumer
            publishToRabbit(event, payload, response.notificationNumber());
        }
    }

    private void publishToRabbit(OutboxEvent event, Map<String, Object> payload,
                                  String notificationNumber) {
        String channel = stringFromPayload(payload, "channel");
        if (!StringUtils.hasText(channel)) {
            channel = "EMAIL";
        }

        NotificationRabbitMessage message = new NotificationRabbitMessage(
                notificationNumber,
                channel,
                stringFromPayload(payload, "recipientEmail"),
                stringFromPayload(payload, "recipientName"),
                event.getEventType(),
                event.getAggregateType(),
                event.getAggregateId(),
                stringFromPayload(payload, "subject"),
                event.getPayload(),
                stringFromPayload(payload, "templateCode"),
                stringFromPayload(payload, "templateName"),
                Instant.now()
        );

        rabbitPublisher.publishNotification(message);
    }

    private String stringFromPayload(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value != null ? value.toString() : null;
    }

    private Map<String, Object> readPayload(OutboxEvent event) {
        try {
            return objectMapper.readValue(event.getPayload(),
                    new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to read notification outbox payload", ex);
        }
    }
}
```

### 5.4 NotificationDispatchSupport.java — Publier dans RabbitMQ au lieu de Redis Pub/Sub

**Fichier** : `features/notification/service/support/NotificationDispatchSupport.java`

**Changements** :
- Méthode `process()` : pour IN_APP, publier dans RabbitMQ au lieu de Redis Pub/Sub
- Méthode `process()` : pour EMAIL, publier dans la queue email RabbitMQ avec priorité
- Supprimer la dépendance à `RedisEventPublisher`

```java
// Dans la méthode process(), remplacer le bloc IN_APP :

// AVANT :
// if (message.getChannel() == NotificationChannel.IN_APP) {
//     markSent(message);
//     publishInAppToRedis(message);
//     return;
// }

// APRÈS :
private final EmailRabbitPublisher rabbitPublisher;

private void process(NotificationMessage message) {
    message.setStatus(NotificationDeliveryStatus.PROCESSING);
    messageRepository.saveAndFlush(message);

    if (message.getChannel() == NotificationChannel.IN_APP) {
        markSent(message);
        publishInAppToRabbit(message);
        return;
    }
    if (message.getChannel() == NotificationChannel.WEBHOOK) {
        markSent(message);
        return;
    }
    if (!StringUtils.hasText(message.getRecipientEmail())) {
        fail(message, "Recipient email is required for email notification");
        return;
    }

    // Publier dans RabbitMQ avec priorité au lieu d'envoyer synchrone
    publishEmailToRabbit(message);
    markSent(message); // Le consumer gérera le statut final
}

private void publishInAppToRabbit(NotificationMessage message) {
    try {
        NotificationRabbitMessage rabbitMsg = new NotificationRabbitMessage(
                message.getNotificationNumber(),
                "IN_APP",
                message.getRecipientEmail(),
                message.getRecipientName(),
                message.getEventType(),
                message.getAggregateType(),
                message.getAggregateId(),
                message.getSubject(),
                message.getPayloadJson(),
                message.getTemplateCode(),
                message.getTemplateName(),
                message.getCreatedAt()
        );
        rabbitPublisher.publishNotification(rabbitMsg);
    } catch (Exception ex) {
        log.warn("Failed to publish IN_APP notification {} to RabbitMQ: {}",
                message.getNotificationNumber(), ex.getMessage());
    }
}

private void publishEmailToRabbit(NotificationMessage message) {
    Map<String, Object> variables = payloadSupport.toMap(message.getPayloadJson());
    variables.putIfAbsent("recipientName", defaultText(message.getRecipientName(), "client"));
    variables.putIfAbsent("eventType", message.getEventType());
    variables.putIfAbsent("subject", message.getSubject());

    String html = renderBody(message, variables);
    EmailPriority priority = EmailPriority.fromEventType(message.getEventType());

    EmailMessage emailMsg = new EmailMessage(
            message.getNotificationNumber(),
            message.getRecipientEmail(),
            null, // from = default
            message.getSubject(),
            html,
            priority,
            message.getEventType(),
            message.getAggregateType(),
            message.getAggregateId(),
            null, null,    // relatedType, relatedCode
            null, null,    // attachmentName, attachmentBytes
            null, null,    // inlineImage
            0,
            Instant.now()
    );

    rabbitPublisher.publishEmail(emailMsg);
}
```

### 5.5 OttMailServiceImpl.java — Email OTP via queue CRITICAL

**Fichier** : `core/communication/mailService/implementation/OttMailServiceImpl.java`

**Changement** : Au lieu d'appeler `emailSender.sendHtmlEmail()` async, publier dans la queue `email.critical`.

```java
// Injecter EmailRabbitPublisher
// Dans sendOneTimeTokenMail() :
//   1. Rendre le template HTML (existant)
//   2. Créer EmailMessage avec priority=CRITICAL
//   3. Publier via rabbitPublisher.publishEmail()
//   4. Retourner CompletableFuture.completedFuture(true)
```

### 5.6 PasswordResetMailServiceImpl.java — Email Password Reset via queue CRITICAL

Même pattern que OttMailServiceImpl — publier dans `email.critical`.

### 5.7 RedisPubSubConfig.java — Simplifier

**Fichier** : `core/configuration/RedisPubSubConfig.java`

**Changement** : Supprimer les subscribers liés aux notifications (InAppNotificationRedisSubscriber, AdminAlertRedisSubscriber, InventoryAlertRedisSubscriber). Redis Pub/Sub n'est plus utilisé pour les notifications.

Option 1 : Supprimer la classe entièrement si Redis n'est plus utilisé pour le pub/sub.
Option 2 : La garder si d'autres fonctionnalités futures ont besoin de Redis Pub/Sub.

**Recommandation** : Supprimer `RedisPubSubConfig.java` et les 3 subscribers Redis. Migrer les alertes admin et inventory vers RabbitMQ aussi.

### 5.8 AsyncConfig.java — Ajuster les pools

**Fichier** : `core/aysnc/AsyncConfig.java`

**Changement** : Le taskExecutor n'est plus le goulot d'étranglement pour les emails (RabbitMQ gère la concurrence). Garder le pool pour les autres tâches async mais réduire la pression.

```java
// Le pool principal peut rester à 4-12 pour les tâches non-email
// Les emails sont gérés par les consumers RabbitMQ avec leurs propres thread pools
```

### 5.9 OutboxWorker.java — Réduire le polling

**Fichier** : `core/outbox/worker/OutboxWorker.java`

```yaml
# application.yml — changer les defaults
app:
  outbox:
    worker:
      fixed-delay-ms: 500    # 500ms au lieu de 2000ms
      batch-size: 100         # 100 au lieu de 25
```

### 5.10 NotificationWorker.java — Réduire le polling

```yaml
bokati:
  notification:
    worker:
      delay-ms: 2000    # 2s au lieu de 30s
      batch-size: 50     # 50 au lieu de 25
```

---

## 6. Fichiers à supprimer (après migration)

| Fichier | Raison |
|---------|--------|
| `core/event/subscriber/InAppNotificationRedisSubscriber.java` | Remplacé par `NotificationInAppConsumer` |
| `core/event/subscriber/AdminAlertRedisSubscriber.java` | Remplacé par publication directe via STOMP relay |
| `core/event/subscriber/InventoryAlertRedisSubscriber.java` | Remplacé par publication directe via STOMP relay |
| `core/event/RedisEventPublisher.java` | Plus nécessaire — RabbitMQ remplace Redis Pub/Sub |
| `core/event/RedisChannelSubscriber.java` | Interface obsolète |
| `core/event/RedisChannels.java` | Constantes obsolètes |
| `core/configuration/RedisPubSubConfig.java` | Config Redis Pub/Sub obsolète |

**Note** : Redis reste dans le stack pour les sessions HTTP (`spring-session-data-redis`). Ne pas supprimer la dépendance Redis ni la configuration de connexion Redis.

---

## 7. Migration de données (Flyway)

### V170__add_email_priority_column.sql

```sql
-- Ajouter la colonne priority à notification_message pour le tracking
ALTER TABLE notification_message
    ADD COLUMN IF NOT EXISTS priority VARCHAR(20) DEFAULT 'NORMAL';

-- Index pour les requêtes de monitoring par priorité
CREATE INDEX IF NOT EXISTS idx_notification_message_priority
    ON notification_message(priority, status);

-- Ajouter la colonne priority à email_delivery_log
ALTER TABLE email_delivery_log
    ADD COLUMN IF NOT EXISTS priority VARCHAR(20) DEFAULT 'NORMAL';

CREATE INDEX IF NOT EXISTS idx_email_delivery_log_priority
    ON email_delivery_log(priority, status);
```

---

## 8. Diagramme de flux complet

### 8.1 Flux Email (avec priorité)

```
┌──────────────────────────────────────────────────────────────────────┐
│ BUSINESS SERVICE (Booking, Payment, Contract, etc.)                  │
│                                                                      │
│  bookingService.create()                                             │
│      ↓                                                               │
│  outboxService.publish("BOOKING_CONFIRMED", "BOOKING", id, payload) │
│      ↓ (dans la même transaction JPA)                                │
│  INSERT INTO outbox_event (status=PENDING)                           │
│  COMMIT                                                              │
└──────────────────────────────────────────────────────────────────────┘
        ↓ (500ms max)
┌──────────────────────────────────────────────────────────────────────┐
│ OUTBOX WORKER (polling 500ms, batch 100)                             │
│                                                                      │
│  claimPending(100) → SELECT ... WHERE status IN (PENDING, FAILED)    │
│  UPDATE status = PROCESSING, available_at = now + 5min               │
│      ↓                                                               │
│  NotificationOutboxEventProcessor.process(event)                     │
│      ↓                                                               │
│  1. dispatchSupport.createForEvent() → INSERT notification_message   │
│  2. Déterminer EmailPriority.fromEventType("BOOKING_CONFIRMED")      │
│     → NORMAL                                                         │
│  3. rabbitPublisher.publishEmail(EmailMessage{priority=NORMAL})      │
│      ↓                                                               │
│  markPublished(eventId)                                              │
└──────────────────────────────────────────────────────────────────────┘
        ↓ (< 10ms)
┌──────────────────────────────────────────────────────────────────────┐
│ RABBITMQ (CloudAMQP)                                                 │
│                                                                      │
│  Exchange "email" (topic)                                            │
│    routing key: "email.normal.booking.confirmed"                     │
│    → Queue "email.normal" (2 consumers, prefetch 20)                 │
│                                                                      │
│  Si tous les consumers occupés → message attend dans la queue        │
│  Si consumer crash → message requeue automatique                     │
│  Si max retries → DLX → "email.dlq"                                 │
└──────────────────────────────────────────────────────────────────────┘
        ↓
┌──────────────────────────────────────────────────────────────────────┐
│ EMAIL CONSUMER                                                       │
│                                                                      │
│  rateLimiter.acquirePermission()   ← attend si > 800 req/min        │
│      ↓                                                               │
│  deliveryTracker.markSending(emailNumber)                            │
│      ↓                                                               │
│  emailSender.sendHtmlEmailBlocking() → Microsoft Graph API           │
│      ↓                                                               │
│  deliveryTracker.markSent(emailNumber)                               │
│                                                                      │
│  Si échec → throw → RabbitMQ retry avec backoff                      │
│  Si max retries → DLQ + alerte admin                                 │
└──────────────────────────────────────────────────────────────────────┘
```

### 8.2 Flux Notification IN_APP (temps réel)

```
┌──────────────────────────────────────────────────────────────────────┐
│ BUSINESS SERVICE                                                     │
│                                                                      │
│  adminInAppNotifier.notify(...)                                      │
│      ↓                                                               │
│  notificationService.send(SendNotificationRequest{channel=IN_APP})   │
│      ↓                                                               │
│  dispatchSupport.create() → INSERT notification_message (PENDING)    │
│  dispatchSupport.dispatchIfDue() → process()                         │
│      ↓                                                               │
│  markSent(message)                                                   │
│  publishInAppToRabbit(message) → rabbitPublisher.publishNotification │
└──────────────────────────────────────────────────────────────────────┘
        ↓ (< 10ms)
┌──────────────────────────────────────────────────────────────────────┐
│ RABBITMQ                                                             │
│                                                                      │
│  Exchange "notification" → Queue "notification.inapp"                │
└──────────────────────────────────────────────────────────────────────┘
        ↓
┌──────────────────────────────────────────────────────────────────────┐
│ NOTIFICATION INAPP CONSUMER                                          │
│                                                                      │
│  messagingTemplate.convertAndSendToUser(                             │
│      recipientEmail,                                                 │
│      "/queue/notifications",                                         │
│      payload                                                         │
│  )                                                                   │
│      ↓                                                               │
│  STOMP Broker Relay → RabbitMQ → Client WebSocket                    │
│                                                                      │
│  Latence totale : < 500ms (vs 30s avant)                             │
└──────────────────────────────────────────────────────────────────────┘
        ↓
┌──────────────────────────────────────────────────────────────────────┐
│ CLIENT BROWSER (multi-dyno safe)                                     │
│                                                                      │
│  Dyno 1 ← WebSocket ← Client A                                      │
│  Dyno 2 ← WebSocket ← Client B                                      │
│                                                                      │
│  Les deux dynos sont des STOMP relays vers RabbitMQ.                 │
│  RabbitMQ route le message au bon dyno selon le user subscription.   │
│  Si un dyno redémarre, le client se reconnecte à un autre dyno      │
│  et le STOMP relay reprend la subscription.                          │
└──────────────────────────────────────────────────────────────────────┘
```

### 8.3 Flux OTP / Password Reset (CRITICAL)

```
┌──────────────────────────────────────────────────────────────────────┐
│ AUTH CONTROLLER                                                      │
│                                                                      │
│  ottMailService.sendOneTimeTokenMail(user, token)                    │
│      ↓                                                               │
│  1. Rendre template Thymeleaf "ott-login"                            │
│  2. deliveryTracker.queue(...) → INSERT email_delivery_log           │
│  3. Créer EmailMessage{priority=CRITICAL}                            │
│  4. rabbitPublisher.publishEmail(message)                            │
│      ↓ (non-bloquant, < 5ms)                                        │
│  return CompletableFuture.completedFuture(true)                      │
└──────────────────────────────────────────────────────────────────────┘
        ↓ (< 10ms)
┌──────────────────────────────────────────────────────────────────────┐
│ RABBITMQ                                                             │
│                                                                      │
│  Exchange "email" → Queue "email.critical"                           │
│  8 consumers, prefetch 5 → traitement immédiat                       │
│  Pas de backlog possible (8 threads dédiés)                          │
└──────────────────────────────────────────────────────────────────────┘
        ↓ (< 100ms)
┌──────────────────────────────────────────────────────────────────────┐
│ EMAIL CONSUMER (critical pool)                                       │
│                                                                      │
│  rateLimiter.acquirePermission() → priorité critique = pas d'attente │
│  emailSender.sendHtmlEmailBlocking() → Microsoft Graph               │
│                                                                      │
│  Temps total : < 2s (vs 5-30s avant)                                 │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 9. Plan d'implémentation par phases

### Phase 1 : Infrastructure & Configuration (1-2 jours)

| # | Tâche | Fichier |
|---|-------|---------|
| 1.1 | Ajouter dépendances Maven (spring-amqp, resilience4j, reactor-netty) | `pom.xml` |
| 1.2 | Ajouter RabbitMQ au Docker Compose + fichier enabled_plugins | `compose.yaml`, `docker/rabbitmq/enabled_plugins` |
| 1.3 | Créer `RabbitMQConfig.java` (exchanges, queues, bindings, factories) | `core/configuration/RabbitMQConfig.java` |
| 1.4 | Configuration YAML dev + prod | `application.yml`, `application-dev.yml`, `application-prod.yml` |
| 1.5 | Flyway migration V170 (colonne priority) | `db/migration/V170__add_email_priority_column.sql` |
| 1.6 | Vérifier que l'app démarre avec RabbitMQ local | Test manuel |

### Phase 2 : Email via RabbitMQ (2-3 jours)

| # | Tâche | Fichier |
|---|-------|---------|
| 2.1 | Créer `EmailPriority` enum | `core/communication/mailService/enums/EmailPriority.java` |
| 2.2 | Créer `EmailMessage` record | `core/communication/mailService/dto/EmailMessage.java` |
| 2.3 | Créer `GraphApiRateLimiter` | `core/communication/mailService/service/GraphApiRateLimiter.java` |
| 2.4 | Créer `EmailRabbitPublisher` | `core/communication/mailService/service/EmailRabbitPublisher.java` |
| 2.5 | Créer `EmailConsumer` (4 listeners par priorité) | `core/communication/mailService/consumer/EmailConsumer.java` |
| 2.6 | Modifier `OttMailServiceImpl` → publier dans queue CRITICAL | Modifier existant |
| 2.7 | Modifier `PasswordResetMailServiceImpl` → publier dans queue CRITICAL | Modifier existant |
| 2.8 | Modifier `NotificationDispatchSupport.process()` → publier email dans RabbitMQ | Modifier existant |
| 2.9 | Modifier les OutboxEventProcessors (Document, Contract, KYC) → publier dans RabbitMQ | Modifier existants |
| 2.10 | Ajuster `OutboxWorker` polling (500ms, batch 100) | `application.yml` |
| 2.11 | Tests unitaires des consumers et du rate limiter | Nouveaux tests |

### Phase 3 : Notifications temps réel via RabbitMQ STOMP Relay (2 jours)

| # | Tâche | Fichier |
|---|-------|---------|
| 3.1 | Créer `NotificationRabbitMessage` record | `core/communication/mailService/dto/NotificationRabbitMessage.java` |
| 3.2 | Modifier `WebSocketConfig` → STOMP relay conditionnel | Modifier existant |
| 3.3 | Créer `NotificationInAppConsumer` | `features/notification/consumer/NotificationInAppConsumer.java` |
| 3.4 | Créer `NotificationWebhookConsumer` | `features/notification/consumer/NotificationWebhookConsumer.java` |
| 3.5 | Modifier `NotificationDispatchSupport` → publier IN_APP dans RabbitMQ | Modifier existant |
| 3.6 | Modifier `NotificationOutboxEventProcessor` → publier dans RabbitMQ | Modifier existant |
| 3.7 | Ajuster `NotificationWorker` polling (2s, batch 50) | `application.yml` |
| 3.8 | Configurer heartbeat SockJS (25s pour Heroku) | `WebSocketConfig.java` |
| 3.9 | Migrer les alertes admin/inventory vers publication STOMP directe | Modifier les notifiers existants |
| 3.10 | Tests WebSocket end-to-end | Nouveaux tests |

### Phase 4 : Cleanup & Suppression Redis Pub/Sub (1 jour)

| # | Tâche | Fichier |
|---|-------|---------|
| 4.1 | Supprimer `InAppNotificationRedisSubscriber.java` | Supprimer |
| 4.2 | Supprimer `AdminAlertRedisSubscriber.java` | Supprimer |
| 4.3 | Supprimer `InventoryAlertRedisSubscriber.java` | Supprimer |
| 4.4 | Supprimer `RedisEventPublisher.java` | Supprimer |
| 4.5 | Supprimer `RedisChannelSubscriber.java` | Supprimer |
| 4.6 | Supprimer `RedisChannels.java` | Supprimer |
| 4.7 | Supprimer `RedisPubSubConfig.java` | Supprimer |
| 4.8 | Nettoyer les imports et références dans les classes modifiées | Refactoring |
| 4.9 | Vérifier que Redis reste fonctionnel pour les sessions | Test |

### Phase 5 : Production Heroku (1 jour)

| # | Tâche | Action |
|---|-------|--------|
| 5.1 | Ajouter addon CloudAMQP (Little Lemur) | `heroku addons:create cloudamqp:lemur` |
| 5.2 | Configurer les env vars STOMP | Heroku config |
| 5.3 | Activer le plugin STOMP sur CloudAMQP | Dashboard CloudAMQP |
| 5.4 | Deploy et smoke test | Test prod |
| 5.5 | Monitorer les queues via CloudAMQP dashboard | Monitoring |

---

## 10. Configuration Heroku détaillée

### 10.1 Env vars à configurer

```bash
# CloudAMQP (auto-provisioned par l'addon)
# CLOUDAMQP_URL=amqp://user:pass@host/vhost

# STOMP relay — extraire du dashboard CloudAMQP
heroku config:set CLOUDAMQP_STOMP_HOST=<stomp-host-from-cloudamqp>
heroku config:set CLOUDAMQP_STOMP_PORT=61614
heroku config:set CLOUDAMQP_STOMP_USER=<user>
heroku config:set CLOUDAMQP_STOMP_PASS=<pass>

# Activer le STOMP relay
heroku config:set BOKATI_WEBSOCKET_USE_STOMP_RELAY=true
```

### 10.2 CloudAMQP — Activer STOMP

1. Aller sur le dashboard CloudAMQP (via Heroku add-on page)
2. Configuration → Plugins → Activer `rabbitmq_stomp`
3. Noter le host et port STOMP (différent du host AMQP)

### 10.3 Procfile

Aucun changement — l'app Spring Boot démarre les consumers RabbitMQ automatiquement via `@RabbitListener`.

```
web: java -Dserver.port=$PORT -jar target/bokati-cowork-*.jar --spring.profiles.active=prod
```

### 10.4 Scaling multi-dyno

```bash
# Scaler à 2+ dynos (Standard-1X ou Performance-M)
heroku ps:scale web=2

# Les consumers RabbitMQ fonctionnent sur chaque dyno
# RabbitMQ distribue les messages entre les consumers (round-robin)
# Pas de conflit ni de duplication
```

---

## 11. Monitoring & Observabilité

### 11.1 Métriques à surveiller

| Métrique | Source | Alerte si |
|----------|--------|-----------|
| Queue depth `email.critical` | CloudAMQP | > 10 messages pendant > 30s |
| Queue depth `email.normal` | CloudAMQP | > 500 messages pendant > 5min |
| Queue depth `email.dlq` | CloudAMQP | > 0 (tout message en DLQ = problème) |
| Consumer count | CloudAMQP | < nombre attendu (consumer crash) |
| Rate limiter wait time | Resilience4j metrics | > 10s |
| `email_delivery_log.status = FAILED` count | DB query | > 5% du total sur 1h |
| WebSocket connections | Spring actuator | > 80% de la capacité |

### 11.2 Endpoint admin DLQ

Ajouter un endpoint admin pour consulter et retraiter les messages DLQ :

```java
// AdminRabbitController.java — à créer
// GET  /admin/rabbitmq/dlq/email    → lister les messages en DLQ
// POST /admin/rabbitmq/dlq/email/{id}/retry → republier un message DLQ
```

---

## 12. Rollback plan

Si la migration RabbitMQ pose problème en production :

1. **Rollback WebSocket** : Mettre `bokati.websocket.use-stomp-relay=false` → retour au SimpleBroker
2. **Rollback Email** : Les OutboxEventProcessors peuvent être revertis pour appeler directement `emailSender.sendHtmlEmailBlocking()` (le code existant reste en place pendant la migration)
3. **Redis Pub/Sub** : Les classes supprimées en Phase 4 peuvent être restaurées via git

**Stratégie recommandée** : Déployer en phases avec feature flags :
- `bokati.email.use-rabbitmq=true|false`
- `bokati.websocket.use-stomp-relay=true|false`
- `bokati.notification.use-rabbitmq=true|false`

---

## 13. Estimation effort total

| Phase | Effort | Dépendances |
|-------|--------|-------------|
| Phase 1 : Infrastructure | 1-2 jours | Aucune |
| Phase 2 : Email RabbitMQ | 2-3 jours | Phase 1 |
| Phase 3 : Notifications STOMP | 2 jours | Phase 1 |
| Phase 4 : Cleanup Redis | 1 jour | Phases 2 + 3 |
| Phase 5 : Prod Heroku | 1 jour | Phases 1-4 |
| **Total** | **7-9 jours** | |

---

## 14. Checklist de validation

- [ ] RabbitMQ démarre en dev (Docker Compose)
- [ ] Les 4 queues email sont créées avec les bonnes policies DLX
- [ ] Les 3 queues notification sont créées
- [ ] Email OTP arrive en < 5s via queue CRITICAL
- [ ] Email booking confirmation arrive en < 30s via queue NORMAL
- [ ] Rate limiter bloque au-delà de 800 req/min
- [ ] Message en échec → DLQ après max retries
- [ ] WebSocket fonctionne avec STOMP relay en dev
- [ ] Notification IN_APP arrive en < 1s via RabbitMQ
- [ ] Multi-dyno : notification arrive quel que soit le dyno du client
- [ ] Heartbeat SockJS empêche la déconnexion Heroku (55s timeout)
- [ ] Redis fonctionne toujours pour les sessions HTTP
- [ ] Pas de régression sur les emails existants (templates, pièces jointes, inline images)
- [ ] CloudAMQP dashboard montre les métriques
- [ ] DLQ vide après un run complet de tests