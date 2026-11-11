# Immobilisations et amortissements

Volet du [plan finance](README.md) consacré aux biens durables de l'exploitant : mobilier et aménagements des espaces, matériel informatique et audiovisuel, logiciels, véhicules, dépôts de garantie versés. Il couvre tout le cycle de vie d'un bien : entrée, mise en service, amortissement, dépréciation, transfert, entretien, inventaire physique, sortie. Les états qui en sortent alimentent le bilan, les notes annexes 3A à 3E et la liasse fiscale.

Rédigé le 11/11/2026. Les numéros de comptes sont des **propositions conformes au SYSCOHADA révisé**, chargées comme valeurs initiales et à valider par l'expert-comptable (décision 2 du plan) ; ceux marqués « à confirmer » doivent l'être avant le lot. Toutes les écritures sont en partie double ([section 4.6](README.md#46-partie-double--règles-et-garanties)), et les pièces suivent la numérotation continue et protégée ([section 4.8](README.md#48-numérotation-des-pièces--continuité-et-protection)).

---

## Sommaire

1. [Principes](#1-principes)
2. [Catégories et comptes](#2-catégories-et-comptes)
3. [Fiche d'immobilisation](#3-fiche-dimmobilisation)
4. [Entrée d'un bien](#4-entrée-dun-bien)
5. [Amortissements](#5-amortissements)
6. [Dépréciations](#6-dépréciations)
7. [Vie du bien](#7-vie-du-bien)
8. [Sortie d'un bien](#8-sortie-dun-bien)
9. [Inventaire physique](#9-inventaire-physique)
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

1. **Un bien, une fiche.** Chaque bien immobilisé a une fiche unique, avec un code généré (`IMM-2026-00001`) et une étiquette QR collée sur le bien. Un lot de biens identiques (20 chaises) peut avoir une fiche avec une quantité, ou une fiche par unité, selon la catégorie.
2. **La fiche est la source, la comptabilité suit.** Toute opération sur un bien (entrée, mise en service, dotation, transfert, dépréciation, sortie) produit sa pièce par le moteur d'imputation. On ne saisit jamais à la main une écriture sur un compte de classe `2` ou `28` d'un bien suivi : ces comptes refusent la saisie manuelle hors schéma, sauf droit spécial et motif.
3. **Le fichier des immobilisations égale la comptabilité, à tout moment.** Le total des valeurs brutes des fiches égale le solde des comptes `2x` ; le cumul des amortissements des fiches égale le solde des comptes `28x` ; idem pour les dépréciations `29x`. Un écart est une anomalie bloquante pour la clôture (section 12).
4. **Approche par composants** (SYSCOHADA révisé) : un bien dont les éléments ont des durées d'utilisation différentes (aménagement d'un plateau : cloisons, électricité, climatisation, revêtements) est découpé en composants, chacun avec sa durée et son plan.
5. **Saisie minimale** : la fiche est pré-remplie depuis la facture fournisseur, la demande de fonds ou la justification ; la catégorie propose les comptes, la durée et le mode ; le lieu et la section analytique se déduisent de la ressource ou du site (section 3.2).
6. **Rien n'est figé dans le code** : catégories, comptes, durées, modes, coefficients, seuil d'immobilisation, rythme des dotations sont des paramètres (section 17).

---

## 2. Catégories et comptes

Valeurs initiales, adaptées à un espace de coworking. Chaque catégorie porte ses comptes, sa durée et son mode par défaut ; on en crée d'autres à l'écran.

| Catégorie | Compte du bien | Amortissement | Dotation | Durée par défaut | Mode par défaut |
| --- | --- | --- | --- | --- | --- |
| Logiciels, licences acquises, site internet | `213` | `2813` | `6812` | 3 ans | Linéaire |
| Frais de développement immobilisés (plateforme) | `211` | `2811` | `6812` | 5 ans | Linéaire |
| Aménagements, agencements et installations des espaces (cloisons, électricité, climatisation, revêtements) | `234` ou `235` (aménagements de bureaux) | `2834`, `2835` | `6813` | 10 ans, ou durée restante du bail si plus courte | Linéaire |
| Matériel de bureau (imprimantes, photocopieurs, destructeurs) | `2441` | `28441` | `6813` | 5 ans | Linéaire |
| Matériel informatique (ordinateurs, serveurs, bornes, réseau, Wi-Fi) | `2442` | `28442` | `6813` | 3 ans | Linéaire |
| Matériel audiovisuel des salles (écrans, vidéoprojecteurs, visioconférence) | `2441` ou divisionnaire dédié | `28441` | `6813` | 5 ans | Linéaire |
| Mobilier de bureau (bureaux, chaises, rangements, mobilier des espaces détente) | `2444` | `28444` | `6813` | 10 ans | Linéaire |
| Matériel et outillage (cuisine, électroménager, entretien) | `241` | `2841` | `6813` | 5 ans | Linéaire |
| Matériel de transport | `245` | `2845` | `6813` | 4 ans | Linéaire |
| Bâtiments (si l'exploitant devient propriétaire) | `231` ou `232` | `2831`, `2832` | `6813` | 20 ans | Linéaire |
| Terrains | `22` | aucun | aucune | non amortissable | |
| Dépôts et cautionnements versés (caution du bail des locaux, compteurs) | `275` | aucun | aucune | non amortissable | |
| Avances et acomptes versés sur immobilisations | `251`, `252` | aucun | aucune | non amortissable | |
| Immobilisations en cours (aménagement en chantier) | `239` (à confirmer) | aucun | aucune | pas amorti avant la mise en service | |

Comptes associés :

| Usage | Compte proposé |
| --- | --- |
| Fournisseurs d'investissements (lettrable, auxiliaire par fournisseur) | `481` (`4812` immobilisations corporelles, `4811` incorporelles) |
| TVA récupérable sur immobilisations | `4451` |
| Créances sur cessions d'immobilisations (lettrable) | `485` |
| Valeur comptable des cessions | `811` incorporelles, `812` corporelles |
| Produits des cessions | `821` incorporelles, `822` corporelles |
| Dépréciations des immobilisations | `29x` ; dotation `691x`, reprise `791x` (à confirmer) |
| Amortissements dérogatoires | `151` ; dotation `851`, reprise `861` |
| Production immobilisée | `721` incorporelle, `722` corporelle |
| Subventions d'investissement | `14x` ; reprise au résultat `799` |
| Dettes de location-acquisition | `17x` (avec [financement-engagements-paie.md](financement-engagements-paie.md)) |

---

## 3. Fiche d'immobilisation

### 3.1 Contenu

| Groupe | Champs | Origine |
| --- | --- | --- |
| Identification | Code `IMM-`, désignation, catégorie, marque, modèle, numéro de série, quantité, photo, étiquette QR | Code et QR **générés** ; désignation, marque et modèle **repris** de la ligne de facture ; série **scannée** ou saisie |
| Origine | Fournisseur, facture fournisseur, commande, demande de fonds, justification, pièce comptable d'entrée, mode d'entrée (achat, en cours, production, apport, location-acquisition, don, reprise) | **Déduits** de la pièce qui a créé la fiche |
| Valeurs | Coût d'acquisition HT, frais accessoires (transport, installation, droits de douane), TVA non récupérable incorporée, valeur d'entrée, valeur résiduelle | **Calculés** depuis les lignes rattachées ; valeur résiduelle à 0 par défaut |
| Dates | Date d'acquisition, date de mise en service, date de fin de garantie | Acquisition **déduite** de la facture ; mise en service = date de réception par défaut, confirmée par le PV de mise en service |
| Amortissement comptable | Mode, durée, taux, coefficient (dégressif), base, plan | **Proposés** par la catégorie, modifiables avant la première dotation |
| Amortissement fiscal | Mode et durée fiscaux, s'ils diffèrent | Proposés par la catégorie ; l'écart produit l'amortissement dérogatoire |
| Localisation | Organisation, site, ressource (bureau, salle, espace du module `ressource`), emplacement libre | **Liste** des sites et des ressources ; l'emplacement reprend `locationLabel` de la ressource |
| Affectation | Employé détenteur (ordinateur portable, téléphone), date de remise | **Liste** des employés ; fiche de remise signée |
| Analytique | Section par axe | **Déduite** du site et de la ressource ; modifiable avec droit |
| Composants | Fiche parent, liste des composants | Le découpage propose les composants types de la catégorie |
| Suivi | Contrat d'entretien, assureur et police, valeur assurée, documents (GED : facture, garantie, notice, photos) | Documents **rattachés** automatiquement depuis la facture ; le reste en option |
| État | `DRAFT`, `IN_PROGRESS` (en cours), `IN_SERVICE`, `FULLY_DEPRECIATED`, `DISPOSED` (sorti), `CANCELLED` | **Calculé** par les opérations |

### 3.2 Lien avec les ressources de la plateforme

- Une fiche peut être rattachée à une **ressource** (`features/ressource`, `Resource.code`) : la salle « Baobab » porte son écran, sa table et ses chaises. La section analytique de la ressource reçoit les dotations du bien, ce qui donne le **coût d'amortissement par espace** et la rentabilité par ressource dans les états analytiques ([analytique-et-budget.md](analytique-et-budget.md)).
- Un **équipement** d'une ressource (`ResourceAmenityLink`, par exemple « vidéoprojecteur ») peut pointer vers la fiche du bien qui le fournit. À la sortie du bien, le logiciel propose de retirer l'équipement de la ressource, pour que le portail n'affiche plus un équipement disparu.
- Une **panne longue** déclarée sur un bien indispensable peut proposer une fermeture de la ressource (`ResourceClosure`), à confirmer par l'exploitation.

---

## 4. Entrée d'un bien

### 4.1 Sources

| Source | Ce qui se passe |
| --- | --- |
| **Facture fournisseur** (lot F13) | Une ligne dont la nature d'opération est de type « immobilisation », ou dont le montant dépasse le seuil pour une catégorie immobilisable, crée une fiche `DRAFT` pré-remplie. La facture s'écrit au débit du compte du bien, au crédit de `481`. |
| **Demande de fonds et justification** (lots F5, F6) | Une ligne de justification d'avance de nature « immobilisation » crée la fiche ; l'écriture débite le compte du bien au lieu d'une charge. |
| **Dépense directe en caisse** | Même règle sur la nature du bon de sortie ; au-delà du plafond de dépense directe, la demande de fonds est obligatoire (décision 7b). |
| **Acompte à un fournisseur** | Débit `252` (ou `251`) ; il est soldé à la réception de la facture et rattaché à la fiche. |
| **Immobilisation en cours** | Un chantier d'aménagement a une fiche `IN_PROGRESS` ; chaque facture du chantier s'y rattache au débit de `239`. À la réception des travaux, le PV de mise en service transfère le total vers les comptes définitifs, par composant. |
| **Production immobilisée** | Travaux faits par le personnel (aménagement, développement de la plateforme si la direction l'immobilise) : coûts rattachés, puis D compte du bien ; C `721` ou `722`. |
| **Apport, constitution** | Biens apportés à la constitution : fiches créées à l'ouverture de 2026 avec les écritures de constitution fournies par l'expert-comptable ([README, section 17](README.md#17-bascule-et-reprise-de-lhistorique)). |
| **Location-acquisition** | Bien pris en location avec option d'achat : inscrit à l'actif contre une dette `17x`, échéancier tenu dans le volet financement. Une location simple (le bail des locaux) reste une charge et un engagement hors bilan. |
| **Don, subvention d'équipement** | Bien reçu gratuitement ou financé par une subvention : subvention au crédit de `14x`, reprise au résultat au rythme de l'amortissement. |
| **Reprise** | Biens acquis en 2026 avant la bascule : rejoués avec les autres faits de 2026 (décision 6). Aucune immobilisation antérieure à reprendre (décision 12). |

### 4.2 Seuil et biens de faible valeur

- En dessous du **seuil d'immobilisation** (paramètre par catégorie, valeur initiale 100 000 XAF HT par unité, à valider avec l'expert-comptable), le bien va en charge (`605` ou compte de la nature) et aucune fiche comptable n'est créée.
- On peut néanmoins garder une **fiche de suivi hors comptabilité** pour les petits biens qu'on veut inventorier (chaises, téléphones) : même étiquette QR, même inventaire physique, sans plan d'amortissement.

### 4.3 Valeur d'entrée

- Valeur d'entrée = prix d'achat HT net de remises + frais accessoires rattachés (transport, installation, douane) + TVA **non récupérable** le cas échéant.
- Les frais accessoires facturés à part se rattachent à la fiche depuis leur facture (liste des fiches `DRAFT` et `IN_SERVICE` récentes du même fournisseur, proposée d'office).
- Un avoir fournisseur sur un bien réduit sa valeur d'entrée ; s'il arrive après la première dotation, le plan est recalculé pour l'avenir.
- Devise : conversion au cours du jour de l'acquisition ; l'écart de change sur la dette `481` va en résultat de change, pas sur la valeur du bien.

### 4.4 Mise en service

- L'amortissement commence à la **date de mise en service**, pas à la date de facture.
- Le **PV de mise en service** (`PMS-`) est proposé à la réception : date, lieu, ressource, détenteur, composants, photo ; signé par le réceptionnaire et le responsable des moyens généraux. Sa validation fait passer la fiche en `IN_SERVICE` et calcule le plan.
- Un bien reçu mais non mis en service à la fin d'un mois est signalé (état des biens en attente de mise en service).

---

## 5. Amortissements

### 5.1 Base et modes

- **Base amortissable** = valeur d'entrée moins valeur résiduelle.
- **Linéaire** (par défaut) : annuité constante, **prorata temporis** en jours la première et la dernière année (année de 360 jours, paramétrable).
- **Dégressif** : taux linéaire multiplié par un **coefficient** selon la durée (valeurs initiales OHADA courantes : 1,5 pour 3 ou 4 ans ; 2 pour 5 ou 6 ans ; 2,5 au-delà, paramétrables par pays), calculé sur la valeur nette, à partir du premier jour du mois d'acquisition ; **bascule automatique en linéaire** quand l'annuité linéaire sur la durée restante devient supérieure.
- **Unités d'œuvre** (option) : pour un bien dont l'usure suit l'usage (photocopieur au nombre de copies, véhicule au kilométrage) ; le relevé du compteur est saisi chaque mois, sinon la dotation est estimée et alertée.
- **Non amortissable** : terrains, dépôts versés, acomptes, immobilisations en cours.

### 5.2 Plan d'amortissement

Calculé à la mise en service et conservé par version : chaque révision (section 5.5) crée une nouvelle version, l'ancienne reste consultable.

Exemple : ordinateur portable, 900 000 XAF HT, mis en service le 1er avril 2026, 3 ans.

| Exercice | Linéaire : dotation | Linéaire : valeur nette fin | Dégressif (coefficient 1,5, taux 50 %) : dotation | Dégressif : valeur nette fin |
| --- | --- | --- | --- | --- |
| 2026 (9 mois) | 225 000 | 675 000 | 337 500 | 562 500 |
| 2027 | 300 000 | 375 000 | 281 250 | 281 250 |
| 2028 | 300 000 | 75 000 | 225 000 (bascule en linéaire) | 56 250 |
| 2029 (3 mois) | 75 000 | 0 | 56 250 | 0 |
| **Total** | **900 000** | | **900 000** | |

### 5.3 Dotations

- **Rythme** : dotation **mensuelle** par défaut (paramètre `MONTHLY`, sinon `QUARTERLY` ou `YEARLY`), pour que les situations mensuelles, les soldes de gestion et le coût par espace soient justes. La dotation annuelle de clôture n'est alors qu'un complément au jour près.
- **Traitement automatique** le premier jour du mois (heure paramétrable) pour le mois écoulé : une pièce par catégorie et par section analytique dans le journal `OD` (ou un journal `IM` dédié si on le crée), avec le détail par fiche en annexe de la pièce. La pièce est validée d'office et numérotée comme les autres.
- **Simulation** avant comptabilisation : aperçu des dotations du mois ou de l'exercice, par catégorie, par section, par fiche.
- **Idempotence** : une fiche ne reçoit qu'une dotation par période ; relancer le traitement ne double rien. Une période verrouillée n'est plus dotée ; le rattrapage se fait sur la période ouverte suivante, tracé.
- **Période close et bien oublié** : un bien découvert après la clôture d'une période (facture arrivée en retard) reçoit le rattrapage des dotations manquantes dans la période ouverte, avec mention.
- **Biens totalement amortis** : passent en `FULLY_DEPRECIATED` et restent à l'actif tant qu'ils sont en service (valeur brute et cumul égaux) ; ils apparaissent dans un état dédié.

### 5.4 Amortissement dérogatoire

Quand le plan fiscal diffère du plan comptable (dégressif fiscal sur un bien amorti en linéaire comptable, durée fiscale plus courte), l'écart de chaque période s'inscrit en amortissement dérogatoire : dotation D `851` ; C `151` tant que l'amortissement fiscal dépasse le comptable, reprise D `151` ; C `861` ensuite. Le tableau de passage au résultat fiscal de la liasse reprend ces montants.

### 5.5 Révision du plan

- Changement de durée d'utilisation, de valeur résiduelle ou de mode, dépense ultérieure immobilisée, dépréciation : le plan est **recalculé pour l'avenir** à partir de la valeur nette à la date de révision. Les dotations passées ne sont jamais modifiées.
- Motif obligatoire choisi dans une liste, droit `FIXED_ASSET:REVISE`, version du plan conservée.
- Une erreur sur une dotation déjà comptabilisée se corrige par une écriture de correction, jamais en modifiant la pièce.

---

## 6. Dépréciations

- **Indices de perte de valeur** relevés à la clôture (et à tout moment) : bien endommagé, obsolète, inutilisé durablement, espace fermé, fin anticipée du bail des locaux pour les aménagements. Une liste de contrôle par catégorie est proposée à la clôture.
- **Constatation** : si la valeur actuelle (estimée et justifiée par pièce jointe) est inférieure à la valeur nette, dépréciation D `691x` ; C `29x`, puis plan recalculé sur la nouvelle base.
- **Reprise** quand l'indice disparaît : D `29x` ; C `791x`, plafonnée à ce qu'aurait été la valeur nette sans dépréciation.
- Une dépréciation exige le droit `FIXED_ASSET:IMPAIR` et une seconde validation au-delà d'un seuil.

---

## 7. Vie du bien

| Opération | Ce que fait le logiciel |
| --- | --- |
| **Transfert de lieu** (site, ressource, emplacement) | Historique des lieux ; si la section analytique change, les dotations suivantes vont sur la nouvelle section, et un reclassement analytique est proposé pour le mois en cours. Aucune écriture générale. |
| **Changement de catégorie ou de compte** | Reclassement D nouveau compte ; C ancien compte, pour la valeur brute et pour le cumul d'amortissement ; droit `FIXED_ASSET:REVISE`. |
| **Affectation à un employé** | Fiche de remise de matériel (`PRM-`) signée par l'employé ; restitution exigée à son départ : la désactivation du compte d'un employé qui quitte l'entreprise liste les biens qu'il détient encore. |
| **Entretien et réparation** | Charge de la période (`624` entretien, réparations) rattachée à la fiche pour l'historique et le coût total de possession ; aucune modification de la valeur. |
| **Dépense ultérieure immobilisée** (amélioration, extension de durée, remplacement d'un composant) | Ajoutée comme nouveau composant ou à la valeur du composant, plan recalculé ; le composant remplacé sort (section 8). |
| **Garantie et contrats d'entretien** | Alerte avant la fin de garantie et avant l'échéance d'un contrat ; une panne sous garantie est signalée à l'enregistrement de la réparation. |
| **Assurance** | Assureur, police, valeur assurée ; état des biens non assurés ou sous-assurés au-delà d'un seuil. |
| **Panne, hors service** | État opérationnel (en service, en panne, en réparation, inutilisé) distinct de l'état comptable ; un bien inutilisé plus de N mois est proposé à la revue de dépréciation. |

---

## 8. Sortie d'un bien

### 8.1 Motifs

Cession (vente), mise au rebut, vol ou perte, sinistre, don, échange, sortie d'un composant remplacé, retour au fournisseur. Le motif est choisi dans une liste ; la sortie exige le **PV de sortie d'immobilisation** (`PSI-`), signé par deux personnes, avec la pièce justificative (facture de vente, constat de destruction, déclaration de vol, rapport d'expertise).

### 8.2 Déroulé

1. **Complément de dotation** jusqu'à la date de sortie, au jour près, calculé d'office.
2. **Sortie de l'actif** : le cumul des amortissements et des dépréciations est soldé, la valeur nette comptable passe en charge HAO : D `28x` cumul ; D `29x` le cas échéant ; D `812` (ou `811`) valeur nette ; C compte du bien, valeur brute.
3. **Prix de cession** s'il y en a : facture de cession émise par la facturation (numérotation des factures), D `485` (ou le client) ; C `822` (ou `821`) ; C `4431` si la cession est soumise à TVA.
4. **Amortissement dérogatoire** restant repris : D `151` ; C `861`.
5. **Subvention** encore au passif pour ce bien : reprise du solde au résultat.
6. **Plus ou moins-value** calculée et affichée : prix de cession moins valeur nette. Elle figure dans l'état des plus et moins-values (note 3D) sans écriture propre, puisqu'elle résulte des comptes `81x` et `82x`.
7. La fiche passe en `DISPOSED`, l'étiquette QR est désactivée, l'équipement de la ressource est retiré si on le confirme (section 3.2).

Exemple : l'ordinateur de la section 5.2 (linéaire) est vendu 200 000 XAF le 30 juin 2028. Cumul au 30 juin 2028 : 225 000 + 300 000 + 150 000 = 675 000 ; valeur nette 225 000 ; moins-value de 25 000.

| Pièce | Débit | Crédit |
| --- | --- | --- |
| Complément de dotation janvier à juin 2028 (si les dotations mensuelles ne l'ont pas déjà passé) | `6813` 150 000 | `28442` 150 000 |
| Sortie de l'actif | `28442` 675 000 ; `812` 225 000 | `2442` 900 000 |
| Facture de cession | `485` 200 000 | `822` 200 000 |
| Encaissement | `571x` ou `521x` 200 000 | `485` 200 000 (lettré) |

### 8.3 Vol, sinistre, indemnité d'assurance

Sortie de l'actif comme ci-dessus au motif « vol » ou « sinistre » ; l'indemnité d'assurance, quand elle est notifiée, s'écrit D compte de l'assureur ; C produit HAO (compte à confirmer). Une déclaration de vol non suivie d'indemnité reste en alerte jusqu'à la décision.

---

## 9. Inventaire physique

- **Étiquettes** : imprimées depuis la fiche ou par lot (planche d'étiquettes A4, imprimante d'étiquettes) avec le code, la désignation courte et le QR. Réimpression tracée.
- **Campagne d'inventaire** : ouverte par le responsable des moyens généraux, sur un périmètre (site, ressources, catégories), avec une équipe d'inventaire. Au moins une par exercice, avant la clôture.
- **Scan** avec un téléphone (page mobile du back-office) : chaque QR scanné marque le bien « vu », à tel endroit, par telle personne, avec photo facultative. Un bien vu ailleurs que son lieu enregistré propose un transfert. Un bien trouvé sans étiquette se déclare (« bien non inscrit ») avec photo.
- **Écarts** à la fin de la campagne : biens non vus (à rechercher, puis sortie au motif « perte » après décision), biens trouvés non inscrits (à régulariser : fiche créée, origine à documenter), biens mal localisés.
- **PV d'inventaire physique** (`PIP-`) : périmètre, dates, équipe, nombre de biens attendus, vus, manquants, trouvés, décisions ; signé par le responsable et un témoin ; il est joint au dossier de justification de clôture ([rapprochement-et-comptes-tiers.md](rapprochement-et-comptes-tiers.md#43-dossier-de-justification-des-soldes-de-clôture)).

---

## 10. Écritures

Ajoutées au [catalogue des écritures](catalogue-ecritures.md#14-immobilisations-et-amortissements).

| Événement | Journal | Écriture |
| --- | --- | --- |
| Facture fournisseur d'immobilisation | `AC` | D compte du bien (ou `239` en cours) HT ; D `4451` TVA récupérable ; C `481` fournisseur d'investissement (lettrable) |
| Acompte versé sur immobilisation | trésorerie | D `252` (ou `251`) ; C `521x` ou `571x` |
| Imputation de l'acompte à la facture | `OD` | D `481` ; C `252` |
| Paiement du fournisseur d'investissement | trésorerie | D `481` (lettré) ; C `521x` ou `571x` |
| Immobilisation acquise par une avance justifiée | `OD` | D compte du bien ; D `4451` ; C `421` employé (lettré) |
| Mise en service d'une immobilisation en cours | `OD` | D comptes définitifs par composant ; C `239` |
| Production immobilisée | `OD` | D compte du bien ; C `721` ou `722` |
| Dépôt de garantie versé (caution des locaux) | trésorerie | D `275` ; C `521x` ; restitution : écriture inverse, retenue : D `6x` ; C `275` |
| Location-acquisition | `OD` | D compte du bien ; C `17x` |
| Subvention d'équipement reçue | trésorerie | D `521x` ; C `14x` ; reprise annuelle D `14x` ; C `799` |
| Dotation aux amortissements (mensuelle) | `OD` | D `6812` ou `6813` ; C `28x`, ventilée par section |
| Amortissement dérogatoire | `OD` | D `851` ; C `151` ; reprise D `151` ; C `861` |
| Dépréciation et reprise | `OD` | D `691x` ; C `29x` ; reprise D `29x` ; C `791x` |
| Reclassement de compte | `OD` | D nouveau compte ; C ancien, valeur brute et cumul |
| Sortie de l'actif | `OD` | D `28x` ; D `29x` ; D `811` ou `812` ; C compte du bien |
| Cession | `VT` | D `485` ; C `821` ou `822` ; C `4431` si TVA |
| Encaissement du prix de cession | trésorerie | D `521x` ou `571x` ; C `485` (lettré) |

Les comptes `481` et `485` s'ajoutent aux comptes lettrables ([README, section 4.7](README.md#47-lettrage)), avec pour clé le numéro de facture fournisseur et le numéro de facture de cession. Le compte `275` se lettre par contrat de bail.

---

## 11. États

| État | Contenu | Usage |
| --- | --- | --- |
| **Fichier des immobilisations** (registre) | Toutes les fiches à une date : code, désignation, catégorie, lieu, date de mise en service, valeur brute, cumul, dépréciation, valeur nette, état | Contrôle, audit, impression |
| **Fiche d'un bien** imprimable | Identification, origine, valeurs, plan d'amortissement, historique (lieux, détenteurs, entretiens, révisions), documents | Dossier du bien |
| **Plan d'amortissement** par bien | Par exercice et par mois, comptable et fiscal, version en cours et versions antérieures | Contrôle, annexe |
| **Tableau des immobilisations** (note 3A) | Par catégorie : valeur brute début, acquisitions, virements de poste à poste, sorties, valeur brute fin | États financiers, liasse |
| **Biens en location-acquisition** (note 3B) | Biens, valeurs, dettes associées | États financiers |
| **Tableau des amortissements** (note 3C) | Par catégorie : cumul début, dotations, reprises et sorties, cumul fin | États financiers, liasse |
| **Plus et moins-values de cession** (note 3D) | Par bien sorti : valeur brute, cumul, valeur nette, prix, résultat | États financiers, fiscal |
| **Dépréciations** | Par bien : indice, montant, reprise | Clôture |
| **Dotations prévisionnelles** | Dotations des 12 prochains mois et des 5 prochains exercices, par catégorie et par section | Budget (lot F16), prévisionnel |
| **Coût par espace** | Dotations, entretien et assurance par ressource et par site | Analytique, rentabilité des espaces |
| **Biens totalement amortis encore en service** | Liste, ancienneté | Renouvellement, budget d'investissement |
| **Biens en attente de mise en service**, **en cours** | Liste, ancienneté | Contrôle |
| **Garanties et contrats** | Fins de garantie, échéances d'entretien | Moyens généraux |
| **Biens par détenteur** | Biens affectés à chaque employé, fiches de remise | Départs, contrôle |
| **Inventaire physique** | Résultat d'une campagne, écarts, PV | Clôture |
| **Rapprochement fichier et comptabilité** | Par compte `2x`, `28x`, `29x` : solde comptable, total des fiches, écart | Clôture (bloquant) |

Exports PDF, Excel et CSV comme les autres états ([etats-financiers.md](etats-financiers.md#1-règles-communes-de-génération)).

---

## 12. Contrôles et clôture

Contrôles quotidiens, avec liste des écarts :

- solde des comptes `2x` = total des valeurs brutes des fiches, par compte ; idem `28x` et `29x` ;
- écriture sur un compte de classe `2`, `28` ou `29` sans fiche rattachée ;
- fiche `DRAFT` de plus de N jours (facture comptabilisée, fiche non complétée) ;
- bien reçu non mis en service depuis plus de N jours ;
- dotation manquante pour une fiche en service sur une période ;
- solde `481` non lettré au-delà de l'échéance, `485` non encaissé.

À la **clôture de l'exercice**, la liste de clôture ([etats-financiers.md](etats-financiers.md#6-états-dinventaire-et-de-clôture)) exige :

1. toutes les dotations de l'exercice comptabilisées ;
2. un écart nul entre le fichier et la comptabilité pour chaque compte ;
3. la revue des indices de dépréciation faite et signée ;
4. une campagne d'inventaire physique close dans l'exercice, avec son PV ;
5. les immobilisations en cours justifiées (chantier réel, factures rattachées) ;
6. les amortissements dérogatoires calculés pour le tableau de passage fiscal.

Le tableau des immobilisations et le tableau des amortissements sont alors figés avec les états financiers définitifs.

---

## 13. Pièces émises

Ajoutées aux [pièces comptables](pieces-comptables.md#préfixes-proposés), avec les règles communes (numérotation continue, QR, signatures, duplicata).

| Pièce | Préfixe | Contenu |
| --- | --- | --- |
| PV de mise en service | `PMS` | Bien, composants, date, lieu, ressource, détenteur, photo, signatures du réceptionnaire et du responsable |
| Fiche de remise de matériel | `PRM` | Bien, état, accessoires, engagement de restitution, signatures de l'employé et du remettant ; restitution sur la même fiche |
| PV de sortie d'immobilisation | `PSI` | Bien, motif, date, valeur brute, cumul, valeur nette, prix, plus ou moins-value, justificatif, deux signatures |
| PV d'inventaire physique | `PIP` | Périmètre, équipe, attendus, vus, manquants, trouvés, décisions, signatures |

Le code d'un bien (`IMM-`) n'est pas une pièce : il suit sa propre séquence, sans exigence de continuité, mais n'est jamais réutilisé.

---

## 14. Données

| Table | Colonnes principales |
| --- | --- |
| `fa_category` | code, libellé, compte du bien, compte d'amortissement, compte de dotation, compte de dépréciation, mode, durée, coefficient, seuil, suivi par unité ou par lot, composants types, actif |
| `fixed_asset` | code `IMM-`, company_code, parent_id (composant), catégorie, désignation, marque, modèle, série, quantité, mode d'entrée, fournisseur, supplier_invoice_id, fund_request_id, justification_line_id, acquisition_date, in_service_date, entry_value, residual_value, accounting_status, operational_status, organisation, site, resource_code, location_label, holder_employee_id, analytic_sections, warranty_end, insurer, policy, insured_value, tracked_off_books |
| `fixed_asset_value_line` | asset_id, type (`PURCHASE`, `ACCESSORY_COST`, `NON_RECOVERABLE_VAT`, `CREDIT_NOTE`, `SUBSEQUENT_EXPENSE`), montant, origine (ligne de facture, de justification), entry_number |
| `depreciation_plan` | asset_id, version, kind (`ACCOUNTING`, `TAX`), mode, durée, taux, coefficient, base, valid_from, reason, created_by |
| `depreciation_schedule` | plan_id, période, dotation prévue, dotation comptabilisée, cumul, valeur nette, entry_number, status (`PLANNED`, `POSTED`, `SKIPPED_LOCKED`, `CAUGHT_UP`) |
| `fixed_asset_event` | asset_id, type (mise en service, transfert, affectation, restitution, révision, dépréciation, reprise, reclassement, entretien, panne, sortie), date, avant, après, motif, pièce émise, entry_number, auteur |
| `fixed_asset_disposal` | asset_id, motif, date, valeur brute, cumul, dépréciation, valeur nette, prix, résultat, billing_document_id, PV `PSI-` |
| `fixed_asset_impairment` | asset_id, date, indice, valeur actuelle, montant, reprise, justificatif |
| `fixed_asset_inventory_campaign` | périmètre, dates, équipe, statut, PV `PIP-` |
| `fixed_asset_inventory_scan` | campagne, asset_id ou bien non inscrit, lieu constaté, scanned_by, scanned_at, photo |
| `fixed_asset_maintenance` | asset_id, contrat, prestataire, date, coût, sous garantie, supplier_invoice_id |

Liens avec l'existant : `Resource.code` et `ResourceAmenityLink` (module `ressource`), documents de la GED, `supplier_invoice` (lot F13), `fund_request_line` et `expense_justification_line` (lots F5, F6), `billing_document` pour la facture de cession.

---

## 15. Routes

| Route | Usage |
| --- | --- |
| `GET`, `POST /accounting/fixed-assets/categories` | Catégories |
| `GET /accounting/fixed-assets?status=&category=&site=&resource=&holder=` | Fichier des immobilisations, filtres |
| `POST /accounting/fixed-assets` | Créer une fiche à la main (rare : la plupart viennent d'une pièce) |
| `GET`, `PATCH /accounting/fixed-assets/{code}` | Lire, compléter une fiche `DRAFT` |
| `POST /accounting/fixed-assets/{code}/components` | Découper en composants |
| `POST /accounting/fixed-assets/{code}/commission` | Mise en service, PV `PMS-` |
| `GET /accounting/fixed-assets/{code}/plan?kind=` | Plan d'amortissement, versions |
| `POST /accounting/fixed-assets/{code}/revisions` | Révision du plan |
| `POST /accounting/fixed-assets/{code}/transfers`, `/assignments`, `/returns` | Transfert, affectation, restitution |
| `POST /accounting/fixed-assets/{code}/impairments` | Dépréciation, reprise |
| `POST /accounting/fixed-assets/{code}/disposals` | Sortie, PV `PSI-` |
| `GET /accounting/fixed-assets/{code}/sheet.pdf`, `/label.pdf` | Fiche imprimable, étiquette |
| `POST /accounting/fixed-assets/labels` | Planche d'étiquettes pour une sélection |
| `GET /accounting/fixed-assets/depreciation-runs/preview?period=` | Simulation des dotations |
| `POST /accounting/fixed-assets/depreciation-runs` | Lancer les dotations d'une période (sinon automatique) |
| `POST`, `GET /accounting/fixed-assets/inventories` | Campagnes d'inventaire |
| `POST /accounting/fixed-assets/inventories/{id}/scans` | Scan d'un QR (page mobile) |
| `POST /accounting/fixed-assets/inventories/{id}/close` | Clôture de la campagne, PV `PIP-` |
| `GET /accounting/fixed-assets/reports/{report}` | États de la section 11 (PDF, Excel, CSV) |

---

## 16. Droits

| Permission | Pour |
| --- | --- |
| `FIXED_ASSET:READ` | Consulter le fichier, les fiches, les états (comptable, moyens généraux, direction, auditeur) |
| `FIXED_ASSET:MANAGE` | Compléter les fiches, découper en composants, mettre en service, transférer, affecter, enregistrer l'entretien (moyens généraux, comptable) |
| `FIXED_ASSET:REVISE` | Réviser un plan, reclasser (comptable, avec motif) |
| `FIXED_ASSET:IMPAIR` | Déprécier et reprendre (comptable ; seconde validation du responsable financier au-delà du seuil) |
| `FIXED_ASSET:DISPOSE` | Préparer une sortie ; la validation du PV exige une seconde personne avec le même droit |
| `FIXED_ASSET:DEPRECIATION_RUN` | Lancer ou relancer les dotations d'une période |
| `FIXED_ASSET:INVENTORY` | Ouvrir et clore une campagne ; le scan n'exige que d'être membre de l'équipe de la campagne |
| `FIXED_ASSET_SETTINGS:MANAGE` | Catégories, seuils, coefficients, rythme des dotations (administrateur finance) |

Séparation des tâches : celui qui prépare une sortie ne la valide pas ; celui qui détient un bien ne le scanne pas lors de l'inventaire qui le concerne (alerte, pas blocage, pour les petites équipes).

---

## 17. Paramétrage

| Paramètre | Valeur initiale |
| --- | --- |
| Catégories, comptes, durées, modes | Tableau de la section 2 |
| Seuil d'immobilisation | 100 000 XAF HT par unité, par catégorie (à valider avec l'expert-comptable) |
| Suivi hors comptabilité des petits biens | Activé pour le mobilier et le matériel informatique |
| Rythme des dotations | Mensuel (`MONTHLY`) ; `QUARTERLY`, `YEARLY` disponibles |
| Jour et heure du traitement des dotations | 1er du mois, 03 h 00 |
| Journal des dotations et des opérations sur immobilisations | `OD` (journal `IM` dédié en option) |
| Base du prorata temporis | Année de 360 jours, mois de 30 jours |
| Coefficients du dégressif | 1,5 (3 à 4 ans), 2 (5 à 6 ans), 2,5 (au-delà), par pays |
| Mise en service par défaut | Date de réception, PV exigé |
| Amortissement fiscal distinct | Désactivé ; activable par catégorie |
| Seuil de seconde validation des dépréciations | 1 000 000 XAF |
| Délais d'alerte | Fiche `DRAFT` : 15 jours ; bien non mis en service : 30 jours ; fin de garantie : 30 jours avant ; bien inutilisé : 6 mois |
| Inventaire physique exigé avant clôture | Oui, une campagne par exercice |
| Modèle d'étiquette | 38 x 21 mm, 65 par planche A4 ; code, désignation courte, QR |
| Retrait de l'équipement de la ressource à la sortie du bien | Proposé, à confirmer |

---

## 18. Lots et critères d'acceptation

Le volet devient un lot à part, **F18**, détaché de F13 qui garde les fournisseurs et les achats.

| Lot | Contenu | Dépend de | Critère d'acceptation |
| --- | --- | --- | --- |
| **F18. Immobilisations et amortissements** | Catégories, fiches pré-remplies depuis les factures, demandes et justifications, composants, en cours, mise en service et PV, plans linéaire, dégressif et par unités d'œuvre avec versions, dotations mensuelles automatiques ventilées par section, dérogatoire, révisions, dépréciations, transferts, affectations, entretien, sorties avec PV et facture de cession, étiquettes QR, inventaire physique par scan, états et notes 3A à 3E, contrôles fichier et comptabilité, lien avec les ressources | F1, F2, F9, F13 (factures fournisseurs) ; F5 et F6 pour les entrées par avance ; F17 pour la location-acquisition | Une facture fournisseur d'un ordinateur de 900 000 XAF HT crée une fiche pré-remplie ; après mise en service au 1er avril, la dotation mensuelle est de 25 000 XAF et va sur la section de la ressource ; relancer les dotations ne double rien ; la cession produit les écritures de la section 8.2 et une moins-value de 25 000 ; à tout moment, le fichier égale les comptes `2x` et `28x` et la clôture est refusée sinon ; une campagne de scan signale un bien non vu et produit son PV |
