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

- Les modèles WhatsApp sont déjà tenus par usage dans le module WhatsApp (créés chez Infobip, approuvés par Meta). Trois usages s'ajoutent aux modèles proposés à la création : `QUOTE_AVAILABLE` (`devis_disponible`), `PROFORMA_AVAILABLE` (`proforma_disponible`), `CREDIT_NOTE_ISSUED` (`avoir_emis`). Aucun paramètre de plus.
- L'envoi choisit l'usage par type de pièce. Sans modèle approuvé pour cet usage, l'envoi WhatsApp de cette pièce est noté en échec avec un message clair (comportement du module WhatsApp) ; le courriel demandé en même temps part quand même.
- Tests : choix du modèle par type de pièce, modèles proposés à la création.

## C3. Dérogation de remise approuvée

- À l'émission, et à la révision d'un devis déjà proposé, une ligne sous le prix plancher, sous la marge minimale ou au-dessus de la remise maximale fait partir une demande `DISCOUNT_OVERRIDE` au centre de la facturation (circuit `BILLING_DISCOUNT_OVERRIDE`, N+1), avec le détail des écarts. La pièce reste en brouillon ; la réponse est un refus 409 `DEROGATION_EN_ATTENTE` qui donne le numéro de la demande (la demande, elle, est enregistrée).
- L'accord vaut pour l'offre approuvée : une empreinte des lignes le lie. L'émission suivante passe, avec le motif et l'approbateur notés. Une offre modifiée fait annuler la demande en cours et en envoie une nouvelle. L'émission n'est pas automatique à l'accord : le demandeur émet, ce qui garde les autres contrôles (stock, état de la pièce) au moment où la pièce engage.
- Refus : la pièce reste en brouillon avec le commentaire du responsable (`DEROGATION_REFUSEE`) ; un nouveau motif relance une demande.
- Sans motif : `DEROGATION_MOTIF_REQUIS`.
- Le titulaire du droit `BILLING:DISCOUNT_OVERRIDE` passe outre directement avec un motif. Paramètre `billing.discount.override-mode` : `APPROVAL` par défaut, `PERMISSION` pour l'ancien comportement (droit et motif, sans circuit).
- Tests : demande créée et pièce en attente, attente sans nouvelle demande, accord puis émission, nouvelle demande quand l'offre change, refus puis nouveau motif, droit de dérogation, mode `PERMISSION`.

## C4. Bon de livraison

- Pièce non fiscale tenue à part (`billing_delivery_note` et ses lignes), numérotée `BL-AAAAMM-000001`, tirée d'un devis accepté (aussi après conversion) ou d'une facture émise : `POST /billing/documents/{n}/delivery-notes` avec les quantités par numéro de ligne ; sans lignes, tout le reste à livrer.
- Reste à livrer par ligne : `GET /billing/documents/{n}/delivery-status` (livré, en brouillon, reste). Une quantité au-delà du reste est refusée, à la création comme à la livraison.
- `PATCH /billing/delivery-notes/{n}/deliver` : sortie de stock si `billing.stock.issue-on = DELIVERY` (CD7), réservations du devis consommées d'abord. `.../cancel` (motif requis) : la marchandise revient en stock ; un bon facturé ne s'annule pas (avoir sur la facture). `.../signed-copy` : référence de la copie signée déposée en GED.
- `POST /billing/delivery-notes/invoice` : facture en brouillon des bons livrés d'un même devis, aux quantités livrées (remise fixe de ligne et remise globale fixe au prorata). Le devis passe en `CONVERTED` à sa première facture. Les acomptes du devis ne sont pas repris : un devis avec acomptes se facture par sa conversion. Un bon tiré d'une facture ne se refacture pas.
- PDF : `GET /billing/delivery-notes/{n}/pdf`, sans prix par défaut (`showPrices` à la création), cases « Remis par » et « Reçu par le client ».
- Tests : livraisons partielles (3 puis 2), refus au-delà du reste, sortie de stock à chaque livraison, retour en stock à l'annulation, facture de deux bons, devis en brouillon refusé.

## C5. Statut réel des courriels

- Le courriel d'une pièce est noté dans le journal des courriels du socle avec la pièce concernée (`BILLING_DOCUMENT`, numéro). L'envoi garde le numéro du courriel ; la lecture des envois remonte son statut réel : `QUEUED` (en file ou en cours), `SENT` (remis au serveur d'envoi, avec la date), `FAILED` (avec la raison).
- Limite : le serveur d'envoi (Microsoft 365) ne remonte pas les rebonds qui arrivent après la remise ; un rebond tardif reste visible dans la boîte d'envoi seulement.
- Pas de nouvelle tâche planifiée : le statut est lu au moment de la consultation.
- Tests : envoi réussi avec sa date, envoi en échec avec la raison.

## C6. Retenues à la source

- Liste des types de retenue (`billing_withholding_type`) : code, libellé, taux (facultatif), base (`TTC`, `HT`, `TVA` de la facture), compte de la créance sur l'État (CD5). Reprise : « Retenue de TVA » (100 % de la TVA, `4449`) et « Précompte » (montant saisi, base HT, `4492`).
- À l'encaissement : `POST /billing/documents/{n}/withholdings` (type, montant ou calcul par le taux, date, référence de l'attestation). La retenue solde la facture à hauteur de son montant, comme un règlement ; elle ne dépasse pas le reste dû.
- Attestation : `PATCH /billing/withholdings/{n}/certificate` (référence et fichier en GED) ; suivi `GET /billing/withholdings?certificate=missing`.
- Annulation (erreur de saisie) : `PATCH /billing/withholdings/{n}/cancel`, la facture redevient due d'autant.
- Écriture (règle `SALES_WITHHOLDING`, événement `WITHHOLDING_RECORDED`) : débit du compte de la retenue, crédit `411` du client lettré avec la facture ; contre-passation à l'annulation. Relevé et balance âgée suivent le reste dû de la facture.
- Tests : facture soldée par la retenue de TVA, refus au-delà du reste dû, type sans taux, annulation, bases.

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
