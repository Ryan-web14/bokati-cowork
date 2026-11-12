# Provisions et créances douteuses

Volet du [plan finance](README.md) consacré aux pertes probables : créances clients qu'on risque de ne pas encaisser, risques et charges futurs (litiges, garanties, redressements, indemnités de fin de carrière, remise en état des locaux), dépréciations des stocks et des autres actifs. Il couvre le classement des créances, le calcul et l'ajustement des provisions, le passage en perte, les récupérations, le registre des risques et les états de clôture.

Rédigé le 11/11/2026, d'après le code de la facturation (`features/billing`, statut `WRITTEN_OFF`, balance âgée de `BillingRecoverableServiceImpl`) et des relances (`features/billing/dunning` : `DunningPolicy`, `DunningStep` avec les actions `REMINDER`, `FORMAL_NOTICE`, `GRACE_PERIOD`, `SUSPEND`, `HANDOVER`, `DunningNotice`). Les numéros de comptes sont des **propositions SYSCOHADA révisé** à valider par l'expert-comptable (décision 2) ; ceux marqués « à confirmer » doivent l'être avant le lot. Toutes les écritures sont en partie double ([section 4.6](README.md#46-partie-double--règles-et-garanties)) et les pièces suivent la numérotation continue ([section 4.8](README.md#48-numérotation-des-pièces--continuité-et-protection)).

---

## Sommaire

