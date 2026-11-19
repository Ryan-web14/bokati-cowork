package com.sni.bokaticowork.features.booking.mapper.decorator;

import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.core.generator.qrcode.QrCodeRenderer;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.booking.dto.request.BookingParticipantRequest;
import com.sni.bokaticowork.features.booking.dto.request.CreateBookingRequest;
import com.sni.bokaticowork.features.booking.dto.response.*;
import com.sni.bokaticowork.features.booking.enums.BookingParticipantRole;
import com.sni.bokaticowork.features.booking.enums.BookingParticipantStatus;
import com.sni.bokaticowork.features.booking.enums.BookingPaymentStatus;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.mapper.interfaces.BookingMapper;
import com.sni.bokaticowork.features.booking.model.*;
import com.sni.bokaticowork.features.booking.repository.BookingLineRepository;
import com.sni.bokaticowork.features.booking.repository.BookingParticipantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public abstract class BookingMapperDecorator implements BookingMapper {

    @Autowired
    private BookingLineRepository lineRepository;

    @Autowired
    private BookingParticipantRepository participantRepository;

    @Autowired
    private QrCodeRenderer qrCodeRenderer;

    @Value("${app.api-base-url:https://api.elleaose.com}")
    private String apiBaseUrl;

    private PlatformSettings platformSettings;

    @Autowired(required = false)
    void setPlatformSettings(PlatformSettings platformSettings) {
        this.platformSettings = platformSettings;
    }

    private static final String API_BASE_URL_KEY = "app.api-base-url";

    private String apiBaseUrl() {
        return platformSettings == null ? apiBaseUrl : platformSettings.text(API_BASE_URL_KEY, apiBaseUrl);
    }

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

    private com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository documents;

    @Autowired(required = false)
    void setDocuments(com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository documents) {
        this.documents = documents;
    }

    /** Lignes qui s'ajoutent au prix de la ressource (lot B7, ecart Bn-7). */
    static final java.util.Set<com.sni.bokaticowork.features.booking.enums.BookingLineType> EXTRA_LINES = java.util.EnumSet.of(
            com.sni.bokaticowork.features.booking.enums.BookingLineType.SERVICE,
            com.sni.bokaticowork.features.booking.enums.BookingLineType.AMENITY,
            com.sni.bokaticowork.features.booking.enums.BookingLineType.PARTICIPANT,
            com.sni.bokaticowork.features.booking.enums.BookingLineType.DEPOSIT,
            com.sni.bokaticowork.features.booking.enums.BookingLineType.OVERRUN,
            com.sni.bokaticowork.features.booking.enums.BookingLineType.NO_SHOW);

    /** Fiche : QR et facture compris. */
    @Override
    public BookingResponse toResponse(Booking booking) {
        List<BookingLine> lines = booking.getId() == null ? List.of() : lineRepository.findByBookingId(booking.getId());
        List<BookingParticipant> participants = booking.getId() == null ? List.of() : participantRepository.findByBookingId(booking.getId());
        String invoice = null;
        if (documents != null && StringUtils.hasText(booking.getBillableNumber())) {
            invoice = documents.findFirstBySourceAndType("BILLABLE_ITEM", booking.getBillableNumber(), "INVOICE")
                    .map(com.sni.bokaticowork.features.billing.model.BillingDocument::getDocumentNumber).orElse(null);
        }
        String qr = booking.getCheckInToken() == null ? null
                : generateQrCode(apiBaseUrl().stripTrailing() + ApiPath.V1 + "/public/bookings/check-in/scan/" + booking.getCheckInToken());
        return build(booking, lines, participants, qr, invoice);
    }

    /** Liste : ni QR (image) ni facture ; lignes et participants de la page en deux requetes. */
    @Override
    public List<BookingResponse> toListResponses(List<Booking> bookings) {
        List<Long> ids = bookings.stream().map(Booking::getId).filter(java.util.Objects::nonNull).toList();
        java.util.Map<Long, List<BookingLine>> lines = ids.isEmpty() ? java.util.Map.of()
                : lineRepository.findByBookingIdIn(ids).stream().collect(java.util.stream.Collectors.groupingBy(line -> line.getBooking().getId()));
        java.util.Map<Long, List<BookingParticipant>> participants = ids.isEmpty() ? java.util.Map.of()
                : participantRepository.findByBookingIdIn(ids).stream()
                .collect(java.util.stream.Collectors.groupingBy(participant -> participant.getBooking().getId()));
        return bookings.stream().map(booking -> build(booking, lines.getOrDefault(booking.getId(), List.of()),
                participants.getOrDefault(booking.getId(), List.of()), null, null)).toList();
    }

    private BookingResponse build(Booking booking, List<BookingLine> lineEntities, List<BookingParticipant> participantEntities,
                                  String qr, String invoiceNumber) {
        List<BookingLineResponse> lines = lineEntities.stream().map(this::toResponse).toList();
        List<BookingParticipantResponse> participants = participantEntities.stream().map(this::toResponse).toList();
        java.math.BigDecimal extras = lineEntities.stream().filter(line -> EXTRA_LINES.contains(line.getLineType()))
                .map(line -> line.getAmount() == null ? java.math.BigDecimal.ZERO : line.getAmount())
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        java.math.BigDecimal grandTotal = (booking.getTotalAmount() == null ? java.math.BigDecimal.ZERO : booking.getTotalAmount()).add(extras);
        BookingPaymentStatus paymentStatus = BookingPaymentStatus.of(
                booking.getPaymentMode(), booking.getStatus(), booking.getPaidAt());
        com.sni.bokaticowork.features.booking.service.support.BookingSeats seats =
                com.sni.bokaticowork.features.booking.service.support.BookingSeats.of(booking);
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
                paymentStatus,
                paymentStatus.getLabel(),
                booking.getSubscriptionNumber(),
                booking.getPassNumber(),
                booking.getEntitlementCode(),
                booking.getUnitPrice(),
                booking.getSubtotalAmount(),
                booking.getTotalAmount(),
                booking.getCurrency(),
                booking.getNotes(),
                booking.getMetadataJson(),
                booking.getCheckInToken(),
                qr,
                booking.getVirtualMeetingUrl(),
                booking.getConfirmedAt(),
                booking.getCompletedAt(),
                booking.getCancelledAt(),
                booking.getCreatedAt(),
                lines,
                participants,
                seats.resourceCapacity(),
                seats.wholeResource(),
                seats.label(),
                booking.getBatchNumber(),
                booking.getModificationCount() == null ? 0 : booking.getModificationCount(),
                booking.getLastModifiedAt(),
                booking.getLastModification(),
                booking.getCreatedBy(),
                booking.getChannel(),
                booking.getHoldNumber(),
                booking.getRecurrenceGroupNumber(),
                booking.getPaymentDueAt(),
                booking.getPaidAt(),
                booking.getCheckedInAt(),
                booking.getCheckedOutAt(),
                booking.getApprovedBy(),
                booking.getApprovedAt(),
                booking.getRejectedBy(),
                booking.getRejectedAt(),
                booking.getRejectionReason(),
                booking.getRejectionReasonCode(),
                booking.getCancellationReason(),
                booking.getCancellationReasonCode(),
                invoiceNumber,
                extras,
                grandTotal,
                Boolean.TRUE.equals(booking.getPayOnSite()),
                booking.getOriginalAmount(),
                booking.getPriceAdjustmentAmount(),
                booking.getPriceAdjustmentRate(),
                booking.getPriceAdjustmentReason(),
                booking.getPriceAdjustmentReasonCode(),
                booking.getPriceAdjustmentBy(),
                booking.getPriceAdjustmentStatus(),
                booking.getPriceAdjustmentApprovalNumber(),
                booking.getLeadNumber(),
                booking.getCallNote(),
                booking.getAttendanceAnswer(),
                booking.getAttendanceAnsweredAt(),
                booking.getReleasedEarlyAt(),
                booking.getDepositAmount(),
                booking.getDepositPaidAt(),
                booking.getRoomLayout(),
                booking.getSpecialRequests(),
                booking.getInternalNotes(),
                com.sni.bokaticowork.features.booking.service.support.BookingGuests.tags(booking)
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
                participant.getStatus(),
                participant.getInvitedAt(),
                participant.getRespondedAt(),
                participant.getCheckedInAt()
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

    /**
     * Rendu delegue au composant partage · le meme code vivait ici en methode privee, et une
     * seconde copie serait apparue des qu'un autre module a eu besoin d'un QR.
     */
    private String generateQrCode(String content) {
        return qrCodeRenderer.toDataUri(content);
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
