# Lot F12 : clôture d'exercice et états financiers SYSCOHADA

Spécification : `docs/backend/plan-finance-comptabilite/README.md` (sections 4.2, 4.6 et 4.7), `etats-financiers.md` (sections 4, 6 et 11) et `rapprochement-et-comptes-tiers.md` (section 4). Branche `feature/finance-comptabilite`.

## Migrations

- Dev `V318__cloture_etats_financiers_f12.sql`, prod `V312__cloture_etats_financiers_f12.sql`, même contenu.
- Colonnes ajoutées :
  - `acc_fiscal_year` : `net_result`, `opening_entry_number`, `closing_derogation` ;
  - `acc_entry_line` : `carried_from_line_id` (ligne de l'exercice clos reprise par une ligne d'à-nouveau).
- Le lettrage accepte un nouveau type, `CARRY_FORWARD` (reprise en à-nouveau).
- Tables :
  - `acc_statement_template`, `acc_statement_line`, `acc_statement_mapping` : modèle d'état versionné, postes, correspondance des comptes vers les postes ;
  - `acc_closing_step` : liste de clôture par exercice ;
  - `acc_generated_report` : éditions définitives figées ;
  - `acc_note_text` : notes annexes rédigées ;
  - `balance_justification` : justificatifs de solde figés (préparé par, revu par) ;
  - `acc_confirmation_request` : confirmations de solde et leur suivi.
- Données livrées : bilan (`BILAN`), compte de résultat (`COMPTE_RESULTAT`) et tableau des flux (`TFT`) du SYSCOHADA révisé, avec leurs codes de rubrique (`AD` à `DZ`, `TA` à `XI`, `ZA` à `ZH`) et la correspondance des comptes.
- Droits : `ACCOUNTING_CLOSING:READ`, `MANAGE`, `FINALIZE`, `ACCOUNTING_STATEMENT:EDIT`, `BALANCE_JUSTIFICATION:PREPARE` et `REVIEW`.

