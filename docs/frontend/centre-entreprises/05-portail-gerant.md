# Compte de l'organisation dans le portail du gérant

Préfixe des routes : `/sni/api/v1`.

Réservé au gérant actif (`OWNER`). Pour tout autre membre, l'organisation répond 404. Une facture
qui n'est ni à l'organisation ni reliée à elle, ou qui est en brouillon, répond aussi 404.

| Méthode et chemin | Effet |
| --- | --- |
| `GET /client/companies/{customerId}/statement` | état de compte ; ses `lines` servent de liste des factures (celles avec `balanceDue > 0` sont à régler) |
| `GET /client/companies/{customerId}/invoices/{documentNumber}` | détail d'une facture (même forme que `/client/billing/invoices/{n}`) |
| `GET /client/companies/{customerId}/invoices/{documentNumber}/pdf` | PDF de la facture |
| `POST /client/companies/{customerId}/invoices/{documentNumber}/pay/mobile-money` | paiement mobile money, même corps que le paiement d'un membre (`phoneNumber`, `correspondent`) |
| `GET /client/companies/{customerId}/mobile-money/deposits/{depositId}` | suivi de ce paiement |
| `POST /client/companies/{customerId}/invoices/{documentNumber}/pay/cash` | « je passerai payer en espèces » ; la facture reste due jusqu'à la caisse |
| `GET /client/companies/{customerId}/payments/{transactionNumber}/receipt` (et `/pdf`) | reçu d'un paiement sur une facture de l'organisation |

Le gérant peut régler les factures de ses employés qui sont reliées à l'organisation : le
paiement porte l'identifiant du gérant comme initiateur, la facture reste celle du membre.

Le portefeuille personnel du gérant ne paie pas pour l'organisation : celle-ci a son propre
portefeuille, voir [08-portefeuille.md](./08-portefeuille.md).
