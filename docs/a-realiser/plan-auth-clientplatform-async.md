# Plan d'implémentation — Auth, Permissions, ClientPlatform & Async

## Contexte

Ce document est le plan de travail pour trois chantiers interdépendants :

1. **Async** — finaliser la mise en asynchrone des méthodes restantes
2. **Sécurité & permissions** — sécuriser proprement tous les endpoints admin avec du RBAC granulaire
3. **ClientPlatform** — créer le portail client avec sa propre auth et ses endpoints dédiés

Ces trois chantiers doivent être faits dans cet ordre. La sécurité admin doit être stabilisée avant d'ouvrir le portail client.

---

## Étape 1 — Finaliser les méthodes async restantes

### 1.1 Audit logging — `AuditServiceImpl.save()`

**Problème actuel :**
L'aspect `@Audited` intercepte chaque appel controller et fait une écriture DB synchrone avant de retourner la réponse.

**Action :**
- Ajouter `@Async` sur `AuditServiceImpl.save(AuditLog)`
- Ajouter `@Transactional(propagation = Propagation.REQUIRES_NEW)` pour isoler la transaction d'audit
- Vérifier que `AsyncUncaughtExceptionHandler` dans `AsyncConfig` log correctement les erreurs

**Précaution :**
L'audit ne doit pas rater silencieusement. Si le thread async échoue, l'`AsyncUncaughtExceptionHandler` log l'erreur. La requête principale n'est pas bloquée.

**Fichiers :**
- `core/audit/service/implementation/AuditServiceImpl.java`
- `core/audit/aop/AspectAudit.java` (aucun changement — appelle déjà save())

---

### 1.2 PDF factures — `BillingDocumentPdfServiceImpl.generatePdf()`

**Problème actuel :**
Même bug que les contrats PDF : conversion HTML → Jsoup XHTML → OpenHTMLtoPDF inutile. La conversion Jsoup produit du XHTML invalide que le parseur rejette silencieusement → PDF vide.

**Action :**
- Supprimer la conversion Jsoup dans `renderPdf()`
- Passer le HTML Thymeleaf directement à `builder.withHtmlContent(html, baseUri)`
- Même fix que `ContractGenerationServiceImpl.renderPdf()` déjà corrigé

**Fichiers :**
- `features/billing/service/implementation/BillingDocumentPdfServiceImpl.java`

---

### 1.3 Notifications — `NotificationServiceImpl.send()`

**Problème actuel :**
Appelé synchronement depuis des services métier (booking confirmé, paiement reçu, etc.).

**Action :**
- Ajouter `@Async` sur `send(SendNotificationRequest request)`

**Fichiers :**
- `features/notification/service/implementation/NotificationServiceImpl.java`

---

### 1.4 Email OTP — `OttMailServiceImpl.sendOneTimeTokenMail()`

**Problème actuel :**
Appelé pendant le flow reset password. Le token est déjà créé et sauvegardé avant l'appel mail. L'envoi lui-même peut être async.

**Action :**
- Ajouter `@Async` sur la méthode d'envoi

**Fichiers :**
- `core/communication/mailService/implementation/OttMailServiceImpl.java`

---

## Étape 2 — Sécurité Admin (RBAC granulaire)

### 2.1 Problème actuel

```java
// SecurityConfig.java — configuration actuelle
.requestMatchers(ApiPath.V1 + "/**").permitAll()   // ← tous les endpoints /v1 ouverts
.requestMatchers(ApiPath.V1 + "/admin/**").hasRole("ADMIN")
```

Tous les endpoints `/v1/**` sont accessibles sans authentification. Seul `/admin/**` vérifie un rôle.

### 2.2 Nouveau modèle de protection

**Trois niveaux de protection :**

```
PUBLIC        → /v1/auth/**  /v1/client/auth/**  OPTIONS /**
ADMIN         → /v1/**  (tout sauf client)  → JWT realm=ADMIN + permission
CLIENT        → /v1/client/**              → JWT realm=CLIENT
```

