# Plan de réalisation facturation et devis

Au 13/11/2026. Je corrige d'abord les 13 écarts encore ouverts du module de facturation (B0, B1), puis j'ajoute les fonctionnalités demandées en neuf lots (B2 à B10), chacun livré avec ses migrations, ses tests et sa doc. Le dossier de référence : [README.md](README.md), [ecarts.md](ecarts.md), [ameliorations.md](ameliorations.md).

## 1. Décisions retenues

| N° | Sujet | Décision |
| --- | --- | --- |
| D1 | Devis accepté | Révision admise : retour en négociation, nouvelle version, signature annulée, nouvelle acceptation |
| D2 | Paiement d'une proforma | La proforma passe en devis, puis en facture (détail ci-dessous) |
| D3 | TVA des acomptes | Validé le 13/11/2026 : TVA portée par la facture finale ; en régime des encaissements, exigible au fur et à mesure que les acomptes sont déduits de la facture finale, pas à leur encaissement |
| D4 | Acompte d'un devis expiré | Au choix : rembourser, garder en avoir, ou remettre le devis en validité en changeant sa date de validité |
| D5 | Stock insuffisant | Alerte sur le devis, blocage à la facture ; la facture s'émet quand même si l'article autorise le stock négatif |
| D6 | Changer le client d'une facture payée | Le système propose les deux : paiements gardés en avoir au client d'origine, ou transférés sur la facture du nouveau client |
| D7 | Statut VOIDED | Retiré : CANCELLED suffit |
| D8 | Facture scellée non payée modifiée | À confirmer : facture rectificative, montrée comme nouvelle version (une pièce scellée ne change pas) |
| D9 | Proforma | Modifiable quel que soit son statut, avec versions |
| D10 | Facture non payée | Modifiable avec versions, sans suppression, après approbation d'un responsable ou d'un administrateur |
| D11 | Validation par un N+ | Un niveau supérieur qui valide en avance approuve la demande, le niveau sauté est tracé |
| D12 | Centres d'approbation | Distincts par module (facturation, caisse), sur un même modèle de données et un même moteur |

**D2 expliqué.** Une proforma est un document commercial sans valeur fiscale : le client s'en sert pour faire valider un budget ou préparer son paiement. Elle ne doit donc ni se sceller ni s'encaisser comme une facture, ce que le code permet aujourd'hui.

Ma proposition partait d'un paiement reçu sur la proforma : paiement total, conversion directe en facture ; paiement partiel, facture d'acompte. Votre circuit garde le devis au milieu, et c'est celui que je retiens :

1. La proforma est envoyée au client.
2. À son accord, ou à son paiement, elle devient un **devis accepté**, avec les mêmes lignes et le lien vers la proforma.
3. Un paiement reçu à ce moment est enregistré comme **acompte du devis** (lot B3).
4. Le devis devient une **facture**, qui déduit l'acompte.

Avantage : un seul chemin pour tout le monde (devis, acompte, facture), et les contrôles du devis (prix, stock, remise) s'appliquent aussi aux ventes parties d'une proforma.

## 2. Ordre de réalisation

Seize lots, livrés un par un sur `feature/facturation-devis` ; chacun est utilisable dès sa livraison.

| Étape | Lot | Pourquoi à ce rang | S'appuie sur |
| --- | --- | --- | --- |
| 1 | B0 Sécurité et intégrité | Un défaut de sécurité (signature publique) et deux défauts de calcul | |
| 2 | B1 Cycle de vie | Toutes les fonctions suivantes s'appuient sur une table des transitions fiable | B0 |
| 3 | B12 Auteur et visa | L'utilisateur connecté signe chaque pièce et chaque version | B1 |
| 4 | B15 Centres d'approbation | Modèle commun, hiérarchie et circuits, dont dépendent les modifications approuvées ; centre de la caisse branché dessus | B12 |
| 5 | B13 Simulation interactive | Un seul calcul, ligne par ligne, descriptions longues | B1 |
| 6 | B2 Négociation des devis | Cœur du devis : versions, prix, lignes | B1, B13 |
| 7 | B14 Facture modifiable avant paiement | Versions sans suppression, approbation | B2, B15 |
| 8 | B3 Acomptes | Acomptes en montant ou en pourcentage, plan d'acomptes, sur devis et sur facture, `4191` | B2, B14 |
| 9 | B8 Montant ventilé | Saisie d'un total, ventilation des taxes et des lignes | B13 |
| 10 | B11 Affaires | Regroupe devis, proformas, acomptes, factures et paiements | B2, B3 |
| 11 | B5 Proforma | Modifiable à tout statut, circuit proforma, devis, facture | B2, B3 |
| 12 | B4 Envois | Destinataires, copies, WhatsApp, journal | B1 |
| 13 | B6 Changement de client | Avoir et nouvelle facture, choix pour les paiements | B3, B14 |
| 14 | B7 Devis et inventaire | Stock, réservation, ressources | B2 |
| 15 | B9 États clients | Relevé, balance âgée, échéanciers | B1 |
| 16 | B10 Catalogue, routes, messages | Catalogue complet, routes uniques, messages français | Tous |

