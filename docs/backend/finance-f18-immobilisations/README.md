# Lot F18 : immobilisations et amortissements

Spécification : `docs/backend/plan-finance-comptabilite/immobilisations-et-amortissements.md`. Branche `feature/finance-comptabilite`.

## Migrations

- Dev `V313__immobilisations_amortissements_f18.sql`, prod `V307__immobilisations_amortissements_f18.sql`, même contenu.
- Tables : `fa_category` (14 catégories de départ, section 2), `fixed_asset_counter`, `fixed_asset`, `fixed_asset_value_line`, `fixed_asset_inventory_link`, `fixed_asset_plan`, `fixed_asset_schedule`, `fixed_asset_meter_reading`, `fixed_asset_event`, `fixed_asset_disposal`, `fixed_asset_impairment`, `fixed_asset_maintenance`, `fixed_asset_inventory_campaign`, `fixed_asset_inventory_scan`.
- `acc_supplier_invoice.payable_account`, `acc_supplier_invoice_line.quantity` et `fixed_asset_code` relient une facture fournisseur (lot F13) à ses fiches.
- Comptes ajoutés au plan (`481`, `485` et voisins).
- Permissions `FIXED_ASSET:READ`, `MANAGE`, `REVISE`, `IMPAIR`, `DISPOSE`, `DEPRECIATION_RUN`, `INVENTORY` et `FIXED_ASSET_SETTINGS:MANAGE`.

## Services

| Classe | Rôle |
| --- | --- |
| `FixedAssetService` | Catégories ; fiches créées à la main ou depuis une facture fournisseur (`onInvoicePosted`) ; composants ; immobilisations en cours ; mise en service et PV `PMS` ; plans linéaire, dégressif et par unités d'œuvre avec versions ; révision ; transfert, affectation et restitution avec fiche de remise `PRM` ; entretien ; état d'usage ; relevés d'unités ; lien avec les actifs de l'inventaire ; reclassement ; régularisation de la TVA déduite ; dépréciation et reprise, avec seconde validation au-delà du seuil ; sortie préparée puis validée par une seconde personne, PV `PSI`, cession au journal des ventes, encaissement lettré sur `485`, indemnité d'assurance ; garde des saisies directes sur les comptes 2, 28 et 29 |
| `DepreciationRunService` | Dotations d'une période : simulation, passage, rattrapage d'une période verrouillée, dérogatoire, reprise des subventions. Ventilées par section analytique. Relancer une période ne double rien. |
| `FixedAssetReportService` | États de la section 11, contrôles quotidiens et liste de clôture (section 12), fiche imprimable, exports CSV et PDF |
| `FixedAssetInventoryService` | Campagnes d'inventaire physique : périmètre, équipe, scan, écarts, clôture avec PV `PIP` ; planche d'étiquettes QR (38 x 21 mm, 65 par A4) |
| `DepreciationCalculator` | Calculs sans état : prorata au jour (30/360 ou jours réels), dégressif avec bascule en linéaire, arrondi au franc sur le cumul, plus ou moins-value, régularisation de la TVA |
| `FixedAssetWorker` | Dotations automatiques au rythme choisi : mensuel le 1er à 3 h ; trimestriel ou annuel au début de la période. Contrôles quotidiens à 6 h 30. Un écart bloquant est signalé aux comptables. Planifications relues à chaud. |

## Écritures

Celles de la section 10 du plan :
- facture d'immobilisation : D compte du bien, D `4451`, C `481` ;
- mise en service d'un en-cours ;
- dotation : D `6812` ou `6813`, C `28x`, ventilée par section ;
- dérogatoire `851` / `151` / `861` ;
- dépréciation et reprise `691x` / `29x` / `791x` ;
- reclassement ;
- sortie : D `28x`, D `29x`, D `811` ou `812`, C compte du bien ;
- cession : D `485`, C `821` ou `822`, C `4431` ;
- encaissement lettré sur `485`.

Une fiche saisie à la main avec une contrepartie sur un compte de tiers (`481`...) porte le fournisseur (`supplierCode`) comme tiers. Sans fournisseur, la saisie est refusée avec un message clair.

## Routes (`/sni/api/v1/accounting/fixed-assets`)

| Route | Usage |
| --- | --- |
| `GET`, `POST /categories` | Catégories |
| `GET ?status=&category=&site=&resource=&holder=` | Fichier filtré |
| `POST`, `GET /{code}`, `PATCH /{code}` | Créer, lire, compléter |
| `POST /{code}/components` | Découper en composants |
| `POST /{code}/commission`, `GET /{code}/commission.pdf` | Mise en service, PV `PMS` |
| `GET /{code}/plan?kind=` | Plan et versions |
| `POST /{code}/revisions`, `/reclassifications` | Révision, reclassement |
| `POST /{code}/transfers`, `/assignments`, `/returns`, `GET /{code}/handover.pdf` | Transfert, affectation, restitution, fiche de remise |
| `POST /{code}/maintenance`, `/operational-status`, `/meter-readings`, `/inventory-links`, `/vat-regularizations` | Entretien, état d'usage, unités d'œuvre, lien inventaire, TVA |
| `POST /{code}/impairments`, `POST /impairments/{id}/decision` | Dépréciation, reprise, seconde validation |
| `POST /{code}/disposals`, `POST /disposals/{id}/validate`, `/cancel`, `/collections`, `/indemnities`, `GET /disposals/{id}/pdf` | Sortie et PV `PSI` |
| `GET /{code}/sheet.pdf`, `/label.pdf`, `POST /labels` | Fiche, étiquette, planche d'étiquettes |
| `GET /depreciation-runs/preview?period=`, `POST /depreciation-runs?period=` | Simulation, passage des dotations |
| `GET`, `POST /inventories`, `GET /inventories/{code}`, `POST /inventories/{code}/scans`, `/close`, `GET /inventories/{code}/pdf` | Inventaire physique, PV `PIP` |
| `GET /reports/{report}?year=&date=&format=json\|csv\|pdf` | États |
| `GET /controls?date=`, `GET /closing-check?year=` | Contrôles quotidiens, liste de clôture |

