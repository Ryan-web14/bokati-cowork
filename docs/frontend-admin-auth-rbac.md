# Frontend admin - Authentification et permissions

## Changement principal

Les URLs admin existantes ne changent pas. Les routes comme `/sni/api/v1/bookings`, `/sni/api/v1/billing`, `/sni/api/v1/payments`, `/sni/api/v1/customers`, etc. restent identiques.

La difference est que le frontend admin doit maintenant envoyer un token JWT sur chaque requete protegee.

```http
Authorization: Bearer <accessToken>
```

## Routes publiques

Ces routes restent accessibles sans token :

```http
POST /sni/api/v1/auth/login
POST /sni/api/v1/auth/refresh
POST /sni/api/v1/auth/ott/request
POST /sni/api/v1/auth/ott/validate
POST /sni/api/v1/auth/password-reset/request
POST /sni/api/v1/auth/password-reset/confirm

POST /sni/api/v1/payments/mobile-money/pawapay/callback
POST /sni/api/v1/payments/mobile-money/pawaypay/callback
POST /sni/api/v1/payments/mobile-money/pawapay/refund-callback
POST /sni/api/v1/payments/mobile-money/pawaypay/refund-callback

GET /verify/doc/{documentNumber}
GET /verify/receipt/{receiptNumber}
```

Tout le reste sous `/sni/api/v1/**` est considere admin pour cette phase.

## Login

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

Reponse :

```json
{
  "accessToken": "...",
  "refreshToken": "..."
}
```

Le frontend doit stocker le `accessToken` pour les appels API admin et utiliser le `refreshToken` quand l'access token expire.

## Refresh token

```http
POST /sni/api/v1/auth/refresh
Content-Type: application/json
```

```json
{
  "refreshToken": "..."
}
```

## Utilisateur courant

```http
GET /sni/api/v1/auth/me
Authorization: Bearer <accessToken>
```

La reponse contient les autorites de l'utilisateur. Le frontend peut s'en servir pour afficher ou masquer les actions.

Exemples d'autorites :

```json
[
  "ROLE_ADMIN",
  "BILLING:READ",
  "BILLING_READ",
  "PAYMENT:PROCESS",
  "PAYMENT_PROCESS"
]
```

Preferer les permissions au format `MODULE:ACTION` cote UI.

## Permissions principales

Gestion systeme :

```text
SYSTEM:USERS
SYSTEM:ROLES
SYSTEM:PERMISSIONS
SYSTEM:AUDIT
SYSTEM:SETTINGS
```

Operations :

```text
BILLING:READ
BILLING:CREATE
BILLING:UPDATE
BILLING:SEND
BILLING:CANCEL
PAYMENT:READ
PAYMENT:PROCESS
PAYMENT:REFUND
CASH:READ
CASH:OPEN_SESSION
CASH:CLOSE_SESSION
CASH:ADJUST
BOOKING:READ
BOOKING:CREATE
BOOKING:UPDATE
BOOKING:CANCEL
BOOKING:CHECKIN
CLIENT:READ
CLIENT:CREATE
CLIENT:UPDATE
CLIENT:DELETE
KYC:READ
KYC:APPROVE
KYC:REJECT
DOCUMENT:READ
DOCUMENT:UPLOAD
DOCUMENT:APPROVE
REPORT:VIEW
REPORT:EXPORT
VISITOR:READ
VISITOR:WRITE
VISITOR:CHECKIN
SUPPORT:READ
SUPPORT:WRITE
SUPPORT:ASSIGN
SUPPORT:METRICS
```

## Nouveau role composite

Le role `OPERATIONS_AGENT` regroupe finance, caisse, staff et support, mais sans droits sensibles :

Il n'a pas :

```text
PAYMENT:REFUND
RESOURCE:PRICE
BILLING:UPDATE
```

Le frontend doit donc masquer les boutons de remboursement, de modification de prix et de modification de montant de facture si ces permissions ne sont pas presentes.

## Module frontend - Utilisateurs, roles et permissions

### Charger les utilisateurs

```http
GET /sni/api/v1/admin/users?page=0&size=20
Authorization: Bearer <accessToken>
```

Filtres disponibles :

```text
email
enabled
locked
deleted
```

Recherche rapide par email ou identifiant utilisateur :

```http
GET /sni/api/v1/admin/users/search/by-name?query=admin
Authorization: Bearer <accessToken>
```

Cette recherche utilise le moteur PostgreSQL `pg_trgm`.

### Creer un utilisateur

