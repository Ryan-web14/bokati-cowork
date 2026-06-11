# Plan d'implémentation — Améliorations du module Inventory

## Contexte

Suite à l'audit du module (catalogue, stock, procurement, assets, intelligence — voir
échanges précédents), ce plan retient 5 chantiers concrets demandés en priorité :

1. Rendre les **suggestions de réapprovisionnement** plus intelligentes et plus riches
2. Compléter la **gestion des lots** (création explicite, proposition si absent)
3. Transformer la sortie d'un actif en véritable **fiche de prêt** documentée
4. Rendre **obligatoire la documentation** des sorties d'articles / mouvements de stock
5. Permettre la **saisie du numéro de série** quand l'article l'exige

Prochaine migration Flyway : **V127** (dernière existante :
`V126__task_parent_task_reference.sql`).

---

## Phase 1 — Suggestions de réapprovisionnement plus intelligentes et enrichies

### État actuel
`InventoryReorderRuleServiceImpl.suggestions(...)` calcule déjà : sévérité, score de
priorité, raison (`NO_STOCK_LEVEL/OUT_OF_STOCK/BELOW_MINIMUM/AT_MINIMUM/
BELOW_TARGET_MAX`), taux de consommation sur 30 jours et jours de stock restants. C'est
une bonne base, mais trois angles morts limitent sa pertinence :
- aucune prise en compte du **délai de livraison fournisseur** (lead time)
- aucune prise en compte des **commandes déjà en cours** (PR/PO ouvertes pour le même
  article/emplacement) → risque de sur-suggestion (sur-stock en double commande)
- la tendance de consommation est figée sur une fenêtre unique de 30 jours (pas de
  comparaison de période, pas de détection de tendance haussière/baissière)

### 1.1 Intégrer le délai de livraison fournisseur
- Ajouter `averageLeadTimeDays` sur `Supplier` (ou calculé depuis l'historique des
  `GoodsReceipt` vs `PurchaseOrder.orderedAt`, si la donnée est déjà disponible)
- Calculer une **date de commande recommandée** = date de rupture prévisionnelle −
  lead time, et l'exposer dans `ReorderSuggestionResponse.recommendedOrderByDate`
- Migration `V127__supplier_lead_time.sql`

### 1.2 Déduire les quantités déjà en commande
- Nouvelle requête agrégée sur `PurchaseOrderLine`/`PurchaseRequestLine` (statuts actifs :
  `SUBMITTED/APPROVED/ORDERED/PARTIALLY_RECEIVED`) → `quantityOnOrder` par
  article/emplacement
- `shortageQuantity` et `reorderQuantity` de la suggestion doivent être nets des
  quantités déjà en commande : `besoin = max(0, target - quantityAvailable -
  quantityOnOrder)`
- Exposer `quantityOnOrder` dans `ReorderSuggestionResponse` pour transparence

### 1.3 Détection de tendance de consommation
- Calculer le taux de consommation sur deux fenêtres glissantes (ex. 30j vs 30-60j) et
  exposer une `ConsumptionTrend` (`RISING/STABLE/FALLING`) + variation en %
- Pondérer `priorityScore` et `reorderQuantity` suggérée selon la tendance (une
  consommation en hausse justifie une quantité suggérée plus généreuse que la simple
  règle min/max statique)

### 1.4 Comparaison multi-fournisseurs dans la suggestion
- Si plusieurs fournisseurs sont liés à l'article (cf. besoin de modèle `SupplierItem`
  identifié dans l'audit — à créer si absent), enrichir la suggestion avec le meilleur
  couple prix/délai et l'exposer (`bestSupplierCode`, `bestSupplierPrice`,
  `bestSupplierLeadTimeDays`) plutôt que le seul `preferredSupplierCode` figé sur la règle

---

## Phase 2 — Gestion complète des lots

### État actuel
`StockServiceImpl.upsertLot(...)` crée silencieusement un lot — y compris un lot
fictif `"NO-LOT-" + itemCode` quand aucun numéro n'est fourni et que ce n'est pas
strictement requis. `validateLotInput` vérifie seulement la *présence* d'un numéro de
lot/date d'expiration quand l'article l'exige, mais ne vérifie jamais si **le lot
désigné existe réellement** lors d'une sortie/transfert — ce qui peut désynchroniser la
traçabilité (ex. taper un mauvais numéro de lot à la sortie crée un nouveau lot fantôme
au lieu de consommer le bon).

