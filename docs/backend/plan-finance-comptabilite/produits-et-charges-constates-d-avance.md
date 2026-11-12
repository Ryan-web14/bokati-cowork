# Produits et charges constatés d'avance, rattachement à la période

Volet du [plan finance](README.md) consacré au **rattachement des produits et des charges à la bonne période** :

- **produits constatés d'avance** (`477`) : ce qui est facturé mais pas encore rendu (abonnement annuel, pass, domiciliation, réservation future, événement à venir) ;
- **charges constatées d'avance** (`476`) : ce qui est facturé à l'exploitant mais concerne une période future (loyer payé d'avance, assurance annuelle, licences, maintenance) ;
- et leur pendant, pour un rattachement complet : **produits à recevoir** (service rendu, pas encore facturé) et **charges à payer** (service reçu, facture pas encore arrivée).

Il détaille la [décision 5](README.md#19-décisions-prises) (chiffre d'affaires étalé sur la période de service, mode mensuel par défaut).

Rédigé le 11/11/2026 d'après le code :

- les lignes de facture (`BillingDocumentLine`) n'ont **pas de période de service** : elles ont `itemCode`, `category`, `sourceType`, `sourceCode` et les montants ;
- les abonnements (`Subscription` : `currentPeriodStart`, `currentPeriodEnd`, `pauseUntil`, gels `SubscriptionFreeze`, résiliations `SubscriptionTermination`, prorata `ProrationPolicy`, dépassements `SubscriptionOverageCharge`) ont leurs dates ;
- les pass (`Pass` : `validFrom`, `validUntil`) aussi ;
- les contrats de domiciliation (`DomiciliationContract` : `startDate`, `endDate`, `billingCycle`) aussi ;
- les réservations et les événements (`Event`, `EventRegistration`) aussi.

Les numéros de comptes sont des propositions SYSCOHADA révisé à valider par l'expert-comptable. Toutes les écritures sont en partie double et numérotées sans trou.

---

## Sommaire

1. [Principes](#1-principes)
2. [Comptes](#2-comptes)
3. [La période de service sur chaque ligne](#3-la-période-de-service-sur-chaque-ligne)
4. [Produits constatés d'avance](#4-produits-constatés-davance)
5. [Charges constatées d'avance](#5-charges-constatées-davance)
6. [Produits à recevoir et charges à payer](#6-produits-à-recevoir-et-charges-à-payer)
7. [Traitement de fin de période](#7-traitement-de-fin-de-période)
8. [Effets sur les autres modules](#8-effets-sur-les-autres-modules)
9. [Écritures](#9-écritures)
10. [États](#10-états)
11. [Contrôles et clôture](#11-contrôles-et-clôture)
12. [Données](#12-données)
13. [Routes](#13-routes)
14. [Droits](#14-droits)
15. [Paramétrage](#15-paramétrage)
16. [Lots et critères d'acceptation](#16-lots-et-critères-dacceptation)

---

## 1. Principes

1. **Le produit suit le service, la charge suit la consommation**, pas la date de facture ni la date de paiement. Un abonnement annuel facturé en octobre produit du chiffre d'affaires chaque jour pendant douze mois.
2. **La période vient de la plateforme, pas d'une saisie.** Chaque ligne de facture porte sa période de service, renseignée par le module qui la crée (abonnement, pass, domiciliation, réservation, événement). Côté charges, la période est proposée par la nature d'opération (assurance : 12 mois à partir de la date de la pièce) et confirmée d'un clic.
3. **Un échéancier par ligne** : chaque ligne concernée reçoit un échéancier de reconnaissance. Le compte `477` (ou `476`) est, à toute date, la somme exacte des parts non encore reconnues des échéanciers. C'est son justificatif.
4. **Méthode de l'ajustement du solde** : en fin de période, le traitement calcule le solde nécessaire de `477` et de `476` ligne par ligne, et passe une seule pièce pour la différence avec le solde déjà comptabilisé. Il n'y a ni double écriture ni oubli d'extourne, et relancer le traitement ne change rien.
5. **Les avoirs se compensent d'eux-mêmes** : une ligne d'avoir reprend la période de la ligne qu'elle corrige, avec un montant négatif ; le calcul suivant réduit le `477` sans traitement spécial.
6. **La TVA n'est pas étalée** : elle suit son exigibilité (décision 4), sur les débits ou sur les encaissements. Seul le hors taxe est étalé.
7. **Paiement et étalement sont indépendants** : une facture impayée s'étale quand même ; un acompte reçu sans facture va en `4191`, pas en `477`.
8. **Seuil de signification** : une charge sous le seuil (50 000 XAF par défaut) n'est pas étalée ; côté produits, tout s'étale (volume et automatisme le permettent).

---

## 2. Comptes

| Usage | Compte proposé |
| --- | --- |
| Produits constatés d'avance | `477` (divisionnaires par activité si souhaité : abonnements, pass, domiciliation, réservations, événements) |
| Charges constatées d'avance | `476` |
| Clients, factures à établir (produits à recevoir) | `4181` |
| Fournisseurs, factures non parvenues (charges à payer) | `408` |
| Autres charges à payer et produits à recevoir | Personnel `4286` / `4287`, organismes sociaux `4386` / `4387`, État `4486` / `4487`, débiteurs et créditeurs divers `4686` / `4687` (à confirmer) |
| Avances versées aux fournisseurs (sans facture) | `409` |
| Avances reçues des clients (sans facture) | `4191` |

Les comptes `4181` et `408` sont **lettrables** : l'estimation se lettre avec la facture définitive (clé : référence de la réservation, du contrat ou de la commande).

---

## 3. La période de service sur chaque ligne

### 3.1 Nouveaux champs de la ligne de facture

Ajoutés à `billing_document_line` (migration dans les deux dossiers Flyway) :

| Champ | Contenu |
| --- | --- |
| `service_start`, `service_end` | Période couverte par la ligne (dates incluses) |
| `recognition_method` | `POINT_IN_TIME`, `DAILY`, `MONTHLY`, `USAGE`, `EVENT_DATE` |
| `recognition_source` | Origine de la période : `SUBSCRIPTION`, `PASS`, `DOMICILIATION`, `BOOKING`, `EVENT`, `CATEGORY_RULE`, `MANUAL` |
| `original_line_id` | Pour une ligne d'avoir : la ligne corrigée, dont elle reprend la période |

### 3.2 Qui la renseigne

| Source de la ligne | Période | Méthode par défaut |
| --- | --- | --- |
| Abonnement (cycle facturé) | `currentPeriodStart` à `currentPeriodEnd` du cycle facturé, ou `pendingRenewalStart` à `pendingRenewalEnd` pour un renouvellement | `DAILY` |
| Option d'abonnement (`SubscriptionAddon`) | Période du cycle de l'abonnement | `DAILY` |
| Prorata (changement de formule en cours de période) | Du jour du changement à la fin du cycle | `DAILY` |
| Pass à durée de validité | `validFrom` à `validUntil` | `DAILY` |
| Pass à crédits (entrées, heures) | `validFrom` à `validUntil`, reconnaissance à la consommation | `USAGE`, solde reconnu à l'expiration |
| Domiciliation | Période du cycle (`billingCycle`) dans le contrat | `DAILY` |
| Réservation facturée avant sa date | Date (ou dates) du créneau réservé | `EVENT_DATE` |
| Inscription à un événement | Date de l'événement | `EVENT_DATE` |
| Dépassement (`SubscriptionOverageCharge`) facturé après coup | Période de consommation passée | `POINT_IN_TIME` (déjà rendu) |
| Marketplace, boissons, frais de dossier, débours | Aucune | `POINT_IN_TIME` |
| Ligne libre saisie au back-office | Proposée par la catégorie (`category`) de la ligne ; modifiable avec droit | Selon la règle de la catégorie |

Une ligne sans période est reconnue à la date de la facture. Une ligne d'une catégorie qui exige une période (abonnement, pass, domiciliation) mais qui n'en a pas est signalée comme anomalie (section 11).

### 3.3 Événements qui modifient la période

| Événement | Effet sur l'échéancier |
| --- | --- |
| **Gel ou pause** d'abonnement (`SubscriptionFreeze`, `pauseUntil`) | La période restante est **décalée** de la durée du gel ; rien n'est reconnu pendant le gel ; une nouvelle version de l'échéancier est créée |
| **Résiliation avec avoir** de la part non rendue | L'avoir porte la période restante ; le `477` de la ligne tombe à zéro au calcul suivant |
| **Résiliation sans remboursement** (part conservée, `EarlyTerminationFormula`) | La part non rendue est reconnue à la date de résiliation (produit de résiliation, compte paramétrable) |
| **Suspension pour impayé** | Aucun effet par défaut (le service reste dû) ; paramètre pour suspendre la reconnaissance si le contrat le prévoit |
| **Annulation d'une réservation** | Avoir : le `477` de la ligne s'annule ; sans avoir (frais d'annulation conservés) : reconnu à la date d'annulation |
| **Pass expiré avec crédits non utilisés** | Le solde est reconnu à l'expiration ; option de reconnaissance anticipée sur un taux de non-utilisation historique |
| **Report d'un événement** | La date de l'événement est mise à jour ; l'échéancier suit |

---

## 4. Produits constatés d'avance

### 4.1 Calcul

- `DAILY` : part reconnue = montant HT × jours de service écoulés / jours de la période (jours réels). La dernière échéance prend l'arrondi, pour que le total soit exact au franc près.
- `MONTHLY` : par mois entier, pour les cycles alignés sur le mois.
- `USAGE` : part reconnue = montant HT × crédits consommés / crédits achetés ; consommations lues dans les réservations ou les pointages d'entrée.
- `EVENT_DATE` : tout est reconnu à la date du service.
- La part non reconnue à la date de calcul est le **besoin de `477`** pour la ligne.

### 4.2 Exemples

**Abonnement annuel** de 1 200 000 XAF HT, facturé le 15 octobre 2026 pour la période du 15 octobre 2026 au 14 octobre 2027 (365 jours).

| Date | Jours écoulés | Reconnu cumulé | Besoin de `477` | Pièce de fin de mois |
| --- | --- | --- | --- | --- |
| 15/10/2026 | | | | Facture : D `411` TTC ; C `706` 1 200 000 ; C `4431` TVA |
| 31/10/2026 | 17 | 55 890 | 1 144 110 | D `706` 1 144 110 ; C `477` 1 144 110 |
| 30/11/2026 | 47 | 154 521 | 1 045 479 | D `477` 98 631 ; C `706` 98 631 |
| 31/12/2026 | 78 | 256 438 | 943 562 | D `477` 101 917 ; C `706` 101 917 |
| 14/10/2027 | 365 | 1 200 000 | 0 | Dernière reprise, arrondi absorbé |

Au bilan du 31 décembre 2026, le `477` porte 943 562 XAF pour cette ligne, et le chiffre d'affaires 2026 retient 256 438 XAF.

**Abonnement mensuel** facturé le 25 décembre pour janvier : tout le HT est en `477` au 31 décembre, et reconnu en janvier.

**Salle réservée pour le 10 janvier**, facturée le 20 décembre : tout en `477` au 31 décembre, reconnu le 10 janvier.

**Carnet de 10 entrées** à 50 000 XAF HT, valable trois mois, dont 4 entrées utilisées à la fin du mois : 20 000 reconnus, 30 000 en `477`. À l'expiration, avec 2 entrées non utilisées, le solde est reconnu.

---

## 5. Charges constatées d'avance

### 5.1 Sources

| Source | Période |
| --- | --- |
| Facture fournisseur (lot F13) | Champs `service_start`, `service_end` sur la ligne, **proposés** par la nature d'opération (loyer : le trimestre facturé ; assurance, licence, maintenance : 12 mois à partir de la date de la pièce) |
| Ligne de justification d'une avance (lot F6) | Idem, sur la ligne de justification |
| Bon de sortie de caisse (dépense directe) | Idem, sur le mouvement, si la nature est « périodique » |
| Écriture manuelle | Échéancier saisi sur la ligne de charge, avec droit |

Natures « périodiques » proposées : loyer et charges locatives, assurances, abonnements logiciels et licences, maintenance et contrats d'entretien, internet et téléphonie facturés d'avance, location de matériel, publicité pour une période future, cotisations annuelles, frais d'un événement futur.

### 5.2 Calcul et règles

- Même calcul que pour les produits (`DAILY` par défaut, `MONTHLY` pour les loyers alignés sur le mois) ; besoin de `476` par ligne ; ajustement du solde en fin de période : D `476` ; C compte de charge, ou l'inverse.
- Sous le seuil de signification, la charge reste dans la période de la pièce.
- La section analytique de la ligne de charge est conservée : la charge reconnue chaque mois va sur la bonne section, ce qui lisse le coût par espace (le loyer trimestriel devient un coût mensuel).
- Le budget (lot F16) est consommé à l'engagement et à la facture comme prévu ; le **réalisé par période** utilise la charge reconnue, avec les deux vues disponibles ([analytique-et-budget.md](analytique-et-budget.md)).
- Un paiement d'avance **sans facture** reste en `409` (avance fournisseur) ; il n'y a pas de charge constatée d'avance sans charge.

---

## 6. Produits à recevoir et charges à payer

Le pendant des constatations d'avance, pour que chaque période porte tout son produit et toute sa charge.

### 6.1 Produits à recevoir (factures à établir)

| Cas | Source | Montant |
| --- | --- | --- |
| Dépassements consommés, pas encore facturés | `SubscriptionOverageCharge` non facturés | Montant calculé par la plateforme |
| Réservations rendues, facturées à terme échu (entreprises facturées au mois) | Réservations terminées sans facture | Prix de la réservation |
| Domiciliation ou services facturés à terme échu | Contrats dont la période est rendue et non facturée | Prorata du cycle |
| Autres (refacturations à venir) | Saisie avec pièce | Estimation |

À la fin de la période : D `4181` ; C `706` (HT ; la TVA n'est pas constatée avant la facture, sauf avis contraire de l'expert-comptable). L'écriture est **extournée** au premier jour de la période suivante, et la vraie facture s'écrit normalement. Le `4181` se lettre par référence.

### 6.2 Charges à payer (factures non parvenues)

| Cas | Source | Montant |
| --- | --- | --- |
| Commande réceptionnée non facturée | Bon de commande réceptionné (lot F13) | Montant de la commande |
| Contrat récurrent sans facture du mois (électricité, eau, ménage, gardiennage, internet) | Fournisseurs récurrents : contrat ou moyenne des 3 dernières factures | Proposé, modifiable |
| Honoraires (expert-comptable, commissaire aux comptes) de l'exercice | Lettre de mission | Prorata |
| Personnel, charges sociales, impôts | Import de paie ([financement-engagements-paie.md](financement-engagements-paie.md#4-import-du-journal-de-paie)), déclarations | Calculé |
| Intérêts courus | Volet financement | Calculé |

À la fin de la période : D charge (avec sa section) ; D `4452` ou `4454` si la TVA est connue (paramètre) ; C `408`. Extourne au premier jour de la période suivante. Une charge à payer non soldée par une facture après N jours est signalée.

---

## 7. Traitement de fin de période

- **Rythme** : mensuel par défaut (`accounting.revenue.deferral = MONTHLY`, `accounting.expense.deferral = MONTHLY`) ; `YEAR_END` (à la clôture seulement, extourne à l'ouverture) et `NONE` disponibles. Le traitement tourne le premier jour du mois (heure paramétrable) pour le mois écoulé, et à la demande.
- **Pièces** : une pièce par nature (produits constatés d'avance, charges constatées d'avance, produits à recevoir, charges à payer), dans `OD` (ou `EXT` pour les extournes), avec le détail par ligne en annexe, ventilée par compte de produit ou de charge et par section analytique.
- **Simulation** avant comptabilisation : besoin par ligne, solde actuel, différence ; filtres par activité, site, client, fournisseur.
- **Idempotence** : le calcul repart toujours des échéanciers ; relancer la même période produit une différence nulle.
- **Période verrouillée** : une facture émise après le verrouillage, ou un avoir sur une période close, est pris en compte dans la première période ouverte, avec mention. Les périodes closes ne sont jamais modifiées.
- **Reprise** : à la bascule, les échéanciers sont créés pour toutes les lignes de 2026 encore en cours (abonnements, pass, domiciliations), à partir des dates des modules ; le premier traitement constitue le `477` d'ouverture de la reprise (décision 6).

---

## 8. Effets sur les autres modules

| Module | Effet |
| --- | --- |
| Facturation | Champs de période sur les lignes, affichés sur la facture (« Période : du 15/10/2026 au 14/10/2027 »), ce qui est aussi une bonne pratique pour le client |
| Abonnements, pass, domiciliation, réservations, événements | Renseignent la période à la création de la ligne ; publient les événements de gel, résiliation, annulation, report, consommation de crédits |
| Analytics et reporting | Deux indicateurs distincts et nommés : **chiffre d'affaires facturé** (existant) et **chiffre d'affaires reconnu** (comptable) ; le tableau de bord financier utilise le reconnu |
| Analytique | Le produit et la charge reconnus vont sur la section de la ligne d'origine, mois par mois |
| Budget | Réalisé par période sur le reconnu, réalisé à la facture disponible |
| Créances douteuses | Indépendantes : une facture douteuse continue de s'étaler ; un avoir ou une perte arrête l'échéancier pour la part concernée ([provisions-et-creances-douteuses.md](provisions-et-creances-douteuses.md)) |
| Justification des soldes | Le détail du `477` et du `476` par ligne est le justificatif du dossier de clôture ([rapprochement-et-comptes-tiers.md](rapprochement-et-comptes-tiers.md#43-dossier-de-justification-des-soldes-de-clôture)) |

---

## 9. Écritures

Ajoutées au [catalogue des écritures](catalogue-ecritures.md#16-produits-et-charges-constatés-davance-rattachement).

| Événement | Journal | Écriture |
| --- | --- | --- |
| `REVENUE_DEFERRAL_ADJUSTED` : fin de période, produits | `OD` | Besoin en hausse : D `{PRODUIT}` ; C `477`. En baisse : D `477` ; C `{PRODUIT}`. Détail par ligne, ventilé par compte de produit et section |
| `EXPENSE_DEFERRAL_ADJUSTED` : fin de période, charges | `OD` | Besoin en hausse : D `476` ; C `{CHARGE}`. En baisse : D `{CHARGE}` ; C `476` |
| `ACCRUED_REVENUE_POSTED` : factures à établir | `OD` | D `4181` ; C `{PRODUIT}` HT |
| `ACCRUED_EXPENSE_POSTED` : factures non parvenues | `OD` | D `{CHARGE}` ; D `4452` ou `4454` (si TVA connue) ; C `408` |
| Extourne des produits à recevoir et des charges à payer | `EXT` ou `OD` | Contre-passation au premier jour de la période suivante |
| Résiliation sans remboursement | `OD` | La part non rendue sort du `477` vers le compte de produit de résiliation (paramétrable) au calcul suivant |
| Pass expiré non consommé | `OD` | La part non consommée sort du `477` vers le compte de produit du pass au calcul suivant |

---

## 10. États

| État | Contenu | Usage |
| --- | --- | --- |
| **Détail des produits constatés d'avance** | À une date, par ligne de facture : client, facture, activité, période, montant HT, reconnu, restant ; total égal au `477` | Justificatif du `477`, clôture, audit |
| **Détail des charges constatées d'avance** | Par ligne : fournisseur, pièce, nature, période, montant, restant ; total égal au `476` | Justificatif du `476` |
| **Échéancier du chiffre d'affaires à reconnaître** | Chiffre d'affaires déjà facturé à reconnaître mois par mois sur les 12 prochains mois, par activité et par site | Prévisionnel, direction |
| **Chiffre d'affaires facturé et reconnu** | Par mois, par activité, par site : facturé, reconnu, variation du `477` | Tableau de bord, budget |
| **Factures à établir, factures non parvenues** | Par référence : estimation, extourne, facture réelle, écart | Clôture, contrôle des estimations |
| **Crédits de pass non consommés** | Par pass : crédits restants, valeur, expiration | Suivi, reconnaissance à l'expiration |
| **Anomalies de période** | Lignes d'abonnement, de pass ou de domiciliation sans période ; échéanciers incohérents | Correction |

Exports PDF, Excel et CSV comme les autres états.

---

## 11. Contrôles et clôture

Contrôles quotidiens :

- solde du `477` = somme des restants des échéanciers de produits ; solde du `476` = somme des restants des échéanciers de charges ;
- ligne d'une catégorie qui exige une période, sans période ;
- échéancier d'une ligne dont la facture est annulée sans avoir ;
- produit à recevoir ou charge à payer non extourné, ou non lettré avec la facture réelle après N jours ;
- écart de plus de N % entre une charge à payer estimée et la facture réelle (qualité des estimations).

À la **clôture**, la liste de clôture exige :

1. le traitement de fin de période de chaque mois de l'exercice comptabilisé, y compris le dernier ;
2. un écart nul entre les échéanciers et les comptes `477` et `476` ;
3. la revue des factures non parvenues (relevé des commandes réceptionnées, contrats récurrents) et des factures à établir ;
4. la revue des anomalies de période.

---

## 12. Données

| Table | Colonnes principales |
| --- | --- |
| `billing_document_line` (colonnes ajoutées) | service_start, service_end, recognition_method, recognition_source, original_line_id |
| `supplier_invoice_line`, `expense_justification_line`, `cash_movement` (colonnes ajoutées) | service_start, service_end, recognition_method |
| Nature d'opération (colonnes ajoutées) | periodic, default_duration_months, default_method |
| `acc_deferral_schedule` | company_code, kind (`REVENUE`, `EXPENSE`), source_type, source_line_id, account_number, section, montant HT, start, end, method, version, status (`ACTIVE`, `PAUSED`, `ENDED`, `CANCELLED`) |
| `acc_deferral_schedule_event` | schedule_id, type (gel, décalage, résiliation, annulation, expiration, report), date, avant, après |
| `acc_usage_consumption` | schedule_id, date, crédits consommés, source (réservation, pointage) |
| `acc_deferral_run` | period, kind, statut (`PREVIEW`, `POSTED`), entry_number, total besoin, total précédent, différence |
| `acc_deferral_run_line` | run_id, schedule_id, besoin, précédent, différence |
| `acc_accrual` | kind (`ACCRUED_REVENUE`, `ACCRUED_EXPENSE`), source_type, référence, tiers, compte, section, montant estimé, entry_number, reversal_entry_number, lettrage avec la facture réelle, écart |

---

## 13. Routes

| Route | Usage |
| --- | --- |
| `GET /accounting/deferrals?kind=&date=` | Détail des constatations d'avance à une date |
| `GET /accounting/deferrals/schedules/{id}` | Échéancier d'une ligne, versions et événements |
| `PATCH /billing/documents/{number}/lines/{lineId}/service-period` | Corriger la période d'une ligne (droit, motif) |
| `GET /accounting/deferral-runs/preview?period=&kind=` | Simulation de fin de période |
| `POST /accounting/deferral-runs` | Comptabiliser (sinon automatique) |
| `GET`, `POST /accounting/accruals` | Factures à établir et non parvenues : propositions, estimations, validation |
| `GET /accounting/deferrals/reports/{report}` | États de la section 10 |

---

## 14. Droits

| Permission | Pour |
| --- | --- |
| `ACCOUNTING_DEFERRAL:READ` | Consulter les échéanciers et les états |
| `ACCOUNTING_DEFERRAL:RUN` | Lancer, relancer, comptabiliser le traitement de fin de période (comptable) |
| `ACCOUNTING_DEFERRAL:EDIT_PERIOD` | Corriger la période d'une ligne, avec motif (comptable ; facturation pour les lignes libres) |
| `ACCOUNTING_ACCRUAL:MANAGE` | Valider ou ajuster les factures à établir et non parvenues (comptable) |

---

## 15. Paramétrage

| Paramètre | Valeur initiale |
| --- | --- |
| Rythme des produits constatés d'avance | `MONTHLY` (décision 5) ; `YEAR_END`, `NONE` disponibles |
| Rythme des charges constatées d'avance | `MONTHLY` ; `YEAR_END`, `NONE` disponibles |
| Méthode par source de ligne | Tableau de la section 3.2 |
| Base du prorata | Jours réels |
| Seuil de signification des charges | 50 000 XAF HT par ligne |
| Natures périodiques et durée par défaut | Section 5.1 |
| Reconnaissance pendant une suspension pour impayé | Oui (non disponible si le contrat le prévoit) |
| Crédits de pass non consommés | Reconnus à l'expiration ; taux de non-utilisation anticipé désactivé |
| Compte du produit de résiliation | Compte de l'activité d'origine |
| Factures à établir et non parvenues | Activées ; sources actives (dépassements, réservations à terme échu, commandes réceptionnées, contrats récurrents) ; TVA sur les charges à payer : non |
| Jour et heure du traitement | 1er du mois, 04 h 00 |
| Délai d'alerte d'une estimation non soldée | 60 jours |

---

## 16. Lots et critères d'acceptation

Le volet devient un lot à part, **F20**. Le lot F12 garde les contrôles de clôture, les à-nouveaux et les états financiers, et s'appelle désormais « Clôture ». La partie produits peut partir dès que F4 est livré. La partie charges attend F13.

| Lot | Contenu | Dépend de | Critère d'acceptation |
| --- | --- | --- | --- |
| **F20. Constatations d'avance et rattachement** | Période de service sur les lignes de facture, renseignée par les abonnements, pass, domiciliations, réservations et événements ; échéanciers et leurs événements (gel, résiliation, annulation, expiration, report) ; consommation des pass à crédits ; période sur les lignes de charge et natures périodiques ; traitement de fin de période par ajustement du solde ; factures à établir et non parvenues avec extourne ; états, contrôles, reprise | F4 (produits) ; F13, F5, F6 (charges) ; F9 (sections) | L'exemple de l'abonnement annuel de la section 4.2 donne 943 562 XAF en `477` au 31 décembre 2026 ; relancer le traitement d'un mois ne produit aucune écriture ; un gel de deux semaines décale la fin de l'échéancier de deux semaines ; un avoir de résiliation remet le `477` de la ligne à zéro ; le `477` égale toujours le détail par ligne, et la clôture est refusée sinon |
