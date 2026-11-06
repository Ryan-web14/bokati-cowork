# Acompte : il suit le paiement de sa facture

Le statut d'un acompte suit ce qui est payé sur sa facture, à chaque paiement et à chaque annulation
de paiement.

| `status` | Sens | Libellé sur le PDF |
| --- | --- | --- |
| `PENDING` | rien n'est payé | En attente |
| `PARTIALLY_PAID` | le payé ne couvre pas encore l'acompte (nouveau) | Partiellement réglé |
| `PAID` | le payé couvre l'acompte | Réglé |
| `CANCELLED` | acompte annulé, inchangé par les paiements | Annulé |

À faire côté frontend : prévoir la valeur `PARTIALLY_PAID` dans les badges et les filtres.

Les acomptes restés bloqués en attente ont été remis d'aplomb par la migration, avec la même règle.
