# Immobilisations et amortissements

Contrat exact de chaque route (paramètres, corps, réponse, champs JSON) : [routes.md](routes.md). Ce fichier explique le fonctionnement et ce qu'il faut à l'écran. Index du module : [README.md](README.md).

## Taux, modes et profils de marque

Demandé par l'exploitant : un ordinateur ne s'amortit pas en 36 mois linéaires, et la marque compte (un Mac ne se déprécie pas comme un autre ordinateur).

- **Taux annuel par catégorie** (`fa_category.annual_rate`, en %) : quand il est renseigné, la durée en découle (15 % donne 80 mois, 20 % donne 60 mois).
- **Matériel informatique par défaut** : dégressif SYSCOHADA au taux linéaire de 15 %, soit 6 ans et 8 mois. Le coefficient est 2,5 au-delà de 6 ans, donc le taux dégressif est de 37,5 %, avec bascule en linéaire quand celui-ci devient plus fort. Sur 900 000, cela donne 337 500 la première année et 210 938 la deuxième. Le taux et le mode se modifient par `POST /categories`.
- **Table des coefficients** (`accounting.fixed-asset.declining-coefficients`, défaut `3-4:1.5,5-6:2,7+:2.5`) : des tranches contiguës. Plus de 2 ans et jusqu'à 4 ans : 1,5. Plus de 4 ans et jusqu'à 6 ans : 2. Plus de 6 ans : 2,5. Avant correction, une durée de 6 ans et 8 mois recevait 2.
- **Profils d'amortissement** (`fa_depreciation_profile`) : une marque, et facultativement une catégorie et un motif de modèle (« MacBook »). Un profil fixe un mode, un taux ou une durée, un coefficient et une valeur résiduelle en % du coût, appliquée à la mise en service quand la fiche n'en porte pas.
- **Ordre d'application** : valeurs saisies sur la fiche, puis profil, puis catégorie. Entre plusieurs profils, le plus précis gagne (catégorie et modèle renseignés), puis la priorité la plus haute.
- **Portée** : un profil s'applique aux biens mis en service après sa création. Pour un bien déjà en service, on passe par une révision du plan.

Exemple vérifié par les tests : un profil Apple pour les « MacBook », linéaire à 20 % avec 20 % de valeur résiduelle. Un MacBook de 900 000 a une valeur résiduelle de 180 000 et amortit 720 000 sur 60 mois.

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
| `GET`, `POST /depreciation-profiles`, `PATCH /depreciation-profiles/{id}`, `POST /depreciation-profiles/{id}/deactivate`, `GET /{code}/depreciation-profile` | Profils de marque, profil retenu pour une fiche |
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

**Réglé** : la liste de clôture du lot F12 appelle ce contrôle (étape `FIXED_ASSETS`) et refuse la clôture tant qu'il reste un constat bloquant.

## Paramètres (`ACCOUNTING`, tous lus à chaud)

| Thème | Clés `accounting.fixed-asset.*` |
| --- | --- |
| Rythme et planification | `rhythm`, `depreciation-cron`, `controls-cron` |
| Calcul | `day-basis`, `declining-coefficients` |
| Journaux | `journal`, `sale-journal` |
| Comptes | `in-progress-account`, `supplier-account`, `disposal-receivable-account`, `derogatory-*`, `subsidy-income-account`, `insurance-*`, `vat-regularization-account` |
| Seuils et délais | `vat-regularization-years`, `impairment-second-validation`, `draft-alert-days`, `commission-alert-days`, `warranty-alert-days` |
| Règles | `inventory-required`, `manual-entry-guard`, `resource-axis` |

## Limites connues

- **Réglé** : biens en location-acquisition (note 3B). `POST /{code}/lease` rattache un bien à son emprunt du lot F17 (`fixed_asset.loan_code`, migrations dev `V317`, prod `V311`). Le bien passe en entrée `LEASE`, et l'état `note-3b` donne sa valeur brute, ses amortissements, sa valeur nette et la dette restante de l'emprunt.
- Coût par espace : seules les dotations sont comptées pour l'instant. L'entretien et l'assurance par ressource sont à ajouter.
- Page mobile de scan : le frontend la construit sur `POST /inventories/{code}/scans`. La photo est une référence de la GED (`photoRef`).
- Export Excel : les états sortent en CSV (ouvrable dans Excel) et en PDF.

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
