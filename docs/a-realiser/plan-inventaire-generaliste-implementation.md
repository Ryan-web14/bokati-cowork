# Plan d'implémentation · Inventaire généraliste

> Plan d'exécution pour combler les manques identifiés en **partie 2** de
> [`plan-inventaire-btp.md`](plan-inventaire-btp.md), sur le module `features/inventory` existant
> décrit en partie 1 du même document.
>
> **Le périmètre BTP est explicitement exclu.** Chantiers, sorties de matériel nominatives, caisses à
> outils, flotte, location et leasing d'équipement font l'objet d'un projet de développement distinct,
> dans un autre dépôt. Ce plan ne construit que le socle d'inventaire généraliste.

---

## 1. Périmètre

### Dans le périmètre

Les sections 2.1 à 2.18 du document de référence, regroupées en dix lots :

| Lot | Objet | Sections couvertes |
|---|---|---|
| 0 | Durcissement du socle | 2.17 |
| 1 | Référentiel article et identification | 2.9, 2.3 |
| 2 | Valorisation et comptabilité matière | 2.1 |
| 3 | Traçabilité et qualité | 2.2, 2.10 |
| 4 | Inventaires physiques | 2.5, 2.14 |
| 5 | Achats avancés | 2.4, 2.12 |
| 6 | Opérations d'entrepôt | 2.6, 2.13 |
| 7 | Planification et prévision | 2.11 |
| 8 | Flux tiers, propriété et divers | 2.15, 2.8 |
| 9 | Recherche, interopérabilité et pilotage | 2.7, 2.16, 2.18 |

### Hors périmètre, renvoyé au projet BTP

`Worksite`, `ManagementProfile` et les six paliers, `MaterialIssue` et la chaîne de détention,
`Container` et les caisses, `EquipmentLease`, `FleetVehicle`, `WorkOrder`, `PlantRollCall`,
`MaterialRequisition`, cuves de carburant, certificats matière et essais, déchets, emballages
consignés, EPI et certifications.

### Règle de frontière

Le projet BTP consommera ce socle. Chaque lot doit donc laisser des **points d'extension** propres
plutôt que des impasses, sans pour autant implémenter le BTP par anticipation. Trois principes :

1. Ne pas coder en dur d'énumération fermée là où le BTP devra ajouter des valeurs
   (`StockReferenceType`, `StockOutReasonCode`, `InventoryLocationType`, `AssetAssigneeType`).
2. Exposer les opérations de stock comme des **services applicatifs réutilisables**, sur le modèle
   de l'`InventoryConsumptionService` existant, et non uniquement comme des contrôleurs HTTP.
3. Garder l'imputation analytique **générique** : un mouvement porte déjà `referenceType` et
   `referenceCode`. Ne pas y ajouter de colonne métier spécifique.

---

## 2. État de départ vérifié

Ces points ont été contrôlés dans le code et corrigent certaines hypothèses initiales.

| Point | État réel |
|---|---|
| Concurrence sur `StockLevel` | **déjà traité** : `@Version` sur l'entité et `findByItemAndLocationForUpdate` en `PESSIMISTIC_WRITE`. Même chose sur `StockLot`, `StockReservation`, `Asset`, `InventoryCount`, `PurchaseOrder`, `PurchaseRequest`, `StockTransferWorkflow` |
| Idempotence | **déjà câblée** : `@Idempotent` sur les 13 contrôleurs du module, dont les 5 endpoints de mouvement. Tous en `required = false` |
| CMUP | **implémenté** dans `StockServiceImpl` (entrée, transfert, ajustement) |
| Contre-passation | **implémentée** : `reversed`, `reversalOfMovement`, `reversedAt`, `reversedBy`, `reversalReason` |
| Elasticsearch | **infrastructure seule** : déclaré dans `compose.yaml` sur le port 9200, aucune dépendance `pom.xml`, aucune configuration `application.yml`, aucune référence Java. Le module `ressource` est lui aussi en JPA Specification |
| Paquets `integration/` et `search/` | prévus au design initial, **jamais créés** |
| `InventoryBarcode` | prévu au design initial, **jamais créé** |
| Tests | 2 classes de test sur le module (`InventoryItemServiceImplTest`, `InventoryImportServiceImplTest`) pour 54 classes de test dans le projet. La couverture du module est le principal risque de régression |

Conséquence directe : le lot 0 **vérifie et durcit** un socle déjà largement en place, il ne le
construit pas. Et la recherche Elasticsearch (lot 9) est un chantier complet, pas un branchement.

---

## 3. Règles transverses, applicables à chaque lot

Ces règles ne sont pas négociables lot par lot. Elles viennent de `CLAUDE.md` et des conventions
observées dans le code.

**Structure.** Chaque nouveau sous-domaine respecte le découpage `controller/`, `dto/request/`,
`dto/response/`, `mapper/interfaces/`, `mapper/decorator/`, `model/`, `repository/`,
`service/interfaces/`, `service/implementation/`, `enums/`.

**Migrations.** Deux dossiers Flyway avec des numérotations distinctes :
`src/main/resources/db/migration` (dev) et `src/main/resources/db/migration-prod` (prod). Toute
modification de schéma exige **un fichier dans chacun**, avec le numéro propre à chaque série.
`hibernate.ddl-auto` reste à `none`. Aucun remplacement automatique de caractères sur ces fichiers.

**Enums.** Toute valeur ajoutée à un enum persisté en `STRING` impose de mettre à jour la contrainte
`CHECK` correspondante, dans une migration, dans les deux dossiers.

**Codes métier.** Aucun identifiant fonctionnel généré à la main : passage obligatoire par
`SequenceGeneratorFacade`, avec déclaration de la séquence en migration.

**Idempotence.** Tout nouvel endpoint mutant porte `@Idempotent(operation = "...", required = false)`,
nommé sur le motif `INVENTORY_<DOMAINE>_<ACTION>`.

**Sécurité.** `AdminApiAuthorizationManager` mappe `/inventory` sur la ressource `INVENTORY`. Les
nouvelles ressources RBAC sont introduites au lot où elles deviennent nécessaires, avec la migration
de permissions correspondante.

**Événements.** Publication via l'outbox, jamais d'appel direct à un service externe dans la
transaction.

**Montants.** Entiers `BIGINT` en XAF, conformément à V41. Le multi-devise du lot 5 est la seule
exception, et il ajoute un code devise plutôt que de changer le type.

**Tests.** Chaque lot livre ses tests unitaires de service et, pour les lots 2, 3, 4 et 6, des tests
d'intégration sur la cohérence du stock. Un lot sans test n'est pas terminé.

**Listes de valeurs servies par le backend.** Tout champ dont le domaine de valeurs est connu du
serveur doit être une sélection, jamais une saisie libre. Chaque lot qui introduit une énumération
l'ajoute au registre `InventoryReferenceServiceImpl`, avec son libellé français dans
`InventoryReferenceLabels`, et l'expose sous `/inventory/reference/enums/{groupe}`. Chaque lot qui
introduit un référentiel vivant (motifs d'ajustement, stratégies d'allocation, plans de contrôle,
fournisseurs, emplacements) l'expose sous `/inventory/reference/data`. Le front ne compose jamais ses
propres listes : il consomme celles du backend, ce qui garantit que les valeurs envoyées sont
toujours valides et qu'un ajout côté serveur apparaît sans livraison côté client.

**Automatisation par défaut.** À chaque fois qu'une valeur peut être calculée, proposée ou
pré-remplie par le serveur, elle doit l'être, l'utilisateur ne faisant que confirmer ou corriger.
Cela vaut notamment pour : le coût unitaire proposé depuis le contrat fournisseur puis le dernier
prix connu (lot 5), l'emplacement de rangement proposé par les règles de put-away (lot 6), le lot à
prélever proposé par la stratégie d'allocation (lot 6), la quantité à commander calculée (lot 7),
les quantités théoriques pré-remplies dans un comptage (lot 4), et le motif d'ajustement proposé
selon le contexte (lot 2). Un formulaire du module doit se remplir majoritairement par sélection et
pré-remplissage, la saisie libre restant l'exception documentée.

