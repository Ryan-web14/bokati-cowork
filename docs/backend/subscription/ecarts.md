# Écarts relevés dans le module subscription (audit du 19/11/2026)

> Partie du [plan subscription](README.md). Chemins relatifs à `src/main/java/com/sni/bokaticowork/`. `SLO` = `features/subscription/subscription/service/support/subscription/SubscriptionLifecycleOperator.java`, `SCO` = `.../SubscriptionCreationOperator.java`, `AAM` = `security/authorization/AdminApiAuthorizationManager.java`, `P` = `features/subscription/promotion`. La colonne Lot dit où l'écart sera réglé.

## A. Cœur et cycle de vie

| N° | Écart | Preuve | Lot |
| --- | --- | --- | --- |
| A1 | Aucune machine d'états : `changeStatus` accepte toute transition ; `activate` réactive un `CANCELLED` ou `EXPIRED` ; `cancel` sans garde | `SubscriptionStatusManager.java:17-30` ; SLO:115-129, 214-237 | S1 |
| A2 | Reprise après pause inversée, pas de reprise automatique à `pauseUntil` | SLO:193-195 | S1 |
| A3 | Suspension et pause ne ferment pas les droits | SLO:157-187 ; `EntitlementGrantRepository.java:23-34` | S1 |
| A4 | Une demande de résiliation fait passer `PAUSED`/`SUSPENDED` en `PENDING_TERMINATION`, qui rouvre les droits ; le retrait remet `ACTIVE` ; le gel ouvert n'est pas clos | `SubscriptionTerminationService.java:118-147, 216-218` ; `SubscriptionStatus.java:22` | S1 |
| A5 | Payer la facture de renouvellement réactive un abonnement suspendu à la main | SLO:61-62, 632-635 | S1 |
| A6 | Annulation en fin de période : renouvellement en attente, dépôt, échéancier non traités (contrairement à l'annulation manuelle) ; clôtures automatiques (fin de terme, expiration) sans révocation des droits, ni contrat, ni dépôt | SLO:474-495 ; `SubscriptionEndNoticeService.java:130-147` ; `SubscriptionExpiryService.java:115-125` | S1 |
| A7 | `retryPendingDocuments` appelé sur `this` (sans transaction) ; `kycCompliant` laisse passer sur exception ; alerte KYC publiée chaque heure | `SubscriptionLifecycleWorker.java:94-111` ; SLO:780-797 | S1 |
| A8 | Cycle `ONE_TIME` : fin de période le jour du début, renouvellement automatique à vrai | `SubscriptionPeriodCalculator.java:17` ; SCO:161 | S1 |
| A9 | Renouvellement payé d'avance : `GET /current` répond 404 | `SubscriptionRepository.java:32` | S1 |
| A10 | Annulation en fin de période clôturée pendant le dernier jour payé | `SubscriptionRepository.java:68-75` | S1 |
| A11 | Statuts jamais atteints : `DRAFT`, `TRIALING`, `PAST_DUE` | | S1, N1, N5 |
| A12 | Demande de changement : `approve` sans garde ni droit distinct, `approvedBy` texte libre ; `apply` ignore la date d'effet ; options et quantité sans effet ; le portail s'approuve lui-même ; un échec annule tout le lot | `SubscriptionChangeRequestServiceImpl.java:63-104` ; `ClientSubscriptionSelfService.java:105` | S1 |
| A13 | Changement d'offre : le prix négocié est perdu sans prévenir | `PlanChangeProrationService.java:85-88` | S2 |
| A14 | Aucun moyen de corriger le prix à l'activation sans devis | SCO:88-131 ; SLO:115-155 | S2 |
| A15 | Un membre qui accepte un devis approuve aussi la dérivation de prix ; devis brouillon (jamais envoyé) acceptable ; dérogation au prix plancher en texte libre | `ClientSubscriptionSelfService.java:229` ; `SubscriptionQuoteService.java:130-132, 231` ; `SubscriptionQuote.java:35-36` | S2 |
| A16 | `planVersionId` : toute version active acceptée, y compris la version privée d'un autre client ou d'une autre offre ; offre archivée souscriptible ; publication sans contrôle (brouillon, prix) ; le portail ne contrôle ni visibilité ni audience | `SubscriptionPlanResolver.java:31-33, 81-83` ; `PlanCatalogOperator.java:88-140` ; `ClientCatalogService.java:70-84` | S0 |
| A17 | Résiliation au portail (`DELETE`) : contourne préavis et frais de sortie | `ClientSubscriptionService.java:119-127` | S1 |
| A18 | Contrôle de doublon sur le code brut et la version : contourné par un code d'entreprise, une nouvelle version ou une version privée | SCO:111 | S0 |
| A19 | Abonnement d'entreprise : le portail ne sert que les membres (le gestionnaire ne peut pas accepter un devis de l'entreprise) ; contrat sans bénéficiaires des sièges déjà assignés | `ClientSubscriptionSelfService.java:214, 248-254` ; `CompanyContractsService.java:102-123` | S4 |
| A20 | Aucun `@Audited` ni `@Idempotent` dans le module ; acteurs en texte libre, `SYSTEM` par défaut | SLO:700-702 | S0 |
| A21 | Listes non paginées : résiliations, devis par client, mandats, gels, historique, droits ; `dispatch-due` sans plafond | | S0 |
| A22 | Messages en anglais (SLO:160, 177, 191, 262 ; `SubscriptionServiceImpl`, `SubscriptionPlanResolver`, portail) ; libellé de facture « Subscription renewal » ; sujets de courriel sans accents ; courriel de suspension sous le gabarit de création | `SubscriptionEmailNotifier.java:132-139` | S0, S1 |
| A23 | Plans : `requiredKycLevel` et `trialDays` non réglables ; publier force `visible` | `PlanCatalogOperator.java:138` | S0 |
| A24 | Traitements en lot dans une transaction (dérivation, protection tarifaire, changements) | `PlanDerivationBatchService.java:104` | S1 |

