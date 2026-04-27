package com.sni.bokaticowork.features.booking.service.interfaces;

import com.sni.bokaticowork.features.booking.dto.request.BookingAvailabilityRequest;
import com.sni.bokaticowork.features.booking.dto.response.BookingAvailabilityResponse;

public interface BookingAvailabilityService {

    BookingAvailabilityResponse check(BookingAvailabilityRequest request);
}
