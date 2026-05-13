package com.sni.bokaticowork.features.booking.worker;

import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingNoShowWorker {

    private final BookingService bookingService;

    @Scheduled(fixedDelayString = "${bokati.booking.no-show.worker-delay-ms:60000}")
    public void markOverdueNoShows() {
        int marked = bookingService.markOverdueNoShows(200);
        if (marked > 0) {
            log.info("Marked {} overdue bookings as no-show", marked);
        }
    }
}
