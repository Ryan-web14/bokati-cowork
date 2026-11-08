# subscription : parcours du cycle de vie

> Partie de [README.md](README.md). Routes exactes et champs : [routes.md](routes.md). Règles communes : [README racine](../README.md).

Les routes sous `/sni/api/v1/subscriptions/lifecycle/`, `/subscriptions/terminations/` exigent, en plus du droit indiqué, le rôle `ROLE_ADMIN`, `ROLE_SUPER_ADMIN` ou `ROLE_STAFF`. L'acteur y est pris dans le jeton. Sur les routes de `SubscriptionController` (`activate`, `suspend`, `pause`, `resume`, `cancel`), l'acteur est le champ `changedBy` du corps, `SYSTEM` s'il manque.

Les valeurs entre crochets (`[gracePeriodDays]`…) sont celles de la [politique d'abonnement](parcours-catalogue.md#politique-dabonnement).

## Renouvellement automatique et paiement

Concerne les abonnements `autoRenew: true`. Le renouvellement **émet une facture** ; la nouvelle période, les droits et l'avenant au contrat ne prennent effet **qu'au paiement**.

Calendrier, avec D le dernier jour de la période (`currentPeriodEnd`) et les valeurs par défaut de la politique :

| Moment | Ce que fait le serveur | Ce que voit l'écran |
| --- | --- | --- |
| D+1 (`nextBillingDate`), premier passage du worker de renouvellement (15 min) | Applique d'éventuels changements de tarif négocié (retour au catalogue d'une dérivation échue). Facture la période D+1 à D+1+cycle. Si un mandat actif existe, tente le prélèvement. Si rien n'a été prélevé, prévient l'abonné (courriel `SUBSCRIPTION_RENEWAL_PAYMENT_DUE` et notification in-app avec la facture, la période et le montant). Comme la période est finie, l'abonnement passe `GRACE_PERIOD` sans second message. | `pendingRenewalInvoice`, `pendingRenewalStart`, `pendingRenewalEnd` remplis ; `currentPeriodStart` et `currentPeriodEnd` **inchangés** ; statut `GRACE_PERIOD`. |
| Paiement complet de la facture, à tout moment avant l'expiration | La période commence : le jour prévu si le paiement est à l'heure ou en avance, **le jour du paiement** s'il est en retard (l'abonné reçoit toute la durée payée). Statut `ACTIVE`, champs `pendingRenewal*` vidés, droits de la nouvelle période ouverts, avenant de renouvellement au contrat, courriel et notification de renouvellement. | Nouvelle période, `nextBillingDate` décalée d'autant. |
| [suspensionAfterGraceDays] = 3 jours après l'entrée en tolérance (worker horaire) | `SUSPENDED`, échéancier en pause, courriel de suspension. | statut `SUSPENDED`, `pendingRenewalInvoice` toujours rempli : le paiement reste possible et réactive. |
| Après [renewalExpiryDays] = 7 jours depuis D (worker horaire) | `EXPIRED`. La facture de renouvellement est annulée et archivée si rien n'a été payé dessus ; si elle est en partie payée, elle est laissée à traiter à la main. | statut `EXPIRED`, abonnement terminé. |

Le paiement peut venir du portail (`POST /sni/api/v1/client/subscriptions/{n}/pay/wallet` ou `/pay/mobile-money`, module [portal](../portal/README.md)), de la facture dans [billing](../billing/README.md), ou du prélèvement. Quand le paiement n'est pas rattaché directement à l'abonnement, le worker de renouvellement constate la facture soldée et applique le renouvellement au plus tard 15 minutes plus tard. Un paiement partiel ne change rien.

Un abonnement `ACTIVE` dont une échéance est payée en retard est aussi recalé : sa période repart du jour du paiement.

Si le renouvellement n'a rien à facturer (montant nul ou entièrement couvert par un avoir), la nouvelle période commence aussitôt.

