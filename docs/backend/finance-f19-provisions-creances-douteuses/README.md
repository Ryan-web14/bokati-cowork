# Lot F19 : provisions et créances douteuses

Spécification : `docs/backend/plan-finance-comptabilite/provisions-et-creances-douteuses.md`. Branche `feature/finance-comptabilite`.

## Migrations

- Dev `V315__provisions_creances_douteuses_f19.sql`, prod `V309__provisions_creances_douteuses_f19.sql`, même contenu.
- Comptes ajoutés au plan : `6515`, `6591`, `6593`, `7591`, `7593`, `197`, `6911`, `7911`, `3911`, `697`, `797`, `4991`, `4918`, `297`. Les comptes `4161`, `4162`, `4911`, `4912`, `6594`, `7594`, `6511`, `499`, `19x` et `758` existaient déjà.
- Tables :
  - `acc_receivable_status` : état comptable de chaque facture ;
  - `acc_receivable_proposal` : file de revue ;
  - `acc_receivable_impairment` : dépréciation par facture et par période, et ses reprises ;
  - `acc_receivable_writeoff` : décisions de passage en perte ;
  - `acc_risk`, `acc_risk_valuation`, `acc_risk_provision_move` : registre des risques ;
  - `acc_provision_review` : revue de clôture ;
  - `acc_retirement_obligation` : indemnités de fin de carrière.
- Permissions `RECEIVABLE_RISK:READ`, `CLASSIFY`, `IMPAIR`, `WRITE_OFF_PREPARE`, `WRITE_OFF_APPROVE`, `WRITE_OFF_DIRECTION`, puis `RISK_PROVISION:MANAGE`, `REVIEW` et `CUSTOMER_CREDIT:OVERRIDE`. Elles sont données à `SUPER_ADMIN`, `ADMIN` et `FINANCE`, sauf `WRITE_OFF_DIRECTION`, réservée à `SUPER_ADMIN` et `ADMIN`. `MANAGER` reçoit la lecture et la dérogation.

## Créances clients (`ReceivableRiskService`)

| État | Compte | Déclencheur |
| --- | --- | --- |
| `NORMAL` | `411` | Facture émise |
| `DISPUTED` | `4161` | Classement à la main (contestation) |
| `DOUBTFUL` | `4162` | Proposition acceptée, classement à la main, ou automatique au-delà de 365 jours |
| `RECOVERED` | soldée | Encaissement total d'une facture à risque |
| `WRITTEN_OFF` | soldée par `6511` | Décision de perte validée, ou facture `WRITTEN_OFF` dans la facturation |
| `RECOVERED_AFTER_LOSS` | `758` | Encaissement après la perte |

- **File de revue** : chaque nuit, une proposition pour toute facture échue depuis plus de 90 jours ou ayant atteint l'étape de relance `HANDOVER`. Au-delà de 365 jours, le classement est fait d'office. Décisions possibles : accepter, reporter avec une date de revue, rejeter avec un motif.
- **Contagion** : quand une facture d'un client est classée douteuse, ses autres factures échues sont proposées.
- **Reclassement** : D `4162` (ou `4161`) ; C `411` du reste dû TTC, avec la référence de la facture (lettrage automatique de nuit sur `4161` et `4162` comme sur `411`) ; PV `PCD`.
- **Retour en normal** : reclassement inverse et reprise de la dépréciation.
- **Dépréciation** : base HT du reste dû (reste dû TTC multiplié par HT/TTC), diminuée des garanties. Les garanties sont le solde créditeur du `4191` du client, imputé d'abord sur la créance la plus ancienne.
  - Méthode statistique, avec la grille `91-180:25,181-365:50,366+:100`.
  - Ou taux individuel justifié par un motif.
  - Au-delà de 500 000 HT sans taux individuel : la grille s'applique, avec un avertissement.
- **Ajustement** : simulation, puis passage par période (le 1er du mois pour le mois écoulé, rythme `MONTHLY` ; `CLOSING_ONLY` disponible). Une écriture unique par période, avec le détail par facture : D `6594` C `4912` (ou `4911`), ou l'inverse vers `7594`.
- **Encaissement** : un paiement imputé à une facture à risque crédite `4162` ou `4161` au lieu de `411` (moteur F4, compte par imputation). La dépréciation est reprise au prorata de ce qui est encaissé : D `491x` ; C `7594`, aussitôt (`IMMEDIATE`) ou au prochain ajustement. Un paiement total passe la facture en `RECOVERED`.
- **Passage en perte** : décision préparée (motif, justificatifs), puis validée par une autre personne selon la grille.
  - Jusqu'à 100 000 TTC : responsable financier (`WRITE_OFF_APPROVE`).
  - Au-delà : direction (`WRITE_OFF_DIRECTION`).
  - Écritures : D `6511` HT ; D `4431` TVA si sa récupération est admise (sinon D `6511` TTC) ; C `4162` ou `411` ; reprise de la dépréciation. PV `DPP`.
  - La facture passe `WRITTEN_OFF` dans la facturation.
