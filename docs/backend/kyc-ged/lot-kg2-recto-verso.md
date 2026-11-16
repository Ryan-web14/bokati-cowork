# Lot KG2 : recto, verso et remplacements

> Partie du [plan KYC et GED](README.md). Fait le 16/11/2026 sur `feature/kyc-ged`.

## Rattachement en un seul endroit

`KycPieceLinker` (`features/document/kyc/service/support`) rattache tout document KYC déposé à sa pièce, quelle que soit la voie : portail, dépôt par l'équipe (`/kyc/documents/admin/upload`), route documentaire `/documents/upload`. Le dépôt indique la face par `DocumentUploadMetadataRequest.kycSide` (`FRONT` par défaut, `BACK`).

| Face | Pièce du même type existe | Effet |
| --- | --- | --- |
| Recto | non | Pièce créée, `PENDING`. |
| Recto | oui | Même pièce (même `id`) : nouveau fichier, champs repris du dépôt, `PENDING`. L'ancien recto et l'ancien verso passent `SUPERSEDED` ; le verso est à redéposer. |
| Verso | non | `400` « Le recto doit être déposé avant le verso pour la pièce : … ». |
| Verso | oui | Le document devient le verso de la pièce ; l'ancien verso passe `SUPERSEDED` ; pièce `PENDING`. Jamais de pièce pour un verso. |

- La règle « un seul document du type » (`multipleAllowed`) ne s'applique plus aux pièces KYC (elle reste pour les autres catégories).
- Chaque fichier remplacé publie `DOCUMENT_SUPERSEDED` (avec `replacedBy`) ; il reste consultable dans la GED.
- Les services du portail et de l'équipe ne créent ni ne suppriment plus de ligne eux-mêmes : ils reprennent la pièce rattachée (par recto ou par verso).

## Synchronisation après validation

- `KycDocumentSyncRunner` programme la synchronisation d'une pièce **après validation** de la transaction de l'appelant, **une fois** par document et par transaction ; rien si la transaction est annulée ; tout de suite hors transaction. Le document est relu dans une transaction propre (`applyDocumentSync`).
- Avant, la tâche asynchrone partait pendant la transaction (lecture de données non validées) et un dépôt la lançait deux ou trois fois.
- Un verso met à jour sa pièce, il n'en crée plus. État de la pièce : celui du recto, `REJECTED` si le verso est refusé ou archivé. Un fichier `SUPERSEDED` n'est plus rattaché.

## Portail

- Retrait d'une pièce `PENDING` : ses fichiers sont archivés (motif « Pièce retirée par le titulaire depuis son espace »), la pièce supprimée, le dossier recalculé ; un nouveau dépôt du même type est possible aussitôt.
- Dépôt, remplacement et retrait recalculent le dossier par ses exigences (plus de synchronisation par statut du membre).

## Identifiants

- `KycDocumentResponse.id` est partout le numéro court de la pièce (mappage `shortId`).
- `POST /kyc/documents/bulk-approve` accepte `documentCodes` ; `documentIds` (ancien nom, des codes aussi) reste accepté ; `400` si aucun code.

## Écarts réglés

K4, K5, K6, K-n3, K-n10.

## Tests

`KycPieceLinkerKg2Test` (création, verso rattaché et ancien verso remplacé, verso sans recto, nouveau recto, fichier archivé), `KycDocumentSyncRunnerKg2Test` (une fois après validation, rien après annulation, immédiat hors transaction), `ClientKycServiceUploadTest` réécrit (face transmise, pièce reprise par recto ou verso, retrait archivé).
