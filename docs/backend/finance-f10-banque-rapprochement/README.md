# Lots F10 et C5 : banque, chèques, mobile money, rapprochement ; devises des caisses

Lots du [plan finance](../plan-finance-comptabilite/README.md), détaillés dans [rapprochement-et-comptes-tiers.md](../plan-finance-comptabilite/rapprochement-et-comptes-tiers.md), section 1, et [caisses-et-sessions.md](../plan-finance-comptabilite/caisses-et-sessions.md) (instruments et devises, lot C5). S'appuie sur F1 à F4 (pièces, moteur, caisse, encaissements).

Branche `feature/finance-comptabilite`. Migrations : dev `V304__banque_rapprochement_f10_c5.sql`, prod `V298__banque_rapprochement_f10_c5.sql` (contenu identique). Code : `BankService`, `ChequeService`, `ExchangeRateService`, `BankController` dans `features/accounting`.

## Comptes suivis

`bank_account` : code, type (`BANK`, `MOBILE_MONEY`, `CARD`), banque, agence, libellé, RIB, devise, compte comptable, journal, modèle d'import, solde et date de début du suivi. Livrés : `BANQUE-PRINCIPALE` (`5211`, `BQ-5211`), `PAWAPAY` (`5212`, `MM-5212`), `MM-MARCHANDS` (`5213`, `MM-5213`). Un nouveau compte sans numéro reçoit un compte sous `521` et son journal (`BQ-` ou `MM-`).

Ce qui précède la **date de début du suivi** est couvert par le **solde d'ouverture** : ni suspens, ni à pointer. Ce solde doit figurer en comptabilité (à-nouveaux) pour que le rapprochement tombe juste.

## Relevés