États disponibles :

| Code | Contenu |
| --- | --- |
| `registry` | Fichier à une date |
| `note-3a` | Tableau des immobilisations |
| `note-3c` | Tableau des amortissements |
| `note-3d` | Plus et moins-values |
| `impairments` | Dépréciations |
| `forecast` | 12 mois et 5 exercices |
| `fully-depreciated` | Biens totalement amortis encore en service |
| `pending` | Biens en attente de mise en service et en cours |
| `warranties` | Garanties et contrats arrivant à échéance |
| `holders` | Biens par détenteur |
| `reconciliation` | Fichier et comptes `2x`, `28x`, `29x` |
| `cost-by-resource` | Dotations par espace |

## Contrôles et clôture

Contrôles quotidiens :
- écart entre le fichier et les comptes (bloquant) ;
- fiche à compléter depuis plus de 15 jours ;
- bien non mis en service depuis plus de 30 jours ;
- dotations non comptabilisées ;
- prix de cession non encaissé.

La liste de clôture (`closing-check`) exige :
- toutes les dotations de l'exercice passées ;
- un écart nul entre le fichier et les comptes ;
- une campagne d'inventaire close avec son PV dans l'exercice (paramètre `accounting.fixed-asset.inventory-required`) ;
- des en-cours justifiés.

La clôture d'exercice (lot F12) appellera cette liste et refusera tant qu'elle n'est pas vide.

## Paramètres (`ACCOUNTING`, tous lus à chaud)

| Thème | Clés `accounting.fixed-asset.*` |
| --- | --- |
| Rythme et planification | `rhythm`, `depreciation-cron`, `controls-cron` |
| Calcul | `day-basis`, `declining-coefficients` |
| Journaux | `journal`, `sale-journal` |
| Comptes | `in-progress-account`, `supplier-account`, `disposal-receivable-account`, `derogatory-*`, `subsidy-income-account`, `insurance-*`, `vat-regularization-account` |
| Seuils et délais | `vat-regularization-years`, `impairment-second-validation`, `draft-alert-days`, `commission-alert-days`, `warranty-alert-days` |
| Règles | `inventory-required`, `manual-entry-guard`, `resource-axis` |

## Non fait dans ce lot

- Biens en location-acquisition (note 3B) : ils relèvent du lot F17. La table des emprunts ne porte pas encore de lien vers une fiche ; l'état sera ajouté quand ce lien existera.
- Coût par espace : seules les dotations sont comptées pour l'instant. L'entretien et l'assurance par ressource sont à ajouter.
- Page mobile de scan : le frontend la construit sur `POST /inventories/{code}/scans`. La photo est une référence de la GED (`photoRef`).
- Export Excel : les états sortent en CSV (ouvrable dans Excel) et en PDF.

## Tests

- `DepreciationCalculatorTest` : 36 dotations de 25 000, prorata sans dérive, dégressif, coefficients, plus ou moins-value.
- `FixedAssetF18IT`, sur le schéma de production. Il vérifie le critère d'acceptation du plan :
  - un ordinateur de 900 000 HT est mis en service le 1er avril ;
  - la dotation d'avril vaut 25 000, et relancer avril ne double rien ;
  - la cession à 825 000 après deux dotations donne une moins-value de 25 000 ;
  - celui qui prépare la sortie ne la valide pas ;
  - la note 3D sort en CSV et en PDF ;
  - les comptes `2442` et `28442` sont rapprochés ;
  - une campagne d'inventaire signale le bien non vu, exige une décision et produit son PV ;
  - les étiquettes s'impriment.
- Suite unitaire complète : 2201 tests ; tests d'intégration finance : 55.

## Pour le frontend

Écrans à prévoir :
- fichier des immobilisations, avec ses filtres ;
- fiche d'un bien, avec onglets valeurs, plan, historique, documents et actions selon les droits ;
- mise en service ;
- sortie en deux temps : préparer, puis valider par une autre personne ;
- dotations : simulation puis passage ;
- inventaire, avec une page mobile de scan de QR ;
- états, avec export CSV et PDF ;
- contrôles.

Les actions renvoient la fiche complète (`AssetView`) à substituer à l'affichage. Les droits sont contrôlés par le serveur (`403` avec le droit manquant).
