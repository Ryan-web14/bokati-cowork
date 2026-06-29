# Guide Frontend/API Document Et KYC

## Vue d'ensemble

Ce document explique comment le frontend doit utiliser les endpoints :

- `document`
- `document-type`
- `document-requirement`
- `kyc`
- `contracts`

Base URL :

- `/sni/api/v1`

## Concepts principaux

- `DocumentType`
  - definit les types de documents autorises
- `DocumentRequirement`
  - definit les pieces obligatoires par type de proprietaire
- `Document`
  - represente le document logique
- `DocumentVersion`
  - represente chaque version physique du fichier
- `KycCase`
  - represente le dossier KYC d'un proprietaire

Types de proprietaire :

- `MEMBER`
- `CUSTOMER`
- `BUSINESS`

Categories de document les plus courantes :

- `KYC`
- `LEGAL`

## 1. Types de document

### Lister les types de document

`GET /document-types?ownerType=MEMBER`

Exemple de reponse :

```json
[
  {
    "code": "MEMBER_ID_CARD",
    "name": "Member ID Card",
    "category": "KYC",
    "ownerType": "MEMBER",
    "description": "Primary member identity document",
    "required": true,
    "requiresExpiryDate": true,
    "requiresReview": true,
    "requiresSignature": false,
    "multipleAllowed": false,
    "allowedMimeTypes": "application/pdf,image/jpeg,image/png,image/webp",
    "maxFileSizeBytes": 10485760,
    "active": true
  }
]
```

Usage frontend :

- construire dynamiquement l'interface d'upload
- afficher les contraintes de taille et de format
- afficher si la date d'expiration est obligatoire

## 2. Exigences documentaires

### Lister les documents obligatoires

`GET /document-requirements?ownerType=BUSINESS`

Exemple de reponse :

```json
[
  {
    "id": 2002,
    "ownerType": "BUSINESS",
    "documentTypeCode": "BUSINESS_RCCM",
    "documentTypeName": "Business RCCM",
    "customerType": null,
    "businessLegalForm": null,
    "required": true,
    "active": true
  }
]
```

Usage frontend :

- afficher une checklist des documents requis
- indiquer clairement les pieces manquantes

## 3. Uploader un document

### Endpoint

`POST /documents/upload`

Type de contenu :

- `multipart/form-data`

Champs attendus :

- `ownerType`
- `ownerCode`
- `documentTypeCode`
- `title`
- `description`
- `documentNumber`
- `issueDate`
- `expiryDate`
- `uploadedBy`
- `file`

Exemple :

```text
ownerType=MEMBER
ownerCode=MBR-000001
documentTypeCode=MEMBER_ID_CARD
title=Carte nationale d'identite
description=Piece d'identite principale
documentNumber=123456789
issueDate=2025-01-10
expiryDate=2035-01-10
uploadedBy=1
file=<binary>
```

Comportement :

- le vrai type MIME est verifie depuis le contenu du fichier
- le hash SHA-256 est calcule
- le fichier est stocke sous `storage/documents/...`
- une `DocumentVersion` est creee
- si le document est de categorie `KYC`, un `KycDocument` est relie automatiquement

## 4. Remplacer une version existante

### Endpoint

`POST /documents/{code}/replace`

Parametres :

- `uploadedBy`
- `file`

Comportement :

- cree une nouvelle `DocumentVersion`
- conserve l'ancienne version en historique
- remet le statut KYC du document a `PENDING`

## 5. Recuperer le detail d'un document

### Endpoint

`GET /documents/{code}`

Exemple de reponse :

