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

Le contrat reste un seul et même contrat tant que l'abonnement n'a pas pris fin : il n'expire pas, et
il garde toute son histoire.

### Versions du contrat

`GET /contracts/{code}/versions` donne l'historique du contrat, du plus ancien au plus récent. La
dernière ligne est la version en vigueur. Le même tableau est dans `versions` sur la fiche de contrat
d'une organisation (`GET /companies/{id}/contracts`) et sur le détail du portail
(`GET /client/documents/contracts/{code}`).

| Champ | Sens |
| --- | --- |
| `version` | 1 pour l'original, puis +1 à chaque changement |
| `kind` | `ORIGINAL`, `AMENDMENT` (avenant) ou `EXTENSION` (date repoussée sans avenant) |
| `amendmentCode` | le code de l'avenant, vide sinon |
| `description` | l'objet de l'avenant ou le motif de l'extension |
| `effectiveDate` | la date d'effet |
| `previousEndDate`, `endDate` | la fin du contrat avant et après ce changement |
| `at` | quand il a été appliqué |
| `documentCode` | le PDF de cette version, vide pour une extension |

Le PDF d'un avenant se télécharge dans le portail par
`GET /client/documents/contracts/{code}/amendments/{amendmentCode}/pdf`.

- Les avenants se lisent sur `GET /contracts/{code}/amendments` (voir
  [09-contrats-domiciliation.md](./09-contrats-domiciliation.md)). Celui d'un renouvellement porte les
  variables `periode_debut`, `periode_fin` et `prochaine_facturation`, avec l'ancienne et la nouvelle
  valeur, et son PDF est généré.
- Un contrat ne passe jamais `EXPIRED` tant que son abonnement n'a pas pris fin, même pendant
  l'attente d'un paiement. Sa date de fin peut alors être dépassée de quelques jours. Il suit
  l'abonnement quand celui-ci prend fin.
- Un contrat `AMENDED` arrive désormais à expiration et se résilie comme un contrat actif.
- Les anciens avenants de renouvellement restés en examen sont annulés au prochain renouvellement.

## Abonnements en cours au déploiement

Une facture de renouvellement déjà émise avant cette version garde l'ancienne règle : sa période a
déjà avancé, et son paiement fait seulement sortir de la tolérance.

Les contrats restés derrière leur abonnement sont rattrapés automatiquement toutes les 30 minutes :
un contrat marqué `EXPIRED` alors que l'abonnement court redevient en vigueur, et un seul avenant du
même contrat couvre tout l'écart, jusqu'à la fin de la période en cours. Aucun nouveau contrat n'est
créé. Avec une facture de renouvellement impayée, le contrat redevient en vigueur, mais l'avenant et
ses dates attendent le paiement.
