# Portail Client — Phase 12 : Guide Frontend — Catalogue & Achat d'Abonnements et Passes

Tous les endpoints requièrent `Authorization: Bearer <accessToken>` sauf mention contraire.

---

## Catalogue des offres

### GET `/client/catalog/plans`

Liste les plans d'abonnement disponibles (actifs et visibles uniquement).

**Paramètres de requête :**
| Paramètre | Type | Requis | Description |
|---|---|---|---|
| `planType` | enum | Non | Filtrer par type de plan (voir valeurs ci-dessous) |
| `page` | int | Non | Défaut : 0 |
| `size` | int | Non | Défaut : 20 |
| `sort` | string | Non | Défaut : `sortOrder,asc` |

**Valeurs de `planType` :** `MEMBERSHIP`, `COWORKING_ACCESS`, `DEDICATED_DESK`, `PRIVATE_OFFICE`, `MEETING_ROOM_PACK`, `VIRTUAL_OFFICE`, `COMPANY_PLAN`, `CUSTOM`

**Réponse — `200 OK` :**
```json
{
  "data": [
    {
      "code": "PLN-COW-IND-202604-00000001",
      "name": "Accès Coworking Mensuel",
      "description": "Accès illimité à l'espace partagé...",
      "planType": "COWORKING_ACCESS",
      "targetAudience": "INDIVIDUAL",
      "sortOrder": 1,
      "startingPrice": 25000.00,
      "currency": "XAF",
      "highlightedBenefits": [
        {
          "title": "Wi-Fi Haut Débit",
          "description": "Connexion fibre optique illimitée",
          "icon": "wifi"
        },
        {
          "title": "Espace Café",
          "description": "Accès libre au coin café et boissons",
          "icon": "coffee"
        }
      ]
    }
  ],
  "pageable": {
    "page": 0,
    "size": 20,
    "totalElements": 5,
    "totalPages": 1,
    "hasNext": false,
    "hasPrevious": false
  }
}
```

**UX :** Afficher sous forme de grille de cartes. Chaque carte montre le nom, la description courte, le prix de départ et les avantages mis en avant. Utiliser `sortOrder` pour l'ordre d'affichage.

---

### GET `/client/catalog/plans/{planCode}`

Retourne le détail complet d'un plan avec ses tarifs, avantages et droits.

**Réponse — `200 OK` :**
```json
{
  "code": "PLN-COW-IND-202604-00000001",
  "name": "Accès Coworking Mensuel",
  "description": "Accès illimité à l'espace partagé...",
  "planType": "COWORKING_ACCESS",
  "targetAudience": "INDIVIDUAL",
  "sortOrder": 1,
  "requiredKycLevel": 1,
  "termsJson": "{ ... }",
  "prices": [
    {
      "id": 1,
      "billingCycle": "MONTHLY",
      "currency": "XAF",
      "amount": 25000.00,
      "setupFee": 0.00,
      "depositAmount": 0.00,
      "taxIncluded": true,
      "trialDays": 0,
      "commitmentMonths": 0,
      "taxCode": null
    },
    {
      "id": 2,
      "billingCycle": "QUARTERLY",
      "currency": "XAF",
      "amount": 67500.00,
      "setupFee": 0.00,
      "depositAmount": 0.00,
      "taxIncluded": true,
      "trialDays": 0,
      "commitmentMonths": 3,
      "taxCode": null
    }
  ],
  "benefits": [
    {
      "id": 1,
      "title": "Wi-Fi Haut Débit",
      "description": "Connexion fibre optique illimitée",
      "icon": "wifi",
      "category": "ACCESS",
      "displayOrder": 1,
      "highlighted": true,
      "included": true,
      "metadataJson": null
    }
  ],
  "entitlements": [
    {
      "id": 1,
      "entitlementCode": "ENT-ACC-HOU-CON-MON",
      "entitlementName": "Heures bureau partagé",
      "quantity": 160.00,
      "unlimited": false,
      "rolloverAllowed": false,
      "rolloverLimit": null,
      "validForDays": null,
      "priority": 100,
      "restrictionsJson": null
    }
  ]
}
```

**Erreur — `404` :** Plan introuvable, archivé ou non visible.

**UX :**
- Afficher une page détaillée avec :
  - Description complète du plan
  - Tableau comparatif des tarifs par cycle de facturation
  - Liste de tous les avantages avec icônes
  - Droits inclus avec quantités (ou "Illimité" si `unlimited = true`)
