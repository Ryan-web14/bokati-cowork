# Rapprochement bancaire et comptes de tiers

Volet du [plan finance](README.md) consacré à deux justifications de solde :

- le **rapprochement bancaire** : prouver que le solde comptable de chaque compte de banque ou de mobile money correspond au relevé de l'établissement ;
- les **comptes de tiers** : chaque client, membre, organisation, fournisseur, employé ou associé a son propre compte, débité et crédité. On en tire un **état de compte imprimable** et un **justificatif de solde**.

Rédigé le 11/11/2026. Les comptes cités sont ceux du [plan de comptes](README.md#41-plan-de-comptes) ; les écritures, celles du [catalogue](catalogue-ecritures.md#10-banque-chèques-mobile-money). Tout se fait en partie double ([section 4.6](README.md#46-partie-double--règles-et-garanties)), et les pièces générées suivent la numérotation continue ([section 4.8](README.md#48-numérotation-des-pièces--continuité-et-protection)).

---

## Sommaire

1. [Rapprochement bancaire](#1-rapprochement-bancaire)
2. [Comptes de tiers](#2-comptes-de-tiers)
3. [État de compte d'un tiers](#3-état-de-compte-dun-tiers)
4. [Justificatif de solde](#4-justificatif-de-solde)
5. [Données](#5-données)
6. [Routes](#6-routes)
7. [Droits](#7-droits)
8. [Paramétrage](#8-paramétrage)
9. [Lots et critères d'acceptation](#9-lots-et-critères-dacceptation)

---

## 1. Rapprochement bancaire

### 1.1 Comptes concernés

| Compte de trésorerie | Relevé de référence |
| --- | --- |
| Comptes bancaires (`521x`), un par compte ouvert | Relevé de la banque |
| Compte mobile money de la plateforme (PawaPay) | Relevé ou export PawaPay : dépôts, remboursements, frais, reversements |
| Numéros marchands (MTN MoMo Pay, Airtel Money) | Historique du compte marchand |
| Compte d'attente carte (`5851`), si un terminal est ajouté | Relevé de l'acquéreur |

Les caisses ne se rapprochent pas avec un relevé : elles se comptent ([caisses-et-sessions.md](caisses-et-sessions.md#6-comptages-et-billetage)). Le portefeuille se rapproche de `4191` ([README, 14.2](README.md#142-à-améliorer-existe-mais-incomplet)).

### 1.2 Fiche du compte bancaire

- Banque (liste), agence, intitulé, numéro et clé (RIB), devise, compte comptable, journal (`BQ-<code>`, créé avec le compte).
- Modèle d'import du relevé.
- Solde d'ouverture et date de début du suivi.
- Personnes habilitées à rapprocher et à valider.

### 1.3 Relevés

**Sources**

- **Import de fichier** : CSV, Excel, OFX, et MT940 si la banque le fournit.
  - Un **modèle d'import par banque** (paramétrable) indique : colonnes, format des dates et des montants, sens (colonnes débit et crédit, ou montant signé), lignes d'en-tête, encodage.
  - Le modèle se crée une fois, en associant les colonnes d'un fichier d'exemple ; il est réutilisé ensuite.
- **PawaPay** : import du rapport de l'opérateur, ou lecture par API si elle est disponible. Chaque dépôt garde son identifiant, qui sert de clé de pointage.
- **Saisie manuelle** pour une banque sans export : lignes saisies d'après le relevé papier, scanné en pièce jointe.

**Contrôles à l'import**

- Le solde initial du relevé égale le solde final du relevé précédent, sans période manquante entre eux. Sinon : relevé refusé, ou accepté avec une alerte « relevé manquant » (paramètre).
- La somme des lignes égale le solde final moins le solde initial.
- **Pas de doublon** : chaque ligne a une empreinte (date, montant, libellé, référence). Une ligne déjà importée est signalée et ignorée, ce qui permet de réimporter un fichier qui chevauche le précédent.
- Le relevé importé est conservé tel quel (fichier d'origine dans la GED), avec un numéro de relevé, la date d'import et son auteur. Il ne se modifie plus ; une erreur se corrige par un nouvel import qui remplace le relevé, en gardant l'ancien.

### 1.4 Pointage

Pointer, c'est associer une ou plusieurs lignes du relevé à une ou plusieurs lignes d'écriture du compte de banque, pour un total égal. Les éléments pointés reçoivent un **code de pointage**.

**Pointage automatique**, à chaque import puis chaque nuit, par ordre de fiabilité :

1. **Référence exacte** :
   - numéro de bordereau de remise (`BRB-`) ;
   - numéro de chèque ;
   - ordre de transfert (`OTI-`) ;
   - identifiant de dépôt PawaPay ;
   - numéro de facture ou référence de virement client.
2. **Montant exact et date proche** (plus ou moins N jours, paramètre), avec une correspondance unique.
3. **Combinaisons** :
   - une ligne de relevé égale à la somme de plusieurs écritures (remise de plusieurs chèques, reversement groupé de l'opérateur) ;
   - ou l'inverse.
   Le nombre de lignes combinées et la fenêtre de dates sont limités (paramètres).

Une correspondance unique et sûre est pointée d'office (paramètre). Les autres sont **proposées**, avec un indice de confiance, et attendent une validation.

**Pointage manuel** : écran en deux colonnes, lignes du relevé non pointées à gauche et écritures du compte non pointées à droite. On coche de chaque côté, les totaux et la différence s'affichent, et on pointe quand la différence est nulle.

**Dépointage** : droit dédié, motif choisi dans une liste, tracé. Les éléments d'un rapprochement validé (1.6) ne se dépointent plus, sauf par le responsable financier.

### 1.5 Lignes sans contrepartie

**Ligne du relevé sans écriture dans le logiciel.** Le logiciel propose de la comptabiliser.

- **Règles d'affectation** (paramétrables), du type « si le libellé contient … et que le montant est … alors compte …, journal …, tiers … ». Exemples :
  - frais de tenue de compte, commissions, agios : `631` ;
  - intérêts créditeurs : `771` ;
  - commissions de l'opérateur mobile money : `631`.
  La règle produit l'écriture en partie double (D `631` ; C `521x`), validée puis pointée d'un seul geste.
- **Virement reçu d'un client inconnu du logiciel** : le logiciel cherche le client par la référence (numéro de facture, code client) et le nom du donneur d'ordre, puis propose les factures ouvertes de ce client.
  - À la validation, un **paiement par virement** est enregistré dans le module de paiement : c'est le fait métier, imputé aux factures.
  - L'écriture d'encaissement naît de ce paiement, comme pour tout encaissement, puis se pointe. Il n'y a donc jamais d'encaissement client écrit directement en comptabilité sans paiement correspondant.
  - Sans client identifié : écriture sur un compte d'attente des encaissements à identifier (`4719` proposé), notifiée, à reclasser.
- **Virement émis** (fournisseur, salaire) non saisi : proposé contre le compte du fournisseur ou `422`, puis lettré.

**Écriture du logiciel sans ligne au relevé** (un **suspens**) :

- remise d'espèces ou de chèques non encore créditée ;
- chèque émis non encore débité ;
- virement en cours.

Les suspens restent non pointés et vieillissent à vue. Au-delà d'un délai par type, une alerte est envoyée. Exemples : remise non créditée après 3 jours ouvrés ; chèque émis non débité après 6 mois, à examiner pour annulation.

### 1.6 État de rapprochement

Le **justificatif du solde bancaire** à une date, en général la fin de mois, imprimable.

```
Solde du relevé au 30/11/2026                                     12 450 000
  + Encaissements comptabilisés, non encore au relevé (remises)     + 850 000
  - Décaissements comptabilisés, non encore au relevé (chèques)     - 300 000
  + / - Lignes du relevé non encore comptabilisées                   - 12 500
= Solde rapproché                                                  12 987 500
Solde comptable du compte 5211 au 30/11/2026                       12 987 500
Écart                                                                       0
```

- Chaque ligne de suspens est détaillée : date, pièce, libellé, montant, ancienneté.
- **Validation** :
  - l'état est préparé par un comptable, puis validé par une autre personne ;
  - il n'est validable qu'avec un écart nul ;
  - une fois validé, il est **figé et archivé** (PDF dans la GED, numéro continu `RAP-`) et ses pointages sont verrouillés.
- **Règle de clôture** : une période comptable ne se verrouille pas tant que chaque compte de trésorerie n'a pas son rapprochement validé à la fin de la période (paramètre, bloquant par défaut).
- **Historique** : les états validés restent consultables et réimprimables (mention « DUPLICATA »).
- **Comptes en devise** : rapprochement en devise ; l'écart de conversion à la date est constaté par écriture (`676` / `776`).

### 1.7 Suivi

- Tableau de bord des comptes de trésorerie :
  - solde relevé, solde comptable, nombre et montant des suspens, ancienneté du plus vieux suspens ;
  - date du dernier relevé importé, date du dernier rapprochement validé.
- Alertes :
  - relevé non importé depuis N jours ;
  - rapprochement du mois non validé à J+N ;
  - suspens trop ancien ;
  - écart non expliqué.

---

## 2. Comptes de tiers

### 2.1 Un compte par tiers

Chaque tiers a **son** compte auxiliaire, rattaché à un compte collectif. Il est créé automatiquement à la première écriture, ou à la création du tiers (paramètre).

| Tiers | Collectif | Compte auxiliaire |
| --- | --- | --- |
| Membre, client particulier, entreprise cliente (`CUS-COM`) | `411` Clients | `411` + code du tiers |
| Le même, pour ses avances et son portefeuille | `4191` | `4191` + code |
| Fournisseur | `401` | `401` + code |
| Employé (avances, frais) | `421` | `421` + code |
| Employé (salaire dû) | `422` | `422` + code |
| Associé (compte courant) | `462` | `462` + code |
| Déposant d'une caution | `165` | `165` + code |

- Le compte s'affiche sous le nom du tiers, avec son code, ses coordonnées et ses identifiants (NIU, RCCM).
- Un même tiers qui joue plusieurs rôles (un client qui est aussi fournisseur) a un compte par rôle. Une **compensation** entre ses comptes est possible :
  - par une écriture en partie double (D `401` ; C `411`), lettrée des deux côtés ;
  - autorisée par paramètre et soumise au droit `THIRD_PARTY:OFFSET`.
- Pour une entreprise cliente, les membres rattachés qui font facturer l'entreprise écrivent dans le compte **de l'entreprise** (la facture est à son nom). Ceux qui paient eux-mêmes ont leur propre compte.
- **Solde du compte** :
  - débiteur : le tiers doit à l'exploitant ;
  - créditeur : l'exploitant lui doit (avance, trop-perçu, avoir non utilisé).
  - À la clôture, les clients créditeurs sont présentés au passif et les fournisseurs débiteurs à l'actif (reclassement automatique, contre-passé à l'ouverture).

### 2.2 Ce qui débite et crédite un compte client

| Débit (le client doit plus) | Crédit (le client doit moins) |
| --- | --- |
| Facture émise | Paiement reçu, quel que soit le moyen |
| Remboursement versé au client | Avoir |
| Chèque rejeté (contre-passation du paiement) | Escompte accordé |
| Frais de rejet refacturés | Imputation du portefeuille ou d'un acompte |
| Écart de règlement en faveur de l'exploitant (lettrage) | Perte sur créance (passage en perte) |
|  | Écart de règlement en faveur du client (lettrage) |

Chaque ligne garde sa référence (facture, paiement, avoir), sa date d'échéance et son état de lettrage ([README, section 4.7](README.md#47-lettrage)).

### 2.3 Consultation

Fiche comptable du tiers, accessible depuis la fiche client, membre, entreprise ou fournisseur :

- solde, montant échu, montant non échu, montant à venir ;
- grand livre auxiliaire :
  - date, journal, pièce, libellé, référence, échéance, débit, crédit, solde progressif, lettrage ;
  - filtres par période, par lignes non lettrées seulement, par type de pièce ;
- balance âgée du tiers ;
- historique des relances (`dunning`), des états de compte envoyés et des confirmations de solde.

---

## 3. État de compte d'un tiers

Le document remis au tiers, ou conservé, qui retrace son compte. Il est **tiré du compte auxiliaire** : il dit exactement ce que dit la comptabilité. Il remplace, une fois la comptabilité active, le relevé reconstitué depuis les factures (`/billing/customers/{type}/{code}/statement`, dont la route est gardée ; voir [etats-financiers.md](etats-financiers.md#3-états-de-compte-des-tiers)).

### 3.1 Modèles

| Modèle | Contenu | Usage |
| --- | --- | --- |
| **Complet** | Toutes les lignes de la période, avec solde d'ouverture et solde progressif | Envoi mensuel, demande du client |
| **Pièces ouvertes** | Seulement les factures et avoirs non lettrés (ou leur reste), avec leur échéance et leur retard | Relance, recouvrement |
| **Synthétique** | Totaux par mois : facturé, réglé, solde | Grands comptes |
| **Fournisseur, employé, associé** | Même structure, avec le vocabulaire adapté | Rapprochement avec le tiers |

### 3.2 Contenu imprimé

- **En-tête** : identité de l'exploitant (raison sociale, RCCM, NIU, adresse, logo), titre « État de compte », numéro du document (`RDC-`, séquence continue), date d'édition, période.
- **Tiers** : nom, code, adresse, NIU, interlocuteur, courriel de facturation (une ou plusieurs adresses).
- **Solde d'ouverture**, au premier jour de la période, avec son sens.
- **Lignes** : date, nature (facture, paiement, avoir...), numéro de pièce, libellé, échéance, débit, crédit, solde progressif, état (réglé, partiellement réglé, ouvert, en retard).
- **Totaux** de la période : débits, crédits.
- **Solde de clôture**, en chiffres et en lettres, avec son sens : « vous restez nous devoir » ou « nous restons vous devoir ».
- **Échéancier du solde** : non échu, échu de 1 à 30 jours, de 31 à 60, de 61 à 90, plus de 90 jours.
- **Modalités de règlement** : coordonnées bancaires, numéros marchands, lien de paiement en ligne vers les factures ouvertes.
- **Mention** : « Sauf erreur ou omission de notre part. Toute contestation doit nous parvenir sous N jours. »
- **QR de vérification** (module `verify`) : le tiers vérifie l'authenticité et le solde à la date d'édition.

### 3.3 Fonctions

- **Imprimer** : PDF prêt à imprimer en A4 ; réimpression avec la mention « DUPLICATA » et le même numéro.
- **Exporter** : PDF, Excel, CSV.
- **Envoyer** par courriel :
  - aux adresses de facturation du tiers, en un clic ou en masse ;
  - envoi en masse en fin de mois, pour tous les tiers ayant un solde ou des mouvements (paramètre) ;
  - envoi tracé dans le journal des courriels existant.
- **Portail** : le membre ou l'entreprise télécharge son état de compte depuis son espace, à toute date.
- **Archive** : chaque état émis est conservé (PDF dans la GED, numéro, période, solde affiché, destinataires).

---

## 4. Justificatif de solde

### 4.1 Justificatif du solde d'un tiers

À une date donnée, le solde d'un compte de tiers est **justifié** par la liste de ses lignes non lettrées (ou des restes de lettrages partiels) : leur somme doit égaler le solde comptable.

```
Client CUS-COM-202611-00000012, compte 411CUSCOM20261100000012, au 31/12/2026
  FAC-202611-000231   15/11/2026   échue depuis 16 jours       450 000   D
  FAC-202612-000047   01/12/2026   non échue                   300 000   D
  AVO-202612-000003   10/12/2026   avoir non imputé           - 50 000   C
Total des pièces ouvertes                                      700 000   D
Solde comptable                                                700 000   D
Écart                                                                0
```

- **Écart non nul** : le lettrage est incomplet ou faux (par exemple, un paiement non lettré alors que la facture l'est). Le justificatif signale l'écart et renvoie à l'écran de lettrage. Le contrôle quotidien le détecte aussi.
- Imprimable, signé par le comptable (préparé par, revu par).

### 4.2 Justification des comptes collectifs

Pour `411`, `401`, `421`, `4191`, `422`, `462` et `165`, à une date :

- la liste des tiers avec leur solde. Leur somme égale le solde du collectif ; un écart est une anomalie bloquante pour la clôture ;
- pour chaque tiers, ses pièces ouvertes (4.1), avec leur ancienneté ;
- les soldes anormaux à part : clients créditeurs, fournisseurs débiteurs, avances au personnel anciennes.

C'est le document que l'expert-comptable et le commissaire aux comptes demandent pour valider les comptes de tiers.

### 4.3 Dossier de justification des soldes de clôture

À la clôture, chaque compte de bilan a son justificatif. Le logiciel les assemble en un dossier, compte par compte, avec l'état de chaque justification (justifié, écart, à revoir) :

| Comptes | Justificatif |
| --- | --- |
| Banques, mobile money | État de rapprochement validé (1.6) |
| Caisses | Dernier PV de billetage ([caisses-et-sessions.md](caisses-et-sessions.md#6-comptages-et-billetage)) |
| Tiers (`411`, `401`, `421`, `4191`, `422`, `462`, `165`) | Justificatifs 4.1 et 4.2 |
| Comptes d'attente (`471`, `4718`, `4719`, `585`) | Lignes non lettrées et leur ancienneté ; doivent être soldés ou expliqués |
| Chèques à encaisser (`513`) | Liste des chèques en portefeuille |
| Produits constatés d'avance (`477`) | Détail par abonnement, pass, domiciliation |
| Immobilisations et amortissements | Tableau des immobilisations |
| Emprunts, comptes courants | Tableau d'amortissement, relevé d'associé |
| TVA | État de TVA de la dernière déclaration |

Le dossier s'exporte en PDF avec un sommaire, pour l'expert-comptable et le commissaire aux comptes. Il fait partie de la liste de clôture ([etats-financiers.md](etats-financiers.md#6-états-dinventaire-et-de-clôture)).

### 4.4 Confirmation de solde (circularisation)

- **Lettre de confirmation** générée pour un tiers ou une sélection (par exemple les 20 plus gros soldes). Elle porte le solde à la date, le détail des pièces ouvertes et un coupon-réponse : « solde confirmé » ou « solde contesté » avec explication.
- Imprimable et envoyable par courriel ; le tiers peut aussi répondre depuis son portail.
- **Suivi** : envoyée, relancée, répondue, conforme ou en écart. Un écart ouvre un rapprochement avec le tiers : on compare le compte du tiers avec l'état qu'il envoie, ligne par ligne.
- Les réponses sont archivées dans le dossier de révision.

---

## 5. Données

| Table | Champs principaux |
| --- | --- |
| `bank_account` | code, bank_code, branch, label, rib, currency, account_number (`521x`), journal_code, import_template_code, opening_balance, opening_date, active |
| `bank_import_template` | code, bank_code, format (CSV, XLSX, OFX, MT940), column_mapping (jsonb), date_format, amount_format, sign_mode, header_rows, encoding |
| `bank_statement` | number, bank_account_code, statement_date, period_from, period_to, opening_balance, closing_balance, source_file (GED), imported_by, imported_at, status (`IMPORTED`, `REPLACED`), replaced_by |
| `bank_statement_line` | statement_id, line_no, value_date, operation_date, label, reference, amount, direction, fingerprint, matching_code, matched_at, matched_by |
| `bank_matching` | code, bank_account_code, kind (`AUTO_REFERENCE`, `AUTO_AMOUNT`, `AUTO_COMBINATION`, `MANUAL`), statement_lines, entry_lines, created_by, created_at, unmatched_by, unmatched_reason, locked |
| `bank_matching_proposal` | bank_account_code, statement_lines, entry_lines, confidence, status, decided_by |
| `bank_assignment_rule` | label_pattern, amount_range, direction, account_number, journal_code, third_party_code, analytic, priority, active |
| `bank_reconciliation` | number (`RAP-`), bank_account_code, date, statement_balance, book_balance, outstanding (jsonb), difference, prepared_by, validated_by, validated_at, pdf (GED), status (`DRAFT`, `VALIDATED`) |
| `acc_third_party_account` (existant au plan) | ajouter : collective_account, party_role, auto_created, offset_allowed |
| `third_party_statement` | number (`RDC-`), party_type, party_code, account_number, model, period_from, period_to, opening_balance, closing_balance, pdf (GED), recipients, sent_at, issued_by |
| `balance_justification` | account_number, third_party_code, date, open_items (jsonb), total_open, book_balance, difference, prepared_by, reviewed_by, pdf |
| `acc_confirmation_request` (existant au plan) | ajouter : open_items, reply_channel, reply_detail, reconciled_by |

Lignes d'écriture `acc_entry_line` : ajout de `matching_code` (pointage bancaire), distinct de `lettering_code`.

---

## 6. Routes

| Méthode et chemin | Rôle |
| --- | --- |
| `GET`, `POST`, `PATCH /accounting/bank-accounts` | Comptes bancaires et mobile money |
| `GET`, `POST /accounting/bank-import-templates` | Modèles d'import ; `POST .../preview` pour essayer un modèle sur un fichier |
| `POST /accounting/bank-accounts/{code}/statements` | Importer un relevé (fichier ou saisie) |
| `GET /accounting/bank-accounts/{code}/statements` | Relevés importés |
| `GET /accounting/bank-accounts/{code}/matching` | Écran de pointage : lignes non pointées des deux côtés, propositions |
| `POST /accounting/bank-accounts/{code}/matching` | Pointer ; `DELETE .../matching/{code}` pour dépointer, avec motif |
| `POST /accounting/bank-accounts/{code}/statement-lines/{id}/post` | Comptabiliser une ligne du relevé (règle d'affectation, ou paiement client) |
| `GET`, `POST /accounting/bank-assignment-rules` | Règles d'affectation |
| `POST /accounting/bank-accounts/{code}/reconciliations` | Préparer un état de rapprochement à une date |
| `PATCH /accounting/reconciliations/{n}/validate` | Valider ; `GET .../pdf` pour imprimer |
| `GET /accounting/third-parties/{type}/{code}/ledger` | Compte du tiers (grand livre auxiliaire) |
| `GET /accounting/third-parties/{type}/{code}/statement?model=&from=&to=&format=pdf` | État de compte imprimable, ou en Excel et CSV |
| `POST /accounting/third-parties/statements/send` | Envoi d'états de compte, un tiers ou en masse |
| `GET /accounting/third-parties/{type}/{code}/balance-justification?date=` | Justificatif de solde du tiers |
| `GET /accounting/accounts/{number}/balance-justification?date=` | Justification d'un compte collectif ou d'attente |
| `GET /accounting/closing/{year}/justification-file` | Dossier de justification des soldes de clôture |
| `POST`, `GET /accounting/confirmations` | Confirmations de solde |
| Portail : `GET /me/account-statement` | État de compte du membre ou de l'entreprise connectée |

---

## 7. Droits

| Permission | Pour |
| --- | --- |
| `BANK_ACCOUNT:MANAGE` | Créer et modifier les comptes bancaires, les modèles d'import |
| `BANK_STATEMENT:IMPORT` | Importer un relevé |
| `BANK_RECONCILIATION:MANAGE` (existant au plan) | Pointer, dépointer, comptabiliser une ligne de relevé, préparer l'état de rapprochement |
| `BANK_RECONCILIATION:VALIDATE` | Valider l'état de rapprochement ; doit être une autre personne que celle qui l'a préparé |
| `BANK_RECONCILIATION:UNLOCK` | Responsable financier : dépointer dans un rapprochement validé |
| `THIRD_PARTY_STATEMENT:READ`, `:SEND` | Consulter, imprimer et envoyer les états de compte |
| `THIRD_PARTY:OFFSET` | Compenser les comptes d'un même tiers |
| `BALANCE_JUSTIFICATION:PREPARE`, `:REVIEW` | Justificatifs de solde et dossier de clôture |

---

## 8. Paramétrage

| Paramètre | Valeur initiale |
| --- | --- |
| Modèles d'import par banque | À créer pour chaque banque |
| Relevé manquant à l'import | Alerte (sinon : refus) |
| Fenêtre de dates du pointage | Plus ou moins 5 jours |
| Nombre maximal de lignes combinées | 10 |
| Pointage automatique des correspondances sûres | Oui |
| Règles d'affectation des lignes de relevé | Frais, agios, commissions, intérêts |
| Compte d'attente des encaissements à identifier | `4719` |
| Délais d'alerte des suspens | Remise non créditée : 3 jours ouvrés ; chèque émis non débité : 6 mois ; autres : 30 jours |
| Rapprochement exigé avant de verrouiller une période | Oui |
| Création du compte de tiers | À la première écriture (sinon : à la création du tiers) |
| Compensation entre comptes d'un même tiers | Autorisée avec droit |
| Reclassement des soldes anormaux à la clôture | Oui |
| Modèle d'état de compte par défaut | Complet |
| Envoi mensuel en masse | Désactivé ; jour du mois et tiers concernés si activé |
| Délai de contestation imprimé sur l'état de compte | 15 jours |
| Seuil de sélection des confirmations de solde | 20 plus gros soldes, et tout solde supérieur à 1 000 000 XAF |

---

## 9. Lots et critères d'acceptation

| Lot du plan | Contenu de ce volet | Critère d'acceptation |
| --- | --- | --- |
| **F4. Encaissements et ventes** | Comptes clients auxiliaires, état de compte client imprimable (modèles, envoi, portail, archive), justificatif de solde client | L'état de compte imprimé d'un client affiche le même solde que son compte `411` ; la somme de ses pièces ouvertes égale ce solde |
| **F10. Banque, chèques, mobile money** | Comptes bancaires, modèles et contrôles d'import, pointage automatique et manuel, règles d'affectation, virements clients identifiés et transformés en paiements, suspens, état de rapprochement validé et figé | Un relevé réel d'un mois se pointe automatiquement à plus de 80 % ; l'état de rapprochement validé a un écart nul ; un virement client inconnu devient un paiement imputé à sa facture |
| **F12. Abonnements et clôture** | Justification des comptes collectifs, reclassement des soldes anormaux, dossier de justification des soldes, confirmations de solde | La clôture est refusée si un collectif ne se justifie pas ou si un compte bancaire n'a pas son rapprochement validé |
| **F13. Achats** | États de compte fournisseurs, justificatif de solde fournisseur, compensation client et fournisseur | Une compensation produit une écriture équilibrée lettrée des deux côtés |
