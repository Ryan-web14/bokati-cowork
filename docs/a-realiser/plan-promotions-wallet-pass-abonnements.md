# Promotions, portefeuille, pass et abonnements · état des lieux et conception

> Document de référence pour quatre chantiers liés : la tarification promotionnelle, le portefeuille
> électronique, l'opérationnalisation des pass, et l'élargissement des abonnements aux services de
> domiciliation et assimilés.
>
> Chaque partie s'ouvre sur un état des lieux vérifié dans le code, puis expose la cible et les
> écarts. Les quatre chantiers se recoupent : une promotion crédite parfois un portefeuille, un pass
> naît d'un abonnement, un abonnement se paie depuis un portefeuille. L'ordre de réalisation proposé
> en partie 6 tient compte de ces dépendances.

- **Modules concernés** : `features/subscription`, `features/payment`, `features/billing`,
  `features/portal`
- **Base HTTP** : `/sni/api/v1`
- **Montants** : entiers XAF, conformément à la convention du projet

## Cadre acté

Quatre décisions métier ont été prises et sont intégrées au document. Elles ne sont pas des
hypothèses de travail : elles conditionnent le modèle.

1. **Le portefeuille détient des avances clients, pas de la monnaie électronique**, tout en visant le
   niveau d'exigence d'un opérateur de mobile money. Quatre exclusions fermes en découlent,
   détaillées en 3.1 et rappelées en 3.8.
2. **La domiciliation se facture au mois, au trimestre ou à l'année.**
3. **L'adresse n'est fiscale que si l'abonnement est pris pour un an.** La qualité fiscale est
   déduite de l'engagement, jamais saisie, et se perd si l'engagement se raccourcit.
4. **Toute domiciliation génère un contrat enregistré et timbré** auprès de l'administration
   publique. L'enregistrement est une étape bloquante, pas une formalité annexe.

Quatre exigences fonctionnelles s'y ajoutent, intégrées aux parties 2 et 5.

5. **Les coupons s'utilisent côté client, avant le paiement.** Le client voit ceux dont il dispose,
   en saisit ou en choisit un, et le panier se recalcule sous ses yeux. Le coupon n'est consommé
   qu'au paiement, pas à la saisie.
6. **Les promotions s'appliquent automatiquement** sur les plans, pass, services, ressources et
   articles tant qu'elles sont valides, et **les prix promotionnels s'affichent côté client**, prix
   barré compris.
7. **Une promotion peut viser des clients nommés**, un par un, sans passer par un segment artificiel.
8. **Un abonnement peut être dérivé pour un abonné précis**, avec son prix et ses avantages propres,
   sans créer un plan de catalogue par client.

---

# Partie 1 · État des lieux

## 1.1 Promotions et remises

### Ce qui existe

Le sous-module `subscription/promotion` contient deux entités et deux énumérations.

```text
Promotion            code, name, description, discountType, discountValue,
                     startsAt, endsAt, maxRedemptions, redemptionCount,
                     status, metadataJson
CouponRedemption     promotion, subscriberType, subscriberCode, subscription, redeemedAt

DiscountType         PERCENTAGE, FIXED_AMOUNT, FREE_TRIAL_DAYS, FREE_ENTITLEMENT, WAIVE_SETUP_FEE
PromotionStatus      DRAFT, ACTIVE, PAUSED, EXPIRED, ARCHIVED
```

Quatre endpoints : créer, activer, utiliser, lister. Plus un worker qui expire les promotions dont
la date de fin est passée.

Côté facturation, `BillingDocumentDiscount` porte `discountCode`, `description`, `discountType`,
`value` et `amount`. Le module billing sait donc appliquer une remise sur un document, et le fait
déjà : les commits récents ont retravaillé la déduction sur le TTC et le chiffrage unique.

### Le constat qui compte

**Les deux mondes ne se parlent pas.** Une recherche sur l'ensemble du code source montre que
`Promotion` et `CouponRedemption` ne sont référencés dans aucun fichier en dehors de leur propre
paquet. `BillingDocumentDiscount.discountCode` est un champ texte libre, sans clé étrangère vers
`Promotion`.

Concrètement : on peut créer une promotion, l'activer, enregistrer qu'un abonné l'a utilisée, et le
compteur s'incrémente. Mais **aucun montant n'est jamais calculé, et aucune remise n'est jamais
appliquée**. `PromotionServiceImpl.redeem()` se contente d'enregistrer la ligne d'utilisation.

De plus, `FREE_TRIAL_DAYS` est explicitement refusé à la création :
`"Trial promotions are not supported in this version"`. Deux des cinq types de remise déclarés
(`FREE_ENTITLEMENT`, `WAIVE_SETUP_FEE`) n'ont pas non plus de traitement.

### Ce qui manque, en une phrase

Un moteur de tarification : quelque chose qui, devant un panier ou un abonnement, sache dire quelles
règles s'appliquent, dans quel ordre, pour quel montant, et qui l'écrive sur le document.

## 1.2 Portefeuille électronique

### Ce qui existe

```text
WalletAccount        walletNumber, ownerType, ownerCode, currency, status,
                     availableBalance, ledgerBalance, heldBalance, version,
                     openedAt, closedAt
WalletLedgerEntry    entryNumber, wallet, direction, amount, currency, balanceAfter,
                     entryType, sourceType, sourceCode, reference, idempotencyKey, createdBy
WalletHold           holdNumber, wallet, amount, currency, status, sourceType,
                     sourceCode, expiresAt, createdBy

WalletStatus         ACTIVE, SUSPENDED, LOCKED, CLOSED, UNDER_REVIEW
WalletEntryType      ADMIN_TOPUP, ADMIN_DEBIT, PAYMENT, REFUND, REVERSAL, HOLD,
                     HOLD_RELEASE, ADJUSTMENT, CASHBACK, PROMOTIONAL_CREDIT, OVERPAYMENT_CREDIT
WalletEntryDirection DEBIT, CREDIT
WalletHoldStatus     ACTIVE, CAPTURED, RELEASED, EXPIRED
```

Le socle est sérieux. `WalletLedgerService` tient trois soldes distincts, disponible, comptable et
retenu, avec verrouillage optimiste par `@Version` et clé d'idempotence sur chaque écriture. Les
retenues fonctionnent en pose, capture et libération. `WalletEntryType` anticipe déjà le cashback et
le crédit promotionnel.

Endpoints existants :

```text
GET   /wallets                        GET  /wallets/{n}        GET /wallets/{n}/ledger
POST  /wallets/admin/top-up           POST /wallets/admin/get-or-create
GET   /wallet-holds                   POST /wallet-holds
PATCH /wallet-holds/{n}/capture | /release
GET   /client/wallet                  GET  /client/wallet/{n}  GET /client/wallet/{n}/ledger
PATCH /payments/intents/{n}/wallet
```

### Le constat qui compte

**Le portefeuille est en lecture seule pour son propriétaire.** Le contrôleur client n'expose que
trois `GET`. Toute alimentation passe par `POST /wallets/admin/top-up`, donc par un administrateur.
Un abonné ne peut rien initier lui-même.

**Aucun code secret nulle part.** Une recherche sur l'ensemble du code ne trouve aucune notion de
code PIN, de code secret ou de confirmation de transaction. Le portefeuille s'utilise avec la seule
session applicative.

**Aucune limite.** Ni plafond par transaction, ni plafond journalier, ni palier lié au niveau de
vérification d'identité, alors que `PassPlan.requiredKycLevel` montre que la notion de niveau KYC
existe déjà ailleurs dans le projet.

**Aucun transfert entre abonnés.** Le portefeuille sert à payer ses propres factures, rien d'autre.

## 1.3 Pass

### Ce qui existe

Le modèle est riche, nettement plus que ce que la plupart des projets ont à ce stade.

```text
Pass               passNumber, passType, ownerType, ownerCode, subscription,
                   planVersion, passVersion, status, validFrom, validUntil,
                   transferable, shareable, maxUses, usedCount,
                   currency, subtotalAmount, taxAmount, totalAmount,
                   autoRenew, nextRenewalDate, renewalCount,
                   cancelledAt, cancellationReason, contractCode
PassPlan           code, name, passType, targetAudience, status, visible,
                   sortOrder, requiredKycLevel, deleted
PassPlanVersion, PassPlanPrice, PassPlanEntitlement, PassEntitlement,
PassEvent, PassStatusHistory, PassTransaction, PassRenewalSchedule

PassType           DAY_PASS, TIME_PACK, VISITOR_PASS, MEETING_ROOM_PACK,
                   PROMOTIONAL_PASS, SUBSCRIPTION_PASS, COMPANY_SHARED_PASS, CUSTOM
PassStatus         DRAFT, PENDING_ACTIVATION, ACTIVE, PARTIALLY_USED, CONSUMED,
                   PAST_DUE, EXPIRED, CANCELLED, SUSPENDED
```

Neuf classes de support portent la logique : création, cycle de vie, renouvellement, calcul de
période, résolution de plan, facturation, notification, journal d'événements, fabrique de codes.

Le système de droits est lui aussi abouti : `EntitlementDefinition`, `EntitlementGrant`,
`EntitlementLedger`, `EntitlementReservation`, avec sept types de droits, neuf unités et quatre modes
de consommation dont `RESERVE_THEN_CONSUME`.

### Le constat qui compte

Le pass sait naître, se renouveler, expirer et être facturé. Ce qui manque est ce qui se passe
**entre les deux** : l'usage.

- `transferable` et `shareable` sont des booléens portés par l'entité, **sans aucun processus
  derrière**. Rien ne permet de transférer un pass ni d'en partager l'usage.
- `maxUses` et `usedCount` existent, mais le geste qui incrémente le compteur au moment où quelqu'un
  se présente n'est pas décrit comme un parcours complet.
- `COMPANY_SHARED_PASS` est déclaré comme type, sans modèle de bénéficiaires : qui, dans
  l'entreprise, a le droit de s'en servir.
- Aucune règle de validité fine : un pass vaut pour toute la plage entre `validFrom` et `validUntil`,
  sans restriction de jours, d'horaires ni de site.

## 1.4 Abonnements

### Ce qui existe

Le module est le plus fourni du projet : onze sous-modules, dont `addon`, `change`, `overage`,
`rollover`, `seat`, `usage`, `timeline`, `metrics`, `notification`.

```text
Subscription       subscriptionNumber, subscriberType, subscriberCode,
                   member, customer, businessEntity, planVersion, status,
                   startDate, currentPeriodStart, currentPeriodEnd, nextBillingDate,
                   trialStart, trialEnd, autoRenew, billingCycle,
                   subtotalAmount, taxAmount, totalAmount,
                   cancelAtPeriodEnd, cancelledAt, suspendedAt, pausedAt, pauseUntil,
                   contractCode, kycCompliant

PlanType           MEMBERSHIP, COWORKING_ACCESS, DEDICATED_DESK, PRIVATE_OFFICE,
                   MEETING_ROOM_PACK, VIRTUAL_OFFICE, COMPANY_PLAN, CUSTOM
SubscriptionStatus DRAFT, PENDING_ACTIVATION, TRIALING, ACTIVE, PAST_DUE,
                   PAUSED, SUSPENDED, CANCELLED, EXPIRED
BillingCycle       ONE_TIME, DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY
```

