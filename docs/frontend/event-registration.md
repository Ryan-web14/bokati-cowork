# Confirmation de présence à un événement (endpoint public)

> **Audience** : Développeur frontend
> **Base URL** : `https://api.elleaose.com/sni/api/v1`

---

## 1. Vue d'ensemble

Ce module expose un formulaire public (aucune authentification requise) permettant à n'importe quel visiteur de confirmer sa présence à un événement organisé par Bokati Cowork.

Pour la **Grande Ouverture**, une URL **dédiée et fixe** est disponible (§2) — c'est celle à utiliser pour le formulaire d'ouverture. Elle pointe automatiquement vers l'événement `OUVERTURE`, **pré-chargé en base au démarrage** : aucune intervention admin n'est nécessaire, l'endpoint est utilisable dès maintenant.

Le moteur sous-jacent reste **générique et réutilisable** pour de futurs événements (§4) : chaque événement est identifié par un `eventCode`, et un nouvel événement peut être ajouté côté back-office sans changement de ce module.

Ce qui se passe après soumission du formulaire :
1. Un **précompte** est créé côté back-office (statut `PENDING_VALIDATION`) — l'administrateur le validera plus tard pour créer un compte client (Customer + Member) réel.
2. Un **email de remerciement** est envoyé immédiatement au visiteur.
3. Une **notification interne** est envoyée à `coworkspace@elleaose.com` avec les informations du visiteur, pour que l'équipe soit informée en temps réel.

---

## 2. Grande Ouverture — URL dédiée

### `GET /public/opening`

Retourne les infos de l'événement (titre, description, statut) pour affichage en haut du formulaire.

**Response** `200 OK` :
```json
{
  "code": "OUVERTURE",
  "name": "Grande Ouverture — Bokati Cowork",
  "description": "Confirmez votre présence à l'ouverture officielle de notre espace de coworking.",
  "eventDate": null,
  "active": true
}
```

Si `active` est `false`, désactiver le formulaire (l'API refusera la soumission avec une erreur 400).

### `POST /public/opening/registrations`

**Headers** (optionnel mais recommandé pour éviter les doubles soumissions réseau) :
```
Idempotency-Key: <uuid généré côté client, ex: au clic sur "Envoyer">
```

**Request Body** :
```json
{
  "firstname": "Ryan",
  "lastname": "Otchambe",
  "phone": "061135836",
  "whatsappPhone": "061135836",
  "email": "ryan@example.com"
}
```

| Champ | Requis | Validation |
|-------|--------|------------|
| `firstname` | Oui | max 180 caractères |
| `lastname` | Oui | max 180 caractères |
| `phone` | Oui | max 30 caractères |
| `whatsappPhone` | Non | max 30 caractères — si omis, le `phone` est utilisé comme numéro WhatsApp |
| `email` | Oui | format email valide, max 250 caractères |

**Response** `201 Created` :
```json
{
  "id": 42,
  "registrationNumber": "REG-1751234567890",
  "eventCode": "OUVERTURE",
  "eventName": "Grande Ouverture — Bokati Cowork",
  "firstname": "Ryan",
  "lastname": "Otchambe",
  "fullName": "Ryan Otchambe",
  "email": "ryan@example.com",
  "phone": "061135836",
  "whatsappPhone": "061135836",
  "status": "PENDING_VALIDATION",
  "rejectionReason": null,
  "memberId": null,
  "customerId": null,
  "createdAt": "2026-07-07T10:00:00Z",
  "validatedAt": null
}
```

**Erreurs possibles** :
| Code | Message | Cause |
|------|---------|-------|
| 400 | Erreurs de validation par champ | Champ requis manquant / format invalide |
| 400 | "Event is not open for registration: OUVERTURE" | L'événement n'est plus actif |
| 409 / 400 | "Vous êtes déjà inscrit(e) à cet événement avec cet email" | Une inscription active existe déjà pour cet email |

**Recommandation frontend** : afficher un message de confirmation générique après le `201` (ex: *"Merci ! Votre présence est confirmée, vous recevrez un email de confirmation."*) — pas besoin d'exposer `registrationNumber`/`status` à l'utilisateur final, ils sont surtout utiles pour le support.

---

## 3. Ce qui se passe côté backend

1. Création du précompte (`EventRegistration`), statut `PENDING_VALIDATION`.
2. Envoi d'un email de remerciement au visiteur (adresse fournie dans `email`).
3. Envoi d'une notification interne à `coworkspace@elleaose.com` avec les coordonnées du visiteur.
4. L'administrateur valide/rejette plus tard depuis le back-office — la validation crée automatiquement un compte client (Customer + Member) préremplis avec ces informations ; le visiteur les complètera lui-même plus tard via le portail client.

---

## 4. Pour un futur événement (mécanisme générique)

Si un nouvel événement doit proposer sa propre confirmation de présence, il n'est pas nécessaire d'attendre un développement frontend/backend supplémentaire : il suffit qu'un nouvel `Event` actif soit créé côté back-office avec un nouveau `eventCode`. Les mêmes endpoints génériques, paramétrés par ce code, sont alors disponibles :

| Action | Méthode | Endpoint |
|--------|---------|----------|
| Infos événement | GET | `/public/events/{eventCode}` |
| Confirmer sa présence | POST | `/public/events/{eventCode}/registrations` |

Le comportement (validation, emails, création du précompte) est strictement identique à celui décrit en §2 et §3 — seule l'URL change. `/public/opening` (§2) n'est qu'un alias fixe de ce mécanisme, dédié à l'événement `OUVERTURE` pour donner une URL simple et mémorable à partager (site web, réseaux sociaux, QR code).

---

## Résumé des endpoints

| Action | Méthode | Endpoint | Auth requise |
|--------|---------|----------|:------------:|
| Infos Grande Ouverture | GET | `/public/opening` | ❌ |
| Confirmer sa présence (Grande Ouverture) | POST | `/public/opening/registrations` | ❌ |
| Infos événement (générique) | GET | `/public/events/{eventCode}` | ❌ |
| Confirmer sa présence (générique) | POST | `/public/events/{eventCode}/registrations` | ❌ |
