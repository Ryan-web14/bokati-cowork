# Inventaire des documents générés (novembre 2026)

Relevé du code de la branche `feature/finance-comptabilite`, en lecture seule. Point de départ du chantier des modèles de documents ([README](README.md)). Les défauts marqués « suspecté » ou « probable » sont déduits de la lecture du code, pas observés.

J'ai inventorié les documents générés sur `feature/finance-comptabilite` (lecture seule). Chacun a son modèle ou son constructeur, son moteur de rendu, ses routes et ses défauts visibles. Quelques défauts sont déduits de la lecture du code, sans exécution : ils sont marqués « suspecté » ou « probable ».

**Comment tout est construit aujourd'hui :** chaque PDF est du HTML Thymeleaf, nettoyé avec jsoup, puis rendu par OpenHtmlToPDF (`PdfRendererBuilder`) avec PDFBox. Il n'y a aucun modèle JTE ni Freemarker (le starter Freemarker est dans le pom, mais aucun fichier `.ftl` n'existe). Il n'y a aucun export XLSX (pas d'Apache POI) : chaque export tableur est un CSV construit à la main.

Le moteur a deux résolveurs, `templates/` et `templates/email/` (`core/communication/mailService/config/EmailTemplateConfig.java:17`). C'est pourquoi des noms nus comme `"billing-document"` se résolvent vers le dossier email.

**Deux mécanismes de modèles en base existent déjà :**
- **Contrats :** table `contract_template`, entité `ContractTemplate` avec `htmlContent` / `cssContent` / `headerHtml` / `footerHtml` / sections / `variableDefinitions`. CRUD sur `/api/v1/contract-templates`. Substitution `{{token}}` dans `ContractGenerationServiceImpl.renderFromDbTemplate` (`features/contract/service/implementation/ContractGenerationServiceImpl.java:183`).
- **Notifications :** table `notification_template` avec `template_name` et un `body_template` en ligne, rendu par `NotificationTemplateRenderer.renderInline` (`features/notification/service/support/NotificationTemplateRenderer.java`).

Les chemins ci-dessous sont relatifs à `src/main/java/com/sni/bokaticowork/` (Java) ou `src/main/resources/templates/` (modèles). « En-tête de facture » désigne le style de `billing/document.html` : logo encre (repli sur le texte « ELLE A OSE »), adresse et RCCM en dur, filet doré.

---

## (1) Documents de facturation et de finance

| Document | Code / préfixe | Modèle | Moteur de rendu (fichier:ligne) | Format / sortie | Routes | En-tête et fonctions | Données transmises | Défauts |
|---|---|---|---|---|---|---|---|---|
| Facture | INVOICE. Deux séries concurrentes : `FAC` (`DocumentSequenceService:83`, numéro fiscal) et `INV` (`BillingNumberingSupport:27`, numéro de document) | `billing/document.html` | `BillingDocumentPdfServiceImpl.renderHtml:312`. Point d'entrée public `generatePdf:97` ; admin `generateAdminPdf:124` | A4, PDF en ligne, aussi pièce jointe d'email (`BillingEmailServiceImpl.sendDocument:43`) | Admin `GET /api/v1/billing/documents/{n}/pdf` (original + duplicata). Portail `GET /client/billing/invoices/{n}/pdf` et `/client/companies/{id}/invoices/{n}/pdf` (une seule copie). `POST .../send-email` | En-tête de facture. Case « Visa ELLE A OSE / Cachet et signature ». Montant en lettres (`fmt.inWords`). QR fiscal vers `/verify/doc/{n}?k=`. Duplicata via `PdfCopies`. Filigrane de statut (BROUILLON / EN RETARD / ANNULÉ). Signé PAdES et scellé (sha256 + stockage) une fois verrouillé. Page d'annexe de consommation | `document` = `BillingDocumentResponse` ; `fmt` = `BillingDocumentTemplateFormatter` ; `payments` = `List<PaymentInfo>` ; `qrCode` ; `logo` ; `customerIdentifiers` ; `companyName` / `companyIdentifiers` ; `customerVatExemptionNote` ; `usageAnnex` = `InvoiceUsageAnnex` | L'adresse, le RCCM, l'email (`coworkspace@elleaose.com`) et le téléphone (`05 204 2552`) sont en dur. Les colonnes d'instantané `seller_*` (V157) existent mais rien ne les écrit jamais : le modèle se rabat donc toujours sur les valeurs en dur. Aucun opérateur ni « établi par » affiché. Mentions légales en dur (1,5 %/mois, 40 000 XAF). Pied de page « Engineered by SNI » |
| Devis | QUOTE, `DEV` / `QUO` | même `billing/document.html` (aiguillage par type) | idem | PDF A4 | idem | Comme la facture, plus « Sans valeur fiscale », « Proposition arrêtée à… » et le texte légal du devis | idem | Comme la facture |
| Proforma | `PRF` | idem | idem | PDF A4 | idem | idem | idem | idem |
| Note de débit | `DBN` | idem | idem | PDF A4 | idem | idem | idem | idem |
| Facture rectificative | CORRECTIVE_INVOICE, `REC` | idem | idem | PDF A4 | idem | idem | idem | Pas de `th:case` pour ce type : il imprime « FACTURE » |
| Avoir | CREDIT_NOTE, `AVR` / `CRN` | `billing/avoir-note-credit.html` | `BillingDocumentPdfServiceImpl.renderCreditNoteHtml:352` | PDF A4, duplicata, sceau, filigrane comme la facture | même `/documents/{n}/pdf` | Copie propre de l'en-tête (même aspect). Ligne de signature. Montant en lettres. Pas de QR | `creditNote` = `CreditNoteView`, `customer` = `CustomerView`, `document`, `fmt`, `origin` (facture d'origine), `logo` | Adresse et RCCM en dur à trois endroits ; en-tête dupliqué, non partagé |
| Reçu de paiement | séquence `receipt` (`PaymentReceiptServiceImpl:133`) | `payment/receipt.html` | `PaymentReceiptServiceImpl.renderHtml:299` / `renderPdf:284` | PDF A4 en ligne | Admin `GET /api/v1/payments/transactions/{tx}/receipt/pdf` et `/payments/receipts/{n}/pdf` (original + duplicata). Portail `/client/billing/payments/{tx}/receipt/pdf`, `/client/companies/{id}/payments/{tx}/receipt/pdf` | Copie de l'en-tête de facture, filet doré, montant en lettres, QR vers `/verify/receipt/{n}`, « Encaissé par » (`receivedBy`) | `receipt` = `PaymentReceiptResponse` (avec la liste `PaymentReceiptAllocationResponse`), `fmt` = `PaymentReceiptTemplateFormatter`, `qrCode`, `logo` | Charge son propre logo `/static/images/logo.png`, blanc sur fond transparent ; `BrandLogo` note que ce fichier est invisible sur blanc (`BrandLogo.java`). Le pied de page dit « Document scellé électroniquement » mais les reçus ne sont jamais signés ni scellés. Adresse et RCCM en dur |
| État de compte (organisation / membre) | aucun | `billing/account-statement.html` | `company/statement/service/AccountStatementExporter.pdf:38` ; CSV dans `csv()` | PDF A4 en pièce jointe, plus CSV | `GET /api/v1/companies/{id}/statement.pdf\|.csv`, `/members/{id}/statement.pdf\|.csv`, portail `/client/billing/statement.pdf`, `/client/companies/{id}/statement.pdf` | Logo, filet doré. Pas d'adresse, de RCCM, de signature ni de QR | `statement` = `StatementDtos.Statement`, `fmt` = `AccountStatementExporter.Formatter`, `logo` | En-tête différent de la facture (pas d'identité du vendeur) |
| Relevé client (facturation) | aucun | CSV construit dans le code (`BillingDocumentController.toStatementCsv`) | `BillingDocumentController.statementCsv:339` | CSV | `GET /api/v1/billing/customers/{type}/{code}/statement.csv` | aucun | `CustomerStatementResponse` | Recoupe l'état de compte ci-dessus |
| Bon de commande fournisseur | `BC-YYYYMM-NNNNNN` (compteur de pièces) | `fund/voucher.html` (pièce générique) | `accounting/.../PurchaseService.orderPdf:270` -> `VoucherService.render:183` | PDF A4 en ligne | `GET /api/v1/accounting/purchases/orders/{n}/pdf` | Mise en page de pièce (voir section 2) | `VoucherService.View` (`Section`s construites en Java) | Les libellés de section sont construits en Java : ils ne sont pas modifiables comme un modèle |
| Déclaration de TVA | `DTV-…` ; brouillon numéroté `BROUILLON-<période>` | `fund/voucher.html` | `VatService.pdf:409` | PDF A4 | `GET /api/v1/accounting/vat-returns/{id}/pdf` ; CSV `/{id}/export` (`VatService.csv:465`) | Mise en page de pièce, montant en lettres, QR | `View` | Titre sans accents (« Declaration de TVA ») |
| État de rapprochement bancaire | `RAP-…` | `fund/voucher.html` | `BankService.pdf:763` | PDF A4 | `GET /api/v1/accounting/reconciliations/{id}/pdf` | Mise en page de pièce | `View` | Titre sans accents |
| Tableau d'amortissement d'emprunt | `EMP`, pseudo-numéro `<code>-V<n>` (non émis par un compteur) | `fund/voucher.html` | `LoanService.pdf:467` ; CSV `csv:455` | PDF A4 + CSV | `GET /api/v1/accounting/loans/{code}/pdf`, `/loans/{code}/export` | Mise en page de pièce, sans signatures | `View` | Le QR pointe vers `/verify/voucher/<pseudo-number>`, jamais enregistré : la vérification renvoie 404 |
| Bordereau de remise en banque | `BRB-…` | aucun | `ChequeService:109` n'émet qu'un numéro | - | aucune | - | - | Numéro émis mais aucun PDF n'existe (manque) |
| Retenues à la source | aucun | CSV construit dans le code | `PurchaseService.withholdingsCsv:654` | CSV | `GET /api/v1/accounting/purchases/withholdings/export` | - | - | - |
| Relevé fournisseur / relevé partenaire | aucun | aucun (JSON seulement) | `PurchaseService.statement`, `PartnerAccountService.statement` | JSON | `/accounting/purchases/suppliers/{code}/statement`, `/accounting/partners/{code}/statement` | - | - | Pas de version imprimable (manque) |

