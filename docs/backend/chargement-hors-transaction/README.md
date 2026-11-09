# Erreurs 500 de chargement hors transaction

Chantier des 2026-11-08 et 2026-11-09. Le premier jour, des 500 ont été signalés un par un : devis,
dérivations, contrats d'organisation, relances. Le second jour, une revue a couvert les 196 contrôleurs.

## La cause

`spring.jpa.open-in-view` vaut `false` : la session Hibernate se ferme quand la méthode `@Transactional`
du service se termine. Toute association paresseuse lue **après** coupe court :
`LazyInitializationException: Could not initialize proxy ... - no session`, renvoyée en 500. Les
associations paresseuses sont les `@ManyToOne(fetch = LAZY)` et toutes les collections.

Les quatre formes rencontrées :

1. **Vue construite dans le contrôleur.** Par exemple `service.list().stream().map(XView::of)`, où
   `XView.of` lit `entity.getPlan().getCode()`. Même chose pour l'entité rendue par un create,
   approve ou reject.
2. **Objet détaché passé à un service.** Le contrôleur lit une entité dans une transaction, puis la
   passe à un autre service, qui lit ses liens. Par exemple, l'organisation passée à
   `CompanyContractsService`.
3. **Entité renvoyée telle quelle en JSON.** Jackson parcourt alors ses liens, comme pour les
   événements de pass.
4. **Collection paresseuse lue par une vue.** Par exemple les étapes d'une politique de relance, ou
   les pièces de sortie d'un préavis.

## Les corrections, par ordre de préférence

1. **`@EntityGraph` sur la requête que la vue utilise.** Les noms d'attributs doivent être exacts ;
   un test d'intégration qui exécute la requête les valide. Deux règles à respecter :
   - Ne jamais mettre deux collections `List` dans un même graphe (`MultipleBagFetchException`).
   - Ne pas mettre de collection dans le graphe d'une requête paginée : Hibernate paginerait en
     mémoire. Charger la collection dans le service à la place.
2. **`Hibernate.initialize(...)` dans le service avant de rendre.** C'est la solution pour une requête
   native, pour une entité rendue par une écriture, ou quand un graphe gênerait un verrou pris
   ensuite (dossier de conformité).
3. **Relire l'objet reçu dans la transaction du service.** C'est le cas 2 : par exemple
   `customerRepository.findById(company.getId())`.
4. **Construire la vue dans le service, ou une vue dédiée plutôt que l'entité.**

`open-in-view` et `hibernate.enable_lazy_load_no_trans` ne sont pas activés : ils cachent le problème
et multiplient les requêtes.

## Ce qui a été corrigé

| Domaine | Routes | Correction |
| --- | --- | --- |
| Devis d'abonnement | consultation, liste, devis d'un abonné, création avec version | graphe `planVersion.plan` ; plan initialisé à la création |
| Dérivations | liste, consultation, création, visa, refus, retour au catalogue, prix plancher | graphes ; `loaded(...)` dans le service |
| Contrats d'une organisation | liste et PDF (admin et portail) | organisation relue dans le service |
| Relances | notices, politiques | graphes `step` et `steps` |
| Préavis de résiliation | toutes les vues (admin et portail) | graphe `subscription` + `exitItems`, initialisation pour la requête native |
| Engagement | modification d'un engagement existant | abonnement relu dans le service |
| Mandats de prélèvement | révocation, réactivation | graphe `subscription` |
| Coupons | disponibles, révocation | graphe `promotion` + `batch` |
| Passes | événements, historique, renouvellement | vues dédiées au lieu des entités |
| Domiciliation | contrats, plis, immatriculations, services, adresses (admin et portail) | graphes |
| Conformité des portefeuilles | dossiers et gels | graphes sur les listes ; initialisation après verrou |
| Stock | suggestion de prélèvement, conversions d'unités | graphe `location` ; code article pris de l'article déjà lu |
| KYC | validation et rejet groupés d'un dossier | graphe `document` |
| Adresses | `GET` et `PUT /addresses/{id}` | service rendu transactionnel ; la modification n'était en plus jamais enregistrée |

Corrigés dans la foulée, hors de ce type d'erreur :
- devis : création en 500 (colonne `jsonb`) ;
- acceptation d'un devis par son auteur, refusée par la règle des quatre yeux (V283 / V279) ;
- page publique de vérification : une panne affichait « introuvable » ;
- achat d'un pass pour une organisation : il n'était pas fait en une seule transaction.

## Tests ajoutés

- `SubscriptionQuoteRepositoryIT`, `LazyViewGraphsIT`, `ComplianceLazyLoadingIT`,
  `LazyLoadingInventoryDocumentRepositoryIT` : ils exécutent chaque requête annotée (ce qui valide les
  noms d'attributs) et, quand c'est simple à préparer, vérifient `Hibernate.isInitialized(...)` après
  `entityManager.clear()`.
- `AddressServiceOutsideTransactionIT` appelle le service hors transaction, comme le contrôleur. Il
  échoue sans le correctif.
- Tests unitaires des relectures : `CompanyContractsServiceTest`, `SubscriptionCommitmentServiceTest`,
  `ClientSubscriptionSelfServiceTest`, `StockPlanningControllerTest`.

## Pour le code à venir

- Un contrôleur qui reçoit une entité ne lit que ses champs simples. S'il lui faut un lien, le
  service le charge ou rend une vue.
- Un service qui reçoit une entité d'un autre service la relit avant de lire ses liens.
- Ne jamais renvoyer une entité en JSON.
- Toute nouvelle `@EntityGraph` va dans un test d'intégration qui exécute la requête.
