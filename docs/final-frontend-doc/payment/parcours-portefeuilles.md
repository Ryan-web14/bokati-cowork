# payment : parcours des portefeuilles (comptes, retenues, contrôle, conformité, sécurité)

> Rédigé d'après le code. Point d'entrée du module : [README.md](README.md). Routes exactes : [routes.md](routes.md). Règles communes : [README racine](../README.md).

Ce fichier couvre les routes du back-office sur les portefeuilles : `WalletController`, `WalletHoldController`, `WalletControlCentreController`, `ComplianceController`, `WalletIntegrityController`, `WalletSecurityAdminController`. Le titulaire gère son propre portefeuille (rechargement, transferts, code secret, appareils) par les routes `/sni/api/v1/client/wallet/**` du module `portal`, non décrites ici.

## Notions

| Objet | Identifiant exposé | Rôle |
| --- | --- | --- |
| Portefeuille (`WalletAccount`) | `walletNumber` | Un compte par titulaire (`ownerType` + `ownerCode`) **et par devise** (unicité en base). Trois soldes : `ledgerBalance` (comptable), `heldBalance` (retenu), `availableBalance` (utilisable). |
| Écriture (`WalletLedgerEntry`) | numéro d'écriture (`entryNumber`) et `transactionNumber` | Chaque mouvement, jamais modifié. Type dans `WalletEntryType` (`ADMIN_TOPUP`, `TOPUP`, `PAYMENT`, `REFUND`, `REVERSAL`, `HOLD`, `HOLD_RELEASE`, `OVERPAYMENT_CREDIT`, `TRANSFER_IN`/`OUT`/`FEE`, `ADMIN_DEBIT`, `ADJUSTMENT`, `CASHBACK`, `PROMOTIONAL_CREDIT`). Chaînées entre elles (vérification d'intégrité). |
| Retenue (`WalletHold`) | `holdNumber` | Fonds bloqués pour une opération à venir (une réservation par exemple), puis prélevés (`capture`) ou rendus (`release`). |
| Action administrative | `actionNumber` | Geste du back-office sur un portefeuille (crédit, débit, ajustement, contre-passation, suspension, clôture…), avec double visa au-delà d'un seuil. |
| Signalement de risque | `flagNumber` | Levé par une règle de surveillance ou par le système (code secret forcé, appareil révoqué…). |
| Dossier de conformité | `caseNumber` | Regroupe des signalements, se confie, s'escalade, se clôt par une décision motivée. |
| Gel sur instruction | `freezeNumber` | Gel ordonné par une autorité, avec la référence de l'instruction. |
| Contrôle de filtrage | `checkNumber` | Comparaison d'une personne aux listes de sanctions et de personnes politiquement exposées. |
| Règle de surveillance | `ruleCode` | Seuils et action (`FLAG`, `REQUIRE_REVIEW`, `BLOCK_OPERATION`, `FREEZE_WALLET`). |
| Rapprochement de trésorerie | `reconciliationNumber` | Comparaison de l'encours des portefeuilles avec la trésorerie déclarée. |

## Statuts et cycle de vie

### Portefeuille (`WalletStatus`)

Seul `ACTIVE` permet un débit (paiement, transfert, retenue capturée) : sinon `400` « Wallet is not active » (`src/main/java/com/sni/bokaticowork/features/payment/service/implementation/WalletServiceImpl.java:136`).

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `ACTIVE` | Utilisable. | Création (`get-or-create`, premier paiement, trop-perçu) | `SUSPENDED`, `UNDER_REVIEW`, `LOCKED`, `CLOSED` |
| `SUSPENDED` | Suspendu par un administrateur. | Action `SUSPEND` | `ACTIVE` par action `UNSUSPEND` |
| `UNDER_REVIEW` | En examen. | Revue d'un signalement avec `holdWallet: true` ; gel de protection par une règle `FREEZE_WALLET` | `ACTIVE` quand le dernier signalement ouvert est écarté, quand un dossier est clos, ou par `UNSUSPEND` |
| `LOCKED` | Gelé sur instruction d'une autorité. | `POST .../compliance/wallets/{walletNumber}/freeze` | `ACTIVE` par la levée du gel uniquement (`UNSUSPEND` refusé en `409`) |
| `CLOSED` | Clôturé, solde restitué. | Action `CLOSE` | aucun |

