# Constat : comptabilité

> Partie du [constat des listes de choix](README.md). Relevé du 18/11/2026 sur `feature/booking` (lecture du code, sans modification). Chemins relatifs à `src/main/java/com/sni/bokaticowork/features/accounting/` ; `AR` = `dto/AccountingRequests.java`, `PD` = `dto/PostingDtos.java`. Toutes les routes sont sous `/api/v1`.

## En bref

215 constats sur 17 contrôleurs et toutes les requêtes :

| Nature | Nombre | Constat |
| --- | --- | --- |
| Valeurs fixes non exposées | **58** | Aucune route `/accounting/**` ne rend des valeurs avec libellés. 17 énumérations, libellés français seulement en commentaire. Environ 40 ensembles cachés dans les services (`Set.of`, `List.of`, `switch`), certains même pas contrôlés : énumérations des emprunts, `BankAccount.kind`, `AccPayrollRubric.kind`, sens des règles bancaires. |
| Recherche de choix manquante | **73** | Aucune recherche paginée avec `q` dans le module (seul `Pageable` : `AccPostingEventRepository`). Les listes partielles (comptes avec `search`, journaux, sections, fournisseurs…) ne sont pas paginées et rarement filtrées. **Aucune recherche des tiers.** |
| Texte libre à mettre en liste | **37** | Surtout des motifs, familles, marques, sites, assureurs, types de retenue, conditions de paiement, codes de retraitement fiscal. Seule table de vocabulaire : `acc_unplanned_reason`, lue par une requête brute, sans administration. |
| Recherche manquante | **16** | Écritures (journal, statut, dates seulement, mois courant par défaut), fournisseurs, commandes et factures (aucun filtre), immobilisations (pas de recherche texte), tiers (aucune route). |
| Automatisation manquante | **31** | Codes « compte + 1 », compte et journal de trésorerie saisis au lieu d'être déduits du compte bancaire, propositions de lettrage seulement sur 411, pas d'écritures récurrentes, plusieurs passages manuels (TVA, intérêts des associés, intérêts courus des emprunts, relances des confirmations). |

**Entités prises telles quelles comme corps de requête, presque sans contrôle** : `BankAccount`, `BankImportTemplate`, `BankAssignmentRule`, `AccSupplier`, `AccCommitmentType`, `AccPayrollTemplate`, `AccPayrollRubric`, `AccPayrollEmployee`.

## Mécanismes et routes proposés

| Repère | Proposition |
| --- | --- |
| **[ENUMS]** | `GET /accounting/reference/enums[/{group}]`, copie de `/inventory/reference/enums` (valeur, libellé, description, sélectionnable), mis en cache. Un seul registre sert à la fois à l'exposition et au contrôle. |
| **[LISTE:X]** | Liste de l'administrateur sur le modèle des valeurs de référence de la caisse (`listCode, code, label, sortOrder, active, requiresText, systemValue, usageCount`, « Autre, préciser »), idéalement promu au socle (voir [README](README.md)). |
| **[CATALOGUE:X]** | Table à attributs avec mode souple ou strict, sur le modèle des listes du catalogue de facturation : natures de risque (avec comptes), types de retenue (taux, base, compte), types de composants d'immobilisation, marques, conditions de paiement, codes de retraitement fiscal, profils saisonniers. |
| **[RECH:type]** | `GET /accounting/lookup/{type}?q=&page=&size=&includeInactive=false` rendant `[{code, label, hint, active}]` : comptes (filtres classe, trésorerie, lettrable, journal), journaux, sections, fournisseurs, lignes budgétaires, immobilisations, emprunts, comptes bancaires. |
| **[TIERS]** | `GET /accounting/third-parties?q=&type=&accountNumber=&page=` : union des comptes de tiers, fournisseurs, membres, salariés et associés, avec solde. |

## Constats par domaine

### Grand livre (`AccountingController`, AR)

