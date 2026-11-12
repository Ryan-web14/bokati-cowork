# États comptables et financiers

Tout ce que le module comptable doit produire : livres, balances, relevés de compte, états financiers SYSCOHADA (système normal, [décision 1](README.md#19-décisions-prises)), liasse fiscale, états d'inventaire, de gestion et de trésorerie. Complète la [section 13 du README](README.md#13-états-contrôles-et-exports).

## Sommaire

1. [Règles communes de génération](#1-règles-communes-de-génération)
2. [Livres et balances](#2-livres-et-balances)
3. [États de compte des tiers](#3-états-de-compte-des-tiers)
4. [États financiers SYSCOHADA](#4-états-financiers-syscohada)
5. [Liasse fiscale (DSF)](#5-liasse-fiscale-dsf)
6. [États d'inventaire et de clôture](#6-états-dinventaire-et-de-clôture)
7. [États fiscaux](#7-états-fiscaux)
8. [États de trésorerie](#8-états-de-trésorerie)
9. [États de gestion et analytiques](#9-états-de-gestion-et-analytiques)
10. [Piste d'audit et contrôle fiscal](#10-piste-daudit-et-contrôle-fiscal)
11. [Données et paramétrage](#11-données-et-paramétrage)
12. [Place dans les lots](#12-place-dans-les-lots)
13. [Décisions](#13-décisions)

---

## 1. Règles communes de génération

| Règle | Détail |
| --- | --- |
| Période | À une date (bilan, balance à date) ou sur une période (mois, trimestre, exercice, dates libres) |
| Provisoire ou définitif | **Provisoire** : à tout moment, en option avec le brouillard, avec la mention « PROVISOIRE ». **Définitif** : seulement sur des périodes closes ; l'état est figé (PDF et données archivés, empreinte SHA-256), daté et numéroté, et se réimprime à l'identique |
| Comparatif | Colonne N-1 sur tous les états financiers et de gestion. 2026 est le **premier exercice** (décision 12) : la colonne N-1 est vide avec la mention « premier exercice », elle se remplit à partir de 2027 |
| Filtres | Société (prévu pour le multi-société), journal, compte ou plage de comptes, tiers, section analytique, site, devise |
| Niveau de détail | Comptes à 2, 3, 4 chiffres ou détaillés ; avec ou sans comptes auxiliaires ; avec ou sans comptes à solde nul |
| Formats | Écran (avec renvoi de chaque montant vers les écritures qui le composent), PDF, Excel, CSV |
| Présentation | En-tête de la société, période, date d'édition, utilisateur, pagination, totaux reportés de page en page pour les livres |
| Performance | Calcul sur les soldes mensuels pré-calculés (`acc_monthly_balance`), détail seulement à la demande ; génération lourde (liasse, grand livre annuel) en tâche de fond avec notification |
| Droits | `ACCOUNTING_REPORT:READ` ; les états définitifs ne sont générés que par le responsable financier |

---

## 2. Livres et balances

| État | Contenu | Options |
| --- | --- | --- |
| **Brouillard** | Écritures non validées | Par journal, par auteur |
| **Journal** | Écritures validées d'un journal, dans l'ordre des numéros | Total par page et général ; contrôle de continuité des numéros |
| **Journal général** | Tous les journaux, dans l'ordre chronologique | |
| **Journal centralisateur** | Totaux débit et crédit par journal et par mois | Contrôle : égal aux totaux de la balance |
| **Grand livre général** | Par compte : report à nouveau, écritures (date, journal, pièce, libellé, lettrage, débit, crédit), solde progressif, total | Plage de comptes, avec ou sans lettrées |
| **Grand livre auxiliaire** | Même chose par tiers : clients, fournisseurs, personnel | Par tiers ou par collectif |
| **Grand livre analytique** | Par section : lignes ventilées | Si l'analytique est activée |
| **Balance générale** | Par compte : à-nouveaux, mouvements de la période, soldes. Présentation **4, 6 ou 8 colonnes** au choix | Avant ou après inventaire ; par classe avec sous-totaux |
| **Balance auxiliaire** | Par tiers, mêmes colonnes | Clients, fournisseurs, personnel |
| **Balance âgée** | Créances clients (ou dettes fournisseurs) non lettrées par ancienneté : non échu, 0 à 30, 31 à 60, 61 à 90, plus de 90 jours | Tranches paramétrables ; par client, par organisation |
| **Balance analytique** | Par section et par compte | Par axe |
| **Livre de caisse** | Par caisse et par jour : solde d'ouverture, entrées, sorties, solde de clôture, rapproché du billetage | Lien vers les pièces de caisse |
| **État du lettrage** | Par compte lettrable et par tiers : lignes non lettrées, lettrages partiels et leur reste, ancienneté | Par compte, par tiers, par ancienneté |
| **Propositions de lettrage** | Rapprochements proposés par le traitement de nuit, avec indice de confiance | À valider ou rejeter |
| **État de continuité des numéros** | Par journal et par type de pièce émise : premier et dernier numéro, nombre, trous, doublons, pièces annulées, état de la chaîne d'empreintes | Par exercice ; imprimable pour l'audit |
| **Pièces acceptées déséquilibrées et compte d'attente d'équilibrage** | Selon la politique choisie : pièces acceptées non régularisées, solde et lignes non lettrées du compte d'attente d'équilibrage | Ancienneté |
| **Journal des lettrages et délettrages** | Qui, quand, lignes, type, écart de règlement généré, motif de délettrage | Piste d'audit |
| **État des comptes d'attente** | Soldes des `471` et `585` avec ancienneté | Alerte au-delà du délai paramétré |

---

## 3. États de compte des tiers

### 3.1 Ce qui existe

- `GET /billing/customers/{customerType}/{customerCode}/statement` (et `.csv`) : relevé de compte client construit **à partir des factures et des paiements**.
- `company/statement` (`AccountStatementExporter`) : relevé d'une organisation.
- Relevé de portefeuille (`WalletStatementService`).

### 3.2 Cible

Détail du contenu imprimé, des modèles, du justificatif de solde et du dossier de justification de clôture dans [rapprochement-et-comptes-tiers.md](rapprochement-et-comptes-tiers.md#3-état-de-compte-dun-tiers).

Une fois la comptabilité activée, le relevé de compte est **tiré du compte auxiliaire du tiers** (grand livre). Ce n'est plus une reconstitution des factures. Il devient donc identique à ce que voit le comptable.

- Pendant la bascule, les deux coexistent et un contrôle compare leurs soldes.
- Les routes actuelles gardent leur forme : elles lisent la comptabilité quand elle est active, la facturation sinon (paramètre).

| Relevé | Contenu |
| --- | --- |
| **Client** (membre, client, organisation) | Solde d'ouverture ; factures, avoirs, paiements, remboursements, imputations du portefeuille ; échéance de chaque facture ; lettrage (réglée, partiellement réglée, ouverte) ; solde de clôture ; balance âgée du client ; montant échu à régler |
| **Fournisseur** | Factures, règlements, avoirs, solde, échéances |
| **Personnel** | Avances décaissées, justifications, reliquats rendus, solde dû par l'employé ou à l'employé |
| **Portefeuille** | Existant, rapproché du sous-compte `4191` du client |

Fonctions :

- envoi par courriel au tiers (PDF), en un clic ou en masse en fin de mois ;
- accès du membre ou de l'organisation à son relevé depuis le portail ;
- **lettre de confirmation de solde** (circularisation) pour l'audit de fin d'année ;
- lien depuis le relevé vers la relance (`dunning`).

---

## 4. États financiers SYSCOHADA

Système normal (Acte uniforme relatif au droit comptable et à l'information financière, 2017). Les états se calculent à partir de la balance après inventaire, par une **table de correspondance** comptes vers postes, paramétrable et versionnée (voir [section 11](#11-données-et-paramétrage)).

### 4.1 Bilan

- **Actif** en trois colonnes (brut, amortissements et dépréciations, net) et N-1 net : immobilisations incorporelles, corporelles, financières ; actif circulant HAO ; stocks ; créances et emplois assimilés (fournisseurs avances versées, clients, autres créances) ; trésorerie actif (titres de placement, valeurs à encaisser, banques, chèques postaux, caisse) ; écart de conversion actif.
- **Passif**, N et N-1 : capitaux propres (capital, primes, réserves, report à nouveau, résultat net, subventions d'investissement, provisions réglementées) ; dettes financières et ressources assimilées ; passif circulant (dettes circulantes HAO, clients avances reçues, fournisseurs d'exploitation, dettes fiscales et sociales, autres dettes, provisions pour risques à court terme) ; trésorerie passif (crédits de trésorerie, banques découverts) ; écart de conversion passif.
- Chaque poste porte sa **référence SYSCOHADA** (codes de rubrique de la liasse) et un renvoi à sa note annexe.
- Règles de classement par **solde** : un compte de tiers à solde inversé change de côté (client créditeur au passif). Les comptes de banque débiteurs vont en trésorerie actif, les comptes créditeurs en trésorerie passif.

### 4.2 Compte de résultat

Présentation en liste avec les **soldes significatifs de gestion** du SYSCOHADA révisé :

| Solde | Composition (résumé) |
| --- | --- |
| Marge commerciale | Ventes de marchandises moins achats de marchandises et variation de stocks |
| Chiffre d'affaires | Ventes de marchandises, produits fabriqués, travaux et services vendus, produits accessoires |
| Valeur ajoutée | Marge, production et autres produits, moins consommations (achats, transports, services extérieurs, impôts et taxes, autres charges) |
| Excédent brut d'exploitation | Valeur ajoutée moins charges de personnel |
| Résultat d'exploitation | EBE, reprises et transferts, moins dotations |
| Résultat financier | Produits financiers moins charges financières |
| Résultat des activités ordinaires | Exploitation et financier |
| Résultat hors activités ordinaires | Produits HAO moins charges HAO |
| Résultat net | Activités ordinaires et HAO, moins participation des travailleurs et impôt sur le résultat |

Colonnes N et N-1, renvois aux notes.

### 4.3 Tableau des flux de trésorerie

- Trésorerie nette au 1er janvier.
- Flux de trésorerie provenant des **activités opérationnelles** : capacité d'autofinancement globale (CAFG), puis variations de l'actif circulant HAO, des stocks, des créances et du passif circulant.
- Flux provenant des **activités d'investissement** : décaissements et encaissements liés aux immobilisations.
- Flux provenant du **financement par capitaux propres** et par **capitaux étrangers**.
- Variation de la trésorerie nette ; trésorerie nette au 31 décembre.
- **Contrôle** : la trésorerie de fin calculée est égale à la trésorerie du bilan (actif moins passif de trésorerie).
- Les flux d'investissement et de financement exigent de distinguer les opérations : les natures d'opération et schémas portent un **code de flux** (paramétrable) pour que le tableau se calcule sans retraitement manuel.

### 4.4 Notes annexes

Le SYSCOHADA révisé prévoit une trentaine de notes. Deux familles :

| Famille | Exemples | Production |
| --- | --- | --- |
| **Chiffrées** | Immobilisations (mouvements de l'exercice), amortissements, provisions, stocks, clients, autres créances, trésorerie, capital, dettes financières, fournisseurs, dettes fiscales et sociales, chiffre d'affaires par activité, achats, transports, services extérieurs, charges de personnel, impôts et taxes, charges et produits financiers, HAO, effectifs | **Automatique** depuis la balance, le registre des immobilisations, l'état des provisions et l'analytique ; montants ajustables avec trace |
| **Rédactionnelles** | Règles et méthodes comptables, événements postérieurs, engagements hors bilan, informations sur les parties liées | **Saisie** dans un éditeur, reprise de N-1 comme base |

Les notes sans objet sont marquées « Néant » comme le prévoit la liasse.

### 4.5 Contrôles de cohérence avant édition définitive

- Balance équilibrée (débit = crédit, à-nouveaux, mouvements, soldes) ; balances analytique et hors bilan de la classe 9 équilibrées.
- Aucune pièce en attente d'équilibrage datée dans l'exercice ; compte d'attente d'équilibrage soldé ; aucune pièce acceptée déséquilibrée non régularisée.
- Numérotation continue dans chaque journal (aucun trou, aucun doublon, chaîne d'empreintes intacte).
- Total actif = total passif.
- Résultat du bilan = résultat net du compte de résultat.
- Trésorerie du tableau des flux = trésorerie du bilan.
- Aucun compte mouvementé absent de la table de correspondance (sinon liste des comptes non classés).
- Comptes d'attente soldés ; rapprochements bancaires faits ; écarts de caisse régularisés ; avances au personnel revues.
- Totaux des notes égaux aux postes correspondants.

Un état définitif ne se génère qu'avec tous les contrôles au vert, ou avec une dérogation motivée du responsable financier.

### 4.6 Système minimal de trésorerie

Non retenu (décision 1) mais disponible par paramètre : bilan et compte de résultat simplifiés, état des recettes et dépenses, à partir de la même table de correspondance (version SMT).

---

## 5. Liasse fiscale (DSF)

- La liasse suit le **modèle SYSCOHADA commun** aux pays qui appliquent ce système (décision 11) : fiche d'identification, états financiers, notes et tableaux fiscaux. Le modèle commun est livré ; les éventuelles variantes nationales (Congo ou autre pays, si l'activité s'étend) se règlent en paramétrage.
- Le logiciel produit la liasse **pré-remplie** : identification de l'entreprise (fiche société paramétrée : RCCM, NIU, activité, dirigeants, effectifs), états de la section 4, notes chiffrées, tableaux fiscaux disponibles.
- Formats : PDF conforme au modèle, et Excel au format du modèle officiel pour import ou télédéclaration.
- Le modèle de liasse (rubriques, codes, mise en page) est **une donnée paramétrable et versionnée**, pour suivre les mises à jour de l'administration sans développement.
- Le passage du résultat comptable au **résultat fiscal** (réintégrations, déductions) se fait dans un tableau de détermination saisi par le comptable, avec reprise des charges non déductibles identifiées par compte (paramétrage « non déductible » sur les comptes).

---

## 6. États d'inventaire et de clôture

| État | Contenu |
| --- | --- |
| Tableau des immobilisations | Valeur d'origine, acquisitions, cessions, valeur de fin ; par catégorie ; tous les états des immobilisations dans [immobilisations-et-amortissements.md](immobilisations-et-amortissements.md#11-états) |
| Tableau des amortissements | Cumul début, dotation, reprises, cumul fin, valeur nette ; plan d'amortissement par bien |
| État des provisions et dépréciations | Créances douteuses par client, risques ; dotations et reprises |
| État des créances douteuses | Factures concernées, ancienneté, provision, perte ; tous les états des créances douteuses et des provisions dans [provisions-et-creances-douteuses.md](provisions-et-creances-douteuses.md#11-états) |
| Produits constatés d'avance | Par facture d'abonnement ou de pass : montant, part reportée, période ; tous les états dans [produits-et-charges-constates-d-avance.md](produits-et-charges-constates-d-avance.md#10-états) |
| Charges constatées d'avance, charges à payer, produits à recevoir | Par pièce |
| Rapprochements bancaires | Par compte : solde banque, solde comptable, suspens des deux côtés |
| Inventaire de caisse | PV de billetage au 31 décembre par caisse, comparé au solde comptable |
| Inventaire des stocks | Existant (`inventory`), rapproché des comptes `31` |
| Inventaire des chèques à encaisser | Chèques `RECEIVED` ou `DEPOSITED` non crédités au 31 décembre |
| Liste de contrôle de clôture | Chaque étape cochée, par qui, quand ; bloque la clôture tant qu'une étape obligatoire manque |
| Balance avant et après inventaire | Avec la liste des écritures d'inventaire |

---

## 7. États fiscaux

| État | Contenu |
| --- | --- |
| Déclaration de TVA | TVA collectée par taux, TVA déductible sur achats et sur services, crédit reporté, montant à payer ; détail par facture ; écriture de liquidation |
| Rapprochement TVA | TVA déclarée comparée aux soldes des comptes `443x` et `445x` |
| Chiffre d'affaires déclaré | CA par mois et par taux, rapproché des factures scellées SEFC |
| Retenues à la source | Par prestataire et par mois, si applicables |
| Charges non déductibles | Pour la détermination du résultat fiscal |

---

## 8. États de trésorerie

| État | Contenu |
| --- | --- |
| Position de trésorerie | Par compte (caisses, banques, mobile money, coffre) : solde comptable, dernier solde compté ou relevé, écart, date du dernier contrôle |
| Flux de trésorerie mensuels | Encaissements et décaissements par nature et par moyen |
| Prévisionnel | Échéances clients (factures ouvertes, échéanciers, abonnements à renouveler), fournisseurs, salaires et charges récurrentes ; projection sur 13 semaines et 12 mois |
| Encaissements par moyen | Espèces, mobile money plateforme et hors plateforme, chèques, virements, portefeuille ; frais opérateur |
| Portefeuilles clients | Total des soldes, rapproché de `4191` |

---

## 9. États de gestion et analytiques

| État | Contenu |
| --- | --- |
| Soldes intermédiaires de gestion mensuels | Ceux du compte de résultat, par mois, cumul et N-1 |
| Tableau de bord financier | CA du mois et cumul, marge, EBE, trésorerie, créances échues, délai moyen de paiement des clients (DSO), dépenses par nature, avances ouvertes |
| Résultat analytique | Par site, activité, ressource, centre de coût ; produits, charges directes, charges réparties, marge |
| Rentabilité par ressource | CA de la ressource (bureaux, salles) comparé à ses coûts affectés |
| Rentabilité par organisation cliente | CA, remises, coûts affectés, créances |
| Budget et réalisé | Par compte et par section : budget, engagé, réalisé, écart, taux de consommation |
| Ratios | Liquidité, autonomie financière, rotation des créances, poids des charges de personnel, rentabilité |
| Dépenses de caisse | Par nature, par demandeur, par service, par mois |

Ces états rejoignent le module `reporting` existant (rapports d'activité et financiers) : les rapports financiers actuels liront la comptabilité une fois activée.

---

## 10. Piste d'audit et contrôle fiscal

- **Fichier des écritures comptables** de l'exercice, complet et dans l'ordre de validation : journal, numéro, dates, compte, tiers, libellé, débit, crédit, lettrage, date de validation, référence de la pièce. Il est à remettre à l'administration en cas de contrôle.
- Pour chaque écriture : chemin vers la pièce justificative (GED) et vers le fait métier d'origine (facture, transaction, mouvement, demande).
- Journal des modifications de paramétrage (plan, schémas, correspondances) avec date d'effet.
- Journal des éditions définitives (qui, quand, empreinte).
- Archive annuelle complète pour la conservation de 10 ans.

---

## 11. Données et paramétrage

| Table | Rôle |
| --- | --- |
| `acc_statement_template` | Modèle d'état (bilan, compte de résultat, TFT, notes, DSF, SMT) : version, date d'effet |
| `acc_statement_line` | Poste : référence (code de rubrique), libellé, ordre, niveau, type (détail, sous-total, total), formule des sous-totaux, note de renvoi |
| `acc_statement_mapping` | Correspondance poste et comptes : plage ou liste de comptes, sens retenu (solde débiteur, créditeur, net), colonne (brut, amortissement, net), règle des soldes inversés |
| `acc_cash_flow_code` | Codes de flux pour le tableau des flux, portés par les natures et les schémas |
| `acc_generated_report` | États définitifs : type, période, version du modèle, fichier, données figées (jsonb), empreinte, auteur, date |
| `acc_note_text` | Textes des notes rédactionnelles par exercice : note, texte, statut (brouillon, validé), auteur, validé par |
| `acc_confirmation_request` | Circularisation : tiers, solde à date, lettre envoyée, réponse, écart, statut |
| `acc_tax_adjustment` | Tableau de passage au résultat fiscal |
| `acc_company_profile` | Fiche d'identification pour la liasse (si non couverte par l'existant) |

Les correspondances sont livrées pour le SYSCOHADA révisé et restent **modifiables** (principe de la [section 20 du README](README.md#20-tout-est-paramétrable)). Un écran « comptes non classés » liste les comptes mouvementés sans poste.

---

## 12. Place dans les lots

| Lot | États ajoutés |
| --- | --- |
| F1 Socle | Brouillard, journal, journal général, centralisateur, grand livre général, balance générale (4, 6, 8 colonnes), fichier des écritures |
| F3 Trésorerie et caisse | Livre de caisse, état des comptes d'attente, position de trésorerie |
| F4 Encaissements et ventes | Grand livre et balance auxiliaires, balance âgée, **relevés de compte tirés de la comptabilité**, contrôle avec les relevés actuels, envoi en masse |
| F6 Avances | Relevé de compte du personnel, état des avances |
| F9 Analytique | Grand livre et balance analytiques, résultat analytique, rentabilités |
| F10 Banque | Rapprochements, inventaire des chèques |
| F11 Fiscalité | Déclaration et rapprochement de TVA, CA déclaré |
| F18 Immobilisations | Fichier des immobilisations, fiche et plan par bien, notes 3A à 3E, dotations prévisionnelles, coût par espace, inventaire physique, rapprochement fichier et comptabilité |
| F19 Provisions et créances douteuses | Balance âgée avec risque, état des créances douteuses et litigieuses, variation des dépréciations et des provisions, registre des risques, passifs éventuels, pertes sur créances, réintégrations fiscales |
| F20 Constatations d'avance | Détail du `477` et du `476`, échéancier du chiffre d'affaires à reconnaître, chiffre d'affaires facturé et reconnu, factures à établir et non parvenues, crédits de pass non consommés |
| F12 Clôture | Table de correspondance, **bilan, compte de résultat, tableau des flux, notes chiffrées et rédactionnelles**, contrôles de cohérence, états d'inventaire, liste de clôture, éditions définitives |
| **F15 Liasse et gestion** (nouveau) | DSF pré-remplie, passage au résultat fiscal, SIG mensuels, tableau de bord financier, ratios, budget et réalisé, prévisionnel de trésorerie, circularisation |

---

## 13. Décisions

| Sujet | Décision |
| --- | --- |
| Liasse | Modèle SYSCOHADA commun, pré-rempli, PDF et Excel ; variantes nationales paramétrables |
| Exercice 2025 | Aucun : 2026 premier exercice, pas de comparatif N-1 en 2026, pas d'immobilisations antérieures |
| Commissaire aux comptes | Oui : rôle `AUDITOR` en lecture seule sur la comptabilité, les pièces et les états ; circularisation des soldes clients, fournisseurs, banques et associés (lettres générées, réponses saisies, écarts suivis) ; **dossier de révision** exporté (balance, grands livres, états, rapprochements, PV de caisse, registres) |
| Notes rédactionnelles | Rédigées dans le logiciel : éditeur par note, modèles de texte, reprise de N-1, validation, intégrées aux états et à la liasse |
