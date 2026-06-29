# Guide frontend consolide - changements recents

Ce document regroupe les changements backend ajoutes ou ajustes sur les modules existants. Il sert de base d'integration frontend pour les ecrans back-office et client.

Base API: `/sni/api/v1`

Regle generale: le frontend consomme les DTO exposes par les controllers et ne reconstruit pas les objets metier internes. Les champs calcules par les mappers (`ownerName`, `resourceTypeCode`, `resourceGroupCode`, `entitlementCode`, `qrValue`, etc.) doivent etre utilises directement dans l'UI.

## Priorites frontend

1. Reservation avec subscription/pass: verifier le parcours de creation, les erreurs et l'affichage des entitlements.
2. Subscription + paiement: afficher activation, contrats generes, factures et liens de paiement.
3. KYC: afficher le nom du proprietaire, la file de revue, OCR, SLA, expirations et actions bulk.
4. Contracts/signature: exposer les contrats generes automatiquement et la signature simple in-app.
5. Inventory: integrer dashboard, mouvements, alertes, etiquettes QR/barcode et rapports.
6. Analytics/reporting: brancher les dashboards operationnels.

## Regles transverses

- Les actions sensibles doivent utiliser les endpoints idempotents existants quand ils sont exposes. Eviter les doubles clics cote UI.
- Tout ce qui est automatisable est gere backend: activation subscription apres paiement, generation de contrat, OCR, rappels KYC, waitlist, expiration d'offres, notifications et workers.
- Le frontend doit afficher les statuts et proposer des actions de relecture ou relance, pas refaire la logique worker.
- Pour les parcours reservation, ne pas envoyer `ownerCode`, `subscriptionNumber`, `passNumber` ou `entitlementCode` dans les payloads de creation. Envoyer un selecteur d'identite (`memberId`, `customerId`, `businessCode`, `email`, `phone`, `walkIn`) et `paymentMode`.
- Les messages d'erreur doivent distinguer disponibilite ressource, absence de subscription/pass, entitlement incompatible et politique de reservation.

## 1. Booking / reservation

### Creation de reservation

Le backend resout le proprietaire, la subscription active, le pass utilisable et l'entitlement compatible avec la ressource.

Champs importants retournes par `BookingResponse`:

```json
{
  "bookingNumber": "BKG-MEE-202604-000001",
  "resourceCode": "RES-MEETING-01",
  "resourceName": "Salle Reunion A",
  "resourceTypeCode": "MEETING_ROOM",
  "resourceGroupCode": "GRP-PREMIUM",
  "ownerType": "MEMBER",
  "ownerCode": "MBR-000001",
  "paymentMode": "SUBSCRIPTION",
  "subscriptionNumber": "SUB-MEM-202604-000001",
  "passNumber": null,
  "entitlementCode": "ENT-TIME-MEETING",
  "checkInToken": "BKG-CHECKIN-...",
  "checkInQrValue": "bokati:booking:checkin:...",
  "virtualMeetingUrl": null,
  "lines": []
}
```

Impact UI:

- Afficher `resourceTypeCode` et `resourceGroupCode` dans le detail reservation pour faciliter le diagnostic.
- Afficher `subscriptionNumber`, `passNumber` et `entitlementCode` seulement en lecture.
- Afficher `checkInQrValue` sous forme QR code sur les reservations confirmees.
- Afficher `virtualMeetingUrl` si la ressource est virtuelle.

### Reservation avec subscription

Points bloquants a afficher clairement:

- aucun proprietaire resolu;
- aucune subscription active pour le proprietaire;
- subscription active mais aucun grant compatible;
- entitlement compatible mais solde insuffisant;
- resource type/group de la ressource non couvert par l'entitlement;
- creneau indisponible ou hors horaires.

Correction backend importante: si une subscription est active mais que les grants ne sont pas encore materialises, le backend tente de generer/recuperer les grants immediatement avant de refuser la reservation. Le frontend doit donc relire la reponse backend et ne pas supposer qu'une subscription active suffit.

### Recurrence avancee

