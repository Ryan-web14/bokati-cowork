package com.sni.bokaticowork.features.booking.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.booking.dto.request.BookingStatusChangeRequest;
import com.sni.bokaticowork.features.booking.dto.request.BookingApprovalRequest;
import com.sni.bokaticowork.features.booking.dto.request.BookingCheckRequest;
import com.sni.bokaticowork.features.booking.dto.request.BookingParticipantRequest;
import com.sni.bokaticowork.features.booking.dto.request.CreateRecurringBookingRequest;
import com.sni.bokaticowork.features.booking.dto.request.CreateBookingRequest;
import com.sni.bokaticowork.features.booking.dto.response.BookingEventResponse;
import com.sni.bokaticowork.features.booking.dto.response.BookingParticipantResponse;
import com.sni.bokaticowork.features.booking.dto.response.BookingResponse;
import com.sni.bokaticowork.features.booking.dto.response.BookingStatusHistoryResponse;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingService {

    BookingResponse create(CreateBookingRequest request);

    List<BookingResponse> createRecurring(CreateRecurringBookingRequest request);

    BookingResponse get(String bookingNumber);

    PaginatedResponse<BookingResponse> list(String query,
                                            SubscriberType ownerType,
                                            String ownerCode,
                                            String resourceCode,
                                            BookingStatus status,
                                            LocalDateTime startedFrom,
                                            LocalDateTime startedTo,
                                            Pageable pageable);

    List<BookingResponse> basicSearch(String query);

    BookingResponse confirm(String bookingNumber, BookingStatusChangeRequest request);

    BookingResponse approve(String bookingNumber, BookingApprovalRequest request);

    BookingResponse reject(String bookingNumber, BookingApprovalRequest request);

    BookingResponse start(String bookingNumber, BookingStatusChangeRequest request);

    BookingResponse complete(String bookingNumber, BookingStatusChangeRequest request);

    BookingResponse cancel(String bookingNumber, BookingStatusChangeRequest request);

    BookingResponse noShow(String bookingNumber, BookingStatusChangeRequest request);

    BookingResponse checkIn(String bookingNumber, BookingCheckRequest request);

    BookingResponse checkOut(String bookingNumber, BookingCheckRequest request);

    BookingParticipantResponse addParticipant(String bookingNumber, BookingParticipantRequest request);

    BookingParticipantResponse removeParticipant(String bookingNumber, Long participantId);

    List<BookingStatusHistoryResponse> history(String bookingNumber);

    List<BookingEventResponse> events(String bookingNumber);
}
