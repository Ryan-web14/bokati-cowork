# Centre KYC et GED : plan d'amélioration et règlement des écarts

Branche `feature/kyc-ged`, partie de new-main 68407979 le 16/11/2026. Plan seulement : rien n'est implémenté.

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
| **KG1. Cycle KYC cohérent** | Un seul calcul des exigences (celles du dossier, sinon les globales filtrées par type de client et forme juridique) pour le recalcul, la fiche et le portail. Une seule approbation : effets communs (client ou membre actif, portefeuille, courriel `KYC_CASE_APPROVED`) que l'approbation vienne de l'équipe ou du recalcul. Gardes de statut : une décision manuelle (refus, correction, soumission) n'est plus écrasée par le recalcul ; approbation refusée sur un dossier non soumis ; pas de second courriel. Activer un client n'approuve plus son dossier. Recalcul après ajout ou retrait d'une exigence ; exigence retirée ré-ajoutable. Le verso doit être approuvé quand le type en demande un. Liste du portail avec le statut et le code de chaque pièce. | K2, K3, K-n2, K-n6, K-n7, K-n8, K-n9, K-n11 |
| **KG2. Recto, verso et remplacements** | Verso dispensé de la règle « un seul document du type » ; ancien recto ou verso passé `SUPERSEDED` au remplacement (statut enfin utilisé). Synchronisation par document **ou** verso : jamais de pièce créée pour un verso, plus d'appel en double. Synchronisation lancée après validation de la transaction (plus de lecture de données non validées). Suppression depuis le portail : document archivé, nouveau dépôt possible. `KycDocumentResponse.id` toujours l'identifiant court ; `documentCodes` accepté (alias de `documentIds`). | K4, K5, K6, K-n3, K-n10 |
| **KG3. Événements, relances, automatismes** | Tous les événements KYC ont un processeur (notifiants ou simplement consignés) : plus d'événement abandonné. Courriel en échec réessayé par la boîte d'envoi. `KYC_CASE_REJECTED` publié par un vrai refus (voir KG7) ou supprimé ; courriel pour `KYC_RENEWAL_REQUIRED`. Relances d'expiration chaque jour, par seuil franchi (60, 30, 7 jours) et mémorisé, sur les pièces vivantes seulement. Expiration : une seule logique (document et pièce), avec le délai de grâce et le passage en renouvellement. Approbation automatique : une seule règle (`autoApprove` au dépôt, `autoApproveAfterDays` après délai), jamais sur une pièce renvoyée en correction ; une transaction par élément. | K7, K8, K9, K-n4, K-n5, K-n12, G-n3 |
| **KG4. GED : revue, dépôt, documents** | Opérations en masse et rétention nocturne : une transaction par document, résultat partiel exact ; `{space}` filtre vraiment. Remplacement refusé sur un document archivé, expiré, remplacé ou signé ; `autoApprove` respecté. Verrou appliqué aussi aux décisions, étiquettes, métadonnées, signatures ; état du verrou dans `DocumentResponse`. Formats : une seule liste par défaut, contrôle de l'espace sur le type détecté, `upload-config` exact ; import ZIP borné à la taille réelle des requêtes et protégé contre l'archive piégée (taille décompressée limitée). `ownerCode` métier pour contrats et factures ; `ownerCode`, `ownerLabel`, `folderCode`, `folderName` dans la réponse. Fichier supprimé du stockage quand la transaction est annulée. File de revue sans N+1. | G1, G4, G7, G8, G9, G11, G-n2, G-n4, G-n6, G-n8 |
| **KG5. Partage, signature, circuits, permissions** | Partage : voir la décision 1. Signature : le signataire doit être le compte connecté ; dernière signature = document `SIGNED` et événement ; demande refusée sur un document rejeté ou archivé. Circuits d'approbation : voir la décision 2. Permissions par document : voir la décision 3. Politique de rétention réactivable. | G3, G5, G6, G10, G11, G-n5, G-n9 |
| **KG6. Erreurs et messages** | Réponses dédiées : fichier trop lourd `413`, valeur ou énumération invalide `400` (paramètre et valeurs admises nommés), transaction annulée `409` au lieu de `500`. Messages KYC et GED en français. Double contrôle de droit sur la rétention supprimé. | G13, G-n7 |
| **KG7. Améliorations du centre** | Voir la décision 4 : à choisir. | |
| **KG8. Doc et tests** | Doc frontend `document/` mise à jour (écarts marqués Réglé, parcours corrigés), routes régénérées ; tests unitaires et d'intégration par lot ; démarrage complet vérifié. | |

