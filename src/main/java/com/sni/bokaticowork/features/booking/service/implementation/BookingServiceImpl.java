package com.sni.bokaticowork.features.booking.service.implementation;

import org.springframework.beans.factory.annotation.Autowired;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.booking.config.BookingCheckInProperties;
import com.sni.bokaticowork.features.booking.dto.request.BookingChangeResourceRequest;
import com.sni.bokaticowork.features.booking.dto.request.BookingRescheduleRequest;
import com.sni.bokaticowork.features.booking.dto.request.BookingTransferRequest;
import com.sni.bokaticowork.features.ressource.enums.ResourceStatus;
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
import com.sni.bokaticowork.features.portal.notification.service.MemberInAppNotifier;
import com.sni.bokaticowork.features.ressource.dto.request.ReleaseResourceAvailabilityRequest;
import com.sni.bokaticowork.features.ressource.dto.request.ReserveResourceAvailabilityRequest;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourcePolicy;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceAvailabilityService;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceService;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.task.dto.TaskDtos.ChecklistRequest;
import com.sni.bokaticowork.features.task.dto.TaskDtos.CreateTaskRequest;
import com.sni.bokaticowork.features.task.enums.TaskPriority;
import com.sni.bokaticowork.features.task.enums.TaskRecurrence;
import com.sni.bokaticowork.features.task.service.interfaces.TaskManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.security.SecureRandom;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private org.springframework.beans.factory.ObjectProvider<com.sni.bokaticowork.core.accounting.ReceivableRiskQuery> receivableRisk;

    /** Lot F19 : creances a risque ; facultatif, sans comptabilite rien ne change. */
    @org.springframework.beans.factory.annotation.Autowired
    void setReceivableRisk(org.springframework.beans.factory.ObjectProvider<com.sni.bokaticowork.core.accounting.ReceivableRiskQuery> receivableRisk) {
        this.receivableRisk = receivableRisk;
    }

    private static final char[] TOKEN_CHARS = "ABCDEFGHJKLMNPRSTUVWXY23456789".toCharArray();
    private static final SecureRandom TOKEN_RANDOM = new SecureRandom();

    private final BookingRepository bookingRepository;
    private final BookingLineRepository lineRepository;
    private final BookingParticipantRepository participantRepository;
    private final BookingStatusHistoryRepository historyRepository;
    private final BookingEventRepository eventRepository;
    private final BookingRecurrenceGroupRepository recurrenceGroupRepository;
    private final BookingHoldRepository holdRepository;
    private final CancellationPolicyRepository cancellationPolicyRepository;
    private final ResourceService resourceService;
    private final ResourceAvailabilityService resourceAvailabilityService;
    private final BookingMapper bookingMapper;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final BookingIdentityResolver identityResolver;
    private final BookingPaymentContextResolver paymentContextResolver;
    private final BookingPricingCalculator pricingCalculator;
    private com.sni.bokaticowork.features.ressource.service.pricing.PriceContextResolver priceContextResolver;
    private com.sni.bokaticowork.features.booking.service.support.BookingRepricer repricer;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setRepricer(@org.springframework.context.annotation.Lazy com.sni.bokaticowork.features.booking.service.support.BookingRepricer repricer) {
        this.repricer = repricer;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setPriceContextResolver(com.sni.bokaticowork.features.ressource.service.pricing.PriceContextResolver priceContextResolver) {
        this.priceContextResolver = priceContextResolver;
    }
    private final BookingResourceGuard resourceGuard;
    private final BookingEntitlementBridge entitlementBridge;
    private final com.sni.bokaticowork.features.company.membership.service.MemberBillingCompanyResolver billingCompanyResolver;
    private final BookingEventWriter eventWriter;
    private final BookingPolicyEnforcer policyEnforcer;
    private final BookingBillableBridge billableBridge;
    private final BookingCancellationRefundSupport cancellationRefundSupport;
    private final BookingWalletSettlementSupport walletSettlement;
    private final BookingVirtualMeetingSupport virtualMeetingSupport;
    private final TaskManagementService taskManagementService;
    private final BookingCheckInProperties checkInProperties;
    private final MemberInAppNotifier memberInAppNotifier;
    private final com.sni.bokaticowork.features.ressource.service.support.StaffBookingOverride staffOverride;

    @Value("${bokati.task.booking-prep-hours:2}")
    private int bookingPrepHours;

    @Value("${bokati.task.booking-default-assignee:}")
    private String bookingDefaultAssignee;

    @Value("${bokati.task.booking-cleanup-default-assignee:}")
    private String bookingCleanupDefaultAssignee;

    private PlatformSettings platformSettings;

    @Autowired(required = false)
    void setPlatformSettings(PlatformSettings platformSettings) {
        this.platformSettings = platformSettings;
    }

    private static final String PREP_HOURS_KEY = "bokati.task.booking-prep-hours";
    private static final String DEFAULT_ASSIGNEE_KEY = "bokati.task.booking-default-assignee";
    private static final String CLEANUP_ASSIGNEE_KEY = "bokati.task.booking-cleanup-default-assignee";

    private int bookingPrepHours() {
        return platformSettings == null ? bookingPrepHours : platformSettings.integer(PREP_HOURS_KEY, bookingPrepHours);
    }

    private String bookingDefaultAssignee() {
        return platformSettings == null ? bookingDefaultAssignee
                : platformSettings.text(DEFAULT_ASSIGNEE_KEY, bookingDefaultAssignee);
    }

    private String bookingCleanupDefaultAssignee() {
        return platformSettings == null ? bookingCleanupDefaultAssignee
                : platformSettings.text(CLEANUP_ASSIGNEE_KEY, bookingCleanupDefaultAssignee);
    }

    /**
     * Un rejeu ne rend la reservation qu'a celui qui l'a faite.
     *
     * <p>La cle d'idempotence etait cherchee sans regarder le titulaire · un membre qui envoyait la
     * cle d'un autre recevait sa reservation entiere, jeton de pointage et lien de visio compris.
     * Quand la demande nomme son titulaire (le portail fixe toujours le membre), le titulaire de la
     * reservation existante doit etre le meme. Une demande de l'accueil par e-mail ou telephone ne
     * nomme personne et garde le rejeu simple · l'accueil voit deja toutes les reservations.</p>
     */
    private void assertReplayBelongsToRequester(Booking existing, CreateBookingRequest request) {
        SubscriberType requestedType = null;
        String requestedCode = null;
        if (StringUtils.hasText(request.memberId())) {
            requestedType = SubscriberType.MEMBER;
            requestedCode = request.memberId().trim();
        } else if (StringUtils.hasText(request.customerId())) {
            requestedType = SubscriberType.CUSTOMER;
            requestedCode = request.customerId().trim();
        } else if (StringUtils.hasText(request.businessCode())) {
            requestedType = SubscriberType.BUSINESS_ENTITY;
            requestedCode = request.businessCode().trim();
        }
        if (requestedType == null) {
            return;
        }
        if (existing.getOwnerType() != requestedType || !requestedCode.equalsIgnoreCase(existing.getOwnerCode())) {
            throw new ConflictException("booking", "this idempotency key is already used by another booking");
        }
    }

    @Override
    public BookingResponse create(CreateBookingRequest request) {
        return bookingMapper.toResponse(createInternal(request, null));
    }

    private Booking createInternal(CreateBookingRequest request, BookingIdentityResolver.ResolvedBookingIdentity suppliedIdentity) {
        if (StringUtils.hasText(request.idempotencyKey())) {
            Booking existing = bookingRepository.findByIdempotencyKey(request.idempotencyKey().trim()).orElse(null);
            if (existing != null) {
                assertReplayBelongsToRequester(existing, request);
                return existing;
            }
        }

        Resource resource = resourceService.getResourceForService(request.resourceCode().trim());
        int quantity = request.quantity() == null ? 1 : request.quantity();
        // Saisie par l'equipe (administration, gerance, operations) hors du portail : le preavis de
        // la ressource ne s'applique pas, sauf si sa politique l'interdit (stricte, evenementielle).
        String staffActor = staffOverride == null ? null
                : staffOverride.actorFor(resource.getEffectivePolicy()).orElse(null);
        resourceGuard.validateBookable(resource, request.startedAt(), request.endedAt(), quantity, staffActor != null);
        BookingIdentityResolver.ResolvedBookingIdentity identity = suppliedIdentity == null
                ? identityResolver.resolve(request.identityLookup())
                : suppliedIdentity;
        BookingPaymentContextResolver.BookingPaymentContext paymentContext = paymentContextResolver.resolve(
                request.paymentMode(), identity, resource, request.billingCompanyCode());
        BookingPolicyEnforcer.EffectivePolicy effectivePolicy = policyEnforcer.enforce(identity.ownerType(), identity.ownerCode(),
                resource, request.startedAt(), request.endedAt(), staffActor != null);
        com.sni.bokaticowork.core.accounting.ReceivableRiskQuery risk = receivableRisk == null ? null : receivableRisk.getIfAvailable();
        if (risk != null) {
            // Lot F19 : un client bloque pour creance douteuse ne reserve pas sans derogation.
            String warning = risk.guardNewSale(String.valueOf(identity.ownerType()), identity.ownerCode(), "la reservation");
            // Politique PREPAY : une reservation facturee sur compte (organisation payeuse) est une vente a credit.
            if (StringUtils.hasText(request.billingCompanyCode())) {
                risk.guardCreditSale(String.valueOf(identity.ownerType()), identity.ownerCode(), "la reservation facturee sur compte");
            }
            if (warning != null) {
                log.warn("Reservation de {} : {}", identity.ownerCode(), warning);
            }
        }

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
        booking.setBillingCompanyCode(billingCompany(request.billingCompanyCode(), identity));
        if (paymentContext.forCompany()) {
            // Le droit est celui de l'organisation (place ou pass partage) : decompte chez elle, le
            // membre reste le beneficiaire.
            booking.setEntitlementOwnerType(paymentContext.entitlementOwnerType());
            booking.setEntitlementOwnerCode(paymentContext.entitlementOwnerCode());
            booking.setSubscriptionSeatId(paymentContext.seatId());
            if (booking.getBillingCompanyCode() == null) {
                booking.setBillingCompanyCode(paymentContext.entitlementOwnerCode());
            }
        }
        booking.setCheckInToken(generateCheckInToken());
        booking.setPolicyOverrideBy(staffActor);
        booking.setCreatedBy(sessionActor());
        if (booking.getChannel() == null) {
            booking.setChannel(currentChannel());
        }

        // Lot R1 : prix du moteur unique, avec le contexte du titulaire (segment, plan, canal), comme le devis.
        BookingPricingCalculator.Price price = pricingCalculator.calculate(resource, request.startedAt(), request.endedAt(), quantity,
                request.bookingUnit(), priceContextResolver == null ? null
                        : priceContextResolver.forOwner(booking.getOwnerType() == null ? null : booking.getOwnerType().name(), booking.getOwnerCode()));
        booking.setPriceTrace(BookingPricingCalculator.trace(price));
        booking.setBookingUnit(price.unit());
        booking.setUnitPrice(price.unitPrice());
        booking.setSubtotalAmount(price.amount());
        booking.setTotalAmount(price.amount());
        booking.setCurrency(price.currency());
        booking.setUnitForced(request.bookingUnit() != null);
        // Lot B2 : le solde du droit ne couvre qu'une partie ; le reste est facture au prorata.
        if (partialCoverage != null) {
            Integer covered = partialCoverage.coveredMinutes(booking);
            if (covered != null) {
                booking.setCoveredMinutes(covered);
                booking.setTotalAmount(BookingPartialCoverage.uncoveredAmount(price.amount(), booking.getDurationMinutes(), covered));
            }
        }
        // Lot B10 : ressource sur demande, client du portail non designe : approbation de l'equipe.
        boolean onRequest = resourceVisibility != null
                && booking.getChannel() == com.sni.bokaticowork.features.booking.enums.BookingChannel.PORTAL
                && resourceVisibility.access(resource, booking.getOwnerType() == null ? null : booking.getOwnerType().name(),
                        booking.getOwnerCode()) != com.sni.bokaticowork.features.ressource.service.support.ResourceVisibilityService.Access.DIRECT;
        boolean approval = effectivePolicy.approvalRequired() || onRequest;
        booking.setApprovalRequired(approval);
        booking.setVirtualMeetingUrl(virtualMeetingSupport.meetingUrl(resource, booking.getBookingNumber()));
        boolean awaitsPayment = request.paymentMode() != null && request.paymentMode().awaitsPayment();
        if (approval) {
            booking.setStatus(BookingStatus.PENDING_APPROVAL);
        } else if (awaitsPayment) {
            booking.setStatus(BookingStatus.PENDING_PAYMENT);
        }
        booking = bookingRepository.save(booking);
        // Lot B1 (ecart 4, D2) : une reservation en attente retient son creneau des maintenant.
        if (booking.getStatus() == BookingStatus.PENDING_PAYMENT || booking.getStatus() == BookingStatus.PENDING_APPROVAL) {
            retainSlots(booking);
            if (booking.getStatus() == BookingStatus.PENDING_PAYMENT && paymentDue != null) {
                booking.setPaymentDueAt(paymentDue.dueAt(LocalDateTime.now(), booking.getStartedAt()));
            }
            booking = bookingRepository.save(booking);
        }

        saveLines(booking, price);
        saveParticipants(booking, request);
        writeHistory(booking, null, BookingStatus.DRAFT, booking.getCreatedBy(), "Booking created");
        eventWriter.write(booking, BookingEventType.BOOKING_CREATED, "Booking created", "Booking was created", null);

        if (staffActor != null && !request.startedAt().isAfter(LocalDateTime.now())) {
            // Debut deja passe : le client a occupe la ressource, la reservation est saisie apres
            // coup. Confirmee aussitot, facture comprise · il ne reste que le paiement.
            booking = confirmInternal(booking, new BookingStatusChangeRequest(staffActor,
                    "Reservation saisie par l'equipe apres le debut", Boolean.FALSE));
            booking = progressStaffBooking(booking, staffActor);
        } else if (approval) {
            writeHistory(booking, BookingStatus.DRAFT, BookingStatus.PENDING_APPROVAL, null, "Approval required");
            eventWriter.write(booking, BookingEventType.BOOKING_CREATED, "Booking pending approval", "Booking requires approval", null);
        } else if (awaitsPayment) {
            writeHistory(booking, BookingStatus.DRAFT, BookingStatus.PENDING_PAYMENT, null, "Awaiting payment");
            eventWriter.write(booking, BookingEventType.BOOKING_CREATED, "Booking pending payment", "Booking awaiting payment before confirmation", null);
            booking.setBillableNumber(billableBridge.ensureBillableItem(booking));
            bookingRepository.save(booking);
        } else if (request.confirmImmediately() == null || request.confirmImmediately()) {
            booking = confirmInternal(booking, new BookingStatusChangeRequest(null, "Auto confirmation", request.sendEmail()));
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
    @Transactional(readOnly = true)
    public List<com.sni.bokaticowork.features.booking.dto.response.CancellationPolicyResponse> cancellationPolicies() {
        return cancellationPolicyRepository.findAllByActiveTrueOrderByRuleOrderAsc().stream()
                .map(p -> new com.sni.bokaticowork.features.booking.dto.response.CancellationPolicyResponse(
                        p.getId(), p.getHoursBeforeStart(), p.getRefundPercentage(), p.getRuleOrder(), p.getActive()))
                .toList();
    }

    @Override
    public BookingResponse confirm(String bookingNumber, BookingStatusChangeRequest request) {
        return bookingMapper.toResponse(confirmInternal(getForService(bookingNumber), request));
    }

    @Override
    public BookingResponse approve(String bookingNumber, BookingApprovalRequest request) {
        Booking booking = getForService(bookingNumber);
        requireStatus(booking, BookingStatus.PENDING_APPROVAL);
        booking.setApprovedBy(request == null ? null : trim(actor(request.actor())));
        booking.setApprovedAt(Instant.now());
        boolean awaitsPayment = booking.getPaymentMode().awaitsPayment();
        if (awaitsPayment) {
            booking.setStatus(BookingStatus.PENDING_PAYMENT);
            booking = bookingRepository.save(booking);
            writeHistory(booking, BookingStatus.PENDING_APPROVAL, BookingStatus.PENDING_PAYMENT, booking.getApprovedBy(), request == null ? "Approved · awaiting payment" : reason(request.reason(), "Approved · awaiting payment"));
            eventWriter.write(booking, BookingEventType.BOOKING_APPROVED, "Booking approved", "Booking was approved, awaiting payment", null,
                    emailRequested(request == null ? null : request.sendEmail()));
            notifyIfRequested(booking, BookingEventType.BOOKING_APPROVED, new BookingStatusChangeRequest(booking.getApprovedBy(), null, request == null ? null : request.sendEmail()));
            booking.setBillableNumber(billableBridge.ensureBillableItem(booking));
            bookingRepository.save(booking);
            if (billableBridge.invoiceSettled(booking)) {
                // Reservation d'un lot multi-ressources : la facture commune, ou figurait deja cette
                // reservation, a ete reglee pendant l'attente d'approbation.
                return bookingMapper.toResponse(confirmInternal(booking, new BookingStatusChangeRequest(
                        booking.getApprovedBy(), "Approuvee · facture deja reglee", request == null ? null : request.sendEmail())));
            }
            return bookingMapper.toResponse(booking);
        }
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
        releaseSlotsIfRetained(booking, false);
        BookingStatus from = booking.getStatus();
        booking.setStatus(BookingStatus.REJECTED);
        booking.setRejectedBy(request == null ? null : trim(actor(request.actor())));
        booking.setRejectedAt(Instant.now());
        booking.setRejectionReason(request == null ? "Rejected" : reason(request.reason(), "Rejected"));
        booking = bookingRepository.save(booking);
        walletSettlement.release(booking, "reservation refusee");
        if (StringUtils.hasText(booking.getBatchNumber())) {
            // Reservation d'un lot : elle figurait deja dans la facture commune, sa part en sort
            try {
                cancellationRefundSupport.processRejection(booking);
            } catch (Exception ex) {
                log.warn("Failed to remove rejected booking {} from its batch invoice", booking.getBookingNumber(), ex);
            }
        }
        writeHistory(booking, from, BookingStatus.REJECTED, booking.getRejectedBy(), booking.getRejectionReason());
        eventWriter.write(booking, BookingEventType.BOOKING_REJECTED, "Booking rejected", booking.getRejectionReason(), null,
                emailRequested(request == null ? null : request.sendEmail()));
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
        eventWriter.write(booking, BookingEventType.BOOKING_STARTED, "Booking started", "Booking is in progress", null,
                emailRequested(request));
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
    public BookingResponse cancelForResourceUnavailability(String bookingNumber, String reason) {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String actor = auth == null ? "SYSTEM" : auth.getName();
        return bookingMapper.toResponse(cancelInternal(
                getForService(bookingNumber),
                new BookingStatusChangeRequest(actor, reason, Boolean.TRUE),
                true
        ));
    }

    @Override
    public BookingResponse noShow(String bookingNumber, BookingStatusChangeRequest request) {
        return bookingMapper.toResponse(markNoShowInternal(getForService(bookingNumber), request));
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> overdueNoShowCandidates(int limit) {
        LocalDateTime now = LocalDateTime.now();
        return bookingRepository.findOverdueNoShowCandidates(now, Math.max(1, limit)).stream()
                .filter(booking -> isOverdueNoShowCandidate(booking, now))
                .map(Booking::getBookingNumber)
                .toList();
    }

    /**
     * Une reservation a la fois, dans sa propre transaction · le worker en enchaine plusieurs.
     *
     * <p>Le lot entier tenait dans une seule transaction : une reservation dont le droit ne se
     * decomptait pas annulait tout le lot, puis recommencait en tete du lot suivant, chaque
     * minute. Plus aucune reservation n'etait marquee absente ni close.</p>
     */
    @Override
    public boolean markOverdueNoShow(String bookingNumber) {
        Booking booking = getForService(bookingNumber);
        if (!isOverdueNoShowCandidate(booking, LocalDateTime.now())) {
            return false;
        }
        markNoShowInternal(booking, new BookingStatusChangeRequest("SYSTEM", "Automatic no-show: booking ended without check-in or admin start", Boolean.FALSE));
        return true;
    }

    @Override
    public int startCheckedInBookings(int limit) {
        int resolvedLimit = Math.max(1, limit);
        LocalDateTime now = LocalDateTime.now();
        List<Booking> candidates = bookingRepository.findCheckedInAwaitingStart(now, resolvedLimit);
        int started = 0;
        for (Booking booking : candidates) {
            BookingStatus from = booking.getStatus();
            booking.setStatus(BookingStatus.IN_PROGRESS);
            bookingRepository.save(booking);
            writeHistory(booking, from, BookingStatus.IN_PROGRESS, "SYSTEM",
                    "Reservation demarree automatiquement · pointage effectue et heure atteinte");
            started++;
        }
        return started;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> overdueCompletionCandidates(int limit) {
        LocalDateTime now = LocalDateTime.now();
        return bookingRepository.findOverdueCompletionCandidates(now, Math.max(1, limit)).stream()
                .filter(booking -> isOverdueCompletionCandidate(booking, now))
                .map(Booking::getBookingNumber)
                .toList();
    }

    /** Une reservation a la fois, comme {@link #markOverdueNoShow} · un echec n'arrete pas les autres. */
    @Override
    public boolean markOverdueCompleted(String bookingNumber) {
        Booking booking = getForService(bookingNumber);
        if (!isOverdueCompletionCandidate(booking, LocalDateTime.now())) {
            return false;
        }
        completeInternal(booking, new BookingStatusChangeRequest("SYSTEM", "Automatic completion: booking ended after check-in", Boolean.FALSE));
        return true;
    }

    /**
     * CONFIRMED admis a cote de IN_PROGRESS : une reservation pointee d'avance dont le balayage
     * n'a pas eu le temps de tourner reste CONFIRMED, et resterait sinon coincee la, ni close ni
     * marquee absente. completeInternal accepte deja les deux statuts.
     */
    private static boolean isOverdueCompletionCandidate(Booking booking, LocalDateTime now) {
        return (booking.getStatus() == BookingStatus.IN_PROGRESS || booking.getStatus() == BookingStatus.CONFIRMED)
                && booking.getEndedAt() != null
                && booking.getEndedAt().isBefore(now)
                && (booking.getCheckedInAt() != null || booking.getStartedEventAt() != null);
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
        startIfHourHasCome(booking);
        booking = bookingRepository.save(booking);
        eventWriter.write(booking, BookingEventType.BOOKING_CHECKED_IN, "Booking checked in", request == null ? null : request.note(), null,
                emailRequested(request == null ? null : request.sendEmail()));
        notifyIfRequested(booking, BookingEventType.BOOKING_CHECKED_IN, new BookingStatusChangeRequest(actor(request == null ? null : request.actor()), request == null ? null : request.note(), request == null ? null : request.sendEmail()));
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
        // Lot R5c : depassement mesure au depart, selon la politique.
        if (bookingExtras != null) {
            bookingExtras.onCheckOut(booking);
        }
        eventWriter.write(booking, BookingEventType.BOOKING_CHECKED_OUT, "Booking checked out", request == null ? null : request.note(), null,
                emailRequested(request == null ? null : request.sendEmail()));
        notifyIfRequested(booking, BookingEventType.BOOKING_CHECKED_OUT, new BookingStatusChangeRequest(actor(request == null ? null : request.actor()), request == null ? null : request.note(), request == null ? null : request.sendEmail()));
        return bookingMapper.toResponse(booking);
    }

    @Override
    public BookingResponse earlyCheckIn(String bookingNumber, BookingCheckRequest request) {
        Booking booking = getForService(bookingNumber);
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new ConflictException("booking", "only confirmed or in-progress bookings can be checked in");
        }
        booking.setCheckedInAt(Instant.now());
        startIfHourHasCome(booking);
        booking = bookingRepository.save(booking);
        eventWriter.write(booking, BookingEventType.BOOKING_CHECKED_IN, "Early check-in by admin",
                request == null ? null : request.note(), null,
                emailRequested(request == null ? null : request.sendEmail()));
        notifyIfRequested(booking, BookingEventType.BOOKING_CHECKED_IN,
                new BookingStatusChangeRequest(
                        actor(request == null ? null : request.actor()),
                        request == null ? null : request.note(),
                        request == null ? null : request.sendEmail()));
        return bookingMapper.toResponse(booking);
    }

    @Override
    public BookingResponse transfer(String bookingNumber, BookingTransferRequest request) {
        Booking booking = getForService(bookingNumber);
        // Lot B1 (ecart 11) : seulement une reservation vivante, payee directement ; nouveau titulaire verifie.
        if (booking.getStatus() != BookingStatus.DRAFT && booking.getStatus() != BookingStatus.PENDING_PAYMENT
                && booking.getStatus() != BookingStatus.PENDING_APPROVAL && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ConflictException("Une réservation " + statusLabel(booking.getStatus()) + " ne se transfère pas");
        }
        if (booking.getPaymentMode() != null && (booking.getPaymentMode().usesEntitlement()
                || booking.getPaymentMode() == com.sni.bokaticowork.features.booking.enums.BookingPaymentMode.WALLET)) {
            throw new ConflictException("Réservation couverte par un droit ou réglée par portefeuille : annulez-la et réservez pour le nouveau titulaire");
        }
        if (request.newOwnerType() == null || !StringUtils.hasText(request.newOwnerCode())) {
            throw new BadRequestException("Le nouveau titulaire (type et code) est requis");
        }
        if (!ownerExists(request.newOwnerType(), request.newOwnerCode().trim())) {
            throw new BadRequestException("Nouveau titulaire introuvable : " + request.newOwnerType() + " " + request.newOwnerCode().trim());
        }
        booking.setOwnerType(request.newOwnerType());
        booking.setOwnerCode(request.newOwnerCode().trim());
        if (StringUtils.hasText(request.newContactName())) booking.setContactName(request.newContactName().trim());
        if (StringUtils.hasText(request.newContactEmail())) booking.setContactEmail(request.newContactEmail().trim());
        if (StringUtils.hasText(request.newContactPhone())) booking.setContactPhone(request.newContactPhone().trim());
        booking = bookingRepository.save(booking);
        eventWriter.write(booking, BookingEventType.BOOKING_TRANSFERRED,
                "Booking transferred to " + request.newOwnerCode(), request.note(), null,
                emailRequested(request.sendEmail()));
        notifyIfRequested(booking, BookingEventType.BOOKING_TRANSFERRED,
                new BookingStatusChangeRequest(actor(request.actor()), request.note(), request.sendEmail()));
        return bookingMapper.toResponse(booking);
    }

    @Override
    public BookingResponse reschedule(String bookingNumber, BookingRescheduleRequest request) {
        Booking booking = getForService(bookingNumber);
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.PENDING_APPROVAL
                && booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new ConflictException("Seule une réservation confirmée ou en attente se déplace");
        }
        LocalDateTime newStart = request.newStartedAt();
        LocalDateTime newEnd = request.newEndedAt();
        if (!newEnd.isAfter(newStart)) {
            throw new BadRequestException("New end time must be after start time");
        }
        Resource resource = booking.getResource();
        validateForReschedule(resource, newStart, newEnd, bookingNumber, booking);
        // Lot B1 (ecart 3) : seuls des creneaux vraiment retenus sont rendus puis repris (la reprise
        // controle la capacite) ; une reservation qui ne retenait rien n'en prend pas.
        if (Boolean.TRUE.equals(booking.getSlotsReserved())) {
            releaseResourceSlot(resource, booking.getStartedAt(), booking.getEndedAt(), booking.getQuantity());
            eventWriter.write(booking, BookingEventType.RESOURCE_RELEASED, "Resource released for reschedule",
                    booking.getStartedAt() + " – " + booking.getEndedAt(), null);
            reserveResourceSlot(resource, newStart, newEnd, booking.getQuantity());
            eventWriter.write(booking, BookingEventType.RESOURCE_RESERVED, "Resource reserved after reschedule",
                    newStart + " – " + newEnd, null);
        }
        boolean entitlementRetained = booking.getPaymentMode().usesEntitlement()
                && (booking.getStatus() == BookingStatus.CONFIRMED || booking.getStatus() == BookingStatus.IN_PROGRESS);
        if (entitlementRetained) {
            entitlementBridge.release(booking);
        }
        BookingStatus from = booking.getStatus();
        booking.setStartedAt(newStart);
        booking.setEndedAt(newEnd);
        booking.setDurationMinutes((int) Duration.between(newStart, newEnd).toMinutes());
        if (booking.getCoveredMinutes() != null) {
            booking.setCoveredMinutes(Math.min(booking.getCoveredMinutes(), booking.getDurationMinutes()));
        }
        // Lot B2 (ecart Bn-2) : le droit est rendu sur l'ancienne duree et repris sur la nouvelle.
        if (entitlementRetained) {
            entitlementBridge.reserve(booking);
        }
        // Lot R1 (D1) : nouveau prix ; ecart facture en complement ou rendu par un avoir.
        if (repricer != null) {
            repricer.reprice(booking, "nouvel horaire " + newStart + " – " + newEnd);
        }
        booking = bookingRepository.save(booking);
        writeHistory(booking, from, booking.getStatus(), actor(request.actor()), reason(request.note(), "Booking rescheduled"));
        eventWriter.write(booking, BookingEventType.BOOKING_RESCHEDULED, "Booking rescheduled",
                "New slot: " + newStart + " – " + newEnd, null,
                emailRequested(request.sendEmail()));
        notifyIfRequested(booking, BookingEventType.BOOKING_RESCHEDULED,
                new BookingStatusChangeRequest(actor(request.actor()), request.note(), request.sendEmail()));
        return bookingMapper.toResponse(booking);
    }

    @Override
    public BookingResponse changeResource(String bookingNumber, BookingChangeResourceRequest request) {
        Booking booking = getForService(bookingNumber);
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ConflictException("booking", "only CONFIRMED bookings can have their resource changed");
        }
        Resource newResource = resourceService.getResourceForService(request.newResourceCode().trim());
        validateForReschedule(newResource, booking.getStartedAt(), booking.getEndedAt(), bookingNumber, booking);
        Resource oldResource = booking.getResource();
        boolean retained = Boolean.TRUE.equals(booking.getSlotsReserved());
        if (retained) {
            releaseResourceSlot(oldResource, booking.getStartedAt(), booking.getEndedAt(), booking.getQuantity());
            eventWriter.write(booking, BookingEventType.RESOURCE_RELEASED, "Resource released for resource change",
                    oldResource.getCode(), null);
        }
        // Lot B2 (ecart Bn-2) : le droit doit couvrir la nouvelle ressource ; il est repris.
        boolean entitlementRetained = booking.getPaymentMode().usesEntitlement();
        if (entitlementRetained) {
            entitlementBridge.release(booking);
            var identity = new BookingIdentityResolver.ResolvedBookingIdentity(booking.getOwnerType(), booking.getOwnerCode(),
                    booking.getContactName(), booking.getContactEmail(), booking.getContactPhone(), false);
            var context = paymentContextResolver.resolve(booking.getPaymentMode(), identity, newResource, booking.getBillingCompanyCode());
            booking.setEntitlementCode(context.entitlementCode());
            booking.setSubscriptionNumber(context.subscriptionNumber());
            booking.setPassNumber(context.passNumber());
        }
        booking.setResource(newResource);
        if (entitlementRetained) {
            entitlementBridge.reserve(booking);
        }
        if (retained) {
            reserveResourceSlot(newResource, booking.getStartedAt(), booking.getEndedAt(), booking.getQuantity());
            eventWriter.write(booking, BookingEventType.RESOURCE_RESERVED, "Resource reserved after resource change",
                    newResource.getCode(), null);
        }
        // Lot R1 (D1) : prix de la nouvelle ressource ; ecart facture en complement ou rendu par un avoir.
        if (repricer != null) {
            repricer.reprice(booking, "ressource " + oldResource.getCode() + " → " + newResource.getCode());
        }
        booking = bookingRepository.save(booking);
        eventWriter.write(booking, BookingEventType.BOOKING_RESOURCE_CHANGED,
                "Resource changed from " + oldResource.getCode() + " to " + newResource.getCode(),
                request.note(), null,
                emailRequested(request.sendEmail()));
        notifyIfRequested(booking, BookingEventType.BOOKING_RESOURCE_CHANGED,
                new BookingStatusChangeRequest(actor(request.actor()), request.note(), request.sendEmail()));
        return bookingMapper.toResponse(booking);
    }

    @Override
    public BookingParticipantResponse addParticipant(String bookingNumber, BookingParticipantRequest request) {
        Booking booking = getForService(bookingNumber);
        // Lot B1 (ecart Bn-12) : seulement sur une reservation vivante, et dans la capacite.
        if (booking.getStatus() != BookingStatus.DRAFT && booking.getStatus() != BookingStatus.PENDING_PAYMENT
                && booking.getStatus() != BookingStatus.PENDING_APPROVAL && booking.getStatus() != BookingStatus.CONFIRMED
                && booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new ConflictException("Une réservation " + statusLabel(booking.getStatus()) + " ne reçoit plus de participant");
        }
        Integer capacity = booking.getResource() == null ? null : booking.getResource().getCapacity();
        if (capacity != null) {
            long present = participantRepository.findByBookingId(booking.getId()).stream()
                    .filter(p -> p.getStatus() == null || !"REMOVED".equals(p.getStatus().toString())).count();
            if (present + 1 > capacity) {
                throw new ConflictException("Capacité de " + booking.getResource().getCode() + " atteinte : " + capacity + " personnes au plus");
            }
        }
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

    /**
     * Une reservation de l'equipe dont le debut est passe · close si toute la plage est passee,
     * en cours sinon. Sans cela, le balayage des absences la marquait absente faute de pointage.
     */
    private Booking progressStaffBooking(Booking booking, String staffActor) {
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            return booking;
        }
        Instant startedAt = booking.getStartedAt().atZone(ZoneId.systemDefault()).toInstant();
        booking.setStartedEventAt(startedAt);
        if (!booking.getEndedAt().isAfter(LocalDateTime.now())) {
            return completeInternal(booking, new BookingStatusChangeRequest(staffActor,
                    "Reservation passee saisie par l'equipe · faite, reste le paiement", Boolean.FALSE));
        }
        BookingStatus from = booking.getStatus();
        booking.setStatus(BookingStatus.IN_PROGRESS);
        booking = bookingRepository.save(booking);
        writeHistory(booking, from, BookingStatus.IN_PROGRESS, staffActor, "Reservation en cours saisie par l'equipe");
        eventWriter.write(booking, BookingEventType.BOOKING_STARTED, "Booking started", "Booking is in progress", null);
        return booking;
    }

    private Booking confirmInternal(Booking booking, BookingStatusChangeRequest request) {
        if (booking.getStatus() != BookingStatus.DRAFT && booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new ConflictException("booking", "expected status DRAFT or PENDING_PAYMENT but was " + booking.getStatus());
        }
        // Une reservation saisie par l'equipe hors preavis le reste a sa confirmation, meme quand
        // celle-ci vient du paiement, sans personne de l'equipe derriere.
        // Lot B1 (ecart 4) : un creneau deja retenu a la creation n'est ni recontrole (le preavis
        // pouvait faire echouer la confirmation apres un paiement arrive tard) ni repris.
        if (!Boolean.TRUE.equals(booking.getSlotsReserved())) {
            resourceGuard.validateBookable(booking.getResource(), booking.getStartedAt(), booking.getEndedAt(), booking.getQuantity(),
                    StringUtils.hasText(booking.getPolicyOverrideBy())
                            && com.sni.bokaticowork.features.ressource.service.support.StaffBookingOverride.allowedBy(
                            booking.getResource().getEffectivePolicy()));
            resourceGuard.validateNoSingleCapacityConflict(booking.getResource(), booking.getStartedAt(), booking.getEndedAt(), booking.getBookingNumber());
            retainSlots(booking);
        }
        if (booking.getPaymentMode().usesEntitlement()) {
            entitlementBridge.reserve(booking);
            eventWriter.write(booking, BookingEventType.ENTITLEMENT_RESERVED, "Entitlement reserved", "Booking entitlement was reserved", null);
        }
        holdWalletFunds(booking);
        booking.setBillableNumber(billableBridge.ensureBillableItem(booking));
        BookingStatus from = booking.getStatus();
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(Instant.now());
        booking.setPaymentDueAt(null);
        // Lot B1 (ecart 2) : le portefeuille bloque les fonds a la confirmation (acquis) ; un
        // reglement exterieur est constate par la chaine de paiement (markPaid). Une confirmation
        // manuelle sans paiement laisse la reservation « à régler ».
        if (booking.getPaymentMode() == com.sni.bokaticowork.features.booking.enums.BookingPaymentMode.WALLET
                || "SYSTEM".equals(changedBy(request))) {
            booking.setPaidAt(Instant.now());
        }
        booking = bookingRepository.save(booking);
        writeHistory(booking, from, BookingStatus.CONFIRMED, changedBy(request), reason(request, "Booking confirmed"));
        eventWriter.write(booking, BookingEventType.BOOKING_CONFIRMED, "Booking confirmed", "Booking was confirmed", null,
                emailRequested(request));
        notifyIfRequested(booking, BookingEventType.BOOKING_CONFIRMED, request);
        createBookingPreparationTask(booking);
        return booking;
    }

    @Override
    public BookingResponse revertConfirmation(String bookingNumber, String reason) {
        Booking booking = getForService(bookingNumber);
        requireStatus(booking, BookingStatus.CONFIRMED);
        // Lot B1 (ecart Bn-8) : le creneau reste retenu, avec une nouvelle echeance ; le reglement
        // n'est plus constate, le blocage du portefeuille est rendu.
        if (booking.getPaymentMode().usesEntitlement()) {
            entitlementBridge.release(booking);
            eventWriter.write(booking, BookingEventType.ENTITLEMENT_RELEASED, "Entitlement released", "Booking entitlement reservation was released", null);
        }
        walletSettlement.release(booking, "confirmation annulee");
        booking.setPaidAt(null);
        booking.setConfirmedAt(null);
        if (paymentDue != null) {
            booking.setPaymentDueAt(paymentDue.dueAt(LocalDateTime.now(), booking.getStartedAt()));
        }
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking = bookingRepository.save(booking);
        writeHistory(booking, BookingStatus.CONFIRMED, BookingStatus.PENDING_PAYMENT, "SYSTEM", reason);
        return bookingMapper.toResponse(booking);
    }

    private Booking cancelInternal(Booking booking, BookingStatusChangeRequest request, boolean systemOverride) {
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return booking;
        }
        // Lot B1 (ecart Bn-9) : une reservation terminee, absente, refusee ou expiree ne s'annule plus.
        if (!systemOverride && (booking.getStatus() == BookingStatus.COMPLETED || booking.getStatus() == BookingStatus.NO_SHOW
                || booking.getStatus() == BookingStatus.REJECTED || booking.getStatus() == BookingStatus.EXPIRED)) {
            throw new ConflictException("Une réservation " + statusLabel(booking.getStatus()) + " ne s'annule pas");
        }
        if (!systemOverride) {
            assertCancellationAllowed(booking);
        }
        boolean wasActive = booking.getStatus() == BookingStatus.CONFIRMED || booking.getStatus() == BookingStatus.IN_PROGRESS;
        releaseSlotsIfRetained(booking, wasActive);
        if (wasActive) {
            if (booking.getPaymentMode().usesEntitlement()) {
                entitlementBridge.release(booking);
                eventWriter.write(booking, BookingEventType.ENTITLEMENT_RELEASED, "Entitlement released", "Booking entitlement reservation was released", null);
            }
        }
        walletSettlement.release(booking, "reservation annulee");
        BookingStatus from = booking.getStatus();
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reason(request, "Booking cancelled"));
        booking.setCancelledAt(Instant.now());
        booking = bookingRepository.save(booking);
        // Lot R5d : taches ouvertes de la reservation annulees avec elle.
        if (bookingOperations != null) {
            bookingOperations.cancelTasks(booking);
        }
        writeHistory(booking, from, BookingStatus.CANCELLED, changedBy(request), reason(request, "Booking cancelled"));
        eventWriter.write(booking, BookingEventType.BOOKING_CANCELLED, "Booking cancelled", booking.getCancellationReason(), null,
                emailRequested(request));
        notifyIfRequested(booking, BookingEventType.BOOKING_CANCELLED, request);
        // Y compris lorsque la reservation n'avait rien engage : sa facture etait deja emise et
        // devait disparaitre avec elle.
        try {
            cancellationRefundSupport.processCancellation(booking, wasActive);
        } catch (Exception ex) {
            log.error("Remboursement ou avoir de la reservation annulee {} en echec", booking.getBookingNumber(), ex);
            // Lot B2 (ecart Bn-5) : une tache a l'equipe et un evenement, au lieu d'un simple journal.
            eventWriter.write(booking, BookingEventType.BOOKING_CANCELLED, "Remboursement à traiter",
                    "Échec automatique : " + ex.getMessage(), null);
            try {
                taskManagementService.createFromAutomation(new CreateTaskRequest(
                        "Remboursement à traiter · réservation " + booking.getBookingNumber(),
                        "L'avoir ou le remboursement automatique de l'annulation a échoué : " + ex.getMessage()
                                + ". Contrôler la facture de la réservation et rembourser le client.",
                        null, TaskPriority.HIGH, Instant.now().plus(Duration.ofHours(24)), "BOOKING", booking.getBookingNumber(),
                        TaskRecurrence.NONE, null));
            } catch (Exception taskError) {
                log.warn("Tache de remboursement non creee pour {}", booking.getBookingNumber(), taskError);
            }
        }
        return booking;
    }

    private com.sni.bokaticowork.features.booking.service.support.BookingExtras bookingExtras;
    private com.sni.bokaticowork.features.ressource.service.support.ResourceVisibilityService resourceVisibility;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setResourceVisibility(com.sni.bokaticowork.features.ressource.service.support.ResourceVisibilityService resourceVisibility) {
        this.resourceVisibility = resourceVisibility;
    }
    private com.sni.bokaticowork.features.booking.service.support.BookingOperations bookingOperations;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setBookingOperations(@org.springframework.context.annotation.Lazy com.sni.bokaticowork.features.booking.service.support.BookingOperations bookingOperations) {
        this.bookingOperations = bookingOperations;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setBookingExtras(@org.springframework.context.annotation.Lazy com.sni.bokaticowork.features.booking.service.support.BookingExtras bookingExtras) {
        this.bookingExtras = bookingExtras;
    }

    private Booking markNoShowInternal(Booking booking, BookingStatusChangeRequest request) {
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new ConflictException("booking", "only confirmed or in-progress bookings can be marked as no-show");
        }
        // Lot B1 (ecart Bn-10) : absent seulement une fois la fenetre d'arrivee passee.
        LocalDateTime noShowFrom = booking.getStartedAt().plusMinutes(checkInProperties.getLateWindowMinutes());
        if (LocalDateTime.now().isBefore(noShowFrom)) {
            throw new ConflictException("Trop tôt pour constater une absence : possible à partir de " + noShowFrom);
        }
        if (booking.getPaymentMode().usesEntitlement() && entitlementBridge.consume(booking)) {
            eventWriter.write(booking, BookingEventType.ENTITLEMENT_CONSUMED, "Entitlement consumed", "No-show entitlement was consumed", null);
        }
        BookingStatus from = booking.getStatus();
        booking.setStatus(BookingStatus.NO_SHOW);
        booking.setCompletedAt(Instant.now());
        booking = bookingRepository.save(booking);
        // Lot R5c : frais de non-presentation de la politique.
        if (bookingExtras != null) {
            bookingExtras.onNoShow(booking);
        }
        writeHistory(booking, from, BookingStatus.NO_SHOW, changedBy(request), reason(request, "No-show"));
        eventWriter.write(booking, BookingEventType.BOOKING_NO_SHOW, "Booking no-show", "Booking was marked as no-show", null,
                emailRequested(request));
        notifyIfRequested(booking, BookingEventType.BOOKING_NO_SHOW, request);
        return booking;
    }

    private Booking completeInternal(Booking booking, BookingStatusChangeRequest request) {
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new ConflictException("booking", "only confirmed or in-progress bookings can be completed");
        }
        // Lot B1 (ecart Bn-10) : on ne clot pas une reservation qui n'a pas commence.
        if (LocalDateTime.now().isBefore(booking.getStartedAt())) {
            throw new ConflictException("La réservation n'a pas commencé : elle ne se clôt pas avant " + booking.getStartedAt());
        }
        if (booking.getPaymentMode().usesEntitlement() && entitlementBridge.consume(booking)) {
            eventWriter.write(booking, BookingEventType.ENTITLEMENT_CONSUMED, "Entitlement consumed", "Booking entitlement was consumed", null);
            eventWriter.write(booking, BookingEventType.USAGE_RECORDED, "Usage recorded", "Booking usage was recorded", null);
        }
        BookingStatus from = booking.getStatus();
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(Instant.now());
        booking = bookingRepository.save(booking);
        writeHistory(booking, from, BookingStatus.COMPLETED, changedBy(request), reason(request, "Booking completed"));
        eventWriter.write(booking, BookingEventType.BOOKING_COMPLETED, "Booking completed", "Booking was completed", null,
                emailRequested(request));
        notifyIfRequested(booking, BookingEventType.BOOKING_COMPLETED, request);
        createBookingCleanupTask(booking);
        return booking;
    }

    private void createBookingPreparationTask(Booking booking) {
        // Lot R5d : les modeles de taches de la ressource remplacent la tache par defaut.
        if (bookingOperations != null && bookingOperations.hasTemplates(booking)) {
            bookingOperations.createBookingTasks(booking);
            return;
        }
        try {
            taskManagementService.createFromAutomation(new CreateTaskRequest(
                    "Preparer la ressource " + booking.getResource().getName(),
                    "Preparation automatique pour la reservation " + booking.getBookingNumber()
                            + ". Ressource: " + booking.getResource().getName()
                            + ". Debut: " + booking.getStartedAt()
                            + ". Contact: " + nullSafe(booking.getContactName()),
                    configuredAssignee(bookingDefaultAssignee()),
                    TaskPriority.HIGH,
                    booking.getStartedAt().minusHours(Math.max(0, bookingPrepHours())).atZone(ZoneId.systemDefault()).toInstant(),
                    "BOOKING",
                    booking.getBookingNumber(),
                    TaskRecurrence.NONE,
                    List.of(
                            new ChecklistRequest("Verifier disponibilite et configuration de la ressource", false, 1),
                            new ChecklistRequest("Preparer les acces et supports necessaires", false, 2)
                    )
            ));
        } catch (Exception ex) {
            log.warn("Failed to create booking preparation task for {}", booking.getBookingNumber(), ex);
        }
    }

    private void createBookingCleanupTask(Booking booking) {
        // Lot R5d : avec des modeles, les taches d'apres sont deja creees a la confirmation.
        if (bookingOperations != null && bookingOperations.hasTemplates(booking)) {
            return;
        }
        try {
            taskManagementService.createFromAutomation(new CreateTaskRequest(
                    "Nettoyer la ressource " + booking.getResource().getName(),
                    "Tache generee apres la reservation " + booking.getBookingNumber()
                            + ". Ressource: " + booking.getResource().getName()
                            + ". Fin: " + booking.getEndedAt(),
                    configuredAssignee(bookingCleanupDefaultAssignee()),
                    TaskPriority.MEDIUM,
                    booking.getEndedAt().atZone(ZoneId.systemDefault()).toInstant(),
                    "BOOKING",
                    booking.getBookingNumber(),
                    TaskRecurrence.AFTER_BOOKING,
                    List.of(
                            new ChecklistRequest("Inspecter la ressource apres utilisation", false, 1),
                            new ChecklistRequest("Remettre la ressource en etat pour la prochaine reservation", false, 2)
                    )
            ));
        } catch (Exception ex) {
            log.warn("Failed to create booking cleanup task for {}", booking.getBookingNumber(), ex);
        }
    }

    private Long configuredAssignee(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            log.warn("Ignoring invalid task assignee configuration value '{}'", value);
            return null;
        }
    }

    private String nullSafe(String value) {
        return StringUtils.hasText(value) ? value.trim() : "N/A";
    }

    /**
     * Lot B1 : retient les creneaux de la reservation (blocage d'option repris s'il y en a un,
     * meme expire : la capacite est alors reprise directement).
     */
    private void retainSlots(Booking booking) {
        if (Boolean.TRUE.equals(booking.getSlotsReserved())) {
            return;
        }
        boolean fromHold = false;
        try {
            fromHold = confirmHoldIfPresent(booking);
        } catch (ConflictException ex) {
            if (!holdExpired(booking)) {
                throw ex;
            }
            // Option expiree : sa capacite a ete (ou sera) rendue, la reservation la reprend.
        }
        if (!fromHold) {
            reserveResource(booking);
            eventWriter.write(booking, BookingEventType.RESOURCE_RESERVED, "Resource reserved", "Booking resource availability was reserved", null);
        }
        booking.setSlotsReserved(Boolean.TRUE);
    }

    private boolean holdExpired(Booking booking) {
        if (!StringUtils.hasText(booking.getHoldNumber())) {
            return false;
        }
        return holdRepository.findByHoldNumber(booking.getHoldNumber())
                .map(hold -> hold.getStatus() != BookingHoldStatus.ACTIVE || hold.getExpiresAt().isBefore(Instant.now()))
                .orElse(false);
    }

    /** Lot B1 : rend les creneaux s'ils etaient retenus (ou, pour les anciennes reservations, si actives). */
    private void releaseSlotsIfRetained(Booking booking, boolean active) {
        if (Boolean.TRUE.equals(booking.getSlotsReserved()) || active) {
            releaseResource(booking);
            eventWriter.write(booking, BookingEventType.RESOURCE_RELEASED, "Resource released", "Booking resource availability was released", null);
            booking.setSlotsReserved(Boolean.FALSE);
        }
    }

    private static String statusLabel(BookingStatus status) {
        return switch (status) {
            case COMPLETED -> "terminée";
            case NO_SHOW -> "marquée absente";
            case REJECTED -> "refusée";
            case EXPIRED -> "expirée";
            case CANCELLED -> "annulée";
            case IN_PROGRESS -> "en cours";
            default -> String.valueOf(status);
        };
    }

    private boolean ownerExists(com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType type, String code) {
        try {
            return switch (type) {
                case MEMBER -> memberOwnerExists(code);
                default -> true;
            };
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository memberRepositoryB1;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setMemberRepositoryB1(com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository memberRepositoryB1) {
        this.memberRepositoryB1 = memberRepositoryB1;
    }

    private boolean memberOwnerExists(String code) {
        return memberRepositoryB1 == null || memberRepositoryB1.findByMemberIdAndDeletedFalse(code).isPresent();
    }

    // ------------------------------------------------------------------ lot B1 : expiration et reglement

    private BookingPaymentDue paymentDue;
    private BookingPartialCoverage partialCoverage;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setPartialCoverage(BookingPartialCoverage partialCoverage) {
        this.partialCoverage = partialCoverage;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setPaymentDue(BookingPaymentDue paymentDue) {
        this.paymentDue = paymentDue;
    }

    @Override
    public int expireUnpaid(int limit) {
        int expired = 0;
        for (Booking booking : bookingRepository.findUnpaidPastDue(LocalDateTime.now(), Math.max(1, limit))) {
            try {
                expireInternal(booking);
                expired++;
            } catch (RuntimeException ex) {
                log.warn("Reservation {} non expiree : {}", booking.getBookingNumber(), ex.getMessage());
            }
        }
        return expired;
    }

    /** Echeance passee sans reglement : creneau rendu, facture annulee, statut EXPIRED, client et equipe prevenus. */
    private void expireInternal(Booking booking) {
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            return;
        }
        releaseSlotsIfRetained(booking, false);
        walletSettlement.release(booking, "reservation expiree");
        booking.setStatus(BookingStatus.EXPIRED);
        booking.setCancellationReason("Échéance de paiement passée");
        booking.setCancelledAt(Instant.now());
        booking = bookingRepository.save(booking);
        if (bookingOperations != null) {
            bookingOperations.cancelTasks(booking);
        }
        writeHistory(booking, BookingStatus.PENDING_PAYMENT, BookingStatus.EXPIRED, "SYSTEM", "Échéance de paiement passée");
        eventWriter.write(booking, BookingEventType.BOOKING_CANCELLED, "Réservation expirée",
                "Non réglée à l'échéance : créneau rendu", null, true);
        try {
            cancellationRefundSupport.processCancellation(booking, false);
        } catch (Exception ex) {
            log.warn("Facture de la reservation expiree {} non annulee : {}", booking.getBookingNumber(), ex.getMessage());
        }
    }

    @Override
    public void markPaid(String bookingNumber) {
        Booking booking = getForService(bookingNumber);
        if (booking.getPaidAt() == null) {
            booking.setPaidAt(Instant.now());
            bookingRepository.save(booking);
        }
    }

    private void reserveResource(Booking booking) {
        resourceAvailabilityService.reserve(ReserveResourceAvailabilityRequest.builder()
                .staffOverride(StringUtils.hasText(booking.getPolicyOverrideBy()))
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
        // Lot B1 (ecart Bn-13) : un invite recoit un nouveau code a chaque appel ; on le reconnait a
        // ses coordonnees.
        boolean sameGuest = hold.getOwnerType() == booking.getOwnerType()
                && ((StringUtils.hasText(hold.getContactEmail()) && hold.getContactEmail().equalsIgnoreCase(booking.getContactEmail()))
                || (StringUtils.hasText(hold.getContactPhone()) && hold.getContactPhone().equals(booking.getContactPhone())));
        if (!sameGuest && (hold.getOwnerType() != booking.getOwnerType() || !hold.getOwnerCode().equals(booking.getOwnerCode()))) {
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

    private void releaseResourceSlot(Resource resource, LocalDateTime start, LocalDateTime end, int quantity) {
        resourceAvailabilityService.release(ReleaseResourceAvailabilityRequest.builder()
                .resourceCode(resource.getCode())
                .startedAt(start)
                .endedAt(end)
                .quantity(quantity)
                .build());
    }

    private void reserveResourceSlot(Resource resource, LocalDateTime start, LocalDateTime end, int quantity) {
        resourceAvailabilityService.reserve(ReserveResourceAvailabilityRequest.builder()
                .resourceCode(resource.getCode())
                .startedAt(start)
                .endedAt(end)
                .quantity(quantity)
                .build());
    }

    private void validateForReschedule(Resource resource, LocalDateTime start, LocalDateTime end, String excludedBookingNumber,
                                       Booking booking) {
        // Lot B1 (ecart 3) : memes controles qu'a la reservation (ressource, alignement, politique,
        // date passee), l'equipe passant outre comme a la creation.
        boolean staff = isCurrentUserStaff() && com.sni.bokaticowork.features.ressource.service.support.StaffBookingOverride
                .allowedBy(resource.getEffectivePolicy());
        resourceGuard.validateBookable(resource, start, end, booking.getQuantity(), staff);
        // La capacite se controle a la reprise des creneaux ; sans creneau retenu, une piece entiere
        // reste protegee par le conflit de reservations.
        if (!Boolean.TRUE.equals(booking.getSlotsReserved()) && resource.isWholeResourceBooking()
                && bookingRepository.existsActiveConflict(resource.getId(), start, end, excludedBookingNumber)) {
            throw new ConflictException("La ressource est déjà réservée sur ce créneau");
        }
    }

    private void saveLines(Booking booking, BookingPricingCalculator.Price price) {
        lineRepository.save(BookingLine.builder()
                .booking(booking)
                .lineType(BookingLineType.RESOURCE)
                .description(resourceLineDescription(booking))
                .quantity(price.quantity())
                .unit(price.unit())
                .unitPrice(price.unitPrice())
                .amount(booking.getCoveredMinutes() != null ? booking.getTotalAmount() : price.amount())
                .currency(price.currency())
                .build());
        if (booking.getPaymentMode().usesEntitlement() && StringUtils.hasText(booking.getEntitlementCode())) {
            // Meme source que le debit reel · la ligne annonce ce qui est retire au droit, dans
            // l'unite de celui-ci, et non dans celle de la reservation.
            BookingEntitlementBridge.Charge charge = entitlementBridge.charge(booking);
            lineRepository.save(BookingLine.builder()
                    .booking(booking)
                    .lineType(BookingLineType.ENTITLEMENT)
                    .description(entitlementLineDescription(booking))
                    .quantity(charge.quantity())
                    .unit(charge.displayUnit())
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

    /**
     * Applique la politique d'annulation de la ressource.
     *
     * <p>Elle ne s'applique jamais a une reservation qui n'engage encore rien. C'etait le cas des
     * brouillons et des reservations en attente de paiement, et c'est desormais aussi celui d'une
     * reservation au portefeuille dont les fonds sont bloques sans etre debites : elle est confirmee
     * des la premiere seconde, mais son annulation ne coute rien a personne tant que le debit n'a
     * pas eu lieu. Sans cette exemption, une ressource au preavis serre deviendrait ininterrompable
     * une minute apres la reservation, alors meme que l'argent n'a pas bouge.</p>
     */
    private void assertCancellationAllowed(Booking booking) {
        ResourcePolicy policy = booking.getResource().getEffectivePolicy();
        // Lot B1 (ecart 10) : rien d'engage tant que la reservation attend (paiement ou approbation) ;
        // l'equipe annule hors regle (elle porte la decision, tracee dans l'historique).
        if (policy == null || booking.getStatus() == BookingStatus.DRAFT
                || booking.getStatus() == BookingStatus.PENDING_PAYMENT
                || booking.getStatus() == BookingStatus.PENDING_APPROVAL
                || isCurrentUserStaff()) {
            return;
        }
        if (booking.getPaymentMode().isPrepaid() && walletSettlement.hasPendingHold(booking)) {
            return;
        }
        if (!Boolean.TRUE.equals(policy.getAllowCancellation())) {
            throw new ConflictException("booking", "resource policy does not allow cancellation");
        }
        if (LocalDateTime.now().plusMinutes(policy.getCancellationNoticeMinutes()).isAfter(booking.getStartedAt())) {
            throw new ConflictException("booking", "cancellation notice is not respected");
        }
    }

    /**
     * Fenetre de pointage autonome, cadree autour de l'heure de debut.
     *
     * <p>Le pointage etait accepte jusqu'a la fin de la reservation. Pointer a la derniere minute
     * d'un creneau de neuf heures valait alors presence pleine, ce qui vidait la notion de presence
     * et rendait le marquage automatique en absence pratiquement inoperant.
     *
     * <p>L'administration reste hors de cette fenetre : c'est elle qui traite les cas particuliers,
     * un client arrive en retard ou un incident d'acces.
     */
    private void assertActivationAllowed(Booking booking, @SuppressWarnings("unused") String action) {
        // Lot B0 (ecart 1) : toute l'equipe d'accueil passe, pas seulement l'administration.
        if (isCurrentUserStaff()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime earliest = booking.getStartedAt().minusMinutes(checkInProperties.getEarlyWindowMinutes());
        LocalDateTime latest = booking.getStartedAt().plusMinutes(checkInProperties.getLateWindowMinutes());
        if (now.isBefore(earliest) || now.isAfter(latest)) {
            throw com.sni.bokaticowork.core.exception.customs.ConflictException.coded("CHECKIN_OUTSIDE_WINDOW", "Le pointage est possible de "
                    + checkInProperties.getEarlyWindowMinutes() + " minutes avant a "
                    + checkInProperties.getLateWindowMinutes()
                    + " minutes après le début de la réservation. Passé ce délai, adressez-vous à l'accueil.");
        }
    }

    /**
     * Passe en cours si l'heure est arrivee · le pointage seul ne suffit pas.
     *
     * <p>Pointer quinze minutes avant l'heure n'ouvre pas la salle pour autant. La reservation
     * reste CONFIRMED, et {@code startCheckedInBookings} l'active a l'heure dite.
     */
    private void startIfHourHasCome(Booking booking) {
        if (booking.getStatus() == BookingStatus.CONFIRMED
                && !LocalDateTime.now().isBefore(booking.getStartedAt())) {
            booking.setStatus(BookingStatus.IN_PROGRESS);
        }
    }

    private boolean isCurrentUserStaff() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> com.sni.bokaticowork.features.ressource.service.support.StaffBookingOverride.ROLES
                        .contains(authority.getAuthority()));
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

    /**
     * Saisit les fonds d'une reservation reglee au portefeuille, au moment ou elle est confirmee.
     *
     * <p>Le blocage vaut verification de solde et vaut paiement : il echoue, la confirmation
     * echoue avec lui et rien n'est annonce au client. Il reussit, le montant est acquis et la
     * reservation est aussi ferme qu'une reservation deja reglee · le debit comptable suivra en
     * arriere-plan. Sans effet pour tout autre mode de reglement.</p>
     */
    private void holdWalletFunds(Booking booking) {
        if (booking.getPaymentMode() == BookingPaymentMode.WALLET) {
            walletSettlement.hold(booking);
        }
    }

    private Booking getForService(String bookingNumber) {
        if (!StringUtils.hasText(bookingNumber)) {
            throw new BadRequestException("Booking number is required");
        }
        return bookingRepository.findByBookingNumber(bookingNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Booking " + bookingNumber + " not found"));
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
        notifyInApp(booking, eventType);
    }

    private boolean emailRequested(BookingStatusChangeRequest request) {
        return request == null || emailRequested(request.sendEmail());
    }

    private boolean emailRequested(Boolean sendEmail) {
        return sendEmail == null || Boolean.TRUE.equals(sendEmail);
    }

    private void notifyInApp(Booking booking, BookingEventType eventType) {
        String subject = switch (eventType) {
            case BOOKING_CREATED, BOOKING_CONFIRMED -> "Reservation confirmee " + booking.getBookingNumber();
            case BOOKING_CANCELLED -> "Reservation annulee " + booking.getBookingNumber();
            default -> "Mise a jour reservation " + booking.getBookingNumber();
        };
        memberInAppNotifier.notify(
                eventType.name(),
                "BOOKING",
                booking.getBookingNumber(),
                booking.getContactEmail(),
                booking.getContactName(),
                booking.getOwnerCode(),
                subject,
                java.util.Map.of(
                        "bookingNumber", booking.getBookingNumber(),
                        "status", booking.getStatus() != null ? booking.getStatus().name() : "",
                        "resourceName", booking.getResource() != null ? booking.getResource().getName() : ""
                )
        );
    }

    private String changedBy(BookingStatusChangeRequest request) {
        return actor(request == null ? null : request.changedBy());
    }

    /**
     * Lot B0 (ecart Bn-18) : « qui a fait » vient de la session ; la valeur de la requete ne sert
     * que sans session (taches du systeme, paiement recu).
     */
    private String actor(String requested) {
        String session = sessionActor();
        return session != null ? session : trim(requested);
    }

    private static String sessionActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            return null;
        }
        String name = authentication.getName();
        return name == null || name.isBlank() || "anonymousUser".equals(name) ? null : name;
    }

    /** Lot B0 : canal deduit de l'appel (portail ou equipe) ; le telephone et l'accueil viennent au lot B5. */
    private static com.sni.bokaticowork.features.booking.enums.BookingChannel currentChannel() {
        try {
            var attributes = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attributes instanceof org.springframework.web.context.request.ServletRequestAttributes servlet) {
                String uri = servlet.getRequest().getRequestURI();
                if (uri != null && (uri.contains("/client/") || uri.contains("/portal/"))) {
                    return com.sni.bokaticowork.features.booking.enums.BookingChannel.PORTAL;
                }
                return com.sni.bokaticowork.features.booking.enums.BookingChannel.TEAM;
            }
        } catch (RuntimeException ignored) {
            // hors requete : tache du systeme
        }
        return null;
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
                // Chaque occurrence herite de l'unite imposee · sinon la premiere serait facturee
                // en demi-journee et les suivantes a l'heure, pour des creneaux identiques.
                base.bookingUnit(),
                base.participants(),
                // Le choix « pour l'organisation » vaut pour chaque occurrence.
                base.billingCompanyCode()
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

    /**
     * L'organisation choisie « pour son compte » · verifiee des la reservation, pas a la facture.
     *
     * <p>Un membre qui n'y est pas rattache est refuse ici, avant que la reservation n'existe :
     * sinon elle passerait, et sa facture echouerait plus tard, a la confirmation.</p>
     */
    private String billingCompany(String requested, BookingIdentityResolver.ResolvedBookingIdentity identity) {
        if (!StringUtils.hasText(requested)) {
            return null;
        }
        String code = requested.trim();
        if (com.sni.bokaticowork.features.company.membership.service.MemberBillingCompanyResolver.NO_COMPANY.equalsIgnoreCase(code)) {
            return com.sni.bokaticowork.features.company.membership.service.MemberBillingCompanyResolver.NO_COMPANY;
        }
        String ownerType = identity.ownerType() == null ? null : identity.ownerType().name();
        return billingCompanyResolver.resolve(ownerType, identity.ownerCode(), code)
                .map(com.sni.bokaticowork.features.client.customer.model.Customer::getCustomerId)
                .orElse(null);
    }

    private static String generateCheckInToken() {
        char[] t = new char[9];
        for (int i = 0; i < 4; i++) t[i] = TOKEN_CHARS[TOKEN_RANDOM.nextInt(TOKEN_CHARS.length)];
        t[4] = '-';
        for (int i = 5; i < 9; i++) t[i] = TOKEN_CHARS[TOKEN_RANDOM.nextInt(TOKEN_CHARS.length)];
        return new String(t);
    }
}
