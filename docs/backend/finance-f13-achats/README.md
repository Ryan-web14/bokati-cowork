# Lot F13 : achats et fournisseurs

Lot du [plan finance](../plan-finance-comptabilite/README.md) (section « Fournisseurs et achats » et tableau des lots). S'appuie sur F1 (pièces), F5 (états de besoin), F7 (pièces imprimées), F9 (analytique), F11 (TVA) et F16 (contrôle budgétaire). Les immobilisations (fiche, plan d'amortissement) passent au lot F18.

Critère d'acceptation du plan : **une commande validée engage le budget**. Vérifié par `PurchaseF13IT`.

Branche `feature/finance-comptabilite`. Migrations : dev `V310__achats_fournisseurs_f13.sql`, prod `V305__achats_fournisseurs_f13.sql` (contenu identique ; la prod `V304` est prise par le correctif des courriels de facturation renuméroté). Code dans `features/accounting` : `PurchaseService`, `PurchaseController`, entités `AccSupplier`, `AccPurchaseOrder(Line)`, `AccSupplierInvoice(Line)`, `AccSupplierPayment`.

## Fournisseurs

`acc_supplier` : code (`FRN-n` par défaut), nom, NIU, RCCM, adresse, téléphone, courriel, coordonnées bancaires, délai de paiement en jours (30), compte collectif (`401`), compte de charge par défaut, exonération de TVA, **retenue à la source** : taux (en %), compte (`447`), base (`HT` ou `TTC`), libellé ; statut actif ou inactif. Le code du fournisseur est son compte auxiliaire (tiers des lignes `401`).

## Commandes

- Saisie (`POST /purchases/orders`) ou **depuis un état de besoin validé** (`POST /purchases/orders/from-need` : toutes ses lignes chiffrées restantes, ou celles choisies), sans ressaisie ; les lignes de l'état de besoin sont marquées transformées et l'état passe `TRANSFORMED` ou `PARTIALLY_TRANSFORMED`.
- Numéro `BC-AAAAMM-0001`. Chaque ligne : désignation, quantité, prix unitaire HT, nature d'opération ou compte, taux de TVA (18 % par défaut, `accounting.purchase.default-vat-rate` ; 0 pour un fournisseur exonéré), section.
- Compte de charge : celui de la nature d'opération, sinon celui saisi, sinon celui par défaut du fournisseur.
- **Validation** (`PATCH /orders/{n}/validate`) par une **autre personne** que le préparateur : contrôle budgétaire (F16) sur les montants hors taxe, puis **engagement** (`acc_budget_commitment`, source `PURCHASE_ORDER`). Un dépassement d'une ligne bloquante est refusé (`409`), sauf dérogation tracée : droit `BUDGET:OVERRIDE` et motif dans le corps (`{ "reason": "..." }`).
- **Annulation** (`PATCH /orders/{n}/cancel`, motif obligatoire) tant qu'elle n'est pas facturée : l'engagement est libéré.
- Bon de commande imprimé (`GET /orders/{n}/pdf`), pièce numérotée `BC` du lot F7.
- États : `DRAFT`, `VALIDATED`, `PARTIALLY_INVOICED`, `INVOICED`, `CANCELLED`.

## Factures et avoirs fournisseurs

