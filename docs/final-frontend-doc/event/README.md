# event : fonctionnement

> Rédigé d'après le code. Routes exactes, droits et champs JSON : [routes.md](routes.md) et `routes/`. Règles communes : [README racine](../README.md).

## En bref

Le module recueille des confirmations de présence à un événement (la « Grande Ouverture » en premier lieu) depuis une page publique, sans compte. Chaque inscription arrive en attente ; le back-office la valide, ce qui crée un **membre** avec accès portail (module `client`), ou la rejette. Deux publics : une page publique (site, réseaux sociaux, QR code) et un écran back-office de traitement des inscriptions. Les événements eux-mêmes ne se gèrent pas par l'API : ils sont posés en base par migration.

## Notions

| Objet | Identifiant exposé | Rôle |
| --- | --- | --- |
| Événement (`Event`) | `code` (ex. `OUVERTURE`), insensible à la casse dans les chemins | Nom, description, date (`eventDate`, facultative), indicateur `active`. Créé uniquement par migration (`src/main/resources/db/migration/V199__event_registration.sql`, renommé par `V200__rename_opening_event.sql`). |
| Inscription (`EventRegistration`) | `id` numérique (dans les chemins du back-office) et `registrationNumber` lisible `REG-<millisecondes>` | Prénom, nom, courriel, téléphone, WhatsApp, statut. Après validation : `memberId` et `customerId` du membre créé. |

L'`id` est un `BIGSERIAL` (petit entier) : il ne dépasse pas `2^53` en pratique, mais le modèle le marque int64 ; le garder en texte comme le demande le [README racine](../README.md#identifiants--attention-aux-grands-nombres) ne coûte rien.

L'événement de l'ouverture est désigné côté serveur par `app.event.opening-code` (défaut `OUVERTURE`) : les routes `/public/opening` sont des raccourcis de `/public/events/OUVERTURE`.

## Statuts et cycle de vie

Inscription (`status`, énumération `RegistrationStatus`) :

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `PENDING_VALIDATION` | Inscription reçue, à traiter | `POST /sni/api/v1/public/events/{eventCode}/registrations` ou `POST /sni/api/v1/public/opening/registrations` | `VALIDATED`, `REJECTED` |
| `VALIDATED` | Acceptée, membre créé | `PATCH /sni/api/v1/event-registrations/{id}/validate` | aucun (état final) |
| `REJECTED` | Refusée | `PATCH /sni/api/v1/event-registrations/{id}/reject` | aucun (état final) ; la même adresse peut se réinscrire |

Événement : pas de statut, seulement `active`. Un événement inactif refuse la lecture publique et l'inscription. Aucune transition automatique.

## Parcours

### Page publique d'inscription (sans jeton)

