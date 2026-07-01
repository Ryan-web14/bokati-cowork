# Audit — Pipeline Outbox & Email Delivery

> Date : 2026-06-30  
> Statut : **Critique — problèmes bloquants identifiés**

---

## Résumé exécutif

Trois défaillances majeures expliquent les symptômes signalés :

| # | Symptôme | Cause racine | Sévérité |
|---|----------|--------------|----------|
| E1 | Emails marqués `SENT` mais jamais reçus | Graph API renvoie 2xx sans délivrance réelle (mauvaise config tenant / expéditeur non vérifié / spam) | Critique |
| E2 | OTP en `email.dlq`, jamais réenvoyé | Graph API lève une exception → DLQ sans consommateur | Critique |
| P1 | Souscription reste `PENDING_ACTIVATION` après paiement | `@Async + @TransactionalEventListener` — exception silencieuse, zéro retry | Critique |
| P2 | Conflit de processors outbox sur `BOOKING` | `NotificationOutboxEventProcessor` intercepte les events avant `BookingOutboxEventProcessor` | Élevée |
| P3 | Outbox events `FAILED` retentés à l'infini | Aucune limite de `attemptCount` | Moyenne |
| P4 | Fenêtre de 5 min si crash pendant `PROCESSING` | `claimPending` et `process` dans des transactions séparées | Moyenne |

---

## 1. Architecture du pipeline — État actuel

### 1.1 Outbox (transactions durables)

```
Service métier
  └── outboxService.publish(eventType, aggregateType, aggregateId, payload)
        → INSERT outbox_event (status=PENDING, availableAt=now)   ← même transaction

OutboxWorker [toutes les 500 ms, batch 100]
  └── claimPending()                 ← transaction A (PESSIMISTIC_WRITE)
        → status = PROCESSING
        → availableAt += 300s        ← lease d'exclusivité
  └── resolveProcessor(event).process(event)   ← hors transaction
  └── markPublished(id)              ← transaction B → PUBLISHED
      ou markFailed(id, err)         ← transaction C → FAILED, availableAt += 30s
```

**Processors enregistrés (ordre d'injection Spring) :**

| Processor | `supports()` condition | Module |
|---|---|---|
| `NotificationOutboxEventProcessor` | `aggregateType IN (BOOKING, SUBSCRIPTION, PAYMENT, BILLING, WALLET, ...)` **OU** `eventType LIKE NOTIFICATION_%` | core |
| `BookingOutboxEventProcessor` | `aggregateType == BOOKING` | booking |
| `KycOutboxEventProcessor` | `aggregateType == KYC_CASE` AND `eventType IN (KYC_CASE_*)` | kyc |
| `KycDocumentOutboxEventProcessor` | `aggregateType == KYC_DOCUMENT` | kyc |
| `DocumentOutboxEventProcessor` | `aggregateType == DOCUMENT` | document |
| `DocumentSignatureOutboxEventProcessor` | `aggregateType == DOCUMENT_SIGNATURE` | document |
| `ContractOutboxEventProcessor` | `aggregateType == CONTRACT` | contract |
| `ContractGenerationOutboxEventProcessor` | `eventType == CONTRACT_GENERATION_REQUESTED` | core |
| `ContractRenewalAmendmentOutboxEventProcessor` | `eventType == CONTRACT_RENEWAL_AMENDMENT_REQUESTED` | core |

### 1.2 Email (RabbitMQ)

```
DefaultEmailSender.sendHtmlEmail()    ← async, retourne immédiatement
  → EmailDeliveryTracker.queue()      → INSERT email_delivery_log (QUEUED)
  → EmailRabbitPublisher.publish()    → RabbitMQ exchange "email"
       routingKey: "email.<priority>.<eventType>"

EmailConsumer [@RabbitListener]
  → markSending()                     → email_delivery_log (SENDING)
  → GraphServiceClient.sendMail()     ← Microsoft Graph API (blocking)
  → markSent()                        → email_delivery_log (SENT)
  |  si exception Graph
  → markFailed()                      → email_delivery_log (FAILED)
  → throw RuntimeException            → message → email.dlq

email.dlq ← queue de rétention, AUCUN consommateur
```

### 1.3 Activation après paiement (chemin fragile)

```
PaymentServiceImpl.payWithWallet() / registerCashPayment()
  └── publishTransactionWorkflow(txnNumber, SUCCEEDED)
        └── ApplicationEventPublisher.publishEvent(...)

[AFTER_COMMIT, @Async, thread séparé]
PaymentTransactionWorkflowListener.onPaymentTransactionWorkflow()
  └── PaymentTransactionWorkflowProcessor.process()
        ├── resolveSource(intent.sourceType, intent.sourceCode)
        ├── vérifier PENDING_ACTIVATION + solde réglé
        └── subscriptionService.activate(subscriptionNumber, ...)
              └── SubscriptionLifecycleOperator.activate()
                    ├── status → ACTIVE
                    ├── entitlements granted
                    ├── emailNotifier.notify(SUBSCRIPTION_ACTIVATED)
                    └── outbox.publish("CONTRACT_GENERATION_REQUESTED", ...)
```

---

## 2. Défaillances détaillées

---

### [E1] Emails marqués SENT mais jamais reçus

**Symptôme** : `email_delivery_log.status = SENT`, `sent_at` renseigné, mais le destinataire ne reçoit rien.

**Cause** : `GraphServiceClient.sendMail()` retourne HTTP 202 Accepted. Microsoft Graph API accepte le message dans sa file mais ne le délivre pas. Causes possibles :

1. **Expéditeur non autorisé** : le `senderEmail` configuré dans `application.yml` n'appartient pas au tenant Azure AD, ou la permission `Mail.Send` de l'application n'est pas accordée sur cette boîte.
2. **Mail filtré côté Microsoft** : ATP (Advanced Threat Protection), règles de quarantaine, ou politique anti-spam du tenant bloquent l'email avant délivrance.
3. **Domaine non vérifié** : si le `senderEmail` utilise un domaine non validé dans le tenant M365.
4. **Email filtré chez le destinataire** : le serveur de destination rejette silencieusement l'email (SPF/DKIM/DMARC non configuré pour le domaine d'envoi).