## 3. B0 et B1 : sécurité et cycle de vie

B0 ferme une fuite de données et deux défauts de calcul ; B1 rend le statut d'un document toujours juste.

**B0**
- **Signature publique d'un devis** : la réponse ne donne plus que le numéro, la date, le montant, la devise et l'état de la signature ; plus de notes internes. Signature refusée (409) sur un devis annulé, refusé ou converti, ou révisé depuis la demande.
- **Annuler une rectificative payée** : l'avoir accepte facture, rectificative et note de débit scellées.
- **Exonération de TVA** : le recalcul d'un brouillon applique les conditions du client comme à la création.

**B1**
- Une **table des transitions** par type (devis, facture, avoir, proforma), seule autorité sur les statuts.
- `send` trace l'envoi sans toucher au statut ; un brouillon s'émet avant de partir.
- `validate` exige un document émis : plus de scellement qui saute le contrôle des remises.
- Avoir : émis, puis imputé (APPLIED) ou remboursé (REFUNDED) ; émettre ne le consomme plus.
- Devis échus passés en EXPIRED chaque nuit ; acompte abandonné en CANCELLED ; statut VOIDED retiré (D7).
- `recoveredBy` et `changedBy` pris dans le jeton de l'utilisateur, jamais dans la requête.

**Tests** : une signature sur un devis annulé est refusée sans note interne ; une facture payée envoyée reste PAID ; un brouillon ne se valide pas sans émission ; un avoir émis reste imputable.

## 4. B2 : négociation et révision des devis

Un devis se modifie tant qu'il n'est pas converti : prix, quantités, remises, lignes ajoutées ou retirées, chaque fois dans une nouvelle version.

- **Statuts modifiables** : brouillon, envoyé, vu, en négociation. Un devis accepté se révise aussi (D1) : il repasse en négociation, sa signature est annulée, le client l'accepte de nouveau.
- **Chaque modification = une version**, avec l'auteur, le motif et l'origine (proposition du client ou contre-proposition).
- **Frise de négociation** : `GET /quotes/{n}/negotiation` montre les versions et l'écart de prix entre elles.
- **Contrôles à chaque version** : prix plancher, remise maximale et marge minimale du catalogue ; au-delà, dérogation motivée avec le droit adapté.
- **Validité** : prolongée à la révision (nombre de jours en paramètre).

**Test** : un devis en négociation reçoit une baisse de prix et une ligne de plus ; la version 2 montre les deux changements, un prix sous le plancher est refusé sans dérogation.

## 5. B3 : acomptes, du devis à la facture

Un devis peut porter plusieurs acomptes ; ce qui n'a pas été pris sur le devis se demande sur la facture ; la facture finale déduit tout.