`dormantSince` (marqué par le worker après 180 jours sans mouvement) n'est pas un statut : il ne bloque rien.

### Retenue (`WalletHoldStatus`)

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `ACTIVE` | Fonds bloqués (`heldBalance` augmenté). | `POST /sni/api/v1/wallet-holds` | `CAPTURED`, `RELEASED`, `EXPIRED` |
| `CAPTURED` | Fonds prélevés. | `PATCH .../capture` | aucun |
| `RELEASED` | Fonds rendus. | `PATCH .../release` | aucun |
| `EXPIRED` | Rendus automatiquement à `expiresAt`. | `WalletHoldExpiryWorker`, toutes les 5 min | aucun |

### Action administrative

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `PENDING_APPROVAL` | Attend le second visa ; rien n'est exécuté. | Demande d'une contre-passation, d'une clôture avec solde positif, ou d'un crédit, débit ou ajustement dont la valeur absolue atteint `bokati.wallet.admin.four-eyes-threshold` (100 000 par défaut) | `EXECUTED`, `REJECTED` |
| `EXECUTED` | Exécutée ; `resultReference` porte l'écriture produite. | Demande sous le seuil, ou approbation | aucun |
| `REJECTED` | Refusée avec motif. | Refus | aucun |

### Signalement (`WalletRiskFlag.Status`)

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `OPEN` | À examiner. | Règle, détection système | `UNDER_REVIEW`, `CONFIRMED`, `DISMISSED` |
| `UNDER_REVIEW` | Pris en revue. | `POST .../flags/{flagNumber}/review`, rattachement à un dossier | `CONFIRMED`, `DISMISSED` |
| `CONFIRMED` / `DISMISSED` | Conclu, `resolution` motivée. | `POST .../flags/{flagNumber}/resolve`, clôture du dossier | aucun |

### Dossier de conformité (`ComplianceCase.Status`)

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `OPEN` | Ouvert. | Règle `REQUIRE_REVIEW`, `BLOCK_OPERATION` ou `FREEZE_WALLET`, revue d'identité échue | `IN_REVIEW`, `ESCALATED`, clôture |
| `IN_REVIEW` | Confié. | `assign` | `ESCALATED`, clôture |
| `ESCALATED` | Escaladé à quelqu'un, avec motif. | `escalate` | clôture |
| `CLOSED_CLEARED` / `CLOSED_CONFIRMED` / `CLOSED_REPORTED` | Clos par la décision `CLEARED`, `CONFIRMED` ou `REPORTED`. Un dossier clos ne se rouvre pas. | `close` | aucun |

### Gel sur instruction

Actif tant que `liftedAt` est nul ; un seul gel actif par portefeuille. Levée par `POST .../freezes/{freezeNumber}/lift`.

### Rapprochement de trésorerie

