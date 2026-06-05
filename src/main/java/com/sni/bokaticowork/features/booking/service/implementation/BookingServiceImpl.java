package com.sni.bokaticowork.features.booking.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.utils.code.CodeComposer;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.booking.dto.request.BookingApprovalRequest;
import com.sni.bokaticowork.features.booking.dto.request.BookingCheckRequest;
import com.sni.bokaticowork.features.booking.dto.request.BookingParticipantRequest;
import com.sni.bokaticowork.features.booking.dto.request.BookingStatusChangeRequest;
import com.sni.bokaticowork.features.booking.dto.request.CreateRecurringBookingRequest;
import com.sni.bokaticowork.features.booking.dto.request.CreateBookingRequest;
import com.sni.bokaticowork.features.booking.dto.response.*;
import com.sni.bokaticowork.features.booking.enums.*;
import com.sni.bokaticowork.features.booking.mapper.interfaces.BookingMapper;
import com.sni.bokaticowork.features.booking.model.*;
import com.sni.bokaticowork.features.booking.repository.*;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import com.sni.bokaticowork.features.booking.service.support.*;
import com.sni.bokaticowork.features.ressource.dto.request.ReleaseResourceAvailabilityRequest;
import com.sni.bokaticowork.features.ressource.dto.request.ReserveResourceAvailabilityRequest;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourcePolicy;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceAvailabilityService;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceService;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.security.SecureRandom;