1. `GET /sni/api/v1/public/opening` (ou `GET /sni/api/v1/public/events/{eventCode}`) : `code`, `name`, `description`, `eventDate`, `active`. Afficher le titre et la description. Un événement inconnu renvoie `404`, un événement inactif `400` (`Event is not open for registration: <code>`) : afficher « inscriptions closes ».
2. Formulaire : `firstname`, `lastname`, `phone`, `email` obligatoires ; `whatsappPhone` facultatif (s'il est vide, le serveur recopie `phone`).
3. `POST /sni/api/v1/public/opening/registrations` (ou `POST /sni/api/v1/public/events/{eventCode}/registrations`) avec l'en-tête `Idempotency-Key` (UUID par clic sur « Envoyer », réutilisé en cas de nouvelle tentative).
4. Réponse `201` : l'inscription, statut `PENDING_VALIDATION`, avec `registrationNumber`. Afficher un remerciement et ce numéro.
5. `409` (`RESOURCE_ALREADY_EXISTS`) avec `Vous êtes déjà inscrit(e) à cet événement avec cet email` si une inscription non rejetée existe déjà pour ce courriel (comparaison sans casse) : afficher ce message tel quel.

### Traiter les inscriptions (back-office)

1. `GET /sni/api/v1/event-registrations?eventCode=OUVERTURE&status=PENDING_VALIDATION&searchText=...&page=0&size=20` ([détail](routes/EventRegistrationController.md#r-get-sni-api-v1-event-registrations)) : page maison `PaginatedResponse`. `searchText` cherche dans prénom, nom, courriel (sans casse), téléphone et numéro d'inscription (avec casse). Tri fixe : plus récentes d'abord ; le paramètre `sort` est ignoré.
2. Détail : `GET /sni/api/v1/event-registrations/{id}`.
3. Valider : `PATCH /sni/api/v1/event-registrations/{id}/validate`, sans corps. Le serveur crée un membre de type `PERSON` avec les nom, prénom, courriel, téléphone et WhatsApp de l'inscription, mot de passe généré, accès portail activé. Réponse : l'inscription `VALIDATED` avec `memberId`, `customerId`, `validatedAt`. Proposer un lien vers la fiche membre (module `client`).
4. Rejeter : `PATCH /sni/api/v1/event-registrations/{id}/reject` avec, facultativement, `{ "reason": "..." }` (255 caractères au plus). Réponse : l'inscription `REJECTED` avec `rejectionReason`.

## Règles et erreurs à refléter dans l'écran

| Situation | Statut | Message |
| --- | --- | --- |
| Champ obligatoire manquant, courriel invalide, longueur dépassée (`firstname`/`lastname` 180, `phone`/`whatsappPhone` 30, `email` 250) | `400` | `VALIDATION_FAILED`, détail dans `errors` |
| Événement inconnu | `404` | `Event not found: <code>` |
| Événement inactif | `400` | `Event is not open for registration: <code>` |
| Courriel déjà inscrit (inscription non rejetée) | `409` | `Vous êtes déjà inscrit(e) à cet événement avec cet email` |
| Valider ou rejeter une inscription déjà traitée | `400` | `This registration has already been VALIDATED` (ou `REJECTED`) |
| Validation alors que le courriel (ou le téléphone) appartient déjà à un compte, un membre ou un client | `400` | message du module membre ; l'inscription reste `PENDING_VALIDATION` (non vérifié : texte exact selon le conflit) |
| Inscription inconnue | `404` | `Registration not found: <id>` |
| Trop d'envois publics depuis la même adresse IP | `429` | `TOO_MANY_REQUESTS` |

Plafond des envois publics : 10 requêtes d'écriture par tranche de 10 minutes, par adresse IP et par chemin (`src/main/java/com/sni/bokaticowork/security/ratelimit/RateLimitRules.java`, seau `public-write`). Une salle d'accueil derrière une seule IP l'atteint vite : prévoir un message clair sur `429`.

## À prévoir côté frontend

- Écrans : page publique d'inscription (une par événement, ou la page « ouverture ») ; liste back-office des inscriptions avec filtres événement, statut et recherche ; détail avec boutons Valider et Rejeter (motif facultatif).
- Droits : `CRM:READ` pour la liste et le détail ; `CRM:CONVERT` pour valider et rejeter. La règle `@PreAuthorize` s'applique aussi à `ROLE_SUPER_ADMIN`.
- Aucune route ne liste les événements : le filtre `eventCode` se remplit avec des codes connus (au minimum `OUVERTURE`). Un code inconnu dans le filtre renvoie `404`, pas une liste vide.
- Afficher Valider et Rejeter seulement pour `PENDING_VALIDATION`.
- La page publique n'envoie aucun jeton ; ne pas y joindre l'en-tête `Authorization`.
- Le courriel est enregistré en minuscules et sans espaces autour : l'afficher tel que renvoyé.

## Effets automatiques

- À chaque inscription publique, après validation de la transaction, deux courriels sont mis en file d'envoi (gabarit `templates/email/event-registration.html`) : un remerciement au participant (« Merci pour votre confirmation · <événement> ») et une alerte à l'équipe (« Nouvelle confirmation · <événement> ») à l'adresse `app.event.notification-email` (défaut `coworkspace@elleaose.com`).
- À la validation, la création du membre déclenche les effets du module membre : courriel de création de compte et lien de définition du mot de passe envoyés après validation de la transaction, initialisation du dossier KYC, contrôle de conformité. Le membre est créé au statut `PENDING`.
- La validation et le rejet sont tracés dans le journal d'audit (module `EVENT_REGISTRATION`, actions `VALIDATE` et `REJECT`).
- Aucun worker, aucun temps réel.

## Écarts relevés

- Aucune route de gestion des événements (création, activation, date) : tout passe par une migration (`src/main/resources/db/migration/V199__event_registration.sql:35-38`).
- La réponse publique d'inscription expose l'`id` technique et des champs internes vides (`memberId`, `customerId`, `rejectionReason`, `validatedAt`) à un visiteur anonyme (`src/main/java/com/sni/bokaticowork/features/event/controller/PublicEventController.java:39-43`).
- `registrationNumber` dérivé de l'horloge (`"REG-" + currentTimeMillis`) : deux inscriptions dans la même milliseconde violent l'unicité (`src/main/java/com/sni/bokaticowork/features/event/model/EventRegistration.java:76`).
- Un événement inexistant passé en filtre de la liste renvoie `404` au lieu d'une liste vide (`src/main/java/com/sni/bokaticowork/features/event/service/implementation/EventRegistrationServiceImpl.java:95-98`).
- Recherche sensible à la casse sur le numéro d'inscription (`src/main/java/com/sni/bokaticowork/features/event/repository/EventRegistrationRepository.java:29`) ; paramètre `sort` ignoré (`EventRegistrationServiceImpl.java:102`).
- Messages du back-office en anglais (`This registration has already been ...`, `Event not found ...`), message public de doublon en français.
- La validation dépend de la création de membre : si elle échoue (courriel déjà utilisé), l'inscription ne peut plus que rester en attente ou être rejetée, sans lien possible vers le compte existant (`EventRegistrationServiceImpl.java:120-130`).
