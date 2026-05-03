# Roadmap — Fonctionnalités & Améliorations

> Document de référence complet. Répertorie tout ce qui est faisable dans le projet,
> organisé par priorité et par thème. Basé sur l'audit de l'état actuel de la plateforme.

---

## Légende

| Symbole | Signification |
|---------|---------------|
| 🔴 Critique | Bloque une expérience utilisateur ou une sécurité |
| 🟠 Important | Fort impact métier, à planifier bientôt |
| 🟡 Moyen | Améliore le confort ou la complétude |
| 🟢 Optionnel | Valeur ajoutée à long terme |
| ✅ Fait | Déjà implémenté |
| 🏗️ En cours | Partiellement implémenté |

---

## 1. Nouveaux Modules

### 1.1 🔴 Support & Ticketing

**Pourquoi c'est critique :**
Aucun canal formalisé pour qu'un client ou un membre signale un problème.
Sans ce module, les réclamations se gèrent par email ou téléphone — aucune traçabilité.

**Ce qu'il faut construire :**

```
features/support/
  model/
    SupportTicket        → titre, description, statut, priorité, catégorie
    TicketMessage        → messages du fil (client ↔ agent)
    TicketAttachment     → pièces jointes liées au ticket
  enums/
    TicketStatus         → OPEN, IN_PROGRESS, WAITING_CLIENT, RESOLVED, CLOSED
    TicketPriority       → LOW, MEDIUM, HIGH, URGENT
    TicketCategory       → BILLING, BOOKING, ACCESS, TECHNICAL, OTHER
  controller/
    SupportTicketController
    ClientSupportController   (portail client)
```

**Endpoints admin :**
```
POST   /v1/support/tickets                      → créer ticket (côté admin pour un client)
GET    /v1/support/tickets                      → lister tous les tickets (filtrable)
GET    /v1/support/tickets/{ticketNumber}        → détail ticket + messages
PATCH  /v1/support/tickets/{ticketNumber}/assign → assigner à un agent
PATCH  /v1/support/tickets/{ticketNumber}/status → changer statut
POST   /v1/support/tickets/{ticketNumber}/messages → répondre
GET    /v1/support/tickets/metrics              → SLA + tickets ouverts + temps de résolution
```

**Endpoints client :**
```
POST   /v1/client/support/tickets               → ouvrir un ticket
GET    /v1/client/support/tickets               → mes tickets
POST   /v1/client/support/tickets/{n}/messages  → répondre à l'agent
PATCH  /v1/client/support/tickets/{n}/close     → clore le ticket
```

**Fonctionnalités liées :**
- Notification email automatique à chaque changement de statut
- SLA configurable par catégorie (ex : URGENT → 2h de première réponse)
- Lien vers la réservation ou la facture concernée
- Intégration dans la section analytics (tickets en retard, taux de résolution)

---

### 1.2 🟠 Visitor Management

