# Achats : fournisseurs, commandes, factures fournisseurs, règlements, suggestion de fournisseurs

Contrat exact de chaque route (paramètres, corps, réponse, champs JSON) : [routes.md](routes.md). Ce fichier explique le fonctionnement et ce qu'il faut à l'écran. Index du module : [README.md](README.md).

## Lot F13 : achats et fournisseurs

Critère d'acceptation du plan : **une commande validée engage le budget**. Vérifié par `PurchaseF13IT`.

### Fournisseurs

`acc_supplier` : code (`FRN-n` par défaut), nom, NIU, RCCM, adresse, téléphone, courriel, coordonnées bancaires, délai de paiement en jours (30), compte collectif (`401`), compte de charge par défaut, exonération de TVA, **retenue à la source** : taux (en %), compte (`447`), base (`HT` ou `TTC`), libellé ; statut actif ou inactif. Le code du fournisseur est son compte auxiliaire (tiers des lignes `401`).

### Commandes

- Saisie (`POST /purchases/orders`) ou **depuis un état de besoin validé** (`POST /purchases/orders/from-need` : toutes ses lignes chiffrées restantes, ou celles choisies), sans ressaisie ; les lignes de l'état de besoin sont marquées transformées et l'état passe `TRANSFORMED` ou `PARTIALLY_TRANSFORMED`.
- Numéro `BC-AAAAMM-0001`. Chaque ligne : désignation, quantité, prix unitaire HT, nature d'opération ou compte, taux de TVA (18 % par défaut, `accounting.purchase.default-vat-rate` ; 0 pour un fournisseur exonéré), section.
- Compte de charge : celui de la nature d'opération, sinon celui saisi, sinon celui par défaut du fournisseur.
- **Validation** (`PATCH /orders/{n}/validate`) par une **autre personne** que le préparateur : contrôle budgétaire (F16) sur les montants hors taxe, puis **engagement** (`acc_budget_commitment`, source `PURCHASE_ORDER`). Un dépassement d'une ligne bloquante est refusé (`409`), sauf dérogation tracée : droit `BUDGET:OVERRIDE` et motif dans le corps (`{ "reason": "..." }`).
- **Annulation** (`PATCH /orders/{n}/cancel`, motif obligatoire) tant qu'elle n'est pas facturée : l'engagement est libéré.
- Bon de commande imprimé (`GET /orders/{n}/pdf`), pièce numérotée `BC` du lot F7.
- États : `DRAFT`, `VALIDATED`, `PARTIALLY_INVOICED`, `INVOICED`, `CANCELLED`.

### Factures et avoirs fournisseurs