Changements de plan, modules complémentaires, dépassements, report de droits, sièges, suivi d'usage,
frise chronologique, indicateurs, notifications : tout cela existe déjà.

### Le constat qui compte

`VIRTUAL_OFFICE` figure dans `PlanType`, ce qui est la porte d'entrée de la domiciliation. Mais un
plan de domiciliation n'est pas un plan d'accès à un espace : il porte des obligations que le modèle
actuel ne sait pas exprimer.

- Une **adresse commerciale attribuée**, unique, suivie, et dont la **qualité fiscale dépend de la
  durée d'engagement** : seul un abonnement annuel y donne droit.
- Un **contrat écrit, enregistré et timbré** auprès de l'administration publique, sans lequel la
  domiciliation n'est pas opposable.
- Une **attestation de domiciliation** à produire, dont la portée, commerciale ou fiscale, découle de
  l'engagement souscrit.
- Du **courrier** qui arrive, qu'il faut enregistrer, notifier, numériser, faire suivre ou remettre.
- La **vérification d'identité** du représentant légal, la tenue d'un registre, la conservation des
  pièces.

Rien de tout cela n'a de place dans `Subscription` aujourd'hui. Et surtout, aucune de ces règles n'est
exprimable dans le modèle actuel : la durée d'engagement n'y conditionne rien, et le contrat n'y est
qu'un code de référence libre.

---

# Partie 2 · Remises, coupons, promotions et prix spéciaux

## 2.1 Distinguer quatre choses que l'on confond souvent

Le module actuel mélange sous un seul nom, `Promotion`, quatre objets qui ont des cycles de vie et
des règles différents. Les séparer est la première décision de conception.

| Objet | Ce que c'est | Qui le déclenche |
|---|---|---|
| **Promotion** | Une campagne : une réduction, une période, une cible, un budget | Le système, automatiquement, quand les conditions sont réunies |
| **Coupon** | Un code que quelqu'un saisit | Le client, en saisissant le code |
| **Prix spécial** | Un tarif négocié pour un client ou un segment, qui remplace le tarif catalogue | Personne : c'est le prix, tout simplement |
| **Bon cadeau** | Un montant prépayé, porteur d'une valeur, utilisable comme un moyen de paiement | Le porteur, au moment de payer |

Un bon cadeau n'est pas une remise : il ne réduit pas le prix, il le paie. Sa place naturelle est le
portefeuille, traité en partie 3.

## 2.2 Modèle cible

### Promotion, étendue

```text
Promotion
  id, code, name, description
  promotionType        AUTOMATIC | COUPON | REFERRAL | LOYALTY
  status               DRAFT, SCHEDULED, ACTIVE, PAUSED, EXHAUSTED, EXPIRED, ARCHIVED
  startsAt, endsAt

  -- Cumul
  stackable            boolean, peut se cumuler avec d'autres promotions
  exclusive            boolean, empeche toute autre promotion si elle s'applique
  priority             entier, ordre d'evaluation, le plus petit d'abord

  -- Budget et plafonds
  maxRedemptions              total, toutes personnes confondues
  maxRedemptionsPerSubscriber par personne
  maxDiscountAmount           plafond de remise par utilisation
  budgetAmount                enveloppe totale consommee
  consumedBudgetAmount

  -- Restitution
  redemptionCount, totalDiscountGranted

  -- Comptabilite
  counterpartAccount   compte de charge ou de reduction de produit

  createdBy, approvedBy, approvedAt
```

L'ajout de `promotionType` est ce qui permet de distinguer une campagne automatique d'un code à
saisir, sans dupliquer l'entité.

### Conditions et récompenses, séparées de la promotion

Une promotion utile n'est presque jamais « dix pour cent sur tout ». C'est « dix pour cent sur le
premier mois, pour un nouveau membre, sur un plan mensuel, si le paiement est mensualisé ». Ces
conditions doivent être des données, pas du code.

```text
PromotionCondition
  promotion, conditionType, operator, value, valueList

  conditionType   SUBSCRIBER_TYPE, SUBSCRIBER_SEGMENT, FIRST_PURCHASE, PLAN_CODE,
                  PLAN_TYPE, PASS_TYPE, BILLING_CYCLE, MIN_AMOUNT, MIN_QUANTITY,
                  CHANNEL, PAYMENT_METHOD, KYC_LEVEL, DAY_OF_WEEK, TIME_RANGE,
                  LOCATION_CODE, TENURE_MONTHS, REFERRAL_CODE

  operator        EQUALS, NOT_EQUALS, IN, NOT_IN, GREATER_THAN, LESS_THAN, BETWEEN

PromotionReward
  promotion, rewardType, value, targetScope, targetCode, maxAmount

  rewardType      PERCENTAGE_OFF, FIXED_AMOUNT_OFF, FREE_PERIODS, FREE_TRIAL_DAYS,
                  FREE_ENTITLEMENT, WAIVE_SETUP_FEE, WALLET_CREDIT, FREE_ADDON,
                  UPGRADE_TIER, EXTRA_ENTITLEMENT_UNITS

  targetScope     WHOLE_ORDER | LINE | PLAN | ADDON | PASS | ENTITLEMENT |
                  SERVICE | RESOURCE | INVENTORY_ITEM | CATEGORY
```

`targetScope` couvre délibérément plusieurs modules. Une promotion doit pouvoir porter sur un plan
d'abonnement, un pass, un service, **une ressource réservable** ou **un article vendu au comptoir**.
Le moteur ne doit donc rien savoir du module d'origine : il manipule un objet tarifable désigné par
un couple portée et code, et chaque module déclare ce qu'il expose. C'est ce qui évite d'écrire trois
moteurs de promotion.

Séparer conditions et récompenses permet la promotion « achetez un pass journée, le second est
offert » sans ajouter de type d'énumération à chaque nouvelle idée commerciale.

Les deux types déclarés mais non traités aujourd'hui, `FREE_ENTITLEMENT` et `WAIVE_SETUP_FEE`,
trouvent ici leur place, ainsi que `FREE_TRIAL_DAYS` que la version actuelle refuse.

### Coupon, distinct de la promotion

```text
Coupon
  id, code, promotion
  couponKind        SINGLE_USE | MULTI_USE | UNIQUE_PER_SUBSCRIBER
  batchCode         lot de generation, pour une campagne imprimee ou par courriel
  assignedToType, assignedToCode   coupon nominatif, ou null pour un coupon public
  maxRedemptions, redemptionCount
  validFrom, validUntil            peut etre plus court que la promotion porteuse
  status            ACTIVE, USED, EXPIRED, REVOKED
  issuedAt, issuedBy, revokedAt, revokedReason
```

Un coupon nominatif à usage unique et un code public partagé sur les réseaux sont deux objets
différents dans la vie réelle. `couponKind` les distingue sans deux entités.

La génération de lot est un besoin concret : mille codes uniques pour un partenariat, imprimés sur
des flyers, avec un suivi du taux d'utilisation par lot.

### Prix spéciaux et grilles tarifaires

Un tarif négocié n'est pas une remise. Il ne doit pas apparaître comme telle sur la facture, et il ne
se cumule pas avec les promotions de la même façon.

```text
PriceList
  id, code, name, currency
  audienceType      ALL | SUBSCRIBER | SEGMENT | BUSINESS_ENTITY | PARTNER
  audienceCode
  validFrom, validUntil
  priority          en cas de chevauchement, la plus prioritaire gagne
  active

PriceListEntry
  priceList, targetScope, targetCode
  priceMode         FIXED_PRICE | PERCENTAGE_OFF_LIST | FIXED_AMOUNT_OFF_LIST
  value, currency
  minQuantity       palier de quantite
  billingCycle      tarif propre a un rythme de facturation
```

Une grille tarifaire répond aux cas suivants, tous réels dans un espace de coworking :

- un partenaire institutionnel dont les membres paient un tarif négocié ;
- une entreprise cliente qui a signé un volume et obtient un prix au poste ;
- un tarif étudiant, ou un tarif association ;
- un tarif dégressif au-delà de cinq postes.

### Parrainage

```text
ReferralProgram
  code, name, status, validFrom, validUntil
  referrerReward     recompense du parrain, reutilise PromotionReward
  refereeReward      recompense du filleul
  qualificationRule  ce qui declenche : inscription, premier paiement, anciennete
  maxReferralsPerReferrer

ReferralLink
  program, referrerType, referrerCode, code, createdAt, clickCount

Referral
  program, referrerCode, refereeCode, status, qualifiedAt,
  referrerRewardGranted, refereeRewardGranted
```

Le parrainage crédite typiquement un portefeuille : c'est le point de jonction avec la partie 3, via
`WalletEntryType.PROMOTIONAL_CREDIT` qui existe déjà.

### Ciblage nominatif, au cas par cas

Les conditions de la section précédente décrivent des populations : les nouveaux, les mensuels, ceux
d'un segment. Il faut aussi pouvoir désigner **des personnes précises**, une par une, sans inventer
un segment artificiel pour chaque geste commercial.

```text
PromotionAudience
  promotion
  audienceType     ALL | SEGMENT | SUBSCRIBER_LIST | BUSINESS_ENTITY | PLAN | COHORT
  segmentCode              pour SEGMENT
  planCode                 pour PLAN
  cohortRule               pour COHORT, par exemple inscrits entre deux dates

PromotionBeneficiary
  promotion
  subscriberType, subscriberCode
  addedBy, addedAt, addedReason
  notifiedAt, notificationChannel
  redeemedAt
  revokedAt, revokedBy
```

Trois usages que cela débloque, tous courants :

1. **Le geste commercial.** Un client mécontent reçoit trente pour cent sur son prochain mois. La
   promotion existe, elle ne vise que lui, elle expire, et la trace reste.
2. **La liste d'invités.** Un partenariat donne droit à un tarif préférentiel à quarante personnes
   nommées. On importe la liste, on notifie, on suit le taux d'utilisation.
3. **La relance ciblée.** Les abonnés partis dans les six derniers mois reçoivent une offre de retour.

Points de conception :

- **Import de masse d'une liste de bénéficiaires**, par fichier, avec rapport ligne à ligne sur le
  modèle de l'import du module inventaire : lignes acceptées, lignes rejetées et pourquoi.
- **Un bénéficiaire peut être retiré** avant utilisation, ce qui est la seule façon d'annuler un
  geste accordé par erreur.
- **La notification est tracée** : savoir qui a été prévenu, quand et par quel canal, faute de quoi
  un client réclamera une offre qu'il n'a jamais reçue.
- Une promotion nominative **n'apparaît qu'à ses bénéficiaires**. Elle ne doit fuiter ni dans le
  catalogue public, ni dans une réponse d'API consultée par un autre client.

## 2.3 Application automatique et affichage des prix promotionnels

Une promotion qui n'est visible qu'au moment de payer ne vend rien. Ce qui déclenche l'achat, c'est
le prix barré vu au moment de choisir.

### Le moteur doit savoir tarifer une liste, pas seulement un panier

```text
PricingEngine.priceCatalogue(PricingContext, List<PriceableRef>) -> List<EffectivePrice>

PriceableRef
  scope     PLAN | PASS | SERVICE | ADDON | RESOURCE | INVENTORY_ITEM
  code
  quantity, billingCycle, dates    selon la portee

EffectivePrice
  scope, code
  listPrice                prix catalogue, celui qui sera barre
  effectivePrice           prix a payer
  savingsAmount, savingsPercent
  priceSource              CATALOGUE | PRICE_LIST | PROMOTION | OVERRIDE
  promotionCode, promotionLabel      libelle a afficher, par exemple moins vingt pour cent
  validUntil               pour afficher une echeance et creer l urgence
  requiresCoupon           vrai si le prix suppose la saisie d un code
  conditionsSummary        ce qu il faut remplir, en une phrase lisible
```

