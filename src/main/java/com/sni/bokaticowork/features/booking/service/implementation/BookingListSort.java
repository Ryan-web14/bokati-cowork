package com.sni.bokaticowork.features.booking.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import org.springframework.data.domain.Sort;

import java.util.Map;

/**
 * Tri de {@code GET /bookings} (lot B7, ecart 16). Le parametre {@code sort} etait ignore : la
 * requete native triait toujours par pertinence puis par debut decroissant.
 *
 * <p>Sans {@code sort} : pertinence si {@code query} est donne, puis debut decroissant. Avec
 * {@code sort=champ,asc|desc} : ce champ, parmi ceux de {@link #FIELDS}. Un champ inconnu est
 * refuse (400) plutot qu'ignore sans bruit.</p>
 */
record BookingListSort(String key, String direction, Sort applied) {

    static final Map<String, String> FIELDS = Map.of(
            "startedAt", "STARTED_AT",
            "endedAt", "ENDED_AT",
            "createdAt", "CREATED_AT",
            "totalAmount", "TOTAL_AMOUNT",
            "status", "STATUS",
            "bookingNumber", "BOOKING_NUMBER",
            "resourceCode", "RESOURCE_CODE",
            "contactName", "CONTACT_NAME");

    static BookingListSort of(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return new BookingListSort("RELEVANCE", "DESC", Sort.unsorted());
        }
        Sort.Order order = sort.iterator().next();
        String key = FIELDS.get(order.getProperty());
        if (key == null) {
            throw new BadRequestException("Tri impossible sur « " + order.getProperty() + " » ; champs permis : "
                    + String.join(", ", new java.util.TreeSet<>(FIELDS.keySet())));
        }
        return new BookingListSort(key, order.isAscending() ? "ASC" : "DESC", Sort.by(order));
    }
}
