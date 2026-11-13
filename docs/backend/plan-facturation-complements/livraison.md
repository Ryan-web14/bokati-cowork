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

## C1. Taxe de l'article appliquée

- Le calcul de ligne (création, mise à jour, simulation, montant ventilé) lit la règle de taxe de l'article quand l'appelant ne fixe pas le taux : exonérée ou hors champ, la ligne n'est pas taxable (ni TVA ni centimes additionnels), même si l'écran envoie `taxable: true` ; taux réduit, il s'applique. Un taux fixé par l'appelant (conversion d'une pièce, dérogation) reste prioritaire.
- `taxableByDefault: false` sur l'article rend la ligne non taxable quand l'écran n'envoie pas `taxable`.
- La ligne garde la règle appliquée (`tax_rule_code`, rendue `taxRuleCode` dans la réponse de la ligne). Le PDF imprime sous le tableau une mention par règle exonérée ou hors champ, avec son motif.
- Plusieurs taux de TVA sur une pièce : une ligne de taxe par taux (« TVA 5 % », « TVA 18 % »), bases et montants ventilés au prorata, arrondi reporté sur le taux le plus lourd ; la somme égale exactement les totaux. Un seul taux : la ligne unique habituelle. La comptabilité lit les totaux de la pièce, elle n'est pas touchée.
- Pièces déjà émises : jamais recalculées.
- Migrations : dev `V339__taxe_article_ligne.sql`, prod `V333__taxe_article_ligne.sql`.
- Tests : `BillingItemTaxC1Test` (article exonéré malgré `taxable: true`, taux réduit, taux fixé par l'appelant prioritaire, ventilation exacte par taux, ligne unique pour un seul taux).

## C2. WhatsApp pour devis et proforma

- Nouveaux modèles proposés à la création (module WhatsApp) : `devis_disponible` (usage `QUOTE_AVAILABLE`), `proforma_disponible` (`PROFORMA_AVAILABLE`), `avoir_emis` (`CREDIT_NOTE_ISSUED`). Ils sont à créer depuis l'écran des modèles WhatsApp puis à faire approuver par Meta.
- `POST /billing/documents/{n}/send` en WhatsApp choisit le modèle par type de pièce ; le bouton du devis mène à `devis` dans l'espace client.
- Tests : `BillingWhatsAppC2Test`.

## C3. Dérogation de remise approuvée

- Service `DiscountApprovalService`, décisions par `DiscountApprovalHandler` (routes communes `/approvals/...`).
- Champs de la pièce : `discount_approval_number`, `discount_approval_status` (PENDING, APPROVED, REJECTED, RETURNED), `discount_approval_fingerprint`, `discount_approval_reason`, `discount_approval_comment`, `discount_approved_by`.
- Codes : `DEROGATION_EN_ATTENTE` (409, demande partie ou en attente), `DEROGATION_MOTIF_REQUIS`, `DEROGATION_REFUSEE`.
- Paramètre : `billing.discount.override-mode` (ENUM, LIVE, `APPROVAL`).
- Migrations : dev `V340__derogation_remise_approuvee.sql`, prod `V334__derogation_remise_approuvee.sql`.
- Tests : `DiscountApprovalC3Test`.

## C4. Bon de livraison

- Tables `billing_delivery_note` et `billing_delivery_note_line`, numérotation `billing_delivery_note` (`BL`). Statuts : `DRAFT`, `DELIVERED`, `CANCELLED`, `INVOICED`.
- Routes : `POST /billing/documents/{n}/delivery-notes`, `GET /billing/documents/{n}/delivery-status`, `GET /billing/delivery-notes` (filtres `customerType`, `customerCode`, `status`), `GET /billing/delivery-notes/{n}`, `GET .../pdf`, `PATCH .../deliver`, `PATCH .../cancel`, `PATCH .../signed-copy`, `POST /billing/delivery-notes/invoice`.
- Codes : `LIVRAISON_IMPOSSIBLE`, `LIVRAISON_INVALIDE`, `LIVRAISON_AU_DELA_DU_RESTE`, `RIEN_A_LIVRER`, `LIVRAISON_STATUT`, `LIVRAISON_DEJA_FACTUREE`, `MOTIF_REQUIS`.
- Stock : `billing.stock.issue-on` accepte `DELIVERY` ; en `DELIVERY`, la facture ne sort plus rien, chaque bon sort ce qu'il livre ; défaut inchangé (`INVOICE`).
- Facture des quantités livrées : `BillingDocumentServiceImpl.invoiceDeliveredQuantities`, sans reprise des acomptes du devis.
- Migrations : dev `V341__bons_de_livraison.sql`, prod `V335__bons_de_livraison.sql`.
- Tests : `DeliveryNoteC4Test`.

## C5. Statut réel des courriels

- Le courriel d'une pièce porte la pièce dans le journal des courriels du socle ; l'envoi garde son numéro (`providerReference`) ; `GET /billing/documents/{n}/sendings` rend `SENT` avec `deliveredAt`, ou `FAILED` avec `error`.
- Les rebonds tardifs ne sont pas remontés par Microsoft 365.
- Tests : `BillingEmailStatusC5Test`.

## C6. Retenues à la source

- Tables `billing_withholding_type` et `billing_withholding`, numérotation `billing_withholding` (`RAS`), règle comptable `SALES_WITHHOLDING`.
- Routes : `POST`, `GET /billing/documents/{n}/withholdings`, `GET /billing/withholdings` (`customerType`, `customerCode`, `certificate=missing`), `PATCH /billing/withholdings/{n}/certificate`, `PATCH .../cancel` ; types : `GET`, `POST /billing/withholding-types`, `PUT /{code}`, `PATCH .../activate`, `.../deactivate` (droit `BILLING:TAX_RULES` pour écrire).
- Codes : `RETENUE_IMPOSSIBLE`, `RETENUE_MONTANT`, `RETENUE_ANNULEE`.
- Comptabilité : alimentée par le traitement des ventes (`SalesAccountingService`), une fois la facture comptabilisée ; une retenue annulée avant d'être comptabilisée ne l'est jamais.
- Point à confirmer par le comptable (CD5) : comptes `4449` et `4492`, et, en régime des encaissements, le sort de la TVA en attente sur la part retenue.
- Migrations : dev `V342__retenues_a_la_source.sql`, prod `V336__retenues_a_la_source.sql`.
- Tests : `WithholdingC6Test`.

### Hors lot, livré en même temps

- **Procfile :** tas porté à 600 Mo. Le démarrage en production dépassait 90 secondes depuis la fusion de la facturation ; aussi sur `hotfix/demarrage-memoire`.
- **Alertes d'administration :** une alerte émise avant la connexion au courtier de messages (premières secondes après un démarrage) attend la connexion au lieu d'être perdue (`AdminInAppNotifier`, test `AdminInAppNotifierBrokerTest`).
