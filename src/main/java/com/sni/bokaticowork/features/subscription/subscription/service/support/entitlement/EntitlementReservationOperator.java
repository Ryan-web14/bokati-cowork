package com.sni.bokaticowork.features.subscription.subscription.service.support.entitlement;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.EntitlementOperationRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.EntitlementOperationResponse;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementGrantStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementReservationStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementTransactionType;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementGrant;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementReservation;
import com.sni.bokaticowork.features.subscription.repository.EntitlementGrantRepository;
import com.sni.bokaticowork.features.subscription.repository.EntitlementLedgerRepository;
import com.sni.bokaticowork.features.subscription.repository.EntitlementReservationRepository;
import com.sni.bokaticowork.features.subscription.subscription.service.support.EntitlementLedgerWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EntitlementReservationOperator {

    private final EntitlementGrantRepository grantRepository;
    private final EntitlementLedgerRepository ledgerRepository;
    private final EntitlementReservationRepository reservationRepository;
    private final EntitlementLedgerWriter ledgerWriter;
    private final EntitlementBalanceReader balanceReader;
    private final EntitlementGrantBalanceOperator grantBalanceOperator;
    private final EntitlementRequestValidator validator;
    private final EntitlementQuotaAlertService quotaAlertService;

    public EntitlementOperationResponse reserve(EntitlementOperationRequest request) {
        validator.reference(request);
        if (isDuplicate(request)) {
            return balanceReader.check(request);
        }

        BigDecimal remainingToReserve = request.quantity();
        List<EntitlementGrant> grants = balanceReader.usableGrants(request);

        if (balanceReader.hasUnlimitedGrant(grants)) {
            EntitlementGrant grant = firstUnlimited(grants);
            reservationRepository.save(toReservation(request, grant, request.quantity()));
            ledgerWriter.write(grant, EntitlementTransactionType.RESERVE, request.quantity(), null, null, request.referenceType(), request.referenceId(), request.idempotencyKey(), request.reason());
            return new EntitlementOperationResponse(true, request.entitlementCode(), request.quantity(), null, "Unlimited entitlement reserved");
        }

        BigDecimal available = balanceReader.availableQuantity(grants);
        if (available.compareTo(request.quantity()) < 0) {
            throw new ConflictException("entitlement", "insufficient balance");
        }

        for (EntitlementGrant candidate : grants) {
            if (remainingToReserve.signum() <= 0) {
                break;
            }
            EntitlementGrant grant = grantRepository.findLockedById(candidate.getId()).orElseThrow();
            BigDecimal before = grant.getQuantityRemaining();
            BigDecimal reserved = before.min(remainingToReserve);
            BigDecimal after = before.subtract(reserved);
            grant.setQuantityRemaining(after);
            if (after.signum() == 0) {
                grant.setStatus(EntitlementGrantStatus.DEPLETED);
            }
            grantRepository.save(grant);
            reservationRepository.save(toReservation(request, grant, reserved));
            ledgerWriter.write(grant, EntitlementTransactionType.RESERVE, reserved, before, after, request.referenceType(), request.referenceId(), request.idempotencyKey(), request.reason());
            quotaAlertService.checkAndAlert(grant, after);
            remainingToReserve = remainingToReserve.subtract(reserved);
        }

        return new EntitlementOperationResponse(true, request.entitlementCode(), request.quantity(), available.subtract(request.quantity()), "Entitlement reserved");
    }

    public EntitlementOperationResponse consume(EntitlementOperationRequest request) {
        List<EntitlementReservation> reservations = activeReservations(request);
        if (reservations.isEmpty()) {
            return grantBalanceOperator.debit(request, EntitlementTransactionType.CONSUME);
        }

        BigDecimal reservedQuantity = reservations.stream()
                .map(EntitlementReservation::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (reservedQuantity.compareTo(request.quantity()) < 0) {
            throw new ConflictException("entitlement reservation", "reserved quantity is lower than requested consumption");
        }

        reservations.forEach(reservation -> {
            reservation.setStatus(EntitlementReservationStatus.CONSUMED);
            reservationRepository.save(reservation);
            ledgerWriter.write(
                    reservation.getGrant(),
                    EntitlementTransactionType.CONSUME,
                    reservation.getQuantity(),
                    reservation.getGrant().getQuantityRemaining(),
                    reservation.getGrant().getQuantityRemaining(),
                    request.referenceType(),
                    request.referenceId(),
                    request.idempotencyKey(),
                    request.reason()
            );
        });
        return new EntitlementOperationResponse(true, request.entitlementCode(), request.quantity(), null, "Reserved entitlement consumed");
    }

    public EntitlementOperationResponse release(EntitlementOperationRequest request) {
        return releaseReservations(request, EntitlementReservationStatus.RELEASED, "Reserved entitlement released");
    }

    public int expireReservations() {
        List<EntitlementReservation> reservations = reservationRepository.findExpiredActiveReservations(
                EntitlementReservationStatus.ACTIVE.name(),
                Instant.now()
        );
        reservations.forEach(reservation -> {
            reservation.setStatus(EntitlementReservationStatus.EXPIRED);
            reservationRepository.save(reservation);
            EntitlementGrant grant = grantRepository.findLockedById(reservation.getGrant().getId()).orElseThrow();
            if (!Boolean.TRUE.equals(grant.getUnlimited())) {
                BigDecimal before = grant.getQuantityRemaining();
                BigDecimal after = before.add(reservation.getQuantity());
                grant.setQuantityRemaining(after);
                grant.setStatus(EntitlementGrantStatus.ACTIVE);
                grantRepository.save(grant);
                ledgerWriter.write(grant, EntitlementTransactionType.RELEASE, reservation.getQuantity(), before, after, "WORKER", "RESERVATION_EXPIRY", null, "reservation expired");
            } else {
                ledgerWriter.write(grant, EntitlementTransactionType.RELEASE, reservation.getQuantity(), null, null, "WORKER", "RESERVATION_EXPIRY", null, "reservation expired");
            }
        });
        return reservations.size();
    }

    private EntitlementOperationResponse releaseReservations(EntitlementOperationRequest request,
                                                            EntitlementReservationStatus targetStatus,
                                                            String message) {
        validator.reference(request);
        List<EntitlementReservation> reservations = activeReservations(request);
        if (reservations.isEmpty()) {
            return grantBalanceOperator.credit(request, EntitlementTransactionType.RELEASE);
        }

        BigDecimal released = BigDecimal.ZERO;
        for (EntitlementReservation reservation : reservations) {
            reservation.setStatus(targetStatus);
            reservationRepository.save(reservation);
            EntitlementGrant grant = grantRepository.findLockedById(reservation.getGrant().getId()).orElseThrow();
            if (!Boolean.TRUE.equals(grant.getUnlimited())) {
                BigDecimal before = grant.getQuantityRemaining();
                BigDecimal after = before.add(reservation.getQuantity());
                grant.setQuantityRemaining(after);
                grant.setStatus(EntitlementGrantStatus.ACTIVE);
                grantRepository.save(grant);
                ledgerWriter.write(grant, EntitlementTransactionType.RELEASE, reservation.getQuantity(), before, after, request.referenceType(), request.referenceId(), request.idempotencyKey(), request.reason());
            } else {
                ledgerWriter.write(grant, EntitlementTransactionType.RELEASE, reservation.getQuantity(), null, null, request.referenceType(), request.referenceId(), request.idempotencyKey(), request.reason());
            }
            released = released.add(reservation.getQuantity());
        }

        return new EntitlementOperationResponse(true, request.entitlementCode(), released, null, message);
    }

    private List<EntitlementReservation> activeReservations(EntitlementOperationRequest request) {
        if (!StringUtils.hasText(request.referenceType()) || !StringUtils.hasText(request.referenceId())) {
            return List.of();
        }
        return reservationRepository.findActiveByReference(
                request.ownerType().name(),
                request.ownerCode(),
                request.entitlementCode(),
                request.referenceType(),
                request.referenceId(),
                EntitlementReservationStatus.ACTIVE.name()
        );
    }

    private EntitlementReservation toReservation(EntitlementOperationRequest request, EntitlementGrant grant, BigDecimal quantity) {
        return EntitlementReservation.builder()
                .grant(grant)
                .ownerType(request.ownerType())
                .ownerCode(request.ownerCode())
                .entitlementCode(request.entitlementCode())
                .quantity(quantity)
                .referenceType(request.referenceType())
                .referenceId(request.referenceId())
                .status(EntitlementReservationStatus.ACTIVE)
                .expiresAt(Instant.now().plusSeconds(900))
                .build();
    }

    private boolean isDuplicate(EntitlementOperationRequest request) {
        return StringUtils.hasText(request.idempotencyKey()) && ledgerRepository.existsByIdempotencyKey(request.idempotencyKey());
    }

    private EntitlementGrant firstUnlimited(List<EntitlementGrant> grants) {
        return grants.stream()
                .filter(candidate -> Boolean.TRUE.equals(candidate.getUnlimited()))
                .findFirst()
                .orElseThrow();
    }
}
