# Frontend - Authentification et gestion des tokens

## Base API

Toutes les routes ci-dessous sont prefixees par :

```text
/sni/api/v1
```

Exemple :

```text
POST /sni/api/v1/auth/login
```

## Resume

Le backend utilise une authentification JWT sans session HTTP serveur cote navigateur.

- Le frontend recoit un `accessToken` et un `refreshToken`.
- Le `accessToken` sert a appeler les routes protegees avec `Authorization: Bearer <accessToken>`.
- Le `refreshToken` sert uniquement a obtenir une nouvelle paire de tokens via `/auth/refresh`.
- Le refresh token est rotate a chaque refresh : l'ancien refresh token devient invalide.
- Le frontend doit toujours remplacer les deux tokens apres un refresh reussi.
- Le backend ne pose pas de cookie auth. Le frontend gere lui-meme le stockage.

## Routes publiques

Ces routes ne demandent pas de token :

```http
POST /sni/api/v1/auth/login
POST /sni/api/v1/auth/refresh
POST /sni/api/v1/auth/ott/request
POST /sni/api/v1/auth/ott/validate
POST /sni/api/v1/auth/password-reset/request
POST /sni/api/v1/auth/password-reset/confirm
```

Autres routes publiques techniques :

```http
POST /sni/api/v1/payments/mobile-money/pawapay/callback
POST /sni/api/v1/payments/mobile-money/pawaypay/callback
POST /sni/api/v1/payments/mobile-money/pawapay/refund-callback
POST /sni/api/v1/payments/mobile-money/pawaypay/refund-callback

GET /verify/**
GET /actuator/health
GET /actuator/info
```

Toutes les autres routes sous `/sni/api/v1/**` demandent une authentification et une permission admin.

## Methode 1 - Connexion email + mot de passe

### Requete

```http
POST /sni/api/v1/auth/login
Content-Type: application/json
```

```json
{
  "email": "admin@bokati.com",
  "password": "********"
}
```

### Reponse 200

```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ..."
}
```

### Cote frontend

Apres un login reussi :

- Stocker `accessToken`.
- Stocker `refreshToken`.
- Charger l'utilisateur courant avec `GET /auth/me`.
- Rediriger vers l'application protegee.

## Methode 2 - Connexion OTT par email

OTT signifie `One-Time Token`. Ce flow permet une connexion avec un code recu par email.

### Etape 1 - Demander le code

```http
POST /sni/api/v1/auth/ott/request
Content-Type: application/json
```

```json
{
  "email": "admin@bokati.com"
}
```

### Reponse 200

```json
{
  "verificationToken": "eyJ...",
  "message": "OTP sent to email"
}
```

Le backend envoie le code OTT a 6 chiffres par email.

Le frontend doit conserver temporairement le `verificationToken`, par exemple en memoire ou en `sessionStorage`, jusqu'a la validation du code.

### Etape 2 - Valider le code

```http
POST /sni/api/v1/auth/ott/validate
Content-Type: application/json
```

```json
{
  "ottToken": "123456",
  "verificationToken": "eyJ..."
}
```

### Reponse 200

```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ..."
}
```

Apres cette reponse, le comportement frontend est le meme que pour le login classique.

## Methode 3 - Mot de passe oublie

Ce flow ne connecte pas directement l'utilisateur. Il sert uniquement a changer le mot de passe.

### Etape 1 - Demander le reset

```http
POST /sni/api/v1/auth/password-reset/request
Content-Type: application/json
```

```json
{
  "email": "admin@bokati.com"
}
```

### Reponse

```http
204 No Content
```

Le backend envoie un lien ou token de reset par email.

### Etape 2 - Confirmer le nouveau mot de passe

```http
POST /sni/api/v1/auth/password-reset/confirm
Content-Type: application/json
```

```json
{
  "token": "reset-token-from-email",
  "newPassword": "NouveauMotDePasseFort"
}
```

### Reponse

```http
204 No Content
```

Apres confirmation, le frontend doit rediriger vers l'ecran de login.

## Utilisation du access token

Pour toutes les routes protegees :

```http
Authorization: Bearer <accessToken>
```

Exemple :

```http
GET /sni/api/v1/auth/me
Authorization: Bearer eyJ...
```

Ne pas envoyer le refresh token pour les appels metier. Le refresh token doit rester reserve au refresh et au logout.

## Utilisateur courant

### Requete

```http
GET /sni/api/v1/auth/me
Authorization: Bearer <accessToken>
```

### Reponse 200

