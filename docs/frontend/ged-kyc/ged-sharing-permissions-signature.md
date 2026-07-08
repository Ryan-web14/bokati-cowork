# GED — Partage, permissions &amp; signature électronique

## Liens de partage — `/sni/api/v1`

| Méthode &amp; Route | Rôle | Auth |
|---|---|---|
| `POST /documents/{documentCode}/share` | Créer un lien de partage pour un document | admin |
| `POST /document-folders/{folderCode}/share` | Créer un lien de partage pour un dossier | admin |
| `GET /documents/{documentCode}/shares` | Lister les liens actifs d'un document | admin |
| `DELETE /shares/{token}` | Révoquer un lien | admin |
| `GET /shares/{token}?password=` | **Ouvrir/consulter un lien de partage** | **public, aucune authentification** |

**Requête de création** `{ expiresInHours*, password, allowDownload (défaut true), maxAccessCount }`.

**Réponse** `ShareLinkResponse` : `token, shareUrl, documentCode, folderCode, allowDownload, passwordProtected, maxAccessCount, accessCount, expiresAt, createdAt, createdBy, active`.

`shareUrl` = `{base}/sni/api/v1/shares/{token}` — c'est exactement le chemin `/shares/**` en accès public dans la configuration de sécurité (`SecurityConfig`), donc utilisable tel quel dans un email/lien partagé sans connexion.

- **Expiration** : vérifiée à chaque accès (`expiresAt`), erreur 400 si dépassée.
- **Quota d'accès** : erreur 400 si `accessCount >= maxAccessCount`.
- **Mot de passe appliqué** : si `passwordProtected = true` sur la réponse de création, le frontend doit demander le mot de passe à l'utilisateur avant d'appeler `GET /shares/{token}` et le transmettre en query param `?password=...`. Si le mot de passe est absent ou incorrect, l'API renvoie **401 Unauthorized** (`"This share link requires a valid password"`). Si le lien n'est pas protégé (`passwordProtected = false`), le paramètre est ignoré et l'accès est libre.

## Permissions fines — `/sni/api/v1/document-permissions`

| Méthode &amp; Route | Rôle | Paramètres clés | Réponse |
|---|---|---|---|
| `POST /document-permissions` | Accorder une permission à un utilisateur/rôle sur un document ou dossier | body `targetType*`, `targetCode*`, `granteeType*`, `granteeId*`, `permission*` (ex: VIEW/EDIT/DOWNLOAD/DELETE) | `DocumentPermissionResponse` (201) |
| `DELETE /document-permissions` | Révoquer | query `targetType*`, `targetCode*`, `granteeType*`, `granteeId*`, `permission*` | 204 |
| `GET /document-permissions` | Lister les permissions d'une cible | query `targetType*`, `targetCode*` | `List<DocumentPermissionResponse>` |
| `GET /document-permissions/effective` | Permissions effectives (résolues) d'un utilisateur sur un document | query `userId*`, `documentCode*` | `Set<String>` (⚠️ tableau brut, pas d'objet englobant) |

Aucune permission spécifique déclarée sur ce contrôleur.

## Signature électronique — `/sni/api/v1/documents/{documentCode}/signatures`

| Méthode &amp; Route | Rôle | Paramètres clés | Réponse |
|---|---|---|---|
| `POST /documents/{documentCode}/signatures` | Créer une demande de signature | `signerType*`, `signerId*`, `signerName*`, `signerEmail` | `DocumentSignatureResponse` (201) |
| `GET /documents/{documentCode}/signatures` | Lister les demandes/signatures d'un document | — | `List<DocumentSignatureResponse>` |
| `PATCH /documents/{documentCode}/signatures/{signatureId}/sign` | Signer une demande | `accepted*` (doit être `true`), `signatureData` (optionnel, ex: image/consentement en base64) | `DocumentSignatureResponse` |

`ipAddress`/`User-Agent` sont capturés côté serveur (pas à envoyer par le frontend) et apparaissent dans la réponse pour la traçabilité.

**Enum `DocumentSignatureStatus`** : `PENDING, SIGNED, DECLINED, EXPIRED`.

⚠️ Ce contrôleur est back-office uniquement — il n'existe pas aujourd'hui de route publique/portail pour qu'un signataire externe signe sans compte admin. Si un parcours de signature "sans connexion" est nécessaire, il faudra passer par le mécanisme `/shares/**` ou une future route publique dédiée.