**Rédaction.** Pas de tiret cadratin, ni dans le code, ni dans les messages de commit.

---

## 4. Lot 0 · Durcissement du socle

> **État : livré.** Détail des contrôles à passer en recette dans
> [`../backend/inventory-lot0-verification.md`](../backend/inventory-lot0-verification.md).

**Objectif.** Rendre le journal de stock inattaquable et vérifiable avant d'empiler des
fonctionnalités dessus. C'est le lot qui protège tous les suivants.

### Ce qui a été livré

| Élément | Emplacement |
|---|---|
| Triggers d'immuabilité et d'interdiction de suppression | `db/migration/V216__inventory_stock_movement_immutability.sql` et `db/migration-prod/V212__...` |
| Service de réconciliation et export CSV | `stock/service/interfaces/StockReconciliationService`, `stock/service/implementation/StockReconciliationServiceImpl` |
| Requêtes de réconciliation | `StockLevelRepository.findReconciliationDivergences`, `countReconciliationPairs` |
| Contrôle hebdomadaire et alerte | `InventoryDailyWorker.runStockReconciliationCheck`, `InventoryAlertType.STOCK_LEVEL_DIVERGENCE` |
| Rapport de dérogations et export CSV | `InventoryAdminService.overrideReport`, `StockMovementRepository.findOverrideMovements`, `findOverrideBreakdown` |
| Idempotence pilotable par configuration | `core/idempotency/aop/IdempotencyRequirementPolicy`, propriété `idempotency.required-operations` |
| Finder en lecture seule explicite | `StockLevelRepository.findByItemAndLocationReadOnly` |
| Listes de valeurs servies par le backend | `features/inventory/reference/` |
| Configuration | `application.yml`, sections `inventory.worker` et `idempotency` |
| Tests | `StockReconciliationServiceImplTest`, `IdempotencyRequirementPolicyTest`, `InventoryReferenceServiceImplTest` |

Endpoints ajoutés :

```text
GET /inventory/admin/reconciliation            GET /inventory/admin/reconciliation.csv
GET /inventory/admin/reports/overrides         GET /inventory/admin/reports/overrides.csv
GET /inventory/reference/enums                 GET /inventory/reference/enums/{group}
GET /inventory/reference/enums/groups          GET /inventory/reference/data
```

### Écarts par rapport au plan initial

1. **Concurrence et idempotence étaient déjà en place**, contrairement à l'hypothèse de départ. Le
   lot les vérifie et les durcit au lieu de les construire, ce qui l'a allégé.
2. **L'immuabilité applicative n'a pas été doublée d'un garde-fou dans le code.** Le trigger de base
   est la garantie, et aucun chemin applicatif ne modifie un mouvement hors contre-passation. Ajouter
   un `@PreUpdate` aurait dupliqué la règle sans la renforcer.
3. ~~Les tests de charge et les tests d'immuabilité restent manuels.~~ **Résolu** avant le lot 2 :
   le socle Testcontainers (`PostgresIntegrationTestBase`) automatise les trois contrôles qui
   restaient manuels, via `StockMovementImmutabilityIT`, `StockReconciliationQueryIT` et
   `StockLevelConcurrencyIT`.
4. **Aucune réparation automatique des divergences n'a été implémentée**, délibérément : une
   divergence signale un défaut à analyser, l'écraser en silence ferait disparaître le symptôme sans
   traiter la cause. La correction passe par un ajustement explicite et tracé.

### Contenu

1. **Immuabilité de `stock_movement`.** Contrainte en base interdisant `UPDATE` et `DELETE` sur les
   colonnes métier (règle PostgreSQL ou déclencheur), les seules colonnes modifiables étant celles de
   la contre-passation. Doublée d'une interdiction applicative dans le repository.
2. **Commande de réconciliation.** Service `StockReconciliationService` recalculant
   `StockLevel.quantityOnHand` à partir de la somme des mouvements non contre-passés, comparant au
   stocké et produisant un rapport des divergences. Exposé en endpoint admin et exécutable en tâche
   planifiée hebdomadaire.
3. **Audit des chemins d'écriture.** Vérifier que toute écriture sur `StockLevel` passe par
   `findByItemAndLocationForUpdate` et non par `findByItemAndLocation`. Interdire le second usage en
   écriture.
4. **Tests de charge.** Scénario de sorties simultanées sur un même couple article et emplacement,
   validant l'absence de stock faux sous concurrence.
5. **Durcissement de l'idempotence.** Paramètre de configuration permettant de basculer les cinq
   endpoints de mouvement en `required = true`. Livré en `false` par défaut, pour ne pas casser les
   clients existants, avec une note de migration d'API.
6. **Rapport de dérogations.** `allowNegativeOverride` existe déjà sur `StockMovement` et le rapport
   d'anomalies le compte. Ajouter le détail : auteur, motif, article, emplacement, date, et le rendre
   exportable.

### Livrables techniques

```text
core ou inventory/stock/service/
  StockReconciliationService, StockReconciliationServiceImpl
inventory/admin/
  endpoints /reconciliation et /reports/overrides
migration : inventory_movement_immutability
```

### Critères de fin

- une tentative d'`UPDATE` direct sur un mouvement échoue en base ;
- la réconciliation sur un jeu de données de démonstration remonte zéro divergence ;
- le test de charge passe sur 100 sorties concurrentes ;
- le rapport de dérogations est consultable et exportable.

**Dépendances.** Aucune. **Ordre de grandeur.** 5 à 8 jours.

---

## 5. Lot 1 · Référentiel article et identification

> **État : livré.** Migrations `V217` (dev) et `V213` (prod).

**Objectif.** Donner au catalogue la profondeur attendue d'un référentiel, et rendre tout objet
identifiable par scan. C'est le socle de la mobilité et des lots 4 et 6.

### Ce qui a été livré

| Élément | Emplacement |
|---|---|
| Cycle de vie article | `ItemLifecycleStatus`, `InventoryItem.lifecycleStatus`, garde-fous dans `StockServiceImpl` |
| Codes-barres multiples | `InventoryBarcode`, `InventoryBarcodeType`, `InventoryIdentificationService` |
| Conditionnements | `InventoryPackaging`, `InventoryPackagingLevel` |
| Caractéristiques physiques | `weightKg`, `volumeM3`, `lengthMm`, `widthMm`, `heightMm`, `stackable` sur `InventoryItem` |
| Substituts et traductions | `InventoryItemSubstitute`, `InventoryItemTranslation`, `InventoryItemRelationService` |
| Historiques | `InventoryItemPriceHistory`, `InventoryItemRevisionHistory`, alimentés automatiquement |
| Variantes | `InventoryItemTemplate`, `InventoryVariantAxis`, `InventoryVariantValue`, `InventoryItemTemplateService` |
| Scan unifié | `InventoryScanService`, résout 7 natures d'objet |
| Référentiel | 4 nouveaux groupes d'énumérations exposés sous `/inventory/reference/enums` |
| Tests | `ItemLifecycleStatusTest`, `InventoryIdentificationServiceImplTest`, `InventoryItemTemplateServiceImplTest` |

Endpoints ajoutés :

```text
PATCH  /inventory/items/{itemCode}/lifecycle | /revision
GET    /inventory/items/{itemCode}/price-history | /revision-history
POST   /inventory/items/{itemCode}/barcodes          GET .../barcodes
PATCH  /inventory/items/barcodes/{id}/primary        DELETE /inventory/items/barcodes/{id}
POST   /inventory/items/{itemCode}/packagings        GET .../packagings
DELETE /inventory/items/packagings/{id}
POST   /inventory/items/{itemCode}/substitutes       GET .../substitutes
DELETE /inventory/items/substitutes/{id}
PUT    /inventory/items/{itemCode}/translations      GET .../translations
DELETE /inventory/items/translations/{id}
POST   /inventory/item-templates                     PUT /inventory/item-templates/{code}
GET    /inventory/item-templates                     GET /inventory/item-templates/{code}
POST   /inventory/item-templates/{code}/variants     génération, avec dryRun
GET    /inventory/scan/{code}
```

