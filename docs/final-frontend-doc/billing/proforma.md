# billing : facture proforma (lot B5)

> Retour au [README du module](README.md). Toutes les routes sont sous `/sni/api/v1/billing`.

Une proforma est un **document sans valeur fiscale** : jamais certifiée, jamais comptabilisée, jamais une créance (elle ne compte ni dans l'encours, ni dans les relances, ni dans les retards, ni dans `customerImpact`). Elle suit le circuit proforma, devis accepté, facture (décision D2).

## Créer

- Directement : `POST /proformas` (même corps que `POST /documents`, le type est forcé à `PROFORMA_INVOICE`).
- Depuis un devis : `POST /quotes/{quoteNumber}/proforma` (mêmes lignes, remises, clauses ; date de validité reprise ; `sourceType = QUOTE`).

Émise (`PATCH /documents/{n}/issue`) ou envoyée, elle passe à `SENT`. Passée sa `dueDate`, elle passe à `EXPIRED`.

## Modifier (décision D9)

`PUT /documents/{n}` modifie une proforma **quel que soit son statut**, même transformée : chaque modification archive une version (auteur, date) ; le devis issu d'elle n'est pas touché. Une proforma expirée dont on repousse la date redevient `SENT`.

## Transformer en devis

`POST /proformas/{proformaNumber}/to-quote` : un devis est créé avec les mêmes lignes (`sourceType = PROFORMA`), émis puis **accepté** ; la proforma passe à `CONVERTED` et porte `convertedToDocumentNumber`. Une seule transformation par proforma. Le devis suit ensuite son propre cycle (acomptes, conversion en facture).

## Paiement reçu sur une proforma

Un paiement imputé sur une proforma (par exemple un client qui paie le lien de la proforma) devient un **acompte du devis** issu d'elle : le devis est créé et accepté s'il ne l'est pas encore, un acompte du montant payé est facturé, et le paiement s'impute sur cette facture d'acompte. L'imputation renvoyée par le serveur porte le numéro de la facture d'acompte, pas celui de la proforma.

Exemple : proforma de 1 000 000 payée à 40 % ; elle devient un devis accepté avec un acompte de 400 000 encaissé, puis une facture qui le déduit.

## PDF

Titre « FACTURE PROFORMA », mention « Document sans valeur fiscale » sous le numéro.

## Ce qui est refusé

Certifier (`validate`), encaisser directement (`ensureCanPay`), créer un avoir ou une rectificative sur une proforma, imputer un avoir sur une proforma.
