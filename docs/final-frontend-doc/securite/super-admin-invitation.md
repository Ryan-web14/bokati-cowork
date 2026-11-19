# Super-administrateur par invitation (19/11/2026)

> Retour au [README sécurité](README.md).

Un `ADMIN` ne peut pas modifier son propre rôle ni donner des droits qu'il n'a pas. Pour créer un premier `SUPER_ADMIN` en production, le serveur porte une **invitation à usage unique** posée par la migration (dev V398, prod V392) pour le compte `admin@sni-cg.com`.

## Parcours

1. Ce compte se connecte normalement (`POST /sni/api/v1/auth/login`). La réponse porte en plus `superAdminSetupUrl` ; le même lien part par courriel à cette adresse.
2. Le frontend affiche un bandeau « Créer le super-administrateur » qui ouvre le lien (page du serveur, pas un écran du frontend).
3. La page demande prénom, nom, adresse, mot de passe et confirmation. Elle crée le compte, lui donne le rôle `SUPER_ADMIN`, l'active, puis clôt l'invitation.
4. Se connecter ensuite avec ce nouveau compte.

## Règles

- Le lien vaut 30 minutes et ne sert qu'une fois. Une nouvelle connexion du compte invité en génère un autre (l'ancien ne vaut plus) tant que l'invitation n'a pas servi.
- Seule l'empreinte SHA-256 du jeton est gardée en base (`super_admin_invitation`).
- Une fois utilisée, l'invitation est close : plus aucun lien, plus de champ `superAdminSetupUrl` à la connexion.
- L'adresse du nouveau compte ne doit pas déjà exister ; le mot de passe suit la règle habituelle.
- Les autres comptes ne reçoivent jamais `superAdminSetupUrl` (champ absent).
