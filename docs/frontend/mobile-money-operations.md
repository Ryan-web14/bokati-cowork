# Outil admin des paiements mobile money

Base : `/sni/api/v1/payments/mobile-money/operations`. Consultation : ADMIN, SUPER_ADMIN, MANAGER.
Forcer l'issue d'un paiement : ADMIN et SUPER_ADMIN seulement.

## Routes

| Méthode | Chemin | Rôle |
| --- | --- | --- |
| GET | `/` | Liste paginée. Filtres : `from`, `to` (dates `AAAA-MM-JJ`), `status`, `provider`, `customerCode`, `phone` (chiffres, recherche partielle), `reference` (dépôt PawaPay, référence opérateur, intention, transaction), `page`, `size` (100 au plus). Plus récent d'abord. |
| GET | `/summary` | Nombre et montant par statut et devise. 30 derniers jours par défaut. |
| GET | `/reconciliation` | Relit chez PawaPay les paiements de la période (7 derniers jours par défaut, 200 au plus) et ne rend que ceux qui divergent. |
| GET | `/{depositId}` | Fiche : notre dépôt, statuts de l'intention et de la transaction, ce que PawaPay en dit relu en direct, rappels reçus, écarts. |
| POST | `/{depositId}/resolve` | Trancher un paiement. |

## Fiche d'un paiement

- `deposit` : notre dépôt (statut, `phase`, montant, opérateur, numéro, référence opérateur, message client).
- `provider` : PawaPay relu à l'instant. `available: false` si PawaPay n'a pas répondu ; `found: false` s'il ne connaît pas ce paiement.
- `discrepancies` : chaque écart avec `severity` (`CRITICAL`, `WARNING`, `INFO`), `code`, `message` et `suggestedAction`.

| Code | Sens | Action |
| --- | --- | --- |
| `PAID_AT_PROVIDER_ONLY` | PawaPay confirme, notre base attend encore (rappel perdu) | `SYNC` |
| `FAILED_AT_PROVIDER` | PawaPay a clos en échec, notre base attend encore | `SYNC` |
| `PAID_HERE_ONLY` | Notre base enregistre le paiement, PawaPay non | Ne rien rendre, contacter PawaPay |
| `AMOUNT_MISMATCH`, `CURRENCY_MISMATCH` | Montant ou devise différents | Vérifier avant toute résolution |
| `TRANSACTION_NOT_SETTLED` | Dépôt réussi, transaction non réglée | Vérifier l'affectation à la facture |
| `UNRESOLVED` | Aucune réponse définitive de l'opérateur | `SYNC`, puis résolution manuelle avec preuve |

## Résoudre un litige

```json
POST /payments/mobile-money/operations/{depositId}/resolve
{ "action": "SYNC" }

{ "action": "MARK_SUCCEEDED", "reason": "Relevé MTN fourni par le client", "providerTransactionId": "8442462698" }

{ "action": "MARK_FAILED", "reason": "L'opérateur confirme qu'aucun prélèvement n'a eu lieu" }
```

- `SYNC` : relit PawaPay et applique sa réponse. Sans risque, c'est le cas courant.
- `MARK_SUCCEEDED` / `MARK_FAILED` : seulement sur un paiement en attente ou non résolu. Motif de 10 caractères au moins ; référence opérateur obligatoire pour un succès. La décision passe par le même traitement qu'un rappel (facture, réservation, reçu, notifications), est tracée dans l'audit et envoyée aux super-administrateurs.
- Un paiement déjà réussi ne se force pas en échec : c'est un remboursement.
