# Financement, engagements hors bilan et paie

Trois manques relevés à la relecture du plan, tranchés par la direction ([décisions 15 à 17](README.md#19-décisions-prises)) :

- gestion complète des emprunts et des comptes courants d'associés ;
- registre des engagements hors bilan ;
- import du journal de paie.

## 1. Emprunts

### 1.1 Fiche emprunt

| Champ | Détail |
| --- | --- |
| Numéro, prêteur (banque, associé, autre), objet | Saisi ; le prêteur est un tiers |
| Montant, devise, date de déblocage | Saisi |
| Taux (fixe ou variable), base de calcul (30/360, réel/365) | Saisi |
| Durée, périodicité (mensuelle, trimestrielle...), différé (en capital ou total) | Saisi |
| Mode d'amortissement | Échéances constantes, amortissement constant du capital, in fine |
| Frais de dossier, assurance par échéance | Saisi |
| Garanties données | Reliées au registre hors bilan (section 3) |
| Compte comptable | `16x` selon la nature (emprunts bancaires, dettes envers les associés) |
| Compte de trésorerie de versement et de prélèvement | Banque concernée |
| Section analytique et ligne budgétaire des intérêts | Paramétrables |

### 1.2 Tableau d'amortissement

- Calculé à la création : date, capital restant dû, intérêts, capital remboursé, assurance, échéance totale.
- Imprimable et exportable.
- **Recalculé** en cas de :
  - remboursement anticipé, partiel ou total, avec indemnité éventuelle ;
  - changement de taux (taux variable, renégociation) ;
  - report d'échéance.
- L'ancien tableau est conservé.
- Rapprochement possible avec le tableau fourni par la banque (import).

### 1.3 Écritures automatiques

| Événement | Écriture |
| --- | --- |
| Déblocage des fonds | D banque ; C `16x` (net des frais : D `631`) |
| Échéance (à la date, ou au relevé bancaire selon paramètre) | D `16x` capital ; D `671` intérêts ; D `616x` assurance ; C banque |
| Intérêts courus en clôture | D `671` ; C `166` intérêts courus |
| Extourne à l'ouverture | Contre-passation des intérêts courus |
| Reclassement court terme en clôture (si demandé par l'expert-comptable) | Part à moins d'un an, pour l'annexe |

### 1.4 Suivi

- Échéancier de tous les emprunts, intégré au **prévisionnel de trésorerie**.
- Alertes avant échéance.
- Charges d'intérêts dans le budget.
- **Note annexe des dettes financières** remplie automatiquement : à plus d'un an, à moins d'un an, garanties.

## 2. Comptes courants d'associés

- Un compte par associé (`462` ou divisionnaire de `16x` selon le caractère bloqué ou non, choix de l'expert-comptable, paramétrable).
- Mouvements :
  - apports en compte courant (entrée en banque ou en caisse) ;
  - remboursements ;
  - dépenses payées personnellement par l'associé pour la société (justifiées comme une note de frais, créditées au compte courant) ;
  - affectation de dividendes.
- **Convention** : taux de rémunération, plafond, blocage éventuel.
- **Intérêts** calculés au prorata temporis en clôture (D `671` ; C compte courant), avec signalement de la part non déductible fiscalement au-delà du taux admis (paramètre fiscal).
- **Relevé de compte courant** par associé, comme un relevé de tiers.
- Note annexe : comptes courants par associé.

## 3. Registre des engagements hors bilan

### 3.1 Contenu

| Champ | Détail |
| --- | --- |
| Type | Paramétrable : caution donnée, caution reçue, garantie bancaire, nantissement, hypothèque, crédit-bail ou location longue durée, engagement de location (bail des locaux), aval, engagement d'achat |
| Sens | Donné ou reçu |
| Bénéficiaire ou donneur | Tiers |
| Montant, devise | Saisi ; pour un bail, total des loyers restant à courir, recalculé chaque mois |
| Dates | Début, fin, échéance de renouvellement, préavis |
| Opération liée | Emprunt (garantie), contrat de bail, marché |
| Documents | Contrat, acte, dans la GED |
| Statut | Actif, levé (avec date et pièce), expiré |

### 3.2 Fonctions

- **Alertes** : échéance, date de préavis de résiliation, renouvellement à décider.
- **Calcul automatique** des loyers restant dus pour les baux, et des engagements de crédit-bail : à moins d'un an, de un à cinq ans, au-delà.
- **Note annexe des engagements hors bilan** remplie automatiquement à la date de clôture, donnés et reçus, par type.
- **Lien avec les cautions clients** : les dépôts de garantie reçus des membres (compte `165`) sont au bilan, pas hors bilan. Une **garantie bancaire** remise par un client à la place d'un dépôt, elle, est un engagement reçu.

## 4. Import du journal de paie

### 4.1 Principe

La paie est produite hors du logiciel (logiciel de paie ou cabinet). Chaque mois, son **journal de paie** est importé et transformé en écriture dans le journal `PA`.

### 4.2 Import

- Fichier CSV ou Excel. Le format est décrit par un **modèle d'import paramétrable** : colonnes, séparateur, format des montants. Un modèle peut être enregistré par fournisseur de paie.
- Contenu attendu : période, matricule ou employé, rubrique (salaire de base, primes, heures supplémentaires, avantages, CNSS part salariale et patronale, IRPP / impôt sur les traitements, autres retenues, avances et acomptes retenus, net à payer), montant.
- **Correspondance des rubriques vers les comptes**, paramétrable :
  - rémunérations `661x` ;
  - charges sociales `664x` ;
  - organismes sociaux `431x` ;
  - État, impôts retenus `447x` ;
  - personnel, rémunérations dues `422` ;
  - personnel, avances et acomptes `421`.
- **Analytique** : centre de coût et site de chaque employé (fiche employé), ou ventilation fournie dans le fichier ; ligne budgétaire de personnel réalisée.

### 4.3 Contrôles avant validation

- Le fichier s'équilibre (brut = net + retenues ; charges patronales cohérentes).
- Chaque rubrique a un compte et chaque employé une section.
- La période n'a pas déjà été importée ; un réimport remplace le brouillard, jamais une écriture validée.
- **Retenues d'avances** : les montants retenus sur salaire soldent les avances au personnel (`421`) non justifiées que la direction a décidé de retenir. Elles se rapprochent des avances en retard du [circuit de justification](demandes-et-justification.md#53-solde-de-lavance).

### 4.4 Paiement des salaires

- Par virement : écriture au relevé bancaire, D `422` ; C banque, lettrée.
- En espèces : décaissement de caisse avec état d'émargement, D `422` ; C caisse.
- Versements CNSS et impôts : D `431x` / `447x` ; C banque.
- Ces échéances sociales et fiscales entrent dans le prévisionnel de trésorerie.

### 4.5 Ce qui reste hors périmètre

Le calcul de la paie (bulletins, barèmes CNSS et fiscaux, déclarations sociales) reste dans l'outil de paie. Un module de paie complet serait un chantier séparé.

## 5. Place dans les lots

| Lot | Contenu |
| --- | --- |
| **F17. Financement, hors bilan, paie** (nouveau) | Emprunts et tableaux d'amortissement, écritures d'échéance et intérêts courus, comptes courants d'associés et intérêts, registre des engagements hors bilan et alertes, notes annexes correspondantes, import du journal de paie avec correspondance des rubriques, retenues d'avances, paiement des salaires |

Dépendances : F1, F2, F10 (banque) ; F9 et F16 pour l'analytique et le budget de la paie et des intérêts ; F12 pour les notes annexes.
