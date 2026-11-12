# inventory : parcours équipements

> Suite de [README.md](README.md). Routes exactes et champs JSON : [routes/AssetController.md](routes/AssetController.md), [routes/AssetMaintenanceController.md](routes/AssetMaintenanceController.md). Code : `src/main/java/com/sni/bokaticowork/features/inventory/asset/service/implementation/AssetServiceImpl.java`, `AssetMaintenanceServiceImpl.java`.

Un équipement (`Asset`) est un exemplaire physique d'un article de type `ASSET` (vidéoprojecteur, ordinateur portable, casque…), suivi à l'unité : où il est, à qui il est prêté, dans quel état, quelles maintenances.

## Lister et consulter

1. `GET /sni/api/v1/inventory/assets` avec `q` (texte libre), `itemCode`, `status`, `locationCode`, `assignedToType`, `assignedToCode` ; page Spring, tri `assetCode`.
2. `GET .../assets/{assetCode}` : fiche. `depreciatedValue` est calculé à la volée (amortissement linéaire mensuel entre `purchaseDate` et aujourd'hui sur `usefulLifeMonths`, plancher `residualValue` ; égal à `purchaseCost` sans durée de vie).
3. `GET .../assets/{assetCode}/location-history` : changements d'emplacement (création, modification de `locationCode`).
4. Le scan d'un code d'équipement, d'étiquette ou de numéro de série renvoie `resolvedType = ASSET` (voir [parcours-catalogue.md](parcours-catalogue.md#scan)).

## Créer

Deux chemins :

- **Par une entrée de stock** d'un article `ASSET` ([parcours-stock.md](parcours-stock.md#entrée-en-stock)) ou une réception d'achat : un équipement par unité, statut `AVAILABLE`, état `GOOD`, au coût unitaire reçu. Le stock de l'article augmente aussi.
- **Directement** : `POST /sni/api/v1/inventory/assets` avec `itemCode` (obligatoire, article de type `ASSET`, sinon `400`), `serialNumber`, `assetTag` (généré `TAG-...` si absent), `status` (défaut `AVAILABLE`), `condition` (défaut `GOOD`), `locationCode`, `purchaseDate`, `purchaseCost`, `warrantyEndDate`, `usefulLifeMonths`, `residualValue`, `notes`. `assetCode` est généré ; un code envoyé est ignoré. Ce chemin ne touche **pas** au stock de l'article.

Numéro de série et étiquette sont uniques parmi les équipements (`409`).

`PUT .../assets/{assetCode}` : remplacement complet ; un `status` envoyé est appliqué sans contrôle. Ne pas exposer le champ statut dans le formulaire de modification : utiliser les actions ci-dessous.

## Réserver

`PATCH .../assets/{assetCode}/reserve` avec `assigneeType` (`MEMBER`, `CUSTOMER`, `BUSINESS`, `USER`, `RESOURCE`), `assigneeCode` (obligatoires, code du bénéficiaire dans son module, normalisé), `reservedFrom` (défaut maintenant), `expectedReturnAt`, `reservedBy`, `notes`.

- L'équipement doit être `AVAILABLE`, sinon `400` `Asset is not available for reservation`. Il passe `RESERVED` **immédiatement**, même si `reservedFrom` est dans le futur : il ne peut plus être prêté à quelqu'un d'autre d'ici là.
- `expectedReturnAt` doit suivre `reservedFrom`.
- Annuler : `PATCH .../cancel-reservation?cancelledBy=&reason=` : retour à `AVAILABLE`.

## Prêter

`PATCH .../assets/{assetCode}/assign` ([détail](routes/AssetController.md#r-patch-sni-api-v1-inventory-assets-assetcode-assign)) avec `assigneeType`, `assigneeCode`, `expectedReturnAt`, `assignedBy`, `purpose` (tous obligatoires), et `startAt` (défaut maintenant), `checkoutPhotoUrl`, `receiverSignatureUrl`, `notes`.

- Équipement `AVAILABLE`, ou `RESERVED` **pour le même bénéficiaire** (la réservation devient le prêt). Sinon `400` `Asset is not available for assignment` ou `Reserved asset can only be assigned to the reserved assignee`.
- L'état de l'équipement au départ est enregistré (`checkoutCondition`). Statut `ASSIGNED`.
- Les photos et la signature sont des **adresses** (texte) : le module ne reçoit pas de fichier. Les téléverser ailleurs (module document) puis envoyer l'URL. Non vérifié : aucune route de téléversement dédiée dans ce module.
- Fiche de prêt PDF : `GET .../assets/{assetCode}/assignments/{assignmentId}/loan-sheet.pdf` existe mais l'`assignmentId` n'est fourni par aucune réponse (écart 3 du [README](README.md#écarts-relevés)) : le bouton ne peut pas être proposé en l'état.

## Restituer

`PATCH .../assets/{assetCode}/return` avec `returnedBy`, `returnCondition`, `returnPhotoUrl`, `notes` (tous facultatifs, mais un corps JSON est exigé : envoyer au moins `{}`). Il faut un prêt `ACTIVE` (`404` `No active asset assignment found` sinon ; une simple réservation s'annule, elle ne se restitue pas). L'état de retour devient l'état de l'équipement : `DAMAGED` ou `UNUSABLE` le passe `DAMAGED`, sinon `AVAILABLE`.

## Perte, casse, réforme

- `PATCH .../mark-damaged` : statut et état `DAMAGED`. Le prêt en cours n'est pas fermé (on peut encore enregistrer le retour).
- `PATCH .../mark-lost` : `LOST`, prêt en cours fermé (`CANCELLED`).
- `PATCH .../retire` : `RETIRED`, prêt en cours fermé.

Aucune de ces actions ne modifie le stock de l'article : prévoir, si besoin, une sortie de stock (motif `LOSS` ou `DAMAGE`) à la suite.

## Maintenance

1. Planifier : `POST /sni/api/v1/inventory/assets/{assetCode}/maintenances` avec `maintenanceType` (`PREVENTIVE`, `CORRECTIVE`, `WARRANTY`, `INSPECTION`), `scheduledAt`, `providerName`, `cost`, `description`. Statut `PLANNED`. Le statut de l'équipement ne change pas.
2. Liste : `GET .../assets/{assetCode}/maintenances`.
3. Démarrer : `PATCH /sni/api/v1/inventory/asset-maintenances/{maintenanceCode}/start`. Doit être `PLANNED`. Refus si l'équipement est prêté (`400` `Assigned asset must be returned before maintenance`). L'équipement passe `IN_MAINTENANCE`.
4. Terminer : `PATCH .../asset-maintenances/{maintenanceCode}/complete` avec `assetCondition` (défaut `GOOD`), `resolution`, `cost` (remplace le coût prévu). Doit être `IN_PROGRESS`. L'équipement passe `DAMAGED` si l'état est `DAMAGED` ou `UNUSABLE`, sinon `AVAILABLE`.
5. Annuler : `PATCH .../asset-maintenances/{maintenanceCode}/cancel`. Refus si `COMPLETED`. Une maintenance en cours annulée remet l'équipement `AVAILABLE`.

Points d'attention : le démarrage n'est refusé que pour un équipement prêté ; un équipement réservé, perdu ou réformé peut passer en maintenance, et en ressort `AVAILABLE` (la réservation reste alors `RESERVED` sans que l'équipement le soit). Griser « Démarrer » hors des statuts `AVAILABLE` et `DAMAGED`.

## Alertes liées

Toutes les 3 h, le worker ouvre `WARRANTY_SOON` (garantie qui finit dans 30 jours ou est déjà finie, y compris pour un équipement réformé), `MAINTENANCE_DUE` (maintenance `PLANNED` dans moins de 24 h ou en retard), `ASSET_RETURN_OVERDUE` (prêt dont `expectedReturnAt` est passé), `ASSET_RETURN_DUE_SOON` (retour attendu dans les 24 h). Une seule alerte `OPEN` par type et par équipement. Elles sont poussées sur `/topic/inventory/alerts` avec `assetCode`. Voir [parcours-achats.md](parcours-achats.md#alertes).