Les reservations recurrentes supportent:

- `intervalValue`;
- `endDate`;
- `excludedDates`;
- `recurrenceGroupNumber` en lecture.

UI recommandee:

- un panneau recurrence avec frequence, intervalle, date de fin et dates exclues;
- une previsualisation des occurrences avant confirmation;
- un lien vers toutes les reservations du meme `recurrenceGroupNumber`.

## 2. Waitlist et alternatives

Endpoints:

| Methode | Endpoint | Usage |
| --- | --- | --- |
| `POST` | `/bookings/waitlist` | Entrer en liste d'attente |
| `GET` | `/bookings/waitlist?status=WAITING` | Lister les entrees |
| `PATCH` | `/bookings/waitlist/{id}/cancel` | Annuler une entree |
| `POST` | `/bookings/waitlist/promote-available?limit=100` | Admin: proposer les creneaux disponibles |
| `POST` | `/bookings/waitlist/expire-offers?limit=100` | Admin: expirer les offres echues |

Payload:

```json
{
  "resourceCode": "RES-MEETING-01",
  "identityLookup": {
    "memberId": 12
  },
  "startedAt": "2026-05-10T09:00:00",
  "endedAt": "2026-05-10T10:00:00",
  "quantity": 1,
  "paymentMode": "SUBSCRIPTION"
}
```

Reponse:

```json
{
  "id": 42,
  "resourceCode": "RES-MEETING-01",
  "resourceName": "Salle Reunion A",
  "ownerType": "MEMBER",
  "ownerCode": "MBR-000001",
  "contactName": "Jean Dupont",
  "startedAt": "2026-05-10T09:00:00",
  "endedAt": "2026-05-10T10:00:00",
  "paymentMode": "SUBSCRIPTION",
  "status": "WAITING",
  "offeredAt": null,
  "expiresAt": null,
  "alternatives": []
}
```

Relation avec le service de creneaux alternatifs:

- le service d'alternatives propose des creneaux proches;
- la waitlist memorise une demande non satisfaite et peut offrir plus tard un creneau libere;
- l'UI doit afficher les alternatives immediates sans annuler l'entree waitlist;
- si l'utilisateur reserve une alternative, proposer d'annuler l'entree waitlist pour eviter une double intention.

## 3. Subscription, pass et entitlements

Le module subscription expose maintenant une logique plus complete autour des plans, subscriptions, pass, addons, promotions, seats, usage, rollover et timeline.

Elements frontend a integrer:

- liste/detail des plans avec entitlements inclus;
- detail subscription avec statut, dates, facturation, entitlements et contrat lie;
- actions pause/resume si exposees sur l'ecran back-office;
- affichage `requiredKycLevel` quand un plan exige un niveau KYC;
- soldes entitlement par proprietaire;
- timeline des evenements subscription.

Regles UI:

- ne pas permettre une reservation par subscription si la subscription est suspendue ou expiree;
- afficher les soldes par `entitlementCode`, unite et ressource couverte;
- afficher le fallback paiement direct si l'entitlement est insuffisant et que la politique le permet;
- afficher le contrat associe une fois genere.

## 4. Paiement, billing et activation automatique

Changements a integrer:

- creation de payment intent pour facture ou groupe de factures;
- lien de paiement public par token;
- paiement Mobile Money/PawaPay avec callback et polling;
- receipts JSON/PDF;
- cash metrics;
- activation automatique de subscription apres paiement complet;
- generation automatique de contrat apres subscription/pass/addon quand applicable.

Parcours frontend recommande:

1. L'utilisateur choisit un plan ou une facture a payer.
2. Le frontend cree/recupere une intention de paiement.
3. Le frontend redirige ou affiche le lien public.
4. Le frontend poll le statut ou attend le retour callback.
5. Quand le paiement est complet, relire la subscription et la liste des contrats.

Point backend corrige: l'event de paiement doit declencher le workflow sans empiler un `@Transactional` sur un `@TransactionalEventListener`. Pour le frontend, cela signifie que la subscription et le contrat peuvent apparaitre apres le traitement asynchrone; prevoir un etat "traitement en cours" et un bouton de rafraichissement.

