package com.sni.bokaticowork.features.booking.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public enum BookingParticipantRole {
    HOST,
    GUEST,
    ORGANIZER,
    /** Un participant membre de l'espace · il a son propre compte, ce n'est pas un invite. */
    MEMBER;

    /**
     * Lu sans tenir compte de la casse ni des espaces · {@code "member"} vaut {@code MEMBER}.
     *
     * <p>Un role vide revient a {@code null}, que le mappeur range en {@code GUEST}. Une valeur
     * inconnue est refusee en disant lesquelles sont acceptees.</p>
     */
    @JsonCreator
    public static BookingParticipantRole from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(role -> role.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Rôle de participant inconnu : " + value
                        + ". Valeurs acceptées : " + Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", "))));
    }
}
