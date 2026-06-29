# Portail Client — Phase 9 : Guide Frontend — Suivi Abonnement

Tous les endpoints requièrent `Authorization: Bearer <accessToken>`.

---

## Abonnements

### GET `/client/subscriptions`

Liste les abonnements du membre, du plus récent au plus ancien.

**Paramètres de requête :**
| Paramètre | Type | Requis | Description |
|---|---|---|---|
| `status` | enum | Non | Filtrer par statut (voir valeurs ci-dessous) |
| `page` | int | Non | Défaut : 0 |
| `size` | int | Non | Défaut : 20 |

**Valeurs de `status` :** `PENDING`, `ACTIVE`, `PAUSED`, `CANCELLED`, `EXPIRED`, `PAST_DUE`

**Badges de statut :**
| Statut | Couleur | Libellé |
|---|---|---|
| `ACTIVE` | Vert | Actif |
| `PENDING` | Jaune | En attente |
| `PAUSED` | Bleu | Suspendu |
| `PAST_DUE` | Orange | Paiement en retard |
| `CANCELLED` | Rouge | Annulé |
| `EXPIRED` | Gris | Expiré |

Si `cancelAtPeriodEnd = true` et `status = ACTIVE`, afficher un badge d'avertissement : "Se termine le {currentPeriodEnd}".

---

### GET `/client/subscriptions/current`

Retourne l'abonnement actif du membre. Utiliser pour le widget tableau de bord.

**Erreur — `404` :** Le membre n'a pas d'abonnement actif.

**UX :** Appeler en premier sur la page d'accueil. Si `404`, afficher "Aucun abonnement actif — [Voir les offres]".

---

### GET `/client/subscriptions/{subscriptionNumber}`

Retourne le détail complet d'un abonnement spécifique.

---

### DELETE `/client/subscriptions/{subscriptionNumber}`

Résilie un abonnement **à la fin de la période en cours** (jamais immédiatement).

**Corps de la requête (optionnel) :**
```json
{
  "reason": "Je déménage hors de la ville"
}
```

**Réponse — `200 OK` :** Abonnement mis à jour avec `cancelAtPeriodEnd: true`.

**Flux UX :**
1. Boîte de confirmation : "Votre abonnement restera actif jusqu'au {currentPeriodEnd}, puis sera résilié."
2. Zone de texte optionnelle : "Motif de résiliation (optionnel)"
3. À la confirmation → `DELETE` → toast de succès
4. Rafraîchir ; afficher le badge "Résiliation programmée"

**Ne jamais afficher ce bouton si `cancelAtPeriodEnd = true` ou si `status` est `CANCELLED`/`EXPIRED`.**

---

### GET `/client/subscriptions/{subscriptionNumber}/entitlements`

Liste les droits rattachés à cet abonnement.

**Valeurs de `unit` :** `HOUR`, `DAY`, `SESSION`, `CREDIT`, `UNIT`, `ACCESS`

**Valeurs de `status` :** `ACTIVE`, `EXHAUSTED`, `EXPIRED`, `REVOKED`

Afficher une barre de progression : `quantityRemaining / quantityGranted`. Si `unlimited = true`, afficher "Illimité".

---

### GET `/client/subscriptions/{subscriptionNumber}/history`

Retourne l'historique des changements de statut, du plus récent au plus ancien.

Afficher sous forme de timeline verticale. Si `changedBy = "system"`, afficher "Système".

---

### GET `/client/subscriptions/{subscriptionNumber}/billing`

Retourne le calendrier de facturation.

**Valeurs de `status` :** `ACTIVE`, `PAUSED`, `CANCELLED`, `FAILED`

Si `retryCount > 0`, afficher : "Tentative de paiement échouée ({retryCount} essai(s)) — Mettez à jour votre moyen de paiement."

---

## Passes

### GET `/client/passes`

Liste les passes du membre.

**Paramètres de requête :**
| Paramètre | Type | Description |
|---|---|---|
| `passType` | enum | `SUBSCRIPTION_PASS`, `PREPAID_PASS`, `PROMOTIONAL_PASS`, `TRIAL_PASS` |
| `status` | enum | `ACTIVE`, `EXPIRED`, `CANCELLED`, `PENDING` |
| `page` / `size` | int | Pagination (défaut : 20) |

Si `maxUses` n'est pas null, afficher "Utilisé {usedCount}/{maxUses} fois".
Si `validUntil` est dans moins de 7 jours, afficher un badge d'expiration imminente.

---

### GET `/client/passes/{passNumber}`

Retourne le détail complet d'un pass avec tous ses droits rattachés.

**Erreur — `404` :** Pass introuvable ou appartient à un autre membre.

---

## Solde des droits

### GET `/client/entitlements/balances`

Retourne tous les droits actifs du membre (issus de tous ses abonnements et passes). Utiliser pour le widget "Solde de droits" du tableau de bord.

**UX :** Afficher sous forme de cartes. Regrouper par `entitlementCode` si le membre possède plusieurs droits du même type.

---

## Widget Abonnement — Tableau de bord

```
┌──────────────────────────────────────────────────────┐
│  Abonnement actuel                                    │
│  Premium Mensuel                          [ACTIF]     │
│                                                       │
│  Période en cours : 1 juin — 30 juin 2026             │
│  Prochain renouvellement : 1 juillet 2026             │
│                                                       │
│  Droits disponibles :                                 │
│  ▓▓▓▓▓▓▒▒▒▒  17 / 40 h  Heures bureau partagé        │
│                                                       │
│  [Voir mon abonnement]  [Voir mes passes]             │
└──────────────────────────────────────────────────────┘
```

Si `cancelAtPeriodEnd = true` :
```
⚠ Résiliation programmée au 30 juin 2026
```

Si aucun abonnement actif :
```
Aucun abonnement actif — [Voir les offres]
```

---

## Séquence de chargement recommandée

```
1. GET /client/subscriptions/current          → widget tableau de bord
2. GET /client/entitlements/balances          → widget droits
3. GET /client/subscriptions/{n}              → détail abonnement
4. GET /client/subscriptions/{n}/entitlements
5. GET /client/subscriptions/{n}/billing
6. GET /client/subscriptions/{n}/history
7. GET /client/passes                         → onglet passes
8. GET /client/passes/{n}                     → détail pass
```

---

## Référence des erreurs

| HTTP | Condition |
|---|---|
| `400` | Valeur de statut invalide dans les paramètres |
| `403` | Pas un membre ou accès portail non accordé |
| `404` | Abonnement/pass introuvable ou appartient à un autre membre |
| `409` | Annulation sur un abonnement déjà annulé ou expiré |
