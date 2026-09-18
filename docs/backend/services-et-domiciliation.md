# Catalogue de services et domiciliation · lot F

Un abonnement devient un contenant : il porte un plan, et un ou plusieurs services. La domiciliation
est le service qui demande le plus de mécanique propre, et trois règles métier la structurent, tenues
par la base autant que par le code. Tout est sous `/sni/api/v1/services` (ADMIN, SUPER_ADMIN, STAFF),
avec une vue en lecture seule pour le domicilié sous `/sni/api/v1/client/domiciliation`.

## Le catalogue

`service_definition` · code, catégorie, mode de délivrance (`CONTINUOUS`, `ON_DEMAND`, `SCHEDULED`,
`METERED`), obligations (contrat, niveau KYC, ressource physique, réglementaire), rythme et
engagement par défaut, prix unitaire, et pour un service à l'acte le code de droit sous lequel chaque
acte est facturé. Huit services seedés, dont `SVC-DOMICILIATION` (contrat, KYC niveau 2) et les trois
actes de courrier (`SVC-MAIL-SCAN`, `SVC-MAIL-FORWARD`, `SVC-MAIL-STORAGE`).

`POST /subscriptions/{n}` souscrit un service. Le niveau KYC est contrôlé avec le nom du service
dans le message. Un service qui exige un contrat ou une ressource reste `PENDING` · c'est le contrat
qui l'active.

## Les trois règles

1. **Trois rythmes** · `MONTHLY`, `QUARTERLY`, `YEARLY`, rien d'autre (contrainte en base).
2. **L'adresse n'est fiscale que sur douze mois d'engagement.** `fiscal_address_eligible` se
   **déduit** de `commitment_months` (et de l'adresse, qui peut ne pas le permettre) ; la base refuse
   `TRUE` sous douze mois. C'est l'engagement qui compte, pas le rythme : mensuel sur douze mois
   d'engagement est fiscal. Passer d'annuel à mensuel retire la qualité, **révoque l'attestation
   fiscale en cours**, prévient le domicilié, et l'administration si
   `bokati.domiciliation.notify-administration-on-termination` le demande.
3. **Le contrat est enregistré et timbré.** Sans enregistrement obtenu, pas d'activation et aucune
   attestation, quelle que soit sa portée.

## Le cycle de vie · un point d'entrée par étape

```
POST /domiciliation/contracts                              DRAFT
POST /domiciliation/contracts/{n}/prepare                  verrou 1 · pièces KYC du représentant, sinon PENDING_DOCUMENTS
                                                            → contrat PDF généré, PENDING_SIGNATURE
POST /domiciliation/contracts/{n}/signed                   {signedDocumentCode} → PENDING_REGISTRATION, démarche ouverte
POST /domiciliation/contracts/{n}/registration/submit      office, droits, qui paie, refacturé
POST /domiciliation/contracts/{n}/registration/confirm     verrou 2 · référence + exemplaire timbré → ACTIVE, service rendu
POST /domiciliation/contracts/{n}/registration/reject      motivé · une nouvelle démarche s'ouvre aussitôt
POST /domiciliation/contracts/{n}/certificate              verrou 3 · {scope: COMMERCIAL|FISCAL}
POST /domiciliation/contracts/{n}/commitment               recalcule la qualité fiscale
POST /domiciliation/contracts/{n}/terminate                préavis, révocation, notification, conservation 10 ans
GET  /domiciliation/registrations/in-progress              le tableau où les dossiers s'enlisent
GET  /domiciliation/registry                               CSV · entrées, sorties, référence d'enregistrement
```

Chaque refus dit ce qu'il faut : « pièces d'identité du représentant légal à vérifier », « l'adresse
n'est fiscale que si l'abonnement est pris pour un an · seule une attestation commerciale peut être
délivrée ». L'attestation ne survit jamais au contrat : sa validité est bornée par `end_date`.

L'enregistrement est suivi à part (`domiciliation_registration`) : c'est une démarche externe, avec
son délai, son coût (`stamp_duty_amount`, `registration_fee_amount`, `paid_by`, `rebilled`) et sa
possibilité d'échec. Les questions de refacturation restent ouvertes par construction.

## Le registre des adresses

`domiciliation_address` · libellé, adresse postale, `fiscal_capable`, `max_occupants`. Une adresse
et un complément distinctif ne désignent qu'un domicilié en cours (index unique partiel). Le registre
des domiciliés est exportable en CSV.

## Le courrier · preuve de remise

`mail_item` + `mail_item_event` (journal immuable par trigger).

- **Réception** notifie aussitôt ; les recommandés et actes le disent dans le sujet.
- **Remise** : un nom suffit pour un pli simple ; recommandé, administratif et acte exigent une
  pièce d'identité · la base le refuse aussi. Le journal garde qui a remis, à qui, contre quoi.
- **Numérisation**, **réexpédition**, **garde prolongée** se facturent à l'acte via le module d'usage,
  au prix du catalogue. Sans prix au catalogue, l'acte est fait et rien n'est facturé · le journal le
  dit.
- Un recommandé ne se détruit pas, il se retourne. Un pli remis ne bouge plus.
- Délai de garde `bokati.domiciliation.mail.storage-days` (30) ; au-delà, relance quotidienne et
  garde facturée par jour.

## Ce qui tourne seul

- Relance des attestations qui expirent dans `bokati.domiciliation.renewal-reminder-days` (30).
- Garde du courrier au-delà du délai.
- Contrats à terme échu sans renouvellement automatique → `EXPIRED`, attestation révoquée.

## À confirmer avec le conseil (paramétrable, jamais codé en dur)

`certificate-validity-days` (365), `retention-years` (10), `notify-administration-on-termination`
(false) et `administration-email`, `kyc-level-required` (2).
