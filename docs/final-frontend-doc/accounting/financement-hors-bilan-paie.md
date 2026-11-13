# Financement, engagements hors bilan et paie

Contrat exact de chaque route (paramètres, corps, réponse, champs JSON) : [routes.md](routes.md). Ce fichier explique le fonctionnement et ce qu'il faut à l'écran. Index du module : [README.md](README.md).

Critères d'acceptation du plan, vérifiés par `FinancingF17IT` : **une échéance d'emprunt s'écrit seule** ; **un journal de paie importé s'équilibre et solde les avances retenues**.

## 1. Emprunts

### Fiche

`acc_loan` : code (`EMP-n` par défaut), prêteur (`BANK`, `PARTNER`, `OTHER`, nom, tiers), objet, montant, devise, date de déblocage, première échéance, taux annuel (fixe ou variable), base de calcul (`30_360` ou `ACT_365`), périodicité en mois, nombre d'échéances, différé (`NONE`, `CAPITAL`, `TOTAL`, et son nombre d'échéances), mode (`ANNUITY` échéances constantes, `CONSTANT_CAPITAL` amortissement constant, `BULLET` in fine), frais de dossier, assurance par échéance, comptes (emprunt `162`, intérêts `671`, assurance `625`, frais `631`, intérêts courus `166`), compte bancaire suivi (F10) de versement et de prélèvement, section analytique et poste budgétaire des intérêts, mode d'écriture des échéances (`AT_DATE` à la date, `AT_STATEMENT` à la demande, au relevé).

Un emprunt auprès d'un associé (`PARTNER`) ou porté sur un compte `46x` garde le tiers sur ses lignes.

### Tableau d'amortissement

- Calculé à la création (`LoanSchedule`), arrondi au franc : date, capital restant dû, intérêts, capital remboursé, assurance, échéance totale, capital restant après.
- Échéance constante : B × r / (1 − (1 + r)^−n) au taux périodique nominal ; la dernière échéance solde le capital.
- Différé en capital : seuls les intérêts sont payés ; différé total : les intérêts s'ajoutent au capital (ligne de capital négative).
- **Versions** (`acc_loan_installment.version`) : chaque recalcul crée une nouvelle version qui recopie les échéances déjà écrites puis recalcule la suite ; **l'ancien tableau est conservé** (`GET /loans/{code}/history`).
- Recalculs :
  - **remboursement anticipé** partiel ou total, après la dernière échéance écrite : D `162` ; D `671` (indemnité) ; C banque ; puis tableau à durée réduite (`REDUCE_TERM`, même échéance) ou à échéance réduite (`REDUCE_PAYMENT`, même durée) ; un remboursement total clôt l'emprunt ;
  - **changement de taux** à partir de la prochaine échéance ;
  - **report d'échéance** d'une période, les intérêts de la période reportée s'ajoutant au capital.
- Imprimable (`/pdf`) et exportable (`/export`, CSV).

### Écritures

| Événement | Écriture |
| --- | --- |
| Déblocage (`/disburse`) | D banque (net des frais) ; D `631` frais ; C `162` |
| Échéance | D `162` capital ; D `671` intérêts ; D `625` assurance ; C banque (journal du compte bancaire) |
| Échéance en différé total | D `671` ; C `162` (intérêts capitalisés) ; C banque pour l'assurance |
| Intérêts courus (`/loans/accrue?date=`) | D `671` ; C `166`, au prorata des jours écoulés de l'échéance en cours ; **extourne** datée du lendemain. Une seule fois par date. |

- **Échéances écrites seules** : chaque matin (`FinancingWorker`, `accounting.financing.cron`, 6 h 45), les échéances arrivées des emprunts actifs en mode `AT_DATE` sont écrites. En mode `AT_STATEMENT`, l'échéance s'écrit à la demande (`POST /loans/{code}/installments/{n}/post`), dans l'ordre.
- Le dernier règlement clôt l'emprunt (`CLOSED`).
- Le reclassement de la part à moins d'un an n'est pas une écriture : il est donné par la note annexe.

### Suivi

- **Échéancier** de tous les emprunts sur une période (`GET /loans/upcoming?from=&to=`), pour le prévisionnel de trésorerie (lot F15).
- **Alertes** aux comptables `accounting.loan.alert-days` jours (7) avant chaque échéance.
- **Note annexe des dettes financières** (`GET /loans/note?date=`) : par emprunt, capital restant dû, part à moins d'un an (capital des échéances des douze mois), part à plus d'un an, taux, dernière échéance, garanties données (engagements hors bilan liés `linkedType = LOAN`).
- Hors de ce lot : rapprochement avec un tableau importé de la banque.

## 2. Comptes courants d'associés

