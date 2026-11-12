# Correctif : case de visa ELLE A OSE sur les factures

Demande : ajouter en bas des factures une case où l'exploitant signe et appose son cachet.

## Changement

- Modèle `src/main/resources/templates/billing/document.html` (factures, devis, pro forma, factures rectificatives) : le bas de document est un tableau à deux colonnes.
  - **À gauche** : les « Conditions et modalités de règlement » et les « Mentions légales ».
  - **À droite** : une case fermée (cadre complet) de 5 cm de large, avec l'en-tête « Visa ELLE A OSE », une case vide de 4,5 cm de haut pour le cachet et la signature, et la légende « Cachet et signature ». Les dimensions sont en centimètres, fixées par l'exploitant.
- Placée à côté des textes et non en dessous, la case n'ajoute presque pas de hauteur : une facture d'une ligne tient sur une seule page, bas de page compris (la première version, posée sous les mentions, faisait passer la facture sur deux pages).
- Historique : la deuxième version (case de 2,2 cm de haut) était trop petite pour le cachet ; une troisième la portait à 6,6 cm sur 6 cm ; l'exploitant a ensuite demandé 5 cm de large et 4,5 cm de haut pour la case du cachet. La facture d'une ligne tient sur une page.
- Le bloc ne se coupe jamais entre deux pages (`page-break-inside: avoid`).
- L'avoir (`avoir-note-credit.html`) n'est pas modifié.
- Aucune migration, aucun réglage.

## Tests

- `BillingDocumentPdfLinesRenderTest.theInvoiceCarriesTheOperatorVisaBox` : la case est présente, avant le pied de page.
- `BillingDocumentOnePageTest` : le vrai PDF d'une facture d'une ligne, avec logo, QR, conditions, instructions de règlement et coordonnées bancaires, fait une seule page.
- `BillingDocumentOnePageTest.theStampCellKeepsItsSize` : la case du cachet mesure au moins 4,5 cm de haut, le visa au moins 5 cm de large.

## Pour le frontend

Rien à changer : la case apparaît dans le PDF de la facture (téléchargement, envoi par courriel, page de vérification).
