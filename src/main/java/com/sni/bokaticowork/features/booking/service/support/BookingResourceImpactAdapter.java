package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import com.sni.bokaticowork.features.ressource.service.support.ResourceBookingImpact;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/** Lot R2 : reservations touchees par la suppression, la fermeture ou la maintenance d'une ressource. */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingResourceImpactAdapter implements ResourceBookingImpact {

    private final BookingRepository bookingRepository;
    @Lazy
    private final BookingService bookingService;

    @Override
    public List<ImpactedBooking> activeBookings(Long resourceId, LocalDateTime from, LocalDateTime to) {
        return bookingRepository.findActiveOnResource(resourceId, from, to).stream()
                .map(booking -> new ImpactedBooking(booking.getBookingNumber(), booking.getStatus() == null ? null : booking.getStatus().name(),
                        booking.getStartedAt(), booking.getEndedAt(),
                        booking.getOwnerType() == null ? null : booking.getOwnerType().name(), booking.getOwnerCode(), booking.getContactName(),
                        booking.getQuantity() == null ? 1 : booking.getQuantity()))
                .toList();
    }

    @Override
    public void cancelAndNotify(List<String> bookingNumbers, String reason) {
        for (String number : bookingNumbers) {
            bookingService.cancelForResourceUnavailability(number, reason);
        }
    }
}
