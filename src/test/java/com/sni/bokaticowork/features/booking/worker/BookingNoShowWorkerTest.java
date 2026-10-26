package com.sni.bokaticowork.features.booking.worker;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Une reservation en echec n'arrete plus le worker.
 *
 * <p>Le lot tenait dans une seule transaction : un droit non decomptable annulait tout, chaque
 * minute, et les clotures automatiques ne passaient plus jamais.</p>
 */
class BookingNoShowWorkerTest {

    @Test
    @DisplayName("Une reservation qui echoue est consignee, les suivantes et la cloture passent quand meme")
    void oneFailureDoesNotStopTheOthers() {
        BookingService service = mock(BookingService.class);
        when(service.overdueNoShowCandidates(BookingNoShowWorker.BATCH)).thenReturn(List.of("BKG-1", "BKG-2"));
        when(service.markOverdueNoShow("BKG-1")).thenThrow(new ConflictException("entitlement", "insufficient balance"));
        when(service.markOverdueNoShow("BKG-2")).thenReturn(true);
        when(service.overdueCompletionCandidates(BookingNoShowWorker.BATCH)).thenReturn(List.of("BKG-3"));
        when(service.markOverdueCompleted("BKG-3")).thenReturn(true);

        new BookingNoShowWorker(service).processOverdueBookings();

        verify(service).markOverdueNoShow("BKG-2");
        verify(service).markOverdueCompleted("BKG-3");
    }

    @Test
    @DisplayName("La retenue d'un droit tient jusqu'a une semaine apres la fin de la reservation")
    void holdOutlivesTheBooking() {
        LocalDateTime end = LocalDateTime.of(2026, 10, 28, 17, 0);
        Booking booking = Booking.builder().endedAt(end).build();

        assertThat(com.sni.bokaticowork.features.booking.service.support.BookingEntitlementBridge.holdUntil(booking))
                .isEqualTo(end.plusDays(7).atZone(ZoneId.systemDefault()).toInstant());
    }
}