Le champ `priceSource` est ce qui permet au front d'afficher honnêtement : un tarif négocié n'est pas
une promotion et ne doit pas s'afficher comme une réduction exceptionnelle.

### Où cela s'applique

Toute liste de prix vue par un client doit passer par ce calcul, jamais afficher le prix catalogue
brut : la grille des plans, le catalogue des pass, les services optionnels, les ressources
réservables, les articles vendus au comptoir.

### Performance et fraîcheur

Calculer les promotions applicables à chaque affichage de catalogue, pour chaque visiteur, est
coûteux. Trois mesures :

1. **Mise en cache par profil**, pas par personne : le couple segment, type d'abonné et canal suffit
   dans la grande majorité des cas. Les promotions nominatives, elles, ne se cachent pas.
2. **Invalidation sur événement** : activation, suspension, épuisement de budget ou fin de campagne
   vident le cache concerné. L'outbox est le bon véhicule.
3. **Pré-calcul nocturne** des prix effectifs du catalogue public, rafraîchi à chaque changement de
   campagne.

### Le prix affiché engage

Si un prix promotionnel est affiché, il doit être honoré jusqu'à la fin du parcours d'achat. Deux
garde-fous : le prix effectif est **figé dans le panier** avec sa date d'expiration, et le moteur
**revérifie à la validation**. Si la promotion a expiré entre-temps, le client doit être prévenu
explicitement plutôt que de voir un montant changer en silence.

## 2.4 Parcours client : coupons disponibles et application avant paiement

### Ce que le client doit voir

```text
GET /client/promotions/available
```

La liste de ce à quoi il a droit maintenant : promotions automatiques déjà appliquées, promotions
nominatives qui lui sont réservées, coupons qui lui ont été attribués, coupons publics en cours. Pour
chacun : le libellé, l'économie, la date de fin, et ce qu'il faut faire pour en bénéficier.

```text
ClientPromotionView
  kind            AUTOMATIC | NOMINATIVE | COUPON_ASSIGNED | COUPON_PUBLIC
  code            null pour une promotion automatique
  label, description
  estimatedSaving sur le panier courant, ou sur son abonnement
  validUntil
  status          APPLICABLE | ALREADY_APPLIED | NOT_ELIGIBLE | EXPIRED | EXHAUSTED
  ineligibilityReason   lisible, pas un code technique
```

Afficher aussi ce à quoi il **n'a pas** droit, avec la raison, est un choix délibéré : un client qui
comprend pourquoi son code est refusé n'écrit pas au support.

### Appliquer un coupon avant de payer

```text
POST /client/cart/coupons        saisie d un code, renvoie le panier recalcule
DELETE /client/cart/coupons/{code}
POST /client/cart/preview        recalcul complet sans engagement
```

Le parcours attendu :

1. Le client saisit un code, ou choisit un coupon dans sa liste.
2. Le moteur **valide sans consommer** : le code existe-t-il, est-il actif, lui est-il destiné,
   le panier remplit-il les conditions, le budget est-il épuisé.
3. Le panier est recalculé et **le détail est montré** : quelle règle a joué, sur quelle ligne, pour
   quel montant.
4. En cas de refus, le message dit **pourquoi**, en français, pas un code d'erreur.
5. Le coupon n'est **réellement consommé qu'au paiement**, pas à la saisie.

Ce point cinq est essentiel. Un coupon à usage unique consommé dès la saisie serait perdu si le
client abandonne son panier. La réservation temporaire est la bonne mécanique :

```text
CouponReservation
  coupon, cartReference, subscriberCode
  reservedAt, expiresAt        courte, le temps du parcours de paiement
  status      HELD | CONSUMED | RELEASED | EXPIRED
```

C'est exactement le mécanisme de retenue déjà utilisé pour le stock et pour le portefeuille : on
réserve, puis on capture ou on libère. Un travailleur de fond libère les réservations expirées.

### Cumul visible

Quand plusieurs promotions s'appliquent, le client doit voir le détail ligne par ligne plutôt qu'un
montant global. Et quand un coupon saisi est **moins avantageux** qu'une promotion automatique déjà
active et non cumulable, le système doit retenir le meilleur pour le client et le dire.

## 2.5 Le moteur de tarification

C'est la pièce manquante. Sans elle, tout ce qui précède reste déclaratif.

```text
PricingEngine.evaluate(PricingContext) -> PricingResult

PricingContext
  subscriberType, subscriberCode, subscriberSegment, kycLevel, tenureMonths
  lines               ce qui est achete : plan, pass, addon, article
  billingCycle, channel, paymentMethod, locationCode
  couponCodes         codes saisis par le client
  evaluationDate

PricingResult
  originalTotal
  appliedRules        liste ordonnee, chacune avec son montant et sa justification
  discountTotal
  finalTotal
  rejectedCoupons     avec le motif du refus, lisible par l'utilisateur
```

### Ordre d'évaluation, à fixer une fois pour toutes

L'ordre change le montant final. Le figer et le documenter évite des écarts inexplicables.

1. **Prix catalogue** : le tarif de la version de plan en vigueur.
2. **Grille tarifaire** : si une grille s'applique, elle remplace le prix catalogue. Ce n'est pas une
   remise, cela ne se voit pas comme telle sur la facture.
3. **Conditions propres à l'abonnement** : si l'abonné bénéficie d'un abonnement dérivé, décrit en
   partie 5, son prix négocié remplace tout ce qui précède. C'est un terme de contrat, pas une
   promotion.
4. **Promotions automatiques**, par priorité croissante.
5. **Coupons saisis**, par priorité croissante.
6. **Plafonnement** : application de `maxDiscountAmount` et du budget restant.
7. **Arrondi** : à l'entier XAF, une seule fois, à la fin.

Un abonnement dérivé peut porter `promotionsAllowed = false` : un prix déjà négocié n'a pas vocation
à recevoir une remise supplémentaire. L'évaluation s'arrête alors après l'étape 3.

Règles de cumul : une promotion `exclusive` qui s'applique arrête l'évaluation des suivantes. Une
promotion non `stackable` ne s'ajoute pas à une autre déjà retenue. En cas d'égalité de priorité, la
règle la plus avantageuse pour le client l'emporte, choix commercial à confirmer.

### Simulation avant application

```text
POST /pricing/simulate
```

Le même moteur, sans effet de bord, renvoyant le détail des règles appliquées et des coupons refusés
avec leur motif. Indispensable pour trois usages : l'affichage du panier côté client, l'aide à la
vente côté commercial, et le test d'une campagne avant son lancement.

## 2.6 Application et traçabilité

Une remise calculée doit laisser une trace exploitable.

```text
AppliedDiscount
  sourceType        PROMOTION | COUPON | PRICE_LIST | REFERRAL | MANUAL
  sourceCode
  documentType      BILLING_DOCUMENT | SUBSCRIPTION | PASS
  documentCode
  lineReference
  originalAmount, discountAmount, finalAmount
  appliedAt, appliedBy
  reversedAt, reversalReason
```

`BillingDocumentDiscount` existe déjà et fonctionne. La bonne approche est de **lui ajouter une clé
vers l'origine** plutôt que de créer une table concurrente : `sourceType` et `sourceCode`, plus une
contrainte qui interdit un `discountCode` inconnu lorsque la source est une promotion.

### Ce que cela permet enfin

- Le **coût réel des promotions**, par campagne, par période, par canal.
- Le **taux d'utilisation** d'un lot de coupons.
- La **marge après remise**, par plan et par segment.
- Le **contrôle** : une remise manuelle hors promotion doit être justifiée et visée, exactement comme
  un forçage de stock l'est déjà dans le module inventaire.

## 2.7 Garde-fous

1. **Approbation des campagnes** au-delà d'un seuil de budget ou d'un taux de remise, sur le modèle
   des règles d'approbation déjà en place pour les achats et les ajustements de stock.
2. **Budget consommé mis à jour dans la transaction**, pour qu'une campagne à budget épuisé cesse
   immédiatement de s'appliquer, y compris sous forte concurrence.
3. **Coupon à usage unique verrouillé** pendant l'évaluation, sinon deux paniers simultanés
   consomment le même code.
4. **Remise plafonnée par une valeur absolue**, même en pourcentage : quatre-vingts pour cent sur un
   abonnement annuel d'entreprise n'est presque jamais l'intention.
5. **Aucune remise ne descend le total sous zéro**, le reliquat devant devenir un crédit explicite et
   non une facture négative.

## 2.8 Endpoints

```text
POST   /promotions                             PUT    /promotions/{code}
PATCH  /promotions/{code}/activate | /pause | /archive
POST   /promotions/{code}/conditions           POST   /promotions/{code}/rewards
GET    /promotions                             GET    /promotions/{code}
GET    /promotions/{code}/performance          cout, utilisations, taux de conversion

POST   /promotions/{code}/beneficiaries         ajout nominatif, un ou plusieurs
POST   /promotions/{code}/beneficiaries/import  import de liste par fichier
DELETE /promotions/{code}/beneficiaries/{subscriberCode}
GET    /promotions/{code}/beneficiaries         avec taux d utilisation
POST   /promotions/{code}/notify                notification des beneficiaires

POST   /coupons/batches                        generation de lot
GET    /coupons/batches/{batchCode}            suivi du lot
POST   /coupons                                coupon unitaire ou nominatif
PATCH  /coupons/{code}/revoke
GET    /coupons/{code}/validate                verification sans consommation

GET    /catalogue/prices                       prix effectifs d une liste d objets tarifables
GET    /catalogue/prices/{scope}/{code}        prix effectif d un seul

GET    /client/promotions/available            ce a quoi l abonne a droit aujourd hui
GET    /client/coupons                         ses coupons, utilises et disponibles
POST   /client/cart/preview                    recalcul complet sans engagement
POST   /client/cart/coupons                    saisie d un code, panier recalcule
DELETE /client/cart/coupons/{code}

POST   /price-lists                            POST /price-lists/{code}/entries
GET    /price-lists                            GET  /price-lists/resolve
POST   /referral-programs                      GET  /referrals
GET    /client/referral-link                   lien de parrainage de l'abonne

POST   /pricing/simulate                       simulation sans effet de bord
GET    /pricing/applicable                     ce a quoi un abonne a droit aujourd'hui
```

---

# Partie 3 · Portefeuille électronique

## 3.1 Nature juridique : une avance client, pas de la monnaie électronique

**Décision actée.** Le portefeuille ne constitue pas une émission de monnaie électronique. Ce qu'il
enregistre est une **avance versée par le client**, donc une dette de l'entreprise envers lui,
utilisable pour payer ses prestations. Le niveau d'exigence visé est en revanche celui d'un
opérateur de mobile money : même rigueur, même traçabilité, même confort d'usage.

Cette distinction n'est pas cosmétique. Elle est confortable juridiquement, mais elle **n'est pas
acquise une fois pour toutes** : certaines fonctionnalités, si elles sont ouvertes sans garde-fou,
feraient basculer le portefeuille du côté du service de paiement. Trois règles de frontière
préservent la qualification d'avance client.

