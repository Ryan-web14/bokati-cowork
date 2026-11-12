# Correctif : plusieurs courriels de facturation, NIU commençant par M

Correctif de production, branche `hotfix/emails-facturation-niu` partie de `new-main`.

## Ce qui change

| Sujet | Avant | Après |
| --- | --- | --- |
| Courriel de facturation d'un client ou d'une entreprise | Une seule adresse (`@Email`, 150 caractères) | Une ou plusieurs adresses séparées par des virgules (ou des points-virgules), 500 caractères au plus ; normalisée en minuscules, séparée par `, `, sans doublon |
| Envoi | Un destinataire Graph avec la chaîne entière : refusé dès qu'il y avait une virgule | `DefaultEmailSender.message` découpe la chaîne : un destinataire par adresse. Vaut pour tous les courriels (factures, relances, relevés, notifications) |
| NIU | `^[PE][0-9]{15}$` | `^[PEM][0-9]{15}$` ; message d'erreur mis à jour |

## Code

| Élément | Rôle |
| --- | --- |
| `core/utils/validation/EmailAddresses` | Découper, valider, normaliser une liste ; `first` pour un usage à adresse unique |
| `core/utils/validation/EmailList` | Contrainte de validation pour un champ liste |
| `CustomerRequest.billingEmail`, `CompanyDtos` (création, `UpdateBillingRequest`) | `@Email` remplacé par `@EmailList` |
| `CustomerServiceImpl`, `CustomerMapperDecorator` (création et mise à jour), `CompanyAccountService` | Validation et normalisation de la liste |
| `DefaultEmailSender` | Plusieurs destinataires |
| `ValidationUtils.NIU_PATTERN`, `OrganizationIdentifiers` | Lettre M acceptée |

## Migration

Prod `V279_1__courriels_facturation_multiples.sql`, dev `V283_1` sur `new-main` et `V289` sur `feature/reservation-places` (contenu identique, idempotent) :

- `customer.billing_email` passe à `VARCHAR(500)` ;
- `billing_document.customer_email` passe à `VARCHAR(500)` ;
- `email_delivery_log.recipient_email` passe à `VARCHAR(500)`.

Élargir un VARCHAR ne réécrit pas la table : la migration est instantanée.

Le numéro décimal `279_1` (version 279.1) s'intercale entre la dernière migration de prod (`V279`) et celles de la branche de fonctionnalité en cours (`V280` et suivantes), qui restent dans l'ordre à leur déploiement.

## Membres

Étendu aux membres : `CreateMemberRequest.billingEmail` (`@EmailList`, 500), validation et normalisation dans `MemberServiceImpl` ; réglages du membre (`BillingPreferences.billingEmail`, création et mise à jour dans `MemberSettingsMapper` et `MemberSettingsServiceImpl`). Le courriel de facturation d'un membre est porté par sa fiche client (`customer.billing_email`, déjà élargie) et par ses réglages (`user_settings.billing_email`, élargie par `V279_2` en prod, `V283_2` en dev sur `new-main`).

## Tests

`EmailAddressesAndNiuTest` : découpage, doublons, validation, normalisation, contrainte, NIU avec P, E, M.
