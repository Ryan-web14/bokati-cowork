# GED — Partage, permissions & signature électronique

> **Base URL** : `https://api.elleaose.com/sni/api/v1`
> **Back-office** (admin), **sauf** l'ouverture d'un lien de partage (`GET /shares/{token}`) qui est **publique**.
> Aucune permission spécifique déclarée sur ces contrôleurs (auth admin standard).

---

## Partie A — Liens de partage

Permettent de partager un document ou un dossier via une URL, éventuellement protégée par mot de passe, avec expiration et quota d'accès.

### A.1 Créer un lien

```http
POST /documents/{documentCode}/share          → ShareLinkResponse (201)
POST /document-folders/{folderCode}/share      → ShareLinkResponse (201)
Content-Type: application/json
```
```json
{
  "expiresInHours": 48,
  "password": "secret123",
  "allowDownload": true,
  "maxAccessCount": 5
}
```

| Champ | Obligatoire | Description |
|---|---|---|
| `expiresInHours` | ✅ | Durée de validité en heures. |
| `password` | — | Si fourni, le lien devient protégé (`passwordProtected = true`). |
| `allowDownload` | — | Autorise le téléchargement (défaut `true`). |
| `maxAccessCount` | — | Nombre max d'accès (nul = illimité). |

**Réponse** `201 Created` → `ShareLinkResponse` :
```json
{
  "token": "a1b2c3d4e5",
  "shareUrl": "https://api.elleaose.com/sni/api/v1/shares/a1b2c3d4e5",
  "documentCode": "DOC-...42",
  "folderCode": null,
  "allowDownload": true,
  "passwordProtected": true,
  "maxAccessCount": 5,
  "accessCount": 0,
  "expiresAt": "2026-07-11T10:00:00Z",
  "createdAt": "2026-07-09T10:00:00Z",
  "createdBy": 42,
  "createdByEmail": "agent@elleaose.com",
  "active": true
}
```

`shareUrl` correspond exactement au chemin public `/shares/{token}` — utilisable tel quel dans un email/lien sans connexion.

### A.2 Ouvrir un lien (public)

```http
GET /shares/{token}?password=secret123        (public, aucune authentification)
```
**Réponse** `200 OK` → `ShareLinkResponse` (avec `accessCount` incrémenté).

Règles d'accès :
- **Expiration** — vérifiée à chaque accès ; **400** si `expiresAt` dépassé.
- **Quota** — **400** si `accessCount >= maxAccessCount`.
- **Mot de passe** — si `passwordProtected = true`, le frontend doit demander le mot de passe et le transmettre en query `?password=...`. Absent/incorrect → **401 Unauthorized** (`"This share link requires a valid password"`). Si le lien n'est pas protégé, le paramètre est ignoré.

### A.3 Lister / révoquer

```http
GET    /documents/{documentCode}/shares        → List<ShareLinkResponse>   (liens actifs du document)
DELETE /shares/{token}                          → 204 No Content            (révoque)
```

---

## Partie B — Permissions fines · `/document-permissions`

Accorder à un utilisateur/rôle un droit précis sur un document ou un dossier.

### B.1 Accorder

```http
POST /document-permissions
Content-Type: application/json
```
```json
{
  "targetType": "DOCUMENT",
  "targetCode": "DOC-...42",
  "granteeType": "USER",
  "granteeId": 17,
  "permission": "VIEW"
}
```

| Champ | Obligatoire | Description |
|---|---|---|
| `targetType` | ✅ | Cible : `DOCUMENT` ou `FOLDER`. |
| `targetCode` | ✅ | Code de la cible. |
| `granteeType` | ✅ | Bénéficiaire : `USER` ou `ROLE`. |
| `granteeId` | ✅ | Id de l'utilisateur/rôle. |
| `permission` | ✅ | Droit accordé (ex `VIEW`, `EDIT`, `DOWNLOAD`, `DELETE`). |

**Réponse** `201 Created` → `DocumentPermissionResponse` :
```json
{
  "id": 88,
  "targetType": "DOCUMENT",
  "targetId": 1287,
  "targetCode": "DOC-...42",
  "granteeType": "USER",
  "granteeId": 17,
  "granteeEmail": "legal@elleaose.com",
  "permission": "VIEW",
  "grantedBy": 42,
  "grantedByEmail": "agent@elleaose.com",
  "grantedAt": "2026-07-09T10:00:00Z"
}
```

### B.2 Révoquer / lister / permissions effectives

```http
DELETE /document-permissions?targetType=DOCUMENT&targetCode=DOC-...42&granteeType=USER&granteeId=17&permission=VIEW
       → 204 No Content
GET    /document-permissions?targetType=DOCUMENT&targetCode=DOC-...42
       → List<DocumentPermissionResponse>   (toutes les permissions de la cible)
GET    /document-permissions/effective?userId=17&documentCode=DOC-...42
       → Set<String>   (permissions résolues)
```

⚠️ `/effective` renvoie un **tableau brut de chaînes**, pas un objet englobant :
```json
["VIEW", "DOWNLOAD"]
```

---

## Partie C — Signature électronique · `/documents/{documentCode}/signatures`

### C.1 Créer une demande de signature

```http
POST /documents/{documentCode}/signatures
Content-Type: application/json
```
```json
{
  "signerType": "MEMBER",
  "signerId": 1287,
  "signerName": "Jean Kabila",
  "signerEmail": "jean.kabila@example.com"
}
```

| Champ | Obligatoire | Description |
|---|---|---|
| `signerType` | ✅ | Type de signataire (ex `MEMBER`, `CUSTOMER`, `USER`). |
| `signerId` | ✅ | Id du signataire. |
| `signerName` | ✅ | Nom affiché. |
| `signerEmail` | — | Email (validé si fourni). |

**Réponse** `201 Created` → `DocumentSignatureResponse` :
```json
{
  "id": 301,
  "documentCode": "DOC-...42",
  "signerType": "MEMBER",
  "signerId": 1287,
  "signerName": "Jean Kabila",
  "signerEmail": "jean.kabila@example.com",
  "signatureStatus": "PENDING",
  "signedAt": null,
  "ipAddress": null,
  "userAgent": null
}
```

### C.2 Lister / signer

```http
GET   /documents/{documentCode}/signatures                        → List<DocumentSignatureResponse>
PATCH /documents/{documentCode}/signatures/{signatureId}/sign      → DocumentSignatureResponse
```
```json
{ "accepted": true, "signatureData": "data:image/png;base64,iVBORw0..." }
```
- `accepted` doit être **`true`** (sinon 400).
- `signatureData` optionnel (image de signature / consentement en base64).
- `ipAddress`/`User-Agent` sont **capturés côté serveur** (pas à envoyer) et apparaissent dans la réponse après signature, pour la traçabilité.

**Enum `DocumentSignatureStatus`** : `PENDING, SIGNED, DECLINED, EXPIRED`.

> ⚠️ Ce contrôleur est **back-office uniquement** — il n'existe pas aujourd'hui de route publique/portail pour qu'un signataire externe signe sans compte admin. Pour un parcours « sans connexion », passer par le mécanisme `/shares/**` (Partie A) ou une future route publique dédiée.
