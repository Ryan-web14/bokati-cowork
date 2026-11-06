# Contrats et domiciliation de l'organisation

Préfixe des routes : `/sni/api/v1`.

Les contrats et la domiciliation d'une organisation lisent sa fiche (dénomination, forme
juridique, RCCM, NIU, représentant légal, adresse) au lieu de la faire ressaisir.

## Signataire désigné

Signent pour l'organisation : son représentant légal (fiche), et les membres rattachés dont le
lien porte `canSign` (`PATCH /customers/{customerId}/members/{linkId}` `{ canSign, canCollectMail }`).

- `POST /contracts/{code}/request-signing` : pour un contrat d'organisation, `signerEmail` et
  `signerName` deviennent facultatifs ; sans eux, le premier signataire désigné ayant une adresse
  est retenu. Donnés, ils doivent désigner l'un d'eux (adresse d'un membre autorisé, ou nom du
  représentant), sinon 400. Pour un particulier, ils restent requis.
- Le contrat d'abonnement d'une organisation est signé au nom de son représentant légal.
- Les modèles reçoivent `clientRccm`, `clientNiu`, `clientLegalForm`, `clientAddress`,
  `clientRepresentative`, `clientRepresentativeTitle` (et `ownerNiu`, `ownerLegalForm`,
  `ownerRepresentative` dans le contexte).
- Un collaborateur qui reçoit une place devient partie `BENEFICIARY` du contrat de l'abonnement,
  sans signer ; il en sort quand sa place est libérée.

| Qui | Méthode et chemin | Effet |
| --- | --- | --- |
| Administration | `GET /companies/{customerId}/contracts` | signataires désignés, contrats (parties, avenants), domiciliations |
| Administration | `GET /companies/{customerId}/contracts/{code}/pdf` | le contrat signé, ou à défaut généré |
| Gérant | `GET /client/companies/{customerId}/contracts` (et `.../{code}/pdf`) | comme l'administration |

## Domiciliation

- L'organisation domiciliée est le souscripteur (`CUSTOMER`). À l'ouverture, sa fiche fait foi :
  `legalName`, `legalForm`, `registrationNumber`, `taxNumber`, `legalRepresentativeName` de la
  demande deviennent facultatifs et ne servent qu'à compléter une fiche vide (ils y sont reportés).
- Une modification de la fiche se reporte sur les domiciliations en cours ; le contrat généré et
  l'attestation suivante l'impriment (la fiche est relue avant chaque document).
- Le gérant et les membres au drapeau `canCollectMail` voient la domiciliation et son courrier dans
  le portail ; la fin du lien leur retire l'accès.
- Retrait d'un pli (`POST /services/domiciliation/mail/{item}/collect`) : `collectorMemberId`
  facultatif ; pour une organisation, le membre doit être son gérant ou autorisé à retirer le
  courrier, et son nom tient lieu de `collectedBy`. Le pli garde `collectedByMember`.

| Qui | Méthode et chemin | Effet |
| --- | --- | --- |
| Membre | `GET /client/domiciliation/contracts` | à son nom, et celles des organisations dont il voit le courrier |
| Gérant ou membre autorisé | `GET /client/companies/{customerId}/domiciliation` | les domiciliations de l'organisation |
| Gérant ou membre autorisé | `GET /client/domiciliation/contracts/{n}/document`, `/certificate` | contrat et attestation en vigueur (PDF) |
| Gérant ou membre autorisé | `GET /client/domiciliation/contracts/{n}/mail` | le courrier |