- Bouton "S'abonner" qui ouvre un sélecteur de cycle de facturation

---

## Souscription

### POST `/client/catalog/subscribe`

Crée un abonnement pour le membre authentifié.

**Corps de la requête :**
```json
{
  "planCode": "PLN-COW-IND-202604-00000001",
  "billingCycle": "MONTHLY",
  "autoRenew": true
}
```

| Champ | Type | Requis | Description |
|---|---|---|---|
| `planCode` | string | Oui | Code du plan choisi |
| `billingCycle` | enum | Oui | `MONTHLY`, `QUARTERLY`, `YEARLY`, `ONE_TIME`, `DAILY`, `WEEKLY` |
| `autoRenew` | boolean | Non | Défaut : `true` |

**Réponse — `200 OK` :** Retourne l'objet `SubscriptionResponse` (voir Phase 9).

**Flux UX :**
1. Utilisateur consulte le catalogue → sélectionne un plan
2. Choix du cycle de facturation (afficher le prix de chaque cycle)
3. Confirmation : "Vous allez souscrire à {planName} pour {amount} {currency}/{cycle}"
4. À la confirmation → `POST /client/catalog/subscribe`
5. Redirection vers la page de détail de l'abonnement
6. Si une facture est générée, proposer le paiement immédiat (voir section Paiement)

**Erreurs :**
| HTTP | Condition |
|---|---|
| `400` | Champ requis manquant ou cycle de facturation invalide |
| `404` | Plan introuvable ou non disponible |
| `409` | Le membre possède déjà un abonnement actif sur ce plan |

---

## Achat de pass

### POST `/client/catalog/passes/purchase`

Achète un pass pour le membre authentifié.

**Corps de la requête :**
```json
{
  "planCode": "PLN-MRP-IND-202604-00000001",
  "passType": "MEETING_ROOM_PACK",
  "name": "Pack Salle de réunion 10h"
}
```

| Champ | Type | Requis | Description |
|---|---|---|---|
| `planCode` | string | Oui | Code du plan associé au pass |
| `passType` | enum | Oui | Type de pass (voir valeurs ci-dessous) |
| `name` | string | Oui | Libellé du pass |

**Valeurs de `passType` :** `DAY_PASS`, `TIME_PACK`, `VISITOR_PASS`, `MEETING_ROOM_PACK`, `PROMOTIONAL_PASS`, `SUBSCRIPTION_PASS`, `COMPANY_SHARED_PASS`, `CUSTOM`

**Réponse — `200 OK` :** Retourne l'objet `PassResponse` (voir Phase 9).

**Flux UX :**
1. Utilisateur consulte le catalogue des passes
2. Sélection du type et du plan
3. Confirmation de l'achat
4. Si une facture est générée, proposer le paiement immédiat

**Erreurs :**
| HTTP | Condition |
|---|---|
| `400` | Champ requis manquant ou type de pass invalide |
| `404` | Plan introuvable ou pas de version active |

---

## Paiement depuis la page abonnement

Les clients peuvent payer directement depuis la page de détail d'un abonnement ou d'un pass, sans passer par la page de facturation.

### POST `/client/subscriptions/{subscriptionNumber}/pay/mobile-money`

Initie un paiement Mobile Money pour la facture liée à l'abonnement.

**Corps de la requête :**
```json
{
  "phoneNumber": "+242064000000",
  "correspondent": "MTN_MOMO_COG"
}
```

| Champ | Type | Requis | Description |
|---|---|---|---|
| `phoneNumber` | string | Oui | Numéro de téléphone au format international |
| `correspondent` | enum | Oui | Opérateur mobile money |

**Valeurs de `correspondent` :** `MTN_MOMO_COG`, `AIRTEL_COG`, `MTN_MOMO_COD`, `AIRTEL_COD`, `ORANGE_COD`, `VODACOM_COD`

**Réponse — `200 OK` :** Retourne `MobileMoneyDepositResponse` avec le statut de l'initiation.

**Erreurs :**
| HTTP | Condition |
|---|---|
| `403` | L'abonnement n'appartient pas au membre |
| `404` | Abonnement introuvable ou aucune facture impayée |

---

### POST `/client/subscriptions/{subscriptionNumber}/pay/wallet`

Paie la facture liée à l'abonnement via le portefeuille du membre.

**Aucun corps de requête.**

**Réponse — `200 OK` :** Retourne `PaymentTransactionResponse`.

