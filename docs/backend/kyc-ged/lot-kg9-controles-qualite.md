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

## OpenCV (à venir)

Voir la réponse du 17/11/2026 : `org.bytedeco:opencv` restreint à `linux-x86_64` (et `windows-x86_64` en dev) par la propriété Maven `javacpp.platform`, chargé à la demande derrière un paramètre, pour la détection du contour de la carte et du visage.

## Tests

`KycCrossCheckServiceTest` (dates saisies, NIU de la fiche sans séparateurs, numéro chez un autre titulaire, OCR absent ignoré), `KycImageQualityServiceTest` (carte nette, carte floue, image petite ou sombre, PNG décodé, format inconnu).