```http
POST /sni/api/v1/admin/users
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "email": "agent@elleaose.com",
  "generatePassword": true,
  "roleNames": ["OPERATIONS_AGENT"]
}
```

Si `generatePassword` vaut `false`, envoyer aussi `password`.

```json
{
  "email": "agent@elleaose.com",
  "password": "MotDePasseTemporaire",
  "generatePassword": false,
  "roleNames": ["STAFF", "SUPPORT"]
}
```

Reponse :

```json
{
  "id": 123,
  "userId": "USR-020526-ABCD1234",
  "email": "agent@elleaose.com",
  "accountEnabled": true,
  "accountLocked": false,
  "failedLoginAttempts": 0,
  "roleNames": ["OPERATIONS_AGENT"],
  "generatedPassword": "********"
}
```

Le backend envoie automatiquement un email a l'utilisateur cree. Cet email contient :

```text
email de connexion
roles assignes
url d'acces: https://admin.elleaose.com
```

Le mot de passe n'est pas envoye par email.

### Gerer les roles d'un utilisateur

Voir les roles d'un utilisateur :

```http
GET /sni/api/v1/admin/users/{userId}/roles
Authorization: Bearer <accessToken>
```

Assigner un role :

```http
POST /sni/api/v1/admin/users/{userId}/roles
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "roleName": "OPERATIONS_AGENT"
}
```

Retirer un role :

```http
DELETE /sni/api/v1/admin/users/{userId}/roles/{roleId}
Authorization: Bearer <accessToken>
```

### Charger et rechercher les roles

```http
GET /sni/api/v1/admin/roles
Authorization: Bearer <accessToken>
```

Recherche par nom, libelle ou description :

```http
GET /sni/api/v1/admin/roles/search/by-name?query=finance
Authorization: Bearer <accessToken>
```

### Charger et rechercher les permissions

```http
GET /sni/api/v1/admin/permissions?activeOnly=true&size=200
Authorization: Bearer <accessToken>
```

Recherche par nom, libelle ou `MODULE:ACTION` :

```http
GET /sni/api/v1/admin/permissions/search/by-name?query=BILLING
Authorization: Bearer <accessToken>
```

Chaque permission contient maintenant :

```json
{
  "id": 10,
  "name": "BILLING_READ",
  "displayName": "Read billing documents",
  "module": "BILLING",
  "action": "READ",
  "fullPermissionName": "BILLING:READ",
  "isActive": true
}
```

### Assigner les permissions d'un role

Par IDs :

```http
PATCH /sni/api/v1/admin/roles/{roleId}/permissions
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "permissionIds": [10, 11, 12]
}
```

Par noms, recommande pour le frontend :

```http
PATCH /sni/api/v1/admin/roles/{roleId}/permission-names
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "permissionNames": [
    "BILLING:READ",
    "PAYMENT:PROCESS",
    "SUPPORT:READ"
  ]
}
```

Le backend accepte aussi le format underscore :

```json
{
  "permissionNames": [
    "BILLING_READ",
    "PAYMENT_PROCESS",
    "SUPPORT_READ"
  ]
}
```

### Permissions requises pour l'ecran

Pour ouvrir l'ecran gestion des acces :

```text
SYSTEM:USERS
SYSTEM:ROLES
SYSTEM:PERMISSIONS
```

Le frontend peut masquer les onglets selon ces permissions.

## Gestion des erreurs

`401 Unauthorized` :

Le token est absent, expire, invalide ou la session a ete revoquee. Le frontend doit rediriger vers le login ou tenter un refresh si possible.

`403 Forbidden` :

L'utilisateur est authentifie mais n'a pas la permission necessaire. Le frontend doit afficher un message du type : "Vous n'avez pas les droits pour effectuer cette action."

`429 Too Many Requests` :

Trop de requetes dans une courte periode. Cela concerne surtout login, OTP, reset password, callbacks publics et appels admin intensifs.

## Rate limiting

Valeurs backend par defaut :

```text
Login: 5 requetes / minute / IP
OTP et reset password: 3 requetes / 10 minutes / IP
Callbacks PawaPay: 120 requetes / minute / IP
Admin API: 600 requetes / minute / IP
```

## Consequence pour le frontend

A partir de cette migration, tout appel admin doit passer par un client HTTP qui ajoute automatiquement :

```http
Authorization: Bearer <accessToken>
```

Les ecrans doivent aussi exploiter `/sni/api/v1/auth/me` pour adapter l'affichage selon les permissions recues.
