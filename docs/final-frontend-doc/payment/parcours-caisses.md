# payment : parcours des caisses (espèces)

> Rédigé d'après le code. Point d'entrée du module : [README.md](README.md). Routes exactes : [routes.md](routes.md).

Ce fichier couvre les caisses physiques (espèces, en XAF), leurs sessions, leurs mouvements, les demandes de caisse, les anomalies, les statistiques, et la file des espèces annoncées depuis le portail. Tout est back-office. Code : `src/main/java/com/sni/bokaticowork/features/payment/` (sous-dossiers `controller/Cash*`, `service/implementation/Cash*`, `service/support/Cash*`, `worker/`, `cash/`).

## Notions

| Objet | Identifiant exposé | Ce que c'est |
| --- | --- | --- |
| Caisse (`CashRegister`) | `registerCode` (ex. `CSR-...`, généré) | Un tiroir-caisse. Porte `active`, `maxCashAmount` (plafond, sert seulement aux alertes), `managerEmail` (destinataire des rapports et alertes), `systemManaged`, `restrictedToMethod`. |
| Caisse automatique | `CSR-AUTO-WALLET`, `CSR-AUTO-MOBILE-MONEY` | Créée et tenue par le serveur pour journaliser les paiements portefeuille et mobile money réussis. `systemManaged = true` : **aucune saisie humaine n'y est acceptée** (`CashRegisterServiceImpl.java:685`). Elle apparaît dans les listes comme les autres. |
| Session (`CashSession`) | `sessionNumber` | Une période de tenue de caisse par un caissier : ouverture avec un fonds, mouvements, puis clôture avec comptage. Une seule session bloquante (`OPEN` ou `CLOSING_REVIEW`) par caisse **et** par caissier (`openedBy`). |
| Mouvement (`CashMovement`) | `movementNumber` | Une entrée ou sortie d'espèces dans une session. Type (`movementType`), pièce (`documentType`, `documentNumber`), contrepartie, motif. Toujours en XAF pour les saisies manuelles (`CashRegisterServiceImpl.java:57`). |
| Pièce jointe de mouvement | `id` numérique (int64) | Référence de fichier (`fileName`, `storagePath`) attachée à un mouvement. Le serveur ne reçoit pas le fichier, seulement son chemin. |
| Demande de caisse (`CashRequest`) | `requestNumber` | Demande soumise par un caissier sur sa session (remise de fonds, avance, remboursement de frais, justificatif). Son approbation crée automatiquement le mouvement correspondant. |
| Anomalie (`CashAnomalyFlag`) | `flagNumber` | Signalement calculé sur une session (écart inhabituel, montants ronds, remboursements excessifs...). Au plus un signalement par type et par session (`CashAnomalyDetectionServiceImpl.java:298`). |
| Annonce d'espèces (`CashPaymentDeclaration`) | `declarationNumber` | Un membre annonce depuis le portail qu'il viendra payer une facture en espèces. Rien n'est encaissé tant que la caisse ne confirme pas. |

Les champs « acteur » (`openedBy`, `closedBy`, `reviewedBy`, `createdBy`, `requestedBy`, `signedBy`, `uploadedBy`) sont du **texte libre envoyé par le frontend**, jamais déduit du jeton (sauf pour les annonces d'espèces). Le serveur s'en sert pour retrouver l'adresse de courriel : il cherche d'abord un compte dont l'identifiant (`userId`) vaut ce texte, puis un compte dont l'email vaut ce texte (`service/support/CashEmailNotifier.java:67`). Envoyer donc l'identifiant ou l'email du compte connecté, toujours le même, sinon la règle « un caissier, une session » et les statistiques par caissier ne tiennent pas.

### Types de mouvement et effet sur le solde attendu

Le montant attendu en fin de session vaut : fonds d'ouverture + paiements + entrées + ajustements - remboursements - sorties (`service/support/CashSessionSummarySupport.java:46`, totaux dans `repository/CashMovementRepository.java:22`).

| `movementType` | Sens (`flowDirection`) | Compté dans | Saisie manuelle par `POST .../movements` |
| --- | --- | --- | --- |
| `OPENING_FLOAT` | `IN` | fonds d'ouverture | non (créé à l'ouverture) |
| `PAYMENT` | `IN` | `totalPayments` | non (créé par l'encaissement d'une intention) |
| `CASH_IN`, `TRANSFER_IN` | `IN` | `totalCashIn` | oui |
| `REFUND` | `OUT` | `totalRefunds` | oui, motif obligatoire |
| `CASH_OUT`, `SAFE_DEPOSIT`, `TRANSFER_OUT` | `OUT` | `totalCashOut` | oui, motif obligatoire |
| `ADJUSTMENT` | `ADJUSTMENT` | `totalAdjustments`, **ajouté** au solde attendu | oui, motif obligatoire, montant strictement positif |
| `CLOSING_COUNT` | `ADJUSTMENT` | rien | non |

`documentType` possibles : `CASH_VOUCHER`, `ENTRY_VOUCHER`, `EXIT_VOUCHER`, `RECEIPT`, `INVOICE`, `EXPENSE_NOTE`, `BANK_SLIP`, `TRANSFER_NOTE`, `EXTERNAL_REFERENCE`, `OTHER`. Libellés français utilisés par le PDF (`CashDocumentPdfServiceImpl.java:116`) : pièce de caisse, bon d'entrée, bon de sortie, reçu, facture, note de frais, bordereau de remise en banque, avis de virement, pièce justificative.

## Statuts et cycle de vie

### Caisse

Pas d'énumération : un booléen `active`.

| État | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `active = true` | Peut ouvrir une session | création (`POST /sni/api/v1/cash-registers`), `PATCH .../{registerCode}/activate` | inactive |
| `active = false` | Aucune session ne peut s'ouvrir | `PATCH .../{registerCode}/deactivate`, refusé si une session `OPEN` ou `CLOSING_REVIEW` existe | active. Une caisse automatique désactivée est réactivée d'office au paiement suivant (`CashRegisterServiceImpl.java:530`). |

