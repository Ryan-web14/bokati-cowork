# Améliorations de la facturation et des devis

Fonctionnalités demandées par l'exploitant le 13/11/2026, avec l'existant relevé dans le code (branche `feature/finance-comptabilite`) et ce qu'il faut ajouter. Les lots sont repris dans le [README](README.md#4-lots).

Principe commun : **une pièce scellée (SEFC) ne se modifie jamais**. Toute correction après scellement passe par un avoir, une rectificative ou un duplicata. Avant scellement, tout est modifiable, avec une version à chaque changement.

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
  - plusieurs acomptes (échéances : 30 % à la commande, 40 % à mi-parcours…) ;
  - chaque acompte demandé produit une **facture d'acompte** numérotée, envoyée au client, encaissable par tous les moyens (caisse, mobile money, virement, portefeuille) ;
  - l'encaissement passe le devis en DEPOSIT_PAID quand l'acompte exigé est couvert.
- **Côté facture** :
  - un acompte non pris pendant le devis se demande sur la facture finale (facture d'acompte rattachée), ou on en prend acte au paiement partiel ;
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

**Existant** : le type `PROFORMA_INVOICE` (série `PRF`) existe, mais il se comporte comme une facture : il s'encaisse et se scelle (SEFC).

**À faire**
- **Une proforma n'est pas une pièce fiscale** : pas de scellement, pas d'écriture comptable, pas d'encaissement direct.
- **Création** : directement, ou depuis un devis (accepté ou non) ; date de validité.
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
