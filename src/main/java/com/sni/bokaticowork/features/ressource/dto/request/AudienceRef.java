package com.sni.bokaticowork.features.ressource.dto.request;

/**
 * Lot B10 : un client ou un groupe de clients. {@code type} : MEMBER, CUSTOMER, BUSINESS_ENTITY,
 * SEGMENT ou PLAN ; {@code code} : code du membre, du client, de l'entreprise, du segment (PUBLIC,
 * MEMBER, SUBSCRIBER, COMPANY, PARTNER) ou du plan.
 */
public record AudienceRef(String type, String code) {

    public static final java.util.Set<String> TYPES = java.util.Set.of("MEMBER", "CUSTOMER", "BUSINESS_ENTITY", "SEGMENT", "PLAN");
    public static final java.util.Set<String> SEGMENTS = java.util.Set.of("PUBLIC", "MEMBER", "SUBSCRIBER", "COMPANY", "PARTNER");

    /** Forme normalisee « TYPE:CODE » ; refuse un type ou un segment inconnu. */
    public String key() {
        if (type == null || code == null || type.isBlank() || code.isBlank()) {
            throw new com.sni.bokaticowork.core.exception.customs.BadRequestException("Chaque client désigné porte un type et un code");
        }
        String t = type.trim().toUpperCase(java.util.Locale.ROOT);
        if ("BUSINESS".equals(t)) t = "BUSINESS_ENTITY";
        if (!TYPES.contains(t)) {
            throw new com.sni.bokaticowork.core.exception.customs.BadRequestException(
                    "Type de client invalide : MEMBER, CUSTOMER, BUSINESS_ENTITY, SEGMENT ou PLAN");
        }
        String c = code.trim();
        if ("SEGMENT".equals(t)) {
            c = c.toUpperCase(java.util.Locale.ROOT);
            if (!SEGMENTS.contains(c)) {
                throw new com.sni.bokaticowork.core.exception.customs.BadRequestException(
                        "Segment invalide : PUBLIC, MEMBER, SUBSCRIBER, COMPANY ou PARTNER");
            }
        }
        if (c.contains(",") || c.contains(":")) {
            throw new com.sni.bokaticowork.core.exception.customs.BadRequestException("Code invalide : " + c);
        }
        return t + ":" + c;
    }

    public static AudienceRef of(String key) {
        int colon = key.indexOf(':');
        return colon < 0 ? new AudienceRef(key, "") : new AudienceRef(key.substring(0, colon), key.substring(colon + 1));
    }

    /** « TYPE:CODE,TYPE:CODE » depuis une liste ; nul si vide. */
    public static String join(java.util.List<AudienceRef> refs) {
        if (refs == null || refs.isEmpty()) {
            return null;
        }
        return String.join(",", refs.stream().map(AudienceRef::key).distinct().toList());
    }

    public static java.util.List<AudienceRef> split(String joined) {
        if (joined == null || joined.isBlank()) {
            return java.util.List.of();
        }
        return java.util.Arrays.stream(joined.split(",")).filter(part -> !part.isBlank()).map(AudienceRef::of).toList();
    }
}
