# Lot F15 : liasse fiscale et états de gestion

Spécification : `docs/backend/plan-finance-comptabilite/etats-financiers.md` (sections 5, 8 et 9). Branche `feature/finance-comptabilite`.

## Migrations

- Dev `V322__liasse_etats_gestion_f15.sql`, prod `V316__liasse_etats_gestion_f15.sql`, même contenu.
- `acc_account.non_deductible_rate` : part non déductible fiscalement des charges d'un compte, en pourcentage.
- Tables :
  - `acc_tax_adjustment` : réintégrations et déductions saisies ;
  - `acc_tax_result` : résultat fiscal arrêté par exercice, avec le report des déficits et l'impôt ;
  - `acc_liasse_sheet` : modèle de liasse versionné, avec la liste ordonnée des feuillets.
- Droits : ceux du lot F12 (`ACCOUNTING_CLOSING:READ`, `MANAGE`, `FINALIZE`). Les états de gestion acceptent aussi `ACCOUNTING_REPORT:READ`.

## Passage au résultat fiscal

Le tableau se recalcule à chaque lecture. Résultat comptable, plus les réintégrations, moins les déductions, donne le résultat avant imputation des déficits.

Lignes proposées par le logiciel :
- impôt sur le résultat comptabilisé (comptes `89`), réintégré ;
- charges des comptes marqués non déductibles, au taux du compte (`PATCH /accounting/accounts/{number}/non-deductible`) ;
- provisions et dépréciations non déductibles du lot F19 : dotations réintégrées, reprises déduites ;
- intérêts des comptes courants d'associés au-delà du taux admis (lot F17).

Les lignes saisies par le comptable (`REINTEGRATION` ou `DEDUCTION`) s'y ajoutent.

Déficits et impôt :
- les déficits reportables viennent du résultat fiscal arrêté de l'exercice précédent (`save`), à défaut du paramètre d'ouverture. Ils s'imputent sur le bénéfice, le reste se reporte ;
- impôt : taux sur le résultat fiscal positif, au moins le minimum de perception sur le chiffre d'affaires. Défauts 28 % et 1 % : **taux à faire valider par l'expert-comptable** ;
- `post` comptabilise la part non encore passée de l'impôt dû (débit `891`, crédit `441`) au dernier jour de l'exercice. Le résultat fiscal n'en change pas, puisque l'impôt comptabilisé est réintégré.

## États de gestion

| État | Contenu |
| --- | --- |
| Soldes intermédiaires de gestion | Marge commerciale, chiffre d'affaires, valeur ajoutée, EBE, résultat d'exploitation, financier, des activités ordinaires, HAO, net : par mois, en cumul et pour le même mois de N-1 |
| Tableau de bord | Chiffre d'affaires du mois et cumulé (et N-1), valeur ajoutée, EBE, résultat, trésorerie nette, créances clients et créances échues, délai moyen de paiement des clients (encours rapporté aux facturations des 90 derniers jours), avances au personnel, dépenses du mois par nature |
| Ratios | Liquidité générale, réduite, immédiate ; autonomie financière ; endettement ; délais clients et fournisseurs ; poids des charges de personnel ; taux de marge brute d'exploitation ; rentabilité nette et financière. Chaque ratio porte sa formule ; vide si le dénominateur est nul |
| Prévisionnel de trésorerie | 13 semaines ou 12 mois, depuis la trésorerie nette du jour |

Contenu du prévisionnel :
- **encaissements** : pièces clients ouvertes à leur échéance (à défaut, date de la pièce plus 30 jours ; les échues en première période), abonnements actifs à chaque échéance de leur cycle ;
- **décaissements** : pièces fournisseurs ouvertes, échéances d'emprunt prévues, personnel et charges courantes à leur moyenne mensuelle des 3 derniers mois, en fin de mois ;
- les hypothèses sont servies avec l'état.

Le budget comparé au réalisé existe déjà (lot F16, `BudgetReportService`). Les confirmations de solde sont au lot F12.

## Liasse fiscale

Feuillets du modèle (version 1), dans l'ordre :

| Feuillet | Contenu |
| --- | --- |
| `IDENTIFICATION` | Fiche de l'exploitant (dénomination, forme, NIU, RCCM, adresse, contacts), activité, centre des impôts, régime, exercice et durée, exercice précédent, référentiel |
| `DIRIGEANTS` | Représentant légal ; commissaire aux comptes et expert-comptable s'ils sont paramétrés |
| `EFFECTIFS` | Effectif salarié (fiches de paie actives), charges de personnel, personnel extérieur |
| `BILAN`, `COMPTE_RESULTAT`, `TFT` | États du lot F12, avec N-1 |
| `NOTES` | Notes annexes : montants N et N-1, texte, ou « Néant » |
| `RESULTAT_FISCAL` | Tableau de passage au résultat fiscal |