## 5. Contrats, documents et signature

Endpoints principaux:

| Methode | Endpoint | Usage |
| --- | --- | --- |
| `GET` | `/contracts/templates` | Lister les templates |
| `POST` | `/contracts` | Creer un contrat |
| `GET` | `/contracts/{contractCode}` | Detail contrat |
| `GET` | `/contracts?ownerType=MEMBER&ownerCode=...` | Contrats d'un proprietaire |
| `POST` | `/contracts/generate` | Generer un brouillon document |
| `POST` | `/contracts/{contractCode}/generate-draft` | Generer depuis un contrat existant |
| `PATCH` | `/contracts/{contractCode}/signed-document` | Marquer le document signe |

Impact UI:

- ajouter un onglet `Contrats` sur les fiches member/customer/business/subscription;
- afficher le statut contrat, document draft, document signe et dates;
- proposer la signature simple in-app quand un contrat attend signature;
- apres signature, relire le contrat pour afficher le document signe et le statut final.

Generation automatique:

- apres prise de subscription, le contrat doit etre cree par le workflow backend;
- le frontend ne doit pas creer manuellement un contrat pour compenser un delai worker;
- afficher "Contrat en generation" tant que le contrat n'est pas encore visible.

## 6. KYC et OCR

Le KYC est compose de trois couches frontend:

- configuration documentaire: types de documents et requirements;
- gestion documentaire: upload, versioning, preview/download, revue document;
- dossier KYC: workflow de compliance, assignment, SLA, OCR, bulk actions, export.

### Configuration documentaire

Endpoints `document-types`:

| Methode | Endpoint | Usage |
| --- | --- | --- |
| `POST` | `/document-types` | Creer un type de document |
| `PATCH` | `/document-types/{code}` | Modifier un type |
| `GET` | `/document-types/{code}` | Detail type |
| `GET` | `/document-types?ownerType=MEMBER&active=true` | Lister/filtrer |
| `PATCH` | `/document-types/{code}/activate` | Activer |
| `PATCH` | `/document-types/{code}/deactivate` | Desactiver |
| `DELETE` | `/document-types/{code}` | Supprimer |

Endpoints `document-requirements`:

| Methode | Endpoint | Usage |
| --- | --- | --- |
| `POST` | `/document-requirements` | Creer une exigence documentaire |
| `PATCH` | `/document-requirements/{id}` | Modifier une exigence |
| `GET` | `/document-requirements/{id}` | Detail exigence |
| `GET` | `/document-requirements?ownerType=BUSINESS&active=true` | Lister/filtrer |
| `PATCH` | `/document-requirements/{id}/activate` | Activer |
| `PATCH` | `/document-requirements/{id}/deactivate` | Desactiver |
| `DELETE` | `/document-requirements/{id}` | Supprimer |

UI configuration:

- ecran admin `DocumentTypeAdminPage`;
- ecran admin `DocumentRequirementAdminPage`;
- filtres `ownerType`, `active`;
- badge obligatoire/optionnel si le DTO l'expose;
- ne pas autoriser la suppression sans confirmation.

### Documents KYC et centre documentaire

Endpoints `documents`:

| Methode | Endpoint | Usage |
| --- | --- | --- |
| `POST` | `/documents/upload` | Upload multipart d'un document |
| `POST` | `/documents/{code}/replace?uploadedBy=...` | Remplacer le fichier, cree une version |
| `GET` | `/documents/{code}` | Detail document |
| `GET` | `/documents?ownerType=MEMBER&ownerCode=...` | Documents d'un proprietaire |
| `GET` | `/documents/{code}/versions` | Historique versions |
| `GET` | `/documents/{code}/download` | Telechargement |
| `GET` | `/documents/{code}/preview` | Preview inline |
| `POST` | `/documents/{code}/approve` | Approuver un document |
| `POST` | `/documents/{code}/reject` | Rejeter un document |

Upload multipart:

```http
POST /sni/api/v1/documents/upload
Content-Type: multipart/form-data
```