### 2.1 Lookup des lots existants
- `GET /inventory/items/{itemCode}/lots?locationCode=...&activeOnly=true` → liste des
  lots actifs avec quantité restante, date d'expiration (alimente un sélecteur côté
  frontend avant toute sortie/transfert)

### 2.2 Validation stricte à la consommation (sortie / transfert)
- Pour un article `requiresLotNumber = true` ou `trackingType = LOT/EXPIRY` :
  - si le `lotNumber` fourni correspond à un lot **existant et actif** sur l'emplacement
    → consommation normale (FIFO déjà en place via `LotConsumption`)
  - si le `lotNumber` ne correspond à **aucun lot existant** → réponse 409 explicite
    (`LOT_NOT_FOUND`) proposant deux options côté frontend : sélectionner un lot
    existant, ou confirmer la création d'un nouveau lot (flag explicite
    `confirmCreateLot=true` dans la requête, plutôt que la création silencieuse actuelle)
- Réserver la création silencieuse (`upsertLot` actuel) aux **entrées de stock**
  (réception), où créer un nouveau lot est l'opération normale

### 2.3 Endpoint de création explicite de lot
- `POST /inventory/items/{itemCode}/lots` (`StockLotCreateRequest(locationCode,
  lotNumber, expiryDate, initialQuantity, ownershipType, ownerCode)`) pour créer un lot
  indépendamment d'un mouvement (ex. régularisation, pré-enregistrement avant réception)

---

## Phase 3 — Fiche de prêt d'actif complète

### État actuel
`AssetAssignRequest`/`AssetAssignment` couvrent déjà : type/code du bénéficiaire, dates
de début et de retour attendu, qui a assigné, photo de remise, signature, notes. Il
manque cependant deux informations essentielles pour qu'il s'agisse d'une vraie **fiche
de prêt** : **le motif du prêt** (« pourquoi ») et le caractère **obligatoire** de « qui
a fait sortir ».

### 3.1 Compléter le modèle et la requête
- Ajouter `purpose` (motif du prêt, ex. « déplacement client », « réunion externe »,
  « remplacement matériel défectueux ») sur `AssetAssignment` et `AssetAssignRequest`
- Rendre `assigneeCode`, `assignedBy`, `expectedReturnAt` et `purpose` obligatoires
  (`@NotBlank`/`@NotNull`) dans `AssetAssignRequest` — aujourd'hui seuls `assigneeType`
  et `assigneeCode` sont requis
- Migration `V128__asset_assignment_purpose.sql`

### 3.2 Fiche de prêt imprimable / PDF
- `GET /inventory/assets/{assetCode}/assignments/{assignmentId}/loan-sheet.pdf` —
  document récapitulatif (qui, quoi, quand, pourquoi, retour prévu, qui a remis,
  signature) à faire signer/conserver, mirroring le pipeline OpenHtmlToPDF déjà utilisé
  pour les étiquettes/rapports du module (`InventoryAdminServiceImpl.movementReportPdf`)

### 3.3 Rappel avant échéance de retour
- `InventoryAlertType.ASSET_RETURN_OVERDUE` alerte déjà le **retard**, mais rien ne
  prévient **avant** l'échéance. Ajouter une vérification (dans `InventoryDailyWorker`)
  qui notifie l'emprunteur/le gestionnaire J-1 avant `expectedReturnAt`

---

## Phase 4 — Documentation obligatoire des sorties / mouvements de stock

### État actuel
`StockOutRequest` porte déjà `reason`, `performedBy`, `referenceType`, `referenceCode`,
mais **aucun de ces champs n'est obligatoire** — une sortie de stock peut donc être
enregistrée sans motif ni traçabilité de l'opérateur.

### 4.1 Rendre `reason` et `performedBy` obligatoires sur les mouvements sortants
- `@NotBlank` sur `reason` et `performedBy` dans `StockOutRequest` (et tout DTO
  équivalent pour ajustements/transferts négatifs — `StockTransferWorkflow` à vérifier)
- Pour les sorties liées à une référence métier (`referenceType` =
  `BOOKING/SUBSCRIPTION/...`), `referenceCode` devient également obligatoire — seules les
  sorties « libres » (consommation interne, casse, perte) exigent un `reason` détaillé en
  texte libre

### 4.2 Normaliser des motifs structurés
- Remplacer/compléter le `reason` texte libre par un `StockOutReasonCode` énuméré
  (`CONSUMPTION/DAMAGE/LOSS/INTERNAL_USE/SAMPLE/DONATION/OTHER`) + commentaire libre
  optionnel — facilite le reporting et l'analyse des causes de sortie