**Vérifications immédiates** :
- Connecter l'admin Microsoft 365 → **Message Trace** (`admin.microsoft.com → Exchange → Mail Flow → Message Trace`) pour voir si l'email est reçu par Microsoft puis supprimé/mis en quarantaine.
- Vérifier dans Azure AD → App Registrations → l'app Graph → Permissions : `Mail.Send` doit être de type **Application** (pas Delegated) et doit être **approuvée par un admin**.
- Tester avec `curl` ou Postman en appelant Graph directement avec les mêmes credentials.

**Code concerné** :
```
DefaultEmailSender.java → sendHtmlEmailBlocking() → graphClient.users(senderEmail).sendMail(body, false).post()
application.yml → bokati.email.sender-email, bokati.email.azure.*
```

---

### [E2] OTP en `email.dlq` — email jamais envoyé

**Symptôme** : après tentative de connexion OTP, le message arrive dans `email.dlq` mais rien n'est reçu.

**Cause** : le `EmailConsumer` lève une exception en appelant `GraphServiceClient.sendMail()`. Avec `setDefaultRequeueRejected(false)`, le message part directement en DLQ. La DLQ n'a **aucun consommateur** — les messages y stagnent indéfiniment.

Flux précis :
```
EmailConsumer.consumeCritical()      ← @RabbitListener("email.critical")
  → sendHtmlEmailBlocking()
     → GraphServiceClient...         ← EXCEPTION (Graph API inaccessible, credentials invalides,
                                        rate limit, permission manquante)
  → markFailed()                     → email_delivery_log (FAILED)
  → throw RuntimeException
     → RabbitMQ: requeue=false
        → email.dlx exchange
           → email.dlq               ← MESSAGE STAGNE ICI SANS TRAITEMENT
```

**Cause probable de l'exception Graph** :
- `ClientSecretCredential` mal configuré (client_id, client_secret, tenant_id incorrect)
- `GraphServiceClient` ne parvient pas à obtenir un token OAuth2
- Timeout réseau vers `login.microsoftonline.com` ou `graph.microsoft.com`

**Vérification** :
```yaml
# application.yml — à vérifier
bokati.email:
  azure:
    client-id:     ${AZURE_CLIENT_ID}
    client-secret: ${AZURE_CLIENT_SECRET}
    tenant-id:     ${AZURE_TENANT_ID}
  sender-email:    ${EMAIL_SENDER}
```
Valider que ces variables d'environnement sont correctement injectées en dev/prod.

**Bug secondaire** : `OttMailServiceImpl.sendOneTimeTokenMail()` passe `user.getEmail()` comme `firstname` dans le contexte Thymeleaf (ligne 29). Le template `ott-login` affichera l'adresse email à la place du prénom.

