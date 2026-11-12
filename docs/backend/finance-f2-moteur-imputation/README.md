# Lot F2 : moteur d'imputation

Lot du [plan finance](../plan-finance-comptabilite/README.md), sections 3.2 (chaîne d'une écriture automatique), 3.3 (activation progressive) et 4.5 (schémas d'écriture). Les valeurs initiales des schémas sont dans [catalogue-ecritures.md](../plan-finance-comptabilite/catalogue-ecritures.md) ; elles sont chargées par les lots qui branchent les faits métier (F3 caisse, F4 ventes et encaissements, etc.). Lot précédent : [F1, socle comptable](../finance-f1-socle-comptable/README.md).

Branche `feature/finance-comptabilite`. Migrations : dev `V297__comptabilite_moteur_imputation_f2.sql`, prod `V291__comptabilite_moteur_imputation_f2.sql` (contenu identique). Code : `features/accounting` (moteur, schémas) et `core/accounting/AccountingEventPublisher` (publication par les modules métier).

## Chaîne

```
Fait métier (dans sa transaction)
  AccountingEventPublisher.publish(type, famille, référence, date, données)
    -> outbox_event ACCOUNTING_EVENT (worker outbox, 10 s)
      -> AccountingOutboxEventProcessor : enregistrement dans acc_posting_event (idempotence)
        -> PostingWorker (30 s, si accounting.enabled) : PostingEngine.process
           1. date antérieure à accounting.start-date (hors reprise) : IGNORED
           2. schéma actif en vigueur à la date, conditions remplies (priorité)
           3. variables résolues (comptes, tiers, journal), lignes calculées
           4. première période ouverte à partir de la date du fait (mention si report)
           5. ordre chronologique du journal : un fait en retard prend la dernière date du journal (mention)
           6. pièce validée, ou brouillard (journal sans validation automatique, ou reprise)
```

- Les modules métier ne dépendent que de `AccountingEventPublisher`, jamais du module comptable. L'outbox ne fait qu'enregistrer l'événement : un problème comptable ne fait jamais rejouer l'outbox ni échouer un paiement.
- **Idempotence** : un couple (type, référence) n'existe qu'une fois dans `acc_posting_event` (contrainte unique). Republier ou retraiter un événement ne produit pas de seconde pièce.
- Comptabilité inactive (`accounting.enabled = false`) : les événements s'accumulent en `PENDING` et seront comptabilisés à l'activation, dans l'ordre des dates.
- Interrupteur par famille : `accounting.posting.disabled-families` (par exemple `SALES,WALLET`) laisse ces familles en file.

### États d'un événement

| État | Sens | Actions |
| --- | --- | --- |
| `PENDING` | En file | Le moteur le traitera |
| `POSTED` | Pièce produite (validée, ou brouillard) | Une pièce fausse se contre-passe |
| `UNBALANCED` | Pièce produite mais en attente d'équilibrage (déséquilibre, compte absent ou inactif, tiers manquant, contrepartie non autorisée), selon la politique du lot F1 | Corriger le paramétrage (compte, schéma), puis **Régénérer** ; ou **Écarter** |
| `FAILED` | Échec technique : aucun schéma actif, variable non résolue, données illisibles, journal absent, période introuvable | Corriger, puis **Régénérer** ; activer un schéma remet en file les échecs de son type |
| `IGNORED` | Aucune écriture : schéma « sans écriture », fait antérieur à la bascule, ou écarté par un comptable (motif) | Aucune |

**Régénérer** rejette la pièce en attente (suivi `REGENERATED`) et en calcule une nouvelle avec les schémas et le plan du moment. Un événement `POSTED` ne se régénère pas.

## Schémas d'écriture (`acc_posting_rule`)

