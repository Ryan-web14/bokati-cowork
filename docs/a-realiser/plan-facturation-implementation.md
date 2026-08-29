# Facturation — plan d'implémentation

Écrit le 2026-08-29. Spécification de construction, à lire après
`plan-facturation-remises.md` qui porte l'analyse et le chiffrage.

Décisions prises :

- Le plancher se **déduit** du coût lorsqu'aucun montant n'est saisi.
- Les documents ne s'écrasent plus : chaque modification produit une **version**.
- Un avoir peut être **reversé au portefeuille** du client.
- Le catalogue est enrichi et devient administrable.

Trois arbitrages restaient ouverts. Faute de réponse, j'applique mes recommandations, signalées
comme telles au § 3. Elles sont réversibles par configuration, sauf mention contraire.

---

## Ordre de livraison

| Lot | Contenu | Dépend de | Coût |
|---|---|---|---|
| **L1** | Catalogue enrichi + API d'administration | — | 2 j |
| **L2** | Plancher dérivé et garde-fous de remise | L1 | 2,5 j |
| **L3** | Versionnage des documents | — | 2 j |
| **L4** | Avoir vers portefeuille | — | 1 j |
| **L5** | Documentation frontend | L1–L4 | 0,5 j |

L3 et L4 sont indépendants et peuvent être menés en parallèle de L1–L2.
**Total : 8 j.**

Migrations : dev `V209` à `V211`, prod `V205` à `V207`. Toute migration se fait **en double**,
les deux dossiers ayant des numérotations distinctes.

---

## L1 — Catalogue enrichi

### 1.1 Champs ajoutés à `service_catalog_item`

Tous facultatifs sauf mention. Laissés vides, le comportement reste celui d'aujourd'hui.

**Économie de la ligne**

| Champ | Type | Rôle |
|---|---|---|
| `cost_price` | `NUMERIC(19,4)` | Prix de revient. Base du plancher dérivé et du calcul de marge. |
| `floor_price` | `NUMERIC(19,4)` | Plancher explicite. Prime sur le plancher dérivé. |
| `min_margin_rate` | `NUMERIC(9,4)` | Marge minimale en %, sert à dériver le plancher. |
| `max_discount_rate` | `NUMERIC(9,4)` | Remise maximale admise sur ce service. |
| `discount_policy` | `VARCHAR(20)` | `NONE` (défaut), `WARN`, `BLOCK`. |

**Présentation et contenu de la prestation**

| Champ | Type | Rôle |
|---|---|---|
| `detailed_description` | `text` | Descriptif long, repris sur le document. |
| `included_items` | `jsonb` | Ce que la prestation comprend, liste ordonnée de libellés. Rendu en sous-lignes sur le PDF. |
| `image_url` | `VARCHAR(500)` | Visuel, pour le catalogue côté interface. |

**Aide à la saisie**

| Champ | Type | Rôle |
|---|---|---|
| `billing_mode` | `VARCHAR(20)` | `UNIT`, `HOURLY`, `DAILY`, `MONTHLY`, `FIXED`. Oriente l'unité et le calcul de quantité. |
| `default_quantity` | `NUMERIC(19,4)` | Quantité pré-remplie. |
| `min_quantity` / `max_quantity` | `NUMERIC(19,4)` | Bornes de saisie. |
| `taxable_by_default` | `BOOLEAN` | Évite de reposer la question à chaque ligne. |

**Classement et validité**

| Champ | Type | Rôle |
|---|---|---|
| `subcategory` | `VARCHAR(100)` | Second niveau sous `category`. |
| `tags` | `jsonb` | Étiquettes libres, pour la recherche. |
| `external_reference` | `VARCHAR(120)` | Référence dans un système tiers. |
| `valid_from` / `valid_until` | `date` | Fenêtre de validité tarifaire. Hors fenêtre, l'article n'est plus proposé mais reste lisible sur les documents passés. |

### 1.2 API d'administration — correction

**L'API existe déjà.** `ServiceCatalogController` expose la création, la liste paginée avec
filtres, la mise à jour, l'activation, la désactivation, la suppression et un `lookup` unifié sur
trois sources (catalogue, inventaire, ressources). Mon inventaire précédent affirmait le
contraire ; c'était faux.

Ce lot se réduit donc à **enrichir l'existant** : les nouveaux champs traversent l'entité, les
deux DTO de requête et le DTO de réponse, sans nouvel endpoint.

Deux ajouts au passage :

- **`clearFields`** sur la requête de mise à jour. L'implémentation n'applique que les champs
  non nuls ; sans mécanisme d'effacement explicite, un plancher posé par erreur ne pourrait
  jamais être retiré par l'API.
