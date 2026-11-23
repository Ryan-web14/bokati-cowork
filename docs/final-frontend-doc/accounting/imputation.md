# Moteur d'imputation : schémas, file des faits, reprise

Contrat exact de chaque route (paramètres, corps, réponse, champs JSON) : [routes.md](routes.md). Ce fichier explique le fonctionnement et ce qu'il faut à l'écran. Index du module : [README.md](README.md).

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
           5. ordre chronologique du journal, dans le mois seulement (lot K1) : un fait en retard garde sa date
              si sa période est ouverte ; dans son mois, il prend au plus tôt la dernière date du journal (mention)
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
| `POSTED` | Pièce produite (validée, ou brouillard) | Pièce fausse : **Recomptabiliser** (contre-passation, relecture des données, nouvelle pièce liée), ou depuis la pièce **Reclasser** / **Corriger** |
| `UNBALANCED` | Pièce produite mais en attente d'équilibrage (déséquilibre, compte absent ou inactif, tiers manquant, contrepartie non autorisée), selon la politique du lot F1 | Corriger le paramétrage (compte, schéma), puis **Régénérer** ; ou **Écarter** |
| `FAILED` | Échec technique : aucun schéma actif, variable non résolue, données illisibles, journal absent, période introuvable ; ou **lecture du fait impossible** (motif commençant par « Lecture du fait impossible : », données `{}`), par exemple taux de change manquant, paiement en espèces sans session, facture incohérente | Corriger (à la source pour une lecture impossible), puis **Régénérer** ; activer un schéma remet en file les échecs de son type |
| `IGNORED` | Aucune écriture : schéma « sans écriture », fait antérieur à la bascule, ou écarté par un comptable (motif) | Aucune |
| `REVERSED` | Lot K1 : la pièce a été contre-passée hors du moteur (à la main, ou recomptabilisation dont la contre-passation attendait une seconde validation). Motif dans `failureReason` | **Recomptabiliser** (ou **Régénérer**) pour passer la pièce juste ; **Écarter** si le fait ne doit plus rien produire |

**Régénérer** relit d'abord les données du fait à la source (facture, avoir, paiement, portefeuille, caisse, depuis le lot K0) : une correction faite dans la facturation passe dans la pièce. Puis la pièce en attente est rejetée (suivi `REGENERATED`) et une nouvelle est calculée avec les schémas et le plan du moment. Si la relecture échoue, l'événement reste `FAILED` avec le nouveau motif. Un événement `POSTED` ne se régénère pas : il se **recomptabilise**.

Depuis le lot K1 :
- **Recomptabiliser** (`PATCH /events/{id}/repost`, motif, date de contre-passation facultative) : la pièce validée est contre-passée, les données sont relues à la source, une nouvelle pièce est passée. La nouvelle pièce porte `correctionOf` (ancienne pièce) et `correctionKind = REPOST` ; l'ancienne porte `correctedBy`. Au-delà du seuil de seconde validation, la contre-passation reste en brouillard (`awaitingSecondValidation = true` dans la réponse) : une fois validée par un autre comptable, l'événement passe `REVERSED` et **Recomptabiliser** termine. Un événement d'annulation (`_REVERSE`) ne se recomptabilise pas.
- **Recomptabiliser en lot** après correction d'un schéma (`POST /events/repost`) : `{ruleCode | eventType, from, to, dryRun, reason}`. Par défaut c'est un aperçu (`dryRun` absent ou `true`) : pour chaque événement comptabilisé (200 au plus), la pièce actuelle (`before`), celle que le schéma actif produirait sur les données enregistrées (`after`) et `changed`. Avec `dryRun: false` et un motif, seuls les événements dont la pièce change sont recomptabilisés ; `result` donne le bilan. Montrer l'aperçu, puis demander confirmation.
- Contre-passer à la main une pièce automatique fait passer son événement `REVERSED` (avant : il restait `POSTED`).
- Rejeter une pièce automatique (brouillard ou en attente) fait repasser son événement `FAILED` avec « Pièce … rejetée : motif » ; un brouillard d'événement ne se supprime plus, il se rejette.