- Saisie en brouillon (`POST /purchases/invoices`) : fournisseur, **numéro de la facture du fournisseur** (un même numéro pour un même fournisseur est refusé en `409`), date, échéance (par défaut date + délai du fournisseur), commande liée (sans lignes, la facture reprend le reste à facturer de la commande), lignes, référence du document (GED). Numéro interne `FF-AAAAMM-0001` (avoirs `AF-`).
- TVA récupérable par ligne : `4452` sur achats, `4454` sur services extérieurs (comptes `62`, `63`), `4451` sur immobilisations (classe `2`) ; code de taxe `TVA18` et base sur la ligne (déclaration F11).
- **Retenue à la source** : taux du fournisseur × base (HT ou TTC), ou montant saisi ; net à payer = TTC − retenue.
- **Comptabilisation** (`POST /invoices/{n}/post`) au journal `AC` (`accounting.purchase.journal`) : D charges HT (par section) ; D TVA récupérable ; C `401` au nom du fournisseur pour le net à payer, avec l'échéance ; C `447` pour la retenue (code `RAS`). Sur commande : les montants facturés sont reportés sur la commande ; entièrement facturée (ou facture marquée `closeOrder`), la commande passe `INVOICED` et l'engagement budgétaire est **réalisé** pour le montant facturé (l'écart est libéré).
- **Avoir** (`kind = CREDIT_NOTE`, facture d'origine obligatoire) : écriture inverse, référence de lettrage de la facture d'origine ; il diminue le reste à payer de la facture et se lettre avec elle.
- Un brouillon se supprime (`DELETE /invoices/{n}`) ; une facture comptabilisée se corrige par un avoir.
- États : `DRAFT`, `POSTED`, `PARTIALLY_PAID`, `PAID`.

### Règlements et lettrage

- `POST /invoices/{n}/pay` (montant, par défaut le reste à payer ; date ; compte et journal de trésorerie ; référence) : D `401` ; C trésorerie ; le compte fournisseur est **lettré** par le numéro interne de la facture dès qu'elle est soldée.
- Le lettrage automatique de nuit (F4) couvre aussi le `401`.
- Paiement depuis la caisse : la nature « Paiement fournisseur » de la caisse reste disponible pour les petites factures.

### États

- **Échéancier** (`GET /purchases/payables?until=`) : factures à payer par date d'échéance, reste à payer, jours de retard.
- **Relevé fournisseur** (`GET /suppliers/{code}/statement?from=&to=`) : grand livre auxiliaire du `401`, solde courant (créditeur = dû au fournisseur).
- **Retenues à la source du mois** (`GET /purchases/withholdings?period=AAAA-MM`, export CSV `/withholdings/export`) : par facture, fournisseur, NIU, base, taux, retenue, compte ; total à reverser ; mouvement du compte de retenue sur le mois. Le reversement à l'État se passe par la banque : D `447` ; C banque.

### Routes

Préfixe `/api/v1/accounting/purchases`. Écritures auditées.

| Route | Droit |
| --- | --- |
| Toutes les lectures (`GET`) | `PURCHASE:READ` |
| `POST /suppliers`, `POST /suppliers/from-inventory/{code}` | `SUPPLIER:MANAGE` |
| `POST /orders`, `/orders/from-need` | `PURCHASE_ORDER:MANAGE` |
| `PATCH /orders/{n}/validate`, `/orders/{n}/cancel` | `PURCHASE_ORDER:VALIDATE` |
| `POST /invoices`, `/invoices/{n}/post`, `DELETE /invoices/{n}` | `SUPPLIER_INVOICE:MANAGE` |
| `POST /invoices/{n}/pay` | `SUPPLIER_PAYMENT:MANAGE` |

Rôles : `SUPER_ADMIN`, `ADMIN` et `FINANCE` tout ; `MANAGER` la lecture, la préparation et la validation des commandes.

### Suggestion de fournisseurs (complément)

Voir [finance-suggestion-fournisseurs](../../backend/finance-suggestion-fournisseurs/README.md). La fiche fournisseur de référence est celle de l'inventaire ; `acc_supplier` gagne `inventory_supplier_id` (facultatif, unique) :
- `POST /suppliers` accepte `inventorySupplierId` ; `POST /suppliers/from-inventory/{code}` retrouve ou crée le fournisseur comptable d'une fiche d'inventaire (nom, NIU, adresse, téléphone, courriel copiés ; `401` et `447` par défaut) ;
- `POST /orders/from-need` : `supplierCode` devient facultatif ; absent, la fiche fournisseur portée par les lignes retenues (toutes la même) donne le fournisseur de la commande ; un code de fiche d'inventaire est aussi accepté.

### Paramètres (domaine Comptabilité)

`accounting.purchase.journal` (`AC`), `accounting.purchase.supplier-account` (`401`), `accounting.purchase.withholding-account` (`447`), `accounting.purchase.vat-account` (`4452`), `accounting.purchase.vat-account-services` (`4454`), `accounting.purchase.vat-account-fixed-assets` (`4451`), `accounting.purchase.default-vat-rate` (`18`), `accounting.purchase.analytic-axis` (vide : ventilation par défaut des comptes).

### Hors de ce lot

- Fiche d'immobilisation et plan d'amortissement depuis une facture (lot F18) : la facture d'un bien en classe 2 est écrite, sa TVA va en `4451`, la fiche viendra avec F18.
- Factures non parvenues (`408`) en clôture : lot F20 (rattachement).
- Retenue à la source sur un décaissement de caisse direct : passer par une facture fournisseur.

### Pour le frontend

- **Fournisseurs** : liste, fiche (coordonnées, NIU, délai, compte par défaut, exonération, retenue à la source), relevé.
- **Commandes** : liste par état ; création manuelle ou « Créer une commande » depuis un état de besoin validé (choix du fournisseur et des lignes) ; fiche avec lignes et totaux ; « Valider » (masqué pour le préparateur ; en cas de `409` budget, proposer la dérogation avec motif à un détenteur de `BUDGET:OVERRIDE`) ; « Annuler » (motif) ; « Imprimer le bon de commande ».
- **Factures fournisseurs** : saisie (depuis une commande : lignes reprises), contrôle de doublon (`409`), totaux HT, TVA, TTC, retenue, net à payer ; « Comptabiliser » ; avoir depuis une facture ; « Régler » (montant, date, compte et journal) ; historique des règlements.
- **États** : échéancier (retards en rouge), retenues du mois avec export.

## Suggestion de fournisseurs (complément du lot F13)

### Constat

- Demande de l'exploitant : les fournisseurs proposés pour les demandes sont ceux du **module inventaire** (`inventory_supplier`) ; un module de suggestion enrichit la fiche actuelle **sans la casser** et propose, selon la catégorie du besoin, des fournisseurs qui peuvent appartenir à une ou plusieurs catégories. Complément : un **référentiel propre de catégories de fournisseurs**, relié aux natures d'opération.
- La catégorie d'une ligne de besoin (état de besoin `fund_need_line`, demande de fonds `fund_request_line`, justification `expense_justification_line`) est sa **nature d'opération** (`nature_code`, table `cash_operation_nature`), devinée depuis la désignation quand elle n'est pas saisie.
- Ces lignes ne portaient qu'un fournisseur en texte libre (`supplier`), sans lien avec une fiche.
- Le fournisseur comptable du lot F13 (`acc_supplier`) n'était pas relié à la fiche d'inventaire.

### Modèle

| Table | Rôle |
| --- | --- |
| `inventory_supplier_category_ref` | Référentiel des catégories de fournisseurs : `code` unique, `label`, `description`, `parent_id` (sous-catégories, sans cycle), `active`, `sort_order`, `created_by/at`, `updated_by/at`. |
| `inventory_supplier_category_ref_target` | Pont : une catégorie fournisseur est reliée à des **natures d'opération** (`NATURE`) et à des **catégories de stock** (`INVENTORY_CATEGORY`). Unique sur (catégorie, sorte, code). |
| `inventory_supplier_category` | Catégories d'un fournisseur : `category_kind` = `NATURE`, `INVENTORY_CATEGORY` ou `SUPPLIER_CATEGORY`, `category_code`. Unique sur (fournisseur, sorte, code). Codes vérifiés à l'écriture (nature existante, catégorie de stock existante, catégorie fournisseur existante et active). |
| `fund_need_line.supplier_code`, `fund_request_line.supplier_code`, `expense_justification_line.supplier_code` | Fiche fournisseur de la ligne, facultative. |
| `acc_supplier.inventory_supplier_id` | Fiche d'inventaire du fournisseur comptable, facultative, unique (index partiel). |

Catégories de départ (migration) : Fournitures de bureau, Informatique et téléphonie, Entretien et nettoyage, Restauration, café et traiteur, Travaux et maintenance, Mobilier et aménagement, Énergie et eau, Prestations de services, Transport et logistique, Communication et impression, Sécurité et gardiennage. Pont posé vers les natures existantes : `ACHAT_FOURNITURES_BUREAU` (fournitures), `TELECOMMUNICATIONS` et `ACHAT_PETIT_MATERIEL` (informatique), `ACHAT_PRODUITS_ENTRETIEN` (entretien), `ACHAT_CONSOMMABLES_ESPACES` (restauration), `PETITES_REPARATIONS` (travaux), `ACHAT_PETIT_MATERIEL` (mobilier), `TRANSPORT_COURSE` et `AFFRANCHISSEMENT` (transport). Une nature absente de la base est ignorée.

### Fiche fournisseur enrichie, sans rupture

- `SupplierRequest` (création `POST /suppliers`, modification `PUT /suppliers/{code}`) gagne `categories` (facultatif : liste de `{kind, code}`). Absent (`null`) : les catégories restent inchangées ; liste vide : toutes retirées.
- `SupplierResponse` gagne `categories` : `[{kind, code, label, active}]`, le libellé étant celui de la nature, de la catégorie de stock ou de la catégorie fournisseur.
- `GET /suppliers` accepte `categoryKind` et `categoryCode`. Sans `categoryKind`, le filtre porte sur le référentiel des catégories fournisseurs ; une catégorie parente englobe ses sous-catégories. Sans `categoryCode`, la liste est celle d'avant.

### Lignes reliées aux fiches, sans rupture

- `supplierCode` facultatif sur les lignes d'état de besoin (`NeedLineRequest`), de demande de fonds (`RequestLine`) et de justification (`ExpenseInput`). Fourni : la fiche doit exister et être **active** (`400` « Ligne n : fournisseur inconnu » ou « n'est pas actif ») ; son nom remplit `supplier`. Absent : le texte libre `supplier` est accepté comme avant.
- Les réponses (`NeedLine`, `RequestLine`, `ExpenseLine`) renvoient `supplierCode`.
- La transformation d'un état de besoin en demande de fonds et la duplication d'une demande reprennent `supplierCode`.
- La meilleure suggestion n'est pas ajoutée aux réponses des lignes (coût d'une requête par ligne à chaque lecture) : la route de suggestion par état de besoin ou par demande suffit.