**Modifier `SecurityConfig` :**

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers(OPTIONS, "/**").permitAll()
    // Auth publique
    .requestMatchers(POST, V1 + "/auth/login", V1 + "/auth/refresh").permitAll()
    .requestMatchers(POST, V1 + "/auth/password-reset/**").permitAll()
    // Client platform — géré par son propre filtre
    .requestMatchers(V1 + "/client/auth/**").permitAll()
    .requestMatchers(V1 + "/client/**").hasRole("CLIENT")
    // Admin — tout le reste nécessite une auth
    .requestMatchers(V1 + "/**").authenticated()
    .anyRequest().authenticated()
)
```

### 2.3 Rôles Admin à créer en DB

| Rôle | Description | Usage |
|------|-------------|-------|
| `SUPER_ADMIN` | Accès total + gestion des utilisateurs et rôles | 1 seul compte |
| `ADMIN` | Administration complète sauf gestion système | Direction |
| `MANAGER` | Billing, paiements, clients, réservations, abonnements | Responsable opérationnel |
| `CASHIER` | Caisse, encaissements, remboursements | Agent de caisse |
| `STAFF` | Réservations, ressources, consultation clients | Personnel d'accueil |
| `AUDITOR` | Lecture seule sur tout + rapports | Comptable / auditeur |

### 2.4 Permissions à créer en DB

Format : `MODULE:ACTION`

```
# Billing
BILLING:READ
BILLING:CREATE
BILLING:UPDATE
BILLING:CANCEL
BILLING:SEND

# Payment
PAYMENT:READ
PAYMENT:PROCESS
PAYMENT:REFUND
PAYMENT:VOID

# Cash Register
CASH:READ
CASH:OPEN_SESSION
CASH:CLOSE_SESSION
CASH:ADJUST

# Booking
BOOKING:READ
BOOKING:CREATE
BOOKING:UPDATE
BOOKING:CANCEL
BOOKING:CHECKIN

# Subscription
SUBSCRIPTION:READ
SUBSCRIPTION:CREATE
SUBSCRIPTION:ACTIVATE
SUBSCRIPTION:CANCEL

# Client (member + customer)
CLIENT:READ
CLIENT:CREATE
CLIENT:UPDATE
CLIENT:DELETE

# KYC
KYC:READ
KYC:APPROVE
KYC:REJECT

# Inventory
INVENTORY:READ
INVENTORY:CREATE
INVENTORY:ADJUST
INVENTORY:APPROVE

# Contract
CONTRACT:READ
CONTRACT:GENERATE
CONTRACT:SIGN

# Document
DOCUMENT:READ
DOCUMENT:UPLOAD
DOCUMENT:APPROVE

# Report
REPORT:VIEW
REPORT:EXPORT

# Notification
NOTIFICATION:READ
NOTIFICATION:SEND

# System
SYSTEM:USERS
SYSTEM:ROLES
SYSTEM:SETTINGS
SYSTEM:AUDIT
```

### 2.5 Attribution des permissions par rôle

| Permission | SUPER_ADMIN | ADMIN | MANAGER | CASHIER | STAFF | AUDITOR |
|-----------|:-----------:|:-----:|:-------:|:-------:|:-----:|:-------:|
| BILLING:READ | ✓ | ✓ | ✓ | ✓ | — | ✓ |
| BILLING:CREATE | ✓ | ✓ | ✓ | — | — | — |
| BILLING:CANCEL | ✓ | ✓ | ✓ | — | — | — |
| PAYMENT:PROCESS | ✓ | ✓ | ✓ | ✓ | — | — |
| PAYMENT:REFUND | ✓ | ✓ | ✓ | — | — | — |
| CASH:OPEN_SESSION | ✓ | ✓ | ✓ | ✓ | — | — |
| CASH:CLOSE_SESSION | ✓ | ✓ | ✓ | ✓ | — | — |
| CASH:ADJUST | ✓ | ✓ | ✓ | — | — | — |
| BOOKING:CREATE | ✓ | ✓ | ✓ | — | ✓ | — |
| BOOKING:CANCEL | ✓ | ✓ | ✓ | — | ✓ | — |
| BOOKING:CHECKIN | ✓ | ✓ | ✓ | — | ✓ | — |
| SUBSCRIPTION:ACTIVATE | ✓ | ✓ | ✓ | — | — | — |
| CLIENT:CREATE | ✓ | ✓ | ✓ | — | ✓ | — |
| CLIENT:DELETE | ✓ | ✓ | — | — | — | — |
| KYC:APPROVE | ✓ | ✓ | ✓ | — | — | — |
| INVENTORY:APPROVE | ✓ | ✓ | ✓ | — | — | — |
| REPORT:VIEW | ✓ | ✓ | ✓ | ✓ | — | ✓ |
| REPORT:EXPORT | ✓ | ✓ | ✓ | — | — | ✓ |
| SYSTEM:USERS | ✓ | — | — | — | — | — |
| SYSTEM:ROLES | ✓ | — | — | — | — | — |
| SYSTEM:AUDIT | ✓ | ✓ | — | — | — | ✓ |

### 2.6 Activation `@PreAuthorize` sur les controllers

Exemple d'implémentation :

```java
// BillingDocumentController
@GetMapping
@PreAuthorize("hasAuthority('BILLING:READ')")
public ResponseEntity<PaginatedResponse<BillingDocumentResponse>> list(...) { }

