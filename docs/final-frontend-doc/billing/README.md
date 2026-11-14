# billing : fonctionnement

> Rédigé d'après le code (refonte devis et facturation, lots B0 à B15, et compléments, lots C0 à C16, 13/11/2026). Routes exactes, droits et champs JSON : [routes.md](routes.md) et `routes/`. Règles communes : [README racine](../README.md).

## En bref

Le module produit et suit les pièces de facturation : devis, factures, factures d'acompte, proformas, avoirs, factures rectificatives, notes de débit. Il calcule les montants (TVA, centime additionnel, remises), contrôle prix et remises, **certifie** les factures (facture certifiée par notre système : numéro de certification, chaîne d'empreintes, signature), produit les PDF, envoie par courriel et WhatsApp, gère acomptes, échéanciers, relances, affaires, catalogue et objets à récupérer.

Toutes les routes `/sni/api/v1/billing/**` et `/sni/api/v1/admin/fiscal-integrity/**` sont du **back-office**. Seul point public : la signature d'un devis (`/public/quotes/sign/{token}`). Le membre voit ses factures par le module [portal](../portal/README.md).

Modules voisins : [approval](../approval/README.md) (centre d'approbation de la facturation), [payment](../payment/README.md) (encaissements), [company](../company/README.md) (conditions de l'organisation : exonération, échéance, plafond, blocage), [inventory](../inventory/README.md) (stock), [crm](../crm/README.md) (opportunités), [accounting](../accounting/README.md) (écritures), [subscription](../subscription/README.md), [booking](../booking/README.md), [marketplace](../marketplace/README.md), [verify](../verify/README.md).

## Domaines

| Domaine | Fichier | Contenu |
| --- | --- | --- |
| Statuts et cycle de vie | [cycle-de-vie.md](cycle-de-vie.md) | Statuts par type, transitions, **actions par état** |
| Devis | [devis-negociation.md](devis-negociation.md) | Révision par versions, frise de négociation, réactivation, signature, expiration |
| Acomptes | [acomptes.md](acomptes.md) | Plan d'acomptes, factures d'acompte, déduction sur la facture finale, remboursement |
| Proforma | [proforma.md](proforma.md) | Document sans valeur fiscale, transformation en devis, paiement en acompte |
| Modifier une facture | [modification-facture.md](modification-facture.md) | Demande de modification, approbation, version ou rectificative |
| Changer le client | [changement-client.md](changement-client.md) | Sur place ou avoirs et nouvelle facture, sort des paiements |
| Envois | [envois.md](envois.md) | Destinataires, copies, WhatsApp, journal, duplicata |
| Affaires | [affaires.md](affaires.md) | Fiche, rattachement des pièces, tableau, export |
| Saisie et simulation | [saisie-et-simulation.md](saisie-et-simulation.md) | Simulation ligne par ligne, montant ventilé, descriptions longues, auteur et visa |
| Stock | [stock.md](stock.md) | Disponibilités, réservation, sortie |
| États clients | [etats-clients.md](etats-clients.md) | Relevé, balance âgée, échéanciers |
| Catalogue | [catalogue.md](catalogue.md) | Prix par segment et datés, liens, CSV, marges |
| Listes de référence | [listes-reference.md](listes-reference.md) | **Familles, sous-familles, unités, taxes chargées depuis le backend** (C0) |
| Taxe de l'article, débours | [taxes-et-debours.md](taxes-et-debours.md) | Exonération par article, taux par ligne, débours hors TVA (C1, C10) |
| Bons de livraison | [livraisons.md](livraisons.md) | Livraisons partielles, reste à livrer, stock à la livraison, facture des quantités livrées (C4) |
| Retenues à la source | [retenues.md](retenues.md) | Retenue de TVA, précompte, attestations (C6) |
| Cautions | [cautions.md](cautions.md) | Dépôts de garantie : registre, restitution, retenue, imputation (C7) |
| Tarifs clients | [tarifs-clients.md](tarifs-clients.md) | Prix négociés par contrat (C8) |
| Paiements en attente | [paiements-en-attente.md](paiements-en-attente.md) | Affectation des sommes non imputées (C9) |
| Rattachement aux éléments | [rattachement-paiements.md](rattachement-paiements.md) | Paiement ou facture vers abonnement, pass, réservation, domiciliation ; effets ; annulation d'une affectation (R1 à R4) |
| Réclamations | [reclamations.md](reclamations.md) | Réclamation sur facture, avoir ou refus motivé (C11) |
| Recouvrement | [recouvrement.md](recouvrement.md) | Promesses de paiement, plans de règlement, relance WhatsApp (C12) |
| Facturation groupée | [facturation-groupee.md](facturation-groupee.md) | Une facture par client sur une période (C13) |
| Devis types, lots | [devis-types.md](devis-types.md) | Modèles de devis, articles composés (C14) |
| Tableau de bord | [tableau-de-bord.md](tableau-de-bord.md) | Indicateurs commerciaux (C15) |
| Erreurs | [erreurs.md](erreurs.md) | Codes stables `reason`, routes dépréciées |

