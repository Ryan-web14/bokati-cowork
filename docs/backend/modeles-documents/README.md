# Modèles de documents stockés en base

Chantier demandé par l'exploitant : toutes les pièces produites par l'application (bons de caisse, états de besoin, demandes de fonds, factures, reçus...) et tous les courriels deviennent des modèles enregistrés en base, modifiables et prévisualisables depuis le frontend. Les gabarits Thymeleaf actuels restent en place comme repli : sans version publiée en base, la pièce ou le courriel sort comme aujourd'hui.

- **Champs :** on peut ajouter des champs, soit imprimés vides et remplis à la main, soit saisis dans l'application et imprimés.
- **Mise en page commune :** en-tête et pied de page de la facture, cases de signature et de cachet configurables, montant en lettres.
- **Petits bons :** deux exemplaires par feuille A4, avec les mentions « Original » et « Copie ».

Maquettes validées avec l'exploitant : bon de sortie de caisse, bon d'entrée de caisse, demande de fonds, état de besoin. Les cases de signature mesurent environ 4,2 x 4,3 cm et sont centrées sur leur rangée.

État des lieux : [inventaire.md](inventaire.md).

## Constat (résumé de l'inventaire)

- **Identité :** trois noms de marque (ELLE A OSE, Bokati Cowork, Elle A Osé) et quatre styles d'en-tête (facture, caisse et portefeuille, pièces de fonds, contrats). L'identité de l'exploitant est écrite en dur dans une vingtaine de gabarits. La fiche exploitant (`business`, `operator = true`) n'est lue que par les contrats. Les colonnes `seller_*` des factures existent mais ne sont jamais écrites.
- **Rendu PDF :** la même chaîne jsoup, W3CDom puis OpenHtmlToPDF est copiée dans 14 services. Le QR est généré de cinq façons.
- **Défauts relevés :**
  - rapport de clôture de caisse qui lit une variable `session` jamais fournie (supposé) ;
  - QR du tableau d'emprunt qui mène à une erreur 404 ;
  - bordereau de remise en banque sans PDF ;
  - reçu marqué « scellé » sans l'être ;
  - logo invisible sur le reçu ;
  - titres sans accents ;
  - six gabarits jamais utilisés.
- **Base existante :** deux mécanismes de modèles en base existent déjà, `contract_template` (contrats, jetons `{{variable}}`) et `notification_template` (courriels).

## Principe retenu

Un modèle n'est pas du HTML libre. Laisser l'exploitant écrire du Thymeleaf exposerait le serveur à l'exécution d'expressions. Un modèle est une **suite de blocs** configurés en JSON, et chaque bloc est rendu par un fragment fixe du code. Le frontend édite ces blocs, et l'aperçu montre le PDF réel.

| Bloc | Contenu configurable |
| --- | --- |
| `HEADER` | en-tête commun de la facture (logo, identité de l'exploitant, filet doré) ; QR de vérification oui / non |
| `TITLE` | titre, numéro, mention d'exemplaire |
| `FIELDS` | grille de champs : champs de données du type de pièce et champs ajoutés, nombre de colonnes |
| `LINES` | tableau des lignes : colonnes affichées, libellés, largeurs |
| `TOTALS` | total en chiffres et en lettres dans un encadré, devise |
| `TEXT` | paragraphe libre (mentions, engagement), avec jetons `{{champ}}` |
| `SIGNATURES` | cases : libellé, nom prérempli ou vide, case de cachet, taille, alignement |
| `FOOTER` | pied commun (numéro, vérification, mentions légales) |

Mise en page d'un modèle :
- `A4` : pleine page ;
- `A5_TWO_UP` : deux exemplaires sur une A4, avec la mention de chaque exemplaire (par exemple « Original, à conserver par la caisse » et « Copie, à remettre au bénéficiaire ») ;
- `A6` : badges.

Le nombre d'exemplaires et leurs mentions sont configurables. « DUPLICATA » s'imprime à la réimpression.

## Repli sur les gabarits actuels

- Chaque modèle en base désigne son gabarit Thymeleaf d'origine (`fallback_template`, par exemple `fund/voucher` ou `email/billing-document`).
- Au rendu :
  - s'il existe une version publiée, elle est utilisée ;
  - sinon, ou si son rendu échoue (bloc invalide, champ manquant), le gabarit d'origine sert, et l'échec est journalisé.
- Un modèle peut être « remis au gabarit d'origine » en archivant sa version publiée.
- Les gabarits Thymeleaf ne sont donc supprimés qu'en fin de chantier, et seulement ceux qui ne servent à rien (gabarits morts de l'inventaire).