- **Synchronisation** : une facture mise en `WRITTEN_OFF` par la facturation sans décision comptable est soldée chaque nuit, de la même façon.
- **Récupération après perte** : un paiement sur une facture perdue est crédité au `758`.

### Effets hors comptabilité

- Relances : une facture contestée (`DISPUTED`) n'est plus relancée. Le palier est noté `SKIPPED` avec le motif.
- Politique d'accès, paramètre `accounting.doubtful.customer-policy` (suffixes `.member`, `.customer`, `.business-entity` par segment) :
  - `ALERT` : avertissement journalisé ;
  - `PREPAY` : avertissement « payable d'avance » ;
  - `BLOCK` : réservation et souscription refusées (`403`), sauf avec le droit `CUSTOMER_CREDIT:OVERRIDE`. La dérogation est journalisée.
- Interface `core/accounting/ReceivableRiskQuery` : la facturation, les réservations et les abonnements ne dépendent pas de la comptabilité.

## Risques et provisions (`RiskProvisionService`)

- **Fiche** `RSK-` : nature, tiers, contrat, conseil, juridiction, probabilité (`PROBABLE`, `POSSIBLE`, `REMOTE`), estimation, date de sortie probable (moins d'un an : court terme `4991`), déductibilité, section.
- **Natures et comptes** :

  | Nature | Provision | Dotation | Reprise |
  | --- | --- | --- | --- |
  | Litige client ou salarié | `191` | `6911` | `7911` |
  | Garantie donnée | `192` | `6911` | `7911` |
  | Contrôle fiscal | `195` | `6911` | `7911` |
  | Indemnités de fin de carrière | `196` | `6911` | `7911` |
  | Restructuration | `197` | `6911` | `7911` |
  | Sinistre, remise en état des locaux, autre | `198` | `6911` | `7911` |
  | Court terme (risques et charges) | `4991` | `6591` | `7591` |
  | Stocks | `3911` | `6593` | `7593` |
  | Avances au personnel, créances sur cession | `4918` | `6594` | `7594` |
  | Dépôts versés | `297` | `697` | `797` |
  | Trésorerie | `59` | `697` | `797` |

- **Mouvements** :
  - `ADJUST` porte la provision au montant estimé d'un risque probable ;
  - `ALLOWANCE` : dotation ;
  - `USED_RELEASE` et `UNUSED_RELEASE` : reprises, distinguées dans les états ;
  - reclassement long terme vers court terme quand l'échéance passe sous un an.

  Un risque `POSSIBLE` n'est pas provisionné : il figure en passif éventuel.
- **Indemnités de fin de carrière** : le résultat d'un calcul, interne ou d'un actuaire, est importé. La provision `196` est portée à ce montant.
- **Revue de clôture** : préparée, puis validée par une autre personne (droit `RISK_PROVISION:REVIEW`). PV `RVP`.

## Clôture

`GET /accounting/provisions/closing-check?year=` refuse la clôture tant que l'une des conditions suivantes n'est pas remplie :
- revue des provisions validée ;
- ajustement des dépréciations de décembre passé ;
- file de revue vide ;
- provisions à moins d'un an reclassées ;
- indemnités de fin de carrière recalculées ;
- contrôles des créances sans écart bloquant.

**Réglé** : la liste de clôture du lot F12 appelle ce contrôle (étape `PROVISIONS`).

## Contrôles quotidiens

- Soldes de `4161` et `4162` égaux au reste dû suivi.
- Soldes de `4911` et `4912` égaux aux dépréciations par facture.
- Aucune facture `WRITTEN_OFF` de la facturation non soldée.
- Factures échues depuis plus de 90 jours ni classées ni reportées.
- Dépréciation restante sur une facture soldée.

Les écarts bloquants sont signalés aux comptables.

## Routes (`/sni/api/v1/accounting`)

| Route | Usage |
| --- | --- |
| `GET /receivables/aging?date=&segment=`, `GET /receivables/{n}` | Balance âgée avec risque, une facture |
| `GET /receivables/proposals`, `POST /receivables/proposals/{id}/decision` | File de revue (`ACCEPT`, `POSTPONE`, `REJECT`) |
| `POST /receivables/{n}/classify`, `/back-to-normal`, `/individual-rate` | Classement, retour en normal, taux individuel |
| `GET /receivables/impairment-runs/preview?period=`, `POST /receivables/impairment-runs?period=` | Ajustement |
| `GET`, `POST /receivables/write-offs`, `POST /receivables/write-offs/{id}/approve`, `/reject` | Passage en perte |
| `GET /receivables/controls` | Contrôles |
| `GET`, `POST /risks`, `GET /risks/{code}`, `POST /risks/{code}/valuations`, `/provision`, `POST /risks/reclass-short-term`, `POST /risks/retirement-obligations` | Registre et provisions |
| `POST /provisions/reviews`, `POST /provisions/reviews/{id}/validate`, `GET /provisions/closing-check?year=` | Revue et clôture |
| `GET /provisions/reports/{report}?year=&date=&segment=&format=json\|csv\|pdf` | États |

États disponibles :

| Code | Contenu |
| --- | --- |
| `aging` | Balance âgée avec risque |
| `doubtful` | Créances douteuses et litigieuses |
| `impairment-variation` | Variation des dépréciations |
| `provision-variation` | Variation des provisions pour risques et charges |
| `risk-register` | Registre des risques |
| `contingent-liabilities` | Passifs éventuels |
| `losses` | Pertes sur créances |
| `tax-reintegrations` | Réintégrations et déductions fiscales |
| `indicators` | Indicateurs du recouvrement |

## Paramètres (`ACCOUNTING`, tous lus à chaud)

| Thème | Clés |
| --- | --- |
| Créances | `accounting.receivable.*` : `cron`, `trigger-days`, `auto-days`, `contagion`, `individual-threshold`, `grid`, `adjustment`, `release`, `writeoff-direction-threshold`, `writeoff-vat-recovery`, `guarantee-account`, comptes et `journal` |
| Politique d'accès | `accounting.doubtful.customer-policy` |
| Provisions | `accounting.provision.*` : `journal` et comptes à court terme |

## Non fait dans ce lot

- **Réglé** : déclencheurs supplémentaires. Un chèque rejeté non régularisé après 15 jours (`CHEQUE_REJECTED`) et un abonnement suspendu pour impayé depuis plus de 30 jours (`SUBSCRIPTION_SUSPENDED`) proposent maintenant le classement des factures concernées. Les délais sont réglables : `accounting.receivable.cheque-rejected-days` et `suspended-days`. La procédure collective reste un classement à la main, avec pièce.
- **Réglé** : garanties. La base déduit le portefeuille (`4191`), la caution reçue (`165`, paramètre `accounting.receivable.deposit-account`) et les avoirs émis non encore imputés.
- **Réglé** : politique `PREPAY`. Une réservation facturée sur compte (organisation payeuse) est une vente à crédit : elle est refusée (`403`) sauf avec le droit `CUSTOMER_CREDIT:OVERRIDE`. Une réservation payée d'avance (paiement direct attendu, portefeuille, droit d'abonnement ou de pass) passe, avec l'avertissement.
- **TVA à la récupération après perte** : elle n'est pas reversée automatiquement.
- **Portail** : la mention « en cours d'examen » sur les factures contestées est à afficher par le frontend, à partir de l'état `DISPUTED`.

