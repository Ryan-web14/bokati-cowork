# Traductions frontend en francais

Document genere depuis les DTO, reponses et enums Java du projet le 2026-04-22 04:09.
Il sert de dictionnaire de libelles pour afficher proprement les champs techniques de l API dans le front-end.

## Regles d utilisation cote front

- Garder les cles API en anglais dans les payloads et utiliser la colonne `Libelle FR` uniquement pour l affichage.
- Pour les tableaux, utiliser la section `Champs communs` comme fallback global.
- Pour les statuts et types, utiliser la section `Valeurs enumerees` afin de traduire les valeurs renvoyees par l API.
- Les libelles generes automatiquement doivent etre valides lors de la creation de nouveaux ecrans metier sensibles.

## Champs communs

| Cle API | Libelle FR |
|---|---|
| `accessToken` | Jeton d acces |
| `accountEnabled` | Compte active |
| `accountExpired` | Compte expire |
| `accountLocked` | Compte verrouille |
| `acknowledgedAt` | Confirme le |
| `action` | Action |
| `activatedAt` | Activated le |
| `active` | Actif |
| `activity` | Activite |
| `address` | Adresse |
| `alertCode` | Alerte Code |
| `alertType` | Alerte Type |
| `allowCancellation` | Autoriser Annulation |
| `allowedMimeTypes` | Types MIME autorises |
| `allowNegativeOverride` | Autoriser Negatif Derogation |
| `allowNegativeStock` | Autoriser Negatif Stock |
| `amenityCode` | Equipement Code |
| `antivirusStatus` | Antivirus Statut |
| `appliedAt` | Applique le |
| `approvalLevel` | Approbation Niveau |
| `approvalSteps` | Approbation Etapes |
| `approved` | Approuve |
| `approvedAt` | Approuve le |
| `approvedBy` | Approuve By |
| `assetCode` | Actif immobilise Code |
| `assetCondition` | Actif immobilise Etat |
| `assetSerialNumbers` | Actif immobilise Serie Numbers |
| `assetTag` | Actif immobilise Etiquette |
| `assetTags` | Actif immobilise Etiquettes |
| `assignedBy` | Assigne By |
| `assignedToCode` | Assigne Vers Code |
| `assignedToType` | Assigne Vers Type |
| `assigneeCode` | Assigne Code |
| `assigneeType` | Assigne Type |
| `authorities` | Autorisations |
| `availabilities` | Disponibilites |
| `available` | Disponible |
| `availableQuantity` | Disponible Quantite |
| `averageCost` | Moyen Cout |
| `averageDeliveryDelayDays` | Moyen Livraison Delay Days |
| `averageUnitCost` | Moyen Unite Cout |
| `backorderQuantity` | Reliquat Quantite |
| `barcodeValue` | Code-barres Valeur |
| `baseCurrencyCode` | Base Devise Code |
| `billingEmail` | E-mail de facturation |
| `billingPref` | Facturation Preference |
| `birthDate` | Naissance Date |
| `bookingEnabled` | Reservation Active |
| `bookingUnit` | Reservation Unite |
| `businessCode` | Entreprise Code |
| `businessEntityCode` | Entreprise Entity Code |
| `businessEntityName` | Entreprise Entity Nom |
| `businessLegalForm` | Entreprise Legal Forme |
| `cancellationNoticeMinutes` | Preavis d annulation en minutes |
| `capacity` | Capacite |
| `category` | Categorie |
| `categoryCode` | Categorie Code |
| `categoryName` | Categorie Nom |
| `changedAt` | Modifie le |
| `changedBy` | Modifie By |
| `changeNumber` | Changement Numero |
| `changeType` | Changement Type |
| `checkoutCondition` | Sortie Etat |
| `checkoutPhotoUrl` | Sortie Photo URL |
| `checksumSha256` | Somme de controle Sha256 |
| `city` | Ville |
| `clauses` | Clauses |
| `closedAt` | Ferme le |
| `code` | Code |
| `comment` | Commentaire |
| `companyName` | Nom de l entreprise |
| `companyRole` | Entreprise Role |
| `complete` | Complet |
| `completedAt` | Termine le |
| `completeReceiptCount` | Complet Reception Nombre |
| `completeReceiptRate` | Complet Reception Taux |
| `condition` | Etat |
| `contractCode` | Contrat Code |
| `cost` | Cout |
| `countCode` | Nombre Code |
| `countedQuantity` | Counted Quantite |
| `country` | Pays |
| `countryCode` | Pays Code |
| `createdAt` | Date de creation |
| `createdBy` | Cree By |
| `currencyCode` | Devise Code |
| `currencyName` | Devise Nom |
| `current` | Actuel |
| `currentPlanVersion` | Actuel Plan Version |
| `currentQuantity` | Actuel Quantite |
| `currentVersionNumber` | Actuel Version Numero |
| `customerId` | Identifiant client |
| `customerType` | Client Type |
| `data` | Donnees |
| `decisionComment` | Decision Commentaire |
| `defaultCost` | Default Cout |
| `defaultCurrency` | Default Devise |
| `deleted` | Supprime |
| `deliveryNoteDocumentCode` | Livraison Note Document Code |
| `depreciatedValue` | Depreciated Valeur |
| `description` | Description |
| `deviceType` | Device Type |
| `displayCode` | Affichage Code |
| `displayName` | Nom d affichage |
| `displayOrder` | Affichage Commande |
| `displayText` | Affichage Text |
| `district` | Quartier |
| `documentCode` | Document Code |
| `documentDetails` | Document Details |
| `documentNumber` | Document Numero |
| `documents` | Documents |
| `documentType` | Document Type |
| `documentTypeCode` | Code type de document |
| `documentTypeName` | Document Type Nom |
| `draftDocumentCode` | Draft Document Code |
| `dryRun` | Dry Run |
| `durationMinutes` | Duree en minutes |
| `effectiveDate` | Effective Date |
| `effectivePolicy` | Effective Policy |
| `email` | E-mail |
| `endAt` | Fin le |
| `endDate` | Fin Date |
| `endedAt` | Date de fin |
| `errorCode` | Erreur Code |
| `errorCount` | Erreur Nombre |
| `errorDescription` | Erreur Description |
| `estimatedOrderCost` | Estimated Commande Cout |
| `estimatedUnitCost` | Estimated Unite Cout |
| `existingCustomerId` | Existing Client Identifiant |
| `expectedDeliveryDate` | Expected Livraison Date |
| `expectedQuantity` | Expected Quantite |
| `expectedReturnAt` | Expected Return le |
| `expiresAt` | Date d expiration |
| `expiresIn` | Expire In |
| `expiringLots30Days` | Expiring Lots30 Days |
| `expiryDate` | Expiration Date |
| `expirySoonAlerts` | Expiration Soon Alerts |
| `factor` | Factor |
| `failedLoginAttempts` | Failed Login Attempts |
| `fileExtension` | Fichier Extension |
| `fileName` | Fichier Nom |
| `fileSize` | Fichier Taille |
| `fileSizeBytes` | Fichier Taille Bytes |
| `fileUrl` | Fichier URL |
| `firstMovementAt` | Premier Mouvement le |
| `firstname` | Prenom |
| `firstName` | Prenom |
| `floorLabel` | Floor Libelle |
| `fromDate` | Depuis Date |
| `fromLocationCode` | Depuis Emplacement Code |
| `fromUnitCode` | Depuis Unite Code |
| `fullAddress` | Full Adresse |
| `fullname` | Fullname |
| `fullName` | Full Nom |
| `fullyCovered` | Fully Covered |
| `generatedAt` | Generated le |
| `generatedPassword` | Generated Password |
| `generatePassword` | Generate Password |
| `groupCode` | Groupe Code |
| `groupId` | Groupe Identifiant |
| `helpText` | Help Text |
| `html` | Html |
| `id` | Identifiant |
| `identificationCode` | Identification Code |
| `importType` | Import Type |
| `initialQuantity` | Initial Quantite |
| `invoiceDocumentCode` | Facture Document Code |
| `ipAddress` | Ip Adresse |
| `isAccountEnabled` | Is Compte Active |
| `isAccountLocked` | Is Compte Verrouille |
| `isActive` | Is Actif |
| `isDevEnvironment` | Is Dev Environment |
| `isOhadaMember` | Is Ohada Membre |
| `issueDate` | Emission Date |
| `itemCode` | Article Code |
| `itemName` | Article Nom |
| `items` | Articles |
| `itemType` | Article Type |
| `jobTitle` | Job Titre |
| `labelType` | Libelle Type |
| `langage` | Langage |
| `largeInventoryCountVariances` | Large Inventaire Nombre Variances |
| `lastAccessAt` | Dernier Acces le |
| `lastLogin` | Dernier Login |
| `lastMovementAt` | Dernier Mouvement le |
| `lastname` | Nom |
| `lastName` | Nom |
| `legalForm` | Legal Forme |
| `lines` | Lines |
| `location` | Emplacement |
| `locationCode` | Emplacement Code |
| `locationFromCode` | Emplacement Depuis Code |
| `locationGuess` | Emplacement Guess |
| `locationLabel` | Emplacement Libelle |
| `locationName` | Emplacement Nom |
| `locations` | Locations |
| `locationToCode` | Emplacement Vers Code |
| `locationType` | Emplacement Type |
| `lotNumber` | Lot Numero |
| `lots` | Lots |
| `lowStockAlerts` | Low Stock Alerts |
| `maintenanceCode` | Maintenance Code |
| `maintenanceType` | Maintenance Type |
| `maxAmount` | Maximum Montant |
| `maxBookingDurationMinutes` | Duree maximale de reservation en minutes |
| `maxFileSizeBytes` | Maximum Fichier Taille Bytes |
| `maxQuantity` | Maximum Quantite |
| `memberId` | Identifiant membre |
| `message` | Message |
| `mimeType` | MIME Type |
| `mimeTypeDeclared` | MIME Type Declared |
| `mimeTypeDetected` | MIME Type Detected |
| `minAmount` | Minimum Montant |
| `minBookingDurationMinutes` | Duree minimale de reservation en minutes |
| `minBookingNoticeMinutes` | Preavis minimum de reservation en minutes |
| `minQuantity` | Minimum Quantite |
| `missingDocumentTypeCodes` | Missing Document Type Codes |
| `module` | Module |
| `movementCode` | Mouvement Code |
| `movementCount` | Mouvement Nombre |
| `movementsWithNegativeOverride` | Mouvements With Negatif Derogation |
| `movementType` | Mouvement Type |
| `multipleAllowed` | Multiple Autorise |
| `mustSign` | Must Sign |
| `name` | Nom |
| `negativeStockAlerts` | Negatif Stock Alerts |
| `negativeStockLevels` | Negatif Stock Levels |
| `netQuantity` | Net Quantite |
| `netValue` | Net Valeur |
| `newPassword` | New Password |
| `niuNumber` | Niu Numero |
| `notes` | Notes |
| `notificationPref` | Notification Preference |
| `openAlerts` | Open Alerts |
| `openPurchaseOrders` | Open Achat Orders |
| `orderCode` | Commande Code |
| `orderedBy` | Ordered By |
| `originalFileName` | Original Fichier Nom |
| `originalMovementCode` | Original Mouvement Code |
| `ottToken` | Ott Jeton |
| `outOfStockAlerts` | Out Of Stock Alerts |
| `outOfStockLevels` | Out Of Stock Levels |
| `ownerCode` | Code proprietaire |
| `ownerId` | Proprietaire Identifiant |
| `ownerName` | Proprietaire Nom |
| `ownershipType` | Ownership Type |
| `ownerType` | Type de proprietaire |
| `pageable` | Pagination |
| `parentCategoryCode` | Parent Categorie Code |
| `parentCategoryName` | Parent Categorie Nom |
| `parentLocationCode` | Parent Emplacement Code |
| `parties` | Parties |
| `partyCode` | Partie Code |
| `partyType` | Partie Type |
| `password` | Password |
| `patternKey` | Pattern Key |
| `performedAt` | Performed le |
| `performedBy` | Performed By |
| `permissions` | Permissions |
| `phone` | Telephone |
| `phoneCode` | Telephone Code |
| `pickQuantity` | Pick Quantite |
| `policyCode` | Policy Code |
| `policyId` | Policy Identifiant |
| `portalAccess` | Portal Acces |
| `portalVisible` | Portal Visible |
| `postedAt` | Comptabilise le |
| `preferredSupplierCode` | Preferred Fournisseur Code |
| `price` | Prix |
| `priorityScore` | Priorite Score |
| `proofDocumentCode` | Proof Document Code |
| `prorationAmount` | Proration Montant |
| `providerName` | Fournisseur Nom |
| `psku` | Psku |
| `purchaseCost` | Achat Cout |
| `purchaseDate` | Achat Date |
| `purchaseOrderCount` | Achat Commande Nombre |
| `qrValue` | Qr Valeur |
| `qualityAccepted` | Quality Accepted |
| `quantity` | Quantite |
| `quantityAvailable` | Quantite Disponible |
| `quantityDelta` | Quantite Delta |
| `quantityOnHand` | Quantite On Hand |
| `quantityReserved` | Quantite Reserved |
| `quarantineAcceptedStock` | Quarantine Accepted Stock |
| `quarantined` | Quarantined |
| `quarantineReason` | Quarantine Motif |
| `rccmNumber` | Rccm Numero |
| `reason` | Motif |
| `reasonCode` | Motif Code |
| `receiptCode` | Reception Code |
| `receivedAt` | Recu le |
| `receivedBy` | Recu By |
| `receivedOrderCount` | Recu Commande Nombre |
| `receivedQuantity` | Recu Quantite |
| `receiverSignatureUrl` | Receiver Signature URL |
| `recentMovements` | Recent Mouvements |
| `referenceCode` | Reference Code |
| `referenceType` | Reference Type |
| `refreshToken` | Refresh Jeton |
| `rejectedQuantity` | Rejected Quantite |
| `rejectionReason` | Rejet Motif |
| `rejectionReasonCode` | Rejet Motif Code |
| `rejectionReasonDetail` | Rejet Motif Detail |
| `remainingCapacity` | Capacite restante |
| `remainingQuantity` | Restant Quantite |
| `renewalType` | Renouvellement Type |
| `reorderQuantity` | Reapprovisionnement Quantite |
| `requestCode` | Demande Code |
| `requestedAt` | Requested le |
| `requestedBy` | Requested By |
| `requestedQuantity` | Requested Quantite |
| `required` | Obligatoire |
| `requiredApprovals` | Obligatoire Approvals |
| `requiresExpiryDate` | Requires Expiration Date |
| `requiresLotNumber` | Requires Lot Numero |
| `requiresReview` | Requires Review |
| `requiresSerialNumber` | Requires Serie Numero |
| `requiresSignature` | Requires Signature |
| `reservationCode` | Reservation Code |
| `reservedAt` | Reserved le |
| `reservedBy` | Reserved By |
| `reservedFrom` | Reserved Depuis |
| `residualValue` | Residual Valeur |
| `resolution` | Resolution |
| `resolvedAt` | Resolved le |
| `resourceCode` | Code ressource |
| `resourceName` | Nom de la ressource |
| `returnCondition` | Return Etat |
| `returnedBy` | Returned By |
| `returnPhotoUrl` | Return Photo URL |
| `reversalMovementCode` | Reversal Mouvement Code |
| `reversalOfMovementCode` | Reversal Of Mouvement Code |
| `reversalReason` | Reversal Motif |
| `reversed` | Reversed |
| `reversedAt` | Reversed le |
| `reversedBy` | Reversed By |
| `reviewedAt` | Revise le |
| `reviewedBy` | Revise By |
| `role` | Role |
| `roleNames` | Role Names |
| `rowNumber` | Row Numero |
| `rows` | Rows |
| `ruleScope` | Rule Scope |
| `salePrice` | Sale Prix |
| `scheduledAt` | Planifie le |
| `serialNumber` | Serie Numero |
| `sessionId` | Session Identifiant |
| `severity` | Severity |
| `shippedAt` | Shipped le |
| `shippedBy` | Shipped By |
| `shortageQuantity` | Shortage Quantite |
| `shortCode` | Short Code |
| `showEmail` | Show E-mail |
| `showNameOnDisplay` | Show Nom On Affichage |
| `showPhone` | Show Telephone |
| `signatoryName` | Signatory Nom |
| `signatoryRole` | Signatory Role |
| `signedAt` | Signed le |
| `signedDocumentCode` | Signed Document Code |
| `signedQuantity` | Signed Quantite |
| `signedValue` | Signed Valeur |
| `signOrder` | Sign Commande |
| `slotCount` | Nombre de creneaux |
| `slotDurationMinutes` | Creneau Duree Minutes |
| `sourceRequestCode` | Source Demande Code |
| `specification` | Specification |
| `startAt` | Debut le |
| `startDate` | Debut Date |
| `startedAt` | Date de debut |
| `status` | Statut |
| `storagePath` | Storage Chemin |
| `storageProvider` | Storage Fournisseur |
| `storedFileName` | Stored Fichier Nom |
| `streetName` | Rue Nom |
| `streetNumber` | Rue Numero |
| `submittedAt` | Soumis le |
| `subscription` | Abonnement |
| `success` | Success |
| `successCount` | Success Nombre |
| `suggestedQuantity` | Suggested Quantite |
| `summary` | Summary |
| `supplierCode` | Fournisseur Code |
| `supplierName` | Fournisseur Nom |
| `suspiciousAdjustments` | Suspicious Adjustments |
| `targetPlanVersion` | Cible Plan Version |
| `targetQuantity` | Cible Quantite |
| `taxable` | Taxable |
| `taxId` | Taxe Identifiant |
| `templateCode` | Modele Code |
| `terminatedAt` | Terminated le |
| `terminationReason` | Termination Motif |
| `thresholdQuantity` | Seuil Quantite |
| `timestamp` | Horodatage |
| `timeZone` | Heure Zone |
| `title` | Titre |
| `toDate` | Vers Date |
| `token` | Jeton |
| `toLocationCode` | Vers Emplacement Code |
| `totalAmount` | Montant total |
| `totalCapacity` | Capacite totale |
| `totalCost` | Total Cout |
| `totalInQuantity` | Total In Quantite |
| `totalInValue` | Total In Valeur |
| `totalMovementValue` | Total Mouvement Valeur |
| `totalOutQuantity` | Total Out Quantite |
| `totalOutValue` | Total Out Valeur |
| `totalQuantity` | Total Quantite |
| `totalRows` | Total Rows |
| `totalStockValue` | Total Stock Valeur |
| `totalValue` | Total Valeur |
| `toUnitCode` | Vers Unite Code |
| `trackingType` | Tracking Type |
| `transferCode` | Transfert Code |
| `type` | Type |
| `typeCode` | Type Code |
| `typeId` | Type Identifiant |
| `unitCode` | Unite Code |
| `unitCost` | Unite Cout |
| `unitName` | Unite Nom |
| `unitType` | Unite Type |
| `updatedAt` | Date de mise a jour |
| `uploadedAt` | Uploaded le |
| `uploadedBy` | Uploaded By |
| `uploadStatus` | Upload Statut |
| `usefulLifeMonths` | Useful Life Months |
| `userAgent` | Utilisateur Agent |
| `userId` | Utilisateur Identifiant |
| `validatedAt` | Validated le |
| `variables` | Variables |
| `varianceQuantity` | Variance Quantite |
| `verificationToken` | Verification Jeton |
| `versionNumber` | Version Numero |
| `versions` | Versions |
| `walletPref` | Portefeuille Preference |
| `warrantyEndDate` | Warranty Fin Date |
| `whatsapp_phone` | WhatsApp Telephone |
| `whatsappPhone` | WhatsApp Telephone |
| `zone` | Zone |

