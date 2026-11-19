# Constat des listes de choix, recherches et automatisations

> Relevé du 18/11/2026 sur `feature/booking` (lecture du code, sans modification), à la demande de l'utilisateur. Constat : beaucoup de champs se saisissent au clavier alors qu'ils devraient être des listes déroulantes alimentées par le serveur. L'administrateur ne peut pas enrichir ces listes depuis son panneau, des recherches manquent, et des étapes manuelles devraient être automatiques. Périmètre demandé : comptabilité, demande de fonds, approbation, puis tous les modules mis à jour récemment.

## Fiches

| Fiche | Modules |
| --- | --- |
| [comptabilite.md](comptabilite.md) | Comptabilité (grand livre, banque, analytique, budget, achats, immobilisations, financement, paie, clôture, régularisations, provisions, TVA, exports) |
| [fonds-approbation.md](fonds-approbation.md) | Demande de fonds, approbation |
| [facturation-paiement-abonnement.md](facturation-paiement-abonnement.md) | Facturation, paiement (caisse, PawaPay, portefeuille), abonnement (plans, promotions, pass, dépassements) |
| [documents-audit-ressources-portail.md](documents-audit-ressources-portail.md) | Documents (GED, KYC, modèles), centre d'audit, ressources, réservations, portail, notifications et WhatsApp |

## Chiffres

| Module | Valeurs fixes non exposées | Recherche de choix manquante | Texte libre à mettre en liste | Recherche manquante | Automatisation manquante | Total |
| --- | --- | --- | --- | --- | --- | --- |
| Comptabilité | 58 | 73 | 37 | 16 | 31 | 215 |
| Demande de fonds | 7 | 24 | 6 | 10 | 19 | 66 |
| Approbation | 6 | 5 | 3 | 6 | 9 | 29 |
| Facturation | 11 | 9 | 6 | 7 | 5 | 38 |
| Paiement | 7 | 9 | 8 | 6 | 2 | 32 |
| Abonnement | 2 | 9 | 6 | 8 | 4 | 29 |
| Ressources | 17 | 4 | 6 | 8 | 2 | 37 |
| Réservations | 3 | 1 | 4 | 4 | 3 | 15 |
| Documents | 10 | 6 | 7 | 8 | 2 | 33 |
| Portail | 1 | 0 | 1 | 4 | 1 | 7 |
| Notifications et WhatsApp | 3 | 4 | 0 | 3 | 1 | 11 |
| Centre d'audit | 3 | 2 | 2 | 0 | 0 | 7 |
| **Total** | **128** | **146** | **86** | **80** | **79** | **519** |

À cela s'ajoute un manque d'administration critique : le formulaire des promotions, dont la moitié des réglages ne s'écrit que par la base.

## Causes communes