### Décisions de conception

1. **Le statut de cycle de vie est la seule source de vérité**, le booléen `active` en est dérivé.
   Laisser les deux s'écrire mutuellement produisait un conflit : passer un article en `PHASE_OUT`
   sans toucher au booléen le faisait aussitôt retomber en `OBSOLETE`. La traduction d'un `active`
   reçu se fait une seule fois, dans le service.
2. **`PHASE_OUT` bloque les entrées mais pas les sorties.** C'est tout l'intérêt du statut : écouler
   le stock restant sans racheter. Les gardes sont posés dans `StockServiceImpl.receive`, `issue` et
   `transfer`.
3. **L'ajustement reste autorisé quel que soit le statut.** C'est le seul chemin de correction ;
   le bloquer rendrait un stock erroné impossible à remettre d'aplomb.
4. **Une variante est un `InventoryItem` à part entière**, rattaché au modèle par `template_id` et
   `variant_signature`. Le stock n'a donc rien à savoir des variantes, et rien n'a été touché côté
   mouvements.
5. **La génération est rejouable** : les combinaisons déjà créées sont sautées, ce qui permet
   d'ajouter une valeur d'axe et de relancer. Un garde-fou à 500 combinaisons évite qu'une erreur de
   saisie crée des centaines d'articles.
6. **Modifier les axes d'un modèle ne touche pas aux variantes déjà générées.** Supprimer des
   articles porteurs de stock parce qu'une valeur d'axe disparaît serait destructeur.
7. **Le scan ne renvoie jamais d'erreur sur un code inconnu.** Il le signale et propose de le
   rattacher à un article ou d'en créer un, ce qui est le comportement attendu sur le terrain.

### Reste ouvert

- La recherche article (`searchText`, index trigram de V70 et V71) n'intègre pas encore les
  codes-barres : un scan d'EAN passe par `/inventory/scan`, pas par `/inventory/items?q=`. À traiter
  avec la recherche unifiée du lot 9.
- L'import d'articles n'accepte pas encore codes-barres et conditionnements en colonnes.

### Contenu

1. **Cycle de vie article.** Remplacer l'usage exclusif du booléen `active` par
   `ItemLifecycleStatus` (`DRAFT`, `NEW`, `ACTIVE`, `PHASE_OUT`, `OBSOLETE`, `BLOCKED`). `active` est
   conservé en lecture pour compatibilité, calculé depuis le statut. Règles : un article `OBSOLETE`
   ne peut plus entrer en stock, un article `BLOCKED` ne peut plus sortir.
2. **`InventoryBarcode`.** Article, type (`EAN13`, `EAN8`, `UPC`, `CODE128`, `QR`, `INTERNAL`,
   `SUPPLIER`), valeur, unité, quantité représentée, principal, actif. Unicité sur la valeur, avec
   résolution des collisions documentée.
3. **`InventoryPackaging`.** Article, niveau (unité, carton, palette), unité, quantité contenue,
   dimensions, poids, code-barres associé. Alimente les conversions existantes plutôt que de les
   dupliquer.
4. **Caractéristiques physiques.** `weightKg`, `volumeM3`, `lengthMm`, `widthMm`, `heightMm`,
   `stackable` sur `InventoryItem`.
5. **`InventoryItemSubstitute`.** Article, substitut, bidirectionnel, priorité, taux de conversion,
   actif. Exploité au lot 7 pour les propositions en cas de rupture.
6. **`InventoryItemTranslation`.** Article, langue, nom, description.
7. **`InventoryItemPriceHistory`.** Type de prix (coût par défaut, prix de vente), valeur, période de
   validité, auteur, motif. Alimenté automatiquement à chaque modification.
8. **`InventoryItemRevisionHistory`.** Révision, date, motif, document associé.
9. **Variantes.** `InventoryItemTemplate` et `InventoryItemVariant`, avec axes de variation
   (`InventoryVariantAxis`, `InventoryVariantValue`). Un article variante reste un `InventoryItem` à
   part entière côté stock, pour ne rien casser.
10. **Endpoint de scan unifié.** `GET /inventory/scan/{code}` résolvant dans l'ordre code-barres,
    code article, code d'emplacement, numéro de série, numéro de lot, code d'asset, et renvoyant la
    nature de l'objet, son identité et les actions disponibles selon le rôle.

### Impacts sur l'existant

- `InventoryItemSpecification` et les colonnes de recherche introduites par V70 et V71 doivent
  intégrer les codes-barres ;
- `InventoryItemMapper` et son décorateur ;
- `InventoryImportService` : ajouter les codes-barres et les conditionnements aux imports d'articles ;
- le générateur d'étiquettes de `admin/` doit pouvoir imprimer un code-barres réel.

### Endpoints

```text
POST   /inventory/items/{itemCode}/barcodes        GET /inventory/items/{itemCode}/barcodes
DELETE /inventory/barcodes/{id}
POST   /inventory/items/{itemCode}/packagings      GET /inventory/items/{itemCode}/packagings
POST   /inventory/items/{itemCode}/substitutes     GET /inventory/items/{itemCode}/substitutes
GET    /inventory/items/{itemCode}/price-history
PATCH  /inventory/items/{itemCode}/lifecycle
POST   /inventory/item-templates                   GET /inventory/item-templates
POST   /inventory/item-templates/{code}/variants   génération en masse des variantes
GET    /inventory/scan/{code}
```

### Migrations

`inventory_barcodes_packaging`, `inventory_item_lifecycle_and_physical`, `inventory_item_variants`,
`inventory_item_history`.

### Critères de fin

- un article peut porter trois codes-barres et être retrouvé par chacun ;
- le scan résout les six natures d'objet et renvoie des actions cohérentes avec le rôle ;
- l'import d'articles accepte codes-barres et conditionnements ;
- une modification de prix laisse une trace dans l'historique.

**Dépendances.** Lot 0. **Ordre de grandeur.** 12 à 18 jours.

---

## 6. Lot 2 · Valorisation et comptabilité matière

> **État : livré.** Migrations `V218` (dev) et `V214` (prod).

**Objectif.** Passer d'un CMUP implicite à une valorisation paramétrable, auditable et rattachable à
la comptabilité. C'est le lot le plus sensible du plan : il touche le cœur des mouvements.

### Ce qui a été livré

| Élément | Emplacement |
|---|---|
| Méthode paramétrable | `ValuationMethod`, `valuationMethod` sur `InventoryItem` et `InventoryCategory`, `effectiveValuationMethod()` |
| Couches de coût | `StockCostLayer`, `StockCostLayerRepository`, consommation verrouillée en `PESSIMISTIC_WRITE` |
| Moteur de valorisation | `StockValuationService`, entrée, sortie, transfert, bascule de méthode |
| Périodes comptables | `InventoryPeriod`, `InventoryPeriodService`, blocage des mouvements datés dans une période close |
| Photos de valorisation | `StockValuationSnapshot`, prise automatique à la clôture |
| Écritures de stock | `StockJournalEntry`, `StockAccountingService`, export CSV équilibré |
| Motifs d'ajustement | `AdjustmentReason` avec compte de contrepartie, six motifs SYSCOHADA amorcés |
| Seuils d'approbation | `InventoryAdjustmentApprovalRule`, visa exigé au-delà du seuil |
| Bascule de méthode | `PATCH /inventory/items/{code}/valuation-method`, amorce les couches |
| Référentiel | 3 nouveaux groupes d'énumérations |
| Tests | `StockValuationServiceIT` (9 cas sur PostgreSQL réel), `StockAccountingServiceTest` (9 cas) |

Endpoints ajoutés :