```json
{
  "code": "DOC-202604-000001",
  "ownerId": 12,
  "ownerType": "MEMBER",
  "category": "KYC",
  "documentTypeCode": "MEMBER_ID_CARD",
  "documentTypeName": "Member ID Card",
  "title": "Carte nationale d'identite",
  "description": "Piece d'identite principale",
  "fileName": "id-card.pdf",
  "fileUrl": "C:\\\\java-project\\\\bokati-cowork\\\\storage\\\\documents\\\\member\\\\12\\\\DOC-202604-000001\\\\v1\\\\uuid.pdf",
  "fileSize": 251230,
  "mimeType": "application/pdf",
  "checksumSha256": "abc123...",
  "currentVersionNumber": 1,
  "status": "PENDING_REVIEW",
  "issueDate": "2025-01-10",
  "expiryDate": "2035-01-10",
  "uploadedBy": 1,
  "uploadedAt": "2026-04-09T01:00:00Z",
  "updatedAt": "2026-04-09T01:00:00Z",
  "versions": [
    {
      "versionNumber": 1,
      "storageProvider": "FILESYSTEM",
      "storagePath": "C:\\\\java-project\\\\bokati-cowork\\\\storage\\\\documents\\\\member\\\\12\\\\DOC-202604-000001\\\\v1\\\\uuid.pdf",
      "originalFileName": "id-card.pdf",
      "storedFileName": "uuid.pdf",
      "mimeTypeDeclared": "application/pdf",
      "mimeTypeDetected": "application/pdf",
      "fileExtension": "pdf",
      "checksumSha256": "abc123...",
      "fileSizeBytes": 251230,
      "uploadStatus": "READY",
      "antivirusStatus": "CLEAN",
      "uploadedBy": 1,
      "uploadedAt": "2026-04-09T01:00:00Z",
      "current": true
    }
  ]
}
```

Usage frontend :

- page detail document
- onglet historique des versions
- ecran de revue ou de controle

## 6. Lister les documents

### Endpoint

`GET /documents?ownerType=MEMBER&ownerCode=MBR-000001&page=0&size=20`

Retour :

- `PaginatedResponse<DocumentResponse>`

Usage frontend :

- centre documentaire par `member`, `customer` ou `business`
- file de revue backoffice

## 7. Revue d'un document

### Valider

`POST /documents/{code}/approve`

Body :

```json
{
  "reviewedBy": 1,
  "comment": "Document valide"
}
```

### Rejeter

`POST /documents/{code}/reject`

Body :

```json
{
  "reviewedBy": 1,
  "comment": "Document illisible",
  "rejectionReasonCode": "UNREADABLE",
  "rejectionReasonDetail": "La photo est floue"
}
```

Usage frontend :

- panneau de revue compliance
- affichage des motifs de rejet

## 8. Creer un dossier KYC

### Endpoint

`POST /kyc/cases`

Body :

```json
{
  "ownerType": "MEMBER",
  "ownerCode": "MBR-000001"
}
```

Comportement :

- cree un nouveau dossier KYC
- pour un `member`, conserve ou remet le statut a `PENDING`

## 9. Recuperer un dossier KYC

### Endpoint

`GET /kyc/cases/{code}`

Exemple de reponse :

```json
{
  "code": "KCS-202604-000001",
  "ownerType": "MEMBER",
  "ownerId": 12,
  "status": "IN_PROGRESS",
  "startedAt": "2026-04-09T01:00:00Z",
  "submittedAt": null,
  "completedAt": null,
  "reviewedBy": null,
  "reviewedAt": null,
  "decisionComment": null,
  "complete": false,
  "approved": false,
  "missingDocumentTypeCodes": [
    "MEMBER_ID_CARD"
  ],
  "documents": []
}
```

Usage frontend :

- ecran de progression KYC
- affichage des pieces manquantes
- suivi du dossier

## 10. Soumettre un dossier KYC

### Endpoint

`POST /kyc/cases/{code}/submit`

Comportement :

- verifie que toutes les pieces obligatoires sont presentes
- met le statut du `member` a `UNDER_REVIEW`

Usage frontend :

- bouton final de soumission

## 11. Valider un dossier KYC

### Endpoint

`POST /kyc/cases/{code}/approve`

Body :

```json
{
  "reviewedBy": 1,
  "comment": "KYC conforme"
}
```

Comportement :

- toutes les pieces obligatoires doivent deja etre validees
- le statut du `member` devient `ACTIVE`

## 12. Rejeter un dossier KYC

### Endpoint

`POST /kyc/cases/{code}/reject`

Body :

```json
{
  "reviewedBy": 1,
  "comment": "Merci de recharger une piece plus lisible"
}
```

Comportement :

- le dossier passe a `PENDING_CORRECTION`
- le statut du `member` passe a `PENDING_CORRECTION`

