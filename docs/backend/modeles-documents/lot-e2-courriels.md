# Lot E2 : reprise de tous les courriels

Branche `feature/modeles-documents`. Migrations : dev `V356__modeles_documents_e2_courriels.sql`, prod `V350__modeles_documents_e2_courriels.sql`.

## Ce qui change pour l'exploitant

- **Chaque courriel de la plateforme est un modèle** (famille `EMAIL`) : 64 de plus, en plus des deux du lot E1. Ils sont listés par domaine dans le back-office : facturation, paiement, réservation, abonnements, portefeuille, domiciliation, contrats, documents, sécurité, assistance, tâches, système.
- **Aucune version n'est créée.** Rien ne change à l'envoi tant que l'exploitant n'a pas rédigé un modèle et ne l'a pas publié. Le catalogue de chaque courriel donne les jetons permis, lus dans son gabarit d'origine.
- **Les notifications sont couvertes aussi.** Les courriels envoyés par le circuit des notifications (portefeuille, domiciliation, KYC...) passent par le même moteur de gabarits : un modèle publié pour leur gabarit les remplace sans autre changement.
- **Quatre courriels écrits en Java ont désormais un gabarit**, et sont donc modifiables :

  | Courriel | Gabarit | Code d'envoi |
  | --- | --- | --- |
  | Lien de paiement | `email/payment-link` | `PaymentLinkServiceImpl` |
  | Paiement en attente bientôt expiré | `email/payment-expiry-reminder` | `PaymentIntentExpiryAlertWorker` |
  | Paiement mobile money tranché à la main (équipe) | `email/mobile-money-resolution` | `MobileMoneyResolutionNotifier` |
  | Signature de devis | `email/quote-signature-request` | `QuoteSignatureServiceImpl` |

  Le nom du client et le message personnalisé y sont désormais échappés : avant, le lien de paiement et le rappel d'expiration inséraient le nom tel quel dans le HTML. Sans moteur de gabarits (tests), l'ancien texte sert de repli, nom échappé.

## Courriels de sécurité : jetons obligatoires

Un modèle ne peut pas être publié s'il perd le code ou le lien que le courriel doit porter.

| Modèle | Jeton obligatoire |
| --- | --- |
| `EMAIL_OTT_LOGIN` (code de connexion) | `{{token}}` |
| `EMAIL_EMAIL_CONFIRMATION` (confirmation d'adresse) | `{{token}}` |
| `EMAIL_PASSWORD_RESET` (réinitialisation) | `{{resetLink}}` |
| `EMAIL_GENERATED_PASSWORD` (accès créés) | `{{generatedPassword}}` |
| `EMAIL_CONTRACT_SIGNING_REQUEST` (signature de contrat) | `{{signingUrl}}` |
| `EMAIL_QUOTE_SIGNATURE_REQUEST` (signature de devis) | `{{signatureUrl}}` |
| `EMAIL_PAYMENT_LINK` (lien de paiement) | `{{checkoutUrl}}` |

Les gabarits du code de connexion et de la confirmation d'adresse n'affichaient le code que chiffre par chiffre (`tokenDigits`). Un commentaire Thymeleaf de niveau analyse (`<!--/* ... ${token} */-->`), invisible à l'envoi, déclare désormais `token` (et `code`) comme variables permises.

## Gabarits non inscrits

Sept gabarits de `templates/email` ne sont appelés nulle part (ni code, ni migration, ni autre gabarit) et ne sont pas inscrits : `billing-reminder`, `booking-confirmation-v3`, `booking-reminder-email`, `booking-reminder`, `member-created`, `subscription-expiry-reminder`, `welcome-email-v2`. Leur suppression relève du lot D6.

## Tests

- `EmailTemplatesE2Test` : chaque modèle inscrit a son gabarit ; chaque jeton obligatoire fait partie des variables permises ; migrations dev et prod identiques ; les quatre nouveaux gabarits se rendent et échappent les valeurs.
- `DocumentTemplateE1IT` : la migration passe sur une vraie base avec les lots précédents.