- **Côté devis** : un acompte se demande à tout moment sur un devis déjà créé, **en montant ou en pourcentage** ; un **plan d'acomptes** en règle plusieurs d'un coup (par exemple 30 % à la commande, 20 % à mi-parcours, avec leurs échéances). Chacun donne une **facture d'acompte** numérotée, envoyée et encaissable par tous les moyens (caisse, mobile money, virement, portefeuille).
- **Côté facture** : exactement les mêmes possibilités (un ou plusieurs acomptes, montant ou pourcentage, plan d'acomptes) ; un acompte non pris sur le devis se demande ainsi sur la facture.
- **Facture finale** : une ligne « Acompte n° … du … » par acompte facturé ; seul le reste est réclamé.
- **Devis expiré avec acompte encaissé (D4)** : au choix, remboursement, avoir gardé pour le client, ou devis remis en validité avec une nouvelle date de validité.
- **TVA (D3)** : TVA portée par la facture finale ; en régime des encaissements, exigible au fur et à mesure que les acomptes sont déduits de la facture finale, pas à leur encaissement (validé le 13/11/2026).
- **Comptabilité** : l'acompte au crédit du `4191` (clients, avances et acomptes reçus) ; la facture finale solde le `4191` contre le `411`.
- **Routes** : `POST /quotes/{n}/deposits`, `POST /invoices/{n}/deposits`, `GET /documents/{n}/deposits`, `PATCH /deposits/{id}/cancel`, `PATCH /quotes/{n}/revalidate`.

**Test** : devis de 1 000 000 avec deux acomptes de 30 % et 20 %, tous deux encaissés ; la facture finale affiche 1 000 000, déduit 500 000, réclame 500 000, et le `4191` est soldé.

## 6. B4 : envois, destinataires et canaux

L'envoi choisit ses destinataires et son canal, et chaque envoi est tracé ; il ne change jamais le statut du document.

- **Destinataires** : principaux, copie, copie cachée ; parmi les adresses de facturation du membre, les contacts de l'organisation, ou saisies librement.
- **Préférence par client** : par exemple « les factures de cette organisation partent à sa comptabilité ».
- **Canaux** : courriel avec le PDF joint ; WhatsApp par Infobip avec les modèles déjà créés (`facture_payee`, `rappel_paiement`…) et le bouton vers `espace.elleaose.com`.
- **Journal des envois** : date, auteur, canal, destinataires, et pour WhatsApp le statut de remise et de lecture.
- **Duplicata** : après une correction de destinataire sur une pièce scellée, le PDF envoyé est un duplicata daté ; l'original reste la référence fiscale.
- **Routes** : `POST /documents/{n}/send`, `GET /documents/{n}/sendings`, `GET` et `PUT /customers/{type}/{code}/billing-recipients`.

**Test** : une facture d'organisation part à deux adresses en copie plus une adresse saisie ; le journal montre les trois ; le statut ne change pas.

## 7. B5 : proforma

Une proforma devient un document sans valeur fiscale, qui suit le circuit proforma, devis, facture (D2).

- Ni scellement, ni écriture comptable, ni encaissement direct.
- **Modifiable quel que soit son statut**, avec une version à chaque modification (D9).
- Création directe, ou depuis un devis ; date de validité ; statuts : brouillon, envoyée, acceptée, expirée, transformée.
- À l'accord ou au paiement : transformation en **devis accepté** (mêmes lignes, lien conservé) ; un paiement reçu devient **acompte du devis**.
- PDF : mention « Facture proforma, document sans valeur fiscale ».
- **Routes** : `POST /proformas`, `POST /proformas/{n}/to-quote`, `POST /quotes/{n}/proforma`.

**Test** : une proforma payée à 40 % devient un devis accepté avec un acompte de 40 %, puis une facture qui le déduit ; la proforma ne compte dans aucun total fiscal ni comptable.

## 8. B6 : changement de client sur une facture

Le changement de client est libre avant scellement ; après scellement, il passe par un avoir et une nouvelle facture, et le système propose le sort des paiements (D6).

- **Avant scellement** : changement avec motif ; reprise de la fiche du nouveau client (adresse, NIU, conditions, exonération, devise), recalcul, nouvelle version.
- **Après scellement**, en une action guidée :
  1. avoir total au client d'origine ;
  2. nouvelle facture identique au nouveau client ;
  3. paiements reçus : au choix, gardés en avoir au client d'origine (à rembourser ou imputer), ou transférés sur la nouvelle facture, le payeur réel restant tracé.
- **Comptabilité** : écritures de l'avoir et de la nouvelle facture sur le `411` de chacun.
- **Route** : `POST /invoices/{n}/change-customer` `{customerType, customerCode, reason, payments: KEEP_AS_CREDIT | TRANSFER}`.

**Test** : une facture scellée et payée de l'organisation A passe à l'organisation B, une fois avec chaque choix ; les comptes `411` de A et de B sont justes.

## 9. B7 : devis et inventaire

Une ligne de devis peut viser un article d'inventaire ou une ressource, et le système vérifie la disponibilité dès la saisie (D5).

- **Article** : choix dans le catalogue ou l'inventaire, prix repris, vérification du stock disponible (en stock moins réservé), du prix plancher et de l'article actif.
- **Ressource** (salle, bureau) : disponibilité aux dates demandées.
- **Stock insuffisant** : alerte précise sur le devis (« 2 manquantes ») ; blocage à l'émission de la facture, sauf si l'article autorise le stock négatif (réglage par article), ou dérogation avec droit.
- **Réservation** du stock à l'acceptation, libérée au refus ou à l'expiration ; **sortie** du stock à la facturation.
- **Route** : `POST /documents/check-availability`, appelée aussi à la création et à la mise à jour.

**Test** : 12 chaises demandées pour 10 disponibles : alerte sur le devis ; facture bloquée ; facture émise si l'article autorise le stock négatif.

## 10. B8 : saisie d'un montant ventilé par le système

L'utilisateur saisit un total, et le système calcule les taxes et répartit le montant sur les lignes, au franc près.

- **Saisie** : un total TTC (ou HT) pour tout le document, ou pour un groupe de lignes.
- **Calcul** : HT, TVA et centimes additionnels selon la règle fiscale de chaque ligne ; répartition au prorata des prix du catalogue (ou des quantités saisies) ; arrondi au franc, le reste sur la dernière ligne.
- **Remise implicite** : l'écart avec le prix catalogue apparaît comme une remise, soumise au plancher et à la remise maximale.
- **Simulation** : même calcul qu'à la création, avec exonération de l'organisation et plafond d'encours (écart E17).
- **Route** : `POST /documents/allocate-amount`.

**Test** : 500 000 TTC sur trois lignes à 18 % donnent des HT et des TVA dont la somme fait exactement 500 000, avec la remise implicite affichée.

## 11. B9 et B10 : états clients, catalogue, routes et messages

**B9, états clients**
- **Relevé client** : pagination complète, tri respecté, export CSV et PDF de tout le relevé, envoi par les canaux de B4 ; tiré du compte `411` quand la comptabilité est active.
- **Balance âgée** : par devise, jamais de cumul entre devises.
- **Échéanciers** : réservés aux factures émises non soldées ; un paiement direct se répartit sur les échéances dans l'ordre.

**B10, catalogue, routes et messages**
- **Catalogue** : familles, prix par segment de client et prix datés avec historique, lien vers l'article d'inventaire, compte de produit propre à l'article, import et export CSV.
- **Routes** : une seule famille `/quotes/...` ; les routes `/quotations/...` restent six mois, dépréciées.
- **Messages** : en français accentué, avec un code d'erreur stable pour le frontend.

## 13. B11 : gestion des affaires

Une affaire regroupe tout ce qui concerne une vente à un client, du premier devis au dernier paiement.

- **Fiche** : client, intitulé, responsable, montant prévu, probabilité, étape (prospection, proposition, négociation, gagnée, perdue avec motif).
- **Création** : à la main, depuis une opportunité du CRM, ou au premier devis d'un client.
- **Tableau** : devisé, accepté, facturé, encaissé, reste à facturer et à encaisser, acomptes, marge prévue et réalisée.
- **Liens** : chaque document porte son affaire, gardée à la conversion du devis en facture.
- **Routes** : `GET`, `POST`, `PUT /billing/deals`, `GET /billing/deals/{code}`, `PATCH /billing/deals/{code}/stage`.

**Test** : une affaire avec un devis refusé, un devis accepté, un acompte et une facture payée en partie affiche les bons totaux.

## 14. B12 : auteur de la pièce et visa

L'utilisateur connecté signe chaque pièce sans rien saisir.

- Son nom et sa fonction, repris du jeton, remplissent « Établi par ».
- Le **visa** de la facture affiche le même nom, la fonction et la date, et sa signature enregistrée si elle existe.
- Chaque version garde son auteur ; le PDF montre l'auteur de la version imprimée.

**Test** : une facture créée par un commercial porte son nom dans « Établi par » et dans le visa.

## 15. B13 : simulation interactive et descriptions

Chaque saisie sur une ligne renvoie aussitôt son calcul et celui du document.

- **Ligne par ligne** : HT, remises, TVA, centimes additionnels, TTC, et les alertes (prix plancher, remise maximale, stock, plafond d'encours), sans rien créer.
- **Un seul calcul** pour la simulation, la création et la mise à jour.
- **Descriptions longues** : description et description détaillée jusqu'à 4 000 caractères, retours à la ligne gardés à l'impression ; notes et conditions aussi.
- **Routes** : `POST /billing/documents/simulate-line`, `POST /billing/documents/simulate`.

**Test** : changer une quantité met à jour la ligne et les totaux en moins d'une seconde ; 2 000 caractères de description s'impriment entiers.

## 16. B14 : facture modifiable avant paiement

Une facture se modifie tant qu'elle n'est pas payée, par versions, sans rien supprimer, après approbation.

- **Tant qu'aucun paiement n'est imputé** ; ensuite, avoir ou rectificative.
- **Versions** : chaque modification en crée une, les précédentes restent consultables et comparables (auteur, motif, date).
- **Approbation** : la modification d'une facture émise attend l'accord d'un responsable ou d'un administrateur dans le centre d'approbation ; la version en vigueur ne change qu'à l'accord.
- **Facture scellée** : la pièce scellée ne change pas ; la modification approuvée produit une rectificative, montrée comme nouvelle version (D8, à confirmer).
- **Comptabilité** : contre-passation de l'ancienne écriture et nouvelle écriture.

**Test** : une facture émise non payée est modifiée ; la demande attend ; une fois approuvée, la version 2 est en vigueur, la version 1 reste visible, rien n'est supprimé.

## 17. B15 : centres d'approbation et hiérarchie

Des centres distincts (facturation, caisse) sur un même modèle de données et un même moteur, avec une hiérarchie N+1, N+2, et la validation en avance par un niveau supérieur (D12).

- **Hiérarchie** : chaque utilisateur a son responsable direct ; organigramme, intérims et délégations datées.
- **Circuits** : un par type de demande (modification de facture, dérogation de prix ou de remise, demande de fonds…), niveaux selon le montant ou l'objet.
- **Validation en avance** : si le N+2 approuve avant le N+1, la demande passe à « approuvée », le niveau sauté est noté « approuvé par le N+2 » (D11).
- **Suivi** : mes demandes (où, qui doit agir, depuis quand), à approuver (moi ou par délégation), notifications, relances, escalade.
- **Historique** : toutes les demandes et décisions passées, filtrables, avec la frise complète de chaque demande.
- **Modèle commun** : demande d'approbation, étapes, décisions, hiérarchie et circuits dans les mêmes tables ; chaque centre a son écran, ses droits et sa file. Un nouveau centre se branche sans nouvelle table.
- **Demandes de fonds de la caisse** : elles ont déjà file, frise, délégations et escalade, mais leurs niveaux viennent du montant et un N+ ne peut pas valider en avance. Elles passent sur le modèle commun ; le centre de la caisse garde son écran et ses routes.
- **Routes** : communes `/approvals/hierarchy`, `/approvals/circuits` ; par centre `/approvals/{center}/mine`, `/to-approve`, `/history` ; `PATCH /approvals/{id}/approve`, `/reject`, `/send-back`.

**Test** : une demande à deux niveaux approuvée d'abord par le N+2 passe à « approuvée » ; la frise montre le niveau 1 approuvé par le N+2 ; elle figure dans l'historique des deux.

## 18. Livraison

- **Branche** : `feature/facturation-devis`, poussée après chaque lot ; vous fusionnez et déployez vous-même.
- **Migrations** : dans les deux dossiers (`db/migration` et `db/migration-prod`), numérotées au-dessus de la plus haute (dev V324, prod V318 pour la première).
- **Tests** : un test d'intégration par lot sur le schéma de production, plus les tests unitaires ; toute la suite passe avant chaque envoi.
- **Doc** : un dossier backend par lot, et la doc frontend finale (`docs/final-frontend-doc/billing`) mise à jour à chaque lot, routes régénérées depuis le code.
- **Écarts réglés** : marqués « Réglé » dans `ecarts.md` et dans la doc frontend à chaque lot.
