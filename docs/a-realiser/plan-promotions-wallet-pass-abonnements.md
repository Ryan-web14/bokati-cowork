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

- Une **adresse commerciale attribuée**, qui doit être unique, suivie, et mentionnée sur des
  documents officiels.
- Une **attestation de domiciliation** à produire, avec sa validité et son renouvellement.
- Du **courrier** qui arrive, qu'il faut enregistrer, notifier, numériser, faire suivre ou remettre.
- Des **obligations réglementaires** : contrat de domiciliation écrit, vérification d'identité du
  domicilié, tenue d'un registre, conservation des pièces.

Rien de tout cela n'a de place dans `Subscription` aujourd'hui.

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

  targetScope     WHOLE_ORDER | LINE | PLAN | ADDON | PASS | ENTITLEMENT
```

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

## 2.3 Le moteur de tarification

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
3. **Promotions automatiques**, par priorité croissante.
4. **Coupons saisis**, par priorité croissante.
5. **Plafonnement** : application de `maxDiscountAmount` et du budget restant.
6. **Arrondi** : à l'entier XAF, une seule fois, à la fin.

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

## 2.4 Application et traçabilité

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

## 2.5 Garde-fous

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

## 2.6 Endpoints

```text
POST   /promotions                             PUT    /promotions/{code}
PATCH  /promotions/{code}/activate | /pause | /archive
POST   /promotions/{code}/conditions           POST   /promotions/{code}/rewards
GET    /promotions                             GET    /promotions/{code}
GET    /promotions/{code}/performance          cout, utilisations, taux de conversion

POST   /coupons/batches                        generation de lot
GET    /coupons/batches/{batchCode}            suivi du lot
POST   /coupons                                coupon unitaire ou nominatif
PATCH  /coupons/{code}/revoke
GET    /coupons/{code}/validate                verification sans consommation

POST   /price-lists                            POST /price-lists/{code}/entries
GET    /price-lists                            GET  /price-lists/resolve
POST   /referral-programs                      GET  /referrals
GET    /client/referral-link                   lien de parrainage de l'abonne

POST   /pricing/simulate                       simulation sans effet de bord
GET    /pricing/applicable                     ce a quoi un abonne a droit aujourd'hui
```

---

# Partie 3 · Portefeuille électronique

## 3.1 Ce qu'il faut entendre par fiable et conforme

Trois exigences, de nature différente.

**Intégrité** : le solde affiché est toujours égal à la somme des écritures, et personne ne peut
modifier une écriture passée. Le socle actuel y est presque : il manque l'immuabilité en base et un
contrôle de réconciliation.

**Non-répudiation** : le titulaire ne peut pas nier avoir ordonné une opération. Cela suppose un
secret que lui seul détient, et une trace de son usage. C'est ce que le code PIN apporte.

**Conformité** : la capacité à démontrer, devant un tiers, qui détient quoi, d'où vient l'argent, et
que les plafonds ont été respectés. Le cadre réglementaire précis applicable à votre activité reste
à confirmer avec votre conseil : ce document décrit les mécanismes usuels sans préjuger de ce qui
vous est juridiquement imposé.

## 3.2 Code PIN et confirmation des opérations

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

## 3.3 Transfert entre abonnés

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

## 3.4 Initiation de paiement vers l'entreprise

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

## 3.5 Limites, plafonds et niveaux

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

## 3.6 Intégrité et conformité

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

### Compte de contrepartie

Un portefeuille crédité est une dette de l'entreprise envers son titulaire. La somme des soldes doit
correspondre à un compte de contrepartie au passif, et idéalement à des fonds réellement disponibles.
Un état de rapprochement entre le total des portefeuilles et la trésorerie est le contrôle qui
manque le plus à une exploitation sérieuse.

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

## 3.7 Autres fonctionnalités à prévoir

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
7. **Remboursement vers la source**, avec traçabilité du sens de retour.
8. **Export comptable** des mouvements de portefeuille, sur le modèle de l'export des écritures de
   stock.

## 3.8 Centre de contrôle administrateur

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

```text
DomiciliationContract
  contractNumber, subscription, subscriptionService
  businessEntity                 la societe domiciliee
  legalName, legalForm, registrationNumber, taxNumber
  assignedAddressId              adresse commerciale attribuee
  suiteNumber                    complement distinctif, boite ou bureau
  status        DRAFT, PENDING_DOCUMENTS, ACTIVE, SUSPENDED, TERMINATED, EXPIRED
  startDate, endDate, noticePeriodDays
  legalRepresentativeCode
  contractDocumentCode           contrat signe, via features/document
  certificateDocumentCode        attestation de domiciliation
  certificateValidUntil
  mailForwardingMode  HOLD | FORWARD | SCAN_AND_FORWARD | SCAN_ONLY
  forwardingAddressId, forwardingFrequency
  terminatedAt, terminationReason
