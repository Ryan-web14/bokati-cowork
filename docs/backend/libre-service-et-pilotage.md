# Libre-service et pilotage · lot I

Le portail client en écriture, les indicateurs qu'on ne pouvait pas dire, les relances d'impayé
paramétrées par segment, l'historique lisible par le client. Rien de nouveau dans les règles : le
membre passe par les mêmes services que le guichet, avec les mêmes politiques.

## Le portail en écriture · `/sni/api/v1/client/...`

`ClientSubscriptionSelfService` · le membre n'agit que sur ce qui est à lui (un abonnement d'un
autre « n'existe pas »), l'acteur est son code membre, le canal est `PORTAL`.

```
GET  /subscriptions/{n}/plan-change/preview?targetPlanCode&atNextPeriod   ce que le changement coûte ou rend
POST /subscriptions/{n}/plan-change   {targetPlanCode, atNextPeriod, reason}
GET  /subscriptions/{n}/freeze/terms  quota restant, durée max, part facturée, préavis
POST /subscriptions/{n}/freeze        {until, reason} · immédiat
POST /subscriptions/{n}/resume
GET  /subscriptions/{n}/commitment/early-termination?on=
POST /subscriptions/{n}/termination/preview   {reasonCategory, requestedEffectiveDate}
POST /subscriptions/{n}/termination           pose le préavis
GET  /subscriptions/{n}/terminations
POST /terminations/{code}/retract
GET|POST|DELETE /subscriptions/{n}/debit-mandate   {walletNumber, maxAmountPerDebit}
GET  /quotes · GET /quotes/{n} · POST /quotes/{n}/accept · POST /quotes/{n}/reject
GET  /subscriptions/{n}/timeline · GET /history
```

Ce que le membre fait seul et ce qui reste au guichet :

- **Changer de plan** · catalogue à catalogue, au rythme de son abonnement. Sa confirmation vaut
  approbation (pas de visa : rien n'est négocié). Immédiat, le changement est appliqué avec le
  prorata du lot H ; à la prochaine échéance, il est approuvé et le worker l'appliquera. Refusé
  pendant un préavis.
- **Geler** · dans les bornes de la politique. Si `freezeNoticeDays` > 0, le portail ne gèle pas
  (le gel portail est immédiat) · `freeze/terms` le dit (`canFreezeNow`), le membre passe par le
  support.
- **Résilier** · préavis de la politique, jamais dérogatoire ; les frais de rupture sont montrés
  avant. La dispense de frais reste au guichet (ADMIN). Il peut retirer son préavis tant que rien
  n'a été facturé.
- **Prélèvement** · le consentement est le sien (`PORTAL`, référence `portal:<membre>`) ; il
  révoque seul.
- **Devis** · il voit les siens, accepte ou refuse ; l'acceptation convertit (lot H).

## L'historique lisible · `GET /client/history`, `GET /client/subscriptions/{n}/timeline`

`ClientHistoryService` fusionne la frise interne (sans les types internes : signaux de fraude,
notifications, droits accordés, changements de statut bruts), les gels, les préavis, les
conditions particulières accordées (dérivations actives ou terminées, jamais celles en attente de
visa) et les changements de plan, en entrées datées, typées (`SUBSCRIPTION`, `CHANGE`, `FREEZE`,
`TERMINATION`, `BENEFIT`, `PASS`, `USAGE`) et libellées en français, du plus récent au plus ancien.

## Indicateurs · `GET /subscription-metrics/kpis?from&to`

`SubscriptionKpiService` (défaut : les trente derniers jours) :

- **MRR / ARR** · tous les rythmes ramenés au mois (annuel ÷ 12, trimestriel ÷ 3, hebdo × 52 ÷ 12)
  sur les statuts qui gardent leurs droits ; ventilé par rythme.
- **Attrition** · résiliés sur la période ÷ actifs au début, en %, et ramené au mois.
- **Mouvement de MRR** · nouveau MRR (souscrits sur la période) moins MRR parti.
- **ARPU** et **valeur vie client** · ARPU × (100 ÷ attrition mensuelle) ; `null` si l'attrition
  est nulle. Une estimation, dite comme telle.
- **Occupation** · sièges actifs ÷ capacité des ressources actives (`bokati.subscription.kpi.workspace-type-codes`
  pour ne compter que certains types de ressource) ; **revenu par poste** · MRR ÷ sièges.
- Les compteurs des nouveaux statuts : en tolérance, en préavis, en attente de pièces.

## Relances d'impayé paramétrées · `/billing/dunning`

`dunning_policy` (une par segment `DEFAULT`, `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY`, une seule
active par segment ; un ton `SOFT` / `STANDARD` / `FIRM`) et ses `dunning_step` : tant de jours
après l'échéance, une action, un canal, un sujet et un message avec `{documentNumber}`,
`{balanceDue}`, `{currency}`, `{dueDate}`, `{daysOverdue}`, `{customerName}`, `{subscriptionNumber}`.

Actions : `REMINDER` (rappel, e-mail + in-app pour un membre), `FORMAL_NOTICE` (mise en demeure),
`GRACE_PERIOD` (l'abonnement lié entre en tolérance), `SUSPEND` (suspendu s'il est encore ouvert),
`HANDOVER` (e-mail à `bokati.billing.dunning.handover-email` · plus rien d'automatique).

Le worker (`bokati.billing.dunning.cron`, 8 h 30) parcourt les factures échues avec un solde ; pour
chacune, la politique de son segment (sinon `DEFAULT`) ; tout palier atteint et pas encore exécuté
l'est, **une fois** (`uk_dunning_notice_step`), dans sa propre transaction. Un palier n'attend pas
le précédent : une facture découverte tard reçoit rappel et mise en demeure le même jour, dans
l'ordre. Chaque exécution est écrite (`dunning_notice` : `SENT`, `SKIPPED` avec la raison,
`FAILED` avec l'erreur). L'abonnement derrière une facture est retrouvé par ses éléments facturés.

```
GET|PUT /policies · GET /policies/{code} · POST /policies/{code}/deactivate
GET /notices · GET /notices/documents/{n} · GET /notices/customers/{type}/{code}
POST /run   un passage à la main
```

Deux politiques seedées : `DNP-DEFAULT` (J+3 rappel, J+10 rappel, J+20 mise en demeure, J+30
suspension, J+45 remise) et `DNP-BUSINESS` (J+7, J+21, J+35 mise en demeure, J+60 remise · pas de
suspension automatique). Dès qu'une politique est active, l'ancien renvoi périodique du document
(`BillingPaymentReminderWorker`) se tait.

## Schéma

`V242` (dev) / `V238` (prod) : `dunning_policy`, `dunning_step`, `dunning_notice`, seeds. Pas de
DDL pour le portail ni les indicateurs.

## Ce qui reste ouvert

- Gabarits d'e-mail `BILLING_DUNNING_*` et `SUBSCRIPTION_QUOTE_SENT` côté notification.
- Le gel planifié depuis le portail (avec préavis) demanderait une pause à date · non fait, le
  support gèle.
- Les indicateurs sont calculés à la demande ; s'ils sont consultés souvent, une vue matérialisée
  quotidienne suffira.