Écran conseillé : sur la fiche, un bandeau « Renouvellement du `pendingRenewalStart` au `pendingRenewalEnd` en attente du paiement de la facture `pendingRenewalInvoice` », avec un lien vers la facture et, si un mandat existe, le bouton « Prélever maintenant » (voir plus bas).

## Renouveler à la main

Pour faire payer la période suivante avant l'échéance, ou pour un abonnement sans reconduction automatique.

1. `PATCH /sni/api/v1/subscriptions/{subscriptionNumber}/renew`, sans corps ([route](routes/SubscriptionController.md#r-patch-sni-api-v1-subscriptions-subscriptionnumber-renew)). Seulement depuis `ACTIVE` (`409` sinon).
2. Le serveur facture la période qui suit la période courante et remplit les champs `pendingRenewal*`. Si une facture de renouvellement attend déjà, la route ne fait rien et rend l'abonnement tel quel.
3. Le paiement fait prendre effet la nouvelle période comme ci-dessus. Payé avant la fin, la nouvelle période commence le lendemain de la fin actuelle.

Attention : tant que la nouvelle période n'a pas commencé, `GET /subscriptions/current` ne trouve plus l'abonnement (voir [Écarts](README.md#écarts-relevés)).

## Abonnement sans reconduction automatique

Concerne `autoRenew: false`, ou `cancelAtPeriodEnd: true`. Le worker horaire prévient puis clôt :

- 7 jours avant D : courriel `SUBSCRIPTION_ENDING_SOON` (« se termine dans 7 jours ») ;
- 3 jours avant D : même courriel, rappel ;
- le jour D : `SUBSCRIPTION_ENDS_TODAY` (« prend fin ce soir ») ;
- D+1 : `CANCELLED`, courriel `SUBSCRIPTION_ENDED`, facture de renouvellement non payée annulée.

Un abonnement vu pour la première fois à 3 jours de sa fin ne reçoit que l'avis de J-3. Renouveler (route ci-dessus) et payer avant D+1 fait repartir la période et remet les avis à zéro. Pour `cancelAtPeriodEnd: true`, voir aussi [Annuler](#annuler).

## Tolérance

`GRACE_PERIOD` laisse les droits ouverts pendant que l'impayé se règle.

- Entrée automatique : facture de renouvellement impayée depuis plus de [gracePeriodDays] (worker horaire), période échue non renouvelée, ou prélèvement en échec hors renouvellement. Un courriel `SUBSCRIPTION_GRACE_PERIOD` indique le nombre de jours avant suspension (sauf quand l'entrée suit un renouvellement, qui a déjà son message).
- Sortie automatique : paiement, ou prélèvement réussi.
- Sortie manuelle, quand le règlement a été constaté hors système : `POST /sni/api/v1/subscriptions/lifecycle/{subscriptionNumber}/grace/exit` avec `{ "reason": "..." }` facultatif ([route](routes/SubscriptionLifecycleController.md#r-post-sni-api-v1-subscriptions-lifecycle-subscriptionnumber-grace-exit)). Réponse `204` vide, même si l'abonnement n'était pas en tolérance. **Si une facture de renouvellement reste due, la sortie manuelle ne fait pas prendre effet le renouvellement** : le worker remet l'abonnement en tolérance au passage suivant. Pour un renouvellement, il faut que la facture soit soldée.

## Mandat de prélèvement

Le prélèvement débite le portefeuille du souscripteur ([payment](../payment/README.md)) à chaque échéance. Sans mandat, rien n'est prélevé.

1. Voir les mandats : `GET /sni/api/v1/subscriptions/lifecycle/{subscriptionNumber}/debit-mandate` (liste, plus récent d'abord ; un seul peut être non révoqué).
2. Enregistrer l'accord : `POST .../{subscriptionNumber}/debit-mandate` ([route](routes/SubscriptionLifecycleController.md#r-post-sni-api-v1-subscriptions-lifecycle-subscriptionnumber-debit-mandate)) avec `walletNumber` (portefeuille du souscripteur, `409` sinon), `channel` (`PORTAL`, `SIGNED_FORM`, `EMAIL`, `STAFF`), `reference` (référence du formulaire signé, par exemple), `maxAmountPerDebit` (plafond par prélèvement, facultatif, positif). Réponse `MandateView` `ACTIVE`.
3. À chaque facture de renouvellement, une tentative unique est faite : `SUCCEEDED`, `INSUFFICIENT_FUNDS`, `OVER_LIMIT` (montant au-delà du plafond) ou `FAILED`. Un échec envoie un courriel `SUBSCRIPTION_DIRECT_DEBIT_FAILED`. Trois échecs de suite suspendent le mandat (`SUSPENDED`).
4. Tentatives : `GET .../{subscriptionNumber}/debit-attempts` (`invoiceNumber`, `amount`, `status`, `message`, `transactionNumber`).
5. Prélever maintenant une facture restée due : `POST .../{subscriptionNumber}/debit-mandate/collect/{invoiceNumber}`. Rend la tentative ; `404` sans mandat actif ou si la facture n'a plus rien à payer.
6. Réactiver un mandat suspendu, une fois le portefeuille rechargé : `POST /sni/api/v1/subscriptions/lifecycle/debit-mandates/{mandateCode}/reactivate` (seulement `SUSPENDED`, `400` sinon). Remet le compteur d'échecs à zéro.
7. Révoquer : `POST .../debit-mandates/{mandateCode}/revoke` avec `{ "reason": "..." }` facultatif. Définitif ; un nouveau mandat peut ensuite être donné.

Le membre gère aussi son mandat depuis le portail (`/sni/api/v1/client/subscriptions/{n}/debit-mandate`).

## Mettre en pause (gel) et reprendre

La pause est un gel encadré par la politique.

1. Avant de proposer la pause : `GET /sni/api/v1/subscriptions/lifecycle/{subscriptionNumber}/freezes/allowance` ([route](routes/SubscriptionLifecycleController.md#r-get-sni-api-v1-subscriptions-lifecycle-subscriptionnumber-freezes-allowance)) rend `freezesUsedThisYear`, `freezesAllowedPerYear`, `maxDays`, `feePercent`, `canFreeze`. Masquer le bouton si `canFreeze` est faux et borner le sélecteur de date à `maxDays`.
2. Mettre en pause : `PATCH /sni/api/v1/subscriptions/{subscriptionNumber}/pause` avec `resumeDate` (date de reprise prévue) **ou** `days` (≥ 1), plus `reason`, `changedBy`. Seulement depuis `ACTIVE`.
   - Contrôles : reprise après aujourd'hui (`400`), au plus [freezeMaxDays] jours (`409`), au plus [freezeMaxPerYear] gels sur douze mois glissants (`409`), pas de gel déjà ouvert (`409`).
   - Si [freezeFeePercent] > 0, la part retenue est facturée tout de suite (`feeBillableNumber` du gel).
   - Statut `PAUSED`, `pausedAt`, `pauseUntil` remplis, échéancier en pause.
3. Historique des gels : `GET .../{subscriptionNumber}/freezes` (`startedOn`, `plannedUntil`, `resumedOn`, `daysPlanned`, `daysEffective`, `feeAmount`).
4. Reprendre : `PATCH /sni/api/v1/subscriptions/{subscriptionNumber}/resume` avec `reason`, `changedBy` facultatifs. Seulement depuis `PAUSED`. **Aucune reprise automatique** à `pauseUntil` : prévoir dans l'écran une liste des abonnements `PAUSED` dont `pauseUntil` est passée. La reprise prolonge la période et la prochaine échéance (et le contrat) du nombre de jours restant entre aujourd'hui et `pauseUntil` (voir [Écarts](README.md#écarts-relevés) sur ce calcul).

La politique `freezeNoticeDays` ne s'applique qu'au gel demandé depuis le portail (`POST /sni/api/v1/client/subscriptions/{n}/freeze`).

## Suspendre

1. `PATCH /sni/api/v1/subscriptions/{subscriptionNumber}/suspend` avec `reason`, `changedBy` facultatifs. Seulement depuis `ACTIVE`, `GRACE_PERIOD` (ou `PAST_DUE`) : `409` sinon.
2. `SUSPENDED`, `suspendedAt`, `suspensionReason` remplis, échéancier en pause, courriel.
3. Pas de route « lever la suspension » : un abonnement suspendu par impayé de renouvellement revient `ACTIVE` au paiement de la facture. Une suspension manuelle sans facture en attente n'a pas de sortie prévue autre que `PATCH .../activate` (qui ne contrôle pas le statut de départ) ou l'annulation.

Les droits accordés ne sont pas fermés par la suspension ni par la pause (voir [Écarts](README.md#écarts-relevés)).

## Annuler

Annulation sans préavis ni frais, réservée au back-office. Pour une résiliation avec préavis, engagement et liste de sortie, utiliser le parcours suivant.

- **Tout de suite** : `PATCH /sni/api/v1/subscriptions/{subscriptionNumber}/cancel` avec `{ "reason": "...", "changedBy": "..." }` (corps facultatif). `CANCELLED` ; droits actifs révoqués ; contrat résilié s'il était actif, sinon annulé ; facture de renouvellement non payée annulée ; dépôt de garantie rendu puis imputé sur les impayés du souscripteur ; courriel et notification.
- **En fin de période** : même route avec `{ "cancelAtPeriodEnd": true }`. Seul le drapeau `cancelAtPeriodEnd` change ; l'abonnement reste `ACTIVE`, reçoit les avis de fin, et le worker l'annule le jour de `currentPeriodEnd`.

Il n'existe aucune route pour retirer `cancelAtPeriodEnd`. Ne jamais envoyer `cancelAtPeriodEnd: false` en pensant annuler la demande : cela annule l'abonnement immédiatement.

## Résilier avec préavis

Écran : « Résiliation » depuis la fiche abonnement, puis file des préavis.

1. **Prévoir** : `POST /sni/api/v1/subscriptions/terminations/subscriptions/{subscriptionNumber}/preview` avec le même corps que la demande ([route](routes/SubscriptionTerminationController.md#r-post-sni-api-v1-subscriptions-terminations-subscriptions-subscriptionnumber-preview)) : `reasonCategory` (obligatoire), `reason`, `channel` (`STAFF` par défaut), `requestedEffectiveDate`, `noticePeriodDays` (sinon [defaultNoticeDays]). Afficher :
   - `earliestEffectiveDate` (aujourd'hui + préavis) et `effectiveDate` retenue (la date demandée si elle est plus tardive, sinon la plus proche) ;
   - `earlyTermination`, `commitmentEnd`, `remainingCommitmentMonths`, `feeAmount` : frais de rupture si la date d'effet tombe avant la fin de l'engagement ;
   - `bridgingAmount` : jours au-delà de la période payée jusqu'à la date d'effet, facturés au prorata ;
   - `exitChecklist` : libellés de la liste de sortie.
2. **Demander** : `POST .../terminations/subscriptions/{subscriptionNumber}` avec le même corps. Réponse `TerminationView` `REQUESTED` ; l'abonnement passe `PENDING_TERMINATION` (il ne se renouvellera plus) ; courriel au souscripteur. Possible depuis `ACTIVE`, `GRACE_PERIOD`, `PAUSED`, `SUSPENDED`.
3. **Dispenser des frais** (avant acceptation, ADMIN ou SUPER_ADMIN) : `POST /sni/api/v1/subscriptions/terminations/{terminationCode}/waive-fee` avec `{ "reason": "..." }` obligatoire. `feeDue` passe à 0, `feeWaived` vrai.
4. **Accepter** : `POST .../{terminationCode}/accept`. Les frais restants (`feeDue`) et le montant de raccordement sont facturés (`feeBillableNumber`, `bridgingBillableNumber`) ; sans frais, la ligne « frais de rupture » de la liste se coche seule. Courriel d'acceptation.
5. **Liste de sortie** (dans `exitItems`) : `BADGE_RETURN` (obligatoire), `KEYS_RETURN`, `BALANCE_DUE` (obligatoire), et selon le cas `MAIL_PENDING` et `DOMICILIATION_END` (domiciliation en cours, obligatoires), `DEPOSIT_SETTLEMENT` (dépôt retenu), `TERMINATION_FEE` (frais dus, obligatoire).
   - Cocher : `POST .../{terminationCode}/exit-items/{itemCode}/done` avec `{ "detail": "..." }` facultatif.
   - Dispenser : `POST .../{terminationCode}/exit-items/{itemCode}/waive` avec `{ "reason": "..." }` obligatoire.
   - Se cochent seules à l'achèvement : solde et frais quand le souscripteur n'a plus de facture ouverte, courrier quand aucun courrier n'attend, domiciliation quand le contrat de domiciliation est clos.
6. **Achever** : automatiquement par le worker horaire à la date d'effet pour un préavis `ACCEPTED`, si toutes les lignes obligatoires sont faites ou dispensées ; ou tout de suite par `POST .../{terminationCode}/complete` (`409` si une ligne obligatoire reste ouverte, `400` si le préavis n'est pas accepté). L'abonnement est alors annulé comme par `PATCH .../cancel` (droits, contrat, dépôt), le préavis passe `COMPLETED`, courriel de clôture.
7. **Retirer** : `POST .../{terminationCode}/retract` avec `{ "reason": "..." }`. Possible tant que le préavis est `REQUESTED` ou `ACCEPTED`, sauf si des frais ont déjà été facturés (`409` : émettre d'abord un avoir). L'abonnement redevient `ACTIVE`.

Listes : `GET /sni/api/v1/subscriptions/terminations?status=REQUESTED` (défaut `REQUESTED`, non paginée, triée par date d'effet), `GET .../terminations/blocked` (préavis acceptés dont la date d'effet est passée mais la liste de sortie encore ouverte : le tableau des sorties à débloquer), `GET .../terminations/subscriptions/{subscriptionNumber}` (historique d'un abonnement), `GET .../terminations/{terminationCode}`.

Le membre peut demander sa résiliation depuis le portail (`/sni/api/v1/client/subscriptions/{n}/termination`) ; elle arrive dans la même file avec `channel: PORTAL`.

## Engagement

1. Lire : `GET /sni/api/v1/subscriptions/lifecycle/{subscriptionNumber}/commitment` (`404` sans engagement). `source` dit d'où il vient : `PLAN` (prix du plan), `QUOTE` (devis), `MANUAL`.
2. Chiffrer une sortie anticipée : `GET .../{subscriptionNumber}/commitment/early-termination?on=2026-12-31` (`on` par défaut aujourd'hui) rend `early`, `remainingMonths` (tout mois entamé compte), `fee`, `formula`, `commitmentEnd`. La pénalité se calcule sur la mensualité équivalente (`totalAmount` ramené au mois).
3. Poser ou remplacer : `PUT .../{subscriptionNumber}/commitment` avec `commitmentMonths` (≥ 1, obligatoire), `commitmentStart` (par défaut le début de l'abonnement), `formula`, `percent`, `fixedFee` (par défaut ceux de la politique), `autoRenewCommitment`. Un engagement existant est remplacé.
4. Lever (ADMIN ou SUPER_ADMIN) : `DELETE .../{subscriptionNumber}/commitment` (`204`).
5. Au terme, le worker horaire reconduit l'engagement pour la même durée si `autoRenewCommitment` est vrai et l'abonnement ouvert ; sinon il le supprime.

## Changer de plan en cours de période

Le prorata suit [prorationPolicy] : le reste à courir au prix actuel est rendu, le reste à courir au nouveau prix est dû ; la différence est facturée si elle est positive, créditée au **portefeuille** du souscripteur si elle est négative. Avec `NONE`, rien n'est facturé ni crédité.

1. Prévoir : `GET /sni/api/v1/subscriptions/lifecycle/{subscriptionNumber}/plan-change/preview?targetPlanVersionId=...&effectiveDate=...` ([route](routes/SubscriptionLifecycleController.md#r-get-sni-api-v1-subscriptions-lifecycle-subscriptionnumber-plan-change-preview)). `targetPlanVersionId` est l'id de la version cible (texte reçu dans `PlanVersionResponse.id`). Rend `unusedAtCurrentPrice`, `remainingAtTargetPrice`, `amount`, `credit` (vrai : montant rendu), `targetAmounts`. `409` si la version cible n'a pas de prix pour le cycle de l'abonnement.
2. Demander : `POST /sni/api/v1/subscriptions/{subscriptionNumber}/changes` avec `changeType` (`UPGRADE`, `DOWNGRADE`…), `targetPlanVersionId`, `effectivePolicy` (`IMMEDIATE` : aujourd'hui, `NEXT_BILLING_PERIOD` : prochaine échéance, `CUSTOM_DATE` : `effectiveDate`), `reason`, `requestedBy`. Statut `REQUESTED`, `changeNumber` rendu.
3. Approuver : `PATCH /sni/api/v1/subscriptions/changes/{changeNumber}/approve` avec `{ "approvedBy": "..." }` facultatif.
4. Appliquer :
   - tout de suite : `PATCH .../changes/{changeNumber}/apply` (depuis `REQUESTED` ou `APPROVED`, `409` sinon) : l'abonnement passe sur la version cible, ses montants sont recalculés, le prorata est facturé (`prorationBillableNumber`) ou crédité ;
   - à date : une demande `APPROVED` est appliquée par le worker (15 min) quand `effectiveDate` est atteinte.
5. Suivre : `GET /sni/api/v1/subscriptions/changes?subscriptionNumber=...` (filtres `changeType`, `status`).

Seuls les changements avec `targetPlanVersionId` modifient l'abonnement ; les autres types ne font que changer de statut. Le membre peut demander un changement depuis le portail (`/sni/api/v1/client/subscriptions/{n}/plan-change`).

## Aligner les échéances d'un souscripteur

Pour qu'une entreprise reçoive une seule échéance pour tous ses abonnements.

1. Simuler : `POST /sni/api/v1/subscriptions/lifecycle/align` avec `subscriberType`, `subscriberCode`, `targetPeriodEnd` (facultatif : sinon la plus tardive des fins de période) et `simulateOnly` (vrai **par défaut**) ([route](routes/SubscriptionLifecycleController.md#r-post-sni-api-v1-subscriptions-lifecycle-align)).
2. La réponse liste chaque abonnement ouvert (hors cycles `DAILY` et `ONE_TIME`) : `currentPeriodEnd`, `newPeriodEnd`, `bridgingDays`, `bridgingAmount`, `aligned`, `message`. Une période qui dépasse déjà la date commune n'est pas raccourcie (`aligned: false`). Au moins deux abonnements sont nécessaires (`400`), date commune dans le futur (`400`).
3. Appliquer : même appel avec `simulateOnly: false`. Les périodes et contrats sont prolongés jusqu'à la date commune, l'échéance suivante est le lendemain, et les jours de raccordement sont facturés.

## Lien avec le contrat

- À l'activation, un contrat est créé, généré en PDF et **signé par le système** ; son code apparaît dans `contractCode` (module [contract](../contract/README.md)). Un contrat manquant est rattrapé chaque minute.
- Chaque nouvelle période payée prolonge **le même contrat** par un avenant de renouvellement ; une reprise après pause ou un alignement prolonge sa date de fin sans avenant.
- Le contrat d'un abonnement encore ouvert (ni `CANCELLED` ni `EXPIRED`) n'expire pas : s'il a été marqué expiré, le worker d'intégrité le remet en vigueur et le prolonge jusqu'à la fin de la période courante.
- L'annulation (`PATCH .../cancel` ou préavis achevé) résilie un contrat actif, ou annule un contrat pas encore actif.
