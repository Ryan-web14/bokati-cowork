# Sécurisation des PDF générés

Écrit le 2026-08-30.

## 0. Le principe

**Un PDF livré ne peut pas être rendu non modifiable.** C'est un fichier sur le disque du
destinataire. Les protections du format — mot de passe propriétaire, drapeaux de permission — sont
déclaratives : elles demandent au lecteur de se comporter, et tout outil gratuit les retire en
quelques secondes. Un simple *imprimer vers PDF* produit de toute façon un fichier neuf, sans
aucune restriction.

L'objectif atteignable est différent, et il règle le vrai problème :

- **toute altération est détectable**, y compris par un tiers qui n'a pas nos accès ;
- **le serveur fait foi**, pas le fichier.

Un faux ne devient pas impossible : il devient inutile, parce que quiconque le vérifie voit qu'il
ne correspond à rien.

---

## 1. État des lieux

### 1.1 Ce qui existe

| Brique | Où |
|---|---|
| Chaîne de hachage fiscale SHA-256 | `FiscalHashService` |
| Signature HMAC du hash | `FiscalSignatureService.sign` / `.verify` |
| QR code sur la facture | `BillingDocumentPdfServiceImpl` + `FiscalQrCodeService` |
| Page de vérification publique | `VerifyController`, `/verify/**` ouvert dans `SecurityConfig` |
| Stockage de fichiers | `DocumentStorageService.storeBytes` / `.read`, filesystem ou MinIO |

L'ossature est là. Les manques sont dans le chaînage entre ces briques.

### 1.2 Les générateurs de PDF

Douze points de génération, qui n'appellent pas la même exigence :

| Niveau | Documents | Exigence |
|---|---|---|
| **Fiscal** | Facture, avoir, facture rectificative, note de débit | Vérifiable par un tiers, opposable, signé |
| **Contractuel** | Contrat, avenant, reçu de paiement | Vérifiable, signé |
| **Opérationnel** | Devis, confirmation de réservation, badge visiteur | Vérifiable suffit |
| **Interne** | Rapports, fiche de prêt, mouvements d'inventaire | Aucune · ne sort pas de la structure |

Le plan traite les deux premiers niveaux. Le troisième hérite gratuitement du lot 1. Le quatrième
est explicitement hors périmètre : y ajouter de la signature coûterait sans rien protéger.

### 1.3 Les trois manques

**A — Le QR d'un document scellé ne mène nulle part.** `FiscalQrCodeService.buildContent` renvoie
une URL de vérification pour un document **non** validé, et un JSON compact pour un document
**validé**. C'est inversé : la facture scellée, celle qui a le plus besoin d'être vérifiable par un
tiers, porte un QR qu'un téléphone affiche en texte brut. Personne ne recopiera un hash à la main.

**B — Le PDF n'est jamais figé.** `generatePdf` le reconstruit à chaque appel, rien n'est stocké,
aucune empreinte n'est conservée. Impossible de prouver qu'un fichier présenté est bien celui émis.

**C — Aucune signature dans le PDF.** Rien ne fait réagir Adobe Reader, Foxit ou un navigateur.
Le destinataire doit faire une démarche pour vérifier — donc il ne la fera pas.

### 1.4 Un défaut de confidentialité à corriger au passage

`VerifyController.verifyDocument` charge le document complet et **la liste de ses paiements**, et
les rend à `verify/document`. La route est publique, sans jeton, et la clé est le numéro de
document — c'est-à-dire une valeur **énumérable** : `INV-MEM-20260830-00000009` désigne sans
ambiguïté ses voisins.

Autrement dit, une boucle sur les numéros expose la facturation de tous les clients : montants,
identité du destinataire, paiements reçus.

Ce n'est pas un manque de sécurisation des PDF, c'est une fuite de données, et elle est plus
urgente que les trois manques ci-dessus. Elle est traitée au lot 1.

---

## 2. Lot 1 — QR utile et page de vérification durcie

**1,5 j.** Aucun prérequis. Le meilleur rapport de toute la liste, et il ferme la fuite du § 1.4.

### 2.1 Le QR porte une URL dans tous les cas

```
https://app.elleaose.com/verify/doc/INV-MEM-20260830-00000009?k=a1b2c3d4e5f6a7b8
```

`k` est le préfixe de la **signature HMAC**, pas du hash. Le hash se recalcule à partir de données
qu'un faussaire connaît (elles sont imprimées sur la facture) ; la signature, non — elle exige la
clé serveur. C'est ce qui fait que `k` ne peut pas être fabriqué.

Seize caractères hexadécimaux, soit 64 bits : suffisant contre la devinette, et le QR reste dense.

### 2.2 La page devient une réponse, pas un dossier

Aujourd'hui elle affiche le document entier. Elle doit répondre à une question et rien de plus.

**Sans `k` valide** — trois informations, pas une de plus :