Champs attendus cote formulaire:

- metadonnees du document via `DocumentUploadMetadataRequest`;
- fichier dans la part `file`;
- `ownerType` et `ownerCode` doivent venir du contexte metier, pas d'une saisie libre en portail client.

UI centre documentaire:

- liste par proprietaire;
- preview fichier dans un panneau lateral;
- onglet versions;
- actions `Remplacer`, `Telecharger`, `Approuver`, `Rejeter`;
- statut visible par document;
- bloc OCR quand disponible.

### Workflow KYC

| Methode | Endpoint | Usage |
| --- | --- | --- |
| `POST` | `/kyc/cases` | Creer un dossier KYC |
| `GET` | `/kyc/cases/{code}` | Detail dossier |
| `GET` | `/kyc/cases` | Recherche avec filtres |
| `GET` | `/kyc/cases/dashboard` | Dashboard compliance |
| `GET` | `/kyc/cases/expiring-soon?days=30` | Documents proches expiration |
| `GET` | `/kyc/cases/my-queue?userId=...` | File agent |
| `POST` | `/kyc/cases/{code}/submit` | Soumettre |
| `POST` | `/kyc/cases/{code}/approve` | Approuver |
| `POST` | `/kyc/cases/{code}/reject` | Rejeter |
| `GET` | `/kyc/cases/{code}/missing-requirements` | Pieces manquantes |
| `PATCH` | `/kyc/cases/{code}/assign` | Assigner |
| `POST` | `/kyc/cases/{code}/notes` | Ajouter une note |
| `GET` | `/kyc/cases/{code}/notes` | Lister les notes |
| `GET` | `/kyc/cases/{code}/timeline` | Timeline |
| `GET` | `/kyc/cases/{code}/expiry-status` | Etat expiration documents |
| `PATCH` | `/kyc/cases/{code}/risk-level` | Modifier risque |
| `GET` | `/kyc/cases/{code}/export/pdf` | Export PDF |
| `POST` | `/kyc/documents/bulk-approve` | Approbation bulk |
| `POST` | `/kyc/documents/bulk-reject` | Rejet bulk |
| `GET` | `/kyc/documents/{documentCode}/ocr-result` | Resultat OCR |

Filtres de recherche KYC:

- `status`;
- `ownerType`;
- `submittedAfter`;
- `submittedBefore`;
- `reviewedBy`;
- `pendingReviewOnly`;
- `expiringWithinDays`;
- `riskLevel`.

`KycCaseResponse` contient maintenant le nom du proprietaire:

```json
{
  "code": "KYC-202604-000001",
  "ownerName": "Jean Dupont",
  "ownerCode": "MBR-000001",
  "ownerType": "MEMBER",
  "status": "SUBMITTED",
  "assignedTo": 7,
  "slaDeadline": "2026-05-03T12:00:00Z",
  "riskLevel": "LOW",
  "kycLevel": 1,
  "complete": true,
  "approved": false,
  "requirements": [],
  "documents": []
}
```

Champs KYC a afficher:

- `ownerName`: nom du proprietaire a afficher partout;
- `ownerCode`, `ownerType`, `ownerId`: identifiants techniques;
- `status`: etat du dossier;
- `startedAt`, `submittedAt`, `completedAt`;
- `reviewedBy`, `reviewedAt`, `decisionComment`;
- `assignedTo`, `assignedAt`;
- `slaDeadline`, `lastReminderSentAt`, `reminderCount`;
- `riskLevel`, `kycLevel`;
- `complete`, `approved`;
- `missingDocumentTypeCodes`;
- `requirements`;
- `documents`.

Fonctionnalites KYC a couvrir cote frontend:

- remplacer les affichages `ownerCode` seuls par `ownerName` + `ownerCode`;
- creer et consulter un dossier KYC;
- soumettre un dossier complet;
- afficher les pieces attendues et les pieces manquantes;
- uploader/remplacer des documents;
- previsualiser et telecharger les documents;
- approuver/rejeter un document;
- approuver/rejeter le dossier KYC;
- assigner un agent de revue;
- afficher la file `my-queue`;
- ajouter et consulter les notes internes;
- afficher la timeline;
- afficher les expirations documentaires;
- modifier le niveau de risque;
- afficher le niveau KYC;
- exporter le dossier en PDF;
- bulk approve/reject sur documents selectionnes;
- afficher le dashboard compliance;
- afficher les dossiers avec documents bientot expires;
- afficher les rappels envoyes et le compteur.

### OCR

Fonctionnement:

- fournisseur retenu pour commencer: Tesseract;
- execution backend locale, gratuite, sans appel cloud;
- le frontend n'upload pas vers Tesseract directement;
- le frontend consulte `/kyc/documents/{documentCode}/ocr-result`.

Fonctionnalites OCR UI:

- panneau OCR sur le detail document;
- affichage texte brut detecte;
- affichage champs detectes si le DTO les expose;
- score/confiance si expose;
- comparaison OCR vs valeurs saisies;
- statut d'erreur visible sans bloquer toute la revue KYC.

Etats OCR recommandes cote UI:

- `PENDING`: document charge, OCR pas encore disponible;
- `PROCESSED`: texte/champs detectes disponibles;
- `FAILED`: proposer relance admin si endpoint expose plus tard, sinon signaler au support;
- `NOT_ENABLED`: masquer les panneaux OCR.

### Ecrans KYC recommandes

- `KycDashboardPage`;
- `KycCaseListPage`;
- `KycCaseDetailPage`;
- `KycReviewQueuePage`;
- `KycExpiringDocumentsPage`;
- `KycDocumentPreviewPanel`;
- `KycOcrResultPanel`;
- `KycTimelinePanel`;
- `KycNotesPanel`;
- `KycRequirementChecklist`;
- `DocumentTypeAdminPage`;
- `DocumentRequirementAdminPage`;
- `OwnerDocumentCenterPage`.

### Parcours KYC standard

1. Charger les requirements actifs par `ownerType`.
2. Creer ou recuperer le dossier KYC.
3. Uploader les documents requis.
4. Afficher la checklist `requirements` et `missingDocumentTypeCodes`.
5. Soumettre le dossier quand `complete=true`.
6. Agent: ouvrir `my-queue`, consulter OCR/preview, ajouter notes.
7. Agent: approuver/rejeter documents puis dossier.
8. Apres approbation, relire le proprietaire/subscription si une fonctionnalite depend du `kycLevel`.

## 7. Notifications et workers

Templates a prevoir dans le centre notification/admin:

- rappel booking;
- rappel billing;
- expiration subscription;
- expiration KYC;
- notification waitlist;
- notification contrat/signature.

Regle frontend:

- exposer les templates et statuts d'envoi;
- ne pas declencher manuellement les rappels automatiques dans les parcours standards;
- garder des actions admin separees pour relance ou reprocessing si elles sont exposees.

## 8. Inventory

Le module Inventory est inclus dans les changements a documenter. La doc complete reste dans `docs/inventory-frontend-api.md`; ci-dessous les points prioritaires pour le frontend.

### Dashboard et rapports

| Methode | Endpoint | Usage |
| --- | --- | --- |
| `GET` | `/inventory/admin/dashboard` | KPIs stock, alertes, achats |
| `GET` | `/inventory/admin/reports/movements` | Rapport mouvements JSON |
| `GET` | `/inventory/admin/reports/movements.csv` | Export CSV |
| `GET` | `/inventory/admin/reports/movements.pdf` | Export PDF |
| `GET` | `/inventory/admin/reports/anomalies` | Rapport anomalies |

Parametres rapports mouvements:

- `itemCode`;
- `locationCode`;
- `fromDate`;
- `toDate`.

UI recommandee:

- dashboard inventaire avec alertes ouvertes, ruptures, valeur stock, lots expirants;
- page rapports avec filtres periode/article/emplacement;
- boutons export CSV/PDF.

### Etiquettes QR/barcode

Endpoints:

| Methode | Endpoint | Usage |
| --- | --- | --- |
| `GET` | `/inventory/admin/labels/{type}/{code}` | Generer une etiquette |
| `POST` | `/inventory/admin/labels` | Generer des etiquettes en lot |

Payload batch:

```json
{
  "items": [
    { "type": "item", "code": "ART-001" },
    { "type": "asset", "code": "AST-001" },
    { "type": "location", "code": "ENTREPOT-A" }
  ]
}
```

Reponse:

```json
[
  {
    "labelType": "asset",
    "code": "AST-001",
    "displayText": "Chaise de bureau",
    "barcodeValue": "AST-001",
    "qrValue": "inventory:asset:AST-001"
  }
]
```

Impact UI:

- ajouter `Imprimer etiquette` sur fiches article, actif, emplacement et lot si supporte;
- ajouter une page batch labels avec selection multiple;
- rendre `barcodeValue` en code-barres et `qrValue` en QR code;
- permettre impression A4/etiquettes et export PDF cote navigateur si necessaire.

### Achats, demandes d'achat et transformation en bon de commande

Endpoints procurement prioritaires:

| Methode | Endpoint | Usage |
| --- | --- | --- |
| `POST` | `/inventory/procurement/suppliers` | Creer un fournisseur |
| `GET` | `/inventory/procurement/suppliers?q=...` | Lister/rechercher fournisseurs |
| `GET` | `/inventory/procurement/suppliers/{supplierCode}/performance` | Performance fournisseur |
| `POST` | `/inventory/procurement/purchase-requests` | Creer une demande d'achat |
| `POST` | `/inventory/procurement/purchase-requests/from-reorder-suggestions?locationCode=...&supplierCode=...&requestedBy=...` | Creer automatiquement une demande depuis les suggestions de reappro |
| `PATCH` | `/inventory/procurement/purchase-requests/{requestCode}/submit` | Soumettre la demande |
| `PATCH` | `/inventory/procurement/purchase-requests/{requestCode}/approve?approvedBy=...&supplierCode=...` | Approuver la demande |
| `PATCH` | `/inventory/procurement/purchase-requests/{requestCode}/reject?reason=...` | Rejeter la demande |
| `GET` | `/inventory/procurement/purchase-requests?status=APPROVED&locationCode=...&q=...` | Lister les demandes |
| `POST` | `/inventory/procurement/purchase-orders` | Creer un bon de commande manuel |
| `POST` | `/inventory/procurement/purchase-requests/{requestCode}/purchase-order?supplierCode=...` | Transformer automatiquement une demande approuvee en bon de commande |
| `PATCH` | `/inventory/procurement/purchase-orders/{orderCode}/approve?approvedBy=...` | Approuver un bon de commande |
| `PATCH` | `/inventory/procurement/purchase-orders/{orderCode}/mark-ordered` | Marquer la commande envoyee |
| `GET` | `/inventory/procurement/purchase-orders?status=ORDERED&supplierCode=...&locationCode=...&q=...` | Lister les bons de commande |
| `POST` | `/inventory/procurement/goods-receipts` | Enregistrer une reception |
| `GET` | `/inventory/procurement/goods-receipts?status=...&locationCode=...&orderCode=...&q=...` | Lister les receptions |
| `POST` | `/inventory/procurement/approval-rules` | Creer/modifier une regle d'approbation |
| `GET` | `/inventory/procurement/approval-rules` | Lister les regles d'approbation |

Transformation demande d'achat vers bon de commande:

```http
POST /sni/api/v1/inventory/procurement/purchase-requests/PR-202604-000001/purchase-order?supplierCode=SUP-001
```

Comportement attendu cote UI:

- afficher le bouton `Creer un bon de commande` sur une demande approuvee;
- demander/selectionner `supplierCode` avant l'appel si la demande n'a pas deja un fournisseur clair;
- apres succes, rediriger vers le detail du bon de commande retourne;
- ne pas dupliquer les lignes cote frontend: le backend copie les lignes de la demande;
- si la demande vient des suggestions de reappro, afficher l'origine dans l'historique de la demande si le DTO l'expose.

