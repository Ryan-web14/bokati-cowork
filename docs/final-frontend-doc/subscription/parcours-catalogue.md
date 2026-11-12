# subscription : parcours du catalogue

> Partie de [README.md](README.md). Routes exactes et champs : [routes.md](routes.md). Règles communes : [README racine](../README.md).

Tous ces écrans sont du back-office. Les droits exigés sont indiqués sur chaque route dans les fichiers `routes/`.

## Définitions de droits

Une définition de droit décrit ce qu'un plan ou un pass accorde : un type (`ACCESS`, `CREDIT`, `TIME`…), une unité (`HOUR`, `DAY`, `CREDIT`, `BOOKING`…), un mode de consommation (`CHECK_ONLY`, `RESERVABLE`, `CONSUMABLE`, `RESERVE_THEN_CONSUME`) et une périodicité de remise à zéro (`resetPolicy`). `resourceTypeCode` et `resourceGroupCode` la rattachent aux ressources réservables (module [ressource](../ressource/README.md)).

1. Lister : `GET /sni/api/v1/subscriptions/entitlement-definitions` avec les filtres utiles (`query`, `unit`, `active`…), paginé ([route](routes/SubscriptionCatalogController.md#r-get-sni-api-v1-subscriptions-entitlement-definitions)). Pour une saisie assistée, `GET /sni/api/v1/subscriptions/entitlement-definitions/search/basic?query=...` rend une liste simple.
2. Créer : `POST /sni/api/v1/subscriptions/entitlement-definitions` avec `name`, `entitlementType`, `unit`, `consumptionMode`, `resetPolicy` (obligatoires). Le `code` est **généré** par le serveur et revient dans la réponse (`201`).
3. Modifier : `PUT /sni/api/v1/subscriptions/entitlement-definitions/{code}` avec **le même modèle complet** que la création (les champs obligatoires doivent être renvoyés).
4. Consulter : `GET /sni/api/v1/subscriptions/entitlement-definitions/{code}`.

Il n'y a pas de suppression ; `active: false` retire la définition des choix.

## Plans d'abonnement et versions

Un plan est une vitrine (nom, type, public, ordre, visibilité). Son contenu vendable (prix, avantages, droits) est porté par ses **versions**. Une version n'est **pas modifiable** après sa création : pour changer un prix, créer une nouvelle version puis la publier.

### Créer un plan

1. `POST /sni/api/v1/subscriptions/plans` avec `name`, `planType`, `targetAudience` (obligatoires), `description`, `visible`, `sortOrder` ([route](routes/SubscriptionCatalogController.md#r-post-sni-api-v1-subscriptions-plans)). Réponse `201` `PlanResponse` : `code` généré (préfixe `PLN`), `status: DRAFT`.
2. L'écran ouvre la fiche du plan sur l'onglet « Versions », vide.

### Créer une version

1. `POST /sni/api/v1/subscriptions/plans/{planCode}/versions` ([route](routes/SubscriptionCatalogController.md#r-post-sni-api-v1-subscriptions-plans-plancode-versions)) avec :
   - `name` (obligatoire), `description`, `effectiveFrom`, `effectiveTo`, `termsJson` ;
   - `prices[]` : une ligne par cycle (`billingCycle`, `currency`, `amount` ≥ 0 obligatoires ; `setupFee`, `depositAmount` à 0 par défaut ; `taxIncluded` vrai par défaut ; `commitmentMonths` à 0 par défaut). Un prix avec `commitmentMonths` > 0 pose automatiquement un engagement à chaque souscription ;
   - `benefits[]` : lignes d'affichage (`title` obligatoire, `category` par défaut `CUSTOM`, `included` vrai par défaut) ;
   - `entitlements[]` : `entitlementCode` d'une définition existante (`404` sinon), `quantity` obligatoire sauf `unlimited: true` (`400`), `rolloverAllowed`, `rolloverLimit` (report du reliquat), `validForDays` (durée propre du droit, sinon la période), `priority` (100 par défaut, la plus basse est consommée d'abord).
2. Réponse `201` `PlanVersionResponse` : `id` (texte, à garder pour la suite), `versionNumber`, `status: DRAFT`, prix, avantages, droits.

### Publier une version

1. `PATCH /sni/api/v1/subscriptions/plans/{planCode}/versions/{versionId}/publish`, avec l'`id` texte de la version ([route](routes/SubscriptionCatalogController.md#r-patch-sni-api-v1-subscriptions-plans-plancode-versions-versionid-publish)).
2. Le serveur archive la version de catalogue active précédente (avec `effectiveTo` = aujourd'hui), passe celle-ci `ACTIVE` (avec `effectiveFrom` = aujourd'hui si vide), et passe le plan `ACTIVE` avec `visible: true`, même s'il avait été masqué.
3. Les abonnements existants **restent sur leur ancienne version** ; pour garder leur prix lors d'une hausse, voir la [protection tarifaire](parcours-derivations.md#protection-tarifaire-après-une-hausse-de-catalogue). Les nouvelles souscriptions prennent la version active.

Aucun contrôle de statut : republier une version archivée la réactive et archive la version courante.

### Consulter, modifier, retirer

- Liste : `GET /sni/api/v1/subscriptions/plans` (filtres `code`, `name`, `planType`, `targetAudience`, `status`, `visible`). Chaque plan porte `activeVersion` et `versions`.
- Fiche : `GET /sni/api/v1/subscriptions/plans/{planCode}` ; versions : `GET .../{planCode}/versions` (versions de catalogue seulement, les versions privées des dérivations n'y figurent pas) ; une version : `GET .../{planCode}/versions/{versionId}`.
- Modifier la vitrine : `PUT /sni/api/v1/subscriptions/plans/{planCode}` (`name`, `description`, `planType`, `targetAudience`, `visible`, `sortOrder`, tous facultatifs).
- Retirer : `DELETE /sni/api/v1/subscriptions/plans/{planCode}`. Un plan sans version est supprimé ; un plan avec versions est seulement `ARCHIVED` et masqué. Réponse `204`.

Le niveau KYC exigé par un plan (`requiredKycLevel`) ne se règle ni ne se lit par l'API (voir [Écarts](README.md#écarts-relevés)).

## Plans de pass

Même logique qu'un plan d'abonnement, mais chaque version se désigne par un **code lisible** `versionCode` = `{planCode}-V{n}`, insensible à la casse. Les réponses n'exposent aucun identifiant technique : `id` recopie le code. Toutes ces routes exigent `ADMIN:ACCESS` et `PASS_PLAN:READ` ou `PASS_PLAN:WRITE`.

### Créer le plan et sa version

1. `POST /sni/api/v1/pass-plans` avec `name`, `passType`, `targetAudience` (obligatoires), `visible` (faux par défaut), `sortOrder`, `requiredKycLevel` (1 par défaut, aucune exigence en dessous de 2) ([route](routes/PassPlanController.md#r-post-sni-api-v1-pass-plans)). Réponse `201`, `code` généré (préfixe `PPL`), `status: DRAFT`.
2. `POST /sni/api/v1/pass-plans/{planCode}/versions` avec `name`, `duration`, `durationUnit` (obligatoires), `maxUses` (vide = illimité), `autoRenewable` (faux par défaut), `requiredKycLevel`, `effectiveFrom`, `effectiveTo`, `termsJson`. Réponse `201` avec `versionCode`, `versionNumber`, `status: DRAFT`.
3. Prix : `POST /sni/api/v1/pass-plans/versions/{versionCode}/prices` avec `currency`, `amount` (obligatoires), `setupFee`, `depositAmount`, `taxIncluded` (vrai par défaut), `taxCode`. Un seul prix par devise : renvoyer la même devise **remplace** le prix. Liste : `GET .../versions/{versionCode}/prices`.
4. Droits : `POST /sni/api/v1/pass-plans/versions/{versionCode}/entitlements` avec `entitlementCode` (définition existante, `404` sinon), `quantity` ou `unlimited`, `validForDays`, `priority` (100 par défaut). Retirer : `DELETE .../versions/{versionCode}/entitlements/{entitlementCode}` (`204`). Liste : `GET .../versions/{versionCode}/entitlements`.
5. Publier : `PATCH /sni/api/v1/pass-plans/{planCode}/versions/{versionCode}/publish` puis `PATCH /sni/api/v1/pass-plans/{planCode}/publish`. Les deux n'acceptent que `DRAFT` (`400` `Only DRAFT ... can be published`). Les deux doivent être `ACTIVE` pour vendre.

La vente sans `versionCode` prend la version `ACTIVE` au plus grand numéro ; publier une nouvelle version **n'archive pas** les précédentes.

### Gérer

- Liste : `GET /sni/api/v1/pass-plans` (`passType`, `status`, `searchText` sur nom, code et description).
- Fiche : `GET /sni/api/v1/pass-plans/{planCode}` ; versions : `GET .../{planCode}/versions` (plus récente d'abord).
- Modifier : `PATCH /sni/api/v1/pass-plans/{planCode}` (`name`, `description`, `visible`, `sortOrder`, `requiredKycLevel`). Le type et le public ne se modifient pas.
- Archiver : `PATCH /sni/api/v1/pass-plans/{planCode}/archive` (tout statut). Un plan archivé ne se vend plus.

Prix et droits d'une version publiée restent modifiables par ces routes : l'écran devrait les verrouiller une fois la version publiée.

## Politique d'abonnement

Une seule politique, lue par tout le cycle de vie (prorata, préavis, pénalité de rupture, gel, tolérance, expiration, validité des devis). Deux accès :

- **Dans ce module** : `GET /sni/api/v1/subscriptions/lifecycle/policy` (renvoie l'entité, avec un `id` 64 bits à ignorer) et `PUT /sni/api/v1/subscriptions/lifecycle/policy` (réservé à `ROLE_ADMIN` et `ROLE_SUPER_ADMIN`) ([route](routes/SubscriptionLifecycleController.md#r-put-sni-api-v1-subscriptions-lifecycle-policy)). Tous les champs du corps sont facultatifs : seuls ceux envoyés changent.
- **Dans les paramètres** : les 13 réglages apparaissent dans le domaine `SUBSCRIPTIONS` de `GET /sni/api/v1/admin/settings` sous les clés `subscription.policy.*` et se modifient un par un par `PUT /sni/api/v1/admin/settings/{key}` (droit `SYSTEM:SETTINGS`, journalisé). Voir [parametres](../parametres/README.md). Effet immédiat.

Contrôles : jours ≥ 0, pourcentages entre 0 et 100 (`400`), validité d'un devis ≥ 1 jour, formule `FIXED_FEE` exige un montant fixe (`400` `Une pénalité fixe demande un montant`). Dans les paramètres, vider le montant fixe le supprime.

Signification des champs pour l'écran :

| Champ | Effet |
| --- | --- |
| `prorationPolicy` | `DAILY` au jour près, `MONTH_STARTED` tout mois entamé est dû, `NONE` pas de prorata (changement de plan sans facture ni avoir). |
| `defaultNoticeDays` | Préavis de résiliation par défaut. |
| `earlyTerminationFormula`, `earlyTerminationPercent`, `earlyTerminationFixedFee` | Pénalité de rupture d'engagement par défaut : `NONE`, `FIXED_FEE`, `PERCENT_OF_REMAINING` (pourcentage des mois restants), `REMAINING_PERIODS` (tous les mois restants). |
| `freezeMaxPerYear`, `freezeMaxDays`, `freezeFeePercent` | Bornes et part facturée d'un gel. |
| `freezeNoticeDays` | Délai de prévenance d'un gel, appliqué seulement par le portail (le back-office ne le contrôle pas). |
| `gracePeriodDays` | Jours d'impayé avant l'entrée en tolérance. |
| `suspensionAfterGraceDays` | Jours de tolérance avant suspension. |
| `renewalExpiryDays` | Jours après la fin de période avant `EXPIRED`. |
| `quoteValidityDays` | Validité par défaut d'un devis. |

## Règle de visa et prix plancher

Encadrent les tarifs négociés ([parcours-derivations.md](parcours-derivations.md)).

1. Lire : `GET /sni/api/v1/subscriptions/derivations/policy/approval-rule` (`404` `Aucune règle de visa active` s'il n'y en a pas).
2. Modifier (ADMIN ou SUPER_ADMIN) : `PUT .../policy/approval-rule` avec `maxDiscountPercentWithoutApproval` (au-delà, visa requis), `maxImpactWithoutApproval` (montant d'écart au-delà duquel un visa est requis ; négatif pour le retirer), `maxDiscountPercentAllowed` (au-delà, refus même visé ; pas sous le seuil de visa). Pourcentages entre 0 et 100 (`400`).
3. Prix plancher d'une version de catalogue (ADMIN ou SUPER_ADMIN) : `PUT /sni/api/v1/subscriptions/derivations/policy/plan-versions/{versionId}/floor-price` avec `{ "floorPrice": 50000 }` ; corps vide ou `floorPrice: null` le retire. Refusé sur une version privée ou avec un montant négatif (`400`). Aucune dérivation ni devis ne peut descendre sous ce prix.

## Politiques de dépassement

Ce qui se passe quand un usage dépasse le solde d'un droit d'abonnement (voir [parcours-droits-et-consommation.md](parcours-droits-et-consommation.md#dépassements)).

1. Lister : `GET /sni/api/v1/subscription-overage/policies?planVersionId=...&entitlementCode=...`.
2. Créer : `POST /sni/api/v1/subscription-overage/policies` avec `planVersionId` (id numérique de la version de plan d'abonnement), `entitlementCode`, `mode` (`BLOCK` refuse, `BILLABLE` facture au-delà de `freeQuantity` à `unitPrice` en `currency`, `ALLOW_UNBILLED` laisse passer sans facturer), `active`. Version ou définition inconnue : `404`.

Pas de modification ni de suppression par l'API.