> Document **INV-MEM-20260830-00000009** — émis le 30/08/2026 — **authentique**

Ni montant, ni destinataire, ni paiements. Cela suffit à confirmer l'existence et la date, et ne
révèle rien à qui n'a pas le document sous les yeux.

**Avec `k` valide** — le récapitulatif complet : montant, destinataire, statut, paiements. La
détention de la signature prouve la détention du document.

**Avec `k` invalide** — refus explicite : « ce document ne correspond à aucun document émis ».

### 2.3 Limitation de débit

Dix requêtes par minute et par IP sur `/verify/**`. Sans cela, l'énumération reste possible même
avec la réponse minimale : on apprendrait quels numéros existent. Resilience4j est déjà au projet.

### 2.4 Fichiers

| Fichier | Modification |
|---|---|
| `FiscalQrCodeService` | Toujours une URL, avec `k` sur les documents scellés |
| `VerifyController` | Paramètre `k`, deux niveaux de réponse, plus de chargement systématique des paiements |
| `templates/verify/document.html` | Vue réduite et vue complète |
| `SecurityConfig` | Limiteur de débit sur `/verify/**` |

Aucune migration.

---

## 3. Lot 2 — PDF figé et empreinte

**2 j.** Dépend du lot 1 pour la page de vérification.

### 3.1 Le principe

À la validation d'un document, le PDF est généré **une fois**, stocké, et son SHA-256 conservé.
C'est cette version-là qui est servie et envoyée, plus jamais une régénération.

Deux bénéfices distincts :

- **Opposabilité.** On peut affirmer « voici l'octet près ce que nous avons émis ».
- **Stabilité.** Une régénération ultérieure produirait un fichier différent — ne serait-ce que par
  la date de génération en pied de page — et rendrait toute comparaison impossible.

> Ce second point est la vraie raison du lot. Sans fichier figé, le lot 3 est inapplicable : on ne
> signe pas un document qui change à chaque téléchargement.

### 3.2 Schéma — `V212` dev / `V208` prod

```sql
ALTER TABLE billing_document
    ADD COLUMN IF NOT EXISTS pdf_storage_provider VARCHAR(30),
    ADD COLUMN IF NOT EXISTS pdf_storage_path     VARCHAR(500),
    ADD COLUMN IF NOT EXISTS pdf_sha256           VARCHAR(64),
    ADD COLUMN IF NOT EXISTS pdf_sealed_at        TIMESTAMP WITH TIME ZONE;
```

Les quatre vont ensemble ; contrainte de complétude en `NOT VALID`, comme pour la dérogation de
remise en `V210`.

`DocumentStorageService.storeBytes` et `.read` sont réutilisés tels quels — filesystem en dev,
MinIO en production, sans code nouveau.

### 3.3 Comportement

- `validate()` génère, stocke, calcule l'empreinte.
- `generatePdf()` sert le fichier stocké s'il existe ; sinon il régénère, comme aujourd'hui, ce qui
  laisse l'existant fonctionner sans reprise.
- La page de vérification accepte le **dépôt d'un fichier** et répond « c'est bien l'original » ou
  « ce fichier diffère de celui que nous avons émis ». C'est l'outil qui tranche un litige.

### 3.4 Point d'attention

Le stockage grossit d'un PDF par document scellé. À raison de ~80 Ko et quelques milliers de
documents par an, c'est négligeable — mais c'est à surveiller si le volume change d'ordre.

---

## 4. Lot 3 — Signature PAdES

**2 j de développement**, plus l'obtention du certificat, qui est l'inconnue.

### 4.1 Ce que ça apporte, et que rien d'autre n'apporte

C'est le seul mécanisme qui fait qu'un lecteur PDF affiche **de lui-même**, sans démarche du
destinataire :

> ⚠ Ce document a été modifié depuis sa signature.

| Certificat | Ce que voit le destinataire |
|---|---|
| Auto-signé | Bandeau jaune · « signature valide, émetteur non vérifié » |
| Autorité reconnue | Bandeau vert · « signature valide » |

**Même auto-signé, le bandeau change dès qu'un octet bouge.** C'est ce qui produit l'effet
« document non modifiable » recherché. Commencer auto-signé est donc utile : le code ne changera
pas quand un certificat reconnu arrivera, seule la source du keystore change.

### 4.2 Mise en œuvre

PDFBox est déjà au projet — `openhtmltopdf-pdfbox` l'embarque. La signature s'applique **après**
la génération, sur les octets figés du lot 2.

```yaml
bokati.pdf.signature:
  enabled: ${PDF_SIGNATURE_ENABLED:false}
  keystore-path: ${PDF_SIGNATURE_KEYSTORE:}
  keystore-password: ${PDF_SIGNATURE_KEYSTORE_PASSWORD:}
  alias: ${PDF_SIGNATURE_ALIAS:bokati}
  reason: ${PDF_SIGNATURE_REASON:Document emis par Bokati Cowork}
```

