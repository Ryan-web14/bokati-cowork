# subscription : parcours des promotions, coupons et parrainages

> Partie de [README.md](README.md). Routes exactes et champs : [routes.md](routes.md). Règles communes : [README racine](../README.md).

Le calcul des prix suit un ordre fixe : prix du catalogue, grille tarifaire éventuelle (remplace le prix sans compter comme une remise), prix négocié d'une dérivation (qui bloque les promotions sauf `promotionsAllowed`), promotions automatiques par priorité, coupons saisis par priorité, plafonds (`maxDiscountAmount`, budget de la campagne), puis arrondi final. Chaque remise se calcule sur le prix d'origine : elles ne se composent pas. Une promotion non cumulable est refusée si une remise est déjà retenue ; une promotion exclusive arrête l'évaluation.

Rien n'est consommé à l'affichage : évaluer un prix ne dépense ni budget ni coupon. Les remises sont confirmées à la création de l'abonnement (champ `couponCodes` de `POST /sni/api/v1/subscriptions`).

Lire d'abord la [réserve sur les récompenses](README.md#écarts-relevés) : une promotion créée par l'API n'a pas de récompense, et le moteur la rejette avec « Cette promotion n'accorde rien ».

## Promotions

Écran : « Promotions » (droits `BILLING:READ`, `BILLING:CREATE`, `BILLING:UPDATE`).

