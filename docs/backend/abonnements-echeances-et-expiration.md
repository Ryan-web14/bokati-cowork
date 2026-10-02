# Abonnements · pourquoi la facturation s'était arrêtée, et pourquoi rien n'expirait

## Les deux symptômes constatés en production

1. Les échéances de renouvellement ne produisaient plus de facture.
2. Un abonnement dont la période s'achevait le **07/09** était encore `ACTIVE` le **01/10**, droits
   ouverts, sans qu'aucune facture n'ait été émise ni qu'aucune relance n'ait été envoyée.

Ce sont deux défauts distincts. Le second n'est pas une conséquence du premier : il aurait existé
même si la facturation avait fonctionné.

## Défaut 1 · une seule transaction pour tout le balayage

```java
// SubscriptionLifecycleOperator, avant
public int renewDueSubscriptions() {
    List<Subscription> subscriptions = ...;   // les échéances du jour
    subscriptions.forEach(this::renewActive); // aucun rattrapage par abonnement
    return subscriptions.size();              // annonce un succès qui n'a pas eu lieu
}
```

`SubscriptionServiceImpl` est `@Transactional` au niveau de la classe et `renewDueSubscriptions()`
ne redéfinissait rien : **tout le balayage tenait dans une seule transaction**. Un abonnement qui
jetait · un montant récurrent absent, un `planVersion` devenu introuvable, un prélèvement refusé ·
annulait la passe entière, y compris les renouvellements déjà écrits.

`SubscriptionRenewalWorker` attrapait l'exception, journalisait `SubscriptionRenewalWorker failed`,
et recommençait un quart d'heure plus tard **sur la même liste, dans le même ordre, avec le même
abonnement en tête**. Plus rien n'était facturé, pour personne, indéfiniment.

Le compte rendu aggravait le diagnostic : la méthode rendait `subscriptions.size()`, donc le
worker annonçait « Renewed N due subscriptions » pour des renouvellements qui venaient d'être
annulés par le rollback.

**Corrigé** · chaque abonnement a sa propre transaction (`renewOne`, `REQUIRES_NEW`), le balayage
n'en porte plus aucune (`NOT_SUPPORTED`), et l'échec d'un abonnement est journalisé **avec son
numéro** avant de passer au suivant. Le compte rendu ne compte plus que ce qui a abouti.

`cancelEndedSubscriptions()` avait la même structure et reçoit le même traitement.

## Le calendrier de fin d'abonnement

Un abonnement qui ne se reconduit pas tout seul s'arrete a la fin de sa periode, et l'abonne doit
le savoir avant. `SubscriptionEndNoticeService`, du point de vue du client :

| Quand | Ce qui part | Ce que dit le message |
|---|---|---|
| **J-7** | `SUBSCRIPTION_ENDING_SOON` | « Votre abonnement se termine le … », et quoi faire pour continuer |
| **J-3** | `SUBSCRIPTION_ENDING_SOON` | Le rappel |
| **Le jour meme** | `SUBSCRIPTION_ENDS_TODAY` | « Il prend fin ce soir a minuit » |
| **Le lendemain** | `SUBSCRIPTION_ENDED` | L'abonnement passe `CANCELLED`, les acces sont clos |

**Qui recoit ces avis** · uniquement les abonnements qui prennent reellement fin : pas de
reconduction automatique (`auto_renew = false`), ou une cloture de fin de periode demandee
(`cancel_at_period_end = true`). Un abonnement en reconduction automatique ne prend pas fin, il se
renouvelle · lui annoncer une fin serait faux. Quand sa reconduction echoue, c'est le filet du
defaut 2 ci-dessous qui le rattrape.

**Le client renouvelle depuis son espace.**

```http
POST /api/v1/client/subscriptions/{subscriptionNumber}/renew
```

Ouvert dans les **sept jours** qui précèdent la fin, c'est-à-dire à partir du premier avis · avant,
un `400` dit à quelle date ce sera possible. Reconduire un abonnement qui court encore un mois
avancerait sa période sans raison, et le client paierait une échéance qu'il ne devait pas encore.

La réponse porte la période reconduite **et la facture de renouvellement**, pour que l'écran
propose de la régler dans le même mouvement. Sans elle, le client devait aller la chercher dans sa
liste de factures, et beaucoup repartaient sans payer.

Les avis de J-7, J-3 et du jour même deviennent donc actionnables : ils disent quoi faire, et le
client peut le faire.

**Renouveler emet une facture** · c'est le renouvellement qui facture, jamais l'avis. Les avis
disent quoi faire, ils n'engagent rien : personne ne recoit de facture pour une periode qu'il n'a
pas demandee. `PATCH /subscriptions/{n}/renew` fait avancer la periode et emet la facture de
renouvellement, comme pour une reconduction automatique.

**Un avis ne part qu'une fois.** Le balayage passe toutes les heures ; `subscription.end_notice_stage`
retient le jalon le plus urgent deja envoye (7, puis 3, puis 0). Un renouvellement le remet a nul,
la periode suivante repart de zero. Un abonnement vu pour la premiere fois a deux jours de sa fin
recoit l'avis de J-3, pas la serie des trois d'un coup.

## Défaut 2 · le filet des reconductions automatiques en échec

Trois chemins mènent à la fin d'une période. Aucun ne se refermait :

