# company : fonctionnement

> Rédigé d'après le code. Routes exactes, droits et champs JSON : [routes.md](routes.md) et `routes/`. Règles communes : [README racine](../README.md).

## En bref

Le « centre de gestion des entreprises » : la fiche d'une organisation cliente (société, établissement, association, ONG, institution publique…), les membres qui lui sont rattachés (gérant ou employé), la façon dont leurs factures se relient à elle, ses états de compte, le partage de ses abonnements et de ses passes entre collaborateurs, son portefeuille avec des autorisations de dépense, et la liste de ses contrats. Les routes de ce module sont celles du **back-office** (`/sni/api/v1/companies/**`, `/customers/{id}/members`, `/businesses/**`), plus deux routes du portail du gérant pour les contrats. Le reste du portail du gérant (membres, abonnements partagés, passes, portefeuille, validations) est dans le module [portal](../portal/README.md) : il appelle les mêmes services, donc les mêmes règles que celles décrites ici. Modules liés : `client` (une organisation est un client de type `COMPANY`, un collaborateur est un membre), `billing` (factures, encours, plafond), `subscription` (abonnements, passes, droits), `booking` (réservations sur une place ou un pass), `payment` (portefeuilles), `contract` et `domiciliation`.

Sommaire : [Notions](#notions) · [Statuts](#statuts-et-cycle-de-vie) · [Parcours](#parcours) · [Règles et erreurs](#règles-et-erreurs-à-refléter-dans-lécran) · [À prévoir](#à-prévoir-côté-frontend) · [Effets automatiques](#effets-automatiques) · [Écarts](#écarts-relevés)

## Notions

| Objet | Identifiant exposé | Ce que c'est |
| --- | --- | --- |
| Organisation | `customerId` (texte, forme `CUS-COM-AAAAMM-00000012`) | Un client (`Customer`) de type `COMPANY`. Porte le compte commercial : courriels, téléphone, adresses, délai de paiement, plafond d'encours, statut et blocage. |
| Profil légal | `businessCode` (texte, 3 lettres du nom + date + 3 chiffres, par ex. `ACM20261108123`) | Une ligne `business_entity` reliée à l'organisation : type d'organisation, forme juridique, activité, identifiants, représentant légal, régime de TVA. Créé avec la fiche par `POST /companies`. Le même objet sert pour l'**entité exploitante** de l'espace (drapeau `operator`), gérée par `/businesses`. |
| Identifiant légal | `type` + `value` | NIU, RCCM, numéro d'ordre, récépissé, agrément, acte de création, code administratif, ou `AUTRE` avec un libellé. Imprimés sur les factures quand ils sont saisis. |
| Rattachement | `id` (entier court, colonne `short_id`) | Lien membre ↔ organisation, avec un rôle `OWNER` (gérant) ou `EMPLOYEE`, un mode de rattachement des factures, et deux drapeaux `canSign` (signataire désigné des contrats) et `canCollectMail` (peut retirer le courrier de domiciliation). |
| Mode de rattachement des factures | `AUTOMATIC` ou `ON_CHOICE` | Défini par défaut sur l'organisation (`ON_CHOICE` à la création), remplaçable lien par lien. `AUTOMATIC` : toute facture du membre est reliée d'office à l'organisation. `ON_CHOICE` : on choisit à chaque facture. |
| État de compte | `code` de l'organisation ou du membre | Factures, avoirs, proformas d'une période, plus tout ce qui reste dû à la date de l'état. |
| Abonnement partagé | `subscriptionNumber` | Un abonnement souscrit par l'organisation (`subscriberType = CUSTOMER`). Ses droits (`entitlementCode`) se répartissent entre des **places**. |
| Place | `id` (entier court) | Un collaborateur rattaché qui peut consommer les droits de l'abonnement, avec une part et un plafond par droit. |
| Règle de partage | `entitlementCode` | Par droit : `POOL` (pot commun), `QUOTA` (part fixe par place), `QUOTA_AND_POOL` (sa part, puis le pot). Sans règle enregistrée, un droit est en `POOL`. |
| Pass d'organisation | `passNumber` | Un pass détenu par l'organisation. Ses **accès** (`id` entier court) l'ouvrent à des collaborateurs. |
| Portefeuille | `walletNumber` | Un portefeuille `wallet_account` au nom de l'organisation, un par devise (`XAF` par défaut), ouvert à la première consultation. |
| Autorisation de dépense | `id` (entier court) | Permet à un collaborateur de régler des factures de l'organisation avec son portefeuille, pour certaines opérations, sous plafonds. Une seule autorisation ouverte par collaborateur et par portefeuille. |
| Dépense | `id` (entier court) | Un règlement fait sur le portefeuille par un collaborateur ou le gérant, avec la facture payée. |

Tous les `id` de ce module (rattachement, place, accès, autorisation, dépense) sont des **numéros courts** sûrs en JavaScript, pas les identifiants 64 bits : voir [identifiants](../README.md#identifiants--attention-aux-grands-nombres).

Types d'organisation (`organizationType`) et identifiants proposés à la saisie (`suggestedIdentifiers` dans la réponse), tous facultatifs (`src/main/java/com/sni/bokaticowork/features/company/enums/OrganizationType.java`) :

| Type | Libellé | Identifiants proposés |
| --- | --- | --- |
| `SOCIETE` | Société | NIU, RCCM |
| `ETABLISSEMENT` | Établissement | NIU, RCCM |
| `ENTREPRENEUR_INDIVIDUEL` | Entrepreneur individuel | NIU, RCCM |
| `PROFESSION_LIBERALE` | Profession libérale | NIU, ORDRE_PROFESSIONNEL |
| `ASSOCIATION` | Association | NIU, RECEPISSE |
| `ONG` | ONG | NIU, AGREMENT |
| `INSTITUTION_PUBLIQUE` | Institution publique | NIU, ACTE_CREATION, CODE_ADMINISTRATIF |

## Statuts et cycle de vie

### Organisation (statut du client)

Le statut est celui du client (`CustomerStatus`). Ce module n'utilise que `ACTIVE` et `SUSPENDED` (« bloquée »).

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `ACTIVE` | Utilisable | création (`POST /sni/api/v1/companies`), déblocage | `SUSPENDED` (blocage), ou les autres statuts par `PATCH /sni/api/v1/customers/{customerId}/status` (module `client`) |
| `SUSPENDED` | Bloquée : aucune facture ne peut lui être émise, ni à un membre pour son compte | `POST /sni/api/v1/companies/{customerId}/block` (motif obligatoire) | `ACTIVE` par `POST .../unblock` |

`blockedReason`, `blockedBy`, `blockedAt` sont remplis par le blocage et vidés par le déblocage. Un passage en `SUSPENDED` par la route générique du module `client` bloque aussi la facturation, mais sans motif.

### Profil légal (`/businesses`)

`status` vaut `ACTIVE`, `INACTIVE`, `DELETED` ou `BLOCKED`, changé librement par `PATCH /sni/api/v1/businesses/{code}/status` sans table de transitions. La suppression (`DELETE`) est logique : le profil disparaît de toutes les lectures. Aucune règle métier ne lit ce statut dans le module (non vérifié ailleurs).

### Rattachement

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `ACTIVE` | En cours | `POST /sni/api/v1/customers/{customerId}/members` | `ENDED` |
| `ENDED` | Terminé, gardé en historique (date et motif) | `POST .../members/{linkId}/end` | aucun. Pour rattacher à nouveau, créer un nouveau lien. |

### Place sur un abonnement

`ACTIVE` à l'attribution, `REMOVED` à la libération (manuelle ou fin du rattachement). Une place retirée ne revient pas : on en attribue une nouvelle.

### Accès à un pass

Ouvert (`open = true`) si non révoqué et dans sa période et sous son nombre de passages ; révoqué par la route `revoke` ou la fin du rattachement. Un accès révoqué est **rouvert** (même `id`, compteur de passages remis à zéro) si l'on rajoute la même personne.

### Autorisation de dépense

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `ACTIVE` | Utilisable dans sa période (`inForce` dit si elle l'est maintenant) | création ; `PATCH` avec `status: ACTIVE` (le collaborateur doit être encore rattaché) | `SUSPENDED`, `REVOKED` |
| `SUSPENDED` | Suspendue | `PATCH` avec `status: SUSPENDED` ; automatiquement à la fin du rattachement | `ACTIVE`, `REVOKED` |
| `REVOKED` | Définitive | `PATCH` avec `status: REVOKED` | aucun (toute modification est refusée : en créer une nouvelle) |

Passer une autorisation hors `ACTIVE` annule les dépenses de ce collaborateur qui attendaient le gérant et rend leurs fonds.

### Dépense sur le portefeuille

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `AWAITING_OTP` | Code à usage unique envoyé par courriel (valable 10 minutes), rien n'est retenu | règlement au-delà du seuil OTP de la politique du portefeuille | `EXECUTED` ou `PENDING_APPROVAL` après saisie du code ; `EXPIRED` par le worker |
| `PENDING_APPROVAL` | Fonds retenus, attend le gérant | montant au-dessus de `approvalAbove` de l'autorisation | `EXECUTED` (gérant accepte), `REJECTED` (gérant refuse), `CANCELLED` (retirée par le collaborateur, autorisation suspendue ou révoquée, fin du rattachement), `EXPIRED` (délai passé) |
| `HELD` | Fonds retenus pour une réservation prépayée | réservation payée par le portefeuille de l'organisation (module `booking`) | `EXECUTED` au règlement de la facture, `CANCELLED` si la réservation est annulée |
| `EXECUTED` | Facture réglée, `transactionNumber` rempli | règlement direct, validation, ou règlement de la retenue | aucun |
| `REJECTED`, `EXPIRED`, `CANCELLED` | Terminées, fonds rendus | voir ci-dessus | aucun |

Ces transitions (sauf l'expiration) se font depuis le portail ([portal](../portal/README.md)) ; le back-office ne fait que lire les dépenses.

## Parcours

### 1. Lister et rechercher les organisations

1. `GET /sni/api/v1/companies?q=...&status=...&page=0&size=20` ([détail](routes/CompanyAccountController.md#r-get-sni-api-v1-companies)). `q` cherche, sans tenir compte de la casse, dans le nom, le courriel, le courriel de facturation, le `customerId` et la valeur de n'importe quel identifiant légal. `status` est un `CustomerStatus` (`ACTIVE`, `SUSPENDED`…). Tri imposé par nom.
2. Chaque ligne donne `customerId`, `name`, `organizationType`, `niu`, `rccm`, `status` et `outstanding` (encours dû, calculé à chaque appel). Afficher un badge « Bloquée » si `status = SUSPENDED`.

### 2. Créer une organisation

1. Charger les listes : types d'organisation (table ci-dessus), formes juridiques pour une société (`SARL`, `SARLU`, `SA`, `SAU`, `SAS`, `SASU`, `SNC`, `SCS`, `SP`, `SCOOP`, `SCI`, `ETABLISSEMENT`), types d'identifiant, devises (module `socle`), pays pour l'adresse.
2. `POST /sni/api/v1/companies` ([détail](routes/CompanyAccountController.md#r-post-sni-api-v1-companies)) avec au minimum `organizationType`, `name`, `phone`. Facultatifs utiles : `legalForm`, `identifiers: [{type, value, label?}]`, `vatRegime` (`SUBJECT` par défaut ; `EXEMPT` exige `vatExemptionNote`), `email`, `billingEmail`, `address`, `billingAddress`, `paymentTermsDays`, `creditLimit`, `currencyCode` (`XAF` par défaut). `billingEmail` accepte **une ou plusieurs adresses séparées par des virgules** (`"compta@acme.cg, dg@acme.cg"`, 500 caractères au plus) : les factures, relances et relevés partent à toutes ; le serveur renvoie la liste en minuscules, séparée par `, `.
3. Réponse `201` : la fiche complète (`CompanyResponse`), avec `customerId` et `businessCode`. Le serveur crée le client, initialise son dossier KYC et envoie un courriel de bienvenue au client (module `client`).
4. Ensuite : ouvrir la fiche, proposer de rattacher des membres.

Sans `legalForm`, la forme enregistrée est le nom du type (`ASSOCIATION`…). Pour un type autre que `SOCIETE`, une `legalForm` libre est acceptée (60 caractères gardés).

### 3. Consulter et modifier la fiche, bloc par bloc

1. `GET /sni/api/v1/companies/{customerId}` ([détail](routes/CompanyAccountController.md#r-get-sni-api-v1-companies-customerid)) : tout en un appel, dont `identifiers` (chacun avec `format = STANDARD` ou `LEGACY`, ce dernier pour un RCCM à l'ancienne forme, à signaler), `suggestedIdentifiers`, `outstanding`, `membersCount` (rattachements actifs), `activeSubscriptions`.
2. Chaque bloc a sa route ; un champ absent ou `null` reste inchangé. Toutes renvoient la fiche à jour.
   - Légal : `PATCH .../legal` (type, nom, forme, activité, représentant, TVA, identifiants). **`identifiers` remplace la liste entière** quand il est fourni : renvoyer tous les identifiants à garder. Changer le nom change aussi le nom du client.
   - Facturation : `PATCH .../billing` (`billingEmail`, une ou plusieurs adresses séparées par des virgules ; `billingAddress`, `paymentTermsDays`, `creditLimit`). Pour retirer le plafond : `removeCreditLimit: true` (un `creditLimit` absent ne retire rien).
   - Contact : `PATCH .../contact` (`email`, `phone`, `website`, `address`). Le courriel doit être libre chez les autres clients.
3. Une fiche créée avant le centre de gestion n'a pas de profil légal (`businessCode = null`) : le premier `PATCH .../legal` en crée un (type `SOCIETE` par défaut). On peut aussi relier un profil existant : `POST .../legal-profile` avec `{ "businessCode": "..." }` ([détail](routes/CompanyAccountController.md#r-post-sni-api-v1-companies-customerid-legal-profile)).

### 4. Bloquer et débloquer

1. `POST /sni/api/v1/companies/{customerId}/block` avec `{ "reason": "..." }` (obligatoire, 500 caractères au plus).
2. `POST .../unblock` sans corps. Refusé si l'organisation n'est pas bloquée.

### 5. Rattacher des membres à l'organisation

1. Rechercher un membre (module `client`, `GET /sni/api/v1/members/search/basic`) pour obtenir son `memberId`.
2. `POST /sni/api/v1/customers/{customerId}/members` ([détail](routes/CompanyMembershipController.md#r-post-sni-api-v1-customers-customerid-members)) avec `memberId`, `role` (`OWNER` ou `EMPLOYEE`), et facultativement `startedAt` (pas dans le futur), `invoiceLinkMode` (vide = celui de l'organisation), `canSign`, `canCollectMail`.
3. Réponse `201` : le lien, avec `id` (à garder pour les actions suivantes), `invoiceLinkMode` (propre au lien, ou `null`) et `effectiveInvoiceLinkMode` (celui qui s'applique).
4. Lister : `GET /sni/api/v1/customers/{customerId}/members?status=ACTIVE|ENDED|ALL` (actifs par défaut). Côté membre : `GET /sni/api/v1/members/{memberId}/companies` (tous ses liens, actifs et terminés).
5. Modifier : `PATCH .../members/{linkId}` avec `role`, `invoiceLinkMode`, `useCompanyDefaultMode: true` pour revenir au mode de l'organisation, `canSign`, `canCollectMail`.
6. Mode par défaut de l'organisation : `PATCH /sni/api/v1/companies/{customerId}/invoice-link-mode` avec `{ "mode": "AUTOMATIC" }`. La réponse est la valeur seule, en chaîne JSON (`"AUTOMATIC"`).

Un membre peut être rattaché à plusieurs organisations, une fois chacune, mais **une seule** en mode automatique effectif (voir les règles).

### 6. Mettre fin à un rattachement

1. `POST /sni/api/v1/customers/{customerId}/members/{linkId}/end` avec `{ "reason": "..." }` (obligatoire).
2. Le serveur, dans la même opération : libère les places du membre sur les abonnements de l'organisation, révoque ses accès aux passes de l'organisation, suspend ses autorisations de dépense et annule ses dépenses en attente (fonds rendus).
3. Si l'organisation était aussi le **payeur par défaut** du membre (`member.customerId`), il repasse sur une fiche client personnelle (existante par son courriel, ou créée) : la réponse porte `payerChanged: true`.
4. Afficher `warning` s'il est rempli : réservations à venir prises sur les places ou passes de l'organisation (elles ne sont **pas** annulées, à traiter à la main), ou échec du changement de payeur (à corriger sur la fiche du membre).

### 7. Factures émises pour le compte de l'organisation

Le rattachement d'une facture se décide à l'émission, dans le module `billing` : le champ `companyCustomerCode` de la demande de facture vaut le `customerId` d'une organisation du membre, ou `NONE` pour facturer le membre seul ; vide, c'est l'organisation dont le lien est en `AUTOMATIC`, sinon personne (`src/main/java/com/sni/bokaticowork/features/company/membership/service/MemberBillingCompanyResolver.java`). Les réservations ont le même choix (`billingCompanyCode`, module `booking`). La facture garde son destinataire (le membre) et rejoint le compte de l'organisation : encours, plafond, état de compte.

### 8. Relier après coup d'anciennes factures de membres

Toujours en deux temps.

1. Prévisualiser : `POST /sni/api/v1/billing/documents/company-links/preview` ([détail](routes/CompanyInvoiceLinkController.md#r-post-sni-api-v1-billing-documents-company-links-preview)) avec `companyCustomerCode` et soit `documentNumbers: [...]`, soit `memberId` + `from` + `to` (dates `AAAA-MM-JJ`). Rien n'est écrit.
2. La réponse liste chaque facture avec `eligible` et, sinon, `reason` (pas au nom d'un membre, pas une facture, brouillon, membre jamais rattaché à cette organisation, déjà reliée), plus `eligible` (nombre) et `eligibleBalance` (reste dû cumulé). Afficher le tableau et les raisons.
3. Confirmer : `POST /sni/api/v1/billing/documents/company-links` avec le même corps. Seules les admissibles sont reliées ; `linked` donne le nombre.
4. Retirer un lien posé après coup : `DELETE /sni/api/v1/billing/documents/{documentNumber}/company-link`. Refusé pour un lien posé à l'émission (il est imprimé : corriger par un avoir).

Le document émis ne change pas (ni nom, ni PDF scellé).

### 9. État de compte

1. `GET /sni/api/v1/companies/{customerId}/statement?from=AAAA-MM-JJ&to=AAAA-MM-JJ` ([détail](routes/AccountStatementController.md#r-get-sni-api-v1-companies-customerid-statement)). Sans dates : les douze derniers mois jusqu'à aujourd'hui. Même chose pour un membre : `GET /sni/api/v1/members/{memberId}/statement` (droit `CLIENT:READ`).
2. La réponse donne `totals` par devise (`invoiced` et `paid` sur la période ; `balanceDue`, `overdue`, `creditAvailable`, `netBalanceDue`, l'ancienneté `aging` sur tout ce qui reste dû à la date de l'état), `accounts` (répartition par compte : l'organisation et chacun de ses membres, ou pour un membre son compte personnel et chaque organisation), et `lines`. Une ligne `inPeriod: false` est une facture plus ancienne encore due : l'afficher, marquée « hors période ». `impact` est l'effet signé sur le solde (avoir disponible en négatif).
3. Exports : même chemin suivi de `.pdf` ou `.csv`, mêmes paramètres ; fichier nommé `etat-de-compte-<code>-<to>.<ext>`.

### 10. Partager un abonnement entre collaborateurs

1. `GET /sni/api/v1/companies/{customerId}/subscriptions` : les abonnements ouverts de l'organisation avec `maxSeats` (places réglées), `seatCeiling` (plafond vendu) et `activeSeats`.
2. `GET .../subscriptions/{subscriptionNumber}/sharing` ([détail](routes/CompanySharingController.md#r-get-sni-api-v1-companies-customerid-subscriptions-subscriptionnumber-sharing)) : la vue complète. `entitlements` : pour chaque droit, sa règle (`mode`, `defaultQuota`, `seatCap`), `granted` / `remaining` pour l'organisation, `distributedQuotas`, `poolSize` / `poolUsed` en `QUOTA_AND_POOL`. `seats` : chaque place avec, par droit, `quota`, `quotaUsed`, `quotaRemaining`, `cap`, `used`. Toutes les écritures ci-dessous renvoient cette même vue.
3. Plafond de places (administration seulement) : `PATCH .../max-seats` avec `{ "maxSeats": 5 }` ; fixe à la fois le plafond vendu et le nombre de places. `null` : sans limite.
4. Règle d'un droit : `PUT .../sharing/{entitlementCode}` avec `mode`, `defaultQuota` (ignoré en `POOL`), `seatCap`.
5. Attribuer une place : `POST .../seats` avec `memberId` et facultativement `allowances: [{entitlementCode, quota, cap}]` (vides = ceux de la règle). Le membre devient bénéficiaire (sans signature) du contrat de l'abonnement s'il existe.
6. Modifier les parts d'une place : `PATCH .../seats/{seatId}` avec `allowances` (remplace droit par droit, les autres droits ne bougent pas).
7. Libérer : `POST .../seats/{seatId}/release` avec un corps facultatif `{ "reason": "..." }`. Réponse `ReleaseResult` : `futureBookings` et `warning` à afficher (réservations à venir non annulées). Ce qui restait de la part est perdu ; ce qui n'a pas été pris dans le pot y reste.

Le collaborateur consomme ensuite par ses réservations (module `booking`) : la part, le plafond et le pot sont contrôlés à la réservation. Parts et pot repartent à chaque période de l'abonnement.

### 11. Ouvrir un pass d'organisation à des collaborateurs

1. `GET /sni/api/v1/companies/{customerId}/passes` : les passes non clos (hors `CONSUMED`, `EXPIRED`, `CANCELLED`), avec `beneficiaries` (accès ouverts).
2. `GET .../passes/{passNumber}/beneficiaries` : droits du pass (`granted`, `remaining`) et chaque accès avec sa consommation par droit. Accès ouverts en premier.
3. Ajouter : `POST .../passes/{passNumber}/beneficiaries` avec `memberId`, et facultativement `maxUses` (vide = ceux du pass), `validFrom` (vide = tout de suite), `validUntil` (vide = fin du pass). Renvoie la vue du pass.
4. Révoquer : `POST .../beneficiaries/{accessId}/revoke` avec un corps facultatif `{ "reason": "..." }` (255 caractères). Réponse avec `futureBookings` et `warning`.

### 12. Portefeuille de l'organisation

1. `GET /sni/api/v1/companies/{customerId}/wallet?currency=XAF` ([détail](routes/CompanyWalletController.md#r-get-sni-api-v1-companies-customerid-wallet)) : le portefeuille (ouvert à la volée s'il n'existe pas), les 100 derniers mouvements (`entries`, avec le collaborateur et la facture pour une dépense) et les dépenses en attente du gérant (`pendingApprovals`). Cette lecture **crée** le portefeuille au premier appel.
2. `GET .../wallet/spends?currency=&status=` : les 200 dernières dépenses, filtrables par statut. Liste vide si le portefeuille n'existe pas encore.
3. Recharge manuelle : `POST .../wallet/top-ups` avec `amount`, `method` (`CASH` ou `BANK_TRANSFER`), `reason` (5 à 500 caractères), `reference` et `currency` facultatifs. Réponse `AdminTopUpResult` : `status = EXECUTED` (créditée) ou `PENDING_APPROVAL` (au-delà du seuil de double validation `bokati.wallet.admin.four-eyes-threshold`, 100 000 par défaut, une autre personne doit valider dans le centre de contrôle des portefeuilles du module `payment`, `POST /sni/api/v1/wallets/control/actions/{actionNumber}/approve`). Afficher `message`.

### 13. Autorisations de dépense

1. `GET /sni/api/v1/companies/{customerId}/spending-authorizations` : toutes, avec `inForce`, `pinSet` (le collaborateur a posé son code), `engagedToday`, `engagedThisMonth`.
2. Créer : `POST .../spending-authorizations` avec `memberId` (rattaché et actif), `operations` (au moins une parmi `BOOKING`, `SUBSCRIPTION`, `PASS`, `INVOICE_PAYMENT`, `MARKETPLACE`), plafonds facultatifs `maxPerTransaction`, `maxPerDay`, `maxPerMonth`, seuil `approvalAbove` (au-dessus, le gérant valide ; vide, jamais), période `validFrom` / `validUntil`, `currency`.
3. Modifier : `PATCH .../spending-authorizations/{authorizationId}`. **Remplace tout** : `operations` obligatoire, et un plafond ou une date omis est effacé. Renvoyer donc toutes les valeurs à garder. `status` (`ACTIVE`, `SUSPENDED`, `REVOKED`) et `reason` facultatifs.
4. Réinitialiser le code de dépense du collaborateur : `POST .../spending-authorizations/{authorizationId}/pin/reset` (sans corps). Le collaborateur en pose un nouveau depuis son portail sans l'ancien.

Les règlements eux-mêmes (par le collaborateur ou le gérant), la saisie du code à usage unique, l'acceptation ou le refus par le gérant, la recharge par mobile money se font dans le portail : voir [portal](../portal/README.md).

### 14. Contrats de l'organisation

1. `GET /sni/api/v1/companies/{customerId}/contracts` ([détail](routes/CompanyContractsController.md#r-get-sni-api-v1-companies-customerid-contracts)) : `signatories` (représentant légal d'abord, puis les membres `canSign`), `contracts` (avec parties, avenants, versions) et `domiciliations`.
2. PDF : `GET .../contracts/{contractCode}/pdf` (le document signé, sinon le brouillon ; `404` « Document pas encore disponible » s'il n'y en a aucun). `hasDocument` dit à l'avance s'il existe.
3. Portail du gérant : mêmes réponses sur `GET /sni/api/v1/client/companies/{customerId}/contracts` et `.../contracts/{contractCode}/pdf`. Réservé à un membre rattaché comme `OWNER` actif ; sinon `404` « Organisation introuvable ».

### 15. Entité exploitante et profils légaux (paramètres)

Écran de paramétrage, droit `SYSTEM:SETTINGS`.

1. `GET /sni/api/v1/businesses/paged` (ou `GET /businesses` pour tout, `GET /businesses/search/basic?query=` pour une recherche rapide, `POST /businesses/search` avec des critères). Attention : ces listes contiennent **aussi** les profils légaux des organisations clientes.
2. Créer l'entité exploitante : `POST /sni/api/v1/businesses` avec `name`, `legalForm` (valeur de la liste `LegalForm`), `rccmNumber` (obligatoire ici), `phone`, `email`, `baseCurrencyCode`, et facultativement `niuNumber`, `taxId`, `activity`, `address`.
3. La désigner : `PATCH /sni/api/v1/businesses/{code}/operator` (sans corps, `204`). Le drapeau est retiré à l'ancienne. **Indispensable avant toute génération de contrat.**

## Règles et erreurs à refléter dans l'écran

Sauf mention, les refus sont des `400` (`BAD_REQUEST`) dont `message` est en français et peut s'afficher tel quel.

Fiche d'organisation :
- `name` et `phone` obligatoires à la création ; courriels au format courriel.
- Identifiants : tous les refus sont réunis dans un seul `message`. Un même type saisi deux fois (sauf `AUTRE`) est refusé ; `AUTRE` exige `label`.
  - NIU : une lettre (P, E ou M) suivie de 15 chiffres ; espaces retirés, mis en majuscules.
  - RCCM : forme OHADA `CG-BZV-01-2024-B12-00001`, ou ancienne forme `CG/BZV/16 B 6261` (acceptée, renvoyée en `format: LEGACY`).
  - Autres : lettres, chiffres et ponctuation simple, 150 caractères au plus.
  - Un NIU ou un RCCM déjà porté par un autre profil est refusé (« Le NIU ... est déjà celui de ... »).
- `vatRegime = EXEMPT` sans `vatExemptionNote` : refus (« Une organisation exonérée de TVA doit porter sa mention d'exonération »).
- Forme juridique inconnue pour une `SOCIETE` : refus avec la liste des formes admises.
- Devise inconnue : « Devise inconnue : ... ».
- À la création, un courriel ou un téléphone déjà utilisé par un autre client : `409` `RESOURCE_ALREADY_EXISTS` (« A customer with this email already exists », en anglais). Au changement de contact : `400` « Un autre client utilise déjà le courriel ... ».
- `customerId` d'un client personne : `400` « ... est une personne, pas une organisation ». Inconnu ou supprimé : `404` « Organisation introuvable ».
- Relier un profil légal : `404` s'il n'existe pas ; refus s'il est celui de l'exploitant ou s'il est déjà relié à un autre client.

Facturation (module `billing`, à anticiper sur les écrans d'émission) :
- Organisation bloquée : « L'organisation ... est bloquée : .... Aucune facture ne peut lui être émise tant qu'elle n'est pas débloquée. »
- Plafond d'encours : une facture qui ferait dépasser `creditLimit` (encours compris des factures de membres reliées) est refusée, sauf pour un `ROLE_SUPER_ADMIN` (« Plafond d'encours dépassé pour ... »). Afficher `outstanding` et `creditLimit` sur la fiche.

Rattachements :
- Déjà rattaché (lien actif) à la même organisation : refus.
- `startedAt` plus d'une minute dans le futur : « Un rattachement ne commence pas dans le futur. »
- Deux organisations en mode automatique effectif pour un même membre : refus nommant les deux, « passez l'une d'elles en « au choix » ». S'applique aussi au changement du mode par défaut de l'organisation (tous les membres qui en héritent sont vérifiés).
- Agir sur un lien terminé : « Ce rattachement est déjà terminé. » ; `linkId` d'une autre organisation : `404`.
- `status` de liste inconnu : « Statut inconnu : ... Valeurs : ACTIVE, ENDED, ALL. »

Liaison de factures après coup : sans numéros ni (`memberId`, `from`, `to`) : refus ; période inversée : refus ; numéro inconnu : `404` listant les numéros introuvables.

État de compte : période inversée refusée ; plus de trois ans (3 × 366 jours) refusée.

Partage d'abonnement :
- L'abonnement doit appartenir à l'organisation (`404` sinon) et être ouvert pour attribuer une place.
- Le membre doit être rattaché (lien actif) ; une seule place par membre et par abonnement ; plus de place libre : « Toutes les places de l'abonnement sont occupées (n). »
- Descendre `maxSeats` sous le nombre de places occupées : « n places sont occupées : libérez-en avant de descendre à m. »
- `entitlementCode` inconnu de l'abonnement : « L'abonnement ... n'a pas de droit ... ».
- Somme des parts nominatives supérieure à ce que l'organisation a reçu pour la période (droit limité, mode `QUOTA` ou `QUOTA_AND_POOL`) : refus « Les parts distribuées sur ... dépassent ... ».
- À la réservation (module `booking`) : « Plafond de votre place atteint », « Votre part sur ce droit est épuisée », « le pot commun de l'organisation ne suffit pas », et, si le membre a des places dans plusieurs organisations sans en désigner une, « choisissez celle pour laquelle vous réservez (billingCompanyCode) ».

Passes partagés : pass clos (`CONSUMED`, `EXPIRED`, `CANCELLED`) : refus ; membre non rattaché : refus ; déjà un accès ouvert : refus ; `validUntil` avant `validFrom` : refus ; `maxUses` doit être positif.

Portefeuille et autorisations :
- Membre non rattaché : refus ; déjà une autorisation ouverte sur ce portefeuille : « modifiez-la » ; fin avant début : « La fin de l'autorisation précède son début. »
- Autorisation révoquée : toute modification refusée ; réactivation d'un collaborateur qui n'est plus rattaché : refusée.
- Recharge : `amount` strictement positif, `reason` de 5 caractères au moins.
- Refus au règlement (portail), à connaître pour les afficher côté gérant : opération non couverte, « Plafond par paiement / du jour / du mois dépassé », « Le portefeuille de l'organisation est bloqué », « Solde insuffisant », « Une demande attend déjà la validation du gérant pour cette facture », code de dépense absent, incorrect ou bloqué.

Profils légaux (`/businesses`) : messages **en anglais**, par exemple `400` `VALIDATION_FAILED` « Invalid business request » avec le détail dans `errors` (« the RCCM number is not valid »…), `409` « A business with this name already exists » (nom, NIU ou RCCM en double), `404` « Business with code ... not found ».

## À prévoir côté frontend

Écrans back-office et droits (pour masquer les boutons) :

| Écran | Routes | Droit exigé |
| --- | --- | --- |
| Liste et fiche d'organisation, blocage, mode de rattachement par défaut, partage, passes, portefeuille, autorisations, contrats, état de compte d'organisation | `/sni/api/v1/companies/**` | `ADMIN:ACCESS` **et** un rôle `ROLE_ADMIN`, `ROLE_SUPER_ADMIN` ou `ROLE_MANAGER` (règle `@PreAuthorize` sur chaque contrôleur) |
| Membres d'une organisation, organisations d'un membre | `/sni/api/v1/customers/{id}/members`, `/members/{id}/companies` | lecture `CLIENT:READ`, rattacher et terminer `CLIENT:CREATE`, modifier `CLIENT:UPDATE`, plus l'un des trois rôles |
| État de compte d'un membre | `/sni/api/v1/members/{id}/statement*` | `CLIENT:READ` plus l'un des trois rôles |
| Liaison de factures | `/sni/api/v1/billing/documents/company-links*` | prévisualiser et relier `BILLING:CREATE`, retirer `BILLING:CANCEL`, plus l'un des trois rôles |
| Paramètres : profils légaux, entité exploitante | `/sni/api/v1/businesses/**` | `SYSTEM:SETTINGS` |

Le détail exact de chaque route est dans `routes/`. Un compte qui a le droit mais pas l'un des trois rôles reçoit `403` sur les routes à `@PreAuthorize`.

Fiche d'organisation, onglets suggérés : Identité (bloc légal), Contact, Facturation (plafond, délai, encours, blocage), Membres, Abonnements partagés, Passes, Portefeuille, Contrats, État de compte.

Listes de référence à embarquer ou charger : types d'organisation et leurs identifiants proposés (la fiche renvoie `suggestedIdentifiers`), formes juridiques (`LegalForm`, pas de route dédiée), types d'identifiant (`IdentifierType`), régimes de TVA, rôles de lien, modes de rattachement, opérations de dépense, modes de partage. Devises et pays : module `socle`.

Fichiers : état de compte en PDF et CSV, contrat en PDF ; nom dans `Content-Disposition`. Aucun téléversement dans ce module.

Pièges :
- `PATCH .../legal` avec `identifiers` remplace toute la liste ; `PATCH .../spending-authorizations/{id}` remplace tous les plafonds et dates.
- Pour retirer un plafond d'encours, envoyer `removeCreditLimit: true`, pas `creditLimit: null`.
- `GET .../wallet` crée un portefeuille s'il n'existe pas : ne pas l'appeler pour une simple vérification d'existence, préférer `GET .../wallet/spends` (vide sans portefeuille) si c'est le besoin.
- Les opérations de fin (fin de rattachement, libération de place, révocation d'accès) **n'annulent pas** les réservations à venir : afficher `warning` / `futureBookings` et proposer d'aller les traiter dans le module `booking`.
- `outstanding` est recalculé à chaque lecture de la fiche ou de la liste ; ne pas le mettre en cache longtemps.
- La recharge manuelle n'accepte pas d'en-tête `Idempotency-Key` : désactiver le bouton pendant l'envoi pour éviter un double crédit.
- Pas de temps réel dans ce module.

## Effets automatiques

- **Expiration des dépenses** : `CompanySpendExpiryWorker` tourne toutes les 10 minutes (`bokati.company.wallet.approval-expiry-delay-ms`, 600 000 ms), par lots de 50. Il passe en `EXPIRED` les dépenses `PENDING_APPROVAL` dont le délai de validation est dépassé (48 heures par défaut, réglage `bokati.company.wallet.approval-hours`, modifiable dans les paramètres de la plateforme, valable pour les dépenses suivantes) et rend leurs fonds, puis prévient le collaborateur ; et les dépenses `AWAITING_OTP` dont le code (10 minutes) a expiré (`src/main/java/com/sni/bokaticowork/features/company/wallet/worker/CompanySpendExpiryWorker.java`).
- **Notifications** : une dépense qui attend validation envoie à chaque gérant actif une notification dans le portail (`COMPANY_SPEND_APPROVAL_REQUESTED`) et un courriel ; la décision (acceptée, refusée, expirée) notifie le collaborateur (`COMPANY_SPEND_DECIDED`) ; le code à usage unique part par courriel.
- **Création d'organisation** : dossier KYC initialisé et courriel de bienvenue au client (module `client`).
- **Mise à jour du bloc légal** : publie `CompanyLegalCardChangedEvent`, que le module `domiciliation` écoute pour relire la fiche des domiciliations en cours (`src/main/java/com/sni/bokaticowork/features/domiciliation/service/DomiciliationCardListener.java`).
- **Fin de rattachement** : libération des places, révocation des accès aux passes, suspension des autorisations, annulation des dépenses en attente, et éventuel retour du membre sur une fiche client personnelle (créée au besoin).
- **Place attribuée ou libérée** : le collaborateur est ajouté ou retiré comme partie `BENEFICIARY` du contrat de l'abonnement.
- **Factures de renouvellement** : une annexe liste ce que chaque collaborateur a pris sur la période écoulée, pour un abonnement partagé (`SeatUsageAnnexProvider`) et pour un pass d'organisation (`PassUsageAnnexProvider`).

## Écarts relevés

- `src/main/java/com/sni/bokaticowork/features/company/service/implementation/BusinessServiceImpl.java:100` : `PUT /sni/api/v1/businesses/{code}` appelle `business.getRccmNumber().equalsIgnoreCase(...)` sans tester `null`. Un profil légal créé par `POST /companies` sans RCCM (cas normal pour une association ou une ONG) fait échouer la modification en `500`.
- `src/main/java/com/sni/bokaticowork/features/company/dto/response/BusinessEntityResponse.java` : le drapeau `operator` n'est pas exposé. Le frontend ne peut pas savoir quelle entité est l'exploitante, alors que sa désignation conditionne la génération des contrats.
- `src/main/java/com/sni/bokaticowork/features/company/controller/BusinessController.java:57` et `:62` : `GET /businesses` et `/businesses/paged` mélangent l'entité exploitante et tous les profils légaux des organisations clientes (créés par `POST /companies`), sans filtre.
- `src/main/java/com/sni/bokaticowork/features/company/wallet/controller/CompanyWalletController.java:51` : la recharge manuelle du portefeuille (mouvement d'argent) n'est pas idempotente (`@Idempotent` absent), contrairement aux routes `/businesses`.
- `src/main/java/com/sni/bokaticowork/features/company/wallet/service/CompanyWalletService.java` (`view`, appelée par `GET .../wallet`) : une lecture crée le portefeuille (`walletService.getOrCreate`).
- `src/main/java/com/sni/bokaticowork/features/company/account/service/CompanyAccountService.java:220` : `block` passe en `SUSPENDED` quel que soit le statut courant, y compris `ARCHIVED` ou `INACTIVE`, hors de la table de transitions du module `client` ; le déblocage remet alors `ACTIVE`.
- `src/main/java/com/sni/bokaticowork/features/company/account/service/CompanyAccountService.java:384` : le message de refus de forme juridique omet `ETABLISSEMENT`, pourtant acceptée. Les libellés de `LegalForm` sont faux (`SARL` « Société Anonyme à responsabilité limitée », `SAS` « Société à Responsabilité Limitée », `src/main/java/com/sni/bokaticowork/features/company/enums/LegalForm.java:6`), sans effet visible car non exposés.
- `src/main/java/com/sni/bokaticowork/features/company/service/implementation/BusinessServiceImpl.java` : messages d'erreur en anglais, `created_by` et `updated_by` toujours `SYSTEM` (l'auteur réel n'est pas noté) ; le statut d'un profil légal n'a ni table de transitions ni effet lu dans le module.
- `src/main/java/com/sni/bokaticowork/features/company/membership/controller/CompanyMembershipController.java:41` et `:56` : rattacher et **terminer** un lien exigent `CLIENT:CREATE` (et non `CLIENT:UPDATE` ou `CLIENT:DELETE`) ; à connaître pour masquer les boutons.
- Création d'organisation : un courriel ou un téléphone en double renvoie un message anglais (« A customer with this email already exists ») venant du module `client`, alors que le reste du module répond en français.
