# Écarts relevés sur la facturation

Source : la section « Écarts relevés » de `docs/final-frontend-doc/billing/README.md`. Chaque constat a été revérifié le 13/11/2026 dans le code de la branche `feature/finance-comptabilite`, qui porte le dernier état du module.

États :
- **Corrigé** : réglé dans le code avant le plan ;
- **Réglé** : réglé par un lot du plan (13/11/2026, branche `feature/facturation-devis`, détail dans [livraison.md](livraison.md)) ;
- **Ouvert** : constat confirmé, à corriger ;
- **Partiel** : une partie est réglée.

## 1. Synthèse

| N° | Écart | État | Lot |
| --- | --- | --- | --- |
| E1 | Mise à jour d'un brouillon avec remises ou clauses : lignes et taxes dupliquées | Corrigé (08/11/2026) | |
| E2 | Mise à jour sans remises : remises de document perdues dans les totaux | Réglé (B0) : exonération appliquée au recalcul | B0 |
| E3 | Imputation d'un avoir non validé ou déjà consommé | Corrigé (08/11/2026) | |
| E4 | Confirmation publique de signature : document complet renvoyé, devis annulé ou converti repassé à ACCEPTED | Réglé (B0) | B0 |
| E5 | Annulation d'une rectificative scellée et payée : refus « Payments can only be allocated to invoices » | Réglé (B0) | B0 |
| E6 | `send` écrase le statut (une facture payée repasse à SENT, un brouillon part sans émission) | Réglé (B1) | B1 |
| E7 | `validate` depuis un brouillon contourne l'émission (contrôle des remises, notification) | Réglé (B1) | B1 |
| E8 | `issue` sur un avoir le marque consommé | Réglé (B1) | B1 |
| E9 | Devis en NEGOTIATION, VIEWED ou ACCEPTED non modifiable | Réglé (B2) | B2 |
| E10 | Statuts jamais atteints : EXPIRED, VOIDED, REFUNDED ; acompte CANCELLED | Réglé (B1, B3) : EXPIRED, REFUNDED, APPLIED, acompte CANCELLED posés ; VOIDED retiré ; WRITTEN_OFF posé par la comptabilité | B1 |
| E11 | Balance âgée : devise recopiée, montants de devises différentes additionnés | Réglé (B9) | B9 |
| E12 | Échéancier : type du document non contrôlé ; un paiement direct ne met pas à jour les échéances | Réglé (B9) | B9 |
| E13 | Relevé client : pas de métadonnées de pagination, CSV limité à la première page, tri ignoré | Réglé (B9) | B9 |
| E14 | `recoveredBy` jamais renseigné ; `changedBy` pris dans le corps de la requête | Réglé (B1) | B1 |
| E15 | PDF figé à la première génération : une correction de destinataire n'y apparaît pas | Réglé (B4) : duplicata daté après correction du destinataire | B4 |
| E16 | `cashSessionNumber` obligatoire pour tout mode de paiement | Corrigé (c6089dc) | |
| E17 | Simulation sans exonération de TVA ni plafond d'encours | Réglé (B13) | B8 |
| E18 | Routes en double `/quotations/...` et `/quotes/...` | Réglé (B10) : routes /quotations dépréciées jusqu'au 13/05/2027 | B10 |
| E19 | Messages mêlant anglais et français, souvent sans accents | Réglé (B10) : messages en français accentué, code stable `reason` | B10 |

## 2. Détail et correction prévue

### E4 · Confirmation publique de signature (sécurité)

- **Constat** :
  - `POST .../quote-signatures/{token}/confirm` (`QuoteSignaturePublicController`) renvoie un `BillingDocumentResponse` complet à quiconque détient le jeton, notes internes et objets à récupérer compris ;
  - `QuoteSignatureServiceImpl.confirmSignature` ne regarde pas le statut du devis : un devis annulé, converti ou refusé après la demande repasse à ACCEPTED.
- **Correction** :
  - réponse publique réduite : numéro, date, montant TTC, devise, statut de la signature, nom de l'exploitant ; aucune note interne, aucun objet à récupérer ;
  - signature refusée (409) si le devis n'est plus en SENT, VIEWED, NEGOTIATION ou ACCEPTED ;
  - la demande de signature garde la version du devis ; une révision du devis annule les demandes en cours (le signataire signe ce qu'il a vu).

### E5 · Annuler une rectificative scellée et payée

- **Constat** : `requiresCreditNoteToCancel` vise la rectificative, mais `createCreditNote` appelle `ensureCanPay`, qui n'admet que facture et proforma.
- **Correction** : un contrôle propre à l'avoir, qui admet facture, rectificative et note de débit scellées. `ensureCanPay` reste réservé aux encaissements.

### E2 (reste) · Exonération de TVA au recalcul

- **Correction** : le recalcul d'un brouillon applique les mêmes conditions client qu'à la création (exonération de l'organisation, régime fiscal). Une seule fonction calcule la création, la mise à jour et la simulation (voir E17).

### E6, E7, E8 · Cycle de vie

Une table des transitions autorisées par type de document, appliquée par toutes les routes :

| Type | Transitions |
| --- | --- |
| Devis | DRAFT → SENT (émission) → VIEWED → NEGOTIATION → ACCEPTED, REJECTED, EXPIRED ; ACCEPTED → DEPOSIT_REQUESTED → DEPOSIT_PAID → CONVERTED ; révisable jusqu'à ACCEPTED (lot B2) |
| Facture, rectificative, note de débit | DRAFT → ISSUED (émission : contrôle des remises, notification) → certification par notre système → PARTIALLY_PAID, PAID, OVERDUE ; CANCELLED, par avoir si de l'argent a été encaissé |
| Avoir | DRAFT → ISSUED (émis, non imputé) → certification par notre système → APPLIED (imputé) ou REFUNDED (remboursé) |
| Proforma | Voir le lot B5 |

- `send` ne change plus le statut : il trace l'envoi (date, canal, destinataires). Un brouillon s'émet avant de partir.
- `validate` exige un document émis. Un brouillon passe d'abord par l'émission et ses contrôles.
- Un avoir émis n'est « consommé » qu'une fois imputé ou remboursé, avec un statut distinct.

### E10 · Statuts

- `EXPIRED` : tâche quotidienne sur les devis dont la date de validité est passée (SENT, VIEWED, NEGOTIATION), avec notification au commercial.
- `REFUNDED` : avoir remboursé (portefeuille, espèces, virement).
- `VOIDED` : retiré s'il n'a aucun sens distinct de CANCELLED (décision D7 du [README](README.md#5-décisions-à-prendre)).
- Acompte `CANCELLED` : acompte demandé puis abandonné (devis refusé ou expiré sans encaissement).

### E14 · Auteurs

- `recoveredBy` et `changedBy` viennent de l'utilisateur connecté, jamais du corps de la requête.

### E15 · PDF figé

- Le PDF scellé reste la référence fiscale. Une correction de destinataire après scellement produit un **duplicata** daté, avec la mention de la correction ; l'original et son empreinte sont gardés.

### E17 · Simulation

- La simulation passe par le même calcul que la création : exonération de TVA de l'organisation, plafond d'encours (avertissement), politique de crédit de la comptabilité (lot F19).

### E11, E12, E13 · États clients

Voir le lot B9 dans [ameliorations.md](ameliorations.md#b9-relevé-client-balance-âgée-et-échéanciers).

### E18, E19 · Routes et messages

- Une seule famille de routes, `/quotes/...`. Les routes `/quotations/...` restent six mois, marquées dépréciées, puis disparaissent.
- Tous les messages d'erreur en français accentué, avec un code stable que le frontend peut tester.
