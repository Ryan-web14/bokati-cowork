# Abonnements dérivés · lot G

Un abonnement peut avoir son propre prix, ses propres droits et ses propres avantages, sans qu'on
crée un plan de catalogue pour lui. Le mécanisme : on **dérive la version de plan**, pas le plan. La
version dérivée est une copie privée de la version de catalogue (`scope = SUBSCRIPTION`,
`owner_subscription_id`, `derived_from_version_id`) ; l'abonnement la lit comme n'importe quelle
version, et tout ce qui existait déjà (facturation, droits, renouvellement) fonctionne sans
changement. Le catalogue, lui, ne la voit jamais : les deux requêtes de catalogue de
`PlanVersionRepository` filtrent `scope = 'CATALOGUE'`. C'est la seule ligne du module qui change.

Tout est sous `/sni/api/v1/subscriptions/derivations` (ADMIN, SUPER_ADMIN, STAFF ; la politique en
écriture est réservée aux ADMIN).

## Simuler, créer, viser

```
POST /subscriptions/{n}/simulate     ce que la dérivation ferait · écart, coût, visa nécessaire, blocage
POST /subscriptions/{n}              crée · appliquée aussitôt, ou PENDING_APPROVAL
GET  /subscriptions/{n}              l'historique des dérivations de l'abonnement
POST /subscriptions/{n}/revert       {reason} · retour au catalogue courant, la dérivation passe EXPIRED
GET  /                               ?status=PENDING_APPROVAL,ACTIVE · paginé
GET  /{code}                         la dérivation
GET  /{code}/deltas                  ligne par ligne · valeur catalogue, valeur dérivée, impact
POST /{code}/approve                 visa · par une autre personne que le demandeur
POST /{code}/reject                  {reason} · la version privée est archivée, l'abonné n'a pas bougé
```

Le corps d'une dérivation (`SpecRequest`) : `price`, `setupFee`, `trialDays`, `commitmentMonths`,
`entitlementQuantities` (`{"HOURS": 40}`), `unlimitedEntitlements`, `includedBenefits` (titres),
`reason` (obligatoire · `NEGOTIATION`, `GOODWILL`, `PARTNERSHIP`, `PILOT`, `GRANDFATHERING`,
`LOYALTY`, `CORRECTION`), `reasonDetails`, `effectiveFrom`, `effectiveTo`, `renewalBehaviour`,
`revertAfterPeriods`, `promotionsAllowed`.

Le demandeur est celui qui est authentifié, jamais un champ du corps. C'est ce qui donne un sens au
second visa et au rapport des concessions par commercial.

## Ce qui borne

Trois bornes, dans cet ordre :

1. **Le prix plancher** d'une version de catalogue (`floor_price`). En dessous, aucune dérivation,
   même approuvée. `PUT /policy/plan-versions/{id}/floor-price` `{floorPrice}` · un corps vide le
   retire. Il ne se pose que sur une version de catalogue.
2. **Le maximum admis** (`max_discount_percent_allowed`, 50 % par défaut) · au-delà, refus.
3. **Le seuil de visa** (`max_discount_percent_without_approval` 10 %, `max_impact_without_approval`
   50 000) · au-delà, `PENDING_APPROVAL`. `GET|PUT /policy/approval-rule`. Les seuils sont des
   données, comme pour les achats et les ajustements de stock.

Une dérivation sans aucun écart avec le catalogue est refusée : elle n'a pas d'objet.

## Ce qui se passe au renouvellement

`SubscriptionLifecycleOperator.renewActive` appelle `PlanDerivationService.beforeRenewal` avant de
chiffrer la période suivante. La dérivation a décidé d'avance :

- `KEEP` · elle reste, `periods_applied` s'incrémente.
- `REVERT_TO_CATALOGUE` · retour au catalogue au premier renouvellement.
- `REVERT_AFTER_PERIODS` · retour après N périodes (`revertAfterPeriods` obligatoire).
- Une `effectiveTo` dépassée rend au catalogue quel que soit le comportement.

Le retour au catalogue va toujours vers la **version de catalogue active** du plan, pas vers celle
d'origine : si le catalogue a augmenté entre-temps, l'abonné rejoint le tarif courant. La version
privée est archivée avec sa date de fin, la dérivation passe `EXPIRED` ou `SUPERSEDED` · elle ne
disparaît jamais. « Quel prix appliquions-nous en mars » a toujours une réponse.

Une nouvelle dérivation sur un abonnement qui en a déjà une la **remplace** (`supersedes_derivation_id`)
et repart toujours de la version de catalogue, jamais de la version privée précédente. La base ne
tolère qu'une dérivation `ACTIVE` par abonnement (index unique partiel).

## En lot

```
POST /bulk           {subscriptionNumbers, spec, simulateOnly=true}
POST /protect        {previousVersionId, protectedUntil, simulateOnly=true}
GET  /batches/{code} les dérivations d'un lot
GET  /concessions    ?from=&to= · par commercial et par motif, nombre et impact total
```

Un lot ne s'arrête pas à la première erreur : chaque ligne est rapportée avec sa raison, le lot
continue. Un lot qui s'arrêterait au milieu laisserait la moitié des abonnés protégés sans que
personne ne sache lesquels. La simulation n'écrit rien et n'a pas de code de lot.

**Protection tarifaire** (`/protect`) : une nouvelle version de plan a été publiée avec un prix plus
élevé ; les abonnés de l'ancienne conservent leur tarif jusqu'à `protectedUntil` (nul : sans terme).
Chacun est basculé sur la version de catalogue courante puis dérivé en `GRANDFATHERING` au prix qu'il
payait · l'écart se lit contre le catalogue courant, et on sait exactement combien la protection
coûte. Un abonné dont le prix n'a pas changé n'a rien à protéger et n'est pas compté comme une
erreur ; un cycle sans prix sur l'ancienne version l'est.

## Schéma

`V240` (dev) / `V236` (prod) : colonnes `scope`, `owner_subscription_id`, `derived_from_version_id`,
`floor_price` sur `subscription_plan_version` (`ck_plan_version_scope` : une version privée a un
propriétaire et une source, une version de catalogue n'en a pas) ; tables `plan_derivation`
(`uk_plan_derivation_active`, `ck_plan_derivation_four_eyes` : l'approbateur diffère du demandeur),
`plan_derivation_delta`, `plan_derivation_approval_rule` (seed `PDR-DEFAULT`) ; séquences
`plan_derivation` (`DRV-{YYYY}-{SEQ}`) et `plan_derivation_batch` (`DRB-{SEQ}`).

## Ce qui reste ouvert

- `promotionsAllowed` est porté par la dérivation et exposé par `PlanDerivationService.promotionsAllowed`,
  mais n'est pas encore branché dans `SubscriptionPricingBridge` : le pont n'est appelé qu'à la
  création, les renouvellements ne repassent pas par les promotions. À brancher quand le lot H
  retouchera le renouvellement.
- Les versions privées comptent dans `max_version_number` du plan : les numéros de version du
  catalogue ne sont donc plus contigus. C'est voulu (une version, un numéro), mais visible.