## 13. Lister les templates de contrat

### Endpoint

`GET /contracts/templates`

Exemple de reponse :

```json
[
  {
    "code": "membership-agreement",
    "name": "Membership Agreement",
    "description": "Standard membership contract template"
  }
]
```

## 14. Previsualiser un contrat en HTML

### Endpoint

`POST /contracts/preview`

Body :

```json
{
  "templateCode": "membership-agreement",
  "ownerType": "MEMBER",
  "ownerCode": "MBR-000001",
  "businessCode": "BOK202604123",
  "title": "Contrat d'adhesion mensuelle",
  "description": "Acces mensuel aux espaces et services",
  "uploadedBy": 1,
  "effectiveDate": "2026-04-10",
  "startDate": "2026-04-10",
  "endDate": "2026-05-09",
  "signatoryName": "Jean Dupont",
  "signatoryRole": "Membre",
  "clauses": [
    "Paiement mensuel exigible au debut de chaque periode",
    "Respect du reglement interieur obligatoire"
  ],
  "variables": {
    "offerName": "Membership Pro",
    "amount": "150000 XAF"
  }
}
```

Reponse :

```json
{
  "templateCode": "membership-agreement",
  "html": "<!DOCTYPE html>..."
}
```

Usage frontend :

- modal de previsualisation
- page de validation avant generation PDF

## 15. Generer un contrat PDF

### Endpoint

`POST /contracts/generate`

Body :

- identique a `/contracts/preview`

Comportement :

- rend le HTML a partir d'un template dans `resources/templates/contracts`
- convertit le HTML en PDF
- stocke le PDF comme un `Document`
- cree une nouvelle version documentaire de type legal

Reponse :

- `DocumentResponse`

## Workflow frontend recommande

### KYC Member

1. Creer ou charger le dossier KYC.
2. Charger les exigences documentaires pour `MEMBER`.
3. Charger les types de document pour `MEMBER`.
4. Uploader chaque piece obligatoire.
5. Rafraichir le detail du dossier KYC.
6. Quand `missingDocumentTypeCodes` est vide, activer le bouton de soumission.
7. Soumettre le dossier.
8. Afficher les statuts :
   - `IN_PROGRESS`
   - `SUBMITTED`
   - `PENDING_CORRECTION`
   - `APPROVED`

### KYC Business

1. Creer le dossier KYC du business.
2. Charger les exigences pour `BUSINESS`.
3. Uploader `RCCM`, `NIU` et les autres documents obligatoires.
4. Soumettre le dossier.
5. L'equipe compliance valide ou rejette.

### Generation de contrat

1. Charger les templates disponibles.
2. Construire le formulaire de generation.
3. Previsualiser le HTML du contrat.
4. Laisser l'utilisateur ou le staff verifier le contenu.
5. Generer le PDF.
6. Rediriger vers le document genere.

## Points importants cote frontend

- ne jamais faire confiance uniquement au controle MIME du navigateur
- toujours afficher les commentaires et motifs de rejet
- traiter `PENDING_CORRECTION` comme un etat de correction, pas comme un blocage final
- afficher l'historique des versions d'un document
- toujours indiquer clairement la version courante
- apres remplacement d'un document, recharger le detail du dossier KYC

## Cas d'erreur a gerer

- piece obligatoire manquante lors de la soumission KYC
- type de fichier non supporte
- template de contrat introuvable
- owner code introuvable
- dossier KYC rejete avec demande de correction

## Ecrans recommandes pour le frontend

Cette section propose les ecrans possibles et la presentation recommandee pour couvrir tout le module `document + kyc`.

L'objectif n'est pas de figer le design, mais de donner une base claire au frontend pour construire une experience complete.

### 1. Tableau de bord KYC

Usage :

- vue d'ensemble d'un dossier KYC
- entree principale pour un `member`, `customer` ou `business`

Contenu recommande :

- en-tete avec :
  - nom du proprietaire
  - type de proprietaire
  - code du dossier KYC
  - badge de statut
- carte resume :
  - progression globale
  - nombre de documents fournis
  - nombre de documents valides
  - nombre de documents rejetes
  - nombre de documents manquants
