# Améliorations de la facturation et des devis

Fonctionnalités demandées par l'exploitant le 13/11/2026, avec l'existant relevé dans le code (branche `feature/finance-comptabilite`) et ce qu'il faut ajouter. Les lots sont repris dans le [README](README.md#4-lots).

Principe commun : **une pièce certifiée par notre système ne se modifie jamais**. Toute correction après scellement passe par un avoir, une rectificative ou un duplicata. Avant scellement, tout est modifiable, avec une version à chaque changement.

## B2. Négociation et révision des devis

**Existant**
- Statuts NEGOTIATION, VIEWED, ACCEPTED ; options de devis ; versions et différences entre versions (`/documents/{n}/versions`, `.../diff`).
- Une mise à jour n'est admise qu'en DRAFT, ISSUED ou SENT (écart E9).

**À faire**
- **Modifier un devis en négociation** : prix unitaires, quantités, remises, ajout et retrait de lignes, conditions. Admis en DRAFT, SENT, VIEWED et NEGOTIATION.
- **Réviser un devis accepté** : la révision le repasse en NEGOTIATION, annule la signature et les demandes de signature en cours, et demande une nouvelle acceptation.
- **Chaque modification crée une version** (déjà en place), avec l'auteur, le motif et le canal (« proposition du client », « contre-proposition »). L'écran montre la frise des propositions et les écarts de prix entre versions.
- **Contrôles à chaque version** : prix plancher et remise maximale du catalogue, marge minimale (`BillingDiscountGuard`). Au-delà, dérogation motivée, avec le droit adéquat.
- **Validité** : la date de validité se prolonge à la révision (paramètre : nombre de jours) ; un devis échu passe en EXPIRED (écart E10).
- **Routes** : `PUT /quotes/{n}` (mise à jour, avec motif), `GET /quotes/{n}/negotiation` (frise des versions et des écarts).

**Critère** : un devis en NEGOTIATION reçoit une baisse de prix et une ligne de plus ; une version 2 existe, la différence avec la version 1 montre les deux changements, le devis reste en NEGOTIATION, et le prix sous le plancher est refusé sans dérogation.

## B3. Acomptes, du devis à la facture

**Existant**
- Acompte sur devis accepté : en montant ou en pourcentage, sur tout ou partie des lignes (`BillingDocumentAdvance`).
- Demande (`request-deposit`), paiement (`mark-deposit-paid`), statut DEPOSIT_PAID. À la conversion, l'acompte payé est repris sur la facture.
- Un acompte qui n'a pas été pris pendant le devis ne peut pas être demandé sur la facture.

**À faire**
- **Côté devis** :
  - un acompte se demande à tout moment sur un devis déjà créé, **en montant ou en pourcentage** (du total, ou de lignes choisies) ;
  - **plan d'acomptes** : plusieurs acomptes réglés d'un coup (par exemple 30 % à la commande, 40 % à mi-parcours, avec leur date d'échéance), modifiable tant qu'aucun n'est encaissé ;
  - chaque acompte demandé produit une **facture d'acompte** numérotée, envoyée au client, encaissable par tous les moyens (caisse, mobile money, virement, portefeuille) ;
  - l'encaissement passe le devis en DEPOSIT_PAID quand l'acompte exigé est couvert.
- **Côté facture** :
  - les mêmes possibilités que sur le devis : un ou plusieurs acomptes, en montant ou en pourcentage, avec un plan d'acomptes ;
  - un acompte qui n'a pas été pris pendant le devis se demande ainsi sur la facture (facture d'acompte rattachée) ;
  - la facture finale déduit chaque acompte facturé (ligne « Acompte n° ... du ... ») et ne réclame que le reste.
- **Annulation** : un acompte demandé puis abandonné passe en CANCELLED. Un acompte encaissé sur un devis refusé se rembourse ou se garde en avoir, selon la décision prise.
- **Comptabilité** : facture d'acompte au crédit du `4191` (clients, avances et acomptes reçus) ; la facture finale solde le `4191` contre le `411`. La TVA sur les acomptes suit le régime choisi (décision D3).
- **Routes** : `POST /quotes/{n}/deposits` (un ou plusieurs), `POST /invoices/{n}/deposits`, `GET /documents/{n}/deposits`, `PATCH /deposits/{id}/cancel`.

**Critère** : un devis de 1 000 000 avec deux acomptes de 30 % puis 20 % ; les deux factures d'acompte sont encaissées ; la facture finale affiche 1 000 000, déduit 500 000 et réclame 500 000 ; le `4191` est soldé après la facture finale.

## B4. Envoi : destinataires, canaux, traces

**Existant**
- Envoi par courriel (`send-email`) à une seule adresse, celle du client. Les membres ont plusieurs adresses de facturation (migration V312), mais l'envoi n'en tient pas compte.
- `send` change le statut (écart E6).
- Le PDF reste figé après scellement (écart E15).

