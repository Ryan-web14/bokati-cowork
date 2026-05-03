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
}