| Rôle | Droits |
| --- | --- |
| `SUPER_ADMIN`, `ADMIN` | Tous |
| `FINANCE` | `READ`, `MANAGE`, `BALANCE_JUSTIFICATION:PREPARE` et `REVIEW` |
| `MANAGER`, `AUDITOR` | `READ` (et `ACCOUNTING_REPORT:READ` pour l'auditeur) |

`FINALIZE` (clore, déroger, éditer en définitif) revient au responsable financier : à donner à la personne concernée.

## États financiers

| État | Contenu |
| --- | --- |
| Bilan | Actif en brut, amortissements et dépréciations, net ; passif en net ; colonne N-1 |
| Compte de résultat | Postes et soldes de gestion : marge commerciale, chiffre d'affaires, valeur ajoutée, EBE, résultat d'exploitation, financier, des activités ordinaires, HAO, résultat net |
| Tableau des flux | CAFG, variations du besoin de financement, investissement, capitaux propres, capitaux étrangers, trésorerie d'ouverture et de clôture |

Règles de calcul :
- **Correspondance** : pour chaque compte, le préfixe le plus long l'emporte. Un sens (`DEBIT`, `CREDIT`) ne vaut que pour un solde de ce sens : une banque créditrice passe en trésorerie passif. Les comptes `28`, `29`, `39`, `49`, `59` vont en colonne amortissements et dépréciations.
- **Soldes anormaux** : les comptes de tiers (classe 4) sont classés tiers par tiers. Un client créditeur passe en « Clients, avances reçues », un fournisseur débiteur en « Fournisseurs, avances versées », sans écriture de reclassement.
- **Totaux** : formules de codes de postes (`XG+XH-RQ-RS`), stockées sur le poste.
- **Périmètre** : les comptes de bilan cumulent depuis le lendemain du dernier exercice clos (à-nouveaux compris), les comptes de gestion portent sur l'exercice. Le résultat de l'exercice entre au poste `CJ`. Tant qu'un exercice antérieur n'est pas clos, son résultat y entre aussi (en instance d'affectation).
- **Tableau des flux** : les mouvements excluent les à-nouveaux. La CAFG part du résultat net, ajoute les dotations et la valeur comptable des cessions, retire les reprises et les produits de cession. Les variations portent sur les valeurs brutes de l'actif circulant et sur le passif circulant, hors provisions à court terme et dividendes à payer.
- **Mention** : un état est `provisional` tant que l'exercice n'est pas clos. `firstYear` signale l'absence de colonne N-1.

Contrôles servis avec chaque état :
- comptes mouvementés sans poste (`unclassified`) ;
- total actif égal au total passif ;
- résultat porté au bilan égal au résultat net calculé poste par poste ;
- résultat net égal aux produits moins les charges ;
- trésorerie du tableau des flux égale à la trésorerie du bilan.

La correspondance se modifie par les routes `mappings` (droit `EDIT`). Les postes et leurs formules sont des données : une nouvelle version du modèle se charge par migration, sans développement.

## Liste de clôture

Chaque lecture recalcule les contrôles automatiques et enregistre leur résultat.

| Étape | Type | Obligatoire |
| --- | --- | --- |
| `TRIAL_BALANCE` balance équilibrée | Contrôle | Oui |
| `PENDING_ENTRIES` aucune pièce en attente ou en brouillard dans l'exercice | Contrôle | Oui |
| `UNBALANCED` compte d'attente d'équilibrage soldé, pièces acceptées déséquilibrées régularisées | Contrôle | Oui |
| `NUMBERING` numérotation continue et chaîne d'empreintes de chaque journal | Contrôle | Oui |
| `SUSPENSE_ACCOUNTS` comptes d'attente soldés (`471`, `585`) | Contrôle | Oui |
| `BANK_RECONCILIATION` rapprochements validés au dernier jour (lot F10) | Contrôle | Oui |
| `CASH_REVIEW` aucune session de caisse en revue d'écart | Contrôle | Oui |
| `FIXED_ASSETS` contrôle de clôture du lot F18 | Contrôle | Oui |
| `PROVISIONS` contrôle de clôture du lot F19 | Contrôle | Oui |
| `DEFERRALS` contrôle de clôture du lot F20 | Contrôle | Oui |
| `COLLECTIVES` comptes collectifs justifiés par leurs tiers | Contrôle | Oui |
| `PRIOR_RESULT` résultat précédent affecté (comptes `13` soldés) | Contrôle | Oui |
| `CLASS_NINE` balances analytique et hors bilan équilibrées | Contrôle | Oui |
| `UNCLASSIFIED` chaque compte mouvementé a son poste | Contrôle | Oui |
| `STATEMENTS` bilan équilibré, résultats concordants | Contrôle | Oui |
| `STAFF_ADVANCES` avances au personnel revues | À cocher | Oui |
| `CASH_COUNT` procès-verbal de caisse au dernier jour | À cocher | Oui |
| `STOCK_COUNT` inventaire des stocks | À cocher | Non |
| `CHEQUES` inventaire des chèques à encaisser | À cocher | Non |
| `NOTES` notes rédactionnelles validées | Contrôle | Non |

- Une étape cochée garde son auteur, sa date et son commentaire.
- Une étape en échec se corrige, ou se lève par dérogation motivée (droit `FINALIZE`, statut `WAIVED`).
- Les seuls constats bloquants des lots F18 à F20 comptent, pas leurs avertissements.

## Clôture et à-nouveaux

`POST /accounting/closing/{year}/close` :
1. refuse si un exercice antérieur n'est pas clos, ou si une étape obligatoire n'est ni faite ni levée ;
2. crée l'exercice suivant s'il n'existe pas (douze périodes mensuelles) ;
3. passe les à-nouveaux dans le journal `AN`, au premier jour de l'exercice suivant, en une pièce validée ;
4. reprend les engagements hors bilan (comptes `90`) dans le journal `HB` ;
5. clôt les périodes de l'exercice dans l'ordre, puis l'exercice ; enregistre le résultat et le numéro de la pièce d'à-nouveaux.

Contenu des à-nouveaux :
- **Comptes lettrables** (`411`, `401`...) : une ligne par reste non lettré au dernier jour, avec sa référence, son échéance, son tiers et sa devise. Un écart résiduel avec le solde du tiers fait une ligne à part.
- **Autres comptes de bilan** : leur solde, par tiers.
- **Résultat** : au crédit du `131` (bénéfice) ou au débit du `139` (perte).
- **Analytique** (`91` à `99`) : non reprise, elle repart de zéro.

Suite du lettrage :
- chaque ligne reprise de l'exercice clos est soldée par un lettrage `CARRY_FORWARD` ;
- un lettrage déjà fait avec une ligne de l'exercice suivant (un règlement de janvier lettré avant la clôture) passe sur la ligne d'à-nouveau ;
- un lettrage de reprise ne se défait pas : on délettre la ligne d'à-nouveau.

`GET /accounting/closing/{year}/opening-preview` simule les à-nouveaux sans écrire.

## Justification des soldes

**Justificatif d'un tiers** (ou d'un compte) à une date :
- pièces ouvertes : lignes datées au plus tard ce jour, moins ce que lettrent les lettrages actifs dont toutes les lignes sont datées au plus tard ce jour. Une facture réglée en décembre et lettrée en janvier n'est donc pas ouverte au 31 décembre ;
- pour chaque pièce : date, référence, échéance, jours de retard, montant signé ;
- total des pièces ouvertes, solde comptable, écart. Un écart non nul signale un lettrage incomplet ou faux.

Un justificatif se fige (`POST`) avec son préparateur, puis se revoit (`review`) par une autre personne.

**Compte collectif** (`411`, `401`, `421`, `4191`, `422`, `462`, `165`, paramètre `accounting.closing.collective-accounts`) :
- solde par tiers, somme des tiers, montant porté sans tiers ;
- écart de justification de chaque tiers (comptes lettrables) ;
- soldes anormaux à part : client créditeur, fournisseur débiteur.

Un montant sans tiers ou un écart de justification bloque la clôture (étape `COLLECTIVES`).

**Dossier de justification des soldes de clôture** : chaque compte de bilan non soldé, son justificatif et son état.

| Comptes | Justificatif | État |
| --- | --- | --- |
| `52`, `53`, `55` | Rapprochement bancaire validé au dernier jour | Justifié, en écart (compte suivi sans rapprochement), à revoir (compte non suivi) |
| `57` | PV de billetage | À revoir |
| Collectifs | Justification par tiers | Justifié ou en écart |
| `471`, `585` | Compte d'attente | En écart tant qu'il n'est pas soldé |
| `513` | Chèques en portefeuille | Justifié si le total égale le solde |
| `476`, `477` | Échéanciers du lot F20 | Justifié ou en écart |
| `2` | Fichier des immobilisations (lot F18) | Justifié ou en écart |
| `16`, `17` | Tableau d'amortissement | À revoir |
| `44` | Dernière déclaration | À revoir |
| Autres | Pièce à joindre | À revoir |

Un compte à revoir passe à justifié quand un justificatif figé à la date de clôture a été revu. Le dossier s'exporte en PDF ou en CSV.

## Confirmations de solde

- **Sélection** : les tiers désignés, ou les 20 plus gros soldes du collectif et tout solde au-delà de 1 000 000 (paramètres `accounting.confirmation.top-count` et `threshold`). Un tiers déjà circularisé à la même date n'est pas repris.
- **Lettre** (PDF) : solde dans nos livres, pièces ouvertes, coupon-réponse, numéro `CONF-{année}-{rang}` à rappeler.
- **Suivi** : préparée (`PREPARED`), envoyée (`SENT`), relancée (`REMINDED`), conforme (`AGREED`) ou en écart (`DISPUTED`), puis rapprochée (`RECONCILED`, droit `REVIEW`, avec l'explication de l'écart).
- La réponse note son canal (courrier, portail...), le solde confirmé et l'écart.

## Éditions définitives

- Seulement sur un exercice clos, contrôles de cohérence au vert, ou avec un motif de dérogation.
- Données figées (JSON), PDF, empreinte SHA-256, numéro `EF-{exercice}-{état}-{rang}`.
- Le PDF se réimprime à l'identique. La route `verify` recalcule l'empreinte.

## Notes annexes

Catalogue du SYSCOHADA révisé (notes 1 à 35 utiles, plus événements postérieurs `EP`, parties liées `PL` et engagements hors bilan `HB`) :
- **notes chiffrées** : comptes repris de la balance, montant N et N-1. Les notes 3A à 3D et 28 renvoient aussi aux états détaillés des lots F18 et F19 (`source`) ;
- **notes rédigées** : texte saisi, repris de N-1 à défaut, puis validé ;
- **« Néant »** : une note sans montant ni texte est marquée sans objet (`notApplicable`).

## Routes (`/sni/api/v1`)

| Route | Usage |
| --- | --- |
| `GET /accounting/statements/balance-sheet?date=&format=json\|csv\|pdf` | Bilan |
| `GET /accounting/statements/income-statement?from=&to=&format=` | Compte de résultat |
| `GET /accounting/statements/cash-flow?year=&format=` | Tableau des flux |
| `GET`, `POST /accounting/statements/{BILAN\|COMPTE_RESULTAT}/mappings`, `DELETE /accounting/statements/mappings/{id}` | Correspondance des comptes |
| `GET /accounting/closing/{year}` | Liste de clôture |
| `POST /accounting/closing/{year}/steps/{step}` `{done, comment}` | Cocher ou décocher une étape manuelle |
| `POST /accounting/closing/{year}/steps/{step}/waive` `{reason}` | Lever une étape |
| `GET /accounting/closing/{year}/opening-preview` | Simulation des à-nouveaux |
| `POST /accounting/closing/{year}/close` `{derogation}` | Clôture |
| `POST`, `GET /accounting/closing/{year}/editions` `{report, derogation}` | Éditions définitives (`BILAN`, `COMPTE_RESULTAT`, `TFT`) |
| `GET /accounting/closing/editions/{number}/pdf`, `/verify` | PDF figé, contrôle d'empreinte |
| `GET /accounting/closing/{year}/notes`, `PUT /notes/{note}`, `POST /notes/{note}/validate?validate=` | Notes annexes |
| `GET /accounting/accounts/{number}/balance-justification?thirdParty=&date=` | Justificatif de solde |
| `POST /accounting/accounts/{number}/balance-justification` `{thirdPartyCode, date, comment}` | Figer un justificatif |
| `PATCH /accounting/balance-justifications/{id}/review` | Revue d'un justificatif |
| `GET /accounting/accounts/{number}/collective?date=` | Justification d'un compte collectif |
| `GET /accounting/closing/{year}/justification-file?format=json\|csv\|pdf` | Dossier de justification des soldes |
| `POST /accounting/confirmations` `{accountNumber, date, top, threshold, thirdParties}` | Préparer des confirmations |
| `GET /accounting/confirmations` | Suivi des confirmations |
| `POST /accounting/confirmations/{number}/send?reminder=` | Envoi ou relance |
| `POST /accounting/confirmations/{number}/reply` `{channel, confirmedBalance, detail}` | Réponse du tiers |
| `POST /accounting/confirmations/{number}/reconcile` `{comment}` | Rapprochement d'un écart |
| `GET /accounting/confirmations/{number}/letter?format=pdf\|json` | Lettre de confirmation |

## Paramètres (`ACCOUNTING`, lus à chaud)

| Clé | Défaut |
| --- | --- |
| `accounting.closing.opening-journal` | `AN` |
| `accounting.closing.off-balance-journal` | `HB` |
| `accounting.closing.profit-account`, `loss-account` | `131`, `139` |
| `accounting.closing.suspense-accounts` | `471,585` |
| `accounting.closing.collective-accounts` | `411,401,421,4191,422,462,165` |
| `accounting.confirmation.top-count`, `threshold` | `20`, `1000000` |

## Non fait dans ce lot

- **Balance avant et après inventaire** : la balance générale existe (lot F1). La liste des écritures d'inventaire n'est pas encore isolée.
- **Synchronisation avec la clôture de facturation** : clore une période de facturation ne propose pas encore de verrouiller la période comptable.
- **Codes de flux** sur les natures d'opération : le tableau des flux se calcule à partir des comptes, sans code porté par les schémas.
- **Système minimal de trésorerie** : non retenu (décision 1). Un modèle `SMT` peut se charger par migration.
- **Notes chiffrées** : elles listent les comptes et leurs soldes. Les tableaux de mouvements propres à chaque note (entrées, sorties) restent dans les états des lots F18 et F19.
- **Confirmations par courriel et réponse depuis le portail** : la lettre se télécharge et s'imprime, l'envoi est noté à la main. L'envoi automatique et le coupon en ligne restent à faire.
- **Comparaison ligne à ligne** avec l'état envoyé par le tiers : l'écart se rapproche avec un commentaire.

## Tests

`ClosingF12IT`, sur le schéma de production, avec un exercice 2020 isolé :
- bilan provisoire de 11 390 000 à l'actif et au passif, résultat de 720 000 ; l'avance d'un client (client créditeur de 100 000) passe au passif ;
- compte de résultat : chiffre d'affaires 1 500 000, EBE 1 200 000 ;
- tableau des flux : trésorerie de clôture 8 880 000, égale au bilan ;
- justificatif du client : la facture B ouverte au 31/12, écart nul ; collectif `411` justifié avec un solde anormal ; revue refusée à son préparateur ;
- confirmations : deux tiers, l'un conforme, l'autre en écart de 10 000 puis rapproché ;
- liste de clôture : seules les deux étapes à cocher manquent, la clôture est refusée avant ;
- clôture : à-nouveaux égaux aux soldes, résultat en `131`, facture client reprise avec sa référence ;
- suite du lettrage : le règlement de janvier 2021 est lettré avec la ligne d'à-nouveau, la facture fournisseur reste ouverte en 2021 ;
- bilan de janvier 2021 avec sa colonne N-1 ;
- édition définitive figée et vérifiée.

## Pour le frontend

Écrans à prévoir :
- bilan, compte de résultat, tableau des flux, avec la colonne N-1, la mention « PROVISOIRE », les contrôles et les comptes non classés ;
- correspondance des comptes vers les postes ;
- liste de clôture : contrôles, étapes à cocher, dérogations ;
- justificatifs de solde, comptes collectifs, dossier de justification ;
- confirmations de solde : sélection, lettre, suivi des réponses ;
- simulation des à-nouveaux, puis clôture ;
- éditions définitives et leur PDF ;
- notes annexes : montants, éditeur de texte, validation.