Ordre conseillé : KG0 (sécurité), KG1, KG2, KG3, KG4, KG6, KG5, KG7, KG8 (au fil de l'eau).

## Décisions à prendre

1. **Liens de partage.** Aujourd'hui ils ne servent aucun fichier. Choix : (a) servir le fichier par le lien (jeton, expiration, mot de passe en en-tête, téléchargement si permis, compteur), ce qui ouvre un accès externe aux documents ; (b) **conseillé** au vu de la règle « pas d'ouverture vers l'extérieur » : supprimer l'accès public, garder le partage entre comptes de l'équipe (lien interne qui exige la connexion), et envoyer un document à un client par courriel ou par son espace client.
2. **Circuits d'approbation** (`/document-workflows`). Aujourd'hui décoratifs. Choix : (a) les rendre réels (approbateur contrôlé, statut du document changé, délai d'approbation automatique) ; (b) les retirer et garder la revue simple par espace. Conseil : (b) si personne ne s'en sert, sinon (a).
3. **Permissions par document** (`/document-permissions`). Aujourd'hui seules les permissions de dossier (dossiers restreints) sont appliquées. Conseil : retirer les permissions par document et tout faire par les accès de dossier, déjà en place dans l'explorateur.
4. **Améliorations (KG7)**, à retenir ou non :
   - a. Tableau de bord KYC : files par ancienneté, délai de traitement, échéances, charge par agent, dossiers bloqués.
   - b. Calendrier des renouvellements : pièces qui expirent dans 90 jours, relance groupée.
   - c. Motifs de refus et de correction normalisés (liste), repris dans le courriel et le portail.
   - d. Refus définitif d'un dossier (statut `REJECTED` réel, aujourd'hui jamais posé) distinct de la demande de correction.
   - e. Pièces demandées au client par WhatsApp (modèle à faire approuver) en plus du courriel.
   - f. Détection de doublons : même numéro de pièce sur deux titulaires.
   - g. Avant / après des dossiers KYC, pièces et documents dans le journal des actions (centre d'audit, lot A1) et onglet « Historique » de la fiche.

## Tableau des écarts

| N° | Écart | État |
| --- | --- | --- |
| K1 | `/disapproved`, `/desapproved` en `KYC:READ` | confirmé |
| K2 | Approbation par recalcul sans courriel ni portefeuille | confirmé |
| K3 | Exigences du dossier et type de client ignorés | confirmé (plus large) |
| K4 | Verso et `multipleAllowed`, recto jamais `SUPERSEDED` | confirmé |
| K5 | Verso recréé en pièce distincte | confirmé (et peut approuver seul) |
| K6 | `KycDocumentResponse.id` incohérent, `documentIds` = codes | confirmé |
| K7 | Événements KYC sans processeur, `KYC_CASE_REJECTED` jamais publié, courriels en échec perdus | confirmé |
| K8 | Relances d'expiration à date exacte du lundi | confirmé |
| K9 | Approbation automatique incohérente | confirmé |
| K10 | `reviewedBy`, `authorId` fournis par le client | confirmé (plus large) |
| K-n1 | Écritures KYC en `KYC:READ` ; `DOCUMENT:REVIEW` contourne `KYC:APPROVE` | nouveau |
| K-n2 | Activer un client approuve son dossier | nouveau |
| K-n3 | Synchronisation asynchrone dans la transaction de l'appelant | nouveau |
| K-n4 | Approbation automatique de pièces renvoyées en correction ; une transaction pour tout le passage | nouveau |
| K-n5 | Expiration horaire des documents court-circuite le renouvellement KYC | nouveau |
| K-n6 | Verso jamais exigé approuvé | nouveau |
| K-n7 | Exigence retirée non ré-ajoutable ; pas de recalcul | nouveau |
| K-n8 | Statut de correction écrasé | nouveau |
| K-n9 | Liste des pièces du portail sans statut | nouveau |
| K-n10 | Suppression au portail bloque un nouveau dépôt | nouveau |
| K-n11 | Décisions manuelles écrasées, approbation sans garde | nouveau |
| K-n12 | Relances envoyées à des pièces refusées ou remplacées | nouveau |
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
| G-n3 | `autoApproveAfterDays` presque inutilisé, sans trace de revue | nouveau |
| G-n4 | `folderCode`, `folderName` jamais remplis | nouveau |
| G-n5 | Demande de signature sur un document refusé ou archivé | nouveau |
| G-n6 | Fichiers orphelins après annulation | nouveau |
| G-n7 | Double contrôle de droit sur la rétention | nouveau |
| G-n8 | N+1 dans la file de revue | nouveau |
| G-n9 | Plantage possible sur un auteur inconnu (partage, circuits) | nouveau |
