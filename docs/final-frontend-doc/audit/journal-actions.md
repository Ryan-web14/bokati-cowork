# Journal des actions (lots A1 et A7)

Routes : [socle/routes/AdminAuditController.md](../socle/routes/AdminAuditController.md).

## Liste

`GET /sni/api/v1/admin/audit-logs` : page `PaginatedResponse<AuditLog>`, 20 par page, du plus récent au plus ancien.

| Filtre | Effet |
| --- | --- |
| `module`, `action` | égalité exacte, sans casse |
| `actorEmail` | « contient », sans casse |
| `status` | `SUCCESS` ou `FAILURE` |
| `createdFrom`, `createdTo` | date-heure ISO, bornes incluses |
| `entityType`, `entityCode` | élément principal touché (`BillingDocument`, `FAC-...`), sans casse |
| `requestId` | identifiant de requête exact |
| `ressource` | « contient » |

Une ligne : `createdAt`, `actorEmail`, `actorId`, `module`, `action`, `ressource`, `entityType`, `entityCode`, `auditStatus`, `ipAddress` (adresse réelle du client), `userAgent`, `sessionId`, `requestId`, `errorCode`, `errorMessage`, `metadata`, `diff`.

L'écriture est faite hors du fil de la requête : une action toute récente peut n'apparaître qu'après un court délai (bouton « Actualiser »). Dans `metadata.queryString`, les paramètres sensibles de l'adresse (`token`, `code`, clé...) sont écrits `***`.

## Avant / après (`diff`)

```json
{
  "changes": [
    { "entityType": "BillingDocument", "entityCode": "INV-CUS-20261116-00000026", "operation": "UPDATE",
      "fields": { "status": { "before": "ISSUED", "after": "PAID" }, "paidAmount": { "before": 0, "after": 45000 } } },
    { "entityType": "PaymentTransaction", "entityCode": "PAY-20261116-00000012", "operation": "CREATE",
      "fields": { "amount": { "before": null, "after": 45000 }, "status": { "before": null, "after": "SUCCEEDED" } } },
    { "entityType": "LedgerEntry", "entityCode": "#987", "operation": "CREATE" }
  ],
  "omitted": 0
}
```

- `operation` : `CREATE`, `UPDATE`, `DELETE`.
- `fields` n'existe que pour les **éléments principaux** : documents de facturation, paiements, abonnements, réservations, membres, clients, utilisateurs, rôles, droits, attributions de rôle ou de droit, paramètres, modèles de document, modèles de notification. Seuls les champs modifiés y sont (à la création : les valeurs saisies, `before` à `null`). Les autres éléments touchés sont listés sans champs.
- Une référence à un autre élément s'écrit `Type:code` ; un texte long est coupé à 300 caractères (`...`).
- **`***`** : donnée sensible jamais écrite (mot de passe, jeton, code, clé, carte, valeur d'un paramètre secret). Afficher « modifié (masqué) ».
- 30 éléments et 40 champs par action au plus ; `omitted` compte les éléments non listés.
- Une action en échec (`FAILURE`) n'a pas de `diff` (rien n'a été écrit) mais garde son élément principal.
- Les actions antérieures au lot A1 n'ont ni `diff` ni élément.

Écran conseillé : tableau avant / après par élément, valeurs alignées, `entityCode` cliquable vers la chronologie de l'élément.

## Export

`GET /sni/api/v1/admin/audit-logs/export` avec les mêmes filtres : fichier CSV (point-virgule, UTF-8 avec BOM, `journal-actions-AAAA-MM-JJ.csv`), 10 000 lignes au plus (paramètre `bokati.audit.export.max-rows`). En-têtes de réponse lisibles par le navigateur : `X-Export-Rows`, `X-Export-Truncated` (`true` : prévenir que le fichier est coupé et proposer de réduire la période). Colonne `changements` : une ligne lisible par élément (`UPDATE BillingDocument FAC-1 : status DRAFT -> ISSUED`). Appeler la route avec `responseType: blob` et lire le nom du fichier dans `Content-Disposition`.
