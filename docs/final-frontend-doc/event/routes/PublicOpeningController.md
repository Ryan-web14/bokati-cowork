# PublicOpeningController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [event](../README.md) · 2 route(s).

## Routes

<a id="r-get-sni-api-v1-public-opening"></a>
### GET /sni/api/v1/public/opening

- Accès : public, sans jeton
- Réponse : `EventRegistrationDtos.EventSummaryResponse`
- Modèles : [EventRegistrationDtos.EventSummaryResponse](#m-EventRegistrationDtos-EventSummaryResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/event/controller/PublicOpeningController.java`, méthode `getOpeningEvent`

<a id="r-post-sni-api-v1-public-opening-registrations"></a>
### POST /sni/api/v1/public/opening/registrations

- Accès : public, sans jeton
- En-tête `Idempotency-Key` : conseillé (opération `OPENING_REGISTRATION_CREATE`, voir le README racine)
- Corps JSON (obligatoire) : `EventRegistrationDtos.PublicEventRegistrationRequest`
- Réponse : `EventRegistrationDtos.EventRegistrationResponse`
- Modèles : [EventRegistrationDtos.EventRegistrationResponse](#m-EventRegistrationDtos-EventRegistrationResponse), [EventRegistrationDtos.PublicEventRegistrationRequest](#m-EventRegistrationDtos-PublicEventRegistrationRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/event/controller/PublicOpeningController.java`, méthode `register`

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-EventRegistrationDtos-EventSummaryResponse"></a>
#### EventRegistrationDtos.EventSummaryResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `eventDate` | date AAAA-MM-JJ |  |  |
| `active` | booléen |  |  |

<a id="m-EventRegistrationDtos-EventRegistrationResponse"></a>
#### EventRegistrationDtos.EventRegistrationResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `registrationNumber` | texte |  |  |
| `eventCode` | texte |  |  |
| `eventName` | texte |  |  |
| `firstname` | texte |  |  |
| `lastname` | texte |  |  |
| `fullName` | texte |  |  |
| `email` | texte |  |  |
| `phone` | texte |  |  |
| `whatsappPhone` | texte |  |  |
| `status` | enum : `PENDING_VALIDATION`, `VALIDATED`, `REJECTED` |  |  |
| `rejectionReason` | texte |  |  |
| `memberId` | texte |  |  |
| `customerId` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `validatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-EventRegistrationDtos-PublicEventRegistrationRequest"></a>
#### EventRegistrationDtos.PublicEventRegistrationRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `firstname` | texte | oui | longueur min `0`, longueur max `180` |
| `lastname` | texte | oui | longueur min `0`, longueur max `180` |
| `phone` | texte | oui | longueur min `0`, longueur max `30` |
| `whatsappPhone` | texte |  | longueur min `0`, longueur max `30` |
| `email` | string (email) | oui | longueur min `0`, longueur max `250` |