```text
POST   /inventory/accounting/periods              GET /inventory/accounting/periods
GET    /inventory/accounting/periods/{code}
PATCH  /inventory/accounting/periods/{code}/close | /reopen
POST   /inventory/accounting/snapshots            GET /inventory/accounting/snapshots
GET    /inventory/accounting/journal              GET /inventory/accounting/journal.csv
POST   /inventory/accounting/adjustment-reasons   GET .../adjustment-reasons
POST   /inventory/accounting/adjustment-approval-rules   GET .../adjustment-approval-rules
PATCH  /inventory/items/{itemCode}/valuation-method
```

### Décisions de conception

1. **Les couches n'existent que pour les articles en FIFO.** Un article en CMUP ne crée aucune
   couche, n'en consomme aucune, ne prend aucun verrou de plus. C'est ce qui rend le lot non cassant.
2. **`averageCost` reste alimenté dans les deux méthodes**, recalé sur la valeur réelle des couches
   en FIFO. Le tableau de bord et `totalStockValue`, tous deux basés dessus, restent justes.
3. **Le prélèvement par couche est rendu explicitement**, pas déduit de la quantité restante : une
   couche déjà partiellement consommée aurait fait transférer bien plus que demandé. Ce défaut a été
   pris au vol et il est couvert par un test.
4. **Un transfert transporte les couches avec leur date d'origine.** Déplacer du stock ne doit pas le
   rajeunir, sinon l'ancienneté du stock devient une mesure du dernier déménagement.
5. **Un transfert ne produit aucune écriture comptable** : la valeur du patrimoine ne change pas.
6. **Les sorties sont désormais valorisées.** Elles portaient jusqu'ici un coût unitaire toujours
   nul, ce qui vidait de sens la colonne valeur des rapports de mouvements. C'est un changement de
   comportement en restitution, délibéré et correctif.
7. **Le motif d'ajustement reste facultatif** pour ne pas casser les appelants existants, mais il est
   vérifié dès qu'il est fourni : code inconnu, inactif, ou inadapté au sens de l'ajustement sont
   refusés.
8. **Le seuil d'approbation porte sur la valeur, pas sur la quantité.** Cent vis et cent moteurs
   n'engagent pas la même responsabilité.
9. **Les périodes ne contraignent rien tant qu'aucune n'est déclarée.** Une entreprise qui ne tient
   pas de périodes comptables continue de fonctionner comme avant.

### Reste ouvert

- **Provision pour dépréciation** : non livrée. L'ancienneté des couches est calculée et stockée dans
  les snapshots (`oldestLayerAgeDays`), la base est donc posée, mais aucun taux de provision n'est
  appliqué. À traiter avec les indicateurs du lot 9.
- **Antériorité du FIFO** : un article basculé en cours de route n'a pas l'historique de coût de ses
  entrées antérieures. La couche d'amorçage porte le coût moyen du moment et est marquée `seeded`.
- **Contrôle d'équilibre comptable** : l'export CSV totalise débit et crédit, mais aucun contrôle
  automatique ne refuse un journal déséquilibré. À ajouter si un cabinet l'exige.

### Règle de non-régression, décidée

CMUP et FIFO coexistent, choisis par article ou par catégorie, **sans rien changer au comportement
actuel**. Concrètement :

- `valuationMethod` vaut `WEIGHTED_AVERAGE` par défaut partout, y compris sur l'existant.
- Les couches de coût n'existent que pour les articles en FIFO. Un article en CMUP ne crée aucune
  couche, ne consomme aucune couche, et ne prend aucun verrou supplémentaire.
- `StockLevel.averageCost` continue d'être alimenté dans les deux méthodes. Le tableau de bord,
  `totalStockValue` et les états existants ne bougent pas.
- Les contrats d'API existants sont inchangés ; tout ce qui est ajouté l'est en plus.
- Passer un article au FIFO est une opération explicite, `PATCH /inventory/items/{code}/valuation-method`,
  qui amorce une couche initiale par emplacement depuis la quantité et le coût moyen courants, et
  journalise le changement. C'est ce qu'impose de toute façon le principe de permanence des méthodes.

Limite assumée : pour un article passé au FIFO en cours de route, le coût d'origine des entrées
antérieures n'existe pas. Le FIFO est exact à partir de la date de bascule, approximé avant. Ce
compromis est standard lors d'un changement de méthode.

### Contenu

1. **Méthode de valorisation paramétrable.** `ValuationMethod` (`WEIGHTED_AVERAGE`, `FIFO`, `LIFO`,
   `STANDARD_COST`, `SPECIFIC_IDENTIFICATION`) portée par `InventoryCategory` et surchargeable par
   `InventoryItem`. Le CMUP existant devient l'implémentation par défaut d'une interface
   `ValuationStrategy`, ce qui évite de réécrire `StockServiceImpl`.
2. **`StockCostLayer`.** Article, emplacement, lot, quantité initiale, quantité restante, coût
   unitaire, date d'entrée, référence d'origine. Consommée selon la stratégie, avec verrouillage
   pessimiste au même titre que `StockLot`.
3. **`StockJournalEntry`.** Mouvement source, compte de stock, compte de contrepartie, sens, montant,
   date comptable, statut de report. Généré à chaque mouvement valorisé, jamais modifié.
4. **`AdjustmentReason`.** Code, libellé, compte comptable, niveau d'approbation requis, actif.
   Remplace le texte libre `reason` sur les ajustements, sans le supprimer.
5. **Seuils d'approbation d'écart.** Règle par montant ou par pourcentage au-delà de laquelle un
   ajustement ou la validation d'un inventaire exige un visa supérieur. Réutilise le modèle de
   `PurchaseApprovalRule`, qui existe déjà et fonctionne.
6. **`InventoryPeriod`.** Période comptable avec statut `OPEN`, `CLOSING`, `CLOSED`. Un mouvement
   daté dans une période close est refusé. La clôture exige zéro inventaire en cours et zéro écart
   non validé.
7. **`StockValuationSnapshot`.** Photo périodique par article, emplacement et propriétaire :
   quantité, coût unitaire, valeur. Alimente les états historiques et la clôture.
8. **Provision pour dépréciation.** Taux par tranche d'ancienneté, calculé sur l'ancienneté des
   couches de coût, restitué en état mais non écrit en comptabilité par défaut.

### Points de vigilance

- La reprise des données existantes doit créer une couche de coût initiale par `StockLevel`
  valorisée au CMUP courant, sinon le FIFO démarre sur du vide.
- `StockLevel.averageCost` reste maintenu même en FIFO, comme indicateur, pour ne pas casser le
  tableau de bord admin ni la valeur totale du stock.
- Le transfert recopie aujourd'hui le coût moyen source vers la destination. En FIFO, il doit
  transporter les couches. C'est le changement de comportement le plus risqué du lot.

### Endpoints

```text
GET   /inventory/stock/cost-layers                filtres article, emplacement, lot
GET   /inventory/valuation                        valorisation à une date, par méthode
GET   /inventory/valuation/snapshots              POST /inventory/valuation/snapshots
POST  /inventory/periods                          PATCH /inventory/periods/{code}/close | /reopen
GET   /inventory/periods
POST  /inventory/adjustment-reasons               GET /inventory/adjustment-reasons
GET   /inventory/accounting/journal-entries       export comptable
```

### Migrations

`inventory_valuation_layers`, `inventory_accounting_journal`, `inventory_periods`,
`inventory_adjustment_reasons`, `inventory_valuation_snapshots`.

### Critères de fin

- un article en FIFO et un article en CMUP coexistent et se valorisent correctement ;
- un mouvement dans une période close est refusé ;
- la somme des couches de coût égale la quantité en stock, vérifiée par la réconciliation du lot 0 ;
- l'export d'écritures est équilibré au débit et au crédit.

**Dépendances.** Lot 0. **Ordre de grandeur.** 20 à 28 jours. C'est le lot le plus lourd.

---

## 7. Lot 3 · Traçabilité et qualité

> **État : livré.** Migrations `V219` et `V220` (dev), `V215` et `V216` (prod).

**Objectif.** Exploiter `StockLot.quarantined`, qui existe sans processus, et rendre la traçabilité
consultable dans les deux sens.

