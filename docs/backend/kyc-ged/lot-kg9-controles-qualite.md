# Lot KG9 (première partie) : contrôles croisés préenregistrés et qualité d'image

> Partie du [plan KYC et GED](README.md). Fait le 17/11/2026 sur `feature/kyc-ged`, à la demande de l'exploitant (Tesseract absent de Heroku, avertissement d'image floue).

## Contrôles croisés

- Les règles ne comparaient que des champs lus par OCR : sans Tesseract, elles ne s'appliquaient jamais.
- `KycCrossRuleType` : `FIELD_MATCH` (ancien comportement), `DECLARED_VS_OCR`, `PROFILE_MATCH`, `DOCUMENT_NOT_EXPIRED`, `ISSUE_BEFORE_EXPIRY`, `MIN_VALIDITY`, `UNIQUE_DOCUMENT_NUMBER`. Seuls les types OCR dépendent de Tesseract.
- `KycCrossCheckService` évalue chaque règle active sur chaque pièce vivante visée (`PASSED`, `FAILED`, `SKIPPED` avec la raison) ; l'approbation manuelle refuse sur une règle bloquante en échec, en nommant les règles.
- `GET /kyc/cases/{code}/cross-checks` pour l'écran du réviseur.
- Dix règles préenregistrées (`system_rule`), modifiables, désactivables, non supprimables. Liste et détail dans [parcours-kyc](../../final-frontend-doc/document/parcours-kyc.md#règles-de-contrôle-croisé).
- Migrations : dev `V369__kyc_controles_croises_qualite.sql`, prod `V363` (colonnes `code`, `name`, `description`, `rule_type`, `parameter`, `system_rule` ; types de pièces élargis à 1000 caractères ; anciennes colonnes facultatives).

## Qualité d'image au dépôt

- `KycImageQualityService`, en Java seul (pas de bibliothèque native) : netteté par variance du laplacien sur l'image en gris ramenée à 1000 px, plus petit côté, luminosité. Un PDF est lu par sa première page (PDFBox). HEIC ou WEBP : `UNKNOWN`.
- Mesurée dans `DocumentService.upload` pour les pièces KYC, enregistrée sur la pièce (recto `quality_*`, verso `back_quality_*`), renvoyée par le portail, le dépôt de l'équipe, la fiche et les files de revue. Avertissement seulement.
- Le contrôle de reflet a été écarté : le fond blanc d'un scan le déclencherait à tort.
- Paramètres : `app.kyc.quality.blur-threshold` (100), `app.kyc.quality.min-side-px` (600), `app.kyc.quality.enabled`.

## OCR absent du serveur

- Sans la bibliothèque native, Tess4J lève une `UnsatisfiedLinkError` (une `Error`, que le service n'attrapait pas) : la synchronisation de la pièce et le recalcul du dossier échouaient à chaque dépôt.
- Désormais : premier échec de chargement, OCR marqué indisponible jusqu'au prochain démarrage, résultat `SKIPPED` (`OCR_UNAVAILABLE`), un seul message dans les journaux.
- Pour avoir l'OCR sur Heroku plus tard : buildpack `heroku-community/apt` avec un `Aptfile` (`tesseract-ocr`, `tesseract-ocr-fra`, `tesseract-ocr-eng`) et `TESSDATA_PREFIX` ; environ 40 Mo de slug, chargement à la première pièce.

## Analyse « pièce d'identité » (seconde partie, 17/11/2026)

Au dépôt d'une pièce d'identité (types dont le code contient `ID_CARD`, `NATIONAL_ID`, `PASSPORT`, `DRIVERS_LICENSE`, `RESIDENCE_PERMIT`, `REPRESENTATIVE_ID`), en même temps que la netteté et sur la même image : est-ce bien une pièce d'identité ? Avertissement seulement, jamais un refus.

- Moteur **Java** (défaut, `JavaIdentityEngine`, sans bibliothèque native) : contour par les bords droits longs (profils de gradient), sinon image supposée recadrée ; format par le rapport des côtés (ID1, carte 1,586 ; ID3, page de passeport 1,42 ; 7 % de tolérance) ; lignes de texte par densité de transitions ; bande MRZ (deux lignes serrées sur toute la largeur en bas). Pas de visage.
- Moteur **OpenCV** (`app.kyc.identity.engine=OPENCV`, `OpenCvIdentityEngine`) : contour par Canny et plus grand quadrilatère convexe, visage par la cascade de Haar `haarcascade_frontalface_default` (fournie dans le paquet OpenCV Linux), texte et MRZ repris du moteur Java dans le cadre trouvé. Classe ordinaire créée à la première analyse : rien de natif au démarrage ni dans l'entraînement du cache AOT. Échec de chargement : moteur Java jusqu'au prochain démarrage, un seul message.
- Verdict (`KycIdentityVerdicts`) : `LIKELY_IDENTITY` (format reconnu et MRZ, visage, ou contour et texte), `NOT_IDENTITY` (ni format, ni texte, ni visage), sinon `UNCERTAIN`. Avertissements : pièce méconnaissable, format non reconnu, peu de texte, MRZ absente au recto d'un passeport, aucun visage (OpenCV, recto).
- Enregistré sur la pièce (`identity_verdict`, `back_identity_verdict`, `identity_engine`) ; avertissements ajoutés à ceux de la qualité (`qualityWarnings`, statut `WARNING`).
- Paramètres : `app.kyc.identity.enabled` (vrai), `app.kyc.identity.engine` (`JAVA` ou `OPENCV`), `app.kyc.identity.document-types`.
- Dépendances : `org.bytedeco` `javacpp` 1.5.11, `opencv` 4.10.0-1.5.11, `openblas` 0.3.28-1.5.11, bibliothèques natives `linux-x86_64` seules dans le build (environ 46 Mo de plus au slug Heroku, 323 Mo avant, limite 500) ; celles de Windows par le profil Maven `opencv-windows`, actif sur un poste Windows seulement.
- Mémoire : avec OpenCV, environ 50 à 150 Mo natifs de plus au premier usage, hors du tas Java ; à surveiller sur le dyno (marge faible, voir docs/backend/demarrage-et-dynos). Le moteur Java n'ajoute rien.
- Migrations : dev `V371__kyc_analyse_piece_identite.sql`, prod `V365`.

## Tests

`KycCrossCheckServiceTest` (dates saisies, NIU de la fiche sans séparateurs, numéro chez un autre titulaire, OCR absent ignoré), `KycImageQualityServiceTest` (carte nette, carte floue, image petite ou sombre, PNG décodé, format inconnu), `KycIdentityAnalysisTest` (carte ID1 avec texte et MRZ, image sans format ni texte, page de passeport sans MRZ, types analysés, moteur OpenCV réellement chargé : contour trouvé, pas de visage sur une carte fabriquée).