## Courriels

Les courriels suivent le même principe, avec des blocs propres :
- `SUBJECT` : objet avec jetons ;
- `GREETING` ;
- `TEXT` : paragraphes avec jetons `{{champ}}` ;
- `DETAILS` : tableau clé / valeur des données de l'événement ;
- `BUTTON` : lien d'action, par exemple payer, signer ou voir la facture ;
- `ATTACHMENT_NOTE` ;
- `SIGNATURE` ;
- `FOOTER` : pied commun, avec mentions et désinscription quand elle s'applique.

Le cadre commun (`email/fragments/notice-layout.html`, logo et couleurs) reste fixe.

- **Table existante :** la table `notification_template` existe déjà et sert de point d'entrée par `template_name`. Elle gagne un lien vers le modèle en blocs, ou elle est reprise dans `document_template` avec la famille `EMAIL` ; à trancher au lot E1 d'après son usage réel.
- **Catalogue :** pour chaque courriel, un catalogue de jetons disponibles (nom du client, montant, numéro, lien...) est fourni à l'éditeur.
- **Aperçu :** l'aperçu rend le HTML final avec des données d'exemple. Un envoi de test va à l'adresse de l'administrateur.
- **Courriels écrits en Java :** les courriels aujourd'hui écrits directement en Java (lien de paiement, expiration d'intention de paiement, résolution mobile money, demande de signature de devis) reçoivent d'abord un gabarit Thymeleaf de repli, puis leur modèle.
- **Courriels de sécurité :** ils sont modifiables en texte seulement (codes à usage unique, réinitialisation de mot de passe). Le jeton du code ou du lien est obligatoire et contrôlé à la publication.

## Modèle de données