## Notions

| Objet | Identifiant exposé | Ce que c'est |
| --- | --- | --- |
| Document (`BillingDocumentResponse`) | `documentNumber` | Une pièce ; type dans `documentType`. Aucun `id` numérique : toutes les routes prennent le numéro. |
| Numéro de certification | `fiscalNumber` | Attribué à la certification (`validate`). Format `FAC20260617-00001abc-BKTWK` : préfixe (`FAC` facture, `AVR` avoir, `REC` rectificative, `DBN` note de débit), date, séquence annuelle, clé, code machine. |
| Ligne | `lineOrder` | Pas d'identifiant propre ; on modifie, retire ou choisit une ligne par son rang. |
| Remise de document | liste `discounts` | Pourcentage ou montant, sur le TTC. |
| Acompte | `id` (`DepositResponse`) | Voir [acomptes](acomptes.md). L'ancien `advance` (un par document) reste lisible sur les pièces anciennes. |
| Version | `versionNumber` | Instantané **avant** chaque modification ; la version courante est la pièce elle-même. Chaque version garde son auteur, son motif et son origine. |
| Auteur | bloc `author` | Créateur et auteur de la version courante, repris du jeton ([saisie](saisie-et-simulation.md#auteur-et-visa-b12)). |
| Affaire | `code` (`AFF-AAAA-NNNNN`), champ `dealCode` des pièces | Voir [affaires](affaires.md). |
| Demande de modification | `id`, `approvalNumber` | Voir [modification](modification-facture.md). |
| Envoi | `id` | Ligne du journal des envois, voir [envois](envois.md). |
| Échéancier | `scheduleNumber`, `installmentNumber` | Voir [états clients](etats-clients.md#échéanciers). |
| Objet à récupérer | `recoverableNumber` | Objet remis au client (badge, clé, matériel) à reprendre. |
| Article du catalogue | `itemCode` | Voir [catalogue](catalogue.md). Dans une ligne : `itemCode` (alias `catalogSourceCode`). |
| Signature de devis | jeton de 64 caractères | Demande de signature électronique, valable 7 jours. |

**Numéro de document** : `<préfixe>-<type de client>-<AAAAMMJJ>-<séquence>`, par exemple `INV-MAN-20260904-00000018`. Préfixes : `QUO` devis, `PRF` proforma, `INV` facture (et facture d'acompte), `CRN` avoir, `REC` rectificative, `DBN` note de débit.

**Client** : `customerType` + `customerCode` (`MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY`). Le serveur relit la fiche et fige nom, courriel, téléphone, adresse, identifiants fiscaux. Sans client connu : `customerName` seul (type `MANUAL`).

**Organisation payeuse** : une facture de membre peut être émise pour une organisation (`companyCustomerCode`, `companyName`, `companyLinkSource`) : vide suit le mode de rattachement du membre, `NONE` force le membre seul.

**Ce que pèse un document** : utiliser `receivable` (créance en cours : facture émise, ni brouillon, ni annulée, ni réglée ; **une proforma n'en est jamais une**) et `customerImpact` (positif si le client doit, négatif pour un avoir disponible, zéro sinon), jamais `balanceDue`.

**Certification (`locked`)** : une pièce certifiée est immuable (montants, client, NIU, adresse, date, devise, lignes) ; on la corrige par avoir, rectificative ou [demande de modification](modification-facture.md).

## Parcours courants

Préfixe : `/sni/api/v1/billing`.

1. **Liste** : `GET /documents?type=&status=&customerType=&customerCode=&sourceType=&sourceCode=&fromDate=&toDate=&searchText=&page=&size=` (tri fixe `createdAt` décroissant). Afficher numéro, type, statut, client, total, solde, échéance, badge « certifiée » si `locked`, `fiscalNumber`.
2. **Créer** : `POST /quotes/manual`, `POST /invoices/manual`, `POST /proformas`, ou `POST /documents` avec `documentType` (et `companyCustomerCode`). Simuler à chaque saisie ([saisie et simulation](saisie-et-simulation.md)) ; vérifier le stock ([stock](stock.md)).
3. **Fiche** : `GET /documents/{n}` : en-tête, lignes, remises, taxes, clauses, totaux, bloc de certification, `author`, signature, objets à récupérer, notes internes. Onglets : versions, acomptes, envois, demandes de modification, affaire.
4. **Modifier un brouillon** : `PUT /documents/{n}` ; lignes par opérations `UPSERT` / `REMOVE` (`description` obligatoire même pour `REMOVE`), `discounts` et `clauses` remplacent l'existant. Un devis se révise ([négociation](devis-negociation.md)), une proforma se modifie à tout statut, une facture émise passe par une [demande de modification](modification-facture.md).
5. **Corriger le destinataire** : `PATCH /documents/{n}/recipient` avec les champs à changer et `reason`. Sur une pièce certifiée, nom, NIU et adresse de facturation sont refusés ; courriel, téléphone, catégorie, référence, livraison restent corrigeables. L'auteur de la correction est l'utilisateur connecté. Les envois suivants d'une pièce certifiée partent en duplicata daté.
6. **Émettre** : `PATCH /documents/{n}/issue` (brouillon seulement). Contrôle des remises (dérogation : `?discountOverrideReason=...` et droit `BILLING:DISCOUNT_OVERRIDE`), du stock pour une facture, déduction des acomptes encaissés, sortie de stock.
7. **Certifier** : `PATCH /documents/{n}/validate` (facture, avoir, rectificative, note de débit émis). **Irréversible** : confirmation explicite. Peut aussi se faire au paiement complet (voir [Effets automatiques](#effets-automatiques)).
8. **Envoyer** : `POST /documents/{n}/send` ([envois](envois.md)).
9. **PDF** : `GET /documents/{n}/pdf` (avec le jeton, via un `Blob`) : original et duplicata dans le back-office. Filigranes `BROUILLON`, `EN RETARD`, `ANNULÉ`. PDF d'une pièce certifiée figé à la première génération ; QR vers `/verify/doc/{documentNumber}`. Imprime « Établi par » et le visa (nom, fonction, date, signature).
10. **Encaisser** : `POST /invoices/{n}/pay` (droit `PAYMENT:PROCESS`), corps `paymentMethod`, `amount` (vide : le solde), `walletNumber`, `cashSessionNumber`, `idempotencyKey`. Une facture d'acompte s'encaisse de même ; une proforma ne s'encaisse pas directement ([proforma](proforma.md)).
11. **Annuler** : `DELETE /documents/{n}?reason=...` (droit `BILLING:CANCEL`) : annulation et archivage, jamais de suppression. Pièce certifiée avec encaissement : un avoir est créé, certifié et imputé. Avoir consommé : son imputation est défaite d'abord (refus s'il a été reversé au portefeuille). Devis : son stock réservé est libéré.
12. **Avoir** : `POST /invoices/{n}/credit-note` (`reason`, `amount`, `validateImmediately`, `applyImmediately`, `lines`) sur une facture, une rectificative ou une note de débit. Puis une seule voie : `PATCH /credit-notes/{n}/apply` (facture d'origine), `PATCH /credit-notes/{n}/apply-to/{facture}` (autre facture du même client), `POST /credit-notes/{n}/refund-to-wallet?reason=...` (portefeuille). L'avoir passe à `APPLIED` ou `REFUNDED`.
13. **Rectificative** : `POST /invoices/{n}/corrective` (facture certifiée) ; réponse `201` : la rectificative en brouillon.
14. **Depuis une autre source** : `POST /invoices/from-billable-items`, `POST /invoices/from-reservation` (`BOOKING` ou `EXTERNAL`), `POST /documents/{n}/duplicate`.
15. **Objets à récupérer** : `POST /documents/{n}/recoverables`, `GET .../recoverables?status=`, `PATCH .../recoverables/{r}/recover` (`partialQuantity` facultatif), `PATCH .../recoverables/{r}/write-off`. L'auteur de la récupération est l'utilisateur connecté (`recoveredBy`).
16. **Relances d'impayés** ([routes/DunningController.md](routes/DunningController.md)) : politiques par segment (`PUT /dunning/policies`), paliers (`REMINDER`, `FORMAL_NOTICE`, `GRACE_PERIOD`, `SUSPEND`, `HANDOVER`), journal (`/dunning/notices...`), lancement manuel (`POST /dunning/run`).
17. **Intégrité de la certification** : `GET /admin/fiscal-integrity/last-report`, `GET /admin/fiscal-integrity/check` (continuité de la numérotation, chaîne d'empreintes, signatures).

## Droits pour masquer les boutons

| Action | Droit (règle d'URL) | Contrôle supplémentaire |
| --- | --- | --- |
| Lire documents, PDF, versions, relevé, balance âgée, catalogue, échéancier, affaires, envois | `BILLING:READ` | |
| Créer, simuler, ventiler, vérifier, dupliquer, convertir, plan d'acomptes, proforma, signature, rectificative, échéancier, affaire | `BILLING:CREATE` | |
| Modifier, réviser, corriger le destinataire, émettre, certifier, actions de devis, demande de modification, changement de client | `BILLING:UPDATE` | |
| Dérogation de prix ou de remise | `BILLING:UPDATE` | `BILLING:DISCOUNT_OVERRIDE` (ou `ROLE_SUPER_ADMIN`) |
| Envoyer | `BILLING:SEND` | |
| Annuler, avoirs, annuler un échéancier ou un acompte | `BILLING:CANCEL` | |
| Encaisser | `PAYMENT:PROCESS` | |
| Dépasser le plafond d'encours | | `ROLE_SUPER_ADMIN` |
| Approuver une modification de facture | voir [approval](../approval/README.md) | N+1, N+2, `BILLING:APPROVE`, `BILLING:APPROVE_L2` |
| Relances | `BILLING:READ` / `BILLING:UPDATE` | rôles `ADMIN`, `SUPER_ADMIN` (lecture aussi `STAFF`) |
| Intégrité de la certification | `SYSTEM:SETTINGS` | rôles `ADMIN`, `SUPER_ADMIN` |

Le droit exact de chaque route figure dans [routes.md](routes.md) (généré).

## Pièges

- JSON strict : ne jamais renvoyer `BillingDocumentResponse` dans un `PUT`. Seules `simulate`, `simulate-line`, `allocate-amount` et `check-availability` tolèrent des champs en trop.
- `discountOverrideReason` (émission) et `reason` (annulation, reversement) sont des **paramètres de requête**.
- `billingAddressJson`, `deliveryAddressJson`, `bankDetailsJson`, `metadataJson` sont des chaînes contenant du JSON.
- `changedBy` du corps de correction du destinataire est ignoré : l'auteur vient du jeton.
- Montants à 4 décimales en réponse ; arrondir à l'affichage (XAF sans décimales).
- `convert-to-invoice` renvoie la facture, pas le devis : naviguer vers elle.
- Chaque création consomme un numéro : pour les essais, simuler.
- Tester le champ `reason` des erreurs, pas le texte ([erreurs](erreurs.md)).

## Effets automatiques

| Déclencheur | Moment | Effet |
| --- | --- | --- |
| `BillingOverdueWorker` | toutes les heures (`bokati.billing.workers.overdue-delay-ms`) | Factures émises échues avec un solde : `OVERDUE` ; alerte `BILLING_OVERDUE` sur `/topic/admin/alerts`. |
| `BillingQuoteExpiryWorker` | toutes les heures (`bokati.billing.workers.quote-expiry-delay-ms`) | Devis échus sans réponse : `EXPIRED` (acomptes non encaissés annulés, stock libéré) ; proformas échues : `EXPIRED` ; alerte `BILLING_QUOTE_EXPIRED`. |
| `PaymentScheduleOverdueWorker` | toutes les heures | Échéances dont la date est passée : `OVERDUE`. |
| `DunningPolicyWorker` | chaque jour à 08:30 | Paliers de relance des politiques actives. |
| `BillingPaymentReminderWorker` | chaque jour à 08:00 | Sans politique active : renvoi du courriel des factures en retard (3 fois au plus). |
| `ApprovalWorker` | heures ouvrées | Rappels et escalades des demandes d'approbation ([approval](../approval/README.md)). |
| `FiscalIntegrityWorker` | chaque jour à 02:00 | Vérification de la certification. |
| Paiement complet par une intention de paiement | à la réussite | La facture soldée émise est certifiée automatiquement, renvoyée par courriel, avec une confirmation de paiement. |
| Paiement imputé | à chaque imputation | L'échéancier de la facture et l'acompte d'une facture d'acompte suivent ; un paiement sur une proforma devient un acompte du devis. |
| Acceptation d'un devis | accept, signature, proforma transformée | Stock réservé. |
| Émission d'une facture | `issue` | Acomptes encaissés déduits, stock sorti, notification `BILLING_DOCUMENT_ISSUED` au membre. |
| Premier devis d'un client | création | Affaire créée ou rejointe. |
| Toute action | à chaque action | Événements dans la file sortante (`BILLING_DOCUMENT_*`, `QUOTE_*`, `CREDIT_NOTE_*`, `INVOICE_*`, `DEPOSITS_PLANNED`...), lus par les autres modules. |

## Écarts relevés

Les écarts E1 à E19 relevés avant la refonte sont réglés par les lots B0 à B15 ; le suivi détaillé est dans `docs/backend/plan-facturation-devis/ecarts.md`. Décision D3 validée : TVA portée par la facture finale ; en régime des encaissements, exigible au fur et à mesure que les acomptes sont déduits de la facture finale, pas à leur encaissement.
