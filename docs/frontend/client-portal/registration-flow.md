# Client Portal — Self-Registration, Authentication & Onboarding Flow

> **Audience** : Développeur frontend  
> **Base URL** : `https://api.elleaose.com/sni/api/v1`  
> **Portal URL** : `https://espace.elleaose.com`

---

## Table des matières

1. [Vue d'ensemble du flux](#1-vue-densemble-du-flux)
2. [Inscription (Self-Registration)](#2-inscription-self-registration)
3. [Vérification email (OTT)](#3-vérification-email-ott)
4. [Connexion — 3 méthodes](#4-connexion--3-méthodes)
5. [Onboarding Status](#5-onboarding-status)
6. [KYC — Soumission des documents](#6-kyc--soumission-des-documents)
7. [Profil client](#7-profil-client)
8. [Mot de passe — Reset & Change](#8-mot-de-passe--reset--change)
9. [Déverrouillage de compte](#9-déverrouillage-de-compte)
10. [Gestion des sessions](#10-gestion-des-sessions)
11. [Diagramme de séquence complet](#11-diagramme-de-séquence-complet)

---

## 1. Vue d'ensemble du flux

```
┌─────────────┐     ┌──────────────┐     ┌───────────────┐     ┌─────────────┐
│ Inscription │ ──▶ │ Vérification │ ──▶ │  Onboarding   │ ──▶ │   Accès     │
│   (email +  │     │  email (OTT) │     │  KYC + grace  │     │   complet   │
│  password)  │     │  6 chiffres  │     │   period 7j   │     │   portail   │
└─────────────┘     └──────────────┘     └───────────────┘     └─────────────┘
```

**Statuts du membre** : `PENDING` → `ACTIVE` (après vérification email)

**Accès portail** :
- Après vérification email : accès limité (grace period 7 jours)
- Après approbation KYC : accès complet (aucune restriction)
- Si grace period expirée sans KYC approuvé : actions restreintes

---

## 2. Inscription (Self-Registration)

### `POST /auth/register`

Crée un compte membre et envoie un code de vérification par email.

**Request Body** :
```json
{
  "firstname": "Ryan",
  "lastname": "Otchambe",
  "email": "ryan@example.com",
  "password": "MonMotDePasse123",
  "phone": "061135836",
  "whatsappPhone": "061135836",
  "birthDate": "1995-03-15",
  "gender": "MALE",
  "preferredCommunicationChannel": "WHATSAPP",
  "address": {
    "streetNumber": "12",
    "streetName": "Avenue de la Paix",
    "district": "Mpita",
    "city": "Pointe-Noire",
    "countryCode": "CG"
  }
}
```

> **Note** : Le type de client est automatiquement `PERSON` pour l'auto-inscription. Il n'y a pas de champ `customerType` ou `companyName` dans ce formulaire.

| Champ | Requis | Validation |
|-------|--------|------------|
| `firstname` | Oui | max 350 chars |
| `lastname` | Oui | max 350 chars |
| `email` | Oui | format email valide, unique |
| `password` | Oui | 8–45 caractères |
| `phone` | Oui | format `+?[0-9]{8,15}` |
| `whatsappPhone` | Non | même format que phone |
| `birthDate` | Non | format `YYYY-MM-DD` |
| `gender` | Non | `MALE`, `FEMALE`, `OTHER` |
| `preferredCommunicationChannel` | Non | `EMAIL`, `WHATSAPP`, `SMS`, `PHONE` |
| `address` | Non | objet adresse (voir ci-dessous) |

### Champs de l'objet `address`

| Champ | Requis | Description |
|-------|--------|-------------|
| `streetNumber` | Non | Numéro de rue (ex: "12") |
| `streetName` | Non | Nom de rue (ex: "Avenue de la Paix") |
| `district` | Oui* | Quartier / arrondissement (ex: "Mpita") |
| `city` | Oui* | Ville (ex: "Pointe-Noire") |
| `countryCode` | Oui* | Code ISO 2 lettres du pays (ex: "CG") |

\* Requis si l'objet `address` est envoyé.

### Récupérer la liste des pays (pour le select countryCode)

#### `GET /countries`

Retourne la liste de tous les pays disponibles. Utiliser ce endpoint pour peupler le sélecteur de pays dans le formulaire d'inscription.

```json
[
  {
    "id": 1,
    "countryCode": "CG",
    "name": "Congo",
    "phoneCode": "+242",
    "isOhadaMember": true
  },
  {
    "id": 2,
    "countryCode": "CD",
    "name": "RD Congo",
    "phoneCode": "+243",
    "isOhadaMember": false
  }
]
```

**Usage frontend** : Appeler ce endpoint au chargement du formulaire d'inscription pour remplir le `<select>` du pays. Stocker le `countryCode` sélectionné dans `address.countryCode`.

**Response** `201 Created` :
```json
{
  "verificationToken": "eyJhbGciOiJIUzI1NiJ9...",
  "message": "Registration successful. Please verify your email with the code sent to ryan@example.com"
}
```

**Ce qui se passe côté backend** :
1. Création du Customer + Member (statut `PENDING`)
2. Création du User (compte verrouillé = email non vérifié)
3. Génération OTT 6 chiffres (expire en 15 min)
4. Envoi email avec le code
5. Initialisation du dossier KYC (vide)

**Erreurs possibles** :
| Code | Message | Cause |
|------|---------|-------|
| 400 | "A valid email is required" | Email invalide |
| 400 | "A member with this email already exists" | Email déjà utilisé |
| 400 | "A valid phone number is required" | Téléphone invalide |

---

## 3. Vérification email (OTT)

### `POST /auth/ott/validate`

Valide le code 6 chiffres envoyé par email. Active le compte et retourne les tokens JWT.

**Request Body** :
```json
{
  "ottToken": "482916",
  "verificationToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

| Champ | Description |
|-------|-------------|
| `ottToken` | Code 6 chiffres reçu par email |
| `verificationToken` | JWT reçu dans la réponse d'inscription |

**Response** `200 OK` :
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Ce qui se passe** :
1. Vérification du code OTT (validité + expiration 15 min)
2. Déverrouillage du compte (email vérifié ✓)
3. Attribution du rôle `MEMBER`
4. Membre passe en statut `ACTIVE`
5. `portalActivatedAt` = maintenant
6. Grace period KYC initialisée (7 jours)
7. Création de session + retour tokens

**⚠️ Stocker les tokens** :
- `accessToken` : envoyé dans le header `Authorization: Bearer {token}` pour chaque requête authentifiée
- `refreshToken` : stocké de façon sécurisée pour renouveler l'access token

### `POST /auth/email/verify/resend`

Renvoie un nouveau code si le précédent a expiré.

**Request Body** :
```json
{
  "email": "ryan@example.com"
}
```

**Response** `200 OK` :
```json
{
  "verificationToken": "eyJhbGciOiJIUzI1NiJ9...",
  "message": "Verification code resent to ryan@example.com"
}
```

---

## 4. Connexion — 3 méthodes

### Méthode 1 : Login classique (email + mot de passe)

#### `POST /auth/login`

**Request Body** :
```json
{
  "email": "ryan@example.com",
  "password": "MonMotDePasse123"
}
```

**Response** `200 OK` :
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Erreurs** :
| Code | Cause |
|------|-------|
| 401 | Mot de passe incorrect |
| 401 | Compte verrouillé (trop de tentatives) |
| 404 | Email non trouvé |

---

### Méthode 2 : Login sans mot de passe (OTT / Magic Link)

Utile si l'utilisateur a oublié son mot de passe ou préfère un login rapide.

#### Étape 1 : `POST /auth/ott/request`

**Request Body** :
```json
{
  "email": "ryan@example.com"
}
```

**Response** `200 OK` :
```json
{
  "verificationToken": "eyJhbGciOiJIUzI1NiJ9...",
  "message": "OTP sent to email"
}
```

#### Étape 2 : `POST /auth/ott/validate`

Même endpoint que la vérification email (voir section 3).

```json
{
  "ottToken": "738291",
  "verificationToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

→ Retourne `accessToken` + `refreshToken`

---

### Méthode 3 : Refresh token (renouvellement silencieux)

#### `POST /auth/refresh`

Quand l'access token expire (15 min), utiliser le refresh token pour en obtenir un nouveau.

**Request Body** :
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Response** `200 OK` :
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**⚠️ Token rotation** : le refreshToken est à usage unique. Stocker le nouveau à chaque refresh.

**Erreurs** :
| Code | Cause |
|------|-------|
| 401 | Refresh token expiré (7 jours) |
| 401 | Refresh token déjà utilisé (rotation) |

---

### Déconnexion

#### `POST /auth/logout`

**Headers** : `Authorization: Bearer {accessToken}`  
**Response** : `204 No Content`

Révoque le refresh token et invalide la session.

---

## 5. Onboarding Status

### `GET /client/me/onboarding-status`

**Headers** : `Authorization: Bearer {accessToken}`

Endpoint central pour le frontend — détermine l'état d'avancement du membre et les actions à afficher.

**Response** `200 OK` :
```json
{
  "emailVerified": true,
  "profileComplete": true,
  "kycStatus": "NOT_STARTED",
  "kycCompletionPercent": 0,
  "gracePeriodEndsAt": "2026-06-26T10:00:00Z",
  "gracePeriodActive": true,
  "gracePeriodDaysRemaining": 6,
  "portalFullyActive": false,
  "nextStep": "SUBMIT_KYC",
  "restrictedActions": []
}
```

### Mapping des statuts KYC

| Statut backend (KycCaseStatus) | Statut frontend (`kycStatus`) | `kycCompletionPercent` |
|------|------|------|
| `NOT_STARTED` | `NOT_STARTED` | 0% |
| `IN_PROGRESS` | `DRAFT` | 30% |
| `SUBMITTED`, `UNDER_REVIEW` | `SUBMITTED` | 70% |
| `APPROVED` | `APPROVED` | 100% |
| `REJECTED`, `PENDING_CORRECTION`, `RENEWAL_REQUIRED`, `EXPIRED` | `REJECTED` | 40% |

### Valeurs de `nextStep`

| Valeur | Signification | Action frontend |
|--------|---------------|-----------------|
| `VERIFY_EMAIL` | Email pas encore vérifié | Afficher écran de vérification |
| `SUBMIT_KYC` | KYC non démarré | Afficher formulaire upload docs |
| `COMPLETE_KYC` | KYC en cours (brouillon) | Afficher documents manquants |
| `AWAIT_KYC_REVIEW` | KYC soumis, en attente | Afficher message "en cours de vérification" |
| `CORRECT_KYC` | KYC rejeté, corrections demandées | Afficher documents à corriger |
| `SUBMIT_KYC_BEFORE_GRACE_EXPIRES` | Grace period en cours | Afficher urgence + formulaire KYC |
| `COMPLETED` | Tout est validé | Accès complet |
| `CONTACT_SUPPORT` | Grace expirée, KYC non approuvé | Afficher message contacter support |

### `restrictedActions`

Actions bloquées tant que le KYC n'est pas validé ET que la grace period est expirée :
- `BOOKING` — Réservation de salles
- `PAYMENT` — Paiements
- `CONTRACT` — Signature de contrats
- `SUBSCRIPTION` — Souscription d'abonnements

**Logique** :
- Si email non vérifié → TOUT bloqué
- Si KYC non approuvé ET grace period expirée → TOUT bloqué
- Si KYC non approuvé MAIS grace period active → Rien de bloqué (accès temporaire)
- Si KYC approuvé → Rien de bloqué ✓

### Quand appeler cet endpoint

- Au premier rendu après login (dashboard / layout principal)
- Après chaque action KYC (upload, submit)
- Périodiquement (polling toutes les 30s si en attente de review)

---

## 6. KYC — Soumission des documents

Base path : `/client/documents/kyc`

### 6.1 Voir les documents requis

#### `GET /client/documents/kyc/requirements`

```json
[
  {
    "documentTypeCode": "NATIONAL_ID",
    "label": "Carte nationale d'identité",
    "required": true,
    "description": "Recto-verso de votre CNI en cours de validité"
  },
  {
    "documentTypeCode": "PROOF_OF_ADDRESS",
    "label": "Justificatif de domicile",
    "required": true,
    "description": "Facture de moins de 3 mois"
  }
]
```

### 6.2 Uploader un document

#### `POST /client/documents/kyc/documents`

**Content-Type** : `multipart/form-data`

| Param | Type | Requis | Description |
|-------|------|--------|-------------|
| `file` | File | Oui | Le fichier (PDF, JPG, PNG) |
| `documentType` | String | Oui | Code du type (ex: `NATIONAL_ID`) |
| `documentNumber` | String | Non | Numéro du document |
| `issueDate` | Date | Non | Date d'émission (YYYY-MM-DD) |
| `expiryDate` | Date | Non | Date d'expiration (YYYY-MM-DD) |

**Response** `201 Created` :
```json
{
  "id": "DOC-202606-000001",
  "documentType": "NATIONAL_ID",
  "status": "PENDING",
  "uploadedAt": "2026-06-19T10:30:00Z"
}
```

### 6.3 Lister mes documents KYC

#### `GET /client/documents/kyc/documents`

```json
[
  {
    "id": "DOC-202606-000001",
    "documentType": "NATIONAL_ID",
    "status": "PENDING",
    "uploadedAt": "2026-06-19T10:30:00Z"
  },
  {
    "id": "DOC-202606-000002",
    "documentType": "PROOF_OF_ADDRESS",
    "status": "REJECTED",
    "rejectionReason": "Document illisible",
    "uploadedAt": "2026-06-19T10:32:00Z"
  }
]
```

### 6.4 Re-soumettre un document rejeté

#### `PUT /client/documents/kyc/documents/{id}`

Même format multipart que l'upload initial. Remplace le document précédent.

### 6.5 Soumettre le dossier KYC

#### `POST /client/documents/kyc/submit`

Soumet le dossier complet pour examen. Tous les documents requis doivent être présents.

**Response** :
```json
{
  "status": "SUBMITTED",
  "submittedAt": "2026-06-19T11:00:00Z"
}
```

### 6.6 Voir la complétion KYC

#### `GET /client/documents/kyc/completion`

```json
{
  "totalRequired": 3,
  "submitted": 2,
  "approved": 1,
  "rejected": 0,
  "missing": 1,
  "completionPercent": 66
}
```

---

## 7. Profil client

### `GET /client/profile` ou `GET /client/me`

```json
{
  "memberId": "MBR-202606-001",
  "firstname": "Ryan",
  "lastname": "Otchambe",
  "fullName": "Ryan Otchambe",
  "email": "ryan@example.com",
  "phone": "061135836",
  "whatsappPhone": "061135836",
  "status": "ACTIVE",
  "avatarUrl": null,
  "portalActivatedAt": "2026-06-19T10:05:00Z",
  "createdAt": "2026-06-19T10:00:00Z"
}
```

### `PATCH /client/profile`

Mise à jour partielle du profil.

```json
{
  "firstname": "Ryan",
  "lastname": "Otchambe",
  "phone": "061135836",
  "whatsappPhone": "061135836"
}
```

---

## 8. Mot de passe — Reset & Change

### 8.1 Mot de passe oublié (depuis la page login)

#### Étape 1 : `POST /auth/password-reset/request`

```json
{
  "email": "ryan@example.com"
}
```

**Response** : `204 No Content`

→ Un email est envoyé avec un lien contenant un token de reset.

#### Étape 2 : `POST /auth/password-reset/confirm`

```json
{
  "token": "abc123-reset-token-from-email",
  "newPassword": "NouveauMotDePasse456"
}
```

**Response** : `204 No Content`

**Validation** :
- Token non expiré
- Nouveau mot de passe : 8–45 caractères

**Erreurs** :
| Code | Cause |
|------|-------|
| 400 | Token expiré ou invalide |
| 400 | Mot de passe trop court/long |

---

### 8.2 Changer le mot de passe (depuis le portail, connecté)

#### `POST /client/profile/change-password`

**Headers** : `Authorization: Bearer {accessToken}`  
**Response** : `204 No Content`

→ Envoie un email avec un lien de reset (même flux que "mot de passe oublié" mais déclenché depuis le portail).

---

## 9. Déverrouillage de compte

Si un compte est verrouillé après trop de tentatives de login :

### Étape 1 : `POST /auth/unlock-account`

```json
{
  "email": "ryan@example.com"
}
```

**Response** :
```json
{
  "verificationToken": "eyJhbGciOiJIUzI1NiJ9...",
  "message": "Unlock code sent to email"
}
```

### Étape 2 : `POST /auth/unlock-account/confirm`

```json
{
  "ottToken": "482916",
  "verificationToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Response** : `200 OK` — compte déverrouillé, l'utilisateur peut se reconnecter.

---

## 10. Gestion des sessions

### `GET /client/sessions`

Liste toutes les sessions actives du membre.

### `DELETE /client/sessions/{sessionId}`

Révoque une session spécifique (ex: déconnexion d'un autre appareil).

### `DELETE /client/sessions`

Révoque toutes les sessions sauf la courante.

---

## 11. Diagramme de séquence complet

```
┌──────────┐          ┌──────────┐          ┌──────────┐          ┌──────────┐
│ Frontend │          │   API    │          │  Email   │          │  Admin   │
└────┬─────┘          └────┬─────┘          └────┬─────┘          └────┬─────┘
     │                     │                     │                     │
     │ POST /auth/register │                     │                     │
     │────────────────────▶│                     │                     │
     │                     │──── Send OTT ──────▶│                     │
     │◀─── 201 {token} ───│                     │                     │
     │                     │                     │                     │
     │  (User checks email, gets 6-digit code)  │                     │
     │                     │                     │                     │
     │ POST /auth/ott/validate                   │                     │
     │────────────────────▶│                     │                     │
     │◀── 200 {access, refresh} ──│              │                     │
     │                     │                     │                     │
     │ GET /client/me/onboarding-status          │                     │
     │────────────────────▶│                     │                     │
     │◀── {nextStep: SUBMIT_KYC, grace: 7d} ──│ │                     │
     │                     │                     │                     │
     │ GET /client/documents/kyc/requirements    │                     │
     │────────────────────▶│                     │                     │
     │◀── [{NATIONAL_ID, required}, ...] ──│    │                     │
     │                     │                     │                     │
     │ POST /client/documents/kyc/documents      │                     │
     │──── (multipart file upload) ────────▶│   │                     │
     │◀── 201 {docId, status: PENDING} ───│    │                     │
     │                     │                     │                     │
     │ POST /client/documents/kyc/submit         │                     │
     │────────────────────▶│                     │                     │
     │◀── {status: SUBMITTED} ──│               │                     │
     │                     │                     │                     │
     │ GET /client/me/onboarding-status          │                     │
     │────────────────────▶│                     │                     │
     │◀── {nextStep: AWAIT_KYC_REVIEW} ──│      │                     │
     │                     │                     │                     │
     │                     │                     │    (Admin reviews)  │
     │                     │◀───── Approve KYC ──────────────────────│
     │                     │                     │                     │
     │ GET /client/me/onboarding-status          │                     │
     │────────────────────▶│                     │                     │
     │◀── {nextStep: COMPLETED, portalFullyActive: true} ──│          │
     │                     │                     │                     │
     │  ✅ Full portal access                    │                     │
```

---

## Résumé des endpoints

| Action | Méthode | Endpoint | Auth requise |
|--------|---------|----------|:------------:|
| Inscription | POST | `/auth/register` | ❌ |
| Vérifier email | POST | `/auth/ott/validate` | ❌ |
| Renvoyer code | POST | `/auth/email/verify/resend` | ❌ |
| Login (password) | POST | `/auth/login` | ❌ |
| Login (OTT request) | POST | `/auth/ott/request` | ❌ |
| Login (OTT validate) | POST | `/auth/ott/validate` | ❌ |
| Refresh token | POST | `/auth/refresh` | ❌ |
| Logout | POST | `/auth/logout` | ✅ |
| User info | GET | `/auth/me` | ✅ |
| Reset password (request) | POST | `/auth/password-reset/request` | ❌ |
| Reset password (confirm) | POST | `/auth/password-reset/confirm` | ❌ |
| Unlock account | POST | `/auth/unlock-account` | ❌ |
| Unlock confirm | POST | `/auth/unlock-account/confirm` | ❌ |
| Onboarding status | GET | `/client/me/onboarding-status` | ✅ |
| Mon profil | GET | `/client/profile` | ✅ |
| Modifier profil | PATCH | `/client/profile` | ✅ |
| Changer mot de passe | POST | `/client/profile/change-password` | ✅ |
| Mes sessions | GET | `/client/sessions` | ✅ |
| KYC requirements | GET | `/client/documents/kyc/requirements` | ✅ |
| KYC upload doc | POST | `/client/documents/kyc/documents` | ✅ |
| KYC list docs | GET | `/client/documents/kyc/documents` | ✅ |
| KYC re-submit doc | PUT | `/client/documents/kyc/documents/{id}` | ✅ |
| KYC submit case | POST | `/client/documents/kyc/submit` | ✅ |
| KYC completion | GET | `/client/documents/kyc/completion` | ✅ |
