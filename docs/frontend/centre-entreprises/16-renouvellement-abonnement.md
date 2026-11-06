# Renouvellement d'abonnement : payé d'abord, contrat ensuite

Préfixe des routes : `/sni/api/v1`.

Un abonnement ne se renouvelle plus à l'émission de la facture, mais à son paiement. La période, les
droits, le statut actif et le contrat suivent le règlement.

## Déroulé

1. **À l'échéance** (le lendemain de `currentPeriodEnd`), ou quand le client demande le
   renouvellement dans les 7 jours avant la fin : la facture de la période à venir est émise. Rien
   d'autre ne bouge. `pendingRenewalInvoice`, `pendingRenewalStart` et `pendingRenewalEnd` sont
   renseignés.
2. **Avec un mandat de prélèvement actif**, le portefeuille est prélevé aussitôt. Sans mandat, rien
   n'est débité : seul un mandat autorise le prélèvement.
3. **Sans paiement**, l'abonné est prévenu par e-mail et dans le portail
   (`SUBSCRIPTION_RENEWAL_PAYMENT_DUE`). Si un prélèvement a échoué, c'est son message
   (`SUBSCRIPTION_DIRECT_DEBIT_FAILED`) qui fait foi : pas de second avis.
4. **Une fois la période finie**, l'abonnement passe en `GRACE_PERIOD`, puis `SUSPENDED` après le
   délai de la politique (`suspensionAfterGraceDays`), puis `EXPIRED` après `renewalExpiryDays`
   comptés depuis la fin de période. À l'expiration, la facture impayée est annulée.
5. **Au paiement complet**, quel que soit le moyen (portefeuille, mobile money, caisse) : nouvelle
   période, statut `ACTIVE`, nouveaux droits, avenant au contrat, notification
   `SUBSCRIPTION_RENEWED`.

Tant que la facture n'est pas soldée, aucun droit de la nouvelle période n'est ouvert. Un paiement
partiel ne change rien.

## Dates de la nouvelle période

| Paiement | Début de la nouvelle période |
| --- | --- |
| Avant ou le jour de l'échéance | le lendemain de la période en cours : aucun jour perdu |
| En retard, tolérance ou suspension comprises | le jour du paiement : l'abonné reçoit toute la durée payée |

## Réponse d'abonnement

Trois champs s'ajoutent à `SubscriptionResponse` :

| Champ | Sens |
| --- | --- |
| `pendingRenewalInvoice` | la facture à régler, vide si rien n'attend |
| `pendingRenewalStart`, `pendingRenewalEnd` | la période prévue. Payée en retard, elle repart du jour du paiement |

À afficher dans le portail : quand `pendingRenewalInvoice` est renseigné, un appel à régler cette
facture, y compris en `GRACE_PERIOD` et `SUSPENDED`.

`POST /client/subscriptions/{n}/renew` et `PATCH /subscriptions/{n}/renew` :

- renvoient l'abonnement encore sur sa période en cours, avec le renouvellement en attente ;
- ne créent pas de seconde facture si un renouvellement attend déjà.

Ne plus annoncer « renouvelé » après cet appel, mais « facture à régler ». Le renouvellement est
acquis quand `pendingRenewalInvoice` redevient vide.

## Contrat

Le contrat reste le même : aucun nouveau contrat n'est créé.

| Cas | Effet sur le contrat |
| --- | --- |
| Renouvellement payé, y compris anticipé | un avenant du même contrat, appliqué sans examen ni signature, repousse `endDate` ; statut `AMENDED` |
| Extension en cours de période (reprise après pause, alignement des échéances, paiement en retard) | `endDate` est repoussée, sans avenant ; événement d'audit `CONTRACT_TERM_EXTENDED` |
| Contrat pas encore signé | `endDate` est repoussée, sans avenant |
| Contrat résilié ou annulé | rien |

- Les avenants se lisent sur `GET /contracts/{code}/amendments` (voir
  [09-contrats-domiciliation.md](./09-contrats-domiciliation.md)). Celui d'un renouvellement porte les
  variables `periode_debut`, `periode_fin` et `prochaine_facturation`, avec l'ancienne et la nouvelle
  valeur, et son PDF est généré.
- Un contrat arrivé à son terme pendant l'attente du paiement passe `EXPIRED`, puis redevient en
  vigueur avec l'avenant.
- Un contrat `AMENDED` arrive désormais à expiration et se résilie comme un contrat actif.
- Les anciens avenants de renouvellement restés en examen sont annulés au prochain renouvellement.

## Abonnements en cours au déploiement

Une facture de renouvellement déjà émise avant cette version garde l'ancienne règle : sa période a
déjà avancé, et son paiement fait seulement sortir de la tolérance.

Les contrats restés derrière leur abonnement sont rattrapés automatiquement toutes les 30 minutes :
un seul avenant du même contrat couvre tout l'écart, jusqu'à la fin de la période en cours, et un
contrat échu redevient en vigueur (`AMENDED`). Aucun nouveau contrat n'est créé. Un abonnement qui a
une facture de renouvellement impayée attend son paiement.
