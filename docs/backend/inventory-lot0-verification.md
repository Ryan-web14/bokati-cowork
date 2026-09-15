# Lot 0 · Procédure de vérification

Ce qui est livré dans le lot 0 se vérifie à deux niveaux. Les règles applicatives sont couvertes par
des tests unitaires Mockito, exécutés par `./mvnw test`. Les deux garanties qui vivent dans la base
de données, immuabilité et concurrence, ne sont pas couvrables par le harnais de test actuel : le
projet n'a ni Testcontainers ni base en mémoire. Elles se vérifient donc à la main, une fois, contre
la base de développement, avec les requêtes ci-dessous.

Cette lacune est le point 7 de la section « Points à trancher » du plan d'implémentation : tant qu'un
harnais d'intégration n'est pas financé, ces contrôles restent manuels.

## Prérequis

```bash
docker compose up -d
./mvnw spring-boot:run          # applique les migrations Flyway, dont V216
```

Connexion à la base :

```bash
docker compose exec postgres psql -U <user> -d cowork_dev_db
```

## 1. Immuabilité du journal de mouvements

### 1.1 Les triggers sont en place

```sql
SELECT tgname, tgrelid::regclass AS table_name
FROM pg_trigger
WHERE NOT tgisinternal
  AND tgname IN ('trg_stock_movement_immutable',
                 'trg_stock_movement_no_delete',
                 'trg_stock_movement_lot_immutable');
```

Attendu : trois lignes.

### 1.2 Une modification d'un champ métier est refusée

```sql
UPDATE stock_movement
SET quantity = quantity + 1
WHERE id = (SELECT id FROM stock_movement ORDER BY id LIMIT 1);
```

Attendu : `ERROR: Le mouvement de stock <code> est immuable...`

À rejouer sur `quantity`, `item_id`, `movement_type`, `unit_cost`, `performed_at` et
`allow_negative_override`. Chacun doit être refusé.

### 1.3 Une suppression est refusée

```sql
DELETE FROM stock_movement
WHERE id = (SELECT id FROM stock_movement ORDER BY id LIMIT 1);
```

Attendu : `ERROR: La suppression du mouvement de stock <code> est interdite...`

### 1.4 La contre-passation reste possible

C'est le contrôle qui compte le plus : un trigger trop strict casserait `reverseMovement()`.

```sql
-- Les colonnes de contre-passation restent modifiables
UPDATE stock_movement
SET reversed = TRUE,
    reversed_at = now(),
    reversed_by = 'verification-lot0',
    reversal_reason = 'Test de la procedure de verification'
WHERE id = (SELECT id FROM stock_movement WHERE reversed = FALSE ORDER BY id LIMIT 1);
```

Attendu : `UPDATE 1`.

```sql
-- Le retour en arriere sur une contre-passation est refuse
UPDATE stock_movement
SET reversed = FALSE
WHERE reversed_by = 'verification-lot0';
```

Attendu : `ERROR: ... est deja contre-passe.`

Puis, par l'API, contre-passer un mouvement réel et vérifier qu'il n'y a pas d'erreur :

```bash
curl -X POST "http://localhost:8080/sni/api/v1/inventory/stock/movements/<code>/reverse" \
     -H "Authorization: Bearer <token>" \
     -H "Content-Type: application/json" \
     -d '{"performedBy":"verification","reason":"Verification lot 0"}'
```

Attendu : 200, et création d'un mouvement compensatoire distinct.

### 1.5 La ventilation par lot est protégée

```sql
UPDATE stock_movement_lot SET quantity = quantity + 1 WHERE id = (SELECT id FROM stock_movement_lot LIMIT 1);
DELETE FROM stock_movement_lot WHERE id = (SELECT id FROM stock_movement_lot LIMIT 1);
```

Attendu : les deux sont refusées.

## 2. Réconciliation

### 2.1 Une base saine ne remonte aucun écart

```bash
curl "http://localhost:8080/sni/api/v1/inventory/admin/reconciliation" -H "Authorization: Bearer <token>"
```

Attendu : `"consistent": true`, `"divergenceCount": 0`, et un `pairsChecked` non nul.

### 2.2 Un écart introduit est bien détecté

La seule façon d'introduire un écart est d'écrire directement sur `stock_level`, ce qu'aucun code
applicatif ne fait. C'est précisément ce que la réconciliation doit rattraper.

```sql
UPDATE stock_level
SET quantity_on_hand = quantity_on_hand + 7
WHERE id = (SELECT id FROM stock_level ORDER BY id LIMIT 1);
```

Relancer l'appel : la ligne doit apparaître avec `difference = 7`.

Puis remettre en état :

```sql
UPDATE stock_level
SET quantity_on_hand = quantity_on_hand - 7
WHERE id = (SELECT id FROM stock_level ORDER BY id LIMIT 1);
```

