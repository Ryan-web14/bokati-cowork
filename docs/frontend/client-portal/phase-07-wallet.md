# Portail Client — Phase 7 : Guide Frontend — Portefeuille

Tous les endpoints requièrent `Authorization: Bearer <accessToken>`.

Un membre peut posséder plusieurs portefeuilles — un par devise (ex. : XAF et CDF). Les portefeuilles sont créés automatiquement lors du premier besoin (ex. : premier paiement par portefeuille). Le membre n'a jamais besoin de créer un portefeuille manuellement.

---

## GET `/client/wallet`

Liste tous les portefeuilles du membre.

**Réponse — `200 OK` :**
```json
{
  "data": [
    {
      "walletNumber": "WAL-2026-0001",
      "ownerType": "MEMBER",
      "ownerCode": "MBR-2026-0001",
      "currency": "XAF",
      "status": "ACTIVE",
      "availableBalance": 25000,
      "ledgerBalance": 25000,
      "heldBalance": 0,
      "openedAt": "2026-01-15T10:00:00Z",
      "closedAt": null
    }
  ],
  "pageable": { "page": 0, "size": 20, "totalElements": 1, "totalPages": 1, "first": true, "last": true }
}
```

**Signification des champs de solde :**
| Champ | Signification |
|---|---|
| `availableBalance` | Montant disponible immédiatement |
| `ledgerBalance` | Total déposé (y compris les montants bloqués) |
| `heldBalance` | Montant temporairement réservé (ex. : transactions en attente) |

Toujours afficher `availableBalance` à l'utilisateur. Afficher `heldBalance` uniquement s'il est > 0 (ex. : "XAF 5 000 en attente").

**Valeurs de `status` :**
| Statut | Signification |
|---|---|
| `ACTIVE` | Normal — utilisable |
| `SUSPENDED` | Temporairement bloqué par un admin |
| `LOCKED` | Verrouillé en attente d'examen |
| `UNDER_REVIEW` | En examen de conformité |
| `CLOSED` | Définitivement fermé |

Seuls les portefeuilles `ACTIVE` peuvent être utilisés pour le paiement.

---

## GET `/client/wallet/{walletNumber}`

Retourne un portefeuille spécifique par son numéro.

**Réponse — `200 OK` :** Même structure qu'un élément de la liste ci-dessus.

---

## GET `/client/wallet/{walletNumber}/ledger`

Retourne l'historique des transactions d'un portefeuille, du plus récent au plus ancien.

**Paramètres de requête :** `page` (défaut : 0), `size` (défaut : 30)

**Réponse — `200 OK` :**
```json
{
  "data": [
    {
      "entryNumber": "WLE-2026-0088",
      "walletNumber": "WAL-2026-0001",
      "direction": "DEBIT",
      "amount": 75000,
      "currency": "XAF",
      "balanceAfter": 25000,
      "entryType": "PAYMENT",
      "sourceType": "INVOICE",
      "sourceCode": "INV-2026-0042",
      "reference": "Paiement Facture INV-2026-0042",
      "createdBy": "MBR-2026-0001",
      "createdAt": "2026-06-15T09:05:00Z"
    },
    {
      "entryNumber": "WLE-2026-0055",
      "walletNumber": "WAL-2026-0001",
      "direction": "CREDIT",
      "amount": 100000,
      "currency": "XAF",
      "balanceAfter": 100000,
      "entryType": "ADMIN_TOPUP",
      "sourceType": "ADMIN",
      "sourceCode": null,
      "reference": "Rechargement initial",
      "createdBy": "admin",
      "createdAt": "2026-01-15T10:00:00Z"
    }
  ],
  "pageable": { ... }
}
```

**Valeurs de `direction` :**
| Valeur | Signification | Affichage |
|---|---|---|
| `CREDIT` | Argent ajouté au portefeuille | `+` vert |
| `DEBIT` | Argent débité du portefeuille | `−` rouge |

**Valeurs de `entryType` :**
| Type | Signification |
|---|---|
| `ADMIN_TOPUP` | Rechargement par un admin |
| `ADMIN_DEBIT` | Débit manuel par un admin |
| `PAYMENT` | Utilisé pour payer une facture |
| `REFUND` | Remboursement crédité |
| `REVERSAL` | Annulation d'une transaction |
| `HOLD` | Montant mis en attente |
| `HOLD_RELEASE` | Libération d'une mise en attente |
| `ADJUSTMENT` | Ajustement manuel |
| `CASHBACK` | Cashback/récompense crédité |
| `PROMOTIONAL_CREDIT` | Crédit promotionnel ajouté |
| `OVERPAYMENT_CREDIT` | Excédent de paiement retourné au portefeuille |

**Notes UX :**
- Afficher `balanceAfter` comme colonne de solde courant dans le relevé
- Regrouper les entrées par date pour un affichage style relevé bancaire
- `reference` est toujours une description lisible — l'utiliser directement comme libellé de l'entrée

---

## Widget Portefeuille

Mise en page suggérée pour une carte compacte :

```
┌─────────────────────────────────┐
│  Portefeuille XAF               │
│  Solde disponible               │
│  XAF 25 000                     │
│                                 │
│  [Voir les mouvements →]        │
└─────────────────────────────────┘
```

Si `heldBalance > 0`, ajouter une ligne :
```
  XAF 5 000 en attente de confirmation
```

Si `status != "ACTIVE"`, afficher une bannière :
```
  ⚠ Portefeuille suspendu — contactez le support
```

---

## Recharge du portefeuille

Les portefeuilles sont crédités par l'équipe admin. Les membres ne peuvent pas recharger leur propre portefeuille depuis le portail. Des crédits apparaissent automatiquement lorsque :
- Un admin effectue un rechargement manuel
- Un paiement est remboursé
- Une facture est surpayée (l'excédent est crédité)
- Un crédit promotionnel est accordé

Si le membre souhaite ajouter des fonds, il doit contacter le support ou utiliser un moyen de paiement directement sur une facture.
