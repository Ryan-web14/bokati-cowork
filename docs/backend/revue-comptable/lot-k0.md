# Lot K0 : écritures automatiques justes, file qui ne se bloque plus

Branche `feature/revue-comptable`. Migrations : dev `V400__comptabilite_k0_ecritures_automatiques.sql`, prod `V394__comptabilite_k0_ecritures_automatiques.sql` (contenu identique).

> Numérotation : la branche `feature/subscription` (lot S0, pas encore fusionné) utilise V399 / V393. Flyway refuse les migrations hors ordre : fusionner S0 avant K0, ou renuméroter S0 si K0 passe en premier.

## Ce qui change

| Écart | Avant | Après |
|---|---|---|
| A1 | Échéance jamais portée sur la ligne 411 (`"dueDate":"dueDate"` lu comme du texte) | Un chemin nu se lit dans l'événement, comme un montant ; une date illisible fait échouer l'événement avec « Échéance illisible » |
| A2 | Caution (165) et débours sans client : pièce en attente | Chaque ligne de vente sur un compte de tiers porte le client ; le D 165 d'un mouvement de caution et la contrepartie 165 du portefeuille aussi |
| A3 | Débours sur 4718, le compte des écarts d'équilibrage | Nouveau compte **4711 Débours pour le compte des clients** (suivi par client, lettrable) ; réglage `accounting.sales.disbursement-account` à 4711 ; 4711 exclu du contrôle des comptes d'attente à la clôture (`accounting.closing.suspense-exclusions`) |
| A4 | Avoir sur facture d'acompte au débit du 706 ; annulation d'une facture d'acompte en partie réglée sur tout le TTC | Schéma `SALES_DEPOSIT_CREDIT_NOTE` : D 4191 / C 411. Schéma `SALES_DEPOSIT_REMAINDER_CANCELLED` : seul le reste non réglé quitte le 4191 et le 411 |
| A5 | Avoir imputé sur une autre facture : aucune écriture | La facture cible est enregistrée sur l'avoir (`applied_to_document_number`, `applied_at`, rattrapage depuis le journal fiscal). Événement `CREDIT_NOTE_APPLIED`, schéma `SALES_CREDIT_NOTE_APPLIED` : D 411 titulaire (référence de l'avoir) / C 411 client de la facture (référence de la facture), TVA libérée en régime des encaissements. Couvre l'imputation sur la facture de l'entreprise du client |
| A6 | TVA bloquée au 4438 après une affectation tardive ou un transfert de créance | Les trois schémas libèrent (ou remettent en attente pour une annulation d'affectation) la TVA au prorata |
| A7 | Un seul code de TVA par facture | Liste `vatLines` : une ligne de TVA par taux, avec son code (`TVA18`…), sa base et son libellé ; la somme égale la TVA du document |
| A8 | Remise négative passée au 709 en silence | Au-delà de 1 XAF par ligne, l'événement échoue avec le détail de l'incohérence |
| A9 | Espèces sans session de caisse en 4719, sans alerte | Compte de caisse du réglage `accounting.payment.CASH.account` ; vide (défaut) : échec avec le motif |
| A10 | Facture à montant nul en échec | Événement écarté : « Montant nul : aucune écriture à passer » |
| A11 | Décisions sur écart de caisse laissées en brouillard (OD) | Journal OD en validation automatique (les saisies manuelles ne changent pas) |
| A12 | TVA de la caisse et des justifications sans code ni base ; nature fournisseur sans tiers | Code et base sur la ligne de TVA ; une nature en 40x porte le fournisseur de la ligne |
| M1 | Pièce en attente complétée à la main : événement bloqué en attente | À la validation, l'événement passe comptabilisé, prend le numéro définitif, les modules sont prévenus |
| M2 | 50 événements qui attendent suffisaient à bloquer la file | File sans les familles coupées, les moins tentés d'abord |
| M3 | Un fait illisible bloquait la lecture des suivants ; un mouvement de caisse en échec n'était jamais retenté | Le fait passe en échec avec « Lecture du fait impossible : … » et quitte la file. Il se retente tout seul, de plus en plus espacé (tout de suite, puis une minute par tentative, une heure au plus), ou à la main par **Régénérer** |
| M4 | Contre-passation automatique refusée si le journal a une pièce plus récente | Datée au plus tôt de la dernière pièce du journal, avec mention |
| M6 | Régénérer reprenait les anciennes données | Régénérer relit d'abord le fait (pour un événement en échec ou en attente d'équilibrage ; une livraison en double d'un événement en file ne change rien) (facture, paiement, portefeuille, caisse…) : une correction à la source passe dans la pièce |
| M7 | Montant d'une répétition pris dans l'événement entier ; `a-b` lu comme un seul chemin | Montant lu dans l'élément (préfixe `event.` pour l'événement) ; `a-b` est une soustraction. Reste : un résolveur dont le chemin est absent prend toujours l'argument comme valeur (aucun schéma livré ne s'en sert) |

Reportés en K1 : A13 (contre-passations de l'imputation d'acompte, du transfert de créance, du mouvement de caution annulé : la facturation n'a pas encore d'état d'annulation pour ces faits) et A14 (remboursement d'un trop-perçu, à traiter avec le module des paiements).

## Pour l'exploitant

- Rien à faire pour les schémas : la migration crée les nouvelles versions et retire les anciennes. Un schéma modifié à la main depuis l'écran n'est pas touché (le remplacement ne s'applique qu'au texte livré).
- Paiements en espèces sans session : soit les rattacher à une session, soit renseigner `accounting.payment.CASH.account` (par exemple 571).
- Un événement en échec « Lecture du fait impossible » se corrige à la source (taux de change, session, facture) : il repasse tout seul, ou tout de suite par **Régénérer**.

## Tests

`K0EcrituresAutomatiquesTest` (échéance, répétition, soustraction, montant nul, TVA par taux, migration identique en dev et en prod) ; tests unitaires de la comptabilité, de la facturation, des demandes de fonds et des réglages : 443 passés. Tests d'intégration sur PostgreSQL (migration appliquée) : `PostingEngineF2IT`, `SalesAccountingF4IT`, `VatF11IT`, `CashAccountingF3IT`, 26 passés.
