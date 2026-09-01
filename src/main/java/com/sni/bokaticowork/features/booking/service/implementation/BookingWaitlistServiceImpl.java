package com.sni.bokaticowork.features.booking.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.booking.dto.request.BookingAvailabilityRequest;
import com.sni.bokaticowork.features.booking.dto.request.JoinBookingWaitlistRequest;
import com.sni.bokaticowork.features.booking.dto.response.BookingSuggestionResponse;
import com.sni.bokaticowork.features.booking.dto.response.BookingWaitlistEntryResponse;
import com.sni.bokaticowork.features.booking.enums.BookingWaitlistStatus;
import com.sni.bokaticowork.features.booking.model.BookingWaitlistEntry;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.booking.repository.BookingWaitlistEntryRepository;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingSuggestionService;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingWaitlistService;
import com.sni.bokaticowork.features.booking.service.support.BookingIdentityResolver;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class BookingWaitlistServiceImpl implements BookingWaitlistService {

    private final BookingWaitlistEntryRepository waitlistRepository;
    private final BookingRepository bookingRepository;
    private final ResourceService resourceService;
    private final BookingIdentityResolver identityResolver;
    private final BookingSuggestionService suggestionService;

    @Value("${bokati.booking.waitlist.offer-ttl-minutes:30}")
    private int offerTtlMinutes;

    @Override
    public BookingWaitlistEntryResponse join(JoinBookingWaitlistRequest request) {
        if (!request.endedAt().isAfter(request.startedAt())) {
            throw new BadRequestException("Waitlist end time must be after start time");
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
                .map(entry -> toResponse(entry, alternatives(entry)))
                .toList();
    }

    @Override
    public BookingWaitlistEntryResponse cancel(Long id) {
        BookingWaitlistEntry entry = waitlistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking waitlist entry not found"));
        entry.setStatus(BookingWaitlistStatus.CANCELLED);
        return toResponse(waitlistRepository.save(entry), alternatives(entry));
    }

    @Override
    public int promoteAvailable(int limit) {
        int promoted = 0;
        for (BookingWaitlistEntry entry : waitlistRepository.findWaitingCandidates(Math.max(1, limit))) {
            boolean conflict = bookingRepository.existsActiveConflict(
                    entry.getResource().getId(),
                    entry.getStartedAt(),
                    entry.getEndedAt(),
                    null
            );
            if (!conflict) {
                entry.setStatus(BookingWaitlistStatus.OFFERED);
                entry.setOfferedAt(Instant.now());
                entry.setExpiresAt(entry.getOfferedAt().plus(Math.max(5, offerTtlMinutes), ChronoUnit.MINUTES));
                waitlistRepository.save(entry);
                promoted++;
            }
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

    private List<BookingSuggestionResponse> alternatives(BookingWaitlistEntry entry) {
        return suggestionService.suggest(new BookingAvailabilityRequest(
                entry.getResource().getCode(),
                entry.getOwnerType().name().equals("MEMBER") ? entry.getOwnerCode() : null,
                entry.getOwnerType().name().equals("CUSTOMER") ? entry.getOwnerCode() : null,
                entry.getOwnerType().name().equals("BUSINESS") ? entry.getOwnerCode() : null,
                null,
                null,
                Boolean.FALSE,
                entry.getContactName(),
                entry.getContactEmail(),
                entry.getContactPhone(),
                entry.getPaymentMode(),
                entry.getStartedAt(),
                entry.getEndedAt(),
                entry.getQuantity(),
                null
        ));
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
                alternatives
        );
    }
}
