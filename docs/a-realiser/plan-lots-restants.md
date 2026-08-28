# Lots restants — chantier robustesse & fonctionnalités

> État au 2026-08-27 · Fait suite à l'audit facturation / portefeuille / KYC du 2026-08-26
> Plan initial en 5 lots. **Lot 1 livré**, lots 2 à 5 à faire.

---

## 1. Ce qui est déjà livré (ne pas refaire)

> ⚠️ **72 fichiers non commités** sur `feature/payment-facturation` au moment d'écrire.
> Tout ce qui suit existe dans l'arbre de travail mais n'est ni commité ni déployé.

### Lot 1 — Verrouillage et idempotence des modules monétaires ✅

| Sujet | Livré |
|---|---|
| Verrou portefeuille | `WalletLedgerService` devient le **seul** propriétaire des mutations de solde. `flush` + `refresh(PESSIMISTIC_WRITE)` — le `@Lock` seul ne suffit pas, Hibernate rendrait l'instance en cache avec un solde périmé |
| Blocages de fonds | `placeHold` / `captureHold` / `releaseHold` centralisés ; les trois chemins de `WalletHoldServiceImpl` ne muraient plus les soldes en direct |
| Idempotence | Clé **explicite** passée par l'appelant, jamais dérivée automatiquement. `null` = pas de déduplication, indispensable pour les remboursements partiels successifs |
| Verrou facturation | `applyPayment` / `reversePayment` sous `lockByDocumentNumber` (JPQL — Spring Data ignore `@Lock` sur les requêtes natives) |
| Rattrapage PawaPay | L'échec d'imputation devient un événement outbox `PAYMENT_ALLOCATION_RETRY` rejoué avec backoff, au lieu d'être journalisé et perdu |
| Réconciliation | `WalletReconciliationWorker` (3h15 quotidien) : contrôle `ledger = disponible + bloqué` et `ledger = somme des écritures`. Ne corrige rien volontairement |

### Hors-lot — demandes arrivées en cours de route ✅

- Remise **par ligne** affichée sur le PDF (devis et facture), plus seulement dans le récapitulatif
- **Unité** affichée à côté de la quantité
- **Catégorie** de ligne : nouvelle colonne, figée à la création, reprise du catalogue via `itemCode`
- Correction du **destinataire** sans changer le propriétaire : `PATCH /billing/documents/{n}/recipient`

### Bugs corrigés en chemin ✅

| Bug | Cause |
|---|---|
| Annulation de facture impossible en ADMIN | `AdminApiAuthorizationManager` résolvait `DELETE /billing` en `BILLING:DELETE`, permission absente du référentiel donc refusée pour tous sauf SUPER_ADMIN. Même défaut sur BOOKING, SUBSCRIPTION et les 5 `DELETE` de `/contracts` |
| Endpoint d'annulation inexistant | `cancelAndArchive` implémenté depuis longtemps mais jamais exposé. Câblé sur le contrat déjà publié (`docs/frontend/billing-immutability.md:134`) |
| 403 indiagnosticable | La réponse nomme désormais `requiredPermission`, et un `WARN` serveur journalise les autorités réelles |
| `/auth/me` bloqué par `ADMIN:ACCESS` | L'endpoint censé révéler tes droits était bloqué par le problème qu'il devait expliquer |
| Lignes non taxables comptées pour zéro | Le total document se calculait depuis la **base taxable**, or `taxableAmount` vaut 0 sur une ligne exonérée. Tous les avoirs sortaient à 0,00 → annulation d'une facture scellée impossible |
| Correction destinataire sur facture scellée → 500 | `trg_billing_document_immutable` est **plus strict que la chaîne de hachage** : il protège aussi `customer_name`, `customer_niu`, `billing_address_json` |
| Badge catégorie coupé en deux au PDF | Le moteur PDF calcule mal la largeur d'un `inline-block` mêlant `text-transform` et `letter-spacing` |

### Migrations écrites (dev + prod)

| Dev | Prod | Contenu |
|---|---|---|
| V206 | V202 | Garde-fous de concurrence monétaire : `wallet_account.version`, `wallet_ledger_entry.idempotency_key` + index unique partiel, CHECK de solde en `NOT VALID`, unicité `(owner, devise)` et `(transaction, facture)` |
| V207 | V203 | `billing_document_line.category` |
| V208 | V204 | Réalignement des droits SUPER_ADMIN / ADMIN — réactive les rattachements désactivés, trou laissé par V148/V150 |

**Appliquées en dev uniquement.** La prod les recevra au prochain déploiement.

---

## 2. Lot 2 — Promotion applicable à une réservation

**Pourquoi** : `Promotion` est structurellement incapable de sortir de l'abonnement.
`grep -r "Promotion" features/booking features/billing` → **0 résultat**.
`Booking` n'a ni `discountAmount` ni `promotionCode` ; `BookingServiceImpl` pose `totalAmount = subtotal`, point.

