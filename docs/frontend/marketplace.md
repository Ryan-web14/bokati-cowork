# Marketplace

Le portail vend des articles de stock et des prestations du catalogue. Le membre remplit un
panier, le valide, règle la facture, puis retire sa commande à l'accueil. Le retrait au comptoir
est le seul mode de remise.

Toutes les routes sont préfixées par `/sni/api/v1`.

## Offres

Une offre vend :

- soit un article de stock depuis un seul emplacement (`kind: PRODUCT`, `inventoryItemCode`,
  `locationCode`) ;
- soit une prestation du catalogue, sans stock (`kind: SERVICE`, `serviceItemCode`).

Le prix vient de l'article (`salePrice`) ou de la prestation (`unitPrice`). L'offre peut avoir son
propre prix (`price`) ; `resetPrice: true` lui rend celui de l'article.

Le titre, la description, l'image et la catégorie se reprennent de la prestation quand ils ne
sont pas donnés. Ce que l'offre vend, et d'où, ne change plus après la création : une autre chose
est une autre offre.

Une offre naît non publiée. Pour la publier, il lui faut un prix supérieur à zéro et un article ou
une prestation encore actif. `maxPerOrder` plafonne la quantité par commande.

Dans `OfferView` :

- `price` est le prix demandé ;
- `ownPrice` est le prix propre à l'offre (vide si elle suit l'article) ;
- `available` est le stock disponible à l'emplacement (vide pour une prestation).

| Qui | Méthode et chemin | Effet |
| --- | --- | --- |
| Admin, manager, agent | `GET /marketplace/offers?published=&category=` | le catalogue |
| Admin, manager, agent | `GET /marketplace/offers/{id}` | une offre |
| Admin, manager | `POST /marketplace/offers` | crée une offre, non publiée |
| Admin, manager | `PATCH /marketplace/offers/{id}` | titre, description, image, catégorie, prix (`resetPrice`), plafond (`clearMaxPerOrder`), ordre |
| Admin, manager | `POST /marketplace/offers/{id}/publish`, `/unpublish` | met en vente ou retire |
| Membre | `GET /client/marketplace/offers?category=` | la vitrine : offres publiées et vendables |

## Panier et validation

Un membre a un seul panier. Au panier, le prix suit l'offre ; à la validation, il est figé sur la
ligne.

La quantité d'une offre déjà présente s'additionne. Elle est refusée au-delà de `maxPerOrder` ou
du stock disponible. Mettre une quantité à `0` retire la ligne.

À la validation (`checkout`) :

- le payeur est choisi par `billingCompanyCode` :
  - le code d'une organisation à laquelle le membre est rattaché ;
  - `NONE` pour le membre seul ;
  - vide pour l'organisation de son lien automatique, s'il en a une ;
- le stock des articles est retenu, sans échéance propre : c'est la commande qui expire ;
- une facture est émise au nom du payeur, une ligne par article ou prestation, avec
  `sourceType = MARKETPLACE_ORDER` ;
- la commande passe en `PENDING_PAYMENT`. Elle a 30 minutes pour être réglée
  (`bokati.marketplace.payment-window-minutes`) ; `expiresAt` donne l'échéance.

Le règlement se fait comme pour toute facture : paiement en ligne, portefeuille du membre, caisse,
ou portefeuille de l'organisation. Sur le portefeuille de l'organisation, la dépense est de type
`MARKETPLACE` : l'autorisation du collaborateur doit inclure cette opération.

| Qui | Méthode et chemin | Effet |
| --- | --- | --- |
| Membre | `GET /client/marketplace/cart` | le panier (vide si aucun) |
| Membre | `POST /client/marketplace/cart/lines` `{offerId, quantity}` | ajoute |
| Membre | `PATCH /client/marketplace/cart/lines/{lineId}` `{quantity}` | change la quantité, `0` retire |
| Membre | `DELETE /client/marketplace/cart/lines/{lineId}` | retire |
| Membre | `POST /client/marketplace/cart/checkout` `{billingCompanyCode?}` | valide : facture émise, stock retenu |

## Cycle de la commande

| Statut | Sens |
| --- | --- |
| `CART` | le panier |
| `PENDING_PAYMENT` | validée, facture à régler (`invoiceBalanceDue` dans la vue) |
| `PAID` | réglée, le stock est sorti, elle attend son retrait |
| `FULFILLED` | remise à l'accueil (`fulfilledBy`, `collectedBy`) |
| `CANCELLED` | annulée (`cancelledBy`, `cancellationReason`, `creditNoteNumber` si remboursée) |
| `EXPIRED` | non réglée à temps : stock rendu, facture annulée |

La commande passe en `PAID` dès que le paiement en ligne réussit. Un passage chaque minute
rattrape les autres règlements (caisse, portefeuille de l'organisation) et expire les commandes
échues.

Une commande échue mais déjà réglée en partie n'expire pas : elle garde son stock et attend la
suite du règlement.

Le membre est prévenu par e-mail et dans le portail à chaque étape : validée, réglée, remise,
annulée ou expirée.

## Retrait et annulation

- **Retrait :** `fulfil` n'accepte qu'une commande réglée. `collectedBy` est facultatif ; à
  défaut, le nom de l'acheteur est retenu.
- **Annulation par le membre :** seulement une commande non réglée, sans aucun règlement reçu. Le
  stock est rendu et la facture annulée.
- **Annulation par l'administration :**
  - **non réglée :** comme le membre ;
  - **réglée :** un avoir du montant payé est validé puis reversé au portefeuille du payeur (le
    membre ou l'organisation), et les articles reviennent à leur emplacement ;
  - **déjà remise :** même remboursement, mais retour en stock seulement si `returnToStock: true` ;
  - **en partie réglée :** refusée. Remboursez d'abord le paiement reçu.

| Qui | Méthode et chemin | Effet |
| --- | --- | --- |
| Membre | `GET /client/marketplace/orders`, `/orders/{orderNumber}` | ses commandes |
| Membre | `POST /client/marketplace/orders/{orderNumber}/cancel` `{reason?}` | renonce à une commande non réglée |
| Gérant | `GET /client/companies/{customerId}/marketplace/orders` | les commandes que l'organisation paie |
| Admin, manager, agent | `GET /marketplace/orders?status=&limit=`, `/orders/{orderNumber}` | les commandes |
| Admin, manager, agent | `POST /marketplace/orders/{orderNumber}/fulfil` `{collectedBy?}` | remise au comptoir |
| Admin, manager | `POST /marketplace/orders/{orderNumber}/cancel` `{reason, returnToStock?}` | annule, rembourse si réglée |
