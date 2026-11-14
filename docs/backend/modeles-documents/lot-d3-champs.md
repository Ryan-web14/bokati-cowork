# Lot D3 : champs ajoutés

Branche `feature/modeles-documents`. Migrations : dev `V351__modeles_documents_d3_champs.sql`, prod `V345__modeles_documents_d3_champs.sql`.

## Ce qui change pour l'exploitant

- Un modèle peut recevoir jusqu'à vingt champs de plus que les données de la pièce. Chaque champ a l'un de ces deux modes :
  - `PRINTED_BLANK` : imprimé vide, avec une ligne pointillée à remplir à la main ;
  - `CAPTURED` : saisi dans l'application sur la pièce, puis imprimé à chaque impression et réimpression.
- **Champ obligatoire non saisi :** l'impression est refusée avec la liste des champs à saisir (code `CHAMPS_A_SAISIR`). Seule une pièce dont le modèle a une version publiée est concernée.
- **Champ placé nulle part :** un champ qu'aucun bloc de champs ne place s'imprime dans un bloc « Informations complémentaires ». Ce bloc s'insère avant les signatures.

## Déclaration (dans la version du modèle)

`fields` du brouillon (`PUT /document-templates/{code}/draft`) :

| Clé | Contenu |
| --- | --- |
| `key` | minuscules, chiffres, souligné, 40 caractères au plus, unique dans la version |
| `label` | libellé imprimé, 80 caractères au plus |
| `type` | `TEXT`, `NUMBER`, `AMOUNT`, `DATE`, `LIST`, `CHECKBOX` |
| `mode` | `PRINTED_BLANK` ou `CAPTURED` |
| `required` | saisie exigée avant d'imprimer (`CAPTURED` seulement) |
| `options` | valeurs d'une liste (une à trente) |
| `help` | aide de saisie, 200 caractères au plus |

- **Dans les blocs :** un champ ajouté se désigne par `champ.<key>`, dans un bloc `FIELDS` ou dans un jeton de texte `{{champ.<key>}}`.
- **Versions :** les champs suivent la version. Publier une version qui retire un champ ne supprime pas les valeurs déjà saisies : elles ne s'impriment simplement plus.

## Saisie (routes de la pièce, droits de la pièce)

| Route | Pièce, rattachement des valeurs | Droit |
| --- | --- | --- |
| `GET`, `PUT /cash-registers/movements/{n}/fields` | bon de sortie ou d'entrée, au mouvement | `CASH_MOVEMENT:CREATE` |
| `GET`, `PUT /fund-needs/{n}/fields` | état de besoin | `FUND_REQUEST:CREATE` |
| `GET`, `PUT /fund-requests/{n}/fields?template=FUND_REQUEST\|JUSTIFICATION_SHEET` | demande de fonds ou fiche de justification, à la demande | `FUND_REQUEST:CREATE` |
| `GET`, `PUT /fund-requests/{n}/disbursements/{mouvement}/fields` | bon de décaissement, au mouvement de décaissement | `FUND_REQUEST:CREATE` |

**Lecture (`GET`) :**
- elle renvoie les champs de la version publiée, avec la valeur saisie, son auteur et sa date ;
- elle renvoie aussi `missingRequired`, les clés obligatoires encore vides.

**Saisie (`PUT`) :**
- corps : `{"values": {"cle": "valeur"}}` ; seuls les champs donnés changent, et une valeur vide efface la saisie ;
- contrôle par type :
  - nombre et montant : décimal ; un montant ne peut pas être négatif ;
  - date : au format `AAAA-MM-JJ` ;
  - liste : une des valeurs déclarées ;
  - case à cocher : `true` ou `false` ;
- un champ `PRINTED_BLANK` ou inconnu est refusé ;
- code d'erreur : `CHAMP_INVALIDE`. Sans version publiée : `AUCUN_CHAMP`.

**Impression :**
- date au format `jj/mm/aaaa` ;
- montant groupé par milliers ;
- case à cocher imprimée « Oui » ou « Non ».

**Aperçu :**
- avec les données d'exemple, chaque champ saisi montre une valeur fictive ;
- avec une pièce réelle, il montre les valeurs saisies, sans exiger les champs obligatoires.

## Données

- `document_template_version.fields_json` : les champs de la version.
- `document_field_value` :
  - colonnes : modèle, pièce (`source_code`), clé, valeur, auteur, date ;
  - une seule valeur par modèle, pièce et clé.

## Composants

- `TemplateContent.fieldSpecs` et `FieldSpec` : lecture et contrôle des champs.
- `DocumentTemplatePrinter.addFields` : ajoute les champs aux données et exige les champs obligatoires (`MissingFieldsException`, qui remonte au lieu de basculer sur le gabarit d'origine).
- `DocumentFieldService` : lecture et saisie.
- `DocumentFieldController` : routes de saisie.
- `DocumentData.source` : numéro de la pièce métier auquel les valeurs se rattachent.

## Tests

`DocumentFieldD3IT` (schéma de production), sur le bon de sortie :
- un champ obligatoire non saisi bloque l'impression ;
- une saisie invalide est refusée avec chaque erreur nommée ;
- les valeurs saisies s'impriment : date formatée, bloc « Informations complémentaires », ligne vide du champ à remplir à la main ;
- effacer une valeur rend l'impression de nouveau impossible.