| Champ | Rôle |
| --- | --- |
| `code`, `version` | Identifiant ; enregistrer un schéma actif crée la version suivante en brouillon |
| `eventType`, `family` | Type d'événement et famille |
| `priority` | Plusieurs schémas pour un type : le premier dont les conditions sont remplies (plus petite priorité) |
| `conditions` | Objet JSON : `{"method": "CASH"}`, `{"method": ["CASH", "CHEQUE"]}`, `"!CANCELLED"` exclut, `"*"` exige une valeur, `""` exige l'absence |
| `journal` | Code ou variable (`{journalCode}`, `{REGISTER_JOURNAL:registerCode}`) |
| `entryLabel` | Libellé de la pièce, avec variables (`Facture {documentNumber}`) |
| `lines` | Tableau JSON des lignes, voir plus bas |
| `noEntry` | Le fait ne produit pas d'écriture : l'événement est écarté (`IGNORED`) |
| `samplePayload` | Événement d'exemple, utilisé par l'aperçu et l'activation |
| `effectiveFrom` | Date d'effet : par code, la version active la plus récente en vigueur à la date du fait s'applique |
| `status` | `DRAFT`, `ACTIVE`, `RETIRED` |

### Ligne de schéma

```json
{"side": "DEBIT", "account": "{CLIENT_ACCOUNT:customerCode}", "amount": "totalTtc",
 "thirdParty": "{customerCode}", "label": "{designation}", "reference": "{documentNumber}",
 "foreach": "lines", "when": {"kind": "GOODS"}, "signed": true,
 "dueDate": "dueDate", "taxCode": "{taxCode}", "taxBase": "ht", "currency": "{currency}", "foreignAmount": "foreignAmount"}
```

- `side` : `DEBIT` ou `CREDIT`.
- `amount` : chemin, nombre, ou somme (`totalTtc - discount`, `a + b`). Une ligne à montant nul est omise. Avec `signed`, un montant négatif passe de l'autre côté ; sans, il est refusé.
- `foreach` : la ligne est répétée pour chaque élément du tableau ; les chemins cherchent d'abord dans l'élément, puis dans l'événement (`event.x` force l'événement).
- `when` : conditions propres à la ligne (même syntaxe).
- Variables : `{chemin}` (minuscule) lit l'événement ; `{NOM}` ou `{NOM:argument}` (majuscules) appelle un résolveur. L'argument est un chemin de l'événement, ou à défaut sa valeur littérale (`{SETTING:accounting.unbalanced-entry.suspense-account}`).

### Résolveurs

