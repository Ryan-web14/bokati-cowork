# Module facturation — remises encadrées, simulation et outils

Écrit le 2026-08-29. Fait suite à la demande : ajustement du montant global, déduction
automatique des remises, garde-fous sur le catalogue, et inventaire des outils manquants.

---

## 0. Ce qui existe déjà

À ne pas redévelopper.

| Acquis | Où |
|---|---|
| Remise par ligne, en taux ou en montant | `BillingDocumentLine.discountRate` / `discountAmount` |
| Remise au niveau du document, `PERCENTAGE` ou `FIXED_AMOUNT` | `BillingDiscountType`, `BillingCalculationService.calculateDocumentDiscounts` |
| Répartition de la remise document entre part taxable et part exonérée | `BillingCalculationService`, corrigé le 2026-08-27 |
| Affichage de la remise sur la ligne du PDF | `templates/billing/document.html`, colonne REMISE |
| Catégorie et unité de ligne, avec valeurs par défaut prises au catalogue | `BillingCalculationService.catalogDefaults` |

Le calcul est donc juste et la remise est déjà visible ligne à ligne. Ce qui manque n'est pas
le calcul mais **le moment où il est disponible** et **ce qui l'encadre**.

---

## 1. Simulation avant création — le point le plus rentable

**Le problème.** Aujourd'hui le seul moyen de connaître le total d'un document est de le créer.
D'où la calculatrice, et d'où les documents brouillons créés pour être aussitôt supprimés.

**La solution.** `POST /billing/documents/simulate`, qui accepte exactement le corps de
`CreateBillingDocumentRequest` et renvoie le même bloc de totaux que la création — sans rien
écrire, sans consommer de numéro de séquence.

`BillingCalculationService.calculate(lines, discounts)` fait déjà tout le travail et ne touche
pas la base. L'endpoint est une façade de quelques lignes sur une méthode existante.

Réponse : sous-total, remise ligne, remise document, base taxable, part exonérée, TVA, centimes
additionnels, total, **et le détail par ligne** — montant brut, remise appliquée, net. C'est ce
détail par ligne qui remplace la calculatrice.

Effet de bord utile : la même façade sert de validateur. L'interface peut appeler `simulate` à
chaque frappe et afficher le total en direct.

**Coût : 0,5 j.**

---

## 2. Remise cible — « je veux que ça tombe à ce montant »

**Le problème.** Le besoin réel n'est presque jamais « applique 12 % ». C'est « je veux que le
client paie 450 000 ». Aujourd'hui il faut chercher le taux qui produit ce total.

**La solution.** `POST /billing/documents/simulate/target`, qui prend les lignes et un
`targetTotal`, et renvoie la remise qui l'atteint — en taux et en montant — ou l'explication du
refus si elle est hors des limites du § 3.

Deux modes, à choisir dans la requête :

- `DOCUMENT` — une remise document unique. Le calcul doit s'inverser à travers la TVA : le total
  TTC dépend de la part taxable, elle-même dépendant de la remise. Résolution directe possible,
  la relation est affine par morceaux.
- `PRORATA` — remise répartie sur les lignes au prorata de leur montant, ce qui donne un document
  où chaque ligne porte sa remise. C'est le mode à privilégier quand la remise doit être lisible
  par le client sur le PDF.

**Coût : 1 j**, dont la moitié en tests sur l'inversion TVA.

---

## 3. Garde-fous au catalogue — la partie qui demande un arbitrage

Trois champs **non obligatoires** sur `ServiceCatalogItem` :

| Champ | Type | Sens |
|---|---|---|
| `floorPrice` | `NUMERIC(19,4)` | Prix plancher. Aucune ligne de ce service ne peut descendre en dessous. |
| `maxDiscountRate` | `NUMERIC(9,4)` | Taux de remise maximal admis sur ce service. |
| `discountPolicy` | `VARCHAR(30)` | `NONE` (défaut), `WARN`, `BLOCK`. |

Laissés à `null`, le comportement est celui d'aujourd'hui : aucun contrôle. C'est ce qui rend
l'ajout non intrusif sur le catalogue existant.

**Contrôle global du document.** En plus du contrôle ligne à ligne, un seuil global :
remise totale rapportée au sous-total. Au-delà, le document est bloqué. Configurable, et
activable ou désactivable :

```yaml
bokati:
  billing:
    discount-guard:
      enabled: ${BILLING_DISCOUNT_GUARD_ENABLED:true}
      max-document-discount-rate: ${BILLING_MAX_DOCUMENT_DISCOUNT_RATE:20}
      max-document-discount-amount: ${BILLING_MAX_DOCUMENT_DISCOUNT_AMOUNT:}
```

