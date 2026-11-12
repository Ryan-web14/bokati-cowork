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

Écran : onglet « Codes » d'une promotion, droits de la facturation (`BILLING:READ` pour lire, `BILLING:CREATE` pour générer, révoquer ou retenir).

**Générer et suivre un lot**

1. Générer : `POST /sni/api/v1/coupons/batches?createdBy=...` ([route](routes/CouponController.md#r-post-sni-api-v1-coupons-batches)) avec `promotionCode`, `name`, `couponKind` (`SINGLE_USE`, `MULTI_USE`, `UNIQUE_PER_SUBSCRIBER`) obligatoires, `quantity` (1 à 50 000), `codePrefix`, `codeLength` (6 à 32), `maxRedemptionsPerCoupon`, `validFrom`, `validUntil` (pas avant le début), `channel`. Réponse `{ "batchCode", "generated", "promotionCode", "batch" }`, `batch` étant la vue du lot ci-dessous. Afficher ensuite la page du lot.
2. Liste des lots : `GET /sni/api/v1/coupons/batches?promotionCode=...&page=0&size=20` (taille au plus 100), du plus récent au plus ancien, `promotionCode` facultatif. Chaque lot (`CouponBatchView`) : `batchCode`, `name`, `description`, `promotionCode`, `promotionName`, `couponKind`, `codePrefix`, `codeLength`, `requestedQuantity`, `generatedQuantity`, `maxRedemptionsPerCoupon`, `validFrom`, `validUntil`, `channel`, `createdAt`, `createdBy`, `totalCoupons`, `countsByStatus` (toujours les quatre clés `ACTIVE`, `USED`, `EXPIRED`, `REVOKED`), `totalRedemptions`, `totalReserved`. Les compteurs sont calculés sur les codes eux-mêmes, à jour.
3. Un lot : `GET /sni/api/v1/coupons/batches/{batchCode}` (`404` s'il n'existe pas).
4. Les codes du lot : `GET /sni/api/v1/coupons/batches/{batchCode}/coupons?status=...&page=0&size=50` (taille au plus 500), triés par code, `status` facultatif. Chaque ligne : `code`, `status`, `couponKind`, `assignedToType`, `assignedToCode`, `maxRedemptions` (plafond effectif : 1 pour un code à usage unique), `redemptionCount`, `reservedCount`, `validFrom`, `validUntil`, `issuedAt`, `revokedAt`, `revokedReason`.
5. Exporter les codes pour un partenaire : `GET /sni/api/v1/coupons/batches/{batchCode}/export` télécharge `coupons-<batchCode>.csv` (UTF-8 avec BOM, séparateur `;`, s'ouvre dans Excel). Colonnes : Code, Statut, Type, Titulaire type, Titulaire code, Utilisations max, Utilisations, Retenues, Valide du, Valide au, Émis le, Révoqué le, Motif de révocation. Le fichier demande le jeton : le télécharger par `fetch` puis l'offrir en `blob:`.
6. Révoquer tout un lot : `POST /sni/api/v1/coupons/batches/{batchCode}/revoke?reason=...&revokedBy=...` (`reason` obligatoire, `400` sinon). Rend `{ "batchCode", "total", "revoked", "alreadyRevoked", "exhausted", "expired" }`. Les codes déjà révoqués gardent leur motif ; les codes épuisés ou expirés ne sont pas touchés ; un code seulement retenu dans un panier est révoqué.

**Un code**

7. Consulter : `GET /sni/api/v1/coupons/{code}` (`CouponResponse`, `404` s'il n'existe pas).
8. Codes disponibles pour un souscripteur : `GET /sni/api/v1/coupons/available?subscriberType=...&subscriberCode=...` (un coupon nominatif n'apparaît qu'à son titulaire). Réponse `CouponResponse[]` : `code`, promotion, `couponKind`, `status`, `remainingUses`, validité.
9. Retenir un code pour un panier (guichet) : `POST /sni/api/v1/coupons/{code}/reserve?cartReference=...&subscriberType=...&subscriberCode=...&reservedBy=...`. Le code est retenu 30 minutes (`expiresAt`), sans être consommé ; une même référence de panier prolonge sa retenue. Un code refusé renvoie `400` avec son motif en français ; un code inconnu `404` `Ce code est introuvable`.
10. Rendre les codes d'un panier abandonné ou modifié : `DELETE /sni/api/v1/coupons/reservations?cartReference=...&reason=...` rend `{ "released": n }`. Les retenues expirées sont rendues seules toutes les 2 minutes.
11. Révoquer un code : `POST /sni/api/v1/coupons/{code}/revoke?reason=...&revokedBy=...` (`reason` obligatoire).

## Bénéficiaires nominatifs d'une promotion

Réserver une promotion à des souscripteurs désignés (droits `BILLING:*`).

1. Ajouter : `POST /sni/api/v1/promotions/{promotionCode}/beneficiaries?addedBy=...` avec `subscriberType`, `subscriberCode`, `reason`. Un bénéficiaire retiré puis rajouté est réactivé. L'existence du souscripteur n'est pas vérifiée : contrôler la saisie côté écran.
2. Importer une liste : `POST .../{promotionCode}/beneficiaries/import?addedBy=...` avec `beneficiaries[]` (au moins un, au plus 10 000) et `reason`. Réponse `ImportReport` : `submitted`, `accepted`, `rejected`, et par ligne `outcome` (« Ajouté », « Doublon dans le fichier », « Type et code de souscripteur requis »…). Les lignes refusées n'empêchent pas les autres. Le fichier se lit côté frontend et s'envoie en JSON.
3. Retirer : `DELETE .../{promotionCode}/beneficiaries?subscriberType=...&subscriberCode=...&reason=...&revokedBy=...` (`reason` obligatoire). Impossible si le bénéficiaire a déjà utilisé la promotion (`400`).
4. Noter qu'il a été prévenu : `POST .../{promotionCode}/beneficiaries/notified?subscriberType=...&subscriberCode=...&channel=...` (`204`).
5. Suivi : `GET .../{promotionCode}/beneficiaries/usage` rend `active` et `redeemed`.

Il n'y a pas de route pour lister les bénéficiaires.

## Parrainage

Un **programme** dit ce que gagnent le parrain et le filleul (deux promotions : crédit de portefeuille ou remise) et quand (`qualificationRule`). Chaque parrain a **un lien** par programme : un code court et une adresse à partager. Le filleul s'inscrit avec ce code ; son **parrainage** se qualifie selon la règle, et les récompenses sont alors accordées.

### Back-office : programmes (droits `BILLING:*`)

1. Créer : `POST /sni/api/v1/referrals/programs` avec `code` (mis en majuscules, unique, `409` sinon), `name`, et facultatifs `description`, `validFrom`, `validUntil`, `referrerPromotionCode` (récompense du parrain), `refereePromotionCode` (récompense du filleul), `qualificationRule` (`FIRST_PAYMENT` par défaut, `SIGNUP`, `TENURE_REACHED`), `qualificationDelayDays` (obligatoire pour `TENURE_REACHED`), `maxReferralsPerReferrer` (plafond des récompenses du parrain), `currency` (`XAF` par défaut). Au moins une des deux promotions est obligatoire (`400`). Le programme naît en brouillon (`DRAFT`). Réponse `201` `ProgramView`.
2. `ProgramView` : `code`, `name`, `description`, `status` (`DRAFT`, `ACTIVE`, `PAUSED`, `ENDED`), `open` (ouvert à cet instant), `validFrom`, `validUntil`, `referrerPromotion` et `refereePromotion` (`{ code, name, description }` ou `null`), `qualificationRule`, `qualificationDelayDays`, `maxReferralsPerReferrer`, `currency`, `links` (nombre de liens), `referralsByStatus` (`PENDING`, `QUALIFIED`, `REWARDED`, `REJECTED`), `createdAt`, `createdBy`, `updatedAt`.
3. Lister : `GET /sni/api/v1/referrals/programs`. Un programme : `GET .../programs/{code}`.
4. Modifier : `PUT .../programs/{code}`, même corps (le `code` du corps est ignoré). Nom, description, dates, plafond et devise changent à tout moment ; règle et récompenses seulement en brouillon (`400` ensuite : les parrainages en cours ont été promis avec elles). Un programme clos ne se modifie plus.
5. Piloter : `POST .../programs/{code}/activate` (depuis le brouillon ou une pause ; `400` sans récompense ou si la date de fin est passée), `.../pause` (plus de nouveaux parrainages, ceux en cours se qualifient toujours), `.../end` (définitif ; la date de fin devient maintenant).

### Back-office : liens et parrainages

6. Créer le lien d'un parrain : `POST /sni/api/v1/referrals/links` avec `programCode`, `referrerType` (`MEMBER`, `CUSTOMER`...), `referrerCode`. Rend le lien existant s'il en a déjà un (un seul par parrain et par programme). Un membre inconnu : `404` ; un programme clos : `400`. L'ancienne forme `GET /referrals/links?programCode=&referrerType=&referrerCode=` fait la même chose.
7. `LinkView` : `code`, `url` (l'adresse à partager), `programCode`, `programName`, `programOpen`, `referrerType`, `referrerCode`, `clickCount`, `signupCount`, `active`, `createdAt`.
8. Liens d'un programme : `GET .../programs/{code}/links?page=0&size=20`. Couper un lien (abus) : `POST /referrals/links/{linkCode}/deactivate`, le rouvrir : `.../activate` ; les parrainages déjà enregistrés suivent leur cours.
9. Parrainages : `GET /sni/api/v1/referrals?programCode=...&status=...&referrerCode=...&page=0&size=20` (filtres facultatifs, du plus récent au plus ancien). Un parrainage : `GET /referrals/{referralNumber}`. `ReferralView` : `referralNumber`, `programCode`, `programName`, `linkCode`, parrain et filleul (`...Type`, `...Code`), `status`, `registeredAt`, `qualifiedAt`, `rejectedAt`, `rejectionReason`, récompenses du parrain et du filleul (`...RewardGranted`, `...RewardAmount` pour un crédit de portefeuille, `...RewardedAt`), `currency`.
10. Inscrire un filleul à la main (filleul déjà client) : `POST /sni/api/v1/referrals` avec `linkCode`, `refereeType`, `refereeCode`. Réponse `{ "referralNumber", "status" }`. Refus en `400` : lien inactif, programme fermé, auto-parrainage, filleul déjà parrainé sur ce programme.
11. Écarter : `POST /referrals/{referralNumber}/reject?reason=...` ; impossible une fois récompensé (`400`).

### Espace client : « Parrainer un ami »

12. Programmes ouverts : `GET /sni/api/v1/client/referrals/programs` : `code`, `name`, `description`, `validUntil`, ce que gagne le membre (`referrerPromotion`) et son ami (`refereePromotion`), `qualificationRule` (afficher « dès que votre ami a réglé son premier achat » pour `FIRST_PAYMENT`), `qualificationDelayDays`, `maxReferralsPerReferrer`, et `myLinkCode` / `myLinkUrl` si le membre a déjà son lien.
13. Obtenir son lien : `POST /sni/api/v1/client/referrals/links` avec `{ "programCode" }` ; le même lien à chaque appel. Rend `LinkView` : afficher `url` avec « Copier » et « Partager » (`navigator.share` sur mobile, WhatsApp), et le code seul pour une saisie à la main. `400` si le programme n'est pas ouvert. Le membre est toujours le parrain : le corps ne désigne personne d'autre.
14. Ses liens et leurs statistiques : `GET /sni/api/v1/client/referrals/links` (`clickCount`, `signupCount`).
15. Ses parrainages : `GET /sni/api/v1/client/referrals` : `referralNumber`, `programName`, `friend` (prénom et initiale du nom de l'ami, « Awa N. »), `status` (afficher « Inscrit », « Validé », « Récompensé », « Écarté »), `registeredAt`, `qualifiedAt`, `rewardGranted`, `rewardAmount`, `currency`. Seule la récompense du parrain y figure.

### Page publique du lien et inscription

16. L'adresse partagée mène à la page d'inscription du portail avec le code : `https://elleaose.com/inscription?parrain=K7P2QX` par défaut. Le modèle se règle par `app.referral.share-url-template` (`{code}` y est remplacé) : il doit pointer vers la route d'inscription réelle du frontend.
17. À l'arrivée, sans connexion : `GET /sni/api/v1/public/referrals/{code}` rend toujours `200` avec `code`, `valid`, `reason` (pourquoi le code ne sert pas : inconnu, lien coupé, programme fermé), `programName`, `programDescription`, `refereePromotion` (ce que gagne la personne invitée) et `referrerFirstName` (« Awa vous invite »). Compter la visite : `POST /sni/api/v1/public/referrals/{code}/clicks` (`204`, une fois par visite).
18. S'inscrire : `POST /sni/api/v1/auth/register` avec le champ facultatif `referralCode`. Le code est vérifié **avant** la création du compte : un code inutilisable renvoie `400` « Code de parrainage refusé : ... » et rien n'est créé ; proposer de corriger ou d'effacer le code. Avec un code valable, le filleul est enregistré à la création du compte.
19. Qualification : `SIGNUP` à l'inscription ; `FIRST_PAYMENT` au premier paiement réussi du filleul (traité juste après la validation du paiement, sans jamais le bloquer) ; `TENURE_REACHED` par le traitement quotidien (2 h 30). Le filleul puis le parrain, dans la limite du plafond du programme, reçoivent alors leur récompense : crédit de portefeuille, ou inscription comme bénéficiaire nominatif de la promotion. Statut `REWARDED` si une récompense a été accordée.

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
