# Suggestion de fournisseurs (complément du lot F13)

Branche `feature/finance-comptabilite`. Migrations : dev `V311__suggestion_fournisseurs.sql`, prod `V306__suggestion_fournisseurs.sql` (contenu identique). Code dans `features/inventory/procurement/suggestion` (catégories, suggestion), avec des retouches dans `features/inventory/procurement` (fiche fournisseur), `features/fund` (lignes) et `features/accounting` (`PurchaseService`).

## Constat

- Demande de l'exploitant : les fournisseurs proposés pour les demandes sont ceux du **module inventaire** (`inventory_supplier`) ; un module de suggestion enrichit la fiche actuelle **sans la casser** et propose, selon la catégorie du besoin, des fournisseurs qui peuvent appartenir à une ou plusieurs catégories. Complément : un **référentiel propre de catégories de fournisseurs**, relié aux natures d'opération.
- La catégorie d'une ligne de besoin (état de besoin `fund_need_line`, demande de fonds `fund_request_line`, justification `expense_justification_line`) est sa **nature d'opération** (`nature_code`, table `cash_operation_nature`), devinée depuis la désignation quand elle n'est pas saisie.
- Ces lignes ne portaient qu'un fournisseur en texte libre (`supplier`), sans lien avec une fiche.
- Le fournisseur comptable du lot F13 (`acc_supplier`) n'était pas relié à la fiche d'inventaire.

## Modèle

| Table | Rôle |
| --- | --- |
| `inventory_supplier_category_ref` | Référentiel des catégories de fournisseurs : `code` unique, `label`, `description`, `parent_id` (sous-catégories, sans cycle), `active`, `sort_order`, `created_by/at`, `updated_by/at`. |
| `inventory_supplier_category_ref_target` | Pont : une catégorie fournisseur est reliée à des **natures d'opération** (`NATURE`) et à des **catégories de stock** (`INVENTORY_CATEGORY`). Unique sur (catégorie, sorte, code). |
| `inventory_supplier_category` | Catégories d'un fournisseur : `category_kind` = `NATURE`, `INVENTORY_CATEGORY` ou `SUPPLIER_CATEGORY`, `category_code`. Unique sur (fournisseur, sorte, code). Codes vérifiés à l'écriture (nature existante, catégorie de stock existante, catégorie fournisseur existante et active). |
| `fund_need_line.supplier_code`, `fund_request_line.supplier_code`, `expense_justification_line.supplier_code` | Fiche fournisseur de la ligne, facultative. |
| `acc_supplier.inventory_supplier_id` | Fiche d'inventaire du fournisseur comptable, facultative, unique (index partiel). |

Catégories de départ (migration) : Fournitures de bureau, Informatique et téléphonie, Entretien et nettoyage, Restauration, café et traiteur, Travaux et maintenance, Mobilier et aménagement, Énergie et eau, Prestations de services, Transport et logistique, Communication et impression, Sécurité et gardiennage. Pont posé vers les natures existantes : `ACHAT_FOURNITURES_BUREAU` (fournitures), `TELECOMMUNICATIONS` et `ACHAT_PETIT_MATERIEL` (informatique), `ACHAT_PRODUITS_ENTRETIEN` (entretien), `ACHAT_CONSOMMABLES_ESPACES` (restauration), `PETITES_REPARATIONS` (travaux), `ACHAT_PETIT_MATERIEL` (mobilier), `TRANSPORT_COURSE` et `AFFRANCHISSEMENT` (transport). Une nature absente de la base est ignorée.

## Fiche fournisseur enrichie, sans rupture

- `SupplierRequest` (création `POST /suppliers`, modification `PUT /suppliers/{code}`) gagne `categories` (facultatif : liste de `{kind, code}`). Absent (`null`) : les catégories restent inchangées ; liste vide : toutes retirées.
- `SupplierResponse` gagne `categories` : `[{kind, code, label, active}]`, le libellé étant celui de la nature, de la catégorie de stock ou de la catégorie fournisseur.
- `GET /suppliers` accepte `categoryKind` et `categoryCode`. Sans `categoryKind`, le filtre porte sur le référentiel des catégories fournisseurs ; une catégorie parente englobe ses sous-catégories. Sans `categoryCode`, la liste est celle d'avant.

## Lignes reliées aux fiches, sans rupture

- `supplierCode` facultatif sur les lignes d'état de besoin (`NeedLineRequest`), de demande de fonds (`RequestLine`) et de justification (`ExpenseInput`). Fourni : la fiche doit exister et être **active** (`400` « Ligne n : fournisseur inconnu » ou « n'est pas actif ») ; son nom remplit `supplier`. Absent : le texte libre `supplier` est accepté comme avant.
- Les réponses (`NeedLine`, `RequestLine`, `ExpenseLine`) renvoient `supplierCode`.
- La transformation d'un état de besoin en demande de fonds et la duplication d'une demande reprennent `supplierCode`.
- La meilleure suggestion n'est pas ajoutée aux réponses des lignes (coût d'une requête par ligne à chaque lecture) : la route de suggestion par état de besoin ou par demande suffit.