### Règle 1 · Pas de retrait en espèces

Un solde qui peut ressortir en cash n'est plus une avance sur prestation, c'est un dépôt. Le
portefeuille ne doit donc **pas proposer de retrait**. `WalletLimitPolicy.withdrawalAllowed` reste à
`false` par défaut et n'est levé que pour un cas : la clôture du compte, avec remboursement **vers la
source d'origine** et non en espèces.

C'est la règle la plus structurante, et la plus tentante à contourner. Il faut la tenir.

### Règle 2 · Le solde ne sert qu'à payer l'entreprise

Un portefeuille dont on peut payer des tiers est un instrument de paiement. Ici, les débits possibles
se limitent aux prestations de l'entreprise : abonnements, pass, services, factures. Le catalogue des
types d'écriture doit refléter cette clôture, et aucun bénéficiaire externe ne doit pouvoir être
désigné.

### Règle 3 · Le transfert entre abonnés reste encadré

C'est le point le plus délicat, traité en détail en 3.4. Transférer une avance d'un client à un autre
est juridiquement une **cession de créance entre deux clients**, ce qui reste admissible tant que
l'opération demeure occasionnelle, plafonnée, sans frais lucratifs, et sans possibilité de sortie en
espèces au bout de la chaîne. Ouvert sans limite, le mécanisme deviendrait un service de transfert de
fonds.

### Ce que cela implique au bilan

Le solde des portefeuilles est une **dette d'exploitation**, à inscrire au passif sur un compte
d'avances et acomptes reçus des clients, à faire confirmer par votre comptable pour le numéro de
compte exact dans votre plan SYSCOHADA. Ce n'est ni du produit, ni de la trésorerie disponible
librement : l'encaissement initial est un produit constaté d'avance, le produit n'étant acquis qu'à
la consommation de la prestation.

Conséquence opérationnelle directe : **le total des portefeuilles doit être adossé à de la trésorerie
réellement disponible**. L'état de rapprochement décrit en 3.7 n'est pas un confort, c'est la
contrepartie de cette liberté juridique.

## 3.2 Ce qu'il faut entendre par fiable

Trois exigences, de nature différente.

**Intégrité** : le solde affiché est toujours égal à la somme des écritures, et personne ne peut
modifier une écriture passée. Le socle actuel y est presque : il manque l'immuabilité en base et un
contrôle de réconciliation.

**Non-répudiation** : le titulaire ne peut pas nier avoir ordonné une opération. Cela suppose un
secret que lui seul détient, et une trace de son usage. C'est ce que le code PIN apporte.

**Démontrabilité** : la capacité d'établir, devant un client en litige ou un vérificateur, qui
détient quoi, d'où vient chaque mouvement, et que les plafonds ont été respectés. L'exigence est la
même que pour un opérateur de mobile money, même si l'obligation juridique, elle, ne l'est pas.

## 3.3 Code PIN et confirmation des opérations

```text
WalletCredential
  wallet
  pinHash              empreinte, jamais le code en clair
  pinAlgorithm         algorithme et parametres, pour pouvoir migrer plus tard
  pinSetAt, pinExpiresAt
  failedAttempts, lockedUntil
  mustChangePin        vrai apres une reinitialisation par un administrateur
  lastUsedAt
  createdAt, updatedAt
```

Règles :

- Le code est **haché avec un algorithme lent et salé**, jamais chiffré ni stocké en clair. Un
  administrateur ne peut pas le lire, seulement le réinitialiser.
- **Verrouillage progressif** après échecs répétés : temporisation croissante, puis blocage exigeant
  une réinitialisation.
- **Codes triviaux refusés** : suites, répétitions, date de naissance du titulaire.
- **Le code ne quitte jamais le client en clair côté journaux** : aucune trace dans les logs, aucun
  écho dans une réponse d'erreur.
- **Changement de code** exigeant l'ancien, ou une réinitialisation vérifiée par un second canal.

### Confirmation d'une opération

```text
WalletTransactionConfirmation
  confirmationCode     identifiant de la demande
  wallet, operationType, amount, counterpartyLabel
  challengeType        PIN | OTP | PIN_AND_OTP
  status               PENDING, CONFIRMED, REJECTED, EXPIRED
  expiresAt            courte, quelques minutes
  attempts, maxAttempts
  payloadHash          empreinte de l'operation, pour qu'elle ne puisse pas changer
                       entre la demande et la confirmation
  confirmedAt, rejectedAt
  ipAddress, deviceId
```

Le point important est `payloadHash` : sans lui, un attaquant pourrait faire confirmer un transfert
de mille et exécuter un transfert de cent mille. L'empreinte lie la confirmation à l'opération exacte.

Le niveau d'exigence doit dépendre du montant et du type : un paiement de facture interne peut se
contenter du PIN, un transfert vers un tiers mérite un second canal au-delà d'un seuil.

## 3.4 Transfert entre abonnés

```text
WalletTransfer
  transferNumber
  sourceWallet, destinationWallet
  amount, currency
  fee                    frais eventuels, a parametrer
  reference              libelle saisi par l'emetteur
  status                 PENDING_CONFIRMATION, CONFIRMED, COMPLETED,
                         REJECTED, EXPIRED, CANCELLED, REVERSED
  confirmationCode       lien vers la confirmation
  initiatedBy, initiatedAt, completedAt
  reversedAt, reversalReason, reversedBy
  debitEntryNumber, creditEntryNumber
```

Mécanique :

1. L'émetteur désigne le destinataire, par numéro de portefeuille, par numéro de téléphone ou par
   code membre. Le système renvoie **le nom du destinataire pour confirmation visuelle**, garde-fou
   le plus efficace contre l'erreur de saisie.
2. Le montant est **retenu** sur le portefeuille source, en réutilisant `WalletHold` qui existe déjà,
   pas débité immédiatement.
3. L'émetteur confirme par PIN.
4. La retenue est capturée, le destinataire crédité, dans **une seule transaction** : les deux
   écritures existent ou aucune.
5. Les deux parties sont notifiées.

Option à trancher : le destinataire doit-il accepter ? Un transfert à acceptation protège contre
l'erreur de destinataire mais complique le parcours. Recommandation : non par défaut, avec une
fenêtre d'annulation courte côté émetteur tant que le destinataire n'a pas utilisé les fonds.

### Garde-fous imposés par la qualification d'avance client

Le transfert est la fonctionnalité qui approche le plus la frontière décrite en 3.1. Cinq contraintes
la maintiennent du bon côté, et elles doivent être posées dès la conception plutôt qu'ajoutées après
coup.

1. **Entre abonnés seulement.** L'émetteur et le destinataire doivent tous deux être clients actifs.
   Pas de transfert vers un portefeuille fermé, suspendu, ou vers un tiers non client.
2. **Plafonné, et le plafond compte.** Montant par opération, nombre d'opérations par mois, volume
   cumulé. `WalletLimitPolicy` porte déjà ces bornes : c'est ici qu'elles servent le plus.
3. **Sans frais lucratifs.** Un frais proportionnel qui rapporte transforme le service en activité de
   transfert. Si un frais est appliqué, il doit couvrir un coût réel et rester forfaitaire.
4. **Sans sortie en espèces au bout de la chaîne.** Le destinataire hérite d'une avance, soumise aux
   mêmes règles : elle ne peut que payer l'entreprise.
5. **Motif conservé.** Le libellé saisi par l'émetteur, la date et les deux identités restent
   attachés à l'opération. Un transfert anonyme n'a pas sa place ici.

Un seuil d'alerte doit remonter dans le centre de contrôle quand un portefeuille devient un point de
passage : beaucoup d'entrées, beaucoup de sorties, peu de consommation réelle. C'est le signal que
l'usage dérive de ce que l'avance client est censée être, et `WalletRiskFlag.RAPID_IN_OUT` est fait
pour cela.

## 3.5 Initiation de paiement vers l'entreprise

Le cas d'usage : un abonné paie une prestation depuis son portefeuille, sans passer par une facture
préexistante.

```text
WalletMerchantPayment
  paymentNumber
  wallet, amount, currency
  serviceCode            prestation visee, du catalogue de services
  description
  status                 PENDING_CONFIRMATION, CONFIRMED, COMPLETED, REJECTED, REFUNDED
  billingDocumentCode    facture emise en contrepartie
  confirmationCode
  initiatedAt, completedAt
```

Point de conception : **un paiement vers l'entreprise doit produire une pièce comptable**. Sans
facture ou reçu en face, l'encaissement est un flux sans justification, ce que le module billing
sait déjà éviter pour les autres moyens de paiement. Le paiement déclenche donc l'émission d'un
document, en réutilisant la chaîne existante.

## 3.6 Limites, plafonds et niveaux

```text
WalletLimitPolicy
  code, name
  kycLevel                niveau de verification exige pour cette politique
  maxBalance              solde maximum detenu
  maxPerTransaction
  maxDailyOut, maxMonthlyOut
  maxDailyIn, maxMonthlyIn
  maxTransfersPerDay
  transferAllowed, merchantPaymentAllowed, withdrawalAllowed
  active

WalletLimitUsage
  wallet, periodType, periodStart
  outboundAmount, inboundAmount, transferCount
```

Le rattachement au niveau KYC est cohérent avec ce qui existe déjà : `PassPlan.requiredKycLevel` et
`Subscription.kycCompliant` montrent que la notion est en place. Un portefeuille non vérifié doit
plafonner bas ; la vérification débloque des paliers.

Le compteur d'usage est tenu en base, pas recalculé à la volée : sous concurrence, deux opérations
simultanées ne doivent pas franchir ensemble un plafond que chacune respecte seule.

## 3.7 Intégrité et conformité

### Immuabilité du journal

`WalletLedgerEntry` doit devenir intouchable, exactement comme `stock_movement` l'est devenu au lot 0
de l'inventaire et comme `billing_document` l'est déjà. Le mécanisme est connu et éprouvé dans ce
projet : un déclencheur PostgreSQL qui refuse toute modification et toute suppression, la correction
passant par une écriture de contre-passation.

### Chaînage des écritures

```text
WalletLedgerEntry, colonnes ajoutees
  previousHash, currentHash
```

Chaque écriture porte l'empreinte de la précédente sur le même portefeuille. Une écriture retirée ou
modifiée rompt la chaîne, et un contrôle périodique le détecte. Le module billing applique déjà ce
principe avec `previous_hash` et `current_hash` : la même approche, transposée.

### Réconciliation

Un contrôle qui vérifie, pour chaque portefeuille, que `ledgerBalance` égale la somme signée des
écritures, et que `availableBalance` égale `ledgerBalance` moins `heldBalance`. Même logique que la
réconciliation de stock du lot 0 : une divergence signale un défaut, jamais une valeur à écraser.

### Compte de contrepartie et adossement en trésorerie

Un portefeuille crédité est une dette de l'entreprise envers son titulaire, inscrite au passif sur un
compte d'avances et acomptes reçus des clients. La somme des soldes doit correspondre à ce compte.

**L'état de rapprochement entre le total des portefeuilles et la trésorerie disponible n'est pas
optionnel.** C'est la contrepartie directe du choix de ne pas être émetteur de monnaie électronique :
aucune autorité ne vous impose de cantonner les fonds, donc c'est à vous de vérifier que vous pouvez
honorer les prestations déjà payées. Un écart persistant entre l'encours des portefeuilles et la
trésorerie signifie que des avances clients ont financé autre chose que ce pour quoi elles ont été
versées.

