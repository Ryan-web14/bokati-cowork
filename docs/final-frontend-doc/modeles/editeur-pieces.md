# Éditeur des pièces PDF

> Rédigé d'après le code (`TemplateContent`, `DocumentTemplateService`, 14/11/2026).

## Contenu d'une version

Corps de `PUT /document-templates/{code}/draft` et de `POST .../preview` :

```json
{
  "pageFormat": "A5_TWO_UP",
  "copies": [ { "label": "Original · à conserver par la caisse" }, { "label": "Copie · à remettre au bénéficiaire" } ],
  "style": { "accent": "#c9a84c" },
  "blocks": [ ... ],
  "fields": [ ... ],
  "note": "Ajout du motif"
}
```

| Champ | Règle |
| --- | --- |
| `pageFormat` | `A4`, `A5_TWO_UP` (deux A5 sur une feuille A4, un par exemplaire) ou `A6` |
| `copies` | Exemplaires, 4 au plus ; `label` de 120 caractères au plus, imprimé par le bloc `TITLE` si `copyLabel` |
| `style.accent` | Couleur `#RRGGBB` des filets et titres |
| `blocks` | De 1 à 40 blocs, dans l'ordre d'impression |
| `fields` | Champs ajoutés, 20 au plus, voir [champs-ajoutes.md](champs-ajoutes.md) |
| `note` | Note de version, 500 caractères au plus |

Une option inconnue d'un bloc est refusée (pas ignorée) : l'éditeur n'envoie que les options ci-dessous.

## Blocs

| Type | Options | Ce qui s'imprime |
| --- | --- | --- |
| `HEADER` | `qr` (booléen) | En-tête de l'exploitant (logo, raison sociale, coordonnées lues dans la fiche exploitant) et QR de vérification d'une pièce enregistrée |
| `TITLE` | `title`, `copyLabel` (booléen) | Titre de la pièce, son numéro, et la mention d'exemplaire |
| `FIELDS` | `title`, `columns` (1 à 4), `fields` (liste de clés du catalogue, ou `champ.<clé>` d'un champ ajouté) | Grille libellé / valeur |
| `LINES` | `title`, `table` (clé d'un tableau du catalogue), `columns` (clés de colonnes à garder, toutes par défaut) | Tableau de lignes |
| `TOTALS` | `label`, `words` (booléen), `wordsLabel` | Montant total, et en lettres si `words` |
| `TEXT` | `title`, `text` (2000 caractères au plus, jetons permis), `boxed` (booléen) | Paragraphe |
| `SIGNATURES` | `boxes` (1 à 6), `fromData` (booléen), `width` (80 à 260 px), `height` (60 à 260 px) | Cases de signature |
| `SECTIONS` | `columns` (1 à 4), `exclude` (sections ou champs à retirer) | Toutes les sections de la pièce, dans l'ordre : champs puis tableau. Point de départ des brouillons livrés, à remplacer par des `FIELDS` et `LINES` choisis si besoin |
| `FOOTER` | `text` | Pied de page |

Une case de `SIGNATURES` : `{ "label": "Caissier", "signer": "caissier", "note": "Visa", "stamp": false }`. `label` est obligatoire ; `signer` désigne un signataire du catalogue (nom imprimé) ; `stamp: true` réserve la place du cachet ; `fromData: true` ajoute les signataires que la pièce porte déjà (circuit de validation).

Textes : 150 caractères au plus pour un titre ou un libellé.

## Jetons

Dans un texte ou un libellé, `{{clé}}` est remplacé par la valeur de la pièce :
- les clés du catalogue (`fields`) ;
- `{{champ.<clé>}}` pour un champ ajouté ;
- les jetons communs (`commonTokens` du catalogue), dont `{{exploitant}}`, la raison sociale.

Le texte saisi n'est jamais interprété : seules les clés connues sont remplacées.

## Catalogue

`GET /document-templates/{code}/fields/catalog` : ce que les blocs peuvent désigner pour ce modèle, avec des valeurs d'exemple.

`{ code, fields[{key, label, sample}], tables[{key, title, columns[{key, label, numeric}]}], signers[{key, role}], commonTokens[], blockTypes[], pageFormats[], realPreview, previewNumberHint }`

L'éditeur construit ses listes de choix depuis ce catalogue, jamais depuis une liste écrite en dur.

## Aperçu

`POST /document-templates/{code}/preview?number=` renvoie un PDF (`application/pdf`, à afficher dans un cadre) :
- sans `number` : données d'exemple ;
- avec `number` (si `realPreview` est vrai) : une vraie pièce, par exemple un numéro de mouvement de caisse ou de facture ; `previewNumberHint` dit quel numéro saisir. Rien n'est écrit, aucune pièce n'est modifiée ni numérotée.

Le corps facultatif est le contenu en cours d'édition : l'aperçu montre ce qui n'est pas encore enregistré.
