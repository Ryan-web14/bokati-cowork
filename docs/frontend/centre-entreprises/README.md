# Centre de gestion des entreprises : documentation frontend

Tout ce qui a été développé depuis le lancement du centre de gestion des entreprises, un document
par sujet. Base de l'API : `/sni/api/v1`.

Le plan d'ensemble est dans
[docs/a-realiser/plan-centre-gestion-entreprises.md](../../a-realiser/plan-centre-gestion-entreprises.md).

## Centre de gestion des entreprises

| Document | Contenu |
| --- | --- |
| [01-fiche-organisation.md](./01-fiche-organisation.md) | Créer et mettre à jour une organisation, identifiants, effets sur la facturation |
| [02-rattachement-membres.md](./02-rattachement-membres.md) | Gérants et employés, plusieurs organisations par membre |
| [03-factures-pour-organisation.md](./03-factures-pour-organisation.md) | Factures de membre émises pour le compte de l'organisation |
| [04-etats-de-compte.md](./04-etats-de-compte.md) | États de compte de l'organisation et du membre |
| [05-portail-gerant.md](./05-portail-gerant.md) | Le compte de l'organisation dans le portail du gérant |
| [06-abonnement-partage.md](./06-abonnement-partage.md) | Abonnement partagé par places, réservation par un collaborateur, annexe de facture |
| [07-passes-partages.md](./07-passes-partages.md) | Passes d'organisation partagés |
| [08-portefeuille.md](./08-portefeuille.md) | Portefeuille de l'organisation, dépenses autorisées, codes, réservation prépayée |
| [09-contrats-domiciliation.md](./09-contrats-domiciliation.md) | Contrats, signataire désigné, domiciliation et courrier |
| [10-marketplace.md](./10-marketplace.md) | Offres, panier, commandes, retrait et annulation |

## Évolutions livrées en parallèle

| Document | Contenu |
| --- | --- |
| [11-reservations-equipe.md](./11-reservations-equipe.md) | Réservation par l'équipe hors préavis, créneaux du jour même, vérification de disponibilité |
| [12-pdf-duplicata.md](./12-pdf-duplicata.md) | PDF d'administration : original et duplicata dans un même fichier |
| [13-caisses-fin-de-mois.md](./13-caisses-fin-de-mois.md) | Clôture et renouvellement automatiques des caisses, rapport de clôture |
| [14-acomptes.md](./14-acomptes.md) | Le statut d'un acompte suit le paiement de sa facture (`PARTIALLY_PAID`) |
| [15-erreurs-api.md](./15-erreurs-api.md) | Une erreur garde son statut : un `401` ne veut plus dire qu'une chose |
