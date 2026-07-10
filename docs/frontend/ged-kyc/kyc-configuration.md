# KYC — Règles de validation croisée

> **Base URL** : `https://api.elleaose.com/sni/api/v1`
> **Base de ce module** : `/kyc/cross-validation-rules` — **back-office**, configuration compliance.

Une **règle de validation croisée** vérifie qu'un champ donné correspond entre **deux types de documents** d'un même dossier KYC (ex : le nom doit être identique sur la CNI et le passeport).

---

## Le modèle `KycCrossValidationRuleResponse`

```json
{
  "id": 5,
  "documentTypeCode1": "CNI",
  "documentTypeCode2": "PASSEPORT",
  "fieldToCompare": "lastName",
  "blocking": true,
  "active": true
}
```

---

## 1. Créer une règle

```http
POST /kyc/cross-validation-rules
Content-Type: application/json
```
```json
{
  "documentTypeCode1": "CNI",
  "documentTypeCode2": "PASSEPORT",
  "fieldToCompare": "lastName",
  "blocking": true,
  "active": true
}
```

| Champ | Obligatoire | Règle |
|---|---|---|
| `documentTypeCode1` | ✅ | Premier type de document (max 150). |
| `documentTypeCode2` | ✅ | Second type de document (max 150). |
| `fieldToCompare` | ✅ | Champ à comparer entre les deux (max 80 ; ex `lastName`, `documentNumber`). |
| `blocking` | — | `true` = une divergence **bloque** l'approbation automatique et impose une revue manuelle. `false` = simple avertissement. |
| `active` | — | Règle active. |

**Réponse** `201 Created` → `KycCrossValidationRuleResponse`.

---

## 2. Modifier / lire / supprimer

```http
PUT    /kyc/cross-validation-rules/{id}              → KycCrossValidationRuleResponse   (mêmes champs)
GET    /kyc/cross-validation-rules/{id}              → KycCrossValidationRuleResponse
GET    /kyc/cross-validation-rules?activeOnly=true    → List<KycCrossValidationRuleResponse>   (activeOnly optionnel)
DELETE /kyc/cross-validation-rules/{id}              → 204 No Content
```

La liste renvoie un **tableau brut** (pas d'enveloppe paginée). Le path utilise l'**`id`** numérique de la règle.
