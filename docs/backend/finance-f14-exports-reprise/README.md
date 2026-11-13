# Lot F14 : exports, archive annuelle et reprise

Spécification : `docs/backend/plan-finance-comptabilite/README.md` (sections 13 et 17, décisions 6 et 9) et `etats-financiers.md` (section 10). Branche `feature/finance-comptabilite`.

## Migrations

- Dev `V323__exports_reprise_f14.sql`, prod `V317__exports_reprise_f14.sql`, même contenu.
- Tables :
  - `acc_export_mapping` : correspondance d'un compte ou d'un journal vers le logiciel cible, par format ;
  - `acc_export_batch` : lots d'export (format, mode, période, nombres, totaux, pièces exclues, empreinte) ;
  - `acc_archive` : archives annuelles (fichier, taille, empreinte, sommaire).
- Droits :
  - `ACCOUNTING_EXPORT:RUN` : `SUPER_ADMIN`, `ADMIN`, `FINANCE`, `AUDITOR` ;
  - `ACCOUNTING_EXPORT:MAPPING`, `ACCOUNTING_IMPORT:RUN` : `SUPER_ADMIN`, `ADMIN`, `FINANCE` ;
  - `ACCOUNTING_ARCHIVE:RUN` : `SUPER_ADMIN`, `ADMIN`.

## Exports

Deux modes :
- `ENTRIES` : le fichier des écritures d'une période (option : un journal) ;
- `BALANCE` : une seule pièce au dernier jour, avec le solde de chaque compte (par tiers pour les comptes de tiers). Réimportée dans le logiciel cible, elle redonne la même balance.

Règles communes :
- seules les pièces validées partent, hors classe 9 (analytique et hors bilan) ;
- une pièce acceptée déséquilibrée est exclue des formats cibles, qui la refuseraient (le lot compte les exclusions). Le CSV la garde, marquée ;
- comptes et journaux passent par la table de correspondance du format. À défaut, le compte est complété par des zéros à la longueur du format.

| Format | Fichier |
| --- | --- |
| `SAGE100` | Import paramétrable de Sage 100 Comptabilité : une ligne par mouvement, point-virgule, sans en-tête, Windows-1252. Colonnes : journal, date JJMMAA, pièce (13), compte général (8 chiffres par défaut), compte tiers (17), libellé (35), débit, crédit (virgule décimale), échéance, référence |
| `SAGEX3` | Import à deux types d'enregistrement : `G` (type de pièce, site, journal, date AAAAMMJJ, numéro, libellé, devise) puis `D` (numéro de ligne, compte, tiers, libellé, sens 1 ou -1, montant, échéance). Le modèle d'import se déclare dans X3 avec ces champs ; type et site sont des paramètres |
| `ODOO` | Import des pièces (`account.move`) : la première ligne d'une pièce porte son identifiant externe `bokati_...`, son journal, sa date et sa référence ; les suivantes seulement les colonnes `line_ids/...`. Virgule, point décimal, UTF-8 |
| `CSV` | Toutes les colonnes, UTF-8 avec BOM, point-virgule, marqueur des pièces déséquilibrées |

Chaque export crée un lot `EXP-{format}-{rang}` avec son empreinte SHA-256. Le numéro est renvoyé dans l'en-tête `X-Export-Batch`.

## Reprise et bascule

Fichiers CSV avec en-tête, point-virgule ou virgule. Dates `AAAA-MM-JJ`, `JJ/MM/AAAA` ou `JJMMAA` ; montants à point ou à virgule, espaces tolérées. UTF-8, ou Windows-1252 à défaut.

| Outil | Colonnes | Effet |
| --- | --- | --- |
| Import d'écritures | `journal;date;piece;compte;tiers;libelle;debit;credit;reference;echeance` | Une pièce par journal et numéro, en brouillard, validée si demandé, dans l'ordre des dates. Chaque pièce a sa propre transaction : une pièce en erreur reste au brouillard, signalée, sans arrêter le fichier |
| Soldes d'ouverture | `compte;tiers;reference;echeance;debit;credit;libelle` | Une pièce au premier jour de l'exercice, dans le journal des à-nouveaux. Une ligne par pièce ouverte pour les comptes lettrables, pour que le lettrage continue |
| Comparaison (double tenue) | `compte;debit;credit` ou `compte;solde` | Balance à une date comparée à celle de l'autre logiciel, nos comptes passés par la correspondance du format choisi : écarts par compte |

