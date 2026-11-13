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

- Constat : la caution d'un abonnement est déjà facturée (ligne « Dépôt de garantie » de la première facture) et retenue sur le portefeuille, puis réglée à la résiliation (`SubscriptionDepositSettlement`). Mais la ligne était taxable et TTC (de la TVA extraite d'une caution) et la comptabilité la portait en produit `706`.
- Une ligne de caution (caution d'abonnement, ou article du catalogue marqué caution) n'est plus taxable et part au compte `165` (paramètre `accounting.sales.security-deposit-account`).
- Registre des cautions (`billing_security_deposit`) alimenté à l'émission de la facture ; mouvements une fois la facture soldée : restitution (D `165` / C `411`, le client devient créditeur et se rembourse par les circuits habituels), retenue motivée (D `165` / C `758`, paramètre `accounting.sales.deposit-retention-account`), imputation sur une facture ouverte du même client (D `165` / C `411` lettré avec elle).
- Une caution d'abonnement encore couverte par sa retenue de portefeuille se règle par la sortie d'abonnement, pas par le registre (refus `CAUTION_ABONNEMENT`), pour ne jamais la rendre deux fois.
- Point ouvert (CD6) : le circuit de la caution d'abonnement (portefeuille jamais crédité de la caution encaissée) est à revoir avec les abonnements ; les cautions déjà comptabilisées en `706` sont à reclasser par le comptable.
- Tests : inscription au registre, facture d'acompte écartée, retenue puis restitution, imputation, refus avant encaissement et pour une caution d'abonnement.

## C8. Tarifs négociés par client

- Table `billing_customer_price` : type et code du client, article, prix fixe ou taux de remise (l'un des deux), devise, dates, référence du contrat, motif, auteur.
- Ordre des prix (CD8) : contrat du client, puis prix du segment daté, puis prix de la fiche. Un taux négocié s'applique au prix du segment ou de la fiche. `GET /billing/catalog/{itemCode}/price` accepte `customerType` et `customerCode` ; la réponse porte `source = CONTRACT` quand le contrat s'applique (`reason` = référence du contrat).
- Routes : `GET`, `POST /billing/customers/{type}/{code}/prices` (`current=true` pour les tarifs en vigueur), `PATCH /billing/customers/{type}/{code}/prices/{id}/end`. Un nouveau tarif du même article arrête le précédent la veille ; un tarif qui commencerait le même jour qu'un autre est refusé.
- Tests : prix du contrat en premier, taux négocié, retour au prix du catalogue après la fin du contrat, prix ou taux exigé.

## C9. Paiements non affectés

- `GET /billing/unallocated-payments/suggestions` (`customerType`, `customerCode`, `limit`) : paiements réussis dont une partie n'est imputée à aucune facture (hors portefeuille), avec les factures ouvertes du même client notées sur 100 (même montant 50, numéro dans la référence du paiement 40, échéance à moins de 15 jours 10) et le montant proposé.
- `POST /billing/unallocated-payments/{transactionNumber}/allocate` : une ou plusieurs factures du même client ; chaque montant ne dépasse ni le reste dû ni l'attente ; le reliquat reste en attente.
- L'imputation est marquée `LATE` (`payment_allocation.allocation_mode`) : l'écriture du paiement la laisse au `4719`, son écriture propre (règle `SALES_LATE_ALLOCATION`) passe D `4719` / C `411` lettré avec la facture.
- Tests : classement des candidates, affectation partielle marquée `LATE`, refus au-delà de l'attente ou pour un autre client.

## C10. Débours

- Type de ligne `DISBURSEMENT` : jamais taxable (même si l'écran envoie `taxable: true`), hors remise globale (ajouté au total après les remises), compte de tiers `accounting.sales.disbursement-account` (`4718`, CD9), donc hors chiffre d'affaires.
- Un avoir d'un débours suit le même compte.
- Tests : TVA sans les débours, remise globale sans les débours, débours jamais taxé.

## C11. Réclamations

- Table `billing_claim` (motif, description, lignes visées, montant demandé, canal `STAFF` ou `PORTAL`, décision) et liste des motifs `billing_claim_reason` (erreur de prix, quantité, prestation non rendue, qualité, doublon, autre ; enrichie depuis l'écran).
- Une réclamation ouverte suspend les relances de la facture (nouveau point d'arrêt commun des relances, `DunningHold`). Une seule réclamation ouverte par facture.
- Acceptation : avoir du montant accordé (par défaut le montant demandé), appliqué sur le reste dû s'il le couvre ; `PARTIALLY_ACCEPTED` si moins que demandé. Refus : motif obligatoire, communiqué au client. Retrait possible. Les relances reprennent dès la clôture.
- Classer la facture en créance litigieuse (lot F19, compte `4161`) reste une décision comptable distincte.
- Routes : `POST /billing/documents/{n}/claims`, `GET /billing/claims` (`documentNumber`, client, `openOnly`), `GET /billing/claims/{n}`, `PATCH .../accept`, `.../reject`, `.../withdraw` ; motifs `GET`, `PUT /billing/claim-reasons`. Ouverture côté client au lot C16.
- Tests : relances suspendues, une seule réclamation ouverte, avoir partiel appliqué, refus motivé.

## C12. Recouvrement

- Promesse de paiement (`billing_payment_promise`) : date, montant (par défaut le reste dû), canal (`PHONE`, `EMAIL`, `WHATSAPP`, `VISIT`, `PORTAL`). Les relances attendent la date ; ensuite la promesse est tenue si le reste dû a baissé du montant promis, sinon rompue, et les relances reprennent. Une nouvelle promesse remplace celle en cours.
- Plan de règlement : le reste dû en échéances égales (la dernière porte l'arrondi), transformé en échéancier de la facture (lot B9). Au-delà de `billing.collection.plan-approval-months` mois (3 par défaut), il demande le droit d'approbation de la facturation (responsable).
- Palier de relance WhatsApp : canal `WHATSAPP` dans les politiques de relance, modèle `rappel_paiement` au numéro de la facture.
- Routes : `POST /billing/documents/{n}/payment-promises`, `GET /billing/payment-promises`, `PATCH /billing/payment-promises/{n}/cancel`, `POST /billing/documents/{n}/payment-plan`.
- Tests : suspension puis promesse rompue, promesse tenue, plan en échéances égales, plan long refusé sans le droit.

## C13. Facturation groupée périodique

- Les consommations à facturer sont les éléments facturables en attente (abonnements, options, dépassements, réservations...), sans facture.
- `POST /billing/batch-runs` (période, origines facultatives, titre) : aperçu client par client (organisation payeuse et devise comprises), total à facturer. Une consommation déjà retenue par un aperçu en cours n'entre pas dans un second.
- `PATCH /billing/batch-runs/{n}/exclude` : exclure ou réintégrer un client. `POST .../issue` : une facture émise par client non exclu, chacune dans sa transaction ; une erreur est notée sur la ligne du client sans arrêter les autres. `POST .../cancel` : abandon de l'aperçu. `GET /billing/batch-runs`, `GET .../{n}`.
- Une consommation facturée porte sa facture : un second lancement de la même période ne crée aucune facture de plus.
- Tests : regroupement par client, consommation non reprise par un second aperçu, exclusion, filtre par origine.

## C14. Devis types et lots d'articles

- Modèles de devis (`billing_quote_template`) : articles du catalogue et quantités, validité, conditions. `PUT /billing/quote-templates` (création ou remplacement), `GET /billing/quote-templates`, `GET .../{code}` ; `POST /billing/quotes/from-template/{code}` crée un devis en brouillon aux prix du client (contrat, segment, fiche).
- Articles composés (`billing_catalog_component`) : `PUT`, `GET /billing/catalog/{itemCode}/components` (prestation du catalogue ou article d'inventaire, quantité). Vendus à prix global ; en comptabilité, le produit se ventile entre les composants au prorata de leur valeur (prix du catalogue ou prix de vente d'inventaire), chacun sur son compte ; le stock sort composant d'inventaire par composant (facture ou bon de livraison).
- Tests : devis de quatre lignes tiré du modèle « Séminaire journée », ventilation au prorata.

## C15. Tableau de bord commercial

- `GET /billing/dashboard?from=&to=&currency=&weeks=` (par défaut le mois en cours, XAF, 8 semaines) : devis émis et transformés, taux de transformation, délai moyen du devis à sa facture ; factures, facturé, encaissé, reste dû, en retard, délai moyen de paiement (factures soldées de la période) ; prévision d'encaissement par semaine (reste dû à l'échéance, échéances d'échéanciers ; une échéance passée compte pour la semaine en cours) ; affaires par étape (montant attendu et pondéré par la probabilité) ; dix meilleurs clients. Une devise à la fois, jamais de cumul.
- Le délai d'acceptation d'un devis n'est pas mesuré : la date d'acceptation n'est pas conservée en base ; le délai du devis à la facture le remplace.
- Test : `BillingDashboardC15IT` sur le schéma de production.

## C16. Espace client

- Routes de l'espace client authentifié (pas de lien à jeton) sous `/client/billing` : `GET /quotes?type=QUOTE|PROFORMA_INVOICE`, `GET /quotes/{n}`, `GET /quotes/{n}/pdf`, `POST /quotes/{n}/accept`, `POST /quotes/{n}/reject`, `POST /proformas/{n}/pay/mobile-money` (le paiement devient un acompte du devis, lot B5), `GET /claim-reasons`, `POST /invoices/{n}/claims` (canal `PORTAL`), `GET /claims`, `POST /claims/{n}/withdraw`.
- Factures, paiement (mobile money, portefeuille, espèces) et relevés restaient déjà disponibles sur les routes existantes.
- Le membre ne voit que ses pièces, jamais un brouillon ; un devis déjà répondu ne s'accepte plus.
- Tests : acceptation de son devis seulement, brouillon et pièce d'un autre invisibles, réclamation par l'espace client, retrait de ses seules réclamations.
