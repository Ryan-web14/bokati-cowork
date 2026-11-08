# subscription : parcours des droits et de la consommation

> Partie de [README.md](README.md). Routes exactes et champs : [routes.md](routes.md). Règles communes : [README racine](../README.md).

Un **droit accordé** (grant) est un solde ouvert pour un titulaire (`ownerType` + `ownerCode`) sur une définition de droit : quantité accordée, quantité restante, ou illimité, valable de `validFrom` à `validUntil`. Il naît de l'activation ou du renouvellement d'un abonnement (un droit par ligne de la version de plan, pour la période), de l'activation d'un pass, ou d'un report. Les réservations ([booking](../booking/README.md)) consomment ces droits par les routes `/internal/entitlements/*` ; le back-office les utilise pour consulter et corriger.

Les droits d'un titulaire sont consommés dans l'ordre : `priority` croissante, puis la fin de validité la plus proche. Le statut de l'abonnement ou du pass d'origine n'est pas regardé : seul compte le statut du droit (`ACTIVE`) et sa validité.

## Consulter les soldes

1. Soldes en cours d'un titulaire : `GET /sni/api/v1/entitlements/balances?ownerType=MEMBER&ownerCode=...` ([route](routes/EntitlementController.md#r-get-sni-api-v1-entitlements-balances)) : droits actifs et valables maintenant. Afficher par droit `entitlementName`, `quantityRemaining` / `quantityGranted` (ou « illimité » si `unlimited`), `validUntil`, et l'origine (`sourceType` `SUBSCRIPTION`, `PASS`, `ROLLOVER` ; `sourceId` = numéro d'abonnement, de pass ou de droit d'origine).
2. Recherche complète : `GET /sni/api/v1/entitlements/grants` avec `ownerType`, `ownerCode`, `entitlementCode`, `status`, `validAt` (date-heure), paginé.
3. Droits d'un abonnement : `GET /sni/api/v1/subscriptions/{subscriptionNumber}/entitlements` (toutes périodes).

Le membre voit ses soldes depuis le portail (`GET /sni/api/v1/client/entitlements/balances`).

## Vérifier, retenir, consommer, rendre

Ces routes exigent `ADMIN:ACCESS`. Corps commun `EntitlementOperationRequest` : `ownerType`, `ownerCode`, `entitlementCode`, `quantity` (positive) obligatoires ; `referenceType` + `referenceId` (ce qui consomme, par exemple `BOOKING` et le numéro de réservation) ; `idempotencyKey` ; `reason` ; `holdUntil` ; `beneficiaryType` + `beneficiaryCode` (la personne qui consomme pour le compte du titulaire) ; `scopeType` (`SUBSCRIPTION` ou `PASS`) + `scopeNumber` pour ne puiser que dans les droits d'un abonnement ou d'un pass précis.

| Étape | Route | Effet | Réponse et erreurs |
| --- | --- | --- | --- |
| Vérifier | `POST /sni/api/v1/internal/entitlements/check` | Rien n'est écrit. | `allowed`, `availableQuantity`, `message` (anglais). |
| Retenir | `POST .../internal/entitlements/reserve` | `referenceType` et `referenceId` obligatoires (`400`). Le solde est **débité tout de suite**, la retenue expire à `holdUntil` (15 min par défaut) et rend alors la quantité (worker toutes les 5 min). | `409` `insufficient balance`. Une `idempotencyKey` déjà vue rend la vérification sans rien retenir. |
| Consommer | `POST .../internal/entitlements/consume` | Avec une retenue active sur la même référence : la retenue passe consommée, sans nouveau débit (`409` si la quantité retenue est inférieure). Sans retenue : débit direct. | `409` `insufficient balance`. |
| Rendre une retenue | `POST .../internal/entitlements/release` | Les retenues actives de la référence sont rendues au solde. | Sans retenue active, la quantité est **créditée quand même** sur le premier droit utilisable : n'appeler `release` que pour une retenue existante. |
| Rembourser | `POST .../internal/entitlements/refund` | Crédite la quantité sur le premier droit utilisable du titulaire. | `400` s'il n'a aucun droit actif sur ce code. |

Quand un droit passe 80 % puis 100 % de consommation, une alerte `ENTITLEMENT_LOW_BALANCE` puis `ENTITLEMENT_EXHAUSTED` est mise en file des notifications d'abonnement (non envoyée, voir [parcours-suivi.md](parcours-suivi.md#notifications-dabonnement)). Un droit à zéro passe `DEPLETED` ; il redevient `ACTIVE` si une quantité lui est rendue.

## Relevés d'usage

Un relevé d'usage enregistre une consommation constatée (impressions, heures) et, au besoin, la facture.

1. Enregistrer : `POST /sni/api/v1/usage-records` ([route](routes/UsageRecordController.md#r-post-sni-api-v1-usage-records)) avec `ownerType`, `ownerCode`, `entitlementCode`, `quantity`, `unit`, `referenceType`, `referenceId` (obligatoires), et :
   - `consumeEntitlement` : vrai par défaut ; faux pour un simple relevé sans débit ;
   - `billable` + `billableAmount` + `currency` : facturer le relevé hors dépassement (`400` si le montant ou la devise manque) ;
   - `beneficiaryType`, `beneficiaryCode`, `scopeType`, `scopeNumber`, `occurredAt`, `metadataJson`.
2. Si le solde suffit, il est débité. Sinon la politique de dépassement de l'abonnement s'applique (voir ci-dessous).
3. Réponse `UsageRecordResponse` : `status` `RECORDED`, ou `BILLED` avec `billableNumber` si une facture a été émise.
4. Lister : `GET /sni/api/v1/usage-records` (filtres `ownerType`, `ownerCode`, `entitlementCode`, `referenceType`, `referenceId`, `status`), tri par défaut `occurredAt,DESC`.

## Dépassements

Quand un relevé dépasse le solde, le serveur cherche l'abonnement concerné (celui de `scopeNumber`, sinon l'abonnement courant actif du titulaire) et la politique de dépassement de sa version de plan pour ce droit ([parcours-catalogue.md](parcours-catalogue.md#politiques-de-dépassement)) :

- aucune politique, ou aucun abonnement actif : `409`, rien n'est enregistré ;
- `BLOCK` : `409` `insufficient balance and overage is blocked` ;
- `ALLOW_UNBILLED` : le reste du solde est débité, le dépassement est enregistré sans facture ;
- `BILLABLE` : le reste du solde est débité, la quantité au-delà de `freeQuantity` est facturée à `unitPrice` ; une charge de dépassement est créée.

Consulter les charges : `GET /sni/api/v1/subscription-overage/charges` (filtres `subscriptionNumber`, `ownerType`, `ownerCode`, `entitlementCode`, liste non paginée) : `overageQuantity`, `unitPrice`, `amount`, `billableNumber`, `usageNumber`.

## Reports de reliquat

Si une ligne de la version de plan autorise le report (`rolloverAllowed`), le reliquat d'un droit d'abonnement arrivé à échéance est reporté sur la période courante, dans la limite de `rolloverLimit`, sous forme d'un nouveau droit `sourceType: ROLLOVER`, consommé avant les autres. L'abonnement doit être `ACTIVE`.

- Automatique : worker toutes les 15 min.
- Manuel : `POST /sni/api/v1/subscription-rollovers/apply-due` rend le nombre de droits examinés.
- Historique : `GET /sni/api/v1/subscription-rollovers?subscriptionNumber=...&entitlementCode=...` : `sourceGrantNumber`, `rolloverGrantNumber`, `quantity`.