## Archive annuelle

ZIP contenant :
- `ecritures.csv` : fichier complet des écritures de l'exercice ;
- `balance.csv` ;
- `etats/` : bilan, compte de résultat et tableau des flux (JSON et CSV) ;
- `liasse/liasse.xls` ;
- `notes.json` ;
- `editions/` : éditions définitives, données et PDF figés ;
- `lettrages.csv` ;
- `pieces-justificatives.csv` : chaque pièce, son origine et ses pièces jointes ;
- `manifest.json` : exercice, auteur, date, et l'empreinte SHA-256 de chaque fichier.

L'archive est tracée (`ARC-{exercice}-{rang}`) avec son empreinte, renvoyée dans l'en-tête `X-Archive-Sha256`. À conserver 10 ans hors de l'application.

## Routes (`/sni/api/v1`)

| Route | Usage |
| --- | --- |
| `POST /accounting/exports` `{format, mode, from, to, journal}` | Export (fichier joint) |
| `GET /accounting/exports` | Derniers lots d'export |
| `GET /accounting/exports/mappings?format=`, `POST`, `DELETE /accounting/exports/mappings/{id}` | Correspondance des comptes et journaux |
| `POST /accounting/imports/entries?validate=` (fichier `file`) | Import d'écritures |
| `POST /accounting/imports/opening/{year}` (fichier `file`) | Soldes d'ouverture |
| `POST /accounting/imports/compare?date=&format=` (fichier `file`) | Comparaison de balances |
| `POST /accounting/archives/{year}` | Produire l'archive (ZIP) |
| `GET /accounting/archives/{year}` | Archives produites |

## Paramètres (`ACCOUNTING`, lus à chaud)

| Clé | Défaut |
| --- | --- |
| `accounting.export.sage100.account-length` | `8` |
| `accounting.export.sagex3.account-length`, `entry-type`, `site` | `0`, `OD`, vide |
| `accounting.export.odoo.account-length` | `0` |

## Non fait dans ce lot

- **Format natif Sage 100 (PNM) et modèle X3 de l'éditeur** : les fichiers suivent un import paramétrable, à déclarer une fois dans chaque logiciel avec les colonnes ci-dessus. Le réglage exact se fera avec l'expert-comptable, sur son logiciel.
- **Conservation de l'archive dans la GED** : l'archive se télécharge ; son empreinte est gardée.
- **Reprise des anciennes demandes de caisse et correspondance des catégories de flux** : elles relèvent des lots de caisse (C1 à C3).

## Tests

`ExportF14IT`, sur un exercice 2023 isolé :
- soldes d'ouverture importés (montants avec espaces et virgules, date JJ/MM/AAAA) ;
- trois pièces importées : deux validées, une restée au brouillard et signalée (compte inconnu) ;
- Sage 100 : 9 lignes de 10 colonnes, comptes sur 8 chiffres, journal `OD` converti en `ODV`, débits égaux aux crédits ;
- Odoo : en-tête et identifiants sur la première ligne de chaque pièce ;
- Sage X3 : 3 en-têtes `G`, 9 lignes `D` ;
- CSV : colonne du marqueur ;
- **critère du plan** : la balance exportée en Sage 100 est comparée à nos soldes sans écart, et un écart volontaire est détecté ;
- archive : sommaire, fichiers et empreintes.

## Pour le frontend

Écrans à prévoir :
- export : format, mode, période, journal, puis téléchargement ; historique des lots ;
- correspondance des comptes et journaux par format ;
- reprise : dépôt des fichiers, résultat pièce par pièce, erreurs ;
- double tenue : dépôt de la balance externe, écarts par compte ;
- archive annuelle : production, téléchargement, historique avec empreintes.
