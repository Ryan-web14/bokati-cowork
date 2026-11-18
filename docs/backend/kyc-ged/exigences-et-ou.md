# Exigences KYC en ET / OU

> Ajout au [plan KYC et GED](README.md), demandé par l'exploitant le 17/11/2026, sur `feature/kyc-exigences-groupes`.

## Besoin

On exige une pièce d'identité, mais le titulaire a un passeport. Il doit pouvoir choisir parmi les exigences, et l'équipe ne doit pas être obligée de rendre facultatives la carte et le passeport (ce qui, avant, ouvrait mal les dossiers et les approuvait vides). Le tout en fonction des niveaux KYC.

## Modèle

- `document_requirement.alternative_group` et `alternative_group_label` (et les mêmes colonnes sur `kyc_case_requirement`) : les exigences de même groupe valent **OU**, une pièce suffit ; groupes et exigences seules valent **ET**.
- Un groupe est obligatoire si l'une de ses exigences l'est. Le niveau (`kyc_level`) filtre chaque exigence comme avant : un niveau supérieur peut ajouter un groupe ou une alternative à un groupe.
- Exigences propres au dossier : leur groupe, sinon celui de l'exigence globale du même type (`KycRequirementResolver`).
- Sans aucune exigence obligatoire, l'ensemble forme une seule place : au moins une pièce (règle du 17/11/2026, conservée).

## Calcul

`KycRequirementSlots` (seul calcul, partagé par la fiche, le recalcul automatique et le portail) :

- place remplie : une pièce de la place déposée, verso compris s'il est exigé ;
- place vérifiée : une pièce vérifiée, verso approuvé ;
- refus bloquant : une pièce refusée dont la place n'a ni pièce vérifiée ni autre pièce utilisable (un passeport refusé ne bloque plus si la carte du même groupe est vérifiée) ;
- manquantes : code seul, `CODE_BACK` pour un verso, `CODE_A|CODE_B` pour un groupe vide ;
- `satisfiedByAlternative` sur chaque ligne d'exigence, `required = false` au portail pour une alternative devenue inutile, avancement compté par place.

## Migration

Dev `V372__kyc_exigences_alternatives.sql`, prod `V366`.

## Tests

`KycRequirementSlotsTest` : une pièce du groupe suffit, groupe vide manquant en un seul choix, alternative refusée non bloquante quand l'autre est vérifiée, refus bloquant sans alternative, autre pièce du groupe plus attendue, exigences toutes facultatives.