## Tests

- `ProvisionF19IT`, sur le schéma de production :
  - l'exemple de la section 6.5 rejoué au franc près : classement de 590 000, dépréciation de 250 000, encaissement de 236 000 crédité au `4162` avec reprise de 100 000, perte de 300 000 HT et 54 000 de TVA avec reprise de 150 000, facture `WRITTEN_OFF`, validation par une autre personne. L'encaissement est daté du jour, une date future n'étant pas encore reprise ;
  - une perte décidée dans la facturation, soldée en comptabilité ;
  - un client en `BLOCK` refusé sans dérogation, accepté avec ;
  - une clôture refusée sans revue validée par une autre personne, puis acceptée.
- Le test utilise son propre journal : ses écritures de décembre ne gênent pas la chronologie stricte des autres tests sur `OD`.

## Pour le frontend

Écrans à prévoir :
- balance âgée avec risque ;
- file de revue, avec décision en masse ;
- fiche de créance (classement, retour en normal, taux individuel) ;
- ajustement : simulation puis passage ;
- décisions de passage en perte, en deux temps ;
- registre des risques, avec estimation et provision ;
- revue de clôture ;
- états.

Au guichet et sur la fiche client, afficher l'avertissement renvoyé par la politique d'accès. Le client ne voit jamais le mot « douteux ».
