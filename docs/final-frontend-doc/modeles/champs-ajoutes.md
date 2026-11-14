# Champs ajoutés (lot D3)

> Rédigé d'après le code (`TemplateContent`, `DocumentFieldService`, `DocumentFieldController`, 14/11/2026).

Un modèle de pièce peut déclarer des champs qui n'existent pas dans l'application : une zone à remplir à la main sur le papier, ou une valeur saisie dans l'application avant l'impression.

## Déclaration dans le modèle

Dans `fields` du contenu (20 au plus) :

```json
{ "key": "code_projet", "label": "Code projet", "type": "LIST", "mode": "CAPTURED", "required": true,
  "options": ["Kintélé", "Siège"], "help": "Projet qui porte la dépense" }
```

| Option | Règle |
| --- | --- |
| `key` | Minuscules, chiffres, souligné, commence par une lettre, 40 caractères au plus, unique dans le modèle |
| `label` | Obligatoire, 80 caractères au plus |
| `type` | `TEXT`, `NUMBER`, `AMOUNT`, `DATE`, `LIST` ou `CHECKBOX` |
| `mode` | `PRINTED_BLANK` (zone vide imprimée, remplie à la main) ou `CAPTURED` (saisi dans l'application) |
| `required` | Pour `CAPTURED` : l'impression est refusée tant qu'il n'est pas saisi |
| `options` | Pour `LIST` : de 1 à 30 valeurs |
| `help` | Aide affichée à la saisie, 200 caractères au plus |

Placement à l'impression : un champ cité dans un bloc (`FIELDS` avec `champ.<clé>`, ou `{{champ.<clé>}}` dans un texte) s'imprime à cet endroit. Les autres s'impriment dans un bloc « Informations complémentaires » ajouté automatiquement.

## Saisie sur une pièce

Les champs suivent la version **publiée** du modèle de la pièce.

| Pièce | Lire | Enregistrer |
| --- | --- | --- |
| Mouvement de caisse (bon d'entrée ou de sortie) | `GET /cash-registers/movements/{number}/fields` | `PUT` même chemin |
| État de besoin | `GET /fund-needs/{number}/fields` | `PUT` même chemin |
| Demande de fonds, ou fiche de justification (`?template=JUSTIFICATION_SHEET`) | `GET /fund-requests/{number}/fields` | `PUT` même chemin |
| Bon de décaissement | `GET /fund-requests/{number}/disbursements/{movement}/fields` | `PUT` même chemin |

- Les droits sont ceux de la pièce (caisse, fonds), pas `SYSTEM:DOCUMENT_TEMPLATES`.
- Réponse : `{ templateCode, publishedVersion, sourceCode, fields[{ key, blockKey, label, type, mode, required, options, help, value, enteredBy, enteredAt }], missingRequired[] }`.
- `PUT` : `{ "values": { "code_projet": "Kintélé", "visa_dg": true } }`. Une valeur vide ou nulle efface le champ. Chaque valeur est contrôlée selon son type (nombre, montant, date `AAAA-MM-JJ`, valeur de la liste, case cochée) : sinon `400 CHAMP_INVALIDE` avec le détail.
- Modèle sans version publiée ou sans champ saisi : `400 AUCUN_CHAMP`.

## Impression refusée

Si un champ obligatoire saisi n'a pas de valeur, l'impression de la pièce répond `400` avec le code `CHAMPS_A_SAISIR` et le message « Champs à saisir avant d'imprimer la pièce X : Libellé 1, Libellé 2 ». L'écran ouvre alors la saisie des champs de la pièce, puis relance l'impression.

Écran conseillé : sur la fiche d'une pièce, un encart « Informations complémentaires » qui liste `fields`, signale `missingRequired`, et enregistre en un `PUT`.
