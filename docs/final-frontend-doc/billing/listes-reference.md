# Listes de référence du catalogue (lot C0)

> Rédigé d'après le code (compléments de facturation, lots C0 à C16, 13/11/2026). Routes exactes et champs JSON : [routes.md](routes.md) et `routes/`.

## À retenir pour l'écran

- **Ne plus coder de liste en dur.** Le formulaire d'un article se remplit avec un seul appel : `GET /billing/catalog/references` (familles avec leurs sous-familles, unités, taxes choisissables aujourd'hui, et `strict`).
- Familles, sous-familles et unités s'ajoutent depuis l'écran (droit `BILLING:CATALOG_REFERENCES`) ; les règles de taxe avec le droit `BILLING:TAX_RULES`.
- L'article garde ses champs texte `category`, `subcategory`, `unit`, `taxRuleCode`, qui portent désormais les **codes** des listes. Afficher les libellés renvoyés : `categoryLabel`, `subcategoryLabel`, `unitLabel`, `taxRule` (code, nom, taux, traitement, mention).
- `referenceWarnings` (liste de textes) dans la réponse d'un article : à afficher après l'enregistrement (par exemple un code de taxe inconnu gardé tel quel).

## Réponse de `GET /billing/catalog/references`

```json
{
  "categories": [
    {"code": "SALLES", "label": "Salles", "parentCode": null, "revenueAccount": "7063", "active": true, "itemCount": 12,
     "subcategories": [{"code": "REUNION", "label": "Salles de réunion", "parentCode": "SALLES", "itemCount": 5, "subcategories": []}]}
  ],
  "units": [{"code": "heure", "label": "Heure", "pluralLabel": "heures", "symbol": "h", "quantityScale": 2, "active": true, "itemCount": 8}],
  "taxRules": [{"code": "TVA_CG_18", "baseCode": "TVA_CG_18", "name": "TVA Congo 18%", "rate": 18, "treatment": "STANDARD",
                "labelOnDocument": "TVA 18 %", "currentlyValid": true, "selectableOnItem": true}],
  "strict": false
}
```

`includeInactive=true` ajoute les entrées désactivées (écran de gestion). Les centimes additionnels ne sont pas dans `taxRules` : ils se calculent sur la TVA et ne se choisissent pas sur un article.

## Gestion

| Route | Rôle |
| --- | --- |
| `GET`, `POST /billing/catalog/categories` | Familles ; sans `code`, il est tiré du libellé (majuscules, sans accents) |
| `PUT /billing/catalog/categories/{code}` | Libellé, description, compte de produit, ordre (le code ne change pas) |
| `PATCH .../{code}/activate`, `.../deactivate` | Désactiver retire des choix, l'entrée reste lisible sur les articles |
| `DELETE /billing/catalog/categories/{code}` | Seulement si inutilisée, sinon 409 `REFERENCE_UTILISEE` |
| `GET`, `POST /billing/catalog/categories/{code}/subcategories`, `PUT`, `DELETE .../{subCode}`, `PATCH .../{subCode}/activate`, `.../deactivate` | Sous-familles |
| `GET`, `POST /billing/catalog/units`, `PUT`, `DELETE /billing/catalog/units/{code}`, `PATCH .../activate`, `.../deactivate` | Unités (`pluralLabel`, `symbol`, `quantityScale` 0 à 4, `inventoryUnitCode`) |
| `POST /billing/catalog/references/merge` | Fusion d'un doublon : `{"list": "CATEGORY"|"SUBCATEGORY"|"UNIT", "parentCode", "sourceCode", "targetCode"}` ; renvoie `movedItems` |
| `GET /billing/tax-rules`, `GET /billing/tax-rules/{baseCode}/versions` | Règles et leurs versions |
| `POST /billing/tax-rules` | Nouvelle règle : `code`, `name`, `taxType` (VAT), `rate`, `treatment` (STANDARD, REDUCED, EXEMPT, OUT_OF_SCOPE), `labelOnDocument`, `exemptionReason` (obligatoire pour EXEMPT), `validFrom` |
| `PUT /billing/tax-rules/{code}` | Libellés et mentions seulement |
| `POST /billing/tax-rules/{baseCode}/successor` | **Changement de taux** à une date : `{"rate": 19, "validFrom": "2027-01-01"}` ; la version en cours s'arrête la veille ; l'article garde son code |

## Contrôle à l'enregistrement d'un article

| Valeur envoyée | Mode souple (`strict: false`, défaut) | Mode strict |
| --- | --- | --- |
| Code ou libellé connu | Le code de l'entrée est enregistré | Pareil |
| Famille, sous-famille, unité inconnue | Ajoutée à la liste (`createdFromItem: true`) | 400 `REFERENCE_INCONNUE` |
| Entrée désactivée | Acceptée | 400 `REFERENCE_INACTIVE` |
| Code de taxe inconnu | Gardé, avertissement dans `referenceWarnings` | 400 `REFERENCE_INCONNUE` |
| Valeur identique à celle déjà enregistrée | Toujours acceptée | Toujours acceptée |

Le mode strict s'active avec le paramètre `billing.catalog.reference-lists.strict`, une fois l'écran passé aux listes. Filtre de la liste des articles : `GET /billing/catalog?category=&subcategory=`.