```text
WalletTreasuryReconciliation
  reconciliationDate
  totalWalletBalance          somme des soldes comptables
  ledgerAccountBalance        solde du compte d avances au passif
  availableCash               tresorerie reellement disponible
  coverageRatio               availableCash rapporte a totalWalletBalance
  variance, varianceExplained, explainedBy
  status                      BALANCED | VARIANCE | UNDER_REVIEW
```

Un ratio de couverture inférieur à un seuil défini doit alerter la direction, pas seulement figurer
dans un rapport.

### Surveillance

```text
WalletRiskFlag
  wallet, flagType, severity, status
  detectedAt, detectedBy, reviewedBy, reviewedAt, resolution

  flagType   UNUSUAL_VOLUME, RAPID_IN_OUT, STRUCTURING, MANY_COUNTERPARTIES,
             DORMANT_REACTIVATION, FAILED_PIN_BURST, LIMIT_BREACH_ATTEMPT,
             NEGATIVE_BALANCE
```

Détection automatique, revue humaine, et possibilité de placer le portefeuille en `UNDER_REVIEW`,
statut qui existe déjà dans `WalletStatus` sans être utilisé.

### Dormance et clôture

Un portefeuille sans mouvement pendant une longue période, et un portefeuille dont le titulaire part,
posent la même question : que devient le solde. Il faut une politique écrite, des relances, et une
procédure de clôture avec restitution. Le champ `closedAt` existe, la procédure non.

## 3.8 Autres fonctionnalités à prévoir

1. **Rechargement par mobile money en autonomie.** L'intégration PawaPay existe déjà pour les
   paiements. La brancher sur le portefeuille permet à l'abonné de se recharger seul, ce qui supprime
   le goulot du rechargement administrateur.
2. **Relevé de portefeuille en PDF**, mensuel ou à la demande. Le module billing sait déjà produire
   et sceller des PDF.
3. **Débit automatique des factures** à l'échéance, avec accord préalable du titulaire et
   notification avant prélèvement.
4. **Épargne fléchée** : réserver une partie du solde pour le renouvellement d'un abonnement, en
   réutilisant `WalletHold`.
5. **Portefeuille d'entreprise avec sous-comptes**, pour qu'une société alimente et que ses membres
   consomment dans une limite. Le modèle de sièges du module abonnement offre un précédent.
6. **Cashback et crédits promotionnels** : les types d'écriture existent déjà, il ne manque que le
   déclencheur, qui viendra du moteur de la partie 2.
7. **Remboursement vers la source**, avec traçabilité du sens de retour. C'est le seul mode de sortie
   admis : ce qui est entré par mobile money repart par mobile money, sur le même numéro. Jamais
   d'espèces, jamais vers un tiers, jamais vers un autre instrument que celui d'origine.
8. **Export comptable** des mouvements de portefeuille, sur le modèle de l'export des écritures de
   stock, avec la ventilation entre avance reçue et produit acquis à la consommation.
9. **Péremption des crédits promotionnels.** Un crédit offert n'est pas une avance versée par le
   client : il peut porter une date limite, contrairement au solde payé. Les deux doivent donc être
   distingués dans le solde, faute de quoi une expiration mordrait sur l'argent du client.

### Ce qui est délibérément exclu

| Fonctionnalité | Pourquoi elle est écartée |
|---|---|
| Retrait en espèces | Ferait du solde un dépôt, et non une avance sur prestation |
| Paiement vers un tiers hors entreprise | Ferait du portefeuille un instrument de paiement |
| Rémunération du solde | Rapprocherait le compte d'un produit d'épargne |
| Transfert sans plafond ni traçabilité | Basculerait vers un service de transfert de fonds |

Ces quatre exclusions sont ce qui permet de tenir le reste. Elles méritent d'être rappelées en tête
de tout cahier des charges dérivé de ce document, parce que chacune sera demandée un jour par
quelqu'un qui ne connaît pas la raison du refus.

## 3.9 Centre de contrôle administrateur

Un écran unique, avec les droits qui vont avec.

**Vue d'ensemble** : nombre de portefeuilles par statut, encours total, mouvements du jour, alertes
ouvertes, écarts de réconciliation, tentatives de dépassement de plafond.

**Par portefeuille** : solde et historique, retenues actives, limites applicables, état du code PIN
et verrouillages, signalements, contreparties fréquentes.

**Actions**, chacune tracée et soumise à permission :

```text
WALLET_VIEW          consulter
WALLET_TOPUP         crediter
WALLET_DEBIT         debiter, avec justification obligatoire
WALLET_ADJUST        ajustement de regularisation
WALLET_REVERSE       contre-passer une ecriture
WALLET_SUSPEND       suspendre, lever la suspension
WALLET_RESET_PIN     reinitialiser le code, jamais le lire
WALLET_RAISE_LIMIT   accorder une derogation de plafond, bornee dans le temps
WALLET_CLOSE         cloturer avec restitution
WALLET_REVIEW_FLAG   traiter un signalement
```

Deux principes : **aucune action administrative sans motif saisi**, et **séparation des rôles** sur
les opérations sensibles, un débit important exigeant un second visa comme les ajustements de stock
au-delà d'un seuil.

---

# Partie 4 · Opérationnalisation des pass

## 4.1 Ce qui manque n'est pas le modèle

Le modèle est bon. Ce qui manque est le parcours d'usage : le moment où quelqu'un se présente et où
le pass doit être validé, décompté, et refusé s'il ne donne pas droit.

## 4.2 Utilisation d'un pass

```text
PassUsage
  usageNumber, pass
  usedByType, usedByCode      celui qui s'en sert, pas forcement le titulaire
  usageType                   CHECK_IN, BOOKING, SERVICE, MEETING_ROOM, PRINT, OTHER
  locationCode, resourceCode
  quantity, entitlementCode   ce qui est decompte
  startedAt, endedAt
  status                      RESERVED, ACTIVE, COMPLETED, CANCELLED, NO_SHOW
  validatedBy, validationChannel   QR_SCAN, MANUAL, KIOSK, API
  reversedAt, reversalReason
```

Le parcours :

1. **Présentation** : scan du QR du pass, ou saisie du numéro.
2. **Validation** : le pass est-il actif, dans sa période, dans ses plages horaires, sur le bon site,
   le porteur a-t-il le droit de s'en servir, reste-t-il du droit à consommer.
3. **Réservation ou consommation**, selon le `ConsumptionMode` du droit, mécanique qui existe déjà.
4. **Décompte** : `usedCount` incrémenté, `EntitlementLedger` mouvementé.
5. **Bascule d'état** : `PARTIALLY_USED` puis `CONSUMED` quand `maxUses` est atteint.

Le point 2 est le cœur : un **service de validation unique**, appelé par tous les canaux, qui répond
oui ou non avec un motif lisible. Dupliquer cette règle entre le module checkin, le module booking et
un kiosque produirait trois comportements divergents.

## 4.3 Règles de validité fines

```text
PassValidityRule
  pass ou passPlanVersion
  ruleType        DAY_OF_WEEK | TIME_RANGE | LOCATION | RESOURCE_TYPE |
                  BLACKOUT_DATE | MAX_PER_DAY | MAX_CONCURRENT
  value
  allowOrDeny
```

Ce qui devient exprimable : un pass valable en semaine seulement, un pass heures creuses, un pass
limité à un site, un pass bloqué les jours fériés, un pass limitant à une visite par jour.

## 4.4 Transfert et partage

Les deux booléens existent. Voici ce qu'ils devraient déclencher.

**Transfert** : le pass change de titulaire définitivement.

```text
PassTransfer
  pass, fromOwnerType, fromOwnerCode, toOwnerType, toOwnerCode
  status          PENDING_ACCEPTANCE, ACCEPTED, REJECTED, CANCELLED, COMPLETED
  reason, transferFee
  requestedAt, requestedBy, acceptedAt, completedAt
```

Règles : uniquement si `transferable`, uniquement sur un pass actif et non consommé, avec acceptation
du destinataire, et vérification que celui-ci remplit les conditions du plan, dont le niveau KYC.

**Partage** : plusieurs personnes peuvent utiliser le même pass, ce qui est le sens de
`COMPANY_SHARED_PASS`.

```text
PassBeneficiary
  pass, beneficiaryType, beneficiaryCode
  role              HOLDER | AUTHORISED_USER
  maxUsesForBeneficiary   quota individuel dans le quota global
  usedCount
  validFrom, validUntil
  addedBy, addedAt, revokedAt
```

Une entreprise achète un pass de vingt journées, désigne cinq collaborateurs, et peut plafonner
chacun à six journées. Sans cette table, `COMPANY_SHARED_PASS` reste un mot.

## 4.5 Matérialisation du pass

```text
PassCredential
  pass, credentialType    QR_CODE | BARCODE | NFC_TAG | ACCESS_CODE
  value, rotating         un code tournant limite la copie d'ecran
  validUntil, revokedAt
```

Le projet produit déjà des QR pour l'inventaire et possède une bibliothèque de génération. Un pass
doit être présentable, donc imprimable et affichable en PDF ou dans un portefeuille mobile.

## 4.6 Autres manques

1. **Prolongation et suspension à la demande**, avec ou sans frais, et impact sur `validUntil`.
2. **Report du solde non consommé** vers la période suivante. Le sous-module `rollover` existe pour
   les abonnements : l'étendre aux pass plutôt que de refaire.
3. **Remboursement partiel** d'un pass peu utilisé, avec sa politique de calcul.
4. **Invitations visiteurs** rattachées à un pass : `VISITOR_PASS` existe comme type, le module
   `visitor` existe, le lien entre les deux reste à décrire.
5. **Alertes** : pass bientôt expiré, solde bientôt épuisé, pass inutilisé, le tout sur
   l'infrastructure de notification déjà en place.
6. **Vente de pass en libre-service** depuis le portail client, payée depuis le portefeuille, ce qui
   relie les parties 2, 3 et 4.

---

# Partie 5 · Abonnements et nouveaux services

## 5.1 Catalogue de services, au-dessus des plans

Le modèle actuel raisonne en plans et en droits. La domiciliation impose d'introduire la notion de
**service souscrit**, qui a ses propres obligations opérationnelles.

```text
ServiceDefinition
  code, name, description
  serviceCategory     DOMICILIATION | MAIL_HANDLING | PHONE_ANSWERING | STORAGE |
                      MEETING_ROOM | COWORKING_ACCESS | PRIVATE_OFFICE |
                      ADMIN_SUPPORT | LEGAL_SUPPORT | ACCOUNTING_SUPPORT |
                      IT_SUPPORT | EVENT_SPACE | PARKING | LOCKER | OTHER
  deliveryMode        CONTINUOUS | ON_DEMAND | SCHEDULED | METERED
  requiresContract, requiresKycLevel
  requiresPhysicalResource
  hasRegulatoryObligations
  defaultBillingCycle, defaultNoticePeriodDays, defaultCommitmentMonths
  active

SubscriptionService
  subscription, serviceDefinition
  status, activatedAt, suspendedAt, terminatedAt
  quantity, unitPrice
  serviceMetadataJson   donnees propres au service
```