| Champ ou route | Nature | Constat | Proposition |
| --- | --- | --- | --- |
| `parentNumber`, `treasuryAccountNumber`, `allowedAccounts`, `EntryLine.accountNumber`, `Letter.accountNumber` | Recherche | `/accounts?search=` sans pagination, charge tout le plan, ignore les comptes admis du journal, pas de filtre trésorerie ou lettrable | [RECH:accounts] avec ces filtres |
| `thirdPartyType`, `treasuryType`, `JournalType`, `NumberingReset` | Valeurs fixes | Non exposés | [ENUMS] |
| `taxCode` (compte, ligne), `taxBase` | Recherche | Libres, jamais contrôlés (`AccountingChartService:143`) | `/billing/tax-rules` ; défaut depuis le compte ; base calculée |
| `statementLineCode` | Recherche | Libre, aucun catalogue des lignes des états | `GET /accounting/statements/{template}/lines` |
| `cashRegisterCode` | Recherche | Libre | `/cash-registers` |
| `EntryLine.thirdPartyCode`, `Letter.thirdPartyCode` | Recherche | `/accounts/{n}/third-parties` sans `q` | [TIERS] |
| `EntryLine.currency`, `rate` | Recherche et automatisation | Devise libre ; taux saisi alors que la conversion existe | Liste des devises ; taux par défaut à la date |
| `dueDate`, `analytic` des lignes | Automatisation | Saisis ; règles analytiques seulement à la comptabilisation | Échéance depuis les conditions du tiers ; `GET /accounting/analytic/suggest` |
| `Entry.journalCode` | Recherche | `/journals` sans filtre actif ou type | Filtres ou [RECH:journals] |
| Libellés d'écriture et de ligne, `sourceType` | Texte en liste | Ressaisis ; vocabulaire libre aussi utilisé par les règles analytiques | [LISTE:ACC_ENTRY_LABEL], [LISTE:ACC_SOURCE_TYPE] ; libellé recopié sur les lignes |
| `sourceCode` | Recherche | Référence libre | Recherche des documents |
| Motifs (compte, contre-passation, rejet) | Texte en liste | Saisis | [LISTE:ACC_ACCOUNT_CHANGE_REASON], `ACC_REVERSAL_REASON`, `ACC_REJECT_REASON` |
| Code d'exercice, code de journal | Automatisation | Saisis | Proposés (année de fin, type et séquence) |
| Modèles d'écriture | Automatisation | Absents | `/accounting/entry-templates` et écritures récurrentes mensuelles |
| Lettrage | Automatisation | Propositions seulement sur 411 | Propositions sur tout compte lettrable ; lettrage automatique 401, 47x |
| `GET /accounting/entries` | Recherche | Journal, statut, dates ; mois courant ; non paginé | `q` (numéro, libellé, référence), compte, tiers, montants, origine, pagination |
| `/accounts`, `/periods`, `/journals` | Recherche | Listes complètes | Pagination et filtres |

### Règles de comptabilisation, caisse, ventes

| Champ ou route | Nature | Constat | Proposition |
| --- | --- | --- | --- |
| `RuleRequest.eventType`, `family`, `SubmitEvent.eventType` | Valeurs fixes | Libres ; types connus seulement du code | `GET /accounting/posting/event-types` (famille, libellé, exemple) |
| `RuleRequest.journal` | Recherche | Liste partielle | [RECH:journals] |
| Motif d'ignorance d'un événement | Texte en liste | Saisi | [LISTE:ACC_EVENT_IGNORE_REASON] |
| Codes de flux de trésorerie | Valeurs fixes | FF à FQ sans libellés | [ENUMS] |
| `/posting/events`, `/posting/rules` | Recherche | `limit` ou type seulement | `q`, statut, famille, pagination |
| Journal de caisse (type, libellé) | Valeurs fixes et automatisation | Saisis | Sous-ensemble des types ; libellé « Caisse … » par défaut |

### Banque (`BankController`, `BankService`, `ChequeService`)

