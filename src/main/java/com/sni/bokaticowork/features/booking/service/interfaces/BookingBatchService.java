package com.sni.bokaticowork.features.booking.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.booking.dto.request.CreateBookingBatchRequest;
import com.sni.bokaticowork.features.booking.dto.response.BookingBatchResponse;
import org.springframework.data.domain.Pageable;

/** Reservation multi-ressources : un lot, une facture par membre. */
public interface BookingBatchService {

    BookingBatchResponse create(CreateBookingBatchRequest request);

    BookingBatchResponse get(String batchNumber);

    PaginatedResponse<BookingBatchResponse.Summary> list(Pageable pageable);
}