Un abonnement devient alors un contenant : il porte un plan, et un ou plusieurs services.

## 5.2 Domiciliation

Le service qui demande le plus de mécanique propre.

### Trois règles métier actées, qui structurent le modèle

**1. Trois rythmes de facturation : mensuel, trimestriel, annuel.** `BillingCycle` porte déjà
`MONTHLY`, `QUARTERLY` et `YEARLY`, aucune extension n'est nécessaire de ce côté.

**2. L'adresse n'est fiscale que si l'abonnement est pris pour un an.** C'est une règle de gestion
forte, pas une option de configuration. Elle a trois conséquences directes :

- une souscription mensuelle ou trimestrielle donne une **adresse commerciale seulement** ;
- l'attestation utilisable auprès de l'administration fiscale ne peut être émise que sur un contrat
  annuel, et le système doit **refuser de la produire** autrement ;
- un client qui passe d'annuel à mensuel **perd la qualité fiscale de son adresse**. Ce n'est pas un
  détail administratif : c'est un changement qui doit déclencher une notification, la révocation de
  l'attestation en cours, et probablement une information de l'administration.

Le champ ne peut donc pas être un simple booléen saisi à la main. Il se **déduit** de l'engagement,
et toute modification de l'engagement le recalcule.

**3. Le contrat de domiciliation est obligatoire, enregistré et timbré.** Une domiciliation sans
contrat enregistré auprès de l'administration publique n'existe pas. Le contrat n'est donc pas une
pièce jointe facultative : c'est une **étape bloquante du cycle de vie**. Tant qu'il n'est pas
enregistré et timbré, la domiciliation ne peut pas être pleinement opposable, et l'attestation ne
doit pas être délivrée.

### Modèle

```text
DomiciliationContract
  contractNumber, subscription, subscriptionService
  businessEntity                 la societe domiciliee
  legalName, legalForm, registrationNumber, taxNumber
  assignedAddressId              adresse commerciale attribuee
  suiteNumber                    complement distinctif, boite ou bureau

  -- Engagement et qualite de l adresse
  billingCycle                   MONTHLY | QUARTERLY | YEARLY
  commitmentMonths
  fiscalAddressEligible          derive : vrai seulement si engagement de douze mois
  fiscalAddressGrantedAt, fiscalAddressRevokedAt, fiscalAddressRevocationReason

  status        DRAFT, PENDING_DOCUMENTS, PENDING_SIGNATURE, PENDING_REGISTRATION,
                ACTIVE, SUSPENDED, TERMINATED, EXPIRED
  startDate, endDate, noticePeriodDays
  legalRepresentativeCode

  contractDocumentCode           contrat signe, via features/document
  certificateDocumentCode        attestation de domiciliation
  certificateValidUntil, certificateScope   COMMERCIAL | FISCAL

  mailForwardingMode  HOLD | FORWARD | SCAN_AND_FORWARD | SCAN_ONLY
  forwardingAddressId, forwardingFrequency
  terminatedAt, terminationReason
```

```text
DomiciliationRegistration
  domiciliationContract
  status              PENDING, SUBMITTED, REGISTERED, REJECTED
  submittedAt, submittedBy
  administrationOffice          service aupres duquel l enregistrement est fait
  registrationNumber            reference delivree par l administration
  registrationDate
  stampDutyAmount               droit de timbre acquitte
  registrationFeeAmount         droits d enregistrement
  totalDutyAmount
  paidBy             COMPANY | CLIENT
  rebilled, rebilledDocumentCode
  receiptDocumentCode           quittance ou recu de l administration
  registeredDocumentCode        exemplaire enregistre et timbre, numerise
  expiresAt                     si l enregistrement a une duree de validite
  rejectionReason
```

Séparer l'enregistrement du contrat est délibéré : c'est une **démarche externe**, avec son propre
délai, son propre coût et sa propre possibilité d'échec. La mêler au contrat rendrait impossible de
suivre ce qui est en attente auprès de l'administration.

### Cycle de vie, avec ses points de blocage

```text
DRAFT
  -> PENDING_DOCUMENTS      pieces du representant legal manquantes
  -> PENDING_SIGNATURE      contrat genere, en attente de signature
  -> PENDING_REGISTRATION   contrat signe, en attente d enregistrement et de timbre
  -> ACTIVE                 enregistrement obtenu
```

Trois verrous, chacun avec un message explicite :

1. **Pas d'activation sans pièces d'identité vérifiées** du représentant légal, via le module KYC
   existant.
2. **Pas d'activation sans enregistrement obtenu.** Le statut `PENDING_REGISTRATION` doit être
   visible dans un tableau de suivi : c'est là que les dossiers s'enlisent.
3. **Pas d'attestation fiscale sans engagement annuel.** Le système refuse la génération, avec un
   message qui explique la règle plutôt qu'une erreur technique.

### Coût de l'enregistrement

Les droits de timbre et d'enregistrement sont un débours. Trois questions à trancher, que le modèle
laisse ouvertes par construction avec `paidBy` et `rebilled` :

- l'entreprise les avance-t-elle puis les refacture, ou le client les règle-t-il directement ?
- sont-ils inclus dans le tarif annoncé, ou facturés en sus ?
- que se passe-t-il si l'enregistrement échoue après paiement ?

### Autres obligations à tenir

1. **Adresse unique et suivie.** Deux sociétés peuvent partager une adresse, mais le complément
   distinctif doit permettre de les séparer. Un registre des adresses attribuées est nécessaire, avec
   la mention de celles qui portent une qualité fiscale.
2. **Registre des domiciliés**, exportable, avec entrées et sorties datées, et pour chacun la
   référence d'enregistrement du contrat.
3. **Renouvellement de l'attestation** avant échéance, avec relance automatique.
4. **Conservation des pièces**, le module document sachant déjà le faire.
5. **Notification de fin** au domicilié et, le cas échéant, à l'administration, puisque le contrat
   qu'elle a enregistré cesse de produire effet.

Restent à confirmer avec votre conseil, parce qu'ils conditionnent des durées et des destinataires
que le modèle rend paramétrables plutôt que codés en dur : la durée de conservation des pièces, la
validité d'une attestation, et l'obligation ou non d'informer l'administration à la résiliation.

### Gestion du courrier

```text
MailItem
  itemNumber, domiciliationContract
  mailType        LETTER | REGISTERED_LETTER | PARCEL | ADMINISTRATIVE | LEGAL_NOTICE | OTHER
  senderName, senderReference
  receivedAt, receivedBy
  weightGrams, dimensions
  status          RECEIVED, NOTIFIED, SCANNED, COLLECTED, FORWARDED, RETURNED, DESTROYED
  scanDocumentCode
  notifiedAt, notificationChannel
  collectedAt, collectedBy, collectorIdDocument, collectorSignatureUrl
  forwardedAt, forwardingTrackingNumber, forwardingCost
  storageDeadline                 au-dela, relance puis destruction ou retour
  billable, billedAmount
```

Le courrier recommandé et les actes administratifs demandent une rigueur particulière : accusé de
réception, notification immédiate, et remise contre signature. C'est exactement la même mécanique de
preuve que le bon de sortie de matériel décrit dans le document BTP, et elle peut s'en inspirer.

Facturation : la réception simple est généralement incluse, la réexpédition, la numérisation et le
stockage prolongé sont facturables à l'acte. Cela relie la domiciliation au module de facturation par
consommation, `usage` et `overage`, qui existent déjà.

## 5.3 Autres services à modéliser

| Service | Mécanique propre | Réutilise |
|---|---|---|
| **Permanence téléphonique** | Ligne attribuée, script d'accueil, journal des appels, transfert de messages | `usage` pour le décompte |
| **Casier et stockage** | Ressource physique attribuée, clé ou code, état des lieux, durée | `ressource`, et le registre de clés du document BTP |
| **Salle de réunion** | Quota d'heures, réservation, dépassement facturé | `booking`, `entitlement`, `overage` |
| **Assistance administrative** | Prestation à l'acte ou forfait d'heures, suivi de consommation | `usage`, `task` |
| **Assistance comptable ou juridique** | Forfait d'heures, prestataire, confidentialité des pièces | `document`, `usage` |
| **Support informatique** | Tickets, niveau de service, temps de réponse | `support` |
| **Place de parking** | Ressource attribuée, plaque, badge | `ressource` |
| **Espace événementiel** | Devis, caution, état des lieux, personnel | `booking`, `billing` |

Le point commun : chacun consomme un droit, occupe une ressource, ou produit un acte facturable. Les
trois mécaniques existent déjà. Ce qui manque est la couche de service qui les relie à un abonnement.

## 5.4 Abonnements dérivés : un prix et des avantages propres, sans multiplier les plans

### Le problème

Aujourd'hui un `Subscription` pointe vers une `PlanVersion` partagée par tous ceux qui ont souscrit ce
plan. Pour accorder à un client un prix différent ou des avantages différents, il n'y a qu'une
issue : créer un plan dédié. Multiplié par chaque négociation, cela produit un catalogue illisible où
mille plans existent pour mille clients, et où plus personne ne sait quel est le tarif de référence.

### La solution : dériver la version de plan, pas le plan

Plutôt qu'une table d'exceptions posée à côté du modèle, **on réutilise la mécanique de version de
plan qui existe déjà**, en créant une version privée rattachée à un seul abonnement.

```text
PlanVersion, colonnes ajoutees
  scope              CATALOGUE | SUBSCRIPTION
  ownerSubscriptionId    renseigne seulement si scope vaut SUBSCRIPTION
  derivedFromVersionId   version de catalogue dont celle-ci descend
```

L'intérêt de ce choix est qu'il ne demande **aucune modification des moteurs existants**. La
facturation, les droits, le calcul de période, tout lit déjà une `PlanVersion` : une version privée
se comporte exactement comme une version publique. Ce qui change tient en une ligne : les versions de
portée `SUBSCRIPTION` sont exclues des listes de catalogue.

```text
PlanDerivation
  derivationCode
  subscription
  sourcePlanVersion, derivedPlanVersion
  status          DRAFT, PENDING_APPROVAL, ACTIVE, SUPERSEDED, EXPIRED, REJECTED
  reason          NEGOTIATION | GOODWILL | PARTNERSHIP | PILOT | GRANDFATHERING |
                  LOYALTY | CORRECTION
  reasonDetails
  effectiveFrom, effectiveTo
  renewalBehaviour   KEEP | REVERT_TO_CATALOGUE | REVERT_AFTER_PERIODS
  revertAfterPeriods
  promotionsAllowed  faux par defaut sur un prix negocie
  requestedBy, approvedBy, approvedAt, rejectionReason
  supersedesDerivationId
```

```text
PlanDerivationDelta
  derivation
  deltaType    PRICE | ENTITLEMENT | BILLING_CYCLE | COMMITMENT | NOTICE_PERIOD |
               TRIAL | SETUP_FEE | PAYMENT_TERMS | ADDON_INCLUDED
  targetCode
  catalogueValue, derivedValue
  impactAmount        ecart chiffre sur une periode, positif si concession
```

Le delta est ce qui rend l'ensemble pilotable. Sans lui, on sait qu'un client a un traitement
particulier, mais personne ne peut dire **ce qui a été concédé ni combien cela coûte**. Avec lui, on
produit la liste des concessions par commercial, par période, par montant.

### Les règles qui évitent la dérive

