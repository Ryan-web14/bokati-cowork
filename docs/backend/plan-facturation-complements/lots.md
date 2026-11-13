# Détail des lots

Les routes sont relatives à `/api/v1`. Les droits reprennent ceux de la refonte (`BILLING_*`, `APPROVAL_MANAGE`) ; un droit nouveau est indiqué quand il en faut un.

## C0. Listes de référence du catalogue

**But.** Familles, sous-familles, unités et règles de taxe tenues dans le backend, chargées par le frontend, enrichies depuis l'écran, sans casser les articles ni les écrans actuels.

**Tables (migration du lot).**

| Table | Colonnes principales |
| --- | --- |
| `billing_catalog_category` | `code` (unique, stable), `label`, `parent_code` (nul pour une famille, code de la famille pour une sous-famille), `description`, `revenue_account` (compte de produit par défaut, facultatif), `display_order`, `active`, `created_from_item` (vrai si ajoutée en mode souple), dates |
| `billing_catalog_unit` | `code`, `label` (« heure »), `plural_label` (« heures »), `symbol` (« h »), `quantity_scale` (décimales permises sur la quantité, 0 à 4), `inventory_unit_code` (facultatif, CD1), `display_order`, `active`, `created_from_item`, dates |
| `billing_tax_rule` (existante) | colonnes ajoutées : `label_on_document` (mention imprimée), `exemption_reason` (motif légal d'exonération), `treatment` (`STANDARD`, `REDUCED`, `EXEMPT`, `OUT_OF_SCOPE`), `display_order` |

**Reprise.** La migration remplit les listes avec les valeurs distinctes déjà présentes dans `service_catalog_item` (famille, sous-famille, unité) et `billing_document_line.unit`. Le code repris est la valeur elle-même, pour que chaque article reste valide sans réécriture ; le libellé est la même valeur. Les sous-familles sans famille vont sous « Autres » (CD3). Unités de base ajoutées si absentes : heure, demi-journée, jour, semaine, mois, an, personne, poste, forfait, unité, page. La règle de taxe par défaut (TVA 18 % et centimes additionnels) est complétée avec `treatment = STANDARD` ; s'y ajoutent « Exonéré » et « Hors champ » à 0 %.

**Routes.**

| Route | Rôle |
| --- | --- |
| `GET /billing/catalog/references` | Toutes les listes en un appel, pour remplir le formulaire d'article : familles avec leurs sous-familles, unités, règles de taxe applicables aujourd'hui. Paramètre `includeInactive` (faux par défaut) |
| `GET`, `POST /billing/catalog/categories` | Lister, créer une famille |
| `PUT /billing/catalog/categories/{code}` | Modifier le libellé, l'ordre, le compte de produit (le code ne change pas) |
| `PATCH /billing/catalog/categories/{code}/activate`, `/deactivate` | Afficher ou retirer des choix |
| `DELETE /billing/catalog/categories/{code}` | Seulement si aucun article ni sous-famille ne l'emploie, sinon `REFERENCE_UTILISEE` |
| `GET`, `POST /billing/catalog/categories/{code}/subcategories` | Sous-familles d'une famille |
| Mêmes routes pour `/billing/catalog/units` | Unités |
| `GET`, `POST /billing/tax-rules`, `PUT /billing/tax-rules/{code}`, `.../activate`, `.../deactivate` | Règles de taxe. `PUT` ne change que libellés et mentions ; un changement de taux passe par `POST /billing/tax-rules/{code}/successor` (nouvelle règle datée, l'ancienne s'arrête la veille) |
| `POST /billing/catalog/references/merge` | Fusionner deux entrées (faute de frappe) : les articles passent sur le code retenu, l'autre est désactivé |

Lecture ouverte aux droits de consultation du catalogue ; gestion réservée à un nouveau droit `BILLING_CATALOG_REFERENCES` (attribué par la migration aux rôles qui gèrent déjà le catalogue). Règles de taxe : droit `BILLING_TAX_RULES`.

**Article du catalogue.** À la création et à la mise à jour, famille, sous-famille, unité et taxe sont contrôlées contre les listes (mode souple ou strict, CD2). La réponse d'un article garde `category`, `subcategory`, `unit`, `taxRuleCode` et ajoute `categoryLabel`, `subcategoryLabel`, `unitLabel`, `taxRule` (code, libellé, taux, traitement). Filtres de la liste et de `lookup` par code de famille et de sous-famille. Import CSV : mêmes contrôles, erreurs par ligne.

**Paramètre.** `billing.catalog.reference-lists.strict` (BOOLEAN, LIVE, faux).

**Tests.** Reprise des valeurs existantes ; mode souple qui crée l'entrée ; mode strict qui refuse ; suppression refusée d'une entrée utilisée ; fusion ; successeur d'une taxe ; route `references` complète et triée.

## C1. Taxe de l'article appliquée

- Le calcul de ligne (création, mise à jour, simulation, saisie d'un montant ventilé) prend la règle de taxe de la ligne, reprise de l'article et modifiable sur la ligne avec le droit de dérogation existant. Sans règle, le profil par défaut, comme aujourd'hui.
- `EXEMPT` et `OUT_OF_SCOPE` : pas de TVA ni de centimes additionnels ; la mention et le motif d'exonération s'impriment sous le tableau du PDF, une fois par motif.
- Le tableau des taxes de la pièce ventile par taux (bases et montants), ce qui sert déjà à la comptabilité (`4431` par taux).
- Les pièces déjà émises ne sont pas recalculées.
- Tests : ligne exonérée, pièce mêlant deux taux, simulation identique à l'enregistrement, écritures par taux.

## C2. WhatsApp pour devis et proforma

- Paramètres de modèles par type : `billing.whatsapp.template.quote`, `.proforma`, `.invoice`, `.credit-note`, `.statement` (noms des modèles validés chez Meta via Infobip), en plus des deux existants.
- L'envoi choisit le modèle par type de pièce ; sans modèle configuré pour ce type, le canal WhatsApp est refusé avec `MODELE_WHATSAPP_ABSENT`, le courriel part quand même si demandé.
- Le texte des modèles à faire valider est donné dans la doc du lot (variables : nom du client, numéro, montant, date de validité).
- Tests : choix du modèle par type, refus sans modèle, envoi mixte courriel et WhatsApp.

## C3. Dérogation de remise approuvée

- À l'émission (et à l'enregistrement d'une révision de devis), si une ligne passe sous le prix plancher, sous la marge minimale ou au-dessus de la remise maximale de l'article : la pièce passe en `PENDING_APPROVAL` (drapeau `discountApprovalPending`, le statut reste `DRAFT`) et une demande `BILLING_DISCOUNT_OVERRIDE` part au N+1 avec le détail des écarts.
- Accord : l'émission se fait, l'approbateur est noté sur la pièce et dans le visa. Refus : la pièce reste en brouillon avec le motif. Toute modification des lignes annule la demande en cours.
- Le droit de dérogation actuel (`BILLING_PRICE_OVERRIDE`) devient le droit d'approuver sans circuit, réservé aux N+ (paramètre `billing.discount.override-mode` : `APPROVAL` par défaut, `PERMISSION` pour l'ancien comportement).
- Tests : demande créée, émission bloquée, accord puis émission, refus, annulation à la modification.

## C4. Bon de livraison

- Nouveau type `DELIVERY_NOTE`, non fiscal, numérotation propre (`BL-`), tiré d'un devis accepté ou d'une facture : `POST /billing/quotes/{n}/delivery-notes`, `POST /billing/invoices/{n}/delivery-notes` avec les quantités livrées par ligne.
- Reste à livrer par ligne de la pièce d'origine, `GET /billing/documents/{n}/delivery-status`. Livraison au-delà du reste refusée.
- `PATCH /billing/delivery-notes/{n}/deliver` (sortie de stock si `billing.stock.issue-on = DELIVERY`, CD7), `.../cancel` (remise en stock), dépôt du bon signé en GED (`.../signed-copy`).
- Facture tirée des bons livrés : `POST /billing/delivery-notes/invoice` (plusieurs bons d'un même client en une facture).
- PDF du bon : sans prix par défaut, avec prix sur option.
- Tests : livraisons partielles, refus au-delà du reste, sortie de stock à la livraison, facture de plusieurs bons, annulation.

## C5. Statut réel des courriels

- Le journal des envois (`billing_document_sending`) est relié à l'entrée du journal des courriels du socle ; la lecture des envois remonte `QUEUED`, `SENT`, `DELIVERED`, `FAILED`, `BOUNCED` avec la raison.
- Mise à jour par le traitement qui écrit déjà le journal des courriels, pas de nouvelle tâche planifiée.
- Tests : envoi réussi, envoi en échec avec raison.

## C6. Retenues à la source

- Liste de référence `billing_withholding_type` (même gestion que C0) : code, libellé, taux par défaut, base (`TTC`, `HT`, `TVA`), compte comptable (CD5), active. Reprise : « Retenue de TVA » (100 % de la TVA) et « Précompte » (taux à confirmer).
- À l'encaissement d'une facture : retenues saisies (type, montant proposé par le taux, modifiable, référence de l'attestation, fichier joint en GED). La facture est soldée quand paiements + retenues = total.
- Une retenue sans attestation reste « attestation attendue » ; liste de suivi `GET /billing/withholdings?certificate=missing`.
- Écriture : débit du compte de la retenue, crédit `411` du client.
- Relevé client et balance âgée tiennent compte des retenues.
- Tests : facture soldée par paiement et retenue, écriture, suivi des attestations, annulation de la retenue.

## C7. Dépôts de garantie

- Table `billing_security_deposit` : client, pièce d'origine, montant, devise, statut (`HELD`, `PARTIALLY_RETAINED`, `REFUNDED`, `RETAINED`, `APPLIED`), mouvements.
- Encaissement : crédit `165` (paramètre `accounting.sales.security-deposit-account`), jamais en produit.
- Sortie : restitution (paiement sortant par caisse ou banque, avec l'approbation de caisse existante), retenue motivée (produit ou facture de dégâts), imputation sur la dernière facture.
- Abonnements (CD6) : la ligne de caution crée le dépôt ; la rubrique de sortie `DEPOSIT_SETTLEMENT` ouvre la restitution. Reprise des cautions déjà encaissées par écriture de reclassement.
- Routes : `GET /billing/security-deposits`, `POST .../{id}/refund`, `.../retain`, `.../apply`.
- Tests : encaissement en `165`, restitution partielle et retenue, imputation, sortie d'abonnement, reprise.

## C8. Tarifs négociés par client

- Table `billing_customer_price` : type et code du client (organisation ou membre), article, prix fixe ou taux de remise, devise, dates, référence du contrat, auteur.
- Résolution du prix selon CD8 ; la réponse de simulation et de recherche d'article indique la source (`CONTRACT`, `SEGMENT`, `ITEM`).
- Routes : `GET`, `POST /billing/customers/{type}/{code}/prices`, `PUT`, fin de validité ; import CSV.
- Tests : priorité contrat, dates, segment quand le contrat a expiré.

## C9. Paiements non affectés

- `GET /billing/unallocated-payments/suggestions` : pour chaque somme en attente (`4719`), les factures ouvertes candidates notées (même client, même montant, référence trouvée dans le libellé, échéance proche).
- `POST /billing/unallocated-payments/{id}/allocate` vers une ou plusieurs factures, avec le reliquat laissé en attente.
- Tests : proposition exacte, affectation partagée, reliquat.

## C10. Débours

- Type de ligne `DISBURSEMENT` : hors TVA, hors remise globale, hors chiffre d'affaires, compte `billing.disbursement.account` (CD9).
- PDF : rubrique « Débours » sous le total HT, avant le total à payer.
- Tests : taxes sans les débours, écriture au compte de tiers, avoir d'un débours.

## C11. Réclamations

- Table `billing_claim` : facture, lignes visées, motif (liste de référence), montant demandé, statut (`OPEN`, `ACCEPTED`, `PARTIALLY_ACCEPTED`, `REJECTED`), décision motivée, pièces jointes.
- Ouverture d'une réclamation : la facture passe en litige par le mécanisme du lot F19 (relances suspendues). Acceptation : avoir des lignes ou du montant accordé. Refus : motif envoyé au client. Clôture : fin du litige, relances reprises.
- Routes back-office `/billing/claims`, ouverture côté client en C16.
- Tests : avoir à l'acceptation, reprise des relances, refus motivé.

## C12. Recouvrement

- Promesse de paiement sur une facture (date, montant, canal, auteur) : relances suspendues jusqu'à la date, reprise automatique si rien n'est arrivé.
- Plan de règlement négocié : transforme le reste dû en échéancier (lot B9), avec accord du N+1 au-delà d'une durée paramétrée (`billing.collection.plan-approval-months`).
- Palier de relance WhatsApp (modèle « rappel de paiement » existant) dans les paliers de relance.
- Tests : suspension et reprise, plan transformé en échéancier, relance WhatsApp.

## C13. Facturation groupée périodique

- Lancement par période : collecte des consommations non facturées par client (réservations, impressions, ventes de la marketplace, services à l'acte), aperçu par client, exclusions, puis émission en lot.
- Un lancement est idempotent : une consommation facturée porte le numéro de sa facture et ne revient plus.
- Routes : `POST /billing/batch-runs` (aperçu), `GET /billing/batch-runs/{id}`, `POST .../{id}/issue`, `POST .../{id}/cancel`.
- Tests : aperçu, émission, second lancement sans doublon, exclusion.

## C14. Devis types et lots d'articles

- Modèles de devis (`billing_quote_template`) : lignes, conditions, validité ; `POST /billing/quotes/from-template/{code}`.
- Articles composés : un article du catalogue liste ses composants (quantité) ; vendu à prix global, ventilé au prorata pour les comptes de produit, stock sorti par composant.
- Tests : devis créé du modèle, ventilation, sortie de stock des composants.

## C15. Tableau de bord commercial

- `GET /billing/dashboard?from=&to=` : devis émis, acceptés, taux de transformation, délai moyen d'acceptation, délai moyen de paiement (DSO), prévision d'encaissement par semaine depuis les échéances, affaires par étape et montant pondéré, top clients, par devise.
- Tests : recoupement avec les pièces d'un jeu de données.

## C16. Espace client

- Routes du portail client authentifié (pas de lien à jeton) : liste des devis, factures, proformas et relevés du client ; accepter ou refuser un devis, le signer ; payer une facture, une proforma ou un acompte par mobile money (PawaPay, déjà intégré) ; télécharger les PDF ; ouvrir une réclamation (C11).
- Un paiement de proforma devient acompte du devis (règle du lot B5).
- Tests : un client ne voit que ses pièces, acceptation, paiement imputé, réclamation ouverte.
