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

## 5. Autres fonctionnalités proposées

Aucune n'est demandée. Toutes correspondent à un manque vérifié dans le code, pas à une liste
générique de fonctions de facturation. Les relances sont volontairement exclues.

### 5.a — Encadrement commercial

Même famille que le § 3 : ce sont les garde-fous qui rendent la remise encadrée réellement utile.

**5.1 — Encours client autorisé.** Aucune notion de plafond d'encours n'existe dans le code
(`creditLimit` est introuvable). Un client peut accumuler des factures impayées sans qu'aucun
seuil ne se déclenche. Deux champs sur `customer` — `creditLimit` et `creditLimitPolicy`
(`NONE` / `WARN` / `BLOCK`) — et le contrôle à l'émission d'une facture ou à la confirmation
d'une réservation. Le montant se lit déjà : `statementTotals` le calcule.
**1,5 j.**

**5.2 — Prix de revient et marge.** `ServiceCatalogItem` porte `unitPrice` mais aucun coût. En
ajoutant `costPrice`, la simulation du § 1 affiche la marge par ligne et pour le document, et
surtout le plancher du § 3 se **déduit du coût** au lieu d'être saisi à la main. C'est la
différence entre bloquer sur un taux arbitraire et bloquer sur une marge réelle. Cette
fonctionnalité change la nature du § 3 ; à décider avant de le construire.
**1,5 j.**

**5.3 — Validation à deux mains.** Au-delà d'un montant, une facture exige une seconde
validation avant émission. Complète la permission `BILLING:DISCOUNT_OVERRIDE` du § 3 : l'une
encadre la remise, l'autre le montant.
**1,5 j.**

### 5.b — Traitement des encaissements

**5.4 — Lettrage multi-facture.** `PaymentAllocationService.allocateIfBillingDocument` rattache
une transaction à **un seul** document, celui de son `sourceCode`. Un client qui règle trois
factures d'un virement unique ne peut donc pas être lettré : il faut trois paiements séparés.
Allocation automatique en FIFO sur les factures ouvertes du client, avec reprise manuelle.
C'est, de toute la liste, le manque qui coûte le plus de temps administratif.
**2 j.**

### 5.c — Volume et cycle

**5.5 — Facturation périodique consolidée.** `BillingAutoInvoiceService` émet une facture **par
transaction de paiement**. Un client qui réserve quinze fois dans le mois reçoit quinze
factures. Un travail périodique qui regroupe la consommation d'une période en un document
unique par client réduirait d'autant le volume émis, envoyé et à recouvrer.
**2,5 j.**

**5.6 — Duplication avec report de date.** `duplicate` existe mais recopie les dates
d'origine. Reporter la période d'un cran en fait l'outil de la facturation récurrente manuelle,
en attendant le 5.5.
**0,5 j.**

**5.7 — Actions par lot.** Émettre, envoyer ou exporter l'ensemble des documents d'un filtre,
en une opération suivie. Aujourd'hui tout se fait document par document.
**1 j.**

### 5.d — Documents

**5.8 — Révisions de devis.** Un devis renégocié s'écrase. `BillingDocumentEditHistory` garde
bien un instantané JSON par modification, mais il n'existe aucune notion de **version envoyée
au client** : impossible de dire ce que le client a vu, ni de lui montrer ce qui a changé entre
deux propositions. Numéroter les révisions et conserver chaque version émise.
**1,5 j.**

**5.9 — Avoir partiel par ligne.** L'avoir porte un montant global. Un litige porte presque
toujours sur une ligne précise, et l'imputation comptable devrait la suivre.
**1 j.**

**5.10 — Pièces jointes.** Bon de commande client, procès-verbal de réception, justificatif.
Le module documentaire et le stockage existent déjà, il ne manque que le rattachement.
**0,75 j.**

**5.11 — Règle d'arrondi.** L'arrondi est fixé dans le code. En XAF, les pièces sous 5 F ne
circulent pas : pouvoir arrondir le total au franc ou aux 5 F évite les écarts de caisse à
l'encaissement.
**0,5 j.**

### 5.e — Catalogue

**5.12 — API d'administration du catalogue.** `ServiceCatalogItem` ne se peuple que par
migration. Sans écran, ni les garde-fous du § 3 ni le coût du 5.2 ne sont administrables : ce
point conditionne les deux.
**1 j.**

**5.13 — Grilles tarifaires par client.** Un tarif négocié se ressaisit à chaque facture.
Source d'erreur récurrente, et rend le § 3 nettement plus pertinent — le plancher devient
propre au contrat.
**2 j.**

---

## 6. Ordre proposé

1. **§ 1** — simulation. *Livré le 2026-08-29.*
2. **§ 5.12** — catalogue administrable. 1 j. Conditionne le § 3 et le 5.2.
3. **§ 5.2** — prix de revient. 1,5 j. À trancher **avant** le § 3, dont il change la nature.
4. **§ 3** — garde-fous et seuil global. 2 j, sous réserve des décisions D-A à D-C.
5. **§ 2** — remise cible. 1 j.
6. **§ 5.4** — lettrage multi-facture. 2 j. Le plus gros gain administratif de la liste.
7. **§ 4** — réassignation. 1,5 j.
8. **§ 5.1** — encours client. 1,5 j.
9. Le reste, par ordre de besoin.

Points demandés (§ 1 à § 4) : **4,5 j** restants. Avec le § 5 complet : **≈ 22 j**.

Deux remarques sur l'enchaînement :

- **Le 5.2 doit être tranché avant le § 3.** Bloquer sur une marge réelle plutôt que sur un
  taux saisi à la main change la conception des garde-fous, pas seulement leur paramétrage.
- **Le 5.12 conditionne les deux.** Tant que le catalogue se peuple par migration, un plancher
  ou un coût ne peuvent pas être maintenus par un administrateur.

---

## 7. Déjà corrigé

- **Relevé client.** `statementTotals` sommait tous les types de documents : les devis entraient
  dans le total facturé montré au client. Limité aux factures et proformas le 2026-08-29.
- **Facture annulée.** Elle restait en statut `PAID` après annulation par avoir, et son montant
  continuait donc d'être compté en chiffre d'affaires. Corrigé le 2026-08-29.
