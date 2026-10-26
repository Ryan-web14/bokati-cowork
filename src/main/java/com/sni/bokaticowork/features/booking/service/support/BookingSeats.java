package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.ressource.model.Resource;

/**
 * Ce qu'une reservation occupe, dit comme le client doit le lire.
 *
 * <p>La quantite d'une reservation ne veut pas dire la meme chose selon la ressource. Sur un espace
 * partage (open space, parking), ce sont des places : 2 places d'un bureau de 4. Sur une piece
 * louee entiere (salle, bureau privatif), ce sont des participants, et la piece entiere est a eux.
 * L'e-mail et les ecrans affichaient la capacite de la ressource, « 4 personnes », a qui avait
 * reserve 2 places · le client croyait avoir le bureau entier.</p>
 *
 * @param reservedSeats      places reservees (espace partage) ou participants annonces (piece entiere)
 * @param resourceCapacity   places de la ressource
 * @param wholeResource      la piece est louee entiere
 * @param label              le libelle a afficher, tel quel
 */
public record BookingSeats(Integer reservedSeats, Integer resourceCapacity, boolean wholeResource, String label) {

    public static BookingSeats of(Booking booking) {
        Resource resource = booking == null ? null : booking.getResource();
        int quantity = booking == null || booking.getQuantity() == null ? 1 : Math.max(1, booking.getQuantity());
        Integer capacity = resource == null ? null : resource.getCapacity();
        boolean whole = resource != null && resource.isWholeResourceBooking();
        return new BookingSeats(quantity, capacity, whole, label(quantity, capacity, whole));
    }

    static String label(int quantity, Integer capacity, boolean whole) {
        if (whole) {
            String participants = quantity + (quantity > 1 ? " participants" : " participant");
            return capacity == null
                    ? "Espace entier · " + participants
                    : "Espace entier · " + participants + " (" + capacity + " places)";
        }
        String seats = quantity + (quantity > 1 ? " places réservées" : " place réservée");
        return capacity == null || capacity <= quantity ? seats : seats + " sur " + capacity;
    }
}
