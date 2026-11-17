# Lot KG4 : GED, revue, dépôt, documents

> Partie du [plan KYC et GED](README.md). Fait le 17/11/2026 sur `feature/kyc-ged`.

## Verrou appliqué partout

- `DocumentLockService.assertEditable` sur les décisions (approuver, refuser, faire corriger), les étiquettes (pose, retrait), les métadonnées et la signature, en plus du remplacement, de la restauration et de l'archivage. Un autre utilisateur que le détenteur reçoit `409`.
- `DocumentResponse` porte `locked`, `lockedById`, `lockedByName`, `lockExpiresAt`.

## Remplacement

- Refusé sur un document `ARCHIVED`, `EXPIRED`, `SUPERSEDED` ou `SIGNED` (déposer un nouveau document).
- Le statut après remplacement suit la règle du dépôt (`autoApprove`, délai, revue) ; avant, `autoApprove` était ignoré.

## Propriétaires par code métier

- `DocumentOwnerCodes` : contrat (`contract_record.contract_code`), facture (`billing_document.document_number`), paiement (`payment_transaction.transaction_number`), proposition (`crm_commercial_proposal.proposal_number`), actif (`asset.asset_code`). Le dépôt accepte le code métier ; l'identifiant numérique reste accepté.
- `DocumentResponse.ownerCode` et `ownerLabel` pour tous les propriétaires (membre, client, entreprise compris).

## Formats

- Une seule liste par défaut (`DocumentSecurityService.DEFAULT_ALLOWED_MIME_TYPES` : PDF, JPEG, PNG, WebP), reprise par `upload-config`.
- Dépôt dans un espace : contrôle sur le format **détecté** (signature), plus sur celui que déclare le navigateur.
- `upload-config?space=` : formats du type et de l'espace.

## Lots et ZIP

- Un fichier = une transaction (document, dossier, étiquettes).
- Le dossier passe par le dépôt (droits des dossiers restreints) ; l'espace KYC ne s'impose ni ne se quitte par un lot.
- ZIP : limite effective = plus petite de 100 Mo et de la taille de requête du serveur (10 Mo) ; lecture bornée à 25 Mo par fichier et 200 Mo décompressés pour l'archive ; chemins `..` ou absolus refusés.

## Rétention

- Application nocturne : un document par transaction (avant, une seule transaction pour tout le passage).
- `PATCH /document-retention-policies/{code}/activate` réactive une politique.

## Divers

- Fichier écrit dans le stockage pendant une transaction annulée : supprimé (plus d'orphelin).
- Files de revue : réponse construite depuis le document déjà chargé, sans relecture ni versions (`DocumentService.summarize`).
- `{space}` des opérations en masse et transaction par document : déjà réglés au lot KG0.

## Écarts réglés

G1, G4, G7, G8, G9, G11, G-n2, G-n4, G-n6, G-n8.

## Tests

`DocumentGedKg4Test` (remplacement refusé sur un document clos, décision refusée sous verrou d'un autre, lecture bornée d'une entrée ZIP, limite effective du ZIP) ; suites documents et KYC vertes.