Désactivé par défaut : sans keystore configuré, rien ne doit échouer.

`PdfSignatureService.sign(byte[]) → byte[]`, appelé par le lot 2 juste avant le stockage. Un échec
de signature **ne doit pas empêcher l'émission** : on journalise, on stocke le PDF non signé, et le
document reste valide — les lots 1 et 2 continuent de le couvrir.

### 4.3 La clé

Le keystore ne va pas dans le dépôt. Sur Heroku, base64 dans une variable de configuration, décodé
au démarrage — même traitement que `BILLING_FISCAL_SIGNING_KEY`. Sa compromission permettrait de
signer de faux documents : rotation à prévoir, et la procédure doit exister avant la mise en
service.

### 4.4 Horodatage

Une signature PAdES sans horodatage vaut jusqu'à l'expiration du certificat. Avec un jeton d'un
service de temps (RFC 3161), elle reste vérifiable au-delà. À considérer si les documents doivent
tenir dix ans — ce qui est le cas d'une facture. Pas dans ce lot ; à noter.

---

## 5. Lot 4 — Extension à la flotte

**1,5 j.** Une fois les trois lots posés sur la facturation, l'extension est mécanique.

| Document | Lot 1 | Lot 2 | Lot 3 |
|---|---|---|---|
| Reçu de paiement | déjà un QR `/verify/receipt/` | oui | oui |
| Contrat, avenant | à ajouter | oui | oui |
| Devis | à ajouter | non — il change par nature | non |
| Confirmation de réservation, badge visiteur | à ajouter | non | non |
| Rapports, fiche de prêt, mouvements | non | non | non |

`PaymentReceiptServiceImpl` construit déjà une URL `/verify/receipt/` : il hérite du lot 1 sans
modification autre que l'ajout du paramètre `k`.

Le devis est volontairement exclu des lots 2 et 3 : il est fait pour être révisé, et le versionnage
livré le 2026-08-29 couvre déjà le besoin de savoir ce que le client a vu.

---

## 6. Ce qu'on ne fait pas

**Mot de passe propriétaire et permissions PDF.** Retirés en une commande par n'importe quel outil,
ils cassent l'accessibilité et gênent l'archivage. Surtout, ils donnent une **fausse assurance** :
on croit le document protégé, on cesse de vérifier. C'est pire que rien.

Si le besoin est d'empêcher la copie du texte par confort, c'est un ralentisseur, pas une sécurité,
et il ne doit jamais remplacer les lots 1 à 3.

**Aplatir le PDF en images.** Empêche la sélection du texte, quadruple le poids, rend le document
inaccessible aux lecteurs d'écran et illisible par un OCR comptable. Ne protège de rien : l'image
se modifie aussi.

---

## 7. Récapitulatif

### 7.1 Ordre et coûts

| Lot | Contenu | Dépend de | Coût |
|---|---|---|---|
| **1** | QR utile, page durcie, limitation de débit | — | 1,5 j |
| **2** | PDF figé, empreinte, comparaison de fichier | 1 | 2 j |
| **3** | Signature PAdES | 2 | 2 j + certificat |
| **4** | Extension aux reçus et contrats | 1–3 | 1,5 j |

**7 jours.** Le lot 1 est à faire indépendamment de la suite : il ferme la fuite du § 1.4.

### 7.2 Migrations

| Dev | Prod | Contenu |
|---|---|---|
| `V212` | `V208` | Empreinte et stockage du PDF scellé |

### 7.3 Configuration

| Variable | Défaut | Effet |
|---|---|---|
| `PDF_SIGNATURE_ENABLED` | `false` | Active la signature PAdES |
| `PDF_SIGNATURE_KEYSTORE` | vide | Keystore en base64 |
| `PDF_SIGNATURE_KEYSTORE_PASSWORD` | vide | — |
| `PDF_SIGNATURE_ALIAS` | `bokati` | Alias de la clé |
| `VERIFY_RATE_LIMIT_PER_MINUTE` | `10` | Requêtes par IP sur `/verify/**` |

### 7.4 Décisions attendues

- **D-1.** Certificat auto-signé pour commencer, ou attendre un certificat d'une autorité
  reconnue ? Ma recommandation : auto-signé tout de suite. Le bandeau change de couleur dès la
  moindre altération, ce qui est l'essentiel, et le code ne bougera pas ensuite.
- **D-2.** La page de vérification sans `k` doit-elle afficher le montant ? Ma recommandation :
  non. Le numéro, la date et « authentique » suffisent à confirmer, et n'exposent rien.
- **D-3.** Horodatage RFC 3161 (§ 4.4) — nécessaire si les factures doivent rester vérifiables
  au-delà de la validité du certificat.
