# Lots F10 et C5 : banque, chèques, mobile money, rapprochement ; devises des caisses

Lots du [plan finance](../plan-finance-comptabilite/README.md), détaillés dans [rapprochement-et-comptes-tiers.md](../plan-finance-comptabilite/rapprochement-et-comptes-tiers.md), section 1, et [caisses-et-sessions.md](../plan-finance-comptabilite/caisses-et-sessions.md) (instruments et devises, lot C5). S'appuie sur F1 à F4 (pièces, moteur, caisse, encaissements).

Branche `feature/finance-comptabilite`. Migrations : dev `V304__banque_rapprochement_f10_c5.sql`, prod `V298__banque_rapprochement_f10_c5.sql` (contenu identique) ; compléments : dev `V305__banque_formats_change_f10.sql` / prod `V299__banque_formats_change_f10.sql` (OFX, MT940, écarts de change des caisses, détail figé du rapprochement) et dev `V306__multidevise_f10.sql` / prod `V300__multidevise_f10.sql` (gestion multidevise). Code : `BankService`, `ChequeService`, `ExchangeRateService`, `BankController` dans `features/accounting`.

## Comptes suivis

`bank_account` : code, type (`BANK`, `MOBILE_MONEY`, `CARD`), banque, agence, libellé, RIB, devise, compte comptable, journal, modèle d'import, solde et date de début du suivi. Livrés : `BANQUE-PRINCIPALE` (`5211`, `BQ-5211`), `PAWAPAY` (`5212`, `MM-5212`), `MM-MARCHANDS` (`5213`, `MM-5213`). Un nouveau compte sans numéro reçoit un compte sous `521` et son journal (`BQ-` ou `MM-`).

Ce qui précède la **date de début du suivi** est couvert par le **solde d'ouverture** : ni suspens, ni à pointer. Ce solde doit figurer en comptabilité (à-nouveaux) pour que le rapprochement tombe juste.

## Relevés