@Service
@Transactional
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final char[] TOKEN_CHARS = "ABCDEFGHJKLMNPRSTUVWXY23456789".toCharArray();
    private static final SecureRandom TOKEN_RANDOM = new SecureRandom();

    private final BookingRepository bookingRepository;
    private final BookingLineRepository lineRepository;
    private final BookingParticipantRepository participantRepository;
    private final BookingStatusHistoryRepository historyRepository;
    private final BookingEventRepository eventRepository;
    private final BookingRecurrenceGroupRepository recurrenceGroupRepository;
    private final BookingHoldRepository holdRepository;
    private final ResourceService resourceService;
    private final ResourceAvailabilityService resourceAvailabilityService;
    private final BookingMapper bookingMapper;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final BookingIdentityResolver identityResolver;
    private final BookingPaymentContextResolver paymentContextResolver;
    private final BookingPricingCalculator pricingCalculator;
    private final BookingResourceGuard resourceGuard;
    private final BookingEntitlementBridge entitlementBridge;
    private final BookingEventWriter eventWriter;
    private final BookingEmailNotifier emailNotifier;
    private final BookingPolicyEnforcer policyEnforcer;
    private final BookingBillableBridge billableBridge;
    private final BookingVirtualMeetingSupport virtualMeetingSupport;

    @Override
    public BookingResponse create(CreateBookingRequest request) {
        return bookingMapper.toResponse(createInternal(request, null));
    }

    private Booking createInternal(CreateBookingRequest request, BookingIdentityResolver.ResolvedBookingIdentity suppliedIdentity) {
        if (StringUtils.hasText(request.idempotencyKey())) {
            Booking existing = bookingRepository.findByIdempotencyKey(request.idempotencyKey().trim()).orElse(null);
            if (existing != null) {
                return existing;
            }
        }

        Resource resource = resourceService.getResourceForService(request.resourceCode().trim());
        int quantity = request.quantity() == null ? 1 : request.quantity();
        resourceGuard.validateBookable(resource, request.startedAt(), request.endedAt(), quantity);
        BookingIdentityResolver.ResolvedBookingIdentity identity = suppliedIdentity == null
                ? identityResolver.resolve(request.identityLookup())
                : suppliedIdentity;
        BookingPaymentContextResolver.BookingPaymentContext paymentContext = paymentContextResolver.resolve(request.paymentMode(), identity, resource);
        BookingPolicyEnforcer.EffectivePolicy effectivePolicy = policyEnforcer.enforce(identity.ownerType(), identity.ownerCode(), resource, request.startedAt(), request.endedAt());

        Booking booking = bookingMapper.toEntity(request);
        String resourceTypeName = resource.getResourceType() != null ? resource.getResourceType().getName() : "RES";
        long bookingSeq = CodeComposer.extractSeq(sequenceGenerator.next("booking"));
        booking.setBookingNumber(CodeComposer.withDay("BKG", CodeComposer.abbrev(resourceTypeName), LocalDate.now(), bookingSeq));
        booking.setIdempotencyKey(trim(request.idempotencyKey()));
        booking.setHoldNumber(trim(request.holdNumber()));
        booking.setResource(resource);
        booking.setOwnerType(identity.ownerType());
        booking.setOwnerCode(identity.ownerCode());
        booking.setSubscriptionNumber(paymentContext.subscriptionNumber());
        booking.setPassNumber(paymentContext.passNumber());
        booking.setEntitlementCode(paymentContext.entitlementCode());
        booking.setDurationMinutes(Math.toIntExact(Duration.between(request.startedAt(), request.endedAt()).toMinutes()));
        booking.setContactName(identity.contactName());
        booking.setContactEmail(identity.contactEmail());
        booking.setContactPhone(identity.contactPhone());
        booking.setCheckInToken(generateCheckInToken());

        BookingPricingCalculator.Price price = pricingCalculator.calculate(resource, request.startedAt(), request.endedAt(), quantity);
        booking.setBookingUnit(price.unit());
        booking.setUnitPrice(price.unitPrice());
        booking.setSubtotalAmount(price.amount());
        booking.setTotalAmount(price.amount());
        booking.setCurrency(price.currency());
        booking.setApprovalRequired(effectivePolicy.approvalRequired());
        booking.setVirtualMeetingUrl(virtualMeetingSupport.meetingUrl(resource, booking.getBookingNumber()));
        if (effectivePolicy.approvalRequired()) {
            booking.setStatus(BookingStatus.PENDING_APPROVAL);
        }
        booking = bookingRepository.save(booking);

        saveLines(booking, price);
        saveParticipants(booking, request);
        writeHistory(booking, null, BookingStatus.DRAFT, null, "Booking created");
        eventWriter.write(booking, BookingEventType.BOOKING_CREATED, "Booking created", "Booking was created", null);

        if (!effectivePolicy.approvalRequired() && (request.confirmImmediately() == null || request.confirmImmediately())) {
            booking = confirmInternal(booking, new BookingStatusChangeRequest(null, "Auto confirmation", request.sendEmail()));
        } else if (effectivePolicy.approvalRequired()) {
            writeHistory(booking, BookingStatus.DRAFT, BookingStatus.PENDING_APPROVAL, null, "Approval required");
            eventWriter.write(booking, BookingEventType.BOOKING_CREATED, "Booking pending approval", "Booking requires approval", null);
        }

        return booking;
    }

    @Override
    public List<BookingResponse> createRecurring(CreateRecurringBookingRequest request) {
        int occurrences = request.occurrences() == null ? 2 : request.occurrences();
        int interval = request.intervalValue() == null ? 1 : request.intervalValue();
        String groupNumber = sequenceGenerator.next("booking_recurrence_group");
        CreateBookingRequest base = request.booking();
        BookingIdentityResolver.ResolvedBookingIdentity identity = identityResolver.resolve(base.identityLookup());
        Set<LocalDate> excludedDates = request.excludedDates() == null
                ? Set.of()
                : new HashSet<>(request.excludedDates());
        recurrenceGroupRepository.save(BookingRecurrenceGroup.builder()
                .groupNumber(groupNumber)
                .resourceCode(base.resourceCode())
                .ownerType(identity.ownerType())
                .ownerCode(identity.ownerCode())
                .frequency(request.frequency())
                .intervalValue(interval)
                .occurrences(occurrences)
                .endDate(request.endDate())
                .excludedDatesJson(excludedDatesJson(excludedDates))
                .firstStartAt(base.startedAt())
                .firstEndAt(base.endedAt())
                .build());
        List<BookingResponse> created = new java.util.ArrayList<>();
        int index = 0;
        while (created.size() < occurrences && index < 370) {
            CreateBookingRequest occurrence = shift(base, request.frequency(), interval * index);
            index++;
            if (request.endDate() != null && occurrence.startedAt().toLocalDate().isAfter(request.endDate())) {
                break;
            }
            if (excludedDates.contains(occurrence.startedAt().toLocalDate())) {
                continue;
            }
            Booking booking = createInternal(occurrence, identity);
            booking.setRecurrenceGroupNumber(groupNumber);
            bookingRepository.save(booking);
            created.add(bookingMapper.toResponse(booking));
        }
        return created;
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse get(String bookingNumber) {
        return bookingMapper.toResponse(getForService(bookingNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<BookingResponse> list(String query,
                                                   SubscriberType ownerType,
                                                   String ownerCode,
                                                   String resourceCode,
                                                   BookingStatus status,
                                                   LocalDateTime startedFrom,
                                                   LocalDateTime startedTo,
                                                   Pageable pageable) {
        String normalizedQuery = StringUtils.hasText(query) ? query.trim() : null;
        Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<BookingResponse> page = bookingRepository.nativeSearch(
                normalizedQuery,
                ownerType == null ? null : ownerType.name(),
                trim(ownerCode),
                trim(resourceCode),
                status == null ? null : status.name(),
                startedFrom,
                startedTo,
                unsortedPageable
        ).map(bookingMapper::toResponse);
        return new PaginatedResponse<>(page);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> basicSearch(String query) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        return bookingRepository.basicSearch(query.trim()).stream().map(bookingMapper::toResponse).toList();
    }

    @Override
    public BookingResponse confirm(String bookingNumber, BookingStatusChangeRequest request) {
        return bookingMapper.toResponse(confirmInternal(getForService(bookingNumber), request));
    }

    @Override
    public BookingResponse approve(String bookingNumber, BookingApprovalRequest request) {
        Booking booking = getForService(bookingNumber);
        requireStatus(booking, BookingStatus.PENDING_APPROVAL);
        booking.setApprovedBy(request == null ? null : trim(request.actor()));
        booking.setApprovedAt(Instant.now());
        booking.setStatus(BookingStatus.DRAFT);
        booking = bookingRepository.save(booking);
        writeHistory(booking, BookingStatus.PENDING_APPROVAL, BookingStatus.DRAFT, booking.getApprovedBy(), request == null ? "Approved" : reason(request.reason(), "Approved"));
        eventWriter.write(booking, BookingEventType.BOOKING_APPROVED, "Booking approved", "Booking was approved", null);
        return bookingMapper.toResponse(confirmInternal(booking, new BookingStatusChangeRequest(booking.getApprovedBy(), "Approved", request == null ? null : request.sendEmail())));
    }

    @Override
    public BookingResponse reject(String bookingNumber, BookingApprovalRequest request) {
        Booking booking = getForService(bookingNumber);
        requireStatus(booking, BookingStatus.PENDING_APPROVAL);
        BookingStatus from = booking.getStatus();
        booking.setStatus(BookingStatus.REJECTED);
        booking.setRejectedBy(request == null ? null : trim(request.actor()));
        booking.setRejectedAt(Instant.now());
        booking.setRejectionReason(request == null ? "Rejected" : reason(request.reason(), "Rejected"));
        booking = bookingRepository.save(booking);
        writeHistory(booking, from, BookingStatus.REJECTED, booking.getRejectedBy(), booking.getRejectionReason());
        eventWriter.write(booking, BookingEventType.BOOKING_REJECTED, "Booking rejected", booking.getRejectionReason(), null);
        notifyIfRequested(booking, BookingEventType.BOOKING_REJECTED, new BookingStatusChangeRequest(booking.getRejectedBy(), booking.getRejectionReason(), request == null ? null : request.sendEmail()));
        return bookingMapper.toResponse(booking);
    }

    @Override
    public BookingResponse start(String bookingNumber, BookingStatusChangeRequest request) {
        Booking booking = getForService(bookingNumber);
        requireStatus(booking, BookingStatus.CONFIRMED);
        assertActivationAllowed(booking, "start");
        BookingStatus from = booking.getStatus();
        booking.setStatus(BookingStatus.IN_PROGRESS);
        booking.setStartedEventAt(Instant.now());
        booking = bookingRepository.save(booking);
        writeHistory(booking, from, BookingStatus.IN_PROGRESS, changedBy(request), reason(request, "Booking started"));
        eventWriter.write(booking, BookingEventType.BOOKING_STARTED, "Booking started", "Booking is in progress", null);
        notifyIfRequested(booking, BookingEventType.BOOKING_STARTED, request);
        return bookingMapper.toResponse(booking);
    }

    @Override
    public BookingResponse complete(String bookingNumber, BookingStatusChangeRequest request) {
        return bookingMapper.toResponse(completeInternal(getForService(bookingNumber), request));
    }

    @Override
    public BookingResponse cancel(String bookingNumber, BookingStatusChangeRequest request) {
        return bookingMapper.toResponse(cancelInternal(getForService(bookingNumber), request, false));
    }

    @Override
    public BookingResponse systemCancel(String bookingNumber, String reason) {
        return bookingMapper.toResponse(cancelInternal(
                getForService(bookingNumber),
                new BookingStatusChangeRequest("SYSTEM", reason, Boolean.FALSE),
                true
        ));
    }

    @Override
    public BookingResponse noShow(String bookingNumber, BookingStatusChangeRequest request) {
        return bookingMapper.toResponse(markNoShowInternal(getForService(bookingNumber), request));
    }

    @Override
    public int markOverdueNoShows(int limit) {
        int resolvedLimit = Math.max(1, limit);
        LocalDateTime now = LocalDateTime.now();
        List<Booking> candidates = bookingRepository.findOverdueNoShowCandidates(now, resolvedLimit);
        int marked = 0;
        for (Booking booking : candidates) {
            if (isOverdueNoShowCandidate(booking, now)) {
                markNoShowInternal(booking, new BookingStatusChangeRequest("SYSTEM", "Automatic no-show: booking ended without check-in or admin start", Boolean.FALSE));
                marked++;
            }
        }
        return marked;
    }

    @Override
    public int markOverdueCompleted(int limit) {
        int resolvedLimit = Math.max(1, limit);
        LocalDateTime now = LocalDateTime.now();
        List<Booking> candidates = bookingRepository.findOverdueCompletionCandidates(now, resolvedLimit);
        int completed = 0;
        for (Booking booking : candidates) {
            if (booking.getStatus() == BookingStatus.IN_PROGRESS
                    && booking.getEndedAt().isBefore(now)
                    && (booking.getCheckedInAt() != null || booking.getStartedEventAt() != null)) {
                completeInternal(booking, new BookingStatusChangeRequest("SYSTEM", "Automatic completion: booking ended after check-in", Boolean.FALSE));
                completed++;
            }
        }
        return completed;
    }

    @Override
    public BookingResponse checkIn(String bookingNumber, BookingCheckRequest request) {
        Booking booking = getForService(bookingNumber);
        return checkInInternal(booking, request);
    }

    @Override
    public BookingResponse checkInByToken(String checkInToken, BookingCheckRequest request) {
        if (!StringUtils.hasText(checkInToken)) {
            throw new BadRequestException("Check-in token is required");
        }
        Booking booking = bookingRepository.findByCheckInToken(checkInToken.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Booking check-in token not found"));
        return checkInInternal(booking, request);
    }

    private BookingResponse checkInInternal(Booking booking, BookingCheckRequest request) {
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new ConflictException("booking", "only confirmed or in-progress bookings can be checked in");
        }
        assertActivationAllowed(booking, "check in");
        booking.setCheckedInAt(Instant.now());
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            booking.setStatus(BookingStatus.IN_PROGRESS);
        }
        booking = bookingRepository.save(booking);
        eventWriter.write(booking, BookingEventType.BOOKING_CHECKED_IN, "Booking checked in", request == null ? null : request.note(), null);
        notifyIfRequested(booking, BookingEventType.BOOKING_CHECKED_IN, new BookingStatusChangeRequest(request == null ? null : request.actor(), request == null ? null : request.note(), request == null ? null : request.sendEmail()));
        return bookingMapper.toResponse(booking);
    }

    @Override
    public BookingResponse checkOut(String bookingNumber, BookingCheckRequest request) {
        Booking booking = getForService(bookingNumber);
        if (booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new ConflictException("booking", "only in-progress bookings can be checked out");
        }
        booking.setCheckedOutAt(Instant.now());
        booking = bookingRepository.save(booking);
        eventWriter.write(booking, BookingEventType.BOOKING_CHECKED_OUT, "Booking checked out", request == null ? null : request.note(), null);
        notifyIfRequested(booking, BookingEventType.BOOKING_CHECKED_OUT, new BookingStatusChangeRequest(request == null ? null : request.actor(), request == null ? null : request.note(), request == null ? null : request.sendEmail()));
        return bookingMapper.toResponse(booking);
    }

    @Override
    public BookingParticipantResponse addParticipant(String bookingNumber, BookingParticipantRequest request) {
        Booking booking = getForService(bookingNumber);
        BookingParticipant participant = bookingMapper.toEntity(request);
        participant.setBooking(booking);
        participant = participantRepository.save(participant);
        eventWriter.write(booking, BookingEventType.BOOKING_CREATED, "Participant added", participant.getEmail(), null);
        return bookingMapper.toResponse(participant);
    }

    @Override
    public BookingParticipantResponse removeParticipant(String bookingNumber, Long participantId) {
        Booking booking = getForService(bookingNumber);
        BookingParticipant participant = participantRepository.findByIdAndBookingId(participantId, booking.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking participant not found"));
        participant.setStatus(BookingParticipantStatus.REMOVED);
        participant = participantRepository.save(participant);
        eventWriter.write(booking, BookingEventType.BOOKING_CREATED, "Participant removed", participant.getEmail(), null);
        return bookingMapper.toResponse(participant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingStatusHistoryResponse> history(String bookingNumber) {
        Booking booking = getForService(bookingNumber);
        return historyRepository.findByBookingId(booking.getId()).stream().map(bookingMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingEventResponse> events(String bookingNumber) {
        Booking booking = getForService(bookingNumber);
        return eventRepository.findByBookingId(booking.getId()).stream().map(bookingMapper::toResponse).toList();
    }

    private Booking confirmInternal(Booking booking, BookingStatusChangeRequest request) {
        requireStatus(booking, BookingStatus.DRAFT);
        resourceGuard.validateBookable(booking.getResource(), booking.getStartedAt(), booking.getEndedAt(), booking.getQuantity());
        resourceGuard.validateNoSingleCapacityConflict(booking.getResource(), booking.getStartedAt(), booking.getEndedAt(), booking.getBookingNumber());
        boolean confirmedFromHold = confirmHoldIfPresent(booking);
        if (!confirmedFromHold) {
            reserveResource(booking);
            eventWriter.write(booking, BookingEventType.RESOURCE_RESERVED, "Resource reserved", "Booking resource availability was reserved", null);
        }
        if (booking.getPaymentMode() != BookingPaymentMode.DIRECT) {
            entitlementBridge.reserve(booking, entitlementQuantity(booking));
            eventWriter.write(booking, BookingEventType.ENTITLEMENT_RESERVED, "Entitlement reserved", "Booking entitlement was reserved", null);
        }
        booking.setBillableNumber(billableBridge.ensureBillableItem(booking));
        BookingStatus from = booking.getStatus();
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(Instant.now());
        booking = bookingRepository.save(booking);
        writeHistory(booking, from, BookingStatus.CONFIRMED, changedBy(request), reason(request, "Booking confirmed"));
        eventWriter.write(booking, BookingEventType.BOOKING_CONFIRMED, "Booking confirmed", "Booking was confirmed", null);
        notifyIfRequested(booking, BookingEventType.BOOKING_CONFIRMED, request);
        return booking;
    }

    private Booking cancelInternal(Booking booking, BookingStatusChangeRequest request, boolean systemOverride) {
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return booking;
        }
        if (!systemOverride && booking.getStatus() == BookingStatus.COMPLETED) {
            throw new ConflictException("booking", "booking cannot be cancelled from status " + booking.getStatus());
        }
        if (!systemOverride) {
            assertCancellationAllowed(booking);
        }
        if (booking.getStatus() == BookingStatus.CONFIRMED || booking.getStatus() == BookingStatus.IN_PROGRESS) {
            releaseResource(booking);
            eventWriter.write(booking, BookingEventType.RESOURCE_RELEASED, "Resource released", "Booking resource availability was released", null);
            if (booking.getPaymentMode() != BookingPaymentMode.DIRECT) {
                entitlementBridge.release(booking, entitlementQuantity(booking));
                eventWriter.write(booking, BookingEventType.ENTITLEMENT_RELEASED, "Entitlement released", "Booking entitlement reservation was released", null);
            }
        }
        BookingStatus from = booking.getStatus();
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reason(request, "Booking cancelled"));
        booking.setCancelledAt(Instant.now());
        booking = bookingRepository.save(booking);
        writeHistory(booking, from, BookingStatus.CANCELLED, changedBy(request), reason(request, "Booking cancelled"));
        eventWriter.write(booking, BookingEventType.BOOKING_CANCELLED, "Booking cancelled", booking.getCancellationReason(), null);
        notifyIfRequested(booking, BookingEventType.BOOKING_CANCELLED, request);
        return booking;
    }

    private Booking markNoShowInternal(Booking booking, BookingStatusChangeRequest request) {
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new ConflictException("booking", "only confirmed or in-progress bookings can be marked as no-show");
        }
        if (booking.getPaymentMode() != BookingPaymentMode.DIRECT) {
            entitlementBridge.consume(booking, entitlementQuantity(booking));
            eventWriter.write(booking, BookingEventType.ENTITLEMENT_CONSUMED, "Entitlement consumed", "No-show entitlement was consumed", null);
        }
        BookingStatus from = booking.getStatus();
        booking.setStatus(BookingStatus.NO_SHOW);
        booking.setCompletedAt(Instant.now());
        booking = bookingRepository.save(booking);
        writeHistory(booking, from, BookingStatus.NO_SHOW, changedBy(request), reason(request, "No-show"));
        eventWriter.write(booking, BookingEventType.BOOKING_NO_SHOW, "Booking no-show", "Booking was marked as no-show", null);
        notifyIfRequested(booking, BookingEventType.BOOKING_NO_SHOW, request);
        return booking;
    }

    private Booking completeInternal(Booking booking, BookingStatusChangeRequest request) {
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new ConflictException("booking", "only confirmed or in-progress bookings can be completed");
        }
        if (booking.getPaymentMode() != BookingPaymentMode.DIRECT) {
            entitlementBridge.consume(booking, entitlementQuantity(booking));
            eventWriter.write(booking, BookingEventType.ENTITLEMENT_CONSUMED, "Entitlement consumed", "Booking entitlement was consumed", null);
            eventWriter.write(booking, BookingEventType.USAGE_RECORDED, "Usage recorded", "Booking usage was recorded", null);
        }
        BookingStatus from = booking.getStatus();
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(Instant.now());
        booking = bookingRepository.save(booking);
        writeHistory(booking, from, BookingStatus.COMPLETED, changedBy(request), reason(request, "Booking completed"));
        eventWriter.write(booking, BookingEventType.BOOKING_COMPLETED, "Booking completed", "Booking was completed", null);
        notifyIfRequested(booking, BookingEventType.BOOKING_COMPLETED, request);
        return booking;
    }

    private void reserveResource(Booking booking) {
        resourceAvailabilityService.reserve(ReserveResourceAvailabilityRequest.builder()
                .resourceCode(booking.getResource().getCode())
                .startedAt(booking.getStartedAt())
                .endedAt(booking.getEndedAt())
                .quantity(booking.getQuantity())
                .build());
    }

    private boolean confirmHoldIfPresent(Booking booking) {
        if (!StringUtils.hasText(booking.getHoldNumber())) {
            return false;
        }
        BookingHold hold = holdRepository.findByHoldNumber(booking.getHoldNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Booking hold " + booking.getHoldNumber() + " not found"));
        if (hold.getStatus() != BookingHoldStatus.ACTIVE || hold.getExpiresAt().isBefore(Instant.now())) {
            throw new ConflictException("booking hold", "hold is not active");
        }
        if (hold.getOwnerType() != booking.getOwnerType() || !hold.getOwnerCode().equals(booking.getOwnerCode())) {
            throw new ConflictException("booking hold", "hold owner does not match booking owner");
        }
        if (!hold.getResource().getId().equals(booking.getResource().getId())
                || !hold.getStartedAt().equals(booking.getStartedAt())
                || !hold.getEndedAt().equals(booking.getEndedAt())
                || !hold.getQuantity().equals(booking.getQuantity())) {
            throw new ConflictException("booking hold", "hold does not match booking slot");
        }
        hold.setStatus(BookingHoldStatus.CONFIRMED);
        holdRepository.save(hold);
        eventWriter.write(booking, BookingEventType.BOOKING_HOLD_CREATED, "Booking hold confirmed", hold.getHoldNumber(), null);
        return true;
    }

    private void releaseResource(Booking booking) {
        resourceAvailabilityService.release(ReleaseResourceAvailabilityRequest.builder()
                .resourceCode(booking.getResource().getCode())
                .startedAt(booking.getStartedAt())
                .endedAt(booking.getEndedAt())
                .quantity(booking.getQuantity())
                .build());
    }

    private void saveLines(Booking booking, BookingPricingCalculator.Price price) {
        lineRepository.save(BookingLine.builder()
                .booking(booking)
                .lineType(BookingLineType.RESOURCE)
                .description(resourceLineDescription(booking))
                .quantity(price.quantity())
                .unit(price.unit())
                .unitPrice(price.unitPrice())
                .amount(price.amount())
                .currency(price.currency())
                .build());
        if (booking.getPaymentMode() != BookingPaymentMode.DIRECT && StringUtils.hasText(booking.getEntitlementCode())) {
            lineRepository.save(BookingLine.builder()
                    .booking(booking)
                    .lineType(BookingLineType.ENTITLEMENT)
                    .description(entitlementLineDescription(booking))
                    .quantity(entitlementQuantity(booking))
                    .unit(booking.getBookingUnit())
                    .unitPrice(BigDecimal.ZERO)
                    .amount(BigDecimal.ZERO)
                    .currency(booking.getCurrency())
                    .entitlementCode(booking.getEntitlementCode())
                    .build());
        }
    }

    private void saveParticipants(Booking booking, CreateBookingRequest request) {
        if (request.participants() == null) {
            return;
        }
        request.participants().forEach(participant -> {
            BookingParticipant entity = bookingMapper.toEntity(participant);
            entity.setBooking(booking);
            participantRepository.save(entity);
        });
    }

    private void assertCancellationAllowed(Booking booking) {
        ResourcePolicy policy = booking.getResource().getResourcePolicy();
        if (policy == null || booking.getStatus() == BookingStatus.DRAFT) {
            return;
        }
        if (!Boolean.TRUE.equals(policy.getAllowCancellation())) {
            throw new ConflictException("booking", "resource policy does not allow cancellation");
        }
        if (LocalDateTime.now().plusMinutes(policy.getCancellationNoticeMinutes()).isAfter(booking.getStartedAt())) {
            throw new ConflictException("booking", "cancellation notice is not respected");
        }
    }

    private void assertActivationAllowed(Booking booking, String action) {
        if (isCurrentUserAdmin()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(booking.getStartedAt()) || !now.isBefore(booking.getEndedAt())) {
            throw new ForbiddenException("Only admins can " + action + " a booking outside its scheduled time window");
        }
    }

    private boolean isCurrentUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())
                        || "ROLE_SUPER_ADMIN".equals(authority.getAuthority()));
    }

    private boolean isOverdueNoShowCandidate(Booking booking, LocalDateTime now) {
        return (booking.getStatus() == BookingStatus.CONFIRMED || booking.getStatus() == BookingStatus.IN_PROGRESS)
                && booking.getEndedAt().isBefore(now)
                && booking.getCheckedInAt() == null
                && booking.getStartedEventAt() == null;
    }

    private Booking getForService(String bookingNumber) {
        if (!StringUtils.hasText(bookingNumber)) {
            throw new BadRequestException("Booking number is required");
        }
        return bookingRepository.findByBookingNumber(bookingNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Booking " + bookingNumber + " not found"));
    }

    private BigDecimal entitlementQuantity(Booking booking) {
        return pricingCalculator.entitlementQuantity(booking.getBookingUnit(), booking.getStartedAt(), booking.getEndedAt(), booking.getQuantity());
    }

    private void writeHistory(Booking booking, BookingStatus from, BookingStatus to, String changedBy, String reason) {
        historyRepository.save(BookingStatusHistory.builder()
                .booking(booking)
                .fromStatus(from)
                .toStatus(to)
                .changedBy(changedBy)
                .reason(reason)
                .changedAt(Instant.now())
                .build());
    }

    private void requireStatus(Booking booking, BookingStatus expected) {
        if (booking.getStatus() != expected) {
            throw new ConflictException("booking", "expected status " + expected + " but was " + booking.getStatus());
        }
    }

    private void notifyIfRequested(Booking booking, BookingEventType eventType, BookingStatusChangeRequest request) {
        if (request == null || request.sendEmail() == null || request.sendEmail()) {
            emailNotifier.notify(booking, eventType);
        }
    }

    private String changedBy(BookingStatusChangeRequest request) {
        return request == null ? null : trim(request.changedBy());
    }

    private String reason(BookingStatusChangeRequest request, String fallback) {
        return request == null || !StringUtils.hasText(request.reason()) ? fallback : request.reason().trim();
    }

    private String reason(String reason, String fallback) {
        return StringUtils.hasText(reason) ? reason.trim() : fallback;
    }

    private String excludedDatesJson(Set<LocalDate> excludedDates) {
        if (excludedDates == null || excludedDates.isEmpty()) {
            return null;
        }
        return excludedDates.stream()
                .sorted()
                .map(date -> "\"" + date + "\"")
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }

    private CreateBookingRequest shift(CreateBookingRequest base, BookingRecurrenceFrequency frequency, int amount) {
        LocalDateTime start = switch (frequency) {
            case DAILY -> base.startedAt().plusDays(amount);
            case WEEKLY -> base.startedAt().plusWeeks(amount);
            case MONTHLY -> base.startedAt().plusMonths(amount);
        };
        LocalDateTime end = switch (frequency) {
            case DAILY -> base.endedAt().plusDays(amount);
            case WEEKLY -> base.endedAt().plusWeeks(amount);
            case MONTHLY -> base.endedAt().plusMonths(amount);
        };
        return new CreateBookingRequest(
                base.resourceCode(),
                base.memberId(),
                base.customerId(),
                base.businessCode(),
                base.email(),
                base.phone(),
                base.walkIn(),
                base.contactName(),
                base.contactEmail(),
                base.contactPhone(),
                null,
                null,
                start,
                end,
                base.quantity(),
                base.paymentMode(),
                base.confirmImmediately(),
                base.sendEmail(),
                base.notes(),
                base.metadataJson(),
                base.participants()
        );
    }



    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String resourceLineDescription(Booking booking) {
        String base = "Booking resource - " + booking.getResource().getName();
        if (booking.getPaymentMode() == BookingPaymentMode.SUBSCRIPTION) {
            return base + " (Abonne - couvert par abonnement"
                    + (StringUtils.hasText(booking.getSubscriptionNumber()) ? " " + booking.getSubscriptionNumber() : "")
                    + ")";
        }
        if (booking.getPaymentMode() == BookingPaymentMode.PASS) {
            return base + " (Couvert par pass"
                    + (StringUtils.hasText(booking.getPassNumber()) ? " " + booking.getPassNumber() : "")
                    + ")";
        }
        return base;
    }

    private String entitlementLineDescription(Booking booking) {
        if (booking.getPaymentMode() == BookingPaymentMode.SUBSCRIPTION) {
            return "Abonne - consommation de l'abonnement " + booking.getEntitlementCode();
        }
        if (booking.getPaymentMode() == BookingPaymentMode.PASS) {
            return "Pass - consommation du pass " + booking.getEntitlementCode();
        }
        return "Entitlement consumption - " + booking.getEntitlementCode();
    }

    @Async
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordCsat(String bookingNumber, int score) {
        if (score < 1 || score > 5) return;
        Booking booking = getForService(bookingNumber);
        booking.setCsatScore(score);
        bookingRepository.save(booking);
    }

    private static String generateCheckInToken() {
        char[] t = new char[9];
        for (int i = 0; i < 4; i++) t[i] = TOKEN_CHARS[TOKEN_RANDOM.nextInt(TOKEN_CHARS.length)];
        t[4] = '-';
        for (int i = 5; i < 9; i++) t[i] = TOKEN_CHARS[TOKEN_RANDOM.nextInt(TOKEN_CHARS.length)];
        return new String(t);
    }
}
