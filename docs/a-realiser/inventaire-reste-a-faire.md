# Inventaire · point d'arrêt et reste à faire

> Note de reprise. Le module inventaire est mis en pause après le lot 3. Ce document consigne ce qui
> est livré, ce qui reste, les décisions non tranchées et la dette accumulée, pour pouvoir reprendre
> sans relire tout l'historique.
>
> Le détail de chaque lot restant se trouve dans
> [`plan-inventaire-generaliste-implementation.md`](plan-inventaire-generaliste-implementation.md),
> sections 8 à 13. Le périmètre BTP reste dans
> [`plan-inventaire-btp.md`](plan-inventaire-btp.md) et fait l'objet d'un projet distinct.

## 1. Où on en est

Quatre lots livrés, sept commits, 399 tests verts dont 32 tests d'intégration sur PostgreSQL réel.

| Lot | Contenu | Migrations |
|---|---|---|
| 0 | Immuabilité du journal de mouvements, réconciliation, rapport de dérogations, idempotence pilotable par configuration, référentiel de listes servi par le backend | V216 / V212 |
| 1 | Cycle de vie article, codes-barres multiples, conditionnements, substituts, traductions, historiques de prix et de révision, variantes, scan unifié | V217 / V213 |
| 2 | CMUP et FIFO coexistants, couches de coût, périodes comptables, photos de valorisation, écritures de stock, motifs d'ajustement, seuils d'approbation | V218 / V214 |
| 3 | Quarantaine et blocage de lot, généalogie, traçabilité amont et aval, plans de contrôle, inspections, non-conformités, rappels produit | V219 et V220 / V215 et V216 |

Hors lot : socle de tests d'intégration Testcontainers (`PostgresIntegrationTestBase`), branché sur
`db/migration-prod`.

Commandes :

```bash
./mvnw test                                  # tout, exige Docker
./mvnw test -DexcludedGroups=integration     # boucle rapide sans Docker
```

## 2. Lots restants

Ordre de grandeur total : **120 à 160 jours-développeur**.

### Lot 4 · Inventaires physiques (12 à 16 j, dépend des lots 0 et 2)

1. Comptage aveugle, masquant la quantité théorique au compteur.
2. Double comptage indépendant avec arbitrage obligatoire en cas d'écart entre les deux passages.
3. Gel de zone pendant le comptage, avec refus ou file d'attente selon paramétrage.
4. Comptage tournant piloté par une classification ABC et XYZ, avec calendrier généré et taux de
   couverture annuel mesuré.
5. Granularité fine : comptage par emplacement de rangement et par lot.
6. Seuils d'approbation d'écart, branchés sur les règles posées au lot 2.
7. Motifs d'écart codifiés, réutilisant les `AdjustmentReason` du lot 2.
8. Indicateur d'exactitude d'inventaire, suivi dans le temps.

C'est le lot le plus court et le plus autonome. Bon point de reprise.

### Lot 5 · Achats avancés (22 à 30 j, dépend des lots 2 et 3)

1. Appels d'offres avec grille comparative et attribution tracée.
2. Contrats-cadres, paliers tarifaires, révision de prix, pénalités.
3. Avis d'expédition pré-remplissant la réception.
4. Rapprochement à trois voies commande, réception, facture, avec tolérances et blocage du paiement.
5. Retours et litiges fournisseur, alimentés par les non-conformités du lot 3.
6. Frais accessoires incorporés au coût d'entrée.
7. Multi-devise et écart de change.
8. Taxes d'achat ventilées sur les lignes.
9. Scoring fournisseur pondéré remplaçant l'endpoint `performance` actuel.
10. Budgets d'achat avec contrôle d'engagement.

### Lot 6 · Opérations d'entrepôt (25 à 32 j, dépend des lots 1, 2 et 3)

1. Zonage des emplacements avec règles et droits propres.
2. Règles de rangement dirigé.
3. Stratégies d'allocation paramétrables, remplaçant la logique implicite actuelle.
4. Préparations multi-lignes avec contrôle par scan et colisage.
5. Unités logistiques, déplacer un contenant déplaçant son contenu.
6. Tâches d'entrepôt, pour piloter et mesurer l'activité.
7. Réapprovisionnement interne de la zone de prélèvement.
8. Capacité d'emplacement en poids, volume et dimensions.
9. Cross-docking et livraison directe.
10. Nomenclatures, avec assemblage et désassemblage.

**C'est ce lot qui alimentera enfin la généalogie des lots**, aujourd'hui lisible mais jamais écrite.

### Lot 7 · Planification et prévision (18 à 24 j, dépend des lots 2, 4 et 5)

1. Prévision de consommation par moyenne mobile, lissage exponentiel, saisonnalité.
2. Stock de sécurité calculé au lieu de saisi, à partir de la variabilité et du délai fournisseur.
3. Quantité économique de commande, respectant les multiples de conditionnement du lot 1.
4. Calcul des besoins nets avec horizon paramétrable.
5. Mesure du taux de service réel, impossible aujourd'hui faute d'événements de rupture.
6. Mode simulation sur les propositions de réapprovisionnement.
7. Proposition de substituts en rupture, exploitant ceux du lot 1.

### Lot 8 · Flux tiers, propriété et divers (15 à 20 j, dépend des lots 2 et 5)

