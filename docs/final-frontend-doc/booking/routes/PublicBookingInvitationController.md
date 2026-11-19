# PublicBookingInvitationController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [booking](../README.md) · 1 route(s).

## Routes

<a id="r-get-sni-api-v1-public-bookings-invitations-token"></a>
### GET /sni/api/v1/public/bookings/invitations/{token}

- Accès : public, sans jeton
- Réponse : `ModelAndView`
- Code : `src/main/java/com/sni/bokaticowork/features/booking/controller/PublicBookingInvitationController.java`, méthode `answer`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `token` | chemin | `String` | oui |  |
| `answer` | requête | `String` |  | ACCEPT |
