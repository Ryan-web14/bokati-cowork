# Centre KYC et GED : plan d'amélioration et règlement des écarts

Branche `feature/kyc-ged`, partie de new-main 68407979 le 16/11/2026. Plan validé avec les décisions de l'exploitant du 16/11/2026 (voir plus bas).

État : **KG0 fait** ([lot-kg0-droits.md](lot-kg0-droits.md)), **KG1 fait** ([lot-kg1-cycle-kyc.md](lot-kg1-cycle-kyc.md)), **KG2 fait** ([lot-kg2-recto-verso.md](lot-kg2-recto-verso.md)), **KG3 fait** ([lot-kg3-evenements-relances.md](lot-kg3-evenements-relances.md)), **KG9 première partie faite** ([lot-kg9-controles-qualite.md](lot-kg9-controles-qualite.md) : contrôles croisés préenregistrés, qualité d'image au dépôt).

## Constat (vérifié dans le code le 16/11/2026)

Les écarts listés dans `docs/final-frontend-doc/document/README.md` ont été revus un par un dans le code actuel :

- **KYC** : 9 confirmés (le dixième, l'approbation en masse avec `KYC:READ`, est réglé depuis le 08/11) et 12 défauts nouveaux.
- **GED** : 9 confirmés, 4 en partie réglés (verrou, permissions, rétention, formats) et 9 défauts nouveaux.

Les numéros ci-dessous (K pour KYC, G pour GED) renvoient au tableau en fin de fichier.

Les plus graves :

1. **Droits KYC trop larges.** Presque toutes les écritures KYC n'exigent que `KYC:READ` : créer, soumettre ou assigner un dossier, changer le niveau de risque, ajouter une note ou une exigence, déposer une pièce. Un compte qui porte `DOCUMENT:REVIEW` sans `KYC:APPROVE` peut aussi faire approuver un dossier : il approuve les pièces, et le recalcul automatique approuve ensuite le dossier.
2. **Dossier approuvé sans effet.** Le recalcul approuve sans courriel ni création du portefeuille. Activer un client approuve aussi son dossier.
3. **Événements bloqués.** 12 types d'événements KYC n'ont aucun processeur ; ils échouent et finissent abandonnés dans la boîte d'envoi.
4. **Verso de pièce cassé.** Son dépôt échoue si le type n'accepte qu'un document. Une fois approuvé, le verso devient une pièce séparée et peut, à lui seul, faire approuver le dossier.
5. **Opérations par lots fragiles.** Les opérations en masse de la GED, la rétention nocturne et l'approbation automatique KYC passent tout en une transaction : un seul échec annule tout le passage.
6. **Lien de partage public.** `DELETE /shares/{token}` est public, l'anonyme voit le courriel de l'auteur, et aucun fichier n'est jamais servi par le lien.
7. **Écritures avec le droit de lecture.** Plusieurs écritures GED n'exigent que la lecture : métadonnées, étiquettes, signature. Une signature ne vérifie pas son signataire.
8. **Auteur fourni par le client.** L'auteur d'une décision ou d'une note est celui que le corps de la requête déclare (`reviewedBy`, `authorId`), pas le compte connecté.

## Lots proposés

| Lot | Contenu | Écarts |
| --- | --- | --- |
| **KG0. Droits et auteurs** | Règles d'URL KYC selon la méthode : lecture `KYC:READ` ; écritures de dossier (création, soumission, assignation, risque, notes, exigences, dépôt par l'équipe, règles de contrôle croisé) avec un nouveau droit `KYC:MANAGE` donné à `KYC_REVIEWER`, `ADMIN` et `SUPER_ADMIN` ; décisions `KYC:APPROVE` / `KYC:REJECT` sur suffixe exact ; alias `/disapproved` et `/desapproved` traités comme lectures. Décision sur une pièce de l'espace KYC par `/documents/{code}/approve` : exige aussi `KYC:APPROVE`. GED : `PATCH` et `DELETE` sur `/documents/**` exigent `DOCUMENT:WRITE` ; règles d'URL pour `/document-folders`, `/document-tags`, `/document-workflows`, `/document-permissions`, `/spaces`. Droits `DOCUMENT:REVIEW_<ESPACE>` rendus effectifs (revue limitée à l'espace). Auteur toujours tiré du compte connecté (`reviewedBy`, `authorId`, `userId` acceptés mais ignorés). Dossiers restreints respectés au dépôt, sur les étiquettes, métadonnées, signatures, partages, journaux d'accès, tableau de bord, export et file de revue. | K1, K10, K-n1, G2, G3, G5, G12, G-n1 |
| **KG1. Cycle KYC cohérent** | Un seul calcul des exigences (les globales filtrées par type de client, forme juridique et niveau KYC demandé, plus celles du dossier ; niveau KYC modifiable, `PATCH /kyc/cases/{code}/level`) pour le recalcul, la fiche et le portail ; exigences propres à un dossier demandées, téléversables et comptées (section dédiée ci-dessous). Une seule approbation : effets communs (client ou membre actif, portefeuille, courriel `KYC_CASE_APPROVED`) que l'approbation vienne de l'équipe ou du recalcul. Gardes de statut : une décision manuelle (refus, correction, soumission) n'est plus écrasée par le recalcul ; approbation refusée sur un dossier non soumis ; pas de second courriel. Activer un client n'approuve plus son dossier. Recalcul après ajout ou retrait d'une exigence ; exigence retirée ré-ajoutable. Le verso doit être approuvé quand le type en demande un. Liste du portail avec le statut et le code de chaque pièce. | K2, K3, K-n2, K-n6, K-n7, K-n8, K-n9, K-n11 |
| **KG2. Recto, verso et remplacements** | Verso dispensé de la règle « un seul document du type » ; ancien recto ou verso passé `SUPERSEDED` au remplacement (statut enfin utilisé). Synchronisation par document **ou** verso : jamais de pièce créée pour un verso, plus d'appel en double. Synchronisation lancée après validation de la transaction (plus de lecture de données non validées). Suppression depuis le portail : document archivé, nouveau dépôt possible. `KycDocumentResponse.id` toujours l'identifiant court ; `documentCodes` accepté (alias de `documentIds`). | K4, K5, K6, K-n3, K-n10 |
| **KG3. Événements, relances, automatismes** | Tous les événements KYC ont un processeur (notifiants ou simplement consignés) : plus d'événement abandonné. Courriel en échec réessayé par la boîte d'envoi. `KYC_CASE_REJECTED` publié par un vrai refus (voir KG7) ou supprimé ; courriel pour `KYC_RENEWAL_REQUIRED`. Relances d'expiration chaque jour, par seuil franchi (60, 30, 7 jours) et mémorisé, sur les pièces vivantes seulement. Expiration : une seule logique (document et pièce), avec le délai de grâce et le passage en renouvellement. Approbation automatique : une seule règle (`autoApprove` au dépôt, `autoApproveAfterDays` après délai), jamais sur une pièce renvoyée en correction ; une transaction par élément. | K7, K8, K9, K-n4, K-n5, K-n12, G-n3 |
| **KG4. GED : revue, dépôt, documents** | Opérations en masse et rétention nocturne : une transaction par document, résultat partiel exact ; `{space}` filtre vraiment. Remplacement refusé sur un document archivé, expiré, remplacé ou signé ; `autoApprove` respecté. Verrou appliqué aussi aux décisions, étiquettes, métadonnées, signatures ; état du verrou dans `DocumentResponse`. Formats : une seule liste par défaut, contrôle de l'espace sur le type détecté, `upload-config` exact ; import ZIP borné à la taille réelle des requêtes et protégé contre l'archive piégée (taille décompressée limitée). `ownerCode` métier pour contrats et factures ; `ownerCode`, `ownerLabel`, `folderCode`, `folderName` dans la réponse. Fichier supprimé du stockage quand la transaction est annulée. File de revue sans N+1. | G1, G4, G7, G8, G9, G11, G-n2, G-n4, G-n6, G-n8 |
| **KG5. Partage, signature, circuits, permissions** | **Partage interne seulement** : plus aucune route publique (`/shares/**` retiré des chemins publics) ; un lien de partage exige la connexion, respecte les droits et les dossiers restreints, ne sert le fichier qu'à un compte de l'équipe (téléchargement si permis, expiration, compteur) ; révocation par l'auteur ou un administrateur ; courriel de l'auteur jamais exposé. Un document part vers un client par courriel ou par son espace client. **Circuits d'approbation réels** : un circuit inactif, ou d'un autre espace ou type, ne démarre pas ; l'approbateur de chaque étape est contrôlé (compte désigné ou rôle) ; la dernière approbation approuve le document, un refus le refuse ; `autoApproveDays` appliqué par la tâche du cycle de vie ; état du circuit lu sur la dernière instance (plus d'erreur 500). **Permissions par document retirées** : `/document-permissions` supprimé (lecture des anciennes lignes de dossier reprise par les accès de dossier de l'explorateur), tout passe par les dossiers restreints. Signature : le signataire doit être le compte connecté ; dernière signature = document `SIGNED` et événement ; demande refusée sur un document rejeté ou archivé. Politique de rétention réactivable. | G3, G5, G6, G10, G11, G-n5, G-n9 |
| **KG6. Erreurs et messages** | Réponses dédiées : fichier trop lourd `413`, valeur ou énumération invalide `400` (paramètre et valeurs admises nommés), transaction annulée `409` au lieu de `500`. Messages KYC et GED en français. Double contrôle de droit sur la rétention supprimé. | G13, G-n7 |
| **KG7. Améliorations du centre** | Les quatre retenues : tableau de bord KYC ; renouvellements, refus définitif et motifs normalisés ; demande de pièces par WhatsApp et détection des doublons ; historique dans le centre d'audit. Détail plus bas. | |
| **KG8. Doc et tests** | Doc frontend `document/` mise à jour (écarts marqués Réglé, parcours corrigés), routes régénérées ; tests unitaires et d'intégration par lot ; démarrage complet vérifié. | |

Ordre conseillé : KG0 (sécurité), KG1, KG2, KG3, KG4, KG6, KG5, KG7, KG8 (au fil de l'eau).

## Exigences propres à un dossier (demande de l'exploitant du 16/11/2026, lot KG1)

Une exigence ajoutée à un dossier (`POST /kyc/cases/{code}/requirements`) doit être prise en compte partout, comme une exigence globale. Aujourd'hui elle n'apparaît pas dans la liste du portail, le recalcul l'ignore (le dossier peut être approuvé sans elle) et, une fois retirée, elle ne peut plus être remise.

Règle : les exigences d'un dossier sont les exigences globales filtrées par type de titulaire, type de client, forme juridique et niveau KYC demandé, **plus** celles du dossier (une ligne active ajoute ou remplace, une ligne inactive dispense). Un seul calcul (`KycRequirementResolver`) sert à tout.

Critères de validation (testés) :

1. **Demandée** : l'exigence ajoutée figure aussitôt dans la fiche du dossier (`requirements`, pièces manquantes), dans la liste et le taux d'avancement de l'espace client (membre et client), avec son caractère obligatoire ou facultatif.
2. **Téléversable** : la pièce se dépose par l'équipe (`/kyc/documents/admin/upload`) et par le titulaire depuis son espace client, recto et verso si le type le demande ; le dépôt d'un type qui n'est ni exigé ni facultatif pour ce dossier est refusé avec un message clair.
3. **Comptée** : une exigence obligatoire manquante ou non vérifiée empêche l'approbation manuelle et automatique ; le recalcul en tient compte aussitôt après l'ajout ou le retrait.
4. **Dossier déjà approuvé** : l'ajout d'une exigence obligatoire rouvre le dossier (`IN_PROGRESS`, événement consigné) jusqu'au dépôt et à la vérification de la pièce ; une exigence facultative ne le rouvre pas.
5. **Titulaire prévenu** : courriel (et WhatsApp, lot KG7) « pièce demandée » avec le nom de la pièce et le lien vers son espace client.
6. **Retrait** : une exigence retirée disparaît des listes et du calcul ; elle peut être remise ensuite.

## Décisions de l'exploitant (16/11/2026)

| Sujet | Décision |
| --- | --- |
| Liens de partage | **Interne seulement** : plus d'accès public, partage entre comptes de l'équipe ; un document part vers un client par courriel ou par son espace client (règle « pas d'ouverture vers l'extérieur ») |
| Circuits d'approbation de documents | **Les rendre réels** (approbateur contrôlé, statut du document changé, approbation après délai, circuit inactif refusé) |
| Permissions par document | **Accès par dossier seul** : `/document-permissions` retiré, tout passe par les dossiers restreints de l'explorateur |
| Améliorations | **Toutes les quatre retenues** (détail ci-dessous) |

## Améliorations retenues (KG7)

1. **Tableau de bord KYC** : dossiers par statut ; files par ancienneté (moins de 2 jours, 2 à 7, plus de 7) ; délai moyen et médian de traitement (dépôt complet à décision) ; dossiers en retard sur leur échéance (`slaDeadline`) ; charge par agent (dossiers assignés, décidés sur la période) ; dossiers bloqués (en attente de correction depuis plus de 14 jours, sans pièce depuis la création). Route `GET /kyc/dashboard/center`, droit `KYC:READ`.
2. **Renouvellements, refus définitif, motifs normalisés** :
   - calendrier des pièces qui expirent dans les 90 jours, par semaine, avec relance groupée (`POST /kyc/renewals/remind`, `KYC:MANAGE`) ;
   - refus définitif d'un dossier : statut `REJECTED` réellement posé (aujourd'hui jamais), distinct de la demande de correction, avec motif obligatoire, courriel `KYC_CASE_REJECTED` et dossier fermé aux dépôts ; réouverture par un administrateur ;
   - motifs de refus et de correction tirés d'une liste tenue dans le backend (liste de référence modifiable, code et libellé), repris dans le courriel, le portail et le tableau de bord ; texte libre en complément.
3. **Demande de pièces par WhatsApp et détection des doublons** :
   - quand une pièce manque ou est renvoyée en correction, message WhatsApp au titulaire (modèle `kyc_pieces_demandees` à créer chez Infobip et à faire approuver par Meta), en plus du courriel ; lien vers l'espace client ;
   - même numéro de pièce (type et numéro normalisé) sur deux titulaires : signalé sur la fiche et dans le tableau de bord (`duplicateOf`), sans bloquer ; route de liste des doublons.
4. **Historique dans le centre d'audit** : `KycCase`, `KycDocument` (pièce), `Document`, `KycCaseRequirement` ajoutés aux entités principales du journal des actions (avant / après) ; onglet « Historique » de la fiche d'un dossier, d'une pièce et d'un document par la chronologie du centre d'audit (`/admin/audit/timeline?entityCode=...`), doc frontend.

## Tableau des écarts

| N° | Écart | État |
| --- | --- | --- |
| K1 | `/disapproved`, `/desapproved` en `KYC:READ` | confirmé |
| K2 | Approbation par recalcul sans courriel ni portefeuille | Réglé (KG1) |
| K3 | Exigences du dossier et type de client ignorés | Réglé (KG1) |
| K4 | Verso et `multipleAllowed`, recto jamais `SUPERSEDED` | Réglé (KG2) |
| K5 | Verso recréé en pièce distincte | Réglé (KG2) |
| K6 | `KycDocumentResponse.id` incohérent, `documentIds` = codes | Réglé (KG2) |
| K7 | Événements KYC sans processeur, `KYC_CASE_REJECTED` jamais publié, courriels en échec perdus | Réglé (KG3) |
| K8 | Relances d'expiration à date exacte du lundi | Réglé (KG3) |
| K9 | Approbation automatique incohérente | Réglé (KG3) |
| K10 | `reviewedBy`, `authorId` fournis par le client | confirmé (plus large) |
| K-n1 | Écritures KYC en `KYC:READ` ; `DOCUMENT:REVIEW` contourne `KYC:APPROVE` | nouveau |
| K-n2 | Activer un client approuve son dossier | Réglé (KG1) |
| K-n3 | Synchronisation asynchrone dans la transaction de l'appelant | Réglé (KG2) |
| K-n4 | Approbation automatique de pièces renvoyées en correction ; une transaction pour tout le passage | Réglé (KG3) |
| K-n5 | Expiration horaire des documents court-circuite le renouvellement KYC | Réglé (KG3) |
| K-n6 | Verso jamais exigé approuvé | Réglé (KG1) |
| K-n7 | Exigence retirée non ré-ajoutable ; pas de recalcul | Réglé (KG1) |
| K-n8 | Statut de correction écrasé | Réglé (KG1) |
| K-n9 | Liste des pièces du portail sans statut | Réglé (KG1) |
| K-n10 | Suppression au portail bloque un nouveau dépôt | Réglé (KG2) |
| K-n11 | Décisions manuelles écrasées, approbation sans garde | Réglé (KG1) |
| K-n12 | Relances envoyées à des pièces refusées ou remplacées | Réglé (KG3) |
| G1 | Opérations en masse en une transaction ; `{space}` sans effet | confirmé |
| G2 | Droits `DOCUMENT:REVIEW_<ESPACE>` sans effet | confirmé |
| G3 | Écritures en `DOCUMENT:READ` ; signature sans contrôle ni statut `SIGNED` | confirmé |
| G4 | Verrou ignoré par les décisions, étiquettes, métadonnées, signatures ; non exposé | en partie réglé |
| G5 | Permissions par document jamais appliquées ; `/document-permissions` en `ADMIN:ACCESS` | en partie réglé |
| G6 | Lien de partage sans contenu, révocation publique, courriel exposé | confirmé (plus large) |
| G7 | Formats annoncés mais refusés ; limite ZIP inatteignable | en partie réglé |
| G8 | Remplacement d'un document archivé, expiré ou refusé | en partie réglé (verrou et blocage légal contrôlés) |
| G9 | `ownerCode` numérique pour contrats et factures | confirmé |
| G10 | Circuits d'approbation décoratifs ; erreur 500 au deuxième circuit | confirmé |
| G11 | Politique de rétention non réactivable ; rétention en une transaction | en partie réglé (`PURGE_FLAG` utilisé) |
| G12 | `reviewedBy` fourni par le client | confirmé |
| G13 | Messages en anglais ; erreurs de taille et de conversion en 500 | confirmé |
| G-n1 | Dossiers restreints contournés (dépôt, étiquettes, file de revue...) | nouveau |
| G-n2 | Archive ZIP piégée (taille décompressée non bornée) | nouveau |
| G-n3 | `autoApproveAfterDays` presque inutilisé, sans trace de revue | Réglé (KG3) |
| G-n4 | `folderCode`, `folderName` jamais remplis | nouveau |
| G-n5 | Demande de signature sur un document refusé ou archivé | nouveau |
| G-n6 | Fichiers orphelins après annulation | nouveau |
| G-n7 | Double contrôle de droit sur la rétention | nouveau |
| G-n8 | N+1 dans la file de revue | nouveau |
| G-n9 | Plantage possible sur un auteur inconnu (partage, circuits) | nouveau |