1. Lister : `GET /sni/api/v1/promotions` (filtres `code`, `name`, `status`), tri par défaut `createdAt,DESC`.
2. Créer : `POST /sni/api/v1/promotions` ([route](routes/PromotionController.md#r-post-sni-api-v1-promotions)) avec `name`, `discountType` (`PERCENTAGE`, `FIXED_AMOUNT`, `FREE_ENTITLEMENT`, `WAIVE_SETUP_FEE` ; `FREE_TRIAL_DAYS` est refusé en `400`), `discountValue` (≥ 0), `startsAt` (obligatoire), `endsAt`, `maxRedemptions`, `description`, `metadataJson`.
   - `startsAt` et `endsAt` sont du **texte** : `AAAA-MM-JJ` (début à 00:00 UTC, fin à 23:59:59 UTC), ou date-heure ISO avec `Z` ou décalage, ou date-heure sans fuseau lue en UTC. Fin avant début : `400`.
   - Le `code` est généré. Statut `DRAFT`.
3. Activer : `PATCH /sni/api/v1/promotions/{code}/activate` (tout statut). Une promotion active dont `endsAt` est passée devient `EXPIRED` (worker toutes les 15 min).
4. Enregistrer manuellement une utilisation : `POST /sni/api/v1/promotions/{code}/redeem` avec `subscriberType`, `subscriberCode`, `subscriptionNumber` facultatif. `404` si la promotion n'est pas utilisable maintenant, `409` si `maxRedemptions` est atteint ou si ce souscripteur l'a déjà utilisée. Cette route compte l'utilisation ; elle ne calcule ni n'applique de remise.

Pas de route de modification, de pause ni d'archivage.

## Coupons

Écran : onglet « Codes » d'une promotion (droit `ADMIN:ACCESS`).

1. Générer un lot : `POST /sni/api/v1/coupons/batches?createdBy=...` ([route](routes/CouponController.md#r-post-sni-api-v1-coupons-batches)) avec `promotionCode`, `name`, `couponKind` (`SINGLE_USE`, `MULTI_USE`, `UNIQUE_PER_SUBSCRIBER`) obligatoires, `quantity` (1 à 50 000), `codePrefix`, `codeLength` (6 à 32), `maxRedemptionsPerCoupon`, `validFrom`, `validUntil` (pas avant le début), `channel`. Réponse `{ "batchCode", "generated", "promotionCode" }`. Les codes générés ne sont pas renvoyés par cette route.
2. Codes disponibles pour un souscripteur : `GET /sni/api/v1/coupons/available?subscriberType=...&subscriberCode=...` (un coupon nominatif n'apparaît qu'à son titulaire). Réponse `CouponResponse[]` : `code`, promotion, `couponKind`, `status`, `remainingUses`, validité.
3. Retenir un code pour un panier (guichet) : `POST /sni/api/v1/coupons/{code}/reserve?cartReference=...&subscriberType=...&subscriberCode=...&reservedBy=...`. Le code est retenu 30 minutes (`expiresAt`), sans être consommé ; une même référence de panier prolonge sa retenue. Un code refusé renvoie `400` avec son motif en français ; un code inconnu `404` `Ce code est introuvable`.
4. Rendre les codes d'un panier abandonné ou modifié : `DELETE /sni/api/v1/coupons/reservations?cartReference=...&reason=...` rend `{ "released": n }`. Les retenues expirées sont rendues seules toutes les 2 minutes.
5. Révoquer un code : `POST /sni/api/v1/coupons/{code}/revoke?reason=...&revokedBy=...` (`reason` obligatoire).

## Bénéficiaires nominatifs d'une promotion

Réserver une promotion à des souscripteurs désignés (droits `BILLING:*`).

1. Ajouter : `POST /sni/api/v1/promotions/{promotionCode}/beneficiaries?addedBy=...` avec `subscriberType`, `subscriberCode`, `reason`. Un bénéficiaire retiré puis rajouté est réactivé. L'existence du souscripteur n'est pas vérifiée : contrôler la saisie côté écran.
2. Importer une liste : `POST .../{promotionCode}/beneficiaries/import?addedBy=...` avec `beneficiaries[]` (au moins un, au plus 10 000) et `reason`. Réponse `ImportReport` : `submitted`, `accepted`, `rejected`, et par ligne `outcome` (« Ajouté », « Doublon dans le fichier », « Type et code de souscripteur requis »…). Les lignes refusées n'empêchent pas les autres. Le fichier se lit côté frontend et s'envoie en JSON.
3. Retirer : `DELETE .../{promotionCode}/beneficiaries?subscriberType=...&subscriberCode=...&reason=...&revokedBy=...` (`reason` obligatoire). Impossible si le bénéficiaire a déjà utilisé la promotion (`400`).
4. Noter qu'il a été prévenu : `POST .../{promotionCode}/beneficiaries/notified?subscriberType=...&subscriberCode=...&channel=...` (`204`).
5. Suivi : `GET .../{promotionCode}/beneficiaries/usage` rend `active` et `redeemed`.

Il n'y a pas de route pour lister les bénéficiaires.

## Parrainage

Routes en `ADMIN:ACCESS`. Les programmes (`programCode`, règle de qualification, promotions du parrain et du filleul) ne se créent par aucune route : ils doivent exister en base.

1. Obtenir le lien d'un parrain : `GET /sni/api/v1/referrals/links?programCode=...&referrerType=...&referrerCode=...` : créé au besoin, un seul par parrain et par programme. Réponse `{ "code", "clickCount", "signupCount" }`.
2. Compter un clic : `POST /sni/api/v1/referrals/links/{linkCode}/clicks` (`204`).
3. Inscrire un filleul : `POST /sni/api/v1/referrals` avec `linkCode`, `refereeType`, `refereeCode`. Réponse `{ "referralNumber", "status" }`. Refus en `400` : lien inactif, programme fermé, auto-parrainage, filleul déjà parrainé sur ce programme.
4. Qualification : immédiate si le programme se qualifie à l'inscription (`SIGNUP`) ; par le worker quotidien (2 h 30) pour l'ancienneté (`TENURE_REACHED`) ; la règle `FIRST_PAYMENT` n'est déclenchée par aucun code. À la qualification, le filleul puis le parrain (dans la limite du plafond du programme) reçoivent leur récompense : crédit de portefeuille, ou inscription comme bénéficiaire nominatif de la promotion du programme. Statut `REWARDED` si une récompense a été accordée.
5. Écarter : `POST /sni/api/v1/referrals/{referralNumber}/reject?reason=...` ; impossible une fois récompensé (`400`).

## Simuler un prix en back-office

`POST /sni/api/v1/pricing/simulate` ([route](routes/PricingController.md#r-post-sni-api-v1-pricing-simulate)) avec un `PricingSimulationRequest` :

- `lines[]` (au moins une) : `scope` (`PLAN`, `PASS`, `ADDON`, `SERVICE`, `RESOURCE`, `INVENTORY_ITEM`…), `code`, `unitPrice` (obligatoire), `quantity`, `setupFee`, `reference` (pour relier le détail à la ligne) ;
- le contexte : `subscriberType`, `subscriberCode`, `subscriberSegment`, `kycLevel`, `tenureMonths`, `firstPurchase`, `billingCycle`, `channel`, `paymentMethod`, `locationCode`, `currency` (déduite du catalogue si absente), `couponCodes`, `evaluationDate`, `promotionsAllowed`.

Réponse : `catalogTotal`, `originalTotal` (après grille tarifaire), `discountTotal`, `finalTotal`, `currency`, et le détail : `appliedRules` (quelle campagne, quel montant, explication), `rejections` (codes ou campagnes écartés avec leur motif), `grants` (avantages non monétaires : jours offerts, droits en plus…), `priceOverrides` (grille appliquée). Afficher les motifs de `rejections` sous les codes saisis.

## Prix et panier dans le portail

Ces cinq routes sont celles du membre connecté (`/sni/api/v1/client/...`). Le souscripteur est **toujours** le membre du jeton : `subscriberType` et `subscriberCode` envoyés dans le corps sont ignorés.

1. Prix barrés d'une liste : `POST /sni/api/v1/client/catalogue/prices` avec `items[]` (`scope`, `code`, et facultativement `listPrice`, `setupFee`, `billingCycle`, `quantity`) et un `context` facultatif. Le libellé, le prix affiché et les frais manquants sont lus dans le catalogue pour les plans et les pass. Réponse par objet : `listPrice`, `effectivePrice`, `savingsAmount`, `savingsPercent`, `priceSource`, `promotionLabel`, `validUntil`, `requiresCoupon`, `conditionsSummary`.
2. Offres disponibles : `POST /sni/api/v1/client/promotions/available` avec le panier courant (ou une ligne quelconque pour une consultation) : promotions automatiques déjà appliquées, promotions nominatives, coupons attribués, coupons publics, chacun avec `status` (`APPLICABLE`, `ALREADY_APPLIED`, `NOT_ELIGIBLE`, `EXPIRED`, `EXHAUSTED`), `estimatedSaving` et `ineligibilityReason`.
3. Recalculer le panier : `POST /sni/api/v1/client/cart/preview` (même modèle que la simulation) : rien n'est retenu.
4. Poser un code : `POST /sni/api/v1/client/cart/coupons?cartReference=...&code=...` avec le panier en corps. Le serveur évalue le panier avec ce code puis le retient 30 minutes ; il rend le panier recalculé. `cartReference` est un identifiant de panier choisi par le frontend (UUID), à garder pendant tout l'achat. Un code refusé à la retenue renvoie `400` avec le motif ; un code qui ne s'applique pas au panier apparaît dans `rejections`.
5. Retirer un code : `DELETE /sni/api/v1/client/cart/coupons/{code}?cartReference=...` rend `{ "released": n }`.