- Migration `V129__stock_movement_reason_code.sql`

---

## Phase 5 — Saisie du numéro de série

### État actuel
La saisie de numéros de série n'existe **qu'à la réception** et **uniquement pour les
articles de type `ASSET`** (`StockServiceImpl.createAssetsFromReceiptIfNeeded` via
`StockInRequest.assetSerialNumbers`). Le flag `InventoryItem.requiresSerialNumber` n'est
en revanche **jamais vérifié** par `validateLotInput`, et aucun mouvement (sortie,
transfert, comptage) ne permet de renseigner ou de consulter un numéro de série pour les
articles non-`ASSET` qui l'exigeraient (ex. `SPARE_PART` sérialisés).

### 5.1 Validation à l'entrée
- Étendre `validateLotInput` (ou une nouvelle `validateSerialInput`) : si
  `requiresSerialNumber = true` et que l'article n'est pas de type `ASSET`, exiger un ou
  plusieurs numéros de série correspondant à la quantité reçue (même logique de
  correspondance quantité ↔ liste de numéros que pour les assets)
- Modéliser le suivi (probablement une nouvelle table légère
  `inventory_item_serial(item_id, serial_number, location_id, status, lot_id)` plutôt que
  de surcharger `Asset`, qui est sémantiquement réservé aux immobilisations)
- Migration `V130__inventory_item_serial.sql`

### 5.2 Sélection du numéro de série à la sortie / au transfert
- `StockOutRequest`/`StockTransferRequest` : champ optionnel `serialNumbers` (liste),
  obligatoire et validé contre les numéros disponibles si `requiresSerialNumber = true`
- `GET /inventory/items/{itemCode}/serials?locationCode=...&status=AVAILABLE` → alimente
  le sélecteur côté frontend, sur le même principe que le lookup de lots (Phase 2.1)

---

## Décisions à valider

| Sujet | Option recommandée | Pourquoi |
|---|---|---|
| **Lot non trouvé à la sortie** | Réponse 409 explicite + confirmation requise pour créer (`confirmCreateLot=true`), pas de création silencieuse | Élimine les lots fantômes créés par erreur de saisie ; force une décision consciente |
| **Suivi des numéros de série hors `ASSET`** | Nouvelle table dédiée `inventory_item_serial` plutôt qu'extension du modèle `Asset` | `Asset` porte une sémantique de gestion d'immobilisation (maintenance, valeur résiduelle...) non pertinente pour une pièce détachée sérialisée simplement tracée |
| **Champ « motif » du prêt d'actif** | Texte libre `purpose` (pas d'énumération fermée au démarrage) | Les motifs de prêt sont très variés et propres à chaque structure ; une énumération figée serait vite incomplète. Réévaluable une fois l'usage observé |
| **Motif de sortie de stock** | Énumération `StockOutReasonCode` + commentaire libre optionnel | Permet le reporting agrégé (ex. % de pertes vs consommation normale) tout en gardant la souplesse d'un commentaire |

---

## Ordre de développement recommandé

| Ordre | Phase | Valeur | Effort | Dépendances |
|---|---|---|---|---|
| 1 | Phase 4 — Documentation obligatoire des sorties | Élevée (traçabilité immédiate, change peu de code) | Faible | Aucune |
| 2 | Phase 3 — Fiche de prêt d'actif complète | Élevée (structure déjà là, juste à compléter + PDF) | Faible-moyen | Pipeline PDF existant |
| 3 | Phase 2 — Gestion complète des lots | Élevée (fiabilise la traçabilité FEFO/FIFO) | Moyen | Aucune |
| 4 | Phase 5 — Saisie du numéro de série | Moyenne (dépend du nombre d'articles concernés) | Moyen | Nouvelle table dédiée |
| 5 | Phase 1 — Suggestions plus intelligentes | Moyenne-élevée (valeur analytique, pas de risque opérationnel) | Moyen-élevé | Données de lead time / commandes en cours |

---

## Prochaine étape

Valider les décisions ci-dessus, puis démarrer par la **Phase 4** (documentation
obligatoire des sorties de stock) : c'est le changement le plus simple à livrer (ajout de
contraintes de validation sur des champs déjà existants), et il pose immédiatement les
bases de traçabilité dont les phases suivantes (prêts, lots, numéros de série)
bénéficieront.