- Formats : JSON, PDF, Excel. L'Excel est un classeur au format XML lu par Excel et LibreOffice, un onglet par feuillet, sans dépendance ajoutée.
- `PROVISOIRE` tant que l'exercice n'est pas clos.
- Édition définitive (`finalize`) : exercice clos seulement ; données, PDF et empreinte SHA-256 figés dans `acc_generated_report` (`EF-{exercice}-LIASSE-{rang}`).
- Le modèle de liasse est une donnée. Une variante nationale ou une mise à jour de l'administration se charge par migration (nouvelle version de `acc_liasse_sheet`).

## Routes (`/sni/api/v1`)

| Route | Usage |
| --- | --- |
| `GET /accounting/management/sig?year=&format=json\|csv\|pdf` | Soldes intermédiaires de gestion mensuels |
| `GET /accounting/management/dashboard?date=` | Tableau de bord financier |
| `GET /accounting/management/ratios?date=` | Ratios |
| `GET /accounting/management/cash-forecast?date=&horizon=WEEKS\|MONTHS&format=` | Prévisionnel de trésorerie |
| `GET /accounting/tax-result/{year}?format=` | Tableau de passage au résultat fiscal |
| `POST /accounting/tax-result/{year}/adjustments` `{kind, code, label, accountNumber, amount, comment}` | Ajouter une réintégration ou une déduction |
| `DELETE /accounting/tax-result/adjustments/{id}` | Supprimer une ligne saisie |
| `POST /accounting/tax-result/{year}/save` | Arrêter le résultat fiscal (base du report des déficits) |
| `POST /accounting/tax-result/{year}/post` | Comptabiliser l'impôt dû |
| `PATCH /accounting/accounts/{number}/non-deductible` `{rate}` | Part non déductible d'un compte |
| `GET /accounting/liasse/{year}?format=json\|pdf\|xls` | Liasse |
| `POST /accounting/liasse/{year}/finalize` | Liasse définitive |

## Paramètres (`ACCOUNTING`, lus à chaud)

| Clé | Défaut |
| --- | --- |
| `accounting.tax.corporate-rate`, `minimum-rate` | `28`, `1` (à valider par l'expert-comptable) |
| `accounting.tax.opening-loss` | `0` |
| `accounting.tax.journal`, `expense-account`, `payable-account` | `OD`, `891`, `441` |
| `accounting.liasse.activity`, `tax-office`, `tax-regime`, `auditor`, `accountant` | Fiche de la liasse |
| `accounting.forecast.payment-term-days`, `history-months` | `30`, `3` |

## Non fait dans ce lot

- **Décision (13/11/2026)** : la liasse suit le modèle commun SYSCOHADA seulement (identification, états, notes, tableau fiscal), sans mise en page nationale case par case.
- **Taux d'impôt** : défauts à confirmer (taux, minimum de perception, base du minimum, plafond de report des déficits).
- **Télédéclaration** : l'export Excel sert à la saisie ou à l'import ; aucun envoi automatique.

## Tests

`ProvisionF19IT` : le paiement du jour est daté d'une minute avant maintenant (à 10 h, il était futur la nuit et le test échouait selon l'heure).

`LiasseF15IT`, sur un exercice 2022 isolé :
- ventes 2 000 000, loyer 500 000 dont 20 % non déductibles, déduction saisie de 50 000 : résultat comptable 1 500 000, résultat fiscal 1 550 000, impôt 434 000, minimum 20 000 ;
- après comptabilisation de l'impôt : résultat comptable 1 066 000, résultat fiscal inchangé, nouvelle comptabilisation refusée ;
- soldes de gestion : 12 mois, chiffre d'affaires de mars, résultat cumulé de décembre ;
- tableau de bord, 11 ratios dont la rentabilité nette de 53,30 % ;
- prévisionnel cohérent (trésorerie de fin égale au départ plus les soldes), à 13 semaines et à 12 mois ;
- liasse : 8 feuillets dans l'ordre, fiche d'identification, résultat fiscal (1 450 000 une fois le taux non déductible retiré), export Excel, édition définitive refusée sur un exercice ouvert.

## Pour le frontend

Écrans à prévoir :
- tableau de passage au résultat fiscal : lignes proposées et saisies, déficits, impôt, arrêté, comptabilisation ;
- comptes non déductibles (dans la fiche du compte) ;
- soldes de gestion mensuels (tableau et graphique), tableau de bord, ratios ;
- prévisionnel de trésorerie, en semaines ou en mois, avec ses hypothèses ;
- liasse : aperçu par feuillet, téléchargement PDF et Excel, édition définitive.
