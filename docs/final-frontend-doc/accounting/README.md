# Module comptabilité (`accounting`)

Comptabilité générale SYSCOHADA révisé (système normal), tenue par le logiciel à partir des faits de la plateforme : ventes, encaissements, caisses, banque, achats, demandes de fonds, paie, immobilisations. Elle couvre aussi l'analytique et le budget, la clôture d'exercice, les états financiers, la liasse fiscale, les états de gestion, les exports vers l'expert-comptable et la reprise.

Lire d'abord le [README général](../README.md) (connexion, erreurs, pagination), puis ce fichier, puis le fichier du domaine visé. Le contrat exact de chaque route est dans [routes.md](routes.md) et `routes/<Contrôleur>.md`.

## 1. Ce qu'il faut savoir avant de coder un écran

**Activation**
- La comptabilité s'active par un paramètre (`accounting.enabled`, domaine Comptabilité de l'écran des paramètres). Désactivée, aucune écriture automatique ne part, mais les écrans de paramétrage, de plan de comptes et de saisie restent utilisables.
- À la bascule, les faits depuis le 1er janvier 2026 sont rejoués en **brouillard**, contrôlés, puis validés en bloc ([imputation.md](imputation.md)).

**Pièces comptables**

| Statut | Sens à l'écran |
| --- | --- |
| `DRAFT` | Brouillard : modifiable, supprimable, numéro provisoire `BR-...` |
| `PENDING_BALANCE` | En attente d'équilibrage (pièce automatique déséquilibrée ou incomplète) : à corriger, motif affiché |
| `VALIDATED` | Validée : numéro définitif, ne se modifie plus ; on la contre-passe |
| `REVERSED` | Contre-passée : garde son numéro, la contre-passation est liée |
| `REJECTED` | Rejetée, avec motif |

- Une pièce validée porte un numéro continu par journal et par exercice, protégé par une chaîne d'empreintes.
- La partie double est contrôlée en base : une pièce déséquilibrée ne se valide jamais.

**Périodes et exercices**

| Statut | Effet |
| --- | --- |
| `OPEN` | Tout passe |
| `LOCKED` | Seule la saisie comptable passe ; les écritures automatiques vont sur la première période ouverte, avec leur date d'origine |
| `CLOSED` | Plus rien ne passe |

Un exercice clos (lot clôture) ne se rouvre pas.

**Montants et dates**
- Montants en nombres décimaux, devise de tenue `XAF` (franc, sans centimes affichés). Les montants en devise portent `currency`, `foreignAmount`, `rate`.
- Dates de pièce au format `AAAA-MM-JJ`, fuseau de l'exploitant (Africa/Brazzaville).

**États et exports**
- La plupart des états acceptent `format=json` (défaut), `csv` ou `pdf`. Le PDF porte la mention « PROVISOIRE » tant que l'exercice n'est pas clos.
- Les éditions définitives sont figées (données, PDF, empreinte) et se réimpriment à l'identique.