```java
// OttMailServiceImpl.java:29 — BUG
context.setVariable("firstname", user.getEmail());  // ← doit être user.getFirstname() ou le display name
```

---

### [P1] Souscription reste `PENDING_ACTIVATION` après paiement

**Symptôme** : après un paiement réussi, la souscription ne passe pas en `ACTIVE`.

**Cause** : le mécanisme d'activation repose sur `@Async + @TransactionalEventListener(AFTER_COMMIT)`. C'est le même pattern à risque que celui qui causait les délais sur la génération de contrats.

```java
// PaymentTransactionWorkflowListener.java
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
public void onPaymentTransactionWorkflow(PaymentTransactionWorkflowEvent event) {
    try {
        processor.process(event);     // ← appelle activate()
    } catch (Exception ex) {
        log.error("...", ex);         // ← exception silencieuse, AUCUN retry
    }
}
```

**Si `processor.process()` échoue** :
- Le log enregistre l'erreur
- La souscription reste `PENDING_ACTIVATION`
- **Aucun mécanisme de retry** — ni outbox, ni requeue, ni relance automatique

**Scénarios de défaillance** :
- `subscriptionService.getForService()` → base de données surchargée
- `isSubscriptionPaymentSettled()` → `billingDocumentRepository` query timeout
- `activate()` → `entitlementService.grantForSubscription()` → exception
- Race condition : le thread async démarre avant que le commit de paiement soit visible en base (MVCC lag sur PostgreSQL sous forte charge)

**Impact** :
- Activation manuelle requise pour chaque cas échoué
- Génération de contrat bloquée (elle est dans `activate()`)
- Email de confirmation non envoyé

---

### [P2] Conflit de processors : `BOOKING` intercepté par `NotificationOutboxEventProcessor`

**Symptôme** : certains events outbox liés aux bookings ne déclenchent pas l'email attendu.

**Cause** : `NotificationOutboxEventProcessor.supports()` inclut `BOOKING` dans sa liste d'aggregates. `OutboxServiceImpl.resolveProcessor()` appelle `findFirst()` — le premier processor dont `supports()` retourne `true` gagne.

```java
// NotificationOutboxEventProcessor.java
private static final Set<String> SUPPORTED_AGGREGATES = Set.of(
    "NOTIFICATION", "BOOKING", "SUBSCRIPTION", "PAYMENT", "BILLING", ...
);

@Override
public boolean supports(OutboxEvent event) {
    return SUPPORTED_AGGREGATES.contains(event.getAggregateType().toUpperCase())  // ← inclut BOOKING
        || event.getEventType().startsWith("NOTIFICATION_");
}
```

Si `NotificationOutboxEventProcessor` est enregistré avant `BookingOutboxEventProcessor` dans la liste injectée, les events `aggregateType=BOOKING` sans préfixe `NOTIFICATION_` sont traités par le mauvais processor — notification in-app créée, email booking jamais envoyé.

**Idem pour** : `SUBSCRIPTION`, `PAYMENT`, `BILLING`, `WALLET`, `INVOICE` — tous présents dans `SUPPORTED_AGGREGATES` mais certains ont leurs propres processors spécialisés.

---

### [P3] Outbox events `FAILED` — retry infini

**Symptôme** : un event en échec permanent est retryé indéfiniment.

**Cause** : `processPending` récupère les events avec `status IN (PENDING, FAILED, PROCESSING)`. Il n'y a pas de condition sur `attemptCount`. Un event qui échoue systématiquement (ex. processor bug, donnée corrompue) sera retryé toutes les 30 secondes sans jamais s'arrêter.

```java
// OutboxServiceImpl.java — claimPending()
List<OutboxEventStatus> POLLABLE = List.of(PENDING, FAILED, PROCESSING);
// → aucune condition sur attemptCount
```

**Conséquence** : accumulation de logs d'erreur, charge inutile, masquage d'autres erreurs.

---

### [P4] Fenêtre de 5 min si crash pendant `PROCESSING`

**Cause** : `claimPending()` s'exécute dans une transaction A (marque `PROCESSING`, `availableAt += 300s`). `process()` s'exécute hors transaction. Si l'application crashe entre les deux, l'event reste `PROCESSING` pendant **5 minutes** avant d'être re-claimé.

```java
// OutboxServiceImpl.java
private static final long PROCESSING_LEASE_SECONDS = 300; // 5 minutes
```

---

## 3. Catalogue des types d'events outbox en production

