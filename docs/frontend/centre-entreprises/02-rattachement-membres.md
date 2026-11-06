# Rattachement des membres

Préfixe des routes : `/sni/api/v1`.

Un membre peut être rattaché à plusieurs organisations, une fois chacune, comme `OWNER`
(gérant ou propriétaire) ou `EMPLOYEE`. Ce lien ne change pas le payeur par défaut du membre.
L'`id` d'un rattachement est un numéro court, lisible par JavaScript.

## Administration (`ADMIN`, `SUPER_ADMIN`, `MANAGER`)

| Méthode et chemin | Effet |
| --- | --- |
| `GET /customers/{customerId}/members?status=` | actifs par défaut ; `ENDED` ou `ALL` pour l'historique |
| `POST /customers/{customerId}/members` | `{ "memberId", "role", "startedAt"?, "invoiceLinkMode"?, "canSign"?, "canCollectMail"? }` |
| `PATCH /customers/{customerId}/members/{id}` | `role`, `invoiceLinkMode`, `useCompanyDefaultMode`, `canSign`, `canCollectMail` |
| `POST /customers/{customerId}/members/{id}/end` | `{ "reason" }` · le lien passe `ENDED` et reste en historique |
| `GET /members/{memberId}/companies` | les organisations d'un membre, avec rôle et dates |
| `PATCH /companies/{customerId}/invoice-link-mode` | `{ "mode": "AUTOMATIC" \| "ON_CHOICE" }` · mode par défaut de l'organisation |

## Portail du membre

| Méthode et chemin | Pour qui |
| --- | --- |
| `GET /client/companies` | tout membre : ses organisations, son rôle, le mode qui s'applique ; `companyDefaultMode` pour le gérant |
| `GET /client/companies/{customerId}/members` | le gérant ; un employé reçoit 404 |
| `PATCH /client/companies/{customerId}/invoice-link-mode` | le gérant : `{ "mode" }` |
| `PATCH /client/companies/{customerId}/members/{id}/invoice-link-mode` | le gérant : `{ "mode" }` ou `{ "useCompanyDefaultMode": true }` |

## Règles

- Seule une organisation (`COMPANY`) reçoit des membres ; un seul lien actif par couple.
- Mode de rattachement des factures : `AUTOMATIC` (toutes les factures du membre reliées
  d'office) ou `ON_CHOICE` (choix à chaque fois). Défaut de l'organisation, remplaçable par lien.
- **Un membre n'a qu'une organisation en mode automatique**, mode hérité compris : sinon 400
  avec les noms en conflit.
- Fin de lien : si l'organisation était aussi le payeur par défaut du membre, il repasse sur sa
  fiche personnelle (`payerChanged: true`). Si elle ne peut pas être créée (courriel ou
  téléphone déjà pris), le lien est quand même terminé et `warning` le dit.
- L'abonnement d'une organisation couvre ses membres rattachés actifs pour les créneaux
  réservés aux abonnés.
- Reprise : les membres dont le payeur était une organisation y sont rattachés comme
  `EMPLOYEE` ; les gérants se désignent ensuite.