- timeline de statut :
  - `NOT_STARTED`
  - `IN_PROGRESS`
  - `SUBMITTED`
  - `UNDER_REVIEW`
  - `PENDING_CORRECTION`
  - `APPROVED`
  - `REJECTED`
- bloc actions :
  - `Uploader un document`
  - `Soumettre le dossier`
  - `Voir les documents rejetes`

Presentation suggeree :

- layout en 2 colonnes desktop
- resume en haut
- checklist documentaire au centre
- panneau latéral pour actions rapides

### 2. Checklist documentaire KYC

Usage :

- visualiser les documents obligatoires et optionnels
- savoir ce qui manque avant soumission

Contenu recommande :

- liste par document requis
- pour chaque ligne :
  - nom du type documentaire
  - statut visuel
  - caractere obligatoire ou optionnel
  - formats autorises
  - taille max
  - date d'expiration requise ou non
  - bouton `Uploader` ou `Remplacer`

Statuts visuels recommandes :

- `Manquant`
- `Charge`
- `En revue`
- `Valide`
- `Rejete`
- `Expire`

Presentation suggeree :

- tableau ou cartes verticales
- icone + couleur de statut
- bouton d'action a droite
- message d'erreur ou de rejet directement sous la ligne

### 3. Centre documentaire du proprietaire

Usage :

- lister tous les documents d'un `member`, `customer` ou `business`
- consulter l'historique et l'etat de chaque piece

Contenu recommande :

- filtres :
  - categorie
  - type de document
  - statut
  - date d'upload
- tableau principal :
  - code document
  - type de document
  - titre
  - version courante
  - statut
  - date d'upload
  - date d'expiration
  - actions

Actions possibles :

- `Voir`
- `Telecharger`
- `Voir les versions`
- `Remplacer`
- `Soumettre a revue`

Presentation suggeree :

- table admin classique
- drawer detail au clic
- filtres en barre superieure

### 4. Ecran detail document

Usage :

- consulter toutes les metadonnees d'un document
- voir les versions
- lancer ou traiter la revue

Contenu recommande :

- header :
  - titre
  - type
  - statut
  - owner
- apercu fichier :
  - PDF viewer integre
  - image preview si image
- bloc metadonnees :
  - numero de document
  - issue date
  - expiry date
  - mime detecte
  - taille
  - checksum
  - uploadedBy
- onglet versions :
  - numero de version
  - date
  - statut antivirus
  - statut upload
  - version courante ou non
- onglet revue :
  - commentaires
  - motifs de rejet
  - decisions precedentes

Presentation suggeree :

- page detail avec viewer a gauche et informations a droite
- ou modal large avec onglets

### 5. Ecran upload / remplacement document

Usage :

- deposer un nouveau document
- remplacer une version rejetee ou obsolete

Contenu recommande :

- zone drag and drop
- selection manuelle de fichier
- recapitulatif du type documentaire choisi
- champs metadonnees :
  - titre
  - description
  - numero de document
  - issue date
  - expiry date
- affichage des contraintes :
  - formats acceptes
  - taille max
  - date d'expiration obligatoire

Etats a gerer :

- upload en cours
- scan en attente
- upload termine
- erreur de validation MIME
- erreur de taille
- document remplace avec succes

Presentation suggeree :

- modal simple pour remplacement rapide
- page complete si l'upload fait partie d'un workflow KYC plus riche

### 6. File de revue compliance

Usage :

- interface back-office pour agents compliance

Contenu recommande :

- KPIs en haut :
  - documents en attente
  - dossiers KYC soumis
  - dossiers en correction
  - documents expires
- filtres :
  - ownerType
  - statut
  - type documentaire
  - date de soumission
- table ou cartes :
  - proprietaire
  - type de document
  - statut
  - date d'upload
  - dernier commentaire
  - action `Revoir`

Actions principales :

- `Valider`
- `Rejeter`
- `Demander correction`

Presentation suggeree :

- interface admin dense
- badges de priorite
- raccourcis actions en ligne

### 7. Ecran de revue d'un document

Usage :

- traiter un document individuel

Contenu recommande :

- visualisation du fichier
- metadonnees du document
- historique des versions
- formulaire de decision :
  - commentaire
  - motif de rejet code
  - detail libre

