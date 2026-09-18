# Cycle de vie des abonnements · lot H

Engagement, résiliation avec préavis et liste de sortie, nouveaux statuts, devis, prélèvement
automatique, gel encadré, prorata explicite, changements planifiés, alignement des échéances. Tout
sous `/sni/api/v1/subscriptions/...` (ADMIN, SUPER_ADMIN, STAFF ; la politique en écriture et la
dispense de frais sont réservées aux ADMIN). Package `features/subscription/lifecycle/`.

## La politique · une ligne, des seuils qui sont des données

`GET|PUT /subscriptions/lifecycle/policy` · `subscription_policy`, ligne `DEFAULT` :

| champ | défaut | ce qu'il gouverne |
|---|---|---|
| `prorationPolicy` | `DAILY` | `DAILY`, `MONTH_STARTED` (tout mois entamé est dû), `NONE` (la période entière est due, rien n'est rendu). **Le même pour l'entrée, la sortie et le changement de plan.** |
| `defaultNoticeDays` | 30 | préavis de résiliation |
| `earlyTerminationFormula` / `Percent` / `FixedFee` | `PERCENT_OF_REMAINING` 50 % | ce que coûte de rompre un engagement · `NONE`, `FIXED_FEE`, `PERCENT_OF_REMAINING`, `REMAINING_PERIODS` |
| `freezeMaxPerYear` / `freezeMaxDays` / `freezeFeePercent` | 2 / 60 / 0 | le gel · quota sur douze mois glissants, durée max, part du tarif facturée pendant |
| `gracePeriodDays` / `suspensionAfterGraceDays` | 7 / 14 | tolérance après échéance impayée, puis suspension |
| `quoteValidityDays` | 30 | validité d'un devis sans date |

## Les statuts ajoutés

- **`PENDING_DOCUMENTS`** · l'activation constate que le niveau KYC du plan n'est pas atteint. Avant,
  l'abonnement était activé avec un avertissement ; désormais il attend. Le worker horaire retente
  l'activation · si les pièces sont arrivées, il s'active seul.
- **`GRACE_PERIOD`** · une échéance de renouvellement impayée depuis `gracePeriodDays`. Les droits
  restent ouverts, l'abonné est prévenu (`SUBSCRIPTION_GRACE_PERIOD`). Un paiement le rend `ACTIVE`
  sans intervention (`PaymentTransactionWorkflowProcessor`, prélèvement réussi) ; au bout de
  `suspensionAfterGraceDays`, suspension. `POST /lifecycle/{n}/grace/exit` pour une régularisation
  constatée hors système.
- **`PENDING_TERMINATION`** · un préavis court. L'abonnement vit jusqu'à sa date d'effet et **ne se
  renouvelle pas** : les jours au-delà de la période payée sont facturés au prorata à l'acceptation.

`SubscriptionStatus.entitled()` dit quels statuts gardent leurs droits, `open()` lesquels ne sont
pas terminés.

## Engagement

`subscription_commitment` · posé automatiquement à la souscription quand le prix de plan porte
`commitment_months`, par un devis, ou à la main (`PUT /lifecycle/{n}/commitment`). La formule de
rupture vient de la politique si l'engagement n'en fixe pas.

`GET /lifecycle/{n}/commitment/early-termination?on=` · ce que coûterait de partir à cette date :
mois restants (tout mois entamé compte), frais selon la formule, sur l'équivalent mensuel du prix
(annuel / 12, trimestriel / 3). Au terme, l'engagement se reconduit s'il l'a dit
(`autoRenewCommitment`), sinon il tombe et l'abonnement continue sans engagement.

## Résiliation · `/subscriptions/terminations`

```
POST /subscriptions/{n}/preview      date d'effet la plus proche, frais de rupture, raccordement, liste de sortie
POST /subscriptions/{n}              pose le préavis · PENDING_TERMINATION
GET  /subscriptions/{n}              l'historique
POST /{code}/accept                  facture les frais (sauf dispense) et le raccordement
POST /{code}/retract                 l'abonné reste · retour ACTIVE · refusé si des frais ont été facturés (avoir d'abord)
POST /{code}/waive-fee               ADMIN · motivé · avant l'acceptation
POST /{code}/exit-items/{item}/done  coche une ligne
POST /{code}/exit-items/{item}/waive dispense une ligne, motivée
POST /{code}/complete                achève avant la date d'effet si la liste est vide
GET  /?status=                       par statut
GET  /blocked                        date d'effet passée, liste encore ouverte
```

Trois règles :

1. **La date d'effet ne peut pas précéder la fin du préavis.** Elle peut être plus tard · c'est
   l'abonné qui choisit. `noticePeriodDays` du corps permet un préavis dérogatoire.
2. **Les frais de rupture sont chiffrés à la demande et facturés à l'acceptation seulement**,
   parce qu'une demande peut se rétracter. Une dispense se décide avant l'acceptation, motivée,
   signée.
3. **L'abonnement ne se ferme pas tant qu'une ligne obligatoire de la liste de sortie est
   ouverte.** Le worker horaire achève les préavis arrivés à leur date d'effet ; ce qui bloque se
   voit dans `/blocked`.

La liste de sortie est construite à la demande : badge (obligatoire), clés (facultatif), solde
réglé (obligatoire, **se coche seul** quand aucune facture n'est ouverte), caution (si un hold
existe), frais de rupture (si dus, se coche seul quand réglés), et pour un domicilié : courrier en
attente (se coche seul quand il n'y en a plus) et contrat de domiciliation clos. L'achèvement
appelle `SubscriptionLifecycleOperator.cancel` · droits révoqués, contrat, caution, comme avant.

## Devis · `/subscriptions/quotes`

`POST /` fige plan, rythme, prix (catalogue par défaut), frais d'entrée, engagement, essai, date de
début, validité. **Un prix négocié est vérifié dès le devis contre les règles de dérivation**
(plancher, maximum admis) · `GET /{n}/outlook` dit l'écart et si un visa sera nécessaire.
`POST /{n}/send` envoie (`SUBSCRIPTION_QUOTE_SENT`). `POST /{n}/accept` convertit : l'abonnement
est créé (sans auto-activation · le paiement active), la dérivation `NEGOTIATION` posée si le prix
diffère du catalogue (avec ses règles : elle peut attendre un visa), l'engagement posé si le devis
en porte. Un devis périmé ne se convertit pas ; le worker expire les devis ouverts dépassés.
`proposalNumber` relie à la proposition commerciale du CRM quand il y en a une.

## Prélèvement automatique · `/subscriptions/lifecycle/{n}/debit-mandate`

**Sans mandat, rien n'est prélevé.** Un mandat dit qui a consenti, par quel canal (`PORTAL`,
`SIGNED_FORM`, `EMAIL`, `STAFF`), avec quelle référence, sur quel portefeuille (qui doit être au
souscripteur), et peut plafonner chaque prélèvement. Un seul mandat vivant par abonnement ; il se
révoque, jamais ne s'efface.

Au renouvellement, une fois la facture validée en base (`afterCommit`), l'échéance est tentée une
fois, dans sa propre transaction (`DebitGateway`, `REQUIRES_NEW`) · un échec ne défait pas le
renouvellement. Chaque tentative est écrite (`subscription_debit_attempt`) : `SUCCEEDED`,
`INSUFFICIENT_FUNDS` (vérifié avant de tenter), `OVER_LIMIT`, `FAILED`. Un échec ouvre la
tolérance et prévient (`SUBSCRIPTION_DIRECT_DEBIT_FAILED`) ; **trois échecs de suite suspendent le
mandat** (`/debit-mandates/{code}/reactivate` quand l'abonné a rechargé). Un succès ferme la
tolérance. `POST /{n}/debit-mandate/collect/{invoice}` retente à la main.

## Gel encadré

`pause` passe désormais par `SubscriptionFreezeService.start` : quota sur douze mois glissants,
durée maximale, un seul gel ouvert. Si `freezeFeePercent` > 0, la part du tarif sur les jours gelés
est facturée à l'ouverture (`FREEZE_FEE`). Chaque gel est écrit (`subscription_freeze`) avec ses
jours prévus et effectifs ; `resume` le ferme. `GET /lifecycle/{n}/freezes` et
`/freezes/allowance` (ce que la politique permet encore). `freezeNoticeDays` est porté par la
politique pour le libre-service du lot I · le guichet gèle sans préavis.

## Prorata explicite et changements planifiés

`SubscriptionChangeRequestServiceImpl.apply` appelle `PlanChangeProrationService` : le reste à
courir à l'ancien prix est rendu, le reste à courir au nouveau prix est dû ; **la différence est
facturée à la hausse (`PLAN_CHANGE_PRORATION`), créditée au portefeuille à la baisse** (écriture
`ADJUSTMENT`, une avance client · pas un avoir à rapprocher). Avec `NONE`, rien ne bouge avant la
prochaine échéance. La politique appliquée est mémorisée sur le changement (`proration_policy`,
`proration_credit`, `proration_billable_number`). `GET /lifecycle/{n}/plan-change/preview` dit
d'avance. Les changements planifiés existaient (`NEXT_BILLING_PERIOD`, worker) · ils passent
désormais par le même prorata.

## Alignement des échéances

`POST /lifecycle/align` `{subscriberType, subscriberCode, targetPeriodEnd?, simulateOnly=true}` ·
les abonnements ouverts d'un souscripteur convergent vers une même fin de période (par défaut la
plus tardive). **On n'aligne que vers l'avant** : le raccordement est facturé au prorata journalier
(`ALIGNMENT_BRIDGING`) ; un abonnement qu'une date plus tôt raccourcirait est laissé tel quel et
dit, parce qu'aligner vers l'arrière demanderait de rendre de l'argent.

## Worker

`SubscriptionLifecycleWorker`, horaire (`bokati.subscription.workers.lifecycle-delay-ms`) : préavis
achevés, devis expirés, tolérance (entrées et suspensions), engagements reconduits, activations
après pièces.

## Schéma

`V241` (dev) / `V237` (prod) : `subscription_policy` (seed `DEFAULT`), `subscription_commitment`,
`subscription_termination` (`uk_subscription_termination_open` : un préavis en cours par
abonnement), `subscription_exit_item`, `subscription_quote`, `subscription_debit_mandate`
(`uk_subscription_debit_mandate_active`), `subscription_debit_attempt`, `subscription_freeze` ;
colonnes `proration_policy`, `proration_credit`, `proration_billable_number` sur
`subscription_change_request` ; séquences `subscription_termination` (`TRM-{YYYY}-{SEQ}`),
`subscription_quote` (`QTE-{YYYY}-{SEQ}`), `subscription_debit_mandate` (`MDT-{SEQ}`). Aucune
contrainte sur `subscription.status` · les nouveaux statuts n'ont pas demandé de DDL.

## Ce qui reste ouvert

- Les gabarits d'e-mail des nouveaux événements (`SUBSCRIPTION_TERMINATION_*`,
  `SUBSCRIPTION_GRACE_PERIOD`, `SUBSCRIPTION_DIRECT_DEBIT_FAILED`, `SUBSCRIPTION_QUOTE_SENT`)
  restent à créer côté notification, comme pour les lots précédents.
- `DunningService.scheduleForFailedIntent` n'est appelé nulle part · la relance mobile money reste
  à brancher sur l'échec de dépôt PawaPay. La tolérance ne dépend pas d'elle.
- Le retrait d'un préavis après facturation demande un avoir manuel · `createCreditNote` existe.
- Le devis n'a pas encore de PDF · le CRM en produit un pour ses propositions.
