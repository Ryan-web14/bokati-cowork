# Correctif : case de visa ELLE A OSE sur les factures

Demande : ajouter en bas des factures une case où l'exploitant signe et appose son cachet.

## Changement

- Modèle `src/main/resources/templates/billing/document.html` (factures, devis, pro forma, factures rectificatives) : après les mentions légales et avant le pied de page, une case encadrée à droite, titrée « Visa ELLE A OSE », avec un espace libre d'environ 2 cm pour la signature et le cachet, et la légende « Signature et cachet ».
- La case ne se coupe jamais entre deux pages (`page-break-inside: avoid`) : si elle ne tient pas en bas de la dernière page, elle passe entière à la page suivante.
- L'avoir (`avoir-note-credit.html`) n'est pas modifié.
- Aucune migration, aucun réglage.

## Tests

`BillingDocumentPdfLinesRenderTest.theInvoiceCarriesTheOperatorVisaBox` : la case est présente, avant le pied de page.

## Pour le frontend

Rien à changer : la case apparaît dans le PDF de la facture (téléchargement, envoi par courriel, page de vérification).