| Champ ou route | Nature | Constat | Proposition |
| --- | --- | --- | --- |
| `BankAccount.kind`, sens des règles d'affectation, format, séparateurs, format de date, encodage des modèles d'import | Valeurs fixes | Chaînes non contrôlées | [ENUMS] et contrôle |
| `bankName` | Texte en liste | Saisi | Liste de caisse `BANK` (existe) |
| Devise, compte et journal du compte bancaire | Recherche | Libres (créés à blanc s'ils manquent) | Devises ; [RECH:accounts] classe 5 |
| Compte et tiers des règles et des lignes comptabilisées, facture rapprochée | Recherche | Libres ; aucune recherche d'élément ouvert par montant | [RECH], [TIERS], `GET /accounting/open-items?q=&amount=` |
| Règle à appliquer, rapprochement | Automatisation | Choisie par l'utilisateur ; rapprochement automatique exact seulement | Règle trouvée rendue par ligne de relevé ; suggestions notées |
| Solde d'ouverture d'un relevé manuel | Automatisation | Saisi | Solde de clôture du relevé précédent |
| Motifs de rejet de chèque, d'annulation de rapprochement | Texte en liste | Saisis | [LISTE:ACC_CHEQUE_REJECT_REASON], `ACC_UNMATCH_REASON` |
| Compte bancaire du rejet de chèque | Automatisation | Saisi | Depuis le bordereau |
| `/cheques` (lecture qui écrit : synchronisation), bordereaux, relevés, rapprochements | Recherche | Pas de filtres ni de pagination | `q`, statut, dates, pagination ; lecture sans écriture |
| Préparation du rapprochement | Automatisation | Manuelle | Brouillon préparé en fin de mois |

### Analytique

| Champ ou route | Nature | Constat | Proposition |
| --- | --- | --- | --- |
| `requiredOn`, `costBehavior` | Valeurs fixes | Ensembles cachés | [ENUMS] |
| Section par défaut, axe, journal, section, clé des règles, comptes d'une section | Recherche | Arbre sans `q` | [RECH:sections] et variantes |
| Famille de section, `sourceType` des règles, motif de reclassement | Texte en liste | Libres | Listes de l'administrateur |
| Responsable de section, tiers des règles | Recherche | Saisis | Recherche d'utilisateur, [TIERS] |
| Pourcentages des clés | Automatisation | Saisis | Calcul depuis des inducteurs (surface, effectif, chiffre d'affaires) |
| Reclassement par identifiant de ligne | Recherche | Identifiant brut | Recherche des lignes d'écriture |
| Répartition par période | Automatisation | Manuelle | Planifiée au verrouillage de la période |

### Budget

| Champ ou route | Nature | Constat | Proposition |
| --- | --- | --- | --- |
| Type de poste, niveau de contrôle, base de création, récurrence et priorité des prévisions, statut de prévision, décisions et actions du chemin | Valeurs fixes | Ensembles cachés et chaînes magiques | [ENUMS] ; actions possibles rendues avec chaque vue |
| Responsables, natures, comptes, parent, fournisseur de prévision | Recherche | Saisis | Utilisateurs, natures de caisse, [RECH] |
| Lignes de transfert | Recherche | Identifiants bruts | `GET /budget/lines/lookup?q=` (avec disponible) |
| Motifs de révision et de transfert | Texte en liste | Saisis | Listes |
| Profil saisonnier (12 poids saisis) | Texte en liste | Saisi | Profils nommés administrés |
| Motifs hors budget | Texte en liste | Table en lecture, sans administration | Passés en liste administrée |
| `/budget/items`, `/transfers`, `/forecasts` | Recherche | Sans filtres | `q`, statut, pagination |

### Achats

| Champ ou route | Nature | Constat | Proposition |
| --- | --- | --- | --- |
| Code fournisseur | Automatisation | « FRN- » plus compte + 1 : collision après suppression | Générateur de séquences |
| Base de retenue, statut, type de facture | Valeurs fixes | Chaînes | [ENUMS] |
| Retenue (libellé, taux, compte) par fournisseur | Texte en liste | Saisie | [CATALOGUE:ACC_WITHHOLDING_TYPE] |
| Délai de paiement | Texte en liste | Nombre saisi | [LISTE:ACC_PAYMENT_TERMS] (30 j, 45 j fin de mois…) |
| Comptes du fournisseur, coordonnées bancaires | Recherche et texte | Libres ; texte | [RECH:accounts] ; banque de la liste `BANK` et RIB |
| Fournisseur, section, nature, compte, taux de TVA, immobilisation des commandes ; commande et facture d'origine ; besoin | Recherche | Libres ou listes partielles | Recherches filtrées (fournisseur, statut) |
| Compte et journal de paiement | Automatisation | Paire saisie | `bankAccountCode`, les deux déduits |
| Motif d'annulation de commande | Texte en liste | Saisi | Liste |
| `/purchases/suppliers`, `/orders`, `/invoices` | Recherche | Aucun filtre | `q`, statut, fournisseur, dates, échéance, pagination |
| Paiements | Automatisation | Une facture à la fois, pas de relance | Proposition de règlement par échéance ; relances |

### Immobilisations

| Champ ou route | Nature | Constat | Proposition |
| --- | --- | --- | --- |
| Méthodes, suivi (`LOT` sinon unité sans bruit), mode d'entrée, motifs de révision et de cession, nature de maintenance, état d'usage, dépréciation, décisions d'inventaire | Valeurs fixes | Ensembles cachés | [ENUMS] |
| Huit comptes de catégorie, compte de contrepartie, compte de l'assureur | Recherche | Libres | [RECH:accounts] par classe |
| Types de composants, marque, modèle | Texte en liste | Saisis ; une faute casse la résolution du profil | [CATALOGUE:ACC_FA_COMPONENT_TYPE], `ACC_FA_BRAND` |
| Catégorie, fournisseur (nom ressaisi), facture, commande, écriture, ressource, articles d'inventaire, détenteur, prestataire, acheteur, emprunt du crédit-bail | Recherche | Libres | Recherches dédiées ou celles des modules (ressources, inventaire, utilisateurs) |
| Site, organisation, assureur, état, indicateur de dépréciation, motifs de mouvement | Texte en liste | Saisis | Listes |
| Encaissement de cession (compte, journal), période des compteurs | Automatisation | Saisis | Compte bancaire ; période courante |
| `GET /fixed-assets` | Recherche | Pas de `q` (code, désignation, série), statut libre, non paginé | `q`, statut, pagination |

### Financement, hors bilan, paie

| Champ ou route | Nature | Constat | Proposition |
| --- | --- | --- | --- |
| Type de prêteur, taux, base de jours, différé, méthode, mode de comptabilisation | Valeurs fixes | Défauts seulement, **aucun contrôle** | [ENUMS] et contrôle |
| Prêteur (nom ressaisi), objet de l'emprunt | Recherche et texte | Libres | [TIERS] ou banques ; liste des objets |
| Cinq comptes de l'emprunt | Automatisation | Saisis | Défauts par type de prêteur |
| Codes d'emprunt, d'associé, d'engagement | Automatisation | Compte + 1 | Générateur de séquences |
| Remboursement anticipé, mouvement d'associé, sens d'engagement, liens d'engagement, nature de rubrique de paie, formats des modèles de paie | Valeurs fixes | Chaînes, parfois converties sans bruit | [ENUMS] |
| Tiers, comptes, sections, journaux, pièces | Recherche | Libres | [TIERS], [RECH], recherche des documents |
| Intérêts des associés, intérêts courus des emprunts | Automatisation | Passages manuels | Tâches de fin de période |
| Paiement de la paie (compte, journal) | Automatisation | Saisis | Compte bancaire |
| `/loans`, `/partners`, `/commitments`, `/payroll/imports` | Recherche | Non filtrés | `q`, statut, pagination |

### Clôture, états, confirmations

| Champ ou route | Nature | Constat | Proposition |
| --- | --- | --- | --- |
| Tiers, comptes et lignes d'état des sélections et correspondances | Recherche | Libres | [TIERS], [RECH], lignes d'état |
| Dérogations, motifs de renonciation | Texte en liste | Saisis | [LISTE:ACC_CLOSING_DEROGATION] |
| Rapports finaux, côté et colonne des correspondances, modèle, canal d'envoi | Valeurs fixes | Ensembles cachés | [ENUMS] |
| Destinataire des confirmations | Automatisation | Saisi | Contact du tiers |
| Relance des confirmations | Automatisation | Manuelle | Tâche : relance après N jours sans réponse |
| `/accounting/confirmations` | Recherche | Non filtré | Statut, compte, année, pagination |

### Charges et produits constatés d'avance, à payer, à recevoir

| Champ ou route | Nature | Constat | Proposition |
| --- | --- | --- | --- |
| Types d'événement, méthode de reconnaissance (repli sans bruit), nature de régularisation, sens des étalements | Valeurs fixes | Ensembles cachés | [ENUMS] |
| Motifs | Texte en liste | Saisis | [LISTE:ACC_DEFERRAL_REASON] |
| Tiers, compte, section, origine, référence d'usage | Recherche | Libres | [TIERS], [RECH], recherche des documents |
| Rapprochement d'une régularisation | Automatisation | Manuel | Suggestion quand une facture du fournisseur arrive |
| Factures non parvenues | Automatisation | Saisies à la clôture | Générées depuis les commandes reçues non facturées |

### Créances douteuses et provisions

| Champ ou route | Nature | Constat | Proposition |
| --- | --- | --- | --- |
| Décision, statut de classement, motif de passage en perte, segment, nature de risque (libellés en dur), probabilité, nature de provision | Valeurs fixes | Ensembles cachés | [ENUMS] ; natures de risque en catalogue ensuite |
| Motifs de classement, de taux, de retour, d'évaluation, de provision | Texte en liste | Saisis | [LISTE:ACC_RECEIVABLE_REASON], `ACC_PROVISION_REASON` |
| Factures et justificatifs du passage en perte, tiers, conseil | Recherche | Libres | Balance âgée avec `q`, documents, [TIERS] |
| Juridiction, méthode d'estimation, méthode des engagements de retraite | Texte en liste | Saisis | Listes |
| Échéance d'un risque | Automatisation | Enregistrée sans rappel | Avis avant l'échéance |
| `/risks`, `/receivables/write-offs`, `/proposals` | Recherche | Statut libre ou rien | `q`, statut, pagination |

### Résultat fiscal, TVA, exports

| Champ ou route | Nature | Constat | Proposition |
| --- | --- | --- | --- |
| Nature de retraitement, horizon de prévision, format et mode d'export, nature de correspondance | Valeurs fixes | Ensembles cachés | [ENUMS] |
| Code et libellé de retraitement | Texte en liste | Saisis | [CATALOGUE:ACC_TAX_ADJUSTMENT] (codes usuels, compte par défaut) |
| Compte du retraitement, journal et source des exports | Recherche | Libres | [RECH] |
| Déclaration de TVA | Automatisation | Préparée à la main | Brouillon mensuel planifié ; rappel d'échéance |
| `/vat-returns`, `/exports` | Recherche | Non filtrés | Année, statut |

## Priorités

1. **`GET /accounting/reference/enums[/{group}]`** (58 constats), un seul registre pour l'exposition et le contrôle. Ajouter les contrôles qui manquent : emprunts, banque, rubriques de paie, méthode de reconnaissance, suivi.
2. **Recherche des tiers** `GET /accounting/third-parties` : écritures, lettrage, banque, analytique, engagements, associés, risques, confirmations.
3. **Recherches paginées** `/accounting/lookup/{type}` (comptes avec filtres, journaux, sections, fournisseurs, lignes budgétaires, immobilisations, emprunts) ; `/accounts` paginé.
4. **Listes de motifs de l'administrateur** (mécanisme commun) ; migration des motifs hors budget.
5. **Recherche des écritures** : `q`, compte, tiers, montant, origine, pagination.
6. **Recherche des achats** : fournisseurs, commandes, factures, et sélecteurs filtrés par fournisseur.
7. **Listes des autres modules** au lieu du texte libre : taxes, natures de caisse, banques, ressources, utilisateurs, documents.
8. **Catalogues à attributs** (souple ou strict) : natures de risque, retenues, conditions de paiement, marques et composants, retraitements fiscaux, profils saisonniers.
9. **Trésorerie et numérotation** : compte bancaire unique d'où dériver compte et journal ; séquences au lieu de « compte + 1 » ; défauts (code d'exercice, de journal, taux, échéance).
10. **Rapprochements et tâches** :
    - lettrage proposé sur tout compte lettrable ;
    - rapprochement bancaire noté ;
    - factures non parvenues générées ;
    - écritures récurrentes ;
    - TVA, intérêts des associés, intérêts courus et relances planifiés.
