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

## Défaut 2 · rien ne fermait une période échue

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

**Corrigé** · `SubscriptionExpiryService`, branché dans `SubscriptionLifecycleWorker` juste après la
tolérance. Il part de `findEndedPeriodsStillOpen` : tout abonnement encore en cours
(`ACTIVE`, `TRIALING`, `GRACE_PERIOD`, `PAST_DUE`) dont `current_period_end` est derrière nous.

Les délais sont ceux que la politique porte déjà · aucune configuration nouvelle :

| Retard après la fin de période | Ce qui se passe |
|---|---|
| jusqu'à `gracePeriodDays` (7 j) | Rien · le client a le temps de régler |
| au-delà de `gracePeriodDays` | Entrée en `GRACE_PERIOD`, le client est prévenu |
| au-delà de `gracePeriodDays + suspensionAfterGraceDays` (21 j) | `EXPIRED`, avec le motif et le nombre de jours de retard |

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

## Rattrapage

Au premier passage du worker de cycle de vie (toutes les heures, 2 min après le démarrage), les
abonnements de cette liste seront prévenus puis fermés selon leur retard. **Ceux de plus de 21
jours passeront directement en `EXPIRED`** · y compris celui du 07/09.

C'est voulu, mais ce n'est pas anodin : ces abonnements n'ont jamais été facturés, donc le client
n'a jamais reçu de demande de règlement pour la période échue. Avant le déploiement, décidez pour
chacun :

- **le fermer** · ne rien faire, le worker s'en charge ;
- **le facturer puis le laisser suivre le cycle normal** · émettre la facture de renouvellement
  manquante, ce qui fait avancer la période et le sort du balayage ;
- **le prolonger** · corriger `current_period_end` et `next_billing_date` à la main.

La liste ci-dessus donne de quoi trancher. Elle devrait être courte : ce sont les abonnements
restés en suspens depuis l'arrêt de la facturation.