`GET /accounting/posting/resolvers` les liste. Lot F2 : `JOURNAL_ACCOUNT` (compte de trésorerie d'un journal), `SETTING` (réglage de la plateforme), `THIRD_PARTY_ACCOUNT` (compte collectif d'un tiers déjà connu). Les lots suivants ajoutent les leurs (compte du client, journal de la caisse, compte de la nature d'opération...).

### Activation

Un schéma ne s'active que si, sur son exemple (ou à défaut le dernier événement reçu de ce type) : ses conditions sont remplies, la pièce a au moins deux lignes et est **équilibrée**, le journal et tous les comptes existent, toutes les variables se résolvent. Activer retire les versions actives plus anciennes du même code en vigueur à la même date ou après, et remet en file les événements `FAILED` de ce type.

## Reprise de l'historique

Décision de l'utilisateur : bascule avec rejeu depuis le 1er janvier 2026. Un événement marqué `replay` produit toujours un **brouillard**, même si le journal valide automatiquement. Après contrôle, `POST /accounting/posting/drafts/validate?journalCode=&to=` valide en bloc, dans l'ordre des dates, chaque pièce dans sa transaction ; la réponse liste les échecs. Les producteurs de reprise (lecture des transactions, factures, mouvements passés) sont livrés avec les lots qui branchent chaque famille.

## Routes

Préfixe `/api/v1/accounting/posting`. Écritures auditées (module `ACCOUNTING`).

| Route | Rôle | Droit |
| --- | --- | --- |
| `GET /rules?eventType=` | Dernière version de chaque schéma | `ACCOUNTING_POSTING:READ` |
| `GET /rules/{code}/versions` | Historique des versions | `ACCOUNTING_POSTING:READ` |
| `POST /rules` | Créer, ou nouvelle version (le brouillon en cours est mis à jour) | `ACCOUNTING_POSTING:MANAGE` |
| `PATCH /rules/{id}/activate`, `/retire` | Activer (contrôlé), retirer | `ACCOUNTING_POSTING:MANAGE` |
| `POST /preview` | Aperçu : `ruleId` ou `rule` (en cours d'édition) sur `eventId` ou `payload` (à défaut l'exemple) | `ACCOUNTING_POSTING:READ` |
| `GET /resolvers` | Résolveurs disponibles | `ACCOUNTING_POSTING:READ` |
| `GET /events?status=&eventType=&from=&to=&limit=` | Événements (écran des écritures en échec : `status=FAILED` puis `UNBALANCED`) | `ACCOUNTING_POSTING:READ` |
| `GET /events/summary` | Compteurs par état, comptabilité active, date de bascule, familles coupées | `ACCOUNTING_POSTING:READ` |
| `GET /events/{id}` | Détail avec les données reçues | `ACCOUNTING_POSTING:READ` |
| `PATCH /events/{id}/regenerate` | Régénérer | `ACCOUNTING_POSTING:REPLAY` |
| `PATCH /events/{id}/ignore` | Écarter (motif) | `ACCOUNTING_POSTING:REPLAY` |
| `POST /events` | Soumettre un événement à la main (test de bout en bout, reprise ponctuelle) | `ACCOUNTING_POSTING:REPLAY` |
| `POST /run` | Traiter la file tout de suite | `ACCOUNTING_POSTING:REPLAY` |
| `POST /drafts/validate` | Valider en bloc les brouillards issus des événements | `ACCOUNTING_POSTING:REPLAY` |

Rôles : `SUPER_ADMIN` et `ADMIN` ont les trois droits ; `FINANCE` lit, régénère, écarte et rejoue, mais ne modifie pas les schémas.

## Paramètres (domaine Comptabilité)

| Clé | Défaut | Effet |
| --- | --- | --- |
| `accounting.start-date` | `2026-01-01` | À chaud |
| `accounting.posting.disabled-families` | vide | À chaud |
| `accounting.posting.batch-size` | `50` | À chaud |
| `accounting.posting.worker-delay-ms` | `30000` | Redémarrage |

## Pour le frontend

- **Écran des écritures automatiques** : bandeau avec `GET /events/summary` (comptabilité active ou non, date de bascule, familles coupées, compteurs). Onglets par état ; `FAILED` et `UNBALANCED` d'abord, avec le motif (`failureReason`), le schéma appliqué, la pièce produite (lien vers `/accounting/entries/{provisionalNumber}`), la mention de report (`shiftedNote`). Actions : Régénérer, Écarter (motif), voir les données reçues (`payload`).
- **Éditeur de schémas** : liste par famille et type ; fiche avec conditions, journal, libellé, grille des lignes (sens, compte ou variable, montant, tiers, référence, répétition, condition, signé) ; liste des résolveurs pour l'aide à la saisie ; zone d'exemple JSON. Bouton **Aperçu** qui appelle `POST /preview` avec le schéma en cours d'édition : lignes calculées, comptes inconnus signalés (`accountExists`), totaux, équilibre, avertissements, erreur. Bouton **Activer**, dont le refus donne la liste des problèmes. Historique des versions.
- **Reprise** : liste des brouillards issus des événements (`GET /accounting/entries?status=DRAFT`), filtre par journal, bouton « Valider jusqu'au… » (`POST /drafts/validate`), compte rendu des échecs.

## Tests

- `PostingEngineF2IT` (schéma de production, hors transaction de test) : idempotence, variable non résolue en `FAILED` sans exception, absence de schéma puis remise en file à l'activation, schéma déséquilibré refusé, compte absent mis en attente puis régénéré après création du compte (`REGENERATED`), nouvelle version qui remplace l'ancienne, schéma sans écriture, reprise en brouillard puis validation en bloc, aperçu d'un schéma non enregistré.
- `PostingTemplateTest` : facture avec lignes répétées, réduction et TVA ; montants signés et soustraction ; refus d'un négatif non signé ; conditions ; variable introuvable.
- `AdminApiAuthorizationManagerTest.shouldGuardTheAccountingRoutes`.