1. **La dérivation garde le lien vers sa source.** Quand le plan de catalogue évolue, on peut afficher
   l'écart et décider. Sans ce lien, les abonnements dérivés deviennent des orphelins que plus
   personne n'ose toucher.
2. **Elle est versionnée, jamais modifiée.** Un prix négocié en janvier puis renégocié en juin laisse
   deux traces chaînées par `supersedesDerivationId`. La question « quel prix appliquions-nous en
   mars » doit toujours avoir une réponse.
3. **Elle est datée et peut expirer.** Un tarif de lancement sur six mois est une dérivation avec une
   `effectiveTo`, pas une promesse orale que personne ne retrouve.
4. **Son comportement au renouvellement est explicite.** `renewalBehaviour` évite le cas classique du
   geste commercial ponctuel qui devient un tarif à vie parce que personne n'a pensé à l'échéance.
5. **Elle est approuvée au-delà d'un seuil.** En réutilisant le modèle de règles d'approbation déjà
   en place pour les achats et les ajustements de stock : un rabais au-delà d'un pourcentage, ou un
   prix sous un plancher, exige un visa.
6. **Un prix plancher est opposable.** `PlanVersion` de catalogue peut porter un `floorPrice` en
   dessous duquel aucune dérivation n'est acceptée, même approuvée.

### Ce que cela permet au-delà de la négociation

- **Protection tarifaire lors d'une hausse.** Le catalogue augmente, les abonnés en cours conservent
  leur tarif : une dérivation de motif `GRANDFATHERING` est générée en masse, datée, et l'on sait
  exactement combien la protection coûte.
- **Pilotes et bêtas.** Un groupe d'abonnés teste un plan enrichi sans que ce plan existe au
  catalogue.
- **Partenariats.** Les membres d'une organisation partenaire reçoivent une dérivation commune,
  créée en lot depuis un modèle.
- **Correction d'erreur.** Une souscription au mauvais tarif se corrige par une dérivation datée
  plutôt que par une modification silencieuse de l'historique.

### Dérivation contre grille tarifaire, quand utiliser quoi

| Situation | Outil |
|---|---|
| Un tarif applicable à une population, stable dans le temps | Grille tarifaire, partie 2 |
| Un traitement propre à un abonné, négocié, daté | Abonnement dérivé |
| Une réduction temporaire et promotionnelle | Promotion ou coupon |

La confusion entre ces trois outils est le principal risque de ce chantier. La règle de partage :
**si cela vaut pour plusieurs personnes et se renouvelle, c'est une grille ; si cela vaut pour une
personne et résulte d'une négociation, c'est une dérivation ; si cela a une fin annoncée et un but
commercial, c'est une promotion.**

### Création en lot

```text
POST /subscriptions/derivations/bulk
```

Appliquer une même dérivation à une liste d'abonnements, avec simulation préalable du coût total
avant exécution. C'est ce qui rend la protection tarifaire et les partenariats praticables.

## 5.5 Améliorations du module abonnement

### Engagement et résiliation

```text
SubscriptionCommitment
  subscription
  commitmentMonths, commitmentStart, commitmentEnd
  earlyTerminationFee, earlyTerminationFormula
  autoRenewCommitment

SubscriptionTermination
  subscription, requestedAt, requestedBy
  noticePeriodDays, effectiveDate
  reason, reasonCategory
  earlyTermination, feeAmount, feeWaived, feeWaivedBy
  status        REQUESTED, ACCEPTED, RETRACTED, COMPLETED
  exitChecklist   restitution de badge, courrier en attente, solde a regler
```

La liste de sortie est ce qui évite les fins d'abonnement bâclées : un domicilié qui part en laissant
du courrier non retiré et un badge non rendu.

### Cycle de vie enrichi

Statuts à ajouter à `SubscriptionStatus` : `PENDING_DOCUMENTS` avant activation quand des pièces
manquent, `GRACE_PERIOD` après échec de paiement mais avant suspension, `PENDING_TERMINATION`
lorsqu'un préavis court.

### Autres fonctionnalités

1. **Devis avant souscription**, converti en abonnement à l'acceptation.
2. **Signature électronique du contrat**, le module `contract` existe et gère déjà la génération et
   la signature.
3. **Prélèvement automatique** depuis le portefeuille, avec accord préalable.
4. **Gel temporaire** sans résiliation : `pausedAt` et `pauseUntil` existent, la politique de
   facturation pendant le gel reste à écrire.
5. **Changement de plan en cours de période** avec calcul du prorata. Le sous-module `change` existe,
   sa politique de prorata mérite d'être explicitée et testée.
6. **Renouvellement anticipé** avec incitation tarifaire, ce qui relie à la partie 2.
7. **Facturation groupée** pour une entreprise ayant plusieurs abonnements, une facture, un
   échéancier.
8. **Portail client en libre-service** : souscrire, changer, suspendre, résilier, payer, sans
   intervention humaine. Le module `portal` existe et n'expose aujourd'hui que de la consultation.
9. **Relances d'impayé** : `PaymentDunningAttempt` existe, la politique de relance et ses paliers
   restent à paramétrer.
10. **Indicateurs** : taux d'attrition, revenu récurrent mensuel, valeur vie client, taux
    d'occupation, revenu par poste. Le sous-module `metrics` existe et mérite d'être complété.

### Réglage fin, propositions complémentaires

Ces fonctionnalités vont dans le même sens que l'abonnement dérivé : permettre le cas particulier
sans casser le modèle général.

**Sur la structure de l'offre**

1. **Lots.** Un plan plus des services à un prix de paquet, inférieur à la somme des parties.
   `SubscriptionService` le permet, il manque la notion de prix de lot et la règle de ce qui arrive
   quand on retire un composant.
2. **Dépendances et exclusions entre options.** Une option qui en exige une autre, deux options
   incompatibles. Aujourd'hui rien n'empêche une combinaison absurde.
3. **Paliers de volume par abonnement.** Le prix au poste décroît au-delà d'un seuil, calculé à
   chaque facturation selon le nombre de sièges réellement occupés. Le sous-module `seat` fournit le
   compteur, il manque la grille de paliers.
4. **Modèles d'abonnement pour la vente.** Une configuration type, prête à proposer, qui pré-remplit
   plan, options, engagement et conditions. Cela réduit les dérivations créées par facilité.

**Sur le temps et les échéances**

5. **Changements planifiés.** Programmer aujourd'hui un changement de plan qui prendra effet à la
   prochaine échéance, plutôt que de devoir y penser le jour dit.
6. **Alignement des dates.** Quand une entreprise a plusieurs abonnements, les faire converger vers
   une même date d'échéance, avec une période de raccordement facturée au prorata. Sans cela, une
   société reçoit cinq factures à cinq dates différentes.
7. **Gel encadré.** Le gel existe, ses limites non : nombre de gels par an, durée maximale, préavis,
   et ce qui est facturé pendant. Un gel sans bornes est un moyen de ne plus payer sans résilier.
8. **Politique de prorata explicite et paramétrable** : au jour, au mois entamé, ou aucun. Le même
   choix doit valoir pour l'entrée, la sortie et le changement de plan, sinon les trois divergent.

**Sur l'argent**

9. **Régularisation d'engagement de volume.** Un client s'engage sur un volume annuel et paie au fil
   de l'eau ; en fin de période, l'écart entre l'engagement et le consommé est facturé. Le
   sous-module `usage` fournit la mesure.
10. **Avoirs de service.** Un manquement donne droit à un avoir automatique, calculé selon une règle
    plutôt que négocié au cas par cas. C'est ce qui évite qu'un incident se règle par une dérivation
    permanente.
11. **Conditions de paiement propres au client** : délai, mode, jour de prélèvement. Aujourd'hui ces
    conditions sont implicites.
12. **Politique de relance par segment.** Un grand compte et un particulier ne se relancent pas au
    même rythme ni sur le même ton.
13. **Produits constatés d'avance.** Un abonnement annuel encaissé en janvier n'est pas un produit de
    janvier. La ventilation mensuelle du produit rejoint la logique d'avance client décrite en
    partie 3, et le module billing a déjà les périodes comptables pour la porter.

**Sur la relation**

14. **Hiérarchie d'abonnements.** Un abonnement parent porté par une entreprise, des abonnements
    enfants portés par ses collaborateurs, une facturation consolidée et des droits partagés. Le
    modèle de sièges couvre une partie du besoin, pas la facturation consolidée.
15. **Reconquête.** Un abonnement résilié depuis peu, une offre de retour, un suivi du taux de
    reconquête. Cela relie directement au ciblage nominatif de la partie 2.
16. **Historique lisible par le client.** La frise existe côté interne, le client devrait voir la
    sienne : souscriptions, changements, gels, factures, avantages accordés.
17. **Approbation des conditions non standard.** Toute dérivation, tout avoir, toute remise manuelle
    au-delà d'un seuil passe par un visa, avec la même mécanique que les autres modules.

---

# Partie 6 · Implications techniques et feuille de route

## 6.1 Nouvelles entités, récapitulatif

| Domaine | Entités |
|---|---|
| Tarification | `PromotionCondition`, `PromotionReward`, `PromotionAudience`, `PromotionBeneficiary`, `Coupon`, `CouponBatch`, `CouponReservation`, `PriceList`, `PriceListEntry`, `ReferralProgram`, `ReferralLink`, `Referral`, `AppliedDiscount` |
| Portefeuille | `WalletCredential`, `WalletTransactionConfirmation`, `WalletTransfer`, `WalletMerchantPayment`, `WalletLimitPolicy`, `WalletLimitUsage`, `WalletRiskFlag`, `WalletStatement`, `WalletTreasuryReconciliation` |
| Pass | `PassUsage`, `PassValidityRule`, `PassTransfer`, `PassBeneficiary`, `PassCredential` |
| Abonnement | `ServiceDefinition`, `SubscriptionService`, `DomiciliationContract`, `DomiciliationRegistration`, `MailItem`, `SubscriptionCommitment`, `SubscriptionTermination`, `PlanDerivation`, `PlanDerivationDelta` |

## 6.2 Extensions d'entités existantes

```text
Promotion              + promotionType, stackable, exclusive, priority,
                         maxRedemptionsPerSubscriber, maxDiscountAmount,
                         budgetAmount, consumedBudgetAmount, totalDiscountGranted,
                         counterpartAccount, approvedBy, approvedAt
BillingDocumentDiscount + sourceType, sourceCode
WalletLedgerEntry      + previousHash, currentHash
WalletAccount          + limitPolicyCode, lastActivityAt, dormantSince
PlanVersion            + scope, ownerSubscriptionId, derivedFromVersionId, floorPrice
Pass                   + rien, le modele suffit
Subscription           + commitmentId, terminationId, activeDerivationId
SubscriptionStatus     + PENDING_DOCUMENTS, GRACE_PERIOD, PENDING_TERMINATION
PromotionStatus        + SCHEDULED, EXHAUSTED
WalletEntryType        + TRANSFER_IN, TRANSFER_OUT, MERCHANT_PAYMENT, FEE,
                         TOPUP_MOBILE_MONEY, WITHDRAWAL
```

## 6.3 Règles transverses, héritées du module inventaire

Les invariants posés pendant les lots inventaire s'appliquent tels quels :

