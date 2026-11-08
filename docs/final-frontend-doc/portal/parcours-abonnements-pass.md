# portal : abonnements, pass, devis et droits

> Partie du [README du module portal](README.md). Routes exactes et modèles : [ClientCatalogController](routes/ClientCatalogController.md), [ClientSubscriptionController](routes/ClientSubscriptionController.md), [ClientSubscriptionSelfServiceController](routes/ClientSubscriptionSelfServiceController.md), [ClientPassController](routes/ClientPassController.md). Fonctionnement de fond (statuts d'abonnement et de pass, facturation, politiques de gel, de résiliation, d'engagement, prélèvement, devis, changement de plan) : [../subscription/README.md](../subscription/README.md). Règles communes : [README racine](../README.md).

Code : `src/main/java/com/sni/bokaticowork/features/portal/subscription/`.

## Ce qui est propre au portail

- Le membre ne voit et n'agit que sur les abonnements, pass et devis dont il est **l'abonné** (`subscriberType = MEMBER`, `subscriberCode = memberId`). Tout autre numéro répond `404` (`Subscription not found`, `Pass not found`, `Devis introuvable`), y compris l'abonnement d'une organisation dont il bénéficie : celui-ci se voit sous `/sni/api/v1/client/me/entitlements` ou, pour le gérant, sous `/sni/api/v1/client/companies/{customerId}/subscriptions` ([parcours-organisation.md](parcours-organisation.md)).
- Chaque action passe par les mêmes services que le guichet, avec le membre comme acteur et le canal `PORTAL`. Ce qui exige une décision humaine (dispense de frais, plan privé, gel avec préavis) reste au guichet.
- Les paiements cherchent eux-mêmes la facture à régler : la plus **ancienne** facture non soldée (ni brouillon, ni annulée) dont la source est l'abonnement ou le pass, parmi les 100 plus récentes (`ClientSubscriptionService.findPayableInvoice`, `src/main/java/com/sni/bokaticowork/features/portal/subscription/service/ClientSubscriptionService.java:310`). L'écran n'envoie pas de numéro de facture.

## Parcours

### Parcourir le catalogue

1. `GET /sni/api/v1/client/catalog/plans?planType=...&page=0&size=20` (public, sans jeton ; tri par défaut `sortOrder` croissant). Seuls les plans `ACTIVE` et visibles. Chaque plan : `code`, `name`, `description`, `planType`, `targetAudience`, `startingPrice` et `currency` (prix le plus bas de la version active), `highlightedBenefits`.
2. `GET /sni/api/v1/client/catalog/plans/{planCode}` (public) : `prices` (un prix par rythme de facturation), `benefits`, `entitlements`, `termsJson`. `requiredKycLevel` est toujours `null` dans cette réponse, alors que la souscription contrôle bien le niveau KYC exigé par le plan (voir écarts du README).
3. Prix personnalisés, coupons, aperçu de panier : routes `/sni/api/v1/client/catalogue/prices`, `/sni/api/v1/client/cart/**`, `/sni/api/v1/client/promotions/available`, documentées dans [../subscription/README.md](../subscription/README.md).

### Souscrire un abonnement

