package com.sni.bokaticowork.features.booking.service.interfaces;

import com.sni.bokaticowork.features.booking.dto.request.BookingAvailabilityRequest;
import com.sni.bokaticowork.features.booking.dto.response.BookingSuggestionResponse;

import java.util.List;

public interface BookingSuggestionService {
    List<BookingSuggestionResponse> suggest(BookingAvailabilityRequest request);
}