### Règles de classement

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

### Routes

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

### Comptabilité (lot F13)

- `acc_supplier.inventory_supplier_id` ; `POST /accounting/purchases/suppliers` accepte `inventorySupplierId` (fiche existante, pas déjà reliée à un autre fournisseur comptable : `409`).
- `POST /accounting/purchases/suppliers/from-inventory/{inventoryCode}` : la fiche (active) donne son fournisseur comptable s'il existe ; sinon il est créé : code = code de la fiche s'il tient en 40 caractères et est libre, sinon `FRN-n` ; nom, NIU (= `tax_id`), adresse, téléphone, courriel copiés ; compte `401` et compte de retenue `447` par défaut (réglages), sans retenue.
- `POST /accounting/purchases/orders/from-need` : `supplierCode` devient facultatif. Absent : les lignes retenues doivent porter la même fiche fournisseur (`supplierCode`), dont le fournisseur comptable est repris (créé au besoin) ; aucune fiche, ou des fiches différentes, ou une ligne sans fiche : `400`. Présent : un code de fournisseur comptable, comme avant ; s'il n'existe pas en comptabilité mais qu'une fiche d'inventaire porte ce code, son fournisseur comptable est repris (créé au besoin). La création implicite se fait avec le droit de la commande (`PURCHASE_ORDER:MANAGE`).

### Points à valider par l'exploitant

- Les catégories de départ et leur pont vers les natures (ajustables depuis l'écran du référentiel).
- Les poids du classement (100 / 70 / 60 / 45, historique 5 par usage plafonné à 50, prix 20 + 10 + 5, délai 10 - jours).
- L'historique par nom en texte libre pour les lignes anciennes (correspondance exacte du nom).
- La création implicite du fournisseur comptable lors d'une commande issue d'un état de besoin, avec le seul droit de commande.
