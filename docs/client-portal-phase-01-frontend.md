# Client Portal — Phase 1 : Sécurité & Accès
## Guide d'intégration Frontend

**Date :** 2026-06-14  
**Phase :** 1 — Fondation sécurité  
**Statut :** Implémenté

---

## 1. Ce qui change pour le frontend

Avant Phase 1, toutes les routes `/sni/api/v1/**` exigeaient un rôle admin. Après Phase 1 :

- `/sni/api/v1/client/**` → réservé aux **membres authentifiés** (`ROLE_MEMBER` + `portalAccess = true`)
- `/sni/api/v1/**` → inchangé, toujours réservé aux admins
- `/sni/api/v1/public/**` → toujours public, sans authentification

---

## 2. Authentification — Flux de base

### 2.1 Login

```
POST /sni/api/v1/auth/login
Content-Type: application/json

{
  "email": "jean@example.com",
  "password": "MonMotDePasse123!"
}
```

**Réponse succès (200) :**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900
  }
}
```

**Réponse erreur compte verrouillé (401) :**
```json
{
  "success": false,
  "errorCode": "ACCOUNT_LOCKED",
  "errorDescription": "Account is locked"
}
```

**Réponse erreur identifiants (401) :**
```json
{
  "success": false,
  "errorCode": "BAD_CREDENTIALS",
  "errorDescription": "Invalid email or password"
}
```

---

### 2.2 Utiliser le token

Ajouter sur **chaque requête** vers `/client/**` :

```
Authorization: Bearer {accessToken}
```

---

### 2.3 Refresh du token

À faire **avant** que l'access token expire (durée : 15 min / 900 s).

```
POST /sni/api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Réponse (200) :**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "eyJ...",
    "expiresIn": 900
  }
}
```

> Le refresh token est **rotatif** : l'ancien est révoqué dès utilisation. Toujours stocker le nouveau.

**Stratégie recommandée :**
```
Rafraîchir 60 secondes avant expiration (à t = 840 s)
Si refresh échoue (401) → rediriger vers /login
```

---

### 2.4 Logout

```
POST /sni/api/v1/auth/logout
Authorization: Bearer {accessToken}
```

**Réponse (200) :**
```json
{ "success": true, "message": "Logged out successfully" }
```

Révoque le refresh token côté serveur. Supprimer les deux tokens côté client.

---

## 3. Codes d'erreur spécifiques au portal

| Code HTTP | `errorCode` | Cause | Action frontend |
|-----------|-------------|-------|-----------------|
| 401 | `UNAUTHORIZED` | Token absent ou expiré | Rafraîchir le token ou rediriger /login |
| 401 | `REFRESH_TOKEN_REVOKED` | Refresh token révoqué (usage unique violé) | Déconnexion forcée → /login |
| 403 | `ROLE_MEMBER_REQUIRED` | Utilisateur connecté mais pas membre | Afficher "Accès non autorisé" |
| 403 | `PORTAL_ACCESS_DISABLED` | `portalAccess = false` (désactivé par admin) | Page "Votre accès portal a été suspendu" |
| 403 | `KYC_REQUIRED` | KYC non soumis, grace period expirée | Rediriger vers /documents/kyc |
| 403 | `ACCOUNT_NOT_VERIFIED` | Email non vérifié | Page de vérification OTT |
| 429 | `RATE_LIMIT_EXCEEDED` | Trop de requêtes | Attendre + retry avec backoff |

---

## 4. Stockage des tokens

| Token | Stockage recommandé | Durée |
|-------|---------------------|-------|
| Access token | **Memory** (variable JS, pas localStorage) | 15 min |
| Refresh token | **HttpOnly cookie** (si même domaine) ou **localStorage** (si mobile/SPA cross-origin) | 7 jours |

> Ne jamais stocker l'access token dans localStorage — XSS lisible.  
> Le refresh token en localStorage est acceptable sur mobile natif ou SPA sans SSR.

---

## 5. Onboarding progressif — Grace period 7 jours

### 5.1 Principe

Après la vérification email, le membre a **accès complet au portal pendant 7 jours**.  
Après 7 jours, les actions sensibles sont bloquées si le KYC n'a pas été soumis.

```
J0   Email vérifié → accès complet
J1-7 Accès complet + bannière de rappel KYC (affichage frontend)
J8+  Actions bloquées si KYC non soumis (erreur 403 KYC_REQUIRED)
     Actions toujours autorisées : consulter profil, uploader KYC, support
```

**Actions bloquées après J7 sans KYC :**
- Créer une réservation
- Effectuer un paiement
- Recharger le wallet
- Signer un contrat

**Actions toujours autorisées (grace period expirée ou non) :**
- Consulter profil, réservations passées, factures
- Uploader des documents KYC
- Contacter le support
- Voir les espaces disponibles

### 5.2 Données renvoyées par le serveur

Toutes les réponses `/client/**` incluent dans les headers (ou dans le body de `/client/me`) :

```
X-Kyc-Grace-Period-Ends: 2026-06-21T09:00:00Z   (si grace period active)
X-Kyc-Status: IN_PROGRESS                        (statut KYC)
```

Ou via `GET /client/me/onboarding-status` :

```json
{
  "emailVerified": true,
  "kycStatus": "NOT_STARTED",
  "kycCompletionPercent": 0,
  "gracePeriodEndsAt": "2026-06-21T09:00:00Z",
  "gracePeriodActive": true,
  "gracePeriodDaysRemaining": 6,
  "portalFullyActive": false,
  "nextStep": "START_KYC",
  "restrictedActions": []
}
```

Après J7 sans KYC soumis :
```json
{
  "gracePeriodActive": false,
  "restrictedActions": ["BOOKING_CREATE", "PAYMENT", "WALLET_TOPUP", "CONTRACT_SIGN"]
}
```

### 5.3 Bannière recommandée (J1–J7)

```
[⚠] Complétez votre vérification d'identité
Vous avez encore {gracePeriodDaysRemaining} jour(s) pour soumettre vos documents
avant que certaines fonctionnalités soient restreintes.
[Compléter le KYC →]   [Me rappeler plus tard]
```

---

## 6. Flux d'inscription (Self-Registration)

### Étape 1 — Formulaire d'inscription

```
POST /sni/api/v1/auth/register
Content-Type: application/json

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

**Réponse succès (201) :**
```json
{
  "success": true,
  "data": {
    "verificationToken": "eyJ...",
    "message": "Un code de vérification a été envoyé à jean@example.com"
  }
}
```

> Pas d'`accessToken` à cette étape. Le membre doit d'abord vérifier son email.

**Erreurs :**
```json
{ "errorCode": "EMAIL_ALREADY_EXISTS" }
{ "errorCode": "PHONE_ALREADY_EXISTS" }
{ "errorCode": "INVALID_EMAIL_FORMAT" }
```

### Étape 2 — Vérification email (OTT)

```
POST /sni/api/v1/auth/ott/validate
Content-Type: application/json

{
  "ottToken": "482931",
  "verificationToken": "eyJ..."
}
```

**Réponse succès (200) :**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "eyJ...",
    "expiresIn": 900
  }
}
```

Après ce point, le membre est authentifié. Rediriger vers le dashboard portal.

**Erreurs :**
```json
{ "errorCode": "OTT_INVALID", "errorDescription": "Code incorrect" }
{ "errorCode": "OTT_EXPIRED", "errorDescription": "Code expiré (15 min)" }
```

### Étape 3 — Renvoyer le code

```
POST /sni/api/v1/auth/ott/request
Content-Type: application/json

{
  "email": "jean@example.com"
}
```

**Réponse (200) :**
```json
{
  "data": {
    "verificationToken": "eyJ...",
    "message": "Nouveau code envoyé"
  }
}
```

> Limite : 3 demandes par 10 minutes par email (429 si dépassé).

---

## 7. Flux connexion OTT (sans mot de passe)

```
Étape 1 — Demander un code :
POST /sni/api/v1/auth/ott/request
{ "email": "jean@example.com" }
→ Reçoit { verificationToken }
→ Email avec code 6 chiffres envoyé au membre

Étape 2 — Valider le code :
POST /sni/api/v1/auth/ott/validate
{ "ottToken": "123456", "verificationToken": "eyJ..." }
→ Reçoit { accessToken, refreshToken }
```

Même résultat que le login classique. Utile comme "magic link" pour les membres qui ont oublié leur mot de passe.

---

## 8. Réinitialisation mot de passe

```
Étape 1 :
POST /sni/api/v1/auth/password-reset/request
{ "email": "jean@example.com" }
→ Email avec lien de réinitialisation (valide 24h)
→ Response: { "success": true } (toujours, même si email inconnu — sécurité)

Étape 2 — Formulaire de nouveau mot de passe :
POST /sni/api/v1/auth/password-reset/confirm
{
  "token": "uuid-depuis-le-lien-email",
  "newPassword": "NouveauMotDePasse456!"
}
→ { "success": true }
→ Tous les appareils connectés sont déconnectés (refresh tokens révoqués)
```

**Erreurs :**
```json
{ "errorCode": "PASSWORD_RESET_TOKEN_INVALID" }
{ "errorCode": "PASSWORD_RESET_TOKEN_EXPIRED" }
```

---

## 9. Utilisateur courant

```
GET /sni/api/v1/auth/me
Authorization: Bearer {accessToken}
```

**Réponse (200) :**
```json
{
  "data": {
    "userId": "USR-001",
    "email": "jean@example.com",
    "accountEnabled": true,
    "authorities": ["ROLE_MEMBER"]
  }
}
```

Utile au démarrage de l'application pour vérifier si le token est encore valide et charger les infos de base.

---

## 10. Checklist d'intégration frontend

### Auth
- [ ] Intercepteur HTTP global qui ajoute `Authorization: Bearer {token}` sur toutes les requêtes `/client/**`
- [ ] Intercepteur qui détecte `401` et tente un refresh automatique
- [ ] Si refresh échoue → flush tokens + redirect `/login`
- [ ] Timer de refresh : déclencher à `(expiresIn - 60)` secondes

### Inscription
- [ ] Formulaire multi-étapes : infos personnelles → vérification OTT → dashboard
- [ ] Stocker `verificationToken` en mémoire entre étapes 1 et 2
- [ ] Bouton "Renvoyer le code" avec cooldown 60 s visible

### Onboarding
- [ ] Appeler `GET /client/me/onboarding-status` au premier login
- [ ] Si `gracePeriodActive: true` → afficher bannière KYC avec compte à rebours
- [ ] Si `gracePeriodActive: false` et KYC non soumis → bloquer visuellement les actions restreintes + afficher message explicatif

### Gestion d'erreurs
- [ ] Handler global pour `403 PORTAL_ACCESS_DISABLED` → page d'information + contact support
- [ ] Handler pour `403 KYC_REQUIRED` → redirect vers espace KYC avec message contextualisé
- [ ] Handler pour `429` → toast "Trop de tentatives, réessayez dans X minutes"

---

## 11. Environnements

| Variable | Dev | Prod |
|----------|-----|------|
| Base URL | `http://localhost:8080/sni/api/v1` | `https://api.bokati.com/sni/api/v1` |
| Access token TTL | 900 s | 900 s |
| Refresh token TTL | 604800 s (7 j) | 604800 s (7 j) |
| Grace period KYC | 7 jours | 7 jours |
