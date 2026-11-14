# Lot D6 : nettoyage et doc frontend

Branche `feature/modeles-documents`.

## Gabarits supprimés

Douze gabarits n'étaient appelés nulle part. La recherche a porté sur le code Java, les migrations, les autres gabarits, les tests, la doc et `notification_template` de la base de dev ; aucun nom de gabarit n'est construit dynamiquement à partir de ceux-ci.

| Gabarit | Remplacé par, ou rôle |
| --- | --- |
| `email/billing-reminder` | `email/billing-dunning` (relances) |
| `email/booking-confirmation-v3` | `email/booking-confirmation` |
| `email/booking-reminder`, `email/booking-reminder-email` | Aucun rappel de réservation par courriel n'est envoyé |
| `email/member-created` | `form/welcome-member-email` |
| `email/subscription-expiry-reminder` | `email/subscription_ending_soon` |
| `email/welcome-email-v2` | `form/welcome-member-email` |
| `billing/invoice-v2` | Gabarit de facture en service et modèle `INVOICE` |
| `form/badge-membre`, `form/planning-occupation` | Formulaires jamais branchés |
| `member/fiche-client`, `member/fiche-inscription` | Fiches jamais branchées |

Les gabarits utilisés restent : ils servent de repli tant qu'un modèle n'est pas publié. Git garde l'historique des fichiers supprimés.

## Gardé, à signaler

`static/index.html` est une ancienne galerie d'aperçu des courriels : ses six liens (`booking-confirmation-v2.html`, `invoice-v3.html`...) pointent vers des fichiers absents. Elle peut servir de page d'accueil du serveur : elle n'est pas supprimée sans accord.

## Doc frontend

`docs/final-frontend-doc/modeles/` : liste et cycle de vie, éditeur des pièces, champs ajoutés, éditeur des courriels, erreurs.
