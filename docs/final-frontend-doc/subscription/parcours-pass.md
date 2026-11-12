# subscription : parcours des pass

> Partie de [README.md](README.md). Routes exactes et champs : [routes.md](routes.md). Règles communes : [README racine](../README.md).

Un pass est un titre détenu par un titulaire (`ownerType` + `ownerCode`, mêmes codes que pour un abonnement), valable de `validFrom` à `validUntil`, avec un nombre d'utilisations `maxUses` (vide : illimité). Il porte des droits (voir [parcours-droits-et-consommation.md](parcours-droits-et-consommation.md)), un contrat signé par le système, et des supports d'accès (QR code, code d'accès).

Le catalogue des pass se prépare dans [parcours-catalogue.md](parcours-catalogue.md#plans-de-pass). Côté membre, l'achat et le paiement se font depuis le portail (`/sni/api/v1/client/catalog/passes/purchase`, `/sni/api/v1/client/passes/...`) et les pass d'entreprise partagés avec leurs bénéficiaires depuis `/sni/api/v1/client/companies/{customerId}/passes/...` : voir [portal](../portal/README.md).

## Vendre un pass depuis un plan

Écran : « Vendre un pass » (titulaire, plan, version, devise, date de début, renouvellement).

1. Charger les plans actifs `GET /sni/api/v1/pass-plans?status=ACTIVE`, leurs versions `GET /sni/api/v1/pass-plans/{planCode}/versions` (ne proposer que `status: ACTIVE`) et les prix d'une version `GET /sni/api/v1/pass-plans/versions/{versionCode}/prices`.
2. Vendre : `POST /sni/api/v1/pass-plans/{planCode}/purchase` ([route](routes/PassPlanController.md#r-post-sni-api-v1-pass-plans-plancode-purchase), droit supplémentaire `PASS:WRITE`) avec :
   - `ownerType`, `ownerCode` (obligatoires) ;
   - `planVersionId` : le **`versionCode`** de la version (malgré son nom) ; omis, la version active la plus récente ;
   - `currency` (défaut `XAF`) ;
   - `validFrom` en texte : date-heure ISO avec fuseau (`2026-11-08T08:00:00Z`) ou sans fuseau (`2026-11-08T08:00`, lue à l'heure de Lagos) ; défaut maintenant. `validUntil` est calculée : début + durée de la version ;
   - `autoRenew` : retenu seulement si la version est `autoRenewable` ;
   - `idempotencyKey` : une clé par intention de vente (UUID), à garder pour les nouvelles tentatives ; rejouée, elle rend le pass déjà créé.
3. Réponse `201` `PassResponse` :
   - montant dû : `PENDING_ACTIVATION`, une facture « Facture pass `passNumber` » est émise ([billing](../billing/README.md)) et le dépôt éventuel est retenu sur le portefeuille ;
   - gratuit : activé tout de suite.
   Les droits de la version sont copiés sur le pass (durée propre `validForDays` ou validité du pass) et ouverts à l'activation.
4. L'écran affiche le pass et, s'il attend son paiement, le lien vers la facture.

Erreurs : `400` plan non actif ou KYC insuffisant (`KYC level N required for this pass plan`) ; `404` plan, version active ou prix dans la devise introuvable ; `409` clé d'idempotence déjà utilisée par un autre titulaire.

## Émettre un pass à la main

Pour un pass offert ou hors catalogue, sans facture.

1. `POST /sni/api/v1/passes` ([route](routes/PassController.md#r-post-sni-api-v1-passes)) avec `passType`, `ownerType`, `ownerCode`, `name`, `validFrom` (texte, mêmes formats que ci-dessus) obligatoires ; `validUntil`, `maxUses`, `transferable`, `shareable`, `description`, `subscriptionNumber` (rattacher à un abonnement), `planVersionId` (id numérique d'une version de plan d'**abonnement**, pour son exigence KYC), `idempotencyKey`, et `entitlements[]` (au moins un : `entitlementCode`, `quantity` ou `unlimited`, `validFrom`, `validUntil`).
2. Réponse `201` : le pass est **déjà `ACTIVE`**, ses droits ouverts, son contrat demandé. Aucun montant ni facture.

## Activer un pass

Normalement automatique : le paiement de la facture d'un pass `PENDING_ACTIVATION` l'active.

- Manuellement (règlement constaté hors système) : `POST /sni/api/v1/passes/{passNumber}/activate`, sans corps. Seulement depuis `PENDING_ACTIVATION` (`400` sinon ; un pass déjà actif est rendu tel quel).
- L'activation ouvre les droits, planifie le renouvellement si `autoRenew`, demande le contrat (`contractCode` apparaît quelques secondes après) et envoie un courriel.

## Consulter les pass

- Liste : `GET /sni/api/v1/passes` avec `ownerType`, `ownerCode`, `passType`, `status`, `expiringBefore` (date-heure) ; tri par défaut `createdAt,DESC`.
- Fiche : `GET /sni/api/v1/passes/{passNumber}` : statut, validité, `usedCount` / `maxUses`, montants, `autoRenew`, `nextRenewalDate`, `renewalCount`, `contractCode`, `entitlements` (droits accordés).
- Historique des statuts : `GET .../{passNumber}/history` ; événements : `GET .../{passNumber}/events` (type et contenu JSON). Ces deux routes renvoient des entités internes avec un objet `pass` complet et des `id` 64 bits : n'en lire que `fromStatus`, `toStatus`, `reason`, `changedBy`, `changedAt`, ou `eventType`, `payloadJson`, `occurredAt`.
- Renouvellement : `GET .../{passNumber}/renewal` (droit `SUBSCRIPTION:ACTIVATE`) : durée, prochaine date, période courante ; corps vide si le pass ne se renouvelle pas.

Il n'existe pas de route pour lister les usages ni les transferts d'un pass : ils se lisent dans les événements.

## Supports d'accès (QR code)

Un support est distinct du pass : on le révoque sans toucher au droit (téléphone perdu).

1. Afficher le support courant : `GET /sni/api/v1/passes/{passNumber}/credentials?type=QR_CODE` ([route](routes/PassCredentialController.md#r-get-sni-api-v1-passes-passnumber-credentials)). S'il n'existe pas ou s'il a expiré, le serveur en émet un nouveau (tournant). Réponse : `value` (le secret), `credentialType`, `rotating`, `validUntil`, `imageDataUri` (image du QR, seulement pour `QR_CODE`, à mettre dans un `<img src>`).
2. Un support **tournant** vit 15 minutes par défaut : rappeler la même route avant `validUntil` pour l'afficher en continu (borne, écran d'accueil).
3. Émettre un nouveau support : `POST .../{passNumber}/credentials?type=...&rotating=true|false&issuedBy=...`. Les supports actifs du même type sont révoqués. Un support non tournant vaut jusqu'à la fin du pass. Refusé pour un pass `CANCELLED` ou `EXPIRED` (`400`).
4. Tout révoquer : `DELETE .../{passNumber}/credentials?reason=...` rend `{ "revoked": n }`.

Types : `QR_CODE`, `BARCODE`, `NFC_TAG`, `ACCESS_CODE` (16 caractères sans O, I, L, 0, 1, pour être dictés).

## Contrôler l'entrée et consommer

Écran : poste d'accueil ou borne. Deux temps distincts : **valider** (question, rien n'est consommé) puis **utiliser** (acte).

1. Valider : `POST /sni/api/v1/passes/validate` ([route](routes/PassOperationController.md#r-post-sni-api-v1-passes-validate)) avec `credentialValue` (valeur scannée) **ou** `passNumber`, et si possible `bearerType` + `bearerCode` (la personne qui se présente), `locationCode`, `resourceTypeCode`. Réponse `200` : `{ "valid": true|false, "reason": "...", "passNumber": "...", "remainingUses": n|null }`. Afficher `reason` tel quel quand `valid` est faux.
2. Le serveur vérifie, dans l'ordre : le pass existe (un support révoqué ou expiré ne le retrouve pas) ; son statut (`ACTIVE` ou `PARTIALLY_USED`) ; ses dates ; les utilisations restantes ; la personne (titulaire, bénéficiaire désigné et son quota, ou refus si le pass n'est pas partageable) ; les règles de validité éventuelles (jour, plage horaire, site, type de ressource, dates bloquées, nombre d'usages par jour, usages simultanés).
3. Utiliser : `POST /sni/api/v1/passes/use` avec les mêmes champs plus `usageType` (`CHECK_IN` par défaut), `resourceCode`, `channel` (`QR_SCAN`, `MANUAL`, `KIOSK`, `API`), `validatedBy` (l'agent), et `referenceType` + `referenceCode` pour qu'un double scan rende le même usage au lieu d'en créer un second. Réponse `{ "usageNumber", "passNumber", "remainingUses" }` (`-1` : illimité). Un refus renvoie `400` avec le même motif que la validation.
4. Après utilisation, le pass passe `PARTIALLY_USED`, puis `CONSUMED` quand `maxUses` est atteint.
5. Annuler une utilisation (erreur de scan, contestation) : `POST /sni/api/v1/passes/usages/{usageNumber}/reverse?reason=...&reversedBy=...` (`reason` obligatoire). L'usage est conservé, marqué contre-passé ; le compteur et le statut du pass sont recalculés. Rejouer la route ne fait rien de plus.

Utiliser un pass ne débite pas ses droits : les droits se consomment par les réservations ([parcours-droits-et-consommation.md](parcours-droits-et-consommation.md)). Les bénéficiaires et les règles de validité n'ont pas de route d'administration dans ce module.

## Transférer un pass

Changement définitif de titulaire, accepté par le destinataire.

1. Demander : `POST /sni/api/v1/passes/{passNumber}/transfers?requestedBy=...` avec `toOwnerType`, `toOwnerCode` (obligatoires), `reason`, `transferFee`. Conditions : pass `transferable`, `ACTIVE` ou `PARTIALLY_USED`, non expiré, destinataire existant et différent, aucun transfert déjà en attente (`400` avec motif). Réponse `{ "transferNumber", "status": "PENDING_ACCEPTANCE" }` : à conserver, il n'y a pas de route de liste.
2. Le destinataire accepte : `POST /sni/api/v1/passes/transfers/{transferNumber}/accept?acceptedBy=...`. Le niveau KYC de la version est vérifié pour lui (`400`). Le pass change de titulaire ; les bénéficiaires désignés et les supports actifs sont révoqués (le nouveau titulaire doit obtenir son propre QR). Statut `COMPLETED`.
3. Ou refuse : `POST .../transfers/{transferNumber}/reject?reason=...` (`REJECTED`). L'émetteur peut retirer sa demande : `DELETE .../transfers/{transferNumber}?reason=...` (`CANCELLED`). Ces routes répondent `400` si le transfert n'est plus en attente.

`transferFee` est enregistré sans être facturé, et les droits accordés restent au nom de l'ancien titulaire (voir [Écarts](README.md#écarts-relevés)).

## Annuler un pass

`PATCH /sni/api/v1/passes/{passNumber}/cancel` avec `{ "reason": "..." }` facultatif (corps de type objet clé-valeur, seule la clé `reason` est lue). Le pass passe `CANCELLED` quel que soit son statut, ses droits actifs sont annulés, un courriel part. Aucun avoir ni remboursement automatique : le traiter dans [billing](../billing/README.md).

## Renouvellement et fin d'un pass

- Un pass en renouvellement automatique (version `autoRenewable` et `autoRenew` à l'achat) est prolongé d'une durée à `validUntil` par le worker (15 min) : nouvelle période, `usedCount` remis à zéro, `renewalCount` + 1, facture émise. Le renouvellement n'attend pas le paiement et ne rouvre pas de droits (voir [Écarts](README.md#écarts-relevés)). Seuls les pass `ACTIVE` sont renouvelés.
- Un pass `ACTIVE` ou `PENDING_ACTIVATION` dont `validUntil` est passée devient `EXPIRED` (worker toutes les 10 min). Le renouvellement et l'expiration se disputent la même échéance : le premier worker qui passe l'emporte.
- Alertes quotidiennes (8 h) mises en file pour le titulaire : expiration dans 7 jours, solde bas (20 % des utilisations), pass inutilisé depuis 14 jours. Elles apparaissent dans `GET /sni/api/v1/subscription-notifications` mais ne sont pas envoyées (voir [parcours-suivi.md](parcours-suivi.md#notifications-dabonnement)).