### a. Généraliser `Promotion`

```
applies_to                      ENUM(SUBSCRIPTION, BOOKING, INVOICE, ANY)
eligibility_json                jsonb — resourceTypeCodes, subscriberTypes, montant min, 1re résa
max_discount_amount             plafond pour DiscountType.PERCENTAGE (aujourd'hui non plafonné)
max_redemptions_per_subscriber
stackable / priority            cumul avec les remises d'abonnement
currency
```

### b. Découpler `CouponRedemption`

Remplacer la FK dure `subscription_id` par `target_type` / `target_code` génériques, en gardant la FK pour l'existant.

### c. Corriger la sur-consommation

`PromotionServiceImpl:82-97` est un read-modify-write non verrouillé :

```java
if (promotion.getRedemptionCount() >= promotion.getMaxRedemptions())  // ligne 82
...
promotion.setRedemptionCount(promotion.getRedemptionCount() + 1);      // ligne 96
```

Deux clients qui saisissent le code en même temps passent tous deux la ligne 82. Il faut :

```sql
UPDATE promotion SET redemption_count = redemption_count + 1
WHERE id = ? AND (max_redemptions IS NULL OR redemption_count < max_redemptions)
```

et tester le nombre de lignes affectées, **plus** un index unique `(promotion_id, subscriber_type, subscriber_code)` — le `existsRedemption` de la ligne 85 ne protège rien sans lui.

### d. Côté réservation