1. [Principes](#1-principes)
2. [Comptes](#2-comptes)
3. [Cycle d'une créance client](#3-cycle-dune-créance-client)
4. [Classement en douteux ou litigieux](#4-classement-en-douteux-ou-litigieux)
5. [Calcul de la dépréciation des créances](#5-calcul-de-la-dépréciation-des-créances)
6. [Ajustement, encaissement, perte, récupération](#6-ajustement-encaissement-perte-récupération)
7. [Effets sur la facturation, les relances et l'accès aux services](#7-effets-sur-la-facturation-les-relances-et-laccès-aux-services)
8. [Provisions pour risques et charges](#8-provisions-pour-risques-et-charges)
9. [Autres dépréciations](#9-autres-dépréciations)
10. [Écritures](#10-écritures)
11. [États](#11-états)
12. [Contrôles et clôture](#12-contrôles-et-clôture)
13. [Pièces émises](#13-pièces-émises)
14. [Données](#14-données)
15. [Routes](#15-routes)
16. [Droits](#16-droits)
17. [Paramétrage](#17-paramétrage)
18. [Lots et critères d'acceptation](#18-lots-et-critères-dacceptation)

---

## 1. Principes

1. **Prudence, sans arbitraire** : toute perte probable et mesurable est constatée dans l'exercice où elle apparaît ; chaque montant est justifié par une méthode, une pièce et une décision tracée.
2. **La facture reste la référence** : classement, dépréciation, encaissement et perte se suivent **facture par facture** (et non seulement par client), pour que le lettrage, la balance âgée et l'état de compte restent justes.
3. **Le logiciel propose, une personne décide** : le traitement de nuit repère les créances à risque et calcule une dépréciation ; un comptable valide le classement et le montant ; le passage en perte exige une seconde validation.
4. **La comptabilité et la facturation disent la même chose** : une facture douteuse est sur `416`, une facture passée en perte est `WRITTEN_OFF` côté facturation et soldée en comptabilité, sans exception (contrôle quotidien, section 12).
5. **Une provision se revoit** : chaque dépréciation et chaque provision est réexaminée au moins à chaque clôture ; elle est ajustée, reprise ou utilisée, jamais oubliée.
6. **Saisie minimale** : ancienneté, base, garanties, taux, comptes et écritures sont calculés ; on ne saisit qu'un taux individuel justifié, un motif dans une liste, une pièce jointe.
7. **Tout est paramétrable** (section 17) : seuils, grille de taux, déclencheurs, comptes, déductibilité fiscale, niveaux de validation.

---

## 2. Comptes

| Usage | Compte proposé |
| --- | --- |
| Clients | `411` (auxiliaire par client) |
| Clients litigieux (créance contestée) | `4161` (auxiliaire par client) |
| Clients douteux (recouvrement incertain) | `4162` (auxiliaire par client) |
| Dépréciation des créances litigieuses, douteuses | `4911`, `4912` |
| Dépréciation des autres créances (débiteurs divers, personnel, cessions) | `49x` (à confirmer) |
| Dotation, reprise : dépréciation des créances d'exploitation | `6594` ; `7594` |
| Pertes sur créances clients, autres débiteurs | `6511`, `6515` |
| Récupération sur créance passée en perte | `75x` (à confirmer) |
| Provisions pour risques à court terme (moins d'un an) | `499` ; dotation `6591`, reprise `7591` |
| Provisions pour risques et charges (plus d'un an) | `191` litiges, `192` garanties données aux clients, `195` impôts, `196` pensions et indemnités de fin de carrière, `197` restructuration, `198` autres (dont remise en état des locaux) ; dotation `6911`, reprise `7911` |
| Provisions financières et hors activités ordinaires | dotation `697` / reprise `797` ; dotation `85x` / reprise `86x` (à confirmer) |
| Dépréciation des stocks (marketplace, boissons, fournitures) | `39x` ; dotation `6593`, reprise `7593` |
| Dépréciation des titres et de la trésorerie | `59x` ; dotation `679x`, reprise `779x` (à confirmer) |

Les comptes `4161` et `4162` sont **lettrables** avec la même clé que `411` (numéro de facture) ; `4911` et `4912` portent la référence de la facture dépréciée sur chaque ligne.

---

## 3. Cycle d'une créance client

| État comptable de la facture | Compte | Ce qui le déclenche |
| --- | --- | --- |
| `NORMAL` | `411` | Émission de la facture |
| `OVERDUE` | `411` | Échéance dépassée (la facturation a déjà `OVERDUE`) ; aucune écriture |
| `DISPUTED` (litigieuse) | `4161` | Contestation enregistrée (section 4.2) |
| `DOUBTFUL` (douteuse) | `4162` | Classement validé (section 4.1) |
| `PROVISIONED` | `4161` ou `4162`, avec `491x` | Dépréciation comptabilisée ; reste douteuse ou litigieuse |
| `RECOVERED` | soldée | Encaissement total, avoir, compensation |
| `WRITTEN_OFF` (perte) | soldée par `651x` | Décision de passage en perte (section 6.3) |
| `RECOVERED_AFTER_LOSS` | `75x` | Encaissement après la perte |

Un **retour en normal** est possible (le client régularise ou le litige se règle en sa faveur) : reclassement inverse vers `411`, reprise de la dépréciation, motif tracé.

---

## 4. Classement en douteux ou litigieux

### 4.1 Créances douteuses

Le traitement de nuit propose le classement d'une facture quand un **déclencheur** est rempli (paramétrable, valeurs initiales) :

| Déclencheur | Source |
| --- | --- |
| Retard supérieur à 90 jours | Balance âgée (`DAYS_OVER_90` de la facturation) |
| Étape de relance `HANDOVER` atteinte sans paiement, ou `FORMAL_NOTICE` restée sans effet après 15 jours | `DunningNotice` |
| Abonnement suspendu pour impayé (`SUSPEND`) depuis plus de 30 jours | Relances, abonnements |
| Chèque rejeté non régularisé sous 15 jours | Cycle des chèques (lot F10) |
| Client en procédure collective (redressement, liquidation), décédé, disparu, courrier revenu | Saisie sur la fiche du tiers, avec pièce |
| Paiement mobile money contesté ou annulé par l'opérateur | Paiements |

- Les propositions apparaissent dans une **file de revue** avec le motif, l'ancienneté, l'historique des relances, les garanties détenues et la dépréciation suggérée. Le comptable valide (seul ou en masse), reporte avec une date de revue, ou rejette avec motif.
- Option `AUTO` par déclencheur : le classement se fait sans revue (par défaut : non, sauf « retard supérieur à 365 jours »).
- **Contagion** (paramètre, activé par défaut) : quand une facture d'un client est classée douteuse, ses autres factures échues sont proposées au classement ; les factures non échues restent en `411` mais sont signalées.
- Le classement produit la pièce de reclassement D `4162` ; C `411` pour le **reste dû TTC**, lettrée avec la ligne d'origine du `411`, et le **PV de classement** (`PCD-`).

### 4.2 Créances litigieuses

- Une contestation vient de la mention de contestation de l'état de compte ([rapprochement-et-comptes-tiers.md](rapprochement-et-comptes-tiers.md#32-contenu-imprimé)), d'un ticket du module `support` rattaché à la facture, ou d'une saisie du comptable ; motif choisi dans une liste (prestation non rendue, montant erroné, double facturation, salle indisponible, autre).
- La facture passe `DISPUTED` : reclassement vers `4161`, **relances suspendues sur cette facture** (les autres continuent), dépréciation selon le risque estimé.
- Issue du litige : avoir (le litige est fondé : la créance s'éteint par l'avoir, la dépréciation est reprise), retour en normal (non fondé), ou douteux puis perte.

---

## 5. Calcul de la dépréciation des créances

### 5.1 Base

- **Base hors taxe** : la TVA facturée se récupère si la perte devient définitive ; on ne déprécie donc que le HT. Pour une facture partiellement payée, la base est le HT du reste dû (reste dû TTC multiplié par HT / TTC).
- **Garanties déduites** : caution du contrat de bureau détenue (`165`), solde du portefeuille électronique du client (`4191`), avoir non imputé, dépôt d'un tiers garant. La garantie s'impute d'abord sur la créance la plus ancienne du client ; la base est réduite au prorata.
- Une créance dont la garantie couvre tout le reste dû n'est pas dépréciée ; elle reste douteuse jusqu'à l'imputation de la garantie.

### 5.2 Méthodes

| Méthode | Quand | Calcul |
| --- | --- | --- |
| **Individuelle** | Créance au-dessus du seuil (500 000 XAF de reste dû HT par défaut), litige, procédure collective | Taux estimé par le comptable, justifié (pièce, commentaire) |
| **Statistique par ancienneté** | Les autres créances douteuses | Grille : 91 à 180 jours de retard 25 % ; 181 à 365 jours 50 % ; plus de 365 jours 100 % (paramétrable) |
| **Procédure collective** | Client en redressement ou liquidation | 100 % par défaut, sauf dividende annoncé par le mandataire |

La méthode et le taux retenus sont enregistrés sur la ligne de dépréciation de chaque facture ; la dépréciation par client est la somme de ses factures.

### 5.3 Déductibilité fiscale

Chaque dépréciation porte un indicateur **déductible** ou **non déductible**, proposé selon des règles paramétrées par pays (par exemple : déductible si la créance a fait l'objet d'une relance écrite et d'une procédure de recouvrement, d'après le code général des impôts applicable ; règles à valider avec l'expert-comptable). Les montants non déductibles alimentent le tableau de passage au résultat fiscal de la liasse (réintégrations, puis déductions à la reprise).

---

## 6. Ajustement, encaissement, perte, récupération

### 6.1 Ajustement

- **Traitement d'ajustement** mensuel (paramètre : `MONTHLY` par défaut, `CLOSING_ONLY` disponible) et obligatoire à la clôture : pour chaque facture dépréciée, nouvelle base, nouveau taux selon l'ancienneté, différence avec la dépréciation existante.
- Différence positive : dotation complémentaire D `6594` ; C `4912`. Négative : reprise D `4912` ; C `7594`.
- **Simulation** avant comptabilisation, par client et par facture ; le comptable peut forcer un taux individuel avec motif. La pièce est unique par période, avec le détail par facture en annexe.

### 6.2 Encaissement d'une créance douteuse

- Le moteur d'imputation sait qu'une facture est douteuse : un paiement imputé à cette facture (`payment_allocation`) crédite **`4162`** (ou `4161`) au lieu de `411`, et se lettre avec la ligne de reclassement.
- La dépréciation devenue sans objet est reprise **dans la même pièce ou au plus tard au prochain ajustement** (paramètre : `IMMEDIATE` par défaut) : D `4912` ; C `7594` au prorata de ce qui a été encaissé.
- Paiement total : la facture passe `RECOVERED`, les relances s'arrêtent, le client peut retrouver ses accès (section 7).

### 6.3 Passage en perte

- **Conditions** proposées (paramétrables) : procédure collective clôturée pour insuffisance d'actif, client disparu après recherches, coût de recouvrement supérieur à la créance, transaction signée, décision de justice défavorable, petite créance au-delà d'un âge (par exemple moins de 25 000 XAF après 24 mois).
- **Décision de passage en perte** (`DPP-`) : préparée par le comptable avec pièces justificatives, validée selon une grille (jusqu'à 100 000 XAF responsable financier ; au-delà direction ; seuils paramétrables). Celui qui prépare ne valide pas.
- **Écritures** : D `6511` HT ; D `4431` TVA si la récupération est admise (sinon D `6511` TTC) ; C `4162` reste dû TTC ; et reprise de la dépréciation D `4912` ; C `7594`.
- Côté facturation : la facture passe `WRITTEN_OFF` (statut existant) par l'événement `INVOICE_WRITTEN_OFF` ; les relances de cette facture s'arrêtent ; la créance reste visible sur l'état de compte du client, au modèle complet, avec la mention « passée en perte ».
- Une perte peut aussi être décidée **sans classement préalable** (petite créance) : la pièce débite directement `6511` depuis `411`.

### 6.4 Récupération après perte

Un encaissement sur une facture `WRITTEN_OFF` est accepté par le module de paiement, imputé à la facture et écrit D trésorerie ; C `75x` récupération sur créances passées en perte (TVA reversée si elle avait été récupérée). La facture passe `RECOVERED_AFTER_LOSS` côté comptabilité ; la facturation garde `WRITTEN_OFF` et ajoute le paiement à l'historique.

### 6.5 Exemple

Facture de location de bureau : 500 000 HT, TVA 90 000, 590 000 TTC, échéance le 31 mars 2026, impayée.

| Date | Événement | Débit | Crédit |
| --- | --- | --- | --- |
| 30/06/2026 | Classement en douteux (91 jours, relance `HANDOVER`) | `4162` 590 000 | `411` 590 000 (lettré) |
| 31/12/2026 | Dépréciation statistique, 181 à 365 jours : 50 % du HT | `6594` 250 000 | `4912` 250 000 |
| 15/03/2027 | Encaissement partiel de 236 000 TTC (40 %) | `521x` 236 000 | `4162` 236 000 |
| 15/03/2027 | Reprise au prorata : 40 % de 250 000 | `4912` 100 000 | `7594` 100 000 |
| 30/09/2027 | Passage en perte du reste : 354 000 TTC (300 000 HT, 54 000 TVA) | `6511` 300 000 ; `4431` 54 000 | `4162` 354 000 |
| 30/09/2027 | Reprise du solde de la dépréciation | `4912` 150 000 | `7594` 150 000 |

Charge nette de 2027 : 300 000 de perte moins 250 000 de reprises, soit 50 000 ; la perte totale sur deux exercices (300 000) égale le HT non encaissé.

---

## 7. Effets sur la facturation, les relances et l'accès aux services

| Événement | Effet hors comptabilité |
| --- | --- |
| Facture classée douteuse | Indicateur « créance douteuse » sur la fiche du client (membre, client, entreprise) ; alerte au guichet et dans le back-office à chaque nouvelle réservation ou souscription |
| Client avec créance douteuse ou litigieuse | Selon le paramètre `accounting.doubtful.customer-policy` : `ALERT` (par défaut), `PREPAY` (nouvelles réservations et pass payables d'avance seulement), `BLOCK` (aucune nouvelle réservation ni souscription, sauf dérogation tracée) |
| Créance litigieuse | Relances suspendues pour cette facture ; reprise automatique à la fin du litige |
| Passage en perte | Facture `WRITTEN_OFF`, relances arrêtées ; le client reste en `PREPAY` ou `BLOCK` jusqu'à décision |
| Récupération totale | Indicateur levé, politique d'accès revenue à la normale |

Le portail du membre affiche, dans son état de compte, les factures en litige avec la mention « en cours d'examen », jamais le mot « douteux ».

---

## 8. Provisions pour risques et charges

### 8.1 Registre des risques

Chaque risque a une **fiche** :

| Champ | Origine |
| --- | --- |
| Code `RSK-`, nature (litige client, litige salarié, sinistre dans les espaces, garantie donnée, contrôle fiscal, indemnités de fin de carrière, remise en état des locaux, restructuration, autre) | **Liste** |
| Tiers, contrat, facture, événement d'origine | **Lien** vers la plateforme (contrat de bureau, réservation, membre, employé) |
| Description, avocat ou conseil, juridiction, prochaine échéance | Saisie, liste des conseils |
| Probabilité (`PROBABLE`, `POSSIBLE`, `REMOTE`) | Liste |
| Montant estimé, méthode d'estimation, date d'estimation, pièces (GED) | Saisie avec pièce |
| Échéance probable de sortie de ressources | Saisie ; elle range la provision en court terme (`499`) ou en long terme (`19x`) |
| Déductibilité fiscale | Proposée selon la nature |
| Section analytique | Déduite du site ou du contrat |

- **Probable et mesurable** : provision comptabilisée.
- **Possible**, ou non mesurable : pas de provision ; le risque figure en **passif éventuel** dans la note annexe et peut être inscrit au registre des engagements hors bilan ([financement-engagements-paie.md](financement-engagements-paie.md#3-registre-des-engagements-hors-bilan)).
- **Peu probable** : suivi seulement.

### 8.2 Cas propres à l'exploitant

| Risque | Traitement proposé |
| --- | --- |
| Litige avec un client ou un membre (remboursement réclamé, dommage) | Provision du montant probable, `191` ou `499` |
| Litige avec un salarié | Provision `191` ; lien avec l'import de paie pour le règlement |
| Sinistre dans les espaces (personne blessée, bien d'un membre endommagé) | Provision de la franchise et de la part non couverte par l'assurance |
| Garantie donnée (engagement de remboursement d'une prestation non satisfaisante) | Provision statistique `192` sur le chiffre d'affaires concerné |
| Contrôle fiscal en cours ou redressement notifié | Provision `195` du montant accepté ou probable, majorations comprises |
| Indemnités de fin de carrière | Provision `196` calculée chaque année (effectif, ancienneté, salaires, barème de la convention collective, probabilité de présence, actualisation) ; méthode et hypothèses en paramètre ; calcul importé si un actuaire le fait |
| Remise en état des locaux en fin de bail | Provision `198` constituée sur la durée du bail, ou intégrée au coût des aménagements selon le choix de l'expert-comptable ([immobilisations-et-amortissements.md](immobilisations-et-amortissements.md)) |
| Gros entretien | Pas de provision : traité par l'approche par composants (SYSCOHADA révisé) |
| Congés payés, primes à payer | Ce ne sont pas des provisions : dettes provisionnées (`428`, `438`) passées par l'import de paie ([financement-engagements-paie.md](financement-engagements-paie.md#4-import-du-journal-de-paie)) |

### 8.3 Vie d'une provision

| Opération | Écriture |
| --- | --- |
| Constitution | D `6911` (ou `6591`, `697`, `85x` selon la nature) ; C `19x` ou `499` |
| Révision à la hausse | Dotation complémentaire, même schéma |
| Révision à la baisse, risque éteint | Reprise **non utilisée** : D `19x` ou `499` ; C `7911` (ou `7591`, `797`, `86x`) |
| Paiement du risque (jugement, transaction, redressement) | La charge réelle est comptabilisée dans son compte (indemnité, impôt, honoraires), puis reprise **utilisée** de la provision : D `19x` ; C `7911` |
| Échéance qui passe sous un an | Reclassement de `19x` vers `499`, proposé d'office à la clôture |

Les reprises utilisées et non utilisées sont distinguées dans les états (note annexe, tableau de variation).

---

## 9. Autres dépréciations

| Actif | Déclencheur et méthode | Écriture |
| --- | --- | --- |
| Stocks de la marketplace, boissons, fournitures | Inventaire (`stock_journal_entry`) : articles périmés, abîmés, sans vente depuis N mois ; valeur de réalisation inférieure au coût | D `6593` ; C `39x` ; reprise inverse |
| Avances au personnel non justifiées et non récupérables | Avance en retard de justification au-delà d'un délai, employé parti ([demandes-et-justification.md](demandes-et-justification.md)) | D `6594` ou `659x` ; C `49x` |
| Créance sur cession d'immobilisation (`485`) | Retard du débiteur | D `6594` ; C `49x` |
| Dépôts de garantie versés (`275`) | Bailleur défaillant, restitution compromise | D `697` ; C `297` (à confirmer) |
| Trésorerie | Banque en difficulté, fonds bloqués chez un opérateur de paiement | D `679x` ; C `59x` (à confirmer) |
| Immobilisations | Voir [immobilisations-et-amortissements.md](immobilisations-et-amortissements.md#6-dépréciations) | |

---

## 10. Écritures

Ajoutées au [catalogue des écritures](catalogue-ecritures.md#15-provisions-et-créances-douteuses).

| Événement | Journal | Écriture |
| --- | --- | --- |
| `RECEIVABLE_DISPUTED` : créance contestée | `OD` | D `4161` ; C `411` reste dû TTC (lettré) |
| `RECEIVABLE_DOUBTFUL` : classement en douteux | `OD` | D `4162` ; C `411` reste dû TTC (lettré) |
| `RECEIVABLE_BACK_TO_NORMAL` : retour en normal | `OD` | D `411` ; C `4161` ou `4162` ; reprise de la dépréciation |
| `RECEIVABLE_IMPAIRMENT_ADJUSTED` : dotation ou reprise d'ajustement | `OD` | D `6594` ; C `4911` ou `4912`, ou l'inverse vers `7594`, détail par facture |
| Encaissement d'une facture douteuse | trésorerie | D trésorerie ; C `4162` ou `4161` (lettré) ; reprise au prorata D `491x` ; C `7594` |
| `INVOICE_WRITTEN_OFF` : passage en perte | `OD` | D `6511` HT ; D `4431` si récupération admise ; C `4162`, `4161` ou `411` ; reprise D `491x` ; C `7594` |
| Récupération après perte | trésorerie | D trésorerie ; C `75x` (et TVA reversée si elle avait été récupérée) |
| Imputation d'une garantie (caution, portefeuille) sur une créance douteuse | `OD` | D `165` ou `4191` ; C `4162` (lettré) |
| `RISK_PROVISION_POSTED` : constitution ou complément | `OD` | D `6911`, `6591`, `697` ou `85x` ; C `19x` ou `499` |
| `RISK_PROVISION_RELEASED` : reprise utilisée ou non | `OD` | D `19x` ou `499` ; C `7911`, `7591`, `797` ou `86x` |
| Reclassement long terme vers court terme | `OD` | D `19x` ; C `499` |
| Dépréciation des stocks | `OD` ou `ST` | D `6593` ; C `39x` ; reprise D `39x` ; C `7593` |

---

## 11. États

| État | Contenu | Usage |
| --- | --- | --- |
| **Balance âgée avec risque** | Par client et par facture : non échu, 1 à 30, 31 à 60, 61 à 90, plus de 90 jours ; état comptable, garanties, dépréciation, net | Recouvrement, clôture |
| **État des créances douteuses et litigieuses** | Factures, date de classement, motif, ancienneté, base HT, garanties, méthode, taux, dépréciation, déductibilité | Clôture, liasse |
| **Tableau de variation des dépréciations** | Par compte `491x`, `39x`, `49x`, `59x` : début, dotations, reprises, fin | Note annexe |
| **Tableau de variation des provisions pour risques et charges** | Par nature : début, dotations, reprises utilisées, reprises non utilisées, reclassements, fin ; court et long terme | Note annexe, bilan |
| **Registre des risques** | Fiches, probabilité, montant, échéance, provision, passif éventuel | Direction, commissaire aux comptes |
| **Passifs éventuels** | Risques possibles non provisionnés, avec estimation | Note annexe |
| **Pertes sur créances** | Factures passées en perte, décision, montant HT et TVA, récupérations | Fiscal, direction |
| **Réintégrations fiscales** | Dépréciations et provisions non déductibles, reprises correspondantes | Tableau de passage de la liasse |
| **Indicateurs** | Délai moyen de paiement (DSO), taux de créances douteuses, taux de couverture par les dépréciations, pertes sur chiffre d'affaires, par segment de relance (`MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY`) | Tableau de bord |
| **File de revue** | Propositions de classement, ajustements à valider, risques à réexaminer | Travail quotidien |

---

## 12. Contrôles et clôture

Contrôles quotidiens :

- solde de `4161` et `4162` par client = reste dû TTC de ses factures `DISPUTED` et `DOUBTFUL` ;
- solde de `491x` par client = somme des dépréciations par facture ;
- toute facture `WRITTEN_OFF` côté facturation est soldée en comptabilité, et inversement ;
- facture échue depuis plus de 90 jours, ni classée ni reportée avec une date de revue ;
- dépréciation sur une facture soldée (reprise oubliée) ;
- risque dont la prochaine échéance est passée, ou dont l'estimation a plus de 12 mois ;
- provision au registre sans écriture, ou écriture sur `19x` ou `499` sans fiche.

À la **clôture**, la liste de clôture ([etats-financiers.md](etats-financiers.md#6-états-dinventaire-et-de-clôture)) exige :

1. l'ajustement des dépréciations de la clôture comptabilisé ;
2. la file de revue vide, ou chaque élément reporté avec un motif ;
3. la revue du registre des risques faite et signée (PV `RVP-`) : chaque risque confirmé, ajusté ou repris ;
4. le reclassement des provisions dont l'échéance passe sous un an ;
5. les indemnités de fin de carrière recalculées ;
6. la dépréciation des stocks issue de l'inventaire ;
7. les contrôles ci-dessus sans écart.

---

## 13. Pièces émises

Ajoutées aux [pièces comptables](pieces-comptables.md#préfixes-proposés).

| Pièce | Préfixe | Contenu |
| --- | --- | --- |
| PV de classement en créance douteuse ou litigieuse | `PCD` | Client, factures, reste dû, déclencheur, historique des relances, garanties, dépréciation retenue, décision et signature |
| Décision de passage en perte | `DPP` | Client, factures, montants HT et TVA, justificatifs, motif, dépréciation reprise, préparateur et validateurs |
| PV de revue des provisions et dépréciations | `RVP` | Date, liste des risques et des créances revues, décisions, montants avant et après, signatures du comptable et du responsable financier |

Le code d'un risque (`RSK-`) n'est pas une pièce : il suit sa propre séquence, sans exigence de continuité, et n'est jamais réutilisé.

---

## 14. Données

| Table | Colonnes principales |
| --- | --- |
| `acc_receivable_status` | document_number, company_code, tiers, état (`NORMAL`, `DISPUTED`, `DOUBTFUL`, `RECOVERED`, `WRITTEN_OFF`, `RECOVERED_AFTER_LOSS`), compte actuel, depuis, motif, déclencheur, next_review_at, entry_number du reclassement, PV `PCD-` |
| `acc_receivable_proposal` | document_number, déclencheur, ancienneté, reste dû, garanties, dépréciation suggérée, statut (`PROPOSED`, `ACCEPTED`, `POSTPONED`, `REJECTED`), décidé par, motif |
| `acc_receivable_impairment` | document_number, période, base HT, garanties imputées, méthode, taux, montant, montant précédent, différence, déductible, motif de forçage, entry_number |
| `acc_receivable_writeoff` | décision `DPP-`, factures, HT, TVA, motif, justificatifs, préparé par, validé par, entry_number |
| `acc_risk` | code `RSK-`, nature, tiers, contrat, événement d'origine, description, conseil, juridiction, probabilité, montant estimé, méthode, échéance, terme (`SHORT`, `LONG`), déductible, section, statut (`OPEN`, `CLOSED`) |
| `acc_risk_valuation` | risk_id, date, montant, probabilité, motif, pièce, auteur |
| `acc_risk_provision_move` | risk_id, type (`ALLOWANCE`, `USED_RELEASE`, `UNUSED_RELEASE`, `RECLASS`), montant, entry_number |
| `acc_provision_review` | exercice, date, PV `RVP-`, revu par, validé par |
| `acc_retirement_obligation` | exercice, hypothèses, effectif, résultat, détail par employé (ou fichier importé), entry_number |

Liens avec l'existant : `billing_document` (statut `WRITTEN_OFF`, `OVERDUE`), balance âgée de la facturation, `DunningNotice` et `DunningPolicy`, `payment_allocation`, contrats (`features/contract`), tickets (`features/support`), stocks (`stock_journal_entry`), GED.

---

## 15. Routes

| Route | Usage |
| --- | --- |
| `GET /accounting/receivables/aging?date=&segment=` | Balance âgée avec risque |
| `GET /accounting/receivables/proposals` | File de revue des classements |
| `POST /accounting/receivables/proposals/{id}/decision` | Accepter, reporter, rejeter |
| `POST /accounting/receivables/{documentNumber}/classify` | Classer à la main en douteux ou litigieux |
| `POST /accounting/receivables/{documentNumber}/back-to-normal` | Retour en normal |
| `GET /accounting/receivables/impairment-runs/preview?period=` | Simulation de l'ajustement |
| `POST /accounting/receivables/impairment-runs` | Comptabiliser l'ajustement |
| `POST`, `GET /accounting/receivables/write-offs` | Préparer et lister les décisions de passage en perte |
| `POST /accounting/receivables/write-offs/{id}/approve` | Valider une décision |
| `GET`, `POST /accounting/risks` | Registre des risques |
| `POST /accounting/risks/{code}/valuations` | Nouvelle estimation |
| `POST /accounting/risks/{code}/provision` | Constituer, compléter, reprendre |
| `POST /accounting/risks/retirement-obligations` | Calcul ou import des indemnités de fin de carrière |
| `POST /accounting/provisions/reviews` | Revue de clôture, PV `RVP-` |
| `GET /accounting/provisions/reports/{report}` | États de la section 11 (PDF, Excel, CSV) |

---

## 16. Droits

| Permission | Pour |
| --- | --- |
| `RECEIVABLE_RISK:READ` | Balance âgée avec risque, états (comptable, recouvrement, direction, auditeur) |
| `RECEIVABLE_RISK:CLASSIFY` | Classer, reclasser, valider les propositions (comptable) |
| `RECEIVABLE_RISK:IMPAIR` | Comptabiliser l'ajustement, forcer un taux avec motif (comptable) |
| `RECEIVABLE_RISK:WRITE_OFF_PREPARE` | Préparer une décision de passage en perte (comptable) |
| `RECEIVABLE_RISK:WRITE_OFF_APPROVE` | Valider selon la grille (responsable financier, direction) ; jamais celui qui a préparé |
| `RISK_PROVISION:MANAGE` | Tenir le registre, estimer, constituer et reprendre (comptable, juriste) |
| `RISK_PROVISION:REVIEW` | Valider la revue de clôture (responsable financier) |
| `CUSTOMER_CREDIT:OVERRIDE` | Déroger au blocage d'un client à créance douteuse (responsable commercial ou financier, motif tracé) |

---

## 17. Paramétrage

| Paramètre | Valeur initiale |
| --- | --- |
| Déclencheurs du classement | Retard supérieur à 90 jours, `HANDOVER`, `FORMAL_NOTICE` sans effet après 15 jours, suspension depuis 30 jours, chèque rejeté non régularisé sous 15 jours, procédure collective |
| Classement automatique sans revue | Seulement pour un retard supérieur à 365 jours |
| Contagion aux autres factures du client | Oui |
| Seuil de la méthode individuelle | 500 000 XAF de reste dû HT |
| Grille statistique | 91 à 180 jours : 25 % ; 181 à 365 jours : 50 % ; plus de 365 jours : 100 % |
| Base | HT, garanties déduites (caution, portefeuille, avoirs non imputés) |
| Rythme de l'ajustement | Mensuel ; obligatoire à la clôture |
| Reprise à l'encaissement | Immédiate |
| Règles de déductibilité fiscale | Par pays, à valider avec l'expert-comptable |
| Grille de validation des pertes | Jusqu'à 100 000 XAF : responsable financier ; au-delà : direction |
| Récupération de la TVA sur les pertes | Selon la législation, à confirmer ; sinon perte TTC |
| Politique d'accès d'un client à créance douteuse | `ALERT` (`PREPAY`, `BLOCK` disponibles), par segment |
| Natures de risques et comptes associés | Tableau de la section 8.2 |
| Seuil court terme des provisions | Échéance à moins d'un an : `499` |
| Indemnités de fin de carrière | Méthode, barème, taux d'actualisation, taux de rotation, ou import d'un calcul externe |
| Délai de réexamen d'un risque | 6 mois ; alerte si l'estimation a plus de 12 mois |
| Dépréciation des stocks | Sans vente depuis 6 mois, date de péremption dépassée |
| Comptes | Section 2 |

---

## 18. Lots et critères d'acceptation

Le volet devient un lot à part, **F19**, détaché de F12 (« Clôture ») qui garde les contrôles de clôture, les à-nouveaux et les états financiers.

| Lot | Contenu | Dépend de | Critère d'acceptation |
| --- | --- | --- | --- |
| **F19. Provisions et créances douteuses** | États comptables des factures, déclencheurs depuis la balance âgée, les relances et les chèques, file de revue, reclassements `4161` et `4162` lettrés, dépréciation individuelle et statistique sur base HT nette des garanties, ajustement mensuel et de clôture, imputation des paiements sur `416`, reprise au prorata, passage en perte avec grille de validation et lien avec `WRITTEN_OFF`, récupération après perte, politique d'accès des clients, registre des risques, provisions pour risques et charges court et long terme, indemnités de fin de carrière, dépréciation des stocks et des autres actifs, états, notes, réintégrations fiscales | F1, F2, F4 (comptes clients, lettrage) ; F10 pour les chèques rejetés ; F12 s'appuie sur lui pour la clôture | L'exemple de la section 6.5 se rejoue à l'identique ; une facture passée `WRITTEN_OFF` dans la facturation est soldée en comptabilité avec reprise de sa dépréciation ; un paiement sur une facture douteuse crédite `4162` et réduit la dépréciation ; un client en `BLOCK` ne peut pas réserver sans dérogation ; la clôture est refusée tant que la revue des provisions n'est pas validée |