## (2) Documents de caisse et de fonds

| Document | Code / préfixe | Modèle | Moteur de rendu | Format / sortie | Routes | En-tête et fonctions | Données transmises | Défauts |
|---|---|---|---|---|---|---|---|---|
| Pièce de caisse / Bon d'entrée / Bon de sortie / Bordereau de remise de fonds / Note de frais… (libellé tiré de `CashDocumentType` ou `CashMovementType`) | numéro de mouvement (CSM-…) | `cash/movement-document.html` | `CashDocumentPdfServiceImpl.generateMovementDocument:55` | PDF A4 en ligne | `GET /api/v1/cash-registers/movements/{n}/document` | En-tête propre, marque texte **« Bokati Cowork »**, pas de logo. Deux cases de signature (émetteur `createdBy`, superviseur `signedBy`). DUPLICATA quand `printCount > 0`. « ANNULÉ PAR » | `movement` = `CashMovementResponse`, `docTypeLabel`, `fmt` = `CashDocumentTemplateFormatter`, `duplicate`, `reversedBy` | Mauvaise marque. Pas de montant en lettres. Pas de QR. Pas d'identité de la société |
| Demande de caisse (justificatif / remise de fonds / avance / remboursement) | CRQ-… | `cash/request-document.html` | `CashDocumentPdfServiceImpl.generateRequestDocument:72` | PDF A4 | `GET /api/v1/cash-registers/requests/{n}/document` | En-tête « Bokati Cowork », deux cases de signature | `request` = `CashRequestResponse`, `requestTypeLabel`, `statusLabel`, `fmt` | Comme ci-dessus |
| Rapport de clôture de caisse | CSS-… | `cash/session-closing-report.html` | `CashDocumentPdfServiceImpl.generateSessionClosingReport:84` | PDF A4 en ligne, aussi pièce jointe d'email (`CashClosingReportMailer.onFinalized`, vers la ligne 85) | `GET /api/v1/cash-registers/sessions/{n}/closing-report` | En-tête « Bokati Cowork », signatures caissier / superviseur | `summary` = `CashSessionSummaryResponse`, `statistics` = `CashSessionStatisticsResponse`, `movements` = `List<CashMovementResponse>`, `reviewedBy`, `fmt` | **Bogue suspecté :** le modèle lit `${session.sessionNumber}` / `${session.registerCode}` (lignes 5, 55, 59, 159) mais le service ne définit jamais de variable `session` |
| État de besoin | `EDB-YYYYMM-NNNNNN` | `fund/voucher.html` | `fund/voucher/FundVoucherService.need:36` -> `VoucherService.render:183` | PDF A4 en ligne | `GET /api/v1/fund-needs/{n}/pdf` | Nom de l'émetteur tiré de la configuration (`fund.voucher.issuer-name`, défaut « ELLE A OSE »), détails de l'émetteur (vides par défaut). Pas de logo, pas de filet doré. Montant en lettres. QR vers `/verify/voucher/{n}`. DUPLICATA (réimpression). « ANNULÉE PAR ». Signatures : Demandeur / Chef de service / Responsable financier. Mention OHADA | `v` = `VoucherService.View` (`Section`, `Signature`) | Titres sans accents. La cible du QR renvoie du JSON, pas une page. RCCM et adresse vides tant que le paramètre n'est pas rempli. Aspect différent de la facture |
| Demande de fonds | `DDF-…` | idem | `FundVoucherService.request:65` | PDF A4 | `GET /api/v1/fund-requests/{n}/pdf` | Signatures : demandeur plus chaque niveau d'approbation | idem | idem |
| Bon de décaissement (et complément) | `BDF-…` | idem | `FundVoucherService.disbursement:84` | PDF A4 | `GET /api/v1/fund-requests/{n}/disbursements/{mv}/pdf` | Signatures Bénéficiaire / Caissière, texte d'engagement | idem | idem |
| Fiche de justification d'avance | `JUS-…` | idem | `FundVoucherService.justification:112` | PDF A4 | `GET /api/v1/fund-requests/{n}/justification/pdf` | Signatures Bénéficiaire / Comptable | idem | idem |
| Rapports de caisse (`/reports/{kind}`) | aucun | CSV construit dans le code (`CashDashboardService.toCsv`) | `CashDashboardController.report` | JSON ou CSV | `GET /api/v1/cash-registers/reports/{kind}?format=csv` | - | `CashReportResponse` | - |
| Export du centre de contrôle des portefeuilles | aucun | CSV construit dans le code | `WalletControlCentreController.export:70` | CSV | `GET /api/v1/wallets/control/export` | - | - | - |

