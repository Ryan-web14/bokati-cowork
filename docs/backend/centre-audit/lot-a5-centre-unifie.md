# Lot A5 : centre unifié

Branche `feature/centre-audit`. Pas de migration.

## Ce qui change

- **Chronologie commune** (`core/audit/timeline/TimelineService`) : 18 journaux lus là où ils sont, sans copie, sous une seule forme (`occurredAt`, `source`, `refId`, `kind`, `actor`, `entityType`, `entityCode`, `summary`, `requestId`, `ipAddress`, `status`), du plus récent au plus ancien.

| Source | Table | Élément | Acteur |
| --- | --- | --- | --- |
| `ACTION` | `audit_log` | élément principal (lot A1) | courriel |
| `SECURITY` | `security_event` | le compte (`Users`, `USR-...`) | identifiant saisi, code du compte |
| `ERROR` | `app_error` | | courriel |
| `REQUEST` | `request_log` | | courriel |
| `FISCAL` | `fiscal_audit_log` | type et code | code de l'acteur |
| `CONTRACT` | `contract_audit_event` | contrat | nom |
| `COMPLIANCE` | `compliance_audit_entry` | sujet | acteur |
| `WALLET` | `wallet_admin_action` | action d'administration du portefeuille | demandeur |
| `DOCUMENT_ACCESS` | `document_access_log` | document (identifiant) | courriel, code du compte |
| `APPROVAL` | `approval_event` | demande d'approbation (identifiant) | acteur |
| `SETTING` | `setting_audit_logs` | clé du paramètre | courriel, code du compte |
| `EMAIL` | `email_delivery_log` | élément lié | |
| `WHATSAPP` | `whatsapp_message` | référence (sans type) | auteur |
| `OUTBOX` | `outbox_event` | agrégat | |
| `NOTIFICATION` | `notification_message` | agrégat | |
| `WEBHOOK` | `webhook_delivery` | agrégat | |
| `PAWAPAY` | `pawapay_callback` | référence | |
| `JOB` | `scheduled_task_run` | | |

- **Trois vues par les filtres** : `user` (courriel ou code du compte, sans casse), `entityCode` avec `entityType` facultatif (ce qui est arrivé à un élément ; `BillingDocument` et `BILLING_DOCUMENT` se valent), `requestId`. Une source qui n'a pas la colonne filtrée est écartée de la lecture. Filtres communs : `from`, `to` (7 derniers jours par défaut, 366 jours au plus), `sources`.
- **Pagination par curseur** : `nextCursor` à repasser tel quel ; rien n'est perdu ni doublé quand de nouvelles lignes arrivent.
- **Export CSV** de la chronologie, limité par `bokati.audit.export.max-rows`, journalisé (`AUDIT` / `EXPORT_TIMELINE`).
- **Routes** (`TimelineController`, droit `SYSTEM:AUDIT`) : `GET /admin/audit/timeline`, `GET /admin/audit/timeline/sources`, `GET /admin/audit/timeline/export`.

## Limites

- Chaque source est lue avec ses propres index de date ; un filtre par utilisateur ou par élément sur une longue période parcourt les journaux de la période.
- Les accès aux documents et les approbations désignent leur élément par un identifiant numérique, pas par un code.

## Tests

`TimelineA5IT` (vraie base : un élément réunit son action et son courriel malgré deux noms de type, un utilisateur réunit actions, connexions, erreurs et requêtes, une requête réunit ce qui s'y est passé, curseur sans perte ni doublon, chaque source avec chaque filtre, export, sources et curseurs invalides refusés).
