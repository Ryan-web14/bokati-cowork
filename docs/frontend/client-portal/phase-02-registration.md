# Portail Client — Phase 2 : Guide Frontend

## Vue d'ensemble

La phase 2 couvre l'inscription en ligne, le déverrouillage de compte, le renvoi de vérification email, l'état d'onboarding et le garde KYC avec période de grâce. Après cette phase, tout le cycle d'onboarding est géré côté serveur ; le frontend n'a qu'à suivre le champ `nextStep`.

---

## Nouveaux endpoints publics (sans token)

### POST `/sni/api/v1/auth/register`

Inscrit un nouveau membre depuis le portail.

**Corps de la requête :**
```json
{
  "firstname": "Jean",
  "lastname": "Dupont",
  "email": "jean@example.com",
  "password": "MonMotDePasse1",
  "phone": "+242064000001",
  "whatsappPhone": "+242064000001",
  "customerType": "INDIVIDUAL",
  "companyName": null
}
```

**Contraintes :**
- `firstname`, `lastname` : requis, max 350 caractères
- `email` : requis, format email valide, max 250 caractères
- `password` : requis, entre 8 et 45 caractères
- `phone` : requis, max 30 caractères, format `^\+?[0-9]{8,15}$`
- `whatsappPhone`, `customerType`, `companyName` : optionnels

**Réponse succès — `201 Created` :**
```json
{
  "verificationToken": "<JWT de vérification>",
  "message": "Inscription réussie. Vérifiez votre email avec le code envoyé à jean@example.com"
}
```

**Flux après inscription :**
1. Sauvegarder `verificationToken` (JWT valable 15 min) en mémoire.
2. Afficher un champ de saisie à 6 chiffres.
3. Le code a été envoyé par email — inviter le membre à vérifier sa boîte.
4. Soumettre le code à `POST /auth/ott/validate` pour compléter la vérification et recevoir `accessToken` + `refreshToken`.

**Erreurs :**
| Statut | Signification |
|--------|---------------|
| `400` | Erreur de validation sur le corps de la requête |
| `409` | Email déjà enregistré |

---

### POST `/sni/api/v1/auth/ott/validate`

Valide le code OTT à 6 chiffres. Utilisé pour la vérification email à l'inscription et pour la connexion sans mot de passe.

**Corps de la requête :**
```json
{
  "ottToken": "123456",
  "verificationToken": "<JWT de register ou ott/request>"
}
```

**Réponse succès — `200 OK` :**
```json
{
  "accessToken": "...",
  "refreshToken": "..."
}
```

Après validation réussie pour un nouveau membre, le compte est activé et l'accès au portail est accordé.

---

### POST `/sni/api/v1/auth/unlock-account`

