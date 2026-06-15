package com.sni.bokaticowork.features.portal.booking.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.booking.dto.request.BookingAvailabilityRequest;
import com.sni.bokaticowork.features.booking.dto.request.BookingStatusChangeRequest;
import com.sni.bokaticowork.features.booking.dto.request.CreateBookingRequest;
import com.sni.bokaticowork.features.booking.dto.response.BookingAvailabilityResponse;
import com.sni.bokaticowork.features.booking.dto.response.BookingResponse;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingAvailabilityService;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.portal.booking.dto.request.ClientCancelBookingRequest;
import com.sni.bokaticowork.features.portal.booking.dto.request.ClientCheckAvailabilityRequest;
import com.sni.bokaticowork.features.portal.booking.dto.request.ClientCreateBookingRequest;
import com.sni.bokaticowork.features.portal.booking.dto.response.ClientBookingResponse;
import com.sni.bokaticowork.features.portal.booking.dto.response.ClientBookingSummaryResponse;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ClientPortalBookingService {

    private final BookingService bookingService;
    private final BookingAvailabilityService bookingAvailabilityService;

    @Transactional(readOnly = true)
    public PaginatedResponse<ClientBookingSummaryResponse> listBookings(Member member,
                                                                         BookingStatus status,
                                                                         LocalDateTime from,
                                                                         LocalDateTime to,
                                                                         Pageable pageable) {
        PaginatedResponse<BookingResponse> source = bookingService.list(
                null, SubscriberType.MEMBER, member.getMemberId(), null, status, from, to, pageable
        );
        PaginatedResponse<ClientBookingSummaryResponse> result = new PaginatedResponse<>();
        result.setData(source.getData().stream().map(this::toSummary).toList());
        result.setPageable(source.getPageable());
        return result;
    }

    @Transactional(readOnly = true)
    public ClientBookingResponse getBooking(Member member, String bookingNumber) {
        BookingResponse booking = bookingService.get(bookingNumber);
        verifyOwnership(member, booking);
        return toDetailResponse(booking);
    }

    @Transactional
    public ClientBookingResponse createBooking(Member member, ClientCreateBookingRequest request) {
        CreateBookingRequest req = new CreateBookingRequest(
                request.getResourceCode(),
                member.getMemberId(),
                null,
                null,
                member.getEmail(),
                member.getPhone(),
                false,
                member.getFirstname() + " " + member.getLastname(),
                member.getEmail(),
                member.getPhone(),
                request.getIdempotencyKey(),
                null,
                request.getStartedAt(),
                request.getEndedAt(),
                request.getQuantity(),
                request.getPaymentMode(),
                true,
                true,
                request.getNotes(),
                null,
                request.getParticipants()
        );
        return toDetailResponse(bookingService.create(req));
    }

    @Transactional
    public ClientBookingResponse cancelBooking(Member member, String bookingNumber,
                                               ClientCancelBookingRequest request) {
        BookingResponse booking = bookingService.get(bookingNumber);
        verifyOwnership(member, booking);
        if (booking.status() != BookingStatus.CONFIRMED
                && booking.status() != BookingStatus.PENDING_APPROVAL) {
            throw new BadRequestException(
                    "Only CONFIRMED or PENDING_APPROVAL bookings can be cancelled. Current status: " + booking.status()
            );
        }
        String reason = request != null ? request.getReason() : null;
        BookingStatusChangeRequest change = new BookingStatusChangeRequest(member.getMemberId(), reason, true);
        return toDetailResponse(bookingService.cancel(bookingNumber, change));
    }

    public BookingAvailabilityResponse checkAvailability(Member member, ClientCheckAvailabilityRequest request) {
        BookingAvailabilityRequest req = new BookingAvailabilityRequest(
                request.getResourceCode(),
                member.getMemberId(),
                null,
                null,
                member.getEmail(),
                member.getPhone(),
                false,
                null,
                null,
                null,
                null,
                request.getStartedAt(),
                request.getEndedAt(),
                request.getQuantity()
        );
        return bookingAvailabilityService.check(req);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void verifyOwnership(Member member, BookingResponse booking) {
        if (booking.ownerType() != SubscriberType.MEMBER
                || !member.getMemberId().equals(booking.ownerCode())) {
            throw new ResourceNotFoundException("Booking not found");
        }
    }

    private ClientBookingSummaryResponse toSummary(BookingResponse b) {
        return ClientBookingSummaryResponse.builder()
                .bookingNumber(b.bookingNumber())
                .resourceCode(b.resourceCode())
                .resourceName(b.resourceName())
                .resourceTypeCode(b.resourceTypeCode())
                .resourceGroupCode(b.resourceGroupCode())
                .status(b.status())
                .startedAt(b.startedAt())
                .endedAt(b.endedAt())
                .durationMinutes(b.durationMinutes())
                .quantity(b.quantity())
                .bookingUnit(b.bookingUnit())
                .paymentMode(b.paymentMode())
                .totalAmount(b.totalAmount())
                .currency(b.currency())
                .confirmedAt(b.confirmedAt())
                .cancelledAt(b.cancelledAt())
                .completedAt(b.completedAt())
                .createdAt(b.createdAt())
                .build();
    }

    private ClientBookingResponse toDetailResponse(BookingResponse b) {
        return ClientBookingResponse.builder()
                .bookingNumber(b.bookingNumber())
                .resourceCode(b.resourceCode())
                .resourceName(b.resourceName())
                .resourceTypeCode(b.resourceTypeCode())
                .resourceGroupCode(b.resourceGroupCode())
                .status(b.status())
                .startedAt(b.startedAt())
                .endedAt(b.endedAt())
                .durationMinutes(b.durationMinutes())
                .quantity(b.quantity())
                .bookingUnit(b.bookingUnit())
                .paymentMode(b.paymentMode())
                .subscriptionNumber(b.subscriptionNumber())
                .passNumber(b.passNumber())
                .entitlementCode(b.entitlementCode())
                .unitPrice(b.unitPrice())
                .subtotalAmount(b.subtotalAmount())
                .totalAmount(b.totalAmount())
                .currency(b.currency())
                .notes(b.notes())
                .checkInToken(b.checkInToken())
                .checkInQrValue(b.checkInQrValue())
                .virtualMeetingUrl(b.virtualMeetingUrl())
                .confirmedAt(b.confirmedAt())
                .completedAt(b.completedAt())
                .cancelledAt(b.cancelledAt())
                .createdAt(b.createdAt())
                .lines(b.lines())
                .participants(b.participants())
                .build();
    }
}
