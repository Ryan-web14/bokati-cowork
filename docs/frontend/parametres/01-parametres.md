# Paramètres

Le catalogue réunit les 237 réglages lus par le code, répartis en 27 domaines. Une valeur modifiée
passe devant `application.yml`. Sans modification, le fichier fait foi.

## Droits

| Rôle | Accès |
| --- | --- |
| `SUPER_ADMIN` | tous les domaines |
| `ADMIN` | les 13 domaines ouverts ci-dessous, les autres ne lui sont même pas listés |
| `MANAGER` et autres | aucun (`403`) |

**Ouverts à l'`ADMIN`** : Réservation, Ressources et horaires, Abonnements, Passes, Marketplace,
Promotions, Domiciliation, Contrats, Support, Tâches, CRM, Événements, Portail.

**`SUPER_ADMIN` seul** : Entreprises, Portefeuille, Paiement et caisses, Facturation, Conformité,
Documents, Langue et téléphone, Sécurité, Notifications et e-mails, Webhooks, Boîte d'envoi, API Graph,
WebSocket, Adresses et environnement.

## Routes

| Méthode et chemin | Effet |
| --- | --- |
| `GET /admin/settings/domains` | les domaines accessibles, avec leur nombre de réglages |
| `GET /admin/settings?domain=` | les réglages, tous ou d'un domaine |
| `PUT /admin/settings/{key}` `{value, reason?}` | modifie |
| `DELETE /admin/settings/{key}?reason=` | rend le réglage au fichier de configuration |
| `GET /admin/settings/{key}/history` | les 100 derniers changements |

La clé contient des points : `PUT /admin/settings/bokati.marketplace.payment-window-minutes`.

## Réponse

| Champ | Sens |
| --- | --- |
| `key`, `label`, `domain` | la clé, le libellé à afficher et le domaine |
| `type` | `TEXT`, `EMAIL`, `INTEGER`, `DECIMAL`, `BOOLEAN`, `CRON`, `TIME`, `TIME_RANGE` ou `SECRET` |
| `effect` | `LIVE` : aussitôt ; `RESTART` : au prochain redémarrage |
| `value` | la valeur voulue |
| `defaultValue` | celle du fichier, retrouvée après une remise à zéro |
| `modified` | une valeur a été enregistrée depuis l'administration |
| `restartPending` | enregistrée, elle attend le redémarrage : à afficher |
| `editable` | `false` pour un secret |
| `configured` | pour un secret seulement : renseigné ou non sur le serveur |
| `updatedBy`, `updatedAt` | le dernier auteur |

Choisir le champ selon `type` : interrupteur pour `BOOLEAN`, plage `HH:mm-HH:mm` pour `TIME_RANGE`
(vide : fermé), champ numérique pour `INTEGER` et `DECIMAL`.

## Règles

- Une valeur invalide répond `400` avec un message prêt à afficher. Elle est contrôlée selon son type :
  une planification doit être un cron valide, une cadence `...delay-ms` vaut au moins 1 000 ms.
- Une valeur ne peut pas contenir `${`.
- Les secrets (clés JWT, mots de passe STOMP, clés de stockage) ne sont jamais renvoyés et ne se
  modifient pas ici : ils restent dans la configuration du serveur.
- Chaque changement et chaque remise à zéro écrit l'ancienne valeur, la nouvelle, l'auteur et le motif
  dans le journal, consultable aussi par `GET /admin/settings-audit-logs?settingKey=`.

## Prise d'effet

Pris en compte aussitôt (`LIVE`) :

| Réglage | Clé | Effet |
| --- | --- | --- |
| Délai de paiement d'une commande | `bokati.marketplace.payment-window-minutes` | commandes validées ensuite |
| Horaires globaux | `bokati.resource.availability.generation.*-hours` | créneaux générés ensuite |
| Rétroaction d'une réservation de l'équipe | `bokati.booking.staff.max-backdate-days` | réservations suivantes |
| Délai de validation d'une dépense | `bokati.company.wallet.approval-hours` | dépenses suivantes, pas celles en attente |
| Fermeture automatique du support | `bokati.support.auto-close-delay-days` | prochain passage, au plus une heure après |

Tous les autres sont lus au démarrage (`RESTART`) : un redémarrage de l'application les applique.