- Saisie en brouillon (`POST /purchases/invoices`) : fournisseur, **numéro de la facture du fournisseur** (un même numéro pour un même fournisseur est refusé en `409`), date, échéance (par défaut date + délai du fournisseur), commande liée (sans lignes, la facture reprend le reste à facturer de la commande), lignes, référence du document (GED). Numéro interne `FF-AAAAMM-0001` (avoirs `AF-`).
- TVA récupérable par ligne : `4452` sur achats, `4454` sur services extérieurs (comptes `62`, `63`), `4451` sur immobilisations (classe `2`) ; code de taxe `TVA18` et base sur la ligne (déclaration F11).
- **Retenue à la source** : taux du fournisseur × base (HT ou TTC), ou montant saisi ; net à payer = TTC − retenue.
- **Comptabilisation** (`POST /invoices/{n}/post`) au journal `AC` (`accounting.purchase.journal`) : D charges HT (par section) ; D TVA récupérable ; C `401` au nom du fournisseur pour le net à payer, avec l'échéance ; C `447` pour la retenue (code `RAS`). Sur commande : les montants facturés sont reportés sur la commande ; entièrement facturée (ou facture marquée `closeOrder`), la commande passe `INVOICED` et l'engagement budgétaire est **réalisé** pour le montant facturé (l'écart est libéré).
- **Avoir** (`kind = CREDIT_NOTE`, facture d'origine obligatoire) : écriture inverse, référence de lettrage de la facture d'origine ; il diminue le reste à payer de la facture et se lettre avec elle.
- Un brouillon se supprime (`DELETE /invoices/{n}`) ; une facture comptabilisée se corrige par un avoir.
- États : `DRAFT`, `POSTED`, `PARTIALLY_PAID`, `PAID`.

## Règlements et lettrage

- `POST /invoices/{n}/pay` (montant, par défaut le reste à payer ; date ; compte et journal de trésorerie ; référence) : D `401` ; C trésorerie ; le compte fournisseur est **lettré** par le numéro interne de la facture dès qu'elle est soldée.
- Le lettrage automatique de nuit (F4) couvre aussi le `401`.
- Paiement depuis la caisse : la nature « Paiement fournisseur » de la caisse reste disponible pour les petites factures.

## États

- **Échéancier** (`GET /purchases/payables?until=`) : factures à payer par date d'échéance, reste à payer, jours de retard.
- **Relevé fournisseur** (`GET /suppliers/{code}/statement?from=&to=`) : grand livre auxiliaire du `401`, solde courant (créditeur = dû au fournisseur).
- **Retenues à la source du mois** (`GET /purchases/withholdings?period=AAAA-MM`, export CSV `/withholdings/export`) : par facture, fournisseur, NIU, base, taux, retenue, compte ; total à reverser ; mouvement du compte de retenue sur le mois. Le reversement à l'État se passe par la banque : D `447` ; C banque.

## Routes

Préfixe `/api/v1/accounting/purchases`. Écritures auditées.

| Route | Droit |
| --- | --- |
| Toutes les lectures (`GET`) | `PURCHASE:READ` |
| `POST /suppliers`, `POST /suppliers/from-inventory/{code}` | `SUPPLIER:MANAGE` |
| `POST /orders`, `/orders/from-need` | `PURCHASE_ORDER:MANAGE` |
| `PATCH /orders/{n}/validate`, `/orders/{n}/cancel` | `PURCHASE_ORDER:VALIDATE` |
| `POST /invoices`, `/invoices/{n}/post`, `DELETE /invoices/{n}` | `SUPPLIER_INVOICE:MANAGE` |
| `POST /invoices/{n}/pay` | `SUPPLIER_PAYMENT:MANAGE` |

Rôles : `SUPER_ADMIN`, `ADMIN` et `FINANCE` tout ; `MANAGER` la lecture, la préparation et la validation des commandes.

## Suggestion de fournisseurs (complément)

Voir [finance-suggestion-fournisseurs](../finance-suggestion-fournisseurs/README.md). La fiche fournisseur de référence est celle de l'inventaire ; `acc_supplier` gagne `inventory_supplier_id` (facultatif, unique) :
- `POST /suppliers` accepte `inventorySupplierId` ; `POST /suppliers/from-inventory/{code}` retrouve ou crée le fournisseur comptable d'une fiche d'inventaire (nom, NIU, adresse, téléphone, courriel copiés ; `401` et `447` par défaut) ;
- `POST /orders/from-need` : `supplierCode` devient facultatif ; absent, la fiche fournisseur portée par les lignes retenues (toutes la même) donne le fournisseur de la commande ; un code de fiche d'inventaire est aussi accepté.

## Paramètres (domaine Comptabilité)

`accounting.purchase.journal` (`AC`), `accounting.purchase.supplier-account` (`401`), `accounting.purchase.withholding-account` (`447`), `accounting.purchase.vat-account` (`4452`), `accounting.purchase.vat-account-services` (`4454`), `accounting.purchase.vat-account-fixed-assets` (`4451`), `accounting.purchase.default-vat-rate` (`18`), `accounting.purchase.analytic-axis` (vide : ventilation par défaut des comptes).

## Hors de ce lot

- Fiche d'immobilisation et plan d'amortissement depuis une facture (lot F18) : la facture d'un bien en classe 2 est écrite, sa TVA va en `4451`, la fiche viendra avec F18.
- Factures non parvenues (`408`) en clôture : lot F20 (rattachement).
- Retenue à la source sur un décaissement de caisse direct : passer par une facture fournisseur.

## Pour le frontend

- **Fournisseurs** : liste, fiche (coordonnées, NIU, délai, compte par défaut, exonération, retenue à la source), relevé.
- **Commandes** : liste par état ; création manuelle ou « Créer une commande » depuis un état de besoin validé (choix du fournisseur et des lignes) ; fiche avec lignes et totaux ; « Valider » (masqué pour le préparateur ; en cas de `409` budget, proposer la dérogation avec motif à un détenteur de `BUDGET:OVERRIDE`) ; « Annuler » (motif) ; « Imprimer le bon de commande ».
- **Factures fournisseurs** : saisie (depuis une commande : lignes reprises), contrôle de doublon (`409`), totaux HT, TVA, TTC, retenue, net à payer ; « Comptabiliser » ; avoir depuis une facture ; « Régler » (montant, date, compte et journal) ; historique des règlements.
- **États** : échéancier (retards en rouge), retenues du mois avec export.

## Tests

`PurchaseF13IT` (schéma de production) :
- commande depuis un état de besoin (20 ramettes à 5 000) : compte `6055` de la nature, TVA 18 000, état de besoin transformé ; validation refusée au préparateur, faite par une autre personne ; **le budget est engagé de 100 000** ;
- facture sur commande avec retenue de 5 % : TTC 118 000, retenue 5 000, net 113 000 ; doublon refusé ; écriture `6055` 100 000, `4452` 18 000 (`TVA18`), `401` 113 000 au nom du fournisseur, `447` 5 000 ; commande `INVOICED`, engagement réalisé ; échéancier et état des retenues ; règlement : facture `PAID`, `401` lettré, relevé soldé ; bon de commande en PDF ;
- avoir de 2 000 HT sur une facture de 10 000 HT : reste à payer 9 440, facture partiellement réglée ;
- commande depuis un état de besoin dont la ligne porte une fiche fournisseur, sans fournisseur indiqué : fournisseur comptable créé depuis la fiche, puis retrouvé.

`AdminApiAuthorizationManagerTest` couvre les routes.