## Règles de classement

Fournisseurs d'inventaire **actifs** seulement. Score = appartenance déclarée (le meilleur cas compte) + historique + prix + délai ; chaque point vient avec une raison lisible.

| Critère | Points | Raison affichée |
| --- | --- | --- |
| Rattaché directement à la nature | 100 | « rattache a la nature « Achat de fournitures de bureau » » |
| Classé dans une catégorie fournisseur reliée à la nature (ou sous-catégorie d'une telle catégorie), ou dans la catégorie fournisseur demandée | 70 | « categorie « Fournitures de bureau » » |
| Rattaché directement à la catégorie de stock | 60 | « rattache a la categorie de stock « ... » » |
| Catégorie fournisseur reliée à la catégorie de stock | 45 | « categorie « ... » » |
| Historique : 5 par ligne d'état de besoin, de demande, de justification non rejetée ou de commande comptable non annulée sur la même nature (commandes de stock non annulées pour une catégorie de stock), au plus 50 | 0 à 50 | « deja retenu 4 fois pour cette nature » |
| Prix fournisseur de l'article (`inventory_supplier_item` actif) : prix contractuel 20 (sans prix 10), le moins cher +10, préféré +5 | 0 à 35 | « prix contractuel pour l'article : 2 500 XAF », « prix le plus bas pour l'article », « fournisseur prefere pour l'article » |
| Délai : 10 moins le délai en jours (celui de l'article, sinon le délai moyen du fournisseur), jamais négatif | 0 à 10 | « delai moyen 3 j » |

- L'historique compte une ligne pour un fournisseur quand elle porte son code, ou, faute de code (lignes antérieures), quand son texte libre est exactement son nom (casse et espaces autour ignorés). Les commandes comptables comptent par le fournisseur comptable relié à la fiche.
- L'historique est plafonné à 50 : un fournisseur sans catégorie déclarée mais avec un historique est proposé, toujours après un fournisseur déclaré pour la nature ou une catégorie reliée.
- Égalité : le plus d'historique, puis le nom.
- Sans nature saisie, elle est devinée depuis la désignation (même règle que l'état de besoin : mots-clés des natures, la plus utilisée d'abord), `natureGuessed: true`.
- Limite : paramètre `limit` de la route, sinon le réglage `inventory.supplier-suggestion.limit` (5, lu à chaud, domaine Comptabilité), au plus 50.
- Le montant (`amount`) est repris dans la réponse ; il n'entre pas dans le score.
- Ni IA ni appel externe.

## Routes

Préfixe `/api/v1`. Droits du module inventaire pour `/inventory/...` : lecture `INVENTORY:READ`, création (`POST`) `INVENTORY:CREATE`, modification (`PUT`, `PATCH`) `INVENTORY:UPDATE`, suppression (`DELETE`) `INVENTORY:DELETE` (règle `AdminApiAuthorizationManager` existante). Écritures auditées (module `INVENTORY`).

| Route | Rôle |
| --- | --- |
| `GET /inventory/procurement/supplier-categories?q=&active=&parentCode=` | Référentiel, filtrable (texte, actives, enfants d'une catégorie) ; chaque catégorie avec parent, natures et catégories de stock reliées, nombre de fournisseurs et de sous-catégories |
| `GET /inventory/procurement/supplier-categories/{code}` | Une catégorie |
| `POST /inventory/procurement/supplier-categories` | Création : `code` (facultatif, déduit du libellé), `label`, `description`, `parentCode`, `active`, `sortOrder`, `natureCodes`, `inventoryCategoryCodes` |
| `PUT /inventory/procurement/supplier-categories/{code}` | Modification ; `natureCodes` / `inventoryCategoryCodes` à `null` : inchangés |
| `PATCH /inventory/procurement/supplier-categories/{code}/deactivate`, `/activate` | Désactivation, réactivation |
| `DELETE /inventory/procurement/supplier-categories/{code}` | Suppression d'une catégorie jamais utilisée ; utilisée par un fournisseur ou parente : `409`, la désactiver |
| `GET /inventory/procurement/suppliers/{supplierCode}/categories` | Catégories d'un fournisseur |
| `PUT /inventory/procurement/suppliers/{supplierCode}/categories` | Remplacement : `{categories: [{kind, code}]}` |
| `POST /inventory/procurement/suppliers/{supplierCode}/categories` | Ajout : `{kind, code}` (sans doublon) |
| `DELETE /inventory/procurement/suppliers/{supplierCode}/categories/{kind}/{code}` | Retrait |
| `GET /inventory/procurement/suppliers?categoryKind=&categoryCode=` | Liste filtrée (enrichie) |
| `GET /inventory/procurement/supplier-suggestions?natureCode=&inventoryCategoryCode=&supplierCategoryCode=&designation=&itemCode=&amount=&limit=` | Suggestion ; sans critère exploitable : `400` |
| `GET /fund-needs/{number}/supplier-suggestions?limit=` | Suggestions pour chaque ligne d'un état de besoin (droit `FUND_REQUEST:CREATE`) |
| `GET /fund-requests/{number}/supplier-suggestions?limit=` | Suggestions pour chaque ligne d'une demande de fonds (`FUND_REQUEST:CREATE`, et le contrôle de lecture de la demande) |
| `POST /accounting/purchases/suppliers/from-inventory/{inventoryCode}` | Fournisseur comptable d'une fiche d'inventaire, retrouvé ou créé (`SUPPLIER:MANAGE`) |

Réponse de suggestion : `{natureCode, natureLabel, natureGuessed, inventoryCategoryCode, supplierCategoryCode, itemCode, amount, limit, suggestions: [{rank, supplierCode, supplierName, phone, email, score, reasons, declared, historyCount, contractUnitPrice, leadTimeDays, categories}]}`. Par ligne : `[{lineNo, designation, natureCode, supplierCode, supplier, result}]`.

## Comptabilité (lot F13)

- `acc_supplier.inventory_supplier_id` ; `POST /accounting/purchases/suppliers` accepte `inventorySupplierId` (fiche existante, pas déjà reliée à un autre fournisseur comptable : `409`).
- `POST /accounting/purchases/suppliers/from-inventory/{inventoryCode}` : la fiche (active) donne son fournisseur comptable s'il existe ; sinon il est créé : code = code de la fiche s'il tient en 40 caractères et est libre, sinon `FRN-n` ; nom, NIU (= `tax_id`), adresse, téléphone, courriel copiés ; compte `401` et compte de retenue `447` par défaut (réglages), sans retenue.
- `POST /accounting/purchases/orders/from-need` : `supplierCode` devient facultatif. Absent : les lignes retenues doivent porter la même fiche fournisseur (`supplierCode`), dont le fournisseur comptable est repris (créé au besoin) ; aucune fiche, ou des fiches différentes, ou une ligne sans fiche : `400`. Présent : un code de fournisseur comptable, comme avant ; s'il n'existe pas en comptabilité mais qu'une fiche d'inventaire porte ce code, son fournisseur comptable est repris (créé au besoin). La création implicite se fait avec le droit de la commande (`PURCHASE_ORDER:MANAGE`).

## Tests

- `SupplierSuggestionServiceTest` (Mockito) : rattachement direct, puis catégorie fournisseur reliée (sous-catégorie comprise), puis historique seul ; fournisseur bloqué écarté ; raisons ; historique plafonné ; nature devinée et limite ; prix contractuel, moins cher, préféré, délai de l'article ; refus sans critère ou nature inconnue ; suggestions par ligne.
- `SupplierCategoryServiceTest` (Mockito) : codes vérifiés (nature, catégorie de stock, catégorie fournisseur inconnue ou désactivée) ; remplacement des catégories d'un fournisseur ; suppression refusée si utilisée ou parente ; cycle de parenté refusé ; code déduit du libellé sans accents ; sous-catégories à toute profondeur.
- `LinkedSupplierTest` : texte libre inchangé ; fiche active qui donne son nom ; fiche inconnue ou inactive refusée.
- `SupplierSuggestionIT` (Testcontainers, schéma de production) : référentiel de départ et pont vers la nature ; classement réel (nature directe, sous-catégorie d'une catégorie reliée, historique par code, historique par nom en texte libre), fournisseur inactif écarté ; nature devinée ; filtre par catégorie parente ; suppression refusée, désactivation.
- `PurchaseF13IT` : commande depuis un état de besoin dont la ligne porte une fiche, sans fournisseur indiqué : fournisseur comptable créé depuis la fiche (nom, NIU, `401`), puis retrouvé par `from-inventory`.

## Points à valider par l'exploitant

- Les catégories de départ et leur pont vers les natures (ajustables depuis l'écran du référentiel).
- Les poids du classement (100 / 70 / 60 / 45, historique 5 par usage plafonné à 50, prix 20 + 10 + 5, délai 10 - jours).
- L'historique par nom en texte libre pour les lignes anciennes (correspondance exacte du nom).
- La création implicite du fournisseur comptable lors d'une commande issue d'un état de besoin, avec le seul droit de commande.
