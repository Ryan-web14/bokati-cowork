# Clôture d'exercice, états financiers, justification des soldes, confirmations

Contrat exact de chaque route (paramètres, corps, réponse, champs JSON) : [routes.md](routes.md). Ce fichier explique le fonctionnement et ce qu'il faut à l'écran. Index du module : [README.md](README.md).

## États financiers

| État | Contenu |
| --- | --- |
| Bilan | Actif en brut, amortissements et dépréciations, net ; passif en net ; colonne N-1 |
| Compte de résultat | Postes et soldes de gestion : marge commerciale, chiffre d'affaires, valeur ajoutée, EBE, résultat d'exploitation, financier, des activités ordinaires, HAO, résultat net |
| Tableau des flux | CAFG, variations du besoin de financement, investissement, capitaux propres, capitaux étrangers, trésorerie d'ouverture et de clôture |

Règles de calcul :
- **Correspondance** : pour chaque compte, le préfixe le plus long l'emporte. Un sens (`DEBIT`, `CREDIT`) ne vaut que pour un solde de ce sens : une banque créditrice passe en trésorerie passif. Les comptes `28`, `29`, `39`, `49`, `59` vont en colonne amortissements et dépréciations.
- **Soldes anormaux** : les comptes de tiers (classe 4) sont classés tiers par tiers. Un client créditeur passe en « Clients, avances reçues », un fournisseur débiteur en « Fournisseurs, avances versées », sans écriture de reclassement.
- **Totaux** : formules de codes de postes (`XG+XH-RQ-RS`), stockées sur le poste.
- **Périmètre** : les comptes de bilan cumulent depuis le lendemain du dernier exercice clos (à-nouveaux compris), les comptes de gestion portent sur l'exercice. Le résultat de l'exercice entre au poste `CJ`. Tant qu'un exercice antérieur n'est pas clos, son résultat y entre aussi (en instance d'affectation).
- **Tableau des flux** : les mouvements excluent les à-nouveaux. La CAFG part du résultat net, ajoute les dotations et la valeur comptable des cessions, retire les reprises et les produits de cession. Les variations portent sur les valeurs brutes de l'actif circulant et sur le passif circulant, hors provisions à court terme et dividendes à payer.
- **Codes de flux** : une pièce peut porter un code de flux (`FF` à `FQ`, postes de détail des flux d'investissement et de financement), saisi sur la pièce ou hérité de son schéma d'imputation. Pour chaque code présent dans l'exercice, le tableau des flux prend le mouvement de trésorerie de ces pièces (classe 5 hors `58`, hors à-nouveaux) à la place de l'estimation par les comptes. Coder une partie seulement des pièces d'un même poste fausse ce poste : le contrôle de trésorerie le signale.
- **Mention** : un état est `provisional` tant que l'exercice n'est pas clos. `firstYear` signale l'absence de colonne N-1.

Contrôles servis avec chaque état :
- comptes mouvementés sans poste (`unclassified`) ;
- total actif égal au total passif ;
- résultat porté au bilan égal au résultat net calculé poste par poste ;
- résultat net égal aux produits moins les charges ;
- trésorerie du tableau des flux égale à la trésorerie du bilan.

La correspondance se modifie par les routes `mappings` (droit `EDIT`). Les postes et leurs formules sont des données : une nouvelle version du modèle se charge par migration, sans développement.

## Liste de clôture

Chaque lecture recalcule les contrôles automatiques et enregistre leur résultat.

| Étape | Type | Obligatoire |
| --- | --- | --- |
| `TRIAL_BALANCE` balance équilibrée | Contrôle | Oui |
| `PENDING_ENTRIES` aucune pièce en attente ou en brouillard dans l'exercice | Contrôle | Oui |
| `UNBALANCED` compte d'attente d'équilibrage soldé, pièces acceptées déséquilibrées régularisées | Contrôle | Oui |
| `NUMBERING` numérotation continue et chaîne d'empreintes de chaque journal | Contrôle | Oui |
| `SUSPENSE_ACCOUNTS` comptes d'attente soldés (`471`, `585`) | Contrôle | Oui |
| `BANK_RECONCILIATION` rapprochements validés au dernier jour (lot F10) | Contrôle | Oui |
| `CASH_REVIEW` aucune session de caisse en revue d'écart | Contrôle | Oui |
| `FIXED_ASSETS` contrôle de clôture du lot F18 | Contrôle | Oui |
| `PROVISIONS` contrôle de clôture du lot F19 | Contrôle | Oui |
| `DEFERRALS` contrôle de clôture du lot F20 | Contrôle | Oui |
| `COLLECTIVES` comptes collectifs justifiés par leurs tiers | Contrôle | Oui |
| `PRIOR_RESULT` résultat précédent affecté (comptes `13` soldés) | Contrôle | Oui |
| `CLASS_NINE` balances analytique et hors bilan équilibrées | Contrôle | Oui |
| `UNCLASSIFIED` chaque compte mouvementé a son poste | Contrôle | Oui |
| `STATEMENTS` bilan équilibré, résultats concordants | Contrôle | Oui |
| `STAFF_ADVANCES` avances au personnel revues | À cocher | Oui |
| `CASH_COUNT` procès-verbal de caisse au dernier jour | À cocher | Oui |
| `STOCK_COUNT` inventaire des stocks | À cocher | Non |
| `CHEQUES` inventaire des chèques à encaisser | À cocher | Non |
| `NOTES` notes rédactionnelles validées | Contrôle | Non |

- Une étape cochée garde son auteur, sa date et son commentaire.
- Une étape en échec se corrige, ou se lève par dérogation motivée (droit `FINALIZE`, statut `WAIVED`).
- Les seuls constats bloquants des lots F18 à F20 comptent, pas leurs avertissements.

## Clôture et à-nouveaux

`POST /accounting/closing/{year}/close` :
1. refuse si un exercice antérieur n'est pas clos, ou si une étape obligatoire n'est ni faite ni levée ;
2. crée l'exercice suivant s'il n'existe pas (douze périodes mensuelles) ;
3. passe les à-nouveaux dans le journal `AN`, au premier jour de l'exercice suivant, en une pièce validée ;
4. reprend les engagements hors bilan (comptes `90`) dans le journal `HB` ;
5. clôt les périodes de l'exercice dans l'ordre, puis l'exercice ; enregistre le résultat et le numéro de la pièce d'à-nouveaux.

Contenu des à-nouveaux :
- **Comptes lettrables** (`411`, `401`...) : une ligne par reste non lettré au dernier jour, avec sa référence, son échéance, son tiers et sa devise. Un écart résiduel avec le solde du tiers fait une ligne à part.
- **Autres comptes de bilan** : leur solde, par tiers.
- **Résultat** : au crédit du `131` (bénéfice) ou au débit du `139` (perte).
- **Analytique** (`91` à `99`) : non reprise, elle repart de zéro.

Suite du lettrage :
- chaque ligne reprise de l'exercice clos est soldée par un lettrage `CARRY_FORWARD` ;
- un lettrage déjà fait avec une ligne de l'exercice suivant (un règlement de janvier lettré avant la clôture) passe sur la ligne d'à-nouveau ;
- un lettrage de reprise ne se défait pas : on délettre la ligne d'à-nouveau.

`GET /accounting/closing/{year}/opening-preview` simule les à-nouveaux sans écrire.

## Balance avant et après inventaire

Pour un exercice, par compte : solde avant inventaire, mouvements d'inventaire, solde après inventaire. Elle donne aussi le résultat avant et après inventaire, et la liste des écritures d'inventaire.

Une écriture d'inventaire est d'origine clôture, ou son type de pièce figure dans `accounting.closing.inventory-sources` :
- dotations et dépréciations des immobilisations, amortissements dérogatoires ;
- dépréciations de créances, provisions pour risques, dépréciations de stock, de trésorerie, d'avances et de dépôts ;
- constatations d'avance de fin de période, factures à établir et non parvenues ;
- impôt sur le résultat.

## Synchronisation avec la clôture de facturation

Quand un mois de facturation est clos (totaux scellés), la facturation publie l'événement `core/accounting/BillingPeriodClosedEvent`. La comptabilité réagit selon `accounting.period.billing-closure-sync` :
- `PROPOSE` (défaut) : les comptables reçoivent une notification qui propose de verrouiller la période comptable du mois ;
- `LOCK` : la période est verrouillée. Si le verrouillage est refusé (rapprochement manquant, pièce en attente), la notification en donne le motif ;
- `NONE` : rien.

Seule une période ouverte est concernée. Les clôtures de jour et d'année n'ont pas d'effet.

## Justification des soldes

**Justificatif d'un tiers** (ou d'un compte) à une date :
- pièces ouvertes : lignes datées au plus tard ce jour, moins ce que lettrent les lettrages actifs dont toutes les lignes sont datées au plus tard ce jour. Une facture réglée en décembre et lettrée en janvier n'est donc pas ouverte au 31 décembre ;
- pour chaque pièce : date, référence, échéance, jours de retard, montant signé ;
- total des pièces ouvertes, solde comptable, écart. Un écart non nul signale un lettrage incomplet ou faux.

Un justificatif se fige (`POST`) avec son préparateur, puis se revoit (`review`) par une autre personne.

**Compte collectif** (`411`, `401`, `421`, `4191`, `422`, `462`, `165`, paramètre `accounting.closing.collective-accounts`) :
- solde par tiers, somme des tiers, montant porté sans tiers ;
- écart de justification de chaque tiers (comptes lettrables) ;
- soldes anormaux à part : client créditeur, fournisseur débiteur.

Un montant sans tiers ou un écart de justification bloque la clôture (étape `COLLECTIVES`).

**Dossier de justification des soldes de clôture** : chaque compte de bilan non soldé, son justificatif et son état.

| Comptes | Justificatif | État |
| --- | --- | --- |
| `52`, `53`, `55` | Rapprochement bancaire validé au dernier jour | Justifié, en écart (compte suivi sans rapprochement), à revoir (compte non suivi) |
| `57` | PV de billetage | À revoir |
| Collectifs | Justification par tiers | Justifié ou en écart |
| `471`, `585` | Compte d'attente | En écart tant qu'il n'est pas soldé |
| `513` | Chèques en portefeuille | Justifié si le total égale le solde |
| `476`, `477` | Échéanciers du lot F20 | Justifié ou en écart |
| `2` | Fichier des immobilisations (lot F18) | Justifié ou en écart |
| `16`, `17` | Tableau d'amortissement | À revoir |
| `44` | Dernière déclaration | À revoir |
| Autres | Pièce à joindre | À revoir |

Un compte à revoir passe à justifié quand un justificatif figé à la date de clôture a été revu. Le dossier s'exporte en PDF ou en CSV.

## Confirmations de solde

- **Sélection** : les tiers désignés, ou les 20 plus gros soldes du collectif et tout solde au-delà de 1 000 000 (paramètres `accounting.confirmation.top-count` et `threshold`). Un tiers déjà circularisé à la même date n'est pas repris.
- **Lettre** (PDF) : solde dans nos livres, pièces ouvertes, coupon-réponse, numéro `CONF-{année}-{rang}` à rappeler.
- **Envoi, sans aucune ouverture vers l'extérieur** (`channel`) :
  - `EMAIL` (défaut) : la lettre PDF part en pièce jointe, à l'adresse donnée ou à celle du tiers (adresse de facturation du client, membre du client, organisation, fournisseur) ;
  - `WHATSAPP` : message texte par Infobip, avec le modèle approuvé `BALANCE_CONFIRMATION` (voir `docs/backend/whatsapp-infobip`), au numéro donné ou au numéro WhatsApp du client ou de son membre. Pas de PDF ;
  - `PRINT` : la lettre s'imprime, l'envoi est seulement noté.
- Le tiers répond par retour de courriel, sur WhatsApp ou par courrier. Le comptable saisit sa réponse.
- Le canal et le destinataire du dernier envoi sont gardés (`sentChannel`, `recipient`).
- **Suivi** : préparée (`PREPARED`), envoyée (`SENT`), relancée (`REMINDED`), conforme (`AGREED`) ou en écart (`DISPUTED`), puis rapprochée (`RECONCILED`, droit `REVIEW`, avec l'explication de l'écart).
- La réponse note son canal (courrier, portail...), le solde confirmé et l'écart.

## Éditions définitives

- Seulement sur un exercice clos, contrôles de cohérence au vert, ou avec un motif de dérogation.
- Données figées (JSON), PDF, empreinte SHA-256, numéro `EF-{exercice}-{état}-{rang}`.
- Le PDF se réimprime à l'identique. La route `verify` recalcule l'empreinte.

## Notes annexes

Catalogue du SYSCOHADA révisé (notes 1 à 35 utiles, plus événements postérieurs `EP`, parties liées `PL` et engagements hors bilan `HB`) :
- **notes chiffrées** : comptes repris de la balance, montant N et N-1. Les notes 3A à 3D et 28 renvoient aussi aux états détaillés des lots F18 et F19 (`source`) ;
- **notes rédigées** : texte saisi, repris de N-1 à défaut, puis validé ;
- **« Néant »** : une note sans montant ni texte est marquée sans objet (`notApplicable`).

## Routes (`/sni/api/v1`)

| Route | Usage |
| --- | --- |
| `GET /accounting/statements/balance-sheet?date=&format=json\|csv\|pdf` | Bilan |
| `GET /accounting/statements/income-statement?from=&to=&format=` | Compte de résultat |
| `GET /accounting/statements/cash-flow?year=&format=` | Tableau des flux |
| `GET`, `POST /accounting/statements/{BILAN\|COMPTE_RESULTAT}/mappings`, `DELETE /accounting/statements/mappings/{id}` | Correspondance des comptes |
| `GET /accounting/closing/{year}` | Liste de clôture |
| `POST /accounting/closing/{year}/steps/{step}` `{done, comment}` | Cocher ou décocher une étape manuelle |
| `POST /accounting/closing/{year}/steps/{step}/waive` `{reason}` | Lever une étape |
| `GET /accounting/closing/{year}/opening-preview` | Simulation des à-nouveaux |
| `POST /accounting/closing/{year}/close` `{derogation}` | Clôture |
| `POST`, `GET /accounting/closing/{year}/editions` `{report, derogation}` | Éditions définitives (`BILAN`, `COMPTE_RESULTAT`, `TFT`) |
| `GET /accounting/closing/editions/{number}/pdf`, `/verify` | PDF figé, contrôle d'empreinte |
| `GET /accounting/closing/{year}/notes`, `PUT /notes/{note}`, `POST /notes/{note}/validate?validate=` | Notes annexes |
| `GET /accounting/accounts/{number}/balance-justification?thirdParty=&date=` | Justificatif de solde |
| `POST /accounting/accounts/{number}/balance-justification` `{thirdPartyCode, date, comment}` | Figer un justificatif |
| `PATCH /accounting/balance-justifications/{id}/review` | Revue d'un justificatif |
| `GET /accounting/accounts/{number}/collective?date=` | Justification d'un compte collectif |
| `GET /accounting/closing/{year}/justification-file?format=json\|csv\|pdf` | Dossier de justification des soldes |
| `POST /accounting/confirmations` `{accountNumber, date, top, threshold, thirdParties}` | Préparer des confirmations |
| `GET /accounting/confirmations` | Suivi des confirmations |
| `POST /accounting/confirmations/{number}/send?reminder=&channel=EMAIL\|WHATSAPP\|PRINT&recipient=` | Envoi ou relance |
| `GET /accounting/closing/{year}/inventory-balance?format=json\|csv\|pdf` | Balance avant et après inventaire |
| `PATCH /accounting/entries/{reference}/cash-flow-code` `{code}` | Code de flux d'une pièce (droit `ACCOUNTING_STATEMENT:EDIT`) |
| `PATCH /accounting/posting-rules/{code}/cash-flow-code` `{code}` | Code de flux d'un schéma d'imputation |
| `POST /accounting/confirmations/{number}/reply` `{channel, confirmedBalance, detail}` | Réponse du tiers |
| `POST /accounting/confirmations/{number}/reconcile` `{comment}` | Rapprochement d'un écart |
| `GET /accounting/confirmations/{number}/letter?format=pdf\|json` | Lettre de confirmation |

## Paramètres (`ACCOUNTING`, lus à chaud)

| Clé | Défaut |
| --- | --- |
| `accounting.closing.opening-journal` | `AN` |
| `accounting.closing.off-balance-journal` | `HB` |
| `accounting.closing.profit-account`, `loss-account` | `131`, `139` |
| `accounting.closing.suspense-accounts` | `471,585` |
| `accounting.closing.collective-accounts` | `411,401,421,4191,422,462,165` |
| `accounting.confirmation.top-count`, `threshold` | `20`, `1000000` |
| `accounting.closing.inventory-sources` | Types de pièce des écritures d'inventaire |
| `accounting.period.billing-closure-sync` | `PROPOSE` |

## Limites connues

- **Réglé** : balance avant et après inventaire (voir plus haut).
- **Réglé** : synchronisation avec la clôture de facturation.
- **Réglé en partie** : codes de flux, portés par les schémas d'imputation et les pièces. Les natures d'opération de caisse n'en portent pas encore.
- **Système minimal de trésorerie** : non retenu (décision 1). Un modèle `SMT` peut se charger par migration.
- **Notes chiffrées** : elles listent les comptes et leurs soldes. Les tableaux de mouvements propres à chaque note (entrées, sorties) restent dans les états des lots F18 et F19.
- **Réglé** : confirmations par courriel (PDF joint) et par WhatsApp (Infobip, texte seul). Pas de réponse en ligne : décision de ne rien ouvrir vers l'extérieur.
- **Comparaison ligne à ligne** avec l'état envoyé par le tiers : l'écart se rapproche avec un commentaire.

## Pour le frontend

Écrans à prévoir :
- bilan, compte de résultat, tableau des flux, avec la colonne N-1, la mention « PROVISOIRE », les contrôles et les comptes non classés ;
- correspondance des comptes vers les postes ;
- liste de clôture : contrôles, étapes à cocher, dérogations ;
- justificatifs de solde, comptes collectifs, dossier de justification ;
- confirmations de solde : sélection, lettre, suivi des réponses ;
- simulation des à-nouveaux, puis clôture ;
- éditions définitives et leur PDF ;
- notes annexes : montants, éditeur de texte, validation.
