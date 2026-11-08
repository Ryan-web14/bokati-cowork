# portal : organisations (gérant et collaborateur)

> Partie du [README du module portal](README.md). Routes exactes et modèles : [ClientCompanyController](routes/ClientCompanyController.md), [ClientCompanyPassController](routes/ClientCompanyPassController.md), [ClientCompanySharingController](routes/ClientCompanySharingController.md), [ClientCompanyWalletController](routes/ClientCompanyWalletController.md). Fonctionnement de fond (rattachement, modes de rattachement des factures, partage d'abonnement en places, accès aux pass, portefeuille d'organisation et autorisations de dépense) : [../company/README.md](../company/README.md). Règles communes : [README racine](../README.md).

Code : `src/main/java/com/sni/bokaticowork/features/portal/company/` ; services dans `src/main/java/com/sni/bokaticowork/features/company/` (`membership/`, `sharing/`, `wallet/`, `statement/`).

## Ce qui est propre au portail

- Un membre peut être rattaché à une ou plusieurs organisations (clients de type `COMPANY`), avec un rôle par rattachement : **`OWNER`** (gérant ou propriétaire) ou **`EMPLOYEE`** (collaborateur). Le rattachement lui-même se fait au back-office.
- **Routes du gérant** : `/sni/api/v1/client/companies/{customerId}/...`. Chacune vérifie que le membre a un rattachement **actif** de rôle `OWNER` sur cette organisation (`CompanyMembershipService.ownedCompany`, `src/main/java/com/sni/bokaticowork/features/company/membership/service/CompanyMembershipService.java:295`). Sinon `404` « Organisation introuvable », sans dire si elle existe. Un collaborateur reçoit donc `404` sur toutes ces routes.
- **Routes du collaborateur** (et de tout membre rattaché) : `/sni/api/v1/client/me/company-passes`, `/sni/api/v1/client/me/entitlements`, `/sni/api/v1/client/me/company-wallets/**`.
- Une facture « de l'organisation » pour le gérant = émise au nom de l'organisation, **ou** émise au nom d'un membre et reliée à l'organisation (`companyCustomerCode`). Les brouillons n'existent pas (`404` « Facture introuvable »).
- Le portefeuille **personnel** du gérant ne paie jamais pour l'organisation : les factures de l'organisation se règlent par mobile money, par annonce d'espèces ou par le portefeuille de l'organisation.
- Les actions du gérant sont journalisées (`@Audited`, module `COMPANY`).

## Parcours

### Mes organisations

