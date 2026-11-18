# Constat : facturation, paiement, abonnement

> Partie du [constat des listes de choix](README.md). Relevé du 18/11/2026 sur `feature/booking` (lecture du code, sans modification). Chemins relatifs à `src/main/java/com/sni/bokaticowork/features/`.

## En bref

| Module | Valeurs fixes non exposées | Recherche de choix manquante | Texte libre à mettre en liste | Recherche manquante | Automatisation manquante |
| --- | --- | --- | --- | --- | --- |
| Facturation | 11 | 9 | 6 | 7 | 5 |
| Paiement (caisse, PawaPay, portefeuille) | 7 | 9 | 8 | 6 | 2 |
| Abonnement (plans, promotions, pass, dépassements) | 2 | 9 | 6 | 8 | 4 |

À cela s'ajoute un manque d'administration critique : le formulaire des promotions (S1).

Constats communs :

- Aucun des trois modules ne rend ses valeurs fixes avec leurs libellés français ; seul modèle : `GET /inventory/reference/enums`.
- Aucune devise n'est contrôlée contre la table des devises (`/currencies` existe, personne ne la lit) ; des repli « XAF » sont en dur.
- Beaucoup de champs « qui a fait » (`createdBy`, `changedBy`, `requestedBy`…) sont envoyés par le client ; le paiement a déjà `actors.resolve(...)` pour lire l'utilisateur de la session.

Ce qui marche déjà et doit servir de modèle :

- **Facturation** :
  - listes de référence du catalogue, `/billing/catalog/references` (familles, unités, règles de taxe, mode souple ou strict) ;
  - motifs de réclamation, types de retenue, politiques de relance, modèles de devis ;
  - recherches `/billing/catalog/lookup?q=` et `/billing/elements/search?query=`.
- **Paiement** :
  - listes `CashReferenceValue` (`/cash-registers/reference-lists/{listCode}`) ;
  - natures de caisse et coupures ;
  - `/cash-registers/form-defaults` avec `choices` ;
  - `/payments/mobile-money/providers` ;
  - analyse automatique des anomalies.
- **Abonnement** :
  - `/subscriptions/entitlement-definitions/search/basic` et `/coupons/available` ;
  - résiliation avec catégorie de motif plus texte (le bon modèle) ;
  - tâches de report, d'avis, d'expiration et de renouvellement.
- **Recherches d'autres modules à brancher** :
  - `/customers/search/basic`, `/members/search/basic`, `/businesses/search/basic`, `/admin/users/search/by-name` ;
  - `/accounting/accounts?search=`, `/accounting/journals`, `/accounting/analytic/axes/{axis}/sections`, `/accounting/bank-accounts` ;
  - `SettingTriggers` pour les planifications réglables.

## Facturation