Actions :

- `Approuver`
- `Rejeter`
- `Demander correction`

Presentation suggeree :

- split screen :
  - gauche = viewer document
  - droite = panneau de revue

### 8. Ecran detail dossier KYC cote back-office

Usage :

- traiter le dossier complet

Contenu recommande :

- resume du proprietaire
- statut du dossier
- checklist de toutes les pieces
- historique des validations et rejets
- bloc de decision finale :
  - approuver le dossier
  - rejeter le dossier
  - envoyer en correction

Presentation suggeree :

- header fort avec badge de statut
- contenu par sections :
  - Identite du proprietaire
  - Documents
  - Historique
  - Decision finale

### 9. Ecran correction utilisateur

Usage :

- permettre au `member`, `customer` ou `business` de corriger les pieces refusees

Contenu recommande :

- resume clair du rejet
- liste des documents a corriger
- pour chaque piece :
  - motif de rejet
  - commentaire reviewer
  - bouton `Remplacer`

Presentation suggeree :

- ecran tres guide
- accent mis sur ce qu'il faut corriger
- pas de surcharge d'information secondaire

### 10. Ecran historique des versions

Usage :

- audit
- support
- controle compliance

Contenu recommande :

- liste chronologique des versions
- version active marquee visuellement
- date d'upload
- type MIME detecte
- taille
- statut scan
- lien de consultation

Presentation suggeree :

- table compacte dans un onglet
- ou drawer lateral depuis detail document

## Parcours UX recommandes

### Parcours KYC Member

1. Ouvrir le tableau de bord KYC du member.
2. Afficher la checklist des documents requis.
3. Permettre l'upload ou remplacement de chaque piece.
4. Rafraichir la progression en temps reel.
5. Activer `Soumettre le dossier` uniquement si les pieces obligatoires sont presentes.
6. Afficher ensuite le statut `UNDER_REVIEW`.
7. Si rejet, basculer sur un ecran ou un bloc `Corrections demandees`.

### Parcours KYC Business

1. Ouvrir le tableau de bord KYC business.
2. Afficher les documents societaires :
   - RCCM
   - NIU
   - statuts
   - piece du representant
   - justificatif d'adresse
3. Visualiser les documents manquants et expires.
4. Soumettre pour revue compliance.
5. Traiter la validation globale du dossier.

### Parcours compliance admin

1. Ouvrir la file de revue.
2. Filtrer les dossiers ou documents en attente.
3. Ouvrir un document ou un dossier.
4. Valider ou rejeter.
5. Si tous les documents sont conformes, valider le dossier KYC.

## Recommandations de presentation

### Codes couleur utiles

- gris : non commence / manquant
- bleu : en cours / en revue
- vert : valide / approuve
- orange : correction demandee
- rouge : rejete / expire

### Composants utiles

- `StatusBadge`
- `ProgressCard`
- `RequirementChecklist`
- `DocumentUploadCard`
- `DocumentViewer`
- `VersionHistoryTable`
- `ReviewDecisionPanel`
- `CorrectionAlert`

### Bonnes pratiques UI

- toujours afficher le statut de facon visible
- toujours afficher le motif de rejet au plus pres du document concerne
- ne pas cacher les contraintes d'upload
- distinguer clairement :
  - document manquant
  - document charge
  - document charge mais non valide
- sur mobile, transformer les tableaux en cartes detaillees

## Proposition de structure d'ecrans

### Cote utilisateur / operateur

- `KycDashboardPage`
- `OwnerDocumentCenterPage`
- `DocumentUploadPage`
- `DocumentDetailPage`
- `KycCorrectionPage`

### Cote admin / compliance

- `KycReviewQueuePage`
- `KycCaseDetailPage`
- `DocumentReviewPage`
- `DocumentTypeAdminPage`
- `DocumentRequirementAdminPage`

## Priorite de construction frontend

Ordre recommande :

1. `KycDashboardPage`
2. `RequirementChecklist`
3. `DocumentUploadPage`
4. `DocumentDetailPage`
5. `KycReviewQueuePage`
6. `KycCaseDetailPage`
7. `DocumentTypeAdminPage`
8. `DocumentRequirementAdminPage`
