# Lot D1 : socle de rendu et corrections

Branche `feature/modeles-documents`. Aucune migration. Le rendu des pièces ne change pas, sauf pour les corrections listées plus bas.

## Composants créés

| Composant | Rôle |
| --- | --- |
| `core/utils/pdf/PdfRenderer` | Rendu unique : HTML, jsoup, W3CDom, OpenHtmlToPDF. Les polices sont enregistrées par `PdfFonts` (reprise de `ContractPdfFonts`). Il remplace les 14 copies de la chaîne. La signature PAdES, le scellement, le filigrane et `PdfCopies` restent appliqués par-dessus. |
| `core/generator/qrcode/QrCodeRenderer` | Générateur de QR unique. Les copies ZXing de la facture, du reçu, de la confirmation de réservation, du courriel de réservation et du badge visiteur passent par lui. |
| `core/identity/OperatorIdentityService` et `OperatorIdentity` | Identité de l'exploitant lue depuis la fiche exploitant (`business`, `operator = true`) : raison sociale, forme, NIU, RCCM, adresse, ville, téléphone, courriel, représentant. Chaque champ vide prend la valeur historique des gabarits (ELLE A OSE, 84 Boulevard du Général Charles de Gaulle, RCCM CG-PNR-01-2017-A11-00472...). Le cache est relu après chaque modification de la fiche (`OperatorIdentityChangeListener`). |
| `templates/fragments/document-brand.html` | Fragments communs `styles`, `letterhead` et `footer`, au style de la facture : logo encre, filet doré, identité de l'exploitant. Ils sont utilisés par 25 gabarits. |
| `features/billing/service/support/BillingSellerSnapshot` | Écrit `seller_name`, `seller_niu`, `seller_address_json`, `seller_phone` et `seller_email` à l'émission d'une facture, depuis l'identité de l'exploitant. Une pièce déjà verrouillée n'est jamais réécrite. |
| `features/document/kyc/service/support/KycReportFormatter` | Libellés français du rapport KYC à la place des valeurs d'énumération brutes. |

## Gabarits passés à l'en-tête commun

- Facture, avoir, état de compte, les huit rapports `reporting/*`, et les pièces de fonds et de comptabilité (`fund/voucher.html` : état de besoin, demande de fonds, bon de décaissement, justification, bon de commande, déclaration de TVA, état de rapprochement, tableau d'emprunt).
- Caisse : pièce de mouvement, demande, rapport de clôture. La marque « Bokati Cowork » est remplacée.
- Portefeuille : relevé et reçu d'opération.
- Domiciliation : contrat et attestation. Le domiciliataire y est nommé avec son RCCM et son adresse.
- Fiche de prêt d'actif, rapport de mouvements de stock, confirmation de réservation, badge visiteur, rapport KYC.
- Contrats : `fragments/contract-brand.html` lit la même identité.

## Corrections

- **Rapport de clôture de caisse :** le gabarit lisait une variable `session` jamais fournie. Il lit désormais `summary`, donc numéro de session et code de caisse s'impriment.
- **Pièces sans numéro au registre** (tableau d'emprunt, numéroté `<code>-V<n>`) : elles n'ont plus de QR. Le QR menait à `/verify/voucher/...`, qui répondait 404 faute de pièce enregistrée. Une pièce présente au registre garde son QR.
- **Reçu de paiement :**
  - logo encre (`BrandLogo`) au lieu de `logo.png`, invisible sur fond blanc ;
  - la fausse mention « Document scellé électroniquement » devient « Document vérifiable en ligne au moyen du code QR ».
- **Contrats :** la même fausse mention est retirée.
- **Titres accentués :** Déclaration de TVA, État de rapprochement...
- **Rapport KYC :** marque, accents et libellés.
- **Facture rectificative :** imprimée « FACTURE RECTIFICATIVE » au lieu de « FACTURE ».
- **Documents de domiciliation :** fuseau `Africa/Brazzaville` au lieu de `Africa/Lagos`.
- **Adresse de vérification des pièces :** quand le paramètre `app.verify-base-url` n'a pas de valeur, la lecture à chaud retombe sur la configuration au lieu d'échouer.

## Hors de ce lot

- Courriels et pages web : `Bokati Cowork` subsiste dans des gabarits de courriel ou inutilisés (`email/booking-confirmation*`, `visitor/visitor-invitation`, `form/*`, `member/fiche-*`). Ils seront traités dans les lots E1, E2 et D6.
- Aucun gabarit n'est supprimé.

## Tests

- Nouveaux :
  - `PdfRendererTest` ;
  - tests de `core/identity` ;
  - `BillingSellerSnapshotTest`, `BillingBrandRenderTest` ;
  - `DocumentBrandFragmentRenderTest`, `FinanceDocumentsBrandRenderTest`, `OperatorBrandedDocumentsRenderTest` (identité imprimée, plus de « Bokati Cowork »).
- Les tests existants de rendu, de scellement (`BillingDocumentPdfSealingTest`) et de taille des cases de la facture passent.
- Tests d'intégration comptables : un `PlatformSettings` simulé rend désormais la valeur de repli de l'appelant (`PlatformSettingsStubs.fallbacks`). Sans cela, les services relus à chaud recevaient `null` ou `0`.