## B. Sièges, droits, consommation

| N° | Écart | Preuve | Lot |
| --- | --- | --- | --- |
| B1 | Deux API de sièges sur la même table ; l'ancienne contourne plafond, lien à l'entreprise, statut ; rôle `OWNER` accepté ; messages en anglais | `SubscriptionSeatServiceImpl.java:41-64` | S4 |
| B2 | Pas d'invitation, pas de réaffectation (reste du quota perdu) ; statuts `INVITED`, `SUSPENDED` jamais posés ; libération sans verrou | `CompanySharingService.java:181, 218-222` | S4 |
| B3 | `resetPolicy`, `consumptionMode`, `stackable`, `restrictionsJson` jamais appliqués ; droits recalculés en UTC, report en fuseau système | `EntitlementGrantIssuer.java:33-94` | S3 |
| B4 | Aucune surcharge des droits par abonnement sans dérivation ; nouvelles quantités d'une dérivation seulement à la période suivante | | S3 |
| B5 | Unité `CREDIT` (et autres que heure, jour, réservation, visite) refusée en réservation | `BookingEntitlementBridge.java:66-93` | S3 |
| B6 | Couverture partielle : solde de tous les droits du titulaire (pas celui choisi, ni quota de siège), heures et jours seulement, sautée si dépassement permis, erreurs avalées | `BookingPartialCoverage.java:54-88` | S3 |
| B7 | Domiciliation : n'utilise jamais les droits inclus ; impression non branchée | `MailItemService.java:313-317` | S3 |
| B8 | Franchise du dépassement déduite à chaque usage | `SubscriptionOverageServiceImpl.java:138` | S3 |
| B9 | Annuler une réservation en dépassement rend des heures gratuites (déduit du code, non exécuté) | `BookingEntitlementBridge.java:112-127, 233` ; `EntitlementReservationOperator.java:164-166` | S3 |
| B10 | Politique de dépassement : aucune validation (prix, devise, droit de l'offre), doublons, ni modification ni désactivation ; non copiée par une dérivation | `SubscriptionOverageServiceImpl.java:65-74` | S3 |
| B11 | Usage : pas d'annulation ; ligne d'historique sans abonnement | `UsageRecordServiceImpl.java:44-57` | S3 |
| B12 | Report : une erreur arrête tout le lot (`NoSuchElementException`) | `SubscriptionRolloverServiceImpl.java:41-96` | S3 |
| B13 | Une option n'accorde aucun droit ; prix pris au hasard (`findFirst`) ; annulation sans avoir ; jamais récurrente | `SubscriptionAddonServiceImpl.java:55-129` | S3 |
| B14 | `/entitlements` et `/internal/entitlements/{consume,refund}` ouverts à tout agent (`ADMIN:ACCESS`) | AAM:74-81 | S0 |
| B15 | Requêtes répétées dans le partage d'entreprise (droits de toutes les périodes, client relu par siège) | `CompanySharingService.java:104, 325-334` | S4 |

## C. Pass

| N° | Écart | Preuve | Lot |
| --- | --- | --- | --- |
| C1 | Pass entamé (`PARTIALLY_USED`) jamais expiré ni renouvelé ; renouvellement avant paiement, sans droits ; `REQUIRES_NEW` sans effet (appel interne) | `PassRenewalOperator.java:38-108` ; `PassLifecycleOperator.java:137-142` | S8 |
| C2 | Usage sans verrou : deux passages simultanés dépassent `maxUses` | `PassUsageService.java` | S8 |
| C3 | Transfert : droits restés à l'ancien titulaire, destinataire non vérifié, frais jamais facturés | `PassTransferService.java:110-158` | S8 |
| C4 | Lecture qui écrit (`GET /passes/{n}/credentials`) | `PassCredentialService.java:132-137` | S8 |
| C5 | Versions publiées modifiables ; `publish` ignore `planCode` ; `transferable` et `shareable` non réglables | | S8 |
| C6 | Annuler un pass journalise toujours `ACTIVE` comme statut d'avant ; message KYC en anglais | `PassLifecycleOperator.java:123` ; `SubscriberKycLevelGuard.java:46` | S8 |

## D. Promotions et coupons

| N° | Écart | Preuve | Lot |
| --- | --- | --- | --- |
| D1 | **Aucune route n'écrit récompenses, conditions, audiences, listes de prix** : toute promotion créée est rejetée | `PricingEngine.java:337-340` ; `CreatePromotionRequest.java` | S5 |
| D2 | `PromotionType` non réglable ; seules les promotions `AUTOMATIC` sont évaluées | `PricingEngine.java:225` | S5 |
| D3 | Cumul : seul le drapeau du nouveau venu est lu | `PricingEngine.java:332` | S5 |
| D4 | Budget ignoré pour les coupons ; `redemptionCount` compté plusieurs fois par promotion à plusieurs récompenses ; verrou optimiste sans reprise (une souscription simultanée échoue) | `DiscountApplicationService.java:62-64` | S5 |
| D5 | Limite par client jamais lue ; l'usage unique par client lit une table que le nouveau flux n'écrit pas | `CouponService.java:124-128` | S5 |
| D6 | Conditions : parrainage toujours faux, types d'offre comparés au code, « supérieur » vaut « supérieur ou égal », premier achat, ancienneté, KYC jamais fournis | `PromotionConditionEvaluator.java` | S5 |
| D7 | Contexte client (segment, KYC, ancienneté, prix unitaire) fourni par le client au portail ; audience entreprise jamais reconnue | `PricingSimulationService.java:29-48` | S5 |
| D8 | Approbation (`approvedBy`) jamais utilisée ; `/activate` depuis tout statut ; `PAUSED`, `ARCHIVED`, coupon `EXPIRED` jamais posés | | S5 |
| D9 | Aucune annulation des remises (budget, coupon) à l'annulation d'un abonnement ou d'une réservation | `DiscountApplicationService.reverse` sans appelant | S5 |
| D10 | Remise d'abonnement fondue dans le prix net, sans ligne de remise ni origine ; durée illimitée (reste à chaque renouvellement) ; compte de contrepartie jamais utilisé | SCO:128, 164-166 | S5 |
| D11 | Coupon réservé au panier puis refusé au paiement (la réservation du panier compte contre lui) ; réservé même rejeté ; montant mêlé aux promotions automatiques | `Coupon.remainingUses()` ; `ClientPricingController.java:105-107` | S6 |
| D12 | Coupon invalide ignoré en silence à la souscription (plein tarif) ; réservation : un code invalide accepté si une promotion automatique s'applique | `SubscriptionPricingBridge` ; `BookingExtras.java:330` | S6 |
| D13 | Codes nommés impossibles, attribution jamais écrite, audience non appliquée aux coupons ; aucun envoi ; pas de « mes coupons » ; `estimatedSaving`, `validUntil`, `requiresCoupon` toujours vides | | S6 |
| D14 | Encaissement absent sur facture, paiement, pass, réservation au portail (`BillingPricingBridge` sans appelant) | | S6 |
| D15 | Effets non monétaires calculés jamais appliqués (mois offerts, essai, heures offertes, option offerte, montée en gamme) | | S6 |
| D16 | `capture` capture tous les codes du panier, même rejetés ; génération de lot sans contrôle de statut ni de dates | `CouponService.capture` | S6 |
| D17 | Aucune trace d'audit ; auteurs (`createdBy`, `revokedBy`...) fournis par la requête | | S0 |
| D18 | Statistiques : `markRedeemed` sans appelant, `redeemed_quantity` jamais mis à jour, aucun état des remises accordées | | S6 |
| D19 | Messages en anglais ou sans accents | `PromotionServiceImpl.java:51-86` ; `PricingEngine.java:306-515` | S0 |
| D20 | Code mort ou doublé : ancien `/redeem`, `discountType`/`discountValue`, `CouponRedemption` sans `coupon_id` | | S5 |