| Balayage | Ce qu'il prend | Ce qu'il laisse passer |
|---|---|---|
| `renewDueSubscriptions` | `auto_renew = true` | Un abonnement **sans reconduction automatique** n'est ni facturé, ni clos |
| `cancelEndedSubscriptions` | `cancel_at_period_end = true` | Tous ceux qui ne l'avaient pas demandé |
| `SubscriptionGraceService.sweep` | ceux qui portent une **facture de renouvellement impayée** | Quand le renouvellement n'a jamais produit de facture, la tolérance ne se déclenche jamais |

Et `SubscriptionStatus.EXPIRED` n'était **écrit par personne** : l'état existait dans l'énumération
sans qu'aucun code ne l'attribue jamais.

Net : période terminée + pas de reconduction + pas de `cancelAtPeriodEnd` = `ACTIVE` pour toujours.
C'est exactement l'abonnement du 07/09.

**Corrigé** · le calendrier ci-dessus couvre les abonnements qui prennent fin. Reste le cas d'un
abonnement **en reconduction automatique dont le renouvellement échoue** : il ne doit pas être
fermé le lendemain, puisque le client veut continuer et que c'est nous qui avons manqué. C'est
`SubscriptionExpiryService` qui le rattrape, avec un délai plus long.

Il part de `findEndedPeriodsStillOpen` : les abonnements encore en cours, `auto_renew = true`,
`cancel_at_period_end = false`, dont `current_period_end` est derrière nous. Les deux balayages ne
se recouvrent donc jamais.

Le délai est court, et volontairement asymétrique avec les abonnements qui prennent fin : la
reconduction a échoué de notre fait, l'abonné n'y est pour rien, il ne doit pas être fermé du jour
au lendemain · mais il ne doit pas non plus bénéficier de semaines de service gratuit.

| Retard après la fin de période | Ce qui se passe |
|---|---|
| dès le lendemain | Entrée en `GRACE_PERIOD`, le client est prévenu |
| au-delà de `gracePeriodDays` (7 j) | `EXPIRED`, avec le motif et le nombre de jours de retard |

`gracePeriodDays` est un champ de la politique · le délai se règle en base, sans déploiement.

Un règlement ou un renouvellement entre-temps fait avancer la période, et l'abonnement sort du
balayage de lui-même. Le service revérifie la date **dans sa propre transaction** avant d'écrire,
pour ne pas fermer un abonnement renouvelé entre la lecture et l'écriture.

Un préavis de résiliation en cours (`PENDING_TERMINATION`) est laissé tranquille : il a sa propre
date d'effet.

## Défaut 3 · une facture scellée n'était pas « impayée »

`findActiveWithRenewalUnpaidSince` filtrait
`bd.status IN ('ISSUED','SENT','PARTIALLY_PAID','OVERDUE')` · **`VALIDATED` et `VIEWED` manquaient**.
Une facture de renouvellement scellée (SEFC) ne déclenchait donc jamais la tolérance. C'est la même
omission que celle corrigée dans `BillingDocumentRepository` (voir
`docs/backend/ce-qui-est-du.md`) ; cette requête-là vit dans `SubscriptionRepository` et avait été
oubliée.

## Vérifier sur la production

Le défaut 1 laisse une trace franche dans les journaux, toutes les 15 minutes :

```
ERROR ... SubscriptionRenewalWorker : SubscriptionRenewalWorker failed: <cause>
```

La cause nommée là est l'abonnement qui bloquait tout. Après ce correctif, le message devient
nominatif :

```
ERROR ... SubscriptionLifecycleOperator : Abonnement SUB-… · renouvellement impossible, les suivants continuent
WARN  ... SubscriptionLifecycleOperator : Echeances d'abonnement · 12 facture(s), 1 en echec a reprendre a la main
```

Les abonnements restés ouverts sur une période échue :

```sql
SELECT subscription_number, status, auto_renew, cancel_at_period_end,
       current_period_end, next_billing_date,
       CURRENT_DATE - current_period_end AS jours_de_retard
FROM subscription
WHERE status IN ('ACTIVE','TRIALING','GRACE_PERIOD','PAST_DUE')
  AND current_period_end < CURRENT_DATE
ORDER BY current_period_end;
```

## Migrations

- `V250` (dev) / `V246` (prod) · `subscription.end_notice_stage` et l'index du balayage.
- `V251` (dev) / `V247` (prod) · les trois gabarits de courriel.

## Rattrapage

Au premier passage du worker de cycle de vie (toutes les heures, 2 min après le démarrage), les
abonnements de cette liste sont traités selon leur nature :

- **sans reconduction automatique** · leur fin est déjà passée, ils passent directement
  `CANCELLED` avec l'avis de fin. C'est le cas de celui du 07/09. Les avis de J-7 et J-3 ne
  rattrapent pas le passé · il n'y a plus rien à annoncer, la fin est derrière nous ;
- **en reconduction automatique** · ils entrent en tolérance puis passent `EXPIRED` au-delà de 7
  jours de retard.

C'est voulu, mais ce n'est pas anodin : ces abonnements n'ont jamais été facturés, donc le client
n'a jamais reçu de demande de règlement pour la période échue. Avant le déploiement, décidez pour
chacun :

- **le fermer** · ne rien faire, le worker s'en charge ;
- **le facturer puis le laisser suivre le cycle normal** · émettre la facture de renouvellement
  manquante, ce qui fait avancer la période et le sort du balayage ;
- **le prolonger** · corriger `current_period_end` et `next_billing_date` à la main.

La liste ci-dessus donne de quoi trancher. Elle devrait être courte : ce sont les abonnements
restés en suspens depuis l'arrêt de la facturation.