- **Modèle d'import** (`bank_import_template`, format CSV) : séparateur, lignes d'en-tête, colonnes (par nom d'en-tête ou numéro) de date, date de valeur, libellé, référence, montant signé ou débit et crédit, format de date, séparateur décimal, encodage. `POST /bank-import-templates/{code}/preview` lit un fichier sans rien enregistrer. OFX et MT940 ne sont pas encore lus : passer par un export CSV de la banque.
- **Import** d'un fichier (`multipart`, solde initial et final facultatifs) ou **saisie manuelle** (JSON) d'après un relevé papier.
- **Contrôles** :
  - le solde initial égale le solde final du relevé précédent et la période se suit, sinon « relevé manquant » : alerte (`accounting.bank.missing-statement = ALERT`) ou refus (`REFUSE`) ;
  - la somme des lignes fait passer du solde initial au solde final, sinon refus ;
  - **pas de doublon** : chaque ligne a une empreinte (date, montant, libellé, référence) ; une ligne déjà importée est ignorée et comptée, ce qui permet de réimporter un fichier qui chevauche le précédent.
- Un relevé ne se modifie pas : un nouvel import avec `replace=<numéro>` le remplace (l'ancien passe `REPLACED`).

## Pointage

Associer des lignes du relevé (montant signé, entrée positive) et des lignes d'écriture du compte (débit moins crédit) de même total ; les deux côtés reçoivent un code de pointage (`P000001`), distinct du lettrage (`acc_entry_line.matching_code`).

**Automatique**, à chaque import et sur demande (`POST /bank-accounts/{code}/matching/auto`) :

1. **référence** : la référence, le libellé ou le paiement attendu de la ligne contient la référence, le numéro de pièce ou le numéro du fait (transaction, bordereau `BRB-`, cheque, ordre `OTI-`, facture), pour le même montant ;
2. **montant exact et date proche** (`accounting.bank.date-window-days`, 5 jours), correspondance unique dans les deux sens ;
3. **combinaison** : une ligne de relevé égale à la somme de 2 à `accounting.bank.max-combined-lines` (10) écritures de même sens dans la fenêtre (remise de plusieurs chèques).

Les correspondances sûres (confiance 90 et plus) sont pointées d'office (`accounting.bank.auto-match`) ; les autres apparaissent comme **propositions** sur l'écran de pointage.

**Manuel** : `POST /bank-accounts/{code}/matching` avec les lignes des deux côtés, totaux égaux. **Dépointage** : `DELETE .../matching/{code}` avec motif, tracé ; un pointage d'un rapprochement validé demande `BANK_RECONCILIATION:UNLOCK`.

## Lignes du relevé sans écriture

`POST /bank-accounts/{code}/statement-lines/{id}/post` :

- **règle d'affectation** (`bank_assignment_rule` : motif du libellé en expression régulière, sens, montants mini et maxi, compte, tiers, priorité) : livrées « frais et commissions » (`631`) et « intérêts créditeurs » (`77`) ; sans indication, la première règle qui s'applique ; écriture validée au journal de la banque puis pointée d'un seul geste ;
- **compte** indiqué (avec tiers) : même chose ; `4719` pour un encaissement à identifier ;
- **virement client** (`invoiceNumber`) : un paiement par virement est enregistré dans le module de paiement et imputé à la facture ; son écriture d'encaissement (lot F4) se pointera d'elle-même par la référence du paiement (`awaitingPayment`). Il n'y a jamais d'encaissement client écrit en comptabilité sans paiement. Le compte de trésorerie d'un virement est celui du réglage `accounting.payment.BANK_TRANSFER.account` (`5211`).

## État de rapprochement

- `POST /bank-accounts/{code}/reconciliations?date=` prépare l'état : solde du relevé (ouverture plus lignes jusqu'à la date), solde comptable, écritures en suspens, lignes du relevé non comptabilisées, solde rapproché, **écart**.
- `PATCH /reconciliations/{id}/validate` : par une **autre personne** que le préparateur (`BANK_RECONCILIATION:VALIDATE`), seulement avec un **écart nul** ; numéro `RAP-AAAAMM-000001` (pièce sans trou du lot F7) ; les pointages du compte sont verrouillés.
- **Verrou de période** : une période ne se verrouille pas tant que chaque compte suivi (au moins un relevé importé) n'a pas un rapprochement validé à sa date de fin (`accounting.bank.reconciliation-before-lock`, vrai par défaut).
- **Tableau de bord** `GET /bank-dashboard` : par compte, solde relevé, solde comptable, nombre de suspens des deux côtés, âge du plus vieux suspens, date du dernier relevé et du dernier rapprochement validé.

## Chèques et remises en banque

- Chaque transaction par chèque devient un chèque suivi (`bank_cheque` : numéro, banque, tireur, montant, caisse, session), `RECEIVED` puis `DEPOSITED`, `CLEARED` ou `REJECTED`.
- **Bordereau de remise** (`BRB-AAAAMM-000001`) : espèces et chèques d'une caisse vers un compte ; `PATCH /deposit-slips/{n}/deposit` (déposé) puis `/credit?statementLineId=` (crédité) : écriture D banque, C `513` (chèques), C `585` (espèces sorties de la caisse par la remise), pointée avec la ligne du relevé ; les chèques passent `CLEARED`.
- **Chèque rejeté** (`PATCH /cheques/{transaction}/reject?reason=&fees=&bankAccountCode=`) : la transaction de paiement est annulée (la facture redevient due, l'écriture inverse suit par le lot F4) ; les frais de rejet passent D `631` C banque.

## Devises des caisses (lot C5)

- **Taux du jour** (`cash_exchange_rate`) saisi par un responsable (`CASH_EXCHANGE_RATE:MANAGE`) : `POST /exchange-rates?currency=EUR&rate=655.957&validOn=`.
- **Solde par devise** de la session ouverte d'une caisse, avec sa contre-valeur en XAF : `GET /cash/registers/{code}/balances`. Le billetage par devise existait déjà (lot C2 : coupures et comptages portent leur devise).
- **Comptabilité** : un mouvement de caisse en devise est comptabilisé en XAF au taux porté par le mouvement, sinon au taux du jour ; le libellé de la pièce rappelle le montant en devise.
- Les écarts de change (`676`, `776`) à la remise d'espèces en devise restent à passer à la main au journal des opérations diverses.

## Routes

Préfixe `/api/v1/accounting`. Écritures auditées.

| Route | Droit |
| --- | --- |
| `GET /bank-accounts`, `/bank-import-templates`, `/bank-assignment-rules`, `/bank-accounts/{code}/statements`, `/matching`, `/reconciliations`, `/bank-dashboard`, `/cheques`, `/deposit-slips` | `ACCOUNTING_REPORT:READ` |
| `POST /bank-accounts`, `/bank-import-templates` (et `/preview`), `/bank-assignment-rules` | `BANK_ACCOUNT:MANAGE` |
| `POST /bank-accounts/{code}/statements` (fichier), `/statements/manual` | `BANK_STATEMENT:IMPORT` |
| `POST /bank-accounts/{code}/matching`, `/matching/auto`, `DELETE .../matching/{code}`, `POST .../statement-lines/{id}/post`, `POST .../reconciliations`, chèques et bordereaux | `BANK_RECONCILIATION:MANAGE` |
| `PATCH /reconciliations/{id}/validate` | `BANK_RECONCILIATION:VALIDATE` |
| `GET /exchange-rates`, `/cash/registers/{code}/balances` | `CASH:READ` |
| `POST /exchange-rates` | `CASH_EXCHANGE_RATE:MANAGE` |

Rôles : `ADMIN` et `SUPER_ADMIN` tout ; `FINANCE` tout sauf `BANK_RECONCILIATION:UNLOCK` ; `MANAGER` les taux de change.

## Paramètres (domaine Comptabilité, à chaud)

`accounting.bank.auto-match` (`true`), `accounting.bank.date-window-days` (`5`), `accounting.bank.max-combined-lines` (`10`), `accounting.bank.missing-statement` (`ALERT`), `accounting.bank.reconciliation-before-lock` (`true`).

## Pour le frontend

- **Comptes bancaires** : liste et fiche (banque, RIB, devise, compte, journal, modèle d'import, solde et date de début du suivi) ; éditeur de modèle d'import avec essai sur un fichier (`/preview`).
- **Import** : dépôt du fichier, soldes initial et final, compte rendu (lignes, doublons, pointées automatiquement, alertes de relevé manquant).
- **Écran de pointage** en deux colonnes (`GET /matching`) : lignes du relevé à gauche, écritures à droite, cases à cocher, totaux et différence, bouton « Pointer » actif quand la différence est nulle ; liste des propositions avec indice de confiance ; sur une ligne du relevé seule : « Comptabiliser » (règle proposée, compte, ou virement client avec numéro de facture).
- **Rapprochement** : préparer à une date, détail des suspens avec ancienneté, écart en rouge s'il n'est pas nul, validation par une autre personne, historique.
- **Chèques et bordereaux** : liste des chèques par état, préparation d'un bordereau (espèces et chèques cochés), dépôt, crédit (choix de la ligne du relevé), rejet avec frais.
- **Caisse en devises** : saisie du taux du jour, soldes par devise de la caisse.

## Tests

`BankF10IT` (schéma de production) :
- un relevé de 5 lignes est pointé automatiquement à 4 sur 5 (référence de bordereau, référence de virement, combinaison de deux chèques, montant et date) ;
- réimporter le même fichier signale 5 doublons ;
- la ligne de frais est comptabilisée par la règle et pointée ;
- le rapprochement a un écart nul, il est refusé au préparateur et validé par une autre personne (`RAP-`) ;
- le dépointage d'un pointage verrouillé est refusé ;
- le verrou de période voit le rapprochement manquant du mois suivant ;
- un virement client devient un paiement imputé à sa facture ;
- un relevé incohérent est refusé ;
- le taux du jour s'applique.

`AdminApiAuthorizationManagerTest` couvre les routes.