- `acc_partner_account` : associé, tiers comptable, compte (`462` par défaut, `accounting.partner.account`, ou un divisionnaire de `16` si l'expert-comptable le retient pour des comptes bloqués), convention : taux de rémunération, taux admis fiscalement, plafond, blocage (et date de fin).
- Mouvements (`POST /partners/{code}/movements`), écrits aussitôt :
  - `CONTRIBUTION` apport : D trésorerie ; C `462` ; refusé au-delà du plafond ;
  - `REPAYMENT` remboursement : D `462` ; C trésorerie ; refusé si le compte est bloqué ou au-delà du solde ;
  - `EXPENSE` dépense payée par l'associé pour la société : D charge (et TVA récupérable) ; C `462` ;
  - `DIVIDEND` affectation de dividendes : D `465` « Associés, dividendes à payer » (créé par la migration) ; C `462`.
  Le journal est celui du compte bancaire de la contrepartie, sinon `accounting.partner.journal` (`OD`).
- **Intérêts** (`POST /partners/{code}/interest?from=&to=`) : somme des soldes créditeurs de chaque jour × taux / 365 ; D `671` ; C `462` ; **part non déductible** au-delà du taux admis : intérêts × (taux − taux admis) / taux, gardée sur `acc_partner_interest` pour la liasse (F15). Une fois par date de fin.
- **Relevé** (`GET /partners/{code}/statement?from=&to=`) : solde d'ouverture, mouvements avec solde courant, solde de clôture.
- **Note annexe** (`GET /partners/note?date=`) : solde, taux, blocage, intérêts et part non déductible de l'exercice, par associé.

## 3. Registre des engagements hors bilan

- `acc_commitment_type` : types paramétrables avec leur sens par défaut ; livrés : caution donnée, caution reçue, garantie bancaire (reçue), nantissement, hypothèque, crédit-bail, bail des locaux, aval, engagement d'achat. Un type « bail » calcule l'engagement à partir du loyer mensuel.
- `acc_commitment` : type, sens (`GIVEN`, `RECEIVED`), tiers, description, montant, loyer mensuel (baux), devise, dates de début, de fin, de renouvellement et de préavis, opération liée (`linkedType` et `linkedCode` : emprunt, contrat, marché), références de documents (GED), statut (`ACTIVE`, `RELEASED`, `EXPIRED`), montant inscrit en classe 90, dernière pièce.
- **Tenue en classe 90, journal `HB`** (`accounting.commitment.*`) :
  - engagement donné : D `901` ; C `902` ;
  - engagement reçu : D `906` ; C `905` ;
  - chaque modification écrit la **différence** avec le montant déjà inscrit ; la **levée** (`/release`, avec date et pièce) et l'**expiration** (fin dépassée) contre-passent le solde inscrit.
- **Baux** : engagement = loyer mensuel × mois entamés jusqu'à la fin, **recalculé chaque jour** ; la variation s'écrit dès que le montant change.
- **Rapprochement** (`GET /commitments/reconciliation`) : total inscrit des engagements actifs, par sens, face aux soldes des comptes 90 ; vérifié chaque jour, les comptables sont alertés d'un écart.
- **Alertes** : échéance, date de préavis et date de renouvellement, à `accounting.commitment.alert-days` jours (30), puis à 7 et à 1 jour.
- **Note annexe** (`GET /commitments/note?date=`) : par sens et par type, nombre, total, à moins d'un an, de un à cinq ans, au-delà (baux : par les loyers de chaque tranche ; autres : selon la date de fin).
- Les dépôts de garantie reçus des membres (`165`) restent au bilan ; une garantie bancaire remise par un client à la place d'un dépôt s'inscrit ici comme engagement reçu.

## 4. Import du journal de paie

La paie reste calculée hors de la plateforme ; son journal mensuel est importé et écrit au journal `PA`.

- **Modèle d'import** (`acc_payroll_template`) : séparateur, lignes d'en-tête, colonnes du matricule, du nom, de la rubrique, du montant et de la section (par nom d'en-tête ou numéro), séparateur décimal, encodage. Livré : `STANDARD` (`Matricule;Nom;Rubrique;Montant;Section`). Un modèle par fournisseur de paie.
- **Rubriques** (`acc_payroll_rubric`) et leur nature :
  - `GROSS` rémunération : D compte (`661`), par section ;
  - `EMPLOYER_CHARGE` charge patronale : D compte de charge (`664`), par section ; C organisme (`431`) ;
  - `DEDUCTION` retenue salariale : C organisme ou État (`431`, `447`) ;
  - `ADVANCE` avance retenue : C `421` au nom du salarié ;
  - `NET` net à payer : C `422` au nom du salarié ;
  - `INFO` ligne ignorée (totaux du fichier).
  Livrées : salaire de base, primes, heures supplémentaires, avantages, CNSS salariale et patronale, IRPP, avances, net à payer, brut (ignoré). Les codes du fichier sont normalisés (majuscules, soulignés).
- **Salariés** (`acc_payroll_employee`) : matricule, nom, tiers comptable (l'adresse du salarié, celle de ses avances en `421`), section par défaut.
- **Contrôles à l'import** (erreurs bloquantes, avertissements affichés) :
  - rubrique sans correspondance : erreur ;
  - par salarié, **brut = retenues + avances + net** : erreur sinon ;
  - avance retenue supérieure aux avances ouvertes du salarié en `421` : erreur ;
  - salarié inconnu (le matricule sert de tiers) ou sans section : avertissement (erreur si `accounting.payroll.section-required`).
- **Réimport** : remplace le brouillon du mois (statut `REPLACED`) ; refusé si le mois est déjà comptabilisé.
- **Comptabilisation** (`/post`) : pièce au journal `PA` datée du dernier jour du mois, lignes agrégées par compte, tiers et section ; puis **lettrage des avances retenues** : chaque retenue est lettrée avec les avances ouvertes du salarié, les plus anciennes d'abord. Les avances retenues sont ainsi soldées.
- **Paiement des salaires** (`/pay`) : D `422` par salarié ; C trésorerie choisie (compte et journal), lettré avec le net dû ; tous les salariés du mois ou une liste de matricules. Les versements CNSS et impôts passent par la banque (F10) : D `431` / `447` ; C banque. Le paiement en espèces avec état d'émargement passe par la caisse.

## Routes

Préfixe `/api/v1/accounting`. Écritures auditées.

| Route | Droit |
| --- | --- |
| `GET /loans`, `/loans/{code}`, `/history`, `/export`, `/pdf`, `/loans/note`, `/loans/upcoming`, `GET /partners...`, `GET /commitments...`, `/commitment-types` | `FINANCING:READ` |
| `POST /loans`, `/loans/{code}/disburse`, `/installments/{n}/post`, `/prepay`, `/rate?rate=&date=`, `/postpone`, `/loans/accrue?date=`, `POST /partners`, `/partners/{code}/movements`, `/interest`, `POST /commitments`, `/commitments/{code}/release`, `/commitment-types` | `FINANCING:MANAGE` |
| `GET /payroll/...`, `POST /payroll/imports` (fichier, `period`, `template`) | `PAYROLL:IMPORT` |
| `POST /payroll/imports/{id}/post`, `/pay` | `PAYROLL:POST` |
| `POST /payroll/templates`, `/rubrics`, `/employees` | `PAYROLL_SETTINGS:MANAGE` |

Rôles : `SUPER_ADMIN`, `ADMIN` et `FINANCE` tout ; `MANAGER` la lecture du financement.

## Paramètres (domaine Comptabilité)

`accounting.financing.cron` (`0 45 6 * * *`, au redémarrage), `accounting.financing.analytic-axis` (vide : les sections des emprunts et de la paie ne sont pas ventilées ; sinon l'axe qui les reçoit), `accounting.loan.alert-days` (`7`), `accounting.loan.accrual-journal` (`OD`), `accounting.partner.account` (`462`), `accounting.partner.dividend-account` (`465`), `accounting.partner.interest-account` (`671`), `accounting.partner.journal` (`OD`), `accounting.commitment.alert-days` (`30`), `accounting.commitment.journal` (`HB`), `accounting.commitment.given-account` (`901`), `accounting.commitment.given-counter-account` (`902`), `accounting.commitment.received-account` (`905`), `accounting.commitment.received-counter-account` (`906`), `accounting.payroll.journal` (`PA`), `accounting.payroll.section-required` (`false`).

## Pour le frontend

- **Emprunts** : liste (prêteur, montant, capital restant dû, prochaine échéance, statut) ; formulaire de création avec aperçu du tableau ; fiche avec le tableau (échéances écrites et prévues, pièce de chaque échéance), historique des versions et des événements ; actions « Débloquer », « Écrire l'échéance » (mode au relevé), « Remboursement anticipé » (date, montant, indemnité, durée ou échéance réduite), « Changer le taux », « Reporter l'échéance », « Imprimer », « Exporter » ; écran de clôture « Intérêts courus au » (date) ; note annexe des dettes financières.
- **Comptes courants d'associés** : liste avec solde ; fiche avec convention, relevé (période), saisie d'un mouvement (type, date, montant, contrepartie, TVA pour une dépense), calcul des intérêts d'une période (intérêts, part non déductible) ; note annexe.
- **Hors bilan** : registre filtrable (type, sens, statut), fiche (dates, opération liée, documents, pièce HB), inscription et modification, levée (date, pièce) ; encart de rapprochement registre et comptes 90 (vert si concordant) ; note annexe à une date ; gestion des types.
- **Paie** : paramétrage des modèles, rubriques (nature, comptes) et salariés (matricule, tiers, section) ; import d'un mois (fichier, période, modèle) avec compte rendu : totaux, tableau par salarié (brut, retenues, avances, net, charges patronales, écart), erreurs bloquantes et avertissements ; « Comptabiliser » (actif sans erreur) ; « Payer les salariés » (compte et journal de trésorerie, date, sélection) ; historique des imports par mois.