## Champs par section

### Abonnements

#### SubscriptionChangeRequest (DTO)

| Champ | Type | Libelle FR |
|---|---|---|
| `appliedAt` | `Instant` | Applique le |
| `approvedBy` | `String` | Approuve By |
| `changeNumber` | `String` | Changement Numero |
| `changeType` | `SubscriptionChangeType` | Changement Type |
| `createdAt` | `Instant` | Date de creation |
| `currentPlanVersion` | `PlanVersion` | Actuel Plan Version |
| `effectiveDate` | `LocalDate` | Effective Date |
| `effectivePolicy` | `SubscriptionChangeEffectivePolicy` | Effective Policy |
| `id` | `Long` | Identifiant |
| `prorationAmount` | `BigDecimal` | Proration Montant |
| `reason` | `String` | Motif |
| `requestedBy` | `String` | Requested By |
| `status` | `SubscriptionChangeStatus` | Statut |
| `subscription` | `Subscription` | Abonnement |
| `targetPlanVersion` | `PlanVersion` | Cible Plan Version |
| `updatedAt` | `Instant` | Date de mise a jour |

### Clients et membres

#### ChangeCustomerStatusRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `status` | `CustomerStatus` | Statut |

#### CompanyCustomerResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `address` | `AddressResponse` | Adresse |
| `billingEmail` | `String` | E-mail de facturation |
| `companyName` | `String` | Nom de l entreprise |
| `createdAt` | `String` | Date de creation |
| `email` | `String` | E-mail |
| `phone` | `String` | Telephone |
| `status` | `String` | Statut |
| `whatsappPhone` | `String` | WhatsApp Telephone |

#### CreateMemberRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `address` | `AddressRequest` | Adresse |
| `billingEmail` | `String` | E-mail de facturation |
| `companyName` | `String` | Nom de l entreprise |
| `customerType` | `String` | Client Type |
| `email` | `String` | E-mail |
| `existingCustomerId` | `String` | Existing Client Identifiant |
| `firstname` | `String` | Prenom |
| `generatePassword` | `boolean` | Generate Password |
| `lastname` | `String` | Nom |
| `password` | `String` | Password |
| `phone` | `String` | Telephone |
| `whatsappPhone` | `String` | WhatsApp Telephone |

#### CustomerCreationResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `customerId` | `String` | Identifiant client |
| `type` | `String` | Type |

#### CustomerRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `address` | `AddressRequest` | Adresse |
| `billingEmail` | `String` | E-mail de facturation |
| `companyName` | `String` | Nom de l entreprise |
| `email` | `String` | E-mail |
| `firstname` | `String` | Prenom |
| `lastname` | `String` | Nom |
| `phone` | `String` | Telephone |
| `type` | `String` | Type |
| `whatsappPhone` | `String` | WhatsApp Telephone |

#### CustomerResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `address` | `AddressResponse` | Adresse |
| `billingEmail` | `String` | E-mail de facturation |
| `companyName` | `String` | Nom de l entreprise |
| `createdAt` | `String` | Date de creation |
| `customerId` | `String` | Identifiant client |
| `email` | `String` | E-mail |
| `firstname` | `String` | Prenom |
| `lastname` | `String` | Nom |
| `phone` | `String` | Telephone |
| `status` | `String` | Statut |
| `type` | `String` | Type |
| `whatsappPhone` | `String` | WhatsApp Telephone |

#### CustomerSummaryresponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `customerId` | `String` | Identifiant client |
| `displayName` | `String` | Nom d affichage |
| `email` | `String` | E-mail |
| `id` | `Long` | Identifiant |
| `phone` | `String` | Telephone |
| `status` | `String` | Statut |

#### MemberProfileRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `address` | `String` | Adresse |
| `birthDate` | `LocalDate` | Naissance Date |
| `city` | `String` | Ville |
| `companyRole` | `String` | Entreprise Role |
| `country` | `String` | Pays |
| `jobTitle` | `String` | Job Titre |

#### MemberProfileResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `address` | `String` | Adresse |
| `birthDate` | `LocalDate` | Naissance Date |
| `city` | `String` | Ville |
| `companyRole` | `String` | Entreprise Role |
| `country` | `String` | Pays |
| `email` | `String` | E-mail |
| `firstName` | `String` | Prenom |
| `jobTitle` | `String` | Job Titre |
| `lastName` | `String` | Nom |
| `memberId` | `String` | Identifiant membre |
| `phone` | `String` | Telephone |
| `whatsappPhone` | `String` | WhatsApp Telephone |

#### MemberResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `createdAt` | `Instant` | Date de creation |
| `customerId` | `String` | Identifiant client |
| `email` | `String` | E-mail |
| `firstname` | `String` | Prenom |
| `fullname` | `String` | Fullname |
| `lastname` | `String` | Nom |
| `memberId` | `String` | Identifiant membre |
| `phone` | `String` | Telephone |
| `portalAccess` | `boolean` | Portal Acces |
| `status` | `String` | Statut |
| `updatedAt` | `Instant` | Date de mise a jour |
| `userId` | `String` | Utilisateur Identifiant |
| `whatsappPhone` | `String` | WhatsApp Telephone |

#### MemberSummaryResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `customerId` | `String` | Identifiant client |
| `email` | `String` | E-mail |
| `fullName` | `String` | Full Nom |
| `memberId` | `String` | Identifiant membre |
| `phone` | `String` | Telephone |
| `portalAccess` | `Boolean` | Portal Acces |
| `status` | `String` | Statut |
| `whatsapp_phone` | `String` | WhatsApp Telephone |

#### UpdateMemberProfileRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `address` | `String` | Adresse |
| `birthDate` | `LocalDate` | Naissance Date |
| `city` | `String` | Ville |
| `companyRole` | `String` | Entreprise Role |
| `country` | `String` | Pays |
| `jobTitle` | `String` | Job Titre |

#### UpdateMemberRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `email` | `String` | E-mail |
| `firstname` | `String` | Prenom |
| `lastname` | `String` | Nom |
| `phone` | `String` | Telephone |
| `whatsappPhone` | `String` | WhatsApp Telephone |

### Contrats

#### AttachContractDocumentRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `documentCode` | `String` | Document Code |

#### ContractPartyRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `displayName` | `String` | Nom d affichage |
| `email` | `String` | E-mail |
| `mustSign` | `Boolean` | Must Sign |
| `partyCode` | `String` | Partie Code |
| `partyType` | `DocumentOwnerType` | Partie Type |
| `phone` | `String` | Telephone |
| `role` | `ContractPartyRole` | Role |
| `signOrder` | `Integer` | Sign Commande |

#### ContractPartyResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `displayName` | `String` | Nom d affichage |
| `email` | `String` | E-mail |
| `mustSign` | `Boolean` | Must Sign |
| `partyCode` | `String` | Partie Code |
| `partyType` | `DocumentOwnerType` | Partie Type |
| `phone` | `String` | Telephone |
| `role` | `ContractPartyRole` | Role |
| `signOrder` | `Integer` | Sign Commande |

#### ContractPreviewResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `html` | `String` | Html |
| `templateCode` | `String` | Modele Code |

#### ContractResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `activatedAt` | `Instant` | Activated le |
| `businessCode` | `String` | Entreprise Code |
| `contractCode` | `String` | Contrat Code |
| `createdAt` | `Instant` | Date de creation |
| `createdBy` | `Long` | Cree By |
| `description` | `String` | Description |
| `draftDocumentCode` | `String` | Draft Document Code |
| `effectiveDate` | `LocalDate` | Effective Date |
| `endDate` | `LocalDate` | Fin Date |
| `ownerCode` | `String` | Code proprietaire |
| `ownerType` | `DocumentOwnerType` | Type de proprietaire |
| `parties` | `List<ContractPartyResponse>` | Parties |
| `renewalType` | `ContractRenewalType` | Renouvellement Type |
| `signedAt` | `Instant` | Signed le |
| `signedDocumentCode` | `String` | Signed Document Code |
| `startDate` | `LocalDate` | Debut Date |
| `status` | `ContractStatus` | Statut |
| `templateCode` | `String` | Modele Code |
| `terminatedAt` | `Instant` | Terminated le |
| `terminationReason` | `String` | Termination Motif |
| `title` | `String` | Titre |
| `updatedAt` | `Instant` | Date de mise a jour |

#### ContractTemplateResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `code` | `String` | Code |
| `description` | `String` | Description |
| `name` | `String` | Nom |

#### CreateContractRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `businessCode` | `String` | Entreprise Code |
| `createdBy` | `Long` | Cree By |
| `description` | `String` | Description |
| `effectiveDate` | `LocalDate` | Effective Date |
| `endDate` | `LocalDate` | Fin Date |
| `ownerCode` | `String` | Code proprietaire |
| `ownerType` | `DocumentOwnerType` | Type de proprietaire |
| `parties` | `List<ContractPartyRequest>` | Parties |
| `renewalType` | `ContractRenewalType` | Renouvellement Type |
| `startDate` | `LocalDate` | Debut Date |
| `templateCode` | `String` | Modele Code |
| `title` | `String` | Titre |

#### GenerateContractRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `businessCode` | `String` | Entreprise Code |
| `clauses` | `List<String>` | Clauses |
| `description` | `String` | Description |
| `effectiveDate` | `LocalDate` | Effective Date |
| `endDate` | `LocalDate` | Fin Date |
| `ownerCode` | `String` | Code proprietaire |
| `ownerType` | `DocumentOwnerType` | Type de proprietaire |
| `signatoryName` | `String` | Signatory Nom |
| `signatoryRole` | `String` | Signatory Role |
| `startDate` | `LocalDate` | Debut Date |
| `templateCode` | `String` | Modele Code |
| `title` | `String` | Titre |
| `uploadedBy` | `Long` | Uploaded By |
| `variables` | `Map<String, String>` | Variables |

#### GenerateStoredContractDraftRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `uploadedBy` | `Long` | Uploaded By |

#### UpdateContractRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `businessCode` | `String` | Entreprise Code |
| `description` | `String` | Description |
| `effectiveDate` | `LocalDate` | Effective Date |
| `endDate` | `LocalDate` | Fin Date |
| `parties` | `List<ContractPartyRequest>` | Parties |
| `renewalType` | `ContractRenewalType` | Renouvellement Type |
| `startDate` | `LocalDate` | Debut Date |
| `templateCode` | `String` | Modele Code |
| `title` | `String` | Titre |

#### UpdateContractStatusRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `reason` | `String` | Motif |
| `status` | `String` | Statut |

### Documents et KYC

#### CreateKycCaseRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `ownerCode` | `String` | Code proprietaire |
| `ownerType` | `DocumentOwnerType` | Type de proprietaire |

#### DocumentRequirementRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `active` | `Boolean` | Actif |
| `businessLegalForm` | `String` | Entreprise Legal Forme |
| `customerType` | `String` | Client Type |
| `documentTypeCode` | `String` | Code type de document |
| `documentTypeName` | `String` | Document Type Nom |
| `ownerType` | `DocumentOwnerType` | Type de proprietaire |
| `required` | `Boolean` | Obligatoire |

#### DocumentRequirementResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `active` | `Boolean` | Actif |
| `businessLegalForm` | `String` | Entreprise Legal Forme |
| `customerType` | `String` | Client Type |
| `documentTypeCode` | `String` | Code type de document |
| `documentTypeName` | `String` | Document Type Nom |
| `id` | `Long` | Identifiant |
| `ownerType` | `DocumentOwnerType` | Type de proprietaire |
| `required` | `Boolean` | Obligatoire |

#### DocumentResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `category` | `DocumentCategory` | Categorie |
| `checksumSha256` | `String` | Somme de controle Sha256 |
| `code` | `String` | Code |
| `currentVersionNumber` | `Integer` | Actuel Version Numero |
| `description` | `String` | Description |
| `documentTypeCode` | `String` | Code type de document |
| `documentTypeName` | `String` | Document Type Nom |
| `expiryDate` | `LocalDate` | Expiration Date |
| `fileName` | `String` | Fichier Nom |
| `fileSize` | `Long` | Fichier Taille |
| `fileUrl` | `String` | Fichier URL |
| `issueDate` | `LocalDate` | Emission Date |
| `mimeType` | `String` | MIME Type |
| `ownerId` | `Long` | Proprietaire Identifiant |
| `ownerType` | `DocumentOwnerType` | Type de proprietaire |
| `status` | `DocumentStatus` | Statut |
| `title` | `String` | Titre |
| `updatedAt` | `Instant` | Date de mise a jour |
| `uploadedAt` | `Instant` | Uploaded le |
| `uploadedBy` | `Long` | Uploaded By |
| `versions` | `List<DocumentVersionResponse>` | Versions |

#### DocumentReviewDecisionRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `comment` | `String` | Commentaire |
| `rejectionReasonCode` | `String` | Rejet Motif Code |
| `rejectionReasonDetail` | `String` | Rejet Motif Detail |
| `reviewedBy` | `Long` | Revise By |

#### DocumentTypeRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `active` | `Boolean` | Actif |
| `allowedMimeTypes` | `String` | Types MIME autorises |
| `category` | `DocumentCategory` | Categorie |
| `code` | `String` | Code |
| `description` | `String` | Description |
| `documentDetails` | `String` | Document Details |
| `helpText` | `String` | Help Text |
| `maxFileSizeBytes` | `Long` | Maximum Fichier Taille Bytes |
| `multipleAllowed` | `Boolean` | Multiple Autorise |
| `name` | `String` | Nom |
| `ownerType` | `DocumentOwnerType` | Type de proprietaire |
| `required` | `Boolean` | Obligatoire |
| `requiresExpiryDate` | `Boolean` | Requires Expiration Date |
| `requiresReview` | `Boolean` | Requires Review |
| `requiresSignature` | `Boolean` | Requires Signature |

#### DocumentTypeResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `active` | `Boolean` | Actif |
| `allowedMimeTypes` | `String` | Types MIME autorises |
| `category` | `DocumentCategory` | Categorie |
| `code` | `String` | Code |
| `description` | `String` | Description |
| `documentDetails` | `String` | Document Details |
| `helpText` | `String` | Help Text |
| `maxFileSizeBytes` | `Long` | Maximum Fichier Taille Bytes |
| `multipleAllowed` | `Boolean` | Multiple Autorise |
| `name` | `String` | Nom |
| `ownerType` | `DocumentOwnerType` | Type de proprietaire |
| `required` | `Boolean` | Obligatoire |
| `requiresExpiryDate` | `Boolean` | Requires Expiration Date |
| `requiresReview` | `Boolean` | Requires Review |
| `requiresSignature` | `Boolean` | Requires Signature |

#### DocumentUploadMetadataRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `description` | `String` | Description |
| `documentNumber` | `String` | Document Numero |
| `documentTypeCode` | `String` | Code type de document |
| `expiryDate` | `LocalDate` | Expiration Date |
| `issueDate` | `LocalDate` | Emission Date |
| `ownerCode` | `String` | Code proprietaire |
| `ownerType` | `DocumentOwnerType` | Type de proprietaire |
| `title` | `String` | Titre |
| `uploadedBy` | `String` | Uploaded By |

#### DocumentVersionResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `antivirusStatus` | `DocumentAntivirusStatus` | Antivirus Statut |
| `checksumSha256` | `String` | Somme de controle Sha256 |
| `current` | `Boolean` | Actuel |
| `fileExtension` | `String` | Fichier Extension |
| `fileSizeBytes` | `Long` | Fichier Taille Bytes |
| `mimeTypeDeclared` | `String` | MIME Type Declared |
| `mimeTypeDetected` | `String` | MIME Type Detected |
| `originalFileName` | `String` | Original Fichier Nom |
| `storagePath` | `String` | Storage Chemin |
| `storageProvider` | `String` | Storage Fournisseur |
| `storedFileName` | `String` | Stored Fichier Nom |
| `uploadedAt` | `Instant` | Uploaded le |
| `uploadedBy` | `Long` | Uploaded By |
| `uploadStatus` | `DocumentVersionUploadStatus` | Upload Statut |
| `versionNumber` | `Integer` | Version Numero |

#### KycCaseResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `approved` | `boolean` | Approuve |
| `code` | `String` | Code |
| `complete` | `boolean` | Complet |
| `completedAt` | `Instant` | Termine le |
| `decisionComment` | `String` | Decision Commentaire |
| `documents` | `List<KycDocumentResponse>` | Documents |
| `missingDocumentTypeCodes` | `List<String>` | Missing Document Type Codes |
| `ownerCode` | `String` | Code proprietaire |
| `ownerId` | `Long` | Proprietaire Identifiant |
| `ownerName` | `String` | Proprietaire Nom |
| `ownerType` | `DocumentOwnerType` | Type de proprietaire |
| `reviewedAt` | `Instant` | Revise le |
| `reviewedBy` | `Long` | Revise By |
| `startedAt` | `Instant` | Date de debut |
| `status` | `KycCaseStatus` | Statut |
| `submittedAt` | `Instant` | Soumis le |

#### KycDecisionRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `comment` | `String` | Commentaire |
| `reviewedBy` | `Long` | Revise By |

#### KycDocumentResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `documentCode` | `String` | Document Code |
| `documentNumber` | `String` | Document Numero |
| `documentType` | `String` | Document Type |
| `expiryDate` | `LocalDate` | Expiration Date |
| `id` | `Long` | Identifiant |
| `issueDate` | `LocalDate` | Emission Date |
| `status` | `KycDocumentVerificationStatus` | Statut |

### Entreprises

#### BusinessEntityRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `activity` | `String` | Activite |
| `address` | `AddressRequest` | Adresse |
| `baseCurrencyCode` | `String` | Base Devise Code |
| `email` | `String` | E-mail |
| `legalForm` | `String` | Legal Forme |
| `name` | `String` | Nom |
| `niuNumber` | `String` | Niu Numero |
| `phone` | `String` | Telephone |
| `rccmNumber` | `String` | Rccm Numero |
| `taxId` | `String` | Taxe Identifiant |

#### BusinessEntityResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `activity` | `String` | Activite |
| `address` | `AddressResponse` | Adresse |
| `baseCurrencyCode` | `String` | Base Devise Code |
| `code` | `String` | Code |
| `email` | `String` | E-mail |
| `legalForm` | `String` | Legal Forme |
| `name` | `String` | Nom |
| `niuNumber` | `String` | Niu Numero |
| `phone` | `String` | Telephone |
| `rccmNumber` | `String` | Rccm Numero |
| `status` | `String` | Statut |
| `taxId` | `String` | Taxe Identifiant |

#### BusinessSearchCriteria (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `activity` | `String` | Activite |
| `baseCurrencyCode` | `String` | Base Devise Code |
| `code` | `String` | Code |
| `email` | `String` | E-mail |
| `legalForm` | `String` | Legal Forme |
| `name` | `String` | Nom |
| `niuNumber` | `String` | Niu Numero |
| `phone` | `String` | Telephone |
| `rccmNumber` | `String` | Rccm Numero |
| `status` | `String` | Statut |
| `taxId` | `String` | Taxe Identifiant |

#### UpdateBusinessStatusRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `status` | `String` | Statut |

### Inventaire

#### AssetAssignmentResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `assetCode` | `String` | Actif immobilise Code |
| `assignedBy` | `String` | Assigne By |
| `assigneeCode` | `String` | Assigne Code |
| `assigneeType` | `AssetAssigneeType` | Assigne Type |
| `checkoutCondition` | `AssetCondition` | Sortie Etat |
| `checkoutPhotoUrl` | `String` | Sortie Photo URL |
| `endAt` | `Instant` | Fin le |
| `expectedReturnAt` | `Instant` | Expected Return le |
| `notes` | `String` | Notes |
| `receiverSignatureUrl` | `String` | Receiver Signature URL |
| `returnCondition` | `AssetCondition` | Return Etat |
| `returnedBy` | `String` | Returned By |
| `returnPhotoUrl` | `String` | Return Photo URL |
| `startAt` | `Instant` | Debut le |
| `status` | `AssetAssignmentStatus` | Statut |

#### AssetAssignRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `assignedBy` | `String` | Assigne By |
| `assigneeCode` | `String` | Assigne Code |
| `assigneeType` | `AssetAssigneeType` | Assigne Type |
| `checkoutPhotoUrl` | `String` | Sortie Photo URL |
| `expectedReturnAt` | `Instant` | Expected Return le |
| `notes` | `String` | Notes |
| `receiverSignatureUrl` | `String` | Receiver Signature URL |
| `startAt` | `Instant` | Debut le |

#### AssetLocationHistoryResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `assetCode` | `String` | Actif immobilise Code |
| `changedAt` | `Instant` | Modifie le |
| `changedBy` | `String` | Modifie By |
| `fromLocationCode` | `String` | Depuis Emplacement Code |
| `reason` | `String` | Motif |
| `toLocationCode` | `String` | Vers Emplacement Code |

#### AssetMaintenanceCompleteRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `assetCondition` | `AssetCondition` | Actif immobilise Etat |
| `cost` | `Long` | Cout |
| `resolution` | `String` | Resolution |

#### AssetMaintenanceRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `cost` | `Long` | Cout |
| `description` | `String` | Description |
| `maintenanceType` | `AssetMaintenanceType` | Maintenance Type |
| `providerName` | `String` | Fournisseur Nom |
| `scheduledAt` | `Instant` | Planifie le |

#### AssetMaintenanceResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `assetCode` | `String` | Actif immobilise Code |
| `completedAt` | `Instant` | Termine le |
| `cost` | `Long` | Cout |
| `description` | `String` | Description |
| `maintenanceCode` | `String` | Maintenance Code |
| `maintenanceType` | `AssetMaintenanceType` | Maintenance Type |
| `providerName` | `String` | Fournisseur Nom |
| `resolution` | `String` | Resolution |
| `scheduledAt` | `Instant` | Planifie le |
| `startedAt` | `Instant` | Date de debut |
| `status` | `AssetMaintenanceStatus` | Statut |

#### AssetRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `assetCode` | `String` | Actif immobilise Code |
| `assetTag` | `String` | Actif immobilise Etiquette |
| `condition` | `AssetCondition` | Etat |
| `itemCode` | `String` | Article Code |
| `locationCode` | `String` | Emplacement Code |
| `notes` | `String` | Notes |
| `purchaseCost` | `Long` | Achat Cout |
| `purchaseDate` | `LocalDate` | Achat Date |
| `residualValue` | `Long` | Residual Valeur |
| `serialNumber` | `String` | Serie Numero |
| `status` | `AssetStatus` | Statut |
| `usefulLifeMonths` | `Integer` | Useful Life Months |
| `warrantyEndDate` | `LocalDate` | Warranty Fin Date |

#### AssetReserveRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `assigneeCode` | `String` | Assigne Code |
| `assigneeType` | `AssetAssigneeType` | Assigne Type |
| `expectedReturnAt` | `Instant` | Expected Return le |
| `notes` | `String` | Notes |
| `reservedBy` | `String` | Reserved By |
| `reservedFrom` | `Instant` | Reserved Depuis |

#### AssetResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `assetCode` | `String` | Actif immobilise Code |
| `assetTag` | `String` | Actif immobilise Etiquette |
| `assignedToCode` | `String` | Assigne Vers Code |
| `assignedToType` | `AssetAssigneeType` | Assigne Vers Type |
| `condition` | `AssetCondition` | Etat |
| `depreciatedValue` | `Long` | Depreciated Valeur |
| `itemCode` | `String` | Article Code |
| `itemName` | `String` | Article Nom |
| `locationCode` | `String` | Emplacement Code |
| `locationName` | `String` | Emplacement Nom |
| `notes` | `String` | Notes |
| `purchaseCost` | `Long` | Achat Cout |
| `purchaseDate` | `LocalDate` | Achat Date |
| `residualValue` | `Long` | Residual Valeur |
| `serialNumber` | `String` | Serie Numero |
| `status` | `AssetStatus` | Statut |
| `usefulLifeMonths` | `Integer` | Useful Life Months |
| `warrantyEndDate` | `LocalDate` | Warranty Fin Date |

#### AssetReturnRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `notes` | `String` | Notes |
| `returnCondition` | `AssetCondition` | Return Etat |
| `returnedBy` | `String` | Returned By |
| `returnPhotoUrl` | `String` | Return Photo URL |

#### InventoryAlertResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `acknowledgedAt` | `Instant` | Confirme le |
| `alertCode` | `String` | Alerte Code |
| `alertType` | `InventoryAlertType` | Alerte Type |
| `assetCode` | `String` | Actif immobilise Code |
| `createdAt` | `Instant` | Date de creation |
| `currentQuantity` | `BigDecimal` | Actuel Quantite |
| `itemCode` | `String` | Article Code |
| `itemName` | `String` | Article Nom |
| `locationCode` | `String` | Emplacement Code |
| `locationName` | `String` | Emplacement Nom |
| `message` | `String` | Message |
| `resolvedAt` | `Instant` | Resolved le |
| `status` | `InventoryAlertStatus` | Statut |
| `thresholdQuantity` | `BigDecimal` | Seuil Quantite |

#### InventoryAnomalyReportResponse (DTO)

| Champ | Type | Libelle FR |
|---|---|---|
| `largeInventoryCountVariances` | `long` | Large Inventaire Nombre Variances |
| `movementsWithNegativeOverride` | `long` | Mouvements With Negatif Derogation |
| `negativeStockAlerts` | `long` | Negatif Stock Alerts |
| `negativeStockLevels` | `long` | Negatif Stock Levels |
| `suspiciousAdjustments` | `long` | Suspicious Adjustments |

#### InventoryCategoryRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `active` | `Boolean` | Actif |
| `code` | `String` | Code |
| `description` | `String` | Description |
| `name` | `String` | Nom |
| `parentCategoryCode` | `String` | Parent Categorie Code |

#### InventoryCategoryResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `active` | `Boolean` | Actif |
| `code` | `String` | Code |
| `description` | `String` | Description |
| `name` | `String` | Nom |
| `parentCategoryCode` | `String` | Parent Categorie Code |
| `parentCategoryName` | `String` | Parent Categorie Nom |

#### InventoryCountCreateRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `createdBy` | `String` | Cree By |
| `locationCode` | `String` | Emplacement Code |
| `notes` | `String` | Notes |

#### InventoryCountItemResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `countedQuantity` | `BigDecimal` | Counted Quantite |
| `expectedQuantity` | `BigDecimal` | Expected Quantite |
| `itemCode` | `String` | Article Code |
| `itemName` | `String` | Article Nom |
| `notes` | `String` | Notes |
| `varianceQuantity` | `BigDecimal` | Variance Quantite |

#### InventoryCountLineRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `countedQuantity` | `BigDecimal` | Counted Quantite |
| `itemCode` | `String` | Article Code |
| `notes` | `String` | Notes |

#### InventoryCountResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `countCode` | `String` | Nombre Code |
| `createdAt` | `Instant` | Date de creation |
| `createdBy` | `String` | Cree By |
| `items` | `List<InventoryCountItemResponse>` | Articles |
| `locationCode` | `String` | Emplacement Code |
| `locationName` | `String` | Emplacement Nom |
| `notes` | `String` | Notes |
| `reviewedAt` | `Instant` | Revise le |
| `startedAt` | `Instant` | Date de debut |
| `status` | `InventoryCountStatus` | Statut |
| `validatedAt` | `Instant` | Validated le |

#### InventoryDashboardResponse (DTO)

| Champ | Type | Libelle FR |
|---|---|---|
| `expiringLots30Days` | `long` | Expiring Lots30 Days |
| `expirySoonAlerts` | `long` | Expiration Soon Alerts |
| `lowStockAlerts` | `long` | Low Stock Alerts |
| `negativeStockAlerts` | `long` | Negatif Stock Alerts |
| `openAlerts` | `long` | Open Alerts |
| `openPurchaseOrders` | `long` | Open Achat Orders |
| `outOfStockAlerts` | `long` | Out Of Stock Alerts |
| `outOfStockLevels` | `long` | Out Of Stock Levels |
| `totalStockValue` | `Long` | Total Stock Valeur |

#### InventoryImportResponse (DTO)

