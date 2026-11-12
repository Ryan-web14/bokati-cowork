# subscription : parcours de souscription

> Partie de [README.md](README.md). Routes exactes et champs : [routes.md](routes.md). Règles communes : [README racine](../README.md).

Le souscripteur est désigné par un couple `subscriberType` + `subscriberCode` : `MEMBER` + `memberId`, `CUSTOMER` + `customerId`, `BUSINESS_ENTITY` + `code` d'une fiche `/businesses` (codes `BIZ-...`). **Une entreprise cliente créée par `/companies` (code `CUS-COM-...`) est un client : `CUSTOMER` + son code.** Envoyée en `BUSINESS_ENTITY`, elle est reconnue et enregistrée en `CUSTOMER` (même chose pour un pass). Le code de son profil légal (fiche `business_entity` créée avec elle, par exemple `ELL20261111482`) envoyé en `BUSINESS_ENTITY` est lui aussi rattaché au client `CUS-COM-...` : l'abonnement figure ainsi dans le portefeuille et le relevé de l'entreprise. Ces codes viennent des modules [client](../client/README.md) et [company](../company/README.md).

## Souscrire directement

Écran : formulaire « Nouvel abonnement » (souscripteur, plan, cycle, date de début, renouvellement automatique, codes promo).

1. Charger les plans actifs : `GET /sni/api/v1/subscriptions/plans?status=ACTIVE`. Chaque plan porte `activeVersion` avec ses `prices` : proposer les cycles (`billingCycle`) présents et afficher montant, frais d'installation, dépôt et engagement de chacun.
2. (Facultatif) Afficher le prix remisé : `POST /sni/api/v1/pricing/simulate` avec une ligne `scope: PLAN`, `code` = code du plan, `unitPrice`, et les `couponCodes` saisis ([parcours-promotions.md](parcours-promotions.md#simuler-un-prix-en-back-office)).
3. Créer : `POST /sni/api/v1/subscriptions` ([route](routes/SubscriptionController.md#r-post-sni-api-v1-subscriptions)) avec :
   - `planCode`, `subscriberType`, `subscriberCode`, `startDate` (obligatoires) ;
   - `billingCycle` (sinon le premier prix de la version est pris, sans ordre garanti : toujours l'envoyer) ;
   - `planVersionId` : l'`id` texte d'une version **active** ; omis, la version active du plan est prise ; une version non active est remplacée sans erreur par la version active ;
   - `autoRenew` (vrai par défaut), `autoActivate` (faux par défaut), `couponCodes`, `metadataJson` ;
   - `existingInvoiceNumber`, `existingPaymentNumber` (facultatifs) : la facture, ou le paiement, déjà faits hors de l'abonnement (voir [Facturation déjà faite](#facturation-déjà-faite-hors-de-labonnement)).
4. Réponse `201` `SubscriptionResponse`. Le serveur a, dans la même opération :
   - calculé la période : fin = début + un cycle - 1 jour (`DAILY` et `ONE_TIME` : fin = début), `nextBillingDate` = début + un cycle (`ONE_TIME` : aucune) ;
   - appliqué les promotions et coupons **avant** la taxe ;
   - émis la première facture (lignes abonnement, frais d'installation, dépôt de garantie) dans [billing](../billing/README.md) ;
   - tenté une retenue du dépôt sur le portefeuille du souscripteur (un échec est ignoré) ;
   - posé l'engagement du prix s'il y en a un ;
   - activé l'abonnement si le total est nul ou si `autoActivate` est vrai ; sinon il reste `PENDING_ACTIVATION` et un courriel de création part.
5. L'écran affiche la fiche de l'abonnement avec le statut et, s'il est `PENDING_ACTIVATION`, un lien vers la facture à régler (factures du souscripteur dans billing, ou paiement depuis le portail par `POST /sni/api/v1/client/subscriptions/{n}/pay/...`, voir [portal](../portal/README.md)).

Erreurs à afficher : `409` abonnement déjà ouvert sur cette version pour ce souscripteur ; `400` niveau KYC insuffisant ; `404` souscripteur, plan ou prix introuvable.

## Facturation déjà faite hors de l'abonnement

Cas : le client a été facturé (et parfois a payé) avant que l'abonnement soit créé. L'abonnement ne doit pas émettre une seconde facture pour sa première période.

- **À la création** (`POST /sni/api/v1/subscriptions`) ou **à l'acceptation d'un devis** (`POST .../quotes/{quoteNumber}/accept` avec le corps `{ "existingInvoiceNumber": "...", "existingPaymentNumber": "..." }`) : indiquer la facture, ou seulement le paiement s'il règle une seule facture.
  - Contrôles (`400` ou `409` avec le message à afficher) : la facture existe, est une facture (ni devis ni avoir), est émise et non annulée, est au nom du souscripteur (code client ou code client de l'entreprise) et n'est pas déjà rattachée à un autre abonnement ; un paiement est réussi, au nom du souscripteur, et imputé sur la facture indiquée.
  - Effet : pas de facture initiale (lignes abonnement, frais d'installation, dépôt) ; la facture rattachée est inscrite dans l'abonnement (`metadataJson` : `linkedInvoiceNumber`, `linkedPaymentNumber`, `linkedBy`) et dans son historique.
  - **Validation** : si la facture est soldée, l'abonnement est activé aussitôt ; sinon il reste `PENDING_ACTIVATION` et il est activé automatiquement quand un paiement solde la facture rattachée.
- **Après coup, sur un abonnement existant** : `POST /sni/api/v1/subscriptions/{subscriptionNumber}/activate-with-billing` avec `{ "invoiceNumber": "...", "paymentNumber": "...", "activate": true, "force": false }` (droit d'activation d'un abonnement). Abonnement `PENDING_ACTIVATION` ou `ACTIVE`.
  - La facture initiale que l'abonnement avait émise, si elle n'est ni réglée ni scellée, est annulée (`cancelledInvoices`) ; réglée ou scellée, elle est laissée et un avertissement demande un avoir.
  - `activate` (vrai par défaut) valide l'abonnement si la facture rattachée est soldée ; `force` (administrateur seulement) le valide même si elle ne l'est pas, avec un avertissement.
  - Réponse : `subscriptionNumber`, `status`, `link` (`invoiceNumber`, `paymentNumber`, `invoiceTotal`, `balanceDue`, `currency`, `paid`, `warnings`), `cancelledInvoices`, `warnings` (à afficher en bandeau orange, non bloquant).
- Avertissements possibles (non bloquants) : facture non soldée, facture plus faible que la première période.

Écran : dans « Nouvel abonnement » et dans l'acceptation d'un devis, un champ « Facture déjà émise » (recherche dans les factures du souscripteur) et « Paiement déjà reçu » ; sur la fiche d'un abonnement en attente, un bouton « Rattacher une facture existante » ouvrant ce formulaire, avec la case « Forcer la validation » visible pour un administrateur.

## Activer

L'activation est normalement automatique : quand la facture de souscription est entièrement réglée, le module paiement active l'abonnement. L'activation manuelle sert aux règlements constatés hors système.

1. `PATCH /sni/api/v1/subscriptions/{subscriptionNumber}/activate` avec, facultativement, `{ "reason": "...", "changedBy": "<utilisateur>" }` ([route](routes/SubscriptionController.md#r-patch-sni-api-v1-subscriptions-subscriptionnumber-activate)).
2. Si le niveau KYC du plan n'est pas atteint, l'abonnement passe `PENDING_DOCUMENTS` (pas d'erreur) ; le worker horaire réessaie seul quand les pièces sont validées.
3. Sinon `ACTIVE` : droits ouverts pour la période, courriel et notification d'activation, demande de génération du contrat. `contractCode` se remplit quelques secondes plus tard (relire la fiche).

L'abonnement déjà `ACTIVE` est rendu tel quel. Le serveur ne bloque pas l'activation d'un abonnement annulé ou expiré : n'afficher le bouton que pour `PENDING_ACTIVATION` et `PENDING_DOCUMENTS`.

## Souscrire par devis

Écran : « Devis » (liste, création, fiche). Les routes exigent aussi le rôle ADMIN, SUPER_ADMIN ou STAFF.

1. Préparer : `POST /sni/api/v1/subscriptions/quotes` ([route](routes/SubscriptionQuoteController.md#r-post-sni-api-v1-subscriptions-quotes)) avec `subscriberType`, `subscriberCode`, `planCode` (obligatoires), `planVersionId` (texte), `billingCycle`, `quotedPrice` (prix de la période, par défaut celui du catalogue), `setupFee`, `commitmentMonths`, `trialDays` (par défaut ceux du prix), `startDate`, `validUntil` (par défaut aujourd'hui + `quoteValidityDays`), `notes`, `proposalNumber` (référence libre d'une proposition commerciale). Réponse `QuoteView`, `status: DRAFT`, `negotiated` vrai si le prix diffère du catalogue.
   - Un prix sous le plancher ou un rabais au-delà du maximum : `409`, le devis n'est pas créé, **sauf pour un administrateur** (`ADMIN`, `SUPER_ADMIN`) : le devis est créé, jusqu'à un prix nul, avec un avertissement non bloquant. La dérogation est gardée dans les notes du devis (ligne `[Dérogation administrateur par ...] <motif>`), et suit le devis jusqu'à l'acceptation, même faite par un membre du personnel. Les autres blocages restent.
   - `QuoteView.warnings` (liste de textes, vide sinon) : la dérogation accordée, et pour un prix nul le rappel que les renouvellements resteront à zéro tant que la dérivation est active. À afficher en bandeau orange sur la fiche du devis. Si la facturation a été faite à part, préférer un devis au prix normal accepté avec la facture existante (voir [Facturation déjà faite](#facturation-déjà-faite-hors-de-labonnement)).
2. Avant d'envoyer, afficher l'aperçu : `GET /sni/api/v1/subscriptions/quotes/{quoteNumber}/outlook` rend `discountPercent`, `requiresApproval` (la dérivation créée à l'acceptation attendra un visa) et `blockingReason`.
3. Envoyer : `POST .../{quoteNumber}/send`. Passe `SENT` et envoie un courriel au souscripteur s'il a une adresse (`subscriberEmail`). Refusé si le devis n'est plus `DRAFT` ou `SENT` (`400`) ou s'il est expiré (`409`).
4. Le souscripteur répond :
   - au guichet : `POST .../{quoteNumber}/accept` (corps facultatif `{ "existingInvoiceNumber": "...", "existingPaymentNumber": "..." }`, voir [Facturation déjà faite](#facturation-déjà-faite-hors-de-labonnement)) ou `POST .../{quoteNumber}/reject` avec `{ "reason": "..." }` facultatif ;
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
