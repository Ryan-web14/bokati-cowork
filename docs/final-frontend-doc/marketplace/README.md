# marketplace : fonctionnement

> Rédigé d'après le code. Routes exactes, droits et champs JSON : [routes.md](routes.md) et `routes/`. Règles communes : [README racine](../README.md).

## En bref

La marketplace vend dans le portail des articles de stock et des prestations du catalogue de facturation. Le membre remplit un panier, le valide (une facture est émise et le stock retenu), règle la facture par les moyens habituels du portail, puis retire sa commande à l'accueil : le retrait au comptoir est le seul mode de remise.

- **Back-office** (`/sni/api/v1/marketplace/**`) : gestion des offres (catalogue), suivi des commandes, remise au comptoir, annulation avec remboursement.
- **Portail membre** (`/sni/api/v1/client/marketplace/**`) : vitrine, panier, validation, suivi et annulation de ses commandes ; le gérant d'une organisation voit aussi les commandes que l'organisation paie.
- Modules liés : `inventory` (articles, emplacements, retenues et sorties de stock), `billing` (prestations du catalogue, facture, avoir, remboursement au portefeuille), `portal` (paiement de la facture, notifications du membre), `company` (organisation payeuse, portefeuille de l'organisation, gérant), `parametres` (délai de paiement).

Code : `src/main/java/com/sni/bokaticowork/features/marketplace/`. L'ancienne doc `docs/frontend/centre-entreprises/10-marketplace.md` a été relue contre le code : elle est juste, celle-ci la remplace.

## Notions

| Objet | Identifiant exposé | Description |
| --- | --- | --- |
| Offre (`OfferView`) | `id` (petit entier, à utiliser dans les chemins `/offers/{id}` et dans `offerId` du panier) et `offerCode` (`MKO-00001`, affichage seulement) | Ce que le portail vend. `kind = PRODUCT` : un article de stock (`inventoryItemCode`) vendu depuis **un** emplacement (`locationCode`). `kind = SERVICE` : une prestation du catalogue de facturation (`serviceItemCode`), sans stock. |
| Panier | aucun numéro (`orderNumber` vide, `id` vide tant qu'il est vide) | Une commande au statut `CART`. Un seul panier par membre (index unique en base). |
| Commande (`OrderView`) | `orderNumber` (`MKC-2026-000001`), attribué à la validation | Le panier validé. `buyerMemberCode` : l'acheteur, toujours le membre. `billingCompanyCode` : l'organisation qui paie, vide si le membre paie seul. `invoiceNumber` : la facture émise. `creditNoteNumber` : l'avoir de remboursement. |
| Ligne (`LineView`) | `id` (petit entier, à utiliser dans `/cart/lines/{lineId}`) | Une offre et sa quantité. `offerId` renvoie à l'offre. |

Les `id` de la marketplace sont des numéros courts tirés d'une séquence (`short_id`, `src/main/resources/db/migration/V273__marketplace.sql`) : ils ne posent pas le problème des grands entiers décrit dans le [README racine](../README.md#identifiants--attention-aux-grands-nombres).

**Prix.** `OfferView.price` est le prix demandé : le prix propre de l'offre (`ownPrice`), sinon le prix de vente de l'article (`salePrice`) ou le prix unitaire de la prestation. `ownPrice` vide signifie que l'offre suit l'article (`MarketplaceCatalogService.java:157`). Au panier, le prix d'une ligne suit l'offre en direct ; à la validation, il est figé sur la ligne avec son intitulé (`MarketplaceOrderService.java:255-258` et `:797-801`).

**Disponibilité.** `OfferView.available` et `LineView.available` (au panier seulement) donnent la quantité disponible à l'emplacement de l'offre ; vide pour une prestation, qui ne s'épuise pas (`MarketplaceCatalogService.java:171`).

**Devise.** Par défaut `XAF`. Une offre `SERVICE` prend la devise de la prestation si `currency` n'est pas donnée ; une offre `PRODUCT`, `XAF`.

## Statuts et cycle de vie

### Offre

Pas de statut, un booléen `published`.

| État | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `published: false` | Invisible du portail | création (`POST /sni/api/v1/marketplace/offers`, toujours non publiée), `POST .../offers/{id}/unpublish` | publiée |
| `published: true` | En vitrine si elle est aussi vendable (prix supérieur à zéro, article ou prestation encore actif) | `POST .../offers/{id}/publish` | non publiée |

Il n'y a pas de suppression d'offre. Retirer une offre de la vente ne touche pas aux commandes déjà validées ; une ligne restée au panier fera échouer la validation (« n'est plus en vente »).

### Commande (`MarketplaceOrder.Status`)

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `CART` | Panier modifiable | premier `POST /sni/api/v1/client/marketplace/cart/lines` | `PENDING_PAYMENT` (validation) |
| `PENDING_PAYMENT` | Validée : prix figés, stock retenu, facture émise. `expiresAt` donne l'échéance | `POST /sni/api/v1/client/marketplace/cart/checkout` | `PAID`, `CANCELLED`, `EXPIRED` |
| `PAID` | Facture soldée, stock sorti (ou sortie en reprise, voir `stockPending`), attend le retrait | automatique : facture soldée, vue après un paiement ou au passage périodique ; aussi à la remise ou à l'annulation par l'administration si le solde est arrivé entre-temps | `FULFILLED`, `CANCELLED` |
| `FULFILLED` | Remise à l'accueil (`fulfilledAt`, `fulfilledBy`, `collectedBy`) | `POST /sni/api/v1/marketplace/orders/{orderNumber}/fulfil` | `CANCELLED` (administration, avec remboursement) |
| `CANCELLED` | Annulée (`cancelledAt`, `cancelledBy`, `cancellationReason`, `creditNoteNumber` si remboursée) | annulation par le membre ou l'administration | aucun |
| `EXPIRED` | Non soldée à temps : le montant déjà reçu est remboursé, le stock rendu, la facture annulée. `cancelledBy = SYSTEM_MARKETPLACE` | automatique (worker) | aucun |

« Soldée » veut dire : un montant payé supérieur à zéro et un reste dû nul (`MarketplaceOrderService.java:721-724`). Un paiement partiel laisse la commande en `PENDING_PAYMENT`.

**Échéance suspendue.** Une commande `PENDING_PAYMENT` dont `expiresAt` est dépassé n'expire pas tant qu'un paiement est en cours (transaction en attente de réponse de l'opérateur depuis moins d'une heure) ou qu'une dépense sur le portefeuille de l'organisation attend la décision du gérant (`MarketplaceOrderService.java:534-539`). `expiresAt` peut donc être dans le passé sur une commande encore `PENDING_PAYMENT` : ne pas la présenter comme expirée.

**`stockPending`.** Vrai seulement en `PAID`, quand un article n'a pas encore pu sortir du stock (motif dans `lines[].stockError`). La remise est refusée tant qu'il est vrai ; la sortie est reprise à chaque passage du worker.

## Parcours

### Back-office : créer et publier une offre

1. Charger les listes de référence : articles `GET /sni/api/v1/inventory/items` et emplacements `GET /sni/api/v1/inventory/locations` (module `inventory`), ou prestations `GET /sni/api/v1/billing/catalog` (module `billing`).
2. `POST /sni/api/v1/marketplace/offers` ([détail](routes/MarketplaceAdminController.md#r-post-sni-api-v1-marketplace-offers)) avec `kind` et :
   - `PRODUCT` : `inventoryItemCode` **et** `locationCode` obligatoires ;
   - `SERVICE` : `serviceItemCode` obligatoire.
   Facultatifs : `title` (sinon le nom de l'article ou de la prestation), `description`, `imageUrl`, `category` (repris de la prestation s'ils manquent), `price` (sinon le prix de l'article ou de la prestation), `currency`, `maxPerOrder`, `displayOrder` (0 par défaut).
3. Réponse `201` : l'offre, `published: false`. Afficher `price` (prix effectif), `ownPrice` et `available`.
4. `POST /sni/api/v1/marketplace/offers/{id}/publish` ([détail](routes/MarketplaceAdminController.md#r-post-sni-api-v1-marketplace-offers-id-publish)). Refus `400` si le prix effectif est nul ou absent, ou si l'article ou la prestation n'est plus actif.

### Back-office : modifier ou retirer une offre

1. `GET /sni/api/v1/marketplace/offers?published=&category=` : le catalogue, trié par `displayOrder` puis titre. `category` est comparée sans tenir compte de la casse.
2. `PATCH /sni/api/v1/marketplace/offers/{id}` ([détail](routes/MarketplaceAdminController.md#r-patch-sni-api-v1-marketplace-offers-id)) : n'envoyer que ce qui change. `resetPrice: true` efface le prix propre (l'offre suit l'article) et l'emporte sur `price` ; `clearMaxPerOrder: true` efface le plafond et l'emporte sur `maxPerOrder`. Ce que l'offre vend (`kind`, article, emplacement, prestation) ne se modifie pas : une autre chose est une autre offre. Une offre publiée est revérifiée : `400` si elle n'a plus de prix.
3. `POST /sni/api/v1/marketplace/offers/{id}/unpublish` pour la retirer de la vitrine.

### Portail : vitrine et panier

1. `GET /sni/api/v1/client/marketplace/offers?category=` ([détail](routes/MarketplaceClientController.md#r-get-sni-api-v1-client-marketplace-offers)) : offres publiées **et** vendables. Une offre `PRODUCT` sans stock y figure quand même avec `available: 0` : désactiver l'ajout.
2. `GET /sni/api/v1/client/marketplace/cart` : le panier. S'il n'existe pas, un panier vide est renvoyé (`id` et `orderNumber` vides, `lines: []`, `currency: "XAF"`), jamais un `404`.
3. `POST /sni/api/v1/client/marketplace/cart/lines` avec `{ "offerId": <OfferView.id>, "quantity": n }` (n au moins 1). Si l'offre est déjà au panier, la quantité **s'additionne**. Réponse : le panier complet.
4. `PATCH /sni/api/v1/client/marketplace/cart/lines/{lineId}` avec `{ "quantity": n }` : remplace la quantité ; `0` retire la ligne. `DELETE /sni/api/v1/client/marketplace/cart/lines/{lineId}` retire la ligne. `lineId` est `LineView.id`. Les deux renvoient le panier.
5. Afficher par ligne `unitPrice`, `amount`, `available`, et `totalAmount` pour le panier : ces montants suivent les prix courants jusqu'à la validation.

### Portail : valider le panier et payer

1. Choisir qui paie. `POST /sni/api/v1/client/marketplace/cart/checkout` ([détail](routes/MarketplaceClientController.md#r-post-sni-api-v1-client-marketplace-cart-checkout)), corps facultatif `{ "billingCompanyCode": ... }` :
   - absent ou vide : l'organisation du lien automatique du membre s'il en a une (lien actif en mode de facturation `AUTOMATIC`), sinon le membre ;
   - `"NONE"` : le membre seul ;
   - un code d'organisation : celle-ci, à condition que le membre y ait un lien actif, sinon `400`.
   La liste des organisations du membre vient du module `portal`/`company`.
2. Réponse : la commande en `PENDING_PAYMENT`, avec `orderNumber`, `invoiceNumber`, `invoiceBalanceDue`, `expiresAt` (validation plus le délai `bokati.marketplace.payment-window-minutes`, 30 minutes par défaut, réglable à chaud). Afficher un compte à rebours sur `expiresAt`.
3. Payer la facture `invoiceNumber` avec les routes de paiement du portail (module [portal](../portal/routes.md)) : `POST /sni/api/v1/client/billing/invoices/{documentNumber}/pay/wallet`, `.../pay/mobile-money`, `.../pay/cash` (annonce de paiement en espèces), ou, si une organisation paie, `POST /sni/api/v1/client/me/company-wallets/{customerId}/invoices/{documentNumber}/pay` (dépense de type `MARKETPLACE` : l'autorisation de dépense du collaborateur doit inclure cette opération) ou les routes `/sni/api/v1/client/companies/{customerId}/invoices/{documentNumber}/pay/...` pour le gérant.
4. Recharger `GET /sni/api/v1/client/marketplace/orders/{orderNumber}` après le paiement : le passage en `PAID` est immédiat quand le paiement passe par une transaction du module paiement ; sinon il intervient au prochain passage du worker (60 s par défaut). Prévoir un rafraîchissement ou s'appuyer sur la notification du membre.

### Portail : suivre et annuler ses commandes

1. `GET /sni/api/v1/client/marketplace/orders?limit=` : ses commandes hors panier, des plus récentes aux plus anciennes (`limit` 50 par défaut, borné entre 1 et 200, pas de pagination).
2. `GET /sni/api/v1/client/marketplace/orders/{orderNumber}` : le détail ; une commande d'un autre membre répond `404`.
3. `POST /sni/api/v1/client/marketplace/orders/{orderNumber}/cancel`, corps facultatif `{ "reason": "..." }` : possible seulement en `PENDING_PAYMENT` **et** sans aucun règlement reçu. Le stock est rendu et la facture annulée. Sinon `400` : renvoyer le membre vers l'accueil.

### Portail : commandes payées par l'organisation (gérant)

1. `GET /sni/api/v1/client/companies/{customerId}/marketplace/orders?limit=` (100 par défaut, borné entre 1 et 500) : les commandes dont `billingCompanyCode` est l'organisation. Réservé au membre qui a un lien actif de rôle `OWNER` sur cette organisation ; sinon `404` « Organisation introuvable ».

### Back-office : comptoir (remise)

1. `GET /sni/api/v1/marketplace/orders?status=PAID&limit=` : les commandes à remettre (100 par défaut, borné entre 1 et 500, tri par date de création décroissante). `GET /sni/api/v1/marketplace/orders/{orderNumber}` pour une commande précise (recherche par numéro).
2. Si `stockPending: true`, afficher un badge et désactiver la remise : montrer `lines[].stockError` et inviter à corriger le stock de l'emplacement (module `inventory`).
3. `POST /sni/api/v1/marketplace/orders/{orderNumber}/fulfil` ([détail](routes/MarketplaceAdminController.md#r-post-sni-api-v1-marketplace-orders-ordernumber-fulfil)), corps facultatif `{ "collectedBy": "..." }` (qui retire, s'il n'est pas l'acheteur ; à défaut le nom de l'acheteur est retenu). Une commande encore `PENDING_PAYMENT` mais dont la facture vient d'être soldée passe d'abord en `PAID`, puis est remise.

### Back-office : stock à régulariser

1. `GET /sni/api/v1/marketplace/orders?stockPending=true` : les commandes réglées dont un article n'est pas sorti du stock. Le paramètre `status` est alors ignoré.
2. Après correction du stock dans `inventory`, la sortie aboutit seule au passage suivant ; la remise la retente aussi immédiatement.

### Back-office : annuler une commande

`POST /sni/api/v1/marketplace/orders/{orderNumber}/cancel` ([détail](routes/MarketplaceAdminController.md#r-post-sni-api-v1-marketplace-orders-ordernumber-cancel)) avec `{ "reason": "...", "returnToStock": true|false }` (`reason` obligatoire, 500 caractères au plus). Effet selon l'état (`MarketplaceOrderService.java:423-444`) :

| État | Effet |
| --- | --- |
| `PENDING_PAYMENT` sans règlement | facture annulée, stock rendu |
| `PENDING_PAYMENT` réglée en partie | avoir du montant reçu, reversé au portefeuille du payeur ; reste de la facture annulé ; stock rendu |
| `PENDING_PAYMENT` soldée entre-temps, ou `PAID` | avoir du montant payé, reversé au portefeuille ; stock rendu |
| `FULFILLED` | avoir et remboursement ; retour en stock seulement si `returnToStock: true` |
| `CANCELLED`, `EXPIRED` | `400` |

Le portefeuille remboursé est celui de l'organisation dès que `billingCompanyCode` est renseigné (quel que soit le moyen de paiement utilisé), sinon celui du membre. L'avoir reste au nom du membre, comme la facture. Afficher ensuite `creditNoteNumber`.

## Règles et erreurs à refléter dans l'écran

Erreurs métier en `400` (`BadRequestException`), introuvables en `404` (`ResourceNotFoundException`). Messages en français, à afficher tels quels.

| Situation | Statut | Message (extrait) |
| --- | --- | --- |
| Offre `PRODUCT` sans article ou emplacement | 400 | « Un article se vend depuis un emplacement : précisez l'article et l'emplacement. » |
| Offre `SERVICE` sans prestation | 400 | « Précisez la prestation du catalogue que l'offre vend. » |
| Article, emplacement, prestation, offre inconnus | 404 | « Article introuvable : X », « Emplacement introuvable : X », « Prestation introuvable : X », « Offre introuvable : id » |
| Publication sans prix effectif supérieur à zéro | 400 | « L'offre « … » n'a pas de prix : donnez-lui le sien, ou un prix de vente à l'article ou à la prestation. » |
| Article ou prestation désactivé | 400 | « L'article X n'est plus actif. » / « La prestation X n'est plus active. » |
| Ajout ou validation d'une offre non vendable | 400 | « « … » n'est plus en vente. » |
| Quantité au-delà de `maxPerOrder` | 400 | « « … » se commande par N au plus. » |
| Quantité au-delà du stock disponible | 400 | « Il ne reste que N « … » en stock. » |
| Validation d'un panier vide | 400 | « Votre panier est vide. » |
| Modification d'une ligne sans panier | 404 | « Votre panier est vide. » |
| Ligne inconnue | 404 | « Ligne introuvable dans votre panier : id » |
| Panier en plusieurs devises | 400 | « Votre panier mêle plusieurs devises : validez-le en plusieurs commandes. » |
| Organisation payeuse non rattachée | 400 | « X n'est pas rattaché à l'organisation Y : la facture ne peut pas être émise pour son compte. » |
| Annulation membre hors `PENDING_PAYMENT` | 400 | « La commande … ne s'annule plus depuis le portail : elle est … Adressez-vous à l'accueil. » |
| Annulation membre après un règlement partiel | 400 | « Un règlement a déjà été reçu pour la commande … : seul l'accueil peut l'annuler. » |
| Remise d'une commande non réglée ou déjà remise | 400 | « La commande … ne se remet pas : elle est en attente de règlement / déjà remise / annulée / expirée. » |
| Remise avec stock non sorti | 400 | « La commande … ne se remet pas encore : la sortie de stock de … n'a pas abouti. Corrigez le stock de l'emplacement, puis recommencez. » |
| Annulation back-office d'une commande close | 400 | « La commande … ne s'annule pas : elle est annulée / expirée. » |

Validations de champs (`400 VALIDATION_FAILED`) : `title` 200 caractères, `imageUrl` 500, `category` 120, `price` positif ou nul, `currency` exactement 3 caractères, `maxPerOrder` strictement positif, `quantity` au moins 1 à l'ajout et au moins 0 en modification, `collectedBy` 200, `reason` 500 (obligatoire et non vide côté back-office).

Contrôles à anticiper : plafond `maxPerOrder` et `available` sur la quantité **cumulée** de la ligne (l'ajout s'additionne à l'existant) ; une seule devise par panier.

## À prévoir côté frontend

**Écrans back-office**

- Catalogue des offres (liste filtrable par `published` et `category`, création, modification, publication). Créer, modifier, publier, dépublier : rôle `ADMIN`, `SUPER_ADMIN` ou `MANAGER`. Lire : ces rôles plus `OPERATIONS_AGENT`. Toutes les routes exigent en plus le droit `ADMIN:ACCESS` (règle d'URL), voir [routes.md](routes.md).
- Commandes (liste par statut, filtre `stockPending`, détail). Lire et remettre (`fulfil`) : `ADMIN`, `SUPER_ADMIN`, `MANAGER`, `OPERATIONS_AGENT`. **Annuler : `ADMIN`, `SUPER_ADMIN`, `MANAGER` seulement** (pas l'agent d'accueil) : masquer le bouton en conséquence.
- Le contrôle porte sur des **rôles** (`hasRole`), pas sur des droits : comparer avec les entrées `ROLE_...` de la liste `roles` reçue à la connexion.

**Écrans portail**

- Vitrine (regroupement par `category`, ordre déjà fourni), panier, validation avec choix du payeur, détail de commande avec compte à rebours et boutons de paiement, liste des commandes, vue gérant des commandes de l'organisation.

**Listes de référence** : articles et emplacements (`inventory`), prestations (`billing`), pour le formulaire d'offre ; organisations du membre (`portal`/`company`) pour le choix du payeur.

**Pas de pagination** : les listes sont bornées par `limit`. Pas de recherche texte côté serveur.

**Images** : `imageUrl` est une simple URL texte, il n'y a pas de téléversement dans ce module.

**Temps réel** : aucun sujet WebSocket propre à la marketplace. Les changements d'état arrivent au membre en notification du portail (`GET /sni/api/v1/client/notifications`, module `portal`) ; côté back-office, recharger la liste.

**Pièges**

- `offerId` et `lineId` sont les `id` des vues (numéros courts), pas `offerCode`.
- `POST /cart/lines` additionne : pour fixer une quantité, utiliser `PATCH /cart/lines/{lineId}`.
- Le panier montre les prix courants ; seule la commande validée a des montants figés.
- `expiresAt` dépassé ne veut pas dire expirée (échéance suspendue par un paiement en cours).
- `invoiceBalanceDue` est lu sur la facture à chaque appel : c'est la valeur à afficher pour le reste à payer.
- `GET /sni/api/v1/marketplace/orders?status=CART` renvoie toujours une liste vide : les paniers ne sont jamais listés côté back-office.

## Effets automatiques

- **Worker** `MarketplaceOrderWorker.syncPendingOrders` (`src/main/java/com/sni/bokaticowork/features/marketplace/worker/MarketplaceOrderWorker.java:31`) : toutes les 60 s (`bokati.marketplace.sync-delay-ms`, premier passage après 60 s), par lots de 100. Pour chaque commande `PENDING_PAYMENT` : passage en `PAID` si la facture est soldée, sinon expiration si l'échéance est dépassée et qu'aucun paiement n'est en cours. Pour chaque commande `PAID` avec stock en attente : nouvelle tentative de sortie.
- **Après un paiement réussi** dont la source est une commande marketplace, le module paiement synchronise la commande juste après la validation de la transaction (`src/main/java/com/sni/bokaticowork/features/payment/service/support/PaymentTransactionWorkflowProcessor.java:205`).
- **Passage en `PAID`** : sortie de stock des articles. Si elle échoue, la commande reste payée, `stockError` est renseigné et les comptes actifs `SUPER_ADMIN`, `ADMIN` et `MANAGER` reçoivent un courriel (une seule fois par ligne en échec).
- **Annulation ou expiration** : facture annulée et archivée, avoir et remboursement au portefeuille si un montant a été reçu, stock rendu. Un retour en stock impossible (article suivi par lot ou numéro de série, période close) n'empêche pas l'annulation : `stockError` est renseigné et l'équipe reçoit un courriel pour saisir le retour à la main.
- **À la validation** : la facture est envoyée par courriel au membre après validation de la transaction.
- **Membre prévenu** par courriel et par notification du portail à chaque étape, avec un type d'événement : `MARKETPLACE_ORDER_PLACED`, `MARKETPLACE_ORDER_PAID`, `MARKETPLACE_ORDER_FULFILLED`, `MARKETPLACE_ORDER_CANCELLED`, `MARKETPLACE_ORDER_REFUNDED`, `MARKETPLACE_ORDER_EXPIRED`. Charge utile de la notification : `orderNumber`, `status`, `invoiceNumber`, `message` (`MarketplaceOrderService.java:764-773`). Le lien du courriel pointe vers le chemin portail `/marketplace/orders`.

Non vérifié : quels moyens de paiement produisent une transaction du module paiement (et donc la synchronisation immédiate). La caisse et le portefeuille de l'organisation sont rattrapés par le worker d'après le commentaire de `MarketplaceOrderWorker.java:17`.

## Écarts relevés

1. **Annulation pendant un paiement en cours.** `cancelByMember` (`src/main/java/com/sni/bokaticowork/features/marketplace/service/MarketplaceOrderService.java:397-415`) et la branche sans règlement de `cancelByStaff` (`:427-436`) ne vérifient pas `paymentUnderway` (`:534`), contrairement à l'expiration. Un membre peut annuler pendant qu'un paiement mobile money attend la réponse de l'opérateur, ou qu'une dépense attend le gérant ; la facture est alors annulée alors que le paiement peut encore aboutir. Côté écran, éviter de proposer l'annulation juste après le lancement d'un paiement.
2. **Vitrine sans contrôle de stock.** `storefront` filtre sur `sellable` (`src/main/java/com/sni/bokaticowork/features/marketplace/service/MarketplaceCatalogService.java:49-54`, `:186-199`), qui ignore le stock : une offre `PRODUCT` épuisée reste affichée et l'ajout échoue en `400` « Il ne reste que 0 ».
3. **Total d'un panier multi-devises.** L'ajout au panier accepte des offres de devises différentes (devise du panier fixée par la première offre, `MarketplaceOrderService.java:187-191`) et `view` additionne les montants sans tenir compte de la devise (`:793-804`) ; l'erreur n'apparaît qu'à la validation.
4. **`status=CART` accepté mais toujours vide** côté back-office : la requête exclut les paniers (`src/main/java/com/sni/bokaticowork/features/marketplace/repository/MarketplaceOrderRepository.java:45-51`).
5. **Course à la création du panier.** Deux premiers ajouts simultanés peuvent tenter de créer deux paniers (`MarketplaceOrderService.java:186-191`) ; l'index unique `uk_marketplace_order_cart` en refuse un. Non vérifié : le statut HTTP renvoyé dans ce cas.
6. **Pas d'accès aux offres par code.** Les routes d'offre ne prennent que l'`id` court (`MarketplaceAdminController.java:46-76`) ; `offerCode` n'est utilisable qu'à l'affichage.
7. **Liste `stockPending` non triée** (`MarketplaceOrderRepository.java:69-74`) : l'ordre de la liste varie d'un appel à l'autre.