`BALANCED` (écart dans la tolérance et couverture suffisante), `VARIANCE` (écart expliqué), `UNDER_REVIEW` (couverture sous `bokati.wallet.treasury.coverage-alert-ratio`, 1,0 par défaut, quelle que soit l'explication, ou écart non expliqué). Calcul : `src/main/java/com/sni/bokaticowork/features/payment/control/service/WalletTreasuryReconciliationService.java:149`.

## Parcours

### 1. Retrouver, ouvrir et consulter un portefeuille

1. Liste : `GET /sni/api/v1/wallets?ownerType=&ownerCode=` ([route](routes/WalletController.md#r-get-sni-api-v1-wallets)). `ownerType` est comparé tel quel (pas de traduction `MEMBRE` → `MEMBER` ici).
2. Fiche : `GET /sni/api/v1/wallets/{walletNumber}` ; historique : `GET /sni/api/v1/wallets/{walletNumber}/ledger` (plus récentes d'abord, `sort` ignoré).
3. Ouvrir sans créditer : `POST /sni/api/v1/wallets/admin/get-or-create` avec `{ ownerType, ownerCode, currency }` : renvoie le portefeuille existant pour ce titulaire et cette devise, ou le crée `ACTIVE` à zéro.
4. Plafonds et consommation du jour : `GET /sni/api/v1/wallets/{walletNumber}/limits` (`LimitSnapshot` : politique, niveau KYC, plafonds, consommé). Rôle `ROLE_ADMIN` ou `ROLE_SUPER_ADMIN` requis.
5. Intégrité : `GET /sni/api/v1/wallets/{walletNumber}/integrity` (`IntegrityReport` : écritures vérifiées, maillons rompus `brokenLinks`, solde attendu et réel). Rôle `ROLE_ADMIN` ou `ROLE_SUPER_ADMIN` requis.

### 2. Créditer ou débiter un portefeuille

Deux chemins coexistent :

- **Chemin contrôlé (à privilégier)** : `POST /sni/api/v1/wallets/control/wallets/{walletNumber}/actions` ([route](routes/WalletControlCentreController.md#r-post-sni-api-v1-wallets-control-wallets-walletnumber-actions)) avec `{ "type": "TOPUP" | "DEBIT" | "ADJUST", "amount": ..., "reason": "...", "reference": "..." }`. Motif d'au moins 5 caractères. `ADJUST` porte un montant signé non nul. Sous le seuil de double visa, exécution immédiate (`status` = `EXECUTED`) ; au-delà, `PENDING_APPROVAL` (parcours 4). L'auteur est lu dans le jeton, jamais dans le corps.
- **Recharge directe** : `POST /sni/api/v1/wallets/admin/top-up` ([route](routes/WalletController.md#r-post-sni-api-v1-wallets-admin-top-up)) avec `{ ownerType, ownerCode, currency, amount, createdBy, reference? }`. Crée le portefeuille au besoin et crédite `ADMIN_TOPUP` sans seuil ni second visa (voir Écarts). Une `reference` rend la recharge idempotente (`ADMIN_TOPUP:<reference>`) ; sans elle, deux envois créditent deux fois.

### 3. Bloquer puis prélever ou rendre des fonds (retenues)

1. `POST /sni/api/v1/wallet-holds` ([route](routes/WalletHoldController.md#r-post-sni-api-v1-wallet-holds)) avec `{ walletNumber, amount, sourceType?, sourceCode?, expiresAt?, createdBy? }`. Renvoie la retenue `ACTIVE` ; `availableBalance` baisse, `heldBalance` monte. `400` « Insufficient wallet balance » si le disponible ne suffit pas.
2. `PATCH /sni/api/v1/wallet-holds/{holdNumber}/capture?createdBy=` prélève ; `PATCH .../release?createdBy=` rend. Sur une retenue qui n'est plus `ACTIVE` : `400` « Wallet hold is not active ».
3. Liste : `GET /sni/api/v1/wallet-holds?walletNumber=&status=&sourceType=&sourceCode=`. `status` invalide : `400` « Invalid wallet hold status ».

### 4. Double visa des actions administratives

1. File : `GET /sni/api/v1/wallets/control/actions/pending` ; historique d'un portefeuille : `GET /sni/api/v1/wallets/control/wallets/{walletNumber}/actions`.
2. Approuver : `POST /sni/api/v1/wallets/control/actions/{actionNumber}/approve` (sans corps). **Le second visa doit venir d'une autre personne que le demandeur** : sinon `409` « le second visa doit venir d'une autre personne que le demandeur ». Masquer le bouton quand `requestedBy` est l'utilisateur connecté.
3. Refuser : `POST .../reject` avec `{ "reason": "..." }`.
4. Actions disponibles et contraintes (`src/main/java/com/sni/bokaticowork/features/payment/control/service/WalletAdminActionService.java:306`) :
   - `REVERSE` : `reference` = numéro de transaction de l'écriture à contre-passer ; toujours en double visa ; une retenue ou une contre-passation ne se contre-passe pas ; une écriture ne se contre-passe qu'une fois.
   - `SUSPEND` / `UNSUSPEND` : immédiat ; `UNSUSPEND` refusé (`409`) sur un gel sur instruction.
   - `RESET_PIN` : immédiat, même effet que le parcours 8.
   - `RAISE_LIMIT` : `reference` = code d'une politique de plafond existante, `until` (fin de la dérogation) obligatoire.
   - `CLOSE` : refusé tant qu'il reste une retenue ; avec un solde positif, `reference` (référence de restitution) est obligatoire et l'action passe en double visa.
   - Toute action sur un portefeuille clôturé : `409` « ce portefeuille est clôturé ».

### 5. Traiter les signalements de risque

1. File : `GET /sni/api/v1/wallets/control/flags?status=OPEN&status=UNDER_REVIEW` (paramètre répétable) ; par portefeuille : `GET /sni/api/v1/wallets/control/wallets/{walletNumber}/flags`.
2. Prendre en revue : `POST /sni/api/v1/wallets/control/flags/{flagNumber}/review` avec `{ "holdWallet": true }` pour passer le portefeuille `UNDER_REVIEW` pendant l'examen (corps facultatif, défaut `false`).
3. Conclure : `POST .../flags/{flagNumber}/resolve` avec `{ "confirmed": true|false, "resolution": "..." }` (au moins 5 caractères). Écarter le dernier signalement ouvert remet le portefeuille `ACTIVE` s'il était `UNDER_REVIEW`. Confirmer ne change pas le portefeuille : la suite est une action administrative.
4. Signalement déjà conclu : `400` « Ce signalement est déjà traité ».

### 6. Dossiers de conformité

1. Tableau de bord : `GET /sni/api/v1/wallets/compliance/dashboard`.
2. Liste : `GET /sni/api/v1/wallets/compliance/cases?scope=open|all|mine` (défaut `open` ; `mine` = confiés à l'utilisateur connecté). Fiche, notes : `GET .../cases/{caseNumber}`, `.../notes`. Dossiers d'un portefeuille : `GET .../wallets/{walletNumber}/cases`.
3. Actions : `POST .../cases/{caseNumber}/assign` `{ "assignee" }` (passe `IN_REVIEW`) ; `.../notes` `{ "note" }` ; `.../escalate` `{ "escalatedTo", "reason" }` ; `.../close` `{ "decision": "CLEARED" | "CONFIRMED" | "REPORTED", "rationale" }` (au moins 10 caractères). La clôture conclut les signalements rattachés (`CLEARED` → `DISMISSED`, sinon `CONFIRMED`) et peut remettre le portefeuille `ACTIVE` s'il était `UNDER_REVIEW`. Toute action sur un dossier clos : `400` « Ce dossier est clos · un dossier clos ne se rouvre pas, on en ouvre un autre ».
4. Export : `GET .../cases/{caseNumber}/export?activityDays=90` télécharge un fichier JSON `dossier-<caseNumber>.json` ; l'export est tracé dans la piste d'audit.
5. Évaluer un portefeuille à la demande : `POST .../wallets/{walletNumber}/evaluate` renvoie `[{ ruleCode, action, details, newFlag }]`.

### 7. Gel sur instruction, filtrage, règles, audit

1. Geler : `POST /sni/api/v1/wallets/compliance/wallets/{walletNumber}/freeze` avec `{ authority, instructionReference, instructionDate?, rationale }` : portefeuille `LOCKED`. Un second gel actif : `409`. Lever : `POST .../freezes/{freezeNumber}/lift` avec `{ liftReference, liftRationale }`. Listes : `GET .../freezes` (actifs), `GET .../wallets/{walletNumber}/freezes` (historique).
2. Filtrage : charger une liste `POST .../screening/lists` avec `{ listCode, listVersion, type: "SANCTION"|"PEP", entries: [{ fullName, aliases?, birthYear?, nationality?, reference? }] }` (l'ancienne version de la même liste est désactivée) ; contrôler une personne `POST .../screening/checks` avec `{ subjectType, subjectCode, fullName, birthYear? }` → `result` `CLEAR`, `POSSIBLE_MATCH` ou `MATCH` ; revoir `POST .../screening/checks/{checkNumber}/review` avec `{ trueMatch, rationale }` (10 caractères au moins ; un contrôle `CLEAR` n'a rien à revoir). Listes : `GET .../screening/checks?scope=pending|all`, `GET .../screening/subjects/{subjectType}/{subjectCode}`.
3. Règles : `GET .../rules` ; `PUT .../rules/{ruleCode}` avec les champs à changer (`severity`, `action`, seuils, `active`, `effectiveFrom`/`To`…) et `rationale` obligatoire.
4. Piste d'audit : `GET .../audit/{subjectType}/{subjectCode}` ; vérification de la chaîne : `GET .../audit/verify`.

### 8. Code secret et tableau de bord du contrôle

1. Réinitialiser le code secret : `POST /sni/api/v1/wallets/{walletNumber}/security/pin/reset` (sans corps, réponse vide). Le titulaire devra poser un nouveau code à sa prochaine opération ; le blocage après échecs est levé. Aucun code ne peut être lu ni posé par le back-office. `400` « Aucun code secret n'est défini sur ce portefeuille » si le titulaire n'en a jamais posé.
2. Tableau de bord : `GET /sni/api/v1/wallets/control/dashboard?currency=` (encours, retenues, comptes par statut, gelés, dormants, soldes négatifs, volume du jour, signalements ouverts, actions en attente).
3. Export comptable : `GET /sni/api/v1/wallets/control/export?from=AAAA-MM-JJ&to=AAAA-MM-JJ` télécharge un CSV `portefeuille-<from>-<to>.csv`.
4. Trésorerie : `GET .../treasury/snapshot?currency=XAF` (encours) ; `POST .../treasury/reconciliations` avec `{ date, currency?, ledgerAccountBalance?, availableCash, explanation? }` ; historique `GET .../treasury/reconciliations`, fiche `GET .../treasury/reconciliations/{number}`.

## Règles et erreurs à refléter dans l'écran

- Montant d'écriture nul ou négatif : `400` « Wallet amount must be positive » ; solde disponible insuffisant : `400` « Insufficient wallet balance » ; retenue insuffisante : `400` « Insufficient held balance on wallet ».
- Motifs : action administrative 5 caractères, conclusion de signalement 5, clôture de dossier 10, revue de filtrage 10. Messages français renvoyés tels quels par le serveur.
- `409` (`CONFLICT`) pour : second visa par le demandeur, levée d'un gel par `UNSUSPEND`, clôture avec retenues ou solde sans référence, portefeuille clôturé, second gel, opération refusée par une règle `BLOCK_OPERATION` (« opération refusée par la règle de surveillance « ... » »).
- `404` : « Portefeuille introuvable », « Signalement introuvable », « Contrôle introuvable », « Wallet not found », « Wallet hold not found ».

## À prévoir côté frontend

- **Droits à deux niveaux.** Le droit de l'URL (`PAYMENT:READ` / `PAYMENT:PROCESS`) ne suffit pas : `WalletControlCentreController`, `ComplianceController`, `.../integrity`, `.../limits` et `.../security/pin/reset` exigent aussi `ROLE_ADMIN` ou `ROLE_SUPER_ADMIN` (`@PreAuthorize` au niveau de la classe ou de la méthode). `WalletController` et `WalletHoldController` n'ont que le droit.
- **Écrans** : recherche et fiche de portefeuille (soldes, statut, écritures, retenues, plafonds, intégrité) ; centre de contrôle (tableau de bord, file des actions à viser, file des signalements, trésorerie, export) ; conformité (dossiers, gels, filtrage, règles, audit).
- **Fichiers téléchargés** : CSV d'export comptable, JSON d'export de dossier.
- **Identifiants numériques** : `GET .../treasury/reconciliations` et `GET .../audit/...` renvoient les entités telles quelles, avec leur `id` int64 (voir [README racine](../README.md#identifiants--attention-aux-grands-nombres)) ; utiliser `reconciliationNumber` pour naviguer.
- **Champs d'auteur** : dans le centre de contrôle et la conformité, l'auteur vient du jeton. Dans `WalletController` et `WalletHoldController`, `createdBy` est du texte libre envoyé par l'écran : y mettre l'identifiant de l'utilisateur connecté.

## Effets automatiques

| Automatisme | Fréquence | Effet |
| --- | --- | --- |
| `WalletHoldExpiryWorker` | toutes les 5 min | Retenues échues → `EXPIRED`, fonds rendus. |
| `WalletDormancyWorker` | chaque jour à 03:15 | Marque `dormantSince` après 180 jours sans mouvement ; le réveil d'un dormant lève un signalement `DORMANT_REACTIVATION` (d'après le commentaire, non vérifié). |
| `WalletReconciliationWorker` | chaque jour à 03:15 | Vérifie que les soldes correspondent aux écritures ; ne corrige rien, journalise les écarts. |
| `ComplianceWorker` | 03:40 filtrage périodique, 03:50 revue d'identité, 08:00 dossiers hors délai | Contrôles sur les listes ; signalement `IDENTITY_REVIEW_DUE` et dossier de priorité basse quand l'échéance de revue est passée ; journal des dossiers hors délai (aucune notification). |
| `WalletConfirmationExpiryWorker`, `WalletUsageExpiryWorker` | 5 min, 10 min | Expiration des confirmations et usages du portail (module `portal`, non détaillé ici). |
| Moteur de règles | après chaque opération du titulaire | Signalement, dossier, refus (`BLOCK_OPERATION`) ou gel de protection (`FREEZE_WALLET` → `UNDER_REVIEW`, dossier critique). |
| Notifications au titulaire | suspension, levée, clôture, gel | `WALLET_SUSPENDED`, `WALLET_UNSUSPENDED`, `WALLET_CLOSED`, `WALLET_FROZEN` (`WalletNotifier`). |
| Alerte de trésorerie | rapprochement sous le ratio | Événement `WALLET_TREASURY_COVERAGE_ALERT` vers `bokati.wallet.treasury.alert-email`. |

## Écarts relevés

- `POST /sni/api/v1/wallets/admin/top-up` crédite sans seuil ni second visa, avec un auteur libre (`createdBy`), alors que le centre de contrôle impose le double visa au-delà de 100 000 pour le même geste. `src/main/java/com/sni/bokaticowork/features/payment/service/implementation/WalletServiceImpl.java:33` et `src/main/java/com/sni/bokaticowork/features/payment/controller/WalletController.java`.
- Le grand livre ne vérifie pas le statut : une recharge directe, un remboursement ou un trop-perçu créditent un portefeuille `LOCKED` ou `CLOSED`, et une retenue peut être posée sur un portefeuille non actif (`WalletHoldServiceImpl.create` ne contrôle pas le statut). `src/main/java/com/sni/bokaticowork/features/payment/service/support/WalletLedgerService.java:122`, `src/main/java/com/sni/bokaticowork/features/payment/service/implementation/WalletHoldServiceImpl.java:27`.
- `GET /sni/api/v1/wallets/compliance/screening/checks?scope=pending` renvoie aussi les contrôles déjà revus (filtre sur le résultat seulement, pas sur `reviewedAt`). `src/main/java/com/sni/bokaticowork/features/payment/compliance/service/ScreeningService.java:170`.
- Entités JPA renvoyées directement (identifiants int64 et champs internes exposés) : `WalletTreasuryReconciliation` et `ComplianceAuditEntry`. `src/main/java/com/sni/bokaticowork/features/payment/control/controller/WalletControlCentreController.java` (routes `treasury/reconciliations`), `compliance/controller/ComplianceController.java:232`.
- Le `ComplianceWorker` des dossiers hors délai ne fait que journaliser : personne n'est prévenu. `compliance/service/ComplianceWorker.java:128`.
- Le code secret se réinitialise par deux voies (route directe et action `RESET_PIN`), dont une seule laisse une trace d'action administrative. `security/controller/WalletSecurityAdminController.java` et `WalletAdminActionService.java:230`.
