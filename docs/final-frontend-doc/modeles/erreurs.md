# Codes d'erreur du centre des modèles

> Rédigé d'après le code (14/11/2026). Format commun des erreurs : voir [../README.md](../README.md). Le code se lit dans `errorCode` / la raison de la réponse ; le message est en français, à afficher tel quel.

| Code | Statut | Cas | Côté écran |
| --- | --- | --- | --- |
| `MODELE_INVALIDE` | 400 | Contenu refusé à l'enregistrement ou à la publication ; le message liste chaque erreur (« Bloc 3 (FIELDS) : de une à quatre colonnes ; ... ») | Afficher la liste, rester dans l'éditeur |
| `BROUILLON_ABSENT` | 400 | Publier ou supprimer sans brouillon | Désactiver le bouton quand `draft` est nul |
| `AUCUN_CONTENU` | 400 | Aperçu sans corps, sans brouillon ni version publiée | Envoyer le contenu en cours d'édition |
| `AUCUNE_VERSION_PUBLIEE` | 400 | Retour à l'original alors que rien n'est publié | Désactiver le bouton quand `published` est nul |
| `VERSION_BROUILLON` | 409 | Restaurer la version qui est le brouillon lui-même | Proposer seulement les versions publiées ou archivées |
| `APERCU_INDISPONIBLE` | 400 | Aperçu HTML ou envoi d'essai demandé pour une pièce PDF | Selon `family` |
| `ADRESSE_ABSENTE` | 400 | Envoi d'essai sans adresse sur le compte connecté | Inviter à compléter son profil |
| `ENVOI_IMPOSSIBLE` | 400 | L'envoi d'essai a échoué | Afficher le message |
| `AUCUN_CHAMP` | 400 | Saisie de champs sur une pièce dont le modèle publié n'en déclare pas | Masquer l'encart |
| `CHAMP_INVALIDE` | 400 | Valeur saisie refusée (type, liste) | Afficher le détail sous le champ |
| `CHAMPS_A_SAISIR` | 400 | Impression d'une pièce dont un champ obligatoire n'est pas saisi | Ouvrir la saisie des champs, puis relancer l'impression |
| (aucun code) | 404 | Modèle, version ou pièce introuvable | |
| (aucun code) | 403 | Droit `SYSTEM:DOCUMENT_TEMPLATES` absent (ou droit de la pièce pour la saisie) | |