| Champ | Type | Libelle FR |
|---|---|---|
| `dryRun` | `boolean` | Dry Run |
| `errorCount` | `int` | Erreur Nombre |
| `fileName` | `String` | Fichier Nom |
| `importType` | `InventoryImportType` | Import Type |
| `rows` | `List<InventoryImportRowResult>` | Rows |
| `successCount` | `int` | Success Nombre |
| `totalRows` | `int` | Total Rows |

#### InventoryImportRowResult (DTO)

| Champ | Type | Libelle FR |
|---|---|---|
| `message` | `String` | Message |
| `referenceCode` | `String` | Reference Code |
| `rowNumber` | `int` | Row Numero |
| `success` | `boolean` | Success |

#### InventoryItemRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `active` | `Boolean` | Actif |
| `allowNegativeStock` | `Boolean` | Autoriser Negatif Stock |
| `categoryCode` | `String` | Categorie Code |
| `defaultCost` | `Long` | Default Cout |
| `description` | `String` | Description |
| `identificationCode` | `String` | Identification Code |
| `itemType` | `InventoryItemType` | Article Type |
| `name` | `String` | Nom |
| `psku` | `String` | Psku |
| `requiresExpiryDate` | `Boolean` | Requires Expiration Date |
| `requiresLotNumber` | `Boolean` | Requires Lot Numero |
| `requiresSerialNumber` | `Boolean` | Requires Serie Numero |
| `salePrice` | `Long` | Sale Prix |
| `specification` | `String` | Specification |
| `taxable` | `Boolean` | Taxable |
| `trackingType` | `InventoryTrackingType` | Tracking Type |
| `unitCode` | `String` | Unite Code |

#### InventoryItemResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `active` | `Boolean` | Actif |
| `allowNegativeStock` | `Boolean` | Autoriser Negatif Stock |
| `categoryCode` | `String` | Categorie Code |
| `categoryName` | `String` | Categorie Nom |
| `defaultCost` | `Long` | Default Cout |
| `description` | `String` | Description |
| `displayCode` | `String` | Affichage Code |
| `identificationCode` | `String` | Identification Code |
| `itemCode` | `String` | Article Code |
| `itemType` | `InventoryItemType` | Article Type |
| `name` | `String` | Nom |
| `psku` | `String` | Psku |
| `requiresExpiryDate` | `Boolean` | Requires Expiration Date |
| `requiresLotNumber` | `Boolean` | Requires Lot Numero |
| `requiresSerialNumber` | `Boolean` | Requires Serie Numero |
| `salePrice` | `Long` | Sale Prix |
| `shortCode` | `String` | Short Code |
| `specification` | `String` | Specification |
| `taxable` | `Boolean` | Taxable |
| `trackingType` | `InventoryTrackingType` | Tracking Type |
| `unitCode` | `String` | Unite Code |
| `unitName` | `String` | Unite Nom |

#### InventoryLabelResponse (DTO)

| Champ | Type | Libelle FR |
|---|---|---|
| `barcodeValue` | `String` | Code-barres Valeur |
| `code` | `String` | Code |
| `displayText` | `String` | Affichage Text |
| `labelType` | `String` | Libelle Type |
| `qrValue` | `String` | Qr Valeur |

#### InventoryLocationRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `active` | `Boolean` | Actif |
| `businessCode` | `String` | Entreprise Code |
| `description` | `String` | Description |
| `locationType` | `InventoryLocationType` | Emplacement Type |
| `name` | `String` | Nom |
| `parentLocationCode` | `String` | Parent Emplacement Code |

#### InventoryLocationResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `active` | `Boolean` | Actif |
| `businessCode` | `String` | Entreprise Code |
| `description` | `String` | Description |
| `locationCode` | `String` | Emplacement Code |
| `locationType` | `InventoryLocationType` | Emplacement Type |
| `name` | `String` | Nom |
| `parentLocationCode` | `String` | Parent Emplacement Code |

#### InventoryMovementReportResponse (DTO)

| Champ | Type | Libelle FR |
|---|---|---|
| `firstMovementAt` | `String` | Premier Mouvement le |
| `fromDate` | `String` | Depuis Date |
| `generatedAt` | `String` | Generated le |
| `itemCode` | `String` | Article Code |
| `itemCode` | `String` | Article Code |
| `itemCode` | `String` | Article Code |
| `itemName` | `String` | Article Nom |
| `itemName` | `String` | Article Nom |
| `items` | `List<ItemLine>` | Articles |
| `lastMovementAt` | `String` | Dernier Mouvement le |
| `lines` | `List<Line>` | Lines |
| `locationCode` | `String` | Emplacement Code |
| `locationCode` | `String` | Emplacement Code |
| `locationFromCode` | `String` | Emplacement Depuis Code |
| `locationName` | `String` | Emplacement Nom |
| `locations` | `List<LocationLine>` | Locations |
| `locationToCode` | `String` | Emplacement Vers Code |
| `movementCode` | `String` | Mouvement Code |
| `movementCount` | `long` | Mouvement Nombre |
| `movementCount` | `long` | Mouvement Nombre |
| `movementCount` | `long` | Mouvement Nombre |
| `movementCount` | `long` | Mouvement Nombre |
| `movementType` | `StockMovementType` | Mouvement Type |
| `movementType` | `StockMovementType` | Mouvement Type |
| `netQuantity` | `BigDecimal` | Net Quantite |
| `netQuantity` | `BigDecimal` | Net Quantite |
| `netQuantity` | `BigDecimal` | Net Quantite |
| `netValue` | `Long` | Net Valeur |
| `performedAt` | `String` | Performed le |
| `performedBy` | `String` | Performed By |
| `quantity` | `BigDecimal` | Quantite |
| `recentMovements` | `List<MovementDetail>` | Recent Mouvements |
| `referenceCode` | `String` | Reference Code |
| `referenceType` | `String` | Reference Type |
| `signedQuantity` | `BigDecimal` | Signed Quantite |
| `signedValue` | `Long` | Signed Valeur |
| `summary` | `Summary` | Summary |
| `toDate` | `String` | Vers Date |
| `totalCost` | `Long` | Total Cout |
| `totalInQuantity` | `BigDecimal` | Total In Quantite |
| `totalInQuantity` | `BigDecimal` | Total In Quantite |
| `totalInQuantity` | `BigDecimal` | Total In Quantite |
| `totalInValue` | `Long` | Total In Valeur |
| `totalMovementValue` | `Long` | Total Mouvement Valeur |
| `totalOutQuantity` | `BigDecimal` | Total Out Quantite |
| `totalOutQuantity` | `BigDecimal` | Total Out Quantite |
| `totalOutQuantity` | `BigDecimal` | Total Out Quantite |
| `totalOutValue` | `Long` | Total Out Valeur |
| `totalQuantity` | `BigDecimal` | Total Quantite |
| `totalValue` | `Long` | Total Valeur |
| `totalValue` | `Long` | Total Valeur |
| `totalValue` | `Long` | Total Valeur |
| `unitCost` | `Long` | Unite Cout |

#### InventoryReorderRuleRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `active` | `Boolean` | Actif |
| `itemCode` | `String` | Article Code |
| `locationCode` | `String` | Emplacement Code |
| `maxQuantity` | `BigDecimal` | Maximum Quantite |
| `minQuantity` | `BigDecimal` | Minimum Quantite |
| `preferredSupplierCode` | `String` | Preferred Fournisseur Code |
| `reorderQuantity` | `BigDecimal` | Reapprovisionnement Quantite |

#### InventoryReorderRuleResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `active` | `Boolean` | Actif |
| `id` | `Long` | Identifiant |
| `itemCode` | `String` | Article Code |
| `itemName` | `String` | Article Nom |
| `locationCode` | `String` | Emplacement Code |
| `locationName` | `String` | Emplacement Nom |
| `maxQuantity` | `BigDecimal` | Maximum Quantite |
| `minQuantity` | `BigDecimal` | Minimum Quantite |
| `preferredSupplierCode` | `String` | Preferred Fournisseur Code |
| `reorderQuantity` | `BigDecimal` | Reapprovisionnement Quantite |

#### InventoryUnitConversionRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `factor` | `BigDecimal` | Factor |
| `fromUnitCode` | `String` | Depuis Unite Code |
| `itemCode` | `String` | Article Code |
| `toUnitCode` | `String` | Vers Unite Code |

#### InventoryUnitConversionResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `factor` | `BigDecimal` | Factor |
| `fromUnitCode` | `String` | Depuis Unite Code |
| `itemCode` | `String` | Article Code |
| `toUnitCode` | `String` | Vers Unite Code |

#### InventoryUnitRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `active` | `Boolean` | Actif |
| `code` | `String` | Code |
| `name` | `String` | Nom |
| `unitType` | `InventoryUnitType` | Unite Type |

#### InventoryUnitResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `active` | `Boolean` | Actif |
| `code` | `String` | Code |
| `name` | `String` | Nom |
| `unitType` | `InventoryUnitType` | Unite Type |

#### ProcurementDtos (DTO)

| Champ | Type | Libelle FR |
|---|---|---|
| `active` | `Boolean` | Actif |
| `active` | `Boolean` | Actif |
| `address` | `String` | Adresse |
| `address` | `String` | Adresse |
| `approvalLevel` | `PurchaseApprovalLevel` | Approbation Niveau |
| `approvalLevel` | `PurchaseApprovalLevel` | Approbation Niveau |
| `approvalLevel` | `PurchaseApprovalLevel` | Approbation Niveau |
| `approvalLevel` | `PurchaseApprovalLevel` | Approbation Niveau |
| `approvalSteps` | `List<PurchaseApprovalStepResponse>` | Approbation Etapes |
| `approvedAt` | `Instant` | Approuve le |
| `approvedAt` | `Instant` | Approuve le |
| `approvedBy` | `String` | Approuve By |
| `approvedBy` | `String` | Approuve By |
| `approvedBy` | `String` | Approuve By |
| `averageDeliveryDelayDays` | `Double` | Moyen Livraison Delay Days |
| `averageUnitCost` | `Long` | Moyen Unite Cout |
| `backorderQuantity` | `BigDecimal` | Reliquat Quantite |
| `completeReceiptCount` | `long` | Complet Reception Nombre |
| `completeReceiptRate` | `double` | Complet Reception Taux |
| `createdAt` | `Instant` | Date de creation |
| `createdAt` | `Instant` | Date de creation |
| `deliveryNoteDocumentCode` | `String` | Livraison Note Document Code |
| `deliveryNoteDocumentCode` | `String` | Livraison Note Document Code |
| `email` | `String` | E-mail |
| `email` | `String` | E-mail |
| `expectedDeliveryDate` | `LocalDate` | Expected Livraison Date |
| `expectedDeliveryDate` | `LocalDate` | Expected Livraison Date |
| `expiryDate` | `LocalDate` | Expiration Date |
| `expiryDate` | `LocalDate` | Expiration Date |
| `invoiceDocumentCode` | `String` | Facture Document Code |
| `invoiceDocumentCode` | `String` | Facture Document Code |
| `itemCode` | `String` | Article Code |
| `itemCode` | `String` | Article Code |
| `itemCode` | `String` | Article Code |
| `itemName` | `String` | Article Nom |
| `lines` | `List<GoodsReceiptLineRequest>` | Lines |
| `lines` | `List<PurchaseLineRequest>` | Lines |
| `lines` | `List<PurchaseLineRequest>` | Lines |
| `lines` | `List<ProcurementLineResponse>` | Lines |
| `lines` | `List<ProcurementLineResponse>` | Lines |
| `lines` | `List<ProcurementLineResponse>` | Lines |
| `locationCode` | `String` | Emplacement Code |
| `locationCode` | `String` | Emplacement Code |
| `locationCode` | `String` | Emplacement Code |
| `locationCode` | `String` | Emplacement Code |
| `locationCode` | `String` | Emplacement Code |
| `locationCode` | `String` | Emplacement Code |
| `lotNumber` | `String` | Lot Numero |
| `lotNumber` | `String` | Lot Numero |
| `maxAmount` | `Long` | Maximum Montant |
| `maxAmount` | `Long` | Maximum Montant |
| `minAmount` | `Long` | Minimum Montant |
| `minAmount` | `Long` | Minimum Montant |
| `name` | `String` | Nom |
| `name` | `String` | Nom |
| `orderCode` | `String` | Commande Code |
| `orderCode` | `String` | Commande Code |
| `orderCode` | `String` | Commande Code |
| `orderedBy` | `String` | Ordered By |
| `orderedBy` | `String` | Ordered By |
| `ownerCode` | `String` | Code proprietaire |
| `ownershipType` | `StockOwnershipType` | Ownership Type |
| `phone` | `String` | Telephone |
| `phone` | `String` | Telephone |
| `postedAt` | `Instant` | Comptabilise le |
| `proofDocumentCode` | `String` | Proof Document Code |
| `proofDocumentCode` | `String` | Proof Document Code |
| `purchaseOrderCount` | `long` | Achat Commande Nombre |
| `qualityAccepted` | `Boolean` | Quality Accepted |
| `qualityAccepted` | `Boolean` | Quality Accepted |
| `quantity` | `BigDecimal` | Quantite |
| `quantity` | `BigDecimal` | Quantite |
| `quarantineAcceptedStock` | `Boolean` | Quarantine Accepted Stock |
| `quarantineReason` | `String` | Quarantine Motif |
| `receiptCode` | `String` | Reception Code |
| `receivedAt` | `Instant` | Recu le |
| `receivedBy` | `String` | Recu By |
| `receivedBy` | `String` | Recu By |
| `receivedOrderCount` | `long` | Recu Commande Nombre |
| `receivedQuantity` | `BigDecimal` | Recu Quantite |
| `receivedQuantity` | `BigDecimal` | Recu Quantite |
| `rejectedQuantity` | `BigDecimal` | Rejected Quantite |
| `rejectedQuantity` | `BigDecimal` | Rejected Quantite |
| `rejectionReason` | `String` | Rejet Motif |
| `rejectionReason` | `String` | Rejet Motif |
| `rejectionReason` | `String` | Rejet Motif |
| `requestCode` | `String` | Demande Code |
| `requestedBy` | `String` | Requested By |
| `requestedBy` | `String` | Requested By |
| `requiredApprovals` | `Integer` | Obligatoire Approvals |
| `requiredApprovals` | `Integer` | Obligatoire Approvals |
| `sourceRequestCode` | `String` | Source Demande Code |
| `sourceRequestCode` | `String` | Source Demande Code |
| `status` | `SupplierStatus` | Statut |
| `status` | `PurchaseOrderStatus` | Statut |
| `status` | `PurchaseRequestStatus` | Statut |
| `status` | `SupplierStatus` | Statut |
| `status` | `GoodsReceiptStatus` | Statut |
| `supplierCode` | `String` | Fournisseur Code |
| `supplierCode` | `String` | Fournisseur Code |
| `supplierCode` | `String` | Fournisseur Code |
| `supplierCode` | `String` | Fournisseur Code |
| `supplierCode` | `String` | Fournisseur Code |
| `supplierName` | `String` | Fournisseur Nom |
| `supplierName` | `String` | Fournisseur Nom |
| `taxId` | `String` | Taxe Identifiant |
| `taxId` | `String` | Taxe Identifiant |
| `totalAmount` | `Long` | Montant total |
| `totalCost` | `Long` | Total Cout |
| `unitCost` | `Long` | Unite Cout |
| `unitCost` | `Long` | Unite Cout |
| `unitCost` | `Long` | Unite Cout |

#### PurchaseRequest (DTO)

| Champ | Type | Libelle FR |
|---|---|---|
| `approvedBy` | `String` | Approuve By |
| `createdAt` | `Instant` | Date de creation |
| `id` | `Long` | Identifiant |
| `lines` | `List<PurchaseRequestLine>` | Lines |
| `location` | `InventoryLocation` | Emplacement |
| `rejectionReason` | `String` | Rejet Motif |
| `requestCode` | `String` | Demande Code |
| `requestedBy` | `String` | Requested By |
| `status` | `PurchaseRequestStatus` | Statut |
| `updatedAt` | `Instant` | Date de mise a jour |

#### ReorderSuggestionResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `categoryCode` | `String` | Categorie Code |
| `categoryName` | `String` | Categorie Nom |
| `currentQuantity` | `BigDecimal` | Actuel Quantite |
| `estimatedOrderCost` | `Long` | Estimated Commande Cout |
| `estimatedUnitCost` | `Long` | Estimated Unite Cout |
| `itemCode` | `String` | Article Code |
| `itemName` | `String` | Article Nom |
| `itemType` | `InventoryItemType` | Article Type |
| `locationCode` | `String` | Emplacement Code |
| `locationName` | `String` | Emplacement Nom |
| `maxQuantity` | `BigDecimal` | Maximum Quantite |
| `minQuantity` | `BigDecimal` | Minimum Quantite |
| `preferredSupplierCode` | `String` | Preferred Fournisseur Code |
| `priorityScore` | `Integer` | Priorite Score |
| `quantityOnHand` | `BigDecimal` | Quantite On Hand |
| `quantityReserved` | `BigDecimal` | Quantite Reserved |
| `reason` | `String` | Motif |
| `reasonCode` | `ReorderSuggestionReason` | Motif Code |
| `reorderQuantity` | `BigDecimal` | Reapprovisionnement Quantite |
| `ruleScope` | `String` | Rule Scope |
| `severity` | `ReorderSuggestionSeverity` | Severity |
| `shortageQuantity` | `BigDecimal` | Shortage Quantite |
| `targetQuantity` | `BigDecimal` | Cible Quantite |
| `unitCode` | `String` | Unite Code |
| `unitName` | `String` | Unite Nom |

#### StockAdjustmentRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `allowNegativeOverride` | `Boolean` | Autoriser Negatif Derogation |
| `expiryDate` | `LocalDate` | Expiration Date |
| `itemCode` | `String` | Article Code |
| `locationCode` | `String` | Emplacement Code |
| `lotNumber` | `String` | Lot Numero |
| `performedBy` | `String` | Performed By |
| `quantityDelta` | `BigDecimal` | Quantite Delta |
| `reason` | `String` | Motif |
| `referenceCode` | `String` | Reference Code |
| `referenceType` | `StockReferenceType` | Reference Type |
| `unitCost` | `Long` | Unite Cout |

#### StockInRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `assetSerialNumbers` | `List<String>` | Actif immobilise Serie Numbers |
| `assetTags` | `List<String>` | Actif immobilise Etiquettes |
| `expiryDate` | `LocalDate` | Expiration Date |
| `itemCode` | `String` | Article Code |
| `locationCode` | `String` | Emplacement Code |
| `lotNumber` | `String` | Lot Numero |
| `ownerCode` | `String` | Code proprietaire |
| `ownershipType` | `StockOwnershipType` | Ownership Type |
| `performedBy` | `String` | Performed By |
| `quantity` | `BigDecimal` | Quantite |
| `quarantined` | `Boolean` | Quarantined |
| `quarantineReason` | `String` | Quarantine Motif |
| `reason` | `String` | Motif |
| `referenceCode` | `String` | Reference Code |
| `referenceType` | `StockReferenceType` | Reference Type |
| `unitCost` | `Long` | Unite Cout |

#### StockLevelResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `averageCost` | `Long` | Moyen Cout |
| `itemCode` | `String` | Article Code |
| `itemName` | `String` | Article Nom |
| `lastMovementAt` | `Instant` | Dernier Mouvement le |
| `locationCode` | `String` | Emplacement Code |
| `locationName` | `String` | Emplacement Nom |
| `quantityAvailable` | `BigDecimal` | Quantite Disponible |
| `quantityOnHand` | `BigDecimal` | Quantite On Hand |
| `quantityReserved` | `BigDecimal` | Quantite Reserved |

#### StockLotResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `active` | `Boolean` | Actif |
| `expiryDate` | `LocalDate` | Expiration Date |
| `initialQuantity` | `BigDecimal` | Initial Quantite |
| `itemCode` | `String` | Article Code |
| `itemName` | `String` | Article Nom |
| `locationCode` | `String` | Emplacement Code |
| `locationName` | `String` | Emplacement Nom |
| `lotNumber` | `String` | Lot Numero |
| `ownerCode` | `String` | Code proprietaire |
| `ownershipType` | `StockOwnershipType` | Ownership Type |
| `quarantined` | `Boolean` | Quarantined |
| `quarantineReason` | `String` | Quarantine Motif |
| `receivedAt` | `Instant` | Recu le |
| `remainingQuantity` | `BigDecimal` | Restant Quantite |

#### StockMovementLotResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `expiryDate` | `LocalDate` | Expiration Date |
| `lotNumber` | `String` | Lot Numero |
| `quantity` | `BigDecimal` | Quantite |

#### StockMovementResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `allowNegativeOverride` | `Boolean` | Autoriser Negatif Derogation |
| `itemCode` | `String` | Article Code |
| `itemName` | `String` | Article Nom |
| `locationFromCode` | `String` | Emplacement Depuis Code |
| `locationToCode` | `String` | Emplacement Vers Code |
| `lots` | `List<StockMovementLotResponse>` | Lots |
| `movementCode` | `String` | Mouvement Code |
| `movementType` | `StockMovementType` | Mouvement Type |
| `originalMovementCode` | `String` | Original Mouvement Code |
| `performedAt` | `Instant` | Performed le |
| `performedBy` | `String` | Performed By |
| `quantity` | `BigDecimal` | Quantite |
| `reason` | `String` | Motif |
| `referenceCode` | `String` | Reference Code |
| `referenceType` | `StockReferenceType` | Reference Type |
| `reversalMovementCode` | `String` | Reversal Mouvement Code |
| `reversalOfMovementCode` | `String` | Reversal Of Mouvement Code |
| `reversalReason` | `String` | Reversal Motif |
| `reversed` | `Boolean` | Reversed |
| `reversedAt` | `Instant` | Reversed le |
| `reversedBy` | `String` | Reversed By |
| `totalCost` | `Long` | Total Cout |
| `unitCost` | `Long` | Unite Cout |

#### StockOutRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `allowNegativeOverride` | `Boolean` | Autoriser Negatif Derogation |
| `itemCode` | `String` | Article Code |
| `locationCode` | `String` | Emplacement Code |
| `performedBy` | `String` | Performed By |
| `quantity` | `BigDecimal` | Quantite |
| `reason` | `String` | Motif |
| `referenceCode` | `String` | Reference Code |
| `referenceType` | `StockReferenceType` | Reference Type |

#### StockPickingSuggestionResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `availableQuantity` | `BigDecimal` | Disponible Quantite |
| `fullyCovered` | `Boolean` | Fully Covered |
| `itemCode` | `String` | Article Code |
| `lines` | `List<Line>` | Lines |
| `locationCode` | `String` | Emplacement Code |
| `locationName` | `String` | Emplacement Nom |
| `pickQuantity` | `BigDecimal` | Pick Quantite |
| `requestedQuantity` | `BigDecimal` | Requested Quantite |
| `suggestedQuantity` | `BigDecimal` | Suggested Quantite |

#### StockReservationRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `expiresAt` | `Instant` | Date d expiration |
| `itemCode` | `String` | Article Code |
| `locationCode` | `String` | Emplacement Code |
| `quantity` | `BigDecimal` | Quantite |
| `referenceCode` | `String` | Reference Code |
| `referenceType` | `StockReferenceType` | Reference Type |
| `reservedBy` | `String` | Reserved By |

#### StockReservationResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `closedAt` | `Instant` | Ferme le |
| `expiresAt` | `Instant` | Date d expiration |
| `itemCode` | `String` | Article Code |
| `itemName` | `String` | Article Nom |
| `locationCode` | `String` | Emplacement Code |
| `locationName` | `String` | Emplacement Nom |
| `quantity` | `BigDecimal` | Quantite |
| `referenceCode` | `String` | Reference Code |
| `referenceType` | `StockReferenceType` | Reference Type |
| `reservationCode` | `String` | Reservation Code |
| `reservedAt` | `Instant` | Reserved le |
| `reservedBy` | `String` | Reserved By |
| `status` | `StockReservationStatus` | Statut |

#### StockTransferRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `allowNegativeOverride` | `Boolean` | Autoriser Negatif Derogation |
| `fromLocationCode` | `String` | Depuis Emplacement Code |
| `itemCode` | `String` | Article Code |
| `performedBy` | `String` | Performed By |
| `quantity` | `BigDecimal` | Quantite |
| `reason` | `String` | Motif |
| `referenceCode` | `String` | Reference Code |
| `referenceType` | `StockReferenceType` | Reference Type |
| `toLocationCode` | `String` | Vers Emplacement Code |

#### StockTransferWorkflowRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `fromLocationCode` | `String` | Depuis Emplacement Code |
| `itemCode` | `String` | Article Code |
| `quantity` | `BigDecimal` | Quantite |
| `reason` | `String` | Motif |
| `requestedBy` | `String` | Requested By |
| `toLocationCode` | `String` | Vers Emplacement Code |

#### StockTransferWorkflowResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `approvedAt` | `Instant` | Approuve le |
| `approvedBy` | `String` | Approuve By |
| `fromLocationCode` | `String` | Depuis Emplacement Code |
| `itemCode` | `String` | Article Code |
| `itemName` | `String` | Article Nom |
| `movementCode` | `String` | Mouvement Code |
| `quantity` | `BigDecimal` | Quantite |
| `reason` | `String` | Motif |
| `receivedAt` | `Instant` | Recu le |
| `receivedBy` | `String` | Recu By |
| `requestedAt` | `Instant` | Requested le |
| `requestedBy` | `String` | Requested By |
| `shippedAt` | `Instant` | Shipped le |
| `shippedBy` | `String` | Shipped By |
| `status` | `StockTransferWorkflowStatus` | Statut |
| `toLocationCode` | `String` | Vers Emplacement Code |
| `transferCode` | `String` | Transfert Code |

### Noyau / Transversal

#### AddressRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `city` | `String` | Ville |
| `countryCode` | `String` | Pays Code |
| `district` | `String` | Quartier |
| `streetName` | `String` | Rue Nom |
| `streetNumber` | `String` | Rue Numero |

#### AddressResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `city` | `String` | Ville |
| `country` | `String` | Pays |
| `district` | `String` | Quartier |
| `fullAddress` | `String` | Full Adresse |
| `streetName` | `String` | Rue Nom |
| `streetNumber` | `String` | Rue Numero |

#### ApiResponse (DTO)

| Champ | Type | Libelle FR |
|---|---|---|
| `data` | `T` | Donnees |
| `errorCode` | `String` | Erreur Code |
| `errorDescription` | `String` | Erreur Description |
| `message` | `String` | Message |
| `success` | `Boolean` | Success |
| `timestamp` | `LocalDateTime` | Horodatage |

#### CountryRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `countryCode` | `String` | Pays Code |
| `currencyCode` | `String` | Devise Code |
| `isOhadaMember` | `Boolean` | Is Ohada Membre |
| `name` | `String` | Nom |
| `phoneCode` | `String` | Telephone Code |

#### CountryResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `countryCode` | `String` | Pays Code |
| `defaultCurrency` | `CurrencyResponse` | Default Devise |
| `isOhadaMember` | `boolean` | Is Ohada Membre |
| `name` | `String` | Nom |
| `phoneCode` | `String` | Telephone Code |

#### CurrencyRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `currencyCode` | `String` | Devise Code |
| `currencyName` | `String` | Devise Nom |

#### CurrencyResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `currencyCode` | `String` | Devise Code |
| `currencyName` | `String` | Devise Nom |

#### ErrorResponse (DTO)

| Champ | Type | Libelle FR |
|---|---|---|
| `isDevEnvironment` | `boolean` | Is Dev Environment |

#### MemberSettingsResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `billingPref` | `BillingPreferencesResponse` | Facturation Preference |
| `langage` | `String` | Langage |
| `notificationPref` | `List<NotificationPreferenceResponse>` | Notification Preference |
| `showEmail` | `Boolean` | Show E-mail |
| `showNameOnDisplay` | `Boolean` | Show Nom On Affichage |
| `showPhone` | `Boolean` | Show Telephone |
| `timeZone` | `String` | Heure Zone |
| `walletPref` | `WalletPreferencesResponse` | Portefeuille Preference |

#### PaginatedResponse (DTO)

| Champ | Type | Libelle FR |
|---|---|---|
| `data` | `List<T>` | Donnees |
| `pageable` | `PageInfo` | Pagination |

#### SequenceCode (DTO)

| Champ | Type | Libelle FR |
|---|---|---|
| `patternKey` | `String` | Pattern Key |

#### UpdateMemberSettingsRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `billingPref` | `BillingPreferences` | Facturation Preference |
| `langage` | `String` | Langage |
| `notificationPref` | `List<NotificationPreferences>` | Notification Preference |
| `showEmail` | `Boolean` | Show E-mail |
| `showNameOnDisplay` | `Boolean` | Show Nom On Affichage |
| `showPhone` | `Boolean` | Show Telephone |
| `timeZone` | `String` | Heure Zone |
| `walletPref` | `WalletPreferencesRequest` | Portefeuille Preference |

#### UserSessionResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `createdAt` | `Instant` | Date de creation |
| `deviceType` | `String` | Device Type |
| `email` | `String` | E-mail |
| `expiresIn` | `String` | Expire In |
| `ipAddress` | `String` | Ip Adresse |
| `lastAccessAt` | `Instant` | Dernier Acces le |
| `locationGuess` | `String` | Emplacement Guess |
| `role` | `String` | Role |
| `sessionId` | `String` | Session Identifiant |
| `status` | `String` | Statut |
| `userAgent` | `String` | Utilisateur Agent |

#### UserSessionToken (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `accessToken` | `String` | Jeton d acces |
| `refreshToken` | `String` | Refresh Jeton |

### Ressources

#### AmenityResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `active` | `Boolean` | Actif |
| `code` | `String` | Code |
| `description` | `String` | Description |
| `name` | `String` | Nom |

#### ChangeTypeRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `groupId` | `String` | Groupe Identifiant |
| `policyId` | `String` | Policy Identifiant |
| `typeId` | `String` | Type Identifiant |

#### CreateAmenityRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `active` | `Boolean` | Actif |
| `description` | `String` | Description |
| `name` | `String` | Nom |

#### CreateResourceAvailabilityRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `active` | `Boolean` | Actif |
| `capacity` | `Integer` | Capacite |
| `endedAt` | `LocalDateTime` | Date de fin |
| `resourceCode` | `String` | Code ressource |
| `startedAt` | `LocalDateTime` | Date de debut |

#### CreateResourceClosureRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `active` | `Boolean` | Actif |
| `endedAt` | `LocalDateTime` | Date de fin |
| `reason` | `String` | Motif |
| `resourceCode` | `String` | Code ressource |
| `startedAt` | `LocalDateTime` | Date de debut |

#### CreateResourceGroupRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `active` | `Boolean` | Actif |
| `description` | `String` | Description |
| `name` | `String` | Nom |
| `portalVisible` | `Boolean` | Portal Visible |

#### CreateResourcePolicyRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `active` | `Boolean` | Actif |
| `allowCancellation` | `Boolean` | Autoriser Annulation |
| `cancellationNoticeMinutes` | `Integer` | Preavis d annulation en minutes |
| `description` | `String` | Description |
| `maxBookingDurationMinutes` | `Integer` | Duree maximale de reservation en minutes |
| `minBookingDurationMinutes` | `Integer` | Duree minimale de reservation en minutes |
| `minBookingNoticeMinutes` | `Integer` | Preavis minimum de reservation en minutes |
| `name` | `String` | Nom |

#### CreateResourcePricingRuleRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `active` | `Boolean` | Actif |
| `bookingUnit` | `String` | Reservation Unite |
| `price` | `Integer` | Prix |
| `resourceCode` | `String` | Code ressource |

#### CreateResourceTypeRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `active` | `Boolean` | Actif |
| `description` | `String` | Description |
| `name` | `String` | Nom |

#### LinkAmenityToResourceRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `amenityCode` | `String` | Equipement Code |
| `resourceCode` | `String` | Code ressource |

#### ReleaseResourceAvailabilityRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `endedAt` | `LocalDateTime` | Date de fin |
| `quantity` | `Integer` | Quantite |
| `resourceCode` | `String` | Code ressource |
| `startedAt` | `LocalDateTime` | Date de debut |

#### ReserveResourceAvailabilityRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `endedAt` | `LocalDateTime` | Date de fin |
| `quantity` | `Integer` | Quantite |
| `resourceCode` | `String` | Code ressource |
| `startedAt` | `LocalDateTime` | Date de debut |

#### ResourceAvailabilityGroupResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `availabilities` | `List<ResourceAvailabilityResponse>` | Disponibilites |
| `resourceCode` | `String` | Code ressource |
| `resourceName` | `String` | Nom de la ressource |

#### ResourceAvailabilityResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `active` | `Boolean` | Actif |
| `available` | `Boolean` | Disponible |
| `endedAt` | `LocalDateTime` | Date de fin |
| `id` | `Long` | Identifiant |
| `remainingCapacity` | `Integer` | Capacite restante |
| `resourceCode` | `String` | Code ressource |
| `slotDurationMinutes` | `Integer` | Creneau Duree Minutes |
| `startedAt` | `LocalDateTime` | Date de debut |
| `totalCapacity` | `Integer` | Capacite totale |

#### ResourceAvailabilityWindowResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `durationMinutes` | `Integer` | Duree en minutes |
| `endedAt` | `LocalDateTime` | Date de fin |
| `remainingCapacity` | `Integer` | Capacite restante |
| `resourceCode` | `String` | Code ressource |
| `slotCount` | `Integer` | Nombre de creneaux |
| `startedAt` | `LocalDateTime` | Date de debut |

#### ResourceClosureResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `active` | `Boolean` | Actif |
| `endedAt` | `LocalDateTime` | Date de fin |
| `id` | `Long` | Identifiant |
| `resourceCode` | `String` | Code ressource |
| `startedAt` | `LocalDateTime` | Date de debut |

#### ResourceGroupResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `active` | `Boolean` | Actif |
| `code` | `String` | Code |
| `description` | `String` | Description |
| `name` | `String` | Nom |
| `portalVisible` | `Boolean` | Portal Visible |

#### ResourcePolicyResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `active` | `Boolean` | Actif |
| `allowCancellation` | `Boolean` | Autoriser Annulation |
| `cancellationNoticeMinutes` | `Integer` | Preavis d annulation en minutes |
| `code` | `String` | Code |
| `description` | `String` | Description |
| `maxBookingDurationMinutes` | `Integer` | Duree maximale de reservation en minutes |
| `minBookingDurationMinutes` | `Integer` | Duree minimale de reservation en minutes |
| `minBookingNoticeMinutes` | `Integer` | Preavis minimum de reservation en minutes |
| `name` | `String` | Nom |

#### ResourcePricingRuleResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `active` | `Boolean` | Actif |
| `bookingUnit` | `String` | Reservation Unite |
| `id` | `Long` | Identifiant |
| `price` | `Integer` | Prix |
| `resourceCode` | `String` | Code ressource |

#### ResourceRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `bookingEnabled` | `Boolean` | Reservation Active |
| `capacity` | `Integer` | Capacite |
| `description` | `String` | Description |
| `groupId` | `String` | Groupe Identifiant |
| `locationLabel` | `String` | Emplacement Libelle |
| `name` | `String` | Nom |
| `policyId` | `String` | Policy Identifiant |
| `portalVisible` | `Boolean` | Portal Visible |
| `status` | `String` | Statut |
| `typeId` | `String` | Type Identifiant |
| `zone` | `String` | Zone |

#### ResourceResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `active` | `Boolean` | Actif |
| `bookingEnabled` | `Boolean` | Reservation Active |
| `capacity` | `Integer` | Capacite |
| `code` | `String` | Code |
| `description` | `String` | Description |
| `displayOrder` | `Integer` | Affichage Commande |
| `groupCode` | `String` | Groupe Code |
| `locationLabel` | `String` | Emplacement Libelle |
| `name` | `String` | Nom |
| `policyCode` | `String` | Policy Code |
| `portalVisible` | `Boolean` | Portal Visible |
| `status` | `String` | Statut |
| `typeCode` | `String` | Type Code |
| `zone` | `String` | Zone |

#### ResourceSummaryResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `active` | `Boolean` | Actif |
| `bookingEnabled` | `Boolean` | Reservation Active |
| `code` | `String` | Code |
| `name` | `String` | Nom |
| `portalVisible` | `Boolean` | Portal Visible |
| `status` | `String` | Statut |
| `typeCode` | `String` | Type Code |

#### ResourceTypeResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `active` | `Boolean` | Actif |
| `code` | `String` | Code |
| `description` | `String` | Description |
| `name` | `String` | Nom |

#### UpdateAmenityRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `active` | `Boolean` | Actif |
| `description` | `String` | Description |
| `name` | `String` | Nom |

#### UpdateResourceGroupRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `active` | `Boolean` | Actif |
| `description` | `String` | Description |
| `name` | `String` | Nom |
| `portalVisible` | `Boolean` | Portal Visible |

#### UpdateResourcePolicyRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `active` | `Boolean` | Actif |
| `allowCancellation` | `Boolean` | Autoriser Annulation |
| `cancellationNoticeMinutes` | `Integer` | Preavis d annulation en minutes |
| `description` | `String` | Description |
| `maxBookingDurationMinutes` | `Integer` | Duree maximale de reservation en minutes |
| `minBookingDurationMinutes` | `Integer` | Duree minimale de reservation en minutes |
| `minBookingNoticeMinutes` | `Integer` | Preavis minimum de reservation en minutes |
| `name` | `String` | Nom |

#### UpdateResourceRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `bookingEnabled` | `Boolean` | Reservation Active |
| `capacity` | `Integer` | Capacite |
| `description` | `String` | Description |
| `floorLabel` | `String` | Floor Libelle |
| `locationLabel` | `String` | Emplacement Libelle |
| `name` | `String` | Nom |
| `portalVisible` | `Boolean` | Portal Visible |
| `zone` | `String` | Zone |

#### UpdateResourceTypeRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `active` | `Boolean` | Actif |
| `description` | `String` | Description |
| `name` | `String` | Nom |

### Securite et authentification

#### AdminCreateUserRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `email` | `String` | E-mail |
| `generatePassword` | `Boolean` | Generate Password |
| `password` | `String` | Password |
| `roleNames` | `List<String>` | Role Names |

#### AdminResetUserPasswordRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `generatePassword` | `Boolean` | Generate Password |
| `newPassword` | `String` | New Password |

#### AdminUpdateUserRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `email` | `String` | E-mail |
| `isAccountEnabled` | `Boolean` | Is Compte Active |
| `isAccountLocked` | `Boolean` | Is Compte Verrouille |
| `roleNames` | `List<String>` | Role Names |

#### AdminUserResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `accountEnabled` | `Boolean` | Compte active |
| `accountExpired` | `Boolean` | Compte expire |
| `accountLocked` | `Boolean` | Compte verrouille |
| `createdAt` | `LocalDateTime` | Date de creation |
| `deleted` | `Boolean` | Supprime |
| `email` | `String` | E-mail |
| `failedLoginAttempts` | `Integer` | Failed Login Attempts |
| `generatedPassword` | `String` | Generated Password |
| `id` | `Long` | Identifiant |
| `lastLogin` | `LocalDateTime` | Dernier Login |
| `roleNames` | `List<String>` | Role Names |
| `updatedAt` | `LocalDateTime` | Date de mise a jour |
| `userId` | `String` | Utilisateur Identifiant |

#### CurrentUserResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `accountEnabled` | `boolean` | Compte active |
| `authorities` | `List<String>` | Autorisations |
| `email` | `String` | E-mail |
| `userId` | `String` | Utilisateur Identifiant |

#### EmailRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `email` | `String` | E-mail |

#### LoginRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `email` | `String` | E-mail |
| `password` | `String` | Password |

#### OttResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `message` | `String` | Message |
| `verificationToken` | `String` | Verification Jeton |

#### PasswordResetConfirmationRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `newPassword` | `String` | New Password |
| `token` | `String` | Jeton |

#### PermissionRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `action` | `String` | Action |
| `displayName` | `String` | Nom d affichage |
| `isActive` | `Boolean` | Is Actif |
| `module` | `String` | Module |
| `name` | `String` | Nom |

#### PermissionResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `displayName` | `String` | Nom d affichage |
| `id` | `Long` | Identifiant |
| `isActive` | `Boolean` | Is Actif |
| `module` | `String` | Module |
| `name` | `String` | Nom |

#### RefreshTokenRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `refreshToken` | `String` | Refresh Jeton |

#### RoleRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `description` | `String` | Description |
| `displayName` | `String` | Nom d affichage |
| `isActive` | `Boolean` | Is Actif |
| `name` | `String` | Nom |

#### RoleResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `description` | `String` | Description |
| `displayName` | `String` | Nom d affichage |
| `id` | `Long` | Identifiant |
| `isActive` | `Boolean` | Is Actif |
| `name` | `String` | Nom |
| `permissions` | `Map<String, String>` | Permissions |

#### SucessOttResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `accessToken` | `String` | Jeton d acces |
| `refreshToken` | `String` | Refresh Jeton |

#### UserRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `email` | `String` | E-mail |
| `generatePassword` | `boolean` | Generate Password |
| `password` | `String` | Password |

#### UserResponse (Reponse)

| Champ | Type | Libelle FR |
|---|---|---|
| `businessEntityCode` | `String` | Entreprise Entity Code |
| `businessEntityName` | `String` | Entreprise Entity Nom |
| `email` | `String` | E-mail |
| `generatedPassword` | `String` | Generated Password |
| `status` | `String` | Statut |
| `userId` | `String` | Utilisateur Identifiant |

#### ValidateOttRequest (Requete)

| Champ | Type | Libelle FR |
|---|---|---|
| `ottToken` | `String` | Ott Jeton |
| `verificationToken` | `String` | Verification Jeton |

## Valeurs enumerees

### Abonnements

#### BenefitCategory

| Valeur API | Libelle FR |
|---|---|
| `ACCESS` | Acces |
| `BOOKING` | Reservation |
| `BUSINESS_SERVICE` | Entreprise Service |
| `COMMUNITY` | Community |
| `CUSTOM` | Custom |
| `DISCOUNT` | Remise |
| `INVENTORY` | Inventaire |
| `SUPPORT` | Support |

#### BillableItemStatus

| Valeur API | Libelle FR |
|---|---|
| `CANCELLED` | Annule |
| `INVOICED` | Invoiced |
| `PENDING` | En attente |
| `SENT_TO_INVOICE` | Envoye Vers Facture |

#### BillingCycle

| Valeur API | Libelle FR |
|---|---|
| `DAILY` | Quotidien |
| `MONTHLY` | Mensuel |
| `ONE_TIME` | One Heure |
| `QUARTERLY` | Quarterly |
| `WEEKLY` | Hebdomadaire |
| `YEARLY` | Annuel |

#### BillingScheduleStatus

| Valeur API | Libelle FR |
|---|---|
| `ACTIVE` | Actif |
| `CANCELLED` | Annule |
| `COMPLETED` | Termine |
| `PAST_DUE` | En retard |
| `PAUSED` | Paused |

#### ConsumptionMode

| Valeur API | Libelle FR |
|---|---|
| `CHECK_ONLY` | Check Only |
| `CONSUMABLE` | Consumable |
| `RESERVABLE` | Reservable |
| `RESERVE_THEN_CONSUME` | Reserver Then Consume |

#### DiscountType

| Valeur API | Libelle FR |
|---|---|
| `FIXED_AMOUNT` | Fixed Montant |
| `FREE_ENTITLEMENT` | Free Droit |
| `FREE_TRIAL_DAYS` | Free Trial Jours |
| `PERCENTAGE` | Pourcentage |
| `WAIVE_SETUP_FEE` | Waive Setup Fee |

#### EntitlementGrantStatus

| Valeur API | Libelle FR |
|---|---|
| `ACTIVE` | Actif |
| `CANCELLED` | Annule |
| `DEPLETED` | Depleted |
| `EXPIRED` | Expire |
| `RESERVED` | Reserved |
| `SUSPENDED` | Suspended |

#### EntitlementReservationStatus

| Valeur API | Libelle FR |
|---|---|
| `ACTIVE` | Actif |
| `CONSUMED` | Consumed |
| `EXPIRED` | Expire |
| `RELEASED` | Released |

#### EntitlementResetPolicy

| Valeur API | Libelle FR |
|---|---|
| `DAILY` | Quotidien |
| `MONTHLY` | Mensuel |
| `NEVER` | Never |
| `PER_BILLING_CYCLE` | Per Facturation Cycle |
| `WEEKLY` | Hebdomadaire |

#### EntitlementTransactionType

| Valeur API | Libelle FR |
|---|---|
| `ADJUST` | Adjust |
| `CANCEL` | Cancel |
| `CONSUME` | Consume |
| `EXPIRE` | Expire |
| `GRANT` | Grant |
| `REFUND` | Remboursement |
| `RELEASE` | Liberer |
| `RESERVE` | Reserver |
| `ROLLOVER` | Report |

#### EntitlementType

| Valeur API | Libelle FR |
|---|---|
| `ACCESS` | Acces |
| `CREDIT` | Credit |
| `DISCOUNT` | Remise |
| `FEATURE_FLAG` | Feature Flag |
| `QUOTA` | Quota |
| `SERVICE_ALLOWANCE` | Service Allowance |
| `TIME` | Heure |

#### EntitlementUnit

| Valeur API | Libelle FR |
|---|---|
| `AMOUNT` | Montant |
| `BOOKING` | Reservation |
| `BOOLEAN` | Boolean |
| `CREDIT` | Credit |
| `DAY` | Jour |
| `HOUR` | Heure |
| `MEMBER` | Membre |
| `PERCENT` | Percent |
| `VISIT` | Visit |

#### OveragePolicyMode

| Valeur API | Libelle FR |
|---|---|
| `ALLOW_UNBILLED` | Autoriser Unbilled |
| `BILLABLE` | Facturable |
| `BLOCK` | Block |

#### PassStatus

| Valeur API | Libelle FR |
|---|---|
| `ACTIVE` | Actif |
| `CANCELLED` | Annule |
| `CONSUMED` | Consumed |
| `DRAFT` | Brouillon |
| `EXPIRED` | Expire |
| `PARTIALLY_USED` | Partially Used |
| `SUSPENDED` | Suspended |

#### PassType

| Valeur API | Libelle FR |
|---|---|
| `COMPANY_SHARED_PASS` | Entreprise Shared Pass |
| `CUSTOM` | Custom |
| `DAY_PASS` | Jour Pass |
| `MEETING_ROOM_PACK` | Meeting Room Pack |
| `PROMOTIONAL_PASS` | Promotional Pass |
| `SUBSCRIPTION_PASS` | Abonnement Pass |
| `TIME_PACK` | Heure Pack |
| `VISITOR_PASS` | Visitor Pass |

#### PlanStatus

| Valeur API | Libelle FR |
|---|---|
| `ACTIVE` | Actif |
| `ARCHIVED` | Archive |
| `DRAFT` | Brouillon |

#### PlanType

| Valeur API | Libelle FR |
|---|---|
| `COMPANY_PLAN` | Entreprise Plan |
| `COWORKING_ACCESS` | Coworking Acces |
| `CUSTOM` | Custom |
| `DEDICATED_DESK` | Dedicated Desk |
| `MEETING_ROOM_PACK` | Meeting Room Pack |
| `MEMBERSHIP` | Membership |
| `PRIVATE_OFFICE` | Private Office |
| `VIRTUAL_OFFICE` | Virtual Office |

#### PromotionStatus

| Valeur API | Libelle FR |
|---|---|
| `ACTIVE` | Actif |
| `ARCHIVED` | Archive |
| `DRAFT` | Brouillon |
| `EXPIRED` | Expire |
| `PAUSED` | Paused |

