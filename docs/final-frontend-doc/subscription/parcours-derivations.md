# subscription : parcours des tarifs négociés (dérivations)

> Partie de [README.md](README.md). Routes exactes et champs : [routes.md](routes.md). Règles communes : [README racine](../README.md).

Une dérivation donne à **un** abonnement un contenu différent du catalogue : prix, frais d'installation, jours d'essai, engagement, quantités de droits, droits illimités, avantages inclus. Le serveur copie la version de catalogue en une **version privée**, y applique l'écart, chiffre chaque écart (`deltas`) et bascule l'abonnement dessus ; ses montants sont recalculés sur le nouveau prix. La version de catalogue reste la référence (`sourceVersion`).

Toutes les routes sont sous `/sni/api/v1/subscriptions/derivations` et exigent, en plus du droit, le rôle ADMIN, SUPER_ADMIN ou STAFF. L'acteur (demandeur, approbateur) est pris dans le jeton. Les règles d'encadrement (seuil de visa, rabais maximal, prix plancher) se règlent dans [parcours-catalogue.md](parcours-catalogue.md#règle-de-visa-et-prix-plancher).

## Négocier le tarif d'un abonnement

Écran : depuis la fiche abonnement, « Tarif négocié ».

1. Saisir l'écart et simuler : `POST /sni/api/v1/subscriptions/derivations/subscriptions/{subscriptionNumber}/simulate` ([route](routes/PlanDerivationController.md#r-post-sni-api-v1-subscriptions-derivations-subscriptions-subscriptionnumber-simulate)) avec un `SpecRequest` :
   - `reason` (obligatoire) : `NEGOTIATION`, `GOODWILL`, `PARTNERSHIP`, `PILOT`, `GRANDFATHERING`, `LOYALTY`, `CORRECTION` ; `reasonDetails` ;
   - ce qui change, tout facultatif : `price` (prix de la période sur le cycle de l'abonnement), `setupFee`, `trialDays`, `commitmentMonths`, `entitlementQuantities` (objet `{ "CODE_DROIT": quantité }`), `unlimitedEntitlements` (codes), `includedBenefits` (titres d'avantages à inclure) ;
   - `effectiveFrom` (aujourd'hui par défaut), `effectiveTo` (fin de validité, facultative) ;
   - `renewalBehaviour` : `KEEP` (garder à chaque renouvellement, défaut), `REVERT_TO_CATALOGUE` (revenir au catalogue au prochain renouvellement), `REVERT_AFTER_PERIODS` avec `revertAfterPeriods` ≥ 1 ;
   - `promotionsAllowed` (faux par défaut : aucune promotion ne s'ajoute au prix négocié).
2. Afficher la simulation (`PlanDerivationService.Preview`) : `cataloguePrice`, `derivedPrice`, `discountPercent`, `totalImpact`, la liste `deltas` (type, cible, valeur catalogue, valeur négociée, impact), `requiresApproval`, et `blockingReason` qui, s'il est rempli, empêche la création (prix sous le plancher, rabais au-delà du maximum admis, ou rien ne diffère du catalogue).
3. Créer : `POST .../derivations/subscriptions/{subscriptionNumber}` avec le même corps. Réponse `DerivationView` :
   - `ACTIVE` si aucun visa n'est requis : l'abonnement est déjà sur sa version privée, ses montants sont recalculés ;
   - `PENDING_APPROVAL` sinon : rien n'a encore changé pour l'abonnement.
   Une dérivation active précédente devient `SUPERSEDED` quand la nouvelle s'applique (`supersedesDerivationId`).
4. Erreurs : `400` motif manquant ou `revertAfterPeriods` absent ; `409` pour un `blockingReason`.

Les montants recalculés valent pour les prochaines factures ; la facture déjà émise de la période en cours n'est pas reprise.

## Viser une dérivation

Écran : file « À viser », `GET /sni/api/v1/subscriptions/derivations?status=PENDING_APPROVAL` (paramètre `status` répétable, paginé, toujours trié par date de création décroissante).

1. Ouvrir : `GET .../derivations/{derivationCode}` et ses écarts `GET .../derivations/{derivationCode}/deltas`.
2. Approuver : `POST .../{derivationCode}/approve`, sans corps. Le visa doit venir d'un **autre compte** que le demandeur (`409` sinon) : masquer le bouton quand `requestedBy` est l'utilisateur connecté. La dérivation s'applique aussitôt (`ACTIVE`).
3. Refuser : `POST .../{derivationCode}/reject` avec `{ "reason": "..." }` obligatoire. Possible pour `PENDING_APPROVAL` (et `DRAFT`). `REJECTED`, la version privée est archivée.

## Revenir au catalogue

- À la main : `POST .../derivations/subscriptions/{subscriptionNumber}/revert` avec `{ "reason": "..." }` obligatoire. L'abonnement repasse sur la **version de catalogue active** du plan (pas forcément celle d'origine), montants recalculés ; la dérivation passe `EXPIRED`. `409` s'il n'y a pas de dérivation active.
- Automatiquement, au renouvellement : une dérivation dont `effectiveTo` est passée, ou réglée sur `REVERT_TO_CATALOGUE`, ou ayant atteint `revertAfterPeriods` (`periodsApplied` compte les renouvellements), rend l'abonnement au catalogue avant que la période suivante soit facturée.

Historique d'un abonnement : `GET .../derivations/subscriptions/{subscriptionNumber}`.

## Dérivation en lot

Appliquer le même écart à plusieurs abonnements (partenariat, geste commercial collectif).

1. Simuler : `POST /sni/api/v1/subscriptions/derivations/bulk` avec `subscriptionNumbers` (liste), `spec` (un `SpecRequest`) et `simulateOnly` (vrai **par défaut**). La réponse donne, par abonnement, `ok`, `impact`, `requiresApproval`, `message`, et les totaux `succeeded`, `failed`, `pendingApproval`, `totalImpact`.
2. Appliquer : même appel avec `simulateOnly: false`. La réponse porte un `batchCode` ; chaque ligne porte son `derivationCode`.
3. Relire le lot : `GET .../derivations/batches/{batchCode}`.

## Protection tarifaire après une hausse de catalogue

Après publication d'une nouvelle version d'un plan plus chère, garder aux abonnés de l'ancienne version le prix qu'ils payaient.

1. Simuler : `POST /sni/api/v1/subscriptions/derivations/protect` avec `previousVersionId` (id numérique de l'ancienne version de catalogue), `protectedUntil` (fin de la protection, vide = sans terme) et `simulateOnly` (vrai par défaut). Les abonnements ouverts sur cette version sont listés avec l'impact ; une ligne « prix inchangé · rien à protéger » compte comme réussie.
2. Appliquer avec `simulateOnly: false` : chaque abonné est basculé sur la version active du plan, puis protégé par une dérivation `GRANDFATHERING` à son ancien prix (`renewalBehaviour: KEEP`). `400` si la version donnée est une version privée.

## Rapport des concessions

`GET /sni/api/v1/subscriptions/derivations/concessions?from=2026-01-01&to=2026-03-31` (dates obligatoires) : par demandeur et par motif, le nombre de dérivations et l'impact total sur la période. À présenter en tableau.
