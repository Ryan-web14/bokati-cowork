package com.sni.bokaticowork.features.booking.service.implementation;

import com.sni.bokaticowork.features.booking.service.support.BookingPriceAdjuster;

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
            throw new ConflictException("Cette clé d'idempotence est déjà utilisée par une autre réservation");
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
        booking.setBookingNumber(CodeComposer.withDay("BKG", CodeComposer.abbrev(resourceTypeName), com.sni.bokaticowork.core.utils.time.AppTime.today(), bookingSeq));
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
        // Lot B2b : canal precise par l'equipe (telephone, accueil, courriel, WhatsApp).
        if (request.channel() != null && request.channel() != com.sni.bokaticowork.features.booking.enums.BookingChannel.PORTAL
                && isCurrentUserStaff()) {
            booking.setChannel(request.channel());
        }
        if (booking.getChannel() == null) {
            booking.setChannel(currentChannel());
        }
        // Lot B3 : rendez-vous sans paiement, regle sur place (paiement direct seulement).
        if (Boolean.TRUE.equals(request.payOnSite())) {
            if (request.paymentMode() != BookingPaymentMode.DIRECT) {
                throw BadRequestException.coded("PAY_ON_SITE_DIRECT_ONLY", "Un rendez-vous réglé sur place se prend en paiement direct (DIRECT)");
            }
            booking.setPayOnSite(true);
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
        // Lot B4 : ajustement manuel du prix ; au-dela du plafond, approbation avant confirmation.
        BookingPriceAdjuster.Plan pricePlan = null;
        if (request.priceAdjustment() != null) {
            if (priceAdjuster == null || !isCurrentUserStaff()) {
                throw new com.sni.bokaticowork.core.exception.customs.ForbiddenException("Seule l'équipe ajuste le prix d'une réservation");
            }
            pricePlan = priceAdjuster.plan(booking.getTotalAmount(), request.priceAdjustment());
        }
        boolean priceApproval = pricePlan != null && pricePlan.needsApproval();
        boolean approval = effectivePolicy.approvalRequired() || onRequest || priceApproval;
        booking.setApprovalRequired(approval);
        booking.setApprovalForPriceOnly(priceApproval && !effectivePolicy.approvalRequired() && !onRequest);
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
                booking.setPaymentDueAt(paymentDue.dueAt(com.sni.bokaticowork.core.utils.time.AppTime.now(), booking.getStartedAt()));
            }
            booking = bookingRepository.save(booking);
        }

        saveLines(booking, price);
        saveParticipants(booking, request);
        if (pricePlan != null) {
            if (pricePlan.needsApproval()) {
                priceAdjuster.requestApproval(booking, pricePlan, sessionActor());
            } else {
                priceAdjuster.apply(booking, pricePlan, sessionActor());
            }
            booking = bookingRepository.findByBookingNumber(booking.getBookingNumber()).orElse(booking);
        }
        // Lot B2b : options prises avec la reservation, sur la meme facture.
        if (request.options() != null && !request.options().isEmpty() && bookingExtras != null) {
            for (var option : request.options()) {
                if (option == null || !StringUtils.hasText(option.code())) {
                    continue;
                }
                String kind = option.kind() == null ? "SERVICE" : option.kind().trim().toUpperCase(java.util.Locale.ROOT);
                if ("AMENITY".equals(kind)) {
                    bookingExtras.addAmenity(booking.getBookingNumber(), option.code(), option.quantity());
                } else if ("SERVICE".equals(kind)) {
                    bookingExtras.addService(booking.getBookingNumber(), option.code(), option.quantity());
                } else {
                    throw new BadRequestException("Option invalide : SERVICE ou AMENITY");
                }
            }
        }
        writeHistory(booking, null, BookingStatus.DRAFT, booking.getCreatedBy(), "Réservation créée");
        eventWriter.write(booking, BookingEventType.BOOKING_CREATED, "Réservation créée", "La réservation a été créée", null);

        if (staffActor != null && !request.startedAt().isAfter(com.sni.bokaticowork.core.utils.time.AppTime.now())) {
            // Debut deja passe : le client a occupe la ressource, la reservation est saisie apres
            // coup. Confirmee aussitot, facture comprise · il ne reste que le paiement.
            booking = confirmInternal(booking, new BookingStatusChangeRequest(staffActor,
                    "Reservation saisie par l'equipe apres le debut", Boolean.FALSE));
            booking = progressStaffBooking(booking, staffActor);
        } else if (approval) {
            writeHistory(booking, BookingStatus.DRAFT, BookingStatus.PENDING_APPROVAL, null, "Approbation requise");
            eventWriter.write(booking, BookingEventType.BOOKING_CREATED, "Réservation en attente d'approbation", "La réservation demande une approbation", null);
        } else if (awaitsPayment) {
            writeHistory(booking, BookingStatus.DRAFT, BookingStatus.PENDING_PAYMENT, null, "En attente de paiement");
            eventWriter.write(booking, BookingEventType.BOOKING_CREATED, "Réservation en attente de paiement", "Réservation en attente de paiement avant confirmation", null);
            booking.setBillableNumber(billableBridge.ensureBillableItem(booking));
            bookingRepository.save(booking);
        } else if (request.confirmImmediately() == null || request.confirmImmediately()) {
            booking = confirmInternal(booking, new BookingStatusChangeRequest(null, "Confirmation automatique", request.sendEmail()));
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
        BookingListSort sort = BookingListSort.of(pageable.getSort());
        Page<Booking> found = bookingRepository.nativeSearch(
                normalizedQuery,
                ownerType == null ? null : ownerType.name(),
                trim(ownerCode),
                trim(resourceCode),
                status == null ? null : status.name(),
                startedFrom,
                startedTo,
                sort.key(),
                sort.direction(),
                unsortedPageable
        );
        // Lot B7 : liste allegee (ni QR ni facture, lignes et participants en une requete par page).
        List<BookingResponse> rows = bookingMapper.toListResponses(found.getContent());
        Page<BookingResponse> page = new org.springframework.data.domain.PageImpl<>(rows,
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort.applied()), found.getTotalElements());
        return new PaginatedResponse<>(page);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> basicSearch(String query) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        return bookingMapper.toListResponses(bookingRepository.basicSearch(query.trim()));
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
            writeHistory(booking, BookingStatus.PENDING_APPROVAL, BookingStatus.PENDING_PAYMENT, booking.getApprovedBy(), request == null ? "Approuvée · en attente de paiement" : reason(request.reason(), "Approuvée · en attente de paiement"));
            eventWriter.write(booking, BookingEventType.BOOKING_APPROVED, "Réservation approuvée", "La réservation a été approuvée, en attente de paiement", null,
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
        writeHistory(booking, BookingStatus.PENDING_APPROVAL, BookingStatus.DRAFT, booking.getApprovedBy(), request == null ? "Approuvée" : reason(request.reason(), "Approuvée"));
        eventWriter.write(booking, BookingEventType.BOOKING_APPROVED, "Réservation approuvée", "La réservation a été approuvée", null);
        return bookingMapper.toResponse(confirmInternal(booking, new BookingStatusChangeRequest(booking.getApprovedBy(), "Approuvée", request == null ? null : request.sendEmail())));
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
        booking.setRejectionReason(request == null ? "Refusée"
                : reasonOf(REJECT_REASON_LIST, request.reasonCode(), request.reasonText(), request.reason(), "Refusée"));
        booking.setRejectionReasonCode(request == null ? null : reasonCodeOf(request.reasonCode()));
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
        eventWriter.write(booking, BookingEventType.BOOKING_REJECTED, "Réservation refusée", booking.getRejectionReason(), null,
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
        writeHistory(booking, from, BookingStatus.IN_PROGRESS, changedBy(request), reason(request, "Réservation démarrée"));
        eventWriter.write(booking, BookingEventType.BOOKING_STARTED, "Réservation démarrée", "Réservation en cours", null,
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
        LocalDateTime now = com.sni.bokaticowork.core.utils.time.AppTime.now();
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
        if (!isOverdueNoShowCandidate(booking, com.sni.bokaticowork.core.utils.time.AppTime.now())) {
            return false;
        }
        markNoShowInternal(booking, new BookingStatusChangeRequest("SYSTEM", "Absence automatique : fin atteinte sans arrivée ni démarrage par l'équipe", Boolean.FALSE));
        return true;
    }

    @Override
    public int startCheckedInBookings(int limit) {
        int resolvedLimit = Math.max(1, limit);
        LocalDateTime now = com.sni.bokaticowork.core.utils.time.AppTime.now();
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
        LocalDateTime now = com.sni.bokaticowork.core.utils.time.AppTime.now();
        return bookingRepository.findOverdueCompletionCandidates(now, Math.max(1, limit)).stream()
                .filter(booking -> isOverdueCompletionCandidate(booking, now))
                .map(Booking::getBookingNumber)
                .toList();
    }

    /** Une reservation a la fois, comme {@link #markOverdueNoShow} · un echec n'arrete pas les autres. */
    @Override
    public boolean markOverdueCompleted(String bookingNumber) {
        Booking booking = getForService(bookingNumber);
        if (!isOverdueCompletionCandidate(booking, com.sni.bokaticowork.core.utils.time.AppTime.now())) {
            return false;
        }
        completeInternal(booking, new BookingStatusChangeRequest("SYSTEM", "Terminée automatiquement : fin atteinte après l'arrivée", Boolean.FALSE));
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
            throw new BadRequestException("Le jeton d'arrivée est requis");
        }
        Booking booking = bookingRepository.findByCheckInToken(checkInToken.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Jeton d'arrivée inconnu"));
        return checkInInternal(booking, request);
    }

    private BookingResponse checkInInternal(Booking booking, BookingCheckRequest request) {
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new ConflictException("Seule une réservation confirmée ou en cours peut être pointée à l'arrivée");
        }
        assertActivationAllowed(booking, "check in");
        booking.setCheckedInAt(Instant.now());
        startIfHourHasCome(booking);
        booking = bookingRepository.save(booking);
        eventWriter.write(booking, BookingEventType.BOOKING_CHECKED_IN, "Arrivée pointée", request == null ? null : request.note(), null,
                emailRequested(request == null ? null : request.sendEmail()));
        notifyIfRequested(booking, BookingEventType.BOOKING_CHECKED_IN, new BookingStatusChangeRequest(actor(request == null ? null : request.actor()), request == null ? null : request.note(), request == null ? null : request.sendEmail()));
        return bookingMapper.toResponse(booking);
    }

    @Override
    public BookingResponse checkOut(String bookingNumber, BookingCheckRequest request) {
        Booking booking = getForService(bookingNumber);
        if (booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new ConflictException("Seule une réservation en cours peut être pointée au départ");
        }
        booking.setCheckedOutAt(Instant.now());
        booking = bookingRepository.save(booking);
        // Lot R5c : depassement mesure au depart, selon la politique.
        if (bookingExtras != null) {
            bookingExtras.onCheckOut(booking);
        }
        eventWriter.write(booking, BookingEventType.BOOKING_CHECKED_OUT, "Départ pointé", request == null ? null : request.note(), null,
                emailRequested(request == null ? null : request.sendEmail()));
        notifyIfRequested(booking, BookingEventType.BOOKING_CHECKED_OUT, new BookingStatusChangeRequest(actor(request == null ? null : request.actor()), request == null ? null : request.note(), request == null ? null : request.sendEmail()));
        return bookingMapper.toResponse(booking);
    }

    @Override
    public BookingResponse earlyCheckIn(String bookingNumber, BookingCheckRequest request) {
        Booking booking = getForService(bookingNumber);
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new ConflictException("Seule une réservation confirmée ou en cours peut être pointée à l'arrivée");
        }
        booking.setCheckedInAt(Instant.now());
        startIfHourHasCome(booking);
        booking = bookingRepository.save(booking);
        eventWriter.write(booking, BookingEventType.BOOKING_CHECKED_IN, "Arrivée anticipée validée par l'équipe",
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
                "Réservation transférée à " + request.newOwnerCode(), request.note(), null,
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
            throw new BadRequestException("La nouvelle fin doit être après le nouveau début");
        }
        Resource resource = booking.getResource();
        validateForReschedule(resource, newStart, newEnd, bookingNumber, booking);
        requirePriceDecision(booking, resource, newStart, newEnd, request.acceptPriceChange());
        // Lot B1 (ecart 3) : seuls des creneaux vraiment retenus sont rendus puis repris (la reprise
        // controle la capacite) ; une reservation qui ne retenait rien n'en prend pas.
        if (Boolean.TRUE.equals(booking.getSlotsReserved())) {
            releaseResourceSlot(resource, booking.getStartedAt(), booking.getEndedAt(), booking.getQuantity());
            eventWriter.write(booking, BookingEventType.RESOURCE_RELEASED, "Créneau rendu pour le report",
                    booking.getStartedAt() + " – " + booking.getEndedAt(), null);
            reserveResourceSlot(resource, newStart, newEnd, booking.getQuantity());
            eventWriter.write(booking, BookingEventType.RESOURCE_RESERVED, "Créneau retenu après le report",
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
            markModified(booking, "Report au " + newStart + " – " + newEnd,
                    repricer.reprice(booking, "nouvel horaire " + newStart + " – " + newEnd));
        } else {
            markModified(booking, "Report au " + newStart + " – " + newEnd, null);
        }
        booking = bookingRepository.save(booking);
        writeHistory(booking, from, booking.getStatus(), actor(request.actor()), reason(request.note(), "Réservation reportée"));
        eventWriter.write(booking, BookingEventType.BOOKING_RESCHEDULED, "Réservation reportée",
                "Nouveau créneau : " + newStart + " – " + newEnd, null,
                emailRequested(request.sendEmail()));
        notifyIfRequested(booking, BookingEventType.BOOKING_RESCHEDULED,
                new BookingStatusChangeRequest(actor(request.actor()), request.note(), request.sendEmail()));
        return bookingMapper.toResponse(booking);
    }

    @Override
    public BookingResponse changeResource(String bookingNumber, BookingChangeResourceRequest request) {
        Booking booking = getForService(bookingNumber);
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ConflictException("Seule une réservation confirmée peut changer de ressource");
        }
        Resource newResource = resourceService.getResourceForService(request.newResourceCode().trim());
        validateForReschedule(newResource, booking.getStartedAt(), booking.getEndedAt(), bookingNumber, booking);
        requirePriceDecision(booking, newResource, booking.getStartedAt(), booking.getEndedAt(), request.acceptPriceChange());
        Resource oldResource = booking.getResource();
        boolean retained = Boolean.TRUE.equals(booking.getSlotsReserved());
        if (retained) {
            releaseResourceSlot(oldResource, booking.getStartedAt(), booking.getEndedAt(), booking.getQuantity());
            eventWriter.write(booking, BookingEventType.RESOURCE_RELEASED, "Créneau rendu pour le changement de ressource",
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
            eventWriter.write(booking, BookingEventType.RESOURCE_RESERVED, "Créneau retenu après le changement de ressource",
                    newResource.getCode(), null);
        }
        // Lot R1 (D1) : prix de la nouvelle ressource ; ecart facture en complement ou rendu par un avoir.
        String change = "Ressource " + oldResource.getCode() + " → " + newResource.getCode();
        markModified(booking, change, repricer == null ? null
                : repricer.reprice(booking, "ressource " + oldResource.getCode() + " → " + newResource.getCode()));
        booking = bookingRepository.save(booking);
        eventWriter.write(booking, BookingEventType.BOOKING_RESOURCE_CHANGED,
                "Ressource changée : " + oldResource.getCode() + " → " + newResource.getCode(),
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
        eventWriter.write(booking, BookingEventType.BOOKING_PARTICIPANT_ADDED, "Participant ajouté", participantLabel(participant), null);
        return bookingMapper.toResponse(participant);
    }

    @Override
    public BookingParticipantResponse removeParticipant(String bookingNumber, Long participantId) {
        Booking booking = getForService(bookingNumber);
        BookingParticipant participant = participantRepository.findByIdAndBookingId(participantId, booking.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Participant introuvable"));
        participant.setStatus(BookingParticipantStatus.REMOVED);
        participant = participantRepository.save(participant);
        eventWriter.write(booking, BookingEventType.BOOKING_PARTICIPANT_REMOVED, "Participant retiré", participantLabel(participant), null);
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
        Instant startedAt = booking.getStartedAt().atZone(com.sni.bokaticowork.core.utils.time.AppTime.zone()).toInstant();
        booking.setStartedEventAt(startedAt);
        if (!booking.getEndedAt().isAfter(com.sni.bokaticowork.core.utils.time.AppTime.now())) {
            return completeInternal(booking, new BookingStatusChangeRequest(staffActor,
                    "Reservation passee saisie par l'equipe · faite, reste le paiement", Boolean.FALSE));
        }
        BookingStatus from = booking.getStatus();
        booking.setStatus(BookingStatus.IN_PROGRESS);
        booking = bookingRepository.save(booking);
        writeHistory(booking, from, BookingStatus.IN_PROGRESS, staffActor, "Reservation en cours saisie par l'equipe");
        eventWriter.write(booking, BookingEventType.BOOKING_STARTED, "Réservation démarrée", "Réservation en cours", null);
        return booking;
    }

    private Booking confirmInternal(Booking booking, BookingStatusChangeRequest request) {
        if (booking.getStatus() != BookingStatus.DRAFT && booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new ConflictException("Une réservation " + booking.getStatus().label() + " ne se confirme pas (en brouillon ou en attente de paiement seulement)");
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
            eventWriter.write(booking, BookingEventType.ENTITLEMENT_RESERVED, "Droit retenu", "Droit retenu pour la réservation", null);
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
        writeHistory(booking, from, BookingStatus.CONFIRMED, changedBy(request), reason(request, "Réservation confirmée"));
        eventWriter.write(booking, BookingEventType.BOOKING_CONFIRMED, "Réservation confirmée", "La réservation a été confirmée", null,
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
            eventWriter.write(booking, BookingEventType.ENTITLEMENT_RELEASED, "Droit rendu", "Droit retenu rendu", null);
        }
        walletSettlement.release(booking, "confirmation annulee");
        booking.setPaidAt(null);
        booking.setConfirmedAt(null);
        if (paymentDue != null) {
            booking.setPaymentDueAt(paymentDue.dueAt(com.sni.bokaticowork.core.utils.time.AppTime.now(), booking.getStartedAt()));
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
                eventWriter.write(booking, BookingEventType.ENTITLEMENT_RELEASED, "Droit rendu", "Droit retenu rendu", null);
            }
        }
        walletSettlement.release(booking, "reservation annulee");
        BookingStatus from = booking.getStatus();
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(request == null ? "Réservation annulée"
                : reasonOf(CANCEL_REASON_LIST, request.reasonCode(), request.reasonText(), request.reason(), "Réservation annulée"));
        booking.setCancellationReasonCode(request == null ? null : reasonCodeOf(request.reasonCode()));
        booking.setCancelledAt(Instant.now());
        booking = bookingRepository.save(booking);
        // Lot R5d : taches ouvertes de la reservation annulees avec elle.
        if (bookingOperations != null) {
            bookingOperations.cancelTasks(booking);
        }
        writeHistory(booking, from, BookingStatus.CANCELLED, changedBy(request), booking.getCancellationReason());
        eventWriter.write(booking, BookingEventType.BOOKING_CANCELLED, "Réservation annulée", booking.getCancellationReason(), null,
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
            throw new ConflictException("Seule une réservation confirmée ou en cours peut être marquée absente");
        }
        // Lot B1 (ecart Bn-10) : absent seulement une fois la fenetre d'arrivee passee.
        LocalDateTime noShowFrom = booking.getStartedAt().plusMinutes(checkInProperties.getLateWindowMinutes());
        if (com.sni.bokaticowork.core.utils.time.AppTime.now().isBefore(noShowFrom)) {
            throw new ConflictException("Trop tôt pour constater une absence : possible à partir de " + noShowFrom);
        }
        if (booking.getPaymentMode().usesEntitlement() && entitlementBridge.consume(booking)) {
            eventWriter.write(booking, BookingEventType.ENTITLEMENT_CONSUMED, "Droit consommé", "Droit consommé pour l'absence", null);
        }
        BookingStatus from = booking.getStatus();
        booking.setStatus(BookingStatus.NO_SHOW);
        booking.setCompletedAt(Instant.now());
        booking = bookingRepository.save(booking);
        // Lot R5c : frais de non-presentation de la politique.
        if (bookingExtras != null) {
            bookingExtras.onNoShow(booking);
        }
        // Lot B3 : trop d'absences sur des rendez-vous regles sur place, option retiree d'office.
        if (payOnSite != null) {
            try {
                payOnSite.onNoShow(booking);
            } catch (RuntimeException ex) {
                log.warn("Option de reglement sur place de {} non revue : {}", booking.getOwnerCode(), ex.getMessage());
            }
        }
        writeHistory(booking, from, BookingStatus.NO_SHOW, changedBy(request), reason(request, "Absence"));
        eventWriter.write(booking, BookingEventType.BOOKING_NO_SHOW, "Réservation marquée absente", "La réservation a été marquée absente", null,
                emailRequested(request));
        notifyIfRequested(booking, BookingEventType.BOOKING_NO_SHOW, request);
        return booking;
    }

    private Booking completeInternal(Booking booking, BookingStatusChangeRequest request) {
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new ConflictException("Seule une réservation confirmée ou en cours peut être terminée");
        }
        // Lot B1 (ecart Bn-10) : on ne clot pas une reservation qui n'a pas commence.
        if (com.sni.bokaticowork.core.utils.time.AppTime.now().isBefore(booking.getStartedAt())) {
            throw new ConflictException("La réservation n'a pas commencé : elle ne se clôt pas avant " + booking.getStartedAt());
        }
        if (booking.getPaymentMode().usesEntitlement() && entitlementBridge.consume(booking)) {
            eventWriter.write(booking, BookingEventType.ENTITLEMENT_CONSUMED, "Droit consommé", "Droit consommé par la réservation", null);
            eventWriter.write(booking, BookingEventType.USAGE_RECORDED, "Utilisation enregistrée", "Utilisation de la réservation enregistrée", null);
        }
        BookingStatus from = booking.getStatus();
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(Instant.now());
        booking = bookingRepository.save(booking);
        writeHistory(booking, from, BookingStatus.COMPLETED, changedBy(request), reason(request, "Réservation terminée"));
        eventWriter.write(booking, BookingEventType.BOOKING_COMPLETED, "Réservation terminée", "La réservation est terminée", null,
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
                    booking.getStartedAt().minusHours(Math.max(0, bookingPrepHours())).atZone(com.sni.bokaticowork.core.utils.time.AppTime.zone()).toInstant(),
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
                    booking.getEndedAt().atZone(com.sni.bokaticowork.core.utils.time.AppTime.zone()).toInstant(),
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
            eventWriter.write(booking, BookingEventType.RESOURCE_RESERVED, "Créneau retenu", "Créneau de la ressource retenu", null);
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
            eventWriter.write(booking, BookingEventType.RESOURCE_RELEASED, "Créneau rendu", "Créneau de la ressource rendu", null);
            booking.setSlotsReserved(Boolean.FALSE);
        }
    }

    private static String statusLabel(BookingStatus status) {
        return status == null ? "" : status.label();
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

    // ------------------------------------------------------------------ lot B2b : reports et modifications

    /**
     * Plus cher : la hausse doit etre confirmee par l'equipe ({@code acceptPriceChange}), car un
     * complement sera facture et encaisse. Moins cher : un avoir suit, sans confirmation.
     */
    private void requirePriceDecision(Booking booking, Resource resource, LocalDateTime start, LocalDateTime end, Boolean accepted) {
        if (repricer == null) {
            return;
        }
        var preview = repricer.preview(booking, resource, start, end);
        boolean payable = booking.getPaymentMode() == null || booking.getPaymentMode().isPayable() || booking.getCoveredMinutes() != null;
        if (payable && preview.difference().signum() > 0 && !Boolean.TRUE.equals(accepted)) {
            throw com.sni.bokaticowork.core.exception.customs.ConflictException.coded("PRICE_INCREASE_CONFIRMATION_REQUIRED",
                    "Le nouveau prix est plus élevé : " + plain(preview.current()) + " → " + plain(preview.next()) + " "
                            + booking.getCurrency() + " (+" + plain(preview.difference()) + "). Confirmez avec acceptPriceChange=true :"
                            + " le complément sera facturé et à encaisser.");
        }
    }

    /** Marque la reservation comme modifiee ; une hausse deja facturee laisse le complement a encaisser. */
    private void markModified(Booking booking, String description, BookingRepricer.Outcome outcome) {
        booking.setModificationCount((booking.getModificationCount() == null ? 0 : booking.getModificationCount()) + 1);
        booking.setLastModifiedAt(Instant.now());
        String priceNote = "";
        if (outcome != null && outcome.difference() != null && outcome.difference().signum() != 0) {
            priceNote = " · " + (outcome.difference().signum() > 0 ? "+" : "") + plain(outcome.difference()) + " " + booking.getCurrency()
                    + (outcome.adjustmentDocument() == null ? "" : " (pièce " + outcome.adjustmentDocument() + ")");
            if (outcome.difference().signum() > 0 && outcome.adjustmentDocument() != null) {
                // Complement emis : la reservation n'est plus entierement reglee.
                booking.setPaidAt(null);
            }
        }
        String text = description + priceNote;
        booking.setLastModification(text.length() > 300 ? text.substring(0, 300) : text);
        informStaff(booking, text, outcome);
    }

    private com.sni.bokaticowork.features.portal.notification.service.AdminInAppNotifier adminNotifier;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setAdminNotifier(@org.springframework.context.annotation.Lazy
                          com.sni.bokaticowork.features.portal.notification.service.AdminInAppNotifier adminNotifier) {
        this.adminNotifier = adminNotifier;
    }

    /**
     * Lot B2b : l'equipe est informee de chaque report ou changement (alerte en direct dans le
     * back-office) ; l'agent qui a cree la reservation recoit aussi une notification dans l'appli.
     * Une hausse facturee est signalee « à encaisser ».
     */
    private void informStaff(Booking booking, String text, BookingRepricer.Outcome outcome) {
        if (adminNotifier == null) {
            return;
        }
        boolean toCollect = outcome != null && outcome.difference() != null && outcome.difference().signum() > 0;
        String title = "Réservation " + booking.getBookingNumber() + " modifiée" + (toCollect ? " : complément à encaisser" : "");
        String by = sessionActor();
        String message = text + (StringUtils.hasText(by) ? " (par " + by + ")" : "");
        try {
            String payload = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of(
                    "bookingNumber", booking.getBookingNumber(),
                    "modificationCount", booking.getModificationCount(),
                    "difference", outcome == null || outcome.difference() == null ? "0" : outcome.difference().toPlainString(),
                    "adjustmentDocument", outcome == null || outcome.adjustmentDocument() == null ? "" : outcome.adjustmentDocument()));
            adminNotifier.broadcastAlert("BOOKING_MODIFIED", "BOOKING", title, message, toCollect ? "WARNING" : "INFO", payload);
            String creator = booking.getCreatedBy();
            if (StringUtils.hasText(creator) && creator.contains("@") && !creator.equalsIgnoreCase(by)) {
                adminNotifier.notify("BOOKING_MODIFIED", "BOOKING", booking.getBookingNumber(), creator, title,
                        java.util.Map.of("bookingNumber", booking.getBookingNumber(), "message", message));
            }
        } catch (Exception ex) {
            log.warn("Alerte de modification de {} non envoyee : {}", booking.getBookingNumber(), ex.getMessage());
        }
    }

    private static String plain(java.math.BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    @Override
    public com.sni.bokaticowork.features.booking.dto.response.BookingChangePreviewResponse previewReschedule(
            String bookingNumber, LocalDateTime newStart, LocalDateTime newEnd) {
        Booking booking = getForService(bookingNumber);
        if (newStart == null || newEnd == null || !newEnd.isAfter(newStart)) {
            throw new BadRequestException("Le nouveau début et la nouvelle fin sont requis, la fin après le début");
        }
        return preview(booking, booking.getResource(), newStart, newEnd);
    }

    @Override
    public com.sni.bokaticowork.features.booking.dto.response.BookingChangePreviewResponse previewChangeResource(
            String bookingNumber, String newResourceCode) {
        Booking booking = getForService(bookingNumber);
        Resource resource = resourceService.getResourceForService(newResourceCode.trim());
        return preview(booking, resource, booking.getStartedAt(), booking.getEndedAt());
    }

    private com.sni.bokaticowork.features.booking.dto.response.BookingChangePreviewResponse preview(
            Booking booking, Resource resource, LocalDateTime start, LocalDateTime end) {
        var preview = repricer == null ? null : repricer.preview(booking, resource, start, end);
        java.math.BigDecimal current = preview == null ? booking.getTotalAmount() : preview.current();
        java.math.BigDecimal next = preview == null ? booking.getTotalAmount() : preview.next();
        java.math.BigDecimal difference = preview == null ? java.math.BigDecimal.ZERO : preview.difference();
        return new com.sni.bokaticowork.features.booking.dto.response.BookingChangePreviewResponse(booking.getBookingNumber(),
                current, next, difference, difference.signum() > 0 ? "INCREASE" : difference.signum() < 0 ? "DECREASE" : "SAME",
                booking.getCurrency(), StringUtils.hasText(booking.getBillableNumber()),
                booking.getModificationCount() == null ? 0 : booking.getModificationCount(),
                booking.getLastModifiedAt(), booking.getLastModification());
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
        for (Booking booking : bookingRepository.findUnpaidPastDue(com.sni.bokaticowork.core.utils.time.AppTime.now(), Math.max(1, limit))) {
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
        alertExpired(booking);
        eventWriter.write(booking, BookingEventType.BOOKING_CANCELLED, "Réservation expirée",
                "Non réglée à l'échéance : créneau rendu", null, true);
        try {
            cancellationRefundSupport.processCancellation(booking, false);
        } catch (Exception ex) {
            log.warn("Facture de la reservation expiree {} non annulee : {}", booking.getBookingNumber(), ex.getMessage());
        }
    }

    private com.sni.bokaticowork.features.booking.service.support.BookingPayOnSite payOnSite;

    private BookingPriceAdjuster priceAdjuster;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setPriceAdjuster(@org.springframework.context.annotation.Lazy BookingPriceAdjuster priceAdjuster) {
        this.priceAdjuster = priceAdjuster;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setPayOnSite(@org.springframework.context.annotation.Lazy com.sni.bokaticowork.features.booking.service.support.BookingPayOnSite payOnSite) {
        this.payOnSite = payOnSite;
    }

    @Override
    public BookingResponse adjustPrice(String bookingNumber,
                                       com.sni.bokaticowork.features.booking.dto.request.BookingPriceAdjustmentRequest request) {
        if (priceAdjuster == null) {
            throw new ConflictException("Ajustement de prix indisponible");
        }
        return bookingMapper.toResponse(priceAdjuster.adjust(bookingNumber, request));
    }

    /** Lot B3 (decision D2) : l'equipe est prevenue qu'un rendez-vous non regle a ete libere. */
    private void alertExpired(Booking booking) {
        if (adminNotifier == null) {
            return;
        }
        try {
            String title = "Réservation " + booking.getBookingNumber() + " libérée faute de règlement";
            String message = (Boolean.TRUE.equals(booking.getPayOnSite()) ? "Rendez-vous réglé sur place non honoré" : "Paiement non reçu")
                    + " à l'échéance : " + booking.getResource().getCode() + ", " + booking.getStartedAt() + ", " + booking.getContactName();
            String payload = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of(
                    "bookingNumber", booking.getBookingNumber(), "payOnSite", Boolean.TRUE.equals(booking.getPayOnSite()),
                    "resourceCode", booking.getResource().getCode()));
            adminNotifier.broadcastAlert("BOOKING_EXPIRED", "BOOKING", title, message, "WARNING", payload);
        } catch (Exception ex) {
            log.warn("Alerte d'expiration de {} non envoyee : {}", booking.getBookingNumber(), ex.getMessage());
        }
    }

    static final String REMINDER_MINUTES_KEY = "bookings.payment-due.reminder-minutes";

    /**
     * Lot B3 (decision D2) : rappel au client (courriel) quand l'echeance de paiement approche
     * ({@code bookings.payment-due.reminder-minutes}, 30 par defaut ; 0 coupe le rappel). Un seul
     * rappel par reservation.
     */
    @Override
    public int remindPaymentsDue(int limit) {
        int minutes = platformSettings == null ? 30 : platformSettings.integer(REMINDER_MINUTES_KEY, 30);
        if (minutes <= 0) {
            return 0;
        }
        LocalDateTime now = com.sni.bokaticowork.core.utils.time.AppTime.now();
        int sent = 0;
        for (Booking booking : bookingRepository.findPaymentRemindersDue(now, now.plusMinutes(minutes), Math.max(1, limit))) {
            try {
                booking.setPaymentReminderSentAt(Instant.now());
                bookingRepository.save(booking);
                eventWriter.write(booking, BookingEventType.BOOKING_PAYMENT_REMINDER, "Rappel de règlement",
                        "Échéance de paiement le " + booking.getPaymentDueAt(), null, true);
                sent++;
            } catch (RuntimeException ex) {
                log.warn("Rappel de reglement de {} non envoye : {}", booking.getBookingNumber(), ex.getMessage());
            }
        }
        return sent;
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
                .orElseThrow(() -> new ResourceNotFoundException("Pré-réservation introuvable : " + booking.getHoldNumber()));
        if (hold.getStatus() != BookingHoldStatus.ACTIVE || hold.getExpiresAt().isBefore(Instant.now())) {
            throw new ConflictException("La pré-réservation n'est plus active");
        }
        // Lot B1 (ecart Bn-13) : un invite recoit un nouveau code a chaque appel ; on le reconnait a
        // ses coordonnees.
        boolean sameGuest = hold.getOwnerType() == booking.getOwnerType()
                && ((StringUtils.hasText(hold.getContactEmail()) && hold.getContactEmail().equalsIgnoreCase(booking.getContactEmail()))
                || (StringUtils.hasText(hold.getContactPhone()) && hold.getContactPhone().equals(booking.getContactPhone())));
        if (!sameGuest && (hold.getOwnerType() != booking.getOwnerType() || !hold.getOwnerCode().equals(booking.getOwnerCode()))) {
            throw new ConflictException("La pré-réservation appartient à un autre titulaire");
        }
        if (!hold.getResource().getId().equals(booking.getResource().getId())
                || !hold.getStartedAt().equals(booking.getStartedAt())
                || !hold.getEndedAt().equals(booking.getEndedAt())
                || !hold.getQuantity().equals(booking.getQuantity())) {
            throw new ConflictException("La pré-réservation porte sur un autre créneau");
        }
        hold.setStatus(BookingHoldStatus.CONFIRMED);
        holdRepository.save(hold);
        eventWriter.write(booking, BookingEventType.BOOKING_HOLD_CREATED, "Pré-réservation confirmée", hold.getHoldNumber(), null);
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
        // Lot B2b : la politique d'annulation de la ressource s'applique a tous, equipe comprise.
        if (policy == null || booking.getStatus() == BookingStatus.DRAFT
                || booking.getStatus() == BookingStatus.PENDING_PAYMENT
                || booking.getStatus() == BookingStatus.PENDING_APPROVAL) {
            return;
        }
        if (booking.getPaymentMode().isPrepaid() && walletSettlement.hasPendingHold(booking)) {
            return;
        }
        if (!Boolean.TRUE.equals(policy.getAllowCancellation())) {
            throw new ConflictException("La politique de la ressource n'autorise pas l'annulation");
        }
        if (com.sni.bokaticowork.core.utils.time.AppTime.now().plusMinutes(policy.getCancellationNoticeMinutes()).isAfter(booking.getStartedAt())) {
            throw new ConflictException("Le délai d'annulation de la ressource est dépassé");
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
        LocalDateTime now = com.sni.bokaticowork.core.utils.time.AppTime.now();
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
                && !com.sni.bokaticowork.core.utils.time.AppTime.now().isBefore(booking.getStartedAt())) {
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
            throw new BadRequestException("Le numéro de réservation est requis");
        }
        return bookingRepository.findByBookingNumber(bookingNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable : " + bookingNumber));
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
            throw new ConflictException("Action impossible : la réservation est " + booking.getStatus().label() + ", il faut qu'elle soit " + expected.label());
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

    /** Lot B7 : listes de motifs de l'administrateur. */
    public static final String CANCEL_REASON_LIST = "BOOKING_CANCEL_REASON";
    public static final String REJECT_REASON_LIST = "BOOKING_REJECT_REASON";

    private com.sni.bokaticowork.core.reference.service.ReferenceLists referenceLists;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setReferenceLists(com.sni.bokaticowork.core.reference.service.ReferenceLists referenceLists) {
        this.referenceLists = referenceLists;
    }

    /**
     * Motif : code de la liste plus precision ; sans code, l'ancien texte libre ; sinon le defaut.
     * Le code est controle (inconnu ou retire : 400).
     */
    private String reasonOf(String list, String code, String text, String legacy, String fallback) {
        if (StringUtils.hasText(code) && referenceLists != null) {
            return referenceLists.describe(list, code, text);
        }
        if (StringUtils.hasText(text)) {
            return text.trim();
        }
        return reason(legacy, fallback);
    }

    private String reasonCodeOf(String code) {
        return StringUtils.hasText(code) && referenceLists != null
                ? com.sni.bokaticowork.core.reference.service.ReferenceLists.normalize(code) : null;
    }

    private static String participantLabel(BookingParticipant participant) {
        String name = StringUtils.hasText(participant.getName()) ? participant.getName().trim() : null;
        String email = StringUtils.hasText(participant.getEmail()) ? participant.getEmail().trim() : null;
        if (name != null && email != null) {
            return name + " (" + email + ")";
        }
        return name != null ? name : email;
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
        String base = "Réservation de ressource · " + booking.getResource().getName();
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
        return "Consommation du droit · " + booking.getEntitlementCode();
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