**Le contournement.** Une permission dédiée `BILLING:DISCOUNT_OVERRIDE`, pas un rôle en dur.
Le raisonnement : `AdminApiAuthorizationManager` résout déjà des permissions par module et
action, et coder `ROLE_ADMIN` en dur créerait un droit invisible du référentiel — exactement le
défaut qui a produit les 403 sur `BILLING:DELETE`. Une permission se voit, s'accorde et se
retire depuis l'écran des rôles.

Le dépassement autorisé n'est pas silencieux : il exige un motif, écrit dans
`billing_document.discount_override_reason`, tracé par `FiscalAuditService` et affiché dans
l'historique du document.

**Décision attendue de ta part** — trois points :

- **D-A.** `floorPrice` et `maxDiscountRate` sont-ils cumulatifs, ou le plancher prime-t-il ?
  Ma recommandation : les deux s'appliquent, le plus contraignant gagne. C'est le comportement
  le moins surprenant.
- **D-B.** Le seuil global compte-t-il les remises de ligne, ou seulement la remise document ?
  Ma recommandation : les deux, sinon on contourne le seuil en éclatant la remise sur les lignes.
- **D-C.** Le blocage s'applique-t-il dès le brouillon, ou seulement à l'émission ? Ma
  recommandation : avertissement au brouillon, blocage à l'émission. On laisse le commercial
  construire son offre, on l'arrête avant qu'elle n'engage.

**Coût : 2 j**, migration dev + prod comprise.

---

## 4. Réassignation d'un document à un autre client

**Le problème.** Une facture émise au mauvais client n'a aujourd'hui aucune issue propre : le
`PATCH /recipient` livré le 2026-08-27 corrige les coordonnées mais refuse de changer
`customerCode`, et le trigger `trg_billing_document_immutable` le protège en base.

**Ce qui est possible, et ce qui ne l'est pas.**

- **Document non scellé** (`locked = false`) : réassignation directe. Rien ne l'interdit.
- **Document scellé** : la réassignation est fiscalement impossible. Le seul chemin légal est
  avoir d'annulation sur le client d'origine, puis nouvelle facture sur le bon client.

**La solution.** `POST /billing/documents/{n}/reassign`, qui choisit seul le bon chemin selon
`locked`, et renvoie dans les deux cas le document résultant en indiquant lequel a été pris. Le
chaînage `originalDocumentNumber` conserve la trace, de sorte que le relevé des deux clients
reste cohérent.

**Coût : 1,5 j.**

---

## 5. Autres manques repérés

Classés par rapport valeur / coût. Aucun n'est demandé, tous sont des trous réels.

| # | Manque | Pourquoi ça compte | Coût |
|---|---|---|---|
| 5.1 | **Le catalogue n'a pas d'API d'administration** — `ServiceCatalogItem` se peuple par migration | Sans écran, les garde-fous du § 3 ne sont pas administrables | 1 j |
| 5.2 | **Grilles tarifaires par client** — un tarif négocié se ressaisit à chaque facture | Source d'erreur récurrente, et rend le § 3 bien plus utile | 2 j |
| 5.3 | **Duplication avec report de date** — `duplicate` existe mais recopie les dates | Facturation récurrente manuelle | 0,5 j |
| 5.4 | **Pas d'avoir partiel par ligne** — l'avoir porte un montant global | Un litige porte presque toujours sur une ligne précise | 1 j |
| 5.5 | **`statementTotals` ne filtre pas `document_type`** — les devis entrent dans le total facturé du relevé client | Défaut de calcul, repéré le 2026-08-28, non corrigé | 0,25 j |
| 5.6 | **Pas de relance graduée** — un seul modèle de rappel | Le recouvrement en dépend | 1 j |

Le **5.5** est un vrai défaut et non une amélioration : il fausse un chiffre affiché au client.
À traiter en premier, il coûte deux heures.

---

## 6. Ordre proposé

1. **§ 5.5** — corriger le relevé client. 0,25 j, c'est un bug.
2. **§ 1** — simulation. 0,5 j, débloque immédiatement le travail à la calculatrice.
3. **§ 3** — garde-fous catalogue et seuil global. 2 j, sous réserve des décisions D-A à D-C.
4. **§ 2** — remise cible. 1 j, s'appuie sur le § 1.
5. **§ 4** — réassignation. 1,5 j.
6. **§ 5.1** puis **§ 5.2** — catalogue administrable, puis grilles par client. 3 j.

Total pour les points demandés (§ 1 à § 4) : **5 j**. Avec le § 5 complet : **11 j**.

Le § 1 et le § 5.5 ne dépendent d'aucune décision et peuvent démarrer tout de suite.
