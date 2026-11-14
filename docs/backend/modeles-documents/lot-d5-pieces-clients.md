# Lot D5 : pièces clients

Branche `feature/modeles-documents`. Migrations : dev `V353__modeles_documents_d5.sql`, prod `V347__modeles_documents_d5.sql`.

## Ce qui change pour l'exploitant

- Six pièces clients passent en modèles :

  | Pièce | Modèle |
  | --- | --- |
  | Facture, devis, proforma, note de débit et facture rectificative | `INVOICE` |
  | Avoir | `CREDIT_NOTE` |
  | Reçu de paiement | `PAYMENT_RECEIPT` |
  | Relevé de portefeuille | `WALLET_STATEMENT` |
  | Reçu d'opération de portefeuille | `WALLET_RECEIPT` |
  | Confirmation de réservation | `BOOKING_CONFIRMATION` |

- **Brouillons de départ non publiés :** rien ne change à l'impression tant que l'exploitant ne publie pas.
  - Le gabarit de la facture garde des éléments qu'un modèle en blocs ne reproduit pas encore : acomptes déduits, annexe de consommation, visa signé, mentions d'exonération par règle.
  - Il vaut mieux comparer l'aperçu sur des factures réelles avant de publier `INVOICE`.
- **Version figée par pièce (factures et avoirs) :**
  - une pièce verrouillée retient, à son premier rendu, la version de modèle qui l'a produite (colonne `billing_document.template_version`) ;
  - `0` veut dire « gabarit d'origine » ;
  - une réimpression, un duplicata ou une régénération garde cette version, même après la publication d'un nouveau modèle ;
  - le PDF scellé (empreinte, signature PAdES) reste la référence ; ce choix ne vaut que lorsqu'il faut le reproduire.
- **Contrats :** ils gardent `contract_template` et l'en-tête commun posé au lot D1.

## Données des pièces

| Modèle | Construction | Aperçu avec une pièce réelle |
| --- | --- | --- |
| `INVOICE`, `CREDIT_NOTE` | `BillingDocumentData` : client, pièce, lignes (désignation, quantité, unité, PU HT, remise, HT, TVA, TTC), totaux, conditions, QR fiscal | oui, par le numéro de pièce |
| `PAYMENT_RECEIPT` | `PaymentReceiptDocumentData` : paiement, client, pièces réglées, montants remis et rendus, QR de vérification | oui, par le numéro de reçu |
| `WALLET_STATEMENT`, `WALLET_RECEIPT` | variables du gabarit d'origine (`DocumentData.fieldsFrom`) et tableau des opérations | non : exemple seulement |
| `BOOKING_CONFIRMATION` | variables du gabarit d'origine, QR d'arrivée | non : exemple seulement |

## Composants

- `DocumentTemplatePrinter.tryRenderPinned` : rendu avec une version donnée, même archivée. Renvoie le PDF et le numéro de version.
- `BillingDocumentPdfServiceImpl` :
  - `renderWithTemplate` essaie le modèle avant le gabarit et fige la version d'une pièce verrouillée (`BillingDocumentRepository.pinTemplateVersion`, mise à jour ciblée hors des colonnes protégées par les déclencheurs d'immuabilité) ;
  - `templateData` fournit les données de l'aperçu.
- `PaymentReceiptServiceImpl`, `WalletStatementService` et `BookingConfirmationDocumentService` essaient le modèle avant leur gabarit.

## Tests

- `DocumentTemplateD5IT` (schéma de production) :
  - les six brouillons se rendent avec les données d'exemple, sans version publiée ;
  - une pièce figée sur une version garde cette version après une nouvelle publication ;
  - une pièce figée à 0 reste sur le gabarit d'origine.
- Les tests de rendu existants (factures, reçus, portefeuille, réservation, pièces de caisse et de fonds) passent.