## (3) Contrats et documents juridiques

| Document | Modèle | Moteur de rendu | Sortie | Routes | En-tête et fonctions | Données transmises | Défauts |
|---|---|---|---|---|---|---|---|
| Contrat d'adhésion membre | `contracts/membership-agreement.html` + `fragments/contract-brand.html` (styles / en-tête / clôture / pied de page) | `ContractGenerationServiceImpl.renderHtml:153` -> `renderPdf:280` (enregistre les polices via `ContractPdfFonts`) | PDF A4 stocké dans le magasin de documents (GED, `CONTRACT_DRAFT`), envoyé par email en pièce jointe (`ContractEmailNotifier`, `OutboxNotificationMailServiceImpl:54`) | `POST /api/v1/contracts/preview`, `/generate` ; téléchargement via `/documents/{code}/download`, `/client/documents/contracts/{c}/pdf`, `/companies/{id}/contracts/{c}/pdf` | En-tête de contrat partagé : logo, filet doré, nom, adresse, RCCM, email et téléphone de l'opérateur tirés du `BusinessEntity` opérateur (`businessRepository.findFirstByOperatorTrue…`). « Fait à {ville}, le {date en lettres} ». Blocs de signature et de cachet. Pas de QR | `request` = `GenerateContractRequest`, `variables` (map : `clientX`, `operatorX`, dates), `owner*`, `business` = `BusinessEntity`, `clauses`, `logo`, `signingDate`, `placeOfSigning` | Le pied de page affirme « document scellé électroniquement » mais les contrats ne sont jamais signés. Repli « ELLE A OSE » en dur |
| Contrat de prestation entreprise | `contracts/business-service-agreement.html` | idem | idem | idem | idem | idem | idem |
| Contrat de domiciliation (version du module contrat) | `contracts/contrat-domiciliation.html` | idem | idem | idem | idem | idem | Double le contrat du module de domiciliation ci-dessous |
| Contrat d'abonnement / pass non remboursable | `contracts/subscription-pass-non-refundable.html` | idem ; déclenché par l'outbox `ContractGenerationOutboxEventProcessor` / `SubscriptionContractSupport` | idem | idem | idem, plus signature automatique de l'opérateur | idem | `SubscriptionContractSupport:459` met `operatorName="ELLE A OSE"` en dur. Repli de RCCM `CG-PNR-…` en dur dans le modèle |
| Contrat issu d'un modèle en base | ligne `contract_template` (en-tête / html / pied de page / css) | `ContractGenerationServiceImpl.renderFromDbTemplate:183` ; aperçu `ContractTemplateServiceImpl` (vers la ligne 213) | idem | `/api/v1/contract-templates/**` | Ce que contient le HTML stocké. Aucun en-tête partagé ni logo injecté | jetons `{{var}}` seulement | Pas de Thymeleaf, pas de fragment d'en-tête |
| Projet de contrat (brouillon) | construit dans le code à partir des sections `ContractDraft` + `DEFAULT_CSS` | `ContractDraftServiceImpl.renderDraftHtml:358`, `generatePdf:264` | PDF A4 vers la GED | `POST /api/v1/contract-drafts/{code}/preview`, `/generate` | Pas d'en-tête sauf si `headerHtml` en fournit un | map `{{var}}` | - |
| Avenant au contrat | construit dans le code (`AMENDMENT_CSS`) | `ContractAmendmentServiceImpl.buildAmendmentHtml:409`, `generatePdf:379`, `previewHtml:369` | PDF vers la GED | `POST /api/v1/amendments/{code}/generate-pdf`, aperçu HTML, portail `/client/documents/contracts/{c}/amendments/{a}/pdf` | Pas de logo, pas d'en-tête. « Fait à Pointe-Noire » en dur. Dates ISO. Deux cases de signature vides | entités | Visuellement différent des contrats ; ville en dur |
| Contrat de domiciliation (module de domiciliation) | `domiciliation/contract.html` | `domiciliation/service/DomiciliationDocuments.contract:44` (appelé depuis `DomiciliationService:284`) | PDF A4 vers la GED (`DOMICILIATION_CONTRACT`) | `POST /api/v1/services/domiciliation/contracts/{n}/prepare` ; client `GET /client/domiciliation/contracts/{n}/document` | Marque texte « Bokati Cowork », pas de logo. Libellés de signature domiciliataire / domicilié | Variables de contexte à plat (`legalName`, `registrationNumber`, `taxNumber`, `addressLine`, …) | Ne nomme jamais le domiciliataire (l'opérateur), ni son RCCM ni son adresse. Clauses en dur. Fuseau horaire `Africa/Lagos` |
| Attestation de domiciliation (fiscale / commerciale) | `domiciliation/certificate.html` | `DomiciliationDocuments.certificate:56` (depuis `DomiciliationService:432`) | PDF A4 vers la GED (`DOMICILIATION_CERTIFICATE`) | `POST /services/domiciliation/contracts/{n}/certificate` ; client `GET /client/domiciliation/contracts/{n}/certificate` | « Bokati Cowork », une ligne de signature. Ni QR ni vérification alors qu'elle est révocable | comme ci-dessus plus `scope`, `validUntil`, `registration*` | Identité de l'opérateur absente ; pas de QR de vérification |
| Registre des domiciliés | CSV construit dans le code | `DomiciliationController.registry:191` | CSV | `GET /api/v1/services/domiciliation/registry` | - | - | - |
| Page et email de signature électronique du devis | chaînes HTML construites dans le code | `QuoteSignaturePublicController.buildPage` (vers la ligne 30) ; corps d'email dans `QuoteSignatureServiceImpl:129` | page HTML / email | page publique `/{token}` | CSS en ligne propre | `BillingDocumentSignature`, `BillingDocumentResponse` | Aucun modèle |

## (4) Documents destinés aux membres et aux clients

| Document | Modèle | Moteur de rendu | Format / sortie | Routes | Notes et défauts |
|---|---|---|---|---|---|
| Relevé de portefeuille | `wallet/statement.html` | `payment/transfer/service/WalletStatementService.statement:58` | PDF A4 | `GET /api/v1/client/wallet/{w}/statement` (portail seulement ; pas de route admin) | Marque « Bokati Cowork », pas de logo, imprime le hash du registre. Données : variables à plat (`walletNumber`, `holderName`, `lines` = `StatementLine`, totaux, `lastHash`) |
| Reçu d'opération portefeuille | `wallet/transaction-receipt.html` | `WalletStatementService.receipt:119` | PDF **A5** | `GET /client/wallet/{w}/ledger/{tx}/receipt` | « Bokati Cowork », pas de QR, pas de montant en lettres. Données : `entry` = `WalletLedgerEntryResponse` + chaînes |
| Confirmation de réservation | `booking/confirmation-pdf.html` | `booking/service/support/BookingConfirmationDocumentService.confirmationPdf:92` | PDF A4 en pièce jointe | `GET /api/v1/public/bookings/{n}/confirmation.pdf?token=` | Marque texte « Elle A Osé », ni logo ni identité de la société. QR = URL d'enregistrement à l'arrivée (pas de vérification). Affiche la ventilation HT / TVA / CA. Variables à plat. Jumeau HTML : `booking/confirmation-view.html` (`PublicBookingController:42`, `fillViewModel`) |
| Badge visiteur | `visitor/visitor-badge.html` | `visitor/service/support/VisitorBadgeService.generateBadge:29` | PDF **A6 paysage** | `GET /api/v1/visitors/passes/{p}/badge` | QR (`VisitorQrGenerator`, sa propre copie de ZXing), texte « Elle A Osé ». Données : `pass` = entité `VisitorPass`, entité `visitor` |
| Rapport KYC | `kyc/kyc-case-report.html` | `KycServiceImpl.exportPdf:651` | PDF (pas de taille `@page`, donc par défaut) | `GET /api/v1/kyc/cases/{code}/export/pdf` | Aucune identité visuelle. Français sans accents (« Genere », « Proprietaire »). Valeurs d'énumération brutes. Données : `case` = `KycCaseResponse`, `timeline`, `expiry`, `notes` |
| Fiche de prêt d'actif | `inventory/asset-loan-sheet.html` | `inventory/asset/service/support/AssetLoanSheetPdfRenderer.render:27` | PDF A4 | `GET /api/v1/inventory/assets/{a}/assignments/{id}/loan-sheet.pdf` | « Bokati Cowork », cases de signature. Données : entités `Asset`, `AssetAssignment` ; `generatedAt` est une chaîne `Instant` ISO brute |
| Modèles inutilisés (aucune référence Java) | `billing/invoice-v2.html`, `member/fiche-client.html`, `member/fiche-inscription.html`, `form/badge-membre.html` (carte de membre avec QR), `form/planning-occupation.html` (A4 paysage), `verify/receipt.html` (les reçus sont vérifiés avec `verify/document`) | - | - | - | Code mort ; candidats à devenir des modèles ou à la suppression |

## (5) Rapports et exports internes

Les huit PDF de rapport sont rendus par `reporting/service/implementation/ReportPdfServiceImpl` (méthodes aux lignes 40-118, `renderPdf:127`). Chacun reçoit `report` = le DTO de réponse, `fmt` = `ReportFormatter`, `logo` (BrandLogo) et `generatedAt`. Tous utilisent l'en-tête ELLE A OSE avec filet doré et **codent en dur** le RCCM et Pointe-Noire.

| Rapport | Modèle | DTO de données | Route |
|---|---|---|---|
| Rapport financier (tableau de bord) | `reporting/financial-report.html` | `FinancialDashboardResponse` | `GET /api/v1/reports/finance/dashboard/pdf` (alias `/reporting/...`) |
| Rapport d'occupation | `reporting/occupancy-report.html` | `OccupancyReportResponse` | `/reports/occupancy/pdf` |
| Rapport d'activité | `reporting/activity-report.html` | `ActivityReportResponse` | `/reports/activity/pdf`, `/csv` |
| Profil membre | `reporting/member-profile-report.html` | `MemberProfileReportResponse` | `/reports/member-profile/{id}/pdf` |
| Recouvrement | `reporting/debt-recovery-report.html` (**A4 paysage**) | `DebtRecoveryReportResponse` | `/reports/debt-recovery/pdf`, `/csv` |
| Utilisation des ressources | `reporting/resource-utilization-report.html` (**A4 paysage**) | `ResourceUtilizationReportResponse` | `/reports/resource-utilization/pdf` |
| Valorisation du stock | `reporting/stock-valuation-report.html` | `StockValuationReportResponse` | `/reports/stock-valuation/pdf` |
| Abonnements | `reporting/subscription-report.html` | `SubscriptionReportResponse` | `/reports/subscriptions/pdf` |

Autres sorties internes :
- **Mouvements de stock (PDF) :** `inventory/movement-report.html`, rendu par `InventoryMovementReportPdfRenderer.render:31`, données `InventoryMovementReportResponse`, route `/api/v1/inventory/admin/reports/movements.pdf`. Marque « Elle A Ose » en dur sans accent, pas de logo.
- **Exports CSV (tous construits dans le code) :**
  - inventaire : `movements.csv`, `overrides.csv`, `reconciliation.csv` (`InventoryAdminController`), `journal.csv` (`InventoryAccountingController`)
  - documents : `/documents/export/csv` (`DocumentSearchServiceImpl.exportCsv:412`)
  - coupons : `/coupons/batches/{b}/export`
  - support : `/support/tickets/analytics.csv`
  - export de dossier de conformité : `/payments/compliance/cases/{n}/export` (JSON)
  - plus les CSV d'emprunt, de TVA, de retenues, d'états de compte, de rapports de caisse et de contrôle des portefeuilles listés plus haut
- **Scan GED vers PDF :** `document/explorer/text/GedScanAssembler.toPdf` (PDFBox) - technique, pas un modèle métier.
- **Pages HTML publiques :** `verify/document|comparison|not-found|unavailable` (`VerifyController`, avec `fragments/page`) ; `auth/password-reset-*` ; `payment/pay-form|pay-status|pay-closed` ; `booking/checkin-*`, `csat-thanks`. La page CSAT est en partie construite en Java (`SupportCsatPublicController`).

## (6) Modèles d'email

Tous dans `templates/email/` sauf mention contraire. Quand un email passe par la table `notification_template`, la migration d'amorçage est indiquée.
- `fragments/notice-layout.html` : fragments partagés d'en-tête (head / header), de pied de page et de mentions légales.
- `billing-document` : email de facture ou de devis, PDF joint (`BillingEmailServiceImpl:141`).
- `payment-confirmation` : paiement confirmé (`BillingEmailServiceImpl:181`).
- `billing-dunning`, `billing-dunning-handover` : relances d'impayés et passage de relais (table de notifications, V243).
- `billing-reminder` : inutilisé.
- `booking-confirmation` / `booking-cancelled` / `booking-completed` / `booking-event` : emails de réservation avec QR en ligne (`BookingEmailNotifier:183`).
- `booking-confirmation-v3`, `booking-reminder`, `booking-reminder-email` : inutilisés.
- `cash-payment-declared` / `-confirmed` / `-declaration-expired` : parcours de déclaration d'espèces (V249).
- `contract-event` : contrat généré ou signé, PDF joint (`ContractEmailNotifier:42`, `OutboxNotificationMailServiceImpl:54`).
- `contract-signing-request` : lien de signature électronique (Outbox `:85`).
- `crm-event` (`CrmEmailServiceImpl`), `customer-created` (`CustomerServiceImpl:340`), `document-event` (Outbox `:30`).
- `domiciliation-activated|administration-notice|certificate-expiring|certificate-issued|fiscal-lost|registration-rejected|terminated` : cycle de vie de la domiciliation (V243).
- `mail-item-event` : courrier reçu au titre de la domiciliation (V243).
- `email-confirmation`, `generated-password`, `admin-alert` : table de notifications (V74).
- `event-registration` : inscription à un événement, copies visiteur et personnel (`EventRegistrationServiceImpl:216`).
- `generic-notification` : modèle fourre-tout utilisé par la comptabilité, le portefeuille d'organisation, la caisse, la clôture de caisse (avec PDF joint) et la notification par défaut.
- `kyc-event`, `kyc-expiry-reminder` : KYC (Outbox).
- `member-created`, `subscription-expiry-reminder`, `welcome-email-v2` : inutilisés.
- `mobile-money-daily-reconciliation|deposit-failed|deposit-pending-review|deposit-unresolved` : mobile money (V245, V247, V260).
- `ott-login` : code de connexion à usage unique. `password_reset`, `password-changed-confirmation` : parcours de mot de passe.
- `refund-confirmation` (`RefundEmailNotifier`). `self-service-payment-received` (V247).
- `subscription-activated|cancelled|created` (`SubscriptionEmailNotifier:106`).
- `subscription-direct-debit-failed|grace-period|quote-sent|renewal-payment-due|termination` (V243, V275).
- `subscription_ended|_ending_soon|_ends_today` (V251).
- `support-event`, `task-event`.
- `wallet-kyc-documents-requested|low-balance|payment-request|security-event|status-changed|topup-completed|transfer-received|transfer-sent|treasury-alert` (V243).
- Hors du dossier email : `form/welcome-member-email.html` (bienvenue au membre, `MemberServiceImpl:491`) et `visitor/visitor-invitation.html` (invitation de visiteur avec QR, `VisitorEmailNotifier:51`).
- Emails construits en Java sans modèle : lien de paiement (`PaymentLinkServiceImpl:146`), expiration d'intention de paiement (`PaymentIntentExpiryAlertWorker:67`), résolution manuelle mobile money (`MobileMoneyResolutionNotifier:57`), demande de signature de devis (`QuoteSignatureServiceImpl:129`).

---

## Éléments partagés existants

- **Fragments :** `fragments/contract-brand.html` (`styles` / `letterhead` / `closing` / `footer`, contrats seulement) ; `fragments/page.html` (`pagehead` / `brand` / `brand-rule` / `foot`, pages web seulement) ; `email/fragments/notice-layout.html`. **Aucun fragment PDF n'est partagé entre la facture, l'avoir, le reçu et les rapports** : chacun copie le CSS de l'en-tête et l'identité en dur.
- **Montant en lettres :** `features/billing/service/support/FrenchAmountWords.withCurrency`, exposé via `fmt.inWords` (formateur de facturation `:565`, formateur de reçu `:394`) et `VoucherService.render` ; utilisé aussi par `FundNeedService` et `FundRequestService`. Dates en lettres : `ContractWording.inWords`.
- **QR :** `core/generator/qrcode/QrCodeRenderer.toDataUri` est partagé, mais seul `VoucherService` l'utilise. Des copies ZXing en double existent dans `BillingDocumentPdfServiceImpl:465`, `PaymentReceiptServiceImpl:320`, `BookingConfirmationDocumentService:140`, `BookingEmailNotifier:365` et `VisitorQrGenerator`. Le contenu de l'URL de vérification vient de `FiscalQrCodeService.buildContent` (`/verify/doc/{n}?k=`) ; `/verify/receipt/{n}` et `/verify/voucher/{n}` (ce dernier renvoie du JSON).
- **Copies et filigrane :** `core/utils/pdf/PdfCopies.originalAndDuplicate` (factures et reçus admin). Le filigrane de facturation est dans `BillingDocumentPdfServiceImpl.addWatermark`. Les duplicata de caisse et de pièces sont dessinés par les modèles eux-mêmes à partir des compteurs d'impression.
- **Signature et scellement :** `features/billing/service/support/PdfSignatureService` plus le scellement de `DocumentStorageService`, documents de facturation seulement.
- **Logo :** `core/utils/BrandLogo.base64()` (`/static/images/logo-ink.png`), utilisé par la facture, l'avoir, l'état de compte, les rapports et les contrats. Le reçu charge `logo.png` directement. Les emails utilisent `EmailBranding.logoUrl()`, exposé sous `#branding` via `BrandingDialect`.
- **Polices :** `ContractPdfFonts.register` (contrats seulement).
- **Chaîne PDF :** le même bloc jsoup -> W3CDom -> `PdfRendererBuilder` est copié dans 14 services. Il n'existe pas de composant `PdfRenderer` commun.

## D'où vient l'identité de la société

1. **En dur dans les modèles (dominant) :** « ELLE A OSE », « 84 Boulevard du Général Charles de Gaulle, centre-ville, Pointe-Noire », « RCCM CG-PNR-01-2017-A11-00472 », `coworkspace@elleaose.com`, « 05 204 2552 ». Présents dans `billing/document.html`, `avoir-note-credit.html`, `payment/receipt.html`, les huit `reporting/*.html`, `subscription-pass-non-refundable.html` (repli de RCCM) et `verify/receipt.html`.
2. **« Bokati Cowork » en dur** (mauvaise marque) dans `cash/*`, `wallet/*`, `domiciliation/*` et `inventory/asset-loan-sheet.html`. « Elle A Osé » en texte seul dans les modèles de réservation, de visiteur et de rapport de mouvements de stock.
3. **`BusinessEntity` opérateur** (table `business`, `operator=true` : nom, forme juridique, NIU, RCCM, adresse, téléphone, email, représentant légal), trouvé via `BusinessRepository.findFirstByOperatorTrueAndDeletedFalse` (`ContractGenerationServiceImpl:349`). **Seuls les contrats l'utilisent.** C'est la source de vérité unique naturelle.
4. **Propriétés de configuration :** `app.company.name` / `legal-name` / `niu` / `fiscal-id` / `machine-code` (`application.yml:71`) ; seul `machine-code` est lu (`DocumentSequenceService:28`). `fund.voucher.issuer-name` / `issuer-details` (`platform-settings.tsv:368-369`) ne sont lus que par `VoucherService`. `app.verify-base-url` sert aux codes QR et au logo des emails.
5. **Instantané en base inutilisé :** `billing_document.seller_name`, `seller_niu`, `seller_address_json`, `seller_phone`, `seller_email` (V157). Ils sont exposés par `BillingDocumentMapperDecorator:145` et lus par le modèle de facture, mais jamais écrits.

Les incohérences qui en résultent sont : trois noms de marque différents, quatre styles d'en-tête différents (facture, caisse/portefeuille, pièce, contrat), et aucune identité de l'opérateur sur les documents de domiciliation.