Parcours achat automatise:

1. L'utilisateur consulte les suggestions de reappro.
2. Il cree une demande via `/purchase-requests/from-reorder-suggestions`.
3. Il soumet la demande.
4. Un manager approuve avec le fournisseur.
5. Le frontend appelle `/purchase-requests/{requestCode}/purchase-order`.
6. Le bon de commande est approuve si necessaire puis marque `ORDERED`.
7. La reception fournisseur met a jour le stock.

### Ecrans Inventory a prevoir

- `InventoryDashboardPage`;
- `CatalogItemListPage` et `CatalogItemFormPage`;
- `StockLevelPage`;
- `StockMovementListPage`;
- `StockInFormPage`, `StockOutFormPage`, `StockAdjustmentFormPage`;
- `InventoryAlertCenterPage`;
- `ReorderRuleListPage`;
- `SupplierListPage`, `PurchaseRequestListPage`, `PurchaseOrderListPage`;
- `GoodsReceiptFormPage`;
- `AssetListPage`, `AssetDetailPage`, `AssetMaintenancePage`;
- `InventoryCountPage`;
- `InventoryLabelPrintPage`;
- `InventoryReportPage`;
- `InventoryImportPage`.

## 9. Analytics et reporting

Endpoints a brancher:

| Methode | Endpoint | Usage |
| --- | --- | --- |
| `GET` | `/analytics/live` | KPIs temps reel |
| `GET` | `/analytics/comparison` | Comparaison periode |
| `GET` | `/reporting/finance/dashboard` | Dashboard finance |
| `GET` | `/reporting/payments` | Reporting paiements |
| `GET` | `/reporting/aging` | Balance agee |
| `GET` | `/reporting/cash-registers` | Caisses |
| `GET` | `/reporting/cash-flow` | Cash-flow |
| `GET` | `/reporting/occupancy` | Occupation ressources |

UI:

- dashboards par domaine: finance, occupancy, paiement, cash;
- filtres periode, ressource, type ressource, owner type;
- export si endpoint disponible.

## 10. Auth client platform - etape 1

Le frontend client doit partir sur une separation claire entre back-office et portail client.

Principes:

- routes client sous un espace dedie;
- token client avec contexte proprietaire (`ownerType`, `ownerCode`) quand disponible;
- toutes les pages client doivent lire uniquement les donnees du proprietaire connecte;
- pas de saisie manuelle de `ownerCode` dans l'UI client.

Ecrans clients attendus:

- mes reservations;
- mes subscriptions/pass/entitlements;
- mes factures et paiements;
- mes contrats;
- mes documents/KYC;
- mon profil.

## 11. Checklist integration

### Reservation avec subscription

- Creer une subscription payee.
- Attendre/rafraichir activation.
- Verifier que le contrat apparait.
- Creer une reservation `paymentMode=SUBSCRIPTION`.
- Verifier `subscriptionNumber` et `entitlementCode` sur la reservation.
- Tester une ressource d'un autre `resourceTypeCode/resourceGroupCode` pour verifier le message d'erreur.

### KYC

- Creer un dossier KYC pour `MEMBER`, `CUSTOMER`, `BUSINESS`.
- Verifier `ownerName` dans liste, detail, export PDF.
- Uploader un document et verifier OCR.
- Tester assignation, notes, timeline, risk level, bulk approve/reject.

### Waitlist

- Demander un creneau occupe.
- Afficher l'entree waitlist et les alternatives.
- Reserver une alternative sans supprimer automatiquement l'entree.
- Proposer l'annulation waitlist apres reservation alternative.

### Inventory

- Charger dashboard.
- Generer une etiquette unitaire.
- Generer des etiquettes batch.
- Verifier rendu QR/barcode.
- Exporter mouvements CSV/PDF.

### Paiement / contrat

- Payer une facture subscription.
- Verifier statut paiement.
- Relire subscription jusqu'a activation.
- Relire contrats jusqu'a apparition du contrat genere.
- Signer in-app et verifier statut/document signe.