#### SeatRole

| Valeur API | Libelle FR |
|---|---|
| `ADMIN` | Administrateur |
| `GUEST` | Guest |
| `MEMBER` | Membre |
| `OWNER` | Proprietaire |

#### SeatStatus

| Valeur API | Libelle FR |
|---|---|
| `ACTIVE` | Actif |
| `INVITED` | Invited |
| `REMOVED` | Removed |
| `SUSPENDED` | Suspended |

#### SubscriberType

| Valeur API | Libelle FR |
|---|---|
| `BUSINESS_ENTITY` | Entreprise Entity |
| `CUSTOMER` | Client |
| `MEMBER` | Membre |

#### SubscriptionAddonStatus

| Valeur API | Libelle FR |
|---|---|
| `ACTIVE` | Actif |
| `CANCELLED` | Annule |
| `EXPIRED` | Expire |
| `PENDING` | En attente |

#### SubscriptionChangeEffectivePolicy

| Valeur API | Libelle FR |
|---|---|
| `CUSTOM_DATE` | Custom Date |
| `IMMEDIATE` | Immediate |
| `NEXT_BILLING_PERIOD` | Suivant Facturation Period |

#### SubscriptionChangeStatus

| Valeur API | Libelle FR |
|---|---|
| `APPLIED` | Applique |
| `APPROVED` | Approuve |
| `CANCELLED` | Annule |
| `REJECTED` | Rejete |
| `REQUESTED` | Requested |

#### SubscriptionChangeType

| Valeur API | Libelle FR |
|---|---|
| `ADD_ADDON` | Ajouter Option |
| `DOWNGRADE` | Downgrade |
| `QUANTITY_CHANGE` | Quantite Changement |
| `REMOVE_ADDON` | Retirer Option |
| `UPGRADE` | Upgrade |

#### SubscriptionEventType

| Valeur API | Libelle FR |
|---|---|
| `BILLING_SCHEDULED` | Facturation Planifie |
| `ENTITLEMENTS_GRANTED` | Entitlements Granted |
| `PLAN_CHANGED` | Plan Modifie |
| `SUBSCRIPTION_ACTIVATED` | Abonnement Activated |
| `SUBSCRIPTION_CANCELLED` | Abonnement Annule |
| `SUBSCRIPTION_CREATED` | Abonnement Cree |
| `SUBSCRIPTION_EXPIRED` | Abonnement Expire |
| `SUBSCRIPTION_RENEWED` | Abonnement Renewed |
| `SUBSCRIPTION_SUSPENDED` | Abonnement Suspended |

#### SubscriptionNotificationChannel

| Valeur API | Libelle FR |
|---|---|
| `EMAIL` | E-mail |
| `IN_APP` | Entree App |
| `SMS` | SMS |
| `WEBHOOK` | Webhook |
| `WHATSAPP` | WhatsApp |

#### SubscriptionNotificationStatus

| Valeur API | Libelle FR |
|---|---|
| `CANCELLED` | Annule |
| `DISPATCHED` | Dispatched |
| `FAILED` | Echoue |
| `PENDING` | En attente |

#### SubscriptionNotificationType

| Valeur API | Libelle FR |
|---|---|
| `CONTRACT_EXPIRING` | Contrat Expiring |
| `ENTITLEMENT_LOW_BALANCE` | Droit Faible Solde |
| `FRAUD_REVIEW` | Fraud Review |
| `OVERAGE_BILLED` | Depassement Billed |
| `OVERAGE_WARNING` | Depassement Warning |
| `PAUSE_ENDING` | Pause Ending |
| `PAUSE_STARTED` | Pause Debut |
| `PAYMENT_DUE` | Paiement Echeance |
| `RENEWAL_UPCOMING` | Renouvellement Upcoming |
| `ROLLOVER_APPLIED` | Report Applique |
| `SUBSCRIPTION_CANCELLED` | Abonnement Annule |
| `WALLET_CREDIT_APPLIED` | Portefeuille Credit Applique |

#### SubscriptionStatus

| Valeur API | Libelle FR |
|---|---|
| `ACTIVE` | Actif |
| `CANCELLED` | Annule |
| `DRAFT` | Brouillon |
| `EXPIRED` | Expire |
| `PAST_DUE` | En retard |
| `PENDING_ACTIVATION` | En attente Activation |
| `SUSPENDED` | Suspended |
| `TRIALING` | Trialing |

#### SubscriptionTimelineEventType

| Valeur API | Libelle FR |
|---|---|
| `ADDON_ADDED` | Option Added |
| `ADDON_CANCELLED` | Option Annule |
| `ADDON_EXPIRED` | Option Expire |
| `BILLING_SCHEDULED` | Facturation Planifie |
| `CONTRACT_BOUND` | Contrat Bound |
| `ENTITLEMENTS_GRANTED` | Entitlements Granted |
| `FRAUD_SIGNAL_RAISED` | Fraud Signal Raised |
| `NOTIFICATION_DISPATCHED` | Notification Dispatched |
| `NOTIFICATION_QUEUED` | Notification Queued |
| `OVERAGE_BILLED` | Depassement Billed |
| `PASS_CANCELLED` | Pass Annule |
| `PASS_EXPIRED` | Pass Expire |
| `PASS_ISSUED` | Pass Emission |
| `PAUSE_ENDED` | Pause Fin |
| `PAUSE_STARTED` | Pause Debut |
| `PLAN_CHANGED` | Plan Modifie |
| `PROMOTION_REDEEMED` | Promotion Redeemed |
| `ROLLOVER_APPLIED` | Report Applique |
| `STATUS_CHANGED` | Statut Modifie |
| `SUBSCRIPTION_ACTIVATED` | Abonnement Activated |
| `SUBSCRIPTION_CANCELLED` | Abonnement Annule |
| `SUBSCRIPTION_CREATED` | Abonnement Cree |
| `SUBSCRIPTION_EXPIRED` | Abonnement Expire |
| `SUBSCRIPTION_RENEWED` | Abonnement Renewed |
| `SUBSCRIPTION_SUSPENDED` | Abonnement Suspended |
| `USAGE_RECORDED` | Utilisation Recorded |
| `WALLET_CREDIT_APPLIED` | Portefeuille Credit Applique |

#### TargetAudience

| Valeur API | Libelle FR |
|---|---|
| `BOTH` | Both |
| `COMPANY` | Entreprise |
| `INDIVIDUAL` | Individual |

#### UsageRecordStatus

| Valeur API | Libelle FR |
|---|---|
| `BILLED` | Billed |
| `CANCELLED` | Annule |
| `RECORDED` | Recorded |

### Clients et membres

#### CustomerStatus

| Valeur API | Libelle FR |
|---|---|
| `ACTIVE` | Actif |
| `ARCHIVED` | Archive |
| `INACTIVE` | Inactif |
| `PENDING` | En attente |
| `SUSPENDED` | Suspended |

#### CustomerType

| Valeur API | Libelle FR |
|---|---|
| `COMPANY` | Entreprise |
| `PERSON` | Personne |

#### MemberStatus

| Valeur API | Libelle FR |
|---|---|
| `ACTIVE` | Actif |
| `ARCHIVED` | Archive |
| `INACTIVE` | Inactif |
| `PENDING` | En attente |
| `PENDING_CORRECTION` | En attente Correction |
| `REJECTED` | Rejete |
| `SUSPENDED` | Suspended |
| `UNDER_REVIEW` | Under Review |

### Contrats

#### ContractPartyRole

| Valeur API | Libelle FR |
|---|---|
| `BENEFICIARY` | Beneficiary |
| `BILLING_CONTACT` | Facturation Contact |
| `OPERATOR` | Operator |
| `SIGNATORY` | Signatory |

#### ContractRenewalType

| Valeur API | Libelle FR |
|---|---|
| `AUTO_RENEW` | Automatique Renew |
| `FIXED_TERM` | Fixed Term |
| `NONE` | Aucun |

#### ContractStatus

| Valeur API | Libelle FR |
|---|---|
| `ACTIVE` | Actif |
| `AWAITING_SIGNATURE` | Awaiting Signature |
| `CANCELLED` | Annule |
| `DRAFT` | Brouillon |
| `EXPIRED` | Expire |
| `GENERATED` | Generated |
| `SIGNED` | Signed |
| `SUSPENDED` | Suspended |
| `TERMINATED` | Terminated |
| `UNDER_REVIEW` | Under Review |

### Documents et KYC

#### DocumentAntivirusStatus

| Valeur API | Libelle FR |
|---|---|
| `CLEAN` | Clean |
| `ERROR` | Erreur |
| `INFECTED` | Infected |
| `PENDING` | En attente |

#### DocumentCategory

| Valeur API | Libelle FR |
|---|---|
| `ASSET` | Actif immobilise |
| `FINANCIAL` | Financial |
| `KYC` | KYC |
| `LEGAL` | Legal |
| `OTHER` | Other |
| `SYSTEM` | System |

#### DocumentOwnerType

| Valeur API | Libelle FR |
|---|---|
| `ASSET` | Actif immobilise |
| `BUSINESS` | Entreprise |
| `CONTRACT` | Contrat |
| `CUSTOMER` | Client |
| `INVOICE` | Facture |
| `MEMBER` | Membre |
| `PAYMENT` | Paiement |
| `PROPOSAL` | Proposal |

#### DocumentReviewStatus

| Valeur API | Libelle FR |
|---|---|
| `APPROVED` | Approuve |
| `NEEDS_CORRECTION` | Needs Correction |
| `PENDING` | En attente |
| `REJECTED` | Rejete |

#### DocumentSignatureStatus

| Valeur API | Libelle FR |
|---|---|
| `DECLINED` | Declined |
| `EXPIRED` | Expire |
| `PENDING` | En attente |
| `SIGNED` | Signed |

#### DocumentStatus

| Valeur API | Libelle FR |
|---|---|
| `APPROVED` | Approuve |
| `ARCHIVED` | Archive |
| `DRAFT` | Brouillon |
| `EXPIRED` | Expire |
| `PENDING_REVIEW` | En attente Review |
| `REJECTED` | Rejete |
| `SIGNED` | Signed |
| `SUPERSEDED` | Superseded |
| `UPLOADED` | Uploaded |

#### DocumentVersionUploadStatus

| Valeur API | Libelle FR |
|---|---|
| `DELETED` | Supprime |
| `PENDING_SCAN` | En attente Scan |
| `QUARANTINED` | Quarantined |
| `READY` | Pret |
| `REJECTED` | Rejete |

#### LegalDocumentStatus

| Valeur API | Libelle FR |
|---|---|
| `DRAFT` | Brouillon |
| `EXPIRED` | Expire |
| `REJECTED` | Rejete |
| `SIGNED` | Signed |
| `UPLOADED` | Uploaded |

#### OwnerType

| Valeur API | Libelle FR |
|---|---|
| `BUSINESS` | Entreprise |
| `CONTRACT` | Contrat |
| `CUSTOMER` | Client |
| `MEMBER` | Membre |

### Entreprises

#### LegalForm

| Valeur API | Libelle FR |
|---|---|
| `SA` | Sa |
| `SARL` | Sarl |
| `SARLU` | Sarlu |
| `SAS` | Sas |
| `SASU` | Sasu |
| `SAU` | Sau |
| `SCI` | Sci |
| `SCOOP` | Scoop |
| `SCS` | Scs |
| `SNC` | Snc |
| `SP` | Sp |

#### Status

| Valeur API | Libelle FR |
|---|---|
| `ACTIVE` | Actif |
| `BLOCKED` | Blocked |
| `DELETED` | Supprime |
| `INACTIVE` | Inactif |

### Facturation

#### BillingDiscountType

| Valeur API | Libelle FR |
|---|---|
| `FIXED_AMOUNT` | Fixed Montant |
| `PERCENTAGE` | Pourcentage |

#### BillingDocumentStatus

| Valeur API | Libelle FR |
|---|---|
| `ACCEPTED` | Accepted |
| `CANCELLED` | Annule |
| `CONVERTED` | Converted |
| `DRAFT` | Brouillon |
| `EXPIRED` | Expire |
| `ISSUED` | Emission |
| `OVERDUE` | Overdue |
| `PAID` | Paye |
| `PARTIALLY_PAID` | Partiellement paye |
| `REFUNDED` | Rembourse |
| `REJECTED` | Rejete |
| `SENT` | Envoye |
| `VOIDED` | Voided |
| `WRITTEN_OFF` | Written Off |

#### BillingDocumentType

| Valeur API | Libelle FR |
|---|---|
| `CREDIT_NOTE` | Credit Note |
| `DEBIT_NOTE` | Debit Note |
| `INVOICE` | Facture |
| `PROFORMA_INVOICE` | Proforma Facture |
| `QUOTE` | Quote |

#### BillingLineType

| Valeur API | Libelle FR |
|---|---|
| `ADDON` | Option |
| `ADJUSTMENT` | Ajustement |
| `BOOKING` | Reservation |
| `OVERAGE` | Depassement |
| `PASS` | Pass |
| `PENALTY` | Penalty |
| `PRODUCT` | Product |
| `SERVICE` | Service |
| `SUBSCRIPTION` | Abonnement |
| `WALLET` | Portefeuille |

### Inventaire

#### AssetAssigneeType

| Valeur API | Libelle FR |
|---|---|
| `BUSINESS` | Entreprise |
| `CUSTOMER` | Client |
| `MEMBER` | Membre |
| `RESOURCE` | Ressource |
| `USER` | Utilisateur |

#### AssetAssignmentStatus

| Valeur API | Libelle FR |
|---|---|
| `ACTIVE` | Actif |
| `CANCELLED` | Annule |
| `RESERVED` | Reserved |
| `RETURNED` | Returned |

#### AssetCondition

| Valeur API | Libelle FR |
|---|---|
| `DAMAGED` | Damaged |
| `FAIR` | Fair |
| `GOOD` | Good |
| `NEW` | New |
| `UNUSABLE` | Unusable |

#### AssetMaintenanceStatus

| Valeur API | Libelle FR |
|---|---|
| `CANCELLED` | Annule |
| `COMPLETED` | Termine |
| `IN_PROGRESS` | Entree Progress |
| `PLANNED` | Planned |

#### AssetMaintenanceType

| Valeur API | Libelle FR |
|---|---|
| `CORRECTIVE` | Corrective |
| `INSPECTION` | Inspection |
| `PREVENTIVE` | Preventive |
| `WARRANTY` | Warranty |

#### AssetStatus

| Valeur API | Libelle FR |
|---|---|
| `ASSIGNED` | Assigne |
| `AVAILABLE` | Disponible |
| `DAMAGED` | Damaged |
| `IN_MAINTENANCE` | Entree Maintenance |
| `IN_USE` | Entree Use |
| `LOST` | Lost |
| `RESERVED` | Reserved |
| `RETIRED` | Retired |

#### GoodsReceiptStatus

| Valeur API | Libelle FR |
|---|---|
| `CANCELLED` | Annule |
| `DRAFT` | Brouillon |
| `POSTED` | Comptabilise |

#### InventoryAlertStatus

| Valeur API | Libelle FR |
|---|---|
| `ACKNOWLEDGED` | Confirme |
| `DISMISSED` | Dismissed |
| `OPEN` | Ouvert |
| `RESOLVED` | Resolved |

#### InventoryAlertType

| Valeur API | Libelle FR |
|---|---|
| `ASSET_RETURN_OVERDUE` | Actif immobilise Return Overdue |
| `EXPIRY_SOON` | Expiration Soon |
| `LOW_STOCK` | Faible Stock |
| `MAINTENANCE_DUE` | Maintenance Echeance |
| `NEGATIVE_STOCK` | Negatif Stock |
| `OUT_OF_STOCK` | Sortie Of Stock |
| `SUSPICIOUS_ADJUSTMENT` | Suspicious Ajustement |
| `WARRANTY_SOON` | Warranty Soon |

#### InventoryCountStatus