- **Modèle d'import** (`bank_import_template`, format CSV) : séparateur, lignes d'en-tête, colonnes (par nom d'en-tête ou numéro) de date, date de valeur, libellé, référence, montant signé ou débit et crédit, format de date, séparateur décimal, encodage. `POST /bank-import-templates/{code}/preview` lit un fichier sans rien enregistrer.
- **OFX et MT940** (complément) : le format est reconnu au contenu du fichier, sans modèle d'import. OFX (SGML ou XML) : chaque `STMTTRN` donne date, montant signé, `FITID` en référence, `NAME` et `MEMO` en libellé ; période (`DTSTART`, `DTEND`) et solde final (`LEDGERBAL`), solde initial déduit. MT940 : chaque `:61:` (date, sens `C`/`D`/`RC`/`RD`, montant, référence après `//` ou référence client) avec son `:86:` en libellé (sur plusieurs lignes) ; soldes `:60F:` et `:62F:`. Les soldes saisis à l'import l'emportent sur ceux du fichier. Le relevé porte la source `OFX` ou `MT940`. Un fichier qui n'est ni OFX ni MT940 passe par le modèle CSV du compte.
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
- **PDF de l'état** (complément) : `GET /reconciliations/{id}/pdf`. À la validation, le détail (suspens des deux côtés, soldes) est figé dans `bank_reconciliation.detail_json` : la réimpression d'un état validé montre ce qui a été validé, sous son numéro `RAP-` et avec la mention DUPLICATA à partir du deuxième tirage. Un brouillon s'imprime recalculé, marqué BROUILLON. Contenu : compte, banque, RIB, compte comptable ; tableau solde du relevé, encaissements et décaissements en suspens, lignes du relevé non comptabilisées, solde rapproché, solde comptable, écart ; liste des suspens avec ancienneté en jours ; signatures « Préparé par » et « Validé par ».
- **Tableau de bord** `GET /bank-dashboard` : par compte, solde relevé, solde comptable, nombre de suspens des deux côtés, âge du plus vieux suspens, date du dernier relevé et du dernier rapprochement validé.

## Chèques et remises en banque

- Chaque transaction par chèque devient un chèque suivi (`bank_cheque` : numéro, banque, tireur, montant, caisse, session), `RECEIVED` puis `DEPOSITED`, `CLEARED` ou `REJECTED`.
- **Bordereau de remise** (`BRB-AAAAMM-000001`) : espèces et chèques d'une caisse vers un compte ; `PATCH /deposit-slips/{n}/deposit` (déposé) puis `/credit?statementLineId=` (crédité) : écriture D banque, C `513` (chèques), C `585` (espèces sorties de la caisse par la remise), pointée avec la ligne du relevé ; les chèques passent `CLEARED`.
- **Chèque rejeté** (`PATCH /cheques/{transaction}/reject?reason=&fees=&bankAccountCode=`) : la transaction de paiement est annulée (la facture redevient due, l'écriture inverse suit par le lot F4) ; les frais de rejet passent D `631` C banque.

## Devises des caisses (lot C5)

- **Taux du jour** (`cash_exchange_rate`) saisi par un responsable (`CASH_EXCHANGE_RATE:MANAGE`) : `POST /exchange-rates?currency=EUR&rate=655.957&validOn=`.
- **Solde par devise** de la session ouverte d'une caisse, avec sa contre-valeur en XAF : `GET /cash/registers/{code}/balances`. Le billetage par devise existait déjà (lot C2 : coupures et comptages portent leur devise).
- **Comptabilité** : un mouvement de caisse en devise est comptabilisé en XAF au taux porté par le mouvement, sinon au taux du jour ; le libellé de la pièce rappelle le montant en devise.
- **Écarts de change des espèces en devise** (complément) : chaque entrée d'espèces en devise est valorisée en XAF et retenue (`cash_movement.accounting_base_amount`) ; une sortie (versement au coffre, à la banque, transfert) est valorisée au **coût moyen** des devises entrées dans la caisse. La différence avec la valeur au taux du jour est passée automatiquement en perte (`676`) ou gain (`776`) de change dans la même pièce (comptes : `accounting.cash.exchange-loss-account`, `accounting.cash.exchange-gain-account`). Exemple : 1 000 USD entrés à 650 puis versés à 660 donnent un gain de 10 000 XAF au crédit de `776`.

## Gestion multidevise (complément)

Demande de l'exploitant : gestion multidevise, récupération des taux sur internet, saisie manuelle possible.

- **Convention** : un taux dit combien vaut **1 unité de la devise en XAF** (ex. 1 USD = 600 XAF). La monnaie de tenue reste le XAF ; les montants en devise sont gardés à côté (`acc_entry_line.currency`, `foreign_amount`).
- **Source des taux** (`cash_exchange_rate.source`) : `MANUAL` (saisi), `AUTO` (récupéré sur internet), `PARITY` (parité fixe). `cash_exchange_rate.provider` garde le fournisseur.
- **Parité de l'euro** : 1 EUR = 655,957 XAF (`accounting.exchange.eur-parity`). Sans taux saisi, l'euro prend cette parité ; elle est aussi enregistrée à chaque récupération.
- **Récupération sur internet** : chaque jour à 7 h 15 (Brazzaville) (`accounting.exchange.fetch-cron`, désactivable par `accounting.exchange.auto-fetch`) et à la demande par `POST /exchange-rates/fetch`. Fournisseur par défaut : `https://api.frankfurter.dev/v1/latest?from=EUR` (taux de référence de la Banque centrale européenne, gratuit, sans clé ; `accounting.exchange.provider-url`). La réponse doit être en base EUR (`base`, `date`, `rates`) ; chaque taux est converti en XAF par la parité : `taux XAF = 655,957 / (devises pour 1 EUR)`. Seules les devises suivies (`accounting.currencies`, par défaut `EUR,USD,GBP,CHF,CNY,JPY,CAD,ZAR`) sont enregistrées, à la date des taux publiés (la BCE ne publie pas le week-end). En cas d'échec, les comptables sont notifiés et la saisie manuelle reste possible.
- **Saisie manuelle** : `POST /exchange-rates?currency=&rate=&validOn=` (inchangé). Un taux saisi pour un jour n'est **jamais remplacé** par la récupération automatique ; une saisie remplace un taux récupéré.
- **Tableau des taux** : `GET /exchange-rates/board?date=` donne, pour chaque devise suivie, le dernier taux connu à la date, sa date, sa source et son fournisseur (taux vide si aucun).
- **Conversion** : `GET /exchange-rates/convert?amount=&from=&to=XAF&date=` convertit entre deux devises quelconques en passant par le XAF.
- **Factures et avoirs en devise** : convertis en XAF au taux porté par le document (`billing_document.exchange_rate`), sinon au taux du jour d'émission ; chaque ligne, la remise, la TVA et les autres taxes sont converties et arrondies au franc, le total client est leur somme (la pièce reste équilibrée). La ligne du client (`411`) porte la devise et le total en devise.
- **Paiements en devise** : convertis au taux du jour du paiement ; chaque imputation sur une facture porte la devise et son montant en devise. Sans taux pour ce jour, le paiement n'est pas comptabilisé et l'alimentation signale « Aucun taux ... : saisissez-le ou récupérez les taux du jour » ; il passe au cycle suivant une fois le taux connu.
- **Portefeuilles en devise** : écritures converties au taux du jour.
- **Écart de change au règlement** : au lettrage automatique d'une facture en devise, si les montants **en devise** se soldent mais pas les montants en XAF, l'écart est passé en perte (`676`) ou gain (`776`) de change (`accounting.exchange.loss-account`, `accounting.exchange.gain-account`), sans seuil, et le compte client est soldé. Exemple : 118 USD facturés à 600 (70 800 XAF) et payés à 610 (71 980 XAF) : gain de change de 1 180 XAF.
- Hors de ce complément : la réévaluation des créances en devise à la clôture et les comptes bancaires tenus en devise (le pointage se fait sur les montants en XAF du relevé).

## Routes

Préfixe `/api/v1/accounting`. Écritures auditées.

| Route | Droit |
| --- | --- |
| `GET /bank-accounts`, `/bank-import-templates`, `/bank-assignment-rules`, `/bank-accounts/{code}/statements`, `/matching`, `/reconciliations`, `/bank-dashboard`, `/cheques`, `/deposit-slips` | `ACCOUNTING_REPORT:READ` |
| `POST /bank-accounts`, `/bank-import-templates` (et `/preview`), `/bank-assignment-rules` | `BANK_ACCOUNT:MANAGE` |
| `POST /bank-accounts/{code}/statements` (fichier), `/statements/manual` | `BANK_STATEMENT:IMPORT` |
| `POST /bank-accounts/{code}/matching`, `/matching/auto`, `DELETE .../matching/{code}`, `POST .../statement-lines/{id}/post`, `POST .../reconciliations`, chèques et bordereaux | `BANK_RECONCILIATION:MANAGE` |
| `PATCH /reconciliations/{id}/validate` | `BANK_RECONCILIATION:VALIDATE` |
| `GET /exchange-rates`, `/exchange-rates/board`, `/exchange-rates/convert`, `/cash/registers/{code}/balances` | `CASH:READ` |
| `POST /exchange-rates`, `/exchange-rates/fetch` | `CASH_EXCHANGE_RATE:MANAGE` |
| `GET /reconciliations/{id}/pdf` | `ACCOUNTING_REPORT:READ` |

Rôles : `ADMIN` et `SUPER_ADMIN` tout ; `FINANCE` tout sauf `BANK_RECONCILIATION:UNLOCK` ; `MANAGER` les taux de change.

## Paramètres (domaine Comptabilité, à chaud)

`accounting.bank.auto-match` (`true`), `accounting.bank.date-window-days` (`5`), `accounting.bank.max-combined-lines` (`10`), `accounting.bank.missing-statement` (`ALERT`), `accounting.bank.reconciliation-before-lock` (`true`).

Compléments : `accounting.cash.exchange-loss-account` (`676`), `accounting.cash.exchange-gain-account` (`776`), `accounting.currencies` (`EUR,USD,GBP,CHF,CNY,JPY,CAD,ZAR`), `accounting.exchange.auto-fetch` (`true`), `accounting.exchange.eur-parity` (`655.957`), `accounting.exchange.fetch-cron` (`0 15 7 * * *`, au redémarrage), `accounting.exchange.provider-url`, `accounting.exchange.loss-account` (`676`), `accounting.exchange.gain-account` (`776`).

## Pour le frontend

- **Comptes bancaires** : liste et fiche (banque, RIB, devise, compte, journal, modèle d'import, solde et date de début du suivi) ; éditeur de modèle d'import avec essai sur un fichier (`/preview`).
- **Import** : dépôt du fichier, soldes initial et final, compte rendu (lignes, doublons, pointées automatiquement, alertes de relevé manquant).
- **Écran de pointage** en deux colonnes (`GET /matching`) : lignes du relevé à gauche, écritures à droite, cases à cocher, totaux et différence, bouton « Pointer » actif quand la différence est nulle ; liste des propositions avec indice de confiance ; sur une ligne du relevé seule : « Comptabiliser » (règle proposée, compte, ou virement client avec numéro de facture).
- **Rapprochement** : préparer à une date, détail des suspens avec ancienneté, écart en rouge s'il n'est pas nul, validation par une autre personne, historique.
- **Chèques et bordereaux** : liste des chèques par état, préparation d'un bordereau (espèces et chèques cochés), dépôt, crédit (choix de la ligne du relevé), rejet avec frais.
- **Caisse en devises** : saisie du taux du jour, soldes par devise de la caisse.
- **Import** : le dépôt accepte aussi les fichiers OFX et MT940 (pas de modèle à choisir) ; le compte rendu indique le format reconnu.
- **Rapprochement** : bouton « Imprimer » (`GET /reconciliations/{id}/pdf`, PDF en ligne).
- **Taux de change** : écran avec le tableau des taux (`/exchange-rates/board`) montrant la source de chaque taux (Saisi, Internet, Parité fixe) et sa date ; bouton « Récupérer les taux du jour » (`POST /exchange-rates/fetch`, renvoie les taux enregistrés) ; saisie manuelle d'un taux (devise, taux, date) ; convertisseur (`/exchange-rates/convert`). Signaler qu'un taux saisi n'est pas écrasé par la récupération.
- **Factures en devise** : le taux du document (`exchangeRate`, 1 devise = x XAF) peut être saisi ; à défaut le taux du jour d'émission s'applique en comptabilité.

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
- le taux du jour s'applique ;
- l'import OFX, la réimpression du PDF validé (DUPLICATA) et le brouillon (compléments).

`BankFileParsersTest` : lecture OFX SGML (opérations, signes, références, soldes, période), MT940 (`:61:`, `:86:` sur plusieurs lignes, soldes), un CSV n'est ni l'un ni l'autre.

`CashAccountingF3IT.foreignCashExchangeGain` : 1 000 USD entrés à 650 puis versés au coffre à 660 donnent 10 000 XAF au crédit de `776`.

`SalesAccountingF4IT` (multidevise) :
- facture de 118 USD au taux du document (600) et paiement au taux du jour (610) : `411` porte USD et 118, gain de change de 1 180 XAF en `776`, compte client soldé ;
- paiement en devise sans taux : erreur lisible, puis comptabilisé une fois le taux saisi ;
- taux récupérés : USD à 655,957 / 1,1 = 596,324545, l'euro à la parité, un taux saisi à la main conservé ; conversion USD vers XAF et EUR vers USD.

`AdminApiAuthorizationManagerTest` couvre les routes.

## Évolutions apportées par les lots suivants

- **Lot F11** : la TVA des encaissements en régime des encaissements passe de `4438` à `4431` dans la pièce d'encaissement ([finance-f11-fiscalite](../finance-f11-fiscalite/README.md)).
- **Lot F17** : les emprunts sont rattachés à un compte bancaire suivi ; déblocage, échéances et remboursements anticipés sont écrits au journal de ce compte et se pointent comme les autres écritures ([finance-f17-financement-hors-bilan-paie](../finance-f17-financement-hors-bilan-paie/README.md)).
