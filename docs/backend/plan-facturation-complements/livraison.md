# Livraison des compléments de facturation

État livré, lot par lot, sur `feature/facturation-complements`. Le plan est dans [README.md](README.md), le détail prévu dans [lots.md](lots.md).

## C0. Listes de référence du catalogue

### Ce qui change

- Familles, sous-familles et unités sont tenues dans deux tables, `billing_catalog_category` (une sous-famille porte `parent_code`) et `billing_catalog_unit`.
- Les règles de taxe ont un code de base stable (`base_code`) que l'article référence, des versions datées, une mention imprimée, un motif d'exonération et un traitement (`STANDARD`, `REDUCED`, `EXEMPT`, `OUT_OF_SCOPE`).
- L'article garde ses colonnes texte (`category`, `subcategory`, `unit`, `tax_rule_code`), qui portent désormais les codes des listes.
- La réponse d'un article ajoute `categoryLabel`, `subcategoryLabel`, `unitLabel`, `taxRule` (code, nom, taux, traitement, mention) et `referenceWarnings`.
- La liste des articles accepte un filtre `subcategory`.

### Contrôle à l'enregistrement d'un article

S'applique à la création, à la mise à jour et à l'import CSV.

| Valeur reçue | Mode souple (par défaut) | Mode strict |
| --- | --- | --- |
| Code ou libellé connu | Le code de l'entrée est enregistré | Pareil |
| Inconnue (famille, sous-famille, unité) | Ajoutée à la liste, marquée `createdFromItem`, l'enregistrement passe | Refus `REFERENCE_INCONNUE` |
| Entrée désactivée | Acceptée | Refus `REFERENCE_INACTIVE` |
| Code de taxe inconnu | Gardé tel quel, avertissement dans `referenceWarnings`, taux par défaut | Refus `REFERENCE_INCONNUE` |
| Taxe calculée sur la TVA (centimes additionnels) | Refus `TAXE_INVALIDE` | Pareil |
| Valeur identique à celle déjà enregistrée sur l'article | Toujours acceptée | Toujours acceptée |

Le mode strict s'active avec le paramètre `billing.catalog.reference-lists.strict` (LIVE, faux par défaut), une fois que le frontend utilise les listes. Une sous-famille envoyée sans famille est rattachée à la famille « Autres ».

### Routes

| Route | Droit | Rôle |
| --- | --- | --- |
| `GET /billing/catalog/references?includeInactive=` | lecture | Familles avec sous-familles, unités, taxes choisissables aujourd'hui, mode strict |
| `GET`, `POST /billing/catalog/categories` | `BILLING:CATALOG_REFERENCES` pour écrire | Familles (code tiré du libellé s'il manque) |
| `PUT`, `DELETE /billing/catalog/categories/{code}`, `PATCH .../activate`, `.../deactivate` | idem | Libellé, description, compte de produit, ordre ; suppression si inutilisée (`REFERENCE_UTILISEE`) |
| `GET`, `POST /billing/catalog/categories/{code}/subcategories`, `PUT`, `DELETE .../{subCode}`, `PATCH .../{subCode}/activate`, `.../deactivate` | idem | Sous-familles |
| `GET`, `POST /billing/catalog/units`, `PUT`, `DELETE /billing/catalog/units/{code}`, `PATCH .../activate`, `.../deactivate` | idem | Unités (pluriel, symbole, décimales de quantité, unité d'inventaire liée) |
| `POST /billing/catalog/references/merge` | idem | Fusion de deux entrées : `{list: CATEGORY, SUBCATEGORY ou UNIT, parentCode, sourceCode, targetCode}`, renvoie `movedItems` |
| `GET /billing/tax-rules?includeInactive=`, `GET /billing/tax-rules/{baseCode}/versions` | lecture | Règles et versions |
| `POST /billing/tax-rules`, `PUT /billing/tax-rules/{code}`, `PATCH .../activate`, `.../deactivate` | `BILLING:TAX_RULES` | Nouvelle règle ; libellés et mentions d'une version |
| `POST /billing/tax-rules/{baseCode}/successor` | idem | Nouveau taux à une date : la version en cours s'arrête la veille |

### Migrations

Dev `V338__listes_reference_catalogue.sql`, prod `V332__listes_reference_catalogue.sql` (identiques).
- **Reprise :** les familles, sous-familles et unités déjà saisies sur les articles, plus les unités des lignes de pièces, entrent dans les listes avec la valeur comme code.
- **Ajouts :** la famille « Autres », onze unités de base (heure, demi-journée, jour, semaine, mois, an, personne, poste, forfait, unité, page), et les règles `TVA_CG_EXONERE` et `TVA_CG_HORS_CHAMP`.
- **Droits :** `BILLING_CATALOG_REFERENCES` (SUPER_ADMIN, ADMIN, FINANCE, MANAGER) et `BILLING_TAX_RULES` (SUPER_ADMIN, ADMIN, FINANCE).

### Tests

- `CatalogReferenceC0Test` : mode souple, mode strict, valeur inchangée, correspondance par libellé, entrée désactivée, taxe inconnue, centimes refusés, codes tirés du libellé.
- `CatalogReferenceC0IT`, sur le schéma de production validé contre les entités : reprise, ajout depuis un article, comptage, suppression refusée, successeur de taux, fusion d'unités, doublon de sous-famille refusé.

### Hors lot, livré en même temps

- **Procfile :** tas porté à 600 Mo. Le démarrage en production dépassait 90 secondes depuis la fusion de la facturation ; aussi sur `hotfix/demarrage-memoire`.
- **Alertes d'administration :** une alerte émise avant la connexion au courtier de messages (premières secondes après un démarrage) attend la connexion au lieu d'être perdue (`AdminInAppNotifier`, test `AdminInAppNotifierBrokerTest`).
