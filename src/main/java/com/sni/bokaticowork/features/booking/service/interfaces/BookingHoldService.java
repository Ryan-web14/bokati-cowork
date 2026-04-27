package com.sni.bokaticowork.features.booking.service.interfaces;

import com.sni.bokaticowork.features.booking.dto.request.CreateBookingHoldRequest;
import com.sni.bokaticowork.features.booking.dto.response.BookingHoldResponse;

public interface BookingHoldService {
    BookingHoldResponse create(CreateBookingHoldRequest request);
    BookingHoldResponse release(String holdNumber);
    int expireDue(int limit);
}