### Ce qui a été livré

| Élément | Emplacement |
|---|---|
| Quantité immobilisée | `StockLevel.quantityQuarantined`, retranchée du disponible |
| Quarantaine et blocage | `StockQuarantineService`, `LotBlockReasonType`, levée à double regard |
| Généalogie | `StockLotGenealogy`, `LotGenealogyRelation` |
| Traçabilité | `StockTraceabilityService`, amont, aval, positions et filiation en un appel |
| Plans de contrôle | `QualityControlPlan`, `QualityCriterion`, portée article ou catégorie |
| Inspections | `QualityInspection`, `QualityInspectionResult`, décision automatique |
| Non-conformités | `NonConformance`, ouverte automatiquement en cas d'échec |
| Rappels produit | `ProductRecall`, `ProductRecallService`, gel des lots et liste des détenteurs |
| Référentiel | 8 nouveaux groupes d'énumérations |
| Tests | `StockQuarantineServiceIT` (7 cas), `QualityControlServiceIT` (9 cas) |

Endpoints ajoutés :

```text
GET    /inventory/traceability/{lotNumber}
PATCH  /inventory/stock/lots/{lotId}/quarantine | /release | /block | /unblock
GET    /inventory/stock/lots/immobilised
POST   /inventory/quality/plans                  GET /inventory/quality/plans
POST   /inventory/quality/lots/{lotId}/inspections
GET    /inventory/quality/lots/{lotId}/inspections
POST   /inventory/quality/non-conformances       GET .../non-conformances
PATCH  /inventory/quality/non-conformances/{code}/close
POST   /inventory/quality/recalls                GET .../recalls
PATCH  /inventory/quality/recalls/{code}/launch | /recovery | /close | /cancel
```

### Décisions de conception

1. **Le disponible retranche désormais le stock immobilisé.** La consommation de lots écartait déjà
   les lots en quarantaine, mais le contrôle de suffisance portait sur `quantityAvailable` qui les
   comptait encore : une sortie pouvait passer le contrôle puis échouer faute de lot consommable.
   Aucun lot n'étant immobilisé après la migration, le disponible reste celui d'avant.
2. **Quarantaine et blocage sont deux mécanismes distincts.** La quarantaine attend une décision
   qualité et sa levée exige un approbateur différent du demandeur. Le blocage est une décision de
   gestion, levée par qui l'a posée.
3. **Un critère non bloquant en échec ne condamne pas le lot.** Il se note, ce qui permet de
   consigner une observation sans immobiliser de la marchandise utilisable.
4. **Une dérogation exige un auteur et un motif.** Sans les deux, elle est refusée : une dérogation
   anonyme n'engage personne et ne vaut rien.
5. **Une dérogation ouvre quand même une non-conformité.** L'écart a été constaté, il est seulement
   accepté.
6. **Une mesure hors plan est conservée mais non bloquante.** Elle documente une observation sans
   faire échouer le contrôle.
7. **Sans plan déclaré, rien ne change.** Aucune mise en quarantaine automatique, les réceptions se
   comportent exactement comme avant.
8. **Annuler un rappel libère les lots qu'il avait gelés**, sinon du stock resterait immobilisé sans
   motif lisible.

### Reste ouvert

- **Alimentation automatique de la généalogie** : la table et les endpoints de lecture existent, mais
  aucun flux ne crée de lien parent-enfant. Il n'y a pas encore d'opération de transformation ni de
  reconditionnement dans le module, elles viennent avec les nomenclatures du lot 6.
- **Notification des détenteurs lors d'un rappel** : la liste est calculée, l'envoi reste à brancher
  sur `features/notification`.
- **`GoodsReceiptLine.qualityAccepted`** n'est pas encore rattaché à une inspection. À traiter avec
  les réceptions du lot 5.
- **Échantillonnage** : le mode et son paramètre sont stockés mais aucun calcul de taille
  d'échantillon n'est fait, la quantité contrôlée étant déclarée par l'inspecteur.

### Contenu

1. **`StockLotGenealogy`.** Lot parent, lot enfant, quantité, type de relation (transformation,
   reconditionnement, fusion, division), mouvement source.
2. **Endpoint de traçabilité.** `GET /inventory/traceability/{lotNumber}` remontant l'amont
   (réception, fournisseur, commande) et descendant l'aval (mouvements, consommations, destinataires),
   en un seul appel.
3. **`QualityControlPlan`, `QualityCriterion`.** Article ou catégorie, étape de contrôle, mode
   d'échantillonnage, critères avec valeur attendue et tolérance, documents exigés, décision par
   défaut en cas d'échec.
4. **`QualityInspection`, `QualityInspectionResult`.** Exécution du plan avec quantités conformes et
   non conformes, décision (`ACCEPTED`, `REJECTED`, `QUARANTINED`, `ACCEPTED_BY_DEROGATION`), auteur
   et motif.
5. **Workflow de quarantaine.** Mise en quarantaine automatique à la réception selon le plan,
   libération soumise à double validation et à la permission `RELEASE`. Un lot en quarantaine est
   invisible du stock disponible.
6. **Blocage de lot.** `blocked` et `blockReason` sur `StockLot`, distincts de la quarantaine, pour
   un blocage administratif.
7. **`NonConformance`.** Source, article, lot, quantité, gravité, description, photos, disposition
   (`USE_AS_IS`, `REWORK`, `RETURN_TO_SUPPLIER`, `SCRAP`, `DOWNGRADE`), action corrective,
   responsable, échéance, réclamation fournisseur associée, impact chiffré.
8. **`ProductRecall`.** Gel de tous les lots d'une plage, calcul des détenteurs via la traçabilité
   descendante, notification, suivi du taux de retour.

### Impacts sur l'existant

- `GoodsReceipt` déclenche le plan de contrôle. `qualityAccepted` et `rejectionReason` existent
  déjà sur `GoodsReceiptLine` et doivent être rattachés à l'inspection plutôt que saisis à part.
- Le calcul du disponible doit exclure quarantaine et blocage, ce qui touche `StockLevel` et les
  suggestions de prélèvement.

### Endpoints

```text
GET   /inventory/traceability/{lotNumber}
POST  /inventory/quality/plans                    GET /inventory/quality/plans
POST  /inventory/quality/inspections              GET /inventory/quality/inspections
PATCH /inventory/quality/inspections/{code}/decide
PATCH /inventory/stock/lots/{id}/quarantine | /release | /block | /unblock
POST  /inventory/quality/non-conformances         PATCH .../{code}/close
POST  /inventory/quality/recalls                  GET /inventory/quality/recalls/{code}/holders
```

### Migrations

`inventory_lot_genealogy`, `inventory_quality_control`, `inventory_non_conformance`,
`inventory_product_recall`.

### Critères de fin

- un lot réceptionné avec plan de contrôle part automatiquement en quarantaine et n'apparaît pas au
  disponible ;
- la traçabilité d'un lot consommé remonte jusqu'au fournisseur et descend jusqu'aux sorties ;
- un rappel identifie les détenteurs et gèle les lots concernés.

**Dépendances.** Lots 0 et 1. **Ordre de grandeur.** 15 à 20 jours.

---

## 8. Lot 4 · Inventaires physiques

**Objectif.** Faire passer `InventoryCount` d'un comptage simple à un dispositif de contrôle sérieux.

### Contenu

1. **Comptage aveugle.** Option masquant `expectedQuantity` au compteur, la variance n'étant calculée
   qu'à la revue.
2. **Double comptage.** Deux passages indépendants, arbitrage obligatoire en cas d'écart entre eux,
   traçabilité des deux compteurs.
3. **Gel de zone.** Blocage des mouvements sur les emplacements concernés pendant le comptage, avec
   file d'attente ou refus explicite selon paramétrage.
4. **Comptage tournant.** `AbcXyzClassification` calculée sur la valeur consommée et la régularité,
   pilotant une fréquence de comptage par classe et générant un calendrier. Mesure du taux de
   couverture annuel.
