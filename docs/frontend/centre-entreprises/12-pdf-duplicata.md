# PDF de facture et de reçu : administration et portail

Préfixe des routes : `/sni/api/v1`.

| Route | Contenu |
| --- | --- |
| `GET /billing/documents/{n}/pdf` (administration) | l'original, puis son duplicata tamponné « DUPLICATA » (en tête et en filigrane), dans un même PDF |
| `GET /payments/transactions/{tx}/receipt/pdf`, `GET /payments/receipts/{n}/pdf` (administration) | idem pour le reçu |
| Routes du portail (membre, client, gérant) et pièces jointes des e-mails | l'exemplaire unique, inchangé |

L'original du PDF d'administration est la version canonique (scellée, signée) ; le fichier
assemblé étant un nouveau document, sa signature électronique n'y est plus vérifiable. La version
signée reste celle servie par le portail et comparée par `compare`.
