# Lot A1 : journal des actions complet

Branche `feature/centre-audit`. Migrations : dev `V362__centre_audit_a1_actions.sql`, prod `V356__centre_audit_a1_actions.sql`.

## Ce qui change

- **Avant / après.** Pendant chaque appel `@Audited` (591 points, presque tous sur les contrôleurs), des écouteurs Hibernate (insertion, modification, suppression, `core/audit/change/EntityChangeListener`) notent ce qui est écrit en base. Le résultat va dans `audit_log.diff_json` : `{ "changes": [ { entityType, entityCode, operation, fields } ], "omitted": n }`. Aucun point d'appel n'a été modifié.
- **Entités principales** (décision du 14/11/2026, `AuditedEntities`) : `BillingDocument`, `PaymentTransaction`, `Subscription`, `Booking`, `Member`, `Customer`, `Users`, `Role`, `Permission`, `RolePermission`, `RoleUser`, `PlatformSetting`, `DocumentTemplate`, `NotificationTemplate`. Leurs champs modifiés sont gardés avec l'avant et l'après (à la création, les valeurs saisies). Les autres entités métier sont notées sans champs (type, code, opération). Les entités techniques du socle (journaux, boîte de sortie, numérotation) sont ignorées.
- **Code lisible** de chaque élément : numéro de facture, de paiement, d'abonnement, de réservation, identifiant du membre, du client, de l'utilisateur, nom du rôle, clé du paramètre, code du modèle ; sinon `code`, `number` ou `reference`, sinon `#id`. Une référence à un autre élément s'écrit `Type:code`, sans jamais charger un élément non chargé.
- **Élément principal** de l'action : `audit_log.entity_type` et `entity_code` (première entité principale touchée, sinon la première entité), avec index pour la chronologie d'un élément.
- **Masquage** (`AuditValues`) : un champ dont le nom contient un mot sensible (mot de passe, secret, jeton, code à usage unique, code PIN, clé d'API, clé privée, numéro de carte, empreinte, sel) est écrit `***`, même modifié. Un paramètre dont la clé désigne un secret (`...api-key`, `...secret`) a toutes ses valeurs masquées. Textes coupés à 300 caractères, binaires résumés.
- **Bornes** : 30 éléments et 40 champs par action ; au-delà, `omitted`. Deux écritures du même élément dans une action : premier avant, dernier après.
- **Échec** : la transaction est annulée, donc pas de `diff` ; l'élément principal reste noté.
- **Recherche** : filtres `entityType`, `entityCode`, `requestId`, `ressource` en plus des filtres existants (`AuditLogFilter`).
- **Export CSV** : `GET /admin/audit-logs/export`, mêmes filtres, 10 000 lignes au plus (`bokati.audit.export.max-rows`), formules de tableur neutralisées, export journalisé (`AUDIT` / `EXPORT_AUDIT_LOGS`).

## Limites

- Seules les écritures faites pendant un appel `@Audited` sont captées : les tâches planifiées et les traitements asynchrones ne produisent pas de ligne d'audit (le lot A4 trace les exécutions des tâches).
- Une écriture SQL directe (`JdbcTemplate`, requête native de mise à jour) échappe aux écouteurs Hibernate.

## Tests

`AuditValuesTest` (masquage par mots, mise en forme, fusion de deux écritures, cadres imbriqués et limite, résumé CSV) ; `EntityChangeA1IT` (vraie base : création, modification des seuls champs changés, suppression, paramètre secret masqué, rien hors d'une action, schéma validé) ; `BokatiCoworkApplicationTests` (démarrage complet).