5. **Granularité fine.** Comptage par emplacement de rangement et par lot, pas seulement par article
   et emplacement.
6. **Seuils d'approbation.** Rattachement aux règles du lot 2 : au-delà d'un montant ou d'un
   pourcentage d'écart, la validation exige un visa supérieur.
7. **Motifs d'écart.** Utilisation des `AdjustmentReason` du lot 2 sur chaque ligne d'écart.
8. **Indicateur d'exactitude.** Nombre d'emplacements justes rapporté au nombre compté, suivi dans le
   temps.

### Impacts sur l'existant

`InventoryCountServiceImpl` et le cycle `DRAFT` vers `VALIDATED` sont conservés. Les ajouts se
greffent dessus, avec deux statuts supplémentaires : `SECOND_COUNT` et `ARBITRATION`.

### Endpoints

```text
POST  /inventory/counts                          paramètres aveugle, double comptage, périmètre
PATCH /inventory/counts/{code}/second-count | /arbitrate
GET   /inventory/counts/{code}/variances         avec valorisation de l'écart
POST  /inventory/counts/cycle-plan               génération du calendrier tournant
GET   /inventory/classification/abc-xyz          POST pour recalcul
GET   /inventory/counts/accuracy                 indicateur d'exactitude
```

### Migrations

`inventory_count_advanced`, `inventory_abc_xyz_classification`, `inventory_location_freeze`.

### Critères de fin

- un comptage aveugle ne laisse pas fuiter la quantité théorique dans la réponse API ;
- un mouvement sur une zone gelée est refusé avec un message explicite ;
- un écart au-delà du seuil bloque la validation sans visa ;
- le calendrier tournant couvre 100 % des articles de classe A sur l'année.

**Dépendances.** Lots 0 et 2. **Ordre de grandeur.** 12 à 16 jours.

---

## 9. Lot 5 · Achats avancés

**Objectif.** Compléter une chaîne d'achat déjà solide (`PurchaseRequest` vers `PurchaseOrder` vers
`GoodsReceipt`, avec règles d'approbation) sur ses angles morts : amont contractuel, aval financier,
et retours.

### Contenu

1. **Appel d'offres.** `RequestForQuotation` et `SupplierQuotation`, grille comparative multi-critères,
   attribution tracée, conversion en commande.
2. **Contrats-cadres.** `SupplierAgreement` (période, engagement de volume, révision de prix,
   pénalités) et `SupplierPriceTier` (quantité minimale, prix, validité). Le prix d'une ligne de
   commande se résout dans l'ordre contrat, palier, `SupplierItem`, coût par défaut.
3. **Avis d'expédition.** `AdvanceShipmentNotice` avec lignes attendues et date, pré-remplissant la
   réception.
4. **Rapprochement à trois voies.** Contrôle commande contre réception contre facture, avec
   tolérances paramétrables de prix et de quantité, et blocage du paiement en cas d'écart. Point
   d'intégration avec `features/billing`.
5. **Retours et litiges.** `SupplierReturn` et `SupplierClaim` (qualité, quantité, prix, retard),
   suivi de l'avoir attendu, alimentés par les non-conformités du lot 3.
6. **Frais accessoires.** `LandedCostAllocation` répartissant transport, dédouanement, assurance et
   manutention sur les lignes réceptionnées, au poids, au volume ou à la valeur. Incorporé au coût
   d'entrée, donc dépendant du lot 2.
7. **Multi-devise.** `currencyCode` et `exchangeRate` sur les documents d'achat, écart de change
   constaté à la facturation. Les montants restent en entiers, la devise s'ajoute.
8. **Taxes d'achat.** Ventilation sur les lignes, récupérable ou non, en cohérence avec la
   ventilation fiscale déjà en place dans `billing`.
9. **Scoring fournisseur.** Remplacer l'endpoint `performance` actuel par un score pondéré :
   ponctualité, conformité quantité, conformité qualité, respect du prix, litiges. Historisé.
10. **Budget achat.** Enveloppes par période et par catégorie, contrôle d'engagement au moment de la
    commande.

### Endpoints

```text
POST  /inventory/procurement/rfq                  POST .../rfq/{code}/quotations
GET   /inventory/procurement/rfq/{code}/comparison
POST  /inventory/procurement/rfq/{code}/award
POST  /inventory/procurement/agreements           GET /inventory/procurement/agreements
POST  /inventory/procurement/asn                  GET /inventory/procurement/asn
POST  /inventory/procurement/goods-receipts/from-asn/{code}
GET   /inventory/procurement/three-way-match      POST .../three-way-match/{code}/resolve
POST  /inventory/procurement/supplier-returns     POST /inventory/procurement/claims
POST  /inventory/procurement/landed-costs
GET   /inventory/procurement/suppliers/{code}/scorecard
POST  /inventory/procurement/budgets
```

### Migrations

`inventory_procurement_rfq`, `inventory_supplier_agreements`, `inventory_asn`,
`inventory_three_way_match`, `inventory_supplier_returns`, `inventory_landed_cost`,
`inventory_purchase_currency_tax`, `inventory_purchase_budget`.

### Critères de fin

- une commande issue d'un appel d'offres conserve le lien vers l'offre retenue ;
- un prix de ligne se résout selon la hiérarchie contrat, palier, article fournisseur ;
- des frais de transport répartis modifient le coût d'entrée et donc la valorisation ;
- une facture hors tolérance bloque le paiement et ouvre un litige.

**Dépendances.** Lots 2 et 3. **Ordre de grandeur.** 22 à 30 jours.

---

## 10. Lot 6 · Opérations d'entrepôt

**Objectif.** Passer d'un suivi de stock à un pilotage d'entrepôt.

### Contenu

1. **Zonage.** Type de zone sur `InventoryLocation` (réception, réserve, prélèvement, expédition,
   quarantaine, rebut) avec règles et droits propres.
2. **`PutAwayRule`.** Article ou catégorie, zone cible, critère de choix, capacité, priorité.
3. **`AllocationStrategy`.** Par article ou catégorie : `FEFO`, `FIFO`, `LIFO`, lot le plus proche de
   la péremption, emplacement le plus proche, quantité exacte. Remplace la logique implicite actuelle
   de `StockPlanningController.picking`.
4. **`PickingList` et `PickingListLine`.** Préparation multi-lignes, regroupement par tournée,
   contrôle par scan, statut par ligne, colisage.
5. **`HandlingUnit`.** Palette ou colis avec identifiant, contenu, poids, emplacement. Déplacer une
   unité déplace son contenu.
6. **`WarehouseTask`.** Type, priorité, opérateur, emplacement source et cible, statut, temps passé.
   Permet de mesurer l'activité et de répartir le travail.
7. **`InternalReplenishmentTask`.** Réapprovisionnement de la zone de prélèvement depuis la réserve,
   déclenché sur seuil.
8. **Capacité d'emplacement.** Dimensions, poids et volume maximum, avec refus ou avertissement au
   rangement. Utilise les caractéristiques physiques du lot 1.
9. **Cross-docking et livraison directe.** Flux réception vers expédition sans mise en stock, et
   commande fournisseur livrée directement au destinataire final.
10. **Nomenclatures.** `InventoryBom` et `InventoryBomLine`, avec opérations d'assemblage et de
    désassemblage générant les mouvements correspondants et la généalogie de lots du lot 3.

### Endpoints

```text
POST  /inventory/warehouse/put-away-rules         GET /inventory/warehouse/put-away-rules
POST  /inventory/warehouse/allocation-strategies
POST  /inventory/warehouse/picking-lists          PATCH .../{code}/start | /complete
GET   /inventory/warehouse/picking-lists/{code}/next-task
POST  /inventory/warehouse/handling-units         PATCH .../{code}/move
GET   /inventory/warehouse/tasks                  PATCH .../tasks/{id}/assign | /complete
POST  /inventory/warehouse/replenishment/run
POST  /inventory/boms                             POST /inventory/boms/{code}/assemble | /disassemble
POST  /inventory/warehouse/cross-dock
```