- **`effectiveFloorPrice` et `floorPriceOrigin`** sur la réponse. L'interface doit afficher le
  plancher réellement appliqué — qui peut être dérivé alors que `floorPrice` est vide — et
  pouvoir expliquer d'où il vient plutôt qu'opposer un nombre nu.

**Remarque sur le `DELETE` existant.** Il supprime physiquement l'article. Comme
`billing_document_line` conserve `item_code` sous forme de chaîne et non de clé étrangère,
l'intégrité référentielle n'est pas menacée, mais la définition de l'article est perdue pour les
documents passés. La désactivation reste préférable. Je ne l'ai pas retiré : ce serait une
rupture d'API.

### 1.3 Migration

`V209` dev / `V205` prod. Ajouts de colonnes uniquement, aucune valeur par défaut contraignante,
aucune réécriture de lignes existantes.

---

## L2 — Plancher dérivé et garde-fous

### 2.1 Résolution du plancher

Ordre d'application, premier servi :

1. `floor_price` renseigné → c'est le plancher.
2. sinon `cost_price` **et** `min_margin_rate` renseignés → plancher dérivé.
3. sinon `cost_price` seul → plancher = prix de revient. On ne vend pas à perte.
4. sinon aucun plancher.

**Définition de la marge — à confirmer.** Je retiens le **taux de marque**, c'est-à-dire la
marge rapportée au prix de vente :

```
plancher = cost_price / (1 - min_margin_rate)
```

Avec un coût de 8 000 et une marge minimale de 20 %, le plancher vaut 10 000, et la marge
réalisée à ce prix est bien de 20 % du prix de vente.

L'autre convention, le taux de marge rapporté au coût, donnerait `8 000 × 1,20 = 9 600`, soit
une marge de 16,7 % du prix de vente. Les deux se défendent ; c'est le premier que les
commerciaux entendent généralement par « 20 % de marge ». **Si tu veux l'autre, c'est une ligne
à changer** — dis-le avant que L2 ne parte.

### 2.2 Contrôles

**Par ligne.** Prix unitaire net après remise comparé au plancher, et taux de remise comparé à
`max_discount_rate`. **Les deux s'appliquent, le plus contraignant l'emporte** — c'est le
comportement le moins surprenant, et il évite qu'un plancher soit contourné par un taux permissif.

**Global.** Remise totale rapportée au sous-total, **remises de ligne comprises**. Ne compter que
la remise document laisserait contourner le seuil en éclatant la remise sur les lignes.

```yaml
bokati:
  billing:
    discount-guard:
      enabled: ${BILLING_DISCOUNT_GUARD_ENABLED:true}
      max-document-discount-rate: ${BILLING_MAX_DOCUMENT_DISCOUNT_RATE:20}
      max-document-discount-amount: ${BILLING_MAX_DOCUMENT_DISCOUNT_AMOUNT:}
```

### 2.3 Quand ça bloque

**Avertissement au brouillon, blocage à l'émission.** Le commercial construit son offre
librement ; on l'arrête avant qu'elle n'engage.

Concrètement : `simulate` et `create` renvoient les violations dans la réponse sans échouer.
`issue()` et `validate()` refusent. Cela rend la simulation du § 1 doublement utile — elle
annonce le refus avant qu'il ne survienne.

### 2.4 Contournement

Permission dédiée `BILLING:DISCOUNT_OVERRIDE`, pas un rôle en dur. Un rôle codé en dur crée un
droit invisible du référentiel — c'est exactement ce qui a produit les 403 sur `BILLING:DELETE`.

Le dépassement exige un motif, écrit dans `billing_document.discount_override_reason`, tracé par
`FiscalAuditService`, et visible dans l'historique du document. Un dépassement silencieux n'a
aucune valeur de contrôle.

La permission doit être **créée dans le référentiel** par la migration et rattachée à
`ADMIN` et `SUPER_ADMIN`, faute de quoi personne ne pourra jamais l'exercer.

### 2.5 Structure

Un `BillingDiscountGuard` dédié, appelé par `BillingCalculationService`. Il renvoie une liste de
violations typées (`FLOOR_PRICE`, `MAX_DISCOUNT_RATE`, `DOCUMENT_DISCOUNT_RATE`), chacune portant
la ligne concernée, la valeur constatée et la limite. C'est cette liste que l'interface affiche.

### 2.6 Migration

`V210` dev / `V206` prod : `discount_override_reason`, `discount_override_by`,
`discount_override_at` sur `billing_document`, plus la permission `BILLING_DISCOUNT_OVERRIDE`
et son rattachement aux deux rôles d'administration.

---

## L3 — Versionnage des documents

### 3.1 L'état réel

`BillingDocumentEditHistory` est écrit à chaque modification, mais **`snapshot_json` n'est jamais
renseigné** : le constructeur ne pose que `document`, `editType`, `changedBy` et `changedAt`. Et
`changedBy` est codé en dur à `"SYSTEM"`, donc l'auteur réel n'est jamais conservé.

