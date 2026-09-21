# Promotions, portefeuille, pass, services et abonnements · Guide Frontend

Ce document couvre tout ce que les lots A à I du plan `docs/a-realiser/plan-promotions-wallet-pass-abonnements.md`
ont ajouté au backend, du point de vue de celui qui construit les écrans :

| Lot | Ce que ça apporte | Section |
|---|---|---|
| A, A bis | Moteur de tarification, coupons, parrainage, ciblage nominatif, prix affichés côté client | [1](#1-tarification-et-promotions) |
| B, C | Portefeuille client : code PIN, plafonds, transferts, rechargement, demandes de paiement, appareils, relevés | [2](#2-portefeuille--espace-client) |
| D, D bis | Centre de contrôle du portefeuille, conformité et surveillance (admin) | [3](#3-centre-de-contrôle-et-conformité-admin) |
| E0, E | Pass opérationnels : identifiants QR, validation, usage, transfert | [4](#4-pass--validation-usage-transfert) |
| F | Catalogue de services, domiciliation, courrier | [5](#5-services-et-domiciliation) |
| G | Abonnements dérivés (prix négocié sans plan de catalogue) | [6](#6-abonnements-dérivés) |
| H | Cycle de vie : engagement, résiliation, devis, prélèvement, gel, prorata, alignement | [7](#7-cycle-de-vie-des-abonnements-guichet) |
| I | Espace client en écriture, historique lisible, indicateurs, relances d'impayés | [8](#8-espace-client--libre-service-et-historique) et [9](#9-indicateurs-et-relances-dimpayés) |
| correctifs | Téléphones internationaux, activation admin, OTP | [10](#10-inscription-téléphones-et-activation) |

> Préfixe API : `/sni/api/v1` (toutes les routes ci-dessous sont relatives à ce préfixe)
> Auth : Bearer JWT. Les routes `/client/...` sont celles de l'espace client (rôle `MEMBER`) ; les autres sont réservées au personnel, avec le rôle indiqué.
> Pagination : `?page=0&size=20` · réponse `{ data: [...], pageable: { page, size, totalPages, totalElements, first, last, hasNext, hasPrevious, sort } }`.
> Montants : `BigDecimal` sérialisés en nombre, 4 décimales en base · afficher avec la devise fournie (`XAF` par défaut).
> Dates : `LocalDate` en `YYYY-MM-DD`, `Instant` en ISO 8601 UTC.
> Erreurs : `400` (validation, `BadRequest`), `404` (introuvable, ou pas à vous côté client), `409` (`Conflict` : règle métier violée, le message dit laquelle), `403` (rôle).

---

## Table des matières

1. [Tarification et promotions](#1-tarification-et-promotions)
2. [Portefeuille · espace client](#2-portefeuille--espace-client)
3. [Centre de contrôle et conformité (admin)](#3-centre-de-contrôle-et-conformité-admin)
4. [Pass · validation, usage, transfert](#4-pass--validation-usage-transfert)
5. [Services et domiciliation](#5-services-et-domiciliation)
6. [Abonnements dérivés](#6-abonnements-dérivés)
7. [Cycle de vie des abonnements (guichet)](#7-cycle-de-vie-des-abonnements-guichet)
8. [Espace client · libre-service et historique](#8-espace-client--libre-service-et-historique)
9. [Indicateurs et relances d'impayés](#9-indicateurs-et-relances-dimpayés)
10. [Inscription, téléphones et activation](#10-inscription-téléphones-et-activation)
11. [Annexe · énumérations](#11-annexe--énumérations)
12. [Annexe · rôles par route](#12-annexe--rôles-par-route)

---

## 1. Tarification et promotions

### 1.1 Le principe

Le moteur tarife une **liste de lignes**, pas seulement un panier. Chaque ligne désigne un objet
tarifable par un couple `scope` + `code` (`PLAN`, `PASS`, `SERVICE`, `RESOURCE`, `INVENTORY_ITEM`,
`ADDON`, `ENTITLEMENT`, `CATEGORY`, `LINE`, `WHOLE_ORDER`). Les sources de remise, dans l'ordre
d'évaluation fixé : grille tarifaire (`PRICE_LIST`), promotion automatique (`PROMOTION`), coupon
(`COUPON`), parrainage (`REFERRAL`), manuel (`MANUAL`). Le résultat dit ce qui a été appliqué, ce
qui a été refusé et pourquoi, et les récompenses non monétaires (jours d'essai, droits offerts,
crédit portefeuille).

**Le prix affiché engage** : ce que `/client/catalogue/prices` renvoie est ce que la souscription
appliquera, au même instant et dans le même contexte.

### 1.2 Contexte de tarification · `PricingSimulationRequest`

Corps commun à la simulation, aux prix catalogue, à l'aperçu panier et aux coupons :

```json
{
  "subscriberType": "MEMBER",
  "subscriberCode": "MBR-2026-000012",
  "subscriberSegment": null,
  "kycLevel": 2,
  "tenureMonths": 7,
  "firstPurchase": false,
  "lines": [
    { "reference": "L1", "scope": "PLAN", "code": "FLEX", "label": "Flex mensuel", "quantity": 1, "unitPrice": 100000, "setupFee": 0 }
  ],
  "billingCycle": "MONTHLY",
  "channel": "PORTAL",
  "paymentMethod": "WALLET",
  "locationCode": null,
  "currency": "XAF",
  "couponCodes": ["BIENVENUE10"],
  "evaluationDate": null,
  "promotionsAllowed": null
}
```

Côté espace client, `subscriberType`/`subscriberCode` sont remplacés par le membre authentifié ·
les envoyer est inutile. `lines[].reference` est votre identifiant de ligne, rendu tel quel dans
les résultats. `promotionsAllowed=false` est renseigné par le backend quand l'abonnement porte une
dérivation qui interdit le cumul (section 6).

### 1.3 Réponse · `PricingSimulationResponse`

```json
{
  "catalogTotal": 100000, "originalTotal": 100000, "discountTotal": 10000, "finalTotal": 90000, "currency": "XAF",
  "appliedRules": [
    { "order": 1, "sourceType": "COUPON", "sourceCode": "BIENVENUE10", "sourceName": "Bienvenue", "rewardType": "PERCENTAGE_OFF",
      "lineReference": "L1", "originalAmount": 100000, "discountAmount": 10000, "explanation": "10 % sur la première période" }
  ],
  "rejections": [ { "sourceType": "PROMOTION", "sourceCode": "ETE-2026", "reason": "réservée aux nouveaux abonnés" } ],
  "grants": [ { "sourceType": "PROMOTION", "sourceCode": "PARRAIN", "rewardType": "FREE_TRIAL_DAYS", "targetCode": null, "value": 7, "explanation": "7 jours offerts" } ],
  "priceOverrides": [ { "lineReference": "L1", "priceListCode": "PL-ENTREPRISE", "priceListName": "Grille entreprise", "catalogUnitPrice": 100000, "effectiveUnitPrice": 95000, "quantity": 1 } ]
}
```

Afficher `catalogTotal` barré, `finalTotal` en évidence, une ligne par `appliedRule`, et les
`rejections` sous forme d'explication (« ce code ne s'applique pas parce que… »), jamais comme une
erreur.

### 1.4 Endpoints

| Méthode | Route | Qui | Rôle dans l'écran |
|---|---|---|---|
| `POST` | `/pricing/simulate` | staff | Simulation libre, corps `PricingSimulationRequest` |
| `POST` | `/client/catalogue/prices` | client | Prix effectifs d'une liste d'objets, pour le catalogue |
| `POST` | `/client/promotions/available` | client | Les promotions et coupons que le membre peut voir, avec leur état |
| `POST` | `/client/cart/preview` | client | Aperçu du panier avant paiement |
| `POST` | `/client/cart/coupons?cartReference=&code=` | client | Applique un coupon au panier (le retient sans le consommer) |
| `DELETE` | `/client/cart/coupons/{code}?cartReference=` | client | Retire un coupon du panier (libère la retenue) |
| `GET` | `/coupons/available?subscriberType=&subscriberCode=` | staff | Coupons disponibles pour un abonné |
| `POST` | `/coupons/batches` | staff | Génère un lot de coupons |
| `POST` | `/coupons/{code}/reserve?cartReference=&subscriberType=&subscriberCode=` | staff | Retenue manuelle |
| `DELETE` | `/coupons/reservations?cartReference=&reason=` | staff | Libère les retenues d'un panier |
| `POST` | `/coupons/{code}/revoke?reason=` | staff | Révoque un coupon |
| `GET` | `/referrals/links?programCode=&referrerType=&referrerCode=` | client/staff | Le lien de parrainage d'un abonné |
| `POST` | `/referrals/links/{linkCode}/clicks` | public | Compte un clic |
| `POST` | `/referrals` | client/staff | Enregistre un filleul `{ linkCode, refereeType, refereeCode }` |
| `POST` | `/referrals/{referralNumber}/reject?reason=` | staff | Rejette un parrainage |
| `POST` | `/promotions/{code}/beneficiaries` | staff | Ajoute un bénéficiaire nominatif `{ subscriberType, subscriberCode, reason }` |
| `POST` | `/promotions/{code}/beneficiaries/import` | staff | Import en masse `{ beneficiaries: [...], reason }` → rapport |
| `DELETE` | `/promotions/{code}/beneficiaries?subscriberType=&subscriberCode=&reason=` | staff | Retire un bénéficiaire |
| `POST` | `/promotions/{code}/beneficiaries/notified?subscriberType=&subscriberCode=&channel=` | staff | Marque comme prévenu |
| `GET` | `/promotions/{code}/beneficiaries/usage` | staff | Utilisation de la promotion nominative |

**`/client/catalogue/prices`** prend `{ context: PricingSimulationRequest, items: [PriceableRef] }`
avec `PriceableRef = { scope, code, categoryCode, label, quantity, listPrice, setupFee, billingCycle }`
et renvoie une liste d'`EffectivePrice` :

```json
{ "scope": "PLAN", "code": "FLEX", "label": "Flex mensuel", "listPrice": 100000, "effectivePrice": 90000,
  "savingsAmount": 10000, "savingsPercent": 10, "priceSource": "PROMOTION", "promotionCode": "ETE-2026", "promotionLabel": "Offre d'été",
  "validUntil": "2026-09-30T23:59:59Z", "requiresCoupon": false, "conditionsSummary": "Nouveaux abonnés, engagement 12 mois", "currency": "XAF" }
```

`priceSource` : `CATALOGUE` (rien appliqué), `PRICE_LIST` (grille tarifaire), `PROMOTION`. `requiresCoupon`
signale un prix qui suppose un code : afficher « avec le code … » plutôt que le prix barré.

**`/client/promotions/available`** renvoie des `ClientPromotionView` :

```json
{ "kind": "COUPON_ASSIGNED", "code": "BIENVENUE10", "label": "Bienvenue", "description": "10 % sur la première période",
  "estimatedSaving": 10000, "currency": "XAF", "validUntil": "2026-12-31T23:59:59Z", "status": "APPLICABLE", "ineligibilityReason": null }
```

`kind` : `AUTOMATIC` (s'applique seule), `NOMINATIVE` (accordée à ce membre), `COUPON_ASSIGNED`
(code qui lui a été attribué), `COUPON_PUBLIC`. `status` : `APPLICABLE`, `ALREADY_APPLIED`,
`NOT_ELIGIBLE` (avec `ineligibilityReason`), `EXPIRED`, `EXHAUSTED`. N'afficher un bouton
« appliquer » que sur `APPLICABLE`.

### 1.5 Coupons · retenue puis capture

Un coupon appliqué au panier est **retenu** (`RESERVED`) sous `cartReference`, pas consommé. Il
est consommé quand la souscription est créée avec ce code (`couponCodes` sur
`POST /client/catalog/subscribe` `{ planCode, billingCycle, autoRenew, couponCodes: ["BIENVENUE10"] }`,
ou sur `POST /subscriptions` côté guichet) : la retenue de l'abonné est capturée, ou, s'il n'y en
avait pas, le code est retenu et capturé dans le même geste. Un panier abandonné libère la
retenue au bout de trente minutes ; retirer le code du panier la libère tout de suite. Le front
génère un `cartReference` stable par panier (UUID) pour la retenue et l'aperçu.

`CreateCouponBatchRequest` : `{ promotionCode, name, quantity (1..50000), couponKind, codePrefix, codeLength (6..32, défaut 10), maxRedemptionsPerCoupon, validFrom, validUntil, channel }` ·
`couponKind` : `SINGLE_USE`, `MULTI_USE`, `UNIQUE_PER_SUBSCRIBER`. `CouponResponse` :
`{ code, promotionCode, promotionName, promotionDescription, couponKind, status, remainingUses, validFrom, validUntil, batchCode }`.

### 1.6 Ce qui n'a pas d'écran d'administration

Les conditions et récompenses d'une promotion, les grilles tarifaires (`PriceList`) et les
programmes de parrainage sont des données gérées en base pour l'instant : il n'existe pas
d'endpoint de création. `POST /promotions` crée toujours une promotion simple (`discountType`,
`discountValue`, dates, `maxRedemptions`).

---

## 2. Portefeuille · espace client

Le portefeuille est une **avance client** : pas de retrait en espèces, un solde qui ne sert qu'à
payer l'entreprise, des transferts entre abonnés encadrés par des plafonds liés au niveau KYC.
Base : `/client/wallet/{walletNumber}` (le membre ne voit que ses portefeuilles ; un autre numéro
répond `404`).

Header optionnel sur toutes les opérations sensibles : `X-Device-Id: <identifiant stable de l'appareil>`.
Un appareil inconnu qui fait un gros transfert lève un signalement côté contrôle ; un appareil
révoqué est refusé. Générer l'identifiant une fois par appareil et le conserver.

### 2.1 Code secret (PIN)

| Méthode | Route | Corps | Réponse |
|---|---|---|---|
| `GET` | `/security` | | `WalletSecurityStatusResponse` |
| `POST` | `/security/pin` | `{ currentPin, newPin }` (`currentPin` absent à la première définition) | `WalletSecurityStatusResponse` |

```json
{ "walletNumber": "WAL-000123", "pinDefined": true, "pinMustChange": false, "pinExpired": false, "locked": false, "lockedForMinutes": null,
  "pinLength": 4, "operationsRequiringPin": ["TRANSFER", "PAYMENT", "UNLOCK"],
  "limits": { "policyCode": "KYC1", "policyName": "Niveau 1", "kycLevel": 1, "currency": "XAF", "maxSingleTransfer": 50000, "dailyTransferUsed": 0,
              "maxDailyTransfer": 100000, "remainingDailyTransfer": 100000, "monthlyTransferUsed": 0, "maxMonthlyTransfer": 500000, "remainingMonthlyTransfer": 500000,
              "dailyTopUpUsed": 0, "maxDailyTopUp": 200000, "remainingDailyTopUp": 200000, "maxBalance": 1000000, "maxDailyOperations": 20, "dailyOperationsUsed": 0,
              "upgradePath": "Passez au niveau KYC 2 pour relever vos plafonds" } }
```

Règles à refléter dans l'écran : tant que `pinDefined` est faux, aucune opération de
`operationsRequiringPin` n'est possible (proposer la création du code) ; `pinMustChange` force
l'écran de changement ; `locked` + `lockedForMinutes` après trop d'essais ; `limits.upgradePath`
est la phrase à afficher quand un plafond bloque.

### 2.2 Paiements avec le portefeuille

Les paiements existants prennent désormais le PIN dans le corps : `{ "pin": "1234" }`

| Méthode | Route |
|---|---|
| `POST` | `/client/billing/invoices/{documentNumber}/pay/wallet` |
| `POST` | `/client/subscriptions/{subscriptionNumber}/pay/wallet` |
| `POST` | `/client/passes/{passNumber}/pay/wallet` |

Sans PIN alors qu'il est requis → `400` avec le message ; PIN faux → `400`, et après N essais le
portefeuille se verrouille (`locked`).

### 2.3 Transferts entre abonnés

```
POST /transfers/simulate      { counterparty, amount, message }            → TransferPreview
POST /transfers               { counterparty, amount, message, paymentRequestNumber? } → InitiatedTransferView
POST /transfers/{n}/confirm   { pin }                                       → TransferView
POST /transfers/{n}/cancel                                                  → TransferView
GET  /transfers               (paginé)                                      → TransferView[]
GET  /transfers/{n}                                                         → TransferView
```

`counterparty` accepte indifféremment un numéro de portefeuille, un téléphone ou un courriel ·
le backend reconnaît la forme. Le destinataire retrouvé est rendu par un simple nom d'affichage
(`counterpartyName`), jamais son téléphone ou son courriel.

`TransferPreview` : `{ counterpartyWallet, counterpartyName, amount, fee, totalDebit, balanceAfter, currency, allowed, blockingReason, upgradePath, pinRequired, pinMissing, ... }` ·
**toujours simuler avant d'initier** et n'activer le bouton que si `allowed`.

`InitiatedTransferView` : `{ transfer: TransferView, confirmationCode, challenge, confirmationExpiresAt, maxAttempts }` ·
`challenge` vaut `PIN`, `OTP` ou `PIN_AND_OTP` ; `confirmationCode` est la référence de la
confirmation (pas un secret). Le transfert reste `PENDING_CONFIRMATION` jusqu'au `confirm` avec le
PIN, dans le délai `confirmationExpiresAt` ; au-delà il est annulé.

`TransferView` : `{ transferNumber, transferUuid, direction (OUT|IN), counterpartyWallet, amount, feeAmount, totalDebit, currency, status, message, paymentRequestNumber, failureReason, expiresAt, completedAt, createdAt }` ·
`status` : `PENDING_CONFIRMATION`, `COMPLETED`, `FAILED`, `CANCELLED`.

### 2.4 Bénéficiaires, demandes de paiement, rechargement

```
GET    /beneficiaries                         → [{ id, alias, walletNumber, transferCount, lastUsedAt, createdAt }]
POST   /beneficiaries   { counterparty, alias }
DELETE /beneficiaries/{id}

POST /payment-requests  { payer, amount, reason }   → PaymentRequestView   (je demande de l'argent à quelqu'un)
GET  /payment-requests  (paginé)                    → direction IN (reçues) ou OUT (émises)
POST /payment-requests/{n}/decline                  (le payeur refuse)
POST /payment-requests/{n}/cancel                   (le demandeur retire)
```

Payer une demande reçue = initier un transfert avec `paymentRequestNumber` renseigné ; la
demande passe `PAID`. `PaymentRequestView` : `{ requestNumber, direction, counterpartyWallet, amount, currency, reason, status (PENDING|PAID|DECLINED|CANCELLED|EXPIRED), transferNumber, expiresAt, resolvedAt, createdAt }`.

`POST /top-ups` `{ amount, phoneNumber, correspondent }` lance un dépôt mobile money (PawaPay,
`correspondent` = opérateur : `MTN_MOMO_COG`, `AIRTEL_COG`, …) et renvoie le
`MobileMoneyDepositResponse` habituel ; le crédit arrive à la confirmation du dépôt.

### 2.5 État, verrouillage, préférences, appareils, documents

```
GET  /state                                  → OwnerStateView { walletNumber, lockedByOwner, lockedAt, frozen, lowBalanceThreshold, notifyOnCredit, notifyOnDebit }
POST /lock                                   (le titulaire gèle lui-même · aucune sortie possible)
POST /unlock            { pin }
PUT  /preferences       { lowBalanceThreshold, notifyOnCredit, notifyOnDebit }
GET  /devices                                → [{ id, deviceId, label, firstSeenAt, lastSeenAt, lastIpAddress, useCount, trusted, revokedAt }]
PATCH /devices/{id}     { label, trusted }
POST /devices/{id}/revoke
GET  /statement?from=&to=                    → PDF (relevé)
GET  /ledger/{transactionNumber}/receipt     → PDF (reçu d'une écriture)
```

`frozen` (gel par l'administration ou la conformité) n'est pas `lockedByOwner` : le premier ne se
lève pas depuis l'espace client · afficher « portefeuille suspendu, contactez le support ».

---

## 3. Centre de contrôle et conformité (admin)

Rôles `ADMIN` / `SUPER_ADMIN` uniquement.

### 3.1 Sécurité et intégrité d'un portefeuille · `/wallets/{walletNumber}/...`

| Méthode | Route | Réponse |
|---|---|---|
| `POST` | `/security/pin/reset` | `204` · le titulaire devra redéfinir son code |
| `GET` | `/limits` | `LimitSnapshot` (voir 2.1) |
| `GET` | `/integrity` | `{ walletNumber, entriesChecked, brokenLinks: [], unchained, expectedBalance, actualBalance, balanceDrift }` |

Un portefeuille est intact si `brokenLinks` est vide et `balanceDrift` vaut 0 · afficher un
badge vert/rouge.

### 3.2 Centre de contrôle · `/wallets/control`

```
GET  /dashboard?currency=XAF         → Dashboard
GET  /export?from=&to=               → CSV des mouvements
POST /wallets/{n}/actions            { type, amount, reference, reason, until }   → AdminActionView
GET  /wallets/{n}/actions            (paginé)
GET  /actions/pending                (paginé) · ce qui attend un second visa
POST /actions/{actionNumber}/approve
POST /actions/{actionNumber}/reject  { reason }
GET  /flags?status=OPEN,UNDER_REVIEW (paginé)   → FlagView
GET  /wallets/{n}/flags
POST /flags/{flagNumber}/review      { holdWallet }
POST /flags/{flagNumber}/resolve     { confirmed, resolution }
GET  /treasury/snapshot?currency=
POST /treasury/reconciliations       { date, currency, ledgerAccountBalance, availableCash, explanation }
GET  /treasury/reconciliations       (paginé) · GET /treasury/reconciliations/{number}
```

`Dashboard` : `{ currency, totalWalletBalance, totalHeldBalance, openWallets, walletsByStatus, frozenWallets, lockedByOwner, dormantWallets, negativeBalances, todayVolume: [{ entryType, direction, count, amount }], openFlags, pendingApprovals, latestReconciliation }`.

**Quatre yeux** : toute action `TOPUP`, `DEBIT`, `ADJUST`, `REVERSE`, `SUSPEND`, `UNSUSPEND`,
`RESET_PIN`, `RAISE_LIMIT`, `CLOSE`, `REVIEW_FLAG` est créée `PENDING_APPROVAL` et n'est exécutée
qu'à l'approbation par **une autre personne** (le backend refuse l'auto-approbation, `409`).
`AdminActionView.status` : `PENDING_APPROVAL`, `EXECUTED`, `REJECTED`. Un `UNSUSPEND`
sur un gel d'instruction (conformité, préfixe « INSTRUCTION ») est refusé : il faut lever le gel
depuis la conformité.

`FlagView` : `{ flagNumber, walletNumber, type, severity (LOW..CRITICAL), status (OPEN|UNDER_REVIEW|CONFIRMED|DISMISSED), details, reference, detectedAt, detectedBy, reviewedBy, reviewedAt, resolution }`.
Types : `UNUSUAL_VOLUME`, `RAPID_IN_OUT`, `STRUCTURING`, `MANY_COUNTERPARTIES`, `DORMANT_REACTIVATION`, `FAILED_PIN_BURST`, `LIMIT_BREACH_ATTEMPT`, `NEGATIVE_BALANCE`, `INTEGRITY_BREAK`, `REVOKED_DEVICE_USED`, `NEW_DEVICE_LARGE_TRANSFER`, `LIST_MATCH`, `IDENTITY_REVIEW_DUE`, `PHONE_CHANGED`.

### 3.3 Conformité et surveillance · `/wallets/compliance`

```
GET  /dashboard                                → { openCases, overdueCases, openByPriority, openByAge, unreviewedListMatches, activeFreezes, rules: [RuleStat] }
GET  /rules · PUT /rules/{ruleCode}            RuleUpdateRequest { name, description, severity, action, amountThreshold, countThreshold, ratioThreshold, windowMinutes, active, effectiveFrom, effectiveTo, rationale }
POST /wallets/{n}/evaluate                     → [{ ruleCode, action, details, newFlag }]  (évaluation à la demande)

GET  /cases?scope=open|mine|all (paginé) · GET /cases/{n} · GET /cases/{n}/notes
POST /cases/{n}/assign   { assignee }
POST /cases/{n}/notes    { note }
POST /cases/{n}/escalate { escalatedTo, reason }
POST /cases/{n}/close    { decision (CLEARED|CONFIRMED|REPORTED), rationale, findings }
GET  /cases/{n}/export?activityDays=90          → JSON du dossier (déclaration / coopération)
GET  /wallets/{n}/cases

POST /screening/lists    { listCode, listVersion, type (SANCTION|PEP), entries: [{ fullName, aliases, birthYear, nationality, reference }] }
POST /screening/checks   { subjectType, subjectCode, fullName, birthYear }        → CheckView
GET  /screening/checks?scope=pending|all (paginé) · GET /screening/subjects/{type}/{code}
POST /screening/checks/{n}/review  { trueMatch, rationale }

POST /wallets/{n}/freeze  { authority, instructionReference, instructionDate, rationale }   → FreezeView (gel sur instruction)
POST /freezes/{n}/lift    { liftReference, liftRationale }
GET  /freezes (paginé) · GET /wallets/{n}/freezes
GET  /audit/{subjectType}/{subjectCode}        → piste d'audit (chaînée, append-only)
GET  /audit/verify                             → { entries, brokenEntries }
```

`CaseView` : `{ caseNumber, subjectType, subjectCode, walletNumber, title, triggeredByFlags, status (OPEN|IN_REVIEW|ESCALATED|CLOSED_CLEARED|CLOSED_CONFIRMED|CLOSED_REPORTED), priority, assignedTo, assignedAt, dueAt, overdue, findings, decision, decisionRationale, decidedBy, decidedAt, escalatedTo, escalatedAt, openedBy, createdAt }`.
`CheckView.result` : `CLEAR`, `POSSIBLE_MATCH`, `MATCH` · un `POSSIBLE_MATCH` ou `MATCH` non revu
figure dans `unreviewedListMatches` et attend `review`.

---

## 4. Pass · validation, usage, transfert

Base `/passes` (personnel, borne ou API de contrôle d'accès).

### 4.1 Identifiants (QR, code, NFC) · `/passes/{passNumber}/credentials`

```
GET    ?type=QR_CODE                          → IssuedCredential (l'identifiant courant, image incluse)
POST   ?type=QR_CODE&rotating=true            → nouvel identifiant (révoque le précédent du même type)
DELETE ?reason=                               → { revoked: n }
```

`IssuedCredential` : `{ value, credentialType (QR_CODE|BARCODE|NFC_TAG|ACCESS_CODE), rotating, validUntil, imageDataUri }` ·
`imageDataUri` est directement utilisable dans un `<img src>`. Un identifiant `rotating` change à
chaque émission et expire (`validUntil`) : régénérer côté client à l'ouverture de l'écran plutôt
que de le mettre en cache.

### 4.2 Valider, consommer, annuler

```
POST /passes/validate   { passNumber?, credentialValue?, bearerType, bearerCode, locationCode, resourceTypeCode }
     → { valid, reason, passNumber, remainingUses }
POST /passes/use        { passNumber?, credentialValue?, bearerType, bearerCode, usageType, locationCode, resourceCode, resourceTypeCode, channel, validatedBy, referenceType, referenceCode }
     → { usageNumber, passNumber, remainingUses }
POST /passes/usages/{usageNumber}/reverse?reason=&reversedBy=
```

`passNumber` **ou** `credentialValue` (le contenu scanné). `bearerType`/`bearerCode` désignent
qui présente le pass (le titulaire ou un utilisateur autorisé) ; les règles de validité fines
(jour, plage horaire, lieu, type de ressource, dates noires, maximum par jour, simultanéité)
sont évaluées ici et `reason` dit laquelle refuse. `usageType` : `CHECK_IN`, `BOOKING`,
`SERVICE`, `MEETING_ROOM`, `PRINT`, `OTHER` · `channel` : `QR_SCAN`, `MANUAL`, `KIOSK`, `API`.

Écran borne : `validate` au scan (vert/rouge + `reason`), puis `use` à l'entrée effective.

### 4.3 Transfert de pass

```
POST   /passes/{passNumber}/transfers?requestedBy=   { toOwnerType, toOwnerCode, reason, transferFee }  → { transferNumber, status }
POST   /passes/transfers/{n}/accept?acceptedBy=
POST   /passes/transfers/{n}/reject?reason=
DELETE /passes/transfers/{n}?reason=
```

Statuts : `PENDING_ACCEPTANCE`, `ACCEPTED`, `REJECTED`, `CANCELLED`, `COMPLETED`. Le pass change
de titulaire à l'acceptation ; les usages déjà consommés restent au cédant.

### 4.4 Espace client

`GET /client/passes` (filtres `passType`, `status`), `GET /client/passes/{n}`, paiement
mobile money / portefeuille (avec PIN), `GET /client/entitlements/balances` (droits restants).

---

## 5. Services et domiciliation

Base `/services` (rôles `ADMIN`, `SUPER_ADMIN`, `STAFF`). L'abonnement devient un **contenant** :
un plan plus des services.

### 5.1 Catalogue de services et services souscrits

```
GET  /catalogue?includeInactive=false       → [ServiceDefinitionView]
PUT  /catalogue                              ServiceDefinitionRequest (création ou remplacement par code)
GET  /subscriptions/{n}                      → [SubscriptionServiceView]
POST /subscriptions/{n}                      { serviceCode, quantity, unitPrice?, metadataJson }
POST /subscribed/{serviceNumber}/suspend     { reason }
POST /subscribed/{serviceNumber}/terminate
```

`ServiceDefinitionView` : `{ code, name, description, serviceCategory, deliveryMode (CONTINUOUS|ON_DEMAND|SCHEDULED|METERED), requiresContract, requiresKycLevel, requiresPhysicalResource, hasRegulatoryObligations, defaultBillingCycle, defaultNoticePeriodDays, defaultCommitmentMonths, unitPrice, currency, usageEntitlementCode, active }`.
Huit services sont seedés, dont `SVC-DOMICILIATION` (contrat + KYC 2) et les actes de courrier
(`SVC-MAIL-SCAN`, `SVC-MAIL-FORWARD`, `SVC-MAIL-STORAGE`).

`SubscriptionServiceView.status` : `PENDING` (attend un contrat ou une ressource), `ACTIVE`,
`SUSPENDED`, `TERMINATED`. Un service qui exige un contrat reste `PENDING` jusqu'à l'activation du
contrat de domiciliation ; un niveau KYC insuffisant répond `409` avec le nom du service.

### 5.2 Adresses de domiciliation

```
GET   /domiciliation/addresses                → [AddressView { code, label, streetNumber, streetName, district, city, countryCode, fiscalCapable, maxOccupants, occupants, active, notes }]
POST  /domiciliation/addresses                { label, address: { ...AddressRequest }, fiscalCapable, maxOccupants, notes }
PATCH /domiciliation/addresses/{code}         { label, fiscalCapable, maxOccupants, active, notes }
```

### 5.3 Contrat de domiciliation · le cycle avec ses verrous

```
POST /domiciliation/contracts                          OpenContractRequest                      → DRAFT
POST /domiciliation/contracts/{n}/prepare              verrou 1 · pièces KYC du représentant → contrat PDF, PENDING_SIGNATURE (sinon PENDING_DOCUMENTS)
POST /domiciliation/contracts/{n}/signed               { signedDocumentCode }                   → PENDING_REGISTRATION, démarche ouverte
GET  /domiciliation/contracts/{n}/registrations
POST /domiciliation/contracts/{n}/registration/submit  { administrationOffice, stampDutyAmount, registrationFeeAmount, paidBy (COMPANY|CLIENT), rebilled }
POST /domiciliation/contracts/{n}/registration/confirm verrou 2 · { administrationReference, registrationDate, expiresAt, receiptDocumentCode, registeredDocumentCode } → ACTIVE
POST /domiciliation/contracts/{n}/registration/reject  { reason } · une nouvelle démarche s'ouvre
GET  /domiciliation/registrations/in-progress          (paginé) · le tableau des dossiers en cours
POST /domiciliation/contracts/{n}/certificate          verrou 3 · { scope: COMMERCIAL | FISCAL }
POST /domiciliation/contracts/{n}/commitment           { billingCycle, commitmentMonths } · recalcule la qualité fiscale
POST /domiciliation/contracts/{n}/terminate            { reason, effectiveDate }
GET  /domiciliation/contracts?status=ACTIVE (paginé) · GET /domiciliation/contracts/{n}
GET  /domiciliation/registry                           → CSV (registre des domiciliés)
```

`OpenContractRequest` : `{ subscriptionNumber, legalName, legalForm, registrationNumber, taxNumber, legalRepresentativeName, addressCode, suiteNumber, billingCycle (MONTHLY|QUARTERLY|YEARLY), commitmentMonths, noticePeriodDays, startDate, mailForwardingMode (HOLD|FORWARD|SCAN_AND_FORWARD|SCAN_ONLY) }`.

Trois règles à montrer dans l'écran : (1) seuls trois rythmes ; (2) l'adresse n'est **fiscale que
sur douze mois d'engagement** · `ContractView.fiscalAddressEligible` se déduit, passer sous douze
mois révoque l'attestation fiscale en cours (`fiscalAddressRevokedAt`) ; (3) sans enregistrement
confirmé, pas d'activation ni d'attestation. Les refus sont explicites (`409`) : « pièces d'identité
du représentant légal à vérifier », « seule une attestation commerciale peut être délivrée ».

`ContractView` porte tout : statut, dates, `contractDocumentCode` / `signedDocumentCode`,
`certificateDocumentCode` + `certificateScope` + `certificateValidUntil` + `certificateInForce`,
`retainDocumentsUntil` (conservation dix ans après la fin).

### 5.4 Courrier

```
POST /domiciliation/contracts/{n}/mail        { mailType, senderName, senderReference, weightGrams, dimensions, notes }  → réceptionné, domicilié prévenu
GET  /domiciliation/contracts/{n}/mail (paginé) · GET /domiciliation/mail/pending (paginé) · GET /domiciliation/mail/{item}
GET  /domiciliation/mail/{item}/trail         → [{ eventType, actor, details, occurredAt }]
POST /domiciliation/mail/{item}/scan          { scanDocumentCode }
POST /domiciliation/mail/{item}/collect       { collectedBy, collectorIdDocument, collectorSignatureUrl }
POST /domiciliation/mail/{item}/forward       { trackingNumber, transportCost }
POST /domiciliation/mail/{item}/return        { reason }
POST /domiciliation/mail/{item}/destroy       { reason }
```

`MailItem.status` : `RECEIVED` → `NOTIFIED` → `SCANNED` / `COLLECTED` / `FORWARDED` / `RETURNED` /
`DESTROYED`. `storageDeadline` et `billable` / `billedAmount` disent quand la garde devient
payante. Les actes de scan et de réexpédition sont facturés à l'acte.

### 5.5 Espace client · `/client/domiciliation`

`GET /contracts`, `GET /contracts/{n}`, `GET /contracts/{n}/mail` (paginé),
`GET /contracts/{n}/mail/{item}/trail` · lecture seule, mêmes vues.

---

## 6. Abonnements dérivés

Un abonnement peut avoir son propre prix, ses droits et ses avantages **sans plan de catalogue
dédié** : on dérive la version de plan en une version privée que seul cet abonnement lit. Base
`/subscriptions/derivations` (rôles `ADMIN`, `SUPER_ADMIN`, `STAFF`).

### 6.1 Simuler, créer, viser

```
POST /subscriptions/{n}/simulate     SpecRequest   → Preview
POST /subscriptions/{n}              SpecRequest   → DerivationView (ACTIVE tout de suite, ou PENDING_APPROVAL)
GET  /subscriptions/{n}                            → historique
POST /subscriptions/{n}/revert       { reason }    → retour au catalogue courant, la dérivation passe EXPIRED
GET  /?status=PENDING_APPROVAL,ACTIVE (paginé) · GET /{code} · GET /{code}/deltas
POST /{code}/approve                               (par une autre personne que le demandeur · sinon 409)
POST /{code}/reject                  { reason }
```

`SpecRequest` :

```json
{ "price": 85000, "setupFee": null, "trialDays": null, "commitmentMonths": 12,
  "entitlementQuantities": { "MEETING_HOURS": 20 }, "unlimitedEntitlements": ["PRINT"], "includedBenefits": ["Casier"],
  "reason": "NEGOTIATION", "reasonDetails": "Accord du 12/09", "effectiveFrom": null, "effectiveTo": "2027-08-31",
  "renewalBehaviour": "KEEP", "revertAfterPeriods": null, "promotionsAllowed": false }
```

`reason` (obligatoire) : `NEGOTIATION`, `GOODWILL`, `PARTNERSHIP`, `PILOT`, `GRANDFATHERING`, `LOYALTY`, `CORRECTION`.
`renewalBehaviour` : `KEEP` (reste), `REVERT_TO_CATALOGUE` (retour au premier renouvellement),
`REVERT_AFTER_PERIODS` (avec `revertAfterPeriods`).

`Preview` : `{ subscriptionNumber, catalogueVersion, cataloguePrice, derivedPrice, discountPercent, totalImpact, currency, deltas: [{ type, target, catalogueValue, derivedValue, impact }], requiresApproval, blockingReason }` ·
`blockingReason` non nul = refus (prix plancher, rabais au-delà du maximum admis, rien ne
diffère) ; `requiresApproval` = un second visa sera demandé. **Toujours simuler avant de créer**
et montrer les `deltas` ligne par ligne.

`DerivationView.status` : `DRAFT`, `PENDING_APPROVAL`, `ACTIVE`, `SUPERSEDED` (remplacée par une
plus récente, `supersedesDerivationId` chaîne), `EXPIRED`, `REJECTED`.

### 6.2 Lot, protection tarifaire, concessions, politique

```
POST /bulk        { subscriptionNumbers: [...], spec, simulateOnly: true }    → Outcome
POST /protect     { previousVersionId, protectedUntil, simulateOnly: true }   → Outcome (GRANDFATHERING des abonnés d'une ancienne version)
GET  /batches/{batchCode}
GET  /concessions?from=&to=   → [{ requestedBy, reason, count, totalImpact }]
GET  /policy/approval-rule · PUT (ADMIN) { maxDiscountPercentWithoutApproval, maxImpactWithoutApproval, maxDiscountPercentAllowed }
PUT  /policy/plan-versions/{versionId}/floor-price (ADMIN)   { floorPrice }  (corps vide = retire le plancher)
```

`Outcome` : `{ batchCode (null en simulation), total, succeeded, failed, pendingApproval, totalImpact, lines: [{ subscriptionNumber, ok, derivationCode, impact, requiresApproval, message }] }` ·
un lot ne s'arrête pas à la première erreur : afficher les lignes en échec avec leur `message`.

---

## 7. Cycle de vie des abonnements (guichet)

Rôles `ADMIN`, `SUPER_ADMIN`, `STAFF` ; les écritures marquées **ADMIN** sont réservées.

### 7.1 Nouveaux statuts d'abonnement

`SubscriptionStatus` gagne trois valeurs à afficher :

| Statut | Sens | Ce que l'écran propose |
|---|---|---|
| `PENDING_DOCUMENTS` | l'activation attend le niveau KYC exigé par le plan | lien vers les pièces KYC ; réessayé automatiquement chaque heure |
| `GRACE_PERIOD` | échéance impayée, droits maintenus, suspension à venir | payer ; `POST /subscriptions/lifecycle/{n}/grace/exit` pour une régularisation constatée hors système |
| `PENDING_TERMINATION` | un préavis court, l'abonnement ne se renouvelle pas | voir le préavis, le retirer |

### 7.2 Politique · `/subscriptions/lifecycle/policy`

`GET` / `PUT` (ADMIN) : `{ prorationPolicy (DAILY|MONTH_STARTED|NONE), defaultNoticeDays, earlyTerminationFormula (NONE|FIXED_FEE|PERCENT_OF_REMAINING|REMAINING_PERIODS), earlyTerminationPercent, earlyTerminationFixedFee, freezeMaxPerYear, freezeMaxDays, freezeNoticeDays, freezeFeePercent, gracePeriodDays, suspensionAfterGraceDays, quoteValidityDays }`.
Un seul prorata pour l'entrée, la sortie et le changement de plan.

### 7.3 Engagement · `/subscriptions/lifecycle/{n}/commitment`

```
GET                                → CommitmentView { subscriptionNumber, commitmentMonths, commitmentStart, commitmentEnd, earlyTerminationFormula, earlyTerminationPercent, earlyTerminationFixedFee, autoRenewCommitment, source (PLAN|QUOTE|MANUAL), createdBy, createdAt }
PUT   { commitmentMonths, commitmentStart, formula, percent, fixedFee, autoRenewCommitment }
DELETE (ADMIN)
GET   /early-termination?on=YYYY-MM-DD   → { early, remainingMonths, fee, formula, commitmentEnd }
```

Posé automatiquement à la souscription si le prix de plan porte un engagement. `early-termination`
est la question à poser **avant** tout préavis.

### 7.4 Résiliation · `/subscriptions/terminations`

```
POST /subscriptions/{n}/preview   TerminationRequest → Preview
POST /subscriptions/{n}           TerminationRequest → TerminationView (abonnement PENDING_TERMINATION)
GET  /subscriptions/{n}
GET  /?status=REQUESTED · GET /blocked (date d'effet passée, liste de sortie ouverte) · GET /{code}
POST /{code}/accept               (facture les frais de rupture et les jours de raccordement)
POST /{code}/retract              { reason }   (409 si des frais ont été facturés · avoir d'abord)
POST /{code}/waive-fee  (ADMIN)   { reason }   (avant l'acceptation seulement)
POST /{code}/exit-items/{itemCode}/done   { detail }
POST /{code}/exit-items/{itemCode}/waive  { reason }
POST /{code}/complete             (avant la date d'effet, si la liste est vide)
```

`TerminationRequest` : `{ reasonCategory (RELOCATION|COST|SERVICE_QUALITY|BUSINESS_CLOSURE|NO_LONGER_NEEDED|COMPETITOR|NON_PAYMENT|OTHER), reason, channel (STAFF|PORTAL|LETTER|EMAIL), requestedEffectiveDate, noticePeriodDays }`.

`Preview` : `{ subscriptionNumber, noticePeriodDays, earliestEffectiveDate, effectiveDate, earlyTermination, commitmentEnd, remainingCommitmentMonths, feeAmount, bridgingAmount, currency, exitChecklist: [libellés] }` ·
la date d'effet ne peut pas précéder `earliestEffectiveDate` ; `bridgingAmount` est ce que
coûtent les jours entre la fin de la période payée et la date d'effet (l'abonnement en préavis
ne se renouvelle pas).

`TerminationView` contient la liste de sortie `exitItems: [{ itemCode, label, mandatory, status (PENDING|DONE|WAIVED), detail, doneBy, doneAt }]`
et `exitChecklistClear`. Codes : `BADGE_RETURN`, `KEYS_RETURN`, `BALANCE_DUE` (se coche seul
quand aucune facture n'est ouverte), `DEPOSIT_SETTLEMENT`, `TERMINATION_FEE`, et pour un domicilié
`MAIL_PENDING` (se coche seul), `DOMICILIATION_END`. Une ligne obligatoire `PENDING` bloque
l'achèvement : c'est le tableau `/blocked`.

### 7.5 Devis · `/subscriptions/quotes`

```
POST /                    QuoteRequest → QuoteView (DRAFT)
GET  /?status= (paginé) · GET /subscribers/{type}/{code} · GET /{n}
GET  /{n}/outlook         → { quoteNumber, cataloguePrice, quotedPrice, discountPercent, negotiated, requiresApproval, blockingReason, commitmentMonths, validUntil }
POST /{n}/send            (SENT, e-mail au souscripteur)
POST /{n}/accept          (CONVERTED · abonnement créé, dérivation posée si prix négocié, engagement posé)
POST /{n}/reject          { reason }
```

`QuoteRequest` : `{ subscriberType, subscriberCode, planCode, planVersionId?, billingCycle, quotedPrice? (catalogue si absent), setupFee?, commitmentMonths?, trialDays?, startDate?, validUntil? (politique si absent), notes, proposalNumber? (CRM) }`.
Un prix négocié est vérifié **dès la création** contre les règles de dérivation (plancher,
maximum) : `409` avec la raison. `QuoteView` porte `negotiated`, `convertedSubscriptionNumber`,
`derivationCode`. L'abonnement créé n'est pas auto-activé : le paiement l'active.

### 7.6 Prélèvement automatique · `/subscriptions/lifecycle/...`

```
POST /{n}/debit-mandate                   { walletNumber, channel (PORTAL|SIGNED_FORM|EMAIL|STAFF), reference, maxAmountPerDebit }  → MandateView
GET  /{n}/debit-mandate                   → [MandateView]
GET  /{n}/debit-attempts                  → [{ invoiceNumber, amount, currency, status (SUCCEEDED|INSUFFICIENT_FUNDS|OVER_LIMIT|FAILED), message, transactionNumber, executedAt }]
POST /debit-mandates/{code}/revoke        { reason }
POST /debit-mandates/{code}/reactivate    (après suspension pour trois échecs)
POST /{n}/debit-mandate/collect/{invoiceNumber}   (retente à la main) → AttemptView
```

`MandateView` : `{ mandateCode, subscriptionNumber, walletNumber, status (ACTIVE|SUSPENDED|REVOKED), consentGivenAt, consentGivenBy, consentChannel, consentReference, maxAmountPerDebit, consecutiveFailures, lastDebitAt, revokedAt, revokedBy, revocationReason }`.
Sans mandat, rien n'est prélevé. Un échec ouvre la tolérance (`GRACE_PERIOD`) ; trois échecs de
suite suspendent le mandat.

### 7.7 Gel, prorata, alignement

```
GET  /subscriptions/lifecycle/{n}/freezes             → [{ startedOn, plannedUntil, resumedOn, daysPlanned, daysEffective, feeAmount, feeBillableNumber, reason, requestedBy }]
GET  /subscriptions/lifecycle/{n}/freezes/allowance   → { freezesUsedThisYear, freezesAllowedPerYear, maxDays, feePercent, canFreeze }
GET  /subscriptions/lifecycle/{n}/plan-change/preview?targetPlanVersionId=&effectiveDate=
     → { policy, effectiveDate, currentPeriodTotal, targetPeriodTotal, unusedAtCurrentPrice, remainingAtTargetPrice, amount, credit, targetAmounts, currency }
POST /subscriptions/lifecycle/align   { subscriberType, subscriberCode, targetPeriodEnd?, simulateOnly: true }
     → { subscriberType, subscriberCode, targetPeriodEnd, total, aligned, skipped, totalBridging, lines: [{ subscriptionNumber, currentPeriodEnd, newPeriodEnd, bridgingDays, bridgingAmount, aligned, message }] }
```

Le gel passe toujours par `POST /subscriptions/{n}/pause` (existant) et respecte désormais
`allowance` (`409` sinon). Le changement de plan passe par le module `change` existant
(`/subscription-changes`) ; à l'application, `amount` est facturé si `credit=false`, crédité au
portefeuille si `credit=true`, et `SubscriptionChangeResponse` porte `prorationPolicy`,
`prorationCredit`, `prorationBillableNumber`. L'alignement ne va que vers l'avant ; une ligne
`aligned=false` explique pourquoi (« demanderait de rendre de l'argent »).

---

## 8. Espace client · libre-service et historique

Base `/client`. Le membre n'agit que sur ses abonnements ; un numéro qui n'est pas à lui répond
`404`. Toutes ces routes passent par les mêmes règles que le guichet.

### 8.1 Changer de plan

```
GET  /subscriptions/{n}/plan-change/preview?targetPlanCode=PRO&atNextPeriod=false
     → { targetPlanCode, targetPlanName, targetPeriodTotal, currentPeriodTotal, amount, credit, prorationPolicy, effectiveDate, upgrade }
POST /subscriptions/{n}/plan-change   { targetPlanCode, atNextPeriod, reason }   → SubscriptionChangeResponse
```

Immédiat : appliqué tout de suite (`status = APPLIED`, `amount` facturé ou crédité). À la
prochaine échéance : `APPROVED`, appliqué par le worker à la date (`amount` = 0 dans la
prévision). Refusé (`409`) pendant un préavis, ou si le plan cible n'existe pas au rythme de
l'abonnement.

### 8.2 Geler et reprendre

```
GET  /subscriptions/{n}/freeze/terms   → { freezesUsedThisYear, freezesAllowedPerYear, maxDays, feePercent, noticeDays, canFreezeNow }
POST /subscriptions/{n}/freeze         { until, reason }    → SubscriptionResponse (PAUSED)
POST /subscriptions/{n}/resume                              → SubscriptionResponse (ACTIVE)
```

Si `canFreezeNow` est faux avec `noticeDays > 0`, le gel passe par le support (le gel portail est
immédiat) · afficher le message plutôt qu'un bouton. `feePercent > 0` : prévenir que la part du
tarif sur les jours gelés sera facturée.

### 8.3 Résilier

```
GET  /subscriptions/{n}/commitment/early-termination?on=     → { early, remainingMonths, fee, formula, commitmentEnd }
POST /subscriptions/{n}/termination/preview   { reasonCategory, requestedEffectiveDate }   → Preview (section 7.4)
POST /subscriptions/{n}/termination           { reasonCategory, reason, requestedEffectiveDate }
     → { terminationCode, status, requestedAt, effectiveDate, noticePeriodDays, reasonCategory, earlyTermination, feeDue, exitItemsPending: [libellés] }
GET  /subscriptions/{n}/terminations
POST /terminations/{code}/retract             { reason }
```

Parcours : montrer `early-termination` (ce que coûte de partir maintenant), puis `preview`
(date d'effet la plus proche, frais, raccordement, liste de sortie), puis demander. Le membre ne
choisit pas son préavis (celui de la politique) ; la dispense de frais reste au guichet.
`exitItemsPending` dit ce qu'il doit faire avant la clôture (rendre le badge, retirer son
courrier…).

### 8.4 Prélèvement

```
GET    /subscriptions/{n}/debit-mandate     → { mandateCode, walletNumber, status, consentGivenAt, maxAmountPerDebit, lastDebitAt }  (404 sans mandat)
POST   /subscriptions/{n}/debit-mandate     { walletNumber, maxAmountPerDebit? }
DELETE /subscriptions/{n}/debit-mandate
```

Le consentement est celui du membre (canal `PORTAL`). Afficher clairement ce que le mandat
autorise : chaque échéance sera prélevée sur ce portefeuille, dans la limite du plafond.

### 8.5 Devis

`GET /quotes`, `GET /quotes/{n}`, `POST /quotes/{n}/accept`, `POST /quotes/{n}/reject { reason }` ·
`QuoteView` : `{ quoteNumber, status (DRAFT|SENT|ACCEPTED|REJECTED|EXPIRED|CONVERTED), planCode, planName, billingCycle, currency, cataloguePrice, quotedPrice, setupFee, commitmentMonths, trialDays, startDate, validUntil, notes, convertedSubscriptionNumber }`.
N'afficher accepter/refuser que sur `SENT` (ou `DRAFT`) et avant `validUntil`.

### 8.6 Historique lisible

```
GET /history                          → [Entry]  (tous les abonnements du membre, du plus récent au plus ancien)
GET /subscriptions/{n}/timeline       → [Entry]
```

`Entry` : `{ occurredAt, kind (SUBSCRIPTION|PAYMENT|CHANGE|FREEZE|TERMINATION|BENEFIT|PASS|USAGE), subscriptionNumber, title, detail, amount, currency }` ·
`title` et `detail` sont déjà en français (« Conditions particulières accordées · Tarif négocié ·
15 % de remise · jusqu'au 2027-08-31 »). Une icône par `kind` suffit.

---

## 9. Indicateurs et relances d'impayés

### 9.1 Indicateurs · `GET /subscription-metrics/kpis?from=&to=` (défaut : 30 derniers jours)

```json
{ "from": "2026-08-22", "to": "2026-09-21",
  "activeSubscriptions": 138, "mrr": 9450000.00, "arr": 113400000.00, "mrrByCycle": { "MONTHLY": 7200000, "QUARTERLY": 900000, "YEARLY": 1350000 },
  "activeAtStart": 131, "newInPeriod": 12, "churnedInPeriod": 5, "churnRate": 3.82, "monthlyChurnRate": 3.7,
  "newMrr": 840000.00, "churnedMrr": 320000.00, "netMrrMovement": 520000.00,
  "arpu": 68478.26, "customerLifetimeValue": 1850764.00, "averageLifetimeMonths": 27.0,
  "seatsOccupied": 96, "workspaceCapacity": 120, "occupancyRate": 80.00, "revenuePerSeat": 98437.50,
  "inGracePeriod": 3, "pendingTermination": 2, "pendingDocuments": 1 }
```

`churnRate` et `occupancyRate` sont en pourcentage. `customerLifetimeValue` et
`averageLifetimeMonths` sont `null` quand l'attrition est nulle (afficher « n/d »). `GET
/subscription-metrics/overview` (existant) reste disponible pour les compteurs bruts.

### 9.2 Relances d'impayés · `/billing/dunning` (staff ; écriture ADMIN)

```
GET  /policies · GET /policies/{code}
PUT  /policies            PolicyRequest { policyCode, name, segment (DEFAULT|MEMBER|CUSTOMER|BUSINESS_ENTITY), tone (SOFT|STANDARD|FIRM), active, steps: [StepRequest] }
POST /policies/{code}/deactivate
GET  /notices             (200 dernières) · GET /notices/documents/{documentNumber} · GET /notices/customers/{type}/{code}
POST /run                 → { documents, notices, failed }   (un passage à la main)
```

`StepRequest` : `{ daysAfterDue, action (REMINDER|FORMAL_NOTICE|GRACE_PERIOD|SUSPEND|HANDOVER), channel (EMAIL|IN_APP|STAFF), subjectTemplate, messageTemplate }` ·
les paliers se donnent dans l'ordre des jours (`409` sinon) ; les gabarits acceptent
`{documentNumber}`, `{balanceDue}`, `{currency}`, `{dueDate}`, `{daysOverdue}`, `{customerName}`,
`{subscriptionNumber}`. Une seule politique active par segment : en activer une désactive l'autre.

`NoticeView` : `{ documentNumber, policyCode, stepOrder, action, customerType, customerCode, subscriptionNumber, daysOverdue, balanceDue, outcome (SENT|SKIPPED|FAILED), detail, executedAt }` ·
`SKIPPED` porte la raison (« facture sans abonnement », « aucune adresse de contact »).

Sur la fiche d'une facture ou d'un client, `notices/documents/{n}` et
`notices/customers/{type}/{code}` donnent la chronologie des relances.

---

## 10. Inscription, téléphones et activation

### 10.1 Numéros de téléphone

Le backend accepte toutes les formes et les conserve au format international :

| Saisi | Pays du formulaire | Conservé |
|---|---|---|
| `06 256 36 15` | CG | `+242062563615` |
| `+33 6 41 53 45 35` | CG | `+33641534535` |
| `33641534535` (sans `+`) | CG | `+33641534535` (l'indicatif est reconnu) |
| `0033641534535` | quelconque | `+33641534535` |
| `0641534535` | FR | `+33641534535` (le zéro de tête tombe) |
| `062563615` | CG | `+242062563615` (le Congo garde son zéro) |

Le champ pays fournit l'indicatif quand le numéro n'en a pas. Les espaces, points, tirets et
parenthèses sont ignorés. Un indicatif inconnu répond `400`. Aucune mise en forme n'est
nécessaire côté front ; afficher tel que renvoyé.

### 10.2 Activation par l'administration et connexion

Quand un administrateur passe un membre `ACTIVE` (ou active l'accès portail), le compte reçoit le
rôle `MEMBER` et l'e-mail est marqué vérifié, même si l'utilisateur n'a pas saisi son code. La
réponse de connexion porte désormais :

```json
{ "accessToken": "...", "refreshToken": "...", "emailVerified": true, "roles": ["MEMBER"] }
```

`roles` vide = compte pas encore activé : ne pas rediriger vers l'espace client, afficher l'écran
« en attente d'activation ». Un `403` sur `/client/me` après connexion vient de là.

### 10.3 Codes à usage unique (admin) · `GET /admin/one-time-tokens?email=&used=` (paginé)

`{ token, email, userId, firstname, lastname, createdAt, expiredAt, used, expired, status (VALID|USED|EXPIRED) }` ·
pour le support, voir quel code a été envoyé à qui et s'il a servi.

---

## 11. Annexe · énumérations

**Tarification** · `EffectivePrice.PriceSource`: CATALOGUE, PRICE_LIST, PROMOTION · `TargetScope`: WHOLE_ORDER, LINE, PLAN, ADDON, PASS, ENTITLEMENT, SERVICE, RESOURCE, INVENTORY_ITEM, CATEGORY ·
`DiscountSourceType`: PROMOTION, COUPON, PRICE_LIST, REFERRAL, MANUAL ·
`RewardType`: PERCENTAGE_OFF, FIXED_AMOUNT_OFF, WAIVE_SETUP_FEE, FREE_PERIODS, FREE_TRIAL_DAYS, FREE_ENTITLEMENT, EXTRA_ENTITLEMENT_UNITS, FREE_ADDON, UPGRADE_TIER, WALLET_CREDIT ·
`CouponKind`: SINGLE_USE, MULTI_USE, UNIQUE_PER_SUBSCRIBER · `CouponStatus`: ACTIVE, USED, EXPIRED, REVOKED.

**Portefeuille** · `WalletTransferStatus`: PENDING_CONFIRMATION, COMPLETED, FAILED, CANCELLED ·
`WalletPaymentRequestStatus`: PENDING, PAID, DECLINED, CANCELLED, EXPIRED · `WalletChallengeType`: PIN, OTP, PIN_AND_OTP ·
`WalletAdminAction.Type`: TOPUP, DEBIT, ADJUST, REVERSE, SUSPEND, UNSUSPEND, RESET_PIN, RAISE_LIMIT, CLOSE, REVIEW_FLAG · `.Status`: PENDING_APPROVAL, EXECUTED, REJECTED ·
`WalletRiskFlag.Severity`: LOW, MEDIUM, HIGH, CRITICAL · `.Status`: OPEN, UNDER_REVIEW, CONFIRMED, DISMISSED.

**Conformité** · `ComplianceCase.Status`: OPEN, IN_REVIEW, ESCALATED, CLOSED_CLEARED, CLOSED_CONFIRMED, CLOSED_REPORTED · `.Priority`: LOW, MEDIUM, HIGH, CRITICAL · `.Decision`: CLEARED, CONFIRMED, REPORTED ·
`ComplianceRule.Category`: VELOCITY, AMOUNT, PATTERN, COUNTERPARTY, IDENTITY, GEOGRAPHY · `.Severity`: INFO, LOW, MEDIUM, HIGH, CRITICAL · `.Action`: FLAG, REQUIRE_REVIEW, BLOCK_OPERATION, FREEZE_WALLET ·
`ScreeningCheck.Result`: CLEAR, POSSIBLE_MATCH, MATCH · `.Trigger`: ONBOARDING, PERIODIC, PHONE_CHANGE, MANUAL, CASE_REVIEW · `ScreeningListEntry.Type`: SANCTION, PEP.

**Pass** · `PassCredentialType`: QR_CODE, BARCODE, NFC_TAG, ACCESS_CODE · `PassUsageType`: CHECK_IN, BOOKING, SERVICE, MEETING_ROOM, PRINT, OTHER ·
`PassValidationChannel`: QR_SCAN, MANUAL, KIOSK, API · `PassTransferStatus`: PENDING_ACCEPTANCE, ACCEPTED, REJECTED, CANCELLED, COMPLETED ·
`PassValidityRuleType`: DAY_OF_WEEK, TIME_RANGE, LOCATION, RESOURCE_TYPE, BLACKOUT_DATE, MAX_PER_DAY, MAX_CONCURRENT · `PassAlertType`: EXPIRING, LOW_BALANCE, UNUSED.

**Services et domiciliation** · `ServiceDefinition.Category`: DOMICILIATION, MAIL_HANDLING, PHONE_ANSWERING, STORAGE, MEETING_ROOM, COWORKING_ACCESS, PRIVATE_OFFICE, ADMIN_SUPPORT, LEGAL_SUPPORT, ACCOUNTING_SUPPORT, IT_SUPPORT, EVENT_SPACE, PARKING, LOCKER, OTHER ·
`DeliveryMode`: CONTINUOUS, ON_DEMAND, SCHEDULED, METERED · `SubscriptionService.Status`: PENDING, ACTIVE, SUSPENDED, TERMINATED ·
`DomiciliationContract.Status`: DRAFT, PENDING_DOCUMENTS, PENDING_SIGNATURE, PENDING_REGISTRATION, ACTIVE, SUSPENDED, TERMINATED, EXPIRED · `.CertificateScope`: COMMERCIAL, FISCAL · `.MailForwardingMode`: HOLD, FORWARD, SCAN_AND_FORWARD, SCAN_ONLY ·
`DomiciliationRegistration.Status`: PENDING, SUBMITTED, REGISTERED, REJECTED · `.PaidBy`: COMPANY, CLIENT ·
`MailItem.Type`: LETTER, REGISTERED_LETTER, PARCEL, ADMINISTRATIVE, LEGAL_NOTICE, OTHER · `.Status`: RECEIVED, NOTIFIED, SCANNED, COLLECTED, FORWARDED, RETURNED, DESTROYED.

**Dérivations** · `PlanDerivation.Status`: DRAFT, PENDING_APPROVAL, ACTIVE, SUPERSEDED, EXPIRED, REJECTED · `.Reason`: NEGOTIATION, GOODWILL, PARTNERSHIP, PILOT, GRANDFATHERING, LOYALTY, CORRECTION ·
`.RenewalBehaviour`: KEEP, REVERT_TO_CATALOGUE, REVERT_AFTER_PERIODS · `PlanDerivationDelta.Type`: PRICE, ENTITLEMENT, BILLING_CYCLE, COMMITMENT, NOTICE_PERIOD, TRIAL, SETUP_FEE, PAYMENT_TERMS, ADDON_INCLUDED.

**Cycle de vie** · `SubscriptionStatus`: DRAFT, PENDING_ACTIVATION, PENDING_DOCUMENTS, TRIALING, ACTIVE, GRACE_PERIOD, PAST_DUE, PAUSED, SUSPENDED, PENDING_TERMINATION, CANCELLED, EXPIRED ·
`ProrationPolicy`: DAILY, MONTH_STARTED, NONE · `EarlyTerminationFormula`: NONE, FIXED_FEE, PERCENT_OF_REMAINING, REMAINING_PERIODS ·
`SubscriptionTermination.Status`: REQUESTED, ACCEPTED, RETRACTED, COMPLETED · `.Channel`: STAFF, PORTAL, LETTER, EMAIL · `.ReasonCategory`: RELOCATION, COST, SERVICE_QUALITY, BUSINESS_CLOSURE, NO_LONGER_NEEDED, COMPETITOR, NON_PAYMENT, OTHER ·
`SubscriptionExitItem.Status`: PENDING, DONE, WAIVED · `SubscriptionQuote.Status`: DRAFT, SENT, ACCEPTED, REJECTED, EXPIRED, CONVERTED ·
`SubscriptionDebitMandate.Status`: ACTIVE, SUSPENDED, REVOKED · `.Channel`: PORTAL, SIGNED_FORM, EMAIL, STAFF · `SubscriptionDebitAttempt.Status`: SUCCEEDED, INSUFFICIENT_FUNDS, OVER_LIMIT, FAILED ·
`SubscriptionCommitment.Source`: PLAN, QUOTE, MANUAL.

**Relances** · `DunningPolicy.Segment`: DEFAULT, MEMBER, CUSTOMER, BUSINESS_ENTITY · `.Tone`: SOFT, STANDARD, FIRM · `DunningStep.Action`: REMINDER, FORMAL_NOTICE, GRACE_PERIOD, SUSPEND, HANDOVER · `.Channel`: EMAIL, IN_APP, STAFF · `DunningNotice.Outcome`: SENT, SKIPPED, FAILED.

**Historique client** · `Kind`: SUBSCRIPTION, PAYMENT, CHANGE, FREEZE, TERMINATION, BENEFIT, PASS, USAGE.

---

## 12. Annexe · rôles par route

| Préfixe | Rôle |
|---|---|
| `/client/**` | `MEMBER` (le membre authentifié, ses objets seulement) |
| `/pricing/simulate`, `/coupons/**`, `/referrals/**`, `/promotions/**` | personnel authentifié |
| `/passes/**` | personnel, borne ou intégration (authentifié) |
| `/wallets/{n}/security/pin/reset`, `/wallets/{n}/limits`, `/wallets/{n}/integrity` | `ADMIN`, `SUPER_ADMIN` |
| `/wallets/control/**`, `/wallets/compliance/**` | `ADMIN`, `SUPER_ADMIN` |
| `/services/**` | `ADMIN`, `SUPER_ADMIN`, `STAFF` |
| `/subscriptions/derivations/**` | `ADMIN`, `SUPER_ADMIN`, `STAFF` · politique (`/policy/**`) en écriture : `ADMIN`, `SUPER_ADMIN` |
| `/subscriptions/lifecycle/**`, `/subscriptions/terminations/**`, `/subscriptions/quotes/**` | `ADMIN`, `SUPER_ADMIN`, `STAFF` · `PUT /policy`, `DELETE commitment`, `waive-fee` : `ADMIN`, `SUPER_ADMIN` |
| `/subscription-metrics/**` | personnel authentifié |
| `/billing/dunning/**` | `ADMIN`, `SUPER_ADMIN`, `STAFF` · `PUT /policies`, `deactivate`, `run` : `ADMIN`, `SUPER_ADMIN` |
| `/admin/one-time-tokens` | `ADMIN`, `SUPER_ADMIN` |

Documentation backend correspondante : `docs/backend/conformite-surveillance-portefeuille.md`,
`services-et-domiciliation.md`, `abonnements-derives.md`, `cycle-de-vie-abonnements.md`,
`libre-service-et-pilotage.md`.
