# Client Portal Module — Spécification & Plan d'implémentation

**Date :** 2026-06-14  
**Branche :** `feature/client-portal`  
**Statut :** Conception

---

## Table des matières

1. [Contexte & Motivation](#1-contexte--motivation)
2. [Périmètre Fonctionnel](#2-périmètre-fonctionnel)
3. [Authentification & Onboarding](#3-authentification--onboarding)
4. [Architecture du Module](#4-architecture-du-module)
5. [Sécurité](#5-sécurité)
6. [Endpoints — Portal Client](#6-endpoints--portal-client)
7. [Endpoints — KYC Client](#7-endpoints--kyc-client)
8. [Espace Support](#8-espace-support)
9. [Best Practices Backend](#9-best-practices-backend)
10. [DTOs principaux](#10-dtos-principaux)
11. [Phases d'implémentation](#11-phases-dimplmentation)
12. [Migrations Flyway](#12-migrations-flyway)
13. [Points d'attention](#13-points-dattention)

---

## 1. Contexte & Motivation

L'ensemble du backend actuel est orienté administration : toutes les routes `/sni/api/v1/**` (sauf `/public/**`) passent par `AdminApiAuthorizationManager` et exigent un rôle admin. Seuls deux débuts de surface client existent :

- `ClientBookingController` — uniquement `POST /` (création)
- `ClientSupportController` — complet (tickets, messages, pièces jointes)

L'objectif est de créer un **module portal client autonome** exposant une surface cohérente et sécurisée pour les **membres** (utilisateurs finaux du coworking), sans dupliquer la logique métier des services existants.

---

## 2. Périmètre Fonctionnel

| Espace | Préfixe | Description |
|--------|---------|-------------|
| Auth / Onboarding | `/auth/**` | Inscription, connexion, vérification, réinitialisation |
| Profil | `/client/me` | Données personnelles, avatar, mot de passe |
| Réservations | `/client/bookings` | Créer, consulter, annuler mes réservations |
| Espaces Réservables | `/client/resources` | Parcourir et vérifier disponibilité |
| Espace Facture | `/client/billing` | Factures, devis, paiements, suivi dépenses |
| Wallet | `/client/wallet` | Solde, recharge, transactions |
| Espace Documentaire | `/client/documents` | Contrats, KYC |
| Suivi Abonnement | `/client/subscriptions` | Abonnements, passes, quotas, échéances |
| Notifications | `/client/notifications` | Notifications in-app et préférences |
| Paramètres | `/client/settings` | Langue, timezone, sessions actives |
| Support | `/client/support` | Tickets, messagerie, base de connaissances |

---

## 3. Authentification & Onboarding

### 3.1 État de l'existant

| Fonctionnalité | État | Localisation |
|----------------|------|-------------|
| Login email/password | ✅ Implémenté | `AuthenticationController` |
| Refresh token (rotatif) | ✅ Implémenté | `RefreshTokenServiceImpl` |
| OTT / Magic code (6 chiffres) | ✅ Implémenté | `OneTimeTokenServiceImpl` |
| Réinitialisation mot de passe | ✅ Implémenté | `PasswordResetServiceImpl` |
| Logout + révocation token | ✅ Implémenté | `AuthenticationController` |
| Création membre (self-service) | ✅ Implémenté | `MemberServiceImpl.create()` |
| Activation portal | ✅ Implémenté | `UserProvisioningServiceImpl` |
| Initialisation KYC automatique | ✅ Implémenté | `KycAutomationServiceImpl` |
| Vérification email (resend) | ⚠️ Partiel | OTT sert de vérification, pas de resend dédié |
| OAuth2 / Google | ❌ Absent | À concevoir |
| 2FA TOTP | ❌ Absent | À concevoir (phase ultérieure) |

---

### 3.2 Flows d'inscription

#### Flow A — Auto-inscription (Self-Registration)

```
Client                          Backend                         Email
  |                               |                               |
  |-- POST /auth/register ------->|                               |
  |   {firstname, lastname,       |                               |
  |    email, password, phone}    |                               |
  |                               |-- validate (email unique) --->|
  |                               |-- create Users (PENDING) ---->|
  |                               |-- create Member (PENDING) --->|
  |                               |-- create MemberProfile ------>|
  |                               |-- initializeMemberKyc() ----->|
  |                               |-- generateOTT() ------------->|
  |                               |                               |-- email: code 6 chiffres
  |<-- 201 {verificationToken} ---|                               |
  |                               |                               |
  |-- POST /auth/ott/validate --->|                               |
  |   {ottToken, verificationToken}                               |
  |                               |-- completePortalVerification()|
  |                               |   add ROLE_MEMBER             |
  |                               |   isAccountEnabled = true     |
  |                               |   member.status = ACTIVE      |
  |<-- 200 {accessToken,          |                               |
  |         refreshToken} --------|                               |
  |                               |                               |-- email: bienvenue
```

**Endpoint :** `POST /sni/api/v1/auth/register`  
**Statut :** À créer (actuellement `POST /members` — exposé admin)  
**Body :**
```json
{
  "firstname": "Jean",
  "lastname": "Dupont",
  "email": "jean@example.com",
  "password": "MonMotDePasse123!",
  "phone": "+242XXXXXXXX",
  "whatsappPhone": "+242XXXXXXXX",
  "customerType": "INDIVIDUAL"
}
```
**Response :** `201 { verificationToken, message }` (pas d'accessToken avant vérification email)

---

#### Flow B — Invitation par l'admin

```
Admin                           Backend                         Client / Email
  |                               |                               |
  |-- POST /members/admin/create  |                               |
  |   {email, firstname, ...}     |                               |
  |                               |-- create Member (PENDING) --->|
  |                               |-- generate temp password ---->|
  |                               |                               |-- email: credentials
  |<-- 201 {memberId} ------------|                               |
  |                               |                               |
  |-- PATCH /{id}/portal-access/  |                               |
  |   enable (quand KYC ok)       |                               |
  |                               |-- activatePortalMember()      |
  |                               |   add ROLE_MEMBER             |
  |                               |   status = ACTIVE             |
  |                               |                               |-- email: accès activé
  |<-- 200 ----------------------|                               |
```

À la première connexion, forcer le changement de mot de passe (flag `requiresPasswordChange` sur Users).

---

#### Flow C — Onboarding progressif (Profile Completion)

Après inscription, l'accès au portal est accordé mais certaines fonctionnalités sont bloquées jusqu'à complétion du profil/KYC.

```
État            Condition de transition             Fonctionnalités accessibles
─────────────────────────────────────────────────────────────────────────────
EMAIL_PENDING   → email vérifié via OTT             Profil seulement
KYC_PENDING     → documents KYC soumis              Réservations (limitées), Profil
KYC_REVIEW      → dossier en cours d'examen         Idem
KYC_APPROVED    → admin approuve                    Toutes fonctionnalités
```

**Endpoint de statut onboarding :**  
`GET /client/me/onboarding-status`
```json
{
  "emailVerified": true,
  "passwordChanged": false,
  "profileComplete": false,
  "kycStatus": "IN_PROGRESS",
  "kycCompletionPercent": 40,
  "missingDocuments": ["ID_DOCUMENT", "PROOF_OF_ADDRESS"],
  "nextStep": "SUBMIT_KYC",
  "portalFullyActive": false
}
```

---

### 3.3 Méthodes de connexion

#### Méthode 1 — Email / Mot de passe *(existant)*

```
POST /sni/api/v1/auth/login
Body: { email, password }
Response: { accessToken (15 min), refreshToken (7 jours) }
```

Comportement :
- `failedLoginAttempts` incrémenté à chaque échec (tracké sur `Users`)
- Verrouillage compte après **5 tentatives** → `isAccountLocked = true`
- Email de notification envoyé au membre lors du verrouillage
- Déverrouillage : admin ou auto-unlock via `POST /auth/unlock-account` (OTT requis)

---

#### Méthode 2 — OTT / Magic Code *(existant)*

Connexion sans mot de passe via code à 6 chiffres envoyé par email.

```
Étape 1 — Demander un code :
POST /sni/api/v1/auth/ott/request
Body: { email }
Response: { verificationToken (JWT 15 min), message }
→ Email envoyé avec le code 6 chiffres

Étape 2 — Valider le code :
POST /sni/api/v1/auth/ott/validate
Body: { ottToken: "123456", verificationToken: "jwt..." }
Response: { accessToken, refreshToken }
```

Limitations :
- 1 seul OTT actif à la fois par utilisateur (précédents invalidés)
- Expiration : 15 minutes
- Rate limit : 3 demandes par 10 minutes par email

---

#### Méthode 3 — OAuth2 / Google *(à concevoir)*

Connexion via compte Google. Nécessite Spring Security OAuth2 + Google Cloud credentials.

```
Étape 1 — Initiation :
GET /sni/api/v1/auth/oauth2/google
→ Redirect vers consent screen Google

Étape 2 — Callback automatique :
GET /sni/api/v1/auth/oauth2/callback/google?code=xxx&state=yyy
→ Backend échange le code contre un token Google
→ Récupère email, prénom, nom, avatar depuis Google profile
→ Si membre existant avec cet email : connexion directe
→ Si nouveau : création compte (portalAccess=true, email pré-vérifié)
→ Retourne: { accessToken, refreshToken }
```

**Dépendances à ajouter :**
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
```

**Configuration `application.yml` :**
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: email, profile
            redirect-uri: "{baseUrl}/sni/api/v1/auth/oauth2/callback/google"
```

**Nouveau service :** `OAuth2MemberProvisioningService`
- Cherche membre par email Google
- Crée si absent (statut ACTIVE, email vérifié car Google l'a validé)
- Gère le cas du compte existant créé par admin (liaison)

---

#### Méthode 4 — Refresh Token *(existant)*

```
POST /sni/api/v1/auth/refresh
Body: { refreshToken }
Response: { accessToken (nouveau), refreshToken (nouveau — rotation) }
```

Le token précédent est révoqué à chaque refresh (rotation).

---

### 3.4 Vérification email

#### Vérification initiale (via OTT)

Lors de l'auto-inscription, le code OTT envoyé sert simultanément de vérification email. Après `ott/validate`, le compte est activé.

#### Resend code de vérification *(à créer)*

```
POST /sni/api/v1/auth/email/verify/resend
Body: { email }
Response: { verificationToken, message }
```

- Invalide l'OTT précédent
- Génère un nouveau code
- Rate limit : 3 par heure par email

#### Changement d'email *(à créer)*

```
POST /client/me/email/change
Body: { newEmail, currentPassword }
→ Envoie OTT sur le NOUVEL email
POST /client/me/email/change/confirm
Body: { ottToken, verificationToken }
→ Met à jour email sur Users + Member
→ Révoque tous les refresh tokens existants
→ Envoie email de confirmation sur l'ancien email
```

---

### 3.5 Réinitialisation mot de passe *(existant + enrichissement)*

#### Flow actuel (existant)

```
Étape 1 — Demander la réinitialisation :
POST /sni/api/v1/auth/password-reset/request
Body: { email }
→ Génère UUID token (24h d'expiration)
→ Email avec lien de réinitialisation envoyé

Étape 2 — Confirmer le nouveau mot de passe :
POST /sni/api/v1/auth/password-reset/confirm
Body: { token: "uuid...", newPassword: "..." }
→ Valide le token (non utilisé, non expiré)
→ Met à jour passwordHash
→ Révoque TOUS les refresh tokens (sécurité)
→ Email de confirmation envoyé
```

#### Ajout recommandé — Unlock account via OTT

```
POST /sni/api/v1/auth/unlock-account
Body: { email }
→ Vérifie que le compte est locked
→ Génère OTT
→ Email envoyé

POST /sni/api/v1/auth/unlock-account/confirm
Body: { ottToken, verificationToken }
→ isAccountLocked = false
→ failedLoginAttempts = 0
→ Connexion normale
```

---

### 3.6 Endpoints Auth — Récapitulatif complet

| Méthode | Endpoint | Auth | Description |
|---------|----------|------|-------------|
| POST | `/auth/register` | Public | Auto-inscription membre |
| POST | `/auth/login` | Public | Connexion email/password |
| POST | `/auth/refresh` | Public | Rafraîchissement token |
| POST | `/auth/logout` | JWT | Déconnexion + révocation |
| GET | `/auth/me` | JWT | Utilisateur courant |
| POST | `/auth/ott/request` | Public | Demander code OTT |
| POST | `/auth/ott/validate` | Public | Valider code OTT |
| POST | `/auth/email/verify/resend` | Public | Renvoyer code vérification |
| POST | `/auth/password-reset/request` | Public | Demander reset mot de passe |
| POST | `/auth/password-reset/confirm` | Public | Confirmer nouveau mot de passe |
| POST | `/auth/unlock-account` | Public | Demander déverrouillage |
| POST | `/auth/unlock-account/confirm` | Public | Confirmer déverrouillage |
| GET | `/auth/oauth2/google` | Public | Initier login Google |
| GET | `/auth/oauth2/callback/google` | Public | Callback OAuth2 Google |

---

## 4. Architecture du Module

### 4.1 Package

```
features/
  portal/
    context/                    ← résolution du membre authentifié
    profile/
      controller/
      dto/request/
      dto/response/
      service/
    booking/
      controller/
      dto/request/
      dto/response/
      service/
    resource/
      controller/
      dto/response/
      service/
    billing/
      controller/
      dto/request/
      dto/response/
      service/
    wallet/
      controller/
      dto/request/
      dto/response/
      service/
    document/
      controller/
      dto/request/
      dto/response/
      service/
    subscription/
      controller/
      dto/response/
      service/
    notification/
      controller/
      dto/response/
      service/
    settings/
      controller/
      dto/request/
      dto/response/
      service/
    support/
      controller/
      dto/request/
      dto/response/
      service/
```

### 4.2 Principe de délégation

Les services portal **ne contiennent pas de logique métier**. Ils délèguent aux services existants en injectant automatiquement le filtre sur `customerCode` / `memberCode` résolu depuis le JWT.

```
ClientBillingService
  └─ appelle BillingDocumentService.findByCustomer(ctx.getCustomerCode(), filters)
  └─ appelle PaymentService.findByCustomer(ctx.getCustomerCode(), filters)
```

Un membre ne peut jamais accéder aux données d'un autre client.

---

## 5. Sécurité

### 5.1 Nouveau rôle

```java
// security/admin/role/model/Role.java
ROLE_MEMBER
```

Attribué lors de `activatePortalMember()` (existant dans `UserProvisioningServiceImpl`).

### 5.2 ClientPortalAuthorizationManager

```
security/authorization/ClientPortalAuthorizationManager.java
```

Règles :
- Route matchée : `/sni/api/v1/client/**`
- Condition : `ROLE_MEMBER` **ET** `member.portalAccess = true`
- Pas de permissions granulaires — enforcement dans les services via `ClientContextService`

### 5.3 SecurityConfig — ajout

```java
.requestMatchers("/sni/api/v1/client/**")
    .access(clientPortalAuthorizationManager)
```

### 5.4 ClientContextService

```java
// features/portal/context/ClientContextService.java
@Service
public class ClientContextService {
    Member getAuthenticatedMember();
    Customer getAuthenticatedCustomer();
    String getMemberCode();
    String getCustomerCode();
}
```

### 5.5 Politique de tokens

| Token | Durée | Stockage client | Rotation |
|-------|-------|-----------------|----------|
| Access token | 15 min | Memory (pas localStorage) | Non |
| Refresh token | 7 jours | HttpOnly cookie ou secure storage | Oui (à chaque refresh) |
| OTT token JWT | 15 min | Memory | Non |
| Password reset UUID | 24 h | Email uniquement | Usage unique |

---

## 6. Endpoints — Portal Client

### 6.1 Profil — `/client/me`

| Méthode | Endpoint | Description | Délègue à |
|---------|----------|-------------|-----------|
| GET | `/client/me` | Profil complet (member + customer) | `MemberService`, `CustomerService` |
| PUT | `/client/me` | Modifier nom, téléphone, whatsapp | `MemberService.update()` |
| PUT | `/client/me/avatar` | Uploader/modifier photo de profil | `MemberService.updateAvatar()` |
| GET | `/client/me/customer` | Infos organisation/compte de facturation | `CustomerService.findById()` |
| PUT | `/client/me/password` | Changer mot de passe (requiert current) | `UserService.changePassword()` |
| GET | `/client/me/onboarding-status` | État d'avancement onboarding | `ClientOnboardingService` |
| POST | `/client/me/email/change` | Initier changement d'email | `MemberService` + OTT |
| POST | `/client/me/email/change/confirm` | Confirmer changement d'email | `MemberService` |

---

### 6.2 Réservations — `/client/bookings`

| Méthode | Endpoint | Description | Délègue à |
|---------|----------|-------------|-----------|
| GET | `/client/bookings` | Mes réservations paginées | `BookingService.findAll(customerCode, filters)` |
| POST | `/client/bookings` | Créer une réservation | `BookingService.create()` |
| GET | `/client/bookings/{number}` | Détail d'une réservation | `BookingService.findByNumber()` |
| PATCH | `/client/bookings/{number}/cancel` | Annuler (si politique le permet) | `BookingService.cancel()` |
| GET | `/client/bookings/{number}/pdf` | Télécharger confirmation PDF | `BookingService.generatePdf()` |
| POST | `/client/bookings/check-availability` | Vérifier disponibilité | `ResourceAvailabilityService.check()` |

**Filtres GET liste :** `status`, `fromDate`, `toDate`, `resourceType`

---

### 6.3 Espaces Réservables — `/client/resources`

| Méthode | Endpoint | Description | Délègue à |
|---------|----------|-------------|-----------|
| GET | `/client/resources` | Parcourir les espaces (avec filtres) | Elasticsearch |
| GET | `/client/resources/{code}` | Détail d'un espace | `ResourceAvailabilityService.findByCode()` |
| GET | `/client/resources/{code}/calendar` | Calendrier disponibilité | `ResourceAvailabilityService.getCalendar()` |
| POST | `/client/resources/suggestions` | Suggestions selon critères | `BookingService.getSuggestions()` |

**Filtres GET liste :** `type`, `capacity`, `fromDate`, `toDate`

---

### 6.4 Espace Facture — `/client/billing`

#### Documents

| Méthode | Endpoint | Description | Délègue à |
|---------|----------|-------------|-----------|
| GET | `/client/billing/documents` | Mes documents paginés | `BillingDocumentService.findByCustomer()` |
| GET | `/client/billing/documents/{number}` | Détail document | `BillingDocumentService.findByNumber()` |
| GET | `/client/billing/documents/{number}/pdf` | Télécharger PDF | `BillingDocumentService.generatePdf()` |
| PATCH | `/client/billing/quotes/{number}/accept` | Accepter un devis | `BillingDocumentService.accept()` |

**Filtres :** `type` (INVOICE, QUOTE, CREDIT_NOTE), `status`, `fromDate`, `toDate`

#### Paiements

| Méthode | Endpoint | Description | Délègue à |
|---------|----------|-------------|-----------|
| GET | `/client/billing/payments` | Historique paiements | `PaymentService.findByCustomer()` |
| GET | `/client/billing/payments/{number}` | Détail paiement | `PaymentService.findByNumber()` |
| POST | `/client/billing/payments` | Payer un ou plusieurs documents | `PaymentService.createIntent()` |

#### Tableau de bord dépenses

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/client/billing/summary` | Totaux par période, répartition par type |

**Paramètres :** `period` (MONTH, QUARTER, YEAR), `year`, `month`

---

### 6.5 Wallet — `/client/wallet`

| Méthode | Endpoint | Description | Délègue à |
|---------|----------|-------------|-----------|
| GET | `/client/wallet` | Solde + infos wallet | `WalletService.getByCustomer()` |
| POST | `/client/wallet/topup` | Recharger le wallet | `PaymentService` → mobile money |
| GET | `/client/wallet/transactions` | Historique transactions paginé | `WalletService.getTransactions()` |

---

### 6.6 Suivi Abonnement — `/client/subscriptions`

| Méthode | Endpoint | Description | Délègue à |
|---------|----------|-------------|-----------|
| GET | `/client/subscriptions` | Mes abonnements | `SubscriptionService.findByCustomer()` |
| GET | `/client/subscriptions/current` | Abonnement actif actuel | `SubscriptionService.getCurrent()` |
| GET | `/client/subscriptions/{number}` | Détail abonnement | `SubscriptionService.findByNumber()` |
| GET | `/client/subscriptions/{number}/entitlements` | Mes droits et quotas | `SubscriptionService.getEntitlements()` |
| GET | `/client/subscriptions/{number}/usage` | Consommation actuelle | `SubscriptionService.getUsage()` |
| GET | `/client/subscriptions/{number}/billing-schedule` | Prochaines échéances | `SubscriptionService.getBillingSchedule()` |
| GET | `/client/subscriptions/passes` | Mes passes actifs | `PassService.findActiveByMember()` |
| GET | `/client/subscriptions/passes/{number}` | Détail d'un pass | `PassService.findByNumber()` |

---

### 6.7 Notifications — `/client/notifications`

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/client/notifications` | Mes notifications paginées |
| GET | `/client/notifications/unread-count` | Compteur non lus |
| PATCH | `/client/notifications/{id}/read` | Marquer comme lu |
| PATCH | `/client/notifications/read-all` | Tout marquer comme lu |
| DELETE | `/client/notifications/{id}` | Supprimer |
| GET | `/client/notifications/preferences` | Préférences (email, SMS, push) |
| PUT | `/client/notifications/preferences` | Modifier préférences |

---

### 6.8 Paramètres — `/client/settings`

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/client/settings` | Mes paramètres (langue, timezone, devise) |
| PUT | `/client/settings` | Modifier paramètres |
| GET | `/client/settings/sessions` | Sessions actives (device, IP, date) |
| DELETE | `/client/settings/sessions/{id}` | Révoquer une session |
| DELETE | `/client/settings/sessions` | Révoquer toutes les autres sessions |

---

### 6.9 Support — `/client/support` *(existant — `ClientSupportController`)*

#### Tickets

| Méthode | Endpoint | Description | Délègue à |
|---------|----------|-------------|-----------|
| GET | `/client/support/tickets` | Mes tickets paginés | `SupportTicketService.findByMember()` |
| POST | `/client/support/tickets` | Ouvrir un ticket | `SupportTicketService.create()` |
| GET | `/client/support/tickets/{number}` | Détail d'un ticket | `SupportTicketService.findByNumber()` |
| PATCH | `/client/support/tickets/{number}/close` | Fermer un ticket | `SupportTicketService.close()` |
| POST | `/client/support/tickets/{number}/csat` | Évaluer la résolution (1-5) | `SupportTicketService.submitCsat()` |

**Filtres GET liste :** `status` (OPEN, IN_PROGRESS, RESOLVED, CLOSED), `fromDate`, `toDate`

**Body POST ticket :**
```json
{
  "subject": "Problème d'accès à la salle",
  "description": "Je n'arrive pas à accéder à la salle de réunion B...",
  "category": "ACCESS",
  "priority": "MEDIUM"
}
```

**Catégories de tickets :**

| Code | Description |
|------|-------------|
| `ACCESS` | Problème d'accès (badge, porte, QR code) |
| `BILLING` | Question ou litige de facturation |
| `BOOKING` | Problème sur une réservation |
| `TECHNICAL` | Problème technique (WiFi, équipement) |
| `KYC` | Question sur la vérification d'identité |
| `SUBSCRIPTION` | Question sur un abonnement ou pass |
| `OTHER` | Autre demande |

#### Messagerie du ticket

| Méthode | Endpoint | Description | Délègue à |
|---------|----------|-------------|-----------|
| POST | `/client/support/tickets/{number}/messages` | Envoyer un message | `SupportMessageService.create()` |

**Body POST message :**
```json
{
  "content": "J'ai essayé avec mon badge, mais la porte reste fermée.",
  "attachmentIds": []
}
```

#### Pièces jointes

| Méthode | Endpoint | Description | Délègue à |
|---------|----------|-------------|-----------|
| POST | `/client/support/tickets/{number}/attachments` | Uploader une pièce jointe | `SupportAttachmentService.upload()` |
| GET | `/client/support/tickets/{number}/attachments` | Lister les pièces jointes | `SupportAttachmentService.findByTicket()` |
| GET | `/client/support/tickets/{number}/attachments/{id}/download` | Télécharger | `SupportAttachmentService.download()` |

**Règles upload pièces jointes :**
- Formats : PDF, JPG, PNG, MP4 (vidéo courte), ZIP
- Taille max : 20 Mo par fichier, 50 Mo total par ticket
- Max 5 pièces jointes par ticket

#### Base de connaissances — `/client/support/knowledge`

| Méthode | Endpoint | Description | Délègue à |
|---------|----------|-------------|-----------|
| GET | `/client/support/knowledge` | Articles publiés (avec catégories) | `KnowledgeArticleService.findPublished()` |
| GET | `/client/support/knowledge/{slug}` | Détail d'un article | `KnowledgeArticleService.findBySlug()` |
| GET | `/client/support/knowledge/search` | Recherche plein texte | `KnowledgeArticleService.search()` |
| GET | `/client/support/knowledge/categories` | Toutes les catégories | `KnowledgeArticleService.getCategories()` |
| POST | `/client/support/knowledge/{id}/helpful` | Marquer utile / non utile | `KnowledgeArticleService.recordFeedback()` |

**Paramètre :** `GET /knowledge/search?q=badge&category=ACCESS`

---

### Workflow statut ticket

```
OPEN
  │
  ├── Agent répond         → IN_PROGRESS
  │
  └── Membre ferme         → CLOSED
         │
         └── Auto-close après 7 j sans réponse → CLOSED

IN_PROGRESS
  │
  ├── Agent résout         → RESOLVED
  │     └── Membre évalue (CSAT 1-5)
  │
  ├── Membre répond        → IN_PROGRESS (reste)
  │
  └── Membre ferme         → CLOSED

RESOLVED
  │
  ├── Membre confirme      → CLOSED
  └── Membre reouvre       → IN_PROGRESS (dans les 48h)
```

---

## 7. Endpoints — KYC Client

Le KYC est actuellement géré côté admin. Le client a besoin d'une surface pour soumettre et suivre ses documents.

### 7.1 Flow KYC client

```
Membre                          Backend                         Admin
  |                               |                               |
  | GET /client/documents/kyc    |                               |
  |     /requirements             |                               |
  |<-- { requiredDocs: [...] } --|                               |
  |                               |                               |
  | POST /client/documents/kyc   |                               |
  |      /documents               |                               |
  |   { file, documentType,       |                               |
  |     documentNumber,           |                               |
  |     issueDate, expiryDate }   |                               |
  |<-- 201 { kycDocumentId }-----|                               |
  |                               |                               |
  | (répéter pour chaque doc)     |                               |
  |                               |                               |
  | POST /client/documents/kyc   |                               |
  |      /submit                  |                               |
  |<-- 200 { status: SUBMITTED } |                               |
  |                               |-- notifie admin ------------>|
  |                               |                               |-- review
  |                               |<-- PATCH /kyc/{code}/review --|
  |<-- email: KYC résultat ------|                               |
```

---

### 7.2 Endpoints KYC client

#### Dossier KYC

| Méthode | Endpoint | Auth | Description | Délègue à |
|---------|----------|------|-------------|-----------|
| GET | `/client/documents/kyc` | ROLE_MEMBER | Mon dossier KYC (cas + statut global) | `KycCaseService.findByMember()` |
| GET | `/client/documents/kyc/requirements` | ROLE_MEMBER | Liste des documents requis selon mon profil | `KycRequirementService.getForMember()` |
| GET | `/client/documents/kyc/completion` | ROLE_MEMBER | % de complétion + documents manquants | `KycAutomationService` |

#### Documents KYC

| Méthode | Endpoint | Auth | Description | Délègue à |
|---------|----------|------|-------------|-----------|
| GET | `/client/documents/kyc/documents` | ROLE_MEMBER | Liste mes documents soumis | `KycDocumentService.findByMember()` |
| POST | `/client/documents/kyc/documents` | ROLE_MEMBER | Uploader un document | `KycDocumentService.submit()` |
| GET | `/client/documents/kyc/documents/{id}` | ROLE_MEMBER | Détail + statut vérification | `KycDocumentService.findById()` |
| PUT | `/client/documents/kyc/documents/{id}` | ROLE_MEMBER | Re-soumettre après rejet | `KycDocumentService.resubmit()` |
| DELETE | `/client/documents/kyc/documents/{id}` | ROLE_MEMBER | Supprimer (si PENDING seulement) | `KycDocumentService.delete()` |

#### Soumission dossier

| Méthode | Endpoint | Auth | Description |
|---------|----------|------|-------------|
| POST | `/client/documents/kyc/submit` | ROLE_MEMBER | Soumettre le dossier complet pour review |

**Conditions de soumission :**
- Tous les documents requis présents
- Aucun document en statut REJECTED sans re-soumission
- Statut du dossier : IN_PROGRESS ou PENDING_CORRECTION

#### Contrats

| Méthode | Endpoint | Auth | Description | Délègue à |
|---------|----------|------|-------------|-----------|
| GET | `/client/documents/contracts` | ROLE_MEMBER | Mes contrats | `ContractService.findByCustomer()` |
| GET | `/client/documents/contracts/{number}` | ROLE_MEMBER | Détail contrat | `ContractService.findByNumber()` |
| GET | `/client/documents/contracts/{number}/pdf` | ROLE_MEMBER | Télécharger PDF | `ContractService.generatePdf()` |
| POST | `/client/documents/contracts/{number}/sign` | ROLE_MEMBER | Signer électroniquement | `ContractService.sign()` |

---

### 7.3 Upload de documents — Détail

**Endpoint :** `POST /client/documents/kyc/documents`  
**Content-Type :** `multipart/form-data`

```
Champs form-data :
  file           : fichier (PDF, JPG, PNG — max 10 Mo)
  documentType   : string (voir types ci-dessous)
  documentNumber : string (optionnel selon type)
  issueDate      : date ISO (optionnel)
  expiryDate     : date ISO (requis si document expirant)
```

**Types de documents acceptés :**

| Code | Label | Expiration |
|------|-------|------------|
| `NATIONAL_ID` | Carte nationale d'identité | Oui |
| `PASSPORT` | Passeport | Oui |
| `RESIDENCE_PERMIT` | Titre de séjour | Oui |
| `PROOF_OF_ADDRESS` | Justificatif de domicile | Non |
| `COMPANY_REGISTRATION` | Registre de commerce | Oui |
| `TAX_CERTIFICATE` | Attestation fiscale | Oui |
| `PHOTO` | Photo d'identité | Non |
| `BANK_STATEMENT` | Relevé bancaire | Non |
| `OTHER` | Autre document | Non |

**Règles de validation upload :**
- Formats acceptés : `application/pdf`, `image/jpeg`, `image/png`
- Taille max : 10 Mo
- 1 fichier par `documentType` par dossier KYC (remplacement possible si PENDING)
- Scan de virus recommandé avant stockage (ClamAV ou équivalent)
- Stockage : filesystem ou S3 (selon config `document.storage`)

---

## 9. Best Practices Backend

### 9.1 Rate Limiting *(RateLimitingFilter existant à étendre)*

| Endpoint | Limite | Fenêtre | Action si dépassé |
|----------|--------|---------|-------------------|
| `POST /auth/login` | 5 tentatives | 10 min / IP | 429 + lockout après 5 |
| `POST /auth/register` | 3 tentatives | 1 h / IP | 429 |
| `POST /auth/ott/request` | 3 demandes | 10 min / email | 429 |
| `POST /auth/password-reset/request` | 3 demandes | 1 h / email | 429 (pas d'info sur existence compte) |
| `POST /client/documents/kyc/documents` | 10 uploads | 1 h / membre | 429 |
| `POST /client/billing/payments` | 10 tentatives | 1 h / membre | 429 |
| `POST /client/support/tickets` | 5 tickets | 1 h / membre | 429 |
| `POST /client/support/tickets/{n}/messages` | 20 messages | 1 h / membre | 429 |
| Autres `/client/**` | 100 req | 1 min / membre | 429 |

---

### 9.2 Account Lockout *(Users.failedLoginAttempts existant)*

```
Après 5 échecs de connexion :
  isAccountLocked = true
  Email de notification envoyé au membre
  
Déverrouillage :
  Option A : Admin via PATCH /members/{id}/status
  Option B : Auto-unlock via POST /auth/unlock-account (OTT requis)
  Option C : Auto-unlock après 30 minutes (à configurer)
```

**Response en cas de compte verrouillé :**
```json
{
  "success": false,
  "errorCode": "ACCOUNT_LOCKED",
  "errorDescription": "Votre compte est temporairement verrouillé. Vérifiez votre email pour déverrouiller.",
  "data": null
}
```

---

### 9.3 Rotation des Refresh Tokens *(existant)*

À chaque utilisation d'un refresh token :
1. L'ancien token est révoqué (`revoked = true`)
2. Un nouveau est généré et stocké
3. Si un token révoqué est réutilisé → **tous les tokens de l'utilisateur sont révoqués** (détection de vol)

---

### 9.4 Idempotency *(module existant)*

Les endpoints créant une ressource ou déclenchant un paiement **doivent** accepter un header idempotency key :

```
Header: Idempotency-Key: <uuid-client-généré>
```

Endpoints concernés :
- `POST /client/bookings` → évite double réservation
- `POST /client/billing/payments` → évite double paiement
- `POST /client/wallet/topup` → évite double recharge
- `POST /client/documents/kyc/submit` → idempotent
- `POST /client/support/tickets/{n}/csat` → idempotent (une seule évaluation par ticket)

---

### 9.5 Pagination — Standard

Toutes les listes suivent `PaginatedResponse<T>` existant.

**Paramètres query par défaut :**

| Paramètre | Défaut | Description |
|-----------|--------|-------------|
| `page` | 0 | Page (0-indexed) |
| `size` | 20 | Éléments par page (max 100) |
| `sort` | `createdAt,desc` | Tri |

---

### 9.6 Codes d'erreur client-friendly

Étendre `ApiError` existant avec des codes spécifiques au portal :

| Code | HTTP | Description |
|------|------|-------------|
| `ACCOUNT_LOCKED` | 403 | Compte verrouillé après trop d'échecs |
| `EMAIL_NOT_VERIFIED` | 403 | Email pas encore vérifié |
| `PORTAL_ACCESS_DISABLED` | 403 | portalAccess = false |
| `KYC_REQUIRED` | 403 | Action nécessite KYC approuvé |
| `KYC_PENDING` | 403 | KYC en cours d'examen |
| `BOOKING_NOT_CANCELLABLE` | 422 | Délai d'annulation dépassé |
| `INSUFFICIENT_WALLET_BALANCE` | 422 | Solde wallet insuffisant |
| `DOCUMENT_TYPE_ALREADY_SUBMITTED` | 409 | Document de ce type déjà soumis |
| `KYC_ALREADY_SUBMITTED` | 409 | Dossier KYC déjà soumis |
| `OTT_EXPIRED` | 400 | Code OTT expiré |
| `OTT_INVALID` | 400 | Code OTT invalide |
| `REFRESH_TOKEN_REVOKED` | 401 | Token de refresh révoqué |
| `RESOURCE_UNAVAILABLE` | 409 | Créneau non disponible |
| `TICKET_ALREADY_CLOSED` | 409 | Ticket déjà fermé |
| `TICKET_NOT_REOPENABLE` | 422 | Délai de réouverture (48h) dépassé |
| `CSAT_ALREADY_SUBMITTED` | 409 | Évaluation déjà soumise pour ce ticket |
| `TICKET_ATTACHMENT_LIMIT` | 422 | Limite de pièces jointes atteinte |

---

### 9.7 Onboarding Progress — Middleware

Créer un `ClientOnboardingGuard` (AOP ou intercepteur) qui vérifie avant certaines actions si le membre peut les effectuer :

```
Actions bloquées si email non vérifié :
  → toutes sauf GET /client/me et /client/me/onboarding-status

Actions bloquées si KYC non soumis :
  → POST /client/bookings, POST /client/billing/payments, POST /client/wallet/topup

Actions bloquées si KYC non approuvé :
  → POST /client/documents/contracts/{number}/sign
```

Response si bloqué :
```json
{
  "success": false,
  "errorCode": "KYC_REQUIRED",
  "errorDescription": "Veuillez compléter votre vérification d'identité pour effectuer une réservation.",
  "data": { "redirectTo": "/client/documents/kyc" }
}
```

---

### 9.8 Sécurité des uploads

```
Validation à appliquer sur POST /client/documents/kyc/documents :
  1. Vérifier Content-Type et extension (pas de confiance au client)
  2. Vérifier magic bytes du fichier (PDF: %PDF, JPG: FF D8, PNG: 89 50)
  3. Taille max configurée (10 Mo)
  4. Nommer le fichier côté serveur (jamais utiliser le nom original)
  5. Stocker hors du webroot (pas accessible directement via URL)
  6. Lien de téléchargement signé temporaire (30 min) pour l'accès
```

---

### 9.9 Sessions et traçabilité

Enrichir le `RefreshToken` avec :
- `userAgent` — navigateur/app
- `ipAddress` — IP de connexion
- `deviceName` — label optionnel
- `lastUsedAt` — dernière utilisation

Exposé via `GET /client/settings/sessions` pour que le membre voit et révoque ses sessions.

---

## 10. DTOs principaux

### ClientProfileResponse

```json
{
  "memberId": "MBR-202601-0001",
  "firstname": "Jean",
  "lastname": "Dupont",
  "email": "jean@example.com",
  "phone": "+242XXXXXXXX",
  "whatsappPhone": "+242XXXXXXXX",
  "avatarUrl": "https://...",
  "status": "ACTIVE",
  "portalAccess": true,
  "customer": {
    "customerCode": "CST-2026-001",
    "type": "INDIVIDUAL",
    "displayName": "Jean Dupont",
    "billingEmail": "jean@example.com",
    "address": { "city": "Brazzaville", "country": "CG" }
  },
  "createdAt": "2026-01-15 09:00:00"
}
```

### OnboardingStatusResponse

```json
{
  "emailVerified": true,
  "passwordChanged": true,
  "profileComplete": false,
  "kycStatus": "IN_PROGRESS",
  "kycCompletionPercent": 40,
  "submittedDocuments": ["NATIONAL_ID"],
  "missingDocuments": ["PROOF_OF_ADDRESS", "PHOTO"],
  "nextStep": "UPLOAD_PROOF_OF_ADDRESS",
  "portalFullyActive": false
}
```

### ClientKycStatusResponse

```json
{
  "caseCode": "KYCCASE-20260115-0001",
  "status": "IN_PROGRESS",
  "riskLevel": "LOW",
  "kycLevel": 1,
  "completionPercent": 40,
  "startedAt": "2026-01-15 09:00:00",
  "submittedAt": null,
  "reviewedAt": null,
  "documents": [
    {
      "id": 1,
      "documentType": "NATIONAL_ID",
      "status": "VERIFIED",
      "documentNumber": "CG-123456",
      "expiryDate": "2030-01-01"
    },
    {
      "id": 2,
      "documentType": "PROOF_OF_ADDRESS",
      "status": "PENDING",
      "uploadedAt": "2026-06-14 10:00:00"
    }
  ],
  "requiredDocuments": ["NATIONAL_ID", "PROOF_OF_ADDRESS", "PHOTO"]
}
```

### ClientBookingSummaryResponse

```json
{
  "bookingNumber": "BKG-2026-0001",
  "resourceName": "Salle de réunion A",
  "resourceType": "MEETING_ROOM",
  "startDate": "2026-06-20",
  "startTime": "09:00",
  "endTime": "11:00",
  "status": "CONFIRMED",
  "totalAmount": 15000,
  "currency": "XAF",
  "cancellableUntil": "2026-06-19 18:00:00"
}
```

### ClientBillingSummaryResponse

```json
{
  "period": "2026-06",
  "totalInvoiced": 85000,
  "totalPaid": 60000,
  "totalPending": 25000,
  "currency": "XAF",
  "breakdown": [
    { "type": "BOOKING", "amount": 45000, "count": 3 },
    { "type": "SUBSCRIPTION", "amount": 40000, "count": 1 }
  ]
}
```

### RegisterRequest *(nouveau)*

```json
{
  "firstname": "Jean",
  "lastname": "Dupont",
  "email": "jean@example.com",
  "password": "MonMotDePasse123!",
  "phone": "+242XXXXXXXX",
  "whatsappPhone": "+242XXXXXXXX",
  "customerType": "INDIVIDUAL"
}
```

### LoginResponse *(existant)*

```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

### ClientSupportTicketResponse

```json
{
  "ticketNumber": "TKT-2026-0042",
  "subject": "Problème d'accès à la salle",
  "category": "ACCESS",
  "priority": "MEDIUM",
  "status": "IN_PROGRESS",
  "csatScore": null,
  "createdAt": "2026-06-14 09:30:00",
  "updatedAt": "2026-06-14 11:00:00",
  "resolvedAt": null,
  "closedAt": null,
  "messages": [
    {
      "id": 1,
      "content": "Je n'arrive pas à accéder à la salle de réunion B...",
      "senderType": "MEMBER",
      "senderName": "Jean Dupont",
      "sentAt": "2026-06-14 09:30:00",
      "attachments": []
    },
    {
      "id": 2,
      "content": "Bonjour Jean, nous allons vérifier votre accès.",
      "senderType": "AGENT",
      "senderName": "Support Bokati",
      "sentAt": "2026-06-14 10:15:00",
      "attachments": []
    }
  ],
  "attachments": [],
  "canReopen": false,
  "canClose": true
}
```

### ClientSupportTicketSummaryResponse

```json
{
  "ticketNumber": "TKT-2026-0042",
  "subject": "Problème d'accès à la salle",
  "category": "ACCESS",
  "status": "IN_PROGRESS",
  "hasUnreadMessages": true,
  "lastMessageAt": "2026-06-14 11:00:00",
  "createdAt": "2026-06-14 09:30:00"
}
```

### KnowledgeArticleResponse

```json
{
  "id": 12,
  "slug": "comment-reserver-une-salle",
  "title": "Comment réserver une salle de réunion ?",
  "content": "...",
  "category": "BOOKING",
  "tags": ["réservation", "salle", "guide"],
  "readingTimeMinutes": 3,
  "helpfulCount": 45,
  "publishedAt": "2026-03-01 00:00:00"
}
```

---

## 11. Phases d'implémentation

### Phase 1 — Fondation sécurité *(débloquer tout le reste)*

- [ ] Ajouter `ROLE_MEMBER` dans `Role.java`
- [ ] Créer `ClientPortalAuthorizationManager`
- [ ] Mettre à jour `SecurityConfig`
- [ ] Créer `ClientContextService`
- [ ] Tests : accès refusé sans `ROLE_MEMBER`, accès refusé si `portalAccess = false`

### Phase 2 — Auth & Onboarding

- [ ] `POST /auth/register` (endpoint public d'auto-inscription)
- [ ] `POST /auth/email/verify/resend`
- [ ] `POST /auth/unlock-account` + `/confirm`
- [ ] `POST /client/me/email/change` + `/confirm`
- [ ] `GET /client/me/onboarding-status` + `ClientOnboardingService`
- [ ] `ClientOnboardingGuard` (intercepteur pour bloquer selon état KYC)
- [ ] Enrichir `RefreshToken` avec `userAgent`, `ipAddress`, `lastUsedAt`

### Phase 3 — Profil

- [ ] `ClientProfileController`
- [ ] `ClientProfileService`
- [ ] DTOs : `ClientProfileResponse`, `UpdateClientProfileRequest`

### Phase 4 — KYC Client

- [ ] `ClientKycController`
- [ ] `ClientKycService` (wraps `KycCaseService`, `KycDocumentService`)
- [ ] Validation sécurisée des uploads (magic bytes, nommage serveur)
- [ ] `GET /client/documents/kyc/requirements`
- [ ] `POST /client/documents/kyc/submit`
- [ ] DTOs : `ClientKycStatusResponse`, `KycDocumentUploadRequest`

### Phase 5 — Réservations + Espaces Réservables

- [ ] `ClientBookingController` (remplace et étend l'existant)
- [ ] `ClientBookingService`
- [ ] `ClientResourceController` + `ClientResourceService`

### Phase 6 — Espace Facture

- [ ] `ClientBillingController`
- [ ] `ClientBillingService`
- [ ] `ClientBillingSummaryResponse` (agrégation dépenses)

### Phase 7 — Wallet

- [ ] `ClientWalletController`
- [ ] `ClientWalletService`

### Phase 8 — Contrats

- [ ] `ClientContractController`
- [ ] `ClientContractService`

### Phase 9 — Suivi Abonnement

- [ ] `ClientSubscriptionController`
- [ ] `ClientSubscriptionService`

### Phase 10 — Support *(raffiner l'existant `ClientSupportController`)*

- [ ] Vérifier et aligner `ClientSupportController` avec la spec (catégories, filtres, CSAT)
- [ ] `ClientSupportService` (facade, scoped sur `memberCode`)
- [ ] `GET /client/support/knowledge` + `/search` + `/categories` + `/{slug}`
- [ ] `POST /client/support/knowledge/{id}/helpful`
- [ ] Auto-close ticket après 7 jours sans réponse (scheduled worker)
- [ ] Réouverture ticket dans les 48h après résolution
- [ ] DTOs : `ClientSupportTicketResponse`, `ClientSupportTicketSummaryResponse`, `KnowledgeArticleResponse`

### Phase 11 — Notifications + Paramètres + Sessions

- [ ] `ClientNotificationController` + service
- [ ] `ClientSettingsController` + service
- [ ] `GET /client/settings/sessions` (depuis RefreshToken enrichi)

### Phase 12 — OAuth2 Google *(optionnel, phase ultérieure)*

- [ ] Ajouter dépendance `spring-boot-starter-oauth2-client`
- [ ] `OAuth2MemberProvisioningService`
- [ ] Config Google dans `application.yml`
- [ ] Migration si ajout colonnes `oauth2Provider`, `oauth2ProviderId` sur `Users`

---

## 12. Migrations Flyway

| Migration | Contenu | Phase |
|-----------|---------|-------|
| V109 | Colonnes `user_agent`, `ip_address`, `device_name`, `last_used_at` sur `refresh_tokens` | 2 |
| V110 | Table `client_settings` (langue, timezone, devise par membre) | 11 |
| V111 | Table `notification_preferences` (canal email/SMS/push par type d'event) | 11 |
| V112 (si absent) | Colonne `category` sur `support_tickets` + index sur `member_id, status` | 10 |
| V113 | Colonnes `oauth2_provider`, `oauth2_provider_id` sur `users` (nullable) | 12 |
| V114 | Flag `requires_password_change` sur `users` | 2 |

---

## 13. Points d'attention

### Isolation des données
Chaque service portal commence par `clientContextService.getCustomerCode()`. Aucun endpoint client n'accepte de `customerId` en paramètre d'URL — le contexte est toujours résolu depuis le JWT.

### portalAccess
Un membre peut avoir `portalAccess = false`. Le `ClientPortalAuthorizationManager` rejette avec `403 PORTAL_ACCESS_DISABLED` avant même d'atteindre le controller.

### Email non vérifié
Après inscription, l'access token n'est pas retourné immédiatement — seulement après validation de l'OTT. Cela garantit que l'email est valide avant le premier accès.

### Compatibilité services existants
Les services admin restent inchangés. Les services portal sont des facades légères qui appellent les mêmes services avec les bons filtres. Aucune modification des services existants.

### Mobile Money
Les paiements (topup wallet, billing) délèguent au flow PawaPay existant (COG + COD operators). Le callback PawaPay est déjà public et fonctionnel.

### Uploads KYC et support
Ne jamais stocker les fichiers dans le dossier `resources/` du projet. Utiliser un chemin externe configuré dans `application.yml` (`document.storage.path`). Les URLs de téléchargement doivent être signées et temporaires. La même règle s'applique aux pièces jointes de tickets support.

### Support — `ClientSupportController` existant
Le controller existe déjà sous `/sni/api/v1/client/support/tickets`. À la Phase 10, valider qu'il est bien sécurisé par `ClientPortalAuthorizationManager` (et non par une règle admin), qu'il filtre sur le `memberCode` authentifié, et qu'il expose les bonnes catégories. Le `ClientKnowledgeArticleController` existant est à intégrer sous `/client/support/knowledge` pour homogénéiser le préfixe.

### Auto-close tickets
Un worker schedulé (toutes les heures ou quotidien) doit fermer automatiquement les tickets en statut RESOLVED sans réponse du membre depuis 7 jours. À ajouter dans `BackgroundWorkers` (voir `project_background_workers.md`).