```

### Obligations à tenir

1. **Adresse unique et suivie.** Deux sociétés peuvent partager une adresse, mais le complément
   distinctif doit permettre de les séparer. Un registre des adresses attribuées est nécessaire.
2. **Vérification d'identité** du représentant légal, via le module KYC existant, avant activation.
3. **Attestation de domiciliation** générée, datée, avec sa validité et son renouvellement, produite
   par la chaîne PDF du module billing.
4. **Registre des domiciliés**, exportable, avec entrées et sorties datées.
5. **Conservation des pièces** pendant une durée à définir, le module document sachant déjà le faire.
6. **Notification de fin** au domicilié et, le cas échéant, aux autorités concernées.

Le cadre réglementaire exact applicable au Congo reste à confirmer avec votre conseil juridique. La
mécanique ci-dessus couvre les obligations usuelles ; les durées et les destinataires des
notifications devront être paramétrables plutôt que codés en dur.

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

## 5.4 Améliorations du module abonnement

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

---

# Partie 6 · Implications techniques et feuille de route

## 6.1 Nouvelles entités, récapitulatif

| Domaine | Entités |
|---|---|
| Tarification | `PromotionCondition`, `PromotionReward`, `Coupon`, `CouponBatch`, `PriceList`, `PriceListEntry`, `ReferralProgram`, `ReferralLink`, `Referral`, `AppliedDiscount` |
| Portefeuille | `WalletCredential`, `WalletTransactionConfirmation`, `WalletTransfer`, `WalletMerchantPayment`, `WalletLimitPolicy`, `WalletLimitUsage`, `WalletRiskFlag`, `WalletStatement` |
| Pass | `PassUsage`, `PassValidityRule`, `PassTransfer`, `PassBeneficiary`, `PassCredential` |
| Abonnement | `ServiceDefinition`, `SubscriptionService`, `DomiciliationContract`, `MailItem`, `SubscriptionCommitment`, `SubscriptionTermination` |

## 6.2 Extensions d'entités existantes

```text
Promotion              + promotionType, stackable, exclusive, priority,
                         maxRedemptionsPerSubscriber, maxDiscountAmount,
                         budgetAmount, consumedBudgetAmount, totalDiscountGranted,
                         counterpartAccount, approvedBy, approvedAt
BillingDocumentDiscount + sourceType, sourceCode
WalletLedgerEntry      + previousHash, currentHash
WalletAccount          + limitPolicyCode, lastActivityAt, dormantSince
Pass                   + rien, le modele suffit
Subscription           + commitmentId, terminationId
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

### Lot A · Moteur de tarification (15 à 20 j)

Conditions, récompenses, coupons, grilles tarifaires, moteur d'évaluation, simulation, branchement
sur la facturation et sur les abonnements. **C'est le lot qui débloque le plus de valeur immédiate**,
parce qu'il rend enfin opérant un module qui existe sans effet.

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

### Lot G · Cycle de vie des abonnements (12 à 16 j)

Engagement, résiliation avec préavis et liste de sortie, nouveaux statuts, devis, prélèvement
automatique, politique de gel, prorata explicité.

### Lot H · Libre-service et pilotage (12 à 16 j)

Portail client en écriture, indicateurs d'attrition et de revenu récurrent, relances d'impayé
paramétrées.

**Volume total** : de l'ordre de 115 à 155 jours-développeur.

## 6.6 Décisions à trancher avant de démarrer

1. **Cadre réglementaire du portefeuille.** Émettez-vous de la monnaie électronique au sens
   réglementaire, ou tenez-vous un simple compte d'avances client ? La réponse change les obligations
   et doit venir de votre conseil, pas d'une hypothèse technique.
2. **Obligations de la domiciliation au Congo.** Durée de conservation, registre, déclarations. À
   confirmer avant de figer le modèle.
3. **Transfert entre abonnés : avec ou sans acceptation du destinataire ?**
4. **Frais de transfert :** gratuits, forfaitaires, ou proportionnels ?
5. **Cumul des promotions :** par défaut cumulables ou exclusives ? Le choix inverse celui de la
   règle par défaut sur toutes les campagnes existantes.
6. **Prorata au changement de plan :** au jour, au mois entamé, ou sans prorata ?
7. **Pénalité de résiliation anticipée :** formule et plafond.
8. **Niveau KYC exigé** pour ouvrir un portefeuille, pour transférer, pour dépasser un seuil.

## 6.7 Risques

| Risque | Impact | Réduction |
|---|---|---|
| Moteur de tarification appliqué en double sur une facture rééditée | Remise comptée deux fois | Idempotence par document, `AppliedDiscount` unique par couple document et règle |
| Transfert rejoué après coupure réseau | Double débit | Clé d'idempotence obligatoire sur les opérations de portefeuille, déjà supportée par le socle |
| Course sur un coupon à usage unique | Code consommé deux fois | Verrouillage pessimiste du coupon pendant l'évaluation |
| Course sur un budget de campagne | Dépassement d'enveloppe | Compteur verrouillé, mis à jour dans la transaction |
| Code PIN faible ou fuité | Compromission de compte | Refus des codes triviaux, verrouillage progressif, notification de chaque opération |
| Solde de portefeuille sans contrepartie en trésorerie | Incapacité à honorer les retraits | État de rapprochement obligatoire, dès le lot D |
| Courrier recommandé perdu ou remis à la mauvaise personne | Responsabilité engagée | Remise contre signature et pièce d'identité, notification immédiate |
| Modèle de service trop rigide | Chaque nouveau service demande du développement | `ServiceDefinition` paramétrable, métadonnées par service plutôt qu'une entité par métier |
