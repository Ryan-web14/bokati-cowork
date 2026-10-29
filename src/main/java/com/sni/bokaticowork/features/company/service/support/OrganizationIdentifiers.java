package com.sni.bokaticowork.features.company.service.support;

import com.sni.bokaticowork.core.utils.validation.ValidationUtils;
import com.sni.bokaticowork.features.company.enums.IdentifierType;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * La forme des identifiants d'une organisation · verifiee seulement la ou elle est connue.
 *
 * <p>Le RCCM suit la nomenclature OHADA de juin 2019 : pays, ville, juridiction, annee,
 * formalite, numero ({@code CG-PNR-01-2017-A11-00472}). Un numero anterieur, de la forme
 * {@code CG/BZV/16 B 6261}, reste accepte tel quel · il a ete delivre, on ne le refuse pas. Le NIU
 * garde la regle deja en place. Les autres identifiants (recepisse, agrement, inscription a l'ordre,
 * acte de creation) n'ont pas de format publie : ils sont pris tels quels.</p>
 */
public final class OrganizationIdentifiers {

    /** OHADA 2019 · CG-PNR-01-2017-A11-00472. */
    private static final Pattern RCCM_OHADA = Pattern.compile("^[A-Z]{2}-[A-Z]{3}-\\d{2}-\\d{4}-[A-Z]\\d{2}-\\d{5}$");

    /**
     * Avant 2019 · CG/BZV/16 B 6261 : pays, ville, annee sur deux chiffres, lettre du registre,
     * numero. Les barres sont exigees · un numero OHADA mal saisi ne passe pas pour un ancien.
     */
    private static final Pattern RCCM_LEGACY = Pattern.compile("^[A-Z]{2}\\s*/\\s*[A-Z]{3}\\s*/\\s*\\d{2}\\s*-?\\s*[A-Z]\\s*-?\\s*\\d{1,6}$");

    private static final Pattern FREE_TEXT = Pattern.compile("^[\\p{L}0-9 ./_()°'-]{1,150}$");

    private OrganizationIdentifiers() {
    }

    /** Ce que la verification a conclu · {@code LEGACY} est accepte, et signale. */
    public enum Verdict { VALID, LEGACY, INVALID }

    /** La valeur telle qu'on la range · majuscules, espaces de bord retires, espaces du RCCM OHADA retires. */
    public static String normalize(IdentifierType type, String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim().replaceAll("\\s+", " ");
        return switch (type) {
            case NIU -> trimmed.replace(" ", "").toUpperCase(Locale.ROOT);
            case RCCM -> {
                String upper = trimmed.toUpperCase(Locale.ROOT);
                String compact = upper.replaceAll("\\s*-\\s*", "-");
                yield RCCM_OHADA.matcher(compact).matches() ? compact : upper;
            }
            default -> trimmed;
        };
    }

    public static Verdict check(IdentifierType type, String normalized) {
        if (!StringUtils.hasText(normalized)) {
            return Verdict.INVALID;
        }
        return switch (type) {
            case NIU -> ValidationUtils.validateNiu(normalized) ? Verdict.VALID : Verdict.INVALID;
            case RCCM -> RCCM_OHADA.matcher(normalized).matches() ? Verdict.VALID
                    : RCCM_LEGACY.matcher(normalized).matches() ? Verdict.LEGACY : Verdict.INVALID;
            default -> FREE_TEXT.matcher(normalized).matches() ? Verdict.VALID : Verdict.INVALID;
        };
    }

    /** Le message d'un refus · dit le format attendu plutot que « invalide ». */
    public static String expected(IdentifierType type) {
        return switch (type) {
            case NIU -> "le NIU attendu est une lettre (P ou E) suivie de 15 chiffres";
            case RCCM -> "le RCCM attendu suit la forme OHADA CG-BZV-01-2024-B12-00001, ou l'ancienne forme CG/BZV/16 B 6261";
            default -> "lettres, chiffres et ponctuation simple, 150 caractères au plus";
        };
    }
}
