# Comptabilité analytique et gestion budgétaire

Deux modules liés :

- la **comptabilité analytique** répond à « où est allé l'argent, et qu'a rapporté chaque activité ? » ;
- la **gestion budgétaire** répond à « avions-nous prévu cette dépense, et reste-t-il de quoi la payer ? ».

Le budget se contrôle **à chaque sortie de fonds et à chaque opération qui engage une dépense** : état de besoin, demande de fonds, décaissement, justification, facture fournisseur, commande, écriture manuelle.

Les deux sont activables par paramètre (`accounting.analytic.enabled`, `accounting.budget.enabled`) et entièrement paramétrables ([README, section 20](README.md#20-tout-est-paramétrable)).

## Sommaire

1. [Comptabilité analytique](#1-comptabilité-analytique)
2. [Analyse par poste de dépense](#2-analyse-par-poste-de-dépense)
3. [Gestion budgétaire](#3-gestion-budgétaire)
4. [Contrôle budgétaire dans les opérations](#4-contrôle-budgétaire-dans-les-opérations)
5. [Suivi, alertes et états](#5-suivi-alertes-et-états)
6. [Données](#6-données)
7. [Droits](#7-droits)
8. [Place dans les lots](#8-place-dans-les-lots)

---

## 1. Comptabilité analytique

### 1.1 Axes et sections

Un **axe** est une façon de découper l'activité ; une **section** est une case de cet axe, organisée en arbre.

| Axe proposé | Sections d'exemple | Source automatique |
| --- | --- | --- |
| Site | Siège > Étage 1 > Open space A | Site de la caisse, de la ressource, de l'employé |
| Activité | Bureaux privés, open space, salles de réunion, domiciliation, événements, marketplace, services annexes | Type de ressource réservée, type de produit ou de service vendu |
| Centre de coût | Accueil, entretien, informatique, direction, commercial | Service de l'employé demandeur, nature d'opération |
| Poste de dépense | Voir [section 2](#2-analyse-par-poste-de-dépense) | Nature d'opération |
| Projet ou événement | Lancement de l'étage 2, salon X | Événement (module `event`), saisie |
| Ressource | Salle Kintélé, bureau B2 | Ressource réservée |
| Organisation cliente | Grands comptes | Client de la facture |

- Axes, sections, arborescence, libellés, dates de validité : paramétrables.
- Chaque axe se déclare **obligatoire** ou **facultatif**, par compte ou classe de comptes (par défaut : classes 6 et 7 obligatoires sur l'axe Activité ou Centre de coût).
- Une section peut être **fermée** (plus d'imputation) sans perdre son historique.

### 1.2 Imputation

- Chaque ligne d'écriture de classe 6 ou 7 (et toute ligne d'un compte marqué « analytique ») porte une **ventilation** sur une ou plusieurs sections de chaque axe actif, en **pourcentage ou en montant**, totalisant 100 %.
- La ventilation est **proposée automatiquement** par des règles, par ordre de priorité :
  1. ce que l'utilisateur a saisi ;
  2. ce que porte le fait métier (ressource réservée, ligne de facture, ligne de demande de fonds) ;
  3. la nature d'opération ;
  4. le compte ;
  5. l'employé ou la caisse.
- **Clés de répartition** enregistrées pour les charges communes : loyer selon les surfaces, électricité selon les compteurs ou les surfaces, internet selon le nombre de postes, personnel d'accueil selon le temps passé. Une clé se recalcule (les surfaces changent) avec date d'effet.
- **Répartition secondaire** en fin de mois (facultatif) : les centres de coût auxiliaires (entretien, informatique, direction) sont déversés sur les centres principaux (activités) selon des clés. Cela permet un coût complet par activité.
- **Réimputation** : une erreur de section se corrige par une écriture analytique de reclassement, sans toucher à l'écriture générale validée. Elle est tracée.

### 1.3 Ce que l'analytique produit

| État | Contenu |
| --- | --- |
| Compte de résultat analytique | Par section : produits, charges directes, charges réparties, marge sur coûts directs, résultat |
| Coût de revient | Par activité, par site, par ressource (coût d'un bureau par mois, d'une heure de salle) |
| Rentabilité | Par ressource, par activité, par organisation cliente, par événement |
| Analyse des charges | Par poste de dépense, par centre de coût, par période, comparée à N-1 et au budget |
| Grand livre et balance analytiques | Lignes et soldes par section et par compte |
| Seuil de rentabilité | Par activité, à partir de la séparation charges fixes / variables (paramètre par compte) |
| Indicateurs croisés | CA par m² ou par poste de travail, coût par membre actif, avec les données d'occupation des réservations |

Tous les états se filtrent par axe, section (avec ou sans sous-sections) et période, et se comparent au budget de la même section.

---

## 2. Analyse par poste de dépense

Demande : **analyser chaque poste de dépense**.

### 2.1 Le poste de dépense

Un **poste de dépense** est une catégorie de gestion, plus parlante que le compte comptable, qui regroupe les dépenses comme la direction les pense. C'est un axe analytique dédié, alimenté par les natures d'opération. Exemples :

| Famille | Postes |
| --- | --- |
| Locaux | Loyer, charges locatives, électricité, eau, entretien et nettoyage, réparations, sécurité |
| Équipement et technique | Internet et télécoms, logiciels et abonnements, matériel informatique, mobilier, consommables d'impression |
| Accueil et vie du lieu | Café et boissons, fournitures de bureau, décoration, événements membres |
| Personnel | Salaires, charges sociales, formation, déplacements, missions |
| Commercial | Publicité, réseaux sociaux, salons, commissions |
| Administratif | Honoraires (expert-comptable, commissaire aux comptes, avocat), assurances, frais bancaires et commissions mobile money, impôts et taxes |
| Financier | Intérêts d'emprunt, agios |

- Un poste a : code, libellé, famille, comptes généraux associés (un poste peut couvrir plusieurs comptes et un compte plusieurs postes), nature fixe ou variable, responsable (qui suit ce poste), ligne budgétaire liée.
- Chaque **nature d'opération** de la caisse et des demandes porte son poste. La caissière ou le demandeur choisit donc un poste sans le savoir, en choisissant « Achat de café ».
- Les factures fournisseurs, notes de frais, justifications d'avance et écritures manuelles portent aussi le poste. Ainsi **100 % des dépenses** sont classées, quel que soit leur chemin.

### 2.2 Analyse d'un poste

Pour chaque poste, un écran et un état présentent :

- **Montant de la période** et cumul de l'exercice, comparés au budget (montant et %), à N-1 et à la même période de N-1.
- **Évolution mensuelle** (graphique sur 12 ou 24 mois), avec tendance et projection de fin d'exercice au rythme actuel.
- **Décomposition** par site, centre de coût, fournisseur, demandeur, moyen de paiement (caisse, banque, mobile money).
- **Détail jusqu'à la pièce** : chaque dépense du poste, avec date, fournisseur, montant, demande, justificatif (clic vers le scan).
- **Indicateurs rapportés à l'activité** : électricité par m², café par membre actif, nettoyage par jour d'ouverture, coût informatique par poste de travail.
- **Alertes** : dépassement du budget du poste, hausse anormale par rapport à la moyenne des 3 derniers mois (seuil paramétrable), dépense fractionnée sous le seuil de validation, nouveau fournisseur sur le poste.
- **Top fournisseurs** du poste et concentration.

### 2.3 Tableau de bord des dépenses

Vue d'ensemble pour la direction :

- tous les postes avec réalisé, budget, écart et taux de consommation ;
- tri par écart ;
- carte de chaleur mois par poste ;
- les cinq postes qui dérivent le plus ;
- dépenses de caisse non encore justifiées par poste.

---

## 3. Gestion budgétaire

### 3.1 Structure du budget

| Élément | Règle |
| --- | --- |
| Exercice | Un budget par exercice comptable ; plusieurs **versions** (initial, révisé 1, révisé 2...) dont une seule **active** |
| Ligne budgétaire | Croisement d'un **poste de dépense** (ou compte) et d'une **section** (site, centre de coût, projet), avec un montant annuel |
| Mensualisation | Répartition du montant annuel par mois : égale, saisonnière (profil enregistré), ou saisie mois par mois |
| Recettes | Budget de produits par activité et par site (CA attendu), pour le résultat prévisionnel |
| Investissements | Budget d'investissement séparé (mobilier, informatique, aménagements), suivi par projet |
| Trésorerie | Budget de trésorerie dérivé (encaissements et décaissements prévus par mois), comparé au réel |
| Responsable | Chaque ligne a un responsable (gestionnaire du budget du centre), qui reçoit les alertes et peut porter un niveau de validation |
| Niveau de contrôle | Par ligne : **bloquant**, **alerte** ou **informatif** (voir section 4) |
| Tolérance | Pourcentage de dépassement toléré avant blocage (ex. 5 %), par ligne ou global |

### 3.2 Élaboration

- **Assistant de construction** :
  - partir du réalisé N-1 (ou de rien la première année) ;
  - appliquer un pourcentage d'évolution global ou par poste ;
  - ajuster ligne par ligne ;
  - importer depuis Excel (modèle téléchargeable) ;
  - exporter pour travailler hors ligne.
- **Circuit de validation du budget** : brouillon, soumis par les responsables de centre, consolidé par le responsable financier, approuvé par la direction, actif. Un budget approuvé ne se modifie plus : on crée une révision.
- **Budget par centre** : chaque responsable saisit sa partie ; la consolidation montre le total et le résultat prévisionnel.

### 3.3 Vie du budget

- **Révision** : nouvelle version, motif, approbation ; l'historique des versions est conservé et comparable.
- **Virement de crédits** entre lignes (d'un poste sous-consommé vers un poste en tension). C'est une demande avec motif, validée selon les montants. Le total ne change pas, la trace est complète.
- **Report** de crédits non consommés sur l'exercice suivant : paramétrable, sur décision.
- **Gel** d'une ligne : plus d'engagement possible.

### 3.4 Les quatre montants d'une ligne

| Montant | Moment | Source |
| --- | --- | --- |
| **Budgété** | Approbation du budget ou de la révision | Version active |
| **Engagé** | Quand une dépense est décidée mais pas encore payée | Demande de fonds approuvée, commande fournisseur, état de besoin validé (pré-engagement, facultatif) |
| **Réalisé** | Quand la charge est comptabilisée | Écriture de charge (décaissement direct, justification d'avance validée, facture fournisseur, écriture manuelle) |
| **Disponible** | À tout instant | Budgété moins engagé non encore réalisé moins réalisé |

Un engagement est **soldé** quand la dépense correspondante est réalisée. L'écart est libéré : un engagement de 100 000 réalisé pour 85 000 libère 15 000. Un engagement **expire** ou est **annulé** si la demande expire ou est annulée.

---

## 4. Contrôle budgétaire dans les opérations

Le contrôle se fait **au moment où la dépense est décidée**, pas après. Chaque opération qui engage de l'argent affiche la ligne budgétaire touchée et son disponible, et applique le niveau de contrôle de la ligne.

### 4.1 Opération par opération

| Opération | Ce qui se passe |
| --- | --- |
| **État de besoin** | Ligne budgétaire déduite (poste de la nature, section du demandeur). Le disponible s'affiche. **Pré-engagement** facultatif à la validation. |
| **Demande de fonds : saisie** | Pour chaque ligne : ligne budgétaire, disponible, et ce qui restera après la demande. Indicateur vert (couvert), orange (dans la tolérance), rouge (dépassement). |
| **Demande de fonds : soumission** | Ligne **bloquante** et dépassement : soumission refusée, ou acceptée avec **validation budgétaire supplémentaire** (responsable du budget ou direction), selon le paramètre. Ligne en **alerte** : soumission acceptée, alerte au responsable du budget. |
| **Demande de fonds : approbation** | **Engagement** créé sur la ligne. Le valideur voit l'impact budgétaire avant de décider. |
| **Décaissement d'une dépense directe** | La charge est réalisée : l'engagement est soldé, le réalisé augmente. |
| **Décaissement d'une avance** | Rien en réalisé (l'argent est en `421`). L'engagement reste ouvert jusqu'à la justification. |
| **Justification d'avance** | Chaque ligne validée **réalise** la dépense sur sa ligne budgétaire (qui peut différer de la ligne prévue) ; à la clôture de l'avance, l'engagement est soldé et l'écart libéré. |
| **Dépense directe au guichet sans demande** (moins de 50 000 XAF) | Contrôle immédiat. Ligne bloquante sans disponible : la caissière ne peut pas décaisser et doit passer par une demande. |
| **Commande fournisseur** | Engagement à la validation de la commande. |
| **Facture fournisseur** | Réalisé ; solde l'engagement de la commande. |
| **Écriture manuelle de charge** | Réalisé ; contrôle en alerte seulement (le comptable régularise). |
| **Note de frais** | Comme une demande de fonds de remboursement. |
| **Paie importée** | Réalisé sur les lignes de personnel. |
| **Dotations, intérêts d'emprunt** | Réalisé sur leurs lignes. |
| **Investissement** | Contrôle sur le budget d'investissement à l'engagement de l'achat. |
| **Virement de crédits** | Demande dédiée, validation, puis mise à jour des deux lignes. |

### 4.2 Comportement en cas de dépassement (paramétrable par ligne)

| Niveau | Effet |
| --- | --- |
| Bloquant | Dépense refusée au-delà du disponible et de la tolérance ; seule issue : virement de crédits, révision, ou dérogation d'un rôle habilité, motivée et tracée |
| Alerte | Dépense acceptée ; alerte au responsable de la ligne et au responsable financier ; signalée sur la pièce |
| Informatif | Aucun blocage ni alerte, suivi dans les états seulement |

La pièce imprimée (demande, bon de décaissement, bon de sortie) porte la **ligne budgétaire, le disponible avant et après**, et la mention de dérogation le cas échéant.

### 4.3 Recettes

Le budget de produits ne bloque rien : il sert au suivi (CA réalisé par activité comparé au prévu) et au résultat prévisionnel.

---

## 5. Suivi, alertes et états

| État ou écran | Contenu |
| --- | --- |
| Suivi budgétaire | Par ligne : budgété, engagé, réalisé, disponible, taux de consommation, écart ; par mois et en cumul ; filtres par poste, section, responsable |
| Budget comparé au réalisé | Charges et produits, résultat prévisionnel comparé au résultat réel, par mois |
| Engagements ouverts | Demandes approuvées non décaissées, avances non justifiées, commandes non facturées, par ligne |
| Projection de fin d'exercice | Réalisé plus engagé plus tendance des mois restants, comparé au budget |
| Historique des versions | Comparaison initial et révisions, virements de crédits |
| Budget de trésorerie | Prévu et réel par mois |
| Tableau de bord du responsable | Ses lignes, ses alertes, les demandes en attente qui touchent son budget |

**Alertes** (canaux existants, destinataires paramétrables) :

- taux de consommation d'une ligne qui franchit 75 %, 90 % ou 100 % (seuils paramétrables) ;
- dépassement ;
- dérogation accordée ;
- consommation en avance sur le rythme mensuel prévu ;
- engagement ancien non soldé.

---

## 6. Données

| Table | Champs principaux |
| --- | --- |
| `acc_analytic_axis` | code, libellé, obligatoire, ordre, actif |
| `acc_analytic_section` | axe, code, libellé, parent, responsable, dates de validité, active |
| `acc_analytic_rule` | priorité, conditions (nature, compte, ressource, employé, caisse), axe, section ou clé, active |
| `acc_allocation_key`, `acc_allocation_key_line` | clé de répartition, sections et pourcentages, date d'effet |
| `acc_analytic_allocation` | ligne d'écriture, axe, section, pourcentage, montant |
| `acc_analytic_reclass` | reclassements analytiques tracés |
| `acc_expense_item` | poste de dépense : code, libellé, famille, nature fixe/variable, responsable, actif |
| `acc_expense_item_account` | correspondance poste et comptes |
| `acc_budget` | exercice, type (charges, produits, investissement, trésorerie), version, statut, motif, approuvé par, approuvé le, active |
| `acc_budget_line` | budget, poste ou compte, section, montant annuel, niveau de contrôle, tolérance, responsable, gelée |
| `acc_budget_line_month` | ligne, mois, montant |
| `acc_budget_commitment` | ligne, source (état de besoin, demande, commande), référence, montant engagé, montant soldé, statut (OPEN, SETTLED, RELEASED, EXPIRED) |
| `acc_budget_actual` | ligne, écriture, montant : alimenté par la comptabilisation des charges, pour un suivi rapide |
| `acc_budget_transfer` | ligne source, ligne cible, montant, motif, statut, validations |
| `acc_budget_override` | dérogation : opération, ligne, dépassement, motif, auteur, date |
| `acc_budget_profile` | profils de mensualisation |

Le disponible est calculé à partir de ces tables et mis à jour dans la même transaction que l'engagement, pour que deux demandes simultanées ne consomment pas le même disponible (verrou sur la ligne budgétaire).

---

## 7. Droits

| Droit | Qui, par défaut |
| --- | --- |
| `ANALYTIC_SETTINGS:MANAGE` | Administrateur finance (axes, sections, règles, clés, postes) |
| `ANALYTIC:RECLASS` | Comptable |
| `ANALYTIC_REPORT:READ` | Direction, responsables de centre (leurs sections), comptable |
| `BUDGET:PREPARE` | Responsables de centre (leurs lignes) |
| `BUDGET:CONSOLIDATE` | Responsable financier |
| `BUDGET:APPROVE` | Direction |
| `BUDGET:TRANSFER_REQUEST`, `BUDGET:TRANSFER_APPROVE` | Responsables de centre, responsable financier ou direction selon le montant |
| `BUDGET:OVERRIDE` | Direction, responsable financier (dérogation au blocage) |
| `BUDGET_REPORT:READ` | Direction, responsable financier, responsables de centre (leurs lignes) |

---

## 8. Place dans les lots

| Lot | Contenu |
| --- | --- |
| **F9. Analytique** (enrichi) | Axes et sections, règles automatiques, clés de répartition, répartition secondaire, reclassements, **postes de dépense** reliés aux natures, analyse par poste, tableau de bord des dépenses, états analytiques |
| **F16. Budget** (nouveau, remplace la partie budget de F13) | Budgets et versions, mensualisation, élaboration et circuit de validation, révisions, virements, engagements, contrôle budgétaire dans les états de besoin, demandes, décaissements, justifications, commandes, factures fournisseurs et écritures ; dérogations ; suivi, projections, alertes |

Dépendances :

- F9 dépend de F3 et F4 ;
- F16 dépend de F5 (demandes), F6 (justifications) et F9 (postes et sections) ;
- le contrôle sur les commandes et factures fournisseurs arrive avec F13.
