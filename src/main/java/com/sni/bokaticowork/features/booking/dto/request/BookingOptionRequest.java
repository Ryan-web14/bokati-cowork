package com.sni.bokaticowork.features.booking.dto.request;

/**
 * Lot B2b : option prise avec la reservation. {@code kind} : SERVICE (article du catalogue des
 * services : cafe, traiteur, impression, materiel) ou AMENITY (equipement optionnel de la ressource).
 */
public record BookingOptionRequest(String kind, String code, Integer quantity) {
}