| `eventType` | `aggregateType` | Processor | Effet |
|---|---|---|---|
| `CONTRACT_GENERATION_REQUESTED` | `SUBSCRIPTION` / `PASS` / `ADDON` | ContractGenerationOutboxEventProcessor | Génère le contrat PDF |
| `CONTRACT_RENEWAL_AMENDMENT_REQUESTED` | `SUBSCRIPTION` | ContractRenewalAmendmentOutboxEventProcessor | Crée un avenant de renouvellement |
| `KYC_CASE_CREATED` / `_APPROVED` / etc. | `KYC_CASE` | KycOutboxEventProcessor | Email notification KYC |
| `*` | `KYC_DOCUMENT` | KycDocumentOutboxEventProcessor | Email rappel expiration doc |
| `*` | `DOCUMENT` | DocumentOutboxEventProcessor | Email notification doc |
| `*` | `DOCUMENT_SIGNATURE` | DocumentSignatureOutboxEventProcessor | Email signature |
| `CONTRACT_SIGNING_REQUESTED` | `CONTRACT` | ContractOutboxEventProcessor | Email demande de signature |
| `*` | `CONTRACT` | ContractOutboxEventProcessor | Email notification contrat |
| `KYC_COMPLIANCE_WARNING` | `NOTIFICATION` | NotificationOutboxEventProcessor | Alerte conformité |
| `*` | `BOOKING` | **Conflit** : NotificationOutboxEventProcessor OU BookingOutboxEventProcessor | Email réservation (intermittent) |

---

## 4. Recommandations par priorité

### Priorité 1 — Blockers immédiats

#### 4.1 Diagnostiquer Graph API (E1 + E2)

Vérifier dans ce ordre :
1. **Variables d'environnement** : confirmer que `AZURE_CLIENT_ID`, `AZURE_CLIENT_SECRET`, `AZURE_TENANT_ID`, `EMAIL_SENDER` sont bien injectées (pas vides, pas les valeurs placeholder).
2. **Test direct Graph** : appeler `POST https://graph.microsoft.com/v1.0/users/{senderEmail}/sendMail` avec un token obtenu via les mêmes credentials. Si 401 → credentials invalides. Si 403 → permissions manquantes.
3. **Permission Azure AD** : dans App Registrations → l'application → API Permissions → vérifier `Mail.Send` (Application, non Delegated) + Admin Consent accordé.
4. **Message Trace M365** : admin.microsoft.com → Exchange → Mail Flow → Message Trace. Si l'email n'apparaît même pas dans les traces, Graph ne l'envoie pas vraiment.
5. **DLQ** : les messages OTP sont dans `email.dlq` — les rejouer via le plugin RabbitMQ Management (`/api/queues/%2F/email.dlq/get` puis `/api/bindings`) ou l'API admin si elle expose la DLQ.

#### 4.2 Migrer l'activation post-paiement vers l'outbox (P1)

**Problème** : `@Async + @TransactionalEventListener` sans retry.

**Solution** : remplacer par un event outbox, exactement comme la génération de contrat.

Dans `PaymentTransactionWorkflowProcessor.handleSucceededTransaction()`, au lieu d'appeler `subscriptionService.activate()` directement, publier :

```java
outboxService.publish(
    "SUBSCRIPTION_ACTIVATION_REQUESTED",
    "SUBSCRIPTION",
    source.code(),        // subscriptionNumber
    Map.of("subscriptionNumber", source.code(),
           "transactionNumber", transaction.getTransactionNumber())
);
```

Créer `SubscriptionActivationOutboxEventProcessor` :
```java
@Override
public boolean supports(OutboxEvent event) {
    return "SUBSCRIPTION_ACTIVATION_REQUESTED".equals(event.getEventType());
}

@Override
public void process(OutboxEvent event) {
    String subscriptionNumber = payload.path("subscriptionNumber").asText();
    Subscription sub = subscriptionService.getForService(subscriptionNumber);
    if (sub.getStatus() != SubscriptionStatus.PENDING_ACTIVATION) return; // idempotent
    subscriptionService.activate(subscriptionNumber, ...);
}
```

Bénéfices : retry automatique toutes les 30s, durabilité, traçabilité dans `outbox_event`.

---

### Priorité 2 — Corrections structurelles

#### 4.3 Corriger le conflit `NotificationOutboxEventProcessor` (P2)

Retirer de `SUPPORTED_AGGREGATES` tous les aggregates qui ont un processor dédié :