### Session (`CashSessionStatus`)

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `OPEN` | Session en cours, mouvements acceptés | `POST /sni/api/v1/cash-registers/sessions` ; ouverture automatique (caisse automatique, renouvellement de fin de mois) | `CLOSED` (clôture sans écart, fin de mois, seuil d'une caisse automatique), `CLOSING_REVIEW` (clôture avec écart) |
| `CLOSING_REVIEW` | Clôturée avec un écart non nul, attend la validation d'un superviseur. Plus aucun mouvement. Bloque encore la caisse et le caissier. | `PATCH .../sessions/{sessionNumber}/close` (ou `POST .../closing-request`) avec un compté différent de l'attendu | `CLOSED` par `PATCH .../approve-variance` |
| `CLOSED` | Définitivement close ; rapport PDF envoyé par courriel | clôture sans écart, validation de l'écart, worker de fin de mois, seuil automatique | aucun |
| `SUSPENDED` | Jamais atteint dans le code | aucun | aucun |
| `CANCELLED` | Jamais atteint dans le code | aucun | aucun |

Il n'existe pas de route pour rejeter un écart ni pour rouvrir une session.

### Mouvement (`CashMovementStatus`)

Tout mouvement est créé `CONFIRMED` (`model/CashMovement.java:81`) et n'en change jamais. `PENDING`, `REVERSED`, `CANCELLED` ne sont jamais atteints : un mouvement ne s'annule pas, il se compense par un mouvement inverse (champ `relatedMovementNumber` pour le lier). La signature (`signedBy`, `signedAt`) et l'impression (`printedAt`) sont des marques, pas des statuts.

### Demande de caisse (`CashRequestStatus`)

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `PENDING` | Soumise, en attente de décision | `POST .../sessions/{sessionNumber}/requests` | `EXECUTED` (approbation), `REJECTED` |
| `APPROVED` | Transitoire, jamais visible : l'approbation enchaîne l'exécution dans la même transaction | approbation | `EXECUTED` |
| `EXECUTED` | Approuvée et mouvement créé (`executedMovementNumber`) | `PATCH .../requests/{requestNumber}/approve` | aucun |
| `REJECTED` | Refusée | `PATCH .../requests/{requestNumber}/reject` | aucun |
| `CANCELLED` | Jamais atteint dans le code | aucun | aucun |

Type de demande et mouvement créé à l'approbation (`CashRequestServiceImpl.java:198`) :

| `requestType` | Mouvement | Pièce |
| --- | --- | --- |
| `REMISE_DE_FONDS` | `SAFE_DEPOSIT` (sortie) | `BANK_SLIP` |
| `CASH_ADVANCE` | `CASH_OUT` | `EXIT_VOUCHER` |
| `EXPENSE_REIMBURSEMENT` | `CASH_OUT` | `EXPENSE_NOTE` |
| `JUSTIFICATIF` | `ADJUSTMENT` (augmente le solde attendu) | `CASH_VOUCHER` |

Aucun type ne produit une entrée (`CASH_IN`) : il n'existe pas de demande d'approvisionnement. Un apport d'espèces se saisit directement en bon d'entrée.

### Anomalie (`CashAnomalyStatus`)

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `OPEN` | Détectée, non revue | analyse (automatique à la clôture, ou `POST .../anomalies/sessions/{sessionNumber}/analyze`) | n'importe quel statut par `PATCH .../anomalies/{flagNumber}/review` |
| `ACKNOWLEDGED` | Vue, en cours de traitement | revue | n'importe quel statut par revue |
| `DISMISSED`, `ESCALATED`, `RESOLVED` | Revue terminée | revue | aucun : une nouvelle revue est refusée |

`ESCALATED` ne déclenche rien de plus (pas de ticket) : le ticket de support n'est créé qu'à la détection d'une anomalie de sévérité `HIGH`.

### Annonce d'espèces (`CashDeclarationStatus`)

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `AWAITING_CONFIRMATION` | Le membre a annoncé un paiement en espèces, la caisse attend. La facture reste due, la réservation reste en attente de paiement. | portail : `POST /sni/api/v1/client/billing/invoices/{documentNumber}/pay/cash`, `POST /sni/api/v1/client/bookings/{bookingNumber}/pay/cash`, `POST /sni/api/v1/client/companies/{customerId}/invoices/{documentNumber}/pay/cash` | `CONFIRMED`, `CANCELLED`, `EXPIRED` |
| `CONFIRMED` | La caisse a compté l'argent ; un paiement espèces existe (`transactionNumber`, `cashSessionNumber`) | `POST /sni/api/v1/payments/cash-declarations/{declarationNumber}/confirm` | aucun |
| `CANCELLED` | Abandonnée par le membre ou par la caisse ; la facture reste due | `POST .../cash-declarations/{declarationNumber}/cancel` (back-office) ou `POST /sni/api/v1/client/billing/cash-declarations/{declarationNumber}/cancel` (membre) | aucun |
| `EXPIRED` | Délai dépassé (4 h par défaut) sans encaissement ; la réservation liée est annulée si elle attendait encore le paiement | worker toutes les 15 minutes | aucun |

## Parcours

### 1. Créer et gérer les caisses

1. `POST /sni/api/v1/cash-registers` ([route](routes/CashRegisterController.md#r-post-sni-api-v1-cash-registers)) avec `name` (obligatoire), `locationCode`, `maxCashAmount`, `managerEmail`. `businessEntityCode` et `deviceCode` envoyés ne sont **pas** repris tels quels : ils servent d'indice pour fabriquer `BIZ-...` et `DEV-...` horodatés (`CashRegisterServiceImpl.java:910`). Le `registerCode` est aussi généré. Afficher les codes renvoyés, ne pas présumer ceux saisis.
2. `GET /sni/api/v1/cash-registers?active=&locationCode=&businessEntityCode=&searchText=` pour la liste paginée.
3. `PATCH .../{registerCode}/deactivate` ou `/activate`. La désactivation échoue tant qu'une session est `OPEN` ou `CLOSING_REVIEW`.
4. Pour chaque caisse, si `systemManaged = true`, masquer toute action de saisie (ouverture, mouvement, bon, clôture) : le serveur les refuse.

### 2. Ouvrir une session

1. `POST /sni/api/v1/cash-registers/sessions` ([route](routes/CashRegisterController.md#r-post-sni-api-v1-cash-registers-sessions)) avec `registerCode`, `openedBy` (identifiant du caissier connecté), `openingAmount` (fonds de caisse, facultatif, 0 par défaut).
2. Retour : `CashSessionResponse` en `OPEN`. Si `openingAmount > 0`, un mouvement `OPENING_FLOAT` (pièce `CASH_VOUCHER`, numéro = `sessionNumber`) est créé (`CashRegisterServiceImpl.java:153`).
3. L'écran bascule sur le tableau de bord de la session : `GET .../sessions/{sessionNumber}/summary` pour les totaux et le solde attendu, `GET .../movements?sessionNumber=...` pour le journal.
4. Pour retrouver la session en cours d'un caissier : `GET .../sessions?openedBy=<identifiant>&status=OPEN`.

### 3. Enregistrer des mouvements, bons d'entrée et de sortie

Toutes ces routes exigent une session `OPEN` sur une caisse non automatique.

- **Bon d'entrée** : `POST .../sessions/{sessionNumber}/entry-vouchers` ([route](routes/CashRegisterController.md#r-post-sni-api-v1-cash-registers-sessions-sessionnumber-entry-vouchers)) avec `amount`, `documentNumber` (obligatoire, numéro de la pièce papier saisi par l'utilisateur, le serveur n'en génère pas), `createdBy` (obligatoire), `reason` (obligatoire avec le type de pièce par défaut), contrepartie facultative. Crée un `CASH_IN` de pièce `ENTRY_VOUCHER`.
- **Bon de sortie** : `POST .../sessions/{sessionNumber}/exit-vouchers`, mêmes champs. Crée un `CASH_OUT` de pièce `EXIT_VOUCHER`.
- **Mouvement libre** : `POST .../sessions/{sessionNumber}/movements` ([route](routes/CashRegisterController.md#r-post-sni-api-v1-cash-registers-sessions-sessionnumber-movements)) avec `movementType` (parmi les types saisissables de la table plus haut), `amount`, `createdBy`, et selon le cas `reason`, `documentType`, `documentNumber`. Champs d'enrichissement facultatifs : `relatedMovementNumber` (doit exister), `batchId`, `exchangeRate`, `channel` (`MANUAL` par défaut), `deviceCode`, `deviceIp`, `subCategory`, `tags`.
- Retour : `CashMovementResponse`. `runningBalance` n'est calculé que pour les mouvements créés par cette dernière route (et par l'approbation d'une demande), il est `null` pour les bons et les paiements.
- Listes : `GET .../sessions/{sessionNumber}/entry-vouchers` et `.../exit-vouchers` filtrent sur le **type de pièce** `ENTRY_VOUCHER` / `EXIT_VOUCHER`. Un bon créé avec un autre `documentType` n'y apparaît pas : utiliser `GET /sni/api/v1/cash-registers/movements` (filtres `registerCode`, `sessionNumber`, `movementType`, `documentType`, `documentNumber`, `referenceCode`, `counterpartyCode`, `createdBy`, `fromDate`, `toDate`, `searchText`).
- Détail : `GET .../movements/{movementNumber}`. Pour un paiement, les champs `relatedTransactionNumber`, `relatedIntentNumber`, `relatedReceiptNumber`, `relatedCustomer*` et `relatedSource*` sont remplis à partir de la transaction.

Un mouvement ne se modifie ni ne se supprime. Pour corriger, saisir un mouvement inverse et renseigner `relatedMovementNumber`.

### 4. Pièces jointes, signature, impression

1. Téléverser le fichier ailleurs (non vérifié : aucune route de téléversement n'existe dans le module caisse ; le serveur ne stocke que le chemin), puis `POST .../movements/{movementNumber}/attachments` ([route](routes/CashRegisterController.md#r-post-sni-api-v1-cash-registers-movements-movementnumber-attachments)) avec `fileName` et `storagePath` (obligatoires), `contentType`, `label`, `uploadedBy`. Retour : le mouvement complet avec `attachments` (plus récent d'abord). Aucun contrôle de statut de session : possible aussi sur une session close.
2. Signer : `PATCH .../movements/{movementNumber}/sign?signedBy=<identifiant>` (paramètre de requête obligatoire, pas de corps). Écrase une signature précédente. `requiresSignature` vaut toujours `false` : aucun code ne le pose.
3. Imprimer : `GET /sni/api/v1/cash-registers/movements/{movementNumber}/document` ([route](routes/CashDocumentController.md#r-get-sni-api-v1-cash-registers-movements-movementnumber-document)) renvoie le PDF de la pièce (`Content-Disposition: inline; filename="<movementNumber>.pdf"`). Puis `PATCH .../movements/{movementNumber}/print` pour horodater `printedAt` (le serveur ne le fait pas seul à la génération).

### 5. Encaisser une intention de paiement en espèces (lien avec les paiements)

Côté caisse, un paiement en espèces est un mouvement `PAYMENT` (pièce `RECEIPT`, `referenceType = PAYMENT_TRANSACTION`, `referenceCode` = numéro de transaction) écrit dans une session ouverte.

1. Le guichet doit avoir une session `OPEN` sur une caisse ordinaire. Charger la session du caissier connecté (parcours 2, étape 4).
2. `PATCH /sni/api/v1/payments/intents/{intentNumber}/cash` ([route](routes/PaymentController.md#r-patch-sni-api-v1-payments-intents-intentnumber-cash)) avec `receivedBy`, `cashSessionNumber` (exigé, une chaîne vide est refusée), `amount` facultatif.
3. Le serveur (`PaymentServiceImpl.java:280`) refuse une devise autre que XAF, crée la transaction réussie, puis appelle `recordPayment` (`CashRegisterServiceImpl.java:477`) qui exige une session `OPEN`, une caisse non automatique et un montant positif. Tout échec annule l'ensemble (transaction unique).
4. Le paiement apparaît dans le journal de la session et augmente `totalPayments` et le solde attendu.

Ne sont pas contrôlés : que la session appartienne à `receivedBy`, que la caisse soit active, que le plafond `maxCashAmount` soit respecté.

Autres chemins qui écrivent un paiement espèces en caisse : la confirmation d'une annonce (parcours 12) et le règlement de facture `POST /sni/api/v1/billing/invoices/{documentNumber}/pay` avec `paymentMethod = CASH` ([route](../billing/routes/BillingDocumentController.md#r-post-sni-api-v1-billing-invoices-documentnumber-pay)) ; ce dernier ne crée le mouvement que si `cashSessionNumber` est non vide (voir Écarts). Les paiements portefeuille et mobile money réussis sont journalisés seuls dans les caisses automatiques. Un remboursement de transaction espèces ne crée aucun mouvement : le saisir en `REFUND` (non vérifié en dehors du module `payment`).

### 6. Demandes de caisse et leur approbation

1. Caissier : `POST .../sessions/{sessionNumber}/requests` ([route](routes/CashRequestController.md#r-post-sni-api-v1-cash-registers-sessions-sessionnumber-requests)) avec `requestType`, `amount` (> 0), `reason` (obligatoire côté service), `requestedBy` (défaut : `openedBy` de la session), `attachments` (liste de références `fileName`, `contentType`, `storagePath`, `label`). Retour `PENDING`. Le statut de la session n'est pas vérifié à la soumission.
2. Superviseur : `GET /sni/api/v1/cash-registers/requests?status=PENDING` (filtres `registerCode`, `sessionNumber`, `requestType`, `requestedBy`, `searchText`), puis `GET .../requests/{requestNumber}`.
3. `PATCH .../requests/{requestNumber}/approve` avec `reviewedBy` et `note`. Le serveur crée le mouvement (table plus haut) dans la session de la demande, avec `createdBy = reviewedBy`, puis passe la demande en `EXECUTED`. Si la session n'est plus `OPEN`, l'approbation échoue en `400 Cash session is not open` et la demande reste `PENDING` : elle ne pourra plus qu'être rejetée.
4. `PATCH .../requests/{requestNumber}/reject` avec `reviewedBy` et `note` (motif).
5. PDF de la demande : `GET .../requests/{requestNumber}/document` ([route](routes/CashDocumentController.md#r-get-sni-api-v1-cash-registers-requests-requestnumber-document)).

### 7. Clôturer une session (comptage et écart)

1. Avant la saisie, afficher `GET .../sessions/{sessionNumber}/summary` : `expectedClosingAmount` et le détail des totaux.
2. `PATCH /sni/api/v1/cash-registers/sessions/{sessionNumber}/close` ([route](routes/CashRegisterController.md#r-patch-sni-api-v1-cash-registers-sessions-sessionnumber-close)) avec `closedBy`, `countedClosingAmount` (ou, à défaut, `closingAmount` ; l'un des deux est requis) et `varianceReason` (obligatoire si le compté diffère de l'attendu). L'écran calcule l'écart en direct (`compté - attendu`) et rend le motif obligatoire dès qu'il n'est pas nul.
3. Retour :
   - écart nul : `CLOSED`, `closedAt` posé, rapport PDF envoyé par courriel ;
   - écart non nul : `CLOSING_REVIEW`, courriel « Écart de caisse à valider » au superviseur. L'écran indique que la caisse et le caissier restent bloqués jusqu'à validation.
4. Dans les deux cas, l'analyse d'anomalies est lancée après validation en base.

`POST .../sessions/{sessionNumber}/closing-request` appelle exactement la même méthode (`CashRegisterController.java:87`) : ce n'est pas une demande séparée, c'est une clôture. Elle n'exige que `CASH:ADJUST` au lieu de `CASH:CLOSE_SESSION`.

### 8. Valider un écart

1. Liste : `GET .../sessions?status=CLOSING_REVIEW`.
2. `PATCH .../sessions/{sessionNumber}/approve-variance` ([route](routes/CashRegisterController.md#r-patch-sni-api-v1-cash-registers-sessions-sessionnumber-approve-variance)) avec `reviewedBy` et `note`. La note est ajoutée à `varianceReason` sous la forme `<motif> | Review: <note>`.
3. Retour `CLOSED`. Courriel au caissier (`openedBy`) et rapport de clôture envoyé.

### 9. Rapport de clôture PDF

`GET /sni/api/v1/cash-registers/sessions/{sessionNumber}/closing-report` ([route](routes/CashDocumentController.md#r-get-sni-api-v1-cash-registers-sessions-sessionnumber-closing-report)) : synthèse, statistiques de session et liste des mouvements. Disponible quel que soit le statut, y compris `OPEN` (rapport provisoire). Le même PDF part en pièce jointe à chaque clôture définitive.

### 10. Anomalies

1. Liste : `GET /sni/api/v1/cash-registers/anomalies` ([route](routes/CashAnomalyFlagController.md#r-get-sni-api-v1-cash-registers-anomalies)) avec filtres `registerCode`, `sessionNumber`, `severity`, `status`, `anomalyType`.
2. Relancer l'analyse : `POST .../anomalies/sessions/{sessionNumber}/analyze`. Retour : **seulement les nouveaux** signalements (liste vide si tous existaient déjà).
3. Revue : `PATCH .../anomalies/{flagNumber}/review` avec `reviewedBy`, `status`, `note`. Proposer `ACKNOWLEDGED`, `DISMISSED`, `ESCALATED`, `RESOLVED` tant que le statut est `OPEN` ou `ACKNOWLEDGED`.

Règles de détection (`CashAnomalyDetectionServiceImpl.java:122` à `261`) :

| Type | Déclenché quand | Sévérité |
| --- | --- | --- |
| `VARIANCE_OUTLIER` | écart de la session éloigné de la moyenne du caissier (z-score > 2 ; ou > 20 000 sans historique) | `MEDIUM` si z > 2, `HIGH` si z > 3, `LOW` sans historique |
| `ROUND_NUMBER_PATTERN` | au moins 3 mouvements et plus de 50 % multiples de 5 000 | `LOW`, `MEDIUM` à partir de 80 % |
| `EXCESSIVE_REFUNDS` | au moins 3 remboursements et ratio remboursements / paiements > 30 % | `MEDIUM`, `HIGH` à partir de 60 % |
| `EXCESSIVE_ADJUSTMENTS` | au moins 5 ajustements | `MEDIUM`, `HIGH` à partir de 10 |
| `OFF_HOURS_SESSION` | session ouverte entre 22 h et 6 h **UTC** | `LOW` |
| `THRESHOLD_STRUCTURING` | au moins 3 mouvements entre 80 % et 100 % de `maxCashAmount` | `MEDIUM`, `HIGH` à partir de 5 |
| `NEAR_MAX_CASH_RECURRENCE` | au moins 2 mouvements dont `runningBalance` atteint 90 % du plafond | `MEDIUM`, `HIGH` à partir de 4 |

En plus, un `runningBalance` au-dessus du plafond envoie un courriel au responsable de la caisse (au plus un toutes les 6 heures par caisse), sans créer de signalement.

### 11. Statistiques et tableaux de bord

- Vue d'ensemble : `GET /sni/api/v1/cash-registers/metrics/overview?registerCode=&businessEntityCode=&fromDate=&toDate=` : nombres de caisses, de sessions par statut, montants par type, solde net, `pendingVarianceAmount` (somme des écarts en attente). Devise toujours `XAF`.
- Par caisse : `GET .../metrics/registers?businessEntityCode=&fromDate=&toDate=` (liste non paginée).
- Session : `GET /sni/api/v1/cash-registers/statistics/sessions/{sessionNumber}` ([route](routes/CashStatisticsController.md#r-get-sni-api-v1-cash-registers-statistics-sessions-sessionnumber)) : durée, mouvements par heure, entrées, sorties, temps mort, écart en pourcentage. Pour une session ouverte, la fin de référence est l'instant présent.
- Caissier : `GET .../statistics/cashiers/{cashierCode}` où `cashierCode` est la valeur exacte de `openedBy`. Renvoie fréquence d'écart, écart moyen et écart type, tendance `STABLE` / `IMPROVING` / `DEGRADING` (au moins 4 sessions closes), `riskScore` sur 100. Un code inconnu renvoie des zéros, pas une erreur.
- Caisse : `GET .../statistics/registers/{registerCode}?fromDate=&toDate=` : heures de pointe (5 heures les plus chargées, en heure **UTC**), variation par rapport à la période précédente de même durée (`null` sans `fromDate` et `toDate`, ou si la période précédente est vide).

### 12. File des espèces annoncées depuis le portail

Le membre annonce depuis le portail (voir [portal/routes.md](../portal/routes.md), routes `.../pay/cash`) ; la caisse confirme quand il se présente.

1. File du guichet : `GET /sni/api/v1/payments/cash-declarations?status=AWAITING_CONFIRMATION` ([route](routes/CashPaymentDeclarationController.md#r-get-sni-api-v1-payments-cash-declarations)). Avec un `status`, la liste est triée de la plus ancienne à la plus récente (ordre de passage) ; sans `status`, de la plus récente à la plus ancienne. Afficher `customerName`, `documentNumber`, `bookingNumber`, `amount`, `expiresAt` (compte à rebours).
2. Le membre se présente : `POST .../cash-declarations/{declarationNumber}/confirm` ([route](routes/CashPaymentDeclarationController.md#r-post-sni-api-v1-payments-cash-declarations-declarationnumber-confirm)) avec `cashSessionNumber` (obligatoire, session `OPEN` du caissier), `amount` facultatif (défaut : montant annoncé ; un montant plus faible encaisse partiellement), `receivedBy` (défaut : nom du compte connecté), `note`.
3. Le serveur relit le solde de la facture, crée une intention de paiement sur la facture puis l'encaisse en espèces comme au parcours 5 : mouvement `PAYMENT` dans la session, imputation sur la facture, confirmation de la réservation quand l'intention est soldée. Retour : l'annonce `CONFIRMED` avec `transactionNumber`. Proposer ensuite l'impression du reçu (module paiements) ou de la pièce de caisse du mouvement.
4. Le membre ne viendra pas : `POST .../cash-declarations/{declarationNumber}/cancel` avec `{ "reason": "..." }` facultatif (défaut « Annulée par la caisse »). La facture reste due, la réservation n'est pas touchée.
5. Une confirmation partielle clôt l'annonce : le reste de la facture reste dû sans nouvelle annonce.

## Règles et erreurs à refléter dans l'écran

Toutes les erreurs métier sont en `400 BAD_REQUEST` ou `404 RESOURCE_NOT_FOUND` ; messages exacts ci-dessous. La plupart des messages des caisses sont en anglais.

| Situation | Statut | Message |
| --- | --- | --- |
| Ouverture sur caisse inactive | 400 | `Cash register is inactive` |
| Caisse déjà dotée d'une session `OPEN` ou `CLOSING_REVIEW` | 400 | `Cash register already has an active session` |
| Caissier (`openedBy`) déjà sur une session `OPEN` ou `CLOSING_REVIEW` | 400 | `Cashier already has an active cash session` |
| Saisie sur caisse automatique | 400 | `La caisse <code> est tenue automatiquement par le systeme · aucune saisie manuelle n'y est acceptee, utilisez une caisse ordinaire` |
| Désactivation avec session bloquante | 400 | `Cash register has an active session` |
| Session absente / caisse absente | 404 | `Cash session not found` / `Cash register not found` |
| Mouvement, bon, clôture, encaissement sur session non ouverte | 400 | `Cash session is not open` |
| `movementType` `PAYMENT`, `OPENING_FLOAT` ou `CLOSING_COUNT` en saisie | 400 | `Movement type is system managed` |
| Montant nul ou négatif | 400 | `Cash amount must be positive` |
| `createdBy` absent sur un mouvement | 400 | `Created by is required for manual cash movement` |
| Motif absent (types `REFUND`, `CASH_OUT`, `SAFE_DEPOSIT`, `ADJUSTMENT`, `TRANSFER_OUT`) | 400 | `Reason is required for this cash movement type` |
| Motif absent (pièces `ENTRY_VOUCHER`, `EXIT_VOUCHER`, `EXPENSE_NOTE`) | 400 | `Reason is required for this cash document type` |
| Numéro de pièce absent (pièces `ENTRY_VOUCHER`, `EXIT_VOUCHER`, `CASH_VOUCHER`, `BANK_SLIP`, `EXPENSE_NOTE`) | 400 | `Document number is required for this cash document type` |
| `relatedMovementNumber` inconnu | 404 | `Related cash movement not found: <numéro>` |
| Mouvement inconnu | 404 | `Cash movement not found: <numéro>` |
| `signedBy` vide | 400 | `Signer identifier is required` |
| Clôture sans montant compté | 400 | `Counted closing amount is required` |
| Écart sans motif | 400 | `Variance reason is required when counted amount differs from expected amount` |
| Validation d'écart hors `CLOSING_REVIEW` | 400 | `Cash session is not waiting for variance review` |
| Demande : montant ou motif manquant | 400 | `Cash request amount must be positive` / `Cash request reason is required` |
| Décision sur une demande non `PENDING` | 400 | `Cash request <numéro> is not pending` |
| Demande inconnue | 404 | `Cash request not found: <numéro>` |
| Revue d'une anomalie déjà close | 400 | `Anomaly flag <numéro> has already been resolved` |
| Anomalie inconnue | 404 | `Anomaly flag not found: <numéro>` |
| Statistiques : session ou caisse inconnue | 404 | `Session de caisse introuvable : <numéro>` / `Caisse introuvable : <code>` |
| Échec de génération PDF | 400 | `Unable to generate cash document PDF` |
| Encaissement d'intention hors XAF | 400 | `Cash payments are only allowed in XAF` |
| Encaissement d'intention sans session | 400 | `Cash session number is required for cash payments` |
| Confirmation d'une annonce déjà close | 400 | `L'annonce <numéro> est déjà confirmée · elle ne peut plus être confirmée.` (ou `annulée`, `expirée`) |
| Annulation d'une annonce déjà close | 400 | `L'annonce <numéro> est déjà <état>.` |
| Facture soldée entre-temps | 400 | `La facture <numéro> a été réglée entre-temps · rien à encaisser.` |
| Montant encaissé invalide | 400 | `Le montant encaissé doit être positif.` / `Le montant encaissé dépasse le solde de la facture (<solde> <devise>).` |
| Filtre `status` inconnu sur la file | 400 | `État inconnu : <valeur>. Valeurs possibles : AWAITING_CONFIRMATION, CONFIRMED, CANCELLED, EXPIRED.` |
| Annonce inconnue | 404 | `Annonce de paiement introuvable : <numéro>` |

Autres règles :

- Montants arrondis à 4 décimales côté serveur ; afficher en XAF sans décimales.
- `ADJUSTMENT` n'accepte qu'un montant positif et **augmente** toujours le solde attendu. Il n'existe pas d'ajustement négatif : pour un manque, saisir une sortie (`CASH_OUT`) avec motif.
- Le plafond `maxCashAmount` ne bloque jamais une saisie. Il alimente seulement les anomalies et l'alerte de dépassement.
- `cashControlEnabled` est enregistré mais lu nulle part : il ne change aucun comportement.

## À prévoir côté frontend

### Écrans

1. **Caisses** : liste, création, activation ou désactivation ; badge « automatique » si `systemManaged`, moyen accepté si `restrictedToMethod`.
2. **Ma caisse** (caissier) : ouverture de session ; tableau de bord de la session ouverte (synthèse, journal, boutons bon d'entrée, bon de sortie, mouvement, demande) ; clôture avec comptage.
3. **Sessions** : liste filtrable par caisse, statut, caissier ; détail avec synthèse, mouvements, demandes, anomalies, statistiques, rapport PDF ; file des écarts à valider (`CLOSING_REVIEW`).
4. **Mouvement** : détail, pièces jointes, signature, PDF, marque d'impression.
5. **Demandes de caisse** : file `PENDING`, détail, approbation, rejet, PDF.
6. **Anomalies** : liste filtrable, revue.
7. **Tableau de bord des caisses** : métriques globales et par caisse, statistiques caissier et caisse.
8. **Guichet espèces** : file des annonces `AWAITING_CONFIRMATION`, confirmation (avec choix de la session ouverte), annulation.

Le serveur envoie dans ses courriels des liens vers le back-office (`app.admin-access-url`) de la forme `/payments/cash-registers/sessions/{sessionNumber}`, `/payments/cash-registers/requests/{requestNumber}` et `/payments/cash-registers/anomalies/{flagNumber}` (ou `/payments/cash-registers/anomalies/{sessionNumber}` pour l'alerte de plafond). Prévoir ces routes côté frontend.

### Droits (pour masquer les boutons)

Règle d'URL appliquée par `security/authorization/AdminApiAuthorizationManager.java:147` :

| Action | Droit |
| --- | --- |
| Toute lecture sous `/sni/api/v1/cash-registers` (listes, détails, PDF, métriques, statistiques) | `CASH:READ` |
| `PATCH .../sessions/{sessionNumber}/close` | `CASH:CLOSE_SESSION` |
| Tout autre écrit sous `/cash-registers` : création et (dés)activation de caisse, ouverture de session, mouvements, bons, pièces jointes, signature, impression, `closing-request`, validation d'écart, demandes (soumission, approbation, rejet), analyse et revue d'anomalies | `CASH:ADJUST` |
| File des annonces (lecture) | `PAYMENT:READ` et l'un des rôles `ADMIN`, `SUPER_ADMIN`, `MANAGER`, `CASHIER` |
| Confirmation et annulation d'une annonce, encaissement d'intention (`PATCH /payments/intents/{intentNumber}/cash`) | `PAYMENT:PROCESS` (et, pour les annonces, l'un des quatre rôles) |

Le droit `CASH:OPEN_SESSION` existe en base mais n'est exigé par aucune route : l'ouverture de session demande `CASH:ADJUST`. Il n'y a pas de droit séparé pour approuver (demande ou écart) : quiconque a `CASH:ADJUST` peut soumettre et approuver, y compris sa propre demande ou son propre écart. Si l'organisation veut séparer les rôles, l'écran doit au moins masquer « Approuver » quand `reviewedBy` serait égal à `requestedBy` ou `closedBy` ; le serveur ne le refuse pas.

### Pièges

- Les champs acteurs sont à remplir par le frontend avec l'identifiant du compte connecté (voir Notions). Ne pas proposer de les saisir librement.
- `CashSessionResponse` d'une session `OPEN` renvoie `countedClosingAmount = closingAmount = expectedClosingAmount` et `varianceAmount = 0` (valeurs calculées, `CashRegisterMapperDecorator.java:72`). N'afficher compté et écart que si `status` n'est pas `OPEN`. Pour une session close par le worker de fin de mois (rien compté), la même réponse affiche aussi un compté égal à l'attendu et un écart nul ; `GET .../summary` renvoie, lui, `countedClosingAmount = null` : s'appuyer sur la synthèse et sur `closedBy = SYSTEM`.
- Les sessions automatiques ont `openedBy` `SYSTEM_WALLET`, `SYSTEM_MOBILE_MONEY` ou `SYSTEM`, et sont closes à `SYSTEM` quand leur total atteint 10 000 000 XAF (configurable, `bokati.payment.cash-session.auto-limit`). Les exclure des écrans caissier.
- `runningBalance` est souvent `null` (voir parcours 3) : ne pas en faire une colonne de solde du journal ; calculer le solde courant côté écran depuis la synthèse si besoin.
- `relatedMovementNumber`, pas l'identifiant numérique, sert à lier deux mouvements. Seul l'`id` des pièces jointes est un int64 (voir [README racine](../README.md#identifiants--attention-aux-grands-nombres)).
- Les heures des anomalies « hors horaires » et des heures de pointe sont en UTC ; le reste de l'application vit en `Africa/Brazzaville` (UTC+1).
- Tri : les listes de caisses, sessions, mouvements, demandes et anomalies ignorent le paramètre `sort` (le service le retire, `CashRegisterServiceImpl.java:882`) ; les sessions sortent par `openedAt` décroissant, les autres selon l'ordre fixé dans leur requête.
- Les routes d'écriture ne déclarent pas `Idempotency-Key` : désactiver le bouton pendant l'envoi pour éviter un double bon ou une double confirmation.

## Effets automatiques

| Déclencheur | Effet |
| --- | --- |
| Paiement portefeuille ou mobile money réussi (`PaymentTransactionWorkflowProcessor.java:107`) | Mouvement `PAYMENT` dans la caisse automatique du moyen ; caisse et session créées au besoin ; session close (`CLOSED`, sans écart) et renouvelée quand le total atteint le seuil (10 000 000 XAF par défaut). Un échec est journalisé sans bloquer le paiement. |
| Worker de fin de mois (`worker/CashSessionMonthEndWorker.java:37`), chaque nuit à 00:05 `Africa/Brazzaville` (`bokati.payment.cash-session.month-end-cron`), une seule passe par jour même à plusieurs instances | Toute session `OPEN` ouverte avant le 1er du mois courant est close par `SYSTEM` au montant attendu, sans comptage (`countedClosingAmount` et `varianceAmount` à `null`, motif explicite), et une nouvelle session est ouverte au nom du même caissier avec ce montant comme fonds (mouvement `OPENING_FLOAT` « Report de la session ... »). Les sessions en `CLOSING_REVIEW` ne sont pas touchées. |
| Clôture définitive (`CLOSED`) par un humain, une validation d'écart ou le système | Courriel avec le PDF de clôture (`cloture-caisse-<session>.pdf`) à tous les comptes actifs `SUPER_ADMIN`, `ADMIN`, `MANAGER`, plus `managerEmail` de la caisse ou, à défaut, l'adresse superviseur (`service/support/CashClosingReportMailer.java:69`). Désactivable par `bokati.payment.cash-session.closing-report.enabled`. |
| Clôture humaine (avec ou sans écart) et fin de mois d'une caisse ordinaire | Analyse d'anomalies après validation en base (`service/support/CashSessionAnomalyDetectionListener.java:21`) ; si des anomalies nouvelles sont trouvées, alerte WebSocket sur `/topic/admin/alerts` (`alertType = CASH_ANOMALY`, sévérité `CRITICAL`). |
| Clôture avec écart | Courriel « Écart de caisse à valider · <session> » à l'adresse superviseur (`bokati.payment.cash-session.supervisor-email`). |
| Validation d'écart | Courriel « Écart de caisse validé » au caissier (`openedBy`). |
| Soumission d'une demande | Courriel « Nouvelle demande de caisse » au superviseur. |
| Approbation ou rejet | Courriel au demandeur (`requestedBy`). |
| Anomalie `MEDIUM` ou `HIGH` détectée | Courriel « Anomalie de caisse détectée » au superviseur. `HIGH` : ticket de support automatique (priorité `HIGH`, catégorie `BILLING`, source `CASH_ANOMALY_FLAG` / `flagNumber`), échec silencieux. |
| Solde courant au-dessus du plafond | Courriel « Dépassement du plafond de caisse » au responsable, au plus toutes les 6 h par caisse (`bokati.payment.cash-register.alert-cooldown-hours`). |
| Annonce d'espèces créée | Notification in-app et courriel (file d'événements, modèle `CASH_PAYMENT_DECLARED`) à chaque adresse de `bokati.payment.cash-declaration.desk-email` ; alerte WebSocket `/topic/admin/alerts` (`CASH_PAYMENT_DECLARED`, `WARNING`). |
| Annonce confirmée | Courriel au client (`CASH_PAYMENT_CONFIRMED`) si une adresse est connue. |
| Worker d'expiration (`cash/worker/CashDeclarationExpiryWorker.java:46`), toutes les 15 min (`bokati.payment.cash-declaration.expiry-delay-ms`), lots de 100 | Annonces échues (validité 4 h, `bokati.payment.cash-declaration.validity-hours`) passées en `EXPIRED` ; réservation liée annulée par le système si elle est encore `PENDING_PAYMENT` ou `DRAFT` ; courriel au client (`CASH_PAYMENT_DECLARATION_EXPIRED`). |
| Annonce annulée | Rien d'autre qu'une ligne de journal. |

## Écarts relevés

- `security/authorization/AdminApiAuthorizationManager.java:149` et `controller/CashRegisterController.java:87` : `POST .../sessions/{sessionNumber}/closing-request` exécute la même clôture que `PATCH .../close` mais n'exige que `CASH:ADJUST`. Le droit `CASH:CLOSE_SESSION` est contournable.
- `security/authorization/AdminApiAuthorizationManager.java:148` : la règle `/open` ne correspond à aucune route ; `CASH:OPEN_SESSION` n'est jamais exigé, l'ouverture passe sous `CASH:ADJUST`.
- `service/implementation/CashRegisterServiceImpl.java:142`, `:192`, `:229` et `CashRequestServiceImpl.java:97` : `openedBy`, `closedBy`, `reviewedBy` sont pris dans le corps, jamais du jeton ; aucune vérification que le valideur d'un écart ou d'une demande diffère du caissier.
- `service/implementation/CashRegisterServiceImpl.java:367` à `379` : `runningBalance` part de `openingAmount` puis ajoute à nouveau le mouvement `OPENING_FLOAT` de la même session ; le fonds d'ouverture est compté deux fois. Les anomalies `NEAR_MAX_CASH_RECURRENCE` et l'alerte de plafond s'appuient sur cette valeur.
- `service/implementation/CashRegisterServiceImpl.java:381` contre `repository/CashMovementRepository.java:27` : `ADJUSTMENT` est neutre pour `runningBalance` mais ajouté au solde attendu ; une demande `JUSTIFICATIF` approuvée augmente donc l'argent attendu en caisse.
- `service/support/CashSessionAnomalyDetectionListener.java:21` : l'écouteur `AFTER_COMMIT` appelle un service `@Transactional` sans `REQUIRES_NEW` (le rapport de clôture, lui, le déclare, `CashClosingReportMailer.java:70`). Non vérifié par exécution : les signalements créés à la clôture risquent de ne jamais être enregistrés alors que courriels et alerte partent.
- `cash/service/CashPaymentDeclarationService.java:210` à `215` : quand la facture est déjà soldée, l'annonce est passée en `CANCELLED` puis une exception est levée dans la même transaction ; l'annulation est annulée avec elle et l'annonce reste `AWAITING_CONFIRMATION` jusqu'à son expiration.
- `cash/service/CashPaymentDeclarationService.java:242` : la note de confirmation est rangée dans `closeReason`.
- `service/implementation/PaymentServiceImpl.java:508` : un règlement de facture `CASH` via `POST /sni/api/v1/billing/invoices/{documentNumber}/pay` avec `cashSessionNumber` vide (`""` passe le `@NotNull` de `PayInvoiceRequest`) réussit sans aucun mouvement de caisse, contrairement à `PATCH /payments/intents/{intentNumber}/cash` (`PaymentServiceImpl.java:290`). Le même `@NotNull` impose aussi le champ pour un paiement portefeuille.
- `service/implementation/CashRequestServiceImpl.java:57` : la soumission d'une demande ne vérifie pas que la session est ouverte ; l'approbation échoue ensuite et la demande reste `PENDING` indéfiniment.
- `mapper/decorator/CashRegisterMapperDecorator.java:72` à `75` : `CashSessionResponse` invente un compté et un écart nul pour les sessions ouvertes et pour les clôtures de fin de mois non comptées.
- Statuts jamais atteints : `CashSessionStatus.SUSPENDED` et `CANCELLED`, `CashRequestStatus.CANCELLED` (et `APPROVED` jamais visible), `CashMovementStatus.PENDING`, `REVERSED`, `CANCELLED`. `requiresSignature` et `riskScore` des mouvements ne sont jamais renseignés ; `cashControlEnabled` n'est lu nulle part.
- `service/implementation/CashAnomalyDetectionServiceImpl.java:110` : la revue accepte n'importe quel statut cible, y compris `OPEN`. Message en anglais « has already been resolved » même pour `DISMISSED` ou `ESCALATED`.
- `service/implementation/CashAnomalyDetectionServiceImpl.java:211` et `CashStatisticsServiceImpl.java` (heures de pointe) : heures calculées en UTC alors que le worker de fin de mois et les courriels utilisent `Africa/Brazzaville`.
- `service/implementation/CashAnomalyDetectionServiceImpl.java:288` : le lien du courriel de dépassement de plafond pointe sur `/anomalies/{sessionNumber}` alors que les autres liens d'anomalie utilisent `flagNumber`.
- `cash/service/CashDeclarationNotifier.java:97` : le commentaire annonce que la caisse est prévenue d'une annulation, le code ne fait que journaliser.
- `controller/CashRegisterController.java:47` et suivants : `businessEntityCode` et `deviceCode` envoyés à la création sont transformés en codes générés, sans que le modèle de requête le signale.
- Messages d'erreur des caisses, demandes et anomalies en anglais, ceux des annonces et statistiques en français.