1. `GET /sni/api/v1/client/companies` : une ligne par rattachement actif : `customerId`, `name`, `role`, `startedAt`, `effectiveInvoiceLinkMode` (`AUTOMATIC` : toute facture du membre est reliée d'office à l'organisation ; `ON_CHOICE` : « pour moi » ou « pour l'organisation » à chaque fois), `companyDefaultMode` (rempli pour le gérant seulement, `null` pour un collaborateur).
2. Selon `role`, afficher l'espace gérant (`OWNER`) ou l'espace collaborateur (`EMPLOYEE`).

### Gérant : membres et rattachement des factures

1. Membres : `GET /sni/api/v1/client/companies/{customerId}/members` : rattachements actifs (`id` numéro court, `memberId`, `memberName`, `memberEmail`, `role`, `invoiceLinkMode` propre au lien, `effectiveInvoiceLinkMode`, `canSign`, `canCollectMail`).
2. Mode par défaut de l'organisation : `PATCH .../invoice-link-mode` avec `{ "mode": "AUTOMATIC" | "ON_CHOICE" }`. Réponse : l'organisation (`MyCompany`).
3. Mode d'un collaborateur : `PATCH .../members/{linkId}/invoice-link-mode` avec `{ "mode": ... }`, ou `{ "useCompanyDefaultMode": true }` pour revenir au défaut.

Règle : un membre n'a qu'**une** organisation en mode automatique (mode hérité compris). Sinon `400` « <nom> aurait ses factures reliées automatiquement à A et B. Un membre n'a qu'une organisation en mode automatique : passez l'une d'elles en « au choix ». » (le refus annule tout le changement). `400` « Ce rattachement est déjà terminé. » sur un lien clos.

### Gérant : compte de l'organisation

1. État de compte : `GET .../{customerId}/statement?from=&to=` et `.../statement.pdf` (mêmes règles de période que l'état de compte personnel : douze mois par défaut, trois ans au plus).
2. Facture : `GET .../{customerId}/invoices/{documentNumber}` et `.../pdf`.
3. Payer par mobile money : `POST .../{customerId}/invoices/{documentNumber}/pay/mobile-money` avec `{ "phoneNumber", "correspondent" }`, puis suivi par `GET .../{customerId}/mobile-money/deposits/{depositId}` (accepte seulement les dépôts de factures de l'organisation).
4. Annoncer des espèces : `POST .../{customerId}/invoices/{documentNumber}/pay/cash` avec `{ "amount", "note" }` (facultatifs). Suivi et annulation de l'annonce : non disponibles sous `/companies` (non vérifié : `GET /sni/api/v1/client/billing/cash-declarations` ne liste que les annonces au nom du membre).
5. Payer avec le portefeuille de l'organisation : voir [Gérant : portefeuille de l'organisation](#gérant--portefeuille-de-lorganisation).
6. Reçus : `GET .../{customerId}/payments/{transactionNumber}/receipt` et `.../receipt/pdf`.

### Gérant : abonnements partagés

1. Abonnements de l'organisation : `GET .../{customerId}/subscriptions`.
2. Vue de partage d'un abonnement : `GET .../{customerId}/subscriptions/{subscriptionNumber}/sharing` (places, règles par droit, consommation). Consommation par collaborateur sur tous les abonnements : `GET .../{customerId}/usage`.
3. Règle d'un droit : `PUT .../subscriptions/{subscriptionNumber}/sharing/{entitlementCode}` avec `{ "mode", "defaultQuota", "seatCap" }`. `mode` : `POOL` (pot commun, chaque place y puise sous son plafond), `QUOTA` (part fixe par place, rien au-delà), `QUOTA_AND_POOL` (sa part, puis le pot restant). `defaultQuota` sert en `QUOTA` et `QUOTA_AND_POOL`, `seatCap` partout.
4. Nombre de places : `PATCH .../subscriptions/{subscriptionNumber}/max-seats` avec `{ "maxSeats" }` (vide : le plafond vendu). Refus `400` au-delà du plafond vendu (« L'abonnement compte N places au plus... contactez l'administration. ») ou en dessous des places occupées (« N places sont occupées : libérez-en avant de descendre à M. »).
5. Attribuer une place : `POST .../subscriptions/{subscriptionNumber}/seats` avec `{ "memberId", "allowances": [ { "entitlementCode", "quota", "cap" } ] }` (`allowances` facultatif : sinon la règle du droit).
6. Ajuster une place : `PATCH .../seats/{seatId}` avec `{ "allowances": [...] }` (remplace droit par droit, les autres inchangés).
7. Libérer une place : `POST .../seats/{seatId}/release` avec `{ "reason" }` facultatif.

### Gérant : pass de l'organisation

1. Liste : `GET .../{customerId}/passes`.
2. Acheter au nom de l'organisation : `POST .../{customerId}/passes/purchase` avec `{ "planCode", "passType", "name", "idempotencyKey" }` (même corps que l'achat personnel). La facture est à l'organisation.
3. Bénéficiaires d'un pass : `GET .../passes/{passNumber}/beneficiaries`.
4. Ouvrir l'accès à un collaborateur rattaché : `POST .../passes/{passNumber}/beneficiaries` avec `{ "memberId", "maxUses", "validFrom", "validUntil" }` (seul `memberId` obligatoire ; `validFrom`/`validUntil` sont des dates-heures avec fuseau).
5. Retirer l'accès : `POST .../passes/{passNumber}/beneficiaries/{accessId}/revoke` avec `{ "reason" }` facultatif.

### Gérant : portefeuille de l'organisation

1. Vue : `GET .../{customerId}/wallet?currency=` : `wallet` (solde), `entries` (mouvements, avec le collaborateur qui a dépensé), `pendingApprovals`.
2. Dépenses : `GET .../wallet/spends?currency=&status=` (`status` parmi `AWAITING_OTP`, `HELD`, `PENDING_APPROVAL`, `EXECUTED`, `REJECTED`, `EXPIRED`, `CANCELLED`).
3. Recharger par mobile money : `POST .../wallet/top-ups` avec `{ "amount", "phoneNumber", "correspondent" }`. Le suivi du dépôt n'est pas possible par les routes du portail (voir écarts du README).
4. Code secret du portefeuille de l'organisation : `POST .../wallet/security/pin` avec `{ "currentPin", "newPin" }` (`204`).
5. Payer une facture de l'organisation : `POST .../{customerId}/invoices/{documentNumber}/pay/wallet` avec `{ "pin" }` (code du portefeuille de l'organisation, si la politique l'exige). Réponse `SpendResult` : `spend` (statut) et `message` à afficher. Au-delà du seuil de la politique, la dépense passe `AWAITING_OTP` et un code à usage unique est envoyé **par courriel** au payeur : le saisir avec `POST .../wallet/spends/{spendId}/confirm` et `{ "code" }`.
6. Autorisations des collaborateurs :
   - liste : `GET .../{customerId}/spending-authorizations` ;
   - créer : `POST .../spending-authorizations` avec `{ "memberId", "operations" (au moins une parmi `BOOKING`, `SUBSCRIPTION`, `PASS`, `INVOICE_PAYMENT`...), "maxPerTransaction", "maxPerDay", "maxPerMonth", "approvalAbove", "validFrom", "validUntil" }` ;
   - modifier : `PATCH .../spending-authorizations/{authorizationId}` (mêmes champs sans `memberId`) ;
   - réinitialiser le code de dépense d'un collaborateur : `POST .../spending-authorizations/{authorizationId}/pin/reset` (il en posera un nouveau sans l'ancien).
7. Validations : `GET .../{customerId}/approvals` (dépenses `PENDING_APPROVAL`), puis `POST .../approvals/{spendId}/approve` ou `POST .../approvals/{spendId}/reject` avec `{ "reason" }` facultatif. Sans décision dans le délai, la dépense est annulée et les fonds réservés libérés.

### Collaborateur : ce que l'organisation me donne

1. Mes places sur les abonnements partagés : `GET /sni/api/v1/client/me/entitlements` : par organisation et abonnement, ma part, mon plafond, le pot restant, par droit.
2. Mes accès aux pass de l'organisation : `GET /sni/api/v1/client/me/company-passes`.
3. Réserver avec ces droits : [parcours-reservations.md](parcours-reservations.md) avec `paymentMode` `SUBSCRIPTION` ou `PASS` (et `billingCompanyCode` si nécessaire).

### Collaborateur : payer avec le portefeuille de l'organisation

1. Mes autorisations : `GET /sni/api/v1/client/me/company-wallets` : par organisation, `authorization` (opérations permises, plafonds, `approvalAbove`, validité) et `mySpends` (mes seules dépenses ; le solde n'est pas montré).
2. Poser ou changer mon code de dépense : `POST /sni/api/v1/client/me/company-wallets/{customerId}/pin` avec `{ "currentPin", "newPin" }` (`204`). Le code doit avoir la longueur imposée, ne pas répéter un seul chiffre ni former une suite.
3. Payer une facture de l'organisation ou reliée à elle : `POST /sni/api/v1/client/me/company-wallets/{customerId}/invoices/{documentNumber}/pay` avec `{ "pin" }`. Réponse `SpendResult` :
   - `EXECUTED` : réglé ;
   - `AWAITING_OTP` : saisir le code reçu par courriel via `POST .../{customerId}/spends/{spendId}/confirm` avec `{ "code" }` ;
   - `PENDING_APPROVAL` : au-delà de `approvalAbove`, attente du gérant ; fonds réservés ; annulation automatique sans réponse dans le délai annoncé par `message`.
4. Annuler ma dépense en attente : `POST .../{customerId}/spends/{spendId}/cancel`.

## Règles et erreurs à refléter dans l'écran

| Cas | Statut | Message |
| --- | --- | --- |
| Membre non gérant actif | `404` | `Organisation introuvable` |
| Facture ni à l'organisation ni reliée, ou brouillon | `404` | `Facture introuvable` |
| Facture déjà réglée | `400` | `Cette facture est déjà réglée.` |
| Collaborateur non rattaché | `400` | `<nom> n'est pas rattaché à <organisation>...` |
| Autorisation en double | `400` | `<nom> a déjà une autorisation sur ce portefeuille : modifiez-la.` |
| Opération non couverte | `400` | `Votre autorisation ne couvre pas ce type de dépense (...).` |
| Code de dépense absent, bloqué, faux | `400` | `Définissez d'abord votre code de dépense pour ce portefeuille.`, `Code de dépense bloqué après trop d'essais, jusqu'à ...`, `Code de dépense incorrect : N essai(s) restant(s).` |
| Code à usage unique expiré ou faux | `400` | `Ce code n'est plus valable : recommencez le règlement.`, `Code de confirmation incorrect : N essai(s) restant(s).` |
| Plafonds | `400` | `Plafond par paiement dépassé : ...`, `Plafond du jour dépassé : ...`, `Plafond du mois dépassé : ...` |
| Portefeuille d'organisation bloqué, solde insuffisant | `400` | `Le portefeuille de l'organisation est bloqué : aucune dépense n'y passe.`, `Solde insuffisant sur le portefeuille de l'organisation : ...` |
| Demande déjà en attente pour la facture | `400` | `Une demande attend déjà la validation du gérant pour cette facture.` |
| Validation hors délai ou déjà tranchée | `400` | `Le délai de validation est passé : la dépense est annulée.`, `Cette dépense n'attend plus de validation (...).` |

## À prévoir côté frontend

- Un sélecteur d'organisation alimenté par `GET /sni/api/v1/client/companies`, et deux espaces distincts selon `role`.
- Espace gérant : membres et modes, état de compte et factures, paiements, abonnements partagés (places et règles), pass et bénéficiaires, portefeuille (vue, recharge, code, autorisations, validations).
- Espace collaborateur : mes droits partagés, mes accès aux pass, mes autorisations de dépense, paiement avec code et éventuel code à usage unique, suivi de mes dépenses.
- Les `id`/`linkId`/`seatId`/`accessId`/`authorizationId`/`spendId` sont des numéros courts.
- Notifications à relier : `COMPANY_SPEND_APPROVAL_REQUESTED` (gérant : une dépense attend), `COMPANY_SPEND_DECIDED` (collaborateur : décision), `COMPANY_WALLET_SPEND` ; leur `message` est vide dans la liste REST, afficher `title`.
