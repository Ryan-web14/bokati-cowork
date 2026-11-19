package com.sni.bokaticowork.features.booking.service.implementation;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.time.AppTime;
import com.sni.bokaticowork.features.booking.dto.request.BookingAvailabilityRequest;
import com.sni.bokaticowork.features.booking.dto.request.CreateBookingRequest;
import com.sni.bokaticowork.features.booking.dto.request.JoinBookingWaitlistRequest;
import com.sni.bokaticowork.features.booking.dto.response.BookingAvailabilityResponse;
import com.sni.bokaticowork.features.booking.dto.response.BookingResponse;
import com.sni.bokaticowork.features.booking.dto.response.BookingSuggestionResponse;
import com.sni.bokaticowork.features.booking.dto.response.BookingWaitlistEntryResponse;
import com.sni.bokaticowork.features.booking.enums.BookingWaitlistStatus;
import com.sni.bokaticowork.features.booking.model.BookingWaitlistEntry;
import com.sni.bokaticowork.features.booking.repository.BookingWaitlistEntryRepository;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingAvailabilityService;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingSuggestionService;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingWaitlistService;
import com.sni.bokaticowork.features.booking.service.support.BookingIdentityResolver;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceService;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

/**
 * Liste d'attente (refaite au lot B6, ecarts 6 et Bn-15).
 *
 * <ul>
 *   <li>Seuls les creneaux a venir sont proposes ; une inscription dont le creneau est passe expire.</li>
 *   <li>Une place est proposee quand la capacite le permet (controle de disponibilite complet, et
 *   non plus « aucune reservation sur la plage »), a une seule inscription a la fois par creneau :
 *   la plus ancienne. Elle a {@code bokati.booking.waitlist.offer-ttl-minutes} pour accepter.</li>
 *   <li>Le contact est prevenu par courriel a l'offre.</li>
 *   <li>Accepter cree la reservation ({@code BOOKED}, numero garde) ; decliner ou laisser expirer
 *   passe la main a l'inscription suivante.</li>
 * </ul>
 */
@Slf4j
@Service
@Transactional
public class BookingWaitlistServiceImpl implements BookingWaitlistService {

    private static final String OFFER_TTL_KEY = "bokati.booking.waitlist.offer-ttl-minutes";
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("EEEE d MMMM 'à' HH:mm", Locale.FRENCH);
    static final int MAX_PAGE = 100;

    private final BookingWaitlistEntryRepository waitlistRepository;
    private final ResourceService resourceService;
    private final BookingIdentityResolver identityResolver;
    private final BookingSuggestionService suggestionService;
    private final BookingAvailabilityService availabilityService;
    private final ObjectProvider<BookingService> bookingService;

    @Value("${bokati.booking.waitlist.offer-ttl-minutes:30}")
    private int offerTtlMinutes;

    private PlatformSettings platformSettings;
    private DefaultEmailSender emailSender;

    public BookingWaitlistServiceImpl(BookingWaitlistEntryRepository waitlistRepository, ResourceService resourceService,
                                      BookingIdentityResolver identityResolver, BookingSuggestionService suggestionService,
                                      BookingAvailabilityService availabilityService, ObjectProvider<BookingService> bookingService) {
        this.waitlistRepository = waitlistRepository;
        this.resourceService = resourceService;
        this.identityResolver = identityResolver;
        this.suggestionService = suggestionService;
        this.availabilityService = availabilityService;
        this.bookingService = bookingService;
    }

    @Autowired(required = false)
    void setPlatformSettings(PlatformSettings platformSettings) {
        this.platformSettings = platformSettings;
    }

    @Autowired(required = false)
    void setEmailSender(DefaultEmailSender emailSender) {
        this.emailSender = emailSender;
    }

    private int offerTtlMinutes() {
        return platformSettings == null ? offerTtlMinutes : platformSettings.integer(OFFER_TTL_KEY, offerTtlMinutes);
    }

