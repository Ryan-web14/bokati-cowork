# Inscription, vérification de l'adresse et codes à usage unique

Ce que fait le système entre le moment où quelqu'un s'inscrit et celui où l'espace client
l'accepte · et ce que le support peut voir quand ça coince.

## Le parcours

1. `POST /auth/register` crée l'utilisateur, le client et le membre (`PENDING`), envoie un code à
   six chiffres par courriel et renvoie un `verificationToken`.
2. `POST /auth/ott/validate` avec le code et ce jeton · l'adresse est marquée vérifiée
   (`OTT_VERIFICATION`), le rôle `MEMBER` est donné, le compte est activé.
3. À partir de là, `/client/**` accepte le jeton d'accès.

Tant que l'étape 2 n'a pas eu lieu, **le login fonctionne mais le jeton ne porte aucun rôle**, et
`/client/**` répond 403. C'est voulu : le mot de passe prouve qu'on connaît le mot de passe, pas
qu'on possède l'adresse. La réponse du login le dit désormais explicitement :

```json
{
  "accessToken": "…",
  "refreshToken": "…",
  "emailVerified": false,
  "roles": []
}
```

Une interface qui reçoit `emailVerified: false` doit proposer la saisie du code, avec
`POST /auth/email/verify/resend` si besoin · et non pas appeler `/client/me`.

## L'activation par un administrateur

Un administrateur peut activer un membre sans attendre son code, par trois chemins :
`PATCH /members/{id}/status` avec `ACTIVE`, l'ouverture de l'accès portail, ou l'activation
explicite du compte portail. **Les trois font désormais la même chose** : rôle `MEMBER`, compte
activé, adresse marquée vérifiée avec `email_verified_by = ADMIN_ACTIVATION` (ou le nom de
l'administrateur quand il est connu).

Activer sans le code, c'est se porter garant de l'adresse. La colonne dit qui l'a fait, pour que
la vérification ait toujours un auteur.

Avant cette correction, le changement de statut rendait le membre `ACTIVE` sans donner le rôle ·
le membre se connectait, recevait un jeton sans rôle, et l'espace client lui répondait 403.

## Le fait, pas la déduction

`users.email_verified_at` et `users.email_verified_by` portent la vérification. L'espace client
en déduisait autrefois l'état depuis « compte non verrouillé », ce qui était vrai pour tout le
monde dès l'inscription. La migration `V237` (`V233` en prod) reconstitue l'historique : ceux qui
ont saisi un code, ceux qu'un administrateur a activés, le personnel.

## Consulter les codes · support

```
GET /sni/api/v1/admin/one-time-tokens?email=&used=&page=0&size=20
```

Réservé aux rôles `ADMIN` et `SUPER_ADMIN`. Renvoie, du plus récent au plus ancien :

| champ | sens |
|---|---|
| `token` | le code, tel qu'envoyé |
| `email`, `userId`, `firstname`, `lastname` | à qui |
| `createdAt`, `expiredAt` | quand, et jusqu'à quand |
| `used` | déjà consommé |
| `expired` | échu à l'instant de la consultation |
| `status` | `VALID`, `USED` ou `EXPIRED` |

`email` restreint à un titulaire ; `used=true` ou `false` filtre sur l'usage ; sans filtre, tout.

**Ce que cela implique.** Un code valide est un moyen de connexion : le lire au téléphone à
quelqu'un, c'est le connecter. Chaque consultation est journalisée avec le nom de l'administrateur
et ses filtres. Le support doit vérifier l'identité de l'appelant avant de communiquer un code,
comme il le ferait pour un mot de passe.