1. `POST /sni/api/v1/client/catalog/subscribe` ([détail](routes/ClientCatalogController.md#r-post-sni-api-v1-client-catalog-subscribe)) avec `{ "planCode", "billingCycle", "autoRenew" (défaut true), "couponCodes" (facultatif) }`. Le serveur crée l'abonnement au nom du membre, date de début aujourd'hui. Réponse : l'abonnement (`subscriptionNumber`, `status`, souvent `PENDING_ACTIVATION`), ses factures sont émises par le module `subscription`.
2. Proposer le paiement aussitôt : voir [Payer un abonnement](#payer-un-abonnement-ou-un-pass).
3. Refus possibles : plan inconnu ou non publié, rythme absent du plan, coupon invalide, niveau KYC du plan non atteint (`SubscriberKycLevelGuard`), selon les règles du module `subscription`.

### Consulter ses abonnements

1. Liste : `GET /sni/api/v1/client/subscriptions?status=...` (page, tri `createdAt` décroissant).
2. Abonnement courant : `GET /sni/api/v1/client/subscriptions/current` : l'abonnement actif sur la date du jour au nom du membre. **`404`** « No current active subscription found » s'il n'y en a pas : à traiter comme « aucun abonnement » et non comme une erreur.
3. Détail : `GET /sni/api/v1/client/subscriptions/{subscriptionNumber}`.
4. Droits accordés par cet abonnement : `GET .../{subscriptionNumber}/entitlements`. Soldes de tous les droits du membre (abonnements et pass confondus) : `GET /sni/api/v1/client/entitlements/balances`.
5. Échéancier : `GET .../{subscriptionNumber}/billing`.
6. Historique technique : `GET .../{subscriptionNumber}/history`. Historique lisible : voir [Relire son historique](#relire-son-historique).

### Payer un abonnement ou un pass

Deux moyens, sur l'abonnement (`/sni/api/v1/client/subscriptions/{subscriptionNumber}/pay/...`) ou sur le pass (`/sni/api/v1/client/passes/{passNumber}/pay/...`).

**Mobile money**

1. `POST .../pay/mobile-money` avec `{ "phoneNumber", "correspondent" }` (opérateur, par exemple `MTN_MOMO_COG`, `AIRTEL_COG`).
2. Le serveur crée une intention de paiement sur la facture trouvée et lance le dépôt chez l'opérateur. Réponse `MobileMoneyDepositResponse` : `depositId`, `status`, `phase` (`WAITING_FOR_PAYER`, `CONFIRMING`, `COMPLETED`, `FAILED`, `UNRESOLVED`), `userMessage` (phrase en français à afficher), `retryable`, `nextStatusCheckAt`.
3. Afficher « validez le paiement sur votre téléphone », puis interroger `GET /sni/api/v1/client/billing/mobile-money/deposits/{depositId}` (par exemple toutes les 5 à 10 secondes, ou à `nextStatusCheckAt`) jusqu'à `phase` `COMPLETED` ou `FAILED`. Détail du dépôt : [../payment/README.md](../payment/README.md).
4. `COMPLETED` : relire l'abonnement ou le pass (son statut change quand la facture est soldée, voir [../subscription/README.md](../subscription/README.md)).

**Portefeuille**

1. `POST .../pay/wallet`, corps facultatif `{ "pin": "1234" }`. Le portefeuille du membre dans la devise est trouvé ou créé.
2. Le code secret n'est exigé que si la politique de sécurité le demande pour l'opération `BILL_PAYMENT` ; sans code, le refus `400` « Ce paiement exige votre code secret » indique qu'il faut le demander, puis rejouer avec `pin`. Pour l'anticiper : `GET /sni/api/v1/client/wallet/{walletNumber}/security` (`operationsRequiringPin`), voir [parcours-portefeuille.md](parcours-portefeuille.md).
3. Réponse `PaymentTransactionResponse` : le paiement est immédiat.

Erreurs : `404` « Aucune facture à payer pour ... » (rien à régler), `409` portefeuille verrouillé ou non débitable, `400` code incorrect ou portefeuille sans code alors qu'il est exigé, `409` solde insuffisant (message du module `payment`).

### Renouveler soi-même

1. Ouvert seulement si l'abonnement est `ACTIVE` et qu'on est à moins de 7 jours de `currentPeriodEnd` (fenêtre des avis de fin).
2. `POST /sni/api/v1/client/subscriptions/{subscriptionNumber}/renew` (sans corps). Réponse `{ "subscription": {...}, "invoice": {...} }` : `invoice` est la facture du renouvellement à régler, ou `null` si rien n'est à facturer.
3. Si `invoice` n'est pas nul : proposer aussitôt [le paiement](#payer-un-abonnement-ou-un-pass).

Erreurs `400` : « Seul un abonnement actif peut être renouvelé. Le vôtre est ... », « Cet abonnement n'a pas de période à reconduire. », « Votre abonnement court jusqu'au AAAA-MM-JJ. Le renouvellement sera possible à partir du AAAA-MM-JJ. » Pour griser le bouton : comparer la date du jour à `currentPeriodEnd - 7 jours`.

### Changer de plan

1. Aperçu : `GET /sni/api/v1/client/subscriptions/{subscriptionNumber}/plan-change/preview?targetPlanCode=...&atNextPeriod=false`. Réponse : `targetPlanName`, `currentPeriodTotal`, `targetPeriodTotal`, `amount` (prorata, `0` si `atNextPeriod`), `credit` (vrai si le membre est crédité), `prorationPolicy`, `effectiveDate`, `upgrade`.
2. Confirmer : `POST .../plan-change` avec `{ "targetPlanCode", "atNextPeriod" (défaut false), "reason" }`. La confirmation vaut approbation : immédiat, le changement est appliqué tout de suite ; à la prochaine échéance, il est planifié et appliqué par le worker du module `subscription`. Réponse : la demande de changement (`changeNumber`, statut).
3. Relire l'abonnement et ses factures (un prorata facturé crée une facture, un prorata crédité alimente le portefeuille, selon la politique du module `subscription`).

Erreurs : `400` « Vous êtes déjà sur ce plan », `400` « Ce plan n'est pas au catalogue », `409` « un abonnement <statut> ne change pas de plan » (seulement si l'abonnement ouvre des droits et n'est pas en préavis de résiliation), `409` « ce plan n'existe pas au rythme ... ».

### Geler et reprendre

1. Conditions : `GET .../{subscriptionNumber}/freeze/terms` : `freezesUsedThisYear`, `freezesAllowedPerYear`, `maxDays`, `feePercent`, `noticeDays`, `canFreezeNow`. N'afficher le bouton que si `canFreezeNow` est vrai.
2. Geler : `POST .../freeze` avec `{ "until": "AAAA-MM-JJ", "reason" }`. Immédiat. Si la politique impose un préavis (`noticeDays > 0`) : `409` « un gel se demande N jour(s) à l'avance · contactez le support ».
3. Reprendre avant la date : `POST .../resume` (sans corps).

Bornes (durée maximale, nombre par an, frais) : [../subscription/README.md](../subscription/README.md).

### Résilier

Deux routes existent ; seule la seconde applique la politique de résiliation.

- `DELETE /sni/api/v1/client/subscriptions/{subscriptionNumber}` avec `{ "reason" }` facultatif : pose seulement « fin à l'échéance » (`cancelAtPeriodEnd = true`), sans préavis, sans frais, sans dossier de résiliation. Voir écarts du README.
- Parcours de résiliation complet :
  1. Frais d'engagement à une date : `GET .../{subscriptionNumber}/commitment/early-termination?on=AAAA-MM-JJ` (date facultative, aujourd'hui par défaut).
  2. Aperçu : `POST .../termination/preview` avec `{ "reasonCategory", "requestedEffectiveDate" }` (corps facultatif ; `reasonCategory` parmi `RELOCATION`, `COST`, `SERVICE_QUALITY`, `BUSINESS_CLOSURE`, `NO_LONGER_NEEDED`, `COMPETITOR`, `NON_PAYMENT`, `OTHER`, défaut `OTHER`). Réponse : date d'effet, préavis, frais.
  3. Demander : `POST .../termination` avec `{ "reasonCategory", "reason", "requestedEffectiveDate" }`. Réponse : `terminationCode`, `status` (`REQUESTED`), `effectiveDate`, `noticePeriodDays`, `earlyTermination`, `feeDue`, `exitItemsPending` (libellés des formalités de sortie encore à faire).
  4. Suivre : `GET .../{subscriptionNumber}/terminations`.
  5. Se rétracter tant que c'est ouvert (`REQUESTED` ou `ACCEPTED`) : `POST /sni/api/v1/client/terminations/{terminationCode}/retract` avec `{ "reason" }` facultatif.

Statuts d'une résiliation : `REQUESTED`, `ACCEPTED`, `RETRACTED`, `COMPLETED`. L'abonnement passe en `PENDING_TERMINATION` jusqu'à la date d'effet. Détail : [../subscription/README.md](../subscription/README.md).

### Prélèvement automatique sur le portefeuille

1. État : `GET .../{subscriptionNumber}/debit-mandate` renvoie toujours `200` : `{ "hasMandate": false, "mandate": null }` ou `{ "hasMandate": true, "mandate": { mandateCode, walletNumber, status, consentGivenAt, maxAmountPerDebit, lastDebitAt } }`.
2. Donner un mandat : `POST .../debit-mandate` avec `{ "walletNumber", "maxAmountPerDebit" (facultatif) }`. `walletNumber` est celui du membre (`GET /sni/api/v1/client/wallet`). Canal `PORTAL`, preuve de consentement `portal:<memberId>`.
3. Révoquer : `DELETE .../debit-mandate`. `404` « Aucun mandat de prélèvement sur cet abonnement » s'il n'y en a pas.

Statuts d'un mandat : `ACTIVE`, `SUSPENDED`, `REVOKED`. Les prélèvements eux-mêmes sont faits par le module `subscription`.

### Accepter ou refuser un devis

1. Liste : `GET /sni/api/v1/client/quotes` (tous les devis au nom du membre, du plus récent au plus ancien, **brouillons compris**). Détail : `GET /sni/api/v1/client/quotes/{quoteNumber}`. Champs utiles : `status`, `planName`, `billingCycle`, `cataloguePrice`, `quotedPrice`, `setupFee`, `commitmentMonths`, `trialDays`, `startDate`, `validUntil`, `notes`, `convertedSubscriptionNumber`.
2. Accepter (statut `DRAFT` ou `SENT`) : `POST .../{quoteNumber}/accept`. Le serveur crée l'abonnement (début : `startDate` ou aujourd'hui), pose la dérogation de prix et l'engagement si le devis en porte. Réponse : le devis `CONVERTED` avec `convertedSubscriptionNumber` ; enchaîner sur l'abonnement et son paiement.
3. Refuser : `POST .../{quoteNumber}/reject` avec `{ "reason" }` facultatif.

Statuts : `DRAFT`, `SENT`, `ACCEPTED`, `REJECTED`, `EXPIRED`, `CONVERTED`. Erreurs : `400` « Ce devis ne peut plus être accepté · il est ... », `409` « ce devis a expiré le ... » (le devis passe alors `EXPIRED`).

### Relire son historique

- Un abonnement : `GET /sni/api/v1/client/subscriptions/{subscriptionNumber}/timeline`.
- Tous ses abonnements : `GET /sni/api/v1/client/history`.

Chaque entrée : `occurredAt`, `kind` (`SUBSCRIPTION`, `PAYMENT`, `CHANGE`, `FREEZE`, `TERMINATION`, `BENEFIT`, `PASS`, `USAGE`), `subscriptionNumber`, `title` et `detail` en français, `amount`, `currency`. Tri du plus récent au plus ancien, sans pagination. Les événements internes (signal de fraude, notifications, planification, changements de statut bruts) sont filtrés (`src/main/java/com/sni/bokaticowork/features/portal/subscription/service/ClientHistoryService.java:44`).

### Acheter et consulter un pass

1. Acheter : `POST /sni/api/v1/client/catalog/passes/purchase` avec `{ "planCode", "passType", "name", "idempotencyKey" (facultatif) }`. Le plan doit être actif et visible, avec une version active. Le pass est créé au nom du membre et facturé. `idempotencyKey` (dans le **corps**, pas en en-tête) : un second envoi avec la même clé rend le pass déjà acheté. Générer une clé par clic sur « Acheter ».
2. Payer : `POST /sni/api/v1/client/passes/{passNumber}/pay/mobile-money` ou `/pay/wallet`, voir [plus haut](#payer-un-abonnement-ou-un-pass).
3. Liste : `GET /sni/api/v1/client/passes?passType=...&status=...` ; détail : `GET /sni/api/v1/client/passes/{passNumber}`.
4. Pass d'organisation auxquels le membre a accès : `GET /sni/api/v1/client/me/company-passes` ([parcours-organisation.md](parcours-organisation.md)).

## Règles et erreurs à refléter dans l'écran

- Tout numéro qui n'est pas au membre : `404`.
- Le paiement vise toujours la plus ancienne facture impayée de l'abonnement ou du pass : si plusieurs factures sont dues (loyer, frais d'entrée, caution), il faut appeler la route de paiement autant de fois, ou passer par l'écran factures ([parcours-facturation-paiements.md](parcours-facturation-paiements.md)) pour choisir la facture.
- Une facture dans une autre devise que celle de l'abonnement est ignorée par le paiement d'abonnement.
- Renouvellement : fenêtre de 7 jours avant `currentPeriodEnd`, abonnement `ACTIVE` seulement.
- Gel : immédiat seulement, refusé si `noticeDays > 0`.
- Changement de plan : abonnement ouvrant des droits (`TRIALING`, `ACTIVE`, `GRACE_PERIOD`, `PAST_DUE`) et pas `PENDING_TERMINATION` ; même rythme de facturation disponible dans le plan cible.

## À prévoir côté frontend

- Écrans : catalogue (public), détail de plan, panier et souscription, mes abonnements (liste, détail avec onglets droits, échéancier, historique), renouvellement, changement de plan avec aperçu, gel, résiliation avec aperçu et rétractation, prélèvement, devis, pass.
- Toujours appeler l'aperçu (`preview`, `freeze/terms`, `termination/preview`, `early-termination`) avant l'action, et afficher montants et dates rendus par le serveur plutôt que de les calculer.
- `GET .../current` en `404` = état vide.
- Pour « Résilier », utiliser le parcours `termination` ; `DELETE /sni/api/v1/client/subscriptions/{subscriptionNumber}` contourne la politique de résiliation (préavis, frais).
- Désactiver les boutons de paiement pendant l'appel : aucune protection serveur contre le double envoi sur `pay/*`.
