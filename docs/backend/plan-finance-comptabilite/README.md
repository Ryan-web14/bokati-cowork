# Plan finance : trésorerie, caisses et comptabilité SYSCOHADA

Plan directeur du volet finance de Bokati Cowork. Il couvre la refonte des caisses et des demandes de fonds, les pièces comptables émises par le logiciel, la justification des dépenses, et la création d'un module de comptabilité générale et analytique conforme au SYSCOHADA révisé. Avec ce module, **toute transaction de la plateforme produit une écriture comptable**.

Rédigé d'après le code au 11/11/2026 (branche `feature/reservation-places`, commit `c6089dc`).

Fichiers du dossier :

| Fichier | Contenu |
| --- | --- |
| [README.md](README.md) | Ce plan : existant, cible, modules, données, droits, découpage en lots, décisions prises, paramétrage. |
| [catalogue-ecritures.md](catalogue-ecritures.md) | Pour chaque événement de la plateforme, l'écriture produite (journal, comptes au débit et au crédit, analytique). |
| [etats-financiers.md](etats-financiers.md) | Livres, balances, relevés de compte des tiers, bilan, compte de résultat, tableau des flux, notes annexes, liasse DSF, états d'inventaire, fiscaux, de trésorerie et de gestion ; correspondance comptes vers postes. |
| [analytique-et-budget.md](analytique-et-budget.md) | Comptabilité analytique, analyse par poste de dépense, gestion budgétaire complète et contrôle budgétaire dans les sorties de fonds et les autres opérations. |
| [financement-engagements-paie.md](financement-engagements-paie.md) | Emprunts et tableaux d'amortissement, comptes courants d'associés, registre des engagements hors bilan, import du journal de paie. |
| [caisses-et-sessions.md](caisses-et-sessions.md) | Le système de caisse complet : défauts de l'existant, référentiel des caisses, cycle de vie des sessions, titulaire et remplaçants, congés et absences des caissiers, transfert de responsabilité, comptages et billetage, catalogue des opérations, transferts à deux temps, corrections, plafonds et contrôles, états, données, routes, droits, saisie minimale (champs déduits, générés, listes), lots C0 à C6. |
| [pieces-comptables.md](pieces-comptables.md) | Les pièces émises (bon d'entrée, bon de sortie, bon de décaissement, état de besoin, fiche de justification, procès-verbaux...) : champs, champs automatiques, signatures, numérotation. |
| [demandes-et-justification.md](demandes-et-justification.md) | Le nouveau circuit des demandes de fonds (état de besoin, demande, validation, décaissement) et la justification des dépenses une par une. |

---

## Sommaire

1. [Principes directeurs](#1-principes-directeurs)
2. [Existant : ce qu'on garde, ce qu'on améliore](#2-existant--ce-quon-garde-ce-quon-améliore)
3. [Architecture cible](#3-architecture-cible)
4. [Module comptabilité SYSCOHADA](#4-module-comptabilité-syscohada)
5. [Comptabilité analytique et budget](#5-comptabilité-analytique-et-budget)
6. [Trésorerie et caisses](#6-trésorerie-et-caisses)
7. [Mouvements de caisse et choix du journal par la caissière](#7-mouvements-de-caisse-et-choix-du-journal-par-la-caissière)
8. [Sorties et entrées de fonds : typologie](#8-sorties-et-entrées-de-fonds--typologie)
9. [Demandes, état de besoin, justification](#9-demandes-état-de-besoin-justification)
10. [Pièces comptables](#10-pièces-comptables)
11. [Écritures automatiques de la plateforme](#11-écritures-automatiques-de-la-plateforme)
12. [Fiscalité](#12-fiscalité)
13. [États, contrôles et exports](#13-états-contrôles-et-exports)
14. [Fonctionnalités à compléter, améliorer et prendre en compte](#14-fonctionnalités-à-compléter-améliorer-et-prendre-en-compte)
15. [Modèle de données](#15-modèle-de-données)
16. [Droits et séparation des tâches](#16-droits-et-séparation-des-tâches)
17. [Bascule et reprise de l'historique](#17-bascule-et-reprise-de-lhistorique)
18. [Découpage en lots](#18-découpage-en-lots)
19. [Décisions prises](#19-décisions-prises)
20. [Tout est paramétrable](#20-tout-est-paramétrable)

---

## 1. Principes directeurs

1. **Une seule source de vérité par fait.** Le fait métier (paiement, mouvement de caisse, facture) reste dans son module. L'écriture comptable en est la **traduction**, produite par le module comptable à partir d'un événement. Elle n'est jamais saisie une deuxième fois à la main.
2. **Rien ne s'efface.** Une écriture validée ne se modifie pas et ne se supprime pas : on la contre-passe. C'est déjà la règle des mouvements de caisse (contre-mouvement), des factures scellées (SEFC) et des écritures de portefeuille ; la comptabilité la reprend.
3. **Toute écriture a une pièce.** Chaque écriture pointe son justificatif : facture, reçu, bon, procès-verbal, scan. Une écriture sans pièce reste en brouillard et ne se valide pas, sauf les écritures d'inventaire passées par un comptable.
4. **Le paramétrage décide, pas le code.** Les comptes, journaux, schémas d'écriture, axes analytiques, seuils de validation et plafonds sont des données modifiables par un administrateur finance. Le code fournit des valeurs initiales conformes au SYSCOHADA.
5. **Asynchrone, idempotent, rattrapable.** Les écritures automatiques passent par l'outbox existante (`core/outbox`). Un échec comptable ne bloque jamais un encaissement : l'événement reste en file, il est visible dans un écran « écritures en échec », et on peut le rejouer. Une même opération ne produit jamais deux écritures (clé d'idempotence = type d'événement + référence).
6. **La caisse reste simple pour la caissière.** Elle choisit une nature d'opération en langage courant (« achat de fournitures », « avance pour déplacement »). Le logiciel en déduit le compte, le journal par défaut et l'analytique. Elle peut changer le journal, ou en créer un si elle en a le droit.
7. **Séparation des tâches.** Celui qui demande n'approuve pas, celui qui approuve ne décaisse pas, celui qui décaisse ne valide pas l'écriture. Le logiciel l'impose et l'administrateur peut l'assouplir, avec trace.
8. **Tout est paramétrable.** Comptes, journaux, schémas, natures, seuils, délais, formulaires, pièces, exports : rien n'est codé en dur (voir [section 20](#20-tout-est-paramétrable)).
9. **Le moins de saisie possible.** Tout champ que le système peut connaître est déduit (contexte, objet lié, paramétrage), généré (numéros, dates, libellés, totaux, montant en lettres) ou choisi dans une liste préenregistrée ; on ne saisit que ce que personne d'autre ne sait. Règles et inventaire champ par champ pour la caisse dans [caisses-et-sessions.md](caisses-et-sessions.md#21-saisie-minimale--champs-déduits-générés-et-listes) ; la même règle vaut pour les demandes, les justifications et les écritures.
10. **Conformité.** SYSCOHADA révisé (Acte uniforme OHADA relatif au droit comptable, 2017) : plan de comptes, système normal ou système minimal de trésorerie, états financiers annuels. Conservation des pièces et écritures 10 ans.

---

## 2. Existant : ce qu'on garde, ce qu'on améliore

### 2.1 Ce qui existe et reste

| Élément | Où | Constat |
| --- | --- | --- |
| Caisses (`cash_register`) | `features/payment` | Code, site, organisation (`business_entity_code`), plafond d'espèces, responsable, caisses automatiques `CSR-AUTO-WALLET` et `CSR-AUTO-MOBILE-MONEY` fermées à la saisie, restriction par moyen. |
| Sessions (`cash_session`) | idem | Ouverture avec fonds de caisse, clôture avec montant compté, écart, revue d'écart (`CLOSING_REVIEW`), clôture automatique de fin de mois avec report, rapport de clôture PDF envoyé par courriel. |
| Mouvements (`cash_movement`) | idem | Types `OPENING_FLOAT`, `PAYMENT`, `REFUND`, `CASH_IN`, `CASH_OUT`, `SAFE_DEPOSIT`, `TRANSFER_IN/OUT`, `ADJUSTMENT`, `CLOSING_COUNT`. Catégorie de flux, sous-catégorie, contrepartie, pièce, signature, impression, pièces jointes, solde courant, mouvement lié (contre-passation). |
| Bons d'entrée et de sortie | idem | `createEntryVoucher`, `createExitVoucher`, PDF `cash/movement-document.html`. |
| Demandes de caisse (`cash_request`) | idem | Types `JUSTIFICATIF`, `REMISE_DE_FONDS`, `CASH_ADVANCE`, `EXPENSE_REIMBURSEMENT` ; statuts `PENDING`, `APPROVED`, `REJECTED`, `EXECUTED`, `CANCELLED` ; PDF `cash/request-document.html`. |
| Anomalies de caisse | idem | Écart anormal, montants ronds, remboursements ou ajustements excessifs, session hors heures, fractionnement sous seuil, plafond souvent frôlé. |
| Statistiques de caisse | idem | Par caisse, session, caissier. |
| Encaissement au guichet | idem | Monnaie rendue, chèque, virement, mobile money hors plateforme, rattachement à la session (commit `c6089dc`). |
| Portefeuille | `payment/control`, `payment/transfer` | Grand livre du portefeuille, retenues, actions à double visa, rapprochement de trésorerie (`WalletTreasuryReconciliation`). |
| Facturation | `features/billing` | Factures, avoirs, devis, acomptes, échéanciers, escompte, taxes (`TaxRule`), clôture de période (`BillingPeriodClosure`), scellement SEFC, journal d'audit fiscal. |
| Comptabilité de stock | `inventory/stock` | `stock_journal_entry` avec compte de stock et compte de contrepartie, motifs d'ajustement liés à un compte (`V214`). C'est l'embryon d'imputation à généraliser. |
| Vérification de documents | `features/verify` | QR de vérification publique, réutilisable pour les pièces de caisse. |

### 2.2 Lacunes constatées

| Lacune | Conséquence |
| --- | --- |
| Pas de comptabilité générale | Rien ne produit de journal, de grand livre ni de balance. L'expert-comptable ressaisit tout à partir des exports. |
| `flowCategory` est un texte libre | Une même dépense se classe de plusieurs façons, sans compte comptable : impossible à agréger proprement. |
| Une demande de caisse est rattachée à une **session** de caissier (`submit(sessionNumber, ...)`) | Le demandeur, qui n'a pas de session, ne peut pas demander lui-même : la caissière saisit pour lui, et l'auteur réel se perd. |
| Approuver une demande l'exécute aussitôt | Pas de distinction entre « accordée » et « décaissée » ; le décaissement ne peut pas attendre que l'argent soit disponible ou que le bénéficiaire se présente. |
| Une demande ne porte qu'un montant et un motif | Pas de lignes, de nature de dépense, de fournisseur, de budget, d'analytique, de bénéficiaire distinct du demandeur. |
| Avance (`CASH_ADVANCE`) sans suivi de justification | On ne sait pas ce qui reste à justifier, par qui, depuis quand ; pas de restitution du reliquat ni de complément. |
| Pas d'état de besoin | Le besoin n'est pas exprimé ni chiffré avant la demande d'argent. |
| Pas de billetage | La clôture saisit un total, pas le détail par coupure ; pas de double comptage. |
| Acteurs saisis dans les requêtes, droits trop grossiers, solde courant faux, ajustement sans sens, transferts unilatéraux | Détail et correctifs (lot C0) dans [caisses-et-sessions.md](caisses-et-sessions.md#14-défauts-relevés). |
| Pas de passation entre caissiers | Une session est liée à un caissier ; une relève en cours de journée n'est pas tracée. |
| Comptes bancaires et mobile money absents | Les remises en banque (`SAFE_DEPOSIT`) sortent de la caisse sans arriver nulle part. |
| Chèques et virements tenus pour encaissés dès la saisie | Pas d'état « à remettre en banque », « remis », « rejeté ». |
| Frais PawaPay non constatés | Le net reçu de l'opérateur diffère du brut encaissé sans écriture de frais. |
| Abonnements facturés d'avance | Pas de produits constatés d'avance : le chiffre d'affaires est reconnu à la facture et non sur la période de service. |

---

## 3. Architecture cible

### 3.1 Nouveau module `features/accounting`

Organisation habituelle (`controller`, `dto`, `mapper`, `model`, `repository`, `service`), avec ces sous-domaines :

| Sous-domaine | Rôle |
| --- | --- |
| `chart` | Plan de comptes, classes, comptes de tiers, comptes de trésorerie. |
| `fiscalyear` | Exercices, périodes, clôtures, à-nouveaux. |
| `journal` | Journaux et leurs règles (type, compte de trésorerie lié, numérotation). |
| `entry` | Pièces et lignes d'écriture, brouillard, validation, contre-passation, lettrage. |
| `posting` | Moteur d'imputation : événement vers schéma d'écriture vers pièce. |
| `analytic` | Axes, sections, règles de ventilation. |
| `tax` | Comptes de taxe, déclarations, états de TVA. |
| `bank` | Comptes de trésorerie bancaires et mobile money, relevés, rapprochement. |
| `reporting` | Journal, grand livre, balance, balance âgée, états financiers, exports. |

Les modules existants (`payment`, `billing`, `inventory`, `booking`, `subscription`, `marketplace`) **ne dépendent pas** de `accounting`. Ils publient des événements ; `accounting` les consomme. Couper la comptabilité ne casse donc pas l'encaissement.

### 3.2 Chaîne d'une écriture automatique

```
Fait métier (transaction réussie, mouvement de caisse, facture émise...)
   │  même transaction SQL
   ▼
outbox_event  type=ACCOUNTING_EVENT  payload={eventType, reference, date, montants, tiers, analytique}
   │  worker outbox (10 s, lot de 25)
   ▼
PostingEngine
   1. idempotence : (eventType, reference) déjà comptabilisé ? alors rien
   2. schéma d'écriture actif pour eventType (+ conditions : moyen, devise, organisation)
   3. résolution des comptes (tiers 411xxx du client, compte de trésorerie de la caisse...)
   4. résolution de l'analytique (règles, valeurs du fait métier)
   5. période ouverte ? sinon première période ouverte suivante, avec mention
   6. pièce en BROUILLARD ou VALIDÉE selon le paramétrage du journal
   ▼
accounting_entry + accounting_entry_line (+ ventilations analytiques)
```

Un échec (compte manquant, période close, schéma absent) met l'événement en `FAILED` avec un motif lisible. L'écran « Écritures en attente » permet de corriger le paramétrage puis de rejouer.

### 3.3 Activation progressive

Paramètres `accounting.enabled`, `accounting.analytic.enabled`, `accounting.start-date` (date de bascule), plus un interrupteur par famille d'événements, pour monter en charge famille par famille (caisse d'abord, puis ventes, puis le reste).

---

## 4. Module comptabilité SYSCOHADA

### 4.1 Plan de comptes

- Chargé à l'installation avec le plan SYSCOHADA révisé : classes 1 à 9, comptes principaux et divisionnaires utiles à l'activité. L'administrateur finance l'étend, mais ne supprime pas un compte mouvementé : il le désactive.
- Champs : numéro, intitulé, classe, nature (bilan / gestion / hors activités ordinaires / analytique), sens normal, **lettrable** oui/non, **compte de tiers** (collectif client, fournisseur, personnel), **compte de trésorerie** (caisse, banque, mobile money), **analytique obligatoire** oui/non (par défaut oui pour les classes 6 et 7), taxe associée, actif oui/non.
- **Comptes auxiliaires de tiers** : un compte client par membre, client ou organisation (`411` + code), créé à la première écriture ; un compte fournisseur par fournisseur (`401` + code) ; un compte personnel par employé pour les avances (`421` + code).

Comptes de départ proposés pour un espace de coworking (valeurs par défaut retenues, ajustables par l'expert-comptable, voir [décision 2](#19-décisions-prises)) :

| Usage | Compte SYSCOHADA proposé |
| --- | --- |
| Caisse principale, caisses de guichet, petite caisse | `571` Caisse siège, divisionnaires `5711`, `5712`... (un par caisse) |
| Banques | `521` Banques locales, un divisionnaire par compte |
| Mobile money plateforme (compte PawaPay) | divisionnaire de trésorerie dédié, ex. `5214` ou `5781` selon l'avis de l'expert-comptable |
| Mobile money marchand (MTN MoMo Pay, Airtel Money) | un divisionnaire par numéro marchand |
| Virements internes de fonds (caisse vers banque, entre caisses) | `585` Virements de fonds |
| Chèques à encaisser | `513` Chèques à encaisser |
| Clients | `411` Clients (auxiliaires par client) |
| Avances reçues des clients, solde des portefeuilles électroniques | `4191` Clients, avances et acomptes reçus |
| Dépôts de garantie reçus (cautions) | `165` Dépôts et cautionnements reçus |
| Fournisseurs | `401` Fournisseurs (auxiliaires) |
| Personnel, avances et acomptes (avances à justifier) | `421` Personnel, avances et acomptes |
| TVA facturée | `4431` État, TVA facturée sur ventes |
| TVA récupérable | `4452` TVA récupérable sur achats, `4454` sur services extérieurs |
| Produits constatés d'avance (abonnements payés d'avance) | `477` Produits constatés d'avance |
| Comptes d'attente | `471` Débiteurs et créditeurs divers, comptes d'attente |
| Locations d'espaces, abonnements, réservations, domiciliation | `706` Services vendus, divisionnaires par activité |
| Ventes de marchandises (marketplace, boissons, fournitures) | `701` Ventes de marchandises |
| Réductions accordées (remises, coupons, parrainage) | `7069` ou compte de réductions sur ventes à arrêter |
| Escompte de règlement accordé | `673` Escomptes accordés |
| Achats de fournitures non stockées | `605` Autres achats |
| Transports et déplacements | `61` Transports (`614` Transports du personnel, `618` Autres frais de transport) |
| Services extérieurs | `62`, `63` (`631` Frais bancaires, dont commissions PawaPay) |
| Pertes sur créances | `651` Pertes sur créances clients |
| Manquants et excédents de caisse | `658` Charges diverses / `758` Produits divers, en passant par `471` tant que l'écart est en revue |
| Stocks (déjà utilisé par l'inventaire) | `31`, `603` Variation des stocks |

### 4.2 Exercices et périodes

- Un exercice = 12 mois (1er janvier au 31 décembre par défaut, paramétrable), découpé en périodes mensuelles.
- Statuts de période : `OPEN`, `LOCKED` (plus d'écriture automatique, saisie comptable seulement), `CLOSED`. Une période close n'accepte plus rien. Une écriture automatique datée dans une période close est passée au premier jour de la période ouverte suivante, avec mention de la date d'origine.
- Synchronisation avec `BillingPeriodClosure` : clore une période de facturation propose de verrouiller la période comptable, sans l'imposer.
- Clôture d'exercice : contrôles (balance équilibrée, comptes d'attente `471` soldés, écarts de caisse régularisés, avances `421` revues, rapprochements faits), calcul du résultat, écritures d'à-nouveaux dans le journal `AN` de l'exercice suivant.

### 4.3 Journaux

| Code par défaut | Type | Contenu |
| --- | --- | --- |
| `VT` | Ventes | Factures, avoirs, débours refacturés |
| `AC` | Achats | Factures fournisseurs, notes de frais justifiées |
| `CA-xxx` | Caisse | **Un journal par caisse**, créé avec la caisse, lié à son compte `571x` |
| `BQ-xxx` | Banque | Un journal par compte bancaire |
| `MM-xxx` | Mobile money | Un journal par compte opérateur (plateforme et marchand) |
| `PF` | Portefeuille | Mouvements des portefeuilles électroniques clients |
| `OD` | Opérations diverses | Régularisations, provisions, produits constatés d'avance, écarts |
| `ST` | Stock | Reprise des écritures `stock_journal_entry` |
| `AN` | À-nouveaux | Ouverture d'exercice |
| `EXT` | Extournes | Contre-passations d'inventaire (facultatif, sinon dans `OD`) |

Règles d'un journal : type, compte de trésorerie de contrepartie (journaux de trésorerie), comptes autorisés (liste blanche facultative), numérotation (continue par journal et par exercice, sans trou), mode de validation (écritures automatiques validées d'office ou laissées en brouillard), qui peut écrire, actif oui/non.

### 4.4 Pièces et écritures

- **Pièce** (`accounting_entry`) : numéro (`journal-exercice-séquence`), journal, date comptable, date de la pièce justificative, libellé, origine (`MANUAL`, `CASH`, `PLATFORM`, `IMPORT`, `CLOSING`), référence métier (`sourceType` + `sourceCode`), justificatifs (lien vers la GED existante), statut `DRAFT` puis `VALIDATED` puis éventuellement `REVERSED`, auteur, valideur, horodatages, pièce de contre-passation liée.
- **Ligne** (`accounting_entry_line`) : compte, compte auxiliaire de tiers, libellé, débit, crédit, devise et montant en devise, taux, code de taxe, base taxable, échéance, lettrage, ventilations analytiques.
- **Contrôles à la validation** : pièce équilibrée ; période ouverte ; comptes actifs et autorisés dans le journal ; analytique présente là où elle est obligatoire et ventilée à 100 % ; justificatif présent sauf dispense.
- **Validation** : rend la pièce immuable et lui attribue son numéro définitif. Le brouillard peut porter un numéro provisoire.
- **Contre-passation** : pièce inverse datée du jour, liée à l'originale, motif obligatoire.
- **Saisie manuelle** : formulaire en lignes avec équilibrage assisté, modèles d'écritures récurrentes (loyer, abonnements logiciels), import CSV contrôlé.
- **Lettrage** : manuel et automatique (même référence de facture, même montant), délettrage tracé. Il alimente la balance âgée et le recouvrement.

### 4.5 Moteur d'imputation (schémas d'écriture)

Un **schéma** décrit, pour un type d'événement, les lignes à produire :

```
eventType:   PAYMENT_RECEIVED
conditions:  paymentMethod = CASH
journal:     {caisse de la session}            # résolu à l'exécution
lignes:
  - débit  {compte trésorerie de la caisse}  montant = amount
  - crédit {compte tiers du client}            montant = amount   lettrage = facture(s)
analytique:  héritée de la facture imputée, sinon section de la caisse
```

- Les variables (`{compte du client}`, `{compte de la caisse}`, `{compte de la nature de dépense}`) sont résolues par des **résolveurs** dans le code ; les schémas eux-mêmes sont en base, éditables, versionnés (date d'effet) et testables sur un événement passé (« aperçu de l'écriture »).
- Le catalogue complet des événements et de leurs écritures est dans [catalogue-ecritures.md](catalogue-ecritures.md).

---

## 5. Comptabilité analytique et budget

Détail complet dans [analytique-et-budget.md](analytique-et-budget.md). En bref :

- **Analytique** (activable) :
  - axes paramétrables (site, activité, centre de coût, **poste de dépense**, projet, ressource, organisation cliente), sections en arbre ;
  - ventilation de chaque ligne de charge ou de produit, proposée automatiquement (ressource, nature, compte, employé, caisse) ;
  - clés de répartition et répartition secondaire des centres auxiliaires ;
  - reclassements tracés.
- **Analyse par poste de dépense** : chaque nature d'opération porte un poste (loyer, électricité, café, internet, déplacements...). Toutes les dépenses sont donc classées, quel que soit leur chemin (caisse, avance, facture fournisseur, paie, écriture manuelle). Pour chaque poste :
  - réalisé, budget, N-1 et tendance ;
  - décomposition par site, fournisseur et demandeur ;
  - détail jusqu'au justificatif ;
  - indicateurs rapportés à l'activité (par m², par membre) ;
  - alertes de dérive.
- **Postes définis par l'utilisateur** : chaque organisation crée ses axes, sections et postes analytiques, ainsi que ses **postes budgétaires** (nomenclature en arbre, reliée aux comptes, postes de dépense et natures). La création se fait depuis un écran dédié ou à la volée depuis une demande, avec validation selon le paramètre.
- **Budget complet** :
  - **prévisions** : dépenses attendues, ponctuelles ou récurrentes, avec objet, montant et mois, rattachées à un poste budgétaire ; rappels quand une prévision approche sans demande ;
  - **dépense prévue ou non prévue** : chaque dépense est liée à une prévision, ou déclarée non prévue avec motif, et éventuellement validée en plus ; états du prévu et du non prévu, qualité de la prévision, réserve pour imprévus ;
  - versions et révisions, mensualisation ;
  - élaboration par centre et circuit de validation ;
  - virements de crédits, gel, report ;
  - les quatre montants d'une ligne : budgété, engagé, réalisé, disponible.
- **Contrôle budgétaire dans les opérations** : l'état de besoin, la demande de fonds (saisie, soumission, approbation qui engage), le décaissement, la justification d'avance, la dépense directe au guichet, la commande et la facture fournisseur, l'écriture manuelle et la paie importée. Chaque ligne budgétaire est **bloquante**, **en alerte** ou **informative**, avec tolérance et dérogation tracée. La pièce imprimée porte le disponible avant et après.

## 6. Trésorerie et caisses

Le système de caisse complet (référentiel, sessions, comptages, opérations, transferts, contrôles, états, données, routes, droits, lots) est détaillé dans [caisses-et-sessions.md](caisses-et-sessions.md). Cette section en donne le cadre comptable.

### 6.1 Comptes de trésorerie

Nouvelle notion qui englobe : caisses (existantes), comptes bancaires, comptes mobile money (plateforme et marchands), coffre. Chacun a son compte comptable et son journal. Une caisse existante reçoit à la migration un compte `571x` et un journal `CA-<code>`.

### 6.2 Types de caisse

| Type | Usage | Règles |
| --- | --- | --- |
| Caisse principale | Reçoit les remises des guichets, alimente les guichets, fait les remises en banque | Plafond élevé, accès restreint |
| Caisse de guichet | Encaissement des clients, petites dépenses | Session par caissier, plafond, remise obligatoire au-delà |
| Petite caisse à fonds fixe | Menues dépenses sur pièces | Montant fixe ; réalimentée exactement du total des dépenses justifiées |
| Coffre | Stockage | Mouvements uniquement par transfert interne |
| Caisse automatique | Portefeuille, mobile money plateforme | Existant, aucune saisie manuelle |

### 6.3 Sessions : améliorations

1. **Billetage** à l'ouverture et à la clôture : saisie par coupure XAF (liste paramétrable, par exemple billets 10 000, 5 000, 2 000, 1 000, 500 et pièces 500, 100, 50, 25, 10, 5, 2, 1). Le total se calcule, le détail est conservé et imprimé sur le procès-verbal.
2. **Clôture à l'aveugle** (paramétrable) : la caissière saisit son comptage **avant** de voir le montant attendu ; l'écart n'apparaît qu'ensuite. Cela supprime la tentation d'ajuster le comptage.
3. **Double comptage** au-delà d'un seuil d'écart : un second compteur (responsable) recompte ; les deux comptages sont gardés.
4. **Passation** entre caissiers sans fermer la caisse : comptage contradictoire, procès-verbal de passation signé par les deux, la session suivante reprend le montant compté.
5. **Remise obligatoire** : au-delà du plafond `maxCashAmount`, le système bloque les encaissements en espèces (paramétrable : alerte ou blocage) jusqu'à une remise à la caisse principale ou en banque.
6. **Écart de clôture** : écriture automatique vers `471` (écart en revue), puis à l'approbation de l'écart vers `658` (manquant) ou `758` (excédent), ou vers le compte du caissier `421` si la direction décide de le lui imputer.
7. **Solde réel en temps réel** : tableau de bord de trésorerie avec, par compte de trésorerie, le solde théorique (écritures), le dernier solde compté (billetage) ou relevé (banque, opérateur), l'écart et son ancienneté. On sait à tout moment combien il y a réellement, où, et depuis quand ce n'a pas été vérifié.
8. **Rapport de clôture enrichi** : espèces (billetage), paiements hors espèces à remettre (chèques, déjà en `nonCashPayments`), demandes décaissées, avances ouvertes, écritures générées.

### 6.4 Banque et mobile money

- **Remise en banque** : bordereau de remise (espèces et chèques), mouvement `SAFE_DEPOSIT` côté caisse, écriture `585` ; à réception du relevé, l'écriture bancaire solde le `585`.
- **Chèques** : nouveau cycle `REÇU` (compte `513`), `REMIS` (bordereau), `ENCAISSÉ` (relevé), `REJETÉ` (contre-passation, la facture redevient due, le client est notifié, des frais de rejet sont facturables).
- **Virements reçus** : saisie au guichet (existant), puis pointage sur le relevé.
- **Relevés** : import CSV ou OFX par banque, import du relevé PawaPay (dépôts, remboursements, frais), saisie du relevé des numéros marchands.
- **Rapprochement** : pointage automatique (montant, date à plus ou moins N jours, référence), pointage manuel, écritures de régularisation proposées (frais bancaires, agios, commissions opérateur), état de rapprochement imprimable.
- **Frais PawaPay** : constatés au relevé ou calculés au taux contractuel à chaque dépôt (`631` contre le compte mobile money).

---

## 7. Mouvements de caisse et choix du journal par la caissière

### 7.1 Les mouvements existants restent

Les tables et les mouvements actuels sont conservés tels quels ; rien n'est réécrit. Ils sont **enrichis** :

| Champ ajouté | Rôle |
| --- | --- |
| `operation_nature_code` | Nature d'opération (référentiel, voir 7.2). Elle remplace l'usage libre de `flowCategory`, qui est gardé pour l'historique. |
| `journal_code` | Journal choisi pour l'écriture (par défaut celui de la caisse). |
| `accounting_entry_number` | Pièce comptable produite, une fois comptabilisée. |
| `accounting_status` | `NOT_APPLICABLE`, `PENDING`, `POSTED`, `FAILED`, avec motif. |
| `vat_amount`, `vat_code` | TVA récupérable d'une dépense sur facture. |
| `supplier_code` | Fournisseur pour une dépense. |
| `beneficiary_*` | Bénéficiaire réel d'une sortie (nom, pièce d'identité, téléphone), distinct de la contrepartie. |
| `request_number` | Demande de fonds à l'origine du mouvement. |
| `analytic_allocations` | Ventilation analytique (si activée). |

### 7.2 Natures d'opération

Référentiel paramétrable qui traduit le langage de la caisse en comptabilité :

| Champ | Exemple |
| --- | --- |
| Code, libellé | `ACHAT_FOURNITURES`, « Achat de fournitures de bureau » |
| Sens | Sortie |
| Compte de charge ou de produit | `6056` |
| TVA récupérable possible | oui, code `TVA18` |
| Justificatif exigé | facture ou ticket, au décaissement ou dans les N jours |
| Section analytique par défaut | Centre de coût Administration |
| Plafond par opération | 50 000 XAF au guichet, au-delà demande obligatoire |
| Demande préalable obligatoire | non |
| Journal par défaut | celui de la caisse |
| Familles | Achats, Déplacements, Avances, Remises, Transferts, Remboursements clients, Divers |

Les natures sont proposées à la caissière par famille, avec recherche, les plus utilisées d'abord.

### 7.3 Choisir le livre, ou en créer un

- À la saisie d'un mouvement, le champ **Journal** est prérempli avec le journal de la caisse. La caissière peut choisir un autre journal parmi ceux **autorisés pour sa caisse** (paramétrage du journal : caisses qui peuvent y écrire).
- **Créer un journal** depuis la caisse : bouton « Nouveau journal », réservé au droit `ACCOUNTING_JOURNAL:CREATE` (attribuable aux caissières principales). Formulaire réduit : libellé, code proposé automatiquement, type (caisse, opérations diverses), compte de trésorerie (prérempli avec celui de la caisse). Le journal est créé actif, rattaché à la caisse, et signalé à l'administrateur finance, qui peut le désactiver ou le fusionner avant toute écriture validée.
- **Garde-fous** : un journal de trésorerie doit avoir un compte de trésorerie ; un mouvement d'espèces ne peut pas aller dans un journal dont la contrepartie n'est pas une caisse ; une fois le journal mouvementé, on ne change plus son compte.
- Le journal choisi est **imprimé sur la pièce** (bon d'entrée ou de sortie) avec l'imputation.

### 7.4 Écriture d'un mouvement

Chaque mouvement confirmé produit sa pièce dans le journal choisi ; un contre-mouvement produit la contre-passation. Les mouvements de **paiement client** sont comptabilisés par l'événement de paiement, pas par le mouvement : sans cette règle, on aurait deux écritures pour un même encaissement. Détail par type dans le [catalogue](catalogue-ecritures.md#2-caisse).

---

## 8. Sorties et entrées de fonds : typologie

### 8.1 Sorties

| Type | Exemple | Pièce | Justification | Écriture |
| --- | --- | --- | --- | --- |
| **Dépense directe sur pièce** | Achat de fournitures avec facture en main | Bon de sortie + facture | Au décaissement | Charge (+ TVA récupérable) / caisse |
| **Avance à justifier** | Achat ponctuel dont le montant exact est inconnu, déplacement, mission | Bon de décaissement | **Après coup, ligne par ligne**, dans un délai paramétré | `421` / caisse, puis charges / `421` à la justification |
| **Remboursement de frais** | Employé qui a payé de sa poche | Note de frais + pièces | Avant décaissement | Charges / caisse (ou via `421`) |
| **Paiement fournisseur** | Règlement d'une facture fournisseur saisie | Bon de sortie + facture | Facture déjà enregistrée | `401` / caisse, lettré |
| **Remboursement client** | Annulation de réservation (existant `REFUND`) | Reçu de remboursement | Avoir | `411` / caisse |
| **Remise en banque** | Versement d'espèces | Bordereau de remise | Bordereau tamponné | `585` / caisse |
| **Transfert interne** | Guichet vers caisse principale ou coffre | Ordre de transfert | Contresigné par le receveur | `585` / caisse émettrice |
| **Réalimentation de petite caisse** | Fonds fixe | Bon + état des dépenses | Pièces de la petite caisse | Petite caisse / caisse principale |
| **Acompte au personnel** | Avance sur salaire | Bon de décaissement | Reconnaissance signée | `421` / caisse |
| **Dépôt de garantie restitué** | Fin de contrat de bureau | Bon de sortie | Contrat, état des lieux | `165` / caisse |

### 8.2 Entrées (hors encaissement client)

| Type | Exemple | Écriture |
| --- | --- | --- |
| **Approvisionnement depuis la banque** | Retrait pour alimenter la caisse | Caisse / `585` |
| **Retour de reliquat d'avance** | Le missionnaire rend ce qu'il n'a pas dépensé | Caisse / `421` |
| **Transfert reçu** | De la caisse principale | Caisse / `585` |
| **Dépôt de garantie reçu** | Caution d'un bureau privé | Caisse / `165` |
| **Apport, remboursement d'un tiers, produit divers** | Remboursement d'assurance, vente d'un vieux mobilier | Caisse / compte de la nature |
| **Excédent constaté** | Comptage supérieur | Caisse / `471` puis `758` |

---

## 9. Demandes, état de besoin, justification

Résumé ici, détail dans [demandes-et-justification.md](demandes-et-justification.md).

- **État de besoin** : expression chiffrée d'un besoin (lignes, quantités, prix estimés, motif, urgence). Il se valide puis se **transforme** en demande de fonds, en commande fournisseur ou en achat sur petite caisse, sans ressaisie.
- **Demande de fonds** : ouverte à tout employé habilité depuis son espace, **sans session de caisse**. Formulaire guidé par type, multi-lignes, avec un maximum de champs remplis automatiquement (demandeur, service, centre de coût, caisse habituelle, solde de la caisse, budget restant, avances non soldées, montant en lettres).
- **Circuit** : `BROUILLON`, `SOUMISE`, `EN_VALIDATION` (niveaux selon montant et nature), `APPROUVÉE`, `DÉCAISSÉE` (partiellement ou totalement), `À_JUSTIFIER`, `JUSTIFIÉE`, `SOLDÉE` ; plus `REJETÉE`, `ANNULÉE`, `EXPIRÉE`. Approuver ne décaisse plus : le décaissement est un acte distinct de la caissière, sur sa session, avec signature du bénéficiaire.
- **Justification une par une** : une avance se justifie ligne par ligne (date, fournisseur, nature, montant TTC, TVA, analytique, scan). Chaque ligne produit son écriture de charge contre `421`. L'écart final se solde par un retour d'espèces (entrée) ou un complément (sortie). Des relances partent à J+N ; au-delà d'un délai, une nouvelle avance est bloquée pour la personne.

---

## 10. Pièces comptables

Liste, champs, champs automatiques, signatures et numérotation : [pieces-comptables.md](pieces-comptables.md). Principes communs :

- Numérotation continue par type de pièce, par organisation et par exercice, via le moteur de séquences (`core/generator`).
- Mentions : raison sociale et identifiants de l'exploitant, caisse, session, journal et imputation comptable (et analytique si activée), montant en chiffres et **en lettres**, motif, bénéficiaire, signatures, QR de vérification (module `verify`), « DUPLICATA » sur toute réimpression.
- Une pièce émise ne se modifie pas : une erreur s'annule par une pièce d'annulation liée.
- Les gabarits sont des fichiers Thymeleaf (`templates/cash/...`), dans la même charte que la facture et le reçu.

---

## 11. Écritures automatiques de la plateforme

Toute transaction de la plateforme produit une écriture. Le catalogue complet est dans [catalogue-ecritures.md](catalogue-ecritures.md). Familles :

1. **Ventes** : facture émise, avoir, facture annulée, escompte, remise, coupon, parrainage, débours.
2. **Encaissements** : espèces, portefeuille, mobile money plateforme, mobile money hors plateforme, chèque, virement, carte, trop-perçu crédité au portefeuille.
3. **Remboursements** : par moyen, total ou partiel, remboursement mobile money refusé.
4. **Portefeuille** : rechargement, paiement, retenue (sans écriture), capture, transfert entre portefeuilles, expiration, ajustement administratif.
5. **Abonnements et pass** : facturation d'avance et produits constatés d'avance, reconnaissance mensuelle.
6. **Réservations** : confirmée, annulée, pénalité d'annulation, lot multi-ressources (une facture commune, une écriture).
7. **Domiciliation, événements, marketplace** : ventes, éventuelles commissions.
8. **Caisse** : tous les types de mouvement.
9. **Stock** : reprise de `stock_journal_entry`.
10. **Clôture** : écarts, provisions pour créances douteuses, produits constatés d'avance, pertes sur créances.

---

## 12. Fiscalité

- **TVA** : `TaxRule` existant (taux, pays, période d'application) relié à des comptes (`4431` collectée, `4452`/`4454` récupérable). Les règles en vigueur au Congo (taux de TVA, centimes additionnels, éventuelles exonérations) restent paramétrées dans `TaxRule` et validées par l'expert-comptable ; le plan n'en fige aucun taux.
- **Exigibilité** : sur les débits (facture) ou sur les encaissements (selon le régime) ; le moteur gère les deux et le paramètre décide. En régime d'encaissement, la TVA passe d'abord par un compte d'attente, puis devient exigible au paiement.
- **Déclaration** : état mensuel TVA collectée, TVA déductible, crédit ou dette ; détail par facture ; écriture de liquidation ; export.
- **Retenues à la source** sur prestataires (si applicables) : nature d'opération avec compte de retenue.
- **SEFC** : la facture scellée reste la référence fiscale ; l'écriture de vente reprend son numéro définitif et son empreinte.
- **Mentions** : l'exonération d'une organisation (déjà figée sur la facture) se reflète dans le code de taxe de l'écriture.

---

## 13. États, contrôles et exports

Résumé ci-dessous ; le détail de chaque état (colonnes, règles de calcul, contrôles de cohérence, correspondance des comptes vers les postes, liasse DSF, relevés de compte des tiers) est dans [etats-financiers.md](etats-financiers.md).

| État | Contenu |
| --- | --- |
| Brouillard | Écritures non validées, par journal |
| Journal | Par journal et période, avec totaux |
| Journal centralisateur | Totaux par journal et par mois |
| Grand livre | Par compte, avec soldes progressifs ; grand livre auxiliaire clients, fournisseurs, personnel |
| Balance générale | 6 colonnes (à-nouveaux, mouvements, soldes) ; balance auxiliaire ; balance âgée clients |
| États de compte des tiers | Relevé client, fournisseur, personnel tiré du grand livre auxiliaire ; envoi par courriel ; confirmation de solde |
| États financiers SYSCOHADA | Bilan, compte de résultat avec soldes de gestion, tableau des flux de trésorerie, notes annexes, comparatif N-1, éditions définitives figées ; ou système minimal de trésorerie selon le régime |
| Liasse fiscale | DSF pré-remplie, passage au résultat fiscal |
| Inventaire | Immobilisations et amortissements, provisions, créances douteuses, produits constatés d'avance, rapprochements, liste de clôture |
| Analytique | Résultat par section, par site, par activité ; budget comparé au réalisé |
| Trésorerie | Position par compte, prévisionnel (échéances clients et fournisseurs, abonnements à venir), rapprochements |
| Caisse | Brouillard de caisse journalier, état des avances non justifiées, état des écarts |
| TVA | Déclaration et justificatifs |

**Contrôles automatiques** quotidiens : balance équilibrée ; comptes d'attente non soldés depuis plus de N jours ; écarts entre le solde comptable d'une caisse et son solde opérationnel ; événements comptables en échec ; avances en retard de justification ; chèques reçus non remis ; écarts entre le grand livre du portefeuille et `4191`.

**Exports** : CSV et Excel de tous les états ; fichier des écritures aux formats d'import **Sage 100, Sage X3 et Odoo**, plus un CSV générique, avec table de correspondance des comptes et journaux par format ; archive annuelle (écritures, pièces, états) pour la conservation de 10 ans.

---

## 14. Fonctionnalités à compléter, améliorer et prendre en compte

### 14.1 À compléter (absent aujourd'hui)

1. **Fournisseurs et achats** : fiche fournisseur, factures fournisseurs, bons de commande issus des états de besoin, échéancier de paiement, retenues.
2. **Budget** : gestion complète et contrôle dans chaque opération, voir [analytique-et-budget.md](analytique-et-budget.md#3-gestion-budgétaire).
3. **Immobilisations** : mobilier, informatique, aménagements de l'espace ; fiche, amortissement linéaire ou dégressif, écritures de dotation, sortie d'actif.
4. **Dépôts de garantie** : caution à la signature d'un contrat de bureau, restitution ou retenue à la sortie (lien avec le module `contract`).
5. **Produits constatés d'avance** pour les abonnements, pass et domiciliations facturés d'avance.
6. **Créances douteuses** : passage en douteux depuis la relance (module `dunning`), provision, perte définitive (`WRITTEN_OFF` existe déjà côté facture).
7. **Cycle des chèques** (reçu, remis, encaissé, rejeté) et bordereaux de remise.
8. **Comptes bancaires et mobile money** comme comptes de trésorerie, relevés, rapprochement.
9. **Notes de frais** des employés.
10. **Paie** : import mensuel du journal de paie dans le journal `PA`, voir [financement-engagements-paie.md](financement-engagements-paie.md#4-import-du-journal-de-paie) ; le calcul de la paie reste hors périmètre.
11. **Emprunts et comptes courants d'associés** : tableaux d'amortissement, écritures d'échéance, intérêts courus, voir [financement-engagements-paie.md](financement-engagements-paie.md).
12. **Engagements hors bilan** : registre, alertes, note annexe, voir [financement-engagements-paie.md](financement-engagements-paie.md#3-registre-des-engagements-hors-bilan).

### 14.2 À améliorer (existe mais incomplet)

1. Demandes de caisse : décaissement distinct de l'approbation, demandeur sans session, lignes, natures, circuit multi-niveaux (voir 9).
2. Bons d'entrée et de sortie : champs, mentions, montant en lettres, imputation, signatures, QR (voir 10).
3. `flowCategory` libre remplacé par les natures d'opération.
4. Clôture de session : billetage, clôture à l'aveugle, double comptage, passation.
5. Anomalies de caisse : ajouter avances en retard, dépense sans justificatif, décaissement fractionné sous le seuil de validation, journal inhabituel pour une nature.
6. Encaissement : monnaie rendue créditable au portefeuille au choix du client ; frais de rejet de chèque.
7. Reçu et facture : mention de l'écriture comptable (numéro de pièce) pour l'audit.
8. Rapprochement du portefeuille (`WalletTreasuryReconciliation`) étendu au compte `4191`.
9. Statistiques de caisse : ajouter les sorties par nature et les avances.

### 14.3 À prendre en compte

1. **Multi-organisation** : une seule comptabilité au départ, mais toutes les tables comptables portent un `company_code` pour ouvrir plus tard une comptabilité par organisation sans migration lourde (décision 3).
2. **Multi-devises** : espèces en XAF seulement aujourd'hui ; prévoir devise et taux sur les lignes, et les écarts de change (`676` / `776`).
3. **Hors ligne** : une caisse qui perd le réseau ; à défaut d'un mode déconnecté, une procédure papier avec numérotation de secours et saisie différée marquée.
4. **Performance** : index sur (compte, date) et (journal, période) ; soldes mensuels pré-calculés par compte, mis à jour à la validation, pour la balance et le grand livre.
5. **Sécurité** : double visa au-delà de seuils, journal d'audit (module `core/audit`), réimpressions tracées, verrouillage des périodes.
6. **Ergonomie** : écrans de caisse au clavier, montants en grand, recherche de nature par mots, aperçu de la pièce et de l'écriture avant validation, mobile pour les demandes et les justifications (photo du ticket).
7. **Notifications** : demande à valider, demande approuvée prête à décaisser, avance à justifier, relance, écart de caisse, écriture en échec.
8. **Documentation** : chaque lot a son dossier de doc backend et sa doc frontend.

---

## 15. Modèle de données

Tables nouvelles (préfixe `acc_` pour la comptabilité, `cash_` pour la trésorerie). Migrations dev et prod à chaque lot.

| Table | Champs principaux |
| --- | --- |
| `acc_account` | number, label, class_code, nature, normal_side, reconcilable, third_party_type, treasury, analytic_required, tax_code, active |
| `acc_third_party_account` | account_number, party_type (MEMBER, CUSTOMER, BUSINESS, SUPPLIER, EMPLOYEE), party_code, label |
| `acc_fiscal_year` | code, start_date, end_date, status, closed_at, closed_by |
| `acc_period` | fiscal_year_id, code (AAAA-MM), start_date, end_date, status |
| `acc_journal` | code, label, type, treasury_account_number, numbering, auto_validate, active, created_by, cash_register_code (facultatif) |
| `acc_journal_register` | journal_code, cash_register_code : caisses autorisées à écrire dans le journal |
| `acc_entry` | entry_number, journal_code, period_id, entry_date, document_date, label, origin, source_type, source_code, status, posted_by, validated_by, validated_at, reversal_of, attachments |
| `acc_entry_line` | entry_id, line_no, account_number, third_party_code, label, debit, credit, currency, foreign_amount, rate, tax_code, tax_base, due_date, lettering_code |
| `acc_lettering` | code, account_number, third_party_code, lettered_at, lettered_by |
| `acc_posting_rule` | event_type, conditions (jsonb), journal_resolver, lines (jsonb), analytic_resolver, version, effective_from, active |
| `acc_posting_event` | event_type, reference, payload, status (PENDING, POSTED, FAILED, IGNORED), failure_reason, entry_number, attempts : registre d'idempotence |
| `acc_analytic_axis` | code, label, required, active |
| `acc_analytic_section` | axis_code, code, label, parent_code, active |
| `acc_analytic_allocation` | entry_line_id, section_code, percentage, amount |
| `acc_allocation_key` | code, label, lines (jsonb) : clés de répartition |
| `acc_budget`, `acc_budget_line` | exercice, compte, section, montant, engagé, consommé |
| `acc_monthly_balance` | account, third_party, period, debit, credit : soldes pré-calculés |
| `cash_operation_nature` | code, label, family, direction, account_number, vat_code, requires_document, document_delay_days, default_section, max_amount, requires_request, default_journal_code |
| `cash_treasury_account` | code, type (CASH, BANK, MOBILE_MONEY, SAFE), label, account_number, journal_code, bank_details, cash_register_code |
| `cash_denomination_count` | session_id, moment (OPENING, CLOSING, HANDOVER, RECOUNT), denomination, quantity, counted_by |
| `cash_handover` | session_from, session_to, counted_amount, signatures |
| `fund_need_statement` | numéro, demandeur, service, motif, urgence, statut, total estimé (état de besoin) |
| `fund_need_line` | désignation, quantité, prix estimé, nature, section |
| `fund_request` | remplace `cash_request` pour les nouvelles demandes (l'ancienne table est conservée en lecture) |
| `fund_request_line` | nature, désignation, montant, fournisseur, section |
| `fund_request_approval` | niveau, approbateur, décision, date, commentaire |
| `fund_disbursement` | demande, mouvement de caisse, montant, bénéficiaire, signature |
| `expense_justification` | demande (avance), statut, total justifié, reliquat |
| `expense_justification_line` | date, fournisseur, nature, montant TTC, TVA, section, pièce (GED), entry_number |
| `cheque` | numéro, banque, tireur, montant, transaction, statut (RECEIVED, DEPOSITED, CLEARED, REJECTED), bordereau |
| `bank_deposit_slip` | bordereau de remise : lignes espèces et chèques |
| `bank_statement`, `bank_statement_line` | relevés importés et pointage |
| `supplier`, `supplier_invoice` | lot achats |
| `fixed_asset`, `depreciation_schedule` | lot immobilisations |

Colonnes ajoutées à `cash_movement` : voir 7.1. À `payment_transaction` : `accounting_status`, `accounting_entry_number`. À `billing_document` : idem.

---

## 16. Droits et séparation des tâches

| Droit | Qui, par défaut |
| --- | --- |
| `FUND_NEED:CREATE`, `FUND_REQUEST:CREATE` | Tout employé habilité |
| `FUND_REQUEST:APPROVE_L1`, `_L2`, `_L3` | Chef de service, responsable financier, direction (seuils paramétrés) |
| `FUND_REQUEST:DISBURSE` | Caissière en session |
| `EXPENSE:JUSTIFY` | Bénéficiaire de l'avance, ou la caissière pour lui |
| `EXPENSE:VALIDATE_JUSTIFICATION` | Comptable |
| `CASH_MOVEMENT:CREATE` | Caissière |
| `ACCOUNTING_JOURNAL:CREATE` | Caissière principale, comptable |
| `ACCOUNTING_JOURNAL:MANAGE` | Administrateur finance |
| `ACCOUNTING_ENTRY:CREATE`, `:VALIDATE`, `:REVERSE` | Comptable ; validation par un second comptable au-delà d'un seuil |
| `ACCOUNTING_PERIOD:LOCK`, `:CLOSE` | Responsable financier |
| `ACCOUNTING_SETTINGS:MANAGE` | Administrateur finance (plan, schémas, natures, axes) |
| `ACCOUNTING_REPORT:READ` | Direction, comptable, expert-comptable (lecture seule) |
| `BANK_RECONCILIATION:MANAGE` | Comptable |

Les droits propres à la caisse (sessions, écarts, visas, transferts, contre-passations) sont détaillés dans [caisses-et-sessions.md](caisses-et-sessions.md#17-droits).

Règles imposées : le demandeur n'approuve pas sa demande ; l'approbateur ne décaisse pas ; le caissier ne valide pas la justification de sa propre avance ; le comptable qui saisit une écriture manuelle au-delà du seuil ne la valide pas seul. Toute dérogation est tracée.

---

## 17. Bascule et reprise de l'historique

1. **Date de bascule** : au plus tôt, dès le socle prêt (décision 6).
2. **Ouverture** : 2026 est le **premier exercice** (pas d'exercice 2025, décision 12) ; l'ouverture reprend les écritures de constitution (capital, apports, premières immobilisations) fournies par l'expert-comptable, dans `AN` ou `OD` ; soldes clients par facture ouverte (pour le lettrage) ; soldes des portefeuilles vers `4191` ; soldes des caisses au dernier comptage.
3. **Reprise depuis le 1er janvier 2026** (décision 6) des événements de la plateforme entre le début d'exercice et la bascule : le moteur peut rejouer les faits passés (transactions, factures, mouvements) dans un mode « reprise » qui produit des écritures en brouillard, à valider en bloc après contrôle.
4. **Anciennes demandes** (`cash_request`) : consultables en lecture ; celles en `PENDING` à la bascule sont migrées en nouvelles demandes `SOUMISE`.
5. **Natures d'opération** : correspondance proposée depuis les `flowCategory` existantes (requête des valeurs distinctes), validée à la main.
6. **Période de double tenue** d'un mois : export vers l'outil actuel de l'expert-comptable et comparaison des balances.

---

## 18. Découpage en lots

Chaque lot est livrable seul, avec migrations dev et prod, tests, doc backend et doc frontend dans son dossier.

| Lot | Contenu | Dépend de | Critère d'acceptation |
| --- | --- | --- | --- |
| **F1. Socle comptable** | Plan de comptes SYSCOHADA chargé, exercices et périodes, journaux, pièces et lignes, saisie manuelle, validation, contre-passation, brouillard, journal, grand livre, balance | | Une écriture manuelle se saisit, se valide, apparaît dans la balance équilibrée ; une période close refuse toute écriture |
| **F2. Moteur d'imputation** | Événements comptables par l'outbox, registre d'idempotence, schémas en base, résolveurs, écran des écritures en échec, aperçu d'écriture | F1 | Rejouer un événement ne double pas l'écriture ; un compte manquant met l'événement en échec sans bloquer le paiement |
| **F3. Trésorerie et caisse comptable** | Comptes de trésorerie, journal par caisse, natures d'opération, enrichissement des mouvements, choix et création de journal par la caissière, écritures de tous les mouvements, écarts de clôture | F2 | Chaque mouvement de caisse produit sa pièce dans le journal choisi ; le solde comptable d'une caisse égale son solde opérationnel |
| **F4. Encaissements et ventes** | Écritures des factures, avoirs, encaissements par moyen, remboursements, portefeuille, comptes clients auxiliaires, lettrage automatique | F2 | Une facture payée en espèces laisse le compte client soldé et lettré ; `4191` égale la somme des soldes de portefeuille |
| **F5. Demandes et état de besoin** | État de besoin, nouvelle demande de fonds multi-lignes, champs automatiques, circuit multi-niveaux, décaissement distinct, notifications, nouvelles pièces | F3 | Un employé sans session crée une demande ; elle traverse deux niveaux de validation ; la caissière la décaisse avec signature ; la pièce sort avec son imputation |
| **F6. Avances et justification** | Avances à justifier, justification ligne par ligne, reliquat et complément, relances, blocage, état des avances | F5 | Une avance de 100 000 justifiée en 3 lignes pour 85 000 se solde par un retour de 15 000 ; `421` du bénéficiaire revient à zéro |
| **F7. Pièces comptables** | Refonte des gabarits (bons, bon de décaissement, état de besoin, fiche de justification, PV de billetage et de passation, bordereau de remise), montant en lettres, QR, duplicata | F5 | Chaque pièce porte les mentions de [pieces-comptables.md](pieces-comptables.md) |
| **F8. Système de caisse complet** | Lots C0 à C6 de [caisses-et-sessions.md](caisses-et-sessions.md#20-découpage-en-lots-et-critères-dacceptation) : correctifs, référentiel des caisses, sessions et comptages, opérations et transferts à deux temps, contrôles, instruments et devises, pilotage | C0 : aucune ; C1 à C6 : C0 (les champs comptables attendent F3) | Ceux de chaque lot C |
| **F9. Analytique** | Axes, sections, ventilation, clés, répartition secondaire, règles automatiques, obligation par compte, reclassements, **postes de dépense** et analyse par poste, tableau de bord des dépenses, états analytiques | F3, F4 | Avec l'analytique activée, une dépense de classe 6 sans section ne se valide pas ; le résultat par site se calcule ; chaque dépense a un poste |
| **F10. Banque, chèques, mobile money** | Comptes bancaires, cycle des chèques, bordereaux de remise, import de relevés, rapprochement, frais PawaPay | F3, F4 | Un relevé importé se pointe automatiquement à plus de 80 % sur un mois réel |
| **F11. Fiscalité** | Comptes de taxe, exigibilité, déclaration de TVA, liquidation | F4 | L'état de TVA d'un mois égale la somme des TVA des factures scellées du mois |
| **F12. Abonnements et clôture** | Produits constatés d'avance, créances douteuses, contrôles de clôture, à-nouveaux, états financiers SYSCOHADA | F4, F11 | La clôture d'un exercice de test produit bilan, compte de résultat et à-nouveaux équilibrés |
| **F13. Achats et immobilisations** | Fournisseurs, factures fournisseurs, commandes depuis l'état de besoin, immobilisations et amortissements | F5, F9 | Une commande validée engage le budget ; une immobilisation génère son plan d'amortissement |
| **F16. Budget** | Budgets et versions, mensualisation, élaboration et validation, révisions, virements de crédits, engagements, contrôle budgétaire dans les états de besoin, demandes, décaissements, justifications, dépenses directes, écritures ; dérogations ; suivi, projections, alertes | F5, F6, F9 | Une demande sur une ligne bloquante sans disponible est refusée ou part en validation budgétaire ; un engagement de 100 000 réalisé pour 85 000 libère 15 000 |
| **F17. Financement, hors bilan, paie** | Emprunts et tableaux d'amortissement, comptes courants d'associés, registre hors bilan, import du journal de paie | F1, F2, F10 | Une échéance d'emprunt s'écrit seule ; un journal de paie importé s'équilibre et solde les avances retenues |
| **F15. Liasse et états de gestion** | DSF pré-remplie, passage au résultat fiscal, soldes de gestion mensuels, tableau de bord financier, ratios, budget et réalisé, prévisionnel de trésorerie, confirmation de solde | F12 | La DSF d'un exercice de test se remplit sans saisie hors notes rédactionnelles et tableau fiscal |
| **F14. Exports et bascule** | Exports pour l'expert-comptable, archive annuelle, outils de reprise et d'à-nouveaux | F1 à F4 | La balance exportée se réimporte sans écart dans Sage 100, Sage X3 et Odoo |

Le lot C0 (correctifs de caisse) part tout de suite, indépendamment du reste. Ordre conseillé : F1, F2, F3, F4 (le cœur : chaque transaction écrit), puis F5, F6, F7 (demandes et pièces, très visibles pour les équipes), puis F8, F9, F16 (analytique puis budget, qui s'appuie sur les demandes), F10, F11, F17, F12, F13, F14, F15. Les états de chaque lot sont listés dans [etats-financiers.md](etats-financiers.md#12-place-dans-les-lots).

---

## 19. Décisions prises

Réponses de la direction (11/11/2026). **Chaque décision est une valeur par défaut, modifiable dans le paramétrage** (voir [section 20](#20-tout-est-paramétrable)) ; aucune n'est codée en dur.

| # | Sujet | Décision | Paramètre |
| --- | --- | --- | --- |
| 1 | Régime SYSCOHADA | **Système normal** : bilan, compte de résultat, tableau des flux de trésorerie, notes annexes ; comptabilité d'engagement | `accounting.regime = NORMAL` (`SMT` reste disponible) |
| 2 | Plan de comptes | **Proposition par défaut** de la [section 4.1](#41-plan-de-comptes) chargée comme valeurs initiales, modifiable à l'écran ; l'expert-comptable ajuste ensuite | Plan de comptes, comptes par défaut des natures et des schémas |
| 3 | Sociétés | **Une seule comptabilité aujourd'hui, multi-société prévue** : toutes les tables comptables portent dès le départ un identifiant de société (`company_code`), une seule société est créée ; les organisations et sites sont des axes analytiques | `accounting.multi-company.enabled = false` |
| 4 | TVA | **À confirmer avec l'expert-comptable** : le moteur gère l'exigibilité sur les débits et sur les encaissements | `accounting.vat.chargeability = INVOICE | PAYMENT`, taux dans `TaxRule` |
| 5 | Abonnements, pass, domiciliation | **Chiffre d'affaires étalé** sur la période de service : produits constatés d'avance (`477`) en fin de mois, repris au fil du service | `accounting.revenue.deferral = MONTHLY` (`NONE`, `YEAR_END` disponibles) |
| 6 | Bascule | **Au plus tôt, avec reprise** : bascule dès que le socle est prêt ; les faits depuis le 1er janvier 2026 sont rejoués en brouillard, contrôlés puis validés en bloc ; ouverture par les écritures de constitution (2026 est le premier exercice, décision 12) | `accounting.start-date`, `accounting.replay-from = 2026-01-01` |
| 7 | Validation des demandes | **3 niveaux** : jusqu'à 50 000 XAF chef de service ; jusqu'à 500 000 XAF + responsable financier ; au-delà + direction. Le demandeur ne valide jamais sa propre demande | Grille de validation (montant, nature, type, niveaux, rôles) |
| 7b | Plafond de dépense directe au guichet | **50 000 XAF** : en dessous, bon de sortie direct sur pièce ; au-dessus, demande de fonds obligatoire | Plafond par caisse et par nature |
| 7c | Justification des avances | **7 jours** après la fin de la mission ou l'achat ; relances ; **nouvelle avance bloquée** en cas de retard | Délai par type de demande, mode `BLOCK` ou `ALERT` |
| 8 | Écarts de caisse | **Au cas par cas** : à l'approbation, le responsable choisit charge/produit (`658`/`758`) ou imputation au caissier (`421`) | Comptes d'écart, choix proposé par défaut |
| 9 | Exports | **Sage 100, Sage X3, Odoo et CSV** | Format d'export choisi à l'export ; correspondance des comptes et journaux par format |
| 10 | Création de journal | **Caissières principales**, journal **utilisable aussitôt** ; l'administrateur finance est prévenu et peut le désactiver | Droit `ACCOUNTING_JOURNAL:CREATE`, mode `IMMEDIATE` ou `AFTER_APPROVAL` |
| 11 | Liasse fiscale | **Modèle SYSCOHADA commun** aux pays OHADA, produit pré-rempli ; variantes nationales en paramétrage | Modèle de liasse versionné, pays |
| 12 | Exercice 2025 | **Aucun** : 2026 est le premier exercice ; pas de colonne N-1 en 2026 (mention « premier exercice ») ; pas d'immobilisations antérieures à reprendre | `accounting.first-fiscal-year = 2026` |
| 13 | Commissaire aux comptes | **Oui** : accès en lecture seule, circularisation des soldes, dossier de révision exporté | Rôle `AUDITOR`, activé |
| 14 | Notes annexes rédactionnelles | **Dans le logiciel** : éditeur par note, reprise du texte N-1, intégrées aux états et à la liasse | |
| 15 | Emprunts et comptes courants d'associés | **Gestion complète** : tableaux d'amortissement, écritures automatiques, intérêts courus, relevés | Comptes, mode de comptabilisation des échéances |
| 16 | Engagements hors bilan | **Registre** avec alertes et note annexe automatique | Types d'engagement |
| 17 | Paie | **Import du journal de paie** mensuel avec correspondance des rubriques ; calcul de la paie hors périmètre | Modèles d'import, correspondance rubriques et comptes |
| 18 | Analytique et budget | **Analytique par poste de dépense** et **budget complet** contrôlé dans chaque sortie de fonds et opération | `accounting.analytic.enabled`, `accounting.budget.enabled`, niveau de contrôle par ligne |
| 19 | Postes, prévisions, prévu / non prévu | L'utilisateur **définit ses postes analytiques et ses postes budgétaires** ; le budget porte des **prévisions** ; chaque dépense indique si elle **était prévue**, sinon motif obligatoire | Droits de création, validation des postes, motifs de non-prévision, validation supplémentaire du non prévu, seuil de dépassement de prévision |

Reste à obtenir de l'expert-comptable : régime d'exigibilité et taux de TVA (4), ajustements du plan de comptes (2), écritures de constitution pour l'ouverture de 2026 (6, 12), et le choix du compte des comptes courants d'associés (bloqués ou non, 15).

## 20. Tout est paramétrable

Règle de conception de tous les lots : **aucune valeur métier n'est codée en dur**. Le code fournit des valeurs initiales conformes aux décisions ci-dessus ; un administrateur finance les change à l'écran, sans redéploiement, avec historique (qui, quand, ancienne et nouvelle valeur) et date d'effet quand le changement touche des écritures.

| Domaine | Ce qui se paramètre |
| --- | --- |
| Général | Activation de la comptabilité, de l'analytique, du budget ; régime (normal, SMT) ; date de bascule et de reprise ; devise de tenue ; multi-société |
| Plan de comptes | Comptes, intitulés, nature, lettrable, tiers, trésorerie, analytique obligatoire, taxe associée, actif ; règles de création des comptes auxiliaires (préfixe, longueur) |
| Exercices et périodes | Début et durée d'exercice, découpage, verrouillage automatique après N jours, synchronisation avec la clôture de facturation |
| Journaux | Code, type, compte de trésorerie, numérotation, validation automatique ou brouillard, caisses autorisées, comptes autorisés, qui peut créer (et mode immédiat ou après validation) |
| Schémas d'écriture | Pour chaque événement : conditions, journal, lignes, comptes, analytique ; versionnés avec date d'effet ; activation par famille d'événements |
| Natures d'opération | Libellé, famille, sens, compte, TVA, justificatif exigé et délai, section par défaut, plafond, demande préalable, journal par défaut |
| Analytique | Axes, sections, obligation par compte et par axe, clés de répartition, règles automatiques |
| Fiscalité | Taux et règles (`TaxRule`), comptes de taxe, exigibilité, exonérations |
| Produits constatés d'avance | Mode (mensuel, en clôture, aucun), comptes, types de produits concernés |
| Caisses et trésorerie | Types de caisse, caisse mère et circuits de transfert, comptes et journaux liés, plafonds (détention, par opération, par jour), comportement au plafond (alerte ou blocage), coupures par devise, devises et taux, clôture à l'aveugle, tolérance, seuil de double comptage, double visa, délai de contre-passation, durée de session, politique de fin de mois, transit maximal, fréquence des comptages inopinés, comptes d'écart et choix par défaut ; liste complète dans [caisses-et-sessions.md](caisses-et-sessions.md#18-paramétrage) |
| Demandes de fonds | Types disponibles, formulaires (champs visibles et obligatoires par type), grille de validation (seuils, niveaux, rôles), délégation, délais de rappel et d'escalade, expiration d'une demande approuvée, plafond de dépense directe, barèmes de mission |
| Avances et justification | Délai par type, relances (jours et destinataires), blocage ou alerte, natures dispensées de pièce et leur plafond |
| Pièces comptables | Préfixes et format des numéros, mentions, signatures demandées, logo et en-tête par organisation, mention de duplicata |
| Notifications | Événements notifiés, destinataires, canaux |
| Exports | Formats (Sage 100, Sage X3, Odoo, CSV), correspondance des comptes et journaux vers le logiciel cible, séparateurs et encodage |
| Contrôles | Seuils des anomalies, ancienneté tolérée des comptes d'attente, contrôles quotidiens actifs |
| Droits | Rôles et permissions de la [section 16](#16-droits-et-séparation-des-tâches), dérogations à la séparation des tâches |

Écran unique « Paramètres finance », organisé par ces domaines, avec recherche, aide en ligne sur chaque paramètre et export/import de la configuration (pour passer d'un environnement de test à la production).
