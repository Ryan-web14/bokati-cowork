# billing : fonctionnement

> Rédigé d'après le code. Routes exactes, droits et champs JSON : [routes.md](routes.md) et `routes/`. Règles communes : [README racine](../README.md).

## En bref

Le module produit et suit les documents de facturation : devis, factures, factures proforma, avoirs, factures rectificatives et notes de débit. Il calcule les montants (TVA, centime additionnel, remises), applique les limites de remise, scelle fiscalement les documents (SEFC : numéro fiscal, chaîne de hachage, signature), génère les PDF, envoie les courriels, gère les échéanciers, les relances d'impayés, le catalogue de services facturables et les objets à récupérer chez le client.

Toutes les routes `/sni/api/v1/billing/**` et `/sni/api/v1/admin/fiscal-integrity/**` sont du **back-office**. Le seul point public est la signature électronique d'un devis (`/public/quotes/sign/{token}`). Le membre voit ses factures par le module [portal](../portal/README.md) (`/sni/api/v1/client/billing/**`), pas par ce module.

Modules voisins : [payment](../payment/README.md) (encaissements, intentions de paiement, portefeuille ; `POST .../invoices/{documentNumber}/pay` est servi ici mais exécuté par `PaymentService`), [company](../company/README.md) (conditions de l'organisation cliente : exonération de TVA, échéance, plafond d'encours, blocage, rattachement des factures de membres), [subscription](../subscription/README.md) (factures de renouvellement, promotions, suspension par la relance), [booking](../booking/README.md) et [marketplace](../marketplace/README.md) (factures créées et annulées automatiquement), [verify](../verify/README.md) (vérification publique d'un PDF par son QR code).

## Sommaire

- [Notions](#notions)
- [Statuts et cycle de vie](#statuts-et-cycle-de-vie)
- [Parcours](#parcours)
- [Règles et erreurs à refléter dans l'écran](#règles-et-erreurs-à-refléter-dans-lécran)
- [À prévoir côté frontend](#à-prévoir-côté-frontend)
- [Effets automatiques](#effets-automatiques)
- [Écarts relevés](#écarts-relevés)

## Notions

| Objet | Identifiant exposé | Ce que c'est |
| --- | --- | --- |
| Document de facturation (`BillingDocumentResponse`) | `documentNumber`, texte lisible | Un devis, une facture, un avoir, etc. Le type est dans `documentType`. Aucun `id` numérique n'est exposé : toutes les routes prennent le numéro. |
| Numéro fiscal | `fiscalNumber` | Attribué uniquement à la validation (scellement SEFC). Format `FAC20260617-00001abc-BKTWK` : préfixe par type (`FAC` facture, `AVR` avoir, `REC` rectificative, `DBN` note de débit, `PRF` proforma), date, séquence annuelle, clé de contrôle de 3 caractères, code machine (`src/main/java/com/sni/bokaticowork/features/billing/service/fiscal/DocumentSequenceService.java`). |
| Ligne | `lineOrder` (entier, position dans le document) | Pas d'identifiant propre. On modifie, supprime ou sélectionne une ligne par son `lineOrder`. |
| Remise de document | aucune (liste `discounts`) | Pourcentage ou montant fixe, appliqué sur le TTC. Peut porter `sourceType`/`sourceCode` (origine, par exemple une promotion du module subscription). |
| Acompte (`advance`) | aucun (un par document) | Part à verser d'avance, en pourcentage ou montant, calculée sur tout ou partie des lignes. |
| Escompte (`earlyPaymentDiscount`) | aucun | Mention « Escompte de X % si règlement avant le JJ/MM/AAAA » imprimée. Calculé sur le total, il ne modifie pas le total. |
| Version archivée | `versionNumber` (1, 2, ...) | Instantané du document **avant** chaque modification (`PUT`). La version courante est le document lui-même. |
| Échéancier (`PaymentScheduleResponse`) | `scheduleNumber` | Découpage du solde d'une facture en échéances (`installmentNumber`). Un seul échéancier `ACTIVE` par facture. |
| Objet à récupérer (`BillingRecoverableResponse`) | `recoverableNumber` | Objet remis au client (badge, clé, matériel) à reprendre, rattaché à un document. |
| Article du catalogue (`ServiceCatalogItemResponse`) | `itemCode` | Prestation facturable avec prix, unité, plancher de prix, remise maximale, politique de remise. Désigné dans une ligne par `itemCode` (alias accepté : `catalogSourceCode`). |
| Politique de relance | `policyCode` (choisi à la création) | Paliers de relance d'impayés par segment de client. |
| Relance exécutée (`NoticeView`) | `documentNumber` + `stepOrder` | Trace d'un palier exécuté pour une facture, une seule fois. |
| Signature de devis | jeton de 64 caractères (dans le lien envoyé) | Demande de signature électronique d'un devis, valable 7 jours. |

**Format du numéro de document** : `<préfixe>-<abrégé du type de client>-<AAAAMMJJ>-<séquence>`, par exemple `INV-MAN-20260904-00000018`. Préfixes : `QUO` devis, `PRF` proforma, `INV` facture, `CRN` avoir, `REC` rectificative, `DBN` note de débit (`src/main/java/com/sni/bokaticowork/features/billing/service/support/BillingNumberingSupport.java`). Le préfixe permet de reconnaître le type sans charger le document.

**Client du document** : `customerType` + `customerCode`. Types reconnus : `MEMBER` (code `MBR-...`), `CUSTOMER` (`CUS-...`), `BUSINESS_ENTITY` (alias `BUSINESS`). Le serveur relit la fiche et fige nom, courriel, téléphone, adresse et identifiants fiscaux sur le document. Sans type ni code, on peut facturer un tiers libre en donnant `customerName` : le type devient `MANUAL` et un code est généré.

**Organisation payeuse** : une facture de membre peut être émise pour le compte d'une organisation (`companyCustomerCode`, `companyName`, `companyLinkSource`). À la création (`POST /sni/api/v1/billing/documents` seulement), `companyCustomerCode` vide suit le mode de rattachement du membre, `NONE` force le membre seul, un code client désigne l'organisation. Le rattachement après coup se fait dans le module company (`/sni/api/v1/billing/documents/company-links`).

**Ce que pèse un document sur le solde du client** : deux champs calculés par le serveur, à utiliser au lieu de `balanceDue` :
- `receivable` : vrai si le document est une créance en cours (facture ou proforma émise, ni brouillon, ni annulée, ni réglée).
- `customerImpact` : montant signé. Positif si le client doit, négatif pour un avoir disponible, zéro sinon. La somme de `customerImpact` sur une liste donne le solde juste (`src/main/java/com/sni/bokaticowork/features/billing/service/support/BillingReceivables.java`).

**Scellement (`locked`)** : un document validé (`locked = true`) est immuable en base (déclencheur `trg_billing_document_immutable`, `src/main/resources/db/migration/V192__billing_immutability_triggers.sql`) : montants, client, raison sociale, NIU, adresse de facturation, date d'émission, devise, lignes. On ne le corrige que par un avoir ou une facture rectificative. Seuls le statut, les dates de suivi, le montant payé, le solde et les coordonnées de contact évoluent encore.

## Statuts et cycle de vie

Les statuts sont communs à tous les types (`BillingDocumentStatus`), mais chaque type n'en parcourt qu'une partie. `EXPIRED`, `VOIDED`, `REFUNDED` et `WRITTEN_OFF` existent dans l'énumération mais **aucun code ne les attribue** : ne pas prévoir d'action qui y mène.

### Devis (`QUOTE`)

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `DRAFT` | Brouillon modifiable | création (`POST .../quotes/manual`, `POST .../documents`, `POST .../documents/{n}/duplicate`) | `SENT` (issue ou send), `ACCEPTED`, `REJECTED`, `CANCELLED` |
| `SENT` | Transmis au client | `PATCH .../documents/{n}/issue` (un devis émis passe directement à `SENT`) ou `PATCH .../documents/{n}/send` | `VIEWED`, `NEGOTIATION`, `ACCEPTED`, `REJECTED`, `CANCELLED` |
| `VIEWED` | Ouvert par le client | `PATCH .../quotes/{n}/mark-viewed` (seulement depuis `SENT`, sinon la route renvoie le devis inchangé) | `NEGOTIATION`, `ACCEPTED`, `REJECTED` |
| `NEGOTIATION` | En discussion | `PATCH .../quotes/{n}/start-negotiation` depuis `SENT`, `VIEWED` ou `ACCEPTED` | `ACCEPTED`, `REJECTED` |
| `ACCEPTED` | Accepté | `PATCH .../quotes/{n}/accept` depuis `DRAFT`, `SENT`, `VIEWED`, `NEGOTIATION` ; ou signature publique confirmée (depuis n'importe quel statut) | `DEPOSIT_REQUESTED`, `CONVERTED`, `NEGOTIATION`, `REJECTED` |
| `DEPOSIT_REQUESTED` | Acompte demandé | `PATCH .../quotes/{n}/request-deposit` (seulement depuis `ACCEPTED`) | `DEPOSIT_PAID`, `REJECTED` |
| `DEPOSIT_PAID` | Acompte reçu | `PATCH .../quotes/{n}/mark-deposit-paid` (seulement depuis `DEPOSIT_REQUESTED`) | `CONVERTED`, `REJECTED` |
| `CONVERTED` | Transformé en facture | `POST .../quotes/{n}/convert-to-invoice` depuis `ACCEPTED` ou `DEPOSIT_PAID` | aucun (seule `DELETE` reste possible) |
| `REJECTED` | Refusé | `PATCH .../quotes/{n}/reject` depuis tout statut sauf `CONVERTED` ; `balanceDue` passe à 0 | aucun prévu |
| `CANCELLED` | Annulé et archivé | `DELETE .../documents/{n}` | aucun |

Les routes `/sni/api/v1/billing/quotations/...` (`manual`, `accept`, `reject`, `convert-to-invoice`) sont des alias exacts des routes `/quotes/...` : n'en utiliser qu'une famille.

### Facture (`INVOICE`), proforma (`PROFORMA_INVOICE`), rectificative (`CORRECTIVE_INVOICE`), note de débit (`DEBIT_NOTE`)

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `DRAFT` | Brouillon, pas encore une créance | création | `ISSUED`, `VALIDATED`, `SENT`, `CANCELLED` |
| `ISSUED` | Émise, non scellée | `PATCH .../documents/{n}/issue` ; ou annulation d'un paiement qui ramène le payé à 0 | `VALIDATED`, `SENT`, `PARTIALLY_PAID`, `PAID`, `OVERDUE`, `CANCELLED` |
| `VALIDATED` | Scellée fiscalement (`locked = true`, `fiscalNumber` attribué) | `PATCH .../documents/{n}/validate` depuis `DRAFT`, `ISSUED` ou `PAID` ; ou **automatiquement** après un paiement complet passé par une intention de paiement (voir [Effets automatiques](#effets-automatiques)) | `SENT`, `PARTIALLY_PAID`, `PAID`, `OVERDUE`, `CANCELLED` |
| `SENT` | Transmise | `PATCH .../documents/{n}/send` depuis tout statut sauf `CANCELLED`, `VOIDED`, `REFUNDED` | `PARTIALLY_PAID`, `PAID`, `OVERDUE`, `CANCELLED` |
| `PARTIALLY_PAID` | Payée en partie | paiement, avoir imputé, versement d'échéance, conversion d'un devis avec acompte | `PAID`, `OVERDUE`, `ISSUED` (paiement annulé), `CANCELLED` |
| `PAID` | Soldée | paiement qui solde (`balanceDue` = 0) | `VALIDATED` (scellement automatique ou manuel si non scellée), `PARTIALLY_PAID`/`ISSUED` (paiement annulé), `CANCELLED` |
| `OVERDUE` | Échue avec un solde | worker horaire : statut `ISSUED`, `VALIDATED`, `SENT`, `VIEWED` ou `PARTIALLY_PAID`, `dueDate` passée, `balanceDue > 0` | `PARTIALLY_PAID`, `PAID`, `CANCELLED` |
| `CANCELLED` | Annulée et archivée (`archivedAt`) | `DELETE .../documents/{n}` ; ou annulation automatique par booking, marketplace, subscription, payment | aucun |

Une facture **payée** peut donc finir en `PAID` **ou** en `VALIDATED` avec `balanceDue = 0` : une facture non scellée soldée par une intention de paiement est scellée juste après et passe de `PAID` à `VALIDATED`. Pour afficher « payée », lire `balanceDue`, `paidAmount` et `receivable`, pas le seul statut.

### Avoir (`CREDIT_NOTE`)

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `DRAFT` | Créé, sans valeur | `POST .../invoices/{n}/credit-note` (sans `validateImmediately`) | `VALIDATED`, `CANCELLED` |
| `VALIDATED` | Scellé et **disponible** (compté dans `totalCreditAvailable`, `customerImpact` négatif) | `PATCH .../documents/{n}/validate` ou `validateImmediately: true` | `ISSUED` |
| `ISSUED` | **Consommé** : imputé sur une facture ou reversé au portefeuille | `PATCH .../credit-notes/{n}/apply`, `PATCH .../credit-notes/{n}/apply-to/{facture}`, `POST .../credit-notes/{n}/refund-to-wallet` | `CANCELLED` (l'imputation sur la facture est défaite ; refus si reversé au portefeuille) |
| `CANCELLED` | Annulé | `DELETE .../documents/{n}` | aucun |

Sur un avoir, `ISSUED` veut dire « consommé ». Ne jamais appeler `PATCH .../issue` sur un avoir : il passerait à `ISSUED` sans avoir été imputé (voir [Écarts relevés](#écarts-relevés)).

### Autres objets

| Objet | Statuts | Transitions |
| --- | --- | --- |
| Échéancier | `ACTIVE`, `COMPLETED`, `CANCELLED` | créé `ACTIVE` ; `COMPLETED` quand la somme versée atteint le total ; `CANCELLED` par `DELETE .../schedules/{n}` (refusé si `COMPLETED`) |
| Échéance | `PENDING`, `PARTIALLY_PAID`, `PAID`, `OVERDUE`, `CANCELLED` | `OVERDUE` posé par le worker horaire (`PENDING` dont la date est passée) ; `PARTIALLY_PAID`/`PAID` par `POST .../installments/{n}/pay` ; `CANCELLED` pour les `PENDING`/`OVERDUE` à l'annulation de l'échéancier |
| Acompte (`advance.status`) | `PENDING`, `PARTIALLY_PAID`, `PAID`, `CANCELLED` | suit le montant payé de la facture ; `PAID` sur un devis par `mark-deposit-paid` ; `CANCELLED` jamais posé par le code |
| Signature de devis | `PENDING`, `SIGNED`, `EXPIRED` | `SIGNED` à la confirmation publique ; `EXPIRED` posé seulement quand le lien est ouvert après 7 jours |
| Objet à récupérer | `PENDING`, `PARTIALLY_RECOVERED`, `RECOVERED`, `WRITTEN_OFF` (texte libre, pas une énumération) | `recover` avec `partialQuantity` inférieure à la quantité : `PARTIALLY_RECOVERED`, sinon `RECOVERED` ; `write-off` : `WRITTEN_OFF` |
| Article du catalogue | `active` vrai ou faux | `activate` / `deactivate` ; `DELETE` supprime pour de bon |
| Politique de relance | `active` vrai ou faux | une seule active par segment : en activer une désactive l'autre |
| Relance exécutée | `SENT`, `SKIPPED`, `FAILED` | posé par le worker de relance |

## Parcours

Préfixe commun de toutes les routes back-office ci-dessous : `/sni/api/v1/billing`. Les liens pointent vers [routes/BillingDocumentController.md](routes/BillingDocumentController.md) sauf mention contraire.

### 1. Liste des documents

1. `GET /sni/api/v1/billing/documents` ([détail](routes/BillingDocumentController.md#r-get-sni-api-v1-billing-documents)) avec les filtres `type`, `status`, `customerType`, `customerCode`, `sourceType`, `sourceCode`, `fromDate`, `toDate` (sur `issueDate`, format `AAAA-MM-JJ`), `searchText` (numéro, nom, code et courriel du client, titre, description, code source), `page`, `size`.
2. Réponse : page maison `PaginatedResponse<BillingDocumentResponse>`. Le tri est **toujours** `createdAt` décroissant : le paramètre `sort` est ignoré.
3. Afficher par ligne : `documentNumber`, `documentType`, `status`, `customerName`, `totalAmount`, `balanceDue`, `dueDate`, un badge « scellé » si `locked`, et le `fiscalNumber` quand il existe. Les documents annulés restent listés (filtrer `status` si besoin).

### 2. Créer un devis ou une facture à la main

1. Facultatif, à chaque saisie : `POST /sni/api/v1/billing/documents/simulate` ([détail](routes/BillingDocumentController.md#r-post-sni-api-v1-billing-documents-simulate)) avec `lines` (obligatoire) et `discounts`. Rien n'est créé, aucun numéro consommé. Ce point d'entrée accepte la charge utile complète de la création (propriétés inconnues ignorées, seule exception au JSON strict). La réponse donne par ligne brut, remise, taux de remise effectif, net, et au total `subtotalAmount`, `lineDiscountAmount`, `documentDiscountAmount`, `taxAmount`, `totalAmount`, ainsi que `violations` (dépassements de remise) et `blocked` (au moins un dépassement bloquant).
2. Charger les articles : `GET /sni/api/v1/billing/catalog/lookup?q=...` ([détail](routes/ServiceCatalogController.md#r-get-sni-api-v1-billing-catalog-lookup)) cherche à la fois dans le catalogue de services, l'inventaire et les ressources réservables (paramètre `sources` : `SERVICE_CATALOG`, `INVENTORY_ITEM`, `RESOURCE`).
3. Créer : `POST /sni/api/v1/billing/quotes/manual` (devis) ou `POST /sni/api/v1/billing/invoices/manual` (facture). Champs clés : `currency` (obligatoire), `lines` (au moins une ; chaque ligne `description` et `unitPrice` obligatoires, `itemCode` pour rattacher un article), `customerType` + `customerCode` (ou `customerName` pour un tiers libre), `dueDate`, `discounts`, `clauses`, `advance`, `earlyPaymentDiscount`. Pour un autre type (proforma, note de débit) ou pour choisir l'organisation payeuse (`companyCustomerCode`), utiliser `POST /sni/api/v1/billing/documents` avec `documentType`.
4. Réponse : le document en `DRAFT`, avec ses totaux calculés par le serveur. Si `dueDate` est vide et que le client est une organisation avec un délai de paiement, l'échéance est calculée (`issueDate` + délai). Une organisation exonérée de TVA est facturée hors taxe quoi que l'on saisisse.
5. Écran suivant : la fiche du document avec les actions possibles selon le statut.

Détail de calcul à reproduire si l'écran affiche un total avant simulation : remises de ligne (`discountRate` et `discountAmount` s'additionnent, plafonnées au brut de la ligne) ; TVA puis centime additionnel calculé sur la TVA ; remises de document **additives** (10 % puis 5 % retirent 15 %, l'ordre ne compte pas) appliquées sur le **TTC**, puis ventilées au prorata sur la base, la TVA et le centime ; lignes `optional: true` exclues des totaux (`optionsTotal` les récapitule). Mieux vaut appeler la simulation que recalculer.

### 3. Fiche d'un document

1. `GET /sni/api/v1/billing/documents/{documentNumber}` ([détail](routes/BillingDocumentController.md#r-get-sni-api-v1-billing-documents-documentnumber)).
2. Afficher : en-tête (client, organisation payeuse `companyName`, dates, `title`), lignes, remises, taxes, clauses, acompte, escompte, totaux, `paidAmount`, `balanceDue`, bloc fiscal si `locked` (`fiscalNumber`, `fiscalDate`, `validatedAt`, empreinte `currentHash`), lien vers le document d'origine pour un avoir ou une rectificative (`originalDocumentNumber`, `creditNoteReason`), la dernière demande de signature (`signature`), les objets à récupérer (`recoverables`), les notes internes (`internalNotes`).
3. Proposer les actions selon `documentType`, `status` et `locked` (voir la table « Actions par état » de la section [À prévoir côté frontend](#à-prévoir-côté-frontend)).

### 4. Modifier un document non scellé

1. `PUT /sni/api/v1/billing/documents/{documentNumber}` ([détail](routes/BillingDocumentController.md#r-put-sni-api-v1-billing-documents-documentnumber)).
2. En `DRAFT` : tous les champs. Les lignes se modifient par opération : `lines: [{ "action": "UPSERT", "lineOrder": 2, "description": "...", "unitPrice": 15000, ... }, { "action": "REMOVE", "lineOrder": 3, "description": "x" }]`. `description` est obligatoire même pour `REMOVE`. Une ligne non citée reste telle quelle. `discounts` et `clauses`, s'ils sont envoyés, **remplacent** toutes les remises ou clauses.
3. En `ISSUED` ou `SENT` (non scellé) : seuls `dueDate`, `terms`, `paymentInstructions`, `internalNotes` sont pris en compte ; le reste est ignoré sans erreur.
4. Tout autre statut, ou document scellé : `400`.
5. Chaque appel archive d'abord l'état précédent comme nouvelle version (`editType` `FULL_EDIT` ou `RESTRICTED_EDIT`).
6. Un champ texte envoyé vide ne vide pas la valeur (il est ignoré), sauf `internalNotes` : chaîne vide efface la note.

`discounts` et `clauses` peuvent être envoyés dans le même `PUT` que les lignes (corrigé le 2026-11-08).

### 5. Corriger les coordonnées du destinataire

1. `PATCH /sni/api/v1/billing/documents/{documentNumber}/recipient` ([détail](routes/BillingDocumentController.md#r-patch-sni-api-v1-billing-documents-documentnumber-recipient)) avec les seuls champs à changer, plus `reason`. `null` ou absent : inchangé ; chaîne vide : efface.
2. Possible à tout statut. Sur un document scellé, `customerName`, `customerNiu` et `billingAddressJson` sont refusés en `400` (message qui liste les champs bloqués et rappelle de passer par un avoir ou une rectificative) ; courriel, téléphone, catégorie, référence et adresse de livraison restent corrigeables.
3. Le client rattaché (`customerType`, `customerCode`) ne change jamais par cette route.

### 6. Émettre, sceller, transmettre une facture

1. Émettre : `PATCH /sni/api/v1/billing/documents/{documentNumber}/issue` ([détail](routes/BillingDocumentController.md#r-patch-sni-api-v1-billing-documents-documentnumber-issue)). Seulement depuis `DRAFT`. C'est ici que les limites de remise bloquent. Si la réponse est `400` « Remise hors limites : ... Un motif de dérogation est requis », proposer à l'utilisateur qui porte le droit `BILLING:DISCOUNT_OVERRIDE` de saisir un motif et rappeler la route avec `?discountOverrideReason=...` (paramètre de requête). Le motif est conservé et tracé. Facture : statut `ISSUED`. Devis : statut `SENT`. Le membre reçoit une notification dans son portail.
2. Sceller : `PATCH /sni/api/v1/billing/documents/{documentNumber}/validate` ([détail](routes/BillingDocumentController.md#r-patch-sni-api-v1-billing-documents-documentnumber-validate)). Types admis : facture, proforma, avoir, rectificative, note de débit (pas un devis). Statuts admis : `DRAFT`, `ISSUED`, `PAID`. Effet : `locked = true`, `fiscalNumber`, `fiscalDate`, chaîne de hachage, signature, statut `VALIDATED`. **Irréversible** : demander une confirmation explicite.
3. Marquer comme transmis : `PATCH /sni/api/v1/billing/documents/{documentNumber}/send`. Change le statut en `SENT`, date `sentAt` et marque la dernière version archivée comme transmise. **N'envoie aucun courriel.**
4. Envoyer le courriel : `POST /sni/api/v1/billing/documents/{documentNumber}/send-email` ([détail](routes/BillingDocumentController.md#r-post-sni-api-v1-billing-documents-documentnumber-send-email)). Réponse : `true` ou `false`. Envoie au `customerEmail` le PDF en pièce jointe (exemplaire unique, sans duplicata) avec un texte adapté aux montants (disponible, règlement partiel enregistré, soldée, rappel si `OVERDUE`). Ne change pas le statut. `400` « Billing document customer has no email » si le document n'a pas de courriel : proposer de le corriger par la route du destinataire.

Ordre conseillé pour une facture : créer, (simuler), émettre, sceller, puis `send` et `send-email`. Le scellement peut aussi être laissé au paiement (voir [Effets automatiques](#effets-automatiques)).

### 7. Télécharger le PDF

1. `GET /sni/api/v1/billing/documents/{documentNumber}/pdf` ([détail](routes/BillingDocumentController.md#r-get-sni-api-v1-billing-documents-documentnumber-pdf)) : octets PDF, `Content-Disposition: inline; filename="<documentNumber>.pdf"`. Appeler avec le jeton, puis afficher via un `Blob`.
2. Le PDF du back-office contient **deux exemplaires** : l'original et un duplicata marqué « DUPLICATA ». Le portail et les courriels servent l'exemplaire unique.
3. Filigrane : `BROUILLON` (brouillon non scellé), `EN RETARD` (non scellé et `OVERDUE`), `ANNULÉ` (annulé). Un document scellé n'a de filigrane que s'il est annulé.
4. Un document scellé a un PDF figé dès sa première génération (signé si la signature PDF est activée, empreinte conservée) : les changements ultérieurs de statut ou de contact n'y apparaissent pas. Le QR code imprimé renvoie vers la page publique `/verify/doc/{documentNumber}` (module [verify](../verify/README.md)).
5. Gabarits : `src/main/resources/templates/billing/document.html` (tous types sauf avoir) et `avoir-note-credit.html` (avoir).

### 8. Encaisser une facture au guichet

1. `POST /sni/api/v1/billing/invoices/{documentNumber}/pay` ([détail](routes/BillingDocumentController.md#r-post-sni-api-v1-billing-invoices-documentnumber-pay)), droit `PAYMENT:PROCESS`. Corps `PayInvoiceRequest` : `paymentMethod` (obligatoire), `amount` (vide : tout le solde), `walletNumber` (pour `WALLET`), `cashSessionNumber` (déclaré obligatoire), `idempotencyKey` (à générer par intention, voir [idempotence](../README.md#idempotence)).
2. Refus `400` « La facture n'a pas de solde restant à payer » si `balanceDue` vaut 0.
3. Réponse `PayInvoiceResponse` : le document à jour et la transaction. Rafraîchir la fiche : statut `PARTIALLY_PAID` ou `PAID`, puis éventuellement `VALIDATED` par le scellement automatique.
4. Les autres modes de paiement (intention de paiement, mobile money, portail) sont décrits dans [payment](../payment/README.md).

### 9. Échéancier de paiement

1. Créer : `POST /sni/api/v1/billing/invoices/{documentNumber}/schedule` ([détail](routes/PaymentScheduleController.md#r-post-sni-api-v1-billing-invoices-documentnumber-schedule)) avec `installments: [{ "label": "...", "amount": 50000, "dueDate": "2026-12-01" }, ...]` et `notes`. Règles : la facture doit avoir un solde ; la somme des échéances ne doit pas dépasser `balanceDue` (`400`) ; un seul échéancier actif par facture (`409` sinon). Le libellé par défaut est « Acompte N ».
2. Lire : `GET /sni/api/v1/billing/invoices/{documentNumber}/schedule` (l'actif, sinon le dernier ; `404` « Aucun échéancier trouvé pour cette facture » s'il n'y en a pas : afficher le bouton de création) ou `GET /sni/api/v1/billing/schedules/{scheduleNumber}`.
3. Encaisser une échéance : `POST /sni/api/v1/billing/schedules/installments/{installmentNumber}/pay`, même corps que le paiement de facture. `amount` vide ou supérieur au reste de l'échéance : on paie le reste de l'échéance. Le paiement est imputé sur la facture.
4. Annuler : `DELETE /sni/api/v1/billing/schedules/{scheduleNumber}` (refusé si `COMPLETED`). Les échéances déjà payées restent payées.
5. Un paiement fait directement sur la facture (hors route d'échéance) **ne met pas à jour** les échéances.

### 10. Cycle commercial d'un devis

1. Créer le devis (parcours 2), éventuellement avec des lignes `optional: true` et un `advance`.
2. Émettre (`PATCH .../documents/{n}/issue`, le devis passe à `SENT`) puis `POST .../documents/{n}/send-email`.
3. Suivi : `PATCH /sni/api/v1/billing/quotes/{quoteNumber}/mark-viewed` (ouverture constatée), `PATCH .../quotes/{n}/start-negotiation`.
4. Le client choisit ses options : `PATCH /sni/api/v1/billing/quotes/{quoteNumber}/select-options` avec `selectedLineOrders: [3, 5]`. Les lignes optionnelles citées deviennent fermes ; **les lignes optionnelles non citées sont supprimées**. Les totaux sont recalculés. Refusé sur un devis `CONVERTED` ou `CANCELLED`.
5. Décision : `PATCH .../quotes/{n}/accept` ou `PATCH .../quotes/{n}/reject`, ou signature électronique (parcours 11).
6. Acompte facultatif : `PATCH .../quotes/{n}/request-deposit` (devis `ACCEPTED`), puis, une fois l'argent reçu, `PATCH .../quotes/{n}/mark-deposit-paid`. Ce dernier marque l'acompte payé et enregistre une transaction de paiement « virement, manuel » du montant de l'acompte.
7. Conversion : `POST /sni/api/v1/billing/quotes/{quoteNumber}/convert-to-invoice` ([détail](routes/BillingDocumentController.md#r-post-sni-api-v1-billing-quotes-quotenumber-convert-to-invoice)). Réponse : **la nouvelle facture** (pas le devis), en `DRAFT`, ou en `PARTIALLY_PAID`/`PAID` si un acompte payé est repris. Le devis passe à `CONVERTED`. La facture a `sourceType = "QUOTE"` et `sourceCode` = numéro du devis. Naviguer vers la facture.
8. Historique des propositions : `GET .../documents/{n}/versions` (liste sans contenu), `GET .../documents/{n}/versions/{versionNumber}` (avec `snapshot`), `GET .../documents/{n}/versions/{from}/diff/{to}` (champs changés, valeur avant et après). `sentToCustomerAt` distingue les versions réellement transmises.

### 11. Signature électronique d'un devis

1. Back-office : `POST /sni/api/v1/billing/quotes/{quoteNumber}/request-signature` ([détail](routes/BillingDocumentController.md#r-post-sni-api-v1-billing-quotes-quotenumber-request-signature)), corps facultatif `{ "signerName", "signerEmail", "customMessage" }` (à défaut : nom et courriel du devis). Refusé sur un devis `CANCELLED`, `CONVERTED`, `REJECTED`, ou sans courriel. Réponse : `BillingDocumentSignatureResponse` (`status: PENDING`, `expiresAt` à 7 jours). Un courriel part avec le lien.
2. Le lien du courriel pointe vers `GET /public/quotes/sign/{token}` ([détail](routes/QuoteSignaturePublicController.md#r-get-public-quotes-sign-token)) sur l'adresse de l'API (`app.api-base-url`). Cette route renvoie **une page HTML complète** rendue par le serveur (récapitulatif, zone de signature à dessiner), qui appelle elle-même la confirmation. Le frontend n'a rien à construire pour ce parcours.
3. Si un frontend veut sa propre page : `POST /public/quotes/sign/{token}/confirm` sans jeton d'authentification, avec `{ "signerName": "...", "signatureImageBase64": "data:image/png;base64,..." }` (les deux obligatoires). Le devis passe à `ACCEPTED`. Erreurs : `404` « Lien de signature invalide ou expiré », `400` « Ce devis a déjà été signé », `400` « Le lien de signature a expiré ».
4. Côté back-office, l'état se lit dans `signature` de la fiche du devis. Plusieurs demandes peuvent coexister ; la fiche montre la plus récente.

### 12. Annuler un document

1. `DELETE /sni/api/v1/billing/documents/{documentNumber}?reason=...` ([détail](routes/BillingDocumentController.md#r-delete-sni-api-v1-billing-documents-documentnumber)), droit `BILLING:CANCEL`. Pas de suppression physique : c'est une annulation avec archivage. Le frontend n'a pas à tester `locked` avant l'appel.
2. Le serveur choisit :
   - document non scellé, ou scellé sans aucun encaissement, devis, note de débit : passage direct à `CANCELLED`, `balanceDue` à 0, `cancelledAt` et `archivedAt` posés ;
   - facture ou proforma scellée avec `paidAmount > 0` : un avoir du solde restant (ou du total si tout est payé) est créé, scellé et imputé, puis la facture passe à `CANCELLED` ;
   - avoir déjà consommé : son imputation sur la facture est défaite d'abord ; s'il a été reversé au portefeuille, refus `400` (débiter d'abord le portefeuille).
3. Réponse : le document annulé. Recharger les listes : un avoir a pu apparaître.

### 13. Avoir et facture rectificative

1. Créer un avoir : `POST /sni/api/v1/billing/invoices/{invoiceNumber}/credit-note` ([détail](routes/BillingDocumentController.md#r-post-sni-api-v1-billing-invoices-invoicenumber-credit-note)), droit `BILLING:CANCEL`. Corps : `reason` (obligatoire), `amount` (vide : le solde de la facture ; doit être positif et au plus le total de la facture), `validateImmediately`, `applyImmediately`, `lines` (vide : une ligne d'ajustement non taxable générée). La facture d'origine doit être une facture ou une proforma non annulée. Réponse : l'avoir.
2. Sceller l'avoir : `PATCH .../documents/{n}/validate` (ou `validateImmediately: true`). Un avoir non scellé ne peut être ni imputé sur une autre facture ni reversé.
3. Utiliser l'avoir, une seule des trois voies :
   - `PATCH /sni/api/v1/billing/credit-notes/{creditNoteNumber}/apply` : imputer sur la facture d'origine ;
   - `PATCH /sni/api/v1/billing/credit-notes/{creditNoteNumber}/apply-to/{targetInvoiceNumber}` : imputer sur une autre facture ou proforma **du même client** ;
   - `POST /sni/api/v1/billing/credit-notes/{creditNoteNumber}/refund-to-wallet?reason=...` : créditer le portefeuille du client dans la devise de l'avoir (idempotent : un second appel ne recrédite pas).
   Dans les trois cas l'avoir passe à `ISSUED` (consommé).
4. Facture rectificative : `POST /sni/api/v1/billing/invoices/{invoiceNumber}/corrective` ([détail](routes/BillingDocumentController.md#r-post-sni-api-v1-billing-invoices-invoicenumber-corrective)), corps identique à une création manuelle (`currency` et `lines` obligatoires). La facture d'origine doit être scellée (sinon `400` qui invite à modifier le brouillon). Le client est repris de l'original si ni `customerCode` ni `customerName` ne sont fournis. Réponse `201` : la rectificative en `DRAFT`, à émettre et sceller comme une facture.

### 14. Factures générées depuis une autre source

- `POST /sni/api/v1/billing/invoices/from-billable-items` avec `billableNumbers` (éléments facturables du module payment, statut `PENDING` ou `SENT_TO_INVOICE`, même devise) : une ligne par élément, éléments passés à `INVOICED`, annexe de consommation jointe quand elle existe.
- `POST /sni/api/v1/billing/invoices/from-reservation` : `mode: "BOOKING"` avec `bookingNumber` (réservation `CONFIRMED`, `IN_PROGRESS` ou `COMPLETED`, pas encore facturée) ou `mode: "EXTERNAL"` avec `customerType`, `customerCode`, `externalBooking` (ressource, période, prix). `extraLines` ajoute des lignes ; `documentType` permet un devis ou une proforma (défaut : facture). La réservation reçoit le numéro du document.
- `POST /sni/api/v1/billing/documents/{documentNumber}/duplicate` : copie complète en nouveau `DRAFT` daté du jour (refus si le document n'a aucune ligne). La note interne est préfixée « [Copie de ...] ».

### 15. Relevé client et balance âgée

1. Relevé : `GET /sni/api/v1/billing/customers/{customerType}/{customerCode}/statement` (taille par défaut 50). `customerType` accepte aussi `MEMBRE`, `CLIENT`, `BUSINESS`, `COMPANY`, `ENTREPRISE`. Réponse : `totalInvoiced`, `totalPaid`, `totalBalanceDue`, `totalCreditAvailable`, `netBalanceDue` (jamais négatif), `totalDraft` (pour information) et `documents` (la page demandée, **sans métadonnées de pagination**).
2. Export : `GET .../statement.csv` (fichier CSV, 500 documents au plus par défaut, colonnes `documentNumber` à `currency` dont `receivable` et `customerImpact`).
3. Balance âgée : `GET /sni/api/v1/billing/aging-report?customerType=&customerCode=&currency=XAF`. Factures et proformas ouvertes avec un solde, réparties en `CURRENT`, `DAYS_1_TO_30`, `DAYS_31_TO_60`, `DAYS_61_TO_90`, `DAYS_OVER_90` (sur `dueDate`), avec un total général.

### 16. Objets à récupérer

1. Ajouter : `POST /sni/api/v1/billing/documents/{documentNumber}/recoverables` avec `items: [{ "itemDescription": "Badge", "quantity": 1, "unit": "u", "notes": "..." }]`. Réponse : la liste créée, statut `PENDING`.
2. Lister : `GET .../documents/{n}/recoverables?status=PENDING`.
3. Récupéré : `PATCH .../documents/{n}/recoverables/{recoverableNumber}/recover`, corps facultatif `{ "notes", "partialQuantity" }`. Abandonné : `PATCH .../recoverables/{recoverableNumber}/write-off`, corps facultatif `{ "notes" }`. Refus si l'objet n'appartient pas au document ou est déjà clos.

### 17. Catalogue de services ([routes/ServiceCatalogController.md](routes/ServiceCatalogController.md))

1. Liste : `GET /sni/api/v1/billing/catalog?active=&category=&q=` (page maison, tri fixe par ordre d'affichage).
2. Créer : `POST /sni/api/v1/billing/catalog` (`name` et `unitPrice` obligatoires ; `itemCode` généré). Champs de contrôle de remise : `costPrice`, `floorPrice`, `minMarginRate`, `maxDiscountRate`, `discountPolicy` (`NONE`, `WARN`, `BLOCK`). `billingMode` : `UNIT`, `HOURLY`, `DAILY`, `MONTHLY`, `FIXED`. Réponse avec `effectiveFloorPrice` (plancher saisi, ou déduit du coût et de la marge minimale) et son origine.
3. Modifier : `PUT /sni/api/v1/billing/catalog/{itemCode}`. Un champ absent est conservé ; pour vider un champ, le nommer dans `clearFields` (liste admise dans le message d'erreur, par exemple `floorPrice`, `maxDiscountRate`, `tags`).
4. `PATCH .../catalog/{itemCode}/activate`, `.../deactivate` ; `DELETE .../catalog/{itemCode}` supprime l'article (réponse `204`). Préférer la désactivation : les lignes de documents gardent le code.

### 18. Relances d'impayés ([routes/DunningController.md](routes/DunningController.md))

1. Politiques : `GET /sni/api/v1/billing/dunning/policies`, `GET .../policies/{policyCode}`.
2. Créer ou remplacer : `PUT /sni/api/v1/billing/dunning/policies` avec `policyCode`, `name`, `segment` (`DEFAULT`, `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY`), `tone` (`SOFT`, `STANDARD`, `FIRM`), `active`, `steps` (au moins un). Chaque palier : `daysAfterDue` (0 ou plus, dans l'ordre croissant, sinon `409`), `action` (`REMINDER`, `FORMAL_NOTICE`, `GRACE_PERIOD`, `SUSPEND`, `HANDOVER`), `channel` (`EMAIL`, `IN_APP`, `STAFF` ; défaut `EMAIL`), `subjectTemplate`, `messageTemplate`. Les paliers sont remplacés en bloc. Variables utilisables dans les textes : `{documentNumber}`, `{customerName}`, `{customerCode}`, `{balanceDue}`, `{totalAmount}`, `{currency}`, `{dueDate}`, `{daysOverdue}`, `{stepOrder}`, `{subscriptionNumber}`.
3. Désactiver : `POST .../policies/{policyCode}/deactivate`.
4. Journal : `GET .../dunning/notices` (200 dernières), `GET .../notices/documents/{documentNumber}`, `GET .../notices/customers/{customerType}/{customerCode}`. Afficher `outcome` et `detail` (raison d'un `SKIPPED` ou `FAILED`).
5. Lancer à la main : `POST /sni/api/v1/billing/dunning/run`. Réponse `{ documents, notices, failed }`.

### 19. Intégrité fiscale ([routes/FiscalIntegrityController.md](routes/FiscalIntegrityController.md))

1. `GET /sni/api/v1/admin/fiscal-integrity/last-report` : dernier rapport, ou `204` sans corps s'il n'y en a jamais eu.
2. `GET /sni/api/v1/admin/fiscal-integrity/check` : relance la vérification sur toutes les factures scellées (continuité de la numérotation par année, chaîne de hachage, signature) et enregistre le rapport. Réponse : `valid`, `checkedInvoices`, `brokenChains`, `missingSignatures`, `numberingGaps`, `checkedAt`. Afficher une alerte si `valid` est faux. Seules les factures (`INVOICE`) sont vérifiées.

## Règles et erreurs à refléter dans l'écran

Les erreurs métier sont des `400` (`BAD_REQUEST`) sauf indication. Messages exacts entre guillemets, en français ou en anglais selon le code.

**Création et calcul** (`src/main/java/com/sni/bokaticowork/features/billing/service/support/BillingCalculationService.java`)
- Au moins une ligne : « At least one billing line is required ».
- Quantité strictement positive (absente : 1) : « Line quantity must be positive ». Prix unitaire, remises et taux non négatifs : « Line unit price cannot be negative », « Line discount cannot be negative », « Percentage rate cannot be negative », « Discount value cannot be negative ».
- Remises de document supérieures au total : « Les remises demandées, X, dépassent le total du document, Y. Une remise ne peut pas excéder 100 % ... ».
- Total négatif : « Billing document total cannot be negative ».
- Client : type sans code ou code sans type refusé ; membre ou entreprise introuvable : `404` ; client supprimé : « Le client ... est supprimé et ne peut plus être facturé » ; sans client connu ni nom : « Customer name is required when customer cannot be resolved ».
- Organisation bloquée (facture ou proforma) : « L'organisation X est bloquée : motif. Aucune facture ne peut lui être émise tant qu'elle n'est pas débloquée. »
- Plafond d'encours dépassé (facture ou proforma, contrôlé à la **création**) : « Plafond d'encours dépassé pour X : encours ..., plafond .... Un super-administrateur peut émettre la facture malgré tout. » Seul `ROLE_SUPER_ADMIN` passe.
- Acompte : `advanceValue` positif ; montant calculé nul refusé : « Le montant calculé de l'acompte doit être positif ».

**Limites de remise** (`src/main/java/com/sni/bokaticowork/features/billing/service/support/BillingDiscountGuard.java`, valeurs par défaut de `application.yml`, section `bokati.billing.discount-guard`)
- Chaque ligne garde au moins 50 % de son brut (`BILLING_MIN_LINE_NET_RATE`), même sans article et même si l'article est en politique `NONE`.
- Remise totale (lignes et document) au plus 50 % du sous-total (`BILLING_MAX_DOCUMENT_DISCOUNT_RATE`). Montants plafonds optionnels.
- Article du catalogue en politique `WARN` ou `BLOCK` : prix unitaire net sous `effectiveFloorPrice`, ou taux effectif au-delà de `maxDiscountRate`.
- La simulation et le brouillon **constatent** (`violations`, `blocked`) ; seule l'émission **bloque** les violations `BLOCK`. Messages : « Remise hors limites : ... Un motif de dérogation est requis pour emettre ce document. » ou « ... La derogation exige la permission BILLING:DISCOUNT_OVERRIDE. ». Afficher la liste `violations[].message` dès la saisie.

**Cycle de vie**
- Émission hors `DRAFT` : « Only draft billing documents can be issued ».
- Modification d'un document scellé : « Document X est validé (SEFC) · il ne peut plus être modifié ». Autre statut que `DRAFT`, `ISSUED`, `SENT` : « Document in status S cannot be modified ».
- Validation : « Seules les factures, avoirs, rectificatives et notes de débit peuvent être validés », « Document déjà validé », « Le document doit être en statut DRAFT, ISSUED ou PAID pour être validé, statut actuel : S ».
- Transmission d'un document annulé : « Billing document cannot be sent in status CANCELLED ».
- Devis : « Billing document is not a quote », « Only draft, sent, viewed or negotiation quotes can be accepted », « Converted quote cannot be rejected », « Cannot start negotiation in status S », « Deposit can only be requested on an accepted quote », « Quote must be in DEPOSIT_REQUESTED status to mark deposit as paid », « Quote must be accepted or have its deposit paid before conversion », « Cannot modify options on a converted or cancelled quote ».
- Paiement sur autre chose qu'une facture ou proforma : « Payments can only be allocated to invoices » ; document annulé : « Billing document cannot be paid in status S ».
- Avoir : « Invalid credit note amount », « Credit note is not linked to an invoice », « L'avoir doit être validé (SEFC) avant d'être appliqué à une autre facture », « L'avoir X a déjà été consommé », « L'avoir et la facture cible doivent appartenir au même client », « La cible doit être une facture ou une facture proforma », « L'avoir X doit etre valide (SEFC) avant d'etre reverse ».
- Document introuvable : `404` « Billing document not found » (avec ou sans le numéro selon la route).
- Création depuis réservation : « bookingNumber is required for mode BOOKING », « Booking X is already billed: Y », « Booking status S is not billable », « externalBooking details are required for mode EXTERNAL », « customerType and customerCode are required for mode EXTERNAL ».
- Éléments facturables : `404` « Billable item not found: X », « Billable item is not invoiceable: X », « All billable items must use the same currency ».
- Échéancier : « Cette facture n'a pas de solde restant à échelonner », « Le total des acomptes (X) dépasse le solde restant de la facture (Y) », `409` « Un échéancier actif existe déjà pour cette facture », « Cet acompte ne peut plus être payé (statut: S) », « Un échéancier complété ne peut pas être annulé ».
- Catalogue : « La quantite minimale ne peut pas depasser la quantite maximale », « Le prix plancher (X) depasse le prix de vente (Y) · l'article serait invendable », « discountPolicy doit valoir NONE, WARN ou BLOCK », « billingMode doit valoir UNIT, HOURLY, DAILY, MONTHLY ou FIXED », « Champ non effacable : X. Champs admis : ... ».
- Relances : « Une politique de relance a au moins un palier », « Chaque palier a un nombre de jours et une action », `409` « les paliers se donnent dans l'ordre des jours », `404` « Politique de relance introuvable ».
- Plusieurs messages d'erreur sont en anglais ou sans accents : les afficher tels quels ou les mapper côté frontend.

**Dates** : `issueDate`, `dueDate`, `dueDate` d'échéance, `ifPaidBefore` sont des dates `AAAA-MM-JJ`. Aucune règle serveur ne compare `dueDate` à `issueDate` : contrôler côté écran.

## À prévoir côté frontend

### Écrans

1. Liste des documents (filtres type, statut, client, période, recherche), avec onglets devis / factures / avoirs.
2. Éditeur de document (création et modification d'un brouillon) avec simulation en direct et affichage des dépassements de remise.
3. Fiche document avec actions, historique des versions et comparaison, PDF, échéancier, objets à récupérer, journal des relances (`/dunning/notices/documents/{n}`).
4. Dialogues : motif de dérogation de remise, motif d'annulation, création d'avoir, imputation ou reversement d'avoir, paiement, demande de signature.
5. Relevé client et export CSV ; balance âgée.
6. Catalogue de services.
7. Paramétrage des relances et journal.
8. Tableau d'intégrité fiscale (réservé aux administrateurs).

### Droits pour masquer les boutons

| Action | Droit (règle d'URL) | Contrôle supplémentaire |
| --- | --- | --- |
| Lire documents, PDF, versions, relevé, balance âgée, catalogue, échéancier, objets à récupérer | `BILLING:READ` | |
| Créer un document, simuler, dupliquer, convertir un devis, demander une signature, rectificative, échéancier, objets à récupérer, article du catalogue | `BILLING:CREATE` | |
| Modifier, corriger le destinataire, émettre, sceller, transmettre (`send`), actions de devis, marquer récupéré | `BILLING:UPDATE` | |
| Dérogation de remise à l'émission | `BILLING:UPDATE` | `BILLING:DISCOUNT_OVERRIDE` (ou `ROLE_SUPER_ADMIN`), vérifié dans le service |
| Envoyer le courriel | `BILLING:SEND` | |
| Annuler (`DELETE`), créer, imputer ou reverser un avoir, annuler un échéancier, supprimer un article | `BILLING:CANCEL` | |
| Encaisser une facture ou une échéance | `PAYMENT:PROCESS` | |
| Dépasser le plafond d'encours | | `ROLE_SUPER_ADMIN` uniquement |
| Lire relances | `BILLING:READ` | rôle `ADMIN`, `SUPER_ADMIN` ou `STAFF` |
| Écrire une politique de relance, lancer les relances | `BILLING:UPDATE` (écriture) ou `BILLING:CREATE` (désactiver, lancer) | rôle `ADMIN` ou `SUPER_ADMIN` |
| Intégrité fiscale | `SYSTEM:SETTINGS` | rôle `ADMIN` ou `SUPER_ADMIN` |

Les contrôles `hasRole(...)` s'appliquent à tout le monde : un compte qui a le droit mais pas le rôle reçoit `403`.

### Actions par état (pour afficher les bons boutons)

| Document | État | Actions utiles |
| --- | --- | --- |
| Tout type | `DRAFT` | Modifier, Simuler, Émettre, Dupliquer, Annuler, PDF |
| Facture, proforma, rectificative, note de débit | `DRAFT`, `ISSUED`, `PAID` et `locked = false` | Sceller |
| Facture ou proforma | `receivable = true` | Encaisser, Échéancier, Envoyer le courriel, Créer un avoir |
| Facture ou proforma | `locked = true` | Facture rectificative, Avoir, Corriger le contact uniquement |
| Devis | `SENT`, `VIEWED`, `NEGOTIATION` | Marquer vu (`SENT`), Négocier, Accepter, Refuser, Demander une signature, Choisir les options |
| Devis | `ACCEPTED` | Demander l'acompte, Convertir, Négocier, Refuser |
| Devis | `DEPOSIT_REQUESTED` / `DEPOSIT_PAID` | Marquer l'acompte payé / Convertir |
| Avoir | `DRAFT` | Sceller, Annuler |
| Avoir | `VALIDATED` | Imputer sur la facture d'origine, Imputer sur une autre facture, Reverser au portefeuille |
| Tout document | sauf `CANCELLED` | Annuler, Corriger le destinataire |

### Listes de référence

- Énumérations des modèles (types, statuts, `BillingLineType`, `BillingDiscountType`, `BillingAdvanceType`) : dans [routes/BillingDocumentController.md](routes/BillingDocumentController.md), section Modèles.
- Clients : modules [client](../client/README.md) et [company](../company/README.md) pour choisir `customerType` + `customerCode`.
- Articles : `GET /sni/api/v1/billing/catalog/lookup`.
- Sessions de caisse (`cashSessionNumber`) et portefeuilles (`walletNumber`) : module [payment](../payment/README.md).

### Fichiers

- Téléchargements : PDF (`/documents/{n}/pdf`, deux exemplaires) et CSV (`/statement.csv`), tous deux avec le jeton : utiliser `fetch` puis un `Blob`, pas un simple lien.
- Aucun téléversement dans ce module.

### Champs à ne pas envoyer et pièges

- Ne jamais renvoyer `BillingDocumentResponse` tel quel dans un `PUT` : JSON strict, seuls les champs de `UpdateBillingDocumentRequest` sont acceptés. Pour les lignes, envoyer des opérations `UPSERT` / `REMOVE`.
- `simulate` est la seule route qui tolère des champs en trop.
- `discountOverrideReason` (émission) et `reason` (annulation, reversement d'avoir) sont des **paramètres de requête**, pas des corps JSON.
- `billingAddressJson`, `deliveryAddressJson`, `bankDetailsJson`, `metadataJson` sont des **chaînes contenant du JSON** : faire `JSON.stringify` à l'envoi et `JSON.parse` à la lecture.
- `changedBy` du corps de correction du destinataire est recopié tel quel dans l'historique : envoyer l'identité réelle de l'utilisateur connecté ou rien.
- Montants : décimaux à 4 décimales en réponse (`12500.0000`). Les arrondir à l'affichage selon la devise (XAF sans décimales).
- Le sens « payé » se lit sur `balanceDue` et `paidAmount` (voir [Statuts](#statuts-et-cycle-de-vie)).
- Une facture convertie depuis un devis remplace l'écran : `convert-to-invoice` renvoie la facture, pas le devis.
- Chaque création consomme un numéro de séquence : pour les essais de calcul, utiliser `simulate`.
- Pas de temps réel propre au module. L'alerte « factures en retard » arrive sur `/topic/admin/alerts` (voir [Effets automatiques](#effets-automatiques)).

## Effets automatiques

| Déclencheur | Fréquence ou moment | Effet visible |
| --- | --- | --- |
| `BillingOverdueWorker` | toutes les heures (`bokati.billing.workers.overdue-delay-ms`, défaut 3 600 000 ms) | Factures et proformas `ISSUED`, `VALIDATED`, `SENT`, `VIEWED`, `PARTIALLY_PAID` échues avec un solde passent à `OVERDUE`. Si au moins une : alerte WebSocket `BILLING_OVERDUE` sur `/topic/admin/alerts` (« N facture(s) en retard »). |
| `PaymentScheduleOverdueWorker` | toutes les heures | Échéances `PENDING` dont la date est passée deviennent `OVERDUE`. |
| `DunningPolicyWorker` | chaque jour à 08:30 (`bokati.billing.dunning.cron`) ; ou `POST /dunning/run` | Seulement s'il existe une politique active. Pour chaque facture échue avec un solde (200 par passage), exécute une fois chaque palier atteint : rappel ou mise en demeure par courriel (via la file d'événements) et notification dans le portail pour un membre, mise en tolérance ou suspension de l'abonnement lié, transmission à l'équipe (`bokati.billing.dunning.handover-email`). L'organisation payeuse reçoit aussi la relance. Trace dans `/dunning/notices`. |
| `BillingPaymentReminderWorker` | chaque jour à 08:00 | **Seulement si aucune politique de relance n'est active** : renvoie le courriel de la facture `OVERDUE` (3 fois au plus, 7 jours d'écart). |
| `FiscalIntegrityWorker` | chaque jour à 02:00 | Vérification d'intégrité fiscale, rapport lisible par `/admin/fiscal-integrity/last-report`. |
| `BillingPeriodClosureWorker` | chaque jour à 23:30, le 1er du mois à 00:15, le 1er janvier à 01:00 | Clôtures journalière, mensuelle et annuelle des montants. Aucune route ne les expose. |
| Paiement réussi d'une intention liée à un ou plusieurs documents | à la réussite | Le document reçoit le paiement ; s'il est soldé et non scellé, il est **scellé automatiquement** (`VALIDATED`) ; le document à jour est renvoyé par courriel ; un courriel de confirmation de paiement part aussi. (`src/main/java/com/sni/bokaticowork/features/payment/service/support/PaymentTransactionWorkflowProcessor.java:113`) |
| Paiement réussi non lié à un document (sauf rechargement de portefeuille) | à la réussite | Une facture est créée et émise automatiquement (source `PAYMENT_INTENT`). |
| Émission d'un document | `PATCH .../issue` | Notification `BILLING_DOCUMENT_ISSUED` dans le portail du membre (titre « Facture emise ... », même pour un devis). |
| Demande de signature | `POST .../request-signature` | Courriel au signataire avec le lien de 7 jours. |
| Toute action sur un document | à chaque action | Événement dans la file sortante (`BILLING_DOCUMENT_CREATED`, `_UPDATED`, `_ISSUED`, `_VALIDATED`, `_SENT`, `_CANCELLED`, `_ARCHIVED`, `_DUPLICATED`, `_RECIPIENT_UPDATED`, `QUOTE_*`, `CREDIT_NOTE_*`), lu par les autres modules. Rien à écouter côté frontend. |
| Autres modules | à leurs événements | booking, marketplace, subscription et payment créent, émettent, scellent ou annulent des documents (réservation payée, commande, renouvellement, remboursement). Une liste peut donc changer sans action de l'utilisateur. |

## Écarts relevés

1. **Corrigé le 2026-11-08** : la mise à jour d'un brouillon réécrit taxes, remises et clauses sans dupliquer les lignes. Constat d'origine : **`PUT` avec `discounts` ou `clauses` duplique les lignes** : `src/main/java/com/sni/bokaticowork/features/billing/service/implementation/BillingDocumentServiceImpl.java:389-395` rappelle `documentWriter.save(...)`, qui réenregistre toutes les lignes recalculées comme de nouvelles lignes (`src/main/java/com/sni/bokaticowork/features/billing/service/support/BillingDocumentWriter.java:47-50`) sans supprimer les anciennes, et sans contrainte d'unicité sur `(document_id, line_order)`. Avec `discounts`, les taxes sont aussi réenregistrées sans être supprimées. Constat à la lecture, non reproduit. En attendant : modifier lignes, remises et clauses par des appels séparés et vérifier la fiche après coup.
2. **Corrigé le 2026-11-08** : sans `discounts`, le recalcul reprend les remises enregistrées (l'exonération de TVA de l'organisation n'est toujours appliquée qu'à la création). Constat d'origine : **`PUT` avec `lines` sans `discounts` fait disparaître les remises de document des totaux** : le recalcul utilise une liste vide (`BillingDocumentServiceImpl.java:375-378`) alors que les remises restent enregistrées (`:363-365`). Le recalcul n'applique pas non plus l'exonération de TVA de l'organisation (faite seulement à la création, `:286-288`).
3. **Corrigé le 2026-11-08** : `PATCH /credit-notes/{n}/apply` répond `409` pour un avoir non validé ou déjà consommé, sous verrou contre deux imputations simultanées. Constat d'origine : **`PATCH /credit-notes/{n}/apply` ne vérifie ni le scellement ni la consommation** (`BillingDocumentServiceImpl.java:1447-1457`), à la différence de `apply-to` (`:1812-1817`) : un avoir en brouillon peut être imputé, et un avoir déjà consommé peut l'être une seconde fois.
4. **La confirmation publique de signature renvoie tout le document** (`src/main/java/com/sni/bokaticowork/features/billing/controller/QuoteSignaturePublicController.java:44-56`) : `BillingDocumentResponse` complet, notes internes (`internalNotes`) et objets à récupérer compris, à quiconque détient le jeton. Elle accepte aussi un devis devenu `CANCELLED`, `CONVERTED` ou `REJECTED` après la demande et le repasse à `ACCEPTED` (`src/main/java/com/sni/bokaticowork/features/billing/service/implementation/QuoteSignatureServiceImpl.java:104-121`).
5. **Annuler une rectificative scellée et payée échoue** : `requiresCreditNoteToCancel` inclut `CORRECTIVE_INVOICE` (`BillingDocumentServiceImpl.java:1707-1710`) mais `createCreditNote` exige une facture ou une proforma (`:1330`, `BillingLifecycleSupport.java:27-29`) : `DELETE` répond `400` « Payments can only be allocated to invoices ».
6. **`PATCH .../send` écrase le statut** : il passe à `SENT` depuis `DRAFT`, `PAID`, `VALIDATED`, `OVERDUE`... (`src/main/java/com/sni/bokaticowork/features/billing/service/support/BillingLifecycleSupport.java:18-24`, `BillingDocumentServiceImpl.java:1043`). Une facture payée « transmise » n'affiche plus `PAID`, et un brouillon passe à `SENT` sans l'émission ni son contrôle de remise.
7. **`validate` depuis `DRAFT` contourne l'émission** (`BillingLifecycleSupport.java:66-70`) : pas de contrôle des limites de remise ni de notification au membre.
8. **`PATCH .../issue` sur un avoir le marque consommé** : `ISSUED` signifie « consommé » pour un avoir (`BillingReceivables.java:150-152`), alors que l'émission n'impute rien.
9. **Un devis en `NEGOTIATION`, `VIEWED` ou `ACCEPTED` ne se modifie pas** (`BillingDocumentServiceImpl.java:317-323`) : seuls `DRAFT`, `ISSUED`, `SENT` sont admis. Réviser une offre en négociation oblige à dupliquer.
10. **Statuts jamais atteints** : `EXPIRED`, `VOIDED`, `REFUNDED`, `WRITTEN_OFF` (`src/main/java/com/sni/bokaticowork/features/billing/enums/BillingDocumentStatus.java`) ne sont posés par aucun code ; aucun devis n'expire. `BillingAdvanceStatus.CANCELLED` n'est jamais posé.
11. **Balance âgée** : le paramètre `currency` n'est que recopié dans la réponse, les documents ne sont pas filtrés par devise et les montants de devises différentes s'additionnent (`src/main/java/com/sni/bokaticowork/features/billing/service/implementation/BillingRecoverableServiceImpl.java:100-155`).
12. **Échéancier** : aucune vérification du type ni du statut du document (`src/main/java/com/sni/bokaticowork/features/billing/service/implementation/PaymentScheduleServiceImpl.java:45-60`) ; un paiement direct sur la facture ne met pas à jour les échéances.
13. **Relevé client** : la réponse ne porte pas de métadonnées de pagination alors que la route est paginée, et l'export CSV ne contient que la première page (500 par défaut) (`src/main/java/com/sni/bokaticowork/features/billing/controller/BillingDocumentController.java:330-348`). Le tri demandé est ignoré sur la liste des documents et le relevé (tri fixe `createdAt` décroissant).
14. **Champs jamais remplis ou peu fiables** : `recoveredBy` d'un objet récupéré n'est jamais renseigné (`BillingRecoverableServiceImpl.java:70-83`) ; `changedBy` de la correction de destinataire vient du corps de la requête (`BillingDocumentServiceImpl.java:450`).
15. **PDF figé** : après scellement, le PDF est figé à sa première génération (`src/main/java/com/sni/bokaticowork/features/billing/service/implementation/BillingDocumentPdfServiceImpl.java:97-109`) ; une correction ultérieure du contact par `PATCH .../recipient` n'y apparaît pas.
16. **Réglé (avant les lots finance, c6089dc)** : `cashSessionNumber` n'est plus `@NotNull` ; il n'est exigé qu'en espèces (`400` « La session de caisse est obligatoire pour un paiement en espèces »). Constat d'origine : **`PayInvoiceRequest.cashSessionNumber` est obligatoire** (`src/main/java/com/sni/bokaticowork/features/payment/dto/request/PayInvoiceRequest.java:13-14`) pour tous les modes de paiement, y compris portefeuille ou virement : un paiement sans session de caisse est refusé en `400`.
17. **La simulation ignore les conditions du client** : pas d'exonération de TVA de l'organisation ni de plafond d'encours (`BillingDocumentServiceImpl.java:154-191`) ; le total simulé peut différer du total créé pour une organisation exonérée.
18. **Doublons de routes** : `/quotations/...` répète `/quotes/...` (`BillingDocumentController.java:100-103`, `:239-262`).
19. **Messages mêlant anglais et français, souvent sans accents** (exemples : « Only draft billing documents can be issued », « L'avoir ... doit etre valide (SEFC) avant d'etre reverse »).
