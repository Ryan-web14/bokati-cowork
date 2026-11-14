package com.sni.bokaticowork.features.billing.element.model;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;

/**
 * Element facture designe par son type et son code : {@code SUBSCRIPTION SUB-...},
 * {@code PASS PAS-...}, {@code BOOKING BKG-...}, {@code DOMICILIATION DOM-...}.
 */
public record ElementRef(String type, String code) {

    public static final String SUBSCRIPTION = "SUBSCRIPTION";
    public static final String PASS = "PASS";
    public static final String BOOKING = "BOOKING";
    public static final String DOMICILIATION = "DOMICILIATION";
    public static final Set<String> TYPES = Set.of(SUBSCRIPTION, PASS, BOOKING, DOMICILIATION);

    public static final String FULL_SETTLEMENT = "FULL_SETTLEMENT";
    public static final String FIRST_PAYMENT = "FIRST_PAYMENT";
    public static final Set<String> MODES = Set.of(FULL_SETTLEMENT, FIRST_PAYMENT);

    public static ElementRef of(String type, String code) {
        String normalizedType = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        if (!TYPES.contains(normalizedType)) {
            throw BadRequestException.coded("ELEMENT_TYPE_INCONNU", "Type d'élément inconnu « " + type
                    + " » : SUBSCRIPTION, PASS, BOOKING ou DOMICILIATION");
        }
        if (!StringUtils.hasText(code)) {
            throw BadRequestException.coded("ELEMENT_CODE_REQUIS", "Le code de l'élément est requis");
        }
        return new ElementRef(normalizedType, code.trim());
    }

    public static String mode(String mode) {
        if (!StringUtils.hasText(mode)) {
            return FULL_SETTLEMENT;
        }
        String normalized = mode.trim().toUpperCase(Locale.ROOT);
        if (!MODES.contains(normalized)) {
            throw BadRequestException.coded("MODE_ACTIVATION_INCONNU", "Mode d'activation inconnu « " + mode
                    + " » : FULL_SETTLEMENT (au solde) ou FIRST_PAYMENT (dès le premier paiement)");
        }
        return normalized;
    }

    /** Type d'element d'une ligne facturable, selon sa source ; vide pour une source sans element. */
    public static String typeOfBillableSource(String sourceType) {
        if (sourceType == null) {
            return null;
        }
        String source = sourceType.trim().toUpperCase(Locale.ROOT);
        if (source.startsWith("PASS")) {
            return PASS;
        }
        if (source.equals("BOOKING")) {
            return BOOKING;
        }
        if (source.startsWith("SUBSCRIPTION") || source.equals("DEPOSIT") || source.equals("USAGE_OVERAGE")) {
            return SUBSCRIPTION;
        }
        return null;
    }
}