Demande un code de déverrouillage pour un compte bloqué (ex. : trop d'échecs de connexion).

**Corps de la requête :**
```json
{ "email": "jean@example.com" }
```

**Réponse succès — `200 OK` :**
```json
{
  "verificationToken": "<JWT de vérification>",
  "message": "Si un compte existe pour cet email, un code de déverrouillage a été envoyé."
}
```

> La réponse est identique qu'un compte existe ou non — cela évite l'énumération d'emails.

---

### POST `/sni/api/v1/auth/unlock-account/confirm`

Confirme le déverrouillage avec le code OTT.

**Corps de la requête :**
```json
{
  "ottToken": "654321",
  "verificationToken": "<JWT depuis unlock-account>"
}
```

**Succès — `204 No Content`**

Après cela, le membre peut se reconnecter normalement via `/auth/login`.

**Erreurs :**
| Statut | Signification |
|--------|---------------|
| `400` | Code invalide ou expiré |

---

### POST `/sni/api/v1/auth/email/verify/resend`

Renvoie un code de vérification email. À utiliser si le code d'origine a expiré ou n'a pas été reçu.

**Corps de la requête :**
```json
{ "email": "jean@example.com" }
```

**Réponse succès — `200 OK` :**
```json
{
  "verificationToken": "<nouveau JWT>",
  "message": "Code de vérification renvoyé à jean@example.com"
}
```

L'ancien `verificationToken` est désormais invalide — le remplacer en mémoire par le nouveau.

---

## État d'onboarding (authentifié)

### GET `/sni/api/v1/client/me/onboarding-status`

Retourne la progression d'onboarding du membre. Cet endpoint est **exempté du garde KYC** — toujours accessible.

**En-têtes :** `Authorization: Bearer <accessToken>`

**Réponse — `200 OK` :**
```json
{
  "emailVerified": true,
  "profileComplete": true,
  "kycStatus": "NOT_STARTED",
  "kycCompletionPercent": 0,
  "gracePeriodEndsAt": "2026-06-21T12:00:00Z",
  "gracePeriodActive": true,
  "gracePeriodDaysRemaining": 7,
  "portalFullyActive": false,
  "nextStep": "SUBMIT_KYC",
  "restrictedActions": []
}
```

### Valeurs de `nextStep` et actions frontend associées

| `nextStep` | Action à afficher |
|---|---|
| `VERIFY_EMAIL` | Afficher l'invite de vérification email |
| `SUBMIT_KYC` | Bannière : "Complétez votre KYC pour déverrouiller l'accès complet" |
| `COMPLETE_KYC` | Bannière : "Finalisez la soumission de votre dossier KYC" |
| `AWAIT_KYC_REVIEW` | Message : "Vos documents sont en cours d'examen" |
| `COMPLETED` | Aucune bannière — accès complet |
| `SUBMIT_KYC_BEFORE_GRACE_EXPIRES` | Bannière urgente avec le nombre de jours restants |
| `CONTACT_SUPPORT` | KYC refusé, période de grâce expirée — contacter le support |

### Valeurs de `restrictedActions`

Quand ce tableau n'est pas vide, désactiver les actions correspondantes dans l'interface :
- `BOOKING` — masquer/désactiver les boutons de réservation
- `PAYMENT` — désactiver les flux de paiement
- `CONTRACT` — désactiver la signature de contrat
- `SUBSCRIPTION` — désactiver la souscription à un abonnement

---

## Garde de période de grâce KYC

Toutes les routes `GET /client/**` et `POST /client/**` (sauf `/client/me/onboarding-status`) passent par le `ClientOnboardingGuard`.

### Règles du garde (évaluées dans l'ordre) :

1. **Email non vérifié** → `403 EMAIL_NOT_VERIFIED`
2. **KYC approuvé** → passage autorisé
3. **Dans la période de grâce** → passage autorisé
4. **Période de grâce expirée + KYC non approuvé** → `403 KYC_REQUIRED`

### Format de la réponse 403

```json
{
  "status": 403,
  "errorCode": "KYC_REQUIRED",
  "message": "La vérification KYC est requise pour accéder à cette ressource. Veuillez compléter votre vérification d'identité."
}
```

```json
{
  "status": 403,
  "errorCode": "EMAIL_NOT_VERIFIED",
  "message": "Veuillez vérifier votre email avant d'accéder à cette ressource."
}
```

### Gestion côté frontend

```typescript
async function apiCall(url, options) {
  const res = await fetch(url, options);
  if (res.status === 403) {
    const body = await res.json();
    if (body.errorCode === 'KYC_REQUIRED') {
      router.push('/portal/kyc');
      return;
    }
    if (body.errorCode === 'EMAIL_NOT_VERIFIED') {
      router.push('/portal/verify-email');
      return;
    }
  }
  return res;
}
```

---

## Admin : surcharge de la période de grâce

### PATCH `/sni/api/v1/members/{memberId}/kyc-grace-period`

Réservé aux admins. Définit une période de grâce personnalisée pour un membre.

**Corps de la requête :**
```json
{ "gracePeriodDays": 14 }
```

**Contraintes :** `gracePeriodDays` doit être entre 1 et 365.

**Succès — `204 No Content`**

La période de grâce est calculée à partir de `portalActivatedAt + N jours`. Si le membre n'a pas encore activé son accès, le timestamp courant est utilisé comme base.

**Valeur par défaut globale :** configurée via la variable d'environnement `PORTAL_KYC_GRACE_PERIOD_DAYS` (défaut : 7 jours).

---

## Flux complet d'onboarding

```
[Inscription]
  POST /auth/register
    → reçoit verificationToken + email avec code à 6 chiffres

  [Le membre saisit le code]
  POST /auth/ott/validate
    → reçoit accessToken + refreshToken
    → compte activé, accès portail accordé

  [Arrivée sur le portail]
  GET /client/me/onboarding-status
    → nextStep: "SUBMIT_KYC"
    → gracePeriodActive: true, gracePeriodDaysRemaining: 7
    → restrictedActions: [] (réservation possible pendant la période de grâce)

  [Le membre soumet ses documents KYC — Phase 4]

  [L'admin approuve le KYC]

  GET /client/me/onboarding-status
    → nextStep: "COMPLETED"
    → portalFullyActive: true
    → restrictedActions: []
```

---

## Flux de déverrouillage de compte

```
[Connexions échouées répétées → compte bloqué]

POST /auth/unlock-account
  { "email": "jean@example.com" }
  → reçoit verificationToken

[Le membre saisit le code reçu par email]

POST /auth/unlock-account/confirm
  { "ottToken": "123456", "verificationToken": "..." }
  → 204 No Content

[Le membre peut se reconnecter]
POST /auth/login
  { "email": "...", "password": "..." }
```

---

## Recommandations de stockage des tokens

| Token | Stockage | Durée |
|---|---|---|
| `accessToken` | Mémoire (state/context) | 15 minutes |
| `refreshToken` | Cookie `httpOnly` ou `localStorage` | 7 jours |
| `verificationToken` (JWT OTT) | Mémoire uniquement | 15 minutes |

Ne jamais persister le `verificationToken` en localStorage — il est court-lived et à usage unique.
