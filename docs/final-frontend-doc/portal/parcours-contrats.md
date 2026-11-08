# portal : contrats

> Partie du [README du module portal](README.md). Routes exactes et modèles : [ClientContractController](routes/ClientContractController.md). Fonctionnement de fond (génération, statuts, avenants, ordre de signature, signature électronique par lien) : [../contract/README.md](../contract/README.md). Règles communes : [README racine](../README.md).

Code : `src/main/java/com/sni/bokaticowork/features/portal/document/contract/`.

## Ce qui est propre au portail

- **Périmètre** : contrats dont le propriétaire est le membre (`ownerType = MEMBER`, `ownerCode = memberId`). Les contrats d'une organisation sont sous `/sni/api/v1/client/companies/{customerId}/contracts` (gérant, module `company`) ; ceux de domiciliation sous `/sni/api/v1/client/domiciliation/**` (module `domiciliation`). Un contrat d'un autre répond `404` « Contract not found ».
- **Signature simple dans le portail** : la signature se fait par une case « j'accepte » et, facultativement, une image ou un tracé (`signatureData`). Le serveur enregistre la signature sur le document du brouillon avec l'adresse IP et le navigateur (`User-Agent`), puis marque le contrat signé.
- **Ordre de signature** : si le contrat a plusieurs signataires ordonnés, le membre ne peut signer qu'après ceux qui le précèdent.

## Parcours

### Lire ses contrats

1. Liste : `GET /sni/api/v1/client/documents/contracts` (page, tri `createdAt` décroissant) : `contractCode`, `title`, `status`, `startDate`, `endDate`, `signedAt`, `hasPdf`.
2. Détail : `GET /sni/api/v1/client/documents/contracts/{contractCode}` : en plus `description`, `renewalType`, `effectiveDate`, `activatedAt`, `terminatedAt`, `terminationReason`, `canSign`, `canDownloadPdf`, `versions` (historique des versions et avenants, chacun avec son `amendmentCode`).
3. PDF : `GET .../{contractCode}/pdf` : le document signé s'il existe, sinon le brouillon. `400` « No PDF is available for this contract yet » si aucun.
4. PDF d'un avenant : `GET .../{contractCode}/amendments/{amendmentCode}/pdf`. `400` « No PDF is available for this amendment yet » si l'avenant n'a pas de document.

### Signer

1. N'afficher « Signer » que si `canSign` est vrai : statut `GENERATED` ou `AWAITING_SIGNATURE`, brouillon présent, pas encore de document signé.
2. Faire lire le PDF (étape 3 ci-dessus), puis `POST .../{contractCode}/sign` avec `{ "accepted": true, "signatureData": "..." }`. `accepted` doit être `true` (`400` « You must accept the contract terms to sign » sinon).
3. Réponse : le contrat à jour, au statut `SIGNED`, ou `ACTIVE` s'il s'active automatiquement à la signature.

Statuts possibles d'un contrat : `DRAFT`, `GENERATED`, `UNDER_REVIEW`, `AWAITING_SIGNATURE`, `SIGNED`, `ACTIVE`, `SUSPENDED`, `AMENDED`, `EXPIRED`, `TERMINATED`, `CANCELLED` (voir [../contract/README.md](../contract/README.md)).

## Règles et erreurs à refléter dans l'écran

| Cas | Statut | Message |
| --- | --- | --- |
| Contrat hors `GENERATED`/`AWAITING_SIGNATURE` | `400` | `Contract is not available for signing. Current status: ...` |
| Document pas encore généré | `400` | `Contract document has not been generated yet` |
| Signataires précédents en attente | `400` | `You cannot sign yet · previous signatories have not completed their signatures` |
| Case non cochée | `400` | `You must accept the contract terms to sign` |

## À prévoir côté frontend

- Écrans : liste, détail avec historique des versions, lecteur PDF, signature (case à cocher et zone de tracé facultative).
- Messages en anglais : les traduire à l'affichage.
- Une notification `CONTRACT_SIGNING_REQUESTED` signale un contrat à signer : lien vers le détail.
- La signature du membre marque tout le contrat signé même si d'autres signataires d'ordre supérieur n'ont pas signé (voir écarts du README) : ne pas en déduire que toutes les parties ont signé.
