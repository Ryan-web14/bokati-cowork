# Taxe de l'article et débours (lots C1 et C10)

> Rédigé d'après le code (compléments de facturation, lots C0 à C16, 13/11/2026). Routes exactes et champs JSON : [routes.md](routes.md) et `routes/`.

## Taxe de l'article (C1)

- La règle de taxe de l'article (`taxRuleCode`) pilote la ligne quand l'écran n'envoie pas `vatRate` : exonérée ou hors champ, la ligne n'est pas taxable même si l'écran envoie `taxable: true` ; taux réduit, il s'applique.
- Un `vatRate` envoyé par l'écran reste prioritaire (dérogation, conversion d'une pièce).
- `taxableByDefault: false` sur l'article rend la ligne non taxable quand l'écran n'envoie pas `taxable`.
- La ligne renvoie `taxRuleCode` (règle appliquée) ; le PDF imprime sous le tableau une mention par règle exonérée ou hors champ, avec son motif.
- Plusieurs taux sur une pièce : une ligne de taxe par taux dans `taxes` (« TVA 5 % », « TVA 18 % »).
- La simulation (`/billing/documents/simulate`, `simulate-line`) suit le même calcul : afficher ce qu'elle renvoie.

## Débours (C10)

- Ligne de type `DISBURSEMENT` : frais avancés pour le client et refacturés à l'identique (timbres, frais de dossier, formalités).
- Jamais taxée, hors remise globale (ajoutée au total après les remises), hors chiffre d'affaires (compte de tiers `4718`).
- À l'écran : proposer le type « Débours » dans le choix du type de ligne ; ne pas afficher de TVA sur ces lignes.
