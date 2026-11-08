# subscription : fonctionnement

> Rédigé d'après le code. Routes exactes, droits et champs JSON : [routes.md](routes.md) et `routes/`. Règles communes : [README racine](../README.md).

## En bref

Le module vend et fait vivre ce que le coworking facture dans la durée : les **abonnements** (sur des plans versionnés, avec engagement, gel, tolérance de paiement, prélèvement, résiliation par préavis, renouvellement) et les **pass** (titres à durée ou à nombre d'utilisations, avec QR code, transfert et contrôle d'accès). Il porte aussi les **droits** (quotas consommables par les réservations), les **promotions, coupons et parrainages**, et les **tarifs négociés** (dérivations).

Presque tout est **back-office** (165 routes sur 170). Le portail membre n'a ici que 5 routes de prix et de panier (`/sni/api/v1/client/cart/...`, `/client/catalogue/prices`, `/client/promotions/available`) ; ses écrans d'abonnement et de pass sont dans le module [portal](../portal/README.md), qui appelle les mêmes services.

Il travaille avec [billing](../billing/README.md) (chaque somme due devient une facture), [payment](../payment/README.md) (un paiement réglé active l'abonnement ou le pass, ou fait prendre effet le renouvellement), [contract](../contract/README.md) (un contrat signé par le système à l'activation, prolongé par avenant à chaque renouvellement), [document](../document/README.md) (niveau KYC exigé par un plan), [booking](../booking/README.md) (consommation des droits) et [parametres](../parametres/README.md) (la politique d'abonnement y est aussi modifiable).

## Notions

| Objet | Identifiant exposé | Ce que c'est | Liens |
| --- | --- | --- | --- |
| Plan d'abonnement | `code` (préfixe `PLN`, généré) | Une offre du catalogue (type, public visé, visibilité). | a des versions |
| Version de plan d'abonnement | `id` **numérique** renvoyé en texte dans `PlanVersionResponse.id`, plus `versionNumber` | Le contenu daté d'un plan : prix par cycle, avantages affichés, droits accordés. Une seule version de catalogue `ACTIVE` par plan. | porte `prices`, `benefits`, `entitlements` |
| Version privée | même `id` numérique | Copie d'une version de catalogue réservée à un abonnement, créée par une dérivation. N'apparaît pas dans les listes de versions du plan. | `scope = SUBSCRIPTION` |
| Définition de droit | `code` (généré) | Ce qui se consomme ou se vérifie : heures de salle, jours d'accès, crédits… avec unité et mode de consommation. | utilisée par les versions de plan et de pass |
| Abonnement | `subscriptionNumber` | Un souscripteur (`MEMBER`, `CUSTOMER` ou `BUSINESS_ENTITY`, désigné par `subscriberCode`) sur une version de plan, avec un cycle de facturation et une période courante. | facture, contrat (`contractCode`), droits, engagement, mandat, gels, préavis |
| Droit accordé (grant) | `grantNumber` | Un solde de droit ouvert pour une période (quantité accordée, restante, ou illimité). | source `SUBSCRIPTION`, `PASS` ou `ROLLOVER` |
| Plan de pass | `code` (préfixe `PPL`), recopié dans `id` | L'offre de pass : type, public, niveau KYC. | a des versions |
| Version de plan de pass | `versionCode` lisible `{planCode}-V{n}`, recopié dans `id` | Durée, nombre d'utilisations, renouvelable ou non, prix par devise, droits. | |
| Pass | `passNumber` | Un titre détenu par un titulaire (`ownerType`, `ownerCode`), valable entre `validFrom` et `validUntil`, avec `maxUses`. | supports (QR), usages, transferts |
| Devis | `quoteNumber` (préfixe `QTE`) | Une proposition d'abonnement figée (plan, cycle, prix proposé, engagement, validité). | devient un abonnement à l'acceptation |
| Dérivation | `derivationCode` (préfixe `DRV`) | Un tarif ou un contenu négocié pour un abonnement : version privée, écarts chiffrés, visa éventuel. | `batchCode` pour les traitements en lot |
| Préavis de résiliation | `terminationCode` (préfixe `TRM`) | Une demande de résiliation avec date d'effet, frais de rupture et liste de sortie (`itemCode`). | |
| Engagement | `subscriptionNumber` | Durée minimale en mois et formule de pénalité de rupture. Un seul par abonnement. | |
| Mandat de prélèvement | `mandateCode` (préfixe `MDT`) | Accord de prélever les échéances sur un portefeuille (`walletNumber`). | tentatives par facture |
| Gel | aucun identifiant | Une pause encadrée par la politique (quota annuel, durée, part facturée). | créé par la route de pause |
| Demande de changement | `changeNumber` (préfixe `SCH`) | Montée ou descente de version de plan, avec prorata. | |
| Option (add-on) | `id` **numérique** uniquement | Une version de plan ajoutée à un abonnement, facturée à part. | |
| Siège | `memberCode` dans l'abonnement | Un membre rattaché à un abonnement d'entreprise. | |
| Promotion | `code` | Une campagne de remise. | coupons (`code`, `batchCode`), bénéficiaires |
| Parrainage | `referralNumber`, lien `linkCode`, programme `programCode` | Filleul inscrit par le lien d'un parrain. | |
| Élément facturable | `billableNumber` | Une somme due, transmise à la facturation. | |

Les identifiants numériques (`id` des versions de plan d'abonnement, des options, des politiques de dépassement) relèvent de la règle des [grands nombres](../README.md#identifiants--attention-aux-grands-nombres) : les garder en texte. Les préfixes cités viennent des définitions de séquences des migrations ; traiter tous les codes comme opaques.

Une seule politique d'abonnement (`SubscriptionPolicy`) règle tout le cycle de vie ; ses valeurs par défaut, en l'absence de ligne en base, sont dans `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/model/SubscriptionPolicy.java` : prorata `DAILY`, préavis 30 jours, rupture `PERCENT_OF_REMAINING` à 50 %, 2 gels par an de 60 jours maximum sans frais, tolérance 1 jour, suspension 3 jours après l'entrée en tolérance, expiration 7 jours après la fin de période, devis valable 30 jours.

## Statuts et cycle de vie

### Abonnement (`SubscriptionStatus`)

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `PENDING_ACTIVATION` | Créé, la première facture attend son paiement. | `POST /sni/api/v1/subscriptions`, acceptation d'un devis | `ACTIVE` (paiement de la facture, `PATCH .../activate`, ou création à 0 ou avec `autoActivate: true`), `PENDING_DOCUMENTS`, `CANCELLED` |
| `PENDING_DOCUMENTS` | L'activation a été tentée mais le niveau KYC exigé par le plan n'est pas atteint. | activation | `ACTIVE` dès que les pièces sont validées (worker horaire) |
| `ACTIVE` | En cours, droits ouverts. | activation, reprise, fin de tolérance, renouvellement payé, retrait de préavis | `GRACE_PERIOD`, `PAUSED`, `SUSPENDED`, `PENDING_TERMINATION`, `CANCELLED` |
| `GRACE_PERIOD` | Échéance impayée : droits encore ouverts, l'abonné est prévenu. | worker : facture de renouvellement impayée depuis plus de `gracePeriodDays`, ou période finie sans paiement ; échec de prélèvement | `ACTIVE` (paiement, prélèvement réussi, `POST .../grace/exit`), `SUSPENDED` (après `suspensionAfterGraceDays`), `EXPIRED` |
| `PAUSED` | Gelé jusqu'à une date. | `PATCH .../pause` | `ACTIVE` par `PATCH .../resume` uniquement (aucun automatisme) |
| `SUSPENDED` | Droits coupés. | `PATCH .../suspend`, worker en fin de tolérance | `ACTIVE` si une facture de renouvellement en attente est payée ; `EXPIRED` ; `CANCELLED` |
| `PENDING_TERMINATION` | Un préavis de résiliation court ; pas de renouvellement. | `POST /sni/api/v1/subscriptions/terminations/subscriptions/{n}` | `CANCELLED` à la date d'effet (préavis accepté et liste de sortie soldée), `ACTIVE` si le préavis est retiré |
| `CANCELLED` | Terminé. | `PATCH .../cancel`, fin de période demandée, fin de terme sans reconduction, préavis achevé | aucune sortie prévue (voir Écarts) |
| `EXPIRED` | Terminé : la période est échue depuis plus de `renewalExpiryDays` sans paiement. | worker horaire | aucune |
| `DRAFT`, `TRIALING`, `PAST_DUE` | Présents dans l'énumération, jamais posés par le code. | | |

### Pass (`PassStatus`)

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `PENDING_ACTIVATION` | Vendu, facture en attente. | `POST /sni/api/v1/pass-plans/{planCode}/purchase` avec un montant | `ACTIVE` (paiement, `POST /sni/api/v1/passes/{n}/activate`), `EXPIRED`, `CANCELLED` |
| `ACTIVE` | Utilisable. | activation ; émission manuelle `POST /sni/api/v1/passes` ; achat gratuit | `PARTIALLY_USED`, `CONSUMED`, `EXPIRED`, `CANCELLED` |
| `PARTIALLY_USED` | Au moins une utilisation. | `POST /sni/api/v1/passes/use` | `CONSUMED`, `ACTIVE` (annulation de tous les usages), `CANCELLED` |
| `CONSUMED` | `maxUses` atteint. | utilisation | `PARTIALLY_USED` par contre-passation |
| `EXPIRED` | Fin de validité. | worker toutes les 10 min, pour `ACTIVE`, `PENDING_ACTIVATION` et `PAST_DUE` seulement | aucune |
| `CANCELLED` | Annulé. | `PATCH /sni/api/v1/passes/{n}/cancel` (tout statut) | aucune |
| `DRAFT`, `PAST_DUE`, `SUSPENDED` | Jamais posés par les routes (`PAST_DUE` n'a pas d'appelant). | | |

### Autres objets à statut

| Objet | Statuts et transitions |
| --- | --- |
| Plan d'abonnement | `DRAFT` à la création ; `ACTIVE` (et `visible: true` forcé) à la première publication de version ; `ARCHIVED` par `DELETE` si le plan a des versions (sinon il est supprimé). |
| Version de plan d'abonnement | `DRAFT` à la création ; `ACTIVE` à la publication, qui archive la version de catalogue active précédente ; une version privée passe `ACTIVE` puis `ARCHIVED` au gré des dérivations. |
| Plan et version de pass | `DRAFT` à la création ; `PATCH .../publish` les rend `ACTIVE` (depuis `DRAFT` seulement) ; `PATCH .../archive` archive le plan (tout statut). La vente exige un plan `ACTIVE` et une version `ACTIVE`. Plusieurs versions peuvent être `ACTIVE` : la plus récente est vendue par défaut. |
| Devis | `DRAFT` à la création ; `SENT` par `.../send` ; `CONVERTED` par `.../accept` (abonnement créé) ; `REJECTED` par `.../reject` ; `EXPIRED` par le worker horaire ou par une acceptation tardive. `ACCEPTED` n'est jamais posé. |
| Préavis | `REQUESTED` → `ACCEPTED` (`.../accept`) → `COMPLETED` (worker horaire à la date d'effet, ou `.../complete`) ; `RETRACTED` depuis `REQUESTED` ou `ACCEPTED`. Lignes de sortie : `PENDING` → `DONE` ou `WAIVED`. |
| Dérivation | `PENDING_APPROVAL` si un visa est requis, sinon appliquée aussitôt : `ACTIVE`. Ensuite `SUPERSEDED` (remplacée), `EXPIRED` (retour au catalogue), `REJECTED`. `DRAFT` n'est que transitoire. |
| Mandat | `ACTIVE` → `SUSPENDED` après 3 échecs consécutifs → `ACTIVE` par `.../reactivate` ; `REVOKED` par `.../revoke` (définitif). |
| Demande de changement | `REQUESTED` → `APPROVED` → `APPLIED` (route `apply`, ou worker toutes les 15 min pour les demandes `APPROVED` arrivées à date). `REJECTED` et `CANCELLED` n'ont pas de route. |
| Transfert de pass | `PENDING_ACCEPTANCE` → `COMPLETED`, `REJECTED` ou `CANCELLED`. |
| Option | `ACTIVE` à l'ajout ; `CANCELLED` par la route ; `EXPIRED` par le worker (15 min) à `endsAt`. `PENDING` jamais posé. |
| Promotion | `DRAFT` → `ACTIVE` (`.../activate`) → `EXPIRED` (worker 15 min, à `endsAt`). `PAUSED`, `ARCHIVED` sans route. |
| Coupon | `ACTIVE` → `USED`, `EXPIRED`, `REVOKED` (`.../revoke`). |
| Parrainage | `PENDING` → `QUALIFIED` → `REWARDED`, ou `REJECTED`. |
| Droit accordé | `ACTIVE` ; `DEPLETED` à zéro ; `EXPIRED` (worker 10 min à `validUntil`) ; `CANCELLED` à l'annulation de l'abonnement ou du pass. |
| Notification d'abonnement | `PENDING` → `DISPATCHED` après remise au module `notification` (worker chaque minute), `FAILED` (pas d'adresse, canal SMS ou WhatsApp) ou `CANCELLED`. |

## Parcours

Les parcours détaillés, étape par étape, sont dans des fichiers séparés :

| Fichier | Contenu |
| --- | --- |
| [parcours-catalogue.md](parcours-catalogue.md) | Définitions de droits, plans et versions d'abonnement, plans de pass (versions, prix, droits), politique d'abonnement, règle de visa et prix plancher, politiques de dépassement. |
| [parcours-souscription.md](parcours-souscription.md) | Souscrire en direct, par devis, activer, consulter un abonnement, sièges, options. |
| [parcours-cycle-de-vie.md](parcours-cycle-de-vie.md) | Renouvellement et paiement, tolérance, mandat de prélèvement, pause (gel) et reprise, suspension, annulation, résiliation par préavis, engagement, changement de plan, alignement des échéances, lien avec le contrat. |
| [parcours-derivations.md](parcours-derivations.md) | Tarif négocié pour un abonnement, visa, retour au catalogue, dérivation en lot, protection tarifaire, rapport des concessions. |
| [parcours-pass.md](parcours-pass.md) | Vendre ou émettre un pass, l'activer, le contrôler à l'entrée, le consommer, ses supports QR, le transférer, l'annuler. |
| [parcours-droits-et-consommation.md](parcours-droits-et-consommation.md) | Soldes de droits, vérifier, retenir, consommer, rendre, relevés d'usage, dépassements, reports. |
| [parcours-promotions.md](parcours-promotions.md) | Promotions, coupons, bénéficiaires nominatifs, parrainage, simulation de prix, panier du portail. |
| [parcours-suivi.md](parcours-suivi.md) | Chronologie, notifications d'abonnement, indicateurs, éléments facturables. |

## Règles et erreurs à refléter dans l'écran

Formes d'erreur : voir [Erreurs](../README.md#erreurs). Dans ce module, une `ConflictException` produit un `409` `CONFLICT` dont le message commence par une phrase anglaise : `Could not process <objet>: <raison>` (par exemple `Could not process termination: un préavis court déjà · TRM-2026-000004`). Afficher la partie après les deux-points.

**Abonnements**
- Création : `planCode` obligatoire, `planVersionId` facultatif. Une `planVersionId` inconnue ou non active est **remplacée sans prévenir** par la dernière version active du plan. Sans `billingCycle`, le premier prix trouvé est pris. Pas de prix pour le cycle : `404` `No price found for requested billing cycle`.
- Un même `subscriberCode` ne peut avoir deux abonnements ouverts (hors `CANCELLED`, `EXPIRED`) sur la même version : `409` `RESOURCE_ALREADY_EXISTS` `Subscription already exists for subscriber code: ...`.
- Niveau KYC du plan non atteint à la création : `400` `KYC level N required for this plan`.
- Souscripteur introuvable : `404` `Member X not found`, `Customer X not found`, `Business entity X not found`.
- `suspend` : seulement depuis `ACTIVE`, `PAST_DUE`, `GRACE_PERIOD` ; `pause` : seulement `ACTIVE` ; `resume` : seulement `PAUSED` ; `renew` : seulement `ACTIVE`. Sinon `409` avec un message anglais (`only ACTIVE subscriptions can be renewed`…).
- Pause (gel) : date de reprise dans le futur (`400`), durée au plus `freezeMaxDays` (`409`), au plus `freezeMaxPerYear` gels sur douze mois glissants (`409`), un seul gel ouvert (`409`).
- `GET /sni/api/v1/subscriptions/current` répond `404` `No current active subscription found` quand le souscripteur n'a pas d'abonnement `ACTIVE` dont la période couvre aujourd'hui.

**Résiliation**
- Motif `reasonCategory` obligatoire. Possible depuis `ACTIVE`, `GRACE_PERIOD`, `PAUSED`, `SUSPENDED` (et les statuts jamais atteints `TRIALING`, `PAST_DUE`) ; sinon `409`. Un seul préavis ouvert : `409` `un préavis court déjà · TRM-...`.
- La date d'effet est au plus tôt aujourd'hui + préavis ; une date antérieure demandée est **relevée silencieusement**.
- Accepter : seulement `REQUESTED` (`400`). Dispenser les frais : seulement `REQUESTED`, motif obligatoire, frais non nuls (`400`). Retirer : impossible si des frais ont déjà été facturés (`409`, émettre un avoir d'abord). Achever : `409` si une ligne obligatoire de la liste de sortie est encore ouverte.

**Engagement, mandat, devis**
- Engagement : `commitmentMonths` ≥ 1 ; formule `FIXED_FEE` sans montant : `400` `Une pénalité fixe demande un montant`. Sans engagement : `404` `Cet abonnement n'a pas d'engagement`.
- Mandat : le portefeuille doit appartenir au souscripteur (`409`) ; plafond positif ou absent ; un seul mandat non révoqué par abonnement (`409` `un mandat existe déjà`). Prélèvement manuel sans mandat actif ou sans solde dû : `404` `Aucun mandat actif, ou rien à prélever sur cette facture`.
- Devis : prix proposé ≥ 0, validité dans le futur (`400`). Un prix sous le prix plancher ou au-delà du rabais maximal est refusé dès la création (`409`). Envoyer ou accepter un devis expiré : `409`, et l'acceptation le passe `EXPIRED`.

**Dérivations**
- `reason` obligatoire ; `REVERT_AFTER_PERIODS` exige `revertAfterPeriods` ≥ 1 (`400`). Prix sous le plancher de la version, rabais au-delà de `maxDiscountPercentAllowed`, ou aucun écart avec le catalogue : `409`. Le visa doit venir d'un autre compte que le demandeur (`409`). Refus : motif obligatoire.

**Pass**
- Achat : plan `ACTIVE` (`400` `Pass plan X is not active`), version active, prix dans la devise demandée (défaut `XAF`, `404` sinon). Niveau KYC : `400`.
- Émission manuelle : au moins un droit (`400`), quantité obligatoire si non illimité.
- `Idempotency-Key` n'est pas utilisé ici : la clé est le champ `idempotencyKey` du corps. Rejouée par le même titulaire, elle rend le pass existant ; par un autre, `409`.
- Activation : seulement depuis `PENDING_ACTIVATION` (`400`).
- Validation et utilisation : motifs en français (`Ce pass a expiré`, `Ce pass est entièrement consommé`, `Ce pass n'est utilisable que par son titulaire`, `Ce pass n'est pas utilisable à cette heure`…). `validate` répond toujours `200` avec `valid: false` et `reason` ; `use` répond `400` avec le même motif.
- Transfert : pass `transferable`, utilisable, non expiré, destinataire différent et existant, un seul transfert en attente (`400`). À l'acceptation, niveau KYC du destinataire (`400`).

**Droits**
- Solde insuffisant : `409` `Could not process entitlement: insufficient balance`. Retenue sans `referenceType`/`referenceId` : `400`.
- Usage avec dépassement : `409` si aucune politique de dépassement, si elle est en mode `BLOCK`, ou si aucun abonnement actif.

**Promotions et coupons**
- Promotion `FREE_TRIAL_DAYS` : `400` `Trial promotions are not supported in this version`. Fin avant début : `400`.
- Lot de coupons : 1 à 50 000 codes, longueur 6 à 32 (`400`). Coupon refusé à la retenue : `400` avec un motif en français.
- Bénéficiaire ayant déjà utilisé la promotion : ne peut plus être retiré (`400`). Import limité à 10 000 lignes.

## À prévoir côté frontend

**Écrans back-office**
1. Catalogue : plans d'abonnement (liste, fiche, versions, publication), définitions de droits, plans de pass (versions, prix, droits), politique d'abonnement, règle de visa.
2. Abonnements : liste filtrable, fiche avec onglets (période, montants, renouvellement en attente, droits, historique, échéancier, engagement, gels, mandat et tentatives, préavis, dérivations, options, sièges, chronologie).
3. Devis : liste, création avec aperçu (`outlook`), envoi, acceptation.
4. Résiliations : file des préavis par statut, sorties bloquées, fiche avec liste de sortie.
5. Dérivations : file des visas (`status=PENDING_APPROVAL`), fiche avec écarts, simulation, lots, protection tarifaire, rapport des concessions.
6. Pass : liste, fiche (droits, historique, supports QR, transferts), vente, émission manuelle ; écran de contrôle d'accès (scan, validation, utilisation).
7. Promotions : liste, création, activation, coupons par lot, bénéficiaires (ajout, import, retrait), parrainage.
8. Tableau de bord : indicateurs (`kpis`, `overview`), éléments facturables en attente.

**Droits à tester pour masquer les boutons** (comparer à la liste `roles` reçue à la connexion)
- `SUBSCRIPTION:READ` / `CREATE` / `UPDATE` / `CANCEL` / `ACTIVATE` : la plupart des routes. Attention aux choix du code : `pause` et `suspend` exigent `SUBSCRIPTION:CANCEL`, `resume` et `renew` exigent `SUBSCRIPTION:ACTIVATE`, `GET /passes/{n}/renewal` exige `SUBSCRIPTION:ACTIVATE`.
- Cycle de vie, devis, résiliations, dérivations (`/subscriptions/lifecycle/**`, `/subscriptions/quotes/**`, `/subscriptions/terminations/**`, `/subscriptions/derivations/**`) : en plus du droit, le compte doit avoir `ROLE_ADMIN`, `ROLE_SUPER_ADMIN` ou `ROLE_STAFF`. Réservé à `ROLE_ADMIN` ou `ROLE_SUPER_ADMIN` : modifier la politique, lever un engagement, dispenser des frais de rupture, modifier la règle de visa et le prix plancher.
- Plans de pass : `ADMIN:ACCESS` **et** `PASS_PLAN:READ` (lecture) ou `PASS_PLAN:WRITE` (écriture) ; vente d'un pass : `PASS:WRITE`. Ces droits sont vérifiés même pour `ROLE_SUPER_ADMIN` (voir [Qui peut appeler quoi](../README.md#qui-peut-appeler-quoi)).
- Promotions et bénéficiaires : `BILLING:READ` / `CREATE` / `UPDATE`. Coupons, parrainage, droits `/entitlements`, `/internal/entitlements`, `/pricing/simulate` : `ADMIN:ACCESS`. Éléments facturables : `PAYMENT:READ`.

**Listes de référence à charger**
- Plans : `GET /sni/api/v1/subscriptions/plans?status=ACTIVE` ; versions : `GET .../plans/{planCode}/versions` (seulement les versions de catalogue).
- Définitions de droits : `GET /sni/api/v1/subscriptions/entitlement-definitions/search/basic?query=...` pour une saisie assistée.
- Plans de pass : `GET /sni/api/v1/pass-plans?status=ACTIVE`, puis `GET .../{planCode}/versions` et `GET /sni/api/v1/pass-plans/versions/{versionCode}/prices`.
- Les souscripteurs (membres, clients, entreprises) viennent des modules [client](../client/README.md) et [company](../company/README.md) : envoyer leur code métier (`memberId`, `customerId`, `code` d'entreprise) dans `subscriberCode` ou `ownerCode`.
- Énumérations : toutes dans les modèles des fichiers `routes/`.

**Pièges propres au module**
- **Versions de plan d'abonnement** : pas de code lisible, seulement l'`id` numérique. `PlanVersionResponse.id` arrive en texte : le réutiliser tel quel dans `{versionId}`, dans `planVersionId` (texte) des créations d'abonnement et de devis. Plusieurs champs restent typés entier 64 bits dans le corps (`targetPlanVersionId` des changements, `planVersionId` des options et des politiques de dépassement, `previousVersionId` de la protection tarifaire) : non vérifié si le serveur accepte une chaîne à leur place ; si un nombre est envoyé, il doit être exact (pas d'arrondi JavaScript).
- **Versions de pass** : `versionCode` lisible, insensible à la casse. Dans `CreatePassPurchaseRequest`, le champ s'appelle `planVersionId` mais attend ce **code de version**. Dans `CreatePassRequest` (émission manuelle), `planVersionId` est au contraire l'id numérique d'une version de plan d'**abonnement**.
- **`PATCH .../cancel` sans corps annule immédiatement.** Pour une fin de période, envoyer `{ "cancelAtPeriodEnd": true }`. Il n'existe aucune route pour retirer ce drapeau.
- **`changedBy`, `requestedBy`, `acceptedBy`, `issuedBy`, `createdBy`, `addedBy`, `revokedBy`, `reservedBy`** sont du texte libre fourni par l'écran (corps ou paramètre de requête). Le serveur ne les déduit pas du jeton sur ces routes ; sans valeur, il écrit souvent `SYSTEM`. Envoyer l'identité de l'utilisateur connecté. Les routes du cycle de vie, des devis, résiliations et dérivations, elles, prennent l'acteur dans le jeton.
- **`totalAmount` d'un abonnement** est le prix d'une période (taxe comprise) pour un abonnement créé depuis le 2026-11-08 ; les frais d'installation et le dépôt ne figurent que sur la première facture. Un abonnement plus ancien peut encore contenir ces frais dans `totalAmount` jusqu'à une dérivation ou un changement de plan.
- **Renouvellement en attente** : quand `pendingRenewalInvoice` est rempli, la période n'a **pas** encore changé ; afficher « renouvellement du `pendingRenewalStart` au `pendingRenewalEnd`, en attente du paiement de la facture X » et proposer le paiement (module [billing](../billing/README.md) ou portail).
- Les routes qui renvoient des entités brutes (`GET /passes/{n}/events`, `/history`, `/renewal`, `GET /subscriptions/lifecycle/policy`) exposent des `id` 64 bits et, pour les trois premières, un objet `pass` complet ; ne lire que les champs utiles.
- `GET /passes/{n}/renewal` renvoie un corps vide (`200`) si le pass n'est pas en renouvellement automatique.
- Pagination : les listes de devis et de dérivations ignorent `sort` (tri par date de création décroissante). Les préavis (`GET /terminations`) ne sont pas paginés et filtrent par défaut sur `REQUESTED`.
- Aucun fichier à téléverser dans ce module. Le QR d'un pass arrive en image `data:` dans `imageDataUri`, à afficher directement. Les PDF (factures, contrats) sont dans billing et contract.
- Pas de temps réel propre au module : les changements produits par les workers (tolérance, suspension, activation après paiement, contrat généré) se voient en relisant la fiche. Prévoir un rafraîchissement après un paiement : l'activation suit le paiement, le `contractCode` apparaît quelques secondes après l'activation (file d'événements toutes les 10 s).

## Effets automatiques

**Workers** (délais par défaut, tous réglables dans la configuration, aucun n'est surchargé dans `application.yml`) :

| Worker | Fréquence | Effet visible |
| --- | --- | --- |
| `SubscriptionRenewalWorker` | 15 min | Applique les renouvellements dont la facture est soldée, puis émet la facture de la période suivante pour chaque abonnement `ACTIVE` en reconduction automatique dont `nextBillingDate` est atteinte (voir [parcours-cycle-de-vie.md](parcours-cycle-de-vie.md#renouvellement-automatique-et-paiement)). |
| `SubscriptionLifecycleWorker` | 1 h (premier passage 2 min après le démarrage) | Dans l'ordre : achève les préavis acceptés arrivés à date ; expire les devis ; entrée en tolérance et suspension ; avis de fin J-7, J-3, J0 puis clôture le lendemain des abonnements sans reconduction ; tolérance puis `EXPIRED` des périodes échues non renouvelées ; reconduction ou levée des engagements échus ; nouvelle tentative d'activation des abonnements `PENDING_DOCUMENTS`. |
| `SubscriptionCancellationWorker` | 15 min | Annule les abonnements `cancelAtPeriodEnd` dont `currentPeriodEnd` est atteinte (le jour même de la fin). |
| `SubscriptionIntegrityWorker` | 30 min | Recrée les droits d'un abonnement `ACTIVE` qui n'en a plus ; remet en vigueur un contrat expiré alors que l'abonnement court, et prolonge par avenant un contrat resté derrière la période. |
| `SubscriptionContractGenerationRepairWorker` | 1 min | Génère le contrat des abonnements, pass et options créés depuis plus de 5 min sans contrat. |
| `SubscriptionChangeWorker` | 15 min | Applique les demandes de changement `APPROVED` arrivées à leur date. |
| `SubscriptionAddonExpiryWorker` | 15 min | Passe `EXPIRED` les options dont `endsAt` est passée. |
| `SubscriptionRolloverWorker` | 15 min | Reporte le reliquat des droits reportables sur la période suivante. |
| `EntitlementExpiryWorker` | 10 min | Passe `EXPIRED` les droits dont `validUntil` est passée. |
| `EntitlementReservationExpiryWorker` | 5 min | Rend les retenues de droits expirées (15 min par défaut). |
| `PassExpiryWorker` | 10 min | Passe `EXPIRED` les pass échus. |
| `PassRenewalWorker` | 15 min | Prolonge et refacture les pass en renouvellement automatique. |
| `PassAlertWorker` | chaque jour à 8 h | Met en file les alertes pass : expiration à 7 jours, solde bas (20 % restant), inutilisé depuis 14 jours. |
| `SubscriptionNotificationDispatchWorker` | 1 min | Remet les notifications d'abonnement dues au module `notification` (courriel au titulaire), puis les passe `DISPATCHED` ; sans adresse ou sur un canal sans émetteur : `FAILED`. |
| `PromotionExpiryWorker` | 15 min | Expire les promotions échues. |
| `CouponReservationExpiryWorker` | 2 min | Rend les coupons retenus par des paniers abandonnés (retenue de 30 min). |
| `ReferralTenureWorker` | chaque jour à 2 h 30 | Qualifie les parrainages dont l'ancienneté requise est atteinte. |

**Déclenché par un paiement** (`src/main/java/com/sni/bokaticowork/features/payment/service/support/PaymentTransactionWorkflowProcessor.java:152`) : un paiement réussi sur un abonnement fait prendre effet un renouvellement en attente ; sinon sort de tolérance ; sinon recale la période si l'échéance est payée en retard ; sinon active un abonnement `PENDING_ACTIVATION` dont la facture est soldée. Sur un pass `PENDING_ACTIVATION`, il l'active.

**Courriels** : à la création en attente de paiement, à l'activation, au renouvellement, à l'annulation et à la suspension (`SubscriptionEmailNotifier`) ; par la file d'événements : facture de renouvellement à régler (`SUBSCRIPTION_RENEWAL_PAYMENT_DUE`), entrée en tolérance, avis de fin (`SUBSCRIPTION_ENDING_SOON`, `SUBSCRIPTION_ENDS_TODAY`, `SUBSCRIPTION_ENDED`), préavis demandé, accepté, achevé, devis envoyé, échec de prélèvement, pièces KYC manquantes. Pass : création, activation, annulation.

**Notifications in-app** du portail (module [notification](../notification/README.md)) : création, activation, renouvellement, annulation, facture de renouvellement à régler, pour le membre dont l'adresse est connue.

**Événements internes** : génération de contrat (`CONTRACT_GENERATION_REQUESTED`) à l'activation d'un abonnement, d'un pass ou à l'ajout d'une option ; avenant de renouvellement (`CONTRACT_RENEWAL_AMENDMENT_REQUESTED`) à chaque nouvelle période ; le contrat est créé, généré en PDF et signé par le système.

## Écarts relevés

Constat seulement, classé du plus gênant au moins gênant pour un frontend.

1. **Corrigé le 2026-11-08** : un renouvellement ne facture plus que le prix de la période et sa taxe, pour les abonnements créés désormais et pour les pass. Les abonnements créés avant gardent leurs montants et refactureront les frais tant que leurs données ne sont pas reprises. Constat d'origine : **Le renouvellement refacture les frais d'installation (et, pour un pass, le dépôt).** À la création, `subtotalAmount` et `totalAmount` sont calculés sur prix + frais d'installation + dépôt (`src/main/java/com/sni/bokaticowork/features/subscription/subscription/service/support/subscription/SubscriptionCreationOperator.java:98`) ; le renouvellement facture `subtotalAmount + taxAmount` (`src/main/java/com/sni/bokaticowork/features/subscription/subscription/service/support/subscription/SubscriptionLifecycleOperator.java:557`), donc les frais d'installation à chaque période. Même schéma pour les pass (`.../service/support/pass/PassBillingSupport.java:48`, montant `totalAmount` qui inclut le dépôt). La mensualité de référence des pénalités de rupture et du gel (`totalAmount`) en hérite.
2. **Corrigé le 2026-11-08** : les notifications sont réellement envoyées par le module `notification`. Constat d'origine : **La notification d'abonnement n'envoie rien.** `dispatchDue` se contente de passer la ligne `DISPATCHED` (`src/main/java/com/sni/bokaticowork/features/subscription/notification/service/impl/SubscriptionNotificationServiceImpl.java:60`). Les alertes pass et les alertes de quota (80 %, 100 %) n'atteignent donc personne ; leur `recipient` est d'ailleurs le code du titulaire, pas une adresse.
3. **Une promotion créée par l'API n'accorde aucune remise.** Le moteur de prix lit les récompenses (`promotion_reward`) et rejette une promotion sans récompense avec `Cette promotion n'accorde rien` (`src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/engine/PricingEngine.java:337`) ; aucune route ni migration ne crée de récompense. `discountType` et `discountValue` de `CreatePromotionRequest` ne sont pas lus par le moteur. Coupons et parrainages qui s'appuient sur ces promotions n'ont donc pas d'effet monétaire.
4. **Reprise après une pause : prolongation inversée.** La période est prolongée des jours restant entre aujourd'hui et `pauseUntil`, pas des jours écoulés (`SubscriptionLifecycleOperator.java:166`) : reprendre à la date prévue ne prolonge de rien, reprendre tôt prolonge du reste. Aucun worker ne reprend un abonnement `PAUSED` à `pauseUntil`.
5. **Suspendre ou mettre en pause ne ferme pas les droits.** `suspend` et `pause` ne touchent pas aux droits accordés (`SubscriptionLifecycleOperator.java:130` et `:148`), et la recherche des droits consommables ne regarde que le statut du droit, pas celui de l'abonnement (`src/main/java/com/sni/bokaticowork/features/subscription/repository/EntitlementGrantRepository.java:23`). Un abonnement `SUSPENDED` ou `PAUSED` garde donc ses quotas consommables jusqu'à leur `validUntil`. Non vérifié : un contrôle éventuel du statut de l'abonnement dans les modules qui consomment (réservations).
6. **Pass utilisé jamais renouvelé ni expiré.** Après la première utilisation, le pass est `PARTIALLY_USED` (`src/main/java/com/sni/bokaticowork/features/subscription/subscription/pass/service/PassUsageService.java:165`) ; le renouvellement ne traite que `ACTIVE` (`.../service/support/pass/PassRenewalOperator.java:48`) et l'expiration que `ACTIVE`, `PENDING_ACTIVATION`, `PAST_DUE` (`.../service/support/pass/PassLifecycleOperator.java:112`). Le statut reste `PARTIALLY_USED` après la fin de validité (la validation refuse bien l'accès). Le renouvellement d'un pass prolonge et remet `usedCount` à zéro **avant** paiement et n'ouvre aucun nouveau droit : le paiement n'écrit qu'un événement (`PassRenewalOperator.java:77`).
7. **Devis négocié : première facture au prix catalogue.** L'acceptation crée l'abonnement, qui émet sa facture au prix du catalogue, puis seulement crée la dérivation au prix négocié (`src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/service/SubscriptionQuoteService.java:177` puis `:187`). Non vérifié à l'exécution, mais l'ordre des appels est explicite.
8. **Renouvellement payé d'avance : l'abonnement « courant » disparaît.** Un renouvellement payé avant la fin de période fait démarrer la nouvelle période à sa date future (`SubscriptionLifecycleOperator.java:324`) ; `GET /subscriptions/current` exige `current_period_start <= aujourd'hui` (`src/main/java/com/sni/bokaticowork/features/subscription/repository/SubscriptionRepository.java:21`) et répond `404` jusqu'à cette date.
9. **Activation et annulation sans garde de statut.** `PATCH .../activate` réactive tout statut autre que `ACTIVE`, y compris `CANCELLED` ou `EXPIRED` (`SubscriptionLifecycleOperator.java:89`) ; `PATCH .../cancel` accepte tout statut et réécrit l'annulation. Le retrait d'un préavis remet `ACTIVE` quel que soit le statut d'avant (`PAUSED`, `SUSPENDED`) (`src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/service/SubscriptionTerminationService.java:218`). Annuler un pass écrit toujours `ACTIVE` comme statut de départ dans l'historique (`PassLifecycleOperator.java:97`).
10. **Clôtures automatiques incomplètes.** La fin de terme (`.../lifecycle/service/SubscriptionEndNoticeService.java:130`) et l'expiration (`.../lifecycle/service/SubscriptionExpiryService.java:115`) changent le statut sans révoquer les droits, ni résilier le contrat, ni libérer le dépôt de garantie, contrairement à `PATCH .../cancel`.
11. **Courriel de suspension sous le gabarit de création** : sujet « Abonnement cree X » (`.../service/support/subscription/SubscriptionEmailNotifier.java:111` et `:121`).
12. **Statuts jamais atteints** : abonnement `DRAFT`, `TRIALING`, `PAST_DUE` (les jours d'essai du prix et `trialStart`/`trialEnd` ne sont pas exploités) ; pass `DRAFT`, `PAST_DUE` (`markPastDue` sans appelant), `SUSPENDED` ; devis `ACCEPTED` ; option `PENDING` ; promotion `PAUSED`, `ARCHIVED` ; changement `REJECTED`, `CANCELLED`. `PENDING_DOCUMENTS` n'est atteint que si le KYC baisse entre la création et l'activation, la création refusant déjà un KYC insuffisant (`SubscriptionCreationOperator.java:77`).
13. **Parrainage au premier paiement jamais qualifié** : `qualifyOnFirstPayment` n'a aucun appelant (`src/main/java/com/sni/bokaticowork/features/subscription/promotion/pricing/referral/service/ReferralService.java:153`). Aucune route ne crée de programme de parrainage.
14. **Demande de changement** : `approve` ne vérifie pas le statut (on peut « approuver » une demande appliquée) ; `apply` applique tout de suite même si `effectiveDate` est future ; les types `ADD_ADDON`, `REMOVE_ADDON`, `QUANTITY_CHANGE` ne font rien d'autre que changer le statut (`src/main/java/com/sni/bokaticowork/features/subscription/change/service/impl/SubscriptionChangeRequestServiceImpl.java:63`, `:71`).
15. **Lecture qui écrit** : `GET /passes/{n}/credentials` (droit `SUBSCRIPTION:READ`) émet un nouveau support et révoque l'ancien quand il manque ou a expiré (`.../pass/service/PassCredentialService.java:108`).
16. **Transfert de pass** : les droits accordés restent au nom de l'ancien titulaire ; `transferFee` est enregistré mais jamais facturé (`.../pass/service/PassTransferService.java:110`).
17. **Entités brutes en réponse** : `GET /passes/{n}/events`, `/history`, `/renewal` sérialisent des entités JPA avec une relation `pass` chargée à la demande, alors que `open-in-view` est désactivé (`src/main/resources/application.yml:37`) ; non vérifié à l'exécution, risque de `500` à la sérialisation (`src/main/java/com/sni/bokaticowork/features/subscription/subscription/controller/PassController.java:92`).
18. **Traitements en lot dans une seule transaction** : la protection tarifaire et la dérivation en lot rattrapent l'erreur d'une ligne, mais l'exception levée par le service transactionnel appelé marque la transaction commune à annuler (`src/main/java/com/sni/bokaticowork/features/subscription/derivation/service/PlanDerivationBatchService.java:104`) ; non vérifié à l'exécution, un échec d'une ligne peut faire échouer tout le lot.
19. **Plans de pass** : une version publiée reste modifiable (prix et droits ajoutés ou retirés sans contrôle de statut) ; `PATCH /pass-plans/{planCode}/versions/{versionCode}/publish` ignore `planCode` ; `transferable` et `shareable` des versions ne sont réglables par aucune route, donc toujours faux sur un pass vendu.
20. **Plans d'abonnement** : `requiredKycLevel` ne se règle par aucune route et n'est pas renvoyé ; `trialDays` est renvoyé dans `PlanPriceResponse` mais absent de `PlanPriceRequest`. Publier une version force `visible: true` sur le plan.
21. **Messages en anglais** pour une grande partie des refus (abonnements, droits, pass achetés, promotions), et préfixe `Could not process ...:` sur tous les `409` de `ConflictException`.
22. **Identifiants numériques seulement** : options (`PATCH /subscriptions/addons/{addonId}/cancel`), versions de plan d'abonnement, politiques de dépassement ; `DerivationView.supersedesDerivationId` est un id interne.
23. **Annulation en fin de période le dernier jour** : `SubscriptionCancellationWorker` clôt quand `currentPeriodEnd <= aujourd'hui`, donc pendant le dernier jour payé, alors que la fin de terme clôt le lendemain.