Autrement dit la table et la colonne existent, rien ne les remplit. On sait qu'une modification a
eu lieu, jamais ce que le document contenait avant, ni qui l'a changé.

### 3.2 Ce qui est construit

Trois colonnes sur `billing_document_edit_history` :

| Champ | Rôle |
|---|---|
| `version_number` | Numéro croissant par document, à partir de 1. |
| `sent_to_customer_at` | Horodatage de l'envoi de **cette** version. Distingue un brouillon retouché d'une proposition réellement transmise. |
| `change_summary` | Résumé lisible des champs modifiés, calculé par comparaison avec la version précédente. |

Et surtout : `snapshot_json` **effectivement rempli**, avec l'état complet du document **avant**
modification — en-tête, lignes, remises, clauses — et `changed_by` alimenté depuis le contexte de
sécurité au lieu de la constante.

### 3.3 Comportement

- Toute modification d'un document non scellé archive l'état précédent en version N, puis
  applique les changements. Rien ne s'écrase.
- `send()` marque la version courante comme transmise.
- Le numéro de révision apparaît sur le PDF dès qu'il dépasse 1 : `QUO-…-00000001 rév. 2`.
- Un document scellé n'est pas versionné : il est immuable, et sa correction passe par avoir ou
  facture rectificative, ce qui existe déjà.

```
GET /billing/documents/{n}/versions
GET /billing/documents/{n}/versions/{versionNumber}
GET /billing/documents/{n}/versions/{a}/diff/{b}
```

### 3.4 Point d'attention

Le `snapshot_json` d'un document à cinquante lignes n'est pas anodin. Prévoir l'élagage des
versions intermédiaires **non transmises** au-delà d'un seuil, en conservant systématiquement
toutes les versions envoyées au client. Ce sont elles qui ont une valeur probante.

`V211` dev / `V207` prod.

---

## L4 — Avoir vers portefeuille

### 4.1 Ce qui existe

`WalletEntryType.REFUND` est déjà défini, et `WalletLedgerService.credit(...)` accepte une clé
d'idempotence explicite depuis la reprise du portefeuille. Toute l'infrastructure est en place ;
il manque le chemin métier.

### 4.2 L'endpoint

```
POST /billing/credit-notes/{creditNoteNumber}/refund-to-wallet
```

Conditions, dans cet ordre :

1. Le document est bien un avoir.
2. Il est validé (`locked = true`) — un avoir non scellé n'a pas de valeur.
3. Il n'a pas déjà été consommé, ni par imputation sur facture ni par reversement.
4. Le client possède un portefeuille, ou il en est créé un dans la devise de l'avoir.

Puis crédit du portefeuille avec `WalletEntryType.REFUND`, `sourceType = "CREDIT_NOTE"`,
`sourceCode` = numéro de l'avoir, et **clé d'idempotence `CREDIT_NOTE_REFUND:<numéro>`**. C'est
cette clé qui rend un double appel inoffensif : le second retrouve l'écriture existante au lieu
de recréditer.

L'avoir passe ensuite au statut consommé, et l'opération est tracée par `FiscalAuditService`.

### 4.3 Devise

Le crédit se fait dans la devise de l'avoir. Si le portefeuille du client est dans une autre
devise, l'opération est **refusée** plutôt que convertie : il n'existe aujourd'hui aucun contrôle
de devise sur le portefeuille — c'est un point de dette déjà relevé — et introduire une
conversion implicite ici créerait un écart silencieux.

---

## L5 — Documentation frontend

Livrée dans `docs/frontend/facturation-nouvelles-fonctionnalites.md`, une fois L1 à L4 en
production. Contenu :

- Chaque endpoint : verbe, chemin, corps de requête et de réponse complets, permission exigée.
- Les codes d'erreur et leur signification, en particulier les violations de garde-fou, qui
  doivent s'afficher à l'utilisateur et non finir en erreur générique.
- Les parcours : simuler puis créer, dépasser une limite avec motif, consulter et comparer deux
  versions, reverser un avoir.
- Ce qui change pour l'existant : nouveaux champs sur les réponses de document et de ligne.

---

## Récapitulatif des migrations

| Dev | Prod | Contenu |
|---|---|---|
| `V209` | `V205` | Champs du catalogue |
| `V210` | `V206` | Traçage du dépassement + permission `BILLING_DISCOUNT_OVERRIDE` |
| `V211` | `V207` | Versionnage : `version_number`, `sent_to_customer_at`, `change_summary` |

---

## À confirmer avant de commencer

Un seul point bloque réellement : **la convention de marge du § 2.1**, taux de marque ou taux de
marge. Le reste suit les recommandations déjà formulées et se règle par configuration.