Depuis le lot K0 :
- une pièce en attente complétée et validée à la main fait passer son événement `POSTED` (numéro définitif renseigné) ;
- un fait à montant nul est `IGNORED` avec le motif « Montant nul : aucune écriture à passer » ;
- une contre-passation automatique est datée au plus tôt de la dernière pièce de son journal (mention dans `shiftedNote`) ;
- nouveau type `CREDIT_NOTE_APPLIED` (avoir imputé sur une autre facture, du même client ou de son entreprise) ;
- nouveaux schémas `SALES_DEPOSIT_CREDIT_NOTE`, `SALES_DEPOSIT_REMAINDER_CANCELLED`, `SALES_CREDIT_NOTE_APPLIED` ; les factures ont une ligne de TVA par taux.

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
- `foreach` : la ligne est répétée pour chaque élément du tableau. Le montant se lit dans l'élément seulement (lot K0 ; `event.x` pour l'événement) ; les autres chemins cherchent d'abord dans l'élément, puis dans l'événement (`event.x` force l'événement).
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
| `PATCH /events/{id}/repost` | Recomptabiliser (lot K1) : `{reason, date}` | `ACCOUNTING_POSTING:REPLAY` |
| `POST /events/repost` | Recomptabiliser en lot, aperçu par défaut (lot K1) | `ACCOUNTING_POSTING:REPLAY` |
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

- **Écran des écritures automatiques** : bandeau avec `GET /events/summary` (comptabilité active ou non, date de bascule, familles coupées, compteurs). Onglets par état ; `FAILED` et `UNBALANCED` d'abord, avec le motif (`failureReason`), le schéma appliqué, la pièce produite (lien vers `/accounting/entries/{provisionalNumber}`), la mention de report (`shiftedNote`). Actions : Régénérer, Écarter (motif), voir les données reçues (`payload`). Onglet `REVERSED` (lot K1) avec **Recomptabiliser** ; sur un événement `POSTED`, **Recomptabiliser** demande un motif et affiche le `message` de la réponse (nouvelle pièce, ou contre-passation en attente d'un autre comptable).
- **Recomptabilisation en lot** (lot K1) : depuis la fiche d'un schéma corrigé, bouton « Recomptabiliser les pièces de ce schéma » : aperçu (`POST /events/repost` sans `dryRun`) en tableau avant / après, lignes `changed` mises en avant, erreurs (`error`) ; confirmation avec motif (`dryRun: false`) ; bilan.
- **Éditeur de schémas** : liste par famille et type ; fiche avec conditions, journal, libellé, grille des lignes (sens, compte ou variable, montant, tiers, référence, répétition, condition, signé) ; liste des résolveurs pour l'aide à la saisie ; zone d'exemple JSON. Bouton **Aperçu** qui appelle `POST /preview` avec le schéma en cours d'édition : lignes calculées, comptes inconnus signalés (`accountExists`), totaux, équilibre, avertissements, erreur. Bouton **Activer**, dont le refus donne la liste des problèmes. Historique des versions.
- **Reprise** : liste des brouillards issus des événements (`GET /accounting/entries?status=DRAFT`), filtre par journal, bouton « Valider jusqu'au… » (`POST /drafts/validate`), compte rendu des échecs.

## Évolutions apportées par les lots suivants

- **Lot F3** :
  - variable facultative `{?chemin}` : vide si absente, sans mettre l'événement en échec ;
  - événement d'annulation générique : des données contenant `"_reverse": {"eventType": .., "reference": .., "reason": ..}` contre-passent la pièce de l'événement d'origine (brouillard inverse si l'origine est un brouillard de reprise) ; tant que l'origine n'est pas comptabilisée, l'événement reste en file ;
  - `PostingOutcomeListener` : les modules sources (caisse, ventes) sont prévenus du résultat dans la transaction du moteur et reportent l'état comptable sur le fait d'origine.
- **Lot F4** : registre interrogé par référence (`GET /accounting/sales/status/{référence}`) ; les faits de vente, d'encaissement et de portefeuille sont transmis par anti-jointure sur ce registre.
- **Lot F9** : une ligne de schéma accepte `"analytic": {"AXE": "{variable}"}` ; une valeur vide est ignorée.
- **Lot K1** : statut `REVERSED`, recomptabilisation (unitaire et en lot), chronologie stricte limitée au mois, contre-passation à la main et rejet d'une pièce automatique reportés sur l'événement. Reclasser et Corriger : [socle.md](socle.md#corrections).