| Table | Rôle |
| --- | --- |
| `document_template` | Un modèle par pièce ou courriel : `code` (`CASH_OUT_VOUCHER`, `CASH_IN_VOUCHER`, `NEED_STATEMENT`, `FUND_REQUEST`, `DISBURSEMENT_VOUCHER`, `JUSTIFICATION_SHEET`, `PURCHASE_ORDER`, `PAYMENT_RECEIPT`, `INVOICE`, `EMAIL_BILLING_DOCUMENT`...), libellé, famille (`PDF` ou `EMAIL` et domaine), gabarit de repli, jetons obligatoires, actif |
| `document_template_version` | Versions : numéro, statut (`DRAFT`, `PUBLISHED`, `ARCHIVED`), format, exemplaires (JSON), blocs (JSON), auteur, date de publication. Une seule version publiée par modèle |
| `document_template_field` | Champs ajoutés à une version : clé, libellé, type (texte, nombre, montant, date, liste, case à cocher), mode `PRINTED_BLANK` (imprimé vide) ou `CAPTURED` (saisi dans l'application), obligatoire, options, ordre |
| `document_field_value` | Valeurs saisies d'un champ `CAPTURED` pour une pièce : type de pièce, numéro, clé, valeur, auteur, date |
| colonne `template_version_id` sur chaque pièce imprimable | La version utilisée à la première impression, pour qu'une réimpression reste identique (indispensable pour les factures scellées) |

L'identité de l'exploitant (nom, forme, NIU, RCCM, adresse, téléphone, courriel, représentant, logo et cachet) vient d'une seule source, la fiche exploitant (`business.operator = true`). Elle est figée dans `seller_*` à l'émission des factures.

## Routes prévues (back-office)

- `GET /document-templates` (liste), `GET /document-templates/{code}` (version publiée et brouillon).
- `PUT /document-templates/{code}/draft` : enregistrer le brouillon (blocs, format, exemplaires, champs).
- `POST /document-templates/{code}/preview` : PDF d'aperçu du brouillon avec des données d'exemple, ou avec une pièce réelle (`?number=`).
- `POST /document-templates/{code}/publish` et `/restore/{version}`.
- `GET /document-templates/{code}/fields/catalog` : champs de données disponibles pour ce type de pièce.
- Sur chaque pièce : lecture et saisie des champs `CAPTURED` (`GET/PUT .../{numero}/fields`), contrôlés à la validation si obligatoires.

Droits : nouvelle permission `SYSTEM:DOCUMENT_TEMPLATES` (lecture, modification, publication), donnée à `SUPER_ADMIN` et `ADMIN`.

## Lots

| Lot | Contenu | État |
| --- | --- | --- |
| D1, socle | Composant unique de rendu PDF (remplace les 14 copies), générateur de QR unique, fragments communs d'en-tête et de pied lus depuis la fiche exploitant. Écriture des `seller_*` à l'émission. Corrections : rapport de clôture, QR d'emprunt, marque « Bokati Cowork » / « Elle A Osé », titres accentués, logo du reçu, mention « scellé » du reçu | Fait, voir [lot-d1-socle.md](lot-d1-socle.md) |
| D2, modèles | Tables, routes d'administration, moteur de blocs, aperçu, publication et versions. Modèles de départ des quatre pièces maquettées (bon de sortie et bon d'entrée en deux exemplaires A5, demande de fonds, état de besoin) | Fait, voir [lot-d2-modeles.md](lot-d2-modeles.md) |
| D3, champs ajoutés | Champs `PRINTED_BLANK` et `CAPTURED`, saisie sur les pièces, contrôle des champs obligatoires, valeurs imprimées | Fait, voir [lot-d3-champs.md](lot-d3-champs.md) |
| D4, autres pièces de fonds et d'achats | Bon de décaissement, fiche de justification, bon de commande, demande de caisse, rapport de clôture, déclaration de TVA, état de rapprochement, tableau d'emprunt ; bordereau de remise en banque (nouveau PDF) ; relevés fournisseur et partenaire imprimables | Fait, voir [lot-d4-autres-pieces.md](lot-d4-autres-pieces.md) |
| D5, pièces clients | Reçu de paiement, reçu de portefeuille, relevé de portefeuille, avoir, confirmation de réservation ; facture en dernier, avec la version figée par pièce. Contrats : ils gardent `contract_template`, mais reçoivent l'en-tête commun | Fait, voir [lot-d5-pieces-clients.md](lot-d5-pieces-clients.md) |
| E1, courriels : socle | Blocs de courriel, catalogue de jetons, aperçu HTML et envoi de test, repli sur le gabarit Thymeleaf ; rapprochement avec `notification_template` | À faire |
| E2, courriels : reprise | Tous les courriels de l'inventaire, famille par famille (facturation, paiement, réservation, abonnements, portefeuille, domiciliation, sécurité...) ; gabarits de repli pour les courriels écrits en Java | À faire |
| D6, nettoyage | Suppression des gabarits morts seulement (les gabarits utilisés restent comme repli), doc frontend finale | À faire |

Les migrations vont dans `db/migration` et `db/migration-prod`, à la suite du chantier finance.

## Points à confirmer avec l'exploitant

- Le changement d'un modèle de facture ne touche pas les factures déjà émises, qui gardent leur version.