- **deux dossiers Flyway**, un fichier dans chacun, numérotations distinctes ;
- aucun identifiant fonctionnel à la main, passage par `SequenceGeneratorFacade` ;
- tout endpoint mutant porte `@Idempotent`, d'autant plus critique ici qu'un transfert rejoué
  débiterait deux fois ;
- **toute énumération nouvelle est servie au front** avec son libellé, sur le modèle de
  `/inventory/reference/enums` ;
- **toute valeur calculable par le serveur est proposée ou pré-remplie** ;
- montants en entiers XAF ;
- un lot sans test n'est pas terminé ;
- verrouillage explicite sur tout compteur partagé : budget de promotion, coupon à usage unique,
  plafond de portefeuille.

## 6.4 Sécurité

| Point | Exigence |
|---|---|
| Code PIN | Haché, jamais journalisé, jamais restitué, verrouillage progressif |
| Confirmation | Empreinte de l'opération liée à la confirmation, expiration courte |
| Transferts | Nom du destinataire affiché avant confirmation |
| Actions administrateur | Motif obligatoire, second visa au-delà d'un seuil |
| Journal | Immuable en base, chaîné, réconcilié périodiquement |
| Permissions | Ressources dédiées `WALLET`, `PROMOTION`, `PASS`, `SUBSCRIPTION_SERVICE` avec actions fines |

## 6.5 Feuille de route proposée

L'ordre suit les dépendances et la valeur.

### Lot A · Moteur de tarification (18 à 24 j)

Conditions, récompenses, ciblage nominatif, coupons avec réservation, grilles tarifaires, moteur
d'évaluation, simulation, branchement sur la facturation et sur les abonnements. **C'est le lot qui
débloque le plus de valeur immédiate**, parce qu'il rend enfin opérant un module qui existe sans
effet.

### Lot A bis · Prix promotionnels côté client (10 à 14 j)

Tarification en lot du catalogue, affichage des prix effectifs sur les plans, pass, services,
ressources et articles, mise en cache et invalidation, parcours client de saisie et d'application de
coupon avant paiement, liste des promotions disponibles. Dépend du lot A. **C'est ce lot qui rend les
promotions visibles, donc vendeuses** : sans lui, le lot A reste un moteur que personne ne voit.

### Lot B · Portefeuille, socle de confiance (18 à 24 j)

Code PIN, confirmation d'opération, immuabilité et chaînage du journal, réconciliation, limites et
plafonds liés au KYC. Prérequis de tout le reste du portefeuille : sans secret ni plafond, ouvrir le
portefeuille à l'initiative du client serait imprudent.

### Lot C · Portefeuille, usages (15 à 20 j)

Transfert entre abonnés, paiement vers l'entreprise, rechargement mobile money en autonomie, relevé
PDF, notifications. Dépend du lot B.

### Lot D · Centre de contrôle portefeuille (10 à 14 j)

Tableau de bord, actions administratives tracées, signalements et leur revue, export comptable,
rapprochement avec la trésorerie.

### Lot E · Opérationnalisation des pass (15 à 20 j)

Service de validation unique, usage et décompte, règles de validité, bénéficiaires, transfert,
matérialisation QR, alertes. Dépend du lot A pour la vente de pass en libre-service.

### Lot F · Catalogue de services et domiciliation (20 à 26 j)

Définitions de service, services souscrits, contrat de domiciliation, registre des adresses,
attestation, gestion du courrier avec preuve de remise.

### Lot G · Abonnements dérivés (12 à 16 j)

Dérivation de version de plan scopée à un abonnement, delta chiffré, approbation au-delà d'un seuil,
prix plancher, comportement au renouvellement, création en lot, et protection tarifaire lors d'une
hausse de catalogue. Dépend du lot A pour l'ordre d'évaluation. **C'est le lot qui évite le
catalogue à mille plans**, et plus il arrive tard, plus le désordre à rattraper sera grand.

### Lot H · Cycle de vie des abonnements (12 à 16 j)

Engagement, résiliation avec préavis et liste de sortie, nouveaux statuts, devis, prélèvement
automatique, politique de gel encadrée, prorata explicité, changements planifiés, alignement des
dates.

### Lot I · Libre-service et pilotage (12 à 16 j)

Portail client en écriture, indicateurs d'attrition et de revenu récurrent, relances d'impayé
paramétrées, historique lisible par le client.

**Volume total** : de l'ordre de 137 à 185 jours-développeur.

### Un ordre resserré, si l'on veut de la valeur vite

Les lots A, A bis et G forment un ensemble cohérent qui se tient seul et produit un effet visible en
huit à dix semaines : les promotions s'appliquent enfin, le client les voit et les utilise, et la
force de vente peut négocier sans polluer le catalogue. Les lots portefeuille peuvent suivre sans
dépendance bloquante.

## 6.6 Décisions à trancher avant de démarrer

### Tranchées

1. **Nature du portefeuille. → Avance client, pas de monnaie électronique**, gérée avec le niveau
   d'exigence d'un opérateur de mobile money. Conséquences fermes : pas de retrait en espèces, solde
   utilisable uniquement pour payer l'entreprise, transferts entre abonnés encadrés et plafonnés,
   remboursement uniquement vers la source d'origine. Détail en 3.1.
2. **Domiciliation : rythmes de facturation. → Mensuel, trimestriel ou annuel**, les trois valeurs
   existant déjà dans `BillingCycle`.
3. **Domiciliation : qualité fiscale de l'adresse. → Réservée à l'engagement annuel.**
   `fiscalAddressEligible` est déduit de l'engagement, jamais saisi. Le passage à un rythme plus
   court révoque la qualité fiscale et l'attestation correspondante.
4. **Domiciliation : contrat obligatoire, enregistré et timbré** auprès de l'administration publique.
   `PENDING_REGISTRATION` devient une étape bloquante du cycle de vie, et l'attestation n'est
   délivrable qu'une fois l'enregistrement obtenu.

### Encore ouvertes

5. **Domiciliation, points résiduels.** Durée de conservation des pièces, durée de validité d'une
   attestation, obligation ou non d'informer l'administration à la résiliation. Le modèle les rend
   paramétrables, mais les valeurs doivent venir de votre conseil.
6. **Coût de l'enregistrement.** Avancé par l'entreprise puis refacturé, ou réglé directement par le
   client ? Inclus dans le tarif annoncé ou facturé en sus ? Que devient-il si l'enregistrement
   échoue après paiement ?
7. **Transfert entre abonnés : avec ou sans acceptation du destinataire ?**
8. **Frais de transfert.** La qualification d'avance client impose qu'ils restent forfaitaires et
   couvrent un coût réel, jamais proportionnels et lucratifs. Reste à décider s'il y en a.
9. **Plafonds de transfert.** Montant par opération, nombre par mois, volume cumulé. Ce sont eux qui
   maintiennent le transfert du bon côté de la frontière décrite en 3.1 : les fixer bas au départ,
   quitte à les relever ensuite.
10. **Cumul des promotions :** par défaut cumulables ou exclusives ? Le choix inverse celui de la
    règle par défaut sur toutes les campagnes existantes.
11. **Prorata au changement de plan :** au jour, au mois entamé, ou sans prorata ?
12. **Pénalité de résiliation anticipée :** formule et plafond. Le cas de la domiciliation annuelle
    résiliée en cours d'année est particulier, puisque le client perd la qualité fiscale de son
    adresse et que le contrat enregistré doit cesser de produire effet.
13. **Niveau KYC exigé** pour ouvrir un portefeuille, pour transférer, pour dépasser un seuil.
14. **Seuil d'approbation d'une dérivation.** À partir de quel pourcentage de rabais, ou de quel écart
    en valeur, un visa devient-il obligatoire ? Et qui vise.
15. **Prix plancher.** Existe-t-il, par plan, un tarif sous lequel on ne descend jamais, même
    approuvé ?
16. **Comportement par défaut au renouvellement d'une dérivation.** Conserver ou revenir au
    catalogue ? Le défaut retenu déterminera le sort de la majorité des gestes commerciaux, puisque
    personne ne le renseignera explicitement.
17. **Un prix négocié accepte-t-il une promotion par-dessus ?** `promotionsAllowed` est proposé à
    faux par défaut, à confirmer.
18. **Durée de réservation d'un coupon** pendant le parcours de paiement.
19. **Affichage des offres non éligibles** au client, avec leur motif, ou masquage complet ?

## 6.7 Risques

| Risque | Impact | Réduction |
|---|---|---|
| Moteur de tarification appliqué en double sur une facture rééditée | Remise comptée deux fois | Idempotence par document, `AppliedDiscount` unique par couple document et règle |
| Transfert rejoué après coupure réseau | Double débit | Clé d'idempotence obligatoire sur les opérations de portefeuille, déjà supportée par le socle |
| Course sur un coupon à usage unique | Code consommé deux fois | Réservation temporaire pendant le parcours, puis capture ou libération, comme pour le stock et le portefeuille |
| Prix promotionnel affiché puis non honoré | Litige client, perte de confiance | Prix figé dans le panier avec son échéance, revérification à la validation, message explicite si la promotion a expiré |
| Promotion nominative visible d'un autre client | Fuite commerciale, demandes d'alignement | Filtrage par bénéficiaire dès la requête, jamais dans la réponse |
| Dérivations créées par facilité plutôt que par nécessité | Retour au catalogue à mille plans, sous une autre forme | Approbation au-delà d'un seuil, prix plancher opposable, modèles d'abonnement pour couvrir les cas courants, revue périodique des dérivations actives |
| Geste commercial ponctuel devenu tarif à vie | Érosion silencieuse du revenu | `renewalBehaviour` obligatoire, `effectiveTo` par défaut renseignée, alerte avant échéance |
| Confusion entre grille, dérivation et promotion | Trois outils utilisés au hasard, tarification inexplicable | Règle de partage écrite en 5.4, et `priceSource` restitué dans chaque prix effectif |
| Course sur un budget de campagne | Dépassement d'enveloppe | Compteur verrouillé, mis à jour dans la transaction |
| Code PIN faible ou fuité | Compromission de compte | Refus des codes triviaux, verrouillage progressif, notification de chaque opération |
| Solde de portefeuille sans contrepartie en trésorerie | Incapacité à honorer les prestations déjà payées | `WalletTreasuryReconciliation` obligatoire dès le lot D, avec alerte sur le ratio de couverture |
| Dérive du portefeuille vers un service de paiement | Requalification juridique, obligations non prévues | Les quatre exclusions de 3.8 tenues fermement, plafonds de transfert bas, alerte sur les portefeuilles de transit |
| Attestation fiscale délivrée sur un engagement non annuel | Document sans valeur, responsabilité engagée | `fiscalAddressEligible` déduit de l'engagement, jamais saisi, et refus de génération explicite |
| Domiciliation activée sans contrat enregistré | Contrat inopposable, activité irrégulière | `PENDING_REGISTRATION` bloquant, tableau de suivi des dossiers en attente |
| Passage d'un client de l'annuel au mensuel | Perte silencieuse de la qualité fiscale de son adresse | Révocation automatique de l'attestation, notification du client, trace datée |
| Courrier recommandé perdu ou remis à la mauvaise personne | Responsabilité engagée | Remise contre signature et pièce d'identité, notification immédiate |
| Modèle de service trop rigide | Chaque nouveau service demande du développement | `ServiceDefinition` paramétrable, métadonnées par service plutôt qu'une entité par métier |