### Migrations

`inventory_warehouse_zones`, `inventory_warehouse_rules`, `inventory_picking`,
`inventory_handling_units`, `inventory_warehouse_tasks`, `inventory_bom`.

### Critères de fin

- une préparation de dix lignes se déroule au scan avec contrôle d'erreur ;
- l'allocation respecte la stratégie déclarée sur l'article ;
- déplacer une palette déplace son contenu en un mouvement cohérent ;
- un assemblage consomme les composants et produit le lot fils avec sa généalogie.

**Dépendances.** Lots 1, 2 et 3. **Ordre de grandeur.** 25 à 32 jours.

---

## 11. Lot 7 · Planification et prévision

**Objectif.** Remplacer des seuils saisis à la main par des seuils calculés, et anticiper au lieu de
réagir.

### Contenu

1. **`ConsumptionForecast`.** Article, emplacement, période, quantité prévue, intervalle de confiance,
   méthode. Méthodes : moyenne mobile, lissage exponentiel simple et double, détection de
   saisonnalité. Calcul en tâche planifiée.
2. **Stock de sécurité calculé.** À partir de la variabilité de la demande, du délai fournisseur
   (`Supplier.averageLeadTimeDays` et `SupplierItem.leadTimeDays` existent déjà) et d'un niveau de
   service cible par classe ABC. Alimente `InventoryReorderRule` au lieu de la saisie manuelle, en
   laissant la possibilité de figer une valeur.
3. **Quantité économique de commande.** Arbitrage coût de passation contre coût de possession, avec
   respect des multiples de conditionnement du lot 1.
4. **`NetRequirementRun`.** Calcul des besoins nets : besoin prévu moins disponible moins réservé
   plus commandes attendues, décalé du délai, avec proposition d'ordres. Horizon paramétrable.
5. **`StockOutEvent`.** Article, emplacement, début, fin, demande non servie. Mesure le taux de
   service réel, qui aujourd'hui n'est pas mesurable.
6. **Mode simulation.** `dry-run` sur les propositions de réapprovisionnement, avec impact chiffré sur
   la trésorerie et le taux de service.
7. **Substituts en rupture.** Exploitation de `InventoryItemSubstitute` du lot 1 pour proposer un
   équivalent disponible.

### Impacts sur l'existant

`InventoryReorderRuleServiceImpl` et `ReorderSuggestionResponse` sont conservés et enrichis.
`ConsumptionTrend`, aujourd'hui purement descriptif, devient une sortie du moteur de prévision.

### Endpoints

```text
POST  /inventory/planning/forecast/run            GET /inventory/planning/forecast
GET   /inventory/planning/safety-stock            POST .../safety-stock/apply
GET   /inventory/planning/eoq
POST  /inventory/planning/net-requirements/run    GET /inventory/planning/net-requirements
GET   /inventory/planning/simulation
GET   /inventory/analytics/service-level
```

### Migrations

`inventory_forecast`, `inventory_net_requirements`, `inventory_stockout_events`,
`inventory_reorder_rule_computed`.

### Critères de fin

- la prévision sur un historique de douze mois produit un résultat comparable à la consommation
  réelle du mois suivant, dans l'intervalle annoncé ;
- l'application du stock de sécurité calculé met à jour les règles sans écraser les valeurs figées
  manuellement ;
- le calcul des besoins nets propose des ordres cohérents avec les délais fournisseurs.

**Dépendances.** Lots 2, 4 et 5. **Ordre de grandeur.** 18 à 24 jours.

---

## 12. Lot 8 · Flux tiers, propriété et divers

**Objectif.** Exploiter `StockOwnershipType`, qui existe sans processus, et solder les points isolés
de la section 2.8.

### Contenu

1. **Consignation entrante.** Stock chez nous, propriété du fournisseur, facturé à la consommation.
   Relevé de consommation périodique, rapprochement fournisseur, auto-facturation. Exclu du bilan
   tant que non consommé.
2. **Consignation sortante.** Notre stock chez un tiers, avec inventaire à distance et relevé.
3. **Stock confié par un client.** Ni valorisé ni facturé, suivi et restituable, avec responsabilité
   en cas de perte.
4. **Séparation comptable stricte.** Filtre obligatoire sur `ownershipType` dans **tous** les états
   de valorisation. Un état qui additionne stock propre et stock tiers est un défaut bloquant.
5. **Réservation d'asset par plage de dates.** `AssetReservation` avec calendrier, détection de
   conflit et proposition d'alternative. Le statut `RESERVED` existe déjà sans calendrier.
6. **Pièces jointes normalisées.** Rattachement générique à `features/document` par `entityType` et
   `entityCode`, sur article, lot, mouvement, inventaire, réception, asset. Aujourd'hui seul
   `GoodsReceipt` est câblé.
7. **Cloisonnement multi-société.** `businessCode` existe sur `InventoryLocation` mais n'est pas
   propagé comme dimension de filtrage sur les articles, les assets et les états. À généraliser,
   avec filtrage systématique côté requête et non côté réponse.
8. **`InternalTransferPricing`.** Article ou catégorie, entité source, entité destination, méthode de
   valorisation, marge. Génère une facturation inter-sociétés via `billing` quand les deux entités
   diffèrent.

### Endpoints

```text
GET   /inventory/consignment/statements           POST .../statements/{code}/settle
GET   /inventory/stock/levels?ownershipType=      filtre rendu obligatoire sur les états de valeur
POST  /inventory/assets/{assetCode}/reservations  GET /inventory/assets/availability
POST  /inventory/attachments                      GET /inventory/attachments
POST  /inventory/settings/internal-transfer-pricing
```

### Migrations

`inventory_consignment`, `inventory_asset_reservation_calendar`, `inventory_attachments`,
`inventory_business_scoping`, `inventory_internal_transfer_pricing`.

### Critères de fin

- un état de valorisation refuse de s'exécuter sans filtre de propriété explicite ;
- une réservation d'asset sur une plage déjà prise est refusée avec la liste des alternatives ;
- un utilisateur d'une société ne voit aucune donnée d'une autre société.

**Dépendances.** Lots 2 et 5. **Ordre de grandeur.** 15 à 20 jours.

---

## 13. Lot 9 · Recherche, interopérabilité et pilotage

**Objectif.** Rendre le module consultable, intégrable et pilotable. C'est le lot qui transforme les
données accumulées par les lots précédents en usage.

### Contenu

1. **Recherche Elasticsearch.** Chantier complet : ajout de la dépendance dans `pom.xml`,
   configuration dans `application.yml`, création du paquet `search/`, indexation des articles,
   assets, lots, séries, emplacements et mouvements, réindexation à la demande et synchronisation par
   l'outbox. Recherche par code-barres, code partiel et tolérance aux fautes. La recherche JPA
   actuelle reste le mode de repli si l'index est indisponible.
2. **Webhooks sortants.** Événements sur mouvement, alerte, réception, inspection, en réutilisant
   l'infrastructure de `features/notification`.
3. **Import étendu.** Ajouter mouvements, comptages, prix fournisseur, emplacements et codes-barres
   aux quatre types existants. Mode simulation, rapport d'erreurs téléchargeable, annulation d'import.
4. **Export généralisé.** Export CSV et Excel de toute liste filtrée, plus export programmé récurrent.
5. **Export comptable.** Génération des écritures de stock du lot 2 au format attendu par le cabinet.
6. **GraphQL.** Exposition du catalogue et des niveaux de stock via Netflix DGS, déjà en place dans
   le projet, schémas dans `src/main/resources/graphql-client/`.
7. **Indicateurs.** Rotation du stock, couverture en jours, taux de service, ancienneté et valeur
   dormante, exactitude d'inventaire, valeur par famille et par propriétaire, coût des ajustements et
   des pertes par motif, performance fournisseur consolidée. Poussés vers `analytics` et `reporting`.