| Valeur API | Libelle FR |
|---|---|
| `CANCELLED` | Annule |
| `DRAFT` | Brouillon |
| `IN_PROGRESS` | Entree Progress |
| `REVIEW` | Review |
| `VALIDATED` | Validated |

#### InventoryImportType

| Valeur API | Libelle FR |
|---|---|
| `ASSETS` | Actifs immobilises |
| `INITIAL_STOCK` | Initial Stock |
| `ITEMS` | Articles |
| `SUPPLIERS` | Suppliers |

#### InventoryItemType

| Valeur API | Libelle FR |
|---|---|
| `ASSET` | Actif immobilise |
| `CONSUMABLE` | Consumable |
| `SERVICE` | Service |
| `SPARE_PART` | Spare Part |

#### InventoryLocationType

| Valeur API | Libelle FR |
|---|---|
| `LOCKER` | Locker |
| `ROOM` | Room |
| `SHELF` | Shelf |
| `SITE` | Site |
| `VIRTUAL` | Virtual |
| `WAREHOUSE` | Warehouse |

#### InventoryTrackingType

| Valeur API | Libelle FR |
|---|---|
| `EXPIRY` | Expiration |
| `LOT` | Lot |
| `NONE` | Aucun |
| `QUANTITY` | Quantite |
| `SERIAL` | Serie |

#### InventoryUnitType

| Valeur API | Libelle FR |
|---|---|
| `BOX` | Box |
| `KG` | Kg |
| `LITER` | Liter |
| `METER` | Meter |
| `PACK` | Pack |
| `UNIT` | Unite |

#### PurchaseApprovalLevel

| Valeur API | Libelle FR |
|---|---|
| `DIRECTOR` | Director |
| `EXECUTIVE` | Executive |
| `MANAGER` | Manager |
| `NONE` | Aucun |

#### PurchaseOrderStatus

| Valeur API | Libelle FR |
|---|---|
| `APPROVED` | Approuve |
| `CANCELLED` | Annule |
| `DRAFT` | Brouillon |
| `ORDERED` | Ordered |
| `PARTIALLY_RECEIVED` | Partially Recu |
| `RECEIVED` | Recu |

#### PurchaseRequestStatus

| Valeur API | Libelle FR |
|---|---|
| `APPROVED` | Approuve |
| `CANCELLED` | Annule |
| `CONVERTED` | Converted |
| `DRAFT` | Brouillon |
| `REJECTED` | Rejete |
| `SUBMITTED` | Soumis |

#### ReorderSuggestionReason

| Valeur API | Libelle FR |
|---|---|
| `AT_MINIMUM` | le Minimum |
| `BELOW_MINIMUM` | Below Minimum |
| `BELOW_TARGET_MAX` | Below Cible Maximum |
| `NO_STOCK_LEVEL` | Non Stock Niveau |
| `OUT_OF_STOCK` | Sortie Of Stock |

#### ReorderSuggestionSeverity

| Valeur API | Libelle FR |
|---|---|
| `CRITICAL` | Critique |
| `HIGH` | Eleve |
| `LOW` | Faible |
| `MEDIUM` | Moyen |

#### StockMovementType

| Valeur API | Libelle FR |
|---|---|
| `ADJUSTMENT_IN` | Ajustement Entree |
| `ADJUSTMENT_OUT` | Ajustement Sortie |
| `IN` | Entree |
| `OUT` | Sortie |
| `TRANSFER` | Transfert |

#### StockOwnershipType

| Valeur API | Libelle FR |
|---|---|
| `COMPANY` | Entreprise |
| `CONSIGNMENT` | Consignment |
| `CUSTOMER` | Client |
| `SUPPLIER` | Fournisseur |

#### StockReferenceType

| Valeur API | Libelle FR |
|---|---|
| `ASSET_ASSIGNMENT` | Actif immobilise Assignment |
| `BOOKING` | Reservation |
| `CONTRACT` | Contrat |
| `GOODS_RECEIPT` | Goods Reception |
| `INVENTORY_COUNT` | Inventaire Nombre |
| `MANUAL_ADJUSTMENT` | Manuel Ajustement |
| `OTHER` | Other |
| `POS_SALE` | Pos Sale |
| `PURCHASE_ORDER` | Achat Commande |

#### StockReservationStatus

| Valeur API | Libelle FR |
|---|---|
| `ACTIVE` | Actif |
| `CANCELLED` | Annule |
| `CONSUMED` | Consumed |
| `EXPIRED` | Expire |
| `RELEASED` | Released |

#### StockTransferWorkflowStatus

| Valeur API | Libelle FR |
|---|---|
| `APPROVED` | Approuve |
| `CANCELLED` | Annule |
| `RECEIVED` | Recu |
| `REQUESTED` | Requested |
| `SHIPPED` | Shipped |

#### SupplierStatus

| Valeur API | Libelle FR |
|---|---|
| `ACTIVE` | Actif |
| `BLOCKED` | Blocked |
| `INACTIVE` | Inactif |

### Notifications

#### NotificationChannel

| Valeur API | Libelle FR |
|---|---|
| `EMAIL` | E-mail |
| `IN_APP` | Entree App |
| `WEBHOOK` | Webhook |

#### NotificationDeliveryStatus

| Valeur API | Libelle FR |
|---|---|
| `CANCELLED` | Annule |
| `DELIVERED` | Livre |
| `FAILED` | Echoue |
| `PENDING` | En attente |
| `PROCESSING` | En traitement |
| `SENT` | Envoye |
| `SKIPPED` | Skipped |

#### NotificationRecipientType

| Valeur API | Libelle FR |
|---|---|
| `ADMIN` | Administrateur |
| `BUSINESS` | Entreprise |
| `CUSTOMER` | Client |
| `GUEST` | Guest |
| `MEMBER` | Membre |
| `SYSTEM` | System |

### Noyau / Transversal

#### AuditStatus

| Valeur API | Libelle FR |
|---|---|
| `FAILURE` | Failure |
| `SUCCESS` | Succes |

#### BillingEntityType

| Valeur API | Libelle FR |
|---|---|
| `COMPANY` | Entreprise |
| `PERSONAL` | Personal |

#### EventType

| Valeur API | Libelle FR |
|---|---|
| `BOOKING_CANCELLED` | Reservation Annule |
| `BOOKING_CONFIRMED` | Reservation Confirme |
| `BOOKING_FAILED` | Reservation Echoue |
| `BOOKING_REMINDER` | Reservation Reminder |
| `INVOICE_ISSUED` | Facture Emission |
| `PAYMENT_FAILED` | Paiement Echoue |
| `PAYMENT_RECEIVED` | Paiement Recu |

#### IdempotencyStatus

| Valeur API | Libelle FR |
|---|---|
| `COMPLETED` | Termine |
| `FAILED` | Echoue |
| `PROCESSING` | En traitement |

#### NotificationChannel

| Valeur API | Libelle FR |
|---|---|
| `EMAIL` | E-mail |
| `IN_APP` | Entree App |
| `PUSH` | Push |
| `SMS` | SMS |
| `WHATSAPP` | WhatsApp |

#### OutboxEventStatus

| Valeur API | Libelle FR |
|---|---|
| `FAILED` | Echoue |
| `PENDING` | En attente |
| `PROCESSING` | En traitement |
| `PUBLISHED` | Published |

#### ReceiptPreference

| Valeur API | Libelle FR |
|---|---|
| `EVERY_TRANSACTION` | Every Transaction |
| `INVOICE_ONLY` | Facture Only |

#### ResetPolicy

| Valeur API | Libelle FR |
|---|---|
| `DAILY` | Quotidien |
| `MONTHLY` | Mensuel |
| `NEVER` | Never |
| `YEARLY` | Annuel |

#### SequenceCode

| Valeur API | Libelle FR |
|---|---|
| `ENTRY` | Entry |
| `INVOICE` | Facture |
| `VENDOR` | Vendor |

### Paiements et portefeuilles

#### CashMovementType

| Valeur API | Libelle FR |
|---|---|
| `ADJUSTMENT` | Ajustement |
| `CASH_IN` | Especes Entree |
| `CASH_OUT` | Especes Sortie |
| `CLOSING_COUNT` | Closing Nombre |
| `OPENING_FLOAT` | Opening Float |
| `PAYMENT` | Paiement |
| `REFUND` | Remboursement |
| `SAFE_DEPOSIT` | Safe Deposit |
| `TRANSFER_IN` | Transfert Entree |
| `TRANSFER_OUT` | Transfert Sortie |

#### CashSessionStatus

| Valeur API | Libelle FR |
|---|---|
| `CANCELLED` | Annule |
| `CLOSED` | Ferme |
| `CLOSING_REVIEW` | Closing Review |
| `OPEN` | Ouvert |
| `SUSPENDED` | Suspended |

#### PaymentIntentStatus

| Valeur API | Libelle FR |
|---|---|
| `AUTHORIZED` | Autorise |
| `CANCELLED` | Annule |
| `EXPIRED` | Expire |
| `FAILED` | Echoue |
| `PARTIALLY_REFUNDED` | Partiellement rembourse |
| `PENDING` | En attente |
| `PROCESSING` | En traitement |
| `REFUNDED` | Rembourse |
| `REVERSED` | Reversed |
| `SUCCEEDED` | Succeeded |

#### PaymentMethod

| Valeur API | Libelle FR |
|---|---|
| `BANK_TRANSFER` | Virement bancaire |
| `CARD` | Carte |
| `CASH` | Especes |
| `CHEQUE` | Cheque |
| `CREDIT_NOTE` | Credit Note |
| `MANUAL_ADJUSTMENT` | Manuel Ajustement |
| `MOBILE_MONEY` | Mobile money |
| `WALLET` | Portefeuille |

#### PaymentTransactionStatus

| Valeur API | Libelle FR |
|---|---|
| `CANCELLED` | Annule |
| `FAILED` | Echoue |
| `PENDING` | En attente |
| `PROCESSING` | En traitement |
| `REFUNDED` | Rembourse |
| `REVERSED` | Reversed |
| `SUCCEEDED` | Succeeded |

#### ReconciliationStatus

| Valeur API | Libelle FR |
|---|---|
| `COMPLETED` | Termine |
| `FAILED` | Echoue |
| `OPEN` | Ouvert |

#### WalletEntryDirection

| Valeur API | Libelle FR |
|---|---|
| `CREDIT` | Credit |
| `DEBIT` | Debit |

#### WalletEntryType

| Valeur API | Libelle FR |
|---|---|
| `ADJUSTMENT` | Ajustement |
| `ADMIN_DEBIT` | Administrateur Debit |
| `ADMIN_TOPUP` | Administrateur Topup |
| `CASHBACK` | Cashback |
| `HOLD` | Blocage |
| `HOLD_RELEASE` | Blocage Liberer |
| `OVERPAYMENT_CREDIT` | Overpayment Credit |
| `PAYMENT` | Paiement |
| `PROMOTIONAL_CREDIT` | Promotional Credit |
| `REFUND` | Remboursement |
| `REVERSAL` | Reversal |

#### WalletHoldStatus

| Valeur API | Libelle FR |
|---|---|
| `ACTIVE` | Actif |
| `CAPTURED` | Captured |
| `EXPIRED` | Expire |
| `RELEASED` | Released |

#### WalletStatus

| Valeur API | Libelle FR |
|---|---|
| `ACTIVE` | Actif |
| `CLOSED` | Ferme |
| `LOCKED` | Verrouille |
| `SUSPENDED` | Suspended |
| `UNDER_REVIEW` | Under Review |

### Reservations

#### BookingEventType

| Valeur API | Libelle FR |
|---|---|
| `BOOKING_APPROVED` | Reservation Approuve |
| `BOOKING_CANCELLED` | Reservation Annule |
| `BOOKING_CHECKED_IN` | Reservation Checked Entree |
| `BOOKING_CHECKED_OUT` | Reservation Checked Sortie |
| `BOOKING_COMPLETED` | Reservation Termine |
| `BOOKING_CONFIRMED` | Reservation Confirme |
| `BOOKING_CREATED` | Reservation Cree |
| `BOOKING_HOLD_CREATED` | Reservation Blocage Cree |
| `BOOKING_HOLD_EXPIRED` | Reservation Blocage Expire |
| `BOOKING_NO_SHOW` | Reservation Non Show |
| `BOOKING_REJECTED` | Reservation Rejete |
| `BOOKING_STARTED` | Reservation Debut |
| `EMAIL_FAILED` | E-mail Echoue |
| `EMAIL_QUEUED` | E-mail Queued |
| `EMAIL_SENT` | E-mail Envoye |
| `ENTITLEMENT_CONSUMED` | Droit Consumed |
| `ENTITLEMENT_RELEASED` | Droit Released |
| `ENTITLEMENT_RESERVED` | Droit Reserved |
| `QUOTA_OVERRIDE_APPLIED` | Quota Derogation Applique |
| `REPAIR_APPLIED` | Repair Applique |
| `RESOURCE_RELEASED` | Ressource Released |
| `RESOURCE_RESERVED` | Ressource Reserved |
| `USAGE_RECORDED` | Utilisation Recorded |

#### BookingHoldStatus

| Valeur API | Libelle FR |
|---|---|
| `ACTIVE` | Actif |
| `CONFIRMED` | Confirme |
| `EXPIRED` | Expire |
| `RELEASED` | Released |

#### BookingLineType

| Valeur API | Libelle FR |
|---|---|
| `BILLABLE` | Facturable |
| `ENTITLEMENT` | Droit |
| `RESOURCE` | Ressource |

#### BookingNotificationStatus

| Valeur API | Libelle FR |
|---|---|
| `CANCELLED` | Annule |
| `FAILED` | Echoue |
| `PENDING` | En attente |
| `SENT` | Envoye |

#### BookingNotificationType

| Valeur API | Libelle FR |
|---|---|
| `APPROVAL_REQUEST` | Approbation Demande |
| `APPROVED` | Approuve |
| `CANCELLATION` | Annulation |
| `CHECK_IN` | Check Entree |
| `CHECK_OUT` | Check Sortie |
| `COMPLETION` | Completion |
| `CONFIRMATION` | Confirmation |
| `NO_SHOW` | Non Show |
| `REJECTED` | Rejete |
| `REMINDER` | Reminder |
| `WAITLIST_AVAILABLE` | Waitlist Disponible |

#### BookingParticipantRole

| Valeur API | Libelle FR |
|---|---|
| `GUEST` | Guest |
| `HOST` | Host |
| `ORGANIZER` | Organizer |

#### BookingParticipantStatus

| Valeur API | Libelle FR |
|---|---|
| `CONFIRMED` | Confirme |
| `DECLINED` | Declined |
| `INVITED` | Invited |
| `REMOVED` | Removed |

#### BookingPaymentMode

| Valeur API | Libelle FR |
|---|---|
| `DIRECT` | Direct |
| `PASS` | Pass |
| `SUBSCRIPTION` | Abonnement |

#### BookingRecurrenceFrequency

| Valeur API | Libelle FR |
|---|---|
| `DAILY` | Quotidien |
| `MONTHLY` | Mensuel |
| `WEEKLY` | Hebdomadaire |

#### BookingStatus

| Valeur API | Libelle FR |
|---|---|
| `CANCELLED` | Annule |
| `COMPLETED` | Termine |
| `CONFIRMED` | Confirme |
| `DRAFT` | Brouillon |
| `EXPIRED` | Expire |
| `IN_PROGRESS` | Entree Progress |
| `NO_SHOW` | Non Show |
| `PENDING_APPROVAL` | En attente Approbation |
| `REJECTED` | Rejete |

### Ressources

#### ResourceBookingUnit

| Valeur API | Libelle FR |
|---|---|
| `DAY` | Jour |
| `HALF_DAY` | Demi-journee |
| `HOUR` | Heure |
| `MONTH` | Mois |
| `WEEK` | Semaine |

#### ResourceStatus

| Valeur API | Libelle FR |
|---|---|
| `ACTIVE` | Actif |
| `ARCHIVED` | Archive |
| `INACTIVE` | Inactif |
| `MAINTENANCE` | Maintenance |
| `OUT_OF_SERVICE` | Sortie Of Service |

