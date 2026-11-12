# Lots C4 et C6 : contrôles et pilotage des caisses

Lots du [plan finance](../plan-finance-comptabilite/README.md), détaillés dans [caisses-et-sessions.md](../plan-finance-comptabilite/caisses-et-sessions.md), sections 11, 13 et 14. Lots précédents : [C0](../finance-c0-caisse-correctifs/README.md), [C1](../finance-c1-referentiel-caisses/README.md), [C2](../finance-c2-sessions-comptages/README.md), [C3](../finance-c3-operations-transferts/README.md). Le lot C5 (instruments et devises) est fait avec le lot banque F10.

Branche `feature/finance-comptabilite`. Migrations : dev `V295__caisse_controles_pilotage_c4_c6.sql`, prod `V289__caisse_controles_pilotage_c4_c6.sql` (contenu identique).

## C4 : contrôles

### Plafonds appliqués

| Plafond | Effet |
| --- | --- |
| Détention (`maxCashAmount`, existant, **enfin appliqué**) | `capBehavior = BLOCK` : un encaissement en espèces (paiement au guichet, bon d'entrée) qui le dépasserait est refusé jusqu'à une remise. `ALERT` (par défaut) : il passe, et le responsable est prévenu au plus une fois toutes les 6 heures |
| Sortie par opération (`maxOutPerOperation`) | Au-delà : visa d'un caissier principal ou d'un superviseur (mouvement `PENDING`, lot C3) |
| Sorties du jour par caissier (`maxOutPerDay`) | Idem |
| Dépense directe sans demande | Plafond de la nature (lot C3) : au-delà, demande de fonds |
| Remboursement en espèces (`bokati.cash.refund.max-cash-amount`, 0 par défaut = sans limite) | Au-delà : virement ou mobile money imposé |

Les caisses existantes n'ont ni plafond de sortie ni comportement `BLOCK` : rien ne change pour elles tant qu'on ne les règle pas sur leur fiche.

### Réimpressions

`PATCH /cash-registers/movements/{n}/print` incrémente `printCount` et note le dernier auteur ; la date de première impression reste. La pièce PDF d'un mouvement déjà imprimé porte **DUPLICATA** ; celle d'un mouvement contre-passé porte **ANNULÉ PAR** suivi du numéro du mouvement inverse.

### Journal d'audit

Toutes les routes d'écriture des caisses passent par le journal d'audit (`@Audited`, module `CASH`), avec l'action (par exemple `CASH_OPEN_SESSION`, `CASH_FORCE_CLOSE`, `CASH_REVERSE`), l'auteur, l'adresse IP, l'URL, le succès ou l'échec : sessions, comptages, écarts, mouvements, visas, contre-passations, transferts, absences, scellés, fiche de caisse, affectations, natures et listes.

### Nouvelles anomalies

Ajoutées à l'analyse de session existante (`CashAnomalyType`) :

| Anomalie | Déclencheur | Sévérité |
| --- | --- | --- |
| `OPENING_VARIANCE` | Écart d'ouverture | Moyenne |
| `LONG_SESSION` | Au-delà de la durée maximale de la caisse | Faible, moyenne au double |
| `LONG_PAUSES` | Pauses cumulées de plus d'une heure | Faible |
| `REPEATED_REVERSALS` | 3 mouvements du caissier contre-passés (6 : élevée) | Moyenne |
| `DETENTION_CAP_EXCEEDED` | Solde au-delà du plafond de détention | Moyenne |
| `UNKNOWN_DEVICE` | Session ouverte depuis un poste non autorisé | Faible |
| `DAMAGED_NOTES` | Billets abîmés ou suspects comptés | Moyenne |
| `TRANSFER_VARIANCE` | Transfert reçu pour un autre montant | Moyenne |

**Caisse non comptée** : alerte au superviseur quand une caisse n'a pas été comptée depuis `notCountedAlertDays` jours (7 par défaut), au plus une fois par jour. **Comptage inopiné** : calendrier minimal par caisse (`spotCheckEveryDays`, 30 jours). Le retard est montré dans la position et l'état des comptages.

## C6 : pilotage

### Position de trésorerie en temps réel

`GET /cash-registers/position?locationCode=` (`CASH_REPORT:READ`). Pour chaque caisse :

- état, session en cours et caissier ;
- solde attendu (masqué si la caisse est à clôture aveugle et que l'utilisateur n'a pas `CASH_VARIANCE:DECIDE`) ;
- dernier comptage et son ancienneté ;
- plafond et taux d'occupation ;
- transferts en transit entrants et sortants (nombre, montant) ;
- écarts en revue, opérations en attente de visa ;
- dernier comptage inopiné et retard ;
- alertes : jamais comptée, non comptée depuis N jours, plafond presque atteint ou dépassé, transfert en transit depuis plus de 2 h, session suspendue, comptage inopiné en retard.

Le résultat donne aussi les totaux par site et pour l'ensemble.

### États

`GET /cash-registers/reports/{état}?registerCode=&from=&to=&format=csv` (`CASH_REPORT:READ`). Chaque état porte sa date d'édition et son auteur ; avec `format=csv`, l'export est en UTF-8, séparateur point-virgule.

| État | Contenu |
| --- | --- |
| `daily-blotter` | Brouillard de caisse d'un jour : chaque mouvement avec entrée, sortie, statut et solde progressif ; solde de début, entrées, sorties, solde de fin |
| `sessions` | Historique des sessions : ouverture, clôture, durée, fonds, compté, écart, décision, réviseur |
| `variances` | Écarts par origine, avec la décision, et le cumul par caissier |
| `transfers` | Transferts de la période : envoyé, reçu, statut, auteurs ; total en transit |
| `counts` | Dernier comptage de chaque caisse, ancienneté, dernier comptage inopiné et retard, billets abîmés |
| `outflows-by-nature` | Sorties par nature d'opération : nombre et montant |
| `pending-documents` | Procès-verbaux en attente : passations à confirmer, remises de caisse à signer, clôtures d'office dans leur délai de contestation |

Le livre de caisse comptable (SYSCOHADA) arrive avec le journal de caisse du lot F3.

## Droits

Nouvelle permission `CASH_REPORT_READ`, donnée à `SUPER_ADMIN`, `ADMIN`, `MANAGER`, `FINANCE`.

## Tests

- `CashControlsC4Test` : plafond de détention bloquant et en alerte, sortie au-delà du plafond par opération en attente, petite sortie confirmée, réimpression tracée.
- `CashDashboardServiceImplTest` : position avec attendu masqué ou visible, alertes et totaux par site ; brouillard du jour ; protection des valeurs dans le CSV.
- `AdminApiAuthorizationManagerTest` : droits des routes des lots C2, C3 et C6.
- `CashRoutesMappingTest` : routes de position.
- Suite unitaire complète : 2 037 tests, 0 échec.

## Pour le frontend

À reporter dans la doc frontend finale :

- tableau de bord de la position : cartes par caisse et alertes, totaux par site ;
- écran des états avec filtres et export CSV ;
- bannière de plafond : erreur 400 en mode blocage ;
- bouton de réimpression : appeler `print` avant de télécharger la pièce, qui sort alors en duplicata ;
- liste des anomalies, avec les nouveaux types.