- `Booking.discountAmount` + `promotionCode`
- valeur `DISCOUNT` dans `BookingLineType` (aujourd'hui `RESOURCE, ENTITLEMENT, BILLABLE`)
- endpoint de devis `POST /bookings/quote` renvoyant le prix avec et sans promo **avant** confirmation, pour éviter le « le prix a changé » au paiement
- propager `promotionCode` dans `BillingDocumentDiscount` pour que la facture porte la remise et que l'analytique attribue le manque à gagner

**Estimation** : ~3 j

---

## 3. Lot 3 — Politique de ressource ajustable par abonné

**Ce qui existe déjà** :

| Brique | Portée |
|---|---|
| `ressource/model/ResourcePolicy` | **Globale** — 1 FK `Resource.policy_id` |
| `booking/model/BookingAudiencePolicy` | Par `SubscriberType` × type/groupe de ressource |
| `booking/model/BookingQuotaOverride` | Par abonné mais **quotas seulement** (`extra_bookings_per_*`) |

`BookingQuotaOverride` a déjà le bon squelette (owner + ressource + validité + `reason` + `approvedBy`) mais ne porte pas les durées, préavis, annulation ni approbation.

### Approche : l'étendre en `BookingPolicyOverride`

Plutôt qu'une 4ᵉ table. Colonnes à ajouter, **toutes nullable = hérite du niveau supérieur** :

```
min_booking_duration_minutes, max_booking_duration_minutes,
min_booking_notice_minutes, cancellation_notice_minutes,
allow_cancellation, approval_required, max_advance_booking_days,
resource_type_code, resource_group_code,   -- seul resource_code existe aujourd'hui
subscription_code                           -- la dérogation meurt avec l'abonnement
```

### Chaîne de résolution

Un seul service, `BookingPolicyService.resolveEffective(owner, resource, at)` :

```
ResourcePolicy (base)
  └─> BookingAudiencePolicy (par SubscriberType)
        └─> plan d'abonnement
              └─> BookingPolicyOverride (par abonné)   ← le plus spécifique gagne
```

### Trois points qui font la différence

- **Snapshot obligatoire** — `policy_snapshot_json` sur `Booking`, figé à la création. Sinon annuler une réservation faite il y a 3 mois applique les règles d'aujourd'hui. C'est la raison exacte pour laquelle `sellerName` / `sellerNiu` sont déjà figés sur `BillingDocument`.
- **Endpoint de politique effective** — `GET /bookings/policies/effective?resourceCode=&ownerType=&ownerCode=`, pour que le front grise les créneaux impossibles au lieu d'échouer au POST.
- **Garde-fou** — une dérogation ne devrait pas pouvoir *assouplir* certaines règles sans approbation. Flag `requires_approval` sur l'override, cohérent avec le `approvedBy` déjà présent.

**Estimation** : ~3 j

---

## 4. Lot 4 — `kycLevel` réellement exploité

> La réconciliation portefeuille initialement prévue dans ce lot **a été livrée avec le Lot 1**.

**Le problème** : `kycLevel` est un champ mort. Écrit en dur à `1` (`KycServiceImpl:152`, `KycAutomationServiceImpl:197`) et **aucun code ne l'élève jamais** — or `SubscriptionCreationOperator:209` et `SubscriptionLifecycleOperator:355` s'en servent pour autoriser des abonnements. Le gating lit une constante.

Deux options, à trancher :

1. **Matrice « documents vérifiés → niveau »** — le niveau monte quand les pièces requises passent `VERIFIED`, et redescend à l'expiration.
2. **Retirer le champ** et le gating qui en dépend, si le besoin métier n'existe pas.

À traiter dans le même lot, deux manques KYC réels :

- **Pas de revue périodique** — `riskLevel` existe mais pas de `next_review_date` : un dossier `VERY_HIGH` validé une fois n'est jamais réexaminé. Le worker actuel ne réagit qu'à l'expiration d'un document.
- **Motifs de rejet non structurés** — `decisionComment` est du texte libre, donc ni statistiques de rejet, ni message client automatisable, ni piste d'appel.

**Non retenu pour l'instant** : criblage sanctions/PEP et liveness/selfie-match — dépendances externes, à arbitrer séparément.

**Estimation** : ~2 j

---

## 5. Lot 5 — Compléments facturation

- **Retenue à la source** (contexte OHADA / Congo) : pas de champ ni de règle, alors que `TaxRule` existe déjà pour la TVA.
- **Multi-devise réelle** : `exchangeRate` est un champ libre sur le document, rien ne l'alimente ni ne le valide. Il faut une table de taux datés + le figement du taux à l'émission.
- **Export comptable SYSCOHADA** (journal de ventes / FEC) — aucune sortie comptable structurée aujourd'hui.
- **Avoir → remboursement portefeuille** : le lien avoir ↔ crédit wallet n'existe pas, c'est pourtant le cas d'usage naturel avec un portefeuille.

**Estimation** : ~5 j

---

## 6. Décisions en attente (bloquent ou orientent)

| # | Sujet | Pourquoi ça remonte |
|---|---|---|
| D1 | **Factures sous-facturées par le bug des lignes exonérées** | Toute facture avec une ligne `taxable=false` a un `total_amount` trop bas en base. Les documents scellés sont immuables. Corriger de l'historique facturé est une décision métier — requête de détection ci-dessous |
| D2 | **Diagnostic RBAC prod** | Confirmer laquelle des 3 causes explique le 403 sur l'émission. V204 répare la cause la plus probable (rattachement désactivé) mais pas une permission désactivée dans le catalogue |
| D3 | **Déploiement** | V202 / V203 / V204 ne s'appliqueront qu'au prochain déploiement Heroku. Surveiller le `WARNING V202` sur les allocations en double |
| D4 | **`spring-boot-starter-security` dans `pom.xml`** | Avait été retiré dans l'arbre de travail ; restauré pour que ça compile. À confirmer que le retrait n'était pas volontaire |
| D5 | **`RenderPdfPreviewTest`** | Utilitaire de contrôle visuel PDF, désactivé par défaut. À garder ou retirer |

### Requête D1

```sql
SELECT d.document_number, d.status, d.total_amount,
       SUM(l.total_amount) AS total_recalcule
FROM billing_document d
JOIN billing_document_line l ON l.document_id = d.id
WHERE NOT l.taxable AND NOT COALESCE(l.optional, false)
GROUP BY d.id, d.document_number, d.status, d.total_amount
HAVING d.total_amount <> SUM(l.total_amount);
```

---

## 7. Dette technique repérée, non planifiée

| Sujet | Détail |
|---|---|
| `AdminApiAuthorizationManager` | `path.contains("/pay")` est testé avant les autres règles : `/billing/documents/X/payment-schedule` bascule sur `PAYMENT:PROCESS` au lieu d'une permission `BILLING` |
| `ResourcePricingRule.price` | Déclaré `Integer` alors que toute la chaîne est `BigDecimal(19,4)`, et la règle ne porte pas de devise. Toute tarification à décimales est tronquée à la frontière réservation → facture |
| Contrôle de devise portefeuille | `credit` / `debit` recopient `wallet.getCurrency()` sans vérifier la devise du montant source. Les appelants valident aujourd'hui, mais rien ne le garantit |
| Redis en local | Port 6379 dans une plage réservée Windows (6370–6469). `net stop winnat` en console admin, ou remapper sur 6380 |

---

## 8. Ordre recommandé

Le module **facturation** passe devant à la demande du 2026-08-27. Reprise ensuite dans cet ordre :

| Rang | Lot | Effort | Pourquoi ce rang |
|---|---|---|---|
| 1 | Lot 2 — Promotion sur réservation | ~3 j | Demande métier, et le correctif de sur-consommation est du même chantier |
| 2 | Lot 3 — Politique par abonné | ~3 j | Demande métier, sans risque de régression |
| 3 | Lot 4 — `kycLevel` | ~2 j | Ferme un gating qui lit aujourd'hui une constante |
| 4 | Lot 5 — Compléments facturation | ~5 j | Valeur métier, pas de risque technique |

Les décisions **D1 à D3** sont à traiter en parallèle : elles ne dépendent d'aucun lot et D1 touche des données de production.
