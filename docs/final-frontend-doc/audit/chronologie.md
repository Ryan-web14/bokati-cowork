# Chronologie commune (lot A5)

Routes : [socle/routes/TimelineController.md](../socle/routes/TimelineController.md).

`GET /sni/api/v1/admin/audit/timeline` : tous les journaux sous une seule forme, du plus récent au plus ancien.

## Paramètres

| Paramètre | Effet |
| --- | --- |
| `user` | courriel ou code de compte (`USR-...`), sans casse : **ce qu'a fait un utilisateur** |
| `entityCode` | code d'un élément (`INV-...`, `BKG-...`, `SUB-...`, `MBR-...`), sans casse : **ce qui est arrivé à un élément** |
| `entityType` | facultatif avec `entityCode` ; `BillingDocument`, `billing_document` et `BILLING_DOCUMENT` se valent |
| `requestId` | **ce qui s'est passé dans une requête** |
| `sources` | sources à lire, séparées par des virgules (liste : `GET .../timeline/sources`) ; toutes par défaut |
| `from`, `to` | date-heure ISO ; 7 derniers jours par défaut, 366 jours au plus |
| `limit` | 50 par défaut, 200 au plus |
| `cursor` | `nextCursor` de la page précédente, tel quel |

Réponse : `{ "items": [ ... ], "nextCursor": "..." }` ; `nextCursor` vaut `null` à la dernière page. Pagination « Voir plus » : rappeler avec `cursor`, sans changer les autres paramètres. Rien n'est perdu ni doublé quand de nouvelles lignes arrivent.

Une ligne : `occurredAt`, `source`, `refId` (identifiant dans sa source), `kind` (nature), `actor`, `entityType`, `entityCode`, `summary`, `requestId`, `ipAddress`, `status`.

## Sources

| `source` | Contenu | `kind` | `status` |
| --- | --- | --- | --- |
| `ACTION` | journal des actions | `MODULE:ACTION` | `SUCCESS`, `FAILURE` |
| `SECURITY` | connexions, refus, verrouillages, sessions | type d'événement (`LOGIN_FAILURE`...) | |
| `ERROR` | erreurs serveur | exception | statut HTTP |
| `REQUEST` | requêtes lentes ou en erreur | `SLOW`, `ERROR` | statut HTTP |
| `FISCAL` | journal fiscal des factures | action | |
| `CONTRACT` | journal des contrats | événement | |
| `COMPLIANCE` | journal de conformité | action | |
| `WALLET` | actions d'administration des portefeuilles | type d'action | statut |
| `DOCUMENT_ACCESS` | accès aux documents | action | |
| `APPROVAL` | décisions d'approbation | événement | |
| `SETTING` | changements de paramètres | `SETTING_CHANGED` | |
| `EMAIL` | courriels envoyés | statut | statut |
| `WHATSAPP` | messages WhatsApp | objet | groupe de statut |
| `OUTBOX` | boîte d'envoi interne | type d'événement | statut |
| `NOTIFICATION` | notifications | type d'événement | statut |
| `WEBHOOK` | webhooks sortants | type d'événement | statut |
| `PAWAPAY` | rappels PawaPay | type | résultat |
| `JOB` | exécutions de tâches gardées | nom de la tâche | `SUCCESS`, `FAILURE` |

Une source qui n'a pas la colonne filtrée est absente du résultat (par exemple, `requestId` ne lit que les actions, la sécurité, les erreurs, les requêtes et la boîte d'envoi ; `user` ne lit pas les courriels ni la boîte d'envoi).

## Écran conseillé

Liste verticale datée, une pastille de couleur par source, filtres en haut (utilisateur, élément, requête, sources, période). Chaque `actor`, `entityCode` et `requestId` est un lien qui relance la chronologie sur cette valeur.

## Export

`GET /sni/api/v1/admin/audit/timeline/export`, mêmes paramètres (sans `cursor` ni `limit`) : CSV, mêmes règles que l'export du journal des actions ([journal-actions.md](journal-actions.md#export)).