    @Override
    public BookingWaitlistEntryResponse join(JoinBookingWaitlistRequest request) {
        if (!request.endedAt().isAfter(request.startedAt())) {
            throw new BadRequestException("La fin doit être après le début");
        }
        Resource resource = resourceService.getResourceForService(request.resourceCode().trim());
        BookingIdentityResolver.ResolvedBookingIdentity identity = identityResolver.resolve(request.identityLookup());
        BookingWaitlistEntry entry = BookingWaitlistEntry.builder()
                .resource(resource)
                .ownerType(identity.ownerType())
                .ownerCode(identity.ownerCode())
                .contactName(identity.contactName())
                .contactEmail(identity.contactEmail())
                .contactPhone(identity.contactPhone())
                .startedAt(request.startedAt())
                .endedAt(request.endedAt())
                .quantity(request.quantity() == null ? 1 : request.quantity())
                .paymentMode(request.paymentMode())
                .status(BookingWaitlistStatus.WAITING)
                .build();
        return toResponse(waitlistRepository.save(entry), alternatives(entry));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingWaitlistEntryResponse> list(BookingWaitlistStatus status) {
        BookingWaitlistStatus resolved = status == null ? BookingWaitlistStatus.WAITING : status;
        return waitlistRepository.findAllByStatusOrderByCreatedAtAsc(resolved).stream()
                .map(entry -> toResponse(entry, List.of()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<BookingWaitlistEntryResponse> search(BookingWaitlistStatus status, String resourceCode, SubscriberType ownerType,
                                                                  String ownerCode, Pageable pageable) {
        Pageable page = PageRequest.of(Math.max(0, pageable.getPageNumber()), Math.min(Math.max(1, pageable.getPageSize()), MAX_PAGE));
        Page<BookingWaitlistEntry> found = waitlistRepository.search(status == null ? null : status.name(), trim(resourceCode),
                ownerType == null ? null : ownerType.name(), trim(ownerCode), page);
        // Alternatives seulement pour une inscription encore en attente : elles coutent une recherche chacune.
        return new PaginatedResponse<>(found.map(entry -> toResponse(entry,
                entry.getStatus() == BookingWaitlistStatus.WAITING ? alternatives(entry) : List.of())));
    }

    @Override
    @Transactional(readOnly = true)
    public BookingWaitlistEntryResponse get(Long id) {
        return toResponse(entry(id), List.of());
    }

    @Override
    public BookingWaitlistEntryResponse cancel(Long id) {
        BookingWaitlistEntry entry = entry(id);
        if (entry.getStatus() == BookingWaitlistStatus.BOOKED) {
            throw new ConflictException("Cette inscription a déjà donné une réservation : annulez la réservation " + entry.getBookingNumber());
        }
        entry.setStatus(BookingWaitlistStatus.CANCELLED);
        return toResponse(waitlistRepository.save(entry), List.of());
    }

    @Override
    public BookingWaitlistEntryResponse accept(Long id) {
        BookingWaitlistEntry entry = entry(id);
        if (entry.getStatus() != BookingWaitlistStatus.OFFERED) {
            throw ConflictException.coded("WAITLIST_OFFER_NOT_OPEN", "Aucune place n'est proposée sur cette inscription (état : "
                    + entry.getStatus() + ")");
        }
        if (entry.getExpiresAt() != null && entry.getExpiresAt().isBefore(Instant.now())) {
            entry.setStatus(BookingWaitlistStatus.EXPIRED);
            waitlistRepository.save(entry);
            throw ConflictException.coded("WAITLIST_OFFER_EXPIRED", "L'offre a expiré : la place a été proposée à la personne suivante");
        }
        BookingResponse booking = bookingService.getObject().create(createRequest(entry));
        entry.setStatus(BookingWaitlistStatus.BOOKED);
        entry.setBookingNumber(booking.bookingNumber());
        return toResponse(waitlistRepository.save(entry), List.of());
    }

    @Override
    public BookingWaitlistEntryResponse decline(Long id) {
        BookingWaitlistEntry entry = entry(id);
        if (entry.getStatus() != BookingWaitlistStatus.OFFERED && entry.getStatus() != BookingWaitlistStatus.WAITING) {
            throw new ConflictException("Cette inscription n'est plus en attente");
        }
        entry.setStatus(BookingWaitlistStatus.CANCELLED);
        BookingWaitlistEntryResponse response = toResponse(waitlistRepository.save(entry), List.of());
        promoteAvailable(20);
        return response;
    }

    @Override
    public int promoteAvailable(int limit) {
        LocalDateTimeHolder now = new LocalDateTimeHolder(AppTime.now());
        for (BookingWaitlistEntry past : waitlistRepository.findPastWaiting(now.value(), Math.max(1, limit))) {
            past.setStatus(BookingWaitlistStatus.EXPIRED);
            waitlistRepository.save(past);
        }
        int promoted = 0;
        for (BookingWaitlistEntry entry : waitlistRepository.findWaitingCandidates(now.value(), Math.max(1, limit))) {
            if (waitlistRepository.existsOpenOffer(entry.getResource().getId(), entry.getStartedAt(), entry.getEndedAt())) {
                continue;
            }
            if (!available(entry)) {
                continue;
            }
            entry.setStatus(BookingWaitlistStatus.OFFERED);
            entry.setOfferedAt(Instant.now());
            entry.setExpiresAt(entry.getOfferedAt().plus(Math.max(5, offerTtlMinutes()), ChronoUnit.MINUTES));
            waitlistRepository.saveAndFlush(entry);
            notifyOffer(entry);
            promoted++;
        }
        return promoted;
    }

    @Override
    public int expireOffers(int limit) {
        List<BookingWaitlistEntry> entries = waitlistRepository.findExpiredOffers(Instant.now(), Math.max(1, limit));
        entries.forEach(entry -> entry.setStatus(BookingWaitlistStatus.EXPIRED));
        waitlistRepository.saveAll(entries);
        return entries.size();
    }

    // ------------------------------------------------------------------ outils

    private boolean available(BookingWaitlistEntry entry) {
        try {
            BookingAvailabilityResponse check = availabilityService.check(availabilityRequest(entry));
            return check != null && check.available();
        } catch (RuntimeException ex) {
            log.debug("Inscription {} : disponibilite non verifiee ({})", entry.getId(), ex.getMessage());
            return false;
        }
    }

    private void notifyOffer(BookingWaitlistEntry entry) {
        if (emailSender == null || !StringUtils.hasText(entry.getContactEmail())) {
            return;
        }
        String until = entry.getExpiresAt() == null ? "" : WHEN.format(entry.getExpiresAt().atZone(AppTime.zone()));
        String body = "Bonjour " + (StringUtils.hasText(entry.getContactName()) ? entry.getContactName() : "") + ",\n\n"
                + "Une place s'est libérée : " + entry.getResource().getName() + ", " + WHEN.format(entry.getStartedAt()) + ".\n"
                + "Elle vous est réservée jusqu'au " + until + ". Acceptez-la depuis votre espace client (Réservations, liste d'attente) "
                + "ou en contactant l'accueil ; passé ce délai, elle sera proposée à la personne suivante.\n";
        try {
            emailSender.sendEmail(entry.getContactEmail(), "Une place s'est libérée : " + entry.getResource().getName(), body);
            entry.setNotifiedAt(Instant.now());
            waitlistRepository.save(entry);
        } catch (RuntimeException ex) {
            log.warn("Courriel d'offre de l'inscription {} non envoye : {}", entry.getId(), ex.getMessage());
        }
    }

    private CreateBookingRequest createRequest(BookingWaitlistEntry entry) {
        SubscriberType type = entry.getOwnerType();
        String memberId = type == SubscriberType.MEMBER ? entry.getOwnerCode() : null;
        String businessCode = type == SubscriberType.BUSINESS_ENTITY ? entry.getOwnerCode() : null;
        boolean byContact = memberId == null && businessCode == null;
        return new CreateBookingRequest(entry.getResource().getCode(), memberId, null, businessCode,
                byContact ? entry.getContactEmail() : null, byContact ? entry.getContactPhone() : null, byContact,
                entry.getContactName(), entry.getContactEmail(), entry.getContactPhone(), "WAITLIST-" + entry.getId(), null,
                entry.getStartedAt(), entry.getEndedAt(), entry.getQuantity(), entry.getPaymentMode(), true, true,
                "Réservation issue de la liste d'attente", null, null, null);
    }

    private BookingAvailabilityRequest availabilityRequest(BookingWaitlistEntry entry) {
        SubscriberType type = entry.getOwnerType();
        return new BookingAvailabilityRequest(entry.getResource().getCode(),
                type == SubscriberType.MEMBER ? entry.getOwnerCode() : null,
                null,
                type == SubscriberType.BUSINESS_ENTITY ? entry.getOwnerCode() : null,
                null, null, Boolean.FALSE, entry.getContactName(), entry.getContactEmail(), entry.getContactPhone(),
                entry.getPaymentMode(), entry.getStartedAt(), entry.getEndedAt(), entry.getQuantity(), null);
    }

    private List<BookingSuggestionResponse> alternatives(BookingWaitlistEntry entry) {
        try {
            return suggestionService.suggest(availabilityRequest(entry));
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    private BookingWaitlistEntry entry(Long id) {
        return waitlistRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Inscription en liste d'attente introuvable"));
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private BookingWaitlistEntryResponse toResponse(BookingWaitlistEntry entry, List<BookingSuggestionResponse> alternatives) {
        return new BookingWaitlistEntryResponse(
                entry.getId(),
                entry.getResource().getCode(),
                entry.getResource().getName(),
                entry.getOwnerType(),
                entry.getOwnerCode(),
                entry.getContactName(),
                entry.getContactEmail(),
                entry.getContactPhone(),
                entry.getStartedAt(),
                entry.getEndedAt(),
                entry.getQuantity(),
                entry.getPaymentMode(),
                entry.getStatus(),
                entry.getOfferedAt(),
                entry.getExpiresAt(),
                entry.getCreatedAt(),
                alternatives,
                entry.getBookingNumber(),
                entry.getNotifiedAt()
        );
    }

    private record LocalDateTimeHolder(java.time.LocalDateTime value) {
    }
}