```json
{
  "userId": "USR-0001",
  "email": "admin@bokati.com",
  "accountEnabled": true,
  "authorities": [
    "ROLE_ADMIN",
    "REPORT:VIEW",
    "BOOKING:READ"
  ]
}
```

Le frontend peut utiliser `authorities` pour masquer ou afficher les menus/actions.

Regle simple :

- Une route protegee demande un role admin reconnu.
- Une action specifique demande souvent une permission comme `REPORT:VIEW`, `BOOKING:CREATE`, `PAYMENT:PROCESS`.
- Le backend accepte aussi le format avec underscore pour certaines permissions, par exemple `REPORT_VIEW`.

## Refresh des tokens

### Quand refresh

Le frontend doit appeler `/auth/refresh` quand :

- Une requete protegee retourne `401`.
- Ou juste avant expiration si le frontend decode le JWT et surveille `exp`.

### Requete

```http
POST /sni/api/v1/auth/refresh
Content-Type: application/json
```

```json
{
  "refreshToken": "eyJ..."
}
```

Ne pas mettre obligatoirement `Authorization` sur cette requete. Le refresh token est envoye dans le body.

### Reponse 200

```json
{
  "accessToken": "new-eyJ...",
  "refreshToken": "new-eyJ..."
}
```

Important :

- Le backend rotate le refresh token.
- L'ancien refresh token est revoque.
- Le frontend doit remplacer `accessToken` et `refreshToken` par les nouvelles valeurs.
- Si le frontend garde l'ancien refresh token, le refresh suivant echouera.

### Echec de refresh

Si `/auth/refresh` retourne `401`, `403`, `404` ou une erreur indiquant un refresh token invalide/expire :

- Supprimer les tokens locaux.
- Rediriger vers `/login`.
- Ne pas boucler indefiniment sur `/auth/refresh`.

## Logout

### Requete recommandee avec le backend actuel

```http
POST /sni/api/v1/auth/logout
Authorization: Bearer <refreshToken>
```

### Reponse

```http
204 No Content
```

Avec l'implementation actuelle, le logout lit uniquement le token dans le header `Authorization`.

Pour un logout complet, envoyer le `refreshToken` dans le header. Cela permet au backend de :

- Revoquer le refresh token stocke en base.
- Invalider la session associee.

Apres la reponse, le frontend doit toujours supprimer localement `accessToken` et `refreshToken`, meme si la requete logout echoue.

## Durees de vie des tokens

Valeurs par defaut dans le backend si aucune configuration externe ne les remplace :

```text
accessToken: 90000000 ms environ 25h
refreshToken: 604800000 ms 7 jours
verificationToken OTT: 900000 ms 15 min
code OTT: 900000 ms 15 min
password reset token: 900000 ms 15 min
```

Note backend : le code contient un commentaire indiquant que l'access token devrait revenir a `900000 ms` soit 15 minutes. Le frontend ne doit donc pas coder en dur une duree fixe. Il vaut mieux decoder le claim JWT `exp`.

## Stockage frontend recommande

Option pragmatique pour un backoffice web :

- Stocker `accessToken` en memoire si possible.
- Stocker `refreshToken` dans un stockage plus persistant seulement si l'experience utilisateur exige de rester connecte apres reload.
- Si `localStorage` est utilise, assumer le risque XSS et renforcer strictement la protection frontend.
- Ne jamais logger les tokens.
- Ne jamais mettre les tokens dans l'URL.
- Ne jamais envoyer le refresh token aux endpoints metier.

Option simple si l'app doit survivre au refresh navigateur :

```text
localStorage["accessToken"]
localStorage["refreshToken"]
```

Option plus stricte :

```text
accessToken en memoire
refreshToken en sessionStorage
```

Le backend actuel ne fournit pas de cookie `HttpOnly`, donc le frontend doit choisir explicitement sa strategie.

## Intercepteur HTTP recommande

### Principe

1. Ajouter `Authorization: Bearer <accessToken>` sur toutes les requetes protegees.
2. Ne pas ajouter `Authorization` sur `/auth/login`, `/auth/refresh`, `/auth/ott/**`, `/auth/password-reset/**`.
3. Si une requete protegee retourne `401`, lancer un seul refresh global.
4. Pendant le refresh, mettre les autres requetes en attente.
5. Apres refresh reussi, rejouer les requetes avec le nouveau `accessToken`.
6. Si refresh echoue, nettoyer les tokens et rediriger vers login.

### Pseudo-code TypeScript

