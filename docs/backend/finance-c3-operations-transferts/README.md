# Lot C3 : opérations de caisse et transferts

Quatrième lot du [plan finance](../plan-finance-comptabilite/README.md), détaillé dans [caisses-et-sessions.md](../plan-finance-comptabilite/caisses-et-sessions.md), sections 7, 8 et 9. Lots précédents : [C0](../finance-c0-caisse-correctifs/README.md), [C1](../finance-c1-referentiel-caisses/README.md), [C2](../finance-c2-sessions-comptages/README.md).

Branche `feature/finance-comptabilite`. Migrations : dev `V294__caisse_operations_transferts_c3.sql`, prod `V288__caisse_operations_transferts_c3.sql` (contenu identique).

## Natures d'opération

Table `cash_operation_nature`. Le caissier choisit une nature (« Achat de fournitures de bureau ») ; le reste en découle :

- le type technique (`CASH_IN` ou `CASH_OUT`) et le sens ;
- le compte proposé (complété au lot F3, à valider par l'expert-comptable) ;
- le justificatif exigé et son délai ;
- le plafond par opération, avec 50 000 XAF pour les dépenses (décision 7b) : au-delà, une demande de fonds est obligatoire ;
- l'éventuelle demande préalable obligatoire (avance à justifier) ;
- le seuil de double visa propre à la nature.

Il y a 17 natures initiales, réparties en familles : encaissements, achats, déplacements, avances, remises, transferts, remboursements, divers.

- `GET /cash-registers/natures?direction=&family=&search=` : les plus utilisées d'abord dans chaque famille ; la recherche porte sur le libellé, le code et les mots-clés, sans accents.
- `PUT /cash-registers/natures` (`CASH_REGISTER:MANAGE`) : liste complète. Une nature absente est désactivée, jamais supprimée.
- Mouvements, bons d'entrée et bons de sortie acceptent `natureCode`. Il remplace `flowCategory` (rempli avec le code), et son libellé sert de motif par défaut. `natureCode` reste facultatif tant que l'interface n'est pas passée aux natures.

## Mouvements en attente et double visa

- Un **ajustement** (`ADJUSTMENT`) est toujours créé en `PENDING` : il attend le visa d'un superviseur. **Changement de comportement** : un ajustement ne touche plus l'attendu tant qu'il n'est pas visé.
- Une **sortie au-delà du seuil de double visa** est créée en `PENDING`. Le seuil est celui de la nature, sinon celui de la caisse (`doubleVisaThreshold`) : 50 000 XAF pour les caisses non automatiques, et vide (aucun visa exigé) pour une caisse qui n'en a pas.
- En attente, l'opération ne touche pas au solde. Elle expire en `CANCELLED` sans visa dans le délai (`bokati.cash.visa.expiry-hours`, 24 h).
- `PATCH /cash-registers/movements/{n}/approve` (`CASH_MOVEMENT:APPROVE`) : par une autre personne que l'auteur et le caissier de la session.
- `PATCH .../movements/{n}/cancel` (motif) : l'auteur ou un viseur annule une opération en attente.
- Une session qui a des opérations en attente ne se clôt pas et ne se passe pas.
- Une demande de caisse approuvée (circuit existant) vaut second visa pour le mouvement qu'elle produit.

## Contre-passation

`POST /cash-registers/movements/{n}/reverse` (motif de la liste `REVERSAL_REASON`) :

- seul un mouvement `CONFIRMED` se contre-passe. Un paiement ou un remboursement passe par la transaction de paiement, un transfert par son ordre ;
- le mouvement inverse est un ajustement de sens contraire, de même montant, lié à l'original, qui passe `REVERSED`. Les deux s'annulent dans le solde ;
- il s'inscrit dans la session de l'original si elle est ouverte, sinon dans la session ouverte de la même caisse. Une session close n'est jamais réécrite ;
- le caissier contre-passe seul son propre mouvement dans les 15 minutes et sous 50 000 XAF (`bokati.cash.reversal.cashier-minutes`, `.cashier-max-amount`). Au-delà, il faut `CASH_MOVEMENT:REVERSE`.

## Transferts internes à deux temps (`OTI`)

1. **Envoi** : `POST /cash-registers/transfers` (`CASH_TRANSFER:SEND`), avec la caisse d'origine, la destination (déduite : la caisse mère), le montant ou son billetage, et le porteur.
   - L'émetteur doit avoir une session ouverte sur la caisse d'origine.
   - Un `TRANSFER_OUT` sort l'argent, et l'ordre passe `IN_TRANSIT`.
   - Le type (`FEED`, `REMITTANCE`, `SAFE_DEPOSIT`, `SAFE_WITHDRAWAL`) est déduit.
2. **Circuits** : caisse mère et caisses filles, et coffre du même site. Les autres sont refusés.
3. **Réception** : `PATCH /cash-registers/transfers/{n}/receive` (`CASH_TRANSFER:RECEIVE`), par une **autre personne** que l'émetteur, qui a une session ouverte sur la destination.
   - Elle compte ce qu'elle reçoit, et un `TRANSFER_IN` du montant reçu est créé.
   - Montant différent : un motif est exigé, l'ordre passe `DISPUTED`, et une revue d'écart `TRANSFER` est créée (décidée comme les autres, lot C2).
4. **Annulation** : `PATCH .../transfers/{n}/cancel` par l'émetteur tant que l'ordre est en transit ; la sortie est contre-passée.
5. **Transit trop long** : alerte au-delà de 2 h (`bokati.cash.transfer.max-transit-hours`).
6. **Coffre** : il n'a pas de session de caissier. Ses mouvements datés vont dans une session permanente ouverte au nom du coffre.

**Remise de fin de session** : à la clôture comptée (lot C2), ce qui dépasse le fonds gardé part à la caisse mère par un transfert `REMITTANCE` en transit. Le dernier comptage de la caisse devient alors le fonds gardé. Les caisses existantes, sans fonds standard, gardent tout et ne remettent rien, comme avant.

## Échange de monnaie

`POST /cash-registers/sessions/{n}/exchanges` : coupures données et reçues, de même total. Un mouvement `EXCHANGE` neutre est créé, et le détail des coupures est enregistré.

## Petite caisse à fonds fixe

- `GET /cash-registers/{code}/petty-cash/refill` : la réalimentation proposée, c'est-à-dire le total des sorties confirmées depuis la dernière réalimentation.
- `POST .../petty-cash/refill` : crée le transfert `PETTY_REFILL` de la caisse mère vers la petite caisse, du montant calculé par le système.

## Règle unique de sens

`EXCHANGE` est neutre ; `VARIANCE` (réservé à la comptabilité) suit son sens. Les statistiques de session utilisent désormais la règle unique (C0).

## Droits

| Permission | Routes | Attribution |
| --- | --- | --- |
| `CASH_TRANSFER:SEND` | envoi, annulation, réalimentation de petite caisse | qui avait `CASH_ADJUST`, administrateurs |
| `CASH_TRANSFER:RECEIVE` | réception | qui avait `CASH_ADJUST`, administrateurs |
| `CASH_MOVEMENT:APPROVE` | visa | (C0) |
| `CASH_MOVEMENT:CREATE` | contre-passation (le service exige `CASH_MOVEMENT:REVERSE` hors délai), annulation, échange | (C0) |
| `CASH_REGISTER:MANAGE` | natures | (C0) |

## Tests

- `CashOperationsServiceImplTest` :
  - transfert de 200 000 reçu pour 195 000, motif exigé puis contestation et revue ;
  - l'émetteur ne réceptionne pas ;
  - circuit refusé ;
  - annulation d'un transfert ;
  - contre-passation dans le délai qui rétablit le solde ;
  - limites de la contre-passation ;
  - second visa ;
  - échange équilibré ;
  - réalimentation de la petite caisse.
- `CashCorrectionsC0Test` : l'ajustement attend son visa.
- `CashOperationsC3IT` (PostgreSQL) : natures initiales, contrainte sur les transferts, schéma validé.
- `CashRoutesMappingTest` : nouvelles routes.
- Suite unitaire complète : 2 028 tests, 0 échec.

## Pour le frontend

À reporter dans la doc frontend finale :

- **Saisie d'une opération** : choisir la nature par famille avec recherche, et n'afficher que les champs utiles (justificatif si exigé). Le plafond et le visa sont annoncés par la réponse : statut `PENDING` et `expiresAt`.
- **Écrans** :
  - opérations à viser ;
  - contre-passation, avec choix du motif ;
  - transferts en transit, à envoyer et à recevoir, avec leur ancienneté ;
  - échange de monnaie ;
  - réalimentation de la petite caisse ;
  - administration des natures.
- **Champs ajoutés à un mouvement** : `natureCode`, `transferOrderNumber`, `approvedBy`, `approvedAt`, `expiresAt`, `reversalReason`, `cancelReason`.