**Pourquoi :**
Un espace coworking reçoit des visiteurs (externes à l'abonnement) qui viennent voir des membres.
Actuellement aucun suivi de qui entre et sort.

**Ce qu'il faut construire :**

```
features/visitor/
  model/
    Visitor              → nom, email, téléphone, entreprise
    VisitorPass          → numéro, host (member), validFrom, validUntil, purpose
    VisitorCheckIn       → visiteur, date/heure entrée, date/heure sortie, agent
```

**Endpoints :**
```
POST   /v1/visitors/passes                      → enregistrer visite à venir
GET    /v1/visitors/passes                      → liste des visites prévues
GET    /v1/visitors/passes/today                → visites du jour
POST   /v1/visitors/{passNumber}/check-in       → enregistrer l'arrivée
POST   /v1/visitors/{passNumber}/check-out      → enregistrer le départ
GET    /v1/visitors/log                         → journal entrées/sorties
```

**Fonctionnalités liées :**
- Notification email au host (member) quand le visiteur arrive
- Badge imprimable (QR code)
- Durée max de visite configurable
- Rapport mensuel des visites

---

### 1.3 🟠 Gestion des tâches internes (Task Management)

**Pourquoi :**
Le staff doit gérer des tâches récurrentes (nettoyage salle, maintenance, réapprovisionnement).
Actuellement géré hors système.

**Ce qu'il faut construire :**

```
features/task/
  model/
    Task                 → titre, description, assigné à, deadline, statut, priorité
    TaskChecklist        → sous-étapes
    TaskRecurrence       → DAILY, WEEKLY, MONTHLY, AFTER_BOOKING
    TaskComment          → messages internes entre agents
```

**Endpoints :**
```
POST   /v1/tasks                                → créer tâche
GET    /v1/tasks                                → mes tâches ou toutes les tâches
PATCH  /v1/tasks/{id}/complete                  → marquer terminée
PATCH  /v1/tasks/{id}/assign                    → réassigner
GET    /v1/tasks/due-today                      → tâches du jour
```

**Triggers automatiques :**
- Tâche "Préparer la salle" créée automatiquement X heures avant chaque réservation confirmée
- Tâche "Nettoyer la salle" créée après chaque fin de réservation
- Tâche "Rappel maintenance" déclenchée par l'inventaire

---

### 1.4 🟠 CRM — Gestion des Prospects

**Pourquoi :**
Un coworking a des leads (personnes qui visitent le site, demandent un devis) qui ne sont pas encore clients.
Sans CRM, ces contacts sont perdus.

**Ce qu'il faut construire :**

```
features/crm/
  model/
    Lead                 → nom, email, téléphone, source, intérêt, statut
    LeadActivity         → interactions (appel, email, visite)
    Opportunity          → lead qualifié + montant estimé + probabilité + deadline
    LeadStage            → NEW, CONTACTED, QUALIFIED, PROPOSAL_SENT, WON, LOST
```

**Endpoints :**
```
POST   /v1/crm/leads                            → créer lead (depuis formulaire site)
GET    /v1/crm/leads                            → pipeline commercial
PATCH  /v1/crm/leads/{id}/qualify               → qualifier le lead
PATCH  /v1/crm/leads/{id}/convert               → convertir en member/customer
POST   /v1/crm/leads/{id}/activities            → logger une interaction
GET    /v1/crm/pipeline                         → vue kanban stages
GET    /v1/crm/metrics                          → conversion rate, temps moyen, valeur pipeline
```

**Intégrations :**
- Si un lead est converti → `MemberController.create()` ou `CustomerController.create()`
- Devis auto-généré depuis un lead → `BillingDocumentController.create()` type QUOTE

---

### 1.5 🟡 Programme de Fidélité & Parrainage

**Ce qu'il faut construire :**

```
features/loyalty/
  model/
    LoyaltyAccount       → ownerType, ownerCode, totalPoints, currentPoints
    LoyaltyTransaction   → type (EARNED, REDEEMED, EXPIRED), amount, source
    LoyaltyReward        → nom, pointsRequired, rewardType (DISCOUNT, FREE_HOUR, PASS)
    ReferralCode         → code unique par membre, usedCount
    ReferralReward       → member parrain, member filleul, rewardGranted
```

**Règles d'accumulation :**
- X points par heure de réservation
- X points par XAF de facture payée
- Bonus points première réservation du mois
- Bonus points parrainage (parrain + filleul)

**Rédemption :**
- Points → heures gratuites
- Points → réduction sur facture
- Points → pass ponctuel

---

### 1.6 🟡 Marketplace — Services Additionnels

**Pourquoi :**
Un coworking peut proposer des services à la carte que les membres commandent depuis le portail.

**Exemples de services :**
- Impression / photocopie (X XAF la page)
- Location matériel (vidéoprojecteur, écran de présentation)
- Restauration / boissons
- Domiciliation postale
- Standard téléphonique virtuel

**Ce qu'il faut construire :**

```
features/marketplace/
  model/
    ServiceItem          → nom, description, prix, unité, disponible
    ServiceOrder         → member/customer, items, total, statut
    ServiceOrderLine     → service, quantité, prix unitaire
```

**Endpoints :**
```
GET    /v1/marketplace/services                 → catalogue services disponibles
POST   /v1/client/marketplace/orders            → passer commande
GET    /v1/client/marketplace/orders            → mes commandes
PATCH  /v1/marketplace/orders/{n}/complete      → traiter la commande (staff)
```

**Intégration facturation :**
Les commandes marketplace génèrent automatiquement une ligne de facturation.

---

### 1.7 🟢 Gestion des Accès Physiques (Access Control)

**Pourquoi :**
Les coworkings modernes utilisent des badges RFID ou des codes PIN pour contrôler l'accès aux portes.

**Ce qu'il faut construire :**

```
features/access/
  model/
    AccessCredential     → ownerType, ownerCode, credentialType (BADGE, PIN, QR), value, active
    AccessPoint          → code, nom, location, type (DOOR, TURNSTILE, LOCKER)
    AccessLog            → credential, accessPoint, timestamp, direction (IN/OUT), granted
    AccessPolicy         → ressource ou zone, horaires autorisés, jours
```

**Logique :**
- Un membre avec abonnement actif → accès autorisé aux heures d'ouverture
- Un membre avec réservation confirmée → accès à la salle réservée dans le créneau
- Refus d'accès → alerte temps réel au staff

**Intégrations API hardware :**
- API REST pour les contrôleurs de portes (Salto, Kisi, HID)
- Webhook entrant pour les événements d'accès matériel

---

### 1.8 🟢 Multi-Tenant / Multi-Site

**Pourquoi :**
Si Bokati Cowork s'étend sur plusieurs sites (Brazzaville, Pointe-Noire, Kinshasa),
chaque site doit avoir sa propre configuration tout en partageant une admin centrale.

**Ce qu'il faut construire :**
- Champ `tenantId` sur toutes les entités principales
- Filtrage automatique par tenant dans tous les repositories
- Auth admin avec scope tenant (`realm=ADMIN, tenantId=BZV-01`)
- Dashboard agrégé multi-site pour le SUPER_ADMIN

---

## 2. Améliorations des Modules Existants

### 2.1 🔴 Module Booking — Améliorations critiques

#### Récurrence avancée
**Actuellement :** récurrence créée mais limitée.
**Amélioration :** exceptions dans la récurrence (sauter un jour férié), fin de récurrence à date ou après N occurrences.

#### Waitlist (liste d'attente)
**Actuellement :** si une salle est pleine → refus.
**Amélioration :**
- Rejoindre la liste d'attente sur un créneau complet
- Notification automatique si annulation → première personne de la waitlist

#### Réservation depuis le portail client
**Actuellement :** `BookingServiceImpl` gère la création.
**Amélioration :** Exposer `POST /client/bookings` avec l'interface simplifiée (voir plan ClientPlatform).

#### Check-in QR Code
**Actuellement :** check-in manuel par le staff.
**Amélioration :**
- Générer QR code unique par réservation
- Le client scanne à l'entrée → check-in automatique
- Intégration avec le module Access Control

#### Salle virtuelle (vidéoconférence)
Ajouter un type de ressource `VIRTUAL_ROOM` avec lien de réunion (Zoom/Teams/Jitsi) généré automatiquement.

---

### 2.2 🟠 Module Billing — Améliorations importantes

#### Avoir automatique sur annulation
**Actuellement :** avoir créé manuellement.
**Amélioration :** quand une réservation est annulée avec facturation existante → avoir généré automatiquement selon la politique d'annulation configurée.

#### Politique d'annulation configurable
```
CancellationPolicy:
  - annuler > 24h avant → remboursement 100%
  - annuler entre 12h et 24h avant → remboursement 50%
  - annuler < 12h → aucun remboursement
```

#### Rappels automatiques de paiement
**Actuellement :** aucun rappel.
**Amélioration :**
- Worker qui cherche les factures OVERDUE
- Email de relance J+1, J+7, J+15 avec le PDF en pièce jointe
- Après 3 relances → notification au manager

#### Acompte / Caution (Deposit)
**Actuellement :** pas de gestion de dépôt de garantie.
**Amélioration :**
- Lors de la création d'abonnement : facturer un deposit
- Deposit stocké en wallet ou en DB
- Remboursé à la fin du contrat si aucun impayé

#### Devis multi-lignes envoyé par email avec lien d'acceptation
**Actuellement :** devis créé, email manuel.
**Amélioration :**
- Email avec lien sécurisé `GET /quotes/{token}/view`
- Page de consultation + bouton "Accepter" ou "Refuser"
- Signature électronique simple (checkbox + horodatage IP)

---

### 2.3 🟠 Module Payment — Améliorations importantes

#### Paiement par lien (Payment Link)
**Actuellement :** le paiement se fait via l'API directement.
**Amélioration :**
- Générer un lien de paiement sécurisé : `GET /payment-links/{token}`
- Le client paie via ce lien sans être authentifié
- Utile pour les clients ponctuels (visiteurs, invités)

#### Remboursement partiel
**Actuellement :** remboursement total uniquement.
**Amélioration :** permettre un remboursement partiel avec raison.

#### Réconciliation automatique
**Actuellement :** réconciliation manuelle.
**Amélioration :** worker quotidien qui tente de réconcilier les paiements orphelins avec les factures non réglées du même client.

#### Alertes dépassement caisse
**Actuellement :** `maxCashAmount` existe sur `CashRegister` mais aucune alerte.
**Amélioration :** notification au manager quand le solde caisse dépasse `maxCashAmount`.

#### Export relevé de compte client
**Actuellement :** `/billing/customers/{type}/{code}/statement` retourne du JSON.
**Amélioration :** endpoint PDF ou CSV du relevé sur une période.

---

### 2.4 🟠 Module Subscription — Améliorations importantes

#### Période d'essai (Trial)
**Actuellement :** `SubscriptionStatus.TRIALING` existe dans l'enum mais non implémenté.
**Amélioration :**
- Créer abonnement avec `trialDays: 7`
- Accès complet pendant la période d'essai
- Facturation uniquement après la période d'essai
- Rappel email 2 jours avant fin du trial

#### Upgrade / Downgrade de plan en cours de période
**Actuellement :** `SubscriptionChangeRequestController` existe mais le calcul du prorata est à vérifier.
**Amélioration :**
- Calcul prorata automatique lors du changement
- Avoir ou supplément de facturation généré immédiatement
- Historique complet des changements de plan

#### Gel temporaire d'abonnement (Pause)
**Actuellement :** `SUSPENDED` existe mais bloque complètement.
**Amélioration :**
- `PAUSED` : membre décide de geler pendant X jours (congé, déplacement)
- La période de pause ne compte pas dans la durée de l'abonnement
- Limite de pause configurable par plan (ex : max 30 jours/an)

#### Abonnement famille / multi-users
**Actuellement :** un abonnement = un owner.
**Amélioration :**
- `SubscriptionSeatController` existe déjà → capitaliser dessus
- Un abonnement peut avoir N sièges (SEAT) pour des membres différents
- Chaque siège consomme les entitlements du plan parent

#### Notification avant expiration
**Actuellement :** pas de rappel avant la date de renouvellement.
**Amélioration :**
- Email 30j avant renouvellement → rappel
- Email 7j avant → alerte
- Email J+1 si paiement échoué → `PAST_DUE`

---

### 2.5 🟡 Module Resource — Améliorations

#### Tarification dynamique
**Actuellement :** tarif fixe par règle de pricing.
**Amélioration :**
- Tarif peak/off-peak (ex : -20% avant 9h et après 18h)
- Tarif week-end différent
- Tarif "last minute" (réservation dans les 2h = -10%)

#### Calendrier de disponibilité public
**Actuellement :** `/resources/availability` retourne les fenêtres.
**Amélioration :**
- Vue calendrier mensuel de disponibilité par ressource
- Code couleur : disponible / partiellement occupé / complet
- Exposé en lecture seule sur le portail client sans auth

#### Photos et galerie
**Actuellement :** pas de gestion d'images de ressources.
**Amélioration :**
- Upload photos par ressource (via `DocumentService` existant)
- Tri ordre affichage
- Photo de couverture pour le portail

#### Équipements (Amenities) enrichis
**Actuellement :** lien ressource → amenity (WiFi, AC, etc).
**Amélioration :**
- Amenities avec capacité (ex : "8 chaises", "1 vidéoprojecteur")
- Amenity optionnel avec supplément de prix (ex : +5000 XAF pour le projecteur)

---

### 2.6 🟡 Module Document & KYC — Améliorations

#### Signature électronique intégrée
**Actuellement :** `DocumentSignatureStatus` existe, mais la signature est externe.
**Amélioration :**
- Intégration avec un service de signature (Docusign, YouSign, ou signature simple in-app)
- Flux : document généré → lien envoyé au client → client signe → document archivé signé

#### Validité des documents KYC
**Actuellement :** `expiryDate` existe mais aucun worker de vérification.
**Amélioration :**
- Worker hebdomadaire → détecte les documents KYC expirés
- Notification au membre : "votre carte d'identité expire dans 30 jours, veuillez renouveler"
- Passage automatique en `PENDING_CORRECTION`

#### OCR automatique sur les documents uploadés
**Amélioration :**
- Intégrer un service OCR (Google Vision, AWS Textract, ou Tesseract)
- Extraire automatiquement nom, date naissance, numéro pièce d'identité
- Pré-remplir le profil du membre depuis le document

---

### 2.7 🟡 Module Notification — Améliorations

#### SMS via Orange ou MTN Congo
**Actuellement :** `NotificationChannel.SMS` existe dans l'enum mais non implémenté.
**Amélioration :**
- Intégrer l'API SMS d'un opérateur local (Orange CI API, Africa's Talking)
- Fallback SMS si email non livré
- Rappels de réservation par SMS (30 min avant)

#### Notifications push (app mobile)
**Actuellement :** `NotificationChannel.PUSH` dans l'enum, non implémenté.
**Amélioration :**
- Firebase Cloud Messaging (FCM) pour Android/iOS
- Clé FCM configurée dans `application.yml`

#### Centre de notifications in-app
**Actuellement :** notifications stockées en DB mais pas d'endpoint "mes notifications" pour le client.
**Amélioration :**
- `GET /client/notifications` → liste paginée
- Badge count non-lu
- `PATCH /client/notifications/{n}/read` → marquer lu
- WebSocket ou SSE pour les notifications temps réel

#### Templates email enrichis
**Actuellement :** emails en HTML inline basique.
**Amélioration :**
- Créer des templates Freemarker riches pour chaque type d'événement
- Logo, couleurs de la marque, pied de page légal
- Pré-visualisation dans l'admin

---

### 2.8 🟡 Module Inventory — Améliorations

#### Code-barres / QR Code sur les actifs
**Actuellement :** `generateLabel()` existe dans `InventoryAdminController`.
**Amélioration :**
- Générer des étiquettes imprimables A4 (plusieurs par page)
- Scan mobile → informations instantanées sur l'actif
- Validation des inventaires physiques par scan

#### Commande fournisseur automatique
**Actuellement :** suggestions de réapprovisionnement manuelles.
**Amélioration :**
- Si `quantityAvailable <= reorderPoint` → bon de commande brouillon créé automatiquement
- Envoi email automatique au fournisseur (si email configuré)
- Suivi confirmation fournisseur

#### Gestion des prêts de matériel
**Actuellement :** aucun suivi de prêt.
**Amélioration :**
- Prêter un équipement à un membre (laptop, câble HDMI)
- Date de retour attendue
- Alerte si retour en retard
- Intégration avec les réservations (matériel inclus dans la salle)

---

### 2.9 🟡 Module Analytics & Reporting — Améliorations

#### Dashboard temps réel
**Actuellement :** rapports calculés à la demande.
**Amélioration :**
- Endpoint `/analytics/live` : qui est dans les locaux maintenant, quelles salles sont occupées
- Données rafraîchies toutes les 30 secondes via polling côté front
- Carte des espaces (plan SVG interactif)

#### Comparaison de périodes
**Actuellement :** rapports sur une période fixe.
**Amélioration :**
- Comparer mois N vs mois N-1
- Comparer trimestre actuel vs trimestre précédent
- Variation en % sur chaque indicateur

#### Export planifié par email
**Amélioration :**
- Configurer un envoi automatique du rapport mensuel le 1er de chaque mois
- Format PDF ou Excel
- Destinataires configurables

#### Prévisions (Forecasting)
**Amélioration :**
- Basé sur les abonnements actifs et les réservations récurrentes
- Prévision du chiffre d'affaires des 3 prochains mois
- Taux d'occupation prévisionnel par ressource

---

## 3. Améliorations Techniques

### 3.1 🔴 Sécurité — Points critiques

#### Tous les endpoints /v1/** sont ouverts
```java
// SecurityConfig actuel — PROBLÈME CRITIQUE
.requestMatchers(ApiPath.V1 + "/**").permitAll()
```
→ Implémenter le plan `plan-auth-clientplatform-async.md` (Étape 2)

#### CORS trop permissif
```java
config.setAllowedOriginPatterns(List.of("*"));  // ← à restreindre
```
→ Limiter aux origines connues (admin.bokati.com, app.bokati.com)

#### JWT secret non sécurisé
```yaml
app.security.jwt.secret: change-me-32-bytes  # ← à changer en production
```
→ Utiliser une variable d'environnement ou un secrets manager

#### Rate limiting sur les endpoints auth
→ Bloquer les tentatives de brute force sur `/auth/login`
→ Max 5 tentatives/IP/minute → 429 Too Many Requests

---

### 3.2 🟠 Tests automatisés

**Actuellement :** quelques tests dans `src/test/` mais couverture incomplète.

**Tests à créer :**

```
src/test/
  features/
    billing/
      BillingDocumentServiceImplTest       ← calcul montants, statuts
      BillingWorkflowIntegrationTest       ← DRAFT → PAID
    payment/
      PaymentServiceImplTest               ← encaissement, remboursement
      PawapayLiveIntegrationTest           ← déjà partiellement là
    booking/
      BookingServiceImplTest               ← disponibilité, réservation
      BookingRecurrenceTest                ← récurrences
    subscription/
      SubscriptionLifecycleTest            ← activation, suspension, renouvellement
      EntitlementGrantTest                 ← débit/crédit entitlements
    contract/
      ContractPdfGenerationTest            ← vérifier que le PDF est valide
```

**Outils recommandés :**
- JUnit 5 (déjà dans le projet)
- Testcontainers (PostgreSQL réel, pas de mock)
- Spring Boot Test avec `@SpringBootTest`
- MockMvc pour les tests d'intégration API

---

### 3.3 🟠 Monitoring & Observabilité

**Actuellement :** Spring Actuator basique.

**Améliorations :**

#### Health checks enrichis
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, metrics, info, loggers
  endpoint:
    health:
      show-details: when-authorized
```

Custom health indicators :
- `DatabaseHealthIndicator` → ping DB
- `StorageHealthIndicator` → disk space documents
- `PawapayHealthIndicator` → API sandbox joignable
- `SmtpHealthIndicator` → SMTP connecté

#### Métriques Micrometer
- Compteur réservations créées/annulées par heure
- Temps moyen de génération PDF
- Taux d'échec des emails
- Longueur de la queue outbox

#### Logging structuré (JSON)
```yaml
logging:
  pattern:
    console: '{"ts":"%d","level":"%p","logger":"%c","msg":"%m"}%n'
```
Compatible avec ELK Stack ou Grafana Loki.

---

### 3.4 🟡 Performance — Optimisations DB

#### Indexes manquants probables

```sql
-- Réservations : requêtes fréquentes par période
CREATE INDEX idx_booking_started_at ON booking(started_at, ended_at);

-- Billing : factures dues (worker de relance)
CREATE INDEX idx_billing_doc_due_date ON billing_document(due_date, status)
  WHERE status IN ('ISSUED', 'OVERDUE');

-- Entitlements : lookup par owner et période
CREATE INDEX idx_entitlement_grant_owner_valid ON entitlement_grant(owner_type, owner_code, status, valid_until);

-- Outbox : worker batch
CREATE INDEX idx_outbox_status_created ON outbox_event(status, created_at)
  WHERE status IN ('PENDING', 'FAILED');
```

#### Cache Redis (déjà dans l'infrastructure)
Redis est configuré mais son usage n'est pas visible dans le code.

Candidats pour le cache :
- Types de ressources et groupes (changent rarement)
- Types de documents (référentiel statique)
- Plans d'abonnement actifs
- Taux de taxe

```java
@Cacheable("resource-types")
public List<ResourceTypeResponse> listResourceTypes() { ... }

@CacheEvict(value = "resource-types", allEntries = true)
public ResourceTypeResponse createResourceType(...) { ... }
```

#### Pagination sur les listes volumineuses
Vérifier que tous les endpoints de liste utilisent `Pageable` et que les index DB permettent le tri efficacement.

---

### 3.5 🟡 API & Documentation

#### OpenAPI / Swagger
**Actuellement :** SpringDoc est peut-être configuré (à vérifier), mais la documentation API n'est pas exposée publiquement.

**Amélioration :**
```yaml
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    operationsSorter: method
  info:
    title: Bokati Cowork API
    version: 1.0.0
```

Annoter les controllers avec `@Tag`, `@Operation`, `@ApiResponse`.

#### Versionning d'API
**Actuellement :** `/sni/api/v1/` — une seule version.
**Amélioration :** prévoir `/sni/api/v2/` pour les breaking changes futurs sans casser les intégrations existantes.

#### Webhooks sortants pour les partenaires
**Actuellement :** `WebhookController` existe.
**Amélioration :**
- Permettre à des partenaires de s'abonner à des événements
- Ex : un système RH reçoit un webhook quand un membre est créé
- Signature HMAC sur chaque événement (pattern PawaPay)

---

### 3.6 🟢 Infrastructure & Déploiement

#### Docker & CI/CD
**Amélioration :**
- `Dockerfile` multi-stage (build Maven → image JRE slim)
- `docker-compose.prod.yml` pour la production
- GitHub Actions ou GitLab CI : build → test → push image → deploy

#### Backup automatique
**Amélioration :**
- Script pg_dump automatique quotidien
- Backup des fichiers documents (stockage externe : S3, Cloudflare R2, ou MinIO)
- Rétention 30 jours

#### Variables d'environnement (secrets management)
**Actuellement :** secrets dans `application.yml` ou `.env`.
**Amélioration :** utiliser HashiCorp Vault ou AWS Parameter Store pour les secrets de production.

---

## 4. Fonctionnalités ClientPlatform spécifiques

> Ces fonctionnalités sont exposées uniquement sur le portail client.
> Voir `plan-auth-clientplatform-async.md` pour l'architecture complète.

### 4.1 🔴 Portail de réservation libre-service

Le client peut réserver sans passer par le staff.

**Flow :**
1. S'authentifier sur le portail client
2. Parcourir les espaces disponibles (photos, prix, équipements)
3. Sélectionner un créneau dans le calendrier
4. Confirmer (paiement DIRECT, SUBSCRIPTION ou PASS selon abonnement)
5. Recevoir confirmation email + QR code d'accès

---

### 4.2 🟠 Tableau de bord client

```
/client/dashboard → affiche :
  - Prochaines réservations
  - Heures restantes dans l'abonnement (entitlements)
  - Solde wallet
  - Dernières factures
  - Tickets de support ouverts
  - Notifications non lues
```

---

### 4.3 🟠 Recharge wallet mobile money

```
POST /client/wallet/deposit/mobile-money
  → Crée un PaymentIntent de type MOBILE_MONEY
  → Redirige vers PawaPay (MTN, Orange, Airtel)
  → Callback → solde wallet crédité
```

---

### 4.4 🟡 Historique de présence

```
GET /client/presence/history → dates et heures de check-in/check-out
```
Utile pour les membres en déplacement fréquent qui doivent justifier leur présence.

---

### 4.5 🟡 Gestion du profil et préférences

```
PUT /client/me/preferences
  → notifications par SMS ou email
  → langue préférée
  → type de ressource favori
  → rappel automatique avant réservation (15 min, 1h, 1 jour)
```

---

### 4.6 🟡 Parrainage depuis le portail

```
GET /client/referral-code → mon code de parrainage
GET /client/referral/history → qui j'ai parrainé + récompenses gagnées
POST /client/referral/apply → appliquer un code parrain lors de l'inscription
```

---

## 5. Intégrations Externes envisageables

### 5.1 🟠 Comptabilité — Export vers logiciel comptable

**Export vers :**
- Sage 100, QuickBooks, Wave (Africa)
- Format : CSV, FEC, OFX

**Données exportées :**
- Factures avec TVA ventilée
- Paiements reçus avec méthode
- Mouvements de caisse
- Avoirs

---

### 5.2 🟡 Vidéoconférence intégrée

Pour les salles de réunion virtuelles :
- Jitsi Meet (open source, auto-hébergeable)
- Zoom (API)
- Google Meet (via OAuth)

Génère automatiquement un lien lors de la confirmation d'une réservation `VIRTUAL_ROOM`.

---

### 5.3 🟡 Calendrier externe (CalDAV / iCal)

```
GET /client/bookings/calendar.ics → flux iCal de mes réservations
```

Le membre peut synchroniser ses réservations dans Google Calendar, Outlook, Apple Calendar.

---

### 5.4 🟡 Application mobile

Backend déjà compatible REST → développement d'une app mobile React Native ou Flutter.

**Fonctionnalités mobiles prioritaires :**
- Réservation rapide
- QR code check-in
- Notifications push (FCM)
- Solde wallet + recharge mobile money
- Tickets support

---

### 5.5 🟢 Intelligence Artificielle

#### Recommandation de créneaux
- "En fonction de vos habitudes, nous recommandons le bureau Lembissi de 9h à 12h le mardi"
- Basé sur l'historique des réservations

#### Prédiction d'occupation
- Modèle de prévision de remplissage par jour/heure
- Utile pour la tarification dynamique

#### Chatbot de support
- Réponses automatiques aux questions fréquentes
- Intégration avec Claude API (Anthropic) pour les réponses contextuelles
- Escalade automatique vers un agent humain si non résolu

---

## 6. Récapitulatif par ordre de priorité

### 🔴 Priorité 1 — Sécurité et fondations

| # | Quoi | Module concerné |
|---|------|-----------------|
| 1 | Sécuriser tous les endpoints (retirer permitAll) | Security |
| 2 | @PreAuthorize sur tous les controllers | Security |
| 3 | Migration rôles et permissions en DB | Security |
| 4 | Auth client (realm CLIENT dans JWT) | ClientPlatform |
| 5 | Support / Ticketing | Nouveau module |

### 🟠 Priorité 2 — Expérience utilisateur

| # | Quoi | Module concerné |
|---|------|-----------------|
| 6 | Portail client — réservation libre-service | ClientPlatform |
| 7 | Portail client — tableau de bord | ClientPlatform |
| 8 | Notifications SMS (Orange Congo) | Notification |
| 9 | Rappels automatiques de paiement | Billing |
| 10 | Waitlist sur réservations complètes | Booking |
| 11 | Fix PDF factures (même bug que contrats) | Billing |
| 12 | Visitor management | Nouveau module |
| 13 | Recharge wallet mobile money côté client | ClientPlatform |

### 🟡 Priorité 3 — Complétude métier

| # | Quoi | Module concerné |
|---|------|-----------------|
| 14 | Période d'essai abonnement (TRIALING) | Subscription |
| 15 | Gel abonnement (PAUSED) | Subscription |
| 16 | CRM — gestion prospects | Nouveau module |
| 17 | Politique d'annulation configurable | Booking/Billing |
| 18 | Export comptabilité | Billing |
| 19 | Tâches internes (Task management) | Nouveau module |
| 20 | Centre notifications in-app | Notification |
| 21 | Calendrier iCal export | ClientPlatform |
| 22 | Programme de fidélité | Nouveau module |

### 🟢 Priorité 4 — Long terme

| # | Quoi | Module concerné |
|---|------|-----------------|
| 23 | Application mobile | Infrastructure |
| 24 | Marketplace services | Nouveau module |
| 25 | Access control physique (badges RFID) | Nouveau module |
| 26 | Multi-site / Multi-tenant | Architecture |
| 27 | Vidéoconférence intégrée | Resource |
| 28 | IA — recommandation créneaux | Analytics |
| 29 | Monitoring Grafana / ELK | Infrastructure |
| 30 | CI/CD GitHub Actions | Infrastructure |
