# Pièces comptables imprimables (bons, reçus, pièces de caisse)

Contrat exact de chaque route (paramètres, corps, réponse, champs JSON) : [routes.md](routes.md). Ce fichier explique le fonctionnement et ce qu'il faut à l'écran. Index du module : [README.md](README.md).

## Règles communes

- **Numéro sans trou** par type et par mois : `PREFIXE-AAAAMM-000001`. Le compteur (`acc_voucher_counter`) est verrouillé et avance dans la transaction d'émission : une émission annulée ne consomme pas de numéro. La base refuse un saut de numéro et la suppression d'un compteur.
- **Un numéro par fait** : la première impression attribue le numéro ; les suivantes réutilisent la pièce enregistrée (`acc_voucher`), comptent l'impression et portent **« DUPLICATA, réimpression du … »**.
- Une pièce émise ne se modifie ni ne se supprime (déclencheur) ; une annulation est liée (`cancelled_by_number`) et l'originale porte **« ANNULÉE PAR … »**.
- En-tête : raison sociale et mentions de l'exploitant (`fund.voucher.issuer-name`, `fund.voucher.issuer-details`, à renseigner avec RCCM, NIU, adresse).
- Montant en chiffres et **en lettres**, cases de signature nommées (signature électronique horodatée quand la décision a été prise à l'écran), pied avec date d'émission, auteur, identifiant de génération, mention de conservation, pagination.
- **QR** vers `{app.verify-base-url}/verify/voucher/{numéro}` : page publique qui rend le numéro, le type, le montant, la date d'émission, le nombre d'impressions et l'état (`VALIDE` ou `ANNULEE PAR …`).

## Pièces de ce lot

| Pièce | Préfixe | Route (PDF) | Contenu |
| --- | --- | --- | --- |
| État de besoin | `EDB` | `GET /api/v1/fund-needs/{n}/pdf` | Demandeur, service, site, centre de coût ; objet, urgence, date souhaitée ; tableau des besoins ; total en lettres ; décision ; suite donnée (demandes) ; signatures demandeur, chef de service, responsable financier |
| Demande de fonds | `DDF` | `GET /api/v1/fund-requests/{n}/pdf` | Type, objet, personnes, caisse et mode de remise, mission, dépense non prévue ; lignes ; niveaux requis et avances non soldées du bénéficiaire ; circuit de validation ; signatures du demandeur et de chaque niveau |
| Bon de décaissement | `BDF` | `GET /api/v1/fund-requests/{n}/disbursements/{mouvement}/pdf` | Demande, lignes, mouvement de caisse, session, caissière ; approuvé, décaissé, déjà décaissé, reste ; imputation ; circuit ; engagement à justifier avant l'échéance (avance) ; signatures bénéficiaire et caissière |
| Fiche de justification | `JUS` | `GET /api/v1/fund-requests/{n}/justification/pdf` | Avance, mission, échéance, bons d'origine ; tableau des dépenses (HT, TVA, TTC, pièce, statut) ; avance reçue, complément, justifié, rejeté, rendu, reste à rendre ou complément dû ; signatures bénéficiaire et comptable |

Les bons d'entrée et de sortie de caisse, le rapport de clôture et le reçu de paiement restent produits par leurs modules (caisse, paiement), avec DUPLICATA et ANNULÉ PAR depuis le lot C4. Les PV de caisse, bordereaux et pièces des immobilisations et provisions utiliseront le même `VoucherService` dans leurs lots.

Droits : ceux de la demande (le service vérifie que l'utilisateur est le demandeur, le bénéficiaire, un valideur, la caisse ou un comptable).

## Pour le frontend

- Bouton « Imprimer » sur la fiche d'un état de besoin, d'une demande, de chaque décaissement (liste `disbursements`) et sur la justification d'une avance ; ouvrir le PDF dans un nouvel onglet.
- Après un décaissement, ouvrir automatiquement le bon de décaissement du mouvement créé.
- Page publique de vérification (lien du QR) : afficher la réponse de `GET /verify/voucher/{numéro}`.
