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

    @Override
    public BookingHoldResponse create(CreateBookingHoldRequest request) {
        if (StringUtils.hasText(request.idempotencyKey())) {
            BookingHold existing = holdRepository.findByIdempotencyKey(request.idempotencyKey().trim()).orElse(null);
            if (existing != null) {
                return toResponse(existing);
            }
        }
        Resource resource = resourceService.getResourceForService(request.resourceCode().trim());
        int quantity = request.quantity() == null ? 1 : request.quantity();
        BookingIdentityResolver.ResolvedBookingIdentity identity = identityResolver.resolve(request.identityLookup());
        resourceGuard.validateBookable(resource, request.startedAt(), request.endedAt(), quantity);
        resourceGuard.validateNoSingleCapacityConflict(resource, request.startedAt(), request.endedAt(), null);
        if (holdRepository.existsActiveOverlap(resource.getId(), request.startedAt(), request.endedAt(), null)) {
            throw new ConflictException("booking hold", "resource already has an active hold for the requested range");
        }
        availabilityService.reserve(ReserveResourceAvailabilityRequest.builder()
                .resourceCode(resource.getCode())
                .startedAt(request.startedAt())
                .endedAt(request.endedAt())
                .quantity(quantity)
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
                .expiresAt(Instant.now().plusSeconds((long) (request.ttlMinutes() == null ? 10 : request.ttlMinutes()) * 60))
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
