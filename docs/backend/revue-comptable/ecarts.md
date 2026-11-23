# Écarts relevés : revue du module comptable (23/11/2026)

Chemins relatifs à `src/main/java/com/sni/bokaticowork/features/accounting/` sauf mention. `impl/` = `service/implementation/`. Lignes au commit `7c6b8491` (new-main). La dernière colonne dit dans quel lot l'écart est réglé.

## A. Écritures automatiques

| ID | Constat | Où | Effet concret | Lot |
|---|---|---|---|---|
| A1 | L'échéance de la ligne client n'est jamais renseignée : les schémas écrivent `"dueDate":"dueDate"` sans accolades, la date littérale est illisible et devient nulle sans erreur | `db/migration/V299`, `V330` ; `support/PostingTemplate.java:196, 422-430` | Toutes les lignes 411 sans échéance : balance âgée, justification, contrôles de clôture faussés | K0 · Réglé |
| A2 | Le 165 (cautions reçues) est un compte de tiers, mais trois sources l'écrivent sans tiers : ligne de caution d'une facture, mouvement de caution, portefeuille avec contrepartie 165 | `V296` (165 CUSTOMER) ; `impl/SalesAccountingService.java:598, 671` ; `V343:62` | Toute facture avec caution et tout mouvement de caution part en attente d'équilibrage | K0 · Réglé |
| A3 | Le 4718 sert à la fois de compte d'équilibrage à régulariser et de compte par défaut des débours | `support/AccountingSettings.java:43` ; `impl/SalesAccountingService.java:684` | Les débours se mélangent aux écarts à solder avant clôture ; le contrôle de clôture du compte d'attente devient inutilisable | K0 · Réglé |
| A4 | Un avoir sur une facture d'acompte débite le produit (706) au lieu du 4191 ; l'annulation d'un acompte partiellement réglé contre-passe tout le TTC | `impl/SalesAccountingService.java:686-691` ; `billing/.../DepositService.java:161, 222, 259` | Acompte de 100 000 remboursé au portefeuille : 4191 à 200 000 pour 100 000 réels, 706 négatif de 100 000 | K0 · Réglé |
| A5 | Un avoir imputé sur une autre facture ne produit aucune écriture (l'événement `CREDIT_NOTE_APPLIED_TO_INVOICE` n'est lu par aucun flux comptable) ; aggravé par l'imputation sur facture d'entreprise du 20/11 | `billing/.../BillingDocumentServiceImpl.java:2912-2953` | L'avoir reste au crédit du 411 du client A, la facture de B reste ouverte au 411 alors que la facturation la donne soldée | K0 · Réglé |
| A6 | Régime des encaissements : une affectation tardive ou un transfert de créance ne libère pas la TVA en attente (4438) ; l'annulation d'affectation ne la remet pas | `impl/SalesAccountingService.java:395-410` ; `V345:9`, `V358` | TVA due sous-déclarée, 4438 qui gonfle | K0 · Réglé |
| A7 | Facture à plusieurs taux : un seul code TVA (le premier taux trouvé) pour toute la base taxable | `impl/SalesAccountingService.java:813-816` | Déclaration de TVA fausse par taux | K0 · Réglé |
| A8 | Tout écart d'arrondi ou d'en-tête incohérent part en remise 709, sans échec, parfois au crédit | `impl/SalesAccountingService.java:263, 292, 883` | 709 pollué, erreurs de facturation masquées | K0 · Réglé |
| A9 | Paiement en espèces sans session de caisse retrouvée : repli silencieux sur 4719 au lieu du 571 | `impl/SalesAccountingService.java:626` | Espèces dans « encaissements à identifier » sans alerte | K0 · Réglé |
| A10 | Facture à montant nul : moins de deux lignes, l'événement passe en échec (le catalogue prévoyait « aucune écriture ») | `impl/SalesAccountingService.java:264` ; `support/PostingTemplate.java:214` | Échecs parasites dans la file | K0 · Réglé |
| A11 | Décisions sur écart de caisse passées dans le journal OD sans validation automatique : l'événement est « comptabilisé », la pièce reste en brouillard | `V296` (OD auto_validate faux) ; `V298` | Écarts de caisse hors comptabilité tant que personne ne valide les brouillards | K0 · Réglé |
| A12 | Justification de dépense : pas de tiers transmis pour la nature ; TVA déductible sans code ni base (caisse et justification) | `fund/.../FundRequestService.java:782` ; `V298`, `V300` | Pièce en attente si la nature est un compte de tiers ; TVA déductible absente de la déclaration par taux | K0 · Réglé |
| A13 | Aucune contre-passation prévue pour l'imputation d'acompte quand la facture finale est annulée, le transfert de créance, le mouvement de caution annulé | `impl/SalesAccountingService.java:345, 364, 391` | Écritures orphelines après annulation | K1 · Réglé (acompte et transfert ; la facturation n'annule pas un mouvement de caution : rien à contre-passer) |
| A14 | Remboursement d'un trop-perçu : rouvre d'abord les affectations de la transaction au lieu de débiter le 4719 ; deux remboursements partiels rouvrent deux fois la même affectation | `impl/SalesAccountingService.java:507-538` ; `payment/.../PaymentAllocationService.java:63-72` | Facture rouverte à tort, 4719 qui ne se solde pas | K1 · Réglé |

## M. Moteur d'imputation

| ID | Constat | Où | Effet concret | Lot |
|---|---|---|---|---|
| M1 | Une pièce automatique en attente, complétée et validée à la main, laisse l'événement en `UNBALANCED` | `impl/PostingEngine.java` (seul à changer le statut) ; `impl/AccountingEntryService.java` `update`, `validate` | Contre-passation qui attend indéfiniment, paiements en régime des encaissements bloqués, facture vue « non comptabilisée » | K0 · Réglé |
| M2 | Blocage de la file : les 50 premiers événements en attente sont lus par date ; familles coupées et contre-passations qui attendent leur origine restent en tête sans être écartées | `impl/PostingEngine.java:137, 144, 321` | 50 cas de ce genre suffisent à arrêter toute la comptabilisation | K0 · Réglé |
| M3 | Même blocage côté flux des ventes (tri par date avec limite, fait en erreur relu à chaque passage) ; le flux de caisse, lui, ne relance jamais un échec | `impl/SalesAccountingService.java:895` ; `impl/CashAccountingService.java:141` | Taux de change manquant ou facture non émise : tout le flux s'arrête | K0 · Réglé |
| M4 | La contre-passation automatique ne suit pas la chronologie stricte et échoue dès qu'une pièce plus récente existe dans le journal | `impl/PostingEngine.java:340-352` ; `impl/AccountingEntryService.java:550-557` | Facture annulée le 10/04 avec une pièce VT du 12/04 : échec, sans remise en file | K0 · Réglé |
| M5 | Un fait arrivé en retard est redaté à la dernière date du journal, sans limite de mois | `impl/PostingEngine.java:279-287` | Facture du 31/03 passée au 02/04 : chiffre d'affaires et TVA d'avril | K1 · Réglé |
| M6 | Corriger la donnée source ne corrige pas l'écriture : un événement connu n'est jamais relu, la régénération reprend l'ancien contenu | `impl/PostingEngine.java:110-113` | Après correction d'une facture ou d'un paiement, l'erreur se reproduit | K0 · Réglé |
| M7 | Champ absent = montant nul sans erreur ; dans une répétition, un champ absent est pris dans l'événement entier ; `a-b` sans espaces vaut 0 ; argument de résolveur absent pris au pied de la lettre | `support/PostingTemplate.java:274, 294, 319-320, 373-379` | Ligne disparue, différence passée au 4718 si la politique est le compte d'attente sans plafond | K0 · Réglé en partie (résolveur : voir lot-k0.md) |

## S. Saisie manuelle et correction des pièces mal placées

| ID | Constat | Où | Effet concret | Lot |
|---|---|---|---|---|
| S1 | Contre-passer à la main une pièce automatique laisse l'événement « comptabilisé » : la régénération est refusée et le module source n'est pas prévenu | `impl/AccountingEntryService.java:511-575` ; `impl/PostingEngine.java:168-173` | Après contre-passation, aucune voie pour reproduire l'écriture juste : il faut tout ressaisir à la main | K1 · Réglé |
| S2 | Pas d'opération « corriger » : une pièce mal placée se contre-passe en entier puis se ressaisit, sans lien entre l'erreur, l'annulation et la pièce juste ; pas de reclassement d'une seule ligne (mauvais compte, mauvais tiers) | `impl/AccountingEntryService.java` | Deux pièces de plus pour changer un compte, aucune traçabilité de la correction | K1 · Réglé |
| S3 | Supprimer un brouillard ne vérifie pas son origine : un brouillard produit par un événement se supprime, l'événement garde un numéro qui ne mène plus nulle part | `impl/AccountingEntryService.java:225-232` | Fait métier perdu pour la comptabilité | K1 · Réglé |
| S4 | Modifier un brouillard ou une pièce automatique en attente change librement journal, date et lignes, sans trace de l'écart avec ce que le schéma avait produit | `impl/AccountingEntryService.java:196-224` | Impossible de savoir ce qui a été corrigé à la main | K1 · Réglé |
| S5 | Contre-passation et régularisation datées avec `LocalDate.now()` sans fuseau (serveur en UTC) | `impl/AccountingEntryService.java:519, 584, 590` | Entre 23 h et minuit à Brazzaville, la pièce prend la date de la veille | K1 · Réglé |
| S6 | La régularisation d'une pièce acceptée déséquilibrée est toujours datée du jour, sans choix de date ni de période | `impl/AccountingEntryService.java:577-625` | Régularisation tombée dans la mauvaise période | K1 · Réglé |
| S7 | Rien n'empêche de saisir sur un compte qui a des sous-comptes (compte collectif ou de regroupement) | `impl/AccountingEntryService.java:409-450` | Soldes éclatés entre 411 et 4111, lettrage et justification incomplets | K2 |
| S8 | Aucun avertissement non bloquant : sens anormal (charge au crédit, produit au débit, caisse créditrice), ligne de TVA sans code ou dont le montant ne correspond pas à la base, saisie directe sur 471 ou 4718, doublon probable (même journal, date, montant, libellé) | `impl/AccountingEntryService.java:409-450` | L'erreur ne se voit qu'à la clôture | K2 |
| S9 | Aucune aide à la saisie : pas de contrepartie proposée selon le journal, pas de TVA calculée depuis le code, pas d'aperçu de la ventilation analytique avant validation | `controller/AccountingController.java` | Saisie entièrement à la charge du comptable | K2 |
| S10 | Messages sans accents et sans action proposée (sauf `CHRONOLOGIE_STRICTE`) ; refus de chronologie de la contre-passation sans date proposée | `impl/AccountingEntryService.java` passim, `:550-557` | Le frontend ne peut pas guider l'utilisateur | K2 |
| S11 | Seconde validation au-delà du seuil appliquée à la validation seulement : contre-passation et régularisation y échappent | `impl/AccountingEntryService.java:277-292` | Une seule personne annule une pièce de plusieurs millions | K1 · Réglé |

## P. Propositions du module

| ID | Constat | Où | Effet concret | Lot |
|---|---|---|---|---|
| P1 | Créances douteuses : un rejet ne tient qu'une nuit (seules les propositions en cours ou reportées sont vues) | `impl/ReceivableRiskService.java:172-175` | La même proposition revient chaque nuit | K3 |
| P2 | Au-delà de 365 jours, classement d'office par le système, même si un comptable avait rejeté, sans notification | `impl/ReceivableRiskService.java:176-178` | Décision humaine écrasée | K3 |
| P3 | La contagion aux autres factures du client ignore les reports | `impl/ReceivableRiskService.java:322` | Proposition sur une facture volontairement reportée | K3 |
| P4 | Proposition jamais invalidée quand la facture est payée ; elle bloque la clôture et son acceptation échoue | `impl/ReceivableRiskService.java:262, 302-306, 843-846` | Clôture bloquée par des propositions mortes | K3 |
| P5 | Montant suggéré calculé sans les garanties (alors que la passe mensuelle les déduit), figé, jamais utilisé | `impl/ReceivableRiskService.java:910-912, 438-441` | Chiffre affiché faux | K3 |
| P6 | Les avances clients (4191) sont déduites comme garantie au lieu d'être proposées en imputation sur la facture | `impl/ReceivableRiskService.java:940-951` | Créance dépréciée à tort, avance jamais imputée | K3 |
| P7 | Motif affiché en code brut, entité renvoyée telle quelle, pas de nom de client ni d'explication | `controller/ProvisionController.java:64` | Écran illisible | K3 |
| P8 | Rapprochement bancaire : une combinaison de plusieurs écritures reçoit une confiance fixe de 90 et est pointée d'office, sans vérifier qu'elle est unique | `impl/BankService.java:404, 464-474` | Pointage faux sans humain | K3 |
| P9 | Proposition au montant (confiance 60) qui ne réserve pas la ligne comptable, candidat pris au hasard ; propositions jamais enregistrées, impossibles à écarter | `impl/BankService.java:450-459` | La même écriture proposée pour plusieurs lignes de relevé | K3 |
| P10 | La règle d'affectation d'une ligne de relevé n'est pas montrée avant comptabilisation | `impl/BankService.java:616, 657-661, 872-876` | Contrepartie choisie sans aperçu | K3 |
| P11 | Lettrage : paires 1 pour 1 au montant exact seulement, compte exact (pas les sous-comptes), sans N pour 1, sans proximité de date, sans rejet possible ; pas de proposition 4191 contre 411 ni 4091 contre 401 | `impl/AccountingLetteringService.java:63, 282-310` | Propositions pauvres et répétées | K3 |
| P12 | Factures à établir et non parvenues : ni rejet ni modification avant passage, reproposées chaque mois, non parvenues créées sans vérifier la réception, facture à établir sans TVA, TVA des non parvenues sur 4454 | `impl/DeferralService.java:728-812` ; `controller/DeferralController.java:84-105` | Charges et produits estimés faux ou en double | K3 |
| P13 | Achat imputé en charge au-dessus du seuil d'immobilisation jamais signalé ; facture mixte : la part immobilisation part en 401 au lieu de 481 | `impl/PurchaseService.java:530` ; `impl/FixedAssetService.java:1397` | Immobilisations passées en charges | K3 |
| P14 | Dotation aux amortissements d'une période verrouillée rattrapée sur la suivante, éventuellement l'exercice suivant, sans notification | `impl/DepreciationRunService.java:81-90` | Indépendance des exercices non respectée | K3 |
| P15 | Biens manquants à l'inventaire comptés mais aucune sortie proposée | `impl/FixedAssetInventoryService.java:242` | Actif surévalué | K3 |
| P16 | Budget : seuils sur l'annuel seulement (la mensualisation existe), pas de nouvelle alerte après virement de crédits, dépenses non budgétées non notifiées | `impl/BudgetEngine.java:157-159` ; `impl/BudgetReportService.java:148-185` | Dépassement vu trop tard | K3 |
| P17 | Résultat fiscal : lignes automatiques non contestables, double réintégration si parent et enfant sont marqués non déductibles | `impl/TaxResultService.java:91-128, 98-108` | Résultat fiscal surévalué | K3 |
| P18 | Courriels d'anomalie renvoyés chaque jour sans déduplication ; notifications par courriel seulement, liste vide par défaut, rien dans l'application | `worker/DeferralWorker.java:82-87`, `ProvisionWorker.java:83-90`, `FixedAssetWorker.java:92-99` ; `support/AccountingNotifier.java:48-55` | Alertes ignorées ou jamais reçues | K4 |

## R. Recommandations et contrôles

| ID | Constat | Où | Effet concret | Lot |
|---|---|---|---|---|
| R1 | La liste de clôture ne garde que les constats bloquants : les avertissements des immobilisations, provisions et constatations d'avance n'apparaissent jamais | `impl/ClosingService.java:319-322` | Points à traiter invisibles | K4 |
| R2 | Une étape levée n'est jamais réévaluée ; une seule personne lève une étape obligatoire | `impl/ClosingService.java:170-183, 302-304` | Dérogation définitive même si la situation empire | K4 |
| R3 | Compte d'attente contrôlé en solde net par préfixe | `impl/ClosingService.java:229-238` | 4711 et 4712 qui se compensent passent | K4 |
| R4 | Soldes anormaux par tiers (client créditeur, fournisseur débiteur) calculés mais absents des problèmes ; reclassés en silence dans les états | `impl/BalanceJustificationService.java:207, 218-230` ; `impl/FinancialStatementService.java:291` | Aucune écriture de reclassement proposée | K4 |
| R5 | Pas d'étape TVA ni impôt dans la liste de clôture ; pas d'échéance de déclaration de TVA | `impl/ClosingService.java:112-132` | Déclaration oubliée | K4 |
| R6 | Contrôles permanents absents : 471 et 4718 non soldés en cours d'année avec ancienneté, caisse 57 ou banque 52 créditrice, sens anormal par nature, 4191 et 4091 à imputer, extournes non passées, doublons de factures fournisseurs, paie déjà importée ou en forte variation, écarts de conversion de fin d'exercice | (absent) | Anomalies découvertes à la clôture | K4 |
| R7 | Ouvrir la liste de clôture écrit en base avec un simple droit de lecture | `impl/ClosingService.java:143-148` | Effet de bord à la lecture | K4 |
| R8 | Alertes hors bilan envoyées seulement à J-30, J-7 et J-1 exacts | `impl/CommitmentService.java:166` | Un jour sans tâche et l'alerte est perdue | K4 |
| R9 | Alertes budgétaires : la tâche passe l'année civile comme code d'exercice (à vérifier) | `worker/AccountingWorker.java:74` | Aucune alerte si l'exercice ne s'appelle pas « 2026 » | K4 |

## N. Analytique

| ID | Constat | Où | Effet concret | Lot |
|---|---|---|---|---|
| N1 | La contre-passation ne reprend pas la ventilation ni le reflet en classe 9, alors que les états comptent la pièce d'origine | `impl/AccountingEntryService.java:511-575` ; `impl/AnalyticReportService.java:24-25` | Charge de 500 000 sur BUREAUX contre-passée : toujours 500 000 dans l'analytique et au 93 | K5 |
| N2 | Même défaut pour les annulations automatiques, le délettrage (écart 658/758) et le brouillard inverse d'une reprise, qui est en plus reventilé par défaut | `impl/PostingEngine.java:353` ; `impl/AccountingLetteringService.java:335` ; `impl/AccountingEntryService.java:162-170` | Facture annulée qui garde son produit dans l'analytique ; COWORK +X, STRUCTURE −X | K5 |
| N3 | La régularisation ne ventile pas | `impl/AccountingEntryService.java:577-625` | Complément absent de l'analytique et de la classe 9 | K5 |
| N4 | Réalisé budgétaire par section : montant entier de la ligne au lieu de la part ventilée, axe non précisé, contre-passations exclues | `impl/BudgetEngine.java:138-157` | 1 000 000 à 30 % sur SALLES compte 1 000 000 au lieu de 300 000 | K5 |
| N5 | Le reflet en classe 9 ne peut pas être coupé : un réglage vide est remplacé par la valeur par défaut | `support/AccountingSettings.java:117-120` ; `impl/AnalyticAllocator.java:207, 232` ; `impl/AnalyticService.java:416` | Contraire au réglage et à la doc | K5 |
| N6 | Fusion avec retraitement et répartition secondaire sans pièce en classe 9 | `impl/AnalyticService.java:275-279, 445-487` | Classe 9 qui ne se rapproche plus des états | K5 |
| N7 | Règles par tiers comparées au tiers de la ligne de charge (vide) au lieu de celui de la pièce ; aucun critère ressource, site ou produit | `impl/AnalyticAllocator.java:148` | Règle « fournisseur F001 vers ENTRETIEN » jamais appliquée | K5 |
| N8 | Presque rien n'arrive des faits métier : aucune règle pour l'axe ACTIVITE, les ventes n'envoient pas d'activité | `V302:145, 154` | Tout va sur STRUCTURE, résultat par activité vide | K5 |
| N9 | Le contrôle « axe obligatoire » ne se déclenche jamais : la section par défaut est appliquée avant | `impl/AnalyticAllocator.java:97-100, 179-182` | Saisie sans ventilation acceptée en silence | K5 |
| N10 | Début de validité d'une section non vérifié | `impl/AnalyticAllocator.java:191` | Section future acceptée | K5 |
| N11 | Une règle peut désigner une clé d'un autre axe ; pourcentages négatifs acceptés ; parts arrondies à 4 décimales en XAF | `impl/AnalyticService.java:313` ; `impl/AnalyticAllocator.java:117-123` | Imputation fausse sans avertissement, francs fractionnaires | K5 |
| N12 | Analyse d'un poste par site ou centre : montant entier de la ligne au lieu de la part | `impl/AnalyticReportService.java:216-219` | LOYER à 60 % montre 100 % du site | K5 |
| N13 | Contrôle de rapprochement biaisé (montants sans signe, périmètres différents) ; aucun rapprochement signé entre le résultat 6/7 et chaque axe ni entre le 93 et les ventilations | `impl/AnalyticReportService.java:192-204` ; `impl/ClosingService.java:264-278` | Fausses alertes, vraies erreurs invisibles | K5 |
| N14 | Répartition secondaire : seulement les pièces validées, seulement les charges, pas de cascade, retenue seulement si la période est entière dans l'intervalle | `impl/AnalyticService.java:464` ; `impl/AnalyticReportService.java:49-50` | État du 01 au 15/03 sans la répartition de mars | K5 |
| N15 | Le reclassement analytique modifie une période close ou une pièce contre-passée | `impl/AnalyticService.java:383-436` | États d'un mois clos changés après coup | K5 |
| N16 | Section fermée sur un écart de lettrage, une paie ou un emprunt : toute l'opération échoue | `impl/AccountingLetteringService.java:214-219` ; `impl/LoanService.java:547-550` | Lettrage ou paie refusés pour une question analytique | K5 |
| N17 | Correspondance de comptes initiale : 624 va toujours sur NETTOYAGE, 605 capte 6053 à 6058 | `V302:207-210` | Fournitures d'entretien sur CAFE | K5 |
| N18 | Aucune route pour voir la ventilation proposée avant validation ; pas d'état des montants restés sur la section par défaut ; pas de reprise après activation | `controller/AnalyticController.java` | Ventilation découverte après coup | K5 |

## X. Points de norme à arbitrer (expert-comptable)

| ID | Question | Pratique actuelle | Lot |
|---|---|---|---|
| X1 | TVA collectée sur prestations de services | 4431 (ventes) ; 4432 existe mais n'est pas utilisé | K6 |
| X2 | Centime additionnel | Au 4431 avec la TVA | K6 |
| X3 | Mobile money et cartes | 5212/5213 sous 521 banques ; cartes en 5851 ; le SYSCOHADA révisé prévoit 55 et 515 | K6 |
| X4 | Frais de transfert du portefeuille, cashback et crédits promotionnels | 706 sans TVA ; 658 | K6 |
| X5 | TVA déductible | 4452 en caisse même pour un service ; 4454 pour les non parvenues | K6 |
| X6 | Écarts de conversion latents sur créances en devise | Aucun (478/479) ; seulement 676/776 au lettrage | K6 |
| X7 | Report des déficits fiscaux | Sans limite de durée | K6 |
| X8 | Reprises (79) et transferts de charges (78) en analytique | Ventilés comme des produits | K6 |