| Champ ou route | Nature | Constat | Proposition |
| --- | --- | --- | --- |
| `CreateBillingDocumentRequest.customerType` (`billing/dto/request/CreateBillingDocumentRequest.java:14`) | Valeurs fixes | Texte libre, alias traduits à la main (`BillingDocumentServiceImpl:3409-3440`) | Type de client (MEMBER, CUSTOMER, BUSINESS_ENTITY) dans `/billing/reference/enums` |
| `customerCode` (:15) | Recherche | `/billing/elements/search` sans pagination | Pagination ; documenter comme sélecteur de client |
| `currency` (factures, affaires, articles) | Recherche | Mise en majuscules seulement ; « XAF » en dur (:1627, :1785) | Contrôle contre `/currencies` ; défaut par paramètre |
| `language` (:41) | Valeurs fixes | Texte libre, défaut « fr » | Langues admises dans les valeurs fixes |
| `salespersonCode` (:39) | Recherche | Texte libre | Recherche d'utilisateur ; défaut : l'utilisateur connecté |
| `projectCode` (:38) | Recherche | Texte libre ou généré | Sections analytiques |
| `sourceType` (document et lignes) | Valeurs fixes | Ensemble en dur (`ElementRef.TYPES`) | Énumération et libellés |
| `paymentInstructions`, `bankDetailsJson` (:33-34) | Recherche | Texte ou JSON saisi | Choix d'un compte bancaire ; JSON construit par le serveur |
| Clauses et conditions (`CreateBillingDocumentClauseRequest`, `terms`) | Texte en liste | Saisies à chaque document | Bibliothèque de clauses de l'administrateur (modèle des devis) |
| `vatRate`, `additionalCentRate` des lignes | Recherche | Taux bruts | `taxRuleCode` résolu par la règle de taxe (`/billing/tax-rules`) |
| `unit`, `category` des lignes | Texte en liste | Libres, jamais contrôlés hors articles du catalogue | Résolution par les listes du catalogue (souple ou strict) |
| `discountPolicy`, `billingMode` des articles | Valeurs fixes | Chaînes contrôlées par ensembles cachés (`ServiceCatalogServiceImpl:233, 245`) | Énumérations, libellés dans `/billing/catalog/references` |
| `tags` des articles | Texte en liste | Liste libre | Suggestions des étiquettes en usage, ou liste |
| `DealRequest.owner`, `analyticSection` | Recherche | Texte libre | Recherche d'utilisateur ; sections analytiques |
| `CreateCreditNoteRequest.reason` | Texte en liste | Obligatoire, libre | `reasonCode` (liste de l'administrateur) et précision |
| Motifs de modification de facture, de changement de client, d'annulation de bon de livraison et de retenue | Texte en liste | Libres | Une liste de motifs de facturation par contexte |
| `PromiseRequest.channel` | Valeurs fixes | Ensemble caché (`CollectionService:55`) | Énumération et libellés |
| `MovementRequest.kind`, motif de retenue de caution | Valeurs fixes et texte | RELEASE, RETAIN, APPLY cachés ; motif libre | Énumération ; liste des motifs de retenue |
| `TypeRequest.base`, `account` des retenues | Valeurs fixes et recherche | TTC, HT, TVA cachés ; compte saisi | Énumération ; recherche de compte |
| `SendDocumentRequest.channels`, `PreviewRequest.sourceTypes` | Valeurs fixes | Chaînes | Énumérations |
| Énumérations de facturation (type, statut, ligne, remise, étape commerciale, acompte, traitement fiscal, relance…) | Valeurs fixes | Aucune route | `GET /billing/reference/enums[/{group}]` |
| `/billing/batch-runs`, `/claims`, `/payment-promises`, `/delivery-notes`, `/security-deposits`, `/withholdings`, `/dunning/notices`, `/catalog/lookup`, `/elements/search` | Recherche | Listes sans pagination ni filtres suffisants | Pagination, `q`, dates, statut |
| Facturation groupée | Automatisation | Aperçu et émission manuels, aucune tâche | Aperçu mensuel planifié ; émission automatique en option |
| Paiements non affectés | Automatisation | Suggestions seulement | Affectation automatique si une seule correspondance exacte (réglage) |
| Coordonnées bancaires | Automatisation | Saisies à chaque document | Compte par défaut des paramètres |
| Attestations de retenue manquantes | Automatisation | Simple indicateur | Relances automatiques |
| Restitution de caution | Automatisation | Saisie à la main | Proposée à la fin du contrat ou de l'abonnement |

## Paiement (caisse, PawaPay, portefeuille)

| Champ ou route | Nature | Constat | Proposition |
| --- | --- | --- | --- |
| `CreateCashRegisterRequest.locationCode`, `businessEntityCode`, `deviceCode` | Recherche | Texte libre (`/businesses/search/basic` existe, non branché) | Sites, entreprises et appareils dans les `choices` de `form-defaults` |
| Devises (caisse, recharge, intention, demande d'espèces) | Recherche | Libres, défaut XAF | Contrôle et `choices` depuis `/currencies` |
| Courriels (responsable, titulaire, suppléants, alertes, convoyeur, témoin, délégué) | Recherche | Saisis | Recherche d'utilisateur filtrée sur les droits de caisse |
| `flowCategory` des mouvements et pièces | Texte en liste | Libre sans nature | Nature obligatoire (liste déjà administrée) |
| `referenceType`, `counterpartyType` | Valeurs fixes | Chaînes | Énumérations |
| `counterpartyCode`, `supplierCode`, `journalCode`, `vatCode`, compte et section des natures | Recherche | Libres | Recherches tiers, fournisseurs, journaux, règles de taxe, comptes, sections |
| `subCategory`, `tags` des mouvements | Texte en liste | Libres | Liste `MOVEMENT_SUBCATEGORY` |
| Motifs (demande d'espèces, remboursement, annulation d'intention, résolution forcée PawaPay) | Texte en liste | Libres | Modèle `reasonCode` et précision ; listes dédiées (la résolution PawaPay déplace de l'argent) |
| `bankName`, `depositedIn` | Texte en liste | Libres | Liste `BANK` (existe), liste des coffres |
| Droits délégables | Valeurs fixes | Ensemble caché (`CashStaffServiceImpl:83`) | Exposés avec libellés |
| `WalletTopUpRequest.ownerType`, `purpose`, `sourceType` | Valeurs fixes | Chaînes | Énumérations |
| Énumérations du paiement | Valeurs fixes | Trois seulement via `form-defaults` | `GET /payments/reference/enums[/{group}]` |
| Statuts des dépôts mobile money | Valeurs fixes | Chaînes, ensembles en dur | Énumération pour le filtre |
| Codes des listes de caisse | Valeurs fixes | Sept codes dispersés ; une liste vide est invisible | Registre des listes (code, libellé, usage), listes vides comprises |
| `/wallets`, `/cash-registers/transfers`, `/absences`, `/delegations`, `/variance-reviews`, `/payments/cash-declarations` | Recherche | Listes sans pagination ni recherche | Filtres, dates, `q`, pagination |
| `/payments/mobile-money/deposits` | Recherche | Doublon incomplet de `/operations` | À déprécier |
| Champs « qui a fait » (`createdBy`, `openedBy`, `requestedBy`…) | Automatisation | Envoyés par le client | Lus dans la session, retirés des requêtes |
| Réapprovisionnement de la petite caisse | Automatisation | Suggestion, demande manuelle | Demande créée sous le seuil |

## Abonnement (plans, promotions, pass, dépassements)

| Champ ou route | Nature | Constat | Proposition |
| --- | --- | --- | --- |
| Promotions (`CreatePromotionRequest`, 8 champs) | Administration manquante, **critique** | Type, cumul, exclusivité, priorité, plafonds, budget, compte de contrepartie, conditions, récompenses, audiences et grilles ne s'écrivent que par la base ; ni lecture unitaire, ni modification, ni désactivation | Administration complète, valeurs fixes exposées |
| `metadataJson` des promotions | Texte en liste | Seul endroit des réglages | Champs typés |
| Activation des promotions | Automatisation | Manuelle ; la tâche ne fait qu'expirer | Activation à la date de début ; étape d'approbation |
| Compte de contrepartie | Recherche | Non modifiable | Recherche de compte |
| `promotionCode` (lots de coupons, parrainage) | Recherche | Filtres séparés, sans `q` | `/promotions/search/basic?q=&status=ACTIVE` |
| `channel` des lots de coupons | Texte en liste | Non contrôlé | Liste des canaux de vente |
| `AddBeneficiaryRequest.subscriberType` | Valeurs fixes | Chaîne | Énumération ; recherche du bénéficiaire |
| `planCode` (souscription, devis) | Recherche | Pas de `q` ni de raccourci « publiés » | `/subscriptions/plans/search/basic?q=` |
| `planVersionId` (souscription, dépassement, option, pass) | Recherche | Identifiants bruts | `/subscriptions/plan-versions/search?q=&planType=&status=` |
| `entitlementCode` des dépassements | Recherche | Non limité aux droits de la version | Filtre par version |
| Devises (prix, dépassements, achat de pass) | Recherche | Non contrôlées | `/currencies` |
| `icon`, titres des avantages, `termsJson` | Texte en liste | Saisis ou JSON | Catalogue d'icônes, bibliothèque d'avantages, conditions typées |
| Motifs (statut, pause, changement, annulation et transfert de pass, devis, dérivation) | Texte en liste | Libres (sauf la résiliation) | Catégorie et précision, comme la résiliation |
| `changedBy`, `requestedBy`, `validatedBy` | Automatisation | Envoyés par le client | Lus dans la session |
| `prorationAmount` | Automatisation | Saisi | Calculé par le serveur (l'aperçu existe) |
| Validation de pass (`locationCode`, `resourceCode`, `resourceTypeCode`, `bearerType`) | Recherche | Chaînes | Recherches des ressources et types ; énumération |
| `walletNumber` du mandat, destinataire d'un avis, `proposalNumber` | Recherche et automatisation | Saisis | Portefeuille du souscripteur, contact du canal, recherche des propositions |
| Énumérations de l'abonnement (cycle, souscripteur, type de plan, public, pass, catégories, dépassement, coupon, siège, changement, dérivation, résiliation, prorata…) | Valeurs fixes | Aucune route | `GET /subscriptions/reference/enums[/{group}]` |
| `/subscription-overage/policies` et `/charges`, `/subscription-rollovers`, `/referrals/programs`, `/subscriptions/terminations`, `/blocked`, `/quotes`, `/derivations`, `/subscriptions`, `/passes`, `/entitlement-definitions/search/basic` | Recherche | Listes sans pagination, filtres incomplets, pas de `q` ; politiques de dépassement non modifiables | Pagination, `q`, filtres ; modification et désactivation des politiques |

## Priorités

1. **Administration des promotions** : conditions, récompenses, audiences, budget, priorité, cumul, compte ; activation à la date de début ; approbation.
2. **Valeurs fixes avec libellés** dans les trois modules, une route commune, et les ensembles cachés passés en énumérations.
3. **Lignes de facture branchées sur les listes du catalogue** : règle de taxe, unité, famille.
4. **Devises contrôlées**, défaut par paramètre.
5. **Codes de motif** partout (paiement, facturation, abonnement), avec un registre des listes qui montre aussi les listes vides.
6. **Codes comptables** choisis par recherche (comptes, journaux, sections).
7. **Versions de plan** choisies par recherche, et non plus par identifiant.
8. **Champs « qui a fait » lus dans la session** ; prorata et destinataires calculés par le serveur.
9. **Pagination et recherche** sur les listes de la facturation, du paiement et de l'abonnement.
10. **Automatisations** : facturation groupée planifiée, affectation des paiements exacts, coordonnées bancaires par défaut, relances des attestations, réapprovisionnement de la petite caisse.