8. **Tableaux de bord par rôle.** Remplacer le tableau de bord unique de `admin/` par des vues
   configurables : magasinier, acheteur, responsable qualité, contrôleur de gestion, direction.

### Endpoints

```text
GET   /inventory/search                           recherche unifiée multi-entités
POST  /inventory/search/reindex
POST  /inventory/import/{type}/simulate           POST /inventory/import/{runId}/rollback
GET   /inventory/export/{entity}                  POST /inventory/export/schedules
GET   /inventory/accounting/export
GET   /inventory/analytics/turnover | /aging | /service-level | /accuracy
GET   /inventory/admin/dashboards/{role}
```

### Migrations

`inventory_search_index_state`, `inventory_import_runs`, `inventory_export_schedules`,
`inventory_dashboard_preferences`.

### Critères de fin

- une recherche sur un code partiel avec une faute de frappe retrouve l'article ;
- l'indisponibilité d'Elasticsearch ne casse pas la recherche, elle la dégrade ;
- un import en simulation ne modifie rien et produit un rapport exploitable ;
- chaque rôle dispose d'un tableau de bord distinct et pertinent.

**Dépendances.** Tous les lots précédents, pour les indicateurs. La recherche peut démarrer en
parallèle dès le lot 1. **Ordre de grandeur.** 25 à 35 jours.

---

## 14. Séquencement

```text
Lot 0  Durcissement          ████
Lot 1  Référentiel               ████████
Lot 2  Valorisation                      ██████████████
Lot 3  Traçabilité et qualité                    ██████████
Lot 4  Inventaires                                   ████████
Lot 5  Achats avancés                                     ██████████████
Lot 6  Entrepôt                                                 ████████████
Lot 7  Planification                                                  ██████████
Lot 8  Flux tiers                                                         ████████
Lot 9  Recherche et pilotage       ░░░░ (recherche, en parallèle)     ████████████
```

Trois chemins critiques :

1. **Lot 0 vers lot 2 vers tout le reste.** La valorisation conditionne les achats, les inventaires
   et le pilotage. Tout retard sur le lot 2 se propage.
2. **Lot 1 vers lots 4 et 6.** Sans codes-barres ni caractéristiques physiques, ni le comptage au
   scan ni la gestion de capacité ne tiennent.
3. **Lot 3 vers lot 5.** Les non-conformités alimentent les litiges fournisseur.

La recherche Elasticsearch du lot 9 est le seul chantier réellement parallélisable : elle ne dépend
que du modèle de données et peut être menée par une autre personne dès la fin du lot 1.

**Volume total.** De l'ordre de 170 à 230 jours-développeur, hors recette et reprise de données.
Ces chiffres sont des ordres de grandeur destinés à arbitrer entre lots, pas un engagement.

---

## 15. Points à trancher avant de démarrer

Ces décisions changent le contenu des lots. Elles relèvent du métier, pas de la technique.

### Tranchées

1. **Méthode de valorisation cible. → CMUP et FIFO, les deux, paramétrables par article.**
   Avec une contrainte explicite : ne rien casser pour l'usage actuel du stock. Les couches de coût
   n'existent donc que pour les articles déclarés en FIFO. Un article en CMUP se comporte exactement
   comme aujourd'hui, sans écriture ni verrou supplémentaire. Le passage d'un article au FIFO est une
   opération explicite qui amorce une couche initiale depuis le stock courant, tracée et datée,
   comme l'impose le principe de permanence des méthodes. `averageCost` reste alimenté dans les deux
   méthodes, pour que le tableau de bord et `totalStockValue` ne bougent pas.
2. **Intégration comptable. → Écritures plus export générique.**
   `StockJournalEntry` généré à chaque mouvement valorisé, avec compte de stock et contrepartie, plus
   un export CSV neutre. Aucun format d'éditeur ciblé tant qu'aucun n'est nommé.
7. **Couverture de tests. → Testcontainers, livré avant le lot 2.**
   Socle `PostgresIntegrationTestBase`, plus trois classes couvrant l'immuabilité, la requête de
   réconciliation et la concurrence. `./mvnw test` exécute tout et exige Docker ;
   `./mvnw test -DexcludedGroups=integration` garde une boucle rapide sans Docker.

### Encore ouvertes

3. **Multi-devise.** Y a-t-il réellement des achats en devise ? Si non, le lot 5 s'allège nettement.
4. **Périmètre entrepôt.** Le lot 6 vise un entrepôt piloté par emplacements fins et par tâches.
   Si l'organisation réelle ne compte que quelques dépôts sans zonage, il peut être réduit aux
   stratégies d'allocation et aux nomenclatures, et reporté.
5. **Mobilité hors ligne.** Le socle serveur fournit déjà l'idempotence et fournira le scan. La file
   d'attente hors ligne relève de l'application cliente : est-elle dans ce projet ou dans le projet
   BTP, qui en aura besoin de toute façon ?
6. **Rupture d'API.** Passer les endpoints de mouvement en idempotence obligatoire casse les clients
   qui n'envoient pas de clé. Quand, et avec quel préavis ?
8. **Chaîne de migrations de développement.** `db/migration` ne rejoue pas sur une base vierge :
   six fichiers échouent, un nouveau poste ne peut donc pas créer sa base à partir de ces
   migrations. `db/migration-prod` rejoue intégralement. Corriger les six fichiers changerait leur
   empreinte et casserait `validate-on-migrate` sur les environnements en place. La correction passe
   par des migrations d'alignement supplémentaires côté dev, sur le modèle de ce qui existe déjà
   côté prod. Chantier à part entière, à arbitrer. Détail dans
   [`../backend/inventory-lot0-verification.md`](../backend/inventory-lot0-verification.md).

---

## 16. Risques

| Risque | Impact | Réduction |
|---|---|---|
| Régression sur la valorisation au lot 2 | fausse la valeur du stock et la comptabilité | tests d'intégration avant refonte, double calcul en parallèle pendant une période, réconciliation du lot 0 en filet |
| Couverture de tests très faible sur le module | toute modification du cœur est risquée | socle Testcontainers livré avant le lot 2 : immuabilité, réconciliation et concurrence désormais automatisées |
| La chaîne de migrations dev ne rejoue pas sur une base vierge | un nouvel environnement n'est pas créable depuis les migrations | tests branchés sur `db/migration-prod`, qui rejoue ; correction de la chaîne dev à arbitrer (point 8) |
| Transfert en FIFO qui doit transporter les couches | corruption silencieuse du coût | traiter ce cas en premier dans le lot 2, avec jeu de tests dédié |
| Elasticsearch à construire de zéro | charge sous-estimée au lot 9 | démarrer tôt en parallèle, garder JPA en repli permanent |
| Double dossier Flyway | une migration oubliée en prod bloque le déploiement | contrôle automatisé en intégration continue comparant les deux dossiers |
| Divergence avec le projet BTP | deux modèles de données incompatibles | geler les extensions d'enums et les interfaces de service partagées dès le lot 1, et les documenter comme contrat |
| Clôture de période mal acceptée | rejet utilisateur | démarrer en mode avertissement avant de passer en blocage |

---

## 17. Contrat d'extension pour le projet BTP

Ce que ce plan garantit au projet BTP, et qu'il ne faut donc pas casser :

- `StockService` et `InventoryConsumptionService` restent les seules portes d'écriture du stock, et
  acceptent `referenceType` plus `referenceCode` pour toute imputation externe ;
- `InventoryLocation` reste une arborescence libre, extensible par de nouveaux types ;
- `Asset` et `AssetAssignment` restent ouverts à de nouveaux `AssetAssigneeType` ;
- `StockOwnershipType` et son filtrage sont opérationnels dès le lot 8 ;
- tout objet est résolvable par `GET /inventory/scan/{code}` dès le lot 1 ;
- la valorisation est interrogeable à une date donnée, ce dont le calcul de coût de revient BTP aura
  besoin ;
- les événements de l'outbox portent un contrat stable, documenté au lot 9.

Toute rupture sur ces sept points doit être signalée au projet BTP avant d'être livrée.