@PostMapping
@PreAuthorize("hasAuthority('BILLING:CREATE')")
public ResponseEntity<BillingDocumentResponse> create(...) { }

// PaymentController
@PostMapping("/intents/{n}/pay")
@PreAuthorize("hasAuthority('PAYMENT:PROCESS')")
public ResponseEntity<PaymentTransactionResponse> pay(...) { }

// CashRegisterController
@PostMapping("/{code}/sessions/open")
@PreAuthorize("hasAuthority('CASH:OPEN_SESSION')")
public ResponseEntity<CashSessionResponse> openSession(...) { }

// ReportController
@GetMapping("/reports/finance/dashboard")
@PreAuthorize("hasAuthority('REPORT:VIEW')")
public ResponseEntity<FinancialDashboardResponse> dashboard(...) { }
```

### 2.7 Migration DB pour les nouveaux rôles et permissions

Créer un fichier `V85__seed_roles_and_permissions.sql` avec :
- INSERT des 6 rôles
- INSERT de toutes les permissions
- INSERT de la table de liaison `role_permission`

---

## Étape 3 — ClientPlatform

### 3.1 Architecture auth client

**Nouveau type d'utilisateur** : le client n'est pas un `Users` admin — c'est un `Member`, `Customer` ou `BusinessEntity` qui s'authentifie avec son email.

**Nouveau token JWT client :**

```json
{
  "sub": "jean.dupont@email.com",
  "realm": "CLIENT",
  "ownerType": "MEMBER",
  "ownerCode": "MBR-2026-000001",
  "exp": 1714300800
}
```

**Fichiers à créer :**

```
security/client/
  auth/
    ClientAuthController.java       → POST /client/auth/login
                                      POST /client/auth/refresh
                                      POST /client/auth/otp/request
                                      POST /client/auth/otp/verify
    ClientAuthService.java
    ClientAuthServiceImpl.java
    dto/
      ClientLoginRequest.java
      ClientTokenResponse.java
  principal/
    ClientPrincipal.java            → implémente UserDetails
                                      champs: ownerType, ownerCode, email
  filter/
    ClientJWTFilter.java            → vérifie claim realm=CLIENT
                                      charge ClientPrincipal dans SecurityContext
```

**Modifier `JWTService`** pour générer des tokens avec le claim `realm` + `ownerType` + `ownerCode`.

### 3.2 Endpoints ClientPlatform à créer

Tous sous `/sni/api/v1/client/`. Chaque endpoint vérifie que `clientPrincipal.ownerCode` correspond aux données demandées.

#### Auth
```
POST /client/auth/login             → { email, password } → { accessToken, refreshToken }
POST /client/auth/refresh           → { refreshToken } → { accessToken }
POST /client/auth/otp/request       → { email } → 200 OK (envoie OTP)
POST /client/auth/otp/verify        → { email, token } → { accessToken, refreshToken }
POST /client/auth/logout            → invalide le refresh token
```

#### Profil
```
GET  /client/me                     → infos du member/customer connecté
PUT  /client/me                     → modifier son profil (nom, téléphone)
POST /client/me/password            → changer son mot de passe
```

#### Réservations
```
GET  /client/bookings               → ses propres réservations (paginated)
POST /client/bookings               → créer une réservation
GET  /client/bookings/{number}      → détail d'une réservation
POST /client/bookings/{number}/cancel → annuler
GET  /client/resources/availability → vérifier disponibilité (public)
GET  /client/resources              → liste des ressources disponibles (public)
```

#### Facturation
```
GET  /client/invoices               → ses factures (paginated)
GET  /client/invoices/{number}      → détail facture
GET  /client/invoices/{number}/pdf  → télécharger PDF
```

#### Abonnement
```
GET  /client/subscription           → son abonnement actif
GET  /client/subscription/entitlements → ses soldes d'entitlement (heures restantes)
GET  /client/passes                 → ses passes
GET  /client/passes/{number}        → détail d'un pass
```

#### Paiements
```
GET  /client/payments               → historique de ses paiements
GET  /client/wallet/balance         → solde de son portefeuille
POST /client/wallet/deposit/mobile-money → déposer via MTN/Orange/Airtel
```

#### Contrats et documents
```
GET  /client/contracts              → ses contrats
GET  /client/contracts/{code}/download → télécharger un contrat
GET  /client/documents              → ses documents KYC soumis
POST /client/documents/upload       → soumettre un document KYC
```

#### Notifications
```
GET  /client/notifications          → ses notifications (paginated)
POST /client/notifications/{n}/read → marquer comme lu
```

### 3.3 Ownership guard

Chaque endpoint client doit vérifier que la donnée appartient au client connecté. Créer un composant réutilisable :

```java
// security/client/guard/ClientOwnershipGuard.java
@Component
public class ClientOwnershipGuard {