**Droits**
- Chaque route indique son droit dans [routes.md](routes.md). Les services contrôlent aussi les droits fins (par exemple, la revue d'un justificatif par une autre personne que son préparateur).
- Rôles : `SUPER_ADMIN` et `ADMIN` ont tout ; `FINANCE` tient la comptabilité ; `MANAGER` lit ; `AUDITOR` (commissaire aux comptes) lit et exporte.

## 2. Domaines

| Domaine | Fichier | Contenu |
| --- | --- | --- |
| Socle | [socle.md](socle.md) | Plan de comptes, comptes de tiers, exercices, périodes, journaux, saisie, validation, contre-passation, pièces en attente, lettrage, journal, grand livre, balance, continuité des numéros |
| Moteur d'imputation | [imputation.md](imputation.md) | Schémas d'imputation, file des faits, régénération, reprise en brouillard |
| Caisse comptable | [caisse-comptable.md](caisse-comptable.md) | Natures d'opération, journal par caisse, écarts de caisse |
| Ventes et encaissements | [ventes-encaissements.md](ventes-encaissements.md) | Factures, avoirs, paiements, portefeuilles, comptes de produit, lettrage clients, contrôles |
| Analytique | [analytique.md](analytique.md) | Axes, sections, ventilation, clés de répartition, états |
| Budget | [budget.md](budget.md) | Budget, engagements, contrôle des demandes, suivi |
| Banque | [banque-rapprochement.md](banque-rapprochement.md) | Comptes bancaires et mobile money, relevés, pointage, rapprochement, chèques, devises |
| Fiscalité | [fiscalite-tva.md](fiscalite-tva.md) | Déclarations de TVA, prorata, retenues à la source |
| Achats | [achats-fournisseurs.md](achats-fournisseurs.md) | Fournisseurs, commandes, factures et règlements fournisseurs, suggestion de fournisseurs |
| Financement et paie | [financement-hors-bilan-paie.md](financement-hors-bilan-paie.md) | Emprunts, comptes courants d'associés, engagements hors bilan, import de la paie |
| Immobilisations | [immobilisations.md](immobilisations.md) | Fiches, plans d'amortissement, profils de marque, dotations, sorties, inventaire physique, notes 3A à 3E |
| Provisions | [provisions-creances-douteuses.md](provisions-creances-douteuses.md) | Créances douteuses, dépréciations, pertes, risques et charges, politique de crédit des clients |
| Constatations d'avance | [constatations-avance.md](constatations-avance.md) | Échéanciers de reconnaissance, traitement de fin de période, factures à établir et non parvenues |
| Clôture | [cloture-etats-financiers.md](cloture-etats-financiers.md) | Liste de clôture, à-nouveaux, bilan, compte de résultat, tableau des flux, notes annexes, justificatifs de solde, confirmations de solde |
| Liasse et gestion | [liasse-gestion.md](liasse-gestion.md) | Résultat fiscal et impôt, liasse du modèle commun SYSCOHADA, soldes de gestion, tableau de bord, ratios, prévisionnel de trésorerie |
| Exports et reprise | [exports-reprise.md](exports-reprise.md) | Exports Sage 100, Sage X3, Odoo et CSV, reprise d'écritures et de soldes, double tenue, archive annuelle |

Les demandes de fonds, avances et pièces imprimables sont dans le module [fund](../fund/README.md). Les caisses (sessions, comptages, transferts) sont dans [payment](../payment/README.md).

## 3. Menu proposé

| Entrée | Écrans | Domaines |
| --- | --- | --- |
| Tableau de bord | Indicateurs, contrôles en échec, pièces en attente | liasse-gestion, socle |
| Saisie | Nouvelle pièce, brouillard, pièces en attente, contre-passation | socle |
| Tiers | Comptes clients, fournisseurs, personnel ; lettrage ; relevés ; justificatifs ; confirmations | socle, ventes-encaissements, cloture-etats-financiers |
| Trésorerie | Banques, relevés, pointage, rapprochements, chèques, devises, prévisionnel | banque-rapprochement, liasse-gestion |
| Achats | Fournisseurs, commandes, factures, règlements | achats-fournisseurs |
| Immobilisations | Fiches, dotations, inventaire, états | immobilisations |
| Analytique et budget | Axes, sections, budget, suivi | analytique, budget |
| Fin de période | Constatations d'avance, provisions, TVA, verrouillage des périodes | constatations-avance, provisions-creances-douteuses, fiscalite-tva |
| Clôture | Liste de clôture, à-nouveaux, états financiers, notes, liasse, éditions définitives | cloture-etats-financiers, liasse-gestion |
| États | Journal, grand livre, balance, centralisateur, soldes de gestion, ratios | socle, liasse-gestion |
| Échanges | Exports, reprise, double tenue, archive | exports-reprise |
| Paramétrage | Plan de comptes, journaux, schémas, correspondances, paramètres du domaine Comptabilité | socle, imputation, cloture-etats-financiers |

## 4. Parcours types

**Fin de mois**
1. Valider les pièces en attente.
2. Pointer la banque et valider le rapprochement.
3. Lancer le traitement des constatations d'avance (simulation, puis comptabilisation).
4. Passer les dotations aux amortissements.
5. Revoir les provisions.
6. Préparer la déclaration de TVA.
7. Verrouiller la période.

**Clôture d'exercice**
1. Liste de clôture : corriger chaque contrôle en échec, cocher les étapes manuelles, ou les lever par dérogation motivée.
2. Simuler les à-nouveaux, puis clore.
3. Éditer en définitif le bilan, le compte de résultat, le tableau des flux et la liasse.
4. Produire l'archive annuelle.

**Expert-comptable**
1. Régler une fois la correspondance des comptes et journaux de son logiciel.
2. Exporter les écritures du mois ou la balance.
3. Pendant la double tenue, déposer sa balance pour comparer.

## 5. Paramètres

Tous les paramètres sont dans le domaine **Comptabilité** de l'écran des paramètres ([parametres](../parametres/README.md)). La plupart se lisent à chaud. Chaque fichier de domaine liste les siens.
