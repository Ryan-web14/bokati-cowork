# Bokati Cowork — Changelog Août 2026

Couvre les 14 commits de `02bd127` à `7842ff8`, sur la branche `feature/payment-facturation`.

Sept migrations : dev `V204`–`V211`, prod `V200`–`V207`. **Toute migration existe en double**, les
deux dossiers ayant des numérotations distinctes.

## Table des matières

1. [Portefeuille et paiements](#1-portefeuille-et-paiements)
2. [Facturation — corrections](#2-facturation--corrections)
3. [Facturation — nouvelles fonctionnalités](#3-facturation--nouvelles-fonctionnalités)
4. [Sécurité et RBAC](#4-sécurité-et-rbac)
5. [Communication et workers](#5-communication-et-workers)
6. [Maintenance — purge par membre](#6-maintenance--purge-par-membre)
7. [Récapitulatifs](#7-récapitulatifs)

---

## 1. Portefeuille et paiements

### 1.1 `WalletLedgerService` — propriétaire unique des mutations de solde

Le solde du portefeuille était mutable depuis plusieurs points d'entrée. Toutes les mutations
passent désormais par `WalletLedgerService`, qui expose `credit`, `debit`, `placeHold`,
`captureHold`, `releaseHold`, chacune déléguant à une méthode `apply` privée.

**Le verrou.** `@Lock(PESSIMISTIC_WRITE)` seul ne suffisait pas : Hibernate rendait l'instance
déjà chargée dans le contexte de persistance, avec ses valeurs d'origine. Le verrou était posé
mais le calcul se faisait sur un solde périmé.

```java
entityManager.flush();
WalletAccount managed = entityManager.find(WalletAccount.class, wallet.getId());
entityManager.refresh(managed, LockModeType.PESSIMISTIC_WRITE);
```

Le `flush` préalable couvre le cas d'un portefeuille créé dans la même transaction et pas encore
écrit en base.

À noter également : `@Lock` est **silencieusement ignoré par Spring Data sur une requête
native**. Les requêtes de verrouillage sont donc écrites en JPQL.

**L'invariant, préservé par construction :**

| Opération | `ledgerBalance` | `availableBalance` | `heldBalance` |
|---|---|---|---|
| `credit` | +X | +X | — |
| `debit` | −X | −X | — |
| `placeHold` | — | −X | +X |
| `captureHold` | −X | — | −X |
| `releaseHold` | — | +X | −X |

### 1.2 Idempotence des écritures

`wallet_ledger_entry.idempotency_key` avec index unique **partiel** (`WHERE key IS NOT NULL`), sur
le modèle de `email_delivery_log.dedup_key`.

La clé est un **paramètre explicite de l'appelant**, jamais dérivée automatiquement : un
remboursement partiel se répète légitimement sur une même transaction, et une clé déduite les
aurait fusionnés.

```java
try { return ledgerRepository.saveAndFlush(entry); }
catch (DataIntegrityViolationException ex) { return findReplay(key).orElseThrow(() -> ex); }
```

Le rattrapage sur violation d'unicité couvre la course : deux requêtes concurrentes passent le
contrôle de rejeu, une seule écrit, l'autre retrouve l'écriture d'origine.

### 1.3 Garde-fous de cohérence — `V206` / `V202`

- `wallet_account.version` (`@Version`) — verrou optimiste.
- `ck_wallet_account_non_negative` et `ck_wallet_account_balance_split`, **en `NOT VALID`** :
  la contrainte s'applique aux écritures nouvelles sans faire échouer le déploiement sur une
  ligne ancienne.
- `ux_wallet_account_owner_currency` — un portefeuille par propriétaire et devise.
- `ux_payment_allocation_txn_document`, posé **conditionnellement** : il n'existe pas d'équivalent
  `NOT VALID` pour un index unique, donc un bloc `DO $$` compte les doublons et émet un
  `RAISE WARNING` plutôt que de faire échouer le déploiement.

### 1.4 Réconciliation et rejeu

- `WalletReconciliationWorker` — contrôle périodique de l'invariant de solde.
- `WalletReconciliationChecker` — classe séparée. `@Transactional(REQUIRES_NEW)` sur une méthode
  appelée depuis la même classe ne passe pas par le proxy Spring : l'auto-appel aurait rejoint la
  transaction du worker au lieu d'en ouvrir une nouvelle.
- `PaymentAllocationRetryOutboxEventProcessor` — rejeu des allocations échouées via l'outbox.

---

## 2. Facturation — corrections

### 2.1 Total faussé par toute ligne exonérée — le plus grave

`BillingCalculationService` calculait le total du document **depuis la base taxable** :

```java
BigDecimal total = money(adjustedTaxable.add(adjustedTax));   // avant
```

Or `calculateTaxExcludedAmounts` force `taxableAmount = ZERO` sur une ligne non taxable. Toute
ligne exonérée entrait donc au total pour **zéro**.

Conséquences : **toute facture comportant une ligne exonérée était sous-facturée**, et un avoir —
dont la ligne d'ajustement est toujours `taxable = false` — ressortait à 0,00, ce qui faisait
échouer son application avec « Payment amount must be positive » et bloquait l'annulation d'une
facture scellée.

La part exonérée est maintenant suivie séparément, la remise document est répartie au prorata
entre part taxable et part exonérée, et le total additionne les trois composantes.

> **Historique non corrigé.** Les factures émises avant ce correctif conservent un
> `total_amount` sous-évalué, et les documents scellés sont immuables. Requête de détection dans
> `docs/a-realiser/plan-lots-restants.md`.

### 2.2 Facture annulée restant au statut `PAID`

L'annulation d'une facture scellée émet un avoir, appliqué via `applyPayment` — qui solde la
facture et la met à `PAID`. Une facture annulée ressortait donc comme encaissée.

Comme les agrégats comptables excluent `CANCELLED` et `VOIDED` **mais pas `PAID`**, son montant
restait compté en chiffre d'affaires. Les deux symptômes rapportés n'avaient qu'une cause.

`markCancelledAndArchived` repositionne le statut après application de l'avoir. Les montants ne
sont pas touchés : ils appartiennent à l'historique fiscal, et le trigger
`trg_billing_document_immutable` les protège de toute façon — il ne couvre ni `status`, ni
`paid_amount`, ni `balance_due`, ni les dates d'annulation et d'archivage.

La branche « aucun avoir nécessaire » (montant nul) se contentait d'archiver sans changer le
statut ; elle annule désormais aussi.

### 2.3 Règles d'annulation — avoir seulement si encaissement

Deux défauts distincts, rapportés depuis la production.

**Un avoir était émis même sans encaissement.** `cancelAndArchive` déclenchait un avoir dès que
le document était scellé, sans regarder ce qui avait été perçu. Or un avoir constate une créance
en faveur du client : sur une facture dont rien n'a été encaissé, il n'y a rien à créditer, et
l'émission d'un avoir à 0 encaissé crée un document comptable sans contrepartie.

La règle est désormais : avoir **si et seulement si** le document est une facture scellée **et**
`paidAmount > 0`. Sinon, annulation directe — datée, tracée, et sortant le document des agrégats.

> Une facture scellée annulée sans avoir ne laisse pas de document comptable de contrepassation.
> C'est acceptable tant que rien n'a été perçu : il n'y a aucun flux à contrepasser, et
> l'annulation reste horodatée et auditée. Si le contexte fiscal exige un avoir dans tous les cas,
> la condition tient en une ligne de `requiresCreditNoteToCancel`.

**Un avoir ne pouvait pas être annulé.** `cancelAndArchive` sur un avoir tentait d'émettre un
avoir d'avoir, et échouait sur `ensureCanPay` avec « Payments can only be allocated to invoices ».
Un avoir, un devis et une note de débit s'annulent maintenant directement.

`releaseCreditNoteBeforeCancellation` défait d'abord l'effet d'un avoir déjà consommé — sans quoi
la contrepartie resterait en place, donc un document annulé continuant de peser sur un solde :

- **imputé sur une facture** → l'imputation est retirée par `reversePayment`, sauf si la facture
  est elle-même annulée : elle ne doit pas ressortir du néant avec un solde rouvert ;
- **reversé au portefeuille** → refus explicite. Le solde a pu être dépensé depuis ; le reprendre
  silencieusement créerait un découvert. C'est une décision de gestion, pas une écriture
  technique.

### 2.4 Agrégats comptables ne filtrant pas le statut

| Requête | Défaut |
|---|---|
| `AnalyticsRepository.financial` | Aucun filtre de statut · montant facturé, encaissé, encours et TVA comptaient les annulées en entier |
| `FinancialReportRepository.financialKpis` | `total_paid`, `total_vat`, `total_additional_cent`, `total_discounts` sans filtre |
| `BillingDocumentRepository.statementTotals` | Sommait tous les documents sans exception |

Filtre ajouté sur `CANCELLED` et `VOIDED` **seulement** — le traitement de `DRAFT` diffère d'une
requête à l'autre et n'a pas été touché.

`statementTotals` ne filtrait pas non plus `document_type` : **les devis entraient dans le total
facturé du relevé client**, chiffre montré au client. Limité aux factures et proformas, comme
`findRecoverableDocuments`. Les avoirs restent pris en compte indirectement, par le montant payé
de la facture à laquelle ils s'imputent.

### 2.5 Ligne de facture — remise, catégorie, unité

- La remise apparaît sur la ligne du PDF, plus seulement dans le récapitulatif des totaux.
- `billing_document_line.category` (`V207` / `V203`), reprise du catalogue quand la ligne ne la
  précise pas, comme l'unité.
- Le badge de catégorie porte `white-space: nowrap` : le moteur PDF calcule mal la largeur
  *shrink-to-fit* d'un `inline-block` combinant `text-transform` et `letter-spacing`, et le badge
  se coupait sur deux lignes. Défaut invisible autrement qu'en rendant le PDF en image.

### 2.6 Correction du destinataire — `PATCH /documents/{n}/recipient`

Corrige les coordonnées imprimées sans changer le client propriétaire.

`ensureRecipientChangeAllowed` refuse `customerName`, `customerNiu` et `billingAddressJson` sur un
document validé. Le trigger d'immutabilité protège ces trois colonnes : sans ce contrôle amont,
la tentative remonte en `JpaSystemException` 500 au lieu d'un 400 explicite.

### 2.7 Verrou sur l'application de paiement

`lockedByNumber` — `flush`, `lockByDocumentNumber` en JPQL, puis
`refresh(PESSIMISTIC_WRITE)`. Utilisé par `applyPayment` et `reversePayment`, pour la même raison
qu'au § 1.1.

---

## 3. Facturation — nouvelles fonctionnalités

### 3.1 Simulation avant création

`POST /billing/documents/simulate` accepte le corps de la création et renvoie les totaux **sans
rien écrire ni consommer de numéro de séquence**.

`BillingCalculationService.calculate` faisait déjà tout le travail et ne touche pas la base :
l'endpoint est une façade.

La réponse détaille chaque ligne — brut, remise, **taux effectif recalculé depuis les montants
même quand la remise a été saisie en valeur**, net — et sépare `lineDiscountAmount` de
`documentDiscountAmount`, distinction que le calcul n'exposait pas. `documentDiscountAmount` se
déduit de l'écart entre la remise totale et la somme des remises de ligne.

### 3.2 Catalogue enrichi — `V209` / `V205`

Dix-huit colonnes facultatives sur `service_catalog_item`. L'API d'administration existait déjà
(`ServiceCatalogController`) ; seuls l'entité et les DTO ont été étendus.

**Résolution du plancher** — `ServiceCatalogItem.effectiveFloorPrice()` :

1. `floorPrice` s'il est saisi ;
2. sinon dérivé de `costPrice` et `minMarginRate` ;
3. sinon `costPrice` seul — on ne vend pas à perte ;
4. sinon aucun plancher.

La dérivation emploie le **taux de marque**, la marge rapportée au prix de vente :
`coût / (1 − taux)`. À 8 000 de coût et 20 % de marge minimale, le plancher vaut 10 000 — la marge
réalisée à ce prix est bien de 20 % du prix de vente. La convention inverse (marge rapportée au
coût) donnerait 9 600 ; le changement tient en une ligne de `effectiveFloorPrice()`.

La réponse expose `effectiveFloorPrice` **et** `floorPriceOrigin` (`EXPLICIT`,
`DERIVED_FROM_MARGIN`, `COST_PRICE`, `NONE`), pour qu'un refus puisse être expliqué et pas
seulement opposé.

**`clearFields`** sur la mise à jour : l'implémentation n'applique que les champs non nuls, donc
sans effacement explicite un plancher posé par erreur ne pourrait plus jamais être retiré par
l'API. Un nom de champ inconnu lève une erreur plutôt que d'être ignoré.

Refus à la saisie : plancher supérieur au prix de vente (article invendable), quantité minimale
supérieure à la maximale, valeurs hors énumération pour `discountPolicy` et `billingMode`.

### 3.3 Garde-fous de remise — `V210` / `V206`

`BillingDiscountGuard`, en **deux temps distincts** :

- `evaluate()` constate sans interrompre — la simulation, la création et la mise à jour le
  remontent ;
- `enforce()` refuse, et n'est appelé qu'à l'émission, le moment où le document engage.

**Trois décisions, chacune fermant un contournement :**

| Décision | Contournement fermé |
|---|---|
| Le plancher borne le prix **net**, remise déduite, pas le prix catalogue | Une remise suffirait à passer dessous sans déclencher le contrôle |
| Le taux effectif est recalculé depuis les montants | Une remise saisie en valeur échapperait à un plafond exprimé en taux |
| Le seuil global compte les **remises de ligne** | Éclater la remise sur les lignes contournerait le seuil document |

**Repli indépendant du catalogue.** Les limites par article ne couvrent que les lignes rattachées
à un article ; une ligne libre — le cas le plus courant en saisie manuelle — échappait à tout
contrôle par ligne. Par défaut une ligne conserve au moins **50 %** de son montant de base. Il
s'applique même à un article en politique `NONE` : celle-ci retire les limites *de l'article*,
elle ne lève pas la limite du système.

```yaml
bokati.billing.discount-guard:
  enabled: true
  default-min-net-rate: 50          # part minimale conservée par ligne
  min-line-net-amount:              # montant net minimal absolu
  max-document-discount-rate: 50    # remise totale maximale du document
  max-document-discount-amount:     # plafond en valeur
```

`enabled` ne gouverne que ces limites-ci. Celles du catalogue relèvent de la `discountPolicy` de
chaque article et **restent actives garde-fou coupé** — couper la configuration ne doit pas
désarmer un plancher posé délibérément.

**Contournement** — permission `BILLING:DISCOUNT_OVERRIDE`, créée et rattachée à `ADMIN` et
`SUPER_ADMIN` par la migration. Jamais un rôle codé en dur : un rôle en dur crée un droit
invisible de l'écran des rôles, impossible à accorder, à retirer ou à auditer — exactement le
défaut du § 4.1. Motif obligatoire, conservé avec son auteur et sa date, tracé au journal fiscal.

Un dépassement répond **400 et non 403** : ce n'est pas l'accès à la route qui est refusé mais
l'opération demandée.

### 3.4 Versionnage des documents — `V211` / `V207`

`billing_document_edit_history` existait depuis `V121` et était bien écrite à chaque
modification, mais **`snapshot_json` n'a jamais été renseignée** et `changed_by` valait la
constante `"SYSTEM"`. On savait qu'une modification avait eu lieu, jamais ce que le document
contenait avant, ni qui l'avait faite.

- L'état est archivé **avant** modification, pas après : c'est lui qui permet de reconstituer ce
  que le client avait sous les yeux. La version courante est le document lui-même.
- `version_number`, `sent_to_customer_at`, `change_summary`. L'horodatage de transmission
  distingue un brouillon retouché d'une proposition réellement envoyée — seules les versions
  transmises ont valeur probante.
- **Montants et dates archivés en texte.** Un `BigDecimal` relu depuis JSON revient en `Double`,
  et deux versions identiques ressortiraient différentes au diff sur une simple différence de
  représentation.
- Les listes de lignes sont comparées **dans leur ensemble**, pas ligne à ligne : un
  réordonnancement ou une suppression déplace les indices, et une comparaison positionnelle
  rapporterait des changements qui n'en sont pas.
- La migration **numérote rétroactivement** l'historique existant, par document et dans l'ordre
  chronologique, pour que la suite continue une série cohérente. L'index unique n'est posé que si
  aucun doublon n'existe, sinon `RAISE WARNING`.

Un document scellé n'est pas versionné : il est immuable, sa correction passe par avoir ou
facture rectificative.

### 3.5 Avoir reversé au portefeuille

`POST /billing/credit-notes/{n}/refund-to-wallet`, pour le cas où il n'y a plus rien à imputer
mais où la créance doit rester acquise au client.

`WalletEntryType.REFUND` et la clé d'idempotence existaient déjà (§ 1.2) ; seul le chemin métier
manquait. Clé `CREDIT_NOTE_REFUND:<numéro>`.

Deux lignes de défense contre le double crédit : le contrôle de statut (« déjà consommé »)
d'abord, la clé d'idempotence ensuite — pour deux requêtes concurrentes qui liraient le statut
avant que l'une n'écrive.

Les portefeuilles étant uniques par propriétaire et devise depuis `V206`, demander celui de la
devise de l'avoir donne le bon compte, **sans conversion implicite**.

---

## 4. Sécurité et RBAC

### 4.1 Permissions fantômes dans le résolveur de chemins

`AdminApiAuthorizationManager` résolvait `DELETE` en `MODULE:DELETE` pour tous les modules. Or
seuls `CLIENT`, `INVENTORY` et `RESOURCE` portent réellement une permission `DELETE` au
référentiel. `BILLING:DELETE`, `BOOKING:DELETE` et `SUBSCRIPTION:DELETE` **n'existent pas** — un
administrateur recevait donc un 403 sur l'annulation de facture, quelle que soit sa configuration
de rôles.

```java
private static final Set<String> MODULES_WITH_DELETE_PERMISSION = Set.of("CLIENT","INVENTORY","RESOURCE");
```

Hors de ces trois modules, `DELETE` retombe sur `UPDATE`. Pour la facturation, `DELETE` est une
annulation : il résout `BILLING:CANCEL`, pas `BILLING:UPDATE` — annuler est plus lourd que
corriger.

### 4.2 403 diagnosticable

La permission exigée est déposée en attribut de requête et reprise dans le corps du 403, sous
`requiredPermission`, et journalisée avec la cause. Sans elle, un 403 en production ne permet pas
de distinguer un droit réellement absent d'une permission mal résolue par le mapping de chemins.

### 4.3 Réalignement du référentiel — `V208` / `V204`

`V148` puis `V150` n'inséraient les `role_permission` que si la ligne était **absente**
(`NOT EXISTS`). Une ligne présente mais `is_active = false` n'était donc jamais réparée :
`resolveAuthorities` filtre sur `is_active`, la permission n'arrivait pas dans les autorités, et
l'administrateur recevait un 403 sur une action qu'il est censé pouvoir faire.

Second trou : c'était un instantané. Toute permission créée après le passage de `V150` n'était
accordée à personne.

La migration réactive les rôles, réactive les rattachements désactivés, accorde les permissions
créées depuis, et **signale sans les réactiver** les permissions désactivées au catalogue —
désactiver une entrée est la façon prévue de retirer un droit, la rallumer automatiquement
réintroduirait un droit retiré exprès. Un second bloc vérifie que toutes les permissions attendues
par le résolveur existent, garde-fou au prochain ajout de règle.

`docs/a-realiser/diagnostic-rbac.sql` distingue en une passe les trois causes possibles d'un 403 :
permission absente du référentiel, permission désactivée au catalogue, rattachement désactivé.

### 4.4 Routes mal classées

- **`/admin/notifications`** tombait dans le repli `/admin/` et exigeait donc `SYSTEM:SETTINGS`.
  Afficher son compteur de non-lus demandait le droit de configuration système. Aligné sur
  `ADMIN:ACCESS`, comme `/notifications`.
- **`/admin/maintenance` en lecture** — prévisualisation de purge et liste des candidats — passe
  sur `ADMIN:ACCESS`. Exiger `SYSTEM:SETTINGS` empêchait de vérifier ce qu'une suppression
  emporterait avant de la lancer, ce qui est précisément la garantie que cet appel doit offrir.
  Les verbes destructeurs restent sur `SYSTEM:SETTINGS`.
- `/auth/me` et `/auth/logout` explicitement `authenticated()`.

---

## 5. Communication et workers

### 5.1 Déduplication des envois — `V204` / `V200`

`EmailDedupKeyFactory` : un courriel portant une référence métier (facture, contrat, dossier KYC)
est dédupliqué sur cette référence, fenêtre longue ; les autres retombent sur destinataire +
sujet, fenêtre courte.

```yaml
bokati.email.dedup:
  enabled: true
  business-window-seconds: 86400
  generic-window-seconds: 3600
```

### 5.2 Marqueurs « déjà notifié » — `V205` / `V201`

Quatre workers resélectionnaient les mêmes lignes à chaque passage, rien n'enregistrant que le
courriel était parti. Le rappel repartait tant que la ligne restait sélectionnée.

| Worker | Colonne ajoutée | Symptôme |
|---|---|---|
| Expiration d'intention de paiement | `payment_intent.expiry_alert_sent_at` | Fenêtre de 2 h balayée toutes les 30 min → 4 courriels identiques |
| Pré-expiration de document | `document.expiry_reminder_sent_days` | Worker horaire, condition `expiry_date = today + N` → 24 envois |
| Rappel de paiement | `billing_document.payment_reminder_sent_at`, `payment_reminder_count` | Facture `OVERDUE` jusqu'au paiement → renvoi quotidien, PDF compris |
| Escalade SLA support | `support_ticket.sla_escalation_sent_at` | Idem |

### 5.3 Limite Microsoft Graph

`GRAPH_RATE_LIMIT` ramené de 800 à **25** par minute. Exchange Online plafonne une boîte autour de
30 messages/minute, et le limiteur est **par instance** : le plafond effectif est cette valeur
multipliée par le nombre de dynos.

### 5.4 Relais STOMP CloudAMQP

L'hôte, les identifiants, le vhost et le TLS se dérivent de `CLOUDAMQP_URL`. L'add-on Heroku ne
fournit que cette variable ; les paramètres séparés faisaient retomber le vhost sur `/`, ce qui
est faux sur les plans mutualisés.

---

## 6. Maintenance — purge par membre

`DataPurgeService` vide la base entière (`TRUNCATE` de toutes les tables non référentielles) :
inutilisable pour retirer un jeu de test d'une production contenant aussi des données réelles.

`MemberPurgeService` supprime un membre nommé et tout ce qui s'y rattache — compte utilisateur,
factures, portefeuille, contrats, abonnements, réservations. Les montants disparaissent des
rapports du même coup : la comptabilité est **dérivée** de `billing_document`,
`payment_transaction`, `booking` et `subscription`, il n'existe pas de grand livre séparé à
rectifier.

**Deux mécanismes de rattachement**, tous deux couverts :

- **Par code métier** — `owner_code`, `customer_code`, `subscriber_code`, `party_code`… Les
  colonnes candidates sont découvertes dans `information_schema` **en filtrant sur le type de
  donnée caractère**, ce qui écarte les homonymes numériques : `member_id` porte le code dans la
  table `member` mais peut être une clé étrangère entière ailleurs.
- **Par clé étrangère** — le graphe est lu dans `pg_constraint` à l'exécution et parcouru en
  profondeur d'abord. Aucune liste de tables figée : une table ajoutée plus tard est prise en
  compte sans toucher au code.

Les triggers d'immutabilité fiscale sont neutralisés pour la durée de la transaction et
systématiquement remis, y compris en cas d'échec.

**Garde-fous** — interrupteur `bokati.maintenance.member-purge.enabled` ; un membre rattaché à un
compte administrateur est refusé ; un client partagé par plusieurs membres est conservé ;
**pas de purge par date** : `purge-candidates` liste, les codes retenus sont ensuite passés
explicitement, de sorte qu'une date mal saisie ne puisse pas vider la base.

`DELETE /members/{id}?purge=true` déclenche la purge. **L'archivage reste le défaut** de ce point
d'entrée : c'est celui que l'interface appelle pour retirer un membre ordinaire, et une
suppression définitive déclenchée par ce bouton détruirait la facturation d'un client réel.

### 6.1 « Member not found » sur un membre déjà archivé

`MemberServiceImpl.delete` cherchait avec `findByMemberIdAndDeletedFalse`. Un membre déjà archivé
n'était donc plus trouvé et l'API répondait 404 « Member not found » alors que la ligne existe :
le message désignait la mauvaise cause, et l'archivage n'était pas rejouable.

`findByMemberIdIncludingArchived` ignore le drapeau `deleted` et compare le code sans tenir compte
de la casse ni des espaces de bord. La purge n'était pas concernée — elle interroge la table
directement.

### 6.2 Filtre de statut KYC

`GET /kyc/cases` acceptait un `KycCaseStatus` typé : une valeur inconnue produisait un 500. Le
paramètre est reçu en chaîne, les alias de l'interface sont tolérés (`PENDING_REVIEW` →
`SUBMITTED`, `IN_REVIEW` → `UNDER_REVIEW`), et une valeur inconnue donne un 400.

---

## 7. Récapitulatifs

### 7.1 Migrations

| Dev | Prod | Contenu |
|---|---|---|
| `V204` | `V200` | Clé de déduplication des envois |
| `V205` | `V201` | Marqueurs « déjà notifié » des workers |
| `V206` | `V202` | Concurrence portefeuille et allocations |
| `V207` | `V203` | Catégorie de ligne de facture |
| `V208` | `V204` | Réalignement des droits `ADMIN` / `SUPER_ADMIN` |
| `V209` | `V205` | Enrichissement du catalogue |
| `V210` | `V206` | Traçage de dérogation + permission `BILLING_DISCOUNT_OVERRIDE` |
| `V211` | `V207` | Versionnage des documents |

Appliquées et vérifiées en dev le 2026-08-30, sans avertissement.

### 7.2 Nouveaux endpoints

```
POST   /billing/documents/simulate
DELETE /billing/documents/{n}
PATCH  /billing/documents/{n}/recipient
PATCH  /billing/documents/{n}/issue?discountOverrideReason=
GET    /billing/documents/{n}/versions
GET    /billing/documents/{n}/versions/{v}
GET    /billing/documents/{n}/versions/{a}/diff/{b}
POST   /billing/credit-notes/{n}/refund-to-wallet

GET    /admin/maintenance/members/purge-candidates
GET    /admin/maintenance/members/{code}/purge-preview
DELETE /admin/maintenance/members/{code}
POST   /admin/maintenance/members/purge-batch
DELETE /members/{id}?purge=true
```

### 7.3 Nouvelle permission

`BILLING_DISCOUNT_OVERRIDE` (`BILLING` / `DISCOUNT_OVERRIDE`), rattachée à `ADMIN` et
`SUPER_ADMIN` par `V210` / `V206`.

### 7.4 Configuration ajoutée

| Variable | Défaut | Effet |
|---|---|---|
| `EMAIL_DEDUP_ENABLED` | `true` | Déduplication des envois |
| `EMAIL_DEDUP_BUSINESS_WINDOW_SECONDS` | `86400` | Fenêtre sur référence métier |
| `EMAIL_DEDUP_GENERIC_WINDOW_SECONDS` | `3600` | Fenêtre destinataire + sujet |
| `GRAPH_RATE_LIMIT` | `25` | Messages/minute par instance |
| `BILLING_DISCOUNT_GUARD_ENABLED` | `true` | Limites de remise de configuration |
| `BILLING_MIN_LINE_NET_RATE` | `50` | Part minimale conservée par ligne |
| `BILLING_MIN_LINE_NET_AMOUNT` | vide | Montant net minimal par ligne |
| `BILLING_MAX_DOCUMENT_DISCOUNT_RATE` | `50` | Remise totale maximale |
| `BILLING_MAX_DOCUMENT_DISCOUNT_AMOUNT` | vide | Plafond de remise en valeur |
| `MEMBER_PURGE_ENABLED` | `true` | Autorise la purge effective |
| `MEMBER_PURGE_MAX_BATCH_SIZE` | `25` | Taille de lot maximale |

### 7.5 Couverture de tests

178 tests, 0 échec. Classes ajoutées : `WalletLedgerServiceTest`, `PaymentAllocationServiceTest`,
`BillingCalculationServiceTest`, `BillingDocumentRecipientUpdateTest`, `BillingDiscountGuardTest`,
`BillingDocumentVersionServiceTest`, `BillingDocumentSimulationTest`, `CreditNoteWalletRefundTest`,
`ServiceCatalogEnrichmentTest`, `ServiceCatalogItemFloorPriceTest`,
`AdminApiAuthorizationManagerTest`, `EmailDedupKeyFactoryTest`, `DefaultEmailSenderDedupTest`.

### 7.6 Points ouverts

- **Historique sous-facturé** (§ 2.1) — non corrigé, documents scellés immuables.
- **`statementTotals`** additionne des documents de types différents ; la restriction aux factures
  et proformas corrige le symptôme, la structure de la requête mériterait d'être revue.
- **`MemberPurgeService`** — validé bout en bout en dev, jamais exécuté en production.
- **Convention de marge** (§ 3.2) — taux de marque retenu ; le changement tient en une ligne.

Documentation d'intégration : `docs/frontend/facturation-nouvelles-fonctionnalites.md`.
