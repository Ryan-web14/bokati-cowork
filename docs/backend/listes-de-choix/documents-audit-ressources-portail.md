# Constat : documents, audit, ressources, réservations, portail, notifications

> Partie du [constat des listes de choix](README.md). Relevé du 18/11/2026 sur `feature/booking` (lecture du code, sans modification). Chemins relatifs à `src/main/java/com/sni/bokaticowork/features/` (`core/` pour l'audit et WhatsApp).

## En bref

| Module | Valeurs fixes non exposées | Recherche de choix manquante | Texte libre à mettre en liste | Recherche manquante | Automatisation manquante |
| --- | --- | --- | --- | --- | --- |
| Ressources | 17 | 4 | 6 | 8 | 2 |
| Réservations | 3 | 1 | 4 | 4 | 3 |
| Documents (GED, KYC, modèles) | 10 | 6 | 7 | 8 | 2 |
| Portail | 1 | 0 | 1 | 4 | 1 |
| Notifications et WhatsApp | 3 | 4 | 0 | 3 | 1 |
| Centre d'audit | 3 | 2 | 2 | 0 | 0 |

Aucun de ces modules ne rend ses valeurs fixes avec leurs libellés. La seule liste que l'administrateur peut enrichir ici est celle des motifs KYC (`/kyc/reasons`), utilisée pour les décisions de dossier KYC et nulle part ailleurs. Les `routes.json` de la doc frontend listent les valeurs, mais statiquement : ce n'est pas une source serveur.

## Ressources

| Champ ou route | Nature | Constat | Proposition |
| --- | --- | --- | --- |
| `status`, `bookingUnit`, `adjustmentType`, `customerSegment`, `channel`, `holidayMode`, `daysOfWeek`, fermeture (`kind`, `recurrence`), jour particulier (`kind`), gravité d'incident, moment et priorité des modèles de tâche, nature et base des coûts, `groupBy` de l'occupation | Valeurs fixes | Énumérations ou ensembles en dur (`ResourcePricingRuleServiceImpl.java:390-391, 579`, `ResourceOperations.java:45-46`, `ResourceAnalytics.java:173`) ; `daysOfWeek` en texte « 1,2,3 » | `GET /resources/reference` : codes et libellés français de toutes ces listes ; ensembles en dur passés en énumérations |
| `Site.timeZone` | Valeurs fixes | Texte libre | Fuseaux admis, défaut Africa/Brazzaville |
| `planCode` des règles | Recherche | Code libre | `/subscriptions/plans/lookup?q=` ; contrôle |
| `assignedTo` des modèles de tâche | Recherche | Identifiant brut ; `/admin/users/search/by-name` non paginé, droit `SYSTEM:USERS` | Recherche du personnel assignable, ouverte à l'exploitation |
| `serviceItemCode` des modèles | Recherche | Non contrôlé | `/billing/catalog/lookup` à l'écran ; contrôle à l'enregistrement |
| `typeId`, `groupId`, `policyId` de la ressource | Recherche | `/search/basic` exige `query`, non paginé ; champs nommés `…Id` mais prennent un code | Requête facultative, pagination ; renommer en `…Code` (en gardant l'ancien nom) |
| Motif de fermeture, objet et résolution d'incident, motif des opérations manuelles | Texte en liste | Libres | Listes de l'administrateur (motifs de fermeture, catégories d'incident, codes de résolution) et précision |
| `zone`, `locationLabel`, `floorLabel` | Texte en liste | Doublons du placement site et étage | Déduits du placement |
| Libellé d'un jour particulier | Texte en liste | Saisi | Les fériés nationaux sont déjà calculés (R5a) ; les autres restent saisis |
| Groupes, sites, lots, modèles de tâche, incidents, fermetures, règles tarifaires ; `/search` des référentiels | Recherche | Listes sans pagination ni filtres suffisants | Pagination, `q`, filtres (gravité, dates, nature, actif, unité, segment, canal, plan) |
| Incident bloquant | Automatisation | Statut MAINTENANCE seulement, sans fermeture : ni durée au calendrier, ni plage dans l'historique | Créer une fermeture de maintenance, la lever à la résolution |
| Étage et emplacement | Automatisation | Ressaisis malgré le placement | Remplis depuis le site et l'étage |

## Réservations

| Champ ou route | Nature | Constat | Proposition |
| --- | --- | --- | --- |
| Filtres `status`, `ownerType`, `paymentMode`, `bookingUnit`, statut de liste d'attente ; `audienceType` ; état des lieux `condition` | Valeurs fixes | Non exposés ; GOOD et DAMAGED en dur | `GET /bookings/reference` ; énumération de l'état |
| `memberId`, `customerId`, `businessCode` | Recherche | Trois recherches séparées | `/bookings/owners/lookup?q=` (code, libellé, type) |
| Motifs d'annulation, d'absence, de refus, de dérogation de quota, de transfert, de déplacement et de changement de ressource (back-office et portail) | Texte en liste | Libres | Listes de motifs de l'administrateur (`reasonCode` et précision), sous-ensemble visible au portail |
| `/bookings/search`, `/bookings/waitlist`, `/bookings/policies`, `/bookings/quota-overrides` | Recherche | Listes sans pagination ; politiques et dérogations sans modification ni retrait | Pagination, filtres ; modification, désactivation, révocation |
| Expiration des blocages, rappels | Automatisation | Routes manuelles seulement, aucune tâche planifiée | Tâches planifiées ; rappel créé à la confirmation |
| `changedBy`, `actor`, `approvedBy` | Automatisation | Lus dans la requête | Lus dans la session |

Ces points entrent dans le [plan du module booking](../booking/README.md).

## Documents (GED, KYC, modèles)

| Champ ou route | Nature | Constat | Proposition |
| --- | --- | --- | --- |
| `ownerType`, `space`, `statuses`, `category` ; statut et risque KYC ; règle croisée (`ruleType`, champs comparés) ; conservation (`retentionReference`, `action`) ; accès GED (`granteeType`, `level`) ; `targetType` des gels | Valeurs fixes | Non exposés ; champs comparables en dur | `GET /documents/reference` et `/kyc/reference` avec libellés |
| `kycSide`, `side` | Valeurs fixes | Texte libre ; tout ce qui n'est pas BACK devient FRONT sans erreur | Énumération FRONT, BACK, contrôlée |
| `workflowType`, `signerType` | Valeurs fixes | Texte libre ; `workflowType` jamais utilisé | Énumérations, ou retrait |
| Types et modes des champs de modèle | Valeurs fixes | Absents du catalogue des champs | Ajoutés au catalogue avec libellés |
| `customerType`, `businessLegalForm` des exigences | Recherche | Chaînes alors que les énumérations existent : une faute de frappe désactive l'exigence sans bruit | Typés et exposés |
| `documentTypeCode` (exigences, règles, conservation, circuits, dossiers) | Recherche | `/document-types` sans `q` ni pagination | `/document-types/lookup?q=&ownerType=` |
| `rejectionReasonCode` des documents | Recherche | Enregistré sans contrôle (`DocumentServiceImpl.java:619`), non relié à `/kyc/reasons` | Contrôlé contre la liste des motifs |
| `assignedTo`, `reviewedBy`, destinataire d'un accès GED | Recherche | Identifiant brut, courriel ou rôle saisis | Recherches d'examinateurs, d'utilisateurs et de rôles |
| Catégorie des types de document | Texte en liste | Énumération figée | Liste de l'administrateur |
| Notes de correction, motifs de rejet groupé, de gel, de levée et de corbeille | Texte en liste | Libres | `reasonCode` depuis les motifs |
| Clés de métadonnées, groupe d'alternatives | Texte en liste | Saisis | Schéma par type de document ; choix parmi les groupes existants |
| Étiquettes, conservation, circuits, règles croisées, modèles, renouvellements, doublons, gels, dossiers KYC | Recherche | Filtres absents ou partiels, pas de pagination, pas de `q` | Filtres, `q`, pagination ; examinateur assigné |
| Nom du type et libellé du groupe des exigences | Automatisation | Ressaisis | Déduits du code |

## Portail

| Champ ou route | Nature | Constat | Proposition |
| --- | --- | --- | --- |
| `side` du dépôt KYC | Valeurs fixes | Texte libre | Énumération exposée avec les exigences |
| Motif d'annulation | Texte en liste | Libre | Motifs d'annulation partagés (sous-ensemble visible) |
| Notifications, incidents du membre | Recherche | Non lues seulement ; le membre ne voit pas ses incidents | Historique paginé ; `GET /client/resource-incidents` |
| Ressources | Recherche | Type, groupe, `q`, capacité seulement ; ni groupes, ni équipements, ni sites listés | Filtres site, équipements, libre à un moment ; listes en lecture |
| `bookingUnit` du devis | Défaut | Accepté puis ignoré | Transmis ou retiré |

## Notifications et WhatsApp

| Champ ou route | Nature | Constat | Proposition |
| --- | --- | --- | --- |
| Canal, statut, type de destinataire ; catégorie, statut, langue et boutons des modèles WhatsApp | Valeurs fixes | Non exposés ; listes en dur (`WhatsAppService.java:381`) | `GET /notifications/reference` ; énumérations |
| `purpose` des modèles WhatsApp | Recherche | Texte libre alors qu'environ 21 usages fixes existent ; un usage sans modèle approuvé fait échouer l'envoi (`WhatsAppService.java:195`) | Catalogue des usages (code, libellé, champs, couverture) |
| `templateCode`, `eventType`, `eventTypes` des points d'accroche | Recherche | Libres | Catalogue des événements |
| `recipientCode` | Recherche | Libre | Recherches membres, clients, utilisateurs |
| Messages et modèles WhatsApp, notifications non lues | Recherche | Derniers 200, filtres absents | Filtres, pagination, historique |
| Modèles par défaut | Automatisation | Création manuelle ; un usage manquant ne se voit qu'à l'échec | Semés au démarrage ; rapport de couverture |

## Centre d'audit

| Champ ou route | Nature | Constat | Proposition |
| --- | --- | --- | --- |
| Statut, type d'événement de sécurité, statut d'alerte, résultat, granularité, tri, motif | Valeurs fixes | Chaînes non exposées | Exposées avec libellés |
| Filtres `module`, `action`, `entityType`, `ressource` | Recherche | Saisis | `GET /admin/audit-logs/facets` (valeurs distinctes) |
| Filtres d'utilisateur | Recherche | Saisis | Recherche d'utilisateur |
| Motif de révocation de session, note d'acquittement d'alerte | Texte en liste | Libres, `Map` | Requête typée et listes de motifs |

## Priorités

1. **Rappels et expiration des blocages de réservation** : routes manuelles seulement. Il faut des tâches planifiées, et un rappel créé à la confirmation (plan booking).
2. **« Qui a fait » lu dans la requête** (réservations, politiques) : il faut le lire dans la session.
3. **`GET /resources/reference`** : dix-sept listes en dur exposées d'un coup.
4. **Mécanisme commun de listes de motifs** : il sert aux réservations, aux fermetures, aux incidents, à la GED et aux dérogations.
5. **Motifs de rejet et de correction de la GED** contrôlés contre `/kyc/reasons`.
6. **`customerType` et `businessLegalForm` typés** dans les exigences documentaires.
7. **Recherches code et libellé** : types de document, plans, personnel assignable, examinateurs, titulaire de réservation. Les `/search` des ressources passent en pagination.
8. **Catalogues WhatsApp des usages et des événements**, modèles semés, rapport de couverture.
9. **Filtres et pagination** des listes des ressources, des réservations et de WhatsApp.
10. **Portail** : incidents du membre, historique des notifications, filtres de ressources, `bookingUnit` du devis. Un incident bloquant doit aussi créer une fermeture.
