# Portail Client — Phase 3 : Guide Frontend — Profil membre

## Vue d'ensemble

La phase 3 expose l'espace profil du membre sous `/client/me`. Tous les endpoints requièrent un `accessToken` valide.

**En-têtes requis sur toutes les requêtes :**
```
Authorization: Bearer <accessToken>
```

---

## Endpoints

### GET `/sni/api/v1/client/me`

Retourne le profil complet du membre, y compris le compte client/facturation lié.

**Réponse — `200 OK` :**
```json
{
  "memberId": "MBR-202601-XXXX",
  "firstname": "Jean",
  "lastname": "Dupont",
  "email": "jean@example.com",
  "phone": "+242064000001",
  "whatsappPhone": "+242064000001",
  "avatarUrl": "https://cdn.example.com/avatars/jean.jpg",
  "status": "ACTIVE",
  "portalAccess": true,
  "customer": {
    "customerId": "CST-2026-001",
    "type": "INDIVIDUAL",
    "displayName": "Jean Dupont",
    "email": "jean@example.com",
    "billingEmail": "billing@example.com",
    "phone": "+242064000001",
    "companyName": null,
    "address": {
      "city": "Brazzaville",
      "country": "Congo",
      "district": "Plateau",
      "streetName": "Avenue Amilcar Cabral",
      "streetNumber": "12"
    }
  },
  "createdAt": "2026-06-14T09:00:00Z",
  "updatedAt": "2026-06-14T12:00:00Z"
}
```

`customer` est `null` si le membre n'a pas encore de compte facturation lié.
`avatarUrl` est `null` si aucun avatar n'a été défini.

---

### PUT `/sni/api/v1/client/me`

Met à jour le nom et les coordonnées du membre. Tous les champs sont optionnels — seules les valeurs non nulles sont mises à jour.

**Corps de la requête :**
```json
{
  "firstname": "Jean",
  "lastname": "Dupont",
  "phone": "+242064000001",
  "whatsappPhone": "+242064000001"
}
```

**Contraintes :** `firstname`/`lastname` max 350 caractères, `phone`/`whatsappPhone` max 30 caractères.

**Réponse — `200 OK` :** `ClientProfileResponse` mis à jour (même format que le GET).

> Note : L'email ne peut pas être modifié ici. Utiliser `POST /client/me/email/change` pour les changements d'email.

---

### PATCH `/sni/api/v1/client/me/avatar`

Met à jour l'avatar du membre. Le frontend est responsable de l'upload de l'image vers un CDN et fournit l'URL résultante.

**Corps de la requête :**
```json
{
  "avatarUrl": "https://cdn.example.com/avatars/jean.jpg"
}
```

**Contrainte :** `avatarUrl` requis, max 500 caractères.

**Réponse — `204 No Content`**

---

### GET `/sni/api/v1/client/me/customer`

Retourne le compte client/facturation lié au membre.

**Réponse — `200 OK` :** Objet `ClientCustomerResponse`.

**Réponse — `204 No Content` :** Si aucun compte client n'est lié.

---

### PUT `/sni/api/v1/client/me/password`

Modifie le mot de passe du membre authentifié. Nécessite le mot de passe actuel pour vérification.

**Corps de la requête :**
```json
{
  "currentPassword": "MonAncienMotDePasse1",
  "newPassword": "MonNouveauMotDePasse123"
}
```

**Contraintes :** `currentPassword` requis ; `newPassword` requis, entre 8 et 45 caractères.

**Réponse — `204 No Content`**

**Erreurs :**
| Statut | Signification |
|--------|---------------|
| `400` | Mot de passe actuel incorrect |
| `400` | Nouveau mot de passe trop court ou trop long |

---

### GET `/sni/api/v1/client/me/onboarding-status`

Retourne la progression d'onboarding du membre. Voir le guide Phase 2 pour les détails complets.

Cet endpoint est **exempté du garde KYC** — toujours accessible après vérification email.

---

### POST `/sni/api/v1/client/me/email/change`

Initie un changement d'adresse email. Un code de vérification est envoyé à l'adresse **actuelle** pour confirmer l'identité du compte.

**Corps de la requête :**
```json
{
  "newEmail": "nouveaumail@example.com",
  "currentPassword": "MonMotDePasseActuel1"
}
```

**Réponse — `200 OK` :**
```json
{
  "verificationToken": "<JWT>",
  "message": "Un code de vérification a été envoyé à votre email actuel. Saisissez-le pour confirmer le changement."
}
```

**Erreurs :**
| Statut | Signification |
|--------|---------------|
| `400` | Mot de passe actuel incorrect |
| `400` | Nouvel email déjà utilisé |
| `400` | Format email invalide |

**Flux :**
1. Afficher un champ de saisie à 6 chiffres.
2. Rappeler au membre de vérifier sa boîte **actuelle**.
3. Soumettre le code à `POST /client/me/email/change/confirm`.

---

### POST `/sni/api/v1/client/me/email/change/confirm`

Confirme le changement d'email avec le code OTT à 6 chiffres.

**Corps de la requête :**
```json
{
  "ottToken": "123456",
  "verificationToken": "<JWT depuis email/change>"
}
```

**Réponse — `204 No Content`**

**Effets de bord :**
- L'email est mis à jour sur le compte utilisateur et le profil membre.
- **Toutes les sessions actives sont révoquées** — le membre doit se reconnecter avec le nouvel email.

**Erreurs :**
| Statut | Signification |
|--------|---------------|
| `400` | Code de vérification invalide ou expiré |
| `400` | Aucun changement d'email en attente trouvé |

**Après succès :**
- Effacer tous les tokens côté client.
- Rediriger vers la page de connexion avec le message : "Email mis à jour. Veuillez vous connecter avec votre nouvelle adresse."

---

## Checklist UX — Édition du profil

- [ ] **Champs en lecture seule :** `memberId`, `email` (afficher sans rendre modifiable)
- [ ] **Changement d'email** : flux dédié avec confirmation OTT — ne pas l'inclure dans le formulaire profil général
- [ ] **Avatar** : afficher l'avatar actuel avec un bouton de modification, uploader sur CDN d'abord, puis PATCH l'URL
- [ ] **Changement de mot de passe** : section/modal séparée du formulaire profil, mot de passe actuel requis
- [ ] Après `PUT /client/me`, mettre à jour l'état local depuis le corps de la réponse — ne pas refaire un GET
- [ ] Après confirmation du changement d'email : forcer la déconnexion, effacer les tokens, rediriger vers la connexion
- [ ] Après changement de mot de passe : envisager d'inviter à se reconnecter (optionnel)

---

## Nouvelles migrations DB (Phase 3)

| Migration | Modification |
|---|---|
| `V154` | `avatar_url VARCHAR(500)` sur `member` ; `pending_email VARCHAR(250)` sur `users` |

---

## Notes de sécurité (Phase 3)

- **Le code OTT de changement d'email est envoyé à l'email actuel** — cela prouve la propriété du compte avant de passer à la nouvelle adresse.
- **Révocation des tokens au changement d'email** — tous les refresh tokens sont révoqués côté serveur (`revokeAllTokenForUser`). Le membre est donc déconnecté de partout. Gérer cela proprement côté frontend (redirection vers la connexion avec explication).
- **Vérification du mot de passe** — le changement de mot de passe et le changement d'email requièrent tous deux le mot de passe actuel, vérifié côté serveur. Il n'y a aucun moyen de contourner cette vérification.
