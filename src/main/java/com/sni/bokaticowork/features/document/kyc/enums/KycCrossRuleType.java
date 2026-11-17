package com.sni.bokaticowork.features.document.kyc.enums;

/**
 * Type d'une regle de controle croise KYC.
 *
 * <p>Les types sans OCR s'appuient sur les donnees saisies au depot et sur la fiche du titulaire :
 * ils s'appliquent meme quand l'OCR n'est pas disponible (cas de la production). Les types OCR sont
 * ignores (resultat {@code SKIPPED}) tant qu'aucune lecture n'existe.</p>
 */
public enum KycCrossRuleType {
    /** Meme champ lu (OCR) sur deux pieces ({@code documentTypeCode1}, {@code documentTypeCode2}). */
    FIELD_MATCH,
    /** Valeur saisie au depot egale a la valeur lue (OCR) sur la meme piece. */
    DECLARED_VS_OCR,
    /** Valeur de la piece (saisie, sinon lue) egale a celle de la fiche : {@code niu}, {@code rccm}, {@code last_name}, {@code first_name}. */
    PROFILE_MATCH,
    /** Date d'expiration saisie non depassee. */
    DOCUMENT_NOT_EXPIRED,
    /** Date d'emission passee et anterieure a l'expiration. */
    ISSUE_BEFORE_EXPIRY,
    /** Piece encore valable au moins {@code parameter} jours. */
    MIN_VALIDITY,
    /** Numero saisi non utilise par un autre titulaire. */
    UNIQUE_DOCUMENT_NUMBER
}
