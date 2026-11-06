# Erreurs de l'API : le vrai statut revient au client

Une erreur garde son statut. Le renvoi interne vers `/error` transformait toute erreur, 404 et 500
compris, en `401 Authentication is required`.

À faire côté frontend : ne plus traiter un `401` comme une erreur quelconque. Un `401` signifie
désormais uniquement que le jeton est absent, invalide ou expiré : on rafraîchit la session, ou on
renvoie vers la connexion. Les autres erreurs s'affichent avec leur propre statut.

## Contenus JSON

Les contenus JSON produits par la plateforme (événements d'abonnement, charges utiles) sont
désormais construits et échappés par Jackson. Une annulation d'abonnement avec un motif en texte
libre ne répond plus `500`. Aucun changement de contrat d'API.