1. Consignation entrante, facturée à la consommation, hors bilan tant que non consommée.
2. Consignation sortante avec inventaire à distance.
3. Stock confié par un client, ni valorisé ni facturé mais suivi et restituable.
4. Séparation comptable stricte : aucun état de valeur ne doit additionner stock propre et stock tiers.
5. Réservation d'asset par plage de dates, avec détection de conflit.
6. Pièces jointes normalisées sur toutes les entités du module.
7. Cloisonnement multi-société généralisé.
8. Tarification de cession interne et facturation inter-sociétés.

### Lot 9 · Recherche, interopérabilité et pilotage (25 à 35 j)

1. Recherche Elasticsearch, chantier complet : dépendance, configuration, paquet `search/`,
   indexation, synchronisation par l'outbox, repli sur JPA si l'index est indisponible.
2. Webhooks sortants, sur l'infrastructure de `features/notification`.
3. Import étendu, avec simulation, rapport d'erreurs et annulation.
4. Export généralisé et export programmé.
5. Export comptable des écritures du lot 2.
6. Exposition GraphQL du catalogue et des niveaux de stock.
7. Indicateurs : rotation, couverture, taux de service, ancienneté, exactitude, valeur dormante.
8. Tableaux de bord par rôle.

La recherche est le seul chantier réellement parallélisable : elle ne dépend que du modèle de données.

## 3. Décisions non tranchées

Elles changent le contenu des lots. Elles relèvent du métier.

| # | Décision | Impact |
|---|---|---|
| 3 | **Multi-devise.** Y a-t-il réellement des achats en devise ? | Si non, le lot 5 s'allège nettement |
| 4 | **Périmètre entrepôt.** Emplacements fins et tâches, ou quelques dépôts sans zonage ? | La plus grosse économie potentielle du plan : le lot 6 peut tomber à une fraction de son volume |
| 5 | **Mobilité hors ligne.** Client de ce projet, ou du projet BTP qui en aura besoin de toute façon ? | Détermine où vit la file de synchronisation |
| 6 | **Rupture d'API.** Quand passer les endpoints de mouvement en idempotence obligatoire, avec quel préavis ? | Casse les clients qui n'envoient pas de clé |
| 8 | **Chaîne de migrations de développement.** Six fichiers de `db/migration` échouent sur une base vierge. `db/migration-prod` rejoue intégralement. | Un nouveau poste ne peut pas créer sa base depuis les migrations dev. Chantier à part entière |

Le point 8 est détaillé, avec la liste des six fichiers et leurs erreurs, dans
[`../backend/inventory-lot0-verification.md`](../backend/inventory-lot0-verification.md).

## 4. Dette accumulée pendant les lots livrés

Quatre points relevés au fil de l'eau, chacun rattaché au lot qui le résoudra.

| Point | Où | Résolu par |
|---|---|---|
| La généalogie des lots a sa table et ses endpoints de lecture, mais aucun flux ne crée de lien parent-enfant | `StockLotGenealogy` | Lot 6, opérations d'assemblage |
| La notification des détenteurs lors d'un rappel n'est pas branchée | `ProductRecallServiceImpl.holdersOf` | Lot 9, webhooks et notifications |
| `GoodsReceiptLine.qualityAccepted` n'est pas rattaché à une inspection | `procurement/model/GoodsReceiptLine` | Lot 5, réceptions |
| La provision pour dépréciation n'est pas calculée, l'ancienneté des couches étant pourtant stockée | `StockValuationSnapshot.oldestLayerAgeDays` | Lot 9, indicateurs |
| L'échantillonnage qualité est stocké mais jamais calculé, la quantité contrôlée restant déclarée | `QualityControlPlan.samplingParameter` | Lot 4, avec les plans de comptage |
| La recherche article n'intègre pas les codes-barres : un scan d'EAN passe par `/inventory/scan` | `InventoryItemSpecification` | Lot 9, recherche unifiée |
| L'import d'articles n'accepte ni codes-barres ni conditionnements en colonnes | `InventoryImportService` | Lot 9, import étendu |

## 5. Contrat d'extension, à ne pas casser

Ce que les lots livrés garantissent au projet BTP, et que la suite doit préserver :

1. `StockService` et `InventoryConsumptionService` restent les seules portes d'écriture du stock, et
   acceptent `referenceType` plus `referenceCode` pour toute imputation externe.
2. `InventoryLocation` reste une arborescence libre, extensible par de nouveaux types.
3. `Asset` et `AssetAssignment` restent ouverts à de nouveaux `AssetAssigneeType`.
4. `StockOwnershipType` et son filtrage deviennent opérationnels au lot 8.
5. Tout objet est résolvable par `GET /inventory/scan/{code}`.
6. La valorisation est interrogeable à une date donnée.
7. Les événements de l'outbox portent un contrat stable, à documenter au lot 9.

## 6. Règles transverses à respecter à la reprise

Rappel des invariants posés, détaillés en section 3 du plan d'implémentation :

- **deux dossiers Flyway**, un fichier dans chacun, numérotations distinctes ;
- toute valeur ajoutée à un enum persisté impose de vérifier la contrainte `CHECK` correspondante ;
- aucun identifiant fonctionnel généré à la main, passage par `SequenceGeneratorFacade` ;
- tout endpoint mutant porte `@Idempotent`, nommé `INVENTORY_<DOMAINE>_<ACTION>` ;
- **toute énumération nouvelle est enregistrée** dans `InventoryReferenceServiceImpl` avec son
  libellé français dans `InventoryReferenceLabels` ;
- **toute valeur calculable par le serveur est proposée ou pré-remplie**, la saisie libre restant
  l'exception documentée ;
- montants en entiers XAF ;
- un lot sans test n'est pas terminé ;
- pas de tiret cadratin, ni dans le code, ni dans les messages de commit.
