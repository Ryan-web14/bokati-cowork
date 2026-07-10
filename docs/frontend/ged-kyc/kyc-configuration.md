# KYC — Règles de validation croisée

> Base : `/sni/api/v1/kyc/cross-validation-rules` — back-office, configuration compliance.

Une règle vérifie qu'un champ correspond entre deux types de documents d'un même dossier (ex: le nom doit être identique sur la CNI et le passeport).

| Méthode &amp; Route | Rôle | Paramètres clés | Réponse | 🔒 |
|---|---|---|---|---|
| `POST /kyc/cross-validation-rules` | Créer une règle | `documentTypeCode1*`, `documentTypeCode2*`, `fieldToCompare*`, `blocking` (bool), `active` (bool) | `KycCrossValidationRuleResponse` (201) | `KYC:READ` |
| `PUT /kyc/cross-validation-rules/{id}` | Modifier | mêmes champs | `KycCrossValidationRuleResponse` | `KYC:READ` |
| `GET /kyc/cross-validation-rules/{id}` | Détail | — | `KycCrossValidationRuleResponse` | `KYC:READ` |
| `GET /kyc/cross-validation-rules` | Liste | query `activeOnly` (optionnel) | `List<KycCrossValidationRuleResponse>` | `KYC:READ` |
| `DELETE /kyc/cross-validation-rules/{id}` | Supprimer | — | 204 | `KYC:READ` |

`blocking = true` signifie que la divergence bloque l'approbation automatique du dossier (nécessite une revue manuelle) plutôt que d'être un simple avertissement.
