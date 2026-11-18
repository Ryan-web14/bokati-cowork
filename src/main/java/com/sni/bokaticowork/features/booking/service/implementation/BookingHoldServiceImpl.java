package com.sni.bokaticowork.features.booking.service.implementation;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.booking.dto.request.CreateBookingHoldRequest;
import com.sni.bokaticowork.features.booking.dto.response.BookingHoldResponse;
import com.sni.bokaticowork.features.booking.enums.BookingEventType;
import com.sni.bokaticowork.features.booking.enums.BookingHoldStatus;
import com.sni.bokaticowork.features.booking.model.BookingHold;
import com.sni.bokaticowork.features.booking.repository.BookingHoldRepository;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingHoldService;
import com.sni.bokaticowork.features.booking.service.support.BookingIdentityResolver;
import com.sni.bokaticowork.features.booking.service.support.BookingResourceGuard;
import com.sni.bokaticowork.features.ressource.dto.request.ReleaseResourceAvailabilityRequest;
import com.sni.bokaticowork.features.ressource.dto.request.ReserveResourceAvailabilityRequest;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceAvailabilityService;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Service
@Transactional
@RequiredArgsConstructor
public class BookingHoldServiceImpl implements BookingHoldService {

    private final BookingHoldRepository holdRepository;
    private final ResourceService resourceService;
    private final ResourceAvailabilityService availabilityService;
    private final BookingResourceGuard resourceGuard;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final BookingIdentityResolver identityResolver;
    private com.sni.bokaticowork.features.ressource.service.support.StaffBookingOverride staffOverride;

    /** Option posee par l'equipe : meme derogation que la reservation (preavis, debut passe). */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setStaffOverride(com.sni.bokaticowork.features.ressource.service.support.StaffBookingOverride staffOverride) {
        this.staffOverride = staffOverride;
    }

    @Override
    public BookingHoldResponse create(CreateBookingHoldRequest request) {
        // Lot B1 (ecart Bn-14) : duree bornee.
        int ttl = request.ttlMinutes() == null ? 10 : request.ttlMinutes();
        if (ttl < 1 || ttl > 1440) {
            throw new com.sni.bokaticowork.core.exception.customs.BadRequestException("Durée d'une option : de 1 à 1 440 minutes");
        }
        Resource resource = resourceService.getResourceForService(request.resourceCode().trim());
        int quantity = request.quantity() == null ? 1 : request.quantity();
        BookingIdentityResolver.ResolvedBookingIdentity identity = identityResolver.resolve(request.identityLookup());
        if (StringUtils.hasText(request.idempotencyKey())) {
            BookingHold existing = holdRepository.findByIdempotencyKey(request.idempotencyKey().trim()).orElse(null);
            if (existing != null) {
                // Lot B1 (ecart Bn-14) : un rejeu ne rend jamais l'option d'un autre.
                boolean same = existing.getResource().getId().equals(resource.getId())
                        && existing.getStartedAt().equals(request.startedAt()) && existing.getEndedAt().equals(request.endedAt());
                if (!same) {
                    throw new ConflictException("Clé d'idempotence déjà utilisée pour une autre option");
                }
                return toResponse(existing);
            }
        }
        // L'option suit la reservation : l'equipe (hors portail) pose une option hors preavis ou sur
        // un debut deja passe, sauf si la politique de la ressource l'interdit. Sans cela, l'option
        // refusait ce que la reservation accepte (« the requested start time is in the past »).
        boolean staff = staffOverride != null && staffOverride.actorFor(resource.getEffectivePolicy()).isPresent();
        resourceGuard.validateBookable(resource, request.startedAt(), request.endedAt(), quantity, staff);
        resourceGuard.validateNoSingleCapacityConflict(resource, request.startedAt(), request.endedAt(), null);
        // Lot B1 (ecart Bn-14) : plusieurs options sur un espace partage (la capacite des creneaux
        // tranche) ; une seule sur une piece entiere.
        if (resource.isWholeResourceBooking()
                && holdRepository.existsActiveOverlap(resource.getId(), request.startedAt(), request.endedAt(), null)) {
            throw new ConflictException("booking hold", "resource already has an active hold for the requested range");
        }
        availabilityService.reserve(ReserveResourceAvailabilityRequest.builder()
                .resourceCode(resource.getCode())
                .startedAt(request.startedAt())
                .endedAt(request.endedAt())
                .quantity(quantity)
                .staffOverride(staff)
                .build());
        BookingHold hold = holdRepository.save(BookingHold.builder()
                .holdNumber(sequenceGenerator.next("booking_hold"))
                .resource(resource)
                .ownerType(identity.ownerType())
                .ownerCode(identity.ownerCode())
                .startedAt(request.startedAt())
                .endedAt(request.endedAt())
                .quantity(quantity)
                .idempotencyKey(StringUtils.hasText(request.idempotencyKey()) ? request.idempotencyKey().trim() : null)
                .status(BookingHoldStatus.ACTIVE)
                .contactEmail(identity.contactEmail())
                .contactPhone(identity.contactPhone())
                .expiresAt(Instant.now().plusSeconds((long) ttl * 60))
                .build());
        return toResponse(hold);
    }

    @Override
    public BookingHoldResponse release(String holdNumber) {
        BookingHold hold = get(holdNumber);
        if (hold.getStatus() == BookingHoldStatus.ACTIVE) {
            releaseCapacity(hold);
            hold.setStatus(BookingHoldStatus.RELEASED);
            hold = holdRepository.save(hold);
        }
        return toResponse(hold);
    }

    @Override
    public int expireDue(int limit) {
        var holds = holdRepository.findExpired(Instant.now(), limit);
        holds.forEach(hold -> {
            releaseCapacity(hold);
            hold.setStatus(BookingHoldStatus.EXPIRED);
            holdRepository.save(hold);
        });
        return holds.size();
    }

    private BookingHold get(String holdNumber) {
        return holdRepository.findByHoldNumber(holdNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Booking hold " + holdNumber + " not found"));
    }

    private void releaseCapacity(BookingHold hold) {
        availabilityService.release(ReleaseResourceAvailabilityRequest.builder()
                .resourceCode(hold.getResource().getCode())
                .startedAt(hold.getStartedAt())
                .endedAt(hold.getEndedAt())
                .quantity(hold.getQuantity())
                .build());
    }

    private BookingHoldResponse toResponse(BookingHold hold) {
        return new BookingHoldResponse(hold.getHoldNumber(), hold.getResource().getCode(), hold.getOwnerType(), hold.getOwnerCode(), hold.getStartedAt(), hold.getEndedAt(), hold.getQuantity(), hold.getStatus(), hold.getExpiresAt());
    }
}
