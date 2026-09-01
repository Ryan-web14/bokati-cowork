package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.ressource.enums.ResourceStatus;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.service.support.ResourceSlotPolicy;
import com.sni.bokaticowork.features.ressource.model.ResourcePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class BookingResourceGuard {


    private final BookingRepository bookingRepository;
    private final ResourceSlotPolicy slotPolicy;

    public void validateBookable(Resource resource, LocalDateTime startedAt, LocalDateTime endedAt, int quantity) {
        validateRange(startedAt, endedAt);
        slotPolicy.assertAligned(resource, startedAt, endedAt);
        if (!Boolean.TRUE.equals(resource.getBookingEnabled()) || !Boolean.TRUE.equals(resource.getActive())) {
            throw new ConflictException("booking", "resource is not enabled for booking");
        }
        if (resource.getStatus() != ResourceStatus.ACTIVE) {
            throw new ConflictException("booking", "resource status does not allow booking");
        }
        if (quantity < 1) {
            throw new BadRequestException("Booking quantity must be greater than zero");
        }
        if (quantity > resource.resolveBookableSlots()) {
            throw new ConflictException("booking", "requested quantity exceeds resource bookable slots");
        }
        validatePolicy(resource.getResourcePolicy(), startedAt, endedAt);
    }

    public void validateNoSingleCapacityConflict(Resource resource, LocalDateTime startedAt, LocalDateTime endedAt, String excludedBookingNumber) {
        if (resource.resolveBookableSlots() > 1) {
            return;
        }
        if (bookingRepository.existsActiveConflict(resource.getId(), startedAt, endedAt, excludedBookingNumber)) {
            throw new ConflictException("booking", "resource is already booked for the requested range");
        }
    }

    private void validateRange(LocalDateTime startedAt, LocalDateTime endedAt) {
        if (startedAt == null || endedAt == null) {
            throw new BadRequestException("Booking start and end dates are required");
        }
        if (!endedAt.isAfter(startedAt)) {
            throw new BadRequestException("Booking end date must be after start date");
        }
        // L'alignement se verifie par ResourceSlotPolicy, seule source de la duree de creneau.
        // Cette classe en redefinissait sa propre constante : deux sources de verite pour la
        // meme regle, qu'il suffisait de faire diverger d'une valeur pour rendre reservable une
        // plage sans creneau. Le controle dependant de la ressource, il migre vers validateRange
        // (Resource, ...) ci-dessous.
        if (startedAt.getSecond() != 0 || endedAt.getSecond() != 0 || startedAt.getNano() != 0 || endedAt.getNano() != 0) {
            throw new BadRequestException("Booking range must not contain seconds or fractional seconds");
        }
    }

    private void validatePolicy(ResourcePolicy policy, LocalDateTime startedAt, LocalDateTime endedAt) {
        if (policy == null) {
            return;
        }
        int requestedMinutes = Math.toIntExact(Duration.between(startedAt, endedAt).toMinutes());
        if (requestedMinutes < policy.getMinBookingDurationMinutes()) {
            throw new ConflictException("booking", "requested duration is below resource policy minimum");
        }
        if (requestedMinutes > policy.getMaxBookingDurationMinutes()) {
            throw new ConflictException("booking", "requested duration exceeds resource policy maximum");
        }
        if (startedAt.isBefore(LocalDateTime.now().plusMinutes(policy.getMinBookingNoticeMinutes()))) {
            throw new ConflictException("booking", "minimum booking notice is not respected");
        }
    }
}
