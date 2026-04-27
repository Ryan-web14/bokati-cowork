package com.sni.bokaticowork.features.booking.mapper.decorator;

import com.sni.bokaticowork.features.booking.dto.request.BookingParticipantRequest;
import com.sni.bokaticowork.features.booking.dto.request.CreateBookingRequest;
import com.sni.bokaticowork.features.booking.dto.response.*;
import com.sni.bokaticowork.features.booking.enums.BookingParticipantRole;
import com.sni.bokaticowork.features.booking.enums.BookingParticipantStatus;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.mapper.interfaces.BookingMapper;
import com.sni.bokaticowork.features.booking.model.*;
import com.sni.bokaticowork.features.booking.repository.BookingLineRepository;
import com.sni.bokaticowork.features.booking.repository.BookingParticipantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public abstract class BookingMapperDecorator implements BookingMapper {

    @Autowired
    private BookingLineRepository lineRepository;

    @Autowired
    private BookingParticipantRepository participantRepository;

    @Override
    public Booking toEntity(CreateBookingRequest request) {
        return Booking.builder()
                .contactName(trim(request.contactName()))
                .contactEmail(trim(request.contactEmail()))
                .contactPhone(trim(request.contactPhone()))
                .startedAt(request.startedAt())
                .endedAt(request.endedAt())
                .quantity(request.quantity() == null ? 1 : request.quantity())
                .paymentMode(request.paymentMode())
                .notes(trim(request.notes()))
                .metadataJson(trim(request.metadataJson()))
                .status(BookingStatus.DRAFT)
                .build();
    }

    @Override
    public BookingParticipant toEntity(BookingParticipantRequest request) {
        return BookingParticipant.builder()
                .memberCode(trim(request.memberCode()))
                .name(trim(request.name()))
                .email(trim(request.email()))
                .phone(trim(request.phone()))
                .role(request.role() == null ? BookingParticipantRole.GUEST : request.role())
                .status(BookingParticipantStatus.INVITED)
                .build();
    }

    @Override
    public BookingResponse toResponse(Booking booking) {
        List<BookingLineResponse> lines = booking.getId() == null
                ? List.of()
                : lineRepository.findByBookingId(booking.getId()).stream().map(this::toResponse).toList();
        List<BookingParticipantResponse> participants = booking.getId() == null
                ? List.of()
                : participantRepository.findByBookingId(booking.getId()).stream().map(this::toResponse).toList();
        return new BookingResponse(
                booking.getBookingNumber(),
                booking.getResource().getCode(),
                booking.getResource().getName(),
                booking.getResource().getResourceType() == null ? null : booking.getResource().getResourceType().getCode(),
                booking.getResource().getResourceGroup() == null ? null : booking.getResource().getResourceGroup().getCode(),
                booking.getOwnerType(),
                booking.getOwnerCode(),
                booking.getContactName(),
                booking.getContactEmail(),
                booking.getContactPhone(),
                booking.getStatus(),
                booking.getStartedAt(),
                booking.getEndedAt(),
                booking.getDurationMinutes(),
                booking.getQuantity(),
                booking.getBookingUnit(),
                booking.getPaymentMode(),
                booking.getSubscriptionNumber(),
                booking.getPassNumber(),
                booking.getEntitlementCode(),
                booking.getUnitPrice(),
                booking.getSubtotalAmount(),
                booking.getTotalAmount(),
                booking.getCurrency(),
                booking.getNotes(),
                booking.getMetadataJson(),
                booking.getConfirmedAt(),
                booking.getCompletedAt(),
                booking.getCancelledAt(),
                booking.getCreatedAt(),
                lines,
                participants
        );
    }

    @Override
    public BookingLineResponse toResponse(BookingLine line) {
        return new BookingLineResponse(
                line.getId(),
                line.getLineType(),
                line.getDescription(),
                line.getQuantity(),
                line.getUnit(),
                line.getUnitPrice(),
                line.getAmount(),
                line.getCurrency(),
                line.getEntitlementCode(),
                line.getBillableNumber()
        );
    }

    @Override
    public BookingParticipantResponse toResponse(BookingParticipant participant) {
        return new BookingParticipantResponse(
                participant.getId(),
                participant.getMemberCode(),
                participant.getName(),
                participant.getEmail(),
                participant.getPhone(),
                participant.getRole(),
                participant.getStatus()
        );
    }

    @Override
    public BookingStatusHistoryResponse toResponse(BookingStatusHistory history) {
        return new BookingStatusHistoryResponse(
                history.getFromStatus(),
                history.getToStatus(),
                history.getChangedBy(),
                history.getReason(),
                history.getChangedAt()
        );
    }

    @Override
    public BookingEventResponse toResponse(BookingEvent event) {
        return new BookingEventResponse(
                event.getEventNumber(),
                event.getBooking().getBookingNumber(),
                event.getEventType(),
                event.getTitle(),
                event.getDescription(),
                event.getPayloadJson(),
                event.getCreatedAt()
        );
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
