# subscription : parcours de souscription

> Partie de [README.md](README.md). Routes exactes et champs : [routes.md](routes.md). Règles communes : [README racine](../README.md).

Le souscripteur est désigné par un couple `subscriberType` + `subscriberCode` : `MEMBER` + `memberId`, `CUSTOMER` + `customerId`, `BUSINESS_ENTITY` + `code` de l'entreprise. Ces codes viennent des modules [client](../client/README.md) et [company](../company/README.md).

## Souscrire directement

Écran : formulaire « Nouvel abonnement » (souscripteur, plan, cycle, date de début, renouvellement automatique, codes promo).

1. Charger les plans actifs : `GET /sni/api/v1/subscriptions/plans?status=ACTIVE`. Chaque plan porte `activeVersion` avec ses `prices` : proposer les cycles (`billingCycle`) présents et afficher montant, frais d'installation, dépôt et engagement de chacun.
2. (Facultatif) Afficher le prix remisé : `POST /sni/api/v1/pricing/simulate` avec une ligne `scope: PLAN`, `code` = code du plan, `unitPrice`, et les `couponCodes` saisis ([parcours-promotions.md](parcours-promotions.md#simuler-un-prix-en-back-office)).
3. Créer : `POST /sni/api/v1/subscriptions` ([route](routes/SubscriptionController.md#r-post-sni-api-v1-subscriptions)) avec :
   - `planCode`, `subscriberType`, `subscriberCode`, `startDate` (obligatoires) ;
   - `billingCycle` (sinon le premier prix de la version est pris, sans ordre garanti : toujours l'envoyer) ;
   - `planVersionId` : l'`id` texte d'une version **active** ; omis, la version active du plan est prise ; une version non active est remplacée sans erreur par la version active ;
   - `autoRenew` (vrai par défaut), `autoActivate` (faux par défaut), `couponCodes`, `metadataJson`.
4. Réponse `201` `SubscriptionResponse`. Le serveur a, dans la même opération :
   - calculé la période : fin = début + un cycle - 1 jour (`DAILY` et `ONE_TIME` : fin = début), `nextBillingDate` = début + un cycle (`ONE_TIME` : aucune) ;
   - appliqué les promotions et coupons **avant** la taxe ;
   - émis la première facture (lignes abonnement, frais d'installation, dépôt de garantie) dans [billing](../billing/README.md) ;
   - tenté une retenue du dépôt sur le portefeuille du souscripteur (un échec est ignoré) ;
   - posé l'engagement du prix s'il y en a un ;
   - activé l'abonnement si le total est nul ou si `autoActivate` est vrai ; sinon il reste `PENDING_ACTIVATION` et un courriel de création part.
5. L'écran affiche la fiche de l'abonnement avec le statut et, s'il est `PENDING_ACTIVATION`, un lien vers la facture à régler (factures du souscripteur dans billing, ou paiement depuis le portail par `POST /sni/api/v1/client/subscriptions/{n}/pay/...`, voir [portal](../portal/README.md)).

Erreurs à afficher : `409` abonnement déjà ouvert sur cette version pour ce souscripteur ; `400` niveau KYC insuffisant ; `404` souscripteur, plan ou prix introuvable.

## Activer

L'activation est normalement automatique : quand la facture de souscription est entièrement réglée, le module paiement active l'abonnement. L'activation manuelle sert aux règlements constatés hors système.

1. `PATCH /sni/api/v1/subscriptions/{subscriptionNumber}/activate` avec, facultativement, `{ "reason": "...", "changedBy": "<utilisateur>" }` ([route](routes/SubscriptionController.md#r-patch-sni-api-v1-subscriptions-subscriptionnumber-activate)).
2. Si le niveau KYC du plan n'est pas atteint, l'abonnement passe `PENDING_DOCUMENTS` (pas d'erreur) ; le worker horaire réessaie seul quand les pièces sont validées.
3. Sinon `ACTIVE` : droits ouverts pour la période, courriel et notification d'activation, demande de génération du contrat. `contractCode` se remplit quelques secondes plus tard (relire la fiche).

L'abonnement déjà `ACTIVE` est rendu tel quel. Le serveur ne bloque pas l'activation d'un abonnement annulé ou expiré : n'afficher le bouton que pour `PENDING_ACTIVATION` et `PENDING_DOCUMENTS`.

## Souscrire par devis

Écran : « Devis » (liste, création, fiche). Les routes exigent aussi le rôle ADMIN, SUPER_ADMIN ou STAFF.

1. Préparer : `POST /sni/api/v1/subscriptions/quotes` ([route](routes/SubscriptionQuoteController.md#r-post-sni-api-v1-subscriptions-quotes)) avec `subscriberType`, `subscriberCode`, `planCode` (obligatoires), `planVersionId` (texte), `billingCycle`, `quotedPrice` (prix de la période, par défaut celui du catalogue), `setupFee`, `commitmentMonths`, `trialDays` (par défaut ceux du prix), `startDate`, `validUntil` (par défaut aujourd'hui + `quoteValidityDays`), `notes`, `proposalNumber` (référence libre d'une proposition commerciale). Réponse `QuoteView`, `status: DRAFT`, `negotiated` vrai si le prix diffère du catalogue.
   - Un prix sous le plancher ou un rabais au-delà du maximum : `409`, le devis n'est pas créé.
2. Avant d'envoyer, afficher l'aperçu : `GET /sni/api/v1/subscriptions/quotes/{quoteNumber}/outlook` rend `discountPercent`, `requiresApproval` (la dérivation créée à l'acceptation attendra un visa) et `blockingReason`.
3. Envoyer : `POST .../{quoteNumber}/send`. Passe `SENT` et envoie un courriel au souscripteur s'il a une adresse (`subscriberEmail`). Refusé si le devis n'est plus `DRAFT` ou `SENT` (`400`) ou s'il est expiré (`409`).
4. Le souscripteur répond :
   - au guichet : `POST .../{quoteNumber}/accept` ou `POST .../{quoteNumber}/reject` avec `{ "reason": "..." }` facultatif ;
   - depuis le portail : `POST /sni/api/v1/client/quotes/{quoteNumber}/accept` ou `/reject` (module [portal](../portal/README.md)).
5. À l'acceptation, le serveur crée l'abonnement (début = `startDate`, ou aujourd'hui si elle est passée ; renouvellement automatique ; pas d'activation automatique), puis, si le prix est négocié, une [dérivation](parcours-derivations.md) motif `NEGOTIATION`, puis l'engagement du devis. Le devis passe `CONVERTED` avec `convertedSubscriptionNumber` et `derivationCode`. L'écran propose d'ouvrir l'abonnement créé ; si `requiresApproval` était vrai, signaler que le tarif négocié attend un visa.
6. Un devis dont `validUntil` est passée devient `EXPIRED` au passage horaire du worker ; l'accepter après cette date le passe `EXPIRED` et renvoie `409`.

Listes : `GET /sni/api/v1/subscriptions/quotes?status=...` (paginée, toujours triée par date de création décroissante), `GET .../quotes/subscribers/{subscriberType}/{subscriberCode}` (tous les devis d'un souscripteur).

## Consulter un abonnement

Écran : fiche abonnement, avec des onglets chargés à la demande.

| Onglet | Route | À afficher |
| --- | --- | --- |
| En-tête | `GET /sni/api/v1/subscriptions/{subscriptionNumber}` | statut, souscripteur (`subscriberName`), plan et numéro de version, période courante, `nextBillingDate`, `autoRenew`, `cancelAtPeriodEnd`, montants, `contractCode`, et le bandeau de renouvellement en attente (`pendingRenewalInvoice`, `pendingRenewalStart`, `pendingRenewalEnd`). |
| Historique des statuts | `GET .../{subscriptionNumber}/history` | du plus ancien au plus récent : `fromStatus`, `toStatus`, `reason`, `changedBy`, `changedAt`. |
| Droits | `GET .../{subscriptionNumber}/entitlements` | tous les droits accordés à l'abonnement, toutes périodes et tous statuts confondus ; filtrer sur `status` et `validUntil` pour n'afficher que le solde courant. |
| Échéancier | `GET .../{subscriptionNumber}/billing-schedule` | cycle, prochaine échéance, statut de l'échéancier (`ACTIVE`, `PAUSED`, `CANCELLED`). `retryCount` et `lastAttemptAt` ne sont pas alimentés par le renouvellement. |
| Chronologie | `GET /sni/api/v1/subscription-timeline?subscriptionNumber=...` | voir [parcours-suivi.md](parcours-suivi.md#chronologie). |
| Engagement, gels, mandat, préavis, dérivations, options, sièges | voir les parcours correspondants | |

Rechercher : `GET /sni/api/v1/subscriptions` avec `subscriberType`, `subscriberCode`, `planCode`, `status`, `nextBillingBefore` (date) ; tri par défaut `createdAt,DESC`.

L'abonnement en cours d'un souscripteur : `GET /sni/api/v1/subscriptions/current?subscriberType=...&subscriberCode=...` ; `404` s'il n'a pas d'abonnement `ACTIVE` dont la période couvre aujourd'hui (un abonnement en tolérance ou suspendu n'est donc pas « courant »).

## Sièges d'un abonnement

Rattacher des membres à un abonnement (abonnement d'entreprise partagé). Le back-office ne contrôle ici aucun nombre maximal de sièges ; le partage avec plafond se gère côté portail entreprise (`/sni/api/v1/client/companies/{customerId}/subscriptions/...`, module [portal](../portal/README.md)).

1. Lister : `GET /sni/api/v1/subscriptions/seats?subscriptionNumber=...` (filtres `memberCode`, `status`).
2. Ajouter : `POST /sni/api/v1/subscriptions/{subscriptionNumber}/seats` avec `memberCode` (`memberId` d'un membre non supprimé) et `role` (`OWNER`, `ADMIN`, `MEMBER`, `GUEST`). Le siège est `ACTIVE` d'emblée. `409` si le membre a déjà un siège actif sur cet abonnement, `404` si le membre n'existe pas.
3. Retirer : `DELETE /sni/api/v1/subscriptions/{subscriptionNumber}/seats/{memberCode}` : le siège passe `REMOVED` et la réponse le renvoie.

## Options (add-ons)

Ajouter à un abonnement une autre version de plan (par exemple un casier), facturée à part.

1. Lister : `GET /sni/api/v1/subscriptions/addons?subscriptionNumber=...` (filtres `planCode`, `status`).
2. Ajouter : `POST /sni/api/v1/subscriptions/{subscriptionNumber}/addons` avec `planVersionId` (**id numérique** de la version du plan d'option, obligatoire), `quantity` (≥ 1), `startsAt` (aujourd'hui par défaut), `endsAt` (pas avant le début, `400`), `metadataJson`.
   - Le prix unitaire est le **premier prix** de cette version, quel que soit son cycle.
   - Une facture de `quantity × prix` est émise aussitôt, et un contrat est généré pour l'option.
   - Le statut de l'abonnement n'est pas vérifié.
3. Annuler : `PATCH /sni/api/v1/subscriptions/addons/{addonId}/cancel` avec l'`id` numérique de l'option. `CANCELLED`, `endsAt` = aujourd'hui, aucun avoir automatique.
4. Une option dont `endsAt` est passée devient `EXPIRED` (worker toutes les 15 min). Les options ne se renouvellent pas.