```ts
let accessToken: string | null = tokenStore.getAccessToken();
let refreshToken: string | null = tokenStore.getRefreshToken();
let refreshPromise: Promise<void> | null = null;

function isPublicAuthUrl(url: string): boolean {
  return url.includes("/auth/login")
    || url.includes("/auth/refresh")
    || url.includes("/auth/ott/")
    || url.includes("/auth/password-reset/");
}

async function refreshTokens(api: ApiClient): Promise<void> {
  if (!refreshToken) {
    throw new Error("Missing refresh token");
  }

  const response = await api.post("/sni/api/v1/auth/refresh", {
    refreshToken
  }, {
    skipAuth: true
  });

  accessToken = response.accessToken;
  refreshToken = response.refreshToken;

  tokenStore.setAccessToken(accessToken);
  tokenStore.setRefreshToken(refreshToken);
}

async function request(config: RequestConfig): Promise<Response> {
  if (!config.skipAuth && !isPublicAuthUrl(config.url) && accessToken) {
    config.headers = {
      ...config.headers,
      Authorization: `Bearer ${accessToken}`
    };
  }

  const response = await http.request(config);

  if (response.status !== 401 || config._retry || isPublicAuthUrl(config.url)) {
    return response;
  }

  config._retry = true;

  try {
    refreshPromise ??= refreshTokens(api).finally(() => {
      refreshPromise = null;
    });

    await refreshPromise;

    return request(config);
  } catch (error) {
    tokenStore.clear();
    router.navigate("/login");
    throw error;
  }
}
```

## Decodage minimal du JWT cote frontend

Le backend met ces claims dans le `accessToken` :

```json
{
  "sub": "admin@bokati.com",
  "sessionId": "uuid",
  "type": "access",
  "roles": ["ROLE_ADMIN", "REPORT:VIEW"],
  "iat": 1760000000,
  "exp": 1760000900
}
```

Le refresh token contient au minimum :

```json
{
  "sub": "admin@bokati.com",
  "sessionId": "uuid",
  "type": "refresh",
  "iat": 1760000000,
  "exp": 1760604800
}
```

Le frontend peut lire `exp` pour anticiper le refresh, mais il ne doit pas faire confiance aux roles decodees pour la securite. Les permissions reelles sont verifiees par le backend.

## Erreurs courantes

### 401 Unauthorized

Causes probables :

- Access token absent.
- Access token expire.
- JWT invalide.
- Session revoquee ou expiree.

Action frontend :

- Tenter un refresh une seule fois si un refresh token existe.
- Sinon rediriger vers login.

### 403 Forbidden

Causes probables :

- Utilisateur connecte mais role non autorise.
- Permission manquante.

Action frontend :

- Afficher un message "Acces refuse".
- Ne pas tenter de refresh automatiquement.

### Refresh token invalide ou expire

Action frontend :

- Nettoyer les tokens.
- Rediriger vers login.

## Permissions utiles cote UI

Exemples de permissions frequentes :

```text
REPORT:VIEW
REPORT:EXPORT
BOOKING:READ
BOOKING:CREATE
BOOKING:UPDATE
BOOKING:CANCEL
BOOKING:CHECKIN
BILLING:READ
BILLING:CREATE
BILLING:UPDATE
BILLING:CANCEL
PAYMENT:READ
PAYMENT:PROCESS
PAYMENT:REFUND
CASH:READ
CASH:OPEN_SESSION
CASH:CLOSE_SESSION
CLIENT:READ
CLIENT:CREATE
CLIENT:UPDATE
KYC:READ
KYC:APPROVE
KYC:REJECT
DOCUMENT:READ
DOCUMENT:UPLOAD
DOCUMENT:APPROVE
SUPPORT:READ
SUPPORT:WRITE
SYSTEM:USERS
SYSTEM:ROLES
SYSTEM:PERMISSIONS
SYSTEM:AUDIT
SYSTEM:SETTINGS
```

## Checklist frontend

- Implementer `login(email, password)`.
- Implementer `requestOtt(email)` puis `validateOtt(ottToken, verificationToken)`.
- Implementer `requestPasswordReset(email)` puis `confirmPasswordReset(token, newPassword)`.
- Ajouter `Authorization: Bearer <accessToken>` sur les routes protegees.
- Implementer un refresh global unique pour eviter plusieurs refresh simultanes.
- Remplacer les deux tokens apres chaque refresh.
- Nettoyer les tokens si refresh echoue.
- Appeler `/auth/me` apres login et apres reload si des tokens existent.
- Pour logout, appeler `/auth/logout` avec `Authorization: Bearer <refreshToken>`, puis supprimer les tokens locaux.
