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

- l'organisation qui paie est choisie par `billingCompanyCode` :
  - le code d'une organisation à laquelle le membre est rattaché ;
  - `NONE` pour le membre seul ;
  - vide pour l'organisation de son lien automatique, s'il en a une ;
- le stock des articles est retenu, sans échéance propre : c'est la commande qui expire ;
- une facture est émise au nom du membre, reliée à l'organisation si elle paie, une ligne par
  article ou prestation, avec `sourceType = MARKETPLACE_ORDER` ;
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
| `PAID` | réglée, elle attend son retrait (`stockPending: true` si un article n'est pas encore sorti du stock) |
| `FULFILLED` | remise à l'accueil (`fulfilledBy`, `collectedBy`) |
| `CANCELLED` | annulée (`cancelledBy`, `cancellationReason`, `creditNoteNumber` si remboursée) |
| `EXPIRED` | non soldée à temps : déjà réglé remboursé, stock rendu, facture annulée |

La commande passe en `PAID` dès que le paiement en ligne réussit. Un passage chaque minute
rattrape les autres règlements (caisse, portefeuille de l'organisation) et expire les commandes
échues.

À l'échéance :

- sans règlement, le stock est rendu et la facture annulée ;
- réglée en partie, le montant reçu est remboursé par un avoir, reversé au portefeuille du payeur ;
  le reste de la facture est annulé et le stock rendu (`creditNoteNumber` renseigné) ;
- un paiement en cours (réponse de l'opérateur mobile money attendue, depuis moins d'une heure) ou
  une dépense en attente du gérant suspend l'échéance, le temps qu'ils soient tranchés. `expiresAt`
  peut donc être dépassé sur une commande encore `PENDING_PAYMENT`.

### Sortie de stock

Le règlement n'est jamais remis en cause par le stock. Si la sortie d'un article échoue (stock
réel insuffisant à l'emplacement, par exemple) :

- la commande est bien `PAID`, avec `stockPending: true` et le motif dans `lines[].stockError` ;
- les comptes `SUPER_ADMIN`, `ADMIN` et `MANAGER` reçoivent un e-mail ;
- la sortie est reprise à chaque passage : une fois le stock corrigé, elle aboutit seule ;
- la remise au comptoir est refusée tant que la sortie n'a pas abouti.

À afficher côté administration : un badge sur `stockPending`, et le bouton de remise désactivé.
La liste à traiter s'obtient par `GET /marketplace/orders?stockPending=true`.

Le membre est prévenu par e-mail et dans le portail à chaque étape : validée, réglée, remise,
annulée ou expirée.

## Retrait et annulation

- **Retrait :** `fulfil` n'accepte qu'une commande réglée dont les articles sont sortis du stock.
  `collectedBy` est facultatif ; à défaut, le nom de l'acheteur est retenu.
- **Annulation par le membre :** seulement une commande sans aucun règlement reçu. Le stock est
  rendu et la facture annulée.
- **Annulation par l'administration :**
  - **non réglée :** comme le membre ;
  - **réglée :** un avoir du montant payé est validé puis reversé au portefeuille du payeur, et les
    articles reviennent à leur emplacement ;
  - **en partie réglée :** même remboursement du montant reçu, le reste de la facture est annulé et
    le stock rendu ;
  - **déjà remise :** même remboursement, mais retour en stock seulement si `returnToStock: true`.

Le payeur remboursé est l'organisation dès que la commande lui est facturée
(`billingCompanyCode` renseigné), quel que soit le moyen de paiement ; sinon le membre. L'avoir
reste au nom du membre, comme la facture.

Un retour en stock impossible (article suivi par lot ou numéro de série, période close) n'empêche
pas le remboursement : la commande est annulée, le motif reste dans `lines[].stockError`, et
l'équipe reçoit un e-mail pour saisir le retour à la main.

| Qui | Méthode et chemin | Effet |
| --- | --- | --- |
| Membre | `GET /client/marketplace/orders`, `/orders/{orderNumber}` | ses commandes |
| Membre | `POST /client/marketplace/orders/{orderNumber}/cancel` `{reason?}` | renonce à une commande non réglée |
| Gérant | `GET /client/companies/{customerId}/marketplace/orders` | les commandes que l'organisation paie |
| Admin, manager, agent | `GET /marketplace/orders?status=&limit=`, `/orders/{orderNumber}` | les commandes |
| Admin, manager, agent | `GET /marketplace/orders?stockPending=true` | les commandes réglées dont le stock est à corriger |
| Admin, manager, agent | `POST /marketplace/orders/{orderNumber}/fulfil` `{collectedBy?}` | remise au comptoir |
| Admin, manager | `POST /marketplace/orders/{orderNumber}/cancel` `{reason, returnToStock?}` | annule, rembourse si réglée |