Attendu : `consistent` repasse à `true`.

### 2.3 Le worker hebdomadaire lève une alerte

Réintroduire un écart, puis forcer l'exécution en abaissant temporairement la cadence :

```bash
INVENTORY_RECONCILIATION_CRON="0 */2 * * * *" ./mvnw spring-boot:run
```

Attendu dans les journaux : `Inventory reconciliation worker: pairsChecked=..., divergences=1, ...`,
et une alerte `STOCK_LEVEL_DIVERGENCE` visible sur `GET /inventory/alerts`. Une seconde exécution ne
doit pas créer de doublon tant que l'alerte est ouverte.

## 3. Concurrence sur les sorties simultanées

Le verrouillage était déjà en place avant ce lot : `@Version` sur `StockLevel` et
`findByItemAndLocationForUpdate` en `PESSIMISTIC_WRITE`. Ce contrôle vérifie qu'il tient sous charge.

Préparer un article à 100 unités sur un emplacement, puis lancer 50 sorties d'une unité en parallèle :

```bash
ITEM=ART-TEST; LOC=LOC-MAIN; TOKEN=<token>
for i in $(seq 1 50); do
  curl -s -X POST "http://localhost:8080/sni/api/v1/inventory/stock/out" \
       -H "Authorization: Bearer $TOKEN" \
       -H "Content-Type: application/json" \
       -d "{\"itemCode\":\"$ITEM\",\"locationCode\":\"$LOC\",\"quantity\":1,\"performedBy\":\"charge-$i\"}" &
done
wait
```

Vérifications attendues :

1. le niveau final vaut exactement 50 ;
2. `GET /inventory/admin/reconciliation?itemCode=ART-TEST` renvoie `consistent: true` ;
3. le nombre de mouvements `OUT` créés vaut exactement 50.

Rejouer le scénario en partant de 30 unités pour 50 sorties : les 20 sorties en trop doivent être
refusées proprement, sans stock négatif ni mouvement orphelin.

## 4. Idempotence renforcée

Par défaut la configuration est vide, donc le comportement est inchangé.

```bash
IDEMPOTENCY_REQUIRED_OPERATIONS=INVENTORY_STOCK_OUT ./mvnw spring-boot:run
```

Puis une sortie sans en-tête :

```bash
curl -i -X POST "http://localhost:8080/sni/api/v1/inventory/stock/out" \
     -H "Authorization: Bearer <token>" -H "Content-Type: application/json" \
     -d '{"itemCode":"ART-TEST","locationCode":"LOC-MAIN","quantity":1}'
```

Attendu : 400, `Missing required header: Idempotency-Key`.

La même requête avec `-H "Idempotency-Key: $(uuidgen)"` doit passer, et un rejeu avec la même clé
doit renvoyer la réponse mémorisée sans créer de second mouvement.

Contrôle croisé sur une opération non listée, par exemple `INVENTORY_STOCK_IN` : elle doit rester
acceptée sans clé.

## 5. Listes de valeurs servies par le backend

```bash
curl "http://localhost:8080/sni/api/v1/inventory/reference/enums/groups" -H "Authorization: Bearer <token>"
curl "http://localhost:8080/sni/api/v1/inventory/reference/enums/movementTypes" -H "Authorization: Bearer <token>"
curl "http://localhost:8080/sni/api/v1/inventory/reference/data" -H "Authorization: Bearer <token>"
```

Attendu : 29 groupes d'énumérations, chaque option portant `value` et `label` en français, et quatre
référentiels vivants (`units`, `categories`, `locations`, `suppliers`).

Contrôle de non-régression à faire à chaque ajout de valeur d'énumération : la nouvelle valeur doit
apparaître dans la liste sans modification du front, et son libellé doit être déclaré dans
`InventoryReferenceLabels`, faute de quoi elle ressortira avec un libellé technique.

## 6. Rapport de dérogations

```bash
curl "http://localhost:8080/sni/api/v1/inventory/admin/reports/overrides" -H "Authorization: Bearer <token>"
```

Attendu : `totalOverrides` cohérent avec `movementsWithNegativeOverride` du rapport d'anomalies, et
`manualOverrides` inférieur, puisqu'il exclut les forçages issus des validations d'inventaire.

Point d'attention relevé pendant l'implémentation : `InventoryCountServiceImpl.validate()` pose
`allowNegativeOverride = true` sur **tous** les ajustements d'inventaire, y compris positifs, et leur
affecte le motif `CONSUMPTION`. Le forçage lui-même est légitime, un inventaire doit pouvoir imposer
la quantité comptée. Le motif, lui, est faux pour un écart positif. C'est la raison pour laquelle le
rapport sépare les deux populations. Le nettoyage du motif relève du lot 2, qui introduit les
`AdjustmentReason` paramétrables.