**Erreurs :**
| HTTP | Condition |
|---|---|
| `403` | L'abonnement n'appartient pas au membre |
| `404` | Abonnement introuvable ou aucune facture impayée |
| `400` | Solde du portefeuille insuffisant |

---

### POST `/client/passes/{passNumber}/pay/mobile-money`

Initie un paiement Mobile Money pour la facture liée au pass.

**Corps de la requête :** Identique à `/client/subscriptions/{n}/pay/mobile-money`.

**Erreurs :** Mêmes codes que pour les abonnements.

---

### POST `/client/passes/{passNumber}/pay/wallet`

Paie la facture liée au pass via le portefeuille du membre.

**Aucun corps de requête.**

**Erreurs :** Mêmes codes que pour les abonnements.

---

## Solde des droits

### GET `/client/entitlements/balances`

Retourne tous les droits actifs du membre (issus de tous ses abonnements et passes).

**Réponse — `200 OK` :**
```json
[
  {
    "grantNumber": "EGT-202604-00000001",
    "entitlementCode": "ENT-ACC-HOU-CON-MON",
    "entitlementName": "Heures bureau partagé",
    "unit": "HOUR",
    "ownerType": "MEMBER",
    "ownerCode": "MEM-XYZ123",
    "quantityGranted": 160.00,
    "quantityRemaining": 42.50,
    "unlimited": false,
    "validFrom": "2026-06-01T00:00:00Z",
    "validUntil": "2026-06-30T23:59:59Z",
    "status": "ACTIVE",
    "sourceType": "SUBSCRIPTION",
    "sourceId": "SUB-MEM-COW-MON-202604-00000001"
  }
]
```

**UX :** Afficher sous forme de cartes. Chaque carte montre une barre de progression `quantityRemaining / quantityGranted`. Si `unlimited = true`, afficher "Illimité".

---

## Flux de paiement — Deux chemins possibles

Le client peut payer sa facture de deux manières :

### Chemin 1 : Depuis la page de facturation (existant)

```
1. GET /client/billing/invoices/payable     → liste des factures impayées
2. POST /client/billing/invoices/{n}/pay/mobile-money  ou  /pay/wallet
```

### Chemin 2 : Depuis la page d'abonnement ou du pass (nouveau)

```
1. GET /client/subscriptions/{n}            → détail de l'abonnement
2. POST /client/subscriptions/{n}/pay/mobile-money  ou  /pay/wallet
   — ou —
1. GET /client/passes/{n}                   → détail du pass
2. POST /client/passes/{n}/pay/mobile-money  ou  /pay/wallet
```

Les deux chemins produisent le même résultat : création d'un intent de paiement → transaction → mise à jour de la facture.

---

## Wireframes

### Grille du catalogue

```
┌──────────────────────────────────────────────────────────────────────┐
│  Nos offres                                          [Filtrer ▼]    │
├─────────────────────┬─────────────────────┬─────────────────────────┤
│  Coworking Mensuel  │  Bureau Dédié       │  Bureau Privé           │
│                     │                     │                         │
│  Accès illimité     │  Votre poste fixe   │  Espace privé et       │
│  à l'espace partagé │  dans l'open space  │  sécurisé               │
│                     │                     │                         │
│  ✓ Wi-Fi HD         │  ✓ Wi-Fi HD         │  ✓ Wi-Fi HD             │
│  ✓ Espace Café      │  ✓ Espace Café      │  ✓ Espace Café          │
│                     │  ✓ Casier personnel │  ✓ Accès 24/7           │
│                     │                     │                         │
│  À partir de        │  À partir de        │  À partir de            │
│  25 000 XAF/mois    │  75 000 XAF/mois    │  150 000 XAF/mois       │
│                     │                     │                         │
│  [Voir le détail]   │  [Voir le détail]   │  [Voir le détail]       │
└─────────────────────┴─────────────────────┴─────────────────────────┘
```

### Page détail plan + souscription