    public void assertOwnsBooking(String bookingNumber, ClientPrincipal principal) {
        Booking booking = bookingRepository.findByBookingNumber(bookingNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (!booking.getOwnerCode().equals(principal.getOwnerCode())) {
            throw new AccessDeniedException("Access denied");
        }
    }

    public void assertOwnsDocument(String documentCode, ClientPrincipal principal) { ... }
    public void assertOwnsInvoice(String documentNumber, ClientPrincipal principal) { ... }
}
```

Usage dans les services client :
```java
@GetMapping("/{number}")
public ResponseEntity<BookingResponse> getBooking(@PathVariable String number, Authentication auth) {
    ClientPrincipal client = (ClientPrincipal) auth.getPrincipal();
    ownershipGuard.assertOwnsBooking(number, client);
    return ResponseEntity.ok(bookingService.getByNumber(number));
}
```

### 3.4 Ce que le client NE peut PAS faire

- Voir les données d'autres clients
- Accéder aux endpoints admin (`/v1/billing`, `/v1/payments`, `/v1/members`, etc.)
- Modifier les prix, statuts, configurations
- Générer des factures (il peut seulement voir les siennes)
- Accéder aux rapports financiers globaux
- Gérer les caisses ou sessions
- Approuver des documents KYC

---

## Étape 4 — CORS et sécurité réseau

### 4.1 CORS actuel (trop permissif)

```java
config.setAllowedOriginPatterns(List.of("*"));   // ← à restreindre
```

### 4.2 Configuration cible

```java
// Depuis application.yml
app.cors.admin-origins=https://admin.bokaticowork.com,http://localhost:3000
app.cors.client-origins=https://app.bokaticowork.com,http://localhost:3001

// SecurityConfig
config.setAllowedOriginPatterns(List.of(
    adminOrigins,   // origins de l'app admin
    clientOrigins   // origins du portail client
));
```

### 4.3 Rate limiting à envisager

Pour les endpoints auth (éviter brute force) :
- Max 5 tentatives de login par IP par minute
- Lockout 15 min après 10 échecs consécutifs
- La logique `failedLoginAttempts` existe déjà dans le modèle `Users`

---

## Ordre de livraison recommandé

```
Sprint 1 (async)
├── 1.1 AuditServiceImpl @Async
├── 1.2 BillingDocumentPdfServiceImpl fix Jsoup
├── 1.3 NotificationServiceImpl @Async
└── 1.4 OttMailServiceImpl @Async

Sprint 2 (sécurité admin)
├── 2.7 Migration V85 — rôles + permissions en DB
├── 2.2 Modifier SecurityConfig (retirer permitAll sur /v1/**)
└── 2.6 Ajouter @PreAuthorize sur tous les controllers

Sprint 3 (client auth)
├── ClientPrincipal + ClientJWTFilter
├── ClientAuthController (login, refresh, OTP)
└── Modifier JWTService (claim realm)

Sprint 4 (endpoints client)
├── ClientBookingController
├── ClientBillingController
├── ClientSubscriptionController
└── ClientWalletController

Sprint 5 (client avancé)
├── ClientDocumentController (upload KYC)
├── ClientNotificationController
├── ClientOwnershipGuard (réutilisable)
└── Tests d'isolation (un client ne voit pas les données d'un autre)

Sprint 6 (CORS + hardening)
├── CORS restreint par origine
├── Rate limiting sur /auth/**
└── Audit des endpoints exposés sans permission
```

---

## Notes importantes

**Rétrocompatibilité admin** : Les endpoints admin existants (`/v1/bookings`, `/v1/billing`, etc.) ne changent pas d'URL. Seule la protection change (de `permitAll` à `authenticated + permission`). Le frontend admin devra envoyer le token JWT dans chaque requête — il le fait déjà si le login est implémenté.

**Pas de rupture pour les intégrations existantes** : Les endpoints publics (auth, availability, resources) restent publics. Les webhooks PawaPay restent accessibles sans auth (ils ont leur propre signature HMAC).

**Migration progressive** : On peut activer les `@PreAuthorize` module par module sans tout faire en une fois. Commencer par BILLING et PAYMENT (les plus critiques), puis étendre aux autres.