```java
// Avant
private static final Set<String> SUPPORTED_AGGREGATES = Set.of(
    "NOTIFICATION", "BOOKING", "SUBSCRIPTION", "PAYMENT", "BILLING",
    "INVOICE", "QUOTE", "WALLET", "CASH_REGISTER", "INVENTORY", "ADMIN"
);

// Après — retirer BOOKING (a BookingOutboxEventProcessor)
private static final Set<String> SUPPORTED_AGGREGATES = Set.of(
    "NOTIFICATION", "SUBSCRIPTION", "PAYMENT", "BILLING",
    "INVOICE", "QUOTE", "WALLET", "CASH_REGISTER", "INVENTORY", "ADMIN"
);
```

Ou mieux, utiliser le préfixe `NOTIFICATION_` exclusivement pour cibler les events de notification générique :
```java
@Override
public boolean supports(OutboxEvent event) {
    return event.getEventType() != null
        && event.getEventType().startsWith("NOTIFICATION_");
}
```

#### 4.4 Ajouter un consommateur DLQ + mécanisme de replay (E2)

La `email.dlq` doit avoir un `@RabbitListener` minimal qui :
- Enregistre les messages dans un log d'audit
- Permet un replay administrateur via endpoint REST

```java
@RabbitListener(queues = "email.dlq", containerFactory = "dlqListenerFactory")
public void consumeDlq(EmailRabbitMessage message) {
    log.error("Email DLQ: to={} subject={} attempts={}", message.to(), message.subject(), ...);
    // stocker en DB pour inspection et replay manuel
}
```

#### 4.5 Limiter les retries outbox (P3)

Ajouter une condition dans `claimPending()` :

```java
// OutboxEventRepository — nouvelle query
List<OutboxEvent> findByStatusInAndAvailableAtLessThanEqualAndAttemptCountLessThanOrderByCreatedAtAsc(
    Collection<OutboxEventStatus> statuses, Instant now, int maxAttempts, Pageable pageable
);
```

Avec `maxAttempts = 10` (configurable via `app.outbox.worker.max-attempts`). Au-delà, passer en statut `DEAD` (nouveau statut à ajouter) et alerter.

#### 4.6 Corriger le bug firstname OTP (E2)

```java
// OttMailServiceImpl.java:29
// Avant
context.setVariable("firstname", user.getEmail());
// Après
context.setVariable("firstname", user.getFirstname() != null ? user.getFirstname() : user.getEmail());
```

---

### Priorité 3 — Améliorations de robustesse

#### 4.7 Réduire la fenêtre de lease PROCESSING (P4)

Réduire `PROCESSING_LEASE_SECONDS` de 300s à 30s. Si le processing prend plus de 30s (PDF lourd, API externe lente), utiliser un heartbeat ou accepter le double-processing avec idempotence dans chaque processor.

#### 4.8 Ajouter des alertes sur les métriques critiques

- Outbox `FAILED` count > 5 → alerte Slack/email admin
- `email.dlq` depth > 0 → alerte immédiate
- Souscriptions `PENDING_ACTIVATION` depuis > 5 min après un paiement réussi → alerte

---

## 5. Matrice de risque

```
                 SÉVÉRITÉ
                 Faible    Moyenne    Élevée    Critique
PROBABILITÉ
Faible         |         |          |          |         |
Moyenne        |   [P4]  |   [P3]   |   [P2]   |         |
Élevée         |         |          |   [P1]   |  [E1]   |
Certaine       |         |          |          |  [E2]   |
```

**[E2] + [E1]** = aucun email n'arrive en ce moment. C'est l'urgence absolue.  
**[P1]** = aucune souscription ne s'active automatiquement. Revenue bloqué.  
**[P2]** = emails booking manquants. UX dégradée.

---

## 6. Checklist de résolution

```
[ ] 1. Vérifier les vars d'env Azure Graph (client_id, client_secret, tenant_id, sender_email)
[ ] 2. Tester Graph API directement (Postman / curl)
[ ] 3. Vérifier Message Trace Microsoft 365
[ ] 4. Rejouer les messages OTP de email.dlq
[ ] 5. Implémenter SubscriptionActivationOutboxEventProcessor
[ ] 6. Retirer BOOKING de NotificationOutboxEventProcessor.SUPPORTED_AGGREGATES
[ ] 7. Ajouter @RabbitListener sur email.dlq
[ ] 8. Corriger OttMailServiceImpl (firstname ← user.getEmail())
[ ] 9. Ajouter limite attemptCount dans outbox
[ ] 10. Ajouter monitoring/alertes
```
