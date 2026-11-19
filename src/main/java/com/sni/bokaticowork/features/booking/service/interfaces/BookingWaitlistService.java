package com.sni.bokaticowork.features.booking.service.interfaces;

import com.sni.bokaticowork.features.booking.dto.request.JoinBookingWaitlistRequest;
import com.sni.bokaticowork.features.booking.dto.response.BookingWaitlistEntryResponse;
import com.sni.bokaticowork.features.booking.enums.BookingWaitlistStatus;

import java.util.List;

public interface BookingWaitlistService {
    BookingWaitlistEntryResponse join(JoinBookingWaitlistRequest request);
    List<BookingWaitlistEntryResponse> list(BookingWaitlistStatus status);
    BookingWaitlistEntryResponse cancel(Long id);
    int promoteAvailable(int limit);
    int expireOffers(int limit);

    /** Lot B6 : liste filtree et paginee (statut, ressource, titulaire). */
    com.sni.bokaticowork.core.templateResponse.PaginatedResponse<BookingWaitlistEntryResponse> search(
            BookingWaitlistStatus status, String resourceCode, com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType ownerType,
            String ownerCode, org.springframework.data.domain.Pageable pageable);

    /** Lot B6 : l'offre est acceptee, la reservation est creee pour le titulaire de l'inscription. */
    BookingWaitlistEntryResponse accept(Long id);

    /** Lot B6 : l'offre est declinee ; l'inscription suivante sur ce creneau sera proposee. */
    BookingWaitlistEntryResponse decline(Long id);

    /** Lot B6 : une inscription, pour le portail (controle du titulaire par l'appelant). */
    BookingWaitlistEntryResponse get(Long id);
}