1. **Aucun mécanisme commun.** Trois bonnes pratiques isolées :
   - `/inventory/reference/enums` (valeurs fixes avec libellés) ;
   - les listes du catalogue de facturation (tables à attributs, mode souple ou strict) ;
   - les valeurs de référence de la caisse (listes de l'administrateur avec « Autre, préciser » et décompte des usages).

   Chaque module réinvente ou s'en passe.
2. **Valeurs fixes cachées** dans les services (`Set.of`, `switch`), parfois non contrôlées : l'écran ne peut pas les connaître, et une faute de frappe passe sans bruit.
3. **Recherches derrière des droits d'administration** (`SYSTEM:USERS`, `CASH:READ`, `INVENTORY:READ`, `BUDGET_REPORT:READ`…) que n'a pas l'utilisateur du formulaire.
4. **Listes complètes non paginées**, sans `q`.
5. **Motifs en texte libre** partout (rejets, annulations, corrections, renvois, réductions) : aucune statistique possible.
6. **« Qui a fait » envoyé par le client** au lieu d'être lu dans la session (paiement, abonnement, réservations).
7. **Codes « nombre de lignes + 1 »** au lieu du générateur de séquences (comptabilité).

## Mécanisme commun proposé (socle)

Un seul jeu de briques dans `core/reference`, que chaque module branche :

| Brique | Routes | Rôle |
| --- | --- | --- |
| **Valeurs fixes** | `GET /reference/enums?module=&group=` (et une route par module pour la compatibilité, comme `/inventory/reference/enums`) | Registre unique des énumérations et ensembles en dur : code, libellé français, description, ordre, sélectionnable. Le contrôle des requêtes lit le même registre : exposition et contrôle ne divergent jamais. Mis en cache. |
| **Listes de l'administrateur** | `GET /reference-lists?module=` (liste des listes, vides comprises, avec libellé et usage) ; `GET /reference-lists/{listCode}` (valeurs actives, ou toutes pour l'administration) ; `POST`, `PATCH`, `PUT` (remplacer), désactivation | Promotion au socle des valeurs de référence de la caisse : code, libellé, ordre, actif, « précision obligatoire », valeur système non supprimable, décompte des usages. Aides `require(list, code)` et `describe(list, code, text)`. Mode souple (valeur inconnue ajoutée et signalée) ou strict (refus) par liste. Les listes de la caisse migrent sans changement d'API. |
| **Catalogues à attributs** | Restent propres au module (modèle du catalogue de facturation) | Natures de risque, retenues, conditions de paiement, profils saisonniers… |
| **Recherches de choix** | Convention `GET /{module}/lookup/{type}?q=&page=&size=&includeInactive=` rendant `{ code, label, hint, active }` | Ouvertes au droit du formulaire qui s'en sert (et non au droit d'administration de la donnée). Recherches transverses : tiers, utilisateurs assignables, documents. |
| **Motifs** | Tout motif devient `reasonCode` (liste de l'administrateur) plus `reasonText` (précision), en gardant l'ancien champ texte accepté | Statistiques et filtres sur les motifs. |
| **Acteur** | Lu dans la session partout ; les champs `createdBy`, `changedBy`… des requêtes sont ignorés | Traçabilité fiable. |
| **Listes** | Pagination (`PaginatedResponse`), `q` et filtres sur toute liste d'écran | |

## Avancement

**L0 fait** le 19/11/2026 ([lot-l0-socle.md](lot-l0-socle.md)) : registre des valeurs fixes, listes de l'administrateur au socle (listes de la caisse reprises), convention des recherches de choix, motifs, acteur de la session. L1 à L4 : à la demande.

## Lots proposés

| Lot | Contenu |
| --- | --- |
| **L0. Socle** | Registre des valeurs fixes, listes de l'administrateur au socle (avec migration des listes de la caisse), convention des recherches, motifs, acteur lu dans la session, écran d'administration des listes (routes). |
| **L1. Comptabilité** | Valeurs fixes, recherche des tiers, recherches paginées, motifs, recherche des écritures et des achats, catalogues à attributs, trésorerie et séquences, automatisations (lettrage, rapprochement, factures non parvenues, écritures récurrentes, tâches de fin de période). |
| **L2. Demande de fonds et approbation** | Lot de référence des fonds, recherches ouvertes au demandeur, références de l'approbation, circuits de l'administrateur, motifs, recherche paginée, valeurs par défaut, besoins dans le moteur, délégations depuis les absences, relances en étapes, reliquats. |
| **L3. Facturation, paiement, abonnement** | Administration des promotions, valeurs fixes, lignes de facture branchées sur le catalogue, devises, motifs, codes comptables par recherche, versions de plan par recherche, pagination, automatisations. |
| **L4. Documents, audit, notifications, portail** | Références GED et KYC, motifs contrôlés, exigences typées, recherches, catalogues WhatsApp, facettes d'audit, manques du portail. |
| Ressources et réservations | Dans le [plan booking](../booking/README.md) (lots B7 et B10). |

Ordre conseillé : L0 d'abord (les autres lots en dépendent), puis L1 et L2 (demandés en premier), puis L3 et L4.