```
┌──────────────────────────────────────────────────────────────────────┐
│  ← Retour au catalogue                                              │
│                                                                      │
│  Accès Coworking Mensuel                                             │
│  Accès illimité à l'espace partagé avec tous les services inclus.   │
│                                                                      │
│  ┌─ Tarifs ──────────────────────────────────────────────────────┐   │
│  │  ○ Mensuel      25 000 XAF/mois                              │   │
│  │  ○ Trimestriel  67 500 XAF/trimestre  (économisez 10%)       │   │
│  │  ○ Annuel      240 000 XAF/an         (économisez 20%)       │   │
│  └───────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  Avantages inclus :                                                  │
│  ✓ Wi-Fi Haut Débit — Connexion fibre optique illimitée              │
│  ✓ Espace Café — Accès libre au coin café et boissons              │
│  ✓ Imprimante — 50 pages/mois incluses                              │
│                                                                      │
│  Droits :                                                            │
│  • 160 heures de bureau partagé / mois                               │
│  • 5 heures de salle de réunion / mois                               │
│                                                                      │
│  [  S'abonner  ]                                                     │
└──────────────────────────────────────────────────────────────────────┘
```

### Confirmation de souscription

```
┌──────────────────────────────────────────────────────────────────────┐
│  Confirmer votre souscription                                        │
│                                                                      │
│  Plan : Accès Coworking Mensuel                                      │
│  Cycle : Mensuel                                                     │
│  Montant : 25 000 XAF/mois                                          │
│  Renouvellement automatique : Oui                                    │
│                                                                      │
│  [ Annuler ]                        [ Confirmer la souscription ]    │
└──────────────────────────────────────────────────────────────────────┘
```

### Paiement depuis l'abonnement

```
┌──────────────────────────────────────────────────────────────────────┐
│  Abonnement : Coworking Mensuel               [PAIEMENT EN ATTENTE] │
│                                                                      │
│  Montant dû : 25 000 XAF                                            │
│                                                                      │
│  Payer avec :                                                        │
│  ┌─────────────────────────────────────────────────────────────────┐ │
│  │  ○ Mobile Money                                                 │ │
│  │    Numéro : [+242 ___________]   Opérateur : [MTN ▼]           │ │
│  │                                                                 │ │
│  │  ○ Portefeuille                                                 │ │
│  │    Solde disponible : 45 000 XAF                                │ │
│  └─────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  [  Payer 25 000 XAF  ]                                              │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Séquence de chargement recommandée

```
Page Catalogue :
1. GET /client/catalog/plans                    → grille des offres

Page Détail Plan :
2. GET /client/catalog/plans/{planCode}         → détail complet

Souscription :
3. POST /client/catalog/subscribe               → créer l'abonnement
4. GET /client/subscriptions/{n}                → vérifier le statut
5. POST /client/subscriptions/{n}/pay/wallet    → payer (ou /pay/mobile-money)

Achat de pass :
6. POST /client/catalog/passes/purchase         → créer le pass
7. GET /client/passes/{n}                       → vérifier le statut
8. POST /client/passes/{n}/pay/wallet           → payer (ou /pay/mobile-money)

Widget droits :
9. GET /client/entitlements/balances            → barre de progression des droits
```

---

## Référence des erreurs

| HTTP | Condition |
|---|---|
| `400` | Champ requis manquant, cycle de facturation invalide, solde insuffisant |
| `403` | Pas un membre ou accès portail non accordé |
| `404` | Plan/abonnement/pass introuvable, non visible, ou n'appartient pas au membre |
| `409` | Abonnement déjà actif sur ce plan |

---

## Badges de type de plan

| Type | Couleur | Libellé |
|---|---|---|
| `MEMBERSHIP` | Violet | Adhésion |
| `COWORKING_ACCESS` | Vert | Coworking |
| `DEDICATED_DESK` | Bleu | Bureau dédié |
| `PRIVATE_OFFICE` | Indigo | Bureau privé |
| `MEETING_ROOM_PACK` | Orange | Salle de réunion |
| `VIRTUAL_OFFICE` | Gris | Bureau virtuel |
| `COMPANY_PLAN` | Rouge | Entreprise |
| `CUSTOM` | Gris foncé | Personnalisé |

## Badges de type de pass

| Type | Couleur | Libellé |
|---|---|---|
| `DAY_PASS` | Vert | Pass journée |
| `TIME_PACK` | Bleu | Pack horaire |
| `VISITOR_PASS` | Jaune | Pass visiteur |
| `MEETING_ROOM_PACK` | Orange | Pack salle |
| `PROMOTIONAL_PASS` | Rose | Promotionnel |
| `SUBSCRIPTION_PASS` | Violet | Lié à l'abonnement |
| `COMPANY_SHARED_PASS` | Rouge | Pass entreprise |
| `CUSTOM` | Gris | Personnalisé |