**À faire**
- **Choisir les destinataires au moment d'envoyer** :
  - destinataires principaux, copie, copie cachée ;
  - parmi les contacts du client (adresses de facturation du membre, contacts de l'organisation) ou des adresses saisies librement ;
  - préférence enregistrée par client (« envoyer les factures à la comptabilité de l'organisation »).
- **Canaux** : courriel (PDF joint) et WhatsApp par Infobip (texte et lien vers l'espace client, avec les modèles `facture_payee`, `rappel_paiement`…, voir `docs/backend/whatsapp-infobip`).
- **Journal des envois** : par document, date, auteur, canal, destinataires, objet, et pour WhatsApp le statut de remise et de lecture. Le renvoi d'un document est un nouvel envoi tracé.
- **Duplicata** : après une correction du destinataire sur une pièce scellée, le PDF envoyé est un duplicata daté, l'original reste la référence (écart E15).
- **Routes** :
  - `POST /documents/{n}/send` `{to[], cc[], bcc[], channel, message}` ;
  - `GET /documents/{n}/sendings` ;
  - `GET`, `PUT /customers/{type}/{code}/billing-recipients`.

**Critère** : une facture d'organisation part à deux adresses en copie, plus une adresse saisie ; le journal des envois montre les trois ; le statut de la facture ne change pas.

## B5. Facture proforma

**Existant** : le type `PROFORMA_INVOICE` (série `PRF`) existe, mais il se comporte comme une facture : il s'encaisse et se scelle (certifiée par notre système).

**À faire**
- **Une proforma n'est pas une pièce fiscale** : pas de scellement, pas d'écriture comptable, pas d'encaissement direct.
- **Création** : directement, ou depuis un devis (accepté ou non) ; date de validité.
- **Modifiable quel que soit son statut** (décision de l'exploitant) : chaque modification crée une version ; une proforma déjà transformée en devis reste modifiable, sans effet sur le devis.
- **Statuts** : DRAFT, SENT, ACCEPTED, EXPIRED, CONVERTED.
- **Paiement d'avance sur une proforma** : il crée une facture d'acompte (lot B3), ou convertit la proforma en facture si le paiement couvre le total (décision D2).
- **Conversion en facture** : une seule fois, avec le lien entre les deux documents.
- **PDF** : mention « FACTURE PROFORMA, document sans valeur fiscale ».
- **Routes** : `POST /proformas`, `POST /quotes/{n}/proforma`, `POST /proformas/{n}/convert-to-invoice`.

**Critère** : une proforma envoyée puis payée en totalité devient une facture scellée, une seule ; la proforma ne figure dans aucun total fiscal ni comptable.

## B6. Changement de client sur une facture

**À faire**
- **Brouillon ou facture émise non scellée** :
  - changement libre du client, avec motif ;
  - le système reprend la fiche du nouveau client (adresse, NIU, conditions, exonération de TVA, devise), recalcule et crée une version ;
  - les paiements déjà imputés restent au client d'origine : le changement est refusé tant qu'un paiement est imputé.
- **Facture scellée** : procédure guidée en une action :
  1. avoir total au client d'origine ;
  2. nouvelle facture identique au nouveau client ;
  3. les paiements du client d'origine passent en avoir à rembourser ou à imputer.

  Les trois pièces sont liées et tracées.
- **Comptabilité** : l'avoir et la nouvelle facture produisent leurs écritures (`411` de chacun).
- **Route** : `POST /invoices/{n}/change-customer` `{customerType, customerCode, reason}`.

**Critère** : une facture scellée de l'organisation A passe à l'organisation B ; un avoir A, une facture B, et les comptes `411` de A et de B sont justes.

## B7. Devis et inventaire

**Existant** : aucun lien entre une ligne de devis et l'inventaire. Le catalogue de prestations porte prix, plancher, marge et remise maximale.

**À faire**
- **Ligne d'article d'inventaire** sur un devis ou une facture :
  - choix dans le catalogue ou l'inventaire, prix repris ;
  - **vérifications au moment de la saisie** : stock disponible (quantité en stock moins réservée), prix plancher, article actif ;
  - **vérification des ressources** pour une ligne de réservation (salle, bureau) : disponibilité aux dates demandées.
- **Contrôle paramétrable** : avertissement ou blocage (décision D5).
- **Réservation du stock** à l'acceptation du devis, libérée au refus ou à l'expiration.
- **Sortie de stock** à la facturation (ou à la livraison, selon le paramètre). Le coût sort au `603` en comptabilité, déjà prévu par le lot de stock.
- **Route** : `POST /documents/check-availability` (lignes en entrée, disponibilités et alertes en sortie), appelée aussi automatiquement à la création et à la mise à jour.

**Critère** : un devis de 12 chaises quand 10 sont disponibles donne une alerte précise (2 manquantes), bloque si le paramètre l'exige, et l'acceptation réserve les 10 disponibles.

## B8. Saisie d'un montant et ventilation par le système

**Existant** : le prix TTC se saisit ligne par ligne (`taxIncluded`), mais rien ne part d'un total à répartir. La simulation ignore les conditions du client (écart E17).

**À faire**
- **Saisie d'un total** : TTC (ou HT) pour tout le document, ou pour un groupe de lignes.
- **Calcul par le système** :
  - HT, TVA et centimes additionnels selon les règles fiscales de chaque ligne (`TaxRule`) ;
  - répartition sur les lignes au prorata de leur prix de référence (catalogue), ou de quantités saisies ;
  - arrondi au franc, le reste sur la dernière ligne.
- **Remise implicite contrôlée** : l'écart entre le total saisi et le prix catalogue apparaît comme une remise, soumise au plancher et à la remise maximale.
- **Simulation fidèle** : même calcul qu'à la création, avec exonération de l'organisation et plafond d'encours (E17).
- **Route** : `POST /documents/allocate-amount` `{lines, targetAmount, taxIncluded}` → lignes calculées, à reprendre dans la création.

**Critère** : 500 000 TTC saisis sur trois lignes à 18 % de TVA donnent des HT et des TVA par ligne dont la somme fait exactement 500 000, avec la remise implicite affichée.

## B9. Relevé client, balance âgée et échéanciers

**À faire**
- **Relevé client** :
  - pagination complète (métadonnées), tri demandé respecté ;
  - export CSV et PDF de **tout** le relevé, pas de la première page seulement ;
  - envoi au client par les canaux du lot B4 ;
  - quand la comptabilité est active, le relevé se tire du compte du client (`411`), comme prévu par le plan finance (écart E13).
- **Balance âgée** : filtrée par devise, une colonne par tranche, jamais de cumul entre devises (E11).
- **Échéanciers** :
  - réservés aux factures et rectificatives émises, non soldées ;
  - un paiement direct sur la facture se répartit sur les échéances, dans l'ordre (E12).

## B10. Catalogue, routes et messages

**Catalogue**
- Familles et sous-familles.
- Prix par segment de client (membre, entreprise, visiteur) et prix datés, avec l'historique des changements de prix.
- Lien vers l'article d'inventaire (lot B7).
- Compte de produit propre à un article, prioritaire sur le compte par type de ligne de la comptabilité (lot F4).
- Import et export du catalogue en CSV.
- Recherche, aperçu de la marge.

**Routes** : une seule famille `/quotes/...` ; `/quotations/...` dépréciées six mois puis retirées (E18).

**Messages** : en français accentué, avec un code d'erreur stable (E19).

## B11. Gestion des affaires

Une **affaire** regroupe tout ce qui concerne une vente à un client : devis et leurs versions, proformas, acomptes, factures, avoirs, paiements, échanges et pièces jointes.

- **Fiche d'affaire** : client, intitulé, responsable commercial, montant prévu, probabilité, étape (prospection, proposition, négociation, gagnée, perdue avec motif), dates.
- **Création** : à la main, depuis une opportunité du CRM, ou automatiquement au premier devis d'un client.
- **Tableau de l'affaire** : total devisé, accepté, facturé, encaissé, reste à facturer, reste à encaisser, acomptes, marge prévue et réalisée.
- **Liens** : chaque document porte son affaire ; la conversion d'un devis en facture garde l'affaire.
- **Liste des affaires** : filtres par étape, responsable, client, période ; export.
- **Analytique** : l'affaire peut être une section analytique (lot F9), pour sa rentabilité.
- **Routes** : `GET`, `POST`, `PUT /billing/deals`, `GET /billing/deals/{code}` (tableau), `PATCH /billing/deals/{code}/stage`.

**Critère** : une affaire avec deux devis (un refusé, un accepté), un acompte et une facture partiellement payée affiche les bons totaux à chaque étape.

## B12. Auteur de la pièce et visa

- **L'utilisateur connecté** est repris du jeton à la création : son nom et sa fonction remplissent « Établi par » sur la pièce.
- **Le visa** de la facture (case à côté des mentions légales) affiche le même nom, la fonction et la date ; une signature enregistrée sur la fiche de l'utilisateur s'y imprime si elle existe.
- **Chaque version** garde son auteur ; la pièce imprimée montre l'auteur de la version imprimée.
- Plus aucun champ « auteur » n'est pris dans le corps de la requête (écart E14).

**Critère** : une facture créée par un commercial porte son nom dans « Établi par » et dans le visa, sans saisie.

## B13. Simulation interactive et descriptions

- **Simulation ligne par ligne** : à chaque saisie (quantité, prix, remise, taxe, total saisi), le serveur renvoie le calcul de la ligne et du document : HT, remises, TVA, centimes additionnels, TTC, contrôles (prix plancher, remise maximale, stock, plafond d'encours), avec les alertes ligne par ligne.
- **Même calcul** que la création (écart E17) ; la route ne crée rien : `POST /billing/documents/simulate-line` (une ligne et le contexte du document) et `POST /billing/documents/simulate` (tout le document).
- **Descriptions plus larges** : description et description détaillée de ligne en texte long (jusqu'à 4 000 caractères), avec retours à la ligne gardés à l'impression ; même chose pour les notes et conditions du document.

**Critère** : changer la quantité d'une ligne met à jour la ligne et les totaux à l'écran en moins d'une seconde ; une description de 2 000 caractères s'imprime entière sur le PDF.

## B14. Modification d'une facture non payée, avec versions et approbation

- **Une facture se modifie tant qu'elle n'est pas payée** (aucun encaissement imputé). Après le premier paiement, la correction passe par un avoir ou une rectificative.
- **Aucune suppression** : chaque modification crée une version ; les versions précédentes restent consultables et comparables, avec leur auteur et leur motif.
- **Approbation** : la modification d'une facture émise passe par le centre d'approbation de la facturation (lot B15) : un responsable ou un administrateur l'accepte ou la refuse. Tant qu'elle est en attente, la facture garde sa version en vigueur.
- **Facture déjà certifiée par notre système** : la pièce scellée ne peut pas changer (intégrité fiscale). La modification approuvée produit alors une **facture rectificative**, présentée à l'écran comme la nouvelle version de la facture (décision D8).
- **Comptabilité** : la nouvelle version, ou la rectificative, corrige les écritures (contre-passation et nouvelle écriture).

**Critère** : une facture émise non payée est modifiée ; la demande attend l'approbation ; une fois approuvée, la version 2 est en vigueur, la version 1 reste consultable, et rien n'a été supprimé.

## B15. Centres d'approbation et hiérarchie

**Des centres distincts sur un même modèle de données** (décision de l'exploitant) : le centre de la facturation (modifications de factures, dérogations de prix et de remise) et celui de la caisse (demandes de fonds) sont deux écrans séparés, avec leurs droits et leurs files. Ils partagent les mêmes tables (demande d'approbation, étapes, décisions, hiérarchie, circuits) et le même moteur, ce qui donne partout la même hiérarchie, la même validation en avance et le même historique. Un nouveau centre (achats, ressources humaines…) se branche sur ce modèle sans nouvelle table.

**Hiérarchie**
- Chaque utilisateur a un **responsable direct (N+1)** ; la chaîne remonte jusqu'à la direction (N+2, N+3…).
- Écran d'organigramme : rattachements, intérims, délégations datées.

**Circuits**
- Un circuit par type de demande : niveaux requis selon le montant ou l'objet (par exemple facture modifiée sous 500 000 : N+1 ; au-delà : N+1 puis N+2).
- **Un niveau supérieur peut valider en avance** : si le N+2 approuve avant le N+1, la demande passe directement à « approuvée » ; le niveau sauté est noté « approuvé par le N+2 ».
- Refus à n'importe quel niveau : la demande est refusée, avec motif ; renvoi pour correction possible.
- On ne valide jamais sa propre demande.

**Suivi et historique**
- **Mes demandes** : où en est chacune, qui doit agir, depuis quand.
- **À approuver** : les demandes qui m'attendent, par moi-même ou par délégation.
- **Historique** : toutes les demandes et décisions passées, filtrables (type, demandeur, approbateur, période, issue), avec la frise de chaque demande (qui, quand, décision, commentaire, montant avant et après).
- Notifications à chaque étape (application, courriel, WhatsApp), relances et escalade au niveau suivant après un délai.

**Reprise des demandes de fonds**
- Le module caisse a déjà une file « À valider », une frise par demande, des délégations, des relances et l'escalade. Ses niveaux viennent du **montant** (droits `APPROVE_L1` à `L3`), pas d'une hiérarchie, et un niveau supérieur ne peut pas valider en avance.
- Les demandes de fonds passent sur le modèle commun : même hiérarchie, validation en avance par un N+, même historique. Le centre de la caisse garde son écran et ses routes, branchés sur le moteur commun.

**Routes**
- Communes : `GET`, `PUT /approvals/hierarchy` (rattachements), `GET`, `POST /approvals/circuits` ;
- Par centre (`{center}` = `billing`, `cash`…) : `GET /approvals/{center}/mine`, `/to-approve`, `/history` ;
- `PATCH /approvals/{id}/approve`, `/reject`, `/send-back`.

**Critère** : une demande à deux niveaux approuvée d'abord par le N+2 passe à « approuvée », la frise montre le niveau 1 « approuvé par le N+2 », et elle apparaît dans l'historique des deux approbateurs.
