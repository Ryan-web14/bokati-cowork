# inventory : parcours qualité

> Suite de [README.md](README.md). Routes exactes et champs JSON : [routes/InventoryQualityController.md](routes/InventoryQualityController.md). Code : `src/main/java/com/sni/bokaticowork/features/inventory/stock/service/implementation/QualityControlServiceImpl.java` et `ProductRecallServiceImpl.java`.

La qualité s'applique aux **lots**. Un article sans lot (ni suivi `LOT` ou `EXPIRY`, ni numéro de lot reçu) n'a pas de lot à inspecter ni à rappeler.

## Plans de contrôle

1. Liste : `GET /sni/api/v1/inventory/quality/plans` (triée par nom). Détail : `GET .../plans/{planCode}`.
2. Création : `POST /sni/api/v1/inventory/quality/plans` ([détail](routes/InventoryQualityController.md#r-post-sni-api-v1-inventory-quality-plans)) :
   - `name`, `controlStage` (`ON_RECEIPT`, `IN_STORAGE`, `BEFORE_ISSUE`, `ON_DEMAND`), `samplingMode` (`FULL`, `FIXED_QUANTITY`, `PERCENTAGE`), `samplingParameter` (nombre d'unités ou pourcentage, purement indicatif), `criteria` (au moins un) ;
   - **soit** `itemCode` **soit** `categoryCode`, jamais les deux ni aucun (`400` `A control plan targets either an item or a category, not both nor neither`) ;
   - `decisionOnFail` (défaut `QUARANTINED`) : décision appliquée quand un critère bloquant échoue ;
   - `quarantineOnReceipt` : si vrai et `controlStage = ON_RECEIPT`, chaque lot reçu de l'article (ou de la catégorie) est mis en quarantaine automatiquement en attendant l'inspection ;
   - chaque critère : `criterionCode`, `name`, `unit`, `minValue`, `maxValue` (bornes incluses), `booleanExpected` (critère oui ou non : conforme si la mesure vaut `true`), `blocking` (défaut `true`), `position`.
3. Aucune route de modification ni de désactivation : un plan se crée en une fois. Seuls les stades `ON_RECEIPT` ont un effet automatique (quarantaine à la réception, plan choisi par défaut à l'inspection) ; les autres stades sont descriptifs.

## Inspecter un lot

Pré-requis : le `lotId` ⚠, obtenu par `GET /sni/api/v1/inventory/traceability/{lotNumber}` (voir [parcours-stock.md](parcours-stock.md#lots)).

1. `POST /sni/api/v1/inventory/quality/lots/{lotId}/inspections` ([détail](routes/InventoryQualityController.md#r-post-sni-api-v1-inventory-quality-lots-lotid-inspections)), `201`, avec :
   - `planCode` facultatif : sans lui, le premier plan `ON_RECEIPT` applicable à l'article est utilisé ; s'il n'y en a pas, l'inspection est libre ;
   - `measures` (au moins une) : `criterionCode`, `measuredValue` (critère numérique) ou `measuredFlag` (critère oui ou non), `notes`. Un code qui n'est pas dans le plan est enregistré comme conforme et non bloquant ;
   - `sampledQuantity`, `conformQuantity`, `nonConformQuantity` (informatifs), `inspectedBy`.
2. Le serveur évalue chaque mesure. Une mesure numérique absente est **non conforme**.
3. Décision :
   - aucun critère bloquant en échec : `ACCEPTED` ;
   - échec et `acceptByDerogation: true` : `ACCEPTED_BY_DEROGATION`, à condition d'envoyer `decisionBy` **et** `decisionReason` (sinon `400` `A derogation requires both decisionBy and decisionReason`) ;
   - échec sans dérogation : `decisionOnFail` du plan, ou `QUARANTINED` sans plan.
4. Effets :
   - en cas d'échec, une non-conformité est ouverte automatiquement (`nonConformanceCode` dans la réponse), gravité `CRITICAL` si `REJECTED`, sinon `MAJOR` ;
   - `QUARANTINED` ou `REJECTED` : le lot est mis en quarantaine (`lotQuarantined: true` dans la réponse) ;
   - `ACCEPTED` ou `ACCEPTED_BY_DEROGATION` sur un lot en quarantaine : il en sort, sans double contrôle (contrairement à la libération manuelle). Voir l'écart 6 du [README](README.md#écarts-relevés) : rafraîchir les niveaux ne suffit pas toujours à voir le disponible remonter.
5. Historique du lot : `GET .../lots/{lotId}/inspections` (plus récente d'abord).

Écran conseillé : charger le plan, afficher un champ par critère (case à cocher si `booleanExpected`, sinon nombre avec bornes et unité), montrer en direct la conformité calculée, et ne proposer la dérogation qu'en cas d'échec bloquant.

## Non-conformités

1. Liste : `GET /sni/api/v1/inventory/quality/non-conformances?itemCode=&openOnly=` (page Spring, 20 par défaut).
2. Ouverture manuelle : `POST .../non-conformances` avec `itemCode`, `severity` (`MINOR`, `MAJOR`, `CRITICAL`), `description` (obligatoires), et `lotId` ⚠, `quantity`, `disposition` (`USE_AS_IS`, `REWORK`, `RETURN_TO_SUPPLIER`, `SCRAP`, `DOWNGRADE`, `PENDING` par défaut), `correctiveAction`, `responsibleCode`, `dueDate`, `supplierClaimCode`, `costImpact`, `detectedBy`.
3. Clôture : `PATCH .../non-conformances/{code}/close?closedBy=&correctiveAction=`. Refusée si déjà close ou si la disposition est `PENDING` (`400` `Set a disposition before closing non conformance X`).

**Important** : aucune route ne modifie la disposition. Une non-conformité ouverte en `PENDING` (toutes celles créées par une inspection) ne peut donc jamais être close (écart 4 du [README](README.md#écarts-relevés)). À l'ouverture manuelle, faire choisir la disposition dès la création ; pour les non-conformités automatiques, masquer le bouton « Clore » et l'expliquer.

La disposition n'a aucun effet sur le stock : mettre au rebut ou retourner au fournisseur se fait ensuite par une sortie ou un ajustement.

## Rappels produit

1. Préparer : `POST /sni/api/v1/inventory/quality/recalls` avec `itemCode`, `reason`, et une plage `lotNumberFrom`, `lotNumberTo` (facultatives, mises en majuscules). Bornes incluses, **comparaison alphabétique** des numéros (`LOT-10` est avant `LOT-9`) ; sans borne, tous les lots actifs de l'article. Statut `DRAFT`.
2. Vérifier : `GET .../recalls/{recallCode}` renvoie `affectedLotNumbers` (lots actuellement dans la plage) et `holders` (sorties et transferts non annulés qui ont emporté ces lots : `movementCode`, `locationCode`, `quantity`, `referenceType`, `referenceCode`, `issuedAt`). Afficher cette liste avant de lancer.
3. Lancer : `PATCH .../recalls/{recallCode}/launch?launchedBy=`. Doit être `DRAFT`, et la plage doit contenir au moins un lot (`400` sinon). Les lots sont bloqués (motif `RECALL`), `frozenLotCount`, `quantityInStock` et `quantityIssued` sont figés. Statut `ACTIVE`.
4. Suivre les retours : `PATCH .../recalls/{recallCode}/recovery?quantity=` (positive, rappel `ACTIVE`) **ajoute** la quantité à `quantityRecovered` ; `recoveryRate` est recalculé. Le stock n'est pas modifié : les retours physiques s'enregistrent à part (entrée ou ajustement).
5. Clore : `PATCH .../close?closedBy=`. Les lots **restent bloqués** : les débloquer, les sortir ou les rebuter ensuite par les routes de lot et de stock.
6. Annuler : `PATCH .../cancel?cancelledBy=` (tout statut sauf `CLOSED`) : débloque les lots de la plage bloqués pour `RECALL`.
7. Liste : `GET .../recalls` (plus récent d'abord ; `affectedLotNumbers` et `holders` vides dans la liste, présents seulement dans le détail et la réponse de lancement).

Aucune notification n'est envoyée aux détenteurs : la liste `holders` sert à les contacter hors du module.
