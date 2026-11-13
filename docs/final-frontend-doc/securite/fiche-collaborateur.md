# securite : fiche collaborateur (lot B12)

> Retour au [README du module](README.md).

La fiche collaborateur d'un utilisateur du back-office porte sa **fonction** (imprimée sous son nom sur les pièces), sa **signature** (imprimée dans le visa des factures) et son **supérieur direct** (N+1, utilisé par les [centres d'approbation](../approval/README.md)).

## Administrateur

- `GET /sni/api/v1/admin/users/{userCode}/staff-profile` (droit `SYSTEM:USERS`).
- `PUT /sni/api/v1/admin/users/{userCode}/staff-profile` avec `{ "jobTitle": "Commerciale", "managerUserCode": "USR-...", "signatureImageBase64": null, "clearSignature": false }`. Un champ absent reste inchangé ; `managerUserCode` vide retire le supérieur. Refus si le supérieur est l'utilisateur lui-même ou si le choix crée une boucle.

## Utilisateur connecté

- `GET /sni/api/v1/staff/me/profile` : sa fiche.
- `PUT /sni/api/v1/staff/me/signature` avec `{ "signatureImageBase64": "data:image/png;base64,..." }` : sa signature (PNG ou JPEG en data URI, 300 Ko au plus).
- `DELETE /sni/api/v1/staff/me/signature` : l'efface.

Réponse `StaffProfileResponse` : `userCode`, `email`, `fullName`, `jobTitle`, `hasSignature`, `signatureImageBase64` (pour l'aperçu), `signatureUpdatedAt`, `managerUserCode`, `managerName`, `managerEmail`.

Écran conseillé : une zone de dessin ou de dépôt d'image pour la signature, avec aperçu ; un rappel que la signature s'imprime sur les factures que l'on établit.
