# Dépôts de garantie (lot C7)

> Rédigé d'après le code (compléments de facturation, lots C0 à C16, 13/11/2026). Routes exactes et champs JSON : [routes.md](routes.md) et `routes/`.

## Ce qui change

- Une caution n'est ni taxée ni un produit : la ligne « Dépôt de garantie » d'une facture d'abonnement, et toute ligne d'un article marqué caution, part au compte `165`.
- Marquer un article du catalogue comme caution : `PUT /billing/catalog/{itemCode}/security-deposit` `{"securityDeposit": true}` (champ `securityDeposit` dans la réponse de l'article).
- À l'émission, la facture inscrit sa caution au registre.

## Registre

- `GET /billing/security-deposits?customerType=&customerCode=&open=true`, `GET /billing/security-deposits/{n}` : montant, restitué, retenu, imputé, reste, mouvements.
- `POST /billing/security-deposits/{n}/movements`, une fois la facture de la caution soldée :
  - `{"kind": "RELEASE", "amount": 150000}` : restitution (le client devient créditeur, remboursé par les circuits habituels) ;
  - `{"kind": "RETAIN", "amount": 50000, "reason": "Moquette tachée"}` : retenue motivée ;
  - `{"kind": "APPLY", "amount": 80000, "targetDocumentNumber": "FAC-41"}` : imputation sur une facture ouverte du même client.
  Sans `amount`, tout le reste.

## Caution d'abonnement

- Quand la facture qui la porte est soldée, la caution est créditée sur le portefeuille du client, où la retenue posée à la création de l'abonnement la bloque.
- Elle se règle à la sortie d'abonnement (rubrique « Caution libérée ou imputée ») : le registre note alors les factures réglées par la caution et la part rendue au portefeuille. Tant que la retenue est active, les mouvements du registre sont refusés (`CAUTION_ABONNEMENT`).

## Reprise

`GET /billing/security-deposits/legacy` : cautions facturées avant ce lot (comptées en produit avec TVA), avec le montant hors taxe, la TVA extraite et l'état comptabilisé, pour le comptable.

Codes : `CAUTION_INVALIDE`, `CAUTION_NON_ENCAISSEE`, `CAUTION_ABONNEMENT`, `CAUTION_MONTANT`, `MOTIF_REQUIS`.
